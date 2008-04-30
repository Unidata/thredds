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

import ucar.nc2.ft.TrajectoryFeature;
import ucar.nc2.constants.FeatureType;

/**
 * @author caron
 * @since Mar 26, 2008
 */
public abstract class TrajectoryFeatureImpl extends PointCollectionImpl implements TrajectoryFeature {
  private int npts;

  public TrajectoryFeatureImpl( String name, int npts) {
    super(name);
    this.npts = npts;
  }

  @Override
  public FeatureType getCollectionFeatureType() {
    return FeatureType.TRAJECTORY;
  }

  @Override
  public int size() {
    return npts;
  }
}