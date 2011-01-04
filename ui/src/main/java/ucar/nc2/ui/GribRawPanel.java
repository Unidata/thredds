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

import ucar.grib.GribGridRecord;
import ucar.nc2.NetcdfFile;
import ucar.nc2.ui.widget.FileManager;
import ucar.grib.GribPds;
import ucar.grib.NoValidGribException;
import ucar.grib.grib1.*;
import ucar.ma2.DataType;
import ucar.nc2.iosp.grib.tables.GribTemplate;
import ucar.nc2.ui.widget.*;
import ucar.nc2.ui.widget.PopupMenu;
import ucar.nc2.units.DateFormatter;
import ucar.nc2.util.Misc;
import ucar.unidata.io.KMPMatch;
import ucar.util.prefs.PreferencesExt;
import ucar.util.prefs.ui.BeanTableSorted;
import ucar.unidata.io.RandomAccessFile;
import ucar.grib.grib2.*;

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
 * ToolsUI/Iosp/Grib - low level scanning
 *
 * @author caron
 * @since Aug 15, 2008
 */
public class GribRawPanel extends JPanel {
  private static final KMPMatch matcher = new KMPMatch("GRIB".getBytes());

  private PreferencesExt prefs;

  private BeanTableSorted gds2Table, param2BeanTable, record2BeanTable;
  private BeanTableSorted gds1Table, param1BeanTable, record1BeanTable;
  private JSplitPane split, split2;

  private TextHistoryPane infoPopup, infoPopup2, infoPopup3;
  private IndependentWindow infoWindow, infoWindow2, infoWindow3;
  private FileManager fileChooser;

  private RandomAccessFile raf = null;
  private Map<String, GribTemplate> productTemplates = null;

  private DateFormatter df = new DateFormatter();

  public GribRawPanel(PreferencesExt prefs) {
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
    varPopup.addAction("Show raw PDS", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        Grib2ParameterBean pb = (Grib2ParameterBean) param2BeanTable.getSelectedBean();
        if (pb != null) {
          infoPopup2.setText(pb.toString());
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
    varPopup.addAction("Run accum algorithm", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        Grib2ParameterBean pb = (Grib2ParameterBean) param2BeanTable.getSelectedBean();
        if (pb != null) {
          // infoPopup3.setText(doAccumAlgo(pb));
          infoPopup3.gotoTop();
          infoWindow3.showIfNotIconified();
        }
      }
    });

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
          infoPopup.setText(bean.showComplete());
          infoPopup.gotoTop();
          infoWindow.showIfNotIconified();
        }
      }
    });

    varPopup.addAction("Show Processed GridRecord", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        Grib2RecordBean bean = (Grib2RecordBean) record2BeanTable.getSelectedBean();
        if (bean != null) {
          infoPopup2.setText(bean.toProcessedString());
          infoPopup2.gotoTop();
          infoWindow2.showIfNotIconified();
        }
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

    varPopup.addAction("Save GribRecord to File", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        List list = record2BeanTable.getSelectedBeans();
        if (list.size() > 0) {
          writeToFile(list);
        }
      }
    });

    gds2Table = new BeanTableSorted(Gds2Bean.class, (PreferencesExt) prefs.node("Gds2Bean"), false, "Grib2GridDefinitionSection", "unique from Grib2Records");
    varPopup = new PopupMenu(gds2Table.getJTable(), "Options");

    varPopup.addAction("Show processed GDS", new AbstractAction() {
       public void actionPerformed(ActionEvent e) {
         Gds2Bean bean = (Gds2Bean) gds2Table.getSelectedBean();
         infoPopup.setText(bean.gds.toString());
         infoWindow.setVisible(true);
       }
     });

    varPopup.addAction("Compare GDS", new AbstractAction() {
       public void actionPerformed(ActionEvent e) {
         List list = gds2Table.getSelectedBeans();
         if (list.size() == 2) {
           Gds2Bean bean1 = (Gds2Bean) list.get(0);
           Gds2Bean bean2 = (Gds2Bean) list.get(1);
           Formatter f = new Formatter();
           compare(bean1.gds, bean2.gds, f);
           infoPopup2.setText(f.toString());
           infoPopup2.gotoTop();
           infoWindow2.showIfNotIconified();
         }
       }
     });

    varPopup.addAction("Show Products that use GDS", new AbstractAction() {
       public void actionPerformed(ActionEvent e) {
         Gds2Bean bean = (Gds2Bean) gds2Table.getSelectedBean();
         Formatter f = new Formatter();
         for (Object o : param2BeanTable.getBeans()) {
           Grib2ParameterBean p = (Grib2ParameterBean) o;
           if (p.gdsKey == bean.getKey())
             f.format(" %s%n", p.getName());
         }
         infoPopup2.setText(f.toString());
         infoWindow.setVisible(true);
       }
     });



    ////////////////

    param1BeanTable = new BeanTableSorted(Grib1ParameterBean.class, (PreferencesExt) prefs.node("Param1Bean"), false, "Grib1PDSVariables", "from Grib1Input.getRecords()");
    param1BeanTable.addListSelectionListener(new ListSelectionListener() {
      public void valueChanged(ListSelectionEvent e) {
        Grib1ParameterBean pb = (Grib1ParameterBean) param1BeanTable.getSelectedBean();
        if (pb != null)
          record1BeanTable.setBeans(pb.getRecordBeans());
      }
    });

    varPopup = new PopupMenu(param1BeanTable.getJTable(), "Options");
    varPopup.addAction("Show raw PDS", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        Grib1ParameterBean pb = (Grib1ParameterBean) param1BeanTable.getSelectedBean();
        if (pb != null) {
          infoPopup2.setText(pb.toRawString());
          infoPopup2.gotoTop();
          infoWindow2.showIfNotIconified();
        }
      }
    });
    varPopup.addAction("Show processed PDS", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        Grib1ParameterBean pb = (Grib1ParameterBean) param1BeanTable.getSelectedBean();
        if (pb != null) {
          infoPopup3.setText(pb.toProcessedString());
          infoPopup3.gotoTop();
          infoWindow3.showIfNotIconified();
        }
      }
    });

    record1BeanTable = new BeanTableSorted(Grib1RecordBean.class, (PreferencesExt) prefs.node("Record1Bean"), false, "Grib1Record", "from Grib1Input.getRecords()");
    varPopup = new PopupMenu(record1BeanTable.getJTable(), "Options");

    varPopup.addAction("Show raw PDS bytes", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        Grib1RecordBean bean = (Grib1RecordBean) record1BeanTable.getSelectedBean();
        if (bean != null) {
          infoPopup.setText(bean.toRawString());
          infoPopup.gotoTop();
          infoWindow.showIfNotIconified();
        }
      }
    });

    varPopup.addAction("Show Raw GridRecord", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        Grib1RecordBean bean = (Grib1RecordBean) record1BeanTable.getSelectedBean();
        if (bean != null) {
          infoPopup.setText(bean.toRawString());
          infoPopup.gotoTop();
          infoWindow.showIfNotIconified();
        }
      }
    });
    varPopup.addAction("Show Processed Grib1Record", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        Grib1RecordBean bean = (Grib1RecordBean) record1BeanTable.getSelectedBean();
        if (bean != null) {
          infoPopup2.setText(bean.toProcessedString());
          infoPopup2.gotoTop();
          infoWindow2.showIfNotIconified();
        }
      }
    });

    gds1Table = new BeanTableSorted(Gds1Bean.class, (PreferencesExt) prefs.node("Gds1Bean"), false, "Grib1GridDefinitionSection", "unique from Grib1Records");

    varPopup = new PopupMenu(gds1Table.getJTable(), "Options");
    varPopup.addAction("Show GDS", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        Gds1Bean bean = (Gds1Bean) gds1Table.getSelectedBean();
        if (bean != null) {
          ByteArrayOutputStream os = new ByteArrayOutputStream();
          PrintStream ps = new PrintStream(os);
          Grib1Dump.printGDS(bean.gds, null, ps);
          infoPopup.setText(os.toString());
          infoWindow.setVisible(true);
        }
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
  }

  public void showInfo(Formatter f) {
  }

  public void save() {
    gds2Table.saveState(false);
    param2BeanTable.saveState(false);
    record2BeanTable.saveState(false);
    gds1Table.saveState(false);
    param1BeanTable.saveState(false);
    record1BeanTable.saveState(false);
    prefs.putBeanObject("InfoWindowBounds", infoWindow.getBounds());
    prefs.putBeanObject("InfoWindowBounds2", infoWindow2.getBounds());
    prefs.putBeanObject("InfoWindowBounds3", infoWindow3.getBounds());
    if (split != null) prefs.putInt("splitPos", split.getDividerLocation());
    if (split2 != null) prefs.putInt("splitPos2", split2.getDividerLocation());
  }

  private void writeToFile(List<Grib2RecordBean> beans) {
    Grib2RecordBean first = beans.get(0);

    try {
      String defloc;
      String firstHeader = first.gr.getHeader();
      if (firstHeader != null) {
        firstHeader = firstHeader.split(" ")[0];
      }
      if (firstHeader == null) {
        defloc = (raf.getLocation() == null) ? "." : raf.getLocation();
        int pos = defloc.lastIndexOf(".");
        if (pos > 0)
          defloc = defloc.substring(0, pos);
      } else
        defloc = firstHeader;

      if (fileChooser == null)
        fileChooser = new FileManager(null, null, null, (PreferencesExt) prefs.node("FileManager"));

      String filename = fileChooser.chooseFilenameToSave(defloc + ".grib2");
      if (filename == null) return;
      File file = new File(filename);
      FileOutputStream fos = new FileOutputStream(file);

      for (Grib2RecordBean bean : beans) {
        Grib2IndicatorSection is = bean.gr.getIs();
        if (is.getStartPos() < 0) continue;

        int size = (int)(is.getEndPos() - is.getStartPos());
        byte[] rb = new byte[size];
        raf.seek(is.getStartPos());
        raf.readFully(rb);

        String header = bean.gr.getHeader();
        if (header != null && !header.contains("GRIB"))  // ??
          fos.write(bean.gr.getHeader().getBytes());

        fos.write(rb);
      }
      fos.close();

      JOptionPane.showMessageDialog(GribRawPanel.this, filename + " successfully written");

    } catch (Exception ex) {
      JOptionPane.showMessageDialog(GribRawPanel.this, "ERROR: " + ex.getMessage());
      ex.printStackTrace();
    }
  }

  private void compare(Grib2RecordBean bean1, Grib2RecordBean bean2, Formatter f) {
    Grib2IndicatorSection is1 = bean1.gr.getIs();
    Grib2IndicatorSection is2 = bean2.gr.getIs();
    f.format("Indicator Section%n");
    if (is1.getGribEdition() != is2.getGribEdition())
      f.format("getGribEdition differs %d != %d %n", is1.getGribEdition(),is2.getGribEdition());
    if (is1.getDiscipline() != is2.getDiscipline())
      f.format("getDiscipline differs %d != %d %n", is1.getDiscipline(),is2.getDiscipline());
    if (is1.getGribLength() != is2.getGribLength())
      f.format("getGribLength differs %d != %d %n", is1.getGribLength(),is2.getGribLength());

    f.format("%nId Section%n");
    Grib2IdentificationSection id1 = bean1.id;
    Grib2IdentificationSection id2 = bean2.id;
    if (id1.getCenter_id() != id2.getCenter_id())
      f.format("Center_id differs %d != %d %n", id1.getCenter_id(),id2.getCenter_id());
    if (id1.getSubcenter_id() != id2.getSubcenter_id())
      f.format("Subcenter_id differs %d != %d %n", id1.getSubcenter_id(),id2.getSubcenter_id());
    if (id1.getMaster_table_version() != id2.getMaster_table_version())
      f.format("Master_table_version differs %d != %d %n", id1.getMaster_table_version(),id2.getMaster_table_version());
    if (id1.getLocal_table_version() != id2.getLocal_table_version())
      f.format("Local_table_version differs %d != %d %n", id1.getLocal_table_version(),id2.getLocal_table_version());
    if (id1.getProductStatus() != id2.getProductStatus())
      f.format("ProductStatus differs %d != %d %n", id1.getProductStatus(),id2.getProductStatus());
    if (id1.getProductType() != id2.getProductType())
      f.format("ProductType differs %d != %d %n", id1.getProductType(),id2.getProductType());
    if (id1.getRefTime() != id2.getRefTime())
      f.format("refTime differs %d != %d %n", id1.getRefTime(), id2.getRefTime());
    if (id1.getSignificanceOfRT() != id2.getSignificanceOfRT())
      f.format("getSignificanceOfRT differs %d != %d %n", id1.getSignificanceOfRT(),id2.getSignificanceOfRT());

    byte[] lus1 = bean1.gr.getLocalUseSection();
    byte[] lus2 = bean2.gr.getLocalUseSection();
    if (lus1 == null || lus2 == null) {
      if (lus1 == lus2)
        f.format("%nLus are both null%n");
      else
        f.format("%nLus are different %s != %s %n", lus1, lus2);
    } else {
      f.format("%nCompare LocalUseSection%n");
      compare( lus1, lus2, f);
    }
    
    compare( bean1.pdsv, bean2.pdsv, f);
    compare( bean1.gds, bean2.gds, f);
  }

  private void compare(Grib2GridDefinitionSection gds1, Grib2GridDefinitionSection gds2, Formatter f) {
    Grib2GDSVariables gdsv1 = gds1.getGdsVars();
    Grib2GDSVariables gdsv2 = gds2.getGdsVars();

    f.format("%nCompare Gds%n");
    byte[] raw1 = gdsv1.getGDSBytes();
    byte[] raw2 = gdsv2.getGDSBytes();
    compare( raw1, raw2, f);
  }

  private void compare(GribPds pds1, GribPds pds2, Formatter f) {
    f.format("%nCompare Pds%n");
    byte[] raw1 = pds1.getPDSBytes();
    byte[] raw2 = pds2.getPDSBytes();
    compare( raw1, raw2, f);
  }

  static public void compare(byte[] raw1, byte[] raw2, Formatter f) {
    if (raw1.length != raw2.length) {
      f.format("length 1= %3d != length 2=%3d%n", raw1.length, raw2.length);
    }
    int len = Math.min(raw1.length, raw2.length);

    for (int i = 0; i < len; i++) {
      if (raw1[i] != raw2[i])
        f.format(" %3d : %3d != %3d%n", i + 1, raw1[i], raw2[i]);
    }
    f.format("tested %d bytes %n", len);
  }

  static public void compare(float[] raw1, float[] raw2, Formatter f) {
    if (raw1.length != raw2.length) {
      f.format("compareFloat: length 1= %3d != length 2=%3d%n", raw1.length, raw2.length);
    }
    int len = Math.min(raw1.length, raw2.length);

    for (int i = 0; i < len; i++) {
      if ( !Misc.closeEnough(raw1[i], raw2[i]) && !Double.isNaN(raw1[i]) && !Double.isNaN(raw2[i]))
        f.format(" %5d : %3f != %3f%n", i, raw1[i], raw2[i]);
    }
    f.format("tested %d floats %n", len);
  }

  void compareData(Grib2RecordBean bean1, Grib2RecordBean bean2, Formatter f) {

    float[] data1 = null, data2 = null;
    try {
      //if (ggr1.getEdition() == 2) {
        Grib2Data g2read = new Grib2Data(raf);
        data1 =  g2read.getData(bean1.gr.getGdsOffset(),  bean1.gr.getPdsOffset(), bean1.id.getRefTime());
        data2 =  g2read.getData(bean2.gr.getGdsOffset(),  bean2.gr.getPdsOffset(), bean2.id.getRefTime());
      /* } else  {
        Grib1Data g1read = new Grib1Data(raf);
        data1 =  g1read.getData(ggr1.getGdsOffset(), ggr1.getPdsOffset(), ggr1.getDecimalScale(), ggr1.isBmsExists());
        data2 =  g1read.getData(ggr2.getGdsOffset(), ggr2.getPdsOffset(), ggr2.getDecimalScale(), ggr2.isBmsExists());
      }  */
    } catch (IOException e) {
      f.format("IOException %s", e.getMessage());
      return;
    }

    compare(data1, data2, f);
  }



   public void setGribFile(RandomAccessFile raf) throws Exception {
    this.raf = raf;

    raf.seek(0);
    if (!raf.searchForward(matcher, 8000)) return; // must find "GRIB" in first 8k
    raf.skipBytes(4);
    //  Read Section 0 Indicator Section to get Edition number
    Grib2IndicatorSection is = new Grib2IndicatorSection(raf);  // section 0
    int edition = is.getGribEdition();

    if (edition == 1)
      setGribFile1(raf);
    else if (edition == 2)
      setGribFile2(raf);
  }

  void setGribFile1(RandomAccessFile raf) throws IOException, NoValidGribException {
    split2 = new JSplitPane(JSplitPane.VERTICAL_SPLIT, false, param1BeanTable, record1BeanTable);
    split2.setDividerLocation(prefs.getInt("splitPos2", 800));

    split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, false, split2, gds1Table);
    split.setDividerLocation(prefs.getInt("splitPos", 500));

    removeAll();
    add(split, BorderLayout.CENTER);
    revalidate();

    Map<Integer, Grib1GridDefinitionSection> gdsSet = new HashMap<Integer, Grib1GridDefinitionSection>();
    Map<Integer, Grib1ParameterBean> pdsSet = new HashMap<Integer, Grib1ParameterBean>();

    Grib1Input reader = new Grib1Input(raf);
    raf.seek(0);
    reader.scan(false, false);

    java.util.List<Grib1ParameterBean> products = new ArrayList<Grib1ParameterBean>();
    for (Grib1Record gr : reader.getRecords()) {
      Grib1Pds pds = gr.getPDS().getPdsVars();
      Grib1ParameterBean bean = pdsSet.get(makeUniqueId(pds));
      if (bean == null) {
        bean = new Grib1ParameterBean(gr);
        pdsSet.put(makeUniqueId(pds), bean);
        products.add(bean);
      }
      bean.addRecord(gr);

      Grib1GridDefinitionSection gds = gr.getGDS();
      gdsSet.put(gds.getGdsKey(), gds);
    }
    param1BeanTable.setBeans(products);
    record1BeanTable.setBeans(new ArrayList());
    System.out.printf("GribRawPanel products = %d records = %d%n", products.size(), reader.getRecords().size());

    java.util.List<Gds1Bean> gdsList = new ArrayList<Gds1Bean>();
    for (Grib1GridDefinitionSection gds : gdsSet.values()) {
      gdsList.add(new Gds1Bean(gds.getGdsKey(), gds));
    }
    gds1Table.setBeans(gdsList);
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

  ////////////////////////////////////////////////////////////////////////////////////////////////

  void setGribFile2(RandomAccessFile raf) throws IOException {
    split2 = new JSplitPane(JSplitPane.VERTICAL_SPLIT, false, param2BeanTable, record2BeanTable);
    split2.setDividerLocation(prefs.getInt("splitPos2", 800));

    split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, false, split2, gds2Table);
    split.setDividerLocation(prefs.getInt("splitPos", 500));

    removeAll();
    add(split, BorderLayout.CENTER);
    revalidate();

    Map<Integer, Grib2GridDefinitionSection> gdsSet = new HashMap<Integer, Grib2GridDefinitionSection>();
    Map<Integer, Grib2ParameterBean> pdsSet = new HashMap<Integer, Grib2ParameterBean>();

    Grib2Input reader = new Grib2Input(raf);
    raf.seek(0);
    raf.order(RandomAccessFile.BIG_ENDIAN);
    reader.scan(false, false);

    java.util.List<Grib2ParameterBean> products = new ArrayList<Grib2ParameterBean>();
    for (Grib2Record gr : reader.getRecords()) {
      Grib2ProductDefinitionSection pds = gr.getPDS();
      int discipline = gr.getIs().getDiscipline();
      Grib2ParameterBean bean = pdsSet.get(makeUniqueId(pds.getPdsVars(), discipline));
      if (bean == null) {
        bean = new Grib2ParameterBean(gr);
        pdsSet.put(makeUniqueId(pds.getPdsVars(), discipline), bean);
        products.add(bean);
      }
      bean.addRecord(gr);

      Grib2GridDefinitionSection gds = gr.getGDS();
      gdsSet.put(gds.getGdsKey(), gds);
    }
    param2BeanTable.setBeans(products);
    record2BeanTable.setBeans(new ArrayList());
    System.out.printf("GribRawPanel products = %d records = %d%n", products.size(), reader.getRecords().size());

    java.util.List<Gds2Bean> gdsList = new ArrayList<Gds2Bean>();
    for (Grib2GridDefinitionSection gds : gdsSet.values()) {
      gdsList.add(new Gds2Bean(gds));
    }
    gds2Table.setBeans(gdsList);
  }

  public int makeUniqueId(Grib2Pds pds, int discipline) {
    int result = 17;
    result += result * 37 + pds.getProductDefinitionTemplate();
    result += result * 37 + discipline;
    result += result * 37 + pds.getParameterCategory();
    result += result * 37 + pds.getParameterNumber();
    result *= result * 37 + pds.getLevelType1();
    result *= result * 37 + pds.getLevelType2();
    return result;
  }

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

  private void showRawPds(Grib1Pds pds, Formatter f) {
    byte[] raw = pds.getPDSBytes();
    f.format("%n");
    for (int i = 0; i < raw.length; i++) {
      f.format(" %3d : %3d%n", i + 1, raw[i]);
    }
  }

  private void showRawPds(String key, byte[] raw, Formatter f) {
    if (productTemplates == null)
      try {
        productTemplates = GribTemplate.getParameterTemplates();
      } catch (IOException e) {
        f.format("Read template failed = %s%n", e.getMessage());
        return;
      }

    GribTemplate gt = productTemplates.get(key);
    if (gt == null)
      f.format("Cant find template %s%n", key);
    else
      gt.showInfo(raw, f);
  }

  ////////////////////////////////////////////////////////////////////////////

  public class Grib2ParameterBean {
    Grib2IdentificationSection id;
    Grib2ProductDefinitionSection pds;
    Grib2Pds pdsv;
    List<Grib2RecordBean> records;
    int discipline;
    int gdsKey;

    // no-arg constructor

    public Grib2ParameterBean() {
    }

    public Grib2ParameterBean(Grib2Record r) {
      pds = r.getPDS();
      pdsv = pds.getPdsVars();
      id = r.getId();
      discipline = r.getIs().getDiscipline();
      records = new ArrayList<Grib2RecordBean>();
      gdsKey = r.getGDS().getGdsVars().getGdsKey();
    }

    void addRecord(Grib2Record r) {
      records.add(new Grib2RecordBean(r));
    }

    List<Grib2RecordBean> getRecordBeans() {
      return records;
    }

    public String toString() {
      Formatter f = new Formatter();
      showRawPds(pds, f);
      return f.toString();
    }

    public String toProcessedString() {
      Formatter f = new Formatter();
      showProcessedPds(pdsv, discipline, f);
      return f.toString();
    }

    ///////////////

    public String getName() {
      return ParameterTable.getParameterName(discipline, pdsv.getParameterCategory(), pdsv.getParameterNumber());
    }

    /*  public final String getCenter() {
     return Grib1Tables.getCenter_idName(id.getCenter_id()) + " (" + id.getCenter_id() + "/" + id.getSubcenter_id() + ")";
   }

   public final String getTable() {
     return id.getMaster_table_version() + "-" + id.getLocal_table_version();
   } */

    public final Date getBaseTime() {
      return id.getBaseTime();
    }

    public String getParamNo() {
      return discipline + "-" + pdsv.getParameterCategory() + "-" + pdsv.getParameterNumber();
    }

    public int getTemplate() {
      return pdsv.getProductDefinitionTemplate();
    }

    public final int getLevelType() {
      return pdsv.getLevelType1();
    }

    public final String getLevelName() {
      return Grib2Tables.codeTable4_5(pdsv.getLevelType1());
    }

    public final String getLevelNameShort() {
      return Grib2Tables.getTypeSurfaceNameShort(pdsv.getLevelType1());
    }

    public final String getTypeGenProcess() {
      int tgp = pdsv.getGenProcessType();
      return Grib2Tables.codeTable4_3(tgp);
    }

   /*
    public String getProductStatus() {
      return id.getProductStatusName();
    }

    public String getProductType() {
      return id.getProductTypeName();
    }   */

    /*  public final int getAnalProcId() {
     return pdsv.getAnalysisGenProcess();
   }

   public final int getBackProcId() {
     return pdsv.getBackGenProcess();
   }

   public final int getObsProcId() {
     return pdsv.getObservationProcess();
   } */

    public int getN() {
      return records.size();
    }

  }

  private void showRawGds(Grib2GridDefinitionSection gds, Formatter f) {
    Grib2GDSVariables gdsv = gds.getGdsVars();
    int template = gdsv.getGdtn();
    byte[] raw = gdsv.getGDSBytes();

    showRawPds("3."+template, raw, f);
  }

  private void showRawPds(Grib2ProductDefinitionSection pds, Formatter f) {
    Grib2Pds pdsv = pds.getPdsVars();
    int template = pdsv.getProductDefinitionTemplate();
    byte[] raw = pdsv.getPDSBytes();

    showRawPds("4."+template, raw, f);
  }

  private void showProcessedPds(Grib2Pds pds, int discipline, Formatter f) {
    int template = pds.getProductDefinitionTemplate();
    f.format(" Product Template = %3d %s%n",template, Grib2Tables.codeTable4_0(template));
    f.format(" Parameter Category = %3d %s%n", pds.getParameterCategory(),
            ParameterTable.getCategoryName(discipline, pds.getParameterCategory()));
    f.format(" Parameter Name     = %3d %s %n", pds.getParameterNumber(),
            ParameterTable.getParameterName(discipline, pds.getParameterCategory(), pds.getParameterNumber()));
    f.format(" Parameter Units    = %s %n", ParameterTable.getParameterUnit(discipline, pds.getParameterCategory(), pds.getParameterNumber()));

    int tgp = pds.getGenProcessType();
    f.format(" Generating Process Type = %3d %s %n", tgp, Grib2Tables.codeTable4_3(tgp));
    f.format(" Forecast Offset    = %3d %n", pds.getForecastTime());
    f.format(" First Surface Type = %3d %s %n", pds.getLevelType1(), Grib2Tables.codeTable4_5(pds.getLevelType1()));
    f.format(" First Surface value= %3f %n", pds.getLevelValue1());
    f.format(" Second Surface Type= %3d %s %n", pds.getLevelType2(), Grib2Tables.codeTable4_5(pds.getLevelType2()));
    f.format(" Second Surface val = %3f %n", pds.getLevelValue2());
  }

  public class Grib2RecordBean {
    Grib2Record gr;
    Grib2IdentificationSection id;
    Grib2GridDefinitionSection gds;
    Grib2ProductDefinitionSection pds;
    Grib2Pds pdsv;
    int[] interval;

    // no-arg constructor

    public Grib2RecordBean() {
    }

    public Grib2RecordBean(Grib2Record m) {
      this.gr = m;
      id = gr.getId();
      gds = gr.getGDS();
      pds = gr.getPDS();
      pdsv = pds.getPdsVars();
    }

    public final String getTimeUnit() {
      int unit = pdsv.getTimeUnit();
      return Grib2Tables.codeTable4_4(unit);
    }

    public final Date getBaseTime() {
      return id.getBaseTime();
    }

    public final int getForecastTime() {
      return pdsv.getForecastTime();
    }

    public String getHeader() {
      return gr.getHeader();
    }

    public String getSurfaceType() {
      return pdsv.getLevelType1() + "-" + pdsv.getLevelType2();
    }

    public String getSurfaceValue() {
      return pdsv.getLevelValue1() + "-" + pdsv.getLevelValue2();
    }

    public String getInterval() {
      if (pdsv.isInterval()) {
        int[] intv = pdsv.getForecastTimeInterval();
        return intv[0]+"-"+intv[1];
      }
      return "";
    }

    public final String getStatType() {
      int code = pdsv.getStatisticalProcessType();
      return (code >= 0) ? Grib2Tables.codeTable4_10(pdsv.getStatisticalProcessType()) : "";
    }

    public final boolean isEnsemble() {
      return pdsv.isEnsemble();
    }

    /* public final int getVCoords() {
     return pdsv.getCoordinates();
   } */

    public final int getEnsN() {
      return pdsv.getPerturbationNumber();
    }

    public final int getNForecasts() {
      return pdsv.getNumberEnsembleForecasts();
    }

    public final int getPerturbationType() {
      return pdsv.getPerturbationType();
    }

    public final String getProbLimit() {
      return pdsv.getProbabilityLowerLimit() + "-" + pdsv.getProbabilityUpperLimit();
    }

    /*
    public final int getChem() {
      return pdsv.getChemicalType();
    }

    public final int getBands() {
      return pdsv.getNB();
    }  */

    public final long getGdsOffset() {
      return gr.getGdsOffset();
    }

    public final long getPdsOffset() {
      return gr.getPdsOffset();
    }

    /*
  public final String getCutoff() {
    return pdsv.getHoursAfter() + ":" + pdsv.getMinutesAfter();
  }  */

    public void toRawPdsString(Formatter f) {
      byte[] bytes = pdsv.getPDSBytes();
      int count = 1;
      for (byte b : bytes) {
        short s = DataType.unsignedByteToShort(b);
        f.format(" %d : %d%n", count++, s);
      }
    }

    public String showComplete() {
      Formatter f = new Formatter();
      f.format("Grib2IndicatorSection%n");
      f.format(" Discipline = (%d) %s%n",  gr.getIs().getDiscipline(), gr.getIs().getDisciplineName());
      f.format(" Edition    = %d%n", gr.getIs().getGribEdition());
      f.format(" Length     = %d%n", gr.getIs().getGribLength());

      f.format("%nGrib2IdentificationSection%n");
      f.format(" Length        = %d%n", id.getLength());
      f.format(" Center        = (%d) %s%n", id.getCenter_id(), Grib1Tables.getCenter_idName(id.getCenter_id()));
      f.format(" SubCenter     = (%d) %s%n", id.getSubcenter_id(), Grib1Tables.getSubCenter_idName(id.getCenter_id(), id.getSubcenter_id()));
      f.format(" Master Table  = %d%n", id.getMaster_table_version());
      f.format(" Local Table   = %d%n", id.getLocal_table_version());
      f.format(" RefTimeSignif = %s%n", id.getSignificanceOfRTName());
      f.format(" RefTime       = %s%n", df.toDateTimeStringISO(id.getBaseTime()));
      f.format(" ProductStatus = %s%n", id.getProductStatusName());
      f.format(" ProductType   = %s%n", id.getProductTypeName());

      byte[] lus = gr.getLocalUseSection();
      if (lus != null) {
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

      f.format("%nGrib2GridDefinitionSection%n");
      f.format(" Length             = %d%n", gds.getLength());
      f.format(" Source  (3.0)      = %d%n", gds.getSource());
      f.format(" Npts               = %d%n", gds.getNumberPoints());
      f.format(" Template (3.1)     = %d%n", gds.getGdtn());
      showRawGds(gds, f);

      f.format("%nGrib2ProductDefinitionSection%n");
      f.format("            Length  = %d%n", pds.getLength());
      int[] intv = pds.getPdsVars().getForecastTimeInterval();
      if (intv != null) {
        f.format(" Start interval     = %d%n", intv[0]);
        f.format(" End   interval     = %d%n", intv[1]);
      }

      showRawPds(pds, f);

      return f.toString();
    }

    public String toProcessedString() {
      Formatter f = new Formatter();
      pdsv.show(f);
      return f.toString();
    }

  } // RecordBean (old)


  public class Gds2Bean {
    Grib2GridDefinitionSection gds;
    Grib2GDSVariables gdsv;

    // no-arg constructor

    public Gds2Bean() {
    }

    public Gds2Bean(Grib2GridDefinitionSection m) {
      this.gds = m;
      this.gdsv = gds.getGdsVars();
    }

    public int getKey() {
      return gds.getGdsKey();
    }

    public float getLa1() {
      return gdsv.getLa1();
    }

    public float getLo1() {
      return gdsv.getLo1();
    }

    public float getLa2() {
      return gdsv.getLa2();
    }

    public float getLo2() {
      return gdsv.getLo2();
    }

    public float getDx() {
      return gdsv.getDx();
    }

    public float getDy() {
      return gdsv.getDy();
    }

    public int getGDS() {
      return gds.getGdtn();
    }

    public String getGridName() {
      return Grib2Tables.codeTable3_1( gds.getGdtn());
    }

    public int getNPoints() {
      return gdsv.getNumberPoints();
    }

    public String getScanMode() {
      return Long.toBinaryString(gdsv.getScanMode());
    }

    public String getResolution() {
      return Long.toBinaryString(gdsv.getResolution());
    }
  }

  ////////////////////////////////////////////////////////////////////////////

  public class Gds1Bean {
    Grib1GridDefinitionSection gds;
    int key;

    // no-arg constructor

    public Gds1Bean() {
    }

    public Gds1Bean(int key, Grib1GridDefinitionSection m) {
      this.key = key;
      this.gds = m;
    }

    public int getKey() {
      return key;
    }

    public int getHashCode() {
      return gds.hashCode();
    }

    public int getGridNo() {
      return gds.getGdtn();
    }

    public String getGridName() {
      return gds.getName();
    }

    public String getScanMode() {
      return Long.toBinaryString(gds.getScanMode());
    }

    public String getResolution() {
      return Long.toBinaryString(gds.getResolution());
    }

  }

  public class Grib1ParameterBean {
    Grib1ProductDefinitionSection pds;
    Grib1Pds pdsv;
    List<Grib1RecordBean> records;
    String header;

    // no-arg constructor

    public Grib1ParameterBean() {
    }

    public Grib1ParameterBean(Grib1Record r) {
      pds = r.getPDS();
      pdsv = pds.getPdsVars();
      header = r.getHeader();
      records = new ArrayList<Grib1RecordBean>();
    }

    void addRecord(Grib1Record r) {
      records.add(new Grib1RecordBean(r));
    }

    List<Grib1RecordBean> getRecordBeans() {
      return records;
    }

    public String toRawString() {
      Formatter f = new Formatter();
      showRawPds(pdsv, f);
      return f.toString();
    }

    public String toProcessedString() {
      Formatter f = new Formatter();
      Grib1Dump.printPDS(pdsv, f);
      return f.toString();
    }

    ///////////////

    public String getDesc() {
      return pds.getParameter().getDescription();
    }

    public String getCenter() {
      return Grib1Tables.getCenter_idName(pds.getCenter()) + " (" + pds.getCenter() + "/" + pds.getSubCenter() + ")";
    }

    public int getTable() {
      return pds.getTableVersion();
    }

    public final Date getReferenceDate() {
      return pdsv.getReferenceDate();
    }

    public int getParamNo() {
      return pdsv.getParameterNumber();
    }

    public final int getLevelType() {
      return pdsv.getLevelType1();
    }

    public final String getLevelName() {
      return pdsv.getLevelName();
    }

    public int getN() {
      return records.size();
    }

  }

  public class Grib1RecordBean {
    Grib1Record gr;
    Grib1GridDefinitionSection gds;
    Grib1ProductDefinitionSection pds;
    Grib1Pds pdsv;
    int[] interval;

    // no-arg constructor

    public Grib1RecordBean() {
    }

    public Grib1RecordBean(Grib1Record m) {
      this.gr = m;
      gds = gr.getGDS();
      pds = gr.getPDS();
      pdsv = pds.getPdsVars();
      interval = pdsv.getForecastTimeInterval();
      if (interval == null) interval = new int[] {0,0};
    }


    public String getHeader() {
      return gr.getHeader();
    }

    public final String getTimeUnit() {
      int unit = pdsv.getTimeUnit();
      return Grib2Tables.codeTable4_4(unit);
    }

    public final Date getReferenceDate() {
      return pdsv.getReferenceDate();
    }

    public final int getForecastTime() {
      return pdsv.getForecastTime();
    }

    public String getSurfaceValue() {
      return pdsv.getLevelValue1() + "-" + pdsv.getLevelValue2();
    }

    public int getStartInterval() {
      return interval[0];
    }

    public int getEndInterval() {
      return interval[1];
    }

    public int getTimeInterval() {
      return interval[1] - interval[0];
    }

    public final String getStatType() {
      int code = pdsv.getStatisticalProcessType();
      return (code >= 0) ? Grib2Tables.codeTable4_10(pdsv.getStatisticalProcessType()) : "";
    }

    public final boolean isEnsemble() {
      return pdsv.isEnsemble();
    }

    public final int getNForecasts() {
      return pdsv.getNumberEnsembleForecasts();
    }

    public final int getPerturbationType() {
      return pdsv.getPerturbationType();
    }

    public final String getProbLimit() {
      return pdsv.getProbabilityLowerLimit() + "-" + pdsv.getProbabilityUpperLimit();
    }

    public final long getDataOffset() {
      return gr.getDataOffset();
    }

    public String toRawString() {
      Formatter f = new Formatter();
      showRawPds(pdsv, f);
      return f.toString();
    }

    public String toProcessedString() {
      Formatter f = new Formatter();
      Grib1Dump.printPDS(pdsv, f);
      return f.toString();
    }

  }
}
