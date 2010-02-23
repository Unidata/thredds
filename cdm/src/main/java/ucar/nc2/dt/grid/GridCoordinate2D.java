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
  static final private boolean debug = false;
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

    System.out.printf("Range (%d %d): lat= (%f,%f) lon = (%f,%f) %n", nrows, ncols, latMinMax.min, latMinMax.max, lonMinMax.min, lonMinMax.max);
  }

  /**
   * Find the best index for the given lat,lon point.
   * @param wantLat   lat of point
   * @param wantLon   lon of point
   * @param rectIndex return (row,col) index, or best guess here. may not be null
   *
   * @return false if not in the grid.
   */
  public boolean findCoordElement(double wantLat, double wantLon, int[] rectIndex) {
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
    rectIndex[0] = (int) Math.round(diffLat / gradientLat);
    rectIndex[1] =(int) Math.round(diffLon / gradientLon);

    int count = 0;
    while (true) {
      count++;
      if (debug) System.out.printf("%nIteration %d %n", count);
      if (contains(wantLat, wantLon, rectIndex))
        return true;

      if (!jump(wantLat, wantLon, rectIndex)) return false;

      // bouncing around
      if (count > 100) {
        log.error("findCoordElement didnt converge lat,lon = "+wantLat+" "+ wantLon);
        return false;
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
  private boolean contains(double wantLat, double wantLon, int[] rectIndex) {
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

  private boolean jump(double wantLat, double wantLon, int[] rectIndex) {
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

    if (g2d.findCoordElement(wantLat, wantLon, result))
      System.out.printf("(%f %f) == (%d %d) %n", wantLat, wantLon, result[0], result[1]);
    else
      System.out.printf("(%f %f) FAIL %n", wantLat, wantLon);
    System.out.printf("----------------------------------------%n");

  }

  public static void main(String[] args) throws IOException {
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
}
