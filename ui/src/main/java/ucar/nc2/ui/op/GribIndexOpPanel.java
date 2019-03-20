/*
 * Copyright (c) 1998-2019 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.ui.op;

import ucar.nc2.ui.OpPanel;
import ucar.nc2.ui.grib.GribIndexPanel;
import ucar.util.prefs.PreferencesExt;

import java.awt.BorderLayout;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import javax.swing.JOptionPane;

/**
 *
 */
public class GribIndexOpPanel extends OpPanel {
    private GribIndexPanel gribTable;

/**
 *
 */
    public GribIndexOpPanel(PreferencesExt p) {
        super(p, "index file:", true, false);
        gribTable = new GribIndexPanel(prefs, buttPanel);
        add(gribTable, BorderLayout.CENTER);
    }

/** */
    @Override
    public boolean process(Object o) {
        String command = (String) o;
        boolean err = false;

        try {
            gribTable.setIndexFile(command);
        }
        catch (FileNotFoundException ioe) {
            JOptionPane.showMessageDialog(null, "NetcdfDataset cannot open " + command + "\n" + ioe.getMessage());
            err = true;
        }
        catch (Exception e) {
            e.printStackTrace();
            final StringWriter sw = new StringWriter(5000);
            e.printStackTrace(new PrintWriter(sw));
            detailTA.setText(sw.toString());
            detailWindow.show();
            err = true;
        }

        return !err;
    }

/** */
    @Override
    public void closeOpenFiles() throws IOException {
        gribTable.closeOpenFiles();
    }

/** */
    @Override
    public void save() {
        gribTable.save();
        super.save();
    }
}
