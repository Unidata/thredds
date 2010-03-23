/*
 * Copyright (c) 1998 - 2009. University Corporation for Atmospheric Research/Unidata
 * Portions of this software were developed by the Unidata Program at the
 * University Corporation for Atmospheric Research.
 *
 * Access and use of this software shall impose the following obligations
 * and understandings on the user. The user is granted the right, without
 * any fee or cost, to use, copy, modify, alter, enhance and distribute
 * this software, and any derivative works thereof, and its supporting
 * documentation for any purpose whatsoever, provided that this entire
 * notice appears in all copies of the software, derivative works and
 * supporting documentation.  Further, UCAR requests that the user credit
 * UCAR/Unidata in any publications that result from the use of this
 * software or in any product that includes this software. The names UCAR
 * and/or Unidata, however, may not be used in any advertising or publicity
 * to endorse or promote any products or commercial entity unless specific
 * written permission is obtained from UCAR/Unidata. The user also
 * understands that UCAR/Unidata is not obligated to provide the user with
 * any support, consulting, training or assistance of any kind with regard
 * to the use, operation and performance of this software nor to provide
 * the user with any updates, revisions, new versions or "bug fixes."
 *
 * THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 * INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 * FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 * NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 * WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */

package ucar.nc2.dt.grid;

import ucar.nc2.dataset.CoordinateAxis2D;
import ucar.nc2.dataset.CoordinateAxis;
import ucar.nc2.dt.GridCoordSystem;
import ucar.ma2.ArrayDouble;
import ucar.ma2.MAMath;

import java.io.IOException;

/**
 * 2D Coordinate System has lat(x,y) and lon(x,y).
 * This class implements finding the index (i,j) from (lat, lon) coord.
 * This is for "one-off" computation, not a systematic lookup table for all points in a pixel array.
 * Hueristically searches the 2D space for the cell tha contains the point.
 *
 * @author caron
 * @since Jul 10, 2009
 */


public class GridCoordinate2D {
  static private boolean debug = false;
  static private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(GridCoordinate2D.class);

  private CoordinateAxis2D latCoord, lonCoord;
  private ArrayDouble.D2 latEdge, lonEdge;
  private MAMath.MinMax latMinMax, lonMinMax;
  int nrows, ncols;

  GridCoordinate2D(CoordinateAxis2D latCoord, CoordinateAxis2D lonCoord) {
    this.latCoord = latCoord;
    this.lonCoord = lonCoord;

    assert latCoord.getRank() == 2;
    assert lonCoord.getRank() == 2;
    int[] shape = latCoord.getShape();

    nrows = shape[0];
    ncols = shape[1];
  }

  private void findBounds() {
    if (lonMinMax != null) return;

    lonEdge = CoordinateAxis2D.makeXEdges(lonCoord.getMidpoints());
    latEdge = CoordinateAxis2D.makeYEdges(latCoord.getMidpoints());

    // assume missing values have been converted to NaNs
    latMinMax = MAMath.getMinMax(latEdge);
    lonMinMax = MAMath.getMinMax(lonEdge);

    if (debug)
      System.out.printf("Bounds (%d %d): lat= (%f,%f) lon = (%f,%f) %n", nrows, ncols, latMinMax.min, latMinMax.max, lonMinMax.min, lonMinMax.max);
  }

  // brute force
  public boolean findCoordElementForce(double wantLat, double wantLon, int[] rectIndex) {
    findBounds();
    if (wantLat < latMinMax.min) return false;
    if (wantLat > latMinMax.max) return false;
    if (wantLon < lonMinMax.min) return false;
    if (wantLon > lonMinMax.max) return false;

    boolean saveDebug = debug;
    debug = false;
    for (int row=0; row<nrows; row++) {
      for (int col=0; col<ncols; col++) {

        rectIndex[0] = row;
        rectIndex[1] = col;

        if (contains(wantLat, wantLon, rectIndex)) {
          debug = saveDebug;
          return true;
        }
      }
    }
    //debug = saveDebug;
    return false;
  }

  public boolean findCoordElement(double wantLat, double wantLon, int[] rectIndex) {
    return findCoordElementNoForce(wantLat, wantLon,rectIndex);
  }  


  /**
   * Find the best index for the given lat,lon point.
   * @param wantLat   lat of point
   * @param wantLon   lon of point
   * @param rectIndex return (row,col) index, or best guess here. may not be null
   *
   * @return false if not in the grid.
   */
  public boolean findCoordElementNoForce(double wantLat, double wantLon, int[] rectIndex) {
    findBounds();
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
    rectIndex[1] =(int) Math.round(diffLon / gradientLon);  // col

    int count = 0;
    while (true) {
      count++;
      if (debug) System.out.printf("%nIteration %d %n", count);
      if (contains(wantLat, wantLon, rectIndex))
        return true;

      if (!jump2(wantLat, wantLon, rectIndex)) return false;

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
    rectIndex[0] = Math.max( Math.min(rectIndex[0], nrows-1), 0);
    rectIndex[1] = Math.max( Math.min(rectIndex[1], ncols-1), 0);

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

  this method works for arbitrary convex regions oo the plane.
*/
  private boolean contains(double wantLat, double wantLon, int[] rectIndex) {
    rectIndex[0] = Math.max( Math.min(rectIndex[0], nrows-1), 0);
    rectIndex[1] = Math.max( Math.min(rectIndex[1], ncols-1), 0);

    int row = rectIndex[0];
    int col = rectIndex[1];

    double x1 = lonEdge.get(row, col);
    double y1 = latEdge.get(row, col);

    double x2 = lonEdge.get(row, col+1);
    double y2 = latEdge.get(row, col+1);

    double x3 = lonEdge.get(row+1, col+1);
    double y3 = latEdge.get(row+1, col+1);

    double x4 = lonEdge.get(row+1, col);
    double y4 = latEdge.get(row+1, col);

    // must all have same determinate sign
    boolean sign = detIsPositive(x1, y1, x2, y2, wantLon, wantLat);
    if (sign != detIsPositive(x2, y2, x3, y3, wantLon, wantLat)) return false;
    if (sign != detIsPositive(x3, y3, x4, y4, wantLon, wantLat)) return false;
    if (sign != detIsPositive(x4, y4, x1, y1, wantLon, wantLat)) return false;

    return true;
  }

  private boolean detIsPositive(double x0, double y0, double x1, double y1, double x2, double y2) {
    double det = (x1*y2 - y1*x2 -x0*y2 + y0*x2 + x0*y1 - y0*x1);
    if (det == 0)
      System.out.printf("determinate = 0%n");
    return det > 0;
  }

  private boolean jumpOld(double wantLat, double wantLon, int[] rectIndex) {
    int row = Math.max( Math.min(rectIndex[0], nrows-1), 0);
    int col = Math.max( Math.min(rectIndex[1], ncols-1), 0);
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
      rectIndex[0] = Math.max( Math.min(row + drow, nrows-1), 0);
      rectIndex[1] = Math.max( Math.min(col + dcol, ncols-1), 0);
      if (debug) System.out.printf(" to (%d %d)%n", rectIndex[0], rectIndex[1]);
      if ((row == rectIndex[0]) && (col == rectIndex[1])) return false; // nothing has changed
    }

    return true;
  }

  /**
   * jump to a new row,col
   * @param wantLat want this lat
   * @param wantLon want this lon
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
    int row = Math.max( Math.min(rectIndex[0], nrows-1), 0);
    int col = Math.max( Math.min(rectIndex[1], ncols-1), 0);
    double dlatdy = latEdge.get(row + 1, col) - latEdge.get(row, col);
    double dlatdx = latEdge.get(row, col+1) - latEdge.get(row, col);
    double dlondx = lonEdge.get(row, col + 1) - lonEdge.get(row, col);
    double dlondy = lonEdge.get(row + 1, col) - lonEdge.get(row, col);

    double diffLat = wantLat - latEdge.get(row, col);
    double diffLon = wantLon - lonEdge.get(row, col);

    // solve for dlon

    double dx =  (diffLon - dlondy * diffLat / dlatdy) / (dlondx - dlatdx * dlondy / dlatdy);
    // double dy =  (diffLat - dlatdx * diffLon / dlondx) / (dlatdy - dlatdx * dlondy / dlondx);
    double dy =  (diffLat - dlatdx * dx) / dlatdy;

    if (debug) System.out.printf("   jump from %d %d (dlondx=%f dlondy=%f dlatdx=%f dlatdy=%f) (diffLat,Lon=%f %f) (deltalat,Lon=%f %f)",
            row, col, dlondx, dlondy, dlatdx, dlatdy,
            diffLat, diffLon, dy, dx);

    int drow = (int) Math.round(dy);
    int dcol = (int) Math.round(dx);

    if ((drow == 0) && (dcol == 0)) {
      if (debug) System.out.printf("%n   incr:");
      return incr(wantLat, wantLon, rectIndex);
    } else {
      rectIndex[0] = Math.max( Math.min(row + drow, nrows-1), 0);
      rectIndex[1] = Math.max( Math.min(col + dcol, ncols-1), 0);
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
      if (contains(wantLat, wantLon,  rectIndex)) return true;
      rectIndex[1] = col + ((diffLon > 0) ? 1 : -1);
      if (contains(wantLat, wantLon,  rectIndex)) return true;
    } else {
      rectIndex[1] = col + ((diffLon > 0) ? 1 : -1);
      if (contains(wantLat, wantLon,  rectIndex)) return true;
      rectIndex[0] = row + ((diffLat > 0) ? 1 : -1);
      if (contains(wantLat, wantLon,  rectIndex)) return true;
    }

    // back to original, do box search
    rectIndex[0] = row;
    rectIndex[1] = col;
    return box9(wantLat, wantLon, rectIndex);
  }

  // we think its got to be in one of the 9 boxes around rectIndex
  private boolean box9(double wantLat, double wantLon, int[] rectIndex) {
    int row = rectIndex[0];
    int minrow = Math.max(row-1, 0);
    int maxrow = Math.min(row+1, nrows);

    int col = rectIndex[1];
    int mincol= Math.max(col-1, 0);
    int maxcol = Math.min(col+1, ncols);

    if (debug) System.out.printf("%n   box9:");
    for (int i=minrow; i<=maxrow; i++)
      for (int j=mincol; j<=maxcol; j++) {
        rectIndex[0] = i;
        rectIndex[1] = j;
        if (contains(wantLat, wantLon,  rectIndex)) return true;
      }

    return false;
  }

  private static void doOne(GridCoordinate2D g2d, double wantLat, double wantLon) {
    int[] result = new int[2];
    if (g2d.findCoordElementForce(wantLat, wantLon, result))
      System.out.printf("Brute (%f %f) == (%d %d) %n", wantLat, wantLon, result[0], result[1]);
    else {
      System.out.printf("Brute (%f %f) FAIL", wantLat, wantLon);
      return;
    }

    if (g2d.findCoordElement(wantLat, wantLon, result))
      System.out.printf("(%f %f) == (%d %d) %n", wantLat, wantLon, result[0], result[1]);
    else
      System.out.printf("(%f %f) FAIL %n", wantLat, wantLon);
    System.out.printf("----------------------------------------%n");

  }

  public static void test1() throws IOException {
    String filename = "D:/work/asaScience/EGM200_3.ncml";
    GridDataset gds = GridDataset.open(filename);
    GeoGrid grid = gds.findGridByName("u_wind");
    GridCoordSystem gcs = grid.getCoordinateSystem();
    CoordinateAxis lonAxis = gcs.getXHorizAxis();
    assert lonAxis instanceof CoordinateAxis2D;
    CoordinateAxis latAxis = gcs.getYHorizAxis();
    assert latAxis instanceof CoordinateAxis2D;

    GridCoordinate2D g2d = new GridCoordinate2D((CoordinateAxis2D) latAxis, (CoordinateAxis2D) lonAxis);
    doOne(g2d, 35.0, -6.0);
    doOne(g2d, 34.667302, -5.008376); // FAIL
    doOne(g2d, 34.667303, -6.394240);
    doOne(g2d, 36.6346, -5.0084);
    doOne(g2d, 36.6346, -6.394240);

    gds.close();
  }

  public static void test2() throws IOException {
    String filename = "C:/data/fmrc/apex_fmrc/Run_20091025_0000.nc";
    GridDataset gds = GridDataset.open(filename);
    GeoGrid grid = gds.findGridByName("temp");
    GridCoordSystem gcs = grid.getCoordinateSystem();
    CoordinateAxis lonAxis = gcs.getXHorizAxis();
    assert lonAxis instanceof CoordinateAxis2D;
    CoordinateAxis latAxis = gcs.getYHorizAxis();
    assert latAxis instanceof CoordinateAxis2D;

    GridCoordinate2D g2d = new GridCoordinate2D((CoordinateAxis2D) latAxis, (CoordinateAxis2D) lonAxis);
    doOne(g2d, 40.166959,-73.954234);

    gds.close();
  }

  public static void test3() throws IOException {
    String filename = "/data/testdata/cdmUnitTest/fmrc/rtofs/ofs.20091122/ofs_atl.t00z.F024.grb.grib2";
    GridDataset gds = GridDataset.open(filename);
    GeoGrid grid = gds.findGridByName("Sea_Surface_Height_Relative_to_Geoid");
    GridCoordSystem gcs = grid.getCoordinateSystem();
    CoordinateAxis lonAxis = gcs.getXHorizAxis();
    assert lonAxis instanceof CoordinateAxis2D;
    CoordinateAxis latAxis = gcs.getYHorizAxis();
    assert latAxis instanceof CoordinateAxis2D;

    GridCoordinate2D g2d = new GridCoordinate2D((CoordinateAxis2D) latAxis, (CoordinateAxis2D) lonAxis);
    doOne(g2d, -15.554099426977835, -0.7742870290336263);

    gds.close();
  }



  public static void main(String[] args) throws IOException {
    test3();
  }

}
