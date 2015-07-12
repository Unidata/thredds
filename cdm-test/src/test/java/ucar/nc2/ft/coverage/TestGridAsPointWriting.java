/* Copyright */
package ucar.nc2.ft.coverage;

import com.google.common.collect.Lists;
import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.NetcdfFileWriter;
import ucar.nc2.ft2.coverage.CoverageDataset;
import ucar.nc2.ft2.coverage.CoverageDatasetFactory;
import ucar.nc2.ft2.coverage.SubsetParams;
import ucar.nc2.ft2.coverage.writer.DSGGridCoverageWriter;
import ucar.unidata.test.util.NeedsCdmUnitTest;
import ucar.unidata.test.util.TestDir;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Test DSGGridCoverageWriter
 *
 * @author caron
 * @since 7/8/2015
 */
@RunWith(Parameterized.class)
@Category(NeedsCdmUnitTest.class)
public class TestGridAsPointWriting {

    @Parameterized.Parameters(name = "{0}")
    public static List<Object[]> getTestParameters() {
      List<Object[]> result = new ArrayList<>();
      result.add(new Object[]{TestDir.cdmUnitTestDir + "ft/coverage/03061219_ruc.nc", Lists.newArrayList("P_sfc", "P_trop"), NetcdfFileWriter.Version.netcdf3});
      return result;
    }

    String endpoint;
    List<String> covList;
    NetcdfFileWriter.Version version;

    public TestGridAsPointWriting(String endpoint, List<String> covList, NetcdfFileWriter.Version version) {
      this.endpoint = endpoint;
      this.covList = covList;
      this.version = version;
    }

    @Test
    public void writeTestFile() throws IOException, InvalidRangeException {
      System.out.printf("Test Dataset %s%n", endpoint);
      File tempFile = TestDir.getTempFile();
      System.out.printf(" write to %s%n", tempFile.getAbsolutePath());

      try (CoverageDataset gcd = CoverageDatasetFactory.openCoverage(endpoint)) {
        Assert.assertNotNull(endpoint, gcd);

        for (String covName : covList) {
          Assert.assertNotNull(covName, gcd.findCoverage(covName));
        }

        NetcdfFileWriter ncwriter = NetcdfFileWriter.createNew(version, tempFile.getPath(), null);

        DSGGridCoverageWriter writer = new DSGGridCoverageWriter(gcd, covList, new SubsetParams());
      }

      /* open the new file as a Coverage
      try (GridCoverageDataset gcs = CoverageDatasetFactory.openGridCoverage(tempFile.getPath())) {
        Assert.assertNotNull(tempFile.getPath(), gcs);

        for (String covName : covList) {
          Assert.assertNotNull(covName, gcs.findCoverage(covName));
        }
      }

      // open the new file as a Grid
      try (GridDataset gds = GridDataset.open(tempFile.getPath())) {
        Assert.assertNotNull(tempFile.getPath(), gds);

        for (String covName : covList) {
          Assert.assertNotNull(covName, gds.findGridByName(covName));
        }
      }  */

    }
}
