/*
 * Copyright 1998-2015 John Caron and University Corporation for Atmospheric Research/Unidata
 *
 *  Portions of this software were developed by the Unidata Program at the
 *  University Corporation for Atmospheric Research.
 *
 *  Access and use of this software shall impose the following obligations
 *  and understandings on the user. The user is granted the right, without
 *  any fee or cost, to use, copy, modify, alter, enhance and distribute
 *  this software, and any derivative works thereof, and its supporting
 *  documentation for any purpose whatsoever, provided that this entire
 *  notice appears in all copies of the software, derivative works and
 *  supporting documentation.  Further, UCAR requests that the user credit
 *  UCAR/Unidata in any publications that result from the use of this
 *  software or in any product that includes this software. The names UCAR
 *  and/or Unidata, however, may not be used in any advertising or publicity
 *  to endorse or promote any products or commercial entity unless specific
 *  written permission is obtained from UCAR/Unidata. The user also
 *  understands that UCAR/Unidata is not obligated to provide the user with
 *  any support, consulting, training or assistance of any kind with regard
 *  to the use, operation and performance of this software nor to provide
 *  the user with any updates, revisions, new versions or "bug fixes."
 *
 *  THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 *  IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *  WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *  DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 *  INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 *  FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 *  NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 *  WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */

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

  // This is a persistent attribute added in netcdf-c-4.4.1-RC2. It'll look something like:
  //     _NCProperties = "version=1|netcdflibversion=4.4.1|hdf5libversion=1.8.17"
  static public final String NETCDF4_NC_PROPERTIES = "_NCProperties";
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
