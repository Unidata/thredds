/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.grib.coord;

import java.util.Formatter;

/**
 * Counting statistics
 *
 * @author John
 * @since 11/30/13
 */
public class GribRecordStats {
  public int recordsTotal;
  public int recordsUnique;
  public int dups;
  public int filter;
  public int vars;

  public String show() {
    try (Formatter f = new Formatter()) {
      float dupPercent = ((float) dups) / (recordsTotal);
      float density = ((float) recordsUnique) / (recordsTotal);
      f.format(" Counter: nvars=%d records %d/%d (%f) filtered=%d dups=%d (%f)%n",
          vars, recordsUnique, recordsTotal, density, filter, dups, dupPercent);
      return f.toString();
    }
  }

  public void add(GribRecordStats c) {
    this.recordsUnique += c.recordsUnique;
    this.dups += c.dups;
    this.vars += c.vars;
  }

}
