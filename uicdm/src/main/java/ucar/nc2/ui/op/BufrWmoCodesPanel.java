/*
 * Copyright (c) 1998-2019 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.ui.op;

import ucar.nc2.iosp.bufr.tables.CodeFlagTables;
import ucar.ui.widget.BAMutil;
import ucar.ui.widget.IndependentWindow;
import ucar.ui.widget.TextHistoryPane;
import ucar.util.prefs.PreferencesExt;
import ucar.ui.prefs.BeanTable;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Formatter;
import java.util.List;
import java.util.Map;
import javax.swing.*;

/**
 * BUFR code tables UI
 *
 * @author John
 * @since 8/12/11
 */
public class BufrWmoCodesPanel extends JPanel {
  private PreferencesExt prefs;

  private BeanTable codeTable, entryTable;
  private JSplitPane split;

  private TextHistoryPane compareTA;
  private IndependentWindow infoWindow;

  public BufrWmoCodesPanel(final PreferencesExt prefs, JPanel buttPanel) {
    this.prefs = prefs;

    codeTable = new BeanTable(CodeTableBean.class, (PreferencesExt) prefs.node("CodeTableBean"), false);
    codeTable.addListSelectionListener(e -> {
        CodeTableBean csb = (CodeTableBean) codeTable.getSelectedBean();
        setEntries(csb.code);
    });

    ucar.ui.widget.PopupMenu varPopup = new ucar.ui.widget.PopupMenu(codeTable.getJTable(), "Options");
    varPopup.addAction("Show table", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        Formatter out = new Formatter();
        CodeTableBean csb = (CodeTableBean) codeTable.getSelectedBean();
        csb.showTable(out);
        compareTA.setText(out.toString());
        compareTA.gotoTop();
        infoWindow.setVisible(true);
      }
    });

    entryTable = new BeanTable(EntryBean.class, (PreferencesExt) prefs.node("EntryBean"), false);
    /* entryTable.addListSelectionListener(e -> {
        entryTable.getSelectedBean();
    });  */

    Map<Short, CodeFlagTables> tables = CodeFlagTables.getTables();
    List<CodeTableBean> beans = new ArrayList<>(tables.size());
    List<Short> list = new ArrayList<>(tables.keySet());
    Collections.sort(list);
    for (short key : list) {
      beans.add(new CodeTableBean(tables.get( key)));
    }
    codeTable.setBeans(beans);


    // the info window
    compareTA = new TextHistoryPane();
    infoWindow = new IndependentWindow("Extra Information", BAMutil.getImage("nj22/NetcdfUI"), compareTA);
    infoWindow.setBounds((Rectangle) prefs.getBean("InfoWindowBounds", new Rectangle(300, 300, 800, 600)));

    split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, false, codeTable, entryTable);
    split.setDividerLocation(prefs.getInt("splitPos", 500));

    setLayout(new BorderLayout());
    add(split, BorderLayout.CENTER);

  }

  public void save() {
    codeTable.saveState(false);
    entryTable.saveState(false);
    prefs.putBeanObject("InfoWindowBounds", infoWindow.getBounds());
    //prefs.putBeanObject("InfoWindowBounds2", infoWindow2.getBounds());
    prefs.putInt("splitPos", split.getDividerLocation());
    //prefs.putInt("splitPos2", split2.getDividerLocation());
  }

  public void setEntries(CodeFlagTables codeTable) {
    Map<Integer, String> map = codeTable.getMap();
    List<EntryBean> beans = new ArrayList<>(map.size());
    List<Integer> list = new ArrayList<>(map.keySet());
    Collections.sort(list);
    for (int key : list) {
      beans.add(new EntryBean(key, map.get(key)));
    }
    entryTable.setBeans(beans);
  }

  public class CodeTableBean {
    CodeFlagTables code;

    // no-arg constructor
    public CodeTableBean() {
    }

    // create from a dataset
    public CodeTableBean(CodeFlagTables code) {
      this.code = code;
    }

    public String getName() {
      return code.getName();
    }

    public String getFxy() {
      return code.fxy();
    }

    public short getId() {
      return code.getId();
    }

    public int getSize() {
      return code.getMap().size();
    }

    void showTable(Formatter f) {
      f.format("Code Table %s (%s)%n", code.getName(), code.fxy());
      Map<Integer, String> map = code.getMap();
      for (int key : map.keySet()) {
        f.format("  %3d: %s%n", key, map.get(key));
      }
    }
  }

  public class EntryBean {
    int code;
    String value;

    public EntryBean(int code, String value) {
      this.code = code;
      this.value = value;
    }

    public String getValue() {
      return value;
    }

    public int getCode() {
      return code;
    }
  }
}
