package ucar.coord;

import net.jcip.annotations.Immutable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Formatter;
import java.util.List;

/**
 * Keeps track of the inventory for the run x time 2D time
 *
 * @author John
 * @since 12/27/13
 */
@Immutable
public class TwoDTimeInventory {
  static private final Logger logger = LoggerFactory.getLogger(TwoDTimeInventory.class);

  private final int nruns, ntimes;
  private final int[] count;        // count number of records for each (run,time). > 1 when theres for vert, ens, etc.

  public TwoDTimeInventory(int nruns, int ntimes) {
    this.nruns = nruns;
    this.ntimes = ntimes;
    count = new int[nruns*ntimes];
  }

  public void setAll() {
    for (int idx =0; idx<count.length; idx++) count[idx] = 1;
  }

  public void add(int runIdx, int timeIdx) {
    int idx = runIdx * ntimes + timeIdx;
    if (idx >= count.length)
      logger.error("TwoDTimeInventory BAD index get=" + idx + " max= " + count.length, new Throwable());
    count[idx]++;
  }

  public void showMissing(Formatter f) {
    int idx = 0;
    for (int row=0; row<nruns; row++) {
      for (int col=0; col<ntimes; col++) {
        int n = count[idx++];
        if (n == 0)
          f.format("-");
        else if (n<10)
          f.format("%1d", n);
        else
          f.format("X");
      }
      f.format("%n");
    }
    f.format("%n");
  }

  public int[] getCount() {
    return count;
  }

  public int getCount(int runIdx, int timeIdx) {
    int idx = runIdx * ntimes + timeIdx;
    return count[idx];
  }

  ////////////////////////
  // read from ncx2
  public TwoDTimeInventory(List<Integer> count, int nruns, int ntimes) {
    this.count = new int[count.size()];
    int idx = 0;
    for (int n : count) this.count[idx++] = n;

    this.nruns = nruns;
    this.ntimes = ntimes;
  }

}
