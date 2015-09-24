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
 *
 */
package ucar.nc2.ft2.coverage;

import com.google.common.collect.Lists;
import net.jcip.annotations.Immutable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.ma2.*;
import ucar.nc2.util.Optional;
import ucar.unidata.geoloc.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Formatter;
import java.util.List;

/**
 * Horizontal CoordSys
 * <p>
 * 1) has x,y,proj (1D) isProjection
 * 2) lat,lon      (1D) isLatLon1D
 * 3) lat,lon (2D)      class HorizCoordSys2D
 * 4) has x,y,proj and lat,lon (2D) LOOK 2D not used ?
 * <p>
 * Must be exactly one in a CoverageDataset.
 *
 * @author caron
 * @since 7/11/2015
 */
@Immutable
public class HorizCoordSys {
  static private final Logger logger = LoggerFactory.getLogger(HorizCoordSys.class);

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
  protected final LatLonAxis2D lataxis2D, lonaxis2D;// used in HorizCoordSys2D, do we really need to keep these in HorizCoordSys ?
  private final CoverageTransform transform;
  private final boolean isProjection;
  private final boolean hasLatLon2D;              // may be true if isProjection

  protected HorizCoordSys(CoverageCoordAxis1D xaxis, CoverageCoordAxis1D yaxis, CoverageCoordAxis lataxis, CoverageCoordAxis lonaxis, CoverageTransform transform) {
    this.xaxis = xaxis;
    this.yaxis = yaxis;
    this.transform = transform;
    this.isProjection = (xaxis != null) && (yaxis != null) && (transform != null);
    boolean isLatLon1D = lataxis instanceof CoverageCoordAxis1D && lonaxis instanceof CoverageCoordAxis1D;
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

  public boolean getIsProjection() {
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
    LatLonPoint latlon = (LatLonPoint) params.get(SubsetParams.latlonPoint);
    Integer horizStride = (Integer) params.get(SubsetParams.horizStride);
    if (horizStride == null || horizStride < 1) horizStride = 1;

    CoverageCoordAxis1D xaxisSubset = null, yaxisSubset = null;
    CoverageCoordAxis lataxisSubset = null, lonaxisSubset = null;
    Optional<CoverageCoordAxis> opt;
    Optional<CoverageCoordAxisBuilder> optb;

    Formatter errMessages = new Formatter();
    try {
      if (latlon != null) { // overrides other horiz subset params
        if (isProjection) {
          if (xhelper == null) xhelper = new CoordAxisHelper(xaxis);
          if (yhelper == null) yhelper = new CoordAxisHelper(yaxis);

          // we have to transform latlon to projection coordinates
          ProjectionImpl proj = transform.getProjection();
          ProjectionPoint pp = proj.latLonToProj(latlon);
          optb = xhelper.subsetContaining(pp.getX());
          if (optb.isPresent()) xaxisSubset = new CoverageCoordAxis1D(optb.get());
          else errMessages.format("xaxis: %s;%n", optb.getErrorMessage());

          optb = yhelper.subsetContaining(pp.getY());
          if (optb.isPresent()) yaxisSubset = new CoverageCoordAxis1D(optb.get());
          else errMessages.format("yaxis: %s;%n", optb.getErrorMessage());

        } else {
          if (xhelper == null) xhelper = new CoordAxisHelper(lonaxis);
          if (yhelper == null) yhelper = new CoordAxisHelper(lataxis);

          double lonNormal = LatLonPointImpl.lonNormalFrom(latlon.getLongitude(), lonaxis.getStartValue());
          optb = xhelper.subsetContaining(lonNormal);
          if (optb.isPresent()) lonaxisSubset = new CoverageCoordAxis1D(optb.get());
          else errMessages.format("lonaxis: %s;%n", optb.getErrorMessage());

          optb = yhelper.subsetContaining(latlon.getLatitude());
          if (optb.isPresent()) lataxisSubset = new CoverageCoordAxis1D(optb.get());
          else errMessages.format("lataxis: %s;%n", optb.getErrorMessage());
        }

      } else if (projbb != null) {
        if (isProjection) {
          opt = xaxis.subset(projbb.getMinX(), projbb.getMaxX(), horizStride);
          if (opt.isPresent()) xaxisSubset = (CoverageCoordAxis1D) opt.get();
          else errMessages.format("xaxis: %s;%n", opt.getErrorMessage());

          opt = yaxis.subset(projbb.getMinY(), projbb.getMaxY(), horizStride);
          if (opt.isPresent()) yaxisSubset = (CoverageCoordAxis1D) opt.get();
          else errMessages.format("yaxis: %s;%n", opt.getErrorMessage());

        } /* else {  // WTF projbb on non Projection ?
          ProjectionImpl proj = transform.getProjection();
          LatLonRect llrect = proj.projToLatLonBB(projbb);
          opt = lonaxis.subset(llrect.getLonMin(), llrect.getLonMax(), horizStride);
          if (opt.isPresent()) lonaxisSubset = opt.get();
          else errMessages.format("lonaxis: %s;%n", opt.getErrorMessage());

          opt = lataxis.subset(llrect.getLatMin(), llrect.getLatMax(), horizStride);
          if (opt.isPresent()) lataxisSubset = opt.get();
          else errMessages.format("lataxis: %s;%n", opt.getErrorMessage());
        } */

      } else if (llbb != null) {

        if (isProjection) {
          // we have to transform latlon to projection coordinates
          ProjectionImpl proj = transform.getProjection();
          ProjectionRect prect = proj.latLonToProjBB(llbb); // allow projection to override
          opt = xaxis.subset(prect.getMinX(), prect.getMaxX(), horizStride);
          if (opt.isPresent()) xaxisSubset = (CoverageCoordAxis1D) opt.get();
          else errMessages.format("xaxis: %s;%n", opt.getErrorMessage());

          opt = yaxis.subset(prect.getMinY(), prect.getMaxY(), horizStride);
          if (opt.isPresent()) yaxisSubset = (CoverageCoordAxis1D) opt.get();
          else errMessages.format("yaxis: %s;%n", opt.getErrorMessage());

        } else {
          opt = subsetLon(llbb, horizStride);
          if (opt.isPresent()) lonaxisSubset = opt.get();
          else errMessages.format("lonaxis: %s;%n", opt.getErrorMessage());

          opt = lataxis.subset(llbb.getLatMin(), llbb.getLatMax(), horizStride);
          if (opt.isPresent()) lataxisSubset = opt.get();
          else errMessages.format("lataxis: %s;%n", opt.getErrorMessage());
        }

      } else if (horizStride > 1) { // no bounding box, just horiz stride
        if (isProjection) {
          opt = xaxis.subsetByIndex(xaxis.getRange().setStride(horizStride));
          if (opt.isPresent()) xaxisSubset = (CoverageCoordAxis1D) opt.get();
          else errMessages.format("xaxis: %s;%n", opt.getErrorMessage());

          opt = yaxis.subsetByIndex(yaxis.getRange().setStride(horizStride));
          if (opt.isPresent()) yaxisSubset = (CoverageCoordAxis1D) opt.get();
          else errMessages.format("yaxis: %s;%n", opt.getErrorMessage());

        } else {
          opt = lonaxis.subsetByIndex(lonaxis.getRange().setStride(horizStride));
          if (opt.isPresent()) lonaxisSubset = opt.get();
          else errMessages.format("lonaxis: %s;%n", opt.getErrorMessage());

          opt = lataxis.subsetByIndex(lataxis.getRange().setStride(horizStride));
          if (opt.isPresent()) lataxisSubset = opt.get();
          else errMessages.format("lataxis: %s;%n", opt.getErrorMessage());
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

  /*
    longitude subset, after normalizing to start
    draw a circle, representing longitude values from start to start + 360.
    all values are on this circle and are > start.
    put start at bottom of circle, end > start, data has values from start, counterclockwise to end.
    wantMin, wantMax can be anywhere, want goes from wantMin counterclockwise to wantMax.
    wantMin may be less than or greater than wantMax.

    cases:
      A. wantMin < wantMax
         1 wantMin, wantMax > end : empty
         2 wantMin, wantMax < end : [wantMin, wantMax]
         3 wantMin < end, wantMax > end : [wantMin, end]

      B. wantMin > wantMax
         1 wantMin, wantMax > end : all [start, end]
         2 wantMin, wantMax < end : 2 pieces: [wantMin, end] + [start, max]
         3 wantMin < end, wantMax > end : [wantMin, end]

   */

  // heres where to deal with crossing seam
  private Optional<CoverageCoordAxis> subsetLon(LatLonRect llbb, int stride) throws InvalidRangeException {
    double wantMin = LatLonPointImpl.lonNormalFrom(llbb.getLonMin(), lonaxis.getStartValue());
    double wantMax = LatLonPointImpl.lonNormalFrom(llbb.getLonMax(), lonaxis.getStartValue());
    double start = lonaxis.getStartValue();
    double end  = lonaxis.getEndValue();

    // use MAMath.MinMax as a container for two values, min and max
    List<MAMath.MinMax> lonIntvs = subsetLonIntervals(wantMin, wantMax, start, end);

    if (lonIntvs.size() == 0)
      return Optional.empty(String.format("longitude want [%f,%f] does not intersect lon axis [%f,%f]", wantMin, wantMax, start, end));

    if (lonIntvs.size() == 1) {
      MAMath.MinMax lonIntv = lonIntvs.get(0);
      return lonaxis.subset(lonIntv.min, lonIntv.max, stride);
    }

    // this is the seam crossing case
    return lonaxis.subsetByIntervals(lonIntvs, stride);
 }

 /*
  longitude subset, after normalizing to start
  draw a circle, representing longitude values from start to start + 360.
  all values are on this circle and are > start.
  put start at bottom of circle, end > start, data has values from start, counterclockwise to end.
  wantMin, wantMax can be anywhere, want goes from wantMin counterclockwise to wantMax.
  wantMin may be less than or greater than wantMax.

  cases:
    A. wantMin < wantMax
       1 wantMin, wantMax > end : empty
       2 wantMin, wantMax < end : [wantMin, wantMax]
       3 wantMin < end, wantMax > end : [wantMin, end]

    B. wantMin > wantMax
       1 wantMin, wantMax > end : all [start, end]
       2 wantMin, wantMax < end : 2 pieces: [wantMin, end] + [start, max]
       3 wantMin < end, wantMax > end : [wantMin, end]

 */
  private List<MAMath.MinMax> subsetLonIntervals(double wantMin, double wantMax, double start, double end) throws InvalidRangeException {
    if (wantMin <= wantMax) {
      if (wantMin > end && wantMax > end) // none A.1
        return Collections.EMPTY_LIST;

      if (wantMin < end && wantMax < end) // A.2
        return Lists.newArrayList(new MAMath.MinMax(wantMin, wantMax));

      if (wantMin < end && wantMax > end) // A.3
        return Lists.newArrayList(new MAMath.MinMax(wantMin, end));

    } else {
      if (wantMin > end && wantMax > end) // all B.1
        return Lists.newArrayList(new MAMath.MinMax(start, end));

      if (wantMin < end && wantMax < end) { // B.2
        return Lists.newArrayList(new MAMath.MinMax(wantMin, end), new MAMath.MinMax(start, wantMax));
      }
      if (wantMin < end && wantMax > end) // B.3
        return Lists.newArrayList(new MAMath.MinMax(wantMin, end));
    }

    // otherwise shouldnt get to this
    logger.error("longitude want [%f,%f] does not intersect axis [%f,%f]", wantMin, wantMax, start, end);
    return Collections.EMPTY_LIST;
  }

  ///////////////////////////

  /* heres where to deal with crossing seam ??
  private Optional<RangeIterator> subsetLonRanges(LatLonRect llbb, int stride) throws InvalidRangeException {
    double wantMin = LatLonPointImpl.lonNormalFrom(llbb.getLonMin(), lonaxis.getStartValue());
    double wantMax = LatLonPointImpl.lonNormalFrom(llbb.getLonMax(), lonaxis.getStartValue());

    double start = lonaxis.getStartValue();
    double end  = lonaxis.getEndValue();

    if (wantMin <= wantMax) {
      if (wantMin > end && wantMax > end)
        return Optional.empty(String.format("longitude wantMin %f > haveMax %f", wantMin, end));

      if (wantMin < end && wantMax < end)
        return xhelper.makeRange(wantMin, wantMax, stride);

      if (wantMin < end && wantMax > end)
        return xhelper.makeRange(wantMin, end, stride);

    } else {
      if (wantMin > end && wantMax > end)
        return Optional.of(lonaxis.getRange());

      if (wantMin < end && wantMax < end) {
        Optional<RangeIterator> opt1 = xhelper.makeRange(wantMin, end, stride);
        Optional<RangeIterator> opt2 = xhelper.makeRange(start, wantMax, stride);
        RangeComposite r = new RangeComposite("subsetLon", opt1.get(), opt2.get());
        return Optional.of(r);
      }
      if (wantMin < end && wantMax > end)
        return xhelper.makeRange(wantMin, end, stride);
    }

    // otherwise shouldnt get to this
    return Optional.empty(String.format("longitude want [%f,%f] does not intersect have [%f,%f]", wantMin, wantMax, start, end));
  }



  // heres where to deal with crossing seam ??
  private Optional<CoverageCoordAxis> subsetLonOld(LatLonRect llbb, int stride) throws InvalidRangeException {
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
  } */

  // return y, x range
  public List<RangeIterator> getRanges() {
    List<RangeIterator> result = new ArrayList<>();
    result.add(getYAxis().getRange());
    RangeIterator lonRange = getXAxis().getRangeIterator();
    if (lonRange == null) lonRange = getXAxis().getRange(); // clumsy
    result.add(lonRange);

    return result;
  }

  public CoverageCoordAxis1D getXAxis() {
    return (xaxis != null) ? xaxis : lonaxis;
  }

  public CoverageCoordAxis1D getYAxis() {
    return (yaxis != null) ? yaxis : lataxis;
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

  public static class CoordReturn {
    public int x, y;
    public double xcoord, ycoord;
  }

  public Optional<CoordReturn> findXYindexFromCoord(double x, double y) {
    CoordReturn result = new CoordReturn();
    if (isProjection) {
      if (xhelper == null) xhelper = new CoordAxisHelper(xaxis);
      if (yhelper == null) yhelper = new CoordAxisHelper(yaxis);
      result.x = xhelper.findCoordElement(x, false);
      result.y = yhelper.findCoordElement(y, false);

      if (result.x >= 0 && result.x < xaxis.getNcoords() && result.y >= 0 && result.y < yaxis.getNcoords()) {
        result.xcoord = xaxis.getCoord(result.x);
        result.ycoord = yaxis.getCoord(result.y);
        return Optional.of(result);
      } else {
        return Optional.empty("not in grid");
      }

    } else { // 1D lat lon case
      if (xhelper == null) xhelper = new CoordAxisHelper(lonaxis);
      if (yhelper == null) yhelper = new CoordAxisHelper(lataxis);
      double lon = LatLonPointImpl.lonNormalFrom(x, lonaxis.getStartValue());
      result.x = xhelper.findCoordElement(lon, false);
      result.y = yhelper.findCoordElement(y, false);

      if (result.x >= 0 && result.x < lonaxis.getNcoords() && result.y >= 0 && result.y < lataxis.getNcoords()) {
        result.xcoord = lonaxis.getCoord(result.x);
        result.ycoord = lataxis.getCoord(result.y);
        return Optional.of(result);
      } else {
        return Optional.empty("not in grid");
      }
    }
  }

}
