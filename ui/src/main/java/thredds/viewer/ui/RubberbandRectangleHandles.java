// $Id: RubberbandRectangleHandles.java 50 2006-07-12 16:30:06Z caron $
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

package thredds.viewer.ui;

import java.awt.*;
import java.awt.geom.*;

/** Rectangle Rubberbanding.
 * @author David M. Geary
 * @author John Caron
 * @version $Id: RubberbandRectangleHandles.java 50 2006-07-12 16:30:06Z caron $
 */
public class RubberbandRectangleHandles extends Rubberband {
  static public double handleSizePixels = 6.0;

  private Rectangle current;

  public RubberbandRectangleHandles(Component c, boolean listen) {
    super( c, listen);
  }

  public void setRectangle(Rectangle current) {
    this.current = current;
  }

  /**
   * Erase the last rectangle and shift the rectangle and redraw
   * deltax, deltay: position from original position
   */
  public void move(int deltax, int deltay) {
    lastPt.x      = stretchedPt.x;
    lastPt.y      = stretchedPt.y;

    Graphics2D g = (Graphics2D) component.getGraphics();
    if(g != null) {
     try {
       g.setXORMode(component.getBackground());
       if(firstStretch == true)
         firstStretch = false;
       else
         drawLast(g);

       anchorPt.x = current.x + deltax;
       anchorPt.y = current.y + deltay;
       stretchedPt.x = current.x + current.width + deltax;
       stretchedPt.y = current.y + current.height + deltay;
       drawNext(g);
     }
     finally {
       g.dispose();
     } // try
    } // if
  }

  public boolean anchor(Point p) {
    if (current == null) return false;

    // have to decide which handle is closest, and use opposite corner
    minDiff = Integer.MAX_VALUE;
    testDiff( p, current.x, current.y, current.x + current.width, current.y + current.height);
    testDiff( p, current.x + current.width, current.y, current.x, current.y + current.height);
    testDiff( p, current.x, current.y + current.height, current.x + current.width, current.y);
    testDiff( p, current.x + current.width, current.y + current.height, current.x, current.y);

    // System.out.println(" anchor diff = "+minDiff);
    if (minDiff > 100) return false;

    stretchedPt.x = lastPt.x = p.x;
    stretchedPt.y = lastPt.y = p.y;

    firstStretch = true;
    return true;
  }

  private int minDiff = 0;
  private void testDiff( Point p, int x, int y, int anchor_x, int anchor_y) {
    int dx = p.x - x;
    int dy = p.y - y;
    int diff = dx*dx + dy*dy;
    if (diff < minDiff) {
      minDiff = diff;
      anchorPt.x = anchor_x;
      anchorPt.y = anchor_y;
    }
  }

  public void drawLast(Graphics2D graphics) {
    drawHandledRect( graphics, lastBounds(), handleSizePixels);
    //System.out.println("==RBRH last= "+lastBounds());
  }

  public void drawNext(Graphics2D graphics) {
    drawHandledRect( graphics, getBounds(), handleSizePixels);
    //System.out.println("==RBRH draw= "+getBounds());
  }

  static public void drawHandledRect(Graphics2D graphics, Rectangle2D rect, double handleSize) {
    double x = rect.getX();
    double y = rect.getY();
    double w = rect.getWidth();
    double h = rect.getHeight();

    graphics.draw( rect);
    Rectangle2D hr = new Rectangle2D.Double();

    // corners
    hr.setRect(x - handleSize/2, y - handleSize/2, handleSize, handleSize);
    graphics.fill( hr);
    hr.setRect(x + w - handleSize/2, y - handleSize/2, handleSize, handleSize);
    graphics.fill( hr);
    hr.setRect(x - handleSize/2, y + h - handleSize/2, handleSize, handleSize);
    graphics.fill( hr);
    hr.setRect(x + w - handleSize/2, y + h - handleSize/2, handleSize, handleSize);
    graphics.fill( hr);

    /* sides
    hr.setRect(x + w/2 - handleSize/2, y - handleSize/2, handleSize, handleSize);
    graphics.fill( hr);
    hr.setRect(x - handleSize/2, y + h/2 - handleSize/2, handleSize, handleSize);
    graphics.fill( hr);
    hr.setRect(x + w - handleSize/2, y + h/2 - handleSize/2, handleSize, handleSize);
    graphics.fill( hr);
    hr.setRect(x + w/2 - handleSize/2, y + h - handleSize/2, handleSize, handleSize);
    graphics.fill( hr); */
  }

}

/* Change History:
   $Log: RubberbandRectangleHandles.java,v $
   Revision 1.2  2004/09/24 03:26:39  caron
   merge nj22

   Revision 1.1  2004/05/21 05:57:35  caron
   release 2.0b

   Revision 1.1  2002/12/13 00:53:09  caron
   pass 2

   Revision 1.2  2002/04/29 22:26:57  caron
   minor

   Revision 1.1.1.1  2002/02/26 17:24:51  caron
   import sources
*/
