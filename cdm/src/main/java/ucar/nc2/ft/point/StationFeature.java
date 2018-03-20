/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.ft.point;

import ucar.ma2.StructureData;
import ucar.unidata.geoloc.Station;

import java.io.IOException;

/**
 * A Station that has additional data obtained through getFeatureData().
 *
 * @author caron
 * @since 7/8/2014
 */
public interface StationFeature extends Station {
  StructureData getFeatureData() throws IOException;
}
