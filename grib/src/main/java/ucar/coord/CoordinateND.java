package ucar.coord;

import ucar.ma2.Section;
import ucar.nc2.util.Indent;

import java.util.*;

/**
 * N dimensional coordinates.
 *
 * @author caron
 * @since 11/27/13
 */
public class CoordinateND<T> {

  private List<CoordinateBuilder<T>> builders;
  private List<Coordinate> coordinates; // result is orthogonal coordinates
  private SparseArray<T> sa;            // indexes refer to coordinates

  public CoordinateND() {
    builders = new ArrayList<>();
  }

  public void addBuilder(CoordinateBuilder<T> builder) {
    builders.add(builder);
  }

  public void addRecord( T gr) {
    for (CoordinateBuilder<T> builder : builders)
      builder.addRecord(gr);
  }

  public void finish(List<T> records, Formatter info) {
    coordinates = new ArrayList<>();
    for (CoordinateBuilder builder : builders) {
      Coordinate coord = builder.finish();
     // if (coord.getType() == Coordinate.Type.time2D)
     //   coordinates.add(((CoordinateTime2D) coord).getRuntimeCoordinate());
      coordinates.add(coord);
    }

    buildSparseArray(records, info);
  }

  public void buildSparseArray(List<T> records, Formatter info) {
    int[] sizeArray = new int[coordinates.size()];
    for (int i = 0; i < coordinates.size(); i++)
      sizeArray[i] = coordinates.get(i).getSize();
    sa = new SparseArray<>(sizeArray);

    int[] index = new int[coordinates.size()];
    for (T gr : records) {
      int count = 0;
      for (CoordinateBuilder<T> builder : builders) {
        index[count++] = builder.getIndex(gr);
      }

      sa.add(gr, info, index);
    }
  }


  /////////////////////////////////////////////////////////////////////////
  // apres finis

  public List<Coordinate> getCoordinates() {
    return coordinates;
  }

  public int getNCoordinates() {
    return coordinates.size();
  }

  public SparseArray<T> getSparseArray() {
    return sa;
  }

  public void showInfo(List<T> records, Formatter info) {
    if (sa == null) buildSparseArray(records, info);

    for (Coordinate coord : coordinates)
      coord.showInfo(info, new Indent(2));
    info.format("%n%n");
    sa.showInfo(info, null);
    info.format("%n");
  }

  public void showInfo(Formatter info, Counter all) {
    for (Coordinate coord : coordinates)
       coord.showInfo(info, new Indent(2));

    if (sa != null) sa.showInfo(info, all);
  }

  ////////////////////

  public CoordinateND( List<Coordinate> coordinates, SparseArray<T> sa) {
    this.coordinates = coordinates;
    this.sa = sa;
  }

  public CoordinateND( List<Coordinate> coordinates) {
    this.coordinates = coordinates;
    // make the new sparse array object
    int[] sizeArray = new int[coordinates.size()];
    for (int i = 0; i < coordinates.size(); i++)
      sizeArray[i] = coordinates.get(i).getSize();
    sa = new SparseArray<>(sizeArray);
  }

  /**
   * Reindex the sparse array, based on the new Coordinates.
   * Do this by running all the Records through the Coordinates, assigning each to a possible new spot in the sparse array.
   * @param prev must have same list of Coordinates, with possibly additional values.
   */
  public void reindex(CoordinateND<T> prev) {

    sa.setContent( prev.getSparseArray().getContent());   // content (list of records) is the same

    // for each coordinate, calculate the map of oldIndex -> newIndex
    List<Coordinate> prevCoords = prev.getCoordinates();
    List<IndexMap> indexMaps = new ArrayList<>();
    int count = 0;
    for (Coordinate curr : coordinates)
      indexMaps.add(new IndexMap(curr, prevCoords.get(count++)));

    int[] currIndex = new int[coordinates.size()];
    int[] prevIndex = new int[coordinates.size()];
    int[] track = new int[sa.getTotalSize()];

    // iterate through the contents of the prev track array
    SparseArray prevSa = prev.getSparseArray();
    Section section = new Section(prevSa.getShape());
    Section.Iterator iter = section.getIterator(prevSa.getShape());
    while (iter.hasNext()) {
      int oldTrackIdx = iter.next(prevIndex); // return both the index (1D) and index[n]
      int oldTrackValue = prevSa.getTrack(oldTrackIdx);
      if (oldTrackValue == 0) continue; // skip missing values

      // calculate position in the current track array, and store the value there
      int coordIdx = 0;
      for (IndexMap indexMap : indexMaps) {
        currIndex[coordIdx] = indexMap.map(prevIndex[coordIdx]);
        coordIdx++;
      }
      int trackIdx = sa.calcIndex(currIndex);
      track[trackIdx] = oldTrackValue;
    }
    sa.setTrack(track);
  }

  private class IndexMap {
    boolean identity = true;
    int[] indexMap;

    IndexMap(Coordinate curr, Coordinate prev) {
      identity = curr.equals(prev);
      if (identity) return;

      assert curr.getType() == prev.getType();

      int count = 0;
      Map<Object, Integer> currValMap = new HashMap<>();
      for (Object val : curr.getValues()) currValMap.put(val, count++);

      count = 0;
      indexMap = new int[prev.getSize()];
      for (Object val : prev.getValues())
        indexMap[count++] = currValMap.get(val); // where does this value fit in the curr coordinates?
    }

    int map(int oldIndex) {
      if (identity) return oldIndex;
      return indexMap[oldIndex];
    }

  }


}
