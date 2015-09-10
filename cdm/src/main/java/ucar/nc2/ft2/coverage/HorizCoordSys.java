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
import ucar.ma2.RangeIterator;
import ucar.nc2.util.Optional;
import ucar.unidata.geoloc.*;

import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;

/**
 * Horizontal CoordSys.
 * Must have x,y,proj (or) lat,lon.
 * May have both.
 * Must be exactly one in a CoverageDataset
 *
 * @author caron
 * @since 7/11/2015
 */
@Immutable
public class HorizCoordSys {
  public final CoverageCoordAxis1D xaxis, yaxis;
  public final CoverageCoordAxis lataxis, lonaxis;
  public final CoverageTransform transform;
  public final boolean hasProjection, hasLatLon, is2DlatLon;

  public HorizCoordSys(CoverageCoordAxis1D xaxis, CoverageCoordAxis1D yaxis, CoverageCoordAxis lataxis, CoverageCoordAxis lonaxis, CoverageTransform transform) {
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
    this.is2DlatLon = lataxis instanceof LatLonAxis2D;
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

  public boolean is2DlatLon() {
    return is2DlatLon;
  }

  /////////////////////////////////////////////////////////////////////////////////////

  public Optional<HorizCoordSys> subset(SubsetParams params) throws InvalidRangeException {

    LatLonRect llbb = (LatLonRect) params.get(SubsetParams.latlonBB);
    ProjectionRect projbb = (ProjectionRect) params.get(SubsetParams.projBB);

    CoverageCoordAxis1D xaxisSubset = null, yaxisSubset = null;
    CoverageCoordAxis lataxisSubset= null, lonaxisSubset = null;
    Optional<CoverageCoordAxis> buildero;

    Formatter errMessages = new Formatter();
    if (projbb != null) {
      if (hasProjection) {
        buildero = xaxis.subset(projbb.getMinX(), projbb.getMaxX());
        if (buildero.isPresent()) xaxisSubset = (CoverageCoordAxis1D) buildero.get(); else errMessages.format("xaxis: %s;%n", buildero.getErrorMessage());
        buildero = yaxis.subset(projbb.getMinY(), projbb.getMaxY());
        if (buildero.isPresent()) yaxisSubset = (CoverageCoordAxis1D) buildero.get(); else errMessages.format("yaxis: %s;%n", buildero.getErrorMessage());
      }

      if (hasLatLon) {
        ProjectionImpl proj = transform.getProjection();
        LatLonRect llrect = proj.projToLatLonBB(projbb);
        buildero = lonaxis.subset(llrect.getLonMin(), llrect.getLonMax());
        if (buildero.isPresent()) lonaxisSubset = buildero.get(); else errMessages.format("lonaxis: %s;%n", buildero.getErrorMessage());
        buildero = lataxis.subset(llrect.getLatMin(), llrect.getLatMax());
        if (buildero.isPresent()) lataxisSubset = buildero.get(); else errMessages.format("lataxis: %s;%n", buildero.getErrorMessage());
      }
    }

    else if (llbb != null) {
      if (hasLatLon) {
        buildero = subsetLon(llbb);
        if (buildero.isPresent()) lonaxisSubset = buildero.get(); else errMessages.format("lonaxis: %s;%n", buildero.getErrorMessage());
        buildero = lataxis.subset(llbb.getLatMin(), llbb.getLatMax());
        if (buildero.isPresent()) lataxisSubset = buildero.get(); else errMessages.format("lataxis: %s;%n", buildero.getErrorMessage());
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
        buildero = xaxis.subset(prect.getMinX(), prect.getMaxX());
        if (buildero.isPresent()) xaxisSubset = (CoverageCoordAxis1D) buildero.get(); else errMessages.format("xaxis: %s;%n", buildero.getErrorMessage());
        buildero = yaxis.subset(prect.getMinY(), prect.getMaxY());
        if (buildero.isPresent()) yaxisSubset = (CoverageCoordAxis1D) buildero.get(); else errMessages.format("yaxis: %s;%n", buildero.getErrorMessage());
      }
    }

    String errs = errMessages.toString();
    if (errs.length() > 0)
      return Optional.empty(errs);

    // makes a copy of the axis
    if (xaxisSubset == null && xaxis != null) xaxisSubset = (CoverageCoordAxis1D) xaxis.copy();
    if (yaxisSubset == null && yaxis != null) yaxisSubset = (CoverageCoordAxis1D) yaxis.copy();
    if (lataxisSubset == null && lataxis != null) lataxisSubset = lataxis.copy();
    if (lonaxisSubset == null && lonaxis != null) lonaxisSubset = lonaxis.copy();

    return Optional.of(new HorizCoordSys(xaxisSubset, yaxisSubset, lataxisSubset, lonaxisSubset, transform));
  }

  public LatLonPoint getLatLon(int yindex, int xindex) {
    if (hasLatLon) {
      if (is2DlatLon) {
        double lat = ((LatLonAxis2D)lataxis).getCoord(yindex, xindex);
        double lon = ((LatLonAxis2D)lonaxis).getCoord(yindex, xindex);
        return new LatLonPointImpl(lat, lon);

      } else {
        double lat = ((CoverageCoordAxis1D)lataxis).getCoord(yindex);
        double lon = ((CoverageCoordAxis1D)lonaxis).getCoord(xindex);
        return new LatLonPointImpl(lat, lon);
      }
    }

    // xy, proj case
    double x = xaxis.getCoord(xindex);
    double y = yaxis.getCoord(xindex);
    ProjectionImpl proj = transform.getProjection();
    return proj.projToLatLon(x, y);

  }

  // heres where to deal with crossing seam ??
  private Optional<CoverageCoordAxis> subsetLon( LatLonRect llbb) {
    double wantMin = llbb.getLonMin();
    double wantMax = llbb.getLonMax();
    double wantMin360 = LatLonPointImpl.lonNormal360(wantMin);
    double wantMax360 = wantMin360 + llbb.getWidth();

    double axisMin = lonaxis.getStartValue();
    double axisMax = lonaxis.getEndValue();
    double axisMin360 = LatLonPointImpl.lonNormal360(lonaxis.getStartValue());
    double diff = axisMin360 - lonaxis.getStartValue();
    double axisMax360 = lonaxis.getEndValue() + diff;

    if (wantMin360 > axisMax360)
      return Optional.empty(String.format("longitude wantMin %f > haveMax %f", wantMin360, axisMax360));
    if (wantMax360 < axisMin360)
      return Optional.empty(String.format("longitude wantMax %f < haveMin %f", wantMin360, axisMin360));

    // if org intersects, use it
    if (intersect(wantMin, wantMax, axisMin, axisMax))
      return lonaxis.subset(wantMin, wantMax);

    // if shifted intersects, use it
    if (intersect(wantMin360, wantMax360, axisMin, axisMax))
      return lonaxis.subset(wantMin360, wantMax360);

    // otherwise not sure what cases are left
    return Optional.empty(String.format("longitude want [%f,%f] does not intesect have [%f,%f]", wantMin, axisMax, axisMin, axisMax));
  }

  private boolean intersect(double wantMin, double wantMax, double axisMin, double axisMax) {
    if (wantMin >= axisMin && wantMin <= axisMax)
      return true;
    if (wantMax >= axisMin && wantMax <= axisMax)
      return true;

    if (axisMin >= wantMin && axisMin <= wantMax)
      return true;
    if (axisMax >= wantMin && axisMax <= wantMax)
      return true;

    return false;
  }

  public List<RangeIterator> getRanges() {
    List<RangeIterator> result = new ArrayList<>();
    if (is2DlatLon) {
      return ((LatLonAxis2D) lataxis).getRanges();
    } else {
      result.add(getYAxis().getRange());
      result.add(getXAxis().getRange());
    }

    return result;
  }

  public CoverageCoordAxis getXAxis() {
    return (xaxis != null) ? xaxis : lonaxis;
  }

  public CoverageCoordAxis getYAxis() {
    return (yaxis != null) ? yaxis : lataxis;
  }

}
