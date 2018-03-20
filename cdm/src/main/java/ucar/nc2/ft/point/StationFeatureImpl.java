/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.ft.point;

import ucar.ma2.StructureData;
import ucar.unidata.geoloc.StationImpl;

import java.io.IOException;

/**
 * Implement StationFeature
 *
 * @author caron
 * @since 7/8/2014
 */
public class StationFeatureImpl extends StationImpl implements StationFeature {
  private StructureData sdata;

  public StationFeatureImpl( String name, String desc, String wmoId, double lat, double lon, double alt, int nobs, StructureData sdata) {
    super(name, desc, wmoId, lat, lon, alt, nobs);
    this.sdata = sdata;
  }

  public StationFeatureImpl( StationFeature from) throws IOException {
    super(from, 0);
    this.sdata = from.getFeatureData();
  }

  @Override
  public StructureData getFeatureData() throws IOException {
    return sdata;
  }
}
