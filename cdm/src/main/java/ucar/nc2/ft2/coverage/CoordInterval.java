/* Copyright */
package ucar.nc2.ft2.coverage;

import net.jcip.annotations.Immutable;
import ucar.unidata.util.Format;

/**
 * Describe
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
