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

import ucar.nc2.dt2.PointObsFeature;
import ucar.nc2.dt2.EarthLocationImpl;
import ucar.nc2.dt2.Obs1DFeature;
import ucar.ma2.StructureData;

import java.util.Date;

/**
 * @author caron
 * @since Feb 18, 2008
 */
public class PointObsFeatureAdapter implements PointObsFeature {
  private Obs1DFeature feature;
  private StructureData sdata;

  public PointObsFeatureAdapter( Obs1DFeature feature, StructureData sdata) {
    this.feature = feature;
    this.sdata = sdata;
  }

  public Object getId() {
    return sdata.hashCode();
  }

  public double getObservationTime() {
    return feature.getObservationTime(sdata);
  }

  public Date getObservationTimeAsDate() {
    return feature.getObservationTimeAsDate(sdata);
  }

  public double getNominalTime() {
    return getObservationTime();
  }

  public Date getNominalTimeAsDate() {
    return getObservationTimeAsDate();
  }

  public EarthLocationImpl getLocation() {
    return new EarthLocationImpl( feature.getLatitude(sdata), feature.getLongitude(sdata), feature.getZcoordinate(sdata));
  }

  public StructureData getData() {
    return sdata;
  }


}
