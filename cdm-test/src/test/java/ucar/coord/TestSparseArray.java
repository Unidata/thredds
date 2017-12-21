package ucar.coord;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.nc2.util.Misc;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;

/**
 * Describe
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

    Formatter info = new Formatter(System.out);
    sa.showInfo(info, null);

    assert Misc.closeEnough(sa.getDensity(), 0.906667);

  }
}
