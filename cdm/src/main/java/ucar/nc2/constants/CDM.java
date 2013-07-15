/*
 * Copyright (c) 1998 - 2011. University Corporation for Atmospheric Research/Unidata
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

package ucar.nc2.constants;

import java.nio.charset.Charset;

/**
 * CDM constants.
 *
 * @author caron
 * @since 12/20/11
 */
public interface CDM {
  public static final Charset utf8Charset = Charset.forName("UTF-8");

  // structural
  public static final String CHUNK_SIZE = "_ChunkSize";
  public static final String COMPRESS = "_Compress";
  public static final String COMPRESS_DEFLATE = "deflate";

  // from the Netcdf Users Guide
  // http://www.unidata.ucar.edu/software/netcdf/docs/netcdf.html#Attribute-Conventions
  public static final String ABBREV = "abbreviation";
  public static final String ADD_OFFSET = "add_offset";
  public static final String CONVENTIONS = "Conventions";
  public static final String DESCRIPTION = "description";
  public static final String FILL_VALUE = "_FillValue";
  public static final String HISTORY = "history";
  public static final String LONG_NAME = "long_name";
  public static final String MISSING_VALUE = "missing_value";
  public static final String SCALE_FACTOR = "scale_factor";
  public static final String TIME_INTERVAL = "time_interval";
  public static final String TITLE = "title";
  public static final String UNITS = "units";
  public static final String UNSIGNED = "_Unsigned";
  public static final String VALID_RANGE = "valid_range";

  // UACDD
  // http://www.unidata.ucar.edu/software/netcdf-java/formats/DataDiscoveryAttConvention.html
  public static final String LAT_MIN = "geospatial_lat_min";
  public static final String LON_MIN = "geospatial_lon_min";
  public static final String LAT_MAX = "geospatial_lat_max";
  public static final String LON_MAX = "geospatial_lon_max";
  public static final String TIME_START = "time_coverage_start";
  public static final String TIME_END = "time_coverage_end";


  // staggering for _Coordinate.Stagger
  public static final String ARAKAWA_E = "Arakawa-E";

  // misc
  public static final String FILE_FORMAT = "file_format";
  public static final String LAT_UNITS = "degrees_north";
  public static final String LON_UNITS = "degrees_east";

}
