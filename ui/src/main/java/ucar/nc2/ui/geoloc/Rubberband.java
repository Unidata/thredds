/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.ui.geoloc;

import java.awt.*;
import java.awt.event.*;
import java.io.Serializable;

/**
 * Implements XOR rubberbanding.
 * @author David M. Geary
 * @author John Caron
 */
abstract public class Rubberband implements Serializable {
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
