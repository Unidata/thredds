package ucar.nc2.ui;

import ucar.ma2.Array;
import ucar.nc2.*;
import ucar.nc2.grib.GribCdmIndex;
import ucar.nc2.grib.GribCollection;
import ucar.nc2.ui.event.ActionValueEvent;
import ucar.nc2.ui.event.ActionValueListener;
import ucar.nc2.ui.widget.*;
import ucar.nc2.util.ListenerManager;
import ucar.util.prefs.PreferencesExt;
import ucar.util.prefs.ui.BeanTableSorted;
import ucar.util.prefs.ui.Debug;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.filechooser.FileSystemView;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import java.awt.*;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.Formatter;
import java.util.List;

/**
 * GribCollection Directory Partition Viewer
 *
 * @author caron
 * @since 11/9/13
 */
public class DirectoryPartitionViewer extends JPanel {
  static private final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(DirectoryPartitionViewer.class);

  private FileManager fileChooser;

  private PreferencesExt prefs;
  private NetcdfFile ds;

  private List<NestedTable> nestedTableList = new ArrayList<NestedTable>();
  private BeanTableSorted attTable;

  private JPanel tablePanel;
  private JSplitPane mainSplit;

  private JComponent currentComponent;
  private PartitionTreeBrowser fileBrowser;
  private NCdumpPane dumpPane;

  private TextHistoryPane infoTA;
  private StructureTable dataTable;
  private IndependentWindow infoWindow, dataWindow, dumpWindow, attWindow;

  public DirectoryPartitionViewer(PreferencesExt prefs, FileManager fileChooser) {
    this.prefs = prefs;
    this.fileChooser = fileChooser;
    fileBrowser = new PartitionTreeBrowser();

    setLayout(new BorderLayout());

    tablePanel = new JPanel(new BorderLayout());
    setNestedTable(0, null);
    tablePanel.add(fileBrowser.fileView, BorderLayout.NORTH);

    JScrollPane treeScroll = new JScrollPane(fileBrowser.tree);

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
  }

  ////////////////////////////////////////////////

  public void showDetail(Formatter f) {
  }

  public void setCollection(String name) {
    File f = new File(name);
    if (!f.exists()) return;
    if (!f.isDirectory()) return;

    fileBrowser.setRoot(f);
  }

  private void moveCdmIndexFile(File indexFile) throws IOException {
    GribCollection gc = null;
    try {
      boolean ok = GribCdmIndex.moveCdmIndex(indexFile, logger);
      Formatter f = new Formatter();
      f.format("moved success=%s", ok);
      infoTA.setText(f.toString());
      infoTA.gotoTop();
      infoWindow.show();
    } finally {
      if (gc != null) gc.close();
    }
  }

  private void showCdmIndexFile(File indexFile) throws IOException {
    GribCollection gc = null;
    try {
      gc = GribCdmIndex.openCdmIndex(indexFile.getPath(), logger);
      Formatter f = new Formatter();
      gc.showIndex(f);
      infoTA.setText(f.toString());
      infoTA.gotoTop();
      infoWindow.show();
    } finally {
      if (gc != null) gc.close();
    }
  }


  private void setSelected(Variable v) {

    List<Variable> vchain = new ArrayList<Variable>();
    vchain.add(v);

    Variable vp = v;
    while (vp.isMemberOfStructure()) {
      vp = vp.getParentStructure();
      vchain.add(0, vp); // reverse
    }

    for (int i = 0; i < vchain.size(); i++) {
      vp = vchain.get(i);
      NestedTable ntable = setNestedTable(i, vp.getParentStructure());
      ntable.setSelected(vp);
    }

  }

  private NestedTable setNestedTable(int level, Structure s) {
    NestedTable ntable;
    if (nestedTableList.size() < level + 1) {
      ntable = new NestedTable(level);
      nestedTableList.add(ntable);

    } else {
      ntable = nestedTableList.get(level);
    }

    if (s != null) // variables inside of records
      ntable.table.setBeans(getStructureVariables(s));

    ntable.show();
    return ntable;
  }

  private void hideNestedTable(int level) {
    int n = nestedTableList.size();
    for (int i = n - 1; i >= level; i--) {
      NestedTable ntable = nestedTableList.get(i);
      ntable.hide();
    }
  }

  private class NestedTable {
    int level;
    PreferencesExt myPrefs;

    BeanTableSorted table; // always the left component
    JSplitPane split = null; // right component (if exists) is the nested dataset.
    int splitPos = 100;
    boolean isShowing = false;

    NestedTable(int level) {
      this.level = level;
      myPrefs = (PreferencesExt) prefs.node("NestedTable" + level);

      table = new BeanTableSorted(VariableBean.class, myPrefs, false);

      JTable jtable = table.getJTable();
      ucar.nc2.ui.widget.PopupMenu csPopup = new ucar.nc2.ui.widget.PopupMenu(jtable, "Options");
      csPopup.addAction("Show Declaration", new AbstractAction() {
        public void actionPerformed(ActionEvent e) {
          showDeclaration(table, false);
        }
      });
      csPopup.addAction("Show NcML", new AbstractAction() {
        public void actionPerformed(ActionEvent e) {
          showDeclaration(table, true);
        }
      });
      csPopup.addAction("NCdump Data", "Dump", new AbstractAction() {
        public void actionPerformed(ActionEvent e) {
          dumpData(table);
        }
      });
      if (level == 0) {
        csPopup.addAction("Data Table", new AbstractAction() {
          public void actionPerformed(ActionEvent e) {
            dataTable(table);
          }
        });
      }

      // get selected variable, see if its a structure
      table.addListSelectionListener(new ListSelectionListener() {
        public void valueChanged(ListSelectionEvent e) {
          Variable v = getCurrentVariable(table);
          if ((v != null) && (v instanceof Structure)) {
            hideNestedTable(NestedTable.this.level + 2);
            setNestedTable(NestedTable.this.level + 1, (Structure) v);

          } else {
            hideNestedTable(NestedTable.this.level + 1);
          }
          //if (eventsOK) datasetTree.setSelected( v);
        }
      });

      // layout
      if (currentComponent == null) {
        currentComponent = table;
        tablePanel.add(currentComponent, BorderLayout.CENTER);
        isShowing = true;

      } else {
        split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, false, currentComponent, table);
        splitPos = myPrefs.getInt("splitPos" + level, 500);
        if (splitPos > 0)
          split.setDividerLocation(splitPos);

        show();
      }
    }

    void show() {
      if (isShowing) return;

      tablePanel.remove(currentComponent);
      split.setLeftComponent(currentComponent);
      split.setDividerLocation(splitPos);
      currentComponent = split;
      tablePanel.add(currentComponent, BorderLayout.CENTER);
      tablePanel.revalidate();
      isShowing = true;
    }

    void hide() {
      if (!isShowing) return;
      tablePanel.remove(currentComponent);

      if (split != null) {
        splitPos = split.getDividerLocation();
        currentComponent = (JComponent) split.getLeftComponent();
        tablePanel.add(currentComponent, BorderLayout.CENTER);
      }

      tablePanel.revalidate();
      isShowing = false;
    }

    void setSelected(Variable vs) {

      List beans = table.getBeans();
      for (Object bean1 : beans) {
        VariableBean bean = (VariableBean) bean1;
        if (bean.vs == vs) {
          table.setSelectedBean(bean);
          return;
        }
      }
    }

    void saveState() {
      table.saveState(false);
      if (split != null) myPrefs.putInt("splitPos" + level, split.getDividerLocation());
    }
  }

  /* public void showTreeViewWindow() {
    if (treeWindow == null) {
      datasetTree = new DatasetTreeView();
      treeWindow = new IndependentWindow("TreeView", datasetTree);
      treeWindow.setIconImage(thredds.ui.BAMutil.getImage("netcdfUI"));
      treeWindow.setBounds( (Rectangle) prefs.getBean("treeWindow", new Rectangle( 150, 100, 400, 700)));
    }

    datasetTree.setDataset( ds);
    treeWindow.show();
  } */

  private void showDeclaration(BeanTableSorted from, boolean isNcml) {
    Variable v = getCurrentVariable(from);
    if (v == null) return;
    infoTA.clear();
    if (isNcml) {
      Formatter out = new Formatter();
      try {
        NCdumpW.writeNcMLVariable(v, out);
      } catch (IOException e) {
        e.printStackTrace();
      }
      infoTA.appendLine(out.toString());

    } else {
      infoTA.appendLine(v.toString());
    }

    if (Debug.isSet("Xdeveloper")) {
      infoTA.appendLine("\n");
      infoTA.appendLine("FULL NAME = " + v.getFullName());
      infoTA.appendLine("\n");
      infoTA.appendLine(v.toStringDebug());
    }
    infoTA.gotoTop();
    infoWindow.setTitle("Variable Info");
    infoWindow.show();
  }

  private void dumpData(BeanTableSorted from) {
    Variable v = getCurrentVariable(from);
    if (v == null) return;

    dumpPane.clear();
    String spec;

    try {
      spec = ParsedSectionSpec.makeSectionSpecString(v, null);
      dumpPane.setContext(ds, spec);

    } catch (Exception ex) {
      StringWriter s = new StringWriter();
      ex.printStackTrace(new PrintWriter(s));
      dumpPane.setText(s.toString());
    }

    dumpWindow.show();
  }

  /* private void showMissingData(BeanTableSorted from) {
    VariableBean vb = (VariableBean) from.getSelectedBean();
    if (vb == null) return;
    Variable v = vb.vs;
    if ((v != null) && (v.getDataType() == ucar.nc2.DataType.STRUCTURE)) {
      showMissingStructureData( (Structure) v);
    }
    if (!vb.vs.hasMissing()) return;

    int count = 0, total = 0;
    infoTA.clear();
    infoTA.appendLine( v.toString());
    try {

      Array data = null;
      if (v.isMemberOfStructure())
        data = v.readAllStructures((List)null, true);
      else
        data = v.read();

      IndexIterator iter = data.getIndexIterator();
      while (iter.hasNext()) {
        if (vb.vs.isMissing( iter.getDoubleNext()))
          count++;
        total++;
      }

      double p = ((100.0 * count) / total);
      infoTA.appendLine( " missing values = "+count);
      infoTA.appendLine( " total values = "+total);
      infoTA.appendLine( " percent missing values = "+ Format.d(p, 2) +" %");

    } catch( InvalidRangeException e ) {
      infoTA.appendLine( "ERROR= "+e.getMessage());
    } catch( IOException ioe ) {
      infoTA.appendLine( "ERROR= "+ioe.getMessage());
    }
    infoTA.gotoTop();
    infoWindow.showIfNotIconified();
  }

  private void showMissingStructureData(Structure s) {
    ArrayList members = new ArrayList();
    List allMembers = s.getVariables();
    for (int i=0; i<allMembers.size(); i++) {
      Variable vs = (Variable) allMembers.get(i);
      if (vs.hasMissing())
        members.add( vs);
    }

    if (members.size() == 0) return;
    int[] count = new int[ members.size()];
    int[] total = new int[ members.size()];

    infoTA.clear();
    try {

     Structure.Iterator iter = s.getStructureIterator();
     while (iter.hasNext()) {
       StructureData sdata = iter.next();

       for (int i=0; i<members.size(); i++) {
         Variable vs = (Variable) members.get(i);

         Array data = sdata.findMemberArray( vs.getShortName());
         IndexIterator dataIter = data.getIndexIterator();
         while (dataIter.hasNext()) {
           if (vs.isMissing(dataIter.getDoubleNext()))
             count[i]++;
           total[i]++;
         }
       }
     }
     int countAll = 0, totalAll = 0;
     infoTA.appendLine("      name                missing   total     percent missing");
     for (int i=0; i<members.size(); i++) {
       Variable vs = (Variable) members.get(i);
       double p = ( (100.0 * count[i]) / total[i]);
       infoTA.appendLine(Format.s(vs.getShortName(), 25) +
                         " "+ Format.i(count[i], 7) +
                         "   "+ Format.i(total[i], 7) +
                         "   "+ Format.d(p, 2) + "%");
       countAll += count[i];
       totalAll += total[i];
     }

     infoTA.appendLine("");
     double p = ( (100.0 * countAll) / totalAll);
     infoTA.appendLine(Format.s("TOTAL ALL", 25) +
                       " "+ Format.i(countAll, 7) +
                       "   "+ Format.i(totalAll, 7) +
                       "   "+ Format.d(p, 2) + "%");

    } catch( IOException ioe ) {
      infoTA.appendLine( "ERROR= "+ioe.getMessage());
    }
    infoTA.gotoTop();
    infoWindow.showIfNotIconified();
  } */

  private void dataTable(BeanTableSorted from) {
    VariableBean vb = (VariableBean) from.getSelectedBean();
    if (vb == null) return;
    Variable v = vb.vs;
    if (v instanceof Structure) {
      try {
        dataTable.setStructure((Structure) v);
      } catch (Exception ex) {
        ex.printStackTrace();
      }
    } else return;

    dataWindow.show();
  }

  private Variable getCurrentVariable(BeanTableSorted from) {
    VariableBean vb = (VariableBean) from.getSelectedBean();
    if (vb == null) return null;
    return vb.vs;
  }

  public List<VariableBean> getVariableBeans(NetcdfFile ds) {
    List<VariableBean> vlist = new ArrayList<VariableBean>();
    for (Variable v : ds.getVariables()) {
      vlist.add(new VariableBean(v));
    }
    return vlist;
  }

  public List<VariableBean> getStructureVariables(Structure s) {
    List<VariableBean> vlist = new ArrayList<VariableBean>();
    for (Variable v : s.getVariables()) {
      vlist.add(new VariableBean(v));
    }
    return vlist;
  }

  public class VariableBean {
    // static public String editableProperties() { return "title include logging freq"; }
    private Variable vs;
    private String name, dimensions, desc, units, dataType, shape;
    private String coordSys;

    // no-arg constructor
    public VariableBean() {
    }

    // create from a dataset
    public VariableBean(Variable vs) {
      this.vs = vs;
      //vs = (v instanceof VariableEnhanced) ? (VariableEnhanced) v : new VariableStandardized( v);

      setName(vs.getShortName());
      setDescription(vs.getDescription());
      setUnits(vs.getUnitsString());
      setDataType(vs.getDataType().toString());

      //Attribute csAtt = vs.findAttribute("_coordSystems");
      //if (csAtt != null)
      //  setCoordSys( csAtt.getStringValue());

      // collect dimensions
      StringBuilder lens = new StringBuilder();
      StringBuilder names = new StringBuilder();
      java.util.List dims = vs.getDimensions();
      for (int j = 0; j < dims.size(); j++) {
        ucar.nc2.Dimension dim = (ucar.nc2.Dimension) dims.get(j);
        if (j > 0) {
          lens.append(",");
          names.append(",");
        }
        String name = dim.isShared() ? dim.getShortName() : "anon";
        names.append(name);
        lens.append(dim.getLength());
      }
      setDimensions(names.toString());
      setShape(lens.toString());
    }

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }

    public String getGroup() {
      return vs.getParentGroup().getFullName();
    }

    public String getDimensions() {
      return dimensions;
    }

    public void setDimensions(String dimensions) {
      this.dimensions = dimensions;
    }

    /* public boolean isCoordVar() { return isCoordVar; }
    public void setCoordVar(boolean isCoordVar) { this.isCoordVar = isCoordVar; }

    /* public boolean isAxis() { return axis; }
    public void setAxis(boolean axis) { this.axis = axis; }

    public boolean isGeoGrid() { return isGrid; }
    public void setGeoGrid(boolean isGrid) { this.isGrid = isGrid; }

    public String getAxisType() { return axisType; }
    public void setAxisType(String axisType) { this.axisType = axisType; } */

    //public String getCoordSys() { return coordSys; }
    //public void setCoordSys(String coordSys) { this.coordSys = coordSys; }

    /* public String getPositive() { return positive; }
    public void setPositive(String positive) { this.positive = positive; }

    */

    public String getDescription() {
      return desc;
    }

    public void setDescription(String desc) {
      this.desc = desc;
    }

    public String getUnits() {
      return units;
    }

    public void setUnits(String units) {
      this.units = units;
    }

    public String getDataType() {
      return dataType;
    }

    public void setDataType(String dataType) {
      this.dataType = dataType;
    }

    public String getShape() {
      return shape;
    }

    public void setShape(String shape) {
      this.shape = shape;
    }

    /** Get hasMissing */
    // public boolean getHasMissing() { return hasMissing; }
    /** Set hasMissing */
    // public void setHasMissing( boolean hasMissing) { this.hasMissing = hasMissing; }

  }

  public class AttributeBean {
    private Attribute att;

    // no-arg constructor
    public AttributeBean() {
    }

    // create from a dataset
    public AttributeBean(Attribute att) {
      this.att = att;
    }

    public String getName() {
      return att.getShortName();
    }

    public String getValue() {
      Array value = att.getValues();
      return NCdumpW.printArray(value, null, null);
    }

  }

  class PartitionTreeBrowser {

    /**
     * Title of the application
     */
    // public static final String APP_TITLE = "FileBro";
    /**
     * Used to open/edit/print files.
     */
    //private Desktop desktop;
    /**
     * Provides nice icons and names for files.
     */
    private FileSystemView fileSystemView;

    /**
     * currently selected File.
     */
    private File currentFile;

    /**
     * Main GUI container
     */
    // private JPanel gui;

    /**
     * File-system tree. Built Lazily
     */
    JTree tree;
    private DefaultTreeModel treeModel;

    /**
     * Directory listing
     */
    //private JTable table;
    private JProgressBar progressBar;
    /**
     * Table model for File[].
     */

    /* File controls. */
    private JButton openFile;

    /* File details. */
    JPanel fileView;
    private JLabel fileName;
    private JTextField path;
    private JLabel date;
    private JLabel size;
    private JCheckBox readable;
    private JCheckBox writable;
    private JCheckBox executable;
    private JRadioButton isDirectory;
    private JRadioButton isFile;

    public void setRoot(File rootDir) {
      if (!rootDir.isDirectory()) return;

      DefaultMutableTreeNode root = new DefaultMutableTreeNode();
      root.setUserObject(rootDir);

      for (File file : rootDir.listFiles()) {
        DefaultMutableTreeNode node = new DefaultMutableTreeNode(file);
        root.add(node);
      }

      treeModel = new DefaultTreeModel(root);
      tree.setModel(treeModel);
      tree.setSelectionInterval(0, 0);
    }

    ListenerManager lm = new ListenerManager("ucar.nc2.ui.event.ActionValueListener", "ucar.nc2.ui.event.ActionValueEvent", "actionPerformed");

    public void addActionValueListener(ActionValueListener l) {
      lm.addListener(l);
    }

    public PartitionTreeBrowser() {
      fileSystemView = FileSystemView.getFileSystemView();
      makeGui();
    }

    private void makeGui () {

      // the File tree
      DefaultMutableTreeNode root = new DefaultMutableTreeNode();
      treeModel = new DefaultTreeModel(root);

      TreeSelectionListener treeSelectionListener = new TreeSelectionListener() {
        public void valueChanged(TreeSelectionEvent tse) {
          DefaultMutableTreeNode node = (DefaultMutableTreeNode) tse.getPath().getLastPathComponent();
          showChildren(node);
          setFileDetails((File) node.getUserObject());
        }
      };

      // show the file system roots.
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
        //
      }

      tree = new JTree(treeModel);
      tree.setRootVisible(false);
      tree.addTreeSelectionListener(treeSelectionListener);
      tree.setCellRenderer(new FileTreeCellRenderer());
      tree.expandRow(0);
      JScrollPane treeScroll = new JScrollPane(tree);

      // as per trashgod tip
      tree.setVisibleRowCount(15);

      Dimension preferredSize = treeScroll.getPreferredSize();
      Dimension widePreferred = new Dimension(200, (int) preferredSize.getHeight());
      treeScroll.setPreferredSize(widePreferred);

      /////////////////////
      // details for a File
      JPanel fileMainDetails = new JPanel(new BorderLayout(4, 2));
      fileMainDetails.setBorder(new EmptyBorder(0, 6, 0, 6));

      JPanel fileDetailsLabels = new JPanel(new GridLayout(0, 1, 2, 2));
      fileMainDetails.add(fileDetailsLabels, BorderLayout.WEST);

      JPanel fileDetailsValues = new JPanel(new GridLayout(0, 1, 2, 2));
      fileMainDetails.add(fileDetailsValues, BorderLayout.CENTER);

      fileDetailsLabels.add(new JLabel("File", JLabel.TRAILING));
      fileName = new JLabel();
      fileDetailsValues.add(fileName);
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

      //////////////
      // toolbar

      JToolBar toolBar = new JToolBar();
      toolBar.setFloatable(false);  // mnemonics stop working in a floated toolbar

      JButton locateFile = new JButton("Move");
      locateFile.setMnemonic('l');
      locateFile.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent ae) {
          try {
            moveCdmIndexFile(currentFile);
          } catch (Throwable t) {
            showThrowable(t);
          }
          repaint();
        }
      });
      toolBar.add(locateFile);

      openFile = new JButton("Show");
      openFile.setMnemonic('o');
      openFile.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent ae) {
          try {
            showCdmIndexFile(currentFile);
          } catch (Throwable t) {
            showThrowable(t);
          }
          repaint();
        }
      });
      toolBar.add(openFile);

      flags.add(new JLabel("::  Flags"));
      readable = new JCheckBox("Read  ");
      readable.setMnemonic('a');
      flags.add(readable);

      writable = new JCheckBox("Write  ");
      writable.setMnemonic('w');
      flags.add(writable);

      executable = new JCheckBox("Execute");
      executable.setMnemonic('x');
      flags.add(executable);

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

      fileView = new JPanel(new BorderLayout(3, 3));
      fileView.add(toolBar, BorderLayout.NORTH);
      fileView.add(fileMainDetails, BorderLayout.CENTER);
      fileView.add(progressBar, BorderLayout.EAST);
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

      SwingWorker<Void, File> worker = new SwingWorker<Void, File>() {
        @Override
        public Void doInBackground() {
          File file = (File) node.getUserObject();
          if (file.isDirectory()) {
            File[] files = fileSystemView.getFiles(file, true); //!!
            if (node.isLeaf()) {
              for (File child : files) {
                //if (child.isDirectory()) {
                  publish(child);
                //}
              }
            }
          }
          return null;
        }

        @Override
        protected void process(List<File> chunks) {
          for (File child : chunks) {
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
    private void setFileDetails(File file) {
      currentFile = file;
      Icon icon = fileSystemView.getSystemIcon(file);
      fileName.setIcon(icon);
      fileName.setText(fileSystemView.getSystemDisplayName(file));
      path.setText(file.getPath());
      date.setText(new Date(file.lastModified()).toString());
      size.setText(file.length() + " bytes");
      readable.setSelected(file.canRead());
      writable.setSelected(file.canWrite());
      executable.setSelected(file.canExecute());
      isDirectory.setSelected(file.isDirectory());

      isFile.setSelected(file.isFile());

      JFrame f = (JFrame) getTopLevelAncestor();
      if (f != null) {
        f.setTitle(fileSystemView.getSystemDisplayName(file));
      }

      repaint();
    }

    /**
     * A TreeCellRenderer for a File.
     */
    private class FileTreeCellRenderer extends DefaultTreeCellRenderer {

      private static final long serialVersionUID = -7799441088157759804L;

      private FileSystemView fileSystemView;

      private JLabel label;

      FileTreeCellRenderer() {
        label = new JLabel();
        label.setOpaque(true);
        fileSystemView = FileSystemView.getFileSystemView();
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
        File file = (File) node.getUserObject();
        label.setIcon(fileSystemView.getSystemIcon(file));
        label.setText(fileSystemView.getSystemDisplayName(file));
        label.setToolTipText(file.getPath());

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
}
