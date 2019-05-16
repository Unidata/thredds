/*
 * Copyright (c) 1998-2019 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.ui.op;

import ucar.nc2.ui.OpPanel;
import ucar.nc2.ui.ToolsUI;
import ucar.nc2.ui.grib.GribRewritePanel;
import ucar.ui.widget.BAMutil;
import ucar.ui.widget.FileManager;
import ucar.util.prefs.PreferencesExt;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.invoke.MethodHandles;
import javax.swing.AbstractAction;
import javax.swing.JFileChooser;

/**
 *
 */
public class GribRewriteOpPanel extends OpPanel {

    private static final org.slf4j.Logger logger
                = org.slf4j.LoggerFactory.getLogger (MethodHandles.lookup ( ).lookupClass ( ));

    private GribRewritePanel ftTable;
    private final FileManager dirChooser;

/**
 *
 */
    public GribRewriteOpPanel(final PreferencesExt prefs) {
        super(prefs, "dir:", false, false);
        dirChooser = new FileManager(ToolsUI.getToolsFrame(), null, null,
                                (PreferencesExt) prefs.node("FeatureScanFileManager"));
        ftTable = new GribRewritePanel(prefs, buttPanel);
        add(ftTable, BorderLayout.CENTER);

        ftTable.addPropertyChangeListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent e) {
                if (!(e.getNewValue() instanceof String)) return;

                final String pname = e.getPropertyName();

                final String datasetName = (String) e.getNewValue();

                if ("openNetcdfFile".equals (pname)) {
                    ToolsUI.getToolsUI().openNetcdfFile(datasetName);
                }
                else if ("openGridDataset".equals (pname)) {
                    ToolsUI.getToolsUI().openGridDataset(datasetName);
                }
                else if ("openGrib1Data".equals (pname)) {
                    ToolsUI.getToolsUI().openGrib1Data(datasetName);
                }
                else if ("openGrib2Data".equals (pname)) {
                    ToolsUI.getToolsUI().openGrib2Data(datasetName);
                }
                else {
                    logger.debug("Unknown popertry name {}", pname);
                }
            }
        });

        dirChooser.getFileChooser().setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        dirChooser.setCurrentDirectory(prefs.get("currDir", "."));
        final AbstractAction fileAction = new AbstractAction() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                final String filename = dirChooser.chooseFilename();
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
