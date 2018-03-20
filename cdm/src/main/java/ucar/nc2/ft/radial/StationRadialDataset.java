/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.ft.radial;

import ucar.nc2.dt.RadialDatasetSweep;
import ucar.nc2.ft.StationCollection;
import ucar.nc2.ft.FeatureDataset;

import java.io.IOException;
import java.util.Date;

/**
 * A Station collection of radial data.
 *
 * @author caron
 * @since Feb 18, 2008
 */
public interface StationRadialDataset extends StationCollection, FeatureDataset {

  // LOOK - should return RadialSweepFeature ??
  public RadialDatasetSweep getRadarDataset(String stationName, Date date) throws IOException;

  /*
   * Get a subsetted StationCollection
   *
   * @param stations only contain these stations
   * @return subsetted collection
   * @throws java.io.IOException on i/o error
   *
  public StationRadialDataset subset(List<Station> stations) throws IOException;

  /**
   * Get the collection of data for this Station.
   *
   * @param s at this station
   * @return collection of data for this Station.
   * @throws java.io.IOException on i/o error
   *
  public RadialSweepFeature getFeature(Station s) throws IOException;

  /*
   * Get the collection of data for this Station and date range.
   *
   * @param s at this station
   * @param dateRange date range
   * @return collection of data for this Station and date range.
   * @throws java.io.IOException on i/o error
   *
  public RadialSweepFeature getFeature(Station s, DateRange dateRange) throws IOException;

    /*
     * check if the product available for all stations.
     *
     * @param product the given Product
     * @return true if data avaible for the given Product
     *
    public boolean checkStationProduct(Product product);


    /*
     * check if the product available for one station
     * @param stationName which station
     * @param product the given Product and Station
     * @return true if data avaible for the given Product
     *
    public boolean checkStationProduct(String stationName, Product product);

    /**
     * How many Data Products are available for this Station?
     *
     * @param sName station name
     * @return count or -1 if unknown.
     *
    public int getStationProductCount(String sName);
  */

}
