// $Id: MapBean.java 50 2006-07-12 16:30:06Z caron $
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
  * @version $Id: MapBean.java 50 2006-07-12 16:30:06Z caron $
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
    standardMaps.add( new thredds.viewer.gis.shapefile.ShapeFileBean("WorldDetailMap", "Global Detailed Map", "WorldDetailMap", "/optional/nj22/maps/Countries.zip"));
    standardMaps.add( new thredds.viewer.gis.shapefile.ShapeFileBean("USDetailMap", "US Detailed Map", "USMap", "/optional/nj22/maps/US.zip"));
    
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


