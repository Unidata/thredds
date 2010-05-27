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

package ucar.grib;

import ucar.grid.GridParameter;


/**
 * Abstracts lookup functionality for grib1 and grib2.
 *
 * @deprecated
 */
public interface TableLookup {

  // from the GDS

  /**
   * Get the name of the Variable
   *
   * @param gds the gds record
   * @return GridName.
   */
  public String getGridName(Index.GdsRecord gds);

  /**
   * .
   *
   * @param gds the gds record
   * @return ShapeName.
   */
  public String getShapeName(Index.GdsRecord gds);

  // from the PDS

  /**
   * .
   *
   * @param gr the grib record
   * @return DisciplineName.
   */
  public String getDisciplineName(Index.GribRecord gr);

  /**
   * .
   *
   * @param gr the grib record
   * @return CategoryName.
   */
  public String getCategoryName(Index.GribRecord gr);

  /**
   * .
   *
   * @param gr the grib record
   * @return GridParameter.
   */
  public GridParameter getParameter(Index.GribRecord gr);

  /**
   * process type used to generate data.
   *
   * @param gr the grib record
   * @return String.
   */
  public String getTypeGenProcessName(Index.GribRecord gr);

  //public int getTypeEnsemble(Index.GribRecord gr);
  /**
   * Return an int array that uniquely specifies this parameter
   *
   * @param gr the grib record
   * @return _more_
   */
  public int[] getParameterId(Index.GribRecord gr);

  /**
   * .
   *
   * @param gr the grib record
   * @return ProductDefinitionName.
   */
  public String getProductDefinitionName(Index.GribRecord gr);

  /**
   * .
   *
   * @param gr the grib record
   * @return LevelName.
   */
  public String getLevelName(Index.GribRecord gr);

  /**
   * .
   *
   * @param gr the grib record
   * @return LevelDescription.
   */
  public String getLevelDescription(Index.GribRecord gr);

  /**
   * .
   *
   * @param gr the grib record
   * @return LevelUnit.
   */
  public String getLevelUnit(Index.GribRecord gr);

  /**
   * .
   *
   * @return FirstTimeRangeUnitName.
   */
  public String getFirstTimeRangeUnitName();

  /**
   * .
   *
   * @return FirstCenterName.
   */
  public String getFirstCenterName();

  /**
   * .
   *
   * @return FirstSubcenterId.
   */
  public int getFirstSubcenterId();

  /**
   * .
   *
   * @return FirstProductStatusName.
   */
  public String getFirstProductStatusName();

  /**
   * .
   *
   * @return FirstProductTypeName.
   */
  public String getFirstProductTypeName();

  /**
   * .
   *
   * @return FirstSignificanceOfRTName.
   */
  public String getFirstSignificanceOfRTName();

  /**
   * .
   *
   * @return FirstBaseTime.
   */
  public java.util.Date getFirstBaseTime();

  /**
   * .
   *
   * @param gds the gds record
   * @return is this a LatLon Grid
   */
  public boolean isLatLon(Index.GdsRecord gds);

  /**
   * if vertical level should be made into a coordinate; dont do for surface, 1D levels.
   *
   * @param gr the grib record
   * @return is this a VerticalCoordinate
   */
  public boolean isVerticalCoordinate(Index.GribRecord gr);

  /**
   * .
   *
   * @param gr the grib record
   * @return is this positive up level
   */
  public boolean isPositiveUp(Index.GribRecord gr);

  /**
   * projection enumerations.
   */
  public int PolarStereographic = 1;

  /**
   * _more_
   */
  public int LambertConformal = 2;

  /**
   * _more_
   */
  public int Mercator = 3;

  /**
   * _more_
   */
  public int UTM = 4;

  /**
   * _more_
   */
  public int AlbersEqualArea = 5;

  /**
   * _more_
   */
  public int LambertAzimuthEqualArea = 6;

  /**
   * _more_
   */
  public int Orthographic = 7;

  /**
   * _more_
   */
  public int GaussianLatLon = 8;

  public int RotatedLatLon = 10;
  // return one of the above

  /**
   * .
   *
   * @param gds the gds record
   * @return ProjectionType
   */
  public int getProjectionType(Index.GdsRecord gds);

  /**
   * .
   *
   * @return FirstMissingValue.
   */
  public float getFirstMissingValue();

}