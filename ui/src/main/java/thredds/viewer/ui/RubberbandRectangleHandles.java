// $Id$
/*
 * Copyright 1997-2006 Unidata Program Center/University Corporation for
 * Atmospheric Research, P.O. Box 3000, Boulder, CO 80307,
 * support@unidata.ucar.edu.
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or (at
 * your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser
 * General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */

package thredds.viewer.ui;

import java.awt.*;
import java.awt.geom.*;

/** Rectangle Rubberbanding.
 * @author David M. Geary
 * @author John Caron
 * @version $Id$
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
