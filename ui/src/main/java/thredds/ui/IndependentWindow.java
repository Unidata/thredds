// $Id: IndependentWindow.java 50 2006-07-12 16:30:06Z caron $
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
 *  Provides common L&F for managing independent windows
 *  Will reset L&F
 *
 *  example: <pre>
     infoWindow = new IndependentWindow("Dataset Information");
     datasetInfoTA = new TextHistoryPane(500, 100, true);
     Container cp = infoWindow.getContentPane();
     cp.add(datasetInfoTA, BorderLayout.CENTER);
     infoWindow.pack();
     infoWindow.setSize(700,700);
     infoWindow.setLocation(100,100);
 *   </pre>
 *
 * @author John Caron
 * @version $Id: IndependentWindow.java 50 2006-07-12 16:30:06Z caron $
 */
public class IndependentWindow extends JFrame {
  static private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(IndependentWindow.class);


  /** constructor
     @param title       Window title
     @param iconImage   image to show when iconified
   */
  public IndependentWindow(String title, Image iconImage) {
    super(title);

    // L&F may change
    UIManager.addPropertyChangeListener( new PropertyChangeListener() {
      public void propertyChange( PropertyChangeEvent e) {
        if (e.getPropertyName().equals("lookAndFeel"))
          SwingUtilities.updateComponentTreeUI( IndependentWindow.this);
      }
    });

    if (null != iconImage)
      setIconImage( iconImage);
  }

  /** constructor
     @param title       Window title.
     @param comp the COmponent to put in the window.
   */
  public IndependentWindow(String title, Image iconImage, Component comp) {
    this(title, iconImage);
    setComponent( comp);
  }

  public void setComponent(Component comp ) {
    Container cp = getContentPane();
    cp.add(comp, BorderLayout.CENTER);
    try {
      pack();
    } catch (java.lang.IllegalArgumentException e) {
      // Ticket ID: HEM-237554
      // I'm using IceWM window manager under Linux, and it does not support changing the icon on the top left side of the window.
      // This crashes the whole thing. I dont think this should be a fatal exception.
      // It would be helpful for future releases to catch this exception and let the program go ahead without the icon.
      log.error("Possible problem setting icon (?)", e);
    }
  }

  /** show the window. */
  public void show() {
    setState( Frame.NORMAL );   // deiconify if needed
    super.toFront();
    // need to put on event thread
    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        IndependentWindow.super.show();
      }
    });
  }

  /** show if not iconified */
  public void showIfNotIconified() {
    if (getState() == Frame.ICONIFIED) return;
    // need to put on event thread
    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        IndependentWindow.super.show();
      }
    });
  }


}

/* Change History:
   $Log: IndependentWindow.java,v $
   Revision 1.5  2004/09/30 00:33:38  caron
   *** empty log message ***

   Revision 1.4  2004/09/24 03:26:34  caron
   merge nj22

   Revision 1.3  2004/02/21 02:16:16  caron
   put show in event thread

   Revision 1.2  2003/12/04 22:27:48  caron
   *** empty log message ***

   Revision 1.1.1.1  2002/11/23 17:49:48  caron
   thredds reorg

   Revision 1.1.1.1  2002/02/15 00:01:49  caron
   import sources

*/

