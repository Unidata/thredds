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
package thredds.ui;

import java.awt.*;
import java.beans.*;
import javax.swing.*;

/**
 *  Provides common L&F for managing independent dialogs
 *  Takes RootPaneContainer as parent, to work with both applet and app
 *  Will reset L&F
 *
 *  example of use:
     infoWindow = new IndependentDialog(topLevel.getRootPaneContainer(), false, "Dataset Information");
     datasetInfoTA = new TextHistoryPane(500, 100, true);
     Container cp = infoWindow.getContentPane();
     cp.add(datasetInfoTA, BorderLayout.CENTER);
     infoWindow.pack();
     infoWindow.setSize(700,700);
     infoWindow.setLocation(100,100);
 *
 * @author John Caron
 * @version $Id$
 */
public class IndependentDialog extends JDialog {
  protected JFrame parent;

  /** constructor
     @param parent      JFrame (application) or JApplet (applet)
     @param modal     is modal
     @param title       Window title
   */
  public IndependentDialog(JFrame parent, boolean modal, String title) {
    // having a parent JFrame is better. But what to do about applets?
    super(parent == null ? ucar.util.prefs.ui.PrefPanel.findActiveFrame() : parent);
    this.parent = parent;
    setModal(modal);
    if (title != null)
      setTitle(title);

    // L&F may change
    UIManager.addPropertyChangeListener( new PropertyChangeListener() {
      public void propertyChange( PropertyChangeEvent e) {
        if (e.getPropertyName().equals("lookAndFeel"))
          SwingUtilities.updateComponentTreeUI( IndependentDialog.this);
      }
    });

    // set position based on owned
    Window owner = getOwner();
    if (owner != null) {
      Rectangle b = owner.getBounds();
      int x = (int) ( b.getX() + b.getWidth()/2);
      int y = (int) ( b.getY() + b.getHeight()/2);
      setLocation( x, y);
    }
  }

  public IndependentDialog(JFrame parent, boolean modal, String title, Component comp) {
    this(parent, modal, title);
    setComponent( comp);
  }

  public void setComponent( Component comp) {
    Container cp = getContentPane();
    cp.add(comp, BorderLayout.CENTER);
    pack();
  }

}

/* Change History:
   $Log: IndependentDialog.java,v $
   Revision 1.5  2004/09/30 00:33:38  caron
   *** empty log message ***

   Revision 1.4  2004/09/24 03:26:34  caron
   merge nj22

   Revision 1.3  2004/05/21 05:57:34  caron
   release 2.0b

   Revision 1.2  2004/05/11 23:30:35  caron
   release 2.0a

   Revision 1.1.1.1  2002/11/23 17:49:48  caron
   thredds reorg

   Revision 1.1.1.1  2002/02/15 00:01:49  caron
   import sources

*/

