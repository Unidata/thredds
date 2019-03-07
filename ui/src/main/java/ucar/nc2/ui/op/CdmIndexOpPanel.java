/*
 * Copyright (c) 1998-2019 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.ui.op;

import thredds.featurecollection.FeatureCollectionConfig;
import ucar.nc2.ui.OpPanel;
import ucar.nc2.ui.ToolsUI;
import ucar.nc2.ui.grib.CdmIndexPanel;
import ucar.util.prefs.PreferencesExt;

import java.awt.BorderLayout;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Paths;
import javax.swing.JOptionPane;

/**
 *
 */
public class CdmIndexOpPanel extends OpPanel {
    private CdmIndexPanel indexPanel;

/** */
    public CdmIndexOpPanel(PreferencesExt p) {
        super(p, "index file:", true, false);

        indexPanel = new CdmIndexPanel(prefs, buttPanel);
        indexPanel.addPropertyChangeListener(new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent e) {
                if (e.getPropertyName().equals("openGrib2Collection")) {
                    final String collectionName = (String) e.getNewValue();
                    ToolsUI.getToolsUI().openGrib2Collection(collectionName);
                }
            }
        });

        add(indexPanel, BorderLayout.CENTER);
    }

/** */
    @Override
    public boolean process(Object o) {
        String command = (String) o;
        boolean err = false;

        try {
            indexPanel.setIndexFile(Paths.get(command), new FeatureCollectionConfig());
        }
        catch (FileNotFoundException ioe) {
            JOptionPane.showMessageDialog(null, "GribCdmIndexPanel cannot open " + command + "\n" + ioe.getMessage());
            err = true;
        }
        catch (Throwable e) {
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
        indexPanel.clear();
    }

/** */
    @Override
    public void save() {
        indexPanel.save();
        super.save();
    }
}
