package ucar.nc2.grib.coord;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;

/**
 * Test CoordinateUniquify class.
 *
 * @author caron
 * @since 12/10/13
 */
public class TestCoordinateUniquify {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Test
  public void testCoordinateUnionizer() {

    List<CoordinateND> coordNDs = new ArrayList<>();
    for (int i = 5; i < 15; i += 2) {
      coordNDs.add(TestCoordinateND.makeCoordinateND(i));
    }

    CoordinateSharerBest unionizer = new CoordinateSharerBest();
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
