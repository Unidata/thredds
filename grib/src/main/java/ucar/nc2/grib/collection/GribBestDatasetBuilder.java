/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.grib.collection;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import ucar.nc2.grib.coord.Coordinate;
import ucar.nc2.grib.coord.CoordinateRuntime;
import ucar.nc2.grib.coord.CoordinateSharerBest;
import ucar.nc2.grib.coord.CoordinateTime2D;
import ucar.nc2.grib.coord.CoordinateTimeAbstract;

/**
 * Not used yet.
 *
 * @author caron
 * @since 3/8/2016.
 */
public class GribBestDatasetBuilder {

  static void makeDatasetBest(GribCollectionMutable result, List<GribCollectionMutable.GroupGC> groups2D, boolean isComplete) {
    GribCollectionMutable.Dataset dsBest = result.makeDataset(isComplete ? GribCollectionImmutable.Type.BestComplete : GribCollectionImmutable.Type.Best);

    // int npart = result.getPartitionSize();

    // for each 2D group
    for (GribCollectionMutable.GroupGC group2D : groups2D) {
      GribCollectionMutable.GroupGC groupB = dsBest.addGroupCopy(group2D);  // make copy of group, add to Best dataset
      groupB.isTwoD = false;

      // for each time2D, create the best time coordinates
      HashMap<Coordinate, CoordinateTimeAbstract> map2DtoBest = new HashMap<>(); // associate 2D coord with best
      CoordinateSharerBest sharer = new CoordinateSharerBest();
      for (Coordinate coord : group2D.coords) {
        if (coord instanceof CoordinateRuntime) continue; // skip it
        if (coord instanceof CoordinateTime2D) {
          CoordinateTimeAbstract best = ((CoordinateTime2D) coord).makeBestTimeCoordinate(result.masterRuntime);
          if (!isComplete) best = best.makeBestFromComplete();
          sharer.addCoordinate(best);
          map2DtoBest.put(coord, best);
        } else {
          sharer.addCoordinate(coord);
        }
      }
      groupB.coords = sharer.finish();  // these are the unique coords for group Best

      // transfer variables to Best group, set shared Coordinates
      for (GribCollectionMutable.VariableIndex vi2d : group2D.variList) {
        // copy vi2d and add to groupB
        GribCollectionMutable.VariableIndex viBest = result.makeVariableIndex(groupB, vi2d);
        groupB.addVariable(viBest);

        // set shared coordinates
        List<Coordinate> newCoords = new ArrayList<>();
        for (Integer groupIndex : vi2d.coordIndex) {
          Coordinate coord2D = group2D.coords.get(groupIndex);
          if (coord2D instanceof CoordinateRuntime) continue; // skip runtime;
          if (coord2D instanceof CoordinateTime2D) {
            newCoords.add(map2DtoBest.get(coord2D)); // add the best coordinate for that CoordinateTime2D
          } else {
            newCoords.add(coord2D);
          }
        }
        viBest.coordIndex = sharer.reindex(newCoords);
      }

    } // loop over groups
  }
}
