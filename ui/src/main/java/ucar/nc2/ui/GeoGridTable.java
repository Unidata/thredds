// $Id: GeoGridTable.java,v 1.14 2006/06/06 16:07:15 caron Exp $
/*
 * Copyright 1997-2006 Unidata Program Center/University Corporation for
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
import ucar.nc2.dt.GridDatatype;
import ucar.nc2.dt.GridCoordSystem;
import ucar.nc2.dataset.*;
import ucar.nc2.dataset.grid.*;

import ucar.util.prefs.*;
import ucar.util.prefs.ui.*;
import ucar.unidata.geoloc.ProjectionImpl;
import thredds.ui.*;

import java.awt.*;
import java.awt.event.*;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;
import java.util.*;
import java.util.List;
import java.io.IOException;

import javax.swing.*;

/**
 * A Swing widget to examine a GridDataset.
 *
 *
 * @author caron
 * @version $Revision: 1.18 $ $Date: 2006/05/24 00:12:56 $
 */

public class GeoGridTable extends JPanel {
  private PreferencesExt prefs;
  private NetcdfDataset ds;
  private GridDataset gridDataset;
  private thredds.wcs.WcsDataset wcs = null;

  private BeanTableSorted varTable, csTable = null, axisTable = null;
  private JSplitPane split = null, split2 = null;
  private TextHistoryPane infoTA;
  private IndependentWindow infoWindow;

  public GeoGridTable(PreferencesExt prefs, boolean showCS) {
    this.prefs = prefs;

    varTable = new BeanTableSorted(GeogridBean.class, (PreferencesExt) prefs.node("GeogridBeans"), false);
    JTable jtable = varTable.getJTable();

    thredds.ui.PopupMenu csPopup = new thredds.ui.PopupMenu(jtable, "Options");
    csPopup.addAction("Show Declaration", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        GeogridBean vb = (GeogridBean) varTable.getSelectedBean();
        Variable v = ds.findVariable( vb.getName());
        infoTA.clear();
        infoTA.appendLine( v.toString());
        infoTA.gotoTop();
        infoWindow.showIfNotIconified();
      }
    });

    csPopup.addAction("WCS DescribeCoverage", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        GeogridBean vb = (GeogridBean) varTable.getSelectedBean();
        if (wcs == null)
          wcs = new thredds.wcs.WcsDataset(gridDataset, "", false);
        String dc;
        if (gridDataset.findGridByName(vb.getName()) != null)
          try {
            dc = wcs.describeCoverage(vb.getName());
            infoTA.clear();
            infoTA.appendLine(dc);
            infoTA.gotoTop();
            infoWindow.showIfNotIconified();
          } catch (IOException e1) {
            e1.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
          }
      }
    });

    // the info window
    infoTA = new TextHistoryPane();
    infoWindow = new IndependentWindow("Variable Information", BAMutil.getImage( "netcdfUI"), infoTA);
    infoWindow.setBounds( (Rectangle) prefs.getBean("InfoWindowBounds", new Rectangle( 300, 300, 500, 300)));

    // optionally show coordinate systems and axis
    Component comp = varTable;
    if (showCS) {
      csTable = new BeanTableSorted(GeoCoordinateSystemBean.class, (PreferencesExt) prefs.node("GeoCoordinateSystemBean"), false);
      axisTable = new BeanTableSorted(GeoAxisBean.class, (PreferencesExt) prefs.node("GeoCoordinateAxisBean"), false);

      split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, false, varTable, csTable);
      split.setDividerLocation(prefs.getInt("splitPos", 500));

      split2 = new JSplitPane(JSplitPane.VERTICAL_SPLIT, false, split, axisTable);
      split2.setDividerLocation(prefs.getInt("splitPos2", 500));
      comp = split2;
    }

    setLayout(new BorderLayout());
    add(comp, BorderLayout.CENTER);
  }

  public PreferencesExt getPrefs() { return prefs; }

  public void save() {
    varTable.saveState(false);
    prefs.putBeanObject("InfoWindowBounds", infoWindow.getBounds());
    if (split != null) prefs.putInt("splitPos", split.getDividerLocation());
    if (split2 != null) prefs.putInt("splitPos2", split2.getDividerLocation());
    if (csTable != null) csTable.saveState(false);
    if (axisTable != null) axisTable.saveState(false);
  }

  public void setDataset(NetcdfDataset ds) {
    this.ds = ds;
    this.gridDataset = new GridDataset(ds);

    ArrayList beanList = new ArrayList();
    java.util.List list = gridDataset.getGrids();
    for (int i=0; i<list.size(); i++) {
      GridDatatype g = (GridDatatype) list.get(i);
      beanList.add (new GeogridBean( g));
    }
    varTable.setBeans( beanList);

    if (csTable != null) {
      ArrayList csList = new ArrayList();
      ArrayList axisList = new ArrayList();
      Iterator iter  = gridDataset.getGridSets().iterator();
      while (iter.hasNext()) {
        GridDataset.Gridset gset = (GridDataset.Gridset) iter.next();
        csList.add (new GeoCoordinateSystemBean( gset));
        GridCoordSystem gsys = gset.getGeoCoordSystem();
        List axes = gsys.getCoordinateAxes();
        for (int i = 0; i < axes.size(); i++) {
          CoordinateAxis axis = (CoordinateAxis) axes.get(i);
          GeoAxisBean axisBean = new GeoAxisBean( axis);
          if (!axisList.contains( axisBean))
            axisList.add (axisBean);
        }
      }
      csTable.setBeans( csList);
      axisTable.setBeans( axisList);
    }
  }

  public GridDataset getGridDataset() { return gridDataset; }
  public GridDatatype getGrid() {
    GeogridBean vb = (GeogridBean) varTable.getSelectedBean();
    if (vb == null) {
      List grids = gridDataset.getGrids();
      if (grids.size() > 0)
        return (GridDatatype) grids.get(0);
      else
        return null;
    }
    return gridDataset.findGridByName( vb.getName());
  }

  public class GeogridBean {
    // static public String editableProperties() { return "title include logging freq"; }

    private String name, desc, units, csys;
    private String dims, x, y, z, t;

    // no-arg constructor
    public GeogridBean() {}

    // create from a dataset
    public GeogridBean( GridDatatype geogrid) {
      setName( geogrid.getName());
      setDescription( geogrid.getDescription());
      setUnits( geogrid.getUnitsString());

      setCoordSystem( geogrid.getGridCoordSystem().getName());

            // collect dimensions
      StringBuffer buff = new StringBuffer();
      java.util.List dims = geogrid.getDimensions();
      for (int j=0; j<dims.size(); j++) {
        ucar.nc2.Dimension dim = (ucar.nc2.Dimension) dims.get(j);
        if (j>0) buff.append(",");
        buff.append(dim.getLength());
      }
      setShape( buff.toString());

      Dimension d= geogrid.getXDimension();
      if (d != null) setX( d.getName());
      d= geogrid.getYDimension();
      if (d != null) setY( d.getName());
      d= geogrid.getZDimension();
      if (d != null) setZ( d.getName());
      d= geogrid.getTimeDimension();
      if (d != null) setT( d.getName());
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return desc; }
    public void setDescription(String desc) { this.desc = desc; }

    public String getUnits() { return units; }
    public void setUnits(String units) { this.units = units; }

    public String getCoordSystem() { return csys; }
    public void setCoordSystem(String csys) { this.csys = csys; }

    public String getX() { return x; }
    public void setX(String x) { this.x = x; }

    public String getY() { return y; }
    public void setY(String y) { this.y = y; }

    public String getZ() { return z; }
    public void setZ(String z) { this.z = z; }

    public String getT() { return t; }
    public void setT(String t) { this.t = t; }

    public String getShape() { return dims; }
    public void setShape(String dims) { this.dims = dims; }

  }

  public class GeoCoordinateSystemBean {
    // static public String editableProperties() { return "title include logging freq"; }

    private String name, proj, coordTrans;
    private int domainRank, rangeRank;
    private boolean  isLatLon, isRegular;
    private int ngrids = -1;

    // no-arg constructor
    public GeoCoordinateSystemBean() {}

    public GeoCoordinateSystemBean( GridDataset.Gridset gset) {
      GridCoordSystem cs = gset.getGeoCoordSystem();
      setName( cs.getName());
      setLatLon( cs.isLatLon());
      //setProductSet( cs.isProductSet());
      setDomainRank( cs.getDomain().size());
      setRangeRank( cs.getCoordinateAxes().size());
      setRegularSpatial( cs.isRegularSpatial());

      setNGrids( gset.getGrids().size());

      ProjectionImpl proj = cs.getProjection();
      if (proj != null)
        setProjection(proj.getClassName());

      int count = 0;
      StringBuffer buff = new StringBuffer();
      List ctList = cs.getCoordinateTransforms();
      for (int i = 0; i < ctList.size(); i++) {
        CoordinateTransform ct = (CoordinateTransform) ctList.get(i);
        if (count > 0) buff.append("; ");
        // buff.append( ct.getTransformType());
        if (ct instanceof VerticalCT) {
          buff.append( ((VerticalCT)ct).getVerticalTransformType());
          count++;
        }
        if (ct instanceof ProjectionCT) {
          ProjectionCT pct = (ProjectionCT)ct;
          if (pct.getProjection() == null) { // only show CT if theres no projection
            buff.append("-"+pct.getName());
            count++;
          }
        }
      }
      setCoordTransforms( buff.toString());
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public boolean isRegularSpatial() { return isRegular; }
    public void setRegularSpatial(boolean isRegular) { this.isRegular = isRegular; }

    public boolean isLatLon() { return isLatLon; }
    public void setLatLon(boolean isLatLon) { this.isLatLon = isLatLon; }

    /* public boolean isProductSet() { return isProductSet; }
    public void setProductSet(boolean isProductSet) { this.isProductSet = isProductSet; }  */

    public int getDomainRank() { return domainRank; }
    public void setDomainRank(int domainRank) { this.domainRank = domainRank; }

    public int getRangeRank() { return rangeRank; }
    public void setRangeRank(int rangeRank) { this.rangeRank = rangeRank; }

    public int getNGrids() { return ngrids; }
    public void setNGrids(int ngrids) { this.ngrids = ngrids; }

    public String getProjection() { return proj; }
    public void setProjection(String proj) { this.proj = proj; }

    public String getCoordTransforms() { return coordTrans; }
    public void setCoordTransforms(String coordTrans) { this.coordTrans = coordTrans; }
  }

  public class GeoAxisBean {
    // static public String editableProperties() { return "title include logging freq"; }

    CoordinateAxis axis;
    CoordinateSystem firstCoordSys = null;
    String name, desc, units, axisType="", positive= "", incr = "";
    String dims, shape, csNames;
    boolean isCoordVar;

    // no-arg constructor
    public GeoAxisBean() {}

    // create from a dataset
    public GeoAxisBean( CoordinateAxis v) {
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

    public int hashCode() { return axis.hashCode(); }
    public boolean equals( Object o)  { return hashCode() == o.hashCode(); }
  }


  /** Wrap this in a JDialog component.
   *
   * @param parent      JFrame (application) or JApplet (applet) or null
   * @param title       dialog window title
   * @param modal       modal dialog or not
   */
  public JDialog makeDialog( RootPaneContainer parent, String title, boolean modal) {
      return new Dialog( parent, title, modal);
  }

  private class Dialog extends JDialog {

    private Dialog(RootPaneContainer parent, String title, boolean modal) {
      super(parent instanceof Frame ? (Frame) parent : null, title, modal);

      // L&F may change
      UIManager.addPropertyChangeListener( new PropertyChangeListener() {
        public void propertyChange( PropertyChangeEvent e) {
          if (e.getPropertyName().equals("lookAndFeel"))
            SwingUtilities.updateComponentTreeUI( GeoGridTable.Dialog.this);
        }
      });

      /* add a dismiss button
      JButton dismissButton = new JButton("Dismiss");
      buttPanel.add(dismissButton, null);

      dismissButton.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent evt) {
          setVisible(false);
        }
      }); */

     // add it to contentPane
      Container cp = getContentPane();
      cp.setLayout(new BorderLayout());
      cp.add( GeoGridTable.this, BorderLayout.CENTER);
      pack();
    }
  }
}