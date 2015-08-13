/* Copyright */
package ucar.nc2.ft2.coverage;

import ucar.ma2.InvalidRangeException;
import ucar.nc2.AttributeContainerHelper;
import ucar.nc2.time.CalendarDateRange;
import ucar.unidata.geoloc.LatLonRect;
import ucar.unidata.geoloc.ProjectionRect;

import java.util.*;

/**
 * Helper class to create logical subsets.
 * Used by CFwriter
 *
 * @author caron
 * @since 7/12/2015
 */
public class CoverageSubsetter {

  public CoverageDataset makeCoverageDatasetSubset(CoverageDataset org, List<String> gridsWanted, SubsetParams params) throws InvalidRangeException {

    // Get subset of original objects that are needed by the requested grids
    List<Coverage> orgCoverages = new ArrayList<>();
    Map<String, CoverageCoordSys> orgCoordSys = new HashMap<>();
    Map<String, CoverageCoordAxis> orgCoordAxis = new HashMap<>();
    Set<String> coordTransformSet = new HashSet<>();
    Set<HorizCoordSys> horizSet = new HashSet<>();

    for (String gridName : gridsWanted) {
      Coverage orgGrid =  org.findCoverage(gridName);
      if (orgGrid == null) continue;
      orgCoverages.add(orgGrid);
      CoverageCoordSys cs = orgGrid.getCoordSys();
      orgCoordSys.put(cs.getName(), cs);
      for (CoverageCoordAxis axis : cs.getAxes())
        orgCoordAxis.put(axis.getName(), axis);
      for (String tname : cs.getTransformNames())
        coordTransformSet.add(tname);
      if (cs.getHorizCoordSys() != null)
        horizSet.add(cs.getHorizCoordSys());
    }

    List<CoverageCoordSys> coordSys = new ArrayList<>();
    List<Coverage> coverages = new ArrayList<>();
    List<CoverageCoordAxis> coordAxes = new ArrayList<>();
    List<CoverageTransform> coordTransforms = new ArrayList<>();

    // subset non-dependent non-horiz axes
    for (CoverageCoordAxis orgAxis : orgCoordAxis.values()) {
      if (!orgAxis.getAxisType().isHoriz() && orgAxis.getDependenceType() != CoverageCoordAxis.DependenceType.dependent)
        coordAxes.add( orgAxis.subset(params));
    }
    // subset horiz axes
    for (HorizCoordSys hcs : horizSet) {
      HorizCoordSys hcsSubset = hcs.subset(params);
      coordAxes.addAll( hcsSubset.getCoordAxes());
    }
    // subset dependent axes
    for (CoverageCoordAxis orgAxis : orgCoordAxis.values()) {
      if (!orgAxis.getAxisType().isHoriz() && orgAxis.getDependenceType() == CoverageCoordAxis.DependenceType.dependent) {
        CoverageCoordAxis1D from = findIndependentAxis(orgAxis.getDependsOn(), coordAxes);
        coordAxes.add(orgAxis.subsetDependent(from));
      }
    }

    // subset coordSys, coverages, transforms
    for (CoverageCoordSys orgCs : orgCoordSys.values())
      coordSys.add( new CoverageCoordSys(orgCs));

    for (Coverage orgCov : orgCoverages)
      coverages.add( new Coverage(orgCov, null));

    for (String tname : coordTransformSet) {
      CoverageTransform t = org.findCoordTransform(tname);
      if (t != null)
        coordTransforms.add(t);
    }

    // LOOK TODO
    LatLonRect latLonBoundingBox = null;
    ProjectionRect projBoundingBox = null;
    CalendarDateRange dateRange = null;

    return new CoverageDataset(org.getName(), org.getCoverageType(), new AttributeContainerHelper(org.getName(), org.getGlobalAttributes()),
            latLonBoundingBox, projBoundingBox, dateRange,
            coordSys, coordTransforms, coordAxes, coverages, org.reader);  // use org.reader -> subset always in coord space !
  }

  CoverageCoordAxis1D findIndependentAxis(String want, List<CoverageCoordAxis> axes) {
    String name = want == null ? null : want.trim();
    for (CoverageCoordAxis axis : axes)
      if (axis instanceof CoverageCoordAxis1D && axis.getName().equalsIgnoreCase(name))
        return (CoverageCoordAxis1D) axis;
    return null;
  }

}
