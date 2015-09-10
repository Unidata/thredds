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
package ucar.nc2.ft.coverage;

import org.junit.Assert;
import org.junit.Test;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.ft2.coverage.*;
import ucar.nc2.time.CalendarDate;
import ucar.unidata.test.util.TestDir;

import java.io.IOException;

/**
 * Description
 *
 * @author John
 * @since 8/24/2015
 */
public class TestCoverageCurvilinear {

  @Test
  public void TestGribCurvilinear() throws IOException, InvalidRangeException {
    String endpoint = TestDir.cdmUnitTestDir + "ft/fmrc/rtofs/ofs.20091122/ofs_atl.t00z.F024.grb.grib2";  // GRIB Curvilinear
    System.out.printf("open %s%n", endpoint);

    try (CoverageDatasetCollection cc = CoverageDatasetFactory.open(endpoint)) {
      assert cc != null;
      Assert.assertEquals(1, cc.getCoverageDatasets().size());
      CoverageDataset gds = cc.getCoverageDatasets().get(0);
      Assert.assertNotNull(endpoint, gds);
      Assert.assertEquals(CoverageCoordSys.Type.Curvilinear, gds.getCoverageType());
      Assert.assertEquals(7, gds.getCoverageCount());

      HorizCoordSys hcs = gds.getHorizCoordSys();
      Assert.assertNotNull(endpoint, hcs);
      Assert.assertTrue(endpoint, hcs.hasLatLon);
      Assert.assertTrue(endpoint, !hcs.hasProjection);
      Assert.assertNull(endpoint, hcs.getTransform());

      String covName = "Mixed_layer_depth_surface";
      Coverage cover = gds.findCoverage(covName);
      Assert.assertNotNull(covName, cover);

      GeoReferencedArray geo = cover.readData(new SubsetParams());
      TestCoverageSubsetTime.testGeoArray(geo, null, null, null);
    }

  }

  @Test
  public void TestNetcdfCurvilinear() throws IOException {
    String endpoint = TestDir.cdmUnitTestDir + "ft/coverage/Run_20091025_0000.nc";  // NetCDF has 2D and 1D
    System.out.printf("open %s%n", endpoint);

    try (CoverageDatasetCollection cc = CoverageDatasetFactory.open(endpoint)) {
      assert cc != null;
      Assert.assertEquals(1, cc.getCoverageDatasets().size());
      CoverageDataset gds = cc.getCoverageDatasets().get(0);
      Assert.assertNotNull(endpoint, gds);
      Assert.assertEquals(CoverageCoordSys.Type.Curvilinear, gds.getCoverageType());
      Assert.assertEquals(22, gds.getCoverageCount());
    }
  }

  @Test
  public void TestNetcdfCurvilinear2() throws IOException {
    String endpoint = TestDir.cdmUnitTestDir + "transforms/UTM/artabro_20120425.nc";  // NetCDF Curvilinear 2D only
    System.out.printf("open %s%n", endpoint);

    try (CoverageDatasetCollection cc = CoverageDatasetFactory.open(endpoint)) {
      assert cc != null;
      Assert.assertEquals(1, cc.getCoverageDatasets().size());
      CoverageDataset gds = cc.getCoverageDatasets().get(0);
      Assert.assertNotNull(endpoint, gds);
      Assert.assertEquals(CoverageCoordSys.Type.Curvilinear, gds.getCoverageType());
      Assert.assertEquals(10, gds.getCoverageCount());
    }
  }

}
