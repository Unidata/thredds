package ucar.sparr;

import ucar.nc2.grib.collection.CoordinateRuntime;
import ucar.nc2.grib.collection.CoordinateTime2D;
import ucar.nc2.grib.grib2.Grib2Record;

import java.util.*;

/**
 * Create shared coordinates across variables in the same group,
 * to form the set of group coordinates.
 * Use object.equals() to find unique coordinates.
 *
 * @author John
 * @since 1/4/14
 */
public class CoordinateSharer {
  static private final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(CoordinateSharer.class);
  boolean runtimeUnion;

  /**
   *
   * @param runtimeUnion if true, make union of runtimes ("dense"), otherwise keep seperate runtimes if distinct
   */
  public CoordinateSharer(boolean runtimeUnion) {
    this.runtimeUnion = runtimeUnion;
  }

  List<Coordinate> unionCoords = new ArrayList<>();
  Coordinate runtimeAll;

  CoordinateBuilder runtimeAllBuilder = new CoordinateRuntime.Builder();
  Set<Coordinate> runtimeBuilders = new HashSet<>();
  Set<Coordinate> timeBuilders = new HashSet<>();
  Set<Coordinate> timeIntvBuilders = new HashSet<>();
  Set<Coordinate> vertBuilders = new HashSet<>();
  Set<Coordinate> time2DBuilders = new HashSet<>();
  Map<Coordinate, Integer> coordMap;

  public void addCoords(List<Coordinate> coords) {
    for (Coordinate coord : coords) {
      switch (coord.getType()) {
        case runtime:
          if (runtimeUnion) runtimeAllBuilder.addAll(coord); // always union
          else runtimeBuilders.add(coord);                   // unique coordinates
          break;
        case time:
          timeBuilders.add(coord);
          break;
        case timeIntv:
          timeIntvBuilders.add(coord);
          break;
        case vert:
          vertBuilders.add(coord);
          break;
        case time2D:
          CoordinateTime2D time2D = (CoordinateTime2D) coord;
          time2DBuilders.add(coord);
          if (runtimeUnion) runtimeAllBuilder.addAll(time2D.getRuntimeCoordinate()); // ?? never ??
          break;
      }
    }
  }

  public void finish() {
    if (runtimeUnion) {
      runtimeAll = runtimeAllBuilder.finish();
      unionCoords.add(runtimeAll);
    }
    for (Coordinate coord : runtimeBuilders) unionCoords.add(coord); // will be empty if not used
    for (Coordinate coord : timeBuilders) unionCoords.add(coord);
    for (Coordinate coord : timeIntvBuilders) unionCoords.add(coord);
    for (Coordinate coord : vertBuilders) unionCoords.add(coord);
    for (Coordinate coord : time2DBuilders) unionCoords.add(coord);

    coordMap = new HashMap<>();
    for (int i = 0; i < this.unionCoords.size(); i++) {
      coordMap.put(this.unionCoords.get(i), i);
    }
  }

  // this is the set of shared coordinates to be stored in the group
  public List<Coordinate> getUnionCoords() {
    return unionCoords;
  }

    // find indexes into unionCoords of a variable's  coordinates, this is what is stored in ncx2
   public List<Integer> reindex(List<Coordinate> coords) {
    List<Integer> result = new ArrayList<>();
    for (Coordinate coord : coords) {
      Integer idx = coordMap.get(coord); // index into unionCoords
      if (idx == null && runtimeUnion) {
        if (coord.getType() == Coordinate.Type.runtime)
          idx = 0;
        else
          logger.error("CoordinateSharer can find coordinate {}", coord);
      }
      result.add(idx);
    }
    return result;
  }

  /**
   * If using runtimeUnion, you must reindex the sparse array
   * @param prev  previous CoordinateND
   * @param index new index into shared coordinates; may be null
   * @return new CoordinateND containing shared coordinates and sparseArray for the new coordinates
   *   or the prev CoordinateND if not needed.
   */
  public CoordinateND<Grib2Record> reindex(CoordinateND<Grib2Record> prev, List<Integer> index) {
    if (index == null)
      index = new ArrayList<>();

    List<Coordinate> sharedCoords = new ArrayList<>();
    boolean needReindex = false;

    // redo the variables against the shared coordinates (LOOK this is just possibly runtime
    for (Coordinate coord : prev.getCoordinates()) {
      if (runtimeUnion && coord.getType() == Coordinate.Type.runtime) {
        if (!coord.equals(runtimeAll)) {
          needReindex = true;
          index.add(coordMap.get(runtimeAll)); // index into unionCoords
          sharedCoords.add(runtimeAll);
          continue;
        }
      }
      index.add(coordMap.get(coord)); // index into rect.coords
      sharedCoords.add(coord);
    }

    CoordinateND<Grib2Record> result;
    if (needReindex) {
      result = new CoordinateND<>(sharedCoords);
      result.reindex(prev);
      return result;

    } else {
      return new CoordinateND<>(sharedCoords, prev.getSparseArray());
    }
  }

}
