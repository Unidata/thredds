/*
 * Copyright (c) 1998-2019 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.ui.op;

import ucar.ma2.Array;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.FileWriter2;
import ucar.nc2.NCdumpW;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFileWriter;
import ucar.nc2.Structure;
import ucar.nc2.Variable;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.jni.netcdf.Nc4Iosp;
import ucar.nc2.ncml.NcMLWriter;
import ucar.nc2.stream.NcStreamWriter;
import ucar.nc2.ui.dialog.CompareDialog;
import ucar.nc2.ui.dialog.NetcdfOutputChooser;
import ucar.nc2.util.CancelTask;
import ucar.ui.widget.BAMutil;
import ucar.ui.widget.FileManager;
import ucar.ui.widget.IndependentWindow;
import ucar.ui.widget.PopupMenu;
import ucar.ui.widget.ProgressMonitor;
import ucar.ui.widget.ProgressMonitorTask;
import ucar.ui.widget.TextHistoryPane;
import ucar.nc2.util.CompareNetcdf2;
import ucar.nc2.write.Nc4Chunking;
import ucar.nc2.write.Nc4ChunkingStrategy;
import ucar.util.prefs.PreferencesExt;
import ucar.ui.prefs.BeanTable;
import ucar.ui.prefs.Debug;

import ucar.nc2.ui.StructureTable;

import org.jdom2.Element;

import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTable;

/**
 * UI for writing datasets to netcdf formatted disk files.
 *
 * @author caron
 * @since 12/7/12
 */
public class DatasetWriter extends JPanel {

  private static final org.slf4j.Logger logger
      = org.slf4j.LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private FileManager fileChooser;

  private PreferencesExt prefs;
  private NetcdfFile ds;

  private List<NestedTable> nestedTableList = new ArrayList<>();
  private BeanTable attTable;
  private BeanTable dimTable;

  private JPanel tablePanel;
  private JSplitPane mainSplit;

  private JComponent currentComponent;
  private NetcdfOutputChooser outputChooser;

  private TextHistoryPane infoTA;
  private StructureTable dataTable;
  private IndependentWindow infoWindow, dataWindow, attWindow;

  private Nc4Chunking chunker = Nc4ChunkingStrategy
      .factory(Nc4Chunking.Strategy.standard, 0, false);

  private CompareDialog dialog = null;

  /**
   *
   */
  public DatasetWriter(PreferencesExt prefs, FileManager fileChooser) {
    this.prefs = prefs;
    this.fileChooser = fileChooser;

    // create the variable table(s)
    dimTable = new BeanTable(DimensionBean.class, (PreferencesExt) prefs.node("DimensionBeanTable"),
        false, "Dimensions", null, new DimensionBean());

    tablePanel = new JPanel(new BorderLayout());
    setNestedTable(0, null);

        /* the tree view
        datasetTree = new DatasetTreeView();
        datasetTree.addPropertyChangeListener(new PropertyChangeListener() {
          public void propertyChange(PropertyChangeEvent e) {
            setSelected((Variable) e.getNewValue());
          }
        }); */

    outputChooser = new NetcdfOutputChooser((Frame) null);
    outputChooser.addPropertyChangeListener("OK", new PropertyChangeListener() {
      @Override
      public void propertyChange(PropertyChangeEvent evt) {
        writeFile((NetcdfOutputChooser.Data) evt.getNewValue());
      }
    });
    outputChooser.addEventListener(e -> {
      Nc4Chunking.Strategy strategy = (Nc4Chunking.Strategy) e.getItem();
      chunker = Nc4ChunkingStrategy.factory(strategy, 0, false);
      showChunking();
    });

    // layout
    mainSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, false, dimTable, tablePanel);
    mainSplit.setDividerLocation(prefs.getInt("mainSplit", 300));

    setLayout(new BorderLayout());
    add(outputChooser.getContentPane(), BorderLayout.NORTH);
    add(mainSplit, BorderLayout.CENTER);

    // the info window
    infoTA = new TextHistoryPane();
    infoWindow = new IndependentWindow("Variable Information", BAMutil.getImage("nj22/NetcdfUI"),
        infoTA);
    infoWindow.setBounds(
        (Rectangle) prefs.getBean("InfoWindowBounds", new Rectangle(300, 300, 500, 300)));

    // the data Table
    dataTable = new StructureTable((PreferencesExt) prefs.node("structTable"));
    dataWindow = new IndependentWindow("Data Table", BAMutil.getImage("nj22/NetcdfUI"), dataTable);
    dataWindow
        .setBounds((Rectangle) prefs.getBean("dataWindow", new Rectangle(50, 300, 1000, 600)));

        /* the ncdump Pane
        dumpPane = new NCdumpPane((PreferencesExt) prefs.node("dumpPane"));
        dumpWindow = new IndependentWindow("NCDump Variable Data", BAMutil.getImage( "netcdfUI"), dumpPane);
        dumpWindow.setBounds( (Rectangle) prefs.getBean("DumpWindowBounds", new Rectangle( 300, 300, 300, 200))); */
  }

  /**
   *
   */
  public void addActions(JPanel buttPanel) {
    final AbstractAction attAction = new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent e) {
        showAtts();
      }
    };
    BAMutil.setActionProperties(attAction, "FontDecr", "global attributes", false, 'A', -1);
    BAMutil.addActionToContainer(buttPanel, attAction);
  }

  /**
   *
   */
  public void save() {
    dimTable.saveState(false);

    for (NestedTable nt : nestedTableList) {
      nt.saveState();
    }

    // prefs.putBeanObject("InfoWindowBounds", infoWindow.getBounds());
    // prefs.putBeanObject("DumpWindowBounds", dumpWindow.getBounds());
    // if (attWindow != null) prefs.putBeanObject("AttWindowBounds", attWindow.getBounds());

    prefs.putInt("mainSplit", mainSplit.getDividerLocation());
  }


  /**
   *
   */
  private void showChunking() {
    if (nestedTableList.size() == 0) {
      return;
    }

    final NestedTable t = nestedTableList.get(0);
    final List beans = t.table.getBeans();

    for (Object bean1 : beans) {
      final VariableBean bean = (VariableBean) bean1;
      final boolean isChunked = chunker.isChunked(bean.vs);

      bean.setChunked(isChunked);

      if (isChunked) {
        bean.setChunkArray(chunker.computeChunking(bean.vs));
      } else {
        bean.setChunkArray(null);
      }
    }
    t.table.refresh();
  }

  /**
   *
   */
  void writeFile(NetcdfOutputChooser.Data data) {
    if (ds == null) {
      return;
    }

    String filename = data.outputFilename.trim();
    if (filename.length() == 0) {
      JOptionPane.showMessageDialog(this, "Filename has not been set");
      return;
    }
        /* File f = new File(filename);
        if (!f.canWrite())  {
          JOptionPane.showMessageDialog(this, "Cannot write to "+filename);
          //return;
        } */

    if (data.version == NetcdfFileWriter.Version.ncstream) {
      writeNcstream(data.outputFilename);
      return;
    }

    if (data.version.isNetdf4format()) {
      if (!Nc4Iosp.isClibraryPresent()) {
        JOptionPane.showMessageDialog(this, "NetCDF-4 C library is not loaded");
        return;
      }
    }

    WriterTask task = new WriterTask(data);
    ProgressMonitor pm = new ProgressMonitor(task);
    pm.addActionListener(e -> {
      if (e.getActionCommand().equals("success")) {
        logger.debug("{}}", e.getActionCommand());
      }
    });
    pm.start(null, "Writing " + filename, ds.getVariables().size());
  }

  /**
   *
   */
  class WriterTask extends ProgressMonitorTask implements CancelTask {

    NetcdfOutputChooser.Data data;

     WriterTask(NetcdfOutputChooser.Data data) {
      this.data = data;
    }

     public void run() {
      try {
        final List beans = nestedTableList.get(0).table.getBeans();
        final BeanChunker bc = new BeanChunker(beans, data.deflate, data.shuffle);
        final FileWriter2 writer = new FileWriter2(ds, data.outputFilename, data.version, bc);

        final double start = System.nanoTime();
        // write() return the open file that was just written, so we just need to close it.
        try (NetcdfFile result = writer.write( this)) {
          result.close();
        }

        double took = (System.nanoTime() - start) / 1000 / 1000 / 1000;

        final File oldFile = new File(ds.getLocation());
        final File newFile = new File(data.outputFilename);

        double r = (double) newFile.length() / oldFile.length();

        logger.debug("Rewrite from {} {} to {} {} version = {} ratio = {} took= {} secs",
            ds.getLocation(), oldFile.length(), data.outputFilename, newFile.length(),
            data.version, r, took);

        JOptionPane.showMessageDialog(DatasetWriter.this,
            "File successfully written took=" + took + " secs ratio=" + r);
      } catch (Exception ioe) {
        JOptionPane.showMessageDialog(DatasetWriter.this, "ERROR: " + ioe.getMessage());
        ioe.printStackTrace();
      } finally {
        success = !cancel && !isError();
        done = true;    // do last!
      }
    }
  }

  /**
   *
   */
  void writeNcstream(String filename) {
    try (OutputStream fos = new BufferedOutputStream(new FileOutputStream(filename), 50 * 1000)) {
      final NcStreamWriter writer = new NcStreamWriter(ds, null);
      writer.streamAll(fos);
      JOptionPane.showMessageDialog(this, "File successfully written");
    } catch (Exception ioe) {
      JOptionPane.showMessageDialog(this, "ERROR: " + ioe.getMessage());
      ioe.printStackTrace();
    }
  }

  /**
   *
   */
  void writeNcstreamHeader(String filename) {
    try (FileOutputStream fos = new FileOutputStream(filename)) {
      final NcStreamWriter writer = new NcStreamWriter(ds, null);
      writer.sendHeader(fos);
      JOptionPane.showMessageDialog(this, "File successfully written");
    } catch (Exception ioe) {
      JOptionPane.showMessageDialog(this, "ERROR: " + ioe.getMessage());
      ioe.printStackTrace();
    }
  }

  /**
   *
   */
  public void compareDataset() {
    if (ds == null) {
      return;
    }

    if (dialog == null) {
      dialog = new CompareDialog(null, fileChooser);
      dialog.pack();
      dialog.addPropertyChangeListener("OK", new PropertyChangeListener() {
        @Override
        public void propertyChange(PropertyChangeEvent evt) {
          CompareDialog.Data data = (CompareDialog.Data) evt.getNewValue();
          // logger.debug("name={} {}", evt.getPropertyName(), data);
          compareDataset(data);
        }
      });
    }
    dialog.setVisible(true);
  }

  /**
   *
   */
  private void compareDataset(CompareDialog.Data data) {
    if (data.name == null) {
      return;
    }

    try (NetcdfFile compareFile = NetcdfDataset.openFile(data.name, null)) {
      final Formatter f = new Formatter();
      final CompareNetcdf2 cn = new CompareNetcdf2(f, data.showCompare, data.showDetails,
          data.readData);

      if (data.howMuch == CompareDialog.HowMuch.All) {
        cn.compare(ds, compareFile);
      } else {
        NestedTable nested = nestedTableList.get(0);
        Variable org = getCurrentVariable(nested.table);
        if (org == null) {
          return;
        }
        Variable ov = compareFile.findVariable(org.getFullNameEscaped());
        if (ov != null) {
          cn.compareVariable(org, ov);
        }
      }

      infoTA.setText(f.toString());
      infoTA.gotoTop();
      infoWindow.setTitle("Compare");
      infoWindow.show();
    } catch (Throwable ioe) {
      final StringWriter sw = new StringWriter(10000);
      ioe.printStackTrace(new PrintWriter(sw));
      infoTA.setText(sw.toString());
      infoTA.gotoTop();
      infoWindow.show();
    }
  }

  /**
   *
   */
  public void showAtts() {
    if (ds == null) {
      return;
    }

    if (attTable == null) {
      // global attributes
      attTable = new BeanTable(AttributeBean.class, (PreferencesExt) prefs.node("AttributeBeans"),
          false);
      PopupMenu varPopup = new PopupMenu(attTable.getJTable(), "Options");
      varPopup.addAction("Show Attribute", new AbstractAction() {
        @Override
        public void actionPerformed(ActionEvent e) {
          final AttributeBean bean = (AttributeBean) attTable.getSelectedBean();
          if (bean != null) {
            infoTA.setText(bean.att.toString());
            infoTA.gotoTop();
            infoWindow.show();
          }
        }
      });
      attWindow = new IndependentWindow("Global Attributes", BAMutil.getImage("nj22/NetcdfUI"),
          attTable);
      attWindow.setBounds((Rectangle) prefs.getBean("AttWindowBounds",
          new Rectangle(300, 100, 500, 800)));
    }

    final List<AttributeBean> attlist = new ArrayList<>();
    for (Attribute att : ds.getGlobalAttributes()) {
      attlist.add(new AttributeBean(att));
    }
    attTable.setBeans(attlist);
    attWindow.show();
  }

  /**
   *
   */
  public NetcdfFile getDataset() {
    return this.ds;
  }

  /**
   *
   */
  public void setDataset(NetcdfFile ds) {
    this.ds = ds;
    dimTable.setBeans(makeDimensionBeans(ds));
    final NestedTable nt = nestedTableList.get(0);
    nt.table.setBeans(makeVariableBeans(ds));
    hideNestedTable(1);
    showChunking();
  }

  /**
   *
   */
  private void setSelected(Variable v) {
    final List<Variable> vchain = new ArrayList<>();
    vchain.add(v);

    Variable vp = v;
    while (vp.isMemberOfStructure()) {
      vp = vp.getParentStructure();
      vchain.add(0, vp); // reverse
    }

    for (int i = 0; i < vchain.size(); i++) {
      vp = vchain.get(i);
      final NestedTable ntable = setNestedTable(i, vp.getParentStructure());
      ntable.setSelected(vp);
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

  /**
   *
   */
  private void showDeclaration(BeanTable from, boolean isNcml) {
    Variable v = getCurrentVariable(from);
    if (v == null) {
      return;
    }

    infoTA.clear();

    if (isNcml) {
      final NcMLWriter ncmlWriter = new NcMLWriter();
      ncmlWriter.setNamespace(null);
      ncmlWriter.getXmlFormat().setOmitDeclaration(true);

      final Element varElement = ncmlWriter.makeVariableElement(v, false);
      infoTA.appendLine(ncmlWriter.writeToString(varElement));
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

  /**
   *
   */
  private void dataTable(BeanTable from) {
    final VariableBean vb = (VariableBean) from.getSelectedBean();
    if (vb == null) {
      return;
    }

    Variable v = vb.vs;

    if (v instanceof Structure) {
      try {
        dataTable.setStructure((Structure) v);
      } catch (Exception ex) {
        ex.printStackTrace();
      }
    } else {
      return;
    }

    dataWindow.show();
  }

  /**
   *
   */
  private Variable getCurrentVariable(BeanTable from) {
    final VariableBean vb = (VariableBean) from.getSelectedBean();
    if (vb == null) {
      return null;
    }

    return vb.vs;
  }

  /**
   *
   */
  private List<VariableBean> makeVariableBeans(NetcdfFile ds) {
    final List<VariableBean> vlist = new ArrayList<>();
    for (Variable v : ds.getVariables()) {
      vlist.add(new VariableBean(v));
    }
    return vlist;
  }

  /**
   *
   */
  private List<DimensionBean> makeDimensionBeans(NetcdfFile ds) {
    final List<DimensionBean> dlist = new ArrayList<>();
    for (Dimension d : ds.getDimensions()) {
      dlist.add(new DimensionBean(d));
    }
    return dlist;
  }

  /**
   *
   */
  private List<VariableBean> getStructureVariables(Structure s) {
    final List<VariableBean> vlist = new ArrayList<>();
    for (Variable v : s.getVariables()) {
      vlist.add(new VariableBean(v));
    }
    return vlist;
  }

  /**
   *
   */
  private NestedTable setNestedTable(int level, Structure s) {
    NestedTable ntable;
    if (nestedTableList.size() < level + 1) {
      ntable = new NestedTable(level);
      nestedTableList.add(ntable);
    } else {
      ntable = nestedTableList.get(level);
    }

    if (s != null) {
      // variables inside of records
      ntable.table.setBeans(getStructureVariables(s));
    }
    ntable.show();
    return ntable;
  }

  /**
   *
   */
  private void hideNestedTable(int level) {
    int n = nestedTableList.size();
    for (int i = n - 1; i >= level; i--) {
      final NestedTable ntable = nestedTableList.get(i);
      ntable.hide();
    }
  }

  /**
   *
   */
  private class NestedTable {

    int level;
    PreferencesExt myPrefs;

    BeanTable table; // always the left component
    JSplitPane split = null; // right component (if exists) is the nested dataset.
    int splitPos = 100;
    boolean isShowing = false;

    /**
     *
     */
    NestedTable(int level) {
      this.level = level;
      myPrefs = (PreferencesExt) prefs.node("NestedTable" + level);

      table = new BeanTable(VariableBean.class, myPrefs, false, "Variables", null,
          new VariableBean());

      JTable jtable = table.getJTable();
      PopupMenu csPopup = new PopupMenu(jtable, "Options");
      csPopup.addAction("Show Declaration", new AbstractAction() {
        @Override
        public void actionPerformed(ActionEvent e) {
          showDeclaration(table, false);
        }
      });
      csPopup.addAction("Show NcML", new AbstractAction() {
        public void actionPerformed(ActionEvent e) {
          showDeclaration(table, true);
        }
      });
            /* csPopup.addAction("NCdump Data", "Dump", new AbstractAction() {
                public void actionPerformed(ActionEvent e) {
                dumpData(table);
             } }); */

      if (level == 0) {
        csPopup.addAction("Data Table", new AbstractAction() {
          @Override
          public void actionPerformed(ActionEvent e) {
            dataTable(table);
          }
        });
      }

      // get selected variable, see if its a structure
      table.addListSelectionListener(e -> {
        final Variable v = getCurrentVariable(table);
        if (v instanceof Structure) {
          hideNestedTable(NestedTable.this.level + 2);
          setNestedTable(NestedTable.this.level + 1, (Structure) v);
        } else {
          hideNestedTable(NestedTable.this.level + 1);
        }
        // if (eventsOK) datasetTree.setSelected( v);
      });

      // layout
      if (currentComponent == null) {
        currentComponent = table;
        tablePanel.add(currentComponent, BorderLayout.CENTER);
        isShowing = true;

      } else {
        split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, false, currentComponent, table);
        splitPos = myPrefs.getInt("splitPos" + level, 500);
        if (splitPos > 0) {
          split.setDividerLocation(splitPos);
        }
        show();
      }
    }

    /**
     *
     */
    void show() {
      if (isShowing) {
        return;
      }

      tablePanel.remove(currentComponent);
      split.setLeftComponent(currentComponent);
      split.setDividerLocation(splitPos);
      currentComponent = split;
      tablePanel.add(currentComponent, BorderLayout.CENTER);
      tablePanel.revalidate();
      isShowing = true;
    }

    /**
     *
     */
    void hide() {
      if (!isShowing) {
        return;
      }

      tablePanel.remove(currentComponent);

      if (split != null) {
        splitPos = split.getDividerLocation();
        currentComponent = (JComponent) split.getLeftComponent();
        tablePanel.add(currentComponent, BorderLayout.CENTER);
      }

      tablePanel.revalidate();
      isShowing = false;
    }

    /**
     *
     */
    void setSelected(Variable vs) {
      final List beans = table.getBeans();

      for (Object bean1 : beans) {
        VariableBean bean = (VariableBean) bean1;
        if (bean.vs == vs) {
          table.setSelectedBean(bean);
          return;
        }
      }
    }

    /**
     *
     */
    void saveState() {
      table.saveState(false);
      if (split != null) {
        myPrefs.putInt("splitPos" + level, split.getDividerLocation());
      }
    }
  }

  /**
   *
   */
  public static class BeanChunker implements Nc4Chunking {

    Map<String, VariableBean> map;
    int deflate;
    boolean shuffle;

    /**
     *
     */
    BeanChunker(List<VariableBean> beans, int deflate, boolean shuffle) {
      this.map = new HashMap<>(2 * beans.size());
      for (VariableBean bean : beans) {
        map.put(bean.vs.getFullName(), bean);
      }
      this.deflate = deflate;
      this.shuffle = shuffle;
    }

    /**
     *
     */
    @Override
    public boolean isChunked(Variable v) {
      VariableBean bean = map.get(v.getFullName());
      return (bean != null) && bean.isChunked();
    }

    /**
     *
     */
    @Override
    public long[] computeChunking(Variable v) {
      VariableBean bean = map.get(v.getFullName());
      return (bean == null) ? new long[0] : bean.chunked;
    }

    /**
     *
     */
    @Override
    public int getDeflateLevel(Variable v) {
      return deflate;
    }

    /**
     *
     */
    @Override
    public boolean isShuffle(Variable v) {
      return shuffle;
    }
  }

  /**
   *
   */
  public class DimensionBean {

    Dimension ds;

    /**
     * no-arg constructor
     */
    public DimensionBean() {
    }

    /**
     * create from a dimension
     */
    public DimensionBean(Dimension ds) {
      this.ds = ds;
    }

    /**
     *
     */
    public String editableProperties() {
      return "unlimited";
    }

    public String getName() {
      return ds.getShortName();
    }

    public int getLength() {
      return ds.getLength();
    }

    public boolean isUnlimited() {
      return ds.isUnlimited();
    }

    public void setUnlimited(boolean unlimited) {
      ds.setUnlimited(unlimited);
    }
  }

  /**
   *
   */
  public class VariableBean {

    private Variable vs;
    private String name, dimensions, desc, units, dataType, shape;
    private boolean isChunked;
    private long[] chunked;

    /**
     * no-arg constructor
     */
    public VariableBean() {
    }

    // create from a dataset
    public VariableBean(Variable vs) {
      this.vs = vs;

      setName(vs.getShortName());
      setDescription(vs.getDescription());
      setUnits(vs.getUnitsString());
      setDataType(vs.getDataType().toString());

      // collect dimensions
      final Formatter lens = new Formatter();
      final Formatter names = new Formatter();
      lens.format("(");
      final List<Dimension> dims = vs.getDimensions();
      for (int j = 0; j < dims.size(); j++) {
        Dimension dim = dims.get(j);
        if (j > 0) {
          lens.format(",");
          names.format(",");
        }
        final String name = dim.isShared() ? dim.getShortName() : "anon";
        names.format("%s", name);
        lens.format("%d", dim.getLength());
      }
      lens.format(")");
      setDimensions(names.toString());
      setShape(lens.toString());
    }

    /**
     *
     */
    public String editableProperties() {
      return "chunked chunkSize";
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

    public boolean isUnlimited() {
      return vs.isUnlimited();
    }

    /**
     *
     */
    public String getSize() {
      final Formatter f = new Formatter();
      f.format("%,d", vs.getSize());
      return f.toString();
    }

    /**
     *
     */
    public boolean isChunked() {
      return isChunked;
    }

    public void setChunked(boolean chunked) {
      isChunked = chunked;
      if (chunked) {
        setChunkArray(chunker.computeChunking(vs));
      } else {
        setChunkArray(null);
      }
    }

    /**
     *
     */
    public long getNChunks() {
      if (!isChunked) {
        return 1;
      }
      if (chunked == null) {
        return 1;
      }

      long elementsPerChunk = 1;
      for (long c : chunked) {
        elementsPerChunk *= c;
      }
      return vs.getSize() / elementsPerChunk;
    }

    /**
     *
     */
    public long getOverHang() {
      if (!isChunked) {
        return 0;
      }
      if (chunked == null) {
        return 0;
      }

      final int[] shape = vs.getShape();

      long total = 1;

      for (int i = 0; i < chunked.length; i++) {
        final int overhang = (int) (shape[i] % chunked[i]);
        total *= overhang;
      }
      return total;
    }

    /**
     *
     */
    public String getOHPercent() {
      if (!isChunked) {
        return "";
      }
      if (chunked == null) {
        return "";
      }

      final long total = getOverHang();
      final float p = 100.0f * total / vs.getSize();

      final Formatter f = new Formatter();
      f.format("%6.3f", p);

      return f.toString();
    }

    /**
     *
     */
    public String getChunkSize() {
      if (chunked == null) {
        return "";
      }

      final Formatter f = new Formatter();
      f.format("(");

      for (int i = 0; i < chunked.length; i++) {
        f.format("%d", chunked[i]);
        if (i < chunked.length - 1) {
          f.format(",");
        }
      }
      f.format(")");
      return f.toString();
    }

    /**
     *
     */
    public void setChunkSize(String chunkSize) {
      final StringTokenizer stoke = new StringTokenizer(chunkSize, "(), ");
      this.chunked = new long[stoke.countTokens()];
      int count = 0;
      while (stoke.hasMoreTokens()) {
        this.chunked[count++] = Long.parseLong(stoke.nextToken());
      }
    }

    /**
     *
     */
    public void setChunkArray(long[] chunked) {
      this.chunked = chunked;
      this.isChunked = (chunked != null);
    }
  }

  /**
   *
   */
  public class AttributeBean {

    private Attribute att;

    /**
     * no-arg constructor
     */
    public AttributeBean() {
    }

    /**
     * create from an attribute
     */
    public AttributeBean(Attribute att) {
      this.att = att;
    }

    /**
     *
     */
    public String getName() {
      return att.getShortName();
    }

    public String getValue() {
      final Array value = att.getValues();
      return NCdumpW.toString(value, null, null);
    }
  }
}
