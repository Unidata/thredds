/* Copyright */
package ucar.nc2.dataset;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ucar.nc2.constants.FeatureType;
import ucar.unidata.test.util.TestDir;

import java.util.ArrayList;
import java.util.List;


@RunWith(Parameterized.class)
public class TestConventionFeatureTypes {
  static String base = TestDir.cdmUnitTestDir + "conventions/";

  @Parameterized.Parameters(name = "{0}")
  public static List<Object[]> getTestParameters() {
    List<Object[]> result = new ArrayList<>();

    result.add(new Object[]{"atd", FeatureType.GRID});
    result.add(new Object[]{"atd-radar", FeatureType.GRID});
    result.add(new Object[]{"avhrr", FeatureType.GRID});
    result.add(new Object[]{"awips", FeatureType.GRID});
    result.add(new Object[]{"cedric", FeatureType.GRID});
    result.add(new Object[]{"cf", FeatureType.GRID});
    result.add(new Object[]{"coards", FeatureType.GRID});
    result.add(new Object[]{"csm", FeatureType.GRID});
    result.add(new Object[]{"gdv", FeatureType.GRID});
    result.add(new Object[]{"gief", FeatureType.GRID});
    result.add(new Object[]{"ifps", FeatureType.GRID});
    result.add(new Object[]{"m3io", FeatureType.GRID});
    result.add(new Object[]{"mars", FeatureType.GRID});
    result.add(new Object[]{"mm5", FeatureType.GRID});
    result.add(new Object[]{"nuwg", FeatureType.GRID});
    result.add(new Object[]{"wrf", FeatureType.GRID});
    result.add(new Object[]{"zebra", FeatureType.GRID});

    return result;
  }

  FeatureType type;
  String dir;

  public TestConventionFeatureTypes(FeatureType type, String dir) {
    this.type = type;
    this.dir = dir;
  }

}

