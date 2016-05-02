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
 * Synthetic (Ncml) datasets for testing point feature variants.
 *
 * @author cwardgar
 * @since 2014/07/08
 */
@RunWith(Parameterized.class)
public class TestCfDocDsgExamples {
    public static String cfDocDsgExamplesDir = TestDir.cdmLocalTestDataDir + "cfDocDsgExamples/";

    public static List<Object[]> getPointDatasets() {
        List<Object[]> result = new ArrayList<>();

        result.add(new Object[] { "H.1.1.ncml", FeatureType.POINT, 12 });

        return result;
    }

    public static List<Object[]> getStationDatasets() {
        List<Object[]> result = new ArrayList<>();

        result.add(new Object[] { "H.2.1.1.ncml", FeatureType.STATION, 50  });
        result.add(new Object[] { "H.2.2.1.ncml", FeatureType.STATION, 130 });
        result.add(new Object[] { "H.2.3.1.ncml", FeatureType.STATION, 5   });
        // result.add(new Object[] { "H.2.3.2.ncml", FeatureType.STATION, 15  }); LOOK failing
        result.add(new Object[] { "H.2.4.1.ncml", FeatureType.STATION, 100 });
        result.add(new Object[] { "H.2.5.1.ncml", FeatureType.STATION, 30  });

        return result;
    }

    public static List<Object[]> getProfileDatasets() {
        List<Object[]> result = new ArrayList<>();

        result.add(new Object[] { "H.3.1.1.ncml", FeatureType.PROFILE, 56 });
        result.add(new Object[] { "H.3.3.1.ncml", FeatureType.PROFILE, 42 });
        result.add(new Object[] { "H.3.4.1.ncml", FeatureType.PROFILE, 37 });
        result.add(new Object[] { "H.3.5.1.ncml", FeatureType.PROFILE, 37 });

        return result;
    }

    public static List<Object[]> getTrajectoryDatasets() {
        List<Object[]> result = new ArrayList<>();

        result.add(new Object[] { "H.4.1.1.ncml", FeatureType.TRAJECTORY, 24 });
        result.add(new Object[] { "H.4.2.1.ncml", FeatureType.TRAJECTORY, 42 });
        result.add(new Object[] { "H.4.3.1.ncml", FeatureType.TRAJECTORY, 75 });
        result.add(new Object[] { "H.4.4.1.ncml", FeatureType.TRAJECTORY, 75 });

        return result;
    }

    public static List<Object[]> getStationProfileDatasets() {
        List<Object[]> result = new ArrayList<>();

        result.add(new Object[] { "H.5.1.1.ncml", FeatureType.STATION_PROFILE, 120 });
        result.add(new Object[] { "H.5.1.2.ncml", FeatureType.STATION_PROFILE, 60  });
        result.add(new Object[] { "H.5.2.1.ncml", FeatureType.STATION_PROFILE, 20  });
        result.add(new Object[] { "H.5.3.1.ncml", FeatureType.STATION_PROFILE, 145 });

        return result;
    }

    public static List<Object[]> getSectionDatasets() {
        List<Object[]> result = new ArrayList<>();

        result.add(new Object[] { "H.6.1.1.ncml", FeatureType.SECTION, 120 });
        result.add(new Object[] { "H.6.2.1.ncml", FeatureType.SECTION, 20  });
        result.add(new Object[] { "H.6.3.1.ncml", FeatureType.SECTION, 145 });

        return result;
    }

    @Parameterized.Parameters(name = "{0}")  // Name the tests after the location.
    public static List<Object[]> getTestParameters() {
        List<Object[]> result = new ArrayList<>();

        result.addAll(getPointDatasets());
        result.addAll(getStationDatasets());
        result.addAll(getProfileDatasets());
        result.addAll(getTrajectoryDatasets());
        result.addAll(getStationProfileDatasets());
        result.addAll(getSectionDatasets());

        return result;
    }

    private String location;
    private FeatureType ftype;
    private int countExpected;
    private boolean show = true;

    public TestCfDocDsgExamples(String location, FeatureType ftype, int countExpected) {
        this.location = cfDocDsgExamplesDir + location;
        this.ftype = ftype;
        this.countExpected = countExpected;
    }

    @Test
    public void checkPointDataset() throws IOException {
        assert countExpected == TestPointDatasets.checkPointDataset(location, ftype, show);
    }

    // Convert all NCML files in cfDocDsgExamplesDir to NetCDF-3 files.
    public static void main(String[] args) throws IOException {
        File examplesDir = new File(cfDocDsgExamplesDir);
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
