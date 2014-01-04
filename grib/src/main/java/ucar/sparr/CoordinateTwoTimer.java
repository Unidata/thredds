package ucar.sparr;

import java.util.Formatter;
import java.util.List;

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
        int n = count[row*ntimes+col];
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
  // read back in
  public CoordinateTwoTimer(List<Integer> count) {
    this.count = new int[count.size()];
    int idx = 0;
    for (int n : count) this.count[idx++] = n;
  }

  public void setSize(int nruns, int ntimes) {
    this.nruns = nruns;
    this.ntimes = ntimes;
  }

}
