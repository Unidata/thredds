package ucar.coord;

import org.junit.Test;

import java.util.Formatter;

/**
 * Describe
 *
 * @author caron
 * @since 12/12/13
 */
public class TestSparseArray {

  @Test
  public void testInfo() {
    int[] sizes = new int[] {3, 10, 10};
    int[] track = new int[3*10*10];
    for (int i=0; i<3*10*10; i++) {
      track[i] = i % 11 == 0 ? 0 : 1;
    }

    SparseArray sa = new SparseArray(sizes, track, null);

    Formatter info = new Formatter(System.out);
    sa.showInfo(info, null);
  }
}
