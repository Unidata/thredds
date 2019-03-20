/*
 * Copyright (c) 1998-2019 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.ui.op;

import ucar.util.prefs.PreferencesExt;

import java.awt.BorderLayout;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;

/**
 *
 */
public class UnitsPanel extends JPanel {
    private PreferencesExt prefs;
    private JSplitPane split;
    private JSplitPane split2;
    private UnitDatasetCheck unitDataset;
    private UnitConvert unitConvert;
    private DateFormatMark dateFormatMark;

/**
 *
 */
    public UnitsPanel(final PreferencesExt prefs) {
        this.prefs = prefs;

        unitDataset    = new UnitDatasetCheck((PreferencesExt) prefs.node("unitDataset"));
        unitConvert    = new UnitConvert((PreferencesExt) prefs.node("unitConvert"));
        dateFormatMark = new DateFormatMark((PreferencesExt) prefs.node("dateFormatMark"));

        split2 = new JSplitPane(JSplitPane.VERTICAL_SPLIT, unitConvert, dateFormatMark);
        split2.setDividerLocation(prefs.getInt("splitPos2", 500));

        split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, new JScrollPane(unitDataset), split2);
        split.setDividerLocation(prefs.getInt("splitPos", 500));

        setLayout(new BorderLayout());
        add(split, BorderLayout.CENTER);
    }

/**
 *
 */
    public void save() {
        prefs.putInt("splitPos", split.getDividerLocation());
        prefs.putInt("splitPos2", split2.getDividerLocation());
        unitConvert.save();
        unitDataset.save();
    }
}

