// $Id: GeoGridTable.java,v 1.14 2006/06/06 16:07:15 caron Exp $
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
 * @author John Caron
 * @version $Id: GeoGridTable.java,v 1.14 2006/06/06 16:07:15 caron Exp $
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
        String dc = null;
        if (gridDataset.findGridByName(vb.getName()) != null)
          try {
            dc = wcs.describeCoverage(vb.getName());
            infoTA.clear();
            infoTA.appendLine(dc);
            infoTA.gotoTop();
            infoWindow.showIfNotIconified();
          } catch (IOException e1) {
            e1.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            return;
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
      GeoGrid g = (GeoGrid) list.get(i);
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
        GridCoordSys gsys = gset.getGeoCoordSys();
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
  public GeoGrid getGrid() { 
    GeogridBean vb = (GeogridBean) varTable.getSelectedBean();
    if (vb == null) {
      List grids = gridDataset.getGrids();
      if (grids.size() > 0)
        return (GeoGrid) grids.get(0);
      else
        return null;
    }
    return gridDataset.findGridByName( vb.getName());
  }

  public class GeogridBean {
    // static public String editableProperties() { return "title include logging freq"; }

    private String name, desc, units, csys, proj;
    private String dims, x, y, z, t;

    // no-arg constructor
    public GeogridBean() {}

    // create from a dataset
    public GeogridBean( GeoGrid geogrid) {
      setName( geogrid.getName());
      setDescription( geogrid.getDescription());
      setUnits( geogrid.getUnitsString());

      setCoordSystem( geogrid.getCoordinateSystem().getName());

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
    private boolean isGeoXY, isLatLon, isProductSet, isRegular;
    private int ngrids = -1;

    // no-arg constructor
    public GeoCoordinateSystemBean() {}

    public GeoCoordinateSystemBean( GridDataset.Gridset gset) {
      GridCoordSys cs = gset.getGeoCoordSys();
      setName( cs.getName());
      setGeoXY( cs.isGeoXY());
      setLatLon( cs.isLatLon());
      setProductSet( cs.isProductSet());
      setDomainRank( cs.getDomain().size());
      setRangeRank( cs.getCoordinateAxes().size());
      setRegular( cs.isRegularSpatial());

      setNGrids( gset.getGrids().size());

      ProjectionImpl proj = cs.getProjection();
      if (proj != null)
        setProjection(proj.getName());

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

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public boolean isRegular() { return isRegular; }
    public void setRegular(boolean isRegular) { this.isRegular = isRegular; }

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

/* Change History:
   $Log: GeoGridTable.java,v $
   Revision 1.14  2006/06/06 16:07:15  caron
   *** empty log message ***

   Revision 1.13  2006/06/05 22:23:08  caron
   wcs

   Revision 1.12  2005/12/15 00:29:12  caron
   *** empty log message ***

   Revision 1.11  2005/12/02 00:15:38  caron
   NcML 
   Dimension.isVariableLength()

   Revision 1.10  2005/10/18 23:42:50  caron
   2.2.11.02

   Revision 1.9  2005/07/13 22:26:57  caron
   no message

   Revision 1.8  2005/06/23 19:18:44  caron
   no message

   Revision 1.7  2005/05/26 01:58:04  caron
   fix DateRange bugs

   Revision 1.6  2005/05/25 21:09:44  caron
   no message

   Revision 1.5  2005/03/16 17:08:31  caron
   add WCS capabilities to GribTable
   fix WCS output

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