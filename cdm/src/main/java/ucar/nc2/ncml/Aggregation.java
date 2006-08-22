// $Id: Aggregation.java 69 2006-07-13 00:12:58Z caron $
/*
 * Copyright 1997-2006 Unidata Program Center/University Corporation for
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
import ucar.nc2.dataset.conv._Coordinate;
import ucar.nc2.units.TimeUnit;
import ucar.nc2.units.DateFormatter;
import ucar.nc2.util.CancelTask;
import ucar.nc2.util.DiskCache2;
import ucar.unidata.util.StringUtil;

import java.util.*;
import java.io.*;
import java.nio.channels.FileChannel;
import java.nio.channels.OverlappingFileLockException;
import java.nio.channels.FileLock;

import thredds.util.DateFromString;
import org.jdom.Element;

/**
 * Implement NcML Aggregation.
 *
 * <h2>Implementation Notes</h2>
 * <h3>Caching</h3>
 * <ul>
 *  <li>Case 1. Explicit list / Scan static directories (recheck=null)
 *   <ul>
 *    <li>A. AggCaching - keep track of ncoords, coordValues for joinExisting. Read on open, write on close.
 *      Could skip scan if cache exists.
 *    <li>B. NetcdfFileCache - write on close if changed (only first time). On sync, recheck = null means wont be reread.
 *   </ul>
 *  <li>Case 2. Scan dynamic directories (recheck non-null)
 *   <ul>
 *    <li>A. AggCaching - keep track of ncoords, coordValues for joinExisting. Read on open, write on close.
 *      Could skip scan if cache exists, and recheck time not expired.
 *    <li>B. NetcdfFileCache - write on close if changed. On sync, if recheck time, then rescan.
 *   </ul>
 *  </ul>
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
 * @author caron
 * @version $Revision: 69 $ $Date: 2006-07-13 00:12:58Z $
 */
public class Aggregation implements ucar.nc2.dataset.ProxyReader {
  static protected org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(Aggregation.class);
  static protected DiskCache2 diskCache2 = null;

  static public void setPersistenceCache(DiskCache2 dc) {
    diskCache2 = dc;
  }

  //////////////////////////////////////////////////////////////////////////////////////////

  protected NetcdfDataset ncDataset; // the aggregation belongs to this dataset
  protected String dimName; // the aggregation dimension name
  private Type type; // the aggregation type
  protected ArrayList nestedDatasets; // working set of Aggregation.Dataset
  private int totalCoords = 0;  // the aggregation dimension size
  //private NetcdfFile typical = null; // metadata and non-agg variables come from a "typical" nested file.
  //private Dataset typicalDataset = null; // metadata and non-agg variables come from a "typical" nested file.
  protected Object spiObject;

  // explicit
  private ArrayList vars = new ArrayList(); // variable names (String)
  private ArrayList unionDatasets = new ArrayList(); // NetcdfDataset objects
  protected ArrayList explicitDatasets = new ArrayList(); // explicitly created Dataset objects from netcdf elements

  // joinExisting and JoinNew special handling
  //protected VariableDS joinAggCoord;

  // scan
  protected ArrayList scanList = new ArrayList(); // current set of Directory
  private TimeUnit recheck; // how often to rechecck
  protected long lastChecked; // last time checked
  protected boolean wasChanged = true; // something changed since last aggCache file was written
  private boolean isDate = false;  // has a dateFormatMark, so agg coordinate variable is a Date

  protected DateFormatter formatter = new DateFormatter();
  protected boolean debug = false, debugOpenFile = true, debugCacheDetail = false, debugSyncDetail = false, debugProxy = true;

  /**
   * Create an Aggregation for the NetcdfDataset.
   * The folloeing addXXXX methods are called, then finish(), before the object is ready for use.
   *
   * @param ncd      Aggregation belongs to this NetcdfDataset
   * @param dimName  the aggregation dimension name
   * @param typeName the Aggegation.Type name
   * @param recheckS how often to check if files have changes (secs)
   */
  public Aggregation(NetcdfDataset ncd, String dimName, String typeName, String recheckS) {
    this.ncDataset = ncd;
    this.dimName = dimName;
    this.type = Type.getType(typeName);

    if (recheckS != null) {
      try {
        this.recheck = new TimeUnit(recheckS);
      } catch (Exception e) {
        logger.error("Invalid time unit for recheckEvery = {}", recheckS);
      }
    }
  }

  /**
   * Add a nested dataset (other than a union), specified by an explicit netcdf ekement
   *
   * @param cacheName   a unique name to use for caching
   * @param location    attribute "location" on the netcdf element
   * @param ncoordS     attribute "ncoords" on the netcdf element
   * @param coordValueS attribute "coordValue" on the netcdf element
   * @param reader      factory for reading this netcdf dataset
   * @param cancelTask  user may cancel, may be null
   */
  public void addDataset(String cacheName, String location, String ncoordS, String coordValueS, NetcdfFileFactory reader, CancelTask cancelTask) {
    // boolean enhance = (enhanceS != null) && enhanceS.equalsIgnoreCase("true");
    Dataset nested = makeDataset(cacheName, location, ncoordS, coordValueS, false, reader);
    explicitDatasets.add(nested);
  }

  /**
   * Add a nested union dataset, which has been opened externally
   */
  public void addDatasetUnion(NetcdfDataset ds) {
    unionDatasets.add(ds);
  }

  /**
   * Add a scan elemnt
   *
   * @param dirName
   * @param suffix
   * @param dateFormatMark
   * @param enhance
   * @throws IOException
   */
  public void addDirectoryScan(String dirName, String suffix, String dateFormatMark, String enhance, String subdirs) throws IOException {
    Directory d = new Directory(dirName, suffix, dateFormatMark, enhance, subdirs);
    scanList.add(d);
    if (dateFormatMark != null)
      isDate = true;
  }

  /**
   * Add a variableAgg element
   *
   * @param varName
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

  public List getNestedDatasets() {
    return nestedDatasets;
  }

  public List getUnionDatasets() {
    return unionDatasets;
  }

  public Type getType() {
    return type;
  }

  public boolean isDate() {
    return isDate;
  }

  /**
   * Get the list of aggregation variables: variables whose data spans multiple files.
   */
  public List getVariables() {
    return vars;
  }

  /**
   * What is the data type of the aggregation coordinate ?
   */
  public DataType getCoordinateType() {
    Dataset first = (Dataset) nestedDatasets.get(0);
    return first.isStringValued ? DataType.STRING : DataType.DOUBLE;
  }

  /**
   * Release all resources associated with the aggregation
   *
   * @throws IOException
   */
  public void close() throws IOException {
    //if (null != typical)
    //  typical.close();

    for (int i = 0; i < unionDatasets.size(); i++) {
      NetcdfDataset ds = (NetcdfDataset) unionDatasets.get(i);
      ds.close();
    }

    persist();
  }

  /**
   * Persist info (nccords, coorValues) from joinExisting, since that can be expensive to recreate.
   * @throws IOException
   */
  public void persist() throws IOException {
    // optionally persist info from joinExisting scans, since that can be expensive to recreate
    if ((diskCache2 != null) && (type == Type.JOIN_EXISTING))
      persistWrite();
  }

  // name to use in the DiskCache2 for the persistent XML info.
  // Document root is aggregation
  // has the name getCacheName()
  private String getCacheName() {
    String cacheName = ncDataset.getLocation();
    if (cacheName == null) cacheName = ncDataset.getCacheName();
    return cacheName;
  }

  // write info to a persistent XML file, to save time next time
  // only for joinExisting
  private void persistWrite() throws IOException {
    FileChannel channel = null;
    try {
      String cacheName = getCacheName();
      if (cacheName == null) return;

      File cacheFile = diskCache2.getCacheFile(cacheName);
      boolean exists = cacheFile.exists();
      if (!exists) {
        File dir = cacheFile.getParentFile();
        dir.mkdirs();
      }

      // only write out if something changed after the cache file was last written
      if (!wasChanged)
        return;

      // Get a file channel for the file
      FileOutputStream fos = new FileOutputStream(cacheFile);
      channel = fos.getChannel();

      // Try acquiring the lock without blocking. This method returns
      // null or throws an exception if the file is already locked.
      FileLock lock;
      try {
        lock = channel.tryLock();
      } catch (OverlappingFileLockException e) {
        // File is already locked in this thread or virtual machine
        return; // give up
      }
      if (lock == null) return;

      PrintStream out = new PrintStream(fos);
      out.print("<?xml version='1.0' encoding='UTF-8'?>\n");
      out.print("<aggregation xmlns='http://www.unidata.ucar.edu/namespaces/netcdf/ncml-2.2' ");
      out.print("type='" + type + "' ");
      if (dimName != null)
        out.print("dimName='" + dimName + "' ");
      if (recheck != null)
        out.print("recheckEvery='" + recheck + "' ");
      out.print(">\n");

      for (int i = 0; i < nestedDatasets.size(); i++) {
        Dataset dataset = (Dataset) nestedDatasets.get(i);
        out.print("  <netcdf location='" + dataset.getLocation() + "' ");
        out.print("ncoords='" + dataset.getNcoords(null) + "' ");

        if (dataset.coordValue != null)
          out.print("coordValue='" + dataset.coordValue + "' ");

        out.print("/>\n");
      }

      out.print("</aggregation>\n");
      out.close(); // this also closes the  channel and releases the lock

      cacheFile.setLastModified(lastChecked);
      wasChanged = false;

      if (debug)
        System.out.println("Aggregation persisted = " + cacheFile.getPath() + " lastModified= " + new Date(lastChecked));

    } finally {
      if (channel != null)
        channel.close();
    }
  }

  // read info from the persistent XML file, if it exists
  private void persistRead() {
    String cacheName = getCacheName();
    if (cacheName == null) return;

    File cacheFile = diskCache2.getCacheFile(cacheName);
    if (!cacheFile.exists())
      return;

    if (debug) System.out.println(" *Read cache " + cacheName);

    Element aggElem;
    try {
      aggElem = NcMLReader.readAggregation(cacheFile.getPath());
    } catch (IOException e) {
      return;
    }

    List ncList = aggElem.getChildren("netcdf", NcMLReader.ncNS);
    for (int j = 0; j < ncList.size(); j++) {
      Element netcdfElemNested = (Element) ncList.get(j);
      String location = netcdfElemNested.getAttributeValue("location");
      Dataset ds = findDataset(location);
      if ((null != ds) && (ds.ncoord == 0)) {
        if (debugCacheDetail) System.out.println("  use cache for " + location);
        String ncoordsS = netcdfElemNested.getAttributeValue("ncoords");
        try {
          ds.ncoord = Integer.parseInt(ncoordsS);
        } catch (NumberFormatException e) {
        } // ignore

        String coordValue = netcdfElemNested.getAttributeValue("coordValue");
        if (coordValue != null) {
          ds.coordValue = coordValue;
        }

      }
    }

  }

  // find a dataset in the nestedDatasets by location
  private Dataset findDataset(String location) {
    for (int i = 0; i < nestedDatasets.size(); i++) {
      Dataset ds = (Dataset) nestedDatasets.get(i);
      if (location.equals(ds.getLocation()))
        return ds;
    }
    return null;
  }

  /**
   * Is the named variable an "aggregation variable" ?
   *
   * @param name
   */
  private boolean isAggVariable(String name) {
    for (int i = 0; i < vars.size(); i++) {
      String vname = (String) vars.get(i);
      if (vname.equals(name))
        return true;
    }
    return false;
  }

  ///////////////////////////////////////////////////////////////////////////////////////////////////////////

  // all elements are processed, finish construction
  public void finish(CancelTask cancelTask) throws IOException {
    nestedDatasets = new ArrayList();

    // LOOK fix from Michael Godin 3/14/06 - need to test
    //nestedDatasets.addAll(explicitDatasets);
    for (int i = 0; i < explicitDatasets.size(); i++) {
      Dataset dataset = (Dataset) explicitDatasets.get(i);
      if (dataset.checkOK(cancelTask))
        nestedDatasets.add(dataset);
    }

    if (scanList.size() > 0)
      scan(nestedDatasets, cancelTask);

    // check persistence info
    if ((diskCache2 != null) && (type == Type.JOIN_EXISTING))
      persistRead();

    buildCoords(cancelTask);

    if (getType() == Aggregation.Type.JOIN_NEW)
      aggNewDimension(true, ncDataset, cancelTask);
    else if (getType() == Aggregation.Type.JOIN_EXISTING)
      aggExistingDimension(true, ncDataset, cancelTask);
    else if (getType() == Aggregation.Type.FORECAST_MODEL)
      aggExistingDimension(true, ncDataset, cancelTask);

    this.lastChecked = System.currentTimeMillis();
    wasChanged = true;
  }

  protected void buildCoords(CancelTask cancelTask) throws IOException {

    if ((type == Type.FORECAST_MODEL) || (type == Type.FORECAST_MODEL_COLLECTION)) {
      for (int i = 0; i < nestedDatasets.size(); i++) {
        Dataset nested = (Dataset) nestedDatasets.get(i);
        nested.ncoord = 1;
      }
    }

    totalCoords = 0;
    for (int i = 0; i < nestedDatasets.size(); i++) {
      Dataset nested = (Dataset) nestedDatasets.get(i);
      totalCoords += nested.setStartEnd(totalCoords, cancelTask);
    }
  }

  // not ready yet
  public boolean syncExtend() throws IOException {
    if (scanList.size() == 0) return false;

    // rescan
    ArrayList newDatasets = new ArrayList();
    scan(newDatasets, null);

    // are there any new datasets?
    Dataset lastOld = (Dataset) nestedDatasets.get(nestedDatasets.size() - 1);
    int nextNew;
    for (nextNew = 0; nextNew < newDatasets.size(); nextNew++) {
      Dataset dataset = (Dataset) newDatasets.get(nextNew);
      if (dataset.location.equals(lastOld.location)) break;
    }
    nextNew++;
    if (nextNew >= newDatasets.size())
      return false;

    for (int i = nextNew; i < newDatasets.size(); i++) {
      Dataset newDataset = (Dataset) newDatasets.get(i);
      nestedDatasets.add(newDataset);
      totalCoords += newDataset.setStartEnd(totalCoords, null);
    }

    /* if (getType() == Aggregation.Type.JOIN_NEW)
      resetNewDimension();
    else if (getType() == Aggregation.Type.JOIN_EXISTING)
      resetAggDimensionLength();*/ // LOOK

    return true;
  }

  // sync if the recheckEvery time has passed
  public boolean sync() throws IOException {
    if (getType() == Aggregation.Type.UNION)
      return false;

    // see if we need to recheck
    if (recheck == null)
      return false;
    Date now = new Date();
    Date lastCheckedDate = new Date(lastChecked);
    Date need = recheck.add(lastCheckedDate);
    if (now.before(need)) {
      if (debug) System.out.println(" *Sync not needed, last= "+lastCheckedDate+" now = "+now);
      return false;
    }

    // ok were gonna recheck
    if (debug) System.out.println(" *Sync ");
    lastChecked = System.currentTimeMillis();

    // rescan
    ArrayList newDatasets = new ArrayList();
    scan(newDatasets, null);

    // replace with previous datasets if they exist
    boolean changed = false;
    for (int i = 0; i < newDatasets.size(); i++) {
      Dataset newDataset = (Dataset) newDatasets.get(i);
      int index = nestedDatasets.indexOf(newDataset);
      if (index >= 0) {
        newDatasets.set(i, nestedDatasets.get(index));
        if (debugSyncDetail) System.out.println("  sync using old Dataset= "+newDataset.location);
      } else {
        changed = true;
        if (debugSyncDetail) System.out.println("  sync found new Dataset= "+newDataset.location);
      }
    }

    // see if anything is changed
    if (!changed) return false;

    // recreate the list of datasets
    nestedDatasets = new ArrayList();
    nestedDatasets.addAll(explicitDatasets);
    nestedDatasets.addAll(newDatasets);
    buildCoords(null);

    /* chose a new typical dataset
    if (typical != null) {
      typical.close();
      typical = null;
      typicalDataset = null;
    } */
    //ncd.empty();

    // rebuild the metadata
    if (getType() == Aggregation.Type.JOIN_NEW)
      aggNewDimension(false, ncDataset, null);
    else if (getType() == Aggregation.Type.JOIN_EXISTING)
      aggExistingDimension(false, ncDataset, null);
    else if (getType() == Aggregation.Type.FORECAST_MODEL)
      aggExistingDimension(false, ncDataset, null);

    ncDataset.finish();

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

  /**
   * Open one of the nested datasets as a template for the aggregation dataset.
   */
  Dataset getTypicalDataset() throws IOException {
    //if (typical != null)
    //  return typical;

    int n = nestedDatasets.size();
    if (n == 0) return null;
    // pick a random one, but not the last
    int select = (n < 2) ? 0 : Math.abs(new Random().nextInt()) % (n-1);
    return (Dataset) nestedDatasets.get(select);
  }

  /**
   * Populate the dataset for a "JoinExisting" type.
   *
   * @param isNew
   * @param newds
   * @param cancelTask
   * @throws IOException
   */
  private void aggExistingDimension(boolean isNew, NetcdfDataset newds, CancelTask cancelTask) throws IOException {
    // open a "typical"  nested dataset and copy it to newds
    Dataset typicalDataset = getTypicalDataset();
    NetcdfFile typical =  typicalDataset.acquireFile(null);
    NcMLReader.transferDataset(typical, newds, isNew ? null : new MyReplaceVariableCheck());

    // create aggregation dimension
    String dimName = getDimensionName();
    Dimension aggDim = new Dimension(dimName, getTotalCoords(), true);
    newds.removeDimension(null, dimName); // remove previous declaration, if any
    newds.addDimension(null, aggDim);

    // now we can create the real aggExisting variables
    // all variables with the named aggregation dimension
    List vars = typical.getVariables();
    for (int i = 0; i < vars.size(); i++) {
      Variable v = (Variable) vars.get(i);
      if (v.getRank() < 1)
        continue;
      Dimension d = v.getDimension(0);
      if (!dimName.equals(d.getName()))
        continue;

      VariableDS vagg = new VariableDS(newds, null, null, v.getShortName(), v.getDataType(),
              v.getDimensionsString(), null, null);
      vagg.setProxyReader(this);
      NcMLReader.transferVariableAttributes(v, vagg);

      newds.removeVariable(null, v.getShortName());
      newds.addVariable(null, vagg);

      if (cancelTask != null && cancelTask.isCancel()) return;
    }

    newds.finish();
    makeProxies(typicalDataset, newds);
    typical.close();
  }

  protected void makeProxies(Dataset typicalDataset, NetcdfDataset newds) throws IOException {

    // all normal variables must use a proxy to lock the file
    DatasetProxyReader proxy = new DatasetProxyReader(typicalDataset);
    List allVars = newds.getVariables();
    for (int i = 0; i < allVars.size(); i++) {
      VariableDS vs = (VariableDS) allVars.get(i);
      if (vs.hasProxyReader()) {
        if (debugProxy) System.out.println(" debugProxy: hasProxyReader "+vs.getNameAndDimensions());
        continue; // dont mess with agg variables
      }

      if (vs.isCaching()) {  // cache the small ones
        if (!vs.hasCachedData()) {
          vs.read();
          if (debugProxy) System.out.println(" debugProxy: cached "+vs.getNameAndDimensions());
        } else {
          if (debugProxy) System.out.println(" debugProxy: aleady cached "+vs.getNameAndDimensions());
        }

      } else if (!vs.hasProxyReader()) { // put proxy on the rest
        vs.setProxyReader(proxy);
        if (debugProxy) System.out.println(" debugProxy: proxy on "+vs.getNameAndDimensions());
      }
    }


  }

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

  /**
   * Populate the dataset for a "JoinNew" type.
   *
   * @param isNew
   * @param newds
   * @param cancelTask
   * @throws IOException
   */
  private void aggNewDimension(boolean isNew, NetcdfDataset newds, CancelTask cancelTask) throws IOException {
    // open a "typical"  nested dataset and copy it to newds
    Dataset typicalDataset = getTypicalDataset();
    NetcdfFile typical =  typicalDataset.acquireFile(null);
    NcMLReader.transferDataset(typical, newds, isNew ? null : new MyReplaceVariableCheck());

    // create aggregation dimension
    String dimName = getDimensionName();
    Dimension aggDim = new Dimension(dimName, getTotalCoords(), true);
    newds.removeDimension(null, dimName); // remove previous declaration, if any
    newds.addDimension(null, aggDim);

    // create aggregation coordinate variable
    DataType coordType;
    VariableDS joinAggCoord = (VariableDS) newds.getRootGroup().findVariable(dimName);
    if (joinAggCoord == null) {
      coordType = getCoordinateType();
      joinAggCoord = new VariableDS(newds, null, null, dimName, coordType, dimName, null, null);
      newds.addVariable(null, joinAggCoord);
    } else {
      coordType = joinAggCoord.getDataType();
      joinAggCoord.setDimensions(dimName); // reset its dimension
      if (!isNew) joinAggCoord.setCachedData(null, false); // get rid of any cached data, since its now wrong
    }
    joinAggCoord.setProxyReader(this);

    if (isDate()) {
      joinAggCoord.addAttribute(new ucar.nc2.Attribute(_Coordinate.AxisType, "Time"));
    }

    // now we can create all the aggNew variables
    // use only named variables
    List vars = getVariables();
    for (int i = 0; i < vars.size(); i++) {
      String varname = (String) vars.get(i);
      Variable v = newds.getRootGroup().findVariable(varname);
      if (v == null) {
        logger.error(ncDataset.getLocation() + " aggNewDimension cant find variable " + varname);
        continue;
      }

      // construct new variable, replace old one
      VariableDS vagg = new VariableDS(newds, null, null, v.getShortName(), v.getDataType(),
              dimName + " " + v.getDimensionsString(), null, null);
      vagg.setProxyReader(this);
      NcMLReader.transferVariableAttributes(v, vagg);

      newds.removeVariable(null, v.getShortName());
      newds.addVariable(null, vagg);

      if (cancelTask != null && cancelTask.isCancel()) return;
    }

    newds.finish();
    makeProxies(typicalDataset, newds);
    typical.close();
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
    if (((type == Type.JOIN_NEW) || (type == Type.JOIN_EXISTING) || (type == Type.FORECAST_MODEL_COLLECTION)) && mainv.getShortName().equals(dimName))
      return readAggCoord(mainv, cancelTask);

    DataType dtype = (mainv instanceof VariableDS) ? ((VariableDS)mainv).getOriginalDataType() : mainv.getDataType();
    Array allData = Array.factory(dtype, mainv.getShape()); // LOOK why getOriginalDataType() ?
    int destPos = 0;

    Iterator iter = nestedDatasets.iterator();
    while (iter.hasNext()) {
      Dataset vnested = (Dataset) iter.next();
      Array varData = vnested.read(mainv, cancelTask);
      if ((cancelTask != null) && cancelTask.isCancel())
        return null;

      Array.arraycopy(varData, 0, allData, destPos, (int) varData.getSize());
      destPos += varData.getSize();
    }

    return allData;
  }

  private Array readAggCoord(Variable aggCoord, CancelTask cancelTask) throws IOException {
    DataType dtype = aggCoord.getDataType();
    Array allData = Array.factory(dtype, aggCoord.getShape());
    IndexIterator result = allData.getIndexIterator();

    Iterator iter = nestedDatasets.iterator();
    while (iter.hasNext()) {
      Dataset vnested = (Dataset) iter.next();

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
  public Array read(Variable mainv, CancelTask cancelTask, List section) throws IOException, InvalidRangeException {
    // If its full sized, then use full read, so that data gets cached.
    long size = Range.computeSize( section);
    if (size == mainv.getSize())
      return read( mainv, cancelTask);

    // the case of the agg coordinate var for joinExisting or joinNew
    if (((type == Type.JOIN_NEW) || (type == Type.JOIN_EXISTING) || (type == Type.FORECAST_MODEL_COLLECTION)) && mainv.getShortName().equals(dimName))
      return readAggCoord(mainv, cancelTask, section);

    DataType dtype = (mainv instanceof VariableDS) ? ((VariableDS)mainv).getOriginalDataType() : mainv.getDataType();
    Array sectionData = Array.factory(dtype, Range.getShape(section));
    int destPos = 0;

    Range joinRange = (Range) section.get(0);
    List nestedSection = new ArrayList(section); // copy
    List innerSection = section.subList(1, section.size());

    if (debug) System.out.println("   agg wants range=" + mainv.getName()+"("+joinRange+")");

    Iterator iter = nestedDatasets.iterator();
    while (iter.hasNext()) {
      Dataset nested = (Dataset) iter.next();
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

  private Array readAggCoord(Variable aggCoord, CancelTask cancelTask, List section) throws IOException, InvalidRangeException {
    DataType dtype = aggCoord.getDataType();
    Array allData = Array.factory(dtype, Range.getShape(section));
    IndexIterator result = allData.getIndexIterator();

    Range joinRange = (Range) section.get(0);
    List nestedSection = new ArrayList(section); // copy
    List innerSection = section.subList(1, section.size());

    Iterator iter = nestedDatasets.iterator();
    while (iter.hasNext()) {
      Dataset vnested = (Dataset) iter.next();
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
          Range nestedJoinRange, List nestedSection, List innerSection) throws IOException, InvalidRangeException {

    // we have the coordinates as a String
    if (vnested.coordValue != null) {

      // joinNew, fmrc only can have 1 coord
      if ((type == Type.JOIN_NEW) || (type == Type.FORECAST_MODEL_COLLECTION)) {
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

      } else if ((type == Type.JOIN_NEW) || (type == Type.FORECAST_MODEL_COLLECTION)) {
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
   * @throws IOException
   */
  protected void scan(List result, CancelTask cancelTask) throws IOException {
    List fileList = getFileList(cancelTask);
    if ((cancelTask != null) && cancelTask.isCancel())
      return;

    for (int i = 0; i < fileList.size(); i++) {
      MyFile myf = (MyFile) fileList.get(i);
      String location = myf.file.getAbsolutePath();
      String coordValue = (type == Type.JOIN_NEW) || (type == Type.FORECAST_MODEL_COLLECTION) ? myf.dateCoordS : null;
      myf.nested = makeDataset(location, location, null, coordValue, myf.enhance, null);
      // if (myf.nested.checkOK(cancelTask))
      result.add(myf.nested);

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

  /**
   * Do all the directory scans that were specified.
   * Directories are scanned recursively, by calling File.listFiles().
   * Sort by date if it exists, else filename.
   *
   * @param cancelTask optional canel
   * @return sorted list of MyFile objects.
   */
  private List getFileList(CancelTask cancelTask) {
    ArrayList fileList = new ArrayList();
    for (int i = 0; i < scanList.size(); i++) {
      Directory d = (Directory) scanList.get(i);
      crawlDirectory(d.dirName, d.suffix, d.dateFormatMark, d.enhance, d.subdirs, fileList, cancelTask);

      if ((cancelTask != null) && cancelTask.isCancel())
        return null;
    }

    Collections.sort(fileList, new Comparator() {
      public int compare(Object o1, Object o2) {
        MyFile mf1 = (MyFile) o1;
        MyFile mf2 = (MyFile) o2;
        if (isDate)
          return mf1.dateCoord.compareTo(mf2.dateCoord);
        else
          return mf1.file.getName().compareTo(mf2.file.getName());
      }
    });

    return fileList;
  }

  /**
   * Recursively crawl directories, add matching MyFile files to result List
   *
   * @param dirName        crawl this directory
   * @param suffix         filter with this file suffix
   * @param dateFormatMark extract date from filename
   * @param enhance        open in enhanced mode?
   * @param result         add MyFile objects to this list
   * @param cancelTask     user can cancel
   */
  private void crawlDirectory(String dirName, String suffix, String dateFormatMark, boolean enhance, boolean subdirs, List result, CancelTask cancelTask) {
    File allDir = new File(dirName);
    File[] allFiles = allDir.listFiles();
    if (debug) System.out.println(" NcML Aggregation crawlDirectory");
    for (int i = 0; i < allFiles.length; i++) {
      File f = allFiles[i];
      String location = f.getAbsolutePath();

      if (f.isDirectory() && subdirs)
        crawlDirectory(location, suffix, dateFormatMark, enhance, subdirs, result, cancelTask);
      else if (location.endsWith(suffix)) { // filter

        // optionally parse for date
        Date dateCoord = null;
        String dateCoordS = null;
        if (null != dateFormatMark) {
          String filename = f.getName();
          dateCoord = DateFromString.getDateUsingDemarkatedDateFormat(filename, dateFormatMark, '#');
          dateCoordS = formatter.toDateTimeStringISO(dateCoord);
          if (debug) System.out.println("  adding " + location + " date= " + dateCoordS);
        } else {
          if (debug) System.out.println("  adding " + location);
        }

        result.add(new MyFile(f, dateCoord, dateCoordS, enhance));
      }

      if ((cancelTask != null) && cancelTask.isCancel())
        return;
    }
  }

  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * Encapsolate a "scan" element: a directory that we want to scan.
   */
  private class Directory {
    String dirName, suffix, dateFormatMark;
    boolean enhance = false;
    boolean subdirs = true;

    Directory(String dirName, String suffix, String dateFormatMark, String enhanceS, String subdirsS) {
      this.dirName = dirName;
      this.suffix = suffix;
      this.dateFormatMark = dateFormatMark;
      if ((enhanceS != null) && enhanceS.equalsIgnoreCase("true"))
        enhance = true;
      if ((subdirsS != null) && subdirsS.equalsIgnoreCase("false"))
        subdirs = false;
      if (type == Type.FORECAST_MODEL_COLLECTION)
        enhance = true;
    }
  }

  /**
   * Encapsolate a file that was scanned.
   * Created in crawlDirectory()
   */
  private class MyFile {
    File file;
    Date dateCoord; // will have both or neither
    String dateCoordS;
    boolean enhance;
    Dataset nested;

    MyFile(File file, Date dateCoord, String dateCoordS, boolean enhance) {
      this.file = file;
      this.dateCoord = dateCoord;
      this.dateCoordS = dateCoordS;
      this.enhance = enhance;
    }
  }

  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * Encapsolates a NetcdfFile that is a component of the aggregation.
   * public for NcMLWriter
   */
  public class Dataset {
    private String cacheName, location;
    private int ncoord; // n coordinates in outer dimension for this dataset; joinExisting
    private boolean enhance;
    private NetcdfFileFactory reader;
    private String coordValue;  // if theres a coordValue on the netcdf element

    //private double coordValueDouble; // if numeric valued coordinate
    // private Date coordValueDate; // if date coordinate
    //private String coordValueString; // if String valued
    //private String coordValuesExisting; // cached coordinate values  (joinExisting)

    private boolean isStringValued = false;
    private int aggStart = 0, aggEnd = 0; // index in aggregated dataset; aggStart <= i < aggEnd

    /**
     * Dataset constructor for joinNew or joinExisting.
     * Actually opening the dataset is deferred.
     *
     * @param cacheName   a unique name to use for caching
     * @param location    attribute "location" on the netcdf element
     * @param ncoordS     attribute "ncoords" on the netcdf element
     * @param coordValueS attribute "coordValue" on the netcdf element
     * @param enhance     open dataset in enhance mode
     * @param reader      factory for reading this netcdf dataset
     */
    protected Dataset(String cacheName, String location, String ncoordS, String coordValueS, boolean enhance, NetcdfFileFactory reader) {
      this.cacheName = cacheName;
      this.location = StringUtil.substitute(location, "\\", "/");
      this.coordValue = coordValueS;
      this.enhance = enhance;
      this.reader = (reader != null) ? reader : new PolymorphicReader();

      if (type == Type.JOIN_NEW) {
        this.ncoord = 1;
      } else if (ncoordS != null) {
        try {
          this.ncoord = Integer.parseInt(ncoordS);
        } catch (NumberFormatException e) {
          logger.error("bad ncoord attribute on dataset=" + location);
        }
      }

      if ((type == Type.JOIN_NEW) || (type == Type.FORECAST_MODEL_COLLECTION)) {
        if (coordValue == null) {
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

      /*  this.coordValueS = coordValueS;

      if (coordValueS == null) {
        int pos = this.location.lastIndexOf("/");
        this.coordValueS = (pos < 0) ? this.location : this.location.substring(pos + 1);
        this.isStringValued = true;
      } else {
        // LOOK see if its an ISO date ??
        try {
          this.coordValue = Double.parseDouble(coordValueS);
        } catch (NumberFormatException e) {
          // logger.error("bad coordValue attribute ("+ coordValueS +") on dataset "+location);
          this.isStringValued = true;
        }
      }

    } else if (ncoordS != null) {
      try {
        this.ncoord = Integer.parseInt(ncoordS);
      } catch (NumberFormatException e) {
        logger.error("bad ncoord attribute on dataset " + location);
      }
    }

    if (type == Type.JOIN_EXISTING) {
      this.coordValuesExisting = coordValueS;
    }  */


    }

    /* Set the coordinate value for this Dataset
    public void setCoordValue(double coordValue) {
      this.coordValue = coordValue;
      this.isStringValued = false;
    }

    /** Get the coordinate value for this Dataset
    public double getCoordValue() {
      return coordValue;
    }

    /** Set the coordinate value(s) as a String for this Dataset
    public void setCoordValueString(String coordValueS) {
      this.coordValueS = coordValueS;
      this.isStringValued = true;
    }

    /**
     * Set the coordinate value from a string. May be:
     * <ol>
     * <li> ISO Date format
     * <li> udunit date format
     * <li> double
     * <li> else leave as a String
     * </ol>
     * @param s
     *
    public void setCoordValue(String s) {
     Date d = DateUnit.getStandardOrISO( String text)
     if (d != null)

             // LOOK see if its an ISO date ??
         try {
           this.coordValue = Double.parseDouble(coordValueS);
         } catch (NumberFormatException e) {
           // logger.error("bad coordValue attribute ("+ coordValueS +") on dataset "+location);
           this.isStringValued = true;
         }
  }

    /** Get the coordinate value(s) as a String for this Dataset */
    public String getCoordValueString() {
      return coordValue;
    }

    /**
     * Get the location of this Dataset
     */
    public String getLocation() {
      return location;
    }

    /**
     * Get the desired Range, reletive to this Dataset, if no overlap, return null.
     * <p> wantStart, wantStop are the indices in the aggregated dataset, wantStart <= i < wantEnd.
     * if this overlaps, set the Range required for the nested dataset.
     * note this should handle strides ok.
     * @param totalRange desired range, reletive to aggregated dimension.
     * @return desired Range or null if theres nothing wanted from this datase.
     * @throws InvalidRangeException
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
      /* if (typicalDataset == this) {
        if (debugOpenFile) System.out.println(" acquire typical " + cacheName);
        return typical;
      } */

      NetcdfFile ncfile;
      if (enhance)
        ncfile = NetcdfDatasetCache.acquire(cacheName, -1, cancelTask, spiObject, (NetcdfDatasetFactory) reader);
      else
        ncfile = NetcdfFileCache.acquire(cacheName, -1, cancelTask, spiObject, reader);

      if (debugOpenFile) System.out.println(" acquire " + cacheName);
      if ((type == Type.JOIN_EXISTING) || (type == Type.FORECAST_MODEL))
        cacheCoordValues(ncfile);
      return ncfile;
    }


   /* protected void releaseFile(NetcdfFile ncfile) throws IOException {
      if (typicalDataset != this)
        ncfile.close();
    } */

    /* called only by the "typical" dataset
    private NetcdfFile openFile(CancelTask cancelTask) throws IOException {
      NetcdfFile ncfile;
      if (enhance)
        ncfile = NetcdfDataset.openDataset(location, true, -1, cancelTask, spiObject);
      else
        ncfile = NetcdfDataset.openFile(location, -1, cancelTask, spiObject);

      if (debugOpenFile) System.out.println(" open " + location);

      if ((type == Type.JOIN_EXISTING) || (type == Type.FORECAST_MODEL))
        cacheCoordValues(ncfile);
      return ncfile;
    } */

    private void cacheCoordValues(NetcdfFile ncfile) throws IOException {
      if (coordValue != null) return;

      Variable coordVar = ncfile.findVariable(dimName);
      if (coordVar != null) {
        Array data = coordVar.read();
        coordValue = data.toString();
      }
    }

    /**
     * Get number of coordinates in this Dataset.
     * If not already set, open the file and get it from the aggregation dimension.
     */
    public int getNcoords(CancelTask cancelTask) throws IOException {
      if (ncoord <= 0) {
        NetcdfFile ncd = acquireFile(cancelTask);
        if ((cancelTask != null) && cancelTask.isCancel())
          return 0;

        Dimension d = ncd.getRootGroup().findDimension(dimName);
        if (d != null)
          ncoord = d.getLength();
        ncd.close();
      }
      return ncoord;
    }


    private int setStartEnd(int aggStart, CancelTask cancelTask) throws IOException {
      this.aggStart = aggStart;
      this.aggEnd = aggStart + getNcoords(cancelTask);
      return ncoord;
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

    private Array read(Variable mainv, CancelTask cancelTask, List section) throws IOException, InvalidRangeException {
      NetcdfFile ncd = null;
      try {
        ncd = acquireFile(cancelTask);
        if ((cancelTask != null) && cancelTask.isCancel())
          return null;
        if (debug) {
          System.out.print("agg read " + ncd.getLocation() + " nested= " + getLocation());
          for (int i = 0; i < section.size(); i++) {
            Range range = (Range) section.get(i);
            System.out.print(" " + range + ":");
          }
          System.out.println("");
        }

        Variable v = modifyVariable(ncd, mainv.getName());
        return v.read(section);

      } finally {
        if (ncd != null) ncd.close();
      }
    }

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

      public NetcdfDataset openDataset(String cacheName, int buffer_size, ucar.nc2.util.CancelTask cancelTask, Object spiObject) throws java.io.IOException {
        return NetcdfDataset.openDataset(location, true, buffer_size, cancelTask, spiObject);
      }

      public NetcdfFile open(String location, int buffer_size, ucar.nc2.util.CancelTask cancelTask, Object spiObject) throws IOException {
        return NetcdfDataset.openFile(location, buffer_size, cancelTask, spiObject);
      }
    }
  }

  private class DatasetProxyReader implements ProxyReader {
    Dataset dataset;
    DatasetProxyReader( Dataset dataset) {
      this.dataset = dataset;
    }

    public Array read(Variable mainv, CancelTask cancelTask) throws IOException{
      NetcdfFile ncfile = null;
      try {
        ncfile = dataset.acquireFile( cancelTask);
        if ((cancelTask != null) && cancelTask.isCancel()) return null;
        return mainv.read();
      } finally {
        if (ncfile != null) ncfile.close();
      }
    }

    public Array read(Variable mainv, CancelTask cancelTask, List section) throws IOException, InvalidRangeException {
      NetcdfFile ncfile = null;
      try {
        ncfile = dataset.acquireFile( cancelTask);
        if ((cancelTask != null) && cancelTask.isCancel()) return null;
        return mainv.read(section);
      } finally {
        if (ncfile != null) ncfile.close();
      }
    }
  }

  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  public static class Type {
    private static ArrayList members = new ArrayList(20);

    public final static Type JOIN_EXISTING = new Type("joinExisting");
    public final static Type JOIN_NEW = new Type("joinNew");
    public final static Type UNION = new Type("union");
    public final static Type FORECAST_MODEL = new Type("forecastModelRun");
    public final static Type FORECAST_MODEL_COLLECTION = new Type("forecastModelRunCollection");

    private String name;

    public Type(String s) {
      this.name = s;
      members.add(this);
    }

    public static Collection getAllTypes() {
      return members;
    }

    /**
     * Find the CollectionType that matches this name, ignore case.
     *
     * @param name : match this name
     * @return CollectionType or null if no match.
     */
    public static Type getType(String name) {
      if (name == null) return null;
      for (int i = 0; i < members.size(); i++) {
        Type m = (Type) members.get(i);
        if (m.name.equalsIgnoreCase(name))
          return m;
      }
      return null;
    }

    /**
     * @return the string name.
     */
    public String toString() {
      return name;
    }

    /**
     * Override Object.hashCode() to be consistent with this equals.
     */
    public int hashCode() {
      return name.hashCode();
    }

    /**
     * CollectionType with same name are equal.
     */
    public boolean equals(Object o) {
      if (this == o) return true;
      if (!(o instanceof Type)) return false;
      return o.hashCode() == this.hashCode();
    }
  }

}