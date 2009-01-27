// $Id: PLAF.java 50 2006-07-12 16:30:06Z caron $
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

import java.awt.event.*;
import javax.swing.*;

/**
 *  Pluggable Look and Feel management.
 * @author John Caron
 * @version $Id: PLAF.java 50 2006-07-12 16:30:06Z caron $
 */

public class PLAF {
  private static UIManager.LookAndFeelInfo[] plafInfo = UIManager.getInstalledLookAndFeels();
  private JComponent tree;
  private boolean debug = false;

  /* Constructor.
    * @param tree the top-level JComponent tree: everything in this tree will get switched to
    *   the new L&F. Everything not in the tree (eg Dialogs) should listen for changes like:
    *<pre>
    UIManager.addPropertyChangeListener( new PropertyChangeListener() {
      public void propertyChange( PropertyChangeEvent e) {
        if (e.getPropertyName().equals("lookAndFeel"))
          SwingUtilities.updateComponentTreeUI( <myDialogObject>);
      }
    });
    </pre>
    */
  public PLAF(JComponent tree) {
    this.tree = tree;
  }

    /** Add a set of MenuItems to the given JMenu, one for each possible L&F.
     *  if this platform doesnt support the L&F, disable the MenuItem.
    */
  public void addToMenu( JMenu menu) {
    if (debug) System.out.println("PLAF LookAndFeelInfo  ");
    for (int i = 0; i < plafInfo.length; i++) {
      addToMenu(plafInfo[i].getName(), plafInfo[i].getClassName(), menu);
    }

    addToMenu("Plastic", "com.jgoodies.plaf.plastic.PlasticLookAndFeel", menu);
    addToMenu("Plastic3D", "com.jgoodies.plaf.plastic.Plastic3DLookAndFeel", menu);
    addToMenu("PlasticXP", "com.jgoodies.plaf.plastic.PlasticXPLookAndFeel", menu);
    addToMenu("JGoodiesWindows", "com.jgoodies.plaf.windows.ExtWindowsLookAndFeel", menu);
  }

  private void addToMenu( String name, String className, JMenu menu) {
    if (debug) System.out.println("PLAF LookAndFeelInfo  "+className);
      boolean isSupported = true;
      try {
        Class cl = Class.forName(className);
        LookAndFeel lf = (LookAndFeel) cl.newInstance();
        if (!lf.isSupportedLookAndFeel())
          isSupported = false;
      } catch (Throwable t) {
        isSupported = false;
      }

      AbstractAction act = new PLAFAction( name, className);
      JMenuItem mi = menu.add( act);
      if (!isSupported)
        mi.setEnabled(false);
  }


  private void configureLF() {
    /*


import com.jgoodies.plaf.FontSizeHints;
import com.jgoodies.plaf.LookUtils;
import com.jgoodies.plaf.Options;

  try {
      UIManager.setLookAndFeel(new com.jgoodies.plaf.plastic.PlasticLookAndFeel());
    } catch (Exception e) {}

    // for webstart
    UIManager.put("ClassLoader", LookUtils.class.getClassLoader());
    //com.jgoodies.clearlook.ClearLookManager.setMode(com.jgoodies.clearlook.ClearLookMode.DEBUG);

    // com.jgoodies.plaf.windows.ExtWindowsLookAndFeel
    // com.jgoodies.plaf.plastic.PlasticLookAndFeel
    // com.jgoodies.plaf.plastic.Plastic3DLookAndFeel
    //com.jgoodies.plaf.plastic.PlasticXPLookAndFeel

    UIManager.put(Options.USE_SYSTEM_FONTS_APP_KEY, Boolean.FALSE);
    Options.setGlobalFontSizeHints(FontSizeHints.MIXED);
    com.jgoodies.plaf.plastic.PlasticLookAndFeel.setFontSizeHints(FontSizeHints.LARGE);

    Options.setDefaultIconSize(new Dimension(18, 18)); */
  }


  private class PLAFAction extends AbstractAction {
    String plafClassName;

    PLAFAction( String name, String plafClassName) {
      this.plafClassName = plafClassName;
      putValue( Action.NAME, name);
    }

    public void actionPerformed(ActionEvent evt) {
      try{
        UIManager.setLookAndFeel( plafClassName);
      } catch (Exception ex){
        System.out.println(ex);
        return;
      }

      //this sets L&F for top level and its children only
      // Dialog boxes must listen fo L&F PropertyChangeEvents
      SwingUtilities.updateComponentTreeUI( tree);
    }
  }
}


/* Change History:
   $Log: PLAF.java,v $
   Revision 1.4  2004/09/30 00:33:38  caron
   *** empty log message ***

   Revision 1.3  2004/09/24 03:26:34  caron
   merge nj22

   Revision 1.2  2004/05/11 23:30:35  caron
   release 2.0a

   Revision 1.1.1.1  2002/11/23 17:49:48  caron
   thredds reorg

   Revision 1.1.1.1  2002/02/15 00:01:48  caron
   import sources

*/
