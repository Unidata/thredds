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
import ucar.nc2.dt.grid.GridDataset;
import ucar.nc2.ft2.coverage.CoverageCollection;
import ucar.nc2.ft2.coverage.CoverageDataset;
import ucar.nc2.ft2.coverage.CoverageDatasetFactory;
import ucar.nc2.ft2.coverage.SubsetParams;
import ucar.nc2.ft2.coverage.writer.CFGridCoverageWriter;
import ucar.unidata.test.util.NeedsCdmUnitTest;
import ucar.unidata.test.util.TestDir;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Test CFGridCoverageWriter
 *
 * @author caron
 * @since 7/8/2015
 */
@RunWith(Parameterized.class)
@Category(NeedsCdmUnitTest.class)
public class TestGridCoverageWriting {

  @Parameterized.Parameters(name = "{0}")
  public static List<Object[]> getTestParameters() {
    List<Object[]> result = new ArrayList<>();
    result.add(new Object[]{TestDir.cdmUnitTestDir + "ft/coverage/03061219_ruc.nc", Lists.newArrayList("P_sfc", "P_trop"), NetcdfFileWriter.Version.netcdf3});
    result.add(new Object[]{TestDir.cdmUnitTestDir + "ft/coverage/03061219_ruc.nc", Lists.newArrayList("P_sfc", "P_trop"), NetcdfFileWriter.Version.netcdf4});
    result.add(new Object[]{TestDir.cdmUnitTestDir + "ft/coverage/03061219_ruc.nc", Lists.newArrayList("P_sfc", "P_trop", "T"), NetcdfFileWriter.Version.netcdf3});

    result.add(new Object[]{TestDir.cdmUnitTestDir + "ft/coverage/ECME_RIZ_201201101200_00600_GB", Lists.newArrayList("Surface_pressure_surface"), NetcdfFileWriter.Version.netcdf3});  // scalar runtime
    result.add(new Object[]{TestDir.cdmUnitTestDir + "ft/coverage/testCFwriter.nc", Lists.newArrayList("PS", "Temperature"), NetcdfFileWriter.Version.netcdf3});  // both x,y and lat,lon */

    result.add(new Object[]{TestDir.cdmUnitTestDir + "gribCollections/gfs_2p5deg/gfs_2p5deg.ncx3", Lists.newArrayList("Best/Soil_temperature_depth_below_surface_layer"), NetcdfFileWriter.Version.netcdf4});  // TwoD Best
    result.add(new Object[]{TestDir.cdmUnitTestDir + "gribCollections/gfs_2p5deg/gfs_2p5deg.ncx3", Lists.newArrayList("TwoD/Soil_temperature_depth_below_surface_layer"), NetcdfFileWriter.Version.netcdf4 });  // TwoD

    return result;
  }

  String endpoint;
  List<String> covList;
  NetcdfFileWriter.Version version;

  public TestGridCoverageWriting(String endpoint, List<String> covList, NetcdfFileWriter.Version version) {
    this.endpoint = endpoint;
    this.covList = covList;
    this.version = version;
  }

  @Test
  public void writeTestFile() throws IOException, InvalidRangeException {
    System.out.printf("Test Dataset %s%n", endpoint);
    File tempFile = TestDir.getTempFile();
    System.out.printf(" write to %s%n", tempFile.getAbsolutePath());

    // write the file
    try (CoverageCollection cc = CoverageDatasetFactory.open(endpoint)) {
      Assert.assertNotNull(endpoint, cc);
      Assert.assertEquals(1, cc.getCoverageDatasets().size());
      CoverageDataset gcs = cc.getCoverageDatasets().get(0);;

      for (String covName : covList) {
        Assert.assertNotNull(covName, gcs.findCoverage(covName));
      }

      NetcdfFileWriter writer = NetcdfFileWriter.createNew(version, tempFile.getPath(), null);

      CFGridCoverageWriter.writeFile(gcs, covList, new SubsetParams(), false, writer);
    }

    // open the new file as a Coverage
    try (CoverageCollection cc = CoverageDatasetFactory.open(tempFile.getPath())) {
      Assert.assertNotNull(endpoint, cc);
      Assert.assertEquals(1, cc.getCoverageDatasets().size());
      CoverageDataset gcs = cc.getCoverageDatasets().get(0);

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
    }

  }

}
