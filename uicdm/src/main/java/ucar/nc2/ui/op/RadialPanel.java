/*
 * Copyright (c) 1998-2019 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.ui.op;

import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.dt.RadialDatasetSweep;
import ucar.nc2.ft.FeatureDatasetFactoryManager;
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
import javax.swing.JOptionPane;
import javax.swing.JSplitPane;

/**
 *
 */
public class RadialPanel extends OpPanel {
  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private RadialDatasetTable dsTable;
  private JSplitPane split;

  private RadialDatasetSweep ds;

  /**
   *
   */
  public RadialPanel(PreferencesExt prefs) {
    super(prefs, "dataset:", true, false);
    dsTable = new RadialDatasetTable(prefs);
    add(dsTable, BorderLayout.CENTER);

    final AbstractButton infoButton = BAMutil.makeButtcon("Information", "Parse Info", false);
    infoButton.addActionListener(e -> {
      try (RadialDatasetSweep radialDataset = dsTable.getRadialDataset()) {
        final Formatter info = new Formatter();
        radialDataset.getDetailInfo(info);
        detailTA.setText(info.toString());
        detailTA.gotoTop();
        detailWindow.show();
      } catch (IOException e1) {
        e1.printStackTrace();
      }
    });
    buttPanel.add(infoButton);
  }

  /**
   *
   */
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

      Formatter errlog = new Formatter();
      RadialDatasetSweep rds = (RadialDatasetSweep) FeatureDatasetFactoryManager.wrap(
          FeatureType.RADIAL, newds, null, errlog);
      if (rds == null) {
        JOptionPane.showMessageDialog(null,
            "FeatureDatasetFactoryManager cannot open " + command + "as RADIAL dataset\n" + errlog
                .toString());
        err = true;
      } else {
        setDataset(rds);
      }
    } catch (FileNotFoundException ioe) {
      JOptionPane.showMessageDialog(null,
          "NetcdfDataset.open cannot open " + command + "\n" + ioe.getMessage());
      ioe.printStackTrace();
      err = true;
    } catch (IOException ioe) {
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
  public void setDataset(RadialDatasetSweep newds) {
    if (newds == null) {
      return;
    }

    try {
      if (ds != null) {
        ds.close();
      }
    } catch (IOException ioe) {
      logger.warn("close failed");
    }

    this.ds = newds;
    dsTable.setDataset(newds);
    setSelectedItem(newds.getLocation());
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
  }

  /**
   *
   */
  @Override
  public void save() {
    super.save();
    dsTable.save();
  }
}
