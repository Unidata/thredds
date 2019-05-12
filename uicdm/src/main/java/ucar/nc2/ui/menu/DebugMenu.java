/*
 * Copyright (c) 1998-2019 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.ui.menu;

import ucar.nc2.ui.ToolsUI;
import ucar.ui.widget.BAMutil;
import ucar.ui.prefs.Debug;

import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import javax.swing.JMenu;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;

/**
 *
 */
public class DebugMenu extends JMenu {

    private ToolsUI toolsui;

/**
 *
 */
    public DebugMenu(final ToolsUI tui) {
        super("Debug");
        setMnemonic('D');

        this.toolsui = tui;

        // the list of debug flags are in a submenu
        // they are dynamically discovered, and persisted
        final JMenu flagsMenu = new JMenu("Debug Flags");

        flagsMenu.addMenuListener(new MenuListener() {
            @Override
            public void menuSelected(MenuEvent e) {
                toolsui.setDebugFlags(); // let Debug know about the flag names
                Debug.constructMenu(flagsMenu); // now construct the menu
            }

            @Override
            public void menuDeselected(MenuEvent e) {
                toolsui.setDebugFlags(); // transfer menu values
            }

            @Override
            public void menuCanceled(MenuEvent e) { }
        });

        add(flagsMenu);

        // this deletes all the flags, then they start accumulating again
        final AbstractAction clearDebugFlagsAction = new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                Debug.removeAll();
            }
        };
        BAMutil.setActionProperties(clearDebugFlagsAction, null, "Delete All Debug Flags", false, 'C', -1);
        BAMutil.addActionToMenu(this, clearDebugFlagsAction);
    }
}
