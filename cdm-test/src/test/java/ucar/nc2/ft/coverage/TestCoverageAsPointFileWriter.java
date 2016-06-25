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
import ucar.nc2.constants.FeatureType;
import ucar.nc2.dt.grid.GridDataset;
import ucar.nc2.ft.*;
import ucar.nc2.ft.point.writer.CFPointWriter;
import ucar.nc2.ft2.coverage.FeatureDatasetCoverage;
import ucar.nc2.ft2.coverage.CoverageCollection;
import ucar.nc2.ft2.coverage.CoverageDatasetFactory;
import ucar.nc2.ft2.coverage.SubsetParams;
import ucar.nc2.ft2.coverage.writer.CoverageAsPoint;
import ucar.unidata.geoloc.LatLonPointImpl;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;
import ucar.unidata.util.test.TestDir;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;

/**
 * Test CoverageAsPoint
 *
 * @author caron
 * @since 7/8/2015
 */
@RunWith(Parameterized.class)
@Category(NeedsCdmUnitTest.class)
public class TestCoverageAsPointFileWriter {

    @Parameterized.Parameters(name = "{0}")
    public static List<Object[]> getTestParameters() {
      List<Object[]> result = new ArrayList<>();
      result.add(new Object[]{TestDir.cdmUnitTestDir + "ft/coverage/03061219_ruc.nc", Lists.newArrayList("P_sfc", "P_trop"), NetcdfFileWriter.Version.netcdf3});
      return result;
    }

    String endpoint;
    List<String> covList;
    NetcdfFileWriter.Version version;

    public TestCoverageAsPointFileWriter(String endpoint, List<String> covList, NetcdfFileWriter.Version version) {
      this.endpoint = endpoint;
      this.covList = covList;
      this.version = version;
    }

    @Test
    public void writeTestFile() throws IOException, InvalidRangeException {
      System.out.printf("Test Dataset %s%n", endpoint);
      File tempFile = TestDir.getTempFile();
      System.out.printf(" write to %s%n", tempFile.getAbsolutePath());

      try (FeatureDatasetCoverage cc = CoverageDatasetFactory.open(endpoint)) {
        Assert.assertNotNull(endpoint, cc);
        Assert.assertEquals(1, cc.getCoverageCollections().size());
        CoverageCollection gds = cc.getCoverageCollections().get(0);

        for (String covName : covList) {
          Assert.assertNotNull(covName, gds.findCoverage(covName));
        }

        SubsetParams params = new SubsetParams();
        params.setVariables( covList);
        params.setLatLonPoint(new LatLonPointImpl(35.0, -140.0));

        CoverageAsPoint writer = new CoverageAsPoint(gds, covList, params);
        FeatureDatasetPoint fdp = writer.asFeatureDatasetPoint();
        Assert.assertEquals(1, fdp.getPointFeatureCollectionList().size());

        DsgFeatureCollection fc = fdp.getPointFeatureCollectionList().get(0);

        CFPointWriter.writeFeatureCollection(fdp, tempFile.getAbsolutePath(), NetcdfFileWriter.Version.netcdf3);
      }

      // open the new file as a Coverage
      try (FeatureDatasetCoverage gcs = CoverageDatasetFactory.open(tempFile.getPath())) {
        Assert.assertNull(tempFile.getPath(), gcs);
      }

      // open the new file as a Grid
      try (GridDataset gds = GridDataset.open(tempFile.getPath())) {
        Assert.assertEquals(0, gds.getGrids().size());
      }

      // open the new file as a Point Feature dataset
      Formatter errlog = new Formatter();
      try (FeatureDataset fd = FeatureDatasetFactoryManager.open(FeatureType.ANY_POINT, tempFile.getPath(), null, errlog)) {
        Assert.assertNotNull(tempFile.getPath(), fd);
        Assert.assertEquals(FeatureType.STATION, fd.getFeatureType());

        for (String covName : covList)
          Assert.assertNotNull(covName, fd.getDataVariable(covName));

        FeatureDatasetPoint fdp = (FeatureDatasetPoint) fd;
        Assert.assertEquals(1, fdp.getPointFeatureCollectionList().size());
        DsgFeatureCollection fc = fdp.getPointFeatureCollectionList().get(0);
        Assert.assertNotNull("FeatureCollection", fc);
        Assert.assertEquals(FeatureType.STATION, fc.getCollectionFeatureType());

        StationTimeSeriesFeatureCollection stColl = (StationTimeSeriesFeatureCollection) fc;
        Assert.assertEquals(1, stColl.getStationFeatures().size());

        int count = 0;
        StationTimeSeriesFeature curr = null;
        for (StationTimeSeriesFeature stn : stColl) {
          count++;
          curr = stn;
        }
        Assert.assertEquals(1, count);
        Assert.assertNotNull("single station", curr);

        count = 0;
        for (PointFeature pf : curr) {
          count++;
        }
        Assert.assertEquals(2, count);
      }

    }
}
