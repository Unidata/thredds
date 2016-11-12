/* Copyright Unidata */
package ucar.nc2.ft.coverage;

import com.google.common.collect.Lists;
import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.NetcdfFileWriter;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dt.grid.GridDataset;
import ucar.nc2.ft2.coverage.*;
import ucar.nc2.ft2.coverage.writer.CFGridCoverageWriter2;
import ucar.nc2.grib.collection.Grib;
import ucar.nc2.util.Misc;
import ucar.nc2.util.Optional;
import ucar.unidata.geoloc.LatLonPointImpl;
import ucar.unidata.geoloc.LatLonRect;
import ucar.unidata.util.test.TestDir;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;

import java.io.File;
import java.io.IOException;

/**
 * Test Coverage Cross-Seam subsetting by writing a file.
 *
 * @author caron
 * @since 9/12/2015.
 */
public class TestCoverageCrossSeamWriteFile {

  @Test
  @Category(NeedsCdmUnitTest.class)
  public void testSubsetDt() throws Exception { // try a subset without crossing the seam
    String filename = TestDir.cdmUnitTestDir + "ft/grid/GFS_Global_onedeg_20081229_1800.grib2.nc";
    System.out.printf("open %s%n", filename);

    try (FeatureDatasetCoverage cc = CoverageDatasetFactory.open(filename)) {
      Assert.assertNotNull(filename, cc);
      CoverageCollection gcs = cc.findCoverageDataset(FeatureType.GRID);
      Assert.assertNotNull("gcs", gcs);
      String covName = "Pressure_surface";
      Coverage coverage = gcs.findCoverage(covName);
      Assert.assertNotNull(covName, coverage);

      CoverageCoordSys cs = coverage.getCoordSys();
      Assert.assertNotNull("coordSys", cs);
      System.out.printf(" org coverage shape=%s%n", Misc.showInts(cs.getShape()));

      HorizCoordSys hcs = cs.getHorizCoordSys();
      Assert.assertNotNull("HorizCoordSys", hcs);

      LatLonRect bbox = new LatLonRect(new LatLonPointImpl(10.0, 40.0), 50.0, 120.0);
      writeTestFile(gcs, coverage, bbox, new int[]{1, 51, 121});
    }
  }

  @Test
  @Category(NeedsCdmUnitTest.class)
  public void testCrossLongitudeSeamDt() throws Exception {
    String filename = TestDir.cdmUnitTestDir + "ft/grid/GFS_Global_onedeg_20081229_1800.grib2.nc";
    System.out.printf("open %s%n", filename);

    try (FeatureDatasetCoverage cc = CoverageDatasetFactory.open(filename)) {
      Assert.assertNotNull(filename, cc);
      CoverageCollection gcs = cc.findCoverageDataset(FeatureType.GRID);
      Assert.assertNotNull("gcs", gcs);
      String covName = "Pressure_surface";
      Coverage coverage = gcs.findCoverage(covName);
      Assert.assertNotNull(covName, coverage);

      CoverageCoordSys cs = coverage.getCoordSys();
      Assert.assertNotNull("coordSys", cs);
      System.out.printf(" org coverage shape=%s%n", Misc.showInts(cs.getShape()));

      HorizCoordSys hcs = cs.getHorizCoordSys();
      Assert.assertNotNull("HorizCoordSys", hcs);

      LatLonRect bbox = new LatLonRect(new LatLonPointImpl(40.0, -100.0), 10.0, 120.0);
      writeTestFile(gcs, coverage, bbox, new int[]{1, 11, 121});
    }
  }


  @Test
  @Category(NeedsCdmUnitTest.class)
  public void testCrossLongitudeSeamGrib() throws Exception {
    String filename = TestDir.cdmUnitTestDir + "tds/ncep/GFS_Global_0p5deg_20100913_0000.grib2";
    System.out.printf("open %s%n", filename);

    try (FeatureDatasetCoverage cc = CoverageDatasetFactory.open(filename)) {
      Assert.assertNotNull(filename, cc);
      CoverageCollection gcs = cc.findCoverageDataset(FeatureType.GRID);
      Assert.assertNotNull("gcs", gcs);
      String gribId = "VAR_2-0-0_L1";
      Coverage coverage = gcs.findCoverageByAttribute(Grib.VARIABLE_ID_ATTNAME, gribId); // Land_cover_0__sea_1__land_surface
      Assert.assertNotNull(gribId, coverage);

      CoverageCoordSys cs = coverage.getCoordSys();
      Assert.assertNotNull("coordSys", cs);
      System.out.printf(" org coverage shape=%s%n", Misc.showInts(cs.getShape()));

      HorizCoordSys hcs = cs.getHorizCoordSys();
      Assert.assertNotNull("HorizCoordSys", hcs);
      Assert.assertEquals("rank", 3, cs.getShape().length);

      LatLonRect bbox = new LatLonRect(new LatLonPointImpl(40.0, -100.0), 10.0, 120.0);
      writeTestFile(gcs, coverage, bbox, new int[]{1, 21, 241});
    }
  }

  public void writeTestFile(CoverageCollection coverageDataset, Coverage coverage, LatLonRect bbox, int[] expectedShape) throws IOException, InvalidRangeException {
    String covName = coverage.getName();
    File tempFile = TestDir.getTempFile();
    System.out.printf(" write %s to %s%n", covName, tempFile.getAbsolutePath());

    SubsetParams params = new SubsetParams().set(SubsetParams.latlonBB, bbox).set(SubsetParams.timePresent, true);
    System.out.printf("params=%s%n", params);

    NetcdfFileWriter writer = NetcdfFileWriter.createNew(NetcdfFileWriter.Version.netcdf3, tempFile.getPath(), null);

    Optional<Long> estimatedSizeo = CFGridCoverageWriter2.writeOrTestSize(coverageDataset, Lists.newArrayList(covName), params, false, false, writer);
    if (!estimatedSizeo.isPresent())
      throw new InvalidRangeException("Request contains no data: " + estimatedSizeo.getErrorMessage());

    // open the new file as a Coverage
    try (FeatureDatasetCoverage cc = CoverageDatasetFactory.open(tempFile.getPath())) {
      Assert.assertNotNull(tempFile.getPath(), cc);
      Assert.assertEquals(1, cc.getCoverageCollections().size());
      CoverageCollection cd2 = cc.getCoverageCollections().get(0);

      Coverage coverage2 = cd2.findCoverage(covName);
      Assert.assertNotNull(covName, coverage2);

      CoverageCoordSys gcs2 = coverage2.getCoordSys();
      System.out.printf(" data cs shape=%s%n", Misc.showInts(gcs2.getShape()));
      System.out.printf(" expected shape=%s%n", Misc.showInts(expectedShape));
      Assert.assertArrayEquals("expected data shape", expectedShape, gcs2.getShape());
    }

    // open the new file as a Grid
    try (GridDataset gds = GridDataset.open(tempFile.getPath())) {
      Assert.assertNotNull(tempFile.getPath(), gds);
      Assert.assertNotNull(covName, gds.findGridByName(covName));
    }

    // open the file as old style Grid
    try (NetcdfDataset nf = NetcdfDataset.openDataset(tempFile.getPath())) {
      ucar.nc2.dt.grid.GridDataset dtDataset = new ucar.nc2.dt.grid.GridDataset(nf);
      Assert.assertNotNull(covName, dtDataset.findGridByName(covName));
    }
  }
}
