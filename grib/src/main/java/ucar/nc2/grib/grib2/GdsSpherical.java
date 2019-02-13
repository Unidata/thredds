/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.grib.grib2;

import ucar.nc2.grib.GdsHorizCoordSys;

import java.util.Formatter;

/**
 * Fake implementation of Grib2Gds template 50, to prevent exception.
 *
 * @author caron
 * @since 9/15/2014
 */
public class GdsSpherical extends Grib2Gds {

  private int j, k, m, type, mode;
  protected GdsSpherical(byte[] data, int template) {
    super(data);
    this.template = template;

    j = getOctet4(15);
    k = getOctet4(19);
    m = getOctet4(23);
    type = getOctet(27);
    mode = getOctet(28);
  }

  @Override
  public GdsHorizCoordSys makeHorizCoordSys() {
    return null;
  }

  @Override
  public void testHorizCoordSys(Formatter f) {

  }
}
