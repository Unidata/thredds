/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.dt;

import java.util.Date;

/**
 * A collection of observations at one time and location.
 *
 * @deprecated use ucar.nc2.ft.*
 * @author caron
 */
public interface PointObsDatatype {

  /**
   * Nominal time of the observation. Units are found from getTimeUnits() in the containing dataset.
   * @return nominal time of the observation in units of getTimeUnits()
   */
  public double getNominalTime();

  /**
   * Actual time of the observation. Units are found from getTimeUnits() in the containing dataset.
   * @return actual time of the observation in units of getTimeUnits()
   */
  public double getObservationTime();

  /**
   * Nominal time of the observation, as a Date.
   * @return nominal time of the observation as a Date
   */
  public Date getNominalTimeAsDate();

  /**
   * Actual time of the observation, as a Date.
   * @return actual time of the observation as a Date
   */
  public Date getObservationTimeAsDate();

  /**
   * Location of the observation
   * @return the location of the observation
   */
  public ucar.unidata.geoloc.EarthLocation getLocation();

  /**
   * The actual data of the observation.
   * @return the actual data of the observation.
   * @throws java.io.IOException on io error
   */
  public ucar.ma2.StructureData getData() throws java.io.IOException;
}
