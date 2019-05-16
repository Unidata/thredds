/*
 * Copyright (c) 1998-2019 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.ui.op;

import ucar.nc2.ft2.scan.FeatureScan;
import ucar.ui.widget.BAMutil;
import ucar.ui.widget.IndependentWindow;
import ucar.ui.widget.PopupMenu;
import ucar.ui.widget.TextHistoryPane;
import ucar.util.prefs.PreferencesExt;
import ucar.ui.prefs.BeanTable;

import java.awt.event.ActionEvent;
import java.awt.*;
import java.util.*;
import java.util.List;
import javax.swing.*;

/**
 * Scan for Feature Datasets
 * @author caron
 * @since Dec 30, 2008
 */
public class FeatureScanPanel extends JPanel {
  private PreferencesExt prefs;

  private BeanTable ftTable;
  private JSplitPane split;
  private TextHistoryPane dumpTA;
  private IndependentWindow infoWindow;

  public FeatureScanPanel(PreferencesExt prefs) {
    this.prefs = prefs;

    ftTable = new BeanTable(FeatureScan.Bean.class, (PreferencesExt) prefs.node("FeatureDatasetBeans"), false);
    ftTable.addListSelectionListener(e -> {
        FeatureScan.Bean ftb = (FeatureScan.Bean) ftTable.getSelectedBean();
        setSelectedFeatureDataset(ftb);
    });

    PopupMenu varPopup = new PopupMenu(ftTable.getJTable(), "Options");
    varPopup.addAction("Open as NetcdfFile", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        FeatureScan.Bean ftb = (FeatureScan.Bean) ftTable.getSelectedBean();
        if (ftb == null) return;
        FeatureScanPanel.this.firePropertyChange("openNetcdfFile", null, ftb.f.getPath());
      }
    });

    varPopup.addAction("Open in CoordSystems", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        FeatureScan.Bean ftb = (FeatureScan.Bean) ftTable.getSelectedBean();
        if (ftb == null) return;
        FeatureScanPanel.this.firePropertyChange("openCoordSystems", null, ftb.f.getPath());
      }
    });

    varPopup.addAction("Open as PointDataset", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        FeatureScan.Bean ftb = (FeatureScan.Bean) ftTable.getSelectedBean();
        if (ftb == null) return;
        FeatureScanPanel.this.firePropertyChange("openPointFeatureDataset", null, ftb.f.getPath());
      }
    });

    varPopup.addAction("Open as NcML", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        FeatureScan.Bean ftb = (FeatureScan.Bean) ftTable.getSelectedBean();
        if (ftb == null) return;
        FeatureScanPanel.this.firePropertyChange("openNcML", null, ftb.f.getPath());
      }
    });

    varPopup.addAction("Open as GridDataset", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        FeatureScan.Bean ftb = (FeatureScan.Bean) ftTable.getSelectedBean();
        if (ftb == null) return;
        FeatureScanPanel.this.firePropertyChange("openGridDataset", null, ftb.f.getPath());
      }
    });

    varPopup.addAction("Open as CoverageDataset", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        FeatureScan.Bean ftb = (FeatureScan.Bean) ftTable.getSelectedBean();
        if (ftb == null) return;
        FeatureScanPanel.this.firePropertyChange("openCoverageDataset", null, ftb.f.getPath());
      }
    });

    varPopup.addAction("Open as RadialDataset", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        FeatureScan.Bean ftb = (FeatureScan.Bean) ftTable.getSelectedBean();
        if (ftb == null) return;
        FeatureScanPanel.this.firePropertyChange("openRadialDataset", null, ftb.f.getPath());
      }
    });

    varPopup.addAction("Show Report on selected rows", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        List<FeatureScan.Bean> selected = (List<FeatureScan.Bean>) ftTable.getSelectedBeans();
        Formatter f = new Formatter();
        for (FeatureScan.Bean bean : selected) {
          bean.toString(f, false);
        }
        dumpTA.setText(f.toString());          
      }
    });

    varPopup.addAction("Run Coverage Classifier", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        FeatureScan.Bean ftb = (FeatureScan.Bean) ftTable.getSelectedBean();
        if (ftb == null) return;
        dumpTA.setText(ftb.runClassifier());
      }
    });

    // the info window
    TextHistoryPane infoTA = new TextHistoryPane();
    infoWindow = new IndependentWindow("Extra Information", BAMutil.getImage("nj22/NetcdfUI"), infoTA);
    infoWindow.setBounds((Rectangle) prefs.getBean("InfoWindowBounds", new Rectangle(300, 300, 500, 300)));

    dumpTA = new TextHistoryPane();
    split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, false, ftTable, dumpTA);
    split.setDividerLocation(prefs.getInt("splitPos", 500));

    setLayout(new BorderLayout());
    add(split, BorderLayout.CENTER);
  }

  public PreferencesExt getPrefs() {
    return prefs;
  }

  public void save() {
    ftTable.saveState(false);
    prefs.putInt("splitPos", split.getDividerLocation());
    prefs.putBeanObject("InfoWindowBounds", infoWindow.getBounds());
  }

  public void clear() {
    ftTable.setBeans(new ArrayList()); // clear
  }

  public boolean setScanDirectory(String dirName) {
    clear();

    //repaint();
    FeatureScan scanner = new FeatureScan(dirName, true);
    Formatter errlog = new Formatter();
    List<FeatureScan.Bean> beans = scanner.scan(errlog);
    if (beans.size() == 0)  {
      dumpTA.setText(errlog.toString());
      return false;
    }

    ftTable.setBeans(beans);
    //repaint();
    return true;
  }

  private void setSelectedFeatureDataset(FeatureScan.Bean ftb) {
    dumpTA.setText(ftb.toString());
    dumpTA.gotoTop();
  }

}
