package ucar.sparr;

import org.junit.Test;
import ucar.nc2.util.Misc;

import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;

/**
 * Describe
 *
 * @author caron
 * @since 12/10/13
 */
public class TestCoordinateND {

  @Test
  public void testCoordinateND() {
    CoordinateND prev = makeCoordinateND(2, 10);
    Counter counter = new Counter();
    Formatter f = new Formatter();
    prev.showInfo(f, counter);
    System.out.printf("%s%n", f);
    System.out.printf("%s%n====================%n", counter.show());

    CoordinateND curr = makeCoordinateND(2, 11);
    counter = new Counter();
    f = new Formatter();
    curr.showInfo(f, counter);
    System.out.printf("%s%n", f);
    System.out.printf("%s%n====================%n", counter.show());

    curr.reindex(prev);
    counter = new Counter();
    f = new Formatter();
    curr.showInfo(f, counter);
    System.out.printf("%s%n", f);
    System.out.printf("%s%n====================%n", counter.show());

    assert Misc.closeEnough(curr.getSparseArray().getDensity(), .826446);
  }

  static public CoordinateND makeCoordinateND(int rank, int size) {
    List<Coordinate> coords = new ArrayList<>();
    for (int i=0; i<rank; i++)
      coords.add(new TestCoordinate(size*(i+1)));

    int[] sizeArray = new int[coords.size()];
    for (int i = 0; i < coords.size(); i++)
      sizeArray[i] = coords.get(i).getSize();
    SparseArray<Short> sa = new SparseArray<>(sizeArray);
    CoordinateND<Short> prev = new CoordinateND<>(coords, sa);

    int n = sa.getTotalSize();
    int[] track = new int[n];
    for (int i=0; i<n; i++) track[i] = i+1;
    sa.setTrack(track);

    List<Short> content = new ArrayList<>(n);
    for (int i=0; i<n; i++) content.add((short) (i*10));
    sa.setContent(content);

    return prev;
  }

  static public CoordinateND makeCoordinateND(int size) {
    List<Coordinate> coords = new ArrayList<>();
    coords.add(TestCoordinate.factory(size, Coordinate.Type.runtime));
    coords.add(TestCoordinate.factory(size, Coordinate.Type.time));
    coords.add(TestCoordinate.factory(size, Coordinate.Type.vert));

    int[] sizeArray = new int[coords.size()];
    for (int i = 0; i < coords.size(); i++)
      sizeArray[i] = coords.get(i).getSize();
    SparseArray<Short> sa = new SparseArray<>(sizeArray);
    CoordinateND<Short> prev = new CoordinateND<>(coords, sa);

    int n = sa.getTotalSize();
    int[] track = new int[n];
    for (int i=0; i<n; i++) track[i] = i+1;
    sa.setTrack(track);

    List<Short> content = new ArrayList<>(n);
    for (int i=0; i<n; i++) content.add((short) (i*10));
    sa.setContent(content);

    return prev;
  }

}
