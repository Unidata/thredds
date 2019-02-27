/*
 * Copyright (c) 1998-2019 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.ui;

import ucar.nc2.ui.widget.BAMutil;

import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import javax.swing.JFrame;
import javax.swing.JMenu;

/**
 *
 */
class ToolsHelpMenu extends JMenu {

    private ToolsUI toolsui;
    private ToolsAboutWindow aboutWindow;

/**
 *
 */
    ToolsHelpMenu(final ToolsUI tui) {
        super("Help");
        setMnemonic('H');

        this.toolsui = tui;

        final AbstractAction aboutAction = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                if (aboutWindow == null) {
                    final JFrame parentFrame = (JFrame)toolsui.getTopLevelAncestor ( );

                    aboutWindow = new ToolsAboutWindow(parentFrame);
                }
                aboutWindow.setVisible(true);
            }
        };
        BAMutil.setActionProperties(aboutAction, null, "About", false, 'A', 0);
        BAMutil.addActionToMenu(this, aboutAction);

        final AbstractAction logoAction = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                ToolsSplashScreen.getSharedInstance().setVisible(true);
            }
        };
        BAMutil.setActionProperties(logoAction, null, "Logo", false, 'L', 0);
        BAMutil.addActionToMenu(this, logoAction);
    }
}
