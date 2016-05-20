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

import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.Range;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFileWriter;
import ucar.nc2.Variable;
import ucar.nc2.dataset.CoordinateAxis;
import ucar.nc2.dt.GridCoordSystem;
import ucar.nc2.grib.collection.GribIosp;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.time.CalendarDateRange;
import ucar.nc2.util.DebugFlags;
import ucar.nc2.util.DebugFlagsImpl;
import ucar.unidata.geoloc.LatLonPointImpl;
import ucar.unidata.geoloc.LatLonRect;
import ucar.unidata.geoloc.ProjectionPointImpl;
import ucar.unidata.geoloc.ProjectionRect;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;
import ucar.unidata.util.test.TestDir;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertTrue;

/**
 * Test CFGridWriter2
 *
 * @author caron
 * @since 7/30/2014
 */
@Category(NeedsCdmUnitTest.class)
public class TestCFWriter2 {
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

  @Test
  public void testSizeEstimateOnTP() throws Exception {
    String fileIn = TestDir.cdmUnitTestDir + "gribCollections/tp/GFSonedega.ncx3";
    System.out.printf("Open %s%n", fileIn);

    try (GridDataset dataset = GridDataset.open(fileIn)) {

      List<String> gridList = new ArrayList<>();
      gridList.add("Cloud_mixing_ratio_isobaric");

      //   static public long makeSizeEstimate(ucar.nc2.dt.GridDataset gds, List<String> gridList,
      //                                       LatLonRect llbb, ProjectionRect projRect, int horizStride, Range zRange,
      //                                       CalendarDateRange dateRange, int stride_time, boolean addLatLon) throws IOException, InvalidRangeException {

      CalendarDate date = CalendarDate.parseISOformat(null, "2015-03-26T06:00:00Z");
      CalendarDateRange dateRange = CalendarDateRange.of( date, date);
      long totalSize = CFGridWriter2.makeSizeEstimate(dataset, gridList, null, null, 1, null, null, 1, true);
      long subsetSize = CFGridWriter2.makeSizeEstimate(dataset, gridList, null, null, 1, null, dateRange, 1, true);

      System.out.printf("total size Temp only  = %d%n", totalSize);
      System.out.printf("subset size Temp only = %d%n", subsetSize);
      assertTrue(subsetSize == totalSize/2);
    }
  }

  @Test
  public void testWriteFileOnTP() throws Exception {
    testFileSize( TestDir.cdmUnitTestDir + "gribCollections/tp/GFSonedega.ncx3", "Cloud_mixing_ratio_isobaric", "2015-03-26T06:00:00Z", null, null, true);
  }

  // time_start=2014-10-01T21%3A00%3A00Z&time_end=2014-10-02T21%3A00%3A00Z

  @Ignore("not visible on spock")
  @Test
  public void testWriteFileOnNarr() throws Exception {

    testFileSize( "B:/ncdc/0409/narr/Narr_A_fc.ncx3", "Accum_snow_surface", "2014-10-01T21:00:00Z", null, null, false);
    testFileSize( "B:/ncdc/0409/narr/Narr_A_fc.ncx3", "Accum_snow_surface", "2014-10-01T21:00:00Z", "2014-10-02T21:00:00Z", null, false);
    testFileSize("B:/ncdc/0409/narr/Narr_A_fc.ncx3", "Convective_cloud_cover_entire_atmosphere_3_Hour_Average",
            "2014-10-01T21:00:00Z", "2014-10-02T21:00:00Z", null, false);
  }

  @Ignore("not visible on spock")
  @Test
  public void testWriteFileOnNarr2() throws Exception {
    GribIosp.setDebugFlags(new DebugFlagsImpl("Grib/indexOnly"));
    testFileSize("B:/ncdc/0409/narr/Narr_A_fc.ncx3", "Accum_snow_surface,Convective_cloud_cover_entire_atmosphere_3_Hour_Average",
            "2014-10-01T21:00:00Z", "2014-10-02T21:00:00Z", null, true);
    GribIosp.setDebugFlags(new DebugFlagsImpl(""));
  }

  @Ignore("not visible on spock")
  @Test
  public void testWriteFileOnNarr3() throws Exception {
    GribIosp.setDebugFlags(new DebugFlagsImpl("Grib/indexOnly"));
    ProjectionRect rect = new ProjectionRect( new ProjectionPointImpl(-5645, -4626), 11329, 8992);
    testFileSize("B:/ncdc/0409/narr/Narr_A_fc.ncx3", "Accum_snow_surface,Convective_cloud_cover_entire_atmosphere_3_Hour_Average",
            "2014-10-01T21:00:00Z", "2014-10-02T21:00:00Z", rect, true);
    GribIosp.setDebugFlags(new DebugFlagsImpl(""));
  }

  @Ignore("not visible on spock")
  @Test
  public void testWriteFileOnNarr4() throws Exception {
    GribIosp.setDebugFlags(new DebugFlagsImpl("Grib/indexOnly"));
    ProjectionRect rect = new ProjectionRect( new ProjectionPointImpl(-5645, -4626), 11329, 8992);
    testFileSize("B:/ncdc/0409/narr/Narr_A_fc.ncx3", "Convective_cloud_cover_entire_atmosphere_3_Hour_Average,Accum_snow_surface",
            "2014-10-01T21:00:00Z", "2014-10-02T21:00:00Z", rect, true);
    GribIosp.setDebugFlags(new DebugFlagsImpl(""));
  }

  private void testFileSize(String fileIn, String gridNames, String startDate, String endDate, ProjectionRect rect, boolean writeFile) throws Exception {
    System.out.printf("Open %s%n", fileIn);

    String dir = TestDir.temporaryLocalDataDir;
    String fileOut = dir+"/testWriteFileOnTP.nc";
    long subsetSize;

    List<String> gridList = new ArrayList<>();
    String[] grids = gridNames.split(",");
    Collections.addAll(gridList, grids);

    try (GridDataset dataset = GridDataset.open(fileIn)) {

      CalendarDate date1 = CalendarDate.parseISOformat(null, startDate);
      CalendarDate date2 = (endDate == null) ? date1 : CalendarDate.parseISOformat(null, endDate);
      CalendarDateRange dateRange = CalendarDateRange.of( date1, date2);

            //   static public long makeSizeEstimate(ucar.nc2.dt.GridDataset gds, List<String> gridList,
      //                                       LatLonRect llbb, ProjectionRect projRect, int horizStride, Range zRange,
      //                                       CalendarDateRange dateRange, int stride_time, boolean addLatLon) throws IOException, InvalidRangeException {

      //long totalSize = CFGridWriter2.makeSizeEstimate(dataset, null, null, null, 1, null, null, 1, true);
      //System.out.printf(" estSize size all = %d%n", totalSize);

      //long estSize = CFGridWriter2.makeSizeEstimate(dataset, gridList, null, rect, 1, null, dateRange, 1, false);
      //System.out.printf(" estSize size %s only = %d%n", gridNames, estSize);

      //     static public long writeFile(ucar.nc2.dt.GridDataset gds, List<String> gridList,
      //                                       LatLonRect llbb, ProjectionRect projRect, int horizStride, Range zRange,
      //                                       CalendarDateRange dateRange, int stride_time, boolean addLatLon, NetcdfFileWriter writer) throws IOException, InvalidRangeException {

      if (writeFile) {
        NetcdfFileWriter writer = NetcdfFileWriter.createNew(NetcdfFileWriter.Version.netcdf3, fileOut);
        subsetSize = CFGridWriter2.writeFile(dataset, gridList, null, rect, 1, null, dateRange, 1, false, writer);
        System.out.printf("  subset size %s only = %d%n", gridNames, subsetSize);
        //assert subsetSize == estSize;
      } else
        return;
    }

    try (NetcdfFile ncfile = NetcdfFile.open(fileOut)) {

      long total = 0;
      for (String grid : grids) {
        Variable w = ncfile.findVariable(null, grid);
        assert w != null;
        total += w.getSize()*w.getElementSize();
        System.out.printf(" actual size of %s = %d%n", grid, w.getSize() * w.getElementSize());
      }
      System.out.printf(" actual grids size = %d%n", total);

      total = 0;
      for (Variable v : ncfile.getVariables())
        total += v.getSize()*v.getElementSize();
      System.out.printf(" actual all variable size = %d%n", total);
    }
  }




}
