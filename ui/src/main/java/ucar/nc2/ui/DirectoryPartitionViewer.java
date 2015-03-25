package ucar.nc2.ui;

import org.jdom2.input.SAXBuilder;
import org.jdom2.output.XMLOutputter;
import thredds.featurecollection.FeatureCollectionConfig;
import thredds.featurecollection.FeatureCollectionConfigBuilder;
import thredds.inventory.*;
import thredds.inventory.partition.DirectoryPartition;
import thredds.inventory.partition.DirectoryBuilder;
import ucar.coord.Coordinate;
import ucar.nc2.grib.collection.GribCdmIndex;
import ucar.nc2.grib.collection.GribCollectionMutable;
import ucar.nc2.grib.collection.PartitionCollectionMutable;
import ucar.nc2.ui.widget.*;
import ucar.nc2.ui.widget.PopupMenu;
import ucar.unidata.util.StringUtil2;
import ucar.util.prefs.PreferencesExt;
import ucar.util.prefs.ui.BeanTable;
import ucar.util.prefs.ui.ComboBox;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
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
  private String collectionName;
  private CdmIndex3Panel cdmIndexTables;
  private PartitionsTable partitionsTable;

  private JPanel tablePanel;
  private JSplitPane mainSplit;

  private PartitionTreeBrowser partitionTreeBrowser;
  private MFileTable fileTable;

  private TextHistoryPane infoTA;
  private IndependentWindow infoWindow;
  private ComboBox cb;
  private FileManager dirFileChooser;
  private boolean isFromIndex;

  public DirectoryPartitionViewer(PreferencesExt prefs, JPanel topPanel, JPanel buttPanel) {
    this.prefs = prefs;
    partitionTreeBrowser = new PartitionTreeBrowser();
    partitionsTable = new PartitionsTable((PreferencesExt) prefs.node("partTable"));

    cdmIndexTables = new CdmIndex3Panel((PreferencesExt) prefs.node("cdmIdx"), buttPanel);
    cdmIndexTables.addPropertyChangeListener(new PropertyChangeListener() {
      public void propertyChange(PropertyChangeEvent evt) {
        firePropertyChange(evt.getPropertyName(), evt.getOldValue(), evt.getNewValue());
      }
    });

    if (topPanel != null && buttPanel != null) {

      cb = new ComboBox(prefs);
      cb.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          String filename = (String) cb.getSelectedItem();
          if (filename == null) return;
          File d = new File( filename);
          if (d.isDirectory()) {
            setDirectory(d);
          } else if (d.getName().endsWith(".xml")) {
            setCollectionFromConfig(d.getPath());
          } else if (d.getName().endsWith(CollectionAbstract.NCX_SUFFIX)) {
            setCollectionFromIndex(d.getPath());
          }
          cb.addItem(filename);
        }
      });
      topPanel.add(new JLabel("dir,ncx3,or config:"), BorderLayout.WEST);
      topPanel.add(cb, BorderLayout.CENTER);

      // a file chooser that can choose a directory
      dirFileChooser = new FileManager(null, null, null, (PreferencesExt) prefs.node("fileChooser"));
      dirFileChooser.getFileChooser().setFileSelectionMode( JFileChooser.FILES_AND_DIRECTORIES);
      AbstractAction dirFileAction = new AbstractAction() {
        public void actionPerformed(ActionEvent e) {
          String filename = dirFileChooser.chooseFilename();
          if (filename == null) return;
          cb.setSelectedItem(filename);
        }
      };
      BAMutil.setActionProperties(dirFileAction, "FileChooser", "choose file or directory...", false, 'L', -1);
      BAMutil.addActionToContainer(buttPanel, dirFileAction);

      /* AbstractButton infoButton = BAMutil.makeButtcon("Information", "Show Info", false);
      infoButton.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          showInfo();
        }
      });
      buttPanel.add(infoButton);

      AbstractButton info2Button = BAMutil.makeButtcon("Information", "Show Detail Info", false);
      info2Button.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          showDetailInfo();
        }
      });
      buttPanel.add(info2Button); */
    }

    setLayout(new BorderLayout());

    JScrollPane treeScroll = new JScrollPane(partitionTreeBrowser.tree);
    JPanel treePanel = new JPanel(new BorderLayout());
    treePanel.add(treeScroll, BorderLayout.CENTER);
    treePanel.add(partitionTreeBrowser.view, BorderLayout.SOUTH);

    tablePanel = new JPanel(new BorderLayout());
    tablePanel.add(cdmIndexTables, BorderLayout.CENTER);
    current = cdmIndexTables;

    mainSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, false, treePanel, tablePanel);
    mainSplit.setDividerLocation(prefs.getInt("mainSplit", 100));
    add(mainSplit, BorderLayout.CENTER);

    // the info window
    infoTA = new TextHistoryPane();
    infoWindow = new IndependentWindow("Information", BAMutil.getImage("netcdfUI"), infoTA);
    infoWindow.setBounds((Rectangle) prefs.getBean("InfoWindowBounds", new Rectangle(300, 300, 500, 300)));

    // file popup window
    fileTable = new MFileTable((PreferencesExt) prefs.node("MFileTable"), true);
    fileTable.addPropertyChangeListener(new PropertyChangeListener() {
      public void propertyChange(PropertyChangeEvent evt) {
        firePropertyChange(evt.getPropertyName(), evt.getOldValue(), evt.getNewValue());
      }
    });
  }

  public void save() {
    if (mainSplit != null) prefs.putInt("mainSplit", mainSplit.getDividerLocation());
    if (cb != null) cb.save();
    if (dirFileChooser != null) dirFileChooser.save();

    cdmIndexTables.save();
    partitionsTable.save();
    prefs.putBeanObject("InfoWindowBounds", infoWindow.getBounds());
    fileTable.save();
  }


  public void clear() {
    cdmIndexTables.clear();
    partitionsTable.clear();
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

  public void showInfo() {
    Formatter f = new Formatter();
    if (current == partitionsTable) {
      partitionsTable.showGroupDiffs(f);
    } else if (current == cdmIndexTables) {
      cdmIndexTables.showInfo(f);
    } else {
      return;
    }

    infoTA.setText(f.toString());
    infoTA.gotoTop();
    infoWindow.show();
  }

  public void showDetailInfo() {
    Formatter f = new Formatter();
    if (current == partitionsTable) {
      partitionsTable.showGroupDiffs(f);
    } else if (current == cdmIndexTables) {
      cdmIndexTables.showInfo(f);
    } else {
      return;
    }

    infoTA.setText(f.toString());
    infoTA.gotoTop();
    infoWindow.show();
  }


  ////////////////////////////////////////////////

  // top directory
  private void setDirectory(File dir) {
    config = new FeatureCollectionConfig();
    if (dir.isDirectory()) {
      partitionTreeBrowser.setRoot(dir.toPath());
    }
  }

  // feature collection config
  private void setCollectionFromConfig(String name) {
    Path f = Paths.get(name);
    if (!Files.exists(f)) return;

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
    FeatureCollectionConfigBuilder builder = new FeatureCollectionConfigBuilder(errlog);
    config = builder.readConfig(doc.getRootElement());
    CollectionSpecParser spec = new CollectionSpecParser(config.spec, errlog);
    partitionTreeBrowser.setRoot(Paths.get(spec.getRootDir()));
  }

  // ncx2 index
  private void setCollectionFromIndex(String indexFilename) {
    isFromIndex = true;
    config = new FeatureCollectionConfig(); // LOOK !!

    File indexFile = new File(indexFilename);
    File parentFile = indexFile.getParentFile();

    // whats the bloody collection name?
    String name = indexFile.getName();
    String dirName = parentFile.getName();
    name = StringUtil2.removeFromEnd(name, CollectionAbstract.NCX_SUFFIX);
    name = StringUtil2.removeFromEnd(name, dirName);
    name = StringUtil2.removeFromEnd(name, "-");
    this.collectionName = name;

    setDirectory(parentFile);
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
  } */


  private void cmdSummarizePartitions(final NodeInfo node) {
    // long running task in background thread
    Thread background = new Thread() {
       public void run() {
         Formatter out = new Formatter();
         GribCdmIndex indexReader = new GribCdmIndex(logger);
         final DirectoryPartition dpart = new DirectoryPartition(config, node.dir, true, indexReader, logger);
         dpart.putAuxInfo(FeatureCollectionConfig.AUX_CONFIG, config);

         try (PartitionCollectionMutable tp = (PartitionCollectionMutable)  GribCdmIndex.openMutableGCFromIndex(dpart.getIndexFilename(), config, false, true, logger)) {
           if (tp == null) return;

           for (MCollection dcmp : dpart.makePartitions(null)) {
             dcmp.putAuxInfo(FeatureCollectionConfig.AUX_CONFIG, config);
             tp.addPartition(dcmp);
           }

           final List<GribCollectionMutable> gclist = new ArrayList<>();
           for (PartitionCollectionMutable.Partition tpp : tp.getPartitions()) {
             if (tpp.isBad()) continue;
             try ( GribCollectionMutable gc = tpp.makeGribCollection()) {    // use index if it exists
               if (gc != null)
                 gclist.add(gc);

             } catch (Throwable t) {
               t.printStackTrace();
               out.format("Failed to open partition %s%n", tpp.getName());
               logger.error(" Failed to open partition " + tpp.getName(), t);
             }
           }

           // switch back to Swing event thread to manipulate GUI
           SwingUtilities.invokeLater(new Runnable() {
               public void run() {
                 partitionsTable.clear();
                 partitionsTable.setHeader(dpart.getCollectionName());
                 for (GribCollectionMutable gc : gclist)
                   partitionsTable.addGribCollection(gc);
                 swap(partitionsTable);
               }
             }
           );

         } catch (Throwable t) {
           t.printStackTrace();
           JOptionPane.showMessageDialog(DirectoryPartitionViewer.this, node.dir + " showPartitions failed: " + t.getMessage());
         }
       }
     };

    background.start();
  }

  private void cmdShowIndex(NodeInfo node) {
    try {
      // this opens the index file and constructs a GribCollection
      Path index = node.part.getIndex();
      if (index == null) {
        node.part.findIndex();
      }

      cdmIndexTables.setIndexFile(node.part.getIndex(), config);
      swap(cdmIndexTables);

    } catch (Throwable t) {
      t.printStackTrace();
      JOptionPane.showMessageDialog(this, node + " showIndex failed: " + t.getMessage());
    }
  }

  private void cmdMakeIndex(NodeInfo node) {
    Formatter out = new Formatter();
    out.format("makeTimePartitionIndex %s%n%n", node);
    try {
      boolean ok = GribCdmIndex.makeIndex(config, out, node.dir);
      out.format("makeTimePartitionIndex success %s%n%n", ok);
      infoTA.setText(out.toString());
      infoTA.gotoTop();
      infoWindow.show();

      node.refresh();
      // cmdShowIndex(node);

    } catch (Throwable t) {
      t.printStackTrace();
      JOptionPane.showMessageDialog(this, node + " makeIndex failed: " + t.getMessage());
    }
  }

  private class NodeInfo {
    Path dir;
    DirectoryBuilder part;
    boolean hasIndex;
    boolean isPartition = true;

    NodeInfo(Path dir) {
      this.dir = dir;

      try {
        part = new DirectoryBuilder(collectionName, dir, null);
        hasIndex = part.getIndex() != null;

      } catch (IOException e) {
        e.printStackTrace();
      }
    }

    NodeInfo(DirectoryBuilder part) {
      this.part = part;
      this.dir = part.getDir();
      this.hasIndex = part.getIndex() != null;
    }

    List<NodeInfo> getChildren() {
      return (isFromIndex) ? getChildrenFromIndex() : getChildrenFromScan();
    }

    private List<NodeInfo> getChildrenFromIndex() {
      List<NodeInfo> result = new ArrayList<>(100);
      try {
        for (DirectoryBuilder child : part.constructChildrenFromIndex(new GribCdmIndex(logger), true))
          result.add(new NodeInfo(child));

        isPartition = (result.size() > 0);

      } catch (IOException e) {
        e.printStackTrace();
      }

      return result;
    }

    private List<NodeInfo> getChildrenFromScan() {
      List<NodeInfo> result = new ArrayList<>(100);
      try {
        for (DirectoryBuilder child : part.constructChildren(new GribCdmIndex(logger), CollectionUpdateType.test))
          result.add(new NodeInfo(child));

        isPartition = (result.size() > 0);

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
      Formatter f = new Formatter();
      f.format("NodeInfo{ dir= %s", dir);
      if (hasIndex) f.format(" index= %s", part.getIndex());
      f.format("}");
      return f.toString();
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
      varPopup.addAction("Make Index(es)", new AbstractAction() {
        @Override
        public void actionPerformed(ActionEvent e) {
          TreePath[] selectionPaths = tree.getSelectionPaths();
          // JTree.getSelectionPaths() returns null instead of an empty array. Dumb.
          if (selectionPaths == null) {
            selectionPaths = new TreePath[0];
          }

          for (TreePath path : selectionPaths) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
            cmdMakeIndex( (NodeInfo) node.getUserObject());
          }
        }
      });

      varPopup.addAction("Show Index", new AbstractAction() {
        public void actionPerformed(ActionEvent e) {
          cmdShowIndex(currentNode);
        }
      });

      varPopup.addAction("Summarize Children Partitions", new AbstractAction() {
        public void actionPerformed(ActionEvent e) {
          // do this in another Thread
          new Thread() {
            public void run() {
              cmdSummarizePartitions(currentNode);
            }
          }.start();
        }
      });

       //////////////
      // toolbar

      //JToolBar toolBar = new JToolBar();
      //toolBar.setFloatable(false);  // mnemonics stop working in a floated toolbar

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
      if (uobj.hasIndex && uobj.isPartition) {
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

      if (result.size() == 0) {
        progressBar.setIndeterminate(false);
        tree.setEnabled(true);
        return;
      }

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
      BasicFileAttributes attr;
      try {
        attr = Files.readAttributes(node.dir, BasicFileAttributes.class);
      } catch (IOException e) {
        logger.warn("An I/O error occurred.", e);
        return;
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

    private BeanTable groupsTable, groupTable, varTable;
    private JSplitPane split;

    public PartitionsTable(PreferencesExt prefs) {
      this.prefs = prefs;

      PopupMenu varPopup;

      ////////////////
      groupsTable = new BeanTable(GroupsBean.class, (PreferencesExt) prefs.node("GroupsBean"), false, "Groups", "GribCollection.GroupHcs", null);
      groupsTable.addListSelectionListener(new ListSelectionListener() {
        public void valueChanged(ListSelectionEvent e) {
          GroupsBean bean = (GroupsBean) groupsTable.getSelectedBean();
          if (bean != null)
            setGroups(bean);
        }
      });

      groupTable = new BeanTable(GroupBean.class, (PreferencesExt) prefs.node("GroupBean"), false, "Partitions for this Group", "GribCollection.GroupGC", null);
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
              showFiles(null, bean.group);
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

      varTable = new BeanTable(VarBean.class, (PreferencesExt) prefs.node("Grib2Bean"), false, "Variables in group", "GribCollection.VariableIndex", null);

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
    }

    private void setHeader(String header) {
      groupsTable.setHeader(header);
    }

    private void setGroups(GroupsBean groups) {
      groupTable.setBeans(groups.beans);
    }

    private void setGroup(GribCollectionMutable.GroupGC group) {
      List<VarBean> vars = new ArrayList<>();
      for (GribCollectionMutable.VariableIndex v : group.getVariables())
        vars.add(new VarBean(v, group));
      varTable.setBeans(vars);
    }

   // private void showFiles(File dir, List<MFile> files) {
   //   fileTable.setFiles(dir, files);
   // }

    private void showFiles(GribCollectionMutable gc, GribCollectionMutable.GroupGC group) {
      Collection<MFile> files = (group == null) ? gc.getFiles() : group.getFiles();
      File dir = (gc == null) ? null : gc.getDirectory();
      fileTable.setFiles(dir, files);
    }

    private class Groups implements Comparable<Groups> {
      String groupId;
      SortedSet<String> parts = new TreeSet<>();

      private Groups(String id) {
        this.groupId = id;
      }

      @Override
      public int compareTo(Groups o2) {
        return groupId.compareTo(o2.groupId);
      }
    }

    void showGroupDiffs(Formatter out) {
      Map<Integer, Groups> map = new HashMap<>(100);

      for (GroupsBean gsbean : groupsBeans.values()) {
        for (GroupBean gbean : gsbean.beans) {
          Groups gs = map.get(gbean.getGdsHash());
          if (gs == null) {
            gs = new Groups(gsbean.getGroupId());
            map.put(gbean.getGdsHash(), gs);
          }
          gs.parts.add(gbean.getPartition());
        }
      }

      List<Groups> groups = new ArrayList<>(map.values());
      Collections.sort(groups);

      out.format("=======================Groups%n");
      for (Groups gs : groups) {
        if (gs.parts.size() < partitionsAll.size()) {
          out.format("Missing partitions for group %s:%n", gs.groupId);
          for (String p : partitionsAll) {
            if (!gs.parts.contains(p))
              out.format("   %s%n", p);
          }
        }
      }

      out.format("=======================Partitions%n");
      for (String p : partitionsAll) {
        out.format("%s Missing groups:%n", p);
        for (Groups gs : groups) {
            if (!gs.parts.contains(p))
              out.format("   %s%n", gs.groupId);
          }
        }

    }

    Map<String, GroupsBean> groupsBeans = new HashMap<>(50);
    SortedSet<String> partitionsAll = new TreeSet<>();
    void clear() {
      groupsBeans = new HashMap<>(50);
      partitionsAll = new TreeSet<>();
    }

    void addGribCollection(GribCollectionMutable gc) {
      String partitionName = gc.getLocation();
      int pos = partitionName.lastIndexOf("/");
      if (pos < 0) pos = partitionName.lastIndexOf("\\");
      if (pos > 0) partitionName = partitionName.substring(0, pos);
      partitionsAll.add(partitionName);

      for (GribCollectionMutable.Dataset ds : gc.getDatasets()) {
        for (GribCollectionMutable.GroupGC g : ds.getGroups()) {
          GroupsBean bean = groupsBeans.get(g.getId());
          if (bean == null) {
            bean = new GroupsBean(g);
            groupsBeans.put(g.getId(), bean);
          }
          bean.addGroup(g, ds.gctype.toString(), partitionName);
        }
      }
      groupsTable.setBeans(new ArrayList<>(groupsBeans.values()));
    }

    void showVariableDifferences(GroupBean bean1, GroupBean bean2, Formatter f) {
      f.format("Compare %s to %s%n", bean1.getPartition(), bean2.getPartition());
      for (GribCollectionMutable.VariableIndex var1 : bean1.group.getVariables()) {
        if (bean2.group.findVariableByHash(var1) == null)
          f.format("Var1 %s missing in partition 2%n", var1.id());
      }
      for (GribCollectionMutable.VariableIndex var2 : bean2.group.getVariables()) {
        if (bean1.group.findVariableByHash(var2) == null)
          f.format("Var2 %s missing in partition 1%n", var2.id());
      }
    }
  }

  private static class RangeTracker {
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
    GribCollectionMutable.GroupGC group;
    List<GroupBean> beans = new ArrayList<>(50);
    RangeTracker nvars, nfiles, ncoords, ntimes;

    public GroupsBean(GribCollectionMutable.GroupGC g) {
      this.group = g;
      nvars = new RangeTracker(count(g.getVariables().iterator()));
      nfiles = new RangeTracker(g.getNFiles());
      ncoords = new RangeTracker(count(g.getCoordinates().iterator()));
      ntimes = new RangeTracker(countTimes(g.getCoordinates().iterator()));
    }

    public GroupsBean() {
    }

    private int count(Iterator iter) {
      int count = 0;
      while (iter.hasNext()) {
        iter.next();
        count++;
      }
      return count;
    }

    private int countTimes(Iterator<Coordinate> iter) {
      int count = 0;
      while (iter.hasNext()) {
        Coordinate c = iter.next();
        if (c.getType() == Coordinate.Type.time || c.getType() == Coordinate.Type.timeIntv || c.getType() == Coordinate.Type.time2D)
          count++;
      }
      return count;
    }

    public void addGroup(GribCollectionMutable.GroupGC g, String dataset, String partitionName) {
      beans.add(new GroupBean(g, dataset, partitionName));
      nvars.add(count(g.getVariables().iterator()));
      nfiles.add(g.getNFiles());
      ncoords.add(count(g.getCoordinates().iterator()));
      ntimes.add(countTimes(g.getCoordinates().iterator()));
    }

    public int getNPartitions() {
      return beans.size();
    }

    public String getGroupId() {
      return group.getId();
    }

    public int getGdsHash() {
      return group.getGdsHash().hashCode();
    }

    public String getNVars() {
      return nvars.toString();
    }

    public String getNFiles() {
      return nfiles.toString();
    }

    public String getNCoords() {
      return ncoords.toString();
    }

    public String getNTimes() {
      return ntimes.toString();
    }

  }


  public class GroupBean {
    String partitionName;
    String datasetName;
    GribCollectionMutable.GroupGC group;

    public GroupBean(GribCollectionMutable.GroupGC g, String datasetName, String partitionName) {
      this.group = g;
      this.partitionName = partitionName;
      this.datasetName = datasetName;
    }

    public GroupBean() {
    }

    public String getGroupId() {
      return group.getId();
    }

    public int getGdsHash() {
      return group.getGdsHash().hashCode();
    }

    public int getNFiles() {
      return group.getNFiles();
    }

    /* public int getNTimes() {
      return group.timeCoords.size();
    }

    public int getNVerts() {
      return group.vertCoords.size();
    }

    public int getNVars() {
      return group.varIndex.size();
    } */


    public String getPartition() {
      return partitionName;
    }

    public String getDataset() {
      return datasetName;
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
    GribCollectionMutable.VariableIndex v;
    GribCollectionMutable.GroupGC group;

    public VarBean() {
    }

    public VarBean(GribCollectionMutable.VariableIndex v, GribCollectionMutable.GroupGC group) {
      this.v = v;
      this.group = group;
    }

    public String getTimeCoord() {
      if (0 <= v.getCoordinateIdx(Coordinate.Type.time)) return Coordinate.Type.time.toString();
      if (0 <= v.getCoordinateIdx(Coordinate.Type.timeIntv)) return Coordinate.Type.timeIntv.toString();
      if (0 <= v.getCoordinateIdx(Coordinate.Type.time2D)) return Coordinate.Type.time2D.toString();
      return "ERR";
    }

    public int getVertCoord() {
      return v.getCoordinateIdx(Coordinate.Type.vert);
    }

    public int getEnsCoord() {
      return v.getCoordinateIdx(Coordinate.Type.ens);
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
      return v.getTimeIntvName();
    }

    public String getProbName() {
      return v.probabilityName;
    }

    public int getHash() {
      return v.hashCode();
    }

    public String getGroupId() {
      return group.getId();
    }

    public String getVariableId() {
      return v.discipline + "-" + v.category + "-" + v.parameter;
    }
  }
}
