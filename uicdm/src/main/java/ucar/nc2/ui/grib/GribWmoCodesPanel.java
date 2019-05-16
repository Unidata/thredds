/*
 * Copyright (c) 1998-2019 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.ui.grib;

import ucar.nc2.grib.grib2.table.WmoCodeFlagTables;
import ucar.nc2.grib.grib2.table.WmoCodeFlagTables.TableType;
import ucar.nc2.grib.grib2.table.WmoCodeFlagTables.WmoTable;
import ucar.ui.widget.*;
import ucar.ui.widget.PopupMenu;
import ucar.nc2.units.SimpleUnit;
import ucar.util.prefs.PreferencesExt;
import ucar.ui.prefs.BeanTable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.*;
import java.util.List;

/**
 * WMO Grib 2 Tables - Codes
 *
 * @author caron
 * @since Aug 25, 2010
 */
public class GribWmoCodesPanel extends JPanel {
  private PreferencesExt prefs;

  private BeanTable wmoTable, entryTable;
  private JSplitPane split;

  private TextHistoryPane compareTA;
  private IndependentWindow infoWindow;

  public GribWmoCodesPanel(final PreferencesExt prefs, JPanel buttPanel) {
    this.prefs = prefs;

    wmoTable = new BeanTable(CodeTableBean.class, (PreferencesExt) prefs.node("CodeTableBean"), false);
    wmoTable.addListSelectionListener(e -> {
        CodeTableBean csb = (CodeTableBean) wmoTable.getSelectedBean();
        setEntries(csb.wmoTable);
    });

    ucar.ui.widget.PopupMenu varPopup = new PopupMenu(wmoTable.getJTable(), "Options");
    varPopup.addAction("Show table", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        Formatter out = new Formatter();
        CodeTableBean csb = (CodeTableBean) wmoTable.getSelectedBean();
        csb.showTable(out);
        compareTA.setText(out.toString());
        compareTA.gotoTop();
        infoWindow.setVisible(true);
      }
    });

    entryTable = new BeanTable(EntryBean.class, (PreferencesExt) prefs.node("EntryBean"), false);

    AbstractButton dupButton = BAMutil.makeButtcon("Select", "Look for problems in this table", false);
    dupButton.addActionListener(e -> lookForProblems());
    buttPanel.add(dupButton);

    // the info window
    compareTA = new TextHistoryPane();
    infoWindow = new IndependentWindow("Extra Information", BAMutil.getImage("nj22/NetcdfUI"), compareTA);
    infoWindow.setBounds((Rectangle) prefs.getBean("InfoWindowBounds", new Rectangle(300, 300, 800, 600)));

    split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, false, wmoTable, entryTable);
    split.setDividerLocation(prefs.getInt("splitPos", 500));

    setLayout(new BorderLayout());
    add(split, BorderLayout.CENTER);

    setTable(WmoCodeFlagTables.standard);
  }

  private WmoCodeFlagTables.Version currTable = null;

  public void setTable(WmoCodeFlagTables.Version v) {
    try {
      List<CodeTableBean> dds = new ArrayList<>();
      for (WmoTable wmoTable : WmoCodeFlagTables.getInstance().getWmoTables()) {
        dds.add(new CodeTableBean(wmoTable));
      }
      wmoTable.setBeans(dds);
      currTable = v;

    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public void save() {
    wmoTable.saveState(false);
    entryTable.saveState(false);
    prefs.putBeanObject("InfoWindowBounds", infoWindow.getBounds());
    //prefs.putBeanObject("InfoWindowBounds2", infoWindow2.getBounds());
    prefs.putInt("splitPos", split.getDividerLocation());
    //prefs.putInt("splitPos2", split2.getDividerLocation());
  }

  public void setEntries(WmoTable wmoTable) {
    List<EntryBean> beans = new ArrayList<>();
    for (WmoTable.WmoEntry d : wmoTable.getEntries()) {
      beans.add(new EntryBean(d));
    }
    entryTable.setBeans(beans);
  }

  public class CodeTableBean {
    WmoTable wmoTable;

    // no-arg constructor
    public CodeTableBean() {
    }

    // create from a dataset
    public CodeTableBean(WmoTable wmoTable) {
      this.wmoTable = wmoTable;
    }

    public String getTitle() {
      return wmoTable.getName();
    }

    public String getTableNo() {
      return wmoTable.getId();
    }

    public String getType() {
      return wmoTable.getType().name();
    }

    void showTable(Formatter f) {
      f.format("Code Table %s (%s)%n", wmoTable.getName(), wmoTable.getId());
      for (WmoTable.WmoEntry entry : wmoTable.getEntries()) {
        f.format("  %3d: %s%n", entry.getNumber(), entry.getName());
      }
    }
  }

  public class EntryBean {
    WmoTable.WmoEntry entry;

    // no-arg constructor
    public EntryBean() {
    }

    // create from a dataset
    public EntryBean(WmoTable.WmoEntry entry) {
      this.entry = entry;
    }

    public int getNumber() {
      return entry.getNumber();
    }

    public String getName() {
      return entry.getName();
    }

    public String getUnit() {
      return entry.getUnit();
    }

    public String getUdunit() {
      if (entry.getUnit() == null) return "";                               
      if (entry.getUnit().length() == 0) return "";

      try {
        SimpleUnit su = SimpleUnit.factoryWithExceptions(entry.getUnit());
        if (su.isUnknownUnit())
          return ("UNKNOWN");
        else
          return su.toString();
      } catch (Exception ioe) {
        return "FAIL";
      }
    }

    public String getStatus() {
      return entry.getStatus();
    }

    public int getLine() {
      return entry.getLine();
    }
    public int getValue() {
       return entry.getValue();
     }
  }

  private void lookForProblems() {
    int total = 0;
    int dups = 0;

    Map<String, WmoTable.WmoEntry> paramSet = new HashMap<>();
    try (Formatter f = new Formatter()) {
      f.format("WMO parameter table %s%n", currTable);
      f.format("%nDuplicates Names%n");
      for (Object t : wmoTable.getBeans()) {
        WmoTable gt = ((CodeTableBean) t).wmoTable;
        if (gt.getType() != TableType.param)
          continue;
        for (WmoTable.WmoEntry p : gt.getEntries()) {
          if (p.getMeaning().equalsIgnoreCase("Reserved"))
            continue;
          if (p.getMeaning().equalsIgnoreCase("Missing"))
            continue;
          if (p.getStart() != p.getStop())
            continue;

          WmoTable.WmoEntry pdup = paramSet.get(p.getName());
          if (pdup != null) {
            f.format("Duplicate %s%n", p);
            f.format("          %s%n%n", pdup);
            dups++;
          } else {
            paramSet.put(p.getName(), p);
          }
          total++;
        }
      }
      f.format("%nTotal=%d dups=%d%n%n", total, dups);

      total = 0;
      dups = 0;
      f.format("Names with parenthesis%n");
      for (Object t : wmoTable.getBeans()) {
        WmoTable gt = ((CodeTableBean) t).wmoTable;
        if (gt.getType() != TableType.param)
          continue;
        for (WmoTable.WmoEntry p : gt.getEntries()) {
          if (p.getMeaning().indexOf('(') > 0) {
            f.format("  %s:%n  org='%s'%n name='%s' %n%n", p.getId(), p.getMeaning(), p.getName());
            dups++;
          }
          total++;
        }
      }
      f.format("%nTotal=%d parens=%d%n%n", total, dups);

      total = 0;
      dups = 0;
      f.format("non-udunits%n");
      for (Object t : wmoTable.getBeans()) {
        WmoTable gt = ((CodeTableBean) t).wmoTable;
        if (gt.getType() != TableType.param)
          continue;
        for (WmoTable.WmoEntry p : gt.getEntries()) {
          if (p.getUnit() == null)
            continue;
          if (p.getUnit().length() == 0)
            continue;
          try {
            SimpleUnit su = SimpleUnit.factoryWithExceptions(p.getUnit());
            if (su.isUnknownUnit()) {
              f.format("%s %s has UNKNOWN udunit%n", p.getId(), p.getUnit());
              dups++;
            }
          } catch (Exception ioe) {
            f.format("%s %s FAILS on udunit parse%n", p.getId(), p.getUnit());
            dups++;
          }
          total++;
        }
      }
      f.format("%nTotal=%d problems=%d%n%n", total, dups);

      compareTA.setText(f.toString());
    }
    infoWindow.show();
  }
}

/*

  private boolean showSame = false, showCase = false, showUnknown = false;
  /* private void compareToCurrent() {
    int total = 0;
    int nsame = 0;
    int nsameIgn = 0;
    int ndiff = 0;
    int unknownCount = 0;

    Formatter f = new Formatter();
    f.format("DIFFERENCES of %s with 4.2 standard parameter table%n", currTable);
    List tables = wmoTable.getBeans();
    for (Object t : tables) {
      WmoTable gt = ((CodeTableBean) t).wmoTable;
      if (!gt.isParameter) continue;
      for (WmoTable.WmoEntry p : gt.entries) {
        if (p.meaning.equalsIgnoreCase("Reserved")) continue;
        if (p.meaning.equalsIgnoreCase("Missing")) continue;
        if (p.start != p.stop) continue;

        GridParameter gp = ParameterTable.getParameter(gt.discipline, gt.category, p.start);
        String paramDesc = gp.getDescription();
        boolean unknown = paramDesc.startsWith("Unknown");
        if (unknown) unknownCount++;
        boolean same = paramDesc.equals(p.name);
        if (same) nsame++;
        boolean sameIgnore = paramDesc.equalsIgnoreCase(p.name);
        if (sameIgnore) nsameIgn++;
        else ndiff++;
        total++;

        String unitsCurr = gp.getUnit();
        String unitsWmo = p.unit;
        boolean sameUnits = (unitsWmo == null) ? (unitsCurr == null) : unitsWmo.equals(unitsCurr);
        same = same && sameUnits;

        if (unknown && !showUnknown) continue;
        if (same && !showSame) continue;
        if (sameIgnore && !showCase) continue;

        String state = same ? "  " : (sameIgnore ? "* " : "**");
        f.format("%s%d %d %d (%d)%n wmo =%s%n curr=%s%n", state, gt.discipline, gt.category, p.start, p.line, p.name, paramDesc);
        if (!sameUnits) f.format(" units wmo='%s' curr='%s' %n", unitsWmo, unitsCurr);
      }
    }
    f.format("%nTotal=%d same=%d sameIgnoreCase=%d dif=%d unknown=%d%n", total, nsame, nsameIgn, ndiff, unknownCount);
    compareTA.setText(f.toString());
    infoWindow.show();
  }

  private void compareToStandardWMO() {
    int total = 0;
    int nsame = 0;
    int nsameIgn = 0;
    int ndiff = 0;
    int unknownCount = 0;
    int missingCount = 0;

    Formatter f = new Formatter();
    f.format("DIFFERENCES of %s with standard WMO table%n", currTable);
    List tables = wmoTable.getBeans();
    for (Object t : tables) {
      WmoTable gt = ((CodeTableBean) t).wmoTable;
      if (!gt.isParameter) continue;
      for (WmoTable.WmoEntry p : gt.getEntries()) {
        if (p.getMeaning().equalsIgnoreCase("Reserved")) continue;
        if (p.getMeaning().equalsIgnoreCase("Missing")) continue;
        if (p.start != p.stop) continue;

        WmoTable.WmoEntry wmo = WmoTable.getParameterEntry(gt.discipline, gt.category, p.start);
        if (wmo == null) {
          missingCount++;
          f.format(" NEW %d %d %d %s (%s)%n",  gt.discipline, gt.category, p.start, p.name, p.unit);
          continue;
        }

        String paramDesc = wmo.getName();
        boolean unknown = paramDesc.startsWith("Unknown");
        if (unknown) unknownCount++;
        boolean same = paramDesc.equals(p.name);
        if (same) nsame++;
        boolean sameIgnore = paramDesc.equalsIgnoreCase(p.name);
        if (sameIgnore) nsameIgn++;
        else ndiff++;
        total++;

        String unitsCurr = wmo.getUnit();
        String unitsWmo = p.unit;
        boolean sameUnits = (unitsWmo == null) ? (unitsCurr == null) : unitsWmo.equals(unitsCurr);
        same = same && sameUnits;

        if (unknown && !showUnknown) continue;
        if (same && !showSame) continue;
        if (sameIgnore && !showCase) continue;

        String state = same ? "  " : (sameIgnore ? "* " : "**");
        f.format("%s%d %d %d (%d)%n this='%s'%n wmo='%s'%n", state, gt.discipline, gt.category, p.start, p.line, p.name, paramDesc);
        if (!sameUnits) f.format(" units this='%s' wmo='%s' %n", unitsWmo, unitsCurr);
      }
    }
    f.format("%nTotal=%d same=%d sameIgnoreCase=%d dif=%d unknown=%d new=%d%n", total, nsame, nsameIgn, ndiff, unknownCount, missingCount);
    compareTA.setText(f.toString());
    infoWindow.show();
  }

  private char[] remove = new char[]{'(', ')', ' ', '"', ',', '*', '-'};
  private String[] replace = new String[]{"", "", "", "", "", "", ""};

  private boolean equiv(String org1, String org2) {
    String s1 = StringUtil2.replace(org1, remove, replace).toLowerCase();
    String s2 = StringUtil2.replace(org2, remove, replace).toLowerCase();
    return s1.equals(s2);
  }

  private boolean equivUnits(String unitS1, String unitS2) {
    String lower1 = unitS1.toLowerCase();
    String lower2 = unitS2.toLowerCase();
    if (lower1.equals(lower2)) return true;
    if (lower1.startsWith("code") && lower2.startsWith("code")) return true;
    if (lower1.startsWith("flag") && lower2.startsWith("flag")) return true;
    if (unitS1.startsWith("CCITT") && unitS2.startsWith("CCITT")) return true;

    try {
      return SimpleUnit.isCompatibleWithExceptions(unitS1, unitS2);

    } catch (Exception e) {
      return equiv(unitS1, unitS2);
    }
  }

  private void checkCurrentModels() throws IOException {
    int total = 0;
    int nsame = 0;
    int nsameIgn = 0;
    int ndiff = 0;

    String dirName = "Q:/cdmUnitTest/tds/ncep";

    Formatter fm = new Formatter();
    fm.format("Check Current Models in directory %s%n", dirName);
    File allDir = new File(dirName);
    if (!allDir.exists()) {
      return;
    }

    File[] allFiles = allDir.listFiles();
    List<File> flist = (allFiles == null) ? new ArrayList<File>(0) : Arrays.asList(allFiles);
    Collections.sort(flist);

    for (File f : flist) {
      String name = f.getAbsolutePath();
      if (f.isDirectory()) continue;
      if (!name.endsWith(".grib2")) continue;
      fm.format("Check file %s%n", name);

      try (GridDataset ncfile= GridDataset.open(name)) {
        for (GridDatatype dt : ncfile.getGrids()) {
          String currName = dt.getShortName();
          Attribute att = dt.findAttributeIgnoreCase("Grib2_Parameter");
          if (att != null && att.getLength() == 3) {
            int discipline = (Integer) att.getValue(0);
            int category = (Integer) att.getValue(1);
            int number = (Integer) att.getValue(2);

            if (number >= 192) continue;
            total++;

            WmoTable.WmoEntry entry = WmoTable.getParameterEntry(discipline, category, number);
            if (entry == null) {
              fm.format("%n%d %d %d CANT FIND %s%n", discipline, category, number, currName);
              continue;
            }

            String wmoName = GribUtils.makeNameFromDescription(entry.name);
            boolean same = currName.startsWith(wmoName);
            if (same) {
              nsame++;
            } else {
              currName = dt.getShortName().toLowerCase();
              String wmoNameIgn = entry.name.toLowerCase();
              boolean ignSame = currName.startsWith(wmoNameIgn);
              if (ignSame) nsameIgn++;
              else ndiff++;
            }

            /* String unitsCurr = dt.findAttributeIgnoreCase(CDM.UNITS).getStringValue();
            String unitsWmo = entry.unit;
            boolean sameUnits = (unitsWmo == null) ? (unitsCurr == null) : unitsWmo.equals(unitsCurr);
            same = same && sameUnits;

            if (!same)
              fm.format("%d %d %d%n wmo =%s%n curr=%s%n", discipline, category, number, wmoName, dt.getShortName());
            //if (!sameUnits) fm.format(" units wmo='%s' curr='%s' %n", unitsWmo, unitsCurr);
          }
        }

      } catch (IOException e) {
        fm.format("Error on %s = %s%n", name, e.getMessage());
      }
    }

    fm.format("%nTotal=%d same=%d sameIgnoreCase=%d dif=%d%n", total, nsame, nsameIgn, ndiff);
    compareTA.setText(fm.toString());
    infoWindow.show();
  }
 */
