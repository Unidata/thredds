// $Id: CatalogTreeView.java 50 2006-07-12 16:30:06Z caron $
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

import java.awt.*;
import java.awt.event.*;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;
import java.util.*;

import javax.swing.*;
import javax.swing.event.EventListenerList;
import javax.swing.event.TreeWillExpandListener;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.tree.*;

import ucar.nc2.constants.FeatureType;

/**
 * A Swing widget for THREDDS clients to display catalogs in a JTree, and allows
 * the user to select a dataset.
 * When a new catalog is read, or a dataset is selected, a java.beans.PropertyChangeEvent is
 * thrown, see addPropertyChangeListener.
 * <p>
 * Example:
 * <pre>
 *  CatalogTreeView  tree = new CatalogTreeView();
    tree.addPropertyChangeListener(  new java.beans.PropertyChangeListener() {
      public void propertyChange( java.beans.PropertyChangeEvent e) {
        if (e.getPropertyName().equals("Selection")) {
          ...
        } else if (e.getPropertyName().equals("Dataset")) {
          ...
        } else if (e.getPropertyName().equals("Catalog")) {
          ...
        }
      }
    });
 </pre>
 *
 *
 * Handles Catalog References internally. Catalogs are read in a background thread, which can be cancelled by the user.
 * <p> You probably want to use CatalogChooser or ThreddsDatasetChooser for more complete
 *  functionality.
 * @see CatalogChooser
 * @see ThreddsDatasetChooser
 *
 * @author John Caron
 * @version $Id: CatalogTreeView.java 50 2006-07-12 16:30:06Z caron $
 */

public class CatalogTreeView extends JPanel implements CatalogSetCallback {
  private boolean validate = true;
  private InvCatalogImpl catalog;

  private EventListenerList listenerList = new EventListenerList();
  private boolean eventsOK = true;

    // state
  private DatasetFilter filter = null;
  private boolean accessOnly = true;
  private String catalogURL = "";
  private boolean openCatalogReferences = true;
  private boolean openDatasetScans = true;

    // ui
  private JTree tree;
  private InvCatalogTreeModel model;


    /* popup DQC
  private boolean useDQC = true;
  private QueryChooser queryChooser;
  private JDialog queryChooserDialog = null; */

  private boolean debugRef = false;
  private boolean debugTree = false;

  /**
   * Constructor.
   */
  public CatalogTreeView() {
    // the catalog tree
    tree = new JTree(new DefaultTreeModel(new DefaultMutableTreeNode(null, false)));
    tree.setCellRenderer(new MyTreeCellRenderer());

    tree.putClientProperty("JTree.lineStyle", "Angled");
    tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
    tree.setToggleClickCount(1);

    tree.addMouseListener( new MouseAdapter() {
      public void mousePressed(MouseEvent e) {
        if (!SwingUtilities.isLeftMouseButton(e)) return; // left button only

        int selRow = tree.getRowForLocation(e.getX(), e.getY());
        if (selRow != -1) {
          checkForCatref();
          InvCatalogTreeNode node = (InvCatalogTreeNode) tree.getLastSelectedPathComponent();
          if (node != null)
            firePropertyChangeEvent(new PropertyChangeEvent(this, "Selection", null, node.ds));
        }

        if ((selRow != -1) && (e.getClickCount() == 2)) {
          acceptSelected();
        }
      }
    });

    tree.addTreeWillExpandListener( new TreeWillExpandListener() {
      public void treeWillCollapse(TreeExpansionEvent evt) { }
      public void treeWillExpand(TreeExpansionEvent evt) {
        InvCatalogTreeNode node = (InvCatalogTreeNode) evt.getPath().getLastPathComponent();
        if (node.ds instanceof InvCatalogRef) {
          InvCatalogRef catref = (InvCatalogRef) node.ds;
          boolean isDatasetScan = catref.findProperty("DatasetScan") != null;
          if (!catref.isRead()) {
            if (openCatalogReferences && (openDatasetScans || !isDatasetScan))
              node.readCatref();
          }
        }
      }
    });

    ToolTipManager.sharedInstance().registerComponent(tree);

   // layout
    setLayout(new BorderLayout());
    add(new JScrollPane(tree), BorderLayout.CENTER);
  }

  /** Get the underlying JTree */
  public JTree getJTree() { return tree; }


  /**
   * Set a DatasetFilter to filter any catalog read by this widget.
   * @param filter DatasetFilter or null to turn off filtering.
   */
  public void setDatasetFilter( DatasetFilter filter) {
    this.filter = filter;
  }

  /**
   * Set whether catalog references are opened. default is true.
   * @param openCatalogReferences
   */
  public void setOpenCatalogReferences( boolean openCatalogReferences) {
    this.openCatalogReferences = openCatalogReferences;
  }

  /**
   * Set whether catalog references from dataset scans are opened. default is true.
   * @param openDatasetScans
   */
  public void setOpenDatasetScans( boolean openDatasetScans) {
    this.openDatasetScans = openDatasetScans;
  }

  /**
   * Add a PropertyChangeEvent Listener. Throws a PropertyChangeEvent:
   * <ul><li>  when a new catalog is read and displayed:
   *     propertyName = "Catalog", getNewValue() = catalog URL string
   * <li>  when a node is selected:
   *     propertyName = "Selection", getNewValue() = InvDataset chosen.
   * <li>  when a node is double-clicked:
   *     propertyName = "Dataset", getNewValue() = InvDataset chosen.
   * <li>  when a TreeNode is added:
   *     propertyName = "TreeNode", getNewValue() = InvDataset added.
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

  private void firePropertyChangeEvent(InvDataset ds) {
    PropertyChangeEvent event = new PropertyChangeEvent(this, "Dataset", null, ds);
    firePropertyChangeEvent( event);
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

  /** Whether to detect that the dataset is a DQC, and popup a QueryChooser widget.
   *  @param use if true, popup a QueryChooser widget, otherwise just return the dataset.
   */
  //public void useDQCpopup( boolean use) { this.useDQC = use; }

  /** Whether to throw events only if dataset has an Access.
   *  @param accessOnly if true, throw events only if dataset has an Access
   */
  public void setAccessOnly( boolean accessOnly) { this.accessOnly = accessOnly; }

  /** Get the current catalog. */
  public InvCatalogImpl getCatalog() { return catalog; }

  /** Get the URL of the current catalog. */
  public String getCatalogURL() { return catalogURL; }
  private void setCatalogURL( String catalogURL) {
    this.catalogURL = catalogURL;
  }

  /**
   * Get the currently selected InvDataset.
   * @return selected InvDataset, or null if none.
   */
  public InvDataset getSelectedDataset() {
    InvCatalogTreeNode tnode = getSelectedNode();
    return tnode == null ? null : tnode.ds;
  }

  private InvCatalogTreeNode getSelectedNode() {
    Object node = tree.getLastSelectedPathComponent();
    if (node == null) return null;
    if ( !(node instanceof InvCatalogTreeNode)) return null;
    InvCatalogTreeNode tnode = (InvCatalogTreeNode) node;
    return tnode;
  }

  /**
   * Set the currently selected InvDataset.
   * @param ds select this InvDataset, must be already in the tree.
   * LOOK does this work ?? doesnt throw event
   */
  public void setSelectedDataset(InvDatasetImpl ds) {
    if (ds == null) return;
    TreePath path = makePath(ds);
    tree.setSelectionPath( path);
    tree.scrollPathToVisible( path);
  }

  /**
   * Create the TreePath corresponding to the InvDataset.
   * @param ds the InvDataset, must be already in the tree.
   * @return the corresponding TreePath.
   */
  TreePath makePath(InvDatasetImpl ds) {
    TreeNode node = (TreeNode) ds.getUserProperty("TreeNode");
    return makeTreePath( node);
  }

  /**
   * Create the TreePath corresponding to the given TreeNode.
   * @param node the TreeNode; already in the Tree.
   * @return the corresponding TreePath.
   */
  TreePath makeTreePath(TreeNode node) {
    ArrayList path = new ArrayList();
    path.add( node);
    TreeNode parent = node.getParent();
    while (parent != null) {
      path.add(0, parent);
      parent = parent.getParent();
    }

    Object[] paths = path.toArray();
    return new TreePath(paths);
  }

  /**
   * Open all nodes of the tree.
   * @param includeCatref open catrefs?
   */
  public void openAll( boolean includeCatref) {
    if (catalog == null) return;
    open( (InvCatalogTreeNode) model.getRoot(), includeCatref);
    tree.repaint();
  }

  private void open( InvCatalogTreeNode node, boolean includeCatref) {
    if (node == null) return;
    node.makeChildren( includeCatref);
    tree.expandPath(makeTreePath(node));

    Enumeration e = node.children();
    while (e.hasMoreElements()) {
      open( (InvCatalogTreeNode) e.nextElement(), includeCatref);
    }
  }

  void checkForCatref() {
    InvDataset ds = getSelectedDataset();
    if (ds == null)
      return;

    if ( (ds instanceof InvCatalogRef)) {
      InvCatalogRef catref = (InvCatalogRef) ds;
      boolean isDatasetScan = catref.findProperty("DatasetScan") != null;
      if (!catref.isRead()) {
        if (openCatalogReferences && (openDatasetScans || !isDatasetScan)) {
          InvCatalogTreeNode tnode = getSelectedNode();
          tnode.readCatref();
        }
      }
    }
  }

  void acceptSelected() {
    InvDataset ds = getSelectedDataset();
    if (ds == null) return;
    if (accessOnly && !ds.hasAccess()) return;

    /* see if this dataset is really a qc
    InvAccess qcAccess;
    if (useDQC && (null != (qcAccess = ds.getAccess( ServiceType.QC)))) {
      try {
        queryChooser = new QueryChooser(new ucar.util.prefs.PreferencesExt(null,"")); // fake prefs
        if (queryChooser.setDataset( ds))
          queryChooserDialog = queryChooser.makeDialog(null, "Dataset Query", true);
        else
          return;
      } catch (Exception e) {
        e.printStackTrace();
        return;
      }

      queryChooser.addPropertyChangeListener( new PropertyChangeListener() {
        public void propertyChange( java.beans.PropertyChangeEvent e) {
          firePropertyChangeEvent( e);
          queryChooserDialog.setVisible(false);
        }
      });

      queryChooserDialog.show();
      return;
    } */

      /* convert old format
    if (ds instanceof thredds.catalog.ver4.InvCatalog.Dataset) {
      InvDatasetImpl newDs = new InvDatasetImpl( ds.getName(), ds.getPath(), ds.getDataType(),
        ds.getServer().getBase(), ds.getServer().getServerType());
      ds = newDs;
    } */

    //setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
    firePropertyChangeEvent( ds);
    //setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
  }

  /**
   * Set the InvCatalog to display.
   * The catalog is read asynchronously and displayed if successfully read.
   * You must use a PropertyChangeEventListener to be notified if successful.
   *
   * @param catalogName The URL of the InvCatalog.
   */
  public void setCatalog(String catalogName) {
    getCatalogFactory().readXMLasynch( catalogName, this);
  }

  // reuse factory, but just throw it away if theres a thread conflict
  private CatalogFactoryCancellable factory = null;
  private CatalogFactoryCancellable getCatalogFactory() {
    if ((factory == null) || (factory.isBusy()))
      factory = new CatalogFactoryCancellable( this, "cancel", true);
    return factory;
  }

  /** Should not be public; artifact of interfaces. */
  public void failed() {
    // javax.swing.JOptionPane.showMessageDialog(this, "catalog reading failed ");
  }

  public void redisplay() {
    setCatalog( catalog);
  }

  /**
   * Set the catalog to be displayed. If ok, then a "Catalog" PropertyChangeEvent is sent.
   * @param catalog to be displayed
   */
  public void setCatalog(InvCatalogImpl catalog) {
    if (catalog == null) return;
    String catalogName = catalog.getBaseURI().toString();
    StringBuilder buff = new StringBuilder();
    if (!catalog.check( buff)) {
      javax.swing.JOptionPane.showMessageDialog(this, "Invalid catalog <"+ catalogName+">\n"+
        buff.toString());
      System.out.println("Invalid catalog <"+ catalogName+">\n"+buff.toString());
      tree.setModel(new DefaultTreeModel(new DefaultMutableTreeNode(null, false)));
      return;
    }

    if (filter != null)
      catalog.filter(filter);
    this.catalog = catalog;

    // send catalog event
    setCatalogURL( catalogName);

    // display tree
    // this sends TreeNode events
    try {
      model = new InvCatalogTreeModel( (InvDatasetImpl) catalog.getDataset());
      tree.setModel( model);
    } catch (Exception e) {
      e.printStackTrace();
      javax.swing.JOptionPane.showMessageDialog(this, e.getMessage());
      tree.setModel(new DefaultTreeModel(new DefaultMutableTreeNode(null, false)));
      return;
    }

      // debug
    if (false) {
      System.out.println("*** catalog/showJTree =");
      showNode(tree.getModel(), tree.getModel().getRoot());
      System.out.println("*** ");
    }

    // look for a specific dataset
    int pos = catalogName.indexOf('#');
    if (pos >= 0) {
      String id = catalogName.substring( pos+1);
      InvDataset dataset = catalog.findDatasetByID( id);
      if (dataset != null) {
        setSelectedDataset((InvDatasetImpl) dataset);
        firePropertyChangeEvent( new PropertyChangeEvent(this, "Selection", null, dataset));
      }
    }

    // send catalog event
    firePropertyChangeEvent(new PropertyChangeEvent(this, "Catalog", null, catalogName));

    return;
  }

  // debug
  private void showNode(TreeModel tree, Object node) {
    if (node == null) return;
    InvCatalogTreeNode tnode = (InvCatalogTreeNode) node;
    InvDataset cp = tnode.ds;
    System.out.println(" node= "+cp.getFullName()+" leaf= "+tree.isLeaf( node));
    for (int i=0; i< tree.getChildCount(node); i++)
      showNode(tree, tree.getChild(node, i));
  }

  // make an InvCatalog into a TreeModel
  private class InvCatalogTreeModel extends javax.swing.tree.DefaultTreeModel {
    InvCatalogTreeModel (InvDatasetImpl top) {
      super( new InvCatalogTreeNode( null, top), false);
    }
  }

  // make an InvDataset into a TreeNode
  // defer opening catalogRefs
  private class InvCatalogTreeNode implements javax.swing.tree.TreeNode, CatalogSetCallback {
    InvDataset ds;
    private InvCatalogTreeNode parent;
    private ArrayList children = null;
    private boolean isReading = false;

    InvCatalogTreeNode( InvCatalogTreeNode parent, InvDatasetImpl ds) {
      this.parent = parent;
      this.ds = ds;
      ds.setUserProperty("TreeNode", this);
      if (debugTree) System.out.println("new="+ds.getName()+" ");
      firePropertyChangeEvent(new PropertyChangeEvent(this, "TreeNode", null, ds));
    }

    public Enumeration children() {
      if (debugTree) System.out.println("children="+ds.getName()+" ");
      if (children == null) return Collections.enumeration( new ArrayList());
      return Collections.enumeration(children);
    }

    public boolean getAllowsChildren() {
      return true;
    }

    public TreeNode getChildAt(int index) {
     if (debugTree) System.out.println("getChildAt="+ds.getName()+" "+index);
     return (TreeNode) children.get(index);
    }

    public int getChildCount() {
      if (children == null) makeChildren( false);
      if (children == null) return 0;

      return children.size();
    }

    void makeChildren(boolean force) {
      if (children == null) {
        if (ds instanceof InvCatalogRef) {
          InvCatalogRef catref = (InvCatalogRef) ds;
          if (debugRef) System.out.println("getChildCount on catref="+ds.getName()+" "
            +catref.isRead()+" "+isReading);
          if (!catref.isRead() && !force) { // dont open it until explicitly asked
            return;
          }
        }

        if (debugRef) System.out.println("getChildCount on ds="+ds.getName()+" ");
        children = new ArrayList();
        for (Iterator iter = ds.getDatasets().iterator(); iter.hasNext(); )
          children.add( new InvCatalogTreeNode( this, (InvDatasetImpl) iter.next()));
      }
    }

    void readCatref() {
      InvCatalogRef catref = (InvCatalogRef) ds;
      if (debugRef) System.out.println("readCatref on ="+ds.getName()+" "+isReading);
      if (!isReading) {
        isReading = true;
        catref.readAsynch(getCatalogFactory(), this);
      }
    }

    public int getIndex(TreeNode child) {
      if (debugTree) System.out.println("getIndex="+ds.getName()+" "+child);
      return children.indexOf(child);
    }

    public TreeNode getParent() { return parent; }

    public boolean isLeaf() {
      if (debugTree) System.out.println("isLeaf="+ds.getName());
      if (ds instanceof InvCatalogRef) {
        InvCatalogRef catref = (InvCatalogRef) ds;
        if (!catref.isRead()) return false;
      }
      return !ds.hasNestedDatasets();
    }

    public String toString() { return ds.getName(); }

    public void setCatalog(InvCatalogImpl catalog) {
      children = new ArrayList();
      java.util.List datasets = ds.getDatasets();
      int[] childIndices = new int[ datasets.size()];
      for (int count = 0; count < datasets.size(); count++) {
        children.add( new InvCatalogTreeNode( this, (InvDatasetImpl) datasets.get(count)));
        childIndices[count] = count;
      }

      model.nodesWereInserted(this, childIndices);
      // model.nodeStructureChanged( this);
      tree.expandPath( makeTreePath(this));
      if (debugRef) System.out.println("model.nodeStructureChanged on "+this);
      isReading = false;
    }

    public void failed() {
      if (debugRef) System.out.println("failed called on "+this);
      isReading = false;
    }
  }


  // this is to get the inline documentation into a tooltip
  private class MyTreeCellRenderer extends javax.swing.tree.DefaultTreeCellRenderer {
    ImageIcon refIcon, refReadIcon, gridIcon, imageIcon, dqcIcon, dsScanIcon;

    public MyTreeCellRenderer() {
      refIcon = thredds.ui.BAMutil.getIcon( "CatalogRef", true);
      refReadIcon = thredds.ui.BAMutil.getIcon( "CatalogRefRead", true);
      gridIcon = thredds.ui.BAMutil.getIcon( "GridData", true);
      imageIcon = thredds.ui.BAMutil.getIcon( "ImageData", true);
      dqcIcon = thredds.ui.BAMutil.getIcon( "DQCData", true);
      dsScanIcon = thredds.ui.BAMutil.getIcon( "DatasetScan", true);
    }

    public Component getTreeCellRendererComponent(JTree tree, Object value,
      boolean selected, boolean expanded,boolean leaf, int row, boolean hasFocus) {

      Component c = super.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);

      //if (!hasFocus || !(c instanceof JComponent))
      //  return c;

      if (value instanceof InvCatalogTreeNode) {
        InvCatalogTreeNode node = (InvCatalogTreeNode) value;
        InvDataset ds = node.ds;

        String doc = makeDocs( ds.getDocumentation());
        if (doc != null)
          ((JComponent)c).setToolTipText( doc);

        if (ds instanceof InvDatasetScan) {
          setIcon(dsScanIcon);

        } else if (ds instanceof InvCatalogRef) {
            if (((InvCatalogRef)ds).isRead()) setIcon(refReadIcon); else setIcon(refIcon);
          
        } else if (leaf) {
          if (null != ds.getAccess( ServiceType.QC))
            setIcon( dqcIcon);
          else if (ds.getDataType() == FeatureType.GRID)
            setIcon( gridIcon);
          else if (ds.getDataType() == FeatureType.IMAGE)
            setIcon( imageIcon);
        }
      }

        // setToolTipText( ds.getToolTipText()); // LOOK
      return c;
    }

    private String makeDocs( java.util.List docs) {
      if (docs == null) return null;
      StringBuffer sbuff = new StringBuffer(1000);
      Iterator iter = docs.iterator();
      while(iter.hasNext()) {
        InvDocumentation doc = (InvDocumentation) iter.next();
        sbuff.append( doc.getInlineContent());
      }
      return sbuff.toString();
    }

  }

}

/* Change History:
   $Log: CatalogTreeView.java,v $
   Revision 1.20  2005/09/30 17:34:58  caron
   no message

   Revision 1.19  2005/08/29 21:43:39  caron
   change order of event firing

   Revision 1.18  2005/08/26 00:30:02  caron
   more TDS configurator work
   variable now mixed content

   Revision 1.17  2005/08/22 01:12:25  caron
   DatasetEditor

   Revision 1.16  2005/08/17 00:13:50  caron
   Dataset Editor

   Revision 1.15  2005/08/08 19:38:59  caron
   minor

   Revision 1.14  2005/08/05 18:40:22  caron
   no message

   Revision 1.13  2005/05/25 21:09:37  caron
   no message

   Revision 1.12  2005/04/27 22:08:04  caron
   no message

   Revision 1.11  2004/10/15 19:16:07  caron
   enum now keyword in 1.5
   SelectDateRange send ISO date string

   Revision 1.10  2004/09/30 00:33:37  caron
   *** empty log message ***

   Revision 1.9  2004/09/24 03:26:31  caron
   merge nj22

   Revision 1.8  2004/06/12 02:01:11  caron
   dqc 0.3

   Revision 1.7  2004/06/09 00:27:28  caron
   version 2.0a release; cleanup javadoc

   Revision 1.6  2004/05/11 23:30:32  caron
   release 2.0a

   Revision 1.5  2004/03/05 23:35:48  caron
   rel 1.3.1 javadoc

   Revision 1.4  2004/03/05 17:21:50  caron
   1.3.1 release

   Revision 1.3  2004/02/20 00:49:53  caron
   1.3 changes

   Revision 1.2  2003/12/04 22:27:45  caron
   *** empty log message ***

   Revision 1.1  2003/05/29 22:59:49  john
   refactor choosers into toolkit framework

 */