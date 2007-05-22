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
package ucar.nc2.dt.grid;

import ucar.unidata.geoloc.LatLonRect;
import ucar.unidata.geoloc.Projection;
import ucar.unidata.geoloc.ProjectionPointImpl;
import ucar.unidata.geoloc.LatLonPointImpl;
import ucar.unidata.geoloc.projection.LatLonProjection;
import ucar.nc2.*;
import ucar.nc2.units.DateFormatter;
import ucar.nc2.dt.GridDatatype;
import ucar.nc2.dt.GridCoordSystem;
import ucar.nc2.dataset.*;
import ucar.nc2.dataset.conv._Coordinate;
import ucar.ma2.Range;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.DataType;
import ucar.ma2.Array;

import java.io.IOException;
import java.util.*;
import java.text.ParseException;

import thredds.datatype.DateRange;

/**
 * @author caron
 */
public class NetcdfCFWriter {

  public void makeFile(String location, ucar.nc2.dt.GridDataset gds, List<String> gridList,
          LatLonRect llbb, DateRange range,
          boolean addLatLon,
          int stride_xy, int stride_z, int stride_time) throws IOException, InvalidRangeException {


    FileWriter writer = new FileWriter(location, false);
    NetcdfDataset ncd = (NetcdfDataset) gds.getNetcdfFile();

    // global attributes
    for (Attribute att : gds.getGlobalAttributes())
      writer.writeGlobalAttribute(att);

    writer.writeGlobalAttribute(new Attribute("Conventions", "CF-1.0"));
    writer.writeGlobalAttribute(new Attribute("History",
            "Translated to CF-1.0 Conventions by Netcdf-Java CDM (NetcdfCFWriter)\n" +
                    "Original Dataset = " + gds.getLocationURI() + "; Translation Date = " + new Date()));

    ArrayList<Variable> varList = new ArrayList<Variable>();
    ArrayList<String> varNameList = new ArrayList<String>();
    ArrayList<CoordinateAxis> axisList = new ArrayList<CoordinateAxis>();

    // add each desired Grid to the new file
    for (String gridName : gridList) {
      if (varNameList.contains(gridName))
        continue;

      varNameList.add(gridName);

      GridDatatype grid = gds.findGridDatatype(gridName);
      GridCoordSystem gcsOrg = grid.getCoordinateSystem();

      // make subset if needed
      Range timeRange = null;
      if (range != null) {
        CoordinateAxis1DTime timeAxis = gcsOrg.getTimeAxis1D();
        int startIndex = timeAxis.findTimeIndexFromDate(range.getStart().getDate());
        int endIndex = timeAxis.findTimeIndexFromDate(range.getEnd().getDate());
        timeRange = new Range(startIndex, endIndex);
      }

      if ((llbb != null) || (null != timeRange)) {
        grid = grid.makeSubset(timeRange, null, llbb, 1, 1, 1);
      }

      Variable gridV = (Variable) grid.getVariable();
      varList.add(gridV);

      // add coordinate axes
      GridCoordSystem gcs = grid.getCoordinateSystem();
      List<CoordinateAxis> axes = gcs.getCoordinateAxes();
      for (int j = 0; j < axes.size(); j++) {
        CoordinateAxis axis = axes.get(j);
        if (!varNameList.contains(axis.getName())) {
          varNameList.add(axis.getName());
          varList.add(axis); // LOOK dont we have to subset these ??
          axisList.add(axis);
        }
      }

      // add coordinate transform variables
      List ctList = gcs.getCoordinateTransforms();
      for (int j = 0; j < ctList.size(); j++) {
        CoordinateTransform ct = (CoordinateTransform) ctList.get(j);
        Variable v = ncd.findVariable(ct.getName());
        if (!varNameList.contains(ct.getName()) && (null != v)) {
          varNameList.add(ct.getName());
          varList.add(v);
        }
      }

      // optionaal lat/lon
      if (addLatLon) {
        Projection proj = gcs.getProjection();
        if ((null != proj) && !(proj instanceof LatLonProjection)) {
          addLatLon2D(ncd, varList, proj, gcs.getXHorizAxis(), gcs.getYHorizAxis());
          addLatLon = false;
        }
      }
    }
    writer.writeVariables(varList);

    // now add CF annotataions as needed - dont change original ncd or gds
    NetcdfFileWriteable ncfile = writer.getNetcdf();
    Group root = ncfile.getRootGroup();
    for (String gridName : gridList) {
      GridDatatype grid = gds.findGridDatatype(gridName);
      Variable newV = root.findVariable(gridName);

      // annotate Variable for CF
      StringBuffer sbuff = new StringBuffer();
      GridCoordSystem gcs = grid.getCoordinateSystem();
      List axes = gcs.getCoordinateAxes();
      for (int j = 0; j < axes.size(); j++) {
        Variable axis = (Variable) axes.get(j);
        sbuff.append(axis.getName() + " ");
      }
      if (addLatLon)
        sbuff.append("lat lon");
      newV.addAttribute(new Attribute("coordinates", sbuff.toString()));

      // looking for coordinate transform variables
      List ctList = gcs.getCoordinateTransforms();
      for (int j = 0; j < ctList.size(); j++) {
        CoordinateTransform ct = (CoordinateTransform) ctList.get(j);
        Variable v = ncd.findVariable(ct.getName());
        if (ct.getTransformType() == TransformType.Projection)
          newV.addAttribute(new Attribute("grid_mapping", v.getName()));
      }
    }

    for (CoordinateAxis axis : axisList) {
      Variable newV = root.findVariable(axis.getName());
      if ((axis.getAxisType() == AxisType.Height) || (axis.getAxisType() == AxisType.Pressure) || (axis.getAxisType() == AxisType.GeoZ)) {
        if (null != axis.getPositive())
          newV.addAttribute(new Attribute("positive", axis.getPositive()));
      }
      if (axis.getAxisType() == AxisType.Lat) {
        newV.addAttribute(new Attribute("units", "degrees_north"));
        newV.addAttribute(new Attribute("standard_name", "latitude"));
      }
      if (axis.getAxisType() == AxisType.Lon) {
        newV.addAttribute(new Attribute("units", "degrees_east"));
        newV.addAttribute(new Attribute("standard_name", "longitude"));
      }

      // newV.addAttribute(new Attribute(_Coordinate.AxisType, axis.getAxisType().toString())); // cheating
    }

    // LOOK not dealing with crossing the seam

    writer.finish(); // this writes the data to the new file.
  }

  private void addLatLon2D(NetcdfFile ncfile, List<Variable> varList, Projection proj,
          CoordinateAxis xaxis, CoordinateAxis yaxis) throws IOException {

    double[] xData = (double[]) xaxis.read().get1DJavaArray(double.class);
    double[] yData = (double[]) yaxis.read().get1DJavaArray(double.class);

    Variable latVar = new Variable(ncfile, null, null, "lat");
    latVar.setDataType(DataType.DOUBLE);
    latVar.setDimensions("y x");
    latVar.addAttribute(new Attribute("units", "degrees_north"));
    latVar.addAttribute(new Attribute("long_name", "latitude coordinate"));
    latVar.addAttribute(new Attribute("standard_name", "latitude"));
    latVar.addAttribute(new Attribute(_Coordinate.AxisType, AxisType.Lat.toString()));

    Variable lonVar = new Variable(ncfile, null, null, "lon");
    lonVar.setDataType(DataType.DOUBLE);
    lonVar.setDimensions("y x");
    lonVar.addAttribute(new Attribute("units", "degrees_east"));
    lonVar.addAttribute(new Attribute("long_name", "longitude coordinate"));
    lonVar.addAttribute(new Attribute("standard_name", "longitude"));
    lonVar.addAttribute(new Attribute(_Coordinate.AxisType, AxisType.Lon.toString()));

    int nx = xData.length;
    int ny = yData.length;

    // create the data
    ProjectionPointImpl projPoint = new ProjectionPointImpl();
    LatLonPointImpl latlonPoint = new LatLonPointImpl();
    double[] latData = new double[nx * ny];
    double[] lonData = new double[nx * ny];
    for (int i = 0; i < ny; i++) {
      for (int j = 0; j < nx; j++) {
        projPoint.setLocation(xData[j], yData[i]);
        proj.projToLatLon(projPoint, latlonPoint);
        latData[i * nx + j] = latlonPoint.getLatitude();
        lonData[i * nx + j] = latlonPoint.getLongitude();
      }
    }
    Array latDataArray = Array.factory(DataType.DOUBLE.getClassType(), new int[]{ny, nx}, latData);
    latVar.setCachedData(latDataArray, false);

    Array lonDataArray = Array.factory(DataType.DOUBLE.getClassType(), new int[]{ny, nx}, lonData);
    lonVar.setCachedData(lonDataArray, false);

    varList.add(latVar);
    varList.add(lonVar);
  }

  public static void main(String args[]) throws IOException, InvalidRangeException, ParseException {
    String fileIn = "C:/data/ncmodels/NAM_CONUS_80km_20051206_0000.nc";
    String fileOut = "C:/temp3/cf3.nc";

    ucar.nc2.dt.GridDataset gds = ucar.nc2.dt.grid.GridDataset.open(fileIn);

    NetcdfCFWriter writer = new NetcdfCFWriter();

    List<String> gridList = new ArrayList<String>();
    gridList.add("RH");
    gridList.add("T");

    DateFormatter format = new DateFormatter();
    Date start = format.isoDateTimeFormat("2005-12-06T18:00:00Z");
    Date end = format.isoDateTimeFormat("2005-12-07T18:00:00Z");

    writer.makeFile(fileOut, gds, gridList,
          new LatLonRect(new LatLonPointImpl(37,-109), 400, 7),
          new DateRange(start,end),
          true,
          1, 1, 1);

  }


}

