// $Id: ToolsUI.java 50 2006-07-12 16:30:06Z caron $
/*
 * Copyright 1997-2006 Unidata Program Center/University Corporation for
 * Atmospheric Research, P.O. Box 3000, Boulder, CO 80307,
 * support@unidata.ucar.edu.
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or (at
 * your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser
 * General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */

package ucar.nc2.ui;

import ucar.nc2.*;
import ucar.nc2.thredds.ThreddsDataFactory;
import ucar.nc2.ncml.NcMLReader;
import ucar.nc2.ncml.Aggregation;
import ucar.nc2.dt.*;
import ucar.nc2.dt.fmrc.FmrcDefinition;
import ucar.nc2.dt.fmrc.ForecastModelRunInventory;
import ucar.nc2.dt.fmrc.FmrcInventory;
import ucar.nc2.dt.point.PointObsDatasetFactory;
import ucar.nc2.dt.trajectory.TrajectoryObsDatasetFactory;
import ucar.nc2.dataset.*;

import ucar.nc2.geotiff.GeoTiff;
import ucar.nc2.util.*;
import ucar.nc2.units.*;

import ucar.util.prefs.*;
import ucar.util.prefs.ui.*;

import thredds.ui.*;

import ucar.nc2.ui.grid.GridUI;
import ucar.nc2.ui.image.ImageViewPanel;
import thredds.util.URLStreamHandlerFactory;
import thredds.util.SocketMessage;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;

import javax.swing.*;
import javax.swing.event.*;

/**
 * Netcdf Tools user interface.
 *
 * @author caron
 * @version $Revision: 50 $ $Date: 2006-07-12 16:30:06Z $
 */

public class ToolsUI extends JPanel {
  static private final String WorldDetailMap = "/optional/nj22/maps/Countries.zip";
  static private final String USMap = "/optional/nj22/maps/US.zip";

  static private final String FRAME_SIZE = "FrameSize";
  static private final String DEBUG_FRAME_SIZE = "DebugWindowSize";
  static private final String GRIDVIEW_FRAME_SIZE = "GridUIWindowSize";
  static private final String GRIDIMAGE_FRAME_SIZE = "GridImageWindowSize";
  static private boolean debugListen = false;

  private ucar.util.prefs.PreferencesExt mainPrefs;

  // UI
  private FmrcPanel fmrcPanel;
  private GeoGridPanel gridPanel;
  private ImagePanel imagePanel;
  private NCdumpPanel ncdumpPanel;
  private OpPanel coordSysPanel, ncmlPanel, geotiffPanel;
  private PointObsPanel pointObsPanel;
  private RadialPanel radialPanel;
  private ThreddsUI threddsUI;
  private TrajectoryTablePanel trajTablePanel;
  private UnitsPanel unitsPanel;
  private URLDumpPane urlPanel;
  private ViewerPanel viewerPanel;

  private JTabbedPane tabbedPane;
  private JFrame parentFrame;
  private FileManager fileChooser;
  private AboutWindow aboutWindow = null;

  // data
  private ucar.nc2.thredds.ThreddsDataFactory threddsDataFactory = new ucar.nc2.thredds.ThreddsDataFactory();
  private DateFormatter formatter = new DateFormatter();

  private boolean setUseRecordStructure = false;

  // debugging
  private JMenu debugFlagMenu;
  private DebugFlags debugFlags;
  private AbstractAction useDebugWindowAction;
  private IndependentWindow debugWindow;
  private TextOutputStreamPane debugPane;
  private PrintStream debugOS;
  private boolean debug = false, debugTab = false, debugNcmlWrite = false, debugCB = false;


  public ToolsUI(ucar.util.prefs.PreferencesExt prefs, JFrame parentFrame) {
    this.mainPrefs = prefs;
    this.parentFrame = parentFrame;

    // FileChooser is shared
    javax.swing.filechooser.FileFilter[] filters = new javax.swing.filechooser.FileFilter[2];
    filters[0] = new FileManager.HDF5ExtFilter();
    filters[1] = new FileManager.NetcdfExtFilter();
    fileChooser = new FileManager(parentFrame, null, filters, (PreferencesExt) prefs.node("FileManager"));

    viewerPanel = new ViewerPanel((PreferencesExt) mainPrefs.node("varTable"));

    // the overall UI
    tabbedPane = new JTabbedPane(JTabbedPane.TOP);
    tabbedPane.addTab("Viewer", viewerPanel);

    // all the other component are defferred for fast startup
    tabbedPane.addTab("NCDump", new JLabel("NCDump"));
    tabbedPane.addTab("CoordSys", new JLabel("CoordSys"));
    tabbedPane.addTab("Grids", new JLabel("Grids"));
    tabbedPane.addTab("Fmrc", new JLabel("Fmrc"));
    tabbedPane.addTab("Radial", new JLabel("Radial"));
    tabbedPane.addTab("PointObs", new JLabel("PointObs"));
    tabbedPane.addTab("Trajectory", new JLabel("Trajectory"));
    tabbedPane.addTab("Images", new JLabel("Images"));
    tabbedPane.addTab("THREDDS", new JLabel("THREDDS"));
    tabbedPane.addTab("GeoTiff", new JLabel("GeoTiff"));
    tabbedPane.addTab("Units", new JLabel("Units"));
    tabbedPane.addTab("NcML", new JLabel("NcML"));
    tabbedPane.addTab("URLdump", new JLabel("URLdump"));
    tabbedPane.setSelectedIndex(0);
    tabbedPane.addChangeListener(new ChangeListener() {
      public void stateChanged(ChangeEvent e) {
        Component c = tabbedPane.getSelectedComponent();
        if (c instanceof JLabel) {
          int idx = tabbedPane.getSelectedIndex();
          String title = tabbedPane.getTitleAt(idx);
          makeComponent(title);
        }
      }
    });

    setLayout(new BorderLayout());
    add(tabbedPane, BorderLayout.CENTER);

    // dynamic proxy for DebugFlags
    debugFlags = (DebugFlags) java.lang.reflect.Proxy.newProxyInstance(DebugFlags.class.getClassLoader(), new Class[]{DebugFlags.class}, new DebugProxyHandler());

    // the debug message window
    debugPane = new TextOutputStreamPane();
    debugWindow = debugPane.makeIndependentWindow("Debug Messages");
    Rectangle bounds = (Rectangle) mainPrefs.getBean(DEBUG_FRAME_SIZE, new Rectangle(100, 50, 500, 700));
    debugWindow.setBounds(bounds);
    debugWindow.setIconImage(BAMutil.getImage("netcdfUI"));

    makeMenuBar();
    setDebugFlags();
  }

  // deffered creation of components to minimize startup
  private void makeComponent(String title) {
    // find the correct index
    int n = tabbedPane.getTabCount();
    int idx;
    for (idx = 0; idx < n; idx++) {
      String cTitle = tabbedPane.getTitleAt(idx);
      if (cTitle.equals(title)) break;
    }
    if (idx >= n) {
      if (debugTab) System.out.println("tabbedPane cant find " + title);
      return;
    }

    Component c;
    if (title.equals("NCDump")) {
      ncdumpPanel = new NCdumpPanel((PreferencesExt) mainPrefs.node("NCDump"));
      c = ncdumpPanel;

    } else if (title.equals("NcML")) {
      ncmlPanel = new NcmlPanel((PreferencesExt) mainPrefs.node("NcML"));
      c = ncmlPanel;

    } else if (title.equals("CoordSys")) {
      coordSysPanel = new CoordSysPanel((PreferencesExt) mainPrefs.node("CoordSys"));
      c = coordSysPanel;

    } else if (title.equals("Grids")) {
      gridPanel = new GeoGridPanel((PreferencesExt) mainPrefs.node("grid"));
      c = gridPanel;

    } else if (title.equals("Fmrc")) {
      fmrcPanel = new FmrcPanel((PreferencesExt) mainPrefs.node("fmrc"));
      c = fmrcPanel;

    } else if (title.equals("Radial")) {
      radialPanel = new RadialPanel((PreferencesExt) mainPrefs.node("radial"));
      c = radialPanel;

    } else if (title.equals("PointObs")) {
      pointObsPanel = new PointObsPanel((PreferencesExt) mainPrefs.node("stations"));
      c = pointObsPanel;

    } else if (title.equals("Trajectory")) {
      trajTablePanel = new TrajectoryTablePanel((PreferencesExt) mainPrefs.node("trajectory"));
      c = trajTablePanel;

    } else if (title.equals("Images")) {
      imagePanel = new ImagePanel((PreferencesExt) mainPrefs.node("images"));
      c = imagePanel;

    } else if (title.equals("THREDDS")) {
      threddsUI = new ThreddsUI(ToolsUI.this.parentFrame, (PreferencesExt) mainPrefs.node("thredds"));
      threddsUI.addPropertyChangeListener(new java.beans.PropertyChangeListener() {
        public void propertyChange(java.beans.PropertyChangeEvent e) {
          if (e.getPropertyName().equals("InvAccess")) {
            thredds.catalog.InvAccess access = (thredds.catalog.InvAccess) e.getNewValue();
            setThreddsDatatype(access);
          }

          if (e.getPropertyName().equals("Dataset") || e.getPropertyName().equals("File")) {
            thredds.catalog.InvDataset ds = (thredds.catalog.InvDataset) e.getNewValue();
            setThreddsDatatype(ds, e.getPropertyName().equals("File"));
          }
        }
      });

      c = threddsUI;

    } else if (title.equals("GeoTiff")) {
      geotiffPanel = new GeotiffPanel((PreferencesExt) mainPrefs.node("WCS"));
      c = geotiffPanel;

    } else if (title.equals("Units")) {
      unitsPanel = new UnitsPanel((PreferencesExt) mainPrefs.node("units"));
      c = unitsPanel;

    } else if (title.equals("URLdump")) {
      urlPanel = new URLDumpPane((PreferencesExt) mainPrefs.node("urlDump"));
      c = urlPanel;

    } else if (title.equals("Viewer")) {
      c = viewerPanel;

    } else {
      System.out.println("tabbedPane unknown component " + title);
      return;
    }

    tabbedPane.setComponentAt(idx, c);
    if (debugTab) System.out.println("tabbedPane changed " + title + " added ");
  }

  private void makeMenuBar() {
    JMenuBar mb = new JMenuBar();
    JRootPane rootPane = parentFrame.getRootPane();
    rootPane.setJMenuBar(mb);

    /// System menu
    JMenu sysMenu = new JMenu("System");
    sysMenu.setMnemonic('S');
    mb.add(sysMenu);
    //BAMutil.addActionToMenu( sysMenu, printAction);

    AbstractAction showCacheAction = new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        viewerPanel.detailTA.setText("NetcdfFileCache contents\n");
        java.util.List cacheList = NetcdfFileCache.getCache();
        for (int i = 0; i < cacheList.size(); i++) {
          Object o = cacheList.get(i);
          viewerPanel.detailTA.appendLine(" " + o);
        }
        viewerPanel.detailTA.appendLine("\nNetcdfDatasetCache contents");
        cacheList = NetcdfDatasetCache.getCache();
        for (int i = 0; i < cacheList.size(); i++) {
          Object o = cacheList.get(i);
          viewerPanel.detailTA.appendLine(" " + o);
        }
        viewerPanel.detailWindow.show();
        viewerPanel.detailTA.appendLine("\nRAF Cache contents");
        cacheList = ucar.unidata.io.FileCache.getCache();
        for (int i = 0; i < cacheList.size(); i++) {
          Object o = cacheList.get(i);
          viewerPanel.detailTA.appendLine(" " + o);
        }
        viewerPanel.detailWindow.show();
      }
    };
    BAMutil.setActionProperties(showCacheAction, null, "Show Caches", false, 'S', -1);
    BAMutil.addActionToMenu(sysMenu, showCacheAction);

    AbstractAction clearCacheAction = new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        NetcdfFileCache.clearCache(true);
      }
    };
    BAMutil.setActionProperties(clearCacheAction, null, "Clear NetcdfFileCache", false, 'C', -1);
    BAMutil.addActionToMenu(sysMenu, clearCacheAction);

    AbstractAction clearCacheDSAction = new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        NetcdfDatasetCache.clearCache(true);
      }
    };
    BAMutil.setActionProperties(clearCacheDSAction, null, "Clear NetcdfDatasetCache", false, 'D', -1);
    BAMutil.addActionToMenu(sysMenu, clearCacheDSAction);

    AbstractAction enableCache = new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        Boolean state = (Boolean) getValue(BAMutil.STATE);
        boolean stateB = state.booleanValue();
        if (stateB == isCacheInit) return;
        isCacheInit = stateB;
        if (isCacheInit) {
          initCaches();
        } else {
          NetcdfFileCache.disable();
          NetcdfDatasetCache.disable();
        }
      }
    };
    BAMutil.setActionPropertiesToggle(enableCache, null, "enable Caches", isCacheInit, 'N', -1);
    BAMutil.addActionToMenu(sysMenu, enableCache);

    AbstractAction showPropertiesAction = new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        viewerPanel.detailTA.setText("System Properties\n");
        Properties sysp = System.getProperties();
        Enumeration eprops = sysp.propertyNames();
        ArrayList list = Collections.list(eprops);
        Collections.sort(list);

        for (int i = 0; i < list.size(); i++) {
          String name = (String) list.get(i);
          String value = System.getProperty(name);
          viewerPanel.detailTA.appendLine("  " + name + " = " + value);
        }
        viewerPanel.detailWindow.show();
      }
    };
    BAMutil.setActionProperties(showPropertiesAction, null, "System Properties", false, 'P', -1);
    BAMutil.addActionToMenu(sysMenu, showPropertiesAction);

    /* AbstractAction showLoggingAction = new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        viewerPanel.detailTA.setText("Logging Information\n");
        static private org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(Level2VolumeScan.class);
        org.apache.commons.logging.LogFactory logf = org.apache.commons.logging.LogFactory.getFactory();
        org.apache.commons.logging.Log log = logf.getInstance(this.getClass());
        viewerPanel.detailTA.appendLine(" Log implementation class= " + log.getClass().getName());
        viewerPanel.detailTA.appendLine(" Log Attributes= ");
        String[] atts = logf.getAttributeNames();
        for (int i = 0; i < atts.length; i++) {
          viewerPanel.detailTA.appendLine("  " + atts[i]);
        }
        viewerPanel.detailWindow.show();
      }
    };
    BAMutil.setActionProperties(showLoggingAction, null, "Logging Information", false, 'L', -1);
    BAMutil.addActionToMenu(sysMenu, showLoggingAction);  */

    JMenu plafMenu = new JMenu("Look and Feel");
    plafMenu.setMnemonic('L');
    sysMenu.add(plafMenu);
    PLAF plaf = new PLAF(rootPane);
    plaf.addToMenu(plafMenu);

    AbstractAction exitAction = new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        exit();
      }
    };
    BAMutil.setActionProperties(exitAction, "Exit", "Exit", false, 'X', -1);
    BAMutil.addActionToMenu(sysMenu, exitAction);

    // Modes Menu
    JMenu modeMenu = new JMenu("Modes");
    modeMenu.setMnemonic('M');
    mb.add(modeMenu);
    makeModesMenu(modeMenu);

    // Debug Menu
    JMenu debugMenu = new JMenu("Debug");
    debugMenu.setMnemonic('D');
    mb.add(debugMenu);

    // the list of debug flags are in a pull-aside menu
    // they are dynamically discovered, and persisted
    debugFlagMenu = (JMenu) debugMenu.add(new JMenu("Debug Flags"));
    debugFlagMenu.addMenuListener(new MenuListener() {
      public void menuSelected(MenuEvent e) {
        setDebugFlags(); // let Debug know about the flag names
        ucar.util.prefs.ui.Debug.constructMenu(debugFlagMenu); // now construct the menu
      }

      public void menuDeselected(MenuEvent e) {
        setDebugFlags(); // transfer menu values
      }

      public void menuCanceled(MenuEvent e) {
      }
    });

    // this deletes all the flags, then they start accululating again
    AbstractAction clearDebugFlagsAction = new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        ucar.util.prefs.ui.Debug.removeAll();
      }
    };
    BAMutil.setActionProperties(clearDebugFlagsAction, null, "Delete All Debug Flags", false, 'C', -1);
    BAMutil.addActionToMenu(debugMenu, clearDebugFlagsAction);

    // send output to the debug message window
    useDebugWindowAction = new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        Boolean state = (Boolean) getValue(BAMutil.STATE);
        setDebugOutputStream(state.booleanValue());
      }
    };
    BAMutil.setActionProperties(useDebugWindowAction, null, "Use Debug Window", true, 'C', -1);
    BAMutil.addActionToMenu(debugMenu, useDebugWindowAction);
    if (mainPrefs.getBoolean("useDebugWindow", false)) {
      useDebugWindowAction.putValue(BAMutil.STATE, Boolean.TRUE);
      setDebugFlags();
      setDebugOutputStream(true);
    }

    // show the debug window
    AbstractAction showDebugAction = new AbstractAction() {
      public void actionPerformed(ActionEvent evt) {
        // System.out.println("debugWindow.show() "+debugWindow.getBounds());
        debugWindow.show();
      }
    };
    BAMutil.setActionProperties(showDebugAction, null, "Show Debug Window", false, 'D', 0);
    BAMutil.addActionToMenu(debugMenu, showDebugAction);

    JMenu helpMenu = new JMenu("Help");
    helpMenu.setMnemonic('H');
    mb.add(helpMenu);

    // "about" this application
    AbstractAction aboutAction = new AbstractAction() {
      public void actionPerformed(ActionEvent evt) {
        if (aboutWindow == null)
          aboutWindow = new AboutWindow();
        aboutWindow.setVisible( true);
      }
    };
    BAMutil.setActionProperties(aboutAction, null, "About", false, 'A', 0);
    BAMutil.addActionToMenu(helpMenu, aboutAction);

    AbstractAction logoAction = new AbstractAction() {
      public void actionPerformed(ActionEvent evt) {
        new SplashScreen();
      }
    };
    BAMutil.setActionProperties(logoAction, null, "Logo", false, 'L', 0);
    BAMutil.addActionToMenu(helpMenu, logoAction);
  }

  public void setDebugFlags() {
    if (debug) System.out.println("checkDebugFlags ");
    NetcdfFile.setDebugFlags(debugFlags);
    ucar.nc2.ncml.NcMLReader.setDebugFlags(debugFlags);
    ucar.nc2.dods.DODSNetcdfFile.setDebugFlags(debugFlags);
    ucar.nc2.iosp.grib.GribServiceProvider.setDebugFlags(debugFlags);
    ucar.nc2.thredds.ThreddsDataFactory.setDebugFlags(debugFlags);

    ucar.nc2.FileWriter.setDebugFlags(debugFlags);
  }

  public void setDebugOutputStream(boolean b) {
    // System.out.println("setDebugOutputStream "+b);
    if (b) {
      if (debugOS == null) debugOS = new PrintStream(debugPane.getOutputStream());
      NetcdfFile.setDebugOutputStream(debugOS);
    } else {
      NetcdfFile.setDebugOutputStream(System.out);
    }
  }

  private void makeModesMenu(JMenu modeMenu) {
    AbstractAction a;

    JMenu ncMenu = new JMenu("NetcdfFile");
    modeMenu.add(ncMenu);

    a = new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        Boolean state = (Boolean) getValue(BAMutil.STATE);
        setUseRecordStructure = state.booleanValue();
      }
    };
    BAMutil.setActionPropertiesToggle(a, null, "nc3UseRecords", setUseRecordStructure, 'V', -1);
    BAMutil.addActionToMenu(ncMenu, a);

    JMenu dsMenu = new JMenu("NetcdfDataset");
    modeMenu.add(dsMenu);

    a = new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        Boolean state = (Boolean) getValue(BAMutil.STATE);
        CoordSysBuilder.setUseMaximalCoordSys(state.booleanValue());
      }
    };
    BAMutil.setActionPropertiesToggle(a, null, "set Use Maximal CoordSystem", CoordSysBuilder.getUseMaximalCoordSys(), 'N', -1);
    BAMutil.addActionToMenu(dsMenu, a);

    a = new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        Boolean state = (Boolean) getValue(BAMutil.STATE);
        NetcdfDataset.setUseNaNs(state.booleanValue());
      }
    };
    BAMutil.setActionPropertiesToggle(a, null, "set NaNs for missing values", NetcdfDataset.getUseNaNs(), 'N', -1);
    BAMutil.addActionToMenu(dsMenu, a);

    a = new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        Boolean state = (Boolean) getValue(BAMutil.STATE);
        NetcdfDataset.setFillValueIsMissing(state.booleanValue());
      }
    };
    BAMutil.setActionPropertiesToggle(a, null, "use _FillValue attribute for missing values",
            NetcdfDataset.getFillValueIsMissing(), 'F', -1);
    BAMutil.addActionToMenu(dsMenu, a);

    a = new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        Boolean state = (Boolean) getValue(BAMutil.STATE);
        NetcdfDataset.setInvalidDataIsMissing(state.booleanValue());
      }
    };
    BAMutil.setActionPropertiesToggle(a, null, "use valid_range attribute for missing values",
            NetcdfDataset.getInvalidDataIsMissing(), 'V', -1);
    BAMutil.addActionToMenu(dsMenu, a);

    a = new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        Boolean state = (Boolean) getValue(BAMutil.STATE);
        NetcdfDataset.setMissingDataIsMissing(state.booleanValue());
      }
    };
    BAMutil.setActionPropertiesToggle(a, null, "use mssing_value attribute for missing values",
            NetcdfDataset.getMissingDataIsMissing(), 'M', -1);
    BAMutil.addActionToMenu(dsMenu, a);
  }

  public void save() {
    fileChooser.save();
    if (debugWindow != null) {
      mainPrefs.putBeanObject(DEBUG_FRAME_SIZE, debugWindow.getBounds());
    }
    Boolean useDebugWindow = (Boolean) useDebugWindowAction.getValue(BAMutil.STATE);
    if (useDebugWindow.booleanValue())
      mainPrefs.putBoolean("useDebugWindow", true);

    if (viewerPanel != null) viewerPanel.save();
    if (coordSysPanel != null) coordSysPanel.save();
    if (ncdumpPanel != null) ncdumpPanel.save();
    if (ncmlPanel != null) ncmlPanel.save();
    if (imagePanel != null) imagePanel.save();
    if (gridPanel != null) gridPanel.save();
    if (fmrcPanel != null) fmrcPanel.save();
    if (radialPanel != null) radialPanel.save();
    if (pointObsPanel != null) pointObsPanel.save();
    if (trajTablePanel != null) trajTablePanel.save();
    if (threddsUI != null) threddsUI.storePersistentData();
    if (unitsPanel != null) unitsPanel.save();
    if (urlPanel != null) urlPanel.save();
    if (geotiffPanel != null) geotiffPanel.save();
  }

  //////////////////////////////////////////////////////////////////////////////////

  // jump to the appropriate tab based on datatype of InvDataset
  private void setThreddsDatatype(thredds.catalog.InvDataset invDataset, boolean wantsViewer) {
    if (invDataset == null) return;

    try {
      // just open as a NetcdfDataset
      if (wantsViewer) {
        showInViewer(threddsDataFactory.openDataset(invDataset, true, null, null));
        return;
      }

      // otherwise do the datatype thing
      ThreddsDataFactory.Result threddsData = threddsDataFactory.openDatatype(invDataset, null);
      if (threddsData == null) {
        JOptionPane.showMessageDialog(null, "Unknown datatype");
        return;
      }
      setThreddsDatatype(threddsData);

    } catch (IOException ioe) {
      JOptionPane.showMessageDialog(null, "Error on setThreddsDataset = " + ioe.getMessage());
    }

  }

  // jump to the appropriate tab based on datatype of InvDataset
  private void setThreddsDatatype(thredds.catalog.InvAccess invAccess) {
    if (invAccess == null) return;

    thredds.catalog.InvService s = invAccess.getService();
    if (s.getServiceType() == thredds.catalog.ServiceType.HTTPServer) {
      downloadFile(invAccess.getStandardUrlName());
      return;
    }

    try {
      ThreddsDataFactory.Result threddsData = threddsDataFactory.openDatatype(invAccess, null);
      setThreddsDatatype(threddsData);

    } catch (IOException ioe) {
      JOptionPane.showMessageDialog(null, "Error on setThreddsDataset = " + ioe.getMessage());
    }

  }

  // jump to the appropriate tab based on datatype of InvDataset
  private void setThreddsDatatype(String dataset) {

    try {
      ThreddsDataFactory.Result threddsData = threddsDataFactory.openDatatype(dataset, null);
      setThreddsDatatype(threddsData);

    } catch (IOException ioe) {
      JOptionPane.showMessageDialog(null, "Error on setThreddsDataset = " + ioe.getMessage());
    }

  }

  // jump to the appropriate tab based on datatype of threddsData
  private void setThreddsDatatype(ThreddsDataFactory.Result threddsData) {

    if (threddsData.fatalError) {
      JOptionPane.showMessageDialog(this, "Cant open dataset=" + threddsData.errLog);
      return;
    }

    if (threddsData.dtype == thredds.catalog.DataType.GRID) {
      makeComponent("Grids");
      gridPanel.setDataset( (NetcdfDataset) threddsData.gridDataset.getNetcdfFile());
      tabbedPane.setSelectedComponent(gridPanel);

    } else if (threddsData.dtype == thredds.catalog.DataType.IMAGE) {
      makeComponent("Images");
      imagePanel.setImageLocation(threddsData.imageURL);
      tabbedPane.setSelectedComponent(imagePanel);

    } else if (threddsData.dtype == thredds.catalog.DataType.RADIAL) {
      makeComponent("Radial");
      radialPanel.setDataset(threddsData.radialDataset);
      tabbedPane.setSelectedComponent(radialPanel);

    } else if ((threddsData.dtype == thredds.catalog.DataType.POINT) || (threddsData.dtype == thredds.catalog.DataType.STATION)) {
      makeComponent("PointObs");
      pointObsPanel.setPointObsDataset(threddsData.pobsDataset);
      tabbedPane.setSelectedComponent(pointObsPanel);

    }
  }

  /* jump to the appropriate tab based on datatype of NetcdfDataset
  private void setDataset(thredds.catalog.DataType dtype, NetcdfDataset ds) {

    if (dtype == thredds.catalog.DataType.GRID) {
      makeComponent("Grids");
      gridPanel.setDataset(ds);
      tabbedPane.setSelectedComponent(gridPanel);
      return;
    }

    /* else if (dtype == thredds.catalog.DataType.STATION) {
      makeComponent("StationDataset");
      stnTablePanel.setStationObsDataset( ds);
      tabbedPane.setSelectedComponent( stnTablePanel);
      return;
    } *

    else {

      makeComponent("Viewer");
      viewerPanel.setDataset(ds);
      tabbedPane.setSelectedComponent(viewerPanel);
      return;
    }
  } */

  private void showInViewer(NetcdfDataset ds) {
    makeComponent("Viewer");
    viewerPanel.setDataset(ds);
    tabbedPane.setSelectedComponent(viewerPanel);
  }

  // LOOK put in background task ??
  private NetcdfDataset openDataset(String location, boolean addCoords, CancelTask task) {
    try {
      NetcdfDataset ncd = NetcdfDataset.openDataset( location, addCoords, task);

      /* if (addCoords)
        ncd = NetcdfDatasetCache.acquire(location, task);
      else {
         NetcdfFile ncfile = NetcdfFileCache.acquire(location, task);
         if (ncfile != null) ncd = new NetcdfDataset( ncfile, false);
      } */

      if (setUseRecordStructure)
        ncd.addRecordStructure();

      return ncd;

    } catch (IOException ioe) {
      JOptionPane.showMessageDialog(null, "NetcdfDataset.open cant open " + ioe.getMessage());
      return null;
    }

  }

  private NetcdfFile openFile(String location, boolean addCoords, CancelTask task) {
    NetcdfFile ncfile = null;
    try {
      if (addCoords)
        ncfile = NetcdfDatasetCache.acquire(location, task);
      else
        ncfile = NetcdfDataset.acquireFile(location, task);

      if (ncfile == null)
        JOptionPane.showMessageDialog(null, "NetcdfDataset.open cant open " + location);
      else if (setUseRecordStructure)
        ncfile.addRecordStructure();

    } catch (IOException ioe) {
      String message = ioe.getMessage();
      if ((null == message) && (ioe instanceof EOFException))
        message = "Premature End of File";
      JOptionPane.showMessageDialog(null, "NetcdfDataset.open cant open " + location + "\n" + message);

      try {
        if (ncfile != null) ncfile.close();
      } catch (IOException e) {
      }

      ncfile = null;
    }

    return ncfile;
  }

  private String downloadStatus = null;

  private void downloadFile(String urlString) {
    int pos = urlString.lastIndexOf('/');
    String defFilename = (pos >= 0) ? urlString.substring(pos) : urlString;
    String fileOutName = fileChooser.chooseFilename(defFilename);
    if (fileOutName == null) return;
    String[] values = new String[2];
    values[0] = fileOutName;
    values[1] = urlString;

    // put in background thread with a ProgressMonitor window
    GetDataRunnable runner = new GetDataRunnable() {
      public void run(Object o) {
        String[] values = (String[]) o;
        BufferedOutputStream out = null;
        try {
          FileOutputStream fos = new FileOutputStream(values[0]);
          out = new BufferedOutputStream(fos, 60000);
        } catch (IOException ioe) {
          downloadStatus = "Error opening" + values[0] + "\n" + ioe.getMessage();
          return;
        }

        try {
          thredds.util.IO.copyUrlB(values[1], out, 60000);
          downloadStatus = values[1] + " written to " + values[0];
        } catch (IOException ioe) {
          downloadStatus = "Error reading " + values[1] + "\n" + ioe.getMessage();
        } finally {
          try {
            out.close();
          }
          catch (IOException e) {
          }
        }
      }
    };

    GetDataTask task = new GetDataTask(runner, urlString, values);
    thredds.ui.ProgressMonitor pm = new thredds.ui.ProgressMonitor(task);
    pm.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        JOptionPane.showMessageDialog(null, e.getActionCommand() + "\n" + downloadStatus);
        downloadStatus = null;
      }
    });
    pm.start(this, "Download", 30);
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////
  // the panel contents

  // abstract superclass
  // subclasses must implement process()
  private abstract class OpPanel extends JPanel {
    PreferencesExt prefs;
    TextHistoryPane ta;
    ComboBox cb;
    JPanel buttPanel;
    AbstractButton coordButt = null;
    StopButton stopButton;

    boolean addCoords, defer, busy;
    long lastEvent = -1;
    boolean eventOK = true;

    IndependentWindow detailWindow;
    TextHistoryPane detailTA;

    OpPanel(PreferencesExt prefs, String command) {
      this(prefs, command, true, true);
    }

    OpPanel(PreferencesExt prefs, String command, boolean addFileButton, boolean addCoordButton) {
      this.prefs = prefs;
      ta = new TextHistoryPane(true);

      cb = new ComboBox(prefs);
      cb.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          if ((e.getWhen() != lastEvent) && eventOK) {// eliminate multiple events from same selection
            if (debugCB) System.out.println(" doit " + cb.getSelectedItem() + " cmd=" + e.getActionCommand() + " whne=" + e.getWhen() + " class=" + OpPanel.this.getClass().getName());
            doit(cb.getSelectedItem());
            lastEvent = e.getWhen();
          }
        }
      });

      buttPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));

      /* button and comboBox dont work!
      AbstractAction getAction =  new AbstractAction() {
        public void actionPerformed(ActionEvent e) {
          doit( cb.getSelectedItem());
        }
      };
      BAMutil.setActionProperties( getAction, "Doit", "open selected", false, 'O', -1);  */

      if (addFileButton) {
        AbstractAction fileAction = new AbstractAction() {
          public void actionPerformed(ActionEvent e) {
            String filename = fileChooser.chooseFilename();
            if (filename == null) return;
            cb.setSelectedItem(filename);
          }
        };
        BAMutil.setActionProperties(fileAction, "FileChooser", "open Local dataset...", false, 'L', -1);
        BAMutil.addActionToContainer(buttPanel, fileAction);
      }

      /* AbstractAction v3Action =  new AbstractAction() {
        public void actionPerformed(ActionEvent e) {
          Boolean state = (Boolean) getValue( BAMutil.STATE);
          nc3useRecords = state.booleanValue();
          String tooltip = nc3useRecords ? "nc3 use Records" : "nc3 dont use Records";
          v3Butt.setToolTipText(tooltip);
          doit( cb.getSelectedItem());
        }
      };
      nc3useRecords = prefs.getBoolean( "nc3useRecords", false);
      String tooltip = nc3useRecords ? "nc3 use Records" : "nc3 dont use Records";
      BAMutil.setActionProperties( v3Action, "V3", tooltip, true, 'V', -1);
      v3Action.putValue(BAMutil.STATE, new Boolean(nc3useRecords)); */

      if (addCoordButton) {
        AbstractAction coordAction = new AbstractAction() {
          public void actionPerformed(ActionEvent e) {
            Boolean state = (Boolean) getValue(BAMutil.STATE);
            addCoords = state.booleanValue();
            String tooltip = addCoords ? "add Coordinates is ON" : "add Coordinates is OFF";
            coordButt.setToolTipText(tooltip);
            //doit( cb.getSelectedItem()); // called from cb action listener
          }
        };
        addCoords = prefs.getBoolean("coordState", false);
        String tooltip2 = addCoords ? "add Coordinates is ON" : "add Coordinates is OFF";
        BAMutil.setActionProperties(coordAction, "addCoords", tooltip2, true, 'C', -1);
        coordAction.putValue(BAMutil.STATE, new Boolean(addCoords));
        coordButt = BAMutil.addActionToContainer(buttPanel, coordAction);
      }

      if (this instanceof GetDataRunnable) {
        stopButton = new StopButton("Stop");
        buttPanel.add(stopButton);
      }

      JPanel topPanel = new JPanel(new BorderLayout());
      topPanel.add(new JLabel(command), BorderLayout.WEST);
      topPanel.add(cb, BorderLayout.CENTER);
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
      if (debug) System.out.println(getClass().getName() + " process=" + command);

      busy = true;
      if (process(command)) {
        if (!defer) cb.addItem(command);
      }
      busy = false;
    }

    abstract boolean process(Object command);

    void save() {
      cb.save();
      //if (v3Butt != null) prefs.putBoolean("nc3useRecords", v3Butt.getModel().isSelected());
      if (coordButt != null) prefs.putBoolean("coordState", coordButt.getModel().isSelected());
    }

    void setSelectedItem(Object item) {
      eventOK = false;
      cb.setSelectedItem(item);
      eventOK = true;
    }
  }

  private class NCdumpPanel extends OpPanel implements GetDataRunnable {
    private GetDataTask task;
    NetcdfFile ncfile = null;
    String filename = null;
    String command = null;
    String result;

    NCdumpPanel(PreferencesExt prefs) {
      super(prefs, "command:");

      stopButton.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          if (task.isSuccess())
            ta.setText(result);
          else
            ta.setText(task.errMsg);

          if (task.isCancel())
            ta.appendLine("\n***Cancelled by User");

          ta.gotoTop();

          if (task.isSuccess() && !task.isCancel())
            cb.setSelectedItem(filename);
        }
      });
    }

    boolean process(Object o) {
      int pos;
      String input = ((String) o).trim();

      // deal with possibility of blanks in the filename
      if ((input.indexOf('"') == 0) && ((pos = input.indexOf('"', 1)) > 0)) {
        filename = input.substring(1, pos);
        command = input.substring(pos + 1);

      } else if ((input.indexOf('\'') == 0) && ((pos = input.indexOf('\'', 1)) > 0)) {
        filename = input.substring(1, pos);
        command = input.substring(pos + 1);

      } else {
        pos = input.indexOf(' ');
        if (pos > 0) {
          filename = input.substring(0, pos);
          command = input.substring(pos);
        } else {
          filename = input;
          command = null;
        }
      }

      task = new GetDataTask(this, filename, null);
      stopButton.startProgressMonitorTask(task);

      defer = true;
      return true;
    }

    public void run(Object o) throws IOException {
      try {
        if (addCoords)
          ncfile = NetcdfDataset.openDataset(filename, true, null);
        else
          ncfile = NetcdfDataset.openFile(filename, null);

        ByteArrayOutputStream bos = new ByteArrayOutputStream(10000);
        PrintStream ps = new PrintStream(bos);
        NCdump.print(ncfile, command, ps, task);
        result = bos.toString();

      } finally {
        try {
          if (ncfile != null) ncfile.close();
          ncfile = null;
        } catch (IOException ioe) {
        }
      }
    }

    // allow calling from outside
    void setNetcdfFile(NetcdfFile ncf) {
      this.ncfile = ncf;
      this.filename = ncf.getLocation();

      GetDataRunnable runner = new GetDataRunnable() {
        public void run(Object o) throws IOException {
          ByteArrayOutputStream bos = new ByteArrayOutputStream(10000);
          PrintStream ps = new PrintStream(bos);
          NCdump.print(ncfile, command, ps, task);
          result = bos.toString();
        }
      };
      task = new GetDataTask(runner, filename, null);
      stopButton.startProgressMonitorTask(task);
    }

    /* private class NCdumpTask extends thredds.ui.ProgressMonitorTask implements ucar.nc2.util.CancelTask {
      NetcdfFile ncfile;
      String contents, command;

      NCdumpTask(NetcdfFile ncfile, String command) {
        this.ncfile = ncfile;
        this.command = command;
      }

      public void run() {
        // LOOK: might be able to use JTextArea.read(Reader)
        ByteArrayOutputStream bos = new ByteArrayOutputStream(100000);
        PrintStream ps = new PrintStream(bos);
        try {
          NCdump.print(ncfile, command, ps, this);

        } catch (Exception e) {
          e.printStackTrace(new PrintStream(bos));
          contents = bos.toString();

          setError(e.getMessage());
          done = true;
          return;
        }

        if (cancel)
          ps.println("\n***Cancelled by User");
        contents = bos.toString();

        success = !cancel;
        done = true;
      }
    } */
  }

  /* private class WcsPanel extends OpPanel {
    private boolean ready = true;
    private StopButton stopButton;
    NetcdfFile ncfile = null;

    WcsPanel(PreferencesExt prefs) {
      super(prefs, "WCS server", "WCS server:");
    }

    boolean process(Object o) {
      String name = (String) o;

      boolean err = false;
      try {
        GridDataset gd = GridDataset.open( name);
        WcsDataset wcs = new WcsDataset( gd);
        ta.setText( wcs.getCapabilities());

      } catch (IOException ioe) {
        ta.setText("Cant open " + name);
        err = true;

      } catch (Exception ioe) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream(10000);
        ioe.printStackTrace();
        ioe.printStackTrace(new PrintStream(bos));
        ta.setText(bos.toString());
        err = true;
      }

      return !err;
    }
  } */

  private class UnitsPanel extends JPanel {
    PreferencesExt prefs;
    JSplitPane split;
    UnitDatasetCheck unitDataset;
    UnitConvert unitConvert;

    UnitsPanel(PreferencesExt prefs) {
      super();
      this.prefs = prefs;
      unitDataset = new UnitDatasetCheck((PreferencesExt) prefs.node("unitDataset"));
      unitConvert = new UnitConvert((PreferencesExt) prefs.node("unitConvert"));
      split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, new JScrollPane(unitDataset), unitConvert);
      split.setDividerLocation(prefs.getInt("splitPos", 500));
      setLayout(new BorderLayout());
      add(split, BorderLayout.CENTER);
    }

    void save() {
      prefs.putInt("splitPos", split.getDividerLocation());
      unitConvert.save();
      unitDataset.save();
    }
  }

  private class UnitDatasetCheck extends OpPanel {
    UnitDatasetCheck(PreferencesExt p) {
      super(p, "dataset:");
    }

    boolean process(Object o) {
      String command = (String) o;
      boolean err = false;

      NetcdfFile ncfile = null;
      try {
        ncfile = NetcdfDataset.openDataset(command, addCoords, null);

        ta.setText("Variables for " + command + ":");
        for (Iterator iter = ncfile.getVariables().iterator(); iter.hasNext();) {
          VariableEnhanced vs = (VariableEnhanced) iter.next();
          String units = vs.getUnitsString();
          StringBuffer sb = new StringBuffer();
          sb.append("   " + vs.getName() + " has unit= <" + units + ">");
          if (units != null)

            try {
              SimpleUnit su = SimpleUnit.factoryWithExceptions(units);
              sb.append(" unit convert = " + su.toString());
            } catch (Exception ioe) {
              sb.append(" unit convert failed ");
              sb.insert(0, "**** Fail ");
            }
          ta.appendLine(sb.toString());
        }

      } catch (FileNotFoundException ioe) {
        ta.setText("Failed to open <" + command + ">");
        err = true;

      } catch (IOException ioe) {
        ioe.printStackTrace();
        err = true;
      } finally {
        try {
          if (ncfile != null) ncfile.close();
        } catch (IOException ioe) {
        }
      }

      return !err;
    }
  }

  private class UnitConvert extends OpPanel {
    UnitConvert(PreferencesExt prefs) {
      super(prefs, "unit:", false, false);

      JButton compareButton = new JButton("Compare");
      compareButton.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          compare(cb.getSelectedItem());
        }
      });
      buttPanel.add(compareButton);

      JButton dateButton = new JButton("Date");
      dateButton.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          checkDate(cb.getSelectedItem());
        }
      });
      buttPanel.add(dateButton);
    }

    boolean process(Object o) {
      String command = (String) o;
      try {
        SimpleUnit su = SimpleUnit.factoryWithExceptions(command);
        ta.setText("toString()=" + su.toString() + "\n");
        ta.appendLine("getCanonicalString()=" + su.getUnit().getCanonicalString());
        ta.appendLine("class = " + su.getUnit().getClass().getName());
        return true;

      } catch (Exception e) {

        if (Debug.isSet("Xdeveloper")) {
          ByteArrayOutputStream bos = new ByteArrayOutputStream(10000);
          e.printStackTrace(new PrintStream(bos));
          ta.setText(bos.toString());
        } else {
          ta.setText(e.getClass().getName() + ":" + e.getMessage() + "\n" + command);
        }
        return false;
      }
    }

    void compare(Object o) {
      String command = (String) o;
      StringTokenizer stoke = new StringTokenizer(command);
      ArrayList list = new ArrayList();
      while (stoke.hasMoreTokens()) list.add(stoke.nextToken());

      try {
        String unitS1 = (String) list.get(0);
        String unitS2 = (String) list.get(1);
        SimpleUnit su1 = SimpleUnit.factoryWithExceptions(unitS1);
        SimpleUnit su2 = SimpleUnit.factoryWithExceptions(unitS2);
        ta.setText("<" + su1.toString() + "> isConvertable to <" + su2.toString() + ">=" +
                SimpleUnit.isCompatibleWithExceptions(unitS1, unitS2));

      } catch (Exception e) {

        if (Debug.isSet("Xdeveloper")) {
          ByteArrayOutputStream bos = new ByteArrayOutputStream(10000);
          e.printStackTrace(new PrintStream(bos));
          ta.setText(bos.toString());
        } else {
          ta.setText(e.getClass().getName() + ":" + e.getMessage() + "\n" + command);
        }

      }
    }

    void checkDate(Object o) {
      String command = (String) o;

      try {
        SimpleUnit su = SimpleUnit.factory(command);
        boolean isDate = su instanceof DateUnit;
        boolean isTime = su instanceof TimeUnit;
        ta.setText("<" + command + "> isDateUnit= " + isDate + " isTimeUnit= " + isTime);
        if (isDate) {
          DateUnit du = (DateUnit) su;
          ta.appendLine("\nDateUnit = " + du);
          ta.appendLine("Unit = " + du.getUnit());
          Date d = du.getDate();
          ta.appendLine("getStandardDateString = " + formatter.toDateTimeString(d));
          ta.appendLine("getDateOrigin = " + formatter.toDateTimeString(du.getDateOrigin()));
        }
        if (isTime) {
          TimeUnit du = (TimeUnit) su;
          ta.appendLine("\nTimeUnit = " + du);
          ta.appendLine("Unit = " + du.getUnit());
        }

      } catch (Exception e) {
        if (Debug.isSet("Xdeveloper")) {
          ByteArrayOutputStream bos = new ByteArrayOutputStream(10000);
          e.printStackTrace(new PrintStream(bos));
          ta.setText(bos.toString());
        } else {
          ta.setText(e.getClass().getName() + ":" + e.getMessage() + "\n" + command);
        }
      }

      Date d = DateUnit.getStandardOrISO( command);
      if (d == null)
        ta.appendLine("\nDateUnit.getStandardOrISO = false");
      else
        ta.appendLine("\nDateUnit.getStandardOrISO = " + formatter.toDateTimeString(d));

    }

  }

  private class CoordSysPanel extends OpPanel {
    NetcdfDataset ds = null;
    CoordSysTable coordSysTable;

    boolean useDefinition = false;
    JComboBox defComboBox;
    IndependentWindow defWindow;
    AbstractButton defButt;

    CoordSysPanel(PreferencesExt p) {
      super(p, "dataset:", true, false);
      coordSysTable = new CoordSysTable(prefs);
      add(coordSysTable, BorderLayout.CENTER);

      // allow to set a defintion file for GRIB
      AbstractAction defAction = new AbstractAction() {
        public void actionPerformed(ActionEvent e) {
          Boolean state = (Boolean) getValue(BAMutil.STATE);
          useDefinition = state.booleanValue();
          String tooltip = useDefinition ? "Use GRIB Definition File is ON" : "Use GRIB Definition File is OFF";
          defButt.setToolTipText(tooltip);
          if (useDefinition) {
            defWindow.show();
          }
        }
      };
      String tooltip2 = useDefinition ? "Use GRIB Definition File is ON" : "Use GRIB Definition File is OFF";
      BAMutil.setActionProperties(defAction, "dd", tooltip2, true, 'D', -1);
      defAction.putValue(BAMutil.STATE, new Boolean(useDefinition));
      defButt = BAMutil.addActionToContainer(buttPanel, defAction);

      defComboBox = new JComboBox( FmrcDefinition.fmrcDefinitionFiles);
      defWindow = new IndependentWindow("GRIB Definition File", null, defComboBox);
      defWindow.setLocationRelativeTo(defButt);

      AbstractButton infoButton = BAMutil.makeButtcon("Information", "Parse Info", false);
      infoButton.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          if (ds != null) {
            detailTA.setText(ds.getInfo().writeXML());
            detailTA.appendLine("----------------------");
            detailTA.appendLine(ds.getInfo().getParseInfo().toString());
            detailTA.gotoTop();
            detailWindow.show();
          }
        }
      });
      buttPanel.add(infoButton);

      JButton dsButton = new JButton("DSdump");
      dsButton.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          if (ds != null) {
            ByteArrayOutputStream bos = new ByteArrayOutputStream(10000);
            NetcdfDataset.debugDump(new PrintStream(bos), ds);
            detailTA.setText(bos.toString());
            detailTA.gotoTop();
            detailWindow.show();
          }
        }
      });
      buttPanel.add(dsButton);
    }

    boolean process(Object o) {
      String command = (String) o;
      boolean err = false;

      Object spiObject = null;
      if (useDefinition) {
        String currentDef = (String) defComboBox.getSelectedItem();
        if (currentDef != null) {
          FmrcDefinition fmrc_def = new FmrcDefinition();
          try {
            fmrc_def.readDefinitionXML(currentDef);
            spiObject = fmrc_def;
          } catch (IOException e) {
            e.printStackTrace();
          }
        }
      }

      ByteArrayOutputStream bos = new ByteArrayOutputStream(10000);
      try {
        ds = NetcdfDataset.openDataset(command, true, -1, null, spiObject);
        if (ds == null) {
          ta.setText("Failed to open <" + command + ">");
        } else {
          coordSysTable.setDataset(ds);
        }

      } catch (FileNotFoundException ioe) {
        JOptionPane.showMessageDialog(null, "NetcdfDataset cant open " + command + "\n" + ioe.getMessage());
        ta.setText("Failed to open <" + command + ">\n" + ioe.getMessage());
        err = true;

      } catch (Exception e) {
        e.printStackTrace();
        e.printStackTrace(new PrintStream(bos));
        ta.setText(bos.toString());
        err = true;
      } finally {
        try {
          if (ds != null) ds.close();
        } catch (IOException ioe) {
        }
      }

      return !err;
    }

    void save() {
      coordSysTable.save();
      prefs.putBeanObject(FRAME_SIZE, detailWindow.getBounds());
      super.save();
    }

  }

  private class NcmlPanel extends OpPanel {
    NetcdfDataset ds = null;
    String ncmlLocation = null;
    AbstractButton gButt = null;
    boolean useG;

    NcmlPanel(PreferencesExt p) {
      super(p, "dataset:");

      AbstractAction gAction = new AbstractAction() {
        public void actionPerformed(ActionEvent e) {
          Boolean state = (Boolean) getValue(BAMutil.STATE);
          useG = state.booleanValue();
          String tooltip = useG ? "use NcML-G" : "dont use NcML-G";
          gButt.setToolTipText(tooltip);
          //doit( cb.getSelectedItem()); // called from cb action listener
        }
      };
      useG = prefs.getBoolean("gState", false);
      String tooltip2 = useG ? "use NcML-G" : "dont use NcML-G";
      BAMutil.setActionProperties(gAction, "G", tooltip2, true, 'G', -1);
      gAction.putValue(BAMutil.STATE, new Boolean(useG));
      gButt = BAMutil.addActionToContainer(buttPanel, gAction);

      AbstractAction saveAction = new AbstractAction() {
        public void actionPerformed(ActionEvent e) {
          String location = ds.getLocation();
          int pos = location.lastIndexOf(".");
          if (pos > 0)
            location = location.substring(0,pos);
          String filename = fileChooser.chooseFilename(location+".ncml");
          if (filename == null) return;
          doSave(ta.getText(), filename);
        }
      };
      BAMutil.setActionProperties(saveAction, "Save", "Save NcML", false, 'S', -1);
      BAMutil.addActionToContainer(buttPanel, saveAction);

      AbstractAction netcdfAction = new AbstractAction() {
        public void actionPerformed(ActionEvent e) {
          String filename = fileChooser.chooseFilename();
          if (filename == null) return;
          doWriteNetCDF(ta.getText(), filename);
        }
      };
      BAMutil.setActionProperties(netcdfAction, "netcdf", "Write netCDF", false, 'N', -1);
      BAMutil.addActionToContainer(buttPanel, netcdfAction);

      AbstractAction transAction = new AbstractAction() {
        public void actionPerformed(ActionEvent e) {
          doTransform(ta.getText());
        }
      };
      BAMutil.setActionProperties(transAction, "netcdf", "Transformed NcML", false, 'T', -1);
      BAMutil.addActionToContainer(buttPanel, transAction);
    }

    boolean process(Object o) {
      ncmlLocation = (String) o;
      if (ncmlLocation.endsWith(".xml") || ncmlLocation.endsWith(".ncml")) {
        if (!ncmlLocation.startsWith("http:") && !ncmlLocation.startsWith("file:"))
          ncmlLocation = "file:" + ncmlLocation;
        String text = thredds.util.IO.readURLcontents(ncmlLocation);

        ta.setText(text);
      } else {
        writeNcml(ncmlLocation);
      }
      return true;
    }

    boolean writeNcml(String location) {
      boolean err = false;

      try {
        if (ds != null) ds.close();
      } catch (IOException ioe) {
      }

      ByteArrayOutputStream bos = new ByteArrayOutputStream(10000);
      try {
        ds = openDataset(location, addCoords, null);
        if (ds == null) {
          ta.setText("Failed to open <" + location + ">");
        } else {
          if (useG) {
            boolean showCoords = Debug.isSet("NcML/ncmlG-showCoords");
            ds.writeNcMLG(bos, showCoords, null);
          } else
            ds.writeNcML(bos, null);
          ta.setText(bos.toString());
          ta.gotoTop();
        }

      } catch (FileNotFoundException ioe) {
        ta.setText("Failed to open <" + location + ">");
        err = true;

      } catch (Exception e) {
        e.printStackTrace();
        e.printStackTrace(new PrintStream(bos));
        ta.setText(bos.toString());
        err = true;
      }

      return !err;
    }


    void doWriteNetCDF(String text, String filename) {
      if (debugNcmlWrite) {
        System.out.println("filename=" + filename);
        System.out.println("text=" + text);
      }
      try {
        ByteArrayInputStream bis = new ByteArrayInputStream(text.getBytes());
        NcMLReader.writeNcMLToFile(bis, filename);
        JOptionPane.showMessageDialog(this, "File successfully written");
      } catch (IOException ioe) {
        JOptionPane.showMessageDialog(this, "ERROR: " + ioe.getMessage());
        ioe.printStackTrace();
      }
    }

    void doTransform(String text) {
      try {
        ByteArrayInputStream bis = new ByteArrayInputStream(text.getBytes());
        NetcdfDataset ncd = NcMLReader.readNcML(bis, null);
        ByteArrayOutputStream bos = new ByteArrayOutputStream(10000);
        ncd.writeNcML(bos, null);
        ta.setText(bos.toString());
        ta.gotoTop();
        JOptionPane.showMessageDialog(this, "File successfully transformed");

      } catch (IOException ioe) {
        JOptionPane.showMessageDialog(this, "ERROR: " + ioe.getMessage());
        ioe.printStackTrace();
      }
    }

    void doSave(String text, String filename) {
      if (debugNcmlWrite) {
        System.out.println("filename=" + filename);
        System.out.println("text=" + text);
      }

      try {
        thredds.util.IO.writeToFile(text, new File(filename));
        JOptionPane.showMessageDialog(this, "File successfully written");
      } catch (IOException ioe) {
        JOptionPane.showMessageDialog(this, "ERROR: " + ioe.getMessage());
        ioe.printStackTrace();
      }
      // saveNcmlDialog.setVisible(false);
    }

    void save() {
      // prefs.putBeanObject(FRAME_SIZE, writeDataDialog.getBounds());
      // prefs.putBeanObject(FRAME_SIZE, saveNcmlDialog.getBounds());
      super.save();
    }

  }

  private class FmrcPanel extends OpPanel {
    private boolean useDefinition = false;
    private JComboBox defComboBox, catComboBox;
    private IndependentWindow defWindow, catWindow;
    private AbstractButton defButt;
    private JSpinner catSpinner;

    FmrcPanel(PreferencesExt p) {
      super(p, "dataset:", true, false);

      // allow to set a definition file for GRIB
      AbstractAction defineAction = new AbstractAction() {
        public void actionPerformed(ActionEvent e) {
          Boolean state = (Boolean) getValue(BAMutil.STATE);
          useDefinition = state.booleanValue();
          String tooltip = useDefinition ? "Use GRIB Definition File is ON" : "Use GRIB Definition File is OFF";
          defButt.setToolTipText(tooltip);
          if (useDefinition) {
            if (null == defComboBox) {
              defComboBox = new JComboBox( FmrcDefinition.fmrcDefinitionFiles);
              defComboBox.setEditable( true);
              defWindow = new IndependentWindow("GRIB Definition File", null, defComboBox);
              defWindow.setLocationRelativeTo(defButt);
              defWindow.setLocation(0, 100);
            }
            defWindow.show();
          }
        }
      };
      String tooltip2 = useDefinition ? "Use GRIB Definition File is ON" : "Use GRIB Definition File is OFF";
      BAMutil.setActionProperties(defineAction, "dd", tooltip2, true, 'D', -1);
      defineAction.putValue(BAMutil.STATE, new Boolean(useDefinition));
      defButt = BAMutil.addActionToContainer(buttPanel, defineAction);

      // make definition from catalog
      AbstractAction catAction = new AbstractAction() {
        public void actionPerformed(ActionEvent e) {
          if (null == catComboBox) {
            catComboBox = new JComboBox();
            catComboBox.setEditable( true);
            catSpinner = new JSpinner();
            JPanel catPanel = new JPanel();
            catPanel.add(catComboBox);
            catPanel.add(catSpinner);

            catComboBox.addActionListener( new ActionListener() {
              public void actionPerformed(ActionEvent e) {
                defineFromCatalog( (String) catComboBox.getSelectedItem(), catSpinner.getValue());
              }
            });
            catWindow = new IndependentWindow("Catalogs", null, catPanel);
            catWindow.setLocationRelativeTo(defButt);
            catWindow.setLocation(100, 100);
          }
          catWindow.show();
        }
      };
      BAMutil.setActionProperties(catAction, "catalog", "make definition from catalog", false, 'C', -1);
      BAMutil.addActionToContainer(buttPanel, catAction);

      // delete GRIB index
      AbstractAction deleteAction = new AbstractAction() {
        public void actionPerformed(ActionEvent e) {
          String currentFile = (String) cb.getSelectedItem();
          File file = new File(currentFile + ".gbx");
          if (file.exists()) {
            file.delete();
            JOptionPane.showMessageDialog(null, "Index deleted, reopen= " + currentFile);
            process( currentFile);
          }
        }
      };
      BAMutil.setActionProperties(deleteAction, "Delete", "Delete Grib Index", false, 'T', -1);
      BAMutil.addActionToContainer(buttPanel, deleteAction);
    }

    /* private void makeCatalogPanel() {
      PrefPanel catPP = new PrefPanel("cat", null);
      int row = 0;
      catPP.addTextComboField( DSCAN_ADDSIZE, "Add File Size", false, 0, row++);

      catPP.finish(false);

      catWindow = new IndependentWindow( "Catalog options", BAMutil.getImage( "thredds"), catPP);
      catWindow.setBounds(new Rectangle(150, 50, 700, 300));
    } */


    boolean process(Object o) {
      String command = (String) o;
      boolean err = false;
      ucar.nc2.dt.GridDataset gds = null;
      ByteArrayOutputStream bos = new ByteArrayOutputStream(10000);

      try {

        Object spiObject = null;
        if (useDefinition) {
          String currentDef = (String) defComboBox.getSelectedItem();
          if (currentDef != null) {
            FmrcDefinition fmrc_def = new FmrcDefinition();
            fmrc_def.readDefinitionXML(currentDef);
            spiObject = fmrc_def;
            NetcdfDataset ds = NetcdfDataset.openDataset(command, true, -1, null, spiObject);
            gds = new ucar.nc2.dt.grid.GridDataset(ds);
          } else {
            JOptionPane.showMessageDialog(null, "cant open Defintion file " + currentDef);
            return false;
          }

        } else {
          gds = ucar.nc2.dt.grid.GridDataset.open(command);
        }

        if (gds == null) {
          JOptionPane.showMessageDialog(null, "GridDataset.open cant open " + command);
          return false;
        }

        ForecastModelRunInventory fmrInv = ForecastModelRunInventory.open(gds, null);
        fmrInv.writeXML( bos);
        ta.setText(bos.toString());
        ta.gotoTop();

      } catch (FileNotFoundException ioe) {
        JOptionPane.showMessageDialog(null, "GridDataset.open cant open " + command + "\n" + ioe.getMessage());
        err = true;

      } catch (IOException ioe) {
        ioe.printStackTrace();
        ioe.printStackTrace(new PrintStream(bos));
        ta.setText(bos.toString());
        err = true;
      } finally {

        if (gds != null) try {
          gds.close();
        } catch (IOException e) {
        }
      }

      return !err;
    }

    private void defineFromCatalog(String catalogURLString, Object value) {
      int n = ((Integer) value).intValue();

      ByteArrayOutputStream bos = new ByteArrayOutputStream(10000);
      try {
        FmrcInventory fmrCollection = FmrcInventory.makeFromCatalog( catalogURLString, catalogURLString, n);
        FmrcDefinition def = new FmrcDefinition();
        def.makeFromCollectionInventory( fmrCollection);

        def.writeDefinitionXML( bos);
        ta.setText(bos.toString());
        ta.gotoTop();

      } catch (Exception ioe) {
        ioe.printStackTrace();
        ioe.printStackTrace(new PrintStream(bos));
        ta.setText(bos.toString());
      }
    }

  }

  private class GeoGridPanel extends OpPanel {
    GeoGridTable dsTable;
    JSplitPane split;
    IndependentWindow viewerWindow, imageWindow;
    GridUI gridUI = null;
    ImageViewPanel imageViewer;

    NetcdfDataset ds = null;

    GeoGridPanel(PreferencesExt prefs) {
      super(prefs, "dataset:", true, false);
      dsTable = new GeoGridTable(prefs, true);
      add(dsTable, BorderLayout.CENTER);

      AbstractButton viewButton = BAMutil.makeButtcon("alien", "Grid Viewer", false);
      viewButton.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          if (ds != null) {
            ucar.nc2.dt.grid.GridDataset gridDataset = dsTable.getGridDataset();
            if (gridUI == null) makeGridUI();
            gridUI.setDataset(gridDataset);
            viewerWindow.show();
          }
        }
      });
      buttPanel.add(viewButton);

      AbstractButton imageButton = BAMutil.makeButtcon("VCRMovieLoop", "Image Viewer", false);
      imageButton.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          if (ds != null) {
            GridDatatype grid = dsTable.getGrid();
            if (grid == null) return;
            if (imageWindow == null) makeImageWindow();
            imageViewer.setImageFromGrid(grid);
            imageWindow.show();
          }
        }
      });
      buttPanel.add(imageButton);

      AbstractButton infoButton = BAMutil.makeButtcon("Information", "Parse Info", false);
      infoButton.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          ucar.nc2.dt.grid.GridDataset gridDataset = dsTable.getGridDataset();
          if ((gridDataset != null) && (gridDataset.getParseInfo() != null)) {
            detailTA.setText(gridDataset.getParseInfo().toString());
            detailTA.gotoTop();
            detailWindow.show();
          }
        }
      });
      buttPanel.add(infoButton);

      JButton wcsButton = new JButton("WCS");
      wcsButton.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          if (ds != null) {
            ucar.nc2.dt.grid.GridDataset gridDataset = dsTable.getGridDataset();
            thredds.wcs.WcsDataset wcs = new thredds.wcs.WcsDataset(gridDataset, "", false);
            String gc;
            try {
              gc = wcs.getCapabilities();
              detailTA.setText(gc);
              detailTA.gotoTop();
              detailWindow.show();
            } catch (IOException e1) {
              e1.printStackTrace();
            }

          }
        }
      });
      buttPanel.add(wcsButton);

    }

    private void makeGridUI() {
       // a little tricky to get the parent right for GridUI
       viewerWindow = new IndependentWindow("Grid Viewer", BAMutil.getImage("netcdfUI"));

       gridUI = new GridUI((PreferencesExt) prefs.node("GridUI"), viewerWindow, fileChooser, 800);
       gridUI.addMapBean(new thredds.viewer.gis.worldmap.WorldMapBean());
       gridUI.addMapBean(new thredds.viewer.gis.shapefile.ShapeFileBean("WorldDetailMap", "Global Detailed Map", "WorldDetailMap", WorldDetailMap));
       gridUI.addMapBean(new thredds.viewer.gis.shapefile.ShapeFileBean("USDetailMap", "US Detailed Map", "USMap", USMap));

       viewerWindow.setComponent(gridUI);
       viewerWindow.setBounds((Rectangle) mainPrefs.getBean(GRIDVIEW_FRAME_SIZE, new Rectangle(77, 22, 700, 900)));
     }

    private void makeImageWindow() {
       imageWindow = new IndependentWindow("Grid Image Viewer", BAMutil.getImage("netcdfUI"));
       imageViewer = new ImageViewPanel(null);
       imageWindow.setComponent(imageViewer);
       imageWindow.setBounds((Rectangle) mainPrefs.getBean(GRIDIMAGE_FRAME_SIZE, new Rectangle(77, 22, 700, 900)));
    }

    boolean process(Object o) {
      String command = (String) o;
      boolean err = false;

      NetcdfDataset newds;
      ByteArrayOutputStream bos = new ByteArrayOutputStream(10000);
      try {
        newds = NetcdfDataset.openDataset(command, true, null);
        if (newds == null) {
          JOptionPane.showMessageDialog(null, "NetcdfDataset.open cant open " + command);
          return false;
        }
        setDataset(newds);

      } catch (FileNotFoundException ioe) {
        JOptionPane.showMessageDialog(null, "NetcdfDataset.open cant open " + command + "\n" + ioe.getMessage());
        err = true;

      } catch (Throwable ioe) {
        ioe.printStackTrace();
        ioe.printStackTrace(new PrintStream(bos));
        ta.setText(bos.toString());
        err = true;
      }

      return !err;
    }

    void setDataset(NetcdfDataset newds) {
      if (newds == null) return;
      try {
        if (ds != null) ds.close();
      } catch (IOException ioe) {
      }

      this.ds = newds;
      dsTable.setDataset(newds);
      setSelectedItem(newds.getLocation());
    }

    void save() {
      super.save();
      dsTable.save();
      prefs.putBeanObject(FRAME_SIZE, detailWindow.getBounds());
      if (gridUI != null) gridUI.storePersistentData();
      if (viewerWindow != null) mainPrefs.putBeanObject(GRIDVIEW_FRAME_SIZE, viewerWindow.getBounds());
      if (imageWindow != null) mainPrefs.putBeanObject(GRIDIMAGE_FRAME_SIZE, imageWindow.getBounds());
    }

  }

  private class RadialPanel extends OpPanel {
    RadialDatasetTable dsTable;
    JSplitPane split;
    IndependentWindow viewerWindow;

    RadialDatasetSweep ds = null;

    RadialPanel(PreferencesExt prefs) {
      super(prefs, "dataset:", true, false);
      dsTable = new RadialDatasetTable(prefs);
      add(dsTable, BorderLayout.CENTER);

      AbstractButton infoButton = BAMutil.makeButtcon("Information", "Parse Info", false);
      infoButton.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          RadialDatasetSweep radialDataset = dsTable.getRadialDataset();
          String info;
          if ((radialDataset != null) && ((info = radialDataset.getDetailInfo()) != null)) {
            detailTA.setText(info);
            detailTA.gotoTop();
            detailWindow.show();
          }
        }
      });
      buttPanel.add(infoButton);
    }

    boolean process(Object o) {
      String command = (String) o;
      boolean err = false;

      NetcdfDataset newds;
      ByteArrayOutputStream bos = new ByteArrayOutputStream(10000);
      try {
        newds = NetcdfDataset.openDataset(command, true, null);
        if (newds == null) {
          JOptionPane.showMessageDialog(null, "NetcdfDataset.open cant open " + command);
          return false;
        }
        ucar.nc2.dt.radial.RadialDatasetSweepFactory fac = new ucar.nc2.dt.radial.RadialDatasetSweepFactory();
        RadialDatasetSweep rds = fac.open(newds);
        setDataset(rds);

      } catch (FileNotFoundException ioe) {
        JOptionPane.showMessageDialog(null, "NetcdfDataset.open cant open " + command + "\n" + ioe.getMessage());
        err = true;

      } catch (IOException ioe) {
        ioe.printStackTrace();
        ioe.printStackTrace(new PrintStream(bos));
        ta.setText(bos.toString());
        err = true;
      }

      return !err;
    }

    void setDataset(ucar.nc2.dt.RadialDatasetSweep newds) {
      if (newds == null) return;
      try {
        if (ds != null) ds.close();
      } catch (IOException ioe) {
      }

      this.ds = newds;
      dsTable.setDataset(newds);
      setSelectedItem(newds.getLocationURI());
    }

    void save() {
      super.save();
      dsTable.save();
      prefs.putBeanObject(FRAME_SIZE, detailWindow.getBounds());
    }

  }

  private class ViewerPanel extends OpPanel {
    DatasetViewer dsViewer;
    JSplitPane split;
    NetcdfFile ncfile = null;

    ViewerPanel(PreferencesExt dbPrefs) {
      super(dbPrefs, "dataset:");
      dsViewer = new DatasetViewer(dbPrefs);
      add(dsViewer, BorderLayout.CENTER);

      AbstractButton infoButton = BAMutil.makeButtcon("Information", "Parse Info", false);
      infoButton.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          if (ncfile != null) {
            detailTA.setText(ncfile.getDetailInfo());
            detailTA.gotoTop();
            detailWindow.show();
          }
        }
      });
      buttPanel.add(infoButton);

      AbstractAction dumpAction = new AbstractAction() {
        public void actionPerformed(ActionEvent e) {
          NetcdfFile ds = dsViewer.getDataset();
          if (ds != null) {
            if (ncdumpPanel == null) {
              makeComponent("NCDump");
            }
            ncdumpPanel.setNetcdfFile(ds);
            tabbedPane.setSelectedComponent(ncdumpPanel);
          }
        }
      };
      BAMutil.setActionProperties(dumpAction, "Dump", "NCDump", false, 'D', -1);
      BAMutil.addActionToContainer(buttPanel, dumpAction);

      AbstractAction syncAction = new AbstractAction() {
        public void actionPerformed(ActionEvent e) {
          NetcdfFile ds = dsViewer.getDataset();
          if (ds != null)
            try {
              ds.syncExtend();
              dsViewer.setDataset(ds);
            } catch (IOException e1) {
              e1.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
        }
      };
      BAMutil.setActionProperties(syncAction, null, "Sync", false, 'D', -1);
      BAMutil.addActionToContainer(buttPanel, syncAction);
    }

    boolean process(Object o) {
      String command = (String) o;
      boolean err = false;

      try {
        if (ncfile != null) ncfile.close();
      } catch (IOException ioe) {
      }

      try {
        NetcdfFile ncnew = openFile(command, addCoords, null);
        if (ncnew != null)
          setDataset(ncnew);

      } catch (Exception ioe) {
        ioe.printStackTrace();
        ByteArrayOutputStream bos = new ByteArrayOutputStream(10000);
        ioe.printStackTrace(new PrintStream(bos));
        ta.setText(bos.toString());
        err = true;
      }

      return !err;
    }

    void setDataset(NetcdfFile nc) {
      try {
        if (ncfile != null) ncfile.close();
        ncfile = null;
      } catch (IOException ioe) {
      }
      ncfile = nc;

      if (ncfile != null) {
        dsViewer.setDataset(nc);
        setSelectedItem(nc.getLocation());
      }
    }


    void save() {
      super.save();
      dsViewer.save();
    }

  }

  private class PointObsPanel extends OpPanel {
    PointObsViewer povTable;
    JSplitPane split;
    PointObsDataset pobsDataset = null;

    PointObsPanel(PreferencesExt dbPrefs) {
      super(dbPrefs, "dataset:", true, false);
      povTable = new PointObsViewer(dbPrefs);
      add(povTable, BorderLayout.CENTER);

      AbstractButton infoButton = BAMutil.makeButtcon("Information", "Dataset Info", false);
      infoButton.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          String info;
          if ((pobsDataset != null) && ((info = pobsDataset.getDetailInfo()) != null)) {
            detailTA.setText(info);
            detailTA.gotoTop();
            detailWindow.show();
          }
        }
      });
      buttPanel.add(infoButton);
    }

    boolean process(Object o) {
      String location = (String) o;
      return setPointObsDataset(location);
    }

    void save() {
      super.save();
      povTable.save();
    }

    boolean setPointObsDataset(String location) {
      if (location == null) return false;

      try {
        if (pobsDataset != null) pobsDataset.close();
      } catch (IOException ioe) {
      }

      StringBuffer log = new StringBuffer();
      ByteArrayOutputStream bos = new ByteArrayOutputStream(10000);
      try {
        pobsDataset = PointObsDatasetFactory.open(location, null, log);
        if (pobsDataset == null) {
          JOptionPane.showMessageDialog(null, "Can't open " + location+": "+log);
          return false;
        }


        povTable.setDataset(pobsDataset);
        setSelectedItem(location);
        return true;

      } catch (IOException ioe) {
        ioe.printStackTrace();
        ioe.printStackTrace(new PrintStream(bos));
        ta.setText(log.toString());
        ta.appendLine(bos.toString());
        return false;
      }
    }

    boolean setPointObsDataset(PointObsDataset dataset) {
      if (dataset == null) return false;

      try {
        if (pobsDataset != null) pobsDataset.close();
      } catch (IOException ioe) {
      }

      povTable.setDataset(dataset);
      pobsDataset = dataset;
      setSelectedItem(pobsDataset.getLocationURI());
      return true;
    }
  }

  private class TrajectoryTablePanel extends OpPanel {
    TrajectoryObsViewer viewer;
    JSplitPane split;
    TrajectoryObsDatasetFactory trajDatasetFactory = null;
    TrajectoryObsDataset ds = null;

    TrajectoryTablePanel(PreferencesExt dbPrefs) {
      super(dbPrefs, "dataset:", true, false);
      viewer = new TrajectoryObsViewer(dbPrefs);

      add(viewer, BorderLayout.CENTER);

      AbstractButton infoButton = BAMutil.makeButtcon("Information", "Dataset Info", false);
      infoButton.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          String info;
          if ((ds != null) && ((info = ds.getDetailInfo()) != null)) {
            detailTA.setText(info);
            detailTA.gotoTop();
            detailWindow.show();
          }
        }
      });
      buttPanel.add(infoButton);
    }

    boolean process(Object o) {
      String location = (String) o;
      return setStationObsDataset(location);
    }

    void save() {
      super.save();
      viewer.save();
    }

    boolean setStationObsDataset(String location) {
      if (location == null) return false;

      try {
        if (ds != null) ds.close();
      } catch (IOException ioe) {
      }

      ByteArrayOutputStream bos = new ByteArrayOutputStream(10000);
      try {
        if (trajDatasetFactory == null) trajDatasetFactory = new TrajectoryObsDatasetFactory();
        ds = TrajectoryObsDatasetFactory.open(location);
        if (ds == null)
          return false;

        viewer.setDataset(ds);
        setSelectedItem(location);
        return true;

      } catch (IOException ioe) {
        ioe.printStackTrace();
        ioe.printStackTrace(new PrintStream(bos));
        ta.setText(ioe.getMessage()); // trajDatasetFactory.getErrorMessages());
        ta.appendLine(bos.toString());
        return false;
      }
    }

    boolean setStationObsDataset(TrajectoryObsDataset sobsDataset) {
      if (sobsDataset == null) return false;

      try {
        if (ds != null) ds.close();
      } catch (IOException ioe) {
      }

      viewer.setDataset(sobsDataset);
      ds = sobsDataset;
      setSelectedItem(ds.getLocationURI());
      return true;
    }
  }

  private class ImagePanel extends OpPanel {
    ImageViewPanel imagePanel;
    JSplitPane split;

    ImagePanel(PreferencesExt dbPrefs) {
      super(dbPrefs, "dataset:", true, false);
      imagePanel = new ImageViewPanel(buttPanel);
      add(imagePanel, BorderLayout.CENTER);
    }

    boolean process(Object o) {
      String command = (String) o;

      ByteArrayOutputStream bos = new ByteArrayOutputStream(10000);
      try {
        if (null != command)
          imagePanel.setImageFromUrl(command);

      } catch (Exception ioe) {
        ioe.printStackTrace();
        ioe.printStackTrace(new PrintStream(bos));
        // ta.setText( datasetFactory.getErrorMessages());
        ta.appendLine(bos.toString());
        return false;
      }

      return true;
    }

    void setImageLocation(String location) {
      imagePanel.setImageFromUrl(location);
      setSelectedItem(location);
    }

  }

  private class GeotiffPanel extends OpPanel {
    GeotiffPanel(PreferencesExt p) {
      super(p, "netcdf:", true, false);

      JButton readButton = new JButton("read geotiff");
      readButton.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          String item = cb.getSelectedItem().toString();
          String fname = item.trim();
          read(fname);
        }
      });
      buttPanel.add(readButton);
    }

    boolean process(Object o) {
      String filename = (String) o;

      ucar.nc2.dt.grid.GridDataset gridDs = null;
      try {
        gridDs = ucar.nc2.dt.grid.GridDataset.open(filename);
        java.util.List grids = gridDs.getGrids();
        if (grids.size() == 0) {
          System.out.println("No grids found.");
          return false;
        }

        GridDatatype grid = (GridDatatype) grids.get(0);
        ucar.ma2.Array data = grid.readDataSlice(0, 0, -1, -1); // first time, level

        String name = Integer.toString(filename.hashCode());
        String fileOut = "C:/temp/wcs/" + name + ".tif";

        ucar.nc2.geotiff.GeotiffWriter writer = new ucar.nc2.geotiff.GeotiffWriter(fileOut);
        writer.writeGrid(gridDs, grid, data, true);

        read(fileOut);

      } catch (IOException ioe) {
        ioe.printStackTrace();
        return false;
      } finally {
        try {
          if (gridDs != null) gridDs.close();
        } catch (IOException ioe) {
        }
      }
      return true;
    }

    void read(String filename) {
      GeoTiff geotiff = null;
      try {
        geotiff = new GeoTiff(filename);
        geotiff.read();
        ta.setText(geotiff.showInfo());
      } catch (IOException ioe) {
        ioe.printStackTrace();

      } finally {
        try {
          if (geotiff != null) geotiff.close();
        } catch (IOException ioe) {
        }
      }
    }

  }

  private interface GetDataRunnable {
    public void run(Object o) throws IOException;
  }

  private class GetDataTask extends thredds.ui.ProgressMonitorTask implements ucar.nc2.util.CancelTask {
    GetDataRunnable getData;
    Object o;
    String name, errMsg = null;
    Exception ex;

    GetDataTask(GetDataRunnable getData, String name, Object o) {
      this.getData = getData;
      this.name = name;
      this.o = o;
    }

    public void run() {
      try {
        getData.run(o);

      } catch (FileNotFoundException ioe) {
        errMsg = ("Cant open " + name + " " + ioe.getMessage());
        // ioe.printStackTrace();
        success = false;
        done = true;
        return;

      } catch (Exception e) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream(10000);
        e.printStackTrace(new PrintStream(bos));
        errMsg = bos.toString();
        ex = e;
        success = false;
        done = true;
        return;
      }

      success = true;
      done = true;
    }
  }

/*  private class NioPanel extends OpPanel {

    NioPanel(PreferencesExt prefs) {
      super( prefs, "read NIO", "filename:");
    }

    void process(Object o) {
      String fname = (String) o;
      try {
        ucar.nc2.nio.NetcdfFile nioFile = new ucar.nc2.nio.NetcdfFile( fname);
        ta.setText( nioFile.getDebugReadInfo());
        ta.append( "--------------------------\n");
        ta.append( nioFile.toString());

        Iterator iter = nioFile.getVariableIterator();
        while (iter.hasNext()) {
          ucar.nc2.nio.Variable v = (ucar.nc2.nio.Variable) iter.next();
          v.read();
          ta.append(" "+v.getName()+" read OK\n");
        }

        nioFile.close();
      } catch (IOException ioe) {
        ta.setText( "IOException on "+fname+"\n"+ioe.getMessage());
        ioe.printStackTrace();
      }
    }
  } */

  ///////////////////////////////////////////////////////////////////////////////
  // Dynamic proxy for Debug
  private class DebugProxyHandler implements java.lang.reflect.InvocationHandler {
    public Object invoke(Object proxy, java.lang.reflect.Method method, Object[] args) throws Throwable {
      if (method.getName().equals("toString"))
        return super.toString();
      // System.out.println("proxy= "+proxy+" method = "+method+" args="+args);
      if (method.getName().equals("isSet")) {
        return new Boolean(ucar.util.prefs.ui.Debug.isSet((String) args[0]));
      }
      if (method.getName().equals("set")) {
        ucar.util.prefs.ui.Debug.set((String) args[0], ((Boolean) args[1]).booleanValue());
        return null;
      }
      return Boolean.FALSE;
    }
  }

  /////////////////////////////////////////////////////////////////////////////
  // About Window
  private class AboutWindow extends javax.swing.JWindow {
    public AboutWindow() {
      super(parentFrame);

      JLabel lab1 = new JLabel("<html> <body bgcolor=\"#FFECEC\"> <center>" +
              "<h1>Netcdf 2.2 Toolset</h1>" +
              "<b>" + getVersion() + "</b>" +
              "<br><i>http://www.unidata.ucar.edu/packages/netcdf-java/</i>" +
              "<br><b><i>Developers:</b>John Caron, Ethan Davis, Robb Kambic, Yuan Ho</i></b>" +
              "</center>" +
              "<br><br>With thanks to these <b>Open Source</b> contributers:" +
              "<ul>" +
              "<li><b>ADDE/VisAD</b>: Bill Hibbard, Don Murray, Tom Whittaker, et al (http://www.ssec.wisc.edu/~billh/visad.html)</li>" +
              "<li><b>Apache Commons HttpClient</b> library: (http://http://jakarta.apache.org/commons/httpclient//)</li>" +
              "<li><b>Apache Log4J</b> library: (http://logging.apache.org/log4j/) </li>" +
              "<li><b>IDV:</b> Don Murray, Jeff McWhirter, Doug Lindholm (http://www.unidata.ucar.edu/software/IDV/)</li>" +
              "<li><b>JDOM</b> library: Jason Hunter, Brett McLaughlin et al (www.jdom.org)</li>" +
              "<li><b>JUnit</b> library: Erich Gamma, Kent Beck, Erik Meade, et al (http://sourceforge.net/projects/junit/)</li>" +
              "<li><b>OpenDAP Java</b> library: Nathan Potter, James Gallagher, Don Denbo, et. al.(http://opendap.org)</li>" +
              " </ul><center>Special thanks to <b>Sun Microsystems</b> (java.sun.com) for the platform on which we stand." +
              " </center></body></html> ");

      JPanel main = new JPanel(new BorderLayout());
      main.setBorder(new javax.swing.border.LineBorder(Color.BLACK));
      main.setBackground(new Color(0xFFECEC));

      JLabel icon = new JLabel(new ImageIcon(BAMutil.getImage("netcdfUI")));
      icon.setOpaque(true);
      icon.setBackground(new Color(0xFFECEC));

      JLabel threddsLogo = new JLabel(Resource.getIcon(BAMutil.getResourcePath() + "threddsLogo.png", false));
      threddsLogo.setBackground(new Color(0xFFECEC));
      threddsLogo.setOpaque(true);

      main.add(icon, BorderLayout.NORTH);
      main.add(lab1, BorderLayout.CENTER);
      main.add(threddsLogo, BorderLayout.SOUTH);
      getContentPane().add(main);
      pack();

      //show();
      java.awt.Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
      java.awt.Dimension labelSize = this.getPreferredSize();
      setLocation(screenSize.width / 2 - (labelSize.width / 2), screenSize.height / 2 - (labelSize.height / 2));
      addMouseListener(new MouseAdapter() {
        public void mousePressed(MouseEvent e) {
          setVisible(false);
        }
      });
      setVisible(true);
      //System.out.println("AW ok getPreferredSize="+getPreferredSize()+" screenSize="+screenSize);
    }
  }

  private String version = null;

  private String getVersion() {

    try {
      InputStream is = thredds.util.Resource.getFileResource("/README");
      if (is == null) return "N/A";
      // DataInputStream dataIS = new DataInputStream( new BufferedInputStream(ios, 20000));
      BufferedReader dataIS = new BufferedReader(new InputStreamReader(is));
      StringBuffer sbuff = new StringBuffer();
      for (int i = 0; i < 3; i++) {
        sbuff.append(dataIS.readLine());
        sbuff.append("<br>");
      }
      version = sbuff.toString();
    } catch (IOException ioe) {
      ioe.printStackTrace();
      version = "version unknown";
    }

    return version;
  }

  // Splash Window
  private static class SplashScreen extends javax.swing.JWindow {
    public SplashScreen() {
      Image image = Resource.getImage("/resources/nj22/ui/pix/ring2.jpg");
      ImageIcon icon = new ImageIcon(image);
      JLabel lab = new JLabel(icon);
      getContentPane().add(lab);
      pack();

      //show();
      java.awt.Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
      int width = image.getWidth(null);
      int height = image.getHeight(null);
      setLocation(screenSize.width / 2 - (width / 2), screenSize.height / 2 - (height / 2));
      addMouseListener(new MouseAdapter() {
        public void mousePressed(MouseEvent e) {
          setVisible(false);
        }
      });
      setVisible(true);
    }
  }

  //////////////////////////////////////////////////////////////////////////
  static private void exit() {
    ui.save();
    Rectangle bounds = frame.getBounds();
    prefs.putBeanObject(FRAME_SIZE, bounds);
    try {
      store.save();
    } catch (IOException ioe) {
      ioe.printStackTrace();
    }

    done = true; // on some systems, still get a window close event
    NetcdfFileCache.exit(); // kill the timer thread
    NetcdfDatasetCache.exit(); // kill the timer thread
    System.exit(0);
  }

  // handle messages
  private static ToolsUI ui;
  private static JFrame frame;
  private static PreferencesExt prefs;
  private static XMLStore store;
  private static boolean done = false;

  private static String wantDataset = null;

  private static void setDataset() {
    SwingUtilities.invokeLater(new Runnable() { // do it in the swing event thread

      public void run() {
        ui.makeComponent("THREDDS");
        ui.threddsUI.setDataset(wantDataset);
        ui.tabbedPane.setSelectedComponent(ui.threddsUI);
      }
    });
  }

  static boolean isCacheInit = false;
  static private void initCaches() {
    NetcdfFileCache.init(50, 70, 20 * 60);
    NetcdfDatasetCache.init(20, 40, 20 * 60);
    isCacheInit = true;
  }

  public static void main(String args[]) {

    final SplashScreen splash = new SplashScreen();

    if (debugListen) {
      System.out.println("Arguments:");
      for (int i = 0; i < args.length; i++) {
        String arg = args[i];
        System.out.println(" " + arg);
      }
    }

    SocketMessage sm;

    // handle multiple versions of ToolsUI, along with passing a dataset name
    if (args.length > 0) {
      // munge arguments into a single string
      StringBuffer sbuff = new StringBuffer();
      for (int i = 0; i < args.length; i++) {
        sbuff.append(args[i]);
        sbuff.append(" ");
      }
      String arguments = sbuff.toString();

      // LOOK - why does it have to start with http ??
      if (arguments.startsWith("http:")) {
        wantDataset = arguments;

        // see if another version is running, if so send it the message
        sm = new SocketMessage(14444, wantDataset);
        if (sm.isAlreadyRunning()) {
          System.out.println("ToolsUI already running - pass argument= '" + wantDataset + "' to it and exit");
          System.exit(0);
        }
      }
    }

    // look for messages from another ToolsUI
    sm = new SocketMessage(14444, null);
    if (sm.isAlreadyRunning()) {
      System.out.println("ToolsUI already running - start up another copy");
      sm = null;
    } else {
      sm.addEventListener(new SocketMessage.EventListener() {
        public void setMessage(SocketMessage.Event event) {
          wantDataset = event.getMessage();
          if (debugListen)System.out.println(" got message= '" + wantDataset);
          setDataset();
        }
      });
    }

    // prefs storage
    try {
      String prefStore = ucar.util.prefs.XMLStore.makeStandardFilename(".unidata", "NetcdfUI22.xml");
      store = ucar.util.prefs.XMLStore.createFromFile(prefStore, null);
      prefs = store.getPreferences();
      Debug.setStore(prefs.node("Debug"));
    } catch (IOException e) {
      System.out.println("XMLStore Creation failed " + e);
    }

    // initializations
    BAMutil.setResourcePath("/resources/nj22/ui/icons/");
    // initCaches();
    // LOOK ucar.nc2.dods.DODSNetcdfFile.setAllowSessions( true);  // turned off to allow debugging of typical access

    // for efficiency, persist aggregations. every hour, delete stuff older than 30 days
    Aggregation.setPersistenceCache( new DiskCache2("/.nj22/cachePersist", true, 60 * 24 * 30, 60));

    // test
    // java.util.logging.Logger.getLogger("ucar.nc2").setLevel( java.util.logging.Level.SEVERE);

    // put UI in a JFrame
    frame = new JFrame("NetCDF (2.2) Tools");
    ui = new ToolsUI(prefs, frame);

    frame.setIconImage(BAMutil.getImage("netcdfUI"));

    frame.addWindowListener(new WindowAdapter() {
      public void windowActivated(WindowEvent e) {
        splash.setVisible(false);
        splash.dispose();
      }

      public void windowClosing(WindowEvent e) {
        if (!done) exit();
      }
    });

    frame.getContentPane().add(ui);
    Rectangle bounds = (Rectangle) prefs.getBean(FRAME_SIZE, new Rectangle(50, 50, 800, 450));
    frame.setBounds(bounds);

    frame.pack();
    frame.setBounds(bounds);
    frame.setVisible(true);

    // set Authentication for accessing passsword protected services like TDS PUT
    java.net.Authenticator.setDefault(new thredds.ui.UrlAuthenticatorDialog(frame));
    thredds.util.net.HttpSession.setCredentialsProvider(new thredds.ui.UrlAuthenticatorDialog(frame));

    // load protocol for ADDE URLs
    URLStreamHandlerFactory.install();
    URLStreamHandlerFactory.register("adde", new edu.wisc.ssec.mcidas.adde.AddeURLStreamHandler());

    // in case a dataset was on the command line
    if (wantDataset != null)
      setDataset();
  }
}