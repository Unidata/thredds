/*
 * Copyright 1998-2015 John Caron and University Corporation for Atmospheric Research/Unidata
 *
 *  Portions of this software were developed by the Unidata Program at the
 *  University Corporation for Atmospheric Research.
 *
 *  Access and use of this software shall impose the following obligations
 *  and understandings on the user. The user is granted the right, without
 *  any fee or cost, to use, copy, modify, alter, enhance and distribute
 *  this software, and any derivative works thereof, and its supporting
 *  documentation for any purpose whatsoever, provided that this entire
 *  notice appears in all copies of the software, derivative works and
 *  supporting documentation.  Further, UCAR requests that the user credit
 *  UCAR/Unidata in any publications that result from the use of this
 *  software or in any product that includes this software. The names UCAR
 *  and/or Unidata, however, may not be used in any advertising or publicity
 *  to endorse or promote any products or commercial entity unless specific
 *  written permission is obtained from UCAR/Unidata. The user also
 *  understands that UCAR/Unidata is not obligated to provide the user with
 *  any support, consulting, training or assistance of any kind with regard
 *  to the use, operation and performance of this software nor to provide
 *  the user with any updates, revisions, new versions or "bug fixes."
 *
 *  THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 *  IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *  WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *  DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 *  INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 *  FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 *  NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 *  WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */
package ucar.coord;

import net.jcip.annotations.Immutable;
import ucar.ma2.Section;
import ucar.nc2.util.Indent;

import java.util.*;

/**
 * N coordinates and a SparseArray that they track.
 *
 * @author caron
 * @since 11/27/13
 */
@Immutable
public class CoordinateND<T> {

  private final List<Coordinate> coordinates; // result is orthogonal coordinates
  private final SparseArray<T> sa;            // indexes refer to coordinates

  public CoordinateND( List<Coordinate> coordinates, SparseArray<T> sa) {
    assert coordinates.size() == sa.getRank();
    this.coordinates = Collections.unmodifiableList(coordinates);
    this.sa = sa;
  }

  public List<Coordinate> getCoordinates() {
    return coordinates;
  }

  public int getNCoordinates() {
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

  ////////////////////

  public static class Builder<T> {
    private List<CoordinateBuilder<T>> builders = new ArrayList<>();
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

    public SparseArray<T> buildSparseArray(List<T> records, Formatter info) {
      int[] sizeArray = new int[coordb.size()];
      for (int i = 0; i < coordb.size(); i++) {
        Coordinate coord = coordb.get(i);
        if (coord.getType() == Coordinate.Type.time2D)
          sizeArray[i] = ((CoordinateTime2D) coord).getNtimes();
        else
          sizeArray[i] = coordb.get(i).getSize();
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

     /**
     * Reindex the sparse array, based on the new Coordinates.
     * Do this by running all the Records through the Coordinates, assigning each to a new spot in the new sparse array.
     *
     * @param prev must have same list of Coordinates, with possibly additional values.
     */
    public CoordinateND<T> reindex(List<Coordinate> newCoords, CoordinateND<T> prev) {
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
      List<IndexMapIF> indexMaps = new ArrayList<>();
      int count = 0;
      for (Coordinate curr : newCoords) {
        if (curr.getType() == Coordinate.Type.time2D)
          indexMaps.add(new Time2DIndexMap((CoordinateTime2D) curr, (CoordinateTime2D) prevCoords.get(count++)));
        else
          indexMaps.add(new IndexMap(curr, prevCoords.get(count++)));
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
        for (IndexMapIF indexMap : indexMaps) {
          currIndex[coordIdx] = indexMap.map(prevIndex[coordIdx]);
          coordIdx++;
        }
        int trackIdx = workingSAbuilder.calcIndex(currIndex);
        if (trackIdx >= track.length)
          System.out.println("HEY CoordinateND trackIdx >= track.length");
        track[trackIdx] = oldTrackValue;
      }

      // now that we have the track, make the real SA
      SparseArray<T> newSA = new SparseArray<>(sizeArray, track, prevSA.getContent(), prevSA.getNdups());  // content (list of records) is the same
      return new CoordinateND<>(newCoords, newSA);                                      // reindexed result
    }

    //////////////////////////////////////////////

     // a quick lookup of values from prev coordinate to current coordinate.
    private interface IndexMapIF {
      int map(int oldIndex);
    }

    private static class IndexMap implements IndexMapIF {
      boolean identity = true;
      int[] indexMap;

      IndexMap(Coordinate curr, Coordinate prev) {
        identity = curr.equals(prev);
        if (identity) return;

        assert curr.getType() == prev.getType() : curr.getType()+" != "+prev.getType();

        int count = 0;
        Map<Object, Integer> currValMap = new HashMap<>();
        if (curr.getValues() == null)
          System.out.println("HEY CoordinateND curr.getValues() == null");
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

    private static class Time2DIndexMap implements IndexMapIF {
      int[] indexMap;

      Time2DIndexMap(CoordinateTime2D curr, CoordinateTime2D prev) {
        assert curr.getType() == prev.getType() : curr.getType()+" != "+prev.getType();

        int[] index2D = new int[2];
        Map<Object, Integer> currValMap = new HashMap<>();
        for (Object val : curr.getValues()) {
          boolean ok = curr.getIndex((CoordinateTime2D.Time2D) val, index2D);
          if (!ok)
            System.out.println("HEY CoordinateND !ok");   // LOOK
          currValMap.put(val, index2D[1]); // want the time index
        }

        int count = 0;
        indexMap = new int[prev.getSize()];
        for (Object val : prev.getValues()) {
          if (currValMap.get(val) == null)
            System.out.printf("HEY Time2DIndexMap %s%n", val); // LOOK
          else
            indexMap[count++] = currValMap.get(val); // where does this value fit in the curr coordinates?
        }
      }

      public int map(int oldIndex) {
        return indexMap[oldIndex];
      }

    }

  }


}
