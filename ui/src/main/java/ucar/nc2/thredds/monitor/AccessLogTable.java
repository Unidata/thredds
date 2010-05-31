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
import ucar.nc2.units.TimeUnit;

import javax.swing.*;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ChangeEvent;

import thredds.ui.TextHistoryPane;
import thredds.ui.IndependentWindow;
import thredds.ui.BAMutil;
import thredds.logs.AccessLogParser;
import thredds.logs.TestFileSystem;
import thredds.logs.LogReader;

import java.util.*;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.io.File;
import java.io.FileFilter;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.text.ParseException;

import net.sf.ehcache.Cache;
import net.sf.ehcache.Element;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.Minute;

/**
 * Class Description.
 *
 * @author caron
 * @since Mar 26, 2009
 */
public class AccessLogTable extends JPanel {
  private PreferencesExt prefs;
  private Cache dnsCache;

  private ucar.util.prefs.ui.BeanTableSorted logTable, userTable, datarootTable, serviceTable;
  private JPanel timeSeriesPanel;

  private ArrayList<LogReader.Log> completeLogs;
  private ArrayList<LogReader.Log> restrictLogs;

  private boolean calcUser = true;
  private boolean calcRoot = true;
  private boolean calcService = true;

  private JTabbedPane tabbedPanel;

  private TextHistoryPane infoTA;
  private IndependentWindow infoWindow;

  private JTextArea startDateField, endDateField;

  public AccessLogTable(JTextArea startDateField, JTextArea endDateField, PreferencesExt prefs, Cache dnsCache) {
    this.startDateField = startDateField;
    this.endDateField = endDateField;
    this.prefs = prefs;
    this.dnsCache = dnsCache;

    logTable = new BeanTableSorted(LogReader.Log.class, (PreferencesExt) prefs.node("Logs"), false);
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

    varPopup.addAction("Resend URL", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        LogReader.Log log = (LogReader.Log) logTable.getSelectedBean();
        if (log == null) return;
        String urlString = log.getPath();
        AccessLogTable.this.firePropertyChange("UrlDump", null, "http://" + manager.getServer() + urlString);
      }
    });

    varPopup.addAction("Remove selected logs", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        List all = logTable.getBeans();
        List selected = logTable.getSelectedBeans();
        all.removeAll(selected);
        logTable.setBeans(all);
      }
    });

    userTable = new BeanTableSorted(User.class, (PreferencesExt) prefs.node("LogUser"), false);
    userTable.addListSelectionListener(new ListSelectionListener() {
      public void valueChanged(ListSelectionEvent e) {
        User accum = (User) userTable.getSelectedBean();
        if (accum == null) return;
        accum.run();
      }
    });

    thredds.ui.PopupMenu varPopupU = new thredds.ui.PopupMenu(userTable.getJTable(), "Options");
    varPopupU.addAction("User requests", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        Accum accum = (Accum) userTable.getSelectedBean();
        if (accum == null) return;
        logTable.setBeans(accum.logs);
        tabbedPanel.setSelectedIndex(0);
      }
    });

    datarootTable = new BeanTableSorted(Dataroot.class, (PreferencesExt) prefs.node("DataRoot"), false);
    datarootTable.addListSelectionListener(new ListSelectionListener() {
      public void valueChanged(ListSelectionEvent e) {
        Dataroot accum = (Dataroot) datarootTable.getSelectedBean();
        if (accum == null) return;
      }
    });

    thredds.ui.PopupMenu varPopupR = new thredds.ui.PopupMenu(datarootTable.getJTable(), "Options");
    varPopupR.addAction("User requests", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        Accum accum = (Accum) datarootTable.getSelectedBean();
        if (accum == null) return;
        logTable.setBeans(accum.logs);
        tabbedPanel.setSelectedIndex(0);
      }
    });

    serviceTable = new BeanTableSorted(Service.class, (PreferencesExt) prefs.node("Service"), false);
    serviceTable.addListSelectionListener(new ListSelectionListener() {
      public void valueChanged(ListSelectionEvent e) {
        Service accum = (Service) serviceTable.getSelectedBean();
        if (accum == null) return;
      }
    });

    thredds.ui.PopupMenu varPopupS = new thredds.ui.PopupMenu(serviceTable.getJTable(), "Options");
    varPopupS.addAction("User requests", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        Accum accum = (Accum) serviceTable.getSelectedBean();
        if (accum == null) return;
        logTable.setBeans(accum.logs);
        tabbedPanel.setSelectedIndex(0);
      }
    });

    timeSeriesPanel = new JPanel();
    //timeSeriesPanel.setLayout(new GridLayout(2, 2));
    timeSeriesPanel.setLayout(new BorderLayout());

    // the info window
    infoTA = new TextHistoryPane();
    infoWindow = new IndependentWindow("Extra Information", BAMutil.getImage("netcdfUI"), infoTA);
    infoWindow.setBounds((Rectangle) prefs.getBean("InfoWindowBounds", new Rectangle(300, 300, 800, 100)));


    //////////////////////////////////////////////////////
    // top UI

    /*
    JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));

    AbstractAction showAction = new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        showLogs();
      }
    };
    BAMutil.setActionProperties(showAction, "Import", "get logs", false, 'D', -1);
    BAMutil.addActionToContainer(topPanel, showAction);

    // which server
    final JComboBox serverCB = new JComboBox();
    serverCB.setModel(manage.getServers().getModel());
    serverCB.addActionListener(new ActionListener() {
       public void actionPerformed(ActionEvent e) {
         String server = (String) serverCB.getSelectedItem();
         //serverCB.addItem(server);
         try {
           setServer(server);
           //serverCB.addItem(server);
         } catch (IOException e1) {
           e1.printStackTrace();
         }
       }
     });

    // serverCB.setModel(manage.getServers().getModel());
    topPanel.add(new JLabel("server:"));
    topPanel.add(serverCB);

    // the date selectors
    startDateField = new JTextArea("                    ");
    endDateField = new JTextArea("                    ");

    topPanel.add(new JLabel("Start Date:"));
    topPanel.add(startDateField);
    topPanel.add(new JLabel("End Date:"));
    topPanel.add(endDateField);

    /* AbstractAction restrictAction = new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        restrictLogs(startDateField.getText().trim(), endDateField.getText().trim());
      }
    };
    BAMutil.setActionProperties(restrictAction, "Select", "select by date", false, 'D', -1);
    BAMutil.addActionToContainer(topPanel, restrictAction);

    AbstractAction allAction = new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        resetLogs();
      }
    };
    BAMutil.setActionProperties(allAction, "Refresh", "show All Logs", false, 'A', -1);
    BAMutil.addActionToContainer(topPanel, allAction);

    AbstractAction dnsAction = new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        reverseDNS();
        userTable.getJTable().repaint();
      }
    };
    BAMutil.setActionProperties(dnsAction, "Dataset", "lookup DNS", false, 'D', -1);
    BAMutil.addActionToContainer(topPanel, dnsAction);  */

    // tabbed panes
    tabbedPanel = new JTabbedPane(JTabbedPane.TOP);
    tabbedPanel.addTab("LogTable", logTable);
    tabbedPanel.addTab("User", userTable);
    tabbedPanel.addTab("DataRoot", datarootTable);
    tabbedPanel.addTab("Service", serviceTable);
    tabbedPanel.addTab("TimeSeries", timeSeriesPanel);
    tabbedPanel.setSelectedIndex(0);

    tabbedPanel.addChangeListener(new ChangeListener() {
      public void stateChanged(ChangeEvent e) {
        java.util.ArrayList<LogReader.Log> useBeans = (java.util.ArrayList<LogReader.Log>) logTable.getBeans();

        int idx = tabbedPanel.getSelectedIndex();
        String title = tabbedPanel.getTitleAt(idx);
        if (title.equals("User"))
          initUserLogs(useBeans);
        if (title.equals("DataRoot"))
          initDatarootLogs(useBeans);
        if (title.equals("Service"))
          initServiceLogs(useBeans);
        if (title.equals("TimeSeries"))
          showTimeSeriesAll( useBeans);
      }
    });

    setLayout(new BorderLayout());
    //add(topPanel, BorderLayout.NORTH);
    add(tabbedPanel, BorderLayout.CENTER);
  }

  public void exit() {
    if (executor != null)
      executor.shutdownNow();

    logTable.saveState(false);
    userTable.saveState(false);
    datarootTable.saveState(false);
    serviceTable.saveState(false);
    prefs.putBeanObject("InfoWindowBounds", infoWindow.getBounds());
  }


  private SimpleDateFormat df = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
  private LogLocalManager manager;
  private java.util.List<LogLocalManager.FileDateRange> accessLogFiles = null;

  public void setLocalManager( LogLocalManager manager) {
    this.manager = manager;

    Date startDate = manager.getStartDate();
    Date endDate = manager.getEndDate();
    startDateField.setText(df.format(startDate));
    endDateField.setText(df.format(endDate));
  }

  void showLogs(LogReader.LogFilter filter) {
    Date start, end;
    try {
      start = df.parse(startDateField.getText());
      end = df.parse(endDateField.getText());
      accessLogFiles = manager.getLocalFiles(start, end);
    } catch (Exception e) {
      e.printStackTrace();
      return;
    }

    LogReader reader = new LogReader(new AccessLogParser());
    completeLogs = new ArrayList<LogReader.Log>(30000);

    filter = new LogReader.DateFilter(start.getTime(), end.getTime(), filter);

    try {
      long startElapsed = System.nanoTime();
      LogReader.Stats stats = new LogReader.Stats();

      for (LogLocalManager.FileDateRange fdr : accessLogFiles)
        reader.scanLogFile(fdr.f, new MyClosure(completeLogs), filter, stats);

      long elapsedTime = System.nanoTime() - startElapsed;
       System.out.printf(" setLogFile total= %d passed=%d%n", stats.total, stats.passed);
       System.out.printf(" elapsed=%f msecs %n", elapsedTime / (1000 * 1000.0));

    } catch (IOException ioe) {
      ioe.printStackTrace();
      return;
    }

    resetLogs();
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
    List restrict = logTable.getBeans();
    if (restrict != null && (restrict.size() != n)) {
      f.format("%nRestricted logs n=%d%n", restrict.size());
    }
    if (accessLogFiles != null)
      f.format("%nFiles used%n");
      for (LogLocalManager.FileDateRange fdr : accessLogFiles) {
        f.format(" %s [%s,%s]%n", fdr.f.getName(), fdr.start, fdr.end);
      }
  }

  void resetLogs() {
    logTable.setBeans(completeLogs);
    tabbedPanel.setSelectedIndex(0);

    userTable.setBeans(new ArrayList());
    datarootTable.setBeans(new ArrayList());
    calcUser = true;
    calcRoot = true;
    calcService = true;
    restrictLogs = completeLogs;

    /* int n = completeLogs.size();
    if (n > 0) {
      startDateField.setText(completeLogs.get(0).getDate());
      endDateField.setText(completeLogs.get(n-1).getDate());
    } */
  }

  void restrictLogs(String restrict) {

    restrictLogs = new ArrayList<LogReader.Log>(1000);
    for (LogReader.Log log : completeLogs) {
      String ip = log.getIp();
      if (ip.startsWith(restrict)) continue;
      restrictLogs.add(log);
    }

    logTable.setBeans(restrictLogs);
    tabbedPanel.setSelectedIndex(0);
    
    userTable.setBeans(new ArrayList());
    datarootTable.setBeans(new ArrayList());
    calcUser = true;
    calcRoot = true;
    calcService = true;
  }

  ////////////////////////////////////////////////////////
  class MyClosure implements LogReader.Closure {
    ArrayList<LogReader.Log> logs;

    MyClosure(ArrayList<LogReader.Log> logs) {
      this.logs = logs;
    }

    public void process(LogReader.Log log) {
      logs.add(log);
    }
  }

  ////////////////////////////////////////////////

  public class Accum {
    public String getName() {
      return name;
    }

    public long getMsecs() {
      return msecs;
    }

    public long getMsecsPerRequest() {
      return msecs / count;
    }

    public long getKbytes() {
      return bytes / 1000;
    }

    public int getCount() {
      return count;
    }

    ArrayList<LogReader.Log> logs = new ArrayList<LogReader.Log>(100);
    String name;
    long msecs;
    long bytes;
    int count;

    public Accum() {
    }

    Accum(String name) {
      this.name = name;
    }

    void add(LogReader.Log log) {
      logs.add(log);
      count++;
      bytes += log.getBytes();
      msecs += log.getMsecs();
    }
  }


  public class User extends Accum implements Runnable {
    String ip;

    public String getIp() {
      return ip;
    }

    public User() {
      super();
    }

    User(String ip) {
      this.ip = ip;
    }

    public void run() {
      if (name != null) return;
      try {
        long startElapsed = System.nanoTime();
        name = reverseDNS(ip);
        long elapsedTime = System.nanoTime() - startElapsed;
        if (showDNStime) System.out.printf(" reverseDNS took=%f msecs %n", elapsedTime / (1000 * 1000.0));
      } catch (Throwable e) {
        name = e.getMessage();
      }
    }
  }

  private boolean showDNStime = false;

  void initUserLogs(ArrayList<LogReader.Log> logs) {
    if (!calcUser) return;
    if (logs == null) return;

    HashMap<String, User> map = new HashMap<String, User>();

    for (LogReader.Log log : logs) {
      User accum = map.get(log.getIp());
      if (accum == null) {
        accum = new User(log.getIp());
        map.put(log.getIp(), accum);
      }
      accum.add(log);
    }

    userTable.setBeans(new ArrayList(map.values()));
    calcUser = false;
  }

  private ExecutorService executor = null;

  void showDNS() {

    if (null == executor) executor = Executors.newFixedThreadPool(3); // number of threads
    ArrayList<User> accums = (ArrayList<User>) userTable.getBeans();
    for (User a : accums) {
      executor.execute(a);
    }

    /* boolean ok = false;
    try {
      ok = executor.awaitTermination(3, TimeUnit.SECONDS);
    } catch (InterruptedException e) {
      e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
    }
    long elapsedTime = System.nanoTime() - startElapsed;
    System.out.printf(" reverseDNS took=%f msecs ok=%s %n", elapsedTime / (1000 * 1000.0), ok);  */
  }

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

  public class Dataroot extends Accum {
    public Dataroot() {
      super();
    }

    Dataroot(String name) {
      super(name);
    }

  }

  void initDatarootLogs(ArrayList<LogReader.Log> logs) {
    if (!calcRoot) return;
    if (logs == null) return;

    HashMap<String, Dataroot> map = new HashMap<String, Dataroot>();

    for (LogReader.Log log : logs) {
      String path = log.getPath();
      String dataRoot = TestFileSystem.getDataroot(path);
      Dataroot accum = map.get(dataRoot);
      if (accum == null) {
        accum = new Dataroot(dataRoot);
        map.put(dataRoot, accum);
      }
      accum.add(log);
    }

    datarootTable.setBeans(new ArrayList(map.values()));
    calcRoot = false;
  }

  ////////////////////////////////////////////////

  public class Service extends Accum {

    public Service() {
      super();
    }

    Service(String name) {
      super(name);
    }
  }

  void initServiceLogs(ArrayList<LogReader.Log> logs) {
    if (!calcService) return;
    if (logs == null) return;

    HashMap<String, Service> map = new HashMap<String, Service>();

    for (LogReader.Log log : logs) {
      String path = log.getPath();
        String service = TestFileSystem.getService(path);
        Service accum = map.get(service);
        if (accum == null) {
          accum = new Service(service);
          map.put(service, accum);
        }
        accum.add(log);
    }

    serviceTable.setBeans(new ArrayList(map.values()));
    calcService = false;
  }

  //////////////////

  private void showTimeSeries(java.util.List<LogReader.Log> logs) {
    // 09/Apr/2009:16:38:28 -0600
    SimpleDateFormat df = new SimpleDateFormat("dd/MMM/yyyy:HH:mm:ss Z");
    SimpleDateFormat dfo = new SimpleDateFormat("yyyy/MMM/dd HH:mm:ss");

    TimeSeries bytesSentData = new TimeSeries("Bytes Sent", Minute.class);
    TimeSeries timeTookData = new TimeSeries("Average Latency", Minute.class);
    TimeSeries nreqData = new TimeSeries("Number of Requests", Minute.class);

    long period = 1000 * 60 * 5; // 5 min
    long current = 0;
    long bytes = 0;
    long timeTook = 0;
    long count = 0;
    try {
      for (LogReader.Log log : logs) {
        Date d = df.parse(log.getDate());
        long msecs = d.getTime();
        if (msecs - current > period) {
          if (current > 0) {
            addPoint(bytesSentData, timeTookData, nreqData, new Date(current), bytes, count, timeTook);
          }
          bytes = 0;
          count = 0;
          timeTook = 0;
          current = msecs;
        }
        bytes += log.getBytes();
        timeTook += log.getMsecs();
        count++;
      }
      addPoint(bytesSentData, timeTookData, nreqData, new Date(current), bytes, count, timeTook);

    } catch (ParseException e) {
      e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
    }


    Chart c1 = new Chart("Bytes Sent", "5 min average", "Mbytes/sec", bytesSentData);
    Chart c2 = new Chart("Average Latency", "5 min average", "Millisecs", timeTookData);
    Chart c3 = new Chart("Number of Requests/sec", "5 min average", "", nreqData);

    timeSeriesPanel.removeAll();
    timeSeriesPanel.add(c1);
    timeSeriesPanel.add(c2);
    timeSeriesPanel.add(c3);

  }

  // construct the TImeSeries plot for the list of logs passed in
  private void showTimeSeriesAll(java.util.List<LogReader.Log> logs) {
    TimeSeries bytesSentData = new TimeSeries("Bytes Sent", Minute.class);
    TimeSeries timeTookData = new TimeSeries("Average Latency", Minute.class);
    TimeSeries nreqData = new TimeSeries("Number of Requests", Minute.class);

    String intervalS = "5 minute"; // interval.getText().trim();
    // if (intervalS.length() == 0) intervalS = "5 minute";
    long period = 1000 * 60 * 5;
    try {
      TimeUnit tu = new TimeUnit(intervalS);
      period = (long) (1000 * tu.getValueInSeconds());
    } catch (Exception e) {
      System.out.printf("Illegal Time interval=%s %n", intervalS);
    }

    long current = 0;
    long bytes = 0;
    long timeTook = 0;
    long total_count = 0;
    long count = 0;
    for (LogReader.Log log : logs) {
      long msecs = log.date;
      if (msecs - current > period) {
        if (current > 0) {
          total_count += count;
          addPoint(bytesSentData, timeTookData, nreqData, new Date(current), bytes, count, timeTook);
        }
        bytes = 0;
        count = 0;
        timeTook = 0;
        current = msecs;
      }
      bytes += log.getBytes();
      timeTook += log.getMsecs();
      count++;
    }
    addPoint(bytesSentData, timeTookData, nreqData, new Date(current), bytes, count, timeTook);
    total_count += count;
    System.out.printf("showTimeSeriesAll: total_count = %d logs = %d%n", total_count, logs.size());

    MultipleAxisChart mc = new MultipleAxisChart("Access Logs", intervalS + " average", "Mbytes Sent", bytesSentData);
    mc.addSeries("Number of Requests", nreqData);
    mc.addSeries("Average Latency (secs)", timeTookData);
    mc.finish(new java.awt.Dimension(1000, 1000));

    //MultipleAxisChart mc = new MultipleAxisChart("Bytes Sent", "5 min average", "Mbytes/sec", bytesSentData);
    //Chart c2 = new Chart("Average Latency", "5 min average", "Millisecs", timeTookData);
    //Chart c3 = new Chart("Number of Requests/sec", "5 min average", "", nreqData);

    timeSeriesPanel.removeAll();
    timeSeriesPanel.add(mc);
  }

  void addPoint(TimeSeries bytesSentData, TimeSeries timeTookData, TimeSeries nreqData,
          Date date, long bytes, long count, long timeTook) {

    bytesSentData.add(new Minute(date), bytes / 1000. / 1000.);
    double latency = (double) timeTook / count / 1000.;
    //timeTookData.add(new Minute(date), (latency > 10*1000) ? 0 : latency); // note latency limited to 10 secs.
    timeTookData.add(new Minute(date), latency); 
    nreqData.add(new Minute(date), (double) count);
  }

}
