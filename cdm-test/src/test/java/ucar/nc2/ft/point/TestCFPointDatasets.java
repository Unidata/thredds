/*
 * Copyright 1998-2014 University Corporation for Atmospheric Research/Unidata
 *
 *   Portions of this software were developed by the Unidata Program at the
 *   University Corporation for Atmospheric Research.
 *
 *   Access and use of this software shall impose the following obligations
 *   and understandings on the user. The user is granted the right, without
 *   any fee or cost, to use, copy, modify, alter, enhance and distribute
 *   this software, and any derivative works thereof, and its supporting
 *   documentation for any purpose whatsoever, provided that this entire
 *   notice appears in all copies of the software, derivative works and
 *   supporting documentation.  Further, UCAR requests that the user credit
 *   UCAR/Unidata in any publications that result from the use of this
 *   software or in any product that includes this software. The names UCAR
 *   and/or Unidata, however, may not be used in any advertising or publicity
 *   to endorse or promote any products or commercial entity unless specific
 *   written permission is obtained from UCAR/Unidata. The user also
 *   understands that UCAR/Unidata is not obligated to provide the user with
 *   any support, consulting, training or assistance of any kind with regard
 *   to the use, operation and performance of this software nor to provide
 *   the user with any updates, revisions, new versions or "bug fixes."
 *
 *   THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 *   IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *   WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *   DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 *   INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 *   FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 *   NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 *   WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */

package ucar.nc2.ft.point;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ucar.nc2.FileWriter2;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFileWriter;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.unidata.util.test.TestDir;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Synthetic (Ncml) datasets for testing point feature variants
 *
 * @author caron
 * @since 6/27/2014
 */
@RunWith(Parameterized.class)
public class TestCFPointDatasets {
  static public String CFpointObs_topdir = TestDir.cdmLocalTestDataDir + "point/";

  public static List<Object[]> getPointDatasets() {
    List<Object[]> result = new ArrayList<>();

    result.add(new Object[]{CFpointObs_topdir + "point.ncml", FeatureType.POINT, 3});
    result.add(new Object[]{CFpointObs_topdir + "pointUnlimited.nc", FeatureType.POINT, 3});
    result.add(new Object[]{CFpointObs_topdir + "pointMissing.ncml", FeatureType.POINT, 4});
    return result;
  }

  public static List<Object[]> getStationDatasets() {
    List<Object[]> result = new ArrayList<>();

    result.add(new Object[]{CFpointObs_topdir + "stationSingle.ncml", FeatureType.STATION, 3});
    result.add(new Object[]{CFpointObs_topdir + "stationSingleWithZlevel.ncml", FeatureType.STATION, 3});
    result.add(new Object[]{CFpointObs_topdir + "stationMultidim.ncml", FeatureType.STATION, 15});
    result.add(new Object[]{CFpointObs_topdir + "stationMultidimTimeJoin.ncml", FeatureType.STATION, 15});
    result.add(new Object[]{CFpointObs_topdir + "stationMultidimUnlimited.nc", FeatureType.STATION, 15});     // */
    result.add(new Object[]{CFpointObs_topdir + "stationMultidimUnlimited.ncml", FeatureType.STATION, 15});     // */
    result.add(new Object[]{CFpointObs_topdir + "stationMultidimMissingTime.ncml", FeatureType.STATION, 12});
    result.add(new Object[]{CFpointObs_topdir + "stationMultidimMissingId.ncml", FeatureType.STATION, 9});    // */
    result.add(new Object[]{CFpointObs_topdir + "stationMultidimMissingIdString.ncml", FeatureType.STATION, 12});
    result.add(new Object[]{CFpointObs_topdir + "stationRaggedContig.ncml", FeatureType.STATION, 6});
    result.add(new Object[]{CFpointObs_topdir + "stationRaggedIndex.ncml", FeatureType.STATION, 6});
    result.add(new Object[]{CFpointObs_topdir + "stationRaggedMissing.ncml", FeatureType.STATION, 5});
    result.add(new Object[]{CFpointObs_topdir + "stationData2Levels.ncml", FeatureType.STATION, 15});
    return result;
  }

  public static List<Object[]> getProfileDatasets() {
    List<Object[]> result = new ArrayList<>();

    result.add(new Object[]{CFpointObs_topdir + "profileSingle.ncml", FeatureType.PROFILE, 13});
    result.add(new Object[]{CFpointObs_topdir + "profileSingleTimePrecise.ncml", FeatureType.PROFILE, 12});
    result.add(new Object[]{CFpointObs_topdir + "profileMultidim.ncml", FeatureType.PROFILE, 50});
    result.add(new Object[]{CFpointObs_topdir + "profileMultidimTimePrecise.ncml", FeatureType.PROFILE, 50});
    result.add(new Object[]{CFpointObs_topdir + "profileMultidimZJoin.ncml", FeatureType.PROFILE, 50});
    result.add(new Object[]{CFpointObs_topdir + "profileMultidimZJoinTimePrecise.ncml", FeatureType.PROFILE, 50});
    result.add(new Object[]{CFpointObs_topdir + "profileMultidimMissingId.ncml", FeatureType.PROFILE, 40});
    result.add(new Object[]{CFpointObs_topdir + "profileMultidimMissingAlt.ncml", FeatureType.PROFILE, 14});
    result.add(new Object[]{CFpointObs_topdir + "profileRaggedContig.ncml", FeatureType.PROFILE, 6});
    result.add(new Object[]{CFpointObs_topdir + "profileRaggedContigTimePrecise.ncml", FeatureType.PROFILE, 6});
    result.add(new Object[]{CFpointObs_topdir + "profileRaggedIndex.ncml", FeatureType.PROFILE, 22});
    result.add(new Object[]{CFpointObs_topdir + "profileRaggedIndexTimePrecise.ncml", FeatureType.PROFILE, 22});
    result.add(new Object[]{CFpointObs_topdir + "profileData2Levels.ncml", FeatureType.PROFILE, 50});
    return result;
  }

  public static List<Object[]> getTrajectoryDatasets() {
    List<Object[]> result = new ArrayList<>();

    result.add(new Object[]{CFpointObs_topdir + "trajSingle.ncml", FeatureType.TRAJECTORY, 10});
    result.add(new Object[]{CFpointObs_topdir + "trajMultidim.ncml", FeatureType.TRAJECTORY, 20});
    result.add(new Object[]{CFpointObs_topdir + "trajMultidimJoinTime.ncml", FeatureType.TRAJECTORY, 20});
    result.add(new Object[]{CFpointObs_topdir + "trajMultidimMissingId.ncml", FeatureType.TRAJECTORY, 30});
    result.add(new Object[]{CFpointObs_topdir + "trajMultidimMissingTime.ncml", FeatureType.TRAJECTORY, 18});
    result.add(new Object[]{CFpointObs_topdir + "trajRaggedContig.ncml", FeatureType.TRAJECTORY, 6});
    result.add(new Object[]{CFpointObs_topdir + "trajRaggedIndex.ncml", FeatureType.TRAJECTORY, 6});
    result.add(new Object[]{CFpointObs_topdir + "trajRaggedMissing.ncml", FeatureType.TRAJECTORY, 5});
    return result;
  }

  public static List<Object[]> getStationProfileDatasets() {
    List<Object[]> result = new ArrayList<>();

    result.add(new Object[]{CFpointObs_topdir + "stationProfileSingle.ncml", FeatureType.STATION_PROFILE, 9});
    result.add(new Object[]{CFpointObs_topdir + "stationProfileSingleTimeJoin.ncml", FeatureType.STATION_PROFILE, 9});
    result.add(new Object[]{CFpointObs_topdir + "stationProfileMultidim.ncml", FeatureType.STATION_PROFILE, 18});
    result.add(new Object[]{CFpointObs_topdir + "stationProfileMultidimUnlimited.nc", FeatureType.STATION_PROFILE, 18});
    result.add(new Object[]{CFpointObs_topdir + "stationProfileMultidimJoinZ.ncml", FeatureType.STATION_PROFILE, 24});
    result.add(new Object[]{CFpointObs_topdir + "stationProfileMultidimJoinTime.ncml", FeatureType.STATION_PROFILE, 18});
    result.add(new Object[]{CFpointObs_topdir + "stationProfileMultidimJoinTimeAndZ.ncml", FeatureType.STATION_PROFILE, 36});
    result.add(new Object[]{CFpointObs_topdir + "stationProfileMultidimMissingId.ncml", FeatureType.STATION_PROFILE, 27});
    result.add(new Object[]{CFpointObs_topdir + "stationProfileMultidimMissingIdString.ncml", FeatureType.STATION_PROFILE, 27});
    result.add(new Object[]{CFpointObs_topdir + "stationProfileMultidimMissingTime.ncml", FeatureType.STATION_PROFILE, 16});
    result.add(new Object[]{CFpointObs_topdir + "stationProfileMultidimMissingAlt.ncml", FeatureType.STATION_PROFILE, 15});
    result.add(new Object[]{CFpointObs_topdir + "stationProfileRagged.ncml", FeatureType.STATION_PROFILE, 14});
    result.add(new Object[]{CFpointObs_topdir + "stationProfileRaggedJoinTime.ncml", FeatureType.STATION_PROFILE, 14});
    return result;
  }

  public static List<Object[]> getSectionDatasets() {
    List<Object[]> result = new ArrayList<>();

    result.add(new Object[]{CFpointObs_topdir + "sectionSingle.ncml", FeatureType.SECTION, 50});
    result.add(new Object[]{CFpointObs_topdir + "sectionMultidim.ncml", FeatureType.SECTION, 100});
    result.add(new Object[]{CFpointObs_topdir + "sectionMultidimJoinZ.ncml", FeatureType.SECTION, 100});
    result.add(new Object[]{CFpointObs_topdir + "sectionMultidimMissingId.ncml", FeatureType.SECTION, 100});
    result.add(new Object[]{CFpointObs_topdir + "sectionMultidimMissingIdString.ncml", FeatureType.SECTION, 100});
    result.add(new Object[]{CFpointObs_topdir + "sectionMultidimMissingTime.ncml", FeatureType.SECTION, 28});
    result.add(new Object[]{CFpointObs_topdir + "sectionMultidimMissingAlt.ncml", FeatureType.SECTION, 18});
    result.add(new Object[]{CFpointObs_topdir + "sectionRagged.ncml", FeatureType.SECTION, 12});
    return result;
  }

  @Parameterized.Parameters (name="{0}")
  public static List<Object[]> getTestParameters() {
    List<Object[]> result = new ArrayList<>();

    result.addAll(TestCFPointDatasets.getPointDatasets());
    result.addAll(TestCFPointDatasets.getStationDatasets());
    result.addAll(TestCFPointDatasets.getProfileDatasets());
    result.addAll(TestCFPointDatasets.getTrajectoryDatasets());
    result.addAll(TestCFPointDatasets.getStationProfileDatasets());
    result.addAll(TestCFPointDatasets.getSectionDatasets());

    return result;
  }

  String location;
  FeatureType ftype;
  int countExpected;
  boolean show = false;

  public TestCFPointDatasets(String location, FeatureType ftype, int countExpected) {
    this.location = location;
    this.ftype = ftype;
    this.countExpected = countExpected;
  }

  @Test
  public void checkPointDataset() throws IOException {
    // TestPointDatasets test = new TestPointDatasets(location, ftype, countExpected);
    assert countExpected == TestPointDatasets.checkPointDataset(location, ftype, show);
  }

    // Convert all NCML files in CFpointObs_topdir to NetCDF-3 files.
    public static void main(String[] args) throws IOException {
        File examplesDir = new File(CFpointObs_topdir);
        File convertedDir = new File(examplesDir, "converted");
        convertedDir.mkdirs();

        for (File inputFile : examplesDir.listFiles(new NcmlFilenameFilter())) {
            String inputFilePath = inputFile.getCanonicalPath();
            String outputFileName = inputFile.getName().substring(0, inputFile.getName().length() - 2);
            String outputFilePath = new File(convertedDir, outputFileName).getCanonicalPath();
            System.out.printf("Writing %s to %s.%n", inputFilePath, outputFilePath);

            try (NetcdfFile ncfileIn = NetcdfDataset.openFile(inputFilePath, null)) {
                NetcdfFileWriter.Version version = NetcdfFileWriter.Version.netcdf3;
                FileWriter2 writer = new FileWriter2(ncfileIn, outputFilePath, version, null);

                NetcdfFile ncfileOut = writer.write(null);
                if (ncfileOut != null) {
                    ncfileOut.close();
                }
            }
        }
    }

    private static class NcmlFilenameFilter implements FilenameFilter {
        @Override
        public boolean accept(File dir, String name) {
            return name.endsWith("ncml");
        }
    }
}
