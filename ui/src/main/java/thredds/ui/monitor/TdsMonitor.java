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

package thredds.ui.monitor;

import org.apache.http.client.CredentialsProvider;
import ucar.httpservices.*;
import thredds.logs.LogReader;
import thredds.logs.LogCategorizer;
import ucar.nc2.ui.widget.*;
import ucar.nc2.util.IO;
import ucar.nc2.util.net.HttpClientManager;
import ucar.util.prefs.ui.ComboBox;
import ucar.util.prefs.ui.Debug;
import ucar.util.prefs.PreferencesExt;
import ucar.util.prefs.XMLStore;

//import org.apache.oro.io.GlobFilenameFilter;

import java.awt.event.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.*;
import java.awt.*;
import java.util.*;

import javax.swing.*;

import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Cache;
import net.sf.ehcache.Statistics;

/**
 * Manage TDS logs
 *
 * @author caron
 * @since Mar 26, 2009
 */
public class TdsMonitor extends JPanel {
  static private final String FRAME_SIZE = "FrameSize";

  private static JFrame frame;
  private static PreferencesExt prefs;
  private static XMLStore store;

  private ucar.util.prefs.PreferencesExt mainPrefs;
  private JTabbedPane tabbedPane;
  private ManagePanel managePanel;
  private AccessLogPanel accessLogPanel;
  private ServletLogPanel servletLogPanel;
  private URLDumpPane urlDump;

  private JFrame parentFrame;
  private FileManager fileChooser;
  private ManageForm manage;

  //private HTTPSession session;
  // private CredentialsProvider provider;

  public TdsMonitor(ucar.util.prefs.PreferencesExt prefs, JFrame parentFrame) throws HTTPException {
    this.mainPrefs = prefs;
    this.parentFrame = parentFrame;

    makeCache();

    fileChooser = new FileManager(parentFrame, null, null, (PreferencesExt) prefs.node("FileManager"));

    // the top UI
    tabbedPane = new JTabbedPane(JTabbedPane.TOP);
    managePanel = new ManagePanel((PreferencesExt) mainPrefs.node("ManageLogs"));
    accessLogPanel = new AccessLogPanel((PreferencesExt) mainPrefs.node("LogTable"));
    servletLogPanel = new ServletLogPanel((PreferencesExt) mainPrefs.node("ServletLogPanel"));
    urlDump = new URLDumpPane((PreferencesExt) mainPrefs.node("urlDump"));

    tabbedPane.addTab("ManageLogs", managePanel);
    tabbedPane.addTab("AccessLogs", accessLogPanel);
    tabbedPane.addTab("ServletLogs", servletLogPanel);
    tabbedPane.addTab("UrlDump", urlDump);
    tabbedPane.setSelectedIndex(0);

    setLayout(new BorderLayout());
    add(tabbedPane, BorderLayout.CENTER);

    CredentialsProvider provider = new UrlAuthenticatorDialog(null);
    try {
      HTTPSession.setGlobalCredentialsProvider(provider);
    } catch(HTTPException e) {
      System.err.println("Failed to set credentials");
    }
    HTTPSession.setGlobalUserAgent("TdsMonitor");
  }

  public void exit() {
    if (dnsCache != null) {
      System.out.printf(" cache= %s%n", dnsCache.toString());
      System.out.printf(" cache.size= %d%n", dnsCache.getSize());
      System.out.printf(" cache.memorySize= %d%n", dnsCache.getMemoryStoreSize());
      Statistics stats = dnsCache.getStatistics();
      System.out.printf(" stats= %s%n", stats.toString());
    }

    cacheManager.shutdown();
    fileChooser.save();
    managePanel.save();
    accessLogPanel.save();
    servletLogPanel.save();
    urlDump.save();

    Rectangle bounds = frame.getBounds();
    prefs.putBeanObject(FRAME_SIZE, bounds);
    try {
      store.save();
    } catch (IOException ioe) {
      ioe.printStackTrace();
    }

    done = true; // on some systems, still get a window close event
    System.exit(0);
  }


  private void gotoUrlDump(String urlString) {
    urlDump.setURL(urlString);
    tabbedPane.setSelectedIndex(3);
  }


  ////////////////////////////////////////////////////////////////////////////////////

  static private TdsMonitor ui;
  static private boolean done = false;

  static private File ehLocation = LogLocalManager.getDirectory("cache", "dns");

  //private static String ehLocation = "C:\\data\\ehcache";
  //private static String ehLocation = "/machine/data/thredds/ehcache/";
  private static String config =
          "<ehcache>\n" +
                  "    <diskStore path='" + ehLocation.getPath() + "'/>\n" +
                  "    <defaultCache\n" +
                  "              maxElementsInMemory='10000'\n" +
                  "              eternal='false'\n" +
                  "              timeToIdleSeconds='120'\n" +
                  "              timeToLiveSeconds='120'\n" +
                  "              overflowToDisk='true'\n" +
                  "              maxElementsOnDisk='10000000'\n" +
                  "              diskPersistent='false'\n" +
                  "              diskExpiryThreadIntervalSeconds='120'\n" +
                  "              memoryStoreEvictionPolicy='LRU'\n" +
                  "              />\n" +
                  "    <cache name='dns'\n" +
                  "            maxElementsInMemory='5000'\n" +
                  "            eternal='false'\n" +
                  "            timeToIdleSeconds='86400'\n" +
                  "            timeToLiveSeconds='864000'\n" +
                  "            overflowToDisk='true'\n" +
                  "            maxElementsOnDisk='0'\n" +
                  "            diskPersistent='true'\n" +
                  "            diskExpiryThreadIntervalSeconds='3600'\n" +
                  "            memoryStoreEvictionPolicy='LRU'\n" +
                  "            />\n" +
                  "</ehcache>";

  private CacheManager cacheManager;
  private Cache dnsCache;

  void makeCache() {
    cacheManager = new CacheManager(new StringBufferInputStream(config));
    dnsCache = cacheManager.getCache("dns");
  }

  /////////////////////////

  private class ManagePanel extends JPanel {
    PreferencesExt prefs;

    ManagePanel(PreferencesExt p) {
      this.prefs = p;
      manage = new ManageForm(this.prefs);
      setLayout(new BorderLayout());
      add(manage, BorderLayout.CENTER);

      manage.addPropertyChangeListener(new PropertyChangeListener() {
        public void propertyChange(PropertyChangeEvent evt) {
          if (!evt.getPropertyName().equals("Download")) return;
          ManageForm.Data data = (ManageForm.Data) evt.getNewValue();
          try {
            manage.getTextArea().setText(""); // clear the text area
            manage.getStopButton().setCancel(false); // clear the cancel state

            if (data.wantAccess) {
              TdsDownloader logManager = new TdsDownloader(manage.getTextArea(), data, TdsDownloader.Type.access);
              logManager.getRemoteFiles(manage.getStopButton());
            }
            if (data.wantServlet) {
              TdsDownloader logManager = new TdsDownloader(manage.getTextArea(), data, TdsDownloader.Type.thredds);
              logManager.getRemoteFiles(manage.getStopButton());
            }

            if (data.wantRoots) {
              String urls = data.getServerPrefix() + "/thredds/admin/log/dataroots.txt";
              File localDir = LogLocalManager.getDirectory(data.server, "");
              boolean ok = localDir.mkdirs();
              // if (!ok) manage.getTextArea().append("\nmkdirs failed");
              File file = new File(localDir, "roots.txt");
              HTTPSession session = HTTPFactory.newSession(urls);
              // session.setCredentialsProvider(provider);
              session.setUserAgent("TdsMonitor");
              JTextArea ta = manage.getTextArea();

              try {
                HttpClientManager.copyUrlContentsToFile(session, urls, file);
                String roots = IO.readFile(file.getPath());
                ta.append("\nRoots:\n");
                ta.append(roots);
                LogCategorizer.setRoots(roots);

              } catch (IOException ioe) {
                StringWriter sw = new StringWriter(5000);
                ioe.printStackTrace(new PrintWriter(sw));
                ta.setText(sw.toString());
              }
            }

          } catch (Throwable t) {
            t.printStackTrace();
          }

          if (manage.getStopButton().isCancel())
            manage.getTextArea().append("\nDownload canceled by user");
        }
      });

    }

    void save() {
      ComboBox servers = manage.getServersCB();
      servers.save();
    }
  }

  ///////////////////////////

  private abstract class OpPanel extends JPanel {
    PreferencesExt prefs;
    TextHistoryPane ta;
    IndependentWindow infoWindow;
    JComboBox serverCB;
    JTextArea startDateField, endDateField;
    JPanel topPanel;
    boolean isAccess;
    boolean removeTestReq;
    boolean problemsOnly;

    OpPanel(PreferencesExt prefs, boolean isAccess) {
      this.prefs = prefs;
      this.isAccess = isAccess;
      ta = new TextHistoryPane(true);
      infoWindow = new IndependentWindow("Details", BAMutil.getImage("netcdfUI"), new JScrollPane(ta));
      Rectangle bounds = (Rectangle) prefs.getBean(FRAME_SIZE, new Rectangle(200, 50, 500, 700));
      infoWindow.setBounds(bounds);

      topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));

      // which server
      serverCB = new JComboBox();
      serverCB.setModel(manage.getServersCB().getModel());
      serverCB.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          String server = (String) serverCB.getSelectedItem();
          setServer(server);
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

      AbstractAction showAction = new AbstractAction() {
        public void actionPerformed(ActionEvent e) {
          showLogs();
        }
      };
      BAMutil.setActionProperties(showAction, "Import", "get logs", false, 'G', -1);
      BAMutil.addActionToContainer(topPanel, showAction);

      AbstractAction filterAction = new AbstractAction() {
        public void actionPerformed(ActionEvent e) {
          Boolean state = (Boolean) getValue(BAMutil.STATE);
          removeTestReq = state.booleanValue();
        }
      };
      BAMutil.setActionProperties(filterAction, "time", "remove test Requests", true, 'F', -1);
      BAMutil.addActionToContainer(topPanel, filterAction);

      AbstractAction filter2Action = new AbstractAction() {
        public void actionPerformed(ActionEvent e) {
          Boolean state = (Boolean) getValue(BAMutil.STATE);
          problemsOnly = state.booleanValue();
        }
      };
      BAMutil.setActionProperties(filter2Action, "time", "only show problems", true, 'F', -1);
      BAMutil.addActionToContainer(topPanel, filter2Action);

      AbstractAction infoAction = new AbstractAction() {
        public void actionPerformed(ActionEvent e) {
          Formatter f = new Formatter();
          showInfo(f);
          ta.setText(f.toString());
          infoWindow.show();
        }
      };
      BAMutil.setActionProperties(infoAction, "Information", "info on selected logs", false, 'I', -1);
      BAMutil.addActionToContainer(topPanel, infoAction);

      setLayout(new BorderLayout());
      add(topPanel, BorderLayout.NORTH);
    }

    private LogLocalManager manager;

    public void setServer(String server) {
      manager = new LogLocalManager(server, isAccess);
      manager.getLocalFiles(null, null);
      setLocalManager(manager);
    }

    abstract void setLocalManager(LogLocalManager manager);

    abstract void showLogs();

    abstract void showInfo(Formatter f);

    abstract void resetLogs();

    void save() {
      if (infoWindow != null) prefs.putBeanObject(FRAME_SIZE, infoWindow.getBounds());
    }
  }


  /////////////////////////////////////////////////////////////////////
  String filterIP = "128.117.156,128.117.140,128.117.149";

  private class AccessLogPanel extends OpPanel {
    AccessLogTable logTable;

    AccessLogPanel(PreferencesExt p) {
      super(p, true);
      logTable = new AccessLogTable(startDateField, endDateField, p, dnsCache);
      logTable.addPropertyChangeListener(new java.beans.PropertyChangeListener() {
        public void propertyChange(java.beans.PropertyChangeEvent e) {
          if (e.getPropertyName().equals("UrlDump")) {
            String path = (String) e.getNewValue();
            gotoUrlDump(path);
          }
        }
      });

      AbstractAction allAction = new AbstractAction() {
        public void actionPerformed(ActionEvent e) {
          resetLogs();
        }
      };
      BAMutil.setActionProperties(allAction, "Refresh", "show All Logs", false, 'A', -1);
      BAMutil.addActionToContainer(topPanel, allAction);

      AbstractAction dnsAction = new AbstractAction() {
        public void actionPerformed(ActionEvent e) {
          showDNS();
        }
      };
      BAMutil.setActionProperties(dnsAction, "Dataset", "lookup DNS", false, 'D', -1);
      BAMutil.addActionToContainer(topPanel, dnsAction);

      add(logTable, BorderLayout.CENTER);
    }

    @Override
    void setLocalManager(LogLocalManager manager) {
      logTable.setLocalManager(manager);
    }

    @Override
    void showLogs() {
      LogReader.LogFilter filter = null;
      if (removeTestReq)
        filter = new LogReader.IpFilter(filterIP.split(","), filter);
      if (problemsOnly)
        filter = new LogReader.ErrorOnlyFilter(filter);

      logTable.showLogs(filter);
    }

    void showInfo(Formatter f) {
      logTable.showInfo(f);
    }

    void resetLogs() {
      logTable.resetLogs();
    }

    void showDNS() {
      logTable.showDNS();
    }

    void save() {
      logTable.exit();
      super.save();
    }
  }

  /////////////////////////////////////////////////////////////////////

  private class ServletLogPanel extends OpPanel {
    ServletLogTable logTable;

    ServletLogPanel(PreferencesExt p) {
      super(p, false);
      logTable = new ServletLogTable(startDateField, endDateField, p, dnsCache);
      add(logTable, BorderLayout.CENTER);
    }

    @Override
    void setLocalManager(LogLocalManager manager) {
      logTable.setLocalManager(manager);
    }

    @Override
    void showLogs() {
      ServletLogTable.MergeFilter filter = null;
      if (removeTestReq)
        filter = new ServletLogTable.IpFilter(filterIP.split(","), filter);
      if (problemsOnly)
        filter = new ServletLogTable.ErrorOnlyFilter(filter);

      logTable.showLogs(filter);
    }

    void resetLogs() {
    }


    void showInfo(Formatter f) {
      logTable.showInfo(f);
    }

    void save() {
      logTable.exit();
      super.save();
    }
  }

  //////////////////////////////////////////////////////////////


  /*
   * Finds all files matching
   * a glob pattern.  This method recursively searches directories, allowing
   * for glob expressions like {@code "c:\\data\\200[6-7]\\*\\1*\\A*.nc"}.
   *
   * @param globExpression The glob expression
   * @return List of File objects matching the glob pattern.  This will never
   *         be null but might be empty
   * @throws Exception if the glob expression does not represent an absolute
   *                   path
   * @author Mike Grant, Plymouth Marine Labs; Jon Blower
   *
  java.util.List<File> globFiles(String globExpression) throws Exception {
    // Check that the glob expression is an absolute path.  Relative paths
    // would cause unpredictable and platform-dependent behaviour so
    // we disallow them.
    // If ds.getLocation() is a glob expression this test will still work
    // because we are not attempting to resolve the string to a real path.
    File globFile = new File(globExpression);
    if (!globFile.isAbsolute()) {
      throw new Exception("Dataset location " + globExpression +
              " must be an absolute path");
    }

    // Break glob pattern into path components.  To do this in a reliable
    // and platform-independent way we use methods of the File class, rather
    // than String.split().
    java.util.List<String> pathComponents = new ArrayList<String>();
    while (globFile != null) {
      // We "pop off" the last component of the glob pattern and place
      // it in the first component of the pathComponents List.  We therefore
      // ensure that the pathComponents end up in the right order.
      File parent = globFile.getParentFile();
      // For a top-level directory, getName() returns an empty string,
      // hence we use getPath() in this case
      String pathComponent = parent == null ? globFile.getPath() : globFile.getName();
      pathComponents.add(0, pathComponent);
      globFile = parent;
    }

    // We must have at least two path components: one directory and one
    // filename or glob expression
    java.util.List<File> searchPaths = new ArrayList<File>();
    searchPaths.add(new File(pathComponents.get(0)));
    int i = 1; // Index of the glob path component

    while (i < pathComponents.size()) {
      FilenameFilter globFilter = new GlobFilenameFilter(pathComponents.get(i));
      java.util.List<File> newSearchPaths = new ArrayList<File>();
      // Look for matches in all the current search paths
      for (File dir : searchPaths) {
        if (dir.isDirectory()) {
          // Workaround for automounters that don't make filesystems
          // appear unless they're poked
          // do a listing on searchpath/pathcomponent whether or not
          // it exists, then discard the results
          new File(dir, pathComponents.get(i)).list();

          for (File match : dir.listFiles(globFilter)) {
            newSearchPaths.add(match);
          }
        }
      }
      // Next time we'll search based on these new matches and will use
      // the next globComponent
      searchPaths = newSearchPaths;
      i++;
    }

    // Now we've done all our searching, we'll only retain the files from
    // the list of search paths
    java.util.List<File> filesToReturn = new ArrayList<File>();
    for (File path : searchPaths) {
      if (path.isFile()) filesToReturn.add(path);
    }

    return filesToReturn;
  } */

  //////////////////////////////////////////////

  public static void main(String args[]) throws HTTPException {

    // prefs storage
    try {
      String prefStore = ucar.util.prefs.XMLStore.makeStandardFilename(".unidata", "TdsMonitor.xml");
      store = ucar.util.prefs.XMLStore.createFromFile(prefStore, null);
      prefs = store.getPreferences();
      Debug.setStore(prefs.node("Debug"));
    } catch (IOException e) {
      System.out.println("XMLStore Creation failed " + e);
    }

    // initializations
    BAMutil.setResourcePath("/resources/nj22/ui/icons/");

    // put UI in a JFrame
    frame = new JFrame("TDS Monitor");
    ui = new TdsMonitor(prefs, frame);

    frame.setIconImage(BAMutil.getImage("netcdfUI"));
    frame.addWindowListener(new WindowAdapter() {
      public void windowClosing(WindowEvent e) {
        if (!done) ui.exit();
      }
    });

    frame.getContentPane().add(ui);
    Rectangle bounds = (Rectangle) prefs.getBean(FRAME_SIZE, new Rectangle(50, 50, 800, 450));
    frame.setBounds(bounds);

    frame.pack();
    frame.setBounds(bounds);
    frame.setVisible(true);
  }
}
