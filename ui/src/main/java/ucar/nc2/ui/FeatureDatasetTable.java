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
import ucar.nc2.dataset.*;
import ucar.nc2.Variable;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NCdumpW;
import ucar.nc2.Structure;
import ucar.nc2.iosp.IOServiceProvider;
import ucar.nc2.ft.FeatureDataset;
import ucar.nc2.ft.FeatureDatasetFactoryManager;
import ucar.nc2.constants.AxisType;
import ucar.nc2.constants._Coordinate;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.dt.grid.GridCoordSys;
import ucar.nc2.dt.radial.RadialCoordSys;
import ucar.nc2.units.DateFormatter;
import ucar.nc2.units.DateUnit;
import ucar.unidata.util.Parameter;
import ucar.ma2.Array;
import ucar.ma2.IndexIterator;

import javax.swing.*;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.ListSelectionEvent;

import thredds.ui.TextHistoryPane;
import thredds.ui.IndependentWindow;
import thredds.ui.BAMutil;

import java.awt.event.ActionEvent;
import java.awt.*;
import java.awt.List;
import java.io.IOException;
import java.io.File;
import java.io.PrintStream;
import java.io.ByteArrayOutputStream;
import java.util.*;

/**
 * @author caron
 * @since Dec 30, 2008
 */
public class FeatureDatasetTable extends JPanel {
  private PreferencesExt prefs;

  private BeanTableSorted ftTable;
  private JSplitPane split;
  private TextHistoryPane infoTA;
  private IndependentWindow infoWindow;

  public FeatureDatasetTable(PreferencesExt prefs) {
    this.prefs = prefs;

    ftTable = new BeanTableSorted(FeatureDatasetBean.class, (PreferencesExt) prefs.node("FeatureDatasetBeans"), false);
    ftTable.addListSelectionListener(new ListSelectionListener() {
      public void valueChanged(ListSelectionEvent e) {
        FeatureDatasetBean ftb = (FeatureDatasetBean) ftTable.getSelectedBean();
        setSelectedFeatureDataset(ftb);
      }
    });

    thredds.ui.PopupMenu varPopup = new thredds.ui.PopupMenu(ftTable.getJTable(), "Options");
    varPopup.addAction("Show Declaration", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        FeatureDatasetBean ftb = (FeatureDatasetBean) ftTable.getSelectedBean();
        infoTA.clear();
        infoTA.appendLine(ftb.toString());
        infoTA.gotoTop();
        infoWindow.showIfNotIconified();
      }
    });

    // the info window
    infoTA = new TextHistoryPane();
    infoWindow = new IndependentWindow("Extra Information", BAMutil.getImage("netcdfUI"), infoTA);
    infoWindow.setBounds((Rectangle) prefs.getBean("InfoWindowBounds", new Rectangle(300, 300, 500, 300)));

    split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, false, ftTable, infoTA);
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
    java.util.List<FeatureDatasetBean> beanList = new ArrayList<FeatureDatasetBean>();

    File top = new File(dirName);
    if (!top.exists()) return false;

    scanDirectory(top, beanList);

    ftTable.setBeans(beanList);
    return true;
  }

  private void scanDirectory(File dir, java.util.List<FeatureDatasetBean> beanList) {

    for (File f : dir.listFiles()) {
      if (f.isDirectory())
        scanDirectory(f, beanList);
      else {
        FeatureDatasetBean fdb = new FeatureDatasetBean(f);
        beanList.add(fdb);
      }
    }

  }

  private void setSelectedFeatureDataset(FeatureDatasetBean ftb) {
    infoTA.setText( ftb.toString());
    infoTA.gotoTop();
  }

  public class FeatureDatasetBean {

    File f;
    String iospName;
    String coordMap;
    String ftype;
    String info;
    String ftImpl;
    Throwable problem;

    // no-arg constructor
    public FeatureDatasetBean() {
    }

    public FeatureDatasetBean(File f) {
      this.f = f;

      NetcdfDataset ds = null;
      try {
        ds = NetcdfDataset.openDataset(f.getPath());
        IOServiceProvider iosp = ds.getIosp();
        iospName = (iosp == null) ? "" : iosp.getClass().getName();
        setCoordMap( ds.getCoordinateSystems());

        Formatter errlog = new Formatter();
        try {
          FeatureDataset featureDataset = FeatureDatasetFactoryManager.wrap( null, ds, null, errlog);
          if (featureDataset != null) {
            ftype = featureDataset.getFeatureType().toString();
            ftImpl = featureDataset.getImplementationName();
            Formatter infof = new Formatter();
            featureDataset.getDetailInfo(infof);
            info = infof.toString();
          }
        } catch (Throwable t) {
          ftype = "ERR: "+t.getMessage();
          info = errlog.toString();
          problem = t;
        }

      } catch (Throwable t) {
        iospName = "ERR: "+t.getMessage();
        problem = t;

      } finally {
        if (ds != null) try { ds.close(); } catch (IOException ioe) {}
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
        else if (csys.getCoordinateAxes().size() > use.getCoordinateAxes().size() )
          use = csys;
      }
      coordMap = (use == null) ? "" : "f:D(" + use.getRankDomain()+")->R(" + use.getRankRange()+")";
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
        problem.printStackTrace( new PrintStream(bout));
        f.format("\n%s", bout.toString());
      }
      return f.toString();
    }

  }


}
