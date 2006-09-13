// $Id: FmrcAggregation.java 70 2006-07-13 15:16:05Z caron $
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
import ucar.nc2.units.DateUnit;
import ucar.nc2.dt.GridDatatype;
import ucar.nc2.dt.GridDataset;
import ucar.nc2.dt.GridCoordSystem;
import ucar.nc2.dt.fmrc.FmrcDefinition;
import ucar.nc2.dt.fmrc.ForecastModelRunInventory;
import ucar.nc2.dataset.*;
import ucar.nc2.dataset.conv._Coordinate;
import ucar.nc2.util.CancelTask;

import java.util.*;
import java.io.*;

/**
 * Implement NcML Forecast Model Run Collection Aggregation
 *
 * @author caron
 */
public class AggregationFmrc extends Aggregation {
  static private String definitionDir;
  static public void setDefinitionDirectory( String defDir) {
    definitionDir = defDir;
  }

  private FmrcDefinition fmrcDefinition;
  private boolean debug = false;
  private boolean timeUnitsChange = false;

  public AggregationFmrc(NetcdfDataset ncd, String dimName, String typeName, String recheckS) {
    super(ncd, dimName, typeName, recheckS);
  }

  public void setTimeUnitsChange( boolean timeUnitsChange) {
    this.timeUnitsChange = timeUnitsChange;
  }

  public void setInventoryDefinition(String invDef) {
    String path = ucar.nc2.util.NetworkUtils.resolveFile( definitionDir, invDef);
    fmrcDefinition = new FmrcDefinition();
    try {
      boolean ok = fmrcDefinition.readDefinitionXML(path);
      if (!ok) {
        logger.warn("FmrcDefinition file not found= "+path);
        fmrcDefinition = null;
      } else {
        spiObject = fmrcDefinition;
      }
    } catch (IOException e) {
      e.printStackTrace();
      fmrcDefinition = null;
    }
  }

  protected void buildDataset(boolean isNew, CancelTask cancelTask) throws IOException {
    buildCoords(cancelTask);

    // open a "typical"  nested dataset and copy it to newds
    Dataset typicalDataset = getTypicalDataset();
    NetcdfFile typical =  typicalDataset.acquireFile(null);
    NcMLReader.transferDataset(typical, ncDataset, null); // isNew ? null : new MyReplaceVariableCheck());

    // some additional global attributes
    Group root = ncDataset.getRootGroup();
    root.addAttribute(new Attribute("Conventions", "CF-1.0, "+_Coordinate.Convention));
    root.addAttribute(new Attribute("cdm_data_type", thredds.catalog.DataType.GRID.toString()));

    // create aggregation dimension
    String dimName = getDimensionName();
    int nruns = getTotalCoords(); // same as  nestedDatasets.size()
    Dimension aggDim = new Dimension(dimName, nruns, true);
    ncDataset.removeDimension(null, dimName); // remove previous declaration, if any
    ncDataset.addDimension(null, aggDim);

    // create aggregation coordinate variable
    DataType  coordType = getCoordinateType();
    VariableDS  runtimeCoordVar = new VariableDS(ncDataset, null, null, dimName, coordType, dimName, null, null);
    runtimeCoordVar.addAttribute(new Attribute("long_name", "Run time for ForecastModelRunCollection"));
    runtimeCoordVar.addAttribute(new ucar.nc2.Attribute("standard_name", "forecast_reference_time"));
    runtimeCoordVar.addAttribute(new ucar.nc2.Attribute(_Coordinate.AxisType, AxisType.RunTime.toString()));
    ncDataset.addVariable(null, runtimeCoordVar);
    if (debug) System.out.println("FmrcAggregation: added runtimeCoordVar " + runtimeCoordVar.getName());

    // add its data
    if (true) { // LOOK detect if we have the info
      ArrayObject.D1 runData = (ArrayObject.D1) Array.factory(DataType.STRING, new int[] {nruns});
      for (int j = 0; j < nestedDatasets.size(); j++) {
        Dataset dataset = (Dataset) nestedDatasets.get(j);
        runData.set(j, dataset.getCoordValueString());
      }
      runtimeCoordVar.setCachedData(runData, true);
    } else {
      runtimeCoordVar.setProxyReader( this);
    }

    // handle the 2D time coordinates and dimensions, for the case that we have a fmrcDefinition
    // there may be time coordinates that dont show up in the typical dataset
    if (fmrcDefinition != null) {

      List runSeq = fmrcDefinition.getRunSequences();
      for (int i = 0; i < runSeq.size(); i++) { // each runSeq generates a 2D time coordinate
        FmrcDefinition.RunSeq seq = (FmrcDefinition.RunSeq) runSeq.get(i);
        String timeDimName = seq.getName();

        // whats the maximum size ?
        int max_times = 0;
        for (int j = 0; j < nestedDatasets.size(); j++) {
          Dataset dataset = (Dataset) nestedDatasets.get(j);
          ForecastModelRunInventory.TimeCoord timeCoord = seq.findTimeCoordByRuntime(dataset.getCoordValueDate());
          double[] offsets = timeCoord.getOffsetHours();
          max_times = Math.max(max_times, offsets.length);
        }

        // create time dimension
        Dimension timeDim = new Dimension(timeDimName, max_times, true);
        ncDataset.removeDimension(null, timeDimName); // remove previous declaration, if any
        ncDataset.addDimension(null, timeDim);

        Dataset firstDataset = (Dataset) nestedDatasets.get(0);
        Date baseDate = firstDataset.getCoordValueDate();
        String desc = "Coordinate variable for " + timeDimName + " dimension";
        String units = "hours since " + formatter.toDateTimeStringISO(baseDate);

        String dims = getDimensionName() + " " + timeDimName;
        Variable newV = new VariableDS(ncDataset, null, null, timeDimName, DataType.DOUBLE, dims, desc, units);

        // do we already have the coordinate variable ?
        Variable oldV = ncDataset.getRootGroup().findVariable(timeDimName);
        if (null != oldV) {
          //NcMLReader.transferVariableAttributes(oldV, newV);
          //Attribute att = newV.findAttribute(_Coordinate.AliasForDimension);  // ??
          //if (att != null) newV.remove(att);
          ncDataset.removeVariable(null, timeDimName);
        }
        ncDataset.addVariable(null, newV);

        newV.addAttribute(new Attribute("units", units));
        newV.addAttribute(new Attribute("long_name", desc));
        newV.addAttribute(new Attribute("standard_name", "time"));
        newV.addAttribute(new ucar.nc2.Attribute(_Coordinate.AxisType, AxisType.Time.toString()));

        // compute the coordinates
        Array coordValues = calcTimeCoordinateFromDef(nruns, max_times, seq);
        newV.setCachedData(coordValues, true);
      }

      ncDataset.finish();
    }

    // promote all grid variables
    HashSet timeAxes = new HashSet();
    GridDataset gds = new ucar.nc2.dt.grid.GridDataset((NetcdfDataset) typical);
    List grids = gds.getGrids();
    for (int i = 0; i < grids.size(); i++) {
      GridDatatype grid = (GridDatatype) grids.get(i);
      Variable v = (Variable) grid.getVariable();

      // add new dimension
      String dims = dimName + " " + v.getDimensionsString();

      // construct new variable, replace old one
      VariableDS vagg = new VariableDS(ncDataset, null, null, v.getShortName(), v.getDataType(), dims, null, null);
      vagg.setProxyReader(this);
      NcMLReader.transferVariableAttributes(v, vagg);

      // we need to explicitly list the coordinate axes, because time coord is now 2D
      vagg.addAttribute(new Attribute(_Coordinate.Axes, dimName + " " + grid.getCoordinateSystem().getName()));
      vagg.addAttribute(new Attribute("coordinates", dimName + " " + grid.getCoordinateSystem().getName())); // CF

      ncDataset.removeVariable(null, v.getShortName());
      ncDataset.addVariable(null, vagg);
      if (debug) System.out.println("FmrcAggregation: added grid " + v.getName());

      // track the time axes
      GridCoordSystem gcc = grid.getCoordinateSystem();
      CoordinateAxis1D timeAxis = gcc.getTimeAxis1D();
      if (null != timeAxis)
        timeAxes.add(timeAxis);
    }


    // handle the 2D time coordinates and dimensions, for the case that we dont have a fmrcDefinition
    if (fmrcDefinition == null) { // no fmrcDefinition

      // promote the time coordinate(s) to 2D, read in values if we have to
      Iterator iter = timeAxes.iterator();
      while( iter.hasNext()) {
        CoordinateAxis1DTime v = (CoordinateAxis1DTime) iter.next();

        // construct new variable, replace old one
        String dims = dimName + " " + v.getDimensionsString();
        VariableDS vagg = new VariableDS(ncDataset, null, null, v.getShortName(), v.getDataType(), dims, null, null);
        NcMLReader.transferVariableAttributes(v, vagg);
        Attribute att = vagg.findAttribute(_Coordinate.AliasForDimension);
        if (att != null) vagg.remove(att);

        ncDataset.removeVariable(null, v.getShortName());
        ncDataset.addVariable(null, vagg);

        if (!timeUnitsChange)
          // Case 1: assume the units are all the same, so its just another agg variable
          vagg.setProxyReader(this);
        else {
          // Case 2: assume the time units differ for each nested file
          readTimeCoordinates( vagg, cancelTask);
        }

        if (debug) System.out.println("FmrcAggregation: promoted timeCoord " + v.getName());
        if (cancelTask != null && cancelTask.isCancel()) return;
      }
    }

    ncDataset.finish();
    makeProxies(typicalDataset, ncDataset);
    ncDataset.enhance();

    typical.close();
  }

  // we assume the variables are complete, but the time dimensions and values have to be recomputed
  protected void syncDataset(CancelTask cancelTask) throws IOException {
    buildCoords(cancelTask);

    // redo the aggregation dimension, makes things easier if you dont replace Dimension, just modify the length
    int nruns = getTotalCoords();
    String dimName = getDimensionName();
    Dimension aggDim = ncDataset.findDimension(dimName);
    aggDim.setLength( nruns);

    // recalc runtime array
    VariableDS runtimeCoord = (VariableDS) ncDataset.findVariable(dimName);
    runtimeCoord.setDimensions( runtimeCoord.getDimensionsString());
    if (true) { // LOOK detect if we have the info
      ArrayObject.D1 runData = (ArrayObject.D1) Array.factory(DataType.STRING, new int[] {nruns});
      for (int j = 0; j < nestedDatasets.size(); j++) {
        Dataset dataset = (Dataset) nestedDatasets.get(j);
        runData.set(j, dataset.getCoordValueString());
      }
      runtimeCoord.setCachedData(runData, true);
    }

    if (fmrcDefinition != null) {

      List runSeq = fmrcDefinition.getRunSequences();
      for (int i = 0; i < runSeq.size(); i++) { // each runSeq generates a 2D time coordinate
        FmrcDefinition.RunSeq seq = (FmrcDefinition.RunSeq) runSeq.get(i);
        String timeDimName = seq.getName();

        // whats the maximum size ?
        int max_times = 0;
        for (int j = 0; j < nestedDatasets.size(); j++) {
          Dataset dataset = (Dataset) nestedDatasets.get(j);
          ForecastModelRunInventory.TimeCoord timeCoord = seq.findTimeCoordByRuntime(dataset.getCoordValueDate());
          double[] offsets = timeCoord.getOffsetHours();
          max_times = Math.max(max_times, offsets.length);
        }

        // redo the time dimension, makes things easier if you dont replace Dimension, just modify the length
        Dimension timeDim = ncDataset.findDimension(timeDimName);
        timeDim.setLength(max_times);

        Dataset firstDataset = (Dataset) nestedDatasets.get(0);
        Date baseDate = firstDataset.getCoordValueDate();
        String units = "hours since " + formatter.toDateTimeStringISO(baseDate);

        VariableDS timeCoord = (VariableDS) ncDataset.findVariable(timeDimName);
        timeCoord.setDimensions( timeCoord.getDimensionsString());
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
    ArrayList timeAxes = new ArrayList();
    List vars = ncDataset.getVariables();
    for (int i = 0; i < vars.size(); i++) {
      VariableDS var = (VariableDS) vars.get(i);

      if (var instanceof CoordinateAxis) {
        CoordinateAxis axis = (CoordinateAxis) var;
        if (axis.getAxisType() == AxisType.Time)
          timeAxes.add( var);

        if (fmrcDefinition != null) // skip time coordinates when we have a fmrcDefinition, since they were already done
          continue;
      }

      if ((var.getRank() > 0) && var.getDimension(0).getName().equals(dimName)) {
        var.setDimensions( var.getDimensionsString()); // reset dimension
        var.setCachedData(null, false); // get rid of any cached data, since its now wrong

      } else {
        ProxyReader proxy = var.getProxyReader();
        if (proxy instanceof DatasetProxyReader)
          var.setProxyReader(typicalDatasetProxy);
      }

    }

    if (fmrcDefinition == null) {  // LOOK this is not right - need to reset the time lengths !!
      // recalc the time coordinate(s)
      Iterator iter = timeAxes.iterator();
      while( iter.hasNext()) {
        CoordinateAxis timeAxis = (CoordinateAxis) iter.next();

        if (timeUnitsChange) {
          // Case 2: assume the time units differ for each nested file
          readTimeCoordinates( timeAxis, cancelTask);
        }
      }
    }

  }

  private void readTimeCoordinates( VariableDS timeAxis, CancelTask cancelTask) throws IOException {
    ArrayList dateList = new ArrayList(); // List<java.util.Date[]>
    int maxTimes = 0;
    String units = null;

    for (int i = 0; i < nestedDatasets.size(); i++) {
      Dataset dataset = null;
      NetcdfDataset ncfile = null;
      try {
        dataset = (Dataset) nestedDatasets.get(i);
        ncfile = (NetcdfDataset) dataset.acquireFile(cancelTask);
        VariableDS v = (VariableDS) ncfile.findVariable( timeAxis.getName());
        if (v == null) {
          logger.warn("readTimeCoordinates: variable = "+timeAxis.getName()+" not found in file "+dataset.getLocation());
          return;
        }
        CoordinateAxis1DTime timeCoordVar = new CoordinateAxis1DTime( v, null);
        java.util.Date[] dates = timeCoordVar.getTimeDates();
        maxTimes = Math.max( maxTimes, dates.length);
        dateList.add( dates);

        if (i == 0)
          units = v.getUnitsString();

      } finally {
        if (ncfile != null) ncfile.close();
      }
      if (cancelTask != null && cancelTask.isCancel()) return;
    }

    int[] shape = timeAxis.getShape();
    int ntimes = shape[1];
    if (ntimes != maxTimes) {  //LOOK!!
      shape[1] = maxTimes;
      Dimension d = timeAxis.getDimension(1);
      d.setLength(maxTimes);
      timeAxis.setDimensions( timeAxis.getDimensionsString());
    }

    Array timeCoordVals = Array.factory(timeAxis.getDataType(), shape);
    Index ima = timeCoordVals.getIndex();
    timeAxis.setCachedData(timeCoordVals, false);

    // check if its a String or a udunit
    if (timeAxis.getDataType() == DataType.STRING) {

      for (int i = 0; i < dateList.size(); i++) {
        Date[] dates = (Date[]) dateList.get(i);
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
        Date[] dates = (Date[]) dateList.get(i);
        for (int j = 0; j < dates.length; j++) {
          Date date = dates[j];
          double val = du.makeValue(date);
          timeCoordVals.setDouble(ima.set(i,j), val);
        }
      }
    }

  }

  private Array calcTimeCoordinateFromDef(int nruns, int max_times, FmrcDefinition.RunSeq seq) {
        // compute the coordinates
    ArrayDouble.D2 coordValues = (ArrayDouble.D2) Array.factory(DataType.DOUBLE, new int[]{nruns, max_times});
    Date baseDate = null;
    for (int j = 0; j < nestedDatasets.size(); j++) {
      Dataset dataset = (Dataset) nestedDatasets.get(j);
      Date runTime = dataset.getCoordValueDate();
      if (baseDate == null)
        baseDate = runTime;
      double run_offset = ForecastModelRunInventory.getOffsetInHours(baseDate, runTime);

      ForecastModelRunInventory.TimeCoord timeCoord = seq.findTimeCoordByRuntime(runTime);
      double[] offsets = timeCoord.getOffsetHours();
      for (int k = 0; k < offsets.length; k++)
        coordValues.set(j, k, offsets[k] + run_offset);
      for (int k = offsets.length; k < max_times; k++)
        coordValues.set(j, k, Double.NaN);
    }

    return coordValues;
  }

}