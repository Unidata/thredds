/*
 * Copyright 1998-2009 University Corporation for Atmospheric Research/Unidata
 *
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

package ucar.nc2.thredds.monitor;

import ucar.util.prefs.PreferencesExt;
import ucar.util.prefs.ui.BeanTableSorted;

import javax.swing.*;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ChangeEvent;

import thredds.ui.TextHistoryPane;
import thredds.ui.IndependentWindow;
import thredds.ui.BAMutil;
import thredds.filesystem.server.LogReader;
import thredds.filesystem.server.ServletLogParser;
import thredds.filesystem.server.TestFileSystem;

import java.util.*;
import java.util.List;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.File;
import java.io.FileFilter;
import java.net.InetAddress;
import java.net.UnknownHostException;

import net.sf.ehcache.Cache;
import net.sf.ehcache.Element;

/**
 * Class Description.
 *
 * @author caron
 * @since Mar 26, 2009
 */
public class ServletLogTable extends JPanel {
  private PreferencesExt prefs;
  private Cache dnsCache;

  private ucar.util.prefs.ui.BeanTableSorted logTable, uptimeTable, mergeTable, undoneTable, miscTable;
  private ArrayList<ServletLogParser.ServletLog> completeLogs;
  private boolean calcMerge = true;
  private ArrayList<Merge> completeMerge;

  private JTabbedPane tabbedPanel;
  private JSplitPane split;

  private TextHistoryPane infoTA;
  private IndependentWindow infoWindow;

  private JComboBox rootSelector, serviceSelector;
  private JLabel sizeLable;

  public ServletLogTable(PreferencesExt prefs, JPanel buttPanel, Cache dnsCache) {
    this.prefs = prefs;
    this.dnsCache = dnsCache;

    logTable = new BeanTableSorted(ServletLogParser.ServletLog.class, (PreferencesExt) prefs.node("Logs"), false);
    logTable.addListSelectionListener(new ListSelectionListener() {
      public void valueChanged(ListSelectionEvent e) {
        LogReader.Log log = (LogReader.Log) logTable.getSelectedBean();
        if (log == null) return;
        infoTA.setText(log.toString());
        infoWindow.showIfNotIconified();
      }
    });

    thredds.ui.PopupMenu varPopup = new thredds.ui.PopupMenu(logTable.getJTable(), "Options");
    varPopup.addAction("DNS Lookup", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        LogReader.Log log = (LogReader.Log) logTable.getSelectedBean();
        if (log == null) return;
        try {
          infoTA.setText(log.getIp() + " = " + reverseDNS(log.getIp()));
        } catch (Exception ee) {
          infoTA.setTextFromStackTrace(ee);
        }
        infoWindow.showIfNotIconified();
      }
    });

    uptimeTable = new BeanTableSorted(Uptime.class, (PreferencesExt) prefs.node("UptimeTable"), false);
    uptimeTable.addListSelectionListener(new ListSelectionListener() {
      public void valueChanged(ListSelectionEvent e) {
        Uptime uptime = (Uptime) uptimeTable.getSelectedBean();
        if (uptime == null) return;
        mergeTable.setBeans(uptime.mergeList);
      }
    });

    mergeTable = new BeanTableSorted(Merge.class, (PreferencesExt) prefs.node("MergeTable"), false);
    mergeTable.addListSelectionListener(new ListSelectionListener() {
      public void valueChanged(ListSelectionEvent e) {
        Merge m = (Merge) mergeTable.getSelectedBean();
        if (m == null) return;
        infoTA.setText(m.toString());
        infoWindow.showIfNotIconified();
      }
    });

    thredds.ui.PopupMenu varPopupM = new thredds.ui.PopupMenu(mergeTable.getJTable(), "Options");
    varPopupM.addAction("DNS Lookup", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        Merge m = (Merge) mergeTable.getSelectedBean();
        if (m == null) return;
        try {
          infoTA.setText(m.getIp() + " = " + reverseDNS(m.getIp()));
        } catch (Exception ee) {
          infoTA.setTextFromStackTrace(ee);
        }
        infoWindow.showIfNotIconified();
      }
    });

    undoneTable = new BeanTableSorted(Merge.class, (PreferencesExt) prefs.node("UndoneTable"), false);
    undoneTable.addListSelectionListener(new ListSelectionListener() {
      public void valueChanged(ListSelectionEvent e) {
        Merge m = (Merge) undoneTable.getSelectedBean();
        if (m == null) return;
        infoTA.setText(m.toString());
        infoWindow.showIfNotIconified();
      }
    });

    miscTable = new BeanTableSorted(ServletLogParser.ServletLog.class, (PreferencesExt) prefs.node("Logs"), false);
    miscTable.addListSelectionListener(new ListSelectionListener() {
      public void valueChanged(ListSelectionEvent e) {
        LogReader.Log log = (LogReader.Log) miscTable.getSelectedBean();
        if (log == null) return;
        infoTA.setText(log.toString());
        infoWindow.showIfNotIconified();
      }
    });

    // the info window
    infoTA = new TextHistoryPane();
    infoWindow = new IndependentWindow("Extra Information", BAMutil.getImage("netcdfUI"), infoTA);
    infoWindow.setBounds((Rectangle) prefs.getBean("InfoWindowBounds", new Rectangle(300, 300, 800, 100)));

    // the selectors

    rootSelector = new JComboBox(TestFileSystem.getRoots());
    rootSelector.insertItemAt("", 0);
    rootSelector.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        selectRoot((String) rootSelector.getSelectedItem());
        tabbedPanel.setSelectedIndex(1);
      }
    });

    serviceSelector = new JComboBox(TestFileSystem.services);
    serviceSelector.insertItemAt("", 0);
    serviceSelector.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        selectService((String) serviceSelector.getSelectedItem());
        tabbedPanel.setSelectedIndex(1);
      }
    });

    JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
    topPanel.add(new JLabel("Dataroot:"));
    topPanel.add(rootSelector);
    topPanel.add(new JLabel("Service:"));
    topPanel.add(serviceSelector);
    topPanel.add(new JLabel("Size:"));
    sizeLable = new JLabel();
    topPanel.add(sizeLable);

    split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, false, uptimeTable, mergeTable);
    split.setDividerLocation(prefs.getInt("splitPos", 500));

    // the top UI
    tabbedPanel = new JTabbedPane(JTabbedPane.TOP);
    tabbedPanel.addTab("LogTable", logTable);
    tabbedPanel.addTab("Merge", split);
    tabbedPanel.addTab("Undone", undoneTable);
    tabbedPanel.addTab("Misc", miscTable);
    tabbedPanel.setSelectedIndex(0);

    tabbedPanel.addChangeListener(new ChangeListener() {
      public void stateChanged(ChangeEvent e) {
        int idx = tabbedPanel.getSelectedIndex();
        String title = tabbedPanel.getTitleAt(idx);
        if (title.equals("Merge"))
          calcMergeLogs(completeLogs);
        if (title.equals("Undone"))
          calcMergeLogs(completeLogs);
        if (title.equals("Misc"))
          calcMergeLogs(completeLogs);
      }
    });

    AbstractAction allAction = new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        logTable.setBeans(completeLogs);
        tabbedPanel.setSelectedIndex(0);
      }
    };
    BAMutil.setActionProperties(allAction, "Refresh", "show All Logs", false, 'A', -1);
    BAMutil.addActionToContainer(buttPanel, allAction);

    setLayout(new BorderLayout());
    add(topPanel, BorderLayout.NORTH);
    add(tabbedPanel, BorderLayout.CENTER);
  }

  public void exit() {
    logTable.saveState(false);
    mergeTable.saveState(false);
    undoneTable.saveState(false);
    uptimeTable.saveState(false);
    prefs.putBeanObject("InfoWindowBounds", infoWindow.getBounds());
    prefs.putInt("splitPos", split.getDividerLocation());
  }

  ////////////////////////////////////////////////////////
  class MyLogFilter implements LogReader.LogFilter {
    public boolean pass(LogReader.Log log) {
      return true;
    }
  }

  class MyFF implements FileFilter {
    public boolean accept(File f) {
      String name = f.getName();
      return name.startsWith("access") && name.endsWith(".log");
    }
  }

  class MyClosure implements LogReader.Closure {
    ArrayList<ServletLogParser.ServletLog> logs;

    MyClosure(ArrayList<ServletLogParser.ServletLog> logs) {
      this.logs = logs;
    }

    public void process(LogReader.Log log) {
      if (log != null)
        logs.add((ServletLogParser.ServletLog) log);
      else
        System.out.printf("HEY NULL LOG%n");
    }
  }

  public void setLogFiles(List<File> logFiles) throws IOException {
    LogReader reader = new LogReader(new ServletLogParser());

    long startElapsed = System.nanoTime();
    LogReader.Stats stats = new LogReader.Stats();

    // reverse sort on sequence number
    Collections.sort(logFiles, new Comparator<File>() {
      public int compare(File o1, File o2) {
        return getSeq(o2) - getSeq(o1);
      }

      private int getSeq(File f) {
        String name = f.getName();
        int pos = name.indexOf(".log") + 5;
        if (name.length() <= pos) return 0;
        return Integer.parseInt(name.substring(pos));
      }
    });

    completeLogs = new ArrayList<ServletLogParser.ServletLog>(30000);
    for (File f : logFiles)
      reader.scanLogFile(f, new MyClosure(completeLogs), new MyLogFilter(), stats);
    logTable.setBeans(completeLogs);

    long elapsedTime = System.nanoTime() - startElapsed;
    System.out.printf(" setLogFile total= %d passed=%d%n", stats.total, stats.passed);
    System.out.printf(" elapsed=%f msecs %n", elapsedTime / (1000 * 1000.0));

    mergeTable.setBeans(new ArrayList());
    undoneTable.setBeans(new ArrayList());

    calcMerge = true;
  }

  ////////////////////////////////////////////////


  public class Merge {
    public long getReqTime() {
      return start.getReqTime();
    }

    public long getReqSeq() {
      return start.getReqSeq();
    }

    public String getLevel() {
      return level;
    }

    public boolean isExtra() {
      return extra != null;
    }

    public String getIp() {
      return start.getIp();
    }

    public String getDate() {
      return start.getDate();
    }

    public int getStatus() {
      return (done == null) ? -1 : done.getStatus();
    }

    public long getMsecs() {
      return (done == null) ? -1 : done.getMsecs();
    }

    public long getBytes() {
      return (done == null) ? -1 : done.getBytes();
    }

    public String getPath() {
      return start.getPath();
    }

    ArrayList<ServletLogParser.ServletLog> logs = new ArrayList<ServletLogParser.ServletLog>(2);
    ServletLogParser.ServletLog start;
    ServletLogParser.ServletLog done;
    String level;
    StringBuilder extra;

    public Merge() {
    }

    Merge(ServletLogParser.ServletLog start) {
      this.start = start;
      add(start);
    }

    void setDone(ServletLogParser.ServletLog done) {
      this.done = done;
      add(done);
    }

    void add(ServletLogParser.ServletLog log) {
      logs.add(log);
      if (!log.getLevel().equals("INFO"))
        level = log.getLevel();
      if (log.extra != null)
        extra = log.extra;
    }

    public String toString() {
      Formatter f = new Formatter();
      for (ServletLogParser.ServletLog log : logs)
        f.format("%s%n", log);
      return f.toString();
    }
  }

  private void calcMergeLogs(ArrayList<ServletLogParser.ServletLog> logs) {
    if (!calcMerge) return;
    if (logs == null) return;

    Collections.sort(logs, new Comparator<ServletLogParser.ServletLog>() {
      public int compare(ServletLogParser.ServletLog o1, ServletLogParser.ServletLog o2) {
        return o1.getDate().compareTo(o2.getDate());
      }
    });

    completeMerge = new ArrayList<Merge>(logs.size() / 2);
    ArrayList<ServletLogParser.ServletLog> miscList = new ArrayList<ServletLogParser.ServletLog>(1000);
    ArrayList<Uptime> uptimeList = new ArrayList<Uptime>(10);
    ArrayList<Merge> undoneList = new ArrayList<Merge>(1000);

    ServletLogParser.ServletLog last = null;
    Uptime current = null;
    for (ServletLogParser.ServletLog log : logs) {
      if (current == null) {
        current = new Uptime(log);
        uptimeList.add(current);

      } else if (log.getReqTime() < last.getReqTime()) {
        current.finish(last, undoneList);
        completeMerge.addAll(current.mergeList);
        current = new Uptime(log);
        uptimeList.add(current);
      }

      last = log;
      current.add(log, miscList);
    }
    current.finish(last, undoneList);
    completeMerge.addAll(current.mergeList);

    uptimeTable.setBeans(uptimeList);
    undoneTable.setBeans(undoneList);
    miscTable.setBeans(miscList);
    calcMerge = false;
  }

  public class Uptime {
    private int n = 10 * 1000;


    public int getN() {
      return mergeList.size();
    }

    public String getStartDate() {
      return startDate;
    }

    public String getEndDate() {
      return endDate;
    }

    public long getStartTime() {
      return startTime;
    }

    public long getEndTime() {
      return endTime;
    }

    public long getStartSeq() {
      return startSeq;
    }

    public long getEndSeq() {
      return endSeq;
    }

    private String startDate, endDate;
    private long startTime, endTime;
    private long startSeq, endSeq;
    ArrayList<Merge> mergeList;
    HashMap<Long, Merge> map;

    public Uptime() {
    }

    public Uptime(ServletLogParser.ServletLog log) {
      this.startSeq = log.getReqSeq();
      this.startTime = log.getReqTime();
      this.startDate = log.getDate();
      mergeList = new ArrayList<Merge>(n);
      map = new HashMap<Long, Merge>(n);
    }

    void add(ServletLogParser.ServletLog log, ArrayList<ServletLogParser.ServletLog> miscList) {

      if (log.isStart()) {
        Merge merge = new Merge(log);
        map.put(log.getReqSeq(), merge);

      } else if (log.isDone()) {
        Merge merge = map.get(log.getReqSeq());
        if (merge != null) {
          merge.setDone(log);
          mergeList.add(merge);
          map.remove(log.getReqSeq());
        } else {
          miscList.add(log);
        }

      } else {
        Merge merge = map.get(log.getReqSeq());
        if (merge != null)
          merge.add(log);
        else
          miscList.add(log);
      }
    }

    void finish(ServletLogParser.ServletLog log, ArrayList<Merge> undoneList) {
      this.endSeq = log.getReqSeq();
      this.endTime = log.getReqTime();
      this.endDate = log.getDate();
      undoneList.addAll(map.values());
      map = null;
    }

  }

  ////////////////////////////////////////////////

  String reverseDNS(String ip) throws UnknownHostException {
    Element cacheElem = dnsCache.get(ip);
    if (cacheElem == null) {
      InetAddress addr = InetAddress.getByName(ip);
      cacheElem = new Element(ip, addr.getHostName());
      dnsCache.put(cacheElem);
    }
    return (String) cacheElem.getValue();
  }

  ////////////////////////////////////////////////

  void selectRoot(String root) {
    if (root.length() == 0) {
      mergeTable.setBeans(completeMerge);
      return;
    }

    ArrayList<Merge> restrictMerge = new ArrayList<Merge>(1000);
    for (Merge m : completeMerge) {
      if (TestFileSystem.getDataroot(m.getPath()).equals(root))
        restrictMerge.add(m);
    }
    mergeTable.setBeans(restrictMerge);
    sizeLable.setText(Integer.toString(restrictMerge.size()));
    calcMerge = true;
  }

  void selectService(String service) {
    if (service.length() == 0) {
      mergeTable.setBeans(completeMerge);
      return;
    }

    ArrayList<Merge> restrictMerge = new ArrayList<Merge>(1000);
    for (Merge m : completeMerge) {
      if (TestFileSystem.getService(m.getPath()).equals(service))
        restrictMerge.add(m);
    }
    mergeTable.setBeans(restrictMerge);
    sizeLable.setText(Integer.toString(restrictMerge.size()));
    calcMerge = true;
  }


}
