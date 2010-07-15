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
import ucar.nc2.units.DateUnit;
import ucar.nc2.units.DateFormatter;
import ucar.nc2.dt.radial.RadialCoordSys;
import ucar.nc2.dataset.*;
import ucar.nc2.constants._Coordinate;
import ucar.nc2.constants.AxisType;
import ucar.nc2.dt.grid.*;

import ucar.util.prefs.*;
import ucar.util.prefs.ui.*;
import ucar.ma2.Array;
import ucar.ma2.IndexIterator;
import ucar.unidata.util.Parameter;
import thredds.ui.*;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;
import java.io.IOException;

import javax.swing.*;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.ListSelectionEvent;

/**
 * A Swing widget to examine Coordinate Systems.
 *
 * @author caron
 */

public class CoordSysTable extends JPanel {
  private PreferencesExt prefs;
  private NetcdfDataset ds;

  private BeanTableSorted varTable, csTable, axisTable;
  private JSplitPane split, split2;
  private TextHistoryPane infoTA;
  private IndependentWindow infoWindow;
  private Formatter parseInfo = new Formatter();

  public CoordSysTable(PreferencesExt prefs) {
    this.prefs = prefs;

    varTable = new BeanTableSorted(VariableBean.class, (PreferencesExt) prefs.node("VariableBeans"), false);
    varTable.addListSelectionListener(new ListSelectionListener() {
      public void valueChanged(ListSelectionEvent e) {
        VariableBean vb = (VariableBean) varTable.getSelectedBean();
        if (null != vb.firstCoordSys)
          setSelectedCoordinateSystem(vb.firstCoordSys);
      }
    });

    csTable = new BeanTableSorted(CoordinateSystemBean.class, (PreferencesExt) prefs.node("CoordinateSystemBean"), false);
    csTable.addListSelectionListener(new ListSelectionListener() {
      public void valueChanged(ListSelectionEvent e) {
        CoordinateSystemBean csb = (CoordinateSystemBean) csTable.getSelectedBean();
        setSelectedCoordinateAxes(csb.coordSys);
      }
    });

    axisTable = new BeanTableSorted(AxisBean.class, (PreferencesExt) prefs.node("CoordinateAxisBean"), false);

    thredds.ui.PopupMenu varPopup = new thredds.ui.PopupMenu(varTable.getJTable(), "Options");
    varPopup.addAction("Show Declaration", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        VariableBean vb = (VariableBean) varTable.getSelectedBean();
        VariableDS v = (VariableDS)  ds.findVariable( vb.getName());
        if (v == null) return;
        infoTA.clear();
        infoTA.appendLine(v.toString());
        infoTA.appendLine(showMissing(v));
        infoTA.gotoTop();
        infoWindow.showIfNotIconified();
      }
    });

    varPopup.addAction("Try as Grid", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        VariableBean vb = (VariableBean) varTable.getSelectedBean();
        VariableEnhanced v = (VariableEnhanced) ds.findVariable( vb.getName());
        if (v == null) return;
        infoTA.clear();
        infoTA.appendLine(tryGrid(v));
        infoTA.gotoTop();
        infoWindow.showIfNotIconified();
      }
    });

    thredds.ui.PopupMenu csPopup = new thredds.ui.PopupMenu(csTable.getJTable(), "Options");
    csPopup.addAction("Show CoordSys", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        CoordinateSystemBean csb = (CoordinateSystemBean) csTable.getSelectedBean();
        CoordinateSystem coordSys = csb.coordSys;
        infoTA.clear();
        infoTA.appendLine("Coordinate System = " + coordSys.getName());
        for (CoordinateAxis axis : coordSys.getCoordinateAxes()) {
          infoTA.appendLine("  " + axis.getAxisType()+" "+axis);
        }
        infoTA.appendLine(" Coordinate Transforms");
        for (CoordinateTransform ct : coordSys.getCoordinateTransforms()) {
          infoTA.appendLine("  " + ct.getName()+" type="+ct.getTransformType());
          for (Parameter p : ct.getParameters()) {
            infoTA.appendLine("    " + p);
          }
          if (ct instanceof ProjectionCT) {
            ProjectionCT pct = (ProjectionCT) ct;
            if (pct.getProjection() != null) {
              infoTA.appendLine("    impl.class= " + pct.getProjection().getClass().getName());
              pct.getProjection();
            }
          }
          if (ct instanceof VerticalCT) {
            VerticalCT vct = (VerticalCT) ct;
            infoTA.appendLine("  VerticalCT= " + vct);
          }

        }
        infoTA.gotoTop();
        infoWindow.showIfNotIconified();
      }
    });

    thredds.ui.PopupMenu axisPopup = new thredds.ui.PopupMenu(axisTable.getJTable(), "Options");
    axisPopup.addAction("Show Declaration", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        AxisBean bean = (AxisBean) axisTable.getSelectedBean();
        CoordinateAxis axis = (CoordinateAxis) ds.findVariable( bean.getName());
        if (axis == null) return;
        infoTA.clear();
        infoTA.appendLine(axis.toString());
        infoTA.appendLine(showMissing(axis));
        infoTA.gotoTop();
        infoWindow.showIfNotIconified();
      }
    });

    axisPopup.addAction("Show Values", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        AxisBean bean = (AxisBean) axisTable.getSelectedBean();
        //CoordinateAxis axis = (CoordinateAxis) ds.findVariable( bean.getName());
        CoordinateAxis axis = bean.axis;
        infoTA.clear();
        try {
          infoTA.appendLine(NCdumpW.printVariableData(axis, null));
          if (axis instanceof CoordinateAxis1D && axis.isNumeric()) {
            CoordinateAxis1D axis1D = (CoordinateAxis1D) axis;
            if (axis1D.isContiguous()) {
              printArray("edges=", axis1D.getCoordEdges());
            } else {
              printArray("bound1=", axis1D.getBound1());
              printArray("bound2=", axis1D.getBound2());
            }
          }
        } catch (IOException e1) {
          e1.printStackTrace();
          infoTA.appendLine(e1.getMessage());
        }
        infoTA.gotoTop();
        infoWindow.showIfNotIconified();
      }
    });
    axisPopup.addAction("Show Value Differences", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        AxisBean bean = (AxisBean) axisTable.getSelectedBean();
        CoordinateAxis axis = (CoordinateAxis) ds.findVariable( bean.getName());
        infoTA.clear();
        try {
          if (axis instanceof CoordinateAxis1D && axis.isNumeric()) {
            CoordinateAxis1D axis1D = (CoordinateAxis1D) axis;
            double[] mids = axis1D.getCoordValues();
            for (int i = 0; i < mids.length - 1; i++)
              mids[i] = mids[i + 1] - mids[i];
            mids[mids.length - 1] = 0.0;

            printArray("midpoint differences=", mids);
          }
        } catch (Exception e1) {
          e1.printStackTrace();
          infoTA.appendLine(e1.getMessage());
        }
        infoTA.gotoTop();
        infoWindow.showIfNotIconified();
      }
    });
    axisPopup.addAction("Show Values as Date", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        AxisBean bean = (AxisBean) axisTable.getSelectedBean();
        VariableEnhanced axis = (VariableEnhanced) ds.findVariable( bean.getName());
        String units = axis.getUnitsString();
        infoTA.clear();

        try {
          infoTA.appendLine(units);
          infoTA.appendLine(NCdumpW.printVariableData(axis, null));

          if (axis.getDataType().isNumeric()) {
            DateFormatter format = new DateFormatter();
            DateUnit du = new DateUnit(units);
            Array data = axis.read();
            IndexIterator ii = data.getIndexIterator();
            while (ii.hasNext()) {
              double val = ii.getDoubleNext();
              if (Double.isNaN(val)) {
                infoTA.appendLine(" N/A");
              } else {
                Date date = du.makeDate(val);
                infoTA.appendLine(" " + format.toDateTimeString(date));
              }
            }
          }
        } catch (Exception ex) {
          ex.printStackTrace();
          infoTA.appendLine(ex.getMessage());
        }

        infoTA.gotoTop();
        infoWindow.showIfNotIconified();
      }
    });

    // the info window
    infoTA = new TextHistoryPane();
    infoWindow = new IndependentWindow("Extra Information", BAMutil.getImage("netcdfUI"), infoTA);
    infoWindow.setBounds((Rectangle) prefs.getBean("InfoWindowBounds", new Rectangle(300, 300, 500, 300)));

    split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, false, varTable, csTable);
    split.setDividerLocation(prefs.getInt("splitPos", 500));

    split2 = new JSplitPane(JSplitPane.VERTICAL_SPLIT, false, split, axisTable);
    split2.setDividerLocation(prefs.getInt("splitPos2", 500));

    setLayout(new BorderLayout());
    add(split2, BorderLayout.CENTER);
  }

  private void printArray(String title, double vals[]) {
    StringBuilder sbuff = new StringBuilder();
    sbuff.append(" ").append(title);
    for (int i = 0; i < vals.length; i++) {
      sbuff.append(" ").append(vals[i]);
    }
    sbuff.append("\n");
    infoTA.appendLine(sbuff.toString());
  }

  public PreferencesExt getPrefs() {
    return prefs;
  }

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
    parseInfo = new Formatter();

    List<VariableBean> beanList = new ArrayList<VariableBean>();
    List<AxisBean> axisList = new ArrayList<AxisBean>();
    setVariables(ds.getVariables(), axisList, beanList);

    varTable.setBeans(beanList);
    axisTable.setBeans(axisList);
    csTable.setBeans(getCoordinateSystemBeans(ds));
  }

  private void setVariables(List<Variable> varList, List<AxisBean> axisList, List<VariableBean> beanList) {
    for (int i = 0; i < varList.size(); i++) {
      VariableEnhanced v = (VariableEnhanced) varList.get(i);
      if (v instanceof CoordinateAxis)
        axisList.add(new AxisBean((CoordinateAxis) v));
      else
        beanList.add(new VariableBean(v));

      if (v instanceof Structure) {
        java.util.List<Variable> nested = ((Structure) v).getVariables();
        setVariables(nested, axisList, beanList);
      }
    }
  }

  public List<CoordinateSystemBean> getCoordinateSystemBeans(NetcdfDataset ds) {
    List<CoordinateSystemBean> vlist = new ArrayList<CoordinateSystemBean>();
    java.util.List<CoordinateSystem> list = ds.getCoordinateSystems();
    for (int i = 0; i < list.size(); i++) {
      CoordinateSystem elem = list.get(i);
      vlist.add(new CoordinateSystemBean(elem));
    }
    return vlist;
  }

  private void setSelectedCoordinateSystem(CoordinateSystem coordSys) {
    List beans = csTable.getBeans();
    for (int i = 0; i < beans.size(); i++) {
      CoordinateSystemBean bean = (CoordinateSystemBean) beans.get(i);
      if (bean.coordSys == coordSys) {
        csTable.setSelectedBean(bean);
        return;
      }
    }
  }

  private void setSelectedCoordinateAxes(CoordinateSystem cs) {
    List axesList = cs.getCoordinateAxes();
    if (axesList.size() == 0) return;
    CoordinateAxis axis = (CoordinateAxis) axesList.get(0);

    List beans = axisTable.getBeans();
    for (int i = 0; i < beans.size(); i++) {
      AxisBean bean = (AxisBean) beans.get(i);
      if (bean.axis == axis) {
        axisTable.setSelectedBean(bean);
        return;
      }
    }
  }

  private String tryGrid(VariableEnhanced v) {
    Formatter buff = new Formatter();
    buff.format("%s:", v.getName());
    List<CoordinateSystem> csList = v.getCoordinateSystems();
    if (csList.size() == 0)
      buff.format(" No Coord System found");
    else {
      for (CoordinateSystem cs : csList) {
        buff.format("%s:", cs.getName());
        if (GridCoordSys.isGridCoordSys(buff, cs, v)) {
          buff.format("GRID OK%n");
        } else {
          buff.format(" NOT GRID");
        }
      }
    }
    return buff.toString();
  }

  private String showMissing(VariableDS v) {
    Formatter buff = new Formatter();
    buff.format("%s:", v.getName());
    EnumSet<NetcdfDataset.Enhance> enhanceMode = v.getEnhanceMode();
    buff.format("enhanceMode= %s%n", enhanceMode);
    v.showScaleMissingProxy(buff);
    return buff.toString();
  }

  public class VariableBean {
    // static public String editableProperties() { return "title include logging freq"; }

    VariableEnhanced ve;
    CoordinateSystem firstCoordSys = null;
    String name, desc, units, axisType = "", positive = "";
    String dims, shape, csNames, dataType = "";
    boolean isCoordVar, axis;

    // no-arg constructor
    public VariableBean() {
    }

    // create from a dataset
    public VariableBean(VariableEnhanced v) {
      this.ve = v;

      setName(v.getName());
      setDescription(v.getDescription());
      setUnits(v.getUnitsString());

      // collect dimensions
      StringBuilder lens = new StringBuilder();
      StringBuilder names = new StringBuilder();
      java.util.List dims = v.getDimensions();
      for (int j = 0; j < dims.size(); j++) {
        ucar.nc2.Dimension dim = (ucar.nc2.Dimension) dims.get(j);
        if (j > 0) {
          lens.append(",");
          names.append(",");
        }
        String name = dim.isShared() ? dim.getName() : "anon";
        names.append(name);
        lens.append(dim.getLength());
      }
      setDims(names.toString());
      setShape(lens.toString());

      StringBuilder buff = new StringBuilder();
      List<CoordinateSystem> csList = v.getCoordinateSystems();
      for (CoordinateSystem cs : csList) {
        if (firstCoordSys == null)
          firstCoordSys = cs;
        else
          buff.append("; ");

        if (cs == null)
          System.out.printf("HEY");
        buff.append(cs.getName());

        Formatter gridBuff = new Formatter();
        if (GridCoordSys.isGridCoordSys(gridBuff, cs, v)) {
            addDataType("grid");

        } /* else if (PointDatasetDefaultHandler.isPointFeatureDataset(ds)) {
          addDataType("point");
        } */

      }
      setCoordSys(buff.toString());
    }

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }

    public String getDataType() {
      return dataType;
    }

    void addDataType(String dt) {
      dataType = dataType + " " + dt;
    }

    public String getDims() {
      return dims;
    }

    public void setDims(String dims) {
      this.dims = dims;
    }

    public String getShape() {
      return shape;
    }

    public void setShape(String shape) {
      this.shape = shape;
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
      this.units = (units == null) ? "null" : units;
    }

    public String getCoordSys() {
      return csNames;
    }

    public void setCoordSys(String csNames) {
      this.csNames = csNames;
    }
  }

  public class CoordinateSystemBean {
    // static public String editableProperties() { return "title include logging freq"; }

    CoordinateSystem coordSys;
    private String name, ctNames, dataType = "";
    private int domainRank, rangeRank;
    private boolean isGeoXY, isLatLon, isProductSet, isRegular;

    // no-arg constructor
    public CoordinateSystemBean() {
    }

    public CoordinateSystemBean(CoordinateSystem cs) {
      this.coordSys = cs;

      setCoordSys(cs.getName());
      setGeoXY(cs.isGeoXY());
      setLatLon(cs.isLatLon());
      setProductSet(cs.isProductSet());
      setRegular(cs.isRegular());
      setDomainRank(cs.getDomain().size());
      setRangeRank(cs.getCoordinateAxes().size());

      if (GridCoordSys.isGridCoordSys(parseInfo, cs, null)) {
        addDataType("grid");
      }

      if (RadialCoordSys.isRadialCoordSys(parseInfo, cs)) {
        addDataType("radial");
      }

      StringBuilder buff = new StringBuilder();
      List ctList = cs.getCoordinateTransforms();
      for (int i = 0; i < ctList.size(); i++) {
        CoordinateTransform ct = (CoordinateTransform) ctList.get(i);
        if (i > 0) buff.append(" ");
        buff.append(ct.getTransformType());
        if (ct instanceof VerticalCT)
          buff.append("(").append(((VerticalCT) ct).getVerticalTransformType()).append(")");
        if (ct instanceof ProjectionCT) {
          ProjectionCT pct = (ProjectionCT) ct;
          if (pct.getProjection() != null) {
            buff.append("(").append(pct.getProjection().getClassName()).append(")");
          }
        }
      }
      setCoordTransforms(buff.toString());
    }

    public String getCoordSys() {
      return name;
    }

    public void setCoordSys(String name) {
      this.name = name;
    }

    public boolean isGeoXY() {
      return isGeoXY;
    }

    public void setGeoXY(boolean isGeoXY) {
      this.isGeoXY = isGeoXY;
    }

    public boolean getLatLon() {
      return isLatLon;
    }

    public void setLatLon(boolean isLatLon) {
      this.isLatLon = isLatLon;
    }

    public boolean isProductSet() {
      return isProductSet;
    }

    public void setProductSet(boolean isProductSet) {
      this.isProductSet = isProductSet;
    }

    public boolean isRegular() {
      return isRegular;
    }

    public void setRegular(boolean isRegular) {
      this.isRegular = isRegular;
    }

    public int getDomainRank() {
      return domainRank;
    }

    public void setDomainRank(int domainRank) {
      this.domainRank = domainRank;
    }

    public int getRangeRank() {
      return rangeRank;
    }

    public void setRangeRank(int rangeRank) {
      this.rangeRank = rangeRank;
    }

    public String getCoordTransforms() {
      return ctNames;
    }

    public void setCoordTransforms(String ctNames) {
      this.ctNames = ctNames;
    }

    public String getDataType() {
      return dataType;
    }

    void addDataType(String dt) {
      dataType = dataType + " " + dt;
    }

    public boolean isImplicit() {
      return coordSys.isImplicit();
    }
  }

  public class AxisBean {
    // static public String editableProperties() { return "title include logging freq"; }

    CoordinateAxis axis;
    CoordinateSystem firstCoordSys = null;
    String name, desc, units, axisType = "", positive = "", incr = "";
    String dims, shape, csNames;
    boolean isCoordVar, isLayer;

    // no-arg constructor
    public AxisBean() {
    }

    // create from a dataset
    public AxisBean(CoordinateAxis v) {
      this.axis = v;

      setName(v.getNameEscaped());
      setCoordVar(v.isCoordinateVariable());
      setDescription(v.getDescription());
      setUnits(v.getUnitsString());

      // collect dimensions
      StringBuilder lens = new StringBuilder();
      StringBuilder names = new StringBuilder();
      java.util.List dims = v.getDimensions();
      for (int j = 0; j < dims.size(); j++) {
        ucar.nc2.Dimension dim = (ucar.nc2.Dimension) dims.get(j);
        if (j > 0) {
          lens.append(",");
          names.append(",");
        }
        String name = dim.isShared() ? dim.getName() : "anon";
        names.append(name);
        lens.append(dim.getLength());
      }
      setDims(names.toString());
      setShape(lens.toString());

      AxisType at = v.getAxisType();
      if (at != null)
        setAxisType(at.toString());
      String p = v.getPositive();
      if (p != null)
        setPositive(p);

      if (v instanceof CoordinateAxis1D) {
        CoordinateAxis1D v1 = (CoordinateAxis1D) v;
        if (v1.isRegular())
          setRegular(Double.toString(v1.getIncrement()));
        isLayer = (null != axis.findAttribute(_Coordinate.ZisLayer));
      }
    }

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }

    public boolean isCoordVar() {
      return isCoordVar;
    }

    public void setCoordVar(boolean isCoordVar) {
      this.isCoordVar = isCoordVar;
    }

    public boolean isContig() {
      return axis.isContiguous();
    }

    public boolean isLayer() {
      return isLayer;
    }

    public String getShape() {
      return shape;
    }

    public void setShape(String shape) {
      this.shape = shape;
    }

    public String getAxisType() {
      return axisType;
    }

    public void setAxisType(String axisType) {
      this.axisType = axisType;
    }

    public String getDims() {
      return dims;
    }

    public void setDims(String dims) {
      this.dims = dims;
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
      this.units = (units == null) ? "null" : units;
    }

    public String getPositive() {
      return positive;
    }

    public void setPositive(String positive) {
      this.positive = positive;
    }

    public String getRegular() {
      return incr;
    }

    public void setRegular(String incr) {
      this.incr = incr;
    }
  }

}