/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.ft2.coverage.writer;

import ucar.ma2.InvalidRangeException;
import ucar.nc2.AttributeContainerHelper;
import ucar.nc2.ft2.coverage.*;
import ucar.nc2.time.CalendarDateRange;
import ucar.unidata.geoloc.LatLonRect;
import ucar.unidata.geoloc.ProjectionRect;

import java.util.*;

/**
 * Helper class to create logical subsets.
 * Used by CFGridCoverageWriter2
 *
 * @author caron
 * @since 7/12/2015
 */
public class CoverageSubsetter2 {

  public static ucar.nc2.util.Optional<CoverageCollection> makeCoverageDatasetSubset(CoverageCollection org, List<String> gridsWanted, SubsetParams params) throws InvalidRangeException {

    // Get subset of original objects that are needed by the requested grids
    List<Coverage> orgCoverages = new ArrayList<>();
    Map<String, CoverageCoordSys> orgCoordSys = new HashMap<>();  // eliminate duplicates
    Set<String> coordTransformSet = new HashSet<>();              // eliminate duplicates

    for (String gridName : gridsWanted) {
      Coverage orgGrid =  org.findCoverage(gridName);
      if (orgGrid == null) continue;
      orgCoverages.add(orgGrid);
      CoverageCoordSys cs = orgGrid.getCoordSys();
      orgCoordSys.put(cs.getName(), cs);
      for (String tname : cs.getTransformNames())
        coordTransformSet.add(tname);
    }

    // LOOK bail out if any fail, make more robust
    // subset all coordSys, and eliminate duplicate axes.
    Map<String, CoverageCoordAxis> subsetCoordAxes = new HashMap<>();
    Map<String, CoverageCoordSys> subsetCFCoordSys = new HashMap<>();
    for (CoverageCoordSys orgCs : orgCoordSys.values()) {
      ucar.nc2.util.Optional<CoverageCoordSys> opt = orgCs.subset(params, true, false); // subsetCF make do some CF tweaks, not needed in regular subset
      if (!opt.isPresent())
        return ucar.nc2.util.Optional.empty(opt.getErrorMessage());

      CoverageCoordSys subsetCoordSys = opt.get();
      subsetCFCoordSys.put(orgCs.getName(), subsetCoordSys);
      for (CoverageCoordAxis axis : subsetCoordSys.getAxes()) {
        subsetCoordAxes.put(axis.getName(), axis);  // eliminate duplicates
      }
    }

    // here are the objects we need to make the subsetted dataset
    List<CoverageCoordSys> coordSys = new ArrayList<>();
    List<CoverageCoordAxis> coordAxes = new ArrayList<>();
    List<Coverage> coverages = new ArrayList<>();
    List<CoverageTransform> coordTransforms = new ArrayList<>();

    for (CoverageCoordSys subsetCs : subsetCFCoordSys.values()) {
      coordSys.add( subsetCs);
    }

    for (CoverageCoordAxis subsetAxis : subsetCoordAxes.values())
      coordAxes.add( subsetAxis);               // must use a copy, because of setDataset()

    for (Coverage orgCov : orgCoverages) {
      // must substitute subsetCS
      CoverageCoordSys subsetCs = subsetCFCoordSys.get(orgCov.getCoordSysName());
      coverages.add( new Coverage(orgCov, subsetCs)); // must use a copy, because of setCoordSys()
    }

    for (String tname : coordTransformSet) {
      CoverageTransform t = org.findCoordTransform(tname); // these are truly immutable, so can use originals
      if (t != null)
        coordTransforms.add(t);
    }

    // put it all together
    return ucar.nc2.util.Optional.of(new CoverageCollection(org.getName(), org.getCoverageType(),
            new AttributeContainerHelper(org.getName(), org.getGlobalAttributes()),
            null, null, null,
            coordSys, coordTransforms, coordAxes, coverages, org.getReader()));  // use org.reader -> subset always in coord space !
  }

  CoverageCoordAxis1D findIndependentAxis(String want, List<CoverageCoordAxis> axes) {
    String name = want == null ? null : want.trim();
    for (CoverageCoordAxis axis : axes)
      if (axis instanceof CoverageCoordAxis1D && axis.getName().equalsIgnoreCase(name))
        return (CoverageCoordAxis1D) axis;
    return null;
  }

}
