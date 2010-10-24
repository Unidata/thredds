package ucar.nc2.ui;

import ucar.nc2.ui.widget.*;
import ucar.nc2.ui.widget.IndependentWindow;
import ucar.grib.GribResourceReader;
import ucar.grib.grib1.Grib1Tables;
import ucar.grib.grib1.GribPDSParamTable;
import ucar.grid.GridParameter;
import ucar.nc2.ui.dialog.Grib1TableDialog;
import ucar.nc2.ui.widget.PopupMenu;
import ucar.nc2.ui.widget.TextHistoryPane;
import ucar.nc2.util.IO;
import ucar.util.prefs.PreferencesExt;
import ucar.util.prefs.ui.BeanTableSorted;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/**
 * Describe
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

  private Grib1TableDialog dialog;

  public Grib1TablesViewer(final PreferencesExt prefs, JPanel buttPanel) {
    this.prefs = prefs;

    codeTable = new BeanTableSorted(TableBean.class, (PreferencesExt) prefs.node("CodeBean"), false);
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
        if (dialog == null) {
          dialog = new Grib1TableDialog( (Frame) null);
          dialog.pack();
        }
        dialog.setVisible(true);
      }
    });
    buttPanel.add(infoButton);

    ///

    try {
      GribPDSParamTable[] tables = GribPDSParamTable.getParameterTables();
      java.util.List<TableBean> beans = new ArrayList<TableBean>(tables.length);
      for (GribPDSParamTable t : tables) {
        beans.add(new TableBean(t));
      }
      Collections.sort(beans);
      codeTable.setBeans(beans);

    } catch (Exception e) {
      e.printStackTrace();
    }

  }

  private void showFile(TableBean bean) {
    InputStream is = GribResourceReader.getInputStream(bean.getPath());
    try {
      infoTA.setText( IO.readContents(is));
      infoWindow.setVisible(true);
      
    } catch (IOException e) {
      e.printStackTrace();
    }

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
    Map<String, GridParameter> map = table.getParameters();
    ArrayList<String> params = new ArrayList<String>();
    params.addAll(map.keySet());
    Collections.sort(params);
    java.util.List<EntryBean> beans = new ArrayList<EntryBean>(params.size());
    for (String key : params) {
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
      return Grib1Tables.getCenter_idName( table.getCenter_id());
    }

    public String getSubCenter() {
      return Grib1Tables.getSubCenter_idName(table.getCenter_id(), table.getSubcenter_id());
    }

    public int getSubcenter_id() {
      return table.getSubcenter_id();
    }

    public int getVersionNumber() {
      return table.getTable_number();
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
        ret = getVersionNumber() - o.getVersionNumber();
      return  ret;
    }
  }

  public class EntryBean {
    GridParameter param;
    String key;

    // no-arg constructor
    public EntryBean() {
    }

    public EntryBean(String key, GridParameter param) {
      this.key = key;
      this.param = param;
    }

    public String getKey() {
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

    public int getNumber() {
      return param.getNumber();
    }

  }
}

