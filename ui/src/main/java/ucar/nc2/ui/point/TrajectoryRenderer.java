// $Id: TrajectoryRenderer.java 50 2006-07-12 16:30:06Z caron $
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
package ucar.nc2.ui.point;

import thredds.ui.FontUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.geom.Point2D;
import java.awt.geom.GeneralPath;
import java.awt.*;
import java.io.IOException;

import ucar.unidata.geoloc.*;
import ucar.nc2.dt.PointObsDatatype;
import ucar.nc2.dt.TrajectoryObsDatatype;
import ucar.nc2.dt.EarthLocation;

/**
 *
 * @author caron
 * @version $Revision: 50 $ $Date: 2006-07-12 16:30:06Z $
 */
public class TrajectoryRenderer implements thredds.viewer.ui.Renderer {

  private ArrayList obsUIlist = new ArrayList(); // ObservationUI objects
  private ProjectionImpl project = null;        // display projection
  private AffineTransform world2Normal;

  // drawing parameters
  private Color color = Color.black;
  private Color selectedColor = Color.magenta;
  private int circleRadius = 3;
  private Rectangle2D circleBB = new Rectangle2D.Double(-circleRadius, -circleRadius, 2*circleRadius, 2*circleRadius);
  private Rectangle2D typicalBB = circleBB;
  private FontUtil.StandardFont textFont;

  // working objects to minimize excessive gc
  private Point2D.Double ptN = new Point2D.Double();

  private ObservationUI selected = null;

  // misc state
  private boolean declutter = true;
  private boolean posWasCalc = false;
  private boolean debug = false;

    /** constructor */
  public TrajectoryRenderer () {
    textFont = FontUtil.getStandardFont(10);
  }

  public void incrFontSize() { textFont.incrFontSize(); };
  public void decrFontSize() { textFont.decrFontSize(); };

  public void setColor(java.awt.Color color){ this.color = color; }
  public java.awt.Color getColor() { return color; }

  public LatLonRect getPreferredArea() {
    return null;
  }

  /**
   * Set the trajectory.
   * @param trajectory : obtain list of PointObsDatatype objects from here
   */
  public void setTrajectory( TrajectoryObsDatatype trajectory) throws IOException {
    int n = trajectory.getNumberPoints();
    obsUIlist = new ArrayList( n);
    for (int i = 0; i < n; i++) {
      PointObsDatatype obs = trajectory.getPointObsData(i);
      ObservationUI sui = new ObservationUI( obs); // wrap in a StationUI
      obsUIlist.add(sui); // wrap in a StationUI
    }
    posWasCalc = false;
    calcWorldPos();
    selected = null;
  }


  public void setSelected( PointObsDatatype obs) {
    selected = null;
    for (int i=0; i < obsUIlist.size(); i++) {
      ObservationUI s = (ObservationUI) obsUIlist.get(i);
      if (testPointObsDatatype( s.obs,  obs)) {
        selected = s;
        break;
      }
    }
  }

  private boolean testPointObsDatatype(PointObsDatatype obs1, PointObsDatatype obs2) {
    if (obs1.getObservationTime() != obs2.getObservationTime())
      return false;
    EarthLocation loc1 = obs1.getLocation();
    EarthLocation loc2 = obs2.getLocation();
    if (loc1.getLatitude() != loc2.getLatitude())
      return false;
    if (loc1.getLongitude() != loc2.getLongitude())
      return false;
    return true;
  }

  public void setDeclutter(boolean declut) { declutter = declut; }
  public boolean getDeclutter() { return declutter; }

  public void setProjection(ProjectionImpl project) {
    this.project = project;
    calcWorldPos();
  }

  private void calcWorldPos() {
    if (project == null) return;
    for (int i = 0; i < obsUIlist.size(); i++) {
      ObservationUI s = (ObservationUI) obsUIlist.get(i);
      s.worldPos.setLocation( project.latLonToProj(s.latlonPos));
    }
    posWasCalc = true;
  }

    ///////////////// pickable stuff

  /*
   * Find station that contains this point. If it exists, make it the
   *   "selected" station.
   * @param pickPt: world coordinates
   * @return station that contains this point, or null if none.
   *
  public PointObsDatatype pick(Point2D pickPt) {
    if (world2Normal == null || pickPt == null || obsUIlist.isEmpty()) return null;

    world2Normal.transform(pickPt, ptN); // work in normalized coordinate space
    ObservationUI closest = (ObservationUI) stationGrid.findIntersection(ptN);
    setSelectedStation( closest);

    return getSelectedStation();
  }

  /*
   * Find station closest to this point. Make it the "selected" station.
   * @param pickPt: world coordinates
   * @return station that contains this point, or null if none.
   *
  public PointObsDatatype pickClosest(Point2D pickPt) {
    if (world2Normal == null || pickPt == null || obsUIlist.isEmpty()) return null;

    world2Normal.transform(pickPt, ptN); // work in normalized coordinate space
    ObservationUI closest = (ObservationUI) stationGrid.findClosest(ptN);

    if (debug) System.out.println("closest= " +closest);
    setSelectedStation( closest);

    return getSelectedStation();
  }

  /**
   * Get the selected station, or null.
   *
  public PointObsDatatype getSelectedStation() {
    return (selected != null) ? selected.getObservation() : null;
  }

  /**
   * Is this point contained in a Station bounding box?
   * @param p: normalized coords.
   * @return Station containing pt, or null.
   *
  public ObservationUI isOnStation( Point p) {
    return (ObservationUI) stationGrid.findIntersection (p);
  } */

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
    //g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
    //    RenderingHints.VALUE_ANTIALIAS_ON);
    g.setStroke(new java.awt.BasicStroke(1.0f));

    /* set up the grid
    Rectangle2D bbox = (Rectangle2D) g.getClip(); // clipping area in normal coords
    // clear the grid = "no stations are drawn"
    stationGrid.clear();
    // set the grid size based on typical bounding box
    stationGrid.setGrid(bbox, typicalBB.getWidth(), typicalBB.getHeight());


    // always draw selected
    if (selected != null) {
      selected.calcPos( world2Normal);
      stationGrid.markIfClear( selected.getBB(), selected);
      selected.draw(g);
    } */

    g.setFont( textFont.getFont());
    g.setColor(color);

    int count = 0;
    int npts = obsUIlist.size();
    GeneralPath path = new GeneralPath(GeneralPath.WIND_EVEN_ODD, npts);
    for (int i=0; i < npts; i++) {
      ObservationUI s = (ObservationUI) obsUIlist.get(i);
      s.calcPos( world2Normal);
      s.draw(g);

      if (Double.isNaN( s.screenPos.getX())) {
        System.out.println("screenPos="+s.screenPos+" world = "+s.worldPos);
        continue;
      }

      if (count == 0)
        path.moveTo( (float) s.screenPos.getX(), (float) s.screenPos.getY());
      else
        path.lineTo( (float) s.screenPos.getX(), (float) s.screenPos.getY());
      count++;
    }

    g.setColor(color);
    g.draw( path);

    // draw selected
    if (selected != null)
      selected.draw(g);

    // restore
    g.setTransform(world2Device);
    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, saveHint);
  }


  public class ObservationUI {
    private PointObsDatatype obs;

    private LatLonPointImpl latlonPos = new LatLonPointImpl();       // latlon pos
    private ProjectionPointImpl worldPos = new ProjectionPointImpl();// world pos
    private Point2D.Double screenPos = new Point2D.Double(); // normalized screen pos

    private Rectangle2D bb;       // bounding box, in normalized screen coords, loc = 0, 0
    private Rectangle2D bbPos = new Rectangle2D.Double();    // bounding box, translated to current drawing pos
    // private boolean selected = false;

    ObservationUI(PointObsDatatype obs) {
      this.obs = obs;
      latlonPos.setLatitude(obs.getLocation().getLatitude());
      latlonPos.setLongitude(obs.getLocation().getLongitude());

      // text bb
      Dimension t = textFont.getBoundingBox("O"); // LOOK temp
      bb = new Rectangle2D.Double(-circleRadius, -circleRadius-t.getHeight(), t.getWidth(), t.getHeight());
      // add circle bb
      bb.add( circleBB);
      typicalBB = bb;
    }

    public PointObsDatatype getObservation() { return obs; }
    public LatLonPoint getLatLon() { return latlonPos; }
    public ProjectionPointImpl getLocation() { return worldPos; }
    //public boolean isSelected() { return selected; }
    //public void setIsSelected(boolean selected) { this.selected = selected; }
    public Rectangle2D getBB() { return bbPos; }

    boolean contains( Point p) {
      return bbPos.contains( p);
    }

    void calcPos( AffineTransform w2n) {
      w2n.transform( worldPos, screenPos);  // work in normalized coordinate space
      bbPos.setRect( screenPos.getX() + bb.getX(), screenPos.getY() + bb.getY(),
          bb.getWidth(), bb.getHeight());
    }

    void draw(Graphics2D g) {
      if (this == selected) {
        g.setColor( selectedColor);
        fillCircle(g, screenPos);
        g.setColor( color);
      } else
        drawCircle(g, screenPos);
      //drawText(g, screenPos, id);
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

    private void fillCircle( Graphics2D g, Point2D loc) {
      int x = (int) (loc.getX()-circleRadius);
      int y = (int) (loc.getY()-circleRadius);
      g.fillOval( x, y, 2*circleRadius, 2*circleRadius);
    }

    private void drawText( Graphics2D g, Point2D loc, String text) {
      int x = (int) (loc.getX() - circleRadius);
      int y = (int) (loc.getY() - circleRadius);
      g.drawString( text, x, y);
    }

  } // end inner class ObservationUI

}

