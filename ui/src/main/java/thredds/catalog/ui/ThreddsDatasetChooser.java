// $Id: ThreddsDatasetChooser.java 50 2006-07-12 16:30:06Z caron $
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
 * A Swing widget for THREDDS clients that combines a CatalogChooser, a
 *    QueryChooser, and a SearchChooser widget.
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
 * @version $Id: ThreddsDatasetChooser.java 50 2006-07-12 16:30:06Z caron $
 */

public class ThreddsDatasetChooser extends JPanel {
  private final static String FRAME_SIZE = "FrameSize";
  private ucar.util.prefs.PreferencesExt prefs;

  private EventListenerList listenerList = new EventListenerList();

  private CatalogChooser catalogChooser;
  private QueryChooser queryChooser;
  private CatalogSearcher searchChooser;
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
    this( prefs, tabs, null, false, false);
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
    boolean pipeOutput, boolean messageOutput) {

    this.prefs = prefs;
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
          if (qcAccess.getService().getServiceType() == ServiceType.QC) { // LOOK && (ds.getDataType() != DataType.STATION)) {
            queryChooser.setDataset( qcAccess.getDataset());
            tabbedPane.setSelectedComponent(queryChooser);
            return;
          }

          firePropertyChangeEvent( e);
          return;
        }

        // see if this dataset is really a qc
        if (e.getPropertyName().equals("Dataset") || e.getPropertyName().equals("File")) {
          InvDataset ds = (thredds.catalog.InvDataset) e.getNewValue();
          InvAccess qcAccess = ds.getAccess( ServiceType.QC);
          if ((qcAccess != null)) { // LOOK && (ds.getDataType() != DataType.STATION)) {

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

        // DQC
    node = (prefs == null) ? null : (PreferencesExt) prefs.node("dqc");
    queryChooser = new QueryChooser(node, true);
    queryChooser.addPropertyChangeListener( new PropertyChangeListener() {
      public void propertyChange( PropertyChangeEvent e) {
        firePropertyChangeEvent( e);
      }
    });

    // panel to search catalog
    node = (prefs == null) ? null : (PreferencesExt) prefs.node("search");
    searchChooser = new CatalogSearcher(node);

    // the overall UI
    tabbedPane = (tabs == null) ? new JTabbedPane(JTabbedPane.TOP) : tabs;

    tabbedPane.addTab("Catalog Chooser", catalogChooser);
    tabbedPane.addTab("DQC Chooser", queryChooser);
    if (ucar.util.prefs.ui.Debug.isSet("System/showTools")) // not ready for general use
      tabbedPane.addTab("Search", searchChooser);
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

  /** Get the component CatalogSearcher */
  public CatalogSearcher getSearchChooser() { return searchChooser; }

  /** save the state */
  public void save() {
    catalogChooser.save();
    queryChooser.save();
    searchChooser.save();
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
    StringBuffer buff = new StringBuffer();
    buff.append("Event propertyName = "+e.getPropertyName());
    Object newValue = e.getNewValue();
    if (newValue != null)
      buff.append(", class = "+newValue.getClass().getName());
    buff.append("\n");

    if (e.getPropertyName().equals("Dataset")) {
      showDatasetInfo(buff, (thredds.catalog.InvDataset) e.getNewValue());

    } else if (e.getPropertyName().equals("Datasets")) {
      Object[] ds = (Object[]) e.getNewValue();
      buff.append(" element class = "+ds[0].getClass().getName()+"\n");

      for (int i=0; i<ds.length; i++)
        if (ds[i] instanceof InvDataset)
          showDatasetInfo(buff, (InvDataset) ds[i]);
    }

    try { JOptionPane.showMessageDialog(frame, buff); }
    catch (HeadlessException he) { }
  }

  private void pipeEvent( java.beans.PropertyChangeEvent e) {
    StringBuffer buff = new StringBuffer();

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

  private void getAccessURLs( StringBuffer buff, thredds.catalog.InvDataset ds) {
    Iterator iter = ds.getAccess().iterator();
    while (iter.hasNext()) {
      thredds.catalog.InvAccess ac = (thredds.catalog.InvAccess) iter.next();
      buff.append(ac.getStandardUrlName()+" "+ac.getService().getServiceType()+"\n");
    }
  }

  private void showDatasetInfo( StringBuffer buff, thredds.catalog.InvDataset ds) {
    buff.append(" Dataset = "+ds.getName());
    buff.append(", dataType = "+ds.getDataType()+"\n");
    Iterator iter = ds.getAccess().iterator();
    while (iter.hasNext()) {
      thredds.catalog.InvAccess ac = (thredds.catalog.InvAccess) iter.next();
      buff.append("  service = "+ac.getService().getServiceType()+", url = "+ac.getStandardUrlName()+"\n");
      System.out.println("  url = "+ac.getStandardUrlName());
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

  // dummy
  private class CatalogSearcher extends JPanel {
    CatalogSearcher( PreferencesExt prefs) { }
    void save() { }
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

    chooser = new ThreddsDatasetChooser(p, null, frame, true, usePopup);
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

/* Change History:
   $Log: ThreddsDatasetChooser.java,v $
   Revision 1.21  2005/08/08 19:38:59  caron
   minor

   Revision 1.20  2005/07/27 23:29:13  caron
   minor

   Revision 1.19  2005/06/23 20:02:55  caron
   add "View File" button to thredds dataset chooser

   Revision 1.18  2005/06/23 19:18:50  caron
   no message

   Revision 1.17  2005/04/20 00:05:38  caron
   *** empty log message ***

   Revision 1.16  2004/12/14 15:41:01  caron
   *** empty log message ***

   Revision 1.15  2004/11/16 23:35:37  caron
   no message

   Revision 1.14  2004/11/04 20:16:43  caron
   no message

   Revision 1.13  2004/09/30 00:33:37  caron
   *** empty log message ***

   Revision 1.12  2004/09/24 03:26:31  caron
   merge nj22

   Revision 1.11  2004/06/19 01:16:37  caron
   hide search tab for now

   Revision 1.10  2004/06/09 00:27:29  caron
   version 2.0a release; cleanup javadoc

   Revision 1.9  2004/05/11 23:30:33  caron
   release 2.0a

   Revision 1.8  2004/03/11 23:35:20  caron
   minor bugs

   Revision 1.7  2004/03/05 23:35:48  caron
   rel 1.3.1 javadoc

   Revision 1.6  2004/03/05 17:21:51  caron
   1.3.1 release

   Revision 1.5  2004/02/20 00:49:54  caron
   1.3 changes

   Revision 1.4  2003/12/04 22:27:46  caron
   *** empty log message ***

   Revision 1.3  2003/05/29 22:59:50  john
   refactor choosers into toolkit framework

   Revision 1.2  2003/03/17 20:09:35  john
   improve catalog chooser, use ucar.unidata.geoloc

   Revision 1.1  2003/01/31 22:06:16  john
   ThreddsDatasetChooser standalone

   Revision 1.4  2003/01/13 19:54:54  john
   new prefs usage

   Revision 1.3  2002/12/19 23:02:18  caron
   latest adde mods

   Revision 1.2  2002/12/13 00:36:25  caron
   pass 2 of thredds build environ

   Revision 1.1.1.1  2002/11/23 17:49:46  caron
   thredds reorg

   Revision 1.6  2002/10/18 18:21:03  caron
   thredds server

   Revision 1.5  2002/07/01 23:35:44  caron
   release 0.6 alpha

   Revision 1.4  2002/04/30 22:43:09  caron
   allow 1.3 or 1.4

   Revision 1.3  2002/04/29 22:44:06  caron
   Propert name = Dataset, change button layout

   Revision 1.2  2002/03/09 01:47:00  caron
   seperate JDialog

   Revision 1.1.1.1  2002/02/26 17:24:41  caron
   import sources

   Revision 1.2  2001/09/14 15:47:14  caron
   checkin catalog 0.4

   Revision 1.1  2001/08/29 01:11:25  caron
   RemoteDatasetChooser component added

 */