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
  static public final String NETCDF4_COORDINATES  = "_Netcdf4Coordinates"; // only on the multi-dimensional coordinate variables of the netCDF model (2D chars)
                                                                           // appears to hold the dimension ids of the 2 dimensions
  static public final String NETCDF4_DIMID  = "_Netcdf4Dimid"; // on dimension scales, holds a scalar H5T_NATIVE_INT which is the (zero-based) dimension ID for this dimension.
                                                               // used to maintain creation order
  static public final String NETCDF4_STRICT  = "_nc3_strict";  // global - when using classic model
  static public final String NETCDF4_NON_COORD  = "_nc4_non_coord_";  // appended to variable when it conflicts with dimension scale


}

/*

////////////////////////////////////////////////////////////////////////////////////////////////
  /* 6/21/2013 new algorithm for dimensions.

    1. find all objects with all CLASS = "DIMENSION_SCALE", make into a dimension. use shape(0) as length. keep in order
    2. if also a variable (NAME != "This is a ...") then first dim = itself, second matches length, if muiltiple match, use :_Netcdf4Coordinates = 0, 3 and order of dimensions.
    3. use DIMENSION_LIST to assign dimensions to other variables.

    Examples:

root  :_nc3_strict = 1
  :title = "INSITE traffic weighting"
  :institution = "Forecast Impact and Quality Assessment Section : GSD/ESRL/NOAA : http://esrl.noaa.gov/fiqas/"
  :source = "INSITE"
  :contact = "Jennifer Mahoney : jennifer.mahoney@noaa.gov"
  :Conventions = "CF-1.4"
  :version = "FIQAS 1.1"

float weight(7,24,1209,);
  :long_name = "traffic weighting"
  :_FillValue = -1.0f
  :valid_range = 0.0f, 2500.0f
  :DIMENSION_LIST = "days", "hours", "jetways"

float max_weight(1209,);
  :long_name = "maximum traffic weighting for any day/hour"
  :_FillValue = -1.0f
  :valid_range = 0.0f, 2500.0f
  :DIMENSION_LIST = "jetways"

char jetways(1209,7,);
  :REFERENCE_LIST = null, null
  :CLASS = "DIMENSION_SCALE"
  :NAME = "jetways"
  :_Netcdf4Coordinates = 0, 3
  :long_name = "jetway segment identifier"

float hours(24,);
  :CLASS = "DIMENSION_SCALE"
  :NAME = "This is a netCDF dimension but not a netCDF variable.        24"
  :REFERENCE_LIST = null

float days(7,);
  :CLASS = "DIMENSION_SCALE"
  :NAME = "This is a netCDF dimension but not a netCDF variable.         7"
  :REFERENCE_LIST = null

float jetwaylen(7,);
  :CLASS = "DIMENSION_SCALE"
  :NAME = "This is a netCDF dimension but not a netCDF variable.         7"

////////////////////////////////////////
// example with unlimited dimensions
root
float time(0,);
  :REFERENCE_LIST = null
  :CLASS = "DIMENSION_SCALE"
  :NAME = "This is a netCDF dimension but not a netCDF variable.         0"

float lat(2,);
  :REFERENCE_LIST = null
  :CLASS = "DIMENSION_SCALE"
  :NAME = "This is a netCDF dimension but not a netCDF variable.         2"

float lon(3,);
  :REFERENCE_LIST = null
  :CLASS = "DIMENSION_SCALE"
  :NAME = "This is a netCDF dimension but not a netCDF variable.         3"

float surface_temperature(4,2,3,);
  :DIMENSION_LIST = "time", "lat", "lon"

*/
