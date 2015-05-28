/* Copyright */
package ucar.nc2.ft.coverage;

import org.junit.Assert;
import org.junit.Test;
import ucar.nc2.ft2.coverage.CoverageDatasetFactory;
import ucar.nc2.ft2.coverage.adapter.DtCoverageDataset;
import ucar.nc2.ft2.coverage.grid.GridCoordSys;
import ucar.nc2.ft2.coverage.grid.GridCoverageDataset;
import ucar.unidata.test.util.TestDir;

import java.io.IOException;

/**
 * Describe
 *
 * @author caron
 * @since 5/28/2015
 */
public class TestCoverage {

  @Test
  public void testAdapter() throws IOException {
    String endpoint = TestDir.cdmUnitTestDir + "ft/fmrc/ukmo.nc";

    try (DtCoverageDataset gds = DtCoverageDataset.open(endpoint)) {
      Assert.assertNotNull(endpoint, gds);
      assert gds.getGrids().size() > 0;
      GridCoordSys.Type ctype = gds.getCoverageType();
      Assert.assertEquals(GridCoordSys.Type.Fmrc, ctype);
    }
  }

  @Test
  public void testFactory() throws IOException {
    String endpoint = TestDir.cdmUnitTestDir + "ft/fmrc/ukmo.nc";

    try (GridCoverageDataset gds = CoverageDatasetFactory.openGridCoverage(endpoint)) {
      Assert.assertNotNull(endpoint, gds);
      assert gds.getGrids().size() > 0;
      GridCoordSys.Type ctype = gds.getCoverageType();
      Assert.assertEquals(GridCoordSys.Type.Fmrc, ctype);
    }
  }
}
