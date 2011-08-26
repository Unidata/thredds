package ucar.nc2.ui;

import ucar.grib.GribResourceReader;
import ucar.grib.NotSupportedException;
import ucar.grib.grib1.GribPDSParamTable;
import ucar.nc2.grib.table.GribTables;
import ucar.nc2.ui.dialog.BufrBCompare;
import ucar.nc2.ui.dialog.Grib1TableCompareDialog;
import ucar.nc2.ui.widget.*;
import ucar.nc2.ui.widget.IndependentWindow;
import ucar.nc2.iosp.grid.*;
import ucar.nc2.ui.dialog.Grib1TableDialog;
import ucar.nc2.ui.widget.PopupMenu;
import ucar.nc2.ui.widget.TextHistoryPane;
import ucar.nc2.units.SimpleUnit;
import ucar.nc2.util.IO;
import ucar.nc2.wmo.CommonCodeTable;
import ucar.util.prefs.PreferencesExt;
import ucar.util.prefs.ui.BeanTableSorted;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.List;

/**
 * Show Grib1 Tables
 *
 * @author caron
 * @since Sep 26, 2010
 */
public class Grib1TablesViewer extends JPanel {

  private PreferencesExt prefs;

  private BeanTableSorted codeTable, entryTable;
  private JSplitPane split, split2;

  private TextHistoryPane infoTA;
  private IndependentWindow infoWindow;

  private Grib1TableDialog showTableDialog;
  private Grib1TableCompareDialog compareTableDialog;

  public Grib1TablesViewer(final PreferencesExt prefs, JPanel buttPanel) {
    this.prefs = prefs;

    codeTable = new BeanTableSorted(TableBean.class, (PreferencesExt) prefs.node("CodeTableBean"), false);
    codeTable.addListSelectionListener(new ListSelectionListener() {
      public void valueChanged(ListSelectionEvent e) {
        TableBean csb = (TableBean) codeTable.getSelectedBean();
        setEntries(csb.table);
      }
    });

    ucar.nc2.ui.widget.PopupMenu varPopup = new PopupMenu(codeTable.getJTable(), "Options");
    varPopup.addAction("Show File contents", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        TableBean bean = (TableBean) codeTable.getSelectedBean();
        if (bean == null) return;
        showFile(bean);
      }
    });

    varPopup.addAction("Compare to default WMO table", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        TableBean bean = (TableBean) codeTable.getSelectedBean();
        if (bean == null) return;

        GribPDSParamTable wmo = null;
        try {
          wmo = GribPDSParamTable.getParameterTable(0, 0, bean.getVersion());
        } catch (NotSupportedException e1) {
          infoTA.setText(e1.toString());
          infoWindow.showIfNotIconified();
          return;
        }
        if (wmo == null) {
          infoTA.setText("Cant find WMO version " + bean.getVersion());
          infoWindow.showIfNotIconified();
          return;
        }

         if (compareTableDialog == null) {
          compareTableDialog = new Grib1TableCompareDialog( (Frame) null);
          compareTableDialog.pack();
          compareTableDialog.addPropertyChangeListener("OK", new PropertyChangeListener() {
             public void propertyChange(PropertyChangeEvent evt) {
               compareTables((Grib1TableCompareDialog.Data) evt.getNewValue());
             }
           });
        }

        compareTableDialog.setTable1(new TableBean(wmo));
        compareTableDialog.setTable2(bean);
        compareTableDialog.setVisible(true);
      }
    });

    varPopup.addAction("Compare two tables", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        TableBean bean = (TableBean) codeTable.getSelectedBean();
        if (bean == null) return;

         if (compareTableDialog == null) {
          compareTableDialog = new Grib1TableCompareDialog( (Frame) null);
          compareTableDialog.pack();
          compareTableDialog.addPropertyChangeListener("OK", new PropertyChangeListener() {
             public void propertyChange(PropertyChangeEvent evt) {
               compareTables((Grib1TableCompareDialog.Data) evt.getNewValue());
             }
           });
        }

        List list = codeTable.getSelectedBeans();
        if (list.size() == 2) {
          TableBean bean1 = (TableBean) list.get(0);
          TableBean bean2 = (TableBean) list.get(1);
          compareTableDialog.setTable1(bean1);
          compareTableDialog.setTable2(bean2);
          compareTableDialog.setVisible(true);
        }
      }
    });

    entryTable = new BeanTableSorted(EntryBean.class, (PreferencesExt) prefs.node("EntryBean"), false);
    entryTable.addListSelectionListener(new ListSelectionListener() {
      public void valueChanged(ListSelectionEvent e) {
        EntryBean csb = (EntryBean) entryTable.getSelectedBean();
      }
    });

    // the info window
    infoTA = new TextHistoryPane();
    infoWindow = new IndependentWindow("Extra Information", BAMutil.getImage("netcdfUI"), infoTA);
    infoWindow.setBounds((Rectangle) prefs.getBean("InfoWindowBounds", new Rectangle(300, 300, 800, 600)));

    split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, false, codeTable, entryTable);
    split.setDividerLocation(prefs.getInt("splitPos", 500));

    setLayout(new BorderLayout());
    add(split, BorderLayout.CENTER);

    AbstractButton infoButton = BAMutil.makeButtcon("Information", "Show Table Used", false);
    infoButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        if (showTableDialog == null) {
          showTableDialog = new Grib1TableDialog( (Frame) null);
          showTableDialog.pack();
        }
        showTableDialog.setVisible(true);
      }
    });
    buttPanel.add(infoButton);

    try {
      List<GribPDSParamTable> tables = GribPDSParamTable.getParameterTables();
      java.util.List<TableBean> beans = new ArrayList<TableBean>(tables.size());
      for (GribPDSParamTable t : tables) {
        beans.add(new TableBean(t));
      }
      //Collections.sort(beans);
      codeTable.setBeans(beans);

    } catch (Exception e) {
      e.printStackTrace();
    }

  }

  private void showFile(TableBean bean) {
    infoTA.setText("Table:" + bean.getPath() + "\n");
    InputStream is = GribResourceReader.getInputStream(bean.getPath());
    try {
      infoTA.appendLine( IO.readContents(is));
      infoWindow.setVisible(true);
      
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

/*
          1         2         3         4         5         6         7         8         9         10        11        12
0123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890
  1 |                                  Pressure |                   Pa  |                 PRES |
  2  5
*/
  public void setFilename(String filename) throws IOException {
    GribPDSParamTable table = new GribPDSParamTable(filename);
    TableBean bean = new TableBean(table);
    codeTable.addBean( bean);
    codeTable.setSelectedBean(bean);
  }


  private void compareTables(Grib1TableCompareDialog.Data data) {
    Formatter f = new Formatter();
    compare(data.table1bean.table, data.table2bean.table, data, f);
    infoTA.setText(f.toString());
    infoTA.gotoTop();
    infoWindow.showIfNotIconified();
  }

  private void compare(GribPDSParamTable t1, GribPDSParamTable t2, Grib1TableCompareDialog.Data data, Formatter out) {
    out.format("Compare%n %s%n %s%n", t1.toString(), t2.toString());
    Map<Integer, GridParameter> h1 = t1.getParameters();
    Map<Integer, GridParameter> h2 = t2.getParameters();
    List<Integer> keys = new ArrayList<Integer>(h1.keySet());
    Collections.sort(keys);

    for (Integer key : keys) {
      GridParameter d1 = h1.get(key);
      GridParameter d2 = h2.get(key);
      if (d2 == null) {
        if (data.showMissing) out.format("**No key %s (%s) in second table%n", key, d1);
      } else {
        if (data.compareDesc) {
          if (!equiv(d1.getDescription(), d2.getDescription()))
            out.format(" %s desc%n   %s%n   %s%n", d1.getNumber(), d1.getDescription(), d2.getDescription());
        }
        if (data.compareNames) {
          if (!equiv(d1.getName(), d2.getName()))
            out.format(" %d name%n   %s%n   %s%n", d1.getNumber(), d1.getName(), d2.getName());
        }
        if (data.compareUnits) {
          if (!equiv(d1.getUnit(), d2.getUnit()))
            out.format(" %s units%n   %s%n   %s%n", d1.getNumber(), d1.getUnit(), d2.getUnit());
        }
        if (data.cleanUnits) {
          String cu1 =  GribTables.cleanupUnits(d1.getUnit());
          String cu2 =  GribTables.cleanupUnits(d2.getUnit());
          if (!equiv(cu1, cu2)) out.format(" %s cleanUnits%n   %s%n   %s%n", d1.getNumber(), cu1, cu2);
        }
        if (data.udunits) {
          String cu1 =  GribTables.cleanupUnits(d1.getUnit());
          String cu2 =  GribTables.cleanupUnits(d2.getUnit());
            try {
              SimpleUnit su1 = SimpleUnit.factoryWithExceptions(cu1);
              if (!su1.isCompatible(cu2))
                out.format(" %s udunits%n   %s%n   %s%n", d1.getNumber(), cu1, cu2);
            } catch (Exception e) {
              out.format(" %s udunits%n   cant parse = %s%n   %s%n", d1.getNumber(), cu1, cu2);
            }
        }
      }
    }

    if (data.showMissing) {
      out.format("%n***Check if entries are missing in first table%n");
      keys = new ArrayList<Integer>(h2.keySet());
      Collections.sort(keys);
      for (Integer key : keys) {
        GridParameter d1 = h1.get(key);
        GridParameter d2 = h2.get(key);
        if (d1 == null)
          out.format("**No key %s (%s) in first table%n", key, d2);
      }
    }

  }

  private boolean equiv(String org1, String org2) {
    if (org1 == org2) return true;
    if (org1 == null) return false;
    if (org2 == null) return false;
    return org1.equalsIgnoreCase(org2);
  }

  public void save() {
    codeTable.saveState(false);
    entryTable.saveState(false);
    prefs.putBeanObject("InfoWindowBounds", infoWindow.getBounds());
    //prefs.putBeanObject("InfoWindowBounds2", infoWindow2.getBounds());
    prefs.putInt("splitPos", split.getDividerLocation());
    //prefs.putInt("splitPos2", split2.getDividerLocation());
    // if (fileChooser != null) fileChooser.save();
  }

  public void setEntries(GribPDSParamTable table) {
    Map<Integer, GridParameter> map = table.getParameters();
    ArrayList<Integer> params = new ArrayList<Integer>();
    params.addAll(map.keySet());
    Collections.sort(params);
    java.util.List<EntryBean> beans = new ArrayList<EntryBean>(params.size());
    for (Integer key : params) {
      beans.add(new EntryBean(key, map.get(key)));
    }
    entryTable.setBeans(beans);
  }

  public class TableBean implements Comparable<TableBean> {
    GribPDSParamTable table;

    // no-arg constructor

    public TableBean() {
    }

    // create from a dataset

    public TableBean(GribPDSParamTable table) {
      this.table = table;
    }

    public int getCenter_id() {
      return table.getCenter_id();
    }

    public String getCenter() {
      return CommonCodeTable.getCenterName(table.getCenter_id(), 1);
    }

    public String getSubCenter() {
      return CommonCodeTable.getSubCenterName(table.getCenter_id(), table.getSubcenter_id());
    }

    public int getSubcenter_id() {
      return table.getSubcenter_id();
    }

    public int getVersion() {
      return table.getVersion();
    }

    public String getPath() {
      return table.getPath();
    }

    @Override
    public int compareTo(TableBean o) {
      int ret = getCenter_id() - o.getCenter_id();
      if (ret == 0)
        ret = getSubcenter_id() - o.getSubcenter_id();
      if (ret == 0)
        ret = getVersion() - o.getVersion();
      return  ret;
    }
  }

  public class EntryBean {
    GridParameter param;
    Integer key;

    // no-arg constructor
    public EntryBean() {
    }

    public EntryBean(Integer key, GridParameter param) {
      this.key = key;
      this.param = param;
    }

    public int getKey() {
      return key;
    }

    public String getName() {
      return param.getName();
    }

    public String getDescription() {
      return param.getDescription();
    }

    public String getUnit() {
      return param.getUnit();
    }

    public String getCleanUnit() {
      return GribTables.cleanupUnits(param.getUnit());
    }

    public String getUdunit() {
      String cu =  GribTables.cleanupUnits(param.getUnit());
         try {
           SimpleUnit su1 = SimpleUnit.factoryWithExceptions(cu);
           return su1.toString();
         } catch (Exception e) {
           return "FAIL "+e.getMessage();
         }
    }

    public int getNumber() {
      return param.getNumber();
    }

  }
}

