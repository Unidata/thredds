package ucar.sparr;

import ucar.nc2.grib.collection.CoordinateRuntime;
import ucar.nc2.grib.grib2.Grib2Record;

import java.util.*;

/**
 * Create shared coordinates across variables in a group.
 *
 * @author caron
 * @since 12/10/13
 */
public class CoordinateUniquify {

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
         if (!coord.equals(runtimeAll)) { // LOOK - assumes that all variables have the same runtime coordinate
           System.out.println("Grib2Rectilyser coord.equals(runtimeAll");
         }
       }
       result.add(coordMap.get(coord)); // index into rect.coords
     }
     return result;
   }

   /**
    * Reindex with shared coordinates and return new CoordinateND
    * @param prev  previous
    * @param index new index into chared coordinates; may be null
    * @return new CoordinateND containing shared coordinates and sparseArray for the new coordinates
    */
   public CoordinateND<Grib2Record> reindex(CoordinateND<Grib2Record> prev, List<Integer> index) {
     if (index == null)
       index = new ArrayList<>();

     List<Coordinate> sharedCoords = new ArrayList<>();
     boolean needReindex = false;

     // redo the variables against the shared coordinates (at the moment this is just possibly runtime
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

     CoordinateND<Grib2Record> result;
     if (needReindex) {
       result = new CoordinateND<>(sharedCoords);
       result.reindex(prev);
       return result;

     } else {
       return new CoordinateND<>(sharedCoords, prev.getSparseArray());
     }
   }



   public List<Coordinate> getUnionCoords() {
     return unionCoords;
   }
}
