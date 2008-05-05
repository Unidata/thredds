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
package ucar.nc2.ft;

import ucar.nc2.units.DateUnit;

import java.util.Date;

/**
 * A collection of observations at one time and location.
 * @author caron
 * @since Feb 29, 2008
 */
public interface PointFeature {

  /**
   * Location of this observation
   * @return the location of this observation
   */
  public EarthLocation getLocation();

 /**
   * Actual time of this observation.
   * Convert to Date with getTimeUnit().makeDate()
   * @return actual time of this observation.
   */
  public double getObservationTime();

  /**
   * Actual time of this observation, as a Date.
   * @return actual time of this observation, as a Date.
   */
  public Date getObservationTimeAsDate();

  /**
   * Nominal time of this observation.
   * Convert to Date with getTimeUnit().makeDate().
   * When the nominal time is not given in the data, it is usually set to the observational time.
   * @return Nominal time of this observation.
   */
  public double getNominalTime();

  /**
   * Nominal time of this observation, as a Date.
   * @return Nominal time of this observation, as a Date.
   */
  public Date getNominalTimeAsDate();

  /**
   * Get the time unit of the time coordinate.
   * @return time unit of the time coordinate
   */
  public DateUnit getTimeUnit();

  /**
   * The actual data of this observation.
   * @return the actual data of this observation.
   * @throws java.io.IOException on i/o error
   */
  public ucar.ma2.StructureData getData() throws java.io.IOException;

}
