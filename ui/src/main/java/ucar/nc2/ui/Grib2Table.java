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

import ucar.grib.grib1.Grib1Tables;
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
import java.io.*;
import java.util.*;

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

  private TextHistoryPane infoTA, infoPopup;
  private IndependentWindow infoWindow;

  private StructureTable dataTable;
  private IndependentWindow dataWindow;
  private FileManager fileChooser;

  public Grib2Table(PreferencesExt prefs) {
    this.prefs = prefs;

    recordTable = new BeanTableSorted(RecordBean.class, (PreferencesExt) prefs.node("GridRecordBean"), false);
    recordTable.addListSelectionListener(new ListSelectionListener() {
      public void valueChanged(ListSelectionEvent e) {
        RecordBean mb = (RecordBean) recordTable.getSelectedBean();
        infoTA.setText(mb.gr.toString());
      }
    });


    thredds.ui.PopupMenu varPopup = new thredds.ui.PopupMenu(recordTable.getJTable(), "Options");
    /* varPopup.addAction("Show GDS", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        GridRecordBean bean = (GridRecordBean) gridRecordTable.getSelectedBean();
        GridHorizCoordSys hcs = index2nc.getHorizCoordSys(bean.gr);

        infoTA.clear();
        Formatter f = new Formatter();
        try {
          if (!vb.gr.isTablesComplete()) {
            f.format(" MISSING DATA DESCRIPTORS= ");
            vb.gr.showMissingFields(f);
            f.format("%n%n");
          }

          vb.gr.dump(f);
        } catch (IOException e1) {
          JOptionPane.showMessageDialog(GribTable.this, e1.getMessage());
          e1.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
        infoTA.appendLine(f.toString());
        infoTA.gotoTop();
        infoWindow.showIfNotIconified();
      }
    });  */


    gdsTable = new BeanTableSorted(GdsBean.class, (PreferencesExt) prefs.node("GdsBean"), false);
    gdsTable.addListSelectionListener(new ListSelectionListener() {
      public void valueChanged(ListSelectionEvent e) {
        GdsBean bean = (GdsBean) gdsTable.getSelectedBean();
        infoPopup.setText(bean.gds.toString());
        infoWindow.setVisible(true);
        gdsTable.clearSelection();
      }
    });

    productTable = new BeanTableSorted(ProductBean.class, (PreferencesExt) prefs.node("ProductBean"), false);
    productTable.addListSelectionListener(new ListSelectionListener() {
      public void valueChanged(ListSelectionEvent e) {
        ProductBean pb = (ProductBean) productTable.getSelectedBean();
        infoPopup.setText(pb.toString());
        infoWindow.setVisible(true);
      }
    });

    infoTA = new TextHistoryPane();

    // the info window
    infoPopup = new TextHistoryPane();
    infoWindow = new IndependentWindow("Extra Information", BAMutil.getImage("netcdfUI"), infoPopup);
    infoWindow.setBounds((Rectangle) prefs.getBean("InfoWindowBounds", new Rectangle(300, 300, 500, 300)));

    split2 = new JSplitPane(JSplitPane.VERTICAL_SPLIT, false, recordTable, productTable);
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
    prefs.putInt("splitPos", split.getDividerLocation());
    prefs.putInt("splitPos2", split2.getDividerLocation());
  }

  private String location;

  public void setGribFile(RandomAccessFile raf) throws IOException {
    this.location = raf.getLocation();
    if (location.endsWith(".gbx"))
      setGribFileIndex(raf);

    Map<Integer, Grib2GridDefinitionSection> gdsSet = new HashMap<Integer, Grib2GridDefinitionSection>();
    Grib2Input reader = new Grib2Input(raf);
    raf.seek(0);
    reader.scan(false, false);

    java.util.List<RecordBean> recordList = new ArrayList<RecordBean>();
    for (Grib2Record gr : reader.getRecords()) {
      recordList.add(new RecordBean(gr));
      Grib2GridDefinitionSection gds = gr.getGDS();
      gdsSet.put(gds.getGdsKey(), gds);
    }
    recordTable.setBeans(recordList);
    System.out.printf("num records = %d%n", recordList.size());

    // productList
    java.util.List<ProductBean> productList = new ArrayList<ProductBean>();
    for (Grib2Product gr : reader.getProducts())
      productList.add(new ProductBean(gr));
    System.out.printf("num products = %d%n", productList.size());
    if (productList.size() == 0) {
      for (Grib2Record gr : reader.getRecords())
        productList.add(new ProductBean(gr));
    }
    productTable.setBeans(productList);

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
      return Grib1Tables.getCenter_idName( id.getCenter_id() );
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

    // no-arg constructor
    public ProductBean() {
    }

    public ProductBean(Grib2Product prod) {
      Grib2ProductDefinitionSection pds = prod.getPDS();
      pdsv = pds.getPdsVars();
    }

    public ProductBean(Grib2Record r) {
      Grib2ProductDefinitionSection pds = r.getPDS();
      pdsv = pds.getPdsVars();
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


    public String toString() {
      byte[] bytes = pdsv.getPDSBytes();
      Formatter f = new Formatter();
      int count = 1;
      for (byte b : bytes) {
        f.format(" %d : %d%n", count++, b);
      }
      return f.toString();
    }

    ///////////////

    public final int getTemplate() {
      return pdsv.getProductDefinition();
    }

    public final int getVCoords() {
      return pdsv.getCoordinates();
    }

    public final String getProdId() {
      return pdsv.getParameterCategory() + "-" + pdsv.getProductDefinition() + "-" + pdsv.getParameterNumber();
    }

    public final int getGenProc() {
      return pdsv.getTypeGenProcess();
    }

    public final int getChem() {
      return pdsv.getChemicalType();
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

    public final int getAnalProc() {
      return pdsv.getAnalysisGenProcess();
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
