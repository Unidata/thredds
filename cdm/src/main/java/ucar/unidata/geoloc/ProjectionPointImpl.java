/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
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

  @Override
  public boolean nearlyEquals(ProjectionPoint other, double maxRelDiff) {
    return Misc.nearlyEquals(x, other.getX(), maxRelDiff) && Misc.nearlyEquals(y, other.getY(), maxRelDiff);
  }

  // Exact comparison is needed in order to be consistent with hashCode().
  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ProjectionPointImpl that = (ProjectionPointImpl) o;
    if (Double.compare(that.x, x) != 0) {
      return false;
    }
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
