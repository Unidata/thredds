/*
 * Copyright 1997-2007 Unidata Program Center/University Corporation for
 * Atmospheric Research, P.O. Box 3000, Boulder, CO 80307,
 * support@unidata.ucar.edu.
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or (at
 * your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser
 * General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package ucar.nc2.ncml;

import ucar.ma2.*;
import ucar.nc2.*;
import ucar.nc2.dataset.*;
import ucar.nc2.units.TimeUnit;
import ucar.nc2.units.DateFormatter;
import ucar.nc2.units.DateFromString;
import ucar.nc2.util.CancelTask;
import ucar.nc2.util.DiskCache2;
import ucar.unidata.util.StringUtil;

import java.util.*;
import java.io.*;

/**
 * Implement NcML Aggregation.
 * <p/>
 * <h2>Implementation Notes</h2>
 * <h3>Caching</h3>
 * <ul>
 * <li>Case 1. Explicit list / Scan static directories (recheck=null)
 * <ul>
 * <li>A. AggCaching - keep track of ncoords, coordValues for joinExisting. Read on open, write on close.
 * Could skip scan if cache exists.
 * <li>B. NetcdfFileCache - write on close if changed (only first time). On sync, recheck = null means wont be reread.
 * </ul>
 * <li>Case 2. Scan dynamic directories (recheck non-null)
 * <ul>
 * <li>A. AggCaching - keep track of ncoords, coordValues for joinExisting. Read on open, write on close.
 * Could skip scan if cache exists, and recheck time not expired.
 * <li>B. NetcdfFileCache - write on close if changed. On sync, if recheck time, then rescan.
 * </ul>
 * </ul>
 * <h3>Aggregation Coordinate Variable (aggCoord) Processing</h3>
 * Construction:
 * <ol>
 * <li> The aggregation element is processed first.
 * <li> agg.finish() is called.
 * <li> If the user has defined the aggCoord in the NcML, it is then processed, overriding whatever the aggregation has constructed.
 * If values are defined, they are cached in the new variable.
 * </ol>
 * Data Reading:
 * <ol>
 * <li> If values are cached, agg.read() is never called.
 * <li> Each Dataset may have a coordinate value(s) defined in the NcML coordValue attribute.
 * <li> If not, the coordinate value(s) is cached when the dataset is opened.
 * <li> agg.read() uses those if they exist, else reads and caches.
 * </ol>
 *
 * @author caron
 */
public abstract class Aggregation implements AggregationIF, ProxyReader {
  static protected int TYPICAL_DATASET_RANDOM = 0;
  static protected int TYPICAL_DATASET_LATEST = 1;
  static protected int TYPICAL_DATASET_PENULTIMATE = 2;
  static protected int typicalDatasetMode = 0;

  static protected org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(Aggregation.class);
  static protected DiskCache2 diskCache2 = null;

  static public void setPersistenceCache(DiskCache2 dc) {
    diskCache2 = dc;
  }

  static public void setTypicalDatasetMode(String mode) {
    if (mode.equalsIgnoreCase("random"))
      typicalDatasetMode = TYPICAL_DATASET_RANDOM;
    else if (mode.equalsIgnoreCase("latest"))
      typicalDatasetMode = TYPICAL_DATASET_LATEST;
    else if (mode.equalsIgnoreCase("penultimate"))
      typicalDatasetMode = TYPICAL_DATASET_PENULTIMATE;
    else
      logger.error("Unknown setTypicalDatasetMode= " + mode);
  }

  //////////////////////////////////////////////////////////////////////////////////////////

  protected NetcdfDataset ncDataset; // the aggregation belongs to this dataset
  protected String dimName; // the aggregation dimension name
  protected Type type; // the aggregation type
  protected List<Dataset> nestedDatasets; // working set of Aggregation.Dataset
  private int totalCoords = 0;  // the aggregation dimension size
  protected Object spiObject;

  // explicit
  private List<String> vars = new ArrayList<String>(); // variable names (String)
  protected List<Dataset> explicitDatasets = new ArrayList<Dataset>(); // explicitly created Dataset objects from netcdf elements

  // scan
  protected List<DirectoryScan> scanList = new ArrayList<DirectoryScan>(); // current set of DirectoryScan for scan elements
  protected TimeUnit recheck; // how often to recheck
  protected long lastChecked; // last time checked
  protected boolean wasChanged = true; // something changed since last aggCache file was written
  protected boolean isDate = false;  // has a dateFormatMark, so agg coordinate variable is a Date

  protected DateFormatter formatter = new DateFormatter();
  protected boolean debug = false, debugOpenFile = false, debugCacheDetail = false, debugSyncDetail = false, debugProxy = false,
          debugScan = false, debugRead = false;

  /**
   * Create an Aggregation for the given NetcdfDataset.
   * The following addXXXX methods are called, then finish(), before the object is ready for use.
   *
   * @param ncd      Aggregation belongs to this NetcdfDataset
   * @param dimName  the aggregation dimension name
   * @param type     the Aggregation.Type
   * @param recheckS how often to check if files have changes
   */
  protected Aggregation(NetcdfDataset ncd, String dimName, Type type, String recheckS) {
    this.ncDataset = ncd;
    this.dimName = dimName;
    this.type = type;

    if (recheckS != null) {
      try {
        this.recheck = new TimeUnit(recheckS);
      } catch (Exception e) {
        logger.error("Invalid time unit for recheckEvery = {}", recheckS);
      }
    }
  }

  /**
   * Add a nested dataset (other than a union), specified by an explicit netcdf element.
   * enhance is handled by the reader, so its always false here.
   *
   * @param cacheName   a unique name to use for caching
   * @param location    attribute "location" on the netcdf element
   * @param ncoordS     attribute "ncoords" on the netcdf element
   * @param coordValueS attribute "coordValue" on the netcdf element
   * @param reader      factory for reading this netcdf dataset
   * @param cancelTask  user may cancel, may be null
   */
  public void addExplicitDataset(String cacheName, String location, String ncoordS, String coordValueS, NetcdfFileFactory reader, CancelTask cancelTask) {
    // boolean enhance = (enhanceS != null) && enhanceS.equalsIgnoreCase("true");
    Dataset nested = makeDataset(cacheName, location, ncoordS, coordValueS, false, reader);
    explicitDatasets.add(nested);
  }

  public void addDataset(Dataset nested) {
    explicitDatasets.add(nested);
  }

  /**
   * Add a scan elemnt
   *
   * @param dirName             scan this directory
   * @param suffix              filter on this suffix (may be null)
   * @param regexpPatternString include if full name matches this regular expression (may be null)
   * @param dateFormatMark      create dates from the filename (may be null)
   * @param enhance             should files bne enhanced?
   * @param subdirs             equals "false" if should not descend into subdirectories
   * @param olderThan           files must be older than this time (now - lastModified >= olderThan); must be a time unit, may ne bull
   * @throws IOException if I/O error
   */
  public void addDirectoryScan(String dirName, String suffix, String regexpPatternString, String dateFormatMark, String enhance, String subdirs, String olderThan) throws IOException {
    DirectoryScan d = new DirectoryScan(dirName, suffix, regexpPatternString, dateFormatMark, enhance, subdirs, olderThan);
    scanList.add(d);
    if (dateFormatMark != null)
      isDate = true;
  }

  /**
   * Add a name from a variableAgg element
   *
   * @param varName name of variable to add
   */
  public void addVariable(String varName) {
    vars.add(varName);
  }

  public String getDimensionName() {
    return dimName;
  }

  public int getTotalCoords() {
    return totalCoords;
  }

  public List<Dataset> getNestedDatasets() {
    return nestedDatasets;
  }

  public Type getType() {
    return type;
  }

  public boolean isDate() {
    return isDate;
  }

  /**
   * Get the list of aggregation variables: variables whose data spans multiple files.
   * @return the list of aggregation variable names
   */
  public List<String> getVariables() {
    return vars;
  }

  /**
   * What is the data type of the aggregation coordinate ?
   * @return the data type of the aggregation coordinate
   */
  public DataType getCoordinateType() {
    Dataset first = nestedDatasets.get(0);
    return first.isStringValued ? DataType.STRING : DataType.DOUBLE;
  }

  /**
   * Release all resources associated with the aggregation
   * @throws IOException on error
   */
  public void close() throws IOException {
    persist();
    for (Dataset ds : nestedDatasets) {
      ds.close();
    }
  }

  /**
   * Allow information to be make persistent. Overridden in AggregationExisting
   * @throws IOException on error
   */
  public void persist() throws IOException {
  }

  // read info from the persistent XML file, if it exists; overridden in AggregationExisting
  protected void persistRead() {
  }

  /**
   * Is the named variable an "aggregation variable" ?
   *
   * @param name variable name
   * @return true if the named variable is an aggregation variable
   */
  private boolean isAggVariable(String name) {
    for (String vname : vars) {
      if (vname.equals(name))
        return true;
    }
    return false;
  }

  ///////////////////////////////////////////////////////////////////////////////////////////////////////////

  // all elements are processed, finish construction

  public void finish(CancelTask cancelTask) throws IOException {
    nestedDatasets = new ArrayList<Dataset>();

    // LOOK fix from Michael Godin 3/14/06 - need to test
    // nestedDatasets.addAll(explicitDatasets);
    for (Dataset dataset : explicitDatasets) {
      if (dataset.checkOK(cancelTask))
        nestedDatasets.add(dataset);
    }

    if (scanList.size() > 0)
      scan(nestedDatasets, cancelTask);

    // check persistence info
    if ((diskCache2 != null) && (type == Type.JOIN_EXISTING))
      persistRead();

    //ucar.unidata.io.RandomAccessFile.setDebugAccess( true);
    buildDataset(true, cancelTask);
    //ucar.unidata.io.RandomAccessFile.setDebugAccess( false);

    this.lastChecked = System.currentTimeMillis();
    wasChanged = true;
  }


  // LOOK isNew may be fishy
  protected abstract void buildDataset(boolean isNew, CancelTask cancelTask) throws IOException;

  protected void buildCoords(CancelTask cancelTask) throws IOException {
    if (type == Type.FORECAST_MODEL_COLLECTION) {
      for (Dataset nested : nestedDatasets) {
        nested.ncoord = 1;
      }
    }

    totalCoords = 0;
    for (Dataset nested : nestedDatasets) {
      totalCoords += nested.setStartEnd(totalCoords, cancelTask);
    }
  }

  //////////////////////////////////////////////////////////////
  // LOOK: this whole sync stuff is crap, not tested, only works sometimes !!!!

  /**
   * Check to see if its time to rescan directory, and if so, rescan and extend dataset if needed.
   *
   * @param force if true, always rescan even if time not expired
   * @return true if directory was rescanned and dataset may have been updated
   * @throws IOException on io error
   */
  public synchronized boolean syncExtend(boolean force) throws IOException {
    if (!force && !timeToRescan())
      return false;
    if (!rescan())
      return false;

    // only the set of datasets may have changed
    if ((getType() == Aggregation.Type.FORECAST_MODEL_COLLECTION) || (getType() == Aggregation.Type.FORECAST_MODEL_SINGLE)) {
      //ucar.unidata.io.RandomAccessFile.setDebugAccess( true);
      syncDataset(null);
      //ucar.unidata.io.RandomAccessFile.setDebugAccess( false);

    } /*else if (getType() == Aggregation.Type.FORECAST_MODEL_SINGLE) {
      ncDataset.empty(); 
      buildDataset(false, null);
      ncDataset.finish();
      if (ncDataset.isEnhanced()) { // force recreation of the coordinate systems
        ncDataset.setCoordSysWereAdded(false);
        ncDataset.enhance();
        ncDataset.finish();
      }
    } */

    /* if (getType() == Aggregation.Type.JOIN_NEW)
      resetNewDimension();
    else if (getType() == Aggregation.Type.JOIN_EXISTING)
      resetAggDimensionLength();*/ // LOOK

    return true;
  }

  protected void syncDataset(CancelTask cancelTask) throws IOException {
  }

  public synchronized boolean sync() throws IOException {
    if (!timeToRescan())
      return false;
    if (!rescan())
      return false;

    //ncd.empty(); LOOK not emptying !!

    // LOOK: do we have to reread the NcML ?? This whole thing is fishy.
    // rebuild the metadata
    buildDataset(false, null);
    ncDataset.finish();
    if (ncDataset.getEnhanceMode() != NetcdfDataset.EnhanceMode.None) { // force recreation of the coordinate systems
      ncDataset.clearCoordinateSystems();
      ncDataset.enhance( ncDataset.getEnhanceMode());
      ncDataset.finish();
    }

    return true;
  }

  // check if recheckEvery time has passed

  /**
   * Rescan if recheckEvery time has passed
   * @return if theres new datasets, put new datasets into nestedDatasets
   */
  protected boolean timeToRescan() {
    if (getType() == Aggregation.Type.UNION) {
      if (debugSyncDetail) System.out.println(" *Sync not needed for Union");
      return false;
    }

    // see if we need to recheck
    if (recheck == null) {
      if (debugSyncDetail) System.out.println(" *Sync not needed, recheck is null");
      return false;
    }

    Date now = new Date();
    Date lastCheckedDate = new Date(lastChecked);
    Date need = recheck.add(lastCheckedDate);
    if (now.before(need)) {
      if (debug) System.out.println(" *Sync not needed, last= " + lastCheckedDate + " now = " + now);
      return false;
    }

    return true;
  }

  // protected by synch
  protected boolean rescan() throws IOException {

    // ok were gonna recheck
    lastChecked = System.currentTimeMillis();
    if (debug) System.out.println(" *Sync at " + new Date());

    // rescan
    List<Dataset> newDatasets = new ArrayList<Dataset>();
    scan(newDatasets, null);

    // replace with previous datasets if they exist
    boolean changed = false;
    for (int i = 0; i < newDatasets.size(); i++) {
      Dataset newDataset = newDatasets.get(i);
      int index = nestedDatasets.indexOf(newDataset); // equal if location is equal
      if (index >= 0) {
        newDatasets.set(i, nestedDatasets.get(index));
        if (debugSyncDetail) System.out.println("  sync using old Dataset= " + newDataset.location);
      } else {
        changed = true;
        if (debugSyncDetail) System.out.println("  sync found new Dataset= " + newDataset.location);
      }
    }

    if (!changed) { // check for deletions
      for (Dataset oldDataset : nestedDatasets) {
        if ((newDatasets.indexOf(oldDataset) < 0) && (explicitDatasets.indexOf(oldDataset) < 0)) {
          changed = true;
          if (debugSyncDetail) System.out.println("  sync found deleted Dataset= " + oldDataset.location);
        }
      }
    }

    if (!changed) return false;

    // recreate the list of datasets
    nestedDatasets = new ArrayList<Dataset>();
    nestedDatasets.addAll(explicitDatasets);
    nestedDatasets.addAll(newDatasets);

    return true;
  }

  /* private void resetAggDimensionLength() {
    // reset the aggregation dimension
    Dimension aggDim = ncd.getRootGroup().findDimension(getDimensionName());
    aggDim.setLength(getTotalCoords());

    // reset variables with new length
    List vars = ncd.getVariables();
    for (int i = 0; i < vars.size(); i++) {
      Variable v = (Variable) vars.get(i);
      if (v.getRank() == 0) continue;

      Dimension d = v.getDimension(0);
      if (getDimensionName().equals(d.getName())) {
        v.setDimensions(v.getDimensions());
        v.setCachedData(null, false); // LOOK
      }
    }
  }

  private void resetNewDimension() {
    // reset the aggregation dimension
    Dimension aggDim = ncd.getRootGroup().findDimension(getDimensionName());
    aggDim.setLength(getTotalCoords());

    // create aggregation coordinate variable
    DataType coordType = null;
    Variable coordVar = ncd.getRootGroup().findVariable(dimName);
    coordVar.setDimensions(dimName); // reset its dimension

    // reset coordinate values
    if (!coordVar.hasCachedData()) {
      int[] shape = new int[]{getTotalCoords()};
      Array coordData = Array.factory(coordType.getClassType(), shape);
      Index ima = coordData.getIndex();
      List nestedDataset = getNestedDatasets();
      for (int i = 0; i < nestedDataset.size(); i++) {
        Aggregation.Dataset nested = (Aggregation.Dataset) nestedDataset.get(i);
        if (coordType == DataType.STRING)
          coordData.setObject(ima.set(i), nested.getCoordValueString());
        else
          coordData.setDouble(ima.set(i), nested.getCoordValue());
      }
      coordVar.setCachedData(coordData, true);
    }

    // now we can reset all the aggNew variables
    // use only named variables
    List vars = getVariables();
    for (int i = 0; i < vars.size(); i++) {
      String varname = (String) vars.get(i);
      Variable v = ncd.getRootGroup().findVariable(varname);
      if (v == null) {
        System.out.println("aggNewDimension cant find variable " + varname);
        continue;
      }

      v.setDimensions(v.getDimensions());
      v.setCachedData(null, false);
    }
  } */

  /////////////////////////////////////////////////////////////////////////


  /**
   * Open one of the nested datasets as a template for the aggregation dataset.
   * @return a typical Dataset
   * @throws FileNotFoundException if there are no datasets
   */
  protected Dataset getTypicalDataset() throws IOException {
    int n = nestedDatasets.size();
    if (n == 0)
      throw new FileNotFoundException("No datasets in this aggregation");

    int select;
    if (typicalDatasetMode == TYPICAL_DATASET_LATEST)
      select = n - 1;
    else if (typicalDatasetMode == TYPICAL_DATASET_PENULTIMATE)
      select = (n < 2) ? 0 : n - 2;
    else // random is default
      select = (n < 2) ? 0 : new Random().nextInt(n);

    return nestedDatasets.get(select);
  }

  protected void makeProxies(Dataset typicalDataset, NetcdfDataset newds) throws IOException {

    // all normal variables must use a proxy to lock the file
    DatasetProxyReader proxy = new DatasetProxyReader(typicalDataset);
    List<Variable> allVars = newds.getVariables();
    for (Variable v : allVars) {
      VariableEnhanced ve = (VariableEnhanced) v; // need this for getProxyReader2()
      if (ve.getProxyReader() != null) {
        if (debugProxy) System.out.println(" debugProxy: hasProxyReader " + ve.getName());
        continue; // dont mess with agg variables
      }

      if (v.isCaching()) {  // cache the small ones
        if (!v.hasCachedData()) {
          ve.read();
          if (debugProxy) System.out.println(" debugProxy: cached " + ve.getName());
        } else {
          if (debugProxy) System.out.println(" debugProxy: already cached " + ve.getName());
        }

      } else if (null == ve.getProxyReader()) { // put proxy on the rest
        ve.setProxyReader(proxy);
        if (debugProxy) System.out.println(" debugProxy: proxy on " + ve.getName());
      }
    }
  }

  // LOOK: what is this all about? possible interaction with rest of ncml ??
  protected class MyReplaceVariableCheck implements ReplaceVariableCheck {
    public boolean replace(Variable v) {
      // needs to be replaced if its not an agg variable

      if (getType() == Type.JOIN_NEW) {
        return isAggVariable(v.getName());
      } else {
        if (v.getRank() < 1) return true;
        Dimension d = v.getDimension(0);
        return !getDimensionName().equals(d.getName());
      }
    }
  }

  //////////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * Read an aggregation variable: A variable whose data spans multiple files.
   *
   * @param mainv      the aggregation variable
   * @param cancelTask allow the user to cancel
   * @return the data array
   * @throws IOException
   */
  public Array read(Variable mainv, CancelTask cancelTask) throws IOException {

    // the case of the agg coordinate var for joinExisting or joinNew
    if (((type == Type.JOIN_NEW) || (type == Type.JOIN_EXISTING) || (type == Type.JOIN_EXISTING_ONE) || (type == Type.FORECAST_MODEL_COLLECTION)) && mainv.getShortName().equals(dimName))
      return readAggCoord(mainv, cancelTask);

    DataType dtype = (mainv instanceof VariableDS) ? ((VariableDS) mainv).getOriginalDataType() : mainv.getDataType();
    Array allData = Array.factory(dtype, mainv.getShape()); // LOOK why getOriginalDataType() ?
    int destPos = 0;

    for (Dataset vnested : nestedDatasets) {
      Array varData = vnested.read(mainv, cancelTask);
      if ((cancelTask != null) && cancelTask.isCancel())
        return null;

      Array.arraycopy(varData, 0, allData, destPos, (int) varData.getSize());
      destPos += varData.getSize();
    }

    return allData;
  }

  protected Array readAggCoord(Variable aggCoord, CancelTask cancelTask) throws IOException {
    DataType dtype = aggCoord.getDataType();
    Array allData = Array.factory(dtype, aggCoord.getShape());
    IndexIterator result = allData.getIndexIterator();

    for (Dataset vnested : nestedDatasets) {

      try {
        readAggCoord(aggCoord, cancelTask, vnested, dtype, result, null, null, null);
      } catch (InvalidRangeException e) {
        e.printStackTrace();  // cant happen
      }

      if ((cancelTask != null) && cancelTask.isCancel())
        return null;
    }

    // cache it so we dont have to come here again; make sure we invalidate cache when data changes!
    aggCoord.setCachedData(allData, false);

    return allData;
  }

  /**
   * Read a section of an aggregation variable.
   *
   * @param mainv      the aggregation variable
   * @param cancelTask allow the user to cancel
   * @param section    read just this section of the data, array of Range
   * @return the data array section
   * @throws IOException
   */
  public Array read(Variable mainv, Section section, CancelTask cancelTask) throws IOException, InvalidRangeException {
    // If its full sized, then use full read, so that data gets cached.
    long size = section.computeSize();
    if (size == mainv.getSize())
      return read(mainv, cancelTask);

    // the case of the agg coordinate var for joinExisting or joinNew
    if (((type == Type.JOIN_NEW) || (type == Type.JOIN_EXISTING) || (type == Type.JOIN_EXISTING_ONE) || (type == Type.FORECAST_MODEL_COLLECTION)) && mainv.getShortName().equals(dimName))
      return readAggCoord(mainv, section, cancelTask);

    DataType dtype = (mainv instanceof VariableDS) ? ((VariableDS) mainv).getOriginalDataType() : mainv.getDataType();
    Array sectionData = Array.factory(dtype, section.getShape());
    int destPos = 0;

    List<Range> ranges = section.getRanges();
    Range joinRange = section.getRange(0);
    List<Range> nestedSection = new ArrayList<Range>(ranges); // get copy
    List<Range> innerSection = ranges.subList(1, ranges.size());

    if (debug) System.out.println("   agg wants range=" + mainv.getName() + "(" + joinRange + ")");

    for (Dataset nested : nestedDatasets) {
      Range nestedJoinRange = nested.getNestedJoinRange(joinRange);
      if (nestedJoinRange == null)
        continue;
      if (debug)
        System.out.println("   agg use " + nested.aggStart + ":" + nested.aggEnd + " range= " + nestedJoinRange + " file " + nested.getLocation());

      Array varData;
      if ((type == Type.JOIN_NEW) || (type == Type.FORECAST_MODEL_COLLECTION)) {
        varData = nested.read(mainv, cancelTask, innerSection);
      } else {
        nestedSection.set(0, nestedJoinRange);
        varData = nested.read(mainv, cancelTask, nestedSection);
      }

      if ((cancelTask != null) && cancelTask.isCancel())
        return null;

      Array.arraycopy(varData, 0, sectionData, destPos, (int) varData.getSize());
      destPos += varData.getSize();
    }

    return sectionData;
  }

  protected Array readAggCoord(Variable aggCoord, Section section, CancelTask cancelTask) throws IOException, InvalidRangeException {
    DataType dtype = aggCoord.getDataType();
    Array allData = Array.factory(dtype, section.getShape());
    IndexIterator result = allData.getIndexIterator();

    List<Range> ranges = section.getRanges();
    Range joinRange = section.getRange(0);
    List<Range> nestedSection = new ArrayList<Range>(ranges); // get copy
    List<Range> innerSection = ranges.subList(1, ranges.size());

    for (Dataset vnested : nestedDatasets) {
      Range nestedJoinRange = vnested.getNestedJoinRange(joinRange);
      if (nestedJoinRange == null)
        continue;
      if (debug)
        System.out.println("   agg use " + vnested.aggStart + ":" + vnested.aggEnd + " range= " + nestedJoinRange + " file " + vnested.getLocation());

      readAggCoord(aggCoord, cancelTask, vnested, dtype, result, nestedJoinRange, nestedSection, innerSection);

      if ((cancelTask != null) && cancelTask.isCancel())
        return null;
    }

    return allData;
  }

  // handle the case of cached agg coordinate variables
  private void readAggCoord(Variable aggCoord, CancelTask cancelTask, Dataset vnested, DataType dtype, IndexIterator result,
          Range nestedJoinRange, List<Range> nestedSection, List<Range> innerSection) throws IOException, InvalidRangeException {

    // we have the coordinates as a String
    if (vnested.coordValue != null) {

      // joinNew, fmrc only can have 1 coord
      if ((type == Type.JOIN_NEW) || (type == Type.JOIN_EXISTING_ONE) || (type == Type.FORECAST_MODEL_COLLECTION)) {
        if (dtype == DataType.STRING) {
          result.setObjectNext(vnested.coordValue);
        } else {
          double val = Double.parseDouble(vnested.coordValue);
          result.setDoubleNext(val);
        }

      } else {

        // joinExisting can have multiple coords
        int count = 0;
        StringTokenizer stoker = new StringTokenizer(vnested.coordValue, " ,");
        while (stoker.hasMoreTokens()) {
          String toke = stoker.nextToken();
          if ((nestedJoinRange != null) && !nestedJoinRange.contains(count))
            continue;

          if (dtype == DataType.STRING) {
            result.setObjectNext(toke);
          } else {
            double val = Double.parseDouble(toke);
            result.setDoubleNext(val);
          }
          count++;
        }

        if (count != vnested.ncoord)
          logger.error("readAggCoord incorrect number of coordinates dataset=" + vnested.location);
      }

    } else { // we gotta read it

      Array varData;
      if (nestedJoinRange == null) {  // all data
        varData = vnested.read(aggCoord, cancelTask);

      } else
      if ((type == Type.JOIN_NEW) || (type == Type.JOIN_EXISTING_ONE) || (type == Type.FORECAST_MODEL_COLLECTION)) {
        varData = vnested.read(aggCoord, cancelTask, innerSection);
      } else {
        nestedSection.set(0, nestedJoinRange);
        varData = vnested.read(aggCoord, cancelTask, nestedSection);
      }

      // copy it to the result
      MAMath.copy(dtype, varData.getIndexIterator(), result);
    }

  }

  //////////////////////////////////////////////////////////////////

  /**
   * Scan the directory(ies) and create nested Aggregation.Dataset objects.
   * Directories are scanned recursively, by calling File.listFiles().
   * Sort by date if it exists, else filename.
   *
   * @param result     add to this List objects of type Aggregation.Dataset
   * @param cancelTask allow user to cancel
   * @throws IOException if io error
   */
  protected void scan(List<Dataset> result, CancelTask cancelTask) throws IOException {

    // Directories are scanned recursively, by calling File.listFiles().
    List<MyFile> fileList = new ArrayList<MyFile>();
    for (DirectoryScan dir : scanList) {
      dir.scanDirectory(fileList, cancelTask);
      if ((cancelTask != null) && cancelTask.isCancel())
        return;
    }

    // extract date if possible, before sorting
    for (MyFile myf : fileList) {
      // optionally parse for date
      if (null != myf.dir.dateFormatMark) {
        String filename = myf.file.getName();
        myf.dateCoord = DateFromString.getDateUsingDemarkatedCount(filename, myf.dir.dateFormatMark, '#');
        myf.dateCoordS = formatter.toDateTimeStringISO(myf.dateCoord);
        if (debugScan) System.out.println("  adding " + myf.file.getAbsolutePath() + " date= " + myf.dateCoordS);
      } else {
        if (debugScan) System.out.println("  adding " + myf.file.getAbsolutePath());
      }
    }

    // Sort by date if it exists, else filename.
    Collections.sort(fileList, new Comparator<MyFile>() {
      public int compare(MyFile mf1, MyFile mf2) {
        if (isDate)
          return mf1.dateCoord.compareTo(mf2.dateCoord);
        else
          return mf1.file.getName().compareTo(mf2.file.getName());
      }
    });

    // now add the ordered list of Datasets to the result List
    for (MyFile myf : fileList) {
      String location = myf.file.getAbsolutePath();
      String coordValue = (type == Type.JOIN_NEW) || (type == Type.JOIN_EXISTING_ONE) || (type == Type.FORECAST_MODEL_COLLECTION) ? myf.dateCoordS : null;
      Dataset ds = makeDataset(location, location, null, coordValue, myf.dir.enhance, null);
      ds.coordValueDate = myf.dateCoord;
      result.add(ds);

      if ((cancelTask != null) && cancelTask.isCancel())
        return;
    }
  }

  /**
   * Dataset factory, so subclasses can override
   *
   * @param cacheName   a unique name to use for caching
   * @param location    attribute "location" on the netcdf element
   * @param ncoordS     attribute "ncoords" on the netcdf element
   * @param coordValueS attribute "coordValue" on the netcdf element
   * @param enhance     open dataset in enhance mode
   * @param reader      factory for reading this netcdf dataset
   * @return a Aggregation.Dataset
   */
  protected Dataset makeDataset(String cacheName, String location, String ncoordS, String coordValueS, boolean enhance, NetcdfFileFactory reader) {
    return new Dataset(cacheName, location, ncoordS, coordValueS, enhance, reader);
  }

  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * Encapsolate a "scan" or "scan2" element: a directory that we want to scan.
   */
  protected class DirectoryScan {
    String dirName, dateFormatMark;
    String runMatcher, forecastMatcher, offsetMatcher; // scan2
    boolean enhance = false;
    boolean wantSubdirs = true;

    // filters
    String suffix;
    java.util.regex.Pattern regexpPattern = null;
    long olderThan_msecs; // files must not have been modified for this amount of time (msecs)

    DirectoryScan(String dirName, String suffix, String regexpPatternString, String dateFormatMark, String enhanceS, String subdirsS, String olderS) {
      this.dirName = dirName;
      this.suffix = suffix;
      if (null != regexpPatternString)
        this.regexpPattern = java.util.regex.Pattern.compile(regexpPatternString);

      this.dateFormatMark = dateFormatMark;
      if ((enhanceS != null) && enhanceS.equalsIgnoreCase("true"))
        enhance = true;
      if ((subdirsS != null) && subdirsS.equalsIgnoreCase("false"))
        wantSubdirs = false;
      if (type == Type.FORECAST_MODEL_COLLECTION)
        enhance = true;

      if (olderS != null) {
        try {
          TimeUnit tu = new TimeUnit(olderS);
          this.olderThan_msecs = (long) (1000 * tu.getValueInSeconds());
        } catch (Exception e) {
          logger.error("Invalid time unit for olderThan = {}", olderS);
        }
      }
    }

    DirectoryScan(String dirName, String suffix, String regexpPatternString, String subdirsS, String olderS,
            String runMatcher, String forecastMatcher, String offsetMatcher) {
      this(dirName, suffix, regexpPatternString, null, "true", subdirsS, olderS);

      this.runMatcher = runMatcher;
      this.forecastMatcher = forecastMatcher;
      this.offsetMatcher = offsetMatcher;
    }

    /**
     * Recursively crawl directories, add matching MyFile files to result List
     *
     * @param result     add MyFile objects to this list
     * @param cancelTask user can cancel
     */
    protected void scanDirectory(List<MyFile> result, CancelTask cancelTask) {
      scanDirectory(dirName, new Date().getTime(), result, cancelTask);
    }

    protected void scanDirectory(String dirName, long now, List<MyFile> result, CancelTask cancelTask) {
      File allDir = new File(dirName);
      if (!allDir.exists()) {
        String tmpMsg = "Non-existent scan location <" + dirName + "> for aggregation <" + ncDataset.getLocation() + ">.";
        logger.error("scanDirectory(): " + tmpMsg);
        throw new IllegalArgumentException(tmpMsg);
      }
      for (File f : allDir.listFiles()) {
        String location = f.getAbsolutePath();

        if (f.isDirectory()) {
          if (wantSubdirs) scanDirectory(location, now, result, cancelTask);

        } else if (accept(location)) {
          // dont allow recently modified
          if (olderThan_msecs > 0) {
            long lastModified = f.lastModified();
            if (now - lastModified < olderThan_msecs)
              continue;
          }

          // add to result
          result.add(new MyFile(this, f));
        }

        if ((cancelTask != null) && cancelTask.isCancel())
          return;
      }
    }

    protected boolean accept(String location) {
      if (null != regexpPattern) {
        java.util.regex.Matcher matcher = regexpPattern.matcher(location);
        return matcher.matches();
      }

      return (suffix == null) || location.endsWith(suffix);
    }

  }

  /**
   * Encapsolate a file that was scanned.
   * Created in scanDirectory()
   */
  protected class MyFile {
    DirectoryScan dir;
    File file;

    Date dateCoord; // will have both or neither
    String dateCoordS;

    Date runDate; // fmrcHourly only
    Double offset;

    MyFile(DirectoryScan dir, File file) {
      this.dir = dir;
      this.file = file;
    }

    // MyFile with the same file are equal
    public boolean equals(Object oo) {
      if (this == oo) return true;
      if (!(oo instanceof MyFile)) return false;
      MyFile other = (MyFile) oo;
      return file.equals(other.file);
    }

    public int hashCode() {
      return file.hashCode();
    }
  }

  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * Encapsolates a NetcdfFile that is a component of the aggregation.
   * public for NcMLWriter
   */
  public class Dataset {
    private String location; // location attribute on the netcdf element
    private int aggStart = 0, aggEnd = 0; // index in aggregated dataset; aggStart <= i < aggEnd

    // deferred opening
    private String cacheName;
    private NetcdfFileFactory reader;
    private boolean enhance;

    protected int ncoord; // number of coordinates in outer dimension for this dataset; joinExisting
    protected String coordValue;  // if theres a coordValue on the netcdf element
    protected Date coordValueDate;  // if its a date
    private boolean isStringValued = false;

    /**
     * For subclasses.
     * @param location location attribute on the netcdf element
     */
    protected Dataset(String location) {
      this.location = (location == null) ? null : StringUtil.substitute(location, "\\", "/");
    }

    /**
     * Dataset constructor.
     * With this constructor, the actual opening of the dataset is deferred, and done by the reader.
     * Used with explicit netcdf elements, and scanned files.
     *
     * @param cacheName   a unique name to use for caching
     * @param location    attribute "location" on the netcdf element
     * @param ncoordS     attribute "ncoords" on the netcdf element
     * @param coordValueS attribute "coordValue" on the netcdf element
     * @param enhance     open dataset in enhance mode
     * @param reader      factory for reading this netcdf dataset; if null, use NetcdfDataset.open( location)
     */
    protected Dataset(String cacheName, String location, String ncoordS, String coordValueS, boolean enhance, NetcdfFileFactory reader) {
      this(location);
      this.cacheName = cacheName;
      this.coordValue = coordValueS;
      this.enhance = enhance;
      this.reader = (reader != null) ? reader : new PolymorphicReader();

      if ((type == Type.JOIN_NEW) || (type == Type.JOIN_EXISTING_ONE)) {
        this.ncoord = 1;
      } else if (ncoordS != null) {
        try {
          this.ncoord = Integer.parseInt(ncoordS);
        } catch (NumberFormatException e) {
          logger.error("bad ncoord attribute on dataset=" + location);
        }
      }

      if ((type == Type.JOIN_NEW) || (type == Type.JOIN_EXISTING_ONE) || (type == Type.FORECAST_MODEL_COLLECTION)) {
        if (coordValueS == null) {
          int pos = this.location.lastIndexOf("/");
          this.coordValue = (pos < 0) ? this.location : this.location.substring(pos + 1);
          this.isStringValued = true;
        } else {
          try {
            Double.parseDouble(coordValueS);
          } catch (NumberFormatException e) {
            this.isStringValued = true;
          }
        }
      }

      // allow coordValue attribute on JOIN_EXISTING, may be multiple values seperated by blanks or commas
      if ((type == Type.JOIN_EXISTING) && (coordValueS != null)) {
        StringTokenizer stoker = new StringTokenizer(coordValueS, " ,");
        this.ncoord = stoker.countTokens();
      }
    }

    /**
     * Get the coordinate value(s) as a String for this Dataset
     * @return the coordinate value(s) as a String
     */
    public String getCoordValueString() {
      return coordValue;
    }

    /**
     * Get the coordinate value as a Date for this Dataset; may be null
     * @return the coordinate value as a Date, or null
     */
    public Date getCoordValueDate() {
      return coordValueDate;
    }

    /**
     * Get the location of this Dataset
     * @return the location of this Dataset
     */
    public String getLocation() {
      return location;
    }

    /**
     * Get number of coordinates in this Dataset.
     * If not already set, open the file and get it from the aggregation dimension.
     * @param cancelTask allow cancellation
     * @return number of coordinates in this Dataset.
     * @throws java.io.IOException if io error
     */
    public int getNcoords(CancelTask cancelTask) throws IOException {
      if (ncoord <= 0) {
        NetcdfFile ncd = acquireFile(cancelTask);
        if ((cancelTask != null) && cancelTask.isCancel()) return 0;

        Dimension d = ncd.getRootGroup().findDimension(dimName);
        if (d != null)
          ncoord = d.getLength();
        ncd.close();
      }
      return ncoord;
    }

    /**
     * Set the starting and ending index into the aggregation dimension
     *
     * @param aggStart   starting index
     * @param cancelTask allow to bail out
     * @return number of coordinates in this dataset
     * @throws IOException if io error
     */
    private int setStartEnd(int aggStart, CancelTask cancelTask) throws IOException {
      this.aggStart = aggStart;
      this.aggEnd = aggStart + getNcoords(cancelTask);
      return ncoord;
    }

    /**
     * Get the desired Range, reletive to this Dataset, if no overlap, return null.
     * <p> wantStart, wantStop are the indices in the aggregated dataset, wantStart <= i < wantEnd.
     * if this overlaps, set the Range required for the nested dataset.
     * note this should handle strides ok.
     *
     * @param totalRange desired range, reletive to aggregated dimension.
     * @return desired Range or null if theres nothing wanted from this datase.
     * @throws InvalidRangeException if invalid range request
     */
    private Range getNestedJoinRange(Range totalRange) throws InvalidRangeException {
      int wantStart = totalRange.first();
      int wantStop = totalRange.last() + 1; // Range has last inclusive, we use last exclusive

      // see if this dataset is needed
      if (!isNeeded(wantStart, wantStop))
        return null;

      int firstInInterval = totalRange.getFirstInInterval(aggStart);
      if ((firstInInterval < 0) || (firstInInterval >= aggEnd))
        return null;

      int start = Math.max(aggStart, wantStart) - aggStart;
      int stop = Math.min(aggEnd, wantStop) - aggStart;

      return new Range(start, stop - 1, totalRange.stride()); // Range has last inclusive
    }

    protected boolean isNeeded(Range totalRange) {
      int wantStart = totalRange.first();
      int wantStop = totalRange.last() + 1; // Range has last inclusive, we use last exclusive
      return isNeeded(wantStart, wantStop);
    }

    // wantStart, wantStop are the indices in the aggregated dataset, wantStart <= i < wantEnd
    // find out if this overlaps this nested Dataset indices
    private boolean isNeeded(int wantStart, int wantStop) {
      if (wantStart >= wantStop)
        return false;
      if ((wantStart >= aggEnd) || (wantStop <= aggStart))
        return false;

      return true;
    }

    protected NetcdfFile acquireFile(CancelTask cancelTask) throws IOException {
      try {
        return _acquireFile(cancelTask);
      } catch (IOException ioe) {
        syncExtend(true); // LOOK data has changed, how to notify the user permanently?
        throw ioe;
      }
    }

    private NetcdfFile _acquireFile(CancelTask cancelTask) throws IOException {
      NetcdfFile ncfile;
      long start = System.currentTimeMillis();
      if (debugOpenFile) System.out.println(" try to acquire " + cacheName);
      if (enhance)
        ncfile = NetcdfDatasetCache.acquire(cacheName, -1, cancelTask, spiObject, (NetcdfDatasetFactory) reader);
      else
        ncfile = NetcdfFileCache.acquire(cacheName, -1, cancelTask, spiObject, reader);

      if (debugOpenFile) System.out.println(" acquire " + cacheName + " took " + (System.currentTimeMillis() - start));
      if (type == Type.JOIN_EXISTING)
        cacheCoordValues(ncfile);
      return ncfile;
    }

    protected void close() throws IOException {
    }

    private void cacheCoordValues(NetcdfFile ncfile) throws IOException {
      if (coordValue != null) return;

      Variable coordVar = ncfile.findVariable(dimName);
      if (coordVar != null) {
        Array data = coordVar.read();
        coordValue = data.toString();
      }
    }

    protected Array read(Variable mainv, CancelTask cancelTask) throws IOException {
      NetcdfFile ncd = null;
      try {
        ncd = acquireFile(cancelTask);

        if ((cancelTask != null) && cancelTask.isCancel())
          return null;

        Variable v = modifyVariable(ncd, mainv.getName());
        return v.read();

      } finally {
        if (ncd != null) ncd.close();
      }
    }

    protected Array read(Variable mainv, CancelTask cancelTask, List<Range> section) throws IOException, InvalidRangeException {
      NetcdfFile ncd = null;
      try {
        ncd = acquireFile(cancelTask);
        if ((cancelTask != null) && cancelTask.isCancel())
          return null;

        if (debugRead) {
          System.out.print("agg read " + ncd.getLocation() + " nested= " + getLocation());
          for (Range range : section)
            System.out.print(" " + range + ":");
          System.out.println("");
        }

        Variable v = modifyVariable(ncd, mainv.getName());

        // its possible that we are asking for more of the time coordinate than actually exists (fmrc ragged time)
        // so we need to read only what is there
        Range fullRange = v.getRanges().get(0);
        Range want = section.get(0);
        if (fullRange.last() < want.last()) {
          Range limitRange = new Range(want.first(), fullRange.last(), want.stride());
          section = new ArrayList<Range>(section); // make a copy
          section.set(0, limitRange);
        }

        return v.read(section);

      } finally {
        if (ncd != null) ncd.close();
      }
    }

    // Datasets with the same locations are equal
    public boolean equals(Object oo) {
      if (this == oo) return true;
      if (!(oo instanceof Dataset)) return false;
      Dataset other = (Dataset) oo;
      return location.equals(other.location);
    }

    public int hashCode() {
      return location.hashCode();
    }

    protected boolean checkOK(CancelTask cancelTask) throws IOException {
      return true;
    }

    // allow subclasses to override
    protected Variable modifyVariable(NetcdfFile ncfile, String name) throws IOException {
      return ncfile.findVariable(name);
    }

    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    class PolymorphicReader implements NetcdfFileFactory, NetcdfDatasetFactory {

      public NetcdfDataset openDataset(String location, int buffer_size, ucar.nc2.util.CancelTask cancelTask, Object spiObject) throws java.io.IOException {
        return NetcdfDataset.openDataset(location, true, buffer_size, cancelTask, spiObject);
      }

      public NetcdfFile open(String location, int buffer_size, ucar.nc2.util.CancelTask cancelTask, Object spiObject) throws IOException {
        return NetcdfDataset.openFile(location, buffer_size, cancelTask, spiObject);
      }
    }
  }

  protected class DatasetProxyReader implements ProxyReader {
    Dataset dataset;

    DatasetProxyReader(Dataset dataset) {
      this.dataset = dataset;
    }

    public Array read(Variable mainV, CancelTask cancelTask) throws IOException {
      NetcdfFile ncfile = null;
      try {
        ncfile = dataset.acquireFile(cancelTask);
        if ((cancelTask != null) && cancelTask.isCancel()) return null;
        Variable proxyV = ncfile.findVariable(mainV.getName());
        return proxyV.read();
      } finally {
        if (ncfile != null) ncfile.close();
      }
    }

    public Array read(Variable mainV, Section section, CancelTask cancelTask) throws IOException, InvalidRangeException {
      NetcdfFile ncfile = null;
      try {
        ncfile = dataset.acquireFile(cancelTask);
        Variable proxyV = ncfile.findVariable(mainV.getName());
        if ((cancelTask != null) && cancelTask.isCancel()) return null;
        return proxyV.read(section);
      } finally {
        if (ncfile != null) ncfile.close();
      }
    }
  }

}