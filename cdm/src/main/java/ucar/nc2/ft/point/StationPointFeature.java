/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.ft.point;

import ucar.nc2.ft.PointFeature;

/**
 * A PointFeature from which one can obtain a Station
 *
 * @author caron
 * @since Aug 27, 2009
 */
public interface StationPointFeature extends PointFeature {
  StationFeature getStation();
}
