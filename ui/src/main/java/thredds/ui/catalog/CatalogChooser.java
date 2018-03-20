/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package thredds.ui.catalog;

import thredds.client.catalog.*;
import thredds.client.catalog.tools.DatasetHtmlWriter;
import ucar.nc2.ui.widget.*;
import ucar.util.prefs.PreferencesExt;
import ucar.util.prefs.ui.ComboBox;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.net.URISyntaxException;
import java.util.Formatter;

/**
 * A Swing widget for THREDDS clients to access and choose from Dataset Inventory catalogs.
 * State is maintained in a ucar.util.Preferences store.
 * <p>
 * A list of catalogs is kept in a ComboBox, and the user can choose from them and add new ones.
 * When a catalog is chosen, its contents are displayed in a CatalogTreeView. As the datasets
 * are browsed, the metadata is displayed in an HtmlBrowser widget.
 * <p>
 * When a new dataset is selected, a java.beans.PropertyChangeEvent is thrown, see
 * addPropertyChangeListener.
 * <p>
 * Use Example:
 *
 *  <pre>
    // create widgets
    catalogChooser = new thredds.ui.CatalogChooser( prefs);
    catalogChooserDialog = catalogChooser.makeDialog(rootPaneContainer, "Open THREDDS dataset", true);

      // listen for selection
    catalogChooser.addPropertyChangeListener(  new java.beans.PropertyChangeListener() {
      public void propertyChange( java.beans.PropertyChangeEvent e) {
        if (e.getPropertyName().equals("Dataset")) {
          ..
        }
     }
   });

      // popup dialog
    catalogChooserDialog.show();
 * </pre>
 *
 * You can use the CatalogChooser alone, wrap it into a JDialog for popping up, or
 * use a ThreddsDatasetChooser instead, for a more complete interface.
 *
 * @see ThreddsDatasetChooser
 *
 * @author John Caron
 */

public class CatalogChooser extends JPanel {
  private static final String HDIVIDER = "HSplit_Divider";
  private static final String FILECHOOSER_DEFAULTDIR = "FileChooserDefaultDir";
  private PreferencesExt prefs;

  private String eventType = null;

    // ui
  private ComboBox catListBox;
  private CatalogTreeView tree;
  private HtmlBrowser htmlViewer;
  private FileManager fileChooser;

  private JSplitPane split;
  private JLabel statusLabel;
  private JPanel buttPanel;
  private JLabel sourceText;
  private RootPaneContainer parent = null;

  private boolean datasetEvents = true;
  private boolean catrefEvents = false;
  private String currentURL = "";

  //private boolean catgenShow = true;
  private FileManager catgenFileChooser;

  private boolean debugEvents = false;
  //private boolean debugTree = false;
  private boolean showHTML = false;

  /**
   * Constructor, with control over whether a comboBox of previous catalogs is shown.
   *
   * @param prefs           persistent storage, may be null.
   * @param showComboChooser comboBox persists catalog URLs
   * @param showOpenButton  show the "open" button.
   * @param showFileChooser show a FileChooser (must have showComboChooser true)
   */
  public CatalogChooser(PreferencesExt prefs, boolean showComboChooser,
                        boolean showOpenButton, boolean showFileChooser) {
    this.prefs = prefs;

    JPanel topPanel = null;

    if (showComboChooser) {

      // combo box holds the catalogs
      catListBox = new ComboBox(prefs);

      // top panel buttons
      JButton connectButton = new JButton("Connect");
      connectButton.setToolTipText("read this catalog");
      connectButton.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent evt) {
          String catalogURL = (String) catListBox.getSelectedItem();
          tree.setCatalog(catalogURL.trim()); // will get "Catalog" property change event if ok
        }
      });

      JPanel topButtons = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
      topButtons.add(connectButton);

      topPanel = new JPanel(new BorderLayout());
      topPanel.add(new JLabel("Catalog URL"), BorderLayout.WEST);
      topPanel.add(catListBox, BorderLayout.CENTER);
      topPanel.add(topButtons, BorderLayout.EAST);

      if (showFileChooser) {
         // add a file chooser
        PreferencesExt fcPrefs = (PreferencesExt) prefs.node("FileManager");
        FileFilter[] filters = new FileFilter[] {new FileManager.XMLExtFilter()};
        fileChooser = new FileManager(null, null, filters, fcPrefs);

        AbstractAction fileAction =  new AbstractAction() {
          public void actionPerformed(ActionEvent e) {
            String filename = fileChooser.chooseFilename();
            if (filename == null) return;
            tree.setCatalog("file:"+filename);
          }
        };
        BAMutil.setActionProperties( fileAction, "FileChooser", "open Local catalog...", false, 'L', -1);
        BAMutil.addActionToContainer( topButtons, fileAction);

        // a file chooser used for catgen on a directory
        PreferencesExt catgenPrefs = (PreferencesExt) prefs.node("CatgenFileManager");
        catgenFileChooser = new FileManager(null, null, null, catgenPrefs);
        catgenFileChooser.getFileChooser().setFileSelectionMode( JFileChooser.FILES_AND_DIRECTORIES);
        catgenFileChooser.getFileChooser().setDialogTitle( "Run CatGen on Directory");

        AbstractAction srcEditAction = new AbstractAction() {
          public void actionPerformed(ActionEvent e) {
            TextGetPutPane sourceEditor = new TextGetPutPane(null);
            IndependentWindow sourceEditorWindow = new IndependentWindow( "Source", BAMutil.getImage("threddsIcon.png"), sourceEditor);
            sourceEditorWindow.setBounds(new Rectangle(50, 50, 725, 450));
            sourceEditorWindow.show();
          }
        };
        BAMutil.setActionProperties(srcEditAction, "Edit", "Source Editor", false, 'E', -1);
        BAMutil.addActionToContainer(topButtons, srcEditAction);
      }
    }

    // the catalog tree
    tree = new CatalogTreeView();
    tree.addPropertyChangeListener(new java.beans.PropertyChangeListener() {
      public void propertyChange(java.beans.PropertyChangeEvent e) {
        if (debugEvents)
          System.out.println("CatalogChooser propertyChange name=" +e.getPropertyName() + "=");

        if (e.getPropertyName().equals("Catalog")) {
          String catalogURL = (String) e.getNewValue();
          setCurrentURL(catalogURL);
          if (catListBox != null)
            catListBox.addItem(catalogURL);
          firePropertyChangeEvent(e);

        } else if (e.getPropertyName().equals("Selection")) {
          DatasetNode ds = tree.getSelectedDataset();
          if (ds == null)
            return;

          if (ds instanceof Dataset)
            showDatasetInfo((Dataset)ds);

          if (ds instanceof CatalogRef) {
            CatalogRef ref = (CatalogRef) ds;
            String href = ref.getXlinkHref();
            if (href != null) {
              try {
                java.net.URI uri = ref.getParentCatalog().resolveUri(href);
                setCurrentURL(uri.toString());
              } catch (URISyntaxException ee) {
                throw new RuntimeException(ee);
              }
            }
          }
          else if (ds.getParent() == null) { // top
            setCurrentURL(tree.getCatalogURL());
          }

        } else if (e.getNewValue() instanceof Dataset) { // Dataset or File
          firePropertyChangeEvent((Dataset) e.getNewValue(), e.getPropertyName());
        }
      }
    });

    // htmlViewer Viewer
    htmlViewer = new HtmlBrowser();

    // listen for selection
    htmlViewer.addPropertyChangeListener(new java.beans.PropertyChangeListener() {
      public void propertyChange(java.beans.PropertyChangeEvent e) {
        if (e.getPropertyName().equals("datasetURL")) {
          String datasetURL = (String) e.getNewValue();
          if (debugEvents) System.out.println("***datasetURL= " + datasetURL);
          DatasetNode node = tree.getSelectedDataset();
          if (node == null) return;
          if (node instanceof Dataset) {
            Dataset dataset = (Dataset) node;
            Access access = dataset.findAccess(datasetURL);
            firePropertyChangeEvent(new PropertyChangeEvent(this, "InvAccess", null, access));
          }
        } else if (e.getPropertyName().equals("catrefURL")) {
          String urlString = (String) e.getNewValue();
          if (debugEvents) System.out.println("***catrefURL= " + urlString);
          tree.setCatalog(urlString.trim());
        }
      }
    });

    // splitter
    split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, false, tree, htmlViewer);
    if (prefs != null)
      split.setDividerLocation(prefs.getInt(HDIVIDER, 400));

      //status label
    JPanel statusPanel = new JPanel(new BorderLayout());
    statusLabel = new JLabel("not connected");
    sourceText = new JLabel();
    statusPanel.add(statusLabel, BorderLayout.WEST);
    statusPanel.add(sourceText, BorderLayout.EAST);

    // button panel
    buttPanel = new JPanel();
    JButton openfileButton = new JButton("Open File");
    buttPanel.add(openfileButton, null);
    openfileButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent evt) {
        eventType = "File";
        try {
          tree.acceptSelected();
        } catch (Throwable t) {
          t.printStackTrace();
          JOptionPane.showMessageDialog(CatalogChooser.this, "ERROR "+t.getMessage());
        } finally {
          eventType = null;
        }
      }
    });

    JButton openCoordButton = new JButton("Open CoordSys");
    buttPanel.add(openCoordButton, null);
    openCoordButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent evt) {
        eventType = "CoordSys";
        try {
          tree.acceptSelected();
        } catch (Throwable t) {
          t.printStackTrace();
          JOptionPane.showMessageDialog(CatalogChooser.this, "ERROR "+t.getMessage());
        } finally {
          eventType = null;
        }
      }
    });

    JButton acceptButton = new JButton("Open dataset");
    buttPanel.add(acceptButton, null);
    acceptButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent evt) {
        eventType = "Dataset";
        try {
          tree.acceptSelected();
        } catch (Throwable t) {
          t.printStackTrace();
          JOptionPane.showMessageDialog(CatalogChooser.this, "ERROR "+t.getMessage());
        } finally {
          eventType = null;
        }
      }
    });

    // put it all together
    setLayout(new BorderLayout());
    if (showComboChooser)
      add(topPanel, BorderLayout.NORTH);
    add(split, BorderLayout.CENTER);

    if (showOpenButton) {
      JPanel botPanel = new JPanel(new BorderLayout());
      botPanel.add(buttPanel, BorderLayout.NORTH);
      botPanel.add(statusPanel, BorderLayout.SOUTH);
      add(botPanel, BorderLayout.SOUTH);
    }
  }

  private void makeSourceEditWindow() {
    TextGetPutPane sourceEditor = new TextGetPutPane(null);
    IndependentWindow sourceEditorWindow = new IndependentWindow( "Source", BAMutil.getImage("threddsIcon.png"), sourceEditor);
    sourceEditorWindow.setBounds(new Rectangle(50, 50, 725, 450));
    sourceEditorWindow.show();
  }

  /**
   * Save persistent state.
   */
  public void save() {
    if (catListBox != null) catListBox.save();

    if (prefs != null) {
      if (fileChooser != null)
        fileChooser.save();
      if (catgenFileChooser != null)
        catgenFileChooser.save();
      prefs.putInt(HDIVIDER, split.getDividerLocation());
    }
  }

  private void firePropertyChangeEvent(Dataset ds, String oldPropertyName) {
    String propertyName = (eventType != null) ? eventType : oldPropertyName;
    PropertyChangeEvent event = new PropertyChangeEvent(this, propertyName, null, ds);
    firePropertyChangeEvent( event);
  }

  /**
   * Fires a PropertyChangeEvent:
   * <ul><li>  propertyName = "Catalog", getNewValue() = catalog URL string
   * <li>  propertyName = "Dataset" or "File", getNewValue() = InvDataset chosen.
   * <li>  propertyName = "InvAccess" getNewValue() = InvAccess chosen.
   * <li>  propertyName = "catrefURL", getNewValue() = catref URL was chosen.
   * </ul>
   */
  private void firePropertyChangeEvent(PropertyChangeEvent event) {
    firePropertyChange(event.getPropertyName(), event.getOldValue(), event.getNewValue());
  }

  /**
   * Add this button to the button panel.
   * @param b button to add
   */
  public void addButton( JButton b) {
    buttPanel.add(b, null);
    buttPanel.revalidate();
  }

  // public void useDQCpopup( boolean use) { tree.useDQCpopup( use); }

  /** Whether to throw events only if dataset has an Access.
   *  @param accessOnly if true, throw events only if dataset has an Access
   */
  public void setAccessOnly( boolean accessOnly) { tree.setAccessOnly(accessOnly); }

  /** Whether to throw events if catref URL was chosen catref URL was chosen in HtmlViewer (default false).
   */
  public void setCatrefEvents( boolean catrefEvents) { this.catrefEvents = catrefEvents; }

  /** Whether to throw events if dataset URL was chosen in HtmlViewer (default true).
   */
  public void setDatasetEvents( boolean datasetEvents) { this.datasetEvents = datasetEvents; }

  /**
   * Set the factory to create catalogs.
   * If you do not set this, it will use the default factory.
   * @param factory : read XML with this factory
   *
  public void setCatalogFactory(InvCatalogFactory factory) { tree.setCatalogFactory(factory); } */

  /**
   * Use this to set the string value in the combo box
   * @param item
   */
  public void setSelectedItem( String item) {
    if (catListBox != null)
      catListBox.setSelectedItem( item);
  }

  /**
   * Set the currently selected InvDataset.
   * @param ds select this InvDataset, must be already in the tree.
   */
  public void setSelectedDataset(Dataset ds) {
    tree.setSelectedDataset( ds);
    showDatasetInfo(ds);
  }

  public DatasetNode getSelectedDataset() {
    return tree.getSelectedDataset();
  }

  /**
   * Get the current catalog being shown.
   * @return current catalog, or null.
   */
  public Catalog getCurrentCatalog() { return tree.getCatalog(); }

  /**
   * Get the TreeView component.
   * @return the TreeView component.
   */
  public CatalogTreeView getTreeView() { return tree; }

  /**
   * Get the current URL string. This may be the top catalog, or a catalogRef, depending on
   *  what was last selected. Used to implement the " showSource" debugging tool.
   * @return current URL string
   */
  public String getCurrentURL() { return currentURL; }
  private void setCurrentURL( String currentURL) {
    this.currentURL = currentURL;
    sourceText.setText( currentURL);
    statusLabel.setText("Connected...");
  }

  /**
   * Set the current catalog.
   */
  public void setCatalog(Catalog catalog) {
    tree.setCatalog( catalog);
  }

  /**
   * Set the current catalog with a string URL.
   * May be of form catalog#datasetId
   */
  public void setCatalog(String catalogURL) {
    tree.setCatalog(catalogURL.trim());
  }

  private void showDatasetInfo(Dataset ds) {
    if (ds == null) return;
    Formatter sbuff = new Formatter();
    DatasetHtmlWriter writer = new DatasetHtmlWriter();
    writer.writeHtmlDescription(sbuff, ds, true, false, datasetEvents, catrefEvents, true);
    if (showHTML) System.out.println("HTML=\n"+sbuff);
    htmlViewer.setContent( ds.getName(), sbuff.toString());
  }

 /** Wrap this in a JDialog component.
   *
   * @param parent      JFrame (application) or JApplet (applet) or null
   * @param title       dialog window title
   * @param modal     is modal
   */
  public JDialog makeDialog( RootPaneContainer parent, String title, boolean modal) {
    this.parent = parent;
    return new Dialog( parent, title, modal);
  }

  private class Dialog extends JDialog {

    private Dialog(RootPaneContainer parent, String title, boolean modal) {
      super(parent instanceof Frame ? (Frame) parent : null, title, modal);

      // L&F may change
      UIManager.addPropertyChangeListener( new PropertyChangeListener() {
        public void propertyChange( PropertyChangeEvent e) {
          if (e.getPropertyName().equals("lookAndFeel"))
            SwingUtilities.updateComponentTreeUI( CatalogChooser.Dialog.this);
        }
      });

      // add a dismiss button
      JButton dismissButton = new JButton("Dismiss");
      buttPanel.add(dismissButton, null);

      dismissButton.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent evt) {
          setVisible(false);
        }
      });

     // add it to contentPane
      Container cp = getContentPane();
      cp.setLayout(new BorderLayout());
      cp.add( CatalogChooser.this, BorderLayout.CENTER);
      pack();
    }
  }

}
