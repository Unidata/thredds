/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.grib.coord;

import ucar.ma2.Section;
import ucar.nc2.util.Indent;

import javax.annotation.concurrent.Immutable;
import java.util.*;

/**
 * N-dimensional coordinates and a SparseArray that tracks if data is available.
 *
 * @author caron
 * @since 11/27/13
 */
@Immutable
public class CoordinateND<T> {

  private final List<Coordinate> coordinates; // result is orthogonal coordinates
  private final SparseArray<T> sa;            // indexes refer to coordinates

  CoordinateND(List<Coordinate> coordinates, SparseArray<T> sa) {
    assert coordinates.size() == sa.getRank();
    this.coordinates = Collections.unmodifiableList(coordinates);
    this.sa = sa;
  }

  public List<Coordinate> getCoordinates() {
    return coordinates;
  }

  private int getNCoordinates() {
    return coordinates.size();
  }

  public SparseArray<T> getSparseArray() {
    return sa;
  }

  public void showInfo(Formatter info, GribRecordStats all) {
    for (Coordinate coord : coordinates)
       coord.showInfo(info, new Indent(2));

    sa.showInfo(info, all);
  }

  @Override
  public String toString() {
    try (Formatter f = new Formatter()) {
      f.format("CoordinateND[");
      coordinates.forEach(c -> f.format("%s,", c.getName()));
      f.format("]");
      return f.toString();
    }
  }

  ////////////////////

  public static class Builder<T> {
    private List<CoordinateBuilder<T>> builders;
    private List<Coordinate> coordb = new ArrayList<>();

    public Builder() {
      builders = new ArrayList<>();
    }

    public void addBuilder(CoordinateBuilder<T> builder) {
      builders.add(builder);
    }

    public void addRecord(T gr) {
      for (CoordinateBuilder<T> builder : builders)
        builder.addRecord(gr);
    }

    public CoordinateND<T> finish(List<T> records, Formatter info) {
      for (CoordinateBuilder builder : builders) {
        Coordinate coord = builder.finish();
        if (coord.getType() == Coordinate.Type.time2D)
          coordb.add(((CoordinateTime2D) coord).getRuntimeCoordinate());
        coordb.add(coord);
      }

      SparseArray<T> sa = buildSparseArray(records, info);
      return new CoordinateND<>(coordb, sa);
    }

    SparseArray<T> buildSparseArray(List<T> records, Formatter info) {
      int[] sizeArray = new int[coordb.size()];
      for (int i = 0; i < coordb.size(); i++) {
        Coordinate coord = coordb.get(i);
        if (coord.getType() == Coordinate.Type.time2D)
          sizeArray[i] = ((CoordinateTime2D) coord).getNtimes();
        else
          sizeArray[i] = coord.getSize();
      }
      SparseArray.Builder<T> saBuilder = new SparseArray.Builder<>(sizeArray);

      int[] index = new int[coordb.size()];
      for (T gr : records) {
        int count = 0;
        for (CoordinateBuilder<T> builder : builders) {
          if (builder instanceof CoordinateBuilder.TwoD) {
            CoordinateBuilder.TwoD<T> builder2D  = (CoordinateBuilder.TwoD) builder;
            int[] coordsIdx = builder2D.getCoordIndices(gr);
            index[count++] = coordsIdx[0];
            index[count++] = coordsIdx[1];

          } else {
            index[count++] = builder.getIndex(gr);
          }
        }

        saBuilder.add(gr, info, index);
      }

      return saBuilder.finish();
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////////
    /**
     * Reindex the sparse array, based on the new Coordinates.
     * Do this by running all the Records through the Coordinates, assigning each to a new spot in the new sparse array.
     *
     * @param newCoords must have same list of Coordinates as prev, with possibly additional values.
     */
    CoordinateND<T> reindex(List<Coordinate> newCoords, CoordinateND<T> prev) {
      assert  newCoords.size() == prev.getNCoordinates();

      boolean has2Dcoord = false;
      for (Coordinate coord : newCoords) {
        if (coord.getType() == Coordinate.Type.time2D)
          has2Dcoord = true;
      }

      return (has2Dcoord) ? reindex2D(newCoords, prev) : reindexOrth(newCoords, prev);
    }

    private CoordinateND<T> reindexOrth(List<Coordinate> newCoords, CoordinateND<T> prev) {
      SparseArray<T> prevSA = prev.getSparseArray();
      List<Coordinate> prevCoords = prev.getCoordinates();

      // make a working sparse array with new shape
      int[] sizeArray = new int[newCoords.size()];
      for (int i = 0; i < newCoords.size(); i++) {
        Coordinate coord = newCoords.get(i);
        sizeArray[i] = (coord instanceof CoordinateTime2D) ? ((CoordinateTime2D) coord).getNtimes() : coord.getSize();
      }
      SparseArray.Builder<T> workingSAbuilder = new SparseArray.Builder<>(sizeArray);

      // for each coordinate, calculate the map of oldIndex -> newIndex
      List<IndexMap> indexMaps = new ArrayList<>();
      int count = 0;
      for (Coordinate coord : newCoords) {
        indexMaps.add(new IndexMap(coord, prevCoords.get(count++)));
      }

      int[] currIndex = new int[newCoords.size()];
      int[] prevIndex = new int[newCoords.size()];
      int[] track = new int[SparseArray.calcTotalSize(sizeArray)];

      // iterate through the contents of the prev track array
      Section section = new Section(prevSA.getShape());
      Section.Iterator iter = section.getIterator(prevSA.getShape());
      while (iter.hasNext()) {
        int oldTrackIdx = iter.next(prevIndex); // gets both the oldTrackIdx (1D) and prevIndex (nD)
        int oldTrackValue = prevSA.getTrack(oldTrackIdx);
        if (oldTrackValue == 0) continue; // skip missing values

        // calculate position in the current track array, and store the value there
        int coordIdx = 0;
        for (IndexMap indexMap : indexMaps) {
          currIndex[coordIdx] = indexMap.map(prevIndex[coordIdx]);
          coordIdx++;
        }
        int trackIdx = workingSAbuilder.calcIndex(currIndex);
        track[trackIdx] = oldTrackValue;
      }

      // now that we have the track, make the real SA
      SparseArray<T> newSA = new SparseArray<>(sizeArray, track, prevSA.getContent(), prevSA.getNdups());  // content (list of records) is the same
      return new CoordinateND<>(newCoords, newSA);                                      // reindexed result
    }

    // every coord in prev must be in curr
    private static class IndexMap {
      boolean identity;
      int[] indexMap;

      IndexMap(Coordinate curr, Coordinate prev) {
        identity = curr.equals(prev);
        if (identity) return;

        assert curr.getType() == prev.getType() : curr.getType()+" != "+prev.getType();

        int count = 0;
        Map<Object, Integer> currValMap = new HashMap<>();
        if (curr.getValues() == null)
          throw new IllegalStateException();
        for (Object val : curr.getValues()) currValMap.put(val, count++);

        count = 0;
        indexMap = new int[prev.getSize()];
        for (Object val : prev.getValues()) {
          indexMap[count++] = currValMap.get(val); // where does this value fit in the curr coordinates?
        }
      }

      public int map(int oldIndex) {
        if (identity) return oldIndex;
        return indexMap[oldIndex];
      }
    }

    //////////////////////////////////////////////

    private CoordinateND<T> reindex2D(List<Coordinate> newCoords, CoordinateND<T> prev) {
      SparseArray<T> prevSA = prev.getSparseArray();
      List<Coordinate> prevCoords = prev.getCoordinates();
      CoordinateTime2D prev2Dcoord = null;

      // make a working sparse array with new shape
      int ncoords = newCoords.size();
      int[] sizeArray = new int[ncoords];
      for (int i = 0; i < ncoords; i++) {
        Coordinate coord = newCoords.get(i);
        sizeArray[i] = (coord instanceof CoordinateTime2D) ? ((CoordinateTime2D) coord).getNtimes() : coord.getSize();
      }
      SparseArray.Builder<T> workingSAbuilder = new SparseArray.Builder<>(sizeArray);

      // for each coordinate, calculate the map of oldIndex -> newIndex
      IndexMap[] indexMaps = new IndexMap[ncoords];
      Time2DIndexMap timeIndexMap = null;
      for (int i = 0; i < ncoords; i++) {
        Coordinate coord = newCoords.get(i);
        if (coord.getType() == Coordinate.Type.time2D) {
          prev2Dcoord = (CoordinateTime2D) prevCoords.get(i);
          timeIndexMap = new Time2DIndexMap((CoordinateTime2D) coord, prev2Dcoord);
        } else {
          indexMaps[i] = new IndexMap(coord, prevCoords.get(i));
        }
      }

      int[] currIndex = new int[ncoords];
      int[] prevIndex = new int[ncoords];
      int[] track = new int[SparseArray.calcTotalSize(sizeArray)];

      // iterate through the contents of the prev track array
      Section section = new Section(prevSA.getShape());
      Section.Iterator iter = section.getIterator(prevSA.getShape());
      while (iter.hasNext()) {
        int oldTrackIdx = iter.next(prevIndex); // gets both the oldTrackIdx (1D) and prevIndex (nD)
        int oldTrackValue = prevSA.getTrack(oldTrackIdx);
        if (oldTrackValue == 0) continue; // skip missing values

        for (int i = 0; i < ncoords; i++) {
          Coordinate coord = newCoords.get(i);
          if (coord.getType() == Coordinate.Type.time2D) {
            CoordinateTime2D.Time2D prevValue = prev2Dcoord.getOrgValue(prevIndex[0], prevIndex[1]);
            currIndex[i] = timeIndexMap.map(prevValue);
          } else {
            currIndex[i] = indexMaps[i].map(prevIndex[i]);
          }
        }
        int trackIdx = workingSAbuilder.calcIndex(currIndex);
        track[trackIdx] = oldTrackValue;
      }

      // now that we have the track, make the real SA
      SparseArray<T> newSA = new SparseArray<>(sizeArray, track, prevSA.getContent(), prevSA.getNdups());  // content (list of records) is the same
      return new CoordinateND<>(newCoords, newSA);                                      // reindexed result
    }

    private static class Time2DIndexMap {
      Map<Object, Integer> currValMap;

      // every coord in prev must be in curr
      Time2DIndexMap(CoordinateTime2D curr, CoordinateTime2D prev) {
        assert curr.getType() == prev.getType() : curr.getType()+" != "+prev.getType();
        currValMap = new HashMap<>(2*curr.getValues().size());

        int[] index2D = new int[2];
        for (Object val : prev.getValues()) {
          if (!curr.getIndex((CoordinateTime2D.Time2D) val, index2D))
            throw new IllegalStateException();
          currValMap.put(val, index2D[1]); // save the time index
        }
      }

      public int map(CoordinateTime2D.Time2D prevCoord) {
        Integer val = currValMap.get(prevCoord);
        if (val == null)
          throw new IllegalStateException("reindex does not have coordinate Time2D "+prevCoord);
        return val;
      }

    }

  }
}

