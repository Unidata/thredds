/*
 * Copyright (c) 1998-2019 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.ui.op;

import ucar.nc2.grib.grib2.table.WmoCodeFlagTables;
import ucar.nc2.ui.OpPanel;
import ucar.nc2.ui.grib.GribWmoCodesPanel;
import ucar.util.prefs.PreferencesExt;

import java.awt.BorderLayout;
import javax.swing.JComboBox;

public class GribCodePanel extends OpPanel {
    private GribWmoCodesPanel codeTable;

    public GribCodePanel(PreferencesExt p) {
        super(p, "table:", false, false, false);

        final JComboBox<WmoCodeFlagTables.Version> modes = new JComboBox<>(WmoCodeFlagTables.Version.values());
        modes.setSelectedItem(WmoCodeFlagTables.standard);
        topPanel.add(modes, BorderLayout.CENTER);
        modes.addActionListener(e -> codeTable.setTable((WmoCodeFlagTables.Version) modes.getSelectedItem()));

        codeTable = new GribWmoCodesPanel(prefs, buttPanel);
        add(codeTable, BorderLayout.CENTER);
    }

    @Override
    public boolean process(Object command) {
        return true;
    }

    @Override
    public void save() {
        codeTable.save();
        super.save();
    }
}
