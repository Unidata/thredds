package ucar.nc2.ui;

import thredds.featurecollection.FeatureCollectionConfig;
import thredds.inventory.MFile;
import ucar.coord.*;
import ucar.nc2.grib.collection.*;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.util.Indent;
import ucar.nc2.ui.widget.BAMutil;
import ucar.nc2.ui.widget.IndependentWindow;
import ucar.nc2.ui.widget.PopupMenu;
import ucar.nc2.ui.widget.TextHistoryPane;
import ucar.util.prefs.PreferencesExt;
import ucar.util.prefs.ui.BeanTable;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.List;

/**
 * Read ncx3 index files.
 *
 * @author John
 * @since 12/5/13
 */
public class CdmIndex3Panel extends JPanel {
  static private final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(CdmIndex3Panel.class);

  private PreferencesExt prefs;

  private BeanTable groupTable, varTable, coordTable;
  private JSplitPane split, split2, split3;

  private TextHistoryPane infoTA;
  private IndependentWindow infoWindow;
  private MFileTable fileTable;

  public CdmIndex3Panel(PreferencesExt prefs, JPanel buttPanel) {
    this.prefs = prefs;

    if (buttPanel != null) {
      AbstractButton infoButton = BAMutil.makeButtcon("Information", "Show Info", false);
      infoButton.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          Formatter f = new Formatter();
          showInfo(f);
          infoTA.setText(f.toString());
          infoTA.gotoTop();
          infoWindow.show();
        }
      });
      buttPanel.add(infoButton);

      AbstractButton filesButton = BAMutil.makeButtcon("catalog", "Show Files", false);
      filesButton.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          if (gc != null)
            showFileTable(gc, null);
        }
      });
      buttPanel.add(filesButton);

      AbstractButton rawButton = BAMutil.makeButtcon("TableAppearence", "Estimate memory use", false);
      rawButton.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          Formatter f = new Formatter();
          showMemoryEst(f);
          infoTA.setText(f.toString());
          infoTA.gotoTop();
          infoWindow.show();
        }
      });
      buttPanel.add(rawButton);


      AbstractButton checkAllButton = BAMutil.makeButtcon("Select", "Check entire file", false);
      rawButton.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          Formatter f = new Formatter();
          checkAll(f);
          infoTA.setText(f.toString());
          infoTA.gotoTop();
          infoWindow.show();
        }
      });
      buttPanel.add(checkAllButton);

    }

    ////////////////////////////

    PopupMenu varPopup;

    ////////////////
    groupTable = new BeanTable(GroupBean.class, (PreferencesExt) prefs.node("GroupBean"), false, "GDS group", "GribCollectionImmutable.GroupHcs", null);
    groupTable.addListSelectionListener(new ListSelectionListener() {
      public void valueChanged(ListSelectionEvent e) {
        GroupBean bean = (GroupBean) groupTable.getSelectedBean();
        if (bean != null)
          setGroup(bean);
      }
    });

    varPopup = new PopupMenu(groupTable.getJTable(), "Options");
    varPopup.addAction("Show Group Info", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        GroupBean bean = (GroupBean) groupTable.getSelectedBean();
        if (bean != null && bean.group != null) {
          Formatter f = new Formatter();
          bean.group.show(f);
          infoTA.setText(f.toString());
          infoTA.gotoTop();
          infoWindow.show();
        }
      }
    });
    varPopup.addAction("Show Files Used", new AbstractAction() {
       public void actionPerformed(ActionEvent e) {
         GroupBean bean = (GroupBean) groupTable.getSelectedBean();
         if (bean != null && bean.group != null) {
           showFileTable(gc, bean.group);
         }
       }
     });

    varTable = new BeanTable(VarBean.class, (PreferencesExt) prefs.node("Grib2Bean"), false, "Variables in group", "GribCollectionImmutable.VariableIndex", null);

    varPopup = new PopupMenu(varTable.getJTable(), "Options");
    varPopup.addAction("Show Variable(s)", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        List<VarBean> beans = varTable.getSelectedBeans();
        infoTA.clear();
        for (VarBean bean : beans)
          infoTA.appendLine(bean.v.toStringFrom());
        infoTA.gotoTop();
        infoWindow.show();
      }
    });
    varPopup.addAction("Show Sparse Array", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        VarBean bean = (VarBean) varTable.getSelectedBean();
        if (bean != null) {
          Formatter f = new Formatter();
          bean.showSparseArray(f);
          infoTA.setText(f.toString());
          infoTA.gotoTop();
          infoWindow.show();
        }
      }
    });
    varPopup.addAction("Make Variable(s) GribConfig", new AbstractAction() {
       public void actionPerformed(ActionEvent e) {
         List<VarBean> beans = varTable.getSelectedBeans();
         infoTA.clear();
         Formatter f = new Formatter();
         for (VarBean bean : beans)
           bean.makeGribConfig(f);
         infoTA.appendLine(f.toString());
         infoTA.gotoTop();
         infoWindow.show();
       }
     });


    coordTable = new BeanTable(CoordBean.class, (PreferencesExt) prefs.node("CoordBean"), false, "Coordinates in group", "Coordinates", null);
    varPopup = new PopupMenu(coordTable.getJTable(), "Options");

    varPopup.addAction("Show", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        CoordBean bean = (CoordBean) coordTable.getSelectedBean();
        if (bean != null) {
          Formatter f = new Formatter();
          bean.coord.showCoords(f);
          infoTA.setText(f.toString());
          infoTA.gotoTop();
          infoWindow.show();
        }
      }
    });

    varPopup.addAction("ShowCompact", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        CoordBean bean = (CoordBean) coordTable.getSelectedBean();
        if (bean != null) {
          Formatter f = new Formatter();
          bean.coord.showInfo(f, new Indent(2));
          infoTA.setText(f.toString());
          infoTA.gotoTop();
          infoWindow.show();
        }
      }
    });

    varPopup.addAction("Test Time2D isRegular", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        CoordBean bean = (CoordBean) coordTable.getSelectedBean();
        if (bean != null) {
          Formatter f = new Formatter();
          testOrthogonal(f, bean.coord);
          testRegular(f, bean.coord);
          infoTA.setText(f.toString());
          infoTA.gotoTop();
          infoWindow.show();
        }
      }
    });

    varPopup.addAction("Compare", new AbstractAction() {
       public void actionPerformed(ActionEvent e) {
         List beans = coordTable.getSelectedBeans();
         if (beans.size() == 2) {
           Formatter f = new Formatter();
           CoordBean bean1 = (CoordBean) beans.get(0);
           CoordBean bean2 = (CoordBean) beans.get(1);
           if (bean1.coord.getType() == Coordinate.Type.time2D && bean2.coord.getType() == Coordinate.Type.time2D)
             compareCoords2D(f, (CoordinateTime2D) bean1.coord, (CoordinateTime2D) bean2.coord);
           else
             compareCoords(f, bean1.coord, bean2.coord);
           infoTA.setText(f.toString());
           infoTA.gotoTop();
           infoWindow.show();
         }
       }
     });

    varPopup.addAction("Try to Merge", new AbstractAction() {
       public void actionPerformed(ActionEvent e) {
         List beans = coordTable.getSelectedBeans();
         if (beans.size() == 2) {
           Formatter f = new Formatter();
           CoordBean bean1 = (CoordBean) beans.get(0);
           CoordBean bean2 = (CoordBean) beans.get(1);
           if (bean1.coord.getType() == Coordinate.Type.time2D && bean2.coord.getType() == Coordinate.Type.time2D)
             mergeCoords2D(f, (CoordinateTime2D) bean1.coord, (CoordinateTime2D) bean2.coord);
           else
             f.format("CoordinateTime2D only");
           infoTA.setText(f.toString());
           infoTA.gotoTop();
           infoWindow.show();
         }
       }
     });

    // file popup window
    fileTable = new MFileTable((PreferencesExt) prefs.node("MFileTable"), true);
    fileTable.addPropertyChangeListener(new PropertyChangeListener() {
      @Override
      public void propertyChange(PropertyChangeEvent evt) {
        firePropertyChange(evt.getPropertyName(), evt.getOldValue(), evt.getNewValue());
      }
    });

    /////////////////////////////////////////
    // the info windows
    infoTA = new TextHistoryPane();
    infoWindow = new IndependentWindow("Extra Information", BAMutil.getImage("netcdfUI"), infoTA);
    infoWindow.setBounds((Rectangle) prefs.getBean("InfoWindowBounds", new Rectangle(300, 300, 500, 300)));

    setLayout(new BorderLayout());

    split3 = new JSplitPane(JSplitPane.VERTICAL_SPLIT, false, groupTable, varTable);
    split3.setDividerLocation(prefs.getInt("splitPos3", 800));

    split2 = new JSplitPane(JSplitPane.VERTICAL_SPLIT, false, split3, coordTable);
    split2.setDividerLocation(prefs.getInt("splitPos2", 800));

    //split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, false, split2, vertCoordTable);
    //split.setDividerLocation(prefs.getInt("splitPos", 500));

    add(split2, BorderLayout.CENTER);
  }

  public void save() {
    groupTable.saveState(false);
    varTable.saveState(false);
    coordTable.saveState(false);
    fileTable.save();
    prefs.putBeanObject("InfoWindowBounds", infoWindow.getBounds());
    if (split != null) prefs.putInt("splitPos", split.getDividerLocation());
    if (split2 != null) prefs.putInt("splitPos2", split2.getDividerLocation());
    if (split3 != null) prefs.putInt("splitPos3", split3.getDividerLocation());
  }

  public void clear() {
    if (gc != null) {
      try {
        gc.close();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
    gc = null;
    groupTable.clearBeans();
    varTable.clearBeans();
    coordTable.clearBeans();
  }

  public void showInfo(Formatter f) {
    if (gc == null) return;
    gc.showIndex(f);
    f.format("%n");

    f.format("Groups%n");
    List<GroupBean> groups = groupTable.getBeans();
    for (GroupBean bean : groups) {
      f.format("%-50s %-50s %d%n", bean.getGroupId(), bean.getDescription(), bean.getGdsHash());
      bean.group.show(f);
    }

    f.format("%n");
    // showFiles(f);
  }

  private void showFileTable(GribCollectionImmutable gc, GribCollectionImmutable.GroupGC group) {
    File dir = gc.getDirectory();
    Collection<MFile> files = (group == null) ? gc.getFiles() : group.getFiles();
    fileTable.setFiles(dir, files);
  }

  private static class SortBySize implements Comparable<SortBySize> {
    Object obj;
    int size;

    private SortBySize(Object obj, int size) {
      this.obj = obj;
      this.size = size;
    }

    public int compareTo(SortBySize o) {
      return Integer.compare(size, o.size);
    }
  }


  private void checkAll(Formatter f) {
    if (gc == null) return;
    for (GribCollectionImmutable.Dataset ds : gc.getDatasets()) {
      f.format("Dataset %s%n", ds.getType());
      int bytesTotal = 0;
      int bytesSATotal = 0;
      int coordsAllTotal = 0;

      for (GribCollectionImmutable.GroupGC g : ds.getGroups()) {
        f.format(" Group %s%n", g.getDescription());

        int count = 0;
        for (GribCollectionImmutable.VariableIndex v : g.getVariables()) {
          VarBean bean = new VarBean(v, g);

          if (v instanceof PartitionCollectionImmutable.VariableIndexPartitioned) {
            if (count == 0) f.format(" total   VariablePartitioned%n");

            PartitionCollectionImmutable.VariableIndexPartitioned vip = (PartitionCollectionImmutable.VariableIndexPartitioned) v;
             int nparts = vip.getNparts();
             int memEstBytes = 368 + nparts * 4;  // very rough
             bytesTotal += memEstBytes;
             f.format("%6d %-50s nparts=%6d%n", memEstBytes, bean.getName(), nparts);

          } else {
            if (count == 0) f.format(" total   SA  Variable%n");
            try {
              v.readRecords();
              SparseArray<GribCollectionImmutable.Record> sa = v.getSparseArray();
              int ntracks  = sa.getTotalSize();
              int nrecords = sa.getContent().size();
              int memEstForSA = 276 + nrecords * 40 + ntracks * 4;
              int memEstBytes = 280 + memEstForSA;
              f.format("%6d %6d %-50s nrecords=%6d%n", memEstBytes, memEstForSA, bean.getName(), nrecords);
              bytesTotal += memEstBytes;
              bytesSATotal += memEstForSA;

            } catch (IOException e) {
              e.printStackTrace();
            }
          }
          count++;
        }
      }
      int noSA =  bytesTotal - bytesSATotal;
      f.format("%n total KBytes=%d kbSATotal=%d kbNoSA=%d coordsAllTotal=%d%n", bytesTotal/1000, bytesSATotal/1000, noSA/1000, coordsAllTotal/1000);
      f.format("%n");
    }
  }

  private void showMemoryEst(Formatter f) {
    if (gc == null) return;

    for (GribCollectionImmutable.Dataset ds : gc.getDatasets()) {
      f.format("Dataset %s%n", ds.getType());
      int bytesTotal = 0;
      int bytesSATotal = 0;
      int coordsAllTotal = 0;

      for (GribCollectionImmutable.GroupGC g : ds.getGroups()) {
        f.format(" Group %s%n", g.getDescription());

        List<SortBySize> sortList = new ArrayList<>(g.getCoordinates().size());
        for (Coordinate vc : g.getCoordinates())
           sortList.add(new SortBySize(vc, vc.estMemorySize()));
        Collections.sort(sortList);

        int coordsTotal = 0;
        f.format("  totalKB  type Coordinate%n");
        for (SortBySize ss : sortList) {
          Coordinate vc = (Coordinate) ss.obj;
          f.format("  %6d %-8s %-40s%n", vc.estMemorySize()/1000, vc.getType(), vc.getName());
          bytesTotal += vc.estMemorySize();
          coordsTotal += vc.estMemorySize();
        }
        f.format(" %7d KBytes%n", coordsTotal/1000);
        f.format("%n");
        coordsAllTotal += coordsTotal;

        int count = 0;
        for (GribCollectionImmutable.VariableIndex v : g.getVariables()) {
          VarBean bean = new VarBean(v, g);

          if (v instanceof PartitionCollectionImmutable.VariableIndexPartitioned) {
            if (count == 0) f.format(" total   VariablePartitioned%n");

            PartitionCollectionImmutable.VariableIndexPartitioned vip = (PartitionCollectionImmutable.VariableIndexPartitioned) v;
             int nparts = vip.getNparts();
             int memEstBytes = 368 + nparts * 4;  // very rough
             bytesTotal += memEstBytes;
             f.format("%6d %-50s nparts=%6d%n", memEstBytes, bean.getName(), nparts);

          } else {
            if (count == 0) f.format(" total   SA  Variable%n");
            try {
              v.readRecords();
              SparseArray<GribCollectionImmutable.Record> sa = v.getSparseArray();
              int ntracks  = sa.getTotalSize();
              int nrecords = sa.getContent().size();
              int memEstForSA = 276 + nrecords * 40 + ntracks * 4;
              int memEstBytes = 280 + memEstForSA;
              f.format("%6d %6d %-50s nrecords=%6d%n", memEstBytes, memEstForSA, bean.getName(), nrecords);
              bytesTotal += memEstBytes;
              bytesSATotal += memEstForSA;

            } catch (IOException e) {
              e.printStackTrace();
            }
          }
          count++;
        }
      }
      int noSA =  bytesTotal - bytesSATotal;
      f.format("%n total KBytes=%d kbSATotal=%d kbNoSA=%d coordsAllTotal=%d%n", bytesTotal/1000, bytesSATotal/1000, noSA/1000, coordsAllTotal/1000);
      f.format("%n");
    }
  }

  private void compareCoords(Formatter f, Coordinate coord1, Coordinate coord2) {
    List<? extends Object> vals1 = coord1.getValues();
    List<? extends Object> vals2 = coord2.getValues();
    f.format("Coordinate %s%n", coord1.getName());
    for (Object val1 : vals1) {
      boolean missing = (!vals2.contains(val1));
      f.format(" %s %s%n", val1, (missing ? "MISSING IN 2" : ""));
    }
    f.format("%nCoordinate %s%n", coord2.getName());
    for (Object val2 : vals2) {
      boolean missing = (!vals1.contains(val2));
      f.format(" %s %s%n", val2, (missing ? "MISSING IN 1" : ""));
    }
  }

  private void compareCoords2D(Formatter f, CoordinateTime2D coord1, CoordinateTime2D coord2) {
    CoordinateRuntime runtimes1 = coord1.getRuntimeCoordinate();
    CoordinateRuntime runtimes2 = coord2.getRuntimeCoordinate();
    int n1 = coord1.getNruns();
    int n2 = coord2.getNruns();
    if (n1 != n2) {
      f.format("Coordinate 1 has %d runtimes, Coordinate 2 has %d runtimes, %n", n1, n2);     }

    int min = Math.min(n1, n2);
    for (int idx=0; idx<min; idx++) {
      CoordinateTimeAbstract time1 = coord1.getTimeCoordinate(idx);
      CoordinateTimeAbstract time2 = coord2.getTimeCoordinate(idx);
      f.format("Run %d %n", idx);
      if (!runtimes1.getValue(idx).equals(runtimes2.getValue(idx)))
        f.format("Runtime 1 %s != %s runtime 2%n", runtimes1.getValue(idx), runtimes2.getValue(idx));
      compareCoords(f, time1, time2);
    }
  }

  private void mergeCoords2D(Formatter f, CoordinateTime2D coord1, CoordinateTime2D coord2) {
    if (coord1.isTimeInterval() != coord2.isTimeInterval()) {
      f.format("Coordinate 1 isTimeInterval %s != Coordinate 2 isTimeInterval %s %n", coord1.isTimeInterval(), coord2.isTimeInterval());
      return;
    }

    CoordinateRuntime runtimes1 = coord1.getRuntimeCoordinate();
    CoordinateRuntime runtimes2 = coord2.getRuntimeCoordinate();
    int n1 = coord1.getNruns();
    int n2 = coord2.getNruns();
    if (n1 != n2) {
      f.format("Coordinate 1 has %d runtimes, Coordinate 2 has %d runtimes, %n", n1, n2);
    }

    int min = Math.min(n1, n2);
     for (int idx=0; idx<min; idx++) {
      if (!runtimes1.getValue(idx).equals(runtimes2.getValue(idx)))
        f.format("Runtime 1 %s != %s runtime 2%n", runtimes1.getValue(idx), runtimes2.getValue(idx));
    }

    Set<Object> set1 = makeCoordSet(coord1);
    List<? extends Object> list1 = coord1.getOffsetsSorted();
    Set<Object> set2 = makeCoordSet(coord2);
    List<? extends Object> list2 = coord2.getOffsetsSorted();

    f.format("%nCoordinate %s%n", coord1.getName());
    for (Object val : list1) f.format(" %s,", val);
    f.format(" n=(%d)%n", list1.size());
    testMissing(f, list1, set2);

    f.format("%nCoordinate %s%n", coord2.getName());
    for (Object val : list2) f.format(" %s,", val);
    f.format(" (n=%d)%n", list2.size());
    testMissing(f, list2, set1);
  }

  private Set<Object> makeCoordSet(CoordinateTime2D time2D) {
    Set<Object> result = new HashSet<>(100);
    for (int runIdx=0; runIdx<time2D.getNruns(); runIdx++) {
      Coordinate coord = time2D.getTimeCoordinate(runIdx);
      for (Object val : coord.getValues())
        result.add(val);
    }
    return result;
  }

  private void testMissing(Formatter f, List<? extends Object> test, Set<Object> against) {
    int countMissing = 0;
    for (Object val1 : test) {
      if (!against.contains(val1))
        f.format(" %d: %s MISSING%n", countMissing++, val1);
    }
    f.format("TOTAL MISSING %s%n", countMissing);
  }

  // orthogonal means that all the times can be made into a single time coordinate
  private boolean testOrthogonal(Formatter f, Coordinate c) {
    if (!(c instanceof CoordinateTime2D)) {
      f.format("Must be CoordinateTime2D");
      return false;
    }
    CoordinateTime2D time2D = (CoordinateTime2D) c;
    List<CoordinateTimeAbstract> coords = new ArrayList<>();
    for (int runIdx=0; runIdx<time2D.getNruns(); runIdx++) {
      coords.add(time2D.getTimeCoordinate(runIdx));
    }
    return testOrthogonal(f, coords);
  }

  // regular means that all the times for each offset from 0Z can be made into a single time coordinate (FMRC algo)
  private boolean testRegular(Formatter f, Coordinate c) {
    if (!(c instanceof CoordinateTime2D)) {
      f.format("Must be CoordinateTime2D");
      return false;
    }
    f.format("Test isRegular by Offset Hour%n");
    CoordinateTime2D time2D = (CoordinateTime2D) c;

    // group time coords by offset hour
    Map<Integer, List<CoordinateTimeAbstract>> hourMap = new HashMap<>();
    for (int runIdx=0; runIdx<time2D.getNruns(); runIdx++) {
      CoordinateTimeAbstract coord = time2D.getTimeCoordinate(runIdx);
      CalendarDate runDate = coord.getRefDate();
      int hour = runDate.getHourOfDay();
      List<CoordinateTimeAbstract> hg = hourMap.get(hour);
      if (hg == null) {
        hg = new ArrayList<>();
        hourMap.put(hour, hg);
      }
      hg.add(coord);
    }

    // see if each offset hour is orthogonal
    boolean ok = true;
    for (int hour : hourMap.keySet()) {
      List<CoordinateTimeAbstract> hg = hourMap.get(hour);
      f.format("Hour %d: ", hour);
      for (CoordinateTimeAbstract coord : hg) f.format("%s,", coord.getRefDate());
      f.format("%n");
      ok &= testOrthogonal(f, hg);
    }
    f.format("%nAll orthogonal: %s%n", ok);
    return ok;
  }

  private boolean testOrthogonal(Formatter f, List<CoordinateTimeAbstract> times) {
    int max = 0;
    Set<Object> allCoords = new HashSet<>(100);
    for (CoordinateTimeAbstract coord : times) {
      max = Math.max(max, coord.getSize());

      for (Object val : coord.getValues())
        allCoords.add(val);
    }

    // is the set of all values the same as the component times?
    int totalMax = allCoords.size();
    boolean isOrthogonal = (totalMax == max);
    f.format("isOrthogonal %s : totalMax = %d max=%d %n%n", isOrthogonal, totalMax, max);
    return isOrthogonal;
  }


  /* private void compareFiles(Formatter f) throws IOException {
    if (gc == null) return;
    List<String> canon = new ArrayList<>(gc.getFilenames());
    Collections.sort(canon);

    File idxFile = new File(gc.getIndexFilepathInCache());
    File dir = idxFile.getParentFile();
    File[] files = dir.listFiles(new FilenameFilter() {
      public boolean accept(File dir, String name) {
        return name.endsWith(".ncx");
      }
    });

    if (files == null) {
      // File.listFiles() returns null instead of throwing an exception. Dumb.
      throw new RuntimeException(String.format("Either an I/O error occurred, or \"%s\" is not a directory.", dir));
    }

    int total = 0;
    for (File file : files) {
      RandomAccessFile raf = new RandomAccessFile(file.getPath(), "r");
      GribCollectionImmutable cgc = Grib2CollectionBuilderFromIndex.readFromIndex(file.getName(), raf, null, false, logger);
      List<String> cfiles = new ArrayList<>(cgc.getFilenames());
      Collections.sort(cfiles);
      f.format("Compare files in %s to canonical files in %s%n", file.getPath(), idxFile.getPath());
      compareSortedList(f, canon.iterator(), cfiles.iterator());
      f.format("  Compared %d files to %d files%n%n", cfiles.size(), canon.size());
      raf.close();
      total += cfiles.size();
    }
    f.format("Total files = %d%n%n", total);
  } */

  private void compareSortedList(Formatter f, Iterator<String> i1, Iterator<String> i2) {
    String s1 = null, s2 = null;
    boolean need1 = true, need2 = true;
    while (true) {
      if (need1)
        s1 = (i1.hasNext()) ? i1.next() : null;
      if (need2)
        s2 = (i2.hasNext()) ? i2.next() : null;

      need1 = true;
      need2 = true;

      if (s1 == null && s2 == null) break;
      if (s1 == null) f.format(" extra file = %s%n", s2);
      else if (s2 == null) f.format(" missing file = %s%n", s1);
      else {
        int pos = s1.lastIndexOf("/");
        String name1 = s1.substring(pos);
        name1 = name1.substring(1, name1.indexOf("gdas"));
        String name2 = s2.substring(pos);
        name2 = name2.substring(1, name2.indexOf("gdas"));
        int val = name1.compareTo(name2);
        if (val < 0) { // s1 < s2
          f.format(" missing file = %s%n", s1);
          need2 = false;
        } else if (val > 0) {
          f.format(" extra file = %s%n", s2);
          need1 = false;
        }
      }

    }
  }

  ///////////////////////////////////////////////
  Path indexFile;
  GribCollectionImmutable gc;
  Collection<MFile> gcFiles;
  FeatureCollectionConfig config = new FeatureCollectionConfig();

  public void setIndexFile(Path indexFile, FeatureCollectionConfig config) throws IOException {
    if (gc != null) gc.close();

    this.indexFile = indexFile;
    this.config = config;
    gc = GribCdmIndex.openCdmIndex(indexFile.toString(), config, false, logger);
    if (gc == null)
      throw new IOException("Not a grib collection index file");

    List<GroupBean> groups = new ArrayList<>();

    for (GribCollectionImmutable.Dataset ds : gc.getDatasets())
      for (GribCollectionImmutable.GroupGC g : ds.getGroups())
        groups.add(new GroupBean(g, ds.getType().toString()));

    if (groups.size() > 0)
      setGroup(groups.get(0));
    else {
      varTable.clearBeans();
      coordTable.clearBeans();
    }

    groupTable.setBeans(groups);
    groupTable.setHeader(indexFile.toString());
    gcFiles = gc.getFiles();
  }

  private void setGroup(GroupBean bean) {
    bean.clear();
    List<VarBean> vars = new ArrayList<>();
    for (GribCollectionImmutable.VariableIndex v : bean.group.getVariables()) {
      VarBean vbean = new VarBean(v, bean.group);
      vars.add(vbean);
      bean.nrecords += vbean.getNrecords();
      bean.ndups += vbean.getNdups();
      bean.nmissing += vbean.getNmissing();
    }
    varTable.setBeans(vars);

    int count = 0;
    List<CoordBean> coords = new ArrayList<>();
    for (Coordinate vc : bean.group.getCoordinates())
      coords.add(new CoordBean(vc, count++));
    coordTable.setBeans(coords);
  }

  /* private void showFiles(Formatter f) {
    if (gc == null) return;
    if (gc.getFiles() != null) {
      List<MFile> fs = new ArrayList<>(gc.getFiles());
      Map<MFile, Integer> map = new HashMap<>(fs.size() * 2);

      int count = 0;
      f.format("In order:%n");
      for (MFile file : fs) {
        f.format("%5d %60s lastModified=%s%n", count, file.getName(), CalendarDateFormatter.toDateTimeString(new Date(file.getLastModified())));
        map.put(file, count);
        count++;
      }

      f.format("%nsorted:%n");
      Collections.sort(fs);
      int last = -1;
      for (MFile file : fs) {
        int num = map.get(file);
        f.format("%s%5d %s%n", (num < last) ? "***" : "", num, file.getPath());
      }
    }

    if (gc instanceof PartitionCollection) {
      PartitionCollection pc = (PartitionCollection) gc;
      for (PartitionCollection.Partition part : pc.getPartitions())
        f.format("%s%n", part);
    }

    f.format("============%n%s%n", gc);
  }  */

  ////////////////////////////////////////////////////////////////////////////

  public class GroupBean {
    GribCollectionImmutable.GroupGC group;
    String type;
    int nrecords, nmissing, ndups;

    public GroupBean() {
    }

    public GroupBean(GribCollectionImmutable.GroupGC g, String type) {
      this.group = g;
      this.type = type;

      for (GribCollectionImmutable.VariableIndex vi : group.getVariables()) {
         nrecords += vi.getNrecords();
         ndups += vi.getNdups();
         nmissing += vi.getNmissing();
      }
    }

    void clear() {
      nmissing = 0;
      ndups = 0;
      nrecords = 0;
    }

    public String getGroupId() {
      return group.getId();
    }

    public int getGdsHash() {
      return group.getGdsHash().hashCode();
    }

    public int getNrecords() {
      return nrecords;
    }

    public String getType() {
      return type;
    }

    public int getNFiles() {
      int n = group.getNFiles();
      if (n == 0) {
        if (gc instanceof PartitionCollectionImmutable)
          n = ((PartitionCollectionImmutable) gc).getPartitionSize();
      }
      return n;
    }

    public int getNruntimes() {
      return group.getNruntimes();
    }

    public int getNCoords() {
      return group.getCoordinates().size();
    }

    public int getNVariables() {
      return group.getVariables().size();
    }

   public String getDescription() {
      return group.getDescription();
    }

    public int getNmissing() {
      return nmissing;
    }

    public int getNdups() {
      return ndups;
    }
  }


  public class CoordBean implements Comparable<CoordBean> {
    Coordinate coord;
    int idx;

    // no-arg constructor

    public CoordBean() {
    }

    public String getType() {
      if (coord instanceof CoordinateTime2D) {
        CoordinateTime2D c2d = (CoordinateTime2D) coord;
        Formatter f = new Formatter();
        f.format("%s %s", coord.getType(), (c2d.isTimeInterval() ? "intv" : "offs"));
        if (c2d.isOrthogonal()) f.format(" ort");
        if (c2d.isRegular()) f.format(" reg");
        return f.toString();
      } else {
        return coord.getType().toString();
      }
    }

    public CoordBean(Coordinate coord, int idx) {
      this.coord = coord;
      this.idx = idx;
    }

    public String getValues() {
      Formatter f = new Formatter();
      if (coord instanceof CoordinateRuntime) {
        CoordinateRuntime runtime = (CoordinateRuntime) coord;
        f.format("%s-%s", runtime.getFirstDate(), runtime.getLastDate());
      } else if (coord instanceof CoordinateTime2D) {
        CoordinateTime2D coord2D = (CoordinateTime2D) coord;
        CoordinateRuntime runtime = coord2D.getRuntimeCoordinate();
        f.format("%s-%s", runtime.getFirstDate(), runtime.getLastDate());
      } else {
        if (coord.getValues() == null) return "";
        for (Object val : coord.getValues()) f.format("%s,", val);
      }
      return f.toString();
    }

    public String getSize() {
      if (coord instanceof CoordinateTime2D) {
        CoordinateTime2D c2d = (CoordinateTime2D) coord;
        Formatter f = new Formatter();
        f.format("%d X %d (%d)", c2d.getRuntimeCoordinate().getSize(), c2d.getNtimes(), coord.getSize());
        return f.toString();
      } else {
        return Integer.toString(coord.getSize());
      }
    }

    public int getCode() {
      return coord.getCode();
    }

    public int getIndex() {
      return idx;
    }

    public String getUnit() {
      return coord.getUnit();
    }

    public String getName() {
      String intvName = null;
      if (coord instanceof CoordinateTimeIntv) {
        CoordinateTimeIntv timeiCoord = (CoordinateTimeIntv) coord;
        intvName = timeiCoord.getTimeIntervalName();
      }
      if (coord instanceof CoordinateTime2D) {
        CoordinateTime2D timeiCoord = (CoordinateTime2D) coord;
        intvName = timeiCoord.getTimeIntervalName();
      }

      return (intvName == null) ? coord.getName() : coord.getName() + " (" + intvName+ ")";
    }

    @Override
    public int compareTo(CoordBean o) {
      return getType().compareTo(o.getType());
    }

    void showCoords(Formatter f) {
      coord.showCoords(f);
    }
  }

  ////////////////////////////////////////////////////////////////////////////////////

  public class VarBean {
    GribCollectionImmutable.VariableIndex v;
    GribCollectionImmutable.GroupGC group;
    String name;

    public VarBean() {
    }

    public VarBean(GribCollectionImmutable.VariableIndex vindex, GribCollectionImmutable.GroupGC group) {
      this.v = vindex;
      this.group = group;
      this.name =  vindex.makeVariableName();
    }

    /* public int getNRecords() {
      return v.nrecords;
    }

    public int getNMissing() {
      return v.missing;
    }

    public int getNDups() {
      return v.ndups;
    }

    public float getDensity() {
      return v.density;
    }  */

    public String getIndexes() {
      Formatter f = new Formatter();
      for (int idx : v.getCoordinateIndex()) f.format("%d,", idx);
      return f.toString();
    }

    public String getIntvName() {
      return v.getIntvName();
    }

    public int getCdmHash() {
      return v.hashCode();
    }

    public String getGroupId() {
      return group.getId();
    }

    public String getVariableId() {
      return v.getDiscipline() + "-" + v.getCategory() + "-" + v.getParameter();
    }

    public String getName() {
      return name;
    }

    public int getNdups() {
      return v.getNdups();
    }

    public int getNrecords() {
      return v.getNrecords();
    }

    public int getNmissing() {
      int n = v.getSize();
      return n - v.getNrecords();
    }

    public int getSize() {
      return v.getSize();
    }

    public void makeGribConfig(Formatter f) {
      f.format("<variable id='%s'/>%n", getVariableId());
    }

    private void showSparseArray(Formatter f) {

      /* int count = 0;
      Indent indent = new Indent(2);
      for (Coordinate coord : v.getCoordinates()) {
        f.format("%d: ", count++);
        coord.showInfo(f, indent);
      }
      f.format("%n");  */

      if (v instanceof PartitionCollectionImmutable.VariableIndexPartitioned) {
        PartitionCollectionImmutable.VariableIndexPartitioned vip = (PartitionCollectionImmutable.VariableIndexPartitioned) v;
        vip.show(f);

      } else {
        try {
          v.readRecords();
        } catch (IOException e) {
          e.printStackTrace();
          return;
        }
        if (v.getSparseArray() != null) {
          SparseArray<GribCollectionImmutable.Record> sa = v.getSparseArray();
          sa.showInfo(f, null);
          f.format("%n");
          sa.showContent(f);
        }
      }
    }

    /* private void showRecords(Formatter f) {

       try {
         GribCollectionImmutable.Record[] records = v.getRecords();
         if (records.length == 0) {
           f.format("this index has no records%n");
           return;
         }

         f.format("%s%n%n", v.toStringComplete());
         f.format(" isTimeInterval=%s hasVert=%s%n", tcoord.isInterval(), (vcoord != null));
         f.format(" Show records (file,pos)%n");

         if (tcoord.isInterval()) {
           if (vcoord == null)
             showRecords2Dintv(f, tcoord.getIntervals(), records);
           else
             showRecords2Dintv(f, vcoord, tcoord.getIntervals(), records);
         } else {
           if (vcoord == null)
             showRecords(f, tcoord.getCoords(), records);
           else
             showRecords(f, vcoord, tcoord.getCoords(), records);
         }
       } catch (IOException ioe) {
         ioe.printStackTrace();
       }
     }

     private void showRecords(Formatter f, List<Integer> values, GribCollectionImmutable.Record[] records) throws IOException {
       f.format(" time (down)%n");

       for (int timeIdx = 0; timeIdx < v.ntimes; timeIdx++) {
         f.format("%10s = ", values.get(timeIdx));
         int idx = GribCollectionImmutable.calcIndex(timeIdx, 0, 0, v.nens, v.nverts);
         GribCollectionImmutable.Record r = records[idx];
         if (r == null) f.format("null");
         else f.format("(%d,%8d) ", r.fileno, r.pos);
         f.format("%n");
       }

       f.format("%n Show records in order%n");
       int count = 0;
       for (GribCollectionImmutable.Record r : records) {
         if (r == null) f.format("null%n");
         else f.format("%5d = (%d,%8d)%n", count, r.fileno, r.pos);
         count++;
       }
       f.format("%n");

     }

     private void showRecords(Formatter f, VertCoord vcoord, List<Integer> values, GribCollectionImmutable.Record[] records) throws IOException {
       f.format(" time (down) vertLevel (across) %n");

       f.format("%12s ", " ");
       List<VertCoord.Level> levels = vcoord.getCoords();
       boolean isLayer = vcoord.isLayer();
       for (int j = 0; j < levels.size(); j++)
         f.format("%6s ", levels.get(j).toString(isLayer));
       f.format("%n");

       for (int timeIdx = 0; timeIdx < v.ntimes; timeIdx++) {
         f.format("%10s = ", values.get(timeIdx));
         for (int vertIdx = 0; vertIdx < vcoord.getSize(); vertIdx++) {
           int idx = GribCollectionImmutable.calcIndex(timeIdx, 0, vertIdx, v.nens, v.nverts);
           GribCollectionImmutable.Record r = records[idx];
           if (r == null) f.format("null");
           else f.format("(%d,%8d) ", r.fileno, r.pos);
         }
         f.format("%n");
       }

       f.format("%n Show records in order%n");
       int count = 0;
       for (GribCollectionImmutable.Record r : records) {
         if (r == null) f.format("null%n");
         else f.format("%5d = (%d,%8d)%n", count, r.fileno, r.pos);
         count++;
       }
       f.format("%n");

     }

     void showRecords2Dintv(Formatter f, VertCoord vcoord, List<TimeCoord.Tinv> tinvs, GribCollectionImmutable.Record[] records) throws IOException {
       f.format(" timeIntv (down) vertLevel (across) %n");

       f.format("%12s ", " ");
       List<VertCoord.Level> levels = vcoord.getCoords();
       boolean isLayer = vcoord.isLayer();
       for (VertCoord.Level level : levels)
         f.format("%13s ", level.toString(isLayer));
       f.format("%n");

       for (int timeIdx = 0; timeIdx < v.ntimes; timeIdx++) {
         f.format("%10s = ", tinvs.get(timeIdx));
         Formatter filenames = new Formatter();
         for (int vertIdx = 0; vertIdx < vcoord.getSize(); vertIdx++) {
           int idx = GribCollectionImmutable.calcIndex(timeIdx, 0, vertIdx, v.nens, v.nverts);
           GribCollectionImmutable.Record r = records[idx];
           //f.format("%3d %10d ", r.fileno, r.drsPos);
           if (r == null || r.pos == 0)
             f.format("(%4d,%6d) ", -1, -1);
           else {
             f.format("(%4d,%6d) ", r.fileno, r.pos);
             filenames.format(" %s,", getFilename(r.fileno));
           }
         }
         f.format(" == %s%n", filenames);
       }
     }

    private String getFilename(int fileno) {
      MFile want = gcFiles.get(fileno);
      File wantFile = new File(gc.getDirectory(), want.getPath());
      return wantFile.getPath();
    }

     void showRecords2Dintv(Formatter f, List<TimeCoord.Tinv> tinvs, GribCollectionImmutable.Record[] records) throws IOException {
       f.format(" timeIntv (down) %n");

       for (int timeIdx = 0; timeIdx < v.ntimes; timeIdx++) {
         f.format("%10s = ", tinvs.get(timeIdx));
         int idx = GribCollectionImmutable.calcIndex(timeIdx, 0, 0, v.nens, v.nverts);
         GribCollectionImmutable.Record r = records[idx];
         //f.format("%3d %10d ", r.fileno, r.drsPos);
         f.format("%6d ", (r == null ? -1 : r.fileno));
         f.format("%n");
       }
     }

    void sendFilesToGrib2Collection() {
      try {
        Set<Integer> filenos = new HashSet<>();
        for (GribCollectionImmutable.Record r : v.getRecords()) {
          if (r != null && r.pos > 0)
            filenos.add(r.fileno);
        }
        Formatter f = new Formatter();

        f.format("list:");
        Iterator<Integer> iter = filenos.iterator();
        while (iter.hasNext()) {
          f.format("%s;", getFilename(iter.next()));
        }
        firePropertyChange("openGrib2Collection", null, f.toString());

      } catch (IOException ioe) {
        ioe.printStackTrace();
      }

    }  */

  }


 /*  private void showRecordsInPartition(Formatter f) {
    try {
      PartitionCollection.VariableIndexPartitioned vp = (PartitionCollection.VariableIndexPartitioned) v;
      showPartitionInfo(vp, f);

    } catch (IOException ioe) {
      ioe.printStackTrace();
    }
  }

   private void showPartitionInfo(PartitionCollection.VariableIndexPartitioned vP, Formatter f) throws IOException {

     PartitionCollection tp = (PartitionCollection) gc;
     tp.setPartitionIndexReletive();

     TimeCoordUnion timeCoordP = (TimeCoordUnion) vP.getTimeCoord();
     VertCoord vcoord = vP.getVertCoord();
     EnsCoord ecoord = vP.getEnsCoord();
     int ntimes = (timeCoordP == null) ? 0 : timeCoordP.getSize();
     int nverts = (vcoord == null) ? 1 : vcoord.getSize();
     int nens = (ecoord == null) ? 1 : ecoord.getSize();

     for (int timeIdx = 0; timeIdx < ntimes; timeIdx++) {

       TimeCoordUnion.Val val = timeCoordP.getVal(timeIdx);
       int partno = val.getPartition();
       GribCollectionImmutable.VariableIndex vindex = vP.getVindex(partno); // the variable in this partition
       f.format("time = %d val = %s partition = %d%n", timeIdx, val, partno);

       for (int ensIdx = 0; ensIdx < nens; ensIdx++) {
         if (nens > 1) f.format(" ens = %d%n", ensIdx);

         for (int levelIdx = 0; levelIdx <= nverts; levelIdx++) {
           if (nverts > 1) f.format("  vert = %d%n", levelIdx);

           // where does this record go in the result ??
           int resultIndex = GribCollectionImmutable.calcIndex(timeIdx, ensIdx, levelIdx, nens, nverts);

           // where does this record come from ??
           int recordIndex = -1;

           int flag = vP.flag[partno]; // see if theres a mismatch with vert or ens coordinates
           if (flag == 0) { // no problem
             recordIndex = GribCollectionImmutable.calcIndex(val.getIndex(), ensIdx, levelIdx, vindex.nens, vindex.nverts);

           } else {  // problem - must match coordinates
             recordIndex = GribCollectionImmutable.calcIndex(val.getIndex(), ensIdx, levelIdx, flag, vindex.getEnsCoord(), vindex.getVertCoord(),
                     vP.getEnsCoord(), vP.getVertCoord());
           }

           f.format("   recordIndex=%d / %d, resultIndex=%d,  flag=%d%n", recordIndex,  vindex.records.length, resultIndex, flag);
           if (flag == 0) f.format("   time=%d, ens=%d, level=%d, nens=%d, nverts=%d", val.getIndex(), ensIdx, levelIdx, vindex.nens, vindex.nverts);
           else  f.format("   time=%d, ens=%d, level=%d, flag=%d, nens=%s, vert=%s ensp=%s, vertp=%s", val.getIndex(), ensIdx, levelIdx, flag,
                   vindex.getEnsCoord(), vindex.getVertCoord(), vP.getEnsCoord(), vP.getVertCoord());

         }
       }
     }
   }   */
}
