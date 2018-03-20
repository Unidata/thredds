/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.unidata.geoloc;

import javax.annotation.Nonnull;

/**
 * A named location on the earth.
 * @author caron
 * @since Feb 18, 2008
 */
public interface Station extends EarthLocation, Comparable<Station> {

  /**
   * Station name or id. Must be unique within the collection
   * @return station name or id. May not be null.
   */
  @Nonnull
  String getName();

  /**
   * Station description
   * @return station description or null
   */
  String getDescription();

  /**
   * WMO station id.
   * @return WMO station id, or null.
   */
  String getWmoId();

  /**
   * get Number of obs at this station
   * @return Number of obs or -1 if unknown
   */
  int getNobs();

}
