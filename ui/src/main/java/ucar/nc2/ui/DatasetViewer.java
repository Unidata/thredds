// $Id: DatasetViewer.java,v 1.18 2006/06/06 16:07:15 caron Exp $
/*
 * Copyright 1997-2004 Unidata Program Center/University Corporation for
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

import ucar.ma2.*;
import ucar.nc2.*;
import ucar.nc2.dataset.VariableEnhanced;
// import ucar.nc2.dataset.*;
import ucar.unidata.util.Format;

import ucar.util.prefs.*;
import ucar.util.prefs.ui.*;
import thredds.ui.*;

import java.awt.BorderLayout;
import java.awt.Rectangle;
import java.awt.event.*;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;
import java.io.*;
import java.util.*;

import javax.swing.*;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.ListSelectionEvent;
import java.beans.*;

/**
 * A Swing widget to view the content of a netcdf dataset.
 * It uses a DatasetTree widget and nested BeanTableSorted widget, by
 *  wrapping the Variables in a VariableBean.
 * A pop-up menu allows to view a Structure in a StructureTable.
 *
 * @author John Caron
 * @version $Id: DatasetViewer.java,v 1.18 2006/06/06 16:07:15 caron Exp $
 */

public class DatasetViewer extends JPanel {
  private PreferencesExt prefs;
  private NetcdfFile ds;

  private ArrayList nestedTableList = new ArrayList();

  private JPanel tablePanel;
  private JSplitPane mainSplit;

  private JComponent currentComponent;
  private DatasetTreeView datasetTree;
  private NCdumpPane dumpPane;

  private TextHistoryPane infoTA;
  private StructureTable dataTable;
  private IndependentWindow infoWindow, dataWindow, dumpWindow;

  private boolean eventsOK = true;

  public DatasetViewer(PreferencesExt prefs) {
    this.prefs = prefs;

    // create the variable table(s)
    tablePanel = new JPanel( new BorderLayout());
    setNestedTable(0, null);

    // the tree view
    datasetTree = new DatasetTreeView();
    datasetTree.addPropertyChangeListener(new PropertyChangeListener() {
      public void propertyChange(PropertyChangeEvent e) {
        setSelected( (Variable) e.getNewValue());
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

  public NetcdfFile getDataset() {
    return this.ds;
  }

  public void setDataset(NetcdfFile ds) {
    this.ds = ds;
    NestedTable nt = (NestedTable) nestedTableList.get(0);
    nt.table.setBeans( getVariableBeans(ds));
    hideNestedTable( 1);

    datasetTree.setFile( ds);
  }

  private void setSelected( Variable v ) {
    eventsOK = false;

    ArrayList vchain = new ArrayList();
    vchain.add( v);

    Variable vp = v;
    while (vp.isMemberOfStructure()) {
      vp = (Variable) vp.getParentStructure();
      vchain.add( 0, vp); // reverse
    }

    for (int i=0; i<vchain.size(); i++) {
      vp = (Variable) vchain.get(i);
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
      ntable = (NestedTable) nestedTableList.get(level);
    }

    if (s != null) // variables inside of records
      ntable.table.setBeans( getStructureVariables( s));

    ntable.show();
    return ntable;
  }

  private void hideNestedTable(int level) {
    int n = nestedTableList.size();
    for (int i=n-1; i>=level; i--) {
      NestedTable ntable = (NestedTable) nestedTableList.get(i);
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
          showDeclaration(table);
        }
      });
      csPopup.addAction("NCdump Data", "Dump", new AbstractAction() {
        public void actionPerformed(ActionEvent e) {
          dumpData(table);
        }
      });
      /* csPopup.addAction("Count Missing Data", new AbstractAction() {
        public void actionPerformed(ActionEvent e) {
          showMissingData(table);
        }
      }); */
      csPopup.addAction("Data Table", new AbstractAction() {
        public void actionPerformed(ActionEvent e) {
          dataTable(table);
        }
      });

      // get selected variable, see if its a structure
      table.addListSelectionListener(new ListSelectionListener() {
        public void valueChanged(ListSelectionEvent e) {
          Variable v = getCurrentVariable(table);
          if ((v != null) && (v.getDataType() == ucar.ma2.DataType.STRUCTURE)) {
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

  private void showDeclaration(BeanTableSorted from) {
    Variable v = getCurrentVariable(from);
    if (v == null) return;
    infoTA.clear();
    infoTA.appendLine( v.toString());
    if (Debug.isSet( "Xdeveloper")) {
      infoTA.appendLine("\n");
      infoTA.appendLine(v.toStringDebug());
    }
    infoTA.gotoTop();
    infoWindow.show();
  }

  private void dumpData(BeanTableSorted from) {
    Variable v = getCurrentVariable(from);
    if (v == null) return;

    dumpPane.clear();
    String spec = null;

    try { spec = NCdump.makeSectionString(v, null); }
    catch (InvalidRangeException ex) { return; }

    dumpPane.setContext(ds, spec);
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
      NestedTable nt = (NestedTable) nestedTableList.get(i);
      nt.saveState();
    }
    prefs.putBeanObject("InfoWindowBounds", infoWindow.getBounds());
    prefs.putBeanObject("DumpWindowBounds", dumpWindow.getBounds());

    prefs.putInt("mainSplit", mainSplit.getDividerLocation());
  }

  public ArrayList getVariableBeans(NetcdfFile ds) {
    ArrayList vlist = new ArrayList();
    java.util.List list = ds.getVariables();
    for (int i=0; i<list.size(); i++) {
      Variable elem = (Variable) list.get(i);
      vlist.add( new VariableBean( elem));
    }
    return vlist;
  }

  public ArrayList getStructureVariables(Structure s) {
    ArrayList vlist = new ArrayList();
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
      if (vs instanceof VariableEnhanced) {
        VariableEnhanced ve = (VariableEnhanced) vs;
        setDescription( ve.getDescription());
        setUnits( ve.getUnitsString());
      } else {
        setUnits( ds.findAttValueIgnoreCase(vs, "units", null));
        setDescription( ds.findAttValueIgnoreCase(vs, "long_name", null));
      }

      setDataType( vs.isVariableLength() ? "vlen" : vs.getDataType().toString());

      Attribute csAtt = vs.findAttribute("_coordSystems");
      if (csAtt != null)
        setCoordSys( csAtt.getStringValue());

      // collect dimensions
      StringBuffer lens = new StringBuffer();
      StringBuffer names = new StringBuffer();
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

    public String getGroup() { return vs.getParentGroup().getName(); }

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

    public String getCoordSys() { return coordSys; }
    public void setCoordSys(String coordSys) { this.coordSys = coordSys; }

    /* public String getPositive() { return positive; }
    public void setPositive(String positive) { this.positive = positive; }

    */

    public String getDescription() { return desc; }
    public void setDescription(String desc) { this.desc = desc; }

    public String getUnits() { return units; }
    public void setUnits(String units) { this.units = units; }

    /** Get dataType */
    public String getDataType() { return dataType; }
    /** Set dataType */
    public void setDataType( String dataType) { this.dataType = dataType; }

    /** Get shape */
    public String getShape() { return shape; }
    /** Set shape */
    public void setShape( String shape) { this.shape = shape; }

    /** Get hasMissing */
   // public boolean getHasMissing() { return hasMissing; }
    /** Set hasMissing */
   // public void setHasMissing( boolean hasMissing) { this.hasMissing = hasMissing; }

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

/* Change History:
   $Log: DatasetViewer.java,v $
   Revision 1.18  2006/06/06 16:07:15  caron
   *** empty log message ***

   Revision 1.17  2006/05/08 02:47:34  caron
   cleanup code for 1.5 compile
   modest performance improvements
   dapper reading, deal with coordinate axes as structure members
   improve DL writing
   TDS unit testing

   Revision 1.16  2006/02/16 23:02:38  caron
   *** empty log message ***

   Revision 1.15  2006/02/06 21:17:05  caron
   more fixes to dods parsing.
   ArraySequence.flatten()
   ncml.xml use default namespace. Only way I can get ncml in catalog to validate.
   ThreddsDataFactory refactor

   Revision 1.14  2005/05/11 00:10:06  caron
   refactor StuctureData, dt.point

   Revision 1.13  2005/02/22 22:12:16  caron
   *** empty log message ***

   Revision 1.12  2005/02/18 01:14:57  caron
   no message

   Revision 1.11  2004/11/07 03:00:51  caron
   *** empty log message ***

   Revision 1.10  2004/11/04 00:40:04  caron
   no message

   Revision 1.9  2004/10/29 00:14:12  caron
   no message

   Revision 1.8  2004/10/06 19:03:43  caron
   clean up javadoc
   change useV3 -> useRecordsAsStructure
   remove id, title, from NetcdfFile constructors
   add "in memory" NetcdfFile

   Revision 1.7  2004/10/02 21:01:07  caron
   *** empty log message ***

   Revision 1.6  2004/09/30 00:33:41  caron
   *** empty log message ***

   Revision 1.5  2004/09/28 21:37:19  caron
   *** empty log message ***

   Revision 1.4  2004/09/24 02:32:02  caron
   grib2 reading

   Revision 1.3  2004/09/22 18:44:33  caron
   move common to ucar.unidata

   Revision 1.2  2004/09/22 13:46:39  caron
   *** empty log message ***

   Revision 1.1  2004/08/26 17:55:09  caron
   no message

   Revision 1.5  2004/08/19 21:38:12  caron
   no message

   Revision 1.4  2004/08/17 19:20:07  caron
   2.2 alpha (2)

   Revision 1.3  2004/08/16 20:53:52  caron
   2.2 alpha (2)

   Revision 1.2  2004/07/12 23:40:19  caron
   2.2 alpha 1.0 checkin

   Revision 1.1  2004/07/06 19:28:13  caron
   pre-alpha checkin

   Revision 1.3  2003/10/28 23:57:21  caron
   minor

   Revision 1.2  2003/10/02 20:33:56  caron
   move SimpleUnit to dataset; add <units> tag; add projections to CF

   Revision 1.1  2003/06/09 15:23:17  caron
   add nc2.ui

 */