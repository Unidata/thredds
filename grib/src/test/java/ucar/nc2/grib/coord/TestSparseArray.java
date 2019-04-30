package ucar.nc2.grib.coord;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.unidata.util.test.Assert2;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;

/**
 * Test SparseArray class.
 *
 * @author caron
 * @since 12/12/13
 */
public class TestSparseArray {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Test
  public void testInfo() {
    int[] sizes = new int[] {3, 10, 10};
    int[] track = new int[3*10*10];
    List<Short> list = new ArrayList<>();
    for (int i=0; i<3*10*10; i++) {
      track[i] = i % 11 == 0 ? 0 : 1;
      list.add((short) i);
    }

    SparseArray<Short> sa = new SparseArray<>(sizes, track, list, 0);

    Formatter info = new Formatter();
    sa.showInfo(info, null);
    logger.debug(info.toString());
  
    Assert2.assertNearlyEquals(sa.getDensity(), 0.906667f);
  }
}
