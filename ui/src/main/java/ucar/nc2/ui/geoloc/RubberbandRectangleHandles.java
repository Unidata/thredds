/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.ui.geoloc;

import java.awt.*;
import java.awt.geom.*;

/** Rectangle Rubberbanding.
 * @author David M. Geary
 * @author John Caron
 */
public class RubberbandRectangleHandles extends Rubberband {
  public static double handleSizePixels = 6.0;

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
       if(firstStretch)
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

  public static void drawHandledRect(Graphics2D graphics, Rectangle2D rect, double handleSize) {
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
