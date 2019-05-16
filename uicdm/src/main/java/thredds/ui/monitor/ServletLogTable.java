/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package thredds.ui.monitor;

import thredds.logs.LogReader;
import thredds.logs.ServletLogParser;
import ucar.ui.widget.BAMutil;
import ucar.ui.widget.IndependentWindow;
import ucar.ui.widget.PopupMenu;
import ucar.ui.widget.TextHistoryPane;
import ucar.util.prefs.PreferencesExt;
import ucar.ui.prefs.BeanTable;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;

/**
 * Class Description.
 *
 * @author caron
 * @since Mar 26, 2009
 */
public class ServletLogTable extends JPanel {
  private PreferencesExt prefs;
  private DnsLookup dnsLookup;

  private ucar.ui.prefs.BeanTable logTable, uptimeTable, mergeTable, undoneTable, miscTable;
  private ArrayList<ServletLogParser.ServletLog> completeLogs;
  private boolean calcMerge = true;
  private ArrayList<Merge> completeMerge;

  private JTabbedPane tabbedPanel;
  private JSplitPane split;

  private TextHistoryPane infoTA;
  private IndependentWindow infoWindow;

  //private JComboBox rootSelector, serviceSelector;
  //private JLabel sizeLable;
  private JTextArea startDateField, endDateField;

  public ServletLogTable(JTextArea startDateField, JTextArea endDateField, PreferencesExt prefs, DnsLookup dnsLookup) {
    this.startDateField = startDateField;
    this.endDateField = endDateField;
    this.prefs = prefs;
    this.dnsLookup = dnsLookup;

    logTable = new BeanTable(ServletLogParser.ServletLog.class, (PreferencesExt) prefs.node("Logs"), false);
    logTable.addListSelectionListener(new ListSelectionListener() {
      public void valueChanged(ListSelectionEvent e) {
        LogReader.Log log = (LogReader.Log) logTable.getSelectedBean();
        if (log == null) return;
        infoTA.setText(log.toString());
        infoWindow.show();
      }
    });

    PopupMenu varPopup = new ucar.ui.widget.PopupMenu(logTable.getJTable(), "Options");
    varPopup.addAction("DNS Lookup", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        LogReader.Log log = (LogReader.Log) logTable.getSelectedBean();
        if (log == null) return;
        try {
          infoTA.setText(log.getIp() + " = " + dnsLookup.reverseDNS(log.getIp()));
        } catch (Exception ee) {
          infoTA.setTextFromStackTrace(ee);
        }
        infoWindow.show();
      }
    });

    uptimeTable = new BeanTable(Uptime.class, (PreferencesExt) prefs.node("UptimeTable"), false);
    uptimeTable.addListSelectionListener(new ListSelectionListener() {
      public void valueChanged(ListSelectionEvent e) {
        Uptime uptime = (Uptime) uptimeTable.getSelectedBean();
        if (uptime == null) return;
        mergeTable.setBeans( filter(uptime.mergeList));
      }
    });

    mergeTable = new BeanTable(Merge.class, (PreferencesExt) prefs.node("MergeTable"), false);
    mergeTable.addListSelectionListener(new ListSelectionListener() {
      public void valueChanged(ListSelectionEvent e) {
        Merge m = (Merge) mergeTable.getSelectedBean();
        if (m == null) return;
        infoTA.setText(m.toString());
        infoWindow.show();
      }
    });

    PopupMenu varPopupM = new PopupMenu(mergeTable.getJTable(), "Options");
    varPopupM.addAction("DNS Lookup", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        Merge m = (Merge) mergeTable.getSelectedBean();
        if (m == null) return;
        try {
          infoTA.setText(m.getIp() + " = " + dnsLookup.reverseDNS(m.getIp()));
        } catch (Exception ee) {
          infoTA.setTextFromStackTrace(ee);
        }
        infoWindow.show();
      }
    });

    varPopupM.addAction("Remove selected logs", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        List all = mergeTable.getBeans();
        List selected = mergeTable.getSelectedBeans();
        all.removeAll(selected);
        mergeTable.setBeans(all);
      }
    });

    undoneTable = new BeanTable(Merge.class, (PreferencesExt) prefs.node("UndoneTable"), false);
    undoneTable.addListSelectionListener(new ListSelectionListener() {
      public void valueChanged(ListSelectionEvent e) {
        Merge m = (Merge) undoneTable.getSelectedBean();
        if (m == null) return;
        infoTA.setText(m.toString());
        infoWindow.show();
      }
    });

    miscTable = new BeanTable(ServletLogParser.ServletLog.class, (PreferencesExt) prefs.node("Logs"), false);
    miscTable.addListSelectionListener(new ListSelectionListener() {
      public void valueChanged(ListSelectionEvent e) {
        LogReader.Log log = (LogReader.Log) miscTable.getSelectedBean();
        if (log == null) return;
        infoTA.setText(log.toString());
        infoWindow.show();
      }
    });

    // the info window
    infoTA = new TextHistoryPane();
    infoWindow = new IndependentWindow("Extra Information", BAMutil.getImage("nj22/NetcdfUI"), infoTA);
    infoWindow.setBounds((Rectangle) prefs.getBean("InfoWindowBounds", new Rectangle(300, 300, 800, 100)));

    /* the selectors

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
    topPanel.add(sizeLable);  */

    split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, false, uptimeTable, mergeTable);
    split.setDividerLocation(prefs.getInt("splitPos", 500));

    // the tabbed panes
    tabbedPanel = new JTabbedPane(JTabbedPane.TOP);
    tabbedPanel.addTab("LogTable", logTable);
    tabbedPanel.addTab("Merge", split);
    tabbedPanel.addTab("Undone", undoneTable);
    tabbedPanel.addTab("Misc", miscTable);
    tabbedPanel.setSelectedIndex(0);

    tabbedPanel.addChangeListener(e -> {
        int idx = tabbedPanel.getSelectedIndex();
        String title = tabbedPanel.getTitleAt(idx);
      switch (title) {
        case "Merge":
          calcMergeLogs(completeLogs);
          break;
        case "Undone":
          calcMergeLogs(completeLogs);
          break;
        case "Misc":
          calcMergeLogs(completeLogs);
          break;
      }
    });

    setLayout(new BorderLayout());


    /* AbstractAction allAction = new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        logTable.setBeans(completeLogs);
        tabbedPanel.setSelectedIndex(0);
      }
    };
    BAMutil.setActionProperties(allAction, "Refresh", "show All Logs", false, 'A', -1);
    BAMutil.addActionToContainer(topPanel, allAction);
    add(topPanel, BorderLayout.NORTH); */

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

  private SimpleDateFormat df = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
  private LogLocalManager manager;
  private java.util.List<LogLocalManager.FileDateRange> logFiles = null;

  public void setLocalManager( LogLocalManager manager) {
    this.manager = manager;

    Date startDate = manager.getStartDate();
    Date endDate = manager.getEndDate();
    if (startDate != null)
      startDateField.setText(df.format(startDate));
    if (endDate != null)
     endDateField.setText(df.format(endDate));
  }

  private MergeFilter currFilter = null;

  public void showLogs(MergeFilter filter) {
    Date start = null, end = null;
     try {
       start = df.parse(startDateField.getText());
       end = df.parse(endDateField.getText());
       logFiles = manager.getLocalFiles(start, end);
     } catch (Exception e) {
       e.printStackTrace();
       logFiles = manager.getLocalFiles(null, null);
     }

    if ((start != null) && (end != null))
      currFilter = new DateFilter(start.getTime(), end.getTime(), filter);
    else
      currFilter = filter;
    
    LogReader reader = new LogReader(new ServletLogParser());

    long startElapsed = System.nanoTime();
    LogReader.Stats stats = new LogReader.Stats();

    //  sort on name
    logFiles.sort(new Comparator<LogLocalManager.FileDateRange>() {
      public int compare(LogLocalManager.FileDateRange o1, LogLocalManager.FileDateRange o2) {
        if (o1.f.getName().equals("threddsServlet.log"))
          return 1;
        if (o2.f.getName().equals("threddsServlet.log"))
          return -1;
        return o1.f.getName().compareTo(o2.f.getName());
      }

      private int getSeq(File f) {
        String name = f.getName();
        int pos = name.indexOf(".log") + 5;
        if (name.length() <= pos)
          return 0;
        return Integer.parseInt(name.substring(pos));
      }
    });

    try {
      completeLogs = new ArrayList<>(30000);
      for (LogLocalManager.FileDateRange fdr : logFiles)
        reader.scanLogFile(fdr.f, new MyClosure(completeLogs), new LogReader.FilterNoop(), stats);
      
      // estimate number of threads used
      int nthreads = 0;
      for (ServletLogParser.ServletLog log : completeLogs) {
        if (log.isStart()) nthreads++;
        if (log.isDone() && nthreads > 0) nthreads--;
        log.setNthreads(nthreads);
      }
      logTable.setBeans(completeLogs);

    } catch (IOException ioe) {
      ioe.printStackTrace();
      return;
    }

    long elapsedTime = System.nanoTime() - startElapsed;
    System.out.printf(" setLogFile total= %d passed=%d%n", stats.total, stats.passed);
    System.out.printf(" elapsed=%f msecs %n", elapsedTime / (1000 * 1000.0));

    mergeTable.setBeans(new ArrayList());
    undoneTable.setBeans(new ArrayList());
    tabbedPanel.setSelectedIndex(0);

    calcMerge = true;
  }

  public List<Merge> filter(List<Merge> allMerge) {

    List<Merge> result = new ArrayList<>(allMerge.size());
    for (Merge m : allMerge) {
      if (currFilter.pass(m))
        result.add(m);
    }
    return result;
  }

  void showInfo(Formatter f) {
    f.format(" Current time =   %s%n%n", new Date().toString());

    int n = 0;
    if (completeLogs != null) {
      n = completeLogs.size();
      f.format("Complete logs n=%d%n", n);
      f.format("  first log date= %s%n", completeLogs.get(0).getDate());
      f.format("   last log date= %s%n", completeLogs.get(n-1).getDate());
    }
    List restrict = mergeTable.getBeans();
    if (restrict != null && (restrict.size() != n)) {
      f.format("%nRestricted, merged logs n=%d%n", restrict.size());
    }

    if (logFiles != null) {
      f.format("%nFiles used%n");
      for (LogLocalManager.FileDateRange fdr : logFiles) {
        f.format(" %s [%s,%s]%n", fdr.f.getName(), fdr.start, fdr.end);
      }
    }
  }

  ////////////////////////////////////////////////////////

  interface MergeFilter  {
    boolean pass(Merge log);
  }

  private static class DateFilter implements MergeFilter {
    long start, end;
    MergeFilter chain;

    public DateFilter(long start, long end, MergeFilter chain) {
      this.start = start;
      this.end = end;
      this.chain = chain;
    }

    public boolean pass(Merge log) {
      if (chain != null && !chain.pass(log))
        return false;

      if ((log.start.date < start) || (log.start.date > end))
        return false;

      return true;
    }
  }

  public static class IpFilter implements MergeFilter {
    String[] match;
    MergeFilter chain;

    public IpFilter(String[] match, MergeFilter chain) {
      this.match = match;
      this.chain = chain;
    }

    public boolean pass(Merge log) {
      if (chain != null && !chain.pass(log))
        return false;

      for (String s : match)
        if (log.getIp().startsWith(s))
          return false;

      return true;
    }
  }

  public static class ErrorOnlyFilter implements MergeFilter {
    MergeFilter chain;

    public ErrorOnlyFilter(MergeFilter chain) {
      this.chain = chain;
    }

    public boolean pass(Merge log) {
      if (chain != null && !chain.pass(log))
        return false;

      int status = log.getStatus();
      if ((status < 400) || (status >= 1000)) return false;

      return true;
    }
  }


  private static class MyClosure implements LogReader.Closure {
    ArrayList<ServletLogParser.ServletLog> logs;

    MyClosure(ArrayList<ServletLogParser.ServletLog> logs) {
      this.logs = logs;
    }

    public void process(LogReader.Log log) {
      if (log instanceof ServletLogParser.ServletLog)
        logs.add((ServletLogParser.ServletLog) log);
      else
        System.out.printf("HEY NULL LOG%n");
    }
  }

 /*  public void setLogFiles(List<File> logFiles) {
    LogReader reader = new LogReader(new ServletLogParser());

    long startElapsed = System.nanoTime();
    LogReader.Stats stats = new LogReader.Stats();

    //  sort on name
    Collections.sort(logFiles, new Comparator<File>() {
      public int compare(File o1, File o2) {
        if (o1.getName().equals("threddsServlet.log")) return 1;
        if (o2.getName().equals("threddsServlet.log")) return -1;
        return o1.getName().compareTo(o2.getName());
      }

      private int getSeq(File f) {
        String name = f.getName();
        int pos = name.indexOf(".log") + 5;
        if (name.length() <= pos) return 0;
        return Integer.parseInt(name.substring(pos));
      }
    });

    try {
      completeLogs = new ArrayList<ServletLogParser.ServletLog>(30000);
      for (File f : logFiles)
        reader.scanLogFile(f, new MyClosure(completeLogs), new MyLogFilter(), stats);
      logTable.setBeans(completeLogs);

    } catch (IOException ioe) {
      ioe.printStackTrace();
      return;
    }
    
    long elapsedTime = System.nanoTime() - startElapsed;
    System.out.printf(" setLogFile total= %d passed=%d%n", stats.total, stats.passed);
    System.out.printf(" elapsed=%f msecs %n", elapsedTime / (1000 * 1000.0));

    mergeTable.setBeans(new ArrayList());
    undoneTable.setBeans(new ArrayList());

    calcMerge = true;
  }   */

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

    ArrayList<ServletLogParser.ServletLog> logs = new ArrayList<>(2);
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

    logs.sort(new Comparator<ServletLogParser.ServletLog>() {
      public int compare(ServletLogParser.ServletLog o1, ServletLogParser.ServletLog o2) {
        long d1 = o1.getDateMillisec();
        long d2 = o2.getDateMillisec();
        return Long.compare(d1, d2);
      }
    });

    completeMerge = new ArrayList<>(logs.size() / 2 + 100);
    ArrayList<ServletLogParser.ServletLog> miscList = new ArrayList<>(1000);
    ArrayList<Uptime> uptimeList = new ArrayList<>(10);
    ArrayList<Merge> undoneList = new ArrayList<>(1000);

    ServletLogParser.ServletLog last = null;
    Uptime current = null;
    for (ServletLogParser.ServletLog log : logs) {
      if (log.getReqSeq() == 0) continue;
      if (current == null) {
        current = new Uptime(log);
        uptimeList.add(current);
        
     } else if ((log.getReqSeq() < 50) && (last.getReqSeq() > 100)) {      // no longer sorted by reqTime 4/22/10
        current.finish(last, undoneList);
        completeMerge.addAll(current.mergeList);
        current = new Uptime(log);
        uptimeList.add(current);
      }

      last = log;
      current.add(log, miscList);
    }

    if (current != null) {
      current.finish(last, undoneList);
      completeMerge.addAll(current.mergeList);
    }

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
      mergeList = new ArrayList<>(n);
      map = new HashMap<>(n);
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

  /*
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
  }  */


}
