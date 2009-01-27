/*
 * Copyright 1998-2009 University Corporation for Atmospheric Research/Unidata
 *
 * Portions of this software were developed by the Unidata Program at the
 * University Corporation for Atmospheric Research.
 *
 * Access and use of this software shall impose the following obligations
 * and understandings on the user. The user is granted the right, without
 * any fee or cost, to use, copy, modify, alter, enhance and distribute
 * this software, and any derivative works thereof, and its supporting
 * documentation for any purpose whatsoever, provided that this entire
 * notice appears in all copies of the software, derivative works and
 * supporting documentation.  Further, UCAR requests that the user credit
 * UCAR/Unidata in any publications that result from the use of this
 * software or in any product that includes this software. The names UCAR
 * and/or Unidata, however, may not be used in any advertising or publicity
 * to endorse or promote any products or commercial entity unless specific
 * written permission is obtained from UCAR/Unidata. The user also
 * understands that UCAR/Unidata is not obligated to provide the user with
 * any support, consulting, training or assistance of any kind with regard
 * to the use, operation and performance of this software nor to provide
 * the user with any updates, revisions, new versions or "bug fixes."
 *
 * THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 * INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 * FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 * NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 * WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */

package ucar.nc2.dt.point;

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
 * Helper routines for  station/point datasets
 *
 * @author caron
 */
public class UnidataObsDatasetHelper {

  static public Date getStartDate(NetcdfDataset ds) {
    Attribute att = ds.findGlobalAttributeIgnoreCase("time_coverage_start");
    if (null == att)
      throw new IllegalArgumentException("Must have a time_coverage_start global attribute");

    if (att.getDataType() == DataType.STRING) {
      return DateUnit.getStandardOrISO( att.getStringValue());
    } else {
      throw new IllegalArgumentException("time_coverage_start must be a ISO or udunit date string");
    }
  }

  static public Date getEndDate(NetcdfDataset ds) {
    Attribute att = ds.findGlobalAttributeIgnoreCase("time_coverage_end");
    if (null == att)
      throw new IllegalArgumentException("Must have a time_coverage_end global attribute");

    Date result;
    if (att.getDataType() == DataType.STRING) {
      result = DateUnit.getStandardOrISO( att.getStringValue());
    } else {
      throw new IllegalArgumentException("time_coverage_end must be a ISO or udunit date string");
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
