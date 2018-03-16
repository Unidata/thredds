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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.ma2.*;
import ucar.nc2.util.Optional;
import ucar.unidata.geoloc.*;

import javax.annotation.concurrent.Immutable;
import java.util.*;

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
// TODO: This class is trying to do too much: it's handling projection, latLon1D AND latLon2D CRSs. We should break
// some of the code into HorizCoordSysProj, HorizCoordSysLatlon1D, and HorizCoordSysLatLon2D subclasses. HorizCoordSys
// might then become an interface or abstract class with common method declarations/definitions.
@Immutable
public class HorizCoordSys {
  static private final Logger logger = LoggerFactory.getLogger(HorizCoordSys.class);

  public static HorizCoordSys factory(CoverageCoordAxis1D xAxis, CoverageCoordAxis1D yAxis, CoverageCoordAxis latAxis,
          CoverageCoordAxis lonAxis, CoverageTransform transform) {
    boolean isProjection = (xAxis != null) && (yAxis != null) && (transform != null);
    boolean hasLatLon = (latAxis != null) && (lonAxis != null);
    boolean has2DlatLon = latAxis instanceof LatLonAxis2D && lonAxis instanceof LatLonAxis2D;
    if (!isProjection && !hasLatLon) {
      throw new IllegalArgumentException("must have horiz coordinates (x,y,projection or lat,lon)");
    }

    if (!isProjection && has2DlatLon)
      return new HorizCoordSys2D((LatLonAxis2D) latAxis, (LatLonAxis2D) lonAxis);
    else
      return new HorizCoordSys(xAxis, yAxis, latAxis, lonAxis, transform);
  }

  private final CoverageCoordAxis1D xAxis, yAxis;
  private final CoverageCoordAxis1D latAxis, lonAxis;
  // used in HorizCoordSys2D, do we really need to keep these in HorizCoordSys?
  protected final LatLonAxis2D latAxis2D, lonAxis2D;
  private final CoverageTransform transform;
  private final boolean isProjection;
  private final boolean isLatLon1D;
  private boolean isLatLon2D;  // isProjection and isLatLon2D may both be "true".

  protected HorizCoordSys(CoverageCoordAxis1D xAxis, CoverageCoordAxis1D yAxis, CoverageCoordAxis latAxis,
          CoverageCoordAxis lonAxis, CoverageTransform transform) {
    this.xAxis = xAxis;
    this.yAxis = yAxis;
    this.transform = transform;
    this.isProjection = (xAxis != null) && (yAxis != null) && (transform != null);
    this.isLatLon1D = latAxis instanceof CoverageCoordAxis1D && lonAxis instanceof CoverageCoordAxis1D;
    this.isLatLon2D = latAxis instanceof LatLonAxis2D && lonAxis instanceof LatLonAxis2D;
    assert isProjection || isLatLon1D || isLatLon2D : "missing horiz coordinates (x,y,projection or lat,lon)";

    if (isProjection && isLatLon2D) {
      boolean ok = true;
      if (!latAxis.getDependsOn().equalsIgnoreCase(lonAxis.getDependsOn())) ok = false;
      if (latAxis.getDependenceType() != CoverageCoordAxis.DependenceType.twoD) ok = false;
      if (lonAxis.getDependenceType() != CoverageCoordAxis.DependenceType.twoD) ok = false;
      String dependsOn = latAxis.getDependsOn();
      if (!dependsOn.contains(xAxis.getName())) ok = false;
      if (!dependsOn.contains(yAxis.getName())) ok = false;
      if (!ok) {
        isLatLon2D = false;
      }
    }

    if (!isProjection && isLatLon2D && !(this instanceof HorizCoordSys2D))
      System.out.printf("HEY Should be HorizCoordSys2D%n");

    if (isLatLon1D) {
      this.latAxis = (CoverageCoordAxis1D) latAxis;
      this.lonAxis = (CoverageCoordAxis1D) lonAxis;
    } else {
      this.latAxis = null;
      this.lonAxis = null;
    }

    if (isLatLon2D) {
      this.latAxis2D = (LatLonAxis2D) latAxis;
      this.lonAxis2D = (LatLonAxis2D) lonAxis;
    } else {
      this.latAxis2D = null;
      this.lonAxis2D = null;
    }
  }

  public String getName() {
    if (isProjection)
      return xAxis.getName() + " " + yAxis.getName() + " " + transform.getName();
    else
      return latAxis.getName() + " " + lonAxis.getName();
  }

  public boolean isProjection() {
    return isProjection;
  }

  public boolean isLatLon2D() {
    return false;
  }

  public List<CoverageCoordAxis> getCoordAxes() {
    List<CoverageCoordAxis> result = new ArrayList<>();
    if (xAxis != null) result.add(xAxis);
    if (yAxis != null) result.add(yAxis);
    if (latAxis != null) result.add(latAxis);
    if (lonAxis != null) result.add(lonAxis);
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
          CoordAxisHelper xhelper = new CoordAxisHelper(xAxis);
          CoordAxisHelper yhelper = new CoordAxisHelper(yAxis);

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
          CoordAxisHelper xhelper = new CoordAxisHelper(lonAxis);
          CoordAxisHelper yhelper = new CoordAxisHelper(latAxis);

          double lonNormal = LatLonPointImpl.lonNormalFrom(latlon.getLongitude(), lonAxis.getStartValue());
          optb = xhelper.subsetContaining(lonNormal);
          if (optb.isPresent()) lonaxisSubset = new CoverageCoordAxis1D(optb.get());
          else errMessages.format("lonaxis: %s;%n", optb.getErrorMessage());

          optb = yhelper.subsetContaining(latlon.getLatitude());
          if (optb.isPresent()) lataxisSubset = new CoverageCoordAxis1D(optb.get());
          else errMessages.format("lataxis: %s;%n", optb.getErrorMessage());
        }

      } else if (projbb != null) {
        if (isProjection) {
          opt = xAxis.subset(projbb.getMinX(), projbb.getMaxX(), horizStride);
          if (opt.isPresent()) xaxisSubset = (CoverageCoordAxis1D) opt.get();
          else errMessages.format("xaxis: %s;%n", opt.getErrorMessage());

          opt = yAxis.subset(projbb.getMinY(), projbb.getMaxY(), horizStride);
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
        LatLonRect full = calcLatLonBoundingBox();
        assert full != null;

        if (!full.containedIn(llbb)) { // if request contains entire bb, then no subsetting needed

          if (isProjection) {
            // we have to transform latlon to projection coordinates
            ProjectionImpl proj = transform.getProjection();
            ProjectionRect prect = proj.latLonToProjBB(llbb); // allow projection to override
            opt = xAxis.subset(prect.getMinX(), prect.getMaxX(), horizStride);
            if (opt.isPresent()) xaxisSubset = (CoverageCoordAxis1D) opt.get();
            else errMessages.format("xaxis: %s;%n", opt.getErrorMessage());

            opt = yAxis.subset(prect.getMinY(), prect.getMaxY(), horizStride);
            if (opt.isPresent()) yaxisSubset = (CoverageCoordAxis1D) opt.get();
            else errMessages.format("yaxis: %s;%n", opt.getErrorMessage());

          } else {
            opt = subsetLon(llbb, horizStride);
            if (opt.isPresent()) lonaxisSubset = opt.get();
            else errMessages.format("lonaxis: %s;%n", opt.getErrorMessage());

            opt = latAxis.subset(llbb.getLatMin(), llbb.getLatMax(), horizStride);
            if (opt.isPresent()) lataxisSubset = opt.get();
            else errMessages.format("lataxis: %s;%n", opt.getErrorMessage());
          }
        }

      } else if (horizStride > 1) { // no bounding box, just horiz stride
        if (isProjection) {
          opt = xAxis.subsetByIndex(xAxis.getRange().setStride(horizStride));
          if (opt.isPresent()) xaxisSubset = (CoverageCoordAxis1D) opt.get();
          else errMessages.format("xaxis: %s;%n", opt.getErrorMessage());

          opt = yAxis.subsetByIndex(yAxis.getRange().setStride(horizStride));
          if (opt.isPresent()) yaxisSubset = (CoverageCoordAxis1D) opt.get();
          else errMessages.format("yaxis: %s;%n", opt.getErrorMessage());

        } else {
          opt = lonAxis.subsetByIndex(lonAxis.getRange().setStride(horizStride));
          if (opt.isPresent()) lonaxisSubset = opt.get();
          else errMessages.format("lonaxis: %s;%n", opt.getErrorMessage());

          opt = latAxis.subsetByIndex(latAxis.getRange().setStride(horizStride));
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
    if (xaxisSubset == null && xAxis != null) xaxisSubset = (CoverageCoordAxis1D) xAxis.copy();
    if (yaxisSubset == null && yAxis != null) yaxisSubset = (CoverageCoordAxis1D) yAxis.copy();
    if (lataxisSubset == null && latAxis != null) lataxisSubset = latAxis.copy();
    if (lonaxisSubset == null && lonAxis != null) lonaxisSubset = lonAxis.copy();

    return Optional.of(new HorizCoordSys(xaxisSubset, yaxisSubset, lataxisSubset, lonaxisSubset, transform));
  }

  public LatLonPoint getLatLon(int yindex, int xindex) {
    if (isProjection) {
      double x = xAxis.getCoordMidpoint(xindex);
      double y = yAxis.getCoordMidpoint(xindex);
      ProjectionImpl proj = transform.getProjection();
      return proj.projToLatLon(x, y);
    } else {
      double lat = latAxis.getCoordMidpoint(yindex);
      double lon = lonAxis.getCoordMidpoint(xindex);
      return new LatLonPointImpl(lat, lon);
    }
  }

  // here's where to deal with crossing seam
  private Optional<CoverageCoordAxis> subsetLon(LatLonRect llbb, int stride) throws InvalidRangeException {
    double wantMin = LatLonPointImpl.lonNormalFrom(llbb.getLonMin(), lonAxis.getStartValue());
    double wantMax = LatLonPointImpl.lonNormalFrom(llbb.getLonMax(), lonAxis.getStartValue());
    double start = lonAxis.getStartValue();
    double end  = lonAxis.getEndValue();

    // use MAMath.MinMax as a container for two values, min and max
    List<MAMath.MinMax> lonIntvs = subsetLonIntervals(wantMin, wantMax, start, end);

    if (lonIntvs.size() == 0)
      return Optional.empty(String.format(
              "longitude want [%f,%f] does not intersect lon axis [%f,%f]", wantMin, wantMax, start, end));

    if (lonIntvs.size() == 1) {
      MAMath.MinMax lonIntv = lonIntvs.get(0);
      return lonAxis.subset(lonIntv.min, lonIntv.max, stride);
    }

    // this is the seam crossing case
    return lonAxis.subsetByIntervals(lonIntvs, stride);
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
  private List<MAMath.MinMax> subsetLonIntervals(double wantMin, double wantMax, double start, double end) {
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
    return (xAxis != null) ? xAxis : lonAxis;
  }

  public CoverageCoordAxis1D getYAxis() {
    return (yAxis != null) ? yAxis : latAxis;
  }

  public LatLonAxis2D getLonAxis2D() {
    return lonAxis2D;
  }

  public LatLonAxis2D getLatAxis2D() {
    return latAxis2D;
  }

  public static class CoordReturn {
    public int x, y;
    public double xcoord, ycoord;
  }

  public Optional<CoordReturn> findXYindexFromCoord(double x, double y) {
    CoordReturn result = new CoordReturn();
    if (isProjection) {
      CoordAxisHelper xhelper = new CoordAxisHelper(xAxis);
      CoordAxisHelper yhelper = new CoordAxisHelper(yAxis);
      result.x = xhelper.findCoordElement(x, false);
      result.y = yhelper.findCoordElement(y, false);

      if (result.x >= 0 && result.x < xAxis.getNcoords() && result.y >= 0 && result.y < yAxis.getNcoords()) {
        result.xcoord = xAxis.getCoordMidpoint(result.x);
        result.ycoord = yAxis.getCoordMidpoint(result.y);
        return Optional.of(result);
      } else {
        return Optional.empty("not in grid");
      }

    } else { // 1D lat lon case
      CoordAxisHelper xhelper = new CoordAxisHelper(lonAxis);
      CoordAxisHelper yhelper = new CoordAxisHelper(latAxis);
      double lon = LatLonPointImpl.lonNormalFrom(x, lonAxis.getStartValue());
      result.x = xhelper.findCoordElement(lon, false);
      result.y = yhelper.findCoordElement(y, false);

      if (result.x >= 0 && result.x < lonAxis.getNcoords() && result.y >= 0 && result.y < latAxis.getNcoords()) {
        result.xcoord = lonAxis.getCoordMidpoint(result.x);
        result.ycoord = latAxis.getCoordMidpoint(result.y);
        return Optional.of(result);
      } else {
        return Optional.empty("not in grid");
      }
    }
  }

  ///////////////////////////////////// Boundary calculations /////////////////////////////////////

  /**
   * Calculates the bounding box of this coordinate reference system, in projection coordinates. If this CRS
   * {@link #isProjection isn't a projection}, than {@code null} is returned.
   *
   * @return  the bounding box of this CRS, in projection coordinates.
   */
  public ProjectionRect calcProjectionBoundingBox() {
    if (!isProjection) return null;
    double minX = Math.min(xAxis.getCoordEdgeFirst(), xAxis.getCoordEdgeLast());
    double minY = Math.min(yAxis.getCoordEdgeFirst(), yAxis.getCoordEdgeLast());
    double width = Math.abs(xAxis.getCoordEdgeLast() - xAxis.getCoordEdgeFirst());
    double height = Math.abs(yAxis.getCoordEdgeLast() - yAxis.getCoordEdgeFirst());
    return new ProjectionRect(new ProjectionPointImpl(minX, minY), width, height);
  }

  /**
   * Calculates the bounding box of this coordinate reference system, in latitude/longitude. This method properly
   * handles coverages that straddle the international date line by deriving its bounding box from the
   * {@link #calcConnectedLatLonBoundaryPoints(int, int) connected latitude/longitude boundary}.
   * <p>
   * If this CRS {@link #isProjection is a projection}, its lat/lon boundary is computed by converting each point
   * in its {@link #calcProjectionBoundaryPoints() projection boundary} to latitude/longitude using the
   * {@link Projection projection}.
   *
   * @return  the bounding box of this CRS, in latitude/longitude.
   */
  public LatLonRect calcLatLonBoundingBox() {
    double minLat = Double.MAX_VALUE;
    double minLon = Double.MAX_VALUE;
    double maxLat = -Double.MAX_VALUE;
    double maxLon = -Double.MAX_VALUE;

    for (LatLonPointNoNormalize boundaryPoint : calcConnectedLatLonBoundaryPoints()) {
      minLat = Math.min(minLat, boundaryPoint.getLatitude());
      minLon = Math.min(minLon, boundaryPoint.getLongitude());
      maxLat = Math.max(maxLat, boundaryPoint.getLatitude());
      maxLon = Math.max(maxLon, boundaryPoint.getLongitude());
    }

    return new LatLonRect(new LatLonPointImpl(minLat, minLon), new LatLonPointImpl(maxLat, maxLon));
  }

  /**
   * Calls {@link #calcConnectedLatLonBoundaryPoints(int, int)} with {@link Integer#MAX_VALUE} as both arguments.
   * In effect, the boundary will contain ALL of the points along the edges of the CRS.
   *
   * @return  the connected latitude/longitude boundary of this CRS.
   */
  public List<LatLonPointNoNormalize> calcConnectedLatLonBoundaryPoints() {
    return calcConnectedLatLonBoundaryPoints(Integer.MAX_VALUE, Integer.MAX_VALUE);
  }

  /**
   * Calculates the latitude/longitude boundary of this coordinate reference system. The boundary starts at the lower
   * left corner of the coverage--i.e. {@code (y[0], x[0])}--and consists of the points that lie along the bottom,
   * right, top, and left edges, in that order.
   * <p>
   * The {@code maxPointsInYEdge} parameter establishes a limit on the number of boundary points that'll be included
   * from the right and left edges. {@code maxPointsInXEdge} establishes a similar limit for the bottom and top edges.
   * The size of the returned list will be {@code ≤ 2 * maxPointsInYEdge + 2 * maxPointsInXEdge}. Note that the corners
   * are always included, regardless of the arguments. If you wish to include ALL of the points along the edges in
   * the boundary, simply choose values for the parameters that are greater than the lengths of the corresponding
   * axes in the CRS. {@link Integer#MAX_VALUE} works great. In that case, the size of the returned list will be
   * {@code 2 * numXcoords + 2 * numYcoords}.
   * <p>
   * If this CRS {@link #isProjection is a projection}, the lat/lon boundary is computed by converting each point
   * in its {@link #calcProjectionBoundaryPoints() projection boundary} to latitude/longitude using the
   * {@link Projection projection}.
   * <p>
   * Points in the boundary will be {@link #connectLatLonPoints connected}. This facilitates proper interpretation of
   * the boundary if it's rendered as a georeferenced polygon, particularly when the boundary crosses the international
   * date line.
   *
   * @param maxPointsInYEdge  the maximum number of boundary points to include from the right and left edges.
   * @param maxPointsInXEdge  the maximum number of boundary points to include from the bottom and top edges.
   * @return  the connected latitude/longitude boundary of this CRS.
   */
  public List<LatLonPointNoNormalize> calcConnectedLatLonBoundaryPoints(int maxPointsInYEdge, int maxPointsInXEdge) {
    List<LatLonPoint> points;

    if (isProjection) {
      points = calcLatLonBoundaryPointsFromProjection(maxPointsInYEdge, maxPointsInXEdge);
    } else if (isLatLon1D) {
      points = calcLatLon1DBoundaryPoints(maxPointsInYEdge, maxPointsInXEdge);
    } else if (isLatLon2D) {
      points = calcLatLon2DBoundaryPoints(maxPointsInYEdge, maxPointsInXEdge);
    } else {
      throw new AssertionError("HorizCoordSys was not a projection, latLon1D, or latLon2D.");
    }

    return connectLatLonPoints(points);
  }

  /**
   * Calls {@link #calcProjectionBoundaryPoints(int, int)} with {@link Integer#MAX_VALUE} as both arguments.
   * In effect, the boundary will contain ALL of the points along the edges of the CRS.
   *
   * @return  the boundary of this coordinate reference system, in projection coordinates.
   */
  public List<ProjectionPoint> calcProjectionBoundaryPoints() {
    return calcProjectionBoundaryPoints(Integer.MAX_VALUE, Integer.MAX_VALUE);
  }

  /**
   * Calculates the boundary of this coordinate reference system, in projection coordinates. The boundary starts at the
   * lower left corner of the coverage--i.e. {@code (y[0], x[0])}--and consists of the points that lie along the bottom,
   * right, top, and left edges, in that order.
   * <p>
   * The {@code maxPointsInYEdge} parameter establishes a limit on the number of boundary points that'll be included
   * from the right and left edges. {@code maxPointsInXEdge} establishes a similar limit for the bottom and top edges.
   * The size of the returned list will be {@code ≤ 2 * maxPointsInYEdge + 2 * maxPointsInXEdge}. Note that the corners
   * are always included, regardless of the arguments. If you wish to include ALL of the points along the edges in
   * the boundary, simply choose values for the parameters that are greater than the lengths of the corresponding
   * axes in the CRS. {@link Integer#MAX_VALUE} works great. In that case, the size of the returned list will be
   * {@code 2 * numXcoords + 2 * numYcoords}.
   *
   * @param maxPointsInYEdge  the maximum number of boundary points to include from the right and left edges.
   * @param maxPointsInXEdge  the maximum number of boundary points to include from the bottom and top edges.
   * @return  the boundary of this coordinate reference system, in projection coordinates.
   * @throws UnsupportedOperationException  if this CRS is not a projection.
   */
  public List<ProjectionPoint> calcProjectionBoundaryPoints(int maxPointsInYEdge, int maxPointsInXEdge) {
    if (!isProjection) {
      throw new UnsupportedOperationException("Coordinate system is not a projection.");
    }
    checkMaxPointsInEdges(maxPointsInYEdge, maxPointsInXEdge);

    int numYtotal = yAxis.getNcoords();
    int numXtotal = xAxis.getNcoords();
    int strideY = calcStride(numYtotal, maxPointsInYEdge);
    int strideX = calcStride(numXtotal, maxPointsInXEdge);

    List<ProjectionPoint> points = new LinkedList<>();

    // Bottom boundary points
    for (int i = 0; i < numXtotal; i += strideX) {
      points.add(new ProjectionPointImpl(xAxis.getCoordEdge1(i), yAxis.getCoordEdgeFirst()));
    }

    // Right boundary points
    for (int j = 0; j < numYtotal; j += strideY) {
      points.add(new ProjectionPointImpl(xAxis.getCoordEdgeLast(), yAxis.getCoordEdge1(j)));
    }

    // Top boundary points
    for (int i = numXtotal - 1; i >= 0; i -= strideX) {
      points.add(new ProjectionPointImpl(xAxis.getCoordEdge2(i), yAxis.getCoordEdgeLast()));
    }

    // Left boundary points
    for (int j = numYtotal - 1; j >= 0; j -= strideY) {
      points.add(new ProjectionPointImpl(xAxis.getCoordEdgeFirst(), yAxis.getCoordEdge2(j)));
    }

    assertNotExceedingMaxBoundaryPoints(points.size(), maxPointsInYEdge, maxPointsInXEdge);
    return points;
  }

  private List<LatLonPoint> calcLatLon1DBoundaryPoints(int maxPointsInYEdge, int maxPointsInXEdge) {
    if (!isLatLon1D) {
      throw new UnsupportedOperationException("Coordinate system is not 1D latitude/longitude.");
    }
    checkMaxPointsInEdges(maxPointsInYEdge, maxPointsInXEdge);

    int numYtotal = latAxis.getNcoords();
    int numXtotal = lonAxis.getNcoords();
    int strideY = calcStride(numYtotal, maxPointsInYEdge);
    int strideX = calcStride(numXtotal, maxPointsInXEdge);

    List<LatLonPoint> points = new LinkedList<>();

    // Bottom boundary points
    for (int i = 0; i < numXtotal; i += strideX) {
      points.add(new LatLonPointImpl(latAxis.getCoordEdgeFirst(), lonAxis.getCoordEdge1(i)));
    }

    // Right boundary points
    for (int j = 0; j < numYtotal; j += strideY) {
      points.add(new LatLonPointImpl(latAxis.getCoordEdge1(j), lonAxis.getCoordEdgeLast()));
    }

    // Top boundary points
    for (int i = numXtotal - 1; i >= 0; i -= strideX) {
      points.add(new LatLonPointImpl(latAxis.getCoordEdgeLast(), lonAxis.getCoordEdge2(i)));
    }

    // Left boundary points
    for (int j = numYtotal - 1; j >= 0; j -= strideY) {
      points.add(new LatLonPointImpl(latAxis.getCoordEdge2(j), lonAxis.getCoordEdgeFirst()));
    }

    assertNotExceedingMaxBoundaryPoints(points.size(), maxPointsInYEdge, maxPointsInXEdge);
    return points;
  }

  private List<LatLonPoint> calcLatLonBoundaryPointsFromProjection(int maxPointsInYEdge, int maxPointsInXEdge) {
    List<ProjectionPoint> projPoints = calcProjectionBoundaryPoints(maxPointsInYEdge, maxPointsInXEdge);
    List<LatLonPoint> latLonPoints = new LinkedList<>();

    for (ProjectionPoint projPoint : projPoints) {
      latLonPoints.add(transform.getProjection().projToLatLon(projPoint));
    }

    return latLonPoints;
  }

  private List<LatLonPoint> calcLatLon2DBoundaryPoints(int maxPointsInYEdge, int maxPointsInXEdge) {
    if (!isLatLon2D) {
      throw new UnsupportedOperationException("Coordinate system is not 2D latitude/longitude.");
    }
    checkMaxPointsInEdges(maxPointsInYEdge, maxPointsInXEdge);

    assert Arrays.equals(latAxis2D.getShape(), lonAxis2D.getShape()) : "2D lat/lon axes ought to have the same shape";
    int[] midpointsShape = latAxis2D.getShape().clone();  // Clone because we will modify later.

    int numYtotal = midpointsShape[0];
    int numXtotal = midpointsShape[1];
    int strideY = calcStride(numYtotal, maxPointsInYEdge);
    int strideX = calcStride(numXtotal, maxPointsInXEdge);

    ArrayDouble.D2 latEdges = (ArrayDouble.D2) latAxis2D.getCoordBoundsAsArray();
    ArrayDouble.D2 lonEdges = (ArrayDouble.D2) lonAxis2D.getCoordBoundsAsArray();

    assert Arrays.equals(latEdges.getShape(), lonEdges.getShape()) : "2D lat/lon edges ought to have the same shape";
    int[] edgesShape = latEdges.getShape();
    midpointsShape[0]++;
    midpointsShape[1]++;
    assert Arrays.equals(midpointsShape, edgesShape) : "edgesShape should be 1 greater than midpointsShape in each dim";

    List<LatLonPoint> points = new LinkedList<>();

    // Bottom boundary points: y = 0
    for (int i = 0; i < numXtotal; i += strideX) {
      points.add(new LatLonPointImpl(latEdges.get(0, i), lonEdges.get(0, i)));
    }

    // Right boundary points: x = numXtotal
    // numXtotal is not OOB, because edgesShape is 1 bigger than midpointsShape in each dim, and numXtotal
    // is relative to midpointsShape.
    for (int j = 0; j < numYtotal; j += strideY) {
      points.add(new LatLonPointImpl(latEdges.get(j, numXtotal), lonEdges.get(j, numXtotal)));
    }

    // Top boundary points: y = numYtotal
    for (int i = numXtotal; i > 0; i -= strideX) {
      points.add(new LatLonPointImpl(latEdges.get(numYtotal, i), lonEdges.get(numYtotal, i)));
    }

    // Left boundary points: x = 0
    for (int j = numYtotal; j > 0; j -= strideY) {
      points.add(new LatLonPointImpl(latEdges.get(j, 0), lonEdges.get(j, 0)));
    }

    assertNotExceedingMaxBoundaryPoints(points.size(), maxPointsInYEdge, maxPointsInXEdge);
    return points;
  }

  private static void checkMaxPointsInEdges(int maxPointsInYEdge, int maxPointsInXEdge)
          throws IllegalArgumentException {
    if (maxPointsInYEdge < 1) {
      throw new IllegalArgumentException(String.format("maxPointsInYEdge (%d) must be > 0", maxPointsInYEdge));
    } else if (maxPointsInXEdge < 1) {
      throw new IllegalArgumentException(String.format("maxPointsInXEdge (%d) must be > 0", maxPointsInXEdge));
    }
  }

  private static int calcStride(int numTotal, int maxToInclude) {
    int stride = Math.max(1, (int) Math.ceil(numTotal / (double) maxToInclude));
    int numIncluded = (int) Math.ceil(numTotal / (double) stride);

    assert numIncluded <= maxToInclude : String.format(
            "We're set to include %d points, but we wanted a max of %d.", numIncluded, maxToInclude);
    return stride;
  }

  private static void assertNotExceedingMaxBoundaryPoints(
        int numBoundaryPoints, int maxPointsInYEdge, int maxPointsInXEdge) {
    // Widen to long to avoid possible overflow, because last two arguments will often be Integer.MAX_VALUE.
    long maxBoundaryPoints = 2 * (long) maxPointsInYEdge + 2 * (long) maxPointsInXEdge;

    assert numBoundaryPoints <= maxBoundaryPoints :
        String.format("We should be returning a maximum of %d boundary points, but we're returning %d instead.",
            maxBoundaryPoints, numBoundaryPoints);
  }

  /**
   * Returns a list of points that is equivalent to the input list, but with longitude values adjusted to ensure that
   * adjacent elements are "connected".
   * <p>
   * Two points are "connected" if the absolute difference of their {@link LatLonPointImpl#lonNormal normalized
   * longitudes} is {@code ≤180}. For example, the longitudes {@code 112} and {@code 124} are connected. So are
   * {@code 15} and {@code -27}.
   * <p>
   * Two points may be "disconnected" if they lie on opposite sides of the international date line. For example,
   * the longitudes {@code 175} and {@code -175} are disconnected because their absolute difference is {@code 350},
   * which is {@code >180}. To connect the two points, we adjust the second longitude to an equivalent value
   * in the range {@code [firstLon ± 180]} by adding or subtracting {@code 360}. So, {@code -175} would become
   * {@code 185}. We perform this adjustment for each pair of adjacent elements in the list.
   * <p>
   * Performing the above adjustment will result in longitudes that lie outside of the normalized range of
   * ({@code [-180, 180]}). To be precise, if adjustments are necessary, all of the longitudes in the returned list
   * will be in either {@code [-360, 0]} or {@code [0, 360]}. Consequently, adjusted points cannot be returned as
   * {@link LatLonPoint}s; they are returned as {@link LatLonPointNoNormalize} objects instead.
   * <p>
   * Longitudes {@code lon1} and {@code lon2} are considered equivalent if {@code lon1 == lon2 + 360 * i}, for some
   * integer {@code i}.
   *
   * @param points  a sequence of normalized lat/lon points that potentially crosses the international date line.
   * @return  an equivalent sequence of points that has been adjusted to be "connected".
   */
  public static List<LatLonPointNoNormalize> connectLatLonPoints(List<LatLonPoint> points) {
    LinkedList<LatLonPointNoNormalize> connectedPoints = new LinkedList<>();

    for (LatLonPoint point : points) {
      double curLat = point.getLatitude();
      double curLon = point.getLongitude();

      if (!connectedPoints.isEmpty()) {
        double prevLon = connectedPoints.getLast().getLongitude();
        curLon = LatLonPointImpl.lonNormal(curLon, prevLon);
      }
      connectedPoints.add(new LatLonPointNoNormalize(curLat, curLon));
    }

    return connectedPoints;
  }



  /**
   * Calls {@link #getLatLonBoundaryAsWKT(int, int)} with {@link Integer#MAX_VALUE} as both arguments.
   * In effect, the boundary will contain ALL of the points along the edges of the CRS.
   *
   * @return  the latitude/longitude boundary of this CRS as a polygon in WKT.
   */
  public String getLatLonBoundaryAsWKT() {
    return getLatLonBoundaryAsWKT(Integer.MAX_VALUE, Integer.MAX_VALUE);
  }

  /**
   * Returns the {@link #calcConnectedLatLonBoundaryPoints(int, int) latitude/longitude boundary} of this coordinate
   * reference system as a polygon in WKT. It is used in the OpenLayers map in NCSS, as well as the
   * "datasetBoundaries" endpoint.
   *
   * @param maxPointsInYEdge  the maximum number of boundary points to include from the right and left edges.
   * @param maxPointsInXEdge  the maximum number of boundary points to include from the bottom and top edges.
   * @return  the latitude/longitude boundary of this CRS as a polygon in WKT.
   */
  public String getLatLonBoundaryAsWKT(int maxPointsInYEdge, int maxPointsInXEdge) {
    List<LatLonPointNoNormalize> points = calcConnectedLatLonBoundaryPoints(maxPointsInYEdge, maxPointsInXEdge);
    StringBuilder sb = new StringBuilder("POLYGON((");

    for (LatLonPointNoNormalize point : points) {
      sb.append(String.format("%.3f %.3f, ", point.getLongitude(), point.getLatitude()));
    }

    sb.delete(sb.length() - 2, sb.length());  // Nuke trailing comma and space.
    sb.append("))");
    return sb.toString();
  }

  /**
   * Calls {@link #getLatLonBoundaryAsGeoJSON(int, int)} with {@link Integer#MAX_VALUE} as both arguments.
   * In effect, the boundary will contain ALL of the points along the edges of the CRS.
   *
   * @return  the latitude/longitude boundary of this CRS as a polygon in GeoJSON.
   */
  public String getLatLonBoundaryAsGeoJSON() {
    return getLatLonBoundaryAsGeoJSON(Integer.MAX_VALUE, Integer.MAX_VALUE);
  }

  /**
   * Similar to {@link #getLatLonBoundaryAsWKT}, but returns a GeoJSON polygon instead.
   */
  public String getLatLonBoundaryAsGeoJSON(int maxPointsInYEdge, int maxPointsInXEdge) {
    List<LatLonPointNoNormalize> points = calcConnectedLatLonBoundaryPoints(maxPointsInYEdge, maxPointsInXEdge);
    StringBuilder sb = new StringBuilder("{ 'type': 'Polygon', 'coordinates': [ [ ");

    for (LatLonPointNoNormalize point : points) {
      sb.append(String.format("[%.3f, %.3f], ", point.getLongitude(), point.getLatitude()));
    }

    sb.delete(sb.length() - 2, sb.length());  // Nuke trailing comma and space.
    sb.append(" ] ] }");
    return sb.toString();
  }
}
