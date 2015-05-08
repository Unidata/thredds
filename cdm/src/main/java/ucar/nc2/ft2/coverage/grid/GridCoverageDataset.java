/* Copyright */
package ucar.nc2.ft2.coverage.grid;

import ucar.nc2.Attribute;
import ucar.nc2.constants.AxisType;
import ucar.nc2.dataset.CoordTransBuilder;
import ucar.nc2.time.CalendarDateRange;
import ucar.nc2.util.Indent;
import ucar.unidata.geoloc.*;
import ucar.unidata.geoloc.projection.LatLonProjection;

import java.io.IOException;
import java.util.*;

/**
 * maybe a single horiz coord system? single Calendar. single runtime coord? single time coord?
 *
 message GridCoverageDataset {
   required string name = 1;
   repeated Attribute atts = 2;
   repeated CoordSys coordSys = 3;
   repeated CoordTransform coordTransforms = 4;
   repeated CoordAxis coordAxes = 5;
   repeated GridCoverage grids = 6;
 }
 * @author caron
 * @since 5/2/2015
 */
public class GridCoverageDataset implements AutoCloseable {

  String name;
  List<Attribute> globalAttributes;
  LatLonRect latLonBoundingBox;
  ProjectionRect projBoundingBox;
  CalendarDateRange calendarDateRange;

  List<GridCoordSys> coordSys;
  List<GridCoordTransform> coordTransforms;
  List<GridCoordAxis> coordAxes;
  List<GridCoverage> grids;

  public void close() throws IOException {
    // NOOP
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public List<Attribute> getGlobalAttributes() {
    return globalAttributes;
  }

  public void setGlobalAttributes(List<Attribute> globalAttributes) {
    this.globalAttributes = globalAttributes;
  }

  public LatLonRect getLatLonBoundingBox() {
    return latLonBoundingBox;
  }

  public void setLatLonBoundingBox(LatLonRect latLonBoundingBox) {
    this.latLonBoundingBox = latLonBoundingBox;
  }

  public ProjectionRect getProjBoundingBox() {
    return projBoundingBox;
  }

  public void setProjBoundingBox(ProjectionRect projBoundingBox) {
    this.projBoundingBox = projBoundingBox;
  }

  public CalendarDateRange getCalendarDateRange() {
    return calendarDateRange;
  }

  public void setCalendarDateRange(CalendarDateRange calendarDateRange) {
    this.calendarDateRange = calendarDateRange;
  }

  public List<GridCoverage> getGrids() {
    return grids;
  }

  public void setGrids(List<GridCoverage> grids) {
    this.grids = grids;
  }

  public List<GridCoordSys> getCoordSys() {
    return coordSys;
  }

  public void setCoordSys(List<GridCoordSys> coordSys) {
    this.coordSys = coordSys;
  }

  public List<GridCoordTransform> getCoordTransforms() {
    return (coordTransforms != null) ? coordTransforms : new ArrayList<GridCoordTransform>();
  }

  public void setCoordTransforms(List<GridCoordTransform> coordTransforms) {
    this.coordTransforms = coordTransforms;
  }

  public List<GridCoordAxis> getCoordAxes() {
    return coordAxes;
  }

  public void setCoordAxes(List<GridCoordAxis> coordAxes) {
    this.coordAxes = coordAxes;
  }

  @Override
  public String toString() {
    Formatter f = new Formatter();
    toString(f);
    return f.toString();
  }

  public void toString(Formatter f) {
    Indent indent = new Indent(2);
    f.format("%sGridDatasetCoverage %s%n", indent, name);
    f.format("%s Global attributes:%n", indent);
    for (Attribute att : globalAttributes)
      f.format("%s  %s%n", indent, att);
    f.format("%s Date Range:%s%n", indent, calendarDateRange);
    f.format("%s LatLon BoundingBox:%s%n", indent, latLonBoundingBox);
    if (projBoundingBox != null)
      f.format("%s Projection BoundingBox:%s%n", indent, projBoundingBox);
    f.format("%s Coordinate Systems:%n", indent);
    for (GridCoordSys cs : coordSys)
      cs.toString(f, indent);
    f.format("%s Coordinate Transforms:%n", indent);
    for (GridCoordTransform t : coordTransforms)
      t.toString(f, indent);
    f.format("%s Coordinate Axes:%n", indent);
    for (GridCoordAxis a : coordAxes)
      a.toString(f, indent);
    f.format("%n%s Grids:%n", indent);
    for (GridCoverage grid : grids)
      grid.toString(f, indent);
  }

  public GridCoverage findCoverage(String name) {
    for (GridCoverage grid : grids)
      if (grid.getName().equalsIgnoreCase(name)) return grid;
    return null;
  }

  public GridCoordSys findCoordSys(String name) {
    for (GridCoordSys gcs : coordSys)
      if (gcs.getName().equalsIgnoreCase(name)) return gcs;
    return null;
  }

  public GridCoordAxis findCoordAxis(String name) {
    for (GridCoordAxis axis : coordAxes)
      if (axis.getName().equalsIgnoreCase(name)) return axis;
    return null;
  }

  public GridCoordTransform findCoordTransform(String name) {
    for (GridCoordTransform ct : coordTransforms)
      if (ct.getName().equalsIgnoreCase(name)) return ct;
    return null;
  }

  public GridCoordAxis getXAxis(GridCoordSys gcs) {
    for (String axisName : gcs.getAxisNames()) {
       GridCoordAxis axis = findCoordAxis(axisName);
       if (axis.axisType == AxisType.GeoX || axis.axisType == AxisType.Lon) {
         return axis;
       }
     }
    throw new IllegalArgumentException("Cant find X axis for coordsys "+gcs.getName());
  }

  public GridCoordAxis getYAxis(GridCoordSys gcs) {
    for (String axisName : gcs.getAxisNames()) {
       GridCoordAxis axis = findCoordAxis(axisName);
       if (axis.axisType == AxisType.GeoY || axis.axisType == AxisType.Lat) {
         return axis;
       }
     }
    throw new IllegalArgumentException("Cant find Y axis for coordsys "+gcs.getName());
  }

  public GridCoordAxis getZAxis(GridCoordSys gcs) {
    for (String axisName : gcs.getAxisNames()) {
       GridCoordAxis axis = findCoordAxis(axisName);
       if (axis.axisType == AxisType.GeoZ || axis.axisType == AxisType.Height || axis.axisType == AxisType.Pressure) {
         return axis;
       }
     }
    return null;
  }

  ///////////////////////////////////////////////////////////////

  public ProjectionImpl getProjection(GridCoverage coverage) {
    GridCoordSys gcs = findCoordSys(coverage.getCoordSysName());
    for (String ctName : gcs.getTransformNames()) {
      GridCoordTransform ct = findCoordTransform(ctName);
      if (ct.isHoriz)
        return makeProjection(ct);
    }
    return new LatLonProjection();
  }

  public ProjectionImpl makeProjection(GridCoordTransform transform) {
    ProjectionImpl proj = transform.getProjection();
    if (proj == null) {
      Formatter errInfo = new Formatter();
      proj = CoordTransBuilder.makeProjection(transform, errInfo);  // LOOK could store the projection in the transform
    }
    transform.setProjection(proj);
    return proj;
  }

  /* public boolean containsLatLonPoint(String gridName, LatLonPoint latlon) {
    GridCoverage grid = findCoverage(gridName);
    GridCoordSys gcs = findCoordSys(grid.getCoordSysName());
    Projection p = getProjection(grid);
    ProjectionPoint pp = p.latLonToProj(latlon, null);
    int[] xy = gcs.findXYindexFromCoord(pp.getX(), pp.getY(), null);
    return !(xy[0] < 0 || xy[1] < 0);
  }

  public boolean hasEnsembleDimension() {
    return false;
  }  */

}
