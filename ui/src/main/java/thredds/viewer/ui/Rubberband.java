// $Id: Rubberband.java,v 1.3 2004/09/24 03:26:39 caron Exp $
/*
 * Copyright 1997-2004 Unidata Program Center/University Corporation for
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
import java.awt.event.*;

/**
 * Implements XOR rubberbanding.
 * @author David M. Geary
 * @author John Caron
 * @version $Id: Rubberband.java,v 1.3 2004/09/24 03:26:39 caron Exp $
 */
abstract public class Rubberband {
  protected Point anchorPt    = new Point(0,0);
  protected Point stretchedPt = new Point(0,0);
  protected Point lastPt      = new Point(0,0);
  protected Point endPt       = new Point(0,0);

  protected Component component;
  protected boolean   firstStretch = true;
  private boolean   active = false;

  // actual drawing done by subclass.
  abstract public void drawLast(Graphics2D g);
  abstract public void drawNext(Graphics2D g);

  /**
   * Constructor. use if you want Rubberband to do the event listening.
   * @param c draw on top of this Component.
   */
  public Rubberband(Component c, boolean listen) {
     component = c;
     if (listen) setListeners();
  }

  /**
   * Set whether its in active mode. In active mode it listens for mouse drags and XOR draws.
   * @param b true if in active mode.
   */
  public void setActive(boolean b) { active = b; }
  public boolean isActive    () { return active;      }

  private void setListeners() {
    component.addMouseListener(new MouseAdapter() {
       public void mousePressed(MouseEvent event) {
        anchor(event.getPoint());
        setActive(true);
      }
      public void mouseReleased(MouseEvent event) {
        if (isActive()) end(event.getPoint());
        setActive(false);
      }
    });

    component.addMouseMotionListener( new MouseMotionAdapter() {
      public void mouseDragged(MouseEvent event) {
        if (isActive()) stretch(event.getPoint());
      }
    });
  }

  public Point   getAnchor   () { return anchorPt;    }
  public Point   getStretched() { return stretchedPt; }
  public Point   getLast     () { return lastPt;      }
  public Point   getEnd      () { return endPt;       }

  /**
   * Set the anchor point.
   */
  public boolean anchor(Point p) {
    firstStretch = true;
    anchorPt.x = p.x;
    anchorPt.y = p.y;

    stretchedPt.x = lastPt.x = anchorPt.x;
    stretchedPt.y = lastPt.y = anchorPt.y;

    return true;
  }

  /**
   * Erase the last rectangle and draw a new one from the anchor point to this point.
   */
  public void stretch(Point p) {
    lastPt.x      = stretchedPt.x;
    lastPt.y      = stretchedPt.y;
    stretchedPt.x = p.x;
    stretchedPt.y = p.y;

    Graphics2D g = (Graphics2D) component.getGraphics();
    if(g != null) {
     try {
       g.setXORMode(component.getBackground());
       if(firstStretch == true)
         firstStretch = false;
       else
         drawLast(g);
       drawNext(g);
     }
     finally {
       g.dispose();
     } // try
    } // if
  }

  /**
   * Last point, done with drawing.
   */
  public void end(Point p) {
    lastPt.x = endPt.x = p.x;
    lastPt.y = endPt.y = p.y;

    Graphics2D g = (Graphics2D) component.getGraphics();
    if(g != null) {
     try {
       g.setXORMode(component.getBackground());
       drawLast(g);
     }
     finally {
       g.dispose();
     }
    }
  }

  /**
   * Last point, done with drawing.
   */
  public void done() {
    Graphics2D g = (Graphics2D) component.getGraphics();
    if(g != null) {
     try {
       g.setXORMode(component.getBackground());
       drawLast(g);
     }
     finally {
       g.dispose();
     }
    }
  }

  /** Get current Bounds */
  public Rectangle getBounds() {
    return new Rectangle(stretchedPt.x < anchorPt.x ?
                         stretchedPt.x : anchorPt.x,
                         stretchedPt.y < anchorPt.y ?
                         stretchedPt.y : anchorPt.y,
                         Math.abs(stretchedPt.x - anchorPt.x),
                         Math.abs(stretchedPt.y - anchorPt.y));
  }

  /** Get previous Bounds */
  public Rectangle lastBounds() {
    return new Rectangle(
                lastPt.x < anchorPt.x ? lastPt.x : anchorPt.x,
                lastPt.y < anchorPt.y ? lastPt.y : anchorPt.y,
                Math.abs(lastPt.x - anchorPt.x),
                Math.abs(lastPt.y - anchorPt.y));
  }
}

/* Change History:
   $Log: Rubberband.java,v $
   Revision 1.3  2004/09/24 03:26:39  caron
   merge nj22

   Revision 1.2  2004/05/21 05:57:35  caron
   release 2.0b

   Revision 1.1  2002/12/13 00:53:09  caron
   pass 2

   Revision 1.2  2002/04/29 22:26:57  caron
   minor

   Revision 1.1.1.1  2002/02/26 17:24:51  caron
   import sources
*/
