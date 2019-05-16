/*
 * Copyright (c) 1998-2019 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.ui.op;

import javax.annotation.Nullable;
import ucar.nc2.ft.point.bufr.BufrCdmIndex;
import ucar.nc2.ft.point.bufr.BufrCdmIndexProto;
import ucar.nc2.ft.point.bufr.BufrField;
import ucar.nc2.iosp.bufr.Descriptor;
import ucar.ui.widget.*;
import ucar.util.prefs.PreferencesExt;
import ucar.ui.prefs.BeanTable;

import java.awt.*;
import java.io.*;
import java.lang.invoke.MethodHandles;
import java.util.*;
import java.util.List;
import javax.swing.*;
import javax.swing.table.TableColumn;

/**
 * Examine BUFR CdmIndex files
 *
 * @author caron
 * @since 6/29/11
 */
public class BufrCdmIndexPanel extends JPanel {

    private static final org.slf4j.Logger logger
                = org.slf4j.LoggerFactory.getLogger (MethodHandles.lookup ( ).lookupClass ( ));

    private PreferencesExt prefs;

    private BeanTable stationTable, fldTable;
    private JSplitPane split, split2, split3;

    private TextHistoryPane infoPopup, detailTA;
    private IndependentWindow infoWindow, detailWindow;

/**
 *
 */
    public BufrCdmIndexPanel(PreferencesExt prefs, JPanel buttPanel) {
        this.prefs = prefs;

    AbstractButton infoButton = BAMutil.makeButtcon("Information", "Show Info", false);
    infoButton.addActionListener(e -> {
        if (index == null) { return; }
        Formatter f = new Formatter();
        index.showIndex(f);
        detailTA.setText(f.toString());
        detailTA.gotoTop();
        detailWindow.show();
    });
    buttPanel.add(infoButton);

    AbstractButton writeButton = BAMutil.makeButtcon("nj22/Netcdf", "Write index", false);
    writeButton.addActionListener(e -> {
        Formatter f = new Formatter();
        try {
          if (writeIndex(f)) {
            f.format("Index written");
            detailTA.setText(f.toString());
          }

        } catch (Exception ex) {
          ex.printStackTrace();
          StringWriter sw = new StringWriter(10000);
          ex.printStackTrace(new PrintWriter(sw));
          detailTA.setText(sw.toString());
        }
        detailTA.gotoTop();
        detailWindow.show();
    });
    buttPanel.add(writeButton);

    /* AbstractButton filesButton = BAMutil.makeButtcon("Information", "Show Files", false);
    filesButton.addActionListener(e -> {
        Formatter f = new Formatter();
        showFiles(f);
        detailTA.setText(f.toString());
        detailTA.gotoTop();
        detailWindow.show();
    });
    buttPanel.add(filesButton); */

    ////////////////
    stationTable = new BeanTable(StationBean.class, (PreferencesExt) prefs.node("StationBean"), false, "stations", "BufrCdmIndexProto.Station", null);
    fldTable = new BeanTable(FieldBean.class, (PreferencesExt) prefs.node("FldBean"), false, "Fields", "BufrCdmIndexProto.Field", new FieldBean());

    JTable table = fldTable.getJTable();
    JComboBox<BufrCdmIndexProto.FldType> comboBox = new JComboBox<>(BufrCdmIndexProto.FldType.values());
    table.setDefaultEditor(BufrCdmIndexProto.FldType.class, new DefaultCellEditor(comboBox));

    TableColumn sportColumn = table.getColumnModel().getColumn(2);
    JComboBox<String> cb = new JComboBox<>();
    cb.addItem("Snowboarding");
    cb.addItem("Rowing");
    cb.addItem("Chasing toddlers");
    cb.addItem("Speed reading");
    cb.addItem("Teaching high school");
    cb.addItem("None");
    sportColumn.setCellEditor(new DefaultCellEditor(cb));

    /////////////////////////////////////////
    // the info windows
    infoPopup = new TextHistoryPane();
    infoWindow = new IndependentWindow("Extra Information", BAMutil.getImage("nj22/NetcdfUI"), infoPopup);
    infoWindow.setBounds((Rectangle) prefs.getBean("InfoWindowBounds", new Rectangle(300, 300, 500, 300)));

    detailTA = new TextHistoryPane();
    detailWindow = new IndependentWindow("Extra Information", BAMutil.getImage("nj22/NetcdfUI"), detailTA);
    detailWindow.setBounds((Rectangle) prefs.getBean("DetailWindowBounds", new Rectangle(300, 300, 500, 300)));

    setLayout(new BorderLayout());

    split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, false, stationTable, fldTable);
    split.setDividerLocation(prefs.getInt("splitPos", 800));

    add(split, BorderLayout.CENTER);

  }

/** */
    public void save() {
        stationTable.saveState(false);
        fldTable.saveState(false);
        prefs.putBeanObject("InfoWindowBounds", infoWindow.getBounds());
        prefs.putBeanObject("DetailWindowBounds", detailWindow.getBounds());
        if (split != null) prefs.putInt("splitPos", split.getDividerLocation());
        if (split2 != null) prefs.putInt("splitPos2", split2.getDividerLocation());
        if (split3 != null) prefs.putInt("splitPos3", split3.getDividerLocation());
    }

  ///////////////////////////////////////////////

  String indexFilename;
  BufrCdmIndex index;
  FieldBean rootBean;

  public void setIndexFile(String indexFilename) throws IOException {
    this.indexFilename = indexFilename;

    index = BufrCdmIndex.readIndex(indexFilename);

    List<StationBean> stations = new ArrayList<>();
    if (index.stations != null) {
      for (BufrCdmIndexProto.Station s : index.stations)
        stations.add(new StationBean(s));
    }
    stationTable.setBeans(stations);

    List<FieldBean> flds = new ArrayList<>();
    if (index.root != null) {
      rootBean = new FieldBean(null, index.root);
      addFields(index.root, rootBean, flds);
    }
    fldTable.setBeans(flds);
  }

  private void addFields(BufrCdmIndexProto.Field parent, FieldBean parentBean, List<FieldBean> flds) {
    if (parent.getFldsList() == null) return;
    parentBean.children = new ArrayList<>();
    for (BufrCdmIndexProto.Field child : parent.getFldsList()) {
      FieldBean childBean = new FieldBean(parentBean, child);
      flds.add(childBean);
      parentBean.children.add(childBean);
      addFields(child, childBean, flds);
    }
  }

  // transform beans back into an index and write it out
  public boolean writeIndex(Formatter f) throws IOException {
    makeFileChooser();
    String filename = fileChooser.chooseFilename(indexFilename);
    if (filename == null) return false;
    if (!filename.endsWith(BufrCdmIndex.NCX_IDX))
      filename += BufrCdmIndex.NCX_IDX;
    File idxFile = new File(filename);

    return BufrCdmIndex.writeIndex(index, rootBean, idxFile);
  }

  private FileManager fileChooser;

  private void makeFileChooser() {
    if (fileChooser == null)
      fileChooser = new FileManager(null, null, null, (PreferencesExt) prefs.node("FileManager"));
  }


  ////////////////////////////////////////////////////////////////////////////

  public class StationBean {
    BufrCdmIndexProto.Station s;

    public StationBean(BufrCdmIndexProto.Station s) {
      this.s = s;
    }

    public String getWmoId() {
      return s.getWmoId();
    }

    public String getName() {
      return s.getId();
    }

    public String getDescription() {
      return s.getDesc();
    }

    public double getLatitude() {
      return s.getLat();
    }

    public double getLongitude() {
      return s.getLon();
    }

    public double getAltitude() {
      return s.getAlt();
    }

    public int getCount() {
      return s.getCount();
    }

  }

  ////////////////////////////////////////////////////////////////////////////
  public class FieldBean implements BufrField {
    FieldBean parent;
    BufrCdmIndexProto.Field child;
    List<FieldBean> children;
    BufrCdmIndexProto.FldAction act;
    BufrCdmIndexProto.FldType type;

    public String editableProperties() {
      return "actionS type name";
    }

    public String hiddenProperties() {
      return "action children fxy";
    }

    public FieldBean() {
    }

    public FieldBean(FieldBean parent, BufrCdmIndexProto.Field child) {
      this.parent = parent;
      this.child = child;
    }

    @Nullable
    public String getParent() {
      if (parent == null) return null;
      Formatter f = new Formatter();
      FieldBean a = parent;
      while (a != null) {
        a = a.parent;
        if (a != null && !a.getName().isEmpty())
          f.format("../");
      }
      f.format("%s", parent.getName());
      return f.toString();
    }

    @Override
    public String getName() {
      return child.getName();
    }

    @Override
    public String getDesc() {
      return child.getDesc();
    }

    @Override
    public String getUnits() {
      return child.getUnits();
    }

    @Override
    public short getFxy() {
      return (short) child.getFxy();
    }

    @Override
    public String getFxyName() {
      return Descriptor.makeString((short) child.getFxy());
    }

    @Override
    public BufrCdmIndexProto.FldAction getAction() {
      if (act != null) return act;
      return child.getAction();
    }

    @Override
    public BufrCdmIndexProto.FldType getType() {
      if (type != null) return type;
      return child.getType();
    }

    public void setType(BufrCdmIndexProto.FldType type) {
      this.type = type;
    }

    public String getActionS() {
      if (act != null) return act.toString();
      BufrCdmIndexProto.FldAction fact = getAction();
      return fact != null ? fact.toString() : "";
    }

    public void setActionS(String actS) {
      try {
        this.act = BufrCdmIndexProto.FldAction.valueOf(actS);
      } catch (Exception ee) {
        // never mind
      }
    }

    @Override
    public boolean isSeq() {
      return child.getMax() > 0;
    }

    @Override
    public int getMin() {
      return child.getMin();
    }

    @Override
    public int getMax() {
      return child.getMax();
    }

    @Override
    public int getScale() {
      return child.getScale();
    }

    @Override
    public int getReference() {
      return child.getReference();
    }

    @Override
    public int getBitWidth() {
      return child.getBitWidth();
    }

    @Override
    public List<? extends BufrField> getChildren() {
      return children;
    }

  }

}


