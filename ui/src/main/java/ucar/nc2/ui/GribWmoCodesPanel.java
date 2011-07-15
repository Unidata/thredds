package ucar.nc2.ui;

import ucar.nc2.ui.widget.BAMutil;
import ucar.nc2.ui.widget.FileManager;
import ucar.nc2.ui.widget.IndependentWindow;
import ucar.nc2.ui.widget.TextHistoryPane;
import ucar.grib.grib2.ParameterTable;
import ucar.grid.GridParameter;
import ucar.nc2.Attribute;
import ucar.nc2.dt.GridDatatype;
import ucar.nc2.dt.grid.GridDataset;
import ucar.nc2.iosp.grib.tables.GribCodeTable;
import ucar.nc2.units.SimpleUnit;
import ucar.unidata.util.StringUtil;
import ucar.util.prefs.PreferencesExt;
import ucar.util.prefs.ui.BeanTableSorted;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
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

  private BeanTableSorted codeTable, entryTable;
  private JSplitPane split, split2;

  private TextHistoryPane compareTA;
  private IndependentWindow infoWindow;

  private FileManager fileChooser;

  public GribWmoCodesPanel(final PreferencesExt prefs, JPanel buttPanel) {
    this.prefs = prefs;

    codeTable = new BeanTableSorted(CodeBean.class, (PreferencesExt) prefs.node("CodeBean"), false);
    codeTable.addListSelectionListener(new ListSelectionListener() {
      public void valueChanged(ListSelectionEvent e) {
        CodeBean csb = (CodeBean) codeTable.getSelectedBean();
        setEntries(csb.code);
      }
    });

    entryTable = new BeanTableSorted(EntryBean.class, (PreferencesExt) prefs.node("EntryBean"), false);
    entryTable.addListSelectionListener(new ListSelectionListener() {
      public void valueChanged(ListSelectionEvent e) {
        EntryBean csb = (EntryBean) entryTable.getSelectedBean();
      }
    });

    /* thredds.ui.PopupMenu varPopup = new thredds.ui.PopupMenu(codeTable.getJTable(), "Options");
    varPopup.addAction("Show uses", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        Formatter out = new Formatter();
        CodeBean csb = (CodeBean) codeTable.getSelectedBean();
        if (usedDds != null) {
          List<Message> list = usedDds.get(csb.getId());
          if (list != null) {
            for (Message use : list)
              use.dumpHeaderShort(out);
          }
        }
        compareTA.setText(out.toString());
        compareTA.gotoTop();
        infoWindow.setVisible(true);
      }
    }); */

    AbstractButton compareButton = BAMutil.makeButtcon("Select", "Compare to current table", false);
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
    buttPanel.add(modelsButton);

    /* AbstractAction refAction = new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        refTable = currTable;
        loadVariant(refTable.getName(), refTable);
      }
    };
    BAMutil.setActionProperties(refAction, "Dataset", "useAsRef", false, 'C', -1);
    BAMutil.addActionToContainer(buttPanel, refAction);

    AbstractAction usedAction = new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        try {
          if (fileChooser == null)
            fileChooser = new FileManager(null, null, null, (PreferencesExt) prefs.node("FileManager"));
          String filename = fileChooser.chooseFilename();
          if (filename == null) return;
          showUsed(filename);
        } catch (IOException e1) {
          e1.printStackTrace();
        }
      }
    };
    BAMutil.setActionProperties(usedAction, "dd", "showUsed", false, 'C', -1);
    BAMutil.addActionToContainer(buttPanel, usedAction);

    AbstractAction diffAction = new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        try {
          String defloc = "C:/dev/tds/thredds/bufrTables/src/main/resources/resources/bufrTables/local";
          if (fileChooser == null)
            fileChooser = new FileManager(null, null, null, (PreferencesExt) prefs.node("FileManager"));

          String filename = fileChooser.chooseFilenameToSave(defloc + ".csv");
          if (filename == null) return;
          File file = new File(filename);
          FileOutputStream fos = new FileOutputStream(file);

          Formatter out = new Formatter(fos);
          writeDiff(BufrTables.getWmoTableB(14), currTable, out);
          fos.close();
          JOptionPane.showMessageDialog(GribTableCodes.this, filename + " successfully written");

        } catch (Exception ex) {
          JOptionPane.showMessageDialog(GribTableCodes.this, "ERROR: " + ex.getMessage());
          ex.printStackTrace();
        }
      }
    };
    BAMutil.setActionProperties(diffAction, "dd", "write diff", false, 'C', -1);
    BAMutil.addActionToContainer(buttPanel, diffAction);

    AbstractAction localAction = new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        try {
          String defloc = "C:/dev/tds/thredds/bufrTables/src/main/resources/resources/bufrTables/local";
          if (fileChooser == null)
            fileChooser = new FileManager(null, null, null, (PreferencesExt) prefs.node("FileManager"));

          String filename = fileChooser.chooseFilenameToSave(defloc + ".csv");
          if (filename == null) return;
          File file = new File(filename);
          FileOutputStream fos = new FileOutputStream(file);

          Formatter out = new Formatter(fos);
          writeLocal(currTable, out);
          fos.close();
          JOptionPane.showMessageDialog(GribTableCodes.this, filename + " successfully written");

        } catch (Exception ex) {
          JOptionPane.showMessageDialog(GribTableCodes.this, "ERROR: " + ex.getMessage());
          ex.printStackTrace();
        }
      }
    };
    BAMutil.setActionProperties(localAction, "dd", "write local", false, 'C', -1);
    BAMutil.addActionToContainer(buttPanel, localAction);   */

    // the info window
    compareTA = new TextHistoryPane();
    infoWindow = new IndependentWindow("Extra Information", BAMutil.getImage("netcdfUI"), compareTA);
    infoWindow.setBounds((Rectangle) prefs.getBean("InfoWindowBounds", new Rectangle(300, 300, 800, 600)));

    /* the info window 2
    infoTA2 = new TextHistoryPane();
    infoWindow2 = new IndependentWindow("Extra Information-2", BAMutil.getImage("netcdfUI"), infoTA2);
    infoWindow2.setBounds((Rectangle) prefs.getBean("InfoWindowBounds2", new Rectangle(300, 300, 500, 300)));

    split2 = new JSplitPane(JSplitPane.VERTICAL_SPLIT, false, ddsTable, obsTable);
    split2.setDividerLocation(prefs.getInt("splitPos2", 800)); */

    split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, false, codeTable, entryTable);
    split.setDividerLocation(prefs.getInt("splitPos", 500));

    setLayout(new BorderLayout());
    add(split, BorderLayout.CENTER);

    ///

    try {
      List<GribCodeTable> codes = GribCodeTable.getWmoStandard();
      List<CodeBean> dds = new ArrayList<CodeBean>(codes.size());
      for (GribCodeTable code : codes) {
        dds.add(new CodeBean(code));
      }
      codeTable.setBeans(dds);

    } catch (Exception e) {
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
    if (fileChooser != null) fileChooser.save();
  }

  public void setEntries(GribCodeTable codeTable) {
    List<EntryBean> beans = new ArrayList<EntryBean>(codeTable.entries.size());
    for (GribCodeTable.TableEntry d : codeTable.entries) {
      beans.add(new EntryBean(d));
    }
    entryTable.setBeans(beans);
  }

  private void lookForProblems() {
    int total = 0;
    int dups = 0;

    HashMap<String, GribCodeTable.TableEntry> paramSet = new HashMap<String, GribCodeTable.TableEntry>();
    Formatter f = new Formatter();
    f.format("Duplicates in WMO parameter table%n");
    for (Object t : codeTable.getBeans()) {
      GribCodeTable gt = ((CodeBean) t).code;
      if (!gt.isParameter) continue;
      for (GribCodeTable.TableEntry p : gt.entries) {
        if (p.meaning.equalsIgnoreCase("Reserved")) continue;
        if (p.meaning.equalsIgnoreCase("Missing")) continue;
        if (p.start != p.stop) continue;

        GribCodeTable.TableEntry pdup = paramSet.get(p.name);
        if (pdup != null) {
          f.format("Duplicate %s%n", p);
          f.format("          %s%n", pdup);
          dups++;
        } else {
          paramSet.put(p.name, p);
        }
        total++;
      }
    }
    f.format("%nTotal=%d dups=%d%n%n", total, dups);

    total = 0;
    dups = 0;
    f.format("() in WMO parameter table%n");
    for (Object t : codeTable.getBeans()) {
      GribCodeTable gt = ((CodeBean) t).code;
      if (!gt.isParameter) continue;
      for (GribCodeTable.TableEntry p : gt.entries) {
        if (p.meaning.indexOf('(') > 0) {
          f.format("  org='%s'%n name='%s' %n%n", p.meaning, p.name);
          dups++;
        }
        total++;
      }
    }
    f.format("%nTotal=%d parens=%d%n%n", total, dups);

    compareTA.setText(f.toString());
    infoWindow.show();
  }


  private boolean showSame = false, showCase = false, showUnknown = false;
  private void compareToCurrent() {
    int total = 0;
    int nsame = 0;
    int nsameIgn = 0;
    int ndiff = 0;
    int unknownCount = 0;

    Formatter f = new Formatter();
    f.format("DIFFERENCES with current parameter table%n");
    List tables = codeTable.getBeans();
    for (Object t : tables) {
      GribCodeTable gt = ((CodeBean) t).code;
      if (!gt.isParameter) continue;
      for (GribCodeTable.TableEntry p : gt.entries) {
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

  private char[] remove = new char[]{'(', ')', ' ', '"', ',', '*', '-'};
  private String[] replace = new String[]{"", "", "", "", "", "", ""};

  private boolean equiv(String org1, String org2) {
    String s1 = StringUtil.replace(org1, remove, replace).toLowerCase();
    String s2 = StringUtil.replace(org2, remove, replace).toLowerCase();
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
    int unknownCount = 0;

    String dirName = "Q:/cdmUnitTest/tds/normal";

    Formatter fm = new Formatter();
    fm.format("Check Current Models in directory %s%n", dirName);
    File allDir = new File(dirName);
    File[] allFiles = allDir.listFiles();
    List<File> flist = Arrays.asList(allFiles);
    Collections.sort(flist);

    for (File f : flist) {
      String name = f.getAbsolutePath();
      if (f.isDirectory()) continue;
      if (!name.endsWith(".grib2")) continue;
      fm.format("Check file %s%n", name);

      GridDataset ncfile = null;
      try {
        ncfile = GridDataset.open(name);
        for (GridDatatype dt : ncfile.getGrids()) {
          String currName = dt.getFullName().toLowerCase();
          Attribute att = dt.findAttributeIgnoreCase("GRIB_param_id");
          int discipline = (Integer) att.getValue(1);
          int category = (Integer) att.getValue(2);
          int number = (Integer) att.getValue(3);
          if (number >= 192) continue;
          
          GribCodeTable.TableEntry entry = GribCodeTable.getEntry(discipline, category, number);
          if (entry == null) {
            fm.format("%n%d %d %d CANT FIND %s%n", discipline, category, number, currName);
            continue;
          }

          String wmoName = entry.name.toLowerCase();
          boolean same = currName.startsWith(wmoName);
          if (same) nsame++;
          else ndiff++;
          total++;

          /* String unitsCurr = dt.findAttributeIgnoreCase("units").getStringValue();
          String unitsWmo = entry.unit;
          boolean sameUnits = (unitsWmo == null) ? (unitsCurr == null) : unitsWmo.equals(unitsCurr);
          same = same && sameUnits; */

          if (same && !showSame) continue;

          fm.format("%d %d %d%n wmo =%s%n curr=%s%n", discipline, category, number, wmoName, currName);
          //if (!sameUnits) fm.format(" units wmo='%s' curr='%s' %n", unitsWmo, unitsCurr);

        }
      } finally {
        if (ncfile != null) ncfile.close();
      }
    }

    fm.format("%nTotal=%d same=%d sameIgnoreCase=%d dif=%d unknown=%d%n", total, nsame, nsameIgn, ndiff, unknownCount);
    compareTA.setText(fm.toString());
    infoWindow.show();
  }

  public class CodeBean {
    GribCodeTable code;

    // no-arg constructor
    public CodeBean() {
    }

    // create from a dataset
    public CodeBean(GribCodeTable code) {
      this.code = code;
    }

    public String getTitle() {
      return code.name;
    }

    public int getDiscipline() {
      return code.discipline;
    }

    public String getTableNo() {
      Formatter f = new Formatter();
      f.format("%d.%d",code.m1, code.m2);

      if (code.discipline >= 0)
        f.format(".%d",code.discipline);
      if (code.category >= 0)
        f.format(".%d",code.category);

      return f.toString();
    }

    public int getCategory() {
      return code.category;
    }
    
    public boolean isParameter() {
      return code.isParameter;
    }
  }

  public class EntryBean {
    GribCodeTable.TableEntry te;

    // no-arg constructor
    public EntryBean() {
    }

    // create from a dataset
    public EntryBean(GribCodeTable.TableEntry te) {
      this.te = te;
    }

    public String getCode() {
      return te.code;
    }

    public String getName() {
      return te.name;
    }

    public String getMeaning() {
      return te.meaning;
    }

    public String getUnit() {
      return te.unit;
    }

    public String getUdunit() {
      if (te.unit == null) return "";                               
      if (te.unit.length() == 0) return "";

      try {
        SimpleUnit su = SimpleUnit.factoryWithExceptions(te.unit);
        if (su.isUnknownUnit())
          return ("UNKNOWN");
        else
          return su.toString();
      } catch (Exception ioe) {
        return "FAIL";
      }
    }

    public String getStatus() {
      return te.status;
    }

    public int getLine() {
      return te.line;
    }
  }
}