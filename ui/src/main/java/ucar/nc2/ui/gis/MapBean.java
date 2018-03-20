/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.ui.gis;

import ucar.nc2.ui.gis.shapefile.ShapeFileBean;
import ucar.nc2.ui.gis.worldmap.WorldMapBean;
import ucar.nc2.ui.widget.BAMutil;
import ucar.nc2.ui.widget.PopupMenu;

import javax.swing.*;
import javax.swing.event.EventListenerList;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;

/** Wrap map Renderers as beans.
  *
  * @author John Caron
  **/


public abstract class MapBean {
  private EventListenerList listenerList = new EventListenerList();


  public abstract javax.swing.ImageIcon getIcon();
  public abstract String getActionName();
  public abstract String getActionDesc();

    /** Each bean has one Renderer, made current when Action is called */
  public abstract ucar.nc2.ui.util.Renderer getRenderer();

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
  static public PopupMenu makeMapSelectButton() {

    AbstractAction mapSelectAction = new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        //System.out.println("mapSelectAction");
        //mapPopup.show();
      }
    };
    BAMutil.setActionProperties( mapSelectAction, "WorldMap", "select map", false, 'M', -1);
    AbstractButton mapSelectButton = BAMutil.makeButtconFromAction( mapSelectAction);

    PopupMenu mapPopup = new PopupMenu(mapSelectButton, "Select Map", true);
    return mapPopup;
  }

  static public PopupMenu getStandardMapSelectButton(PropertyChangeListener pcl) {
    PopupMenu mapBeanMenu = makeMapSelectButton();

        // standard maps
    ArrayList standardMaps = new ArrayList();
    standardMaps.add( new WorldMapBean());
    standardMaps.add( new ShapeFileBean("WorldDetailMap", "Global Detailed Map", "WorldDetailMap",
            "/resources/nj22/ui/maps/Countries.shp"));
    standardMaps.add( new ShapeFileBean("USDetailMap", "US Detailed Map", "USMap", "/resources/nj22/ui/maps/us_state.shp"));
    
    for (int i = 0; i < standardMaps.size(); i++) {
      MapBean mb = (MapBean) standardMaps.get(i);
      mapBeanMenu.addAction( mb.getActionDesc(), mb.getIcon(), mb.getAction());
      mb.addPropertyChangeListener( pcl);
    }

    return mapBeanMenu;
  }

}
