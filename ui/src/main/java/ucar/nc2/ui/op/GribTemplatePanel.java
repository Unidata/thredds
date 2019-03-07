/*
 * Copyright (c) 1998-2019 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.ui.op;

import ucar.nc2.grib.grib2.table.WmoTemplateTable;
import ucar.nc2.ui.OpPanel;
import ucar.nc2.ui.grib.GribWmoTemplatesPanel;
import ucar.util.prefs.PreferencesExt;

import java.awt.BorderLayout;
import javax.swing.JComboBox;

/**
 *
 */
public class GribTemplatePanel extends OpPanel {
    private GribWmoTemplatesPanel codeTable;

/**
 *
 */
    public GribTemplatePanel(PreferencesExt p) {
        super(p, "table:", false, false, false);

        final JComboBox<WmoTemplateTable.Version> modes = new JComboBox<>(WmoTemplateTable.Version.values());
        modes.setSelectedItem(WmoTemplateTable.standard);
        topPanel.add(modes, BorderLayout.CENTER);
        modes.addActionListener(e -> {
            codeTable.setTable((WmoTemplateTable.Version) modes.getSelectedItem());
        });

        codeTable = new GribWmoTemplatesPanel(prefs, buttPanel);
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
