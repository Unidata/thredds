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
package ucar.nc2.ft2.coverage.writer;

import ucar.nc2.ft2.coverage.*;
import ucar.unidata.geoloc.LatLonPoint;
import ucar.unidata.geoloc.LatLonPointImpl;
import ucar.unidata.geoloc.LatLonRect;
import ucar.unidata.geoloc.ProjectionImpl;

import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;

/**
 * Describe
 *
 * @author caron
 * @since 10/7/2015.
 */
public class CoverageBoundariesExtractor {

  /**
   * Takes a CoverageCollection and returns the boundary as a polygon in WKT,
   * used in OpenLayers widget in NCSS
   *
   * @return WKT string that is a polyline outlining the horizontal boundary
   */
  public static String getDatasetBoundariesWKT(CoverageCollection cc) {
    CoverageBoundariesExtractor extract = new CoverageBoundariesExtractor(cc);
    return extract.getBoundaryAsWKT();
  }

  private CoverageCollection gridDataset;
  private double minLon = 0;
  private double maxLon = 0;
  private double maxDiffLon = 0;
  private boolean crossesDateLine = false;

  public CoverageBoundariesExtractor(CoverageCollection gridDataset) {
    this.gridDataset = gridDataset;
  }

  // public for testing
  public List<LatLonPoint> getBoundaryPoints() {
    List<LatLonPoint> latlons;
    HorizCoordSys hcs = gridDataset.getHorizCoordSys();
    if (hcs.getIsProjection()) {
      latlons = getLatLonsFromProjection(hcs);
    } else {
      latlons = hcs.isLatLon2D() ? getLatLons2D((HorizCoordSys2D) hcs) : getLatLons1D(hcs);
    }
    return latlons;
  }

  public String getBoundaryAsWKT() {
    List<LatLonPoint> latlons = getBoundaryPoints();

    //Build string from lists
    //Crosses dateLine?
    //Assuming grid cells don't extend more than 270 deg.
    if ((maxLon > 0 && minLon < 0) && maxDiffLon > 270) {
      //either crosses 0 or -180
      crossesDateLine = true;
    }

    Formatter result = new Formatter();
    result.format("POLYGON((");

    int nPoints = latlons.size();
    for (int i = 0; i < nPoints; i++) {
      double lon = latlons.get(i).getLongitude();
      if (crossesDateLine && lon < 0) lon += 360; // LOOK
      if (i > 0) result.format(",");
      result.format("%f %f", lon, latlons.get(i).getLatitude());
    }

    result.format("))");
    return result.toString();
  }

  private List<LatLonPoint> getLatLonsFromProjection(HorizCoordSys hcs) {
    List<LatLonPoint> latLonPoints = new ArrayList<>();

    ProjectionImpl fromProj = hcs.getTransform().getProjection();

    CoverageCoordAxis1D xAxis = hcs.getXAxis();
    CoverageCoordAxis1D yAxis = hcs.getYAxis();
    int nx = xAxis.getNcoords();
    int ny = yAxis.getNcoords();
    int stridex = Math.max(1, nx / 100); // dont need more than 100 points
    int stridey = Math.max(1, ny / 100); // dont need more than 100 points

    double y0 = yAxis.getCoordEdge1(0);
    LatLonPoint prev = fromProj.projToLatLon(xAxis.getCoordEdge1(0), y0);
    latLonPoints.add(prev);

    // Bottom edge y=0
    for (int i = 0; i < nx; i+=stridex) {
      LatLonPoint point = fromProj.projToLatLon(xAxis.getCoordEdge2(i), y0);
      check(prev, point);
      latLonPoints.add(point);
      prev = point;
    }

    // Right edge x= nx-1
    double xlast = xAxis.getCoordEdgeLast();
    for (int j = 0; j < ny; j+=stridey) {
      LatLonPoint point = fromProj.projToLatLon(xlast, yAxis.getCoordEdge2(j));
      check(prev, point);
      latLonPoints.add(point);
      prev = point;
    }

    // Top edge y = ny-1
    double ylast = yAxis.getCoordEdgeLast();
    for (int i = nx - 1; i >= 0; i-=stridex) {
      LatLonPoint point = fromProj.projToLatLon(xAxis.getCoordEdge1(i), ylast);
      check(prev, point);
      latLonPoints.add(point);
      prev = point;
    }

    // Left edge x = 0
    double x0 = xAxis.getCoordEdge1(0);
    for (int j = ny - 1; j >= 0; j-=stridey) {
      LatLonPoint point = fromProj.projToLatLon(x0, yAxis.getCoordEdge1(j));
      check(prev, point);
      latLonPoints.add(point);
      prev = point;
    }

    return latLonPoints;
  }

  private void check(LatLonPoint prev, LatLonPoint point) {
    if (point.getLongitude() < minLon) minLon = point.getLongitude();
    if (point.getLongitude() > maxLon) maxLon = point.getLongitude();

    if (Math.abs(prev.getLongitude() - point.getLongitude()) > maxDiffLon)
      maxDiffLon = Math.abs(prev.getLongitude() - point.getLongitude());
  }

  private List<LatLonPoint> getLatLons1D(HorizCoordSys hcs) {
    LatLonRect latLonBB = hcs.makeLatlonBB(null);

    List<LatLonPoint> latLonPoints = new ArrayList<>();
    latLonPoints.add(latLonBB.getLowerLeftPoint());
    latLonPoints.add(latLonBB.getLowerRightPoint());
    latLonPoints.add(latLonBB.getUpperRightPoint());
    latLonPoints.add(latLonBB.getUpperLeftPoint());

    return latLonPoints;
  }

  private List<LatLonPoint> getLatLons2D(HorizCoordSys2D hcs) {
    List<LatLonPoint> latLonPoints = new ArrayList<>();

    LatLonAxis2D latAxis = hcs.getLatAxis2D();
    LatLonAxis2D lonAxis = hcs.getLonAxis2D();
    int[] shape = latAxis.getShape(); // same for both

    int ny = shape[0];
    int nx = shape[1];
    int stridex = Math.max(1, nx / 100); // dont need more than 100 points
    int stridey = Math.max(1, ny / 100); // dont need more than 100 points

    double y0 = latAxis.getCoord(0, 0);
    double x0 = lonAxis.getCoord(0, 0);
    LatLonPointImpl prev = new LatLonPointImpl(y0, x0);
    latLonPoints.add( prev);

    // Bottom edge y=0
    for (int i = 0; i < nx; i+=stridex) {
      double y = latAxis.getCoord(0, i);
      double x = lonAxis.getCoord(0, i);
      LatLonPointImpl point = new LatLonPointImpl(y, x);
      check(prev, point);
      latLonPoints.add(point);
      prev = point;
    }

    // Right edge x= nx-1
    for (int j = 0; j < ny; j+=stridey) {
      double y = latAxis.getCoord(j, nx-1);
      double x = lonAxis.getCoord(j, nx-1);
      LatLonPointImpl point = new LatLonPointImpl(y, x);
      check(prev, point);
      latLonPoints.add(point);
      prev = point;
    }

    // Top edge y = ny-1
    for (int i = nx - 1; i >= 0; i-=stridex) {
      double y = latAxis.getCoord(ny-1, i);
      double x = lonAxis.getCoord(ny-1, i);
      LatLonPointImpl point = new LatLonPointImpl(y, x);
      check(prev, point);
      latLonPoints.add(point);
      prev = point;
    }

    // Left edge x = 0
    for (int j = ny - 1; j >= 0; j-=stridey) {
      double y = latAxis.getCoord(j, 0);
      double x = lonAxis.getCoord(j, 0);
      LatLonPointImpl point = new LatLonPointImpl(y, x);
      check(prev, point);
      latLonPoints.add(point);
      prev = point;
    }

    return latLonPoints;
  }

  /*
  public String getDatasetBoundariesGeoJSON() {

    StringBuilder polygonJSON = new StringBuilder("{\"type\":\"Polygon\", \"coordinates\":[ [ ");

    List<Double> polLons = new ArrayList<>();
    List<Double> polLats = new ArrayList<>();
    getLatLonsForPolygon(polLons, polLats);

    //Build string from lists
    //Crosses dateLine?
    //Assuming grid cells don't extend more than 270 deg.
    if ((maxLon > 0 && minLon < 0) && maxDiffLon > 270) {
      //either crosses 0 or -180
      crossesDateLine = true;
    }

    int nPoints = polLats.size();
    for (int i = 0; i < nPoints; i++) {

      double lon = polLons.get(i);

      if (crossesDateLine && lon < 0) lon += 360;

      if (i < nPoints - 1)
        polygonJSON.append("[").append(lon).append(", ").append(polLats.get(i)).append("],");
      else
        polygonJSON.append("[").append(lon).append(", ").append(polLats.get(i)).append("]");
    }


    polygonJSON.append(" ] ]}");

    return polygonJSON.toString();
  } */

}
