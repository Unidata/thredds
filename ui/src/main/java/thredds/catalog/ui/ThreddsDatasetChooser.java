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

package thredds.catalog.ui;

import thredds.catalog.*;
import thredds.catalog.ui.query.QueryChooser;
//import thredds.catalog.ui.tools.CatalogSearcher;
import ucar.util.prefs.*;

import java.awt.*;
import java.awt.event.*;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;
import java.io.*;
import java.util.*;
import java.util.List;

import javax.swing.*;
import javax.swing.event.EventListenerList;

/**
 * A Swing widget for THREDDS clients that combines a CatalogChooser, and optionally a QueryChooser
 *  PropertyChangeEvent events are thrown to notify you of various
 *  user actions; see addPropertyChangeListener.
 * <p>
 * You can use the ThreddsDatasetChooser:
 * <ol>
 * <li> add the components into your own JTabbedPanel.
 * <li> wrapped in a JDialog for popping up
 * <li> as a standalone application through its main() method
 * </ol>
 *  Example:
 * <pre>
    datasetChooser = new ThreddsDatasetChooser( prefs, tabbedPane);
    datasetChooser.addPropertyChangeListener(  new java.beans.PropertyChangeListener() {
      public void propertyChange( java.beans.PropertyChangeEvent e) {
        if (e.getPropertyName().equals("Dataset")) {
          thredds.catalog.InvDataset ds = (thredds.catalog.InvDataset) e.getNewValue();
          setDataset( ds);
         }
        }
      }
    });
   </pre>

  To use as popup dialog box:
  <pre>
   ThreddsDatasetChooser datasetChooser = new ThreddsDatasetChooser( prefs, null);
   JDialog datasetChooserDialog = datasetChooser.makeDialog("Open THREDDS dataset", true);
   datasetChooserDialog.show();
  </pre>
 *
 * When using as a standalone application, the default behavior is to write the dataURLs of the
 * selections to standard out. Copy main() and make changes as needed.
  <pre>
   java -classpath clientUI.jar;... thredds.catalog.ui.ThreddsDatasetChooser
  </pre>
 *
 *
 * @author John Caron
 */

public class ThreddsDatasetChooser extends JPanel {
  private final static String FRAME_SIZE = "FrameSize";

  private EventListenerList listenerList = new EventListenerList();

  private CatalogChooser catalogChooser;
  private QueryChooser queryChooser;
  private JTabbedPane tabbedPane;

  private boolean doResolve = false;  // shoul we resolve Resolver datasets?
  private boolean pipeOut = true;  // send results to standard out
  private boolean messageOut = false;  // send results to popup message
  private JFrame frame; // need for popup messages

  private boolean debugResolve = false;

  /**
   * Usual Constructor.
   * Create a CatalogChooser and a QueryChooser widget, add them to a JTabbedPane.
   *
   * @param prefs persistent storage, may be null
   * @param tabs add panels to this JTabbedPane, may be null if you are using as Dialog.
   */
  public ThreddsDatasetChooser(PreferencesExt prefs, JTabbedPane tabs) {
    this( prefs, tabs, null, false, false, false);
  }

  /**
   * General Constructor.
   * Create a CatalogChooser and a QueryChooser widget, add them to a JTabbedPane.
   * Optionally write to stdout and/or pop up event messsages.
   *
   * @param prefs persistent storage
   * @param tabs add to this JTabbedPane
   * @param frame best if non-null when messageOutP = true, otherwise null
   * @param pipeOutput send selection message to System.out
   * @param messageOutput send selection to popup message
   */
  public ThreddsDatasetChooser(ucar.util.prefs.PreferencesExt prefs, JTabbedPane tabs, JFrame frame,
    boolean pipeOutput, boolean messageOutput, boolean addDqc) {

    this.frame = frame;
    this.pipeOut = pipeOutput;
    this.messageOut = messageOutput;

    // create the catalog chooser
    PreferencesExt node = (prefs == null) ? null : (PreferencesExt) prefs.node("catChooser");
    catalogChooser = new CatalogChooser(node, true, true, true);
    catalogChooser.addPropertyChangeListener(  new java.beans.PropertyChangeListener() {
      public void propertyChange( java.beans.PropertyChangeEvent e) {

        if (e.getPropertyName().equals("InvAccess")) {
          InvAccess qcAccess = (InvAccess) e.getNewValue();
          if (queryChooser != null && qcAccess.getService().getServiceType() == ServiceType.QC) { // LOOK && (ds.getDataType() != DataType.STATION)) {
            queryChooser.setDataset( qcAccess.getDataset());
            tabbedPane.setSelectedComponent(queryChooser);
            return;
          }

          firePropertyChangeEvent( e);
          return;
        }

        // see if this dataset is really a qc
        if (e.getPropertyName().equals("Dataset") || e.getPropertyName().equals("CoordSys") || e.getPropertyName().equals("File")) {
          InvDataset ds = (thredds.catalog.InvDataset) e.getNewValue();
          InvAccess qcAccess = ds.getAccess( ServiceType.QC);
          if (queryChooser != null && (qcAccess != null)) { // LOOK && (ds.getDataType() != DataType.STATION)) {

            // non station data DQC
            queryChooser.setDataset( ds);
            tabbedPane.setSelectedComponent(queryChooser);
            return;
          }

          // do we need to resolve it? LOOK what about QC events?
          qcAccess = ds.getAccess( ServiceType.RESOLVER);
          if ((qcAccess != null) && doResolve) {
            resolve( ds);
            return;
          }

        // otherwise send out the info
        firePropertyChangeEvent( e);
        }
      }
    });

    // the overall UI
    tabbedPane = (tabs == null) ? new JTabbedPane(JTabbedPane.TOP) : tabs;
    tabbedPane.addTab("Catalog Chooser", catalogChooser);

     if (addDqc) {
      node = (prefs == null) ? null : (PreferencesExt) prefs.node("dqc");
      queryChooser = new QueryChooser(node, true);
      queryChooser.addPropertyChangeListener( new PropertyChangeListener() {
        public void propertyChange( PropertyChangeEvent e) {
          firePropertyChangeEvent( e);
        }
      });
       tabbedPane.addTab("DQC Chooser", queryChooser);
    }

    tabbedPane.setSelectedComponent(catalogChooser);

    setLayout( new BorderLayout());
    add(tabbedPane, BorderLayout.CENTER);
  }

  /**
   * Set a dataset filter to be used on all catalogs.
   * To turn off, set to null.
   * @param filter DatasetFilter or null
   */
  public void setDatasetFilter( DatasetFilter filter) {
    catalogChooser.setDatasetFilter( filter);
  }

  /**
   * If you want resolver datasets to be resolved (default false).
   * If true, may throw "Datasets" event.
   * @param doResolve
   */
  public void setDoResolve( boolean doResolve) {
    this.doResolve = doResolve;
  }

  /** Get the component QueryChooser */
  public QueryChooser getQueryChooser() { return queryChooser; }

  /** Get the component CatalogChooser */
  public CatalogChooser getCatalogChooser() { return catalogChooser; }

  /** save the state */
  public void save() {
    catalogChooser.save();
    if (queryChooser != null) queryChooser.save();
  }

  private void firePropertyChangeEvent(PropertyChangeEvent event) {
    // System.out.println("firePropertyChangeEvent "+((InvDatasetImpl)ds).dump());
    if (pipeOut)
      pipeEvent( event);
    if (messageOut)
      messageEvent( event);

    // Process the listeners last to first
    Object[] listeners = listenerList.getListenerList();
    for (int i = listeners.length-2; i>=0; i-=2) {
      if (listeners[i] == PropertyChangeListener.class) {
        ((PropertyChangeListener)listeners[i+1]).propertyChange(event);
      }
    }
  }

  /**
   * Add a PropertyChangeEvent Listener. Throws a PropertyChangeEvent:
   * <ul>
   * <li>  propertyName = "Dataset" or "File", getNewValue() = InvDataset chosen.
   * <li>  propertyName = "Datasets", getNewValue() = InvDataset[] chosen. This can only happen if
   *  you have set doResolve = true, and the resolved dataset is a list of datasets.
   * <li>  propertyName = "InvAccess" getNewValue() = InvAccess chosen.
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

  private void messageEvent( java.beans.PropertyChangeEvent e) {
    Formatter buff = new Formatter();
    buff.format("Event propertyName = %s", e.getPropertyName());
    Object newValue = e.getNewValue();
    if (newValue != null)
      buff.format(", class = %s", newValue.getClass().getName());
    buff.format("%n");

    if (e.getPropertyName().equals("Dataset")) {
      showDatasetInfo(buff, (thredds.catalog.InvDataset) e.getNewValue());

    } else if (e.getPropertyName().equals("Datasets")) {
      Object[] ds = (Object[]) e.getNewValue();
      buff.format(" element class = "+ds[0].getClass().getName()+"\n");

      for (int i=0; i<ds.length; i++)
        if (ds[i] instanceof InvDataset)
          showDatasetInfo(buff, (InvDataset) ds[i]);
    }

    try { JOptionPane.showMessageDialog(frame, buff); }
    catch (HeadlessException he) { }
  }

  private void pipeEvent( java.beans.PropertyChangeEvent e) {
    Formatter buff = new Formatter();

    if (e.getPropertyName().equals("Dataset")) {
      getAccessURLs(buff, (thredds.catalog.InvDataset) e.getNewValue());

    } else if (e.getPropertyName().equals("Datasets")) {
      Object[] ds = (Object[]) e.getNewValue();
      for (int i=0; i<ds.length; i++)
        if (ds[i] instanceof InvDataset)
          getAccessURLs(buff, (InvDataset) ds[i]);
    }

    System.out.println( buff);
  }

  private void getAccessURLs( Formatter buff, thredds.catalog.InvDataset ds) {
    for (thredds.catalog.InvAccess ac : ds.getAccess())
      buff.format("%s %s %n", ac.getStandardUrlName(), ac.getService().getServiceType());

  }

  private void showDatasetInfo( Formatter buff, thredds.catalog.InvDataset ds) {
    buff.format(" Dataset = %s", ds.getName());
    buff.format(", dataType = %s", ds.getDataType()+"\n");
    for (thredds.catalog.InvAccess ac : ds.getAccess()) {
      buff.format("  service = %s, url = %s%n", ac.getService().getServiceType(), ac.getStandardUrlName());
      //System.out.println("  url = "+ac.getStandardUrlName());
    }
  }

  private void resolve(thredds.catalog.InvDataset ds) {
    InvAccess resolverAccess;
    if (null != (resolverAccess = ds.getAccess( ServiceType.RESOLVER))) {
      String urlName = resolverAccess.getStandardUrlName();
      if (debugResolve) System.out.println(" resolve="+urlName);
      try {
        InvCatalogFactory factory = InvCatalogFactory.getDefaultFactory( true);
        InvCatalog catalog = factory.readXML( urlName); //should be asynch ?
        if (catalog == null) return;
        StringBuilder buff = new StringBuilder();
        if (!catalog.check( buff)) {
          javax.swing.JOptionPane.showMessageDialog(this, "Invalid catalog <"+ urlName+">\n"+
            buff.toString());
          if (debugResolve) System.out.println("Invalid catalog <"+ urlName+">\n"+buff.toString());
          return;
        }
        InvDataset top = catalog.getDataset();
        if (top.hasAccess())
          firePropertyChangeEvent(new PropertyChangeEvent(this, "Dataset", null, top));
        else {
          List<InvDataset> dsets = top.getDatasets();
          InvDataset[] dsa = (InvDataset[]) dsets.toArray( new InvDataset[dsets.size()] );
          firePropertyChangeEvent(new PropertyChangeEvent(this, "Datasets", null, dsa));
        }
        return;

      } catch (Exception e) {
        e.printStackTrace();
        return;
      }
    }
  }

  /** Wrap this in a JDialog component.
   *
   * @param parent      put dialog on top of this, may be null
   * @param title       dialog window title
   * @param modal     is modal
   */
  public JDialog makeDialog( JFrame parent, String title, boolean modal) {
    return new Dialog( frame, title, modal);
  }

  private class Dialog extends JDialog {

    private Dialog( JFrame frame, String title, boolean modal) {
      super( frame, title, modal);

      // L&F may change
      UIManager.addPropertyChangeListener( new PropertyChangeListener() {
        public void propertyChange( PropertyChangeEvent e) {
          if (e.getPropertyName().equals("lookAndFeel"))
            SwingUtilities.updateComponentTreeUI( ThreddsDatasetChooser.Dialog.this);
        }
      });

      // add a dismiss button
      JButton dismissButton = new JButton("Dismiss");
      //buttPanel.add(dismissButton, null);

      dismissButton.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent evt) {
          setVisible(false);
        }
      });

     // add it to contentPane
      Container cp = getContentPane();
      cp.setLayout(new BorderLayout());
      cp.add( ThreddsDatasetChooser.this, BorderLayout.CENTER);
      pack();
    }
  }

  /**
   * Standalone application.
   * @param args recognized values:
   *     <ul> <li> -usePopup to popup messages </ul>
   */
  public static void main(String args[]) {
    boolean usePopup = false;

    for (int i=0; i<args.length; i++) {
      if (args[i].equals("-usePopup"))
        usePopup = true;
    }

    try {
      store = ucar.util.prefs.XMLStore.createFromFile("ThreddsDatasetChooser", null);
      p = store.getPreferences();
    } catch (IOException e) {
      System.out.println("XMLStore Creation failed "+e);
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
        } catch (IOException ioe) { ioe.printStackTrace(); }

        System.exit(0);
      }
    });

    chooser = new ThreddsDatasetChooser(p, null, frame, true, usePopup, false);
    chooser.setDoResolve( true);

    //
    frame.getContentPane().add(chooser);
    Rectangle bounds = (Rectangle) p.getBean(FRAME_SIZE, new Rectangle(50, 50, 800, 450));
    frame.setBounds( bounds);

    frame.pack();
    frame.setBounds( bounds);
    frame.setVisible(true);
  }
  private static ThreddsDatasetChooser chooser;
  private static PreferencesExt p;
  private static XMLStore store;
}