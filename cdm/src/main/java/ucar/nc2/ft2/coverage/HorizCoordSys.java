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
 * Horizontal CoordSys
 * <p>
 * 1) has x,y,proj (1D) isProjection
 * 2) lat,lon      (1D) isLatLon1D
 * 3) lat,lon (2D)      class HorizCoordSys2D
 * 4) has x,y,proj and lat,lon (2D) LOOK not used ?
 * <p>
 * Must be exactly one in a CoverageDataset.
 *
 * @author caron
 * @since 7/11/2015
 */
@Immutable
public class HorizCoordSys {

  public static HorizCoordSys factory(CoverageCoordAxis1D xaxis, CoverageCoordAxis1D yaxis, CoverageCoordAxis lataxis, CoverageCoordAxis lonaxis, CoverageTransform transform) {
    boolean isProjection = (xaxis != null) && (yaxis != null) && (transform != null);
    boolean hasLatLon = (lataxis != null) && (lonaxis != null);
    boolean has2DlatLon = lataxis instanceof LatLonAxis2D && lonaxis instanceof LatLonAxis2D;
    if (!isProjection && !hasLatLon) {
      throw new IllegalArgumentException("must have horiz coordinates (x,y,projection or lat,lon)");
    }

    if (!isProjection && has2DlatLon)
      return new HorizCoordSys2D((LatLonAxis2D) lataxis, (LatLonAxis2D) lonaxis);
    else
      return new HorizCoordSys(xaxis, yaxis, lataxis, lonaxis, transform);
  }

  private final CoverageCoordAxis1D xaxis, yaxis;
  private final CoverageCoordAxis1D lataxis, lonaxis;
  protected final LatLonAxis2D lataxis2D, lonaxis2D;// do we really need to keep these in HorizCoordSys ?
  private final CoverageTransform transform;
  private final boolean isProjection, isLatLon1D; // exactly one must be true
  private final boolean hasLatLon2D;              // may be true if isProjection

  protected HorizCoordSys(CoverageCoordAxis1D xaxis, CoverageCoordAxis1D yaxis, CoverageCoordAxis lataxis, CoverageCoordAxis lonaxis, CoverageTransform transform) {
    this.xaxis = xaxis;
    this.yaxis = yaxis;
    this.transform = transform;
    this.isProjection = (xaxis != null) && (yaxis != null) && (transform != null);
    this.isLatLon1D = lataxis instanceof CoverageCoordAxis1D && lonaxis instanceof CoverageCoordAxis1D;
    boolean hasLatLon2D = lataxis instanceof LatLonAxis2D && lonaxis instanceof LatLonAxis2D;
    assert isProjection || isLatLon1D || hasLatLon2D : "missing horiz coordinates (x,y,projection or lat,lon)";

    if (isProjection && hasLatLon2D) {
      boolean ok = true;
      if (!lataxis.getDependsOn().equalsIgnoreCase(lonaxis.getDependsOn())) ok = false;
      if (lataxis.getDependenceType() != CoverageCoordAxis.DependenceType.twoD) ok = false;
      if (lonaxis.getDependenceType() != CoverageCoordAxis.DependenceType.twoD) ok = false;
      String dependsOn = lataxis.getDependsOn();
      if (!dependsOn.contains(xaxis.getName())) ok = false;
      if (!dependsOn.contains(yaxis.getName())) ok = false;
      if (!ok) {
        hasLatLon2D = false;
      }
    }
    this.hasLatLon2D = hasLatLon2D;
    if (!isProjection && hasLatLon2D && !(this instanceof HorizCoordSys2D))
      System.out.printf("HEY%n");


      if (isLatLon1D) {
      this.lataxis = (CoverageCoordAxis1D) lataxis;
      this.lonaxis = (CoverageCoordAxis1D) lonaxis;
    } else {
      this.lataxis = null;
      this.lonaxis = null;
    }

    if (hasLatLon2D) {
      this.lataxis2D = (LatLonAxis2D) lataxis;
      this.lonaxis2D = (LatLonAxis2D) lonaxis;
    } else {
      this.lataxis2D = null;
      this.lonaxis2D = null;
    }
  }

  public String getName() {
    if (isProjection)
      return xaxis.getName() + " " + yaxis.getName() + " " + transform.getName();
    else
      return lataxis.getName() + " " + lonaxis.getName();
  }

  public boolean isProjection() {
    return isProjection;
  }

  public boolean isLatLon2D() {
    return false;
  }

  public List<CoverageCoordAxis> getCoordAxes() {
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

  /////////////////////////////////////////////////////////////////////////////////////

  public Optional<HorizCoordSys> subset(SubsetParams params) {

    LatLonRect llbb = (LatLonRect) params.get(SubsetParams.latlonBB);
    ProjectionRect projbb = (ProjectionRect) params.get(SubsetParams.projBB);
    Integer horizStride = (Integer) params.get(SubsetParams.horizStride);
    if (horizStride == null || horizStride < 1) horizStride = 1;

    CoverageCoordAxis1D xaxisSubset = null, yaxisSubset = null;
    CoverageCoordAxis lataxisSubset = null, lonaxisSubset = null;
    Optional<CoverageCoordAxis> buildero;

    Formatter errMessages = new Formatter();
    try {
      if (projbb != null) {
        if (isProjection) {
          buildero = xaxis.subset(projbb.getMinX(), projbb.getMaxX(), horizStride);
          if (buildero.isPresent()) xaxisSubset = (CoverageCoordAxis1D) buildero.get();
          else errMessages.format("xaxis: %s;%n", buildero.getErrorMessage());

          buildero = yaxis.subset(projbb.getMinY(), projbb.getMaxY(), horizStride);
          if (buildero.isPresent()) yaxisSubset = (CoverageCoordAxis1D) buildero.get();
          else errMessages.format("yaxis: %s;%n", buildero.getErrorMessage());

        } else {
          ProjectionImpl proj = transform.getProjection();
          LatLonRect llrect = proj.projToLatLonBB(projbb);
          buildero = lonaxis.subset(llrect.getLonMin(), llrect.getLonMax(), horizStride);
          if (buildero.isPresent()) lonaxisSubset = buildero.get();
          else errMessages.format("lonaxis: %s;%n", buildero.getErrorMessage());

          buildero = lataxis.subset(llrect.getLatMin(), llrect.getLatMax(), horizStride);
          if (buildero.isPresent()) lataxisSubset = buildero.get();
          else errMessages.format("lataxis: %s;%n", buildero.getErrorMessage());
        }

      } else if (llbb != null) {

        if (isProjection) {
          // we have to transform latlon to projection coordinates
          ProjectionImpl proj = transform.getProjection();
          ProjectionRect prect = proj.latLonToProjBB(llbb); // allow projection to override
          buildero = xaxis.subset(prect.getMinX(), prect.getMaxX(), horizStride);
          if (buildero.isPresent()) xaxisSubset = (CoverageCoordAxis1D) buildero.get();
          else errMessages.format("xaxis: %s;%n", buildero.getErrorMessage());

          buildero = yaxis.subset(prect.getMinY(), prect.getMaxY(), horizStride);
          if (buildero.isPresent()) yaxisSubset = (CoverageCoordAxis1D) buildero.get();
          else errMessages.format("yaxis: %s;%n", buildero.getErrorMessage());

        } else {
          buildero = subsetLon(llbb, horizStride);
          if (buildero.isPresent()) lonaxisSubset = buildero.get();
          else errMessages.format("lonaxis: %s;%n", buildero.getErrorMessage());

          buildero = lataxis.subset(llbb.getLatMin(), llbb.getLatMax(), horizStride);
          if (buildero.isPresent()) lataxisSubset = buildero.get();
          else errMessages.format("lataxis: %s;%n", buildero.getErrorMessage());
        }

      } else if (horizStride > 1) { // no bounding box, just horiz stride
        if (isProjection) {
          buildero = xaxis.subsetByIndex(xaxis.getRange().setStride(horizStride));
          if (buildero.isPresent()) xaxisSubset = (CoverageCoordAxis1D) buildero.get();
          else errMessages.format("xaxis: %s;%n", buildero.getErrorMessage());

          buildero = yaxis.subsetByIndex(yaxis.getRange().setStride(horizStride));
          if (buildero.isPresent()) yaxisSubset = (CoverageCoordAxis1D) buildero.get();
          else errMessages.format("yaxis: %s;%n", buildero.getErrorMessage());

        } else {
          buildero = lonaxis.subsetByIndex(lonaxis.getRange().setStride(horizStride));
          if (buildero.isPresent()) lonaxisSubset = buildero.get();
          else errMessages.format("lonaxis: %s;%n", buildero.getErrorMessage());

          buildero = lataxis.subsetByIndex(lataxis.getRange().setStride(horizStride));
          if (buildero.isPresent()) lataxisSubset = buildero.get();
          else errMessages.format("lataxis: %s;%n", buildero.getErrorMessage());
        }
      }
    } catch (InvalidRangeException e) {
      errMessages.format("%s;%n", e.getMessage());
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
    if (isProjection) {
      double x = xaxis.getCoord(xindex);
      double y = yaxis.getCoord(xindex);
      ProjectionImpl proj = transform.getProjection();
      return proj.projToLatLon(x, y);
    } else {
      double lat = lataxis.getCoord(yindex);
      double lon = lonaxis.getCoord(xindex);
      return new LatLonPointImpl(lat, lon);
    }
  }

  // heres where to deal with crossing seam ??
  private Optional<CoverageCoordAxis> subsetLon(LatLonRect llbb, int stride) throws InvalidRangeException {
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
      return lonaxis.subset(wantMin, wantMax, stride);

    // if shifted intersects, use it
    if (intersect(wantMin360, wantMax360, axisMin, axisMax))
      return lonaxis.subset(wantMin360, wantMax360, stride);

    // otherwise not sure what cases are left
    return Optional.empty(String.format("longitude want [%f,%f] does not intersect have [%f,%f]", wantMin, axisMax, axisMin, axisMax));
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

  // return y, x range
  public List<RangeIterator> getRanges() {
    List<RangeIterator> result = new ArrayList<>();
    result.add(getYAxis().getRange());
    result.add(getXAxis().getRange());

    return result;
  }

  public CoverageCoordAxis getXAxis() {
    CoverageCoordAxis result =  (xaxis != null) ? xaxis : lonaxis;
    if (result == null)
      System.out.printf("HEY%n");
    return result;
  }

  public CoverageCoordAxis getYAxis() {
    CoverageCoordAxis result =  (yaxis != null) ? yaxis : lataxis;
    if (result == null)
      System.out.printf("HEY%n");
    return result;
  }

  public LatLonAxis2D getLonAxis2D() {
    return lonaxis2D;
  }

  public LatLonAxis2D getLatAxis2D() {
    return lataxis2D;
  }

  // try to optimize by keeping the helper classes
  private CoordAxisHelper yhelper;
  private CoordAxisHelper xhelper;

  public boolean findXYindexFromCoord(double x, double y, int[] startIndex) {

    if (isProjection) {
      if (xhelper == null) xhelper = new CoordAxisHelper((CoverageCoordAxis1D) getXAxis());
      if (yhelper == null) yhelper = new CoordAxisHelper((CoverageCoordAxis1D) getYAxis());
      startIndex[0] = xhelper.findCoordElement(x, false);
      startIndex[1] = yhelper.findCoordElement(y, false);

    } else { // 1D lat lon case
      if (xhelper == null) xhelper = new CoordAxisHelper((CoverageCoordAxis1D) getXAxis());
      if (yhelper == null) yhelper = new CoordAxisHelper((CoverageCoordAxis1D) getYAxis());
      double lon = LatLonPointImpl.lonNormalFrom(x, lonaxis.getStartValue());
      startIndex[0] = xhelper.findCoordElement(lon, false);
      startIndex[1] = yhelper.findCoordElement(y, false);
    }

    return startIndex[0] >= 0 && startIndex[0] < getXAxis().getNcoords() && startIndex[1] >= 0 && startIndex[1] < getYAxis().getNcoords();
  }

}
