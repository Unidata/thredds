/* Copyright */
package ucar.nc2.ft2.coverage;

/**
 * Horizontal CoordSys.
 * Must have x,y,proj (or) lat,lon
 *
 * @author caron
 * @since 7/11/2015
 */
public class CoverageCoordSysHoriz {
  public CoverageCoordAxis xaxis, yaxis, lataxis, lonaxis;
  public CoverageTransform transform;
  public boolean hasProjection, hasLatLon;

  public CoverageCoordSysHoriz(CoverageCoordAxis xaxis, CoverageCoordAxis yaxis, CoverageCoordAxis lataxis, CoverageCoordAxis lonaxis, CoverageTransform transform) {
    this.xaxis = xaxis;
    this.yaxis = yaxis;
    this.lataxis = lataxis;
    this.lonaxis = lonaxis;
    this.transform = transform;
    this.hasProjection = (xaxis != null) && (yaxis != null) && (transform != null);
    this.hasLatLon = (lataxis != null) && (lonaxis != null);
    assert hasProjection || hasLatLon : "missing horiz coordinates (x,y,projection or lat,lon)";

    if (hasProjection && hasLatLon) {
      boolean ok = true;
      if (!lataxis.getDependsOn().equalsIgnoreCase(lonaxis.getDependsOn())) ok = false;
      if (lataxis.getDependenceType() != CoverageCoordAxis.DependenceType.twoD) ok = false;
      if (lonaxis.getDependenceType() != CoverageCoordAxis.DependenceType.twoD) ok = false;
      String dependsOn = lataxis.getDependsOn();
      if (!dependsOn.contains(xaxis.getName())) ok = false;
      if (!dependsOn.contains(yaxis.getName())) ok = false;
      if (!ok) {
        hasLatLon = false;
        this.lataxis = null;
        this.lonaxis = null;
      }
    }
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
