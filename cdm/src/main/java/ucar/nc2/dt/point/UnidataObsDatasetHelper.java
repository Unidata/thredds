package ucar.nc2.dt.point;

import ucar.nc2.Variable;
import ucar.nc2.Dimension;
import ucar.nc2.Attribute;
import ucar.nc2.Structure;
import ucar.nc2.units.DateUnit;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.AxisType;
import ucar.ma2.DataType;
import ucar.unidata.geoloc.LatLonRect;
import ucar.unidata.geoloc.LatLonPoint;
import ucar.unidata.geoloc.LatLonPointImpl;

import java.util.List;
import java.util.Date;
import java.util.ArrayList;
import java.io.IOException;

/**
 *
 * @author caron
 * @version $Revision$ $Date$
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

    return new LatLonRect(new LatLonPointImpl(lat_min, lon_min), new LatLonPointImpl(lat_max, lon_max));
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
    List varList = ds.getVariables();
    for (int i = 0; i < varList.size(); i++) {
      Variable v = (Variable) varList.get(i);
      if( v instanceof Structure ) {
         //System.out.println( "v is a Structure" );
          ArrayList vars = (ArrayList)((Structure)v).getVariables();
          for( int j = 0; j < vars.size(); j++ ) {
            Variable vs = (Variable) vars.get( j );
            //System.out.println( "vs =" + vs.getShortName() );
            String axisType = ds.findAttValueIgnoreCase(vs, "_CoordinateAxisType", null);
            if ((axisType != null) && axisType.equals(a.toString()))
              return vs;
          }
       } else {
         String axisType = ds.findAttValueIgnoreCase(v, "_CoordinateAxisType", null);
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

  static public Variable findVariable(NetcdfDataset ds, String name) {
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

  static public Dimension findDimension(NetcdfDataset ds, String name) {
    Dimension result = ds.findDimension(name);
    if (result == null) {
      String aname = ds.findAttValueIgnoreCase(null, name+"Dimension", null);
      if (aname != null)
        result = ds.findDimension(aname);
    }
    return result;
  }
}
