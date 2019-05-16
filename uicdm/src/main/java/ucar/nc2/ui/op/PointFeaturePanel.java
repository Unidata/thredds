/*
 * Copyright (c) 1998-2019 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.ui.op;

import ucar.nc2.constants.FeatureType;
import ucar.nc2.ft.FeatureDataset;
import ucar.nc2.ft.FeatureDatasetFactoryManager;
import ucar.nc2.ft.FeatureDatasetPoint;
import ucar.nc2.ft.point.standard.PointConfigXML;
import ucar.nc2.ft.point.writer.FeatureDatasetCapabilitiesWriter;
import ucar.nc2.ui.OpPanel;
import ucar.ui.widget.BAMutil;
import ucar.util.prefs.PreferencesExt;

import java.awt.BorderLayout;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.invoke.MethodHandles;
import java.util.Formatter;
import javax.swing.AbstractButton;
import javax.swing.JComboBox;
import javax.swing.JOptionPane;
import javax.swing.JSplitPane;

/**
 *
 */
public  class PointFeaturePanel extends OpPanel {

    private static final org.slf4j.Logger logger
                = org.slf4j.LoggerFactory.getLogger (MethodHandles.lookup ( ).lookupClass ( ));

    private PointFeatureDatasetViewer pfViewer;
    private JSplitPane split;
    private FeatureDatasetPoint pfDataset;
    private JComboBox<FeatureType> types;

/**
 *
 */
    public PointFeaturePanel(PreferencesExt dbPrefs) {
        super(dbPrefs, "dataset:", true, false);
        pfViewer = new PointFeatureDatasetViewer(dbPrefs, buttPanel);
        add(pfViewer, BorderLayout.CENTER);

        types = new JComboBox<>();
        for (FeatureType ft : FeatureType.values()) {
            types.addItem(ft);
        }
        types.getModel().setSelectedItem(FeatureType.ANY_POINT);
        buttPanel.add(types);

        final AbstractButton infoButton = BAMutil.makeButtcon("Information", "Dataset Info", false);
        infoButton.addActionListener(e -> {
            if (pfDataset == null) {
                return;
            }
            final Formatter f = new Formatter();
            pfDataset.getDetailInfo(f);
            detailTA.setText(f.toString());
            detailTA.appendLine("-----------------------------");
            detailTA.appendLine(getCapabilities(pfDataset));
            detailTA.gotoTop();
            detailWindow.show();
        });
        buttPanel.add(infoButton);

        final AbstractButton calcButton = BAMutil.makeButtcon("nj22/V3", "Calculate the latlon/dateRange", false);
        calcButton.addActionListener(e -> {
            if (pfDataset == null) {
                return;
            }
            final Formatter f = new Formatter();
            pfDataset.calcBounds(f);
            detailTA.setText(f.toString());
            detailTA.gotoTop();
            detailWindow.show();
        });
        buttPanel.add(calcButton);

        final AbstractButton xmlButton = BAMutil.makeButtcon("nj22/XML", "pointConfig.xml", false);
        xmlButton.addActionListener(e -> {
            if (pfDataset == null) {
                return;
            }
            final Formatter f = new Formatter();
            PointConfigXML.writeConfigXML(pfDataset, f);
            detailTA.setText(f.toString());
            detailTA.gotoTop();
            detailWindow.show();
        });
        buttPanel.add(xmlButton);
    }

/** */
    @Override
    public boolean process(Object o) {
        String location = (String) o;
        return setPointFeatureDataset((FeatureType) types.getSelectedItem(), location);
    }

/** */
    @Override
    public void closeOpenFiles() throws IOException {
        if (pfDataset != null) {
            pfDataset.close();
        }
        pfDataset = null;
        pfViewer.clear();
    }

/** */
    @Override
    public void save() {
        super.save();
        pfViewer.save();
    }

/**
 *
 */
     public boolean setPointFeatureDataset(FeatureType type, String location) {
        if (location == null) {
            return false;
        }

        try {
            if (pfDataset != null) {
                pfDataset.close();
            }
        }
        catch (IOException ioe) {
            logger.warn("close failed");
        }
        detailTA.clear();

        Formatter log = new Formatter();
        try {
            FeatureDataset featureDataset = FeatureDatasetFactoryManager.open(type, location, null, log);
            if (featureDataset == null) {
                JOptionPane.showMessageDialog(null, "Can't open " + location + ": " + log);
                return false;
            }
            if (!(featureDataset instanceof FeatureDatasetPoint)) {
                JOptionPane.showMessageDialog(null, location + " could not be opened as a PointFeatureDataset");
                return false;
            }

            pfDataset = (FeatureDatasetPoint) featureDataset;
            pfViewer.setDataset(pfDataset);
            setSelectedItem(location);
            return true;
        }
        catch (IOException e) {
            String message = e.getClass().getName() + ": " + e.getMessage();
            JOptionPane.showMessageDialog(this, message);
            return false;
        }
        catch (Throwable e) {
            StringWriter sw = new StringWriter(5000);
            e.printStackTrace(new PrintWriter(sw));
            detailTA.setText(log.toString());
            detailTA.setText(sw.toString());
            detailWindow.show();

            JOptionPane.showMessageDialog(this, e.getMessage());
            return false;
        }
    }

/**
 *
 */
    public boolean setPointFeatureDataset(FeatureDatasetPoint pfd) {

        try {
            if (pfDataset != null) {
                pfDataset.close();
            }
        }
        catch (IOException ioe) {
            logger.warn("close failed");
        }
        detailTA.clear();

        try {
            pfDataset = pfd;
            pfViewer.setDataset(pfDataset);
            setSelectedItem(pfDataset.getLocation());
            return true;
        }
        catch (Throwable e) {
            StringWriter sw = new StringWriter(5000);
            e.printStackTrace(new PrintWriter(sw));
            detailTA.setText(sw.toString());
            detailWindow.show();

            JOptionPane.showMessageDialog(this, e.getMessage());
            return false;
        }
    }

/**
 *
 */
    private String getCapabilities(FeatureDatasetPoint fdp) {
        FeatureDatasetCapabilitiesWriter xmlWriter = new FeatureDatasetCapabilitiesWriter(fdp, null);
        return xmlWriter.getCapabilities();
    }
}
