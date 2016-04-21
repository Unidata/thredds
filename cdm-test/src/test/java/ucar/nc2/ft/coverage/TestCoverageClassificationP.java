/* Copyright */
package ucar.nc2.ft.coverage;

import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.dataset.CoordinateSystem;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.ft2.coverage.FeatureDatasetCoverage;
import ucar.nc2.ft2.coverage.CoverageCoordSys;
import ucar.nc2.ft2.coverage.CoverageCollection;
import ucar.nc2.ft2.coverage.CoverageDatasetFactory;
import ucar.nc2.ft2.coverage.adapter.DtCoverageCS;
import ucar.nc2.ft2.coverage.adapter.DtCoverageCSBuilder;
import ucar.nc2.ft2.coverage.adapter.DtCoverageDataset;
import ucar.unidata.test.util.NeedsCdmUnitTest;
import ucar.unidata.test.util.TestDir;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;

/**
 * Test GridCoverageDataset, and adapters
 *
 * @author caron
 * @since 5/28/2015
 */
@RunWith(Parameterized.class)
@Category(NeedsCdmUnitTest.class)
public class TestCoverageClassificationP {

  @Parameterized.Parameters(name = "{0}")
  public static List<Object[]> getTestParameters() {
    List<Object[]> result = new ArrayList<>();

    result.add(new Object[]{TestDir.cdmUnitTestDir + "ft/coverage/03061219_ruc.nc", FeatureType.GRID, 4, 4, 31});  // NUWG - has CoordinateAlias
    result.add(new Object[]{TestDir.cdmUnitTestDir + "ft/coverage/ECME_RIZ_201201101200_00600_GB", FeatureType.GRID, 4, 5, 5});  // scalar runtime
    result.add(new Object[]{TestDir.cdmUnitTestDir + "ft/coverage/MM_cnrm_129_red.ncml", FeatureType.FMRC, 6, 6, 1}); // ensemble, time-offset
    result.add(new Object[]{TestDir.cdmUnitTestDir + "ft/coverage/ukmo.nc", FeatureType.FMRC, 4, 5, 1});              // scalar vert
    result.add(new Object[]{TestDir.cdmUnitTestDir + "ft/coverage/testCFwriter.nc", FeatureType.GRID, 3, 5, 4});  // both x,y and lat,lon

    result.add(new Object[]{TestDir.cdmUnitTestDir + "ft/coverage/Run_20091025_0000.nc", FeatureType.CURVILINEAR, 4, 6, 20});  // x,y axis but no projection
    result.add(new Object[]{TestDir.cdmUnitTestDir + "ft/fmrc/rtofs/ofs.20091122/ofs_atl.t00z.F024.grb.grib2", FeatureType.CURVILINEAR, 4, 5, 7});  // GRIB Curvilinear
    result.add(new Object[]{TestDir.cdmUnitTestDir + "conventions/cf/mississippi.nc", FeatureType.CURVILINEAR, 4, 4, 24});  // netcdf Curvilinear

    result.add(new Object[]{TestDir.cdmUnitTestDir + "gribCollections/tp/GFS_Global_onedeg_ana_20150326_0600.grib2.ncx4", FeatureType.GRID, 4, 5, 65}); // SRC

    result.add(new Object[]{TestDir.cdmUnitTestDir + "formats/dmsp/F14200307192230.s.OIS", FeatureType.SWATH, 2, 3, 2});
    result.add(new Object[]{TestDir.cdmUnitTestDir + "formats/hdf4/AIRS.2003.01.24.116.L2.RetStd_H.v5.0.14.0.G07295101113.hdf", FeatureType.SWATH, 2, 3, 93});
    result.add(new Object[]{TestDir.cdmUnitTestDir + "formats/hdf4/ssec/MYD06_L2.A2006188.1655.005.2006194124315.hdf", FeatureType.SWATH, 3, 5, 28});

    return result;
  }

  String endpoint;
  FeatureType expectType;
  int domain, range, ncoverages;


  public TestCoverageClassificationP(String endpoint, FeatureType expectType, int domain, int range, int ncoverages) {
    this.endpoint = endpoint;
    this.expectType = expectType;
    this.domain = domain;
    this.range = range;
    this.ncoverages = ncoverages;
  }

  @Test
  public void testAdapter() throws IOException {
    System.out.printf("open %s%n", endpoint);

    try (DtCoverageDataset gds = DtCoverageDataset.open(endpoint)) {
      Assert.assertNotNull(endpoint, gds);
      Assert.assertEquals("NGrids", ncoverages, gds.getGrids().size());
      Assert.assertEquals(expectType, gds.getCoverageType());
    }

    // check DtCoverageCS
    try (NetcdfDataset ds = NetcdfDataset.openDataset(endpoint)) {
      Formatter errlog = new Formatter();
      DtCoverageCSBuilder builder = DtCoverageCSBuilder.classify(ds, errlog); // uses cs with largest # axes
      Assert.assertNotNull(errlog.toString(), builder);
      DtCoverageCS cs = builder.makeCoordSys();
      Assert.assertEquals(expectType, cs.getCoverageType());
      Assert.assertEquals("Domain", domain, CoordinateSystem.makeDomain(cs.getCoordAxes()).size());
      Assert.assertEquals("Range", range, cs.getCoordAxes().size());
    }
  }

  @Test
  public void testFactory() throws IOException {

    try (FeatureDatasetCoverage cc = CoverageDatasetFactory.open(endpoint)) {
      assert cc != null;
      Assert.assertEquals(1, cc.getCoverageCollections().size());
      CoverageCollection gds = cc.getCoverageCollections().get(0);
      Assert.assertNotNull(endpoint, gds);
      Assert.assertEquals("NGrids", ncoverages, gds.getCoverageCount());
      Assert.assertEquals(expectType, gds.getCoverageType());
    }
  }

}
