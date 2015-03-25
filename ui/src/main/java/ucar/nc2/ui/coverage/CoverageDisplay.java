package ucar.nc2.ui.coverage;

import thredds.client.catalog.ServiceType;
import thredds.client.catalog.Dataset;
import thredds.client.catalog.tools.DataFactory;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.dataset.CoordinateAxis1D;
import ucar.nc2.dataset.CoordinateAxis1DTime;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.ft.cover.Coverage;
import ucar.nc2.ft.cover.CoverageCS;
import ucar.nc2.ft.cover.CoverageDataset;
import ucar.nc2.ft.cover.impl.CoverageDatasetImpl;
import ucar.nc2.ui.event.ActionCoordinator;
import ucar.nc2.ui.event.ActionSourceListener;
import ucar.nc2.ui.event.ActionValueEvent;
import ucar.nc2.ui.geoloc.*;
import ucar.nc2.ui.gis.MapBean;
import ucar.nc2.ui.grid.*;
import ucar.nc2.ui.widget.*;
import ucar.nc2.ui.widget.PopupMenu;
import ucar.nc2.util.NamedObject;
import ucar.unidata.geoloc.ProjectionImpl;
import ucar.unidata.geoloc.ProjectionPointImpl;
import ucar.unidata.geoloc.ProjectionRect;
import ucar.util.prefs.PreferencesExt;
import ucar.util.prefs.ui.Debug;

import javax.swing.*;
import javax.swing.border.EtchedBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.AffineTransform;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.util.*;
import java.util.List;

/**
 * Description
 *
 * @author John
 * @since 12/27/12
 */
public class CoverageDisplay extends JPanel {
  static private final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(CoverageDisplay.class);

  // constants
  private static final int DELAY_DRAW_AFTER_DATA_EVENT = 250;   // quarter sec
  private static final String LastMapAreaName = "LastMapArea";
  private static final String LastProjectionName = "LastProjection";
  private static final String LastDatasetName = "LastDataset";
  private static final String ColorScaleName = "ColorScale";
  static private final String DATASET_URL = "DatasetURL";
  static private final String GEOTIFF_FILECHOOSER_DEFAULTDIR = "geotiffDefDir";

  private PreferencesExt store;
  private JFrame parent;
  private FileManager fileChooser;

  // UI components
  private ColorScale colorScale;
  private ColorScale.Panel colorScalePanel;
  //private VertPanel vertPanel;
  private List<Chooser> choosers;
  private SuperComboBox fieldChooser, levelChooser, timeChooser, ensembleChooser, runtimeChooser;
  private JLabel dataValueLabel, positionLabel;

  private NavigatedPanel navPanel;

  // the main components
  // private GridController controller;
  //private GeoGridTable dsTable;

  // UI components that need global scope
  private TextHistoryPane datasetInfoTA, ncmlTA;
  private JPanel drawingPanel;
  //private JSplitPane splitDraw;
  private JComboBox csDataMinMax;
  private PopupMenu mapBeanMenu;
  private JSpinner strideSpinner;

  private JLabel datasetNameLabel = new JLabel();
  // private Field.TextCombo gridUrlIF;
  // private PrefPanel gridPP;

  // the various managers and dialog boxes
  private ProjectionManager projManager;
  //private ColorScaleManager csManager;
  private IndependentWindow infoWindow = null;
  private IndependentWindow ncmlWindow = null;
  private IndependentWindow gtWindow = null;
  private JDialog dsDialog = null;
  private FileManager geotiffFileChooser;

  // toolbars
  private JPanel fieldPanel, toolPanel;
  private JToolBar navToolbar, moveToolbar;
  private AbstractAction navToolbarAction, moveToolbarAction;

  // actions
  private AbstractAction redrawAction;
  private AbstractAction showDatasetInfoAction;
  //private AbstractAction showNcMLAction;
  //private AbstractAction showGridTableAction;
  //private AbstractAction showGridDatasetInfoAction;
  //private AbstractAction showNetcdfDatasetAction;
  private AbstractAction minmaxHorizAction, minmaxLogAction, minmaxHoldAction;
  private AbstractAction fieldLoopAction, levelLoopAction, timeLoopAction;
  private AbstractAction chooseProjectionAction, saveCurrentProjectionAction;

  private AbstractAction dataProjectionAction, exitAction, helpAction, showGridAction, showContoursAction, showContourLabelsAction, showWindsAction;
  private AbstractAction drawHorizAction, drawVertAction;

  // data components
  private CoverageDataset coverageDataset;
  private Coverage currentField;
  private ProjectionImpl project;

  // state
  private List<NamedObject> levelNames, timeNames, ensembleNames, runtimeNames;
  private int currentLevel;
  private int currentSlice;
  private int currentTime;
  private int currentEnsemble;
  private int currentRunTime;
  private boolean drawHorizOn = true, drawVertOn = false;
  private boolean eventsOK = true;
  private Color mapColor = Color.black;
  private boolean drawWinds = false;
  private boolean hasDependentTimeAxis = false;
  private boolean selected = false;
  private int mapBeanCount = 0;

  // rendering
  private AffineTransform atI = new AffineTransform();  // identity transform
  private ucar.nc2.ui.util.Renderer renderMap = null;
  private CoverageRenderer renderGrid;
  //private WindRenderer renderWind;
  private javax.swing.Timer redrawTimer;

  // debugging
  private boolean debugBeans = false, debugChooser = false, debugPrint = false, debugHelp = false;
  private boolean debugTask = false, debugThread = false;

  public CoverageDisplay(PreferencesExt pstore, RootPaneContainer root, FileManager fileChooser, int defaultHeight) {
    this.store = pstore;
    this.fileChooser = fileChooser;

    try {
      // choosers
      choosers = new ArrayList<Chooser>();
      fieldChooser = new SuperComboBox(root, "field", true, null);
      choosers.add(new Chooser("field", fieldChooser, true));
      levelChooser = new SuperComboBox(root, "level", false, null);
      choosers.add(new Chooser("level", levelChooser, false));
      timeChooser = new SuperComboBox(root, "time", false, null);
      choosers.add(new Chooser("time", timeChooser, false));
      ensembleChooser = new SuperComboBox(root, "ensemble", false, null);
      choosers.add(new Chooser("ensemble", ensembleChooser, false));
      runtimeChooser = new SuperComboBox(root, "runtime", false, null);
      choosers.add(new Chooser("runtime", runtimeChooser, false));

      // gridTable
      //gridTable = new GridTable("field");
      //gtWindow = new IndependentWindow("Grid Table Information", BAMutil.getImage( "GDVs"), gridTable.getPanel());

      //PreferencesExt dsNode = (PreferencesExt) pstore.node("DatasetTable");
      // dsTable = new GeoGridTable(dsNode, true);
      //dsDialog = dsTable.makeDialog(root, "NetcdfDataset Info", false);
      //dsDialog.setIconImage( BAMutil.getImage( "GDVs"));
      //Rectangle bounds = (Rectangle) dsNode.getBean("DialogBounds", new Rectangle(50, 50, 800, 450));
      //dsDialog.setBounds( bounds);

      // colorscale
      Object bean = store.getBean(ColorScaleName, null);
      if ((null == bean) || !(bean instanceof ColorScale))
        colorScale = new ColorScale("default");
      else
        colorScale = (ColorScale) store.getBean(ColorScaleName, null);

      colorScalePanel = new ColorScale.Panel(this, colorScale);
      csDataMinMax = new JComboBox(ColorScale.MinMaxType.values());
      csDataMinMax.setToolTipText("ColorScale Min/Max setting");
      csDataMinMax.addActionListener(new AbstractAction() {
        public void actionPerformed(ActionEvent e) {
          renderGrid.setDataMinMaxType((ColorScale.MinMaxType) csDataMinMax.getSelectedItem());
          redrawLater();
        }
      });

      // renderer
      // set up the renderers; Maps are added by addMapBean()
      renderGrid = new CoverageRenderer(store);
      renderGrid.setColorScale(colorScale);

      strideSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 100, 1));
      strideSpinner.addChangeListener(new ChangeListener() {
        public void stateChanged(ChangeEvent e) {
          Integer val = (Integer) strideSpinner.getValue();
          renderGrid.setHorizStride(val.intValue());
        }
      });

      makeActionsDataset();
      makeActionsToolbars();
      makeActions();
      makeEventManagement();

      //// toolPanel
      toolPanel = new JPanel();
      toolPanel.setBorder(new EtchedBorder());
      toolPanel.setLayout(new MFlowLayout(FlowLayout.LEFT, 0, 0));

      // menus
      JMenu dataMenu = new JMenu("Dataset");
      dataMenu.setMnemonic('D');
      JMenu configMenu = new JMenu("Configure");
      configMenu.setMnemonic('C');
      JMenu toolMenu = new JMenu("Controls");
      toolMenu.setMnemonic('T');
      JMenuBar menuBar = new JMenuBar();
      menuBar.add(dataMenu);
      menuBar.add(configMenu);
      menuBar.add(toolMenu);
      toolPanel.add(menuBar);

      // field chooser panel - delay adding the choosers
      fieldPanel = new JPanel();
      fieldPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));
      toolPanel.add(fieldPanel);

      // stride
      toolPanel.add(strideSpinner);

      // buttcons
      BAMutil.addActionToContainer(toolPanel, drawHorizAction);
      BAMutil.addActionToContainer(toolPanel, drawVertAction);
      mapBeanMenu = MapBean.makeMapSelectButton();
      toolPanel.add(mapBeanMenu.getParentComponent());

      // the Navigated panel and its toolbars
      navPanel = new NavigatedPanel();
      navPanel.setLayout(new FlowLayout());
      ProjectionRect ma = (ProjectionRect) store.getBean(LastMapAreaName, null);
      if (ma != null)
        navPanel.setMapArea( ma);

      navToolbar = navPanel.getNavToolBar();
      moveToolbar = navPanel.getMoveToolBar();
      if ((Boolean) navToolbarAction.getValue(BAMutil.STATE))
        toolPanel.add(navToolbar);
      if ((Boolean) moveToolbarAction.getValue(BAMutil.STATE))
        toolPanel.add(moveToolbar);
      makeNavPanelWiring();
      addActionsToMenus(dataMenu, configMenu, toolMenu);

      BAMutil.addActionToContainer(toolPanel, navPanel.setReferenceAction);
      BAMutil.addActionToContainer(toolPanel, dataProjectionAction);
      BAMutil.addActionToContainer(toolPanel, showGridAction);
      BAMutil.addActionToContainer(toolPanel, showContoursAction);
      BAMutil.addActionToContainer(toolPanel, showContourLabelsAction);
      BAMutil.addActionToContainer(toolPanel, redrawAction);

      //  vertical split
      //vertPanel = new VertPanel();
      //splitDraw = new JSplitPane(JSplitPane.VERTICAL_SPLIT, panz, vertPanel);
      //int divLoc = store.getInt( "vertSplit", 2*defaultHeight/3);
      //splitDraw.setDividerLocation(divLoc);
      drawingPanel = new JPanel(new BorderLayout()); // filled later

      // status panel
      JPanel statusPanel = new JPanel(new BorderLayout());
      statusPanel.setBorder(new EtchedBorder());
      positionLabel = new JLabel("position");
      positionLabel.setToolTipText("position at cursor");
      dataValueLabel = new JLabel("data value", SwingConstants.CENTER);
      dataValueLabel.setToolTipText("data value (double click on grid)");
      statusPanel.add(positionLabel, BorderLayout.WEST);
      statusPanel.add(dataValueLabel, BorderLayout.CENTER);
      navPanel.setPositionLabel(positionLabel);

      // assemble
      JPanel westPanel = new JPanel(new BorderLayout());
      westPanel.add(colorScalePanel, BorderLayout.CENTER);
      westPanel.add(csDataMinMax, BorderLayout.NORTH);

      JPanel northPanel = new JPanel();
      //northPanel.setLayout( new BoxLayout(northPanel, BoxLayout.Y_AXIS));
      northPanel.setLayout(new BorderLayout());
      northPanel.add(datasetNameLabel, BorderLayout.NORTH);
      northPanel.add(toolPanel, BorderLayout.SOUTH);

      setLayout(new BorderLayout());
      add(northPanel, BorderLayout.NORTH);
      add(statusPanel, BorderLayout.SOUTH);
      add(westPanel, BorderLayout.WEST);
      add(drawingPanel, BorderLayout.CENTER);

      setDrawHorizAndVert(drawHorizOn, drawVertOn);

      // get last saved Projection
      project = (ProjectionImpl) store.getBean(LastProjectionName, null);
      if (project != null)
        setProjection(project);

            // other components
      //geotiffFileChooser = new FileManager( parent);
      //geotiffFileChooser.setCurrentDirectory( store.get(GEOTIFF_FILECHOOSER_DEFAULTDIR, "."));

     /* gridPP = new PrefPanel("GridView", (PreferencesExt) store.node("GridViewPrefs"));
     gridUrlIF = gridPP.addTextComboField("url", "Gridded Data URL", null, 10, false);
     gridPP.addButton( BAMutil.makeButtconFromAction( chooseLocalDatasetAction ));
     gridPP.finish(true, BorderLayout.EAST);
     gridPP.addActionListener(new ActionListener() {
       public void actionPerformed(ActionEvent e) {
         InvDatasetImpl ds = new InvDatasetImpl( gridUrlIF.getText(), thredds.catalog.DataType.GRID, ServiceType.NETCDF);
         setDataset( ds);
       }
     }); */




      // redraw timer
      redrawTimer = new javax.swing.Timer(0, new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          SwingUtilities.invokeLater(new Runnable() {  // invoke in event thread
            public void run() {
              draw(false);
            }
          });
          redrawTimer.stop(); // one-shot timer
        }
      });
      redrawTimer.setInitialDelay(DELAY_DRAW_AFTER_DATA_EVENT);
      redrawTimer.setRepeats(false);


    } catch (Exception e) {
      System.out.println("UI creation failed");
      e.printStackTrace();
    }
  }


  // actions that control the dataset
  private void makeActionsDataset() {

    // choose local dataset
    AbstractAction chooseLocalDatasetAction = new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        String filename = fileChooser.chooseFilename();
        if (filename == null) return;

        Dataset invDs;
        try {     // DatasetNode parent, String name, Map<String, Object> flds, List< AccessBuilder > accessBuilders, List< DatasetBuilder > datasetBuilders
          Map<String, Object> flds = new HashMap<>();
          flds.put(Dataset.FeatureType, FeatureType.GRID.toString());
          flds.put(Dataset.ServiceName, ServiceType.File.toString());  // bogus
          invDs = new Dataset(null, filename, flds, null, null);
          // invDs = new Dataset(filename, FeatureType.GRID, ServiceType.NETCDF);
        } catch (Exception ue) {
          JOptionPane.showMessageDialog(CoverageDisplay.this, "Invalid filename = <" + filename + ">\n" + ue.getMessage());
          ue.printStackTrace();
          return;
        }
        setDataset(invDs);
      }
    };
    BAMutil.setActionProperties(chooseLocalDatasetAction, "FileChooser", "open Local dataset...", false, 'L', -1);

    /* saveDatasetAction = new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        String fname = controller.getDatasetName();
        if (fname != null) {
          savedDatasetList.add( fname);
          BAMutil.addActionToMenu( savedDatasetMenu, new DatasetAction( fname), 0);
        }
      }
    };
    BAMutil.setActionProperties( saveDatasetAction, null, "save dataset", false, 'S', 0);
    */

    // Configure
    chooseProjectionAction = new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        getProjectionManager().setVisible();
      }
    };
    BAMutil.setActionProperties(chooseProjectionAction, null, "Projection Manager...", false, 'P', 0);

    saveCurrentProjectionAction = new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        getProjectionManager();
        // set the bounding box
        ProjectionImpl proj = navPanel.getProjectionImpl().constructCopy();
        proj.setDefaultMapArea(navPanel.getMapArea());
        //if (debug) System.out.println(" GV save projection "+ proj);

        // projManage.setMap(renderAll.get("Map"));   LOOK!
        //projManager.saveProjection( proj);
      }
    };
    BAMutil.setActionProperties(saveCurrentProjectionAction, null, "save Current Projection", false, 'S', 0);

    /* chooseColorScaleAction = new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        if (null == csManager) // lazy instantiation
          makeColorScaleManager();
        csManager.show();
      }
    };
    BAMutil.setActionProperties( chooseColorScaleAction, null, "ColorScale Manager...", false, 'C', 0);

    */
    // redraw
    redrawAction = new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        repaint();
        start(true);
        draw(true);
      }
    };
    BAMutil.setActionProperties(redrawAction, "alien", "RedRaw", false, 'W', 0);

    showDatasetInfoAction = new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        if (infoWindow == null) {
          datasetInfoTA = new TextHistoryPane();
          infoWindow = new IndependentWindow("Dataset Information", BAMutil.getImage("GDVs"), datasetInfoTA);
          infoWindow.setSize(700, 700);
          infoWindow.setLocation(100, 100);
        }

        datasetInfoTA.clear();
        if (coverageDataset != null) {
          Formatter f = new Formatter();
          coverageDataset.getDetailInfo(f);
          datasetInfoTA.appendLine(f.toString());
        } else {
          datasetInfoTA.appendLine("No coverageDataset loaded");
        }
        datasetInfoTA.gotoTop();
        infoWindow.show();
      }
    };
    BAMutil.setActionProperties(showDatasetInfoAction, "Information", "Show info...", false, 'S', -1);

    /*showNcMLAction = new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        if (ncmlWindow == null) {
          ncmlTA = new TextHistoryPane();
          ncmlWindow = new IndependentWindow("Dataset NcML", BAMutil.getImage( "GDVs"), ncmlTA);
          ncmlWindow.setSize(700,700);
          ncmlWindow.setLocation(200, 70);
        }

        ncmlTA.clear();
        //datasetInfoTA.appendLine( "GeoGrid XML for "+ controller.getDatasetName()+"\n");
        ncmlTA.appendLine( controller.getNcML());
        ncmlTA.gotoTop();
        ncmlWindow.show();
      }
    };
    BAMutil.setActionProperties( showNcMLAction, null, "Show NcML...", false, 'X', -1);  */

    /* showGridDatasetInfoAction = new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        if (ncmlWindow == null) {
          ncmlTA = new TextHistoryPane();
          ncmlWindow = new IndependentWindow("Dataset NcML", BAMutil.getImage( "GDVs"), ncmlTA);
          ncmlWindow.setSize(700,700);
          ncmlWindow.setLocation(200, 70);
        }

        ncmlTA.clear();
        //datasetInfoTA.appendLine( "GeoGrid XML for "+ controller.getDatasetName()+"\n");
        ncmlTA.appendLine( controller.getDatasetXML());
        ncmlTA.gotoTop();
        ncmlWindow.show();
      }
    };
    BAMutil.setActionProperties( showGridDatasetInfoAction, null, "Show GridDataset Info XML...", false, 'X', -1);

      // show netcdf dataset Table
    /* showNetcdfDatasetAction = new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        NetcdfDataset netcdfDataset = controller.getNetcdfDataset();
        if (null != netcdfDataset) {
          try {
            dsTable.setDataset(netcdfDataset, null);
          } catch (IOException e1) {
            e1.printStackTrace();
            return;
          }
          dsDialog.show();
        }
      }
    };
    BAMutil.setActionProperties( showNetcdfDatasetAction, "netcdf", "NetcdfDataset Table Info...", false, 'D', -1);  */

    minmaxHorizAction = new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        csDataMinMax.setSelectedItem(ColorScale.MinMaxType.horiz);
        setDataMinMaxType(ColorScale.MinMaxType.horiz);
      }
    };
    BAMutil.setActionProperties(minmaxHorizAction, null, "Horizontal plane", false, 'H', 0);

    minmaxLogAction = new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        csDataMinMax.setSelectedItem(ColorScale.MinMaxType.log);
        setDataMinMaxType(ColorScale.MinMaxType.log);
      }
    };
    BAMutil.setActionProperties(minmaxLogAction, null, "log horiz plane", false, 'V', 0);

    /* minmaxVolAction =  new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        csDataMinMax.setSelectedIndex(GridRenderer.VOL_MinMaxType);
        controller.setDataMinMaxType(GridRenderer.MinMaxType.vert;
      }
    };
    BAMutil.setActionProperties( minmaxVolAction, null, "Grid volume", false, 'G', 0); */

    minmaxHoldAction = new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        csDataMinMax.setSelectedItem(ColorScale.MinMaxType.hold);
        setDataMinMaxType(ColorScale.MinMaxType.hold);
      }
    };
    BAMutil.setActionProperties(minmaxHoldAction, null, "Hold scale constant", false, 'C', 0);

    fieldLoopAction = new LoopControlAction(fieldChooser);
    levelLoopAction = new LoopControlAction(levelChooser);
    timeLoopAction = new LoopControlAction(timeChooser);
  }

  private void makeActionsToolbars() {

    navToolbarAction = new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        Boolean state = (Boolean) getValue(BAMutil.STATE);
        if (state.booleanValue())
          toolPanel.add(navToolbar);
        else
          toolPanel.remove(navToolbar);
      }
    };
    BAMutil.setActionProperties(navToolbarAction, "MagnifyPlus", "show Navigate toolbar", true, 'M', 0);
    navToolbarAction.putValue(BAMutil.STATE, new Boolean(store.getBoolean("navToolbarAction", true)));

    moveToolbarAction = new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        Boolean state = (Boolean) getValue(BAMutil.STATE);
        if (state.booleanValue())
          toolPanel.add(moveToolbar);
        else
          toolPanel.remove(moveToolbar);
      }
    };
    BAMutil.setActionProperties(moveToolbarAction, "Up", "show Move toolbar", true, 'M', 0);
    moveToolbarAction.putValue(BAMutil.STATE, new Boolean(store.getBoolean("moveToolbarAction", true)));
  }

  // create all actions here
  // the actions can then be attached to buttcons, menus, etc
  private void makeActions() {
    boolean state;

    dataProjectionAction = new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        ProjectionImpl dataProjection = renderGrid.getDataProjection();
        if (null != dataProjection)
          setProjection(dataProjection);
      }
    };
    BAMutil.setActionProperties(dataProjectionAction, "DataProjection", "use Data Projection", false, 'D', 0);

    // draw horiz
    drawHorizAction = new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        Boolean state = (Boolean) getValue(BAMutil.STATE);
        // System.out.println("showGridAction state "+state);
        drawHorizOn = state.booleanValue();
        setDrawHorizAndVert(drawHorizOn, drawVertOn);
        draw(false);
      }
    };
    BAMutil.setActionProperties(drawHorizAction, "DrawHoriz", "draw horizontal", true, 'H', 0);
    state = store.getBoolean("drawHorizAction", true);
    drawHorizAction.putValue(BAMutil.STATE, new Boolean(state));
    drawHorizOn = state;

    // draw Vert
    drawVertAction = new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        Boolean state = (Boolean) getValue(BAMutil.STATE);
        // System.out.println("showGridAction state "+state);
        drawVertOn = state.booleanValue();
        setDrawHorizAndVert(drawHorizOn, drawVertOn);
        draw(false);
      }
    };
    BAMutil.setActionProperties(drawVertAction, "DrawVert", "draw vertical", true, 'V', 0);
    state = store.getBoolean("drawVertAction", false);
    drawVertAction.putValue(BAMutil.STATE, new Boolean(state));
    drawVertOn = state;

    // show grid
    showGridAction = new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        Boolean state = (Boolean) getValue(BAMutil.STATE);
        // System.out.println("showGridAction state "+state);
        renderGrid.setDrawGridLines(state.booleanValue());
        draw(false);
      }
    };
    BAMutil.setActionProperties(showGridAction, "Grid", "show grid lines", true, 'G', 0);
    state = store.getBoolean("showGridAction", false);
    showGridAction.putValue(BAMutil.STATE, new Boolean(state));
    renderGrid.setDrawGridLines(state);

    // contouring
    showContoursAction = new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        Boolean state = (Boolean) getValue(BAMutil.STATE);
        renderGrid.setDrawContours(state.booleanValue());
        draw(false);
      }
    };
    BAMutil.setActionProperties(showContoursAction, "Contours", "show contours", true, 'C', 0);
    state = store.getBoolean("showContoursAction", false);
    showContoursAction.putValue(BAMutil.STATE, new Boolean(state));
    renderGrid.setDrawContours(state);

    // contouring labels
    showContourLabelsAction = new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        Boolean state = (Boolean) getValue(BAMutil.STATE);
        renderGrid.setDrawContourLabels(state.booleanValue());
        draw(false);
      }
    };
    BAMutil.setActionProperties(showContourLabelsAction, "ContourLabels", "show contour labels", true, 'L', 0);
    state = store.getBoolean("showContourLabelsAction", false);
    showContourLabelsAction.putValue(BAMutil.STATE, new Boolean(state));
    renderGrid.setDrawContourLabels(state);

    /* winds
    showWindsAction = new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        Boolean state = (Boolean) getValue(BAMutil.STATE);
        drawWinds = state.booleanValue();
        draw(true, false, false);
      }
    };
    BAMutil.setActionProperties( showWindsAction, "ShowWinds", "show wind", true, 'W', 0);
    */
  }

  private void makeEventManagement() {

    //// manage field selection events
    String actionName = "field";
    ActionCoordinator fieldCoordinator = new ActionCoordinator(actionName);
    // connect to the fieldChooser
    fieldCoordinator.addActionSourceListener(fieldChooser.getActionSourceListener());
    // connect to the gridTable
    //fieldCoordinator.addActionSourceListener(gridTable.getActionSourceListener());
    // heres what to do when the currentField changes
    ActionSourceListener fieldSource = new ActionSourceListener(actionName) {
      public void actionPerformed(ActionValueEvent e) {
        if (setField(e.getValue())) {
          if (e.getActionCommand().equals("redrawImmediate")) {
            draw(true);
            //colorScalePanel.paintImmediately(colorScalePanel.getBounds());   // kludgerino
          } else
            redrawLater();
        }
      }
    };
    fieldCoordinator.addActionSourceListener(fieldSource);

    //// manage level selection events
    actionName = "level";
    ActionCoordinator levelCoordinator = new ActionCoordinator(actionName);
    // connect to the levelChooser
    levelCoordinator.addActionSourceListener(levelChooser.getActionSourceListener());
    // connect to the vertPanel
    /* levelCoordinator.addActionSourceListener(vertPanel.getActionSourceListener());
      // also manage Pick events from the vertPanel
    vertPanel.getDrawArea().addPickEventListener( new PickEventListener() {
      public void actionPerformed(PickEvent e) {
        int level = renderGrid.findLevelCoordElement(e.getLocation().getY());
         if ((level != -1) && (level != currentLevel)) {
          currentLevel = level;
          redrawLater();
          String selectedName = levelNames.get(currentLevel).getName();
          if (Debug.isSet("pick/event"))
            System.out.println("pick.event Vert: "+selectedName);
          levelSource.fireActionValueEvent(ActionSourceListener.SELECTED, selectedName);
        }
      }
    }); */
    // heres what to do when a level changes
    ActionSourceListener levelSource = new ActionSourceListener(actionName) {
      public void actionPerformed(ActionValueEvent e) {
        int level = findIndexFromName(levelNames, e.getValue().toString());
        if ((level != -1) && (level != currentLevel)) {
          currentLevel = level;
          if (e.getActionCommand().equals("redrawImmediate")) {
            draw(true);
          } else
            redrawLater();
        }
      }
    };
    levelCoordinator.addActionSourceListener(levelSource);

    //// manage time selection events
    actionName = "time";
    ActionCoordinator timeCoordinator = new ActionCoordinator(actionName);
    // connect to the timeChooser
    timeCoordinator.addActionSourceListener(timeChooser.getActionSourceListener());
    // heres what to do when the time changes
    ActionSourceListener timeSource = new ActionSourceListener(actionName) {
      public void actionPerformed(ActionValueEvent e) {
        int time = findIndexFromName(timeNames, e.getValue().toString());
        if ((time != -1) && (time != currentTime)) {
          currentTime = time;
          if (e.getActionCommand().equals("redrawImmediate")) {
            draw(true);
            //colorScalePanel.paintImmediately(colorScalePanel.getBounds());   // kludgerino
          } else
            redrawLater();
        }
      }
    };
    timeCoordinator.addActionSourceListener(timeSource);

    /* manage runtime selection events
    actionName = "runtime";
    ActionCoordinator runtimeCoordinator = new ActionCoordinator(actionName);
      // connect to the timeChooser
    runtimeCoordinator.addActionSourceListener(runtimeChooser.getActionSourceListener());
      // heres what to do when the time changes
    ActionSourceListener runtimeSource = new ActionSourceListener(actionName) {
      public void actionPerformed(ActionValueEvent e) {
        int runtime = findIndexFromName( runtimeNames, e.getValue().toString());
        if ((runtime != -1) && (runtime != currentRunTime)) {
          currentRunTime = runtime;
          if (hasDependentTimeAxis) {
            CoverageCS gcs = currentField.getCoordinateSystem();
            CoordinateAxis1DTime taxis = gcs.getTimeAxisForRun(runtime);
            timeNames = taxis.getNames();
            timeChooser.setCollection(timeNames.iterator());
            if (currentTime >= timeNames.size())
              currentTime = 0;
            timeChooser.setSelectedByIndex( currentTime);
          }

          if (e.getActionCommand().equals("redrawImmediate")) {
            draw(true);
          } else
            redrawLater();
        }
      }
    };
    runtimeCoordinator.addActionSourceListener(runtimeSource);  */

    //// manage runtime selection events
    actionName = "ensemble";
    ActionCoordinator ensembleCoordinator = new ActionCoordinator(actionName);
    // connect to the timeChooser
    ensembleCoordinator.addActionSourceListener(ensembleChooser.getActionSourceListener());
    // heres what to do when the time changes
    ActionSourceListener ensembleSource = new ActionSourceListener(actionName) {
      public void actionPerformed(ActionValueEvent e) {
        int ensIndex = findIndexFromName(ensembleNames, e.getValue().toString());
        if ((ensIndex != -1) && (ensIndex != currentEnsemble)) {
          currentEnsemble = ensIndex;
          if (e.getActionCommand().equals("redrawImmediate")) {
            draw(true);
          } else
            redrawLater();
        }
      }
    };
    ensembleCoordinator.addActionSourceListener(ensembleSource);
  }

  private void makeNavPanelWiring() {

    // get Projection Events from the navigated panel
    navPanel.addNewProjectionListener(new NewProjectionListener() {
      public void actionPerformed(NewProjectionEvent e) {
        if (Debug.isSet("event/NewProjection"))
          System.out.println("Controller got NewProjectionEvent " + navPanel.getMapArea());
        if (eventsOK && renderMap != null) {
          renderMap.setProjection(e.getProjection());
          renderGrid.setProjection(e.getProjection());
          drawH(false);
        }
      }
    });

    // get NewMapAreaEvents from the navigated panel
    navPanel.addNewMapAreaListener(new NewMapAreaListener() {
      public void actionPerformed(NewMapAreaEvent e) {
        if (Debug.isSet("event/NewMapArea"))
          System.out.println("Controller got NewMapAreaEvent " + navPanel.getMapArea());
        drawH(false);
      }
    });


    /* get Pick events from the navigated panel
  np.addPickEventListener( new PickEventListener() {
    public void actionPerformed(PickEvent e) {
      projPoint.setLocation(e.getLocation());
      int slice = renderGrid.findSliceFromPoint(projPoint);
      if (Debug.isSet("pick/event"))
        System.out.println("pick.event: "+projPoint+" "+slice);
      if ((slice >= 0) && (slice != currentSlice)) {
        currentSlice = slice;
        //vertPanel.setSlice( currentSlice);
        redrawLater();
      }
    }
  });  */

    /* get Move events from the navigated panel
  np.addCursorMoveEventListener( new CursorMoveEventListener() {
    public void actionPerformed(CursorMoveEvent e) {
      projPoint.setLocation(e.getLocation());
      String valueS = ""; // renderGrid.getXYvalueStr(projPoint);  LOOK
      dataValueLabel.setText(valueS);
    }
  });

  /*   // get Move events from the vertPanel
  vertPanel.getDrawArea().addCursorMoveEventListener( new CursorMoveEventListener() {
    public void actionPerformed(CursorMoveEvent e) {
      Point2D loc = e.getLocation();
      posLabel.setText(renderGrid.getYZpositionStr(loc));
      dataValueLabel.setText(renderGrid.getYZvalueStr(loc));
    }
  });


    // catch window resize events in vertPanel : LOOK event order problem?
  vertPanel.getDrawArea().addComponentListener( new ComponentAdapter() {
    public void componentResized( ComponentEvent e) {
      draw(false);
    }
  });  */
  }

  private ProjectionPointImpl projPoint = new ProjectionPointImpl();

  private int findIndexFromName(List<NamedObject> list, String name) {
    for (int idx = 0; idx < list.size(); idx++) {
      NamedObject no = list.get(idx);
      if (name.equals(no.getName()))
        return idx;
    }
    log.error("findIndexFromName cant find " + name);
    return -1;
  }

  ////////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * save all data in the PersistentStore
   */
  public void save() {
    //store.putInt( "vertSplit", splitDraw.getDividerLocation());

    store.putBoolean("navToolbarAction", ((Boolean) navToolbarAction.getValue(BAMutil.STATE)).booleanValue());
    store.putBoolean("moveToolbarAction", ((Boolean) moveToolbarAction.getValue(BAMutil.STATE)).booleanValue());

    if (projManager != null)
      projManager.storePersistentData();
    /* if (csManager != null)
      csManager.storePersistentData();
    if (sysConfigDialog != null)
      sysConfigDialog.storePersistentData(); */

    //dsTable.save();
    //dsTable.getPrefs().putBeanObject("DialogBounds", dsDialog.getBounds());

    store.put(GEOTIFF_FILECHOOSER_DEFAULTDIR, geotiffFileChooser.getCurrentDirectory());

    store.putBeanObject(LastMapAreaName, navPanel.getMapArea());
    store.putBeanObject(LastProjectionName, navPanel.getProjectionImpl());
    //if (gridDataset != null)
    //  store.put(LastDatasetName, gridDataset.getTitle());
    store.putBeanObject(ColorScaleName, colorScale);

    store.putBoolean("showGridAction", ((Boolean) showGridAction.getValue(BAMutil.STATE)).booleanValue());
    store.putBoolean("showContoursAction", ((Boolean) showContoursAction.getValue(BAMutil.STATE)).booleanValue());
    store.putBoolean("showContourLabelsAction", ((Boolean) showContourLabelsAction.getValue(BAMutil.STATE)).booleanValue());

  }

  /* private boolean chooseDataset(String url) {
   InvDataset invDs = new InvDatasetImpl( fname, ServerType.NETCDF);
   return chooseDataset( invDs);
 } */

  private void setSelected(boolean b) {
    selected = b;

    //showGridTableAction.setEnabled(b);
    //showNcMLAction.setEnabled(b);
    //showNcMLAction.setEnabled(b);
    //showNetcdfDatasetAction.setEnabled(b);
    //showGridDatasetInfoAction.setEnabled(b);
    //showNetcdfXMLAction.setEnabled( b);

    navToolbarAction.setEnabled(b);
    moveToolbarAction.setEnabled(b);

    //controller.showGridAction.setEnabled( b);
    //controller.showContoursAction.setEnabled( b);
    //controller.showContourLabelsAction.setEnabled( b);
    redrawAction.setEnabled(b);

    minmaxHorizAction.setEnabled(b);
    minmaxLogAction.setEnabled(b);
    minmaxHoldAction.setEnabled(b);

    fieldLoopAction.setEnabled(b);
    levelLoopAction.setEnabled(b);
    timeLoopAction.setEnabled(b);

    navPanel.setEnabledActions(b);
  }

  // add a MapBean to the User Interface
  public void addMapBean(MapBean mb) {
    mapBeanMenu.addAction(mb.getActionDesc(), mb.getIcon(), mb.getAction());

    // first one is the "default"
    if (mapBeanCount == 0) {
      setMapRenderer(mb.getRenderer());
    }
    mapBeanCount++;

    mb.addPropertyChangeListener(new PropertyChangeListener() {
      public void propertyChange(java.beans.PropertyChangeEvent e) {
        if (e.getPropertyName().equals("Renderer")) {
          setMapRenderer((ucar.nc2.ui.util.Renderer) e.getNewValue());
        }
      }
    });
  }

  void setMapRenderer(ucar.nc2.ui.util.Renderer mapRenderer) {
    this.renderMap = mapRenderer;
    mapRenderer.setProjection(navPanel.getProjectionImpl());
    mapRenderer.setColor(mapColor);
    redrawLater();
  }

  public void setDataset(Dataset ds) {
    if (ds == null) return;

    OpenDatasetTask openTask = new OpenDatasetTask(ds);
    ucar.nc2.ui.widget.ProgressMonitor pm = new ucar.nc2.ui.widget.ProgressMonitor(openTask);
    pm.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        if (e.getActionCommand().equals("success")) {
          showDataset();
          //gridTable.setDataset(controller.getFields());
          datasetNameLabel.setText("Dataset:  " + coverageDataset.getLocation());
          setSelected(true);
          gtWindow.hide();
        }
      }
    });
    pm.start(this, "Open Dataset " + ds.getName(), 100);
  }

  // assume that its done in the event thread
  boolean showDataset() {

    // temp kludge for initialization
    java.util.List<Coverage> grids = coverageDataset.getCoverages();
    if ((grids == null) || grids.size() == 0) {
      javax.swing.JOptionPane.showMessageDialog(null, "No gridded fields in file " + coverageDataset.getTitle());
      return false;
    }

    currentField = grids.get(0);
    currentSlice = 0;
    currentLevel = 0;
    currentTime = 0;
    currentEnsemble = 0;
    currentRunTime = 0;

    eventsOK = false; // dont let this trigger redraw
    renderGrid.setCoverage(currentField);
    setFields(grids);
    setField(currentField);

    // if possible, change the projection and the map area to one that fits this
    // dataset
    ProjectionImpl dataProjection = currentField.getCoordinateSystem().getProjection();
    if (dataProjection != null)
      setProjection(dataProjection);

    // ready to draw
    //draw(true);

    // events now ok
    eventsOK = true;
    return true;
  }

  public void setDataMinMaxType(ColorScale.MinMaxType type) {
    renderGrid.setDataMinMaxType(type);
    redrawLater();
  }

  private boolean startOK = true;

  public void setDataset(CoverageDataset coverageDataset) {
    this.coverageDataset = coverageDataset;
    startOK = false; // wait till redraw is hit before drawing
    showDataset();
    datasetNameLabel.setText("Dataset:  " + coverageDataset.getLocation());
    //gridTable.setDataset(controller.getFields());
  }

  void setFields(java.util.List<Coverage> fields) {
    fieldChooser.setCollection(fields.iterator());
  }

  private boolean setField(Object fld) {
    Coverage gg = null;
    if (fld instanceof Coverage)
      gg = (Coverage) fld;
    else if (fld instanceof String)
      gg = coverageDataset.findCoverage((String) fld);
    else if (fld instanceof NamedObject)
      gg = coverageDataset.findCoverage(((NamedObject) fld).getName());
    if (null == gg)
      return false;

    renderGrid.setCoverage(gg);
    currentField = gg;

    CoverageCS gcs = gg.getCoordinateSystem();
    //gcs.setProjectionBoundingBox();

    // set levels
    if (gcs.getVerticalAxis() != null && gcs.getVerticalAxis() instanceof CoordinateAxis1D) {
      CoordinateAxis1D vaxis = (CoordinateAxis1D) gcs.getVerticalAxis();
      levelNames = vaxis.getNames();
      if ((levelNames == null) || (currentLevel >= levelNames.size()))
        currentLevel = 0;
      //vertPanel.setCoordSys(currentField.getCoordinateSystem(), currentLevel);

      setChooserWanted("level", true);
      java.util.List<NamedObject> levels = vaxis.getNames();
      levelChooser.setCollection(levels.iterator(), true);
      NamedObject no = levels.get(currentLevel);
      levelChooser.setSelectedByName(no.getName());
    } else {
      levelNames = new ArrayList<NamedObject>();
      setChooserWanted("level", false);
    }

    // set times
    if (gcs.getTimeAxis() != null && gcs.getTimeAxis() instanceof CoordinateAxis1DTime) {
      CoordinateAxis1DTime taxis = (CoordinateAxis1DTime) gcs.getTimeAxis();
      timeNames = taxis.getNames();
      if ((timeNames == null) || (currentTime >= timeNames.size()))
        currentTime = 0;
      hasDependentTimeAxis = true;

      setChooserWanted("time", true);
      java.util.List<NamedObject> names = taxis.getNames();
      timeChooser.setCollection(names.iterator(), true);
      NamedObject no = names.get(currentTime);
      timeChooser.setSelectedByName(no.getName());

    } else {
      timeNames = new ArrayList<NamedObject>();
      hasDependentTimeAxis = false;
      setChooserWanted("time", false);
    }

    /* set ensembles
    CoordinateAxis1D eaxis = gcs.getEnsembleAxis();
    ensembleNames = (eaxis == null) ? new ArrayList() : eaxis.getNames();
    currentEnsemble = ensembleNames.size() > 0 ? 0 : -1;

    // set runtimes
    CoordinateAxis1DTime rtaxis = gcs.getRunTimeAxis();
    runtimeNames = (rtaxis == null) ? new ArrayList() : rtaxis.getNames();
    currentRunTime = runtimeNames.size() > 0 ? 0 : -1;


      // times
    if (gcs.hasTimeAxis()) {
      axis = gcs.hasTimeAxis1D() ? gcs.getTimeAxis1D() : gcs.getTimeAxisForRun(0);
      setChooserWanted("time", axis != null);
      if (axis != null) {
        java.util.List<NamedObject> names = axis.getNames();
        timeChooser.setCollection(names.iterator(), true);
        NamedObject no =  names.get(currentTime);
        timeChooser.setSelectedByName(no.getName());
      }
    } else {
      setChooserWanted("time", false);
    }

    axis = gcs.getEnsembleAxis();
    setChooserWanted("ensemble", axis != null);
    if (axis != null) {
      java.util.List<NamedObject> names = axis.getNames();
      ensembleChooser.setCollection(names.iterator(), true);
      NamedObject no =  names.get(currentEnsemble);
      ensembleChooser.setSelectedByName(no.getName());
    }

    axis = gcs.getRunTimeAxis();
    setChooserWanted("runtime", axis != null);
    if (axis != null) {
      java.util.List<NamedObject> names = axis.getNames();
      runtimeChooser.setCollection(names.iterator(), true);
      NamedObject no = names.get(currentRunTime);
      runtimeChooser.setSelectedByName(no.getName());
    }   */

    addChoosers();

    fieldChooser.setToolTipText(gg.getShortName());
    colorScalePanel.setUnitString(gg.getUnitsString());
    return true;
  }

  void setDrawHorizAndVert(boolean drawHoriz, boolean drawVert) {
    drawingPanel.removeAll();
    if (drawHoriz && drawVert) {
      // splitDraw.setTopComponent(panz);
      //splitDraw.setBottomComponent(vertPanel);
      drawingPanel.add(navPanel, BorderLayout.CENTER);
    } else if (drawHoriz) {
      drawingPanel.add(navPanel, BorderLayout.CENTER);
    } else if (drawVert) {
      drawingPanel.add(navPanel, BorderLayout.CENTER); // LOOK drawVert not supported
    }
  }

  public void setProjection(ProjectionImpl p) {
    project = p;
    if (renderMap != null)
      renderMap.setProjection(p);
    renderGrid.setProjection(p);
    // renderWind.setProjection( p);
    navPanel.setProjectionImpl(p);
    redrawLater();
  }

  ////////////////////////////////////////////////////////////////////////////////////////////////////////////

  void start(boolean ok) {
    startOK = ok;
    renderGrid.makeStridedGrid();
  }

  synchronized void draw(boolean immediate) {
    if (!startOK) return;

    renderGrid.setLevel(currentLevel);
    renderGrid.setTime(currentTime);
    //renderGrid.setSlice(currentSlice);
    //renderGrid.setEnsemble(currentEnsemble);
    //renderGrid.setRunTime(currentRunTime);

    if (drawHorizOn)
      drawH(immediate);
    //if (drawVertOn)
    //  drawV(immediate);
  }

  private void drawH(boolean immediate) {
    if (!startOK) return;

    // cancel any redrawLater
    boolean already = redrawTimer.isRunning();
    if (debugThread && already) System.out.println("redrawLater canceled ");
    if (already)
      redrawTimer.stop();

    long tstart = System.currentTimeMillis();
    long startTime, tookTime;

    //// horizontal slice
    // the Navigated Panel's BufferedImage graphics
    Graphics2D gNP = navPanel.getBufferedImageGraphics();
    if (gNP == null) // panel not drawn on screen yet
      return;

    // clear
    gNP.setBackground(navPanel.getBackgroundColor());
    gNP.fill(gNP.getClipBounds());

    // draw grid
    startTime = System.currentTimeMillis();
    renderGrid.renderPlanView(gNP, atI);
    if (Debug.isSet("timing/GridDraw")) {
      tookTime = System.currentTimeMillis() - startTime;
      System.out.println("timing.GridDraw: " + tookTime * .001 + " seconds");
    }

    //draw Map
    if (renderMap != null) {
      startTime = System.currentTimeMillis();
      renderMap.draw(gNP, atI);
      if (Debug.isSet("timing/MapDraw")) {
        tookTime = System.currentTimeMillis() - startTime;
        System.out.println("timing/MapDraw: " + tookTime * .001 + " seconds");
      }
    }

    /* draw Winds
    if (drawWinds) {
      startTime = System.currentTimeMillis();
      renderWind.draw(gNP, currentLevel, currentTime);
      if (Debug.isSet("timing/WindsDraw")) {
        tookTime = System.currentTimeMillis() - startTime;
        System.out.println("timing.WindsDraw: " + tookTime*.001 + " seconds");
      }
    } */

    // copy buffer to the screen
    if (immediate)
      navPanel.drawG();
    else
      navPanel.repaint();

    // cleanup
    gNP.dispose();

    if (Debug.isSet("timing/total")) {
      tookTime = System.currentTimeMillis() - tstart;
      System.out.println("timing.total: " + tookTime * .001 + " seconds");
    }
  }

  /* private void drawV(boolean immediate) {
    if (!startOK) return;
    ScaledPanel drawArea = vertPanel.getDrawArea();
    Graphics2D gV = drawArea.getBufferedImageGraphics();
    if (gV == null)
      return;

    long startTime = System.currentTimeMillis();

    gV.setBackground(Color.white);
    gV.fill(gV.getClipBounds());
    renderGrid.renderVertView(gV, atI);

    if (Debug.isSet("timing/GridDrawVert")) {
      long tookTime = System.currentTimeMillis() - startTime;
      System.out.println("timing.GridDrawVert: " + tookTime*.001 + " seconds");
    }
    gV.dispose();

    // copy buffer to the screen
     if (immediate)
      drawArea.drawNow();
    else
      drawArea.repaint();
  } */

  private synchronized void redrawLater() {
    //redrawComplete |= complete;
    boolean already = redrawTimer.isRunning();
    if (debugThread) System.out.println("redrawLater isRunning= " + already);
    if (already)
      redrawTimer.restart();
    else
      redrawTimer.start();
  }

  /*  private void makeSysConfigWindow() {
   sysConfigDialog = new ucar.unidata.ui.PropertyDialog(topLevel.getRootPaneContainer(), true,
       "System Configuration", store, "HelpDir");     // LOOK KLUDGE
   sysConfigDialog.pack();
   sysConfigDialog.setSize(500,200);
   sysConfigDialog.setLocation(300,300);
 }

 private void makeColorScaleManager() {
   csManager = new ColorScaleManager(topLevel.getRootPaneContainer(), store);
   csManager.addPropertyChangeListener(  new java.beans.PropertyChangeListener() {
     public void propertyChange( java.beans.PropertyChangeEvent e) {
       if (e.getPropertyName().equals("ColorScale")) {
         ColorScale cs = (ColorScale) e.getNewValue();
         cs = (ColorScale) cs.clone();
         //System.out.println("UI: new Colorscale got "+cs);
         colorScalePanel.setColorScale(cs);
         controller.setColorScale(cs);
       }
     }
   });
 } */

  public ProjectionManager getProjectionManager() {
    if (null != projManager)
      return projManager;

    projManager = new ProjectionManager(parent, store);
    projManager.addPropertyChangeListener(new java.beans.PropertyChangeListener() {
      public void propertyChange(java.beans.PropertyChangeEvent e) {
        if (e.getPropertyName().equals("ProjectionImpl")) {
          ProjectionImpl p = (ProjectionImpl) e.getNewValue();
          p = p.constructCopy();
          //System.out.println("UI: new Projection "+p);
          setProjection(p);
        }
      }
    });

    return projManager;
  }

  private void addChoosers() {
    fieldPanel.removeAll();
    for (int i = 0; i < choosers.size(); i++) {
      Chooser c = choosers.get(i);
      if (c.isWanted)
        fieldPanel.add(c.field);
    }
  }

  private static class Chooser {
    Chooser(String name, SuperComboBox field, boolean want) {
      this.name = name;
      this.field = field;
      this.isWanted = want;
    }

    boolean isWanted;
    String name;
    SuperComboBox field;
  }

  private void setChooserWanted(String name, boolean want) {
    for (Chooser chooser : choosers) {
      if (chooser.name.equals(name)) chooser.isWanted = want;
    }
  }

  private void addToolbarOption(String toolbarName, JToolBar toolbar, AbstractAction act) {
    boolean wantsToolbar = store.getBoolean(toolbarName, true);
    if (wantsToolbar)
      toolPanel.add(toolbar);
  }

  private void addActionsToMenus(JMenu datasetMenu, JMenu configMenu, JMenu toolMenu) {
    // Info
    //BAMutil.addActionToMenu(datasetMenu, showGridTableAction);
    BAMutil.addActionToMenu(datasetMenu, showDatasetInfoAction);
    //BAMutil.addActionToMenu(datasetMenu, showNcMLAction);
    //BAMutil.addActionToMenu(datasetMenu, showGridDatasetInfoAction);
    //BAMutil.addActionToMenu(datasetMenu, showNetcdfDatasetAction);
    // BAMutil.addActionToMenu( datasetMenu, geotiffAction);
    //BAMutil.addActionToMenu( infoMenu, showNetcdfXMLAction);

    /// Configure
    JMenu toolbarMenu = new JMenu("Toolbars");
    toolbarMenu.setMnemonic('T');
    configMenu.add(toolbarMenu);
    BAMutil.addActionToMenu(toolbarMenu, navToolbarAction);
    BAMutil.addActionToMenu(toolbarMenu, moveToolbarAction);

    BAMutil.addActionToMenu(configMenu, chooseProjectionAction);
    BAMutil.addActionToMenu(configMenu, saveCurrentProjectionAction);

    /* BAMutil.addActionToMenu( configMenu, chooseColorScaleAction);
    BAMutil.addActionToMenu( configMenu, controller.dataProjectionAction);
    */

    //// tools menu
    JMenu displayMenu = new JMenu("Display control");
    displayMenu.setMnemonic('D');

    BAMutil.addActionToMenu(displayMenu, showGridAction);
    BAMutil.addActionToMenu(displayMenu, showContoursAction);
    BAMutil.addActionToMenu(displayMenu, showContourLabelsAction);
    BAMutil.addActionToMenu(displayMenu, redrawAction);
    toolMenu.add(displayMenu);

    // Loop Control
    JMenu loopMenu = new JMenu("Loop control");
    loopMenu.setMnemonic('L');

    BAMutil.addActionToMenu(loopMenu, fieldLoopAction);
    BAMutil.addActionToMenu(loopMenu, levelLoopAction);
    BAMutil.addActionToMenu(loopMenu, timeLoopAction);
    toolMenu.add(loopMenu);

    // MinMax Control
    JMenu mmMenu = new JMenu("ColorScale min/max");
    mmMenu.setMnemonic('C');
    BAMutil.addActionToMenu(mmMenu, minmaxHorizAction);
    BAMutil.addActionToMenu(mmMenu, minmaxLogAction);
    BAMutil.addActionToMenu(mmMenu, minmaxHoldAction);
    toolMenu.add(mmMenu);

    // Zoom/Pan
    JMenu zoomMenu = new JMenu("Zoom/Pan");
    zoomMenu.setMnemonic('Z');
    navPanel.addActionsToMenu(zoomMenu); // items are added by NavigatedPanelToolbar
    toolMenu.add(zoomMenu);
  }

  // loop control for SuperComboBox
  private static class LoopControlAction extends AbstractAction {
    SuperComboBox scbox;

    LoopControlAction(SuperComboBox cbox) {
      this.scbox = cbox;
      BAMutil.setActionProperties(this, null, cbox.getName(), false, 0, 0);
    }

    public void actionPerformed(ActionEvent e) {
      scbox.getLoopControl().show();
    }
  }

  // open remote dataset in cancellable task
  private class OpenDatasetTask extends ProgressMonitorTask implements ucar.nc2.util.CancelTask {
    DataFactory factory;
    Dataset invds;

    OpenDatasetTask(Dataset ds) {
      factory = new DataFactory();
      this.invds = ds;
    }

    public void run() {
      NetcdfDataset dataset = null;
      CoverageDataset gridDataset = null;
      Formatter errlog = new Formatter();

      try {
        dataset = factory.openDataset(invds, true, this, errlog);
        gridDataset = new CoverageDatasetImpl(dataset, errlog);

      } catch (IOException e) {
        setError("Failed to open datset: " + errlog);
      }

      success = !cancel && (gridDataset != null);
      if (success) setDataset(gridDataset);
      done = true;
    }
  }

}

