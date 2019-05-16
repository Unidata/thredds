/*
 * Copyright (c) 1998-2019 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.ui.grib;

import ucar.ma2.DataType;
import ucar.nc2.constants.CDM;
import ucar.nc2.grib.GribUtils;
import ucar.nc2.grib.grib1.*;
import ucar.nc2.grib.grib1.tables.Grib1Customizer;
import ucar.nc2.grib.grib2.*;
import ucar.nc2.grib.grib2.table.Grib2Tables;
import ucar.ui.widget.BAMutil;
import ucar.ui.widget.IndependentWindow;
import ucar.ui.widget.PopupMenu;
import ucar.ui.widget.TextHistoryPane;
import ucar.nc2.util.Misc;
import ucar.unidata.io.RandomAccessFile;
import ucar.util.prefs.PreferencesExt;
import ucar.ui.prefs.BeanTable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.util.*;
import java.util.List;

/**
 * Examine GRIB index (gbx9) files
 *
 * @author caron
 * @since 12/10/12
 */
public class GribIndexPanel extends JPanel {
  private PreferencesExt prefs;

  private BeanTable recordTable, gds1Table, gds2Table;
  private JSplitPane split;

  private TextHistoryPane infoPopup, detailTA;
  private IndependentWindow infoWindow, detailWindow;

  private String indexFile;

  public GribIndexPanel(PreferencesExt prefs, JPanel buttPanel) {
    this.prefs = prefs;

    AbstractButton infoButton = BAMutil.makeButtcon("Information", "Show Info", false);
    infoButton.addActionListener(e -> {
        Formatter f = new Formatter();
        showIndex(f);
        detailTA.setText(f.toString());
        detailTA.gotoTop();
        detailWindow.show();
    });
    buttPanel.add(infoButton);


    /* AbstractButton filesButton = BAMutil.makeButtcon("Information", "Show Files", false);
    filesButton.addActionListener(e -> {
        Formatter f = new Formatter();
        showFiles(f);
        detailTA.setText(f.toString());
        detailTA.gotoTop();
        detailWindow.show();
    });
    buttPanel.add(filesButton);    */

    ////////////////////////////

    PopupMenu popup;

    recordTable = new BeanTable(RecordBean.class, (PreferencesExt) prefs.node("Grib2RecordBean"), false);

    popup = new PopupMenu(recordTable.getJTable(), "Options");
    popup.addAction("Show Record", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        RecordBean bean = (RecordBean) recordTable.getSelectedBean();
        if (bean != null) {
          Formatter f = new Formatter();
          bean.show(f);
          infoPopup.setText(f.toString());
          infoPopup.gotoTop();
          infoWindow.show();
        }
      }
    });

    gds1Table = new BeanTable(Gds1Bean.class, (PreferencesExt) prefs.node("GdsRecordBean"), false);
    PopupMenu varPopup = new PopupMenu(gds1Table.getJTable(), "Options");

    varPopup.addAction("Compare Raw GDS Bytes", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        List list = gds1Table.getSelectedBeans();
        if (list.size() == 2) {
          Gds1Bean bean1 = (Gds1Bean) list.get(0);
          Gds1Bean bean2 = (Gds1Bean) list.get(1);
          Formatter f = new Formatter();
          compareData(bean1, bean2, f);
          infoPopup.setText(f.toString());
          infoPopup.gotoTop();
          infoWindow.show();
        }
      }
    });

    gds2Table = new BeanTable(Gds2Bean.class, (PreferencesExt) prefs.node("Gds2RecordBean"), false);
    varPopup = new PopupMenu(gds2Table.getJTable(), "Options");

    varPopup.addAction("Compare Raw GDS Bytes", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        List list = gds2Table.getSelectedBeans();
        if (list.size() == 2) {
          Gds2Bean bean1 = (Gds2Bean) list.get(0);
          Gds2Bean bean2 = (Gds2Bean) list.get(1);
          Formatter f = new Formatter();
          compareData(bean1, bean2, f);
          infoPopup.setText(f.toString());
          infoPopup.gotoTop();
          infoWindow.show();
        }
      }
    });


    /////////////////////////////////////////
    // the info windows
    infoPopup = new TextHistoryPane();
    infoWindow = new IndependentWindow("Extra Information", BAMutil.getImage("nj22/NetcdfUI"), infoPopup);
    infoWindow.setBounds((Rectangle) prefs.getBean("InfoWindowBounds", new Rectangle(300, 300, 500, 300)));

    detailTA = new TextHistoryPane();
    detailWindow = new IndependentWindow("Extra Information", BAMutil.getImage("nj22/NetcdfUI"), detailTA);
    detailWindow.setBounds((Rectangle) prefs.getBean("DetailWindowBounds", new Rectangle(300, 300, 500, 300)));

    setLayout(new BorderLayout());

    split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, false, recordTable, gds1Table);
    split.setDividerLocation(prefs.getInt("splitPos", 500));

    add(split, BorderLayout.CENTER);
  }

  Object gc = null;

  public void save() {
    recordTable.saveState(false);
    gds1Table.saveState(false);
    gds2Table.saveState(false);
    prefs.putBeanObject("InfoWindowBounds", infoWindow.getBounds());
    prefs.putBeanObject("DetailWindowBounds", detailWindow.getBounds());
    prefs.putInt("splitPos", split.getDividerLocation());
  }

  public void closeOpenFiles() throws IOException {
    // if (gc != null) gc.close();
    gc = null;
  }

  ///////////////////////////////////////////////
  public void setIndexFile(String indexFile) throws IOException {
    closeOpenFiles();
    this.indexFile = indexFile;
    this.cust1 = null;
    this.cust2 = null;

    try (RandomAccessFile raf = new RandomAccessFile(indexFile, "r")) {
      raf.seek(0);
      String magic = raf.readString(Grib2Index.MAGIC_START.getBytes(CDM.utf8Charset).length);
      if (magic.equals(Grib2Index.MAGIC_START)) {
        readIndex2(indexFile);
      } else if (magic.equals(Grib1Index.MAGIC_START)) {
        readIndex1(indexFile);
      } else
        throw new IOException("Not a grib index file =" + magic);
    }

  }

  public void readIndex1(String filename) throws IOException {
    Grib1Index g1idx = new Grib1Index();
    g1idx.readIndex(filename, 0, thredds.inventory.CollectionUpdateType.nocheck);

    java.util.List<RecordBean> records = new ArrayList<>();
    for (Grib1Record gr : g1idx.getRecords())
      records.add(new RecordBean(gr));
    recordTable.setBeans(records);
    gc = g1idx;

    java.util.List<Gds1Bean> gdsList = new ArrayList<>();
    for (Grib1SectionGridDefinition gds : g1idx.getGds())
      gdsList.add(new Gds1Bean(gds));
    gds1Table.setBeans(gdsList);

    split.setBottomComponent(gds1Table);
    split.setDividerLocation(prefs.getInt("splitPos", 500));
  }

  public void readIndex2(String filename) throws IOException {
    Grib2Index g2idx = new Grib2Index();
    g2idx.readIndex(filename, 0, thredds.inventory.CollectionUpdateType.nocheck);

    java.util.List<RecordBean> records = new ArrayList<>();
    for (Grib2Record gr : g2idx.getRecords())
      records.add(new RecordBean(gr));
    recordTable.setBeans(records);
    gc = g2idx;

    java.util.List<Gds2Bean> gdsList = new ArrayList<>();
    for (Grib2SectionGridDefinition gds : g2idx.getGds())
      gdsList.add(new Gds2Bean(gds));
    gds2Table.setBeans(gdsList);

    split.setBottomComponent(gds2Table);
    split.setDividerLocation(prefs.getInt("splitPos", 500));
  }

  private void showIndex(Formatter f) {
    if (gc == null) return;
    if (gc instanceof Grib1Index) {
      Grib1Index index1 = (Grib1Index) gc;
      f.format("GRIB1 = %s%n", index1);
    } else if (gc instanceof Grib2Index) {
      Grib2Index index2 = (Grib2Index) gc;
      f.format("GRIB2 = %s%n", index2);
    }

  }

  private void compareData(RecordBean bean1, RecordBean bean2, Formatter f) {
    byte[]  data1 = bean1.gdsBytes();
    byte[]  data2 = bean2.gdsBytes();
    Misc.compare(data1, data2, f);
  }

  private void compareData(Gds1Bean bean1, Gds1Bean bean2, Formatter f) {
    byte[]  data1 = bean1.gdss.getRawBytes();
    byte[]  data2 = bean2.gdss.getRawBytes();
    Misc.compare(data1, data2, f);
  }

  private void compareData(Gds2Bean bean1, Gds2Bean bean2, Formatter f) {
    byte[]  data1 = bean1.gdss.getRawBytes();
    byte[]  data2 = bean2.gdss.getRawBytes();
    Misc.compare(data1, data2, f);
  }

  ////////////////////////////////////////////////////////////////////////////
  Grib1Customizer cust1;
  Grib2Tables cust2;

  public class RecordBean {
    Grib1Record gr1;
    Grib2Record gr2;

    public RecordBean() {
    }

    public RecordBean(Grib2Record gr) throws IOException {
      this.gr2 = gr;
      if (cust2 == null) cust2 = Grib2Tables.factory(gr2);
    }

    public RecordBean(Grib1Record gr) {
      this.gr1 = gr;
      if (cust1 == null) cust1 = Grib1Customizer.factory(gr1, null);
    }

    public byte[] gdsBytes() {
      return (gr2 == null) ? gr1.getGDSsection().getRawBytes() : gr2.getGDSsection().getRawBytes();
    }

    public int getFile() {
      return (gr2 == null) ? gr1.getFile() : gr2.getFile();
    }

    public long getGdsCRC() {
      return (gr2 == null) ? gr1.getGDSsection().calcCRC() : gr2.getGDSsection().calcCRC();
    }

    public int getPredefinedGrid() {
      return (gr2 == null) ? gr1.getGDSsection().getPredefinedGridDefinition() : -1;
    }

    public String getReferenceDate() {
      return (gr2 == null) ? gr1.getReferenceDate().toString() : gr2.getReferenceDate().toString();
    }

    public String getVariable() {
      return (gr2 == null) ? Integer.toString(gr1.getPDSsection().getParameterNumber()) : Grib2Utils.getVariableName(gr2);
    }

    public long getStart() {
      return (gr2 == null) ? gr1.getIs().getStartPos() : gr2.getIs().getStartPos();
    }

    public long getLength() {
      return (gr2 == null) ? gr1.getIs().getMessageLength() : gr2.getIs().getMessageLength();
    }

    private void show(Formatter f) {
      if (gr2 == null) show(gr1, f);
      else show(gr2, f);
    }

    private void show(Grib1Record gr1, Formatter f) {
      Grib1CollectionPanel.showCompleteRecord(cust1, gr1, indexFile, f);
    }

    private void show(Grib2Record gr2, Formatter f) {
      Grib2Show.showCompleteGribRecord(f, indexFile, gr2, cust2);
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

    public int getHash() {
      return gds.hashCode();
    }

    public long getCRC() {
      return gdss.calcCRC();
    }

    public int getTemplate() {
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

    public long getCRC() {
      return gdss.calcCRC();
    }

    public int getTemplate() {
      return gdss.getGDSTemplateNumber();
    }

    public String getGridName() {
      return cust2.getCodeTableValue("3.1", gdss.getGDSTemplateNumber());
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

}


