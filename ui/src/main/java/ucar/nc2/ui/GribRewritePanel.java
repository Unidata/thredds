/*
 * Copyright 1998-2014 University Corporation for Atmospheric Research/Unidata
 *
 *   Portions of this software were developed by the Unidata Program at the
 *   University Corporation for Atmospheric Research.
 *
 *   Access and use of this software shall impose the following obligations
 *   and understandings on the user. The user is granted the right, without
 *   any fee or cost, to use, copy, modify, alter, enhance and distribute
 *   this software, and any derivative works thereof, and its supporting
 *   documentation for any purpose whatsoever, provided that this entire
 *   notice appears in all copies of the software, derivative works and
 *   supporting documentation.  Further, UCAR requests that the user credit
 *   UCAR/Unidata in any publications that result from the use of this
 *   software or in any product that includes this software. The names UCAR
 *   and/or Unidata, however, may not be used in any advertising or publicity
 *   to endorse or promote any products or commercial entity unless specific
 *   written permission is obtained from UCAR/Unidata. The user also
 *   understands that UCAR/Unidata is not obligated to provide the user with
 *   any support, consulting, training or assistance of any kind with regard
 *   to the use, operation and performance of this software nor to provide
 *   the user with any updates, revisions, new versions or "bug fixes."
 *
 *   THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 *   IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *   WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *   DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 *   INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 *   FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 *   NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 *   WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */

package ucar.nc2.ui;

import ucar.nc2.constants.FeatureType;
import ucar.nc2.constants._Coordinate;
import ucar.nc2.dataset.CoordinateSystem;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.ft.FeatureDataset;
import ucar.nc2.ft.FeatureDatasetFactoryManager;
import ucar.nc2.ft.grid.impl.CoverageCSFactory;
import ucar.nc2.ui.widget.BAMutil;
import ucar.nc2.ui.widget.IndependentWindow;
import ucar.nc2.ui.widget.PopupMenu;
import ucar.nc2.ui.widget.TextHistoryPane;
import ucar.util.prefs.PreferencesExt;
import ucar.util.prefs.ui.BeanTable;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Formatter;
import java.util.List;

/**
 * Describe
 *
 * @author caron
 * @since 8/11/2014
 */
public class GribRewritePanel extends JPanel {
  private PreferencesExt prefs;

  private BeanTable ftTable;
  private JSplitPane split;
  private TextHistoryPane dumpTA;
  private IndependentWindow infoWindow;

  public GribRewritePanel(PreferencesExt prefs) {
    this.prefs = prefs;

    ftTable = new BeanTable(FileBean.class, (PreferencesExt) prefs.node("FeatureDatasetBeans"), false);
    ftTable.addListSelectionListener(new ListSelectionListener() {
      public void valueChanged(ListSelectionEvent e) {
        FileBean ftb = (FileBean) ftTable.getSelectedBean();
        setSelectedFeatureDataset(ftb);
      }
    });

    PopupMenu varPopup = new ucar.nc2.ui.widget.PopupMenu(ftTable.getJTable(), "Options");
    varPopup.addAction("Open as NetcdfFile", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        FileBean ftb = (FileBean) ftTable.getSelectedBean();
        if (ftb == null) return;
        GribRewritePanel.this.firePropertyChange("openNetcdfFile", null, ftb.f.getPath());
      }
    });
    
    varPopup.addAction("Open as GridDataset", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        FileBean ftb = (FileBean) ftTable.getSelectedBean();
        if (ftb == null) return;
        GribRewritePanel.this.firePropertyChange("openGridDataset", null, ftb.f.getPath());
      }
    });

    varPopup.addAction("Show Report on selected rows", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        List<FileBean> selected = ftTable.getSelectedBeans();
        Formatter f = new Formatter();
        for (FileBean bean : selected) {
          bean.toString(f, false);
        }
        dumpTA.setText(f.toString());
      }
    });

    varPopup.addAction("Run Coverage Classifier", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        FileBean ftb = (FileBean) ftTable.getSelectedBean();
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
    Formatter errlog = new Formatter();
    List<FileBean> beans = scan(errlog);
    if (beans.size() == 0)  {
      dumpTA.setText(errlog.toString());
      return false;
    }

    ftTable.setBeans(beans);
    //repaint();
    return true;
  }

  private void setSelectedFeatureDataset(FileBean ftb) {
    dumpTA.setText(ftb.toString());
    dumpTA.gotoTop();
  }
  
  ///////////////
  
  public java.util.List<FileBean> scan(Formatter errlog) {

    List<FileBean> result = new ArrayList<>();

    File topFile = new File(top);
    if (!topFile.exists()) {
      errlog.format("File %s does not exist", top);
      return result;
    }

    if (topFile.isDirectory())
      scanDirectory(topFile, result, errlog);
    else {
      FileBean fdb = new FileBean(topFile);
      result.add(fdb);
    }

    return result;
  }

  private void scanDirectory(File dir, java.util.List<FileBean> result, Formatter errlog) {
    if ((dir.getName().equals("exclude")) || (dir.getName().equals("problem")))return;

    // get list of files
    List<File> files = new ArrayList<>();
    for (File f : dir.listFiles()) {
      if (!f.isDirectory()) {
        files.add(f);
      }
    }

    // eliminate redundant files
    // ".Z", ".zip", ".gzip", ".gz", or ".bz2"
    if (files.size() > 0) {
      Collections.sort(files);
      ArrayList<File> files2 = new ArrayList<File>(files);

      File prev = null;
      for (File f : files) {
        String name = f.getName();
        String stem = stem(name);
        if (name.endsWith(".gbx") || name.endsWith(".gbx8") || name.endsWith(".pdf") || name.endsWith(".xml") || name.endsWith(".gbx9")
                || name.endsWith(".ncx") || name.endsWith(".txt") || name.endsWith(".tar")) {
          files2.remove(f);

        } else if (prev != null) {

          if (name.endsWith(".ncml")) {
            if (prev.getName().equals(stem) || prev.getName().equals(stem + ".nc"))
               files2.remove(prev);
          } else if (name.endsWith(".bz2")) {
            if (prev.getName().equals(stem)) files2.remove(f);
          } else if (name.endsWith(".gz")) {
            if (prev.getName().equals(stem)) files2.remove(f);
          } else if (name.endsWith(".gzip")) {
            if (prev.getName().equals(stem)) files2.remove(f);
          } else if (name.endsWith(".zip")) {
            if (prev.getName().equals(stem)) files2.remove(f);
          } else if (name.endsWith(".Z")) {
            if (prev.getName().equals(stem)) files2.remove(f);
          }
        }
        prev = f;
      }

      // do the remaining
      for (File f : files2) {
        result.add(new FileBean(f));
      }
    }

    // do subdirs
    if (subdirs) {
      for (File f : dir.listFiles()) {
        if (f.isDirectory() && !f.getName().equals("exclude"))
          scanDirectory(f, result, errlog);
      }
    }

  }

  private String stem(String name) {
    int pos = name.lastIndexOf('.');
    return (pos > 0) ? name.substring(0, pos) : name;
  }

  private static final boolean debug = true;

  private class FileBean {
    public File f;
    String fileType;
    String coordMap;
    FeatureType featureType, ftFromMetadata;
    String ftype;
    StringBuilder info = new StringBuilder();
    String coordSysBuilder;
    String ftImpl;
    Throwable problem;
    String isCoverage;

    // no-arg constructor
    public FileBean() {
    }

    public FileBean(File f) {
      this.f = f;

      NetcdfDataset ds = null;
      try {
        if (debug) System.out.printf(" featureScan=%s%n", f.getPath());
        ds = NetcdfDataset.openDataset(f.getPath());
        fileType = ds.getFileTypeId();
        setCoordMap(ds.getCoordinateSystems());
        coordSysBuilder = ds.findAttValueIgnoreCase(null, _Coordinate._CoordSysBuilder, "none");

        Formatter errlog = new Formatter();
        isCoverage = CoverageCSFactory.describe(errlog, ds);
        info.append(errlog.toString());

        ftFromMetadata = FeatureDatasetFactoryManager.findFeatureType(ds);

        try {
          errlog = new Formatter();
          FeatureDataset featureDataset = FeatureDatasetFactoryManager.wrap(null, ds, null, errlog);
          info.append("FeatureDatasetFactoryManager errlog = ");
          info.append(errlog.toString());
          info.append("\n\n");

          if (featureDataset != null) {
            featureType = featureDataset.getFeatureType();
            if (featureType != null)
              ftype = featureType.toString();
            ftImpl = featureDataset.getImplementationName();
            Formatter infof = new Formatter();
            featureDataset.getDetailInfo(infof);
            info.append(infof.toString());
          } else {
            ftype = "";
          }

        } catch (Throwable t) {
          ftype = " ERR: " + t.getMessage();
          info.append(errlog.toString());
          problem = t;
        }

      } catch (Throwable t) {
        fileType = " ERR: " + t.getMessage();
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

    public String getFileType() {
      return fileType;
    }

    public String getSizeK() {
      Formatter fm = new Formatter();
      //long size = f.length();
      //if (size > 10 * 1000 * 1000) fm.format("%6.1f M", ((float) size) / 1000 / 1000);
      //else if (size > 10 * 1000) fm.format("%6.1f K", ((float) size) / 1000);
      //else fm.format("%d", size);
      fm.format("%,10d", f.length() / 1000);
      return fm.toString();
    }

    public String getCoordMap() {
      return coordMap;
    }

    public String getCoordSysBuilder() {
      return coordSysBuilder;
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

    public String getFtMetadata() {
      return (ftFromMetadata == null) ? "" : ftFromMetadata.toString();
    }

    public String getFeatureImpl() {
      return ftImpl;
    }

    public String getCoverage() {
      return isCoverage == null ? "" : isCoverage;
    }

    public void toString(Formatter f, boolean showInfo) {
      f.format("%s%n %s%n map = '%s'%n %s%n %s%n", getName(), getFileType(), getCoordMap(), getFeatureType(), getFeatureImpl());
      if (showInfo && info != null) {
        f.format("%n%s", info);
      }
      if (problem != null) {
        ByteArrayOutputStream bout = new ByteArrayOutputStream(10000);
        problem.printStackTrace(new PrintStream(bout));
        f.format("%n%s", bout.toString());
      }
    }

    public String toString() {
      Formatter f = new Formatter();
      toString(f, true);
      return f.toString();
    }

    public String runClassifier() {
      Formatter ff = new Formatter();
      String type = null;
      try (NetcdfDataset ds = NetcdfDataset.openDataset(f.getPath())) {
        type = CoverageCSFactory.describe(ff, ds);

      } catch (IOException e) {
        ByteArrayOutputStream bout = new ByteArrayOutputStream(10000);
        problem.printStackTrace(new PrintStream(bout));
        ff.format("%n%s", bout.toString());

      }
      ff.format("CoverageCS.Type = %s", type);
      return ff.toString();
    }

  }
}
