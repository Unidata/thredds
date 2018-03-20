/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.unidata.geoloc;

import com.google.common.base.Preconditions;
import ucar.nc2.util.Misc;
import ucar.unidata.util.Format;

import java.util.Objects;
import java.util.StringTokenizer;

/**
 * Bounding box for latitude/longitude points.
 * This is a rectangle in lat/lon coordinates.
 * This class handles the longitude wrapping problem.
 * The Rectangle always starts from lowerLeft, goes east width degrees until upperRight
 * For latitude, lower < upper.
 * Since the longitude must be in the range +/-180., right may be less or greater than left.
 *
 * @author Russ Rew
 * @author John Caron
 */
public class LatLonRect {

  public static LatLonRect INVALID = new LatLonRect(LatLonPointImmutable.INVALID, LatLonPointImmutable.INVALID);

  /**
   * upper right corner
   */
  private LatLonPointImpl upperRight;

  /**
   * lower left corner
   */
  private LatLonPointImpl lowerLeft;

  /**
   * flag for dateline cross
   */
  private boolean crossDateline = false;

  /**
   * All longitudes are included
   */
  private boolean allLongitude = false;

  /**
   * width and initial longitude
   */
  private double width, lon0;

  /**
   * Construct a lat/lon bounding box from a point, and a delta lat, lon.
   * This disambiguates which way the box wraps around the globe.
   *
   * @param p1       one corner of the box
   * @param deltaLat delta lat from p1. (may be positive or negetive)
   * @param deltaLon delta lon from p1. (may be positive or negetive)
   */
  public LatLonRect(LatLonPoint p1, double deltaLat, double deltaLon) {
    init(p1, deltaLat, deltaLon);
  }

  private void init(LatLonPoint p1, double deltaLat, double deltaLon) {
    double lonmin, lonmax;
    double latmin = Math.min(p1.getLatitude(), p1.getLatitude() + deltaLat);
    double latmax = Math.max(p1.getLatitude(), p1.getLatitude() + deltaLat);

    double lonpt = p1.getLongitude();
    if (deltaLon > 0) {
      lonmin = lonpt;
      lonmax = lonpt + deltaLon;
      crossDateline = (lonmax > 180.0);
    } else {
      lonmax = lonpt;
      lonmin = lonpt + deltaLon;
      crossDateline = (lonmin < -180.0);
    }

    this.lowerLeft = new LatLonPointImpl(latmin, lonmin);
    this.upperRight = new LatLonPointImpl(latmax, lonmax);

    // these are an alternative way to view the longitude range
    this.width = Math.abs(deltaLon);
    this.lon0 = LatLonPointImpl.lonNormal(p1.getLongitude() + deltaLon / 2);
    this.allLongitude = (this.width >= 360.0);
  }

  /**
   * Construct a lat/lon bounding box from two points.
   * The order of longitude coord of the two points matters:
   * pt1.lon is always the "left" point, then points contained within the box
   * increase (unless crossing the Dateline, in which case they jump to -180, but
   * then start increasing again) until pt2.lon
   * The order of lat doesnt matter: smaller will go to "lower" point (further south)
   *
   * @param left  left corner
   * @param right right corner
   */
  public LatLonRect(LatLonPoint left, LatLonPoint right) {
    this(left, right.getLatitude() - left.getLatitude(),
        LatLonPointImpl.lonNormal360(right.getLongitude() - left.getLongitude()));
  }

  /**
   * Construct a lat/lon bounding box from a string.
   * @param spec "lat, lon, deltaLat, deltaLon"
   * @see #LatLonRect(LatLonPoint p1, double deltaLat, double deltaLon)
   */
  public LatLonRect(String spec) {
    StringTokenizer stoker = new StringTokenizer( spec, " ,");
    int n = stoker.countTokens();
    if (n != 4) throw new IllegalArgumentException("Must be 4 numbers = lat, lon, latWidth, lonWidth");
    double lat = Double.parseDouble(stoker.nextToken());
    double lon = Double.parseDouble(stoker.nextToken());
    double deltaLat = Double.parseDouble(stoker.nextToken());
    double deltaLon = Double.parseDouble(stoker.nextToken());

    init(new LatLonPointImpl(lat, lon), deltaLat, deltaLon);
  }


  /**
   * Copy Constructor
   *
   * @param r rectangle to copy
   */
  public LatLonRect(LatLonRect r) {
    this(r.getLowerLeftPoint(), r.getUpperRightPoint().getLatitude() - r.getLowerLeftPoint().getLatitude(), r.getWidth());
  }

  /**
   * Create a LatLonRect that covers the whole world.
   */
  public LatLonRect() {
    this(new LatLonPointImpl(-90, -180), 180, 360);
  }

  /**
   * Get the upper right corner of the bounding box.
   *
   * @return upper right corner of the bounding box
   */
  public LatLonPointImpl getUpperRightPoint() {
    return upperRight;
  }

  /**
   * Get the lower left corner of the bounding box.
   *
   * @return lower left corner of the bounding box
   */
  public LatLonPointImpl getLowerLeftPoint() {
    return lowerLeft;
  }

  /**
   * Get the upper left corner of the bounding box.
   *
   * @return upper left corner of the bounding box
   */
  public LatLonPointImpl getUpperLeftPoint() {
    return new LatLonPointImpl(upperRight.getLatitude(), lowerLeft.getLongitude());
  }

  /**
   * Get the lower left corner of the bounding box.
   *
   * @return lower left corner of the bounding box
   */
  public LatLonPointImpl getLowerRightPoint() {
    return new LatLonPointImpl(lowerLeft.getLatitude(), upperRight.getLongitude());
  }

  /**
   * Get whether the bounding box crosses the +/- 180 seam
   *
   * @return true if the bounding box crosses the +/- 180 seam
   */
  public boolean crossDateline() {
    return crossDateline;
  }

  // Exact comparison is needed in order to be consistent with hashCode().
  @Override public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    LatLonRect that = (LatLonRect) o;
    return Objects.equals(upperRight, that.upperRight) && Objects.equals(lowerLeft, that.lowerLeft);
  }

  @Override public int hashCode() {
    return Objects.hash(upperRight, lowerLeft);
  }

  /**
   * Returns the result of {@link #nearlyEquals(LatLonRect, double)}, with {@link Misc#defaultMaxRelativeDiffFloat}.
   */
  public boolean nearlyEquals(LatLonRect other) {
    return nearlyEquals(other, Misc.defaultMaxRelativeDiffFloat);
  }

  /**
   * Returns {@code true} if this rectangle is nearly equal to {@code other}. The "near equality" of corners is
   * determined using {@link LatLonPoint#nearlyEquals(LatLonPoint, double)}, with the specified maxRelDiff.
   *
   * @param other    the other rectangle to check.
   * @param maxRelDiff  the maximum {@link Misc#relativeDifference relative difference} that two corners may have.
   * @return {@code true} if this rectangle is nearly equal to {@code other}.
   */
  public boolean nearlyEquals(LatLonRect other, double maxRelDiff) {
    return this.getLowerLeftPoint() .nearlyEquals(other.getLowerLeftPoint(),  maxRelDiff) &&
           this.getUpperRightPoint().nearlyEquals(other.getUpperRightPoint(), maxRelDiff);
  }

  /**
   * return width of bounding box, always between 0 and 360 degrees.
   *
   * @return width of bounding box in degrees longitude
   */
  public double getWidth() {
    return width;
  }

  /**
   * return height of bounding box, always between 0 and 180 degrees.
   *
   * @return height of bounding box in degrees latitude
   */
  public double getHeight() {
    return getLatMax() - getLatMin();
  }

  /**
   * return center Longitude, always in the range +/-180
   *
   * @return center Longitude
   */
  public double getCenterLon() {
    return lon0;
  }

  /**
   * Get minimum longitude, aka "west" edge
   *
   * @return minimum longitude
   */
  public double getLonMin() {
    return lowerLeft.getLongitude();
  }

  /**
   * Get maximum longitude, aka "east" edge
   *
   * @return maximum longitude
   */
  public double getLonMax() {
    return lowerLeft.getLongitude() + width;
  }

  /**
   * Get minimum latitude, aka "south" edge
   *
   * @return minimum latitude
   */
  public double getLatMin() {
    return lowerLeft.getLatitude();
  }

  /**
   * Get maximum latitude, aka "north" edge
   *
   * @return maximum latitude
   */
  public double getLatMax() {
    return upperRight.getLatitude();
  }


  /**
   * Determine if a specified LatLonPoint is contained in this bounding box.
   *
   * @param p the specified point to be tested
   * @return true if point is contained in this bounding box
   */
  public boolean contains(LatLonPoint p) {
    return contains(p.getLatitude(), p.getLongitude());
  }

  /**
   * Determine if the given lat/lon point is contined inside this rectangle.
   *
   * @param lat lat of point
   * @param lon lon of point
   * @return true if the given lat/lon point is contined inside this rectangle
   */
  public boolean contains(double lat, double lon) {
    // check lat first
    double eps = 1.0e-9;
    if ((lat + eps < lowerLeft.getLatitude()) || (lat - eps > upperRight.getLatitude())) {
      return false;
    }

    if (allLongitude)
      return true;

    if (crossDateline) {
      // bounding box crosses the +/- 180 seam
      return ((lon >= lowerLeft.getLongitude()) || (lon <= upperRight.getLongitude()));
    } else {
      // check "normal" lon case
      return ((lon >= lowerLeft.getLongitude()) && (lon <= upperRight.getLongitude()));
    }
  }


  /**
   * Determine if this bounding box is contained in another LatLonRect.
   *
   * @param b the other box to see if it contains this one
   * @return true if b contained in this bounding box
   */
  public boolean containedIn(LatLonRect b) {
    return (b.getWidth() >= width) && b.contains(upperRight)
        && b.contains(lowerLeft);
  }

  /*
  * Determine if a specified LatLonRect intersects this
  * @param b the specified box to be tested
  *
  * @param p
  * @return true if b intersects this bounding box
  *
  * public boolean intersects(LatLonRect b) {
  *     if (b.getUpperRightPoint().getLatitude() < lowerLeft.getLatitude())
  * return false;
  *     if (b.getLowerLeftPoint().getLatitude() > upperRight.getLatitude())
  * return false;
  *
  * double blon0 = b.getCenterLon();
  * double normal = (blon0 + lon0) / 2;
  * if (Math.abs(blon0-lon0) > 180.0)
  * normal += 180;
  * blon0 = LatLonPoint.lonNormal(blon0, normal);
  * double mylon0 = LatLonPoint.lonNormal(lon0, normal);
  *
  *     if (blon0 + b.getWidth() < mylon0 - width)
  * return false;
  *     if (blon0 - b.getWidth() > mylon0 + width)
  * return false;
  *
  * return true;
  * }
  */

  /**
   * Extend the bounding box to contain this point
   *
   * @param p point to include
   */
  public void extend(LatLonPoint p) {
    if (contains(p))
      return;

    double lat = p.getLatitude();
    double lon = p.getLongitude();

    // lat is easy to deal with
    if (lat > upperRight.getLatitude()) {
      upperRight.setLatitude(lat);
    }
    if (lat < lowerLeft.getLatitude()) {
      lowerLeft.setLatitude(lat);
    }

    // lon is uglier
    if (allLongitude) {
      // do nothing
    } else if (crossDateline) {

      // bounding box crosses the +/- 180 seam
      double d1 = lon - upperRight.getLongitude();
      double d2 = lowerLeft.getLongitude() - lon;
      if ((d1 > 0.0) && (d2 > 0.0)) {  // needed ?
        if (d1 > d2) {
          lowerLeft.setLongitude(lon);
        } else {
          upperRight.setLongitude(lon);
        }
      }

    } else {

      // normal case
      if (lon > upperRight.getLongitude()) {
        if (lon - upperRight.getLongitude() > lowerLeft.getLongitude() - lon + 360) {
          crossDateline = true;
          lowerLeft.setLongitude(lon);
        } else {
          upperRight.setLongitude(lon);
        }
      } else if (lon < lowerLeft.getLongitude()) {
        if (lowerLeft.getLongitude() - lon > lon + 360.0 - upperRight.getLongitude()) {
          crossDateline = true;
          upperRight.setLongitude(lon);
        } else {
          lowerLeft.setLongitude(lon);
        }
      }
    }

    // recalc delta, center
    width = upperRight.getLongitude() - lowerLeft.getLongitude();
    lon0 = (upperRight.getLongitude() + lowerLeft.getLongitude()) / 2;
    if (crossDateline) {
      width += 360;
      lon0 -= 180;
    }

    this.allLongitude = this.allLongitude || (this.width >= 360.0);
  }

  /**
   * Extend the bounding box to contain the given rectangle
   *
   * @param r rectangle to include
   */
  public void extend(LatLonRect r) {
    Preconditions.checkNotNull(r);

    // lat is easy
    double latMin = r.getLatMin();
    double latMax = r.getLatMax();

    if (latMax > upperRight.getLatitude()) {
      upperRight.setLatitude(latMax);
    }
    if (latMin < lowerLeft.getLatitude()) {
      lowerLeft.setLatitude(latMin);
    }

    // lon is uglier
    if (allLongitude)
      return;

    // everything is reletive to current LonMin
    double lonMin = getLonMin();
    double lonMax = getLonMax();

    double nlonMin = LatLonPointImpl.lonNormal( r.getLonMin(), lonMin);
    double nlonMax = nlonMin + r.getWidth();
    lonMin = Math.min(lonMin, nlonMin);
    lonMax = Math.max(lonMax, nlonMax);

    width = lonMax - lonMin;
    allLongitude = width >= 360.0;
    if (allLongitude) {
      width = 360.0;
      lonMin = -180.0;
    } else {
      lonMin = LatLonPointImpl.lonNormal(lonMin);
    }
    
    lowerLeft.setLongitude(lonMin);
    upperRight.setLongitude(lonMin+width);
    lon0 = lonMin+width/2;
    crossDateline = lowerLeft.getLongitude() > upperRight.getLongitude();
  }

  /**
   * Create the instersection of this LatLon with the given one
   *
   * @param clip intersect with this
   * @return intersection, or null if there is no intersection
   */
  public LatLonRect intersect(LatLonRect clip) {
    double latMin = Math.max(getLatMin(), clip.getLatMin());
    double latMax = Math.min(getLatMax(), clip.getLatMax());
    double deltaLat = latMax - latMin;
    if (deltaLat < 0)
      return null;

    // lon as always is a pain : if not intersection, try +/- 360
    double lon1min = getLonMin();
    double lon1max = getLonMax();
    double lon2min = clip.getLonMin();
    double lon2max = clip.getLonMax();
    if (!intersect(lon1min, lon1max, lon2min, lon2max)) {
      lon2min = clip.getLonMin() + 360;
      lon2max = clip.getLonMax() + 360;
      if (!intersect(lon1min, lon1max, lon2min, lon2max)) {
        lon2min = clip.getLonMin() - 360;
        lon2max = clip.getLonMax() - 360;
      }
    }

    // we did our best to find an intersection
    double lonMin = Math.max(lon1min, lon2min);
    double lonMax = Math.min(lon1max, lon2max);
    double deltaLon = lonMax - lonMin;
    if (deltaLon < 0)
      return null;

    return new LatLonRect(new LatLonPointImpl(latMin, lonMin), deltaLat, deltaLon);
  }

  private boolean intersect(double min1, double max1, double min2, double max2) {
    double min = Math.max(min1, min2);
    double max = Math.min(max1, max2);
    return min < max;
  }


  /**
   * Return a String representation of this object.
   * <pre>eg: ll: 90.0S .0E+ ur: 90.0N .0E</pre>
   *
   * @return a String representation of this object.
   */
  public String toString() {
    return " ll: " + lowerLeft + "+ ur: " + upperRight;
  }


  /**
   * Return a String representation of this object.
   * <pre>lat= [-90.00,90.00] lon= [0.00,360.00</pre>
   *
   * @return a String representation of this object.
   */
  public String toString2() {
    return " lat= [" + Format.dfrac(getLatMin(), 2) + "," + Format.dfrac(getLatMax(), 2) +
        "] lon= [" + Format.dfrac(getLonMin(), 2) + "," + Format.dfrac(getLonMax(), 2) + "]";
  }
}
