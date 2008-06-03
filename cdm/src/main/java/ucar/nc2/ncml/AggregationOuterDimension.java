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

import ucar.nc2.dataset.*;
import ucar.nc2.util.CancelTask;
import ucar.nc2.*;
import ucar.ma2.*;

import java.io.IOException;
import java.util.*;

/**
 * Superclass for Aggregations on the outer dimension: joinNew, joinExisting, Fmrc
 *
 * @author caron
 * @since Aug 10, 2007
 */
public abstract class AggregationOuterDimension extends Aggregation {

  protected List<VariableDS> aggVars = new ArrayList<VariableDS>();
  private int totalCoords = 0;  // the aggregation dimension size

  protected List<String> aggVarNames = new ArrayList<String>(); // joinNew
  protected List<CacheVar> cacheList = new ArrayList<CacheVar>(); // promote global attribute to variable
  protected boolean timeUnitsChange = false;

  protected boolean debugCache = true;

  /**
   * Create an Aggregation for the given NetcdfDataset.
   * The following addXXXX methods are called, then finish(), before the object is ready for use.
   *
   * @param ncd      Aggregation belongs to this NetcdfDataset
   * @param dimName  the aggregation dimension name
   * @param type     the Aggregation.Type
   * @param recheckS how often to check if files have changes
   */
  protected AggregationOuterDimension(NetcdfDataset ncd, String dimName, Type type, String recheckS) {
    super(ncd, dimName, type, recheckS);
  }

  /**
   * Set if time units can change. Implies isDate
   *
   * @param timeUnitsChange true if time units can change
   */
  void setTimeUnitsChange(boolean timeUnitsChange) {
    this.timeUnitsChange = timeUnitsChange;
    if (timeUnitsChange) isDate = true;
  }


  /**
   * Add a name for a variableAgg element
   *
   * @param varName name of agg variable
   */
  public void addVariable(String varName) {
    aggVarNames.add(varName);
  }

  /**
   * Promote a global attribute to a variable
   *
   * @param varName name of agg variable
   * @param orgName name of global attribute, if different from the variable
   */
  void addVariableFromGlobalAttribute(String varName, String orgName) {
    cacheList.add(new PromoteVar(varName, orgName));
  }

  /**
   * Cache a variable (for efficiency).
   * Useful for Variables that are used a lot, and not too large, like coordinate variables.
   *
   * @param varName name of variable to cache. must exist.
   */
  void addCacheVariable(String varName) {
    cacheList.add(new CacheVar(varName));
  }

   /**
   * Get dimension name to join on
   *
   * @return dimension name or null if type union/tiled
   */
  public String getDimensionName() {
    return dimName;
  }


  /**
   * Get the list of aggregation variable names: variables whose data spans multiple files.
   * For type joinNew only.
   *
   * @return the list of aggregation variable names
   */
  List<String> getAggVariableNames() {
    return aggVarNames;
  }

  protected void buildCoords(CancelTask cancelTask) throws IOException {
    List<Dataset> nestedDatasets = getDatasets();

    if (type == Type.FORECAST_MODEL_COLLECTION) {
      for (Dataset nested : nestedDatasets) {
        DatasetOuterDimension dod = (DatasetOuterDimension) nested;
        dod.ncoord = 1;
      }
    }

    totalCoords = 0;
    for (Dataset nested : nestedDatasets) {
      DatasetOuterDimension dod = (DatasetOuterDimension) nested;
      totalCoords += dod.setStartEnd(totalCoords, cancelTask);
    }
  }

  protected int getTotalCoords() {
    return totalCoords;
  }

  protected void promoteGlobalAttributes(DatasetOuterDimension typicalDataset) throws IOException {   

    for (CacheVar cv : cacheList) {
      if (!(cv instanceof PromoteVar)) continue;
      PromoteVar pv = (PromoteVar) cv;

      Array data = pv.read( typicalDataset);
      pv.dtype = DataType.getType(data.getElementType());
      VariableDS promotedVar = new VariableDS(ncDataset, null, null, pv.varName, pv.dtype, dimName, null, null);
      ncDataset.addVariable(null, promotedVar);
      promotedVar.setProxyReader(this);
      promotedVar.setSPobject( pv);
    }
  }

  protected void rebuildDataset() throws IOException {
    buildCoords(null);

    // reset dimension length
    Dimension aggDim = ncDataset.findDimension(dimName);
    aggDim.setLength(getTotalCoords());

    // reset coordinate var
    VariableDS joinAggCoord = (VariableDS) ncDataset.getRootGroup().findVariable(dimName);
    joinAggCoord.setDimensions(dimName); // reset its dimension
    joinAggCoord.invalidateCache(); // get rid of any cached data, since its now wrong

    // reset agg variables
    for (Variable aggVar : aggVars) {
      aggVar.setDimensions(dimName); // reset its dimension
      aggVar.invalidateCache(); // get rid of any cached data, since its now wrong
    }

    // reset the typical dataset, where non-agg variables live
    Dataset typicalDataset = getTypicalDataset();
    DatasetProxyReader proxy = new DatasetProxyReader(typicalDataset);
    for (Variable var : ncDataset.getRootGroup().getVariables()) {
      VariableDS varDS = (VariableDS) var;
      if (aggVars.contains(varDS) || dimName.equals(var.getName()))
        continue;
      VariableEnhanced ve = (VariableEnhanced) var;
      ve.setProxyReader(proxy);
    }

    // reset cacheVars
    for (CacheVar cv : cacheList) {
      cv.reset();
    }
  }

  /**
   * Read an aggregation variable: A variable whose data spans multiple files.
   *
   * @param mainv      the aggregation variable
   * @param cancelTask allow the user to cancel
   * @return the data array
   * @throws IOException
   */
  public Array read(Variable mainv, CancelTask cancelTask) throws IOException {

    Object spObj = mainv.getSPobject();
    if (spObj != null && spObj instanceof CacheVar) {
      CacheVar pv = (CacheVar) spObj;
      try {
        Array data = pv.read(mainv.getShapeAsSection(), cancelTask);
        if (data.getElementType() != mainv.getDataType().getPrimitiveClassType()) {
          Array newData = Array.factory(mainv.getDataType(), data.getShape());
          MAMath.copy(newData, data);
          return newData;
        }
        return data;
        
      } catch (InvalidRangeException e) {
        logger.error("readAgg " + getLocation(), e);
        throw new IllegalArgumentException("readAgg " + getLocation(), e);
      }
    }

    // the case of the agg coordinate var
    //if (mainv.getShortName().equals(dimName))
    //  return readAggCoord(mainv, cancelTask);

    // read the original type - if its been promoted to a new type, the conversion happens after this read
    DataType dtype = (mainv instanceof VariableDS) ? ((VariableDS) mainv).getOriginalDataType() : mainv.getDataType();
    Array allData = Array.factory(dtype, mainv.getShape());
    int destPos = 0;

    List<Dataset> nestedDatasets = getDatasets();
    for (Dataset vnested : nestedDatasets) {
      Array varData = vnested.read(mainv, cancelTask);
      if ((cancelTask != null) && cancelTask.isCancel())
        return null;

      // arraycopy( Array arraySrc, int srcPos, Array arrayDst, int dstPos, int len)
      Array.arraycopy(varData, 0, allData, destPos, (int) varData.getSize());
      destPos += varData.getSize();
    }

    return allData;
  }

  /* protected Array readAggCoord(Variable aggCoord, CancelTask cancelTask) throws IOException {
    DataType dtype = aggCoord.getDataType();
    Array allData = Array.factory(dtype, aggCoord.getShape());
    IndexIterator result = allData.getIndexIterator();

    List<Dataset> nestedDatasets = getDatasets();
    for (Dataset vnested : nestedDatasets) {

      try {
        readAggCoord(aggCoord, cancelTask, (DatasetOuterDimension) vnested, dtype, result, null, null, null);
      } catch (InvalidRangeException e) {
        e.printStackTrace();  // cant happen
      }

      if ((cancelTask != null) && cancelTask.isCancel())
        return null;
    }

    // cache it so we dont have to come here again; make sure we invalidate cache when data changes!
    aggCoord.setCachedData(allData, false);

    return allData;
  } */

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

    Object spObj = mainv.getSPobject();
    if (spObj != null && spObj instanceof CacheVar) {
      CacheVar pv = (CacheVar) spObj;
      return pv.read(mainv.getShapeAsSection(), cancelTask);
    }

    // the case of the agg coordinate var
    //if (mainv.getShortName().equals(dimName))
    //  return readAggCoord(mainv, section, cancelTask);

    DataType dtype = (mainv instanceof VariableDS) ? ((VariableDS) mainv).getOriginalDataType() : mainv.getDataType();
    Array sectionData = Array.factory(dtype, section.getShape());
    int destPos = 0;

    List<Range> ranges = section.getRanges();
    Range joinRange = section.getRange(0);
    List<Range> nestedSection = new ArrayList<Range>(ranges); // get copy
    List<Range> innerSection = ranges.subList(1, ranges.size());

    if (debug) System.out.println("   agg wants range=" + mainv.getName() + "(" + joinRange + ")");

    List<Dataset> nestedDatasets = getDatasets();
    for (Dataset nested : nestedDatasets) {
      DatasetOuterDimension dod = (DatasetOuterDimension) nested;
      Range nestedJoinRange = dod.getNestedJoinRange(joinRange);
      if (nestedJoinRange == null)
        continue;
      //if (debug)
      //  System.out.println("   agg use " + nested.aggStart + ":" + nested.aggEnd + " range= " + nestedJoinRange + " file " + nested.getLocation());

      Array varData;
      if ((type == Type.JOIN_NEW) || (type == Type.FORECAST_MODEL_COLLECTION)) {
        varData = dod.read(mainv, cancelTask, innerSection);
      } else {
        nestedSection.set(0, nestedJoinRange);
        varData = dod.read(mainv, cancelTask, nestedSection);
      }

      if ((cancelTask != null) && cancelTask.isCancel())
        return null;

      Array.arraycopy(varData, 0, sectionData, destPos, (int) varData.getSize());
      destPos += varData.getSize();
    }

    return sectionData;
  }

  /* protected Array readAggCoord(Variable aggCoord, Section section, CancelTask cancelTask) throws IOException, InvalidRangeException {
    DataType dtype = aggCoord.getDataType();
    Array allData = Array.factory(dtype, section.getShape());
    IndexIterator result = allData.getIndexIterator();

    List<Range> ranges = section.getRanges();
    Range joinRange = section.getRange(0);
    List<Range> nestedSection = new ArrayList<Range>(ranges); // get copy
    List<Range> innerSection = ranges.subList(1, ranges.size());

    List<Dataset> nestedDatasets = getDatasets();
    for (Dataset vnested : nestedDatasets) {
      DatasetOuterDimension dod = (DatasetOuterDimension) vnested;
      Range nestedJoinRange = dod.getNestedJoinRange(joinRange);
      if (nestedJoinRange == null)
        continue;
      //if (debug)
      //  System.out.println("   agg use " + vnested.aggStart + ":" + vnested.aggEnd + " range= " + nestedJoinRange + " file " + vnested.getLocation());

      readAggCoord(aggCoord, cancelTask, dod, dtype, result, nestedJoinRange, nestedSection, innerSection);

      if ((cancelTask != null) && cancelTask.isCancel())
        return null;
    }

    return allData;
  }

  // handle the case of cached agg coordinate variables
  private void readAggCoord(Variable aggCoord, CancelTask cancelTask, DatasetOuterDimension vnested, DataType dtype, IndexIterator result,
                            Range nestedJoinRange, List<Range> nestedSection, List<Range> innerSection) throws IOException, InvalidRangeException {

    // we have the coordinates as a String
    if (vnested.coordValue != null) {

      // if theres only one coord
      if (vnested.ncoord == 1) {
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
          logger.error("readAggCoord incorrect number of coordinates dataset=" + vnested.getLocation());
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

  } */

  @Override
  protected Dataset makeDataset(String cacheName, String location, String ncoordS, String coordValueS, String sectionSpec,
          boolean enhance, ucar.nc2.util.cache.FileFactory reader) {
    return new DatasetOuterDimension(cacheName, location, ncoordS, coordValueS, enhance, reader);
  }

  /**
   * Encapsolates a NetcdfFile that is a component of the aggregation.
   */
  class DatasetOuterDimension extends Dataset {

    protected int ncoord; // number of coordinates in outer dimension for this dataset; joinExisting
    protected String coordValue;  // if theres a coordValue on the netcdf element - may be multiple, blank seperated
    protected Date coordValueDate;  // if its a date
    protected boolean isStringValued = false;
    private int aggStart = 0, aggEnd = 0; // index in aggregated dataset; aggStart <= i < aggEnd

    protected DatasetOuterDimension(String location) {
      super(location);
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
    protected DatasetOuterDimension(String cacheName, String location, String ncoordS, String coordValueS,
                                    boolean enhance, ucar.nc2.util.cache.FileFactory reader) {
      super(cacheName, location, enhance, reader);
      this.coordValue = coordValueS;

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

    protected void setInfo(MyCrawlableDataset myf) {
      super.setInfo(myf);
      coordValueDate = myf.dateCoord;
      // LOOK why not dateCoordS etc ??
    }


    /**
     * Get the coordinate value(s) as a String for this Dataset
     *
     * @return the coordinate value(s) as a String
     */
    public String getCoordValueString() {
      return coordValue;
    }

    /**
     * Get the coordinate value as a Date for this Dataset; may be null
     *
     * @return the coordinate value as a Date, or null
     */
    public Date getCoordValueDate() {
      return coordValueDate;
    }

    /**
     * Get number of coordinates in this Dataset.
     * If not already set, open the file and get it from the aggregation dimension.
     *
     * @param cancelTask allow cancellation
     * @return number of coordinates in this Dataset.
     * @throws java.io.IOException if io error
     */
    public int getNcoords(CancelTask cancelTask) throws IOException {
      if (ncoord <= 0) {
        NetcdfFile ncd = null;
        try {
          ncd = acquireFile(cancelTask);
          if ((cancelTask != null) && cancelTask.isCancel()) return 0;

          Dimension d = ncd.getRootGroup().findDimension(dimName);
          if (d != null)
            ncoord = d.getLength();
        } finally {
          close(ncd);
        }
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
    protected int setStartEnd(int aggStart, CancelTask cancelTask) throws IOException {
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
    protected Range getNestedJoinRange(Range totalRange) throws InvalidRangeException {
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

    /* @Override
    protected void cacheCoordValues(NetcdfFile ncfile) throws IOException {
      if (coordValue != null) return;

      Variable coordVar = ncfile.findVariable(dimName);
      if (coordVar != null) {
        Array data = coordVar.read();
        coordValue = data.toString();
      }

    } */

    // read any cached variables that need it

    @Override
    protected void cacheVariables(NetcdfFile ncfile) throws IOException {
      for (CacheVar pv : cacheList) {
        pv.read(this);
      }
    }

    @Override
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

        Variable v = ncd.findVariable(mainv.getName());

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
        close(ncd);
      }
    }

  }

  /////////////////////////////////////////////
  // vars that should be cached across the agg for efficiency
  class CacheVar {
    String varName;
    DataType dtype;
    Map<String, Array> dataMap = new HashMap<String, Array>();

    CacheVar(String varName) {
      this.varName = varName;
    }

    // clear out old stuff from the Hash, so it doesnt grow forever
    void reset() {
      Map<String, Array> newMap = new HashMap<String, Array>();
      for (Dataset ds : datasets) {
        String location = ds.getLocation();
        Array data = dataMap.get(location);
        if (data != null)
          newMap.put(location, data);
      }
      dataMap = newMap;
    }

    Array read(Section section, CancelTask cancelTask) throws IOException, InvalidRangeException {
      if (debugCache) System.out.println("caching "+varName+" section= "+section);
      Array allData = Array.factory(dtype, section.getShape());

      List<Range> ranges = section.getRanges();
      Range joinRange = section.getRange(0);
      Section innerSection = null;
      if (section.getRank() > 1)
        innerSection = new Section(ranges.subList(1, ranges.size()));

      int resultPos = 0;
      List<Dataset> nestedDatasets = getDatasets();
      for (Dataset vnested : nestedDatasets) {
        DatasetOuterDimension dod = (DatasetOuterDimension) vnested;

        // can we skip ?
        Range nestedJoinRange = dod.getNestedJoinRange(joinRange);
        if (nestedJoinRange == null)
          continue;

        Array varData = read(dod);
        if ((innerSection != null) && (varData.getSize() != innerSection.computeSize())) // do we need to subset the data array ?
          varData = varData.section(innerSection.getRanges());

        // copy to result array
        int nelems = (int) varData.getSize();
        Array.arraycopy(varData, 0, allData, resultPos, nelems);
        resultPos += nelems;

        if ((cancelTask != null) && cancelTask.isCancel())
          return null;
      }

      return allData;
    }

    protected void setData(Dataset dset, Array data) {
      dataMap.put(dset.getLocation(), data);
    }

    protected Array getData(Dataset dset) {
      return dataMap.get(dset.getLocation());
    }

    // get the Array of data for this var in this dataset
    protected Array read(DatasetOuterDimension dset) throws IOException {
      Array data = getData(dset);
      if (data != null) return data;

      NetcdfFile ncfile = null;
      try {
        ncfile = dset.acquireFile(null);
        Variable v = ncfile.findVariable(varName);
        data = v.read();
        setData(dset, data);
        return data;

      } finally {
        dset.close(ncfile);
      }
    }
  }

  /////////////////////////////////////////////
  // data values might be specified by Dataset.coordValue
  class CoordValueVar extends CacheVar {
    Variable v;
    Section innerSection;

    CoordValueVar(Variable v) {
      super(v.getName());
      dtype = v.getDataType();

      List<Range> ranges = v.getShapeAsSection().getRanges();
      innerSection = new Section(ranges.subList(1, ranges.size()));
    }

    protected Array read(DatasetOuterDimension dset) throws IOException {
      Array data = getData(dset);
      if (data != null) return data;

      data = Array.factory(dtype, innerSection.getShape());
      IndexIterator ii = data.getIndexIterator();

      // we have the coordinates as a String
      if (dset.coordValue != null) {

        // if theres only one coord
        if (dset.ncoord == 1) {
          if (dtype == DataType.STRING) {
            ii.setObjectNext(dset.coordValue);
          } else {
            double val = Double.parseDouble(dset.coordValue);
            ii.setDoubleNext(val);
          }

        } else {

          // multiple coords
          int count = 0;
          StringTokenizer stoker = new StringTokenizer(dset.coordValue, " ,");
          while (stoker.hasMoreTokens()) {
            String toke = stoker.nextToken();

            // LOOK how come you dont have to check if this coordinate is contained ?
            // if (!nestedJoinRange.contains(count))

            if (dtype == DataType.STRING) {
              ii.setObjectNext(toke);
            } else {
              double val = Double.parseDouble(toke);
              ii.setDoubleNext(val);
            }
            count++;
          }

          if (count != dset.ncoord) {
            logger.error("readAggCoord incorrect number of coordinates dataset=" + dset.getLocation());
            throw new IllegalArgumentException("readAggCoord incorrect number of coordinates dataset=" + dset.getLocation());
          }
        }

        setData(dset, data);
        return data;
      }

      return super.read(dset);
    }
  }

  /////////////////////////////////////////////
  // global attributes promoted to variables
  class PromoteVar extends CacheVar {
    String orgName;

    PromoteVar(String varName, String orgName) {
      super(varName);
      this.orgName = orgName != null ? orgName : varName;
    }

    protected Array read(DatasetOuterDimension dset) throws IOException {
      Array data = getData(dset);
      if (data != null) return data;

      NetcdfFile ncfile = null;
      try {
        ncfile = dset.acquireFile(null);
        Attribute att = ncfile.findGlobalAttribute(orgName);
        data = att.getValues();
        setData(dset, data);
        return data;

      } finally {
        dset.close(ncfile);
      }
    }

  }


}
