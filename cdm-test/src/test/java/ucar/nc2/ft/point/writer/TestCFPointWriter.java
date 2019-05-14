package ucar.nc2.ft.point.writer;

import com.google.common.collect.Lists;
import org.junit.*;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.nc2.NetcdfFileWriter;
import ucar.nc2.VariableSimpleIF;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.ft.*;
import ucar.nc2.ft.point.TestCFPointDatasets;
import ucar.nc2.ft.point.TestPointDatasets;
import ucar.nc2.jni.netcdf.Nc4Iosp;
import ucar.nc2.util.CompareNetcdf2;
import ucar.unidata.util.test.TestDir;

import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;

/**
 * Test CFPointWriter, write into nc, nc4 and nc4c (classic) files
 * C:/dev/github/thredds/cdm/target/test/tmp/stationRaggedContig.ncml.nc4
 *
 * @author caron
 * @since 4/11/12
 */
@RunWith(Parameterized.class)
public class TestCFPointWriter {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  @Rule public final TemporaryFolder tempFolder = new TemporaryFolder();

  @Parameterized.Parameters(name="{0}")
  public static List<Object[]> getTestParameters() {
    List<Object[]> result = new ArrayList<>();

    result.addAll(TestCFPointDatasets.getPointDatasets());
    result.addAll(TestCFPointDatasets.getStationDatasets());
    result.addAll(TestCFPointDatasets.getProfileDatasets());
    result.addAll(TestCFPointDatasets.getTrajectoryDatasets());
    result.addAll(TestCFPointDatasets.getStationProfileDatasets());  //  */
    result.addAll(TestCFPointDatasets.getSectionDatasets());  // */

    return result;
  }

  String location;
  FeatureType ftype;
  int countExpected;
  boolean show = false;

  public TestCFPointWriter(String location, FeatureType ftype, int countExpected) {
    this.location = location;
    this.ftype = ftype;
    this.countExpected = countExpected;
  }

  // @Test
  public void testWrite3col() throws IOException {
    CFPointWriterConfig config = new CFPointWriterConfig(NetcdfFileWriter.Version.netcdf3);
    config.recDimensionLength = countExpected;

    int count = writeDataset(location, ftype, config, show, tempFolder.newFile());   // column oriented
    System.out.printf("%s netcdf3 count=%d%n", location, count);
    assert count == countExpected : "count ="+count+" expected "+countExpected;
  }

  @Test
  public void testWrite3() throws IOException {
    int count = writeDataset(
            location, ftype, new CFPointWriterConfig(NetcdfFileWriter.Version.netcdf3), show, tempFolder.newFile());
    System.out.printf("%s netcdf3 count=%d%n", location, count);
    assert count == countExpected : "count ="+count+" expected "+countExpected;
  }

  @Test
  public void testWrite4classic() throws IOException {
    // Ignore this test if NetCDF-4 isn't present.
    if (!Nc4Iosp.isClibraryPresent()) {
      return;
    }

    int count = writeDataset(location, ftype,
            new CFPointWriterConfig(NetcdfFileWriter.Version.netcdf4_classic), show, tempFolder.newFile());
    System.out.printf("%s netcdf4_classic count=%d%n", location, count);
    assert count == countExpected : "count ="+count+" expected "+countExpected;
  }

  //@Test
  @Ignore("doesnt work: coordinate axes that are members of nc4 structures")
  public void testWrite4() throws IOException {
    // Ignore this test if NetCDF-4 isn't present.
    if (!Nc4Iosp.isClibraryPresent()) {
      return;
    }

    int count = writeDataset(
            location, ftype, new CFPointWriterConfig(NetcdfFileWriter.Version.netcdf4), show, tempFolder.newFile());
    System.out.printf("%s netcdf4 count=%d%n", location, count);
    assert count == countExpected : "count ="+count+" expected "+countExpected;
  }


  ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  static int writeDataset(String location, FeatureType ftype, CFPointWriterConfig config, boolean show,
          File fileOut) throws IOException {
    File fileIn = new File(location);
    long start = System.currentTimeMillis();

    System.out.printf("================ TestCFPointWriter%n read %s size=%d%n write to=%s%n",
            fileIn.getAbsoluteFile(), fileIn.length(), fileOut.getAbsoluteFile());

    // open point dataset
    Formatter out = new Formatter();
    try (FeatureDataset fdataset = FeatureDatasetFactoryManager.open(ftype, location, null, out)) {
      if (fdataset == null) {
        System.out.printf("**failed on %s %n --> %s %n", location, out);
        assert false;
      }

      assert fdataset instanceof FeatureDatasetPoint;
      FeatureDatasetPoint fdpoint = (FeatureDatasetPoint) fdataset;
      int count = CFPointWriter.writeFeatureCollection(fdpoint, fileOut.getPath(), config);
      long took = System.currentTimeMillis() - start;
      System.out.printf(" nrecords written = %d took=%d msecs%n%n", count, took);

      ////////////////////////////////
      // open result
      System.out.printf(" open result dataset=%s size = %d (%f ratio out/in) %n", fileOut.getPath(), fileOut.length(), ((double) fileOut.length() / fileIn.length()));
      out = new Formatter();

      try (FeatureDataset result = FeatureDatasetFactoryManager.open(ftype, fileOut.getPath(), null, out)) {
        if (result == null) {
          System.out.printf(" **failed --> %n%s <--END FAIL messages%n", out);
          FeatureDatasetFactoryManager.open(ftype, fileOut.getPath(), null, out);
          assert false;
        }
        if (show) {
          System.out.printf("----------- testPointDataset getDetailInfo -----------------%n");
          result.getDetailInfo(out);
          System.out.printf("%s %n", out);
        }

        // sanity checks
        compare(fdpoint, (FeatureDatasetPoint) result);
        Assert.assertTrue("npoints", 0 < TestPointDatasets.checkPointFeatureDataset(result, show));

      }
      return count;
    }
  }

  static final boolean failOnDataVarsDifferent = true;
  static void compare(FeatureDatasetPoint org, FeatureDatasetPoint copy) {

    FeatureType fcOrg = org.getFeatureType();
    FeatureType fcCopy = copy.getFeatureType();
    assert fcOrg == fcCopy;

    List<VariableSimpleIF> orgVars = org.getDataVariables();
    List<VariableSimpleIF> copyVars = copy.getDataVariables();
    Formatter f = new Formatter();
    boolean ok = CompareNetcdf2.compareLists(getNames(orgVars, Lists.newArrayList("profileId")), getNames(copyVars, Lists.newArrayList("profileId")), f);
    if (ok) System.out.printf("Data Vars OK%n");
    else {
      System.out.printf("Data Vars NOT OK%n %s%n", f);
      if (failOnDataVarsDifferent) assert false;
    }
  }

  static List<String> getNames(List<VariableSimpleIF> vars, List<String> skip) {
    List<String> result = new ArrayList<>();
    for (VariableSimpleIF v : vars) {
      if (!skip.contains(v.getShortName())) {
        result.add(v.getShortName());
        System.out.printf("  %s%n", v.getShortName());
      }
    }
    System.out.printf("%n");
    return result;
  }
}
