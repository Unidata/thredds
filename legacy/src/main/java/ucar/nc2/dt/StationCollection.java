/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
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
 * @deprecated
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
