package ucar.nc2.ui;

import thredds.featurecollection.FeatureCollectionConfig;
import thredds.inventory.*;
import ucar.sparr.Coordinate;
import ucar.ma2.DataType;
import ucar.nc2.grib.*;
import ucar.nc2.grib.collection.Grib2CollectionWriter;
import ucar.nc2.grib.collection.Grib2Rectilyser;
import ucar.nc2.grib.grib2.*;
import ucar.nc2.grib.collection.*;
import ucar.nc2.grib.grib2.table.Grib2Customizer;
import ucar.nc2.grib.grib2.table.NcepLocalTables;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.ui.dialog.GribCollectionConfig;
import ucar.nc2.ui.widget.*;
import ucar.nc2.ui.widget.PopupMenu;
import ucar.nc2.util.Misc;
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
 * Expose Grib2 Rectilyze processing
 *
 * @author John
 * @since 11/28/13
 */
public class Grib2RectilyzePanel extends JPanel {
  static private final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(Grib2CollectionPanel.class);

  private final PreferencesExt prefs;

  private BeanTable param2BeanTable, record2BeanTable, coordTable, allCoordTable;
  private JSplitPane split, split2, split3;

  private TextHistoryPane infoPopup, infoPopup2, infoPopup3;
  private IndependentWindow infoWindow, infoWindow2, infoWindow3;
  private FileManager fileChooser;
  private GribCollectionConfig gribCollectionConfigDialog;

  public Grib2RectilyzePanel(final PreferencesExt prefs, JPanel buttPanel) {
    this.prefs = prefs;

    PopupMenu varPopup;

    ////////////////
    param2BeanTable = new BeanTable(VariableBagBean.class, (PreferencesExt) prefs.node("Param2Bean"), false,
            "Grib2PDSVariables", "from Grib2Input.getRecords()", null);
    param2BeanTable.addListSelectionListener(new ListSelectionListener() {
      public void valueChanged(ListSelectionEvent e) {
        VariableBagBean bean = (VariableBagBean) param2BeanTable.getSelectedBean();
        if (bean != null) {
          java.util.List<Grib2RecordBean> records = new ArrayList<>();
          for (Grib2Record r : bean.vb.atomList)
            records.add(new Grib2RecordBean(r));
          record2BeanTable.setBeans(records);

          int count = 0;
          java.util.List<CoordBean> coords = new ArrayList<>();
          for (Coordinate coord : bean.vb.coordND.getCoordinates())
            coords.add(new CoordBean(coord, bean.vb.coordIndex.get(count++)));
          coordTable.setBeans(coords);

          java.util.List<CoordBean> coordAll = new ArrayList<>();
          int idx = 0;
          for (Coordinate coord : bean.rect.getCoordinates())
            coordAll.add(new CoordBean(coord, idx++));
          allCoordTable.setBeans(coordAll);
        }
      }
    });

    varPopup = new ucar.nc2.ui.widget.PopupMenu(param2BeanTable.getJTable(), "Options");
    varPopup.addAction("Show PDS", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        VariableBagBean pb = (VariableBagBean) param2BeanTable.getSelectedBean();
        if (pb != null) {
          Formatter f = new Formatter();
          Grib2CollectionPanel.showPdsTemplate(pb.vb.first.getPDSsection(), f, cust);
          infoPopup2.setText(f.toString());
          infoPopup2.gotoTop();
          infoWindow2.show();
        }
      }
    });
    varPopup.addAction("Show processed PDS", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        VariableBagBean pb = (VariableBagBean) param2BeanTable.getSelectedBean();
        if (pb != null) {
          infoPopup3.setText(pb.toProcessedString());
          infoPopup3.gotoTop();
          infoWindow3.show();
        }
      }
    });
    varPopup.addAction("Show SparseArray and Duplicates", new AbstractAction() {
     public void actionPerformed(ActionEvent e) {
       VariableBagBean bean = (VariableBagBean) param2BeanTable.getSelectedBean();
       if (bean == null) return;
       Formatter f = new Formatter();
       bean.vb.coordND.showInfo(bean.vb.atomList, f);
       infoPopup2.setText(f.toString());
       infoPopup2.gotoTop();
       infoWindow2.show();
     }
   });
    varPopup.addAction("Compare cdmHash", new AbstractAction() {
     public void actionPerformed(ActionEvent e) {
       List beans = param2BeanTable.getSelectedBeans();
       if (beans == null || beans.size() != 2) return;
       Formatter f = new Formatter();
       compareCdmHash((VariableBagBean) beans.get(0), (VariableBagBean) beans.get(1), f);
       infoPopup2.setText(f.toString());
       infoPopup2.gotoTop();
       infoWindow2.show();
     }
   });

    Class useClass = Grib2RecordBean.class;
    record2BeanTable = new BeanTable(useClass, (PreferencesExt) prefs.node(useClass.getName()), false,
            "Grib2Record", "from VariableBag.atomList", null);
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
          infoWindow2.show();
        }
      }
    });

    varPopup.addAction("Show complete GridRecord", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        Grib2RecordBean bean = (Grib2RecordBean) record2BeanTable.getSelectedBean();
        if (bean != null) {
          Formatter f = new Formatter();
          try {
            Grib2CollectionPanel.showCompleteGribRecord(f, fileList.get(bean.gr.getFile()).getPath(), bean.gr, cust);
          } catch (IOException ioe) {
            ByteArrayOutputStream bos = new ByteArrayOutputStream(10000);
            ioe.printStackTrace(new PrintStream(bos));
            f.format("%s", bos.toString());
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

    coordTable = new BeanTable(CoordBean.class, (PreferencesExt) prefs.node("CoordBean"), false,
            "Variable Coordinates", "From VariableBag.CoordND", null);
    varPopup = new PopupMenu(coordTable.getJTable(), "Options");

     varPopup.addAction("Show Coords", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        CoordBean bean = (CoordBean) coordTable.getSelectedBean();
        if (bean == null) return;
        Formatter f = new Formatter();
        bean.showCoords(f);
        infoPopup2.setText(f.toString());
        infoPopup2.gotoTop();
        infoWindow2.show();
      }
    });

    allCoordTable = new BeanTable(CoordBean.class, (PreferencesExt) prefs.node("CoordBean"), false,
            "Group Coordinates", "From Group", null);;
    varPopup = new PopupMenu(allCoordTable.getJTable(), "Options");

     varPopup.addAction("Show Coords", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        CoordBean bean = (CoordBean) allCoordTable.getSelectedBean();
        if (bean == null) return;
        Formatter f = new Formatter();
        bean.showCoords(f);
        infoPopup2.setText(f.toString());
        infoPopup2.gotoTop();
        infoWindow2.show();
      }
    });

    //////////////////////////////////////////
    // extra buttcons

    gribCollectionConfigDialog = new GribCollectionConfig();
    gribCollectionConfigDialog.pack();

    AbstractButton compareButton = BAMutil.makeButtcon("Select", "GribConfig", false);
    compareButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        gribCollectionConfigDialog.setVisible(true);
      }
    });
    buttPanel.add(compareButton);


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

    split3 = new JSplitPane(JSplitPane.VERTICAL_SPLIT, false, param2BeanTable, record2BeanTable);
    split3.setDividerLocation(prefs.getInt("splitPos3", 800));

    split2 = new JSplitPane(JSplitPane.VERTICAL_SPLIT, false, split3, coordTable);
    split2.setDividerLocation(prefs.getInt("splitPos2", 400));

    split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, false, split2, allCoordTable);
    split.setDividerLocation(prefs.getInt("splitPos", 400));

    add(split, BorderLayout.CENTER);

  }

  void makeRecordTable(Grib2Pds pds) {
    if (record2BeanTable != null) record2BeanTable.saveState(false);

    BeanInfo info = new PdsBeanInfo(pds);

    String prefsName = pds.getClass().getName();
    record2BeanTable = new BeanTable(Grib2RecordBean.class, (PreferencesExt) prefs.node(prefsName), prefsName, "from Grib2Input.getRecords()", info);
    PopupMenu varPopup = new PopupMenu(record2BeanTable.getJTable(), "Options");

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
        for (int i = 0; i < list.size(); i++) {
          Grib2RecordBean bean = (Grib2RecordBean) list.get(i);
          bean.toRawPdsString(f);
        }
        infoPopup.setText(f.toString());
        infoPopup.gotoTop();
        infoWindow.show();
      }
    });

    varPopup.addAction("Show complete GridRecord", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        Grib2RecordBean bean = (Grib2RecordBean) record2BeanTable.getSelectedBean();
        if (bean != null) {
          Formatter f = new Formatter();
          try {
            Grib2CollectionPanel.showCompleteGribRecord(f, fileList.get(bean.gr.getFile()).getPath(), bean.gr, cust);
          } catch (IOException ioe) {
            ByteArrayOutputStream bos = new ByteArrayOutputStream(10000);
            ioe.printStackTrace(new PrintStream(bos));
            f.format("%s", bos.toString());
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

    if (split2 != null) {
      int d = split2.getDividerLocation();
      split2.setRightComponent(record2BeanTable);
      split2.setDividerLocation(d);
    }
  }

  public void save() {
    allCoordTable.saveState(false);
    coordTable.saveState(false);
    param2BeanTable.saveState(false);
    record2BeanTable.saveState(false);
    prefs.putBeanObject("InfoWindowBounds", infoWindow.getBounds());
    prefs.putBeanObject("InfoWindowBounds2", infoWindow2.getBounds());
    prefs.putBeanObject("InfoWindowBounds3", infoWindow3.getBounds());
    if (split != null) prefs.putInt("splitPos", split.getDividerLocation());
    if (split2 != null) prefs.putInt("splitPos2", split2.getDividerLocation());
    if (split3 != null) prefs.putInt("splitPos3", split3.getDividerLocation());
  }

  void closeOpenFiles() throws IOException {
    param2BeanTable.clearBeans();
    record2BeanTable.clearBeans();
    coordTable.clearBeans();
    allCoordTable.clearBeans();
  }

  ///////////////////////////////////////////////

  private String spec;
  private MCollection dcm;
  private List<MFile> fileList;
  private Grib2Customizer cust;
  private Grib2Rectilyser rect2;

  private MCollection makeCollection(String spec, Formatter f) {
    MCollection dc;
    try {
      dc = CollectionAbstract.open("Grib2RectilyzerPanel", spec, null, f);
      return dc;

    } catch (Exception e) {
      ByteArrayOutputStream bos = new ByteArrayOutputStream(10000);
      e.printStackTrace(new PrintStream(bos));
      f.format("%s", bos.toString());
      return null;
    }
  }

  Formatter info;
  public void setCollection(String spec) throws IOException {
    java.util.List<VariableBagBean> bags = new ArrayList<>();

    fileList = new ArrayList<>();
    info = new Formatter();
    dcm = makeCollection(spec, info);
    FeatureCollectionConfig config = gribCollectionConfigDialog.getConfig();
    dcm.putAuxInfo(FeatureCollectionConfig.AUX_CONFIG, config);

    Grib2CollectionBuilder builder = GribCdmIndex2.debugOnly(dcm, logger);
    List<Grib2CollectionWriter.Group> groups = builder.makeGroups(fileList, info);
    // this.cust = builder.getCustomizer();  LOOK

    for (Grib2CollectionWriter.Group group : groups) {
      Grib2Rectilyser rect = group.rect;
      // rect.showInfo(info, cust);
      for (Grib2Rectilyser.VariableBag vb : rect.getGribvars()) {
        bags.add( new VariableBagBean(rect, vb));
      }
    }
    param2BeanTable.setBeans(bags);

    record2BeanTable.clearBeans();
    coordTable.clearBeans();
    allCoordTable.clearBeans();
  }

  public void showCollection(Formatter f) {
    if (fileList != null)
      for (MFile mfile : fileList) {
        f.format("  %s%n", mfile.getPath());
      }

    f.format("%n%s%n", info);
  }

  public void showStats(Formatter f) throws IOException {
    int total_ndups = 0;
    int total_records = 0;
    int total_missing= 0;
    int total_size = 0;

    for (Object beano : param2BeanTable.getBeans()) {
      VariableBagBean bean = (VariableBagBean) beano;
      total_ndups += bean.getNDups();
      total_records += bean.getNRecords();
      total_missing += bean.getNMissing();
      total_size += bean.getSize();
    }

    f.format("total_ndups = %d%n", total_ndups);
    f.format("total_records = %d%n", total_records);
    f.format("total_missing = %d%n", total_missing);
    f.format("total_size = %d%n", total_size);
    f.format("total_density = %f%n", ((float) total_records) / total_size);
  }

  private void merge(Set<Object> set, Coordinate coord) {
    if (coord.getValues() == null) return;
    for (Object val : coord.getValues())
      set.add(val);
  }


  public boolean writeIndex(Formatter f) throws IOException {
    if (dcm == null) return false;
    return GribCdmIndex2.makeIndex(dcm, f, logger);
  }

  public void checkProblems(Formatter f) {
    //checkDuplicates(f);
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

  /* private void checkDuplicates(Formatter f) {

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
  }   */

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
          JOptionPane.showMessageDialog(Grib2RectilyzePanel.this, "Old index does not have message start - record not written");
        }

        byte[] rb = new byte[size];
        raf.seek(startPos);
        raf.readFully(rb);
        fos.write(rb);
        n++;
      }

      JOptionPane.showMessageDialog(Grib2RectilyzePanel.this, filename + ": " + n + " records successfully written, append=" + append);

    } catch (Exception ex) {
      JOptionPane.showMessageDialog(Grib2RectilyzePanel.this, "ERROR: " + ex.getMessage());
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

  private void compare(Grib2SectionGridDefinition gdss1, Grib2SectionGridDefinition gdss2, Formatter f) {
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

  /////////////////////////////////////////////////////////

  public class CoordBean implements Comparable<CoordBean> {
    Coordinate coord;
    int idx;

    // no-arg constructor

    public CoordBean() {
    }

    public String getType() {
      return coord.getType().toString();
    }

    public CoordBean(Coordinate coord, int idx) {
      this.coord = coord;
      this.idx = idx;
    }

    public String getValues() {
      if (coord.getValues() == null) return "";
      Formatter f = new Formatter();
      for (Object val : coord.getValues()) f.format("%s,", val);
      return f.toString();
    }

    public int getSize() {
      return coord.getSize();
    }

    public int getCode() {
      return coord.getCode();
    }

    public int getIndex() {
      return idx;
    }

    public String getUnit() {
      return coord.getUnit();
    }

    public String getName() {
      return coord.getName();
    }

    @Override
    public int compareTo(CoordBean o) {
      return getType().compareTo(o.getType());
    }

    void showCoords(Formatter f) {
      coord.showCoords(f);
    }
  }

  ////////////////////////////////////////////////////////////////////////////

  public class VariableBagBean {
    Grib2Rectilyser rect;
    Grib2Rectilyser.VariableBag  vb;
    Grib2Pds pds;
    Grib2SectionIdentification id;
    int discipline;
    int nrun, ntime, nvert;
    // no-arg constructor

    public VariableBagBean() {
    }

    public VariableBagBean(Grib2Rectilyser rect, Grib2Rectilyser.VariableBag vb) throws IOException {
      this.rect = rect;
      this.vb = vb;
      pds = vb.first.getPDS();
      id = vb.first.getId();
      discipline =  vb.first.getDiscipline();

      for (Coordinate coord : vb.coordND.getCoordinates()) {
        if (coord instanceof CoordinateRuntime)
          nrun = coord.getSize();
        else if (coord instanceof CoordinateTime || coord instanceof CoordinateTimeIntv)
          ntime = coord.getSize();
        else if (coord instanceof CoordinateVert)
          nvert = coord.getSize();
      }
    }

    public String getParamNo() {
      return discipline+ "-" + pds.getParameterCategory() + "-" + pds.getParameterNumber();
    }

    public int getTemplate() {
      return vb.first.getPDSsection().getPDSTemplateNumber();
    }

    public boolean isTimeIntv() {
      return vb.first.getPDS().isTimeInterval();
    }

    public boolean isVert() {
      VertCoord.VertUnit vertUnit = Grib2Utils.getLevelUnit(pds.getLevelType1());
      return vertUnit.isVerticalCoordinate();
    }

     public int getNRecords() {
      return vb.atomList.size();
    }

    public int getLevelType() {
      return pds.getLevelType1();
    }

    public int getNruns() {
      return nrun;
    }

    public int getNTimes() {
      return ntime;
    }

    public int getNVerts() {
      return nvert;
    }

    public int getNDups() {
      return vb.coordND.getSparseArray().getNduplicates();
    }

    public int getNMissing() {
      return vb.coordND.getSparseArray().countMissing();
    }

    public double getDensity() {
      return vb.coordND.getSparseArray().getDensity();
    }

    public int getSize() {
      return vb.coordND.getSparseArray().getTotalSize();
    }

   public final String getStatType() {
      if (pds.isTimeInterval()) {
        Formatter f = new Formatter();
        Grib2Pds.PdsInterval pdsi = (Grib2Pds.PdsInterval) pds;
        int count = 0;
        for (Grib2Pds.TimeInterval ti : pdsi.getTimeIntervals()) {
          if (count++ > 0) f.format(", ");
          f.format("%d %s", ti.statProcessType, cust.getIntervalNameShort(ti.statProcessType));
        }
        return f.toString();
      } else return "";
    }

    public int getGDS() {
      return vb.first.getGDSsection().getGDS().hashCode();
    }

    public long getCdmHash() {
      return vb.cdmHash;
    }

    public String toString() {
      Formatter f = new Formatter();
      Grib2CollectionPanel.showPdsTemplate(vb.first.getPDSsection(), f, cust);
      return f.toString();
    }

    public String toProcessedString() {
      Formatter f = new Formatter();
      showProcessedPds(vb.first, pds, discipline, f);
      return f.toString();
    }

    ///////////////

    public String getName() {
      return GribUtils.makeNameFromDescription(cust.getVariableName(vb.first));
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

    public final boolean isEns() {
      return pds.isEnsemble();
    }

    public void findDuplicates(Formatter info) {
      vb.coordND.showInfo(vb.atomList, info);
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

  ////////////////////////////////////////////////////////
  /*   public int cdmVariableHash(Grib2Record gr, int gdsHash) {
      Grib2SectionGridDefinition gdss = gr.getGDSsection();
      Grib2Pds pds2 = gr.getPDS();

      int result = 17;

      if (gdsHash == 0)
        result += result * 37 + gdss.getGDS().hashCode(); // the horizontal grid
      else
        result += result * 37 + gdsHash;

      // if this uses any local tables, then we have to add the center id, and subcenter if present
      if ((pds2.getParameterCategory() > 191) || (pds2.getParameterNumber() > 191) || (pds2.getLevelType1() > 191)
              || (pds2.isTimeInterval() && pds2.getStatisticalProcessType() > 191)
              || (ensDerivedType > 191) || (probType > 191)) {
        Grib2SectionIdentification id = gr.getId();
        result += result * 37 + id.getCenter_id();
        if (id.getSubcenter_id() > 0)
          result += result * 37 + id.getSubcenter_id();
      }
    } */
  private void compareCdmHash(VariableBagBean bean1, VariableBagBean bean2, Formatter f) {
    f.format("discipline   %d == %d%n", bean1.discipline, bean2.discipline);
    f.format("category     %d == %d%n", bean1.pds.getParameterCategory(), bean2.pds.getParameterCategory());
    f.format("paramNo      %d == %d%n", bean1.pds.getParameterNumber(), bean2.pds.getParameterNumber());
    f.format("template     %d == %d%n", bean1.pds.getTemplateNumber(), bean2.pds.getTemplateNumber());
    f.format("levelType    %d == %d%n", bean1.pds.getLevelType1(), bean2.pds.getLevelType1());
    f.format("isLayer      %s == %s%n", Grib2Utils.isLayer(bean1.pds), Grib2Utils.isLayer(bean2.pds));
    f.format("isTimeIntv   %s == %s%n", bean1.pds.isTimeInterval(), bean2.pds.isTimeInterval());
    if (bean1.pds.isTimeInterval() && bean2.pds.isTimeInterval()) {  //!intvMerge
      f.format("intvSize     %f == %f%n",  cust.getForecastTimeIntervalSizeInHours(bean1.pds), cust.getForecastTimeIntervalSizeInHours(bean2.pds));
    }
    f.format("statType     %d == %d%n", bean1.pds.getStatisticalProcessType(), bean2.pds.getStatisticalProcessType());
    f.format("isProb       %s == %s%n", bean1.pds.isProbability(), bean2.pds.isProbability());
    if (bean1.pds.isProbability() && bean2.pds.isProbability()) {
      Grib2Pds.PdsProbability pdsProb1 = (Grib2Pds.PdsProbability) bean1.pds;
      Grib2Pds.PdsProbability pdsProb2 = (Grib2Pds.PdsProbability) bean2.pds;
      f.format("probType     %d == %d%n", pdsProb1.getProbabilityType(), pdsProb2.getProbabilityType());
      f.format("probName     %s == %s%n", pdsProb1.getProbabilityName(), pdsProb2.getProbabilityName());
      f.format("probHash     %d == %d%n", pdsProb1.getProbabilityHashcode(), pdsProb2.getProbabilityHashcode());
    }
    f.format("isEnsemble   %s == %s%n", bean1.pds.isEnsemble(), bean2.pds.isEnsemble());
    f.format("isEnsDerived %s == %s%n", bean1.pds.isEnsembleDerived(), bean2.pds.isEnsembleDerived());
    if (bean1.pds.isEnsembleDerived() && bean2.pds.isEnsembleDerived()) {
      Grib2Pds.PdsEnsembleDerived pdsDerived1 = (Grib2Pds.PdsEnsembleDerived) bean1.pds;
      Grib2Pds.PdsEnsembleDerived pdsDerived2 = (Grib2Pds.PdsEnsembleDerived) bean2.pds;
      f.format("DerivedType    %d == %d%n", pdsDerived1.getDerivedForecastType(), pdsDerived2.getDerivedForecastType());
    }
   f.format("genType       %d == %d%n", bean1.pds.getGenProcessType(), bean2.pds.getGenProcessType());
  }

  private void showProcessedPds(Grib2Record gr, Grib2Pds pds, int discipline, Formatter f) {
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

  public class Grib2RecordBean {
    Grib2Record gr;
    Grib2Pds pds;

    public Grib2RecordBean() {
    }

    public Grib2RecordBean(Grib2Record gr) {
      this.gr = gr;
      try {
        this.pds = gr.getPDSsection().getPDS();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }

    public final String getRefDate() {
      return gr.getReferenceDate().toString();
    }

    public final String getForecastDate() {
      return cust.getForecastDate(gr).toString();
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

    public int getGenProcessId() {
      return pds.getGenProcessId();
    }

    public int getGenProcessType() {
      return pds.getGenProcessType();
    }

    public final String getFilename() {
      int fno = gr.getFile();
      return fileList.get(fno).getName();
    }

    public final int getFile() {
      return gr.getFile();
    }

    public String getLevel() {
      int v1 = pds.getLevelType1();
      int v2 = pds.getLevelType2();
      if (v1 == 255) return "";
      if (v2 == 255) return "" + pds.getLevelValue1();
      if (v1 != v2) return pds.getLevelValue1() + "-" + pds.getLevelValue2() + " level2 type= " + v2;
      return pds.getLevelValue1() + "-" + pds.getLevelValue2();
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
      GribTables.Parameter param = cust.getParameter(gr.getDiscipline(), gr.getPDS().getParameterCategory(), gr.getPDS().getParameterNumber());
      f.format("  Parameter=%s (%s)%n", param.getName(), param.getAbbrev());

      VertCoord.VertUnit levelUnit = Grib2Utils.getLevelUnit(pds.getLevelType1());
      f.format("  Level=%f/%f %s; level name =  (%s)%n", pds.getLevelValue1(), pds.getLevelValue1(), levelUnit.getUnits(), cust.getLevelNameShort(pds.getLevelType1()));

      String intvName = "none";
      if (pds instanceof Grib2Pds.PdsInterval) {
        Grib2Pds.PdsInterval pdsi = (Grib2Pds.PdsInterval) pds;
        Grib2Pds.TimeInterval[] ti = pdsi.getTimeIntervals();
        int statType = ti[0].statProcessType;
        intvName = cust.getIntervalNameShort(statType);
      }

      f.format("  Time Unit=%s ;Stat=%s%n", Grib2Utils.getCalendarPeriod(pds.getTimeUnit()), intvName);
      f.format("  ReferenceDate=%s%n", gr.getReferenceDate());
      // f.format("  IntervalTimeEnd=%s%n", cust.getIntervalTimeEnd(gr));
      f.format("  ForecastDate=%s%n", cust.getForecastDate(gr));
      TimeCoord.TinvDate intv = cust.getForecastTimeInterval(gr);
      if (intv != null) f.format("  TimeInterval=%s%n", intv);
      f.format("%n");
      pds.show(f);

      //CFSR malarky
      if (pds.getTemplateNumber() == 8 && cust instanceof NcepLocalTables) {
        NcepLocalTables ncepCust = (NcepLocalTables) cust;
        ncepCust.showCfsr(pds, f);
      }

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

    /////////////////////////////////////////////////////////////
    /// time intervals
     /*
    TimeInterval: statProcessType= 0, timeIncrementType= 1, timeRangeUnit= 1, timeRangeLength= 744, timeIncrementUnit= 1, timeIncrement=24
    TimeInterval: statProcessType= 197, timeIncrementType= 2, timeRangeUnit= 1, timeRangeLength= 23, timeIncrementUnit= 1, timeIncrement=0
     *
    public String getTInv() {
      Grib2Pds.PdsInterval pdsi = (Grib2Pds.PdsInterval) pds;

      Formatter f = new Formatter();
      int count = 0;
      for (Grib2Pds.TimeInterval ti : pdsi.getTimeIntervals()) {
        if (count++ > 0) f.format(", ");
        f.format("%d-%d-%d", ti.statProcessType, ti.timeRangeLength, ti.timeIncrement);
      }
      return f.toString();
    }    */

    public String getIntv() {
      if (cust != null) {
        TimeCoord.TinvDate intv = cust.getForecastTimeInterval(gr);
        return intv == null ? "" : intv.toString();
      }
      return "";
    }

    public String getIntv2() {
      if (cust != null) {
        int[] intv = cust.getForecastTimeIntervalOffset(gr);
        return intv == null ? "" :  intv[0] + "-" + intv[1];
      }
      return "";
    }

    /* public long getIntvHash() {
      Grib2Pds.PdsInterval pdsi = (Grib2Pds.PdsInterval) pds;
      return pdsi.getIntervalHash();
    }  */

    /////////////////////////////
    /* Aerosols

    public int getAerType() {
      return ((Grib2Pds.PdsAerosol) pds).getAerosolType();
    }

    public double getAerIntSizeType() {
      return ((Grib2Pds.PdsAerosol) pds).getAerosolIntervalSizeType();
    }

    public double getAerSize1() {
      return ((Grib2Pds.PdsAerosol) pds).getAerosolSize1() * 10e6;
    } // microns

    public double getAerSize2() {
      return ((Grib2Pds.PdsAerosol) pds).getAerosolSize2() * 10e6;
    } // microns

    public double getAerIntWavelType() {
      return ((Grib2Pds.PdsAerosol) pds).getAerosolIntervalWavelengthType();
    }

    public double getAerWavel1() {
      return ((Grib2Pds.PdsAerosol) pds).getAerosolWavelength1();
    }

    public double getAerWavel2() {
      return ((Grib2Pds.PdsAerosol) pds).getAerosolWavelength2();
    }

    ///////////////////////////////
    /* Ensembles
    public int getPertN() {
      Grib2Pds.PdsEnsemble pdsi = (Grib2Pds.PdsEnsemble) pds;
      int v = pdsi.getPerturbationNumber();
      if (v == GribNumbers.UNDEFINED) v = -1;
      return v;
    }

    public int getNForecastsInEns() {
      Grib2Pds.PdsEnsemble pdsi = (Grib2Pds.PdsEnsemble) pds;
      int v = pdsi.getNumberEnsembleForecasts();
      if (v == GribNumbers.UNDEFINED) v = -1;
      return v;
    }

    public int getPertType() {
      Grib2Pds.PdsEnsemble pdsi = (Grib2Pds.PdsEnsemble) pds;
      int v = pdsi.getPerturbationType();
      return (v == GribNumbers.UNDEFINED) ? -1 : v;
    }

    /////////////////////////////////
    // Probability

    public String getProbLimits() {
      Grib2Pds.PdsProbability pdsi = (Grib2Pds.PdsProbability) pds;
      double v = pdsi.getProbabilityLowerLimit();
      if (v == GribNumbers.UNDEFINEDD) return "";
      else return pdsi.getProbabilityLowerLimit() + "-" + pdsi.getProbabilityUpperLimit();
    }  */

  }

  class PdsBeanInfo extends SimpleBeanInfo {
    PropertyDescriptor[] properties;

    PdsBeanInfo(Grib2Pds pds) {
      ArrayList<PropertyDescriptor> props = new ArrayList<PropertyDescriptor>(40);

      Class cl = Grib2RecordBean.class;
      try {
        props.add(new PropertyDescriptor("dataPos", cl, "getDataPos", null));
        props.add(new PropertyDescriptor("file", cl, "getFile", null));
        props.add(new PropertyDescriptor("forecastDate", cl, "getForecastDate", null));
        props.add(new PropertyDescriptor("forecastTime", cl, "getForecastTime", null));
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
