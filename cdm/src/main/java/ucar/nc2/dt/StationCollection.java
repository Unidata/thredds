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

import java.io.IOException;
import java.util.Date;
import java.util.List;

/**
 * A collection of data at unconnected station locations, typically time series.
 * User can subset by stations, bounding box and by date range.
 * Underlying data can be of any type, but all points have the same type.
 *
 * @author caron
 */
public interface StationCollection extends PointCollection {

  /**
   * Get all the Stations in the collection.
   *
   * @return List of Station
   * @throws java.io.IOException on io error
   */
  public List<Station> getStations() throws IOException;

  /**
   * Get all the Stations in the collection, allow user to cancel.
   *
   * @param cancel allow user to cancel. Implementors should return ASAP.
   * @return List of Station
   * @throws java.io.IOException on io error
   */
  public List<Station> getStations(ucar.nc2.util.CancelTask cancel) throws IOException;

  /**
   * Get all the Stations within a bounding box.
   *
   * @param boundingBox restrict data to this bounding nox
   * @return List of Station
   * @throws java.io.IOException on io error
   */
  public List<Station> getStations(ucar.unidata.geoloc.LatLonRect boundingBox) throws IOException;

  /**
   * Get all the Stations within a bounding box, allow user to cancel.
   *
   * @param boundingBox restrict data to this bounding nox
   * @param cancel allow user to cancel. Implementors should return ASAP.
   * @return List of Station
   * @throws java.io.IOException on io error
   */
  public List<Station> getStations(ucar.unidata.geoloc.LatLonRect boundingBox, ucar.nc2.util.CancelTask cancel) throws IOException;

  /**
   * Find a Station by name
   * @param name find this name
   * @return Station, or null
   */
  public Station getStation(String name);

  /**
   * How many Data objects are available for this Station?
   *
   * @param s station
   * @return count or -1 if unknown.
   */
  public int getStationDataCount(Station s);

  /**
   * Get all data for this Station.
   *
   * @param s for this Station
   * @return List of getDataClass()
   * @throws java.io.IOException on io error
   */
  public List getData(Station s) throws IOException;

  /**
   * Get all data for this Station, allow user to cancel.
   *
   * @param s for this Station
   * @param cancel allow user to cancel. Implementors should return ASAP.
   * @return List of getDataClass()
   * @throws java.io.IOException on io error
   */
  public List getData(Station s, ucar.nc2.util.CancelTask cancel) throws IOException;

  /**
   * Get data for this Station within the specified date range.
   *
   * @param s for this Station
   * @param start restrict data to after this time
   * @param end restrict data to before this time
   * @return List of getDataClass()
   * @throws java.io.IOException on io error
   */
  public List getData(Station s, Date start, Date end) throws IOException;

  /**
   * Get data for this Station within the specified date range, allow user to cancel.
   *
   * @param s for this Station
   * @param start restrict data to after this time
   * @param end restrict data to before this time
   * @param cancel allow user to cancel. Implementors should return ASAP.
   * @return List of getDataClass()
   * @throws java.io.IOException on io error
   */
  public List getData(Station s, Date start, Date end, ucar.nc2.util.CancelTask cancel) throws IOException;

  /**
   * Get all data for a list of Stations.
   *
   * @param stations for these Stations
   * @return List of getDataClass()
   * @see #getDataIterator as a (possibly) more efficient alternative
   * @throws java.io.IOException on io error
   */
  public List getData(List<Station> stations) throws IOException;

  /**
   * Get all data for a list of Stations, allow user to cancel.
   *
   * @param stations for these Stations
   * @param cancel allow user to cancel. Implementors should return ASAP.
   * @return List of getDataClass()
   * @see #getDataIterator as a (possibly) more efficient alternative
   * @throws java.io.IOException on io error
   */
  public List getData(List<Station> stations, ucar.nc2.util.CancelTask cancel) throws IOException;

  /**
   * Get data for a list of Stations within the specified date range.
   *
   * @param stations for these Stations
   * @param start restrict data to after this time
   * @param end restrict data to before this time
   * @return List of getDataClass()
   * @see #getDataIterator as a (possibly) more efficient alternative
   * @throws java.io.IOException on io error
   */
  public List getData(List<Station> stations, Date start, Date end) throws IOException;

  /**
   * Get data for a list of Stations within the specified date range, allow user to cancel.
   *
   * @param stations for these Stations
   * @param start restrict data to after this time
   * @param end restrict data to before this time
   * @param cancel allow user to cancel. Implementors should return ASAP.
   * @return List of getDataClass()
   * @see #getDataIterator as a (possibly) more efficient alternative
   * @throws java.io.IOException on io error
   */
  public List getData(List<Station> stations, Date start, Date end, ucar.nc2.util.CancelTask cancel) throws IOException;


  /**
   * Get all data for this Station.
   *
   * @param s for this Station
   * @return iterator over type getDataClass()
   */
  public DataIterator getDataIterator(Station s);

  /**
   * Get data for this Station within the specified date range.
   *
   * @param s for this Station
   * @param start restrict data to after this time
   * @param end restrict data to before this time
    * @return Iterator over type getDataClass()
   */
  public DataIterator getDataIterator(Station s, Date start, Date end);

  /** Get all data for a list of Stations.
   * @return Iterator over type getDataClass() *
  public DataIterator getDataIterator(List stations) throws IOException;

  /** Get data for a list of Stations within the specified date range.
   * @return Iterator over type getDataClass() *
  public DataIterator getDataIterator(List stations, Date start, Date end) throws IOException;
   */

}
