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
import ucar.ma2.DataType;
import ucar.nc2.*;
import ucar.nc2.units.DateUnit;
import ucar.nc2.dt.GridDatatype;
import ucar.nc2.dt.GridDataset;
import ucar.nc2.dt.GridCoordSystem;
import ucar.nc2.dt.fmrc.FmrcDefinition;
import ucar.nc2.dt.fmrc.ForecastModelRunInventory;
import ucar.nc2.dataset.*;
import ucar.nc2.constants.*;
import ucar.nc2.util.CancelTask;
import ucar.nc2.util.URLnaming;

import java.util.*;
import java.io.*;

/**
 * Implement NcML Forecast Model Run Collection Aggregation
 * with files that are complete runs (have all forecast times in the same file)
 *
 * @author caron
 */
public class AggregationFmrc extends AggregationOuterDimension {
  static private String definitionDir;

  static public void setDefinitionDirectory(String defDir) {
    definitionDir = defDir;
  }

  private FmrcDefinition fmrcDefinition;
  private boolean debug = false;

  public AggregationFmrc(NetcdfDataset ncd, String dimName, String recheckS) {
    super(ncd, dimName, Type.FORECAST_MODEL_COLLECTION, recheckS);
  }

  protected AggregationFmrc(NetcdfDataset ncd, String dimName, Aggregation.Type type, String recheckS) {
    super(ncd, dimName, type, recheckS);
  }

  public void setInventoryDefinition(String invDef) {
    String path = URLnaming.resolveFile(definitionDir, invDef);
    fmrcDefinition = new FmrcDefinition();
    try {
      boolean ok = fmrcDefinition.readDefinitionXML(path);
      if (!ok) {
        logger.warn("FmrcDefinition file not found= " + path);
        fmrcDefinition = null;
      } else {
        spiObject = fmrcDefinition;
      }
    } catch (IOException e) {
      e.printStackTrace();
      fmrcDefinition = null;
    }
  }

  @Override
  protected void buildDataset(CancelTask cancelTask) throws IOException {
    // LOOK - should cache the GridDataset directly    
    // open a "typical"  nested dataset and copy it to newds
    Dataset typicalDataset = getTypicalDataset();
    NetcdfFile typical = typicalDataset.acquireFile(cancelTask);
    NetcdfDataset typicalDS = (typical instanceof NetcdfDataset) ? (NetcdfDataset) typical : new NetcdfDataset(typical);
    //if (typicalDS.getEnhanceMode() == NetcdfDataset.EnhanceMode.None) LOOK
    //  typicalDS.enhance();

    // work with a GridDataset
    GridDataset typicalGds = new ucar.nc2.dt.grid.GridDataset(typicalDS);

    // finish the work
    buildDataset(typicalDataset, typical, typicalGds, cancelTask);
  }

  // split out so FmrcSingle can call seperately
  protected void buildDataset(Dataset typicalDataset, NetcdfFile typical, GridDataset typicalGds, CancelTask cancelTask) throws IOException {
    buildCoords(cancelTask);
    DatasetConstructor.transferDataset(typicalGds.getNetcdfFile(), ncDataset, null);

    // some additional global attributes
    Group root = ncDataset.getRootGroup();
    root.addAttribute(new Attribute("Conventions", "CF-1.0, " + _Coordinate.Convention));
    root.addAttribute(new Attribute("cdm_data_type", FeatureType.GRID.toString()));

    // create runtime aggregation dimension
    String dimName = getDimensionName();
    int nruns = getTotalCoords();
    Dimension aggDim = new Dimension(dimName, nruns);
    ncDataset.removeDimension(null, dimName); // remove previous declaration, if any
    ncDataset.addDimension(null, aggDim);

    // deal with promoteGlobalAttribute
    promoteGlobalAttributes( (DatasetOuterDimension) typicalDataset);

    // create runtime aggregation coordinate variable
    DataType coordType = DataType.STRING; // LOOK getCoordinateType();
    VariableDS runtimeCoordVar = new VariableDS(ncDataset, null, null, dimName, coordType, dimName, null, null);
    runtimeCoordVar.addAttribute(new Attribute("long_name", "Run time for ForecastModelRunCollection"));
    runtimeCoordVar.addAttribute(new ucar.nc2.Attribute("standard_name", "forecast_reference_time"));
    runtimeCoordVar.addAttribute(new ucar.nc2.Attribute(_Coordinate.AxisType, AxisType.RunTime.toString()));
    ncDataset.removeVariable(null, runtimeCoordVar.getShortName());
    ncDataset.addVariable(null, runtimeCoordVar);
    if (debug) System.out.println("FmrcAggregation: added runtimeCoordVar " + runtimeCoordVar.getName());

    // deal with runtime coordinates
    if (true) { // LOOK detect if we have the info
      ArrayObject.D1 runData = (ArrayObject.D1) Array.factory(DataType.STRING, new int[]{nruns});
      List<Dataset> nestedDatasets = getDatasets();
      for (int j = 0; j < nestedDatasets.size(); j++) {
        DatasetOuterDimension dataset = (DatasetOuterDimension) nestedDatasets.get(j);
        runData.set(j, dataset.getCoordValueString());
      }
      runtimeCoordVar.setCachedData(runData, true);
    } else {
      runtimeCoordVar.setProxyReader(this);
    }

    // handle the 2D forecast time coordinates and dimensions
    if (fmrcDefinition != null) {
      makeTimeCoordinateWithDefinition(typicalGds, cancelTask);

    } else {
      // for the case that we dont have a fmrcDefinition
      makeTimeCoordinate(typicalGds, cancelTask);
    }

    // promote all grid variables to agg variables
    for (GridDatatype grid : typicalGds.getGrids()) {
      Variable v = (Variable) grid.getVariable();

      // add new dimension
      String dims = dimName + " " + v.getDimensionsString();

      // construct new variable, replace old one
      Group newGroup =  DatasetConstructor.findGroup(ncDataset, v.getParentGroup());
      VariableDS vagg = new VariableDS(ncDataset, newGroup, null, v.getShortName(), v.getDataType(), dims, null, null);
      vagg.setProxyReader(this);
      DatasetConstructor.transferVariableAttributes(v, vagg);

      // we need to explicitly list the coordinate axes, because time coord is now 2D
      vagg.addAttribute(new Attribute(_Coordinate.Axes, dimName + " " + grid.getCoordinateSystem().getName()));
      vagg.addAttribute(new Attribute("coordinates", dimName + " " + grid.getCoordinateSystem().getName())); // CF

      newGroup.removeVariable( v.getShortName());
      newGroup.addVariable( vagg);
      aggVars.add(vagg);

      if (debug) System.out.println("FmrcAggregation: added grid " + v.getName());
    }

    ncDataset.finish();
    setDatasetAcquireProxy(typicalDataset, ncDataset);
    ncDataset.enhance();

    typicalDataset.close( typical);
    typicalGds.close();
  }

  // we assume the variables are complete, but the time dimensions and values have to be recomputed
  @Override
  protected void rebuildDataset() throws IOException {
    buildCoords( null);

    // redo the aggregation dimension, makes things easier if you dont replace Dimension, just modify the length
    int nruns = getTotalCoords();
    String dimName = getDimensionName();
    Dimension aggDim = ncDataset.findDimension(dimName);   // LOOK use group
    aggDim.setLength(nruns);

    // recalc runtime array
    VariableDS runtimeCoord = (VariableDS) ncDataset.findVariable(dimName);
    runtimeCoord.resetDimensions();
    if (true) { // LOOK detect if we have the info
      ArrayObject.D1 runData = (ArrayObject.D1) Array.factory(DataType.STRING, new int[]{nruns});
      List<Dataset> nestedDatasets = getDatasets();
      for (int j = 0; j < nestedDatasets.size(); j++) {
        DatasetOuterDimension dataset = (DatasetOuterDimension) nestedDatasets.get(j);
        runData.set(j, dataset.getCoordValueString());
      }
      runtimeCoord.setCachedData(runData, true);
    }

    if (fmrcDefinition != null) {
      List<FmrcDefinition.RunSeq> runSeq = fmrcDefinition.getRunSequences();
      for (FmrcDefinition.RunSeq seq : runSeq) { // each runSeq generates a 2D time coordinate
        String timeDimName = seq.getName();

        // whats the maximum size ?
        int max_times = 0;
        List<Dataset> nestedDatasets = getDatasets();
        for (Dataset dataset : nestedDatasets) {
          DatasetOuterDimension dod = (DatasetOuterDimension) dataset;
          ForecastModelRunInventory.TimeCoord timeCoord = seq.findTimeCoordByRuntime(dod.getCoordValueDate());
          double[] offsets = timeCoord.getOffsetHours();
          max_times = Math.max(max_times, offsets.length);
        }

        // redo the time dimension, makes things easier if you dont replace Dimension, just modify the length
        Dimension timeDim = ncDataset.findDimension(timeDimName); // LOOK use group
        timeDim.setLength(max_times);

        DatasetOuterDimension firstDataset = (DatasetOuterDimension) nestedDatasets.get(0);
        Date baseDate = firstDataset.getCoordValueDate();
        String units = "hours since " + formatter.toDateTimeStringISO(baseDate);

        VariableDS timeCoord = (VariableDS) ncDataset.findVariable(timeDimName);
        timeCoord.resetDimensions();
        timeCoord.addAttribute(new Attribute("units", units));
        timeCoord.setUnitsString(units);

        // compute the coordinates
        Array coordValues = calcTimeCoordinateFromDef(nruns, max_times, seq);
        timeCoord.setCachedData(coordValues, true);
      }
    }

    // may have to reset non-agg variables with new proxy
    Dataset typicalDataset = getTypicalDataset();
    DatasetProxyReader typicalDatasetProxy = new DatasetProxyReader(typicalDataset);

    // reset all aggregation variables
    List<CoordinateAxis> timeAxes = new ArrayList<CoordinateAxis>();
    List<Variable> vars = ncDataset.getVariables();
    for (Variable v : vars) {

      if (v instanceof CoordinateAxis) {
        CoordinateAxis axis = (CoordinateAxis) v;
        if (axis.getAxisType() == AxisType.Time)
          timeAxes.add(axis);

        if (fmrcDefinition != null) // skip time coordinates when we have a fmrcDefinition, since they were already done
          continue;
      }

      if ((v.getRank() > 0) && v.getDimension(0).getName().equals(dimName) && (v != runtimeCoord)) {
        v.resetDimensions();
        v.setCachedData(null, false); // get rid of any cached data, since its now wrong

      } else {
        VariableEnhanced ve = (VariableDS) v;
        ProxyReader proxy = ve.getProxyReader();
        if (proxy instanceof DatasetProxyReader)
          ve.setProxyReader(typicalDatasetProxy);
      }
    }

    if (fmrcDefinition == null) {  // LOOK this is not right - need to reset the time lengths !!
      // recalc the time coordinate(s)
      for (CoordinateAxis timeAxis : timeAxes) {
        if (timeUnitsChange) {
          // Case 2: assume the time units differ for each nested file
          readTimeCoordinates(timeAxis, null);
        }
      }
    }

  }


  // for the case that we have a fmrcDefinition, there may be time coordinates that dont show up in the typical dataset
  protected void makeTimeCoordinateWithDefinition(GridDataset gds, CancelTask cancelTask) throws IOException {
    List<FmrcDefinition.RunSeq> runSeq = fmrcDefinition.getRunSequences();
    for (FmrcDefinition.RunSeq seq : runSeq) { // each runSeq generates a 2D time coordinate
      String timeDimName = seq.getName();

      // whats the maximum size ?
      boolean isRagged = false;
      int max_times = 0;
      List<Dataset> nestedDatasets = getDatasets();
      for (Dataset dataset : nestedDatasets) {
        DatasetOuterDimension dod = (DatasetOuterDimension) dataset;
        ForecastModelRunInventory.TimeCoord timeCoord = seq.findTimeCoordByRuntime( dod.getCoordValueDate());
        double[] offsets = timeCoord.getOffsetHours();
        max_times = Math.max(max_times, offsets.length);
        if (max_times != offsets.length)
          isRagged = true;
      }

      // create time dimension
      Dimension timeDim = new Dimension(timeDimName, max_times);
      ncDataset.removeDimension(null, timeDimName); // remove previous declaration, if any
      ncDataset.addDimension(null, timeDim);

      DatasetOuterDimension firstDataset = (DatasetOuterDimension) nestedDatasets.get(0);
      Date baseDate = firstDataset.getCoordValueDate();
      String desc = "Coordinate variable for " + timeDimName + " dimension";
      String units = "hours since " + formatter.toDateTimeStringISO(baseDate);

      String dims = getDimensionName() + " " + timeDimName;
      Variable newV = new VariableDS(ncDataset, null, null, timeDimName, DataType.DOUBLE, dims, desc, units);

      // do we already have the coordinate variable ?
      Variable oldV = ncDataset.getRootGroup().findVariable(timeDimName);
      Group aggGroup = null;

      if (null != oldV) {
        //NcMLReader.transferVariableAttributes(oldV, newV);
        //Attribute att = newV.findAttribute(_Coordinate.AliasForDimension);  // ??
        //if (att != null) newV.remove(att);
        aggGroup =  DatasetConstructor.findGroup(ncDataset, oldV.getParentGroup());
        aggGroup.removeVariable(timeDimName);
      }
      ncDataset.addVariable(aggGroup, newV); // add to root if group is null

      newV.addAttribute(new Attribute("units", units));
      newV.addAttribute(new Attribute("long_name", desc));
      newV.addAttribute(new Attribute("standard_name", "time"));
      newV.addAttribute(new Attribute(_Coordinate.AxisType, AxisType.Time.toString()));
      if (isRagged)
        newV.addAttribute(new Attribute("missing_value", Double.NaN));

      // compute the coordinates
      int nruns = getTotalCoords();
      Array coordValues = calcTimeCoordinateFromDef(nruns, max_times, seq);
      newV.setCachedData(coordValues, true);
    }

    ncDataset.finish();
  }


  // for the case that we dont have a fmrcDefinition
  protected void makeTimeCoordinate(GridDataset gds, CancelTask cancelTask) throws IOException {
    // LOOK how do we set the length of the time dimension(s), if its ragged?
    // Here we are just using the typical dataset !!!
    // For now, we dont handle ragged time coordinates.

    // find time axes
    Set<CoordinateAxis1D> timeAxes = new HashSet<CoordinateAxis1D>();
    for (GridDatatype grid : gds.getGrids()) {
      GridCoordSystem gcc = grid.getCoordinateSystem();
      CoordinateAxis1D timeAxis = gcc.getTimeAxis1D();
      if (null != timeAxis)
        timeAxes.add(timeAxis);
    }

    // promote the time coordinate(s) to 2D, read in values if we have to
    for (CoordinateAxis1D taxis : timeAxes) {
      // construct new variable, replace old one
      Group newGroup =  DatasetConstructor.findGroup(ncDataset, taxis.getParentGroup());

      String dims = dimName + " " + taxis.getDimensionsString();
      VariableDS vagg = new VariableDS(ncDataset, newGroup, null, taxis.getShortName(), taxis.getDataType(), dims, null, null);
      DatasetConstructor.transferVariableAttributes(taxis, vagg);
      Attribute att = vagg.findAttribute(_Coordinate.AliasForDimension);
      if (att != null) vagg.remove(att);

      newGroup.removeVariable( taxis.getShortName());
      newGroup.addVariable(vagg);

      if (!timeUnitsChange)
        // Case 1: assume the units are all the same, so its just another agg variable
        vagg.setProxyReader(this);
      else
        // Case 2: assume the time units differ for each nested file
        readTimeCoordinates(vagg, cancelTask);

      if (debug) System.out.println("FmrcAggregation: promoted timeCoord " + taxis.getName());
      if (cancelTask != null && cancelTask.isCancel()) return;
    }
  }

  // problem is that we may actually have a ragged array - in the time dimension only
  // so we need to override .
  // All variables that come through here have both the runtime and time dimensions
  @Override
  public Array read(Variable mainv, CancelTask cancelTask) throws IOException {

    Object spObj = mainv.getSPobject();
    if (spObj != null && spObj instanceof CacheVar) {
      CacheVar pv = (CacheVar) spObj;
      try {
        return pv.read(mainv.getShapeAsSection(), cancelTask);
      } catch (InvalidRangeException e) {
        logger.error("readAgg " + getLocation(), e);
        throw new IllegalArgumentException("readAgg " + getLocation(), e);
       }
    }

    // remove first dimension, calculate size
    List<Range> ranges = mainv.getRanges();
    List<Range> innerSection = ranges.subList(1, ranges.size());
    long fullSize = Range.computeSize(innerSection); // may not be the same as the data returned !!

    // read raw, conversion if needed done later in VariableDS
    DataType dtype = (mainv instanceof VariableDS) ? ((VariableDS) mainv).getOriginalDataType() : mainv.getDataType();
    Array allData = Array.factory(dtype, mainv.getShape());
    int destPos = 0;

    // make concurrent
    List<Dataset> nestedDatasets = getDatasets();
    for (Dataset vnested : nestedDatasets) {
      Array varData = vnested.read(mainv, cancelTask);
      if ((cancelTask != null) && cancelTask.isCancel())
        return null;

      Array.arraycopy(varData, 0, allData, destPos, (int) varData.getSize());
      destPos += fullSize;
      if (fullSize != varData.getSize())
        System.out.println("FMRC RAGGED TIME " + fullSize + " != " + varData.getSize());
    }

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
  @Override
  public Array read(Variable mainv, Section section, CancelTask cancelTask) throws IOException, InvalidRangeException {
    // If its full sized, then use full read, so that data gets cached.
    long size = section.computeSize();
    if (size == mainv.getSize())
      return read(mainv, cancelTask);


    Object spObj = mainv.getSPobject();
    if (spObj != null && spObj instanceof CacheVar) {
      CacheVar pv = (CacheVar) spObj;
      try {
        return pv.read(section, cancelTask);
      } catch (InvalidRangeException e) {
        logger.error("readAgg " + getLocation(), e);
        throw new IllegalArgumentException("readAgg " + getLocation(), e);
       }
    }

    DataType dtype = (mainv instanceof VariableDS) ? ((VariableDS) mainv).getOriginalDataType() : mainv.getDataType();
    Array sectionData = Array.factory(dtype, section.getShape());
    int destPos = 0;

    List<Range> ranges = section.getRanges();
    Range joinRange = section.getRange(0);
    List<Range> innerSection = ranges.subList(1, ranges.size());

    long fullSize = Range.computeSize(innerSection); // may not be the same as the data returned !!
    if (debug) System.out.println("   agg wants range=" + mainv.getName() + "(" + joinRange + ")");

    // make concurrent
    List<Dataset> nestedDatasets = getDatasets();
    for (Dataset nested : nestedDatasets) {
      DatasetOuterDimension dod = (DatasetOuterDimension) nested;
      if (!dod.isNeeded(joinRange))
        continue;

      Array varData = nested.read(mainv, cancelTask, innerSection);

      Array.arraycopy(varData, 0, sectionData, destPos, (int) varData.getSize());
      destPos += fullSize;
      if (fullSize != varData.getSize())
        System.out.println("FMRC RAGGED HERE " + fullSize + " != " + varData.getSize());

      if ((cancelTask != null) && cancelTask.isCancel())
        return null;
    }

    return sectionData;
  }

  private Array calcTimeCoordinateFromDef(int nruns, int max_times, FmrcDefinition.RunSeq seq) {
    // compute the coordinates
    ArrayDouble.D2 coordValues = (ArrayDouble.D2) Array.factory(DataType.DOUBLE, new int[]{nruns, max_times});
    Date baseDate = null;
    List<Dataset> nestedDatasets = getDatasets();
    for (int j = 0; j < nestedDatasets.size(); j++) {
      DatasetOuterDimension dataset = (DatasetOuterDimension) nestedDatasets.get(j);
      Date runTime = dataset.getCoordValueDate();
      if (baseDate == null)
        baseDate = runTime;
      double run_offset = ForecastModelRunInventory.getOffsetInHours(baseDate, runTime);

      ForecastModelRunInventory.TimeCoord timeCoord = seq.findTimeCoordByRuntime(runTime);
      double[] offsets = timeCoord.getOffsetHours();
      for (int k = 0; k < offsets.length; k++)
        coordValues.set(j, k, offsets[k] + run_offset);
      for (int k = offsets.length; k < max_times; k++)
        coordValues.set(j, k, Double.NaN); // possible ragged times
    }

    return coordValues;
  }

  // time units change - must read in time coords and convert, cache the results
  // must be able to be made into a CoordinateAxis1DTime
  protected void readTimeCoordinates(VariableDS timeAxis, CancelTask cancelTask) throws IOException {
    List<Date[]> dateList = new ArrayList<Date[]>();
    int maxTimes = 0;
    String units = null;

    // make concurrent
    List<Dataset> nestedDatasets = getDatasets();
    for (int i = 0; i < nestedDatasets.size(); i++) {
      Dataset dataset = nestedDatasets.get(i);
      NetcdfDataset ncd = null;
      try {
        NetcdfFile ncfile = dataset.acquireFile(cancelTask);
        ncd = (ncfile instanceof NetcdfDataset) ? (NetcdfDataset) ncfile : new NetcdfDataset(ncfile);

        VariableDS v = (VariableDS) ncd.findVariable(timeAxis.getName());
        if (v == null) {
          logger.warn("readTimeCoordinates: variable = " + timeAxis.getName() + " not found in file " + dataset.getLocation());
          return;
        }
        CoordinateAxis1DTime timeCoordVar = CoordinateAxis1DTime.factory(ncDataset, v, null);
        java.util.Date[] dates = timeCoordVar.getTimeDates();
        maxTimes = Math.max(maxTimes, dates.length);
        dateList.add(dates);
        //dataset.setNumberFmrcTimeCoords(dates.length);

        if (i == 0)
          units = v.getUnitsString();

      } finally {
        dataset.close( ncd);
      }
      if (cancelTask != null && cancelTask.isCancel()) return;
    }

    int[] shape = timeAxis.getShape();
    int ntimes = shape[1];
    if (ntimes != maxTimes) {  // make max lengt for time dimension
      shape[1] = maxTimes;
      Dimension d = timeAxis.getDimension(1);
      d.setLength(maxTimes);
      timeAxis.resetDimensions();
    }

    Array timeCoordVals = Array.factory(timeAxis.getDataType(), shape);
    Index ima = timeCoordVals.getIndex();
    timeAxis.setCachedData(timeCoordVals, false);

    // check if its a String or a udunit
    if (timeAxis.getDataType() == DataType.STRING) {

      for (int i = 0; i < dateList.size(); i++) {
        Date[] dates = dateList.get(i);
        for (int j = 0; j < dates.length; j++) {
          Date date = dates[j];
          timeCoordVals.setObject(ima.set(i, j), formatter.toDateTimeStringISO(date));
        }
      }

    } else {

      DateUnit du;
      try {
        du = new DateUnit(units);
      } catch (Exception e) {
        throw new IOException(e.getMessage());
      }
      timeAxis.addAttribute(new Attribute("units", units));

      for (int i = 0; i < dateList.size(); i++) {
        Date[] dates = dateList.get(i);
        for (int j = 0; j < dates.length; j++) {
          Date date = dates[j];
          double val = du.makeValue(date);
          timeCoordVals.setDouble(ima.set(i, j), val);
        }
      }
    }

  }


  /**
   * testing
   */
  public static void main(String arg[]) throws IOException {
    String defaultFilename = "C:/data/rap/fmrc.xml";
    String filename = (arg.length > 0) ? arg[0] : defaultFilename;

    GridDataset gds = ucar.nc2.dt.grid.GridDataset.open(filename);
    GridDatatype gg = gds.findGridDatatype("T");
    GridCoordSystem gsys = gg.getCoordinateSystem();

    // gsys.getTimeAxisForRun(1);  // generate error

    CoordinateAxis1DTime rtaxis = gsys.getRunTimeAxis();
    CoordinateAxis taxis2D = gsys.getTimeAxis();
    Array data = taxis2D.read();
    NCdump.printArray(data, "2D time array", System.out, null);

    System.out.println("Run Time, Valid Times");
    Date[] runtimes = rtaxis.getTimeDates();
    for (int i = 0; i < runtimes.length; i++) {
      System.out.println("\n" + runtimes[i]);

      CoordinateAxis1DTime taxis = gsys.getTimeAxisForRun(i);
      Date[] times = taxis.getTimeDates();
      for (int j = 0; j < times.length; j++) {
        System.out.println("   " + times[j]);
      }
    }

  }


}