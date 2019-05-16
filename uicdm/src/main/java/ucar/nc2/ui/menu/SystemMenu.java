/*
 * Copyright (c) 1998-2019 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.ui.menu;

import thredds.inventory.bdb.MetadataManager;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.ui.ToolsUI;
import ucar.ui.widget.BAMutil;
import ucar.nc2.util.cache.FileCacheIF;
import ucar.ui.widget.PLAF;
import ucar.unidata.io.RandomAccessFile;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Formatter;
import java.util.Properties;
import javax.swing.AbstractAction;
import javax.swing.JMenu;

/**
 *
 */
public class SystemMenu extends JMenu {

    private ToolsUI toolsui;

    static boolean isCacheInit;

/**
 *
 */
    public SystemMenu(final ToolsUI tui) {
        super("System");
        setMnemonic('S');

        this.toolsui = tui;

        // Add the items
        final AbstractAction act = new AbstractAction() {
            public void actionPerformed(final ActionEvent e) {
                MetadataManager.closeAll(); // shutdown bdb
            }
        };
        BAMutil.setActionProperties(act, null, "Close BDB database", false, 'S', -1);
        BAMutil.addActionToMenu(this, act);

        final AbstractAction clearHttpStateAction = new AbstractAction() {
            public void actionPerformed(final ActionEvent e) {
                // IGNORE HttpClientManager.clearState();
            }
        };
        BAMutil.setActionProperties(clearHttpStateAction, null, "Clear Http State", false, 'S', -1);
        BAMutil.addActionToMenu(this, clearHttpStateAction);

        final AbstractAction showCacheAction = new AbstractAction() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                final Formatter f = new Formatter();
                f.format("RandomAccessFileCache contents%n");

                final FileCacheIF rafCache = RandomAccessFile.getGlobalFileCache();
                if (null != rafCache) {
                    rafCache.showCache(f);
                }
                f.format("%nNetcdfFileCache contents%n");

                final FileCacheIF cache = NetcdfDataset.getNetcdfFileCache();
                if (null != cache) {
                    cache.showCache(f);
                }
                toolsui.getDatasetViewerPanel().setText(f.toString());
                toolsui.getDatasetViewerPanel().getDetailWindow().show();
            }
        };
        BAMutil.setActionProperties(showCacheAction, null, "Show Caches", false, 'S', -1);
        BAMutil.addActionToMenu(this, showCacheAction);

        final AbstractAction clearRafCacheAction = new AbstractAction() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                FileCacheIF rafCache = RandomAccessFile.getGlobalFileCache();
                if (rafCache != null) {
                    rafCache.clearCache(true);
                }
            }
        };
        BAMutil.setActionProperties(clearRafCacheAction, null, "Clear RandomAccessFileCache", false, 'C', -1);
        BAMutil.addActionToMenu(this, clearRafCacheAction);

        final AbstractAction clearCacheAction = new AbstractAction() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                FileCacheIF cache = NetcdfDataset.getNetcdfFileCache();
                if (cache != null)
              cache.clearCache(true);
            }
        };
        BAMutil.setActionProperties(clearCacheAction, null, "Clear NetcdfDatasetCache", false, 'C', -1);
        BAMutil.addActionToMenu(this, clearCacheAction);

        final AbstractAction enableCache = new AbstractAction() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                final boolean state = (Boolean) getValue(BAMutil.STATE);
                if (state == isCacheInit) {
                    return;
                }
                isCacheInit = state;
                if (isCacheInit) {
                    FileCacheIF cache = NetcdfDataset.getNetcdfFileCache();
                    if (cache != null) {
                        cache.enable();
                    }
                    else {
                        NetcdfDataset.initNetcdfFileCache(10, 20, 10 * 60);
                    }
                }
                else {
                    FileCacheIF cache = NetcdfDataset.getNetcdfFileCache();
                    if (cache != null) {
                        cache.disable();
                    }
                }
            }
        };
        BAMutil.setActionPropertiesToggle(enableCache, null, "Enable NetcdfDatasetCache", isCacheInit, 'N', -1);
        BAMutil.addActionToMenu(this, enableCache);

        final AbstractAction showPropertiesAction = new AbstractAction() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                toolsui.getDatasetViewerPanel().setText("System Properties\n");
                final Properties sysp = System.getProperties();
                final Enumeration eprops = sysp.propertyNames();
                final ArrayList<String> list = Collections.list(eprops);
                Collections.sort(list);

                for (Object aList : list) {
                    final String name = (String) aList;
                    final String value = System.getProperty(name);
                    toolsui.getDatasetViewerPanel().appendLine("  " + name + " = " + value);
                }
                toolsui.getDatasetViewerPanel().getDetailWindow().show();
            }
        };
        BAMutil.setActionProperties(showPropertiesAction, null, "System Properties", false, 'P', -1);
        BAMutil.addActionToMenu(this, showPropertiesAction);

        addPlafSubmenu();

        final AbstractAction exitAction = new AbstractAction() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                toolsui.exit();
            }
        };
        BAMutil.setActionProperties(exitAction, "Exit", "Exit", false, 'X', -1);
        BAMutil.addActionToMenu(this, exitAction);
    }

/**
 *
 */
    private void addPlafSubmenu() {
        final JMenu plafMenu = new JMenu("Look and Feel");
        plafMenu.setMnemonic('L');

        final PLAF plaf = new PLAF(toolsui);
        plaf.addToMenu(plafMenu);

        add(plafMenu);
    }
}
