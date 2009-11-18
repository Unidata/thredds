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
    cp.removeAll();
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

