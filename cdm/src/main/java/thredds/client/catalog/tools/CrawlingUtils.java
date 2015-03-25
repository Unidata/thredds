/*
 * Copyright 1998-2015 University Corporation for Atmospheric Research/Unidata
 *
 *   Portions of this software were developed by the Unidata Program at the
 *   University Corporation for Atmospheric Research.
 *
 *   Access and use of this software shall impose the following obligations
 *   and understandings on the user. The user is granted the right, without
 *   any fee or cost, to use, copy, modify, alter, enhance and distribute
 *   this software, and any derivative works thereof, and its supporting
 *   documentation for any purpose whatsoever, provided that this entire
 *   notice appears in all copies of the software, derivative works and
 *   supporting documentation.  Further, UCAR requests that the user credit
 *   UCAR/Unidata in any publications that result from the use of this
 *   software or in any product that includes this software. The names UCAR
 *   and/or Unidata, however, may not be used in any advertising or publicity
 *   to endorse or promote any products or commercial entity unless specific
 *   written permission is obtained from UCAR/Unidata. The user also
 *   understands that UCAR/Unidata is not obligated to provide the user with
 *   any support, consulting, training or assistance of any kind with regard
 *   to the use, operation and performance of this software nor to provide
 *   the user with any updates, revisions, new versions or "bug fixes."
 *
 *   THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 *   IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *   WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *   DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 *   INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 *   FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 *   NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 *   WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */
package thredds.client.catalog.tools;

import ucar.ma2.Array;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.Range;
import ucar.ma2.Section;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.util.CancelTask;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Utilities for crawling and testing TDS servers
 *
 * @author caron
 * @since 3/14/2015
 */
public class CrawlingUtils {

  // read a 2D slice out all the variables in the dataset, report stats
  // Its a Runnable, so you can put it into a Thread for mulithreaded testing
  public static class TDSdatasetReader implements Runnable {
    private boolean showDetail = false;
    private String who;
    private String datasetUrl;
    private CancelTask cancel;

    public TDSdatasetReader(String who, String datasetUrl, CancelTask cancel, boolean showDetail) {
      this.who = who;
      this.datasetUrl = datasetUrl;
      this.cancel = cancel;
      this.showDetail = showDetail;
    }

    public void run() {
      int count = 0;
      long total = 0, time = 0;
      System.out.printf("TDSdatasetReader %s started url=%s%n", who, datasetUrl);
      try (NetcdfFile ncfile = NetcdfDataset.openFile(datasetUrl, cancel)) {
        for (Variable var : ncfile.getVariables()) {
          long start = System.currentTimeMillis();
          Array result = doLimitedRead(var);
          long took = System.currentTimeMillis() - start;

          long size = result.getSize();
          double rate = (took == 0) ? 0.0 : size / took / 1000.0;
          if (showDetail) System.out.printf(" took= %d msecs rate= %f MB/sec%n", took, rate);
          total += size;
          time += took;
          //if (stop.isCancel()) break;
          count++;
        }

        double totald = total / (1000. * 1000.);
        double rate = (time == 0) ? 0 : total / time / 1000.0;

        System.out.printf("%n%s%n", ncfile.getLocation());
        System.out.printf(" took= %f secs rate= %f MB/sec%n", totald, rate);

      } catch (IOException | InvalidRangeException e) {
        e.printStackTrace();
      }
      System.out.printf(" thread done %d%n", count);
    }

    private Array doLimitedRead(Variable v) throws IOException, InvalidRangeException {
      long size = v.getSize() * v.getElementSize();
      if (size < 1000 * 1000 || v.getRank() < 3) {
        if (showDetail) System.out.printf(" thread %s read %s bytes = %d ", who, v.getFullName(), size);
        return v.read();

      } else {
        // randomly choose a 2D slice
        int rank = v.getRank();
        List<Range> ranges = new ArrayList<>();
        int i = 0;
        Random r = new Random();
        for (Dimension dim : v.getDimensions()) {
          if (i < rank - 2) {
            int first = r.nextInt(dim.getLength());
            ranges.add(new Range(first, first));
          } else {
            ranges.add(new Range(0, dim.getLength() - 1));
          }
          i++;
        }
        Section s = new Section(ranges);
        if (showDetail) System.out.printf(" thread %s read %s(%s) bytes= %d ", who, v.getFullName(), s, s.computeSize());
        Array result = v.read(s);
        assert result.getSize() == s.computeSize();
        return result;
      }
    }
  }
}
