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
package ucar.nc2.dt2.point;

import ucar.nc2.dt2.ProfileFeature;
import ucar.nc2.dt2.PointFeature;
import ucar.unidata.geoloc.LatLonPoint;

/**
 * Abstract superclass for implementations of ProfileFeature.
 * @author caron
 * @since Feb 29, 2008
 */
public abstract class ProfileFeatureImpl implements ProfileFeature {
  private LatLonPoint latlonPoint;

  public ProfileFeatureImpl( LatLonPoint latlonPoint) {
    this.latlonPoint = latlonPoint;
  }

  public LatLonPoint getLatLon() {
    return latlonPoint;
  }

  // All features in this collection have this feature type
  public Class getCollectionFeatureType() {
    return PointFeature.class;
  }
}
