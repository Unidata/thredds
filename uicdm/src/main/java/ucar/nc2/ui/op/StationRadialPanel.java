/*
 * Copyright (c) 1998-2019 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.ui.op;

import thredds.client.catalog.tools.DataFactory;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.ft.FeatureDataset;
import ucar.nc2.ui.OpPanel;
import ucar.nc2.ui.ToolsUI;
import ucar.ui.widget.BAMutil;
import ucar.util.prefs.PreferencesExt;

import java.awt.BorderLayout;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.invoke.MethodHandles;
import java.util.Formatter;
import javax.swing.AbstractButton;
import javax.swing.JOptionPane;
import javax.swing.JSplitPane;

/**
 *
 */
public class StationRadialPanel extends OpPanel {

    private static final org.slf4j.Logger logger
                            = org.slf4j.LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private StationRadialViewer radialViewer;
    private JSplitPane split;
    private FeatureDataset radarCollectionDataset;

/**
 *
 */
    public StationRadialPanel(PreferencesExt dbPrefs) {
        super(dbPrefs, "dataset:", true, false);
        radialViewer = new StationRadialViewer(dbPrefs);
        add(radialViewer, BorderLayout.CENTER);

        final AbstractButton infoButton = BAMutil.makeButtcon("Information", "Dataset Info", false);
        infoButton.addActionListener(e -> {
            if (radarCollectionDataset != null) {
                final Formatter info = new Formatter();
                radarCollectionDataset.getDetailInfo(info);
                detailTA.setText(info.toString());
                detailTA.gotoTop();
                detailWindow.show();
            }
        });
        buttPanel.add(infoButton);
    }

/** */
    @Override
    public boolean process(Object o) {
        String location = (String) o;
        return setStationRadialDataset(location);
    }

/** */
    @Override
    public void closeOpenFiles() throws IOException {
        if (radarCollectionDataset != null) {
            radarCollectionDataset.close();
        }
        radarCollectionDataset = null;
    }

/** */
    @Override
    public void save() {
        super.save();
        radialViewer.save();
    }

/**
 *
 */
    public boolean setStationRadialDataset(String location) {
        if (location == null) {
            return false;
        }

        try {
            if (radarCollectionDataset != null) radarCollectionDataset.close();
        }
        catch (IOException ioe) {
            logger.warn("close failed");
        }

        DataFactory.Result result = null;
        try {
            result = ToolsUI.getThreddsDataFactory().openFeatureDataset(
                                                FeatureType.STATION_RADIAL, location, null);
            if (result.fatalError) {
                JOptionPane.showMessageDialog(null, "Can't open " + location + ": " + result.errLog.toString());
                return false;
            }

            setStationRadialDataset(result.featureDataset);
            return true;
        }
        catch (Exception e) {
            final StringWriter sw = new StringWriter(5000);
            e.printStackTrace(new PrintWriter(sw));
            detailTA.setText(sw.toString());
            detailWindow.show();

            JOptionPane.showMessageDialog(this, e.getMessage());
            if (result != null) {
                try {
                    result.close();
                }
                catch (IOException ioe2) {
                    JOptionPane.showMessageDialog(null, "Can't open " + location + ": " + ioe2.getMessage());
                }
            }
            return false;
        }
    }

/**
 *
 */
    public boolean setStationRadialDataset(final FeatureDataset dataset) {
        if (dataset == null) {
            return false;
        }

        try {
            if (radarCollectionDataset != null) {
                radarCollectionDataset.close();
            }
        }
        catch (IOException ioe) {
            logger.warn("close failed");
        }

        radarCollectionDataset = dataset;
        radialViewer.setDataset(radarCollectionDataset);
        setSelectedItem(radarCollectionDataset.getLocation());
        return true;
    }
}
