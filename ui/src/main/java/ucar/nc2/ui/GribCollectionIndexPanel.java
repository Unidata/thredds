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

import thredds.inventory.MFile;
import ucar.nc2.grib.*;
import ucar.nc2.grib.grib1.Grib1CollectionBuilder;
import ucar.nc2.grib.grib1.Grib1TimePartitionBuilder;
import ucar.nc2.grib.grib2.Grib2CollectionBuilder;
import ucar.nc2.grib.grib2.Grib2TimePartitionBuilder;
import ucar.nc2.time.CalendarDateFormatter;
import ucar.nc2.ui.widget.BAMutil;
import ucar.nc2.ui.widget.IndependentWindow;
import ucar.nc2.ui.widget.PopupMenu;
import ucar.nc2.ui.widget.TextHistoryPane;
import ucar.unidata.io.RandomAccessFile;
import ucar.util.prefs.PreferencesExt;
import ucar.util.prefs.ui.BeanTableSorted;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.util.*;
import java.util.List;

/**
 * Examine Grib Collection Index files
 * Grib1 or Grib2
 *
 * @author caron
 * @since 6/29/11
 */
public class GribCollectionIndexPanel extends JPanel {
  static private final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(GribCollectionIndexPanel.class);

  private PreferencesExt prefs;

  private BeanTableSorted groupTable, varTable, vertCoordTable, timeCoordTable;
  private JSplitPane split, split2, split3;

  private TextHistoryPane infoPopup, detailTA;
  private IndependentWindow infoWindow, detailWindow;

  public GribCollectionIndexPanel(PreferencesExt prefs, JPanel buttPanel) {
    this.prefs = prefs;

    AbstractButton infoButton = BAMutil.makeButtcon("Information", "Show Info", false);
    infoButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        Formatter f = new Formatter();
        gc.showIndex(f);
        detailTA.setText(f.toString());
        detailTA.gotoTop();
        detailWindow.show();
      }
    });
    buttPanel.add(infoButton);


    AbstractButton filesButton = BAMutil.makeButtcon("Information", "Show Files", false);
    filesButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        Formatter f = new Formatter();
        showFiles(f);
        detailTA.setText(f.toString());
        detailTA.gotoTop();
        detailWindow.show();
      }
    });
    buttPanel.add(filesButton);


   /*  AbstractButton showButt = BAMutil.makeButtcon("Information", "Compare Files", false);
    showButt.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        Formatter f = new Formatter();
        try {
          compareFiles(f);
        } catch (IOException e1) {
          e1.printStackTrace();
        }
        detailTA.setText(f.toString());
        detailTA.gotoTop();
        detailWindow.show();
      }
    });
    buttPanel.add(showButt);  */

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
    varPopup.addAction("Show Files Used", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        GroupBean bean = (GroupBean) groupTable.getSelectedBean();
        if (bean != null) {
          Formatter f= new Formatter();
          bean.showFilesUsed(f);
          
          infoPopup.setText(f.toString());
          infoPopup.gotoTop();
          infoWindow.show();
        }
      }
    });

    varTable = new BeanTableSorted(VarBean.class, (PreferencesExt) prefs.node("Grib2Bean"), false, "Variables in group", "GribCollection.VariableIndex", null);
    
    varPopup = new PopupMenu(varTable.getJTable(), "Options");
    varPopup.addAction("Show Variable", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        VarBean bean = (VarBean) varTable.getSelectedBean();
        if (bean != null) {
          infoPopup.setText(bean.v.toStringComplete());
          infoPopup.gotoTop();
          infoWindow.show();
        }
      }
    });
    varPopup.addAction("Show Record Table", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        VarBean bean = (VarBean) varTable.getSelectedBean();
        if (bean != null) {
          Formatter f = new Formatter();
          bean.showRecords(f);
          infoPopup.setText(f.toString());
          infoPopup.gotoTop();
          infoWindow.show();
        }
      }
    });

    vertCoordTable = new BeanTableSorted(CoordBean.class, (PreferencesExt) prefs.node("CoordBean"), false, "Vertical Coordinates", "VertCoord", null);
    varPopup = new PopupMenu(vertCoordTable.getJTable(), "Options");

    varPopup.addAction("Show", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        CoordBean bean = (CoordBean) vertCoordTable.getSelectedBean();
        if (bean != null) {
          infoPopup.setText(bean.vc.toString());
          infoPopup.gotoTop();
          infoWindow.show();
        }
      }
    });

    timeCoordTable = new BeanTableSorted(TimeCoordBean.class, (PreferencesExt) prefs.node("TimeCoordBean"), false, "Time Coordinates", "TimeCoord", null);
    varPopup = new PopupMenu(timeCoordTable.getJTable(), "Options");

    varPopup.addAction("Show", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        TimeCoordBean bean = (TimeCoordBean) timeCoordTable.getSelectedBean();
        if (bean != null) {
          infoPopup.setText(bean.tc.toString());
          infoPopup.gotoTop();
          infoWindow.show();
        }
      }
    });

    /////////////////////////////////////////
    // the info windows
    infoPopup = new TextHistoryPane();
    infoWindow = new IndependentWindow("Extra Information", BAMutil.getImage("netcdfUI"), infoPopup);
    infoWindow.setBounds((Rectangle) prefs.getBean("InfoWindowBounds", new Rectangle(300, 300, 500, 300)));

    detailTA = new TextHistoryPane();
    detailWindow = new IndependentWindow("Extra Information", BAMutil.getImage("netcdfUI"), detailTA);
    detailWindow.setBounds((Rectangle) prefs.getBean("DetailWindowBounds", new Rectangle(300, 300, 500, 300)));

    setLayout(new BorderLayout());

    split3 = new JSplitPane(JSplitPane.VERTICAL_SPLIT, false, groupTable, varTable);
    split3.setDividerLocation(prefs.getInt("splitPos3", 800));

    split2 = new JSplitPane(JSplitPane.VERTICAL_SPLIT, false, split3, vertCoordTable);
    split2.setDividerLocation(prefs.getInt("splitPos2", 800));

    split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, false, split2, timeCoordTable);
    split.setDividerLocation(prefs.getInt("splitPos", 500));

    add(split, BorderLayout.CENTER);

  }

  public void save() {
    groupTable.saveState(false);
    varTable.saveState(false);
    vertCoordTable.saveState(false);
    prefs.putBeanObject("InfoWindowBounds", infoWindow.getBounds());
    prefs.putBeanObject("DetailWindowBounds", detailWindow.getBounds());
    if (split != null) prefs.putInt("splitPos", split.getDividerLocation());
    if (split2 != null) prefs.putInt("splitPos2", split2.getDividerLocation());
    if (split3 != null) prefs.putInt("splitPos3", split3.getDividerLocation());
  }

  public void closeOpenFiles() throws IOException {
    if (gc != null) gc.close();
    gc = null;
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
      GribCollection cgc = Grib2CollectionBuilder.createFromIndex(file.getPath(), file.getParentFile(), raf, null, logger);
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

  public void setIndexFile(String indexFile) throws IOException {
    if (gc != null) gc.close();

    RandomAccessFile raf = new RandomAccessFile(indexFile, "r");
    raf.seek(0);
    byte[] b = new byte[Grib2CollectionBuilder.MAGIC_START.getBytes().length];
    raf.read(b);
    String magic = new String(b);
    if (magic.equals(Grib2CollectionBuilder.MAGIC_START))
      gc = Grib2CollectionBuilder.createFromIndex(indexFile, null, raf, null, logger);
    else if (magic.equals(Grib1CollectionBuilder.MAGIC_START))
      gc = Grib1CollectionBuilder.createFromIndex(indexFile, null, raf, null, logger);
    else if (magic.equals(Grib2TimePartitionBuilder.MAGIC_START))
      gc = Grib2TimePartitionBuilder.createFromIndex(indexFile, null, raf, logger);
    else if (magic.equals(Grib1TimePartitionBuilder.MAGIC_START))
      gc = Grib1TimePartitionBuilder.createFromIndex(indexFile, null, raf, logger);

    else
      throw new IOException("Not a grib collection index file ="+magic);

    List<GroupBean> groups = new ArrayList<GroupBean>();
    for (GribCollection.GroupHcs g : gc.getGroups()) {
      groups.add(new GroupBean(g));
    }
    groupTable.setBeans(groups);
  }

  private void setGroup(GribCollection.GroupHcs group) {
    List<VarBean> vars = new ArrayList<VarBean>();
    for (GribCollection.VariableIndex v : group.varIndex)
      vars.add(new VarBean(v, group));
    varTable.setBeans(vars);

    int count = 0;
    List<CoordBean> coords = new ArrayList<CoordBean>();
    for (VertCoord vc : group.vertCoords)
      coords.add(new CoordBean(vc, count++));
    vertCoordTable.setBeans(coords);

    count = 0;
    List<TimeCoordBean> tcoords = new ArrayList<TimeCoordBean>();
    for (TimeCoord tc : group.timeCoords)
      tcoords.add(new TimeCoordBean(tc, count++));
    timeCoordTable.setBeans(tcoords);
  }

  private void showFiles(Formatter f) {
    if (gc == null) return;
    if (gc.getFiles() == null) return;
    int count = 0;
    List<MFile> fs = new ArrayList<MFile>(gc.getFiles());
    Map<MFile, Integer> map = new HashMap<MFile, Integer>(fs.size() * 2);

    f.format("In order:%n");
    for (MFile file : fs) {
      f.format("%5d %60s lastModified=%s%n", count, file.getName(), CalendarDateFormatter.toDateTimeString( new Date(file.getLastModified())));
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

    f.format("============%n%s%n", gc);
  }

  ////////////////////////////////////////////////////////////////////////////

  public class GroupBean {
    GribCollection.GroupHcs group;

    public GroupBean() {
    }

    public GroupBean(GribCollection.GroupHcs g) {
      this.group = g;
    }

    public String getGroupId() {
      return group.getId();
    }
    
    public int getGdsHash() {
      return group.gdsHash;
    }

    public String getDescription() {
      return group.getDescription();
    }

    void showFilesUsed(Formatter f) {
      List<MFile> files = group.getFiles();
      for (MFile file : files) {
        f.format(" %s%n", file.getName());
      }
    }

  }

  public class VarBean {
    GribCollection.VariableIndex v;
    GribCollection.GroupHcs group;

    public VarBean() {
    }

    public VarBean(GribCollection.VariableIndex v, GribCollection.GroupHcs group) {
      this.v = v;
      this.group = group;
    }

    public int getTimeCoord() {
      return v.timeIdx;
    }

    public boolean getTimeIntv() {
      return (v.getTimeCoord() == null) ? false : v.getTimeCoord().isInterval();
    }

    public boolean getVertLayer() {
      return (v.getVertCoord() == null) ? false : v.getVertCoord().isLayer();
    }

    public int getVertCoord() {
      return v.vertIdx;
    }

    public int getEnsCoord() {
      return v.ensIdx;
    }

    public int getLevelType() {
      return v.levelType;
    }

    public int getIntvType() {
      return v.intvType;
    }

    public int getProbType() {
      return v.probType;
    }

    public int getEnsType() {
      return v.ensDerivedType;
    }

    public int getGenType() {
      return v.genProcessType;
    }

    public String getIntvName() {
      return v.intvName;
    }

    public String getProbName() {
      return v.probabilityName;
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

    private void showRecords(Formatter f) {
      TimeCoord tcoord = v.getTimeCoord();
      VertCoord vcoord = v.getVertCoord();
      EnsCoord ecoord = v.getEnsCoord();

      try {
        if (tcoord.isInterval()) {
          if (vcoord == null)
            showRecords2Dintv(f, tcoord.getIntervals());
          else
            showRecords2Dintv(f, vcoord, tcoord.getIntervals());
        } else {
          if (vcoord == null)
            showRecords(f, tcoord.getCoords());
          else
            showRecords(f, vcoord, tcoord.getCoords());
        }
      } catch (IOException ioe) {
        ioe.printStackTrace();
      }
    }

    private void showRecords(Formatter f, List<Integer> values) throws IOException {
      f.format("Variable %s%n", v.toStringComplete());
      f.format(" Show records (file,pos)%n");
      f.format(" time (down)%n");

      GribCollection.Record[] records = v.getRecords();
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

    private void showRecords(Formatter f, VertCoord vcoord, List<Integer> values) throws IOException {
      f.format("Variable %s%n", v.toStringComplete());
      f.format(" Show records (file,pos)%n");
      f.format(" time (down) x vert (across) %n");

      f.format("%12s ", " ");
      List<VertCoord.Level> levels = vcoord.getCoords();
      boolean isLayer = vcoord.isLayer();
      for (int j = 0; j < levels.size(); j++)
        f.format("%6s ", levels.get(j).toString(isLayer));
      f.format("%n");

      GribCollection.Record[] records = v.getRecords();
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

      int count = 0;
      for (GribCollection.Record r : records) {
        if (r == null) f.format("null%n");
        else f.format("%5d = (%d,%8d)%n", count, r.fileno, r.pos);
        count++;
      }
      f.format("%n");

    }

    void showRecords2Dintv(Formatter f, VertCoord vcoord, List<TimeCoord.Tinv> tinvs) throws IOException {
      f.format("%12s ", " ");
      List<VertCoord.Level> levels = vcoord.getCoords();
      boolean isLayer = vcoord.isLayer();
      for (int j = 0; j < levels.size(); j++)
        f.format("%6s ", levels.get(j).toString(isLayer));
      f.format("%n");

      GribCollection.Record[] records = v.getRecords();
      for (int timeIdx = 0; timeIdx < v.ntimes; timeIdx++) {
        f.format("%10s = ", tinvs.get(timeIdx));
        for (int vertIdx = 0; vertIdx < vcoord.getSize(); vertIdx++) {
          int idx = GribCollection.calcIndex(timeIdx, 0, vertIdx, v.nens, v.nverts);
          GribCollection.Record r = records[idx];
          //f.format("%3d %10d ", r.fileno, r.drsPos);
          f.format("%6d ", (r == null ? -1 : r.fileno));
        }
        f.format("%n");
      }
    }

    void showRecords2Dintv(Formatter f, List<TimeCoord.Tinv> tinvs) throws IOException {
      GribCollection.Record[] records = v.getRecords();
      for (int timeIdx = 0; timeIdx < v.ntimes; timeIdx++) {
        f.format("%10s = ", tinvs.get(timeIdx));
        int idx = GribCollection.calcIndex(timeIdx, 0, 0, v.nens, v.nverts);
        GribCollection.Record r = records[idx];
        //f.format("%3d %10d ", r.fileno, r.drsPos);
        f.format("%6d ", (r == null ? -1 : r.fileno));
        f.format("%n");
      }
    }

  }

  public class CoordBean {
    VertCoord vc;
    int index;

    public CoordBean() {
    }

    public CoordBean(VertCoord vc, int index) {
      this.vc = vc;
      this.index = index;
    }

    public boolean isLayer() {
      return vc.isLayer();
    }

    public int getSize() {
      return vc.getSize();
    }

    public int getCode() {
      return vc.getCode();
    }

    public String getVertCoords() {
      return vc.showCoords();
    }

    public String getVertCoordName() {
      return vc.getName();
    }

    public String getUnits() {
      return vc.getUnits();
    }

    public int getIndex() {
      return index;
    }
  }

  public class TimeCoordBean {
    TimeCoord tc;
    int index;

    public TimeCoordBean() {
    }

    public TimeCoordBean(TimeCoord tc, int index) {
      this.tc = tc;
      this.index = index;
    }

    public int getNCoords() {
      return tc.getSize();
    }

    public String getCalendarRange() {
      return tc.getCalendarRange().toString();
    }

    public String getTimeUnit() {
      return tc.getTimeUnit().toString();
    }

    public String getTimeIntervalName() {
      return tc.getTimeIntervalName();
    }

    public int getIndex() {
      return index;
    }
  }

}


