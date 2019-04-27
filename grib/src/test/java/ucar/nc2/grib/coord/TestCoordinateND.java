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
 * Test CoordinateND class.
 *
 * @author caron
 * @since 12/10/13
 */
public class TestCoordinateND {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Test
  public void testCoordinateND() {
    CoordinateND<Short> prev = makeCoordinateND(2, 10);
    GribRecordStats counter = new GribRecordStats();
    Formatter f = new Formatter();
    prev.showInfo(f, counter);
    logger.debug("{}", f);
    logger.debug("prev {}", counter.show());

    CoordinateND<Short> curr = makeCoordinateND(2, 11);
    counter = new GribRecordStats();
    f = new Formatter();
    curr.showInfo(f, counter);
    logger.debug("{}", f);
    logger.debug("curr {}", counter.show());

    CoordinateND<Short> reindexed = new CoordinateND.Builder<Short>().reindex(curr.getCoordinates(), prev);
    counter = new GribRecordStats();
    f = new Formatter();
    reindexed.showInfo(f, counter);
    logger.debug("{}", f);
    logger.debug("reindexed {}", counter.show());
  
    Assert2.assertNearlyEquals(curr.getSparseArray().getDensity(), 1.0f);
    Assert2.assertNearlyEquals(reindexed.getSparseArray().getDensity(), .826446f);
  }

  public static CoordinateND<Short> makeCoordinateND(int rank, int size) {
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

  public static CoordinateND<Short> makeCoordinateND(int size) {
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
