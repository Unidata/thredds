package ucar.coord;

//import ucar.nc2.grib.grib2.Grib2Record;

import java.util.*;

/**
 * Create shared coordinates across variables in the same group,
 * to form the set of group coordinates.
 * Use object.equals() to find unique coordinates.
 * NOT USED
 *
 * @author caron
 * @since 12/10/13
 */
public class CoordinateUniquify<T> {
  static private org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(CoordinateUniquify.class);

   List<Coordinate> unionCoords = new ArrayList<>();
   Coordinate runtimeAll;

   CoordinateBuilder runtimeAllBuilder = new CoordinateRuntime.Builder2(null);
   Set<Coordinate> timeBuilders = new HashSet<>();
   Set<Coordinate> timeIntvBuilders = new HashSet<>();
   Set<Coordinate> vertBuilders = new HashSet<>();
   Set<Coordinate> ensBuilders = new HashSet<>();
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
         case ens:
           ensBuilders.add(coord);
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
     for (Coordinate coord : ensBuilders) unionCoords.add(coord);

     coordMap = new HashMap<>();
     for (int i = 0; i < this.unionCoords.size(); i++) {
       coordMap.put(this.unionCoords.get(i), i);
     }
   }

     // redo the variables against the shared coordinates
    public List<Integer> reindex(List<Coordinate> coords) {
     List<Integer> result = new ArrayList<>();
     for (Coordinate coord : coords) {
       Integer idx = coordMap.get(coord); // index into unionCoords
       if (idx == null) {
         if (coord.getType() == Coordinate.Type.runtime)
           idx = 0;
         else
           logger.warn("CoordinateUniquify.reindex missing coordinate");
       }
       result.add(idx);
     }
     return result;
   }

   /**
    * Reindex with shared coordinates and return new CoordinateND
    * @param prev  previous
    * @param index new index into shared coordinates; may be null
    * @return new CoordinateND containing shared coordinates and sparseArray for the new coordinates
    */
   public CoordinateND<T> reindex(CoordinateND<T> prev, List<Integer> index) {
     if (index == null)
       index = new ArrayList<>();

     List<Coordinate> sharedCoords = new ArrayList<>();
     boolean needReindex = false;

     // redo the variables against the shared coordinates (LOOK this is just possibly runtime
     for (Coordinate coord : prev.getCoordinates()) {
       if (coord.getType() == Coordinate.Type.runtime) {
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

     if (needReindex) {
       return new CoordinateND.Builder<T>().reindex(sharedCoords, prev);

     } else {
       return new CoordinateND<>(sharedCoords, prev.getSparseArray());
     }
   }

   public List<Coordinate> getUnionCoords() {
     return unionCoords;
   }
}
