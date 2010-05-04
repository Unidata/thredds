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

import ucar.nc2.*;

import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.ui.dialog.CompareDialog;
import ucar.nc2.util.CompareNetcdf;
import ucar.util.prefs.*;
import ucar.util.prefs.ui.*;
import ucar.ma2.Array;
import thredds.ui.*;

import java.awt.BorderLayout;
import java.awt.Rectangle;
import java.awt.event.*;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;
import java.util.*;
import java.io.*;

import javax.swing.*;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.ListSelectionEvent;

/**
 * A Swing widget to view the content of a netcdf dataset.
 * It uses a DatasetTree widget and nested BeanTableSorted widget, by
 *  wrapping the Variables in a VariableBean.
 * A pop-up menu allows to view a Structure in a StructureTable.
 *
 *
 * @author caron
 */

public class DatasetViewer extends JPanel {
  private FileManager fileChooser;

  private PreferencesExt prefs;
  private NetcdfFile ds;

  private List<NestedTable> nestedTableList = new ArrayList<NestedTable>();
  private BeanTableSorted attTable;

  private JPanel tablePanel;
  private JSplitPane mainSplit;

  private JComponent currentComponent;
  private DatasetTreeView datasetTree;
  private NCdumpPane dumpPane;

  private TextHistoryPane infoTA;
  private StructureTable dataTable;
  private IndependentWindow infoWindow, dataWindow, dumpWindow, attWindow;

  private boolean eventsOK = true;

  public DatasetViewer(PreferencesExt prefs, FileManager fileChooser) {
    this.prefs = prefs;
    this.fileChooser = fileChooser;

    // create the variable table(s)
    tablePanel = new JPanel( new BorderLayout());
    setNestedTable(0, null);

    // the tree view
    datasetTree = new DatasetTreeView();
    datasetTree.addPropertyChangeListener(new PropertyChangeListener() {
      public void propertyChange(PropertyChangeEvent e) {
        setSelected((Variable) e.getNewValue());
      }
    });

    // layout
    mainSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, false, datasetTree, tablePanel);
    mainSplit.setDividerLocation(prefs.getInt("mainSplit", 100));

    setLayout(new BorderLayout());
    add(mainSplit, BorderLayout.CENTER);

    // the info window
    infoTA = new TextHistoryPane();
    infoWindow = new IndependentWindow("Variable Information", BAMutil.getImage( "netcdfUI"), infoTA);
    infoWindow.setBounds( (Rectangle) prefs.getBean("InfoWindowBounds", new Rectangle( 300, 300, 500, 300)));

    // the data Table
    dataTable = new StructureTable( (PreferencesExt) prefs.node("structTable"));
    dataWindow = new IndependentWindow("Data Table", BAMutil.getImage( "netcdfUI"), dataTable);
    dataWindow.setBounds( (Rectangle) prefs.getBean("dataWindow", new Rectangle( 50, 300, 1000, 600)));

    // the ncdump Pane
    dumpPane = new NCdumpPane((PreferencesExt) prefs.node("dumpPane"));
    dumpWindow = new IndependentWindow("NCDump Variable Data", BAMutil.getImage( "netcdfUI"), dumpPane);
    dumpWindow.setBounds( (Rectangle) prefs.getBean("DumpWindowBounds", new Rectangle( 300, 300, 300, 200)));
  }

  public void addActions(JPanel buttPanel) {
    AbstractButton compareButton = BAMutil.makeButtcon("Select", "Compare to another file", false);
    compareButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        compareDataset();
      }
    });
    buttPanel.add(compareButton);

    AbstractAction attAction = new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        showAtts();
      }
    };
    BAMutil.setActionProperties(attAction, "FontDecr", "global attributes", false, 'A', -1);
    BAMutil.addActionToContainer(buttPanel, attAction);
  }

  ///////////////////////////////////////

  private CompareDialog dialog = null;
  public void compareDataset() {
    if (ds == null) return;
    if (dialog == null) {
      dialog = new CompareDialog(null, fileChooser);
      dialog.pack();
      dialog.addPropertyChangeListener("OK", new PropertyChangeListener() {
        public void propertyChange(PropertyChangeEvent evt) {
          String name = evt.getPropertyName();
          CompareDialog.Data data = (CompareDialog.Data) evt.getNewValue();
          //System.out.printf("name=%s %s%n", name, data);
          compareDataset(data);
        }
      });
    }
    dialog.setVisible(true);
  }

  private void compareDataset(CompareDialog.Data data) {
    if (data.name == null) return;

    NetcdfFile compareFile = null;
    try {
      compareFile = NetcdfDataset.openFile(data.name, null);

      Formatter f = new Formatter();
      CompareNetcdf cn = new CompareNetcdf(data.showCompare, data.showDetails, data.readData);
      if (data.howMuch == CompareDialog.HowMuch.All)
        cn.compare(ds, compareFile, f);
      else {
        NestedTable nested = nestedTableList.get(0);
        Variable org = getCurrentVariable(nested.table);
        if (org == null) return;
        Variable ov = compareFile.findVariable(org.getName());
        if (ov != null)
          cn.compareVariable(org, ov, f);
      }      

      infoTA.setText(f.toString());
      infoTA.gotoTop();
      infoWindow.setTitle("Compare");
      infoWindow.show();

    } catch (Throwable ioe) {
      ByteArrayOutputStream bos = new ByteArrayOutputStream(10000);
      ioe.printStackTrace(new PrintStream(bos));
      infoTA.setText(bos.toString());
      infoTA.gotoTop();
      infoWindow.show();

    } finally {
      if (compareFile != null)
        try {
          compareFile.close();
        }
        catch (Exception eek) {
        }
    }
  }

  ////////////////////////////////////////////////

  public void showAtts() {
    if (ds == null) return;
    if (attTable == null) {
      // global attributes
      attTable = new BeanTableSorted(AttributeBean.class, (PreferencesExt) prefs.node("AttributeBeans"), false);
      attWindow = new IndependentWindow("Global Attribuutes", BAMutil.getImage( "netcdfUI"), attTable);
      attWindow.setBounds( (Rectangle) prefs.getBean("AttWindowBounds", new Rectangle( 300, 100, 500, 800)));
    }

    List<AttributeBean> attlist = new ArrayList<AttributeBean>();
    for (Attribute att : ds.getGlobalAttributes()) {
      attlist.add(new AttributeBean(att));      
    }
    attTable.setBeans(attlist);
    attWindow.show();    
  }

  public NetcdfFile getDataset() {
    return this.ds;
  }

  public void setDataset(NetcdfFile ds) {
    this.ds = ds;
    NestedTable nt = nestedTableList.get(0);
    nt.table.setBeans( getVariableBeans(ds));
    hideNestedTable( 1);

    datasetTree.setFile( ds);
  }

  private void setSelected( Variable v ) {
    eventsOK = false;

    List<Variable> vchain = new ArrayList<Variable>();
    vchain.add( v);

    Variable vp = v;
    while (vp.isMemberOfStructure()) {
      vp = vp.getParentStructure();
      vchain.add( 0, vp); // reverse
    }

    for (int i=0; i<vchain.size(); i++) {
      vp = vchain.get(i);
      NestedTable ntable = setNestedTable(i, vp.getParentStructure());
      ntable.setSelected( vp);
    }

    eventsOK = true;
  }

  private NestedTable setNestedTable(int level, Structure s) {
    NestedTable ntable;
    if (nestedTableList.size() < level+1) {
      ntable = new NestedTable(level);
      nestedTableList.add( ntable);

    } else {
      ntable = nestedTableList.get(level);
    }

    if (s != null) // variables inside of records
      ntable.table.setBeans( getStructureVariables( s));

    ntable.show();
    return ntable;
  }

  private void hideNestedTable(int level) {
    int n = nestedTableList.size();
    for (int i=n-1; i>=level; i--) {
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
      myPrefs = (PreferencesExt) prefs.node("NestedTable"+level);

      table = new BeanTableSorted(VariableBean.class, myPrefs, false);

      JTable jtable = table.getJTable();
      PopupMenu csPopup = new PopupMenu(jtable, "Options");
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
            hideNestedTable(NestedTable.this.level+2);
            setNestedTable(NestedTable.this.level+1, (Structure) v);

          } else {
            hideNestedTable(NestedTable.this.level+1);
          }
          if (eventsOK) datasetTree.setSelected( v);
        }
      });

      // layout
      if (currentComponent == null) {
        currentComponent = table;
        tablePanel.add(currentComponent, BorderLayout.CENTER);
        isShowing = true;

      } else {
        split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, false, currentComponent, table);
        splitPos = myPrefs.getInt("splitPos"+level, 500);
        if (splitPos > 0)
          split.setDividerLocation(splitPos);

        show();
      }
    }

    void show() {
      if (isShowing) return;

      tablePanel.remove( currentComponent);
      split.setLeftComponent( currentComponent);
      split.setDividerLocation( splitPos);
      currentComponent = split;
      tablePanel.add(currentComponent, BorderLayout.CENTER);
      tablePanel.revalidate();
      isShowing = true;
    }

    void hide() {
      if (!isShowing) return;
      tablePanel.remove( currentComponent);

      if (split != null) {
        splitPos = split.getDividerLocation();
        currentComponent = (JComponent) split.getLeftComponent();
        tablePanel.add(currentComponent, BorderLayout.CENTER);
      }

      tablePanel.revalidate();
      isShowing = false;
    }

    void setSelected( Variable vs) {

      List beans = table.getBeans();
      for (int i=0; i<beans.size(); i++) {
        VariableBean bean = (VariableBean) beans.get(i);
        if (bean.vs == vs) {
          table.setSelectedBean(bean);
          return;
        }
      }
    }

    void saveState() {
      table.saveState( false);
      if (split != null) myPrefs.putInt("splitPos"+level, split.getDividerLocation());
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
        NCdumpW.writeNcMLVariable( v, out);
      } catch (IOException e) {
        e.printStackTrace(); 
      }
      infoTA.appendLine( out.toString());

    } else {
      infoTA.appendLine( v.toString());
    }

    if (Debug.isSet( "Xdeveloper")) {
      infoTA.appendLine("\n");
      infoTA.appendLine( "FULL NAME = "+ v.getName());
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
      ex.printStackTrace( new PrintWriter(s));
      dumpPane.setText( s.toString());
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
        dataTable.setStructure((Structure)v);
      }
      catch (Exception ex) {
        ex.printStackTrace();
      }
    }
    else return;

    dataWindow.showIfNotIconified();
  }

  private Variable getCurrentVariable(BeanTableSorted from) {
    VariableBean vb = (VariableBean) from.getSelectedBean();
    if (vb == null) return null;
    return vb.vs;
  }

  public PreferencesExt getPrefs() { return prefs; }

  public void save() {
    dumpPane.save();
    for (int i=0; i<nestedTableList.size(); i++) {
      NestedTable nt = nestedTableList.get(i);
      nt.saveState();
    }
    prefs.putBeanObject("InfoWindowBounds", infoWindow.getBounds());
    prefs.putBeanObject("DumpWindowBounds", dumpWindow.getBounds());
    if (attWindow != null) prefs.putBeanObject("AttWindowBounds", attWindow.getBounds());

    prefs.putInt("mainSplit", mainSplit.getDividerLocation());
  }

  public List<VariableBean> getVariableBeans(NetcdfFile ds) {
    List<VariableBean> vlist = new ArrayList<VariableBean>();
    java.util.List list = ds.getVariables();
    for (int i=0; i<list.size(); i++) {
      Variable elem = (Variable) list.get(i);
      vlist.add( new VariableBean( elem));
    }
    return vlist;
  }

  public List<VariableBean> getStructureVariables(Structure s) {
    List<VariableBean> vlist = new ArrayList<VariableBean>();
    java.util.List list = s.getVariables();
    for (int i=0; i<list.size(); i++) {
      Variable elem = (Variable) list.get(i);
      vlist.add( new VariableBean( elem));
    }
    return vlist;
  }

  public class VariableBean {
    // static public String editableProperties() { return "title include logging freq"; }
    private Variable vs;
    private String name, dimensions, desc, units, dataType, shape;
    private String coordSys;

    // no-arg constructor
    public VariableBean() {}

    // create from a dataset
    public VariableBean( Variable vs) {
      this.vs = vs;
      //vs = (v instanceof VariableEnhanced) ? (VariableEnhanced) v : new VariableStandardized( v);

      setName( vs.getShortName());
      setDescription( vs.getDescription());
      setUnits( vs.getUnitsString());
      setDataType( vs.getDataType().toString());

      //Attribute csAtt = vs.findAttribute("_coordSystems");
      //if (csAtt != null)
      //  setCoordSys( csAtt.getStringValue());

      // collect dimensions
      StringBuilder lens = new StringBuilder();
      StringBuilder names = new StringBuilder();
      java.util.List dims = vs.getDimensions();
      for (int j=0; j<dims.size(); j++) {
        ucar.nc2.Dimension dim = (ucar.nc2.Dimension) dims.get(j);
        if (j>0) {
          lens.append(",");
          names.append(",");
        }
        String name = dim.isShared() ? dim.getName() : "anon";
        names.append(name);
        lens.append(dim.getLength());
      }
      setDimensions( names.toString());
      setShape( lens.toString());
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getGroup() {
      return vs.getParentGroup().getName(); 
    }

    public String getDimensions() { return dimensions; }
    public void setDimensions(String dimensions) { this.dimensions = dimensions; }

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

    public String getDescription() { return desc; }
    public void setDescription(String desc) { this.desc = desc; }

    public String getUnits() { return units; }
    public void setUnits(String units) { this.units = units; }

    public String getDataType() { return dataType; }
    public void setDataType( String dataType) { this.dataType = dataType; }

    public String getShape() { return shape; }
    public void setShape( String shape) { this.shape = shape; }

    /** Get hasMissing */
   // public boolean getHasMissing() { return hasMissing; }
    /** Set hasMissing */
   // public void setHasMissing( boolean hasMissing) { this.hasMissing = hasMissing; }

  }

  public class AttributeBean {
    private Attribute att;

    // no-arg constructor
    public AttributeBean() {}

    // create from a dataset
    public AttributeBean( Attribute att) {
      this.att = att;
    }

    public String getName() { return att.getName(); }
    public String getValue() {
      Array value = att.getValues();
      try {
        return NCdumpW.printArray(value, null, null);
      } catch (IOException e) {
        return e.getMessage();
      }
    }

  }


  /* public class StructureBean {
    // static public String editableProperties() { return "title include logging freq"; }

    private String name;
    private int domainRank, rangeRank;
    private boolean isGeoXY, isLatLon, isProductSet, isZPositive;

    // no-arg constructor
    public StructureBean() {}

    // create from a dataset
    public StructureBean( Structure s) {


      setName( cs.getName());
      setGeoXY( cs.isGeoXY());
      setLatLon( cs.isLatLon());
      setProductSet( cs.isProductSet());
      setDomainRank( cs.getDomain().size());
      setRangeRank( cs.getCoordinateAxes().size());
      //setZPositive( cs.isZPositive());
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public boolean isGeoXY() { return isGeoXY; }
    public void setGeoXY(boolean isGeoXY) { this.isGeoXY = isGeoXY; }

    public boolean getLatLon() { return isLatLon; }
    public void setLatLon(boolean isLatLon) { this.isLatLon = isLatLon; }

    public boolean isProductSet() { return isProductSet; }
    public void setProductSet(boolean isProductSet) { this.isProductSet = isProductSet; }

    public int getDomainRank() { return domainRank; }
    public void setDomainRank(int domainRank) { this.domainRank = domainRank; }

    public int getRangeRank() { return rangeRank; }
    public void setRangeRank(int rangeRank) { this.rangeRank = rangeRank; }

    //public boolean isZPositive() { return isZPositive; }
    //public void setZPositive(boolean isZPositive) { this.isZPositive = isZPositive; }
  }  */

}