/* Copyright */
package ucar.nc2.ft2.coverage.grid;

import ucar.nc2.Attribute;
import ucar.nc2.ft.cover.CoverageCS;
import ucar.nc2.ft2.remote.CdmrFeatureProto;
import ucar.nc2.time.CalendarDateRange;
import ucar.unidata.geoloc.LatLonRect;
import ucar.unidata.geoloc.ProjectionRect;

import java.util.*;

/**
 * Describe
 *
 * @author caron
 * @since 5/8/2015
 */
public class GridDatasetHelper {
  GridCoverageDataset gds;
  List<String> gridNames;   // null nmeans all grids
  List<Gridset> gridsets;

  GridDatasetHelper(GridCoverageDataset gds) {
    this.gds = gds;
    this.gridsets = makeGridsets();
  }

  GridDatasetHelper(GridCoverageDataset gds, List<String> gridNames) {
    this.gds = gds;
    this.gridNames = gridNames;
    this.gridsets = makeGridsets();
  }

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

  // these are just the grids which are asked for; grouped by coordinate system into a "Gridset
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

  // make a subsetted GridCoverageDataset
  // the cool thing is we only have to subset the CoordAxes !!
  public GridCoverageDataset subset(GridSubset subset) {
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
      axes.add( subset(orgAxis, subset));
    }
    result.setCoordAxes(axes);

    // LOOK TODO
    LatLonRect latLonBoundingBox;
    ProjectionRect projBoundingBox;
    CalendarDateRange calendarDateRange;

    return result;
  }

  private GridCoordAxis subset(GridCoordAxis orgAxis, GridSubset subset) {
    switch (orgAxis.getAxisType()) {
      case GeoZ:
      case Pressure:
      case Height:
        String val = subset.get(GridCoordAxis.Type.Z);
        if (val == null) return orgAxis;
          val = subset.get("vertCoord");  // ncss using this
        if (val == null) return orgAxis;
        double dval = Double.parseDouble(val);
        return orgAxis.subset(dval, dval);
    }

    return orgAxis;
  }
}
