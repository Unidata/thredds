// $Id: Navigation.java 50 2006-07-12 16:30:06Z caron $
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
package thredds.viewer.ui.geoloc;

import ucar.unidata.geoloc.*;
import ucar.nc2.ui.util.ListenerManager;

import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

/** Consider this a private inner class of NavigatedPanel.
    Handle display to world coordinate transformation, always linear.
    Call np.newMapAreaEvent() when MapArea changes.
    setMapArea() trigger a NewMapAreaEvent also.

 * @author John Caron
 * @version $Id: Navigation.java 50 2006-07-12 16:30:06Z caron $
 **/

public class Navigation  {
  private NavigatedPanel np;

    // fundamental quantities
  private double pwidth = 0, pheight = 0;       // current display size
  private double pix_per_world = 1.0;           // scale always the same in both directions
  private double pix_x0 = 0.0, pix_y0 = 0.0;    // offset from world origin, in pixels

    // derived
  private ProjectionRect bb;                    // current world bounding box
  private AffineTransform at;                   // affine transform for graphics2D
    //misc
  private boolean mapAreaIsSet = false;         // cant initialize until screen size is known
  private boolean screenSizeIsSet = false;      // and initial bounding box is known
  private ZoomStack zoom = new ZoomStack();
  private ListenerManager lm; // manage NewMapAreaListener's

  private static boolean debug = false, debugZoom = false, debugTransform = false;
  private static boolean debugRecalc = false;

  Navigation(NavigatedPanel np) {
    this.np = np;
    bb = new ProjectionRect();
    at = new AffineTransform();
  }

  // screen size
  public double getScreenWidth() { return pwidth; }
  public double getScreenHeight() { return pheight; }
  public void setScreenSize(double pwidth, double pheight) {
    if ((pwidth == 0) || (pheight == 0))
      return;

    if (mapAreaIsSet && screenSizeIsSet) {
      // make sure bb is current
      bb.setRect( getMapArea( null));
    }

    this.pwidth = pwidth;
    this.pheight = pheight;
    screenSizeIsSet = true;
    if (debugRecalc) System.out.println("navigation/setScreenSize "+ pwidth+" "+pheight);

    if (mapAreaIsSet) {
      recalcFromBoundingBox();
    }

    fireMapAreaEvent();
  }

    /** Get the affine transform based on screen size and world bounding box */
  public AffineTransform getTransform() {
    at.setTransform( pix_per_world, 0.0, 0.0, -pix_per_world, pix_x0, pix_y0);

    if (debug) {
      System.out.println("Navigation getTransform = "+ pix_per_world +" "+ pix_x0+" "+ pix_y0);
      System.out.println("  transform = "+ at);
    }
    return at;
  }
    // calculate if we want to rotate based on aspect ratio
  public boolean wantRotate(double displayWidth, double displayHeight) {
    getMapArea( bb); // current world bounding box
    boolean aspectDisplay = displayHeight < displayWidth;
    boolean aspectWorldBB = bb.getHeight() < bb.getWidth();
    return (aspectDisplay ^ aspectWorldBB); // aspects are different
 }

  /** Calculate an affine transform based on the display size parameters - used for printing.
   * @param rotate should the page be rotated?
   * @param displayX            upper right corner of display area
   * @param displayY            upper right corner of display area
   * @param displayWidth   display area
   * @param displayHeight   display area
   *
   * see Navigation.calcTransform
   */
  public AffineTransform calcTransform(boolean rotate, double displayX, double displayY, double displayWidth, double displayHeight) {
    getMapArea( bb); // current world bounding box
       // scale to limiting dimension
    double pxpsx, pypsy;
    if (rotate) {
      pxpsx = displayHeight/bb.getWidth();
      pypsy = displayWidth/bb.getHeight();
    } else {
      pxpsx = displayWidth/bb.getWidth();
      pypsy = displayHeight/bb.getHeight();
    }
    double pps = Math.min( pxpsx, pypsy);

      // calc offset: based on center point staying in center
    double wx0 = bb.getX() + bb.getWidth()/2;  // world midpoint
    double wy0 = bb.getY() + bb.getHeight()/2;
    double x0 = displayX + displayWidth/2 - pps * wx0;
    double y0 = displayY + displayHeight/2 + pps * wy0;

    AffineTransform cat = new AffineTransform(pps, 0.0, 0.0, -pps, x0, y0);

    // rotate if we need to
    if (rotate)
        cat.rotate(Math.PI/2, wx0, wy0);

    if (debug) {
      System.out.println("Navigation calcTransform = "+ displayX +" "+ displayY+
                " "+ displayWidth+" "+displayHeight);
      System.out.println("  world = "+ bb);
      System.out.println("  scale/origin = "+ pps +" "+ x0+" "+ y0);
      System.out.println("  transform = "+ cat);
    }
    return cat;
  }

    /** Get current MapArea .
     * @param rect : place results here, or null to create new Object
     */
  public ProjectionRect getMapArea(ProjectionRect rect) {
    if (rect == null)
      rect = new ProjectionRect();

    double width = pwidth/pix_per_world;
    double height = pheight/pix_per_world;

        // center point
    double wx0 = (pwidth/2-pix_x0)/pix_per_world;
    double wy0 = (pix_y0-pheight/2)/pix_per_world;

    rect.setRect(wx0-width/2, wy0-height/2,             // minx, miny
             width, height);                            // width, height

    return rect;
  }

  public void setMapArea(Rectangle2D ma) {
    if (debugRecalc) System.out.println("navigation/setMapArea "+ ma);

    bb.setRect(ma);
    zoom.push();

    mapAreaIsSet = true;
    if (screenSizeIsSet) {
      recalcFromBoundingBox();
      fireMapAreaEvent();
    }
  }
  // kludgy thing used to deal with cylindrical seams: package private
  void setWorldCenterX( double wx_center) {
    pix_x0 = pwidth/2 - pix_per_world * wx_center;
  }

    /** convert a world coordinate to a display point */
  public Point2D worldToScreen(ProjectionPointImpl w, Point2D p) {
    p.setLocation( pix_per_world*w.getX() + pix_x0,
                   -pix_per_world*w.getY() + pix_y0);
    return p;
  }

    /** convert a display point to a world coordinate */
  public ProjectionPointImpl screenToWorld(Point2D p, ProjectionPointImpl w) {
    w.setLocation((p.getX() - pix_x0) / pix_per_world,
                  (pix_y0 - p.getY()) / pix_per_world);
    return w;
  }

  public double getPixPerWorld() { return pix_per_world; }

    /** convert screen Rectangle to a projection (world) rectangle */
  public ProjectionRect screenToWorld(Point2D start, Point2D end) {
    ProjectionPointImpl p1 = new ProjectionPointImpl();
    ProjectionPointImpl p2 = new ProjectionPointImpl();

    screenToWorld( start, p1);
    screenToWorld( end, p2);

    return new ProjectionRect(p1.getX(), p1.getY(), p2.getX(), p2.getY());
  }

    /** convert a projection (world) rectangle to a screen Rectangle */
  public java.awt.Rectangle worldToScreen(ProjectionRect projRect) {
    Point2D p1 = new Point2D.Double();
    Point2D p2 = new Point2D.Double();

    worldToScreen( (ProjectionPointImpl) projRect.getMaxPoint(), p1);
    worldToScreen( (ProjectionPointImpl) projRect.getMinPoint(), p2);
    ProjectionRect r = new ProjectionRect(p1.getX(), p1.getY(), p2.getX(), p2.getY());
    return r.getBounds();
  }

  /************* domain changing calls ****************************/
  /** call this to change the center of the screen's world coordinates.
     deltax, deltay in display coordinates */
  public void pan( double deltax, double deltay) {
    zoom.push();

    pix_x0 -= deltax;
    pix_y0 -= deltay;
    fireMapAreaEvent();
  }

  /** call this to zoom into a subset of the screen.
    startx, starty are the upper left corner of the box in display coords
    width, height the size of the box in display coords */
  public void zoom(double startx, double starty, double width, double height) {
    if (debugZoom)
      System.out.println("zoom "+ startx+ " "+starty+ " "+width+ " "+height+ " ");

    if ((width < 5) || (height < 5))
      return;
    zoom.push();

    pix_x0 -= startx+width/2 - pwidth/2;
    pix_y0 -= starty+height/2 - pheight/2;
    zoom((double)pwidth/width);
  }

  public void zoomIn(double x, double y) {
    zoomIn();
  }
  public void zoomOut(double x, double y) {
    zoomOut();
  }
  public void zoomIn() {
    zoom(2.0);
  }
  public void zoomOut() {
    zoom(.5);
  }
  private void zoom(double scale) {
    zoom.push();

    // change scale, but leave center point fixed
    // get these equations by solving for pix_x0, pix_y0
    // that leaves center point invariant
    double fac = (1 - scale);
    pix_x0 = scale*pix_x0 + fac*pwidth/2;
    pix_y0 = scale*pix_y0 + fac*pheight/2;
    pix_per_world *= scale;
    fireMapAreaEvent();
  }
  public void moveDown() {
    zoom.push();

    pix_y0 -= pheight/2;
    fireMapAreaEvent();
  }
  public void moveUp() {
    zoom.push();

    pix_y0 += pheight/2;
    fireMapAreaEvent();
  }
  public void moveRight() {
    zoom.push();

    pix_x0 -= pwidth/2;
    fireMapAreaEvent();
  }
  public void moveLeft() {
    zoom.push();

    pix_x0 += pwidth/2;
    fireMapAreaEvent();
  }
  public void zoomPrevious() {
    zoom.pop();
    fireMapAreaEvent();
  }

  /////////////////////////////////////////////////////////////////
  // private methods

     // calculate scale and offset based on the current screen size and bounding box
     // adjust bounding box to fit inside the screen size
  private void recalcFromBoundingBox() {
    if (debugRecalc) {
      System.out.println("Navigation recalcFromBoundingBox= "+ bb);
      System.out.println("  "+ pwidth +" "+ pheight);
    }

      // decide which dimension is limiting
    double pixx_per_wx = (bb.getWidth() == 0.0) ? 1 : pwidth / bb.getWidth();
    double pixy_per_wy = (bb.getHeight() == 0.0) ? 1 : pheight / bb.getHeight();
    pix_per_world = Math.min(pixx_per_wx, pixy_per_wy);

      // calc the center point
    double wx0 = bb.getX() + bb.getWidth()/2;
    double wy0 = bb.getY() + bb.getHeight()/2;

      // calc offset based on center point
    pix_x0 = pwidth/2 - pix_per_world * wx0;
    pix_y0 = pheight/2 + pix_per_world * wy0;

    if (debugRecalc) {
      System.out.println("Navigation recalcFromBoundingBox done= "+ pix_per_world +" "+ pix_x0+" "+ pix_y0);
      System.out.println("  "+ pwidth +" "+ pheight+" "+ bb);
    }
  }

  private synchronized void fireMapAreaEvent() {
    // send out event to Navigated Panel
    np.fireMapAreaEvent();
  }

  // keep stack of previous zooms
  // this should propably be made into a circular buffer
  private class ZoomStack extends java.util.ArrayList {
    private int current = -1;
    ZoomStack() {
      super(20); // stack size
    }

    void push() {
      current++;
      add( current, new Zoom( pix_per_world, pix_x0, pix_y0));
    }

    void pop() {
      if (current < 0)
        return;
      Zoom zoom = (Zoom) get( current);
      pix_per_world = zoom.pix_per_world;
      pix_x0 = zoom.pix_x0;
      pix_y0 = zoom.pix_y0;
      current--;
    }

    private class Zoom {
      double pix_per_world;
      double pix_x0;
      double pix_y0;
      Zoom(double p1, double p2, double p3) {
        pix_per_world = p1;
        pix_x0 = p2;
        pix_y0 = p3;
      }
    }

  }

}

/* Change History:
   $Log: Navigation.java,v $
   Revision 1.5  2004/09/24 03:26:41  caron
   merge nj22

   Revision 1.4  2004/05/21 05:57:36  caron
   release 2.0b

   Revision 1.3  2004/02/20 05:02:56  caron
   release 1.3

   Revision 1.2  2003/03/17 21:12:39  john
   new viewer

   Revision 1.1  2002/12/13 00:55:08  caron
   pass 2

   Revision 1.3  2002/04/29 22:23:34  caron
   NP detects seam crossings and throws NewProjectionEvent instead of NewMapAreaEvent

   Revision 1.2  2002/04/29 22:13:16  caron
   move event handling to NavigatedPanel, also fix bug on startup not calling recalcFromBB

   Revision 1.1.1.1  2002/02/26 17:24:52  caron
   import sources
*/
