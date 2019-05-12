/*
 * Copyright (c) 1998-2019 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.ui.op;

import ucar.nc2.ui.OpPanel;
import ucar.nc2.ui.ToolsUI;
import ucar.ui.widget.BAMutil;
import ucar.util.prefs.PreferencesExt;

import java.awt.BorderLayout;
import javax.swing.AbstractButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JSplitPane;

/**
 *
 */
public class WmsPanel extends OpPanel {
    private WmsViewer wmsViewer;
    private JSplitPane split;
    private JComboBox<String> types;

/**
 *
 */
    public WmsPanel(PreferencesExt dbPrefs) {
        super(dbPrefs, "dataset:", true, false);
        wmsViewer = new WmsViewer(dbPrefs, ToolsUI.getToolsFrame());
        add(wmsViewer, BorderLayout.CENTER);

        buttPanel.add(new JLabel("version:"));
        types = new JComboBox<>();
        types.addItem("1.3.0");
        types.addItem("1.1.1");
        types.addItem("1.0.0");
        buttPanel.add(types);

        final AbstractButton infoButton = BAMutil.makeButtcon("Information", "Detail Info", false);
        infoButton.addActionListener(e -> {
            detailTA.setText(wmsViewer.getDetailInfo());
            detailTA.gotoTop();
            detailWindow.show();
        });
        buttPanel.add(infoButton);
    }

/** */
    @Override
    public boolean process(Object o) {
        String location = (String) o;
        return wmsViewer.setDataset((String) types.getSelectedItem(), location);
    }

/** */
    @Override
    public void closeOpenFiles() {
        // Nothing to do here.
    }

/** */
    @Override
    public void save() {
        super.save();
        wmsViewer.save();
    }
}
