/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.ui.grid;

import ucar.nc2.dataset.CoordinateAxis;
import ucar.nc2.dataset.CoordinateAxis1D;
import ucar.nc2.dt.GridCoordSystem;
import ucar.nc2.ui.event.ActionSourceListener;
import ucar.nc2.ui.widget.ScaledPanel;
import ucar.unidata.geoloc.LatLonPoint;
import ucar.unidata.geoloc.LatLonPointImpl;
import ucar.unidata.geoloc.Projection;
import ucar.unidata.geoloc.ProjectionImpl;

import javax.swing.*;
import java.awt.*;

/**
 *  2D Vertical "slice" drawing widget.
 *  Integrates a drawing area (ucar.unidata.ui.ScaledPanel), a slider widget
 *   (ucar.unidata.view.grid.VertScaleSlider) and a bottom axis.
 *
 * @author caron
 */

public class VertPanel extends JPanel {
  private ScaledPanel drawArea;
  private VertScaleSlider vslider;
  private JLabel leftScale, midScale, rightScale, vertUnitsLabel;

  private double yleft = 0.0, ymid = 0.0, yright = 0.0;
  private boolean isLatLon = true;
  private Projection proj = null;
  private CoordinateAxis xaxis = null;

  private static boolean debugBounds = false, debugLevels = false;

  public VertPanel() {
    setLayout(new BorderLayout());

    JPanel botScale = new JPanel(new BorderLayout());
    leftScale = new JLabel( "leftScale");
    rightScale = new JLabel( "rightScale");
    midScale = new JLabel( "midScale", SwingConstants.CENTER);
    botScale.add( leftScale, BorderLayout.WEST);
    botScale.add( midScale, BorderLayout.CENTER);
    botScale.add( rightScale, BorderLayout.EAST);

    drawArea = new ScaledPanel();
    vslider = new VertScaleSlider();

    JPanel rightPanel = new JPanel(new BorderLayout());
    rightPanel.add( vslider, BorderLayout.CENTER);
    vertUnitsLabel = new JLabel(" ");
    rightPanel.add( vertUnitsLabel, BorderLayout.SOUTH);

    JPanel leftPanel = new JPanel(new BorderLayout());
    leftPanel.add( drawArea, BorderLayout.CENTER);
    leftPanel.add( botScale, BorderLayout.SOUTH);

    add( leftPanel, BorderLayout.CENTER);
    add( rightPanel, BorderLayout.EAST);
  }

      /** better way to do event management */
  public ActionSourceListener getActionSourceListener() { return vslider.getActionSourceListener(); }
    /** User must get this Graphics2D and draw into it when panel needs redrawing */
  public ScaledPanel getDrawArea() { return drawArea; }

  public void setLevels( GridCoordSystem gcs, int current) { vslider.setLevels( gcs, current); }

  public void setCoordSys( GridCoordSystem geocs, int currentLevel) {
    CoordinateAxis1D zaxis = geocs.getVerticalAxis();
    if (zaxis == null) return;

    vslider.setLevels( geocs, currentLevel);
    vertUnitsLabel.setText( "  "+zaxis.getUnitsString());

    /* set the bounds of the world coordinates.
     * The point (world.getX(), world.getY()) is mapped to the lower left point of the screen.
     * The point (world.getX() + world.Width(), world.getY()+world.Height()) is mapped
     * to the upper right corner. Therefore if coords decrease as you go up, world.Height()
     * should be negetive.
     */

    CoordinateAxis yaxis = geocs.getYHorizAxis();
    if ((yaxis == null) || (zaxis == null))
      return;
    //int nz = (int) zaxis.getSize();
    //int ny = (int) yaxis.getSize();

    // must determine which is on top: depends on ifz is up or down!
    double zmin = zaxis.getMinValue(); //Math.min(zaxis.getCoordEdge(0), zaxis.getCoordEdge(nz));
    double zmax = zaxis.getMaxValue(); //Math.max(zaxis.getCoordEdge(0), zaxis.getCoordEdge(nz));
    double zupper, zlower;
    if (geocs.isZPositive()) {
      zlower = zmin;
      zupper = zmax;
    } else {
      zlower = zmax;
      zupper = zmin;
    }

    // LOOK: actuallly may be non-linear if its a 2D XY coordinate system.
    // so this is just an approximation
    yleft = yaxis.getMinValue();
    yright = yaxis.getMaxValue();

    if (debugBounds) {
      System.out.println("VertPanel: ascending from "+ zlower+ " to "+ zupper);
      System.out.println("VertPanel: from left "+ yleft+ " to right "+ yright);
    }

    ScaledPanel.Bounds bounds = new ScaledPanel.Bounds(yleft, yright, zupper, zlower);
    drawArea.setWorldBounds( bounds);

    // set bottom scale
    proj = geocs.getProjection();
    isLatLon = geocs.isLatLon();
    ymid = (yleft + yright)/2;

    xaxis = geocs.getXHorizAxis();

    setSlice( 0);
  }

  public void setSlice( int slice) {

    if (isLatLon) {
      leftScale.setText( LatLonPointImpl.latToString(yleft, 3));
      midScale.setText( LatLonPointImpl.latToString( ymid, 3));
      rightScale.setText( LatLonPointImpl.latToString(yright, 3));
      return;
    }
    double xval = 0.0;

    if ((xaxis != null) && (xaxis instanceof CoordinateAxis1D)) {
      xval = ((CoordinateAxis1D)xaxis).getCoordValue(slice);

          // set bottom scale
      leftScale.setText( getYstr(xval, yleft));
      midScale.setText( getYstr(xval, ymid));
      rightScale.setText( getYstr(xval, yright));
    }

    repaint();
  }

  private String getYstr(double xvalue, double yvalue) {
    LatLonPoint lpt = ((ProjectionImpl)proj).projToLatLon(xvalue, yvalue);
    return LatLonPointImpl.latToString(lpt.getLatitude(), 3);
  }

}
