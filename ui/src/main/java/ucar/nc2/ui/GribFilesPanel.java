/*
 * Copyright (c) 1998 - 2011. University Corporation for Atmospheric Research/Unidata
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

import thredds.inventory.DatasetCollectionMFiles;
import thredds.inventory.MFile;
import ucar.grib.NoValidGribException;
import ucar.grib.grib1.Grib1Input;
import ucar.grib.grib1.Grib1Pds;
import ucar.grib.grib1.Grib1Record;
import ucar.grib.grib1.GribPDSParamTable;
import ucar.grib.grib2.Grib2IndicatorSection;
import ucar.nc2.grib.grib2.*;
import ucar.nc2.grib.table.GribTables;
import ucar.nc2.ui.widget.*;
import ucar.nc2.ui.widget.PopupMenu;
import ucar.nc2.util.Misc;
import ucar.unidata.io.KMPMatch;
import ucar.unidata.io.RandomAccessFile;
import ucar.unidata.util.StringUtil2;
import ucar.util.prefs.PreferencesExt;
import ucar.util.prefs.ui.BeanTableSorted;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.*;
import java.util.List;

/**
 * Describe
 *
 * @author caron
 * @since 4/4/11
 */
public class GribFilesPanel extends JPanel {
  private PreferencesExt prefs;

  private BeanTableSorted grib1Table, grib2Table, collectionTable;
  private JSplitPane split, split2;

  private TextHistoryPane infoPopup;
  private IndependentWindow infoWindow;

  public GribFilesPanel(PreferencesExt prefs) {
    this.prefs = prefs;

    PopupMenu varPopup;

    ////////////////
    collectionTable = new BeanTableSorted(CollectionBean.class, (PreferencesExt) prefs.node("CollectionBean"), true);
    /* collectionTable.addListSelectionListener(new ListSelectionListener() {
      public void valueChanged(ListSelectionEvent e) {
        CollectionBean pb = (CollectionBean) collectionTable.getSelectedBean();
        if (pb != null)
          showFilesInCollection(pb);
      }
    }); */

    varPopup = new ucar.nc2.ui.widget.PopupMenu(collectionTable.getJTable(), "Options");
    varPopup.addAction("Show Files in Collection", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        CollectionBean pb = (CollectionBean) collectionTable.getSelectedBean();
        Formatter f = new Formatter();
        showFilesInCollection(pb, f);
        infoPopup.setText(f.toString());
        infoPopup.gotoTop();
        infoWindow.showIfNotIconified();
      }
    });

    varPopup.addAction("Read Files", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        List list = collectionTable.getSelectedBeans();
        readFiles(list);
      }
    });

    /* varPopup.addAction("Open in Grib2n", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        CollectionBean pb = (CollectionBean) collectionTable.getSelectedBean();
        if (pb == null) return;
        GribFilesPanel.this.firePropertyChange("openGrib2n", null, pb.getSpec());
      }
    }); */

    grib1Table = new BeanTableSorted(Grib1Bean.class, (PreferencesExt) prefs.node("Grib1Bean"), false);
    varPopup = new PopupMenu(grib1Table.getJTable(), "Options");
    varPopup.addAction("Open in Grib-Raw", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        Grib1Bean pb = (Grib1Bean) grib1Table.getSelectedBean();
        if (pb == null) return;
        GribFilesPanel.this.firePropertyChange("openGribRaw", null, pb.m.getPath());
      }
    });

    grib2Table = new BeanTableSorted(Grib2Bean.class, (PreferencesExt) prefs.node("Grib2Bean"), false);
    varPopup = new PopupMenu(grib2Table.getJTable(), "Options");

    varPopup.addAction("Show complete GridRecord", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        Grib2Bean bean = (Grib2Bean) grib2Table.getSelectedBean();
        if (bean != null) {
          Formatter f = new Formatter();
          bean.showComplete(f);
          infoPopup.setText(f.toString());
          infoPopup.gotoTop();
          infoWindow.showIfNotIconified();
        }
      }
    });

      varPopup.addAction("Open in Grib2n", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        Grib2Bean pb = (Grib2Bean) grib2Table.getSelectedBean();
        if (pb == null) return;
        GribFilesPanel.this.firePropertyChange("openGrib2n", null, pb.m.getPath());
      }
    });

    /////////////////////////////////////////
    // the info windows
    infoPopup = new TextHistoryPane();
    infoWindow = new IndependentWindow("Extra Information", BAMutil.getImage("netcdfUI"), infoPopup);
    infoWindow.setBounds((Rectangle) prefs.getBean("InfoWindowBounds", new Rectangle(300, 300, 500, 300)));

    setLayout(new BorderLayout());

    split2 = new JSplitPane(JSplitPane.VERTICAL_SPLIT, false, collectionTable, grib1Table);
    split2.setDividerLocation(prefs.getInt("splitPos2", 800));

    //split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, false, split2, gds2Table);
    //split.setDividerLocation(prefs.getInt("splitPos", 500));

    add(split2, BorderLayout.CENTER);

  }

  public void save() {
    grib1Table.saveState(false);
    grib2Table.saveState(false);
    collectionTable.saveState(false);
    prefs.putBeanObject("InfoWindowBounds", infoWindow.getBounds());
    if (split != null) prefs.putInt("splitPos", split.getDividerLocation());
    if (split2 != null) prefs.putInt("splitPos2", split2.getDividerLocation());
  }

  public void showCollection(Formatter f) {

  }

  private void showFilesInCollection(CollectionBean bean, Formatter f) {
    f.format("Collection= %s %n", bean.spec);
    int count = 0;
    for (MFile mfile : bean.fileList) {
      f.format("  %s%n", mfile.getPath());
      count++;
    }
    f.format(" count = %d %n", count);
  }

  ///////////////////////////////////////////////
  private static final KMPMatch matcher = new KMPMatch("GRIB".getBytes());

  private List<CollectionBean> collections = new ArrayList<CollectionBean>();

  public void setCollection(String spec) throws IOException {
    collections.add(new CollectionBean(spec));
    collectionTable.setBeans(collections);
  }

  private void readFiles(List<CollectionBean> beans) {
    List<Object> files = new ArrayList<Object>();
    for (CollectionBean bean : beans) {
      for (MFile mfile : bean.fileList) {
        String path = mfile.getPath();
        if (path.endsWith(".gbx8") || path.endsWith(".gbx9")  || path.endsWith(".ncx") )  continue;
        Object gbean = getGribBean(mfile);
        if (gbean != null)
          files.add( gbean);
      }
    }
    grib1Table.setBeans(files);
  }

  public Object getGribBean(MFile ff) {
    String path = ff.getPath();
    RandomAccessFile raf = null;
    try {
      raf = new ucar.unidata.io.RandomAccessFile(path, "r");
      raf.order(ucar.unidata.io.RandomAccessFile.BIG_ENDIAN);
      raf.seek(0);

       if (!raf.searchForward(matcher, 8000)) {  // must find "GRIB" in first 8k
         return null;
       }
       raf.skipBytes(4);
       //  Read Section 0 Indicator Section to get Edition number
       Grib2IndicatorSection is = new Grib2IndicatorSection(raf);  // section 0
       int edition = is.getGribEdition();

       if (edition == 1)
         return getGrib1Bean(ff, raf);
       //else if (edition == 2)
       //  setGribFile2(raf);

    } catch (Throwable ioe) {
      System.out.printf("Failed on %s%n", path);
      ioe.printStackTrace();

    } finally {
      if (raf != null) try {
        raf.close();
      } catch (IOException e) {
      }
    }
    return null;
 }

  Object getGrib1Bean(MFile mf, RandomAccessFile raf) throws IOException, NoValidGribException {
    Grib1Input reader = new Grib1Input(raf);
    raf.seek(0);
    reader.scan(false, true);
    List<Grib1Record> records = reader.getRecords();
    Grib1Record first = records.get(0);
    return new Grib1Bean(mf, first);
  }

  // see ggr.cdmVariableHash() {

  public int makeUniqueId(Grib1Pds pds) {
    int result = 17;
    result += result * 37 + pds.getParameterNumber();
    result *= result * 37 + pds.getLevelType1();
    if (pds.isEnsemble())
      result *= result * 37 + 1;
    return result;
  }

  ////////////////////////////////////////////////////////////////////////////

  public class CollectionBean {
    String spec;
    DatasetCollectionMFiles dcm;
    Iterable<MFile> fileList;

    // no-arg constructor

    public CollectionBean() {
    }

    public CollectionBean(String spec) throws IOException {
      this.spec = spec;

      Formatter f = new Formatter();
      DatasetCollectionMFiles dc = null;
      try {
        dc = DatasetCollectionMFiles.open(spec, null, f);
        dc.scan();
        fileList = dc.getFiles();

      } catch (Exception e) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream(10000);
        e.printStackTrace(new PrintStream(bos));
        f.format("%s", bos.toString());
        javax.swing.JOptionPane.showMessageDialog(null, "Collection is null");
      }
    }

    public int getN() {
      return Misc.getSize(fileList);
    }

    public String getSpec() {
      return spec;
    }

  }

  public class Grib1Bean {
    MFile m;
    Grib1Record first;
    GribPDSParamTable table;

    public Grib1Bean() {
    }

    public Grib1Bean(MFile m, Grib1Record first) {
      this.m = m;
      this.first = first;
    }

    public final String getPath() {
      return m.getPath();
    }

    public int getTableVersion() {
      return first.getPDS().getTableVersion();
    }

    public int getCenter() {
      return first.getPDS().getCenter();
    }

    public String getCenterName() {
      return first.getPDS().getCenterName();
    }

    public int getSubCenter() {
      return first.getPDS().getSubCenter();
    }

    public String getSubCenterName() {
      return first.getPDS().getSubCenterName();
    }

    public String getTable() {
      if (table == null)
        table = GribPDSParamTable.getParameterTable(getCenter(), getSubCenter(), getTableVersion());
      return (table == null) ? " missing" : table.getName();
    }

    public int getTableKey() {
      if (table == null)
        table = GribPDSParamTable.getParameterTable(getCenter(), getSubCenter(), getTableVersion());
      return (table == null) ? -1 : table.getKey();
    }

  }


  public class Grib2Bean {
    MFile m;
    GribIndex index;
    int nRecords, localCount = 0, gdsCount = 0;
    Grib2Record first;
    GribTables tables;
    boolean bad = false;

    public Grib2Bean() {
    }

    public Grib2Bean(MFile m) {
      this.m = m;

      try {
        index = new GribIndex();
        if (!index.readIndex(m.getPath(), m.getLastModified())) {
          index.makeIndex(m.getPath(), new Formatter());
        }

        Map<Long, Grib2SectionGridDefinition> gdsSet = new HashMap<Long, Grib2SectionGridDefinition>();
        for (Grib2SectionGridDefinition gds : index.getGds()) {
          if (gdsSet.get(gds.calcCRC()) == null)
            gdsSet.put(gds.calcCRC(), gds);
        }
        gdsCount = gdsSet.size();

        nRecords = index.getRecords().size();
        for (Grib2Record gr : index.getRecords()) {
          if (first == null) first = gr;
          if (tables == null) {
            Grib2SectionIdentification ids = gr.getId();
            tables = GribTables.factory(ids.getCenter_id(), ids.getSubcenter_id(), ids.getMaster_table_version(), ids.getLocal_table_version());
          }
          Grib2Pds pds = gr.getPDSsection().getPDS();
          if ((pds.getParameterCategory() > 191) || (pds.getParameterNumber() > 191))
            localCount++;
        }

      } catch (IOException e) {
        System.out.printf("%s%n", e.getMessage());
        bad = true;
      }
    }

    public final String getPath() {
      return (bad) ? "BAD "+ m.getPath() : m.getPath();
    }

    public final String getRefDate() {
      return (bad) ? "" : first.getReferenceDate().toString();
    }

    public String getHeader() {
      return (bad) ? "" : StringUtil2.cleanup(first.getHeader());
    }

    public int getMasterTable() {
      return (bad) ? -1 : first.getId().getMaster_table_version();
    }

    public int getLocalTable() {
      return (bad) ? -1 : first.getId().getLocal_table_version();
    }

    public int getCenter() {
      return (bad) ? -1 : first.getId().getCenter_id();
    }

    public int getSubCenter() {
      return (bad) ? -1 : first.getId().getSubcenter_id();
    }

    public String getLocalCount() {
      return (bad) ? "" : localCount+"/"+nRecords;
    }

    public int getGdsCount() {
      return gdsCount;
    }

    void showComplete(Formatter f) {
      try {
        GribNewPanel.showCompleteGribRecord(f, m.getPath(), first, tables);
      } catch (IOException e) {
        e.printStackTrace();
        ByteArrayOutputStream bos = new ByteArrayOutputStream(10000);
        e.printStackTrace(new PrintStream(bos));
        f.format("%s", bos.toString());
      }
    }

  }

}

