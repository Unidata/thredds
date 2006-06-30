// $Id: RadialDatasetTable.java,v 1.6 2006/02/16 23:02:38 caron Exp $
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
import ucar.nc2.dt.RadialDataset;
import ucar.nc2.dt.RadialDatasetFixed;
import ucar.nc2.dt.radial.RadialDatasetFactory;
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

/**
 * A Swing widget to examine a RadialDataset.
 *
 * @author John Caron
 * @version $Id: RadialDatasetTable.java,v 1.6 2006/02/16 23:02:38 caron Exp $
 */

public class RadialDatasetTable extends JPanel {
  private PreferencesExt prefs;
  // private NetcdfDataset ds;
  private RadialDataset radialDataset;
  private RadialDatasetFactory factory = new RadialDatasetFactory();

  private BeanTableSorted varTable, csTable = null;
  private JSplitPane split = null;
  private TextHistoryPane infoTA;
  private IndependentWindow infoWindow;

  public RadialDatasetTable(PreferencesExt prefs) {
    this.prefs = prefs;

    varTable = new BeanTableSorted(VariableBean.class, (PreferencesExt) prefs.node("VariableBeans"), false);
    JTable jtable = varTable.getJTable();

    thredds.ui.PopupMenu csPopup = new thredds.ui.PopupMenu(jtable, "Options");
    csPopup.addAction("Show Declaration", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        VariableBean vb = (VariableBean) varTable.getSelectedBean();
        VariableSimpleIF v = radialDataset.getDataVariable( vb.getName());
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

    // optionally show coordinate systems
    csTable = new BeanTableSorted(CoordinateSystemBean.class,
      (PreferencesExt) prefs.node("CoordinateSystemBean"), false);
    split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, false, varTable, csTable);
    split.setDividerLocation(prefs.getInt("splitPos", 500));

    setLayout(new BorderLayout());
    add(split, BorderLayout.CENTER);
  }

  public PreferencesExt getPrefs() { return prefs; }

  public void save() {
    varTable.saveState(false);
    prefs.putBeanObject("InfoWindowBounds", infoWindow.getBounds());
    if (split != null) prefs.putInt("splitPos", split.getDividerLocation());
    if (csTable != null) csTable.saveState(false);
  }

  public void setDataset(RadialDataset rds) {
    this.radialDataset = rds;

    varTable.setBeans( getVariableBeans(rds));
    if (csTable != null) csTable.setBeans( getCoordinateSystemBeans( rds));
  }

  public RadialDataset getRadialDataset() { return radialDataset; }

  public ArrayList getVariableBeans(RadialDataset rds) {
    ArrayList vlist = new ArrayList();
    /* java.util.List list = rds.getDataVariables();
    for (int i=0; i<list.size(); i++) {
      RadialDataset.RadialVariable v = (RadialDataset.RadialVariable) list.get(i);
      addVariableBeans( vlist, v);
    } */
    return vlist;
  }

  private void addVariableBeans(ArrayList vlist, VariableEnhanced v) {
    if (v instanceof StructureDS) {
      StructureDS s = (StructureDS) v;
      List members = s.getVariables();
      for (int i = 0; i < members.size(); i++) {
        VariableEnhanced nested =  (VariableEnhanced) members.get(i);
        // LOOK flatten here ??
        addVariableBeans( vlist, nested);
      }
    } else {
      vlist.add( new VariableBean( v));
    }
  }

  public ArrayList getCoordinateSystemBeans(RadialDataset ds) {
    ArrayList vlist = new ArrayList();
    /* java.util.List list = ds.getCoordinateSystems();
    for (int i=0; i<list.size(); i++) {
      CoordinateSystem elem = (CoordinateSystem) list.get(i);
      vlist.add( new CoordinateSystemBean( elem));
    }            */
    return vlist;
  }

  public class VariableBean {
    // static public String editableProperties() { return "title include logging freq"; }

    private String name, desc, units, axisType="";
    String dims, r, elev, azi, t;
    private boolean isCoordVar, isRadial, axis;

    // no-arg constructor
    public VariableBean() {}

    // create from a dataset
    public VariableBean( VariableEnhanced v) {
      setName( v.getName());
      setCoordVar( v.getCoordinateDimension() != null);
      setDescription( v.getDescription());
      setUnits( v.getUnitsString());

            // collect dimensions
      StringBuffer buff = new StringBuffer();
      java.util.List dims = v.getDimensions();
      for (int j=0; j<dims.size(); j++) {
        ucar.nc2.Dimension dim = (ucar.nc2.Dimension) dims.get(j);
        buff.append(dim.getName()+" ");
      }
      setDims( buff.toString());

      if (v instanceof CoordinateAxis) {
        setAxis( true);
        CoordinateAxis ca = (CoordinateAxis) v;
        AxisType at = ca.getAxisType();
        if (at != null)
          setAxisType( at.toString());
        String p = ca.getPositive();
      }

      /* RadialDatasetFixed.RadialVariableFixed rvar = (RadialDatasetFixed.RadialVariableFixed) radialDataset.getDataVariable( v.getName());
      if (rvar != null) {
        RadialCoordSys rcsys = rvar.getRadialCoordSys();
        setRadialVar (true);
        CoordinateAxis axis = rcsys.getRadialAxis();
        if (axis != null) setR( axis.getName());
        axis = rcsys.getAzimuthAxis();
        if (axis != null) setAzimuth( axis.getName());
        axis = rcsys.getElevationAxis();
        if (axis != null) setElevation( axis.getName());
        axis = rcsys.getTimeAxis();
        if (axis != null) setTime( axis.getName());
      } */
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getR() { return r; }
    public void setR(String r) { this.r = r; }

    public String getElevation() { return elev; }
    public void setElevation(String elev) { this.elev = elev; }

    public String getAzimuth() { return azi; }
    public void setAzimuth(String azi) { this.azi = azi; }

    public String getTime() { return t; }
    public void setTime(String t) { this.t = t; }

    public boolean isCoordVar() { return isCoordVar; }
    public void setCoordVar(boolean isCoordVar) { this.isCoordVar = isCoordVar; }

    public boolean isAxis() { return axis; }
    public void setAxis(boolean axis) { this.axis = axis; }

    public boolean isRadialVar() { return isRadial; }
    public void setRadialVar(boolean isRadial) { this.isRadial = isRadial; }

    public String getAxisType() { return axisType; }
    public void setAxisType(String axisType) { this.axisType = axisType; }

    public String getDims() { return dims; }
    public void setDims(String dims) { this.dims = dims; }

    public String getDescription() { return desc; }
    public void setDescription(String desc) { this.desc = desc; }

    public String getUnits() { return units; }
    public void setUnits(String units) { this.units = units; }
  }

  public class CoordinateSystemBean {
    // static public String editableProperties() { return "title include logging freq"; }

    private String name;
    private int domainRank, rangeRank;
    private boolean isRadial, isLatLon, isProductSet;

    // no-arg constructor
    public CoordinateSystemBean() {}

    // create from a dataset
    public CoordinateSystemBean( CoordinateSystem cs) {
      setName( cs.getName());
      setRadial( cs.isRadial());
      setLatLon( cs.isLatLon());
      setProductSet( cs.isProductSet());
      setDomainRank( cs.getDomain().size());
      setRangeRank( cs.getCoordinateAxes().size());
      //setZPositive( cs.isZPositive());
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public boolean isRadial() { return isRadial; }
    public void setRadial(boolean isRadial) { this.isRadial = isRadial; }

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
  }
}

/* Change History:
   $Log: RadialDatasetTable.java,v $
   Revision 1.6  2006/02/16 23:02:38  caron
   *** empty log message ***

   Revision 1.5  2006/02/06 21:17:05  caron
   more fixes to dods parsing.
   ArraySequence.flatten()
   ncml.xml use default namespace. Only way I can get ncml in catalog to validate.
   ThreddsDataFactory refactor

   Revision 1.4  2005/03/15 23:20:53  caron
   new radial dataset interface
   change getElevation() to getAltitude()

   Revision 1.3  2005/03/11 23:02:14  caron
   *** empty log message ***

   Revision 1.2  2005/03/07 20:49:25  caron
   *** empty log message ***

   Revision 1.1  2005/03/07 20:48:34  caron
   no message

   Revision 1.4  2005/02/18 01:14:57  caron
   no message

   Revision 1.3  2004/12/07 02:43:22  caron
   *** empty log message ***

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