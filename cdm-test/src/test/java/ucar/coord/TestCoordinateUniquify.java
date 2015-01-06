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
public class TestCoordinateUniquify {

  @Test
  public void testCoordinateUnionizer() {

    List<CoordinateND> coordNDs = new ArrayList<>();
    for (int i = 5; i < 15; i += 2) {
      coordNDs.add(TestCoordinateND.makeCoordinateND(i));
    }

    CoordinateUniquify unionizer = new CoordinateUniquify();
    for (CoordinateND coordND : coordNDs) {
      unionizer.addCoordinates(coordND.getCoordinates());
    }
    List<Coordinate> shared =  unionizer.finish();

    Formatter f = new Formatter();
    f.format("Original%n");
    for (CoordinateND coordND : coordNDs) {
      coordND.showInfo(f, null);
    }

    f.format("Uniqueified%n");
    for (Coordinate coord : shared) {
      coord.showInfo(f, null);
    }
  }
}
