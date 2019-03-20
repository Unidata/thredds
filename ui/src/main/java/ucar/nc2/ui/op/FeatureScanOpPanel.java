/*
 * Copyright (c) 1998-2019 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.ui.op;

import ucar.nc2.ui.OpPanel;
import ucar.nc2.ui.ToolsUI;
import ucar.nc2.ui.widget.BAMutil;
import ucar.nc2.ui.widget.FileManager;
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

                final String datasetName = (String) e.getNewValue();

                if (e.getPropertyName().equals("openPointFeatureDataset")) {
                    ToolsUI.getToolsUI().openPointFeatureDataset(datasetName);
                }
                else if (e.getPropertyName().equals("openNetcdfFile")) {
                    ToolsUI.getToolsUI().openNetcdfFile(datasetName);
                }
                else if (e.getPropertyName().equals("openCoordSystems")) {
                    ToolsUI.getToolsUI().openCoordSystems(datasetName);
                }
                else if (e.getPropertyName().equals("openNcML")) {
                    ToolsUI.getToolsUI().openNcML(datasetName);
                }
                else if (e.getPropertyName().equals("openGridDataset")) {
                    ToolsUI.getToolsUI().openGridDataset(datasetName);
                }
                else if (e.getPropertyName().equals("openCoverageDataset")) {
                    ToolsUI.getToolsUI().openCoverageDataset(datasetName);
                }
                else if (e.getPropertyName().equals("openRadialDataset")) {
                    ToolsUI.getToolsUI().openRadialDataset(datasetName);
                }
            }
        });

        dirChooser.getFileChooser().setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        dirChooser.setCurrentDirectory(prefs.get("currDir", "."));
        final AbstractAction fileAction = new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                String filename = dirChooser.chooseFilename();
                if (filename == null) {
                    return;
                }
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
