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
import ucar.nc2.units.DateFormatter;
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

import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.*;
import java.util.*;
import java.util.List;

/**
 * ToolsUI/Iosp/Grib2
 *
 * @author caron
 * @since Aug 15, 2008
 */
public class Grib2Table extends JPanel {
  private PreferencesExt prefs;

  private BeanTableSorted recordTable, gdsTable, productTable;
  private JSplitPane split, split2;

  private TextHistoryPane infoPopup, infoPopup2, infoPopup3;
  private IndependentWindow infoWindow, infoWindow2, infoWindow3;

  private StructureTable dataTable;
  private IndependentWindow dataWindow;
  private DateFormatter df;

  public Grib2Table(PreferencesExt prefs) {
    this.prefs = prefs;

   String tooltip3 = "unique from Grib2Input.getRecords()";
    thredds.ui.PopupMenu varPopup;

    productTable = new BeanTableSorted(ProductBean.class, (PreferencesExt) prefs.node("ProductBean"), false, "Grib2PDSVariables", tooltip3);
    productTable.addListSelectionListener(new ListSelectionListener() {
      public void valueChanged(ListSelectionEvent e) {
        ProductBean pb = (ProductBean) productTable.getSelectedBean();
        if (pb != null) {
          recordTable.setBeans(pb.getRecordBeans());
          // System.out.printf("records = %d%n", pb.getRecordBeans().size());
        }
      }
    });

    varPopup = new thredds.ui.PopupMenu(productTable.getJTable(), "Options");
    varPopup.addAction("Show raw PDS", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        ProductBean pb = (ProductBean) productTable.getSelectedBean();
        if (pb != null) {
          infoPopup2.setText(pb.toString());
          infoPopup2.gotoTop();
          infoWindow2.showIfNotIconified();
        }
      }
    });
    varPopup.addAction("Show processed PDS", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        ProductBean pb = (ProductBean) productTable.getSelectedBean();
        if (pb != null) {
          infoPopup3.setText(pb.toProcessedString());
          infoPopup3.gotoTop();
          infoWindow3.showIfNotIconified();
        }
      }
    });
    varPopup.addAction("Run accum algorithm", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        ProductBean pb = (ProductBean) productTable.getSelectedBean();
        if (pb != null) {
          infoPopup3.setText(showAccumAlgo(pb));
          infoPopup3.gotoTop();
          infoWindow3.showIfNotIconified();
        }
      }
    });

    String tooltip = "from Grib2Input.getRecords()";
    recordTable = new BeanTableSorted(RecordBean.class, (PreferencesExt) prefs.node("GridRecordBean"), false, "Grib2Record", tooltip);
    varPopup = new thredds.ui.PopupMenu(recordTable.getJTable(), "Options");

    varPopup.addAction("Show raw PDS bytes", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        RecordBean bean = (RecordBean) recordTable.getSelectedBean();
        if (bean != null) {
          infoPopup.setText(bean.toRawPdsString());
          infoPopup.gotoTop();
          infoWindow.showIfNotIconified();
        }
      }
    });

    varPopup.addAction("Show Raw GridRecord", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        RecordBean bean = (RecordBean) recordTable.getSelectedBean();
        if (bean != null) {
          infoPopup.setText(bean.toString());
          infoPopup.gotoTop();
          infoWindow.showIfNotIconified();
        }
      }
    });
    varPopup.addAction("Show Processed GridRecord", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        RecordBean bean = (RecordBean) recordTable.getSelectedBean();
        if (bean != null) {
          infoPopup2.setText(bean.gr.toString());
          infoPopup2.gotoTop();
          infoWindow2.showIfNotIconified();
        }
      }
    });

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
    prefs.putBeanObject("InfoWindowBounds3", infoWindow3.getBounds());
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
      int discipline = gr.getIs().getDiscipline();
      ProductBean bean = pdsSet.get(makeUniqueId(pds, discipline));
      if (bean == null) {
        bean = new ProductBean(gr);
        pdsSet.put(makeUniqueId(pds, discipline), bean);
        products.add(bean);
      }
      bean.addRecord(gr);

      Grib2GridDefinitionSection gds = gr.getGDS();
      gdsSet.put(gds.getGdsKey(), gds);
    }
    productTable.setBeans(products);
    recordTable.setBeans(new ArrayList());
    //System.out.printf("products = %d%n", products.size());

    java.util.List<GdsBean> gdsList = new ArrayList<GdsBean>();
    for (Grib2GridDefinitionSection gds : gdsSet.values()) {
      gdsList.add(new GdsBean(gds.getGdsKey(), gds));
    }
    gdsTable.setBeans(gdsList);
  }

  private void setGribFileIndex(RandomAccessFile raf) throws IOException {
    this.location = raf.getLocation();
  }

  public int makeUniqueId(Grib2ProductDefinitionSection pds, int discipline) {
    int result = 17;
    result += result*37 + pds.getProductDefinition();
    result += result*37 + discipline;
    result += result*37 + pds.getParameterCategory();
    result += result*37 + pds.getParameterNumber();
    result *= result*37 + pds.getTypeFirstFixedSurface();
    return result;
  }

  private String showAccumAlgo(ProductBean pb) {
    List<RecordBean> all = new ArrayList<RecordBean>(pb.getRecordBeans());
    List<RecordBean> hourAccum = new ArrayList<RecordBean>(all.size());
    List<RecordBean> runAccum = new ArrayList<RecordBean>(all.size());
    
    for (RecordBean rb : pb.getRecordBeans()) {
      int start = rb.getStartInterval();
      int end = rb.getEndInterval();
      if (end-start == 1) hourAccum.add(rb);
      if (start == 0) runAccum.add(rb);
    }

    Formatter f = new Formatter();
    f.format("all: ");
    showList(all, f);

    for (RecordBean rb :hourAccum)
      all.remove(rb);
    for (RecordBean rb :runAccum)
      all.remove(rb);

    f.format("hourAccum: ");
    showList(hourAccum, f);

    f.format("runningAccum: ");
    showList(runAccum, f);

    f.format("remaining: ");
    showList(all, f);

    return f.toString();
  }

  private void showList(List<RecordBean> list, Formatter f) {
    f.format("(%d) ", list.size());
    for (RecordBean rb : list)
      f.format(" %d-%d", rb.getStartInterval(), rb.getEndInterval());
    f.format("%n");
  }

    ////////////////////////////////////////////////////////////////////////////

  public class ProductBean {
    Grib2IdentificationSection id;
    Grib2ProductDefinitionSection pds;
    Grib2PDSVariables pdsv;
    List<RecordBean> records;
    int discipline;

    // no-arg constructor

    public ProductBean() {
    }

    public ProductBean(Grib2Record r) {
      pds = r.getPDS();
      pdsv = pds.getPdsVars();
      id = r.getId();
      discipline = r.getIs().getDiscipline();
      records = new ArrayList<RecordBean>();
    }

    void addRecord(Grib2Record r) {
      records.add(new RecordBean(r));
    }

    List<RecordBean> getRecordBeans() {
      return records;
    }

   public String toString() {
      Formatter f = new Formatter();
      showRawPds(pds, f);
      return f.toString();
    }

    public String toProcessedString() {
      Formatter f = new Formatter();
      showProcessedPds(pds, discipline, f);
      return f.toString();
    }

     ///////////////
    public String getName() {
      return ParameterTable.getParameterName(discipline, pds.getParameterCategory(), pds.getParameterNumber());
    }

    public final String getCenter() {
       return  Grib1Tables.getCenter_idName(id.getCenter_id())+" ("+ id.getCenter_id() + "/" + id.getSubcenter_id() +  ")";
     }

     public final String getTable() {
       return id.getMaster_table_version() + "-" + id.getLocal_table_version();
     }

     public final Date getBaseTime() {
       return id.getBaseTime();
     }

     public String getProductStatus() {
       return id.getProductStatusName();
     }

     public String getProductType() {
       return id.getProductTypeName();
     }

    public String getParamNo() {
      return pdsv.getProductDefinition()+"-"+discipline+"-"+pdsv.getParameterCategory() + "-" + pdsv.getParameterNumber();
    }

    public final String getSurfaceType() {
      return Grib2Tables.codeTable4_5(pdsv.getTypeFirstFixedSurface());
    }

     public final String getProcType() {
      int tgp = pdsv.getTypeGenProcess();
      return Grib2Tables.codeTable4_3(tgp);
    }

    public final int getProcId() {
      return pdsv.getAnalysisGenProcess();
    }

    public final int getBackProcId() {
      return pdsv.getBackGenProcess();
    }

    public final int getObsProcId() {
      return pdsv.getObservationProcess();
    }

    public int getN() {
      return records.size();
    }
     
  }

  private void showRawPds(Grib2ProductDefinitionSection pds, Formatter f) {
    Grib2PDSVariables pdsv = pds.getPdsVars();
    int template = pdsv.getProductDefinition();
    byte[] raw = pdsv.getPDSBytes();
    byte[] bytes = new byte[raw.length+1];
    System.arraycopy(raw,0,bytes,1,raw.length); // offset by 1 so we can use 1-based indexing

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
  }

  void showTemplate8(byte[] bytes, Formatter f) {
    f.format("                                          Type of generating process = %d (%s)%n", bytes[12] & 0xff,
            Grib2Tables.codeTable4_0(bytes[12] & 0xff));
    f.format("          Background generating process id (from originating center) = %d%n", bytes[13] & 0xff);
    f.format(" Analysis/forecast generating processes id (from originating center) = %d%n", bytes[14] & 0xff);
    f.format("                       Hours after reference time of data cutoff     = %d%n", GribNumbers.int2(bytes[15] & 0xff, bytes[16] & 0xff));
    f.format("                     Minutes after reference time of data cutoff     = %d%n", bytes[17] & 0xff);
    f.format("                                  Indicator of unit of time range    = %d (%s)%n", bytes[18] & 0xff,
            Grib2Tables.codeTable4_4(bytes[18] & 0xff));
    f.format("    Forecast time in above units (beginning of overall time period)  = %d%n", GribNumbers.int4(bytes[19] & 0xff, bytes[20] & 0xff, bytes[21] & 0xff, bytes[22] & 0xff));
    f.format("                                         Type of first fixed surface = %d (%s)%n", bytes[23] & 0xff,
            Grib2Tables.codeTable4_5(bytes[23] & 0xff));
    f.format("                                        Type of second fixed surface = %d (%s)%n", bytes[29] & 0xff,
            Grib2Tables.codeTable4_5(bytes[29] & 0xff));
    f.format("                 Year of time of end of overall time interval        = %d%n", GribNumbers.int2(bytes[35] & 0xff, bytes[36] & 0xff));
    f.format("                  month                                              = %d%n", bytes[37] & 0xff);
    f.format("                  day                                                = %d%n", bytes[38] & 0xff);
    f.format("                  hour                                               = %d%n", bytes[39] & 0xff);
    f.format("                  min                                                = %d%n", bytes[40] & 0xff);
    f.format("                  sec                                                = %d%n", bytes[41] & 0xff);
    f.format("                               Number of time range specifications   = %d%n", bytes[42] & 0xff);
    f.format("                                 number of data values missing       = %d%n", GribNumbers.int4(bytes[43] & 0xff, bytes[44] & 0xff, bytes[45] & 0xff, bytes[46] & 0xff));
    f.format("           Statistical process used to calculate the processed field = %d (%s)%n", bytes[47] & 0xff,
            Grib2Tables.codeTable4_10(bytes[47] & 0xff));
    f.format("                    Type of time increment between successive fields = %d (%s)%n", bytes[48] & 0xff,
            Grib2Tables.codeTable4_11(bytes[48] & 0xff));
    f.format("             Unit of time for time range for statistical processing  = %d (%s)%n", bytes[49] & 0xff,
            Grib2Tables.codeTable4_4(bytes[49] & 0xff));
    f.format(" Length of the time range over which statistical processing is done  = %d%n", GribNumbers.int4(bytes[50] & 0xff, bytes[51] & 0xff, bytes[52] & 0xff, bytes[53] & 0xff));
    f.format("   Unit of time for the increment between the successive fields used = %d (%s)%n", bytes[54] & 0xff,
            Grib2Tables.codeTable4_4(bytes[54] & 0xff));
    f.format("                            Time increment between successive fields = %d%n", GribNumbers.int4(bytes[55] & 0xff, bytes[56] & 0xff, bytes[57] & 0xff, bytes[58] & 0xff));
  }

  private void showProcessedPds(Grib2ProductDefinitionSection pds, int discipline, Formatter f) {
    f.format(" Product Definition = %3d %s%n", pds.getProductDefinition(),  pds.getProductDefinitionName());
    f.format(" Parameter Category = %3d %s%n", pds.getParameterCategory(),
                                            ParameterTable.getCategoryName(discipline, pds.getParameterCategory()));
    f.format(" Parameter Name     = %3d %s %n", pds.getParameterNumber(),
                                            ParameterTable.getParameterName(discipline, pds.getParameterCategory(), pds.getParameterNumber()));
    f.format(" Parameter Units    = %s %n", ParameterTable.getParameterUnit(discipline,  pds.getParameterCategory(), pds.getParameterNumber()));

    int tgp = pds.getTypeGenProcessNumeric();
    f.format(" Generating Process = %3d %s %n", tgp, Grib2Tables.codeTable4_3(tgp));
    f.format(" Forecast Offset    = %3d %n", pds.getForecastTime());
    f.format(" Ending  Time       = %s %n", pds.getEndTI());
    f.format(" First Surface Type = %3d %s %n", pds.getTypeFirstFixedSurface(),
                                               Grib2Tables.codeTable4_5(pds.getTypeFirstFixedSurface()));
    f.format(" First Surface value= %3f %n", pds.getValueFirstFixedSurface());
    f.format(" Second Surface Type= %3d %s %n" , pds.getTypeSecondFixedSurface(),
                                               Grib2Tables.codeTable4_5(pds.getTypeSecondFixedSurface()));
    f.format(" Second Surface val = %3f %n",pds.getValueSecondFixedSurface());
  }

  ////////////////////////////////////////////////////////////////////////////
  public class RecordBean {
    Grib2Record gr;
    Grib2IdentificationSection id;
    Grib2GridDefinitionSection gds;
    Grib2ProductDefinitionSection pds;
    Grib2PDSVariables pdsv;
    int[] interval;

    // no-arg constructor

    public RecordBean() {
    }

    public RecordBean(Grib2Record m) {
      this.gr = m;
      id = gr.getId();
      gds = gr.getGDS();
      pds = gr.getPDS();
      pdsv = pds.getPdsVars();
      interval = pdsv.getForecastTimeInterval();
    }

     public final String getTimeUnit() {
      int unit =  pdsv.getTimeRangeUnit();
      return Grib2Tables.codeTable4_4(unit);
    }

    public final int getForecastTime() {
      return pdsv.getForecastTime();
    }

    public final String getSurfaceType() {
      String s = Grib2Tables.getTypeSurfaceNameShort(pdsv.getTypeFirstFixedSurface());
      int lev2 = pdsv.getTypeSecondFixedSurface();
      if ((lev2 != GribNumbers.UNDEFINED) && (lev2 != GribNumbers.MISSING))
        s += "/"+Grib2Tables.getTypeSurfaceNameShort(lev2);
      return s;
    }

    public String getSurfaceValue() {
      int lev2 = pdsv.getTypeSecondFixedSurface();
      if ((lev2 != GribNumbers.UNDEFINED) && (lev2 != GribNumbers.MISSING))
        return pdsv.getValueFirstFixedSurface() + "-" + lev2;
      else
        return Float.toString(pdsv.getValueFirstFixedSurface());
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
      return pdsv.getIntervalTypeName();
    }

    public final boolean isEnsemble() {
      return pdsv.isEnsemble();
    }

    public final int getVCoords() {
      return pdsv.getCoordinates();
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

    public final long getGdsOffset() {
       return gr.getGdsOffset();
     }

     public final long getPdsOffset() {
       return gr.getPdsOffset();
     }

     public final int getBands() {
       return pdsv.getNB();
     }

     public final String getCutoff() {
       return pdsv.getHoursAfter() + ":" + pdsv.getMinutesAfter();
     }

    public String toRawPdsString() {
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
      Formatter f = new Formatter();
      f.format("Grib2IndicatorSection%n");
      f.format(" Discipline = %s%n", gr.getIs().getDisciplineName());
      f.format(" Edition    = %d%n", gr.getIs().getGribEdition());
      f.format(" Length     = %d%n", gr.getIs().getGribLength());
      f.format("%nGrib2IdentificationSection%n");
      f.format(" Center        = %s%n", Grib1Tables.getCenter_idName(id.getCenter_id()));
      f.format(" Master Table  = %d%n", id.getMaster_table_version());
      f.format(" Local Table   = %d%n", id.getLocal_table_version());
      f.format(" RefTimeSignif = %s%n", id.getSignificanceOfRTName());
      f.format(" RefTime       = %s%n", id.getBaseTime());
      f.format(" ProductStatus = %s%n", id.getProductStatusName());
      f.format(" ProductType   = %s%n", id.getProductTypeName());
      f.format("%nGrib2GridDefinitionSection%n");
      f.format(" Source             = %d%n", gds.getSource());
      f.format(" Npts               = %d%n", gds.getNumberPoints());
      f.format(" QuasiRegularOctets = %d%n", gds.getOlon());
      f.format(" QuasiRegularInterp = %d%n", gds.getIolon());
      f.format(" Grid Template      = %d%n", gds.getGdtn());
      f.format("%nGrib2ProductDefinitionSection%n");
      int[] intv = pds.getPdsVars().getForecastTimeInterval();
      f.format(" Start interval     = %d%n", intv[0]);
      f.format(" End   interval     = %d%n", intv[1]);

      showRawPds(pds, f);

      return f.toString();
    }

  } // RecordBean

  ////////////////////////////////////////////////////////////////////////////
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
