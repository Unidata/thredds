/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.dt.point;

import ucar.nc2.dt.StationObsDatatype;

/**
 * Abstract superclass for implemenation of StationObsDatatype.
 * Concrete subclass must implement getData();
 *
 * @deprecated use ucar.nc2.ft.point
 * @author caron
 */


public abstract class StationObsDatatypeImpl extends PointObsDatatypeImpl implements StationObsDatatype {
  protected ucar.unidata.geoloc.Station station;

  public StationObsDatatypeImpl() {
  }

  public StationObsDatatypeImpl( ucar.unidata.geoloc.Station station, double obsTime, double nomTime) {
    super(station, obsTime,  nomTime);
    this.station = station;
  }

  public ucar.unidata.geoloc.Station getStation() { return station; }
}
