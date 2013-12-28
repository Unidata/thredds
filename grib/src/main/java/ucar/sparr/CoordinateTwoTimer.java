package ucar.sparr;

import java.util.Formatter;

/**
 * Description
 *
 * @author John
 * @since 12/27/13
 */
public class CoordinateTwoTimer {
   private int nruns, ntimes;
   private int[] count;

  public CoordinateTwoTimer(int nruns, int ntimes) {
    this.nruns = nruns;
    this.ntimes = ntimes;
    count = new int[nruns*ntimes];
  }

  public void add(int runIdx, int timeIdx) {
    int idx = runIdx * ntimes + timeIdx;
    if (idx >= count.length)
      System.out.println("HEY");
    count[idx]++;
  }

  public void showMissing(Formatter f) {
    for (int row=0; row<nruns; row++) {
      for (int col=0; col<ntimes; col++) {
        boolean hasRecord = count[row*ntimes+col] > 0;
        if (hasRecord) f.format("X"); else f.format("-");
      }
      f.format("%n");
    }
    f.format("%n");
  }

}
