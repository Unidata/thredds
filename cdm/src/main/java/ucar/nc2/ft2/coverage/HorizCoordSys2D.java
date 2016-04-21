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

import ucar.ma2.*;
import ucar.nc2.util.Optional;
import ucar.unidata.geoloc.LatLonPoint;
import ucar.unidata.geoloc.LatLonPointImpl;
import ucar.unidata.geoloc.LatLonRect;
import ucar.unidata.geoloc.ProjectionRect;

import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;

/**
 * HorizCoordSys with LatLonAxis2D latAxis, lonAxis
 * some code forked from ucar.nc2.dataset.GridCoordinate2D
 */
public class HorizCoordSys2D extends HorizCoordSys {
  static private boolean debug = false;
  static private final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(HorizCoordSys2D.class);

  // private final LatLonAxis2D latAxis, lonAxis;
  private final int nrows, ncols;
  private Edges edges;

   HorizCoordSys2D(LatLonAxis2D latCoord, LatLonAxis2D lonCoord) {
    super(null, null, latCoord, lonCoord, null);
    int[] shape = latCoord.getShape(); // y, x

    nrows = shape[0];
    ncols = shape[1];
  }

  @Override
  public boolean isLatLon2D() {
    return true;
  }

  @Override
  public LatLonRect makeLatlonBB(ProjectionRect projBB) {
    synchronized (this) {
      if (edges == null) edges = new Edges();
    }
    LatLonPointImpl min = new LatLonPointImpl(edges.latMinMax.min, edges.lonMinMax.min);
    double height = edges.latMinMax.max - edges.latMinMax.min;
    double width = edges.lonMinMax.max - edges.lonMinMax.min;
    return new LatLonRect(min, height, width);

  }

    @Override
  public Optional<HorizCoordSys> subset(SubsetParams params) {

    LatLonRect llbb = (LatLonRect) params.get(SubsetParams.latlonBB);
    Integer horizStride = (Integer) params.get(SubsetParams.horizStride);
    if (horizStride == null || horizStride < 1) horizStride = 1;

    LatLonAxis2D lataxisSubset = null, lonaxisSubset = null;

    Formatter errMessages = new Formatter();
    if (llbb != null) {
      Optional<List<RangeIterator>> opt = computeBounds(llbb, horizStride);
      if (!opt.isPresent()) {
        errMessages.format("%s;%n", opt.getErrorMessage());
      } else {
        List<RangeIterator> ranges = opt.get();
        lataxisSubset = lataxis2D.subset(ranges.get(1), ranges.get(0));  // x, y
        lonaxisSubset = lonaxis2D.subset(ranges.get(1), ranges.get(0));
      }
    } else if (horizStride > 1) {
      try {
        lataxisSubset = lataxis2D.subset(new Range(0, ncols-1, horizStride), new Range(0, nrows-1, horizStride));
        lonaxisSubset = lonaxis2D.subset(new Range(0, ncols-1, horizStride), new Range(0, nrows-1, horizStride));
      } catch (InvalidRangeException e) {
        errMessages.format("%s;%n", e.getMessage());
      }
    }

    String errs = errMessages.toString();
    if (errs.length() > 0)
      return Optional.empty(errs);

    // makes a copy of the axis
    if (lataxisSubset == null) lataxisSubset = (LatLonAxis2D) lataxis2D.copy();
    if (lonaxisSubset == null) lonaxisSubset = (LatLonAxis2D) lonaxis2D.copy();

    return Optional.of(new HorizCoordSys2D(lataxisSubset, lonaxisSubset));
  }

  @Override
  public LatLonPoint getLatLon(int yindex, int xindex) {
    double lat = lataxis2D.getCoord(yindex, xindex);
    double lon = lonaxis2D.getCoord(yindex, xindex);
    return new LatLonPointImpl(lat, lon);
  }

  @Override
  public List<RangeIterator> getRanges() {
    return lataxis2D.getRanges(); // both are the same
  }

  @Override
  public Optional<CoordReturn> findXYindexFromCoord(double x, double y) {
    synchronized (this) {
      if (edges == null) edges = new Edges();
    }
    CoordReturn result = new CoordReturn();
    int[] index = new int[2];
    boolean ok = edges.findCoordElement(y, x, index);
    if (!ok)
      return Optional.empty("not in grid2D");

    result.x = index[1];
    result.y = index[0];
    result.xcoord = getLonAxis2D().getCoord(result.y, result.x);
    result.ycoord = getLatAxis2D().getCoord(result.y, result.x);
    return Optional.of(result);
  }

  @Override
  public List<CoverageCoordAxis> getCoordAxes() {
    List<CoverageCoordAxis> result = new ArrayList<>();
    result.add(lataxis2D);
    result.add(lonaxis2D);
    return result;
  }

  // return y, x ranges
  private Optional<List<RangeIterator>> computeBounds(LatLonRect llbb, int horizStride) {
    synchronized (this) {
      if (edges == null) edges = new Edges();
    }
    return edges.computeBoundsExhaustive(llbb, horizStride);
  }

  // assume this class is instantiated when these edges are needed
  private class Edges {
    private ArrayDouble.D2 latEdge, lonEdge;
    private MAMath.MinMax latMinMax, lonMinMax;

    Edges() {
      latEdge = (ArrayDouble.D2) lataxis2D.getCoordBoundsAsArray();
      lonEdge = (ArrayDouble.D2) lonaxis2D.getCoordBoundsAsArray();

      // assume missing values have been converted to NaNs
      latMinMax = MAMath.getMinMax(latEdge);
      lonMinMax = MAMath.getMinMax(lonEdge);

      // normalize to [minLon,minLon+360]
      int nlons = (int) lonEdge.getSize();
      for (int i = 0; i < nlons; i++) {
        double nonVal = lonEdge.getDouble(i);
        lonEdge.setDouble(i, LatLonPointImpl.lonNormalFrom(nonVal, lonMinMax.min));
      }

      if (debug)
        System.out.printf("Bounds (%d %d): lat= (%f,%f) lon = (%f,%f) %n", nrows, ncols, latMinMax.min, latMinMax.max, lonMinMax.min, lonMinMax.max);
    }

    /**
     * Find the best index for the given lat,lon point.
     *
     * @param wantLat   lat of point
     * @param wantLon   lon of point
     * @param rectIndex return (row,col) index, or best guess here. may not be null
     * @return false if not in the grid.
     */
    public boolean findCoordElement(double wantLat, double wantLon, int[] rectIndex) {
      double wantLonNormal = LatLonPointImpl.lonNormalFrom(wantLon, lonMinMax.min);
      return findCoordElementNoForce(wantLat, wantLonNormal, rectIndex);
    }

    private boolean findCoordElementNoForce(double wantLat, double wantLon, int[] rectIndex) {
      if (wantLat < latMinMax.min) return false;
      if (wantLat > latMinMax.max) return false;
      if (wantLon < lonMinMax.min) return false;
      if (wantLon > lonMinMax.max) return false;

      double gradientLat = (latMinMax.max - latMinMax.min) / nrows;
      double gradientLon = (lonMinMax.max - lonMinMax.min) / ncols;

      double diffLat = wantLat - latMinMax.min;
      double diffLon = wantLon - lonMinMax.min;

      // initial guess
      rectIndex[0] = (int) Math.round(diffLat / gradientLat);  // row
      rectIndex[1] = (int) Math.round(diffLon / gradientLon);  // col

      int count = 0;
      while (true) {
        count++;
        if (debug) System.out.printf("%nIteration %d %n", count);
        if (contains(wantLat, wantLon, rectIndex))
          return true;

        if (!jump2(wantLat, wantLon, rectIndex))
          return false;

        // bouncing around
        if (count > 10) {
          // last ditch attempt
          return incr(wantLat, wantLon, rectIndex);
          //if (!ok)
          //  log.error("findCoordElement didnt converge lat,lon = "+wantLat+" "+ wantLon);
          //return ok;
        }
      }
    }

    /**
     * Is the point (lat,lon) contained in the (row, col) rectangle ?
     *
     * @param wantLat   lat of point
     * @param wantLon   lon of point
     * @param rectIndex rectangle row, col, will be clipped to [0, nrows), [0, ncols)
     * @return true if contained
     */
    private boolean containsOld(double wantLat, double wantLon, int[] rectIndex) {
      rectIndex[0] = Math.max(Math.min(rectIndex[0], nrows - 1), 0);
      rectIndex[1] = Math.max(Math.min(rectIndex[1], ncols - 1), 0);

      int row = rectIndex[0];
      int col = rectIndex[1];

      if (debug) System.out.printf(" (%d,%d) contains (%f,%f) in (lat=%f %f) (lon=%f %f) ?%n",
              rectIndex[0], rectIndex[1], wantLat, wantLon,
              latEdge.get(row, col), latEdge.get(row + 1, col), lonEdge.get(row, col), lonEdge.get(row, col + 1));

      if (wantLat < latEdge.get(row, col)) return false;
      if (wantLat > latEdge.get(row + 1, col)) return false;
      if (wantLon < lonEdge.get(row, col)) return false;
      if (wantLon > lonEdge.get(row, col + 1)) return false;
      return true;
    }

    /**
     * Is the point (lat,lon) contained in the (row, col) rectangle ?
     *
     * @param wantLat   lat of point
     * @param wantLon   lon of point
     * @param rectIndex rectangle row, col, will be clipped to [0, nrows), [0, ncols)
     * @return true if contained
     */

/*
  http://mathforum.org/library/drmath/view/54386.html

      Given any three points on the plane (x0,y0), (x1,y1), and
  (x2,y2), the area of the triangle determined by them is
  given by the following formula:

        1 | x0 y0 1 |
    A = - | x1 y1 1 |,
        2 | x2 y2 1 |

  where the vertical bars represent the determinant.
  the value of the expression above is:

       (.5)(x1*y2 - y1*x2 -x0*y2 + y0*x2 + x0*y1 - y0*x1)

  The amazing thing is that A is positive if the three points are
  taken in a counter-clockwise orientation, and negative otherwise.

  To be inside a rectangle (or any convex body), as you trace
  around in a clockwise direction from p1 to p2 to p3 to p4 and
  back to p1, the "areas" of triangles p1 p2 p, p2 p3 p, p3 p4 p,
  and p4 p1 p must all be positive.  If you don't know the
  orientation of your rectangle, then they must either all be
  positive or all be negative.

  this method works for arbitrary convex regions on the plane.
*/
    private boolean contains(double wantLat, double wantLon, int[] rectIndex) {
      rectIndex[0] = Math.max(Math.min(rectIndex[0], nrows - 1), 0);
      rectIndex[1] = Math.max(Math.min(rectIndex[1], ncols - 1), 0);

      int row = rectIndex[0];
      int col = rectIndex[1];

      double x1 = lonEdge.get(row, col);
      double y1 = latEdge.get(row, col);

      double x2 = lonEdge.get(row, col + 1);
      double y2 = latEdge.get(row, col + 1);

      double x3 = lonEdge.get(row + 1, col + 1);
      double y3 = latEdge.get(row + 1, col + 1);

      double x4 = lonEdge.get(row + 1, col);
      double y4 = latEdge.get(row + 1, col);

      // must all have same determinate sign
      boolean sign = detIsPositive(x1, y1, x2, y2, wantLon, wantLat);
      if (sign != detIsPositive(x2, y2, x3, y3, wantLon, wantLat)) return false;
      if (sign != detIsPositive(x3, y3, x4, y4, wantLon, wantLat)) return false;
      if (sign != detIsPositive(x4, y4, x1, y1, wantLon, wantLat)) return false;

      return true;
    }

    private boolean detIsPositive(double x0, double y0, double x1, double y1, double x2, double y2) {
      double det = (x1 * y2 - y1 * x2 - x0 * y2 + y0 * x2 + x0 * y1 - y0 * x1);
      if (det == 0)
        log.warn("determinate = 0 on lat/lon=" + lataxis2D.getName() + ", " + lonaxis2D.getName()); // LOOK needed?
      return det > 0;
    }

    private boolean jumpOld(double wantLat, double wantLon, int[] rectIndex) {
      int row = Math.max(Math.min(rectIndex[0], nrows - 1), 0);
      int col = Math.max(Math.min(rectIndex[1], ncols - 1), 0);
      double gradientLat = latEdge.get(row + 1, col) - latEdge.get(row, col);
      double gradientLon = lonEdge.get(row, col + 1) - lonEdge.get(row, col);

      double diffLat = wantLat - latEdge.get(row, col);
      double diffLon = wantLon - lonEdge.get(row, col);

      int drow = (int) Math.round(diffLat / gradientLat);
      int dcol = (int) Math.round(diffLon / gradientLon);

      if (debug) System.out.printf("   jump from %d %d (grad=%f %f) (diff=%f %f) (delta=%d %d)",
              row, col, gradientLat, gradientLon,
              diffLat, diffLon, drow, dcol);

      if ((drow == 0) && (dcol == 0)) {
        if (debug) System.out.printf("%n   incr:");
        return incr(wantLat, wantLon, rectIndex);
      } else {
        rectIndex[0] = Math.max(Math.min(row + drow, nrows - 1), 0);
        rectIndex[1] = Math.max(Math.min(col + dcol, ncols - 1), 0);
        if (debug) System.out.printf(" to (%d %d)%n", rectIndex[0], rectIndex[1]);
        if ((row == rectIndex[0]) && (col == rectIndex[1])) return false; // nothing has changed
      }

      return true;
    }

    /**
     * jump to a new row,col
     *
     * @param wantLat   want this lat
     * @param wantLon   want this lon
     * @param rectIndex starting row, col and replaced by new guess
     * @return true if new guess, false if failure
     */
  /*
    choose x, y such that (matrix multiply) :

    (wantx) = (fxx fxy)  (x)
    (wanty)   (fyx fyy)  (y)

     where fxx = d(fx)/dx  ~= delta lon in lon direction
     where fxy = d(fx)/dy  ~= delta lon in lat direction
     where fyx = d(fy)/dx  ~= delta lat in lon direction
     where fyy = d(fy)/dy  ~= delta lat in lat direction

    2 linear equations in 2 unknowns, solve in usual way
   */
    private boolean jump2(double wantLat, double wantLon, int[] rectIndex) {
      int row = Math.max(Math.min(rectIndex[0], nrows - 1), 0);
      int col = Math.max(Math.min(rectIndex[1], ncols - 1), 0);
      double lat = latEdge.get(row, col);
      double lon = lonEdge.get(row, col);
      double diffLat = wantLat - lat;
      double diffLon = wantLon - lon;

      double dlatdy = latEdge.get(row + 1, col) - lat;
      double dlatdx = latEdge.get(row, col + 1) - lat;
      double dlondx = lonEdge.get(row, col + 1) - lon;
      double dlondy = lonEdge.get(row + 1, col) - lon;

      // solve for dlon

      double dx = (diffLon - dlondy * diffLat / dlatdy) / (dlondx - dlatdx * dlondy / dlatdy);
      // double dy =  (diffLat - dlatdx * diffLon / dlondx) / (dlatdy - dlatdx * dlondy / dlondx);
      double dy = (diffLat - dlatdx * dx) / dlatdy;

      if (debug)
        System.out.printf("   jump from %d %d (dlondx=%f dlondy=%f dlatdx=%f dlatdy=%f) (diffLat,Lon=%f %f) (deltalat,Lon=%f %f)",
                row, col, dlondx, dlondy, dlatdx, dlatdy,
                diffLat, diffLon, dy, dx);

      int drow = (int) Math.round(dy);
      int dcol = (int) Math.round(dx);

      if ((drow == 0) && (dcol == 0)) {
        if (debug) System.out.printf("%n   incr:");
        return incr(wantLat, wantLon, rectIndex);
      } else {
        rectIndex[0] = Math.max(Math.min(row + drow, nrows - 1), 0);
        rectIndex[1] = Math.max(Math.min(col + dcol, ncols - 1), 0);
        if (debug) System.out.printf(" to (%d %d)%n", rectIndex[0], rectIndex[1]);
        if ((row == rectIndex[0]) && (col == rectIndex[1])) return false; // nothing has changed
      }

      return true;
    }

    private boolean incr(double wantLat, double wantLon, int[] rectIndex) {
      int row = rectIndex[0];
      int col = rectIndex[1];
      double diffLat = wantLat - latEdge.get(row, col);
      double diffLon = wantLon - lonEdge.get(row, col);

      if (Math.abs(diffLat) > Math.abs(diffLon)) { // try lat first
        rectIndex[0] = row + ((diffLat > 0) ? 1 : -1);
        if (contains(wantLat, wantLon, rectIndex)) return true;
        rectIndex[1] = col + ((diffLon > 0) ? 1 : -1);
        if (contains(wantLat, wantLon, rectIndex)) return true;
      } else {
        rectIndex[1] = col + ((diffLon > 0) ? 1 : -1);
        if (contains(wantLat, wantLon, rectIndex)) return true;
        rectIndex[0] = row + ((diffLat > 0) ? 1 : -1);
        if (contains(wantLat, wantLon, rectIndex)) return true;
      }

      // back to original, do box search
      rectIndex[0] = row;
      rectIndex[1] = col;
      return box9(wantLat, wantLon, rectIndex);
    }

    // we think its got to be in one of the 9 boxes around rectIndex
    private boolean box9(double wantLat, double wantLon, int[] rectIndex) {
      int row = rectIndex[0];
      int minrow = Math.max(row - 1, 0);
      int maxrow = Math.min(row + 1, nrows);

      int col = rectIndex[1];
      int mincol = Math.max(col - 1, 0);
      int maxcol = Math.min(col + 1, ncols);

      if (debug) System.out.printf("%n   box9:");
      for (int i = minrow; i <= maxrow; i++)
        for (int j = mincol; j <= maxcol; j++) {
          rectIndex[0] = i;
          rectIndex[1] = j;
          if (contains(wantLat, wantLon, rectIndex)) return true;
        }

      return false;
    }

    // return y, x ranges
    Optional<List<RangeIterator>> computeBoundsExhaustive(LatLonRect rect, int horizStride) {
      LatLonPointImpl llpt = rect.getLowerLeftPoint();
      LatLonPointImpl urpt = rect.getUpperRightPoint();

      double miny = llpt.getLatitude();
      double maxy = urpt.getLatitude();

      // normalize to [minLon,minLon+360], edge already normalized to this
      double minx = LatLonPointImpl.lonNormalFrom(llpt.getLongitude(), lonMinMax.min);
      double maxx = LatLonPointImpl.lonNormalFrom(urpt.getLongitude(), lonMinMax.min);

      int shape[] = lonaxis2D.getShape();
      int ny = shape[0];
      int nx = shape[1];
      int minCol = Integer.MAX_VALUE, minRow = Integer.MAX_VALUE;
      int maxCol = -1, maxRow = -1;

      boolean allX = (minx > lonMinMax.max && maxx > lonMinMax.max && minx > maxx);
      boolean allY =  (miny <= latMinMax.min && maxy >= latMinMax.max);
      if (allX && allY) {
        // return full set
        return Optional.of(getRanges());
      }

      if (minx > lonMinMax.max && maxx > lonMinMax.max && minx < maxx) { // otherwise ignoring minx > maxx
        return Optional.empty("no intersection");
      } else if (minx > lonMinMax.max && maxx > lonMinMax.max && minx > maxx) {
        minCol = 0;   // all of x
        maxCol = nx;
        minx = lonMinMax.min;
      } else if (minx > lonMinMax.min && maxx > lonMinMax.max) {
        maxCol = nx;
      } else if (minx > lonMinMax.max && maxx < lonMinMax.max) {
        minCol = 0;
        minx = lonMinMax.min;
      }

      // probably not needed
      if (miny <= latMinMax.min) {
        minRow = 0;
      } else if (maxy >= latMinMax.max) {
        maxRow = ny;
      }

      // brute force, examine every point LOOK BAD
      for (int row = 0; row <= ny; row++) {
        for (int col = 0; col <= nx; col++) {
          double lat = latEdge.get(row, col);
          double lon = lonEdge.get(row, col);

          if ((lat >= miny) && (lat <= maxy) && (lon >= minx) && (lon <= maxx)) {
            if (col > maxCol) maxCol = col;
            if (col < minCol) minCol = col;
            if (row > maxRow) maxRow = row;
            if (row < minRow) minRow = row;
          }
        }
      }

      try {
        List<RangeIterator> list = new ArrayList<>();
        list.add(new Range(minRow, maxRow-1, horizStride));
        list.add(new Range(minCol, maxCol-1, horizStride));
        return Optional.of(list);

      } catch (InvalidRangeException e) {
        throw new RuntimeException(e);
      }

    }

    private double getMinOrMaxLon(double lon1, double lon2, boolean wantMin) {
      double midpoint = (lon1 + lon2) / 2;
      lon1 = LatLonPointImpl.lonNormal(lon1, midpoint);
      lon2 = LatLonPointImpl.lonNormal(lon2, midpoint);

      return wantMin ? Math.min(lon1, lon2) : Math.max(lon1, lon2);
    }

    // brute force
    public boolean findCoordElementExhaustive(double wantLat, double wantLon, int[] rectIndex) {
      if (wantLat < latMinMax.min) return false;
      if (wantLat > latMinMax.max) return false;
      if (wantLon < lonMinMax.min) return false;
      if (wantLon > lonMinMax.max) return false;

      for (int row = 0; row < nrows; row++) {
        for (int col = 0; col < ncols; col++) {

          rectIndex[0] = row;
          rectIndex[1] = col;

          if (contains(wantLat, wantLon, rectIndex)) {
            return true;
          }
        }
      }
      //debug = saveDebug;
      return false;
    }
  }


  /*
  public int[] findXYindexFromCoord(double x_coord, double y_coord) {
      int[] result = new int[2];

      int[] result2 = new int[2];
      boolean found = g2d.findCoordElement(y_coord, x_coord, result2);
      if (found) {
        result[0] = result2[1];
        result[1] = result2[0];
      } else {
        result[0] = -1;
        result[1] = -1;
      }
      return result;
  } */

}

