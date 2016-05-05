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

import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import org.apache.commons.io.FileUtils;

import org.junit.runners.Parameterized;
import ucar.ma2.Array;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.dataset.CoordinateAxis;
import ucar.nc2.dataset.CoordinateAxis2D;
import ucar.nc2.dt.GridDatatype;
import ucar.nc2.dt.grid.GridDataset;
import ucar.nc2.ft2.coverage.*;
import ucar.unidata.util.test.CompareNetcdf;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;
import ucar.unidata.util.test.TestDir;

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

    result.add(new Object[]{TestDir.cdmUnitTestDir + "gribCollections/tp/GFS_Global_onedeg_ana_20150326_0600.grib2.ncx4", FeatureType.GRID, "Temperature_sigma"});         // SRC                               // TP
    result.add(new Object[]{TestDir.cdmUnitTestDir + "gribCollections/tp/GFSonedega.ncx4", FeatureType.GRID, "Pressure_surface"});                                         // TP
    result.add(new Object[]{TestDir.cdmUnitTestDir + "gribCollections/gfs_2p5deg/gfs_2p5deg.ncx4", FeatureType.GRID, "Best/Soil_temperature_depth_below_surface_layer"});  // TwoD Best
    result.add(new Object[]{TestDir.cdmUnitTestDir + "gribCollections/gfs_2p5deg/gfs_2p5deg.ncx4", FeatureType.FMRC, "TwoD/Soil_temperature_depth_below_surface_layer"});  // TwoD

    result.add(new Object[]{TestDir.cdmUnitTestDir + "ft/coverage/testCFwriter.nc", FeatureType.GRID, "Temperature"});

    return result;
  }

  String filename, field;
  FeatureType type;

  public TestGeoTiffWriter(String filename, FeatureType type, String field) {
    this.filename = filename;
    this.type = type;
    this.field = field;
  }

  @Test
  public void testWriteCoverage() throws IOException, InvalidRangeException {
    File f = new File(filename);
    String gridOut = TestDir.temporaryLocalDataDir + f.getName() + ".grid.tif";
    System.out.printf("geotiff read grid %s (%s) from %s write %s%n", field, type, filename, gridOut);

    Array dtArray;
    try (GridDataset gds = GridDataset.open(filename)) {
      GridDatatype grid = gds.findGridByName(field);
      assert grid != null;
      int rtindex = -1;
      int tindex = -1;
      CoordinateAxis timeAxis = grid.getCoordinateSystem().getTimeAxis();
      if (timeAxis instanceof CoordinateAxis2D) {
        int[] shape = timeAxis.getShape();
        rtindex = shape[0]-1;
        tindex = shape[1]-1;
      } else {
        CoordinateAxis rtimeAxis = grid.getCoordinateSystem().getRunTimeAxis();
        if (rtimeAxis != null)
          rtindex = (int) rtimeAxis.getSize() - 1; // last one
        timeAxis = grid.getCoordinateSystem().getTimeAxis1D();
        if (timeAxis != null)
          tindex =(int) timeAxis.getSize() - 1; // last one
      }
      dtArray = grid.readDataSlice(rtindex, -1, tindex, 0, -1, -1);

      try (GeotiffWriter writer = new GeotiffWriter(gridOut)) {
        writer.writeGrid(gds, grid, dtArray, true);
      }
    }

    // read it back in
    try (GeoTiff geotiff = new GeoTiff(gridOut)) {
      geotiff.read();
      if (show) System.out.printf("%s%n----------------------------------------------------%n", geotiff.showInfo());

      String gridOut2 = TestDir.temporaryLocalDataDir + f.getName() + ".coverage.tif";
      System.out.printf("geotiff2 read coverage %s write %s%n", filename, gridOut2);

      GeoReferencedArray covArray;
      try (FeatureDatasetCoverage cc = CoverageDatasetFactory.open(filename)) {
        assert cc != null;
        CoverageCollection gcd = cc.findCoverageDataset(type);
        Assert.assertNotNull(type.toString(), gcd);

        int pos = field.indexOf("/");
        String covName = (pos > 0) ? field.substring(pos+1) : field;

        Coverage coverage = gcd.findCoverage(covName);
        CoverageCoordAxis1D z = (CoverageCoordAxis1D) coverage.getCoordSys().getZAxis();
        SubsetParams params = new SubsetParams().set(SubsetParams.timePresent, true);
        if (z != null) params.set(SubsetParams.vertCoord, z.getCoordMidpoint(0));
        Assert.assertNotNull(covName, coverage);
        covArray = coverage.readData(params);

        try (GeotiffWriter writer = new GeotiffWriter(gridOut2)) {
          writer.writeGrid(covArray, true);
        }
      }

      CompareNetcdf.compareData(dtArray, covArray.getData());

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
