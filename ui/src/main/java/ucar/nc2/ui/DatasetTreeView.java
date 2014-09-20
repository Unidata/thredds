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

import ucar.nc2.Dimension;
import ucar.nc2.*;
import ucar.nc2.ui.widget.BAMutil;
import ucar.nc2.ui.widget.MultilineTooltip;

import javax.swing.*;
import javax.swing.tree.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

/**
 * A Tree View of the groups and variables inside a NetcdfFile.
 *
 *
 * @author caron
 */

public class DatasetTreeView extends JPanel {
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
      public JToolTip createToolTip() { return new MultilineTooltip(); }
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

  private void firePropertyChangeEvent(PropertyChangeEvent event) {
    firePropertyChange(event.getPropertyName(), event.getOldValue(), event.getNewValue());
  }

  public void setFile( NetcdfFile ds) {
    if (ds != currentDataset) {
      currentDataset = ds;
      model = new DatasetTreeModel(ds);
      tree.setModel(model);
    }
  }

  public void clear() {
    currentDataset = null;
    model = null;
    tree.setModel(null);
  }

  /**
   * Set the currently selected Variable.
   * @param v select this Variable, must be already in the tree.
   */
  public void setSelected( VariableIF v ) {
    if (v == null) return;

    // construct chain of variables
    List<VariableIF> vchain = new ArrayList<>();
    vchain.add( v);

    VariableIF vp = v;
    while (vp.isMemberOfStructure()) {
      vp = vp.getParentStructure();
      vchain.add( 0, vp); // reverse
    }

    // construct chain of groups
    List<Group> gchain = new ArrayList<>();
    Group gp = vp.getParentGroup();

    gchain.add( gp);
    while (gp.getParentGroup() != null) {
      gp = gp.getParentGroup();
      gchain.add( 0, gp); // reverse
    }

    List<Object> pathList = new ArrayList<>();

    // start at root, work down through the nested groups, if any
    GroupNode gnode = (GroupNode) model.getRoot();
    pathList.add( gnode);
    Group parentGroup = gchain.get(0); // always the root group

    for (int i=1; i<gchain.size(); i++) {
      parentGroup = gchain.get(i);
      gnode = gnode.findNestedGroup( parentGroup);
      assert gnode != null;
      pathList.add( gnode);
    }

    vp = vchain.get(0);
    VariableNode vnode = gnode.findNestedVariable( vp);
    if (vnode == null) return; // not found
    pathList.add( vnode);

    // now work down through the structure members, if any
    for (int i=1; i<vchain.size(); i++) {
      vp = vchain.get(i);
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

  // make an NetcdfFile into a TreeModel
  private class DatasetTreeModel extends javax.swing.tree.DefaultTreeModel {
    DatasetTreeModel (NetcdfFile file) {
      super( new GroupNode( null, file.getRootGroup()), false);
    }
  }

  private class GroupNode implements javax.swing.tree.TreeNode {
    private Group group;
    private GroupNode parent;
    private List<Object> children = null;

    GroupNode( GroupNode parent, Group group) {
      this.parent = parent;
      this.group = group;
      if (debugTree) System.out.println("new="+group.getFullName()+" ");
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
      children = new ArrayList<>();

      List dims = group.getDimensions();
      for (int i=0; i<dims.size(); i++)
        children.add( new DimensionNode( this, (Dimension) dims.get(i)));

      List vars = group.getVariables();
      for (int i=0; i<vars.size(); i++)
        children.add( new VariableNode( this, (VariableIF) vars.get(i)));

      List groups = group.getGroups();
      for (int i=0; i<groups.size(); i++)
        children.add( new GroupNode( this, (Group) groups.get(i)));

      if (debugTree) System.out.println("children="+group.getFullName()+" ");
    }

    public int getIndex(TreeNode child) {
      if (debugTree) System.out.println("getIndex="+group.getFullName()+" "+child);
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
        if (children.get(i) instanceof GroupNode) {
          GroupNode elem = (GroupNode) children.get(i);
          if (elem.group == g) return elem;
        }
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
    private List<Object> children = null;

    VariableNode( TreeNode parent, VariableIF var) {
      this.parent = parent;
      this.var = var;
      if (debugTree) System.out.println("new var="+var.getShortName()+" ");
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
      children = new ArrayList<>();

      if (var instanceof Structure) {
        Structure s = (Structure) var;
        List vars = s.getVariables();
        for (int i=0; i<vars.size(); i++)
          children.add( new VariableNode( this, (VariableIF) vars.get(i)));
      }

      if (debugTree) System.out.println("children="+var.getShortName()+" ");
    }

    public int getIndex(TreeNode child) {
      if (debugTree) System.out.println("getIndex="+var.getShortName()+" "+child);
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

  private static class DimensionNode implements javax.swing.tree.TreeNode {
    private Dimension d;
    private TreeNode parent;

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
    public String toString() { return d.getShortName(); }

    public String getToolTipText() {
      return d.toString();
    }
  }


  // this is to get different icons
  private static class MyTreeCellRenderer extends javax.swing.tree.DefaultTreeCellRenderer {
    ImageIcon structIcon, dimIcon;
    String tooltipText = null;

    public MyTreeCellRenderer() {
      structIcon = BAMutil.getIcon( "Structure", true);
      dimIcon = BAMutil.getIcon( "Dimension", true);
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
