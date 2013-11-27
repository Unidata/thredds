package ucar.nc2.grib.grib2.builder;

import ucar.nc2.grib.grib2.Grib2Record;

/**
 * Describe
 *
 * @author caron
 * @since 11/24/13
 */
public interface Coordinate {
  public Object extract(Grib2Record r);
}
