/* Copyright */
package ucar.nc2.ft2.coverage;

import net.jcip.annotations.Immutable;

/**
 * Horizontal CoordSys.
 * Must have x,y,proj (or) lat,lon
 *
 * @author caron
 * @since 7/11/2015
 */
@Immutable
public class CoverageCoordSysHoriz {
  public final CoverageCoordAxis xaxis, yaxis, lataxis, lonaxis;
  public final CoverageTransform transform;
  public final boolean hasProjection, hasLatLon;

  public CoverageCoordSysHoriz(CoverageCoordAxis xaxis, CoverageCoordAxis yaxis, CoverageCoordAxis lataxis, CoverageCoordAxis lonaxis, CoverageTransform transform) {
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
}
