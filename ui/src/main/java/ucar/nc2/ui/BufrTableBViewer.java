/*
 * Copyright (c) 1998 - 2009. University Corporation for Atmospheric Research/Unidata
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

import ucar.nc2.ui.dialog.BufrBCompare;
import ucar.nc2.ui.dialog.CompareDialog;
import ucar.util.prefs.PreferencesExt;
import ucar.util.prefs.ui.BeanTableSorted;
import ucar.nc2.units.DateFormatter;
import ucar.nc2.units.SimpleUnit;
import ucar.nc2.iosp.bufr.*;
import ucar.nc2.iosp.bufr.tables.BufrTables;
import ucar.nc2.iosp.bufr.tables.TableB;
import ucar.unidata.util.StringUtil;
import ucar.unidata.io.RandomAccessFile;

import javax.swing.*;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.ListSelectionEvent;

import thredds.ui.TextHistoryPane;
import thredds.ui.IndependentWindow;
import thredds.ui.FileManager;
import thredds.ui.BAMutil;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.*;
import java.util.List;
import java.io.*;

/**
 * Class Description  HEY
 *
 * @author caron
 * @since Dec 1, 2009
 */


public class BufrTableBViewer extends JPanel {
  private PreferencesExt prefs;

  private BeanTableSorted ddsTable, variantTable;
  private JSplitPane split, split2;

  private TextHistoryPane compareTA, infoTA2;
  private IndependentWindow infoWindow, infoWindow2;

  private TableB currTable, refTable;

  private StructureTable dataTable;
  private IndependentWindow dataWindow;
  private FileManager fileChooser;
  private DateFormatter df = new DateFormatter();

  public BufrTableBViewer(final PreferencesExt prefs, JPanel buttPanel) {
    this.prefs = prefs;

    ddsTable = new BeanTableSorted(DdsBean.class, (PreferencesExt) prefs.node("DdsBean"), false);
    ddsTable.addListSelectionListener(new ListSelectionListener() {
      public void valueChanged(ListSelectionEvent e) {
        DdsBean csb = (DdsBean) ddsTable.getSelectedBean();
        showVariants(csb);
      }
    });

    variantTable = new BeanTableSorted(DdsBean.class, (PreferencesExt) prefs.node("VariantBean"), false);
    variantTable.addListSelectionListener(new ListSelectionListener() {
      public void valueChanged(ListSelectionEvent e) {
        DdsBean csb = (DdsBean) variantTable.getSelectedBean();
      }
    });

    thredds.ui.PopupMenu varPopup = new thredds.ui.PopupMenu(ddsTable.getJTable(), "Options");
    varPopup.addAction("Show uses", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        Formatter out = new Formatter();
        DdsBean csb = (DdsBean) ddsTable.getSelectedBean();
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
    });

    AbstractButton compareButton = BAMutil.makeButtcon("Select", "Compare to standard table", false);
    compareButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        compareToStandard();
      }
    });
    buttPanel.add(compareButton);

    AbstractAction refAction = new AbstractAction() {
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
          JOptionPane.showMessageDialog(BufrTableBViewer.this, filename + " successfully written");

        } catch (Exception ex) {
          JOptionPane.showMessageDialog(BufrTableBViewer.this, "ERROR: " + ex.getMessage());
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
          JOptionPane.showMessageDialog(BufrTableBViewer.this, filename + " successfully written");

        } catch (Exception ex) {
          JOptionPane.showMessageDialog(BufrTableBViewer.this, "ERROR: " + ex.getMessage());
          ex.printStackTrace();
        }
      }
    };
    BAMutil.setActionProperties(localAction, "dd", "write local", false, 'C', -1);
    BAMutil.addActionToContainer(buttPanel, localAction);

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

    split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, false, ddsTable, variantTable);
    split.setDividerLocation(prefs.getInt("splitPos", 500));

    setLayout(new BorderLayout());
    add(split, BorderLayout.CENTER);
  }

  public void save() {
    ddsTable.saveState(false);
    variantTable.saveState(false);
    prefs.putBeanObject("InfoWindowBounds", infoWindow.getBounds());
    //prefs.putBeanObject("InfoWindowBounds2", infoWindow2.getBounds());
    prefs.putInt("splitPos", split.getDividerLocation());
    //prefs.putInt("splitPos2", split2.getDividerLocation());
    if (fileChooser != null) fileChooser.save();
  }

  public void setBufrTableB(String filename, String mode) throws IOException {
    TableB tableB = BufrTables.readTableB(filename, mode, true); // force read
    int pos = filename.lastIndexOf("/");
    String src = (pos > 0) ? filename.substring(pos + 1) : filename;

    List<TableB.Descriptor> listDesc = new ArrayList<TableB.Descriptor>(tableB.getDescriptors());
    Collections.sort(listDesc);
    List<DdsBean> dds = new ArrayList<DdsBean>(listDesc.size());
    for (TableB.Descriptor d : listDesc) {
      dds.add(new DdsBean(src, d));
    }
    ddsTable.setBeans(dds);
    currTable = tableB;
  }

  private String checkDiff(TableB.Descriptor want) {
    if (refTable == null) return "n/a";
    for (TableB.Descriptor d1 : refTable.getDescriptors()) {
      if (d1.getId() != want.getId()) continue;
      if (d1.getScale() != want.getScale()) return "diff";
      if (d1.getRefVal() != want.getRefVal()) return "diff";
      if (d1.getDataWidth() != want.getDataWidth()) return "diff";
      return "";
    }
    return want.isLocal() ? "local" : "new";
  }


  /*
Class,FXY,enElementName,BUFR_Unit,BUFR_Scale,BUFR_ReferenceValue,BUFR_DataWidth_Bits,CREX_Unit,CREX_Scale,CREX_DataWidth
20,20009,General weather indicator (TAF/METAR),Code table,0,0,4,Code table,0,2
   */
  private void writeDiff(TableB wmo, TableB t, Formatter out) {
    out.format("#%n# BUFR diff written from %s against %s %n#%n", t.getName(), wmo.getName());
    out.format("Class,FXY,enElementName,BUFR_Unit,BUFR_Scale,BUFR_ReferenceValue,BUFR_DataWidth_Bits%n");
    List<TableB.Descriptor> listDesc = new ArrayList<TableB.Descriptor>(t.getDescriptors());
    Collections.sort(listDesc);
    for (TableB.Descriptor d1 : listDesc) {
      TableB.Descriptor d2 = wmo.getDescriptor(d1.getId());
      if ((d2 == null) || (d1.getScale() != d2.getScale()) || (d1.getRefVal() != d2.getRefVal()) || (d1.getDataWidth() != d2.getDataWidth())) {
        short fxy = d1.getId();
        int f = (fxy & 0xC000) >> 14;
        int x  = (fxy & 0x3F00) >> 8;
        int y  = fxy & 0xFF;
        out.format("%d,%2d%03d,\"%s\",%s,%d,%d,%d%n",x,x,y, d1.getName(), d1.getUnits(), d1.getScale(), d1.getRefVal(), d1.getDataWidth());
      }
    }
    out.flush();
  }

  private void writeLocal(TableB t, Formatter out) {
    out.format("#%n# BUFR local only from %s %n#%n", t.getName());
    out.format("Class,FXY,enElementName,BUFR_Unit,BUFR_Scale,BUFR_ReferenceValue,BUFR_DataWidth_Bits%n");
    List<TableB.Descriptor> listDesc = new ArrayList<TableB.Descriptor>(t.getDescriptors());
    Collections.sort(listDesc);
    for (TableB.Descriptor d1 : listDesc) {
      if (d1.isLocal()) {
        short fxy = d1.getId();
        int f = (fxy & 0xC000) >> 14;
        int x  = (fxy & 0x3F00) >> 8;
        int y  = fxy & 0xFF;
        out.format("%d,%d%03d,\"%s\",%s,%d,%d,%d%n",x,x,y,d1.getName(), d1.getUnits(), d1.getScale(), d1.getRefVal(), d1.getDataWidth());
      }
    }
    out.flush();
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

  //////////////////////////////////////////////////////////

  private BufrBCompare dialog = null;
  public void compareToStandard() {
    if (currTable == null) return;
    if (dialog == null) {
      dialog = new BufrBCompare(null);
      dialog.pack();
      dialog.addPropertyChangeListener("OK", new PropertyChangeListener() {
        public void propertyChange(PropertyChangeEvent evt) {
          compareToStandard((BufrBCompare.Data) evt.getNewValue());
        }
      });
    }
    dialog.setVisible(true);
  }

  private void compareToStandard(BufrBCompare.Data data) {

    try {
      Formatter out = new Formatter();
      TableB ref = getStandardTable(data.name);
      if (ref == null) {
        compareTA.setText("Cant find standard table = "+data.name);
        compareTA.gotoTop();
        infoWindow.setVisible(true);
        return;
      }
      compare(currTable, ref, !data.compareNames, !data.compareUnits, out);

      compareTA.setText(out.toString());
      compareTA.gotoTop();
      infoWindow.setVisible(true);

    } catch (Throwable ioe) {
      ByteArrayOutputStream bos = new ByteArrayOutputStream(10000);
      ioe.printStackTrace(new PrintStream(bos));
      compareTA.setText(bos.toString());
      compareTA.gotoTop();
      infoWindow.setVisible(true);

    }
  }

  private void compare(TableB t1, TableB t2, boolean skipNames, boolean skipUnits, Formatter out) {
    out.format("Compare Table B%n %s%n %s %n", t1.getName(), t2.getName());
    List<TableB.Descriptor> listDesc = new ArrayList<TableB.Descriptor>(t1.getDescriptors());
    Collections.sort(listDesc);
    for (TableB.Descriptor d1 : listDesc) {
      TableB.Descriptor d2 = t2.getDescriptor(d1.getId());
      if (d2 == null)
        out.format("**No key %s in second table; local=%s%n", d1.getFxy(), d1.isLocal());
      else {
        if (!skipNames) {
          if (!equiv(d1.getName(), d2.getName()))
            out.format(" %s name%n   %s%n   %s%n", d1.getFxy(), d1.getName(), d2.getName());
        }
        if (!skipUnits) {
          if (!equivUnits(d1.getUnits(), d2.getUnits()))
            out.format(" %s units%n   %s%n   %s%n", d1.getFxy(), d1.getUnits(), d2.getUnits());
        }
        if (d1.getScale() != d2.getScale())
          out.format(" %s scale %d != %d %n", d1.getFxy(), d1.getScale(), d2.getScale());
        if (d1.getRefVal() != d2.getRefVal())
          out.format(" %s refVal %d != %d %n", d1.getFxy(), d1.getRefVal(), d2.getRefVal());
        if (d1.getDataWidth() != d2.getDataWidth())
          out.format(" %s bitWidth %d != %d %n", d1.getFxy(), d1.getDataWidth(), d2.getDataWidth());
      }
    }

    // see whats missing
    out.format("%n Missing in first table %n");
    for (TableB.Descriptor d2 : t2.getDescriptors()) {
      TableB.Descriptor d1 = t1.getDescriptor(d2.getId());
      if (d1 == null) {
        out.format("   %s%n", d2.getFxy());
      }
    }

  }


  ///////////////////////

  private HashMap<Short, List<Message>> usedDds = null;

  private void showUsed() throws IOException {
    usedDds = new HashMap<Short, List<Message>>(3000);
    scanFileForDds("Q:/cdmUnitTest/iosp/bufr/uniqueExamples.bufr");
    scanFileForDds("Q:/cdmUnitTest/iosp/bufr/uniqueIDD.bufr");
    scanFileForDds("Q:/cdmUnitTest/iosp/bufr/uniqueBrasil.bufr");
    scanFileForDds("Q:/cdmUnitTest/iosp/bufr/uniqueFnmoc.bufr");
  }

  private void showUsed(String filename) throws IOException {
    usedDds = new HashMap<Short, List<Message>>(3000);
    scanFileForDds(filename);
  }

  public void scanFileForDds(String filename) throws IOException {
    RandomAccessFile raf = new ucar.unidata.io.RandomAccessFile(filename, "r");

    MessageScanner scan = new MessageScanner(raf);
    while (scan.hasNext()) {
      Message m = scan.next();
      if (m == null) continue;
      // String src = m.getHeader().trim() +"("+Integer.toHexString(m.hashCode())+")";
      setDataDescriptors(m, m.getRootDataDescriptor());
    }
  }

  private void setDataDescriptors(Message src, DataDescriptor dds) {
    for (DataDescriptor key : dds.getSubKeys()) {
      List<Message> list = usedDds.get(key.getFxy());
      if (list == null) {
        list = new ArrayList<Message>();
        usedDds.put(key.getFxy(), list);
      }
      if (!list.contains(src))
        list.add(src);

      if (key.getSubKeys() != null)
        setDataDescriptors(src, key);
    }
  }
  ////////////////////////////////////////////////////////


  private HashMap<String, TableB> standardTables = null;
  private void initStandardTables() throws IOException {
    standardTables = new HashMap<String, TableB>();
    standardTables.put("WMO-v14", BufrTables.getWmoTableB(14));
    standardTables.put("ours-v13", BufrTables.getWmoTableB(13));
    standardTables.put("ncep-v13", BufrTables.readTableB("resource:/resources/bufrTables/reference/bufrtab.TableB_STD_0_13", "ncep", false));
    standardTables.put("ncep-v14", BufrTables.readTableB("resource:/resources/bufrTables/reference/bufrtab.TableB_STD_0_14", "ncep", false));
    standardTables.put("ecmwf-v13", BufrTables.readTableB("resource:/resources/bufrTables/local/ecmwf-B0000000000098013001.TXT", "ecmwf", false));
    standardTables.put("ukmet-v13", BufrTables.readTableB("resource:/resources/bufrTables/reference/BUFR_B_080731.xml", "ukmet", false));
  }

  private TableB getStandardTable(String name) throws IOException {
    if (standardTables == null) initStandardTables();
    return standardTables.get(name);
  }

  //////////////////////////////////////////////////////
  private HashMap<Short, List<DdsBean>> allVariants = null;
  private boolean variantsLoaded = false;
  private void loadVariants() throws IOException {
    if (allVariants == null) allVariants = new HashMap<Short, List<DdsBean>>();
    if (standardTables == null) initStandardTables();
    for (String key : standardTables.keySet()) {
      loadVariant(key, standardTables.get(key));
    }
    variantsLoaded = true;
  }

  private void loadVariant(String src, TableB tableB) {
    if (allVariants == null) allVariants = new HashMap<Short, List<DdsBean>>();
    List<TableB.Descriptor> listDesc = new ArrayList<TableB.Descriptor>(tableB.getDescriptors());
    for (TableB.Descriptor d : listDesc) {
      List<DdsBean> list = allVariants.get(d.getId());
      if (list == null) {
        list = new ArrayList<DdsBean>(10);
        allVariants.put(d.getId(), list);
      }
      list.add(new DdsBean(src, d));
    }
  }

  private void showVariants(DdsBean bean) {
    if (!variantsLoaded) try {
      loadVariants();
    } catch (IOException e) {
      e.printStackTrace();
      return;
    }

    List<DdsBean> all = allVariants.get(bean.getId());
    List<DdsBean> ddsBean = new ArrayList<DdsBean>(10);
    ddsBean.add(bean);
    if (all != null) ddsBean.addAll(all);
    variantTable.setBeans(ddsBean);
  }

  public class DdsBean {
    TableB.Descriptor dds;
    String source;
    String udunits;
    boolean checkDiff;
    String isDiff;

    // no-arg constructor
    public DdsBean() {
    }

    // create from a dataset
    public DdsBean(String source, TableB.Descriptor dds) {
      this.source = source;
      this.dds = dds;
    }

    public String getFxy() {
      return dds.getFxy();
    }

    public short getId() {
      return dds.getId();
    }

    public String getSource() {
      return source;
    }

    public String getName() {
      return dds.getName();
    }

    public String getUnits() {
      return dds.getUnits();
    }

    public String getUdunits() {
      if (udunits == null) {
        try {
          SimpleUnit su = SimpleUnit.factoryWithExceptions(dds.getUnits());
          udunits = su.isUnknownUnit() ? "" : su.toString();

        } catch (Exception ioe) {
          udunits = " unit convert failed ";
        }
      }
      return udunits;
    }

    public int getWidth() {
      return dds.getDataWidth();
    }

    public int getScale() {
      return dds.getScale();
    }

    public int getReference() {
      return dds.getRefVal();
    }

    public boolean isNumeric() {
      return dds.isNumeric();
    }

    public boolean isLocal() {
      return dds.isLocal();
    }

    public String getDiff() {
      if (!checkDiff && refTable != null) {
        isDiff = checkDiff(dds);
        checkDiff = true;
      }
      return isDiff;
    }

    public int getUsed() {
      if (usedDds == null) return 0;
      List<Message> list = usedDds.get(dds.getId());
      if (list == null) return 0;
      return list.size();
    }

  }

}
