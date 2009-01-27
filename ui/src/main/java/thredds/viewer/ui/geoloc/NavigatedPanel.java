// $Id: NavigatedPanel.java 50 2006-07-12 16:30:06Z caron $
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
import ucar.unidata.geoloc.projection.*;
import thredds.viewer.ui.*;

import thredds.ui.BAMutil;
import ucar.nc2.ui.util.ListenerManager;
import ucar.unidata.util.Format;
import ucar.util.prefs.ui.Debug;

import java.awt.*;
import java.awt.geom.*;
import java.awt.datatransfer.*;
import java.awt.dnd.*;
import java.awt.event.*;
import java.awt.image.*;
//import java.awt.print.*;
import java.io.*;
import javax.swing.*;

/**
 * Implements a "navigated" JPanel within which a user can zoom and pan.
 *
 * The mapping of the screen area to world coordinates is called "navigation", and
 * it's NavigatedPanel's job to keep track of the navigation as the user zooms and pans.
 * It throws NewMapAreaEvent to indicate that the user has changed the Map area,
 * and the display needs to be redrawn. It throws PickEvents when the user double clicks
 * on the panel. <br>
 * <br>
 * NavigatedPanel has a standard JToolbar that can be displayed.
 * It also implements a "reference" point and fast updating of the
 * status of the mouse position relative to the reference point. <br>
 * <br>
 * A user typically adds a NavigatedPanel and its toolbar to its frame/applet, and
 * registers itself for NewMapAreaEvent's. See NPController class for an example.
 * When an event occurs, the user obtains
 * a Graphics2D (through the getBufferedImageGraphics() method) to draw into. The
 * AffineTransform of the Graphics2D has been set correctly to map projection coords
 * to screen coords, based on the current zoomed and panned Map area.
 * The renderer can use the AffineTransform if needed, but more typically just works in
 * projection coordinates. The renderer can also get a clipping rectangle by calling
 * g.getClip() and casting the Shape to a Rectangle2D, eg:
 * <pre>
 *       Rectangle2D clipRect = (Rectangle2D) g.getClip();
 * </pre>
 *
 * Our "world coordinates" are the same as java2D's "user coordinates".
 * In general, the world coordinate plane is a projective geometry surface, typically
 * in units of "km on the projection surface".
 * The transformation from lat/lon to the projection plane is handled by a ProjectionImpl object.
 * If a user selects a different projection, NavigatedPanel.setProjection() should be called.
 * The default projection is "Cylindrical Equidistant" or "LatLon" which simply maps lat/lon
 * degrees linearly to screen coordinates. A peculiarity of this projection is that the "seam"
 * of the cylinder shifts as the user pans around. Currently our implementation sends
 * a NewMapAreaEvent whenever this happens. <br>
 * <br>
 * <br>
 *
 * @see NPController
 * @author John Caron
 * @version $Id: NavigatedPanel.java 50 2006-07-12 16:30:06Z caron $
 */

public class NavigatedPanel extends JPanel {

  /* Implementation Notes:
     - NavigatedPanel uses an image to buffer the image.
     - geoSelection being kept in Projection coords; should probably be LatLon,
       following reference point implementation. (5/16/04)
     - bug where mouse event / image are sometimes not correctly registered when first starting up.
       goes away when you do a manual resize. (5/16/04) may depend on jgoodies widget being
       in parent layout ?
   */

  // public actions;
  public AbstractAction setReferenceAction;

  // main delegates
  private Navigation navigate = null;
  private ProjectionImpl project = null;

  // ui stuff
  private BufferedImage bImage = null;
  private Color backColor = Color.white;
  private JLabel statusLabel = null;
  private myImageObserver imageObs = new myImageObserver();
  private NToolBar toolbar = null;

  // scheduled redraw
  private javax.swing.Timer redrawTimer = null;

  // state flags
  private boolean changedSinceDraw = true;
  private boolean changeable = true; // user allowed to change zoom/pan

  // dragging and zooming state
  private int startx, starty, deltax, deltay;
  private boolean panningMode = false;
  private boolean zoomingMode = false;
  private Rubberband zoomRB;

  // track reference point
  private boolean isReferenceMode = false, hasReference = false;
  private ProjectionPointImpl refWorld = new ProjectionPointImpl();
  private LatLonPointImpl refLatLon = new LatLonPointImpl();
  private Point2D refScreen = new Point2D.Double();
  private int referenceSize = 12;
  private Cursor referenceCursor = null;
  private static final int REFERENCE_CURSOR = -100;

  // selecting geo area state
  private ProjectionRect geoSelection = null;
  private boolean geoSelectionMode = false, moveSelectionMode = false;
  private RubberbandRectangleHandles selectionRB;

  // event management
  private ListenerManager lmPick;
  private ListenerManager lmMove;
  private ListenerManager lmMapArea;
  private ListenerManager lmProject;
  private ListenerManager lmGeoSelect;

  // some working objects to minimize excessive garbage collection
  private StringBuffer sbuff = new StringBuffer(100);
  private ProjectionPointImpl workW = new ProjectionPointImpl();
  private LatLonPointImpl workL = new LatLonPointImpl();
  private Point2D workS = new Point2D.Double();
  private Bearing workB = new Bearing();
  private Rectangle myBounds = new Rectangle();
  private ProjectionRect boundingBox = new ProjectionRect();

  // DnD
  private DropTarget dropTarget;

  //debug
  private int repaintCount = 0;
  private final boolean debugDraw = false, debugEvent = false, debugThread = false,
      debugStatus = false;
  private final boolean debugTime = false, debugPrinting = false, debugBB = false,
      debugZoom = false;
  private final boolean debugBounds = false, debugSelection = false, debugNewProjection = false;

  /** The constructor. */
  public NavigatedPanel() {
    setDoubleBuffered(false); // we do our own dubble buffer

    // default navigation and projection
    navigate = new Navigation(this);
    project = new LatLonProjection("Cyl.Eq"); // default projection
    navigate.setMapArea(project.getDefaultMapArea());

    // toolbar actions
    makeActions();

    // listen for mouse events
    addMouseListener(new myMouseListener());
    addMouseMotionListener(new myMouseMotionListener());

    // catch resize events
    addComponentListener(new ComponentAdapter() {
      public void componentResized(ComponentEvent e) {
        newScreenSize(getBounds());
      }
    });

    // rubberbanding
    zoomRB = new RubberbandRectangle( this, false);
    selectionRB = new RubberbandRectangleHandles( this, false);

    // DnD
    dropTarget = new DropTarget(this, DnDConstants.ACTION_COPY,
                                new myDropTargetListener());

    // manage Event Listener's
    lmPick = new ListenerManager(
        "thredds.viewer.ui.geoloc.PickEventListener",
        "thredds.viewer.ui.geoloc.PickEvent",
        "actionPerformed");

    lmMove = new ListenerManager(
        "thredds.viewer.ui.geoloc.CursorMoveEventListener",
        "thredds.viewer.ui.geoloc.CursorMoveEvent",
        "actionPerformed");

    lmMapArea = new ListenerManager(
        "thredds.viewer.ui.geoloc.NewMapAreaListener",
        "thredds.viewer.ui.geoloc.NewMapAreaEvent",
        "actionPerformed");

    lmProject = new ListenerManager(
        "thredds.viewer.ui.geoloc.NewProjectionListener",
        "thredds.viewer.ui.geoloc.NewProjectionEvent",
        "actionPerformed");

    lmGeoSelect = new ListenerManager(
        "thredds.viewer.ui.geoloc.GeoSelectionListener",
        "thredds.viewer.ui.geoloc.GeoSelectionEvent",
        "actionPerformed");

  }

 public LatLonRect getGeoSelectionLL() {
    return geoSelection == null ? null : project.projToLatLonBB( geoSelection);
  }
  public ProjectionRect getGeoSelection() { return geoSelection; }
  public void setGeoSelection(ProjectionRect bb) {
    geoSelection = bb;
  }
  public void setGeoSelection(LatLonRect llbb) {
    setGeoSelection( project.latLonToProjBB(llbb));
  }

  public void setGeoSelectionMode(boolean b) {
    geoSelectionMode = b;
  }

  // event management

    /** Register a NewMapAreaListener. */
  public void addNewMapAreaListener( NewMapAreaListener l) {
    lmMapArea.addListener(l);
  }
  /** Remove a NewMapAreaListener. */
  public void removeNewMapAreaListener( NewMapAreaListener l) {
    lmMapArea.removeListener(l);
  }

  /** Register a NewProjectionListener. The only time this is called is when the
   *  projection isLatLon, and a zoom or pan crosses the seam.
   */
  public void addNewProjectionListener( NewProjectionListener l) {
    lmProject.addListener(l);
  }
    /** Remove a NewProjectionListener. */
  public void removeNewProjectionListener( NewProjectionListener l) {
    lmProject.removeListener(l);
  }

    /** Register a CursorMoveEventListener. */
  public void addCursorMoveEventListener( CursorMoveEventListener l) {
    lmMove.addListener(l);
  }
    /** Remove a CursorMoveEventListener. */
  public void removeCursorMoveEventListener( CursorMoveEventListener l) {
    lmMove.removeListener(l);
  }
    /** Register a PickEventListener. */
  public void addPickEventListener( PickEventListener l) {
    lmPick.addListener(l);
  }
    /** Remove a PickEventListener. */
  public void removePickEventListener( PickEventListener l) {
    lmPick.removeListener(l);
  }
    /** Register a PickEventListener. */
  public void addGeoSelectionListener( GeoSelectionListener l) {
    lmGeoSelect.addListener(l);
  }
    /** Remove a PickEventListener. */
  public void removeGeoSelectionListener( GeoSelectionListener l) {
    lmGeoSelect.removeListener(l);
  }

  // called by Navigation
  void fireMapAreaEvent() {
    if (debugZoom)
      System.out.println("NP.fireMapAreaEvent ");

     // decide if we need a new Projection: for LatLonProjection only
    if (project.isLatLon()) {
      LatLonProjection llproj = (LatLonProjection) project;
      ProjectionRect box = getMapArea();
      double center = llproj.getCenterLon();
      double lonBeg = LatLonPointImpl.lonNormal(box.getMinX(), center);
      double lonEnd = lonBeg + box.getMaxX() - box.getMinX();
      boolean showShift = Debug.isSet("projection/LatLonShift") || debugNewProjection;
      if (showShift) System.out.println("projection/LatLonShift: min,max = "+ box.getMinX()+" "+
            box.getMaxX()+" beg,end= "+lonBeg+" "+lonEnd+ " center = "+center);

      if ( (lonBeg < center-180) || (lonEnd > center+180)) {  // got to do it
        double wx0 = box.getX() + box.getWidth()/2;
        llproj.setCenterLon( wx0);              // shift cylinder seam
        double newWx0 = llproj.getCenterLon();  // normalize wx0 to [-180,180]
        setWorldCenterX(newWx0);             // tell navigation panel to shift
        if (showShift)
          System.out.println("projection/LatLonShift: shift center to "+wx0+"->"+newWx0);

        // send projection event instead of map area event
        lmProject.sendEvent( new NewProjectionEvent(this, llproj));
        return;
      }
    }

    // send new map area event
    lmMapArea.sendEvent( new NewMapAreaEvent( this));
  }


  // accessor methods
  /* Get the Navigation. */
  public Navigation getNavigation() { return navigate; }

  /* Get the background color of the NavigatedPanel. */
  public Color getBackgroundColor() { return backColor; }

  /** Set the Map Area. */
  public void setMapArea(ProjectionRect ma) {
    if (debugBB) System.out.println("NP.setMapArea "+ ma);
    navigate.setMapArea(ma);
  }
  /** Set the Map Area by converting LatLonRect to a ProjectionRect.*/
  public void setMapArea(LatLonRect llbb) {
    if (debugBB) System.out.println("NP.setMapArea (ll) "+ llbb);
    navigate.setMapArea( project.latLonToProjBB(llbb));
  }
  /** Get the current Map Area */
  public ProjectionRect getMapArea() {
    return navigate.getMapArea(boundingBox);
  }
  /** Get the current Map Area as a lat/lon bounding box */
  public LatLonRect getMapAreaLL() {
    return project.projToLatLonBB( getMapArea());
  }

  /** kludgy thing to shift LatLon seam */
  public void setWorldCenterX( double wx_center) {
    navigate.setWorldCenterX( wx_center);
  }

  /** set the center point of the MapArea */
  public void setLatLonCenterMapArea( double lat, double lon) {
    ProjectionPointImpl center = project.latLonToProj(lat, lon);

    ProjectionRect ma = getMapArea();
    ma.setX( center.getX() - ma.getWidth()/2);
    ma.setY( center.getY() - ma.getHeight()/2);

    setMapArea(ma);
  }

    /** Get the current Projection.*/
  public ProjectionImpl getProjectionImpl() { return project; }

    /** Set the Projection, change the Map Area to the projection's default. */
  public void setProjectionImpl( ProjectionImpl p) {
      // transfer selection region to new coord system
    if (geoSelection != null)  {
      LatLonRect geoLL = project.projToLatLonBB(geoSelection);
      setGeoSelection( p.latLonToProjBB( geoLL));
    }

      // switch projections
    project = p;
    navigate.setMapArea(project.getDefaultMapArea());
    if (Debug.isSet("projection/set") || debugNewProjection)
      System.out.println("projection/set NP="+ project);

      // transfer reference point to new coord system
    if (hasReference)  {
      refWorld.setLocation(project.latLonToProj( refLatLon));
    }

  }

    /** The status label is where the lat/lon position of the mouse is displayed. May be null.
      @param statusLabel the Jlabel to write into */
  public void setPositionLabel(JLabel statusLabel) { this.statusLabel = statusLabel; }

    /** @return the "Navigate" toolbar */
  public JToolBar getNavToolBar () { return new NToolBar(); }

    /** @return the "Move" toolbar */
  public JToolBar getMoveToolBar () { return new MoveToolBar(); }

    /** Add all of the toolbar's actions to a menu.
      @param menu the menu to add the actions to */
  public void addActionsToMenu (JMenu menu) {
    BAMutil.addActionToMenu( menu, zoomIn);
    BAMutil.addActionToMenu( menu, zoomOut);
    BAMutil.addActionToMenu( menu, zoomBack);
    BAMutil.addActionToMenu( menu, zoomDefault);

    menu.addSeparator();

    BAMutil.addActionToMenu( menu, moveUp);
    BAMutil.addActionToMenu( menu, moveDown);
    BAMutil.addActionToMenu( menu, moveRight);
    BAMutil.addActionToMenu( menu, moveLeft);

    menu.addSeparator();

    BAMutil.addActionToMenu( menu, setReferenceAction);
  }

  public void setEnabledActions(boolean b) {
    zoomIn.setEnabled( b);
    zoomOut.setEnabled( b);
    zoomBack.setEnabled( b);
    zoomDefault.setEnabled( b);
    moveUp.setEnabled( b);
    moveDown.setEnabled( b);
    moveRight.setEnabled( b);
    moveLeft.setEnabled( b);
  }

      // Make sure we dont get overwhelmed by redraw calls
      // from panning, so wait delay msecs before doing the redraw.
  private void redrawLater(int delay) {
    boolean already = (redrawTimer != null) && (redrawTimer.isRunning());
    if (debugThread) System.out.println( "redrawLater isRunning= "+ already);
    if (already)
      return;

      // initialize Timer the first time
    if (redrawTimer == null) {
      redrawTimer = new javax.swing.Timer(0, new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          drawG();
          redrawTimer.stop(); // one-shot timer
        }
      });
    }
      // start the timer running
    redrawTimer.setDelay(delay);
    redrawTimer.start();
  }

    /** sets whether the user can zoom/pan on this NavigatedPanel. Default = true.
     * @param mode  set to false if user can't zoom/pan
     */
  public void setChangeable (boolean mode) {
    if (mode == changeable)
      return;
    changeable = mode;
    if (toolbar != null)
      toolbar.setEnabled(mode);
  }

  /** catch repaints - for debugging */
  // note: I believe that the RepaintManager is not used on JPanel subclasses ???
  public void repaint(long tm, int x, int y, int width, int height) {
    if (debugDraw) System.out.println("REPAINT "+ repaintCount+
        " x "+ x+ " y "+ y+ " width "+ width+ " heit "+ height);
    if (debugThread) System.out.println(" thread = "+ Thread.currentThread());
    repaintCount++;
    super.repaint(tm, x, y, width, height);
  }

  /** System-triggered redraw. */
  public void paintComponent(Graphics g) {
    if (debugDraw) System.out.println( "System called paintComponent clip= "+ g.getClipBounds());
    draw( (Graphics2D) g);
  }

  /** This is used to do some fancy tricks with double buffering */
  public BufferedImage getBufferedImage() { return bImage; }

  /** User must get this Graphics2D and draw into it when panel needs redrawing */
  public Graphics2D getBufferedImageGraphics() {
    if (bImage == null)
      return null;
    Graphics2D g2 = bImage.createGraphics();

    // set clipping rectangle into boundingBox
    navigate.getMapArea( boundingBox);
    if (debugBB) System.out.println(" getBufferedImageGraphics BB = "+ boundingBox);

    // set graphics attributes
    g2.setTransform( navigate.getTransform());
    g2.setStroke(new BasicStroke(0.0f));      // default stroke size is one pixel
    g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);
    g2.setClip( boundingBox);                 // normalized coord system, because transform is applied
    g2.setBackground( backColor);

    return g2;
  }

  //////////////////////// printing ////////////////////////////////
  /** utility routine for printing.
   * @param pwidth : widtht of the page, units are arbitrary
   * @param pheight : height of the page, units are arbitrary
   * @return true if we want to rotate the page
   */
  public boolean wantRotate(double pwidth, double pheight) {
    return navigate.wantRotate(pwidth, pheight);
  }

  /** This calculates the Affine Transform that maps the current map area (in Projection Coordinates)
   * to a display area (in arbitrary units).
   * @param rotate should the page be rotated?
   * @param displayX            upper right corner of display area
   * @param displayY            upper right corner of display area
   * @param displayWidth   display area
   * @param displayHeight   display area
   *
   * see Navigation.calcTransform
   */
  public AffineTransform calcTransform(boolean rotate, double displayX, double displayY,
      double displayWidth, double displayHeight) {
    return navigate.calcTransform(rotate, displayX, displayY, displayWidth, displayHeight);
  }


    // LOOK! change this to an inner class ?
    /* Render to a printer. part of Printable interface
      @param g        the Graphics context
      @param pf       describes the page format
      @param pi       page number

  public int print(Graphics g, PageFormat pf, int pi) throws PrinterException {
    if (pi >= 1) {
      return Printable.NO_SUCH_PAGE;
    }
    Graphics2D g2 = (Graphics2D) g;
    g2.setColor(Color.black);

    double pheight = pf.getImageableHeight();
    double pwidth = pf.getImageableWidth();
    double px = pf.getImageableX();
    double py = pf.getImageableY();
    g2.drawRect( (int) px, (int) py, (int) pwidth, (int)pheight);

    AffineTransform orgAT = g2.getTransform();
    if (debugPrinting) System.out.println(" org transform = "+orgAT);

    //  set clipping rectangle LOOK ????
    //navigate.getMapArea( boundingBox);
    //g2.setClip( boundingBox);

    boolean rotate = navigate.wantRotate(pwidth, pheight);
    AffineTransform at2 = navigate.calcTransform(rotate, px, py, pwidth, pheight);
    g2.transform( at2);
    AffineTransform at = g2.getTransform();
    if (debugPrinting) System.out.println(" use transform = "+at);
    double scale = at.getScaleX();

    // if we need to rotate, also rotate the original transform
    if (rotate)
      orgAT.rotate( -Math.PI/2, px + pwidth/2, py + pheight/2);

      // set graphics attributes                           // LOOK! hanging printer
    //g2.setStroke(new BasicStroke((float)(2.0/scale)));  // default stroke size is two pixels
    g2.setStroke(new BasicStroke(0.0f));  // default stroke size is two pixels
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
    g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);

     // draw the image to the buffer
    //render.draw(g2, orgAT);

    if (debugPrinting) {
      System.out.println("  Graphics clip "+ g2.getClipBounds());
      System.out.println("  Page Format     "+ pf.getOrientation());
      System.out.println("  getH/W          "+ pf.getHeight()+ " "+ pf.getWidth());
      System.out.println("  getImageableH/W "+ pf.getImageableHeight()+ " "+ pf.getImageableWidth());
      System.out.println("  getImageableX/Y "+ pf.getImageableX()+ " "+ pf.getImageableY());

      /* Paper paper = pf.getPaper();
      System.out.println("  Paper     ");
      System.out.println("  getH/W          "+ paper.getHeight()+ " "+ paper.getWidth());
      System.out.println("  getImageableH/W "+ paper.getImageableHeight()+ " "+ paper.getImageableWidth());
      System.out.println("  getImageableX/Y "+ paper.getImageableX()+ " "+ paper.getImageableY());
    }

    return Printable.PAGE_EXISTS;
  }   */

  ///////////////////////////////////////////////////////////////////////////////////
  // private methods

      // when component resizes we need a new buffer
  private void newScreenSize(Rectangle b) {
    boolean sameSize = (b.width == myBounds.width) && (b.height == myBounds.height);
    if (debugBounds) System.out.println( "NavigatedPanel newScreenSize old= "+myBounds);
    if (sameSize && (b.x == myBounds.x) && (b.y == myBounds.y))
      return;

    myBounds.setBounds(b);
    if (sameSize)
      return;

    if (debugBounds) System.out.println( "  newBounds = " +b);

    // create new buffer the size of the window
    //if (bImage != null)
    //  bImage.dispose();

    if ((b.width > 0) && (b.height > 0)) {
      bImage = new BufferedImage(b.width, b.height, BufferedImage.TYPE_INT_RGB); // why RGB ?
    } else {                                                                     // why not device dependent?
      bImage = null;
    }

   navigate.setScreenSize(b.width, b.height);
  }

  // draw and drawG are like "paintImmediately()"
  public void drawG() {
    Graphics g = getGraphics();    // bypasses double buffering ?
    if (null != g) {
      draw( (Graphics2D) g);
      g.dispose();
    }
  }
  private void draw(Graphics2D g) {
    if (bImage == null)
      return;
    boolean redrawReference = true;
    Rectangle bounds = getBounds();

    if (panningMode) {
      if (debugDraw) System.out.println( "draw draggingMode ");
          // Clear the image.
      g.setBackground(backColor);
      g.clearRect(0, 0, bounds.width, bounds.height);
      g.drawImage( bImage, deltax, deltay, backColor, imageObs);
      redrawReference = false;
   } else {
      if (debugDraw) System.out.println( "draw copy ");
      g.drawImage( bImage, 0, 0, backColor, imageObs);
   }

   if (hasReference && redrawReference) {
     refWorld.setLocation(project.latLonToProj( refLatLon));
     navigate.worldToScreen(refWorld, refScreen);
     int px = (int) refScreen.getX();
     int py = (int) refScreen.getY();
     g.setColor( Color.red);
     g.setStroke(new BasicStroke(2.0f));
     // g.drawImage( referenceCursor.getImage(), px, py, Color.red, imageObs);
     g.drawLine(px, py-referenceSize, px, py+referenceSize);
     g.drawLine(px-referenceSize, py, px+referenceSize, py);
   }

   // clean up
   changedSinceDraw = false;
  }

  private void setCursor(int what) {
    if (what == REFERENCE_CURSOR) {
      if (null == referenceCursor) {
        referenceCursor = BAMutil.makeCursor("ReferencePoint");
        if (null == referenceCursor)
          return;
      }
      super.setCursor(referenceCursor);
    } else
      super.setCursor(Cursor.getPredefinedCursor(what));
  }

  private void showStatus(int mousex, int mousey) {
    if ((statusLabel == null) && !lmMove.hasListeners())
      return;

    workS.setLocation( mousex, mousey);
    navigate.screenToWorld( workS, workW);
    workL.set(project.projToLatLon( workW));
    if (lmMove.hasListeners())
      lmMove.sendEvent( new CursorMoveEvent(this, workW));

    if (statusLabel == null)
      return;

    sbuff.setLength(0);
    sbuff.append(workL.toString());
    if (ucar.util.prefs.ui.Debug.isSet("projection/showPosition")) {
      sbuff.append(" "+workW);
    }
    if (hasReference) {
        Bearing.calculateBearing(refLatLon, workL, workB);
        sbuff.append("  (");
        sbuff.append(Format.dfrac(workB.getAngle(), 0));
        sbuff.append(" deg ");
        sbuff.append(Format.d(workB.getDistance(), 4, 5));
        sbuff.append(" km)");
    }

    statusLabel.setText(sbuff.toString());
  }

  private void setReferenceMode() {
    if (isReferenceMode) {   // toggle
      isReferenceMode = false;
      setCursor(Cursor.DEFAULT_CURSOR);
      statusLabel.setToolTipText("position at cursor");
      drawG();
    } else {
      hasReference = false;
      isReferenceMode = true;
      setCursor(Cursor.CROSSHAIR_CURSOR);
      statusLabel.setToolTipText("position (bearing)");
    }
  }

  //////////////////////////////////////////////////////////////
  // inner classes

  /* myMouseListener and myMouseMotionListener implements the standard mouse behavior.

     A: pan:
       * press right : start panning mode
       * drag : pan image
       * release: redraw with new area

     B: zoom:
       * press left : start zooming mode
       * drag : draw rubberband
       * release: redraw with new area

     C: move:
       * show cursor location  / distance from reference point

     D. click:
       * (reference mode) : set reference point
       * (not reference mode): throw pick event.

     E: selection:
       * press left : find anchor point = diagonal corner
       * drag left : draw rubberband
       * press right : start move selection mode
       * drag right : move selection
       * release: throw new selection event
    */

  private class myMouseListener extends MouseAdapter {

    public void mouseClicked( MouseEvent e) {
      // pick event
      if (isReferenceMode) {
        hasReference = true;
        refScreen.setLocation(e.getX(), e.getY());
        navigate.screenToWorld(refScreen, refWorld);
        refLatLon.set(project.projToLatLon(refWorld));
        setCursor(REFERENCE_CURSOR);
        drawG();
      } else {
        workS.setLocation(e.getX(), e.getY());
        navigate.screenToWorld(workS, workW);
        lmPick.sendEvent( new PickEvent(NavigatedPanel.this, workW));
      }
    }

    public void mousePressed( MouseEvent e) {
      if (!changeable)
        return;

      startx = e.getX();
      starty = e.getY();
      if (debugSelection) System.out.println(" NP press="+e.getPoint());

      // geoSelectionMode
      if (geoSelectionMode && (geoSelection != null)) {
        if (e.getClickCount() == 2) {
          ProjectionPointImpl pp = new ProjectionPointImpl();
          navigate.screenToWorld(e.getPoint(), pp);
          // make geoSelection fit in screen
          geoSelection.setWidth( boundingBox.getWidth()/4);
          geoSelection.setHeight( boundingBox.getHeight()/4);
          // make it the center point
          geoSelection.setX( pp.getX() - geoSelection.getWidth()/2);
          geoSelection.setY( pp.getY() - geoSelection.getHeight()/2);
          lmGeoSelect.sendEvent( new GeoSelectionEvent( this, geoSelection));
          return;
        }

        // CTRL is down, but SHIFT is up 
        int onmask = InputEvent.CTRL_DOWN_MASK;
        int offmask = InputEvent.SHIFT_DOWN_MASK;
        if (onmask == (e.getModifiersEx() & (onmask | offmask))) {
          Rectangle sw = navigate.worldToScreen(geoSelection);
          selectionRB.setRectangle(sw);
          if (debugSelection) System.out.println("NB start selection=" + geoSelection + " => " + sw);

          if (SwingUtilities.isRightMouseButton(e)) { // pan
            if (sw.contains( e.getPoint())) {
              moveSelectionMode = true;
              setCursor(Cursor.MOVE_CURSOR);
              selectionRB.setActive(true);
            }
          } else { // zoom
            if (selectionRB.anchor(e.getPoint())) {
              selectionRB.setActive(true);
              if (debugSelection) System.out.println("  anchor at =" + selectionRB.getAnchor());
            }
          }
          return;
        }
      } // geoSelectionMode

      if (!SwingUtilities.isRightMouseButton(e)) { // left and center mouse
        // zoom mapArea
        zoomRB.anchor(e.getPoint());
        zoomRB.setActive(true);
        zoomingMode = true;

      } else {  // right mouse = pan
        // pan mapArea
        panningMode = true;
        setCursor(Cursor.MOVE_CURSOR);
      }

      if (debugEvent) System.out.println( "mousePressed "+startx+" "+starty);
    }

    public void mouseReleased( MouseEvent e) {
      if (!changeable)
        return;
      // System.out.println(" NP release="+e.getPoint());

      deltax = e.getX() - startx;
      deltay = e.getY() - starty;
      if (debugEvent) System.out.println( "mouseReleased "+e.getX()+" "+e.getY()+
                "="+deltax+" "+deltay);

      if (geoSelectionMode && selectionRB.isActive()) {
        selectionRB.setActive(false);
        selectionRB.done();
        moveSelectionMode = false;
        setCursor(Cursor.DEFAULT_CURSOR);

        if (!NavigatedPanel.this.contains(e.getPoint())) { // point is off the panel
          if (debugBounds) System.out.println("NP.select: point "+e.getPoint()+" out of bounds: "+myBounds);
          return;
        }

        geoSelection = navigate.screenToWorld( selectionRB.getAnchor(), selectionRB.getLast());

        // System.out.println(" NP selection rect= "+selectionRB.getAnchor()+" "+selectionRB.getLast()+
        //                   " => "+rect);
            // send new map area event
        lmGeoSelect.sendEvent( new GeoSelectionEvent( this, geoSelection));
      }

      if (panningMode) {
        navigate.pan(-deltax, -deltay);
        panningMode = false;
        setCursor(Cursor.DEFAULT_CURSOR);
      }

      if (zoomingMode) {
        zoomRB.setActive(false);
        zoomRB.end(e.getPoint());
        zoomingMode = false;
        if (!NavigatedPanel.this.contains(e.getPoint())) { // point is off the panel
          if (debugBounds) System.out.println("NP.zoom: point "+e.getPoint()+" out of bounds: "+myBounds);
          return;
        }
        // "start" must be upper left
        startx = Math.min(startx, e.getX());
        starty = Math.min(starty, e.getY());
        navigate.zoom(startx, starty, Math.abs(deltax), Math.abs(deltay));
      }
      //drawG();
    }
  } // end myMouseListener

  // mouseMotionListener
  private class myMouseMotionListener implements MouseMotionListener {
    public void mouseDragged( MouseEvent e) {
      if (!changeable)
        return;
      deltax = e.getX() - startx;
      deltay = e.getY() - starty;
      if (debugEvent) System.out.println( "mouseDragged "+e.getX()+" "+e.getY()+
                "="+deltax+" "+deltay);

      if (zoomingMode) {
        zoomRB.stretch(e.getPoint());
        return;
      }

      if (moveSelectionMode) {
        selectionRB.move(deltax, deltay);
        return;
      }

      if (geoSelectionMode && !SwingUtilities.isRightMouseButton(e) && selectionRB.isActive()) {
        selectionRB.stretch(e.getPoint());
        return;
      }

      repaint();
      //redrawLater(100); // schedule redraw in 100 msecs
    }

    public void mouseMoved( MouseEvent e) {
      showStatus(e.getX(), e.getY());
    }
  }

  // necessary for g.drawImage()
  private class myImageObserver implements ImageObserver {
    public boolean imageUpdate(Image image, int flags, int x, int y, int width, int height) {
      return true;
    }
  }

  //DnD
  private class myDropTargetListener implements DropTargetListener {

    public void dragEnter(DropTargetDragEvent e) {
      System.out.println(" NP dragEnter active = "+ dropTarget.isActive());
      e.acceptDrag(DnDConstants.ACTION_COPY);
    }

    public void drop(DropTargetDropEvent e) {
      try {
        if(e.isDataFlavorSupported(DataFlavor.stringFlavor)){
          Transferable tr = e.getTransferable();
          e.acceptDrop (DnDConstants.ACTION_COPY_OR_MOVE);
          String s = (String)tr.getTransferData (DataFlavor.stringFlavor);
          //dropList.add(s);
          System.out.println(" NP myDropTargetListener got "+ s);
          e.dropComplete(true);
        } else {
          e.rejectDrop();
        }
      } catch (IOException io) {
        io.printStackTrace();
        e.rejectDrop();
      } catch (UnsupportedFlavorException ufe) {
        ufe.printStackTrace();
        e.rejectDrop();
      }
    }
    public void dragExit(DropTargetEvent e) {}
    public void dragOver(DropTargetDragEvent e) {}
    public void dropActionChanged(DropTargetDragEvent e) {}
  }

  //////////////////////////////////////////////////////////////////////////////
  // toolbars

  private AbstractAction zoomIn, zoomOut, zoomDefault, zoomBack;
  private AbstractAction moveUp, moveDown, moveLeft, moveRight;
  private void makeActions() {
      // add buttons/actions
    zoomIn = new AbstractAction() {
      public void actionPerformed(ActionEvent e) { navigate.zoomIn(); drawG();}
    };
    BAMutil.setActionProperties( zoomIn, "MagnifyPlus", "zoom In", false, 'I', KeyEvent.VK_ADD);

    zoomOut = new AbstractAction() {
      public void actionPerformed(ActionEvent e) { navigate.zoomOut(); drawG();}
    };
    BAMutil.setActionProperties( zoomOut, "MagnifyMinus", "zoom Out", false, 'O', KeyEvent.VK_SUBTRACT);

    zoomBack = new AbstractAction() {
      public void actionPerformed(ActionEvent e) { navigate.zoomPrevious();drawG();}
    };
    BAMutil.setActionProperties( zoomBack, "Undo", "Previous map area", false, 'P', KeyEvent.VK_BACK_SPACE);

    zoomDefault = new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        navigate.setMapArea(project.getDefaultMapArea());
        drawG();
      }
    };
    BAMutil.setActionProperties( zoomDefault, "Home", "Home map area", false, 'H', KeyEvent.VK_HOME);

    moveUp = new AbstractAction() {
      public void actionPerformed(ActionEvent e) { navigate.moveUp();drawG();}
    };
    BAMutil.setActionProperties( moveUp, "Up", "move view Up", false, 'U', KeyEvent.VK_UP);

    moveDown = new AbstractAction() {
      public void actionPerformed(ActionEvent e) { navigate.moveDown();drawG();}
    };
    BAMutil.setActionProperties( moveDown, "Down", "move view Down", false, 'D', KeyEvent.VK_DOWN);

    moveLeft = new AbstractAction() {
      public void actionPerformed(ActionEvent e) { navigate.moveLeft();drawG();}
    };
    BAMutil.setActionProperties( moveLeft, "Left", "move view Left", false, 'L', KeyEvent.VK_LEFT);

    moveRight = new AbstractAction() {
      public void actionPerformed(ActionEvent e) { navigate.moveRight(); drawG();}
    };
    BAMutil.setActionProperties( moveRight, "Right", "move view Right", false, 'R', KeyEvent.VK_RIGHT);

    setReferenceAction = new AbstractAction() {
      public void actionPerformed(ActionEvent e) { setReferenceMode(); drawG();}
    };
    BAMutil.setActionProperties( setReferenceAction, "ReferencePoint", "set reference Point", true, 'P', 0);
  }

  class NToolBar extends JToolBar {
    NToolBar() {
      AbstractButton b = BAMutil.addActionToContainer( this, zoomIn);
      b.setName("zoomIn");

      b = BAMutil.addActionToContainer( this, zoomOut);
      b.setName( "zoomOut");

      b = BAMutil.addActionToContainer( this, zoomBack);
      b.setName( "zoomBack");

      b = BAMutil.addActionToContainer( this, zoomDefault);
      b.setName( "zoomHome");
    }
  }

  class MoveToolBar extends JToolBar {
    MoveToolBar() {
      AbstractButton b = BAMutil.addActionToContainer( this, moveUp);
      b.setName( "moveUp");

      b = BAMutil.addActionToContainer( this, moveDown);
      b.setName( "moveDown");

      b = BAMutil.addActionToContainer( this, moveLeft);
      b.setName( "moveLeft");

      b = BAMutil.addActionToContainer( this, moveRight);
      b.setName( "moveRight");
    }
  }

 /*   public void setEnabled( boolean mode) {
      for (int i=0; i< getComponentCount(); i++) {
        Component c = getComponentAtIndex(i);
        c.setEnabled( mode);
      }
    }

    public void remove(String which) {
      // find which
      for (int i=0; i< getComponentCount(); i++) {
        Component c = getComponentAtIndex(i);
        if (which.equals(c.getName()))
          remove(c);
      }
    }

  } // end inner class */

} // end NavPanel

/* Change History:
   $Log: NavigatedPanel.java,v $
   Revision 1.7  2004/09/25 00:09:44  caron
   add images, thredds tab

   Revision 1.6  2004/09/24 03:26:40  caron
   merge nj22

   Revision 1.5  2004/05/21 05:57:36  caron
   release 2.0b

   Revision 1.4  2004/02/20 05:02:56  caron
   release 1.3

   Revision 1.3  2003/04/08 18:16:24  john
   nc2 v2.1

   Revision 1.2  2003/03/17 21:12:39  john
   new viewer

   Revision 1.1  2002/12/13 00:55:08  caron
   pass 2

   Revision 1.3  2002/04/29 22:23:34  caron
   NP detects seam crossings and throws NewProjectionEvent instead of NewMapAreaEvent

   Revision 1.1.1.1  2002/02/26 17:24:52  caron
   import sources
*/

