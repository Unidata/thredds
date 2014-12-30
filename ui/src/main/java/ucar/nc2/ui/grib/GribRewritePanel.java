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

package ucar.nc2.ui.grib;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import thredds.featurecollection.FeatureCollectionConfig;
import thredds.inventory.CollectionAbstract;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dt.GridDatatype;
import ucar.nc2.dt.grid.GridDataset;
import ucar.nc2.grib.collection.GribCdmIndex;
import ucar.nc2.grib.collection.GribCollectionImmutable;
import ucar.nc2.ui.widget.BAMutil;
import ucar.nc2.ui.widget.IndependentWindow;
import ucar.nc2.ui.widget.PopupMenu;
import ucar.nc2.ui.widget.TextHistoryPane;
import ucar.unidata.util.StringUtil2;
import ucar.util.prefs.PreferencesExt;
import ucar.util.prefs.ui.BeanTable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Formatter;
import java.util.List;

/**
 * Rewrite GRIB files - track stats etc
 *
 * @author caron
 * @since 8/11/2014
 */
public class GribRewritePanel extends JPanel {
  static private final Logger logger = LoggerFactory.getLogger(GribRewritePanel.class);

  private PreferencesExt prefs;

  private BeanTable ftTable;
  private JSplitPane split;
  private TextHistoryPane dumpTA;
  private IndependentWindow infoWindow;

  public GribRewritePanel(PreferencesExt prefs, JPanel buttPanel) {
    this.prefs = prefs;

    ftTable = new BeanTable(FileBean.class, (PreferencesExt) prefs.node("FeatureDatasetBeans"), false);
    /* ftTable.addListSelectionListener(new ListSelectionListener() {
      public void valueChanged(ListSelectionEvent e) {
        FileBean ftb = (FileBean) ftTable.getSelectedBean();
        setSelectedFeatureDataset(ftb);
      }
    }); */

    AbstractAction calcAction = new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        calcAverage();
      }
    };
    BAMutil.setActionProperties(calcAction, "Dataset", "calc storage", false, 'C', -1);
    BAMutil.addActionToContainer(buttPanel, calcAction);

    PopupMenu varPopup = new ucar.nc2.ui.widget.PopupMenu(ftTable.getJTable(), "Options");
    varPopup.addAction("Open as NetcdfFile", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        FileBean ftb = (FileBean) ftTable.getSelectedBean();
        if (ftb == null) return;
        GribRewritePanel.this.firePropertyChange("openNetcdfFile", null, ftb.getPath());
      }
    });
    
    varPopup.addAction("Open as GridDataset", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        FileBean ftb = (FileBean) ftTable.getSelectedBean();
        if (ftb == null) return;
        GribRewritePanel.this.firePropertyChange("openGridDataset", null, ftb.getPath());
      }
    });

    varPopup.addAction("Open in Grib2Data", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        FileBean ftb = (FileBean) ftTable.getSelectedBean();
        if (ftb == null) return;
        GribRewritePanel.this.firePropertyChange("openGrib2Data", null, ftb.getPath());
      }
    });

    varPopup.addAction("Open in Grib1Data", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        FileBean ftb = (FileBean) ftTable.getSelectedBean();
        if (ftb == null) return;
        GribRewritePanel.this.firePropertyChange("openGrib1Data", null, ftb.getPath());
      }
    });

    /* varPopup.addAction("Show Report on selected rows", new AbstractAction() {
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
    });  */

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
    List<FileBean> beans = scan(dirName, errlog);
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

  private void calcAverage() {
    Formatter f = new Formatter();
    calcAverage(null, f);
    calcAverage("grib1", f);
    calcAverage("grib2", f);

    dumpTA.setText(f.toString());
    dumpTA.gotoTop();
  }
  
  private void calcAverage(String what, Formatter f) {

    double totalRecords = 0.0;
    double ratio = 0.0;
    List<FileBean> beans = ftTable.getBeans();
    for (FileBean bean : beans) {
      if (bean.getNdups() > 0) continue;
      if (bean.getRatio() == 0) continue;
      if (what != null && (!bean.getPath().contains(what))) continue;
      totalRecords += bean.getNrecords();
      ratio += bean.getNrecords() * bean.getRatio();
    }

    double weightedAvg =  ratio / totalRecords;
    if (what != null) f.format("%n%s%n", what);
    f.format("Weighted average ratio = %f%n", weightedAvg);
    f.format("Total # grib records = %f%n", totalRecords);
  }

  ///////////////
  
  public java.util.List<FileBean> scan(String top, Formatter errlog) {

    List<FileBean> result = new ArrayList<>();

    File topFile = new File(top);
    if (!topFile.exists()) {
      errlog.format("File %s does not exist", top);
      return result;
    }

    if (topFile.isDirectory())
      scanDirectory(topFile, false, result, errlog);
    else {
      FileBean fdb = null;
      try {
        fdb = new FileBean(topFile);
      } catch (IOException e) {
        System.out.printf("FAIL, skip %s%n", topFile.getPath());
      }
      result.add(fdb);
    }

    return result;
  }

  public static class FileFilterFromSuffixes implements FileFilter {
    String[] suffixes;
    public FileFilterFromSuffixes(String suffixes) {
      this.suffixes = suffixes.split(" ");
    }

    @Override
    public boolean accept(File file) {
      for (String s: suffixes)
        if (file.getPath().endsWith(s)) return true;
      return false;
    }
  }


  private void scanDirectory(File dir, boolean subdirs, java.util.List<FileBean> result, Formatter errlog) {
    if ((dir.getName().equals("exclude")) || (dir.getName().equals("problem")))return;

    File[] gribFilesInDir = dir.listFiles(new FileFilterFromSuffixes("grib1 grib2"));
    if (gribFilesInDir == null) {
      // File.listFiles() returns null instead of throwing an exception. Dumb.
      throw new RuntimeException(String.format("Either an I/O error occurred, or \"%s\" is not a directory.", dir));
    }

    List<File> files = new ArrayList<>();
    for (File gribFile : gribFilesInDir) {
      if (!gribFile.isDirectory()) {
        files.add(gribFile);
      }
    }

    // eliminate redundant files
    // ".Z", ".zip", ".gzip", ".gz", or ".bz2"
    if (files.size() > 0) {
      Collections.sort(files);
      List<File> files2 = new ArrayList<>(files);

      File prev = null;
      for (File f : files) {
        String name = f.getName();
        String stem = stem(name);
        if (prev != null) {
          if (name.endsWith(".ncml")) {
            if (prev.getName().equals(stem) || prev.getName().equals(stem + ".grib2"))
               files2.remove(prev);
          }
        }
        prev = f;
      }

      // do the remaining
      for (File f : files2) {
        try {
          result.add(new FileBean(f));
        } catch (IOException e) {
          System.out.printf("FAIL, skip %s%n", f.getPath());
        }
      }
    }

    // do subdirs
    if (subdirs) {
      for (File f : dir.listFiles()) {
        if (f.isDirectory() && !f.getName().equals("exclude"))
          scanDirectory(f, subdirs, result, errlog);
      }
    }

  }

  private String stem(String name) {
    int pos = name.lastIndexOf('.');
    return (pos > 0) ? name.substring(0, pos) : name;
  }

  private static final boolean debug = true;

  public class FileBean {
    private File f;
    String fileType;
    long cdmData2D, nc4Data2D, nc4Size;
    int nrecords, nvars, ndups;

    // no-arg constructor
    public FileBean() {
    }

    public FileBean(File f) throws IOException {
      this.f = f;

      if (debug) System.out.printf(" fileScan=%s%n", getPath());
      try (NetcdfDataset ncd = NetcdfDataset.openDataset(getPath())) {
        fileType = ncd.getFileTypeId();
        cdmData2D = countCdmData2D(ncd);
        countGribData2D(f);
        countNc4Data2D(f);
      }
    }

    public String getPath() {
      String p = f.getPath();
      return StringUtil2.replace(p, "\\", "/");
    }

    public String getFileType() {
      return fileType;
    }

    public double getGribSizeM() {
      return ((double) f.length()) / 1000 /1000;
      /* Formatter fm = new Formatter();
      //long size = f.length();
      //if (size > 10 * 1000 * 1000) fm.format("%6.1f M", ((float) size) / 1000 / 1000);
      //else if (size > 10 * 1000) fm.format("%6.1f K", ((float) size) / 1000);
      //else fm.format("%d", size);
      fm.format("%,-15d", f.length() / 1000);
      return fm.toString(); */
    }

    public long getCdmData2D() {
      return cdmData2D;
    }

    public int getNrecords() {
      return nrecords;
    }

    public boolean isMatch() {
      return nrecords == cdmData2D;
    }

    public int getNvars() {
      return nvars;
    }

    public int getNdups() {
      return ndups;
    }

    public double getNc4SizeM() {
      return ((double) nc4Size) / 1000 / 1000;
    }

    public long getNc4Data2D() {
      return nc4Data2D;
    }

    public double getRatio() {
      long size = f.length();
      return (size == 0) ? 0 : nc4Size / (double) size;
    }

    public long countCdmData2D(NetcdfDataset ncd) throws IOException {
      long result = 0;
      GridDataset ds = new GridDataset(ncd);
      for (GridDatatype grid : ds.getGrids()) {
        int [] shape = grid.getShape();
        int rank = grid.getRank();
        long data2D = 1;
        for (int i=0; i<rank-2; i++)
          data2D *= shape[i];
        result += data2D;
      }
      return result;
    }

    public void countGribData2D(File f) throws IOException {
      String indexFilename = f.getPath() + CollectionAbstract.NCX_SUFFIX;

      FeatureCollectionConfig config = new FeatureCollectionConfig();

      try (GribCollectionImmutable gc = GribCdmIndex.openCdmIndex(indexFilename, config, false, logger)) {
        if (gc == null)
          throw new IOException("Not a grib collection index file");

        for (GribCollectionImmutable.Dataset ds : gc.getDatasets())
          for (GribCollectionImmutable.GroupGC group : ds.getGroups())
            for (GribCollectionImmutable.VariableIndex vi : group.getVariables()) {
              //vi.calcTotalSize();
              //nrecords += vi.nrecords;
              //ndups += vi.ndups;
              nvars++;
            }
      }
    }

    public void countNc4Data2D(File f) {
      String filename = f.getName();

      String nc4Filename = "G:/write/"+filename+".3.grib.nc4";
      File nc4 = new File(nc4Filename);
      if (!nc4.exists()) return;
      nc4Size = nc4.length();

      try (NetcdfDataset ncd = NetcdfDataset.openDataset(nc4Filename)) {
         nc4Data2D = countCdmData2D(ncd);
       } catch (IOException e) {
        System.out.printf("Error opening %s%n", nc4Filename);
      }
    }

  }
}
