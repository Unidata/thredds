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
  public List<ucar.unidata.geoloc.Station> getStations() throws IOException;

  /**
   * Get all the Stations within a bounding box, allow user to cancel.
   *
   * @param boundingBox restrict data to this bounding nox
   * @param cancel allow user to cancel. Implementors should return ASAP.
   * @return List of Station
   * @throws java.io.IOException on io error
   */
  public List<ucar.unidata.geoloc.Station> getStations(ucar.unidata.geoloc.LatLonRect boundingBox, ucar.nc2.util.CancelTask cancel) throws IOException;

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
