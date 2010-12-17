/*
 *
 *
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

/**
 * User: rkambic
 * Date: Jul 13, 2009
 * Time: 11:21:26 AM
 */

/**
 * A interface for handling Grib1 and Grib2 GDS variables from a byte[].
 */
public interface GribGDSVariablesIF {

  /**
   * scale factor for Lat/Lon variables in degrees.
   */
  public static final float tenToNegSix = (float) (1 / 1000000.0);
  public static final float tenToSix = (float)  1000000.0;

  /**
   * scale factor for dx and dy variables plus others
   */
  public static final float tenToNegThree = (float) (1 / 1000.0);
  public static final float tenToThree = (float) 1000.0;

  /**
   * GDS as a byte[]
   *
   * @return GDS bytes
   */
  public byte[] getGDSBytes();

  // getters for ProductDefinitions  Variables

  //  Length of GDS

  public int getLength();

  /**
   * Number of this section .
   *
   * @return section number
   */
  public int getSection();

  /**
   * source of grid definition.
   *
   * @return source
   */
  public int getSource();

  /**
   * number of data points .
   *
   * @return numberPoints
   */
  public int getNumberPoints();

  /**
   * olon > 0 is a quasi regular grid.
   *
   * @return olon
   */
  public int getOlon();

  /**
   * are extreme points in the quasi regular grid.
   *
   * @return iolon
   */
  public int getIolon();

  /**
   * number of points in each parallel for quasi grids.
   *
   * @return olonPts
   */
  //public int[] getOlonPoints();

  /**
   * Max number of points in parallel for quasi grids.
   *
   * @return maxPts
   */
  //public int getMaxPts();

  /**
   * Grid Definition Template Number .
   *
   * @return gdtn
   */
  public int getGdtn();

  /**
   * Grid name .
   *
   * @return gridName
   */
  //public String getName();

  /**
   * .
   *
   * @return shape as a int
   */
  public int getShape();

  /**
   * .
   *
   * @return shapeName as a String
   */
  //public String getShapeName();

  /**
   * .
   *
   * @return EarthRadius as a float
   */
  public float getEarthRadius();

  /**
   * .
   *
   * @return MajorAxis as a float
   */
  public float getMajorAxis();

  /**
   * .
   *
   * @return MinorAxis as a float
   */
  public float getMinorAxis();

  /**
   * Get number of grid columns.
   *
   * @return number of grid columns
   */
  public int getNx();

  /**
   * Get number of grid rows.
   *
   * @return number of grid rows.
   */
  public int getNy();

  /**
   * .
   *
   * @return Angle as a int
   */
  public int getAngle();

  /**
   * .
   *
   * @return Subdivisionsangle as a int
   */
  public int getSubDivisions();

  /**
   * .
   *
   * @return La1 as a float
   */
  public float getLa1();

  /**
   * .
   *
   * @return Lo1 as a float
   */
  public float getLo1();

  /**
   * .
   *
   * @return Resolution as a int
   */
  public int getResolution();

  /**
   * .
   *
   * @return La2 as a float
   */
  public float getLa2();

  /**
   * .
   *
   * @return Lo2 as a float
   */
  public float getLo2();

  /**
   * .
   *
   * @return Lad as a float
   */
  public float getLaD();

  /**
   * .
   *
   * @return Lov as a float
   */
  public float getLoV();

  /**
   * Get x-increment/distance between two grid points.
   *
   * @return x-increment
   */
  public float getDx();

  /**
   * Get y-increment/distance between two grid points.
   *
   * @return y-increment
   */
  public float getDy();

  /**
   * grid units
   *
   * @return grid_units
   */
  public String getGridUnits();

  /**
   * .
   *
   * @return ProjectionCenter as a int
   */
  public int getProjectionFlag();

  /**
   * Get scan mode.
   *
   * @return scan mode
   */
  public int getScanMode();

  /**
   * .
   *
   * @return Latin1 as a float
   */
  public float getLatin1();

  /**
   * .
   *
   * @return Latin2 as a float
   */
  public float getLatin2();

  /**
   * .
   *
   * @return SpLat as a float
   */
  public float getSpLat();

  /**
   * .
   *
   * @return SpLon as a float
   */
  public float getSpLon();

  /**
   * .
   *
   * @return Rotationangle as a float
   */
  public float getRotationAngle();

  /**
   * .
   *
   * @return PoleLat as a float
   */
  public float getPoleLat();

  /**
   * .
   *
   * @return PoleLon as a float
   */
  public float getPoleLon();

  /**
   * .
   *
   * @return Factor as a float
   */
  public float getStretchingFactor();

  /**
   * .
   *
   * @return Np as a int
   */
  public int getNp();

  /**
   * .
   *
   * @return J as a float
   */
  //public float getJ();

  /**
   * .
   *
   * @return K as a float
   */
  //public float getK();

  /**
   * .
   *
   * @return M as a float
   */
  //public float getM();

  /**
   * .
   *
   * @return Method as a int
   */
  //public int getMethod();

  /**
   * .
   *
   * @return Mode as a int
   */
  //public int getMode();

  /**
   * .
   *
   * @return Lap as a float
   */
  public float getLap();

  /**
   * .
   *
   * @return Lop as a float
   */
  public float getLop();

  /**
   * .
   *
   * @return Xp as a float
   */
  public float getXp();

  /**
   * .
   *
   * @return Yp as a float
   */
  public float getYp();

  /**
   * .
   *
   * @return Xo as a float
   */
  public float getXo();

  /**
   * .
   *
   * @return Yo as a float
   */
  public float getYo();

  /**
   * .
   *
   * @return Altitude as a float
   */
  //public float getAltitude();

  /**
   * .
   *
   * @return N2 as a int
   */
  //public int getN2();

  /**
   * .
   *
   * @return N3 as a int
   */
  //public int getN3();

  /**
   * .
   *
   * @return Ni as a int
   */
  //public int getNi();

  /**
   * .
   *
   * @return Nd as a int
   */
  //public int getNd();

  /**
   * .
   *
   * @return Position as a int
   */
  //public int getPosition();

  /**
   * .
   *
   * @return Order as a int
   */
  //public int getOrder();

  /**
   * .
   *
   * @return Nb as a float
   */
  //public float getNb();

  /**
   * .
   *
   * @return Nr as a float
   */
  public float getNr();

  /**
   * .
   *
   * @return Dstart as a float
   */
  //public float getDstart();

  /**
   * .
   *
   * @return CheckSum as a String
   */
  //public String getCheckSum();

  /**
   * .
   *
   * @return gdskey as a int
   */
  public int getGdsKey();

}
