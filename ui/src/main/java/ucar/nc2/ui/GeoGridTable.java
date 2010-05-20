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
import ucar.nc2.Dimension;
import ucar.nc2.constants.AxisType;
import ucar.nc2.dt.GridDatatype;
import ucar.nc2.dt.GridCoordSystem;
import ucar.nc2.dt.GridDataset;
import ucar.nc2.dt.grid.GridCoordSys;
import ucar.nc2.dataset.*;

import ucar.util.prefs.*;
import ucar.util.prefs.ui.*;
import ucar.unidata.geoloc.ProjectionImpl;
import thredds.ui.*;
import thredds.wcs.v1_0_0_1.WcsException;
import thredds.wcs.v1_0_0_1.DescribeCoverage;

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
 */

public class GeoGridTable extends JPanel {
  private PreferencesExt prefs;
  private GridDataset gridDataset;

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
        Variable v = vb.geogrid.getVariable();
        infoTA.clear();
        if (v == null)
          infoTA.appendLine( "Cant find variable "+vb.getName()+" escaped= ("+NetcdfFile.escapeName( vb.getName())+")");
        else {
          infoTA.appendLine( "Variable "+ v.getName()+" :");
          infoTA.appendLine( v.toString());
        }
        infoTA.gotoTop();
        infoWindow.showIfNotIconified();
      }
    });

    csPopup.addAction("WCS DescribeCoverage", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        GeogridBean vb = (GeogridBean) varTable.getSelectedBean();
        if (gridDataset.findGridDatatype(vb.getName()) != null)
        {
          List<String> coverageIdList = Collections.singletonList( vb.getName() );
          try
          {
            DescribeCoverage descCov =
                    ( (thredds.wcs.v1_0_0_1.DescribeCoverageBuilder)
                            thredds.wcs.v1_0_0_1.WcsRequestBuilder
                                    .newWcsRequestBuilder( "1.0.0",
                                                           thredds.wcs.Request.Operation.DescribeCoverage,
                                                           gridDataset, "" ) )
                            .setCoverageIdList( coverageIdList )
                            .buildDescribeCoverage();
            String dc = descCov.writeDescribeCoverageDocAsString();
            infoTA.clear();
            infoTA.appendLine(dc);
            infoTA.gotoTop();
            infoWindow.showIfNotIconified();
          }
          catch (WcsException e1)
          {
            e1.printStackTrace();
          }
          catch (IOException e1)
          {
            e1.printStackTrace();
          }
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

  public void setDataset(NetcdfDataset ds) throws IOException {
    this.gridDataset = new ucar.nc2.dt.grid.GridDataset(ds);

    List<GeogridBean> beanList = new ArrayList<GeogridBean>();
    java.util.List<GridDatatype> list = gridDataset.getGrids();
    for (GridDatatype g : list)
      beanList.add (new GeogridBean( g));
    varTable.setBeans( beanList);

    if (csTable != null) {
      List<GeoCoordinateSystemBean> csList = new ArrayList<GeoCoordinateSystemBean>();
      List<GeoAxisBean> axisList;
      axisList = new ArrayList<GeoAxisBean>();
      for (GridDataset.Gridset gset : gridDataset.getGridsets()) {
        csList.add(new GeoCoordinateSystemBean(gset));
        GridCoordSystem gsys = gset.getGeoCoordSystem();
        List<CoordinateAxis> axes = gsys.getCoordinateAxes();
        for (int i = 0; i < axes.size(); i++) {
          CoordinateAxis axis = axes.get(i);
          GeoAxisBean axisBean = new GeoAxisBean(axis);
          if (!contains(axisList, axisBean.getName()))
            axisList.add(axisBean);
        }
      }
      csTable.setBeans( csList);
      axisTable.setBeans( axisList);
    }
  }

  public void setDataset(GridDataset gds) throws IOException {
    this.gridDataset = gds;

    List<GeogridBean> beanList = new ArrayList<GeogridBean>();
    java.util.List<GridDatatype> list = gridDataset.getGrids();
    for (GridDatatype g : list)
      beanList.add (new GeogridBean( g));
    varTable.setBeans( beanList);

    if (csTable != null) {
      List<GeoCoordinateSystemBean> csList = new ArrayList<GeoCoordinateSystemBean>();
      List<GeoAxisBean> axisList;
      axisList = new ArrayList<GeoAxisBean>();
      for (GridDataset.Gridset gset : gridDataset.getGridsets()) {
        csList.add(new GeoCoordinateSystemBean(gset));
        GridCoordSystem gsys = gset.getGeoCoordSystem();
        List<CoordinateAxis> axes = gsys.getCoordinateAxes();
        for (int i = 0; i < axes.size(); i++) {
          CoordinateAxis axis = axes.get(i);
          GeoAxisBean axisBean = new GeoAxisBean(axis);
          if (!contains(axisList, axisBean.getName()))
            axisList.add(axisBean);
        }
      }
      csTable.setBeans( csList);
      axisTable.setBeans( axisList);
    }
  }

  private boolean contains(List<GeoAxisBean> axisList, String name) {
    for (GeoAxisBean axis : axisList)
      if (axis.getName().equals(name)) return true;
    return false;
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
    return gridDataset.findGridDatatype( vb.getName());
  }

  public class GeogridBean {
    // static public String editableProperties() { return "title include logging freq"; }

    private GridDatatype geogrid;
    private String name, desc, units, csys;
    private String dims, x, y, z, t, ens, rt;

    // no-arg constructor
    public GeogridBean() {}

    // create from a dataset
    public GeogridBean( GridDatatype geogrid) {
      this.geogrid = geogrid;
      setName( geogrid.getName());
      setDescription( geogrid.getDescription());
      setUnits( geogrid.getUnitsString());

      GridCoordSystem gcs = geogrid.getCoordinateSystem();
      setCoordSystem( gcs.getName());

            // collect dimensions
      StringBuffer buff = new StringBuffer();
      java.util.List dims = geogrid.getDimensions();
      for (int j=0; j<dims.size(); j++) {
        ucar.nc2.Dimension dim = (ucar.nc2.Dimension) dims.get(j);
        if (j>0) buff.append(",");
        buff.append(dim.getLength());
      }
      setShape( buff.toString());

      setX(gcs.getXHorizAxis());
      setY(gcs.getYHorizAxis());
      setZ(gcs.getVerticalAxis());
      setT(gcs.getTimeAxis());
      setRt(gcs.getRunTimeAxis());
      setEns(gcs.getEnsembleAxis());

      /* Dimension d= geogrid.getXDimension();
      if (d != null) setX( d.getName());
      d= geogrid.getYDimension();
      if (d != null) setY( d.getName());
      d= geogrid.getZDimension();
      if (d != null) setZ( d.getName());

      GridCoordSystem gcs = geogrid.getCoordinateSystem();

      d= geogrid.getTimeDimension();
      if (d != null)
        setT( d.getName());
      else {
        CoordinateAxis taxis = gcs.getTimeAxis();
        if (taxis != null) setT( taxis.getName());
      }

      CoordinateAxis1D axis = gcs.getEnsembleAxis();
      if (axis != null)
        setEns( axis.getDimension(0).getName());

      axis = gcs.getRunTimeAxis();
      if (axis != null)
        setRt( axis.getDimension(0).getName()); */
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
    private void setX(CoordinateAxis axis) {
      if (axis != null)
        x = axis.getNameAndDimensions(true);
    }

    public String getY() { return y; }
    private void setY(CoordinateAxis axis) { 
      if (axis != null)
        y = axis.getNameAndDimensions(true);
    }

    public String getZ() { return z; }
    private void setZ(CoordinateAxis axis) {
      if (axis != null)
        z = axis.getNameAndDimensions(true);
    }
    public String getT() { return t; }
    private void setT(CoordinateAxis axis) {
      if (axis != null)
        t = axis.getNameAndDimensions(true);
    }
    public String getEns() { return ens; }
    private void setEns(CoordinateAxis axis) {
      if (axis != null)
        ens = axis.getNameAndDimensions(true);
    }
    public String getRt() { return rt; }
    private void setRt(CoordinateAxis axis) {
      if (axis != null)
        rt = axis.getNameAndDimensions(true);
    }
    public String getShape() { return dims; }
    public void setShape(String dims) { this.dims = dims; }

  }

  public class GeoCoordinateSystemBean {
    private GridCoordSystem gcs;
    private String proj, coordTrans;
    private int ngrids = -1;

    // no-arg constructor
    public GeoCoordinateSystemBean() {}

    public GeoCoordinateSystemBean( GridDataset.Gridset gset) {
      gcs = gset.getGeoCoordSystem();

      setNGrids( gset.getGrids().size());

      ProjectionImpl proj = gcs.getProjection();
      if (proj != null)
        setProjection(proj.getClassName());

      int count = 0;
      StringBuffer buff = new StringBuffer();
      List ctList = gcs.getCoordinateTransforms();
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
            buff.append("-").append(pct.getName());
            count++;
          }
        }
      }
      setCoordTransforms( buff.toString());
    }

    public String getName() { return gcs.getName(); }

    public boolean isRegularSpatial() { return gcs.isRegularSpatial(); }
    public boolean isLatLon() { return gcs.isLatLon(); }
    public boolean isGeoXY() { return ((GridCoordSys) gcs).isGeoXY(); }

    /* public boolean isProductSet() { return isProductSet; }
    public void setProductSet(boolean isProductSet) { this.isProductSet = isProductSet; }  */

    public int getDomainRank() { return gcs.getDomain().size(); }
    public int getRangeRank() { return gcs.getCoordinateAxes().size(); }

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
      setCoordVar( v.isCoordinateVariable());
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


  /** Wrap this in a JDialog component.
   *
   * @param parent      JFrame (application) or JApplet (applet) or null
   * @param title       dialog window title
   * @param modal       modal dialog or not
   * @return JDialog
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