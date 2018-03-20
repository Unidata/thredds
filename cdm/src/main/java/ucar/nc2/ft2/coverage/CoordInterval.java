/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.ft2.coverage;

import ucar.unidata.util.Format;

import javax.annotation.concurrent.Immutable;

/**
 * Convenience wrapper for interval coordinates.
 *
 * @author caron
 * @since 7/22/2015
 */
@Immutable
public class CoordInterval {
  private final double start, end;
  private final int ndec;

  public CoordInterval(double start, double end) {
    this.start = start;
    this.end = end;
    this.ndec = 3;
  }

  public CoordInterval(double start, double end, int ndec) {
    this.start = start;
    this.end = end;
    this.ndec = ndec;
  }

  @Override
  public String toString() {
    return Format.d(start, ndec) + "-" + Format.d(end, ndec);
  }
}
