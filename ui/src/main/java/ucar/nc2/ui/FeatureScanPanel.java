/*
 * Copyright 1998-2009 University Corporation for Atmospheric Research/Unidata
 *
 * Portions of this software were developed by the Unidata Program at the
 * University Corporation for Atmospheric Research.
 *
 * Access and use of this software shall impose the following obligations
 * and understandings on the user. The user is granted the right, without
 * any fee or cost, to use, copy, modify, alter, enhance and distribute
 * this software, and any derivative works thereof, and its supporting
 * documentation for any purpose whatsoever, provided that this entire
 * notice appears in all copies of the software, derivative works and
 * supporting documentation.  Further, UCAR requests that the user credit
 * UCAR/Unidata in any publications that result from the use of this
 * software or in any product that includes this software. The names UCAR
 * and/or Unidata, however, may not be used in any advertising or publicity
 * to endorse or promote any products or commercial entity unless specific
 * written permission is obtained from UCAR/Unidata. The user also
 * understands that UCAR/Unidata is not obligated to provide the user with
 * any support, consulting, training or assistance of any kind with regard
 * to the use, operation and performance of this software nor to provide
 * the user with any updates, revisions, new versions or "bug fixes."
 *
 * THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 * INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 * FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 * NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 * WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */
package ucar.nc2.ui;

import ucar.nc2.ui.widget.PopupMenu;
import ucar.util.prefs.PreferencesExt;
import ucar.util.prefs.ui.BeanTable;
import ucar.nc2.ft.scan.FeatureScan;

import javax.swing.*;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.ListSelectionEvent;

import ucar.nc2.ui.widget.TextHistoryPane;
import ucar.nc2.ui.widget.IndependentWindow;
import ucar.nc2.ui.widget.BAMutil;

import java.awt.event.ActionEvent;
import java.awt.*;
import java.util.*;
import java.util.List;

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
    ftTable.addListSelectionListener(new ListSelectionListener() {
      public void valueChanged(ListSelectionEvent e) {
        FeatureScan.Bean ftb = (FeatureScan.Bean) ftTable.getSelectedBean();
        setSelectedFeatureDataset(ftb);
      }
    });

    PopupMenu varPopup = new ucar.nc2.ui.widget.PopupMenu(ftTable.getJTable(), "Options");
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
    infoWindow = new IndependentWindow("Extra Information", BAMutil.getImage("netcdfUI"), infoTA);
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
