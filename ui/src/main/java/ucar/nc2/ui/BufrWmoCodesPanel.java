/*
 * Copyright (c) 1998 - 2013. University Corporation for Atmospheric Research/Unidata
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

import ucar.nc2.iosp.bufr.tables.CodeFlagTables;
import ucar.nc2.ui.widget.BAMutil;
import ucar.nc2.ui.widget.IndependentWindow;
import ucar.nc2.ui.widget.TextHistoryPane;
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
    codeTable.addListSelectionListener(new ListSelectionListener() {
      public void valueChanged(ListSelectionEvent e) {
        CodeTableBean csb = (CodeTableBean) codeTable.getSelectedBean();
        setEntries(csb.code);
      }
    });

    ucar.nc2.ui.widget.PopupMenu varPopup = new ucar.nc2.ui.widget.PopupMenu(codeTable.getJTable(), "Options");
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
    /* entryTable.addListSelectionListener(new ListSelectionListener() {
      public void valueChanged(ListSelectionEvent e) {
        entryTable.getSelectedBean();
      }
    });  */

    Map<Short, CodeFlagTables> tables = CodeFlagTables.getTables();
    java.util.List<CodeTableBean> beans = new ArrayList<>(tables.size());
    List<Short> list = new ArrayList<>(tables.keySet());
    Collections.sort(list);
    for (short key : list) {
      beans.add(new CodeTableBean(tables.get( key)));
    }
    codeTable.setBeans(beans);


    // the info window
    compareTA = new TextHistoryPane();
    infoWindow = new IndependentWindow("Extra Information", BAMutil.getImage("netcdfUI"), compareTA);
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
    java.util.List<EntryBean> beans = new ArrayList<>(map.size());
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
