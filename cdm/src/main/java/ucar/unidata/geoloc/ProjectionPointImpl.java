/*
 * Copyright 1998-2014 University Corporation for Atmospheric Research/Unidata
 *
 *   Portions of this software were developed by the Unidata Program at the
 *   University Corporation for Atmospheric Research.
 *
 *   Access and use of this software shall impose the following obligations
 *   and understandings on the user. The user is granted the right, without
 *   any fee or cost, to use, copy, modify, alter, enhance and distribute
 *   this software, and any derivative works thereof, and its supporting
 *   documentation for any purpose whatsoever, provided that this entire
 *   notice appears in all copies of the software, derivative works and
 *   supporting documentation.  Further, UCAR requests that the user credit
 *   UCAR/Unidata in any publications that result from the use of this
 *   software or in any product that includes this software. The names UCAR
 *   and/or Unidata, however, may not be used in any advertising or publicity
 *   to endorse or promote any products or commercial entity unless specific
 *   written permission is obtained from UCAR/Unidata. The user also
 *   understands that UCAR/Unidata is not obligated to provide the user with
 *   any support, consulting, training or assistance of any kind with regard
 *   to the use, operation and performance of this software nor to provide
 *   the user with any updates, revisions, new versions or "bug fixes."
 *
 *   THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 *   IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *   WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *   DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 *   INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 *   FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 *   NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 *   WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */
package ucar.unidata.geoloc;

import ucar.nc2.util.Misc;
import ucar.unidata.util.Format;

import java.util.Formatter;

/**
 * Implementation of ProjectionPoint
 *
 * @author John Caron
 * @see ProjectionPoint
 */
public class ProjectionPointImpl implements ProjectionPoint, java.io.Serializable {
  private double x, y;

  /**
   * Default constructor, initialized to 0,0
   */
  public ProjectionPointImpl() {
    this(0.0, 0.0);
  }

  /**
   * Constructor that copies Point2D values into this.
   *
   * @param pt point to copy
   *
  public ProjectionPointImpl(Point2D pt) {
    super(pt.getX(), pt.getY());
  } */

  /**
   * Constructor that copies ProjectionPoint values into this.
   *
   * @param pt point to copy
   */
  public ProjectionPointImpl(ProjectionPoint pt) {
    this.x = pt.getX();
    this.y = pt.getY();
  }

  /**
   * Constructor, initialized to x, y
   *
   * @param x x coordinate
   * @param y y coordinate
   */
  public ProjectionPointImpl(double x, double y) {
    this.x = x;
    this.y = y;
  }

  public double getX() {
    return x;
  }

  public double getY() {
    return y;
  }

  public void setX(double x) {
    this.x = x;
  }

  public void setY(double y) {
    this.y = y;
  }

  // must be exact compare to be consistent with hashCode
  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    ProjectionPointImpl that = (ProjectionPointImpl) o;
    if (Double.compare(that.x, x) != 0) return false;
    return Double.compare(that.y, y) == 0;
  }

  @Override
  public int hashCode() {
    int result;
    long temp;
    temp = Double.doubleToLongBits(x);
    result = (int) (temp ^ (temp >>> 32));
    temp = Double.doubleToLongBits(y);
    result = 31 * result + (int) (temp ^ (temp >>> 32));
    return result;
  }

  /**
   * Returns true if this represents the same point as pt, using Misc.closeEnough.
   *
   * @param pt2 point to check against
   * @return true if this represents the same point as pt2.
   */
  public boolean equals(ProjectionPoint pt2) {
    return Misc.closeEnough(getX(), pt2.getX()) &&  Misc.closeEnough(getY(), pt2.getY());
  }

  /**
   * nicely format this point
   *
   * @return nicely formatted point
   */
  public String toString() {
    return Format.d(getX(), 4) + " " + Format.d(getY(), 4);
  }

  public void toString(Formatter f) {
    f.format("x=%f y=%f ", getX(), getY());
  }

  // convenience setting routines

  /**
   * set x,y location from  pt
   *
   * @param pt point to use for values
   */
  public void setLocation(ProjectionPoint pt) {
    setLocation(pt.getX(), pt.getY());
  }

  public void setLocation(double x, double y) {
       this.x = x;
       this.y = y;
   }


  /**
   * set x,y location from  pt
   *
   * @param pt point to use for values
   *
  public void setLocation(Point2D pt) {
    setLocation(pt.getX(), pt.getY());
  } */

  /**
   * See if either coordinate is +/- infinite. This happens sometimes
   * in projective geometry.
   *
   * @return true if either coordinate is +/- infinite.
   */
  public boolean isInfinite() {
    return (x == java.lang.Double.POSITIVE_INFINITY)
        || (x == java.lang.Double.NEGATIVE_INFINITY)
        || (y == java.lang.Double.POSITIVE_INFINITY)
        || (y == java.lang.Double.NEGATIVE_INFINITY);
  }

  /**
   * See if either coordinate in <code>pt</code> is +/- infinite.
   * This happens sometimes in projective geometry.
   *
   * @param pt point to check
   * @return true if either coordinate is +/- infinite.
   */
  static public boolean isInfinite(ProjectionPoint pt) {
    return (pt.getX() == java.lang.Double.POSITIVE_INFINITY)
        || (pt.getX() == java.lang.Double.NEGATIVE_INFINITY)
        || (pt.getY() == java.lang.Double.POSITIVE_INFINITY)
        || (pt.getY() == java.lang.Double.NEGATIVE_INFINITY);
  }
}
