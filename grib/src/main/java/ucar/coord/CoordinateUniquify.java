package ucar.coord;

import ucar.nc2.util.Misc;

import java.util.*;

/**
 * Create shared coordinates across variables in the same group,
 * to form the set of group coordinates.
 * Use object.equals() to find unique coordinates.
 *
 * @author caron
 * @since 12/10/13
 */
public class CoordinateUniquify {
  static private org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(CoordinateUniquify.class);

  HashMap<Coordinate, Coordinate> runtimeBuilders = new HashMap<>();
  HashMap<Coordinate, Coordinate> timeBuilders = new HashMap<>();
  HashMap<Coordinate, Coordinate> timeIntvBuilders = new HashMap<>();
  HashMap<Coordinate, Coordinate> vertBuilders = new HashMap<>();
  HashMap<Coordinate, Coordinate> ensBuilders = new HashMap<>();
  HashMap<Coordinate, Coordinate> time2DBuilders = new HashMap<>();
  HashMap<Coordinate, Coordinate> swap = new HashMap<>();

  List<Coordinate> unionCoords;
  Map<Coordinate, Integer> indexMap;

  public void addCoordinates(List<Coordinate> coords) {
    for (Coordinate coord : coords) addCoordinate(coord);
  }

  public void addCoordinate(Coordinate coord) {
    Coordinate already;
    switch (coord.getType()) {
      case runtime:
        already = runtimeBuilders.get(coord);
        if (already == null)
          runtimeBuilders.put(coord, coord);
        else
          swap.put(coord, already);  // keep track of substitutes
        break;

      case time:
        already = timeBuilders.get(coord);
        if (already == null)
          timeBuilders.put(coord, coord);
        else
          swap.put(coord, already);
        break;

      case timeIntv:
        already = timeIntvBuilders.get(coord);
        if (already == null)
          timeIntvBuilders.put(coord, coord);
        else
          swap.put(coord, already);
        break;

      case time2D:
        already = time2DBuilders.get(coord);
        if (already == null)
          time2DBuilders.put(coord, coord);
        else
          swap.put(coord, already);
        break;

      case vert:
        already = vertBuilders.get(coord);
        if (already == null)
          vertBuilders.put(coord, coord);
        else
          swap.put(coord, already);
        break;

      case ens:
        already = ensBuilders.get(coord);
        if (already == null)
          ensBuilders.put(coord, coord);
        else
          swap.put(coord, already);
        break;
    }
  }

   public List<Coordinate> finish() {
     unionCoords = new ArrayList<>(20);
     for (Coordinate coord : runtimeBuilders.keySet()) unionCoords.add(coord);
     for (Coordinate coord : time2DBuilders.keySet()) unionCoords.add(coord);
     for (Coordinate coord : timeBuilders.keySet()) unionCoords.add(coord);
     for (Coordinate coord : timeIntvBuilders.keySet()) unionCoords.add(coord);
     for (Coordinate coord : vertBuilders.keySet()) unionCoords.add(coord);
     for (Coordinate coord : ensBuilders.keySet()) unionCoords.add(coord);

     indexMap = new HashMap<>();
     for (int i = 0; i < this.unionCoords.size(); i++) {
       indexMap.put(this.unionCoords.get(i), i);
     }

     return unionCoords;
   }

     // redo the variables against the shared coordinates
    public List<Integer> reindex(List<Coordinate> coords) {
     List<Integer> result = new ArrayList<>();
     for (Coordinate coord : coords) {
       Coordinate sub = swap.get(coord);
       Coordinate use = (sub == null) ? coord : sub;
       Integer idx = indexMap.get(use); // index into unionCoords
       if (idx == null) {
         throw new IllegalStateException();
       }
       result.add(idx);
     }
     return result;
   }
}
