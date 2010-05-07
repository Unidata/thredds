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
package ucar.nc2.thredds.monitor;

import ucar.util.prefs.PreferencesExt;
import ucar.util.prefs.XMLStore;
import ucar.util.prefs.ui.ComboBox;
import ucar.util.prefs.ui.Debug;

import javax.swing.*;

import thredds.ui.*;

import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.*;


/**
 * Class Description
 *
 * @author caron
 * @since Jul 4, 2009
 */


public class CacheViewer extends JPanel {
  static private final String FRAME_SIZE = "FrameSize";

  private static JFrame frame;
  private static PreferencesExt prefs;
  private static XMLStore store;

  private ucar.util.prefs.PreferencesExt mainPrefs;
  private JTabbedPane tabbedPane;
  private CachePanel cachePanel;

  private JFrame parentFrame;
  private FileManager fileChooser;

  public CacheViewer(ucar.util.prefs.PreferencesExt prefs, JFrame parentFrame) {
    this.mainPrefs = prefs;
    this.parentFrame = parentFrame;

    fileChooser = new FileManager(parentFrame, null, null, (PreferencesExt) prefs.node("FileManager"));
    fileChooser.getFileChooser().setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
    fileChooser.getFileChooser().setDialogTitle("Ehcache Directory");

    // the top UI
    tabbedPane = new JTabbedPane(JTabbedPane.TOP);
    cachePanel = new CachePanel((PreferencesExt) mainPrefs.node("LogTable"));

    tabbedPane.addTab("ehcache", cachePanel);
    tabbedPane.setSelectedIndex(0);

    setLayout(new BorderLayout());
    add(tabbedPane, BorderLayout.CENTER);

  }

  public void exit() {
    thredds.filesystem.CacheManager.shutdown();
    fileChooser.save();
    cachePanel.save();

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

  ////////////////////////////////////////////////////////////////////////////////////

  static private CacheViewer ui;
  static private boolean done = false;

  ///////////////////////////

  private abstract class OpPanel extends JPanel {
    PreferencesExt prefs;
    TextHistoryPane ta;
    ComboBox fileCB;
    JPanel buttPanel;

    boolean addCoords, defer, busy;
    long lastEvent = -1;
    boolean eventOK = true;

    IndependentWindow detailWindow;
    TextHistoryPane detailTA;

    OpPanel(PreferencesExt prefs) {
      this.prefs = prefs;
      ta = new TextHistoryPane(true);

      fileCB = new ComboBox((PreferencesExt) prefs.node("files"));
      fileCB.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          if ((e.getWhen() != lastEvent) && eventOK) { // eliminate multiple events from same selection
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
          //else
          //  setLogFiles(files);
        }
      };
      BAMutil.setActionProperties(fileAction, "FileChooser", "open Local dataset...", false, 'L', -1);
      BAMutil.addActionToContainer(buttPanel, fileAction);

      JPanel flowPanel = new JPanel(new FlowLayout());
      flowPanel.add(new JLabel("server:"));
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

    void save() {
      fileCB.save();

      //if (v3Butt != null) prefs.putBoolean("nc3useRecords", v3Butt.getModel().isSelected());
      if (detailWindow != null) prefs.putBeanObject(FRAME_SIZE, detailWindow.getBounds());
    }

    void setSelectedItem(Object item) {
      eventOK = false;
      fileCB.setSelectedItem(item);
      eventOK = true;
    }
  }

  /////////////////////////////////////////////////////////////////////
  private class CachePanel extends OpPanel {
    CacheTable cacheTable;
    LogDownloader logManager = null;

    CachePanel(PreferencesExt p) {
      super(p);
      cacheTable = new CacheTable((PreferencesExt) mainPrefs.node("CacheTable"), buttPanel);
      cacheTable.addPropertyChangeListener(new java.beans.PropertyChangeListener() {
        public void propertyChange(java.beans.PropertyChangeEvent e) {
          if (e.getPropertyName().equals("UrlDump")) {
            String path = (String) e.getNewValue();
            //if (logManager != null)
            //logManager.makePath(path);
            //gotoUrlDump(path);
          }
        }
      });

      add(cacheTable, BorderLayout.CENTER);
    }

    boolean process(Object o) {
      String command = (String) o;
      boolean err = false;

      try {
        thredds.filesystem.CacheManager.makeReadOnlyCacheManager(command, "directories");
        cacheTable.setCache(thredds.filesystem.CacheManager.getEhcache());

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
      cacheTable.exit();
      super.save();
    }
  }

  //////////////////////////////////////////////

  public static void main(String args[]) {

    // prefs storage
    try {
      String prefStore = ucar.util.prefs.XMLStore.makeStandardFilename(".unidata", "CacheViewer.xml");
      store = ucar.util.prefs.XMLStore.createFromFile(prefStore, null);
      prefs = store.getPreferences();
      Debug.setStore(prefs.node("Debug"));
    } catch (IOException e) {
      System.out.println("XMLStore Creation failed " + e);
    }

    // initializations
    BAMutil.setResourcePath("/resources/nj22/ui/icons/");

    // put UI in a JFrame
    frame = new JFrame("Cache Viewer");
    ui = new CacheViewer(prefs, frame);

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