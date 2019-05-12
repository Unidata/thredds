/*
 * Copyright (c) 1998-2019 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.ui.op;

import ucar.nc2.NetcdfFile;
import ucar.nc2.ui.OpPanel;
import ucar.nc2.ui.ToolsUI;
import ucar.util.prefs.PreferencesExt;

import java.awt.BorderLayout;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.invoke.MethodHandles;
import javax.swing.JSplitPane;

/**
 *
 */
public class DatasetWriterPanel extends OpPanel {

    private static final org.slf4j.Logger logger
                = org.slf4j.LoggerFactory.getLogger (MethodHandles.lookup ( ).lookupClass ( ));

    private DatasetWriter dsWriter;
    private JSplitPane split;
    private NetcdfFile ncfile;

/**
 *
 */
    public  DatasetWriterPanel(PreferencesExt dbPrefs) {
        super(dbPrefs, "dataset:");
        dsWriter = new DatasetWriter(dbPrefs, fileChooser);
        add(dsWriter, BorderLayout.CENTER);
        dsWriter.addActions(buttPanel);
    }

/** */
    @Override
    public boolean process(Object o) {
        String command = (String) o;
        boolean err = false;

        try {
            if (ncfile != null) {
                ncfile.close();
            }
        }
        catch (IOException ioe) {
            logger.warn("close failed");
        }

        try {
            NetcdfFile ncnew = ToolsUI.getToolsUI().openFile(command, addCoords, null);
            if (ncnew != null) {
                setDataset(ncnew);
            }
        }
        catch (Exception ioe) {
            final StringWriter sw = new StringWriter(5000);
            ioe.printStackTrace(new PrintWriter(sw));
            detailTA.setText(sw.toString());
            detailWindow.show();
            err = true;
        }

        return (! err);
    }

/** */
    @Override
    public void closeOpenFiles() throws IOException {
        if (ncfile != null) {
            ncfile.close();
        }
        ncfile = null;
    }

/**
 *
 */
    void setDataset(NetcdfFile nc) {
        try {
            if (ncfile != null) {
                ncfile.close();
            }
            ncfile = null;
        }
        catch (IOException ioe) {
            logger.warn("close failed");
        }
        ncfile = nc;

        if (nc != null) {
            dsWriter.setDataset(nc);
            setSelectedItem(nc.getLocation());
        }
    }

/** */
    @Override
    public void save() {
        super.save();
        dsWriter.save();
    }
}
