/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.unidata.geoloc;

/**
 * An immutable {@link LatLonPoint}.
 *
 * @author caron
 * @since 7/29/2014
 */
public class LatLonPointImmutable extends LatLonPointImpl {
  public static final LatLonPointImmutable INVALID = new LatLonPointImmutable(
          Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY);

  public LatLonPointImmutable(double lat, double lon) {
    this.lat = latNormal(lat);
    this.lon = lonNormal(lon);
  }

  public LatLonPointImmutable(LatLonPoint pt) {
    this(pt.getLatitude(), pt.getLongitude());
  }

  /**
   * @throws UnsupportedOperationException because instances of this class are meant to be immutable.
   */
  @Override
  public void setLongitude(double lon) throws UnsupportedOperationException {
    throw new UnsupportedOperationException("Instances of this class are meant to be immutable.");
  }

  /**
   * @throws UnsupportedOperationException because instances of this class are meant to be immutable.
   */
  @Override
  public void setLatitude(double lat) {
    throw new UnsupportedOperationException("Instances of this class are meant to be immutable.");
  }
}
