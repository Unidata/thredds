/*
 * Copyright (c) 1998-2019 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.ui.op;

import ucar.nc2.NetcdfFile;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dt.GridDataset;
import ucar.nc2.ui.OpPanel;
import ucar.nc2.ui.ToolsUI;
import ucar.nc2.ui.widget.BAMutil;
import ucar.nc2.ui.widget.FileManager;
import ucar.util.prefs.PreferencesExt;

import java.awt.BorderLayout;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.invoke.MethodHandles;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Formatter;
import javax.swing.AbstractButton;

/**
 *
 */
public class FmrcPanel extends OpPanel {
    private Fmrc2Panel table;

/**
 *
 */
    public FmrcPanel(PreferencesExt dbPrefs) {
       super(dbPrefs, "collection:", true, false);

       table = new Fmrc2Panel(prefs);
       table.addPropertyChangeListener(new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent e) {

                if (e.getPropertyName().equals("openNetcdfFile")) {
                    if (e.getNewValue() instanceof String) {
                        ToolsUI.getToolsUI().openNetcdfFile((String) e.getNewValue());
                    }
                    else {
                        ToolsUI.getToolsUI().openNetcdfFile((NetcdfFile) e.getNewValue());
                    }
                }
                else if (e.getPropertyName().equals("openCoordSys")) {
                    if (e.getNewValue() instanceof String) {
                        ToolsUI.getToolsUI().openCoordSystems((String) e.getNewValue());
                    }
                    else {
                        ToolsUI.getToolsUI().openCoordSystems((NetcdfDataset) e.getNewValue());
                    }
                }
                else if (e.getPropertyName().equals("openGridDataset")) {
                    if (e.getNewValue() instanceof String) {
                        ToolsUI.getToolsUI().openGridDataset((String) e.getNewValue());
                    }
                    else {
                        ToolsUI.getToolsUI().openGridDataset((GridDataset) e.getNewValue());
                    }
                }
            }
        });
        add(table, BorderLayout.CENTER);

        final AbstractButton infoButton = BAMutil.makeButtcon("Information", "Detail Info", false);
        infoButton.addActionListener(e -> {
            final Formatter f = new Formatter();
            try {
                table.showInfo(f);
            }
            catch (final IOException e1) {
                final StringWriter sw = new StringWriter(5000);
                e1.printStackTrace(new PrintWriter(sw));
                f.format("%s", sw.toString());
            }
            detailTA.setText(f.toString());
            detailTA.gotoTop();
            detailWindow.show();
        });
        buttPanel.add(infoButton);

        final AbstractButton collectionButton = BAMutil.makeButtcon("Information", "Collection Parsing Info", false);
        collectionButton.addActionListener(e -> {
            table.showCollectionInfo(true);
        });
        buttPanel.add(collectionButton);

        final AbstractButton viewButton = BAMutil.makeButtcon("Dump", "Show Dataset", false);
        viewButton.addActionListener(e -> {
            try {
                table.showDataset();
            }
            catch (final IOException e1) {
                final StringWriter sw = new StringWriter(5000);
                e1.printStackTrace(new PrintWriter(sw));
                detailTA.setText(sw.toString());
                detailTA.gotoTop();
                detailWindow.show();
            }
        });
        buttPanel.add(viewButton);
    }

/** */
    @Override
    public boolean process(Object o) {
        String command = (String) o;
        if (command == null) {
            return false;
        }

          /* if (fmrc != null) {
            try {
              fmrc.close();
            } catch (IOException ioe) {
            }
          } */

        try {
            table.setFmrc(command);
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
    public void save() {
        table.save();
        super.save();
    }

/** */
    @Override
    public void closeOpenFiles() throws IOException {
        table.closeOpenFiles();
    }
  }
