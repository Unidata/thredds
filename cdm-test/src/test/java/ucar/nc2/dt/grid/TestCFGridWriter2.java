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

package ucar.nc2.dt.grid;

import org.junit.Test;
import ucar.nc2.NetcdfFileWriter;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.time.CalendarDateRange;
import ucar.unidata.geoloc.LatLonPointImpl;
import ucar.unidata.geoloc.LatLonRect;
import ucar.unidata.test.util.TestDir;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertTrue;

/**
 * Test CFGridWriter2
 *
 * @author caron
 * @since 7/30/2014
 */
public class TestCFGridWriter2 {


  @Test
  public void testSubset() throws Exception {
    String fileIn = TestDir.cdmUnitTestDir + "ft/grid/testCFwriter.nc";
    String fileOut = TestDir.temporaryLocalDataDir + "testCFwriter.nc";
    String varName = "Temperature";

    try (ucar.nc2.dt.grid.GridDataset gds = GridDataset.open(fileIn)) {
      List<String> gridList = new ArrayList<>();
      gridList.add(varName);

      NetcdfFileWriter writer = NetcdfFileWriter.createNew(NetcdfFileWriter.Version.netcdf3, fileOut);
      CFGridWriter2.writeFile(gds, gridList,
              new LatLonRect(new LatLonPointImpl(30, -109), 10, 50), null,
              1, null, null, 1, true,
              writer);
    }

    try (ucar.nc2.dt.grid.GridDataset result = GridDataset.open(fileOut)) {
      GeoGrid grid = result.findGridByName(varName);
      assertTrue(grid != null);
    }
  }

  @Test
  public void testSizeEstimate() throws Exception {
    String fileIn = TestDir.cdmUnitTestDir + "ft/grid/testCFwriter.nc";
    System.out.printf("Open %s%n", fileIn);

    try (ucar.nc2.dt.grid.GridDataset gds = GridDataset.open(fileIn)) {
      LatLonRect llbb = new LatLonRect(new LatLonPointImpl(30, -109), 10, 50);

      long totalSize = CFGridWriter2.makeSizeEstimate(gds, null, null, null, 1, null, null, 1, true);
      long subsetSize = CFGridWriter2.makeSizeEstimate(gds, null, llbb, null, 1, null, null, 1, true);
      System.out.printf("total size  = %d%n", totalSize);
      System.out.printf("subset size = %d%n", subsetSize);
      assertTrue(subsetSize < totalSize);

      String varName = "Temperature";
      List<String> gridList = new ArrayList<>();
      gridList.add(varName);

      totalSize = CFGridWriter2.makeSizeEstimate(gds, gridList, null, null, 1, null, null, 1, true);
      subsetSize = CFGridWriter2.makeSizeEstimate(gds, gridList, llbb, null, 1, null, null, 1, true);

      System.out.printf("total size Temp only  = %d%n", totalSize);
      System.out.printf("subset size Temp only = %d%n", subsetSize);
      assertTrue(subsetSize < totalSize);
    }
  }

  @Test
  public void testSizeEstimateTimeSubset() throws Exception {
    String fileIn = TestDir.cdmUnitTestDir + "ft/grid/cg/cg.ncml";
    System.out.printf("Open %s%n", fileIn);

    try (ucar.nc2.dt.grid.GridDataset gds = GridDataset.open(fileIn)) {
      CalendarDateRange dateRange = CalendarDateRange.of(CalendarDate.parseISOformat(null, "2006-06-07T12:00:00Z"), CalendarDate.parseISOformat(null, "2006-06-07T13:00:00Z"));

      long totalSize = CFGridWriter2.makeSizeEstimate(gds, null, null, null, 1, null, null, 1, true);
      long subsetSize = CFGridWriter2.makeSizeEstimate(gds, null, null, null, 1, null, dateRange, 1, true);
      System.out.printf("total size  = %d%n", totalSize);
      System.out.printf("subset size with date range = %d%n", subsetSize);
      assertTrue(subsetSize < totalSize);

      String varName = "CGusfc";
      List<String> gridList = new ArrayList<>();
      gridList.add(varName);

      totalSize = CFGridWriter2.makeSizeEstimate(gds, gridList, null, null, 1, null, null, 1, true);
      subsetSize = CFGridWriter2.makeSizeEstimate(gds, gridList, null, null, 1, null, dateRange, 1, true);

      System.out.printf("total size CGusfc only  = %d%n", totalSize);
      System.out.printf("subset size CGusfc with date range = %d%n", subsetSize);
      assertTrue(subsetSize < totalSize);
    }
  }

}
