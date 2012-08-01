package ucar.nc2.iosp.netcdf4;

/**
 * Special netcdf-4 specific stuff. used by both Java (H5header) and JNA interface (Nc4Iosp)
 *
 * @author caron
 * @since 7/31/12
 *
 * see "http://www.unidata.ucar.edu/software/netcdf/docs/netcdf_4_spec.html"
 * @see "http://www.unidata.ucar.edu/software/netcdf/docs/"
 */
public class Nc4 {

  // special attribute names used by netcdf4 library
  static public final String NETCDF4_COORDINATES  = "_Netcdf4Coordinates"; // the multi-dimensional coordinate variables of the netCDF model  HUH ??
  static public final String NETCDF4_DIMID  = "_Netcdf4Dimid"; // on dimension scales, holds a scalar H5T_NATIVE_INT which is the (zero-based) dimension ID for this dimension.
  static public final String NETCDF4_STRICT  = "_nc3_strict";  // global - when using classic model

  static public final String NETCDF4_NON_COORD  = "_nc4_non_coord_";  // appended to variable when it conflicts with dimension scale


}
