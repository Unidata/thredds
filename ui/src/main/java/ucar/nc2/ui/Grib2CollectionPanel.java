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
import ucar.ma2.DataType;
import ucar.nc2.grib.GribCollection;
import ucar.nc2.grib.GribNumbers;
import ucar.nc2.grib.GribUtils;
import ucar.nc2.grib.grib1.tables.Grib1Parameter;
import ucar.nc2.grib.grib2.Grib2CollectionBuilder;
import ucar.nc2.grib.grib2.Grib2Rectilyser;
import ucar.nc2.grib.grib2.*;
import ucar.nc2.grib.grib2.table.Grib2Tables;
import ucar.nc2.grib.grib2.table.WmoTemplateTable;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.ui.widget.FileManager;
import ucar.nc2.ui.widget.*;
import ucar.nc2.ui.widget.PopupMenu;
import ucar.nc2.util.Misc;
import ucar.nc2.wmo.CommonCodeTable;

import ucar.unidata.util.StringUtil2;
import ucar.util.prefs.PreferencesExt;
import ucar.util.prefs.ui.BeanTableSorted;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import ucar.nc2.ui.widget.IndependentWindow;
import ucar.nc2.ui.widget.BAMutil;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.*;
import java.util.*;
import java.util.List;

/**
 * Grib2 refactor
 *
 * @author caron
 * @since Aug 15, 2008
 */
public class Grib2CollectionPanel extends JPanel {
  static private Map<String, WmoTemplateTable> gribTemplates = null;

  private PreferencesExt prefs;

  private BeanTableSorted param2BeanTable, record2BeanTable, gds2Table;
  private JSplitPane split, split2;

  private TextHistoryPane infoPopup, infoPopup2, infoPopup3;
  private IndependentWindow infoWindow, infoWindow2, infoWindow3;
  private FileManager fileChooser;

  public Grib2CollectionPanel(PreferencesExt prefs) {
    this.prefs = prefs;

    PopupMenu varPopup;

    ////////////////
    param2BeanTable = new BeanTableSorted(Grib2ParameterBean.class, (PreferencesExt) prefs.node("Param2Bean"), false, "Grib2PDSVariables", "from Grib2Input.getRecords()");
    param2BeanTable.addListSelectionListener(new ListSelectionListener() {
      public void valueChanged(ListSelectionEvent e) {
        Grib2ParameterBean pb = (Grib2ParameterBean) param2BeanTable.getSelectedBean();
        if (pb != null)
          record2BeanTable.setBeans(pb.getRecordBeans());
      }
    });

    varPopup = new ucar.nc2.ui.widget.PopupMenu(param2BeanTable.getJTable(), "Options");
    varPopup.addAction("Show PDS", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        Grib2ParameterBean pb = (Grib2ParameterBean) param2BeanTable.getSelectedBean();
        if (pb != null) {
          Formatter f = new Formatter();
          showPdsTemplate(pb.gr.getPDSsection(), f, tables);
          infoPopup2.setText(f.toString());
          infoPopup2.gotoTop();
          infoWindow2.showIfNotIconified();
        }
      }
    });
    varPopup.addAction("Show processed PDS", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        Grib2ParameterBean pb = (Grib2ParameterBean) param2BeanTable.getSelectedBean();
        if (pb != null) {
          infoPopup3.setText(pb.toProcessedString());
          infoPopup3.gotoTop();
          infoWindow3.showIfNotIconified();
        }
      }
    });

    /* varPopup.addAction("Run Aggregator", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        Grib2ParameterBean pb = (Grib2ParameterBean) param2BeanTable.getSelectedBean();
        if (pb != null) {
          Formatter f = new Formatter();
          doAggegator(pb, f);
          infoPopup2.setText(f.toString());
          infoPopup3.gotoTop();
          infoWindow3.showIfNotIconified();
        }
      }
    }); */

    record2BeanTable = new BeanTableSorted(Grib2RecordBean.class, (PreferencesExt) prefs.node("Record2Bean"), false, "Grib2Record", "from Grib2Input.getRecords()");
    varPopup = new PopupMenu(record2BeanTable.getJTable(), "Options");

    varPopup.addAction("Compare GridRecord", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        List list = record2BeanTable.getSelectedBeans();
        if (list.size() == 2) {
          Grib2RecordBean bean1 = (Grib2RecordBean) list.get(0);
          Grib2RecordBean bean2 = (Grib2RecordBean) list.get(1);
          Formatter f = new Formatter();
          compare(bean1, bean2, f);
          infoPopup2.setText(f.toString());
          infoPopup2.gotoTop();
          infoWindow2.showIfNotIconified();
        }
      }
    });

    varPopup.addAction("Show raw PDS bytes", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        Formatter f = new Formatter();
        List list = record2BeanTable.getSelectedBeans();
        for (int i = 0; i < list.size(); i++) {
          Grib2RecordBean bean = (Grib2RecordBean) list.get(i);
          bean.toRawPdsString(f);
        }
        infoPopup.setText(f.toString());
        infoPopup.gotoTop();
        infoWindow.showIfNotIconified();
      }
    });

    varPopup.addAction("Show complete GridRecord", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        Grib2RecordBean bean = (Grib2RecordBean) record2BeanTable.getSelectedBean();
        if (bean != null) {
          Formatter f = new Formatter();
          try {
            showCompleteGribRecord(f, fileList.get(bean.gr.getFile()).getPath(), bean.gr, tables);
          } catch (IOException ioe) {
            ByteArrayOutputStream bos = new ByteArrayOutputStream(10000);
            ioe.printStackTrace(new PrintStream(bos));
            f.format("%s", bos.toString());
          }
          infoPopup.setText(f.toString());
          infoPopup.gotoTop();
          infoWindow.showIfNotIconified();
        }
      }
    });

    varPopup.addAction("Show Processed GridRecord", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        List list = record2BeanTable.getSelectedBeans();
        Formatter f = new Formatter();
        for (Object o : list) {
          Grib2RecordBean bean = (Grib2RecordBean) o;
          bean.showProcessedGridRecord(f);
        }
        infoPopup2.setText(f.toString());
        infoPopup2.gotoTop();
        infoWindow2.showIfNotIconified();
      }
    });

    varPopup.addAction("Compare Data", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        List list = record2BeanTable.getSelectedBeans();
        if (list.size() == 2) {
          Grib2RecordBean bean1 = (Grib2RecordBean) list.get(0);
          Grib2RecordBean bean2 = (Grib2RecordBean) list.get(1);
          Formatter f = new Formatter();
          compareData(bean1, bean2, f);
          infoPopup2.setText(f.toString());
          infoPopup2.gotoTop();
          infoWindow2.showIfNotIconified();
        }
      }
    });

    varPopup.addAction("Show Data", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        Grib2RecordBean bean = (Grib2RecordBean) record2BeanTable.getSelectedBean();
        if (bean != null) {
          Formatter f = new Formatter();
          showData(bean, f);
          infoPopup2.setText(f.toString());
          infoPopup2.gotoTop();
          infoWindow2.showIfNotIconified();
        }
      }
    });

    varPopup.addAction("Extract GribRecord to File", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        List beans = record2BeanTable.getSelectedBeans();
        if (beans.size() > 0)
          writeToFile(beans);
      }
    });

    gds2Table = new BeanTableSorted(Gds2Bean.class, (PreferencesExt) prefs.node("Gds2Bean"), false, "Grib2GridDefinitionSection", "unique from Grib2Records");
    varPopup = new PopupMenu(gds2Table.getJTable(), "Options");

    varPopup.addAction("Show GDS", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        Gds2Bean bean = (Gds2Bean) gds2Table.getSelectedBean();
        if (bean == null) return;
        Formatter f = new Formatter();
        Grib2Gds ggds = bean.gdss.getGDS();
        f.format("GDS hash=%d crc=%d%n", ggds.hashCode(), bean.gdss.calcCRC());
        showGdsTemplate(bean.gdss, f, tables);
        infoPopup2.setText(f.toString());
        infoPopup2.gotoTop();
        infoWindow2.showIfNotIconified();
      }
    });

    varPopup.addAction("Compare GDS", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        List list = gds2Table.getSelectedBeans();
        if (list.size() == 2) {
          Gds2Bean bean1 = (Gds2Bean) list.get(0);
          Gds2Bean bean2 = (Gds2Bean) list.get(1);
          Formatter f = new Formatter();
          compare(bean1.gdss, bean2.gdss, f);
          infoPopup2.setText(f.toString());
          infoPopup2.gotoTop();
          infoWindow2.showIfNotIconified();
        }
      }
    });

    varPopup.addAction("Show raw GDS bytes", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        Formatter f = new Formatter();
        List list = gds2Table.getSelectedBeans();
        for (int i = 0; i < list.size(); i++) {
          Gds2Bean bean = (Gds2Bean) list.get(i);
          bean.toRawGdsString(f);
        }
        infoPopup.setText(f.toString());
        infoPopup.gotoTop();
        infoWindow.showIfNotIconified();
      }
    });

    varPopup.addAction("Show Files that use this GDS", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        Gds2Bean want = (Gds2Bean) gds2Table.getSelectedBean();
        if (want == null) return;
        SortedSet<Integer> files = new TreeSet<Integer>();

        for (Object o : param2BeanTable.getBeans()) {
          Grib2ParameterBean p = (Grib2ParameterBean) o;
          if (p.getGDS() == want.getGDShash()) {
            for (Grib2RecordBean r : p.getRecordBeans())
              files.add(r.gr.getFile());
          }
        }

        Formatter f = new Formatter();
        Iterator<Integer> iter = files.iterator();
        while (iter.hasNext()) {
          int fileno = iter.next();
          f.format(" %d = %s%n", fileno, fileList.get(fileno).getPath());
        }
        infoPopup2.setText(f.toString());
        infoPopup2.gotoTop();
        infoWindow2.showIfNotIconified();
      }
    });

    varPopup.addAction("Restrict to this GDS", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        Gds2Bean want = (Gds2Bean) gds2Table.getSelectedBean();
        if (want == null) return;
        java.util.List<Grib2ParameterBean> params = new ArrayList<Grib2ParameterBean>();
        for (Object o : param2BeanTable.getBeans()) {
          Grib2ParameterBean p = (Grib2ParameterBean) o;
          if (p.getGDS() == want.getGDShash())
            params.add(p);
        }
        param2BeanTable.setBeans(params);
      }
    });

    varPopup.addAction("Test GDS Projection", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        Gds2Bean bean = (Gds2Bean) gds2Table.getSelectedBean();
        if (bean == null) return;
        Formatter f = new Formatter();
        bean.gds.testHorizCoordSys(f);
        infoPopup2.setText(f.toString());
        infoPopup2.gotoTop();
        infoWindow2.showIfNotIconified();
      }
    });

    /////////////////////////////////////////
    // the info windows
    infoPopup = new TextHistoryPane();
    infoWindow = new IndependentWindow("Extra Information", BAMutil.getImage("netcdfUI"), infoPopup);
    infoWindow.setBounds((Rectangle) prefs.getBean("InfoWindowBounds", new Rectangle(300, 300, 500, 300)));

    infoPopup2 = new TextHistoryPane();
    infoWindow2 = new IndependentWindow("Extra Information", BAMutil.getImage("netcdfUI"), infoPopup2);
    infoWindow2.setBounds((Rectangle) prefs.getBean("InfoWindowBounds2", new Rectangle(300, 300, 500, 300)));

    infoPopup3 = new TextHistoryPane();
    infoWindow3 = new IndependentWindow("Extra Information", BAMutil.getImage("netcdfUI"), infoPopup3);
    infoWindow3.setBounds((Rectangle) prefs.getBean("InfoWindowBounds3", new Rectangle(300, 300, 500, 300)));

    setLayout(new BorderLayout());

    split2 = new JSplitPane(JSplitPane.VERTICAL_SPLIT, false, param2BeanTable, record2BeanTable);
    split2.setDividerLocation(prefs.getInt("splitPos2", 800));

    split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, false, split2, gds2Table);
    split.setDividerLocation(prefs.getInt("splitPos", 500));

    add(split, BorderLayout.CENTER);

  }

  public void save() {
    gds2Table.saveState(false);
    param2BeanTable.saveState(false);
    record2BeanTable.saveState(false);
    prefs.putBeanObject("InfoWindowBounds", infoWindow.getBounds());
    prefs.putBeanObject("InfoWindowBounds2", infoWindow2.getBounds());
    prefs.putBeanObject("InfoWindowBounds3", infoWindow3.getBounds());
    if (split != null) prefs.putInt("splitPos", split.getDividerLocation());
    if (split2 != null) prefs.putInt("splitPos2", split2.getDividerLocation());
  }

  ///////////////////////////////////////////////

  private String spec;
  private DatasetCollectionMFiles dcm;
  private List<MFile> fileList;
  private Grib2Tables tables;

  /* public void setCollection(String filename) throws IOException {
    if (filename.endsWith(GribCollection.IDX_EXT)) {
      openIndex(filename);
    } else {
      openCollection(filename);
    }
  }

  private void openIndex(String filename) throws IOException {
    GribCollection gribCollection = new GribCollection(null);
    if (!gribCollection.readIndex(filename))
      throw new FileNotFoundException();

    this.tables = GribTables.factory(gribCollection.center, gribCollection.subcenter, gribCollection.master, gribCollection.local);

    java.util.List<Grib2ParameterBean> params = new ArrayList<Grib2ParameterBean>();
    java.util.List<Gds2Bean> gdsList = new ArrayList<Gds2Bean>();

    for (GribCollection.GroupHcs gHcs : gribCollection.getGroups()) {
      for (GribCollection.VariableIndex vi : gHcs.varIndex) {
        for (vi.records)
      }
    }
      addGroup(ncfile, g, useGroups);


    int fileno = 0;
    fileList = dcm.getFiles();
    for (MFile mfile : fileList) {
      f.format("%n %s%n", mfile.getPath());
      processGribFile(mfile.getPath(), fileno++, pdsSet, gdsSet, params);
    }
    param2BeanTable.setBeans(params);

    for (Grib2SectionGridDefinition gds : gdsSet.values()) {
      gdsList.add(new Gds2Bean( gds));
    }
    gds2Table.setBeans(gdsList);
  }  */

  public void setCollection(String spec) throws IOException {
    this.spec = spec;
    this.tables = null;

    Formatter f = new Formatter();
    this.dcm = scanCollection(spec, f);
    if (dcm == null) {
      javax.swing.JOptionPane.showMessageDialog(this, "Collection is null\n" + f.toString());
      return;
    }

    Map<Integer, Grib2ParameterBean> pdsSet = new HashMap<Integer, Grib2ParameterBean>();
    Map<Integer, Grib2SectionGridDefinition> gdsSet = new HashMap<Integer, Grib2SectionGridDefinition>();

    java.util.List<Grib2ParameterBean> params = new ArrayList<Grib2ParameterBean>();
    java.util.List<Gds2Bean> gdsList = new ArrayList<Gds2Bean>();

    int fileno = 0;
    for (MFile mfile : fileList) {
      f.format("%n %s%n", mfile.getPath());
      processGribFile(mfile, fileno++, pdsSet, gdsSet, params, f);
    }
    param2BeanTable.setBeans(params);

    for (Grib2SectionGridDefinition gds : gdsSet.values()) {
      gdsList.add(new Gds2Bean(gds));
    }
    Collections.sort(gdsList);
    gds2Table.setBeans(gdsList);
  }

  private void processGribFile(MFile mfile, int fileno,
                               Map<Integer, Grib2ParameterBean> pdsSet,
                               Map<Integer, Grib2SectionGridDefinition> gdsSet,
                               List<Grib2ParameterBean> params, Formatter f) throws IOException {

    Grib2Index index = new Grib2Index();
    if (!index.readIndex(mfile.getPath(), mfile.getLastModified())) {
      index.makeIndex(mfile.getPath(), f);
    }

    for (Grib2SectionGridDefinition gds : index.getGds()) {
      int hash = gds.getGDS().hashCode();
      if (gdsSet.get(hash) == null)
        gdsSet.put(hash, gds);
    }

    Grib2Rectilyser rect = new Grib2Rectilyser(tables, null, 0, false);

    for (Grib2Record gr : index.getRecords()) {
      gr.setFile(fileno);

      if (tables == null) {
        Grib2SectionIdentification ids = gr.getId();
        tables = Grib2Tables.factory(ids.getCenter_id(), ids.getSubcenter_id(), ids.getMaster_table_version(), ids.getLocal_table_version());
      }


      int id = rect.cdmVariableHash(gr, tables, 0);
      Grib2ParameterBean bean = pdsSet.get(id);
      if (bean == null) {
        bean = new Grib2ParameterBean(gr);
        pdsSet.put(id, bean);
        params.add(bean);
      }
      bean.addRecord(gr);
    }
  }

  private DatasetCollectionMFiles scanCollection(String spec, Formatter f) {
    DatasetCollectionMFiles dc = null;
    try {
      dc = DatasetCollectionMFiles.open(spec, null, f);
      dc.scan(false);
      fileList = (List<MFile>) Misc.getList(dc.getFiles());
      return dc;

    } catch (Exception e) {
      ByteArrayOutputStream bos = new ByteArrayOutputStream(10000);
      e.printStackTrace(new PrintStream(bos));
      f.format("%s", bos.toString());
      return null;
    }
  }

  public void runAggregator(Formatter f) throws IOException {
    List<Grib2Record> records = new ArrayList<Grib2Record>();
    List<String> filenames = new ArrayList<String>();

    int fileno = 0;
    for (MFile mfile : dcm.getFiles()) {
      f.format("%3d: %s%n", fileno, mfile.getPath());
      filenames.add(mfile.getPath());

      Grib2Index index = new Grib2Index();
      if (!index.readIndex(mfile.getPath(), mfile.getLastModified())) {
        index.makeIndex(mfile.getPath(), f);
      }

      for (Grib2Record gr : index.getRecords()) {
        gr.setFile(fileno);
        records.add(gr);
      }
      fileno++;
    }

    Grib2Rectilyser agg = new Grib2Rectilyser(tables, records, 0, false);
    agg.make(f, new Grib2Rectilyser.Counter());
    agg.dump(f, tables);

    f.format("total records= %d%n", records.size());
  }

  /* public void runCollate(Formatter f) throws IOException {
    DatasetCollectionManager dcm = getCollection(spec, f);
    GribCollection gc = GribCollectionBuilder.factory(dcm);
    ArrayList<String> filenames = new ArrayList<String>();
    List<GribCollection.Group> groups = gc.makeAggregatedGroups(filenames, f);

    for (GribCollection.Group g : groups) {
      f.format("====================================================%n");
      f.format("Group %s%n", g.name);
      g.rect.dump(f, tables);
    }
  } */

  public boolean writeIndex(Formatter f) throws IOException {
    DatasetCollectionMFiles dcm = scanCollection(spec, f);

    if (fileChooser == null)
      fileChooser = new FileManager(null, null, null, (PreferencesExt) prefs.node("FileManager"));
    String name = dcm.getCollectionName();
    int pos = name.lastIndexOf('/');
    if (pos < 0) pos = name.lastIndexOf('\\');
    if (pos > 0) name = name.substring(pos + 1);
    File def = new File(dcm.getRoot(), name + GribCollection.IDX_EXT);
    String filename = fileChooser.chooseFilename(def);
    if (filename == null) return false;
    if (!filename.endsWith(GribCollection.IDX_EXT))
      filename += GribCollection.IDX_EXT;
    File idxFile = new File(filename);

    Grib2CollectionBuilder.writeIndexFile(idxFile, dcm, f);
    return true;
  }

  public void showCollection(Formatter f) {
    if (dcm == null) {
      if (spec == null) return;
      dcm = scanCollection(spec, f);
      if (dcm == null) return;
    }

    // just a list of the files
    f.format("dcm = %s%n", dcm);
    for (MFile mfile : dcm.getFiles()) {
      f.format("  %s%n", mfile.getPath());
    }

    // divided by group
    Map<Integer, Set<Integer>> groups = new HashMap<Integer, Set<Integer>>();
    for (Object o : param2BeanTable.getBeans()) {
      Grib2ParameterBean p = (Grib2ParameterBean) o;
      Set<Integer> group = groups.get(p.getGDS());
      if (group == null) {
        group = new TreeSet<Integer>();
        groups.put(p.getGDS(), group);
      }
      for (Grib2RecordBean r : p.getRecordBeans())
        group.add(r.gr.getFile());
    }

    for (Object o : gds2Table.getBeans()) {
      Gds2Bean gds = (Gds2Bean) o;
      Set<Integer> group = groups.get(gds.getGDShash());
      f.format("%nGroup %s %n", gds.getGroupName());
      if (group == null) continue;
      Iterator<Integer> iter = group.iterator();
      while (iter.hasNext()) {
        int fileno = iter.next();
        f.format(" %d = %s%n", fileno, fileList.get(fileno).getPath());
      }
      f.format("%n");
    }
  }

  public void showGDSuse(Formatter f) {

    Map<Long, Gds2Bean> gdsMap = new HashMap<Long, Gds2Bean>();
    Map<Long, Set<Integer>> fileMap = new HashMap<Long, Set<Integer>>();
    List<Gds2Bean> beans = gds2Table.getBeans();
    for (Gds2Bean gdsBean : beans) {
      fileMap.put(gdsBean.gdss.calcCRC(), new TreeSet<Integer>());
      gdsMap.put(gdsBean.gdss.calcCRC(), gdsBean);
    }

    for (Object o : param2BeanTable.getBeans()) {
      Grib2ParameterBean p = (Grib2ParameterBean) o;
      Set<Integer> files = fileMap.get(p.getGDS());
      for (Grib2RecordBean r : p.getRecordBeans())
        files.add(r.gr.getFile());
    }

    for (Long key : fileMap.keySet()) {
      Gds2Bean gds = gdsMap.get(key);
      Set<Integer> files = fileMap.get(key);
      Iterator<Integer> iter = files.iterator();
      f.format("%nGDS %d == %s%n", key, gds);
      while (iter.hasNext()) {
        int fileno = iter.next();
        f.format(" %d = %s%n", fileno, fileList.get(fileno).getPath());
      }
    }

  }

  public void checkProblems(Formatter f) {
    checkDuplicates(f);
    //checkRuntimes(f);
  }

  private class DateCount implements Comparable<DateCount> {
    CalendarDate d;
    int count;

    private DateCount(CalendarDate d) {
      this.d = d;
    }

    @Override
    public int compareTo(DateCount o) {
      return d.compareTo(o.d);
    }
  }

  /* private void checkRuntimes(Formatter f) {
     Map<Date, DateCount> runs = new HashMap<Date, DateCount>();
     List<Grib2ParameterBean> params = param2BeanTable.getBeans();
     for (Grib2ParameterBean pb : params) {
       List<Grib2RecordBean> records = pb.getRecordBeans();
       for (Grib2RecordBean record : records) {
         Date d = record.getBaseTime();
         DateCount dc = runs.get(d);
         if (dc == null) {
           dc = new DateCount(d);
           runs.put(d, dc);
         }
         dc.count++;
       }
     }

     List<DateCount> dcList= new ArrayList<DateCount>(runs.values());
     Collections.sort(dcList);

     f.format("Run Dates%n");
     for (DateCount dc : dcList)
       f.format(" %s == %d%n", df.toDateTimeStringISO( dc.d), dc.count);
   } */

  private void checkDuplicates(Formatter f) {

    // how unique are the pds ?
    Set<Long> pdsMap = new HashSet<Long>();
    int dups = 0;
    int count = 0;

    // do all records have the same runtime ?
    Map<CalendarDate, DateCount> dateMap = new HashMap<CalendarDate, DateCount>();

    List<Grib2ParameterBean> params = param2BeanTable.getBeans();
    for (Grib2ParameterBean param : params) {
      for (Grib2RecordBean record : param.getRecordBeans()) {
        CalendarDate d = record.gr.getReferenceDate();
        DateCount dc = dateMap.get(d);
        if (dc == null) {
          dc = new DateCount(d);
          dateMap.put(d, dc);
        }
        dc.count++;

        Grib2SectionProductDefinition pdss = record.gr.getPDSsection();
        long crc = pdss.calcCRC();
        if (pdsMap.contains(crc))
          dups++;
        else
          pdsMap.add(crc);
        count++;
      }
    }

    f.format("PDS duplicates = %d / %d%n%n", dups, count);

    List<DateCount> dcList = new ArrayList<DateCount>(dateMap.values());
    Collections.sort(dcList);

    f.format("Run Dates%n");
    int total = 0;
    for (DateCount dc : dcList) {
      f.format(" %s == %d%n", dc.d, dc.count);
      total += dc.count;
    }
    f.format("total records = %d%n", total);
  }

  private void writeToFile(List beans) {

    if (fileChooser == null)
      fileChooser = new FileManager(null, null, null, (PreferencesExt) prefs.node("FileManager"));

    FileOutputStream fos = null;
    RandomAccessFile raf = null;

    try {
      String filename = null;
      boolean append = false;
      int n = 0;
      MFile curr = null;

      for (Object o : beans) {
        Grib2RecordBean bean = (Grib2RecordBean) o;
        MFile mfile = fileList.get(bean.gr.getFile());
        if (curr == null || curr != mfile) {
          if (raf != null) raf.close();
          raf = new RandomAccessFile(mfile.getPath(), "r");
          curr = mfile;
        }

        if (fos == null) {
          String defloc = mfile.getPath();
          filename = fileChooser.chooseFilenameToSave(defloc + ".grib2");
          if (filename == null) return;
          File f = new File(filename);
          append = f.exists();
          fos = new FileOutputStream(filename, append);
        }

        Grib2SectionIndicator is = bean.gr.getIs();
        int size = (int) (is.getMessageLength());
        long startPos = is.getStartPos();
        if (startPos < 0) {
          JOptionPane.showMessageDialog(Grib2CollectionPanel.this, "Old index does not have message start - record not written");
        }

        byte[] rb = new byte[size];
        raf.seek(startPos);
        raf.readFully(rb);
        fos.write(rb);
        n++;
      }

      JOptionPane.showMessageDialog(Grib2CollectionPanel.this, filename + ": " + n + " records successfully written, append=" + append);

    } catch (Exception ex) {
      JOptionPane.showMessageDialog(Grib2CollectionPanel.this, "ERROR: " + ex.getMessage());
      ex.printStackTrace();

    } finally {
      try {
        if (fos != null) fos.close();
        if (raf != null) raf.close();
      } catch (IOException ioe) {
      }
    }
  }


  private void compare(Grib2RecordBean bean1, Grib2RecordBean bean2, Formatter f) {
    Grib2SectionIndicator is1 = bean1.gr.getIs();
    Grib2SectionIndicator is2 = bean2.gr.getIs();
    f.format("Indicator Section%n");
    if (is1.getDiscipline() != is2.getDiscipline())
      f.format("getDiscipline differs %d != %d %n", is1.getDiscipline(), is2.getDiscipline());
    if (is1.getMessageLength() != is2.getMessageLength())
      f.format("getGribLength differs %d != %d %n", is1.getMessageLength(), is2.getMessageLength());

    f.format("%nId Section%n");
    Grib2SectionIdentification id1 = bean1.gr.getId();
    Grib2SectionIdentification id2 = bean2.gr.getId();
    if (id1.getCenter_id() != id2.getCenter_id())
      f.format("Center_id differs %d != %d %n", id1.getCenter_id(), id2.getCenter_id());
    if (id1.getSubcenter_id() != id2.getSubcenter_id())
      f.format("Subcenter_id differs %d != %d %n", id1.getSubcenter_id(), id2.getSubcenter_id());
    if (id1.getMaster_table_version() != id2.getMaster_table_version())
      f.format("Master_table_version differs %d != %d %n", id1.getMaster_table_version(), id2.getMaster_table_version());
    if (id1.getLocal_table_version() != id2.getLocal_table_version())
      f.format("Local_table_version differs %d != %d %n", id1.getLocal_table_version(), id2.getLocal_table_version());
    if (id1.getProductionStatus() != id2.getProductionStatus())
      f.format("ProductionStatus differs %d != %d %n", id1.getProductionStatus(), id2.getProductionStatus());
    if (id1.getTypeOfProcessedData() != id2.getTypeOfProcessedData())
      f.format("TypeOfProcessedData differs %d != %d %n", id1.getTypeOfProcessedData(), id2.getTypeOfProcessedData());
    if (!id1.getReferenceDate().equals(id2.getReferenceDate()))
      f.format("ReferenceDate differs %s != %s %n", id1.getReferenceDate(), id2.getReferenceDate());
    if (id1.getSignificanceOfRT() != id2.getSignificanceOfRT())
      f.format("getSignificanceOfRT differs %d != %d %n", id1.getSignificanceOfRT(), id2.getSignificanceOfRT());


    Grib2SectionLocalUse lus1 = bean1.gr.getLocalUseSection();
    Grib2SectionLocalUse lus2 = bean2.gr.getLocalUseSection();
    if (lus1 == null || lus2 == null) {
      if (lus1 == lus2)
        f.format("%nLus are both null%n");
      else
        f.format("%nLus are different %s != %s %n", lus1, lus2);
    } else {
      f.format("%nCompare LocalUseSection%n");
      Misc.compare(lus1.getRawBytes(), lus2.getRawBytes(), f);
    }

    compare(bean1.gr.getPDSsection(), bean2.gr.getPDSsection(), f);
    compare(bean1.gr.getGDSsection(), bean2.gr.getGDSsection(), f);
  }

  private void compare(Grib2SectionGridDefinition gds1, Grib2SectionGridDefinition gds2, Formatter f) {
    f.format("%nCompare Gds%n");
    byte[] raw1 = gds1.getRawBytes();
    byte[] raw2 = gds2.getRawBytes();
    Misc.compare(raw1, raw2, f);
  }


  private void compare(Grib2SectionProductDefinition pds1, Grib2SectionProductDefinition pds2, Formatter f) {
    f.format("%nCompare Pds%n");
    byte[] raw1 = pds1.getRawBytes();
    byte[] raw2 = pds2.getRawBytes();
    Misc.compare(raw1, raw2, f);
  }

  void compareData(Grib2RecordBean bean1, Grib2RecordBean bean2, Formatter f) {
    float[] data1 = null, data2 = null;
    try {
      data1 = bean1.readData();
      data2 = bean2.readData();
    } catch (IOException e) {
      f.format("IOException %s", e.getMessage());
      return;
    }

    Misc.compare(data1, data2, f);
  }

  void showData(Grib2RecordBean bean1, Formatter f) {
    float[] data;
    try {
      data = bean1.readData();
    } catch (IOException e) {
      f.format("IOException %s", e.getMessage());
      return;
    }

    for (float fd : data)
      f.format("%f%n", fd);
  }

  ////////////////////////////////////////////////////////////////////////////////////////////////

  /*
  /* private String doAccumAlgo(Grib2ParameterBean pb) {
    List<Grib2RecordBean> all = new ArrayList<Grib2RecordBean>(pb.getRecordBeans());
    List<Grib2RecordBean> hourAccum = new ArrayList<Grib2RecordBean>(all.size());
    List<Grib2RecordBean> runAccum = new ArrayList<Grib2RecordBean>(all.size());

    Set<Integer> ftimes = new HashSet<Integer>();

    for (Grib2RecordBean rb : pb.getRecordBeans()) {
      int ftime = rb.getForecastTime();
      ftimes.add(ftime);

      int start = rb.getStartInterval();
      int end = rb.getEndInterval();
      if (end - start == 1) hourAccum.add(rb);
      if (start == 0) runAccum.add(rb);
    }

    int n = ftimes.size();

    Formatter f = new Formatter();
    f.format("      all: ");
    showList(all, f);
    f.format("%n");

    if (hourAccum.size() > n - 2) {
      for (Grib2RecordBean rb : hourAccum) all.remove(rb);
      f.format("hourAccum: ");
      showList(hourAccum, f);
      f.format("%n");
    }

    if (runAccum.size() > n - 2) {
      for (Grib2RecordBean rb : runAccum) all.remove(rb);
      f.format(" runAccum: ");
      showList(runAccum, f);
      f.format("%n");
    }

    if (all.size() > 0) {
      boolean same = true;
      int intv = -1;
      for (Grib2RecordBean rb : all) {
        int start = rb.getStartInterval();
        int end = rb.getEndInterval();
        int intv2 = end - start;
        if (intv < 0) intv = intv2;
        else same = (intv == intv2);
        if (!same) break;
      }

      f.format("remaining: ");
      showList(all, f);
      f.format("%s %n", same ? " Interval=" + intv : " Mixed");
    }

    return f.toString();
  }

  private void showList(List<Grib2RecordBean> list, Formatter f) {
    f.format("(%d) ", list.size());
    for (Grib2RecordBean rb : list)
      f.format(" %d-%d", rb.getStartInterval(), rb.getEndInterval());
  }  */

  ////////////////////////////////////////////////////////////////////////////

  public class Grib2ParameterBean {
    Grib2Record gr;
    Grib2SectionIdentification id;
    Grib2Pds pds;
    List<Grib2RecordBean> records;
    int discipline;
    //long gdsKey;

    // no-arg constructor

    public Grib2ParameterBean() {
    }

    public Grib2ParameterBean(Grib2Record r) throws IOException {
      this.gr = r;

      // long refTime = r.getId().getReferenceDate().getMillis();
      pds = r.getPDSsection().getPDS();
      id = r.getId();
      discipline = r.getDiscipline();
      records = new ArrayList<Grib2RecordBean>();
      //gdsKey = r.getGDSsection().calcCRC();
    }

    void addRecord(Grib2Record r) throws IOException {
      records.add(new Grib2RecordBean(r));
    }

    List<Grib2RecordBean> getRecordBeans() {
      return records;
    }

    public String getParamNo() {
      return discipline + "-" + pds.getParameterCategory() + "-" + pds.getParameterNumber();
    }

    public int getPDS() {
      return gr.getPDSsection().getPDSTemplateNumber();
    }

    public int getN() {
      return records.size();
    }

    public int getLevelType() {
      return pds.getLevelType1();
    }

    public String getLevelName() {
      return tables.getTableValue("4.5", pds.getLevelType1());
    }

    public String getLevelNameShort() {
      return tables.getLevelNameShort(pds.getLevelType1());
    }

    public int getHybrid() {
      return pds.getHybridCoordinatesCount();
    }

    public final String getStatType() {
      if (pds.isInterval()) {
        Formatter f = new Formatter();
        Grib2Pds.PdsInterval pdsi = (Grib2Pds.PdsInterval) pds;
        int count = 0;
        for (Grib2Pds.TimeInterval ti : pdsi.getTimeIntervals()) {
          if (count++ > 0) f.format(", ");
          f.format("%d %s", ti.statProcessType, tables.getIntervalNameShort(ti.statProcessType));
        }
        return f.toString();
      } else return "";
    }

    public int getGDS() {
      return gr.getGDSsection().getGDS().hashCode();
    }

    /* public long getHash() {
      return gr.cdmVariableHash();
    }  */

    public long getIntvHash() {
      if (pds.isInterval()) {
        long sum = 0;
        for (Grib2RecordBean bean : records) {
          Grib2Pds.PdsInterval pdsi = (Grib2Pds.PdsInterval) bean.pds;
          sum += pdsi.getIntervalHash();
        }
        return sum;
      }
      return 0;
    }

    public String toString() {
      Formatter f = new Formatter();
      showPdsTemplate(gr.getPDSsection(), f, tables);
      return f.toString();
    }

    public String toProcessedString() {
      Formatter f = new Formatter();
      showProcessedPds(pds, discipline, f);
      return f.toString();
    }

    ///////////////

    public String getName() {
      return GribUtils.makeNameFromDescription(tables.getVariableName(gr));
    }

    public String getOldName() {
      String oldName = ucar.grib.grib2.ParameterTable.getParameterName(discipline, pds.getParameterCategory(), pds.getParameterNumber());
      boolean diff = !oldName.equalsIgnoreCase(getName());
      return diff ? "*" + oldName : oldName;
    }

    public String getUnits() {
      Grib2Tables.Parameter p = tables.getParameter(discipline, pds.getParameterCategory(), pds.getParameterNumber());
      return (p == null) ? "?" : p.getUnit();
    }

    public final String getCenter() {
      String center = CommonCodeTable.getCenterName(id.getCenter_id(), 2);
      String subcenter = CommonCodeTable.getSubCenterName(id.getCenter_id(), id.getSubcenter_id());
      return id.getCenter_id() + "/" + id.getSubcenter_id() + " (" + center + "/" + subcenter + ")";
    }

    public final String getTable() {
      return id.getMaster_table_version() + "-" + id.getLocal_table_version();
    }

    public String getEnsDerived() {
      if (pds.isEnsembleDerived()) {  // a derived ensemble must have a derivedForecastType
        Grib2Pds.PdsEnsembleDerived pdsDerived = (Grib2Pds.PdsEnsembleDerived) pds;
        int type = pdsDerived.getDerivedForecastType(); // derived type (table 4.7)
        return tables.getProbabilityNameShort(type);
      }
      return null;
    }

    public String getEnsProb() {
      // each probability interval generates a separate variable
      if (pds.isProbability()) {
        Grib2Pds.PdsProbability pdsProb = (Grib2Pds.PdsProbability) pds;
        return pdsProb.getProbabilityName();
      }
      return null;
    }

    public final boolean isEns() {
      return pds.isEnsemble();
    }
  }

  ////////////////////////////////////////////////////

  static void showBytes(Formatter f, byte[] buff) {
    for (byte b : buff) {
      int ub = (b < 0) ? b + 256 : b;
      if (b >= 32 && b < 127)
        f.format("%s", (char) ub);
      else
        f.format("(%d)", ub);
    }
  }

  static public void showCompleteGribRecord(Formatter f, String path, Grib2Record gr, Grib2Tables tables) throws IOException {
    f.format("File=%d %s %n", gr.getFile(), path);
    f.format("Header=\"");
    showBytes(f, gr.getHeader());
    f.format("\"%n%n");
    int d = gr.getDiscipline();
    f.format("Grib2IndicatorSection%n");
    f.format(" Discipline = (%d) %s%n", d, tables.getTableValue("0.0", d));
    f.format(" Length     = %d%n", gr.getIs().getMessageLength());

    Grib2SectionIdentification id = gr.getId();
    f.format("%nGrib2IdentificationSection%n");
    f.format(" Center        = (%d) %s%n", id.getCenter_id(), CommonCodeTable.getCenterName(id.getCenter_id(), 2));
    f.format(" SubCenter     = (%d) %s%n", id.getSubcenter_id(), CommonCodeTable.getSubCenterName(id.getCenter_id(), id.getSubcenter_id()));
    f.format(" Master Table  = %d%n", id.getMaster_table_version());
    f.format(" Local Table   = %d%n", id.getLocal_table_version());
    f.format(" RefTimeSignif = %d (%s)%n", id.getSignificanceOfRT(), tables.getTableValue("1.2", id.getSignificanceOfRT()));
    f.format(" RefTime       = %s%n", id.getReferenceDate());
    f.format(" RefTime Fields = %d-%d-%d %d:%d:%d%n", id.getYear(), id.getMonth(), id.getDay(), id.getHour(), id.getMinute(), id.getSecond());
    f.format(" ProductionStatus      = %d (%s)%n", id.getProductionStatus(), tables.getTableValue("1.3", id.getProductionStatus()));
    f.format(" TypeOfProcessedData   = %d (%s)%n", id.getTypeOfProcessedData(), tables.getTableValue("1.4", id.getTypeOfProcessedData()));

    if (gr.hasLocalUseSection()) {
      byte[] lus = gr.getLocalUseSection().getRawBytes();
      f.format("%nLocal Use Section (grib section 2)%n");
      /* try {
       f.format(" String= %s%n", new String(lus, 0, lus.length, "UTF-8"));
     } catch (UnsupportedEncodingException e) {
       e.printStackTrace();
     } */
      f.format("bytes (len=%d) =", lus.length);
      Misc.showBytes(lus, f);
      f.format("%n");
    }

    Grib2SectionGridDefinition gds = gr.getGDSsection();
    Grib2Gds ggds = gds.getGDS();
    f.format("%nGrib2GridDefinitionSection hash=%d crc=%d%n", ggds.hashCode(), gds.calcCRC());
    f.format(" Length             = %d%n", gds.getLength());
    f.format(" Source  (3.0)      = %d (%s) %n", gds.getSource(), tables.getTableValue("3.0", gds.getSource()));
    f.format(" Npts               = %d%n", gds.getNumberPoints());
    f.format(" Template (3.1)     = %d%n", gds.getGDSTemplateNumber());
    showGdsTemplate(gds, f, tables);

    Grib2SectionProductDefinition pdss = gr.getPDSsection();
    f.format("%nGrib2ProductDefinitionSection%n");
    Grib2Pds pds = pdss.getPDS();
    if (pds.isInterval()) {
      int[] intv = tables.getForecastTimeInterval(gr);
      if (intv != null) {
        f.format(" Start interval     = %d%n", intv[0]);
        f.format(" End   interval     = %d%n", intv[1]);
      }
    }
    showPdsTemplate(pdss, f, tables);
    if (pds.getHybridCoordinatesCount() > 0) {
      float[] coords = pds.getHybridCoordinates();
      f.format("Hybrid Coordinates (%d) %n  ", coords.length);
      for (float fc : coords) f.format("%10.5f ", fc);
      f.format("%n%n");
    }

    Grib2SectionDataRepresentation drs = gr.getDataRepresentationSection();
    f.format("%nGrib2SectionDataRepresentation%n");
    f.format("  Template           = %d (%s) %n", drs.getDataTemplate(), tables.getTableValue("5.0", drs.getDataTemplate()));
    f.format("  NPoints            = %d%n", drs.getDataPoints());

    Grib2SectionData ds = gr.getDataSection();
    f.format("%nGrib2SectionData%n");
    f.format("  Starting Pos       = %d %n", ds.getStartingPosition());
    f.format("  Data Length        = %d%n", ds.getMsgLength());
  }

  static private void showGdsTemplate(Grib2SectionGridDefinition gds, Formatter f, Grib2Tables tables) {
    int template = gds.getGDSTemplateNumber();
    byte[] raw = gds.getRawBytes();
    showRawWithTemplate("3." + template, raw, f, tables);
  }

  static private void showPdsTemplate(Grib2SectionProductDefinition pdss, Formatter f, Grib2Tables tables) {
    int template = pdss.getPDSTemplateNumber();
    byte[] raw = pdss.getRawBytes();
    showRawWithTemplate("4." + template, raw, f, tables);
  }


  static private void showRawWithTemplate(String key, byte[] raw, Formatter f, Grib2Tables tables) {
    if (gribTemplates == null)
      try {
        gribTemplates = WmoTemplateTable.getWmoStandard().map;
      } catch (IOException e) {
        f.format("Read template failed = %s%n", e.getMessage());
        return;
      }

    WmoTemplateTable gt = gribTemplates.get(key);
    if (gt == null)
      f.format("Cant find template %s%n", key);
    else
      gt.showInfo(tables, raw, f);
  }

  ////////////////////////////////////////////////////////
  private void showProcessedPds(Grib2Pds pds, int discipline, Formatter f) {
    int template = pds.getTemplateNumber();
    f.format(" Product Template = %3d %s%n", template, tables.getTableValue("4.0", template));
    f.format(" Parameter Category = %3d %s%n", pds.getParameterCategory(), tables.getTableValue("4.0" + discipline,
            pds.getParameterCategory()));
    Grib2Tables.Parameter entry = tables.getParameter(discipline, pds.getParameterCategory(), pds.getParameterNumber());
    f.format(" Parameter Name     = %3d %s %n", pds.getParameterNumber(), entry.getName());
    f.format(" Parameter Units    = %s %n", entry.getUnit());

    int tgp = pds.getGenProcessType();
    f.format(" Generating Process Type = %3d %s %n", tgp, tables.getTableValue("4.3", tgp));
    f.format(" Forecast Offset    = %3d %n", pds.getForecastTime());
    f.format(" First Surface Type = %3d %s %n", pds.getLevelType1(), tables.getLevelNameShort(pds.getLevelType1()));
    f.format(" First Surface value= %3f %n", pds.getLevelValue1());
    f.format(" Second Surface Type= %3d %s %n", pds.getLevelType2(), tables.getLevelNameShort(pds.getLevelType2()));
    f.format(" Second Surface val = %3f %n", pds.getLevelValue2());
  }

  public class Grib2RecordBean {
    Grib2Record gr;
    Grib2Pds pds;

    public Grib2RecordBean() {
    }

    public Grib2RecordBean(Grib2Record m) throws IOException {
      this.gr = m;
      //long refTime = gr.getId().getReferenceDate().getMillis();
      this.pds = gr.getPDSsection().getPDS();
    }

    public final String getRefDate() {
      return gr.getReferenceDate().toString();
    }

    public final String getForecastDate() {
      return tables.getForecastDate(gr).toString();
    }

    public String getHeader() {
      return StringUtil2.cleanup(gr.getHeader());
    }

    /* public final long getPDShash() {
      return gr.getPDSsection().calcCRC();
    }

    public final long getPdsOffset() {
      return gr.getPDSsection().getOffset();
    }

    public long getGDShash() {
      return gr.getGDSsection().calcCRC();
    } */

    public final String getTimeUnit() {
      int unit = pds.getTimeUnit();
      return tables.getTableValue("4.4", unit);
    }

    public final int getForecastTime() {
      return pds.getForecastTime();
    }

    public final int getFile() {
      return gr.getFile();
    }

    /* public String getSurfaceType() {
      return pds.getLevelType1() + "-" + pds.getLevelType2();
    } */

    public String getLevel() {
      int v1 = pds.getLevelType1();
      int v2 = pds.getLevelType2();
      if (v1 == 255) return "";
      if (v2 == 255) return "" + pds.getLevelValue1();
      if (v1 != v2) return pds.getLevelValue1() + "-" + pds.getLevelValue2() + " level2 type= " + v2;
      return pds.getLevelValue1() + "-" + pds.getLevelValue2();
    }

    /*
    TimeInterval: statProcessType= 0, timeIncrementType= 1, timeRangeUnit= 1, timeRangeLength= 744, timeIncrementUnit= 1, timeIncrement=24
    TimeInterval: statProcessType= 197, timeIncrementType= 2, timeRangeUnit= 1, timeRangeLength= 23, timeIncrementUnit= 1, timeIncrement=0
     */
    public String getTInv() {
      if (pds.isInterval()) {
        Formatter f = new Formatter();
        Grib2Pds.PdsInterval pdsi = (Grib2Pds.PdsInterval) pds;
        int count = 0;
        for (Grib2Pds.TimeInterval ti : pdsi.getTimeIntervals()) {
          if (count++ > 0) f.format(", ");
          f.format("%d-%d-%d", ti.statProcessType, ti.timeRangeLength, ti.timeIncrement);
        }
        return f.toString();
      }
      return "";
    }

    public String getIntv() {
      if (pds.isInterval()) {
        int[] intv = tables.getForecastTimeInterval(gr);
        return intv[0] + "-" + intv[1];
      }
      return "";
    }

    public long getIntvHash() {
      if (pds.isInterval()) {
        Grib2Pds.PdsInterval pdsi = (Grib2Pds.PdsInterval) pds;
        return pdsi.getIntervalHash();
      }
      return 0;
    }

    public final int getPertN() {
      int v = pds.getPerturbationNumber();
      if (v == GribNumbers.UNDEFINED) v = -1;
      return v;
    }

    public final int getNForecastsInEns() {
      int v = pds.getNumberEnsembleForecasts();
      if (v == GribNumbers.UNDEFINED) v = -1;
      return v;
    }

    public final int getPertType() {
      int v = pds.getPerturbationType();
      return (v == GribNumbers.UNDEFINED) ? -1 : v;
    }

    public final String getProbLimits() {
      double v = pds.getProbabilityLowerLimit();
      if (v == GribNumbers.UNDEFINEDD) return "";
      else return pds.getProbabilityLowerLimit() + "-" + pds.getProbabilityUpperLimit();
    }

    public long getDataPos() {
      return gr.getDataSection().getStartingPosition();
    }

    public void toRawPdsString(Formatter f) {
      byte[] bytes = gr.getPDSsection().getRawBytes();
      int count = 1;
      for (byte b : bytes) {
        short s = DataType.unsignedByteToShort(b);
        f.format(" %d : %d%n", count++, s);
      }
    }

    public String showProcessedGridRecord(Formatter f) {
      f.format("%nFile=%s (%d)%n", fileList.get(gr.getFile()).getPath(), gr.getFile());
      f.format("  Parameter=%s%n", tables.getVariableName(gr));
      f.format("  ReferenceDate=%s%n", gr.getReferenceDate());
      f.format("  ForecastDate=%s%n", tables.getForecastDate(gr));
      int[] tinv = tables.getForecastTimeInterval(gr);
      if (tinv != null)
        f.format("  TimeInterval=(%d,%d)%n", tinv[0], tinv[1]);
      f.format("%n");
      pds.show(f);
      return f.toString();
    }

    float[] readData() throws IOException {
      int fileno = gr.getFile();
      MFile mfile = fileList.get(fileno);
      ucar.unidata.io.RandomAccessFile raf = null;
      try {
        raf = new ucar.unidata.io.RandomAccessFile(mfile.getPath(), "r");
        raf.order(ucar.unidata.io.RandomAccessFile.BIG_ENDIAN);
        return gr.readData(raf);
      } finally {
        if (raf != null)
          raf.close();
      }
    }

  }

  public class Gds2Bean implements Comparable<Gds2Bean> {
    Grib2SectionGridDefinition gdss;
    Grib2Gds gds;

    // no-arg constructor

    public Gds2Bean() {
    }

    public Gds2Bean(Grib2SectionGridDefinition m) {
      this.gdss = m;
      gds = gdss.getGDS();
    }

    public int getGDShash() {
      return gds.hashCode();
    }

    public int getTemplate() {
      return gdss.getGDSTemplateNumber();
    }

    public String getGridName() {
      return tables.getTableValue("3.1", gdss.getGDSTemplateNumber());
    }

    public String getGroupName() {
      return getGridName() + "-" + getNy() + "X" + getNx();
    }

    public int getNPoints() {
      return gdss.getNumberPoints();
    }

    public int getNx() {
      return gds.nx;
    }

    public int getNy() {
      return gds.ny;
    }

    public String getScanMode() {
      return Long.toBinaryString(gds.scanMode);
    }

    @Override
    public String toString() {
      return getGridName() + " " + getTemplate() + " " + getNx() + " X " + getNy();
    }

    public void toRawGdsString(Formatter f) {
      byte[] bytes = gds.getRawBytes();
      int count = 1;
      for (byte b : bytes) {
        short s = DataType.unsignedByteToShort(b);
        f.format(" %d : %d%n", count++, s);
      }
    }

    @Override
    public int compareTo(Gds2Bean o) {
      return getGroupName().compareTo(o.getGroupName());
    }
  }
}
