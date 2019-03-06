/*
 * Copyright (c) 1998-2019 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.ui;

import org.springframework.context.support.ClassPathXmlApplicationContext;
import thredds.client.catalog.ServiceType;
import thredds.client.catalog.tools.DataFactory;
import thredds.inventory.bdb.MetadataManager;
import thredds.ui.catalog.ThreddsUI;
import ucar.httpservices.HTTPException;
import ucar.httpservices.HTTPSession;
import ucar.nc2.*;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.dataset.*;
import ucar.nc2.dods.DODSNetcdfFile;
import ucar.nc2.dt.GridDataset;
import ucar.nc2.dt.GridDatatype;
import ucar.nc2.dt.RadialDatasetSweep;
import ucar.nc2.ft.FeatureDataset;
import ucar.nc2.ft.point.PointDatasetImpl;
import ucar.nc2.ft2.coverage.*;
import ucar.nc2.grib.GribIndexCache;
import ucar.nc2.grib.collection.GribCdmIndex;
import ucar.nc2.grib.grib2.table.WmoCodeTable;
import ucar.nc2.grib.grib2.table.WmoTemplateTable;
import ucar.nc2.iosp.hdf5.H5iosp;
import ucar.nc2.jni.netcdf.Nc4Iosp;
import ucar.nc2.ncml.Aggregation;
import ucar.nc2.stream.CdmRemote;
import ucar.nc2.ui.coverage2.CoverageViewer;
import ucar.nc2.ui.dialog.DiskCache2Form;
import ucar.nc2.ui.gis.shapefile.ShapeFileBean;
import ucar.nc2.ui.gis.worldmap.WorldMapBean;
import ucar.nc2.ui.grib.*;
import ucar.nc2.ui.grid.GeoGridTable;
import ucar.nc2.ui.grid.GridUI;
import ucar.nc2.ui.image.ImageViewPanel;
import ucar.nc2.ui.menu.*;
import ucar.nc2.ui.op.*;
import ucar.nc2.ui.simplegeom.SimpleGeomTable;
import ucar.nc2.ui.simplegeom.SimpleGeomUI;
import ucar.nc2.ui.util.SocketMessage;
import ucar.nc2.ui.widget.*;
import ucar.nc2.ui.widget.ProgressMonitor;
import ucar.nc2.units.*;
import ucar.nc2.util.*;
import ucar.nc2.util.cache.FileCache;
import ucar.nc2.util.xml.RuntimeConfigParser;
import ucar.unidata.io.RandomAccessFile;
import ucar.util.prefs.PreferencesExt;
import ucar.util.prefs.XMLStore;
import ucar.util.prefs.ui.Debug;

import java.awt.*;
import java.awt.event.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.*;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Proxy;
import java.util.Formatter;
import javax.swing.*;
import javax.swing.filechooser.FileFilter;

/**
 * Netcdf Tools user interface.
 *
 * @author caron
 */
public class ToolsUI extends JPanel {
  private final static org.slf4j.Logger log
                            = org.slf4j.LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private final static String WORLD_DETAIL_MAP = "/resources/nj22/ui/maps/Countries.shp";
  private final static String US_MAP = "/resources/nj22/ui/maps/us_state.shp";

  final static String FRAME_SIZE = "FrameSize";
  private final static String DIALOG_VERSION = "5.0";
  private final static String GRIDVIEW_FRAME_SIZE = "GridUIWindowSize";
  private final static String GRIDIMAGE_FRAME_SIZE = "GridImageWindowSize";
  private static boolean debugListen;

  private PreferencesExt mainPrefs;

  private static ToolsUI ui;
  private static JFrame frame;
  private static PreferencesExt prefs;
  private static XMLStore store;
  private static boolean done;

  private static String wantDataset;

  // UI
  private AggPanel aggPanel;
  private BufrPanel bufrPanel;
  private BufrTableBPanel bufrTableBPanel;
  private BufrTableDPanel bufrTableDPanel;
  private ReportOpPanel bufrReportPanel;
  private BufrCdmIndexOpPanel bufrCdmIndexPanel;
  private BufrCodePanel bufrCodePanel;
  private CdmrFeature cdmremotePanel;
  private CdmIndexOpPanel cdmIndexPanel;
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
  private GribFilesOpPanel gribFilesPanel;
  private GribIndexOpPanel gribIdxPanel;
  private GribRewriteOpPanel gribRewritePanel;
  private GribTemplatePanel gribTemplatePanel;
  private Grib1CollectionPanel grib1CollectionPanel;
  private ReportOpPanel grib1ReportPanel;
  private Grib1TablePanel grib1TablePanel;
  private Grib2CollectionOpPanel grib2CollectionPanel;
  private Grib2TablePanel grib2TablePanel;
  private ReportOpPanel grib2ReportPanel;
  private Grib1DataOpPanel grib1DataPanel;
  private Grib2DataOpPanel grib2DataPanel;
  private Hdf5ObjectPanel hdf5ObjectPanel;
  private Hdf5DataPanel hdf5DataPanel;
  private Hdf4Panel hdf4Panel;
  private ImagePanel imagePanel;
  private NcStreamOpPanel ncStreamPanel;
  private NCdumpPanel ncdumpPanel;
  private NcmlEditorPanel ncmlEditorPanel;
  private PointFeaturePanel pointFeaturePanel;
  private SimpleGeomPanel simpleGeomPanel;
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
  private FileManager bufrFileChooser;

  // data
  private DataFactory threddsDataFactory = new DataFactory();

  private boolean useRecordStructure;
  private DiskCache2Form diskCache2Form;

  // debugging
  private DebugFlags debugFlags;

  public ToolsUI(PreferencesExt prefs, JFrame parentFrame) {
    this.mainPrefs = prefs;
    this.parentFrame = parentFrame;

    // FileChooser is shared
    FileFilter[] filters = new FileFilter[]{ new FileManager.HDF5ExtFilter(),
                                             new FileManager.NetcdfExtFilter() };
    fileChooser = new FileManager(parentFrame, null, filters, (PreferencesExt) prefs.node("FileManager"));

    OpPanel.setFileChooser(fileChooser);

    // all the tabbed panes
    tabbedPane   = new JTabbedPane(JTabbedPane.TOP);
    iospTabPane  = new JTabbedPane(JTabbedPane.TOP);
    gribTabPane  = new JTabbedPane(JTabbedPane.TOP);
    grib2TabPane = new JTabbedPane(JTabbedPane.TOP);
    grib1TabPane = new JTabbedPane(JTabbedPane.TOP);
    bufrTabPane  = new JTabbedPane(JTabbedPane.TOP);
    ftTabPane    = new JTabbedPane(JTabbedPane.TOP);
    fcTabPane    = new JTabbedPane(JTabbedPane.TOP);
    fmrcTabPane  = new JTabbedPane(JTabbedPane.TOP);
    hdf5TabPane  = new JTabbedPane(JTabbedPane.TOP);
    ncmlTabPane  = new JTabbedPane(JTabbedPane.TOP);

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
    tabbedPane.addChangeListener(e -> {
        final Component c = tabbedPane.getSelectedComponent();
        if (c instanceof JLabel) {
          int idx = tabbedPane.getSelectedIndex();
          final String title = tabbedPane.getTitleAt(idx);
          makeComponent(tabbedPane, title);
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
    ftTabPane.addTab("SimpleGeometry", new JLabel("SimpleGeometry"));
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
    debugFlags = (DebugFlags) Proxy.newProxyInstance(
            DebugFlags.class.getClassLoader(), new Class[]{DebugFlags.class}, new DebugProxyHandler());

    final JMenuBar mb = makeMenuBar();
    parentFrame.setJMenuBar(mb);

    setDebugFlags();
  }

/**
 *
 */
    private void addListeners(final JTabbedPane tabPane) {
        tabPane.addChangeListener(e -> {
            final Component c = tabPane.getSelectedComponent();
            if (c instanceof JLabel) {
                final int idx = tabPane.getSelectedIndex();
                final String title = tabPane.getTitleAt(idx);
                makeComponent(tabPane, title);
            }
        });
        tabPane.addComponentListener(new ComponentAdapter() {
            public void componentShown(final ComponentEvent e) {
            final Component c = tabPane.getSelectedComponent();
                if (c instanceof JLabel) {
                    final int idx = tabPane.getSelectedIndex();
                    final String title = tabPane.getTitleAt(idx);
                    makeComponent(tabPane, title);
                }
            }
        });
    }

/**
 *  deferred creation of components to minimize startup
 */
    public void makeComponent(JTabbedPane parent, final String title) {
        if (parent == null) {
            parent = tabbedPane;
        }

        // find the correct index
        int n = parent.getTabCount();
        int idx;
        for (idx = 0; idx < n; idx++) {
          String cTitle = parent.getTitleAt(idx);
          if (cTitle.equals(title)) break;
        }
        if (idx >= n) {
          log.debug("Cant find {} in {}", title, parent);
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
            ncStreamPanel = new NcStreamOpPanel((PreferencesExt) mainPrefs.node("NcStream"));
            c = ncStreamPanel;
            break;

          case "GRIB1collection":
            grib1CollectionPanel = new Grib1CollectionPanel((PreferencesExt) mainPrefs.node("grib1raw"));
            c = grib1CollectionPanel;
            break;

          case "GRIB1data":
            grib1DataPanel = new Grib1DataOpPanel((PreferencesExt) mainPrefs.node("grib1Data"));
            c = grib1DataPanel;
            break;

          case "GRIB-FILES":
            gribFilesPanel = new GribFilesOpPanel((PreferencesExt) mainPrefs.node("gribFiles"));
            c = gribFilesPanel;
            break;

          case "GRIB2collection":
            grib2CollectionPanel = new Grib2CollectionOpPanel((PreferencesExt) mainPrefs.node("gribNew"));
            c = grib2CollectionPanel;
            break;

          case "GRIB2data":
            grib2DataPanel = new Grib2DataOpPanel((PreferencesExt) mainPrefs.node("grib2Data"));
            c = grib2DataPanel;
            break;

          case "BufrCdmIndex":
            bufrCdmIndexPanel = new BufrCdmIndexOpPanel((PreferencesExt) mainPrefs.node("bufrCdmIdx"));
            c = bufrCdmIndexPanel;
        /* } else if (title.equals("CdmIndex")) {
          gribCdmIndexPanel = new GribCdmIndexPanel((PreferencesExt) mainPrefs.node("cdmIdx"));
          c = gribCdmIndexPanel; */
            break;

          case "CdmIndex4":
            cdmIndexPanel = new CdmIndexOpPanel((PreferencesExt) mainPrefs.node("cdmIdx3"));
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
            gribIdxPanel = new GribIndexOpPanel((PreferencesExt) mainPrefs.node("gribIdx"));
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

          case "GRIB-Rewrite":
            gribRewritePanel = new GribRewriteOpPanel((PreferencesExt) mainPrefs.node("grib-rewrite"));
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

          case "SimpleGeometry":
            simpleGeomPanel = new SimpleGeomPanel((PreferencesExt) mainPrefs.node("simpleGeom"));
            c = simpleGeomPanel;
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
            threddsUI = new ThreddsUI(parentFrame, (PreferencesExt) mainPrefs.node("thredds"));
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
            log.warn ("tabbedPane unknown component {}", title);
            return;
        }

        parent.setComponentAt(idx, c);
        log.trace("tabbedPane changed {} added ", title);
    }

/**
 *
 */
  private JMenuBar makeMenuBar() {
    final JMenuBar mb = new JMenuBar();

    /// System menu
    final JMenu sysMenu = new SystemMenu(ToolsUI.this);
    mb.add(sysMenu);

    // Add modes Menu
    final JMenu modeMenu = new ModesMenu(ToolsUI.this);
    mb.add(modeMenu);

    // Add debug Menu
    final JMenu debugMenu = new DebugMenu(ToolsUI.this);
    mb.add(debugMenu);

    // Add help/about Menu
    final JMenu helpMenu = new HelpMenu(ToolsUI.this);
    mb.add(helpMenu);

    return mb;
  }

/**
 *
 */
    public DatasetViewerPanel getDatasetViewerPanel() {
        return viewerPanel;
    }
/**
 *
 */
  public void setDebugFlags() {
    log.debug("setDebugFlags");

    NetcdfFile.setDebugFlags(debugFlags);
    H5iosp.setDebugFlags(debugFlags);
    ucar.nc2.ncml.NcMLReader.setDebugFlags(debugFlags);
    DODSNetcdfFile.setDebugFlags(debugFlags);
    CdmRemote.setDebugFlags(debugFlags);
    Nc4Iosp.setDebugFlags(debugFlags);
    DataFactory.setDebugFlags(debugFlags);

    ucar.nc2.FileWriter2.setDebugFlags(debugFlags);
    ucar.nc2.ft.point.standard.PointDatasetStandardFactory.setDebugFlags(debugFlags);
    ucar.nc2.grib.collection.Grib.setDebugFlags(debugFlags);
  }

/**
 *
 */
    public void setUseRecordStructure(final boolean use) {
        useRecordStructure = use;
    }

/**
 *
 */
    public void setGribDiskCache() {
        if (diskCache2Form == null) {
            diskCache2Form = new DiskCache2Form(parentFrame, GribIndexCache.getDiskCache2());
        }
        diskCache2Form.setVisible(true);
    }

/**
 *
 */
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
    if (threddsUI          != null) threddsUI.storePersistentData();
    if (unitsPanel         != null) unitsPanel.save();
    if (urlPanel           != null) urlPanel.save();
    if (viewerPanel        != null) viewerPanel.save();
    if (writerPanel        != null) writerPanel.save();
    if (wmoCommonCodePanel != null) wmoCommonCodePanel.save();
    if (wmsPanel           != null) wmsPanel.save();
  }

///
///////////////////////////////////////////////////////////////////////////////////////
///
    public static ToolsUI getToolsUI() { return ui; }


    public static JFrame getToolsFrame() { return ui.getFramePriv(); }

    private JFrame getFramePriv() { return parentFrame; }


    public static JTabbedPane getTabbedPane() { return ui.getTabbedPanePriv(); }

    private JTabbedPane getTabbedPanePriv() { return tabbedPane; }


    public static DataFactory getThreddsDataFactory() { return ui.getThreddsDataFactoryPriv(); }

    private DataFactory getThreddsDataFactoryPriv() { return threddsDataFactory; }


    public static FileManager getBufrFileChooser() { return ui.getBufrFileChooserPriv(); }

    private FileManager getBufrFileChooserPriv() {
        if (bufrFileChooser == null) {
            bufrFileChooser = new FileManager(
                        parentFrame, null, null, (PreferencesExt) mainPrefs.node("bufrFileManager"));
        }

        return bufrFileChooser;
    }

    public static OpPanel getOpPanel(String pname) { return ui.getOpPanelPriv(pname); }

    private OpPanel getOpPanelPriv(String pname) {
        log.debug("getOpPanelPriv - {}", pname);

        switch (pname) {
            case "NCDump":
                if (ncdumpPanel == null) {
                    makeComponent(tabbedPane, "NCDump");
                }
                return ncdumpPanel;

            default:
                // No other cases necessary (yet).
        }

        return null;
    }

///
///////////////////////////////////////////////////////////////////////////////////////
///
    public void openNetcdfFile(String datasetName) {
        makeComponent(tabbedPane, "Viewer");
        viewerPanel.doit(datasetName);
        tabbedPane.setSelectedComponent(viewerPanel);
    }

    public void openNetcdfFile(NetcdfFile ncfile) {
        makeComponent(tabbedPane, "Viewer");
        viewerPanel.setDataset(ncfile);
        tabbedPane.setSelectedComponent(viewerPanel);
    }

    public void openCoordSystems(String datasetName) {
        makeComponent(tabbedPane, "CoordSys");
        coordSysPanel.doit(datasetName);
        tabbedPane.setSelectedComponent(coordSysPanel);
    }

    public void openCoordSystems(NetcdfDataset dataset) {
        makeComponent(tabbedPane, "CoordSys");
        coordSysPanel.setDataset(dataset);
        tabbedPane.setSelectedComponent(coordSysPanel);
    }

    public void openNcML(String datasetName) {
        makeComponent(ncmlTabPane, "NcmlEditor");
        ncmlEditorPanel.doit(datasetName);
        tabbedPane.setSelectedComponent(ncmlTabPane);
        ncmlTabPane.setSelectedComponent(ncmlEditorPanel);
    }

    public void openPointFeatureDataset(String datasetName) {
        makeComponent(ftTabPane, "PointFeature");
        pointFeaturePanel.setPointFeatureDataset(FeatureType.ANY_POINT, datasetName);
        tabbedPane.setSelectedComponent(ftTabPane);
        ftTabPane.setSelectedComponent(pointFeaturePanel);
    }

    public void openGrib1Collection(String collection) {
        makeComponent(grib1TabPane, "GRIB1collection");  // LOOK - does this aleays make component ?
        grib1CollectionPanel.setCollection(collection);
        tabbedPane.setSelectedComponent(iospTabPane);
        iospTabPane.setSelectedComponent(grib1TabPane);
        grib1TabPane.setSelectedComponent(grib1CollectionPanel);
    }

    public void openGrib2Collection(String collection) {
        makeComponent(grib2TabPane, "GRIB2collection");
        grib2CollectionPanel.setCollection(collection);
        tabbedPane.setSelectedComponent(iospTabPane);
        iospTabPane.setSelectedComponent(grib2TabPane);
        grib2TabPane.setSelectedComponent(grib2CollectionPanel);
    }

    public void openGrib2Data(String datasetName) {
        makeComponent(grib2TabPane, "GRIB2data");
        grib2DataPanel.doit(datasetName);
        tabbedPane.setSelectedComponent(iospTabPane);
        iospTabPane.setSelectedComponent(grib2TabPane);
        grib2TabPane.setSelectedComponent(grib2DataPanel);
    }

    public void openGrib1Data(String datasetName) {
        makeComponent(grib1TabPane, "GRIB1data");
        grib1DataPanel.doit(datasetName);
        tabbedPane.setSelectedComponent(iospTabPane);
        iospTabPane.setSelectedComponent(grib1TabPane);
        grib1TabPane.setSelectedComponent(grib1DataPanel);
    }

    public void openGridDataset(String datasetName) {
        makeComponent(ftTabPane, "Grids");
        gridPanel.doit(datasetName);
        tabbedPane.setSelectedComponent(ftTabPane);
        ftTabPane.setSelectedComponent(gridPanel);
    }

    public void openCoverageDataset(String datasetName) {
        makeComponent(ftTabPane, "Coverages");
        coveragePanel.doit(datasetName);
        tabbedPane.setSelectedComponent(ftTabPane);
        ftTabPane.setSelectedComponent(coveragePanel);
    }

    public void openGridDataset(NetcdfDataset dataset) {
        makeComponent(ftTabPane, "Grids");
        gridPanel.setDataset(dataset);
        tabbedPane.setSelectedComponent(ftTabPane);
        ftTabPane.setSelectedComponent(gridPanel);
    }

    public void openGridDataset(GridDataset dataset) {
        makeComponent(ftTabPane, "Grids");
        gridPanel.setDataset(dataset);
        tabbedPane.setSelectedComponent(ftTabPane);
        ftTabPane.setSelectedComponent(gridPanel);
    }

    public void openRadialDataset(String datasetName) {
        makeComponent(ftTabPane, "Radial");
        radialPanel.doit(datasetName);
        tabbedPane.setSelectedComponent(ftTabPane);
        ftTabPane.setSelectedComponent(radialPanel);
    }

    public void openWMSDataset(String datasetName) {
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

    }
    catch (IOException ioe) {
      JOptionPane.showMessageDialog(null, "Error on setThreddsDatatype = " + ioe.getMessage());
      ioe.printStackTrace();
    }

  }

  // jump to the appropriate tab based on datatype of InvAccess
  private void jumptoThreddsDatatype(thredds.client.catalog.Access invAccess) {
    if (invAccess == null) {
      return;
    }

    thredds.client.catalog.Service s = invAccess.getService();
    if (s.getType() == ServiceType.HTTPServer) {
      downloadFile(invAccess.getStandardUrlName());
      return;
    }

    if (s.getType() == ServiceType.WMS) {
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
      }
      catch (IOException ioe) {
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
    }
    catch (IOException ioe) {
      ioe.printStackTrace();
      JOptionPane.showMessageDialog(null, "Error on setThreddsDatatype = " + ioe.getMessage());
      if (threddsData != null) {
        try {
          threddsData.close();
        }
        catch (IOException ioe2) {
          // Okay to fall through?
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
      }
      catch (IOException e) {
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
      }
      else if (threddsData.featureDataset instanceof GridDataset) {
        makeComponent(ftTabPane, "Grids");
        gridPanel.setDataset((GridDataset) threddsData.featureDataset);
        tabbedPane.setSelectedComponent(ftTabPane);
        ftTabPane.setSelectedComponent(gridPanel);
      }
    }
    else if (threddsData.featureType == FeatureType.IMAGE) {
      makeComponent(ftTabPane, "Images");
      imagePanel.setImageLocation(threddsData.imageURL);
      tabbedPane.setSelectedComponent(ftTabPane);
      ftTabPane.setSelectedComponent(imagePanel);
    }
    else if (threddsData.featureType == FeatureType.RADIAL) {
      makeComponent(ftTabPane, "Radial");
      radialPanel.setDataset((RadialDatasetSweep) threddsData.featureDataset);
      tabbedPane.setSelectedComponent(ftTabPane);
      ftTabPane.setSelectedComponent(radialPanel);
    }
    else if (threddsData.featureType.isPointFeatureType()) {
      makeComponent(ftTabPane, "PointFeature");
      pointFeaturePanel.setPointFeatureDataset((PointDatasetImpl) threddsData.featureDataset);
      tabbedPane.setSelectedComponent(ftTabPane);
      ftTabPane.setSelectedComponent(pointFeaturePanel);
    }
    else if (threddsData.featureType == FeatureType.STATION_RADIAL) {
      makeComponent(ftTabPane, "StationRadial");
      stationRadialPanel.setStationRadialDataset(threddsData.featureDataset);
      tabbedPane.setSelectedComponent(ftTabPane);
      ftTabPane.setSelectedComponent(stationRadialPanel);
    }
  }

/**
 *
 */
    public NetcdfFile openFile(String location, boolean addCoords, CancelTask task) {

        NetcdfFile ncfile = null;
        try {
            DatasetUrl durl = DatasetUrl.findDatasetUrl(location);
            if (addCoords) {
                ncfile = NetcdfDataset.acquireDataset(durl, true, task);
            }
            else {
                ncfile = NetcdfDataset.acquireFile(durl, task);
            }

            if (ncfile == null) {
                JOptionPane.showMessageDialog(null, "NetcdfDataset.open cant open " + location);
            }
            else if (useRecordStructure) {
                ncfile.sendIospMessage(NetcdfFile.IOSP_MESSAGE_ADD_RECORD_STRUCTURE);
            }
        }
        catch (IOException ioe) {
            String message = ioe.getMessage();
            if ((null == message) && (ioe instanceof EOFException)) {
                message = "Premature End of File";
            }
            JOptionPane.showMessageDialog(null, "NetcdfDataset.open cant open " + location + "\n" + message);
            if (! (ioe instanceof FileNotFoundException)) {
                ioe.printStackTrace();
            }
            ncfile = null;
        }
        catch (Exception e) {
            JOptionPane.showMessageDialog(null, "NetcdfDataset.open cant open " + location + "\n" + e.getMessage());
            log.error("NetcdfDataset.open cant open " + location, e);
            e.printStackTrace();

            try {
                if (ncfile != null) ncfile.close();
            }
            catch (IOException ee) {
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
    final GetDataRunnable runner = new GetDataRunnable() {
      public void run(Object o) {
        String[] values = (String[]) o;
        BufferedOutputStream out;

        try (FileOutputStream fos = new FileOutputStream(values[0])) {
          out = new BufferedOutputStream(fos, 60000);
          IO.copyUrlB(values[1], out, 60000);
          downloadStatus = values[1] + " written to " + values[0];

        }
        catch (IOException ioe) {
          downloadStatus = "Error opening " + values[0] + " and reading " + values[1] + "\n" + ioe.getMessage();
        }
      }
    };

    final GetDataTask task = new GetDataTask(runner, urlString, values);
    final ProgressMonitor pm = new ProgressMonitor(task);
    pm.addActionListener(e -> {
        JOptionPane.showMessageDialog(null, e.getActionCommand() + "\n" + downloadStatus);
        downloadStatus = null;
    });
    pm.start(this, "Download", 30);
  }

///
///////////////////////////////////////////////////////////////////////////////////////
/// the panel contents


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

    @Override
    public boolean process(Object o) {
      return reportPanel.setCollection((String) o);
    }

    public boolean process() {
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

    @Override
    public void save() {
      reportPanel.save();
      super.save();
    }

  } */

/**
 *  raw grib access - dont go through the IOSP
 */
  private class Grib1CollectionPanel extends OpPanel {
    //RandomAccessFile raf = null;
    ucar.nc2.ui.grib.Grib1CollectionPanel gribTable;

    @Override
    public void closeOpenFiles() throws IOException {
      gribTable.closeOpenFiles();
    }

    Grib1CollectionPanel(PreferencesExt p) {
      super(p, "collection:", true, false);
      gribTable = new ucar.nc2.ui.grib.Grib1CollectionPanel(buttPanel, prefs);
      add(gribTable, BorderLayout.CENTER);

      final AbstractButton showButt = BAMutil.makeButtcon("Information", "Show Collection", false);
      showButt.addActionListener(e -> {
          final Formatter f = new Formatter();
          gribTable.showCollection(f);
          detailTA.setText(f.toString());
          detailTA.gotoTop();
          detailWindow.show();
      });
      buttPanel.add(showButt);

      final AbstractButton writeButton = BAMutil.makeButtcon("netcdf", "Write index", false);
      writeButton.addActionListener(e -> {
          final Formatter f = new Formatter();
          try {
            if (!gribTable.writeIndex(f)) return;
          }
          catch (IOException e1) {
            e1.printStackTrace();
          }
          detailTA.setText(f.toString());
          detailTA.gotoTop();
          detailWindow.show();
      });
      buttPanel.add(writeButton);
    }

    void setCollection(String collection) {
      if (process(collection)) {
        cb.addItem(collection);
      }
    }

    @Override
    public boolean process(Object o) {
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

    @Override
    public void save() {
      gribTable.save();
      super.save();
    }
  }

  /////////////////////////////////////////////////////////////////////

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

    @Override
    public void closeOpenFiles() {
    }

    @Override
    public boolean process(Object o) {
      return gribReport.setCollection((String) o);
    }

    public boolean process() {
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

    @Override
    public void save() {
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

    @Override
    public boolean process(Object o) {
      return gribReport.setCollection((String) o);
    }

    public boolean process() {
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

    @Override
    public void save() {
      gribReport.save();
      super.save();
    }
  }  */

/**
 *
 */
  private class GribTemplatePanel extends OpPanel {
    GribWmoTemplatesPanel codeTable;

    GribTemplatePanel(PreferencesExt p) {
      super(p, "table:", false, false, false);

      final JComboBox<WmoTemplateTable.Version> modes = new JComboBox<>(WmoTemplateTable.Version.values());
      modes.setSelectedItem(WmoTemplateTable.standard);
      topPanel.add(modes, BorderLayout.CENTER);
      modes.addActionListener(e -> {
          codeTable.setTable((WmoTemplateTable.Version) modes.getSelectedItem());
      });

      codeTable = new GribWmoTemplatesPanel(prefs, buttPanel);
      add(codeTable, BorderLayout.CENTER);
    }

    @Override
    public boolean process(Object command) {
      return true;
    }

    @Override
    public void save() {
      codeTable.save();
      super.save();
    }

    @Override
    public void closeOpenFiles() {
    }
  }

/**
 *
 */
  private class GribCodePanel extends OpPanel {
    GribWmoCodesPanel codeTable;

    GribCodePanel(PreferencesExt p) {
      super(p, "table:", false, false, false);

      final JComboBox<WmoCodeTable.Version> modes = new JComboBox<>(WmoCodeTable.Version.values());
      modes.setSelectedItem(WmoCodeTable.standard);
      topPanel.add(modes, BorderLayout.CENTER);
      modes.addActionListener(e -> {
          codeTable.setTable((WmoCodeTable.Version) modes.getSelectedItem());
      });

      codeTable = new GribWmoCodesPanel(prefs, buttPanel);
      add(codeTable, BorderLayout.CENTER);
    }

    @Override
    public boolean process(Object command) {
      return true;
    }

    @Override
    public void save() {
      codeTable.save();
      super.save();
    }

    @Override
    public void closeOpenFiles() {
    }
  }

/**
 *
 */
  private class Hdf4Panel extends OpPanel {
    RandomAccessFile raf = null;
    Hdf4Table hdf4Table;

    @Override
    public void closeOpenFiles() throws IOException {
      hdf4Table.closeOpenFiles();
    }

    Hdf4Panel(PreferencesExt p) {
      super(p, "file:", true, false);
      hdf4Table = new Hdf4Table(prefs);
      add(hdf4Table, BorderLayout.CENTER);

      final AbstractButton eosdump = BAMutil.makeButtcon("alien", "Show EOS processing", false);
      eosdump.addActionListener(e -> {
          try {
            final Formatter f = new Formatter();
            hdf4Table.getEosInfo(f);
            detailTA.setText(f.toString());
            detailWindow.show();
          }
          catch (final IOException ioe) {
            final StringWriter sw = new StringWriter(5000);
            ioe.printStackTrace(new PrintWriter(sw));
            detailTA.setText(sw.toString());
            detailWindow.show();
          }
      });
      buttPanel.add(eosdump);
    }

    @Override
    public boolean process(Object o) {
      String command = (String) o;
      boolean err = false;

      try {
        if (raf != null)
          raf.close();
        raf = new RandomAccessFile(command, "r");

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

    @Override
    public void save() {
      hdf4Table.save();
      super.save();
    }
  }

/**
 *
 */
  private class CdmrFeature extends OpPanel {
    CdmrFeaturePanel panel;

    CdmrFeature(PreferencesExt p) {
      super(p, "file:", true, false);
      panel = new CdmrFeaturePanel(prefs);
      add(panel, BorderLayout.CENTER);

      final AbstractAction infoAction = new AbstractAction() {
        public void actionPerformed(ActionEvent e) {
          final Formatter f = new Formatter();
          try {
            panel.showInfo(f);
          }
          catch (final Exception ioe) {
            final StringWriter sw = new StringWriter(5000);
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

    @Override
    public boolean process(Object o) {
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

    @Override
    public void save() {
      panel.save();
      super.save();
    }

    @Override
    public void closeOpenFiles() throws IOException {
      panel.closeOpenFiles();
    }
  }

/**
 *
 */
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

      final AbstractButton viewButton = BAMutil.makeButtcon("alien", "Grid Viewer", false);
      viewButton.addActionListener(e -> {
          if (ds != null) {
            GridDataset gridDataset = dsTable.getGridDataset();
            if (gridUI == null) makeGridUI();
            gridUI.setDataset(gridDataset);
            viewerWindow.show();
          }
      });
      buttPanel.add(viewButton);

      final AbstractButton imageButton = BAMutil.makeButtcon("VCRMovieLoop", "Image Viewer", false);
      imageButton.addActionListener(e -> {
          if (ds != null) {
            GridDatatype grid = dsTable.getGrid();
            if (grid == null) {
              return;
            }
            if (imageWindow == null) {
              makeImageWindow();
            }
            imageViewer.setImageFromGrid(grid);
            imageWindow.show();
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
      gridUI.addMapBean(new ShapeFileBean("WorldDetailMap", "Global Detailed Map", "WorldDetailMap", WORLD_DETAIL_MAP));
      gridUI.addMapBean(new ShapeFileBean("USDetailMap", "US Detailed Map", "USMap", US_MAP));

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

    @Override
    public boolean process(Object o) {
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

    @Override
    public void closeOpenFiles() throws IOException {
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
        logger.warn("close failed");
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
        logger.warn("close failed");
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

    @Override
    public void save() {
      super.save();
      dsTable.save();
      if (gridUI != null) gridUI.storePersistentData();
      if (viewerWindow != null) mainPrefs.putBeanObject(GRIDVIEW_FRAME_SIZE, viewerWindow.getBounds());
      if (imageWindow != null) mainPrefs.putBeanObject(GRIDIMAGE_FRAME_SIZE, imageWindow.getBounds());
    }
  }

/**
 *
 */
  	private class SimpleGeomPanel extends OpPanel {
	    SimpleGeomTable sgTable;
	    JSplitPane split;
	    IndependentWindow viewerWindow, imageWindow;
	    SimpleGeomUI sgUI = null;
	    ImageViewPanel imageViewer;

	    NetcdfDataset ds = null;

	    SimpleGeomPanel(PreferencesExt prefs) {
	      super(prefs, "dataset:", true, false);
	      sgTable = new SimpleGeomTable(prefs, true);
	      add(sgTable, BorderLayout.CENTER);

	      AbstractButton viewButton = BAMutil.makeButtcon("alien", "Grid Viewer", false);
	      viewButton.addActionListener(new ActionListener() {
	        public void actionPerformed(ActionEvent e) {
	          if (ds != null) {
	            GridDataset gridDataset = sgTable.getGridDataset();
	            if (sgUI == null) makeSimpleGeomUI();
	            sgUI.setDataset(gridDataset);
	            viewerWindow.show();
	          }
	        }
	      });
	      buttPanel.add(viewButton);

	      AbstractButton imageButton = BAMutil.makeButtcon("VCRMovieLoop", "Image Viewer", false);
	      imageButton.addActionListener(new ActionListener() {
	        public void actionPerformed(ActionEvent e) {
	          if (ds != null) {
	            GridDatatype grid = sgTable.getGrid();
	            if (grid == null) return;
	            if (imageWindow == null) makeImageWindow();
	            imageViewer.setImageFromGrid(grid);
	            imageWindow.show();
	          }
	        }
	      });
	      buttPanel.add(imageButton);

	      sgTable.addExtra(buttPanel, fileChooser);
	    }

	    private void makeSimpleGeomUI() {
	      // a little tricky to get the parent right for GridUI
	      viewerWindow = new IndependentWindow("Simple Geometry Viewer", BAMutil.getImage("netcdfUI"));

	      sgUI = new SimpleGeomUI((PreferencesExt) prefs.node("SimpleGeomUI"), viewerWindow, fileChooser, 800);
	      sgUI.addMapBean(new WorldMapBean());
	      sgUI.addMapBean(new ShapeFileBean("WorldDetailMap", "Global Detailed Map", "WorldDetailMap", WORLD_DETAIL_MAP));
	      sgUI.addMapBean(new ShapeFileBean("USDetailMap", "US Detailed Map", "USMap", US_MAP));

	      viewerWindow.setComponent(sgUI);
	      Rectangle bounds = (Rectangle) mainPrefs.getBean(GRIDVIEW_FRAME_SIZE, new Rectangle(77, 22, 700, 900));
	      if (bounds.x < 0) bounds.x = 0;
	      if (bounds.y < 0) bounds.x = 0;
	      viewerWindow.setBounds(bounds);
	    }

	    private void makeImageWindow() {
	      imageWindow = new IndependentWindow("Simple Geometry Image Viewer", BAMutil.getImage("netcdfUI"));
	      imageViewer = new ImageViewPanel(null);
	      imageWindow.setComponent(imageViewer);
	      imageWindow.setBounds((Rectangle) mainPrefs.getBean(GRIDIMAGE_FRAME_SIZE, new Rectangle(77, 22, 700, 900)));
	    }

        @Override
	    public boolean process(Object o) {
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

        @Override
	    public void closeOpenFiles() throws IOException {
	      if (ds != null) ds.close();
	      ds = null;
	      sgTable.clear();
	      if (sgUI != null) sgUI.clear();
	    }

	    void setDataset(NetcdfDataset newds) {
	      if (newds == null) return;
	      try {
	        if (ds != null) ds.close();
	      } catch (IOException ioe) {
	        logger.warn("close failed");
	      }

	      Formatter parseInfo = new Formatter();
	      this.ds = newds;
	      try {
	        sgTable.setDataset(newds, parseInfo);
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
	        logger.warn("close failed");
	      }

	      this.ds = (NetcdfDataset) gds.getNetcdfFile(); // ??
	      try {
	        sgTable.setDataset(gds);
	      } catch (IOException e) {
	        e.printStackTrace();
	        return;
	      }
	      setSelectedItem(gds.getLocation());
	    }

        @Override
	    public void save() {
	      super.save();
	      sgTable.save();
	      if (sgUI != null) sgUI.storePersistentData();
	      if (viewerWindow != null) mainPrefs.putBeanObject(GRIDVIEW_FRAME_SIZE, viewerWindow.getBounds());
	      if (imageWindow != null) mainPrefs.putBeanObject(GRIDIMAGE_FRAME_SIZE, imageWindow.getBounds());
	    }

	  }

/**
 *
 */
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
      viewButton.addActionListener(e -> {
          CoverageCollection gridDataset = dsTable.getCoverageDataset();
          if (gridDataset == null) {
            return;
          }
          if (display == null) {
            makeDisplay();
          }
          display.setDataset(dsTable);
          viewerWindow.show();
      });
      buttPanel.add(viewButton);

      AbstractButton infoButton = BAMutil.makeButtcon("Information", "Show Info", false);
      infoButton.addActionListener(e -> {
          Formatter f = new Formatter();
          dsTable.showInfo(f);
          detailTA.setText(f.toString());
          detailTA.gotoTop();
          detailWindow.show();
      });
      buttPanel.add(infoButton);

      //dsTable.addExtra(buttPanel, fileChooser);
    }

    private void makeDisplay() {
      viewerWindow = new IndependentWindow("Coverage Viewer", BAMutil.getImage("netcdfUI"));

      display = new CoverageViewer((PreferencesExt) prefs.node("CoverageDisplay"), viewerWindow, fileChooser, 800);
      display.addMapBean(new WorldMapBean());
      display.addMapBean(new ShapeFileBean("WorldDetailMap", "Global Detailed Map", "WorldDetailMap", WORLD_DETAIL_MAP));
      display.addMapBean(new ShapeFileBean("USDetailMap", "US Detailed Map", "USMap", US_MAP));

      viewerWindow.setComponent(display);
      Rectangle bounds = (Rectangle) mainPrefs.getBean(GRIDVIEW_FRAME_SIZE, new Rectangle(77, 22, 700, 900));
      if (bounds.x < 0) bounds.x = 0;
      if (bounds.y < 0) bounds.x = 0;
      viewerWindow.setBounds(bounds);
    }

    public boolean process(Object o) {
      String command = (String) o;
      boolean err = false;

      // close previous file
      try {
        closeOpenFiles();
      } catch (IOException ioe) {
        logger.warn("close failed");
      }

      try {
        Optional<FeatureDatasetCoverage> opt = CoverageDatasetFactory.openCoverageDataset(command);
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
        logger.warn("close failed");
      }

      dsTable.setCollection( (FeatureDatasetCoverage) fd);
      setSelectedItem(fd.getLocation());
    }

    @Override
    public void closeOpenFiles() throws IOException {
      if (covDatasetCollection != null) covDatasetCollection.close();
      covDatasetCollection = null;
      dsTable.clear();
    }

    @Override
    public void save() {
      super.save();
      dsTable.save();
      if (viewerWindow != null) mainPrefs.putBeanObject(GRIDVIEW_FRAME_SIZE, viewerWindow.getBounds());
    }
  }

///
///////////////////////////////////////////////////////////////////////////////////////
///
/**
 *
 */
    public static void exit() {
        doSavePrefsAndUI();
        System.exit(0);
    }

/**
 *
 */
    private static void doSavePrefsAndUI() {
        ui.save();
        final Rectangle bounds = frame.getBounds();
        prefs.putBeanObject(FRAME_SIZE, bounds);
        try {
            store.save();
        }
        catch (IOException ioe) {
            ioe.printStackTrace();
        }

        done = true; // on some systems, still get a window close event
        final ucar.nc2.util.cache.FileCacheIF cache = NetcdfDataset.getNetcdfFileCache();
        if (cache != null) {
            cache.clearCache(true);
        }
        FileCache.shutdown();           // shutdown threads
        DiskCache2.exit();              // shutdown threads
        MetadataManager.closeAll();     // shutdown bdb
    }

/**
 * Handle messages.
 */
    private static void setDataset() {
        // do it in the swing event thread
        SwingUtilities.invokeLater(( ) -> {
            int pos = wantDataset.indexOf('#');
            if (pos > 0) {
                final String catName = wantDataset.substring(0, pos); // {catalog}#{dataset}
                if (catName.endsWith(".xml")) {
                    ui.makeComponent(null, "THREDDS");
                    ui.threddsUI.setDataset(wantDataset);
                    ui.tabbedPane.setSelectedComponent(ui.threddsUI);
                }
                return;
            }

            // default
            ui.openNetcdfFile(wantDataset);
        });
    }

/**
 *  Set look-and-feel.
 */
    private static void prepareGui() {

        final String osName = System.getProperty("os.name").toLowerCase();
        final boolean isMacOs = osName.startsWith("mac os x");

        if (isMacOs) {
            System.setProperty("apple.laf.useScreenMenuBar", "true");

            // fixes the case on macOS where users use the system menu option to quit rather than
            // closing a window using the 'x' button.
            Runtime.getRuntime().addShutdownHook(new Thread() {
                @Override
                public void run() {
                    doSavePrefsAndUI();
                }
            });
        }
        else {
            // Not macOS, so try applying Nimbus L&F, if available.
            try {
                for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                    if ("Nimbus".equals(info.getName())) {
                        UIManager.setLookAndFeel(info.getClassName());
                        break;
                    }
                }
            }
            catch (Exception exc) {
                log.warn("Unable to apply Nimbus look-and-feel due to {}", exc.toString());

                if (log.isTraceEnabled()) {
                    exc.printStackTrace();
                }
            }
        }

        // misc Gui initialization(s)
        BAMutil.setResourcePath("/resources/nj22/ui/icons/");

        // Setting up a font metrics object triggers one of the most time-wasting steps of GUI set up,
        // so do it now before trying to create the splash or tools interface.
        SwingUtilities.invokeLater(( ) -> {
            final Toolkit tk = Toolkit.getDefaultToolkit ( );
            final Font f = new Font ("SansSerif", Font.PLAIN, 12);

            @SuppressWarnings("deprecation")
            final FontMetrics fm = tk.getFontMetrics (f);
        });
    }

/**
 *  Must call this method on the event thread.
 */
    private static void createToolsFrame() {
        // put UI in a JFrame
        frame = new JFrame("NetCDF ("+DIALOG_VERSION+") Tools");

        ui = new ToolsUI(prefs, frame);

        frame.setIconImage(BAMutil.getImage("netcdfUI"));

        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowActivated(final WindowEvent e) {
                ToolsSplashScreen.getSharedInstance().setVisible(false);
            }

            @Override
            public void windowClosing(final WindowEvent e) {
                if (! done) {
                    exit();
                }
            }
        });

        frame.getContentPane().add(ui);

        final Rectangle have = frame.getGraphicsConfiguration().getBounds();
        final Rectangle def = new Rectangle(50, 50, 800, 800);

        Rectangle want = (Rectangle) prefs.getBean(FRAME_SIZE, def);

        if (want.getX() > have.getWidth() - 25) {
            // may be off screen when switcing between 2 monitor system
            want = def;
        }

        frame.setBounds(want);

        frame.pack();
        frame.setBounds(want);

        // in case a dataset was on the command line
        if (wantDataset != null) {
            setDataset();
        }
    }

/**
 *
 */
    public static void main(String args[]) {
        final long start = System.currentTimeMillis();

        if (debugListen) {
            System.out.println("Arguments:");
            for (final String arg : args) {
                System.out.println(" " + arg);
            }
            HTTPSession.setInterceptors(true);
        }

        // handle multiple versions of ToolsUI, along with passing a dataset name
        // first, if there were command-line arguments
        if (args.length > 0) {
            // munge arguments into a single string
            final StringBuilder sbuff = new StringBuilder();
            for (final String arg : args) {
                sbuff.append(arg);
                sbuff.append(" ");
            }
            final String arguments = sbuff.toString();
            System.out.println("ToolsUI arguments=" + arguments);

            // see if another version is running, if so send it the message then exit
            final SocketMessage sm = new SocketMessage(14444, wantDataset);
            if (sm.isAlreadyRunning()) {
                log.error("ToolsUI already running - pass argument='{}' and exit", arguments);
                System.exit(0);
            }
        }

        // otherwise if no command-line arguments were passed
        else {
            final SocketMessage sm = new SocketMessage(14444, null);
            if (sm.isAlreadyRunning()) {
                System.out.println("ToolsUI already running - start up another copy");
            }
            else {
                sm.addEventListener(new SocketMessage.EventListener() {
                    @Override
                    public void setMessage(SocketMessage.Event event) {
                        wantDataset = event.getMessage();
                        if (debugListen) {
                            System.out.println(" got message= '" + wantDataset);
                        }
                        setDataset();
                    }
                });
            }
        }

        if (debugListen) {
            System.out.println("Arguments:");
            for (final String arg : args) {
                System.out.println(" " + arg);
            }
            HTTPSession.setInterceptors(true);
        }

        // spring initialization
        // is spring used by ToolsUI for anything? is this necessary?
        try {
            ClassPathXmlApplicationContext springContext = new ClassPathXmlApplicationContext(
                                "classpath:resources/nj22/ui/spring/application-config.xml");
        }
        catch (Exception exc) {
            log.error("failed creating spring context: {}", exc.toString());
            System.exit(1);
        }

        // look for command-line arguments
        boolean configRead = false;
        for (int i = 0; i < args.length; i++) {
            if (args[i].equalsIgnoreCase("-nj22Config") && (i < args.length - 1)) {
                final String runtimeConfig = args[i + 1];
                i++;
                try (final FileInputStream fis = new FileInputStream(runtimeConfig)) {
                    final StringBuilder errlog = new StringBuilder();
                    RuntimeConfigParser.read(fis, errlog);
                    configRead = true;
                    System.out.println(errlog);
                }
                catch (IOException ioe) {
                    log.warn("Error reading {} = {}", runtimeConfig, ioe.getMessage());
                }
            }
        }

        if (!configRead) {
            final String filename = XMLStore.makeStandardFilename(".unidata", "nj22Config.xml");
            final File f = new File(filename);
            if (f.exists()) {
                try (final FileInputStream fis = new FileInputStream(filename)) {
                    final StringBuilder errlog = new StringBuilder();
                    RuntimeConfigParser.read(fis, errlog);
                    configRead = true;
                    System.out.println(errlog);
                }
                catch (IOException ioe) {
                    log.warn("Error reading {} = {}", filename, ioe.getMessage());
                }
            }
        }

        // prefs storage
        try {
            // 4.4
            final String prefStore = XMLStore.makeStandardFilename(".unidata", "ToolsUI.xml");
            final File prefs44 = new File(prefStore);

            if (!prefs44.exists()) {
                // if 4.4 doesnt exist, see if 4.3 exists
                final String prefStoreBack = XMLStore.makeStandardFilename(".unidata", "NetcdfUI22.xml");
                final File prefs43 = new File(prefStoreBack);
                if (prefs43.exists()) {
                    // make a copy of it
                    IO.copyFile(prefs43, prefs44);
                }
            }

            // open 4.4 version, create it if it doesn't exist
            store = XMLStore.createFromFile(prefStore, null);
            prefs = store.getPreferences();

            Debug.setStore(prefs.node("Debug"));
        }
        catch (IOException e) {
            log.warn("XMLStore creation failed - {}", e.toString());
        }

        // Prepare UI management.
        prepareGui();

        // Display the splash screen so there's something to look at while we do some more init.
        SwingUtilities.invokeLater(( ) -> {
            ToolsSplashScreen.getSharedInstance().setVisible(true);
        });

        // LOOK needed? for efficiency, persist aggregations. Every hour, delete stuff older than 30 days
        Aggregation.setPersistenceCache(new DiskCache2("/.unidata/aggCache", true, 60 * 24 * 30, 60));

        try {
            // MetadataManager.setCacheDirectory(fcCache, maxSizeBytes, jvmPercent); // use defaults
            thredds.inventory.CollectionManagerAbstract.setMetadataStore(MetadataManager.getFactory());
        }
        catch (Exception exc) {
            log.error("CdmInit: Failed CollectionManagerAbstract.setMetadataStore - {}", exc.toString());
        }

        // open dap initializations
        DODSNetcdfFile.setAllowCompression(true);
        DODSNetcdfFile.setAllowSessions(true);

        // caching
        RandomAccessFile.enableDefaultGlobalFileCache();
        GribCdmIndex.initDefaultCollectionCache(100, 200, -1);

        // Waste some time on the main thread before adding another task to the event dispatch thread.
        // Somehow this let's the EDT "catch up" and in particular gets the splash to finish rendering.
        try
        {
            Thread.sleep (2500l);
        }
        catch (Exception ignore)
        {
            // LOGGER.debug ("Not implemented.");.
        }

        // Create the UI frame!
        SwingUtilities.invokeLater(( ) -> {
            createToolsFrame();
            frame.setVisible(true);
        });

        // Name HTTP user agent and set Authentication for accessing password protected services
        SwingUtilities.invokeLater(( ) -> {
            UrlAuthenticatorDialog provider = new UrlAuthenticatorDialog(frame);
            try {
                HTTPSession.setGlobalCredentialsProvider(provider);
            }
            catch (HTTPException e) {
                log.error("Failed to set global credentials");
            }
            HTTPSession.setGlobalUserAgent("ToolsUI v5.0");

            java.net.Authenticator.setDefault(provider);
        });
    }
}
