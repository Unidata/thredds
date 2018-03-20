/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.dt.point;

import ucar.nc2.dt.PointObsDatatype;
import ucar.unidata.geoloc.EarthLocation;

/**
 * Abstract superclass for implemenation of PointObsDatatype.
 * Concrete subclass must implement getData();
 *
 * @deprecated use ucar.nc2.ft.point
 * @author caron
 */


public abstract class PointObsDatatypeImpl implements PointObsDatatype, Comparable {
  protected ucar.unidata.geoloc.EarthLocation location;
  protected double obsTime, nomTime;

  public PointObsDatatypeImpl() {
  }

  public PointObsDatatypeImpl( ucar.unidata.geoloc.EarthLocation location, double obsTime, double nomTime) {
    this.location = location;
    this.obsTime = obsTime;
    this.nomTime = nomTime;
  }

  public ucar.unidata.geoloc.EarthLocation getLocation() { return location; }
  public double getNominalTime() { return nomTime; }
  public double getObservationTime() { return obsTime; }

  public int compareTo(Object o) {
    PointObsDatatypeImpl other = (PointObsDatatypeImpl) o;
    if (obsTime < other.obsTime) return -1;
    if (obsTime > other.obsTime) return 1;
    return 0;
  }
}