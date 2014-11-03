/*
 * Copyright 1998-2013 University Corporation for Atmospheric Research/Unidata
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

package ucar.nc2.iosp.mcidas;

import org.junit.Test;
import ucar.nc2.NetcdfFile;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.unidata.test.util.TestDir;

import java.io.File;
import java.io.IOException;

/**
 * Read everything in formats/mcidas
 *
 * @author caron
 * @since 8/30/13
 */
public class TestReadingMcIdas {

  @Test
  public void testCompare() throws IOException {
    long start = System.currentTimeMillis();
    countOk += TestDir.actOnAll(TestDir.cdmUnitTestDir + "formats/mcidas", null, new MyAct());
    long took = (System.currentTimeMillis() - start);
    System.out.printf("countOk=%d, countFail=%d took %d msecs %n", countOk, countFail, took);
    assert countFail == 0;
  }

  int countOk = 0;
  int countFail = 0;

  private class MyAct implements TestDir.Act {

    @Override
    public int doAct(String filename) throws IOException {
      try (NetcdfFile ncfile = NetcdfDataset.openFile(filename, null)) {
        System.out.printf("  Open McIdas File %s ", filename);
        String ft = ncfile.findAttValueIgnoreCase(null, "featureType", "none");
        String iosp = ncfile.getIosp().getFileTypeId();
        System.out.printf(" iosp=%s ft=%s%n", iosp, ft);
        assert iosp.equals("McIDASArea") || iosp.equals("McIDASGrid") : iosp;
        assert ft.equals(FeatureType.GRID.toString()) : ft;
        return 1;
      } catch (Throwable t) {
        System.out.printf(" FAILED =%s%n", t.getMessage());
        t.printStackTrace();
        countFail++;
        return 0;
      }
    }
  }

}
