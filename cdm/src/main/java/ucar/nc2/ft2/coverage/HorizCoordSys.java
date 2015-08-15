/*
 * Copyright 1998-2015 John Caron and University Corporation for Atmospheric Research/Unidata
 *
 *  Portions of this software were developed by the Unidata Program at the
 *  University Corporation for Atmospheric Research.
 *
 *  Access and use of this software shall impose the following obligations
 *  and understandings on the user. The user is granted the right, without
 *  any fee or cost, to use, copy, modify, alter, enhance and distribute
 *  this software, and any derivative works thereof, and its supporting
 *  documentation for any purpose whatsoever, provided that this entire
 *  notice appears in all copies of the software, derivative works and
 *  supporting documentation.  Further, UCAR requests that the user credit
 *  UCAR/Unidata in any publications that result from the use of this
 *  software or in any product that includes this software. The names UCAR
 *  and/or Unidata, however, may not be used in any advertising or publicity
 *  to endorse or promote any products or commercial entity unless specific
 *  written permission is obtained from UCAR/Unidata. The user also
 *  understands that UCAR/Unidata is not obligated to provide the user with
 *  any support, consulting, training or assistance of any kind with regard
 *  to the use, operation and performance of this software nor to provide
 *  the user with any updates, revisions, new versions or "bug fixes."
 *
 *  THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 *  IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *  WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *  DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 *  INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 *  FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 *  NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 *  WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */
package ucar.nc2.ft2.coverage;

import net.jcip.annotations.Immutable;
import ucar.ma2.InvalidRangeException;
import ucar.unidata.geoloc.LatLonRect;
import ucar.unidata.geoloc.ProjectionImpl;
import ucar.unidata.geoloc.ProjectionRect;

import java.util.ArrayList;
import java.util.List;

/**
 * Horizontal CoordSys.
 * Must have x,y,proj (or) lat,lon.
 * May have both.
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

  public List<CoverageCoordAxis> getCoordAxes() throws InvalidRangeException {
    List<CoverageCoordAxis> result = new ArrayList<>();
    if (xaxis != null) result.add(xaxis);
    if (yaxis != null) result.add(yaxis);
    if (lataxis != null) result.add(lataxis);
    if (lonaxis != null) result.add(lonaxis);
    return result;
  }

  public CoverageTransform getTransform() {
    return transform;
  }

  public boolean getHasLatLon() {
    return hasLatLon;
  }

  /////////////////////////////////////////////////////////////////////////////////////

  public HorizCoordSys subset(SubsetParams params) throws InvalidRangeException {

    LatLonRect llbb = (LatLonRect) params.get(SubsetParams.latlonBB);
    ProjectionRect projbb = (ProjectionRect) params.get(SubsetParams.projBB);
    // if (projbb == null && llbb == null) return this;

    CoverageCoordAxis xaxisSubset = null, yaxisSubset = null, lataxisSubset= null, lonaxisSubset = null;

    if (projbb != null) {
      if (hasProjection) {
        xaxisSubset = xaxis.subset(projbb.getMinX(), projbb.getMaxX());
        yaxisSubset = yaxis.subset(projbb.getMinY(), projbb.getMaxY());
      }

      if (hasLatLon) {
        ProjectionImpl proj = transform.getProjection();
        LatLonRect llrect = proj.projToLatLonBB(projbb);
        lonaxisSubset = lonaxis.subset(llrect.getLonMin(), llrect.getLonMax());
        lataxisSubset = lataxis.subset(llrect.getLatMin(), llrect.getLatMax());
      }
    }

    else if (llbb != null) {
      if (hasLatLon) {
        lonaxisSubset = lonaxis.subset(llbb.getLonMin(), llbb.getLonMax());  // heres where to deal with crossing seam
        lataxisSubset = lataxis.subset(llbb.getLatMin(), llbb.getLatMax());
      }

      if (hasProjection) {
      // we have to transform latlon to projection coordinates
      ProjectionImpl proj = transform.getProjection();
      /* if (!(proj instanceof VerticalPerspectiveView) && !(proj instanceof MSGnavigation) && !(proj instanceof Geostationary)) { // LOOK kludge - how to do this generrally ??
        LatLonRect bb = getLatLonBoundingBox(); // first clip the request rectangle to the bounding box of the grid LOOK bb may be null
        LatLonRect rect2 = bb.intersect(llbb);
        if (null == rect2)
          throw new InvalidRangeException("Request Bounding box does not intersect Grid ");
        llbb = rect2;
      } */

        ProjectionRect prect = proj.latLonToProjBB(llbb); // allow projection to override
        xaxisSubset = xaxis.subset(prect.getMinX(), prect.getMaxX());
        yaxisSubset = yaxis.subset(prect.getMinY(), prect.getMaxY());
      }
    }

    if (xaxisSubset == null) xaxisSubset = xaxis;
    if (yaxisSubset == null) yaxisSubset = yaxis;
    if (lataxisSubset == null) lataxisSubset = lataxis;
    if (lonaxisSubset == null) lonaxisSubset = lonaxis;

    return new HorizCoordSys(xaxisSubset, yaxisSubset, lataxisSubset, lonaxisSubset, transform);
  }

  public CoverageCoordAxis getXAxis() {
    return (xaxis != null) ? xaxis : lonaxis;
  }

  public CoverageCoordAxis getYAxis() {
    return (yaxis != null) ? yaxis : lataxis;
  }

}
