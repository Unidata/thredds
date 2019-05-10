/*
 * Copyright (c) 1998-2019 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.ui.op;

import ucar.nc2.ui.OpPanel;
import ucar.nc2.ui.grib.Grib2TableViewer2;
import ucar.util.prefs.PreferencesExt;

import java.awt.BorderLayout;

public class Grib2TablePanel extends OpPanel {
    private Grib2TableViewer2 codeTable;

/**
 *
 */
    public Grib2TablePanel(PreferencesExt p) {
        super(p, "table:", false, false);
        codeTable = new Grib2TableViewer2(prefs, buttPanel);
        add(codeTable, BorderLayout.CENTER);
    }

/** */
    @Override
    public boolean process(Object command) {
        return true;
    }

/** */
    @Override
    public void save() {
        codeTable.save();
        super.save();
    }

/** */
    @Override
    public void closeOpenFiles() {
        // Nothing to do here.
    }
}
