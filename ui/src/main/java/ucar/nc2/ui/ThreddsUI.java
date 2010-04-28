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

import thredds.catalog.ui.query.QueryChooser;
import thredds.ui.*;
import thredds.catalog.*;
import thredds.catalog.ui.*;

import ucar.util.prefs.PreferencesExt;
import ucar.util.prefs.ui.*;
import ucar.nc2.util.IO;

import java.awt.*;
import java.awt.event.*;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;
import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.filechooser.FileFilter;

/**
 * This is the THREDDS User Interface for nj22 ToolsUI.
 * Throws PropertyChangeEvent when a dataset is selected, see addPropertyChangeListener.
 *
 * @author caron
 */
public class ThreddsUI extends JPanel {
    // store keys
  static private final String VIEWER_SIZE = "ViewerSize";
  static private final String SOURCE_WINDOW_SIZE = "SourceWindowSize";
  static private final String XML_WINDOW_SIZE = "XmlWindowSize";

  // tabs

  private PreferencesExt store;
  private Component parent;

  // the main components
  private ThreddsDatasetChooser datasetChooser = null;
  private thredds.catalog.ui.tools.CatalogEnhancer catEditor = null;
  private thredds.catalog.ui.tools.DLCrawler catCrawler = null;
  private thredds.catalog.ui.tools.TDServerConfigurator serverConfigure = null;

  // UI components that need global scope
  private TextGetPutPane sourcePane;
  private JTabbedPane tabbedPane;
  //private JMenu debugMenu;

  private TextHistoryPane xmlPane;
  private IndependentDialog xmlWindow = null;
  private IndependentWindow sourceWindow = null;

  // the various managers and dialog boxes
  FileManager fileChooser = null; // shared with component viewers

  //private JDialog datasetChooserDialog;
  //private IndependentDialog datasetURLDialog = null;

  // debugging
  //private boolean debugBeans = false, debugChooser = false, debugPrint = false, debugHelp = false;
  private boolean debugSelection = false;
  private boolean debugTab = false;

  public ThreddsUI(JFrame parent, PreferencesExt store) {
    this.store = store;
    this.parent = parent;
    //parent = topLevel.getRootPaneContainer().getRootPane();

    enableEvents(AWTEvent.WINDOW_EVENT_MASK);
    Dimension d = (Dimension) store.getBean( VIEWER_SIZE, null);
    int defaultWidth = 700;
    int defaultHeight = 350;
    setPreferredSize((d != null) ? d : new Dimension(defaultWidth, defaultHeight));

    try  {
      makeActionsSystem();
      makeActionsDataset();

      //makeMenu();
      makeUI();

    } catch (Exception e) {
      System.out.println("UI creation Exception");
      e.printStackTrace();
    }

    // other components
    PreferencesExt fcPrefs = (PreferencesExt) store.node("FileManager");
    FileFilter[] filters = new FileFilter[] {new FileManager.NetcdfExtFilter()};
    fileChooser = new FileManager(parent, null, filters, fcPrefs);
  }

  private void makeUI() throws Exception  {
    tabbedPane = new JTabbedPane(JTabbedPane.TOP);

    /// catalog, DQC, query choosers
    datasetChooser = makeDatasetChooser(); // adds itself to the JTabbedPane

    // all the other component are defferred for fast startup
    tabbedPane.addTab("catCrawler", new JLabel("catCrawler"));
    tabbedPane.addTab("Catalog Enhancer", new JLabel("Catalog Enhancer"));
    tabbedPane.addTab("TDS Configure", new JLabel("TDS Configure"));
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

     // panel to show source
    sourcePane = new TextGetPutPane( (PreferencesExt) store.node("getputPane"));
    sourceWindow = new IndependentWindow( "Source", BAMutil.getImage( "thredds"), sourcePane);
    sourceWindow.setBounds((Rectangle)store.getBean(SOURCE_WINDOW_SIZE, new Rectangle(50, 50, 725, 450)));

     // panel to show xml data
    xmlPane = new TextHistoryPane( false);
    xmlWindow = new IndependentDialog( null, false, "XML data", xmlPane);
    xmlWindow.setBounds((Rectangle)store.getBean(XML_WINDOW_SIZE, new Rectangle(50, 50, 725, 450)));

      //catIndexer = new thredds.catalog.search.ui.CatalogIndexer((PreferencesExt) store.node("catIndexer"), topLevel.getJFrame());
      // tabbedPane.addTab("Indexer", catIndexer);

    setLayout( new BorderLayout());
    add(tabbedPane, BorderLayout.CENTER);
  }

  private ThreddsDatasetChooser makeDatasetChooser() {

    datasetChooser = new ThreddsDatasetChooser( (PreferencesExt) store.node("ThreddsDatasetChooser"), tabbedPane);

    if (Debug.isSet("System/filterDataset"))
      datasetChooser.setDatasetFilter( new DatasetFilter.ByServiceType( ServiceType.DODS));

    datasetChooser.addPropertyChangeListener(  new java.beans.PropertyChangeListener() {

      public void propertyChange( java.beans.PropertyChangeEvent e) {
        if (e.getPropertyName().equals("InvAccess")) {
          firePropertyChangeEvent( e);
          return;
        }

        if (e.getPropertyName().equals("Dataset") || e.getPropertyName().equals("CoordSys") || e.getPropertyName().equals("File")) {
          // intercept XML, ASCII return types
          InvDataset ds = (InvDataset) e.getNewValue();
          InvAccess access = ds.getAccess( ServiceType.HTTPServer);
          if (access != null) {
            DataFormatType format = access.getDataFormatType();
            if (format == DataFormatType.PLAIN || format == DataFormatType.XML) {
              String urlString = access.getStandardUrlName();
              //System.out.println("got station XML data access = "+urlString);
              IO.readURLcontents(urlString);
              xmlPane.setText( IO.readURLcontents(urlString));
              xmlPane.gotoTop();
              xmlWindow.setVisible(true);
              return;
            }
          }
          firePropertyChangeEvent( e);
        }
      }
    });

    // add a show source button to catalog chooser
    JButton catSource = new JButton("Source");
    catSource.addActionListener( new ActionListener() {
      public void actionPerformed(ActionEvent evt) {
        CatalogChooser cc = datasetChooser.getCatalogChooser();
        String catURL = cc.getCurrentURL();
        //InvCatalogImpl cat = (InvCatalogImpl) datasetChooser.getCatalogChooser().getCurrentCatalog();
        //String catURL = cat.getUriString();
        if (debugSelection) System.out.println("Catalog Source: url = "+catURL);
        sourcePane.setURL( catURL);
        sourcePane.gotoTop();
        sourceWindow.show();
      }
    });
    datasetChooser.getCatalogChooser().addButton(catSource);

    QueryChooser queryChooser = datasetChooser.getQueryChooser();
    if (queryChooser != null) {
      // add a show source button to dqc chooser
      JButton dcqSource = new JButton("Source");
      dcqSource.addActionListener( new ActionListener() {
        public void actionPerformed(ActionEvent evt) {
          String catURL = datasetChooser.getQueryChooser().getCurrentURL();
          sourcePane.setURL( catURL);
          sourcePane.gotoTop();
          sourceWindow.show();
        }
      });
      queryChooser.addButton(dcqSource);

      // add a show source button to dqc catalog chooser
      JButton dqcCatSource = new JButton("Source");
      dqcCatSource.addActionListener( new ActionListener() {
        public void actionPerformed(ActionEvent evt) {
          String catURL = datasetChooser.getQueryChooser().getCatalogChooser().getCurrentURL();
          System.out.println("DQC Catalog Source: url = "+catURL);
          sourcePane.setURL( catURL);
          sourcePane.gotoTop();
          sourceWindow.show();
        }
      });
      queryChooser.getCatalogChooser().addButton(dqcCatSource);
    }

    return datasetChooser;
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
    if (title.equals("catCrawler")) {
      catCrawler = new thredds.catalog.ui.tools.DLCrawler((PreferencesExt) store.node("catCrawler"), parent);
      c = catCrawler;

    } else if (title.equals("TDS Configure")) {
      serverConfigure = new thredds.catalog.ui.tools.TDServerConfigurator((PreferencesExt) store.node("serverConfigure"), parent);
      c = serverConfigure;

    } else if (title.equals("Catalog Enhancer")) {
      catEditor = new thredds.catalog.ui.tools.CatalogEnhancer((PreferencesExt) store.node("catEditor"), parent);
      c = catEditor;

     } else {
      System.out.println("tabbedPane unknown component " + title);
      return;
    }

    parent.setComponentAt(idx, c);
    if (debugTab) System.out.println("tabbedPane changed " + title + " added ");
  }


      /** save all data in the PersistentStore */
  public void storePersistentData() {
    store.putBeanObject(VIEWER_SIZE, getSize());
    store.putBeanObject(SOURCE_WINDOW_SIZE, (Rectangle) sourceWindow.getBounds());

    if (fileChooser != null)
      fileChooser.save();

    if (datasetChooser != null)
      datasetChooser.save();

    if (sourcePane != null)
      sourcePane.save();

    if (catEditor != null)
      catEditor.save();

    if (catCrawler != null)
      catCrawler.save();

    if (serverConfigure != null)
      serverConfigure.save();
  }

   /**
   * Add a PropertyChangeEvent Listener. Throws a PropertyChangeEvent:
   * <ul>
   * <li>  propertyName = "Dataset" or "File", getNewValue() = InvDataset chosen.
   * <li>  propertyName = "Datasets", getNewValue() = InvDataset[] chosen. This can only happen if
   *  you have set doResolve = true, and the resolved dataset is a list of datasets.
   * </ul>
   */
  public void addPropertyChangeListener( PropertyChangeListener l) {
    listenerList.add(PropertyChangeListener.class, l);
  }

  /**
   * Remove a PropertyChangeEvent Listener.
   */
  public void removePropertyChangeListener( PropertyChangeListener l) {
    listenerList.remove(PropertyChangeListener.class, l);
  }

  private void firePropertyChangeEvent(PropertyChangeEvent event) {

    // Process the listeners last to first
    Object[] listeners = listenerList.getListenerList();
    for (int i = listeners.length-2; i>=0; i-=2) {
      if (listeners[i] == PropertyChangeListener.class) {
        ((PropertyChangeListener)listeners[i+1]).propertyChange(event);
      }
    }
  }

  public void setDataset( String location) {
    datasetChooser.getCatalogChooser().setCatalog( location);
    tabbedPane.setSelectedComponent( datasetChooser.getCatalogChooser());
  }

 /* private boolean chooseDataset(String url) {
    InvDataset invDs = new InvDatasetImpl( fname, ServerType.NETCDF);
    return chooseDataset( invDs);
  }

  void setDataset(InvDataset ds) {
    if (ds == null) return;

    InvAccess access = ds.getAccess( ServiceType.HTTPServer);
    if (access != null) {
      if ((access.getDataFormatType() == DataFormatType.PLAIN) ||
        (access.getDataFormatType() == DataFormatType.XML)) {
        String urlString = access.getStandardUrlName();
        System.out.println("got station XML data access = "+urlString);
        thredds.util.IO.readURLcontents(urlString);
        xmlPane.setText( thredds.util.IO.readURLcontents(urlString));
        xmlPane.gotoTop();
        xmlWindow.show();
        return;
      }
    }

    if (ds.getDataType() == DataType.IMAGE) {
      //setImageAccess( ds);
      //setTab(TAB_IMAGE);
      return;
    }

    else { // !! if (ds.getDataType() == DataType.GRID) {
      // LOOK what to do with dataset ??
      // gridViewer.setDataset( ds);

      //setTab(TAB_GRID);
      return;
    }

    /* JOptionPane.showMessageDialog(UI.this, ds.getName()+": unknown data type= "+ds.getDataType(),
        "Error opening dataset", JOptionPane.ERROR_MESSAGE);

    return;
  } */

  /* private boolean setImageAccess(InvDataset ds) {
    if (ds == null) return false;

    InvAccess access = ds.getAccess( ServiceType.ADDE);
    if (access == null) return false;

    setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

    if (!(imageViewer.setImageFromUrl( access.getStandardUrlName()))) {
      setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
      return false;
    }

    /* if (!imageViewer.isSelected()) {
      gridViewer.setSelected(false);
      imageViewer.setSelected(true);
      // stnViewer.setSelected(false);
      //viewerPanel.removeAll();
      //viewerPanel.add(imageViewer, BorderLayout.CENTER);
      revalidate();
    }

    setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
    return true;

/*
    } else if (ds.getDataType() == DataType.STATION) {

      if (!(stnViewer.setStations( ds))) {
        setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        return false;
      }

      if (!stnViewer.isSelected()) {
        gridViewer.setSelected(false);
        imageViewer.setSelected(false);
        stnViewer.setSelected(true);
        viewerPanel.removeAll();
        viewerPanel.add(stnViewer, BorderLayout.CENTER);
        revalidate();
      }

    }
    return true;

  } */

  /* private void addToDatasetList( String name) {
    int idx;
    if (0 <= (idx = recentDatasetList.indexOf(name))) // move to the top
      recentDatasetList.remove(idx);
    recentDatasetList.add(0, name);
  } */

  // actions that are system-wide
  private void makeActionsSystem() {

    /* aboutAction = new AbstractAction() {
      public void actionPerformed(ActionEvent evt) {
        new AboutWindow();
      }
    };
    BAMutil.setActionProperties( aboutAction, null, "About", false, 'A', 0); */

    /* printAction = new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        PrinterJob printJob = PrinterJob.getPrinterJob();
        PageFormat pf = printJob.defaultPage();

        // do we need to rotate ??
        if (panz.wantRotate( pf.getImageableWidth(), pf.getImageableHeight()))
          pf.setOrientation( PageFormat.LANDSCAPE);
        else
          pf.setOrientation(PageFormat.PORTRAIT);

        printJob.setPrintable(controller.getPrintable(), pf);
        if (printJob.printDialog()) {
          try {
            if (Debug.isSet("print.job")) System.out.println("call printJob.print");
            printJob.print();
            if (Debug.isSet("print.job")) System.out.println(" printJob done");
          } catch (Exception PrintException) {
            PrintException.printStackTrace();
          }
        }
      }
    };
    BAMutil.setActionProperties( printAction, "Print", "Print...", false, 'P', KeyEvent.VK_P);

    sysConfigAction = new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        if (sysConfigDialog == null)
          makeSysConfigWindow();
        sysConfigDialog.show();
      }
    };
    BAMutil.setActionProperties( sysConfigAction, "Preferences", "Configure...", false, 'C', -1);

    clearDebugFlagsAction = new AbstractAction() {
      public void actionPerformed(ActionEvent e) { Debug.clear(); }
    };
    BAMutil.setActionProperties( clearDebugFlagsAction, null, "Clear DebugFlags", false, 'D', -1);

    clearRecentAction = new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        recentDatasetList = new ArrayList();
      }
    };
    BAMutil.setActionProperties( clearRecentAction, null, "Clear Recent Datasets", false, 'R', -1);
    */

    AbstractAction clearDebugFlagsAction = new AbstractAction() {
      public void actionPerformed(ActionEvent e) { /* Debug.clear(); */ }
    };
    BAMutil.setActionProperties(clearDebugFlagsAction, null, "Clear Debug Flags", false, 'D', -1);

    AbstractAction setDebugFlagsAction = new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        // LOOK set netcdf debug flags

        InvCatalogFactory.debugURL = Debug.isSet("InvCatalogFactory/debugURL");
        InvCatalogFactory.debugOpen = Debug.isSet("InvCatalogFactory/debugOpen");
        InvCatalogFactory.debugVersion = Debug.isSet("InvCatalogFactory/debugVersion");
        InvCatalogFactory.showParsedXML = Debug.isSet("InvCatalogFactory/showParsedXML");
        InvCatalogFactory.showStackTrace = Debug.isSet("InvCatalogFactory/showStackTrace");
        InvCatalogFactory.debugXML = Debug.isSet("InvCatalogFactory/debugXML");
        InvCatalogFactory.debugDBurl = Debug.isSet("InvCatalogFactory/debugDBurl");
        InvCatalogFactory.debugXMLopen = Debug.isSet("InvCatalogFactory/debugXMLopen");
        InvCatalogFactory.showCatalogXML = Debug.isSet("InvCatalogFactory/showCatalogXML");
      }
    };
    BAMutil.setActionProperties(setDebugFlagsAction, null, "Set Debug Flags", false, 'S', -1);

    /* exitAction = new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        topLevel.close();
      }
    };
    BAMutil.setActionProperties( exitAction, "Exit", "Exit", false, 'X', -1); */

  }

  // actions that control the dataset
  private void makeActionsDataset() {

      /* choose local dataset
    chooseLocalDatasetAction = new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        if ( null == fileChooser) {
          String dirName = store.get(FILECHOOSER_DEFAULTDIR, null);
          fileChooser = new FileManager(parent, dirName, new FileManager.NetcdfExtFilter());
        }
        java.io.File file = fileChooser.chooseFile();
        if (file == null) return;

        InvDataset invDs;
        try {
          invDs = new InvDatasetImpl( file.toURI().toString(), DataType.GRID, ServiceType.NETCDF);
        } catch (Exception ue) {
          javax.swing.JOptionPane.showMessageDialog(UI.this, "Invalid filename = <"+file+">\n"+ue.getMessage());
          ue.printStackTrace();
          return;
        }
        setDataset( invDs);
      }
    };
    BAMutil.setActionProperties( chooseLocalDatasetAction, "Choose", "open Local dataset...", false, 'L', -1); */

    /*choose THREDDS dataset
    chooseThreddsDatasetAction = new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        datasetChooserDialog.show();
      }
    };
    BAMutil.setActionProperties( chooseThreddsDatasetAction, "Choose", "open THREDDS dataset...", false, 'R', -1);
    */
      /* enter dataset URL
    datasetURLAction = new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        if (datasetURLDialog == null) {
          datasetURLpp = new faqo.ui.PreferencePanel( "Enter Dataset URL", new faqo.ui.PreferenceData(store), null);
          datasetURLpp.addTextField( DATASET_URL, "dataset URL", "                                                  ");
          datasetURLpp.finishSetup();
          datasetURLpp.addActionListener( new ActionListener() {
            public void actionPerformed(ActionEvent e) {
              String url = (String) datasetURLpp.getValue(DATASET_URL);
              System.out.println("dataset url = "+url);
              if (chooseDataset( url)) {
                datasetURLDialog.setVisible(false);
              }
            }
          });
          datasetURLDialog = new IndependentWindow( "Enter Dataset URL", datasetURLpp);
        }
        datasetURLDialog.show();
      }
    };
    BAMutil.setActionProperties( datasetURLAction, null, "enter dataset URL...", false, 'U', -1);

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

  }

  /* private void setTab( String tabName) {
    if (tabName.equals(TAB_CHOOSER))
      tabbedPane.setSelectedIndex(0);
    else if (tabName.equals(TAB_GRID))
      tabbedPane.setSelectedComponent(gridViewer);
    else if (tabName.equals(TAB_IMAGE))
      tabbedPane.setSelectedComponent(imageViewer);
  } */

  /* private void makeMenu() {
    JMenuBar mb = new JMenuBar();
    JRootPane rootPane = topLevel.getRootPaneContainer().getRootPane();
    rootPane.setJMenuBar(mb);

    /// System menu
    JMenu sysMenu = new JMenu("System");
    sysMenu.setMnemonic('S');
    mb.add(sysMenu);
    //BAMutil.addActionToMenu( sysMenu, printAction);

    JMenu plafMenu = new JMenu("Look and Feel");
    plafMenu.setMnemonic('L');
    sysMenu.add(plafMenu);
    PLAF plaf = new PLAF(rootPane);
    plaf.addToMenu( plafMenu);

    if (Debug.isSet("util/configure")) BAMutil.addActionToMenu( sysMenu, sysConfigAction);

    debugMenu = (JMenu) sysMenu.add(new JMenu("Debug"));
    debugMenu.addMenuListener( new MenuListener() {
      public void menuSelected(MenuEvent e) { ucar.util.prefs.ui.Debug.constructMenu(debugMenu); }
      public void menuDeselected(MenuEvent e) {}
      public void menuCanceled(MenuEvent e) {}
    });

    BAMutil.addActionToMenu( sysMenu, setDebugFlagsAction);
    //BAMutil.addActionToMenu( sysMenu, clearDebugFlagsAction);
    /* if (null != System.getProperty("admin")) {
      JMenu adminMenu = new JMenu("Admin");
      plafMenu.setMnemonic('A');
      sysMenu.add(adminMenu);
      BAMutil.addActionToMenu( adminMenu, clearRecentAction);
      BAMutil.addActionToMenu( adminMenu, clearDebugFlagsAction);
    }

    BAMutil.addActionToMenu( sysMenu, exitAction);
    if (topLevel.isApplet())
      exitAction.setEnabled( false);

    /* Data menu
    dataMenu = new JMenu("Dataset");
    dataMenu.setMnemonic('D');
    mb.add(dataMenu);
    //BAMutil.addActionToMenu( dataMenu, chooseLocalDatasetAction);
    //BAMutil.addActionToMenu( dataMenu, chooseThreddsDatasetAction);
    //BAMutil.addActionToMenu( dataMenu, datasetURLAction);

      /* permanent datasets
      savedDatasetMenu = new JMenu("saved");
      //dataMenu.add( savedDatasetMenu);
      savedDatasetList = (ArrayList) store.getBean( "savedDatasetList", null);
      if (null != savedDatasetList) {
        Iterator iter = savedDatasetList.iterator();
        while (iter.hasNext())
          BAMutil.addActionToMenu( savedDatasetMenu, new DatasetAction( (String) iter.next()));
      } else
        savedDatasetList = new ArrayList(); */

        // Info
    /* JMenu infoMenu = new JMenu("Info");
    infoMenu.setMnemonic('I');
    mb.add(infoMenu);

    /// Configure
    configMenu = new JMenu("Configure");
    configMenu.setMnemonic('C');
    mb.add(configMenu);

    //// tools menu
    JMenu toolMenu = new JMenu("Tools");
    toolMenu.setMnemonic( 'T');
    mb.add(toolMenu);

   // Help
    JMenu helpMenu = new JMenu("Help");
    helpMenu.setMnemonic('H');
    mb.add(helpMenu);
    // BAMutil.addActionToMenu( helpMenu, controller.helpAction);
    BAMutil.addActionToMenu( helpMenu, aboutAction);

    // other UI's add their actions to menu
    //gridViewer.addActionsToMenus(dataMenu, configMenu, toolMenu);
  } */
}


