/*
 * Copyright (c) 1998-2019 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.ui.op;

import thredds.featurecollection.FeatureCollectionConfig;
import thredds.featurecollection.FeatureCollectionConfigBuilder;
import thredds.inventory.MCollection;
import thredds.inventory.MFile;
import ucar.nc2.dt.GridDataset;
import ucar.nc2.ft.fmrc.*;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.time.CalendarDateFormatter;
import ucar.nc2.ui.dialog.Fmrc2Dialog;
import ucar.ui.widget.BAMutil;
import ucar.ui.widget.IndependentWindow;
import ucar.ui.widget.PopupMenu;
import ucar.ui.widget.TextHistoryPane;
import ucar.util.prefs.PreferencesExt;
import ucar.ui.prefs.BeanTable;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Formatter;
import java.util.List;
import javax.swing.*;

/**
 * ucar.nc2.ft.fmrc Fmrc refactor.
 *
 * @author caron
 * @since Jan 11, 2010
 */
@SuppressWarnings("StringConcatenationInFormatCall")
public class Fmrc2Panel extends JPanel {
  private PreferencesExt prefs;

  private BeanTable fmrTable, invTable, coordTable, gridTable;
  private JSplitPane split, split2, splitV;

  private TextHistoryPane infoTA;
  private IndependentWindow infoWindow;

  // private String collectionSpec;
  private Fmrc fmrc;
  private FmrcInv fmrcInv;
  private FmrcInvLite lite;

  private Formatter errlog, debug;

  private static final String fmrBeanDesc = "FmrInv: one for each runtime";
  private static final String dataBeanDesc = "GridDatasetInv: one for each file in the run";
  private static final String coordBeanDesc = "unique TimeCoords (from RunSeq), VertCoords";
  private static final String gridBeanDesc = "UberGrids from the FmrcInv";

  public Fmrc2Panel(PreferencesExt prefs) {
    this.prefs = prefs;

    fmrTable = new BeanTable(
            FmrBean.class, (PreferencesExt) prefs.node("DatasetBean"), false, "FmrInv", fmrBeanDesc, null);
    fmrTable.addListSelectionListener(e -> {
        FmrBean fmrBean = (FmrBean) fmrTable.getSelectedBean();
        setFmr(fmrBean.fmr);
    });

    invTable = new BeanTable(
            InvBean.class, (PreferencesExt) prefs.node("DataBean"), false, "GridDatasetInv", dataBeanDesc, null);
    /* invTable.addListSelectionListener(e -> {
        invTable.getSelectedBean();
        //setCoords(invBean.fmrInv);
        //setGrids(invBean.fmrInv);
    }); */

    coordTable = new BeanTable(
            CoordBean.class, (PreferencesExt) prefs.node("CoordBean"), false, "Time,Vert coords", coordBeanDesc, null);
    /* coordTable.addListSelectionListener(e -> {
        coordTable.getSelectedBean();
    }); */
    coordTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

    gridTable = new BeanTable(
            GridBean.class, (PreferencesExt) prefs.node("GridBean"), false, "UberGrids", gridBeanDesc, null);
    gridTable.addListSelectionListener(e -> {
        GridBean gridBean = (GridBean) gridTable.getSelectedBean();
        setSelectedCoord(gridBean);
    });

    PopupMenu varPopup = new PopupMenu(invTable.getJTable(), "Options");
    varPopup.addAction("Open in NetcdfFile Viewer", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        InvBean dsb = (InvBean) invTable.getSelectedBean();
        if (dsb == null) return;
        Fmrc2Panel.this.firePropertyChange("openNetcdfFile", null, dsb.fmrInv.getLocation());
      }
    });

    varPopup.addAction("Open in CoordSys tab", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        InvBean dsb = (InvBean) invTable.getSelectedBean();
        if (dsb == null) return;
        Fmrc2Panel.this.firePropertyChange("openCoordSys", null, dsb.fmrInv.getLocation());
      }
    });

    varPopup.addAction("Open in GridDataset tab", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        InvBean dsb = (InvBean) invTable.getSelectedBean();
        if (dsb == null) return;
        Fmrc2Panel.this.firePropertyChange("openGridDataset", null, dsb.fmrInv.getLocation());
      }
    });

    varPopup.addAction("show GridInventory XML", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        InvBean dsb = (InvBean) invTable.getSelectedBean();
        if (dsb == null) return;
        infoTA.setText(dsb.fmrInv.writeXML(null));
        infoWindow.show();
      }
    });

    varPopup = new PopupMenu(coordTable.getJTable(), "Options");
    varPopup.addAction("Show Inv", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        CoordBean bean = (CoordBean) coordTable.getSelectedBean();
        if (bean == null) return;
        showCoordInv(bean);
      }
    });

    varPopup = new PopupMenu(gridTable.getJTable(), "Options");
    varPopup.addAction("Show Inv Coords", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        GridBean bean = (GridBean) gridTable.getSelectedBean();
        if (bean == null) return;
        showGridInv(bean);
      }
    });

    // the info window
    infoTA = new TextHistoryPane(false, 5000, 50, true, false, 14);
    infoWindow = new IndependentWindow("Extra Information", BAMutil.getImage("nj22/NetcdfUI"), infoTA);
    infoWindow.setBounds((Rectangle) prefs.getBean("InfoWindowBounds", new Rectangle(300, 300, 500, 300)));

    splitV = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, false, fmrTable, invTable);
    splitV.setDividerLocation(prefs.getInt("splitPosV", 500));

    split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, false, splitV, coordTable);
    split.setDividerLocation(prefs.getInt("splitPos", 500));

    split2 = new JSplitPane(JSplitPane.VERTICAL_SPLIT, false, split, gridTable);
    split2.setDividerLocation(prefs.getInt("splitPos2", 500));

    setLayout(new BorderLayout());
    add(split2, BorderLayout.CENTER);
  }

  public void save() {
    fmrTable.saveState(false);
    invTable.saveState(false);
    coordTable.saveState(false);
    gridTable.saveState(false);
    prefs.putBeanObject("InfoWindowBounds", infoWindow.getBounds());
    prefs.putInt("splitPos", split.getDividerLocation());
    prefs.putInt("splitPos2", split2.getDividerLocation());
    prefs.putInt("splitPosV", splitV.getDividerLocation());
  }

  public void closeOpenFiles() throws IOException {
    if (fmrc != null) fmrc.close();
    fmrc = null;
  }

  public void setFmrc(String command) throws IOException {
    closeOpenFiles();
    long start = System.currentTimeMillis();

    String prefix = "featureCollection:";
    if (command.startsWith(prefix)) {
      setFmrcFromConfig(command.substring(prefix.length()));
    } else {
      setFmrcFromCollectionSpec(command);
    }

    long took = System.currentTimeMillis() - start;
    System.out.printf("that took %f secs%n", ((double)took)/1000);    
  }

  private void setFmrcFromConfig(String configFile) throws IOException {
    errlog = new Formatter();
    FeatureCollectionConfigBuilder builder = new FeatureCollectionConfigBuilder(errlog);

      // input is xml file with just the <featureCollection>
    FeatureCollectionConfig config = builder.readConfigFromFile(configFile);
    if (config == null) {
      infoTA.setText(errlog.toString());
      infoTA.gotoTop();
      infoWindow.show();
      return;
    }

    Fmrc fmrc = Fmrc.open(config, errlog);
    if (fmrc == null) {
      infoTA.setText(errlog.toString());
      infoTA.gotoTop();
      infoWindow.show();
      return;
    }

    setFmrc(fmrc);
  }

  private void setFmrcFromCollectionSpec(String collectionSpec) throws IOException {
    errlog = new Formatter();
    Fmrc fmrc = Fmrc.open(collectionSpec, errlog);
    if (fmrc == null) {
      infoTA.setText(errlog.toString());
      infoTA.gotoTop();
      infoWindow.show();
      return;
    }

    setFmrc(fmrc);
  }

  private void setFmrc(Fmrc fmrc) throws IOException {
    this.fmrc = fmrc;
    if (!showCollectionInfo(false))
      return;

    debug = new Formatter();
    fmrcInv = fmrc.getFmrcInv( debug);
    lite = new FmrcInvLite(fmrcInv);

    List<FmrBean> beanList = new ArrayList<>();
    for (FmrInv fmr : fmrcInv.getFmrList()) {
      beanList.add(new FmrBean(fmr));
    }

    fmrTable.setBeans(beanList);
    invTable.setBeans(new ArrayList());

    setCoords(fmrcInv);
    setGrids(fmrcInv);
  }

  public boolean showCollectionInfo(boolean alwaysShow) {
    if (fmrc == null) {
      infoTA.setText("No fmrc, errlog=");
      infoTA.appendLine(errlog.toString());
      if (debug != null) {
        infoTA.appendLine("\ndebug=");
        infoTA.appendLine(debug.toString());
      }
      infoTA.gotoTop();
      infoWindow.show();
      return false;
    }

    infoTA.clear();

    try( MCollection cm = fmrc.getManager()) {
      infoTA.appendLine("CollectionManager= ");
      infoTA.appendLine(cm.toString());

      int count = 0;
      infoTA.appendLine("Files found=");
      try {
        for (MFile mfile : cm.getFilesSorted()) {
          infoTA.appendLine(
              " " + mfile.getPath() + " " + new Date(mfile.getLastModified()) + " " + mfile.getLength());
          count++;
        }
      } catch (IOException e) {
        e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
      }
      infoTA.appendLine("total files=" + count);
    }

    if (alwaysShow)
      infoWindow.show();
    return true;
  }

  private Fmrc2Dialog dialog = null;
  public void showDataset() throws IOException {
    if (fmrcInv == null) return;
    if (dialog == null) {
      dialog = new Fmrc2Dialog(null);
      dialog.pack();
      dialog.addPropertyChangeListener("OK", new PropertyChangeListener() {
        public void propertyChange(PropertyChangeEvent evt) {
          Fmrc2Dialog.Data data = (Fmrc2Dialog.Data) evt.getNewValue();
          if ((data.type == null) || (data.where == null)) return;
          try {
            processDialog(data);
          } catch (IOException e) {
            e.printStackTrace();
          }
        }
      });
    }
    dialog.setFmrc(fmrc);
    dialog.setVisible(true);
  }

  private void processDialog(Fmrc2Dialog.Data data) throws IOException {
    if (data.where.startsWith("Selected UberGrid")) {
      GridBean bean = (GridBean) gridTable.getSelectedBean();
      if (bean == null) return;
      showGridInv(data, bean);
      return;
    }

    GridDataset gds = null;
    try {
      switch (data.type) {
        case "Dataset2D":
          gds = fmrc.getDataset2D(null);
          break;
        case "Best":
          gds = fmrc.getDatasetBest();
          break;
        case "Run": {
          CalendarDate date = CalendarDate.parseISOformat(null, (String) data.param);
          gds = fmrc.getRunTimeDataset(date);

          break;
        }
        case "ConstantForecast": {
          CalendarDate date = CalendarDate.parseISOformat(null, (String) data.param);
          gds = fmrc.getConstantForecastDataset(date);

          break;
        }
        case "ConstantOffset":
          gds = fmrc.getConstantOffsetDataset((Double) data.param);
          break;
      }
      if (gds == null) {
        System.out.print("The GridDataset was null, so the Fmrc2Panel isn't going to update.");
        return;
      }

      if (data.where.startsWith("NetcdfFile")) {
        firePropertyChange("openNetcdfFile", null, gds.getNetcdfFile());
      } else if (data.where.startsWith("CoordSys")) {
        firePropertyChange("openCoordSys", null, gds.getNetcdfFile());
      } else if (data.where.startsWith("GridDataset")) {
        showGridDatasetInfo(gds);
      } else if (data.where.startsWith("Grid")) {
        firePropertyChange("openGridDataset", null, gds);
      }

    } catch (IOException e) {
      e.printStackTrace();
    } finally {
      if (gds != null) gds.close();
    }
  }

  private void showGridDatasetInfo(GridDataset gds) {
    Formatter info = new Formatter();
    gds.getDetailInfo(info);
    infoTA.setText(info.toString());
    infoWindow.show();
  }

  private void setFmr(FmrInv fmr) {
    if (fmr == null) return;
    List<InvBean> beanList = new ArrayList<>();
    for (GridDatasetInv fmrInv : fmr.getInventoryList()) {
      beanList.add(new InvBean(fmrInv));
    }

    invTable.setBeans(beanList);
  }

  private void setCoords(FmrcInv fmrInv) {
    if (fmrInv == null) return;
    List<CoordBean> beanList = new ArrayList<>();
    for (FmrcInv.RunSeq tc : fmrInv.getRunSeqs())
      beanList.add(new TimeCoordBean(tc));
    for (VertCoord vc : fmrInv.getVertCoords())
      beanList.add(new VertCoordBean(vc));

    coordTable.setBeans(beanList);
  }

  private void setGrids(FmrcInv fmrInv) {
    if (fmrInv == null) return;
    List<GridBean> beanList = new ArrayList<>();
    for (FmrcInv.UberGrid grid : fmrInv.getUberGrids()) {
      beanList.add(new GridBean(grid));
    }

    gridTable.setBeans(beanList);
  }

  /* private void setGrids(GridDatasetInv fmrInv) {
    if (fmrInv == null) return;
    List<GridBean> beanList = new ArrayList<GridBean>();
    for (TimeCoord tc : fmrInv.getTimeCoords()) {
      for (GridDatasetInv.Grid grid : tc.getGrids())
        beanList.add(new GridBean(grid));
    }

   gridTable.setBeans(beanList);
  } */

  private void setSelectedCoord(GridBean gridBean) {
    List<CoordBean> beans = coordTable.getBeans();
    List<CoordBean> selected = new ArrayList<>();
    for (CoordBean bean : beans) {
      if (bean instanceof TimeCoordBean) {
        TimeCoordBean tbean = (TimeCoordBean) bean;
        if (tbean.runSeq.getName().equals(gridBean.getTimeCoordName()))
          selected.add(bean);
      } else if (bean instanceof VertCoordBean) {
        VertCoordBean vbean = (VertCoordBean) bean;
        if (vbean.vc.getName().equals(gridBean.getVertCoordName()))
          selected.add(bean);
      }
    }
    coordTable.setSelectedBeans(selected);
  }

  private void showCoordInv(CoordBean coordBean) {
    Formatter out = new Formatter();
    if (coordBean instanceof TimeCoordBean) {
      FmrcInv.RunSeq runSeq = ((TimeCoordBean) coordBean).runSeq;
      out.format("Time coordinate %s %n%n", runSeq.getName());
      for (TimeCoord tc : runSeq.getTimes()) {
        if (tc == null)
          out.format(" NULL%n");
        else
        out.format(" %s : 0x%x %n", tc, tc.hashCode());
      }

      out.format("%n Used by Grids:%n");
      List<FmrcInv.UberGrid> ugrids = runSeq.getUberGrids();
      if (ugrids != null) {
        for (FmrcInv.UberGrid ugrid : ugrids) {
          out.format(" %s%n", ugrid.getName());
        }
      }

    } else if (coordBean instanceof VertCoordBean) {
      VertCoord vc = ((VertCoordBean) coordBean).vc;
      out.format("Compare Vert coordinate %s %n%n", vc.getName());
      out.format(" Uber %s%n", vc.toString());

      for (FmrInv fmr : fmrcInv.getFmrList()) {
        out.format(" Fmr %s%n", fmr.getRunDate());
        for (VertCoord vc2 : fmr.getVertCoords()) {
          if (vc2.getName().equals(vc.getName())) {
            String isSame = vc.equalsData(vc2) ? "" : "DIFF";
            out.format("      %s %s%n", vc2.toString(), isSame);
          }
        }
      }
    }
    infoTA.setText(out.toString());
    infoWindow.show();
  }

  public void showInfo(Formatter result) throws IOException {
    if (debug != null)
      result.format("%s%n", debug.toString());
    if (fmrc != null) {
      //lite.showRuntimeOffsetMatrix(result);
      //lite.showBest(result);
      //lite.showBest2(result);
      fmrc.showDetails(result);
    }
  }

  private void showGridInv(GridBean bean) {
    Formatter out = new Formatter();
    FmrcInv.UberGrid ugrid = bean.grid;
    out.format("Show FmrcInv.UberGrid name= %s%n% 3d expected inventory %n%n", ugrid.getName(), ugrid.countExpected());

    // show actual inventory
    TimeCoord union = ugrid.getUnionTimeCoord();
    //int w = union.isInterval() ? 4 : 6;
    int w2 = union.isInterval() ? 9 : 6;
    out.format("                              ");
    for (int i=0; i<union.getNCoords(); i++)
      out.format("%"+w2+"d ", i);
    out.format("%n");
    out.format("     RunTime             Total ");
    showCoords(union, out);
    out.format("%n");

    int count = 0;
    int runidx = 0;
    for (FmrInv.GridVariable run : ugrid.getRuns()) {
      for (GridDatasetInv.Grid inv : run.getInventory()) {
        out.format(" %3d %s ", runidx, CalendarDateFormatter.toDateTimeString(run.getRunDate()));
        count += showActualInventory(inv, union, w2, out);
        out.format(" %s%n", inv.getLocation());
      }
      runidx++;
    }
    out.format("%n%3d counted inventory%n%n%n", count);

    lite.showGridInfo(ugrid.getName(), out);

    infoTA.setText(out.toString());
    infoWindow.show();
  }

  private int showActualInventory(GridDatasetInv.Grid inv, TimeCoord union, int w, Formatter out) {
    TimeCoord tc = inv.getTimeCoord();
    int nverts = inv.getVertCoordLength();
    out.format("%3d ", inv.countTotal());

    if (tc.isInterval()) {
      for (int i=0; i<union.getNCoords(); i++) {
        boolean hasInventory = tc.findInterval(union.getBound1()[i], union.getBound2()[i]) >= 0;
        if (hasInventory)
          out.format("%" + w + "d ", nverts);
        else
          out.format("%" + w + "s ", ' '); // w blanks

      }
    } else {
      for (int i=0; i<union.getNCoords(); i++) {
        boolean hasInventory = tc.findIndex(union.getOffsetTimes()[i]) >= 0;
        if (hasInventory)
          out.format("%" + w + "d ", nverts);
        else
          out.format("%" + w + "s ", ' '); // w blanks
      }
    }
    return inv.countTotal();
  }

  private void showCoords(TimeCoord tc, Formatter f) {
    if (!tc.isInterval()) {
      for (double off : tc.getOffsetTimes())
        f.format("%6.0f,", off);
    } else {
      double[] bound1 = tc.getBound1();
      double[] bound2 = tc.getBound2();
      for (int i=0; i<bound1.length; i++)
        f.format("%4.0f-%4.0f,", bound1[i], bound2[i]);
    }
  }

  private void showCoords(FmrcInvLite.ValueB timeCoords, Formatter out) {
    if (timeCoords.bounds == null) {
      for (double rc : timeCoords.offset) {
        out.format("%9.0f,", rc);
      }
    } else {
       for (int i=0; i<timeCoords.bounds.length; i+=2) {
        out.format("%4.0f-%4.0f,", timeCoords.bounds[i], timeCoords.bounds[i+1]);
      }
    }
  }

  private void showGridInv(Fmrc2Dialog.Data ddata, GridBean bean) {
    Formatter out = new Formatter();
    FmrcInv.UberGrid ugrid = bean.grid;
    FmrcInvLite.Gridset gset = lite.findGridset(ugrid.getName());
    if (gset == null) {
        out.format("showGridInv(): gset is null!");
        infoTA.setText(out.toString());
        infoWindow.show();
        return;
    }

    TimeInventory ti = null;
    try {
      switch (ddata.type) {
        case "Best":
          ti = lite.makeBestDatasetInventory();
          break;
        case "Run": {
          CalendarDate date = CalendarDate.parseISOformat(null, (String) ddata.param);
          ti = lite.makeRunTimeDatasetInventory(date);
          break;
        }
        case "ConstantForecast": {
          CalendarDate date = CalendarDate.parseISOformat(null, (String) ddata.param);
          ti = lite.getConstantForecastDataset(date);
          break;
        }
        case "ConstantOffset":
          ti = lite.getConstantOffsetDataset((Double) ddata.param);
          break;
      }
    } catch (IOException e) {
      e.printStackTrace();
    }

    if (ti == null) {
      out.format("showGridInv(): ti is null!");
      infoTA.setText(out.toString());
      infoWindow.show();
      return;
    }
    out.format("Show UberGrid '%s' for dataset '%s'%n", ugrid.getName(), ti.getName());

    double[] runCoords = ti.getRunTimeCoords(gset);
    FmrcInvLite.ValueB timeCoords = ti.getTimeCoords(gset);
    double[] offsetCoords = ti.getOffsetCoords(gset);

    out.format("                ");
    for (int i=0; i< ti.getTimeLength(gset); i++)
      out.format("%9d,", i);
    out.format("%n");

    if (runCoords != null) {
      out.format("RunTimeCoords = ");
      for (double rc : runCoords) {
        out.format("%9.2f,", rc);
      }
      out.format("%n");
    }

    if (offsetCoords != null) {
      out.format("offsetCoords = ");
      for (double rc : offsetCoords) {
        out.format("%9.2f,", rc);
      }
      out.format("%n");
    }

    if (timeCoords != null) {
      out.format("  timeCoords = ");
      showCoords(timeCoords, out);
      out.format("%n");
    }

    FmrcInvLite.Gridset.Grid grid = lite.findGrid(ugrid.getName());
    int ntimes = ti.getTimeLength(gset);
    out.format("%nInventory%n");
    if (grid != null) {
      for (int i = 0; i < ntimes; i++) {
        TimeInventory.Instance ins = ti.getInstance(grid, i);
        if (ins != null)
          out.format(" %3d: %3d, %s%n", i, ins.getDatasetIndex(), ins.getDatasetLocation());
        else
          out.format(" %3d: MISSING%n", i);
      }
    } else {
        out.format("showGridInv(): grid is null!");
    }

    infoTA.setText(out.toString());
    infoWindow.show();
  }


  /* for a given offset hour and GridVariable, find the expected and actual inventory
  private FmrcInv.Inventory getInventory(FmrInv.GridVariable grid, double hour) {
    int actual = 0, expected = 0;
      TimeCoord tExpect = grid.getTimeExpected();
      if (tExpect.findIndex(hour) >= 0)
        expected += grid.getNVerts();

      for (GridDatasetInv.Grid inv : grid.getInventory()) {
        TimeCoord tc = inv.getTimeCoord();
        if (tc.findIndex(hour) >= 0)
          actual += inv.getVertCoordLength();
      }

    return new FmrcInv.Inventory(actual, expected);
  }


  private int showCount(GridDatasetInv.Grid inv, double[] offsets, TimeCoord expected, Formatter out) {

    int count = 0;
    TimeCoord tc = inv.getTimeCoord();
    int nverts = inv.getVertCoordLength();
    out.format("%3d ", inv.countTotal());
    for (double wantOffset : offsets) {
      boolean hasExpected = expected.findIndex(wantOffset) >= 0;
      boolean hasInventory = tc.findIndex(wantOffset) >= 0;
      if (hasExpected && hasInventory)
        out.format("%6d ", nverts);
      else if (hasExpected && !hasInventory) {
        out.format("%6s ", "0/" + nverts);
      } else if (!hasExpected && hasInventory)
        out.format("%6s ", nverts + "/0");
      else
        out.format("       "); // blank

      int ninv = hasInventory ? nverts : 0;
      count += ninv;
    }
    return count;
  }   */

  public class FmrBean {
    FmrInv fmr;

    // no-arg constructor
    public FmrBean() {
    }

    // create from a dataset
    public FmrBean(FmrInv fmr) {
      this.fmr = fmr;
    }

    public CalendarDate getRunDate() throws IOException {
      return fmr.getRunDate();
    }

    /* public String getName() {
      return fmr.getName();
    } */

  }

  public class InvBean {
    GridDatasetInv fmrInv;

    // no-arg constructor
    public InvBean() {
    }

    // create from a dataset
    public InvBean(GridDatasetInv fmr) {
      this.fmrInv = fmr;
    }

    /* public String getName() {
      return fmrInv.getName();
    }  */

    public String getLocation() {
      return fmrInv.getLocation();
    }
  }


  public class GridBean {
    FmrcInv.UberGrid grid;

    // no-arg constructor
    public GridBean() {
    }

    // create from a dataset
    public GridBean(FmrcInv.UberGrid grid) {
      this.grid = grid;
    }

    public String getName() {
      return grid.getName();
    }

    public String getTimeCoordName() {
      return grid.getTimeCoordName();
    }

    public String getVertCoordName() {
      return grid.getVertCoordName();
    }

    public int getCount() {
      return grid.countTotal();
    }

    public int getExpected() {
      return grid.countExpected();
    }

    public boolean getStatus() {
      return getExpected() == getCount();
    }
  }

  public abstract class CoordBean {
    // no-arg constructor
    public CoordBean() {
    }

    public abstract String getType();

    public abstract String getName();

    public abstract String getCoords();
  }

  public class TimeCoordBean extends CoordBean {
    FmrcInv.RunSeq runSeq;

    // no-arg constructor
    public TimeCoordBean() {
    }

    // create from a dataset
    public TimeCoordBean(FmrcInv.RunSeq tc) {
      this.runSeq = tc;
    }

    public String getType() {
      return "time";
    }

    public String getName() {
      return runSeq.getName();
    }

    public String getCoords() {
      Formatter sb = new Formatter();
      TimeCoord tc = runSeq.getUnionTimeCoord();
      if (!tc.isInterval()) {
        for (double off : tc.getOffsetTimes())
          sb.format("%f,", off);
      } else {
        double[] bound1 = tc.getBound1();
        double[] bound2 = tc.getBound2();
        for (int i=0; i<bound1.length; i++)
          sb.format("(%f %f),", bound1[i], bound2[i]);
      }
      return sb.toString();
    }
  }

  public class VertCoordBean extends CoordBean {
    VertCoord vc;

    // no-arg constructor
    public VertCoordBean() {
    }

    // create from a dataset
    public VertCoordBean(VertCoord tc) {
      this.vc = tc;
    }

    public String getType() {
      return "vert";
    }

    public String getName() {
      return vc.getName();
    }

    public String getCoords() {
      Formatter sb = new Formatter();
      double[] values1 = vc.getValues1();
      double[] values2 = vc.getValues2();
      if (values2 == null) {
        for (double lev : values1)
          sb.format("%s ", lev);
      } else {
        for (int i = 0; i < values1.length; i++) {
          sb.format("(%f,%f) ", values1[i], values2[i]);
        }
      }

      return sb.toString();
    }
  }

}
