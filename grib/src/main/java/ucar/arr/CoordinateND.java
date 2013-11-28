package ucar.arr;

import ucar.nc2.grib.grib2.Grib2Record;
import ucar.nc2.grib.grib2.builder.CoordinateTime;
import ucar.nc2.util.Indent;

import java.util.*;

/**
 * Describe
 *
 * @author caron
 * @since 11/27/13
 */
public class CoordinateND {

  CoordinateBuilder root;
  List<Coordinate> coordinates;
  SparseArray<Grib2Record> sa;

  public CoordinateND(CoordinateBuilder root) {
    this.root = root;
  }

  public void add(Grib2Record gr) {
    root.add(gr);
  }

  public void finish() {
    root.finish();
    buildOrthogonalCoordinates();
    buildSparseArray(root);

    /* sa = new SparseArray<>(nruns, maxTimes);
    int runIdx = 0;
    for (CoordinateTime time : timesSorted) { // one for each runtime
      for (int timeIdx = 0; timeIdx < time.getSize(); timeIdx++) {
        for (Grib2Record r : time.getRecordList(timeIdx))
          sa.add(r, runIdx, timeIdx);
      }
      runIdx++;
    } */

  }

  private void buildOrthogonalCoordinates() {
    coordinates = new ArrayList<>();
    coordinates.add(root.getCoordinate());
    buildOrthogonalCoordinates(root, root.getCoordinate());
  }

  private Coordinate buildOrthogonalCoordinates(CoordinateBuilder builder, Coordinate coord) {
    Set<Object> allKeys = new HashSet<>();
    for (Object key : coord.getValues()) {
      CoordinateBuilder nestedBuilder = builder.getChildBuilder(key);
      Coordinate nestedCoord = nestedBuilder.getCoordinate();
      for (Object nestedVal : nestedCoord.getValues())
        allKeys.add(nestedVal);
    }
    return builder.makeNestedCoord(allKeys);
  }



  private void buildSparseArray(CoordinateBuilder builder, Coordinate coordinate) {
    int ncoords = coordinate.size();
    for (CoordinateTime.Builder bucket : timeMap.values()) {
      CoordinateTime tc = null; // bucket.finish();
      timesSorted.add(tc);
      maxTimes = Math.max(maxTimes, tc.getSize());
    }

  }

  public void showInfo(Formatter info) {
    showInfo(coordinates, info, new Indent(2));
    //sa.showInfo(info);
  }

  // recurse
  private void showInfo(Coordinate coord, Formatter info, Indent indent) {
    //Coordinate coord = coords.get(0);
    coord.showInfo(info, indent);

    //if (coords.size() > 1) {
   //   showInfo(coords.subList(1, coords.size()), info, indent.incr());
   //   indent.decr();
   // }
  }



}
