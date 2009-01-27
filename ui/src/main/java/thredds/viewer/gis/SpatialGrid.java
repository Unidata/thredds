// $Id: SpatialGrid.java 50 2006-07-12 16:30:06Z caron $
/*
 * Copyright 1998-2009 University Corporation for Atmospheric Research/Unidata
 *
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
package thredds.viewer.gis;

import java.awt.geom.*;

/** Fast implementation for tracking data overlap and closest point in a 2D region.
 * The region of interest is divided into non-overlapping cells. Each cell may contain
 * zero or one data objects. This allows quickly finding if a data object may be drawn
 * (markIfClear) and closest drawn object to a point (findClosest).
 *
 * @author caron, with design help from russ
 * @version $Id: SpatialGrid.java 50 2006-07-12 16:30:06Z caron $
 */

public class SpatialGrid {
  private static final double MAX_DOUBLE = Double.MAX_VALUE;

  private GridCell[] gridArray[];
  private int nx, ny;
  private int countX, countY;
  private double gridWidth, gridHeight;
  private double offsetX, offsetY;

  private double scaleOverlap = 1.0;
  private Rectangle2D result = new Rectangle2D.Double();

  private boolean debug = false, debugMark = false, debugClosest = false;

  /** Constructor
   * @param nx maximum number of grid cells in x
   * @param ny : maximum number of grid cells in y
   */
  public SpatialGrid(int nx, int ny) {
    this.nx = nx;
    this.ny = ny;
    gridArray = new GridCell[ny][];
    for (int y=0; y<ny; y++) {
      gridArray[y] = new GridCell[nx];
      for (int x=0; x<nx; x++)
        gridArray[y][x] = new GridCell();
    }

  }
  /** Set the grid scale.
   * @param bbox bounding box: we are only interested in points inside of this
   * @param width : divide the bounding box into cells of this width.
   * @param height : divide the bounding box into cells of this height.
   *   maximum number of grid cells is nx x ny
   */
  public void setGrid( Rectangle2D bbox, double width, double height) {
    offsetX = bbox.getX();
    offsetY = bbox.getY();

      // number of grid cells
    countX = Math.min( nx, (int) (bbox.getWidth() / (scaleOverlap * width)));
    countY = Math.min( ny, (int) (bbox.getHeight() / (scaleOverlap * height)));

    gridWidth = bbox.getWidth() / countX;
    gridHeight = bbox.getHeight() / countY;

    if (debug)
      System.out.println("SpatialGrid size "+ gridWidth+" "+ gridHeight+" = "+countX+" by "+countY+
        " scaleOverlap= "+ scaleOverlap);
  }

  /** Set how much the data may overlap.
   * @param overlap : percent overlap
   */
  public void setOverlap(int overlap) {
    // overlap limited to [0, 50%]
    double dover = Math.max( 0.0, Math.min(.01*overlap, .50));
    scaleOverlap = 1.0 - dover;
  }

    /** clear all the grid cells  */
  public void clear() {
   for (int y=0; y<countY; y++)
     for (int x=0; x<countX; x++)
       gridArray[y][x].used = false;
  }

   /** Check if the given rect intersects an already drawn one.
    * If not, set the corresponding cell as marked, store object, return true,
    * meaning "ok to draw".
    * @param rect the bounding box of the thing we want to draw
    * @param o store this object
    * @return true if inside the bounding box and no intersection
    */
  public boolean markIfClear (Rectangle2D rect, Object o) {
    double centerX = rect.getX() + rect.getWidth()/2;
    double centerY = rect.getY() + rect.getHeight()/2;

    int indexX = (int) ((centerX-offsetX)/gridWidth);
    int indexY = (int) ((centerY-offsetY)/gridHeight);

    if (debugMark)
      System.out.println("markIfClear "+ rect+ " "+indexX+" "+indexY);

    if ((indexX < 0) || (indexX >= countX) || (indexY < 0) || (indexY >= countY)) // outside box
      return false;

    GridCell gwant = gridArray[indexY][indexX];
    if (gwant.used)  // already taken
      return false;

    if (null != findIntersection(rect))
      return false;

    // its ok to use
    gwant.used = true;
    gwant.objectBB = rect;
    gwant.o = o;
    return true;
  }

  /** Check if the given rect intersects an already drawn object
    * @param rect the bounding box of the thing we want to draw
    * @return object that intersects, or null if no intersection
    */
  public Object findIntersection (Rectangle2D rect) {
    double centerX = rect.getX() + rect.getWidth()/2;
    double centerY = rect.getY() + rect.getHeight()/2;

    int indexX = (int) ((centerX-offsetX)/gridWidth);
    int indexY = (int) ((centerY-offsetY)/gridHeight);

    // outside box
    if ((indexX < 0) || (indexX >= countX) || (indexY < 0) || (indexY >= countY))
      return null;

    // check the surrounding points
    for (int y=Math.max(0,indexY-1); y<=Math.min(countY-1,indexY+1); y++) {
      for (int x=Math.max(0,indexX-1); x<=Math.min(countX-1,indexX+1); x++) {
        GridCell gtest = gridArray[y][x];

        if (!gtest.used)
          continue;

        if (intersectsOverlap( rect, gtest.objectBB)) // hits an adjacent rectangle
          return gtest.o;
      }
    }

    return null;
  }

  /** Check if the given point is contained in already drawn object
    * @param p the point to check
    * @return object that intersects, or null if no intersection
    */
  public Object findIntersection (Point2D p) {
    int indexX = (int) ((p.getX()-offsetX)/gridWidth);
    int indexY = (int) ((p.getY()-offsetY)/gridHeight);

    // outside box
    if ((indexX < 0) || (indexX >= countX) || (indexY < 0) || (indexY >= countY))
      return null;

    // check the surrounding points
    for (int y=Math.max(0,indexY-1); y<=Math.min(countY-1,indexY+1); y++) {
      for (int x=Math.max(0,indexX-1); x<=Math.min(countX-1,indexX+1); x++) {
        GridCell gtest = gridArray[y][x];

        if (!gtest.used)
          continue;

        if (gtest.objectBB.contains( p.getX(), p.getY()))
          return gtest.o;
      }
    }

    return null;
  }

  /** Find the closest marked cell to the given point
   * @param pt:  find the closest marked cell to this point
   * @return the object associated with the closest cell, or null if none
  */
  public Object findClosest(Point2D pt) {
    Object o = null;
    int indexX = (int) ((pt.getX()-offsetX)/gridWidth);
    int indexY = (int) ((pt.getY()-offsetY)/gridHeight);

    if (debugClosest)
      System.out.println("findClosest "+ pt+ " "+indexX+" "+indexY);

    if ((indexX < 0) || (indexX >= countX) || (indexY < 0) || (indexY >= countY)) // outside box
      return null;

    GridCell gwant = gridArray[indexY][indexX];
    if (gwant.used)  // that was easy
      return gwant.o;

    // check the surrounding points along perimeter of increasing diameter
    for (int p=1; p<Math.max(countX-1, countY-1); p++)
      if (null != (o = findClosestAlongPerimeter(pt, indexX, indexY, p)))
        return o;

    return null; // nothing found
  }

    // search for closest marked cell along the perimeter of square of cells
    // with center cell[x,y] and side of length 2*perimeter+1
  private Object findClosestAlongPerimeter(Point2D pt, int centerX, int centerY, int perimeter) {
    Object closestO = null;
    double closestD = MAX_DOUBLE;

    // top and bottom row
    for (int y=centerY-perimeter; y<=centerY+perimeter; y+=2*perimeter)
      for (int x=centerX-perimeter; x<=centerX+perimeter; x++) {
        double distance = distanceSq(pt,x,y);
        if (distance < closestD) {
          closestO = gridArray[y][x].o;
          closestD = distance;
          if (debugClosest) System.out.println("   closest "+ gridArray[y][x]);
        }
      }

    // middle rows
    for (int y=centerY-perimeter+1; y<=centerY+perimeter-1; y++)
      for (int x=centerX-perimeter; x<=centerX+perimeter; x+=2*perimeter) {
        double distance = distanceSq(pt,x,y);
        if (distance < closestD) {
          closestO = gridArray[y][x].o;
          closestD = distance;
          if (debugClosest) System.out.println("   closest "+ gridArray[y][x]);
        }
      }

    return closestO;
  }

    // return distance**2 from pt to center of marked cell[x,y]
    // if out of bbox or cell not marked, return MAX_DOUBLE
  private double distanceSq( Point2D pt, int indexX, int indexY) {
    if ((indexX < 0) || (indexX >= countX) || (indexY < 0) || (indexY >= countY)) // outside bounding box
      return MAX_DOUBLE;

      GridCell gtest = gridArray[indexY][indexX];
      if (!gtest.used)    // nothing in this cell
        return MAX_DOUBLE;

        // get distance from center of cell
      Rectangle2D rect = gtest.objectBB;
      double dx = rect.getX()+rect.getWidth()/2 - pt.getX();
      double dy = rect.getY()+rect.getHeight()/2 - pt.getY();
      return (dx*dx + dy*dy);
    }

  private boolean intersectsOverlap( Rectangle2D r1, Rectangle2D r2) {
    if (scaleOverlap >= 1.0)
      return r1.intersects(r2);

    Rectangle2D.intersect( r1, r2, result);
    double area = result.getWidth() * result.getHeight();
    return (area > 0) && (area > (1.0 - scaleOverlap) * r1.getWidth() * r1.getHeight());
  }

    // inner class
  private class GridCell {
    boolean used = false;
    Rectangle2D objectBB = null;
    Object o = null;
  }
}

/* Change History:
   $Log: SpatialGrid.java,v $
   Revision 1.2  2004/09/24 03:26:37  caron
   merge nj22

   Revision 1.1  2002/12/13 00:53:09  caron
   pass 2

   Revision 1.2  2002/04/29 22:36:50  caron
   improve variable naming

   Revision 1.1.1.1  2002/02/26 17:24:49  caron
   import sources
*/
