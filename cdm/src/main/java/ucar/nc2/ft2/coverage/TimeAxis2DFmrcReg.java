/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.ft2.coverage;

/**
 * Describe
 *
 * @author caron
 * @since 10/13/2015.
 */
public class TimeAxis2DFmrcReg extends TimeAxis2DFmrc {

  public TimeAxis2DFmrcReg(CoverageCoordAxisBuilder builder) {
    super(builder);
  }

  @Override
  public CoverageCoordAxis copy() {
    return new TimeAxis2DFmrcReg(new CoverageCoordAxisBuilder(this));
  }

}
