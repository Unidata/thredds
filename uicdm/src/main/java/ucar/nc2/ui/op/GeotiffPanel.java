/*
 * Copyright (c) 1998-2019 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.ui.op;

import ucar.ma2.Array;
import ucar.nc2.dt.GridDatatype;
import ucar.nc2.dt.grid.GridDataset;
import ucar.nc2.geotiff.GeoTiff;
import ucar.nc2.geotiff.GeotiffWriter;
import ucar.nc2.ui.OpPanel;
import ucar.ui.widget.TextHistoryPane;
import ucar.util.prefs.PreferencesExt;

import java.awt.BorderLayout;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JOptionPane;

public class GeotiffPanel extends OpPanel {
  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private TextHistoryPane ta;

  public GeotiffPanel(PreferencesExt p) {
    super(p, "netcdf:", true, false);

    ta = new TextHistoryPane(true);
    add(ta, BorderLayout.CENTER);

    final JButton readButton = new JButton("read geotiff");
    readButton.addActionListener(e -> {
      if (cb.getSelectedItem() != null) {
        final String item = cb.getSelectedItem().toString();
        final String fname = item.trim();
        read(fname);
      }
    });
    buttPanel.add(readButton);
  }

  @Override
  public boolean process(Object o) {
    String filename = (String) o;

    try (GridDataset gridDs = GridDataset.open(filename)) {
      final List grids = gridDs.getGrids();
      if (grids.size() == 0) {
        logger.warn("No grids found.");
        return false;
      }

      final GridDatatype grid = (GridDatatype) grids.get(0);
      final Array data = grid.readDataSlice(0, 0, -1, -1); // first time, level

      final String fileOut = fileChooser.chooseFilenameToSave(filename + ".tif");
      if (fileOut == null) {
        return false;
      }

      try (GeotiffWriter writer = new GeotiffWriter(fileOut)) {
        writer.writeGrid(gridDs, grid, data, false);
        read(fileOut);
        JOptionPane.showMessageDialog(null, "File written to " + fileOut);
      }
    } catch (IOException ioe) {
      ioe.printStackTrace();
      return false;
    }
    return true;
  }

  public void read(String filename) {
    try (GeoTiff geotiff = new GeoTiff(filename)) {
      geotiff.read();
      ta.setText(geotiff.showInfo());
    } catch (IOException ioe) {
      ioe.printStackTrace();
    }
  }

  @Override
  public void closeOpenFiles() throws IOException {
    // Do nothing
  }
}
