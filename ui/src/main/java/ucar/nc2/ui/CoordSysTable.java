// $Id: CoordSysTable.java,v 1.16 2006/06/06 16:07:15 caron Exp $
/*
 * Copyright 1997-2000 Unidata Program Center/University Corporation for
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
import ucar.nc2.Dimension;
import ucar.nc2.dt.radial.RadialCoordSys;
import ucar.nc2.dataset.*;
import ucar.nc2.dataset.grid.*;

import ucar.util.prefs.*;
import ucar.util.prefs.ui.*;
import thredds.ui.*;

import java.awt.*;
import java.awt.event.*;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;
import java.util.*;
import java.util.List;

import javax.swing.*;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.ListSelectionEvent;

/**
 * A Swing widget to examine Coordinate Systems.
 *
 * @author John Caron
 * @version $Id: CoordSysTable.java,v 1.16 2006/06/06 16:07:15 caron Exp $
 */

public class CoordSysTable extends JPanel {
  private PreferencesExt prefs;
  private NetcdfDataset ds;

  private BeanTableSorted varTable, csTable, axisTable;
  private JSplitPane split, split2;
  private TextHistoryPane infoTA;
  private IndependentWindow infoWindow;
  private StringBuffer parseInfo = new StringBuffer();

  public CoordSysTable(PreferencesExt prefs) {
    this.prefs = prefs;

    varTable = new BeanTableSorted(VariableBean.class, (PreferencesExt) prefs.node("VariableBeans"), false);
    varTable.addListSelectionListener(new ListSelectionListener() {
      public void valueChanged(ListSelectionEvent e) {
        VariableBean vb = (VariableBean) varTable.getSelectedBean();
        if (null != vb.firstCoordSys)
          setSelectedCoordinateSystem( vb.firstCoordSys);
      }
    });

    csTable = new BeanTableSorted(CoordinateSystemBean.class, (PreferencesExt) prefs.node("CoordinateSystemBean"), false);
    csTable.addListSelectionListener(new ListSelectionListener() {
      public void valueChanged(ListSelectionEvent e) {
        CoordinateSystemBean csb = (CoordinateSystemBean) csTable.getSelectedBean();
        setSelectedCoordinateAxes( csb.coordSys);
      }
    });

    axisTable = new BeanTableSorted(AxisBean.class, (PreferencesExt) prefs.node("CoordinateAxisBean"), false);

    JTable jtable = varTable.getJTable();
    thredds.ui.PopupMenu csPopup = new thredds.ui.PopupMenu(jtable, "Options");
    csPopup.addAction("Show Declaration", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        VariableBean vb = (VariableBean) varTable.getSelectedBean();
        Variable v = ds.findVariable( vb.getName());
        infoTA.clear();
        infoTA.appendLine( v.toString());
        infoTA.gotoTop();
        infoWindow.showIfNotIconified();
      }
    });

    // the info window
    infoTA = new TextHistoryPane();
    infoWindow = new IndependentWindow("Variable Information", BAMutil.getImage( "netcdfUI"), infoTA);
    infoWindow.setBounds( (Rectangle) prefs.getBean("InfoWindowBounds", new Rectangle( 300, 300, 500, 300)));

    split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, false, varTable, csTable);
    split.setDividerLocation(prefs.getInt("splitPos", 500));

    split2 = new JSplitPane(JSplitPane.VERTICAL_SPLIT, false, split, axisTable);
    split2.setDividerLocation(prefs.getInt("splitPos2", 500));

    setLayout(new BorderLayout());
    add(split2, BorderLayout.CENTER);
  }

  public PreferencesExt getPrefs() { return prefs; }
  public String getParseInfo() { return parseInfo.toString(); }

  public void save() {
    varTable.saveState(false);
    csTable.saveState(false);
    axisTable.saveState(false);
    prefs.putBeanObject("InfoWindowBounds", infoWindow.getBounds());
    prefs.putInt("splitPos", split.getDividerLocation());
    prefs.putInt("splitPos2", split2.getDividerLocation());
  }

  public void setDataset(NetcdfDataset ds) {
    this.ds = ds;
    parseInfo.setLength(0);

    ArrayList beanList = new ArrayList();
    ArrayList axisList = new ArrayList();
    setVariables( ds.getVariables(), axisList, beanList);

    varTable.setBeans( beanList);
    axisTable.setBeans( axisList);
    csTable.setBeans( getCoordinateSystemBeans( ds));
  }

  private void setVariables(List varList, List axisList, List beanList) {
    for (int i=0; i<varList.size(); i++) {
      VariableEnhanced v = (VariableEnhanced) varList.get(i);
      if (v instanceof CoordinateAxis)
        axisList.add( new AxisBean( (CoordinateAxis) v));
      else
        beanList.add( new VariableBean( v));

      if (v instanceof Structure) {
        java.util.List nested = ((Structure) v).getVariables();
        setVariables( nested, axisList, beanList);
      }
    }
  }

  public ArrayList getCoordinateSystemBeans(NetcdfDataset ds) {
    ArrayList vlist = new ArrayList();
    java.util.List list = ds.getCoordinateSystems();
    for (int i=0; i<list.size(); i++) {
      CoordinateSystem elem = (CoordinateSystem) list.get(i);
      vlist.add( new CoordinateSystemBean( elem));
    }
    return vlist;
  }

  private void setSelectedCoordinateSystem( CoordinateSystem coordSys) {
    List beans = csTable.getBeans();
    for (int i=0; i<beans.size(); i++) {
      CoordinateSystemBean bean = (CoordinateSystemBean) beans.get(i);
      if (bean.coordSys == coordSys) {
        csTable.setSelectedBean(bean);
        return;
      }
    }
  }

  private void setSelectedCoordinateAxes( CoordinateSystem cs) {
    List axesList = cs.getCoordinateAxes();
    if (axesList.size() == 0) return;
    CoordinateAxis axis = (CoordinateAxis) axesList.get(0);

    List beans = axisTable.getBeans();
    for (int i=0; i<beans.size(); i++) {
      AxisBean bean = (AxisBean) beans.get(i);
      if (bean.axis == axis) {
        axisTable.setSelectedBean(bean);
        return;
      }
    }
  }

  public class VariableBean {
    // static public String editableProperties() { return "title include logging freq"; }

    VariableEnhanced ve;
    CoordinateSystem firstCoordSys = null;
    String name, desc, units, axisType="", positive= "";
    String dims, shape, csNames, dataType = "";
    boolean isCoordVar, axis;

    // no-arg constructor
    public VariableBean() {}

    // create from a dataset
    public VariableBean( VariableEnhanced v) {
      this.ve = v;

      setName( v.getName());
      setDescription( v.getDescription());
      setUnits( v.getUnitsString());

            // collect dimensions
      StringBuffer lens = new StringBuffer();
      StringBuffer names = new StringBuffer();
      java.util.List dims = v.getDimensions();
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
      setDims( names.toString());
      setShape( lens.toString());

      StringBuffer buff = new StringBuffer();
      List csList = v.getCoordinateSystems();
      for (int i = 0; i < csList.size(); i++) {
        CoordinateSystem cs = (CoordinateSystem) csList.get(i);
        buff.append( cs.getName()+" ");
        if (firstCoordSys == null)
          firstCoordSys = cs;

        if (GridCoordSys.isGridCoordSys( null, cs)) {
          GridCoordSys gcs = new GridCoordSys( cs);
          if (gcs.isComplete( v))
            addDataType("grid");
        } else {

        }

      }
      setCoordSys( buff.toString());
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDataType() { return dataType; }
    void addDataType( String dt) {
      dataType = dataType + " "+dt;
    }

    public String getDims() { return dims; }
    public void setDims(String dims) { this.dims = dims; }

    public String getShape() { return shape; }
    public void setShape(String shape) { this.shape = shape; }

    public String getDescription() { return desc; }
    public void setDescription(String desc) { this.desc = desc; }

    public String getUnits() { return units; }
    public void setUnits(String units) { this.units = (units == null) ? "null" : units; }

    public String getCoordSys() { return csNames; }
    public void setCoordSys(String csNames) { this.csNames = csNames; }
  }

  public class CoordinateSystemBean {
    // static public String editableProperties() { return "title include logging freq"; }

    CoordinateSystem coordSys;
    private String name, ctNames, dataType = "";
    private int domainRank, rangeRank;
    private boolean isGeoXY, isLatLon, isProductSet, isRegular;

    // no-arg constructor
    public CoordinateSystemBean() {}

    public CoordinateSystemBean( CoordinateSystem cs) {
      this.coordSys = cs;

      setCoordSys( cs.getName());
      setGeoXY( cs.isGeoXY());
      setLatLon( cs.isLatLon());
      setProductSet( cs.isProductSet());
      setRegular( cs.isRegular());
      setDomainRank( cs.getDomain().size());
      setRangeRank( cs.getCoordinateAxes().size());

      if (GridCoordSys.isGridCoordSys( parseInfo, cs)) {
        addDataType("grid");
      }

      if (RadialCoordSys.isRadialCoordSys( parseInfo, cs)) {
        addDataType("radial");
      }

      StringBuffer buff = new StringBuffer();
      List ctList = cs.getCoordinateTransforms();
      for (int i = 0; i < ctList.size(); i++) {
        CoordinateTransform ct = (CoordinateTransform) ctList.get(i);
        if (i > 0) buff.append(" ");
        buff.append( ct.getTransformType());
        if (ct instanceof VerticalCT)
          buff.append( "("+((VerticalCT)ct).getVerticalTransformType()+")");
        if (ct instanceof ProjectionCT) {
          ProjectionCT pct = (ProjectionCT)ct;
          if (pct.getProjection() != null)
            buff.append( "("+pct.getProjection().getClassName()+")");
        }
      }
      setCoordTransforms( buff.toString());
    }

    public String getCoordSys() { return name; }
    public void setCoordSys(String name) { this.name = name; }

    public boolean isGeoXY() { return isGeoXY; }
    public void setGeoXY(boolean isGeoXY) { this.isGeoXY = isGeoXY; }

    public boolean getLatLon() { return isLatLon; }
    public void setLatLon(boolean isLatLon) { this.isLatLon = isLatLon; }

    public boolean isProductSet() { return isProductSet; }
    public void setProductSet(boolean isProductSet) { this.isProductSet = isProductSet; }

    public boolean isRegular() { return isRegular; }
    public void setRegular(boolean isRegular) { this.isRegular = isRegular; }

    public int getDomainRank() { return domainRank; }
    public void setDomainRank(int domainRank) { this.domainRank = domainRank; }

    public int getRangeRank() { return rangeRank; }
    public void setRangeRank(int rangeRank) { this.rangeRank = rangeRank; }

    public String getCoordTransforms() { return ctNames; }
    public void setCoordTransforms(String ctNames) { this.ctNames = ctNames; }

    public String getDataType() { return dataType; }
    void addDataType( String dt) {
      dataType = dataType + " "+dt;
    }

    public boolean isImplicit() { return coordSys.isImplicit(); }
  }

  public class AxisBean {
    // static public String editableProperties() { return "title include logging freq"; }

    CoordinateAxis axis;
    CoordinateSystem firstCoordSys = null;
    String name, desc, units, axisType="", positive= "", incr = "";
    String dims, shape, csNames;
    boolean isCoordVar;

    // no-arg constructor
    public AxisBean() {}

    // create from a dataset
    public AxisBean( CoordinateAxis v) {
      this.axis = v;

      setName( v.getName());
      setCoordVar( v.getCoordinateDimension() != null);
      setDescription( v.getDescription());
      setUnits( v.getUnitsString());

            // collect dimensions
      StringBuffer lens = new StringBuffer();
      StringBuffer names = new StringBuffer();
      java.util.List dims = v.getDimensions();
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
      setDims( names.toString());
      setShape( lens.toString());

      AxisType at = v.getAxisType();
      if (at != null)
        setAxisType( at.toString());
      String p = v.getPositive();
      if (p != null)
        setPositive( p);

      if (v instanceof CoordinateAxis1D) {
        CoordinateAxis1D v1 = (CoordinateAxis1D) v;
        if (v1.isRegular())
          setRegular( Double.toString( v1.getIncrement()));
      }
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public boolean isCoordVar() { return isCoordVar; }
    public void setCoordVar(boolean isCoordVar) { this.isCoordVar = isCoordVar; }

    public String getShape() { return shape; }
    public void setShape(String shape) { this.shape = shape; }

    public String getAxisType() { return axisType; }
    public void setAxisType(String axisType) { this.axisType = axisType; }

    public String getDims() { return dims; }
    public void setDims(String dims) { this.dims = dims; }

    public String getDescription() { return desc; }
    public void setDescription(String desc) { this.desc = desc; }

    public String getUnits() { return units; }
    public void setUnits(String units) { this.units = (units == null) ? "null" : units; }

    public String getPositive() { return positive; }
    public void setPositive(String positive) { this.positive = positive; }

    public String getRegular() { return incr; }
    public void setRegular(String incr) { this.incr = incr; }
  }


}

/* Change History:
   $Log: CoordSysTable.java,v $
   Revision 1.16  2006/06/06 16:07:15  caron
   *** empty log message ***

   Revision 1.15  2006/05/08 02:47:34  caron
   cleanup code for 1.5 compile
   modest performance improvements
   dapper reading, deal with coordinate axes as structure members
   improve DL writing
   TDS unit testing

   Revision 1.14  2006/01/12 22:46:26  caron
   grib now uses valid time = refTime + forecastHour
   minor nexradiosp changes

   Revision 1.13  2005/06/23 19:18:44  caron
   no message

   Revision 1.12  2005/06/11 18:42:03  caron
   no message

   Revision 1.11  2005/05/26 01:58:04  caron
   fix DateRange bugs

   Revision 1.10  2005/05/25 21:09:43  caron
   no message

   Revision 1.9  2005/03/23 22:34:41  caron
   wcs improvements

   Revision 1.8  2005/03/11 23:02:14  caron
   *** empty log message ***

   Revision 1.7  2005/03/07 20:49:25  caron
   *** empty log message ***

   Revision 1.6  2005/03/07 20:48:33  caron
   no message

   Revision 1.5  2005/01/20 00:55:32  caron
   *** empty log message ***

   Revision 1.4  2004/12/09 00:17:32  caron
   *** empty log message ***

   Revision 1.3  2004/12/08 18:08:32  caron
   implement _CoordinateAliasForDimension

   Revision 1.2  2004/12/07 02:43:22  caron
   *** empty log message ***

   Revision 1.1  2004/12/06 19:37:20  caron
   no message

   Revision 1.2  2004/12/01 05:53:43  caron
   ncml pass 2, new convention parsing

   Revision 1.1  2004/10/22 01:01:40  caron
   another round

   Revision 1.5  2004/10/06 19:03:43  caron
   clean up javadoc
   change useV3 -> useRecordsAsStructure
   remove id, title, from NetcdfFile constructors
   add "in memory" NetcdfFile

   Revision 1.4  2004/09/30 00:33:42  caron
   *** empty log message ***

   Revision 1.3  2004/08/26 17:55:09  caron
   no message

   Revision 1.2  2004/08/17 19:20:07  caron
   2.2 alpha (2)

   Revision 1.1  2004/08/16 20:53:51  caron
   2.2 alpha (2)

   Revision 1.4  2004/07/16 17:58:16  caron
   source build self-contained

   Revision 1.3  2003/10/28 23:57:21  caron
   minor

   Revision 1.2  2003/10/02 20:33:56  caron
   move SimpleUnit to dataset; add <units> tag; add projections to CF

   Revision 1.1  2003/06/09 15:23:17  caron
   add nc2.ui

 */