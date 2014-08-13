/*
 * Copyright 1998-2014 University Corporation for Atmospheric Research/Unidata
 *
 *   Portions of this software were developed by the Unidata Program at the
 *   University Corporation for Atmospheric Research.
 *
 *   Access and use of this software shall impose the following obligations
 *   and understandings on the user. The user is granted the right, without
 *   any fee or cost, to use, copy, modify, alter, enhance and distribute
 *   this software, and any derivative works thereof, and its supporting
 *   documentation for any purpose whatsoever, provided that this entire
 *   notice appears in all copies of the software, derivative works and
 *   supporting documentation.  Further, UCAR requests that the user credit
 *   UCAR/Unidata in any publications that result from the use of this
 *   software or in any product that includes this software. The names UCAR
 *   and/or Unidata, however, may not be used in any advertising or publicity
 *   to endorse or promote any products or commercial entity unless specific
 *   written permission is obtained from UCAR/Unidata. The user also
 *   understands that UCAR/Unidata is not obligated to provide the user with
 *   any support, consulting, training or assistance of any kind with regard
 *   to the use, operation and performance of this software nor to provide
 *   the user with any updates, revisions, new versions or "bug fixes."
 *
 *   THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 *   IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *   WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *   DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 *   INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 *   FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 *   NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 *   WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */

package ucar.nc2.ui;

import ucar.nc2.ui.widget.*;
import ucar.nc2.wmo.CommonCodeTable;
import ucar.util.prefs.PreferencesExt;
import ucar.util.prefs.ui.BeanTable;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
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
    codeTable.addListSelectionListener(new ListSelectionListener() {
      public void valueChanged(ListSelectionEvent e) {
        TableBean csb = (TableBean) codeTable.getSelectedBean();
        CommonCodeTable cct = CommonCodeTable.getTable(csb.t.getTableNo());
        setEntries(cct);
      }
    });

    entryTable = new BeanTable(EntryBean.class, (PreferencesExt) prefs.node("EntryBean"), false);

    ucar.nc2.ui.widget.PopupMenu varPopup = new ucar.nc2.ui.widget.PopupMenu(codeTable.getJTable(), "Options");
    varPopup.addAction("Show", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        Formatter out = new Formatter();
        TableBean csb = (TableBean) codeTable.getSelectedBean();
        if (csb == null) return;
        CommonCodeTable cct = CommonCodeTable.getTable(csb.t.getTableNo());
        List<EntryBean> beans = setEntries(cct);
        for (EntryBean bean: beans) {
          bean.show(out);
        }
        compareTA.setText(out.toString());
        compareTA.gotoTop();
        infoWindow.setVisible(true);
      }
    });

    /* AbstractButton compareButton = BAMutil.makeButtcon("Select", "Compare to current table", false);
    compareButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        compareToCurrent();
      }
    });
    buttPanel.add(compareButton);

    AbstractButton dupButton = BAMutil.makeButtcon("Select", "Look for problems in WMO table", false);
    dupButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        lookForProblems();
      }
    });
    buttPanel.add(dupButton);

    AbstractButton modelsButton = BAMutil.makeButtcon("Select", "Check current models", false);
    modelsButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        try {
          checkCurrentModels();
        } catch (IOException e1) {
          e1.printStackTrace();
        }
      }
    });
    buttPanel.add(modelsButton);   */

    // the info window
    compareTA = new TextHistoryPane();
    infoWindow = new IndependentWindow("Extra Information", BAMutil.getImage("netcdfUI"), compareTA);
    infoWindow.setBounds((Rectangle) prefs.getBean("InfoWindowBounds", new Rectangle(300, 300, 800, 600)));

    split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, false, codeTable, entryTable);
    split.setDividerLocation(prefs.getInt("splitPos", 500));

    setLayout(new BorderLayout());
    add(split, BorderLayout.CENTER);

    ///

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
