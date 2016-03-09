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
 *
 */
package ucar.nc2.grib.collection;

import ucar.coord.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * not used yet.
 *
 * @author caron
 * @since 3/8/2016.
 */
public class GribBestDatasetBuilder {

  static void makeDatasetBest(GribCollectionMutable result, List<GribCollectionMutable.GroupGC> groups2D, boolean isComplete) throws IOException {
    GribCollectionMutable.Dataset dsBest = result.makeDataset(isComplete ? GribCollectionImmutable.Type.BestComplete : GribCollectionImmutable.Type.Best);

    // int npart = result.getPartitionSize();

    // for each 2D group
    for (GribCollectionMutable.GroupGC group2D : groups2D) {
      GribCollectionMutable.GroupGC groupB = dsBest.addGroupCopy(group2D);  // make copy of group, add to Best dataset
      groupB.isTwoD = false;

      // for each time2D, create the best time coordinates
      HashMap<Coordinate, CoordinateTimeAbstract> map2DtoBest = new HashMap<>(); // associate 2D coord with best
      CoordinateUniquify sharer = new CoordinateUniquify();
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
