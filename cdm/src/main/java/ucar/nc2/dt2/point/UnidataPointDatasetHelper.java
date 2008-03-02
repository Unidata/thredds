/*
 * Copyright 1997-2008 Unidata Program Center/University Corporation for
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

package ucar.nc2.dt2.point;

import ucar.nc2.*;
import ucar.nc2.units.DateUnit;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.constants.AxisType;
import ucar.nc2.constants._Coordinate;
import ucar.ma2.DataType;
import ucar.unidata.geoloc.LatLonRect;
import ucar.unidata.geoloc.LatLonPointImpl;

import java.util.List;
import java.util.Date;

/**
 * Helper routines for point feature datasets using Unidata Conventions.
 *
 * @author caron
 * @since Feb 29, 2008
 */
public class UnidataPointDatasetHelper {

  static public Date getStartDate(NetcdfDataset ds, DateUnit timeUnit) {
    return getDate(ds, timeUnit, "time_coverage_start");
  }

  static public Date getEndDate(NetcdfDataset ds, DateUnit timeUnit) {
    return getDate(ds, timeUnit, "time_coverage_end");
  }

  static private Date getDate(NetcdfDataset ds, DateUnit timeUnit, String attName) {
    Attribute att = ds.findGlobalAttributeIgnoreCase(attName);
    if (null == att)
      throw new IllegalArgumentException("Must have a global attribute named "+attName);

    Date result;
    if (att.getDataType() == DataType.STRING) {
      result = DateUnit.getStandardOrISO( att.getStringValue());
      if (result == null) {
        double val = Double.parseDouble(att.getStringValue());
        result = timeUnit.makeDate(val);
      }
    } else if (timeUnit != null) {
      double val = att.getNumericValue().doubleValue();
      result = timeUnit.makeDate(val);

    } else {
      throw new IllegalArgumentException(attName+" must be a ISO or udunit date string");
    }

    return result;
  }

  static public LatLonRect getBoundingBox(NetcdfDataset ds) {
    double lat_max = getAttAsDouble( ds, "geospatial_lat_max");
    double lat_min = getAttAsDouble( ds, "geospatial_lat_min");
    double lon_max = getAttAsDouble( ds, "geospatial_lon_max");
    double lon_min = getAttAsDouble( ds, "geospatial_lon_min");

    return new LatLonRect(new LatLonPointImpl(lat_min, lon_min), lat_max-lat_min, lon_max-lon_min);
  }

  static private double getAttAsDouble( NetcdfDataset ds, String attname) {
    Attribute att = ds.findGlobalAttributeIgnoreCase(attname);
    if (null == att)
      throw new IllegalArgumentException("Must have a "+attname+" global attribute");

    if (att.getDataType() == DataType.STRING) {
      return Double.parseDouble( att.getStringValue());
    } else {
      return att.getNumericValue().doubleValue();
    }
  }

  /**
   * Tries to find the coordinate variable of the specified type.
   * @param ds look in this dataset
   * @param a AxisType.LAT, LON, HEIGHT, or TIME
   * @return coordinate variable, or null if not found.
   */
  static public Variable getCoordinate(NetcdfDataset ds, AxisType a) {
    List<Variable> varList = ds.getVariables();
    for (Variable v : varList) {
      if (v instanceof Structure) {
        //System.out.println( "v is a Structure" );
        List<Variable> vars = ((Structure) v).getVariables();
        for (Variable vs : vars) {
          //System.out.println( "vs =" + vs.getShortName() );
          String axisType = ds.findAttValueIgnoreCase(vs, _Coordinate.AxisType, null);
          if ((axisType != null) && axisType.equals(a.toString()))
            return vs;
        }
      } else {
        String axisType = ds.findAttValueIgnoreCase(v, _Coordinate.AxisType, null);
        if ((axisType != null) && axisType.equals(a.toString()))
          return v;
      }
    }

    if (a == AxisType.Lat)
      return findVariable( ds, "latitude");

    if (a == AxisType.Lon)
      return findVariable( ds, "longitude");

    if (a == AxisType.Time)
      return findVariable( ds, "time");

    if (a == AxisType.Height) {
      Variable v = findVariable( ds, "altitude");
      if (null == v) v = findVariable( ds, "depth");
      return v;
    }

    // I think the CF part is done by the CoordSysBuilder adding the _CoordinateAxisType attrinutes.
    return null;
  }

  static public Variable findVariable(NetcdfFile ds, String name) {
    Variable result = ds.findVariable(name);
    if (result == null) {
      String aname = ds.findAttValueIgnoreCase(null, name+"_coordinate", null);
      if (aname != null)
        result = ds.findVariable(aname);
      else {
        aname = ds.findAttValueIgnoreCase(null, name+"_variable", null);
        if (aname != null)
          result = ds.findVariable(aname);
      }
    }
    return result;
  }

  static public Dimension findDimension(NetcdfFile ds, String name) {
    Dimension result = ds.findDimension(name);
    if (result == null) {
      String aname = ds.findAttValueIgnoreCase(null, name+"Dimension", null);
      if (aname != null)
        result = ds.findDimension(aname);
    }
    return result;
  }
}
