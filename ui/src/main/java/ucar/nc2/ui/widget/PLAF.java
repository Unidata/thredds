/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.ui.widget;

import java.awt.event.*;
import javax.swing.*;

/**
 * Pluggable Look and Feel management.
 *
 * @author John Caron
 */

public class PLAF {
  private JComponent topComponent;
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
  public PLAF(JComponent topComponent) {
    this.topComponent = topComponent;
  }

  /**
   * Add a set of MenuItems to the given JMenu, one for each possible L&F.
   * if this platform doesnt support the L&F, disable the MenuItem.
   */
  public void addToMenu(JMenu menu) {
    UIManager.LookAndFeelInfo[] plafInfo = UIManager.getInstalledLookAndFeels();
    for (UIManager.LookAndFeelInfo aPlafInfo : plafInfo) {
      addToMenu(aPlafInfo.getName(), aPlafInfo.getClassName(), menu);
    }

    LookAndFeel current = UIManager.getLookAndFeel();
    System.out.printf("current L&F=%s%n", current.getName());

    /* addToMenu("Plastic", "com.jgoodies.plaf.plastic.PlasticLookAndFeel", menu);
    addToMenu("Plastic3D", "com.jgoodies.plaf.plastic.Plastic3DLookAndFeel", menu);
    addToMenu("PlasticXP", "com.jgoodies.plaf.plastic.PlasticXPLookAndFeel", menu);
    addToMenu("JGoodiesWindows", "com.jgoodies.plaf.windows.ExtWindowsLookAndFeel", menu); */
  }

  private void addToMenu(String name, String className, JMenu menu) {
    if (debug) System.out.println("PLAF LookAndFeelInfo  " + className);
    boolean isSupported = true;
    try {
      Class cl = Class.forName(className);
      LookAndFeel lf = (LookAndFeel) cl.newInstance();
      if (!lf.isSupportedLookAndFeel())
        isSupported = false;
    } catch (Throwable t) {
      isSupported = false;
    }

    AbstractAction act = new PLAFAction(name, className);
    JMenuItem mi = menu.add(act);
    if (!isSupported)
      mi.setEnabled(false);
  }


  /*private void configureLF() {
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

    Options.setDefaultIconSize(new Dimension(18, 18));
  } */


  private class PLAFAction extends AbstractAction {
    String plafClassName;

    PLAFAction(String name, String plafClassName) {
      this.plafClassName = plafClassName;
      putValue(Action.NAME, name);
    }

    public void actionPerformed(ActionEvent evt) {
      try {
        UIManager.setLookAndFeel(plafClassName);

      } catch (Exception ex) {
        ex.printStackTrace();
        return;
      }

      //this sets L&F for top level and its children only
      // Dialog boxes must listen fo L&F PropertyChangeEvents
      SwingUtilities.updateComponentTreeUI(topComponent);
    }
  }
}
