// $Id: DatasetTreeView.java 50 2006-07-12 16:30:06Z caron $
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

package ucar.nc2.ui;

import ucar.nc2.*;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.*;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;
import java.util.*;
import java.util.Enumeration;

import javax.swing.*;
import javax.swing.event.EventListenerList;
import javax.swing.tree.*;

/**
 * A Tree View of the groups and variables inside a NetcdfFile.
 *
 *
 * @author caron
 * @version $Revision: 50 $ $Date: 2006-07-12 16:30:06Z $
 */

public class DatasetTreeView extends JPanel {
  private EventListenerList listenerList = new EventListenerList();

  // ui
  private JTree tree;
  private DatasetTreeModel model;
  private NetcdfFile currentDataset = null;

  private boolean debugTree = false;

  /**
   * Constructor.
   */
  public DatasetTreeView() {
    // the catalog tree
    tree = new JTree() {
      public JToolTip createToolTip() { return new thredds.ui.MultilineTooltip(); }
    };
    tree.setModel(new DefaultTreeModel(new DefaultMutableTreeNode(null, false)));
    tree.setCellRenderer(new MyTreeCellRenderer());

    tree.addMouseListener( new MouseAdapter() {
      public void mousePressed(MouseEvent e) {
        int selRow = tree.getRowForLocation(e.getX(), e.getY());
        if (selRow != -1) {
          TreeNode node = (TreeNode) tree.getLastSelectedPathComponent();
          if (node instanceof VariableNode) {
            VariableIF v = ((VariableNode) node).var;
            firePropertyChangeEvent(new PropertyChangeEvent(this, "Selection", null, v));
          }
        }

        if ((selRow != -1) && (e.getClickCount() == 2)) {
          //acceptSelected();
        }
      }
    });

    tree.putClientProperty("JTree.lineStyle", "Angled");
    tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
    tree.setToggleClickCount(1);
    ToolTipManager.sharedInstance().registerComponent(tree);

   // layout
    setLayout(new BorderLayout());
    add(new JScrollPane(tree), BorderLayout.CENTER);
  }


  /**
   * Add a PropertyChangeEvent Listener. Throws a PropertyChangeEvent:
   * <ul>
   * <li>  when a node is selected:
   *     propertyName = "Selection", getNewValue() = Variable chosen.
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

  public void setFile( NetcdfFile ds) {
    if (ds != currentDataset) {
      currentDataset = ds;
      model = new DatasetTreeModel(ds);
      tree.setModel(model);
    }
  }


  /**
   * Get the currently selected InvDataset.
   * @return selected InvDataset, or null if none.
   *
  public Variable getSelected() {
    TreeNode tnode = tree.getLastSelectedPathComponent();
    return tnode == null ? null : tnode.ds;
  } */

  /**
   * Set the currently selected Variable.
   * @param v select this Variable, must be already in the tree.
   */
  public void setSelected( VariableIF v ) {
    if (v == null) return;

    // construct chain of variables
    ArrayList vchain = new ArrayList();
    vchain.add( v);

    VariableIF vp = v;
    while (vp.isMemberOfStructure()) {
      vp = vp.getParentStructure();
      vchain.add( 0, vp); // reverse
    }

    // construct chain of groups
    ArrayList gchain = new ArrayList();
    Group gp = vp.getParentGroup();
    if (gp == null)
      System.out.println("set an alarm");
    gchain.add( gp);
    while (gp.getParentGroup() != null) {
      gp = gp.getParentGroup();
      gchain.add( 0, gp); // reverse
    }

    ArrayList pathList = new ArrayList();

    // start at root, work down through the nested groups, if any
    GroupNode gnode = (GroupNode) model.getRoot();
    pathList.add( gnode);
    Group parentGroup = (Group) gchain.get(0); // always the root group

    for (int i=1; i<gchain.size(); i++) {
      parentGroup = (Group) gchain.get(i);
      gnode = gnode.findNestedGroup( parentGroup);
      pathList.add( gnode);
    }

    vp = (VariableIF) vchain.get(0);
    VariableNode vnode = gnode.findNestedVariable( vp);
    if (vnode == null) return; // not found
    pathList.add( vnode);

    // now work down through the structure members, if any
    for (int i=1; i<vchain.size(); i++) {
      vp = (VariableIF) vchain.get(i);
      vnode = vnode.findNestedVariable( vp);
      if (vnode == null) return; // not found
      pathList.add(vnode);
    }

    // convert to TreePath, and select it
    Object[] paths = pathList.toArray();
    TreePath treePath = new TreePath(paths);
    tree.setSelectionPath( treePath);
    tree.scrollPathToVisible( treePath);
  }

  /*
   * Create the TreePath corresponding to the InvDataset.
   * @param ds the InvDataset, must be already in the tree.
   * @return the corresponding TreePath.
   *
  TreePath makePath(Variable v) {
    TreeNode node = (TreeNode) ds.getUserProperty("TreeNode");
    return makeTreePath( node);
  }

  /*
   * Create the TreePath corresponding to the given TreeNode.
   * @param node the TreeNode; already in the Tree.
   * @return the corresponding TreePath.
   *
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
  } */

  /*
   * Open all nodes of the tree.
   * @param includeCatref open catrefs?
   *
  public void openAll( boolean includeCatref) {
    if (catalog == null) return;
    open( (InvCatalogTreeNode) model.getRoot(), includeCatref);
    tree.repaint();
  }

  private void open( InvCatalogTreeNode node, boolean includeCatref) {
    if (node == null) return;
    node.makeChildren( includeCatref);
    tree.expandPath(makeTreePath(node));

    Enumeration enum = node.children();
    while (enum.hasMoreElements()) {
      open( (InvCatalogTreeNode) enum.nextElement(), includeCatref);
    }
  } */

  /*

  void acceptSelected() {
    InvDataset ds = getSelectedDataset();
    if (ds == null) return;
    if (accessOnly && !ds.hasAccess()) return;

    //setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
    firePropertyChangeEvent( ds);
    //setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
  } */

  /*
   * Set the catalog to be displayed. If ok, then a "Catalog" PropertyChangeEvent is sent.
   * @param catalog to be displayed
   *
  public void setDataset(NetcdfFile dataset) {
    if (dataset == null) return;
    String catalogName = catalog.getBaseURI().toString();
    StringBuffer buff = new StringBuffer();
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
    firePropertyChangeEvent(new PropertyChangeEvent(this, "Catalog", null, catalogName));

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

    return;
  } */

  // make an InvCatalog into a TreeModel
  private class DatasetTreeModel extends javax.swing.tree.DefaultTreeModel {
    DatasetTreeModel (NetcdfFile file) {
      super( new GroupNode( null, file.getRootGroup()), false);
    }
  }

  private class GroupNode implements javax.swing.tree.TreeNode {
    private Group group;
    private GroupNode parent;
    private ArrayList children = null;

    GroupNode( GroupNode parent, Group group) {
      this.parent = parent;
      this.group = group;
      if (debugTree) System.out.println("new="+group.getName()+" ");
      //firePropertyChangeEvent(new PropertyChangeEvent(this, "TreeNode", null, group));
    }

    public Enumeration children() {
      if (children == null) makeChildren();
      return Collections.enumeration(children);
    }

    public boolean getAllowsChildren() { return true; }
    public TreeNode getChildAt(int index) { return (TreeNode) children.get(index); }

    public int getChildCount() {
      if (children == null) makeChildren();
      return children.size();
    }

    void makeChildren() {
      children = new ArrayList();
      List groups = group.getGroups();
      for (int i=0; i<groups.size(); i++)
        children.add( new GroupNode( this, (Group) groups.get(i)));

      List dims = group.getDimensions();
      for (int i=0; i<dims.size(); i++)
        children.add( new DimensionNode( this, (Dimension) dims.get(i)));

      List vars = group.getVariables();
      for (int i=0; i<vars.size(); i++)
        children.add( new VariableNode( this, (VariableIF) vars.get(i)));

      if (debugTree) System.out.println("children="+group.getName()+" ");
    }

    public int getIndex(TreeNode child) {
      if (debugTree) System.out.println("getIndex="+group.getName()+" "+child);
      return children.indexOf(child);
    }

    public TreeNode getParent() { return parent; }
    public boolean isLeaf() { return false; }
    public String toString() {
      if (parent == null) // root group
        return currentDataset.getLocation();
      else
        return group.getShortName();
    }

    public GroupNode findNestedGroup( Group g) {
      if (children == null) makeChildren();
      for (int i=0; i<children.size(); i++) {
        GroupNode elem = (GroupNode) children.get(i);
        if (elem.group == g) return elem;
      }
      return null;
    }

    public VariableNode findNestedVariable( VariableIF v) {
      if (children == null) makeChildren();
      for (int i=0; i<children.size(); i++) {
        TreeNode node = (TreeNode) children.get(i);
        if (node instanceof VariableNode ) {
          VariableNode vnode= (VariableNode) node;
          if (vnode.var == v) return vnode;
        }
      }
      return null;
    }

    public String getToolTipText() {
      return group.getNameAndAttributes();
    }

  }

  private class VariableNode implements javax.swing.tree.TreeNode {
    private VariableIF var;
    private TreeNode parent;
    private ArrayList children = null;

    VariableNode( TreeNode parent, VariableIF var) {
      this.parent = parent;
      this.var = var;
      if (debugTree) System.out.println("new var="+var.getName()+" ");
      //firePropertyChangeEvent(new PropertyChangeEvent(this, "TreeNode", null, var));
    }

    public Enumeration children() {
      if (children == null) makeChildren();
      return Collections.enumeration(children);
    }

    public boolean getAllowsChildren() { return true; }
    public TreeNode getChildAt(int index) { return (TreeNode) children.get(index); }

    public int getChildCount() {
      if (children == null) makeChildren();
      return children.size();
    }

    void makeChildren() {
      children = new ArrayList();

      if (var instanceof Structure) {
        Structure s = (Structure) var;
        List vars = s.getVariables();
        for (int i=0; i<vars.size(); i++)
          children.add( new VariableNode( this, (VariableIF) vars.get(i)));
      }

      if (debugTree) System.out.println("children="+var.getName()+" ");
    }

    public int getIndex(TreeNode child) {
      if (debugTree) System.out.println("getIndex="+var.getName()+" "+child);
      return children.indexOf(child);
    }

    public TreeNode getParent() { return parent; }
    public boolean isLeaf() { return (getChildCount() == 0); }
    public String toString() { return var.getShortName(); }

    public VariableNode findNestedVariable( VariableIF v) {
      if (children == null) makeChildren();
      for (int i=0; i<children.size(); i++) {
        VariableNode elem = (VariableNode) children.get(i);
        if (elem.var == v) return elem;
      }
      return null;
    }

    public String getToolTipText() {
      return var.toString();
    }
  }

  private class DimensionNode implements javax.swing.tree.TreeNode {
    private Dimension d;
    private TreeNode parent;
    private ArrayList children = null;

    DimensionNode( TreeNode parent, Dimension d) {
      this.parent = parent;
      this.d = d;
    }

    public Enumeration children() { return null;}

    public boolean getAllowsChildren() { return false; }
    public TreeNode getChildAt(int index) { return null; }

    public int getChildCount() { return 0; }

    public int getIndex(TreeNode child) { return 0; }

    public TreeNode getParent() { return parent; }
    public boolean isLeaf() { return true; }
    public String toString() { return d.getName(); }

    public String getToolTipText() {
      return d.toString();
    }
  }


  // this is to get different icons
  private class MyTreeCellRenderer extends javax.swing.tree.DefaultTreeCellRenderer {
    ImageIcon structIcon, dimIcon;
    String tooltipText = null;

    public MyTreeCellRenderer() {
      structIcon = thredds.ui.BAMutil.getIcon( "Structure", true);
      dimIcon = thredds.ui.BAMutil.getIcon( "Dimension", true);
    }

    public Component getTreeCellRendererComponent(JTree tree, Object value,
      boolean selected, boolean expanded,boolean leaf, int row, boolean hasFocus) {

      Component c = super.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);

      if (value instanceof VariableNode) {
        VariableNode node = (VariableNode) value;
        tooltipText = node.getToolTipText();

        if (node.var instanceof Structure) {
          Structure s = (Structure) node.var;
          setIcon( structIcon);
          tooltipText = s.getNameAndAttributes();
        } else
          tooltipText = node.getToolTipText();
      }

      else if (value instanceof DimensionNode) {
        DimensionNode node = (DimensionNode) value;
        tooltipText = node.getToolTipText();
        setIcon( dimIcon);
      }

      else if (value instanceof GroupNode) {
        GroupNode node = (GroupNode) value;
        tooltipText = node.getToolTipText();
      }

      return c;
    }

    public String getToolTipText() { return tooltipText; }

  }

}