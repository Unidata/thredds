/*
 * Copyright (c) 1998-2019 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.ui.op;

import ucar.nc2.ft.FeatureDataset;
import ucar.nc2.ft2.coverage.CoverageCollection;
import ucar.nc2.ft2.coverage.CoverageDatasetFactory;
import ucar.nc2.ft2.coverage.FeatureDatasetCoverage;
import ucar.nc2.ui.OpPanel;
import ucar.nc2.ui.ToolsUI;
import ucar.nc2.ui.coverage2.CoverageTable;
import ucar.nc2.ui.coverage2.CoverageViewer;
import ucar.nc2.ui.gis.shapefile.ShapeFileBean;
import ucar.nc2.ui.gis.worldmap.WorldMapBean;
import ucar.ui.widget.BAMutil;
import ucar.ui.widget.IndependentWindow;
import ucar.nc2.util.Optional;
import ucar.util.prefs.PreferencesExt;

import java.awt.BorderLayout;
import java.awt.Rectangle;
import java.lang.invoke.MethodHandles;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Formatter;
import javax.swing.AbstractButton;
import javax.swing.JOptionPane;
import javax.swing.JSplitPane;

/**
 *
 */
public class CoveragePanel extends OpPanel {

    private static final org.slf4j.Logger logger
                = org.slf4j.LoggerFactory.getLogger (MethodHandles.lookup ( ).lookupClass ( ));

    private CoverageTable dsTable;
    private CoverageViewer display;
    private JSplitPane split;
    private IndependentWindow viewerWindow;

    private FeatureDatasetCoverage covDatasetCollection;

/**
 *
 */
    public CoveragePanel(PreferencesExt prefs) {
        super(prefs, "dataset:", true, false);
        dsTable = new CoverageTable(buttPanel, prefs);
        add(dsTable, BorderLayout.CENTER);

        final AbstractButton viewButton = BAMutil.makeButtcon("alien", "Grid Viewer", false);
        viewButton.addActionListener(e -> {
            final CoverageCollection gridDataset = dsTable.getCoverageDataset();
            if (gridDataset == null) {
                return;
            }
            if (display == null) {
                makeDisplay();
            }
            display.setDataset(dsTable);
            viewerWindow.show();
        });
        buttPanel.add(viewButton);

        final AbstractButton infoButton = BAMutil.makeButtcon("Information", "Show Info", false);
        infoButton.addActionListener(e -> {
            final Formatter f = new Formatter();
            dsTable.showInfo(f);
            detailTA.setText(f.toString());
            detailTA.gotoTop();
            detailWindow.show();
        });
        buttPanel.add(infoButton);

        //dsTable.addExtra(buttPanel, fileChooser);
    }

/**
 *
 */
    private void makeDisplay() {
        viewerWindow = new IndependentWindow("Coverage Viewer", BAMutil.getImage("nj22/NetcdfUI"));

        display = new CoverageViewer((PreferencesExt) prefs.node("CoverageDisplay"), viewerWindow, fileChooser, 800);
        display.addMapBean(new WorldMapBean());
        display.addMapBean(new ShapeFileBean("WorldDetailMap", "Global Detailed Map", "nj22/WorldDetailMap", ToolsUI.WORLD_DETAIL_MAP));
        display.addMapBean(new ShapeFileBean("USDetailMap", "US Detailed Map", "nj22/USMap", ToolsUI.US_MAP));

        viewerWindow.setComponent(display);
        final Rectangle bounds = (Rectangle) ToolsUI.getPrefsBean(ToolsUI.GRIDVIEW_FRAME_SIZE,
                                                                    new Rectangle(77, 22, 700, 900));
        if (bounds.x < 0) {
            bounds.x = 0;
        }
        if (bounds.y < 0) {
            bounds.x = 0;
        }
        viewerWindow.setBounds(bounds);
    }

/** */
    @Override
    public boolean process(Object o) {
        String command = (String) o;
        boolean err = false;

        // close previous file
        try {
            closeOpenFiles();
        }
        catch (IOException ioe) {
            logger.warn("close failed");
        }

        try {
            Optional<FeatureDatasetCoverage> opt = CoverageDatasetFactory.openCoverageDataset(command);
            if (!opt.isPresent()) {
                JOptionPane.showMessageDialog(null, opt.getErrorMessage());
                return false;
            }
            covDatasetCollection = opt.get();
            if (covDatasetCollection == null) {
                return false;
            }
            dsTable.setCollection(covDatasetCollection);
            setSelectedItem(command);
        }
        catch (IOException e) {
            // e.printStackTrace();
            JOptionPane.showMessageDialog(null, String.format("CdmrFeatureDataset2.open cant open %s err=%s", command, e.getMessage()));
        }
        catch (Throwable ioe) {
            ioe.printStackTrace();
            final StringWriter sw = new StringWriter(5000);
            ioe.printStackTrace(new PrintWriter(sw));
            detailTA.setText(sw.toString());
            detailWindow.show();
            err = true;
        }

        return !err;
    }

/**
 *
 */
    public void setDataset(FeatureDataset fd) {
        if (fd == null) {
            return;
        }
        if (!(fd instanceof FeatureDatasetCoverage)) {
            return;
        }

        try {
            closeOpenFiles();
        }
        catch (IOException ioe) {
            logger.warn("close failed");
        }

        dsTable.setCollection( (FeatureDatasetCoverage) fd);
        setSelectedItem(fd.getLocation());
    }

/** */
    @Override
    public void closeOpenFiles() throws IOException {
        if (covDatasetCollection != null) {
            covDatasetCollection.close();
        }
        covDatasetCollection = null;
        dsTable.clear();
    }

/** */
    @Override
    public void save() {
        super.save();
        dsTable.save();
        if (viewerWindow != null) {
            ToolsUI.putPrefsBeanObject(ToolsUI.GRIDVIEW_FRAME_SIZE, viewerWindow.getBounds());
        }
    }
}
