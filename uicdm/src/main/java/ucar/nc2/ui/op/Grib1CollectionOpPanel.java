/*
 * Copyright (c) 1998-2019 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.ui.op;

import ucar.nc2.ui.OpPanel;
import ucar.nc2.ui.grib.Grib1CollectionPanel;
import ucar.ui.widget.BAMutil;
import ucar.util.prefs.PreferencesExt;

import java.awt.BorderLayout;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Formatter;
import javax.swing.AbstractButton;
import javax.swing.JOptionPane;

/**
 *  raw grib access - dont go through the IOSP
 */
public class Grib1CollectionOpPanel extends OpPanel {
    private Grib1CollectionPanel gribTable;

/**
 *
 */
    public Grib1CollectionOpPanel(PreferencesExt p) {
        super(p, "collection:", true, false);

        gribTable = new Grib1CollectionPanel(buttPanel, prefs);
        add(gribTable, BorderLayout.CENTER);

        final AbstractButton showButt = BAMutil.makeButtcon("Information", "Show Collection", false);
        showButt.addActionListener(e -> {
            final Formatter f = new Formatter();
            gribTable.showCollection(f);
            detailTA.setText(f.toString());
            detailTA.gotoTop();
            detailWindow.show();
        });
        buttPanel.add(showButt);

        final AbstractButton writeButton = BAMutil.makeButtcon("nj22/Netcdf", "Write index", false);
        writeButton.addActionListener(e -> {
            final Formatter f = new Formatter();
            try {
                if (!gribTable.writeIndex(f)) {
                    return;
                }
            }
            catch (IOException e1) {
                e1.printStackTrace();
            }
            detailTA.setText(f.toString());
            detailTA.gotoTop();
            detailWindow.show();
        });
        buttPanel.add(writeButton);
    }

/**
 *
 */
    public void setCollection(String collection) {
        if (process(collection)) {
            cb.addItem(collection);
        }
    }

/** */
    @Override
    public boolean process(Object o) {
        String command = (String) o;
        boolean err = false;

        try {
            gribTable.setCollection(command);
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
