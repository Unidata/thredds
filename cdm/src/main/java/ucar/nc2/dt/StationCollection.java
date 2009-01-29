/*
 * Copyright 1998-2009 University Corporation for Atmospheric Research/Unidata
 *
 * Portions of this software were developed by the Unidata Program at the
 * University Corporation for Atmospheric Research.
 *
 * Access and use of this software shall impose the following obligations
 * and understandings on the user. The user is granted the right, without
 * any fee or cost, to use, copy, modify, alter, enhance and distribute
 * this software, and any derivative works thereof, and its supporting
 * documentation for any purpose whatsoever, provided that this entire
 * notice appears in all copies of the software, derivative works and
 * supporting documentation.  Further, UCAR requests that the user credit
 * UCAR/Unidata in any publications that result from the use of this
 * software or in any product that includes this software. The names UCAR
 * and/or Unidata, however, may not be used in any advertising or publicity
 * to endorse or promote any products or commercial entity unless specific
 * written permission is obtained from UCAR/Unidata. The user also
 * understands that UCAR/Unidata is not obligated to provide the user with
 * any support, consulting, training or assistance of any kind with regard
 * to the use, operation and performance of this software nor to provide
 * the user with any updates, revisions, new versions or "bug fixes."
 *
 * THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 * INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 * FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 * NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 * WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
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
  public List<ucar.unidata.geoloc.Station> getStations() throws IOException;

  /**
   * Get all the Stations in the collection, allow user to cancel.
   *
   * @param cancel allow user to cancel. Implementors should return ASAP.
   * @return List of Station
   * @throws java.io.IOException on io error
   */
  public List<ucar.unidata.geoloc.Station> getStations(ucar.nc2.util.CancelTask cancel) throws IOException;

  /**
   * Get all the Stations within a bounding box.
   *
   * @param boundingBox restrict data to this bounding nox
   * @return List of Station
   * @throws java.io.IOException on io error
   */
  public List<ucar.unidata.geoloc.Station> getStations(ucar.unidata.geoloc.LatLonRect boundingBox) throws IOException;

  /**
   * Get all the Stations within a bounding box, allow user to cancel.
   *
   * @param boundingBox restrict data to this bounding nox
   * @param cancel allow user to cancel. Implementors should return ASAP.
   * @return List of Station
   * @throws java.io.IOException on io error
   */
  public List<ucar.unidata.geoloc.Station> getStations(ucar.unidata.geoloc.LatLonRect boundingBox, ucar.nc2.util.CancelTask cancel) throws IOException;

  /**
   * Find a Station by name
   * @param name find this name
   * @return Station, or null
   */
  public ucar.unidata.geoloc.Station getStation(String name);

  /**
   * How many Data objects are available for this Station?
   *
   * @param s station
   * @return count or -1 if unknown.
   */
  public int getStationDataCount(ucar.unidata.geoloc.Station s);

  /**
   * Get all data for this Station.
   *
   * @param s for this Station
   * @return List of getDataClass()
   * @throws java.io.IOException on io error
   */
  public List getData(ucar.unidata.geoloc.Station s) throws IOException;

  /**
   * Get all data for this Station, allow user to cancel.
   *
   * @param s for this Station
   * @param cancel allow user to cancel. Implementors should return ASAP.
   * @return List of getDataClass()
   * @throws java.io.IOException on io error
   */
  public List getData(ucar.unidata.geoloc.Station s, ucar.nc2.util.CancelTask cancel) throws IOException;

  /**
   * Get data for this Station within the specified date range.
   *
   * @param s for this Station
   * @param start restrict data to after this time
   * @param end restrict data to before this time
   * @return List of getDataClass()
   * @throws java.io.IOException on io error
   */
  public List getData(ucar.unidata.geoloc.Station s, Date start, Date end) throws IOException;

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
  public List getData(ucar.unidata.geoloc.Station s, Date start, Date end, ucar.nc2.util.CancelTask cancel) throws IOException;

  /**
   * Get all data for a list of Stations.
   *
   * @param stations for these Stations
   * @return List of getDataClass()
   * @see #getDataIterator as a (possibly) more efficient alternative
   * @throws java.io.IOException on io error
   */
  public List getData(List<ucar.unidata.geoloc.Station> stations) throws IOException;

  /**
   * Get all data for a list of Stations, allow user to cancel.
   *
   * @param stations for these Stations
   * @param cancel allow user to cancel. Implementors should return ASAP.
   * @return List of getDataClass()
   * @see #getDataIterator as a (possibly) more efficient alternative
   * @throws java.io.IOException on io error
   */
  public List getData(List<ucar.unidata.geoloc.Station> stations, ucar.nc2.util.CancelTask cancel) throws IOException;

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
  public List getData(List<ucar.unidata.geoloc.Station> stations, Date start, Date end) throws IOException;

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
  public List getData(List<ucar.unidata.geoloc.Station> stations, Date start, Date end, ucar.nc2.util.CancelTask cancel) throws IOException;


  /**
   * Get all data for this Station.
   *
   * @param s for this Station
   * @return iterator over type getDataClass()
   */
  public DataIterator getDataIterator(ucar.unidata.geoloc.Station s);

  /**
   * Get data for this Station within the specified date range.
   *
   * @param s for this Station
   * @param start restrict data to after this time
   * @param end restrict data to before this time
    * @return Iterator over type getDataClass()
   */
  public DataIterator getDataIterator(ucar.unidata.geoloc.Station s, Date start, Date end);

  /** Get all data for a list of Stations.
   * @return Iterator over type getDataClass() *
  public DataIterator getDataIterator(List stations) throws IOException;

  /** Get data for a list of Stations within the specified date range.
   * @return Iterator over type getDataClass() *
  public DataIterator getDataIterator(List stations, Date start, Date end) throws IOException;
   */

}
