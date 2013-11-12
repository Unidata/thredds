package ucar.nc2.ui;

import org.jdom2.input.SAXBuilder;
import org.jdom2.output.XMLOutputter;
import thredds.catalog.parser.jdom.FeatureCollectionReader;
import thredds.featurecollection.FeatureCollectionConfig;
import thredds.inventory.CollectionSpecParser;
import thredds.inventory.MFile;
import thredds.inventory.partition.DirectoryPartitionBuilder;
import ucar.nc2.grib.*;
import ucar.nc2.time.CalendarDateFormatter;
import ucar.nc2.ui.widget.*;
import ucar.nc2.ui.widget.PopupMenu;
import ucar.util.prefs.PreferencesExt;
import ucar.util.prefs.ui.BeanTableSorted;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import java.awt.*;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.io.*;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.List;

/**
 * GribCollection Directory Partition Viewer
 *
 * @author caron
 * @since 11/9/13
 */
public class DirectoryPartitionViewer extends JPanel {
  static private final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(DirectoryPartitionViewer.class);

  private PreferencesExt prefs;

  private FeatureCollectionConfig config;
  private String collectionName = "ncdc1year";
  private GribCdmIndexPanel cdmIndexTables;
  private PartitionsTable partitionsTable;

  private JPanel tablePanel;
  private JSplitPane mainSplit;

  private PartitionTreeBrowser partitionTreeBrowser;
  private NCdumpPane dumpPane;

  private TextHistoryPane infoTA;
  private StructureTable dataTable;
  private IndependentWindow infoWindow, dataWindow, dumpWindow;

  public DirectoryPartitionViewer(PreferencesExt prefs, JPanel buttPanel) {
    this.prefs = prefs;
    partitionTreeBrowser = new PartitionTreeBrowser();
    cdmIndexTables = new GribCdmIndexPanel((PreferencesExt) prefs.node("cdmIdx"), buttPanel);
    partitionsTable = new PartitionsTable((PreferencesExt) prefs.node("partTable"), buttPanel);

    setLayout(new BorderLayout());

    tablePanel = new JPanel(new BorderLayout());
    tablePanel.add(partitionTreeBrowser.view, BorderLayout.NORTH);
    // tablePanel.add(cdmIndexTables, BorderLayout.CENTER);   LOOK flip back and forth ??
    tablePanel.add(partitionsTable, BorderLayout.CENTER);
    current = partitionsTable;

    JScrollPane treeScroll = new JScrollPane(partitionTreeBrowser.tree);

    mainSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, false, treeScroll, tablePanel);
    mainSplit.setDividerLocation(prefs.getInt("mainSplit", 100));
    add(mainSplit, BorderLayout.CENTER);

    // the info window
    infoTA = new TextHistoryPane();
    infoWindow = new IndependentWindow("Information", BAMutil.getImage("netcdfUI"), infoTA);
    infoWindow.setBounds((Rectangle) prefs.getBean("InfoWindowBounds", new Rectangle(300, 300, 500, 300)));

    // the data Table
    dataTable = new StructureTable((PreferencesExt) prefs.node("structTable"));
    dataWindow = new IndependentWindow("Data Table", BAMutil.getImage("netcdfUI"), dataTable);
    dataWindow.setBounds((Rectangle) prefs.getBean("dataWindow", new Rectangle(50, 300, 1000, 600)));

    // the ncdump Pane
    dumpPane = new NCdumpPane((PreferencesExt) prefs.node("dumpPane"));
    dumpWindow = new IndependentWindow("NCDump Variable Data", BAMutil.getImage("netcdfUI"), dumpPane);
    dumpWindow.setBounds((Rectangle) prefs.getBean("DumpWindowBounds", new Rectangle(300, 300, 300, 200)));
  }

  public void save() {
    if (mainSplit != null) prefs.putInt("mainSplit", mainSplit.getDividerLocation());
    cdmIndexTables.save();
    partitionsTable.save();
  }

  private Component current;

  private void swap(Component want) {
    if (current == want) return;
    tablePanel.remove(current);
    tablePanel.add(want, BorderLayout.CENTER);
    tablePanel.revalidate();
    current = want;
    repaint();
  }


  ////////////////////////////////////////////////

  public void setCollection(String name) {
    Path f = Paths.get(name);
    if (!Files.exists(f)) return;
    if (Files.isDirectory(f)) {
      partitionTreeBrowser.setRoot(f);
      return;
    }

    org.jdom2.Document doc;
    try {
      SAXBuilder builder = new SAXBuilder();
      doc = builder.build(f.toFile());
    } catch (Exception e) {
      javax.swing.JOptionPane.showMessageDialog(this, "Error parsing featureCollection: " + e.getMessage());
      return;
    }

    if (true) {
      XMLOutputter xmlOut = new XMLOutputter();
      System.out.println(xmlOut.outputString(doc));
    }

    Formatter errlog = new Formatter();
    config = FeatureCollectionReader.readFeatureCollection(doc.getRootElement());
    CollectionSpecParser spec = new CollectionSpecParser(config.spec, errlog);
    partitionTreeBrowser.setRoot(Paths.get(spec.getRootDir()));
  }

  private void moveCdmIndexFile(NodeInfo indexFile) throws IOException {
    GribCollection gc = null;
    try {
      boolean ok = GribCdmIndex.moveCdmIndex(indexFile.dir.toString(), logger);
      Formatter f = new Formatter();
      f.format("moved success=%s", ok);
      infoTA.setText(f.toString());
      infoTA.gotoTop();
      infoWindow.show();
    } finally {
      if (gc != null) gc.close();
    }
  }

  /* private void moveCdmIndexAll(NodeInfo indexFile) {
    Formatter out = new Formatter();
    infoWindow.show();

    for (File f : indexFile.getParentFile().listFiles()) {
      if (!f.getName().endsWith(".ncx")) continue;
      GribCollection gc = null;
      try {
        boolean ok = GribCdmIndex.moveCdmIndex(f, logger);
        out.format("%s moved success=%s%n", f.getPath(), ok);
        infoTA.appendLine(f.getPath() + " moved success=" + ok);

      } catch (Throwable t) {
        out.format("%s moved failed=%s%n", f.getPath(), t.getMessage());
      }
    }

    infoTA.setText(out.toString());
    infoTA.gotoTop();
    infoWindow.show();
  } */

  private void cmdShowIndex(NodeInfo node) throws IOException {

    /* String indexFilename;

    if (topDir.isDirectory()) {
      DirectoryPartitionBuilder builder = new DirectoryPartitionBuilder(collectionName, topDir.getPath());
      Path indexPath = builder.getIndex();
      if (indexPath == null) {
        JOptionPane.showMessageDialog(this, "No index found in " + topDir);
        return;
      }
      fileBrowser.setFileDetails(indexPath.toFile());
      indexFilename = indexPath.toString();

    } else {
      indexFilename = topDir.getPath();
    } */

    if (!node.hasIndex) return;

    // this opens the index file and constructs a GribCollection
    cdmIndexTables.setIndexFile(node.part.getIndex());
    swap(cdmIndexTables);
  }

 /*  private void showChildrenIndex(NodeInfo topDir) {
    if (!topDir.isDirectory()) {
      JOptionPane.showMessageDialog(this, topDir.getPath() + " not a directory: ");
      return;
    }

    Formatter out = new Formatter();
    try {
      boolean ok = GribCdmIndex.showDirectoryPartitionIndex(collectionName, topDir, out);
      out.format("%s showChildrenIndex success=%s%n", topDir.getPath(), ok);
      infoTA.appendLine(out.toString());
      infoTA.gotoTop();
      infoWindow.show();

    } catch (Throwable t) {
      JOptionPane.showMessageDialog(this, topDir.getPath() + " showChildrenIndex failed: " + t.getMessage());
    }
  }


  private void showPartitions(File topDir) {
    Formatter out = new Formatter();
    try {
      GribCdmIndex indexWriter = new GribCdmIndex();
      Path topPath = Paths.get(topDir.getPath());
      DirectoryPartition dpart = new DirectoryPartition(config, topPath, indexWriter, out, logger);

      Grib2TimePartition tp = new Grib2TimePartition(dpart.getCollectionName(), topDir, config.gribConfig, logger);
      for (CollectionManagerRO dcm : dpart.makePartitions()) {
        tp.addPartition(dcm);
      }

      partitionsTable.clear();
      for (TimePartition.Partition tpp : tp.getPartitions()) {
        try {
          GribCollection gc = tpp.makeGribCollection(CollectionManager.Force.nocheck);    // use index if it exists
          partitionsTable.addGribCollection(gc);
          gc.close(); // ??
        } catch (Throwable t) {
          logger.error(" Failed to open partition " + tpp.getName(), t);
        }
      }

      swap(partitionsTable);

    } catch (Throwable t) {
      JOptionPane.showMessageDialog(this, topDir + " showPartitions failed: " + t.getMessage());
    }
  }  */


  private void cmdMakeIndex(NodeInfo node) {
    Formatter out = new Formatter();
    try {
      boolean ok = GribCdmIndex.makeDirectoryPartitionIndex(config, node.dir.toFile(), out);
      out.format("%s makeIndex success=%s%n", node, ok);
      infoTA.appendLine(out.toString());
      infoTA.gotoTop();
      infoWindow.show();

      if (ok) {
        node.refresh();
        cmdShowIndex(node);
      }

    } catch (Throwable t) {
      JOptionPane.showMessageDialog(this, node + " makeIndex failed: " + t.getMessage());
    }
  }

  private class NodeInfo {
    Path dir;
    DirectoryPartitionBuilder part;
    boolean hasIndex;

    NodeInfo(Path dir) {
      this.dir = dir;

      try {
        part = new DirectoryPartitionBuilder(collectionName, dir, null);
        hasIndex = part.getIndex() != null;

      } catch (IOException e) {
        e.printStackTrace();
      }
    }

    NodeInfo(DirectoryPartitionBuilder part) {
      this.part = part;
      this.dir = part.getDir();
      this.hasIndex = part.getIndex() != null;
    }

    List<NodeInfo> getChildren() {
      List<NodeInfo> result = new ArrayList<>(100);
      try {
        for (DirectoryPartitionBuilder child : part.constructChildren(new GribCdmIndex())) {
          result.add(new NodeInfo(child));
        }

      } catch (IOException e) {
        e.printStackTrace();
      }
      return result;
    }

    void refresh() {
      try {
        this.hasIndex = part.findIndex();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }

    @Override
    public String toString() {
      final StringBuilder sb = new StringBuilder("NodeInfo{");
      sb.append("dir=").append(dir);
      sb.append('}');
      return sb.toString();
    }
  }

  private class PartitionTreeBrowser {
    //private FileSystemView fileSystemView;
    private Icon dirIcon, fileIcon;

    private NodeInfo currentNode;
    JTree tree;  // File-system tree. Built Lazily
    private DefaultTreeModel treeModel;
    private JProgressBar progressBar;

    /* File details. */
    JPanel view;
    private JLabel nodeName;
    private JTextField path;
    private JLabel date;
    private JLabel size;
    private JCheckBox readable;
    private JCheckBox writable;
    private JRadioButton isDirectory;
    private JRadioButton isFile;

    public void setRoot(Path rootDir) {
      if (!Files.isDirectory(rootDir)) return;

      DefaultMutableTreeNode root = new DefaultMutableTreeNode();
      root.setUserObject(new NodeInfo(rootDir));
      treeModel = new DefaultTreeModel(root);
      tree.setModel(treeModel);
      tree.setSelectionInterval(0, 0);
    }

    public PartitionTreeBrowser() {
      dirIcon = UIManager.getIcon("FileView.directoryIcon");
      fileIcon = UIManager.getIcon("FileView.fileIcon");

      DefaultMutableTreeNode root = new DefaultMutableTreeNode();
      treeModel = new DefaultTreeModel(root);

      TreeSelectionListener treeSelectionListener = new TreeSelectionListener() {
        public void valueChanged(TreeSelectionEvent tse) {
          DefaultMutableTreeNode node = (DefaultMutableTreeNode) tse.getPath().getLastPathComponent();
          showChildren(node);
          currentNode = (NodeInfo) node.getUserObject();
          setFileDetails(currentNode);
        }
      };


      /* show the file system roots.
      File[] roots = fileSystemView.getRoots();
      for (File fileSystemRoot : roots) {
        DefaultMutableTreeNode node = new DefaultMutableTreeNode(fileSystemRoot);
        root.add(node);
        File[] files = fileSystemView.getFiles(fileSystemRoot, true);
        for (File file : files) {
          if (file.isDirectory()) {
            node.add(new DefaultMutableTreeNode(file));
          }
        }

      } */

      tree = new JTree(treeModel);
      tree.setRootVisible(true);
      tree.addTreeSelectionListener(treeSelectionListener);
      tree.setCellRenderer(new FileTreeCellRenderer());
      tree.expandRow(0);
      // tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
      tree.setToggleClickCount(1);

      // as per trashgod tip
      tree.setVisibleRowCount(15);

      JScrollPane treeScroll = new JScrollPane(tree);
      Dimension preferredSize = treeScroll.getPreferredSize();
      Dimension widePreferred = new Dimension(200, (int) preferredSize.getHeight());
      treeScroll.setPreferredSize(widePreferred);

      makePopups();

      /////////////////////
      // details for a File
      JPanel filePanel = new JPanel(new BorderLayout(4, 2));
      filePanel.setBorder(new EmptyBorder(0, 6, 0, 6));

      JPanel fileDetailsLabels = new JPanel(new GridLayout(0, 1, 2, 2));
      filePanel.add(fileDetailsLabels, BorderLayout.WEST);

      JPanel fileDetailsValues = new JPanel(new GridLayout(0, 1, 2, 2));
      filePanel.add(fileDetailsValues, BorderLayout.CENTER);

      fileDetailsLabels.add(new JLabel("File", JLabel.TRAILING));
      nodeName = new JLabel();
      fileDetailsValues.add(nodeName);
      fileDetailsLabels.add(new JLabel("Path/name", JLabel.TRAILING));
      path = new JTextField(5);
      path.setEditable(false);
      fileDetailsValues.add(path);
      fileDetailsLabels.add(new JLabel("Last Modified", JLabel.TRAILING));
      date = new JLabel();
      fileDetailsValues.add(date);
      fileDetailsLabels.add(new JLabel("File size", JLabel.TRAILING));
      size = new JLabel();
      fileDetailsValues.add(size);
      fileDetailsLabels.add(new JLabel("Type", JLabel.TRAILING));

      JPanel flags = new JPanel(new FlowLayout(FlowLayout.LEADING, 4, 0));

      isDirectory = new JRadioButton("Directory");
      flags.add(isDirectory);

      isFile = new JRadioButton("File");
      flags.add(isFile);
      fileDetailsValues.add(flags);

      //JToolBar toolBar = makeButtonBar();

      flags.add(new JLabel("::  Flags"));
      readable = new JCheckBox("Read  ");
      readable.setMnemonic('a');
      flags.add(readable);

      writable = new JCheckBox("Write  ");
      writable.setMnemonic('w');
      flags.add(writable);

      int count = fileDetailsLabels.getComponentCount();
      for (int ii = 0; ii < count; ii++) {
        fileDetailsLabels.getComponent(ii).setEnabled(false);
      }

      count = flags.getComponentCount();
      for (int ii = 0; ii < count; ii++) {
        flags.getComponent(ii).setEnabled(false);
      }

      ///////////////
      // progressBar
      progressBar = new JProgressBar();
      progressBar.setVisible(true);

      //////////////
      // put together the fileView

      view = new JPanel(new BorderLayout(3, 3));
      //view.add(toolBar, BorderLayout.NORTH);
      view.add(progressBar, BorderLayout.NORTH);
      view.add(filePanel, BorderLayout.CENTER);
    }

    private void makePopups() {

      PopupMenu varPopup = new PopupMenu(tree, "Options");
      varPopup.addAction("Make Partition", new AbstractAction() {
        public void actionPerformed(ActionEvent e) {
          cmdMakeIndex(currentNode);
        }
      });

      //////////////
      // toolbar

      JToolBar toolBar = new JToolBar();
      toolBar.setFloatable(false);  // mnemonics stop working in a floated toolbar

      /* JButton moveIndexButt = new JButton("Move");
      moveIndexButt.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent ae) {
          try {
            moveCdmIndexFile(currentNode);
          } catch (Throwable t) {
            showThrowable(t);
          }
          repaint();
        }
      });
      toolBar.add(moveIndexButt);

      JButton moveAllButt = new JButton("MoveAll");
      moveAllButt.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent ae) {
          try {
            moveCdmIndexAll(currentNode);
          } catch (Throwable t) {
            showThrowable(t);
          }
          repaint();
        }
      });
      toolBar.add(moveAllButt);

      JButton showFileButt = new JButton("Show");
      showFileButt.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent ae) {
          try {
            showCdmIndexFile(currentNode);
          } catch (Throwable t) {
            showThrowable(t);
          }
          repaint();
        }
      });
      toolBar.add(showFileButt);

      JButton showIndexButt = new JButton("Show Children");
      showIndexButt.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent ae) {
          try {
            showChildrenIndex(currentNode);
          } catch (Throwable t) {
            showThrowable(t);
          }
          repaint();
        }
      });
      toolBar.add(showIndexButt);

      JButton showPartButt = new JButton("Show Partitions");
      showPartButt.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent ae) {
          try {
            showPartitions(currentNode);
          } catch (Throwable t) {
            showThrowable(t);
          }
          repaint();
        }
      });
      toolBar.add(showPartButt);

      JButton makeIndexButt = new JButton("Make Partition");
      makeIndexButt.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent ae) {
          try {
            makeIndex(currentNode);
          } catch (Throwable t) {
            showThrowable(t);
          }
          repaint();
        }
      });
      toolBar.add(makeIndexButt); */

    }

    private void showThrowable(Throwable t) {
      t.printStackTrace();
      JOptionPane.showMessageDialog(
              DirectoryPartitionViewer.this,
              t.toString(),
              t.getMessage(),
              JOptionPane.ERROR_MESSAGE
      );
      repaint();
    }

    /**
     * Add the files that are contained within the directory of this node.
     * Thanks to Hovercraft Full Of Eels for the SwingWorker fix.
     */
    private void showChildren(final DefaultMutableTreeNode node) {
      tree.setEnabled(false);
      progressBar.setVisible(true);
      progressBar.setIndeterminate(true);

      final List<NodeInfo> result = new ArrayList<>(100);
      NodeInfo uobj = (NodeInfo) node.getUserObject();
      if (uobj.hasIndex) {
        for (NodeInfo child : uobj.getChildren()) {
          result.add(child);
        }

      } else {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(uobj.dir)) {
          for (Path child : stream) {
            if (Files.isDirectory(child))
              result.add(new NodeInfo(child));
          }
        } catch (IOException ioe) {
          ioe.printStackTrace();
        }
      }

      if (result.size() == 0) return;

      SwingWorker<Void, NodeInfo> worker = new SwingWorker<Void, NodeInfo>() {
        @Override
        public Void doInBackground() {
          for (NodeInfo child : result)
            publish(child);
          return null;
        }

        @Override
        protected void process(List<NodeInfo> chunks) {
          for (NodeInfo child : chunks) {
            node.add(new DefaultMutableTreeNode(child));
          }
        }

        @Override
        protected void done() {
          progressBar.setIndeterminate(false);
          //progressBar.setVisible(false);
          tree.setEnabled(true);
        }
      };

      worker.execute();
    }

    /**
     * Update the File details view with the details of this File.
     */
    private void setFileDetails(NodeInfo node) {
      BasicFileAttributes attr = null;
      try {
        attr = Files.readAttributes(node.dir, BasicFileAttributes.class);
      } catch (IOException e) {
        e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
      }

      nodeName.setIcon(attr.isDirectory() ? dirIcon : fileIcon);
      nodeName.setText(node.dir.toString());
      path.setText(node.dir.toString());
      date.setText(attr.lastModifiedTime().toString());
      size.setText(attr.size() + " bytes");
      readable.setSelected(Files.isReadable(node.dir));
      writable.setSelected(Files.isWritable(node.dir));
      isDirectory.setSelected(attr.isDirectory());

      repaint();
    }

    /**
     * A TreeCellRenderer for a File.
     */
    private class FileTreeCellRenderer extends DefaultTreeCellRenderer {

      private JLabel label;

      FileTreeCellRenderer() {
        label = new JLabel();
        label.setOpaque(true);
      }

      @Override
      public Component getTreeCellRendererComponent(
              JTree tree,
              Object value,
              boolean selected,
              boolean expanded,
              boolean leaf,
              int row,
              boolean hasFocus) {

        DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
        NodeInfo uobj = (NodeInfo) node.getUserObject();

        if (uobj != null) {
          label.setIcon(uobj.hasIndex ? fileIcon : dirIcon);
          label.setText(uobj.hasIndex ? uobj.part.getIndex().toString() : uobj.dir.toString());
        }

        if (selected) {
          label.setBackground(backgroundSelectionColor);
          label.setForeground(textSelectionColor);
        } else {
          label.setBackground(backgroundNonSelectionColor);
          label.setForeground(textNonSelectionColor);
        }

        return label;
      }
    }
  }

/////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  private class PartitionsTable extends JPanel {
    private PreferencesExt prefs;

    private BeanTableSorted groupsTable, groupTable, varTable, fileTable;
    private JSplitPane split, split2, split3;

    private IndependentWindow fileWindow;

    public PartitionsTable(PreferencesExt prefs, JPanel buttPanel) {
      this.prefs = prefs;

      PopupMenu varPopup;

      ////////////////
      groupsTable = new BeanTableSorted(GroupsBean.class, (PreferencesExt) prefs.node("GroupsBean"), false, "Groups", "GribCollection.GroupHcs", null);
      groupsTable.addListSelectionListener(new ListSelectionListener() {
        public void valueChanged(ListSelectionEvent e) {
          GroupsBean bean = (GroupsBean) groupsTable.getSelectedBean();
          if (bean != null)
            setGroups(bean);
        }
      });

      groupTable = new BeanTableSorted(GroupBean.class, (PreferencesExt) prefs.node("GroupBean"), false, "Partitions for this Group", "GribCollection.GroupHcs", null);
      groupTable.addListSelectionListener(new ListSelectionListener() {
        public void valueChanged(ListSelectionEvent e) {
          GroupBean bean = (GroupBean) groupTable.getSelectedBean();
          if (bean != null)
            setGroup(bean.group);
        }
      });

      varPopup = new PopupMenu(groupTable.getJTable(), "Options");
      varPopup.addAction("Show Files Used", new AbstractAction() {
        public void actionPerformed(ActionEvent e) {
          GroupBean bean = (GroupBean) groupTable.getSelectedBean();
          if (bean != null) {
            if (bean.group != null)
              showFiles(bean.group.getFiles());
          }
        }
      });
      varPopup.addAction("Show Variable Difference", new AbstractAction() {
        public void actionPerformed(ActionEvent e) {
          List<GroupBean> beans = (List<GroupBean>) groupTable.getSelectedBeans();
          if (beans.size() == 2) {
            Formatter f = new Formatter();
            showVariableDifferences(beans.get(0), beans.get(1), f);
            infoTA.setText(f.toString());
            infoTA.gotoTop();
            infoWindow.show();
          }
        }
      });

      varTable = new BeanTableSorted(VarBean.class, (PreferencesExt) prefs.node("Grib2Bean"), false, "Variables in group", "GribCollection.VariableIndex", null);

      varPopup = new PopupMenu(varTable.getJTable(), "Options");
      varPopup.addAction("Show Variable", new AbstractAction() {
        public void actionPerformed(ActionEvent e) {
          VarBean bean = (VarBean) varTable.getSelectedBean();
          if (bean != null) {
            infoTA.setText(bean.v.toStringComplete());
            infoTA.gotoTop();
            infoWindow.show();
          }
        }
      });

      fileTable = new BeanTableSorted(FileBean.class, (PreferencesExt) prefs.node("FileBean"), false, "Files", "Files", null);
      fileWindow = new IndependentWindow("Files Used", BAMutil.getImage("netcdfUI"), fileTable);
      fileWindow.setBounds((Rectangle) prefs.getBean("DetailWindowBounds", new Rectangle(300, 300, 500, 300)));

      /////////////////////////////////////////
      setLayout(new BorderLayout());

      split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, false, groupsTable, groupTable);
      split.setDividerLocation(prefs.getInt("splitPos2", 600));

      split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, false, split, varTable);
      split.setDividerLocation(prefs.getInt("splitPos", 900));
      add(split, BorderLayout.CENTER);
    }

    public void save() {
      groupsTable.saveState(false);
      groupTable.saveState(false);
      varTable.saveState(false);
      if (split != null) prefs.putInt("splitPos", split.getDividerLocation());
      if (split2 != null) prefs.putInt("splitPos2", split2.getDividerLocation());
      if (split3 != null) prefs.putInt("splitPos3", split3.getDividerLocation());
    }

    private void setGroups(GroupsBean groups) {
      groupTable.setBeans(groups.beans);
    }

    private void setGroup(GribCollection.GroupHcs group) {
      List<VarBean> vars = new ArrayList<>();
      for (GribCollection.VariableIndex v : group.varIndex)
        vars.add(new VarBean(v, group));
      varTable.setBeans(vars);
    }

    private void showFiles(List<MFile> files) {
      if (files == null) return;

      int count = 0;
      List<FileBean> beans = new ArrayList<>();
      for (MFile mfile : files)
        beans.add(new FileBean(mfile, count++));
      fileTable.setBeans(beans);
      fileWindow.show();
    }

    Map<String, GroupsBean> groupsBeans = new HashMap<>(25);

    void clear() {
      groupsBeans = new HashMap<>(25);
    }

    void addGribCollection(GribCollection gc) {
      for (GribCollection.GroupHcs g : gc.getGroups()) {
        GroupsBean bean = groupsBeans.get(g.getId());
        if (bean == null) {
          bean = new GroupsBean(g);
          groupsBeans.put(g.getId(), bean);
        }
        bean.addGroup(g, gc.getLocation());
      }
      groupsTable.setBeans(new ArrayList<>(groupsBeans.values()));
    }

    void showVariableDifferences(GroupBean bean1, GroupBean bean2, Formatter f) {
      f.format("Compare %s to %s%n", bean1.getPartition(), bean2.getPartition());
      for (GribCollection.VariableIndex var1 : bean1.group.varIndex) {
        if (bean2.group.findVariableByHash(var1.cdmHash) == null)
          f.format("Var1 %s missing in partition 2%n", var1.id());
      }
      for (GribCollection.VariableIndex var2 : bean2.group.varIndex) {
        if (bean1.group.findVariableByHash(var2.cdmHash) == null)
          f.format("Var2 %s missing in partition 1%n", var2.id());
      }
    }
  }

  private class RangeTracker {
    int min, max;

    RangeTracker(int value) {
      this.min = value;
      this.max = value;
    }

    void add(int value) {
      min = Math.min(min, value);
      max = Math.max(max, value);
    }

    public String toString() {
      Formatter f = new Formatter();
      if (min == max) f.format("%3d", min);
      else f.format("%3d - %3d", min, max);
      return f.toString();
    }
  }

  public class GroupsBean {
    GribCollection.GroupHcs group;
    List<GroupBean> beans = new ArrayList<>(50);
    RangeTracker nvars, nfiles, ntimes, nverts;

    public GroupsBean(GribCollection.GroupHcs g) {
      this.group = g;
      nvars = new RangeTracker(g.varIndex.size());
      nfiles = new RangeTracker(g.filenose.length);
      ntimes = new RangeTracker(g.timeCoords.size());
      nverts = new RangeTracker(g.vertCoords.size());
    }

    public GroupsBean() {
    }

    public void addGroup(GribCollection.GroupHcs g, String partitionName) {
      beans.add(new GroupBean(g, partitionName));
      nvars.add(g.varIndex.size());
      nfiles.add(g.filenose.length);
      ntimes.add(g.timeCoords.size());
      nverts.add(g.vertCoords.size());
    }

    public int getNPartitions() {
      return beans.size();
    }

    public String getGroupId() {
      return group.getId();
    }

    public int getGdsHash() {
      return group.gdsHash;
    }

    public String getNVars() {
      return nvars.toString();
    }

    public String getNFiles() {
      return nfiles.toString();
    }

    public String getNTimes() {
      return ntimes.toString();
    }

    public String getNVerts() {
      return nverts.toString();
    }

  }


  public class GroupBean {
    String partitionName;
    GribCollection.GroupHcs group;

    public GroupBean(GribCollection.GroupHcs g, String partitionName) {
      this.group = g;
      this.partitionName = partitionName;
    }

    public GroupBean() {
    }

    public String getGroupId() {
      return group.getId();
    }

    public int getGdsHash() {
      return group.gdsHash;
    }

    public int getNFiles() {
      return group.filenose.length;
    }

    public int getNTimes() {
      return group.timeCoords.size();
    }

    public int getNVerts() {
      return group.vertCoords.size();
    }

    public int getNVars() {
      return group.varIndex.size();
    }


    public String getPartition() {
      return partitionName;
    }

    void showFilesUsed(Formatter f) {
      List<MFile> files = group.getFiles();
      for (MFile file : files) {
        f.format(" %s%n", file.getName());
      }
    }

  }

////////////////////////////////////////////////////////////////////////////////////

  public class VarBean {
    GribCollection.VariableIndex v;
    GribCollection.GroupHcs group;

    public VarBean() {
    }

    public VarBean(GribCollection.VariableIndex v, GribCollection.GroupHcs group) {
      this.v = v;
      this.group = group;
    }

    public int getTimeCoord() {
      return v.timeIdx;
    }

    public boolean getTimeIntv() {
      return (v.getTimeCoord() == null) ? false : v.getTimeCoord().isInterval();
    }

    public boolean getVertLayer() {
      return (v.getVertCoord() == null) ? false : v.getVertCoord().isLayer();
    }

    public int getVertCoord() {
      return v.vertIdx;
    }

    public int getEnsCoord() {
      return v.ensIdx;
    }

    public int getLevelType() {
      return v.levelType;
    }

    public int getIntvType() {
      return v.intvType;
    }

    public int getProbType() {
      return v.probType;
    }

    public int getEnsType() {
      return v.ensDerivedType;
    }

    public int getGenType() {
      return v.genProcessType;
    }

    public String getIntvName() {
      return v.intvName;
    }

    public String getProbName() {
      return v.probabilityName;
    }

    public int getHash() {
      return v.cdmHash;
    }

    public String getGroupId() {
      return group.getId();
    }

    public String getVariableId() {
      return v.discipline + "-" + v.category + "-" + v.parameter;
    }
  }

  public class FileBean {
    MFile mfile;
    int count;

    public FileBean() {
    }

    public FileBean(MFile mfile, int count) {
      this.mfile = mfile;
      this.count = count;
    }

    public int getCount() {
      return count;
    }

    public String getName() {
      return mfile.getName();
    }

    public String getLastModified() {
      return CalendarDateFormatter.toDateTimeString(new Date(mfile.getLastModified()));
    }
  }
}
