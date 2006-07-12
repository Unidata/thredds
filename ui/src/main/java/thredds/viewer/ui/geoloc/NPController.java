// $Id$
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
 * @version $Id$
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

/* Change History:
   $Log: NPController.java,v $
   Revision 1.5  2005/06/11 19:03:58  caron
   no message

   Revision 1.4  2004/09/24 03:26:40  caron
   merge nj22

   Revision 1.3  2003/04/08 18:16:23  john
   nc2 v2.1

   Revision 1.2  2003/03/17 21:12:39  john
   new viewer

   Revision 1.1  2002/12/13 00:55:08  caron
   pass 2

   Revision 1.3  2002/04/29 22:23:33  caron
   NP detects seam crossings and throws NewProjectionEvent instead of NewMapAreaEvent

   Revision 1.1.1.1  2002/02/26 17:24:53  caron
   import sources
*/



