/*
 * Copyright 1998-2014 University Corporation for Atmospheric Research/Unidata
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

package ucar.nc2;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dt.grid.GridDataset;
import ucar.unidata.util.test.category.NeedsExternalResource;
import ucar.unidata.util.test.TestDir;

/**
 * Describe
 *
 * @author caron
 * @since 6/19/2014
 */
@RunWith(Parameterized.class)
@Category(NeedsExternalResource.class)
public class TestHttpOpen {

  @Parameterized.Parameters(name="{0}")
  public static Collection testUrls() {
      Object[][] data = new Object[][]{
              {"http://"+ TestDir.remoteTestServer+"/thredds/fileServer/testdata/2004050412_eta_211.nc"},
              {"http://"+TestDir.remoteTestServer+"/thredds/fileServer/testdata/2004050400_eta_211.nc"},
              {"http://"+TestDir.remoteTestServer+"/thredds/fileServer/testdata/2004050312_eta_211.nc"},
              {"http://"+TestDir.remoteTestServer+"/thredds/fileServer/testdata/2004050300_eta_211.nc"},
      };
      return Arrays.asList(data);
  }

  private final String url;
  public TestHttpOpen(String url) {
      this.url = url;
  }

  // HTTP = 4300 HTTP2 = 5500 msec 20-25% slower
  @Test
  public void testOpenDataset() throws IOException {
    long start = System.currentTimeMillis();

    try (NetcdfDataset ncd = ucar.nc2.dataset.NetcdfDataset.openDataset(url)) {
      System.out.printf("%s%n", ncd.getLocation());
    } finally {
      System.out.printf("**testOpenDataset took= %d msecs%n", (System.currentTimeMillis() - start));
    }
  }

  @Test
  public void testOpenGrid() throws IOException {
    long start = System.currentTimeMillis();

    try (GridDataset ncd = ucar.nc2.dt.grid.GridDataset.open(url)) {
      System.out.printf("%s%n", ncd.getLocation());
    } finally {
      System.out.printf("**testOpenGrid took= %d msecs%n", (System.currentTimeMillis() - start));
    }
  }

  @Test
  public void testReadData() throws IOException {
    long start = System.currentTimeMillis();
    long totalBytes = 0;

    try (NetcdfFile ncfile = NetcdfFile.open(url)) {
      totalBytes = readAllData(ncfile);
    } finally {
      System.out.printf("**testRad Data took= %d msecs %d kbytes%n", (System.currentTimeMillis() - start), totalBytes / 1000);
    }
  }

  private long readAllData(NetcdfFile ncfile) throws IOException {
    System.out.println("------Open "+ncfile.getLocation());

    long total = 0;
    for (Variable v : ncfile.getVariables()) {
      long nbytes = v.getSize() * v.getElementSize();
      System.out.println("  Try to read variable " + v.getFullName() + " " + nbytes);
      v.read();
      total += v.getSize() * v.getElementSize();
    }
    ncfile.close();
    return total;
  }
}
