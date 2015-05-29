/* Copyright */
package ucar.nc2.ft.coverage;

import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ucar.nc2.dataset.CoordinateSystem;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.ft2.coverage.CoverageDatasetFactory;
import ucar.nc2.ft2.coverage.adapter.DtCoverageCS;
import ucar.nc2.ft2.coverage.adapter.DtCoverageCSBuilder;
import ucar.nc2.ft2.coverage.adapter.DtCoverageDataset;
import ucar.nc2.ft2.coverage.grid.GridCoordSys;
import ucar.nc2.ft2.coverage.grid.GridCoverageDataset;
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
public class TestCoverage {

  @Parameterized.Parameters(name="{0}")
  public static List<Object[]> getTestParameters() {
    List<Object[]> result = new ArrayList<>();

    result.add(new Object[]{TestDir.cdmUnitTestDir + "ft/coverage/03061219_ruc.nc", GridCoordSys.Type.Grid, 4, 4, 31});
    result.add(new Object[]{TestDir.cdmUnitTestDir + "ft/coverage/ECME_RIZ_201201101200_00600_GB", GridCoordSys.Type.Grid, 4, 5, 5});
    result.add(new Object[]{TestDir.cdmUnitTestDir + "ft/coverage/MM_cnrm_129_red.ncml", GridCoordSys.Type.Fmrc, 6, 6, 1});
    result.add(new Object[]{TestDir.cdmUnitTestDir + "ft/coverage/ukmo.nc", GridCoordSys.Type.Fmrc, 4, 4, 1});

    return result;
  }

  String endpoint;
  GridCoordSys.Type expectType;
  int domain, range, ncoverages;


  public TestCoverage(String endpoint, GridCoordSys.Type expectType, int domain, int range, int ncoverages) {
    this.endpoint = endpoint;
    this.expectType = expectType;
    this.domain = domain;
    this.range = range;
    this.ncoverages = ncoverages;
  }

  @Test
  public void testAdapter() throws IOException {

    try (DtCoverageDataset gds = DtCoverageDataset.open(endpoint)) {
      Assert.assertNotNull(endpoint, gds);
      Assert.assertEquals("NGrids", ncoverages, gds.getGrids().size());
      Assert.assertEquals(expectType, gds.getCoverageType());
    }

    // check DtCoverageCS
    try (NetcdfDataset ds = NetcdfDataset.openDataset(endpoint)) {
      DtCoverageCSBuilder builder = DtCoverageCSBuilder.classify(ds, null);
      Assert.assertNotNull(builder);
      DtCoverageCS cs = builder.makeCoordSys();
      Assert.assertEquals(expectType, cs.getCoverageType());
      Assert.assertEquals("NIndCoordAxes", domain, CoordinateSystem.makeDomain(cs.getCoordAxes()).size());
      Assert.assertEquals("NCoordAxes", range, cs.getCoordAxes().size());
    }
  }

  @Test
  public void testFactory() throws IOException {

    try (GridCoverageDataset gds = CoverageDatasetFactory.openGridCoverage(endpoint)) {
      Assert.assertNotNull(endpoint, gds);
      Assert.assertEquals(ncoverages, gds.getGrids().size());
      Assert.assertEquals(expectType, gds.getCoverageType());
    }
  }

}
