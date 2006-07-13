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
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.VariableDS;
import ucar.nc2.dataset.NetcdfDatasetCache;
import ucar.nc2.dataset.NetcdfDatasetFactory;
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
 * @author caron
 * @version $Revision: 69 $ $Date: 2006-07-13 00:12:58Z $
 */
public class Aggregation {
  static protected org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(Aggregation.class);
  static protected DiskCache2 diskCache2 = null;

  static public void setPersistenceCache(DiskCache2 dc) {
    diskCache2 = dc;
  }

  //////////////////////////////////////////////////////////////////////////////////////////

  protected NetcdfDataset ncd;
  protected String dimName;
  private Type type;
  protected ArrayList nestedDatasets; // working set of Aggregation.Dataset
  private int totalCoords = 0;
  private NetcdfFile typical = null; // metadata and non-agg variables come from a "typical" nested file.
  private Dataset typicalDataset = null; // metadata and non-agg variables come from a "typical" nested file.

  // explicit
  private ArrayList vars = new ArrayList(); // variable names (String)
  private ArrayList unionDatasets = new ArrayList(); // NetcdfDataset objects
  protected ArrayList explicitDatasets = new ArrayList(); // explicitly created Dataset objects from netcdf elements

  // scan
  protected ArrayList scanList = new ArrayList(); // current set of Directory
  private TimeUnit recheck;
  protected long lastChecked;
  private boolean isDate = false;

  protected DateFormatter formatter = new DateFormatter();
  protected boolean debug = false;

  /**
   * Create an Aggregation for the NetcdfDataset.
   * The folloeing addXXXX methods are called, then finish(), before the object is ready for use.
   *
   * @param ncd Aggregation belongs to this NetcdfDataset
   * @param dimName the aggregation dimension name
   * @param typeName the Aggegation.Type name
   * @param recheckS how often to check if files have changes (secs)
   */
  public Aggregation(NetcdfDataset ncd, String dimName, String typeName, String recheckS) {
    this.ncd = ncd;
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
   * @param cacheName
   * @param location
   * @param ncoordS
   * @param coordValue
   * @param reader
   * @param cancelTask
   */
  public void addDataset(String cacheName, String location, String ncoordS, String coordValue, NetcdfFileFactory reader, CancelTask cancelTask) {
    // boolean enhance = (enhanceS != null) && enhanceS.equalsIgnoreCase("true");
    Dataset nested = makeDataset(cacheName, location, ncoordS, coordValue, false, reader);
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
   * @param dirName
   * @param suffix
   * @param dateFormatMark
   * @param enhance
   * @throws IOException
   */
  public void addDirectoryScan(String dirName, String suffix, String dateFormatMark, String enhance) throws IOException {
    Directory d = new Directory(dirName, suffix, dateFormatMark, enhance);
    scanList.add(d);
    if (dateFormatMark != null)
      isDate = true;
  }

  /**
   * Add a variableAgg element
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
   * @throws IOException
   */
  public void close() throws IOException {
    if (null != typical)
      typical.close();

    for (int i = 0; i < unionDatasets.size(); i++) {
      NetcdfDataset ds = (NetcdfDataset) unionDatasets.get(i);
      ds.close();
    }

    // optionally persist info from joinExisting scans, since that can be expensive to recreate
    if ((diskCache2 != null) && (type == Type.JOIN_EXISTING) && (scanList.size() > 0))
      persistWrite();
  }

  // name to use in the DiskCache2 for the persistent XML info.
  // Document root is aggregation
  // has the name getCacheName()
  private String getCacheName() {
    String cacheName = ncd.getLocation();
    if (cacheName == null) cacheName = ncd.getCacheName();
    return cacheName;
  }

  // write info to a persistent XML file, to save time next time
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

      // Get a file channel for the file
      RandomAccessFile raf = new RandomAccessFile(cacheFile, "rw");
      channel = raf.getChannel();

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

      // only write out if we scanned after the file was last modified
      long lastModified = cacheFile.lastModified();
      if (exists && (lastModified >= lastChecked))
        return;

      channel.close();
      channel = null;

      PrintStream out = new PrintStream(new FileOutputStream(cacheFile));
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

        if (type == Type.JOIN_NEW) {
          if (dataset.coordValueS != null)
            out.print("coordValue='" + dataset.coordValueS + "' ");
        } else if (type == Type.JOIN_EXISTING) {
          if (dataset.coordValuesExisting != null)
            out.print("coordValues='" + dataset.coordValuesExisting + "' ");
        }

        out.print("/>\n");
      }

      out.print("</aggregation>\n");
      out.close();
      cacheFile.setLastModified(lastChecked);

      // Release the lock
      lock.release();
      if (debug) System.out.println("Aggregation persisted = " + cacheName + " lastModified= " + new Date(lastChecked));

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
        String ncoordsS = netcdfElemNested.getAttributeValue("ncoords");
        try {
          ds.ncoord = Integer.parseInt(ncoordsS);
          if (debug) System.out.println(" use persistent ncoords for " + location);
        } catch (NumberFormatException e) {
        } // ignore
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

    buildCoords( cancelTask);

    if (getType() == Aggregation.Type.JOIN_NEW)
      aggNewDimension( true, ncd, cancelTask);
    else if (getType() == Aggregation.Type.JOIN_EXISTING)
      aggExistingDimension( true, ncd, cancelTask);
    else if (getType() == Aggregation.Type.FORECAST_MODEL)
      aggExistingDimension( true, ncd, cancelTask);

    this.lastChecked = System.currentTimeMillis();
  }

  protected void buildCoords(CancelTask cancelTask) throws IOException {

    if ((type == Type.FORECAST_MODEL) || (type == Type.FORECAST_MODEL_COLLECTION)) {
      for (int i = 0; i < nestedDatasets.size() ; i++) {
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
    if (now.before(need))
      return false;

    // ok were gonna recheck
    lastChecked = System.currentTimeMillis();

    // rescan
    ArrayList newDatasets = new ArrayList();
    scan(newDatasets, null);

    // replace with previous datasets if they exist
    boolean wasChanged = false;
    for (int i = 0; i < newDatasets.size(); i++) {
      Dataset newDataset = (Dataset) newDatasets.get(i);
      int index = nestedDatasets.indexOf(newDataset);
      if (index >= 0) {
        newDatasets.set(i, nestedDatasets.get(index));
        logger.debug("Agg.sync oldDataset= {}", newDataset.location);
      } else {
        wasChanged = true;
        logger.debug("Agg.sync newDataset= {}", newDataset.location);
      }
    }

    // see if anything is changed
    if (!wasChanged) return false;

    // recreate the list of datasets
    nestedDatasets = new ArrayList();
    nestedDatasets.addAll(explicitDatasets);
    nestedDatasets.addAll(newDatasets);
    buildCoords( null);

    // chose a new typical dataset
    if (typical != null) {
      typical.close();
      typical = null;
      typicalDataset = null;
    }
    //ncd.empty();

    // rebuild the metadata
    if (getType() == Aggregation.Type.JOIN_NEW)
      aggNewDimension( false, ncd, null);
    else if (getType() == Aggregation.Type.JOIN_EXISTING)
      aggExistingDimension( false, ncd, null);
    else if (getType() == Aggregation.Type.FORECAST_MODEL)
      aggExistingDimension( false, ncd, null);

    ncd.finish();

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
  NetcdfFile getTypicalDataset() throws IOException {
    if (typical != null)
      return typical;

    // pick the last one, to minimize possibility of it being deleted
    int n = nestedDatasets.size();
    typicalDataset = (Dataset) nestedDatasets.get(n - 1);

    // open instead of acquiring it
    typical = typicalDataset.openFile(null);
    return typical;
  }

  /**
   * Populate the dataset for a "JoinExisting" type.
   *
   * @param isNew
   * @param newds
   * @param cancelTask
   * @throws IOException
   */
  private void aggExistingDimension( boolean isNew, NetcdfDataset newds, CancelTask cancelTask) throws IOException {
    // open a "typical"  nested dataset and copy it to newds
    NetcdfFile typical = getTypicalDataset();
    NcMLReader.transferDataset(typical, newds, isNew ? null : new MyReplaceVariableCheck());

    // create aggregation dimension
    String dimName = getDimensionName();
    Dimension aggDim = new Dimension(dimName, getTotalCoords(), true);
    newds.removeDimension( null, dimName); // remove previous declaration, if any
    newds.addDimension( null, aggDim);

    // now we can create the real aggExisting variables
    // all variables with the named aggregation dimension
    List vars = typical.getVariables();
    for (int i=0; i<vars.size(); i++) {
      Variable v = (Variable) vars.get(i);
      if (v.getRank() < 1)
        continue;
      Dimension d = v.getDimension(0);
      if (!dimName.equals(d.getName()))
        continue;

      VariableDS vagg = new VariableDS(newds, null, null, v.getShortName(), v.getDataType(),
          v.getDimensionsString(), null, null);
      vagg.setAggregation( this);
      NcMLReader.transferVariableAttributes( v, vagg);

      newds.removeVariable( null, v.getShortName());
      newds.addVariable( null, vagg);

      if (cancelTask != null && cancelTask.isCancel()) return;
    }
  }

  protected class MyReplaceVariableCheck implements ReplaceVariableCheck {
    public boolean replace(Variable v) {
      // needs to be replaced if its not an agg variable

      if (getType() == Type.JOIN_NEW) {
        return isAggVariable( v.getName());
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
  private void aggNewDimension( boolean isNew, NetcdfDataset newds, CancelTask cancelTask) throws IOException {
    // open a "typical"  nested dataset and copy it to newds
    NetcdfFile typical = getTypicalDataset();
    NcMLReader.transferDataset(typical, newds, isNew ? null : new MyReplaceVariableCheck());

    // create aggregation dimension
    String dimName = getDimensionName();
    Dimension aggDim = new Dimension( dimName, getTotalCoords(), true);
    newds.removeDimension( null, dimName); // remove previous declaration, if any
    newds.addDimension( null, aggDim);

    // create aggregation coordinate variable
    DataType coordType;
    Variable coordVar = newds.getRootGroup().findVariable(dimName);
    if (coordVar == null) {
      coordType = getCoordinateType();
      coordVar = new VariableDS(newds, null, null, dimName, coordType, dimName, null, null);
      newds.addVariable(null, coordVar);
    } else {
      coordType = coordVar.getDataType();
      coordVar.setDimensions(dimName); // reset its dimension
      if (!isNew) coordVar.setCachedData(null, false); // get rid of any cached data, since its now wrong
    }

    // if not already set, set its values
    if (!coordVar.hasCachedData()) {
      int[] shape = new int[] { getTotalCoords() };
      Array coordData = Array.factory(coordType.getPrimitiveClassType(), shape);
      Index ima = coordData.getIndex();
      List nestedDataset = getNestedDatasets();
      for (int i = 0; i < nestedDataset.size(); i++) {
        Aggregation.Dataset nested = (Aggregation.Dataset) nestedDataset.get(i);
        if (coordType == DataType.STRING)
          coordData.setObject(ima.set(i), nested.getCoordValueString());
        else
          coordData.setDouble(ima.set(i), nested.getCoordValue());

        if (cancelTask != null && cancelTask.isCancel()) return;
      }
      coordVar.setCachedData( coordData, true);
    } else {
      Array data = coordVar.read();
      IndexIterator ii = data.getIndexIterator();
      List nestedDataset = getNestedDatasets();
      for (int i = 0; i < nestedDataset.size(); i++) {
        Aggregation.Dataset nested = (Aggregation.Dataset) nestedDataset.get(i);
        nested.setCoordValue( ii.getDoubleNext());
      }
    }

    if (isDate()) {
      coordVar.addAttribute( new ucar.nc2.Attribute("_CoordinateAxisType", "Time"));
    }

    // now we can create all the aggNew variables
    // use only named variables
    List vars = getVariables();
    for (int i=0; i<vars.size(); i++) {
      String varname = (String) vars.get(i);
      Variable v = newds.getRootGroup().findVariable(varname);
      if (v == null) {
        logger.error(ncd.getLocation()+" aggNewDimension cant find variable "+varname);
        continue;
      }

      // construct new variable, replace old one
      VariableDS vagg = new VariableDS(newds, null, null, v.getShortName(), v.getDataType(),
          dimName +" "+ v.getDimensionsString(), null, null);
      vagg.setAggregation( this);
      NcMLReader.transferVariableAttributes( v, vagg);

      newds.removeVariable( null, v.getShortName());
      newds.addVariable( null, vagg);

      if (cancelTask != null && cancelTask.isCancel()) return;
    }
  }

  //////////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * Read an aggregation variable: A variable whose data spans multiple files.
   *
   * @param mainv the aggregation variable
   * @param cancelTask allow the user to cancel
   * @return the data array
   * @throws IOException
   */
  public Array read(VariableDS mainv, CancelTask cancelTask) throws IOException {

    Array allData = Array.factory(mainv.getOriginalDataType().getPrimitiveClassType(), mainv.getShape());
    int destPos = 0;

    //System.out.println("Variable read "+mainv.getName()+" has length "+allData.getSize());
    Iterator iter = nestedDatasets.iterator();
    while (iter.hasNext()) {
      Dataset vnested = (Dataset) iter.next();
      Array varData = vnested.read(mainv, cancelTask);
      if ((cancelTask != null) && cancelTask.isCancel())
        return null;

      //System.out.println("  copy "+varData.getSize()+" starting at "+destPos);
      Array.arraycopy(varData, 0, allData, destPos, (int) varData.getSize());
      destPos += varData.getSize();
    }

    return allData;
  }


  /**
   * Read a section of an aggregation variable.
   *
   * @param mainv the aggregation variable
   * @param cancelTask allow the user to cancel
   * @param section read just this section of the data
   * @return the data array section
   * @throws IOException
   */
  public Array read(VariableDS mainv, CancelTask cancelTask, List section) throws IOException, InvalidRangeException {

    Array sectionData = Array.factory(mainv.getOriginalDataType(), Range.getShape(section));
    int destPos = 0;

    Range joinRange = (Range) section.get(0);
    List nestedSection = new ArrayList(section); // copy
    List innerSection = section.subList(1, section.size());

    /* if (type == Type.JOIN_NEW) {
      // iterate over the outer range
      for (int i=joinRange.first(); i<=joinRange.last(); i+= joinRange.stride()) {
        Dataset nested = (Dataset) nestedDatasets.get(i);
        Array varData = nested.read(mainv, cancelTask, innerSection);
        if ((cancelTask != null) && cancelTask.isCancel()) return null;

        // copy the result
        Array.arraycopy(varData, 0, sectionData, destPos, (int) varData.getSize());
        destPos += varData.getSize();
      }
    } else {   */

      if (debug) System.out.println("agg wants range="+joinRange);

      Iterator iter = nestedDatasets.iterator();
      while (iter.hasNext()) {
        Dataset nested = (Dataset) iter.next();
        Range nestedJoinRange = nested.getNestedJoinRange(joinRange);
        if (nestedJoinRange == null)
          continue;
        if (debug) System.out.println("agg use "+nested.aggStart+":"+nested.aggEnd+" range= "+nestedJoinRange+" file "+nested.getLocation());

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
    // }

    return sectionData;
  }

  //////////////////////////////////////////////////////////////////

  /**
   * Scan the directory(ies) and create nested Aggregation.Dataset objects.
   *
   * @param result add to this List objects of type Aggregation.Dataset
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
      myf.nested = makeDataset(location, location, null, myf.dateCoordS, myf.enhance, null);
      // if (myf.nested.checkOK(cancelTask))
        result.add(myf.nested);

      if ((cancelTask != null) && cancelTask.isCancel())
        return;
    }
  }

  // so subclass can override
  protected Dataset makeDataset(String cacheName, String location, String ncoordS, String coordValueS, boolean enhance, NetcdfFileFactory reader) {
    return new Dataset(cacheName, location, ncoordS, coordValueS, enhance, reader);
  }

  /**
   * Get sorted list of MyFile.
   * Sort date if it exists, else filename.
   */
  private List getFileList(CancelTask cancelTask) {
    if (debug) System.out.println("---------------getFileList ");
    ArrayList fileList = new ArrayList();
    for (int i = 0; i < scanList.size(); i++) {
      Directory d = (Directory) scanList.get(i);
      crawlDirectory(d.dirName, d.suffix, d.dateFormatMark, d.enhance, fileList, cancelTask);

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
   * recursively crawl directories, add matching MyFile files to result List
   *
   * @param dirName crawl this directory
   * @param suffix filter with this file suffix
   * @param dateFormatMark extract date from filename
   * @param enhance open in enhanced mode?
   * @param result add MyFile objects to this list
   * @param cancelTask user can cancel
   */
  private void crawlDirectory(String dirName, String suffix, String dateFormatMark, boolean enhance, List result, CancelTask cancelTask) {
    File allDir = new File(dirName);
    File[] allFiles = allDir.listFiles();
    for (int i = 0; i < allFiles.length; i++) {
      File f = allFiles[i];
      String location = f.getAbsolutePath();

      if (f.isDirectory())
        crawlDirectory(location, suffix, dateFormatMark, enhance, result, cancelTask);
      else if (location.endsWith(suffix)) { // filter

        // optionally parse for date
        Date dateCoord = null;
        String dateCoordS = null;
        if (null != dateFormatMark) {
          String filename = f.getName();
          dateCoord = DateFromString.getDateUsingDemarkatedDateFormat(filename, dateFormatMark, '#');
          dateCoordS = formatter.toDateTimeStringISO(dateCoord);
        }

        result.add(new MyFile(f, dateCoord, dateCoordS, enhance));
        if (debug) System.out.println(" crawlDirectory adding " + location);
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

    Directory(String dirName, String suffix, String dateFormatMark, String enhanceS) {
      this.dirName = dirName;
      this.suffix = suffix;
      this.dateFormatMark = dateFormatMark;
      if ((enhanceS != null) && enhanceS.equalsIgnoreCase("true"))
        enhance = true;
      if (type == Type.FORECAST_MODEL_COLLECTION)
        enhance = true;
    }
  }

  /**
   * Encapsolate a file that was scanned.
   */
  private class MyFile {
    File file;
    Date dateCoord;
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
    private String coordValueS; // coordinate value from netcdf coordValue Attribute (joinNew)
    private String coordValuesExisting; // cache coordinate values from joinExisting datasets
    private boolean enhance;
    private NetcdfFileFactory reader;

    private int ncoord; // n coordinates in outer dimension for this dataset; joinExisting
    private double coordValue; // if numeric valued coordinate - joinNew

    private boolean isStringValued = false;
    private int aggStart = 0, aggEnd = 0; // index in aggregated dataset; aggStart <= i < aggEnd

    // joinNew or joinExisting. dataset opening is deferred
    protected Dataset(String cacheName, String location, String ncoordS, String coordValueS, boolean enhance, NetcdfFileFactory reader) {
      this.cacheName = cacheName;
      this.location = StringUtil.substitute(location, "\\", "/");
      this.coordValueS = coordValueS;
      this.enhance = enhance;
      this.reader = (reader != null) ? reader : new PolymorphicReader();

      if (type == Type.JOIN_NEW)
        this.ncoord = 1;
      else if (ncoordS != null) {
        try {
          this.ncoord = Integer.parseInt(ncoordS);
        }
        catch (NumberFormatException e) {
          logger.error("bad ncoord attribute on dataset " + location);
        }
      }

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
    }

    public void setCoordValue(double coordValue) {
      this.coordValue = coordValue;
      this.isStringValued = false;
    }

    public double getCoordValue() {
      return coordValue;
    }

    public void setCoordValueString(String coordValueS) {
      this.coordValueS = coordValueS;
      this.isStringValued = true;
    }

    public String getCoordValueString() {
      return isStringValued ? coordValueS : Double.toString(coordValue);
    }

    public String getLocation() {
      return location;
    }

    // wantStart, wantStop are the indices in the aggregated dataset, wantStart <= i < wantEnd
    // if this overlaps, set the Range required for the nested dataset
    // if no overlap, return null
    // note this should handle strides ok
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
      if (typicalDataset == this)
        return typical;

      NetcdfFile ncfile;
      if (enhance)
        ncfile = NetcdfDatasetCache.acquire(cacheName, cancelTask, (NetcdfDatasetFactory) reader);
      else
        ncfile = NetcdfFileCache.acquire(cacheName, cancelTask, reader);

      if ((type == Type.JOIN_EXISTING) || (type == Type.FORECAST_MODEL))
        cacheCoordValues(ncfile);
      return ncfile;
    }


    protected void releaseFile(NetcdfFile ncfile) throws IOException {
      if (typicalDataset != this)
        ncfile.close();
    }

    private NetcdfFile openFile(CancelTask cancelTask) throws IOException {
      NetcdfFile ncfile;
      if (enhance)
        ncfile = NetcdfDataset.openDataset(location, true, cancelTask);
      else
        ncfile = NetcdfDataset.openFile(location, cancelTask);

      if ((type == Type.JOIN_EXISTING) || (type == Type.FORECAST_MODEL))
        cacheCoordValues(ncfile);
      return ncfile;
    }

    private void cacheCoordValues(NetcdfFile ncfile) throws IOException {
      Variable coordVar = ncfile.findVariable(dimName);
      if (coordVar != null) {
        Array data = coordVar.read();
        coordValuesExisting = data.toString();
      }
    }

    public int getNcoords(CancelTask cancelTask) throws IOException {
      if (ncoord <= 0) {
        NetcdfFile ncd = acquireFile(cancelTask);
        if ((cancelTask != null) && cancelTask.isCancel())
          return 0;

        Dimension d = ncd.getRootGroup().findDimension(dimName);
        if (d != null)
          ncoord = d.getLength();
        releaseFile(ncd);
      }
      return ncoord;
    }


    private int setStartEnd(int aggStart, CancelTask cancelTask) throws IOException {
      this.aggStart = aggStart;
      this.aggEnd = aggStart + getNcoords(cancelTask);
      return ncoord;
    }

    protected Array read(VariableDS mainv, CancelTask cancelTask) throws IOException {
      NetcdfFile ncd = null;
      try {
        ncd = acquireFile(cancelTask);

        if ((cancelTask != null) && cancelTask.isCancel())
          return null;

        Variable v = modifyVariable( ncd, mainv.getName());
        return v.read();

      } finally {
        releaseFile(ncd);

      }
    }

    private Array read(VariableDS mainv, CancelTask cancelTask, List section) throws IOException, InvalidRangeException {
      NetcdfFile ncd = null;
      try {
        ncd = acquireFile(cancelTask);
        if ((cancelTask != null) && cancelTask.isCancel())
          return null;
        if (debug)  {
          System.out.print("agg read "+ncd.getLocation()+" nested= "+getLocation());
          for (int i = 0; i < section.size(); i++) {
            Range range = (Range) section.get(i);
            System.out.print(" "+range+":");
          }
          System.out.println("");
        }

        Variable v = modifyVariable( ncd, mainv.getName());
        return  v.read(section);

      } finally {
        releaseFile(ncd);
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

    protected Variable modifyVariable(NetcdfFile ncfile, String name) throws IOException {
      return ncfile.findVariable( name);
    }

    /* private Variable modifyVariable(NetcdfFile ncfile, String name) throws IOException {
      Variable want = ncfile.findVariable( name);

      // do we need to modify ?
      if (type != Type.FORECAST_MODEL) return want;
      if (want.getRank() < 1) return want;
      Dimension d = want.getDimension(0);
      if (!dimName.equals(d.getName()))
        return want;

      int n = getNcoords(null); // not needed since its always 1 now
      if (d.getLength() == n)
        return want;

      // looks like we need to modify it
      Range aggRange;
      try {
        aggRange = new Range( forecastOffset, forecastOffset);
      } catch (InvalidRangeException e) {
        logger.error(" Aggregation.modify make Range", e);
        return want;
      }

      // replace the first Range
      List ranges = want.getRanges();
      ranges.set(0, aggRange);

      // subset it
      Variable vsubset;
      try {
        vsubset = want.section(ranges);
      } catch (InvalidRangeException e) {
        logger.error(" Aggregation.modify make Variable "+want.getName(), e);
        return want;
      }

      return vsubset;
    }



    /* private int findForecastOffset(NetcdfFile ncfile) throws IOException {
      if (forecastDates == null)
        makeForecastDates( ncfile);

      Date want = forecastDate;
      for (int i = 0; i < forecastDates.size(); i++) {
        Date date = (Date) forecastDates.get(i);
        if (date.equals(want)) {
          System.out.println(" found date "+DateUnit.getStandardDateString(date)+" at index "+i);
          return i;
        }
      }

      // cant find this date
      System.out.println(" didnt find date "+DateUnit.getStandardDateString(want)+" in file "+ncfile.getLocation());

      return 0;
    } */

   /*  private void makeForecastDates(NetcdfFile ncfile) throws IOException {
      forecastDates = new ArrayList();
      Variable coordVar = ncfile.findVariable(forecastDateVariable);
      Array coordValues = coordVar.read();
      Index index = coordValues.getIndex();
      int n = (int) coordValues.getSize();

      // see if it has a valid udunits unit
      String units = coordVar.getUnitsString();
      if (units != null) {
        SimpleUnit su = SimpleUnit.factory(units);
        if ((su != null) && (su instanceof DateUnit)) {
          DateUnit du = (DateUnit) su;
          for (int i = 0; i < n; i++) {
            Date d = du.makeDate(coordValues.getDouble(index.set(i)));
            forecastDates.add(d);
            if (debug) System.out.println(" added forecast date "+formatter.toDateTimeString(d)+" for file "+ncfile.getLocation());
          }
          return;
        }
      }

      // otherwise, see if its a String, and if we can parse the values as an ISO date
      if (coordVar.getDataType() == DataType.STRING) {
        for (int i = 0; i < n; i++) {
          String coordValue = (String) coordValues.getObject(index.set(i));
          Date d = formatter.getISODate(coordValue);
          if (d == null) {
            logger.error("Error on forecast date variable, not udunit or ISO. "+ coordValue);
            forecastDates.add(null);
          } else {
            forecastDates.add(d);
          }
        }
        return;
      }

      if (coordVar.getDataType() == DataType.CHAR) {
        ArrayChar coordValuesChar = (ArrayChar) coordValues;
        for (int i = 0; i < n; i++) {
          String coordValue = coordValuesChar.getString(i);
          Date d = formatter.getISODate(coordValue);
          if (d == null) {
            logger.error("Error on forecast date variable, not udunit or ISO. "+ coordValue);
            forecastDates.add(null);
          } else {
            forecastDates.add(d);
          }
        }
        return;
      }

      logger.error("Error on forecast date variable, not udunit or ISO formatted.");
    }

    private void readModelRunDate(NetcdfFile ncfile) throws IOException {
      Variable coordVar = ncfile.findVariable(referenceDateVariable);
      Array coordValues = coordVar.read();
      Index index = coordValues.getIndex();
      int n = (int) coordValues.getSize();

      // see if it has a valid udunits unit
      String units = coordVar.getUnitsString();
      if (units != null) {
        SimpleUnit su = SimpleUnit.factory(units);
        if ((su != null) && (su instanceof DateUnit)) {
          DateUnit du = (DateUnit) su;
          modelRunDate = du.makeDate( coordValues.getDouble(index));
          return;
        }
      }

      // otherwise, see if its a String, and if we can parse the values as an ISO date
      if (coordVar.getDataType() == DataType.STRING) {
          String coordValue = (String) coordValues.getObject(index);
          Date d = formatter.getISODate(coordValue);
          if (d == null) {
            logger.error("Error on referenceDateVariable, not udunit or ISO. "+ coordValue);
          } else {
            modelRunDate = d;
          }
        return;
        }

      if (coordVar.getDataType() == DataType.CHAR) {
        ArrayChar coordValuesChar = (ArrayChar) coordValues;
          String coordValue = coordValuesChar.getString(0);
          Date d = formatter.getISODate(coordValue);
          if (d == null) {
            logger.error("Error on referenceDateVariable, not udunit or ISO. "+ coordValue);
            forecastDates.add(null);
          } else {
            forecastDates.add(d);
          }
        return;
      }

      logger.error("Error on referenceDateVariable, not udunit or ISO formatted.");
    }  */


    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    class PolymorphicReader implements NetcdfFileFactory, NetcdfDatasetFactory {

      public NetcdfDataset openDataset(String cacheName, ucar.nc2.util.CancelTask cancelTask) throws java.io.IOException {
        return NetcdfDataset.openDataset(location, true, cancelTask);
      }

      public NetcdfFile open(String location, CancelTask cancelTask) throws IOException {
        return NetcdfDataset.openFile(location, cancelTask);
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