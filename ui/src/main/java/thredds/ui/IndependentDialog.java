// $Id: IndependentDialog.java 50 2006-07-12 16:30:06Z caron $
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
 * @version $Id: IndependentDialog.java 50 2006-07-12 16:30:06Z caron $
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

