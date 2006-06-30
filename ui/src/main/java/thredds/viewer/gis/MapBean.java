// $Id: MapBean.java,v 1.5 2005/06/11 19:03:57 caron Exp $
/*
 * Copyright 1997-2004 Unidata Program Center/University Corporation for
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
package thredds.viewer.gis;

import thredds.viewer.gis.worldmap.WorldMapBean;
import thredds.ui.BAMutil;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import javax.swing.event.EventListenerList;
import javax.swing.*;

/** Wrap map Renderers as beans.
  *
  * @author John Caron
  * @version $Id: MapBean.java,v 1.5 2005/06/11 19:03:57 caron Exp $
  **/


public abstract class MapBean {
  private EventListenerList listenerList = new EventListenerList();


  public abstract javax.swing.ImageIcon getIcon();
  public abstract String getActionName();
  public abstract String getActionDesc();

    /** Each bean has one Renderer, made current when Action is called */
  public abstract thredds.viewer.ui.Renderer getRenderer();

 /** Construct the Action that is called when this bean's menu item/buttcon is selected.
   * Typically this routine is only called once when the bean is added.
   * The Action itself is called whenever the menu/buttcon is selected.
   *
   * The action should have NAME, SMALL_ICON and SHORT_DESC properties set.
   * The applications uses these to put up a buttcon and menu item.
   * The actionPerformed() method may do various things, but it must
   *   send a PropertyChangeEvent with newValue = Renderer.
   * @return the Action to be called.
   */
  public javax.swing.Action getAction() {
    AbstractAction useMap = new AbstractAction(getActionName(), getIcon()) {
      public void actionPerformed(java.awt.event.ActionEvent e) {
        firePropertyChangeEvent(this, "Renderer", null, getRenderer());
      }
    };
    useMap.putValue(Action.SHORT_DESCRIPTION, getActionDesc());

    return useMap;
  }


  /**
   * Add a PropertyChangeEvent Listener.
   */
  public void addPropertyChangeListener( PropertyChangeListener l) {
    listenerList.add(PropertyChangeListener.class, l);
  }

  /**
   * Remove a PropertyChangeEvent Listener.
   */
  public void removePropertyChangeListener( PropertyChangeListener l) {
    listenerList.remove(PropertyChangeListener.class, l);
  }

  protected void firePropertyChangeEvent(Object source, String propertyName, Object oldValue, Object newValue) {
    PropertyChangeEvent event = new PropertyChangeEvent(source, propertyName, oldValue, newValue);
    firePropertyChangeEvent( event);
  }

  protected void firePropertyChangeEvent(PropertyChangeEvent event) {
    // Process the listeners last to first
    Object[] listeners = listenerList.getListenerList();
    for (int i = listeners.length-2; i>=0; i-=2) {
      if (listeners[i] == PropertyChangeListener.class) {
        ((PropertyChangeListener)listeners[i+1]).propertyChange(event);
      }
    }
  }

  /**
   * Convenience routine to make a button with a popup menu attached. to use:
   * <pre>
      thredds.ui.PopupMenu mapBeanMenu = MapBean.makeMapSelectButton();
      AbstractButton butt = (AbstractButton) mapBeanMenu.getParentComponent();
      addToMenu (butt);

      // add map beans here
      mapBeanMenu.addAction( mb.getActionDesc(), mb.getIcon(), mb.getAction());

      mb.addPropertyChangeListener( new PropertyChangeListener() {
       public void propertyChange( java.beans.PropertyChangeEvent e) {
         if (e.getPropertyName().equals("Renderer")) {
           mapRender = (thredds.viewer.ui.Renderer) e.getNewValue();
           mapRender.setProjection( np.getProjectionImpl());
           redraw();
         }
       }
     });
   */
  static public thredds.ui.PopupMenu makeMapSelectButton() {

    AbstractAction mapSelectAction = new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        //System.out.println("mapSelectAction");
        //mapPopup.show();
      }
    };
    BAMutil.setActionProperties( mapSelectAction, "WorldMap", "select map", false, 'M', -1);
    AbstractButton mapSelectButton = BAMutil.makeButtconFromAction( mapSelectAction);

    thredds.ui.PopupMenu mapPopup = new thredds.ui.PopupMenu(mapSelectButton, "Select Map", true);
    return mapPopup;
  }

  static public thredds.ui.PopupMenu getStandardMapSelectButton(PropertyChangeListener pcl) {
    thredds.ui.PopupMenu mapBeanMenu = makeMapSelectButton();

        // standard maps
    ArrayList standardMaps = new ArrayList();
    standardMaps.add( new thredds.viewer.gis.worldmap.WorldMapBean());
    standardMaps.add( new thredds.viewer.gis.shapefile.ShapeFileBean("WorldDetailMap", "Global Detailed Map", "WorldDetailMap", "/resources/nj22/maps/Countries.zip"));
    standardMaps.add( new thredds.viewer.gis.shapefile.ShapeFileBean("USDetailMap", "US Detailed Map", "USMap", "/resources/nj22/maps/US.zip"));
    
    for (int i = 0; i < standardMaps.size(); i++) {
      MapBean mb = (MapBean) standardMaps.get(i);
      mapBeanMenu.addAction( mb.getActionDesc(), mb.getIcon(), mb.getAction());
      mb.addPropertyChangeListener( pcl);
    }

    return mapBeanMenu;
  }

}

/* Change History:
   $Log: MapBean.java,v $
   Revision 1.5  2005/06/11 19:03:57  caron
   no message

   Revision 1.4  2004/09/30 00:33:39  caron
   *** empty log message ***

   Revision 1.3  2004/09/28 21:39:10  caron
   *** empty log message ***

   Revision 1.2  2004/09/24 03:26:37  caron
   merge nj22

   Revision 1.1  2002/12/13 00:53:09  caron
   pass 2

   Revision 1.1.1.1  2002/02/26 17:24:49  caron
   import sources

*/


