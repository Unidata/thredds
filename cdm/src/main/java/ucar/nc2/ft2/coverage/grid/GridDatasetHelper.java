/* Copyright */
package ucar.nc2.ft2.coverage.grid;

import ucar.ma2.InvalidRangeException;
import ucar.ma2.Range;
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
 * Helper class for  GridCoverageDataset.
 *  1) groups GridCoverage by GridCoordSys into a Gridsets
 *  2) subsets GridCoverageDataset
 *
 * @author caron
 * @since 5/8/2015
 */
public class GridDatasetHelper {
  GridCoverageDataset gds;
  List<String> gridNames;     // null means all grids
  List<Gridset> gridsets;     // only the named grids
  HorizCoordSys horizCoordSys;    // only one

  public GridDatasetHelper(GridCoverageDataset gds) {
    this.gds = gds;
    this.gridsets = makeGridsets();
    this.horizCoordSys = makeHorizCoordSys();
  }

  public GridDatasetHelper(GridCoverageDataset gds, List<String> gridNames) {
    this.gds = gds;
    this.gridNames = gridNames;
    this.gridsets = makeGridsets();
    this.horizCoordSys = makeHorizCoordSys();
  }

  ////////////////////////////////////////////////////////////////////

  public static class Gridset {
    public GridCoordSys gcs;
    public List<GridCoverage> grids;

    public Gridset(GridCoordSys gcs) {
      this.gcs = gcs;
      grids = new ArrayList<>();
    }
  }

  public List<Gridset> getGridsets() {
    return gridsets;
  }

  // these are just the grids which are asked for in gridNames; grouped by coordinate system into a "Gridset"
  private List<Gridset> makeGridsets() {
    Map<String, Gridset> map = new HashMap<>();

    for (GridCoverage grid : gds.getGrids()) {
      if (gridNames != null && !gridNames.contains(grid.getName())) continue;  // skip ones you dont want
      Gridset gset = map.get(grid.getCoordSysName());
      if (gset == null) {
        gset = new Gridset(gds.findCoordSys(grid.getCoordSysName()));
        map.put(grid.getCoordSysName(), gset);
      }
      gset.grids.add(grid);
    }

    List<Gridset> result = new ArrayList<>(map.values());
    Collections.sort(result, new Comparator<Gridset>() {
      @Override
      public int compare(Gridset o1, Gridset o2) {
        return o1.gcs.getName().compareTo(o2.gcs.getName());
      }
    });

    return result;
  }

  public List<Range> makeSubset(GridCoverageDataset subsetDataset, Gridset gridset) throws InvalidRangeException {
    List<Range> ranges = new ArrayList<>();
    for (String axisName : gridset.gcs.getAxisNames()) {
      GridCoordAxis axis = subsetDataset.findCoordAxis(axisName);
      if (!(axis.getDependenceType() == GridCoordAxis.DependenceType.independent)) continue;
      ranges.add(new Range(axis.getAxisType().name(), (int) axis.getMinIndex(), (int) axis.getMaxIndex(), axis.getStride()));
    }
    return ranges;
  }

    /////////////////////////////////////////////////////

  public static class HorizCoordSys {
    public GridCoordAxis xaxis;
    public GridCoordAxis yaxis;
    public GridCoordTransform transform;

    public HorizCoordSys(GridCoordAxis xaxis, GridCoordAxis yaxis, GridCoordTransform transform) {
      this.xaxis = xaxis;
      this.yaxis = yaxis;
      this.transform = transform;
    }

    // just match on names
    public boolean same(HorizCoordSys that) {
      if (this == that) return true;

      if (!xaxis.getName().equals(that.xaxis.getName())) return false;
      if (!yaxis.getName().equals(that.yaxis.getName())) return false;
      return !(transform != null ? !transform.getName().equals(that.transform.getName()) : that.transform != null);
    }

  }

  private HorizCoordSys makeHorizCoordSys() {
    HorizCoordSys result = null;
    for (Gridset gridset : gridsets) {
      GridCoordAxis xaxis = gds.getXAxis(gridset.gcs);
      GridCoordAxis yaxis = gds.getYAxis(gridset.gcs);
      GridCoordTransform hct = null;

      for (String ctName : gridset.gcs.getTransformNames()) {
        GridCoordTransform ct = gds.findCoordTransform(ctName);
        if (ct.isHoriz) hct = ct;
      }

      HorizCoordSys hcs = new HorizCoordSys(xaxis, yaxis, hct);
      if (result == null) result = hcs;
      else assert result.same(hcs);
    }
    return result;
  }

  /////////////////////////////////////////////////////

  // make a subsetted GridCoverageDataset
  // the cool thing is we only have to subset the CoordAxes !!
  public GridCoverageDataset subset(GridSubset subset) throws InvalidRangeException {
    GridCoverageDataset result = new GridCoverageDataset();

    result.setName(gds.getName());
    result.setGlobalAttributes(gds.atts);

    List<GridCoordSys> coordSys = new ArrayList<>();
    List<GridCoverage> grids = new ArrayList<>();
    Set<String> transformNames = new HashSet<>();
    Set<String> axisNames = new HashSet<>();

    for (Gridset gridset : gridsets) {
      coordSys.add(gridset.gcs);
      grids.addAll(gridset.grids);
      for (String ctName : gridset.gcs.getTransformNames())
        transformNames.add(ctName);
      for (String axisName : gridset.gcs.getAxisNames())
        axisNames.add(axisName);
    }

    result.setCoordSys(coordSys);
    result.setGrids( grids);

    List<GridCoordTransform> transforms = new ArrayList<>();
    for (String ctName : transformNames) {
      transforms.add( gds.findCoordTransform(ctName));
    }
    result.setCoordTransforms(transforms);

    List<GridCoordAxis> axes = new ArrayList<>();
    for (String axisName : axisNames) {
      GridCoordAxis orgAxis = gds.findCoordAxis(axisName);
      GridCoordAxis subAxis = subset(orgAxis, subset, result.getCalendar());
      if (subAxis != null) // leave out x, y
        axes.add( subAxis);
    }
    subsetHorizAxes(subset, axes);
    result.setCoordAxes(axes);

    // LOOK TODO
    LatLonRect latLonBoundingBox;
    ProjectionRect projBoundingBox;
    CalendarDateRange calendarDateRange;

    return result;
  }

  private GridCoordAxis subset(GridCoordAxis orgAxis, GridSubset subset, ucar.nc2.time.Calendar cal) {
    switch (orgAxis.getAxisType()) {
      case GeoZ:
      case Pressure:
      case Height:
        Double dval = (subset == null) ? null : subset.getDouble(GridSubset.vertCoord);
        if (dval == null) return orgAxis.finish();

        // LOOK problems when vertCoord doesnt match any coordinates in the axes
        return orgAxis.subset(dval, dval);

      case RunTime:
        return orgAxis.finish();

      case Time:
        if (subset.isTrue(GridSubset.allTimes))
          return orgAxis.finish();
        if (subset.isTrue(GridSubset.latestTime))
          return orgAxis.subsetLatest();

        CalendarDate date = (CalendarDate) subset.get(GridSubset.date);
        if (date != null)
          return orgAxis.subset(cal, date);

        CalendarDateRange dateRange = (CalendarDateRange) subset.get(GridSubset.dateRange);
         if (dateRange != null)
           return orgAxis.subset(cal, dateRange);

     }

    return null;
  }

  /**
   *     if (hasProjectionBB())
         subset.set(GridSubset.projBB, getProjectionBB());
       else if (hasLatLonBB())
         subset.set(GridSubset.latlonBB, getLatLonBoundingBox());
       if (horizStride != null)
         subset.set(GridSubset.horizStride, horizStride);

   */
  private void subsetHorizAxes(GridSubset subset, List<GridCoordAxis> result) throws InvalidRangeException {
    LatLonRect llbb = (LatLonRect) subset.get(GridSubset.latlonBB);
    ProjectionRect projbb = (ProjectionRect) subset.get(GridSubset.projBB);

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
        LatLonRect bb = gds.getLatLonBoundingBox(); // first clip the request rectangle to the bounding box of the grid
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
    result.add(horizCoordSys.xaxis);
    result.add(horizCoordSys.yaxis);
  }

}
