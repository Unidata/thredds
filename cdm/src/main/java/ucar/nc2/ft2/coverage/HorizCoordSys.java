/* Copyright */
package ucar.nc2.ft2.coverage;

import net.jcip.annotations.Immutable;
import ucar.ma2.InvalidRangeException;
import ucar.unidata.geoloc.LatLonRect;
import ucar.unidata.geoloc.ProjectionImpl;
import ucar.unidata.geoloc.ProjectionRect;
import ucar.unidata.geoloc.projection.VerticalPerspectiveView;
import ucar.unidata.geoloc.projection.sat.Geostationary;
import ucar.unidata.geoloc.projection.sat.MSGnavigation;

import java.util.ArrayList;
import java.util.List;

/**
 * Horizontal CoordSys.
 * Must have x,y,proj (or) lat,lon
 *
 * @author caron
 * @since 7/11/2015
 */
@Immutable
public class HorizCoordSys {
  public final CoverageCoordAxis xaxis, yaxis, lataxis, lonaxis;
  public final CoverageTransform transform;
  public final boolean hasProjection, hasLatLon;

  public HorizCoordSys(CoverageCoordAxis xaxis, CoverageCoordAxis yaxis, CoverageCoordAxis lataxis, CoverageCoordAxis lonaxis, CoverageTransform transform) {
    this.xaxis = xaxis;
    this.yaxis = yaxis;
    this.lataxis = lataxis;
    this.lonaxis = lonaxis;
    this.transform = transform;
    this.hasProjection = (xaxis != null) && (yaxis != null) && (transform != null);
    boolean checkLatLon = (lataxis != null) && (lonaxis != null);
    assert hasProjection || checkLatLon : "missing horiz coordinates (x,y,projection or lat,lon)";

    if (hasProjection && checkLatLon) {
      boolean ok = true;
      if (!lataxis.getDependsOn().equalsIgnoreCase(lonaxis.getDependsOn())) ok = false;
      if (lataxis.getDependenceType() != CoverageCoordAxis.DependenceType.twoD) ok = false;
      if (lonaxis.getDependenceType() != CoverageCoordAxis.DependenceType.twoD) ok = false;
      String dependsOn = lataxis.getDependsOn();
      if (!dependsOn.contains(xaxis.getName())) ok = false;
      if (!dependsOn.contains(yaxis.getName())) ok = false;
      if (!ok) {
        checkLatLon = false;
      }
    }

    this.hasLatLon = checkLatLon;
  }

  public String getName() {
    if (hasProjection) {
      return xaxis.getName()+" "+yaxis.getName()+" " + transform.getName();
    }
    if (hasLatLon) {
      return lataxis.getName()+" "+lonaxis.getName();
    }
    return null;
  }

  public List<CoverageCoordAxis> subset(SubsetParams subset) throws InvalidRangeException {
    List<CoverageCoordAxis> result = new ArrayList<>();

    /* LatLonRect llbb = (LatLonRect) subset.get(SubsetParams.latlonBB);
    ProjectionRect projbb = (ProjectionRect) subset.get(SubsetParams.projBB);

    if (projbb != null) {
      result.add( xaxis.subset(projbb.getMinX(), projbb.getMaxX()));
      result.add( yaxis.subset(projbb.getMinY(), projbb.getMaxY()));
      return result;
    }

    if (llbb != null) {
      if (transform == null) { // this means its a latlon
        result.add(lonaxis.subset(llbb.getLonMin(), llbb.getLonMax()));  // heres where to deal with crossing seam
        result.add(lataxis.subset(llbb.getLatMin(), llbb.getLatMax()));
        return result;
      }

      // we have to transform latlon to projection coordinates
      ProjectionImpl proj = transform.getProjection();
      if (!(proj instanceof VerticalPerspectiveView) && !(proj instanceof MSGnavigation) && !(proj instanceof Geostationary)) { // LOOK kludge - how to do this generrally ??
        LatLonRect bb = org.getLatLonBoundingBox(); // first clip the request rectangle to the bounding box of the grid LOOK bb may be null
        LatLonRect rect2 = bb.intersect(llbb);
        if (null == rect2)
          throw new InvalidRangeException("Request Bounding box does not intersect Grid ");
        llbb = rect2;
      }

      ProjectionRect prect = proj.latLonToProjBB(llbb); // allow projection to override
      result.add(xaxis.subset(prect.getMinX(), prect.getMaxX()));
      result.add(yaxis.subset(prect.getMinY(), prect.getMaxY()));
      return result;
    }

    // otherwise leave originals
    if (hasProjection) {
      result.add(xaxis.copy(null));
      result.add(yaxis.copy(null));
    }

    if (hasLatLon) {
      result.add(lataxis.copy(null));
      result.add(lonaxis.copy(null));
    }   */

    return result;

  }
}
