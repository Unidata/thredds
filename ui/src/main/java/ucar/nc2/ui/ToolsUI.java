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

package ucar.nc2.ui;

import thredds.inventory.FeatureCollectionConfig;
import ucar.nc2.*;
import ucar.nc2.FileWriter;
import ucar.nc2.stream.CdmRemote;
import ucar.nc2.ft.FeatureDatasetPoint;
import ucar.nc2.ft.FeatureDatasetFactoryManager;
import ucar.nc2.ft.FeatureDataset;
import ucar.nc2.ft.point.writer.WriterCFPointDataset;
import ucar.nc2.ft.point.PointDatasetImpl;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.ncml.NcMLWriter;
import ucar.nc2.thredds.ThreddsDataFactory;
import ucar.nc2.thredds.DqcRadarDatasetCollection;
import ucar.nc2.ncml.NcMLReader;
import ucar.nc2.ncml.Aggregation;
import ucar.nc2.dt.*;
import ucar.nc2.dt.radial.StationRadarCollectionImpl;
import ucar.nc2.dt.fmrc.FmrcDefinition;
import ucar.nc2.dt.fmrc.ForecastModelRunInventory;
import ucar.nc2.dt.fmrc.FmrcInventory;
import ucar.nc2.dt.fmrc.FmrcImpl;
import ucar.nc2.dataset.*;

import ucar.nc2.geotiff.GeoTiff;
import ucar.nc2.util.*;
import ucar.nc2.util.net.HttpClientManager;
import ucar.nc2.util.xml.RuntimeConfigParser;
import ucar.nc2.units.*;

import ucar.nc2.ui.grid.GridUI;
import ucar.nc2.ui.image.ImageViewPanel;
import ucar.nc2.ui.util.*;

import ucar.unidata.io.http.HTTPRandomAccessFile;

import ucar.util.prefs.*;
import ucar.util.prefs.ui.*;
import ucar.grib.grib2.Grib2Dump;

import thredds.catalog.query.DqcFactory;
import thredds.wcs.v1_0_0_1.GetCapabilities;
import thredds.wcs.v1_0_0_1.WcsException;
import thredds.inventory.MController;
import thredds.inventory.bdb.MetadataManager;
import thredds.ui.*;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.List;

import javax.swing.*;
import javax.swing.text.PlainDocument;
import javax.swing.event.*;

import org.apache.commons.httpclient.auth.CredentialsProvider;
import org.apache.commons.httpclient.HttpClient;

import org.springframework.context.*;
import org.springframework.context.support.*;
import org.bounce.text.ScrollableEditorPanel;
import org.bounce.text.LineNumberMargin;
import org.bounce.text.xml.XMLStyleConstants;
import org.bounce.text.xml.XMLDocument;
import org.bounce.text.xml.XMLEditorKit;

/**
 * Netcdf Tools user interface   .
 *
 * @author caron
 */

public class ToolsUI extends JPanel {
  static private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ToolsUI.class);

  static private final String WorldDetailMap = "/optional/nj22/maps/Countries.zip";
  static private final String USMap = "/optional/nj22/maps/US.zip";

  static private final String FRAME_SIZE = "FrameSize";
  static private final String GRIDVIEW_FRAME_SIZE = "GridUIWindowSize";
  static private final String GRIDIMAGE_FRAME_SIZE = "GridImageWindowSize";
  static private boolean debugListen = false;

  private ucar.util.prefs.PreferencesExt mainPrefs;

  // UI
  private AggPanel aggPanel;
  private BufrPanel bufrPanel;
  private BufrTableBPanel bufrTableBPanel;
  private BufrTableDPanel bufrTableDPanel;
  private CoordSysPanel coordSysPanel;
  private CollectionPanel collectionPanel;
  private FeatureScanPanel ftPanel;
  private FmrcInvPanel fmrcInvPanel;
  private FmrcImplPanel fmrcImplPanel;
  private FmrcPanel fmrcPanel;
  private GeoGridPanel gridPanel;
  private GribPanel gribPanel;
  private Grib2Panel grib2Panel;
  private Hdf5Panel hdf5Panel;
  private Hdf4Panel hdf4Panel;
  private ImagePanel imagePanel;
  private NCdumpPanel ncdumpPanel;
  private OpPanel ncmlEditorPanel, geotiffPanel;
  private PointObsPanel pointObsPanel;
  private StationObsPanel stationObsPanel;
  private PointFeaturePanel pointFeaturePanel;
  private StationRadialPanel stationRadialPanel;
  private RadialPanel radialPanel;
  private ThreddsUI threddsUI;
  private TrajectoryTablePanel trajTablePanel;
  private UnitsPanel unitsPanel;
  private URLDumpPane urlPanel;
  private ViewerPanel viewerPanel;
  private WmsPanel wmsPanel;

  private JTabbedPane tabbedPane;
  private JTabbedPane iospTabPane;
  private JTabbedPane ftTabPane;
  private JTabbedPane fmrcTabPane;
  private JTabbedPane ncmlTabPane;

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
  private boolean debug = false, debugTab = false, debugNcmlWrite = false, debugCB = false;


  public ToolsUI(ucar.util.prefs.PreferencesExt prefs, JFrame parentFrame) {
    this.mainPrefs = prefs;
    this.parentFrame = parentFrame;

    // FileChooser is shared
    javax.swing.filechooser.FileFilter[] filters = new javax.swing.filechooser.FileFilter[2];
    filters[0] = new FileManager.HDF5ExtFilter();
    filters[1] = new FileManager.NetcdfExtFilter();
    fileChooser = new FileManager(parentFrame, null, filters, (PreferencesExt) prefs.node("FileManager"));

    // all the tabbed panes
    tabbedPane = new JTabbedPane(JTabbedPane.TOP);
    iospTabPane = new JTabbedPane(JTabbedPane.TOP);
    ftTabPane = new JTabbedPane(JTabbedPane.TOP);
    fmrcTabPane = new JTabbedPane(JTabbedPane.TOP);
    ncmlTabPane = new JTabbedPane(JTabbedPane.TOP);

    // the widgets in the top level tabbed pane
    viewerPanel = new ViewerPanel((PreferencesExt) mainPrefs.node("varTable"));
    tabbedPane.addTab("Viewer", viewerPanel);

    // all the other component are defferred for fast startup
    tabbedPane.addTab("NCDump", new JLabel("NCDump"));
    tabbedPane.addTab("Iosp", iospTabPane);
    tabbedPane.addTab("CoordSys", new JLabel("CoordSys"));
    tabbedPane.addTab("FeatureTypes", ftTabPane);
    tabbedPane.addTab("THREDDS", new JLabel("THREDDS"));
    tabbedPane.addTab("Fmrc", fmrcTabPane);
    tabbedPane.addTab("GeoTiff", new JLabel("GeoTiff"));
    tabbedPane.addTab("Units", new JLabel("Units"));
    tabbedPane.addTab("NcML", ncmlTabPane);
    tabbedPane.addTab("URLdump", new JLabel("URLdump"));
    tabbedPane.setSelectedIndex(0);
    tabbedPane.addChangeListener(new ChangeListener() {
      public void stateChanged(ChangeEvent e) {
        Component c = tabbedPane.getSelectedComponent();
        if (c instanceof JLabel) {
          int idx = tabbedPane.getSelectedIndex();
          String title = tabbedPane.getTitleAt(idx);
          makeComponent(tabbedPane, title);
        }
      }
    });

    setLayout(new BorderLayout());
    add(tabbedPane, BorderLayout.CENTER);

    // nested tab - iosp
    iospTabPane.addTab("BUFR", new JLabel("BUFR"));
    iospTabPane.addTab("BUFRTableB", new JLabel("BUFRTableB"));
    iospTabPane.addTab("BUFRTableD", new JLabel("BUFRTableD"));
    iospTabPane.addTab("GRIB", new JLabel("GRIB"));
    iospTabPane.addTab("GRIB2", new JLabel("GRIB2"));
    iospTabPane.addTab("HDF5", new JLabel("HDF5"));
    iospTabPane.addTab("HDF4", new JLabel("HDF4"));
    iospTabPane.addChangeListener(new ChangeListener() {
      public void stateChanged(ChangeEvent e) {
        Component c = iospTabPane.getSelectedComponent();
        if (c instanceof JLabel) {
          int idx = iospTabPane.getSelectedIndex();
          String title = iospTabPane.getTitleAt(idx);
          makeComponent(iospTabPane, title);
        }
      }
    });
    iospTabPane.addComponentListener(new ComponentAdapter() {
      public void componentShown(ComponentEvent e) {
        Component c = iospTabPane.getSelectedComponent();
        if (c instanceof JLabel) {
          int idx = iospTabPane.getSelectedIndex();
          String title = iospTabPane.getTitleAt(idx);
          makeComponent(iospTabPane, title);
        }
      }
    });

    // nested tab - features
    ftTabPane.addTab("Grids", new JLabel("Grids"));
    ftTabPane.addTab("WMS", new JLabel("WMS"));
    ftTabPane.addTab("PointFeature", new JLabel("PointFeature"));
    //ftTabPane.addTab("PointObs", new JLabel("PointObs"));
    //ftTabPane.addTab("StationObs", new JLabel("StationObs"));
    //ftTabPane.addTab("Trajectory", new JLabel("Trajectory"));
    ftTabPane.addTab("Images", new JLabel("Images"));
    ftTabPane.addTab("Radial", new JLabel("Radial"));
    ftTabPane.addTab("StationRadial", new JLabel("StationRadial"));
    ftTabPane.addTab("FeatureScan", new JLabel("FeatureScan"));
    ftTabPane.addChangeListener(new ChangeListener() {
      public void stateChanged(ChangeEvent e) {
        Component c = ftTabPane.getSelectedComponent();
        if (c instanceof JLabel) {
          int idx = ftTabPane.getSelectedIndex();
          String title = ftTabPane.getTitleAt(idx);
          makeComponent(ftTabPane, title);
        }
      }
    });
    ftTabPane.addComponentListener(new ComponentAdapter() {
      public void componentShown(ComponentEvent e) {
        Component c = ftTabPane.getSelectedComponent();
        if (c instanceof JLabel) {
          int idx = ftTabPane.getSelectedIndex();
          String title = ftTabPane.getTitleAt(idx);
          makeComponent(ftTabPane, title);
        }
      }
    });

    // nested tab - fmrc
    //fmrcPanel = new FmrcPanel((PreferencesExt) mainPrefs.node("fmrc2"));
    //fmrcImplPanel = new FmrcImplPanel((PreferencesExt) mainPrefs.node("fmrcImpl"));
    fmrcTabPane.addTab("Fmrc", new JLabel("Fmrc"));
    fmrcTabPane.addTab("Collections", new JLabel("Collections"));
    fmrcTabPane.addTab("FmrcImpl", new JLabel("FmrcImpl"));
    fmrcTabPane.addTab("Inventory", new JLabel("Inventory"));
    fmrcTabPane.addChangeListener(new ChangeListener() {
      public void stateChanged(ChangeEvent e) {
        Component c = fmrcTabPane.getSelectedComponent();
        if (c instanceof JLabel) {
          int idx = fmrcTabPane.getSelectedIndex();
          String title = fmrcTabPane.getTitleAt(idx);
          makeComponent(fmrcTabPane, title);
        }
      }
    });
    fmrcTabPane.addComponentListener(new ComponentAdapter() {
      public void componentShown(ComponentEvent e) {
        Component c = fmrcTabPane.getSelectedComponent();
        if (c instanceof JLabel) {
          int idx = fmrcTabPane.getSelectedIndex();
          String title = fmrcTabPane.getTitleAt(idx);
          makeComponent(fmrcTabPane, title);
        }
      }
    });

    // nested tab - ncml
    //ncmlPanel = new NcmlPanel((PreferencesExt) mainPrefs.node("NcML"));
    //ncmlTabPane.addTab("NcML", ncmlPanel);
    ncmlTabPane.addTab("NcmlEditor", new JLabel("NcmlEditor"));
    ncmlTabPane.addTab("Aggregation", new JLabel("Aggregation"));
    ncmlTabPane.addChangeListener(new ChangeListener() {
      public void stateChanged(ChangeEvent e) {
        Component c = ncmlTabPane.getSelectedComponent();
        if (c instanceof JLabel) {
          int idx = ncmlTabPane.getSelectedIndex();
          String title = ncmlTabPane.getTitleAt(idx);
          makeComponent(ncmlTabPane, title);
        }
      }
    });
    ncmlTabPane.addComponentListener(new ComponentAdapter() {
      public void componentShown(ComponentEvent e) {
        Component c = ncmlTabPane.getSelectedComponent();
        if (c instanceof JLabel) {
          int idx = ncmlTabPane.getSelectedIndex();
          String title = ncmlTabPane.getTitleAt(idx);
          makeComponent(ncmlTabPane, title);
        }
      }
    });

    // dynamic proxy for DebugFlags
    debugFlags = (DebugFlags) java.lang.reflect.Proxy.newProxyInstance(DebugFlags.class.getClassLoader(), new Class[]{DebugFlags.class}, new DebugProxyHandler());

    makeMenuBar();
    setDebugFlags();
  }

  // deferred creation of components to minimize startup
  private void makeComponent(JTabbedPane parent, String title) {
    if (parent == null) parent = tabbedPane;

    // find the correct index
    int n = parent.getTabCount();
    int idx;
    for (idx = 0; idx < n; idx++) {
      String cTitle = parent.getTitleAt(idx);
      if (cTitle.equals(title)) break;
    }
    if (idx >= n) {
      if (debugTab) System.out.println("Cant find " + title + " in " + parent);
      return;
    }

    Component c;
    if (title.equals("Aggregation")) {
      aggPanel = new AggPanel((PreferencesExt) mainPrefs.node("NcMLAggregation"));
      c = aggPanel;

    } else if (title.equals("BUFR")) {
      bufrPanel = new BufrPanel((PreferencesExt) mainPrefs.node("bufr"));
      c = bufrPanel;

    } else if (title.equals("BUFRTableB")) {
      bufrTableBPanel = new BufrTableBPanel((PreferencesExt) mainPrefs.node("bufr2"));
      c = bufrTableBPanel;

    } else if (title.equals("BUFRTableD")) {
      bufrTableDPanel = new BufrTableDPanel((PreferencesExt) mainPrefs.node("bufrD"));
      c = bufrTableDPanel;

    } else if (title.equals("GRIB")) {
      gribPanel = new GribPanel((PreferencesExt) mainPrefs.node("grib"));
      c = gribPanel;

    } else if (title.equals("GRIB2")) {
      grib2Panel = new Grib2Panel((PreferencesExt) mainPrefs.node("grib2"));
      c = grib2Panel;

    } else if (title.equals("CoordSys")) {
      coordSysPanel = new CoordSysPanel((PreferencesExt) mainPrefs.node("CoordSys"));
      c = coordSysPanel;

    } else if (title.equals("FmrcImpl")) {
      fmrcImplPanel = new FmrcImplPanel((PreferencesExt) mainPrefs.node("fmrcImpl"));
      c = fmrcImplPanel;

    } else if (title.equals("FeatureScan")) {
      ftPanel = new FeatureScanPanel((PreferencesExt) mainPrefs.node("ftPanel"));
      c = ftPanel;

    } else if (title.equals("GeoTiff")) {
      geotiffPanel = new GeotiffPanel((PreferencesExt) mainPrefs.node("WCS"));
      c = geotiffPanel;

    } else if (title.equals("Grids")) {
      gridPanel = new GeoGridPanel((PreferencesExt) mainPrefs.node("grid"));
      c = gridPanel;

    } else if (title.equals("HDF5")) {
      hdf5Panel = new Hdf5Panel((PreferencesExt) mainPrefs.node("hdf5"));
      c = hdf5Panel;

    } else if (title.equals("HDF4")) {
      hdf4Panel = new Hdf4Panel((PreferencesExt) mainPrefs.node("hdf4"));
      c = hdf4Panel;

    } else if (title.equals("Images")) {
      imagePanel = new ImagePanel((PreferencesExt) mainPrefs.node("images"));
      c = imagePanel;

    } else if (title.equals("Fmrc")) {
      fmrcPanel = new FmrcPanel((PreferencesExt) mainPrefs.node("fmrc2"));
      c = fmrcPanel;

    } else if (title.equals("Collections")) {
      collectionPanel = new CollectionPanel((PreferencesExt) mainPrefs.node("collections"));
      c = collectionPanel;

     } else if (title.equals("Inventory")) {
      fmrcInvPanel = new FmrcInvPanel((PreferencesExt) mainPrefs.node("fmrc"));
      c = fmrcInvPanel;

    } else if (title.equals("NCDump")) {
      ncdumpPanel = new NCdumpPanel((PreferencesExt) mainPrefs.node("NCDump"));
      c = ncdumpPanel;

      //} else if (title.equals("NcML")) {
      // ncmlPanel = new NcmlPanel((PreferencesExt) mainPrefs.node("NcML"));
      // c = ncmlPanel;

    } else if (title.equals("NcmlEditor")) {
      ncmlEditorPanel = new NcmlEditorPanel((PreferencesExt) mainPrefs.node("NcmlEditor"));
      c = ncmlEditorPanel;

    } else if (title.equals("PointObs")) {
      pointObsPanel = new PointObsPanel((PreferencesExt) mainPrefs.node("points"));
      c = pointObsPanel;

    } else if (title.equals("PointFeature")) {
      pointFeaturePanel = new PointFeaturePanel((PreferencesExt) mainPrefs.node("pointFeature"));
      c = pointFeaturePanel;

    } else if (title.equals("Radial")) {
      radialPanel = new RadialPanel((PreferencesExt) mainPrefs.node("radial"));
      c = radialPanel;

    } else if (title.equals("StationObs")) {
      stationObsPanel = new StationObsPanel((PreferencesExt) mainPrefs.node("stations"));
      c = stationObsPanel;

    } else if (title.equals("StationRadial")) {
      stationRadialPanel = new StationRadialPanel((PreferencesExt) mainPrefs.node("stationRadar"));
      c = stationRadialPanel;

    } else if (title.equals("Trajectory")) {
      trajTablePanel = new TrajectoryTablePanel((PreferencesExt) mainPrefs.node("trajectory"));
      c = trajTablePanel;

    } else if (title.equals("THREDDS")) {
      threddsUI = new ThreddsUI(ToolsUI.this.parentFrame, (PreferencesExt) mainPrefs.node("thredds"));
      threddsUI.addPropertyChangeListener(new java.beans.PropertyChangeListener() {
        public void propertyChange(java.beans.PropertyChangeEvent e) {
          if (e.getPropertyName().equals("InvAccess")) {
            thredds.catalog.InvAccess access = (thredds.catalog.InvAccess) e.getNewValue();
            setThreddsDatatype(access);
          }
          if (e.getPropertyName().equals("Dataset") || e.getPropertyName().equals("CoordSys") || e.getPropertyName().equals("File")) {
            thredds.catalog.InvDataset ds = (thredds.catalog.InvDataset) e.getNewValue();
            setThreddsDatatype(ds, e.getPropertyName());
          }
        }
      });

      c = threddsUI;

    } else if (title.equals("Units")) {
      unitsPanel = new UnitsPanel((PreferencesExt) mainPrefs.node("units"));
      c = unitsPanel;

    } else if (title.equals("URLdump")) {
      urlPanel = new URLDumpPane((PreferencesExt) mainPrefs.node("urlDump"));
      c = urlPanel;

    } else if (title.equals("Viewer")) {
      c = viewerPanel;

    } else if (title.equals("WMS")) {
      wmsPanel = new WmsPanel((PreferencesExt) mainPrefs.node("wms"));
      c = wmsPanel;

    } else {
      System.out.println("tabbedPane unknown component " + title);
      return;
    }

    parent.setComponentAt(idx, c);
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

    AbstractAction act = new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        MetadataManager.closeAll(); // shutdown bdb
      }
    };
    BAMutil.setActionProperties(act, null, "Close BDB database", false, 'S', -1);
    BAMutil.addActionToMenu(sysMenu, act);

    AbstractAction clearHttpStateAction = new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        HttpClientManager.clearState();
      }
    };
    BAMutil.setActionProperties(clearHttpStateAction, null, "Clear Http State", false, 'S', -1);
    BAMutil.addActionToMenu(sysMenu, clearHttpStateAction);

     AbstractAction showCacheAction = new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        Formatter f = new Formatter();
        f.format("NetcdfFileCache contents\n");
        ucar.nc2.util.cache.FileCache cache = NetcdfDataset.getNetcdfFileCache();
        if (null != cache)
          cache.showCache(f);
        viewerPanel.detailTA.setText(f.toString());
        viewerPanel.detailWindow.show();
      }
    };
    BAMutil.setActionProperties(showCacheAction, null, "Show Caches", false, 'S', -1);
    BAMutil.addActionToMenu(sysMenu, showCacheAction);

    AbstractAction clearCacheAction = new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        ucar.nc2.util.cache.FileCache cache = NetcdfDataset.getNetcdfFileCache();
        if (cache != null)
          cache.clearCache(true);
      }
    };
    BAMutil.setActionProperties(clearCacheAction, null, "Clear NetcdfDatasetCache", false, 'C', -1);
    BAMutil.addActionToMenu(sysMenu, clearCacheAction);

    AbstractAction enableCache = new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        Boolean state = (Boolean) getValue(BAMutil.STATE);
        if (state == isCacheInit) return;
        isCacheInit = state;
        if (isCacheInit) {
          ucar.nc2.util.cache.FileCache cache = NetcdfDataset.getNetcdfFileCache();
          if (cache != null)
            cache.enable();
          else
            NetcdfDataset.initNetcdfFileCache(10, 20, 10 * 60);
        } else {
          ucar.nc2.util.cache.FileCache cache = NetcdfDataset.getNetcdfFileCache();
          if (cache != null) cache.disable();
        }
      }
    };
    BAMutil.setActionPropertiesToggle(enableCache, null, "enable NetcdfDatasetCache", isCacheInit, 'N', -1);
    BAMutil.addActionToMenu(sysMenu, enableCache);

    AbstractAction showPropertiesAction = new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        viewerPanel.detailTA.setText("System Properties\n");
        Properties sysp = System.getProperties();
        java.util.Enumeration eprops = sysp.propertyNames();
        ArrayList<String> list = Collections.list(eprops);
        Collections.sort(list);

        for (Object aList : list) {
          String name = (String) aList;
          String value = System.getProperty(name);
          viewerPanel.detailTA.appendLine("  " + name + " = " + value);
        }
        viewerPanel.detailWindow.show();
      }
    };
    BAMutil.setActionProperties(showPropertiesAction, null, "System Properties", false, 'P', -1);
    BAMutil.addActionToMenu(sysMenu, showPropertiesAction);

    /* AbstractAction enableDiskCache = new AbstractAction() {
    public void actionPerformed(ActionEvent e) {
      Boolean state = (Boolean) getValue(BAMutil.STATE);
      if (state == isDiskCacheInit) return;
      isDiskCacheInit = state;
      if (isDiskCacheInit) {
        ucar.nc2.util.cache.FileCache cache = NetcdfDataset.getNetcdfFileCache();
        if (cache != null)
          cache.enable();
        else
          NetcdfDataset.initNetcdfFileCache(10,20,10*60);
      } else {
        ucar.nc2.util.cache.FileCache cache = NetcdfDataset.getNetcdfFileCache();
        if (cache != null) cache.disable();
      }
    }
  };
  BAMutil.setActionPropertiesToggle(enableDiskCache, null, "enable Aggregation DiskCache", isDiskCacheInit, 'N', -1);
  BAMutil.addActionToMenu(sysMenu, enableDiskCache); */

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

    // this deletes all the flags, then they start accumulating again
    AbstractAction clearDebugFlagsAction = new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        ucar.util.prefs.ui.Debug.removeAll();
      }
    };
    BAMutil.setActionProperties(clearDebugFlagsAction, null, "Delete All Debug Flags", false, 'C', -1);
    BAMutil.addActionToMenu(debugMenu, clearDebugFlagsAction);

    JMenu helpMenu = new JMenu("Help");
    helpMenu.setMnemonic('H');
    mb.add(helpMenu);

    // "about" this application
    AbstractAction aboutAction = new AbstractAction() {
      public void actionPerformed(ActionEvent evt) {
        if (aboutWindow == null)
          aboutWindow = new AboutWindow();
        aboutWindow.setVisible(true);
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
    ucar.nc2.iosp.hdf5.H5iosp.setDebugFlags(debugFlags);
    ucar.nc2.ncml.NcMLReader.setDebugFlags(debugFlags);
    ucar.nc2.dods.DODSNetcdfFile.setDebugFlags(debugFlags);
    ucar.nc2.iosp.grib.GribGridServiceProvider.setDebugFlags(debugFlags);
    ucar.nc2.thredds.ThreddsDataFactory.setDebugFlags(debugFlags);

    ucar.nc2.FileWriter.setDebugFlags(debugFlags);
    ucar.nc2.ft.point.standard.PointDatasetStandardFactory.setDebugFlags(debugFlags);
  }

  /*public void setDebugOutputStream(boolean b) {
    // System.out.println("setDebugOutputStream "+b);
    if (b) {
      if (debugOS == null) debugOS = new PrintStream(debugPane.getOutputStream());
      NetcdfFile.setDebugOutputStream(debugOS);
    } else {
      NetcdfFile.setDebugOutputStream(System.out);
    }
  } */

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
    BAMutil.setActionPropertiesToggle(a, null, "use missing_value attribute for missing values",
        NetcdfDataset.getMissingDataIsMissing(), 'M', -1);
    BAMutil.addActionToMenu(dsMenu, a);

    ncMenu = new JMenu("FMRC");
    modeMenu.add(ncMenu);

    a = new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        Boolean state = (Boolean) getValue(BAMutil.STATE);
        FeatureCollectionConfig.setRegularizeDefault(state);
      }
    };
    // ToolsUI default is to regularize the FFMRC
    FeatureCollectionConfig.setRegularizeDefault(true);
    a.putValue(BAMutil.STATE, new Boolean(true));
    BAMutil.setActionPropertiesToggle(a, null, "regularize", true, 'R', -1);
    BAMutil.addActionToMenu(ncMenu, a);
  }

  public void save() {
    fileChooser.save();
    if (bufrFileChooser != null) bufrFileChooser.save();

    if (aggPanel != null) aggPanel.save();
    if (bufrPanel != null) bufrPanel.save();
    if (bufrTableBPanel != null) bufrTableBPanel.save();
    if (bufrTableDPanel != null) bufrTableDPanel.save();
    if (coordSysPanel != null) coordSysPanel.save();
    if (ftPanel != null) ftPanel.save();
    if (fmrcPanel != null) fmrcPanel.save();
    if (collectionPanel != null) collectionPanel.save();
    if (fmrcImplPanel != null) fmrcImplPanel.save();
    if (fmrcInvPanel != null) fmrcInvPanel.save();
    if (geotiffPanel != null) geotiffPanel.save();
    if (gribPanel != null) gribPanel.save();
    if (grib2Panel != null) grib2Panel.save();
    if (gridPanel != null) gridPanel.save();
    if (hdf5Panel != null) hdf5Panel.save();
    if (hdf4Panel != null) hdf4Panel.save();
    if (imagePanel != null) imagePanel.save();
    if (ncdumpPanel != null) ncdumpPanel.save();
    //if (ncmlPanel != null) ncmlPanel.save();
    if (ncmlEditorPanel != null) ncmlEditorPanel.save();
    if (pointFeaturePanel != null) pointFeaturePanel.save();
    if (pointObsPanel != null) pointObsPanel.save();
    if (radialPanel != null) radialPanel.save();
    if (stationObsPanel != null) stationObsPanel.save();
    if (stationRadialPanel != null) stationRadialPanel.save();
    if (trajTablePanel != null) trajTablePanel.save();
    if (threddsUI != null) threddsUI.storePersistentData();
    if (unitsPanel != null) unitsPanel.save();
    if (urlPanel != null) urlPanel.save();
    if (viewerPanel != null) viewerPanel.save();
    if (wmsPanel != null) wmsPanel.save();
  }

  //////////////////////////////////////////////////////////////////////////////////

  private void openNetcdfFile(String datasetName) {
    makeComponent(tabbedPane, "Viewer");
    viewerPanel.doit(datasetName);
    tabbedPane.setSelectedComponent(viewerPanel);
  }

  private void openNetcdfFile(NetcdfFile ncfile) {
    makeComponent(tabbedPane, "Viewer");
    viewerPanel.setDataset(ncfile);
    tabbedPane.setSelectedComponent(viewerPanel);
  }


  /* private void showInViewer(NetcdfDataset ds) {
    makeComponent(tabbedPane, "Viewer");
    viewerPanel.setDataset(ds);
    tabbedPane.setSelectedComponent(viewerPanel);
  } */

  private void openCoordSystems(String datasetName) {
    makeComponent(tabbedPane, "CoordSys");
    coordSysPanel.doit(datasetName);
    tabbedPane.setSelectedComponent(coordSysPanel);
  }

  private void openCoordSystems(NetcdfDataset dataset) {
    makeComponent(tabbedPane, "CoordSys");
    coordSysPanel.setDataset(dataset);
    tabbedPane.setSelectedComponent(coordSysPanel);
  }

  private void openNcML(String datasetName) {
    makeComponent(ncmlTabPane, "NcmlEditor");
    ncmlEditorPanel.doit(datasetName);
    tabbedPane.setSelectedComponent(ncmlTabPane);
    ncmlTabPane.setSelectedComponent(ncmlEditorPanel);
  }

  private void openPointFeatureDataset(String datasetName) {
    makeComponent(ftTabPane, "PointFeature");
    pointFeaturePanel.setPointFeatureDataset(null, datasetName);
    tabbedPane.setSelectedComponent(ftTabPane);
    ftTabPane.setSelectedComponent(pointFeaturePanel);
  }

  private void openGridDataset(String datasetName) {
    makeComponent(ftTabPane, "Grids");
    gridPanel.doit(datasetName);
    tabbedPane.setSelectedComponent(ftTabPane);
    ftTabPane.setSelectedComponent(gridPanel);
  }

  private void openGridDataset(NetcdfDataset dataset) {
    makeComponent(ftTabPane, "Grids");
    gridPanel.setDataset(dataset);
    tabbedPane.setSelectedComponent(ftTabPane);
    ftTabPane.setSelectedComponent(gridPanel);
  }

  private void openGridDataset(GridDataset dataset) {
    makeComponent(ftTabPane, "Grids");
    gridPanel.setDataset(dataset);
    tabbedPane.setSelectedComponent(ftTabPane);
    ftTabPane.setSelectedComponent(gridPanel);
  }

  private void openRadialDataset(String datasetName) {
    makeComponent(ftTabPane, "Radial");
    radialPanel.doit(datasetName);
    tabbedPane.setSelectedComponent(ftTabPane);
    ftTabPane.setSelectedComponent(radialPanel);
  }

  private void openWMSDataset(String datasetName) {
    makeComponent(ftTabPane, "WMS");
    wmsPanel.doit(datasetName);
    tabbedPane.setSelectedComponent(ftTabPane);
    ftTabPane.setSelectedComponent(wmsPanel);
  }

  // jump to the appropriate tab based on datatype of InvDataset

  private void setThreddsDatatype(thredds.catalog.InvDataset invDataset, String wants) {
    if (invDataset == null) return;

    boolean wantsViewer = wants.equals("File");
    boolean wantsCoordSys = wants.equals("CoordSys");

    try {
      // just open as a NetcdfDataset
      if (wantsViewer) {
        openNetcdfFile(threddsDataFactory.openDataset(invDataset, true, null, null));
        return;
      }

      if (wantsCoordSys) {
        NetcdfDataset ncd = threddsDataFactory.openDataset(invDataset, true, null, null);
        ncd.enhance(); // make sure its enhanced
        openCoordSystems(ncd);
        return;
      }

      // otherwise do the datatype thing
      ThreddsDataFactory.Result threddsData = threddsDataFactory.openFeatureDataset(invDataset, null);
      if (threddsData == null) {
        JOptionPane.showMessageDialog(null, "Unknown datatype");
        return;
      }
      setThreddsDatatype(threddsData);

    } catch (IOException ioe) {
      JOptionPane.showMessageDialog(null, "Error on setThreddsDatatype = " + ioe.getMessage());
    }

  }

  // jump to the appropriate tab based on datatype of InvAccess
  private void setThreddsDatatype(thredds.catalog.InvAccess invAccess) {
    if (invAccess == null) return;

    thredds.catalog.InvService s = invAccess.getService();
    if (s.getServiceType() == thredds.catalog.ServiceType.HTTPServer) {
      downloadFile(invAccess.getStandardUrlName());
      return;
    }

    if (s.getServiceType() == thredds.catalog.ServiceType.WMS) {
      openWMSDataset(invAccess.getStandardUrlName());
      return;
    }

    thredds.catalog.InvDataset ds = invAccess.getDataset();
    if (ds.getDataType() == null) {
      // if no feature type, just open as a NetcdfDataset
      try {
        openNetcdfFile(threddsDataFactory.openDataset(invAccess, true, null, null));
      } catch (IOException ioe) {
        JOptionPane.showMessageDialog(null, "Error on setThreddsDatatype = " + ioe.getMessage());
      }
      return;
    }

    try {
      ThreddsDataFactory.Result threddsData = threddsDataFactory.openFeatureDataset(invAccess, null);
      setThreddsDatatype(threddsData);

    } catch (IOException ioe) {
      JOptionPane.showMessageDialog(null, "Error on setThreddsDatatype = " + ioe.getMessage());
    }

  }

  /* jump to the appropriate tab based on datatype of InvDataset
  private void setThreddsDatatype(String dataset) {

    try {
      ThreddsDataFactory.Result threddsData = threddsDataFactory.openDatatype(dataset, null);
      setThreddsDatatype(threddsData);

    } catch (IOException ioe) {
      JOptionPane.showMessageDialog(null, "Error on setThreddsDataset = " + ioe.getMessage());
    }

  }  */

  // jump to the appropriate tab based on datatype of threddsData

  private void setThreddsDatatype(ThreddsDataFactory.Result threddsData) {

    if (threddsData.fatalError) {
      JOptionPane.showMessageDialog(this, "Cant open dataset=" + threddsData.errLog);
      return;
    }

    if (threddsData.featureType == FeatureType.GRID) {
      makeComponent(ftTabPane, "Grids");
      gridPanel.setDataset((NetcdfDataset) threddsData.featureDataset.getNetcdfFile());
      tabbedPane.setSelectedComponent(ftTabPane);
      ftTabPane.setSelectedComponent(gridPanel);

    } else if (threddsData.featureType == FeatureType.IMAGE) {
      makeComponent(ftTabPane, "Images");
      imagePanel.setImageLocation(threddsData.imageURL);
      tabbedPane.setSelectedComponent(ftTabPane);
      ftTabPane.setSelectedComponent(imagePanel);

    } else if (threddsData.featureType == FeatureType.RADIAL) {
      makeComponent(ftTabPane, "Radial");
      radialPanel.setDataset((RadialDatasetSweep) threddsData.featureDataset);
      tabbedPane.setSelectedComponent(ftTabPane);
      ftTabPane.setSelectedComponent(radialPanel);

    } else if (threddsData.featureType.isPointFeatureType()) {
      makeComponent(ftTabPane, "PointFeature");
      pointFeaturePanel.setPointFeatureDataset((PointDatasetImpl) threddsData.featureDataset);
      tabbedPane.setSelectedComponent(ftTabPane);
      ftTabPane.setSelectedComponent(pointFeaturePanel);

    } else if (threddsData.featureType == FeatureType.STATION_RADIAL) {
      makeComponent(ftTabPane, "StationRadial");
      stationRadialPanel.setStationRadialDataset((StationRadarCollectionImpl) threddsData.featureDataset);
      tabbedPane.setSelectedComponent(ftTabPane);
      ftTabPane.setSelectedComponent(stationRadialPanel);

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

  // LOOK put in background task ??
  private NetcdfDataset openDataset(String location, boolean addCoords, CancelTask task) {
    try {
      NetcdfDataset ncd = NetcdfDataset.openDataset(location, addCoords, task);

      /* if (addCoords)
        ncd = NetcdfDatasetCache.acquire(location, task);
      else {
         NetcdfFile ncfile = NetcdfFileCache.acquire(location, task);
         if (ncfile != null) ncd = new NetcdfDataset( ncfile, false);
      } */

      if (setUseRecordStructure)
        ncd.sendIospMessage(NetcdfFile.IOSP_MESSAGE_ADD_RECORD_STRUCTURE);

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
        ncfile = NetcdfDataset.acquireDataset(location, task);
      else
        ncfile = NetcdfDataset.acquireFile(location, task);

      if (ncfile == null)
        JOptionPane.showMessageDialog(null, "NetcdfDataset.open cant open " + location);
      else if (setUseRecordStructure)
        ncfile.sendIospMessage(NetcdfFile.IOSP_MESSAGE_ADD_RECORD_STRUCTURE);

    } catch (IOException ioe) {
      String message = ioe.getMessage();
      if ((null == message) && (ioe instanceof EOFException))
        message = "Premature End of File";
      JOptionPane.showMessageDialog(null, "NetcdfDataset.open cant open " + location + "\n" + message);

      try {
        if (ncfile != null) ncfile.close();
      } catch (IOException ee) {
      }
      ncfile = null;

    } catch (Exception e) {
      JOptionPane.showMessageDialog(null, "NetcdfDataset.open cant open " + location + "\n" + e.getMessage());
      log.error("NetcdfDataset.open cant open " + location, e);

      try {
        if (ncfile != null) ncfile.close();
      } catch (IOException ee) {
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
          IO.copyUrlB(values[1], out, 60000);
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
          if (debugCB)
            System.out.println(" doit " + cb.getSelectedItem() + " cmd=" + e.getActionCommand() + " when=" + e.getWhen() + " class=" + OpPanel.this.getClass().getName());

          // eliminate multiple events from same selection
          if (eventOK && (e.getWhen() > lastEvent + 10000)) { // not sure of units - must be nanosecs - ?? platform dependednt ??
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
      if (coordButt != null) prefs.putBoolean("coordState", coordButt.getModel().isSelected());
      if (detailWindow != null) prefs.putBeanObject(FRAME_SIZE, detailWindow.getBounds());
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

        StringWriter writer = new StringWriter(50000);
        NCdumpW.print(ncfile, command, writer, task);
        result = writer.toString();

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
          StringWriter writer = new StringWriter(50000);
          NCdumpW.print(ncfile, command, writer, task);
          result = writer.toString();
        }
      };
      task = new GetDataTask(runner, filename, null);
      stopButton.startProgressMonitorTask(task);
    }
  }

  private class UnitsPanel extends JPanel {
    PreferencesExt prefs;
    JSplitPane split, split2;
    UnitDatasetCheck unitDataset;
    UnitConvert unitConvert;
    DateFormatMark dateFormatMark;

    UnitsPanel(PreferencesExt prefs) {
      super();
      this.prefs = prefs;
      unitDataset = new UnitDatasetCheck((PreferencesExt) prefs.node("unitDataset"));
      unitConvert = new UnitConvert((PreferencesExt) prefs.node("unitConvert"));
      dateFormatMark = new DateFormatMark((PreferencesExt) prefs.node("dateFormatMark"));

      split2 = new JSplitPane(JSplitPane.VERTICAL_SPLIT, unitConvert, dateFormatMark);
      split2.setDividerLocation(prefs.getInt("splitPos2", 500));

      split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, new JScrollPane(unitDataset), split2);
      split.setDividerLocation(prefs.getInt("splitPos", 500));

      setLayout(new BorderLayout());
      add(split, BorderLayout.CENTER);
    }

    void save() {
      prefs.putInt("splitPos", split.getDividerLocation());
      prefs.putInt("splitPos2", split2.getDividerLocation());
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
        Iterator iter = ncfile.getVariables().iterator();
        while (iter.hasNext()) {
          VariableEnhanced vs = (VariableEnhanced) iter.next();
          String units = vs.getUnitsString();
          StringBuilder sb = new StringBuilder();
          sb.append("   ").append(vs.getName()).append(" has unit= <").append(units).append(">");
          if (units != null) {
            try {
              SimpleUnit su = SimpleUnit.factoryWithExceptions(units);
              sb.append(" unit convert = ").append(su.toString());
              if (su.isUnknownUnit())
                sb.append(" UNKNOWN UNIT");

            } catch (Exception ioe) {
              sb.append(" unit convert failed ");
              sb.insert(0, "**** Fail ");
            }
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
        ta.appendLine("getCanonicalString()=" + su.getCanonicalString());
        ta.appendLine("class = " + su.getImplementingClass());
        if (su.isUnknownUnit())
          ta.appendLine("UNKNOWN UNIT");

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
      List<String> list = new ArrayList<String>();
      while (stoke.hasMoreTokens())
        list.add(stoke.nextToken());

      try {
        String unitS1 = list.get(0);
        String unitS2 = list.get(1);
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

      boolean isDate = false;
      try {
        DateUnit du = new DateUnit(command);
        ta.appendLine("\n<" + command + "> isDateUnit = " + du);
        Date d = du.getDate();
        ta.appendLine("getStandardDateString = " + formatter.toDateTimeString(d));
        ta.appendLine("getDateOrigin = " + formatter.toDateTimeString(du.getDateOrigin()));
        isDate = true;
      } catch (Exception e) {
        // ok to fall through
      }

      if (!isDate) {
        try {
          SimpleUnit su = SimpleUnit.factory(command);
          boolean isTime = su instanceof TimeUnit;
          ta.setText("<" + command + "> isDateUnit= " + isDate + " isTimeUnit= " + isTime);
          if (isTime) {
            TimeUnit du = (TimeUnit) su;
            ta.appendLine("\nTimeUnit = " + du);
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
      }

      Date d = DateUnit.getStandardOrISO(command);
      if (d == null)
        ta.appendLine("\nDateUnit.getStandardOrISO = false");
      else
        ta.appendLine("\nDateUnit.getStandardOrISO = " + formatter.toDateTimeString(d));

    }

  }

  private class DateFormatMark extends OpPanel {
    ComboBox testCB;
    DateFormatter dateFormatter = new DateFormatter();

    DateFormatMark(PreferencesExt prefs) {
      super(prefs, "dateFormatMark:", false, false);

      testCB = new ComboBox(prefs);
      buttPanel.add(testCB);

      JButton compareButton = new JButton("Apply");
      compareButton.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          apply(cb.getSelectedItem(), testCB.getSelectedItem());
        }
      });
      buttPanel.add(compareButton);
    }

    boolean process(Object o) {
      return false;
    }

    void apply(Object mark, Object testo) {
      String dateFormatMark = (String) mark;
      String filename = (String) testo;
      try {
        Date coordValueDate = DateFromString.getDateUsingDemarkatedCount(filename, dateFormatMark, '#');
        String coordValue = dateFormatter.toDateTimeStringISO(coordValueDate);
        ta.setText("got date= " + coordValue);

      } catch (Exception e) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream(10000);
        e.printStackTrace(new PrintStream(bos));
        ta.setText(bos.toString());
      }
    }
  }


  /////////////////////////////////////////////////////////////////////
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

      defComboBox = new JComboBox(FmrcDefinition.getDefinitionFiles());
      defWindow = new IndependentWindow("GRIB Definition File", null, defComboBox);
      defWindow.setLocationRelativeTo(defButt);

      AbstractButton infoButton = BAMutil.makeButtcon("Information", "Parse Info", false);
      infoButton.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          if (ds != null) {
            NetcdfDatasetInfo info = null;
            try {
              info = new NetcdfDatasetInfo(ds.getLocation());
              detailTA.setText(info.writeXML());
              detailTA.appendLine("----------------------");
              detailTA.appendLine(info.getParseInfo());
              detailTA.gotoTop();

            } catch (IOException e1) {
              ByteArrayOutputStream out = new ByteArrayOutputStream();
              e1.printStackTrace(new PrintStream(out));
              detailTA.setText(out.toString());

            } finally {
              if (info != null) try {
                info.close();
              } catch (IOException ee) {
              } // do nothing
            }
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

      // close previous file
      try {
        if (ds != null) ds.close();
      }
      catch (IOException ioe) {
      }

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
      }

      return !err;
    }

    void setDataset(NetcdfDataset ncd) {
      try {
        if (ds != null) ds.close();
        ds = null;
      } catch (IOException ioe) {
      }
      ds = ncd;

      coordSysTable.setDataset(ds);
      setSelectedItem(ds.getLocation());
    }


    void save() {
      coordSysTable.save();
      super.save();
    }

  }

  /////////////////////////////////////////////////////////////////////
  private class AggPanel extends OpPanel {
    AggTable aggTable;
    NetcdfDataset ncd;

    boolean useDefinition = false;
    JComboBox defComboBox;
    IndependentWindow defWindow;
    AbstractButton defButt;

    AggPanel(PreferencesExt p) {
      super(p, "file:", true, false);
      aggTable = new AggTable(prefs, buttPanel);
      aggTable.addPropertyChangeListener(new java.beans.PropertyChangeListener() {
        public void propertyChange(java.beans.PropertyChangeEvent e) {

          if (e.getPropertyName().equals("openNetcdfFile")) {
            NetcdfFile ncfile = (NetcdfFile) e.getNewValue();
            if (ncfile != null) openNetcdfFile(ncfile);

          } else if (e.getPropertyName().equals("openCoordSystems")) {
            NetcdfFile ncfile = (NetcdfFile) e.getNewValue();
            if (ncfile == null) return;
            try {
              NetcdfDataset ncd = NetcdfDataset.wrap(ncfile, NetcdfDataset.getDefaultEnhanceMode());
              openCoordSystems(ncd);
            } catch (IOException e1) {
              e1.printStackTrace();
            }

          } else if (e.getPropertyName().equals("openGridDataset")) {
            NetcdfFile ncfile = (NetcdfFile) e.getNewValue();
            if (ncfile == null) return;
            try {
              NetcdfDataset ncd = NetcdfDataset.wrap(ncfile, NetcdfDataset.getDefaultEnhanceMode());
              openGridDataset(ncd);
            } catch (IOException e1) {
              e1.printStackTrace();
            }
          }
        }
      });

      add(aggTable, BorderLayout.CENTER);
    }

    boolean process(Object o) {
      String command = (String) o;
      boolean err = false;

      try {
        if (ncd != null) {
          try {
            ncd.close();
          } catch (IOException ioe) {
            ioe.printStackTrace();
          }
        }

        ncd = (NetcdfDataset) NetcdfDataset.openFile(command, null);
        aggTable.setAggDataset(ncd);

      } catch (FileNotFoundException ioe) {
        JOptionPane.showMessageDialog(null, "NetcdfDataset cant open " + command + "\n" + ioe.getMessage());
        ta.setText("Failed to open <" + command + ">\n" + ioe.getMessage());
        err = true;

      } catch (Throwable e) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream(5000);
        e.printStackTrace(new PrintStream(bos));
        detailTA.setText(bos.toString());
        detailTA.gotoTop();
        detailWindow.show();
        err = true;
      }

      return !err;
    }

    void save() {
      aggTable.save();
      super.save();
    }

  }


  /////////////////////////////////////////////////////////////////////
  private class BufrPanel extends OpPanel {
    ucar.unidata.io.RandomAccessFile raf = null;
    BufrMessageViewer bufrTable;

    boolean useDefinition = false;
    JComboBox defComboBox;
    IndependentWindow defWindow;
    AbstractButton defButt;

    BufrPanel(PreferencesExt p) {
      super(p, "file:", true, false);
      bufrTable = new BufrMessageViewer(prefs, buttPanel);
      add(bufrTable, BorderLayout.CENTER);
    }

    boolean process(Object o) {
      String command = (String) o;
      boolean err = false;

      ByteArrayOutputStream bos = new ByteArrayOutputStream(10000);
      try {
        if (raf != null)
          raf.close();
        raf = new ucar.unidata.io.RandomAccessFile(command, "r");

        bufrTable.setBufrFile(raf);

      } catch (FileNotFoundException ioe) {
        JOptionPane.showMessageDialog(null, "NetcdfDataset cant open " + command + "\n" + ioe.getMessage());
        ta.setText("Failed to open <" + command + ">\n" + ioe.getMessage());
        err = true;

      } catch (Exception e) {
        e.printStackTrace();
        e.printStackTrace(new PrintStream(bos));
        ta.setText(bos.toString());
        err = true;
      }

      return !err;
    }

    void save() {
      bufrTable.save();
      super.save();
    }

  }

  /////////////////////////////////////////////////////////////////////
  private FileManager bufrFileChooser = null;

  private void initBufrFileChooser() {
    bufrFileChooser = new FileManager(parentFrame, null, null, (PreferencesExt) prefs.node("bufrFileManager"));
  }

  private class BufrTableBPanel extends OpPanel {
    BufrTableBViewer bufrTable;

    boolean useDefinition = false;
    JComboBox defComboBox;
    IndependentWindow defWindow;
    AbstractButton defButt;
    JComboBox modes;

    BufrTableBPanel(PreferencesExt p) {
      super(p, "tableB:", false, false);

      AbstractAction fileAction = new AbstractAction() {
        public void actionPerformed(ActionEvent e) {
          if (bufrFileChooser == null) initBufrFileChooser();
          String filename = bufrFileChooser.chooseFilename();
          if (filename == null) return;
          cb.setSelectedItem(filename);
        }
      };
      BAMutil.setActionProperties(fileAction, "FileChooser", "open Local table...", false, 'L', -1);
      BAMutil.addActionToContainer(buttPanel, fileAction);

      modes = new JComboBox(new String[]{"mel-bufr", "ncep", "ncep-nm", "ecmwf", "csv", "ukmet", "mel-tabs", "wmo-xml"});
      buttPanel.add(modes);

      JButton accept = new JButton("Accept");
      buttPanel.add(accept);
      accept.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          accept();
        }
      });

      bufrTable = new BufrTableBViewer(prefs, buttPanel);
      add(bufrTable, BorderLayout.CENTER);
    }

    boolean process(Object command) {
      return true;
    }

    void accept() {
      String command = (String) cb.getSelectedItem();

      ByteArrayOutputStream bos = new ByteArrayOutputStream(10000);
      try {
        String mode = (String) modes.getSelectedItem();
        bufrTable.setBufrTableB(command, mode);

      } catch (FileNotFoundException ioe) {
        JOptionPane.showMessageDialog(null, "BufrTableViewer cant open " + command + "\n" + ioe.getMessage());
        detailTA.setText("Failed to open <" + command + ">\n" + ioe.getMessage());
        detailTA.setVisible(true);

      } catch (Exception e) {
        e.printStackTrace();
        e.printStackTrace(new PrintStream(bos));
        detailTA.setText(bos.toString());
        detailTA.setVisible(true);
      }

    }

    void save() {
      bufrTable.save();
      super.save();
    }

  }

  /////////////////////////////////////////////////////////////////////
  private class BufrTableDPanel extends OpPanel {
    BufrTableDViewer bufrTable;

    boolean useDefinition = false;
    JComboBox defComboBox;
    IndependentWindow defWindow;
    AbstractButton defButt;
    JComboBox modes;

    BufrTableDPanel(PreferencesExt p) {
      super(p, "tableD:", false, false);

      AbstractAction fileAction = new AbstractAction() {
        public void actionPerformed(ActionEvent e) {
          if (bufrFileChooser == null) initBufrFileChooser();
          String filename = bufrFileChooser.chooseFilename();
          if (filename == null) return;
          cb.setSelectedItem(filename);
        }
      };
      BAMutil.setActionProperties(fileAction, "FileChooser", "open Local table...", false, 'L', -1);
      BAMutil.addActionToContainer(buttPanel, fileAction);

      modes = new JComboBox(new String[]{"mel-bufr", "ncep", "ncep-nm", "ecmwf", "csv", "ukmet", "wmo-xml"});
      buttPanel.add(modes);

      JButton accept = new JButton("Accept");
      buttPanel.add(accept);
      accept.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          accept();
        }
      });

      bufrTable = new BufrTableDViewer(prefs, buttPanel);
      add(bufrTable, BorderLayout.CENTER);
    }

    boolean process(Object command) {
      return true;
    }

    void accept() {
      String command = (String) cb.getSelectedItem();
      boolean err = false;

      ByteArrayOutputStream bos = new ByteArrayOutputStream(10000);
      try {
        String mode = (String) modes.getSelectedItem();
        bufrTable.setBufrTableD(command, mode);

      } catch (FileNotFoundException ioe) {
        JOptionPane.showMessageDialog(null, "BufrTableViewer cant open " + command + "\n" + ioe.getMessage());
        detailTA.setText("Failed to open <" + command + ">\n" + ioe.getMessage());
        detailTA.setVisible(true);
        err = true;

      } catch (Exception e) {
        e.printStackTrace();
        e.printStackTrace(new PrintStream(bos));
        detailTA.setText(bos.toString());
        detailTA.setVisible(true);
        err = true;
      }

    }

    void save() {
      bufrTable.save();
      super.save();
    }

  }

  /////////////////////////////////////////////////////////////////////
  private class GribPanel extends OpPanel {
    ucar.unidata.io.RandomAccessFile raf = null;
    GribTable gribTable;

    boolean useDefinition = false;
    JComboBox defComboBox;
    IndependentWindow defWindow;
    AbstractButton defButt;

    GribPanel(PreferencesExt p) {
      super(p, "file:", true, false);
      gribTable = new GribTable(prefs);
      add(gribTable, BorderLayout.CENTER);

      AbstractButton grib2dump = BAMutil.makeButtcon("alien", "Grib2 dump", false);
      grib2dump.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          Grib2Dump dump = new Grib2Dump();
          String fileIn = (String) cb.getSelectedItem();
          try {
            File fileOut = File.createTempFile("temp", "txt");
            dump.gribDump(new String[]{fileIn, fileOut.getPath(), "false"});
            detailTA.setText(IO.readFile(fileOut.getPath()));
            detailWindow.show();
            fileOut.delete();
          } catch (IOException ioe) {
            ByteArrayOutputStream bos = new ByteArrayOutputStream(10000);
            ioe.printStackTrace(new PrintStream(bos));
            ta.setText(bos.toString());
          }
        }
      });
      buttPanel.add(grib2dump);

      AbstractButton infoButton = BAMutil.makeButtcon("Information", "Detail Info", false);
      infoButton.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
            Formatter f = new Formatter();
            gribTable.showInfo(f);
            detailTA.setText(f.toString());
            detailTA.gotoTop();
            detailWindow.show();
        }
      });
      buttPanel.add(infoButton);

    }

    boolean process(Object o) {
      String command = (String) o;
      boolean err = false;

      ByteArrayOutputStream bos = new ByteArrayOutputStream(10000);
      try {
        if (raf != null)
          raf.close();
        raf = new ucar.unidata.io.RandomAccessFile(command, "r");

        gribTable.setGribFile(raf);

      } catch (FileNotFoundException ioe) {
        JOptionPane.showMessageDialog(null, "NetcdfDataset cant open " + command + "\n" + ioe.getMessage());
        ta.setText("Failed to open <" + command + ">\n" + ioe.getMessage());
        err = true;

      } catch (Exception e) {
        e.printStackTrace();
        e.printStackTrace(new PrintStream(bos));
        detailTA.setText(bos.toString());
        detailWindow.show();
        err = true;
      }

      return !err;
    }

    void save() {
      gribTable.save();
      super.save();
    }

  }

 /////////////////////////////////////////////////////////////////////

  private class Grib2Panel extends OpPanel {
    ucar.unidata.io.RandomAccessFile raf = null;
    Grib2Table gribTable;
    boolean useIndex = true;

    Grib2Panel(PreferencesExt p) {
      super(p, "file:", true, false);
      gribTable = new Grib2Table(prefs);
      add(gribTable, BorderLayout.CENTER);

      AbstractAction indexAction = new AbstractAction() {
        public void actionPerformed(ActionEvent e) {
          Boolean state = (Boolean) getValue(BAMutil.STATE);
          useIndex = state.booleanValue();
        }
      };
      useIndex = prefs.getBoolean("useIndex", true);
      BAMutil.setActionProperties(indexAction, "addCoords", "use index", true, 'C', -1);
      indexAction.putValue(BAMutil.STATE, new Boolean(useIndex));
      BAMutil.addActionToContainer(buttPanel, indexAction);
    }

    boolean process(Object o) {
      String command = (String) o;
      boolean err = false;

      ByteArrayOutputStream bos = new ByteArrayOutputStream(10000);
      try {
        if (raf != null)
          raf.close();

        if (useIndex) {
          gribTable.setGribFile(command);

        } else {
          raf = new ucar.unidata.io.RandomAccessFile(command, "r");
          gribTable.setGribFile(raf);
        }

      } catch (FileNotFoundException ioe) {
        JOptionPane.showMessageDialog(null, "Grib2Table cant open " + command + "\n" + ioe.getMessage());
        ta.setText("Failed to open <" + command + ">\n" + ioe.getMessage());
        err = true;

      } catch (Exception e) {
        e.printStackTrace();
        e.printStackTrace(new PrintStream(bos));
        detailTA.setText(bos.toString());
        detailWindow.show();
        err = true;
      }

      return !err;
    }

    void save() {
      gribTable.save();
      prefs.putBoolean("useIndex", useIndex);
      super.save();
    }

  }

  /////////////////////////////////////////////////////////////////////
  /* private class Grib2Panel extends OpPanel {
    ucar.unidata.io.RandomAccessFile raf = null;
    Grib2Table gribTable;

    Grib2Panel(PreferencesExt p) {
      super(p, "file:", true, false);
      gribTable = new Grib2Table(prefs);
      add(gribTable, BorderLayout.CENTER);
    }

    boolean process(Object o) {
      String command = (String) o;
      boolean err = false;

      ByteArrayOutputStream bos = new ByteArrayOutputStream(10000);
      try {
        if (raf != null)
          raf.close();
        raf = new ucar.unidata.io.RandomAccessFile(command, "r");

        gribTable. setGribFile(raf);

      } catch (FileNotFoundException ioe) {
        JOptionPane.showMessageDialog(null, "Grib2Table cant open " + command + "\n" + ioe.getMessage());
        ta.setText("Failed to open <" + command + ">\n" + ioe.getMessage());
        err = true;

      } catch (Exception e) {
        e.printStackTrace();
        e.printStackTrace(new PrintStream(bos));
        detailTA.setText(bos.toString());
        detailWindow.show();
        err = true;
      }

      return !err;
    }

    void save() {
      gribTable.save();
      super.save();
    }

  }   */

  /////////////////////////////////////////////////////////////////////
  private class Hdf5Panel extends OpPanel {
    ucar.unidata.io.RandomAccessFile raf = null;
    Hdf5Table hdf5Table;

    Hdf5Panel(PreferencesExt p) {
      super(p, "file:", true, false);
      hdf5Table = new Hdf5Table(prefs);
      add(hdf5Table, BorderLayout.CENTER);

      AbstractButton infoButton = BAMutil.makeButtcon("Information", "Detail Info", false);
      infoButton.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          Formatter f = new Formatter();
          try {
            hdf5Table.showInfo(f);
          } catch (IOException ioe) {
            ByteArrayOutputStream bos = new ByteArrayOutputStream(10000);
            ioe.printStackTrace();
            ioe.printStackTrace(new PrintStream(bos));
            // ta.setText( datasetFactory.getErrorMessages());
            ta.appendLine(bos.toString());
          }
          detailTA.setText(f.toString());
          detailTA.gotoTop();
          detailWindow.show();
        }
      });
      buttPanel.add(infoButton);
    }

    boolean process(Object o) {
      String command = (String) o;
      boolean err = false;

      ByteArrayOutputStream bos = new ByteArrayOutputStream(10000);
      try {
        if (raf != null)
          raf.close();
        raf = new ucar.unidata.io.RandomAccessFile(command, "r");

        hdf5Table.setHdf5File(raf);

      } catch (FileNotFoundException ioe) {
        JOptionPane.showMessageDialog(null, "NetcdfDataset cant open " + command + "\n" + ioe.getMessage());
        ta.setText("Failed to open <" + command + ">\n" + ioe.getMessage());
        err = true;

      } catch (Exception e) {
        e.printStackTrace();
        e.printStackTrace(new PrintStream(bos));
        ta.setText(bos.toString());
        err = true;
      }

      return !err;
    }

    void save() {
      hdf5Table.save();
      super.save();
    }

  }

  /////////////////////////////////////////////////////////////////////
  private class Hdf4Panel extends OpPanel {
    ucar.unidata.io.RandomAccessFile raf = null;
    Hdf4Table hdf4Table;

    Hdf4Panel(PreferencesExt p) {
      super(p, "file:", true, false);
      hdf4Table = new Hdf4Table(prefs);
      add(hdf4Table, BorderLayout.CENTER);

      AbstractButton eosdump = BAMutil.makeButtcon("alien", "Show EOS processing", false);
      eosdump.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          try {
            Formatter f = new Formatter();
            hdf4Table.getEosInfo(f);
            detailTA.setText(f.toString());
            detailWindow.show();
          } catch (IOException ioe) {
            ByteArrayOutputStream bos = new ByteArrayOutputStream(10000);
            ioe.printStackTrace(new PrintStream(bos));
            ta.setText(bos.toString());
          }
        }
      });
      buttPanel.add(eosdump);
    }

    boolean process(Object o) {
      String command = (String) o;
      boolean err = false;

      ByteArrayOutputStream bos = new ByteArrayOutputStream(10000);
      try {
        if (raf != null)
          raf.close();
        raf = new ucar.unidata.io.RandomAccessFile(command, "r");

        hdf4Table.setHdf4File(raf);

      } catch (FileNotFoundException ioe) {
        JOptionPane.showMessageDialog(null, "NetcdfDataset cant open " + command + "\n" + ioe.getMessage());
        ta.setText("Failed to open <" + command + ">\n" + ioe.getMessage());
        err = true;

      } catch (Exception e) {
        e.printStackTrace();
        e.printStackTrace(new PrintStream(bos));
        ta.setText(bos.toString());
        err = true;
      }

      return !err;
    }

    void save() {
      hdf4Table.save();
      super.save();
    }

  }

  /////////////////////////////////////////////////////////////////////

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
          String location = (ds == null) ? ncmlLocation : ds.getLocation();
          if (location == null) location = "test";
          int pos = location.lastIndexOf(".");
          if (pos > 0)
            location = location.substring(0, pos);
          String filename = fileChooser.chooseFilenameToSave(location + ".ncml");
          if (filename == null) return;
          doSave(ta.getText(), filename);
        }
      };
      BAMutil.setActionProperties(saveAction, "Save", "Save NcML", false, 'S', -1);
      BAMutil.addActionToContainer(buttPanel, saveAction);

      AbstractAction netcdfAction = new AbstractAction() {
        public void actionPerformed(ActionEvent e) {
          String location = (ds == null) ? ncmlLocation : ds.getLocation();
          if (location == null) location = "test";
          int pos = location.lastIndexOf(".");
          if (pos > 0)
            location = location.substring(0, pos);

          String filename = fileChooser.chooseFilenameToSave(location + ".nc");
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
      BAMutil.setActionProperties(transAction, "Import", "read textArea through NcMLReader\n write NcML back out via resulting dataset", false, 'T', -1);
      BAMutil.addActionToContainer(buttPanel, transAction);
    }

    boolean process(Object o) {
      ncmlLocation = (String) o;
      if (ncmlLocation.endsWith(".xml") || ncmlLocation.endsWith(".ncml")) {
        if (!ncmlLocation.startsWith("http:") && !ncmlLocation.startsWith("file:"))
          ncmlLocation = "file:" + ncmlLocation;
        String text = IO.readURLcontents(ncmlLocation);

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
        String result;
        ds = openDataset(location, addCoords, null);
        if (ds == null) {
          ta.setText("Failed to open <" + location + ">");
        } else {
          if (useG) {
            boolean showCoords = Debug.isSet("NcML/ncmlG-showCoords");
            ds.writeNcMLG(bos, showCoords, null);
            result = bos.toString();
          } else {
            result = new NcMLWriter().writeXML(ds);
          }
          ta.setText(result);
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
      } catch (Exception ioe) {
        JOptionPane.showMessageDialog(this, "ERROR: " + ioe.getMessage());
        ioe.printStackTrace();
      }
    }

    // read text from textArea through NcMLReader
    // then write it back out via resulting dataset
    void doTransform(String text) {
      try {
        StringReader reader = new StringReader(text);
        NetcdfDataset ncd = NcMLReader.readNcML(reader, null);
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
        IO.writeToFile(text, new File(filename));
        JOptionPane.showMessageDialog(this, "File successfully written");
      } catch (IOException ioe) {
        JOptionPane.showMessageDialog(this, "ERROR: " + ioe.getMessage());
        ioe.printStackTrace();
      }
      // saveNcmlDialog.setVisible(false);
    }

    void save() {
      super.save();
    }

  }

  /////////////////////////////////////////////////////////////////////

  private class NcmlEditorPanel extends OpPanel {
    NetcdfDataset ds = null;
    String ncmlLocation = null;
    JEditorPane editor;
    Map<String, String> protoMap = new HashMap<String, String>(10);
    ComboBox protoChooser;

    NcmlEditorPanel(PreferencesExt p) {
      super(p, "dataset:");
      protoChooser = new ComboBox((PreferencesExt) prefs.node("protoChooser"));
      addProtoChoices();
      buttPanel.add(protoChooser);
      protoChooser.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          String ptype = (String) protoChooser.getSelectedItem();
          String proto = protoMap.get(ptype);
          if (proto != null) {
            editor.setText(proto);
          }
        }
      });

      editor = new JEditorPane();

      // Instantiate a XMLEditorKit with wrapping enabled.
      XMLEditorKit kit = new XMLEditorKit(false);

      // Set the wrapping style.
      kit.setWrapStyleWord(true);

      editor.setEditorKit(kit);

      // Set the font style.
      editor.setFont(new Font("Courier", Font.PLAIN, 12));

      // Set the tab size
      editor.getDocument().putProperty(PlainDocument.tabSizeAttribute, 2);

      // Enable auto indentation.
      editor.getDocument().putProperty(XMLDocument.AUTO_INDENTATION_ATTRIBUTE, true);

      // Enable tag completion.
      editor.getDocument().putProperty(XMLDocument.TAG_COMPLETION_ATTRIBUTE, true);

      // Initialise the folding
      kit.setFolding(true);

      // Set a style
      kit.setStyle(XMLStyleConstants.ATTRIBUTE_NAME, Color.RED, Font.BOLD);

      // Put the editor in a panel that will force it to resize, when a different view is choosen.
      ScrollableEditorPanel editorPanel = new ScrollableEditorPanel(editor);

      JScrollPane scroller = new JScrollPane(editorPanel);

      // Add the number margin as a Row Header View
      scroller.setRowHeaderView(new LineNumberMargin(editor));

      AbstractAction wrapAction = new AbstractAction() {
        public void actionPerformed(ActionEvent e) {
          XMLEditorKit kit = (XMLEditorKit) editor.getEditorKit();
          kit.setLineWrappingEnabled(!kit.isLineWrapping());

          // Update the UI and create a new view...
          editor.updateUI();
        }
      };
      BAMutil.setActionProperties(wrapAction, "Wrap", "Toggle Wrapping", false, 'W', -1);
      BAMutil.addActionToContainer(buttPanel, wrapAction);

      AbstractAction saveAction = new AbstractAction() {
        public void actionPerformed(ActionEvent e) {
          String location = (ds == null) ? ncmlLocation : ds.getLocation();
          if (location == null) location = "test";
          int pos = location.lastIndexOf(".");
          if (pos > 0)
            location = location.substring(0, pos);
          String filename = fileChooser.chooseFilenameToSave(location + ".ncml");
          if (filename == null) return;
          doSave(editor.getText(), filename);
        }
      };
      BAMutil.setActionProperties(saveAction, "Save", "Save NcML", false, 'S', -1);
      BAMutil.addActionToContainer(buttPanel, saveAction);

      AbstractAction netcdfAction = new AbstractAction() {
        public void actionPerformed(ActionEvent e) {
          String location = (ds == null) ? ncmlLocation : ds.getLocation();
          if (location == null) location = "test";
          int pos = location.lastIndexOf(".");
          if (pos > 0)
            location = location.substring(0, pos);

          String filename = fileChooser.chooseFilenameToSave(location + ".nc");
          if (filename == null) return;
          doWriteNetCDF(editor.getText(), filename);
        }
      };
      BAMutil.setActionProperties(netcdfAction, "netcdf", "Write netCDF", false, 'N', -1);
      BAMutil.addActionToContainer(buttPanel, netcdfAction);

      AbstractAction transAction = new AbstractAction() {
        public void actionPerformed(ActionEvent e) {
          doTransform(editor.getText());
        }
      };
      BAMutil.setActionProperties(transAction, "Import", "read textArea through NcMLReader\n write NcML back out via resulting dataset", false, 'T', -1);
      BAMutil.addActionToContainer(buttPanel, transAction);

      AbstractButton compareButton = BAMutil.makeButtcon("Select", "Check NcML", false);
      compareButton.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          Formatter f = new Formatter();
          checkNcml(f);

          detailTA.setText(f.toString());
          detailTA.gotoTop();
          detailWindow.show();
        }
      });
      buttPanel.add(compareButton);

      add(scroller, BorderLayout.CENTER);
    }

    boolean process(Object o) {
      ncmlLocation = (String) o;
      if (ncmlLocation.endsWith(".xml") || ncmlLocation.endsWith(".ncml")) {
        if (!ncmlLocation.startsWith("http:") && !ncmlLocation.startsWith("file:"))
          ncmlLocation = "file:" + ncmlLocation;
        String text = IO.readURLcontents(ncmlLocation);

        editor.setText(text);
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
        String result;
        ds = openDataset(location, addCoords, null);
        if (ds == null) {
          editor.setText("Failed to open <" + location + ">");
        } else {
          result = new NcMLWriter().writeXML(ds);
          editor.setText(result);
          editor.setCaretPosition(0);
        }

      } catch (FileNotFoundException ioe) {
        editor.setText("Failed to open <" + location + ">");
        err = true;

      } catch (Exception e) {
        e.printStackTrace();
        e.printStackTrace(new PrintStream(bos));
        editor.setText(bos.toString());
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
      } catch (Exception ioe) {
        JOptionPane.showMessageDialog(this, "ERROR: " + ioe.getMessage());
        ioe.printStackTrace();
      }
    }

    // read text from textArea through NcMLReader
    // then write it back out via resulting dataset
    void doTransform(String text) {
      try {
        StringReader reader = new StringReader(text);
        NetcdfDataset ncd = NcMLReader.readNcML(reader, null);
        ByteArrayOutputStream bos = new ByteArrayOutputStream(10000);
        ncd.writeNcML(bos, null);
        editor.setText(bos.toString());
        editor.setCaretPosition(0);
        JOptionPane.showMessageDialog(this, "File successfully transformed");

      } catch (IOException ioe) {
        JOptionPane.showMessageDialog(this, "ERROR: " + ioe.getMessage());
        ioe.printStackTrace();
      }
    }

    // read text from textArea through NcMLReader
    // then write it back out via resulting dataset
    private void checkNcml(Formatter f) {
      if (ncmlLocation == null) return;
      try {
        NetcdfDataset ncd = NetcdfDataset.openDataset(ncmlLocation);
        ncd.check(f);

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
        IO.writeToFile(text, new File(filename));
        JOptionPane.showMessageDialog(this, "File successfully written");
      } catch (IOException ioe) {
        JOptionPane.showMessageDialog(this, "ERROR: " + ioe.getMessage());
        ioe.printStackTrace();
      }
      // saveNcmlDialog.setVisible(false);
    }

    void addProtoChoices() {
      String xml =
          "<?xml version='1.0' encoding='UTF-8'?>\n" +
              "<netcdf xmlns='http://www.unidata.ucar.edu/namespaces/netcdf/ncml-2.2'>\n" +
              "  <variable name='time' type='int' shape='time'>\n" +
              "    <attribute name='long_name' type='string' value='time coordinate' />\n" +
              "    <attribute name='units' type='string' value='days since 2001-8-31 00:00:00 UTC' />\n" +
              "    <values start='0' increment='10' />\n" +
              "  </variable>\n" +
              "  <aggregation dimName='time' type='joinNew'>\n" +
              "    <variableAgg name='T'/>\n" +
              "    <scan location='src/test/data/ncml/nc/' suffix='.nc' subdirs='false'/>\n" +
              "  </aggregation>\n" +
              "</netcdf>";
      protoMap.put("joinNew", xml);
      protoChooser.addItem("joinNew");

      xml =
          "<?xml version='1.0' encoding='UTF-8'?>\n" +
              "<netcdf xmlns='http://www.unidata.ucar.edu/namespaces/netcdf/ncml-2.2'>\n" +
              "  <aggregation dimName='time' type='joinExisting'>\n" +
              "    <scan location='ncml/nc/pfeg/' suffix='.nc' />\n" +
              "  </aggregation>\n" +
              "</netcdf>";
      protoMap.put("joinExisting", xml);
      protoChooser.addItem("joinExisting");

    }

  }

  // the old inventory stuff
  private class FmrcInvPanel extends OpPanel {
    private boolean useDefinition = false;
    private JComboBox defComboBox, catComboBox, dirComboBox, suffixCB;
    private IndependentWindow defWindow, catWindow;
    private IndependentWindow dirWindow;
    private AbstractButton defButt;
    private JSpinner catSpinner;

    FmrcInvPanel(PreferencesExt p) {
      super(p, "ForecastModelRun:", true, false);

      // allow to set a definition file for GRIB
      AbstractAction defineAction = new AbstractAction() {
        public void actionPerformed(ActionEvent e) {
          Boolean state = (Boolean) getValue(BAMutil.STATE);
          useDefinition = state.booleanValue();
          String tooltip = useDefinition ? "Use GRIB Definition File is ON" : "Use GRIB Definition File is OFF";
          defButt.setToolTipText(tooltip);
          if (useDefinition) {
            if (null == defComboBox) {
              defComboBox = new JComboBox(FmrcDefinition.getDefinitionFiles());
              defComboBox.setEditable(true);
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

      // compare against the  definition file
      AbstractAction testDefAction = new AbstractAction() {
        public void actionPerformed(ActionEvent e) {
          if (!useDefinition) return;
          testDefinition();
        }
      };
      BAMutil.setActionProperties(testDefAction, "dd", "test file against current definition", false, 'T', -1);
      BAMutil.addActionToContainer(buttPanel, testDefAction);

      // make definition from catalog
      AbstractAction catAction = new AbstractAction() {
        public void actionPerformed(ActionEvent e) {
          if (null == catComboBox) {
            catComboBox = new JComboBox();
            catComboBox.setEditable(true);
            makeCatalogDefaults(catComboBox);
            catSpinner = new JSpinner();
            JButton accept = new JButton("Accept");
            JPanel catPanel = new JPanel(new BorderLayout());
            JPanel leftPanel = new JPanel();
            leftPanel.add(new JLabel("Num datasets:"));
            leftPanel.add(catSpinner);
            leftPanel.add(new JLabel("Catalog URL:"));
            catPanel.add(catComboBox, BorderLayout.CENTER);
            catPanel.add(leftPanel, BorderLayout.WEST);
            catPanel.add(accept, BorderLayout.EAST);

            accept.addActionListener(new ActionListener() {
              public void actionPerformed(ActionEvent e) {
                defineFromCatalog((String) catComboBox.getSelectedItem(), catSpinner.getValue());
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

      // make definition from files in a directory
      AbstractAction dirAction = new AbstractAction() {
        public void actionPerformed(ActionEvent e) {
          if (null == dirWindow) {
            dirComboBox = new JComboBox();
            dirComboBox.setEditable(true);
            suffixCB = new JComboBox();
            suffixCB.setEditable(true);
            JButton accept = new JButton("Accept");
            JPanel dirPanel = new JPanel(new BorderLayout());
            JPanel leftPanel = new JPanel();
            leftPanel.add(new JLabel("Suffix:"));
            leftPanel.add(suffixCB);
            leftPanel.add(new JLabel("Directory:"));
            dirPanel.add(leftPanel, BorderLayout.WEST);
            dirPanel.add(dirComboBox, BorderLayout.CENTER);
            dirPanel.add(accept, BorderLayout.EAST);

            accept.addActionListener(new ActionListener() {
              public void actionPerformed(ActionEvent e) {
                defineFromDirectory((String) dirComboBox.getSelectedItem(), (String) suffixCB.getSelectedItem());
              }
            });
            dirWindow = new IndependentWindow("Directory", null, dirPanel);
            dirWindow.setLocationRelativeTo(defButt);
            dirWindow.setLocation(100, 100);
          }
          dirWindow.show();
        }
      };
      BAMutil.setActionProperties(dirAction, "Dimension", "make definition from files in directory", false, 'D', -1);
      BAMutil.addActionToContainer(buttPanel, dirAction);

      // delete GRIB index
      AbstractAction deleteAction = new AbstractAction() {
        public void actionPerformed(ActionEvent e) {
          String currentFile = (String) cb.getSelectedItem();
          File file = new File(currentFile + ".gbx");
          if (file.exists()) {
            boolean ok = file.delete();
            JOptionPane.showMessageDialog(null, "Index deleted " + ok + ", reopen= " + currentFile);
            process(currentFile);
          }
        }
      };
      BAMutil.setActionProperties(deleteAction, "Delete", "Delete Grib Index", false, 'T', -1);
      BAMutil.addActionToContainer(buttPanel, deleteAction);
    }

    private void makeCatalogDefaults(JComboBox cb) {
      String server = "http://motherlode.ucar.edu:8080/thredds/catalog/fmrc/";
      for (String ds : FmrcDefinition.fmrcDatasets)
        cb.addItem(server + ds + "/files/catalog.xml");
    }

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
            System.out.println("Read Definition file = " + currentDef);
            spiObject = fmrc_def;
            NetcdfDataset ds = NetcdfDataset.openDataset(command, true, -1, null, spiObject);
            gds = new ucar.nc2.dt.grid.GridDataset(ds);
          } else {
            JOptionPane.showMessageDialog(null, "cant open Definition file " + currentDef);
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
        fmrInv.writeXML(bos);
        ta.setText("ForecastModelRunInventory output for a single model run:\n\n");
        ta.appendLine(bos.toString());
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

    private void testDefinition() {
      ByteArrayOutputStream bos = new ByteArrayOutputStream(10000);
      try {
        String currentDef = (String) defComboBox.getSelectedItem();
        if (currentDef == null) return;

        String currentFilename = (String) cb.getSelectedItem();
        if (currentFilename == null) return;

        FmrcDefinition fmrc_def = new FmrcDefinition();
        fmrc_def.readDefinitionXML(currentDef);
        NetcdfDataset.openDataset(currentFilename, true, -1, null, fmrc_def);

      } catch (Exception ioe) {
        ioe.printStackTrace();
        ioe.printStackTrace(new PrintStream(bos));
        ta.setText(bos.toString());
      }
    }


    private void defineFromDirectory(String dirName, String suffix) {
      ByteArrayOutputStream bos = new ByteArrayOutputStream(10000);
      try {
        FmrcInventory fmrCollection = FmrcInventory.makeFromDirectory(null, "test",
            null, dirName, suffix, ForecastModelRunInventory.OPEN_FORCE_NEW);

        FmrcDefinition def = new FmrcDefinition();
        def.makeFromCollectionInventory(fmrCollection);

        def.writeDefinitionXML(bos);
        ta.setText(bos.toString());
        ta.gotoTop();

      } catch (Exception ioe) {
        ioe.printStackTrace();
        ioe.printStackTrace(new PrintStream(bos));
        ta.setText(bos.toString());
      }
    }

    private void defineFromCatalog(String catalogURLString, Object value) {
      int n = ((Integer) value).intValue();

      ByteArrayOutputStream bos = new ByteArrayOutputStream(10000);
      try {
        FmrcInventory fmrCollection = FmrcInventory.makeFromCatalog(catalogURLString, catalogURLString, n, ForecastModelRunInventory.OPEN_FORCE_NEW);
        FmrcDefinition def = new FmrcDefinition();
        def.makeFromCollectionInventory(fmrCollection);

        def.writeDefinitionXML(bos);
        ta.setText(bos.toString());
        ta.gotoTop();

      } catch (Exception ioe) {
        ioe.printStackTrace();
        ioe.printStackTrace(new PrintStream(bos));
        ta.setText(bos.toString());
      }
    }

  }

  // the old Fmrc Impl stuff
  private class FmrcImplPanel extends OpPanel {
    FmrcImpl fmrc;
    FmrcTable table;

    FmrcImplPanel(PreferencesExt dbPrefs) {
      super(dbPrefs, "dataset:", true, false);
      table = new FmrcTable(prefs);
      table.addPropertyChangeListener(new java.beans.PropertyChangeListener() {
        public void propertyChange(java.beans.PropertyChangeEvent e) {
          if (e.getPropertyName().equals("openNetcdfFile")) {
            NetcdfDataset dataset = (NetcdfDataset) e.getNewValue();
            openNetcdfFile(dataset);
          } else if (e.getPropertyName().equals("openCoordSystems")) {
            NetcdfDataset dataset = (NetcdfDataset) e.getNewValue();
            openCoordSystems(dataset);
          } else if (e.getPropertyName().equals("openGridDataset")) {
            NetcdfDataset dataset = (NetcdfDataset) e.getNewValue();
            openGridDataset(dataset);
          }
        }
      });
      add(table, BorderLayout.CENTER);

      AbstractButton infoButton = BAMutil.makeButtcon("Information", "Detail Info", false);
      infoButton.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          if (fmrc != null) {
            Formatter f = new Formatter();
            try {
              fmrc.dump(f);
            } catch (IOException ioe) {
              ByteArrayOutputStream bos = new ByteArrayOutputStream(10000);
              ioe.printStackTrace();
              ioe.printStackTrace(new PrintStream(bos));
              // ta.setText( datasetFactory.getErrorMessages());
              ta.appendLine(bos.toString());
            }
            detailTA.setText(f.toString());
            detailTA.gotoTop();
            detailWindow.show();
          }
        }
      });
      buttPanel.add(infoButton);
    }

    boolean process(Object o) {
      String command = (String) o;
      if (command == null) return false;

      if (fmrc != null) {
        try {
          fmrc.close();
        } catch (IOException ioe) {
        }
      }

      try {
        fmrc = new FmrcImpl(command);
        table.setFmrc(fmrc);
        return true;

      } catch (Exception ioe) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream(10000);
        ioe.printStackTrace();
        ioe.printStackTrace(new PrintStream(bos));
        detailTA.setText(bos.toString());
        detailTA.gotoTop();
        detailWindow.show();
      }

      return false;
    }
  }

  // new ucar.nc2.ft.fmrc stuff
  private class FmrcPanel extends OpPanel {
    Fmrc2Panel table;

    FmrcPanel(PreferencesExt dbPrefs) {
      super(dbPrefs, "collection:", true, false);
      table = new Fmrc2Panel(prefs);
      table.addPropertyChangeListener(new java.beans.PropertyChangeListener() {
        public void propertyChange(java.beans.PropertyChangeEvent e) {

          if (e.getPropertyName().equals("openNetcdfFile")) {
            if (e.getNewValue() instanceof String)
              openNetcdfFile((String) e.getNewValue());
            else
              openNetcdfFile((NetcdfFile) e.getNewValue());

          } else if (e.getPropertyName().equals("openCoordSys")) {
            if (e.getNewValue() instanceof String)
              openCoordSystems((String) e.getNewValue());
            else
              openCoordSystems((NetcdfDataset) e.getNewValue());

          } else if (e.getPropertyName().equals("openGridDataset")) {
            if (e.getNewValue() instanceof String)
              openGridDataset((String) e.getNewValue());
            else
              openGridDataset((GridDataset) e.getNewValue());
          }
        }
      });
      add(table, BorderLayout.CENTER);

      AbstractButton infoButton = BAMutil.makeButtcon("Information", "Detail Info", false);
      infoButton.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          Formatter f = new Formatter();
          try {
            table.showInfo(f);
          } catch (IOException e1) {
            ByteArrayOutputStream bos = new ByteArrayOutputStream(5000);
            e1.printStackTrace(new PrintStream(bos));
            f.format("%s", bos.toString());
          }
          detailTA.setText(f.toString());
          detailTA.gotoTop();
          detailWindow.show();
        }
      });
      buttPanel.add(infoButton);

      AbstractButton collectionButton = BAMutil.makeButtcon("Information", "Collection Parsing Info", false);
      collectionButton.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
            table.showCollectionInfo(true);
        }
      });
      buttPanel.add(collectionButton);

      AbstractButton viewButton = BAMutil.makeButtcon("Dump", "Show in Netcdf Viewer", false);
      viewButton.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          try {
            table.showDataset();

          } catch (IOException e1) {
            ByteArrayOutputStream bos = new ByteArrayOutputStream(5000);
            e1.printStackTrace(new PrintStream(bos));
            detailTA.setText(bos.toString());
            detailTA.gotoTop();
            detailWindow.show();
          }
        }
      });
      buttPanel.add(viewButton);
    }

    boolean process(Object o) {
      String command = (String) o;
      if (command == null) return false;

      /* if (fmrc != null) {
        try {
          fmrc.close();
        } catch (IOException ioe) {
        }
      } */

      try {
        table.setFmrc(command);
        return true;

      } catch (Exception ioe) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream(10000);
        ioe.printStackTrace();
        ioe.printStackTrace(new PrintStream(bos));
        detailTA.setText(bos.toString());
        detailTA.gotoTop();
        detailWindow.show();
      }

      return false;
    }

    void save() {
      table.save();
      super.save();
    }
  }

  // new Fmrc
  private class CollectionPanel extends OpPanel {
    CollectionTable table;

    CollectionPanel(PreferencesExt dbPrefs) {
      super(dbPrefs, "collection:", true, false);
      table = new CollectionTable(prefs);
      /* table.addPropertyChangeListener(new java.beans.PropertyChangeListener() {
        public void propertyChange(java.beans.PropertyChangeEvent e) {

          if (e.getPropertyName().equals("openNetcdfFile")) {
            if (e.getNewValue() instanceof String)
              openNetcdfFile((String) e.getNewValue());
            else
              openNetcdfFile((NetcdfFile) e.getNewValue());

          } else if (e.getPropertyName().equals("openCoordSys")) {
            if (e.getNewValue() instanceof String)
              openCoordSystems((String) e.getNewValue());
            else
              openCoordSystems((NetcdfDataset) e.getNewValue());

          } else if (e.getPropertyName().equals("openGridDataset")) {
            if (e.getNewValue() instanceof String)
              openGridDataset((String) e.getNewValue());
            else
              openGridDataset((NetcdfDataset) e.getNewValue());
          }
        }
      }); */
      add(table, BorderLayout.CENTER); 

      AbstractButton infoButton = BAMutil.makeButtcon("Information", "Detail Info", false);
      infoButton.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          Formatter f = new Formatter();
          try {
            table.showInfo(f);
          } catch (IOException e1) {
            ByteArrayOutputStream bos = new ByteArrayOutputStream(5000);
            e1.printStackTrace(new PrintStream(bos));
            f.format("%s", bos.toString());
          }
          detailTA.setText(f.toString());
          detailTA.gotoTop();
          detailWindow.show();
        }
      });
      buttPanel.add(infoButton);

      AbstractButton refreshButton = BAMutil.makeButtcon("Undo", "Refresh", false);
      refreshButton.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          try {
            table.refresh();
            
          } catch (Exception e1) {
            Formatter f = new Formatter();
            ByteArrayOutputStream bos = new ByteArrayOutputStream(5000);
            e1.printStackTrace(new PrintStream(bos));
            f.format("%s", bos.toString());
            detailTA.setText(f.toString());
            detailTA.gotoTop();
            detailWindow.show();
          }
        }
      });
      buttPanel.add(refreshButton);

    }

    boolean process(Object o) {
      String command = (String) o;
      if (command == null) return false;

      try {
        // table.setCacheRoot(command);
        return true;

      } catch (Exception ioe) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream(10000);
        ioe.printStackTrace();
        ioe.printStackTrace(new PrintStream(bos));
        detailTA.setText(bos.toString());
        detailTA.gotoTop();
        detailWindow.show();
      }

      return false;
    }

    void save() {
      table.save();
      super.save();
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
            GridDataset gridDataset = dsTable.getGridDataset();
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
          GridDataset gridDataset = dsTable.getGridDataset();
          if ((gridDataset != null) && (gridDataset instanceof ucar.nc2.dt.grid.GridDataset)) {
            ucar.nc2.dt.grid.GridDataset gdsImpl = (ucar.nc2.dt.grid.GridDataset) gridDataset;
            detailTA.clear();
            detailTA.appendLine(gdsImpl.getDetailInfo());
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
            GridDataset gridDataset = dsTable.getGridDataset();
            URI gdUri = null;
            try {
              gdUri = new URI("http://none.such.server/thredds/wcs/dataset");
            }
            catch (URISyntaxException e1) {
              e1.printStackTrace();
              return;
            }
            GetCapabilities getCap =
                ((thredds.wcs.v1_0_0_1.GetCapabilitiesBuilder)
                    thredds.wcs.v1_0_0_1.WcsRequestBuilder
                        .newWcsRequestBuilder("1.0.0",
                            thredds.wcs.Request.Operation.GetCapabilities,
                            gridDataset, ""))
                    .setServerUri(gdUri)
                    .setSection(GetCapabilities.Section.All)
                    .buildGetCapabilities();
            try {
              String gc = getCap.writeCapabilitiesReportAsString();
              detailTA.setText(gc);
              detailTA.gotoTop();
              detailWindow.show();
            } catch (WcsException e1) {
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
      try {
        dsTable.setDataset(newds);
      } catch (IOException e) {
        e.printStackTrace();
        return;
      }
      setSelectedItem(newds.getLocation());
    }

    void setDataset(GridDataset gds) {
      if (gds == null) return;
      try {
        if (ds != null) ds.close();
      } catch (IOException ioe) {
      }

      this.ds = (NetcdfDataset) gds.getNetcdfFile(); // ??
      try {
        dsTable.setDataset(gds);
      } catch (IOException e) {
        e.printStackTrace();
        return;
      }
      setSelectedItem(gds.getLocationURI());
    }

    void save() {
      super.save();
      dsTable.save();
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

        Formatter errlog = new Formatter();
        RadialDatasetSweep rds = (RadialDatasetSweep) FeatureDatasetFactoryManager.wrap(FeatureType.RADIAL, newds, null, errlog);
        if (rds == null) {
          JOptionPane.showMessageDialog(null, "FeatureDatasetFactoryManager cant open " + command + "as RADIAL dataset\n" + errlog.toString());
          err = true;
        } else {
          setDataset(rds);
        }

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
    }

  }

  private class ViewerPanel extends OpPanel {
    DatasetViewer dsViewer;
    JSplitPane split;
    NetcdfFile ncfile = null;

    ViewerPanel(PreferencesExt dbPrefs) {
      super(dbPrefs, "dataset:");
      dsViewer = new DatasetViewer(dbPrefs, fileChooser);
      add(dsViewer, BorderLayout.CENTER);

      AbstractButton infoButton = BAMutil.makeButtcon("Information", "Detail Info", false);
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
              makeComponent(tabbedPane, "NCDump");
            }
            ncdumpPanel.setNetcdfFile(ds);
            tabbedPane.setSelectedComponent(ncdumpPanel);
          }
        }
      };
      BAMutil.setActionProperties(dumpAction, "Dump", "NCDump", false, 'D', -1);
      BAMutil.addActionToContainer(buttPanel, dumpAction);

      AbstractAction netcdfAction = new AbstractAction() {
        public void actionPerformed(ActionEvent e) {
          String location = ncfile.getLocation();
          if (location == null) location = "test";
          int pos = location.lastIndexOf(".");
          if (pos > 0)
            location = location.substring(0, pos);

          String filename = fileChooser.chooseFilenameToSave(location + ".nc");
          if (filename == null) return;
          doWriteNetCDF(filename);
        }
      };
      BAMutil.setActionProperties(netcdfAction, "netcdf", "Write local netCDF file", false, 'S', -1);
      BAMutil.addActionToContainer(buttPanel, netcdfAction);

      dsViewer.addActions( buttPanel);


      /* AbstractAction syncAction = new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        NetcdfFile ds = dsViewer.getDataset();
        if (ds != null)
          try {
            ds.syncExtend();
            dsViewer.setDataset(ds);
          } catch (IOException e1) {
            e1.printStackTrace();
          }
      }
    };
    BAMutil.setActionProperties(syncAction, null, "SyncExtend", false, 'D', -1);
    BAMutil.addActionToContainer(buttPanel, syncAction); */
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

    void doWriteNetCDF(String filename) {
      try {
        FileWriter.writeToFile(ncfile, filename, false, -1, false);
        JOptionPane.showMessageDialog(this, "File successfully written");
      } catch (Exception ioe) {
        JOptionPane.showMessageDialog(this, "ERROR: " + ioe.getMessage());
        ioe.printStackTrace();
      }
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

      StringBuilder log = new StringBuilder();
      ByteArrayOutputStream bos = new ByteArrayOutputStream(10000);
      try {
        pobsDataset = (PointObsDataset) TypedDatasetFactory.open(FeatureType.POINT, location, null, log);
        if (pobsDataset == null) {
          JOptionPane.showMessageDialog(null, "Can't open " + location + ": " + log);
          return false;
        }

        povTable.setDataset(pobsDataset);
        setSelectedItem(location);
        return true;

      } catch (Exception e) {
        e.printStackTrace();
        e.printStackTrace(new PrintStream(bos));
        ta.setText(log.toString());
        ta.appendLine(bos.toString());

        JOptionPane.showMessageDialog(this, e.getMessage());
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

  private class StationObsPanel extends OpPanel {
    StationObsViewer povTable;
    JSplitPane split;
    StationObsDataset sobsDataset = null;

    StationObsPanel(PreferencesExt dbPrefs) {
      super(dbPrefs, "dataset:", true, false);
      povTable = new StationObsViewer(dbPrefs);
      add(povTable, BorderLayout.CENTER);

      AbstractButton infoButton = BAMutil.makeButtcon("Information", "Dataset Info", false);
      infoButton.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          String info;
          if ((sobsDataset != null) && ((info = sobsDataset.getDetailInfo()) != null)) {
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
      povTable.save();
    }

    boolean setStationObsDataset(String location) {
      if (location == null) return false;

      try {
        if (sobsDataset != null) sobsDataset.close();
      } catch (IOException ioe) {
      }

      StringBuilder log = new StringBuilder();
      ByteArrayOutputStream bos = new ByteArrayOutputStream(10000);
      try {
        sobsDataset = (StationObsDataset) TypedDatasetFactory.open(FeatureType.STATION, location, null, log);
        if (sobsDataset == null) {
          JOptionPane.showMessageDialog(null, "Can't open " + location + ": " + log);
          return false;
        }

        povTable.setDataset(sobsDataset);
        setSelectedItem(location);
        return true;

      } catch (Exception e) {
        e.printStackTrace();
        e.printStackTrace(new PrintStream(bos));
        ta.setText(log.toString());
        ta.appendLine(bos.toString());

        JOptionPane.showMessageDialog(this, e.getMessage());
        return false;
      }
    }

    boolean setStationObsDataset(StationObsDataset dataset) {
      if (dataset == null) return false;

      try {
        if (sobsDataset != null) sobsDataset.close();
      } catch (IOException ioe) {
      }

      povTable.setDataset(dataset);
      sobsDataset = dataset;
      setSelectedItem(sobsDataset.getLocationURI());
      return true;
    }
  }

  /////////////////////////////////////////////////////////////////////
  private class FeatureScanPanel extends OpPanel {
    ucar.unidata.io.RandomAccessFile raf = null;
    ucar.nc2.ui.FeatureScanPanel ftTable;
    final FileManager dirChooser = new FileManager(parentFrame);

    boolean useDefinition = false;
    JComboBox defComboBox;
    IndependentWindow defWindow;
    AbstractButton defButt;

    FeatureScanPanel(PreferencesExt p) {
      super(p, "dir:", false, false);
      ftTable = new ucar.nc2.ui.FeatureScanPanel(prefs);
      add(ftTable, BorderLayout.CENTER);
      ftTable.addPropertyChangeListener(new java.beans.PropertyChangeListener() {
        public void propertyChange(java.beans.PropertyChangeEvent e) {
          if (e.getPropertyName().equals("openPointFeatureDataset")) {
            String datasetName = (String) e.getNewValue();
            openPointFeatureDataset(datasetName);
          } else if (e.getPropertyName().equals("openNetcdfFile")) {
            String datasetName = (String) e.getNewValue();
            openNetcdfFile(datasetName);
          } else if (e.getPropertyName().equals("openCoordSystems")) {
            String datasetName = (String) e.getNewValue();
            openCoordSystems(datasetName);
          } else if (e.getPropertyName().equals("openNcML")) {
            String datasetName = (String) e.getNewValue();
            openNcML(datasetName);
          } else if (e.getPropertyName().equals("openGridDataset")) {
            String datasetName = (String) e.getNewValue();
            openGridDataset(datasetName);
          } else if (e.getPropertyName().equals("openRadialDataset")) {
            String datasetName = (String) e.getNewValue();
            openRadialDataset(datasetName);
          }
        }
      });

      dirChooser.getFileChooser().setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
      dirChooser.setCurrentDirectory(prefs.get("currDir", "."));
      AbstractAction fileAction = new AbstractAction() {
        public void actionPerformed(ActionEvent e) {
          String filename = dirChooser.chooseFilename();
          if (filename == null) return;
          cb.setSelectedItem(filename);
        }
      };
      BAMutil.setActionProperties(fileAction, "FileChooser", "open Local dataset...", false, 'L', -1);
      BAMutil.addActionToContainer(buttPanel, fileAction);
    }

    boolean process(Object o) {
      String command = (String) o;
      return ftTable.setScanDirectory(command);
    }

    void save() {
      ftTable.save();
      prefs.put("currDir", dirChooser.getCurrentDirectory());
      super.save();
    }

  }

////////////////////////////////////////////////////////////////////////

  private class PointFeaturePanel extends OpPanel {
    PointFeatureDatasetViewer pfViewer;
    JSplitPane split;
    FeatureDatasetPoint pfDataset = null;
    JComboBox types;

    PointFeaturePanel(PreferencesExt dbPrefs) {
      super(dbPrefs, "dataset:", true, false);
      pfViewer = new PointFeatureDatasetViewer(dbPrefs);
      add(pfViewer, BorderLayout.CENTER);

      types = new JComboBox();
      for (FeatureType ft : FeatureType.values())
        types.addItem(ft);
      types.getModel().setSelectedItem(FeatureType.ANY_POINT);
      buttPanel.add(types);

      AbstractButton infoButton = BAMutil.makeButtcon("Information", "Dataset Info", false);
      infoButton.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          if (pfDataset == null) return;
          Formatter f = new Formatter();
          pfDataset.getDetailInfo(f);
          detailTA.setText(f.toString());
          detailTA.appendLine("-----------------------------");
          detailTA.appendLine(getCapabilities(pfDataset));
          detailTA.gotoTop();
          detailWindow.show();
        }
      });
      buttPanel.add(infoButton);

      AbstractButton xmlButton = BAMutil.makeButtcon("XML", "pointConfig.xml", false);
      xmlButton.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          if (pfDataset == null) return;
          Formatter f = new Formatter();
          ucar.nc2.ft.point.standard.PointConfigXML.writeConfigXML(pfDataset, f);
          detailTA.setText(f.toString());
          detailTA.gotoTop();
          detailWindow.show();
        }
      });
      buttPanel.add(xmlButton);

      AbstractButton calcButton = BAMutil.makeButtcon("V3", "CalcBounds", false);
      calcButton.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          if (pfDataset == null) return;
          Formatter f = new Formatter();
          try {
            pfDataset.calcBounds();
            pfDataset.getDetailInfo(f);
            detailTA.setText(f.toString());

          } catch (IOException ioe) {
            ByteArrayOutputStream bos = new ByteArrayOutputStream(5000);
            ioe.printStackTrace(new PrintStream(bos));
            detailTA.setText(bos.toString());
          }

          detailTA.gotoTop();
          detailWindow.show();
        }
      });
      buttPanel.add(calcButton);

      AbstractAction netcdfAction = new AbstractAction() {
        public void actionPerformed(ActionEvent e) {
          String location = pfDataset.getLocation();
          if (location == null) location = "test";
          int pos = location.lastIndexOf(".");
          if (pos > 0)
            location = location.substring(0, pos);

          String filename = fileChooser.chooseFilenameToSave(location + ".nc");
          if (filename == null) return;
          doWriteCF(filename);
        }
      };
      BAMutil.setActionProperties(netcdfAction, "netcdf", "Write netCDF-CF", false, 'N', -1);
      BAMutil.addActionToContainer(buttPanel, netcdfAction);
    }

    boolean process(Object o) {
      String location = (String) o;
      return setPointFeatureDataset((FeatureType) types.getSelectedItem(), location);
    }

    void save() {
      super.save();
      pfViewer.save();
    }

    void doWriteCF(String filename) {
      try {
        int count = WriterCFPointDataset.writePointFeatureCollection(pfDataset, filename);
        JOptionPane.showMessageDialog(this, count + " records written");
      } catch (Exception e) {
        JOptionPane.showMessageDialog(this, "ERROR: " + e.getMessage());
        e.printStackTrace();
      }
    }

    private boolean setPointFeatureDataset(FeatureType type, String location) {
      if (location == null) return false;

      try {
        if (pfDataset != null) pfDataset.close();
      } catch (IOException ioe) {
      }
      detailTA.clear();

      Formatter log = new Formatter();
      ByteArrayOutputStream bos = new ByteArrayOutputStream(10000);
      try {
        FeatureDataset featureDataset = FeatureDatasetFactoryManager.open(type, location, null, log);
        if (featureDataset == null) {
          JOptionPane.showMessageDialog(null, "Can't open " + location + ": " + log);
          return false;
        }
        if (!(featureDataset instanceof FeatureDatasetPoint)) {
          JOptionPane.showMessageDialog(null, location + " could not be opened as a PointFeatureDataset");
          return false;
        }

        pfDataset = (FeatureDatasetPoint) featureDataset;
        pfViewer.setDataset(pfDataset);
        setSelectedItem(location);
        return true;

      } catch (IOException e) {
        String message = e.getClass().getName() + ": " + e.getMessage();
        JOptionPane.showMessageDialog(this, message);
        return false;

      } catch (Throwable e) {
        e.printStackTrace();
        e.printStackTrace(new PrintStream(bos));
        ta.setText(log.toString());
        ta.appendLine(bos.toString());

        JOptionPane.showMessageDialog(this, e.getMessage());
        return false;
      }
    }

    private boolean setPointFeatureDataset(FeatureDatasetPoint pfd) {

      try {
        if (pfDataset != null) pfDataset.close();
      } catch (IOException ioe) {
      }
      detailTA.clear();

      Formatter log = new Formatter();
      ByteArrayOutputStream bos = new ByteArrayOutputStream(10000);
      try {
        pfDataset = pfd;
        pfViewer.setDataset(pfDataset);
        setSelectedItem(pfDataset.getLocation());
        return true;

      } catch (Throwable e) {
        e.printStackTrace();
        e.printStackTrace(new PrintStream(bos));
        ta.setText(log.toString());
        ta.appendLine(bos.toString());

        JOptionPane.showMessageDialog(this, e.getMessage());
        return false;
      }
    }

    private String getCapabilities(FeatureDatasetPoint fdp) {
      ucar.nc2.ft.point.writer.FeatureDatasetPointXML xmlWriter = new ucar.nc2.ft.point.writer.FeatureDatasetPointXML(fdp, null);
      return xmlWriter.getCapabilities();
    }
  }

  private class WmsPanel extends OpPanel {
    WmsViewer wmsViewer;
    JSplitPane split;
    FeatureDatasetPoint pfDataset = null;
    JComboBox types;

    WmsPanel(PreferencesExt dbPrefs) {
      super(dbPrefs, "dataset:", true, false);
      wmsViewer = new WmsViewer(dbPrefs, frame);
      add(wmsViewer, BorderLayout.CENTER);

      buttPanel.add(new JLabel("version:"));
      types = new JComboBox();
      types.addItem("1.3.0");
      types.addItem("1.1.1");
      types.addItem("1.0.0");
      buttPanel.add(types);

      AbstractButton infoButton = BAMutil.makeButtcon("Information", "Detail Info", false);
      infoButton.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          detailTA.setText(wmsViewer.getDetailInfo());
          detailTA.gotoTop();
          detailWindow.show();
        }
      });
      buttPanel.add(infoButton);
    }

    boolean process(Object o) {
      String location = (String) o;
      return wmsViewer.setDataset((String) types.getSelectedItem(), location);
    }

    void save() {
      super.save();
      wmsViewer.save();
    }
  }

  private class StationRadialPanel extends OpPanel {
    StationRadialViewer radialViewer;
    JSplitPane split;
    StationRadarCollectionImpl radarCollectionDataset = null;

    StationRadialPanel(PreferencesExt dbPrefs) {
      super(dbPrefs, "dataset:", true, false);
      radialViewer = new StationRadialViewer(dbPrefs);
      add(radialViewer, BorderLayout.CENTER);

      AbstractButton infoButton = BAMutil.makeButtcon("Information", "Dataset Info", false);
      infoButton.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          String info;
          if ((radarCollectionDataset != null) && ((info = radarCollectionDataset.getDetailInfo()) != null)) {
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
      return setStationRadialDataset(location);
    }

    void save() {
      //super.save();
      //radialViewer.save();
    }

    boolean setStationRadialDataset(String location) {
      if (location == null) return false;

      try {
        if (radarCollectionDataset != null) radarCollectionDataset.close();
      } catch (IOException ioe) {
      }

      StringBuilder log = new StringBuilder();
      ByteArrayOutputStream bos = new ByteArrayOutputStream(10000);
      try {
        radarCollectionDataset = (StationRadarCollectionImpl) TypedDatasetFactory.open(FeatureType.STATION_RADIAL, location, null, log);
        if (radarCollectionDataset == null) {
          JOptionPane.showMessageDialog(null, "Can't open " + location + ": " + log);
          return false;
        }

        radialViewer.setDataset((DqcRadarDatasetCollection) radarCollectionDataset);
        setSelectedItem(location);
        return true;

      } catch (Exception e) {
        e.printStackTrace();
        e.printStackTrace(new PrintStream(bos));
        ta.setText(log.toString());
        ta.appendLine(bos.toString());

        JOptionPane.showMessageDialog(this, e.getMessage());
        return false;
      }
    }

    boolean setStationRadialDataset(StationRadarCollectionImpl dataset) {
      if (dataset == null) return false;

      try {
        if (radarCollectionDataset != null) radarCollectionDataset.close();
      } catch (IOException ioe) {
      }

      radarCollectionDataset = dataset;
      radialViewer.setDataset((DqcRadarDatasetCollection) radarCollectionDataset);
      setSelectedItem(radarCollectionDataset.getLocation());
      return true;
    }
  }

  private class TrajectoryTablePanel extends OpPanel {
    TrajectoryObsViewer viewer;
    JSplitPane split;
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
        StringBuilder errlog = new StringBuilder();
        ds = (TrajectoryObsDataset) TypedDatasetFactory.open(FeatureType.TRAJECTORY, location, null, errlog);
        if (ds == null) {
          JOptionPane.showMessageDialog(null, "Can't open " + location + ": " + errlog);
          return false;
        }

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

      GridDataset gridDs = null;
      try {
        gridDs = ucar.nc2.dt.grid.GridDataset.open(filename);
        java.util.List grids = gridDs.getGrids();
        if (grids.size() == 0) {
          System.out.println("No grids found.");
          return false;
        }

        GridDatatype grid = (GridDatatype) grids.get(0);
        ucar.ma2.Array data = grid.readDataSlice(0, 0, -1, -1); // first time, level

        String fileOut = fileChooser.chooseFilenameToSave(filename + ".tif");
        if (fileOut == null) return false;

        ucar.nc2.geotiff.GeotiffWriter writer = new ucar.nc2.geotiff.GeotiffWriter(fileOut);
        writer.writeGrid(gridDs, grid, data, false);

        read(fileOut);
        JOptionPane.showMessageDialog(null, "File written to " + fileOut);


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
          "<h1>Netcdf Tools User Interface (ToolsUI)</h1>" +
          "<b>" + getVersion() + "</b>" +
          "<br><i>http://www.unidata.ucar.edu/software/netcdf-java/</i>" +
          "<br><b><i>Developers:</b>John Caron, Ethan Davis, Robb Kambic, Yuan Ho</i></b>" +
          "</center>" +
          "<br><br>With thanks to these <b>Open Source</b> contributers:" +
          "<ul>" +
          "<li><b>ADDE/VisAD</b>: Bill Hibbard, Don Murray, Tom Whittaker, et al (http://www.ssec.wisc.edu/~billh/visad.html)</li>" +
          "<li><b>Apache Jakarta Commons</b> libraries: (http://http://jakarta.apache.org/commons/)</li>" +
          "<li><b>Apache Log4J</b> library: (http://logging.apache.org/log4j/) </li>" +
          "<li><b>IDV:</b> Don Murray, Jeff McWhirter (http://www.unidata.ucar.edu/software/IDV/)</li>" +
          "<li><b>JDOM</b> library: Jason Hunter, Brett McLaughlin et al (www.jdom.org)</li>" +
          "<li><b>JGoodies</b> library: Karsten Lentzsch (www.jgoodies.com)</li>" +
          "<li><b>JPEG-2000</b> Java library: (http://www.jpeg.org/jpeg2000/)</li>" +
          "<li><b>JUnit</b> library: Erich Gamma, Kent Beck, Erik Meade, et al (http://sourceforge.net/projects/junit/)</li>" +
          "<li><b>OPeNDAP Java</b> library: Nathan Potter, James Gallagher, Don Denbo, et. al.(http://opendap.org)</li>" +
          "<li><b>Spring lightweight framework</b> library: Rod Johnson, et. al.(http://www.springsource.org/)</li>" +
          "<li><b>Imaging utilities:</b>: Richard Eigenmann</li>" +
          "</ul><center>Special thanks to <b>Sun Microsystems</b> (java.sun.com) for the platform on which we stand." +
          "</center></body></html> ");

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

  private String getVersion() {

    String version;
    try {
      InputStream is = ucar.nc2.ui.util.Resource.getFileResource("/README");
      if (is == null) return "4.2";
// DataInputStream dataIS = new DataInputStream( new BufferedInputStream(ios, 20000));
      BufferedReader dataIS = new BufferedReader(new InputStreamReader(is));
      StringBuilder sbuff = new StringBuilder();
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
    ucar.nc2.util.cache.FileCache cache = NetcdfDataset.getNetcdfFileCache();
    if (cache != null)
      cache.clearCache(true);
    NetcdfDataset.shutdown(); // shutdown threads
    cacheManager.close(); // shutdown ehcache
    MetadataManager.closeAll(); // shutdown bdb

    System.exit(0);
  }

  // handle messages
  private static ToolsUI ui;
  private static JFrame frame;
  private static PreferencesExt prefs;
  private static XMLStore store;
  private static boolean done = false;
  private static MController cacheManager;

  private static String wantDataset = null;

  private static void setDataset() {
    SwingUtilities.invokeLater(new Runnable() { // do it in the swing event thread

      public void run() {
        ui.makeComponent(null, "THREDDS");
        ui.threddsUI.setDataset(wantDataset);
        ui.tabbedPane.setSelectedComponent(ui.threddsUI);
      }
    });
  }

  static boolean isCacheInit = false;
  static boolean isDiskCacheInit = false;

  public static void main(String args[]) {

    // get a splash screen up right away
    final SplashScreen splash = new SplashScreen();

    if (debugListen) {
      System.out.println("Arguments:");
      for (int i = 0; i < args.length; i++) {
        String arg = args[i];
        System.out.println(" " + arg);
      }
    }

    //////////////////////////////////////////////////////////////////////////
    // handle multiple versions of ToolsUI, along with passing a dataset name
    SocketMessage sm;
    if (args.length > 0) {
      // munge arguments into a single string
      StringBuilder sbuff = new StringBuilder();
      for (int i = 0; i < args.length; i++) {
        sbuff.append(args[i]);
        sbuff.append(" ");
      }
      String arguments = sbuff.toString();
      System.out.println("ToolsUI arguments=" + arguments);

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

    } else { // no arguments were passed

      // look for messages from another ToolsUI
      sm = new SocketMessage(14444, null);
      if (sm.isAlreadyRunning()) {
        System.out.println("ToolsUI already running - start up another copy");
        sm = null;
      } else {
        sm.addEventListener(new SocketMessage.EventListener() {
          public void setMessage(SocketMessage.Event event) {
            wantDataset = event.getMessage();
            if (debugListen) System.out.println(" got message= '" + wantDataset);
            setDataset();
          }
        });
      }
    }
    ////////////////////////////////////////////////////////////////////////////////////////////////

    // spring initialization
    ApplicationContext springContext =
        new ClassPathXmlApplicationContext("classpath:resources/nj22/ui/spring/application-config.xml");

    // look for run line arguments
    boolean configRead = false;
    for (int i = 0; i < args.length; i++) {
      if (args[i].equalsIgnoreCase("-nj22Config") && (i < args.length - 1)) {
        String runtimeConfig = args[i + 1];
        i++;
        try {
          StringBuilder errlog = new StringBuilder();
          FileInputStream fis = new FileInputStream(runtimeConfig);
          RuntimeConfigParser.read(fis, errlog);
          configRead = true;
          System.out.println(errlog);
        } catch (IOException ioe) {
          System.out.println("Error reading " + runtimeConfig + "=" + ioe.getMessage());
        }
      }
    }

    if (!configRead) {
      String filename = ucar.util.prefs.XMLStore.makeStandardFilename(".unidata", "nj22Config.xml");
      File f = new File(filename);
      if (f.exists()) {
        try {
          StringBuilder errlog = new StringBuilder();
          FileInputStream fis = new FileInputStream(filename);
          RuntimeConfigParser.read(fis, errlog);
          configRead = true;
          System.out.println(errlog);
        } catch (IOException ioe) {
          System.out.println("Error reading " + filename + "=" + ioe.getMessage());
        }
      }
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

    // misc initializations
    BAMutil.setResourcePath("/resources/nj22/ui/icons/");

    // filesystem caching
    DiskCache2 cacheDir = new DiskCache2(".unidata/ehcache", true, -1, -1);
    cacheManager = thredds.filesystem.ControllerCaching.makeTestController(cacheDir.getRootDirectory());
    thredds.inventory.DatasetCollectionManager.setController(cacheManager);

    // for efficiency, persist aggregations. every hour, delete stuff older than 30 days
    Aggregation.setPersistenceCache(new DiskCache2("/.unidata/aggCache", true, 60 * 24 * 30, 60));
    DqcFactory.setPersistenceCache(new DiskCache2("/.unidata/dqc", true, 60 * 24 * 365, 60));

    // test
    // java.util.logging.Logger.getLogger("ucar.nc2").setLevel( java.util.logging.Level.SEVERE);

    // put UI in a JFrame
    frame = new JFrame("NetCDF (4.2) Tools");
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

    // open dap initializations
    ucar.nc2.dods.DODSNetcdfFile.setAllowCompression(true);
    ucar.nc2.dods.DODSNetcdfFile.setAllowSessions(true);

    // use HTTPClient - could use bean wiring here
    CredentialsProvider provider = new thredds.ui.UrlAuthenticatorDialog(frame);
    HttpClient client = HttpClientManager.init(provider, "ToolsUI");
    //opendap.dap.DConnect2.setHttpClient(client);
    HTTPRandomAccessFile.setHttpClient(client);
    CdmRemote.setHttpClient(client);
    NetcdfDataset.setHttpClient(client);
    WmsViewer.setHttpClient(client);

    // in case a dataset was on the command line
    if (wantDataset != null)
      setDataset();
  }
}