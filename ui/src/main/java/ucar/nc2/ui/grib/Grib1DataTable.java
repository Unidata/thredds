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

import thredds.featurecollection.FeatureCollectionConfig;
import thredds.inventory.CollectionAbstract;
import thredds.inventory.MCollection;
import thredds.inventory.MFile;
import ucar.ma2.DataType;
import ucar.nc2.grib.GribData;
import ucar.nc2.grib.collection.Grib1Iosp;
import ucar.nc2.grib.grib1.*;
import ucar.nc2.grib.grib1.tables.Grib1Customizer;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.ui.widget.*;
import ucar.nc2.ui.widget.PopupMenu;
import ucar.nc2.util.Misc;
import ucar.util.prefs.PreferencesExt;
import ucar.util.prefs.ui.BeanTable;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.*;
import java.nio.ByteOrder;
import java.util.*;
import java.util.List;

/**
 * Describe
 *
 * @author caron
 * @since 8/12/2014
 */
public class Grib1DataTable extends JPanel {
  static private final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(Grib2DataPanel.class);

  private PreferencesExt prefs;

  private BeanTable param1BeanTable, record1BeanTable;
  private JSplitPane split, split2;

  private TextHistoryPane infoPopup, infoPopup2, drsInfo;
  private IndependentWindow infoWindow, infoWindow2;
  private FileManager fileChooser;

  public Grib1DataTable(PreferencesExt prefs) {
    this.prefs = prefs;

    PopupMenu varPopup;

    ////////////////
    param1BeanTable = new BeanTable(Grib1ParameterBean.class, (PreferencesExt) prefs.node("Param1Bean"), false,
            "UniquePDSVariables", "from Grib2Input.getRecords()", null);
    param1BeanTable.addListSelectionListener(new ListSelectionListener() {
      public void valueChanged(ListSelectionEvent e) {
        Grib1ParameterBean pb = (Grib1ParameterBean) param1BeanTable.getSelectedBean();
        if (pb != null)
          record1BeanTable.setBeans(pb.getRecordBeans());
      }
    });

    varPopup = new PopupMenu(param1BeanTable.getJTable(), "Options");
    varPopup.addAction("Show raw PDS bytes", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        Grib1ParameterBean pb = (Grib1ParameterBean) param1BeanTable.getSelectedBean();
        if (pb != null) {
          Formatter f = new Formatter();
          Grib1CollectionPanel.showRawPds(pb.pds, f);
          infoPopup2.setText(f.toString());
          infoPopup2.gotoTop();
          infoWindow2.show();
        }
      }
    });
    varPopup.addAction("Show processed PDS", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        Grib1ParameterBean pbean = (Grib1ParameterBean) param1BeanTable.getSelectedBean();
        if (pbean != null) {
          Formatter f = new Formatter();
          pbean.pds.showPds(cust, f);
          infoPopup2.setText(f.toString());
          infoPopup2.gotoTop();
          infoWindow2.show();
        }
      }
    });

    record1BeanTable = new BeanTable(Grib1RecordBean.class, (PreferencesExt) prefs.node("Record2Bean"), false,
            "DataRepresentation", "from Grib2Input.getRecords()", null);
    varPopup = new PopupMenu(record1BeanTable.getJTable(), "Options");

    varPopup.addAction("Show raw PDS bytes", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        Grib1RecordBean bean = (Grib1RecordBean) record1BeanTable.getSelectedBean();
        if (bean != null) {
          Formatter f = new Formatter();
          Grib1CollectionPanel.showRawPds(bean.pds, f);
          infoPopup2.setText(f.toString());
          infoPopup2.gotoTop();
          infoWindow2.show();
        }
      }
    });

    varPopup.addAction("Show Complete Grib1 Record", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        Grib1RecordBean bean = (Grib1RecordBean) record1BeanTable.getSelectedBean();
        if (bean != null) {
          Formatter f = new Formatter();
          String filename = fileList.get(bean.gr.getFile()).getPath();
          Grib1CollectionPanel.showCompleteRecord(cust, bean.gr, filename, f);
          infoPopup2.setText(f.toString());
          infoPopup2.gotoTop();
          infoWindow2.show();
        }
      }
    });

    varPopup.addAction("Show Data.Info", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        Grib1RecordBean bean = (Grib1RecordBean) record1BeanTable.getSelectedBean();
        if (bean != null) {
          Formatter f = new Formatter();
          bean.showDataRecord(f);
          infoPopup2.setText(f.toString());
          infoPopup2.gotoTop();
          infoWindow2.show();
        }
      }
    });

    varPopup.addAction("Compare Grib1 Records", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        List list = record1BeanTable.getSelectedBeans();
        if (list.size() == 2) {
          Grib1RecordBean bean1 = (Grib1RecordBean) list.get(0);
          Grib1RecordBean bean2 = (Grib1RecordBean) list.get(1);
          Formatter f = new Formatter();
          compare(bean1, bean2, f);
          infoPopup2.setText(f.toString());
          infoPopup2.gotoTop();
          infoWindow2.show();
        }
      }
    });

    varPopup.addAction("Compare Data", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        List list = record1BeanTable.getSelectedBeans();
        if (list.size() == 2) {
          Grib1RecordBean bean1 = (Grib1RecordBean) list.get(0);
          Grib1RecordBean bean2 = (Grib1RecordBean) list.get(1);
          Formatter f = new Formatter();
          compareData(bean1, bean2, f);
          infoPopup2.setText(f.toString());
          infoPopup2.gotoTop();
          infoWindow2.show();
        }
      }
    });

    varPopup.addAction("Show Data Cubic Interpolation", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        Grib1RecordBean bean = (Grib1RecordBean) record1BeanTable.getSelectedBean();
        if (bean != null) {
          Formatter f = new Formatter();
          showData(bean, true, GribData.InterpolationMethod.cubic, f);
          infoPopup2.setText(f.toString());
          infoPopup2.gotoTop();
          infoWindow2.show();
        }
      }
    });

    varPopup.addAction("Show Data Max/Min", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        Grib1RecordBean bean = (Grib1RecordBean) record1BeanTable.getSelectedBean();
        if (bean != null) {
          Formatter f = new Formatter();
          showData(bean, false, GribData.InterpolationMethod.none, f);
          infoPopup2.setText(f.toString());
          infoPopup2.gotoTop();
          infoWindow2.show();
        }
      }
    });

    varPopup.addAction("Show Data Raw", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        Grib1RecordBean bean = (Grib1RecordBean) record1BeanTable.getSelectedBean();
        if (bean != null) {
          Formatter f = new Formatter();
          showData(bean, true, GribData.InterpolationMethod.none, f);
          infoPopup2.setText(f.toString());
          infoPopup2.gotoTop();
          infoWindow2.show();
        }
      }
    });

    varPopup.addAction("Show Data Linear Interpretation", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        Grib1RecordBean bean = (Grib1RecordBean) record1BeanTable.getSelectedBean();
        if (bean != null) {
          Formatter f = new Formatter();
          showData(bean, true, GribData.InterpolationMethod.none, f);
          infoPopup2.setText(f.toString());
          infoPopup2.gotoTop();
          infoWindow2.show();
        }
      }
    });

    varPopup.addAction("Extract GribRecord to File", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        List beans = record1BeanTable.getSelectedBeans();
        if (beans.size() > 0)
          writeToFile(beans);
      }
    });

    varPopup.addAction("Show Bitmap", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        Grib1RecordBean bean = (Grib1RecordBean) record1BeanTable.getSelectedBean();
        if (bean != null) {
          Formatter f = new Formatter();
          try {
            showBitmap(bean, f);
          } catch (IOException e1) {
            e1.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
          }
          infoPopup2.setText(f.toString());
          infoPopup2.gotoTop();
          infoWindow2.show();
        }
      }
    });

    varPopup.addAction("Compute Scale/offset of data", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        Grib1RecordBean bean = (Grib1RecordBean) record1BeanTable.getSelectedBean();
        if (bean != null) {
          Formatter f = new Formatter();
          GribData.calcScaleOffset(bean, f);
          infoPopup2.setText(f.toString());
          infoPopup2.gotoTop();
          infoWindow2.show();
        }
      }
    });

    //gds2Table = new BeanTable(Gds2Bean.class, (PreferencesExt) prefs.node("Gds2Bean"), false, "Grib2GridDefinitionSection", "unique from Grib1Records");
    //varPopup = new PopupMenu(gds2Table.getJTable(), "Options");

    /////////////////////////////////////////
    // the info windows
    infoPopup = new TextHistoryPane();
    infoWindow = new IndependentWindow("Extra Information", BAMutil.getImage("netcdfUI"), infoPopup);
    infoWindow.setBounds((Rectangle) prefs.getBean("InfoWindowBounds", new Rectangle(300, 300, 500, 300)));

    infoPopup2 = new TextHistoryPane();
    infoWindow2 = new IndependentWindow("Extra Information", BAMutil.getImage("netcdfUI"), infoPopup2);
    infoWindow2.setBounds((Rectangle) prefs.getBean("InfoWindowBounds2", new Rectangle(300, 300, 500, 300)));

    drsInfo = new TextHistoryPane();

    setLayout(new BorderLayout());

    split2 = new JSplitPane(JSplitPane.VERTICAL_SPLIT, false, param1BeanTable, record1BeanTable);
    split2.setDividerLocation(prefs.getInt("splitPos2", 800));

    split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, false, split2, drsInfo);
    split.setDividerLocation(prefs.getInt("splitPos", 500));

    add(split, BorderLayout.CENTER);

  }

  public void save() {
    param1BeanTable.saveState(false);
    record1BeanTable.saveState(false);
    prefs.putBeanObject("InfoWindowBounds", infoWindow.getBounds());
    prefs.putBeanObject("InfoWindowBounds2", infoWindow2.getBounds());
    if (split != null) prefs.putInt("splitPos", split.getDividerLocation());
    if (split2 != null) prefs.putInt("splitPos2", split2.getDividerLocation());
  }

  ///////////////////////////////////////////////

  private String spec;
  private MCollection dcm;
  private List<MFile> fileList;
  private Grib1Customizer cust;
  private FeatureCollectionConfig config = new FeatureCollectionConfig(); // default values

  public void setCollection(String spec) throws IOException {
    this.spec = spec;
    this.cust = null;

    Formatter f = new Formatter();
    this.dcm = scanCollection(spec, f);
    if (dcm == null) {
      javax.swing.JOptionPane.showMessageDialog(this, "Collection is null\n" + f.toString());
      return;
    }

    Map<Integer, Grib1ParameterBean> pdsSet = new HashMap<>();
    Map<Integer, Grib1SectionGridDefinition> gdsSet = new HashMap<>();

    java.util.List<Grib1ParameterBean> params = new ArrayList<>();

    int fileno = 0;
    for (MFile mfile : fileList) {
      f.format("%n %s%n", mfile.getPath());
      try (ucar.unidata.io.RandomAccessFile raf = new ucar.unidata.io.RandomAccessFile(mfile.getPath(), "r")) {
        raf.order(ByteOrder.BIG_ENDIAN);
        processGribFile(mfile, fileno++, raf, pdsSet, gdsSet, params, f);
      }
    }
    param1BeanTable.setBeans(params);
  }

  private void processGribFile(MFile mfile, int fileno, ucar.unidata.io.RandomAccessFile raf,
                               Map<Integer, Grib1ParameterBean> pdsSet,
                               Map<Integer, Grib1SectionGridDefinition> gdsSet,
                               List<Grib1ParameterBean> params, Formatter f) throws IOException {

    Grib1Index index = new Grib1Index();
    if (!index.readIndex(mfile.getPath(), mfile.getLastModified())) {
      index.makeIndex(mfile.getPath(), null);
    }

    for (Grib1SectionGridDefinition gds : index.getGds()) {
      int hash = gds.getGDS().hashCode();
      if (gdsSet.get(hash) == null)
        gdsSet.put(hash, gds);
    }

    for (Grib1Record gr : index.getRecords()) {
      if (cust == null)
        cust = Grib1Customizer.factory(gr, null);

      gr.setFile(fileno);

    // public static int cdmVariableHash(Grib1Customizer cust, Grib1Record gr, int gdsHash, boolean useTableVersion, boolean intvMerge, boolean useCenter) {

      int id = Grib1Variable.cdmVariableHash(cust, gr, 0, FeatureCollectionConfig.useTableVersionDef, FeatureCollectionConfig.intvMergeDef, FeatureCollectionConfig.useCenterDef);
      Grib1ParameterBean bean = pdsSet.get(id);
      if (bean == null) {
        bean = new Grib1ParameterBean(gr);
        pdsSet.put(id, bean);
        params.add(bean);
      }
      bean.addRecord(gr, raf);
    }
  }

  private MCollection scanCollection(String spec, Formatter f) {
    MCollection dc = null;
    try {
      dc = CollectionAbstract.open(spec, spec, null, f);
      fileList = (List<MFile>) Misc.getList(dc.getFilesSorted());
      return dc;

    } catch (Exception e) {
      StringWriter sw = new StringWriter(5000);
      e.printStackTrace(new PrintWriter(sw));
      f.format(sw.toString());
      if (dc != null) dc.close();
      return null;
    }
  }

  /* public boolean writeIndex(Formatter f) throws IOException {
    MCollection dcm = scanCollection(spec, f);

    if (fileChooser == null)
      fileChooser = new FileManager(null, null, null, (PreferencesExt) prefs.node("FileManager"));
    String name = dcm.getCollectionName();
    int pos = name.lastIndexOf('/');
    if (pos < 0) pos = name.lastIndexOf('\\');
    if (pos > 0) name = name.substring(pos + 1);
    File def = new File(dcm.getRoot(), name + GribCollection.NCX_IDX);
    String filename = fileChooser.chooseFilename(def);
    if (filename == null) return false;
    if (!filename.endsWith(GribCollection.NCX_IDX))
      filename += GribCollection.NCX_IDX;
    File idxFile = new File(filename);

    Grib2CollectionBuilder.makeIndex(dcm, new Formatter(), logger);
    return true;
  }     */

  public void showCollection(Formatter f) {
    if (dcm == null) {
      if (spec == null) return;
      dcm = scanCollection(spec, f);
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
    for (Object o : param1BeanTable.getBeans()) {
      Grib1ParameterBean p = (Grib1ParameterBean) o;
      int gdsHash = p.gr.getGDSsection().getGDS().hashCode();

      Set<Integer> group = groups.get(gdsHash);
      if (group == null) {
        group = new TreeSet<>();
        groups.put(gdsHash, group);
      }
      for (Grib1RecordBean r : p.getRecordBeans())
        group.add(r.gr.getFile());
    }
  }

  public void checkProblems(Formatter f) {
    checkDuplicates(f);
    //checkRuntimes(f);
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
       List<Grib1RecordBean> records = pb.getRecordBeans();
       for (Grib1RecordBean record : records) {
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

    List<Grib1ParameterBean> params = param1BeanTable.getBeans();
    for (Grib1ParameterBean param : params) {
      for (Grib1RecordBean record : param.getRecordBeans()) {
        CalendarDate d = record.gr.getReferenceDate();
        DateCount dc = dateMap.get(d);
        if (dc == null) {
          dc = new DateCount(d);
          dateMap.put(d, dc);
        }
        dc.count++;

        Grib1SectionProductDefinition pdss = record.gr.getPDSsection();
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
        Grib1RecordBean bean = (Grib1RecordBean) o;
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

        Grib1SectionIndicator is = bean.gr.getIs();
        int size = (int) (is.getMessageLength());
        long startPos = is.getStartPos();
        if (startPos < 0) {
          JOptionPane.showMessageDialog(Grib1DataTable.this, "Old index does not have message start - record not written");
        }

        byte[] rb = new byte[size];
        raf.seek(startPos);
        raf.readFully(rb);
        fos.write(rb);
        n++;
      }

      JOptionPane.showMessageDialog(Grib1DataTable.this, filename + ": " + n + " records successfully written, append=" + append);

    } catch (Exception ex) {
      JOptionPane.showMessageDialog(Grib1DataTable.this, "ERROR: " + ex.getMessage());
      ex.printStackTrace();

    } finally {
      try {
        if (fos != null) fos.close();
        if (raf != null) raf.close();
      } catch (IOException ioe) {
      }
    }
  }

  private void compareData(Grib1RecordBean bean1, Grib1RecordBean bean2, Formatter f) {
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


  private void compare(Grib1RecordBean bean1, Grib1RecordBean bean2, Formatter f) {
    Grib1CollectionPanel.compare(bean1.gr.getPDSsection(), bean2.gr.getPDSsection(), f);
    Grib1CollectionPanel.compare(bean1.gr.getGDSsection(), bean2.gr.getGDSsection(), f);
  }

  private void showData(Grib1RecordBean bean1, boolean showData, GribData.InterpolationMethod method, Formatter f) {
    float[] data;
    try {
      data = bean1.readData(method);
    } catch (IOException e) {
      f.format("IOException %s", e.getMessage());
      return;
    }


    float max = -Float.MAX_VALUE;
    float min = Float.MAX_VALUE;
    for (float fd : data) {
      if (showData) f.format("%f%n", fd);
      if (Float.isNaN(fd)) continue;
      max = Math.max(fd, max);
      min = Math.min(fd, min);
    }
    f.format("max = %f%n", max);
    f.format("min = %f%n", min);
  }

  void showBitmap(Grib1RecordBean bean1, Formatter f) throws IOException {
    byte[] bitmap;
    ucar.unidata.io.RandomAccessFile raf = null;
    try {
      raf = bean1.getRaf();
      Grib1SectionBitMap bms = bean1.gr.getBitMapSection();
      f.format("%s%n", bms);
      bitmap = bms.getBitmap(raf);
    } finally {
      if (raf != null) raf.close();
    }

    if (bitmap == null) {
      f.format(" no bitmap%n");
      return;
    }

    int count = 0;
    int bits = 0;
    for (byte b : bitmap) {
      short s = DataType.unsignedByteToShort(b);
      bits += Long.bitCount(s);
      f.format("%8s", Long.toBinaryString(s));
      if (++count % 10 == 0)
        f.format("%n");
    }
    f.format("%n%n#bits on = %d%n", bits);
    f.format("bitmap size = %d%n", 8 * count);
  }


  ////////////////////////////////////////////////////////////////////////////

  public class Grib1ParameterBean {
    Grib1Record gr;
    Grib1SectionIndicator id;
    Grib1SectionProductDefinition pds;
    List<Grib1RecordBean> records;
    Grib1Parameter param;

    // no-arg constructor

    public Grib1ParameterBean() {
    }

    public Grib1ParameterBean(Grib1Record r) throws IOException {
      this.gr = r;

      // long refTime = r.getId().getReferenceDate().getMillis();
      pds = r.getPDSsection();
      id = r.getIs();
      records = new ArrayList<>();
      param = cust.getParameter(pds.getCenter(), pds.getSubCenter(), pds.getTableVersion(), pds.getParameterNumber());
      //gdsKey = r.getGDSsection().calcCRC();
    }

    void addRecord(Grib1Record r, ucar.unidata.io.RandomAccessFile raf) throws IOException {
      records.add(new Grib1RecordBean(r, raf));
    }

    List<Grib1RecordBean> getRecordBeans() {
      return records;
    }


    public String getName() {
      if (param == null) return null;
      return Grib1Iosp.makeVariableName(cust, config.gribConfig, pds);
    }

    public String getUnit() {
      return (param == null) ? null : param.getUnit();
    }

    public int getParamNo() {
      return pds.getParameterNumber();
    }

    public int getNRecords() {
      return records.size();
    }

    public int getLevelType() {
      return pds.getLevelType();
    }

    public final String getLevelName() {
      Grib1ParamLevel plevel = cust.getParamLevel(pds);
      return plevel.getNameShort();
    }

    public String getNBits() {
      calcBits();
      if (minBits == maxBits) return Integer.toString(minBits);
      return minBits+"-"+maxBits;
    }

    public String getBinScale() {
      calcBits();
      if (minBinscale == maxBinscale) return Integer.toString(minBinscale);
      return minBinscale+","+maxBinscale;
    }

    public String getDecScale() {
      calcBits();
      if (minDecscale == maxDecscale) return Integer.toString(minDecscale);
      return minDecscale+","+maxDecscale;
    }

    public float getAvgBits() {
      calcBits();
      return avgbits;
    }

    public float getCompress() {
      calcBits();
      return compress;
    }

    private int minBits, maxBits;
    private int minBinscale, maxBinscale;
    private int minDecscale, maxDecscale;
    private float nbits = -1;
    private float avgbits;
    private float compress;
    private void calcBits() {
      if (nbits >= 0) return;
      nbits = 0;
      int count = 0;
      minBits = Integer.MAX_VALUE;
      minBinscale = Integer.MAX_VALUE;
      minDecscale = Integer.MAX_VALUE;
      for (Grib1RecordBean bean : records) {
        minBits = Math.min(minBits, bean.getNBits());
        maxBits = Math.max(maxBits, bean.getNBits());
        minBinscale = Math.min(minBinscale, bean.getBinScale());
        maxBinscale = Math.max(maxBinscale, bean.getBinScale());
        minDecscale = Math.min(minDecscale, bean.getDecScale());
        maxDecscale = Math.max(maxDecscale, bean.getDecScale());
        nbits += bean.getNBits();
        avgbits += bean.getAvgBits();
        count++;
      }
      compress = nbits / avgbits;
      if (count > 0) {
        nbits /= count;
        avgbits /= count;
      }
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

  public class Grib1RecordBean implements GribData.Bean {
    Grib1Record gr;
    Grib1SectionGridDefinition gds;
    Grib1SectionProductDefinition pds;
    Grib1ParamLevel plevel;
    Grib1ParamTime ptime;
    GribData.Info info;
    Grib1Gds gdss;

    double minimum, maximum, scale;

    public Grib1RecordBean() {
    }

    public Grib1RecordBean(Grib1Record m, ucar.unidata.io.RandomAccessFile raf) throws IOException {
      this.gr = m;
      gds = gr.getGDSsection();
      pds = gr.getPDSsection();
      gdss = gds.getGDS();
      plevel = cust.getParamLevel(pds);
      ptime = gr.getParamTime(cust);

      info = gr.getBinaryDataInfo(raf);

      double pow10 =  Math.pow(10.0, -getDecScale());        // 1/10^D
      minimum = (float) (pow10 * info.referenceValue);      // R / 10^D
      scale = (float) (pow10 * Math.pow(2.0, getBinScale()));  // 2^E / 10^D

      double maxPacked = Math.pow(2.0, getNBits()) - 1;
      maximum = minimum +  scale * maxPacked;
    }

    public String getTimeCoord() {
      if (ptime.isInterval()) {
        int[] intv = ptime.getInterval();
        return intv[0] + "-" + intv[1] + "("+ptime.getIntervalSize()+")";
      }
      return Integer.toString(ptime.getForecastTime());
    }

    public String getLevel() {
      if (cust.isLayer(pds.getLevelType())) {
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

    public float[] readData() throws IOException {
        return readData(GribData.getInterpolationMethod()); // use default interpolation
    }

    public float[] readData(GribData.InterpolationMethod method) throws IOException {
      int fileno = gr.getFile();
      MFile mfile = fileList.get(fileno);
      try (ucar.unidata.io.RandomAccessFile raf = new ucar.unidata.io.RandomAccessFile(mfile.getPath(), "r")) {
        raf.order(ucar.unidata.io.RandomAccessFile.BIG_ENDIAN);
        return gr.readDataRaw(raf, method);              // use interpolation passed in
      }
    }

    public int getNBits() {
      return info.numberOfBits;
    }

    public long getDataLength() {
      return info.dataLength;
    }

    @Override
    public long getMsgLength() {
      return info.msgLength;
          }

    public int getBinScale() {
      return info.binaryScaleFactor;
    }

    public int getDecScale() {
      return info.decimalScaleFactor;
    }

    public double getMinimum() {
      return minimum;
    }

    public double getMaximum() {
      return maximum;
    }

    public double getScale() {
      return scale;
    }

    public int getNDataPoints() {
      return info.ndataPoints;
    }

    public int getNPoints() {
       return info.nPoints;
     }

    public String getDataType() {
      return info.getDataTypeS() ;
    }

    public String getGridPoint() {
      return info.getGridPointS() ;
    }

    public String getPacking() {
      return info.getPackingS() ;
    }

    public float getAvgBits() {
      float len = getDataLength();
      int npts = gdss.getNpts();
      return (npts == 0) ? 0 : len * 8 / npts;
    }

    void showDataRecord(Formatter f) {
      int fileno = gr.getFile();
      MFile mfile = fileList.get(fileno);
      try (ucar.unidata.io.RandomAccessFile raf = new ucar.unidata.io.RandomAccessFile(mfile.getPath(), "r")) {
        raf.order(ucar.unidata.io.RandomAccessFile.BIG_ENDIAN);
        gr.showDataInfo(raf, f);
      } catch (IOException e) {
        e.printStackTrace();
        logger.error("showDataRecord", e);
      }
    }

    ucar.unidata.io.RandomAccessFile getRaf() throws IOException {
      int fileno = gr.getFile();
      MFile mfile = fileList.get(fileno);
      return new ucar.unidata.io.RandomAccessFile(mfile.getPath(), "r");
    }
  }

  public static void main(String arg[]) {
    float add_offset = 143.988f;
    float scale_factor = 0.000614654f;
    float fd = 339.029f;

    System.out.printf("res = %f%n", scale_factor / 2);

    int packed_data = Math.round((fd - add_offset) / scale_factor);   // nint((unpacked_data_value - add_offset) / scale_factor)
    float unpacked_data = packed_data * scale_factor + add_offset;
    float diff = Math.abs(fd-unpacked_data);
    System.out.printf("***   org=%f, packed_data=%d unpacked=%f diff = %f%n",fd, packed_data, unpacked_data, diff);

    packed_data++;
    unpacked_data = packed_data * scale_factor + add_offset;
    diff = Math.abs(fd-unpacked_data);
    System.out.printf("***   org=%f, packed_data+1=%d unpacked=%f diff = %f%n",fd, packed_data, unpacked_data, diff);

    packed_data -=2;
    unpacked_data = packed_data * scale_factor + add_offset;
    diff = Math.abs(fd-unpacked_data);
    System.out.printf("***   org=%f, packed_data-1=%d unpacked=%f diff = %f%n",fd, packed_data, unpacked_data, diff);

  }

}