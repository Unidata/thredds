// $Id: $
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

import ucar.nc2.dataset.*;
import ucar.nc2.dataset.conv._Coordinate;
import ucar.nc2.dt.fmrc.FmrcDefinition;
import ucar.nc2.dt.fmrc.ForecastModelRunInventory;
import ucar.nc2.dt.GridDataset;
import ucar.nc2.dt.GridDatatype;
import ucar.nc2.dt.GridCoordSystem;
import ucar.nc2.util.CancelTask;
import ucar.nc2.*;
import ucar.ma2.DataType;
import ucar.ma2.ArrayObject;
import ucar.ma2.Array;

import java.io.IOException;
import java.util.List;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;

/**
 * Class Description.
 *
 * @author caron
 * @version $Revision$ $Date$
 */
public class AggregationFmrcHourly extends AggregationFmrc {

  public AggregationFmrcHourly(NetcdfDataset ncd, String dimName, String typeName, String recheckS) {
    super(ncd, dimName, typeName, recheckS);
  }


  protected void buildDataset(boolean isNew, CancelTask cancelTask) throws IOException {



    /* buildCoords(cancelTask);

    // open a "typical"  nested dataset and copy it to newds
    Dataset typicalDataset = getTypicalDataset();
    NetcdfFile typical =  typicalDataset.acquireFile(null);
    NcMLReader.transferDataset(typical, ncDataset, null); // isNew ? null : new MyReplaceVariableCheck());

    // some additional global attributes
    Group root = ncDataset.getRootGroup();
    root.addAttribute(new Attribute("Conventions", "CF-1.0, "+ _Coordinate.Convention));
    root.addAttribute(new Attribute("cdm_data_type", thredds.catalog.DataType.GRID.toString()));

    // create runtime aggregation dimension
    String dimName = getDimensionName();
    int nruns = getTotalCoords(); // same as  nestedDatasets.size()
    Dimension aggDim = new Dimension(dimName, nruns, true);
    ncDataset.removeDimension(null, dimName); // remove previous declaration, if any
    ncDataset.addDimension(null, aggDim);

    // create runtime aggregation coordinate variable
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

    // work with a GridDataset
    NetcdfDataset typicalDS;
    if (typical instanceof NetcdfDataset)
      typicalDS = (NetcdfDataset) typical;
    else
      typicalDS = new NetcdfDataset( typical);
    if (!typicalDS.isEnhanced())
      typicalDS.enhance();

    GridDataset gds = new ucar.nc2.dt.grid.GridDataset(typicalDS);

    // handle the 2D time coordinates and dimensions
    // for the case that we have a fmrcDefinition, there may be time coordinates that dont show up in the typical dataset
    if (fmrcDefinition != null) {

      List runSeq = fmrcDefinition.getRunSequences();
      for (int i = 0; i < runSeq.size(); i++) { // each runSeq generates a 2D time coordinate
        FmrcDefinition.RunSeq seq = (FmrcDefinition.RunSeq) runSeq.get(i);
        String timeDimName = seq.getName();

        // whats the maximum size ?
        boolean isRagged = false;
        int max_times = 0;
        for (int j = 0; j < nestedDatasets.size(); j++) {
          Dataset dataset = (Dataset) nestedDatasets.get(j);
          ForecastModelRunInventory.TimeCoord timeCoord = seq.findTimeCoordByRuntime(dataset.getCoordValueDate());
          double[] offsets = timeCoord.getOffsetHours();
          max_times = Math.max(max_times, offsets.length);
          if (max_times != offsets.length)
            isRagged = true;
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
        if (isRagged)
          newV.addAttribute(new ucar.nc2.Attribute("missing_value", new Double(Double.NaN)));

        // compute the coordinates
        Array coordValues = calcTimeCoordinateFromDef(nruns, max_times, seq);
        newV.setCachedData(coordValues, true);
      }

      ncDataset.finish();

    } else {
      // for the case that we dont have a fmrcDefinition

      // LOOK how do we set the length of the time dimension(s), if its ragged?
      // Here we are just using the typical dataset !!!
      // For now, we dont handle ragged time coordinates.

      // find time axes
      HashSet timeAxes = new HashSet();
      List grids = gds.getGrids();
      for (int i = 0; i < grids.size(); i++) {
        GridDatatype grid = (GridDatatype) grids.get(i);
        GridCoordSystem gcc = grid.getCoordinateSystem();
        CoordinateAxis1D timeAxis = gcc.getTimeAxis1D();
        if (null != timeAxis)
          timeAxes.add(timeAxis);
      }

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

    // promote all grid variables
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
    }

    ncDataset.finish();
    makeProxies(typicalDataset, ncDataset);
    ncDataset.enhance();

    typical.close(); */
  }


}
