/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.ui.widget;

/** common toplevel for applets (JApplet) and applications (JFrames) */

public interface TopLevel {
    /** get the getRootPaneContainer */
  public javax.swing.RootPaneContainer getRootPaneContainer();

    /** get the underlying Frame; call only if !isApplet() */
  public javax.swing.JFrame getJFrame();

    /** close and exit the progem */
  public void close();

    /** save any persistant data */
  public void save();

    /** return true if this is an Applet */
  public boolean isApplet();
}
