/*
 * Copyright (c) 1998-2019 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.ui.op;

import ucar.nc2.ui.OpPanel;
import ucar.nc2.ui.ToolsUI;
import ucar.ui.widget.BAMutil;
import ucar.ui.widget.FileManager;
import ucar.util.prefs.PreferencesExt;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import javax.swing.AbstractAction;
import javax.swing.JFileChooser;

/**
 *
 */
public class FeatureScanOpPanel extends OpPanel {
    private FeatureScanPanel ftTable;
    final FileManager dirChooser;

/**
 *
 */
    public FeatureScanOpPanel(PreferencesExt prefs) {
        super(prefs, "dir:", false, false);

        dirChooser = new FileManager(ToolsUI.getToolsFrame(), null, null,
                                (PreferencesExt) prefs.node("FeatureScanFileManager"));

        ftTable = new FeatureScanPanel(prefs);
        add(ftTable, BorderLayout.CENTER);

        ftTable.addPropertyChangeListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(final PropertyChangeEvent e) {
                if (!(e.getNewValue() instanceof String)) return;

                final String datasetName = (String) e.getNewValue();

                switch (e.getPropertyName()) {
                    case "openPointFeatureDataset":
                        ToolsUI.getToolsUI().openPointFeatureDataset(datasetName);
                        break;
                    case "openNetcdfFile":
                        ToolsUI.getToolsUI().openNetcdfFile(datasetName);
                        break;
                    case "openCoordSystems":
                        ToolsUI.getToolsUI().openCoordSystems(datasetName);
                        break;
                    case "openNcML":
                        ToolsUI.getToolsUI().openNcML(datasetName);
                        break;
                    case "openGridDataset":
                        ToolsUI.getToolsUI().openGridDataset(datasetName);
                        break;
                    case "openCoverageDataset":
                        ToolsUI.getToolsUI().openCoverageDataset(datasetName);
                        break;
                    case "openRadialDataset":
                        ToolsUI.getToolsUI().openRadialDataset(datasetName);
                        break;
                }
            }
        });

        dirChooser.getFileChooser().setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        dirChooser.setCurrentDirectory(prefs.get("currDir", "."));
        final AbstractAction fileAction = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String filename = dirChooser.chooseFilename();
                if (filename == null) { return; }
                cb.setSelectedItem(filename);
            }
        };
        BAMutil.setActionProperties(fileAction, "FileChooser", "open Local dataset...", false, 'L', -1);
        BAMutil.addActionToContainer(buttPanel, fileAction);
    }

/** */
    @Override
    public boolean process(Object o) {
        String command = (String) o;
        return ftTable.setScanDirectory(command);
    }

/** */
    @Override
    public void closeOpenFiles() {
        ftTable.clear();
    }

/** */
    @Override
    public void save() {
        dirChooser.save();
        ftTable.save();
        prefs.put("currDir", dirChooser.getCurrentDirectory());
        super.save();
    }
}
