/*
 * Copyright (c) 1998-2019 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.ui.op;

import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dt.GridDataset;
import ucar.nc2.dt.GridDatatype;
import ucar.nc2.ui.OpPanel;
import ucar.nc2.ui.ToolsUI;
import ucar.nc2.ui.gis.shapefile.ShapeFileBean;
import ucar.nc2.ui.gis.worldmap.WorldMapBean;
import ucar.nc2.ui.image.ImageViewPanel;
import ucar.nc2.ui.simplegeom.SimpleGeomTable;
import ucar.nc2.ui.simplegeom.SimpleGeomUI;
import ucar.ui.widget.BAMutil;
import ucar.ui.widget.IndependentWindow;
import ucar.util.prefs.PreferencesExt;

import java.awt.BorderLayout;
import java.awt.Rectangle;
import java.lang.invoke.MethodHandles;
import java.io.FileNotFoundException;
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
public class SimpleGeomPanel extends OpPanel {

    private static final org.slf4j.Logger logger
                = org.slf4j.LoggerFactory.getLogger (MethodHandles.lookup ( ).lookupClass ( ));

    private SimpleGeomTable sgTable;
    private JSplitPane split;
    private IndependentWindow viewerWindow, imageWindow;
    private SimpleGeomUI sgUI;
    private ImageViewPanel imageViewer;

    private NetcdfDataset ds;

/**
 *
 */
    public SimpleGeomPanel(PreferencesExt prefs) {
        super(prefs, "dataset:", true, false);
        sgTable = new SimpleGeomTable(prefs, true);
        add(sgTable, BorderLayout.CENTER);

        final AbstractButton viewButton = BAMutil.makeButtcon("alien", "Grid Viewer", false);
        viewButton.addActionListener(e -> {
            if (ds != null) {
                GridDataset gridDataset = sgTable.getGridDataset();
                if (sgUI == null) {
                    makeSimpleGeomUI();
                }
                sgUI.setDataset(gridDataset);
                viewerWindow.show();
            }
        });
        buttPanel.add(viewButton);

        final AbstractButton imageButton = BAMutil.makeButtcon("VCRMovieLoop", "Image Viewer", false);
        imageButton.addActionListener(e ->{
            if (ds != null) {
                GridDatatype grid = sgTable.getGrid();
                if (grid == null) {
                    return;
                }
                if (imageWindow == null) {
                    makeImageWindow();
                }
                imageViewer.setImageFromGrid(grid);
                imageWindow.show();
                }
        });
        buttPanel.add(imageButton);

        sgTable.addExtra(buttPanel, fileChooser);
    }

/**
 *
 */
    private void makeSimpleGeomUI() {
        // a little tricky to get the parent right for GridUI
        viewerWindow = new IndependentWindow("Simple Geometry Viewer", BAMutil.getImage("nj22/NetcdfUI"));

        sgUI = new SimpleGeomUI((PreferencesExt) prefs.node("SimpleGeomUI"), viewerWindow, fileChooser, 800);
        sgUI.addMapBean(new WorldMapBean());
        sgUI.addMapBean(new ShapeFileBean("WorldDetailMap", "Global Detailed Map", "nj22/WorldDetailMap", ToolsUI.WORLD_DETAIL_MAP));
        sgUI.addMapBean(new ShapeFileBean("USDetailMap", "US Detailed Map", "nj22/USMap", ToolsUI.US_MAP));

        viewerWindow.setComponent(sgUI);
        Rectangle bounds = (Rectangle) ToolsUI.getPrefsBean(ToolsUI.GRIDVIEW_FRAME_SIZE,
                                                                new Rectangle(77, 22, 700, 900));
        if (bounds.x < 0) {
            bounds.x = 0;
        }
        if (bounds.y < 0) {
            bounds.x = 0;
        }
        viewerWindow.setBounds(bounds);
    }

/**
 *
 */
    private void makeImageWindow() {
        imageWindow = new IndependentWindow("Simple Geometry Image Viewer", BAMutil.getImage("nj22/NetcdfUI"));
        imageViewer = new ImageViewPanel(null);
        imageWindow.setComponent(imageViewer);
        imageWindow.setBounds((Rectangle) ToolsUI.getPrefsBean(ToolsUI.GRIDIMAGE_FRAME_SIZE,
                                                                new Rectangle(77, 22, 700, 900)));
    }

/** */
    @Override
    public boolean process(Object o) {
        String command = (String) o;
        boolean err = false;

        NetcdfDataset newds;
        try {
            newds = NetcdfDataset.openDataset(command, true, null);
            if (newds == null) {
                JOptionPane.showMessageDialog(null, "NetcdfDataset.open cannot open " + command);
                return false;
            }
            setDataset(newds);

        }
        catch (final FileNotFoundException ioe) {
            JOptionPane.showMessageDialog(null, "NetcdfDataset.open cannot open " + command + "\n" + ioe.getMessage());
            //ioe.printStackTrace();
            err = true;

        }
        catch (final Throwable ioe) {
            ioe.printStackTrace();
            final StringWriter sw = new StringWriter(5000);
            ioe.printStackTrace(new PrintWriter(sw));
            detailTA.setText(sw.toString());
            detailWindow.show();
            err = true;
        }

        return !err;
    }

/** */
    @Override
    public void closeOpenFiles() throws IOException {
        if (ds != null) {
            ds.close();
        }
        ds = null;
        sgTable.clear();
        if (sgUI != null) {
            sgUI.clear();
        }
    }

/**
 *
 */
    public void setDataset(NetcdfDataset newds) {
        if (newds == null) {
            return;
        }
        try {
            if (ds != null) {
                ds.close();
            }
        }
        catch (final IOException ioe) {
            logger.warn("close failed");
        }

        final Formatter parseInfo = new Formatter();
        this.ds = newds;
        try {
            sgTable.setDataset(newds, parseInfo);
        }
        catch (final IOException e) {
            final String info = parseInfo.toString();
            if (info.length() > 0) {
                detailTA.setText(info);
                detailWindow.show();
            }
            e.printStackTrace();
            return;
        }
        setSelectedItem(newds.getLocation());
    }

/**
 *
 */
    public void setDataset(GridDataset gds) {
        if (gds == null) {
            return;
        }
        try {
            if (ds != null) {
                ds.close();
            }
        }
        catch (IOException ioe) {
            logger.warn("close failed");
        }

        this.ds = (NetcdfDataset) gds.getNetcdfFile(); // ??
        try {
            sgTable.setDataset(gds);
        }
        catch (IOException e) {
            e.printStackTrace();
            return;
        }
        setSelectedItem(gds.getLocation());
    }

/** */
    @Override
    public void save() {
        super.save();
        sgTable.save();
        if (sgUI != null) {
            sgUI.storePersistentData();
        }
        if (viewerWindow != null) {
            ToolsUI.putPrefsBeanObject(ToolsUI.GRIDVIEW_FRAME_SIZE, viewerWindow.getBounds());
        }
        if (imageWindow != null) {
            ToolsUI.putPrefsBeanObject(ToolsUI.GRIDIMAGE_FRAME_SIZE, imageWindow.getBounds());
        }
    }
}
