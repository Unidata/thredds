/*
 * Copyright (c) 1998-2019 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.ui.op;

import ucar.ui.widget.*;
import ucar.ui.widget.PopupMenu;
import ucar.nc2.wmo.CommonCodeTable;
import ucar.util.prefs.PreferencesExt;
import ucar.ui.prefs.BeanTable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.*;
import java.util.List;

/**
 * WMO Common Codes
 *
 * @author caron
 * @since Aug 25, 2010
 */
public class WmoCommonCodesPanel extends JPanel {
  private PreferencesExt prefs;

  private BeanTable codeTable, entryTable;
  private JSplitPane split;

  private TextHistoryPane compareTA;
  private IndependentWindow infoWindow;

  public WmoCommonCodesPanel(final PreferencesExt prefs, JPanel buttPanel) {
    this.prefs = prefs;

    codeTable = new BeanTable(TableBean.class, (PreferencesExt) prefs.node("CodeTableBean"), false);
    codeTable.addListSelectionListener(e -> {
        TableBean csb = (TableBean) codeTable.getSelectedBean();
        CommonCodeTable cct = CommonCodeTable.getTable(csb.t.getTableNo());
        setEntries(cct);
    });

    entryTable = new BeanTable(EntryBean.class, (PreferencesExt) prefs.node("EntryBean"), false);

    PopupMenu varPopup = new PopupMenu(codeTable.getJTable(), "Options");
    varPopup.addAction("Show", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        Formatter out = new Formatter();
        TableBean csb = (TableBean) codeTable.getSelectedBean();
        if (csb == null) return;
        CommonCodeTable cct = CommonCodeTable.getTable(csb.t.getTableNo());
        out.format("%s ==== %s%n", csb.getName(), csb.getResource());
        List<EntryBean> beans = setEntries(cct);
        for (EntryBean bean: beans) {
          bean.show(out);
        }
        compareTA.setText(out.toString());
        compareTA.gotoTop();
        infoWindow.setVisible(true);
      }
    });

    // the info window
    compareTA = new TextHistoryPane();
    infoWindow = new IndependentWindow("Extra Information", BAMutil.getImage("nj22/NetcdfUI"), compareTA);
    infoWindow.setBounds((Rectangle) prefs.getBean("InfoWindowBounds", new Rectangle(300, 300, 800, 600)));

    split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, false, codeTable, entryTable);
    split.setDividerLocation(prefs.getInt("splitPos", 500));

    setLayout(new BorderLayout());
    add(split, BorderLayout.CENTER);

    try {
      List<TableBean> tables = new ArrayList<>();
      for (CommonCodeTable.Table t : CommonCodeTable.Table.values()) {
        tables.add(new TableBean(t));
      }
      codeTable.setBeans(tables);

    } catch (Exception e) {
      e.printStackTrace();
    }

  }

  public void save() {
    codeTable.saveState(false);
    entryTable.saveState(false);
    prefs.putBeanObject("InfoWindowBounds", infoWindow.getBounds());
    prefs.putInt("splitPos", split.getDividerLocation());
  }

  public List<EntryBean> setEntries(CommonCodeTable codeTable) {
    List<EntryBean> beans = new ArrayList<>(codeTable.entries.size());
    for (CommonCodeTable.TableEntry d : codeTable.entries) {
      beans.add(new EntryBean(d));
    }
    entryTable.setBeans(beans);
    return beans;
  }

  public class TableBean {
    CommonCodeTable.Table t;

    // no-arg constructor
    public TableBean() {
    }

    // create from a dataset
    public TableBean(CommonCodeTable.Table t) {
      this.t = t;
    }

    public String getName() {
      return t.getName();
    }

    public String getEnumName() {
      return t.name();
    }

    public int getType() {
      return t.getTableType();
    }

    public String getResource() {
      return t.getResourceName();
    }

  }

  public class EntryBean {
    CommonCodeTable.TableEntry te;

    // no-arg constructor
    public EntryBean() {
    }

    // create from a dataset
    public EntryBean(CommonCodeTable.TableEntry te) {
      this.te = te;
    }

    public String getValue() {
      return te.value;
    }

    public String getComment() {
      return te.comment;
    }

    public String getStatus() {
      return te.status;
    }

    public int getCode() {
      return te.code;
    }

    public int getCode2() {
      return te.code2;
    }

    public int getLine() {
      return te.line;
    }

    private void show(Formatter f) {
      if (getCode() > 0)
      f.format("%4d; %4d; %s%n", getCode(), getCode2(), getValue());
    }
  }
}
