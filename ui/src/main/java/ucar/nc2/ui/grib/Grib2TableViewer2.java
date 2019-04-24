/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.ui.grib;

import ucar.nc2.grib.GribTables;
import ucar.nc2.grib.grib2.table.Grib2Customizer;
import ucar.nc2.grib.grib2.table.Grib2Table;
import ucar.nc2.grib.grib2.table.WmoParamTable;
import ucar.nc2.ui.widget.BAMutil;
import ucar.nc2.ui.widget.IndependentWindow;
import ucar.nc2.ui.widget.PopupMenu;
import ucar.nc2.ui.widget.TextHistoryPane;
import ucar.nc2.units.SimpleUnit;
import ucar.util.prefs.PreferencesExt;
import ucar.util.prefs.ui.BeanTable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;

/**
 * Describe
 *
 * @author caron
 * @since 9/14/2014
 */
public class Grib2TableViewer2 extends JPanel {

  private PreferencesExt prefs;

  private BeanTable gribTable, entryTable;
  private JSplitPane split;

  private TextHistoryPane infoTA;
  private IndependentWindow infoWindow;

  private Grib2Table current;

  public Grib2TableViewer2(final PreferencesExt prefs, JPanel buttPanel) {
    this.prefs = prefs;

    gribTable = new BeanTable(TableBean.class, (PreferencesExt) prefs.node("CodeTableBean"), false);
    gribTable.addListSelectionListener(e -> {
        TableBean csb = (TableBean) gribTable.getSelectedBean();
        setEntries(csb.table);
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

    entryTable = new BeanTable(EntryBean.class, (PreferencesExt) prefs.node("EntryBean"), false);
    /* entryTable.addListSelectionListener(new ListSelectionListener() {
      public void valueChanged(ListSelectionEvent e) {
        entryTable.getSelectedBean();
      }
    }); */

    AbstractButton dupButton = BAMutil.makeButtcon("Select", "Look for problems in this table", false);
    dupButton.addActionListener(e -> {
        lookForProblems();
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
      java.util.List<Grib2Table> tables = Grib2Table.getTables();
      java.util.List<TableBean> beans = new ArrayList<>(tables.size());
      for (Grib2Table t : tables) {
        beans.add(new TableBean(t));
      }
      //Collections.sort(beans);
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

  private void setEntries(Grib2Table tableId) {
    Grib2Customizer gt = Grib2Customizer.factory(tableId);
    List<GribTables.Parameter> params = gt.getParameters();
    java.util.List<EntryBean> beans = new ArrayList<>(params.size());
    for (GribTables.Parameter p : params) {
      beans.add(new EntryBean( p));
    }
    entryTable.setBeans(beans);
    current = tableId;
  }

  private void lookForProblems() {
    int total = 0;
    int probs = 0;

    Formatter f = new Formatter();
    Grib2Customizer cust = Grib2Customizer.factory(current);
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

    int local = 0;
    int extra = 0;
    int nameDiffers = 0;
    int caseDiffers = 0;
    int unitsDiffer = 0;
    f.format("Conflicts with WMO%n");
    for (Object t : entryTable.getBeans()) {
      Grib2Customizer.Parameter p = ((EntryBean) t).param;
      if (Grib2Customizer.isLocal(p)) {
        local++;
        continue;
      }
      if (p.getNumber() < 0) continue;
      GribTables.Parameter wmo = WmoParamTable.getParameter(p.getDiscipline(), p.getCategory(), p.getNumber());

      if (wmo == null) {
        extra++;
        f.format(" NEW %s%n", p);
        // WmoCodeTable.TableEntry wmo3 = WmoCodeTable.getParameterEntry(p.getDiscipline(), p.getCategory(), p.getNumber());

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
    f.format("%nWMO differences: nameDiffers=%d caseDiffers=%d, unitsDiffer=%d, extra=%d local=%d%n%n",
            nameDiffers, caseDiffers, unitsDiffer, extra, local);

    infoTA.setText(f.toString());
    infoWindow.show();
  }

  private void compareTables(Grib2Table id1, Grib2Table id2) {
    Grib2Customizer t1 = Grib2Customizer.factory(id1);
    Grib2Customizer t2 = Grib2Customizer.factory(id2);

    Formatter f = new Formatter();

    f.format("Table 1 = %s (%s)%n", id1.name, t1.getTablePath(0, 192,192)); // local  // WTF ??
    f.format("Table 2 = %s (%s)%n", id2.name, t2.getTablePath(0, 192,192)); // local

    int conflict = 0;
    f.format("Table 1 : %n");
    for (Object t : t1.getParameters()) {
      Grib2Customizer.Parameter p1 = (Grib2Customizer.Parameter) t;
      Grib2Customizer.Parameter  p2 = t2.getParameterRaw(p1.getDiscipline(), p1.getCategory(), p1.getNumber());
      if (p1.getName() == null || p1.getUnit() == null) {
        f.format(" Missing name or unit in table 1 param=%s%n", p1);
      } else if (p2 != null && (!p1.getName().equals( p2.getName()) || !p1.getUnit().equals( p2.getUnit()) ||
                (p1.getAbbrev() != null && !p1.getAbbrev().equals( p2.getAbbrev())))) {
        f.format("  t1=%10s %40s %15s  %15s %s%n", p1.getId(), p1.getName(), p1.getUnit(), p1.getAbbrev(), p1.getDescription());
        f.format("  t2=%10s %40s %15s  %15s %s%n%n", p2.getId(), p2.getName(), p2.getUnit(), p2.getAbbrev(), p2.getDescription());
        conflict++;
      }
    }
    f.format("%nConflicts=%d%n%n", conflict);

    int extra = 0;
    for (Object t : t1.getParameters()) {
      Grib2Customizer.Parameter p1 = (Grib2Customizer.Parameter) t;
      Grib2Customizer.Parameter  p2 = t2.getParameterRaw(p1.getDiscipline(), p1.getCategory(), p1.getNumber());
      if (p2 == null) {
        extra++;
        f.format(" Missing %s in table 2%n", p1);
      }
    }
    f.format("%nextra=%d%n%n", extra);

    extra = 0;
    f.format("Table 2 has the following not in Table 1:%n");
    for (Object t : t2.getParameters()) {
      Grib2Customizer.Parameter p2 = (Grib2Customizer.Parameter) t;
      Grib2Customizer.Parameter  p1 = t1.getParameterRaw(p2.getDiscipline(), p2.getCategory(), p2.getNumber());
      if (p1 == null) {
        extra++;
        f.format(" %s%n", p2);
      }
    }
    f.format("%ntotal extra=%d%n%n", extra);


    infoTA.setText(f.toString());
    infoWindow.show();
  }

  public class TableBean implements Comparable<TableBean> {
    Grib2Table table;
    Grib2Table.Id id;

    public TableBean() {
    }

    public TableBean(Grib2Table table) {
      this.table = table;
      this.id = table.getId();
    }

    public String getName() {
      return table.name;
    }

    public int getCenter_id() {
      return id.center;
    }

    public int getSubcenter_id() {
      return id.subCenter;
    }

    public int getVersionNumber() {
      return id.masterVersion;
    }

    public int getLocalVersion() {
      return id.localVersion;
    }

    public int getGenProcessId() {
       return id.genProcessId;
     }

    public String getPath() {
     return table.getPath();
   }

    public String getType() {
     return table.type.toString();
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
    GribTables.Parameter param;

    // no-arg constructor
    public EntryBean() {
    }

    public EntryBean(GribTables.Parameter param) {
      this.param = param;
    }

    public String getName() {
      return param.getName();
    }

    public String getId() {
      return param.getId();
    }

    // This gives the correct sort order.
    public int getKey() {
      return Grib2Customizer.makeParamId(param.getDiscipline(), param.getCategory(), param.getNumber());
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


  /*
  //////////////////////////////////////////////////////////////////////////
  // LOOK - compare to Grib2TablesViewer, From NCepLocalParameters

  private static void compareTables(NcepLocalParams test, Grib2Customizer current) {
    Formatter f = new Formatter();
    //f.format("Table 1 = %s%n", test.tableName);
    //f.format("Table 2 = %s%n", "currentNcep");

    int extra = 0;
    int udunits = 0;
    int conflict = 0;
    // f.format("Table 1 : %n");
    for (Grib2Parameter p1 : test.getParameters()) {
      Grib2Customizer.Parameter  p2 = current.getParameter(p1.getDiscipline(), p1.getCategory(), p1.getNumber());
      if (p2 == null) {
        extra++;
        if (p1.getNumber() < 192) f.format("  WMO missing %s%n", p1);

      } else {
        String p1n = Util.cleanName(StringUtil2.substitute(p1.getName(), "-", " "));
        String p2n = Util.cleanName(StringUtil2.substitute(p2.getName(), "-", " "));

        if (!p1n.equalsIgnoreCase(p2n) ||
           (p1.getNumber() >= 192 && !p1.getAbbrev().equals(p2.getAbbrev()))) {
          f.format("  p1=%10s %40s %15s  %15s%n", p1.getId(), p1.getName(), p1.getUnit(), p1.getAbbrev());
          f.format("  p2=%10s %40s %15s  %15s%n%n", p2.getId(), p2.getName(), p2.getUnit(), p2.getAbbrev());
          conflict++;
        }

        if (!p1.getUnit().equalsIgnoreCase(p2.getUnit())) {
          String cu1 = Util.cleanUnit(p1.getUnit());
          String cu2 = Util.cleanUnit(p2.getUnit());

          // eliminate common non-udunits
          boolean isUnitless1 = isUnitless(cu1);
          boolean isUnitless2 = isUnitless(cu2);

          if (isUnitless1 != isUnitless2) {
            f.format("  ud=%10s %s != %s for %s (%s)%n%n", p1.getId(), cu1, cu2, p1.getId(), p1.getName());
            udunits++;

          } else if (!isUnitless1) {

            try {
              SimpleUnit su1 = SimpleUnit.factoryWithExceptions(cu1);
              if (!su1.isCompatible(cu2)) {
                f.format("  ud=%10s %s (%s) != %s for %s (%s)%n%n", p1.getId(), cu1, su1, cu2, p1.getId(), p1.getName());
                udunits++;
              }
            } catch (Exception e) {
              f.format("  udunits cant parse=%10s %15s %15s%n", p1.getId(), cu1, cu2);
            }
          }

        }
      }

    }
    f.format("Conflicts=%d extra=%d udunits=%d%n%n", conflict, extra, udunits);

    /* extra = 0;
    f.format("Table 2 : %n");
    for (Object t : current.getParameters()) {
      Grib2Tables.Parameter p2 = (Grib2Tables.Parameter) t;
      Grib2Parameter  p1 = test.getParameter(p2.getNumber());
      if (p1 == null) {
        extra++;
        f.format(" Missing %s in table 1%n", p2);
      }
    }
    f.format("%nextra=%d%n%n", extra);
    System.out.printf("%s%n", f);
  }  */

