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

  // results
  private List<Coordinate> unionCoords;
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
     unionCoords = new ArrayList<>(20);
     for (Coordinate coord : runtimeBuilders.keySet()) unionCoords.add(coord);
     for (Coordinate coord : time2DBuilders.keySet()) unionCoords.add(coord);
     for (Coordinate coord : timeBuilders.keySet()) unionCoords.add(coord);
     for (Coordinate coord : timeIntvBuilders.keySet()) unionCoords.add(coord);
     for (Coordinate coord : vertBuilders.keySet()) unionCoords.add(coord);
     for (Coordinate coord : ensBuilders.keySet()) unionCoords.add(coord);

     indexMap = new HashMap<>();
     for (int i = 0; i < unionCoords.size(); i++) {
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
