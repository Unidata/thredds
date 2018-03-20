/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.ft;

import ucar.unidata.geoloc.LatLonRect;
import ucar.unidata.geoloc.Station;

import java.io.IOException;
import java.util.List;

/**
 * A collection of data at named locations called Stations.
 * User can subset by bounding box .
 *
 * @author caron
 */
public interface StationCollection {

  /**
   * Get all the Stations in the collection.
   *
   * @return List of Station
   * @throws java.io.IOException on i/o error
   */
  List<Station> getStations() throws IOException;

  /**
   * Get all the Stations within a bounding box.
   *
   * @param boundingBox spatial subset
   * @return List of Station
   * @throws java.io.IOException on i/o error
   */
  List<Station> getStations(ucar.unidata.geoloc.LatLonRect boundingBox) throws IOException;

  /**
   * Translate list of station names to list of Stations. Skip any not found
   *
   * @param stnNames list of stnNames
   * @return  corresponding list of Stations
   */
  List<Station> getStations( List<String> stnNames);

  /**
   * Find a Station by name.
   *
   * @param name name/id of the station
   * @return Station or null if not found
   */
  Station getStation(String name);

  /**
   * Get the bounding box including all the stations.
   * @return bounding box as a LatLonRect
   */
  LatLonRect getBoundingBox();

}
