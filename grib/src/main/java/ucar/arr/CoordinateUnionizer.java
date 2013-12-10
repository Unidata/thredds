package ucar.arr;

import ucar.nc2.grib.collection.CoordinateRuntime;

import java.util.*;

/**
 * Description
 *
 * @author John
 * @since 12/10/13
 */
public class CoordinateUnionizer {

  List<Coordinate> unionCoords = new ArrayList<>();
  Coordinate runtimeAll;

  CoordinateBuilder runtimeAllBuilder = new CoordinateRuntime.Builder();
  Set<Coordinate> timeBuilders = new HashSet<>();
  Set<Coordinate> timeIntvBuilders = new HashSet<>();
  Set<Coordinate> vertBuilders = new HashSet<>();
  Map<Coordinate, Integer> coordMap;

  public void addCoords(List<Coordinate> coords) {
    for (Coordinate coord : coords) {
      switch (coord.getType()) {
        case runtime:
          runtimeAllBuilder.addAll(coord); // always union
          break;
        case time:
          timeBuilders.add(coord);        // unique coordinates
          break;
        case timeIntv:
          timeIntvBuilders.add(coord);
          break;
        case vert:
          vertBuilders.add(coord);
          break;
      }
    }
  }

  public void finish() {
    runtimeAll = runtimeAllBuilder.finish();
    unionCoords.add(runtimeAll);
    for (Coordinate coord : timeBuilders) unionCoords.add(coord);
    for (Coordinate coord : timeIntvBuilders) unionCoords.add(coord);
    for (Coordinate coord : vertBuilders) unionCoords.add(coord);

    coordMap = new HashMap<>();
    for (int i = 0; i < this.unionCoords.size(); i++) {
      coordMap.put(this.unionCoords.get(i), i);
    }
  }

  public List<Integer> reindex(List<Coordinate> coords) {
    // redo the variables against the shared coordinates (at the moment this is just possibly runtime
    List<Integer> result = new ArrayList<>();
    for (Coordinate coord : coords) {
      if (coord.getType() == Coordinate.Type.runtime) {
        if (!coord.equals(runtimeAll)) { // LOOK - wrong
          System.out.println("Grib2Rectilyser coord.equals(runtimeAll");
        }
      }
      result.add(coordMap.get(coord)); // index into rect.coords
    }
    return result;
  }

  public List<Coordinate> getUnionCoords() {
    return unionCoords;
  }

  public Coordinate getRuntimeAll() {
    return runtimeAll;
  }
}
