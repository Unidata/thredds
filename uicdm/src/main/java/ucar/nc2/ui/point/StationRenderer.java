/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.ui.point;

/*
  Renders collections of stations. package private.

  @author caron
 */

import ucar.nc2.ui.gis.SpatialGrid;
import ucar.nc2.ui.util.Renderer;
import ucar.ui.widget.FontUtil;
import ucar.unidata.geoloc.*;

import java.awt.*;
import java.awt.geom.*;
import java.util.*;
import java.util.List;

class StationRenderer implements Renderer {
  private List<StationUI> stations = new ArrayList<>(); // StationUI objects
  private HashMap<String, StationUI> stationHash = new HashMap<>();
  private SpatialGrid stationGrid;              // for "decluttering" and closest point
  private ProjectionImpl project = null;        // display projection
  private AffineTransform world2Normal;

  // drawing parameters
  private Color color = Color.black;
  private int circleRadius = 5;
  private Rectangle2D circleBB = new Rectangle2D.Double(-circleRadius, -circleRadius, 2*circleRadius, 2*circleRadius);
  private Rectangle2D typicalBB = circleBB;
  private FontUtil.StandardFont textFont;

    // pick stuff
  private StationUI selected = null;

  // working objects to minimize excessive gc
  private Point2D.Double ptN = new Point2D.Double();

  // misc state
  private boolean declutter = true;
  private boolean posWasCalc = false;
  private boolean debug = false;

    /** constructor */
  public StationRenderer () {
    stationGrid = new SpatialGrid(50, 30);
    stationGrid.setOverlap(10);
    textFont = FontUtil.getStandardFont(10);
  }

  public void incrFontSize() { textFont.incrFontSize(); }

  public void decrFontSize() { textFont.decrFontSize(); }

  public void setColor(java.awt.Color color){ this.color = color; }
  public java.awt.Color getColor() { return color; }

  public LatLonRect getPreferredArea() {
    return null;
  }

  /**
   * Set the list of stations.
   * @param stns: list of DDStation objects
   */
  public void setStations(java.util.List<ucar.unidata.geoloc.Station> stns) {
    stations = new ArrayList<>(stns.size());
    stationHash.clear();
    for (Station stn : stns) {
      Station s = stn;
      StationUI sui = new StationUI(s); // wrap in a StationUI
      stations.add(sui); // wrap in a StationUI
      stationHash.put(s.getName(), sui);
    }
    posWasCalc = false;
    calcWorldPos();
  }

  // set selected station based on the sttion id.
  public void setSelectedStation( String name) {
    StationUI sui = stationHash.get( name);
    if (sui != null) {
      setSelectedStation( sui);
    }
  }

  private void setSelectedStation( StationUI sui) {
    if (selected != null) {
      selected.selected = false;
      selected = null;
    }

    if (sui != null) {
      selected = sui;
      selected.selected = true;
    }
  }

  public void setDeclutter(boolean declut) { declutter = declut; }
  public boolean getDeclutter() { return declutter; }

  public void setProjection(ProjectionImpl project) {
    this.project = project;
    calcWorldPos();
  }

  private void calcWorldPos() {
    if (project == null) return;
    for (StationUI station : stations) {
      StationUI s = station;
      s.worldPos.setLocation(project.latLonToProj(s.latlonPos));
    }
    posWasCalc = true;
  }

    ///////////////// pickable stuff

  /**
   * Find station that contains this point. If it exists, make it the
   *   "selected" station.
   * @param pickPt: world coordinates
   * @return station that contains this point, or null if none.
   */
  public ucar.unidata.geoloc.Station pick(Point2D pickPt) {
    if (world2Normal == null || pickPt == null || stations.isEmpty()) return null;

    world2Normal.transform(pickPt, ptN); // work in normalized coordinate space
    StationUI closest = (StationUI) stationGrid.findIntersection(ptN);
    setSelectedStation( closest);

    return getSelectedStation();
  }

  /**
   * Find station closest to this point. Make it the "selected" station.
   * @param pickPt: world coordinates
   * @return station that contains this point, or null if none.
   */
  public ucar.unidata.geoloc.Station pickClosest(Point2D pickPt) {
    if (world2Normal == null || pickPt == null || stations.isEmpty()) return null;

    world2Normal.transform(pickPt, ptN); // work in normalized coordinate space
    StationUI closest = (StationUI) stationGrid.findClosest(ptN);

    if (debug) System.out.println("closest= " +closest);
    setSelectedStation( closest);

    return getSelectedStation();
  }

  /**
   * Get the selected station.
   * @return the selected station, or null if none selected
   */
  public ucar.unidata.geoloc.Station getSelectedStation() {
    return (selected != null) ? selected.ddStation : null;
  }

  /**
   * Is this point contained in a Station bounding box?
   * @param p: normalized coords.
   * @return Station containing pt, or null.
   */
  public StationUI isOnStation( Point p) {
    return (StationUI) stationGrid.findIntersection (p);
  }

  public void draw(java.awt.Graphics2D g, java.awt.geom.AffineTransform normal2Device) {
    if ((project == null) || !posWasCalc) return;

    // use world coordinates for position, but draw in screen coordinates
    // so that the symbols stay the same size
    AffineTransform world2Device = g.getTransform();
    g.setTransform(normal2Device);        //  identity transform for screen coords

    // transform World to Normal coords:
    //    world2Normal = pixelAT-1 * world2Device
    // cache for pick closest
    try {
      world2Normal = normal2Device.createInverse();
      world2Normal.concatenate( world2Device);
    } catch ( java.awt.geom.NoninvertibleTransformException e) {
      System.out.println(
            " RendSurfObs: NoninvertibleTransformException on " +
            normal2Device);
      return;
    }

    // we want aliasing; but save previous state to restore at end
    Object saveHint = g.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
        RenderingHints.VALUE_ANTIALIAS_ON);
    g.setStroke(new java.awt.BasicStroke(1.0f));

    // set up the grid
    Rectangle2D bbox = (Rectangle2D) g.getClip(); // clipping area in normal coords
    // clear the grid = "no stations are drawn"
    stationGrid.clear();
    // set the grid size based on typical bounding box
    stationGrid.setGrid(bbox, typicalBB.getWidth(), typicalBB.getHeight());

    g.setFont( textFont.getFont());
    g.setColor(color);

    // always draw selected
    if (selected != null) {
      selected.calcPos( world2Normal);
      stationGrid.markIfClear( selected.getBB(), selected);
      selected.draw(g);
    }

    for (StationUI station : stations) {
      StationUI s = station;
      s.calcPos(world2Normal);
      if (stationGrid.markIfClear(s.getBB(), s) || !declutter) {
        s.draw(g);
      }
    }

    // restore
    g.setTransform(world2Device);
    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, saveHint);
  }

  public class StationUI {
    private ucar.unidata.geoloc.Station ddStation;
    private String id;

    private LatLonPointImpl latlonPos = new LatLonPointImpl();       // latlon pos
    private ProjectionPointImpl worldPos = new ProjectionPointImpl();// world pos
    private Point2D.Double screenPos = new Point2D.Double(); // normalized screen pos

    private Rectangle2D bb;       // bounding box, in normalized screen coords, loc = 0, 0
    private Rectangle2D bbPos = new Rectangle2D.Double();    // bounding box, translated to current drawing pos
    private boolean selected = false;

    StationUI(ucar.unidata.geoloc.Station stn) {
      ddStation = stn;
      latlonPos.setLatitude(stn.getLatitude());
      latlonPos.setLongitude(stn.getLongitude());

      id = stn.getName();

      // text bb
      Dimension t = textFont.getBoundingBox(id);
      bb = new Rectangle2D.Double(-circleRadius, -circleRadius-t.getHeight(), t.getWidth(), t.getHeight());
      // add circle bb
      bb.add( circleBB);
      typicalBB = bb;
    }

    public String getID() { return id; }
    public ucar.unidata.geoloc.Station getStation() { return ddStation; }
    public LatLonPoint getLatLon() { return latlonPos; }
    public ProjectionPointImpl getLocation() { return worldPos; }
    public boolean isSelected() { return selected; }
    public void setIsSelected(boolean selected) { this.selected = selected; }
    public Rectangle2D getBB() { return bbPos; }

    boolean contains( Point p) {
      return bbPos.contains( p);
    }

    void calcPos( AffineTransform w2n) {
      Point2D.Double p2d = new Point2D.Double(worldPos.getX(), worldPos.getY());
      w2n.transform( p2d, screenPos);  // work in normalized coordinate space
      bbPos.setRect( screenPos.getX() + bb.getX(), screenPos.getY() + bb.getY(),
          bb.getWidth(), bb.getHeight());
    }

    void draw(Graphics2D g) {
      g.setColor( selected ? Color.red : Color.black);
      drawCircle(g, screenPos);
      drawText(g, screenPos, id);
    }

    /** draw the symbol at the specified location
     * @param g Graphics2D object
     * @param loc the reference point (center of the CloudSymbol icon offset from here)
     */
    private void drawCircle( Graphics2D g, Point2D loc) {
      int x = (int) (loc.getX()-circleRadius);
      int y = (int) (loc.getY()-circleRadius);
      g.drawOval( x, y, 2*circleRadius, 2*circleRadius);
    }

    private void drawText( Graphics2D g, Point2D loc, String text) {
      int x = (int) (loc.getX() - circleRadius);
      int y = (int) (loc.getY() - circleRadius);
      g.drawString( text, x, y);
    }

    public String toString() {
      return id+": "+ddStation.getName();
    }

  } // end inner class StationUI

}

