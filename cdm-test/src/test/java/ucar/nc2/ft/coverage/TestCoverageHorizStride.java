/* Copyright Unidata */
package ucar.nc2.ft.coverage;

import org.junit.Assert;
import org.junit.Test;
import ucar.ma2.Array;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.ft2.coverage.*;
import ucar.nc2.util.Misc;
import ucar.nc2.util.Optional;
import ucar.unidata.test.util.TestDir;

import java.io.IOException;

/**
 * Created by John on 9/11/2015.
 */
public class TestCoverageHorizStride {

  @Test
  public void testBestStride() throws IOException, InvalidRangeException {
    String endpoint = TestDir.cdmUnitTestDir + "gribCollections/gfs_2p5deg/gfs_2p5deg.ncx3";
    String covName = "Ozone_Mixing_Ratio_isobaric";
    System.out.printf("Test Dataset %s%n", endpoint);

    try (CoverageDatasetCollection cc = CoverageDatasetFactory.open(endpoint)) {
      Assert.assertNotNull(endpoint, cc);
      CoverageDataset gcs = cc.findCoverageDataset(CoverageCoordSys.Type.Grid);
      Assert.assertNotNull("gcs", gcs);
      Coverage cover = gcs.findCoverage(covName);
      Assert.assertNotNull(covName, cover);
      CoverageCoordSys csys = cover.getCoordSys();
      int[] csysShape =  csys.getShape();
      System.out.printf("csys shape = %s%n", Misc.showInts(csysShape));

      SubsetParams params = new SubsetParams().set(SubsetParams.horizStride, 2);
      Optional<CoverageCoordSys> opt = csys.subset(params, false);
      if (!opt.isPresent()) {
        System.out.printf("err=%s%n", opt.getErrorMessage());
        assert false;
      }

      CoverageCoordSys subsetCoordSys = opt.get();
      int[] subsetShape =  subsetCoordSys.getShape();
      System.out.printf("csysSubset shape = %s%n", Misc.showInts(subsetShape));

      int n = csysShape.length;
      csysShape[n-1] = (csysShape[n-1]+1)/2;
      csysShape[n-2] = (csysShape[n-2]+1)/2;

      Assert.assertArrayEquals(csysShape, subsetShape);
    }
  }

  @Test
  public void TestGribCurvilinearHorizStride() throws IOException, InvalidRangeException {
    String endpoint = TestDir.cdmUnitTestDir + "ft/fmrc/rtofs/ofs.20091122/ofs_atl.t00z.F024.grb.grib2";  // GRIB Curvilinear
    System.out.printf("open %s%n", endpoint);

    try (CoverageDatasetCollection cc = CoverageDatasetFactory.open(endpoint)) {
      assert cc != null;
      Assert.assertEquals(1, cc.getCoverageDatasets().size());
      CoverageDataset gds = cc.getCoverageDatasets().get(0);
      Assert.assertNotNull(endpoint, gds);
      Assert.assertEquals(CoverageCoordSys.Type.Curvilinear, gds.getCoverageType());

      HorizCoordSys hcs = gds.getHorizCoordSys();
      Assert.assertNotNull(endpoint, hcs);
      Assert.assertTrue(endpoint, !hcs.isProjection());
      Assert.assertNull(endpoint, hcs.getTransform());

      String covName = "Mixed_layer_depth_surface";
      Coverage coverage = gds.findCoverage(covName);
      Assert.assertNotNull(covName, coverage);
      CoverageCoordSys csys = coverage.getCoordSys();
      int[] csysShape =  csys.getShape();
      System.out.printf("csys shape = %s%n", Misc.showInts(csysShape));

      SubsetParams params = new SubsetParams().set(SubsetParams.timePresent, true).set(SubsetParams.horizStride, 2);
      Optional<CoverageCoordSys> opt = csys.subset(params, false);
      if (!opt.isPresent()) {
        System.out.printf("err=%s%n", opt.getErrorMessage());
        assert false;
      }

      CoverageCoordSys subsetCoordSys = opt.get();
      int[] subsetShape =  subsetCoordSys.getShape();
      System.out.printf("csysSubset shape = %s%n", Misc.showInts(subsetShape));

      int n = csysShape.length;
      csysShape[n-1] = (csysShape[n-1]+1)/2;
      csysShape[n-2] = (csysShape[n-2]+1)/2;

      Assert.assertArrayEquals(csysShape, subsetShape);

      ///////////////////////////
      GeoReferencedArray geo = coverage.readData(params);
      System.out.printf("CoordSysForData shape=%s%n", Misc.showInts(geo.getCoordSysForData().getShape()));

      Array data = geo.getData();
      System.out.printf("data shape=%s%n", Misc.showInts(data.getShape()));
      Assert.assertArrayEquals(csysShape, data.getShape());
    }
  }
}
