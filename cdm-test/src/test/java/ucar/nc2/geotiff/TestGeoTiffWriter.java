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

package ucar.nc2.geotiff;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import org.apache.commons.io.FileUtils;

import org.junit.runners.Parameterized;
import ucar.ma2.Array;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.dataset.CoordinateAxis;
import ucar.nc2.dt.GridDatatype;
import ucar.nc2.dt.grid.GridDataset;
import ucar.nc2.ft2.coverage.*;
import ucar.unidata.test.util.CompareNetcdf;
import ucar.unidata.test.util.NeedsCdmUnitTest;
import ucar.unidata.test.util.TestDir;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;

/**
 * GeoTiffWriter2 writing geotiffs
 *
 * @author caron
 * @since 7/31/2014
 */
@RunWith(Parameterized.class)
@Category(NeedsCdmUnitTest.class)
public class TestGeoTiffWriter {
  private boolean show = false;

  @Parameterized.Parameters(name = "{0}")
  public static List<Object[]> getTestParameters() {
    List<Object[]> result = new ArrayList<>();

    result.add(new Object[]{TestDir.cdmUnitTestDir + "gribCollections/tp/GFS_Global_onedeg_ana_20150326_0600.grib2.ncx3", "Temperature_sigma"});         // SRC                               // TP
    result.add(new Object[]{TestDir.cdmUnitTestDir + "gribCollections/tp/GFSonedega.ncx3", "Pressure_surface"});                                         // TP
    result.add(new Object[]{TestDir.cdmUnitTestDir + "gribCollections/gfs_2p5deg/gfs_2p5deg.ncx3", "Best/Soil_temperature_depth_below_surface_layer"});  // TwoD Best
    // result.add(new Object[]{TestDir.cdmUnitTestDir + "gribCollections/gfs_2p5deg/gfs_2p5deg.ncx3", "TwoD/Soil_temperature_depth_below_surface_layer"});  // TwoD

    //result.add(new Object[]{TestDir.cdmUnitTestDir + "ft/coverage/testCFwriter.nc", "Temperature"});
    //result.add(new Object[]{TestDir.cdmUnitTestDir + "ft/coverage/MM_cnrm_129_red.ncml", "geopotential"});

    return result;
  }

  String filename, field;

  public TestGeoTiffWriter(String filename, String field) {
    this.filename = filename;
    this.field = field;
  }


  @Test
  public void testWriteCoverage() throws IOException, InvalidRangeException {
    File f = new File(filename);
    String gridOut = TestDir.temporaryLocalDataDir + f.getName() + ".grid.tif";
    System.out.printf("geotiff read grid %s write %s%n", filename, gridOut);

    Array data1;
    try (GridDataset gds = GridDataset.open(filename)) {
      GridDatatype grid = gds.findGridByName(field);
      assert grid != null;
      CoordinateAxis timeAxis = grid.getCoordinateSystem().getTimeAxis1D();
      int tindex = (timeAxis == null) ? -1 : (int) timeAxis.getSize() - 1;
      data1 = grid.readDataSlice(tindex, 0, -1, -1);

      try (GeotiffWriter writer = new GeotiffWriter(gridOut)) {
        writer.writeGrid(gds, grid, data1, true);
      }
    }

    // read it back in
    try (GeoTiff geotiff = new GeoTiff(gridOut)) {
      geotiff.read();
      if (show) System.out.printf("%s%n----------------------------------------------------%n", geotiff.showInfo());

      String gridOut2 = TestDir.temporaryLocalDataDir + f.getName() + ".coverage.tif";
      System.out.printf("geotiff2 read coverage %s write %s%n", filename, gridOut2);

      GeoReferencedArray data2;
      try (CoverageDataset gcd = CoverageDatasetFactory.openCoverage(filename)) {
        assert gcd != null;
        Coverage coverage = gcd.findCoverage(field);
        assert coverage != null;
        data2 = coverage.readData(new SubsetParams()
                .set(SubsetParams.latestTime, true)
                .set(SubsetParams.vertIndex, 0));

        try (GeotiffWriter writer = new GeotiffWriter(gridOut2)) {
          writer.writeGrid(gcd, coverage, data2.getData(), true);
        }
      }

      CompareNetcdf.compareData(data1, data2.getData());

      // read it back in
      try (GeoTiff geotiff2 = new GeoTiff(gridOut2)) {
        geotiff2.read();
        if (show) System.out.printf("%s%n----------------------------------------------------%n", geotiff2.showInfo());

        Formatter out = new Formatter(System.out);
        geotiff.compare(geotiff2, out);
      }

      // compare file s are equal
      File file1 = new File(gridOut);
      File file2 = new File(gridOut2);

      assert FileUtils.contentEquals(file1, file2);
    }
  }

}
