/*
 * Copyright (c) 1998-2019 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.ui.op;

import ucar.nc2.NetcdfFile;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.ui.OpPanel;
import ucar.nc2.ui.ToolsUI;
import ucar.util.prefs.PreferencesExt;

import java.awt.BorderLayout;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import javax.swing.JOptionPane;

/**
 *
 */
public class AggPanel extends OpPanel {
    private AggTable aggTable;
    private NetcdfDataset ncd;

/**
 *
 */
    public AggPanel(final PreferencesExt p) {
        super(p, "file:", true, false);
        aggTable = new AggTable(prefs, buttPanel);
        aggTable.addPropertyChangeListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent e) {
                switch (e.getPropertyName()) {
                    case "openNetcdfFile": {
                        final NetcdfFile ncfile = (NetcdfFile) e.getNewValue();
                        if (ncfile != null) {
                            ToolsUI.getToolsUI().openNetcdfFile(ncfile);
                        }
                        break;
                    }
                    case "openCoordSystems": {
                        final NetcdfFile ncfile = (NetcdfFile) e.getNewValue();
                        if (ncfile == null) {
                            return;
                        }
                        try {
                            final NetcdfDataset ncd = NetcdfDataset
                                .wrap(ncfile, NetcdfDataset.getDefaultEnhanceMode());
                            ToolsUI.getToolsUI().openCoordSystems(ncd);
                        } catch (IOException e1) {
                            e1.printStackTrace();
                        }
                        break;
                    }
                    case "openGridDataset": {
                        NetcdfFile ncfile = (NetcdfFile) e.getNewValue();
                        if (ncfile == null)
                            return;
                        try {
                            NetcdfDataset ncd = NetcdfDataset
                                .wrap(ncfile, NetcdfDataset.getDefaultEnhanceMode());
                            ToolsUI.getToolsUI().openGridDataset(ncd);
                        } catch (IOException e1) {
                            e1.printStackTrace();
                        }
                        break;
                    }
                }
            }
        });

        add(aggTable, BorderLayout.CENTER);
    }

/** */
    @Override
    public boolean process(Object o) {
        String command = (String) o;
        boolean err = false;

        try {
            if (ncd != null) {
                try {
                    ncd.close();
                }
                catch (IOException ioe) {
                    ioe.printStackTrace();
                }
            }

            ncd = NetcdfDataset.openDataset(command);
            aggTable.setAggDataset(ncd);
        }
        catch (FileNotFoundException ioe) {
            JOptionPane.showMessageDialog(null, "NetcdfDataset cannot open " + command + "\n" + ioe.getMessage());
            err = true;
        }
        catch (Throwable e) {
            e.printStackTrace();
            final StringWriter sw = new StringWriter(5000);
            e.printStackTrace(new PrintWriter(sw));
            detailTA.setText(sw.toString());
            detailTA.gotoTop();
            detailWindow.show();
            err = true;
        }

        return !err;
    }

/** */
    @Override
    public void closeOpenFiles() throws IOException {
        if (ncd != null) {
            ncd.close();
        }
        ncd = null;
        aggTable.clear();
    }

/** */
    @Override
    public void save() {
        aggTable.save();
        super.save();
    }
}
