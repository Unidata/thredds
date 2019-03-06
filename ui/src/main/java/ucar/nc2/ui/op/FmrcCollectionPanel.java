/*
 * Copyright (c) 1998-2019 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.ui.op;

import ucar.nc2.ui.OpPanel;
import ucar.nc2.ui.ToolsUI;
import ucar.nc2.ui.widget.BAMutil;
import ucar.util.prefs.PreferencesExt;

import java.awt.BorderLayout;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Formatter;
import javax.swing.AbstractButton;
import javax.swing.JOptionPane;

/**
 * new Fmrc Collection Metadata storage in bdb
 */
public class FmrcCollectionPanel extends OpPanel {
    private FmrcCollectionTable table;

/**
 *
 */
    public FmrcCollectionPanel(PreferencesExt dbPrefs) {
        super(dbPrefs, "collection:", true, false);
        table = new FmrcCollectionTable(prefs);
        add(table, BorderLayout.CENTER);

        final AbstractButton infoButton = BAMutil.makeButtcon("Information", "Detail Info", false);
        infoButton.addActionListener(e -> {
            Formatter f = new Formatter();
            try {
                table.showInfo(f);
            }
            catch (IOException e1) {
                final StringWriter sw = new StringWriter(5000);
                e1.printStackTrace(new PrintWriter(sw));
                f.format("%s", sw.toString());
            }
            detailTA.setText(f.toString());
            detailTA.gotoTop();
            detailWindow.show();
        });
        buttPanel.add(infoButton);

        final AbstractButton refreshButton = BAMutil.makeButtcon("Undo", "Refresh", false);
        refreshButton.addActionListener(e -> {
            try {
                table.refresh();
            }
            catch (Exception e1) {
                final Formatter f = new Formatter();
                final StringWriter sw = new StringWriter(5000);
                e1.printStackTrace(new PrintWriter(sw));
                f.format("%s", sw.toString());
                detailTA.setText(f.toString());
                detailTA.gotoTop();
                detailWindow.show();
            }
        });
        buttPanel.add(refreshButton);
    }

/** */
    @Override
    public boolean process(Object o) {
        String command = (String) o;
        if (command == null) {
            return false;
        }

        try {
            // table.setCacheRoot(command);
            return true;
        }
        catch (Exception ioe) {
            final StringWriter sw = new StringWriter(5000);
            ioe.printStackTrace(new PrintWriter(sw));
            detailTA.setText(sw.toString());
            detailTA.gotoTop();
            detailWindow.show();
        }

        return false;
    }

/** */
    @Override
    public void closeOpenFiles() {
        // Do nothing.
    }

/** */
    @Override
    public void save() {
        table.save();
        super.save();
    }
  }
