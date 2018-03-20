/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package thredds.server.fileserver;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import thredds.TestOnLocalServer;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dt.grid.GridDataset;
import ucar.unidata.io.http.HTTPRandomAccessFile;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.Arrays;
import java.util.Collection;

/**
 * Describe
 *
 * @author caron
 * @since 6/19/2014
 */
@RunWith(Parameterized.class)
public class TestHttpOpen {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Parameterized.Parameters(name="{0}")
  public static Collection testUrls() {
      Object[][] data = new Object[][]{
              {"fileServer/scanLocal/2004050412_eta_211.nc"},
              {"fileServer/scanLocal/1day.nc"},
              {"fileServer/scanLocal/testWrite.nc"},
              {"fileServer/scanLocal/fultrak.hd5"},
      };
      return Arrays.asList(data);
  }

  private final String url;
  public TestHttpOpen(String path) {
      this.url = TestOnLocalServer.withHttpPath(path);
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
      Object mess = ncfile.sendIospMessage(NetcdfFile.IOSP_MESSAGE_RANDOM_ACCESS_FILE);
      assert mess != null;
      assert mess instanceof HTTPRandomAccessFile;

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
