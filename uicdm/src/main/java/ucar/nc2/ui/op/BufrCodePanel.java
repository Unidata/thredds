/*
 * Copyright (c) 1998-2019 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.ui.op;

import ucar.nc2.ui.OpPanel;
import ucar.util.prefs.PreferencesExt;

import java.awt.BorderLayout;

/**
 *
 */
public class BufrCodePanel extends OpPanel {
    private BufrWmoCodesPanel codeTable;

/**
 *
 */
    public BufrCodePanel(PreferencesExt p) {
        super(p, "table:", false, false, false);
        codeTable = new BufrWmoCodesPanel(prefs, buttPanel);
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
