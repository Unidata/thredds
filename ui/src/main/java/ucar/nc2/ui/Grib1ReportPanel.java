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

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import thredds.inventory.CollectionManager;
import thredds.inventory.MFileCollectionManager;
import thredds.inventory.MFile;
import ucar.nc2.NetcdfFile;
import ucar.nc2.dataset.CoordinateAxis1D;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dt.GridCoordSystem;
import ucar.nc2.grib.GribIosp;
import ucar.nc2.grib.GribVariableRenamer;
import ucar.nc2.grib.grib1.*;
import ucar.nc2.Attribute;
import ucar.nc2.dt.GridDatatype;
import ucar.nc2.dt.grid.GridDataset;
import ucar.nc2.grib.grib1.tables.Grib1ParamTable;
import ucar.nc2.grib.grib1.tables.Grib1ParamTables;
import ucar.nc2.ui.widget.TextHistoryPane;
import ucar.unidata.io.RandomAccessFile;
import ucar.util.prefs.PreferencesExt;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.util.*;
import java.util.List;

/**
 * Run through collections of Grib 1 files and make reports
 *
 * @author John
 * @since 8/28/11
 */
public class Grib1ReportPanel extends JPanel {
  public static enum Report {
    checkTables, showLocalParams, scanIssues, rename, checkRename, showEncoding// , localUseSection, uniqueGds, duplicatePds, drsSummary, gdsTemplate, pdsSummary, idProblems
  }

  private PreferencesExt prefs;
  private TextHistoryPane reportPane;

  public Grib1ReportPanel(PreferencesExt prefs, JPanel buttPanel) {
    this.prefs = prefs;
    reportPane = new TextHistoryPane();
    setLayout(new BorderLayout());
    add(reportPane, BorderLayout.CENTER);
  }

  public void save() {
  }

  public void showInfo(Formatter f) {
  }

  public boolean setCollection(String spec) {
    Formatter f = new Formatter();
    f.format("collection = %s%n", spec);
    boolean hasFiles = false;

    CollectionManager dcm = getCollection(spec, f);
    if (dcm == null) {
      return false;
    }

    for (MFile mfile : dcm.getFiles()) {
      f.format(" %s%n", mfile.getPath());
      hasFiles = true;
    }

    reportPane.setText(f.toString());
    reportPane.gotoTop();
    return hasFiles;
  }

  private CollectionManager getCollection(String spec, Formatter f) {
    CollectionManager dc = null;
    try {
      dc = MFileCollectionManager.open(spec, null, f);
      dc.scan(false);

    } catch (Exception e) {
      ByteArrayOutputStream bos = new ByteArrayOutputStream(10000);
      e.printStackTrace(new PrintStream(bos));
      reportPane.setText(bos.toString());
      return null;
    }

    return dc;
  }

  public void doReport(String spec, boolean useIndex, Report which) throws IOException {
    Formatter f = new Formatter();
    f.format("%s %s %s%n", spec, useIndex, which);

    CollectionManager dcm = getCollection(spec, f);
    if (dcm == null) {
      return;
    }

    // CollectionSpecParser parser = dcm.getCollectionSpecParser();

    f.format("top dir = %s%n", dcm.getRoot());
    //f.format("filter = %s%n", parser.getFilter());
    reportPane.setText(f.toString());

    File top = new File(dcm.getRoot());
    if (!top.exists()) {
      f.format("top dir = %s does not exist%n", dcm.getRoot());
    } else {

      switch (which) {
        case checkTables:
          doCheckTables(f, dcm, useIndex);
          break;
        case showLocalParams:
          doCheckLocalParams(f, dcm, useIndex);
          break;
        case scanIssues:
          doScanIssues(f, dcm, useIndex);
          break;
        case rename:
          doRename(f, dcm, useIndex);
          break;
        case checkRename:
          doCheckRename(f, dcm, useIndex);
          break;
        case showEncoding:
          doShowEncoding(f, dcm, useIndex);
          break;
      }
    }

    reportPane.setText(f.toString());
    reportPane.gotoTop();
  }

  ///////////////////////////////////////////////

  private void doCheckLocalParams(Formatter f, CollectionManager dcm, boolean useIndex) throws IOException {
    f.format("Check Grib-1 Parameter Tables for local entries%n");
    int[] accum = new int[4];

    for (MFile mfile : dcm.getFiles()) {
      String path = mfile.getPath();
      if (path.endsWith(".gbx8") || path.endsWith(".gbx9") || path.endsWith(".ncx")) continue;
      f.format("%n %s%n", path);
      try {
        doCheckLocalParams(mfile, f, accum);
      } catch (Throwable t) {
        System.out.printf("FAIL on %s%n", mfile.getPath());
        t.printStackTrace();
      }
    }

    f.format("%nGrand total=%d local = %d missing = %d%n", accum[0], accum[2], accum[3]);
  }

  private void doCheckLocalParams(MFile ff, Formatter fm, int[] accum) throws IOException {
    int local = 0;
    int miss = 0;
    int nonop = 0;
    int total = 0;

    GridDataset ncfile = null;
    try {
      ncfile = GridDataset.open(ff.getPath());
      Attribute gatt = ncfile.findGlobalAttributeIgnoreCase("GRIB table");
      if (gatt != null) {
        String[] s = gatt.getStringValue().split("-");
        Grib1ParamTable gtable = new Grib1ParamTables().getParameterTable(Integer.parseInt(s[0]), Integer.parseInt(s[1]), Integer.parseInt(s[2]));
        fm.format("  %s == %s%n", gatt, gtable.getPath());
      }
      for (GridDatatype dt : ncfile.getGrids()) {
        String currName = dt.getName();
        total++;

        Attribute att = dt.findAttributeIgnoreCase("Grib_Parameter");
        int number = (att == null) ? 0 : att.getNumericValue().intValue();
        if (number >= 128) {
          fm.format("  local parameter = %s (%d) units=%s %n", currName, number, dt.getUnitsString());
          local++;
          if (currName.startsWith("VAR")) miss++;
        }

      }
    } finally {
      if (ncfile != null) ncfile.close();
    }
    fm.format("total=%d local = %d miss=%d %n", total, local, miss);
    accum[0] += total;
    accum[1] += nonop;
    accum[2] += local;
    accum[3] += miss;
  }

  private class Counter {
    Map<Integer, Integer> set = new HashMap<Integer, Integer>();
    String name;

    private Counter(String name) {
      this.name = name;
    }

    void count(int value) {
      Integer count = set.get(value);
      if (count == null)
        set.put(value, 1);
      else
        set.put(value, count + 1);
    }

    void show(Formatter f) {
      f.format("%n%s%n", name);
      java.util.List<Integer> list = new ArrayList<Integer>(set.keySet());
      Collections.sort(list);
      for (int template : list) {
        int count = set.get(template);
        f.format("   %3d: count = %d%n", template, count);
      }
    }

  }

  private class CounterS {
    Map<String, Integer> set = new HashMap<String, Integer>();
    String name;

    private CounterS(String name) {
      this.name = name;
    }

    void count(String value) {
      Integer count = set.get(value);
      if (count == null)
        set.put(value, 1);
      else
        set.put(value, count + 1);
    }

    void show(Formatter f) {
      f.format("%n%s%n", name);
      java.util.List<String> list = new ArrayList<String>(set.keySet());
      Collections.sort(list);
      for (String key : list) {
        int count = set.get(key);
        f.format("   %10s: count = %d%n", key, count);
      }
    }

  }

  /////////////////////////////////////////////////////////////////

  private void doCheckTables(Formatter f, CollectionManager dcm, boolean useIndex) throws IOException {
    CounterS tableSet = new CounterS("table");
    CounterS local = new CounterS("local");
    CounterS missing = new CounterS("missing");

    for (MFile mfile : dcm.getFiles()) {
      String path = mfile.getPath();
      if (path.endsWith(".gbx8") || path.endsWith(".gbx9") || path.endsWith(".ncx")) continue;
      f.format(" %s%n", path);
      doCheckTables(f, mfile, useIndex, tableSet, local, missing);
    }

    f.format("CHECK TABLES%n");
    tableSet.show(f);
    local.show(f);
    missing.show(f);
  }

  private void doCheckTables(Formatter fm, MFile ff, boolean useIndex, CounterS tableSet, CounterS local, CounterS missing) throws IOException {
    String path = ff.getPath();
    RandomAccessFile raf = null;
    try {
      raf = new ucar.unidata.io.RandomAccessFile(path, "r");
      raf.order(ucar.unidata.io.RandomAccessFile.BIG_ENDIAN);
      raf.seek(0);

      Grib1RecordScanner reader = new Grib1RecordScanner(raf);
      while (reader.hasNext()) {
        ucar.nc2.grib.grib1.Grib1Record gr = reader.next();
        Grib1SectionProductDefinition pds = gr.getPDSsection();
        String key = pds.getCenter() + "-" + pds.getSubCenter() + "-" + pds.getTableVersion();
        tableSet.count(key);

        if (pds.getParameterNumber() > 127)
          local.count(key);

        Grib1ParamTable table = new Grib1ParamTables().getParameterTable(pds.getCenter(), pds.getSubCenter(), pds.getTableVersion());
        if (table == null && useIndex) table = Grib1ParamTables.getDefaultTable();
        if (table == null || null == table.getParameter(pds.getParameterNumber()))
          missing.count(key);
      }

    } catch (Throwable ioe) {
      fm.format("Failed on %s == %s%n", path, ioe.getMessage());
      System.out.printf("Failed on %s%n", path);
      ioe.printStackTrace();

    } finally {
      if (raf != null) raf.close();
    }
  }

  /////////////////////////////////////////////////////////////////

  private void doScanIssues(Formatter f, CollectionManager dcm, boolean useIndex) throws IOException {
    Counter predefined = new Counter("predefined");
    Counter thin = new Counter("thin");
    Counter timeUnit = new Counter("timeUnit");
    Counter vertCoord = new Counter("vertCoord");
    Counter vertCoordInGDS = new Counter("vertCoordInGDS");

    for (MFile mfile : dcm.getFiles()) {
      String path = mfile.getPath();
      if (path.endsWith(".gbx8") || path.endsWith(".gbx9") || path.endsWith(".ncx")) continue;
      f.format(" %s%n", path);
      doScanIssues(f, mfile, useIndex, predefined, thin, timeUnit, vertCoord, vertCoordInGDS);
    }

    f.format("SCAN NEW%n");
    predefined.show(f);
    thin.show(f);
    timeUnit.show(f);
    vertCoord.show(f);
    vertCoordInGDS.show(f);
  }

  private void doScanIssues(Formatter fm, MFile ff, boolean useIndex, Counter predefined, Counter thin, Counter timeUnit,
                            Counter vertCoord, Counter vertCoordInGDS) throws IOException {
    boolean showThin = true;
    boolean showPredefined = true;
    boolean showVert = true;
    String path = ff.getPath();
    RandomAccessFile raf = null;
    try {
      raf = new ucar.unidata.io.RandomAccessFile(path, "r");
      raf.order(ucar.unidata.io.RandomAccessFile.BIG_ENDIAN);
      raf.seek(0);

      Grib1RecordScanner reader = new Grib1RecordScanner(raf);
      while (reader.hasNext()) {
        ucar.nc2.grib.grib1.Grib1Record gr = reader.next();
        Grib1SectionGridDefinition gdss = gr.getGDSsection();
        Grib1SectionProductDefinition pds = gr.getPDSsection();
        String key = pds.getCenter() + "-" + pds.getSubCenter() + "-" + pds.getTableVersion(); // for CounterS
        timeUnit.count(pds.getTimeRangeIndicator());
        vertCoord.count(pds.getLevelType());

        if (gdss.isThin()) {
          if (showThin) fm.format("  THIN= (gds=%d) %s%n", gdss.getGridTemplate(), ff.getPath());
          thin.count(gdss.getGridTemplate());
          showThin = false;
        }

        if (!pds.gdsExists()) {
          if (showPredefined) fm.format("   PREDEFINED GDS= %s%n", ff.getPath());
          predefined.count(gdss.getPredefinedGridDefinition());
          showPredefined = false;
        }

        if (gdss.hasVerticalCoordinateParameters()) {
          if (showVert) fm.format("   Has vertical coordinates in GDS= %s%n", ff.getPath());
          vertCoordInGDS.count(pds.getLevelType());
          showVert = false;
        }
      }

    } catch (Throwable ioe) {
      fm.format("Failed on %s == %s%n", path, ioe.getMessage());
      System.out.printf("Failed on %s%n", path);
      ioe.printStackTrace();

    } finally {
      if (raf != null) raf.close();
    }
  }

  /////////////////////////////////////////////////////////////////

  private void doShowEncoding(Formatter f, CollectionManager dcm, boolean useIndex) throws IOException {
    Counter decimals = new Counter("decimalScale");
    Counter nbits = new Counter("nbits");
    Counter refVal = new Counter("refVal");

    for (MFile mfile : dcm.getFiles()) {
      f.format(" %s%n", mfile.getPath());
      //if (useIndex) doShowEncodingIndex(f, mfile, decimals); else doShowEncoding(f, mfile, decimals);
      doShowEncoding(f, mfile, decimals, nbits, refVal);
    }

    decimals.show(f);
    nbits.show(f);
    refVal.show(f);
  }

  /* private void doShowEncodingIndex(Formatter fm, MFile ff, Counter decimals) throws IOException {
    Grib1Index index = createIndex(ff, f);
    if (index == null) return;

    String path = ff.getPath();
    RandomAccessFile raf = new RandomAccessFile(path, "r");

    try {
    for (ucar.nc2.grib.grib1.Grib1Record gr : index.getRecords()) {
    } finally {
    raf.close();
    }
  } */

  private void doShowEncoding(Formatter fm, MFile ff, Counter decimals, Counter nbits, Counter refVal) throws IOException {
    String path = ff.getPath();
    RandomAccessFile raf = null;
    try {
      raf = new ucar.unidata.io.RandomAccessFile(path, "r");
      raf.order(ucar.unidata.io.RandomAccessFile.BIG_ENDIAN);
      raf.seek(0);

      Grib1RecordScanner reader = new Grib1RecordScanner(raf);
      while (reader.hasNext()) {
        ucar.nc2.grib.grib1.Grib1Record gr = reader.next();
        //Grib1SectionGridDefinition gdss = gr.getGDSsection();
        //String key = pds.getCenter() + "-" + pds.getSubCenter() + "-" + pds.getTableVersion(); // for CounterS

        Grib1SectionProductDefinition pds = gr.getPDSsection();
        decimals.count(pds.getDecimalScale());
        double scale = Math.pow(10, pds.getDecimalScale());
        Grib1SectionBinaryData data = gr.getDataSection();
        nbits.count(data.getNBits(raf));
        refVal.count((int) (scale * data.getRefValue(raf)));
      }

    } catch (Throwable ioe) {
      fm.format("Failed on %s == %s%n", path, ioe.getMessage());
      System.out.printf("Failed on %s%n", path);
      ioe.printStackTrace();

    } finally {
      if (raf != null) raf.close();
    }
  }

  ///////////////////////////////////////////////////////////////////////////////////

  private void doCheckRename(Formatter f, CollectionManager dcm, boolean useIndex) throws IOException {
    f.format("CHECK Renaming uniqueness %s%n", dcm.getCollectionName());

    GribVariableRenamer renamer = new GribVariableRenamer();
    int fail = 0;
    int multiple = 0;
    int ok = 0;

    for (MFile mfile : dcm.getFiles()) {
      f.format("%n%s%n", mfile.getPath());

      NetcdfFile ncfileOld = null;
      GridDataset gdsNew = null;
      try {
        ncfileOld = NetcdfFile.open(mfile.getPath(), "ucar.nc2.iosp.grib.GribServiceProvider", -1, null, null);
        NetcdfDataset ncdOld = new NetcdfDataset(ncfileOld);
        GridDataset gridOld = new GridDataset(ncdOld);
        gdsNew = GridDataset.open(mfile.getPath());

        for (GridDatatype grid : gridOld.getGrids()) {
          // if (useIndex) {
            List<String> newNames = renamer.matchNcepNames(gdsNew, grid.getShortName());
            if (newNames.size() == 0) {
              f.format(" ***FAIL %s%n", grid.getShortName());
              fail++;
            } else if (newNames.size() != 1) {
              f.format(" *** %s multiple matches on %n", grid.getShortName());
              for (String newName : newNames)
                f.format("    %s%n", newName);
              f.format("%n");
              multiple++;
            } else if (useIndex) {
              f.format(" %s%n %s%n%n", grid.getShortName(), newNames.get(0));
              ok++;
            }

          /* } else {
            String newName = renamer.getNewName(mfile.getName(), grid.getShortName());
            if (newName == null) {
              f.format(" ***Grid %s renamer failed%n", grid.getShortName());
              continue;
            }

            // test it really exists
            GridDatatype ggrid = gdsNew.findGridByName(newName);
            if (ggrid == null) f.format(" ***Grid %s new name = %s not found%n", grid.getShortName(), newName);
          } */
        }

      } catch (Throwable t) {
        t.printStackTrace();
      } finally {
        if (ncfileOld != null) ncfileOld.close();
        if (gdsNew != null) gdsNew.close();
      }
    }

    f.format("Fail=%d multiple=%d ok=%d%n", fail, multiple, ok);
  }

    ///////////////////////////////////////////////////////////////////////////////////

  private void doRename(Formatter f, CollectionManager dcm, boolean useIndex) throws IOException {
    f.format("CHECK Grib-1 Names: Old vs New for collection %s%n", dcm.getCollectionName());

    List<VarName> varNames = new ArrayList<VarName>(3000);
    Map<String,List<String>> gridsAll = new HashMap<String,List<String>>(1000); // old -> list<new>

    for (MFile mfile : dcm.getFiles()) {
      f.format("%n%s%n", mfile.getPath());
      Map<Integer,GridMatch> gridsNew = getGridsNew(mfile, f);
      Map<Integer,GridMatch> gridsOld = getGridsOld(mfile, f);

      // look for exact match
      for (GridMatch gm : gridsNew.values()) {
        GridMatch match = gridsOld.get(gm.hashCode());
        if (match != null) {
          gm.match = match;
          match.match = gm;
        }
      }

      // look for alternative match
      for (GridMatch gm : gridsNew.values()) {
        if (gm.match == null) {
          GridMatch match = altMatch(gm, gridsOld.values());
          if (match != null) {
            gm.match = match;
            match.match = gm;
          }
        }
      }

      f.format("%n");
      List<GridMatch> listNew = new ArrayList<GridMatch>(gridsNew.values());
      Collections.sort(listNew);
      for (GridMatch gm : listNew) {
        f.format(" %s%n", gm.grid.getFullName());
        if (gm.match != null)
          f.format(" %s%n", gm.match.grid.getFullName());
        f.format("%n");
      }

      f.format("%nMISSING MATCHES IN NEW%n");
      List<GridMatch> list = new ArrayList<GridMatch>(gridsNew.values());
      Collections.sort(list);
      for (GridMatch gm : list) {
        if (gm.match == null)
          f.format(" %s (%s) == %s%n", gm.grid.getFullName(), gm.show(), gm.grid.getDescription());
      }


      f.format("%nMISSING MATCHES IN OLD%n");
      List<GridMatch> listOld = new ArrayList<GridMatch>(gridsOld.values());
      Collections.sort(listOld);
      for (GridMatch gm : listOld) {
        if (gm.match == null)
          f.format(" %s (%s)%n", gm.grid.getFullName(), gm.show());
      }

      // add to gridsAll
       for (GridMatch gmOld : listOld) {
         String key = gmOld.grid.getFullName();
         List<String> newGrids = gridsAll.get(key);
         if (newGrids == null) {
           newGrids = new ArrayList<String>();
           gridsAll.put(key, newGrids);
         }
         if (gmOld.match != null) {
           String keyNew = gmOld.match.grid.getFullName()+" == "+gmOld.match.grid.getDescription();
           if (!newGrids.contains(keyNew)) newGrids.add(keyNew);
         }
      }

      // add matches to VarNames
      for (GridMatch gmOld : listOld) {
        if (gmOld.match == null) {
          f.format("MISSING %s (%s)%n", gmOld.grid.getFullName(), gmOld.show());
          continue;
        }
        Attribute att = gmOld.match.grid.findAttributeIgnoreCase(GribIosp.VARIABLE_ID_ATTNAME);
        String varId = att == null ? "" : att.getStringValue();
        varNames.add(new VarName(mfile.getName(), gmOld.grid.getShortName(), gmOld.match.grid.getShortName(), varId));
      }

    }

    f.format("%nOLD -> NEW MAPPINGS%n");
    List<String> keys = new ArrayList<String>(gridsAll.keySet());
    int total = keys.size();
    int dups = 0;
    Collections.sort(keys);
    for (String key : keys) {
      f.format(" OLD %s%n", key);
      List<String> newGrids = gridsAll.get(key);
      Collections.sort(newGrids);
      if (newGrids.size() > 1) dups++;
      for (String newKey : newGrids)
        f.format(" NEW %s%n", newKey);
      f.format("%n");
    }
    f.format("Number with more than one map=%d total=%d%n", dups, total);

        // old -> new mapping xml table
    if (!useIndex) {
      Element rootElem = new Element("gribVarMap");
      Document doc = new Document(rootElem);
      rootElem.setAttribute("collection", dcm.getCollectionName());

      String currentDs = null;
      Element dsElem = null;
      for (VarName vn : varNames) {
        if (!vn.dataset.equals(currentDs)) {
          dsElem = new Element("dataset");
          rootElem.addContent(dsElem);
          dsElem.setAttribute("name", vn.dataset);
          currentDs = vn.dataset;
        }
        Element param = new Element("param");
        dsElem.addContent(param);
        param.setAttribute("oldName", vn.oldVar);
        param.setAttribute("newName", vn.newVar);
        param.setAttribute("varId", vn.varId);
      }

      FileOutputStream fout = new FileOutputStream("C:/tmp/grib1VarMap.xml");
      XMLOutputter fmt = new XMLOutputter(Format.getPrettyFormat());
      fmt.output(doc, fout);
      fout.close();
    }

  }

  private class VarName {
    String dataset;
    String oldVar;
    String newVar;
    String varId;

    private VarName(String dataset, String oldVar, String newVar, String varId) {
      this.dataset = dataset;
      this.oldVar = oldVar;
      this.newVar = newVar;
      this.varId = varId;
    }
  }

  private GridMatch altMatch(GridMatch want, Collection<GridMatch> test) {
    // look for scale factor errors in prob
    for (GridMatch gm : test) {
      if (gm.match != null) continue; // already matched
      if (gm.altMatch(want)) return gm;
    }

    // give up matching the prob
    for (GridMatch gm : test) {
      if (gm.match != null) continue; // already matched
      if (gm.altMatchNoProb(want)) return gm;
    }

    return null;
  }

  private class GridMatch implements Comparable<GridMatch> {
    GridDatatype grid;
    GridMatch match;
    boolean isNew;
    int[] param = new int[3];
    int level;
    boolean isLayer, isError;
    int interval = -1;
    int prob = -1;
    int ens = -1;
    int probLimit;

    private GridMatch(GridDataset gds, GridDatatype grid, boolean aNew) {
      this.grid = grid;
      isNew = aNew;

      GridCoordSystem gcs = grid.getCoordinateSystem();
      CoordinateAxis1D zaxis = gcs.getVerticalAxis();
      if (zaxis != null) isLayer = zaxis.isInterval();

      if (isNew) {
      /* :Grib1_Center = 7; // int
       :Grib1_Subcenter = 0; // int
       :Grib1_TableVersion = 2; // int
       :Grib1_Parameter = 33; */
        Attribute att = grid.findAttributeIgnoreCase("Grib1_Center");
        param[0] = att.getNumericValue().intValue();
        att = grid.findAttributeIgnoreCase("Grib1_Subcenter");
        param[1] = att.getNumericValue().intValue();
        att = grid.findAttributeIgnoreCase("Grib1_Parameter");
        param[2] = att.getNumericValue().intValue();

        att = grid.findAttributeIgnoreCase("Grib1_Level_Type");
        level = att.getNumericValue().intValue();
        isError = grid.getName().contains("error");

        att = grid.findAttributeIgnoreCase("Grib1_Statistical_Interval_Type");
        if (att != null) {
          int intv = att.getNumericValue().intValue();
          if (intv != 255) interval = intv;
        }

        att = grid.findAttributeIgnoreCase("Grib1_Probability_Type"); // ??
        if (att != null) prob = att.getNumericValue().intValue();

        att = grid.findAttributeIgnoreCase("Grib1_Probability_Name"); // ??
        if (att != null) {
          String pname = att.getStringValue();
          int pos = pname.indexOf('_');
          pname = pname.substring(pos+1);
          probLimit = (int) (1000.0 * Double.parseDouble(pname));
        }

        att = grid.findAttributeIgnoreCase("Grib1_Ensemble_Derived_Type");
        if (att != null) ens = att.getNumericValue().intValue();

      } else { // OLD
        Attribute att = grid.findAttributeIgnoreCase("GRIB_center_id");
        param[0] = att.getNumericValue().intValue();
        att = gds.findGlobalAttributeIgnoreCase("Originating_subcenter_id");
        param[1] = att.getNumericValue().intValue();
        att = grid.findAttributeIgnoreCase("GRIB_param_number");
        param[2] = att.getNumericValue().intValue();

        att = grid.findAttributeIgnoreCase("GRIB_level_type");
        level = att.getNumericValue().intValue();
        isError = grid.getName().contains("error");

        String desc = grid.getDescription();
        if (desc.contains("Accumulation"))
          interval = 4;
        else if (desc.contains("Accumulation"))
          interval = 4;

        att = grid.findAttributeIgnoreCase("GRIB_probability_type");
        if (att != null) prob = att.getNumericValue().intValue();
        if (prob == 0) {
          att = grid.findAttributeIgnoreCase("GRIB_probability_lower_limit");
          if (att != null) probLimit = (int) (1000 * att.getNumericValue().doubleValue());
          //if (Math.abs(probLimit) > 100000) probLimit /= 1000; // wierd bug in 4.2
        } else if (prob == 1) {
          att = grid.findAttributeIgnoreCase("GRIB_probability_upper_limit"); // GRIB_probability_upper_limit = 12.89; // double
          if (att != null) probLimit = (int) (1000 * att.getNumericValue().doubleValue());
          //if (Math.abs(probLimit) > 100000) probLimit /= 1000; // wierd bug in 4.2
        }

        att = grid.findAttributeIgnoreCase("GRIB_ensemble_derived_type");
        if (att != null) ens = att.getNumericValue().intValue();
      }
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      GridMatch gridMatch = (GridMatch) o;

      if (param != gridMatch.param) return false;
      if (ens != gridMatch.ens) return false;
      if (interval != gridMatch.interval) return false;
      if (isError != gridMatch.isError) return false;
      if (isLayer != gridMatch.isLayer) return false;
      if (level != gridMatch.level) return false;
      if (prob != gridMatch.prob) return false;
      if (probLimit != gridMatch.probLimit) return false;

      return true;
    }

    public boolean altMatch(GridMatch gridMatch) {
      if (!altMatchNoProb(gridMatch)) return false;

      if (probLimit/1000 == gridMatch.probLimit) return true;
      if (probLimit == gridMatch.probLimit/1000) return true;

      return false;
    }

    public boolean altMatchNoProb(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      GridMatch gridMatch = (GridMatch) o;

      if (ens != gridMatch.ens) return false;
      if (interval != gridMatch.interval) return false;
      if (isError != gridMatch.isError) return false;
      if (isLayer != gridMatch.isLayer) return false;
      if (level != gridMatch.level) return false;
      if (prob != gridMatch.prob) return false;

      return true;
    }


    @Override
    public int hashCode() {
      int result = 1;
      result = 31 * result + level;
      result = 31 * result + param[0];
      result = 31 * result + (isLayer ? 1 : 0);
      result = 31 * result + (isError ? 1 : 0);
      result = 31 * result + param[1];
      result = 31 * result + interval;
      result = 31 * result + prob;
      result = 31 * result + param[2];
      result = 31 * result + ens;
      result = 31 * result + probLimit;
      return result;
    }

    @Override
    public int compareTo(GridMatch o) {
      return grid.compareTo(o.grid);
    }

    String show() {
      Formatter f = new Formatter();
      f.format("%d-%d-%d-", param[0],param[1],param[2]);
      f.format("%d", level);
      if (isLayer) f.format("_layer");
      if (interval >= 0) f.format("_intv%d",interval);
      if (prob >= 0) f.format("_prob%d_%d",prob,probLimit);
      if (ens >= 0) f.format("_ens%d",ens);
      if (isError) f.format("_error");
      return f.toString();
    }
  }

  private Map<Integer,GridMatch> getGridsNew(MFile ff, Formatter f) throws IOException {
    Map<Integer,GridMatch> grids = new HashMap<Integer,GridMatch>(100);
    GridDataset ncfile = null;
    try {
      ncfile = GridDataset.open(ff.getPath());
      for (GridDatatype dt : ncfile.getGrids()) {
        GridMatch gm = new GridMatch(ncfile, dt, true);
        GridMatch dup = grids.get(gm.hashCode());
        if (dup != null)
          f.format(" DUP NEW (%d == %d) = %s (%s) and DUP %s (%s)%n", gm.hashCode(), dup.hashCode(), gm.grid.getFullName(), gm.show(), dup.grid.getFullName(), dup.show());
        else
          grids.put(gm.hashCode(), gm);
      }
    } finally {
      if (ncfile != null) ncfile.close();
    }
    return grids;
  }

  private Map<Integer,GridMatch> getGridsOld(MFile ff, Formatter f) throws IOException {
    Map<Integer,GridMatch> grids = new HashMap<Integer,GridMatch>(100);
    NetcdfFile ncfile = null;
    try {
      ncfile = NetcdfFile.open(ff.getPath(), "ucar.nc2.iosp.grib.GribServiceProvider", -1, null, null);
      NetcdfDataset ncd = new NetcdfDataset(ncfile);
      GridDataset grid = new GridDataset(ncd);
      for (GridDatatype dt : grid.getGrids()) {
        GridMatch gm = new GridMatch(grid, dt, false);
        GridMatch dup = grids.get(gm.hashCode());
        if (dup != null)
          f.format(" DUP OLD (%d == %d) = %s (%s) and DUP %s (%s)%n", gm.hashCode(), dup.hashCode(), gm.grid.getFullName(), gm.show(), dup.grid.getFullName(), dup.show());
        else
          grids.put(gm.hashCode(), gm);
      }
    } catch (Throwable t) {
      t.printStackTrace();
    } finally {
      if (ncfile != null) ncfile.close();
    }
    return grids;
  }


  ///////////////////////////////////////////////

  /*
    private void doLocalUseSection(Formatter f, CollectionManager dcm, boolean useIndex) throws IOException {
      f.format("Show Local Use Section%n");

      for (MFile mfile : dcm.getFiles()) {
        f.format(" %s%n", mfile.getPath());
        doLocalUseSection(mfile, f, useIndex);
      }
    }

    private void doLocalUseSection(MFile mf, Formatter f, boolean useIndex) throws IOException {
      f.format("File = %s%n", mf);

      String path = mf.getPath();
      GribIndex index = new GribIndex();
      if (!index.readIndex(path, mf.getLastModified()))
        index.makeIndex(path, f);

      for (Grib2Record gr : index.getRecords()) {
        Grib2SectionLocalUse lus = gr.getLocalUseSection();
        if (lus == null || lus.getRawBytes() == null)
          f.format(" %10d == none%n", gr.getDataSection().getStartingPosition());
        else
          f.format(" %10d == %s%n", gr.getDataSection().getStartingPosition(), Misc.showBytes(lus.getRawBytes()));
      }
    }

    ///////////////////////////////////////////////

    private void doUniqueGds(Formatter f, CollectionManager dcm, boolean useIndex) throws IOException {
      f.format("Show Unique GDS%n");

      Map<Integer, GdsList> gdsSet = new HashMap<Integer, GdsList>();
      for (MFile mfile : dcm.getFiles()) {
        f.format(" %s%n", mfile.getPath());
        doUniqueGds(mfile, gdsSet, f);
      }

      for (GdsList gdsl : gdsSet.values()) {
        f.format("%nGDS = %d x %d (%d) %n", gdsl.gds.ny, gdsl.gds.nx, gdsl.gds.template);
        for (FileCount fc : gdsl.fileList)
          f.format("  %5d %s (%d)%n", fc.count, fc.f.getPath(), fc.countGds);
      }
    }

    private void doUniqueGds(MFile mf, Map<Integer, GdsList> gdsSet, Formatter f) throws IOException {
      String path = mf.getPath();
      GribIndex index = new GribIndex();
      if (!index.readIndex(path, mf.getLastModified()))
        index.makeIndex(path, f);

      int countGds = index.getGds().size();
      for (Grib2Record gr : index.getRecords()) {
        int hash = gr.getGDSsection().getGDS().hashCode();
        GdsList gdsList = gdsSet.get(hash);
        if (gdsList == null) {
          gdsList = new GdsList(gr.getGDSsection().getGDS());
          gdsSet.put(hash, gdsList);
        }
        FileCount fc = gdsList.contains(mf);
        if (fc == null) {
          fc = new FileCount(mf, countGds);
          gdsList.fileList.add(fc);
        }
        fc.count++;
      }
    }

    private class GdsList {
      Grib2Gds gds;
      java.util.List<FileCount> fileList = new ArrayList<FileCount>();

      private GdsList(Grib2Gds gds) {
        this.gds = gds;
      }

      FileCount contains(MFile f) {
        for (FileCount fc : fileList)
          if (fc.f.getPath().equals(f.getPath())) return fc;
        return null;
      }

    }

    private class FileCount {
      private FileCount(MFile f, int countGds) {
        this.f = f;
        this.countGds = countGds;
      }

      MFile f;
      int count = 0;
      int countGds = 0;
    }

    ///////////////////////////////////////////////

    private int countPDS, countPDSdup;

    private void doDuplicatePds(Formatter f, CollectionManager dcm, boolean useIndex) throws IOException {
      countPDS = 0;
      countPDSdup = 0;
      f.format("Show Duplicate PDS%n");
      for (MFile mfile : dcm.getFiles()) {
        doDuplicatePds(f, mfile);
      }
      f.format("Total PDS duplicates = %d / %d%n%n", countPDSdup, countPDS);
    }

    private void doDuplicatePds(Formatter f, MFile mfile) throws IOException {
      Set<Long> pdsMap = new HashSet<Long>();
      int dups = 0;
      int count = 0;

      RandomAccessFile raf = new RandomAccessFile(mfile.getPath(), "r");
      Grib2RecordScanner scan = new Grib2RecordScanner(raf);
      while (scan.hasNext()) {
        ucar.nc2.grib.grib2.Grib2Record gr = scan.next();
        Grib2SectionProductDefinition pds = gr.getPDSsection();
        long crc = pds.calcCRC();
        if (pdsMap.contains(crc))
          dups++;
        else
          pdsMap.add(crc);
        count++;
      }
      raf.close();

      f.format("PDS duplicates = %d / %d for %s%n%n", dups, count, mfile.getPath());
      countPDS += count;
      countPDSdup += dups;
    }

    ///////////////////////////////////////////////

    private class Counter {
      Map<Integer, Integer> set = new HashMap<Integer, Integer>();
      String name;

      private Counter(String name) {
        this.name = name;
      }

      void count(int value) {
        Integer count = set.get(value);
        if (count == null)
          set.put(value, 1);
        else
          set.put(value, count + 1);
      }

      void show(Formatter f) {
        f.format("%n%s%n", name);
        java.util.List<Integer> list = new ArrayList<Integer>(set.keySet());
        Collections.sort(list);
        for (int template : list) {
          int count = set.get(template);
          f.format("   %3d: count = %d%n", template, count);
        }
      }

    }

    int total = 0;
    int prob = 0;

    private void doPdsSummary(Formatter f, CollectionManager dcm, boolean useIndex) throws IOException {
      if (useIndex) {
        f.format("Check Grib-2 PDS probability and statistical variables%n");
        total = 0;
        prob = 0;
        for (MFile mfile : dcm.getFiles()) {
          f.format("%n %s%n", mfile.getPath());
          doPdsSummaryIndexed(f, mfile);
        }
        f.format("problems = %d/%d%n", prob, total);

      } else {
        Counter templateSet = new Counter("template");
        Counter timeUnitSet = new Counter("timeUnit");
        Counter statTypeSet = new Counter("statType");
        Counter NTimeIntervals = new Counter("NTimeIntervals");
        Counter processId = new Counter("processId");

        for (MFile mfile : dcm.getFiles()) {
          f.format(" %s%n", mfile.getPath());
          doPdsSummary(f, mfile, templateSet, timeUnitSet, statTypeSet, NTimeIntervals, processId);
        }

        templateSet.show(f);
        timeUnitSet.show(f);
        statTypeSet.show(f);
        NTimeIntervals.show(f);
        processId.show(f);
      }
    }

    private void doPdsSummaryIndexed(Formatter fm, MFile ff) throws IOException {
      String path = ff.getPath();

      //make sure indexes exist
      GribIndex index = new GribIndex();
      if (!index.readIndex(path, ff.getLastModified()))
        index.makeIndex(path, fm);
      GribCollection gc = GribCollectionBuilder.createFromSingleFile(new File(path), fm);
      gc.close();

      GridDataset ncfile = null;
      try {
        ncfile = GridDataset.open(path + GribCollection.IDX_EXT);
        for (GridDatatype dt : ncfile.getGrids()) {
          String currName = dt.getName();
          total++;

          Attribute att = dt.findAttributeIgnoreCase("Grib_Probability_Type");
          if (att != null) {
            fm.format("  %s (PROB) desc=%s %n", currName, dt.getDescription());
            prob++;
          }

          att = dt.findAttributeIgnoreCase("Grib_Statistical_Interval_Type");
          if (att != null) {
            int statType = att.getNumericValue().intValue();
            if ((statType == 7) || (statType == 9)) {
              fm.format("  %s (STAT type %s) desc=%s %n", currName, statType, dt.getDescription());
              prob++;
            }
          }

          att = dt.findAttributeIgnoreCase("Grib_Ensemble_Derived_Type");
          if (att != null) {
            int type = att.getNumericValue().intValue();
            if ((type > 9)) {
              fm.format("  %s (DERIVED type %s) desc=%s %n", currName, type, dt.getDescription());
              prob++;
            }
          }

        }
      } catch (Throwable ioe) {
        fm.format("Failed on %s == %s%n", path, ioe.getMessage());
        System.out.printf("Failed on %s%n", path);
        ioe.printStackTrace();

      } finally {
        if (ncfile != null) ncfile.close();
      }
    }

    private void doPdsSummary(Formatter f, MFile mf, Counter templateSet, Counter timeUnitSet, Counter statTypeSet, Counter NTimeIntervals,
                              Counter processId) throws IOException {
      String path = mf.getPath();
      GribIndex index = new GribIndex();
      if (!index.readIndex(path, mf.getLastModified()))
        index.makeIndex(path, f);

      for (ucar.nc2.grib.grib2.Grib2Record gr : index.getRecords()) {
        Grib2Pds pds = gr.getPDS();
        templateSet.count(pds.getTemplateNumber());
        timeUnitSet.count(pds.getTimeUnit());
        processId.count(pds.getGenProcessId());

        if (pds instanceof Grib2Pds.PdsInterval) {
          Grib2Pds.PdsInterval pdsi = (Grib2Pds.PdsInterval) pds;
          for (Grib2Pds.TimeInterval ti : pdsi.getTimeIntervals())
            statTypeSet.count(ti.statProcessType);
          NTimeIntervals.count(pdsi.getTimeIntervals().length);
        }
      }
    }

    ///////////////////////////////////////////////

    private void doIdProblems(Formatter f, CollectionManager dcm, boolean useIndex) throws IOException {
      f.format("Look for ID Problems%n");

      Counter disciplineSet = new Counter("discipline");
      Counter masterTable = new Counter("masterTable");
      Counter localTable = new Counter("localTable");
      Counter centerId = new Counter("centerId");
      Counter subcenterId = new Counter("subcenterId");
      Counter genProcess = new Counter("genProcess");
      Counter backProcess = new Counter("backProcess");

      for (MFile mfile : dcm.getFiles()) {
        f.format(" %s%n", mfile.getPath());
        doIdProblems(f, mfile, useIndex,
                disciplineSet, masterTable, localTable, centerId, subcenterId, genProcess, backProcess);
      }

      disciplineSet.show(f);
      masterTable.show(f);
      localTable.show(f);
      centerId.show(f);
      subcenterId.show(f);
      genProcess.show(f);
      backProcess.show(f);
    }

    private void doIdProblems(Formatter f, MFile mf, boolean showProblems,
                              Counter disciplineSet, Counter masterTable, Counter localTable, Counter centerId,
                              Counter subcenterId, Counter genProcessC, Counter backProcessC) throws IOException {
      String path = mf.getPath();
      GribIndex index = new GribIndex();
      if (!index.readIndex(path, mf.getLastModified()))
        index.makeIndex(path, f);

      // these should be the same for the entire file
      int center = -1;
      int subcenter = -1;
      int master = -1;
      int local = -1;
      int genProcess = -1;
      int backProcess = -1;

      for (ucar.nc2.grib.grib2.Grib2Record gr : index.getRecords()) {
        disciplineSet.count(gr.getDiscipline());
        masterTable.count(gr.getId().getMaster_table_version());
        localTable.count(gr.getId().getLocal_table_version());
        centerId.count(gr.getId().getCenter_id());
        subcenterId.count(gr.getId().getSubcenter_id());
        genProcessC.count(gr.getPDS().getGenProcessId());
        backProcessC.count(gr.getPDS().getBackProcessId());

        if (!showProblems) continue;

        if (gr.getDiscipline() == 255) {
          f.format("  bad discipline= ");
          gr.show(f);
          f.format("%n");
        }

        int val = gr.getId().getCenter_id();
        if (center < 0) center = val;
        else if (center != val) {
          f.format("  center %d != %d ", center, val);
          gr.show(f);
          f.format(" %s%n", gr.getId());
        }

        val = gr.getId().getSubcenter_id();
        if (subcenter < 0) subcenter = val;
        else if (subcenter != val) {
          f.format("  subcenter %d != %d ", subcenter, val);
          gr.show(f);
          f.format(" %s%n", gr.getId());
        }

        val = gr.getId().getMaster_table_version();
        if (master < 0) master = val;
        else if (master != val) {
          f.format("  master %d != %d ", master, val);
          gr.show(f);
          f.format(" %s%n", gr.getId());
        }

        val = gr.getId().getLocal_table_version();
        if (local < 0) local = val;
        else if (local != val) {
          f.format("  local %d != %d ", local, val);
          gr.show(f);
          f.format(" %s%n", gr.getId());
        }

        val = gr.getPDS().getGenProcessId();
        if (genProcess < 0) genProcess = val;
        else if (genProcess != val) {
          f.format("  genProcess %d != %d ", genProcess, val);
          gr.show(f);
          f.format(" %s%n", gr.getId());
        }

        val = gr.getPDS().getBackProcessId();
        if (backProcess < 0) backProcess = val;
        else if (backProcess != val) {
          f.format("  backProcess %d != %d ", backProcess, val);
          gr.show(f);
          f.format(" %s%n", gr.getId());
        }

      }
    }

    ///////////////////////////////////////////////

    private void doDrsSummary(Formatter f, CollectionManager dcm, boolean useIndex) throws IOException {
      f.format("Show Unique DRS Templates%n");
      Counter template = new Counter("DRS template");
      Counter prob = new Counter("DRS template 40 signed problem");

      for (MFile mfile : dcm.getFiles()) {
        f.format(" %s%n", mfile.getPath());
        doDrsSummary(f, mfile, useIndex, template, prob);
      }

      template.show(f);
      prob.show(f);
    }

    private void doDrsSummary(Formatter f, MFile mf, boolean useIndex, Counter templateC, Counter probC) throws IOException {
      String path = mf.getPath();
      GribIndex index = new GribIndex();
      if (!index.readIndex(path, mf.getLastModified()))
        index.makeIndex(path, f);
      RandomAccessFile raf = new RandomAccessFile(path, "r");

      for (ucar.nc2.grib.grib2.Grib2Record gr : index.getRecords()) {
        Grib2SectionDataRepresentation drss = gr.getDataRepresentationSection();
        int template = drss.getDataTemplate();
        templateC.count(template);

        if (useIndex && template == 40) {  // expensive
          Grib2Drs.Type40 drs40 = gr.readDataTest(raf);
          if (drs40 != null) {
            if (drs40.hasSignedProblem())
              probC.count(1);
            else
              probC.count(0);
          }
        }
      }
      raf.close();
    }

    ///////////////////////////////////////////////

    private void doGdsTemplate(Formatter f, CollectionManager dcm, boolean useIndex) throws IOException {
      f.format("Show Unique GDS Templates%n");

      Map<Integer, Integer> drsSet = new HashMap<Integer, Integer>();
      for (MFile mfile : dcm.getFiles()) {
        f.format(" %s%n", mfile.getPath());
        doGdsTemplate(f, mfile, drsSet);
      }

      for (int template : drsSet.keySet()) {
        int count = drsSet.get(template);
        f.format("%nGDS template = %d count = %d%n", template, count);
      }
    }

    private void doGdsTemplate(Formatter f, MFile mf, Map<Integer, Integer> gdsSet) throws IOException {
      String path = mf.getPath();
      GribIndex index = new GribIndex();
      if (!index.readIndex(path, mf.getLastModified()))
        index.makeIndex(path, f);

      for (Grib2SectionGridDefinition gds : index.getGds()) {
        int template = gds.getGDSTemplateNumber();
        Integer count = gdsSet.get(template);
        if (count == null)
          gdsSet.put(template, 1);
        else
          gdsSet.put(template, count + 1);
      }
    }   */

}

