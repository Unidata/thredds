/* Copyright */
package ucar.nc2.ft2.coverage;

import ucar.ma2.InvalidRangeException;
import ucar.nc2.AttributeContainerHelper;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.time.CalendarDateRange;
import ucar.unidata.geoloc.LatLonRect;
import ucar.unidata.geoloc.ProjectionImpl;
import ucar.unidata.geoloc.ProjectionRect;
import ucar.unidata.geoloc.projection.VerticalPerspectiveView;
import ucar.unidata.geoloc.projection.sat.Geostationary;
import ucar.unidata.geoloc.projection.sat.MSGnavigation;

import java.util.*;

/**
 * Helper class to create logical subsets
 *
 * @author caron
 * @since 7/12/2015
 */
public class CoverageSubsetter {

  private CoverageDataset org;
  private List<String> gridsWanted;
  private SubsetParams subset;

  public CoverageSubsetter(CoverageDataset org, List<String> gridsWanted, SubsetParams subset) {
    this.org = org;
    this.gridsWanted = gridsWanted;
    this.subset = subset;
  }

  public CoverageDataset makeCoverageDatasetSubset() throws InvalidRangeException {

    // Get subset of original objects that are needed by the requested grids
    List<Coverage> orgCoverages = new ArrayList<>();
    Map<String, CoverageCoordSys> orgCoordSys = new HashMap<>();
    Map<String, CoverageCoordAxis> orgCoordAxis = new HashMap<>();
    Set<String> coordTransformSet = new HashSet<>();
    Set<CoverageCoordSysHoriz> horizSet = new HashSet<>();

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

    // subset axes
    for (CoverageCoordAxis orgAxis : orgCoordAxis.values()) {
      CoverageCoordAxis axis = subset(orgAxis, subset);
      if (axis != null)
        coordAxes.add(axis);
    }

    for (CoverageCoordSysHoriz hcs : horizSet)
      subsetHorizAxes(subset, hcs, coordAxes);

    // subset coordSys, coverages, transforms
    for (CoverageCoordSys orgCs : orgCoordSys.values())
      coordSys.add( new CoverageCoordSys(orgCs));

    for (Coverage orgCov : orgCoverages)
      coverages.add( new Coverage(orgCov));

    for (String tname : coordTransformSet) {
      CoverageTransform t = org.findCoordTransform(tname);
      if (t != null)
        coordTransforms.add(t);
    }

    // LOOK TODO
    LatLonRect latLonBoundingBox = null;
    ProjectionRect projBoundingBox = null;
    CalendarDateRange dateRange = null;


    //   String name, CoverageCoordSys.Type coverageType, AttributeContainerHelper atts,
    //                         LatLonRect latLonBoundingBox, ProjectionRect projBoundingBox, CalendarDateRange calendarDateRange,
    //                          List<CoverageCoordSys> coordSys, List<CoverageTransform> coordTransforms, List<CoverageCoordAxis> coordAxes, List<Coverage> coverages) {

    return new CoverageDataset(org.getName(), org.getCoverageType(), new AttributeContainerHelper(org.getName(), org.getGlobalAttributes()),
            latLonBoundingBox, projBoundingBox, dateRange,
            coordSys, coordTransforms, coordAxes, coverages, org.reader);  // use org.reader -> subset always in coord space !

  }

  // LOOK  incomplete handling of subsetting params
  private CoverageCoordAxis subset(CoverageCoordAxis orgAxis, SubsetParams subset) {
    CoordAxisHelper helper = new CoordAxisHelper(orgAxis);

    switch (orgAxis.getAxisType()) {
      case GeoZ:
      case Pressure:
      case Height:
        Double dval = subset.getDouble(SubsetParams.vertCoord);
        if (dval != null) {
          // LOOK problems when vertCoord doesnt match any coordinates in the axes
          return helper.subsetClosest(dval);
        }
        break;

      // x,y gets seperately subsetted
      case GeoX:
      case GeoY:
      case Lat:
      case Lon:
        return null;

      case Time:
        if (subset.isTrue(SubsetParams.allTimes))
          return helper.copy( org.getCalendar());
        if (subset.isTrue(SubsetParams.latestTime))
          return helper.subsetLatest(org.getCalendar());

        CalendarDate date = (CalendarDate) subset.get(SubsetParams.date);
        if (date != null)
          return helper.subset(org.getCalendar(), date);

        CalendarDateRange dateRange = (CalendarDateRange) subset.get(SubsetParams.dateRange);
        if (dateRange != null)
          return helper.subset(org.getCalendar(), dateRange);
        break;
    }

    // otherwise take the entire axis
    return helper.copy(null);
  }

  private void subsetHorizAxes(SubsetParams subset, CoverageCoordSysHoriz horizCoordSys, List<CoverageCoordAxis> result) throws InvalidRangeException {
    LatLonRect llbb = (LatLonRect) subset.get(SubsetParams.latlonBB);
    ProjectionRect projbb = (ProjectionRect) subset.get(SubsetParams.projBB);

    if (projbb != null) {
      result.add(horizCoordSys.xaxis.subset(projbb.getMinX(), projbb.getMaxX()));
      result.add(horizCoordSys.yaxis.subset(projbb.getMinY(), projbb.getMaxY()));
      return;
    }

    if (llbb != null) {
      if (horizCoordSys.transform == null) { // this means its a latlon
        result.add(horizCoordSys.lonaxis.subset(llbb.getLonMin(), llbb.getLonMax()));  // heres where to deal with crossing seam
        result.add(horizCoordSys.lataxis.subset(llbb.getLatMin(), llbb.getLatMax()));
        return;
      }

      // we have to transform latlon to projection coordinates
      ProjectionImpl proj = horizCoordSys.transform.getProjection();
      if (!(proj instanceof VerticalPerspectiveView) && !(proj instanceof MSGnavigation) && !(proj instanceof Geostationary)) { // LOOK kludge - how to do this generrally ??
        LatLonRect bb = org.getLatLonBoundingBox(); // first clip the request rectangle to the bounding box of the grid LOOK bb may be null
        LatLonRect rect2 = bb.intersect(llbb);
        if (null == rect2)
          throw new InvalidRangeException("Request Bounding box does not intersect Grid ");
        llbb = rect2;
      }

      ProjectionRect prect = proj.latLonToProjBB(llbb); // allow projection to override
      result.add(horizCoordSys.xaxis.subset(prect.getMinX(), prect.getMaxX()));
      result.add(horizCoordSys.yaxis.subset(prect.getMinY(), prect.getMaxY()));
      return;
    }

    // otherwise leave originals
    if (horizCoordSys.hasProjection) {
      result.add(horizCoordSys.xaxis.copy(null));
      result.add(horizCoordSys.yaxis.copy(null));
    }
    if (horizCoordSys.hasLatLon) {
      result.add(horizCoordSys.lataxis.copy(null));
      result.add(horizCoordSys.lonaxis.copy(null));
    }
  }
}
