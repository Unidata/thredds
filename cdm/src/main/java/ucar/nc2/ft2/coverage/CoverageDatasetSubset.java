/* Copyright */
package ucar.nc2.ft2.coverage;

import ucar.ma2.Array;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Attribute;
import ucar.nc2.AttributeContainer;
import ucar.nc2.constants.AxisType;
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
 * Describe
 *
 * @author caron
 * @since 7/12/2015
 */
public class CoverageDatasetSubset {

  private CoverageDataset org;
  private List<String> gridsWanted;
  private SubsetParams subset;

  private List<CoordSysSet> csSet = new ArrayList<>();
  private List<CoverageCoordSys> coordSys = new ArrayList<>();
  private List<Coverage> coverages = new ArrayList<>();
  private List<CoverageCoordAxis> coordAxes = new ArrayList<>();
  private List<CoverageTransform> coordTransforms = new ArrayList<>();


  private Set<String> transformNames = new HashSet<>();
  private Set<String> axisNames = new HashSet<>();

  private final Map<String, Coverage> coverageMap = new HashMap<>();

  private LatLonRect latLonBoundingBox;
  private ProjectionRect projBoundingBox;
  private CalendarDateRange dateRange;



  public CoverageDatasetSubset(CoverageDataset org, List<String> gridsWanted, SubsetParams subset) {
    this.org = org;
    this.gridsWanted = gridsWanted;
    this.subset = subset;

    // Get set of orgCoordSys we need to subset
    List<Coverage> orgCoverages = new ArrayList<>();
    for (String gridName : gridsWanted) {
      Coverage orgGrid =  org.findCoverage(gridName);
      if (orgGrid == null) continue;
      orgCoverages.add(orgGrid);
    }
    List<CoordSysSet> orgCoordSys = makeCoordSysSets(orgCoverages);
    for (CoordSysSet cs : orgCoordSys) {
      //csSet.add( subset(cs));
    }
  }

  private List<CoordSysSet> makeCoordSysSets(List<Coverage> coverages) {
    Map<String, CoordSysSet> map = new HashMap<>();

    for (Coverage coverage : coverages) {
      CoordSysSet gset = map.get(coverage.getCoordSysName());
      if (gset == null) {
        gset = new CoordSysSet(coverage.getCoordSys());
        map.put(coverage.getCoordSysName(), gset);
      }
      gset.addCoverage(coverage);
    }

    // sort the coordsys
    List<CoordSysSet> csets = new ArrayList<>(map.values());
    Collections.sort(csets, (o1, o2) -> o1.getCoordSys().getName().compareTo(o2.getCoordSys().getName()));
    return csets;
  }

  public String findAttValueIgnoreCase(String attName, String defaultValue) {
     return org.findAttValueIgnoreCase(attName, defaultValue);
   }

   public Attribute findAttribute(String attName) {
     return org.findAttribute(attName);
   }

   public Attribute findAttributeIgnoreCase(String attName) {
     return org.findAttributeIgnoreCase(attName);
   }

   public String getName() {
     return org.getName();
   }

   public List<Attribute> getGlobalAttributes() {
     return org.getGlobalAttributes();
   }

   public LatLonRect getLatLonBoundingBox() {
     return latLonBoundingBox;
   }

   public ProjectionRect getProjBoundingBox() {
     return projBoundingBox;
   }

   public CalendarDateRange getCalendarDateRange() {
     return dateRange;
   }

   public ucar.nc2.time.Calendar getCalendar() {
     return org.getCalendar();
   }

   public Iterable<Coverage> getCoverages() {
     return coverageMap.values();
   }

   public CoverageCoordSys.Type getCoverageType() {
     return org.getCoverageType();
   }

   public List<CoverageCoordSys> getCoordSys() {
     return coordSys;
   }

   public List<CoverageTransform> getCoordTransforms() {
     return (coordTransforms != null) ? coordTransforms : new ArrayList<>();
   }

   public List<CoverageCoordAxis> getCoordAxes() {
     return coordAxes;
   }


  public Array readSubset(Coverage grid){
    return null;
  }


  /*
   // make a subsetted GridCoverageDataset
  // the cool thing is we only have to subset the CoordAxes !!
  private void subset(SubsetParams subset) throws InvalidRangeException {

    coordSys = new ArrayList<>();
    grids = new ArrayList<>();
    transformNames = new HashSet<>();
    axisNames = new HashSet<>();

    for (Gridset gridset : gridsets) {
      coordSys.add(gridset.gcs);
      grids.addAll(gridset.grids);
      for (String ctName : gridset.gcs.getTransformNames())
        transformNames.add(ctName);
      for (String axisName : gridset.gcs.getAxisNames())
        axisNames.add(axisName);
    }


    List<GridCoordTransform> transforms = new ArrayList<>();
    for (String ctName : transformNames) {
      transforms.add(gds.findCoordTransform(ctName));
    }
    result.setCoordTransforms(transforms);

    List<GridCoordAxis> axes = new ArrayList<>();
    for (String axisName : axisNames) {
      GridCoordAxis orgAxis = gds.findCoordAxis(axisName);
      GridCoordAxis subAxis = subset(orgAxis, subset, result.getCalendar());
      if (subAxis != null) // leave out x, y
        axes.add(subAxis);
    }
    subsetHorizAxes(subset, axes);
    result.setCoordAxes(axes);

    // LOOK TODO
    LatLonRect latLonBoundingBox;
    ProjectionRect projBoundingBox;
    CalendarDateRange calendarDateRange;

    return result;
  }

  // LOOK incomplete subsetting
  private CoverageCoordAxis subset(CoverageCoordAxis orgAxis, SubsetParams subset, ucar.nc2.time.Calendar cal) {
    switch (orgAxis.getAxisType()) {
      case GeoZ:
      case Pressure:
      case Height:
        Double dval = subset.getDouble(SubsetParams.vertCoord);
        if (dval != null) {
          // LOOK problems when vertCoord doesnt match any coordinates in the axes
          return orgAxis.subsetClosest(dval);
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
          return orgAxis.finish();
        if (subset.isTrue(SubsetParams.latestTime))
          return orgAxis.subsetLatest();

        CalendarDate date = (CalendarDate) subset.get(SubsetParams.date);
        if (date != null)
          return orgAxis.subset(cal, date);

        CalendarDateRange dateRange = (CalendarDateRange) subset.get(SubsetParams.dateRange);
        if (dateRange != null)
          return orgAxis.subset(cal, dateRange);
        break;
    }

    // otherwise take the entire axis
    return orgAxis.finish();
  }

  private void subsetHorizAxes(SubsetParams subset, List<CoverageCoordAxis> result) throws InvalidRangeException {
    LatLonRect llbb = (LatLonRect) subset.get(SubsetParams.latlonBB);
    ProjectionRect projbb = (ProjectionRect) subset.get(SubsetParams.projBB);

    if (projbb != null) {
      result.add(horizCoordSys.xaxis.subset(projbb.getMinX(), projbb.getMaxX()));
      result.add(horizCoordSys.yaxis.subset(projbb.getMinY(), projbb.getMaxY()));
      return;
    }

    if (llbb != null) {
      if (horizCoordSys.transform == null) { // this means its a latlon
        result.add(horizCoordSys.xaxis.subset(llbb.getLonMin(), llbb.getLonMax()));  // heres where to deal with crossing seam
        result.add(horizCoordSys.yaxis.subset(llbb.getLatMin(), llbb.getLatMax()));
        return;
      }

      // we have to transform latlon to projection coordinates
      ProjectionImpl proj = gds.makeProjection(horizCoordSys.transform);
      if (!(proj instanceof VerticalPerspectiveView) && !(proj instanceof MSGnavigation) && !(proj instanceof Geostationary)) { // LOOK kludge - how to do this generrally ??
        LatLonRect bb = gds.getLatLonBoundingBox(); // first clip the request rectangle to the bounding box of the grid LOOK bb may be null
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
      result.add(horizCoordSys.xaxis);
      result.add(horizCoordSys.yaxis);
    }
    if (horizCoordSys.hasLatLon) {
      result.add(horizCoordSys.lataxis);
      result.add(horizCoordSys.lonaxis);
    }
  } */
}
