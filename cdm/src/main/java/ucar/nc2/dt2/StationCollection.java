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
package ucar.nc2.dt2;

import ucar.nc2.units.DateRange;

import java.io.IOException;
import java.util.List;

/**
 * A collection of data at named locations called Stations.
 * User can subset by bounding box and by date range.
 * Underlying data can be of any feature type, but all points have the same feature type.
 *
 * @author caron
 */
public interface StationCollection extends PointCollection {

  /**
   * Get all the Stations in the collection.
   *
   * @return List of Station
   * @throws java.io.IOException on i/o error
   */
  public List<Station> getStations() throws IOException;

  /**
   * Get all the Stations within a bounding box.
   *
   * @param boundingBox spatial subset
   * @return List of Station
   * @throws java.io.IOException on i/o error
   */
  public List<Station> getStations(ucar.unidata.geoloc.LatLonRect boundingBox) throws IOException;

  /**
   * Find a Station by name.
   *
   * @param name name or id of the station
   * @return Station or null if not found
   */
  public Station getStation(String name);

  /**
   * Get a subsetted StationCollection
   *
   * @param stations only contain these stations
   * @return subsetted collection
   * @throws java.io.IOException on i/o error
   */
  public StationCollection subset(List<Station> stations) throws IOException;

  /**
   * Get the collection of data for this Station.
   *
   * @param s at this station
   * @return collection of data for this Station.
   * @throws java.io.IOException on i/o error
   */
  public StationCollection subset(Station s) throws IOException;

  /**
   * Get the collection of data for this Station and date range.
   *
   * @param s at this station
   * @param dateRange date range
   * @return collection of data for this Station and date range.
   * @throws java.io.IOException on i/o error
   */
  public StationCollection subset(Station s, DateRange dateRange) throws IOException;

}
