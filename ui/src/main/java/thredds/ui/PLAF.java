// $Id: PLAF.java 50 2006-07-12 16:30:06Z caron $
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
