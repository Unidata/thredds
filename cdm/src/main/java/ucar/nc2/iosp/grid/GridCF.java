/*
 * Copyright (c) 1998 - 2010. University Corporation for Atmospheric Research/Unidata
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
/**
 * User: rkambic
 * Date: Feb 24, 2010
 * Time: 2:33:40 PM
 */

package ucar.nc2.iosp.grid;

/**
 *  A centralized place to store CF conventions for the Grid Iosp
 */
public class GridCF {

  /**
   * Radius of spherical earth
   */
  public static final String EARTH_RADIUS = "earth_radius";

  /**
   * major axis of earth
   */
  public static final String SEMI_MAJOR_AXIS  = "semi_major_axis";

  /**
   * minor axis of earth
   */
  public static final String SEMI_MINOR_AXIS  = "semi_minor_axis";


  /**
   * grid_mapping_name
   */
  public static final String GRID_MAPPING_NAME  = "grid_mapping_name";


 /**
   * earth_shape
   */
  public static final String EARTH_SHAPE  = "earth_shape";

 /**
   * standard_parallel
   */
  public static final String STANDARD_PARALLEL  = "standard_parallel";

  /**
   * longitude_of_central_meridian
   */
  public static final String LONGITUDE_OF_CENTRAL_MERIDIAN  = "longitude_of_central_meridian";

  /**
   * latitude_of_projection_origin
   */
  public static final String LATITUDE_OF_PROJECTION_ORIGIN  = "latitude_of_projection_origin";

  /**
   * longitude_of_projection_origin
   */
  public static final String LONGITUDE_OF_PROJECTION_ORIGIN  = "longitude_of_projection_origin";

  /**
   * straight_vertical_longitude_from_pole
   */
  public static final String STRAIGHT_VERTICAL_LONGITUDE_FROM_POLE  = "straight_vertical_longitude_from_pole";

  /**
   * scale_factor_at_projection_origin
   */
  public static final String SCALE_FACTOR_AT_PROJECTION_ORIGIN  = "scale_factor_at_projection_origin";


}
