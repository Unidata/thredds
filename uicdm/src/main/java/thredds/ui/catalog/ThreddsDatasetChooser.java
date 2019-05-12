/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package thredds.ui.catalog;

import thredds.client.catalog.*;
import ucar.util.prefs.PreferencesExt;
import ucar.util.prefs.XMLStore;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.util.Formatter;

//import thredds.catalog.ui.tools.CatalogSearcher;

/**
 * A Swing widget for THREDDS clients that combines a CatalogChooser, and optionally a QueryChooser
 * PropertyChangeEvent events are thrown to notify you of various
 * user actions; see addPropertyChangeListener.
 * <p/>
 * You can use the ThreddsDatasetChooser:
 * <ol>
 * <li> add the components into your own JTabbedPanel.
 * <li> wrapped in a JDialog for popping up
 * <li> as a standalone application through its main() method
 * </ol>
 * Example:
 * <pre>
 * datasetChooser = new ThreddsDatasetChooser( prefs, tabbedPane);
 * datasetChooser.addPropertyChangeListener(  new java.beans.PropertyChangeListener() {
 * public void propertyChange( java.beans.PropertyChangeEvent e) {
 * if (e.getPropertyName().equals("Dataset")) {
 * thredds.catalog.InvDataset ds = (thredds.catalog.InvDataset) e.getNewValue();
 * setDataset( ds);
 * }
 * }
 * }
 * });
 * </pre>
 * <p/>
 * To use as popup dialog box:
 * <pre>
 * ThreddsDatasetChooser datasetChooser = new ThreddsDatasetChooser( prefs, null);
 * JDialog datasetChooserDialog = datasetChooser.makeDialog("Open THREDDS dataset", true);
 * datasetChooserDialog.show();
 * </pre>
 * <p/>
 * When using as a standalone application, the default behavior is to write the dataURLs of the
 * selections to standard out. Copy main() and make changes as needed.
 * <pre>
 * java -classpath clientUI.jar;... thredds.catalog.ui.ThreddsDatasetChooser
 * </pre>
 *
 * @author John Caron
 */

public class ThreddsDatasetChooser extends JPanel {
  private static final String FRAME_SIZE = "FrameSize";

  private CatalogChooser catalogChooser;
  private JTabbedPane tabbedPane;

  private boolean doResolve = false;  // shoul we resolve Resolver datasets?
  private boolean pipeOut;  // send results to standard out
  private boolean messageOut;  // send results to popup message
  private JFrame frame; // need for popup messages

  private boolean debugResolve = false;

  /**
   * Usual Constructor.
   * Create a CatalogChooser and a QueryChooser widget, add them to a JTabbedPane.
   *
   * @param prefs persistent storage, may be null
   * @param tabs  add panels to this JTabbedPane, may be null if you are using as Dialog.
   */
  public ThreddsDatasetChooser(PreferencesExt prefs, JTabbedPane tabs) {
    this(prefs, tabs, null, false, false, false);
  }

  /**
   * General Constructor.
   * Create a CatalogChooser and a QueryChooser widget, add them to a JTabbedPane.
   * Optionally write to stdout and/or pop up event messsages.
   *
   * @param prefs         persistent storage
   * @param tabs          add to this JTabbedPane
   * @param frame         best if non-null when messageOutP = true, otherwise null
   * @param pipeOutput    send selection message to System.out
   * @param messageOutput send selection to popup message
   */
  public ThreddsDatasetChooser(PreferencesExt prefs, JTabbedPane tabs, JFrame frame,
                               boolean pipeOutput, boolean messageOutput, boolean addDqc) {

    this.frame = frame;
    this.pipeOut = pipeOutput;
    this.messageOut = messageOutput;

    // create the catalog chooser
    PreferencesExt node = (prefs == null) ? null : (PreferencesExt) prefs.node("catChooser");
    catalogChooser = new CatalogChooser(node, true, true, true);
    catalogChooser.addPropertyChangeListener(new java.beans.PropertyChangeListener() {
      public void propertyChange(java.beans.PropertyChangeEvent e) {

        if (e.getPropertyName().equals("InvAccess")) {
          firePropertyChangeEvent(e);
          return;
        }

        // see if this dataset is really a qc
        if (e.getPropertyName().equals("Dataset") || e.getPropertyName().equals("CoordSys") || e.getPropertyName().equals("File")) {
          firePropertyChangeEvent(e);
        }
      }
    });

    // the overall UI
    tabbedPane = (tabs == null) ? new JTabbedPane(JTabbedPane.TOP) : tabs;
    tabbedPane.addTab("Catalog Chooser", catalogChooser);
    tabbedPane.setSelectedComponent(catalogChooser);

    setLayout(new BorderLayout());
    add(tabbedPane, BorderLayout.CENTER);
  }

  /**
   * If you want resolver datasets to be resolved (default false).
   * If true, may throw "Datasets" event.
   */
  public void setDoResolve(boolean doResolve) {
    this.doResolve = doResolve;
  }

  /**
   * Get the component CatalogChooser
   */
  public CatalogChooser getCatalogChooser() {
    return catalogChooser;
  }

  /**
   * save the state
   */
  public void save() {
    catalogChooser.save();
  }

  /**
   * Fires a PropertyChangeEvent:
   * <ul>
   * <li>  propertyName = "Dataset" or "File", getNewValue() = InvDataset chosen.
   * <li>  propertyName = "Datasets", getNewValue() = InvDataset[] chosen. This can only happen if
   * you have set doResolve = true, and the resolved dataset is a list of datasets.
   * <li>  propertyName = "InvAccess" getNewValue() = InvAccess chosen.
   * </ul>
   */
  private void firePropertyChangeEvent(PropertyChangeEvent event) {
    // System.out.println("firePropertyChangeEvent "+((InvDatasetImpl)ds).dump());
    if (pipeOut)
      pipeEvent(event);
    if (messageOut)
      messageEvent(event);

    firePropertyChange(event.getPropertyName(), event.getOldValue(), event.getNewValue());
  }

  private void messageEvent(java.beans.PropertyChangeEvent e) {
    Formatter buff = new Formatter();
    buff.format("Event propertyName = %s", e.getPropertyName());
    Object newValue = e.getNewValue();
    if (newValue != null)
      buff.format(", class = %s", newValue.getClass().getName());
    buff.format("%n");

    if (e.getPropertyName().equals("Dataset")) {
      showDatasetInfo(buff, (thredds.client.catalog.Dataset) e.getNewValue());

    } else if (e.getPropertyName().equals("Datasets")) {
      Object[] ds = (Object[]) e.getNewValue();
      buff.format(" element class = %s%n", ds[0].getClass().getName());

      for (Object d : ds) {
        if (d instanceof Dataset) {
          showDatasetInfo(buff, (Dataset) d);
        }
      }
    }

    JOptionPane.showMessageDialog(frame, buff);
  }

  private void pipeEvent(java.beans.PropertyChangeEvent e) {
    Formatter buff = new Formatter();

    if (e.getPropertyName().equals("Dataset")) {
      getAccessURLs(buff, (Dataset) e.getNewValue());

    } else if (e.getPropertyName().equals("Datasets")) {
      Object[] ds = (Object[]) e.getNewValue();
      for (Object d : ds) {
        if (d instanceof Dataset) {
          getAccessURLs(buff, (Dataset) d);
        }
      }
    }

    System.out.println(buff);
  }

  private void getAccessURLs(Formatter buff, Dataset ds) {
    for (Access ac : ds.getAccess())
      buff.format("%s %s %n", ac.getStandardUrlName(), ac.getService().getType());

  }

  private void showDatasetInfo(Formatter buff, thredds.client.catalog.Dataset ds) {
    buff.format(" Dataset = %s", ds.getName());
    buff.format(", dataType = %s%n", ds.getFeatureType());
    for (Access ac : ds.getAccess()) {
      buff.format("  service = %s, url = %s%n", ac.getService().getType(), ac.getStandardUrlName());
      //System.out.println("  url = "+ac.getStandardUrlName());
    }
  }

  /**
   * Wrap this in a JDialog component.
   *
   * @param parent put dialog on top of this, may be null
   * @param title  dialog window title
   * @param modal  is modal
   */
  public JDialog makeDialog(JFrame parent, String title, boolean modal) {
    return new Dialog(frame, title, modal);
  }

  private class Dialog extends JDialog {

    private Dialog(JFrame frame, String title, boolean modal) {
      super(frame, title, modal);

      // L&F may change
      UIManager.addPropertyChangeListener(new PropertyChangeListener() {
        public void propertyChange(PropertyChangeEvent e) {
          if (e.getPropertyName().equals("lookAndFeel"))
            SwingUtilities.updateComponentTreeUI(ThreddsDatasetChooser.Dialog.this);
        }
      });

      // add a dismiss button
      JButton dismissButton = new JButton("Dismiss");
      //buttPanel.add(dismissButton, null);

      dismissButton.addActionListener(e -> setVisible(false));

      // add it to contentPane
      Container cp = getContentPane();
      cp.setLayout(new BorderLayout());
      cp.add(ThreddsDatasetChooser.this, BorderLayout.CENTER);
      pack();
    }
  }

  /**
   * Standalone application.
   *
   * @param args recognized values:
   *             <ul> <li> -usePopup to popup messages </ul>
   */
  public static void main(String[] args) {
    boolean usePopup = false;

    for (String arg : args) {
      if (arg.equals("-usePopup")) {
        usePopup = true;
      }
    }

    try {
      store = XMLStore.createFromFile("ThreddsDatasetChooser", null);
      p = store.getPreferences();
    } catch (IOException e) {
      System.out.println("XMLStore Creation failed " + e);
    }

    // put it together in a JFrame
    final JFrame frame = new JFrame("Thredds Dataset Chooser");
    frame.addWindowListener(new WindowAdapter() {
      public void windowClosing(WindowEvent e) {
        chooser.save();
        Rectangle bounds = frame.getBounds();
        p.putBeanObject(FRAME_SIZE, bounds);
        try {
          store.save();
        } catch (IOException ioe) {
          ioe.printStackTrace();
        }

        System.exit(0);
      }
    });

    chooser = new ThreddsDatasetChooser(p, null, frame, true, usePopup, false);
    chooser.setDoResolve(true);

    //
    frame.getContentPane().add(chooser);
    Rectangle bounds = (Rectangle) p.getBean(FRAME_SIZE, new Rectangle(50, 50, 800, 450));
    frame.setBounds(bounds);

    frame.pack();
    frame.setBounds(bounds);
    frame.setVisible(true);
  }

  private static ThreddsDatasetChooser chooser;
  private static PreferencesExt p;
  private static XMLStore store;
}
