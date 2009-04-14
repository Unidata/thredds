// $Id: GridUI.java 70 2006-07-13 15:16:05Z caron $
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
package ucar.nc2.ui.grid;

import thredds.catalog.*;
import thredds.ui.*;
import thredds.viewer.ui.Renderer;

import ucar.nc2.dataset.*;

import thredds.viewer.ui.geoloc.NavigatedPanel;
import thredds.viewer.gis.MapBean;

import ucar.nc2.dt.GridDatatype;
import ucar.nc2.dt.GridCoordSystem;
import ucar.nc2.dt.GridDataset;
import ucar.nc2.util.NamedObject;
import ucar.nc2.constants.FeatureType;

import ucar.util.prefs.PreferencesExt;

import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;
import java.util.Formatter;
import java.beans.PropertyChangeListener;
import javax.swing.*;
import javax.swing.border.*;

/**
 * This is the thredds Data Viewer application User Interface for Grids.
 *
 * @author caron
 * @version $Revision: 70 $ $Date: 2006-07-13 15:16:05Z $
 */
public class GridUI extends JPanel {
  static private final String DATASET_URL = "DatasetURL";
  static private final String GEOTIFF_FILECHOOSER_DEFAULTDIR = "geotiffDefDir";

  //private TopLevel topLevel;
  private PreferencesExt store;
  private JFrame parent;
  private FileManager fileChooser;

  // Package private access
  SuperComboBox fieldChooser, levelChooser, timeChooser, ensembleChooser, runtimeChooser;
  NavigatedPanel panz = new NavigatedPanel();
  VertPanel vertPanel;
  GridTable gridTable;
  JLabel dataValueLabel, positionLabel;

  // the main components
  private GridController controller;
  private ColorScale.Panel colorScalePanel;

  private ucar.nc2.ui.GeoGridTable dsTable;

  // UI components that need global scope
  private TextHistoryPane datasetInfoTA, ncmlTA;
  private JPanel drawingPanel;
  private JSplitPane splitDraw;
  private JComboBox csDataMinMax;
  private thredds.ui.PopupMenu mapBeanMenu;

  private JLabel datasetNameLabel;
  // private Field.TextCombo gridUrlIF;
  // private PrefPanel gridPP;

  // the various managers and dialog boxes
  //private ProjectionManager projManager;
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
  private JMenu configMenu;

  // actions
  private AbstractAction redrawAction;
  private AbstractAction showDatasetInfoAction;
  private AbstractAction showNcMLAction;
  private AbstractAction showGridTableAction;
  private AbstractAction showGridDatasetInfoAction;
  private AbstractAction showNetcdfDatasetAction;
  private AbstractAction minmaxHorizAction, minmaxVertAction, minmaxVolAction, minmaxHoldAction;
  private AbstractAction  fieldLoopAction, levelLoopAction, timeLoopAction;

  // state
  private boolean selected = false;
  private int mapBeanCount = 0;

  // debugging
  private boolean debugBeans = false, debugChooser = false, debugPrint = false, debugHelp = false;
  private boolean debugTask = false;

  public GridUI(PreferencesExt pstore, RootPaneContainer root, FileManager fileChooser, int defaultHeight) {
    // this.topUI = topUI;
    this.store = pstore;
    this.fileChooser = fileChooser;

    try  {
      choosers = new ArrayList();
      fieldChooser = new SuperComboBox(root, "field", true, null);
      choosers.add( new Chooser("field", fieldChooser, true));
      levelChooser = new SuperComboBox(root, "level", false, null);
      choosers.add( new Chooser("level", levelChooser, false));
      timeChooser = new SuperComboBox(root, "time", false, null);
      choosers.add( new Chooser("time", timeChooser, false));
      ensembleChooser = new SuperComboBox(root, "ensemble", false, null);
      choosers.add( new Chooser("ensemble", ensembleChooser, false));
      runtimeChooser = new SuperComboBox(root, "runtime", false, null);
      choosers.add( new Chooser("runtime", runtimeChooser, false));

      makeActionsDataset();
      makeActionsToolbars();

      gridTable = new GridTable("field");
      gtWindow = new IndependentWindow("Grid Table Information", BAMutil.getImage( "GDVs"), gridTable.getPanel());

      PreferencesExt dsNode = (PreferencesExt) pstore.node("DatasetTable");
      dsTable = new ucar.nc2.ui.GeoGridTable(dsNode, true);
      dsDialog = dsTable.makeDialog(root, "NetcdfDataset Info", false);
      //dsDialog.setIconImage( BAMutil.getImage( "GDVs"));
      Rectangle bounds = (Rectangle) dsNode.getBean("DialogBounds", new Rectangle(50, 50, 800, 450));
      dsDialog.setBounds( bounds);

      controller = new GridController( this, store);
      makeUI(defaultHeight);
      controller.finishInit();

          // other components
      geotiffFileChooser = new FileManager( parent);
      geotiffFileChooser.setCurrentDirectory( store.get(GEOTIFF_FILECHOOSER_DEFAULTDIR, "."));

    } catch (Exception e) {
      System.out.println("UI creation failed");
      e.printStackTrace();
    }
  }

    // give access to the Controller
    /* NavigatedPanel getNavigatedPanel() { return panz; }
    VertPanel getVertPanel() { return vertPanel; }
    SuperComboBox getFieldChooser() { return fieldChooser; }
    SuperComboBox getLevelChooser() { return levelChooser; }
    SuperComboBox getTimeChooser() { return timeChooser; }
    GridTable getGridTable() { return gridTable; }
    JLabel getDataValueLabel() { return dataValueLabel; }
    JLabel getPositionLabel() { return positionLabel; } */

      /** save all data in the PersistentStore */
  public void storePersistentData() {
    store.putInt( "vertSplit", splitDraw.getDividerLocation());

    store.putBoolean( "navToolbarAction", ((Boolean)navToolbarAction.getValue(BAMutil.STATE)).booleanValue());
    store.putBoolean( "moveToolbarAction", ((Boolean)moveToolbarAction.getValue(BAMutil.STATE)).booleanValue());

    /* if (projManager != null)
      projManager.storePersistentData();
    if (csManager != null)
      csManager.storePersistentData();
    if (sysConfigDialog != null)
      sysConfigDialog.storePersistentData(); */

    dsTable.save();
    dsTable.getPrefs().putBeanObject("DialogBounds", dsDialog.getBounds());

    store.put(GEOTIFF_FILECHOOSER_DEFAULTDIR, geotiffFileChooser.getCurrentDirectory());

    controller.storePersistentData();
  }

 /* private boolean chooseDataset(String url) {
    InvDataset invDs = new InvDatasetImpl( fname, ServerType.NETCDF);
    return chooseDataset( invDs);
  } */

  boolean isSelected() { return selected; }
  void setSelected( boolean b) {
    selected = b;

    showGridTableAction.setEnabled( b);
    showNcMLAction.setEnabled( b);
    showNcMLAction.setEnabled( b);
    showNetcdfDatasetAction.setEnabled( b);
    showGridDatasetInfoAction.setEnabled( b);
    //showNetcdfXMLAction.setEnabled( b);

    navToolbarAction.setEnabled( b);
    moveToolbarAction.setEnabled( b);

    controller.showGridAction.setEnabled( b);
    controller.showContoursAction.setEnabled( b);
    controller.showContourLabelsAction.setEnabled( b);
    redrawAction.setEnabled( b);

    minmaxHorizAction.setEnabled( b);
    minmaxVertAction.setEnabled( b);
    minmaxVolAction.setEnabled( b);
    minmaxHoldAction.setEnabled( b);

    fieldLoopAction.setEnabled( b);
    levelLoopAction.setEnabled( b);
    timeLoopAction.setEnabled( b);

    panz.setEnabledActions( b);
  }

           // add a MapBean to the User Interface
  public void addMapBean( thredds.viewer.gis.MapBean mb) {
    mapBeanMenu.addAction( mb.getActionDesc(), mb.getIcon(), mb.getAction());

    // first one is the "default"
    if (mapBeanCount == 0) {
      setMapRenderer( mb.getRenderer());
    }
    mapBeanCount++;

   mb.addPropertyChangeListener( new PropertyChangeListener() {
     public void propertyChange( java.beans.PropertyChangeEvent e) {
       if (e.getPropertyName().equals("Renderer")) {
         setMapRenderer( (Renderer) e.getNewValue());
       }
     }
   });
  }

  void setMapRenderer( thredds.viewer.ui.Renderer mapRenderer) {
    controller.setMapRenderer( mapRenderer);
  }

  public void setDataset(InvDataset ds) {
     if (ds == null) return;

     OpenDatasetTask openTask = new OpenDatasetTask(ds);
     thredds.ui.ProgressMonitor pm = new thredds.ui.ProgressMonitor(openTask);
     pm.addActionListener( new ActionListener() {
       public void actionPerformed(ActionEvent e) {
         if (e.getActionCommand().equals("success")) {
           controller.showDataset();
           gridTable.setDataset(controller.getFields());
           datasetNameLabel.setText("Dataset:  "+ controller.getDatasetUrlString());
           setSelected(true);
           gtWindow.hide();
         }
       }
     });
     pm.start( this, "Open Dataset "+ds.getName(), 100);
   }


  public void setDataset(GridDataset ds) {
     controller.setGridDataset( ds);
     controller.showDataset();
     datasetNameLabel.setText("Dataset:  "+ controller.getDatasetUrlString());
     gridTable.setDataset(controller.getFields());
   }

   void setFields( java.util.List fields) {
    fieldChooser.setCollection(fields.iterator());
  }

  void setField(GridDatatype field) {
    int idx = fieldChooser.setSelectedByName(field.getName());
    if (idx < 0)
      fieldChooser.setSelectedByIndex(0);
    fieldChooser.setToolTipText( field.getDescription());

    GridCoordSystem gcs = field.getCoordinateSystem();

      // levels
    CoordinateAxis1D axis = gcs.getVerticalAxis();
    setChooserWanted("level", axis != null);
    if (axis != null) {
      List levels = axis.getNames();
      levelChooser.setCollection(levels.iterator());
      NamedObject no = (NamedObject)levels.get( controller.getCurrentLevelIndex());
      levelChooser.setSelectedByName(no.getName());
    }

      // times
    if (gcs.hasTimeAxis()) {
      axis = gcs.hasTimeAxis1D() ? gcs.getTimeAxis1D() : gcs.getTimeAxisForRun(0);
      setChooserWanted("time", axis != null);
      if (axis != null) {
        List names = axis.getNames();
        timeChooser.setCollection(names.iterator());
        NamedObject no = (NamedObject) names.get(controller.getCurrentTimeIndex());
        timeChooser.setSelectedByName(no.getName());
      }
    } else {
      setChooserWanted("time", false);
    }

    axis = gcs.getEnsembleAxis();
    setChooserWanted("ensemble", axis != null);
    if (axis != null) {
      List names = axis.getNames();
      ensembleChooser.setCollection(names.iterator());
      NamedObject no = (NamedObject) names.get(controller.getCurrentEnsembleIndex());
      ensembleChooser.setSelectedByName(no.getName());
    }

    axis = gcs.getRunTimeAxis();
    setChooserWanted("runtime", axis != null);
    if (axis != null) {
      List names = axis.getNames();
      runtimeChooser.setCollection(names.iterator());
      NamedObject no = (NamedObject) names.get(controller.getCurrentRunTimeIndex());
      runtimeChooser.setSelectedByName(no.getName());
    }

    setChoosers();

    colorScalePanel.setUnitString( field.getUnitsString());
  }

  void setDrawHorizAndVert( boolean drawHoriz, boolean drawVert) {
    drawingPanel.removeAll();
    if (drawHoriz && drawVert) {
      splitDraw.setTopComponent(panz);
      splitDraw.setBottomComponent(vertPanel);
      drawingPanel.add( splitDraw,  BorderLayout.CENTER);
    } else if (drawHoriz) {
      drawingPanel.add( panz,  BorderLayout.CENTER);
    } else if (drawVert) {
      drawingPanel.add( splitDraw,  BorderLayout.CENTER);
    }
  }

  // actions that control the dataset
  private void makeActionsDataset() {

      // choose local dataset
    AbstractAction chooseLocalDatasetAction = new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        String filename = fileChooser.chooseFilename();
        if (filename == null) return;

        InvDataset invDs;
        try {
          invDs = new InvDatasetImpl(filename, FeatureType.GRID, ServiceType.NETCDF);
        } catch (Exception ue) {
          JOptionPane.showMessageDialog(GridUI.this, "Invalid filename = <" + filename + ">\n" + ue.getMessage());
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
    /* chooseProjectionAction = new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        getProjectionManager().show();
      }
    };
    BAMutil.setActionProperties( chooseProjectionAction, null, "Projection Manager...", false, 'P', 0);


    saveCurrentProjectionAction = new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        getProjectionManager();
          // set the bounding box
        ProjectionImpl proj = (ProjectionImpl) panz.getProjectionImpl().clone();
        proj.setDefaultMapArea( panz.getMapArea());
        //if (debug) System.out.println(" GV save projection "+ proj);

        // projManage.setMap(renderAll.get("Map"));   LOOK!
        projManager.saveProjection( proj);
      }
    };
    BAMutil.setActionProperties( saveCurrentProjectionAction, null, "save Current Projection", false, 'S', 0);

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
        controller.start(true);
        controller.draw(true);
      }
    };
    BAMutil.setActionProperties( redrawAction, "alien", "RedRaw", false, 'W', 0);

    showDatasetInfoAction = new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        if (infoWindow == null) {
          datasetInfoTA = new TextHistoryPane();
          infoWindow = new IndependentWindow("Dataset Information", BAMutil.getImage( "GDVs"), datasetInfoTA);
          infoWindow.setSize(700,700);
          infoWindow.setLocation(100,100);
        }

        datasetInfoTA.clear();
        datasetInfoTA.appendLine( controller.getDatasetInfo());
        datasetInfoTA.gotoTop();
        infoWindow.show();
      }
    };
    BAMutil.setActionProperties( showDatasetInfoAction, "Information", "Show info...", false, 'S', -1);

    showNcMLAction = new AbstractAction() {
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
    BAMutil.setActionProperties( showNcMLAction, null, "Show NcML...", false, 'X', -1);

    showGridDatasetInfoAction = new AbstractAction() {
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

      // show gridTable
    showGridTableAction = new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        gtWindow.show();
      }
    };
    BAMutil.setActionProperties( showGridTableAction, "Table", "grid Table...", false, 'T', -1);

      // show netcdf dataset Table
    showNetcdfDatasetAction = new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        NetcdfDataset netcdfDataset = controller.getNetcdfDataset();
        if (null != netcdfDataset) {
          try {
            dsTable.setDataset(netcdfDataset);
          } catch (IOException e1) {
            e1.printStackTrace();
            return;
          }
          dsDialog.show();
        }
      }
    };
    BAMutil.setActionProperties( showNetcdfDatasetAction, "netcdf", "NetcdfDataset Table Info...", false, 'D', -1);

      /* write geotiff file
    geotiffAction = new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        GeoGrid grid = controller.getCurrentField();
        ucar.ma2.Array data = controller.getCurrentHorizDataSlice();
        if ((grid == null) || (data == null)) return;

        String filename = geotiffFileChooser.chooseFilename();
        if (filename == null) return;

        GeoTiff geotiff = null;
        try {
          /* System.out.println("write to= "+filename);
          ucar.nc2.geotiff.Writer.write2D(grid, data, filename+".tfw");
          geotiff = new GeoTiff(filename); // read back in
          geotiff.read();
          System.out.println( geotiff.showInfo());
          //geotiff.testReadData();
          geotiff.close(); * /

          // write two
          ucar.nc2.geotiff.GeotiffWriter writer = new ucar.nc2.geotiff.GeotiffWriter(filename);
          writer.writeGrid(grid, data, false);
          geotiff = new GeoTiff(filename); // read back in
          geotiff.read();
          System.out.println( "*************************************");
          System.out.println( geotiff.showInfo());
          //geotiff.testReadData();
          geotiff.close();


        } catch (IOException ioe) {
          ioe.printStackTrace();

        } finally {
          try {
            if (geotiff != null) geotiff.close();
          } catch (IOException ioe) { }
        }

      }
    };
    BAMutil.setActionProperties( geotiffAction, "Geotiff", "Write Geotiff file", false, 'G', -1);
    */

    minmaxHorizAction =  new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        csDataMinMax.setSelectedIndex(GridRenderer.HORIZ_MinMaxType);
        controller.setDataMinMaxType(GridRenderer.HORIZ_MinMaxType);
      }
    };
    BAMutil.setActionProperties( minmaxHorizAction, null, "Horizontal plane", false, 'H', 0);
    minmaxVertAction =  new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        csDataMinMax.setSelectedIndex(GridRenderer.VERT_MinMaxType);
        controller.setDataMinMaxType(GridRenderer.VERT_MinMaxType);
      }
    };
    BAMutil.setActionProperties( minmaxVertAction, null, "Vertical plane", false, 'V', 0);
    minmaxVolAction =  new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        csDataMinMax.setSelectedIndex(GridRenderer.VOL_MinMaxType);
        controller.setDataMinMaxType(GridRenderer.VOL_MinMaxType);
      }
    };
    BAMutil.setActionProperties( minmaxVolAction, null, "Grid volume", false, 'G', 0);
    minmaxHoldAction =  new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        csDataMinMax.setSelectedIndex(GridRenderer.HOLD_MinMaxType);
        controller.setDataMinMaxType(GridRenderer.HOLD_MinMaxType);
      }
    };
    BAMutil.setActionProperties( minmaxHoldAction, null, "Hold scale constant", false, 'C', 0);

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
    BAMutil.setActionProperties( navToolbarAction, "MagnifyPlus", "show Navigate toolbar", true, 'M', 0);
    navToolbarAction.putValue(BAMutil.STATE, new Boolean(store.getBoolean( "navToolbarAction", true)));

    moveToolbarAction = new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        Boolean state = (Boolean) getValue(BAMutil.STATE);
        if (state.booleanValue())
          toolPanel.add(moveToolbar);
        else
          toolPanel.remove(moveToolbar);
      }
    };
    BAMutil.setActionProperties( moveToolbarAction, "Up", "show Move toolbar", true, 'M', 0);
    moveToolbarAction.putValue(BAMutil.STATE, new Boolean(store.getBoolean( "moveToolbarAction", true)));
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
  }

  public ProjectionManager getProjectionManager() {
    if (null != projManager)
      return projManager;

    projManager = new ProjectionManager(topLevel.getRootPaneContainer(), store);
    projManager.addPropertyChangeListener(  new java.beans.PropertyChangeListener() {
      public void propertyChange( java.beans.PropertyChangeEvent e) {
        if (e.getPropertyName().equals("ProjectionImpl")) {
          ProjectionImpl p = (ProjectionImpl) e.getNewValue();
          p = (ProjectionImpl) p.clone();
          //System.out.println("UI: new Projection "+p);
          controller.setProjection( p);
        }
      }
    });

    return projManager;
  }
  */

  private void makeUI(int defaultHeight) {

    datasetNameLabel = new JLabel();
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

    // top tool panel
    toolPanel = new JPanel();
    toolPanel.setBorder(new EtchedBorder());
    toolPanel.setLayout(new thredds.ui.MFlowLayout(FlowLayout.LEFT, 0, 0));

    // menus
    JMenu dataMenu = new JMenu("Dataset");
    dataMenu.setMnemonic('D');
    configMenu = new JMenu("Configure");
    configMenu.setMnemonic('C');
    JMenu toolMenu = new JMenu("Controls");
    toolMenu.setMnemonic( 'T');
    addActionsToMenus(dataMenu, configMenu, toolMenu);
    JMenuBar menuBar = new JMenuBar();
    menuBar.add(dataMenu);
    menuBar.add(configMenu);
    menuBar.add(toolMenu);
    toolPanel.add(menuBar);

    // field choosers
    fieldPanel = new JPanel();
    fieldPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));
    toolPanel.add( fieldPanel);

    // stride
    toolPanel.add( controller.strideSpinner);

    // buttcons
    BAMutil.addActionToContainer( toolPanel, controller.drawHorizAction);
    BAMutil.addActionToContainer( toolPanel, controller.drawVertAction);
    mapBeanMenu = MapBean.makeMapSelectButton();
    toolPanel.add( mapBeanMenu.getParentComponent());

    // the Navigated panel and its toolbars
    panz.setLayout(new FlowLayout());
    navToolbar = panz.getNavToolBar();
    moveToolbar = panz.getMoveToolBar();
    if (((Boolean)navToolbarAction.getValue(BAMutil.STATE)).booleanValue())
      toolPanel.add(navToolbar);
    if (((Boolean)moveToolbarAction.getValue(BAMutil.STATE)).booleanValue())
      toolPanel.add(moveToolbar);

    BAMutil.addActionToContainer( toolPanel, panz.setReferenceAction);
    BAMutil.addActionToContainer( toolPanel, controller.dataProjectionAction);
    BAMutil.addActionToContainer( toolPanel, controller.showGridAction);
    BAMutil.addActionToContainer( toolPanel, controller.showContoursAction);
    BAMutil.addActionToContainer( toolPanel, controller.showContourLabelsAction);

    BAMutil.addActionToContainer( toolPanel, redrawAction);

      //  vertical split
    vertPanel = new VertPanel();
    splitDraw = new JSplitPane(JSplitPane.VERTICAL_SPLIT, panz, vertPanel);
    int divLoc = store.getInt( "vertSplit", 2*defaultHeight/3);
    splitDraw.setDividerLocation(divLoc);
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
    panz.setPositionLabel( positionLabel);

    // colorscale panel
    colorScalePanel = new ColorScale.Panel(this, controller.getColorScale());
    String [] csDataTypes = {"horiz", "vert", "Vol", "hold"};
    csDataMinMax = new JComboBox( csDataTypes);
    csDataMinMax.setToolTipText("ColorScale Min/Max setting");
    csDataMinMax.addActionListener( new AbstractAction () {
      public void actionPerformed(ActionEvent e) {
        //System.out.println("csDataType = "+csDataType.getSelectedItem());
        controller.setDataMinMaxType(csDataMinMax.getSelectedIndex());
      }
    });
    JPanel westPanel = new JPanel(new BorderLayout());
    westPanel.add( colorScalePanel, BorderLayout.CENTER);
    westPanel.add( csDataMinMax, BorderLayout.NORTH);

    // lay it out
    JPanel northPanel = new JPanel();
    //northPanel.setLayout( new BoxLayout(northPanel, BoxLayout.Y_AXIS));
    northPanel.setLayout( new BorderLayout());
    northPanel.add( datasetNameLabel, BorderLayout.NORTH);
    northPanel.add( toolPanel, BorderLayout.SOUTH);

    setLayout(new BorderLayout());
    add(northPanel, BorderLayout.NORTH);
    add(statusPanel, BorderLayout.SOUTH);
    add(westPanel, BorderLayout.WEST);
    add(drawingPanel, BorderLayout.CENTER);

    setDrawHorizAndVert( controller.drawHorizOn, controller.drawVertOn);
  }

  private ArrayList choosers;
  private void setChoosers() {
    fieldPanel.removeAll();
    for (int i = 0; i < choosers.size(); i++) {
      Chooser c = (Chooser) choosers.get(i);
      if (c.isWanted)
        fieldPanel.add(c.field);
    }
  }

  private class Chooser {
    Chooser(String name, SuperComboBox field, boolean want){
      this.name = name;
      this.field = field;
      this.isWanted = want;
    }
    boolean isWanted;
    String  name;
    SuperComboBox field;
  }

  private void setChooserWanted(String name, boolean want) {
    for (int i = 0; i < choosers.size(); i++) {
      Chooser chooser = (Chooser) choosers.get(i);
      if (chooser.name.equals(name)) chooser.isWanted = want;
    }
  }

  private void addToolbarOption(String toolbarName, JToolBar toolbar, AbstractAction act) {
    boolean wantsToolbar = store.getBoolean( toolbarName, true);
    if (wantsToolbar)
      toolPanel.add(toolbar);
 }

  void addActionsToMenus(JMenu datasetMenu, JMenu configMenu, JMenu toolMenu) {
      // Info
    BAMutil.addActionToMenu( datasetMenu, showGridTableAction);
    BAMutil.addActionToMenu( datasetMenu, showDatasetInfoAction);
    BAMutil.addActionToMenu( datasetMenu, showNcMLAction);
    BAMutil.addActionToMenu( datasetMenu, showGridDatasetInfoAction);
    BAMutil.addActionToMenu( datasetMenu, showNetcdfDatasetAction);
    // BAMutil.addActionToMenu( datasetMenu, geotiffAction);
    //BAMutil.addActionToMenu( infoMenu, showNetcdfXMLAction);

    /// Configure
    JMenu toolbarMenu = new JMenu("Toolbars");
    toolbarMenu.setMnemonic( 'T');
    configMenu.add(toolbarMenu);
    BAMutil.addActionToMenu( toolbarMenu, navToolbarAction);
    BAMutil.addActionToMenu( toolbarMenu, moveToolbarAction);


    /* BAMutil.addActionToMenu( configMenu, chooseColorScaleAction);
    BAMutil.addActionToMenu( configMenu, chooseProjectionAction);
    BAMutil.addActionToMenu( configMenu, saveCurrentProjectionAction);
    BAMutil.addActionToMenu( configMenu, controller.dataProjectionAction);
    */

    //// tools menu
    JMenu displayMenu = new JMenu("Display control");
    displayMenu.setMnemonic( 'D');

    BAMutil.addActionToMenu( displayMenu, controller.showGridAction);
    BAMutil.addActionToMenu( displayMenu, controller.showContoursAction);
    BAMutil.addActionToMenu( displayMenu, controller.showContourLabelsAction);
    BAMutil.addActionToMenu( displayMenu, redrawAction);
    toolMenu.add(displayMenu);

    // Loop Control
    JMenu loopMenu = new JMenu("Loop control");
    loopMenu.setMnemonic( 'L');

    BAMutil.addActionToMenu( loopMenu, fieldLoopAction);
    BAMutil.addActionToMenu( loopMenu, levelLoopAction);
    BAMutil.addActionToMenu( loopMenu, timeLoopAction);
    toolMenu.add(loopMenu);

    // MinMax Control
    JMenu mmMenu = new JMenu("ColorScale min/max");
    mmMenu.setMnemonic('C');
    BAMutil.addActionToMenu( mmMenu, minmaxHorizAction);
    BAMutil.addActionToMenu( mmMenu, minmaxVertAction);
    BAMutil.addActionToMenu( mmMenu, minmaxVolAction);
    BAMutil.addActionToMenu( mmMenu, minmaxHoldAction);
    toolMenu.add(mmMenu);

    // Zoom/Pan
    JMenu zoomMenu = new JMenu("Zoom/Pan");
    zoomMenu.setMnemonic('Z');
    panz.addActionsToMenu( zoomMenu); // items are added by NavigatedPanelToolbar
    toolMenu.add(zoomMenu);
  }

  private class LoopControlAction extends AbstractAction {
    SuperComboBox scbox;
    LoopControlAction( SuperComboBox cbox) {
      this.scbox = cbox;
      BAMutil.setActionProperties( this, null, cbox.getName(), false, 0, 0);
    }
    public void actionPerformed(ActionEvent e) {
      scbox.getLoopControl().show();
    }
  }

  private class OpenDatasetTask extends ProgressMonitorTask implements ucar.nc2.util.CancelTask {
    ucar.nc2.thredds.ThreddsDataFactory factory;
    thredds.catalog.InvDataset invds;

    OpenDatasetTask(thredds.catalog.InvDataset ds) {
      factory = new ucar.nc2.thredds.ThreddsDataFactory();
      this.invds = ds;
    }

    public void run() {
      NetcdfDataset dataset = null;
      GridDataset gridDataset = null;
      Formatter errlog = new Formatter();

      try {
        dataset = factory.openDataset( invds, true, this, errlog);
        gridDataset = new ucar.nc2.dt.grid.GridDataset(dataset);

      } catch (IOException e) {
        setError("Failed to open datset: "+errlog);
      }

      success = !cancel && (gridDataset != null);
      if (success) controller.setGridDataset( gridDataset);
      done = true;
    }
  }

}