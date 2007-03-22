// $Id: $
/*
 * Copyright 1997-2004 Unidata Program Center/University Corporation for
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
import ucar.nc2.dt.GridDatatype;
import ucar.nc2.dt.GridCoordSystem;
import ucar.nc2.dataset.*;
import ucar.nc2.dataset.conv._Coordinate;
import ucar.ma2.Range;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.DataType;
import ucar.ma2.Array;

import java.io.IOException;
import java.io.File;
import java.util.*;

/**
 * @author john
 */
public class NetcdfCFWriter {

  private NetcdfFile makeFile(String location, GridDataset gds, List gridList,
                              LatLonRect llbb,
                              boolean hasTime, double time_start, double time_end,
                              boolean addLatLon,
                              int stride_xy, int stride_z, int stride_time) throws IOException, InvalidRangeException {


    NetcdfFileWriteable result = NetcdfFileWriteable.createNew(location, false);
    NetcdfDataset ncd = (NetcdfDataset) gds.getNetcdfFile();

    // global attributes
    List glist = ncd.getGlobalAttributes();
    for (int i = 0; i < glist.size(); i++) {
      Attribute att = (Attribute) glist.get(i);

      if (att.isArray())
        result.addGlobalAttribute(att.getName(), att.getValues());
      else if (att.isString())
        result.addGlobalAttribute(att.getName(), att.getStringValue());
      else
        result.addGlobalAttribute(att.getName(), att.getNumericValue());
    }

    result.addGlobalAttribute("Conventions", "CF-1.0");
    result.addGlobalAttribute(new Attribute("History", "GridDatatype extracted from dataset " + ncd.getLocation()));

    ArrayList varList = new ArrayList();
    ArrayList varNameList = new ArrayList();

    for (int i = 0; i < gridList.size(); i++) {

      String gridName = (String) gridList.get(i);
      if (varNameList.contains(gridName))
        continue;
      varNameList.add(gridName);

      GridDatatype grid = gds.findGridDatatype(gridName);
      GridCoordSystem gcsOrg = grid.getCoordinateSystem();

      Range timeRange = null;
      if (hasTime) {
        CoordinateAxis1D timeAxis = gcsOrg.getTimeAxis1D();
        int startIndex = timeAxis.findCoordElement(time_start);
        int endIndex = timeAxis.findCoordElement(time_end);
        timeRange = new Range(startIndex, endIndex);
      }

      if ((llbb != null) || (null != timeRange)) {
        grid = grid.makeSubset(timeRange, null, llbb, 1, 1, 1);
      }

      Variable gridV = (Variable) grid.getVariable();
      varList.add(gridV);

      GridCoordSystem gcs = grid.getCoordinateSystem();
      List axes = gcs.getCoordinateAxes();
      for (int j = 0; j < axes.size(); j++) {
        Variable axis = (Variable) axes.get(j);
        if (!varNameList.contains(axis.getName())) {
          varNameList.add(axis.getName());
          varList.add(axis);
        }
      }

      // looking for coordinate transform variables
      List ctList = gcs.getCoordinateTransforms();
      for (int j = 0; j < ctList.size(); j++) {
        CoordinateTransform ct = (CoordinateTransform) ctList.get(j);
        Variable v = ncd.findVariable(ct.getName());
        if (!varNameList.contains(ct.getName()) && (null != v)) {
          varNameList.add(ct.getName());
          varList.add(v);
        }
      }

      if (addLatLon) {
        Projection proj = gcs.getProjection();
        if ((null != proj) && !(proj instanceof LatLonProjection)) {
          addLatLon2D(ncd, varList, proj, gcs.getXHorizAxis(), gcs.getYHorizAxis());
          addLatLon = false;
        }
      }
    }

    // add dimensions, variables 
    HashMap dimHash = new HashMap();
    for (int i = 0; i < varList.size(); i++) {
      Variable v = (Variable) varList.get(i);

      List dimvList = v.getDimensions();
      for (int j = 0; j < dimvList.size(); j++) {
        Dimension dim = (Dimension) dimvList.get(j);
        if (null == dimHash.get(dim.getName())) {
          Dimension newDim = result.addDimension(dim.getName(), dim.isUnlimited() ? -1 : dim.getLength(),
              dim.isShared(), dim.isUnlimited(), dim.isVariableLength());
          dimHash.put(newDim.getName(), newDim);
        }
      }

      Variable newvar = result.addVariable(v.getName(), v.getDataType(), v.getDimensionsString());
      List attList = v.getAttributes();
      for (int j = 0; j < attList.size(); j++)
        result.addVariableAttribute(v.getName(), (Attribute) attList.get(j));
    }

    result.finish();
    return result;
  }

  private void addLatLon2D (NetcdfFile ncfile, List varList, Projection  proj,
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
}