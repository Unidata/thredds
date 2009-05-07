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

import ucar.nc2.ui.StopButton;
import ucar.util.prefs.ui.Debug;
import ucar.util.prefs.ui.ComboBox;
import ucar.util.prefs.PreferencesExt;
import ucar.util.prefs.XMLStore;

import org.apache.oro.io.GlobFilenameFilter;

import java.io.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.*;
import java.util.*;
import java.lang.reflect.Array;

import thredds.ui.*;

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
  private AccessLogPanel accessLogPanel;
  private ServletLogPanel servletLogPanel;
  private URLDumpPane urlDump;

  private JFrame parentFrame;
  private FileManager fileChooser;

  public TdsMonitor(ucar.util.prefs.PreferencesExt prefs, JFrame parentFrame) {
    this.mainPrefs = prefs;
    this.parentFrame = parentFrame;

    makeCache();

    fileChooser = new FileManager(parentFrame, null, null, (PreferencesExt) prefs.node("FileManager"));

    // the top UI
    tabbedPane = new JTabbedPane(JTabbedPane.TOP);
    accessLogPanel = new AccessLogPanel((PreferencesExt) mainPrefs.node("LogTable"));
    servletLogPanel = new ServletLogPanel((PreferencesExt) mainPrefs.node("ServletLogPanel"));
    urlDump = new URLDumpPane((PreferencesExt) mainPrefs.node("urlDump"));

    tabbedPane.addTab("AccessLogs", accessLogPanel);
    tabbedPane.addTab("ServletLogs", servletLogPanel);
    tabbedPane.addTab("UrlDump", urlDump);
    tabbedPane.setSelectedIndex(0);

    setLayout(new BorderLayout());
    add(tabbedPane, BorderLayout.CENTER);
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
    tabbedPane.setSelectedIndex(2);
  }


  ////////////////////////////////////////////////////////////////////////////////////

  static private TdsMonitor ui;
  static private boolean done = false;

  private static String ehLocation = "/data/thredds/ehcache/";
  private static String config =
          "<ehcache>\n" +
                  "    <diskStore path='" + ehLocation + "'/>\n" +
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

  ///////////////////////////

  private abstract class OpPanel extends JPanel {
    PreferencesExt prefs;
    TextHistoryPane ta;
    ComboBox serverCB, fileCB;
    JPanel buttPanel;
    AbstractButton coordButt = null;
    StopButton stopButton;

    boolean addCoords, defer, busy;
    long lastEvent = -1;
    boolean eventOK = true;

    IndependentWindow detailWindow;
    TextHistoryPane detailTA;

    OpPanel(PreferencesExt prefs) {
      this.prefs = prefs;
      ta = new TextHistoryPane(true);

      serverCB = new ComboBox((PreferencesExt)prefs.node("servers"));
      serverCB.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          if ((e.getWhen() != lastEvent) && eventOK) {// eliminate multiple events from same selection
            String server = (String) serverCB.getSelectedItem();
            serverCB.addItem(server);
            try {
              if (setLogFiles(server))
                serverCB.addItem(server);
            } catch (IOException e1) {
              e1.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
            lastEvent = e.getWhen();
          }
        }
      });

      fileCB = new ComboBox((PreferencesExt)prefs.node("files"));
      fileCB.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          if ((e.getWhen() != lastEvent) && eventOK) {// eliminate multiple events from same selection
            doit(fileCB.getSelectedItem());
            lastEvent = e.getWhen();
          }
        }
      });

      buttPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
      AbstractAction fileAction = new AbstractAction() {
        public void actionPerformed(ActionEvent e) {
          File[] files = fileChooser.chooseFiles();
          if ((files == null) || (files.length == 0)) return;
          if (files.length == 1)
            fileCB.setSelectedItem(files[0].getPath());
          else
            setLogFiles(files);
        }
      };
      BAMutil.setActionProperties(fileAction, "FileChooser", "open Local dataset...", false, 'L', -1);
      BAMutil.addActionToContainer(buttPanel, fileAction);

      JPanel flowPanel = new JPanel(new FlowLayout());
      flowPanel.add(new JLabel("server:"));
      flowPanel.add(serverCB);
      flowPanel.add(new JLabel("file:"));
      flowPanel.add(fileCB);

      JPanel topPanel = new JPanel(new BorderLayout());
      topPanel.add(flowPanel, BorderLayout.CENTER);
      topPanel.add(buttPanel, BorderLayout.EAST);

      setLayout(new BorderLayout());
      add(topPanel, BorderLayout.NORTH);
      add(ta, BorderLayout.CENTER);

      detailTA = new TextHistoryPane();
      detailTA.setFont(new Font("Monospaced", Font.PLAIN, 12));
      detailWindow = new IndependentWindow("Details", BAMutil.getImage("netcdfUI"), new JScrollPane(detailTA));
      Rectangle bounds = (Rectangle) prefs.getBean(FRAME_SIZE, new Rectangle(200, 50, 500, 700));
      detailWindow.setBounds(bounds);
    }

    void doit(Object command) {
      if (busy) return;
      if (command == null) return;
      if (command instanceof String)
        command = ((String) command).trim();

      busy = true;
      if (process(command)) {
        if (!defer) fileCB.addItem(command);
      }
      busy = false;
    }

    abstract boolean process(Object command);
    abstract boolean setLogFiles(String command)  throws IOException ;
    abstract void setLogFiles(File[] files) ;

    void save() {
      fileCB.save();
      serverCB.save();

      //if (v3Butt != null) prefs.putBoolean("nc3useRecords", v3Butt.getModel().isSelected());
      if (coordButt != null) prefs.putBoolean("coordState", coordButt.getModel().isSelected());
      if (detailWindow != null) prefs.putBeanObject(FRAME_SIZE, detailWindow.getBounds());
    }

    void setSelectedItem(Object item) {
      eventOK = false;
      fileCB.setSelectedItem(item);
      eventOK = true;
    }
  }

  /////////////////////////////////////////////////////////////////////
  private class AccessLogPanel extends OpPanel {
    AccessLogTable logTable;
    LogManager logManager = null;

    AccessLogPanel(PreferencesExt p) {
      super(p);
      logTable = new AccessLogTable((PreferencesExt) mainPrefs.node("LogTable"), dnsCache);
      logTable.addPropertyChangeListener(new java.beans.PropertyChangeListener() {
         public void propertyChange(java.beans.PropertyChangeEvent e) {
           if (e.getPropertyName().equals("UrlDump")) {
             String path = (String) e.getNewValue();
             if (logManager != null)
               path = logManager.makePath(path);
             gotoUrlDump(path);
           }
         }
       });

      add(logTable, BorderLayout.CENTER);
    }

    boolean setLogFiles(String server) throws IOException {
      logManager = new LogManager(server, true);
      logTable.setLogFiles(logManager.getLocalFiles());
      return true;
    }

    void setLogFiles(File[] files) {
      logTable.setLogFiles(Arrays.asList(files));
    }

    boolean process(Object o) {
      String command = (String) o;
      boolean err = false;

      try {
        logTable.setLogFiles(globFiles(command));

      } catch (FileNotFoundException ioe) {
        JOptionPane.showMessageDialog(null, "NetcdfDataset cant open " + command + "\n" + ioe.getMessage());
        ta.setText("Failed to open <" + command + ">\n" + ioe.getMessage());
        err = true;

      } catch (Exception e) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream(5000);
        e.printStackTrace();
        e.printStackTrace(new PrintStream(bos));
        ta.setText(bos.toString());
        err = true;
      }

      return !err;
    }

    void save() {
      logTable.exit();
      super.save();
    }
  }

  /////////////////////////////////////////////////////////////////////
  private class ServletLogPanel extends OpPanel {
    ServletLogTable logTable;
    LogManager logManager = null;

    ServletLogPanel(PreferencesExt p) {
      super(p);
      logTable = new ServletLogTable((PreferencesExt) mainPrefs.node("ServletLogTable"), buttPanel, dnsCache);
      add(logTable, BorderLayout.CENTER);
    }

    boolean setLogFiles(String server) throws IOException {
      logManager = new LogManager(server, false);
      logTable.setLogFiles(logManager.getLocalFiles());
      return true;
    }

    void setLogFiles(File[] files) {
      logTable.setLogFiles(Arrays.asList(files));
    }

    boolean process(Object o) {
      String command = (String) o;
      boolean err = false;

      try {
        logTable.setLogFiles(globFiles(command));

      } catch (FileNotFoundException ioe) {
        JOptionPane.showMessageDialog(null, "NetcdfDataset cant open " + command + "\n" + ioe.getMessage());
        ta.setText("Failed to open <" + command + ">\n" + ioe.getMessage());
        err = true;

      } catch (Exception e) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream(5000);
        e.printStackTrace();
        e.printStackTrace(new PrintStream(bos));
        ta.setText(bos.toString());
        err = true;
      }

      return !err;
    }

    void save() {
      logTable.exit();
      super.save();
    }
  }

    /**
     * Finds all files matching
     * a glob pattern.  This method recursively searches directories, allowing
     * for glob expressions like {@code "c:\\data\\200[6-7]\\*\\1*\\A*.nc"}.
     * @param globExpression The glob expression
     * @return List of File objects matching the glob pattern.  This will never
     * be null but might be empty
     * @throws Exception if the glob expression does not represent an absolute
     * path
     * @author Mike Grant, Plymouth Marine Labs; Jon Blower
     */
    public static java.util.List<File> globFiles(String globExpression) throws Exception {
        // Check that the glob expression is an absolute path.  Relative paths
        // would cause unpredictable and platform-dependent behaviour so
        // we disallow them.
        // If ds.getLocation() is a glob expression this test will still work
        // because we are not attempting to resolve the string to a real path.
        File globFile = new File(globExpression);
        if (!globFile.isAbsolute())
        {
            throw new Exception("Dataset location " + globExpression +
                " must be an absolute path");
        }

        // Break glob pattern into path components.  To do this in a reliable
        // and platform-independent way we use methods of the File class, rather
        // than String.split().
        java.util.List<String> pathComponents = new ArrayList<String>();
        while (globFile != null)
        {
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

        while(i < pathComponents.size())
        {
            FilenameFilter globFilter = new GlobFilenameFilter(pathComponents.get(i));
            java.util.List<File> newSearchPaths = new ArrayList<File>();
            // Look for matches in all the current search paths
            for (File dir : searchPaths)
            {
                if (dir.isDirectory())
                {
                    // Workaround for automounters that don't make filesystems
                    // appear unless they're poked
                    // do a listing on searchpath/pathcomponent whether or not
                    // it exists, then discard the results
                    new File(dir, pathComponents.get(i)).list();

                    for (File match : dir.listFiles(globFilter))
                    {
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
        for (File path : searchPaths)
        {
            if (path.isFile()) filesToReturn.add(path);
        }

        return filesToReturn;
    }

  //////////////////////////////////////////////

  public static void main(String args[]) {

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
