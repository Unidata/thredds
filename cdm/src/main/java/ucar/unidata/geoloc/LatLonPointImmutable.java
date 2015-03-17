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

/**
 * An immutable LatLonPoint
 *
 * @author caron
 * @since 7/29/2014
 */
public class LatLonPointImmutable implements LatLonPoint {
  public static final LatLonPointImmutable INVALID = new LatLonPointImmutable(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY);
  private final double lat, lon;

  public LatLonPointImmutable(double lat, double lon) {
    this.lat = lat;
    this.lon = lon;
  }

  public LatLonPointImmutable(LatLonPoint pt) {
    this.lat = pt.getLatitude();
    this.lon = pt.getLongitude();
  }

  @Override
  public double getLongitude() {
    return lon;
  }

  @Override
  public double getLatitude() {
    return lat;
  }

  @Override
  public boolean equals(LatLonPoint pt) {
    if (Double.compare(pt.getLatitude(), lat) != 0) return false;
    if (Double.compare(pt.getLongitude(), lon) != 0) return false;
    return true;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof LatLonPoint)) return false;
    LatLonPoint that = (LatLonPoint) o;
    return equals(that);
  }

  @Override
  public int hashCode() {
    int result;
    long temp;
    temp = Double.doubleToLongBits(lat);
    result = (int) (temp ^ (temp >>> 32));
    temp = Double.doubleToLongBits(lon);
    result = 31 * result + (int) (temp ^ (temp >>> 32));
    return result;
  }
}
