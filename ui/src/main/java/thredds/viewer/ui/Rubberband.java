// $Id: Rubberband.java 50 2006-07-12 16:30:06Z caron $
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
import java.awt.event.*;

/**
 * Implements XOR rubberbanding.
 * @author David M. Geary
 * @author John Caron
 * @version $Id: Rubberband.java 50 2006-07-12 16:30:06Z caron $
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
