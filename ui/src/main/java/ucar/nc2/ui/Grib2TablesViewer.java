/*
 * Copyright (c) 1998 - 2011. University Corporation for Atmospheric Research/Unidata
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

import ucar.nc2.grib.grib2.table.Grib2Customizer;
import ucar.nc2.grib.grib2.table.WmoCodeTable;
import ucar.nc2.ui.dialog.Grib1TableDialog;
import ucar.nc2.ui.widget.BAMutil;
import ucar.nc2.ui.widget.IndependentWindow;
import ucar.nc2.ui.widget.PopupMenu;
import ucar.nc2.ui.widget.TextHistoryPane;
import ucar.nc2.units.SimpleUnit;
import ucar.util.prefs.PreferencesExt;
import ucar.util.prefs.ui.BeanTableSorted;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;
import java.util.List;

/**
 * Show Grib2 Tables
 *
 * @author caron
 * @since 6/17/11
 */
public class Grib2TablesViewer extends JPanel {

  private PreferencesExt prefs;

  private BeanTableSorted gribTable, entryTable;
  private JSplitPane split, split2;

  private TextHistoryPane infoTA;
  private IndependentWindow infoWindow;

  private Grib1TableDialog dialog;
  private Grib2Customizer.GribTableId current;

  public Grib2TablesViewer(final PreferencesExt prefs, JPanel buttPanel) {
    this.prefs = prefs;

    gribTable = new BeanTableSorted(TableBean.class, (PreferencesExt) prefs.node("CodeTableBean"), false);
    gribTable.addListSelectionListener(new ListSelectionListener() {
      public void valueChanged(ListSelectionEvent e) {
        TableBean csb = (TableBean) gribTable.getSelectedBean();
        setEntries(csb.table);
      }
    });

    ucar.nc2.ui.widget.PopupMenu varPopup = new PopupMenu(gribTable.getJTable(), "Options");
    varPopup.addAction("Compare two tables", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        List list = gribTable.getSelectedBeans();
        if (list.size() == 2) {
          TableBean bean1 = (TableBean) list.get(0);
          TableBean bean2 = (TableBean) list.get(1);
          compareTables(bean1.table, bean2.table);
        }
      }
    });

    entryTable = new BeanTableSorted(EntryBean.class, (PreferencesExt) prefs.node("EntryBean"), false);
    entryTable.addListSelectionListener(new ListSelectionListener() {
      public void valueChanged(ListSelectionEvent e) {
        EntryBean csb = (EntryBean) entryTable.getSelectedBean();
      }
    });

    AbstractButton dupButton = BAMutil.makeButtcon("Select", "Look for problems in this table", false);
    dupButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        lookForProblems();
      }
    });
    buttPanel.add(dupButton);

    // the info window
    infoTA = new TextHistoryPane();
    infoWindow = new IndependentWindow("Extra Information", BAMutil.getImage("netcdfUI"), infoTA);
    infoWindow.setBounds((Rectangle) prefs.getBean("InfoWindowBounds", new Rectangle(300, 300, 800, 600)));

    split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, false, gribTable, entryTable);
    split.setDividerLocation(prefs.getInt("splitPos", 500));

    setLayout(new BorderLayout());
    add(split, BorderLayout.CENTER);

    /* AbstractButton infoButton = BAMutil.makeButtcon("Information", "Show Table Used", false);
    infoButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        if (dialog == null) {
          dialog = new Grib1TableDialog((Frame) null);
          dialog.pack();
        }
        dialog.setVisible(true);
      }
    });
    buttPanel.add(infoButton); */

    try {
      java.util.List<Grib2Customizer.GribTableId> tables = Grib2Customizer.getTableIds();
      java.util.List<TableBean> beans = new ArrayList<TableBean>(tables.size());
      for (Grib2Customizer.GribTableId t : tables) {
        beans.add(new TableBean(t));
      }
      Collections.sort(beans);
      gribTable.setBeans(beans);

    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public void save() {
    gribTable.saveState(false);
    entryTable.saveState(false);
    prefs.putBeanObject("InfoWindowBounds", infoWindow.getBounds());
    //prefs.putBeanObject("InfoWindowBounds2", infoWindow2.getBounds());
    prefs.putInt("splitPos", split.getDividerLocation());
    //prefs.putInt("splitPos2", split2.getDividerLocation());
    // if (fileChooser != null) fileChooser.save();
  }

  private void setEntries(Grib2Customizer.GribTableId tableId) {
    Grib2Customizer gt = Grib2Customizer.factory(tableId.center, tableId.subCenter, tableId.masterVersion, tableId.localVersion);
    List params = gt.getParameters();
    java.util.List<EntryBean> beans = new ArrayList<EntryBean>(params.size());
    for (Object p : params) {
      beans.add(new EntryBean((Grib2Customizer.Parameter) p));
    }
    entryTable.setBeans(beans);
    current = tableId;
  }

  private void lookForProblems() {
    int total = 0;
    int probs = 0;

    Formatter f = new Formatter();
    Grib2Customizer cust = Grib2Customizer.factory(current.center, current.subCenter, current.masterVersion, current.localVersion);
    cust.lookForProblems(f);

    f.format("PROBLEMS with units%n");
    for (Object t : entryTable.getBeans()) {
      Grib2Customizer.Parameter p = ((EntryBean) t).param;
      if (p.getUnit() == null) continue;
      if (p.getUnit().length() == 0) continue;
      try {
        SimpleUnit su = SimpleUnit.factoryWithExceptions(p.getUnit());
        if (su.isUnknownUnit()) {
          f.format("%s '%s' has UNKNOWN udunit%n", p.getId(), p.getUnit());
          probs++;
        }
      } catch (Exception ioe) {
        f.format("%s '%s' FAILS on udunit parse%n", p.getId(), p.getUnit());
        probs++;
      }
      total++;
    }
    f.format("%nUNITS: Total=%d problems=%d%n%n", total, probs);

    int extra = 0;
    int nameDiffers = 0;
    int caseDiffers = 0;
    int unitsDiffer = 0;
    f.format("Conflicts with WMO%n");
    for (Object t : entryTable.getBeans()) {
      Grib2Customizer.Parameter p = ((EntryBean) t).param;
      if (Grib2Customizer.isLocal(p)) continue;
      if (p.getNumber() < 0) continue;
      WmoCodeTable.TableEntry wmo = WmoCodeTable.getParameterEntry(p.getDiscipline(), p.getCategory(), p.getNumber());
      if (wmo == null) {
        extra++;
        f.format(" NEW %s%n", p);
        WmoCodeTable.TableEntry wmo3 = WmoCodeTable.getParameterEntry(p.getDiscipline(), p.getCategory(), p.getNumber());

      } else if (!p.getName().equals( wmo.getName()) || !p.getUnit().equals( wmo.getUnit())) {
        boolean nameDiffer = !p.getName().equals( wmo.getName());
        boolean caseDiffer = nameDiffer && p.getName().equalsIgnoreCase( wmo.getName());
        if (nameDiffer)  nameDiffers++;
        if (caseDiffer)  caseDiffers++;
        if (!p.getUnit().equals( wmo.getUnit())) unitsDiffer++;
        
        f.format("this=%10s %40s %15s%n", p.getId(), p.getName(), p.getUnit());
        f.format(" wmo=%10s %40s %15s%n%n", wmo.getId(), wmo.getName(), wmo.getUnit());
      }
    }
    f.format("%nWMO differences: nameDiffers=%d caseDiffers=%d, unitsDiffer=%d, extra=%d %n%n",
            nameDiffers, caseDiffers, unitsDiffer, extra);

    infoTA.setText(f.toString());
    infoWindow.show();
  }

  private void compareTables(Grib2Customizer.GribTableId id1, Grib2Customizer.GribTableId id2) {
    Grib2Customizer t1 = Grib2Customizer.factory(id1.center, id1.subCenter, id1.masterVersion, id1.localVersion);
    Grib2Customizer t2 = Grib2Customizer.factory(id2.center, id2.subCenter, id2.masterVersion, id2.localVersion);

    Formatter f = new Formatter();

    f.format("Table 1 = %s (%s)%n", id1.name, t1.getTablePath(0, 192,192)); // local
    f.format("Table 2 = %s (%s)%n", id2.name, t2.getTablePath(0, 192,192)); // local

    int extra = 0;
    int conflict = 0;
    f.format("Table 1 : %n");
    for (Object t : t1.getParameters()) {
      Grib2Customizer.Parameter p1 = (Grib2Customizer.Parameter) t;
      Grib2Customizer.Parameter  p2 = t2.getParameterRaw(p1.getDiscipline(), p1.getCategory(), p1.getNumber());
      if (p2 == null) {
        extra++;
        f.format(" Missing %s in table 2%n", p1);
      } else if (p1.getName() == null || p1.getUnit() == null) {
        f.format(" Missing name or unit in table 1 param=%s%n", p1);
      } else if (!p1.getName().equals( p2.getName()) || !p1.getUnit().equals( p2.getUnit()) ||
                (p1.getAbbrev() != null && !p1.getAbbrev().equals( p2.getAbbrev()))) {
        f.format("  t1=%10s %40s %15s  %15s %s%n", p1.getId(), p1.getName(), p1.getUnit(), p1.getAbbrev(), p1.getDescription());
        f.format("  t2=%10s %40s %15s  %15s %s%n%n", p2.getId(), p2.getName(), p2.getUnit(), p2.getAbbrev(), p2.getDescription());
        conflict++;
      }
    }
    f.format("%nConflicts=%d extra=%d%n%n", conflict, extra);

    extra = 0;
    f.format("Table 2 : %n");
    for (Object t : t2.getParameters()) {
      Grib2Customizer.Parameter p2 = (Grib2Customizer.Parameter) t;
      Grib2Customizer.Parameter  p1 = t2.getParameterRaw(p2.getDiscipline(), p2.getCategory(), p2.getNumber());
      if (p1 == null) {
        extra++;
        f.format(" Missing %s in table 1%n", p2);
      }
    }
    f.format("%nextra=%d%n%n", extra);

    infoTA.setText(f.toString());
    infoWindow.show();
  }

  public class TableBean implements Comparable<TableBean> {
    Grib2Customizer.GribTableId table;
    String filename;

    public TableBean() {
    }

    public TableBean(Grib2Customizer.GribTableId table) {
      this.table = table;
      Grib2Customizer t1 = Grib2Customizer.factory(table.center, table.subCenter, table.masterVersion, table.localVersion);
      filename = t1.getTablePath(0, 192, 192); // local
    }

    public String getName() {
      return table.name;
    }

    public int getCenter_id() {
      return table.center;
    }

    public int getSubcenter_id() {
      return table.subCenter;
    }

    public int getVersionNumber() {
      return table.masterVersion;
    }

    public int getLocalVersion() {
      return table.localVersion;
    }

    public String getFilename() {
      return filename;
    }

    @Override
    public int compareTo(TableBean o) {
      int ret = getCenter_id() - o.getCenter_id();
      if (ret == 0)
        ret = getSubcenter_id() - o.getSubcenter_id();
      if (ret == 0)
        ret = getVersionNumber() - o.getVersionNumber();
      return ret;
    }
  }

  public class EntryBean {
    Grib2Customizer.Parameter param;

    // no-arg constructor
    public EntryBean() {
    }

    public EntryBean(Grib2Customizer.Parameter param) {
      this.param = param;
    }

    public String getName() {
      return param.getName();
    }

    public String getId() {
      return param.getDiscipline() + "-" + param.getCategory() + "-" + param.getNumber();
    }

    public int getKey() {
      return Grib2Customizer.makeHash(param.getDiscipline(),param.getCategory(),param.getNumber());
    }

    public String getUnit() {
      return param.getUnit();
    }

    public String getAbbrev() {
      return param.getAbbrev();
    }

    public String getDesc() {
      return param.getDescription();
    }

  }
}


