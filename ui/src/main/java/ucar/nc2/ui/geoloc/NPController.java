/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.ui.geoloc;

import ucar.nc2.ui.gis.MapBean;
import ucar.nc2.ui.gis.worldmap.WorldMap;
import ucar.nc2.ui.widget.*;
import ucar.nc2.ui.util.Renderer;
import ucar.unidata.geoloc.*;

import javax.swing.*;
import javax.swing.border.EtchedBorder;
import java.awt.geom.AffineTransform;
import java.awt.*;
import java.util.ArrayList;
import java.beans.PropertyChangeListener;

/**
 * A superclass for Navigated Panel controllers
 *
 * @author John Caron
 */
public class NPController extends JPanel {
  protected NavigatedPanel np;
  protected ArrayList renderers = new ArrayList(); // thredds.viewer.ui.Renderer
  protected ProjectionImpl project;
  protected AffineTransform atI = new AffineTransform();  // identity transform
  protected boolean eventOk = true;

  protected JPanel toolPanel;

  // debugging
  private boolean debug = false;

  public NPController() {
      // here's where the map will be drawn:
    np = new NavigatedPanel();
    Renderer render = new WorldMap();    // default Renderer
    project = np.getProjectionImpl();
    render.setProjection( project);
    addRenderer( render);

          // get Projection Events from the navigated panel
    np.addNewProjectionListener(new NewProjectionListener() {
      public void actionPerformed(NewProjectionEvent e) {
        ProjectionImpl p = e.getProjection();
        for (int i = 0; i < renderers.size(); i++) {
          Renderer r = (Renderer) renderers.get(i);
          r.setProjection(p);
        }
        redraw(true);
      }
    });

          // get NewMapAreaEvents from the navigated panel
    np.addNewMapAreaListener( new NewMapAreaListener() {
      public void actionPerformed(NewMapAreaEvent e) {
        redraw(true);
      }
    });

    ucar.nc2.ui.widget.PopupMenu mapBeanMenu = MapBean.getStandardMapSelectButton( new PropertyChangeListener() {
     public void propertyChange( java.beans.PropertyChangeEvent e) {
       if (e.getPropertyName().equals("Renderer")) {
         Renderer mapRender = (Renderer) e.getNewValue();
         mapRender.setProjection( np.getProjectionImpl());
         renderers.set(0, mapRender); // always first
         redraw( true);
       }
     }
   });

    toolPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
    toolPanel.add( mapBeanMenu.getParentComponent());
    toolPanel.add(np.getNavToolBar());
    toolPanel.add(np.getMoveToolBar());
    BAMutil.addActionToContainer( toolPanel, np.setReferenceAction);

    makeUI();
  }

  protected void makeUI() {
    setLayout(new BorderLayout());

    JPanel statusPanel = new JPanel(new BorderLayout());
    statusPanel.setBorder(new EtchedBorder());
    JLabel positionLabel = new JLabel("position");
    statusPanel.add(positionLabel, BorderLayout.CENTER);

    np.setPositionLabel(positionLabel);
    add(toolPanel, BorderLayout.NORTH);
    add(np, BorderLayout.CENTER);
    add(statusPanel, BorderLayout.SOUTH);
  }

  public NavigatedPanel getNavigatedPanel() { return np; }

  public void addRenderer( Renderer r) {
    renderers.add( r);
    r.setProjection( project);
  }

  public void setProjection( ProjectionImpl p) {
    project = p;
    for (int i = 0; i < renderers.size(); i++) {
      Renderer r = (Renderer) renderers.get(i);
      r.setProjection( p);
    }

    eventOk = false;
    np.setProjectionImpl( p);
    eventOk = true;
    redraw(true);
  }

  protected void redraw(boolean complete) {
    if (project == null)
     return;

    long tstart = System.currentTimeMillis();

    java.awt.Graphics2D gNP = np.getBufferedImageGraphics();
    if (gNP == null) // panel not drawn on screen yet
      return;

      // clear it
    gNP.setBackground(np.getBackgroundColor());
    gNP.fill(gNP.getClipBounds());

    for (int i = 0; i < renderers.size(); i++) {
      Renderer r = (Renderer) renderers.get(i);
      r.draw(gNP, atI);
    }
    gNP.dispose();

    if (debug) {
      long tend = System.currentTimeMillis();
      System.out.println("NPController draw time = "+ (tend - tstart)/1000.0+ " secs");
    }
      // copy buffer to the screen
    np.repaint();
  }

}