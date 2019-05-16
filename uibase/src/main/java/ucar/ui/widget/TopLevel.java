/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.ui.widget;

/** common toplevel for applets (JApplet) and applications (JFrames) */

public interface TopLevel {
    /** get the getRootPaneContainer */
    javax.swing.RootPaneContainer getRootPaneContainer();

    /** get the underlying Frame; call only if !isApplet() */
    javax.swing.JFrame getJFrame();

    /** close and exit the progem */
    void close();

    /** save any persistant data */
    void save();

    /** return true if this is an Applet */
    boolean isApplet();
}
