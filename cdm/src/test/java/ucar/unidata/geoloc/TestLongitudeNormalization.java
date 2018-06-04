/* Copyright Unidata */
package ucar.unidata.geoloc;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.unidata.util.test.Assert2;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;

/**
 * Describe
 *
 * @author caron
 * @since 9/11/2015.
 */
@RunWith(Parameterized.class)
public class TestLongitudeNormalization {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Parameterized.Parameters(name = "{0}")
  public static List<Object[]> getTestParameters() {
    List<Object[]> result = new ArrayList<>();

    result.add(new Object[]{100.0, -100.0, 0.0});
    result.add(new Object[]{-100.0, 100.0, 360.0});
    result.add(new Object[]{-100.0, -180.0, 0.0});
    result.add(new Object[]{-180.0, -100.0, 360.0});
    result.add(new Object[]{-180.0, 180.0, 360.0});
    result.add(new Object[]{181.0, -180.0, -360.0});
    result.add(new Object[]{181.0, -200.0, -360.0});
    result.add(new Object[]{-200.0, 200.0, 720.0});
    result.add(new Object[]{-179.0, 180.0, 360.0});

    return result;
  }

  double lon, from;
  Double expectedDiff;

  public TestLongitudeNormalization(double lon, double from, Double expectedDiff) {
    this.lon = lon;
    this.from = from;
    this.expectedDiff = expectedDiff;
  }

  @Test
  public void doit() {
    double compute = lonNormalFrom(lon, from);
    
    if (expectedDiff != null) {
      logger.debug("({} from {}) = {}, diff = {} expectedDiff {}", lon, from, compute, compute - lon, expectedDiff);
      Assert2.assertNearlyEquals(expectedDiff, compute - lon);
    } else {
      logger.debug("({} from {}) = {}, diff = {}", lon, from, compute, compute - lon);
    }
    
    String msg = String.format("(%f from %f) = %f%n", lon, from, compute);
    Assert.assertTrue(msg, compute >= from);
    Assert.assertTrue(msg, compute < from + 360);
  }

  /**
   * put longitude into the range [start, start+360] deg
   *
   * @param lon    lon to normalize
   * @param start starting point
   * @return longitude into the range [center +/- 180] deg
   */
  static public double lonNormalFrom(double lon, double start) {
    while (lon < start) lon += 360;
    while (lon > start + 360) lon -= 360;
    return lon;
  }
}
