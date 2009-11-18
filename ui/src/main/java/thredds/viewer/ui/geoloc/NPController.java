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

import thredds.viewer.ui.*;
import thredds.viewer.ui.Renderer;
import thredds.viewer.gis.MapBean;
import thredds.ui.BAMutil;
import ucar.unidata.geoloc.*;
import ucar.unidata.geoloc.projection.*;
import ucar.util.prefs.ui.Debug;

import javax.swing.*;
import javax.swing.border.EtchedBorder;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.AffineTransform;
import java.awt.*;
import java.util.ArrayList;
import java.beans.PropertyChangeListener;

/**
 * An abstract superclass for Navigated Panel controllers
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
    thredds.viewer.ui.Renderer render = new thredds.viewer.gis.worldmap.WorldMap();    // default Renderer
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

    thredds.ui.PopupMenu mapBeanMenu = MapBean.getStandardMapSelectButton( new PropertyChangeListener() {
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