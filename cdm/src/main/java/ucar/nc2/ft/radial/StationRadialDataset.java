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
package ucar.nc2.ft.radial;

import ucar.unidata.util.Product;
import ucar.nc2.units.DateRange;
import ucar.nc2.ft.radial.RadialSweepFeature;
import ucar.nc2.ft.StationCollection;
import ucar.nc2.ft.FeatureDataset;
import ucar.unidata.geoloc.Station;

import java.util.List;
import java.io.IOException;

/**
 * A Station collection of radial data.
 *
 * @author caron
 * @since Feb 18, 2008
 */
public interface StationRadialDataset extends StationCollection, FeatureDataset {

  /**
   * Get a subsetted StationCollection
   *
   * @param stations only contain these stations
   * @return subsetted collection
   * @throws java.io.IOException on i/o error
   */
  public StationRadialDataset subset(List<Station> stations) throws IOException;

  /**
   * Get the collection of data for this Station.
   *
   * @param s at this station
   * @return collection of data for this Station.
   * @throws java.io.IOException on i/o error
   */
  public RadialSweepFeature getFeature(Station s) throws IOException;

  /**
   * Get the collection of data for this Station and date range.
   *
   * @param s at this station
   * @param dateRange date range
   * @return collection of data for this Station and date range.
   * @throws java.io.IOException on i/o error
   */
  public RadialSweepFeature getFeature(Station s, DateRange dateRange) throws IOException;

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
