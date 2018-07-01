/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.ui;

import org.springframework.context.support.ClassPathXmlApplicationContext;
import thredds.client.catalog.ServiceType;
import thredds.client.catalog.tools.DataFactory;
import thredds.featurecollection.FeatureCollectionConfig;
import thredds.inventory.bdb.MetadataManager;
import thredds.ui.catalog.ThreddsUI;
import ucar.httpservices.HTTPException;
import ucar.httpservices.HTTPSession;
import ucar.nc2.*;
import ucar.nc2.constants.CDM;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.dataset.*;
import ucar.nc2.dt.GridDataset;
import ucar.nc2.dt.GridDatatype;
import ucar.nc2.dt.RadialDatasetSweep;
import ucar.nc2.ft.FeatureDataset;
import ucar.nc2.ft.FeatureDatasetFactoryManager;
import ucar.nc2.ft.FeatureDatasetPoint;
import ucar.nc2.ft.point.PointDatasetImpl;
import ucar.nc2.ft.point.writer.FeatureDatasetCapabilitiesWriter;
import ucar.nc2.ft2.coverage.*;
import ucar.nc2.geotiff.GeoTiff;
import ucar.nc2.grib.GribData;
import ucar.nc2.grib.GribIndexCache;
import ucar.nc2.grib.collection.GribCdmIndex;
import ucar.nc2.grib.grib1.tables.Grib1ParamTables;
import ucar.nc2.grib.grib2.table.WmoCodeTable;
import ucar.nc2.grib.grib2.table.WmoTemplateTable;
import ucar.nc2.iosp.bufr.tables.BufrTables;
import ucar.nc2.iosp.hdf4.H4header;
import ucar.nc2.iosp.hdf5.H5iosp;
import ucar.nc2.jni.netcdf.Nc4Iosp;
import ucar.nc2.ncml.Aggregation;
import ucar.nc2.stream.CdmRemote;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.time.CalendarDateUnit;
import ucar.nc2.ui.coverage2.CoverageViewer;
import ucar.nc2.ui.dialog.DiskCache2Form;
import ucar.nc2.ui.gis.shapefile.ShapeFileBean;
import ucar.nc2.ui.gis.worldmap.WorldMapBean;
import ucar.nc2.ui.grib.*;
import ucar.nc2.ui.grid.GeoGridTable;
import ucar.nc2.ui.grid.GridUI;
import ucar.nc2.ui.image.ImageViewPanel;
import ucar.nc2.ui.util.Resource;
import ucar.nc2.ui.util.SocketMessage;
import ucar.nc2.ui.widget.*;
import ucar.nc2.ui.widget.ProgressMonitor;
import ucar.nc2.units.*;
import ucar.nc2.util.*;
import ucar.nc2.util.cache.FileCache;
import ucar.nc2.util.xml.RuntimeConfigParser;
import ucar.util.prefs.PreferencesExt;
import ucar.util.prefs.XMLStore;
import ucar.util.prefs.ui.ComboBox;
import ucar.util.prefs.ui.Debug;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;
import java.awt.*;
import java.awt.event.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.*;
import java.nio.file.Paths;
import java.util.*;
import java.util.List;

/**
 * Netcdf Tools user interface.
 *
 * @author caron
 */
public class ToolsUI extends JPanel {
  static private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ToolsUI.class);

  static private final String WorldDetailMap = "/resources/nj22/ui/maps/Countries.shp";
  static private final String USMap = "/resources/nj22/ui/maps/us_state.shp";

  static private final String FRAME_SIZE = "FrameSize";
  static private final String DIALOG_VERSION = "5.0";
  static private final String GRIDVIEW_FRAME_SIZE = "GridUIWindowSize";
  static private final String GRIDIMAGE_FRAME_SIZE = "GridImageWindowSize";
  static private boolean debugListen = false;

  private PreferencesExt mainPrefs;

  // UI
  private AggPanel aggPanel;
  private BufrPanel bufrPanel;
  private BufrTableBPanel bufrTableBPanel;
  private BufrTableDPanel bufrTableDPanel;
  private ReportOpPanel bufrReportPanel;
  private BufrCdmIndexPanel bufrCdmIndexPanel;
  private BufrCodePanel bufrCodePanel;
  private CdmrFeature cdmremotePanel;
  private CdmIndexPanel cdmIndexPanel;
  private ReportOpPanel cdmIndexReportPanel;
  private CollectionSpecPanel fcPanel;
  private CoordSysPanel coordSysPanel;
  private CoveragePanel coveragePanel;
  private DatasetViewerPanel viewerPanel;
  private DatasetViewerPanel nc4viewer;
  private DatasetWriterPanel writerPanel;
  private DirectoryPartitionPanel dirPartPanel;
  private FeatureScanPanel ftPanel;
  private FmrcPanel fmrcPanel;
  private FmrcCollectionPanel fmrcCollectionPanel;
  private GeoGridPanel gridPanel;
  private GeotiffPanel geotiffPanel;
  private GribCodePanel gribCodePanel;
  private GribFilesPanel gribFilesPanel;
  private GribIndexPanel gribIdxPanel;
  private GribRenamePanel gribVariableRenamePanel;
  private GribRewritePanel gribRewritePanel;
  private GribTemplatePanel gribTemplatePanel;
  private Grib1CollectionPanel grib1CollectionPanel;
  private ReportOpPanel grib1ReportPanel;
  private Grib1TablePanel grib1TablePanel;
  private Grib2CollectionPanel grib2CollectionPanel;
  private Grib2TablePanel grib2TablePanel;
  private ReportOpPanel grib2ReportPanel;
  private Grib1DataPanel grib1DataPanel;
  private Grib2DataPanel grib2DataPanel;
  private Hdf5ObjectPanel hdf5ObjectPanel;
  private Hdf5DataPanel hdf5DataPanel;
  private Hdf4Panel hdf4Panel;
  private ImagePanel imagePanel;
  private NcStreamPanel ncStreamPanel;
  private NCdumpPanel ncdumpPanel;
  private NcmlEditorPanel ncmlEditorPanel;
  private PointFeaturePanel pointFeaturePanel;
  private StationRadialPanel stationRadialPanel;
  private RadialPanel radialPanel;
  private ThreddsUI threddsUI;
  private UnitsPanel unitsPanel;
  private URLDumpPane urlPanel;
  private WmoCCPanel wmoCommonCodePanel;
  private WmsPanel wmsPanel;

  private JTabbedPane tabbedPane;
  private JTabbedPane iospTabPane, bufrTabPane, gribTabPane, grib2TabPane, grib1TabPane, hdf5TabPane;
  private JTabbedPane ftTabPane, fcTabPane;
  private JTabbedPane fmrcTabPane;
  private JTabbedPane ncmlTabPane;

  private JFrame parentFrame;
  private FileManager fileChooser;
  private AboutWindow aboutWindow = null;

  // data
  private DataFactory threddsDataFactory = new DataFactory();
  private DateFormatter formatter = new DateFormatter();

  private boolean setUseRecordStructure = false;

  // debugging
  private JMenu debugFlagMenu;
  private DebugFlags debugFlags;
  private boolean debug = false, debugTab = false, debugCB = false;

  // Check if on a mac
  static private final String osName = System.getProperty("os.name").toLowerCase();
  static private final boolean isMacOs = osName.startsWith("mac os x");

  public ToolsUI(PreferencesExt prefs, JFrame parentFrame) {
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
    gribTabPane = new JTabbedPane(JTabbedPane.TOP);
    grib2TabPane = new JTabbedPane(JTabbedPane.TOP);
    grib1TabPane = new JTabbedPane(JTabbedPane.TOP);
    bufrTabPane = new JTabbedPane(JTabbedPane.TOP);
    ftTabPane = new JTabbedPane(JTabbedPane.TOP);
    fcTabPane = new JTabbedPane(JTabbedPane.TOP);
    fmrcTabPane = new JTabbedPane(JTabbedPane.TOP);
    hdf5TabPane = new JTabbedPane(JTabbedPane.TOP);
    ncmlTabPane = new JTabbedPane(JTabbedPane.TOP);

    // the widgets in the top level tabbed pane
    viewerPanel = new DatasetViewerPanel((PreferencesExt) mainPrefs.node("varTable"), false);
    tabbedPane.addTab("Viewer", viewerPanel);

    // all the other component are deferred construction for fast startup
    tabbedPane.addTab("Writer", new JLabel("Writer"));
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
    iospTabPane.addTab("BUFR", bufrTabPane);
    iospTabPane.addTab("GRIB", gribTabPane);
    iospTabPane.addTab("GRIB2", grib2TabPane);
    iospTabPane.addTab("GRIB1", grib1TabPane);
    iospTabPane.addTab("HDF5", hdf5TabPane);
    iospTabPane.addTab("HDF4", new JLabel("HDF4"));
    iospTabPane.addTab("NcStream", new JLabel("NcStream"));
    iospTabPane.addTab("CdmrFeature", new JLabel("CdmrFeature"));
    addListeners(iospTabPane);

    // nested-2 tab - bufr
    bufrTabPane.addTab("BUFR", new JLabel("BUFR"));
    bufrTabPane.addTab("BufrCdmIndex", new JLabel("BufrCdmIndex"));
    bufrTabPane.addTab("BUFRTableB", new JLabel("BUFRTableB"));
    bufrTabPane.addTab("BUFRTableD", new JLabel("BUFRTableD"));
    bufrTabPane.addTab("BUFR-CODES", new JLabel("BUFR-CODES"));
    bufrTabPane.addTab("BufrReports", new JLabel("BufrReports"));
    addListeners(bufrTabPane);

    // nested-2 tab - grib
    //gribTabPane.addTab("CdmIndex", new JLabel("CdmIndex"));
    gribTabPane.addTab("CdmIndex4", new JLabel("CdmIndex4"));
    gribTabPane.addTab("CdmIndexReport", new JLabel("CdmIndexReport"));
    gribTabPane.addTab("GribIndex", new JLabel("GribIndex"));
    gribTabPane.addTab("WMO-COMMON", new JLabel("WMO-COMMON"));
    gribTabPane.addTab("WMO-CODES", new JLabel("WMO-CODES"));
    gribTabPane.addTab("WMO-TEMPLATES", new JLabel("WMO-TEMPLATES"));
    gribTabPane.addTab("GRIB-Rename", new JLabel("GRIB-Rename"));
    gribTabPane.addTab("GRIB-Rewrite", new JLabel("GRIB-Rewrite"));
    addListeners(gribTabPane);

    // nested-2 tab - grib-2
    grib2TabPane.addTab("GRIB2collection", new JLabel("GRIB2collection"));
    grib2TabPane.addTab("GRIB2rectilyze", new JLabel("GRIB2rectilyze"));
    grib2TabPane.addTab("GRIB2-REPORT", new JLabel("GRIB2-REPORT"));
    grib2TabPane.addTab("GRIB2data", new JLabel("GRIB2data"));
    grib2TabPane.addTab("GRIB2-TABLES", new JLabel("GRIB2-TABLES"));
    addListeners(grib2TabPane);

    // nested-2 tab - grib-1
    grib1TabPane.addTab("GRIB1collection", new JLabel("GRIB1collection"));
    grib1TabPane.addTab("GRIB-FILES", new JLabel("GRIB-FILES"));
    grib1TabPane.addTab("GRIB1-REPORT", new JLabel("GRIB1-REPORT"));
    grib1TabPane.addTab("GRIB1data", new JLabel("GRIB1data"));
    grib1TabPane.addTab("GRIB1-TABLES", new JLabel("GRIB1-TABLES"));
    addListeners(grib1TabPane);

    // nested-2 tab - hdf5
    hdf5TabPane.addTab("HDF5-Objects", new JLabel("HDF5-Objects"));
    hdf5TabPane.addTab("HDF5-Data", new JLabel("HDF5-Data"));
    hdf5TabPane.addTab("Netcdf4-JNI", new JLabel("Netcdf4-JNI"));
    addListeners(hdf5TabPane);

    // nested tab - features
    ftTabPane.addTab("Grids", new JLabel("Grids"));
    ftTabPane.addTab("Coverages", new JLabel("Coverages"));
    ftTabPane.addTab("WMS", new JLabel("WMS"));
    ftTabPane.addTab("PointFeature", new JLabel("PointFeature"));
    ftTabPane.addTab("Images", new JLabel("Images"));
    ftTabPane.addTab("Radial", new JLabel("Radial"));
    ftTabPane.addTab("FeatureScan", new JLabel("FeatureScan"));
    ftTabPane.addTab("FeatureCollection", fcTabPane);
    addListeners(ftTabPane);

    // nested tab - feature collection
    fcTabPane.addTab("DirectoryPartition", new JLabel("DirectoryPartition"));
    // fcTabPane.addTab("PartitionReport", new JLabel("PartitionReport"));
    fcTabPane.addTab("CollectionSpec", new JLabel("CollectionSpec"));
    addListeners(fcTabPane);

    // nested tab - fmrc
    fmrcTabPane.addTab("Fmrc", new JLabel("Fmrc"));
    fmrcTabPane.addTab("Collections", new JLabel("Collections"));
    addListeners(fmrcTabPane);

    // nested tab - ncml
    ncmlTabPane.addTab("NcmlEditor", new JLabel("NcmlEditor"));
    ncmlTabPane.addTab("Aggregation", new JLabel("Aggregation"));
    addListeners(ncmlTabPane);

    // dynamic proxy for DebugFlags
    debugFlags = (DebugFlags) java.lang.reflect.Proxy.newProxyInstance(DebugFlags.class.getClassLoader(), new Class[]{DebugFlags.class}, new DebugProxyHandler());

    makeMenuBar();
    setDebugFlags();
  }

  private void addListeners(final JTabbedPane tabPane) {
    tabPane.addChangeListener(new ChangeListener() {
      public void stateChanged(ChangeEvent e) {
        Component c = tabPane.getSelectedComponent();
        if (c instanceof JLabel) {
          int idx = tabPane.getSelectedIndex();
          String title = tabPane.getTitleAt(idx);
          makeComponent(tabPane, title);
        }
      }
    });
    tabPane.addComponentListener(new ComponentAdapter() {
      public void componentShown(ComponentEvent e) {
        Component c = tabPane.getSelectedComponent();
        if (c instanceof JLabel) {
          int idx = tabPane.getSelectedIndex();
          String title = tabPane.getTitleAt(idx);
          makeComponent(tabPane, title);
        }
      }
    });
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
    switch (title) {
      case "Aggregation":
        aggPanel = new AggPanel((PreferencesExt) mainPrefs.node("NcMLAggregation"));
        c = aggPanel;
        break;

      case "BUFR":
        bufrPanel = new BufrPanel((PreferencesExt) mainPrefs.node("bufr"));
        c = bufrPanel;
        break;

      case "BUFRTableB":
        bufrTableBPanel = new BufrTableBPanel((PreferencesExt) mainPrefs.node("bufr2"));
        c = bufrTableBPanel;
        break;

      case "BUFRTableD":
        bufrTableDPanel = new BufrTableDPanel((PreferencesExt) mainPrefs.node("bufrD"));
        c = bufrTableDPanel;
        break;

      case "BufrReports": {
        PreferencesExt prefs = (PreferencesExt) mainPrefs.node("bufrReports");
        ReportPanel rp = new BufrReportPanel(prefs);
        bufrReportPanel = new ReportOpPanel(prefs, rp);
        c = bufrReportPanel;
        break;
      }
      case "BUFR-CODES":
        bufrCodePanel = new BufrCodePanel((PreferencesExt) mainPrefs.node("bufr-codes"));
        c = bufrCodePanel;
        break;

      case "CdmrFeature":
        cdmremotePanel = new CdmrFeature((PreferencesExt) mainPrefs.node("CdmrFeature"));
        c = cdmremotePanel;
        break;

      case "CollectionSpec":
        fcPanel = new CollectionSpecPanel((PreferencesExt) mainPrefs.node("collSpec"));
        c = fcPanel;
        break;

      case "DirectoryPartition":
        dirPartPanel = new DirectoryPartitionPanel((PreferencesExt) mainPrefs.node("dirPartition"));
        c = dirPartPanel;
        break;

      case "NcStream":
        ncStreamPanel = new NcStreamPanel((PreferencesExt) mainPrefs.node("NcStream"));
        c = ncStreamPanel;
        break;

      case "GRIB1collection":
        grib1CollectionPanel = new Grib1CollectionPanel((PreferencesExt) mainPrefs.node("grib1raw"));
        c = grib1CollectionPanel;
        break;

      case "GRIB1data":
        grib1DataPanel = new Grib1DataPanel((PreferencesExt) mainPrefs.node("grib1Data"));
        c = grib1DataPanel;
        break;

      case "GRIB-FILES":
        gribFilesPanel = new GribFilesPanel((PreferencesExt) mainPrefs.node("gribFiles"));
        c = gribFilesPanel;
        break;

      case "GRIB2collection":
        grib2CollectionPanel = new Grib2CollectionPanel((PreferencesExt) mainPrefs.node("gribNew"));
        c = grib2CollectionPanel;
        break;

      case "GRIB2data":
        grib2DataPanel = new Grib2DataPanel((PreferencesExt) mainPrefs.node("grib2Data"));
        c = grib2DataPanel;
        break;

      case "BufrCdmIndex":
        bufrCdmIndexPanel = new BufrCdmIndexPanel((PreferencesExt) mainPrefs.node("bufrCdmIdx"));
        c = bufrCdmIndexPanel;
    /* } else if (title.equals("CdmIndex")) {
      gribCdmIndexPanel = new GribCdmIndexPanel((PreferencesExt) mainPrefs.node("cdmIdx"));
      c = gribCdmIndexPanel; */
        break;

      case "CdmIndex4":
        cdmIndexPanel = new CdmIndexPanel((PreferencesExt) mainPrefs.node("cdmIdx3"));
        c = cdmIndexPanel;
        break;

      case "CdmIndexReport": {
        PreferencesExt prefs = (PreferencesExt) mainPrefs.node("CdmIndexReport");
        ReportPanel rp = new CdmIndexReportPanel(prefs);
        cdmIndexReportPanel = new ReportOpPanel(prefs, rp);
        c = cdmIndexReportPanel;
        break;
      }

      case "GribIndex":
        gribIdxPanel = new GribIndexPanel((PreferencesExt) mainPrefs.node("gribIdx"));
        c = gribIdxPanel;
        break;

      case "GRIB1-REPORT": {
        PreferencesExt prefs = (PreferencesExt) mainPrefs.node("grib1Report");
        ReportPanel rp = new Grib1ReportPanel(prefs);
        grib1ReportPanel = new ReportOpPanel(prefs, rp);
        c = grib1ReportPanel;
        break;
      }

      case "GRIB2-REPORT": {
        PreferencesExt prefs = (PreferencesExt) mainPrefs.node("gribReport");
        ReportPanel rp = new Grib2ReportPanel(prefs);
        grib2ReportPanel = new ReportOpPanel(prefs, rp);
        c = grib2ReportPanel;
        break;
      }

      case "WMO-COMMON":
        wmoCommonCodePanel = new WmoCCPanel((PreferencesExt) mainPrefs.node("wmo-common"));
        c = wmoCommonCodePanel;
        break;

      case "WMO-CODES":
        gribCodePanel = new GribCodePanel((PreferencesExt) mainPrefs.node("wmo-codes"));
        c = gribCodePanel;
        break;

      case "WMO-TEMPLATES":
        gribTemplatePanel = new GribTemplatePanel((PreferencesExt) mainPrefs.node("wmo-templates"));
        c = gribTemplatePanel;

        break;
      case "GRIB1-TABLES":
        grib1TablePanel = new Grib1TablePanel((PreferencesExt) mainPrefs.node("grib1-tables"));
        c = grib1TablePanel;
        break;

      case "GRIB2-TABLES":
        grib2TablePanel = new Grib2TablePanel((PreferencesExt) mainPrefs.node("grib2-tables"));
        c = grib2TablePanel;
        break;

      case "GRIB-Rename":
        gribVariableRenamePanel = new GribRenamePanel((PreferencesExt) mainPrefs.node("grib-rename"));
        c = gribVariableRenamePanel;
        break;

      case "GRIB-Rewrite":
        gribRewritePanel = new GribRewritePanel((PreferencesExt) mainPrefs.node("grib-rewrite"));
        c = gribRewritePanel;
        break;

      case "CoordSys":
        coordSysPanel = new CoordSysPanel((PreferencesExt) mainPrefs.node("CoordSys"));
        c = coordSysPanel;
        break;

      case "FeatureScan":
        ftPanel = new FeatureScanPanel((PreferencesExt) mainPrefs.node("ftPanel"));
        c = ftPanel;
        break;

      case "GeoTiff":
        geotiffPanel = new GeotiffPanel((PreferencesExt) mainPrefs.node("WCS"));
        c = geotiffPanel;
        break;

      case "Grids":
        gridPanel = new GeoGridPanel((PreferencesExt) mainPrefs.node("grid"));
        c = gridPanel;
        break;

      case "Coverages":
        coveragePanel = new CoveragePanel((PreferencesExt) mainPrefs.node("coverage2"));
        c = coveragePanel;
        break;

      case "HDF5-Objects":
        hdf5ObjectPanel = new Hdf5ObjectPanel((PreferencesExt) mainPrefs.node("hdf5"));
        c = hdf5ObjectPanel;
        break;

      case "HDF5-Data":
        hdf5DataPanel = new Hdf5DataPanel((PreferencesExt) mainPrefs.node("hdf5data"));
        c = hdf5DataPanel;
        break;

      case "Netcdf4-JNI":
        nc4viewer = new DatasetViewerPanel((PreferencesExt) mainPrefs.node("nc4viewer"), true);
        c = nc4viewer;
        break;

      case "HDF4":
        hdf4Panel = new Hdf4Panel((PreferencesExt) mainPrefs.node("hdf4"));
        c = hdf4Panel;
        break;

      case "Images":
        imagePanel = new ImagePanel((PreferencesExt) mainPrefs.node("images"));
        c = imagePanel;
        break;

      case "Fmrc":
        fmrcPanel = new FmrcPanel((PreferencesExt) mainPrefs.node("fmrc2"));
        c = fmrcPanel;
        break;

      case "Collections":
        fmrcCollectionPanel = new FmrcCollectionPanel((PreferencesExt) mainPrefs.node("collections"));
        c = fmrcCollectionPanel;
        break;

      case "NCDump":
        ncdumpPanel = new NCdumpPanel((PreferencesExt) mainPrefs.node("NCDump"));
        c = ncdumpPanel;
        break;

      case "NcmlEditor":
        ncmlEditorPanel = new NcmlEditorPanel((PreferencesExt) mainPrefs.node("NcmlEditor"));
        c = ncmlEditorPanel;
        break;

      case "PointFeature":
        pointFeaturePanel = new PointFeaturePanel((PreferencesExt) mainPrefs.node("pointFeature"));
        c = pointFeaturePanel;
        break;

      case "Radial":
        radialPanel = new RadialPanel((PreferencesExt) mainPrefs.node("radial"));
        c = radialPanel;
        break;

      case "StationRadial":
        stationRadialPanel = new StationRadialPanel((PreferencesExt) mainPrefs.node("stationRadar"));
        c = stationRadialPanel;
        break;

      case "THREDDS":
        threddsUI = new ThreddsUI(ToolsUI.this.parentFrame, (PreferencesExt) mainPrefs.node("thredds"));
        threddsUI.addPropertyChangeListener(new PropertyChangeListener() {
          public void propertyChange(PropertyChangeEvent e) {
            if (e.getPropertyName().equals("InvAccess")) {
              thredds.client.catalog.Access access = (thredds.client.catalog.Access) e.getNewValue();
              jumptoThreddsDatatype(access);
            }
            if (e.getPropertyName().equals("Dataset") || e.getPropertyName().equals("CoordSys") || e.getPropertyName().equals("File")) {
              thredds.client.catalog.Dataset ds = (thredds.client.catalog.Dataset) e.getNewValue();
              setThreddsDatatype(ds, e.getPropertyName());
            }
          }
        });

        c = threddsUI;
        break;

      case "Units":
        unitsPanel = new UnitsPanel((PreferencesExt) mainPrefs.node("units"));
        c = unitsPanel;
        break;

      case "URLdump":
        urlPanel = new URLDumpPane((PreferencesExt) mainPrefs.node("urlDump"));
        c = urlPanel;
        break;

      case "Viewer":
        c = viewerPanel;
        break;

      case "Writer":
        writerPanel = new DatasetWriterPanel((PreferencesExt) mainPrefs.node("writer"));
        c = writerPanel;
        break;

      case "WMS":
        wmsPanel = new WmsPanel((PreferencesExt) mainPrefs.node("wms"));
        c = wmsPanel;
        break;

      default:
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
        // IGNORE HttpClientManager.clearState();
      }
    };
    BAMutil.setActionProperties(clearHttpStateAction, null, "Clear Http State", false, 'S', -1);
    BAMutil.addActionToMenu(sysMenu, clearHttpStateAction);

    AbstractAction showCacheAction = new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        Formatter f = new Formatter();
        f.format("RandomAccessFileCache contents%n");
        ucar.nc2.util.cache.FileCacheIF rafCache = ucar.unidata.io.RandomAccessFile.getGlobalFileCache();
        if (null != rafCache)
          rafCache.showCache(f);
        f.format("%nNetcdfFileCache contents%n");
        ucar.nc2.util.cache.FileCacheIF cache = NetcdfDataset.getNetcdfFileCache();
        if (null != cache)
          cache.showCache(f);
        viewerPanel.detailTA.setText(f.toString());
        viewerPanel.detailWindow.show();
      }
    };
    BAMutil.setActionProperties(showCacheAction, null, "Show Caches", false, 'S', -1);
    BAMutil.addActionToMenu(sysMenu, showCacheAction);

    AbstractAction clearRafCacheAction = new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        ucar.nc2.util.cache.FileCacheIF rafCache = ucar.unidata.io.RandomAccessFile.getGlobalFileCache();
        if (rafCache != null)
          rafCache.clearCache(true);
      }
    };
    BAMutil.setActionProperties(clearRafCacheAction, null, "Clear RandomAccessFileCache", false, 'C', -1);
    BAMutil.addActionToMenu(sysMenu, clearRafCacheAction);

    AbstractAction clearCacheAction = new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        ucar.nc2.util.cache.FileCacheIF cache = NetcdfDataset.getNetcdfFileCache();
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
          ucar.nc2.util.cache.FileCacheIF cache = NetcdfDataset.getNetcdfFileCache();
          if (cache != null)
            cache.enable();
          else
            NetcdfDataset.initNetcdfFileCache(10, 20, 10 * 60);
        } else {
          ucar.nc2.util.cache.FileCacheIF cache = NetcdfDataset.getNetcdfFileCache();
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
        new MySplashScreen();
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
    CdmRemote.setDebugFlags(debugFlags);
    Nc4Iosp.setDebugFlags(debugFlags);
    DataFactory.setDebugFlags(debugFlags);

    ucar.nc2.FileWriter2.setDebugFlags(debugFlags);
    ucar.nc2.ft.point.standard.PointDatasetStandardFactory.setDebugFlags(debugFlags);
    ucar.nc2.grib.collection.Grib.setDebugFlags(debugFlags);
  }

  private void makeModesMenu(JMenu modeMenu) {
    AbstractAction a;

    JMenu ncMenu = new JMenu("NetcdfFile");
    modeMenu.add(ncMenu);

    /////////////////////////////////////
    a = new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        setUseRecordStructure = (Boolean) getValue(BAMutil.STATE);
      }
    };
    BAMutil.setActionPropertiesToggle(a, null, "nc3UseRecords", setUseRecordStructure, 'V', -1);
    BAMutil.addActionToMenu(ncMenu, a);

    /////////////////////////////////////
    JMenu dsMenu = new JMenu("NetcdfDataset");
    modeMenu.add(dsMenu);

    a = new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        Boolean state = (Boolean) getValue(BAMutil.STATE);
        CoordSysBuilder.setUseMaximalCoordSys(state);
      }
    };
    BAMutil.setActionPropertiesToggle(a, null, "set Use Maximal CoordSystem", CoordSysBuilder.getUseMaximalCoordSys(), 'N', -1);
    BAMutil.addActionToMenu(dsMenu, a);

    a = new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        Boolean state = (Boolean) getValue(BAMutil.STATE);
        NetcdfDataset.setFillValueIsMissing(state);
      }
    };
    BAMutil.setActionPropertiesToggle(a, null, "use _FillValue attribute for missing values",
            NetcdfDataset.getFillValueIsMissing(), 'F', -1);
    BAMutil.addActionToMenu(dsMenu, a);

    a = new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        Boolean state = (Boolean) getValue(BAMutil.STATE);
        NetcdfDataset.setInvalidDataIsMissing(state);
      }
    };
    BAMutil.setActionPropertiesToggle(a, null, "use valid_range attribute for missing values",
            NetcdfDataset.getInvalidDataIsMissing(), 'V', -1);
    BAMutil.addActionToMenu(dsMenu, a);

    a = new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        Boolean state = (Boolean) getValue(BAMutil.STATE);
        NetcdfDataset.setMissingDataIsMissing(state);
      }
    };
    BAMutil.setActionPropertiesToggle(a, null, "use missing_value attribute for missing values",
            NetcdfDataset.getMissingDataIsMissing(), 'M', -1);
    BAMutil.addActionToMenu(dsMenu, a);

    /////////////////////////////////////
    JMenu subMenu = new JMenu("HDF-EOS");
    modeMenu.add(subMenu);
    a = new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        Boolean state = (Boolean) getValue(BAMutil.STATE);
        H5iosp.useHdfEos(state);
      }
    };
    a.putValue(BAMutil.STATE, true);
    BAMutil.setActionProperties(a, null, "use HDF-EOS StructMetadata to augment HDF5", true, '5', -1);
    BAMutil.addActionToMenu(subMenu, a);

    a = new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        Boolean state = (Boolean) getValue(BAMutil.STATE);
        H4header.useHdfEos(state);
      }
    };
    a.putValue(BAMutil.STATE, true);
    BAMutil.setActionProperties(a, null, "use HDF-EOS StructMetadata to augment HDF4", true, '4', -1);
    BAMutil.addActionToMenu(subMenu, a);

    a = new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        Boolean state = (Boolean) getValue(BAMutil.STATE);
        Nc4Iosp.useHdfEos(state);
      }
    };
    a.putValue(BAMutil.STATE, true);
    BAMutil.setActionProperties(a, null, "use HDF-EOS StructMetadata to augment netcdf4 (JNI)", true, 'N', -1);
    BAMutil.addActionToMenu(subMenu, a);

    /////////////////////////////////////
    subMenu = new JMenu("GRIB");
    modeMenu.add(subMenu);
    a = new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        setGribDiskCache();
      }
    };
    BAMutil.setActionProperties(a, null, "set Grib disk cache...", false, 'G', -1);
    BAMutil.addActionToMenu(subMenu, a);

    a = new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        Boolean state = (Boolean) getValue(BAMutil.STATE);
        Grib1ParamTables.setStrict(state);
      }
    };
    boolean strictMode = Grib1ParamTables.isStrict();
    a.putValue(BAMutil.STATE, strictMode);
    BAMutil.setActionPropertiesToggle(a, null, "GRIB1 strict", strictMode, 'S', -1);
    BAMutil.addActionToMenu(subMenu, a);

    a = new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        Boolean state = (Boolean) getValue(BAMutil.STATE);
        GribData.setInterpolationMethod(state ? GribData.InterpolationMethod.cubic : GribData.InterpolationMethod.linear);
      }
    };
    boolean useCubic = GribData.getInterpolationMethod() == GribData.InterpolationMethod.cubic;
    a.putValue(BAMutil.STATE, useCubic);
    BAMutil.setActionPropertiesToggle(a, null, "Use Cubic Interpolation on Thin Grids", useCubic, 'I', -1);
    BAMutil.addActionToMenu(subMenu, a);

    //static public boolean useGenTypeDef = false, useTableVersionDef = true, intvMergeDef = true, useCenterDef = true;

    a = new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        FeatureCollectionConfig.useGenTypeDef = (Boolean) getValue(BAMutil.STATE);
      }
    };
    a.putValue(BAMutil.STATE, FeatureCollectionConfig.useGenTypeDef);
    BAMutil.setActionPropertiesToggle(a, null, "useGenType", FeatureCollectionConfig.useGenTypeDef, 'S', -1);
    BAMutil.addActionToMenu(subMenu, a);

    a = new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        FeatureCollectionConfig.useTableVersionDef = (Boolean) getValue(BAMutil.STATE);
      }
    };
    a.putValue(BAMutil.STATE, FeatureCollectionConfig.useTableVersionDef);
    BAMutil.setActionPropertiesToggle(a, null, "useTableVersion", FeatureCollectionConfig.useTableVersionDef, 'S', -1);
    BAMutil.addActionToMenu(subMenu, a);

    a = new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        FeatureCollectionConfig.intvMergeDef = (Boolean) getValue(BAMutil.STATE);
      }
    };
    a.putValue(BAMutil.STATE, FeatureCollectionConfig.intvMergeDef);
    BAMutil.setActionPropertiesToggle(a, null, "intvMerge", FeatureCollectionConfig.intvMergeDef, 'S', -1);
    BAMutil.addActionToMenu(subMenu, a);

    a = new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        FeatureCollectionConfig.useCenterDef = (Boolean) getValue(BAMutil.STATE);
      }
    };
    a.putValue(BAMutil.STATE, FeatureCollectionConfig.useCenterDef);
    BAMutil.setActionPropertiesToggle(a, null, "useCenter", FeatureCollectionConfig.useCenterDef, 'S', -1);
    BAMutil.addActionToMenu(subMenu, a);

    /////////////////////////////////////
    subMenu = new JMenu("FMRC");
    modeMenu.add(subMenu);

    a = new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        Boolean state = (Boolean) getValue(BAMutil.STATE);
        FeatureCollectionConfig.setRegularizeDefault(state);
      }
    };
    // ToolsUI default is to regularize the FMRC
    FeatureCollectionConfig.setRegularizeDefault(true);
    a.putValue(BAMutil.STATE, true);
    BAMutil.setActionPropertiesToggle(a, null, "regularize", true, 'R', -1);
    BAMutil.addActionToMenu(subMenu, a);

    a = new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        Boolean state = (Boolean) getValue(BAMutil.STATE);
        DataFactory.setPreferCdm(state);
      }
    };
    // ToolsUI default is to use cdmRemote access
    DataFactory.setPreferCdm(true);
    a.putValue(BAMutil.STATE, true);
    BAMutil.setActionPropertiesToggle(a, null, "preferCdm", true, 'P', -1);
    BAMutil.addActionToMenu(subMenu, a);
  }

  DiskCache2Form diskCache2Form = null;

  private void setGribDiskCache() {
    if (diskCache2Form == null) {
      diskCache2Form = new DiskCache2Form(parentFrame, GribIndexCache.getDiskCache2());
    }
    diskCache2Form.setVisible(true);
  }

  public void save() {
    fileChooser.save();
    if (aggPanel != null) aggPanel.save();
    if (bufrFileChooser != null) bufrFileChooser.save();
    if (bufrPanel != null) bufrPanel.save();
    if (bufrTableBPanel != null) bufrTableBPanel.save();
    if (bufrTableDPanel != null) bufrTableDPanel.save();
    if (bufrReportPanel != null) bufrReportPanel.save();
    if (bufrCodePanel != null) bufrCodePanel.save();
    if (coordSysPanel != null) coordSysPanel.save();
    if (coveragePanel != null) coveragePanel.save();
    if (cdmIndexPanel != null) cdmIndexPanel.save();
    if (cdmIndexReportPanel != null) cdmIndexReportPanel.save();
    if (cdmremotePanel != null) cdmremotePanel.save();
    if (dirPartPanel != null) dirPartPanel.save();
    if (bufrCdmIndexPanel != null) bufrCdmIndexPanel.save();
    //if (gribCdmIndexPanel != null) gribCdmIndexPanel.save();
    if (fmrcCollectionPanel != null) fmrcCollectionPanel.save();
    if (fcPanel != null) fcPanel.save();
    if (ftPanel != null) ftPanel.save();
    if (fmrcPanel != null) fmrcPanel.save();
    if (geotiffPanel != null) geotiffPanel.save();
    if (gribFilesPanel != null) gribFilesPanel.save();
    if (grib2CollectionPanel != null) grib2CollectionPanel.save();
    // if (grib2RectilyzePanel != null) grib2RectilyzePanel.save();
    if (grib2DataPanel != null) grib2DataPanel.save();
    if (grib1DataPanel != null) grib1DataPanel.save();
    if (gribCodePanel != null) gribCodePanel.save();
    if (gribIdxPanel != null) gribIdxPanel.save();
    if (gribTemplatePanel != null) gribTemplatePanel.save();
    if (grib1CollectionPanel != null) grib1CollectionPanel.save();
    if (grib1ReportPanel != null) grib1ReportPanel.save();
    if (grib2ReportPanel != null) grib2ReportPanel.save();
    if (grib1TablePanel != null) grib1TablePanel.save();
    if (grib2TablePanel != null) grib2TablePanel.save();
    if (gribVariableRenamePanel != null) gribVariableRenamePanel.save();
    if (gribRewritePanel != null) gribRewritePanel.save();
    if (gridPanel != null) gridPanel.save();
    if (hdf5ObjectPanel != null) hdf5ObjectPanel.save();
    if (hdf5DataPanel != null) hdf5DataPanel.save();
    if (hdf4Panel != null) hdf4Panel.save();
    if (imagePanel != null) imagePanel.save();
    if (ncdumpPanel != null) ncdumpPanel.save();
    if (ncStreamPanel != null) ncStreamPanel.save();
    if (nc4viewer != null) nc4viewer.save();
    if (ncmlEditorPanel != null) ncmlEditorPanel.save();
    if (pointFeaturePanel != null) pointFeaturePanel.save();
    //if (pointObsPanel != null) pointObsPanel.save();
    if (radialPanel != null) radialPanel.save();
    // if (stationObsPanel != null) stationObsPanel.save();
    if (stationRadialPanel != null) stationRadialPanel.save();
    // if (trajTablePanel != null) trajTablePanel.save();
    if (threddsUI != null) threddsUI.storePersistentData();
    if (unitsPanel != null) unitsPanel.save();
    if (urlPanel != null) urlPanel.save();
    if (viewerPanel != null) viewerPanel.save();
    if (writerPanel != null) writerPanel.save();
    if (wmoCommonCodePanel != null) wmoCommonCodePanel.save();
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
    pointFeaturePanel.setPointFeatureDataset(FeatureType.ANY_POINT, datasetName);
    tabbedPane.setSelectedComponent(ftTabPane);
    ftTabPane.setSelectedComponent(pointFeaturePanel);
  }

  private void openGrib1Collection(String collection) {
    makeComponent(grib1TabPane, "GRIB1collection");  // LOOK - does this aleays make component ?
    grib1CollectionPanel.setCollection(collection);
    tabbedPane.setSelectedComponent(iospTabPane);
    iospTabPane.setSelectedComponent(grib1TabPane);
    grib1TabPane.setSelectedComponent(grib1CollectionPanel);
  }

  private void openGrib2Collection(String collection) {
    makeComponent(grib2TabPane, "GRIB2collection");
    grib2CollectionPanel.setCollection(collection);
    tabbedPane.setSelectedComponent(iospTabPane);
    iospTabPane.setSelectedComponent(grib2TabPane);
    grib2TabPane.setSelectedComponent(grib2CollectionPanel);
  }

  private void openGrib2Data(String datasetName) {
    makeComponent(grib2TabPane, "GRIB2data");
    grib2DataPanel.doit(datasetName);
    tabbedPane.setSelectedComponent(iospTabPane);
    iospTabPane.setSelectedComponent(grib2TabPane);
    grib2TabPane.setSelectedComponent(grib2DataPanel);
  }

  private void openGrib1Data(String datasetName) {
    makeComponent(grib1TabPane, "GRIB1data");
    grib1DataPanel.doit(datasetName);
    tabbedPane.setSelectedComponent(iospTabPane);
    iospTabPane.setSelectedComponent(grib1TabPane);
    grib1TabPane.setSelectedComponent(grib1DataPanel);
  }

  private void openGridDataset(String datasetName) {
    makeComponent(ftTabPane, "Grids");
    gridPanel.doit(datasetName);
    tabbedPane.setSelectedComponent(ftTabPane);
    ftTabPane.setSelectedComponent(gridPanel);
  }

  private void openCoverageDataset(String datasetName) {
    makeComponent(ftTabPane, "Coverages");
    coveragePanel.doit(datasetName);
    tabbedPane.setSelectedComponent(ftTabPane);
    ftTabPane.setSelectedComponent(coveragePanel);
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

  private void setThreddsDatatype(thredds.client.catalog.Dataset invDataset, String wants) {
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
      DataFactory.Result threddsData = threddsDataFactory.openFeatureDataset(invDataset, null);
      if (threddsData.fatalError) {
        JOptionPane.showMessageDialog(null, "Failed to open err="+threddsData.errLog);
        return;
      }
      jumptoThreddsDatatype(threddsData);

    } catch (IOException ioe) {
      JOptionPane.showMessageDialog(null, "Error on setThreddsDatatype = " + ioe.getMessage());
      ioe.printStackTrace();
    }

  }

  // jump to the appropriate tab based on datatype of InvAccess
  private void jumptoThreddsDatatype(thredds.client.catalog.Access invAccess) {
    if (invAccess == null) return;

    thredds.client.catalog.Service s = invAccess.getService();
    if (s.getType() == thredds.client.catalog.ServiceType.HTTPServer) {
      downloadFile(invAccess.getStandardUrlName());
      return;
    }

    if (s.getType() == thredds.client.catalog.ServiceType.WMS) {
      openWMSDataset(invAccess.getStandardUrlName());
      return;
    }

    if (s.getType() == ServiceType.CdmrFeature) {
      openCoverageDataset(invAccess.getWrappedUrlName());
      return;
    }

    thredds.client.catalog.Dataset ds = invAccess.getDataset();
    if (ds.getFeatureType() == null) {
      // if no feature type, just open as a NetcdfDataset
      try {
        openNetcdfFile(threddsDataFactory.openDataset(invAccess, true, null, null));
      } catch (IOException ioe) {
        JOptionPane.showMessageDialog(null, "Error on setThreddsDatatype = " + ioe.getMessage());
      }
      return;
    }

    DataFactory.Result threddsData = null;
    try {
      threddsData = threddsDataFactory.openFeatureDataset(invAccess, null);
      if (threddsData.fatalError) {
        JOptionPane.showMessageDialog(null, "Failed to open err="+threddsData.errLog);
        return;
      }
      jumptoThreddsDatatype(threddsData);

    } catch (IOException ioe) {
      ioe.printStackTrace();
      JOptionPane.showMessageDialog(null, "Error on setThreddsDatatype = " + ioe.getMessage());
      if (threddsData != null) {
        try {
          threddsData.close();
        } catch (IOException ioe2) {
        }
      }
    }

  }

  // jump to the appropriate tab based on datatype of threddsData
  private void jumptoThreddsDatatype(DataFactory.Result threddsData) {

    if (threddsData.fatalError) {
      JOptionPane.showMessageDialog(this, "Cant open dataset=" + threddsData.errLog);
      try {
        threddsData.close();
      } catch (IOException e) {
        e.printStackTrace();
      }
      return;
    }

    if (threddsData.featureType.isCoverageFeatureType()) {
      if (threddsData.featureDataset instanceof FeatureDatasetCoverage) {
        makeComponent(ftTabPane, "Coverages");
        coveragePanel.setDataset(threddsData.featureDataset);
        tabbedPane.setSelectedComponent(ftTabPane);
        ftTabPane.setSelectedComponent(coveragePanel);

      } else if (threddsData.featureDataset instanceof GridDataset) {
        makeComponent(ftTabPane, "Grids");
        gridPanel.setDataset((GridDataset) threddsData.featureDataset);
        tabbedPane.setSelectedComponent(ftTabPane);
        ftTabPane.setSelectedComponent(gridPanel);
      }

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
      stationRadialPanel.setStationRadialDataset(threddsData.featureDataset);
      tabbedPane.setSelectedComponent(ftTabPane);
      ftTabPane.setSelectedComponent(stationRadialPanel);
    }
  }


  private NetcdfFile openFile(String location, boolean addCoords, CancelTask task) {


    NetcdfFile ncfile = null;
    try {
      DatasetUrl durl = DatasetUrl.findDatasetUrl(location);
      if (addCoords)
        ncfile = NetcdfDataset.acquireDataset(durl, true, task);
      else
        ncfile = NetcdfDataset.acquireFile(durl, task);

      if (ncfile == null)
        JOptionPane.showMessageDialog(null, "NetcdfDataset.open cant open " + location);
      else if (setUseRecordStructure)
        ncfile.sendIospMessage(NetcdfFile.IOSP_MESSAGE_ADD_RECORD_STRUCTURE);

    } catch (IOException ioe) {
      String message = ioe.getMessage();
      if ((null == message) && (ioe instanceof EOFException))
        message = "Premature End of File";
      JOptionPane.showMessageDialog(null, "NetcdfDataset.open cant open " + location + "\n" + message);
      if (!(ioe instanceof FileNotFoundException))
        ioe.printStackTrace();

      ncfile = null;

    } catch (Exception e) {
      JOptionPane.showMessageDialog(null, "NetcdfDataset.open cant open " + location + "\n" + e.getMessage());
      log.error("NetcdfDataset.open cant open " + location, e);
      e.printStackTrace();

      try {
        if (ncfile != null) ncfile.close();
      } catch (IOException ee) {
        System.out.printf("close failed%n");
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
        BufferedOutputStream out;

        try (FileOutputStream fos = new FileOutputStream(values[0])) {
          out = new BufferedOutputStream(fos, 60000);
          IO.copyUrlB(values[1], out, 60000);
          downloadStatus = values[1] + " written to " + values[0];

        } catch (IOException ioe) {
          downloadStatus = "Error opening " + values[0] + " and reading " + values[1] + "\n" + ioe.getMessage();
        }
      }
    };

    GetDataTask task = new GetDataTask(runner, urlString, values);
    ProgressMonitor pm = new ProgressMonitor(task);
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
    ComboBox cb;
    JPanel buttPanel, topPanel;
    AbstractButton coordButt = null;
    StopButton stopButton;

    boolean addCoords, busy;
    long lastEvent = -1;
    boolean eventOK = true;

    IndependentWindow detailWindow;
    TextHistoryPane detailTA;

    OpPanel(PreferencesExt prefs, String command) {
      this(prefs, command, true, true);
    }

    OpPanel(PreferencesExt prefs, String command, boolean addFileButton, boolean addCoordButton) {
      this(prefs, command, true, addFileButton, addCoordButton);
    }

    OpPanel(PreferencesExt prefs, String command, boolean addComboBox, boolean addFileButton, boolean addCoordButton) {
      this.prefs = prefs;
      buttPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));

      cb = new ComboBox(prefs);
      cb.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          if (debugCB)
            System.out.println(" doit " + cb.getSelectedItem() + " cmd=" + e.getActionCommand() + " when=" + e.getWhen() + " class=" + OpPanel.this.getClass().getName());

          // eliminate multiple events from same selection by ignoring events occurring within 100ms of last one.
          if (eventOK && (e.getWhen() > lastEvent + 100)) {
            doit(cb.getSelectedItem());
            lastEvent = e.getWhen();
          }
        }
      });

      AbstractAction closeAction = new AbstractAction() {
        public void actionPerformed(ActionEvent e) {
          try {
            closeOpenFiles();
          } catch (IOException e1) {
            System.out.printf("close failed");
          }
        }
      };
      BAMutil.setActionProperties(closeAction, "Close", "release files", false, 'L', -1);
      BAMutil.addActionToContainer(buttPanel, closeAction);

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

      if (addCoordButton) {
        AbstractAction coordAction = new AbstractAction() {
          public void actionPerformed(ActionEvent e) {
            addCoords = (Boolean) getValue(BAMutil.STATE);
            String tooltip = addCoords ? "add Coordinates is ON" : "add Coordinates is OFF";
            coordButt.setToolTipText(tooltip);
            //doit( cb.getSelectedItem()); // called from cb action listener
          }
        };
        addCoords = prefs.getBoolean("coordState", false);
        String tooltip2 = addCoords ? "add Coordinates is ON" : "add Coordinates is OFF";
        BAMutil.setActionProperties(coordAction, "addCoords", tooltip2, true, 'C', -1);
        coordAction.putValue(BAMutil.STATE, Boolean.valueOf(addCoords));
        coordButt = BAMutil.addActionToContainer(buttPanel, coordAction);
      }

      if (this instanceof GetDataRunnable) {
        stopButton = new StopButton("Stop");
        buttPanel.add(stopButton);
      }

      topPanel = new JPanel(new BorderLayout());
      if (addComboBox) {
        topPanel.add(new JLabel(command), BorderLayout.WEST);
        topPanel.add(cb, BorderLayout.CENTER);
        topPanel.add(buttPanel, BorderLayout.EAST);
      } else {
        topPanel.add(buttPanel, BorderLayout.EAST);
      }

      setLayout(new BorderLayout());
      add(topPanel, BorderLayout.NORTH);

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
        setSelectedItem(command);
      }
      busy = false;
    }

    abstract boolean process(Object command);

    void closeOpenFiles() throws IOException {
    }

    void save() {
      cb.save();
      if (coordButt != null) prefs.putBoolean("coordState", coordButt.getModel().isSelected());
      if (detailWindow != null) prefs.putBeanObject(FRAME_SIZE, detailWindow.getBounds());
    }

    void setSelectedItem(Object item) {
      eventOK = false;
      cb.addItem(item);
      eventOK = true;
    }
  }

  private class NCdumpPanel extends OpPanel implements GetDataRunnable {
    private GetDataTask task;
    NetcdfFile ncfile = null;
    String filename = null;
    String command = null;
    String result;
    TextHistoryPane ta;

    NCdumpPanel(PreferencesExt prefs) {
      super(prefs, "command:");

      ta = new TextHistoryPane(true);
      add(ta, BorderLayout.CENTER);

      stopButton.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          if (task.isSuccess())
            ta.setText(result);
          else
            ta.setText(task.errMsg);

          if (task.isCancel())
            ta.appendLine("\n***Cancelled by User");

          ta.gotoTop();
        }
      });
    }

    void closeOpenFiles() throws IOException {
      if (ncfile != null) ncfile.close();
      ncfile = null;
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

      return true;
    }

    public void run(Object o) throws IOException {
      try {
        if (addCoords)
          ncfile = NetcdfDataset.openDataset(filename, true, null);
        else
          ncfile = NetcdfDataset.openFile(filename, null);

        StringWriter sw = new StringWriter(50000);
        NCdumpW.print(ncfile, command, sw, task);
        result = sw.toString();

      } finally {
        try {
          if (ncfile != null) ncfile.close();
          ncfile = null;
        } catch (IOException ioe) {
          System.out.printf("Error closing %n");
        }
      }
    }

    // allow calling from outside
    void setNetcdfFile(NetcdfFile ncf) {
      this.ncfile = ncf;
      this.filename = ncf.getLocation();

      GetDataRunnable runner = new GetDataRunnable() {
        public void run(Object o) throws IOException {
          StringWriter sw = new StringWriter(50000);
          NCdumpW.print(ncfile, command, sw, task);
          result = sw.toString();
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
    TextHistoryPane ta;

    UnitDatasetCheck(PreferencesExt p) {
      super(p, "dataset:");
      ta = new TextHistoryPane(true);
      add(ta, BorderLayout.CENTER);
    }

    boolean process(Object o) {
      String command = (String) o;
      boolean err = false;

      try (NetcdfFile ncfile = NetcdfDataset.openDataset(command, addCoords, null)) {

        ta.setText("Variables for " + command + ":");
        for (Variable o1 : ncfile.getVariables()) {
          VariableEnhanced vs = (VariableEnhanced) o1;
          String units = vs.getUnitsString();
          StringBuilder sb = new StringBuilder();
          sb.append("   ").append(vs.getShortName()).append(" has unit= <").append(units).append(">");
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
      }

      return !err;
    }

    void closeOpenFiles() throws IOException {
      ta.clear();
    }

  }


  private class UnitConvert extends OpPanel {
    TextHistoryPane ta;

    UnitConvert(PreferencesExt prefs) {
      super(prefs, "unit:", false, false);

      ta = new TextHistoryPane(true);
      add(ta, BorderLayout.CENTER);

      JButton compareButton = new JButton("Compare");
      compareButton.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          compare(cb.getSelectedItem());
        }
      });
      buttPanel.add(compareButton);

      JButton dateButton = new JButton("UdunitDate");
      dateButton.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          checkUdunits(cb.getSelectedItem());
        }
      });
      buttPanel.add(dateButton);

      JButton cdateButton = new JButton("CalendarDate");
      cdateButton.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          checkCalendarDate(cb.getSelectedItem());
        }
      });
      buttPanel.add(cdateButton);
    }

    boolean process(Object o) {
      String command = (String) o;
      try {
        SimpleUnit su = SimpleUnit.factoryWithExceptions(command);
        ta.setText("parse=" + command + "\n");
        ta.appendLine("SimpleUnit.toString()          =" + su.toString() + "\n");
        ta.appendLine("SimpleUnit.getCanonicalString  =" + su.getCanonicalString());
        ta.appendLine("SimpleUnit.getImplementingClass= " + su.getImplementingClass());
        ta.appendLine("SimpleUnit.isUnknownUnit       = " + su.isUnknownUnit());

        return true;

      } catch (Exception e) {

        if (Debug.isSet("Xdeveloper")) {
          StringWriter sw = new StringWriter(10000);
          e.printStackTrace(new PrintWriter(sw));
          ta.setText(sw.toString());
        } else {
          ta.setText(e.getClass().getName() + ":" + e.getMessage() + "\n" + command);
        }
        return false;
      }
    }

    void closeOpenFiles() {
    }

    void compare(Object o) {
      String command = (String) o;
      StringTokenizer stoke = new StringTokenizer(command);
      List<String> list = new ArrayList<>();
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
          StringWriter sw = new StringWriter(10000);
          e.printStackTrace(new PrintWriter(sw));
          ta.setText(sw.toString());
        } else {
          ta.setText(e.getClass().getName() + ":" + e.getMessage() + "\n" + command);
        }

      }
    }

    void checkUdunits(Object o) {
      String command = (String) o;

      boolean isDate = false;
      try {
        DateUnit du = new DateUnit(command);
        ta.appendLine("\nFrom udunits:\n <" + command + "> isDateUnit = " + du);
        Date d = du.getDate();
        ta.appendLine("getStandardDateString = " + formatter.toDateTimeString(d));
        ta.appendLine("getDateOrigin = " + formatter.toDateTimeString(du.getDateOrigin()));
        isDate = true;

        Date d2 = DateUnit.getStandardOrISO(command);
        if (d2 == null)
          ta.appendLine("\nDateUnit.getStandardOrISO = false");
        else
          ta.appendLine("\nDateUnit.getStandardOrISO = " + formatter.toDateTimeString(d2));

      } catch (Exception e) {
        // ok to fall through
      }
      ta.appendLine("isDate = " + isDate);

      if (!isDate) {
        try {
          SimpleUnit su = SimpleUnit.factory(command);
          boolean isTime = su instanceof TimeUnit;
          ta.setText("<" + command + "> isTimeUnit= " + isTime);
          if (isTime) {
            TimeUnit du = (TimeUnit) su;
            ta.appendLine("\nTimeUnit = " + du);
          }

        } catch (Exception e) {
          if (Debug.isSet("Xdeveloper")) {
            StringWriter sw = new StringWriter(10000);
            e.printStackTrace(new PrintWriter(sw));
            ta.setText(sw.toString());
          } else {
            ta.setText(e.getClass().getName() + ":" + e.getMessage() + "\n" + command);
          }
        }
      }
    }

    void checkCalendarDate(Object o) {
      String command = (String) o;

      try {
        ta.setText("\nParse CalendarDate: <" + command + ">\n");
        CalendarDate cd = CalendarDate.parseUdunits(null, command);
        ta.appendLine("CalendarDate = " + cd);
      } catch (Throwable t) {
        ta.appendLine("not a CalendarDateUnit= " + t.getMessage());
      }

      try {
        /* int pos = command.indexOf(' ');
        if (pos < 0) return;
        String valString = command.substring(0, pos).trim();
        String unitString = command.substring(pos+1).trim();  */

        ta.appendLine("\nParse CalendarDateUnit: <" + command + ">\n");

        CalendarDateUnit cdu = CalendarDateUnit.of(null, command);
        ta.appendLine("CalendarDateUnit = " + cdu);
        ta.appendLine(" Calendar        = " + cdu.getCalendar());
        ta.appendLine(" PeriodField     = " + cdu.getCalendarPeriod().getField());
        ta.appendLine(" PeriodValue     = " + cdu.getCalendarPeriod().getValue());
        ta.appendLine(" Base            = " + cdu.getBaseCalendarDate());
        ta.appendLine(" isCalendarField = " + cdu.isCalendarField());

      } catch (Exception e) {
        ta.appendLine("not a CalendarDateUnit= " + e.getMessage());

        try {
          String[] s = command.split("%");
          if (s.length == 2) {
            Double val = Double.parseDouble(s[0].trim());
            ta.appendLine("\nval= " + val + " unit=" + s[1]);
            CalendarDateUnit cdu = CalendarDateUnit.of(null, s[1].trim());
            ta.appendLine("CalendarDateUnit= " + cdu);
            CalendarDate cd = cdu.makeCalendarDate(val);
            ta.appendLine(" CalendarDate = " + cd);
            Date d = cd.toDate();
            ta.appendLine(" Date.toString() = " + d);
            DateFormatter format = new DateFormatter();
            ta.appendLine(" DateFormatter= " + format.toDateTimeString(cd.toDate()));
          }
        } catch (Exception ee) {
          ta.appendLine("Failed on CalendarDateUnit " + ee.getMessage());
        }
      }

    }
  }

  private class DateFormatMark extends OpPanel {
    ComboBox testCB;
    DateFormatter dateFormatter = new DateFormatter();
    TextHistoryPane ta;

    DateFormatMark(PreferencesExt prefs) {
      super(prefs, "dateFormatMark:", false, false);

      ta = new TextHistoryPane(true);
      add(ta, BorderLayout.CENTER);

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

    void closeOpenFiles() {
    }

    void apply(Object mark, Object testo) {
      String dateFormatMark = (String) mark;
      String filename = (String) testo;
      try {
        Date coordValueDate = DateFromString.getDateUsingDemarkatedCount(filename, dateFormatMark, '#');
        String coordValue = dateFormatter.toDateTimeStringISO(coordValueDate);
        ta.setText("got date= " + coordValue);

      } catch (Exception e) {
        StringWriter sw = new StringWriter(5000);
        e.printStackTrace(new PrintWriter(sw));
        ta.setText(sw.toString());
      }
    }
  }


  /////////////////////////////////////////////////////////////////////
  private class CoordSysPanel extends OpPanel {
    NetcdfDataset ds = null;
    CoordSysTable coordSysTable;

    void closeOpenFiles() throws IOException {
      if (ds != null) ds.close();
      ds = null;
      coordSysTable.clear();
    }

    CoordSysPanel(PreferencesExt p) {
      super(p, "dataset:", true, false);
      coordSysTable = new CoordSysTable(prefs, buttPanel);
      add(coordSysTable, BorderLayout.CENTER);

      AbstractButton summaryButton = BAMutil.makeButtcon("Information", "Summary Info", false);
      summaryButton.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          Formatter f = new Formatter();
          coordSysTable.summaryInfo(f);
          detailTA.setText(f.toString());
          detailWindow.show();
        }
      });
      buttPanel.add(summaryButton);

      AbstractButton infoButton = BAMutil.makeButtcon("Information", "Parse Info", false);
      infoButton.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          if (ds != null) {
            try (NetcdfDatasetInfo info = new NetcdfDatasetInfo(ds)) {
              detailTA.appendLine(info.getParseInfo());
              detailTA.gotoTop();

            } catch (Exception e1) {
              StringWriter sw = new StringWriter(5000);
              e1.printStackTrace(new PrintWriter(sw));
              detailTA.setText(sw.toString());
            }
            detailWindow.show();
          }
        }
      });
      buttPanel.add(infoButton);


      JButton dsButton = new JButton("Object dump");
      dsButton.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          if (ds != null) {
            StringWriter sw = new StringWriter(5000);
            NetcdfDataset.debugDump(new PrintWriter(sw), ds);
            detailTA.setText(sw.toString());
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
      } catch (IOException ioe) {
        System.out.printf("close failed %n");
      }

      Object spiObject = null;
      try {
        ds = NetcdfDataset.openDataset(command, true, -1, null, spiObject);
        if (ds == null) {
          JOptionPane.showMessageDialog(null, "Failed to open <" + command + ">");
        } else {
          coordSysTable.setDataset(ds);
        }

      } catch (FileNotFoundException ioe) {
        JOptionPane.showMessageDialog(null, "NetcdfDataset cant open " + command + "\n" + ioe.getMessage());
        err = true;

      } catch (Exception e) {
        StringWriter sw = new StringWriter(5000);
        e.printStackTrace(new PrintWriter(sw));
        detailTA.setText(sw.toString());
        detailWindow.show();
        err = true;
        e.printStackTrace();
      }

      return !err;
    }

    void setDataset(NetcdfDataset ncd) {
      try {
        if (ds != null) ds.close();
        ds = null;
      } catch (IOException ioe) {
        System.out.printf("close failed %n");
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

    void closeOpenFiles() throws IOException {
      if (ncd != null) ncd.close();
      ncd = null;
      aggTable.clear();
    }

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

        ncd = NetcdfDataset.openDataset(command);
        aggTable.setAggDataset(ncd);

      } catch (FileNotFoundException ioe) {
        JOptionPane.showMessageDialog(null, "NetcdfDataset cant open " + command + "\n" + ioe.getMessage());
        err = true;

      } catch (Throwable e) {
        e.printStackTrace();
        StringWriter sw = new StringWriter(5000);
        e.printStackTrace(new PrintWriter(sw));
        detailTA.setText(sw.toString());
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

    void closeOpenFiles() throws IOException {
      if (raf != null) raf.close();
      raf = null;
    }

    BufrPanel(PreferencesExt p) {
      super(p, "file:", true, false);
      bufrTable = new BufrMessageViewer(prefs, buttPanel);
      add(bufrTable, BorderLayout.CENTER);
    }

    boolean process(Object o) {
      String command = (String) o;
      boolean err = false;

      try {
        if (raf != null)
          raf.close();
        raf = new ucar.unidata.io.RandomAccessFile(command, "r");

        bufrTable.setBufrFile(raf);

      } catch (FileNotFoundException ioe) {
        JOptionPane.showMessageDialog(null, "NetcdfDataset cant open " + command + "\n" + ioe.getMessage());
        err = true;

      } catch (Exception e) {
        e.printStackTrace();
        StringWriter sw = new StringWriter(5000);
        e.printStackTrace(new PrintWriter(sw));
        detailTA.setText(sw.toString());
        detailWindow.show();
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
    bufrFileChooser = new FileManager(parentFrame, null, null, (PreferencesExt) mainPrefs.node("bufrFileManager"));
  }

  private class BufrTableBPanel extends OpPanel {
    BufrTableBViewer bufrTable;
    JComboBox<BufrTables.Format> modes;
    JComboBox<BufrTables.TableConfig> tables;

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

      modes = new JComboBox<>(BufrTables.Format.values());
      buttPanel.add(modes);

      JButton accept = new JButton("Accept");
      buttPanel.add(accept);
      accept.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          accept();
        }
      });

      tables = new JComboBox<>(BufrTables.getTableConfigsAsArray());
      buttPanel.add(tables);
      tables.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          acceptTable((BufrTables.TableConfig) tables.getSelectedItem());
        }
      });

      bufrTable = new BufrTableBViewer(prefs, buttPanel);
      add(bufrTable, BorderLayout.CENTER);
    }

    boolean process(Object command) {
      return true;
    }

    void closeOpenFiles() {
    }

    void accept() {
      String command = (String) cb.getSelectedItem();

      try {
        Object format = modes.getSelectedItem();
        bufrTable.setBufrTableB(command, (BufrTables.Format) format);

      } catch (FileNotFoundException ioe) {
        JOptionPane.showMessageDialog(null, "BufrTableViewer cant open " + command + "\n" + ioe.getMessage());
        detailTA.setText("Failed to open <" + command + ">\n" + ioe.getMessage());
        detailTA.setVisible(true);

      } catch (Exception e) {
        e.printStackTrace();
        StringWriter sw = new StringWriter(5000);
        e.printStackTrace(new PrintWriter(sw));
        detailTA.setText(sw.toString());
        detailTA.setVisible(true);
      }

    }

    void acceptTable(BufrTables.TableConfig tc) {

      try {
        bufrTable.setBufrTableB(tc.getTableBname(), tc.getTableBformat());

      } catch (FileNotFoundException ioe) {
        JOptionPane.showMessageDialog(null, "BufrTableViewer cant open " + tc + "\n" + ioe.getMessage());
        detailTA.setText("Failed to open <" + tc + ">\n" + ioe.getMessage());
        detailTA.setVisible(true);

      } catch (Exception e) {
        e.printStackTrace();
        StringWriter sw = new StringWriter(5000);
        e.printStackTrace(new PrintWriter(sw));
        detailTA.setText(sw.toString());
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
    JComboBox<BufrTables.Format> modes;
    JComboBox<BufrTables.TableConfig> tables;

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

      modes = new JComboBox<>(BufrTables.Format.values());
      buttPanel.add(modes);

      JButton accept = new JButton("Accept");
      buttPanel.add(accept);
      accept.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          accept();
        }
      });

      tables = new JComboBox<>(BufrTables.getTableConfigsAsArray());
      buttPanel.add(tables);
      tables.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          acceptTable((BufrTables.TableConfig) tables.getSelectedItem());
        }
      });


      bufrTable = new BufrTableDViewer(prefs, buttPanel);
      add(bufrTable, BorderLayout.CENTER);
    }

    boolean process(Object command) {
      return true;
    }

    void closeOpenFiles() {
    }

    void accept() {
      String command = (String) cb.getSelectedItem();
      if (command == null) return;

      try {
        Object mode = modes.getSelectedItem();
        bufrTable.setBufrTableD(command, (BufrTables.Format) mode);

      } catch (FileNotFoundException ioe) {
        JOptionPane.showMessageDialog(null, "BufrTableViewer cant open " + command + "\n" + ioe.getMessage());
        detailTA.setText("Failed to open <" + command + ">\n" + ioe.getMessage());
        detailTA.setVisible(true);

      } catch (Exception e) {
        e.printStackTrace();
        StringWriter sw = new StringWriter(5000);
        e.printStackTrace(new PrintWriter(sw));
        detailTA.setText(sw.toString());
        detailTA.setVisible(true);
      }

    }

    void acceptTable(BufrTables.TableConfig tc) {

      try {
        bufrTable.setBufrTableD(tc.getTableDname(), tc.getTableDformat());

      } catch (FileNotFoundException ioe) {
        JOptionPane.showMessageDialog(null, "BufrTableViewer cant open " + tc + "\n" + ioe.getMessage());
        detailTA.setText("Failed to open <" + tc + ">\n" + ioe.getMessage());
        detailTA.setVisible(true);

      } catch (Exception e) {
        e.printStackTrace();
        StringWriter sw = new StringWriter(5000);
        e.printStackTrace(new PrintWriter(sw));
        detailTA.setText(sw.toString());
        detailTA.setVisible(true);
      }

    }

    void save() {
      bufrTable.save();
      super.save();
    }

  }

  ////////////////////////////////////////////////////////////////////////
  /* private class BufrReportPanel extends OpPanel {
    ucar.nc2.ui.BufrReportPanel reportPanel;
    boolean useIndex = true;
    JComboBox reports;

    BufrReportPanel(PreferencesExt p) {
      super(p, "collection:", true, false);
      reportPanel = new ucar.nc2.ui.BufrReportPanel(prefs, buttPanel);
      add(reportPanel, BorderLayout.CENTER);

      reports = new JComboBox(ucar.nc2.ui.BufrReportPanel.Report.values());
      buttPanel.add(reports);

      AbstractAction useIndexButt = new AbstractAction() {
        public void actionPerformed(ActionEvent e) {
          Boolean state = (Boolean) getValue(BAMutil.STATE);
          useIndex = state.booleanValue();
        }
      };
      useIndexButt.putValue(BAMutil.STATE, useIndex);
      BAMutil.setActionProperties(useIndexButt, "Doit", "use default table", true, 'C', -1);
      BAMutil.addActionToContainer(buttPanel, useIndexButt);

      AbstractAction doitButt = new AbstractAction() {
        public void actionPerformed(ActionEvent e) {
          process();
        }
      };
      BAMutil.setActionProperties(doitButt, "alien", "make report", false, 'C', -1);
      BAMutil.addActionToContainer(buttPanel, doitButt);
    }

    boolean process(Object o) {
      return reportPanel.setCollection((String) o);
    }

    boolean process() {
      boolean err = false;
      String command = (String) cb.getSelectedItem();

      ByteArrayOutputStream bos = new ByteArrayOutputStream(10000);
      try {
        reportPanel.doReport(command, useIndex, (ucar.nc2.ui.BufrReportPanel.Report) reports.getSelectedItem());

      } catch (IOException ioe) {
        JOptionPane.showMessageDialog(null, "Grib2ReportPanel cant open " + command + "\n" + ioe.getMessage());
        ioe.printStackTrace();
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
      reportPanel.save();
      super.save();
    }

  } */

  /////////////////////////////////////////////////////////////////////
  private class GribFilesPanel extends OpPanel {
    ucar.nc2.ui.grib.GribFilesPanel gribTable;

    void closeOpenFiles() throws IOException {
    }

    GribFilesPanel(PreferencesExt p) {
      super(p, "collection:", true, false);
      gribTable = new ucar.nc2.ui.grib.GribFilesPanel(prefs);
      add(gribTable, BorderLayout.CENTER);
      gribTable.addPropertyChangeListener(new java.beans.PropertyChangeListener() {
        public void propertyChange(java.beans.PropertyChangeEvent e) {
          if (e.getPropertyName().equals("openGrib1Collection")) {
            String filename = (String) e.getNewValue();
            openGrib1Collection(filename);
          }
        }
      });

      AbstractButton showButt = BAMutil.makeButtcon("Information", "Show Collection", false);
      showButt.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          Formatter f = new Formatter();
          gribTable.showCollection(f);
          detailTA.setText(f.toString());
          detailTA.gotoTop();
          detailWindow.show();
        }
      });
      buttPanel.add(showButt);
    }

    boolean process(Object o) {
      String command = (String) o;
      boolean err = false;

      try {
        gribTable.setCollection(command);

      } catch (FileNotFoundException ioe) {
        JOptionPane.showMessageDialog(null, "NetcdfDataset cant open " + command + "\n" + ioe.getMessage());
        err = true;

      } catch (Exception e) {
        e.printStackTrace();
        StringWriter sw = new StringWriter(5000);
        e.printStackTrace(new PrintWriter(sw));
        detailTA.setText(sw.toString());
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
  // GRIB2
  private class Grib2CollectionPanel extends OpPanel {
    ucar.nc2.ui.grib.Grib2CollectionPanel gribTable;

    void closeOpenFiles() throws IOException {
      gribTable.closeOpenFiles();
    }

    Grib2CollectionPanel(PreferencesExt p) {
      super(p, "collection:", true, false);
      gribTable = new ucar.nc2.ui.grib.Grib2CollectionPanel(prefs, buttPanel);
      add(gribTable, BorderLayout.CENTER);

      gribTable.addPropertyChangeListener(new java.beans.PropertyChangeListener() {
        public void propertyChange(java.beans.PropertyChangeEvent e) {
          if (e.getPropertyName().equals("openGrib2Collection")) {
            String collectionName = (String) e.getNewValue();
            openGrib2Collection(collectionName);
          }
        }
      });

      AbstractButton showButt = BAMutil.makeButtcon("Information", "Show Collection", false);
      showButt.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          Formatter f = new Formatter();
          gribTable.showCollection(f);
          detailTA.setText(f.toString());
          detailTA.gotoTop();
          detailWindow.show();
        }
      });
      buttPanel.add(showButt);

      AbstractButton infoButton = BAMutil.makeButtcon("Information", "Check Problems", false);
      infoButton.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          Formatter f = new Formatter();
          gribTable.checkProblems(f);
          detailTA.setText(f.toString());
          detailTA.gotoTop();
          detailWindow.show();
        }
      });
      buttPanel.add(infoButton);

      AbstractButton gdsButton = BAMutil.makeButtcon("Information", "Show GDS use", false);
      gdsButton.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          Formatter f = new Formatter();
          gribTable.showGDSuse(f);
          detailTA.setText(f.toString());
          detailTA.gotoTop();
          detailWindow.show();
        }
      });
      buttPanel.add(gdsButton);

      AbstractButton writeButton = BAMutil.makeButtcon("netcdf", "Write index", false);
      writeButton.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          Formatter f = new Formatter();
          try {
            if (!gribTable.writeIndex(f)) return;
          } catch (IOException e1) {
            e1.printStackTrace();
          }
          detailTA.setText(f.toString());
          detailTA.gotoTop();
          detailWindow.show();
        }
      });
      buttPanel.add(writeButton);
    }

    void setCollection(String collection) {
      if (process(collection)) {
        cb.addItem(collection);
      }
    }

    boolean process(Object o) {
      String command = (String) o;
      boolean err = false;

      try {
        gribTable.setCollection(command);

      } catch (FileNotFoundException ioe) {
        JOptionPane.showMessageDialog(null, "NetcdfDataset cant open " + command + "\n" + ioe.getMessage());
        err = true;

      } catch (Exception e) {
        e.printStackTrace();
        StringWriter sw = new StringWriter(5000);
        e.printStackTrace(new PrintWriter(sw));
        detailTA.setText(sw.toString());
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
  private class Grib2DataPanel extends OpPanel {
    ucar.nc2.ui.grib.Grib2DataPanel gribTable;

    void closeOpenFiles() throws IOException {
    }

    Grib2DataPanel(PreferencesExt p) {
      super(p, "collection:", true, false);
      gribTable = new ucar.nc2.ui.grib.Grib2DataPanel(prefs);
      add(gribTable, BorderLayout.CENTER);

      gribTable.addPropertyChangeListener(new java.beans.PropertyChangeListener() {
        public void propertyChange(java.beans.PropertyChangeEvent e) {
          if (e.getPropertyName().equals("openGrib2Collection")) {
            String collectionName = (String) e.getNewValue();
            openGrib2Collection(collectionName);
          }
        }
      });

      AbstractButton infoButton = BAMutil.makeButtcon("Information", "Show Info", false);
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

      AbstractButton checkButton = BAMutil.makeButtcon("Information", "Check Problems", false);
      checkButton.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          Formatter f = new Formatter();
          gribTable.checkProblems(f);
          detailTA.setText(f.toString());
          detailTA.gotoTop();
          detailWindow.show();
        }
      });
      buttPanel.add(checkButton);
    }

    void setCollection(String collection) {
      if (process(collection)) {
        cb.addItem(collection);
      }
    }

    boolean process(Object o) {
      String command = (String) o;
      boolean err = false;

      try {
        gribTable.setCollection(command);

      } catch (FileNotFoundException ioe) {
        JOptionPane.showMessageDialog(null, "NetcdfDataset cant open " + command + "\n" + ioe.getMessage());
        err = true;

      } catch (Exception e) {
        e.printStackTrace();
        StringWriter sw = new StringWriter(5000);
        e.printStackTrace(new PrintWriter(sw));
        detailTA.setText(sw.toString());
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
  private class Grib1DataPanel extends OpPanel {
    Grib1DataTable gribTable;

    void closeOpenFiles() throws IOException {
    }

    Grib1DataPanel(PreferencesExt p) {
      super(p, "collection:", true, false);
      gribTable = new Grib1DataTable(prefs);
      add(gribTable, BorderLayout.CENTER);

      gribTable.addPropertyChangeListener(new java.beans.PropertyChangeListener() {
        public void propertyChange(java.beans.PropertyChangeEvent e) {
          if (e.getPropertyName().equals("openGrib1Collection")) {
            String collectionName = (String) e.getNewValue();
            openGrib1Collection(collectionName);
          }
        }
      });

      AbstractButton infoButton = BAMutil.makeButtcon("Information", "Check Problems", false);
      infoButton.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          Formatter f = new Formatter();
          gribTable.checkProblems(f);
          detailTA.setText(f.toString());
          detailTA.gotoTop();
          detailWindow.show();
        }
      });
      buttPanel.add(infoButton);
    }

    void setCollection(String collection) {
      if (process(collection)) {
        cb.addItem(collection);
      }
    }

    boolean process(Object o) {
      String command = (String) o;
      boolean err = false;

      try {
        gribTable.setCollection(command);

      } catch (FileNotFoundException ioe) {
        JOptionPane.showMessageDialog(null, "NetcdfDataset cant open " + command + "\n" + ioe.getMessage());
        err = true;

      } catch (Exception e) {
        e.printStackTrace();
        StringWriter sw = new StringWriter(5000);
        e.printStackTrace(new PrintWriter(sw));
        detailTA.setText(sw.toString());
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
  private class BufrCdmIndexPanel extends OpPanel {
    ucar.nc2.ui.BufrCdmIndexPanel table;

    void closeOpenFiles() throws IOException {
      //table.closeOpenFiles();
    }

    BufrCdmIndexPanel(PreferencesExt p) {
      super(p, "index file:", true, false);
      table = new ucar.nc2.ui.BufrCdmIndexPanel(prefs, buttPanel);
      add(table, BorderLayout.CENTER);
    }

    boolean process(Object o) {
      String command = (String) o;
      boolean err = false;

      try {
        table.setIndexFile(command);

      } catch (FileNotFoundException ioe) {
        JOptionPane.showMessageDialog(null, "BufrCdmIndexPanel cant open " + command + "\n" + ioe.getMessage());
        err = true;

      } catch (Exception e) {
        e.printStackTrace();
        StringWriter sw = new StringWriter(5000);
        e.printStackTrace(new PrintWriter(sw));
        detailTA.setText(sw.toString());
        detailWindow.show();
        err = true;
      }

      return !err;
    }

    void save() {
      table.save();
      super.save();
    }

  }

  /////////////////////////////////////////////////////////////////////
  private class CdmIndexPanel extends OpPanel {
    ucar.nc2.ui.grib.CdmIndexPanel indexPanel;

    void closeOpenFiles() throws IOException {
      indexPanel.clear();
    }

    CdmIndexPanel(PreferencesExt p) {
      super(p, "index file:", true, false);
      indexPanel = new ucar.nc2.ui.grib.CdmIndexPanel(prefs, buttPanel);
      indexPanel.addPropertyChangeListener(new PropertyChangeListener() {
        public void propertyChange(PropertyChangeEvent e) {
          if (e.getPropertyName().equals("openGrib2Collection")) {
            String collectionName = (String) e.getNewValue();
            openGrib2Collection(collectionName);
          }
        }
      });

      add(indexPanel, BorderLayout.CENTER);
    }

    boolean process(Object o) {
      String command = (String) o;
      boolean err = false;

      try {
        indexPanel.setIndexFile(Paths.get(command), new FeatureCollectionConfig());

      } catch (FileNotFoundException ioe) {
        JOptionPane.showMessageDialog(null, "GribCdmIndexPanel cant open " + command + "\n" + ioe.getMessage());
        err = true;

      } catch (Throwable e) {
        e.printStackTrace();
        StringWriter sw = new StringWriter(5000);
        e.printStackTrace(new PrintWriter(sw));
        detailTA.setText(sw.toString());
        detailWindow.show();
        err = true;
      }

      return !err;
    }

    void save() {
      indexPanel.save();
      super.save();
    }

  }

  /////////////////////////////////////////////////////////////////////
  private class GribIndexPanel extends OpPanel {
    ucar.nc2.ui.grib.GribIndexPanel gribTable;

    void closeOpenFiles() throws IOException {
      gribTable.closeOpenFiles();
    }

    GribIndexPanel(PreferencesExt p) {
      super(p, "index file:", true, false);
      gribTable = new ucar.nc2.ui.grib.GribIndexPanel(prefs, buttPanel);
      add(gribTable, BorderLayout.CENTER);
    }

    boolean process(Object o) {
      String command = (String) o;
      boolean err = false;

      try {
        gribTable.setIndexFile(command);

      } catch (FileNotFoundException ioe) {
        JOptionPane.showMessageDialog(null, "NetcdfDataset cant open " + command + "\n" + ioe.getMessage());
        err = true;

      } catch (Exception e) {
        e.printStackTrace();
        StringWriter sw = new StringWriter(5000);
        e.printStackTrace(new PrintWriter(sw));
        detailTA.setText(sw.toString());
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
  // raw grib access - dont go through the IOSP
  private class Grib1CollectionPanel extends OpPanel {
    //ucar.unidata.io.RandomAccessFile raf = null;
    ucar.nc2.ui.grib.Grib1CollectionPanel gribTable;

    void closeOpenFiles() throws IOException {
      gribTable.closeOpenFiles();
    }

    Grib1CollectionPanel(PreferencesExt p) {
      super(p, "collection:", true, false);
      gribTable = new ucar.nc2.ui.grib.Grib1CollectionPanel(buttPanel, prefs);
      add(gribTable, BorderLayout.CENTER);

      AbstractButton showButt = BAMutil.makeButtcon("Information", "Show Collection", false);
      showButt.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          Formatter f = new Formatter();
          gribTable.showCollection(f);
          detailTA.setText(f.toString());
          detailTA.gotoTop();
          detailWindow.show();
        }
      });
      buttPanel.add(showButt);

      AbstractButton writeButton = BAMutil.makeButtcon("netcdf", "Write index", false);
      writeButton.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          Formatter f = new Formatter();
          try {
            if (!gribTable.writeIndex(f)) return;
          } catch (IOException e1) {
            e1.printStackTrace();
          }
          detailTA.setText(f.toString());
          detailTA.gotoTop();
          detailWindow.show();
        }
      });
      buttPanel.add(writeButton);
    }

    void setCollection(String collection) {
      if (process(collection)) {
        cb.addItem(collection);
      }
    }

    boolean process(Object o) {
      String command = (String) o;
      boolean err = false;

      try {
        gribTable.setCollection(command);

      } catch (FileNotFoundException ioe) {
        JOptionPane.showMessageDialog(null, "NetcdfDataset cant open " + command + "\n" + ioe.getMessage());
        err = true;

      } catch (Exception e) {
        e.printStackTrace();
        StringWriter sw = new StringWriter(5000);
        e.printStackTrace(new PrintWriter(sw));
        detailTA.setText(sw.toString());
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

  private class ReportOpPanel extends OpPanel {
    ucar.nc2.ui.ReportPanel reportPanel;
    boolean useIndex = true;
    boolean eachFile = false;
    boolean extra = false;
    JComboBox reports;

    ReportOpPanel(PreferencesExt p, ReportPanel reportPanel) {
      super(p, "collection:", true, false);
      this.reportPanel = reportPanel;
      add(reportPanel, BorderLayout.CENTER);
      reportPanel.addOptions(buttPanel);

      reports = new JComboBox(reportPanel.getOptions());
      buttPanel.add(reports);

      AbstractAction useIndexButt = new AbstractAction() {
        public void actionPerformed(ActionEvent e) {
          useIndex = (Boolean) getValue(BAMutil.STATE);
        }
      };
      useIndexButt.putValue(BAMutil.STATE, useIndex);
      BAMutil.setActionProperties(useIndexButt, "Doit", "use Index", true, 'C', -1);
      BAMutil.addActionToContainer(buttPanel, useIndexButt);

      AbstractAction eachFileButt = new AbstractAction() {
        public void actionPerformed(ActionEvent e) {
          eachFile = (Boolean) getValue(BAMutil.STATE);
        }
      };
      eachFileButt.putValue(BAMutil.STATE, eachFile);
      BAMutil.setActionProperties(eachFileButt, "Doit", "report on each file", true, 'E', -1);
      BAMutil.addActionToContainer(buttPanel, eachFileButt);

      AbstractAction extraButt = new AbstractAction() {
        public void actionPerformed(ActionEvent e) {
          extra = (Boolean) getValue(BAMutil.STATE);
        }
      };
      extraButt.putValue(BAMutil.STATE, extra);
      BAMutil.setActionProperties(extraButt, "Doit", "extra info", true, 'X', -1);
      BAMutil.addActionToContainer(buttPanel, extraButt);

      AbstractAction doitButt = new AbstractAction() {
        public void actionPerformed(ActionEvent e) {
          process();
        }
      };
      BAMutil.setActionProperties(doitButt, "alien", "make report", false, 'C', -1);
      BAMutil.addActionToContainer(buttPanel, doitButt);
    }

    void closeOpenFiles() {
    }

    boolean process(Object o) {
      return reportPanel.showCollection((String) o);
    }

    boolean process() {
      boolean err = false;
      String command = (String) cb.getSelectedItem();

      try {
        reportPanel.doReport(command, useIndex, eachFile, extra, reports.getSelectedItem());

      } catch (IOException ioe) {
        JOptionPane.showMessageDialog(null, "Grib2ReportPanel cant open " + command + "\n" + ioe.getMessage());
        ioe.printStackTrace();
        err = true;

      } catch (Exception e) {
        StringWriter sw = new StringWriter(5000);
        e.printStackTrace(new PrintWriter(sw));
        detailTA.setText(sw.toString());
        detailWindow.show();
        err = true;
      }

      return !err;
    }

    void save() {
      // reportPanel.save();
      super.save();
    }

  }

  /* private class Grib2ReportPanel extends OpPanel {
    ucar.nc2.ui.grib.Grib2ReportPanel gribReport;
    boolean useIndex = true;
    boolean eachFile = false;
    boolean extra = false;
    JComboBox reports;

    Grib2ReportPanel(PreferencesExt p) {
      super(p, "collection:", true, false);
      gribReport = new ucar.nc2.ui.grib.Grib2ReportPanel(prefs, buttPanel);
      add(gribReport, BorderLayout.CENTER);

      reports = new JComboBox(ucar.nc2.ui.grib.Grib2ReportPanel.Report.values());
      buttPanel.add(reports);

      AbstractAction useIndexButt = new AbstractAction() {
        public void actionPerformed(ActionEvent e) {
          Boolean state = (Boolean) getValue(BAMutil.STATE);
          useIndex = state.booleanValue();
        }
      };
      useIndexButt.putValue(BAMutil.STATE, useIndex);
      BAMutil.setActionProperties(useIndexButt, "Doit", "use Index", true, 'C', -1);
      BAMutil.addActionToContainer(buttPanel, useIndexButt);

      AbstractAction eachFileButt = new AbstractAction() {
        public void actionPerformed(ActionEvent e) {
          Boolean state = (Boolean) getValue(BAMutil.STATE);
          eachFile = state.booleanValue();
        }
      };
      eachFileButt.putValue(BAMutil.STATE, eachFile);
      BAMutil.setActionProperties(eachFileButt, "Doit", "report on each file", true, 'E', -1);
      BAMutil.addActionToContainer(buttPanel, eachFileButt);

      AbstractAction extraButt = new AbstractAction() {
        public void actionPerformed(ActionEvent e) {
          Boolean state = (Boolean) getValue(BAMutil.STATE);
          extra = state.booleanValue();
        }
      };
      extraButt.putValue(BAMutil.STATE, extra);
      BAMutil.setActionProperties(extraButt, "Doit", "extra info", true, 'X', -1);
      BAMutil.addActionToContainer(buttPanel, extraButt);

      AbstractAction doitButt = new AbstractAction() {
        public void actionPerformed(ActionEvent e) {
          process();
        }
      };
      BAMutil.setActionProperties(doitButt, "alien", "make report", false, 'C', -1);
      BAMutil.addActionToContainer(buttPanel, doitButt);
    }

    void closeOpenFiles() {
    }

    boolean process(Object o) {
      return gribReport.setCollection((String) o);
    }

    boolean process() {
      boolean err = false;
      String command = (String) cb.getSelectedItem();

      ByteArrayOutputStream bos = new ByteArrayOutputStream(10000);
      try {
        gribReport.doReport(command, useIndex, eachFile, extra, (ucar.nc2.ui.grib.Grib2ReportPanel.Report) reports.getSelectedItem());

      } catch (IOException ioe) {
        JOptionPane.showMessageDialog(null, "Grib2ReportPanel cant open " + command + "\n" + ioe.getMessage());
        ioe.printStackTrace();
        err = true;

      } catch (Exception e) {
        e.printStackTrace(new PrintStream(bos));
        detailTA.setText(bos.toString());
        detailWindow.show();
        err = true;
      }

      return !err;
    }

    void save() {
      gribReport.save();
      super.save();
    }

  } */

  /////////////////////////////////////////////////////////////////////

  /* private class Grib1ReportPanel extends OpPanel {
    ucar.nc2.ui.grib.Grib1ReportPanel gribReport;
    boolean useIndex = true;
    JComboBox reports;

    Grib1ReportPanel(PreferencesExt p) {
      super(p, "collection:", true, false);
      gribReport = new ucar.nc2.ui.grib.Grib1ReportPanel(prefs, buttPanel);
      add(gribReport, BorderLayout.CENTER);

      reports = new JComboBox(ucar.nc2.ui.grib.Grib1ReportPanel.Report.values());
      buttPanel.add(reports);

      AbstractAction useIndexButt = new AbstractAction() {
        public void actionPerformed(ActionEvent e) {
          Boolean state = (Boolean) getValue(BAMutil.STATE);
          useIndex = state.booleanValue();
        }
      };
      useIndexButt.putValue(BAMutil.STATE, useIndex);
      BAMutil.setActionProperties(useIndexButt, "Doit", "use default table", true, 'C', -1);
      BAMutil.addActionToContainer(buttPanel, useIndexButt);

      AbstractAction doitButt = new AbstractAction() {
        public void actionPerformed(ActionEvent e) {
          process();
        }
      };
      BAMutil.setActionProperties(doitButt, "alien", "make report", false, 'C', -1);
      BAMutil.addActionToContainer(buttPanel, doitButt);
    }

    boolean process(Object o) {
      return gribReport.setCollection((String) o);
    }

    boolean process() {
      boolean err = false;
      String command = (String) cb.getSelectedItem();

      ByteArrayOutputStream bos = new ByteArrayOutputStream(10000);
      try {
        gribReport.doReport(command, useIndex, (ucar.nc2.ui.grib.Grib1ReportPanel.Report) reports.getSelectedItem());

      } catch (IOException ioe) {
        JOptionPane.showMessageDialog(null, "Grib2ReportPanel cant open " + command + "\n" + ioe.getMessage());
        ioe.printStackTrace();
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
      gribReport.save();
      super.save();
    }

  }  */

  /////////////////////////////////////////////////////////////////////

  private class GribTemplatePanel extends OpPanel {
    GribWmoTemplatesPanel codeTable;

    GribTemplatePanel(PreferencesExt p) {
      super(p, "table:", false, false, false);

      final JComboBox<WmoTemplateTable.Version> modes = new JComboBox<>(WmoTemplateTable.Version.values());
      modes.setSelectedItem(WmoTemplateTable.standard);
      topPanel.add(modes, BorderLayout.CENTER);
      modes.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          codeTable.setTable((WmoTemplateTable.Version) modes.getSelectedItem());
        }
      });

      codeTable = new GribWmoTemplatesPanel(prefs, buttPanel);
      add(codeTable, BorderLayout.CENTER);
    }

    boolean process(Object command) {
      return true;
    }

    void save() {
      codeTable.save();
      super.save();
    }

    void closeOpenFiles() {
    }

  }

  /////////////////////////////////////////////////////////////////////

  private class GribCodePanel extends OpPanel {
    GribWmoCodesPanel codeTable;

    GribCodePanel(PreferencesExt p) {
      super(p, "table:", false, false, false);

      final JComboBox<WmoCodeTable.Version> modes = new JComboBox<>(WmoCodeTable.Version.values());
      modes.setSelectedItem(WmoCodeTable.standard);
      topPanel.add(modes, BorderLayout.CENTER);
      modes.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          codeTable.setTable((WmoCodeTable.Version) modes.getSelectedItem());
        }
      });

      codeTable = new GribWmoCodesPanel(prefs, buttPanel);
      add(codeTable, BorderLayout.CENTER);
    }

    boolean process(Object command) {
      return true;
    }

    void save() {
      codeTable.save();
      super.save();
    }

    void closeOpenFiles() {
    }
  }

  /////////////////////////////////////////////////////////////////////

  private class BufrCodePanel extends OpPanel {
    BufrWmoCodesPanel codeTable;

    BufrCodePanel(PreferencesExt p) {
      super(p, "table:", false, false, false);
      codeTable = new BufrWmoCodesPanel(prefs, buttPanel);
      add(codeTable, BorderLayout.CENTER);
    }

    boolean process(Object command) {
      return true;
    }

    void save() {
      codeTable.save();
      super.save();
    }

    void closeOpenFiles() {
    }
  }

  /////////////////////////////////////////////////////////////////////

  private class WmoCCPanel extends OpPanel {
    WmoCommonCodesPanel codeTable;

    WmoCCPanel(PreferencesExt p) {
      super(p, "table:", false, false);

      codeTable = new WmoCommonCodesPanel(prefs, buttPanel);
      add(codeTable, BorderLayout.CENTER);
    }

    boolean process(Object command) {
      return true;
    }

    void save() {
      codeTable.save();
      super.save();
    }

    void closeOpenFiles() {
    }
  }


  /////////////////////////////////////////////////////////////////////

  private class Grib1TablePanel extends OpPanel {
    Grib1TablesViewer codeTable;

    Grib1TablePanel(PreferencesExt p) {
      super(p, "table:", true, false);
      codeTable = new Grib1TablesViewer(prefs, buttPanel);
      add(codeTable, BorderLayout.CENTER);
    }

    boolean process(Object command) {
      try {
        codeTable.setTable((String) command);
        return true;
      } catch (IOException e) {
        return false;
      }
    }

    void save() {
      codeTable.save();
      super.save();
    }

    void closeOpenFiles() {
    }

  }


  /////////////////////////////////////////////////////////////////////

  private class Grib2TablePanel extends OpPanel {
    Grib2TableViewer2 codeTable;

    Grib2TablePanel(PreferencesExt p) {
      super(p, "table:", false, false);
      codeTable = new Grib2TableViewer2(prefs, buttPanel);
      add(codeTable, BorderLayout.CENTER);
    }

    boolean process(Object command) {
      return true;
    }

    void save() {
      codeTable.save();
      super.save();
    }

    void closeOpenFiles() {
    }
  }

  /////////////////////////////////////////////////////////////////////

  private class GribRenamePanel extends OpPanel {
    ucar.nc2.ui.grib.GribRenamePanel panel;

    GribRenamePanel(PreferencesExt p) {
      super(p, "matchNcepName: ", true, false, false);
      panel = new ucar.nc2.ui.grib.GribRenamePanel(prefs, buttPanel);
      add(panel, BorderLayout.CENTER);
    }

    boolean process(Object o) {
      return panel.matchNcepName((String) o);
    }

    void save() {
      panel.save();
      super.save();
    }

    void closeOpenFiles() {
    }

  }

  /////////////////////////////////////////////////////////////////////
  private class GribRewritePanel extends OpPanel {
    ucar.nc2.ui.grib.GribRewritePanel ftTable;
    final FileManager dirChooser;

    GribRewritePanel(PreferencesExt prefs) {
      super(prefs, "dir:", false, false);
      dirChooser = new FileManager(parentFrame, null, null, (PreferencesExt) prefs.node("FeatureScanFileManager"));
      ftTable = new ucar.nc2.ui.grib.GribRewritePanel(prefs, buttPanel);
      add(ftTable, BorderLayout.CENTER);

      ftTable.addPropertyChangeListener(new java.beans.PropertyChangeListener() {
        public void propertyChange(java.beans.PropertyChangeEvent e) {
          if (e.getPropertyName().equals("openNetcdfFile")) {
            String datasetName = (String) e.getNewValue();
            openNetcdfFile(datasetName);
          } else if (e.getPropertyName().equals("openGridDataset")) {
            String datasetName = (String) e.getNewValue();
            openGridDataset(datasetName);
          } else if (e.getPropertyName().equals("openGrib1Data")) {
            String datasetName = (String) e.getNewValue();
            openGrib1Data(datasetName);
          } else if (e.getPropertyName().equals("openGrib2Data")) {
            String datasetName = (String) e.getNewValue();
            openGrib2Data(datasetName);
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

    void closeOpenFiles() {
      ftTable.clear();
    }

    void save() {
      dirChooser.save();
      ftTable.save();
      prefs.put("currDir", dirChooser.getCurrentDirectory());
      super.save();
    }
  }

  /////////////////////////////////////////////////////////////////////
  private class Hdf5ObjectPanel extends OpPanel {
    ucar.unidata.io.RandomAccessFile raf = null;
    Hdf5ObjectTable hdf5Table;

    void closeOpenFiles() throws IOException {
      hdf5Table.closeOpenFiles();
    }

    Hdf5ObjectPanel(PreferencesExt p) {
      super(p, "file:", true, false);
      hdf5Table = new Hdf5ObjectTable(prefs);
      add(hdf5Table, BorderLayout.CENTER);

      AbstractButton infoButton = BAMutil.makeButtcon("Information", "Compact Representation", false);
      infoButton.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          Formatter f = new Formatter();
          try {
            hdf5Table.showInfo(f);

          } catch (IOException ioe) {
            StringWriter sw = new StringWriter(5000);
            ioe.printStackTrace(new PrintWriter(sw));
            detailTA.setText(sw.toString());
            detailWindow.show();
            return;
          }
          detailTA.setText(f.toString());
          detailTA.gotoTop();
          detailWindow.show();
        }
      });
      buttPanel.add(infoButton);

      AbstractButton infoButton2 = BAMutil.makeButtcon("Information", "Detail Info", false);
      infoButton2.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          Formatter f = new Formatter();
          try {
            hdf5Table.showInfo2(f);

          } catch (IOException ioe) {
            StringWriter sw = new StringWriter(5000);
            ioe.printStackTrace(new PrintWriter(sw));
            detailTA.setText(sw.toString());
            detailWindow.show();
            return;
          }
          detailTA.setText(f.toString());
          detailTA.gotoTop();
          detailWindow.show();
        }
      });
      buttPanel.add(infoButton2);

      AbstractButton eosdump = BAMutil.makeButtcon("alien", "Show EOS processing", false);
      eosdump.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          try {
            Formatter f = new Formatter();
            hdf5Table.getEosInfo(f);
            detailTA.setText(f.toString());
            detailWindow.show();
          } catch (IOException ioe) {
            StringWriter sw = new StringWriter(5000);
            ioe.printStackTrace(new PrintWriter(sw));
            detailTA.setText(sw.toString());
            detailWindow.show();
          }
        }
      });
      buttPanel.add(eosdump);

    }

    boolean process(Object o) {
      String command = (String) o;
      boolean err = false;

      try {
        if (raf != null)
          raf.close();
        raf = new ucar.unidata.io.RandomAccessFile(command, "r");

        hdf5Table.setHdf5File(raf);

      } catch (FileNotFoundException ioe) {
        JOptionPane.showMessageDialog(null, "Hdf5ObjectTable cant open " + command + "\n" + ioe.getMessage());
        err = true;

      } catch (Exception e) {
        StringWriter sw = new StringWriter(5000);
        e.printStackTrace(new PrintWriter(sw));
        detailTA.setText(sw.toString());
        detailWindow.show();
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
  private class Hdf5DataPanel extends OpPanel {
    ucar.unidata.io.RandomAccessFile raf = null;
    Hdf5DataTable hdf5Table;

    void closeOpenFiles() throws IOException {
      hdf5Table.closeOpenFiles();
    }

    Hdf5DataPanel(PreferencesExt p) {
      super(p, "file:", true, false);
      hdf5Table = new Hdf5DataTable(prefs, buttPanel);
      add(hdf5Table, BorderLayout.CENTER);

      AbstractButton infoButton = BAMutil.makeButtcon("Information", "Detail Info", false);
      infoButton.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          Formatter f = new Formatter();
          try {
            hdf5Table.showInfo(f);

          } catch (IOException ioe) {
            StringWriter sw = new StringWriter(5000);
            ioe.printStackTrace(new PrintWriter(sw));
            detailTA.setText(sw.toString());
            detailWindow.show();
            return;
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

      try {
        if (raf != null)
          raf.close();
        raf = new ucar.unidata.io.RandomAccessFile(command, "r");

        hdf5Table.setHdf5File(raf);

      } catch (FileNotFoundException ioe) {
        JOptionPane.showMessageDialog(null, "Hdf5DataTable cant open " + command + "\n" + ioe.getMessage());
        err = true;

      } catch (Exception e) {
        StringWriter sw = new StringWriter(5000);
        e.printStackTrace(new PrintWriter(sw));
        detailTA.setText(sw.toString());
        detailWindow.show();
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

    void closeOpenFiles() throws IOException {
      hdf4Table.closeOpenFiles();
    }

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
            StringWriter sw = new StringWriter(5000);
            ioe.printStackTrace(new PrintWriter(sw));
            detailTA.setText(sw.toString());
            detailWindow.show();
          }
        }
      });
      buttPanel.add(eosdump);
    }

    boolean process(Object o) {
      String command = (String) o;
      boolean err = false;

      try {
        if (raf != null)
          raf.close();
        raf = new ucar.unidata.io.RandomAccessFile(command, "r");

        hdf4Table.setHdf4File(raf);

      } catch (FileNotFoundException ioe) {
        JOptionPane.showMessageDialog(null, "NetcdfDataset cant open " + command + "\n" + ioe.getMessage());
        err = true;

      } catch (Exception e) {
        StringWriter sw = new StringWriter(5000);
        e.printStackTrace(new PrintWriter(sw));
        detailTA.setText(sw.toString());
        detailWindow.show();
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

  private class NcmlEditorPanel extends OpPanel {
    NcmlEditor editor;

    void closeOpenFiles() throws IOException {
      editor.closeOpenFiles();
    }

    NcmlEditorPanel(PreferencesExt p) {
      super(p, "dataset:", true, false);
      editor = new NcmlEditor(buttPanel, prefs);
      add(editor, BorderLayout.CENTER);
    }

    boolean process(Object o) {
      return editor.setNcml((String) o);
    }

    void save() {
      super.save();
      editor.save();
    }
  }

  ////////////////////////////////////////////////////////

  private class CdmrFeature extends OpPanel {
    CdmrFeaturePanel panel;

    CdmrFeature(PreferencesExt p) {
      super(p, "file:", true, false);
      panel = new CdmrFeaturePanel(prefs);
      add(panel, BorderLayout.CENTER);

      AbstractAction infoAction = new AbstractAction() {
        public void actionPerformed(ActionEvent e) {
          Formatter f = new Formatter();
          try {
            panel.showInfo(f);

          } catch (Exception ioe) {
            StringWriter sw = new StringWriter(5000);
            ioe.printStackTrace(new PrintWriter(sw));
            detailTA.setText(sw.toString());
            detailWindow.show();
            return;
          }
          detailTA.setText(f.toString());
          detailTA.gotoTop();
          detailWindow.show();
        }
      };
      BAMutil.setActionProperties(infoAction, "Information", "show Info", false, 'I', -1);
      BAMutil.addActionToContainer(buttPanel, infoAction);
    }

    boolean process(Object o) {
      String command = (String) o;
      boolean err = false;

      try {
        panel.setNcStream(command);

      } catch (FileNotFoundException ioe) {
        JOptionPane.showMessageDialog(null, "CdmremotePanel cant open " + command + "\n" + ioe.getMessage());
        err = true;

      } catch (Exception e) {
        StringWriter sw = new StringWriter(5000);
        e.printStackTrace(new PrintWriter(sw));
        detailTA.setText(sw.toString());
        detailWindow.show();
        err = true;
      }

      return !err;
    }

    void save() {
      panel.save();
      super.save();
    }

    void closeOpenFiles() throws IOException {
      panel.closeOpenFiles();
    }
  }

  ////////////////////////////////////////////////////////

  private class NcStreamPanel extends OpPanel {
    ucar.nc2.ui.NcStreamPanel panel;

    NcStreamPanel(PreferencesExt p) {
      super(p, "file:", true, false);
      panel = new ucar.nc2.ui.NcStreamPanel(prefs);
      add(panel, BorderLayout.CENTER);

      AbstractAction infoAction = new AbstractAction() {
        public void actionPerformed(ActionEvent e) {
          Formatter f = new Formatter();
          try {
            panel.showInfo(f);

          } catch (Exception ioe) {
            StringWriter sw = new StringWriter(5000);
            ioe.printStackTrace(new PrintWriter(sw));
            detailTA.setText(sw.toString());
            detailWindow.show();
            return;
          }
          detailTA.setText(f.toString());
          detailTA.gotoTop();
          detailWindow.show();
        }
      };
      BAMutil.setActionProperties(infoAction, "Information", "show Info", false, 'I', -1);
      BAMutil.addActionToContainer(buttPanel, infoAction);
    }

    boolean process(Object o) {
      String command = (String) o;
      boolean err = false;

      try {
        panel.setNcStreamFile(command);

      } catch (FileNotFoundException ioe) {
        JOptionPane.showMessageDialog(null, "CdmremotePanel cant open " + command + "\n" + ioe.getMessage());
        err = true;

      } catch (Exception e) {
        StringWriter sw = new StringWriter(5000);
        e.printStackTrace(new PrintWriter(sw));
        detailTA.setText(sw.toString());
        detailWindow.show();
        err = true;
      }

      return !err;
    }

    void save() {
      panel.save();
      super.save();
    }

    void closeOpenFiles() throws IOException {
      panel.closeOpenFiles();
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
            StringWriter sw = new StringWriter(5000);
            e1.printStackTrace(new PrintWriter(sw));
            f.format("%s", sw.toString());
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

      AbstractButton viewButton = BAMutil.makeButtcon("Dump", "Show Dataset", false);
      viewButton.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          try {
            table.showDataset();

          } catch (IOException e1) {
            StringWriter sw = new StringWriter(5000);
            e1.printStackTrace(new PrintWriter(sw));
            detailTA.setText(sw.toString());
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
        StringWriter sw = new StringWriter(5000);
        ioe.printStackTrace(new PrintWriter(sw));
        detailTA.setText(sw.toString());
        detailTA.gotoTop();
        detailWindow.show();
      }

      return false;
    }

    void save() {
      table.save();
      super.save();
    }

    void closeOpenFiles() throws IOException {
      table.closeOpenFiles();
    }
  }

  // new Fmrc Collection Metadata storage in bdb
  private class FmrcCollectionPanel extends OpPanel {
    FmrcCollectionTable table;

    FmrcCollectionPanel(PreferencesExt dbPrefs) {
      super(dbPrefs, "collection:", true, false);
      table = new FmrcCollectionTable(prefs);
      add(table, BorderLayout.CENTER);

      AbstractButton infoButton = BAMutil.makeButtcon("Information", "Detail Info", false);
      infoButton.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          Formatter f = new Formatter();
          try {
            table.showInfo(f);
          } catch (IOException e1) {
            StringWriter sw = new StringWriter(5000);
            e1.printStackTrace(new PrintWriter(sw));
            f.format("%s", sw.toString());
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
            StringWriter sw = new StringWriter(5000);
            e1.printStackTrace(new PrintWriter(sw));
            f.format("%s", sw.toString());
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
        StringWriter sw = new StringWriter(5000);
        ioe.printStackTrace(new PrintWriter(sw));
        detailTA.setText(sw.toString());
        detailTA.gotoTop();
        detailWindow.show();
      }

      return false;
    }

    void closeOpenFiles() {
    }

    void save() {
      table.save();
      super.save();
    }
  }

  /////////////////////////////////

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

      dsTable.addExtra(buttPanel, fileChooser);
    }

    private void makeGridUI() {
      // a little tricky to get the parent right for GridUI
      viewerWindow = new IndependentWindow("Grid Viewer", BAMutil.getImage("netcdfUI"));

      gridUI = new GridUI((PreferencesExt) prefs.node("GridUI"), viewerWindow, fileChooser, 800);
      gridUI.addMapBean(new WorldMapBean());
      gridUI.addMapBean(new ShapeFileBean("WorldDetailMap", "Global Detailed Map", "WorldDetailMap", WorldDetailMap));
      gridUI.addMapBean(new ShapeFileBean("USDetailMap", "US Detailed Map", "USMap", USMap));

      viewerWindow.setComponent(gridUI);
      Rectangle bounds = (Rectangle) mainPrefs.getBean(GRIDVIEW_FRAME_SIZE, new Rectangle(77, 22, 700, 900));
      if (bounds.x < 0) bounds.x = 0;
      if (bounds.y < 0) bounds.x = 0;
      viewerWindow.setBounds(bounds);
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
      try {
        newds = NetcdfDataset.openDataset(command, true, null);
        if (newds == null) {
          JOptionPane.showMessageDialog(null, "NetcdfDataset.open cant open " + command);
          return false;
        }
        setDataset(newds);

      } catch (FileNotFoundException ioe) {
        JOptionPane.showMessageDialog(null, "NetcdfDataset.open cant open " + command + "\n" + ioe.getMessage());
        //ioe.printStackTrace();
        err = true;

      } catch (Throwable ioe) {
        ioe.printStackTrace();
        StringWriter sw = new StringWriter(5000);
        ioe.printStackTrace(new PrintWriter(sw));
        detailTA.setText(sw.toString());
        detailWindow.show();
        err = true;
      }

      return !err;
    }

    void closeOpenFiles() throws IOException {
      if (ds != null) ds.close();
      ds = null;
      dsTable.clear();
      if (gridUI != null) gridUI.clear();
    }

    void setDataset(NetcdfDataset newds) {
      if (newds == null) return;
      try {
        if (ds != null) ds.close();
      } catch (IOException ioe) {
        System.out.printf("close failed %n");
      }

      Formatter parseInfo = new Formatter();
      this.ds = newds;
      try {
        dsTable.setDataset(newds, parseInfo);
      } catch (IOException e) {
        String info = parseInfo.toString();
        if (info.length() > 0) {
          detailTA.setText(info);
          detailWindow.show();
        }
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
        System.out.printf("close failed %n");
      }

      this.ds = (NetcdfDataset) gds.getNetcdfFile(); // ??
      try {
        dsTable.setDataset(gds);
      } catch (IOException e) {
        e.printStackTrace();
        return;
      }
      setSelectedItem(gds.getLocation());
    }

    void save() {
      super.save();
      dsTable.save();
      if (gridUI != null) gridUI.storePersistentData();
      if (viewerWindow != null) mainPrefs.putBeanObject(GRIDVIEW_FRAME_SIZE, viewerWindow.getBounds());
      if (imageWindow != null) mainPrefs.putBeanObject(GRIDIMAGE_FRAME_SIZE, imageWindow.getBounds());
    }

  }

  /////////////////////////////////

  private class CoveragePanel extends OpPanel {
    ucar.nc2.ui.coverage2.CoverageTable dsTable;
    CoverageViewer display;
    JSplitPane split;
    IndependentWindow viewerWindow;

    FeatureDatasetCoverage covDatasetCollection = null;

    CoveragePanel(PreferencesExt prefs) {
      super(prefs, "dataset:", true, false);
      dsTable = new ucar.nc2.ui.coverage2.CoverageTable(buttPanel, prefs);
      add(dsTable, BorderLayout.CENTER);

      AbstractButton viewButton = BAMutil.makeButtcon("alien", "Grid Viewer", false);
      viewButton.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          CoverageCollection gridDataset = dsTable.getCoverageDataset();
          if (gridDataset == null) return;
          if (display == null) makeDisplay();
          display.setDataset(dsTable);
          viewerWindow.show();
        }
      });
      buttPanel.add(viewButton);

      AbstractButton infoButton = BAMutil.makeButtcon("Information", "Show Info", false);
      infoButton.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          Formatter f = new Formatter();
          dsTable.showInfo(f);
          detailTA.setText(f.toString());
          detailTA.gotoTop();
          detailWindow.show();
        }
      });
      buttPanel.add(infoButton);

      //dsTable.addExtra(buttPanel, fileChooser);
    }

    private void makeDisplay() {
      viewerWindow = new IndependentWindow("Coverage Viewer", BAMutil.getImage("netcdfUI"));

      display = new CoverageViewer((PreferencesExt) prefs.node("CoverageDisplay"), viewerWindow, fileChooser, 800);
      display.addMapBean(new WorldMapBean());
      display.addMapBean(new ShapeFileBean("WorldDetailMap", "Global Detailed Map", "WorldDetailMap", WorldDetailMap));
      display.addMapBean(new ShapeFileBean("USDetailMap", "US Detailed Map", "USMap", USMap));

      viewerWindow.setComponent(display);
      Rectangle bounds = (Rectangle) mainPrefs.getBean(GRIDVIEW_FRAME_SIZE, new Rectangle(77, 22, 700, 900));
      if (bounds.x < 0) bounds.x = 0;
      if (bounds.y < 0) bounds.x = 0;
      viewerWindow.setBounds(bounds);
    }

    boolean process(Object o) {
      String command = (String) o;
      boolean err = false;

      // close previous file
      try {
        closeOpenFiles();
      } catch (IOException ioe) {
        System.out.printf("close failed %n");
      }

      try {
        ucar.nc2.util.Optional<FeatureDatasetCoverage> opt = CoverageDatasetFactory.openCoverageDataset(command);
        if (!opt.isPresent()) {
          JOptionPane.showMessageDialog(null, opt.getErrorMessage());
          return false;
        }
        covDatasetCollection = opt.get();
        if (covDatasetCollection == null) return false;
        dsTable.setCollection(covDatasetCollection);
        setSelectedItem(command);

      } catch (IOException e) {
        // e.printStackTrace();
        JOptionPane.showMessageDialog(null, String.format("CdmrFeatureDataset2.open cant open %s err=%s", command, e.getMessage()));

      } catch (Throwable ioe) {
        ioe.printStackTrace();
        StringWriter sw = new StringWriter(5000);
        ioe.printStackTrace(new PrintWriter(sw));
        detailTA.setText(sw.toString());
        detailWindow.show();
        err = true;
      }

      return !err;
    }

    void setDataset(FeatureDataset fd) {
      if (fd == null) return;
      if (!(fd instanceof FeatureDatasetCoverage)) return;

      try {
        closeOpenFiles();
      } catch (IOException ioe) {
        System.out.printf("close failed %n");
      }

      dsTable.setCollection( (FeatureDatasetCoverage) fd);
      setSelectedItem(fd.getLocation());
    }

    void closeOpenFiles() throws IOException {
      if (covDatasetCollection != null) covDatasetCollection.close();
      covDatasetCollection = null;
      dsTable.clear();
    }

    void save() {
      super.save();
      dsTable.save();
      if (viewerWindow != null) mainPrefs.putBeanObject(GRIDVIEW_FRAME_SIZE, viewerWindow.getBounds());
    }

  }

  /////////////////////////////////////////////////

  private class RadialPanel extends OpPanel {
    RadialDatasetTable dsTable;
    JSplitPane split;

    RadialDatasetSweep ds = null;

    RadialPanel(PreferencesExt prefs) {
      super(prefs, "dataset:", true, false);
      dsTable = new RadialDatasetTable(prefs);
      add(dsTable, BorderLayout.CENTER);

      AbstractButton infoButton = BAMutil.makeButtcon("Information", "Parse Info", false);
      infoButton.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          RadialDatasetSweep radialDataset = dsTable.getRadialDataset();
          Formatter info = new Formatter();
          radialDataset.getDetailInfo(info);
          detailTA.setText(info.toString());
          detailTA.gotoTop();
          detailWindow.show();
        }
      });
      buttPanel.add(infoButton);
    }

    boolean process(Object o) {
      String command = (String) o;
      boolean err = false;

      NetcdfDataset newds;
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
        ioe.printStackTrace();
        err = true;

      } catch (IOException ioe) {
        StringWriter sw = new StringWriter(5000);
        ioe.printStackTrace(new PrintWriter(sw));
        detailTA.setText(sw.toString());
        detailWindow.show();
        err = true;
      }

      return !err;
    }

    void setDataset(ucar.nc2.dt.RadialDatasetSweep newds) {
      if (newds == null) return;
      try {
        if (ds != null) ds.close();
      } catch (IOException ioe) {
        System.out.printf("close failed %n");
      }

      this.ds = newds;
      dsTable.setDataset(newds);
      setSelectedItem(newds.getLocation());
    }

    void closeOpenFiles() throws IOException {
      if (ds != null) ds.close();
      ds = null;
    }

    void save() {
      super.save();
      dsTable.save();
    }

  }

  ///////////////////////////////////////////////////////////
  private class DatasetViewerPanel extends OpPanel {
    DatasetViewer dsViewer;
    JSplitPane split;
    NetcdfFile ncfile = null;
    boolean jni;

    DatasetViewerPanel(PreferencesExt dbPrefs, boolean jni) {
      super(dbPrefs, "dataset:");
      this.jni = jni;

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

      dsViewer.addActions(buttPanel);
    }

    boolean process(Object o) {
      String location = (String) o;
      boolean err = false;
      NetcdfFile ncnew;

      try {
        if (ncfile != null) ncfile.close();
      } catch (IOException ioe) {
        System.out.printf("close failed %n");
      }

      try {
        if (jni) {
          Nc4Iosp iosp = new Nc4Iosp(NetcdfFileWriter.Version.netcdf4);
          ncnew = new NetcdfFileSubclass(iosp, location);
          ucar.unidata.io.RandomAccessFile raf = new ucar.unidata.io.RandomAccessFile(location, "r");
          iosp.open(raf, ncnew, null);
        } else {
          ncnew = openFile(location, addCoords, null);
        }
        if (ncnew != null)
          setDataset(ncnew);

      } catch (Exception ioe) {
        StringWriter sw = new StringWriter(5000);
        ioe.printStackTrace(new PrintWriter(sw));
        detailTA.setText(sw.toString());
        detailWindow.show();
        err = true;
      }

      return !err;
    }

    void closeOpenFiles() throws IOException {
      if (ncfile != null) ncfile.close();
      ncfile = null;
      dsViewer.clear();
    }

    void setDataset(NetcdfFile nc) {
      try {
        if (ncfile != null) ncfile.close();
        ncfile = null;
      } catch (IOException ioe) {
        System.out.printf("close failed %n");
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

  ///////////////////////////////////////////////////////////
  private class DatasetWriterPanel extends OpPanel {
    DatasetWriter dsWriter;
    JSplitPane split;
    NetcdfFile ncfile = null;

    DatasetWriterPanel(PreferencesExt dbPrefs) {
      super(dbPrefs, "dataset:");
      dsWriter = new DatasetWriter(dbPrefs, fileChooser);
      add(dsWriter, BorderLayout.CENTER);
      dsWriter.addActions(buttPanel);
    }

    boolean process(Object o) {
      String command = (String) o;
      boolean err = false;

      try {
        if (ncfile != null) ncfile.close();
      } catch (IOException ioe) {
        System.out.printf("close failed %n");
      }

      try {
        NetcdfFile ncnew = openFile(command, addCoords, null);
        if (ncnew != null)
          setDataset(ncnew);

      } catch (Exception ioe) {
        StringWriter sw = new StringWriter(5000);
        ioe.printStackTrace(new PrintWriter(sw));
        detailTA.setText(sw.toString());
        detailWindow.show();
        err = true;
      }

      return !err;
    }

    void closeOpenFiles() throws IOException {
      if (ncfile != null) ncfile.close();
      ncfile = null;
    }

    void setDataset(NetcdfFile nc) {
      try {
        if (ncfile != null) ncfile.close();
        ncfile = null;
      } catch (IOException ioe) {
        System.out.printf("close failed %n");
      }
      ncfile = nc;

      if (ncfile != null) {
        dsWriter.setDataset(nc);
        setSelectedItem(nc.getLocation());
      }
    }

    void save() {
      super.save();
      dsWriter.save();
    }

  }

  /////////////////////////////////////////////////////////////////////
  private class FeatureScanPanel extends OpPanel {
    ucar.nc2.ui.FeatureScanPanel ftTable;
    final FileManager dirChooser;

    FeatureScanPanel(PreferencesExt prefs) {
      super(prefs, "dir:", false, false);
      dirChooser = new FileManager(parentFrame, null, null, (PreferencesExt) prefs.node("FeatureScanFileManager"));
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
          } else if (e.getPropertyName().equals("openCoverageDataset")) {
            String datasetName = (String) e.getNewValue();
            openCoverageDataset(datasetName);
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

    void closeOpenFiles() {
      ftTable.clear();
    }

    void save() {
      dirChooser.save();
      ftTable.save();
      prefs.put("currDir", dirChooser.getCurrentDirectory());
      super.save();
    }
  }

  //////////////////////////////////////////////////////////////////////////////////////

  private class CollectionSpecPanel extends OpPanel {
    CollectionSpecTable table;

    CollectionSpecPanel(PreferencesExt dbPrefs) {
      super(dbPrefs, "collection spec:", true, false);
      table = new CollectionSpecTable(prefs);
      add(table, BorderLayout.CENTER);

      AbstractButton infoButton = BAMutil.makeButtcon("Information", "Detail Info", false);
      infoButton.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          Formatter f = new Formatter();
          try {
            table.showCollection(f);
          } catch (Exception e1) {
            StringWriter sw = new StringWriter(5000);
            e1.printStackTrace(new PrintWriter(sw));
            f.format("%s", sw.toString());
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
      if (command == null) return false;

      try {
        table.setCollection(command);
        return true;

      } catch (Exception ioe) {
        StringWriter sw = new StringWriter(5000);
        ioe.printStackTrace(new PrintWriter(sw));
        detailTA.setText(sw.toString());
        detailTA.gotoTop();
        detailWindow.show();
      }

      return false;
    }

    void closeOpenFiles() {
    }

    void save() {
      table.save();
      super.save();
    }
  }

  private class DirectoryPartitionPanel extends OpPanel {
    DirectoryPartitionViewer table;

    DirectoryPartitionPanel(PreferencesExt dbPrefs) {
      super(dbPrefs, "collection:", false, false, false);
      table = new DirectoryPartitionViewer(prefs, topPanel, buttPanel);
      add(table, BorderLayout.CENTER);

      table.addPropertyChangeListener(new java.beans.PropertyChangeListener() {
        public void propertyChange(java.beans.PropertyChangeEvent e) {
          if (e.getPropertyName().equals("openGrib2Collection")) {
            String collectionName = (String) e.getNewValue();
            openGrib2Collection(collectionName);
          }
        }
      });
    }

    boolean process(Object o) {
      String command = (String) o;
      if (command == null) return false;

      try {
        //table.setCollectionFromConfig(command);
        return true;

      } catch (Exception ioe) {
        ioe.printStackTrace();
        StringWriter sw = new StringWriter(5000);
        ioe.printStackTrace(new PrintWriter(sw));
        detailTA.setText(sw.toString());
        detailTA.gotoTop();
        detailWindow.show();
      }

      return false;
    }

    void closeOpenFiles() {
    }

    void save() {
      table.save();
      super.save();
      table.clear();
    }
  }

////////////////////////////////////////////////////////////////////////

  private class PointFeaturePanel extends OpPanel {
    PointFeatureDatasetViewer pfViewer;
    JSplitPane split;
    FeatureDatasetPoint pfDataset = null;
    JComboBox<FeatureType> types;

    PointFeaturePanel(PreferencesExt dbPrefs) {
      super(dbPrefs, "dataset:", true, false);
      pfViewer = new PointFeatureDatasetViewer(dbPrefs, buttPanel);
      add(pfViewer, BorderLayout.CENTER);

      types = new JComboBox<>();
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

      AbstractButton calcButton = BAMutil.makeButtcon("V3", "Calculate the latlon/dateRange", false);
      calcButton.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          if (pfDataset == null) return;
          Formatter f = new Formatter();
          pfDataset.calcBounds(f);
          detailTA.setText(f.toString());
          detailTA.gotoTop();
          detailWindow.show();
        }
      });
      buttPanel.add(calcButton);

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
    }

    boolean process(Object o) {
      String location = (String) o;
      return setPointFeatureDataset((FeatureType) types.getSelectedItem(), location);
    }

    void closeOpenFiles() throws IOException {
      if (pfDataset != null) pfDataset.close();
      pfDataset = null;
      pfViewer.clear();
    }

    void save() {
      super.save();
      pfViewer.save();
    }

    private boolean setPointFeatureDataset(FeatureType type, String location) {
      if (location == null) return false;

      try {
        if (pfDataset != null) pfDataset.close();
      } catch (IOException ioe) {
        System.out.printf("close failed %n");
      }
      detailTA.clear();

      Formatter log = new Formatter();
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
        StringWriter sw = new StringWriter(5000);
        e.printStackTrace(new PrintWriter(sw));
        detailTA.setText(log.toString());
        detailTA.setText(sw.toString());
        detailWindow.show();

        JOptionPane.showMessageDialog(this, e.getMessage());
        return false;
      }
    }

    private boolean setPointFeatureDataset(FeatureDatasetPoint pfd) {

      try {
        if (pfDataset != null) pfDataset.close();
      } catch (IOException ioe) {
        System.out.printf("close failed %n");
      }
      detailTA.clear();

      try {
        pfDataset = pfd;
        pfViewer.setDataset(pfDataset);
        setSelectedItem(pfDataset.getLocation());
        return true;

      } catch (Throwable e) {
        StringWriter sw = new StringWriter(5000);
        e.printStackTrace(new PrintWriter(sw));
        detailTA.setText(sw.toString());
        detailWindow.show();

        JOptionPane.showMessageDialog(this, e.getMessage());
        return false;
      }
    }

    private String getCapabilities(FeatureDatasetPoint fdp) {
      FeatureDatasetCapabilitiesWriter xmlWriter = new FeatureDatasetCapabilitiesWriter(fdp, null);
      return xmlWriter.getCapabilities();
    }
  }

  private class WmsPanel extends OpPanel {
    WmsViewer wmsViewer;
    JSplitPane split;
    JComboBox<String> types;

    WmsPanel(PreferencesExt dbPrefs) {
      super(dbPrefs, "dataset:", true, false);
      wmsViewer = new WmsViewer(dbPrefs, frame);
      add(wmsViewer, BorderLayout.CENTER);

      buttPanel.add(new JLabel("version:"));
      types = new JComboBox<>();
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

    void closeOpenFiles() {
    }

    void save() {
      super.save();
      wmsViewer.save();
    }
  }

  private class StationRadialPanel extends OpPanel {
    StationRadialViewer radialViewer;
    JSplitPane split;
    FeatureDataset radarCollectionDataset = null;

    StationRadialPanel(PreferencesExt dbPrefs) {
      super(dbPrefs, "dataset:", true, false);
      radialViewer = new StationRadialViewer(dbPrefs);
      add(radialViewer, BorderLayout.CENTER);

      AbstractButton infoButton = BAMutil.makeButtcon("Information", "Dataset Info", false);
      infoButton.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          if (radarCollectionDataset != null) {
            Formatter info = new Formatter();
            radarCollectionDataset.getDetailInfo(info);
            detailTA.setText(info.toString());
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

    void closeOpenFiles() throws IOException {
      if (radarCollectionDataset != null) radarCollectionDataset.close();
      radarCollectionDataset = null;
    }

    void save() {
      super.save();
      radialViewer.save();
    }

    boolean setStationRadialDataset(String location) {
      if (location == null) return false;

      try {
        if (radarCollectionDataset != null) radarCollectionDataset.close();
      } catch (IOException ioe) {
        System.out.printf("close failed %n");
      }

      DataFactory.Result result = null;
      try {
        result = threddsDataFactory.openFeatureDataset(FeatureType.STATION_RADIAL, location, null);
        if (result.fatalError) {
          JOptionPane.showMessageDialog(null, "Can't open " + location + ": " + result.errLog.toString());
          return false;
        }

        setStationRadialDataset(result.featureDataset);
        return true;

      } catch (Exception e) {
        StringWriter sw = new StringWriter(5000);
        e.printStackTrace(new PrintWriter(sw));
        detailTA.setText(log.toString());
        detailTA.appendLine(sw.toString());
        detailWindow.show();

        JOptionPane.showMessageDialog(this, e.getMessage());
        if (result != null) {
          try {
            result.close();
          } catch (IOException ioe2) {
            JOptionPane.showMessageDialog(null, "Can't open " + location + ": " + ioe2.getMessage());
          }
        }
        return false;
      }
    }

    boolean setStationRadialDataset(FeatureDataset dataset) {
      if (dataset == null) return false;

      try {
        if (radarCollectionDataset != null) radarCollectionDataset.close();
      } catch (IOException ioe) {
        System.out.printf("close failed %n");
      }

      radarCollectionDataset = dataset;
      radialViewer.setDataset(radarCollectionDataset);
      setSelectedItem(radarCollectionDataset.getLocation());
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

      try {
        if (null != command)
          imagePanel.setImageFromUrl(command);

      } catch (Exception ioe) {
        ioe.printStackTrace();
        StringWriter sw = new StringWriter(5000);
        ioe.printStackTrace(new PrintWriter(sw));
        detailTA.setText(sw.toString());
        detailWindow.show();
        return false;
      }

      return true;
    }

    void setImageLocation(String location) {
      imagePanel.setImageFromUrl(location);
      setSelectedItem(location);
    }

    void closeOpenFiles() throws IOException {
    }

  }

  private class GeotiffPanel extends OpPanel {
    TextHistoryPane ta;

    GeotiffPanel(PreferencesExt p) {
      super(p, "netcdf:", true, false);

      ta = new TextHistoryPane(true);
      add(ta, BorderLayout.CENTER);

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
          System.out.printf("close failed %n");
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
          System.out.printf("close failed %n");
        }
      }
    }

    void closeOpenFiles() throws IOException {
    }

  }

  private interface GetDataRunnable {
    void run(Object o) throws IOException;
  }

  private static class GetDataTask extends ProgressMonitorTask implements ucar.nc2.util.CancelTask {
    GetDataRunnable getData;
    Object o;
    String name, errMsg = null;

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
        StringWriter sw = new StringWriter(5000);
        e.printStackTrace(new PrintWriter(sw));
        errMsg = sw.toString();
        success = false;
        done = true;
        return;
      }

      success = true;
      done = true;
    }
  }

///////////////////////////////////////////////////////////////////////////////
// Dynamic proxy for Debug

  private static class DebugProxyHandler implements java.lang.reflect.InvocationHandler {
    @Override
    public Object invoke(Object proxy, java.lang.reflect.Method method, Object[] args) throws Throwable {
      if (method.getName().equals("toString"))
        return super.toString();
      // System.out.println("proxy= "+proxy+" method = "+method+" args="+args);
      if (method.getName().equals("isSet")) {
        return Debug.isSet((String) args[0]);
      }
      if (method.getName().equals("set")) {
        ucar.util.prefs.ui.Debug.set((String) args[0], (Boolean) args[1]);
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
              "<br><b><i>Developers:</b>John Caron, Sean Arms, Dennis Heimbinger, Ryan May, Christian Ward-Garrison</i></b>" +
              "</center>" +
              "<br><br>With thanks to these <b>Open Source</b> contributors:" +
              "<ul>" +
              "<li><b>ADDE/VisAD</b>: Bill Hibbard, Don Murray, Tom Whittaker, et al (http://www.ssec.wisc.edu/~billh/visad.html)</li>" +
              "<li><b>Apache HTTP Components</b> libraries: (http://hc.apache.org/)</li>" +
              "<li><b>Apache Jakarta Commons</b> libraries: (http://jakarta.apache.org/commons/)</li>" +
              "<li><b>IDV:</b> Yuan Ho, Julien Chastang, Don Murray, Jeff McWhirter, Yuan H (http://www.unidata.ucar.edu/software/IDV/)</li>" +
              "<li><b>Joda Time</b> library: Stephen Colebourne (http://www.joda.org/joda-time/)</li>" +
              "<li><b>JDOM</b> library: Jason Hunter, Brett McLaughlin et al (www.jdom.org)</li>" +
              "<li><b>JGoodies</b> library: Karsten Lentzsch (www.jgoodies.com)</li>" +
              "<li><b>JPEG-2000</b> Java library: (http://www.jpeg.org/jpeg2000/)</li>" +
              "<li><b>JUnit</b> library: Erich Gamma, Kent Beck, Erik Meade, et al (http://sourceforge.net/projects/junit/)</li>" +
              "<li><b>NetCDF C Library</b> library: Russ Rew, Ward Fisher, Dennis Heimbinger</li>" +
              "<li><b>OPeNDAP Java</b> library: Dennis Heimbinger, James Gallagher, Nathan Potter, Don Denbo, et. al.(http://opendap.org)</li>" +
              "<li><b>Protobuf serialization</b> library: Google (http://code.google.com/p/protobuf/)</li>" +
              "<li><b>Simple Logging Facade for Java</b> library: Ceki Gulcu (http://www.slf4j.org/)</li>" +
              "<li><b>Spring lightweight framework</b> library: Rod Johnson, et. al.(http://www.springsource.org/)</li>" +
              "<li><b>Imaging utilities:</b>: Richard Eigenmann</li>" +
              "<li><b>Udunits:</b>: Steve Emmerson</li>" +
              "</ul><center>Special thanks to <b>Sun/Oracle</b> (java.oracle.com) for the platform on which we stand." +
              "</center></body></html> ");

      JPanel main = new JPanel(new BorderLayout());
      main.setBorder(new javax.swing.border.LineBorder(Color.BLACK));
      main.setBackground(new Color(0xFFECEC));

      JLabel ring = new JLabel(new ImageIcon(BAMutil.getImage("netcdfUI")));
      ring.setOpaque(true);
      ring.setBackground(new Color(0xFFECEC));

      JLabel threddsLogo = new JLabel(Resource.getIcon(BAMutil.getResourcePath() + "cdm.png", false));
      threddsLogo.setBackground(new Color(0xFFECEC));
      threddsLogo.setOpaque(true);

      main.add(threddsLogo, BorderLayout.NORTH);
      main.add(lab1, BorderLayout.CENTER);
      main.add(ring, BorderLayout.SOUTH);
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
    try (InputStream is = ucar.nc2.ui.util.Resource.getFileResource("/README")) {
      if (is == null) return "5.0";
      BufferedReader dataIS = new BufferedReader(new InputStreamReader(is, CDM.utf8Charset));
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
  private static class MySplashScreen extends javax.swing.JWindow {
    public MySplashScreen() {
      Image image = Resource.getImage("/resources/nj22/ui/pix/ring2.jpg");
      if (image != null) {
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
  }


  //////////////////////////////////////////////////////////////////////////
  static private void exit() {
    doSavePrefsAndUI();
    System.exit(0);
  }

  static private void doSavePrefsAndUI()  {
    ui.save();
    Rectangle bounds = frame.getBounds();
    prefs.putBeanObject(FRAME_SIZE, bounds);
    try {
      store.save();
    } catch (IOException ioe) {
      ioe.printStackTrace();
    }

    done = true; // on some systems, still get a window close event
    ucar.nc2.util.cache.FileCacheIF cache = NetcdfDataset.getNetcdfFileCache();
    if (cache != null)
      cache.clearCache(true);
    FileCache.shutdown(); // shutdown threads
    DiskCache2.exit(); // shutdown threads
    MetadataManager.closeAll(); // shutdown bdb
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
        int pos = wantDataset.indexOf('#');
        if (pos > 0) {
          String catName = wantDataset.substring(0, pos); // {catalog}#{dataset}
          if (catName.endsWith(".xml")) {
            ui.makeComponent(null, "THREDDS");
            ui.threddsUI.setDataset(wantDataset);
            ui.tabbedPane.setSelectedComponent(ui.threddsUI);
          }
          return;
        }

        // default
        ui.openNetcdfFile(wantDataset);
      }
    });
  }

  /////////////////////////////////////////////////////////////////////

  // run this on the event thread
  private static void createGui() {

    if (isMacOs) {
      System.setProperty("apple.laf.useScreenMenuBar", "true");
      // fixes the case where users on a mac use the system bar to quit rather than
      // closing a window using the 'x' button.
      Runtime.getRuntime().addShutdownHook(new Thread() {
        @Override
        public void run() {
          doSavePrefsAndUI();
        }
      });
    } else {
      try {
        // Switch to Nimbus Look and Feel, if it's available.
        for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
          if ("Nimbus".equals(info.getName())) {
            UIManager.setLookAndFeel(info.getClassName());
            break;
          }
        }
      } catch (ClassNotFoundException | InstantiationException | IllegalAccessException |
              UnsupportedLookAndFeelException e) {
        log.warn("Found Nimbus Look and Feel, but couldn't install it.", e);
      }
    }

    // get a splash screen up right away
    final MySplashScreen splash = new MySplashScreen();

    // misc initializations
    BAMutil.setResourcePath("/resources/nj22/ui/icons/");

    // test
    // java.util.logging.Logger.getLogger("ucar.nc2").setLevel( java.util.logging.Level.SEVERE);

    // put UI in a JFrame
    frame = new JFrame("NetCDF ("+DIALOG_VERSION+") Tools");
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
    Rectangle have = frame.getGraphicsConfiguration().getBounds();
    Rectangle def = new Rectangle(50, 50, 800, 800);
    Rectangle want = (Rectangle) prefs.getBean(FRAME_SIZE, def);
    if (want.getX() > have.getWidth() - 25) want = def; // may be off screen when switcing between 2 monitor system

    frame.setBounds(want);

    frame.pack();
    frame.setBounds(want);
    frame.setVisible(true);

    // in case a dataset was on the command line
    if (wantDataset != null)
      setDataset();
  }

  static boolean isCacheInit = false;

  public static void main(String args[]) {
    if (debugListen) {
      System.out.println("Arguments:");
      for (String arg : args) {
        System.out.println(" " + arg);
      }
      HTTPSession.setInterceptors(true);
    }

    //////////////////////////////////////////////////////////////////////////
    // handle multiple versions of ToolsUI, along with passing a dataset name
    SocketMessage sm;
    if (args.length > 0) {
      // munge arguments into a single string
      StringBuilder sbuff = new StringBuilder();
      for (String arg : args) {
        sbuff.append(arg);
        sbuff.append(" ");
      }
      String arguments = sbuff.toString();
      System.out.println("ToolsUI arguments=" + arguments);

      wantDataset = arguments;

      // see if another version is running, if so send it the message
      sm = new SocketMessage(14444, wantDataset);
      if (sm.isAlreadyRunning()) {
        System.out.println("ToolsUI already running - pass argument= '" + wantDataset + "' to it and exit");
        System.exit(0);
      }

    } else { // no arguments were passed

      // look for messages from another ToolsUI
      sm = new SocketMessage(14444, null);
      if (sm.isAlreadyRunning()) {
        System.out.println("ToolsUI already running - start up another copy");
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

    if (debugListen) {
      System.out.println("Arguments:");
      for (String arg : args) {
        System.out.println(" " + arg);
      }
      HTTPSession.setInterceptors(true);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////

    // spring initialization
    try (ClassPathXmlApplicationContext springContext =
                 new ClassPathXmlApplicationContext("classpath:resources/nj22/ui/spring/application-config.xml")) {

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
        String filename = XMLStore.makeStandardFilename(".unidata", "nj22Config.xml");
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
        // 4.4
        String prefStore = XMLStore.makeStandardFilename(".unidata", "ToolsUI.xml");
        File prefs44 = new File(prefStore);

        if (!prefs44.exists()) { // if 4.4 doesnt exist, see if 4.3 exists
          String prefStoreBack = XMLStore.makeStandardFilename(".unidata", "NetcdfUI22.xml");
          File prefs43 = new File(prefStoreBack);
          if (prefs43.exists()) { // make a copy of it
            IO.copyFile(prefs43, prefs44);
          }
        }

        // open 4.4 version, create it if doesnt exist
        store = XMLStore.createFromFile(prefStore, null);
        prefs = store.getPreferences();

        Debug.setStore(prefs.node("Debug"));
      } catch (IOException e) {
        System.out.println("XMLStore Creation failed " + e);
      }

      // LOOK needed? for efficiency, persist aggregations. Every hour, delete stuff older than 30 days
      Aggregation.setPersistenceCache(new DiskCache2("/.unidata/aggCache", true, 60 * 24 * 30, 60));

      try {
        // thredds.inventory.bdb.MetadataManager.setCacheDirectory(fcCache, maxSizeBytes, jvmPercent); // use defaults
        thredds.inventory.CollectionManagerAbstract.setMetadataStore(thredds.inventory.bdb.MetadataManager.getFactory());
      } catch (Exception e) {
        log.error("CdmInit: Failed to open CollectionManagerAbstract.setMetadataStore", e);
      }

      UrlAuthenticatorDialog provider = new UrlAuthenticatorDialog(frame);
      try {
        HTTPSession.setGlobalCredentialsProvider(provider);
      }catch (HTTPException e) {
        log.error("Failed to set global credentials");
      }
      HTTPSession.setGlobalUserAgent("ToolsUI v5.0");

      // set Authentication for accessing passsword protected services like TDS PUT
      java.net.Authenticator.setDefault(provider);

      // open dap initializations
      ucar.nc2.dods.DODSNetcdfFile.setAllowCompression(true);
      ucar.nc2.dods.DODSNetcdfFile.setAllowSessions(true);

      // caching
      ucar.unidata.io.RandomAccessFile.enableDefaultGlobalFileCache();
      GribCdmIndex.initDefaultCollectionCache(100, 200, -1);

    /* No longer needed
    HttpClient client = HttpClientManager.init(provider, "ToolsUI");
    opendap.dap.DConnect2.setHttpClient(client);
    HTTPRandomAccessFile.setHttpClient(client);
    CdmRemote.setHttpClient(client);
    NetcdfDataset.setHttpClient(client);
    WmsViewer.setHttpClient(client);
    */

      SwingUtilities.invokeLater(new Runnable() {
        @Override
        public void run() {
          createGui();
        }
      });
    }
  }

}
