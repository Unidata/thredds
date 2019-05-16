/*
 * Copyright (c) 1998-2019 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.ui.op;

import javax.annotation.Nullable;
import ucar.nc2.Dimension;
import ucar.nc2.*;
import ucar.ui.widget.BAMutil;
import ucar.ui.widget.MultilineTooltip;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import javax.swing.ImageIcon;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JToolTip;
import javax.swing.JTree;
import javax.swing.ToolTipManager;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

/**
 * A Tree View of the groups and variables inside a NetcdfFile.
 *
 *
 * @author caron
 */
public class DatasetTreeView extends JPanel {

    private static final org.slf4j.Logger logger
                = org.slf4j.LoggerFactory.getLogger (MethodHandles.lookup ( ).lookupClass ( ));

    // ui
    private JTree tree;
    private DatasetTreeModel model;
    private NetcdfFile currentDataset;

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

/**
 *
 */
    private void firePropertyChangeEvent(PropertyChangeEvent event) {
        firePropertyChange(event.getPropertyName(), event.getOldValue(), event.getNewValue());
    }

/**
 *
 */
    public void setFile( NetcdfFile ds) {
        if (ds != currentDataset) {
            currentDataset = ds;
            model = new DatasetTreeModel(ds);
            tree.setModel(model);
        }
    }

/**
 &
 */
    public void clear() {
        currentDataset = null;
        model = null;
        tree.setModel(null);
    }

/**
 * Set the currently selected Variable.
 *
 * @param v select this Variable, must be already in the tree.
 */
    public void setSelected( VariableIF v ) {
        if (v == null) { return; }

        // construct chain of variables
        final List<VariableIF> vchain = new ArrayList<>();
        vchain.add( v);

        VariableIF vp = v;
        while (vp.isMemberOfStructure()) {
            vp = vp.getParentStructure();
            vchain.add( 0, vp); // reverse
        }

        // construct chain of groups
        final List<Group> gchain = new ArrayList<>();
        Group gp = vp.getParentGroup();

        gchain.add( gp);
        while (gp.getParentGroup() != null) {
            gp = gp.getParentGroup();
            gchain.add( 0, gp); // reverse
        }

        final List<Object> pathList = new ArrayList<>();

        // start at root, work down through the nested groups, if any
        GroupNode gnode = (GroupNode) model.getRoot();
        pathList.add( gnode);
        Group parentGroup; // always the root group
        for (int i=1; i < gchain.size(); i++) {
            parentGroup = gchain.get(i);
            gnode = gnode.findNestedGroup( parentGroup);
            assert gnode != null;
            pathList.add( gnode);
        }

        vp = vchain.get(0);
        VariableNode vnode = gnode.findNestedVariable( vp);
        if (vnode == null) { return; } // not found
        pathList.add( vnode);

        // now work down through the structure members, if any
        for (int i=1; i < vchain.size(); i++) {
            vp = vchain.get(i);
            vnode = vnode.findNestedVariable( vp);
            if (vnode == null) { return; } // not found
            pathList.add(vnode);
        }

        // convert to TreePath, and select it
        final Object[] paths = pathList.toArray();
        final TreePath treePath = new TreePath(paths);
        tree.setSelectionPath( treePath);
        tree.scrollPathToVisible( treePath);
    }

/**
 * make an NetcdfFile into a TreeModel
 */
    private class DatasetTreeModel extends DefaultTreeModel {
        DatasetTreeModel (NetcdfFile file) {
            super( new GroupNode( null, file.getRootGroup()), false);
        }
    }

/**
 *
 */
    private class GroupNode implements TreeNode {
        private Group group;
        private GroupNode parent;
        private List<Object> children = null;

    /**
     *
     */
        GroupNode( GroupNode parent, Group group) {
            this.parent = parent;
            this.group = group;

            logger.debug("new={}", group.getFullName());

            //firePropertyChangeEvent(new PropertyChangeEvent(this, "TreeNode", null, group));
        }

    /**
     *
     */
        public Enumeration children() {
            if (children == null) { makeChildren(); }
            return Collections.enumeration(children);
        }

        public boolean getAllowsChildren() { return true; }

        public TreeNode getChildAt(int index) { return (TreeNode) children.get(index); }

        public int getChildCount() {
            if (children == null) { makeChildren(); }
            return children.size();
        }

        void makeChildren() {
            children = new ArrayList<>();

            List dims = group.getDimensions();
            for (Object dim : dims) {
                children.add(new DimensionNode(this, (Dimension) dim));
            }

            List vars = group.getVariables();
            for (Object var : vars) {
                children.add(new VariableNode(this, (VariableIF) var));
            }

            List groups = group.getGroups();
            for (Object group1 : groups) {
                children.add(new GroupNode(this, (Group) group1));
            }

            logger.debug("children={}", group.getFullName());
        }

        public int getIndex(TreeNode child) {
            logger.debug("getIndex={} {}", group.getFullName(), child);
            return children.indexOf(child);
        }

        public TreeNode getParent() { return parent; }

        public boolean isLeaf() { return false; }

        public String toString() {
            if (parent == null) {
                // root group
                return currentDataset.getLocation();
            }
            else {
                return group.getShortName();
            }
        }

    @Nullable
    public GroupNode findNestedGroup( Group g) {
            if (children == null) { makeChildren(); }
            for (Object child : children) {
                if (child instanceof GroupNode) {
                    final GroupNode elem = (GroupNode) child;
                    if (elem.group == g) {
                        return elem;
                    }
                }
            }
            return null;
        }

    @Nullable
    public VariableNode findNestedVariable( VariableIF v) {
            if (children == null) { makeChildren(); }
            for (Object child : children) {
                final TreeNode node = (TreeNode) child;
                if (node instanceof VariableNode) {
                    final VariableNode vnode = (VariableNode) node;
                    if (vnode.var == v) {
                        return vnode;
                    }
                }
            }
            return null;
        }

        public String getToolTipText() {
            return group.getNameAndAttributes();
        }

  }

/**
 *
 */
    private class VariableNode implements TreeNode {
        private VariableIF var;
        private TreeNode parent;
        private List<Object> children = null;

    /**
     *
     */
        VariableNode( TreeNode parent, VariableIF var) {
            this.parent = parent;
            this.var = var;

            logger.debug("new var={}", var.getShortName());

            //firePropertyChangeEvent(new PropertyChangeEvent(this, "TreeNode", null, var));
        }

    /**
     *
     */
        public Enumeration children() {
            if (children == null) { makeChildren(); }
            return Collections.enumeration(children);
        }

        public boolean getAllowsChildren() { return true; }

        public TreeNode getChildAt(int index) { return (TreeNode) children.get(index); }

        public int getChildCount() {
          if (children == null) { makeChildren(); }
          return children.size();
        }

        void makeChildren() {
            children = new ArrayList<>();

            if (var instanceof Structure) {
                final Structure s = (Structure) var;
                final List vars = s.getVariables();
                for (Object var1 : vars) {
                    children.add(new VariableNode(this, (VariableIF) var1));
                }
            }
            logger.debug("children={}", var.getShortName());
        }

        public int getIndex(TreeNode child) {
            logger.debug("getIndex={} {}", var.getShortName(), child);
            return children.indexOf(child);
        }

        public TreeNode getParent() { return parent; }

        public boolean isLeaf() { return (getChildCount() == 0); }

        public String toString() { return var.getShortName(); }

    @Nullable
        public VariableNode findNestedVariable( VariableIF v) {
            if (children == null) { makeChildren(); }
            for (Object child : children) {
                final VariableNode elem = (VariableNode) child;
                if (elem.var == v) {
                    return elem;
                }
            }
            return null;
        }

        public String getToolTipText() {
            return var.toString();
        }
    }

/**
 *
 */
    private static class DimensionNode implements TreeNode {
        private Dimension d;
        private TreeNode parent;

    /**
     *
     */
        DimensionNode( TreeNode parent, Dimension d) {
            this.parent = parent;
            this.d = d;
        }

    @Nullable
        public Enumeration children() { return null;}

        public boolean getAllowsChildren() { return false; }

    @Nullable
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

/**
 * this is to get different icons
 */
    private static class MyTreeCellRenderer extends DefaultTreeCellRenderer {
        ImageIcon structIcon, dimIcon;
        String tooltipText;

    /**
     *
     */
        public MyTreeCellRenderer() {
            structIcon = BAMutil.getIcon( "Structure", true);
            dimIcon = BAMutil.getIcon( "nj22/Dimension", true);
        }

    /**
     *
     */
        public Component getTreeCellRendererComponent(JTree tree, Object value,
                    boolean selected, boolean expanded,boolean leaf, int row, boolean hasFocus) {

            final Component c = super.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);

            if (value instanceof VariableNode) {
                final VariableNode node = (VariableNode) value;
                tooltipText = node.getToolTipText();

                if (node.var instanceof Structure) {
                    final Structure s = (Structure) node.var;
                    setIcon( structIcon);
                    tooltipText = s.getNameAndAttributes();
                }
                else {
                    tooltipText = node.getToolTipText();
                }
            }
            else if (value instanceof DimensionNode) {
                final DimensionNode node = (DimensionNode) value;
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
