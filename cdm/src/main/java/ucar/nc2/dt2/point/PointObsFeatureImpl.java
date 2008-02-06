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

import ucar.nc2.dt2.EarthLocation;
import ucar.nc2.dt2.PointObsFeature;
import ucar.nc2.units.DateUnit;

import java.util.Date;

/**
 * Abstract superclass for implemenation of PointObsFeature.
 * Concrete subclass must implement getData();
 *
 * @author caron
 */


public abstract class PointObsFeatureImpl implements PointObsFeature, Comparable<PointObsFeature> {
  protected EarthLocation location;
  protected double obsTime, nomTime;
  protected DateUnit timeUnit;

  public PointObsFeatureImpl() {
  }

  public PointObsFeatureImpl( EarthLocation location, double obsTime, double nomTime, DateUnit timeUnit) {
    this.location = location;
    this.obsTime = obsTime;
    this.nomTime = nomTime;
    this.timeUnit = timeUnit;
  }

  public EarthLocation getLocation() { return location; }
  public double getNominalTime() { return nomTime; }
  public double getObservationTime() { return obsTime; }

  public ucar.nc2.units.DateUnit getTimeUnits() { return timeUnit; }
  public Date getObservationTimeAsDate() {
    return timeUnit.makeDate( getObservationTime());
  }

  public Date getNominalTimeAsDate() {
    return timeUnit.makeDate( getNominalTime());
  }

  public int compareTo(PointObsFeature other) {
    if (obsTime < other.getObservationTime()) return -1;
    if (obsTime > other.getObservationTime()) return 1;
    return 0;
  }
}