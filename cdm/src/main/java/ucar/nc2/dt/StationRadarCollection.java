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
import java.util.List;

import ucar.unidata.util.Product;


/**
 * A collection of data at unconnected radar station.
 * User can subset by stations, bounding box and by date range.
 * Underlying data can be of any type, but all points have the same type.
 *
 * @author yuan
 */
public interface StationRadarCollection {

  /**
   * Get all the Stations in the collection.
   *
   * @return List of Station
   * @throws java.io.IOException on io error
   */
  public List<Station> getStations() throws IOException;

  /**
   * Get all the Stations within a bounding box, allow user to cancel.
   *
   * @param boundingBox restrict data to this bounding nox
   * @param cancel allow user to cancel. Implementors should return ASAP.
   * @return List of Station
   * @throws java.io.IOException on io error
   */
  public List<Station> getStations(ucar.unidata.geoloc.LatLonRect boundingBox, ucar.nc2.util.CancelTask cancel) throws IOException;

  /** Find a Station by name */
  //public Station getRadarStation( String name);


  /**
   * check if the product available for all stations.
   *
   * @param product the given Product
   * @return true if data avaible for the given Product
   */
  public boolean checkStationProduct(Product product);


  /**
   * check if the product available for one station
   * @param stationName which station
   * @param product the given Product and Station
   * @return true if data avaible for the given Product
   */
  public boolean checkStationProduct(String stationName, Product product);

  /**
   * How many Data Products are available for this Station?
   *
   * @param sName station name
   * @return count or -1 if unknown.
   */
  public int getStationProductCount(String sName);

}
