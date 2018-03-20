/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.ui.widget;

import ucar.nc2.ui.geoloc.CursorMoveEvent;
import ucar.nc2.ui.geoloc.CursorMoveEventListener;
import ucar.nc2.ui.geoloc.PickEvent;
import ucar.nc2.ui.geoloc.PickEventListener;
import ucar.nc2.util.ListenerManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;

/* A simple version of NavigatedPanel.
 * Allows drawing on it in scaled (world) coordinates.
 * It doesnt do its own drawing, but allows the caller to obtain its
 * BufferedImage and draw in it itself.
 *
 * @author John Caron
 */

public class ScaledPanel extends JPanel {
  private Color backColor = Color.white;
  private Rectangle2D screenBounds = new Rectangle2D.Double();
  private ScaledPanel.Bounds worldBounds = new ScaledPanel.Bounds();
  private Point2D worldPt = new Point2D.Double();

  private AffineTransform transform = new AffineTransform();
  private BufferedImage bImage = null;
  private MyImageObserver imageObs = new MyImageObserver();

  private ListenerManager lmPick, lmMove;

  private boolean debugDraw = false, debugTransform = false, debugBounds = false;

  public ScaledPanel() {
    setDoubleBuffered(false);

    // catch window resize events
    addComponentListener(new ComponentAdapter() {
      public void componentResized(ComponentEvent e) {
        Rectangle nb = getBounds();
        boolean sameSize = (nb.width == screenBounds.getWidth()) && (nb.height == screenBounds.getHeight());
        if (debugBounds) System.out.println("TPanel setBounds old= " + screenBounds);
        screenBounds.setRect(nb);

        if ((bImage != null) && sameSize)
          return;

        if (debugBounds) System.out.println("  newBounds = " + nb);
        // create new buffer the size of the window
        if ((nb.width > 0) && (nb.height > 0)) {
          bImage = new BufferedImage(nb.width, nb.height, BufferedImage.TYPE_INT_RGB); // why RGB ?
        } else {
          bImage = null;
        }
        transform = null;
      }
    });

    // listen for mouse events: throw "pick" events
    addMouseListener(new MouseAdapter() {
      public void mousePressed(MouseEvent e) {
        if (lmPick.hasListeners() && (transform != null)) {
          try {
            transform.inverseTransform(e.getPoint(), worldPt);
            lmPick.sendEvent(new PickEvent(ScaledPanel.this, worldPt));
          } catch (NoninvertibleTransformException nte) {
            ;
          }
        }
      }
    });

    // listen for mouse movement: throw "Cursor Move" events
    addMouseMotionListener(new MouseMotionAdapter() {
      public void mouseMoved(MouseEvent e) {
        if (lmMove.hasListeners() && (transform != null)) {
          try {
            transform.inverseTransform(e.getPoint(), worldPt);
            lmMove.sendEvent(new CursorMoveEvent(ScaledPanel.this, worldPt));
          } catch (NoninvertibleTransformException nte) {
            ;
          }
        }
      }
    });

    // manage Event Listener's
    lmPick = new ListenerManager(
            "ucar.nc2.ui.geoloc.PickEventListener",
            "ucar.nc2.ui.geoloc.PickEvent",
            "actionPerformed");

    lmMove = new ListenerManager(
            "ucar.nc2.ui.geoloc.CursorMoveEventListener",
            "ucar.nc2.ui.geoloc.CursorMoveEvent",
            "actionPerformed");
  }

  /**
   * Register a CursorMoveEventListener.
   */
  public void addCursorMoveEventListener(CursorMoveEventListener l) {
    lmMove.addListener(l);
  }

  /**
   * Remove a CursorMoveEventListener.
   */
  public void removeCursorMoveEventListener(CursorMoveEventListener l) {
    lmMove.removeListener(l);
  }

  /**
   * Register a PickEventListener.
   */
  public void addPickEventListener(PickEventListener l) {
    lmPick.addListener(l);
  }

  /**
   * Remove a PickEventListener.
   */
  public void removePickEventListener(PickEventListener l) {
    lmPick.removeListener(l);
  }

  /* set the bounds of the world coordinates.
  * The point (world.getX(), world.getY()) is mapped to the lower left point of the screen.
  * The point (world.getX() + world.Width(), world.getY()+world.Height()) is mapped
  * to the upper right corner. Therefore if coords decrease as you go up, world.Height()
  * should be negetive.
  */

  public void setWorldBounds(ScaledPanel.Bounds world) {
    worldBounds.set(world);
    transform = null;
    if (debugBounds) System.out.println("  setWorldBounds = " + worldBounds);
  }

  public ScaledPanel.Bounds getWorldBounds() {
    return worldBounds;
  }

  /**
   * User must get this Graphics2D and draw into it when panel needs redrawing
   */
  public Graphics2D getBufferedImageGraphics() {
    if (bImage == null)
      return null;
    Graphics2D g2 = bImage.createGraphics();

    // set graphics attributes
    if (transform == null)
      transform = calcTransform(screenBounds, worldBounds);
    g2.setTransform(transform);
    g2.setStroke(new BasicStroke(0.0f));      // default stroke size is one pixel
    g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);
    g2.setBackground(backColor);
    g2.setClip(worldBounds.getRect());

    return g2;
  }

  /**
   * System-triggered redraw.
   */
  public void paintComponent(Graphics g) {
    if (bImage != null)
      g.drawImage(bImage, 0, 0, backColor, imageObs);
  }

  public void drawNow() {
    Graphics g = getGraphics();
    if (null != g) {
      paintComponent(g);
      g.dispose();
    }
  }

  /**
   * map world coords to screen coords.
   *
   * @param world  world coordinate rectangle
   * @param screen screen coordinate rectangle
   * @return AffineTransform for converting world to screen.
   */
  private AffineTransform calcTransform(Rectangle2D screen, Bounds world) {
    // scale to limiting dimension
    double xs = screen.getWidth() / (world.getRight() - world.getLeft());
    double ys = screen.getHeight() / (world.getLower() - world.getUpper());

    AffineTransform cat = new AffineTransform();
    cat.setToScale(xs, ys);
    cat.translate(-world.getLeft(), -world.getUpper());

    if (debugTransform) {
      System.out.println("TPanel calcTransform = ");
      System.out.println("  screen = " + screen);
      System.out.println("  world = " + world);

      System.out.println("  transform = " + cat.getScaleX() + " " + cat.getShearX() + " " + cat.getTranslateX());
      System.out.println("              " + cat.getShearY() + " " + cat.getScaleY() + " " + cat.getTranslateY());

      Point2D src = new Point2D.Double(world.getLeft(), world.getUpper());
      Point2D dst = new Point2D.Double(0.0, 0.0);

      System.out.println("  upper left pt = " + src);
      System.out.println("  transform = " + cat.transform(src, dst));

      src = new Point2D.Double(world.getRight(), world.getLower());
      System.out.println("  lower right pt = " + src);
      System.out.println("  transform = " + cat.transform(src, dst));
    }
    return cat;
  }

  // necessary for g.drawImage()

  private static class MyImageObserver implements ImageObserver {
    @Override
    public boolean imageUpdate(Image image, int flags, int x, int y, int width, int height) {
      return true;
    }
  }

  /**
   * Why the heck did I create yet another Rectangle?
   * Because I needed some precise semantics, especially for the y screen coordinate.
   * "upper" is what you want to be placed on the upper part of the screen,
   * "lower" on the lower part.
   */
  public static class Bounds {
    private double left, right, upper, lower;

    public Bounds() {
      this(0.0, 0.0, 0.0, 0.0);
    }

    public Bounds(double left, double right, double upper, double lower) {
      this.left = left;
      this.right = right;
      this.upper = upper;
      this.lower = lower;
    }

    public double getUpper() {
      return upper;
    }

    public double getLower() {
      return lower;
    }

    public double getRight() {
      return right;
    }

    public double getLeft() {
      return left;
    }

    public void set(Bounds b) {
      this.upper = b.getUpper();
      this.lower = b.getLower();
      this.right = b.getRight();
      this.left = b.getLeft();
    }

    public Rectangle2D getRect() {
      return new Rectangle2D.Double(Math.min(left, right), Math.min(lower, upper),
              Math.abs(right - left), Math.abs(lower - upper));
      //return new Rectangle2D.Double(Math.min(left,right), Math.min(lower,upper),
      //  Math.abs(right-left), Math.abs(lower-upper));
    }

    public String toString() {
      return "left: " + left + " right: " + right + " upper: " + upper + " lower: " + lower;
    }

  }

  public static void main(String[] argv) {

    Rectangle2D w = new Rectangle2D.Double(1.0, 1.0, 10.0, 10.0);
    Rectangle2D s = new Rectangle2D.Double(0.0, 0.0, 200, 100);

    double xs = s.getWidth() / w.getWidth();
    double ys = s.getHeight() / w.getHeight();

    AffineTransform cat = new AffineTransform();
    cat.setToScale(xs, -ys);
    cat.translate(-w.getX(), -w.getY() - w.getHeight());

    Point2D src = new Point2D.Double(1.0, 1.0);
    Point2D dst = new Point2D.Double(0.0, 0.0);

    System.out.println("  screen = " + s);
    System.out.println("  world = " + w);

    System.out.println("  pt = " + src);
    System.out.println("  transform = " + cat.transform(src, dst));

    src = new Point2D.Double(11.0, 11.0);
    System.out.println("  pt = " + src);
    System.out.println("  transform = " + cat.transform(src, dst));
  }

}
