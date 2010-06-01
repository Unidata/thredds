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

import ucar.grib.GribNumbers;
import ucar.grib.grib1.Grib1Tables;
import ucar.ma2.DataType;
import ucar.util.prefs.PreferencesExt;
import ucar.util.prefs.ui.BeanTableSorted;
import ucar.unidata.io.RandomAccessFile;
import ucar.grib.grib2.*;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import thredds.ui.TextHistoryPane;
import thredds.ui.IndependentWindow;
import thredds.ui.BAMutil;
import thredds.ui.FileManager;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.*;
import java.util.*;
import java.util.List;

/**
 * ToolsUI/Iosp/Bufr
 *
 * @author caron
 * @since Aug 15, 2008
 */
public class Grib2Table extends JPanel {
  private PreferencesExt prefs;

  private BeanTableSorted recordTable, gdsTable, productTable;
  private JSplitPane split, split2;

  private TextHistoryPane infoPopup, infoPopup2;
  private IndependentWindow infoWindow, infoWindow2;

  private StructureTable dataTable;
  private IndependentWindow dataWindow;
  private FileManager fileChooser;

  public Grib2Table(PreferencesExt prefs) {
    this.prefs = prefs;

    String tooltip = "from Grib2Input.getRecords()";
    recordTable = new BeanTableSorted(RecordBean.class, (PreferencesExt) prefs.node("GridRecordBean"), false, "Grib2Record", tooltip);

    String tooltip2 = "unique from Grib2Records";
    gdsTable = new BeanTableSorted(GdsBean.class, (PreferencesExt) prefs.node("GdsBean"), false, "Grib2GridDefinitionSection", tooltip2);
    gdsTable.addListSelectionListener(new ListSelectionListener() {
      public void valueChanged(ListSelectionEvent e) {
        GdsBean bean = (GdsBean) gdsTable.getSelectedBean();
        infoPopup.setText(bean.gds.toString());
        infoWindow.setVisible(true);
        gdsTable.clearSelection();
      }
    });

    String tooltip3 = "unique from Grib2Input.getRecords()";
    productTable = new BeanTableSorted(ProductBean.class, (PreferencesExt) prefs.node("ProductBean"), false, "Grib2PDSVariables", tooltip3);
    productTable.addListSelectionListener(new ListSelectionListener() {
      public void valueChanged(ListSelectionEvent e) {
        ProductBean pb = (ProductBean) productTable.getSelectedBean();
        if (pb != null) {
          recordTable.setBeans(pb.getRecordBeans());
          System.out.printf("records = %d%n", pb.getRecordBeans().size());
        }
      }
    });

    thredds.ui.PopupMenu varPopup = new thredds.ui.PopupMenu(productTable.getJTable(), "Options");
    varPopup.addAction("Show raw PDS", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        ProductBean pb = (ProductBean) productTable.getSelectedBean();
        if (pb != null) {
          infoPopup.setText(pb.toRawString());
          infoPopup.gotoTop();
          infoWindow.showIfNotIconified();
        }
      }
    });

    varPopup.addAction("Show processed PDS", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        ProductBean pb = (ProductBean) productTable.getSelectedBean();
        if (pb != null) {
          infoPopup2.setText(pb.toString());
          infoPopup2.gotoTop();
          infoWindow2.showIfNotIconified();
        }
      }
    });

    // the info windows
    infoPopup = new TextHistoryPane();
    infoWindow = new IndependentWindow("Extra Information", BAMutil.getImage("netcdfUI"), infoPopup);
    infoWindow.setBounds((Rectangle) prefs.getBean("InfoWindowBounds", new Rectangle(300, 300, 500, 300)));

    // the info windows
    infoPopup2 = new TextHistoryPane();
    infoWindow2 = new IndependentWindow("Extra Information", BAMutil.getImage("netcdfUI"), infoPopup2);
    infoWindow2.setBounds((Rectangle) prefs.getBean("InfoWindowBounds2", new Rectangle(300, 300, 500, 300)));

    split2 = new JSplitPane(JSplitPane.VERTICAL_SPLIT, false, productTable, recordTable);
    split2.setDividerLocation(prefs.getInt("splitPos2", 800));

    split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, false, split2, gdsTable);
    split.setDividerLocation(prefs.getInt("splitPos", 500));

    setLayout(new BorderLayout());
    add(split, BorderLayout.CENTER);
  }

  private void makeDataTable() {
    // the data Table
    dataTable = new StructureTable((PreferencesExt) prefs.node("structTable"));
    dataWindow = new IndependentWindow("Data Table", BAMutil.getImage("netcdfUI"), dataTable);
    dataWindow.setBounds((Rectangle) prefs.getBean("dataWindow", new Rectangle(50, 300, 1000, 600)));
  }

  public void save() {
    recordTable.saveState(false);
    gdsTable.saveState(false);
    productTable.saveState(false);
    prefs.putBeanObject("InfoWindowBounds", infoWindow.getBounds());
    prefs.putBeanObject("InfoWindowBounds2", infoWindow2.getBounds());
    prefs.putInt("splitPos", split.getDividerLocation());
    prefs.putInt("splitPos2", split2.getDividerLocation());
  }

  private String location;

  public void setGribFile(RandomAccessFile raf) throws IOException {
    this.location = raf.getLocation();
    if (location.endsWith(".gbx"))
      setGribFileIndex(raf);

    Map<Integer, Grib2GridDefinitionSection> gdsSet = new HashMap<Integer, Grib2GridDefinitionSection>();
    Map<Integer, ProductBean> pdsSet = new HashMap<Integer, ProductBean>();

    Grib2Input reader = new Grib2Input(raf);
    raf.seek(0);
    reader.scan(false, false);

    java.util.List<ProductBean> products = new ArrayList<ProductBean>();
    for (Grib2Record gr : reader.getRecords()) {
      Grib2ProductDefinitionSection pds = gr.getPDS();
      Grib2PDSVariables pdsv = pds.getPdsVars();
      ProductBean bean = pdsSet.get(pdsv.getUniqueId());
      if (bean == null) {
        bean = new ProductBean(gr);
        pdsSet.put(pdsv.getUniqueId(), bean);
        products.add(bean);
      }
      bean.addRecord(gr);

      Grib2GridDefinitionSection gds = gr.getGDS();
      gdsSet.put(gds.getGdsKey(), gds);
    }
    productTable.setBeans(products);
    recordTable.setBeans(new ArrayList());
    System.out.printf("products = %d%n", products.size());

    java.util.List<GdsBean> gdsList = new ArrayList<GdsBean>();
    Iterator<Grib2GridDefinitionSection> iter = gdsSet.values().iterator();
    while (iter.hasNext()) {
      Grib2GridDefinitionSection gds = iter.next();
      gdsList.add(new GdsBean(gds.getGdsKey(), gds));
    }
    gdsTable.setBeans(gdsList);
  }

  private void setGribFileIndex(RandomAccessFile raf) throws IOException {
    this.location = raf.getLocation();
  }

  public class RecordBean {
    Grib2Record gr;
    Grib2IdentificationSection id;

    // no-arg constructor

    public RecordBean() {
    }

    public RecordBean(Grib2Record m) {
      this.gr = m;
      id = gr.getId();
    }

    public final long getGdsOffset() {
      return gr.getGdsOffset();
    }

    public final long getPdsOffset() {
      return gr.getPdsOffset();
    }

    public final int getDiscipline() {
      return gr.getIs().getDiscipline();
    }

    public final String getCenter() {
      return id.getCenter_id() + "/" + id.getSubcenter_id();
    }

    public final String getCenterName() {
      return Grib1Tables.getCenter_idName(id.getCenter_id());
    }

    public final String getTable() {
      return id.getMaster_table_version() + "-" + id.getLocal_table_version();
    }

    public final String getSignificance() {
      return id.getSignificanceOfRTName();
    }

    public Date getRefTime() {
      return new Date(id.getRefTime());
    }

    public final Date getBaseTime() {
      return id.getBaseTime();
    }

    public final String getProductStatus() {
      return id.getProductStatusName();
    }

    public final String getProductType() {
      return id.getProductTypeName();
    }

  }

  public class ProductBean {
    Grib2PDSVariables pdsv;
    List<RecordBean> records;

    // no-arg constructor

    public ProductBean() {
    }

    public ProductBean(Grib2Record r) {
      Grib2ProductDefinitionSection pds = r.getPDS();
      pdsv = pds.getPdsVars();
      records = new ArrayList<RecordBean>();
    }

    void addRecord(Grib2Record r) {
      records.add(new RecordBean(r));
    }

    List<RecordBean> getRecordBeans() {
      return records;
    }

    /* public final int getDiscipline() {
     return gr.getDiscipline();
   }

   public Date getBaseTime() {
     return gr.getBaseTime();
   }

   public final long getRefTime() {
     return gr.getRefTime();
   }

   public final int getGDSkeyInt() {
     return gr.getGDSkeyInt();
   }

   public final long getGdsOffset() {
     return gr.getGdsOffset();
   }

   public final long getPdsOffset() {
     return gr.getPdsOffset();
   } */

    public String toRawString() {
      byte[] bytes = pdsv.getPDSBytes();
      Formatter f = new Formatter();
      int count = 1;
      for (byte b : bytes) {
        short s = DataType.unsignedByteToShort(b);
        f.format(" %d : %d%n", count++, s);
      }
      return f.toString();
    }

    public String toString() {
      int template = pdsv.getProductDefinition();
      byte[] raw = pdsv.getPDSBytes();
      byte[] bytes = new byte[raw.length+1];
      System.arraycopy(raw,0,bytes,1,raw.length); // offset by 1 so we can use 1-based indexing

      Formatter f = new Formatter();
      f.format("PDS length   = %d%n", pdsv.getLength());
      f.format(" ncoords     = %d%n", pdsv.getCoordinates());
      f.format(" template    = %d%n", pdsv.getProductDefinition());
      f.format(" category    = %d%n", bytes[10] & 0xff);
      f.format(" number      = %d%n", bytes[11] & 0xff);

      switch (template) {
        case 8: showTemplate8(bytes,f);
                break;
        default: f.format("N/A");
      }
      return f.toString();
    }

    void showTemplate8(byte[] bytes, Formatter f) {
      f.format(" gen         = %d%n", bytes[12] & 0xff);
      f.format(" backGen     = %d%n", bytes[13] & 0xff);
      f.format(" analFore    = %d%n", bytes[14] & 0xff);
      f.format(" cutoffH     = %d%n", GribNumbers.int2(bytes[15] & 0xff, bytes[16] & 0xff));
      f.format(" cutoffM     = %d%n", bytes[17] & 0xff);
      f.format(" timeUnit    = %d%n", bytes[18] & 0xff);
      f.format(" forecast    = %d%n", GribNumbers.int4(bytes[19] & 0xff, bytes[20] & 0xff, bytes[21] & 0xff, bytes[22] & 0xff));
      f.format(" surfaceType1= %d%n", bytes[23] & 0xff);
      f.format(" surfaceType2= %d%n", bytes[29] & 0xff);
      f.format(" year        = %d%n", GribNumbers.int2(bytes[35] & 0xff, bytes[36] & 0xff));
      f.format(" month       = %d%n", bytes[37] & 0xff);
      f.format(" day         = %d%n", bytes[38] & 0xff);
      f.format(" hour        = %d%n", bytes[39] & 0xff);
      f.format(" min         = %d%n", bytes[40] & 0xff);
      f.format(" sec         = %d%n", bytes[41] & 0xff);
      f.format(" ntimeIntv   = %d%n", bytes[42] & 0xff);
      f.format(" nmiss       = %d%n", GribNumbers.int4(bytes[43] & 0xff, bytes[44] & 0xff, bytes[45] & 0xff, bytes[46] & 0xff));
      f.format(" statProcess = %d%n", bytes[47] & 0xff);
      f.format(" timeIncrType= %d%n", bytes[48] & 0xff);
      f.format(" timeUnit    = %d%n", bytes[49] & 0xff);
      f.format(" timeLength  = %d%n", GribNumbers.int4(bytes[50] & 0xff, bytes[51] & 0xff, bytes[52] & 0xff, bytes[53] & 0xff));
      f.format(" timeIncrUnit= %d%n", bytes[54] & 0xff);
      f.format(" timeIncr    = %d%n", GribNumbers.int4(bytes[55] & 0xff, bytes[56] & 0xff, bytes[57] & 0xff, bytes[58] & 0xff));
    }

    ///////////////

    public final int getVCoords() {
      return pdsv.getCoordinates();
    }

    public final int getProdTemplate() {
      return pdsv.getProductDefinition();
    }

    public String getParamNo() {
      return pdsv.getParameterCategory() + "-" + pdsv.getParameterNumber();
    }

    public final int getGenProc() {
      return pdsv.getTypeGenProcess();
    }

    public final int getAnalProc() {
      return pdsv.getAnalysisGenProcess();
    }

    public final int getBackProc() {
      return pdsv.getBackGenProcess();
    }

    public final int getObsProc() {
      return pdsv.getObservationProcess();
    }

    public final int getBands() {
      return pdsv.getNB();
    }

    public final String getCutoff() {
      return pdsv.getHoursAfter() + ":" + pdsv.getMinutesAfter();
    }

    public final int getTimeUnit() {
      return pdsv.getTimeRangeUnit();
    }

    public final int getForecastTime() {
      return pdsv.getForecastTime();
    }

    public final String getSurfaceType() {
      return pdsv.getTypeFirstFixedSurface() + "-" + pdsv.getTypeSecondFixedSurface();
    }

    public String getSurfaceValue() {
      return pdsv.getValueFirstFixedSurface() + "-" + pdsv.getValueSecondFixedSurface();
    }

    public final boolean isEnsemble() {
      return pdsv.isEnsemble();
    }

    public final int getType() {
      return pdsv.getType();
    }

    public final int getEnsN() {
      return pdsv.getPerturbation();
    }

    public final int getNForecasts() {
      return pdsv.getNumberForecasts();
    }

    public final String getLimit() {
      return pdsv.getValueLowerLimit() + "-" + pdsv.getValueUpperLimit();
    }


    public final int getChem() {
      return pdsv.getChemicalType();
    }

  }

  public class GdsBean {
    Grib2GridDefinitionSection gds;
    int key;

    // no-arg constructor

    public GdsBean() {
    }

    public GdsBean(int key, Grib2GridDefinitionSection m) {
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
      return gds.getGridName(gds.getGdtn());
    }

    public String getScanMode() {
      return Long.toBinaryString(gds.getScanMode());
    }

    public String getResolution() {
      return Long.toBinaryString(gds.getResolution());
    }

  }


}
