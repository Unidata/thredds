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

import ucar.util.prefs.PreferencesExt;
import ucar.util.prefs.ui.BeanTableSorted;
import ucar.unidata.io.RandomAccessFile;
import ucar.grid.*;
import ucar.grib.grib1.Grib1WriteIndex;
import ucar.grib.grib2.Grib2Input;
import ucar.grib.grib2.Grib2WriteIndex;
import ucar.grib.GribGridRecord;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Attribute;
import ucar.nc2.iosp.grib.GribGridServiceProvider;
import ucar.nc2.iosp.IOServiceProvider;
import ucar.nc2.iosp.grid.GridHorizCoordSys;

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
import java.util.List;

/**
 * ToolsUI/Iosp/Bufr
 *
 * @author caron
 * @since Aug 15, 2008
 */
public class GribTable extends JPanel {
  private PreferencesExt prefs;

  private BeanTableSorted gridRecordTable, gdsTable;
  private JSplitPane split, split2;

  private TextHistoryPane infoTA, infoPopup;
  private IndependentWindow infoWindow;

  private StructureTable dataTable;
  private IndependentWindow dataWindow;
  private FileManager fileChooser;

  public GribTable(PreferencesExt prefs) {
    this.prefs = prefs;

    String tooltip = "from the index";
    gridRecordTable = new BeanTableSorted(GridRecordBean.class, (PreferencesExt) prefs.node("GridRecordBean"), false, "GribGridRecord", tooltip);
    gridRecordTable.addListSelectionListener(new ListSelectionListener() {
      public void valueChanged(ListSelectionEvent e) {
        GridRecordBean mb = (GridRecordBean) gridRecordTable.getSelectedBean();
        infoTA.setText( mb.gr.toString());
      }
    });

    thredds.ui.PopupMenu varPopup = new thredds.ui.PopupMenu(gridRecordTable.getJTable(), "Options");
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


    String tooltip2 = "Unique Horiz coord systems (GDS)";
    gdsTable = new BeanTableSorted(GdsBean.class, (PreferencesExt) prefs.node("GdsBean"), false, "GridHorizCoordSys", tooltip2);
    gdsTable.addListSelectionListener(new ListSelectionListener() {
      public void valueChanged(ListSelectionEvent e) {
        GdsBean bean = (GdsBean) gdsTable.getSelectedBean();
        if (bean == null)return;
        GridDefRecord gds = bean.horizCoordSys.getGds();
        Formatter f = new Formatter();
        f.format("GDS keys%n");
        List<String> attKeys = new ArrayList<String>(gds.getKeys());
        Collections.sort(attKeys);
        for (String key : attKeys) {
          f.format(" %s == %s %n", key, gds.getParam(key));
        }
        infoTA.setText( f.toString());
      }
    });

    infoTA = new TextHistoryPane();

    // the info window
    infoPopup = new TextHistoryPane();
    infoWindow = new IndependentWindow("Extra Information", BAMutil.getImage("netcdfUI"), infoPopup);
    infoWindow.setBounds((Rectangle) prefs.getBean("InfoWindowBounds", new Rectangle(300, 300, 500, 300)));

    split2 = new JSplitPane(JSplitPane.VERTICAL_SPLIT, false, gdsTable, infoTA);
    split2.setDividerLocation(prefs.getInt("splitPos2", 800));

    split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, false, gridRecordTable, split2);
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
    gridRecordTable.saveState(false);
    gdsTable.saveState(false);
    prefs.putBeanObject("InfoWindowBounds", infoWindow.getBounds());
    prefs.putInt("splitPos", split.getDividerLocation());
    prefs.putInt("splitPos2", split2.getDividerLocation());
  }

  private String location;
  private GridTableLookup lookup;
  private GridIndex index = null;
  private NetcdfFile ncfile;

  public void setGribFile(RandomAccessFile raf) throws IOException {
    this.location = raf.getLocation();
    if (location.endsWith(".gbx"))
      setGribFileIndex(raf);

    // get the lookup
    GribGridServiceProvider iosp = new GribGridServiceProvider();
    ncfile = new GribNetcdfFile(iosp, raf);
    lookup = iosp.getLookup();

    // get the edition
    raf.seek(0);
    Grib2Input g2i = new Grib2Input(raf);
    int edition = g2i.getEdition();
    File gribFile = new File(raf.getLocation());

    // get the index, write to temp file
    File indexFile = File.createTempFile("GribTable", "gbx");
    if (edition == 1) {
      index = new Grib1WriteIndex().writeGribIndex(gribFile, indexFile.getPath(), raf, true);
    } else if (edition == 2) {
      index = new Grib2WriteIndex().writeGribIndex(gribFile, indexFile.getPath(), raf, true);
    }

    // fill the index bean table
    java.util.List<GridRecordBean> grList = new ArrayList<GridRecordBean>();
    for (GridRecord gr : index.getGridRecords()) {
      grList.add(new GridRecordBean((GribGridRecord) gr));
    }

    // find all the GridHorizCoordSys
    Map<String, GridHorizCoordSys> hcsMap = new HashMap<String, GridHorizCoordSys>();
    List<GridDefRecord> hcsList = index.getHorizCoordSys();
    for (GridDefRecord gds : hcsList) {
      GridHorizCoordSys hcs = new GridHorizCoordSys(gds, lookup, null);
      hcsMap.put(gds.getParam(GridDefRecord.GDS_KEY), hcs);
    }

    java.util.List<GdsBean> gdsList = new ArrayList<GdsBean>();
    for (String key : hcsMap.keySet()) {
      GridHorizCoordSys hcs = hcsMap.get(key);
      gdsList.add(new GdsBean(key, hcs));
    }

    gridRecordTable.setBeans(grList);
    gdsTable.setBeans(gdsList);
  }

  public void showInfo(Formatter f) {
    Map<String, String> atts = index.getGlobalAttributes();
    ArrayList<String> attKeys = new ArrayList<String>( atts.keySet());
    Collections.sort(attKeys);
    f.format("Grib Index Global Attributes %n");
    for (String key : attKeys) {
      f.format(" %s == %s %n", key, atts.get(key));
    }
    f.format("%nFile Global Attributes %n");
    for (Attribute att : ncfile.getGlobalAttributes()) {
      f.format(" %s %n", att);
    }
  }

  private class GribNetcdfFile extends NetcdfFile {
    GribNetcdfFile(IOServiceProvider iosp, RandomAccessFile raf) throws IOException {
      super(iosp, raf, raf.getLocation(), null);
    }
  }

  private void setGribFileIndex(RandomAccessFile raf) throws IOException {
    this.location = raf.getLocation();
  }


  public class GridRecordBean {
    GribGridRecord gr;
    GridParameter param;

    // no-arg constructor
    public GridRecordBean() {
    }

    public GridRecordBean(GribGridRecord m) {
      this.gr = m;
      param = lookup.getParameter(gr);
    }

    public String getLevel() {
      return gr.getLevel1() + "/" + gr.getLevel2();
    }

    public String getLevelType() {
      return gr.getLevelType1() + "/" + gr.getLevelType2();
    }

    public String getLevelName() {
      return lookup.getLevelName(gr);
    }

    public String getLevelDescription() {
      return lookup.getLevelDescription(gr);
    }

    public String getLevelUnit() {
      return lookup.getLevelUnit(gr);
    }

    public Date getReferenceTime() {
      return gr.getReferenceTime();
    }

    public Date getValidTime() {
      return gr.getValidTime();
    }

    public int getOffsetHour() {
      return gr.getValidTimeOffset();
    }

    public String getName() {
      return param.getName();
    }

    public String getDesc() {
      return param.getDescription();
    }

    public int getDiscipline() {
      return gr.discipline;
    }

    public String getUnit() {
      return param.getUnit();
    }

    public String getGdsId() {
      return gr.getGridDefRecordId();
    }

    public int getDecimalScale() {
      return gr.getDecimalScale();
    }

    public String getTable() {
      return gr.center + "-" + gr.subCenter + "-" + gr.table;
    }

    public String getParamNo() {
      return gr.productTemplate + "-" + gr.category + "-" + gr.paramNumber;
    }
  }

  public class GdsBean {
    GridHorizCoordSys horizCoordSys;
    String key;

    // no-arg constructor
    public GdsBean() {
    }

    public GdsBean(String key, GridHorizCoordSys m) {
      this.key = key;
      this.horizCoordSys = m;
    }

    public String getKey() {
      return key;
    }

    public String getGridName() {
      return horizCoordSys.getGridName();
    }

    public String getID() {
      return horizCoordSys.getID();
    }

    public boolean isLatLon() {
      return horizCoordSys.isLatLon();
    }

    public int getNx() {
      return horizCoordSys.getNx();
    }

    public int getNy() {
      return horizCoordSys.getNy();
    }

    public double getDxInKm() {
      return horizCoordSys.getDxInKm();
    }

    public double getDyInKm() {
      return horizCoordSys.getDyInKm();
    }

  }


}
