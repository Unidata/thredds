package ucar.nc2.ft.point;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ucar.nc2.constants.FeatureType;
import ucar.unidata.test.util.TestDir;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Synthetic (Ncml) datasets for testing point feature variants
 *
 * @author cwardgar
 * @since 2014/07/08
 */
@RunWith(Parameterized.class)
public class TestCfDocDsgExamples {
    public static String cfDocDsgExamplesDir = TestDir.cdmLocalTestDataDir + "cfDocDsgExamples/";

    public static List<Object[]> getPointDatasets() {
        List<Object[]> result = new ArrayList<>();

        result.add(new Object[] { cfDocDsgExamplesDir + "H.1.1.ncml", FeatureType.POINT, 12 });

        return result;
    }


    public static List<Object[]> getStationDatasets() {
        List<Object[]> result = new ArrayList<>();

        result.add(new Object[] { cfDocDsgExamplesDir + "H.2.1.1.ncml", FeatureType.STATION, 50  });
        result.add(new Object[] { cfDocDsgExamplesDir + "H.2.2.1.ncml", FeatureType.STATION, 130 });
        result.add(new Object[] { cfDocDsgExamplesDir + "H.2.3.1.ncml", FeatureType.STATION, 5   });
        result.add(new Object[] { cfDocDsgExamplesDir + "H.2.3.2.ncml", FeatureType.STATION, 15  });
        result.add(new Object[] { cfDocDsgExamplesDir + "H.2.4.1.ncml", FeatureType.STATION, 100 });
        result.add(new Object[] { cfDocDsgExamplesDir + "H.2.5.1.ncml", FeatureType.STATION, 30  });

        return result;
    }

    @Parameterized.Parameters(name = "{0}")  // Name the tests after the location.
    public static List<Object[]> getTestParameters() {
        List<Object[]> result = new ArrayList<>();

        result.addAll(getPointDatasets());
        result.addAll(getStationDatasets());

        return result;
    }

    private String location;
    private FeatureType ftype;
    private int countExpected;
    private boolean show = false;

    public TestCfDocDsgExamples(String location, FeatureType ftype, int countExpected) {
        this.location = location;
        this.ftype = ftype;
        this.countExpected = countExpected;
    }

    @Test
    public void checkPointDataset() throws IOException {
        TestPointFeatureTypes test = new TestPointFeatureTypes("");
        assert countExpected == test.checkPointDataset(location, ftype, show);
    }
}
