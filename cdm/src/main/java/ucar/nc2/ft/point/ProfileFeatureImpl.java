/*
 * Copyright 1997-2008 Unidata Program Center/University Corporation for
 * Atmospheric Research, P.O. Box 3000, Boulder, CO 80307,
 * support@unidata.ucar.edu.
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or (at
 * your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser
 * General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package ucar.nc2.ft.point;

import ucar.nc2.ft.ProfileFeature;
import ucar.nc2.constants.FeatureType;
import ucar.unidata.geoloc.LatLonPoint;

/**
 * Abstract superclass for implementations of ProfileFeature.
 *
 * @author caron
 * @since Feb 29, 2008
 */
public abstract class ProfileFeatureImpl extends PointCollectionImpl implements ProfileFeature {
  private LatLonPoint latlonPoint;
  private int npts;

  public ProfileFeatureImpl( String name, LatLonPoint latlonPoint, int npts) {
    super(name);
    this.latlonPoint = latlonPoint;
    this.npts = npts;
  }

  public LatLonPoint getLatLon() {
    return latlonPoint;
  }
  
  public Object getId() {
    return getName();
  }

  @Override
  public FeatureType getCollectionFeatureType() {
    return FeatureType.PROFILE;
  }

  public int getNumberPoints() {
    return npts;
  }

  protected void setNumberPoints(int npts) {
    this.npts = npts;
  }
}
