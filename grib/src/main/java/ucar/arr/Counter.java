package ucar.arr;

import java.util.Formatter;

/**
 * Count stats
 *
 * @author John
 * @since 11/30/13
 */
public class Counter {
    public int recordsTotal;
    public int recordsUnique;
    public int dups;
    public int filter;
    public int vars;

    public String show () {
      Formatter f = new Formatter();
      float dupPercent = ((float) dups) / (recordsTotal - filter);
      f.format(" Rectilyser2: nvars=%d records total=%d filtered=%d unique=%d dups=%d (%f)%n",
              vars, recordsTotal, filter, recordsUnique, dups, dupPercent);
      return f.toString();
    }

    public void add(Counter c) {
      this.recordsUnique += c.recordsUnique;
      this.dups += c.dups;
      this.vars += c.vars;
    }

}
