package ucar.coord;

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
    CoordinateND<Short> prev = makeCoordinateND(2, 10);
    Counter counter = new Counter();
    Formatter f = new Formatter();
    prev.showInfo(f, counter);
    System.out.printf("%s%n", f);
    System.out.printf("prev %s%n====================%n", counter.show());

    CoordinateND<Short> curr = makeCoordinateND(2, 11);
    counter = new Counter();
    f = new Formatter();
    curr.showInfo(f, counter);
    System.out.printf("%s%n", f);
    System.out.printf("curr %s%n====================%n", counter.show());

    CoordinateND<Short> reindexed = new CoordinateND.Builder<Short>().reindex(curr.getCoordinates(), prev);
    counter = new Counter();
    f = new Formatter();
    reindexed.showInfo(f, counter);
    System.out.printf("%s%n", f);
    System.out.printf("reindexed %s%n====================%n", counter.show());

    assert Misc.closeEnough(curr.getSparseArray().getDensity(), 1.0);
    assert Misc.closeEnough(reindexed.getSparseArray().getDensity(), .826446);
  }

  static public CoordinateND<Short> makeCoordinateND(int rank, int size) {
    List<Coordinate> coords = new ArrayList<>();
    for (int i=0; i<rank; i++)
      coords.add(new TestCoordinate(size*(i+1)));

    int[] sizeArray = new int[coords.size()];
    for (int i = 0; i < coords.size(); i++)
      sizeArray[i] = coords.get(i).getSize();
    SparseArray.Builder<Short> builder = new SparseArray.Builder<>(sizeArray);

    int n = builder.getTotalSize();
    int[] track = new int[n];
    for (int i=0; i<n; i++) track[i] = i+1;
    builder.setTrack(track);

    List<Short> content = new ArrayList<>(n);
    for (int i=0; i<n; i++) content.add((short) (i*10));
    builder.setContent(content);

    SparseArray<Short> sa = builder.finish();
    return new CoordinateND<>(coords, sa);
  }

  static public CoordinateND<Short> makeCoordinateND(int size) {
    List<Coordinate> coords = new ArrayList<>();
    coords.add(TestCoordinate.factory(size, Coordinate.Type.runtime));
    coords.add(TestCoordinate.factory(size, Coordinate.Type.time));
    coords.add(TestCoordinate.factory(size, Coordinate.Type.vert));

    int[] sizeArray = new int[coords.size()];
    for (int i = 0; i < coords.size(); i++)
      sizeArray[i] = coords.get(i).getSize();
    SparseArray.Builder<Short> builder = new SparseArray.Builder<>(sizeArray);
 //   CoordinateND<Short> prev = new CoordinateND<>(coords, sa);

    int n = builder.getTotalSize();
    int[] track = new int[n];
    for (int i=0; i<n; i++) track[i] = i+1;
    builder.setTrack(track);

    List<Short> content = new ArrayList<>(n);
    for (int i=0; i<n; i++) content.add((short) (i*10));
    builder.setContent(content);

    SparseArray<Short> sa = builder.finish();
    return new CoordinateND<>(coords, sa);
  }

}
