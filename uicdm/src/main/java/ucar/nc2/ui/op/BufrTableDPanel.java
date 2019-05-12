/*
 * Copyright (c) 1998-2019 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.ui.op;

import ucar.nc2.iosp.bufr.tables.BufrTables;
import ucar.nc2.ui.OpPanel;
import ucar.nc2.ui.ToolsUI;
import ucar.ui.widget.BAMutil;
import ucar.ui.widget.FileManager;
import ucar.util.prefs.PreferencesExt;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.StringWriter;
import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JOptionPane;

/**
 *
 */
public class BufrTableDPanel extends OpPanel {
    private BufrTableDViewer bufrTable;
    private JComboBox<BufrTables.Format> modes;
    private JComboBox<BufrTables.TableConfig> tables;

/**
 *
 */
    public BufrTableDPanel(PreferencesExt p) {
      super(p, "tableD:", false, false);

      final AbstractAction fileAction = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final FileManager bufrFileChooser = ToolsUI.getBufrFileChooser();

                final String filename = bufrFileChooser.chooseFilename();
                if (filename == null) {
                    return;
                }
                cb.setSelectedItem(filename);
            }
        };
        BAMutil.setActionProperties(fileAction, "FileChooser", "open Local table...", false, 'L', -1);
        BAMutil.addActionToContainer(buttPanel, fileAction);

        modes = new JComboBox<>(BufrTables.Format.values());
        buttPanel.add(modes);

        final JButton acceptButton = new JButton("Accept");
        buttPanel.add(acceptButton);
        acceptButton.addActionListener(e -> accept());

        tables = new JComboBox<>(BufrTables.getTableConfigsAsArray());
        buttPanel.add(tables);
        tables.addActionListener(e -> acceptTable((BufrTables.TableConfig) tables.getSelectedItem()));

        bufrTable = new BufrTableDViewer(prefs, buttPanel);
        add(bufrTable, BorderLayout.CENTER);
    }

/** */
    @Override
    public boolean process(final Object command) {
      return true;
    }

/** */
    @Override
    public void closeOpenFiles() {
    }

/**
 *
 */
    private void accept() {
        final String command = (String) cb.getSelectedItem();
        if (command == null) {
            return;
        }

        try {
            Object mode = modes.getSelectedItem();
            bufrTable.setBufrTableD(command, (BufrTables.Format) mode);
        }
        catch (FileNotFoundException ioe) {
            JOptionPane.showMessageDialog(null, "BufrTableViewer cannot open " + command + "\n" + ioe.getMessage());
            detailTA.setText("Failed to open <" + command + ">\n" + ioe.getMessage());
            detailTA.setVisible(true);
        }
        catch (Exception e) {
            e.printStackTrace();
            final StringWriter sw = new StringWriter(5000);
            e.printStackTrace(new PrintWriter(sw));
            detailTA.setText(sw.toString());
            detailTA.setVisible(true);
        }
    }

/**
 *
 */
    private void acceptTable(BufrTables.TableConfig tc) {
        try {
            bufrTable.setBufrTableD(tc.getTableDname(), tc.getTableDformat());
        }
        catch (FileNotFoundException ioe) {
            JOptionPane.showMessageDialog(null, "BufrTableViewer cannot open " + tc + "\n" + ioe.getMessage());
            detailTA.setText("Failed to open <" + tc + ">\n" + ioe.getMessage());
            detailTA.setVisible(true);
        }
        catch (Exception e) {
            e.printStackTrace();
            final StringWriter sw = new StringWriter(5000);
            e.printStackTrace(new PrintWriter(sw));
            detailTA.setText(sw.toString());
            detailTA.setVisible(true);
        }
    }

/** */
    @Override
    public void save() {
        bufrTable.save();
        super.save();
    }
}
