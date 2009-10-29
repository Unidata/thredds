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

import ucar.util.prefs.PreferencesExt;
import ucar.util.prefs.ui.BeanTableSorted;
import ucar.nc2.ft.scan.FeatureScan;

import javax.swing.*;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.ListSelectionEvent;

import thredds.ui.TextHistoryPane;
import thredds.ui.IndependentWindow;
import thredds.ui.BAMutil;

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

  private BeanTableSorted ftTable;
  private JSplitPane split;
  private TextHistoryPane infoTA, dumpTA;
  private IndependentWindow infoWindow;

  public FeatureScanPanel(PreferencesExt prefs) {
    this.prefs = prefs;

    ftTable = new BeanTableSorted(FeatureScan.Bean.class, (PreferencesExt) prefs.node("FeatureDatasetBeans"), false);
    ftTable.addListSelectionListener(new ListSelectionListener() {
      public void valueChanged(ListSelectionEvent e) {
        FeatureScan.Bean ftb = (FeatureScan.Bean) ftTable.getSelectedBean();
        setSelectedFeatureDataset(ftb);
      }
    });

    thredds.ui.PopupMenu varPopup = new thredds.ui.PopupMenu(ftTable.getJTable(), "Options");
    varPopup.addAction("Open as NetcdfFile", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        FeatureScan.Bean ftb = (FeatureScan.Bean) ftTable.getSelectedBean();
        if (ftb == null) return;
        FeatureScanPanel.this.firePropertyChange("openNetcdfFile", null, ftb.f.getPath());
      }
    });

    varPopup.addAction("Check CoordSystems", new AbstractAction() {
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

    varPopup.addAction("Open as RadialDataset", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        FeatureScan.Bean ftb = (FeatureScan.Bean) ftTable.getSelectedBean();
        if (ftb == null) return;
        FeatureScanPanel.this.firePropertyChange("openRadialDataset", null, ftb.f.getPath());
      }
    });

    // the info window
    infoTA = new TextHistoryPane();
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

  public boolean setScanDirectory(String dirName) {
    FeatureScan scanner = new FeatureScan(dirName, true);
    List<FeatureScan.Bean> beans = scanner.scan(new Formatter());
    ftTable.setBeans(beans);
    return true;
  }



  private void setSelectedFeatureDataset(FeatureScan.Bean ftb) {
    dumpTA.setText(ftb.toString());
    dumpTA.gotoTop();
  }

  /*

    private void scanDirectory(File dir, java.util.List<FeatureScan.Bean> beanList) {

    for (File f : dir.listFiles()) {
      if (f.isDirectory())
        scanDirectory(f, beanList);
      else {
        FeatureScan.Bean fdb = new FeatureScan.Bean(f);
        beanList.add(fdb);
      }
    }

  }
  public class FeatureScan.Bean {

    File f;
    String iospName;
    String coordMap;
    FeatureType featureType;
    String ftype;
    String info;
    String ftImpl;
    Throwable problem;

    // no-arg constructor
    public FeatureScan.Bean() {
    }

    public FeatureScan.Bean(File f) {
      this.f = f;

      NetcdfDataset ds = null;
      try {
        ds = NetcdfDataset.openDataset(f.getPath());
        IOServiceProvider iosp = ds.getIosp();
        iospName = (iosp == null) ? "" : iosp.getClass().getName();
        setCoordMap(ds.getCoordinateSystems());

        Formatter errlog = new Formatter();
        try {
          FeatureDataset featureDataset = FeatureDatasetFactoryManager.wrap(null, ds, null, errlog);
          if (featureDataset != null) {
            featureType = featureDataset.getFeatureType();
            if (featureType != null)
              ftype = featureType.toString();
            ftImpl = featureDataset.getImplementationName();
            Formatter infof = new Formatter();
            featureDataset.getDetailInfo(infof);
            info = infof.toString();
          } else {
            ftype = "FAIL: " + errlog.toString();
          }
        } catch (Throwable t) {
          ftype = "ERR: " + t.getMessage();
          info = errlog.toString();
          problem = t;
        }

      } catch (Throwable t) {
        iospName = "ERR: " + t.getMessage();
        problem = t;

      } finally {
        if (ds != null) try {
          ds.close();
        } catch (IOException ioe) {
        }
      }
    }


    public String getName() {
      return f.getPath();
    }

    public String getIosp() {
      return iospName;
    }

    public String getCoordMap() {
      return coordMap;
    }

    public void setCoordMap(java.util.List<CoordinateSystem> csysList) {
      CoordinateSystem use = null;
      for (CoordinateSystem csys : csysList) {
        if (use == null) use = csys;
        else if (csys.getCoordinateAxes().size() > use.getCoordinateAxes().size())
          use = csys;
      }
      coordMap = (use == null) ? "" : "f:D(" + use.getRankDomain() + ")->R(" + use.getRankRange() + ")";
    }

    public String getFeatureType() {
      return ftype;
    }

    public String getFeatureImpl() {
      return ftImpl;
    }

    public String toString() {
      Formatter f = new Formatter();
      f.format("%s%n %s%n map = '%s'%n %s%n %s%n", getName(), getIosp(), getCoordMap(), getFeatureType(), getFeatureImpl());
      if (info != null) {
        f.format("\n%s", info);
      }
      if (problem != null) {
        ByteArrayOutputStream bout = new ByteArrayOutputStream(10000);
        problem.printStackTrace(new PrintStream(bout));
        f.format("\n%s", bout.toString());
      }
      return f.toString();
    }

  }  */


}
