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
import ucar.nc2.grib.GdsHorizCoordSys;
import ucar.nc2.grib.GribCollection;
import ucar.nc2.grib.grib1.*;
import ucar.nc2.grib.grib1.tables.Grib1Parameter;
import ucar.nc2.grib.grib1.tables.Grib1Tables;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.ui.widget.*;
import ucar.nc2.ui.widget.PopupMenu;
import ucar.nc2.util.Misc;
import ucar.unidata.geoloc.ProjectionImpl;
import ucar.unidata.io.RandomAccessFile;
import ucar.unidata.util.Parameter;
import ucar.util.prefs.PreferencesExt;
import ucar.util.prefs.ui.BeanTableSorted;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.*;
import java.util.*;
import java.util.List;

/**
 * Refactored Grib1 raw access
 *
 * @author John
 * @since 9/3/11
 */
public class Grib1CollectionPanel extends JPanel {

  private PreferencesExt prefs;

  private BeanTableSorted gds1Table, param1BeanTable, record1BeanTable;
  private JSplitPane split, split2;

  private TextHistoryPane infoPopup, infoPopup2, infoPopup3;
  private IndependentWindow infoWindow, infoWindow2, infoWindow3;
  private Grib1Tables tables = new Grib1Tables(); // default
  private FileManager fileChooser;

  public Grib1CollectionPanel(PreferencesExt prefs) {
    this.prefs = prefs;

    PopupMenu varPopup;

    param1BeanTable = new BeanTableSorted(ParameterBean.class, (PreferencesExt) prefs.node("Param1Bean"), false, "Grib1PDSVariables", "from Grib1Input.getRecords()");
    param1BeanTable.addListSelectionListener(new ListSelectionListener() {
      public void valueChanged(ListSelectionEvent e) {
        ParameterBean pb = (ParameterBean) param1BeanTable.getSelectedBean();
        if (pb != null)
          record1BeanTable.setBeans(pb.getRecordBeans());
      }
    });

    varPopup = new PopupMenu(param1BeanTable.getJTable(), "Options");
    varPopup.addAction("Show raw PDS bytes", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        ParameterBean pb = (ParameterBean) param1BeanTable.getSelectedBean();
        if (pb != null) {
          Formatter f = new Formatter();
          showRawPds(pb.pds, f);
          infoPopup2.setText(f.toString());
          infoPopup2.gotoTop();
          infoWindow2.showIfNotIconified();
        }
      }
    });
    varPopup.addAction("Show processed PDS", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        ParameterBean pb = (ParameterBean) param1BeanTable.getSelectedBean();
        if (pb != null) {
          Formatter f = new Formatter();
          showProcessedPds(pb, f);
          infoPopup3.setText(f.toString());
          infoPopup3.gotoTop();
          infoWindow3.showIfNotIconified();
        }
      }
    });

    record1BeanTable = new BeanTableSorted(RecordBean.class, (PreferencesExt) prefs.node("Record1Bean"), false, "Grib1Record", "from Grib1Input.getRecords()");
    varPopup = new PopupMenu(record1BeanTable.getJTable(), "Options");

    varPopup.addAction("Show raw PDS bytes", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        RecordBean bean = (RecordBean) record1BeanTable.getSelectedBean();
        if (bean != null) {
          Formatter f = new Formatter();
          showRawPds(bean.pds, f);
          infoPopup2.setText(f.toString());
          infoPopup2.gotoTop();
          infoWindow2.showIfNotIconified();
        }
      }
    });

    varPopup.addAction("Show Complete Grib1 Record", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        RecordBean bean = (RecordBean) record1BeanTable.getSelectedBean();
        if (bean != null) {
          Formatter f = new Formatter();
          showCompleteRecord(bean, f);
          infoPopup3.setText(f.toString());
          infoPopup3.gotoTop();
          infoWindow3.showIfNotIconified();
        }
      }
    });

    varPopup.addAction("Compare Grib1 Records", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        List list = record1BeanTable.getSelectedBeans();
        if (list.size() == 2) {
          RecordBean bean1 = (RecordBean) list.get(0);
          RecordBean bean2 = (RecordBean) list.get(1);
          Formatter f = new Formatter();
          compare(bean1, bean2, f);
          infoPopup2.setText(f.toString());
          infoPopup2.gotoTop();
          infoWindow2.showIfNotIconified();
        }
      }
    });

    varPopup.addAction("Compare Data", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        List list = record1BeanTable.getSelectedBeans();
        if (list.size() == 2) {
          RecordBean bean1 = (RecordBean) list.get(0);
          RecordBean bean2 = (RecordBean) list.get(1);
          Formatter f = new Formatter();
          compareData(bean1, bean2, f);
          infoPopup2.setText(f.toString());
          infoPopup2.gotoTop();
          infoWindow2.showIfNotIconified();
        }
      }
    });

    gds1Table = new BeanTableSorted(Gds1Bean.class, (PreferencesExt) prefs.node("Gds1Bean"), false, "Grib1GridDefinitionSection", "unique from Grib1Records");

    varPopup = new PopupMenu(gds1Table.getJTable(), "Options");
    varPopup.addAction("Show raw GDS", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        List list = gds1Table.getSelectedBeans();
        Formatter f = new Formatter();
        for (Object bo : list) {
          Gds1Bean bean = (Gds1Bean) bo;
          showRawGds(bean.gdss, f);
        }
        infoPopup.setText(f.toString());
        infoWindow.setVisible(true);
      }
    });

    varPopup.addAction("Compare GDS", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        List list = gds1Table.getSelectedBeans();
        if (list.size() == 2) {
          Gds1Bean bean1 = (Gds1Bean) list.get(0);
          Gds1Bean bean2 = (Gds1Bean) list.get(1);
          Formatter f = new Formatter();
          compare(bean1.gdss, bean2.gdss, f);
          infoPopup2.setText(f.toString());
          infoPopup2.gotoTop();
          infoWindow2.showIfNotIconified();
        }
      }
    });

    varPopup.addAction("Show GDS", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        List list = gds1Table.getSelectedBeans();
        Formatter f = new Formatter();
        for (Object bo : list) {
          Gds1Bean bean = (Gds1Bean) bo;
          showGds(bean.gdss, bean.gds, f);
        }
        infoPopup.setText(f.toString());
        infoWindow.setVisible(true);
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
    split2 = new JSplitPane(JSplitPane.VERTICAL_SPLIT, false, param1BeanTable, record1BeanTable);
    split2.setDividerLocation(prefs.getInt("splitPos2", 800));

    split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, false, split2, gds1Table);
    split.setDividerLocation(prefs.getInt("splitPos", 500));
    add(split, BorderLayout.CENTER);
  }


  ///////////////////////////////////////////////////////////////////////////

  public void save() {
    gds1Table.saveState(false);
    param1BeanTable.saveState(false);
    record1BeanTable.saveState(false);
    prefs.putBeanObject("InfoWindowBounds", infoWindow.getBounds());
    prefs.putBeanObject("InfoWindowBounds2", infoWindow2.getBounds());
    prefs.putBeanObject("InfoWindowBounds3", infoWindow3.getBounds());
    if (split != null) prefs.putInt("splitPos", split.getDividerLocation());
    if (split2 != null) prefs.putInt("splitPos2", split2.getDividerLocation());
  }

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

    Grib1CollectionBuilder.writeIndexFile(idxFile, dcm, f);
    return true;
  }

  ///////////////////////////////////////////////////////////////////////////////////////////

  private void compare(RecordBean bean1, RecordBean bean2, Formatter f) {
    String h1 = bean1.getHeader();
    String h2 = bean2.getHeader();
    if (!h1.equals(h2))
      f.format("WMO headers differ %s != %s %n", h1, h2);


    /* Grib2SectionIndicator is1 = bean1.gr.getIs();
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
    }   */

    compare(bean1.gr.getPDSsection(), bean2.gr.getPDSsection(), f);
    compare(bean1.gr.getGDSsection(), bean2.gr.getGDSsection(), f);
  }

  private void compare(Grib1SectionGridDefinition gds1, Grib1SectionGridDefinition gds2, Formatter f) {
    f.format("%nCompare Gds%n");
    byte[] raw1 = gds1.getRawBytes();
    byte[] raw2 = gds2.getRawBytes();
    Misc.compare(raw1, raw2, f);
  }

  private void compare(Grib1SectionProductDefinition pds1, Grib1SectionProductDefinition pds2, Formatter f) {
    f.format("%nCompare Pds%n");
    byte[] raw1 = pds1.getRawBytes();
    byte[] raw2 = pds2.getRawBytes();
    Misc.compare(raw1, raw2, f);
  }

  void compareData(RecordBean bean1, RecordBean bean2, Formatter f) {
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

  void showData(RecordBean bean1, Formatter f) {
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

  ///////////////////////////////////////////////////////////////////////////////////
  private String spec;
  private DatasetCollectionMFiles dcm;
  private List<MFile> fileList;

  public void setCollection(String spec) throws IOException {
    this.spec = spec;
    //this.tables = null;

    Formatter f = new Formatter();
    this.dcm = scanCollection(spec, f);
    if (dcm == null) {
      javax.swing.JOptionPane.showMessageDialog(this, "Collection is null\n" + f.toString());
      return;
    }

    Map<Integer, ParameterBean> pdsSet = new HashMap<Integer, ParameterBean>();
    Map<Integer, Grib1SectionGridDefinition> gdsSet = new HashMap<Integer, Grib1SectionGridDefinition>();

    java.util.List<ParameterBean> params = new ArrayList<ParameterBean>();
    java.util.List<Gds1Bean> gdsList = new ArrayList<Gds1Bean>();

    int fileno = 0;
    for (MFile mfile : fileList) {
      f.format("%n %s%n", mfile.getPath());
      processGribFile(mfile, fileno++, pdsSet, gdsSet, params, f);
    }
    param1BeanTable.setBeans(params);

    for (Grib1SectionGridDefinition gds : gdsSet.values())
      gdsList.add(new Gds1Bean(gds));

    Collections.sort(gdsList);
    gds1Table.setBeans(gdsList);
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

  private void processGribFile(MFile mfile, int fileno,
                               Map<Integer, ParameterBean> pdsSet,
                               Map<Integer, Grib1SectionGridDefinition> gdsSet,
                               List<ParameterBean> params, Formatter f) throws IOException {

    Grib1Index index = new Grib1Index();
    if (!index.readIndex(mfile.getPath(), mfile.getLastModified())) {
      index.makeIndex(mfile.getPath(), f);
    }

    for (Grib1SectionGridDefinition gds : index.getGds()) {
      int hash = gds.getGDS().hashCode();
      if (gdsSet.get(hash) == null)
        gdsSet.put(hash, gds);
    }

    for (Grib1Record gr : index.getRecords()) {
      gr.setFile(fileno);

      //if (tables == null) {
      //  Grib1SectionIdentification ids = gr.getId();
      //  tables = Grib2Tables.factory(ids.getCenter_id(), ids.getSubcenter_id(), ids.getMaster_table_version(), ids.getLocal_table_version());
      //}

      int id = gr.cdmVariableHash(0);
      ParameterBean bean = pdsSet.get(id);
      if (bean == null) {
        bean = new ParameterBean(gr);
        pdsSet.put(id, bean);
        params.add(bean);
      }
      bean.addRecord(gr);
    }
  }

  public void setGribFile(RandomAccessFile raf) throws IOException {
    split2 = new JSplitPane(JSplitPane.VERTICAL_SPLIT, false, param1BeanTable, record1BeanTable);
    split2.setDividerLocation(prefs.getInt("splitPos2", 800));

    split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, false, split2, gds1Table);
    split.setDividerLocation(prefs.getInt("splitPos", 500));

    removeAll();
    add(split, BorderLayout.CENTER);
    revalidate();

    Map<Long, Grib1SectionGridDefinition> gdsSet = new HashMap<Long, Grib1SectionGridDefinition>();
    Map<Integer, ParameterBean> pdsSet = new HashMap<Integer, ParameterBean>();
    java.util.List<ParameterBean> products = new ArrayList<ParameterBean>();

    raf.order(ucar.unidata.io.RandomAccessFile.BIG_ENDIAN);
    raf.seek(0);

    int count = 0;
    Grib1RecordScanner reader = new Grib1RecordScanner(raf);
    while (reader.hasNext()) {
      ucar.nc2.grib.grib1.Grib1Record gr = reader.next();
      Grib1SectionProductDefinition pds = gr.getPDSsection();
      ParameterBean bean = pdsSet.get(makeUniqueId(pds));
      if (bean == null) {
        bean = new ParameterBean(gr);
        pdsSet.put(makeUniqueId(pds), bean);
        products.add(bean);
      }
      bean.addRecord(gr);

      Grib1SectionGridDefinition gds = gr.getGDSsection();
      gdsSet.put(gds.calcCRC(), gds);
      count++;
    }
    param1BeanTable.setBeans(products);
    record1BeanTable.setBeans(new ArrayList());
    System.out.printf("GribRawPanel products = %d records = %d%n", products.size(), count);

    java.util.List<Gds1Bean> gdsList = new ArrayList<Gds1Bean>();
    for (Grib1SectionGridDefinition gds : gdsSet.values()) {
      gdsList.add(new Gds1Bean(gds));
    }
    gds1Table.setBeans(gdsList);
  }

  // see ggr.cdmVariableHash() {

  public int makeUniqueId(Grib1SectionProductDefinition pds) {
    int result = 17;
    result += result * 37 + pds.getParameterNumber();
    result *= result * 37 + pds.getLevelType();
    //if (pds.isEnsemble())
    //  result *= result * 37 + 1;
    return result;
  }

  private void showRawPds(Grib1SectionProductDefinition pds, Formatter f) {
    byte[] raw = pds.getRawBytes();
    f.format("%n");
    for (int i = 0; i < raw.length; i++) {
      f.format(" %3d : %3d%n", i + 1, raw[i]);
    }
  }

  public void showRawGds(Grib1SectionGridDefinition gds, Formatter f) {
    byte[] raw = gds.getRawBytes();
    f.format("%n");
    for (int i = 0; i < raw.length; i++) {
      f.format(" %3d : %3d%n", i + 1, raw[i]);
    }
  }

  public void showGds(Grib1SectionGridDefinition gdss, Grib1Gds gds, Formatter f) {
    f.format("Grib1SectionGridDefinition = %s", gdss);
    f.format("Grib1GDS = %s", gds);
    GdsHorizCoordSys gdsHc = gds.makeHorizCoordSys();
    f.format("%n%n%s", gdsHc);
    ProjectionImpl proj = gdsHc.proj;
    f.format("%n%nProjection %s%n", proj.getName());
    for (Parameter p : proj.getProjectionParameters())
      f.format("  %s == %s%n", p.getName(), p.getStringValue());
  }

  public void showCompleteRecord(RecordBean rbean, Formatter f) {
    f.format("Header = %s%n", new String(rbean.gr.getHeader()));
    f.format("file = %d %s%n", rbean.gr.getFile(), fileList.get(rbean.gr.getFile()).getPath());
    rbean.pds.showPds(tables, f);
    showGds(rbean.gds, rbean.gds.getGDS(), f);
  }

  public void showProcessedPds(ParameterBean pbean, Formatter f) {
    pbean.pds.showPds(tables, f);
  }

  //////////////////////////////////////////////////////////////////////////////

  public class ParameterBean {
    Grib1SectionProductDefinition pds;
    List<RecordBean> records;
    String header;
    Grib1Parameter param;

    // no-arg constructor

    public ParameterBean() {
    }

    public ParameterBean(Grib1Record r) {
      pds = r.getPDSsection();
      header = new String(r.getHeader());
      records = new ArrayList<RecordBean>();
      param = tables.getParameter(pds.getCenter(), pds.getSubCenter(), pds.getTableVersion(), pds.getParameterNumber());
    }

    void addRecord(Grib1Record r) {
      records.add(new RecordBean(r));
    }

    List<RecordBean> getRecordBeans() {
      return records;
    }

    public String getTableVersion() {
      return pds.getCenter() + "-" + pds.getSubCenter() + "-" + pds.getTableVersion();
    }

    public final CalendarDate getReferenceDate() {
      return pds.getReferenceDate();
    }

    public int getParamNo() {
      return pds.getParameterNumber();
    }

    public final int getLevelType() {
      return pds.getLevelType();
    }

    public String getParamDesc() {
      return (param == null) ? null : param.getDescription();
    }

    public String getName() {
      if (param == null) return null;
      return Grib1Utils.makeVariableName(tables, pds.getCenter(), pds.getSubCenter(), pds.getTableVersion(), pds.getParameterNumber(),
              pds.getLevelType(), pds.getTimeRangeIndicator());
    }

    public String getUnit() {
      return (param == null) ? null : param.getUnit();
    }

    public final String getLevelName() {
      Grib1ParamLevel plevel = pds.getParamLevel();
      return tables.getLevelNameShort(plevel.getLevelType());
    }

    public int getN() {
      return records.size();
    }

  }

  public class RecordBean {
    Grib1Record gr;
    Grib1SectionGridDefinition gds;
    Grib1SectionProductDefinition pds;
    Grib1ParamLevel plevel;
    Grib1ParamTime ptime;

    // no-arg constructor

    public RecordBean() {
    }

    public RecordBean(Grib1Record m) {
      this.gr = m;
      gds = gr.getGDSsection();
      pds = gr.getPDSsection();
      plevel = pds.getParamLevel();
      ptime = pds.getParamTime();
    }


    public String getHeader() {
      return new String(gr.getHeader()).trim();
    }

    public String getPeriod() {
      return Grib1ParamTime.getCalendarPeriod(pds.getTimeUnit()).toString();
    }

    public String getTimeTypeName() {
      return ptime.getTimeTypeName();
    }

    public CalendarDate getReferenceDate() {
      return pds.getReferenceDate();
    }

    public int getTimeValue1() {
      return pds.getTimeValue1();
    }

    public int getTimeValue2() {
      return pds.getTimeValue2();
    }

    public int getTimeType() {
      return pds.getTimeRangeIndicator();
    }

    public String getTimeCoord() {
      if (ptime.isInterval()) {
        int[] intv = ptime.getInterval();
        return intv[0] + "-" + intv[1] + "("+ptime.getIntervalSize()+")";
      }
      return Integer.toString(ptime.getForecastTime());
    }

    public int getNIncluded() {
      return pds.getNincluded();
    }

    public int getNMissing() {
      return pds.getNmissing();
    }

    public int getLevelType() {
      return plevel.getLevelType();
    }

    public String getLevel() {
      if (plevel.isLayer()) {
        return plevel.getValue1() + "-" + plevel.getValue2();
      }
      return Float.toString(plevel.getValue1());
    }

    public long getPos() {
      return gr.getDataSection().getStartingPosition();
    }

    public final int getFile() {
      return gr.getFile();
    }

    public final String getStatType() {
      Grib1ParamTime.StatType stype = ptime.getStatType();
      return (stype == null) ? null : stype.name();
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

  ////////////////////////////////////////////////////////////////////////////

  public class Gds1Bean implements Comparable<Gds1Bean> {
    Grib1SectionGridDefinition gdss;
    Grib1Gds gds;
    // no-arg constructor

    public Gds1Bean() {
    }

    public Gds1Bean(Grib1SectionGridDefinition m) {
      this.gdss = m;
      gds = gdss.getGDS();
    }

    public long getCRC() {
      return gdss.calcCRC();
    }

    public int getGridNo() {
      return gdss.getGridTemplate();
    }

    public boolean isVertCoords() {
      return gdss.hasVerticalCoordinateParameters();
    }

    public String getGridName() {
      return gds.getNameShort();
    }

    public String getScanMode() {
      return Long.toBinaryString(gds.getScanMode());
    }

    public String getResolution() {
      return Long.toBinaryString(gds.getResolution());
    }

    public double getDx() {
      return gds.getDx();
    }

    public double getDy() {
      return gds.getDy();
    }

    public double getDxRaw() {
      return gds.getDxRaw();
    }

    public double getDyRaw() {
      return gds.getDyRaw();
    }

    public int getNx() {
      return gds.getNx();
    }

    public int getNy() {
      return gds.getNy();
    }

    public int getNxRaw() {
      return gds.getNxRaw();
    }

    public int getNyRaw() {
      return gds.getNyRaw();
    }

    @Override
    public int compareTo(Gds1Bean o) {
      return getGridName().compareTo(o.getGridName());
    }
  }
}
