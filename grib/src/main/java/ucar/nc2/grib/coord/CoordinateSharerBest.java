/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.grib.coord;

import java.util.*;

/**
 * Create shared coordinate variables across the variables in the same group.
 * Use Coordinate.equals(), hashCode() to find unique coordinate variables.
 *
 * Used by GribPartitionBuilder.makeBest(), for all coordinates not CoordinateTime2D and not CoordinateRuntime.
 * Best coordinates are translated into equivilent 2D coordinate, thus we dont need seperate CoordinateND.
 *
 * @author caron
 * @since 12/10/13
 */
public class CoordinateSharerBest {
  private Map<Coordinate, Coordinate> runtimeBuilders = new HashMap<>();
  private Map<Coordinate, Coordinate> timeBuilders = new HashMap<>();
  private Map<Coordinate, Coordinate> timeIntvBuilders = new HashMap<>();
  private Map<Coordinate, Coordinate> vertBuilders = new HashMap<>();
  private Map<Coordinate, Coordinate> ensBuilders = new HashMap<>();
  private Map<Coordinate, Coordinate> time2DBuilders = new HashMap<>();
  private Map<Coordinate, Coordinate> swap = new HashMap<>();

  private Map<Coordinate, Integer> indexMap;

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
     // results
     List<Coordinate> unionCoords = new ArrayList<>(20);
     unionCoords.addAll(runtimeBuilders.keySet());
     unionCoords.addAll(time2DBuilders.keySet());
     unionCoords.addAll(timeBuilders.keySet());
     unionCoords.addAll(timeIntvBuilders.keySet());
     unionCoords.addAll(vertBuilders.keySet());
     unionCoords.addAll(ensBuilders.keySet());

     indexMap = new HashMap<>();
     for (int i = 0; i < unionCoords.size(); i++) {
       indexMap.put(unionCoords.get(i), i);
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
