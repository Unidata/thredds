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

package ucar.nc2.ui.grib;

import thredds.featurecollection.FeatureCollectionConfig;
import thredds.inventory.CollectionAbstract;
import thredds.inventory.MCollection;
import thredds.inventory.MFile;
import ucar.ma2.DataType;
import ucar.nc2.grib.*;
import ucar.nc2.grib.grib2.*;
import ucar.nc2.grib.grib2.table.Grib2Customizer;
import ucar.nc2.grib.grib2.table.NcepLocalTables;
import ucar.nc2.grib.grib2.table.WmoTemplateTable;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.ui.widget.*;
import ucar.nc2.ui.widget.PopupMenu;
import ucar.nc2.util.Misc;
import ucar.nc2.wmo.CommonCodeTable;
import ucar.unidata.geoloc.LatLonPoint;
import ucar.util.prefs.PreferencesExt;
import ucar.util.prefs.ui.BeanTable;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.beans.SimpleBeanInfo;
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
  static private final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(Grib2CollectionPanel.class);

  private final PreferencesExt prefs;

  private BeanTable param2BeanTable, record2BeanTable, gds2Table;
  private JSplitPane split, split2;

  private TextHistoryPane infoPopup, infoPopup2, infoPopup3;
  private IndependentWindow infoWindow, infoWindow2, infoWindow3;
  private FileManager fileChooser;

  public Grib2CollectionPanel(final PreferencesExt prefs, JPanel buttPanel) {
    this.prefs = prefs;

    PopupMenu varPopup;

    AbstractButton xmlButt = BAMutil.makeButtcon("Information", "generate gds xml", false);
    xmlButt.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        Formatter f = new Formatter();
        generateGdsXml(f);
        infoPopup2.setText(f.toString());
        infoPopup2.gotoTop();
        infoWindow2.show();
      }
    });
    buttPanel.add(xmlButt);

    ////////////////
    param2BeanTable = new BeanTable(Grib2ParameterBean.class, (PreferencesExt) prefs.node("Param2Bean"), false,
            "Grib2PDSVariables", "from Grib2Input.getRecords()", null);
    param2BeanTable.addListSelectionListener(new ListSelectionListener() {
      public void valueChanged(ListSelectionEvent e) {
        Grib2ParameterBean pb = (Grib2ParameterBean) param2BeanTable.getSelectedBean();
        if (pb != null) {
          makeRecordTable(pb.pds);
          record2BeanTable.setBeans(pb.getRecordBeans());
        }
      }
    });

    varPopup = new ucar.nc2.ui.widget.PopupMenu(param2BeanTable.getJTable(), "Options");
    varPopup.addAction("Show PDS", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        Grib2ParameterBean pb = (Grib2ParameterBean) param2BeanTable.getSelectedBean();
        if (pb != null) {
          Formatter f = new Formatter();
          showPdsTemplate(pb.gr.getPDSsection(), f, cust);
          infoPopup2.setText(f.toString());
          infoPopup2.gotoTop();
          infoWindow2.show();
        }
      }
    });
    varPopup.addAction("Show processed PDS", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        Grib2ParameterBean pb = (Grib2ParameterBean) param2BeanTable.getSelectedBean();
        if (pb != null) {
          infoPopup3.setText(pb.toProcessedString());
          infoPopup3.gotoTop();
          infoWindow3.show();
        }
      }
    });

    varPopup.addAction("Compare PDS", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        List list = param2BeanTable.getSelectedBeans();
        if (list.size() == 2) {
          Grib2ParameterBean bean1 = (Grib2ParameterBean) list.get(0);
          Grib2ParameterBean bean2 = (Grib2ParameterBean) list.get(1);
          Formatter f = new Formatter();
          compare(bean1.gr.getPDSsection(), bean2.gr.getPDSsection(), f);
          infoPopup2.setText(f.toString());
          infoPopup2.gotoTop();
          infoWindow2.show();
        }
      }
    });

    varPopup.addAction("Extract GribRecords to File", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        List<Grib2RecordBean> allRecords = new ArrayList<>();
        List list = param2BeanTable.getSelectedBeans();
        for (Object bean : list) {
          Grib2ParameterBean param = (Grib2ParameterBean) bean;
          allRecords.addAll( param.records);
        }
        if (allRecords.size() > 0)
          writeToFile(allRecords);
      }
    });

    Class useClass = Grib2RecordBean.class;
    record2BeanTable = new BeanTable(useClass, (PreferencesExt) prefs.node(useClass.getName()), false,
            useClass.getName(), "from Grib2Input.getRecords()", null);

    gds2Table = new BeanTable(Gds2Bean.class, (PreferencesExt) prefs.node("Gds2Bean"), false,
            "Grib2GridDefinitionSection", "unique from Grib2Records", null);
    varPopup = new PopupMenu(gds2Table.getJTable(), "Options");

    varPopup.addAction("Show GDS", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        Formatter f = new Formatter();
        for (Object o : gds2Table.getSelectedBeans()) {
          Gds2Bean bean = (Gds2Bean) o;
          Grib2Gds ggds = bean.gdss.getGDS();
          f.format("GDS hash=%d crc=%d%n", ggds.hashCode(), bean.gdss.calcCRC());
          showGdsTemplate(bean.gdss, f, cust);
        }
        infoPopup2.setText(f.toString());
        infoPopup2.gotoTop();
        infoWindow2.show();
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
          infoPopup3.setText(f.toString());
          infoPopup3.gotoTop();
          infoWindow3.show();
        }
      }
    });

    varPopup.addAction("Show raw GDS bytes", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        Formatter f = new Formatter();
        List list = gds2Table.getSelectedBeans();
        for (Object aList : list) {
          Gds2Bean bean = (Gds2Bean) aList;
          bean.toRawGdsString(f);
        }
        infoPopup.setText(f.toString());
        infoPopup.gotoTop();
        infoWindow.show();
      }
    });

    varPopup.addAction("Show Files that use this GDS", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        Gds2Bean want = (Gds2Bean) gds2Table.getSelectedBean();
        if (want == null) return;
        SortedSet<Integer> files = new TreeSet<>();

        for (Object o : param2BeanTable.getBeans()) {
          Grib2ParameterBean p = (Grib2ParameterBean) o;
          if (p.getGDS() == want.getGDShash()) {
            for (Grib2RecordBean r : p.getRecordBeans())
              files.add(r.gr.getFile());
          }
        }

        Formatter f = new Formatter();
        for (Integer fileno : files) {
          f.format(" %d = %s%n", fileno, fileList.get(fileno).getPath());
        }
        infoPopup2.setText(f.toString());
        infoPopup2.gotoTop();
        infoWindow2.show();
      }
    });

    varPopup.addAction("Restrict to this GDS", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        Gds2Bean want = (Gds2Bean) gds2Table.getSelectedBean();
        if (want == null) return;
        java.util.List<Grib2ParameterBean> params = new ArrayList<>();
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
        infoWindow2.show();
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

  void makeRecordTable(Grib2Pds pds) {
    if (record2BeanTable != null)  record2BeanTable.saveState(false);

    BeanInfo info = new PdsBeanInfo(pds);

    String prefsName = pds.getClass().getName();
    record2BeanTable = new BeanTable(Grib2RecordBean.class, (PreferencesExt) prefs.node(prefsName), prefsName, "from Grib2Input.getRecords()", info);
    PopupMenu varPopup = new PopupMenu(record2BeanTable.getJTable(), "Options");

    varPopup.addAction("Show complete GridRecord", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        Grib2RecordBean bean = (Grib2RecordBean) record2BeanTable.getSelectedBean();
        if (bean != null) {
          Formatter f = new Formatter();
          try {
            showCompleteGribRecord(f, fileList.get(bean.gr.getFile()).getPath(), bean.gr, cust);
          } catch (IOException ioe) {
            StringWriter sw = new StringWriter(10000);
            ioe.printStackTrace(new PrintWriter(sw));
            f.format("%s", sw.toString());
          }
          infoPopup.setText(f.toString());
          infoPopup.gotoTop();
          infoWindow.show();
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
        infoWindow2.show();
      }
    });

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
          infoWindow2.show();
        }
      }
    });

    varPopup.addAction("Show raw PDS bytes", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        Formatter f = new Formatter();
        List list = record2BeanTable.getSelectedBeans();
        for (Object aList : list) {
          Grib2RecordBean bean = (Grib2RecordBean) aList;
          bean.toRawPdsString(f);
        }
        infoPopup.setText(f.toString());
        infoPopup.gotoTop();
        infoWindow.show();
      }
    });

    varPopup.addAction("Show raw Local Use Section bytes", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        Formatter f = new Formatter();
        List list = record2BeanTable.getSelectedBeans();
        for (Object aList : list) {
          Grib2RecordBean bean = (Grib2RecordBean) aList;
          bean.toRawLUString(f);
        }
        infoPopup.setText(f.toString());
        infoPopup.gotoTop();
        infoWindow.show();
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
          infoWindow2.show();
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
          infoWindow2.show();
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

    varPopup.addAction("Consistency checks on GribRecord", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        Grib2RecordBean bean = (Grib2RecordBean) record2BeanTable.getSelectedBean();
        if (bean != null) {
          Formatter f = new Formatter();
          check(bean, f);
          infoPopup2.setText(f.toString());
          infoPopup2.gotoTop();
          infoWindow2.show();
        }
      }
    });

    if (split2 != null) {
      int d = split2.getDividerLocation();
      split2.setRightComponent(record2BeanTable);
      split2.setDividerLocation(d);
    }
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

  public void closeOpenFiles() throws IOException {
    param2BeanTable.clearBeans();
    record2BeanTable.clearBeans();
    gds2Table.clearBeans();
  }

  ///////////////////////////////////////////////

  private String spec;
  private MCollection dcm;
  private List<MFile> fileList;
  private Grib2Customizer cust;
  // private Grib2Rectilyser rect2;

  public void generateGdsXml(Formatter f) {
    f.format("<gribConfig>%n");
    List<Object> gdss = new ArrayList<Object>(gds2Table.getBeans());
    Collections.sort(gdss, new Comparator<Object>() {
      public int compare(Object o1, Object o2) {
        int h1 = ((Gds2Bean) o1).gds.hashCode();
        int h2 =  ((Gds2Bean) o2).gds.hashCode();
        if (h1 < h2) return -1;
        else if (h1 == h2) return 0;
        else return 1;
      }
    });

    for (Object bean : gdss) {
      Gds2Bean gbean = (Gds2Bean)bean;
      f.format("  <gdsName hash='%d' groupName='%s'/>%n", gbean.gds.hashCode(), gbean.getGroupName());
    }
    f.format("</gribConfig>%n");
  }


  public void setCollection(String spec) throws IOException {
    this.spec = spec;
    this.cust = null;

    Formatter f = new Formatter();
    this.dcm = getCollection(spec, f);
    if (dcm == null) {
      javax.swing.JOptionPane.showMessageDialog(this, "Collection is null\n" + f.toString());
      return;
    }

    Map<Grib2Variable, Grib2ParameterBean> pdsSet = new HashMap<>();
    Map<Integer, Grib2SectionGridDefinition> gdsSet = new HashMap<>();

    java.util.List<Grib2ParameterBean> params = new ArrayList<>();
    java.util.List<Gds2Bean> gdsList = new ArrayList<>();

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
                               Map<Grib2Variable, Grib2ParameterBean> pdsSet,
                               Map<Integer, Grib2SectionGridDefinition> gdsSet,
                               List<Grib2ParameterBean> params, Formatter f) throws IOException {

    Grib2Index index = new Grib2Index();
    if (!index.readIndex(mfile.getPath(), mfile.getLastModified())) {
      index.makeIndex(mfile.getPath(), null);
    }

    for (Grib2SectionGridDefinition gds : index.getGds()) {
      int hash = gds.getGDS().hashCode();
      if (gdsSet.get(hash) == null)
        gdsSet.put(hash, gds);
    }

    for (Grib2Record gr : index.getRecords()) {
      gr.setFile(fileno);

      if (cust == null)
        cust = Grib2Customizer.factory(gr);

      Grib2Variable gv = new Grib2Variable(cust, gr, 0, FeatureCollectionConfig.intvMergeDef, FeatureCollectionConfig.useGenTypeDef);
      Grib2ParameterBean bean = pdsSet.get(gv);
      if (bean == null) {
        bean = new Grib2ParameterBean(gr, gv);
        pdsSet.put(gv, bean);
        params.add(bean);
      }
      bean.addRecord(gr);
    }
  }

  private MCollection getCollection(String spec, Formatter f) {
    MCollection dc = null;
    try {
      dc = CollectionAbstract.open("Grib2CollectionPanel", spec, null, f);
      fileList = (List<MFile>) Misc.getList(dc.getFilesSorted());
      return dc;

    } catch (Exception e) {
      StringWriter sw = new StringWriter(10000);
      e.printStackTrace(new PrintWriter(sw));
      f.format("%s", sw.toString());
      if (dc != null) dc.close();
      return null;
    }
  }

  public boolean writeIndex(Formatter f) throws IOException {
    return false;
    /* if (fileChooser == null)
      fileChooser = new FileManager(null, null, null, (PreferencesExt) prefs.node("FileManager"));

    MCollection dcm = getCollection(spec, f);
    String name = dcm.getCollectionName();
    int pos = name.lastIndexOf('/');
    if (pos < 0) pos = name.lastIndexOf('\\');
    if (pos > 0) name = name.substring(pos + 1);
    File def = new File(dcm.getRoot(), name + CollectionAbstract.NCX_SUFFIX);

    String filename = fileChooser.chooseFilename(def);
    if (filename == null) return false;
    if (!filename.endsWith(CollectionAbstract.NCX_SUFFIX))
      filename += CollectionAbstract.NCX_SUFFIX;
    File idxFile = new File(filename);

    FeatureCollectionConfig config = new FeatureCollectionConfig("ds093.1", "test", FeatureCollectionType.GRIB2,
            this.spec, null, null, null, null, null);
    config.gribConfig.unionRuntimeCoord = true;

    org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger("test");
    boolean changed = GribCdmIndex.updateGribCollection(config, CollectionUpdateType.always, logger);
    System.out.printf("changed = %s%n", changed);

    boolean ok = GribCdmIndex.updateGribCollection(false, dcm, CollectionUpdateType.always, FeatureCollectionConfig.PartitionType.directory,
       logger, f);


    return ok;  */
  }

  public void showCollection(Formatter f) {
    if (dcm == null) {
      if (spec == null) return;
      dcm = getCollection(spec, f);
      if (dcm == null) return;
    }

    // just a list of the files
    f.format("dcm = %s%n", dcm);
    try {
      for (MFile mfile : dcm.getFilesSorted()) {
        f.format("  %s%n", mfile.getPath());
      }
    } catch (IOException e) {
      e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
    }

    // divided by group
    Map<Integer, Set<Integer>> groups = new HashMap<>();
    for (Object o : param2BeanTable.getBeans()) {
      Grib2ParameterBean p = (Grib2ParameterBean) o;
      Set<Integer> group = groups.get(p.getGDS());
      if (group == null) {
        group = new TreeSet<>();
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
      for (Integer fileno : group) {
        f.format(" %d = %s%n", fileno, fileList.get(fileno).getPath());
      }
      f.format("%n");
    }
  }

  public void showGDSuse(Formatter f) {

    Map<Integer, Gds2Bean> gdsMap = new HashMap<>();
    Map<Integer, Set<Integer>> fileMap = new HashMap<>();
    List<Gds2Bean> beans = gds2Table.getBeans();
    for (Gds2Bean gdsBean : beans) {
      fileMap.put(gdsBean.getGDShash(), new TreeSet<Integer>());
      gdsMap.put(gdsBean.getGDShash(), gdsBean);
      f.format("<gdsName hash='%d' groupName='%s'/>%n", gdsBean.getGDShash(), gdsBean.getGroupName());
    }
    f.format("%n");

    for (Object o : param2BeanTable.getBeans()) {
      Grib2ParameterBean p = (Grib2ParameterBean) o;
      Set<Integer> files = fileMap.get(p.getGDS());
      for (Grib2RecordBean r : p.getRecordBeans())
        files.add(r.gr.getFile());
    }

    for (Map.Entry<Integer, Set<Integer>> ent : fileMap.entrySet()) {
      Gds2Bean gds = gdsMap.get(ent.getKey());
      Set<Integer> files = ent.getValue();
      Iterator<Integer> iter = files.iterator();
      f.format("%nGDS %d == %s%n", ent.getKey(), gds);
      while (iter.hasNext()) {
        int fileno = iter.next();
        f.format(" %3d = %s%n", fileno, fileList.get(fileno).getPath());
      }
    }

  }

  public void checkProblems(Formatter f) {
    checkDuplicates(f);
    checkLocalParams(f);
  }

  public void check(Grib2RecordBean bean, Formatter f) {
    int fileno = bean.gr.getFile();
    MFile mfile = fileList.get(fileno);
    try (ucar.unidata.io.RandomAccessFile raf = new ucar.unidata.io.RandomAccessFile(mfile.getPath(), "r")) {
      bean.gr.check(raf, f);

    } catch (IOException ioe) {
       StringWriter sw = new StringWriter(10000);
       ioe.printStackTrace(new PrintWriter(sw));
       f.format("%n%s%n", sw.toString());
    }
    f.format("%ndone%n");
  }

  private static class DateCount implements Comparable<DateCount> {
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
    Set<Long> pdsMap = new HashSet<>();
    int dups = 0;
    int count = 0;

    // do all records have the same runtime ?
    Map<CalendarDate, DateCount> dateMap = new HashMap<>();

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

    List<DateCount> dcList = new ArrayList<>(dateMap.values());
    Collections.sort(dcList);

    f.format("Run Dates%n");
    int total = 0;
    for (DateCount dc : dcList) {
      f.format(" %s == %d%n", dc.d, dc.count);
      total += dc.count;
    }
    f.format("total records = %d%n", total);
  }

  private void checkLocalParams(Formatter f) {

    f.format("%nLocal Parameters%n");
    List<Grib2ParameterBean> params = param2BeanTable.getBeans();
    for (Grib2ParameterBean pbean : params) {
      GribTables.Parameter p = pbean.getParameter();
      if (p == null)
        f.format("   null parameter for %s%n", pbean);
      else if (Grib2Customizer.isLocal(p))
        f.format("   %s%n", p);
    }
  }

  private void writeToFile(List<Grib2RecordBean> beans) {

    if (fileChooser == null)
      fileChooser = new FileManager(null, null, null, (PreferencesExt) prefs.node("FileManager"));

    FileOutputStream fos = null;
    RandomAccessFile raf = null;

    try {
      String filename = null;
      boolean append = false;
      int n = 0;
      MFile curr = null;

      for (Grib2RecordBean bean : beans) {
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

  static public  void compare(Grib2SectionGridDefinition gdss1, Grib2SectionGridDefinition gdss2, Formatter f) {
    f.format("1 GribGDS hash = %s%n", gdss1.getGDS().hashCode());
    f.format("2 GribGDS hash = %s%n", gdss2.getGDS().hashCode());

    f.format("%nCompare Gds%n");
    byte[] raw1 = gdss1.getRawBytes();
    byte[] raw2 = gdss2.getRawBytes();
    Misc.compare(raw1, raw2, f);

    Grib2Gds gds1 = gdss1.getGDS();
    Grib2Gds gds2 = gdss2.getGDS();
    GdsHorizCoordSys gdsh1 = gds1.makeHorizCoordSys();
    GdsHorizCoordSys gdsh2 = gds2.makeHorizCoordSys();

    f.format("%ncompare gds1 - gds22%n");
    f.format(" Start x diff : %f%n", gdsh1.getStartX() - gdsh2.getStartX());
    f.format(" Start y diff : %f%n", gdsh1.getStartY() - gdsh2.getStartY());
    f.format(" End x diff : %f%n", gdsh1.getEndX() - gdsh2.getEndX());
    f.format(" End y diff : %f%n", gdsh1.getEndY() - gdsh2.getEndY());

    LatLonPoint pt1 = gdsh1.getCenterLatLon();
    LatLonPoint pt2 = gdsh2.getCenterLatLon();
    f.format(" Center lon diff : %f%n", pt1.getLongitude() - pt2.getLongitude());
    f.format(" Center lat diff : %f%n", pt1.getLatitude() - pt2.getLatitude());
  }


  static public void compare(Grib2SectionProductDefinition pds1, Grib2SectionProductDefinition pds2, Formatter f) {
    f.format("%nCompare Pds%n");
    byte[] raw1 = pds1.getRawBytes();
    byte[] raw2 = pds2.getRawBytes();
    Misc.compare(raw1, raw2, f);
  }

  static public void compareData(Grib2RecordBean bean1, Grib2RecordBean bean2, Formatter f) {
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

  static public void showData(Grib2RecordBean bean1, Formatter f) {
    float[] data;
    try {
      data = bean1.readData();
    } catch (Exception e) {
      StringWriter sw = new StringWriter(5000);
      e.printStackTrace(new PrintWriter(sw));
      f.format("Exception %s", sw.toString());
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

    /////////////////////////////////////////////////////////

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
      return cust.getTableValue("3.1", gdss.getGDSTemplateNumber());
    }

    public String getGroupName() {
      return getGridName() + "-" + getNy() + "X" + getNx();
    }

    public int getNPoints() {
      return gdss.getNumberPoints();
    }

    public int getNx() {
      return gds.getNx();
    }

    public int getNy() {
      return gds.getNy();
    }

    public String getScanMode() {
      int scanMode = gds.getScanMode();
      Formatter f = new Formatter();
      f.format("0x%s=", Long.toHexString(scanMode));
      if (!GribUtils.scanModeXisPositive(scanMode)) f.format(" Xneg");
      if (GribUtils.scanModeYisPositive(scanMode)) f.format(" Ypos");
      if (!GribUtils.scanModeXisConsecutive(scanMode)) f.format(" !XisConsecutive");
      if (!GribUtils.scanModeSameDirection(scanMode)) f.format(" !SameDirection");
      return f.toString();
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

  ////////////////////////////////////////////////////////////////////////////

  public class Grib2ParameterBean {
    Grib2Record gr;
    Grib2SectionIdentification id;
    Grib2Pds pds;
    List<Grib2RecordBean> records;
    int discipline;
    Grib2Variable gv;

    // no-arg constructor

    public Grib2ParameterBean() {
    }

    public Grib2ParameterBean(Grib2Record r, Grib2Variable gv) throws IOException {
      this.gr = r;
      this.gv = gv;

      // long refTime = r.getId().getReferenceDate().getMillis();
      pds = r.getPDS();
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

    GribTables.Parameter getParameter() {
      return cust.getParameter(gr.getDiscipline(), gr.getPDS().getParameterCategory(), gr.getPDS().getParameterNumber());
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

    public int getGenProcess() {
      return pds.getGenProcessId();
    }

    public String getLevelName() {
      return cust.getLevelNameShort(pds.getLevelType1());
    }

    public int getNExtra() {
      return pds.getExtraCoordinatesCount();
    }

    public boolean isLayer() {
      return cust.isLayer(pds);
    }

    public final String getStatType() {
      if (pds.isTimeInterval()) {
        Formatter f = new Formatter();
        Grib2Pds.PdsInterval pdsi = (Grib2Pds.PdsInterval) pds;
        int count = 0;
        for (Grib2Pds.TimeInterval ti : pdsi.getTimeIntervals()) {
          if (count++ > 0) f.format(", ");
          f.format("%d %s", ti.statProcessType, cust.getStatisticNameShort(ti.statProcessType));
        }
        return f.toString();
      } else return "";
    }

    public int getGDS() {
      return gr.getGDSsection().getGDS().hashCode();
    }

   public String getCdmHash() {
     return Integer.toHexString(gv.hashCode());
   }

    public double getIntvHours() {
      if (pds.isTimeInterval()) {
        return cust.getForecastTimeIntervalSizeInHours(pds); // LOOK using an Hour here, but will need to make this configurable
     }

      return -1;
    }

    /*public long getIntvHash() {
      if (pds.isTimeInterval()) {
        long sum = 0;
        for (Grib2RecordBean bean : records) {
          Grib2Pds.PdsInterval pdsi = (Grib2Pds.PdsInterval) bean.pds;
          sum += pdsi.getIntervalHash();
        }
        return sum;
      }
      return 0;
    }  */

    public String toString() {
      Formatter f = new Formatter();
      showPdsTemplate(gr.getPDSsection(), f, cust);
      return f.toString();
    }

    public String toProcessedString() {
      Formatter f = new Formatter();
      showProcessedPds(cust, pds, discipline, f);
      return f.toString();
    }

    ///////////////

    public String getName() {
      return GribUtils.makeNameFromDescription(cust.getVariableName(gr));
    }

   /*  public String getOldName() {
      String oldName = ucar.grib.grib2.ParameterTable.getParameterName(discipline, pds.getParameterCategory(), pds.getParameterNumber());
      boolean diff = !oldName.equalsIgnoreCase(getName());
      return diff ? "*" + oldName : oldName;
    } */

    public String getUnits() {
      Grib2Customizer.Parameter p = cust.getParameter(discipline, pds.getParameterCategory(), pds.getParameterNumber());
      return (p == null) ? "?" : p.getUnit();
    }

    public final String getCenter() {
      //String center = CommonCodeTable.getCenterName(id.getCenter_id(), 2);
      //String subcenter = cust.getSubCenterName(id.getCenter_id(), id.getSubcenter_id());
      return id.getCenter_id() + "/" + id.getSubcenter_id(); // + " (" + center + "/" + subcenter + ")";
    }

    public final String getTable() {
      return id.getMaster_table_version() + "-" + id.getLocal_table_version();
    }

    public String getEnsDerived() {
      if (pds.isEnsembleDerived()) {  // a derived ensemble must have a derivedForecastType
        Grib2Pds.PdsEnsembleDerived pdsDerived = (Grib2Pds.PdsEnsembleDerived) pds;
        int type = pdsDerived.getDerivedForecastType(); // derived type (table 4.7)
        return cust.getProbabilityNameShort(type);
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

  static void showBytes(Formatter f, byte[] buff, int max) {
    int count = 0;
    for (byte b : buff) {
      int ub = (b < 0) ? b + 256 : b;
      if (b >= 32 && b < 127)
        f.format("%s", (char) ub);
      else
        f.format("(%d)", ub);
      if (max > 0 && count++ > max) break;
    }
  }

  static public void showCompleteGribRecord(Formatter f, String path, Grib2Record gr, Grib2Customizer cust) throws IOException {
    f.format("File=%d %s %n", gr.getFile(), path);
    f.format("Header=\"");
    showBytes(f, gr.getHeader(), 100);
    f.format("\"%n");

    Grib2Variable gv = new Grib2Variable(cust, gr, 0, FeatureCollectionConfig.intvMergeDef, FeatureCollectionConfig.useGenTypeDef);
    f.format("cdmHash=%d%n", gv.hashCode());

    int d = gr.getDiscipline();
    f.format("Grib2IndicatorSection%n");
    f.format(" Discipline = (%d) %s%n", d, cust.getTableValue("0.0", d));
    f.format(" Length     = %d%n", gr.getIs().getMessageLength());

    Grib2SectionIdentification id = gr.getId();
    f.format("%nGrib2IdentificationSection%n");
    f.format(" Center        = (%d) %s%n", id.getCenter_id(), CommonCodeTable.getCenterName(id.getCenter_id(), 2));
    f.format(" SubCenter     = (%d) %s%n", id.getSubcenter_id(), cust.getSubCenterName(id.getCenter_id(), id.getSubcenter_id()));
    f.format(" Master Table  = %d%n", id.getMaster_table_version());
    f.format(" Local Table   = %d%n", id.getLocal_table_version());
    f.format(" RefTimeSignif = %d (%s)%n", id.getSignificanceOfRT(), cust.getTableValue("1.2", id.getSignificanceOfRT()));
    f.format(" RefTime       = %s%n", id.getReferenceDate());
    f.format(" RefTime Fields = %d-%d-%d %d:%d:%d%n", id.getYear(), id.getMonth(), id.getDay(), id.getHour(), id.getMinute(), id.getSecond());
    f.format(" ProductionStatus      = %d (%s)%n", id.getProductionStatus(), cust.getTableValue("1.3", id.getProductionStatus()));
    f.format(" TypeOfProcessedData   = %d (%s)%n", id.getTypeOfProcessedData(), cust.getTableValue("1.4", id.getTypeOfProcessedData()));

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
    f.format(" Source  (3.0)      = %d (%s) %n", gds.getSource(), cust.getTableValue("3.0", gds.getSource()));
    f.format(" Npts               = %d%n", gds.getNumberPoints());
    f.format(" Template (3.1)     = %d%n", gds.getGDSTemplateNumber());
    showGdsTemplate(gds, f, cust);

    Grib2SectionProductDefinition pdss = gr.getPDSsection();
    f.format("%nGrib2ProductDefinitionSection%n");
    Grib2Pds pds = gr.getPDS();
    if (pds.isTimeInterval()) {
      TimeCoord.TinvDate intv = cust.getForecastTimeInterval(gr);
      if (intv != null) f.format(" Interval     = %s%n", intv);
    }
    showPdsTemplate(pdss, f, cust);
    if (pds.getExtraCoordinatesCount() > 0) {
      float[] coords = pds.getExtraCoordinates();
      if (coords != null) {
        f.format("Hybrid Coordinates (%d) %n  ", coords.length);
        for (float fc : coords) f.format("%10.5f ", fc);
      }
      f.format("%n%n");
    }

    Grib2SectionDataRepresentation drs = gr.getDataRepresentationSection();
    f.format("%nGrib2SectionDataRepresentation%n");
    f.format("  Template           = %d (%s) %n", drs.getDataTemplate(), cust.getTableValue("5.0", drs.getDataTemplate()));
    f.format("  NPoints            = %d%n", drs.getDataPoints());

    Grib2SectionData ds = gr.getDataSection();
    f.format("%nGrib2SectionData%n");
    f.format("  Starting Pos       = %d %n", ds.getStartingPosition());
    f.format("  Data Length        = %d%n", ds.getMsgLength());
  }

  static public void showGdsTemplate(Grib2SectionGridDefinition gds, Formatter f, Grib2Customizer cust) {
    int template = gds.getGDSTemplateNumber();
    byte[] raw = gds.getRawBytes();
    showRawWithTemplate("3." + template, raw, f, cust);
  }

  static public void showPdsTemplate(Grib2SectionProductDefinition pdss, Formatter f, Grib2Customizer cust) {
    int template = pdss.getPDSTemplateNumber();
    byte[] raw = pdss.getRawBytes();
    showRawWithTemplate("4." + template, raw, f, cust);
  }

  static private void showRawWithTemplate(String key, byte[] raw, Formatter f, Grib2Customizer cust) {
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
      gt.showInfo(cust, raw, f);
  }

  static public void showProcessedPds(Grib2Customizer cust, Grib2Pds pds, int discipline, Formatter f) {
    int template = pds.getTemplateNumber();
    f.format(" Product Template %3d = %s%n", template, cust.getTableValue("4.0", template));
    f.format(" Discipline %3d     = %s%n", discipline, cust.getTableValue("0.0", discipline));
    f.format(" Category %3d       = %s%n", pds.getParameterCategory(), cust.getCategory(discipline, pds.getParameterCategory()));
    Grib2Customizer.Parameter entry = cust.getParameter(discipline, pds.getParameterCategory(), pds.getParameterNumber());
    if (entry != null) {
      f.format(" Parameter Name     = %3d %s %n", pds.getParameterNumber(), entry.getName());
      f.format(" Parameter Units    = %s %n", entry.getUnit());
    } else {
      f.format(" Unknown Parameter  = %d-%d-%d %n", discipline, pds.getParameterCategory(), pds.getParameterNumber());
      cust.getParameter(discipline, pds.getParameterCategory(), pds.getParameterNumber()); // debug
    }
    f.format(" Parameter Table  = %s%n", cust.getTablePath(discipline, pds.getParameterCategory(), pds.getParameterNumber()));

    int tgp = pds.getGenProcessType();
    f.format(" Generating Process Type = %3d %s %n", tgp, cust.getTableValue("4.3", tgp));
    f.format(" Forecast Offset    = %3d %n", pds.getForecastTime());
    f.format(" First Surface Type = %3d %s %n", pds.getLevelType1(), cust.getLevelNameShort(pds.getLevelType1()));
    f.format(" First Surface value= %3f %n", pds.getLevelValue1());
    f.format(" Second Surface Type= %3d %s %n", pds.getLevelType2(), cust.getLevelNameShort(pds.getLevelType2()));
    f.format(" Second Surface val = %3f %n", pds.getLevelValue2());
    f.format("%n Level Name (from table 4.5) = %3s %n", cust.getTableValue("4.5", pds.getLevelType1()));
    f.format(" Gen Process Ttype (from table 4.3) = %3s %n", cust.getTableValue("4.3", pds.getGenProcessType()));
  }

  ////////////////////////////////////////////////////////

  public class Grib2RecordBean {
    Grib2Record gr;
    Grib2Pds pds;

    public Grib2RecordBean() {
    }

    public Grib2RecordBean(Grib2Record m) throws IOException {
      this.gr = m;
      this.pds = gr.getPDS();
    }

    public final String getRefDate() {
      return gr.getReferenceDate().toString();
    }

    public final String getForecastDate() {
      CalendarDate cd =  cust.getForecastDate(gr);
      return cd == null ? null : cd.toString();
    }

    public String getHeader() {
      return Grib2Utils.cleanupHeader(gr.getHeader());
    }

    public final String getTimeUnit() {
      int unit = pds.getTimeUnit();
      return cust.getTableValue("4.4", unit);
    }

    public final int getForecastTime() {
      return pds.getForecastTime();
    }

    public final int getGenProcessType() {
      return pds.getGenProcessType();
    }

    public final String getFile() {
      int fno = gr.getFile();
      return fileList.get(fno).getName();
    }

    public String getLevel() {
      int v1 = pds.getLevelType1();
      int v2 = pds.getLevelType2();
      if (v1 == 255) return "";
      if (v2 == 255) return "" + pds.getLevelValue1();
      if (v1 != v2) return pds.getLevelValue1() + "-" + pds.getLevelValue2() + " level2 type= " + v2;
      return pds.getLevelValue1() + "-" + pds.getLevelValue2();
    }

    public long getStartPos() {
      return gr.getIs().getStartPos();
    }

    public void toRawPdsString(Formatter f) {
      byte[] bytes = gr.getPDSsection().getRawBytes();
      int count = 1;
      for (byte b : bytes) {
        short s = DataType.unsignedByteToShort(b);
        f.format(" %d : %d%n", count++, s);
      }
    }

    public void toRawLUString(Formatter f) {
      if (gr.getLocalUseSection() == null) {
        f.format("No Local Use Section");
        return;
      }
      byte[] bytes = gr.getLocalUseSection().getRawBytes();
      f.format("Local Use Section len=%d%n", bytes.length);
      showBytes(f, bytes, -1);
    }

    public String showProcessedGridRecord(Formatter f) {
      f.format("%nFile=%s (%d)%n", fileList.get(gr.getFile()).getPath(), gr.getFile());
      GribTables.Parameter param = cust.getParameter(gr.getDiscipline(), gr.getPDS().getParameterCategory(), gr.getPDS().getParameterNumber());
      if (param == null)
        f.format("  Parameter=Unknown %d-%d-%d %n", gr.getDiscipline(), gr.getPDS().getParameterCategory(), gr.getPDS().getParameterNumber());
      else
        f.format("  Parameter=%s (%s)%n", param.getName(), param.getAbbrev());

      VertCoord.VertUnit levelUnit = cust.getVertUnit(pds.getLevelType1());
      f.format("  Level=%f/%f %s; level name =  (%s)%n", pds.getLevelValue1(), pds.getLevelValue1(), levelUnit.getUnits(), cust.getLevelNameShort(pds.getLevelType1()));

      String intvName = "none";
      if (pds instanceof Grib2Pds.PdsInterval) {
        Grib2Pds.PdsInterval pdsi = (Grib2Pds.PdsInterval) pds;
        Grib2Pds.TimeInterval[] ti = pdsi.getTimeIntervals();
        int statType = ti[0].statProcessType;
        intvName = cust.getStatisticNameShort(statType);
      }

      f.format("  Time Unit=%s ;Stat=%s%n", Grib2Utils.getCalendarPeriod( pds.getTimeUnit()), intvName);
      f.format("  ReferenceDate=%s%n", gr.getReferenceDate());
      // f.format("  IntervalTimeEnd=%s%n", cust.getIntervalTimeEnd(gr));
      f.format("  ForecastDate=%s%n", cust.getForecastDate(gr));
      TimeCoord.TinvDate intv = cust.getForecastTimeInterval(gr);
      if (intv != null) f.format("  TimeInterval=%s%n", intv);
      f.format("%n");
      pds.show(f);

      //CFSR malarky
      if (pds.getTemplateNumber() == 8 && cust instanceof NcepLocalTables) {
        NcepLocalTables ncepCust =  (NcepLocalTables) cust;
        ncepCust.showCfsr(pds, f);
      }

      return f.toString();
    }

    float[] readData() throws IOException {
      int fileno = gr.getFile();
      MFile mfile = fileList.get(fileno);
      try (ucar.unidata.io.RandomAccessFile raf = new ucar.unidata.io.RandomAccessFile(mfile.getPath(), "r")) {
        raf.order(ucar.unidata.io.RandomAccessFile.BIG_ENDIAN);
        return gr.readData(raf);
      }
    }

    /////////////////////////////////////////////////////////////
    /// time intervals
     /*
    TimeInterval: statProcessType= 0, timeIncrementType= 1, timeRangeUnit= 1, timeRangeLength= 744, timeIncrementUnit= 1, timeIncrement=24
    TimeInterval: statProcessType= 197, timeIncrementType= 2, timeRangeUnit= 1, timeRangeLength= 23, timeIncrementUnit= 1, timeIncrement=0
     */
    public String getTInv() {
      Grib2Pds.PdsInterval pdsi = (Grib2Pds.PdsInterval) pds;

      Formatter f = new Formatter();
      int count = 0;
      for (Grib2Pds.TimeInterval ti : pdsi.getTimeIntervals()) {
        if (count++ > 0) f.format(", ");
        f.format("%d-%d-%d", ti.statProcessType, ti.timeRangeLength, ti.timeIncrement);
      }
      return f.toString();
    }

    public String getIntv() {
      if (cust != null) {
        TimeCoord.TinvDate intv = cust.getForecastTimeInterval(gr);
        if (intv != null) return intv.toString();
      }
      return "";
    }

    public String getIntv2() {
      if (cust != null) {
        int[] intv = cust.getForecastTimeIntervalOffset(gr);
        if (intv != null) return intv[0]+"-"+intv[1];
      }
      return "";
    }

    public long getIntvHash() {
       Grib2Pds.PdsInterval pdsi = (Grib2Pds.PdsInterval) pds;
       return pdsi.getIntervalHash();
    }

    /////////////////////////////
    // Aerosols

    public int getAerType() { return ((Grib2Pds.PdsAerosol) pds).getAerosolType(); }
    public double getAerIntSizeType()  { return ((Grib2Pds.PdsAerosol) pds).getAerosolIntervalSizeType(); }
    public double getAerSize1()  { return ((Grib2Pds.PdsAerosol) pds).getAerosolSize1() * 10e6; } // microns
    public double getAerSize2()  { return ((Grib2Pds.PdsAerosol) pds).getAerosolSize2() * 10e6; } // microns
    public double getAerIntWavelType()  { return ((Grib2Pds.PdsAerosol) pds).getAerosolIntervalWavelengthType(); }
    public double getAerWavel1()   { return ((Grib2Pds.PdsAerosol) pds).getAerosolWavelength1(); }
    public double getAerWavel2()  { return ((Grib2Pds.PdsAerosol) pds).getAerosolWavelength2(); }

  ///////////////////////////////
    // Ensembles
   public  int getPertN() {
     Grib2Pds.PdsEnsemble pdsi = (Grib2Pds.PdsEnsemble) pds;
      int v = pdsi.getPerturbationNumber();
      if (v == GribNumbers.UNDEFINED) v = -1;
      return v;
    }

    public  int getNForecastsInEns() {
      Grib2Pds.PdsEnsemble pdsi = (Grib2Pds.PdsEnsemble) pds;
      int v = pdsi.getNumberEnsembleForecasts();
      if (v == GribNumbers.UNDEFINED) v = -1;
      return v;
    }

    public  int getPertType() {
      Grib2Pds.PdsEnsemble pdsi = (Grib2Pds.PdsEnsemble) pds;
      int v = pdsi.getPerturbationType();
      return (v == GribNumbers.UNDEFINED) ? -1 : v;
    }

    /////////////////////////////////
    // Probability

    public  String getProbLimits() {
      Grib2Pds.PdsProbability pdsi = (Grib2Pds.PdsProbability) pds;
      double v = pdsi.getProbabilityLowerLimit();
      if (v == GribNumbers.UNDEFINEDD) return "";
      else return pdsi.getProbabilityLowerLimit() + "-" + pdsi.getProbabilityUpperLimit();
    }

  }

  private static class PdsBeanInfo extends SimpleBeanInfo {
    PropertyDescriptor[] properties;

    PdsBeanInfo(Grib2Pds pds) {
      ArrayList<PropertyDescriptor> props = new ArrayList<PropertyDescriptor>(40);

      Class cl = Grib2RecordBean.class;
      try {
        props.add(new PropertyDescriptor("startPos", cl, "getStartPos", null));
        props.add(new PropertyDescriptor("file", cl, "getFile", null));
        props.add(new PropertyDescriptor("forecastDate", cl, "getForecastDate", null));
        props.add(new PropertyDescriptor("forecastTime", cl, "getForecastTime", null));
        props.add(new PropertyDescriptor("processType", cl, "getGenProcessType", null));
        props.add(new PropertyDescriptor("header", cl, "getHeader", null));
        props.add(new PropertyDescriptor("level", cl, "getLevel", null));
        props.add(new PropertyDescriptor("refDate", cl, "getRefDate", null));
        props.add(new PropertyDescriptor("timeUnit", cl, "getTimeUnit", null));

        if (pds instanceof Grib2Pds.PdsAerosol) {
          props.add(new PropertyDescriptor("aerIntSizeType", cl, "getAerIntSizeType", null));
          props.add(new PropertyDescriptor("aerIntWavelType", cl, "getAerIntWavelType", null));
          props.add(new PropertyDescriptor("aerSize1", cl, "getAerSize1", null));
          props.add(new PropertyDescriptor("aerSize2", cl, "getAerSize2", null));
          props.add(new PropertyDescriptor("aerType", cl, "getAerType", null));
          props.add(new PropertyDescriptor("aerWavel1", cl, "getAerWavel1", null));
          props.add(new PropertyDescriptor("aerWavel2", cl, "getAerWavel2", null));
        }

        if (pds instanceof Grib2Pds.PdsEnsemble) {
          props.add(new PropertyDescriptor("pertN", cl, "getPertN", null));
          props.add(new PropertyDescriptor("pertType", cl, "getPertType", null));
          props.add(new PropertyDescriptor("nForecastsInEns", cl, "getNForecastsInEns", null));
        }

        if (pds instanceof Grib2Pds.PdsInterval) {
          props.add(new PropertyDescriptor("intv", cl, "getIntv", null));
          props.add(new PropertyDescriptor("intv2", cl, "getIntv2", null));
          props.add(new PropertyDescriptor("intvHash", cl, "getIntvHash", null));
        }

        if (pds instanceof Grib2Pds.PdsProbability) {
          props.add(new PropertyDescriptor("probLimits", cl, "getProbLimits", null));
        }

      } catch (IntrospectionException e) {
        e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
      }

      properties = new PropertyDescriptor[props.size()];
      props.toArray(properties);
    }


    @Override
    public PropertyDescriptor[] getPropertyDescriptors() {
      return properties;
    }

  }

}
