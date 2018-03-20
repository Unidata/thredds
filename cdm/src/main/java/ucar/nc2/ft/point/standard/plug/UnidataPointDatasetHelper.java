/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.ft.point.standard.plug;

import ucar.nc2.*;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.time.CalendarDateUnit;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.constants.AxisType;
import ucar.nc2.constants._Coordinate;
import ucar.ma2.DataType;
import ucar.unidata.geoloc.LatLonRect;
import ucar.unidata.geoloc.LatLonPointImpl;

import java.util.List;

/**
 * Helper routines for point feature datasets using Unidata Conventions.
 *
 * @author caron
 * @since Feb 29, 2008
 */
public class UnidataPointDatasetHelper {

  static public CalendarDate getStartDate(NetcdfDataset ds, CalendarDateUnit timeUnit) {
    return getDate(ds, timeUnit, "time_coverage_start");
  }

  static public CalendarDate getEndDate(NetcdfDataset ds, CalendarDateUnit timeUnit) {
    return getDate(ds, timeUnit, "time_coverage_end");
  }

  static private CalendarDate getDate(NetcdfDataset ds, CalendarDateUnit timeUnit, String attName) {
    Attribute att = ds.findGlobalAttributeIgnoreCase(attName);
    if (null == att)
      throw new IllegalArgumentException("Must have a global attribute named "+attName);

    CalendarDate result;
    if (att.getDataType() == DataType.STRING) {
      result = CalendarDate.parseUdunitsOrIso(null, att.getStringValue());
      if (result == null && timeUnit != null) {
        double val = Double.parseDouble(att.getStringValue());
        result = timeUnit.makeCalendarDate(val);
      }
    } else if (timeUnit != null) {
      double val = att.getNumericValue().doubleValue();
      result = timeUnit.makeCalendarDate(val);

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
   * @param ds search in this dataset
   * @param a AxisType.LAT, LON, HEIGHT, or TIME
   * @return coordinate variable, or null if not found.
   */
  static public String getCoordinateName(NetcdfDataset ds, AxisType a) {
    List<Variable> varList = ds.getVariables();
    for (Variable v : varList) {
      if (v instanceof Structure) {
        List<Variable> vars = ((Structure) v).getVariables();
        for (Variable vs : vars) {
          String axisType = ds.findAttValueIgnoreCase(vs, _Coordinate.AxisType, null);
          if ((axisType != null) && axisType.equals(a.toString()))
            return vs.getShortName();
        }
      } else {
        String axisType = ds.findAttValueIgnoreCase(v, _Coordinate.AxisType, null);
        if ((axisType != null) && axisType.equals(a.toString()))
          return v.getShortName();
      }
    }

    if (a == AxisType.Lat)
      return findVariableName( ds, "latitude");

    if (a == AxisType.Lon)
      return findVariableName( ds, "longitude");

    if (a == AxisType.Time)
      return findVariableName( ds, "time");

    if (a == AxisType.Height) {
      Variable v = findVariable( ds, "altitude");
      if (null == v) v = findVariable( ds, "depth");
      if (v != null) return v.getShortName();
    }

    // I think the CF part is done by the CoordSysBuilder adding the _CoordinateAxisType attrinutes.
    return null;
  }

  /**
   * Tries to find the coordinate variable of the specified type, which has the specified dimension as its firsst dimension
   * @param ds search in this dataset
   * @param a AxisType.LAT, LON, HEIGHT, or TIME
   * @param dim must use this dimension
   * @return coordinate variable, or null if not found.
   */
  static public String getCoordinateName(NetcdfDataset ds, AxisType a, Dimension dim) {
    String name = getCoordinateName(ds, a);
    if (name == null) return null;

    Variable v = ds.findVariable(name);
    if (v == null) return null;

    if (v.isScalar()) return null;
    if (!v.getDimension(0).equals(dim)) return null;

    return name;
  }

  /**
   * Tries to find the coordinate variable of the specified type.
   * @param ds search in this dataset
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
      if (v != null) return v;
    }

    // I think the CF part is done by the CoordSysBuilder adding the _CoordinateAxisType attrinutes.
    return null;
  }

  static public String findVariableName(NetcdfFile ds, String name) {
    Variable result = findVariable(ds, name);
    return result == null ? null : result.getShortName();
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
    Dimension result = ds.findDimension(name); // LOOK use group
    if (result == null) {
      String aname = ds.findAttValueIgnoreCase(null, name+"Dimension", null);
      if (aname != null)
        result = ds.findDimension(aname); // LOOK use group
    }
    return result;
  }

  static public Dimension findObsDimension(NetcdfFile ds) {
    Dimension result = null;
    String aname = ds.findAttValueIgnoreCase(null, "observationDimension", null);
    if (aname != null)
      result = ds.findDimension(aname);   // LOOK use group
    if (result == null)
      result = ds.getUnlimitedDimension();    // LOOK use group
    return result;
  }
}
