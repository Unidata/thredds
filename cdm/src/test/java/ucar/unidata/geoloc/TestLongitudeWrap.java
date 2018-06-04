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
public class TestLongitudeWrap {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Parameterized.Parameters(name = "{0}")
  public static List<Object[]> getTestParameters() {
    List<Object[]> result = new ArrayList<>();

    result.add(new Object[]{100, -100, 200});

    return result;
  }

  double lat1, lat2, expected;

  public TestLongitudeWrap(double lat1, double lat2, double expected) {
    this.lat1 = lat1;
    this.lat2 = lat2;
    this.expected = expected;
  }

  @Test
  public void doit() {
    double compute = lonDiff(lat1, lat2);
    logger.debug("({} - {}) = {}, expect {}", lat1, lat2, compute, expected);
    Assert2.assertNearlyEquals(expected, compute);
    Assert.assertTrue(Math.abs(compute) < 360.0);
  }

  /**
   * Starting from lon1, find
   * Find difference (lon1 - lon2) normalized so that maximum value is += 180.
   *
   * @param lon1 start
   * @param lon2 end
   * @return
   */
  static public double lonDiff(double lon1, double lon2) {
    return Math.IEEEremainder(lon1 - lon2, 720.0);
  }
}
