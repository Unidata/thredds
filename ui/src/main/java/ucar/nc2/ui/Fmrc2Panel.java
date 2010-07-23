/*
 * Copyright (c) 1998 - 2010. University Corporation for Atmospheric Research/Unidata
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

import thredds.inventory.CollectionManager;
import thredds.inventory.CollectionSpecParser;
import thredds.inventory.DatasetCollectionManager;
import thredds.inventory.MFile;
import ucar.nc2.dt.GridDataset;
import ucar.nc2.ui.dialog.Fmrc2Dialog;
import ucar.util.prefs.PreferencesExt;
import ucar.util.prefs.ui.BeanTableSorted;
import ucar.nc2.ft.fmrc.*;
import ucar.nc2.units.DateFormatter;

import javax.swing.*;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.ListSelectionEvent;

import thredds.ui.TextHistoryPane;
import thredds.ui.IndependentWindow;
import thredds.ui.BAMutil;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.util.*;
import java.util.List;
import java.awt.event.ActionEvent;
import java.awt.*;
import java.io.IOException;

/**
 * ucar.nc2.ft.fmrc Fmrc refactor.
 *
 * @author caron
 * @since Jan 11, 2010
 */
public class Fmrc2Panel extends JPanel {
  private PreferencesExt prefs;

  private BeanTableSorted fmrTable, invTable, coordTable, gridTable;
  private JSplitPane split, split2, splitV;

  private TextHistoryPane infoTA;
  private IndependentWindow infoWindow;

  // private String collectionSpec;
  private Fmrc fmrc;
  private FmrcInv fmrcInv;
  private FmrcInvLite lite;

  private Formatter errlog, debug;
  private DateFormatter df = new DateFormatter();

  private static final String fmrBeanDesc = "FmrInv: one for each runtime";
  private static final String dataBeanDesc = "GridDatasetInv: one for each file in the run";
  private static final String coordBeanDesc = "unique TimeCoords (from RunSeq), VertCoords";
  private static final String gridBeanDesc = "UberGrids from the FmrcInv";

  public Fmrc2Panel(PreferencesExt prefs) {
    this.prefs = prefs;

    fmrTable = new BeanTableSorted(FmrBean.class, (PreferencesExt) prefs.node("DatasetBean"), false, "FmrInv", fmrBeanDesc);
    fmrTable.addListSelectionListener(new ListSelectionListener() {
      public void valueChanged(ListSelectionEvent e) {
        FmrBean fmrBean = (FmrBean) fmrTable.getSelectedBean();
        setFmr(fmrBean.fmr);
      }
    });

    invTable = new BeanTableSorted(InvBean.class, (PreferencesExt) prefs.node("DataBean"), false, "GridDatasetInv", dataBeanDesc);
    invTable.addListSelectionListener(new ListSelectionListener() {
      public void valueChanged(ListSelectionEvent e) {
        InvBean invBean = (InvBean) invTable.getSelectedBean();
        //setCoords(invBean.fmrInv);
        //setGrids(invBean.fmrInv);
      }
    });

    coordTable = new BeanTableSorted(CoordBean.class, (PreferencesExt) prefs.node("CoordBean"), false, "Time,Vert coords", coordBeanDesc);
    coordTable.addListSelectionListener(new ListSelectionListener() {
      public void valueChanged(ListSelectionEvent e) {
        CoordBean coordBean = (CoordBean) coordTable.getSelectedBean();
      }
    });
    coordTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

    gridTable = new BeanTableSorted(GridBean.class, (PreferencesExt) prefs.node("GridBean"), false, "UberGrids", gridBeanDesc);
    gridTable.addListSelectionListener(new ListSelectionListener() {
      public void valueChanged(ListSelectionEvent e) {
        GridBean gridBean = (GridBean) gridTable.getSelectedBean();
        setSelectedCoord(gridBean);
      }
    });

    thredds.ui.PopupMenu varPopup = new thredds.ui.PopupMenu(invTable.getJTable(), "Options");
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
        infoWindow.showIfNotIconified();
      }
    });

    varPopup = new thredds.ui.PopupMenu(coordTable.getJTable(), "Options");
    varPopup.addAction("Show Inv", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        CoordBean bean = (CoordBean) coordTable.getSelectedBean();
        if (bean == null) return;
        showCoordInv(bean);
      }
    });

    varPopup = new thredds.ui.PopupMenu(gridTable.getJTable(), "Options");
    varPopup.addAction("Show Inv Coords", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        GridBean bean = (GridBean) gridTable.getSelectedBean();
        if (bean == null) return;
        showGridInv(bean);
      }
    });

    // the info window
    infoTA = new TextHistoryPane(false, 5000, 50, true, false, 14);
    infoWindow = new IndependentWindow("Extra Information", BAMutil.getImage("netcdfUI"), infoTA);
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

  public void setFmrc(String collectionSpec) throws IOException {
    // this.collectionSpec = collectionSpec;
    //if (!showCollectionInfo(false))
    //  return;

    errlog = new Formatter();
    fmrc = Fmrc.open(collectionSpec, errlog);
    if (fmrc == null) {
      infoTA.setText(errlog.toString());
      infoTA.gotoTop();
      infoWindow.show();
      return;
    }

    if (!showCollectionInfo(false))
      return;

    debug = new Formatter();
    fmrcInv = fmrc.getFmrcInv( debug);
    lite = new FmrcInvLite(fmrcInv);

    java.util.List<FmrBean> beanList = new ArrayList<FmrBean>();
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
      infoTA.appendLine("\ndebug=");
      infoTA.appendLine(debug.toString());
      infoTA.gotoTop();
      infoWindow.show();
      return false;
    }

    infoTA.clear();

    CollectionSpecParser sp = null;
    CollectionManager cm = fmrc.getManager();
    if (cm instanceof DatasetCollectionManager) {
      sp = ((DatasetCollectionManager) cm).getCollectionSpecParser();
    }
    if (sp != null) {
      infoTA.appendLine("CollectionSpecParser= "+sp);
      File dir = new File(sp.getTopDir());
      infoTA.appendLine(" topdir exists = = "+dir.exists());
    }

    infoTA.appendLine("CollectionManager= ");
    infoTA.appendLine(cm.toString());

    try {
      cm.scan(null);
    } catch (IOException e1) {
      ByteArrayOutputStream bos = new ByteArrayOutputStream(5000);
      e1.printStackTrace(new PrintStream(bos));
      infoTA.appendLine(bos.toString());
      infoTA.gotoTop();
      infoWindow.show();
      return false;
    }

    boolean status = false;
    List<MFile> files = cm.getFiles();
    if (files.size() == 0) {
      infoTA.appendLine("No Files found\nlog=");
      infoTA.appendLine(errlog.toString());
      infoTA.appendLine(cm.toString());
      alwaysShow = true;
    } else {
      infoTA.appendLine("Files found=");
      for (MFile mfile : files) {
        infoTA.appendLine(" "+mfile.getPath()+" "+ new Date(mfile.getLastModified())+" "+ mfile.getLength());
      }
      status = true;
    }

    if (alwaysShow)
      infoWindow.showIfNotIconified();
    return status;
  }

  public void showInfo(Formatter result) throws IOException {
    if (debug != null)
      result.format("%s%n", debug.toString());
    if (fmrc != null) {
      fmrcInv.showRuntimeOffsetMatrix(result);
      fmrcInv.showBest(result);
      fmrcInv.showBest2(result);
      fmrc.showDetails(result);
    }
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
          //System.out.printf("name=%s %s%n", evt.getPropertyName(), data);
          showDataset(data);
        }
      });
    }
    dialog.setFmrc(fmrc);
    dialog.setVisible(true);
  }

  private void showDataset(Fmrc2Dialog.Data data) {
    GridDataset gds = null;
    try {
      if (data.type.equals("Dataset2D"))
        gds = fmrc.getDataset2D( null);

      else if (data.type.equals("Best"))
        gds = fmrc.getDatasetBest();

      else if (data.type.equals("Run")) {
        DateFormatter df = new DateFormatter();
        gds = fmrc.getRunTimeDataset(df.getISODate((String)data.param));

      } else if (data.type.equals("ConstantForecast")) {
        DateFormatter df = new DateFormatter();
        gds = fmrc.getConstantForecastDataset(df.getISODate((String)data.param));

      } else if (data.type.equals("ConstantOffset")) {
        gds = fmrc.getConstantOffsetDataset( (Double) data.param);
      }

    } catch (IOException e) {
      e.printStackTrace();
    }
    
    if (data.where.startsWith("NetcdfFile"))
      firePropertyChange("openNetcdfFile", null, gds.getNetcdfFile());
    else if (data.where.startsWith("CoordSys"))
      firePropertyChange("openCoordSys", null, gds.getNetcdfFile());
    else if (data.where.startsWith("Grid"))
      firePropertyChange("openGridDataset", null, gds);
    else
      showDetails(gds);
  }

  public void showDetails(GridDataset gds) {
    infoTA.setText(gds.getDetailInfo());
    infoWindow.showIfNotIconified();
  }



  private void setFmr(FmrInv fmr) {
    if (fmr == null) return;
    java.util.List<InvBean> beanList = new ArrayList<InvBean>();
    for (GridDatasetInv fmrInv : fmr.getInventoryList()) {
      beanList.add(new InvBean(fmrInv));
    }

    invTable.setBeans(beanList);
  }

  private void setCoords(FmrcInv fmrInv) {
    if (fmrInv == null) return;
    java.util.List<CoordBean> beanList = new ArrayList<CoordBean>();
    for (FmrcInv.RunSeq tc : fmrInv.getRunSeqs())
      beanList.add(new TimeCoordBean(tc));
    for (VertCoord vc : fmrInv.getVertCoords())
      beanList.add(new VertCoordBean(vc));

    coordTable.setBeans(beanList);
  }

  private void setGrids(FmrcInv fmrInv) {
    if (fmrInv == null) return;
    java.util.List<GridBean> beanList = new ArrayList<GridBean>();
    for (FmrcInv.UberGrid grid : fmrInv.getUberGrids()) {
      beanList.add(new GridBean(grid));
    }

    gridTable.setBeans(beanList);
  }

  /* private void setGrids(GridDatasetInv fmrInv) {
    if (fmrInv == null) return;
    java.util.List<GridBean> beanList = new ArrayList<GridBean>();
    for (TimeCoord tc : fmrInv.getTimeCoords()) {
      for (GridDatasetInv.Grid grid : tc.getGrids())
        beanList.add(new GridBean(grid));
    }

   gridTable.setBeans(beanList);
  } */

  private void setSelectedCoord(GridBean gridBean) {
    java.util.List<CoordBean> beans = coordTable.getBeans();
    java.util.List<CoordBean> selected = new ArrayList<CoordBean>();
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
    infoWindow.showIfNotIconified();
  }

  private void showGridInv(GridBean bean) {
    Formatter out = new Formatter();
    FmrcInv.UberGrid ugrid = ((GridBean) bean).grid;
    out.format("Show Grid variable %s%n%3d expected inventory %n%n", ugrid.getName(), ugrid.countExpected());
    int count = 0;

    out.format("                             Forecast Time Offset %n");
    out.format(" RunTime             Total ");
    double[] offsets = ugrid.getUnionOffsetHours();
    for (double wantOffset : offsets) {
      out.format("%6.0f ", wantOffset);
    }
    out.format("%n");

    for (FmrInv.GridVariable run : ugrid.getRuns()) {
      for (GridDatasetInv.Grid inv : run.getInventory()) {
        out.format(" %s ", df.toDateTimeString(run.getRunDate()));
        count += showCount2(inv, offsets, out);
        out.format(" %s%n", inv.getLocation());
      }
    }
    out.format("%n%3d counted inventory%n%n%n", count);

    out.format("                             Forecast Time Offset %n");
    out.format(" RunTime               ");
    for (double wantOffset : ugrid.getUnionOffsetHours())
      out.format("%9.0f ", wantOffset);
    out.format("%n");
    
    count = 0;
    for (FmrInv.GridVariable run : ugrid.getRuns()) {
      out.format(" %s ", df.toDateTimeString(run.getRunDate()));
      for (double wantOffset : offsets) {
        FmrcInv.Inventory inv = getInventory(run, wantOffset);
        count += inv.showInventory(out);
      }
      out.format("%n");
    }
    out.format("%n%3d counted inventory%n", count);

    fmrcInv.showBest(ugrid, out);

    lite.showGridInfo(ugrid.getName(), out);

    infoTA.setText(out.toString());
    infoWindow.showIfNotIconified();
  }

  // for a given offset hour and GridVariable, find the expected and actual inventory
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
  }

  private int showCount2(GridDatasetInv.Grid inv, double[] offsets, Formatter out) {
    int count = 0;
    TimeCoord tc = inv.getTimeCoord();
    int nverts = inv.getVertCoordLength();
    out.format("%3d ", inv.countTotal());
    for (double wantOffset : offsets) {
      boolean hasInventory = tc.findIndex(wantOffset) >= 0;
      if (hasInventory)
        out.format("%6d ", nverts);
      else
        out.format("       "); // blank

      int ninv = hasInventory ? nverts : 0;
      count += ninv;
    }
    return count;
  }

  public class FmrBean {
    FmrInv fmr;

    // no-arg constructor
    public FmrBean() {
    }

    // create from a dataset
    public FmrBean(FmrInv fmr) {
      this.fmr = fmr;
    }

    public Date getRunDate() throws IOException {
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

  abstract public class CoordBean {
    // no-arg constructor
    public CoordBean() {
    }

    abstract public String getType();

    abstract public String getName();

    abstract public String getCoords();
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
      StringBuilder sb = new StringBuilder();
      for (double off : runSeq.getUnionOffsetHours())
        sb.append(off).append(" ");
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
