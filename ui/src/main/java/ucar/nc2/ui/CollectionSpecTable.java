/*
 * Copyright 1998-2013 University Corporation for Atmospheric Research/Unidata
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

import thredds.inventory.*;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.ui.widget.BAMutil;
import ucar.nc2.ui.widget.IndependentWindow;
import ucar.nc2.ui.widget.TextHistoryPane;
import ucar.nc2.util.Misc;
import ucar.util.prefs.PreferencesExt;
import ucar.util.prefs.ui.BeanTable;

import javax.swing.*;
import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;

/**
 * Feature Collection Spec parsing
 *
 * @author caron
 * @since 4/9/13
 */
public class CollectionSpecTable extends JPanel {
  private PreferencesExt prefs;

  private BeanTable ftTable;
  private JSplitPane split;
  private TextHistoryPane infoTA, dumpTA;
  private IndependentWindow infoWindow;

  private List<MFile> fileList;
  private MFileCollectionManager dcm;

  public CollectionSpecTable(PreferencesExt prefs) {
    this.prefs = prefs;

    ftTable = new BeanTable(Bean.class, (PreferencesExt) prefs.node("FeatureDatasetBeans"), false);
    /* ftTable.addListSelectionListener(new ListSelectionListener() {
      public void valueChanged(ListSelectionEvent e) {
        FeatureScan.Bean ftb = (FeatureScan.Bean) ftTable.getSelectedBean();
        setSelectedFeatureDataset(ftb);
      }
    });

    /* PopupMenu varPopup = new ucar.nc2.ui.widget.PopupMenu(ftTable.getJTable(), "Options");
    varPopup.addAction("Open as NetcdfFile", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        FeatureScan.Bean ftb = (FeatureScan.Bean) ftTable.getSelectedBean();
        if (ftb == null) return;
        FeatureCollectionTable.this.firePropertyChange("openNetcdfFile", null, ftb.f.getPath());
      }
    });

    varPopup.addAction("Open in CoordSystems", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        FeatureScan.Bean ftb = (FeatureScan.Bean) ftTable.getSelectedBean();
        if (ftb == null) return;
        FeatureCollectionTable.this.firePropertyChange("openCoordSystems", null, ftb.f.getPath());
      }
    });

    varPopup.addAction("Open as PointDataset", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        FeatureScan.Bean ftb = (FeatureScan.Bean) ftTable.getSelectedBean();
        if (ftb == null) return;
        FeatureCollectionTable.this.firePropertyChange("openPointFeatureDataset", null, ftb.f.getPath());
      }
    });

    varPopup.addAction("Open as NcML", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        FeatureScan.Bean ftb = (FeatureScan.Bean) ftTable.getSelectedBean();
        if (ftb == null) return;
        FeatureCollectionTable.this.firePropertyChange("openNcML", null, ftb.f.getPath());
      }
    });

    varPopup.addAction("Open as GridDataset", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        FeatureScan.Bean ftb = (FeatureScan.Bean) ftTable.getSelectedBean();
        if (ftb == null) return;
        FeatureCollectionTable.this.firePropertyChange("openGridDataset", null, ftb.f.getPath());
      }
    });

    varPopup.addAction("Open as CoverageDataset", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        FeatureScan.Bean ftb = (FeatureScan.Bean) ftTable.getSelectedBean();
        if (ftb == null) return;
        FeatureCollectionTable.this.firePropertyChange("openCoverageDataset", null, ftb.f.getPath());
      }
    });

    varPopup.addAction("Open as RadialDataset", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        FeatureScan.Bean ftb = (FeatureScan.Bean) ftTable.getSelectedBean();
        if (ftb == null) return;
        FeatureCollectionTable.this.firePropertyChange("openRadialDataset", null, ftb.f.getPath());
      }
    });

    varPopup.addAction("Show Report on selected rows", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        List<FeatureScan.Bean> selected = ftTable.getSelectedBeans();
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
    });   */

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

  public boolean setCollection(String spec) throws Exception {
    spec = spec.trim();
    Formatter f = new Formatter();

    if (spec.startsWith("<collection ")) {
      dcm = setCollectionElement(spec, f);
    } else {
      CollectionSpecParser sp = new CollectionSpecParser(spec, f);
      f.format("spec='%s'%n", sp);
      dcm = scanCollection(spec, f) ;
    }
    showCollection(f);
    dumpTA.setText(f.toString());
    return dcm != null;
  }

  private static final String SPEC = "spec='";
  private static final String DFM = "dateFormatMark='";
  private MFileCollectionManager setCollectionElement(String elem, Formatter f) throws Exception {
    String spec = null;
    int pos1 = elem.indexOf(SPEC);
    if (pos1 > 0) {
      int pos2 = elem.indexOf("'", pos1+SPEC.length());
      if (pos2 > 0) {
        spec = elem.substring(pos1+SPEC.length(), pos2);
      }
    }
    if (spec == null) {
      f.format("want <collection spec='spec' [dateFormatMark='dfm'] ... %n");
      return null;
    }
    f.format("spec='%s' %n", spec);

    String dfm = null;
    pos1 = elem.indexOf(DFM);
    if (pos1 > 0) {
      int pos2 = elem.indexOf("'", pos1+DFM.length());
      if (pos2 > 0) {
        dfm = elem.substring(pos1+DFM.length(), pos2);
      }
    }

    dcm = scanCollection(spec, f);
    if (dfm != null) {
      dcm.setDateExtractor(new DateExtractorFromName(dfm, false));
      f.format("dateFormatMark='%s' %n", dfm);
    }
    return dcm;
  }

  public void showCollection(Formatter f) throws Exception {
    if (dcm == null) return;

    f.format("dcm = %s%n", dcm);
    for (MFile mfile : dcm.getFilesSorted()) {
      f.format("  %s%n", mfile.getPath());
    }
  }

  private MFileCollectionManager scanCollection(String spec, Formatter f) {
    MFileCollectionManager dc;
    try {
      dc = MFileCollectionManager.open(spec, spec, null, f);
      dc.scan(false);
      fileList = (List<MFile>) Misc.getList(dc.getFilesSorted());

      List<Bean> beans = new ArrayList<>();
      for (MFile mfile : fileList)
        beans.add(new Bean(mfile));
      ftTable.setBeans(beans);
      return dc;

    } catch (Exception e) {
      StringWriter sw = new StringWriter(10000);
      e.printStackTrace(new PrintWriter(sw));
      f.format("Exception %s", sw.toString());
      return null;
    }
  }

  public class Bean {
    MFile mfile;

    Bean(MFile mfile) {
      this.mfile = mfile;
    }

    public String getName() {
      return mfile.getName();
    }

    public String getDate() {
      CalendarDate cd = dcm.extractDate(mfile);
      if (cd != null) {
        return cd.toString();
      } else {
        return "Unknown";
      }
    }
  }
}
