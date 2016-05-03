/*
 * Copyright (c) 1998 - 2009. University Corporation for Atmospheric Research/Unidata
 * Portions of this software were developed by the Unidata Program at the
 * University Corporation for Atmospheric Research.
 *
 * Access and use of this software shall impose the following obligations
 * and understandings on the user. The user is granted the right, without
 * any fee or cost, to use, copy, modify, alter, enhance and distribute
 * this software, and any derivative works thereof, and its supporting
 * documentation for any purpose whatsoever, provided that this entire
 * notice appears in all copies of the software, derivative works and
 * supporting documentation.  Further, UCAR requests that the user credit
 * UCAR/Unidata in any publications that result from the use of this
 * software or in any product that includes this software. The names UCAR
 * and/or Unidata, however, may not be used in any advertising or publicity
 * to endorse or promote any products or commercial entity unless specific
 * written permission is obtained from UCAR/Unidata. The user also
 * understands that UCAR/Unidata is not obligated to provide the user with
 * any support, consulting, training or assistance of any kind with regard
 * to the use, operation and performance of this software nor to provide
 * the user with any updates, revisions, new versions or "bug fixes."
 *
 * THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 * INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 * FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 * NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 * WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */

package ucar.nc2.dt.grid;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import ucar.nc2.Variable;
import ucar.nc2.dt.GridDatatype;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.time.CalendarDateRange;
import ucar.unidata.geoloc.LatLonRect;
import ucar.unidata.geoloc.LatLonPointImpl;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;
import ucar.unidata.util.test.TestDir;

import java.util.List;
import java.util.ArrayList;

import static org.junit.Assert.assertTrue;

/**
 * Use CFGridWriter to write a netcdf-3 file
 *
 * @author caron
 * @since May 28, 2009
 */
@Category(NeedsCdmUnitTest.class)
public class TestCFWriter {

  @Test
  public void testSubset() throws Exception {
    String fileIn = TestDir.cdmUnitTestDir + "ft/grid/testCFwriter.nc";
    String fileOut = TestDir.temporaryLocalDataDir + "testCFwriter.nc";
    String varName = "Temperature";

    ucar.nc2.dt.grid.GridDataset gds = GridDataset.open(fileIn);
    List<String> gridList = new ArrayList<>();
    gridList.add(varName);

    CFGridWriter writer = new CFGridWriter();
    writer.makeFile(fileOut, gds, gridList,
            new LatLonRect(new LatLonPointImpl(30, -109), 10, 50),
            null,
            true,
            1, 1, 1);

    gds.close();

    ucar.nc2.dt.grid.GridDataset result = GridDataset.open(fileOut);
    GeoGrid grid = result.findGridByName(varName);
    assertTrue(grid != null);

    result.close();

  }

  @Test
  public void testSizeEstimate() throws Exception {
    String fileIn = TestDir.cdmUnitTestDir + "ft/grid/testCFwriter.nc";
    System.out.printf("Open %s%n", fileIn);

    ucar.nc2.dt.grid.GridDataset gds = GridDataset.open(fileIn);
    List<String> gridList = new ArrayList<String>();
    for  (GridDatatype grid : gds.getGrids()) {
      Variable v = grid.getVariable();
      System.out.printf("  %20s == %d%n", grid.getName(), v.getSize() * v.getElementSize());
      gridList.add(grid.getName());
    }

    CFGridWriter writer = new CFGridWriter();
    //  makeGridFileSizeEstimate(ucar.nc2.dt.GridDataset gds, List<String> gridList,
    //			LatLonRect llbb, int horizStride, Range zRange, CalendarDateRange dateRange, int stride_time, boolean addLatLon)
    long totalSize = writer.makeGridFileSizeEstimate(gds, gridList, (LatLonRect) null, 1, null, null, 1, false);
    long subsetSize = writer.makeGridFileSizeEstimate(gds, gridList, new LatLonRect(new LatLonPointImpl(30, -109), 10, 50), 1, null, null, 1, false);

    System.out.printf("total size  = %d%n", totalSize);
    System.out.printf("subset size = %d%n", subsetSize);
    assertTrue(subsetSize < totalSize);

    String varName = "Temperature";
    gridList = new ArrayList<String>();
    gridList.add(varName);

    totalSize = writer.makeGridFileSizeEstimate(gds, gridList, (LatLonRect) null, 1, null, null, 1, false);
    subsetSize = writer.makeGridFileSizeEstimate(gds, gridList, new LatLonRect(new LatLonPointImpl(30, -109), 10, 50), 1, null, null, 1, false);

    System.out.printf("total size Temp only  = %d%n", totalSize);
    System.out.printf("subset size Temp only = %d%n", subsetSize);
    assertTrue(subsetSize < totalSize);

    gds.close();
  }

  @Test
  public void testSizeEstimateTimeSubset() throws Exception {
    String fileIn = TestDir.cdmUnitTestDir + "ft/grid/cg/cg.ncml";
    System.out.printf("Open %s%n", fileIn);

    ucar.nc2.dt.grid.GridDataset gds = GridDataset.open(fileIn);
    List<String> gridList = new ArrayList<String>();
    for  (GridDatatype grid : gds.getGrids()) {
      Variable v = grid.getVariable();
      System.out.printf("  %20s == %d%n", grid.getName(), v.getSize() * v.getElementSize());
      gridList.add(grid.getName());
    }

    CFGridWriter writer = new CFGridWriter();
    CalendarDateRange dateRange = CalendarDateRange.of(CalendarDate.parseISOformat(null, "2006-06-07T12:00:00Z"), CalendarDate.parseISOformat(null, "2006-06-07T13:00:00Z"));
    long totalSize = writer.makeGridFileSizeEstimate(gds, gridList, (LatLonRect) null, 1, null, null, 1, false);
    long subsetSize = writer.makeGridFileSizeEstimate(gds, gridList, (LatLonRect) null, 1, null, dateRange, 1, false);

    System.out.printf("total size Temp only  = %d%n", totalSize);
    System.out.printf("subset size with date range = %d%n", subsetSize);
    assertTrue(subsetSize < totalSize);

    gds.close();
  }



}
