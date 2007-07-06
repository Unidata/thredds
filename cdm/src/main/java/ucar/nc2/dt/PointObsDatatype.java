/*
 * Copyright 1997-2007 Unidata Program Center/University Corporation for
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
package ucar.nc2.dt;

import java.util.Date;

/**
 * A collection of observations at one time and location.
 *
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
  public EarthLocation getLocation();

  /**
   * The actual data of the observation.
   * @return the actual data of the observation.
   * @throws java.io.IOException on io error
   */
  public ucar.ma2.StructureData getData() throws java.io.IOException;
}
