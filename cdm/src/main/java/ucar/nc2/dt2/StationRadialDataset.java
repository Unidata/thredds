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

import ucar.unidata.util.Product;
import ucar.nc2.units.DateRange;

import java.util.List;
import java.io.IOException;

/**
 * A Station collection of radial data.
 *
 * @author caron
 * @since Feb 18, 2008
 */
public interface StationRadialDataset extends StationCollection {

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
