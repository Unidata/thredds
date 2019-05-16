/*
 * Copyright (c) 1998-2019 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.ui.op;

import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.NetcdfDatasetInfo;
import ucar.nc2.ui.OpPanel;
import ucar.ui.widget.BAMutil;
import ucar.util.prefs.PreferencesExt;

import java.awt.BorderLayout;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.invoke.MethodHandles;
import java.util.Formatter;
import javax.swing.AbstractButton;
import javax.swing.JButton;
import javax.swing.JOptionPane;

/**
 *
 */
public class CoordSysPanel extends OpPanel {

    private static final org.slf4j.Logger logger
                = org.slf4j.LoggerFactory.getLogger (MethodHandles.lookup ( ).lookupClass ( ));

    private NetcdfDataset ds;
    private CoordSysTable coordSysTable;

/**
 *
 */
    public CoordSysPanel(PreferencesExt p) {
        super(p, "dataset:", true, false);
        coordSysTable = new CoordSysTable(prefs, buttPanel);
        add(coordSysTable, BorderLayout.CENTER);

        final AbstractButton summaryButton = BAMutil.makeButtcon("Information", "Summary Info", false);
        summaryButton.addActionListener(e -> {
              Formatter f = new Formatter();
              coordSysTable.summaryInfo(f);
              detailTA.setText(f.toString());
              detailWindow.show();
        });
        buttPanel.add(summaryButton);

        final AbstractButton infoButton = BAMutil.makeButtcon("Information", "Parse Info", false);
        infoButton.addActionListener(e -> {
            if (ds != null) {
                try (NetcdfDatasetInfo info = new NetcdfDatasetInfo(ds)) {
                    detailTA.appendLine(info.getParseInfo());
                    detailTA.gotoTop();
                }
                catch (Exception e1) {
                    StringWriter sw = new StringWriter(5000);
                    e1.printStackTrace(new PrintWriter(sw));
                    detailTA.setText(sw.toString());
                }
                detailWindow.show();
            }
        });
        buttPanel.add(infoButton);

        JButton dsButton = new JButton("Object dump");
        dsButton.addActionListener(e -> {
            if (ds != null) {
                StringWriter sw = new StringWriter(5000);
                NetcdfDataset.debugDump(new PrintWriter(sw), ds);
                detailTA.setText(sw.toString());
                detailTA.gotoTop();
                detailWindow.show();
            }
        });
        buttPanel.add(dsButton);
    }

/**
 *
 */
    @Override
    public boolean process(Object o) {
        String command = (String) o;
        boolean err = false;

        // close previous file
        try {
            if (ds != null) {
                ds.close();
            }
        }
        catch (IOException ioe) {
            logger.warn("close failed");
        }

        try {
            ds = NetcdfDataset.openDataset(command, true, -1, null, null);
            if (ds == null) {
                JOptionPane.showMessageDialog(null, "Failed to open <" + command + ">");
            }
            else {
                coordSysTable.setDataset(ds);
            }
        }
        catch (FileNotFoundException ioe) {
            JOptionPane.showMessageDialog(null, "NetcdfDataset cannot open " + command + "\n" + ioe.getMessage());
            err = true;
        }
        catch (Exception e) {
            StringWriter sw = new StringWriter(5000);
            e.printStackTrace(new PrintWriter(sw));
            detailTA.setText(sw.toString());
            detailWindow.show();
            err = true;
            e.printStackTrace();
        }

        return !err;
    }

/**
 *
 */
    @Override
    public void closeOpenFiles() throws IOException {
        if (ds != null) {
            ds.close();
        }
        ds = null;
        coordSysTable.clear();
    }

/**
 *
 */
    public void setDataset(NetcdfDataset ncd) {
        try {
            if (ds != null) ds.close();
            ds = null;
        }
        catch (IOException ioe) {
            logger.warn("close failed");
        }
        ds = ncd;

        coordSysTable.setDataset(ds);
        setSelectedItem(ds.getLocation());
    }

/**
 *
 */
    @Override
    public void save() {
        coordSysTable.save();
        super.save();
    }
}
