/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.dt;

/**
 * A collection of observations at one time and at one station ( = named location)
 *
 * @deprecated use ucar.nc2.ft.*
 * @author caron
 */
public interface StationObsDatatype extends ucar.nc2.dt.PointObsDatatype, Comparable {

  /**
   * Station location of the observation
   * @return Station location of the observation
   */
  public ucar.unidata.geoloc.Station getStation();
}
