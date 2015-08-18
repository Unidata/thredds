/*
 * Copyright 1998-2015 John Caron and University Corporation for Atmospheric Research/Unidata
 *
 *  Portions of this software were developed by the Unidata Program at the
 *  University Corporation for Atmospheric Research.
 *
 *  Access and use of this software shall impose the following obligations
 *  and understandings on the user. The user is granted the right, without
 *  any fee or cost, to use, copy, modify, alter, enhance and distribute
 *  this software, and any derivative works thereof, and its supporting
 *  documentation for any purpose whatsoever, provided that this entire
 *  notice appears in all copies of the software, derivative works and
 *  supporting documentation.  Further, UCAR requests that the user credit
 *  UCAR/Unidata in any publications that result from the use of this
 *  software or in any product that includes this software. The names UCAR
 *  and/or Unidata, however, may not be used in any advertising or publicity
 *  to endorse or promote any products or commercial entity unless specific
 *  written permission is obtained from UCAR/Unidata. The user also
 *  understands that UCAR/Unidata is not obligated to provide the user with
 *  any support, consulting, training or assistance of any kind with regard
 *  to the use, operation and performance of this software nor to provide
 *  the user with any updates, revisions, new versions or "bug fixes."
 *
 *  THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 *  IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *  WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *  DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 *  INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 *  FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 *  NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 *  WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */
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
