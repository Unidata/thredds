package ucar.nc2.ui;

import thredds.featurecollection.FeatureCollectionConfig;
import thredds.inventory.MFile;
import ucar.nc2.grib.collection.*;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.time.CalendarPeriod;
import ucar.nc2.util.Indent;
import ucar.sparr.Coordinate;
import ucar.nc2.time.CalendarDateFormatter;
import ucar.nc2.ui.widget.BAMutil;
import ucar.nc2.ui.widget.IndependentWindow;
import ucar.nc2.ui.widget.PopupMenu;
import ucar.nc2.ui.widget.TextHistoryPane;
import ucar.sparr.SparseArray;
import ucar.unidata.io.RandomAccessFile;
import ucar.util.prefs.PreferencesExt;
import ucar.util.prefs.ui.BeanTableSorted;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.List;

/**
 * Read ncx2 index files.
 *
 * @author John
 * @since 12/5/13
 */
public class CdmIndex2Panel extends JPanel {
  static private final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(GribCdmIndexPanel.class);

  private PreferencesExt prefs;

  private BeanTableSorted groupTable, varTable, coordTable;
  private JSplitPane split, split2, split3;

  private TextHistoryPane infoTA;
  private IndependentWindow infoWindow;
  private MFileTable fileTable;

  public CdmIndex2Panel(PreferencesExt prefs, JPanel buttPanel) {
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


      AbstractButton filesButton = BAMutil.makeButtcon("Information", "Show Files", false);
      filesButton.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          /* Formatter f = new Formatter();
          showFiles(f);
          infoTA.setText(f.toString());
          infoTA.gotoTop();
          infoWindow.show(); */

          if (gc != null)
            showFileTable(gc, null);
        }
      });
      buttPanel.add(filesButton);
    }

    ////////////////////////////

    PopupMenu varPopup;

    ////////////////
    groupTable = new BeanTableSorted(GroupBean.class, (PreferencesExt) prefs.node("GroupBean"), false, "GDS group", "GribCollection.GroupHcs", null);
    groupTable.addListSelectionListener(new ListSelectionListener() {
      public void valueChanged(ListSelectionEvent e) {
        GroupBean bean = (GroupBean) groupTable.getSelectedBean();
        if (bean != null)
          setGroup(bean.group);
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

    varTable = new BeanTableSorted(VarBean.class, (PreferencesExt) prefs.node("Grib2Bean"), false, "Variables in group", "GribCollection.VariableIndex", null);

    varPopup = new PopupMenu(varTable.getJTable(), "Options");
    varPopup.addAction("Show Variable(s)", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        List<VarBean> beans = varTable.getSelectedBeans();
        infoTA.clear();
        for (VarBean bean : beans)
          infoTA.appendLine(bean.v.toStringComplete());
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


    coordTable = new BeanTableSorted(CoordBean.class, (PreferencesExt) prefs.node("CoordBean"), false, "Coordinates in group", "Coordinates", null);
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

  public void closeOpenFiles() throws IOException {
    if (gc != null) gc.close();
    gc = null;
  }

  public void showInfo(Formatter f) {
    if (gc == null) return;
    gc.showIndex(f);
    f.format("%n");

    List<GroupBean> groups = groupTable.getBeans();
    for (GroupBean bean : groups) {
      f.format("%-50s %-50s %d%n", bean.getGroupId(), bean.getDescription(), bean.getGdsHash());
      if (bean.group.run2part != null) {
        f.format("  run2Part: ");
        for (int part : bean.group.run2part) f.format("%d, ", part);
        f.format("%n");
      }
    }

    f.format("%n");
    // showFiles(f);
  }

  private void compareFiles(Formatter f) throws IOException {
    if (gc == null) return;
    List<String> canon = new ArrayList<String>(gc.getFilenames());
    Collections.sort(canon);

    File idxFile = gc.getIndexFile();
    File dir = idxFile.getParentFile();
    File[] files = dir.listFiles(new FilenameFilter() {
      public boolean accept(File dir, String name) {
        return name.endsWith(".ncx");
      }
    });

    int total = 0;
    for (File file : files) {
      RandomAccessFile raf = new RandomAccessFile(file.getPath(), "r");
      GribCollection cgc = Grib2CollectionBuilderFromIndex.readFromIndex(file.getName(), file.getParentFile(), raf, null, logger);
      List<String> cfiles = new ArrayList<String>(cgc.getFilenames());
      Collections.sort(cfiles);
      f.format("Compare files in %s to canonical files in %s%n", file.getPath(), idxFile.getPath());
      compareSortedList(f, canon.iterator(), cfiles.iterator());
      f.format("  Compared %d files to %d files%n%n", cfiles.size(), canon.size());
      raf.close();
      total += cfiles.size();
    }
    f.format("Total files = %d%n%n", total);
  }

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
  GribCollection gc;
  List<MFile> gcFiles;
  FeatureCollectionConfig.GribConfig config = null;

  public void setIndexFile(Path indexFile, FeatureCollectionConfig.GribConfig config) throws IOException {
    if (gc != null) gc.close();

    this.config = config;
    gc = GribCdmIndex2.openCdmIndex(indexFile.toString(), config, logger);
    if (gc == null)
      throw new IOException("Not a grib collection index file");

    List<GroupBean> groups = new ArrayList<GroupBean>();

    for (PartitionCollection.Dataset ds : gc.getDatasets())
      for (GribCollection.GroupHcs g : ds.getGroups())
        groups.add(new GroupBean(g, ds.getType().toString()));

    groupTable.setBeans(groups);
    groupTable.setHeader(indexFile.toString());
    gcFiles = gc.getFiles();
    varTable.clearBeans();
    coordTable.clearBeans();
  }

  private void setGroup(GribCollection.GroupHcs group) {
    List<VarBean> vars = new ArrayList<>();
    for (GribCollection.VariableIndex v : group.variList)
      vars.add(new VarBean(v, group));
    varTable.setBeans(vars);

    int count = 0;
    List<CoordBean> coords = new ArrayList<>();
    for (Coordinate vc : group.coords)
      coords.add(new CoordBean(vc, count++));
    coordTable.setBeans(coords);
  }

  private void showFiles(Formatter f) {
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


  }

  private void showFileTable(GribCollection gc, GribCollection.GroupHcs group) {
    File dir = gc.getDirectory();
    List<MFile> files = (group == null) ? gc.getFiles() : group.getFiles();
    fileTable.setFiles(dir, files);
  }

  ////////////////////////////////////////////////////////////////////////////

  public class GroupBean {
    GribCollection.GroupHcs group;
    String type;
    float density, avgDensity;
    int nrecords = 0;

    public GroupBean() {
    }

    public GroupBean(GribCollection.GroupHcs g, String type) {
      this.group = g;
      this.type = type;

      int total = 0;
      avgDensity = 0;
      for (GribCollection.VariableIndex vi : group.variList) {
        vi.calcTotalSize();
        total += vi.totalSize;
        nrecords += vi.nrecords;
        avgDensity += vi.density;
      }
      avgDensity /= group.variList.size();
      density = (total == 0) ? 0 : ((float) nrecords) / total;
    }

    public String getGroupId() {
      return group.getId();
    }

    public int getGdsHash() {
      return group.getGdsHash();
    }

    public int getNrecords() {
      return nrecords;
    }

    public String getType() {
      return type;
    }

    public int getNFiles() {
      if (group.filenose == null) return 0;
      return group.filenose.length;
    }

    public int getNCoords() {
      return group.coords.size();
    }

    public int getNVariables() {
      return group.variList.size();
    }

    public float getAvgDensity() {
      return avgDensity;
    }

    public float getDensity() {
      return density;
    }

   public String getDescription() {
      return group.getDescription();
    }

  }


  public class CoordBean implements Comparable<CoordBean> {
    Coordinate coord;
    int idx;

    // no-arg constructor

    public CoordBean() {
    }

    public String getType() {
      return coord.getType().toString();
    }

    public CoordBean(Coordinate coord, int idx) {
      this.coord = coord;
      this.idx = idx;
    }

    public String getValues() {
      if (coord.getValues() == null) return "";
      Formatter f = new Formatter();
      for (Object val : coord.getValues()) f.format("%s,", val);
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
      return coord.getName();
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
    GribCollection.VariableIndex v;
    GribCollection.GroupHcs group;

    public VarBean() {
    }

    public VarBean(GribCollection.VariableIndex v, GribCollection.GroupHcs group) {
      this.v = v;
      this.group = group;
    }

    public int getNRecords() {
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
    }

    public String getIndexes() {
      Formatter f = new Formatter();
      for (int idx : v.coordIndex) f.format("%d,", idx);
      return f.toString();
    }

    public String getIntvName() {
      return v.getTimeIntvName();
    }

    public int getHash() {
      return v.cdmHash;
    }

    public String getGroupId() {
      return group.getId();
    }

    public String getVariableId() {
      return v.discipline + "-" + v.category + "-" + v.parameter;
    }

    public void makeGribConfig(Formatter f) {
      f.format("<variable id='%s'/>%n", getVariableId());
    }

    private void showSparseArray(Formatter f) {

      Indent indent = new Indent(2);
      for (Coordinate coord : v.getCoordinates())
        coord.showInfo(f, indent);
      f.format("%n");

      if (v instanceof PartitionCollection.VariableIndexPartitioned) {
        PartitionCollection.VariableIndexPartitioned vip = (PartitionCollection.VariableIndexPartitioned) v;
        if (vip.twot != null)
          vip.twot.showMissing(f);

        if (vip.time2runtime != null) {
          Coordinate run = v.getCoordinate(Coordinate.Type.runtime);
          Coordinate tcoord = v.getCoordinate(Coordinate.Type.time);
          if (tcoord == null) tcoord = v.getCoordinate(Coordinate.Type.timeIntv);
          CoordinateTimeAbstract time = (CoordinateTimeAbstract) tcoord;
          CalendarDate ref = time.getRefDate();
          CalendarPeriod.Field unit = time.getTimeUnit().getField();
          f.format("time2runtime: %n");
          int count = 0;
          for (int idx : vip.time2runtime) {
            Object val = time.getValue(count);
            f.format(" %2d: %s -> %2d (%s)", count, val, idx-1, run.getValue(idx-1));
            if (val instanceof Integer) {
              int valI = (Integer) val;
              f.format(" == %s ", ref.add((double) valI, unit));
            }
            f.format(" %n");
            count++;
          }
          f.format("%n");
        }

      } else {
        try {
          v.readRecords();
        } catch (IOException e) {
          e.printStackTrace();
          return;
        }
        if (v.getSparseArray() != null) {
          SparseArray<GribCollection.Record> sa = v.getSparseArray();
          sa.showInfo(f, null);
          f.format("%n");
          sa.showContent(f);
        }
      }
    }

    /* private void showRecords(Formatter f) {

       try {
         GribCollection.Record[] records = v.getRecords();
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

     private void showRecords(Formatter f, List<Integer> values, GribCollection.Record[] records) throws IOException {
       f.format(" time (down)%n");

       for (int timeIdx = 0; timeIdx < v.ntimes; timeIdx++) {
         f.format("%10s = ", values.get(timeIdx));
         int idx = GribCollection.calcIndex(timeIdx, 0, 0, v.nens, v.nverts);
         GribCollection.Record r = records[idx];
         if (r == null) f.format("null");
         else f.format("(%d,%8d) ", r.fileno, r.pos);
         f.format("%n");
       }

       f.format("%n Show records in order%n");
       int count = 0;
       for (GribCollection.Record r : records) {
         if (r == null) f.format("null%n");
         else f.format("%5d = (%d,%8d)%n", count, r.fileno, r.pos);
         count++;
       }
       f.format("%n");

     }

     private void showRecords(Formatter f, VertCoord vcoord, List<Integer> values, GribCollection.Record[] records) throws IOException {
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
           int idx = GribCollection.calcIndex(timeIdx, 0, vertIdx, v.nens, v.nverts);
           GribCollection.Record r = records[idx];
           if (r == null) f.format("null");
           else f.format("(%d,%8d) ", r.fileno, r.pos);
         }
         f.format("%n");
       }

       f.format("%n Show records in order%n");
       int count = 0;
       for (GribCollection.Record r : records) {
         if (r == null) f.format("null%n");
         else f.format("%5d = (%d,%8d)%n", count, r.fileno, r.pos);
         count++;
       }
       f.format("%n");

     }

     void showRecords2Dintv(Formatter f, VertCoord vcoord, List<TimeCoord.Tinv> tinvs, GribCollection.Record[] records) throws IOException {
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
           int idx = GribCollection.calcIndex(timeIdx, 0, vertIdx, v.nens, v.nverts);
           GribCollection.Record r = records[idx];
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

     void showRecords2Dintv(Formatter f, List<TimeCoord.Tinv> tinvs, GribCollection.Record[] records) throws IOException {
       f.format(" timeIntv (down) %n");

       for (int timeIdx = 0; timeIdx < v.ntimes; timeIdx++) {
         f.format("%10s = ", tinvs.get(timeIdx));
         int idx = GribCollection.calcIndex(timeIdx, 0, 0, v.nens, v.nverts);
         GribCollection.Record r = records[idx];
         //f.format("%3d %10d ", r.fileno, r.drsPos);
         f.format("%6d ", (r == null ? -1 : r.fileno));
         f.format("%n");
       }
     }

    void sendFilesToGrib2Collection() {
      try {
        Set<Integer> filenos = new HashSet<>();
        for (GribCollection.Record r : v.getRecords()) {
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
       GribCollection.VariableIndex vindex = vP.getVindex(partno); // the variable in this partition
       f.format("time = %d val = %s partition = %d%n", timeIdx, val, partno);

       for (int ensIdx = 0; ensIdx < nens; ensIdx++) {
         if (nens > 1) f.format(" ens = %d%n", ensIdx);

         for (int levelIdx = 0; levelIdx <= nverts; levelIdx++) {
           if (nverts > 1) f.format("  vert = %d%n", levelIdx);

           // where does this record go in the result ??
           int resultIndex = GribCollection.calcIndex(timeIdx, ensIdx, levelIdx, nens, nverts);

           // where does this record come from ??
           int recordIndex = -1;

           int flag = vP.flag[partno]; // see if theres a mismatch with vert or ens coordinates
           if (flag == 0) { // no problem
             recordIndex = GribCollection.calcIndex(val.getIndex(), ensIdx, levelIdx, vindex.nens, vindex.nverts);

           } else {  // problem - must match coordinates
             recordIndex = GribCollection.calcIndex(val.getIndex(), ensIdx, levelIdx, flag, vindex.getEnsCoord(), vindex.getVertCoord(),
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
