package ucar.nc2.ft.point.writer;

import org.junit.Test;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.ft.*;
import ucar.nc2.ft.point.StationPointFeature;
import ucar.nc2.time.CalendarDateRange;
import ucar.unidata.test.util.TestDir;

import java.io.File;
import java.io.IOException;
import java.util.Formatter;

/**
 * Test CFPointWriter
 *
 * @author caron
 * @since 4/11/12
 */
public class TestCFPointWriter {
  private String CFpointObs_topdir = TestDir.cdmLocalTestDataDir + "point/";

  @Test
  public void testWrite() throws IOException {
    writeDataset(TestDir.cdmUnitTestDir + "ft/point/netcdf/Surface_Buoy_20090921_0000.nc", FeatureType.POINT, true);

    writeDataset(TestDir.cdmUnitTestDir + "ft/station/200501q3h-gr.nc", FeatureType.STATION, true);

    writeDataset(CFpointObs_topdir + "profileSingle.ncml", FeatureType.PROFILE, true);
  }

  // synthetic variants
  // @Test
  public void testWriteProfileVariants() throws IOException {
    assert 13 ==  writeDataset(CFpointObs_topdir + "profileSingle.ncml", FeatureType.PROFILE, false);
    assert 12 ==  writeDataset(CFpointObs_topdir + "profileSingleTimeJoin.ncml", FeatureType.PROFILE, false);
    assert 50 ==  writeDataset(CFpointObs_topdir + "profileMultidim.ncml", FeatureType.PROFILE, false);
    assert 50 ==  writeDataset(CFpointObs_topdir + "profileMultidimTimeJoin.ncml", FeatureType.PROFILE, false);
    assert 50 ==  writeDataset(CFpointObs_topdir + "profileMultidimZJoin.ncml", FeatureType.PROFILE, false);
    assert 50 ==  writeDataset(CFpointObs_topdir + "profileMultidimTimeZJoin.ncml", FeatureType.PROFILE, false);
    assert 40 ==  writeDataset(CFpointObs_topdir + "profileMultidimMissingId.ncml", FeatureType.PROFILE, false);
    assert 14 == writeDataset(CFpointObs_topdir + "profileMultidimMissingAlt.ncml", FeatureType.PROFILE, false);
    assert 6 ==  writeDataset(CFpointObs_topdir + "profileRaggedContig.ncml", FeatureType.PROFILE, false);
    assert 6 ==  writeDataset(CFpointObs_topdir + "profileRaggedContigTimeJoin.ncml", FeatureType.PROFILE, false);
    assert 22 ==  writeDataset(CFpointObs_topdir + "profileRaggedIndex.ncml", FeatureType.PROFILE, false);
    assert 22 ==  writeDataset(CFpointObs_topdir + "profileRaggedIndexTimeJoin.ncml", FeatureType.PROFILE, false);
  }

  @Test
  public void testProblem() throws IOException {
    assert 13 ==  writeDataset(CFpointObs_topdir + "profileSingle.ncml", FeatureType.PROFILE, false);
  }


  int writeDataset(String location, FeatureType ftype, boolean show) throws IOException {
    File fileIn = new File(location);

    int pos = location.lastIndexOf("/");
    String name = location.substring(pos + 1);
    if (!name.endsWith(".nc")) name = name + ".nc";
    File fileOut = new File(TestDir.temporaryLocalDataDir, name);

    System.out.printf("================ TestCFPointWriter%n read %s size=%d%n write to=%s %n", fileIn.getAbsolutePath(), fileIn.length(), fileOut.getAbsolutePath());

    // open point dataset
    Formatter out = new Formatter();
    FeatureDataset fdataset = FeatureDatasetFactoryManager.open(ftype, location, null, out);
    if (fdataset == null) {
      System.out.printf("**failed on %s %n --> %s %n", location, out);
      assert false;
    }

    assert fdataset instanceof FeatureDatasetPoint;
    FeatureDatasetPoint fdpoint = (FeatureDatasetPoint) fdataset;
    int count = CFPointWriter.writeFeatureCollection(fdpoint, fileOut.getPath(), null);
    System.out.printf(" nrecords written = %d%n%n", count);

    ////////////////////////////////
    // open result

    System.out.printf(" open result dataset=%s size = %d (%f ratio out/in) %n", fileOut.getPath(), fileOut.length(), ((double) fileOut.length() / fileIn.length()));
    out = new Formatter();
    FeatureDataset result = FeatureDatasetFactoryManager.open(ftype, fileOut.getPath(), null, out);
    if (result == null) {
      System.out.printf(" **failed --> %n%s <--END FAIL messages%n", out);
      assert false;
    }
    System.out.printf("----------- testPointDataset getDetailInfo -----------------%n");
    if (show) {
      result.getDetailInfo(out);
      System.out.printf("%s %n", out);
    }

    return count;
  }

}
