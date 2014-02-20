package ucar.coord;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;

/**
 * Describe
 *
 * @author caron
 * @since 12/10/13
 */
public class TestCoordinateUnionizer {

  @Test
  public void testCoordinateUnionizer() {

    List<CoordinateND> coordNDs = new ArrayList<>();
    for (int i = 5; i < 15; i += 2) {
      coordNDs.add(TestCoordinateND.makeCoordinateND(i));
    }

    CoordinateUniquify unionizer = new CoordinateUniquify();
    for (CoordinateND coordND : coordNDs) {
      unionizer.addCoords(coordND.getCoordinates());
    }
    unionizer.finish();
    List<Coordinate> shared = unionizer.getUnionCoords();

    for (CoordinateND coordND : coordNDs) {
      Formatter f = new Formatter();
      f.format("Original%n");
      coordND.showInfo(f, null);

      List<Integer> coordIndex = new ArrayList<>();
      CoordinateND coordNDr = unionizer.reindex(coordND, coordIndex);
      Counter counter = new Counter();
      f.format("Unionized%n");
      coordNDr.showInfo(f, counter);
      System.out.printf("%s%n", f);
      System.out.printf("%s%n====================%n", counter.show());
    }

  }
}
