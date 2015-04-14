/*
 * Copyright 1998-2014 University Corporation for Atmospheric Research/Unidata
 *
 *   Portions of this software were developed by the Unidata Program at the
 *   University Corporation for Atmospheric Research.
 *
 *   Access and use of this software shall impose the following obligations
 *   and understandings on the user. The user is granted the right, without
 *   any fee or cost, to use, copy, modify, alter, enhance and distribute
 *   this software, and any derivative works thereof, and its supporting
 *   documentation for any purpose whatsoever, provided that this entire
 *   notice appears in all copies of the software, derivative works and
 *   supporting documentation.  Further, UCAR requests that the user credit
 *   UCAR/Unidata in any publications that result from the use of this
 *   software or in any product that includes this software. The names UCAR
 *   and/or Unidata, however, may not be used in any advertising or publicity
 *   to endorse or promote any products or commercial entity unless specific
 *   written permission is obtained from UCAR/Unidata. The user also
 *   understands that UCAR/Unidata is not obligated to provide the user with
 *   any support, consulting, training or assistance of any kind with regard
 *   to the use, operation and performance of this software nor to provide
 *   the user with any updates, revisions, new versions or "bug fixes."
 *
 *   THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 *   IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *   WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *   DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 *   INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 *   FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 *   NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 *   WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */

package ucar.nc2.ui;

import ucar.ma2.*;
import ucar.ma2.DataType;
import ucar.nc2.*;
import ucar.nc2.constants.CDM;
import ucar.nc2.ft.cover.impl.CoverageCSFactory;
import ucar.nc2.time.*;
import ucar.nc2.ui.widget.*;
import ucar.nc2.ui.widget.PopupMenu;
import ucar.nc2.dt.radial.RadialCoordSys;
import ucar.nc2.dataset.*;
import ucar.nc2.constants._Coordinate;
import ucar.nc2.constants.AxisType;
import ucar.nc2.dt.grid.*;
import ucar.unidata.util.Parameter;
import ucar.util.prefs.PreferencesExt;
import ucar.util.prefs.ui.*;

import java.awt.*;
import java.awt.event.*;
import java.io.PrintWriter;
import java.io.StringWriter;
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

  private BeanTable varTable, csTable, axisTable;
  private JSplitPane split, split2;
  private TextHistoryPane infoTA;
  private IndependentWindow infoWindow, attWindow;
  private Formatter parseInfo = new Formatter();

  public CoordSysTable(PreferencesExt prefs, JPanel buttPanel) {
    this.prefs = prefs;

    if (buttPanel != null) {
      AbstractAction attAction = new AbstractAction() {
        public void actionPerformed(ActionEvent e) {
          showAtts();
        }
      };
      BAMutil.setActionProperties(attAction, "FontDecr", "global attributes", false, 'A', -1);
      BAMutil.addActionToContainer(buttPanel, attAction);
    }

    varTable = new BeanTable(VariableBean.class, (PreferencesExt) prefs.node("VariableBeans"), false);
    varTable.addListSelectionListener(new ListSelectionListener() {
      public void valueChanged(ListSelectionEvent e) {
        VariableBean vb = (VariableBean) varTable.getSelectedBean();
        if (null != vb.firstCoordSys)
          setSelectedCoordinateSystem(vb.firstCoordSys);
      }
    });

    csTable = new BeanTable(CoordinateSystemBean.class, (PreferencesExt) prefs.node("CoordinateSystemBean"), false);
    csTable.addListSelectionListener(new ListSelectionListener() {
      public void valueChanged(ListSelectionEvent e) {
        CoordinateSystemBean csb = (CoordinateSystemBean) csTable.getSelectedBean();
        setSelectedCoordinateAxes(csb.coordSys);
      }
    });

    axisTable = new BeanTable(AxisBean.class, (PreferencesExt) prefs.node("CoordinateAxisBean"), false);

    ucar.nc2.ui.widget.PopupMenu varPopup = new PopupMenu(varTable.getJTable(), "Options");
    varPopup.addAction("Show Declaration", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        VariableBean vb = (VariableBean) varTable.getSelectedBean();
        Variable v = ds.findVariable(vb.getName());
        if (v == null) return;
        infoTA.clear();
        infoTA.appendLine(v.toString());
        infoTA.appendLine(showMissing(v));
        infoTA.gotoTop();
        infoWindow.show();
      }
    });

    varPopup.addAction("Try as Grid", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        VariableBean vb = (VariableBean) varTable.getSelectedBean();
        VariableEnhanced v = (VariableEnhanced) ds.findVariable(vb.getName());
        if (v == null) return;
        infoTA.clear();
        infoTA.appendLine(tryGrid(v));
        infoTA.gotoTop();
        infoWindow.show();
      }
    });

    PopupMenu csPopup = new PopupMenu(csTable.getJTable(), "Options");
    csPopup.addAction("Show CoordSys", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        CoordinateSystemBean csb = (CoordinateSystemBean) csTable.getSelectedBean();
        CoordinateSystem coordSys = csb.coordSys;
        infoTA.clear();
        infoTA.appendLine("Coordinate System = " + coordSys.getName());
        for (CoordinateAxis axis : coordSys.getCoordinateAxes()) {
          infoTA.appendLine("  " + axis.getAxisType() + " " + axis.writeCDL(true, false));
        }
        infoTA.appendLine(" Coordinate Transforms");
        for (CoordinateTransform ct : coordSys.getCoordinateTransforms()) {
          infoTA.appendLine("  " + ct.getName() + " type=" + ct.getTransformType());
          for (Parameter p : ct.getParameters()) {
            infoTA.appendLine("    " + p);
          }
          if (ct instanceof ProjectionCT) {
            ProjectionCT pct = (ProjectionCT) ct;
            if (pct.getProjection() != null) {
              infoTA.appendLine("    impl.class= " + pct.getProjection().getClass().getName());
              // pct.getProjection();
            }
          }
          if (ct instanceof VerticalCT) {
            VerticalCT vct = (VerticalCT) ct;
            infoTA.appendLine("  VerticalCT= " + vct);
          }

        }
        infoTA.gotoTop();
        infoWindow.show();
      }
    });

    PopupMenu axisPopup = new PopupMenu(axisTable.getJTable(), "Options");
    axisPopup.addAction("Show Declaration", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        AxisBean bean = (AxisBean) axisTable.getSelectedBean();
        if (bean == null) return;
        VariableDS axis = (VariableDS) ds.findVariable(bean.getName());
        if (axis == null) return;
        infoTA.clear();
        infoTA.appendLine(axis.toString());
        infoTA.appendLine(showMissing(axis));
        infoTA.gotoTop();
        infoWindow.show();
      }
    });

    axisPopup.addAction("Show Values", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        AxisBean bean = (AxisBean) axisTable.getSelectedBean();
        if (bean == null) return;
        infoTA.clear();
        showValues(bean.axis);
        infoTA.gotoTop();
        infoWindow.show();
      }
    });
    axisPopup.addAction("Show Value Differences", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        AxisBean bean = (AxisBean) axisTable.getSelectedBean();
        if (bean == null) return;
        infoTA.clear();
        showValueDiffs(bean.axis);
        infoTA.gotoTop();
        infoWindow.show();
      }
    });

    axisPopup.addAction("Show Values as Date", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        AxisBean bean = (AxisBean) axisTable.getSelectedBean();
        if (bean == null) return;
        infoTA.clear();
        showValuesAsDates(bean.axis);
        infoTA.gotoTop();
        infoWindow.show();
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

  public void summaryInfo(Formatter f) {
    if (ds == null) return;
    f.format("%s%n", ds.getLocation());
    int ngrids = 0;

    for (Object varo : varTable.getBeans()) {
      VariableBean varBean = (VariableBean) varo;
      if (varBean.getDataType().trim().equalsIgnoreCase("grid"))
        ngrids++;
    }
    int ncoordSys = csTable.getBeans().size();
    int ncoords = axisTable.getBeans().size();

    f.format(" ngrids=%d, ncoords=%d, ncoordSys=%d%n", ngrids, ncoords, ncoordSys);
  }

  private BeanTable attTable;
  public void showAtts() {
    if (ds == null) return;
    if (attTable == null) {
      // global attributes
      attTable = new BeanTable(AttributeBean.class, (PreferencesExt) prefs.node("AttributeBeans"), false);
      PopupMenu varPopup = new ucar.nc2.ui.widget.PopupMenu(attTable.getJTable(), "Options");
      varPopup.addAction("Show Attribute", new AbstractAction() {
        public void actionPerformed(ActionEvent e) {
          AttributeBean bean = (AttributeBean) attTable.getSelectedBean();
          if (bean != null) {
            infoTA.setText(bean.att.toString());
            infoTA.gotoTop();
            infoWindow.show();
          }
        }
      });
      attWindow = new IndependentWindow("Global Attributes", BAMutil.getImage( "netcdfUI"), attTable);
      attWindow.setBounds( (Rectangle) prefs.getBean("AttWindowBounds", new Rectangle( 300, 100, 500, 800)));
    }

    List<AttributeBean> attlist = new ArrayList<>();
    for (Attribute att : ds.getGlobalAttributes()) {
      attlist.add(new AttributeBean(att));
    }
    attTable.setBeans(attlist);
    attWindow.show();
  }

  private void showValues(CoordinateAxis axis) {

    try {

      if (axis instanceof CoordinateAxis1D && axis.isNumeric()) {
        CoordinateAxis1D axis1D = (CoordinateAxis1D) axis;
        printArray("midpoints=", axis1D.getCoordValues());

        if (!axis1D.isInterval()) {
          printArray("edges=", axis1D.getCoordEdges());

        } else {
          printArray("bound1=", axis1D.getBound1());
          printArray("bound2=", axis1D.getBound2());

          Formatter f = new Formatter();
          double[] mid = axis1D.getCoordValues();
          double[] b1 = axis1D.getBound1();
          double[] b2 = axis1D.getBound2();
          for (int i = 0; i < b1.length; i++) {
            f.format("%f (%f,%f) = %f%n", mid[i], b1[i], b2[i], b2[i] - b1[i]);
          }
          infoTA.appendLine(f.toString());
        }

      } else if (axis instanceof CoordinateAxis2D && axis.isNumeric()) {
        infoTA.appendLine(NCdumpW.printVariableData(axis, null));
        showValues2D((CoordinateAxis2D) axis);

      } else {
        infoTA.appendLine(NCdumpW.printVariableData(axis, null));
      }

    } catch (IOException e1) {
      e1.printStackTrace();
      infoTA.appendLine(e1.getMessage());
    }

  }


  private void showValues2D(CoordinateAxis2D axis2D) {

    Formatter f = new Formatter();

    if (axis2D.isInterval()) {
      ArrayDouble.D2 coords = axis2D.getCoordValuesArray();
      ArrayDouble.D3 bounds = axis2D.getCoordBoundsArray();
      if (bounds == null) {
        infoTA.appendLine("No bounds for interval " + axis2D.getFullName());
        return;
      }

      IndexIterator coordIter = coords.getIndexIterator();
      IndexIterator boundsIter = bounds.getIndexIterator();
      while (coordIter.hasNext()) {
        double coordValue = coordIter.getDoubleNext();
        if (!boundsIter.hasNext()) break;
        double bounds1 = boundsIter.getDoubleNext();
        if (!boundsIter.hasNext()) break;
        double bounds2 = boundsIter.getDoubleNext();
        f.format("%f (%f,%f) = %f%n", coordValue, bounds1, bounds2, bounds2 - bounds1);
      }

    } else {
      ArrayDouble.D2 coords = axis2D.getCoordValuesArray();
      IndexIterator coordIter = coords.getIndexIterator();
      while (coordIter.hasNext()) {
        double coordValue = coordIter.getDoubleNext();
        f.format("%f%n", coordValue);
      }

    }

    infoTA.appendLine(f.toString());
  }



  private void showValueDiffs(CoordinateAxis axis) {
    if (!axis.isNumeric()) return;
    try {
      if (axis instanceof CoordinateAxis1D) {
        CoordinateAxis1D axis1D = (CoordinateAxis1D) axis;
        double[] mids = axis1D.getCoordValues();
        double[] diffs = new double[mids.length];
        for (int i = 0; i < mids.length - 1; i++)
          diffs[i] = mids[i + 1] - mids[i];
        printArrays("midpoint differences", mids, diffs);

      } else if (axis instanceof CoordinateAxis2D) {
        CoordinateAxis2D axis2D = (CoordinateAxis2D) axis;
        ArrayDouble.D2 mids = axis2D.getCoordValuesArray();
        int[] shape = mids.getShape();
        ArrayDouble.D2 diffx = (ArrayDouble.D2) Array.factory(DataType.DOUBLE, new int[]{shape[0], shape[1] - 1});
        for (int j = 0; j < shape[0]; j++) {
          for (int i = 0; i < shape[1] - 1; i++) {
            double diff = mids.get(j, i + 1) - mids.get(j, i);
            diffx.set(j, i, diff);
          }
        }
        infoTA.appendLine(NCdumpW.toString(diffx, "diff in x", null));

        ArrayDouble.D2 diffy = (ArrayDouble.D2) Array.factory(DataType.DOUBLE, new int[]{shape[0] - 1, shape[1]});
        for (int j = 0; j < shape[0] - 1; j++) {
          for (int i = 0; i < shape[1]; i++) {
            double diff = mids.get(j + 1, i) - mids.get(j, i);
            diffy.set(j, i, diff);
          }
        }
        infoTA.appendLine("\n\n\n");
        infoTA.appendLine(NCdumpW.toString(diffy, "diff in y", null));

      }

    } catch (Exception e1) {
      e1.printStackTrace();
      infoTA.appendLine(e1.getMessage());
    }
  }

  private void showValuesAsDates(CoordinateAxis axis) {
    String units = axis.getUnitsString();
    String cal = getCalendarAttribute(axis);
    CalendarDateUnit cdu = CalendarDateUnit.of(cal, units);

    try {
      infoTA.appendLine(units);
      infoTA.appendLine(NCdumpW.printVariableData(axis, null));

      if (axis.getDataType().isNumeric()) {
        if (axis instanceof CoordinateAxis2D) {
          showDates2D((CoordinateAxis2D) axis, cdu);

        } else if (axis instanceof CoordinateAxis1D) {  // 1D
          showDates1D((CoordinateAxis1D) axis, cdu);

        } else { // > 2D
          Array data = axis.read();
          IndexIterator ii = data.getIndexIterator();
          while (ii.hasNext()) {
            double val = ii.getDoubleNext();
            infoTA.appendLine(makeCalendarDateStringOrMissing(cdu, val));
          }
        }

      } else { // must be iso dates
        Array data = axis.read();
        Formatter f = new Formatter();
        if (data instanceof ArrayChar) {
          ArrayChar dataS = (ArrayChar) data;
          ArrayChar.StringIterator iter = dataS.getStringIterator();
          while (iter.hasNext())
            f.format(" %s%n", iter.next());
          infoTA.appendLine(f.toString());

        } else if (data instanceof ArrayObject) {
          IndexIterator iter = data.getIndexIterator();
          while (iter.hasNext())
            f.format(" %s%n", iter.next());
          infoTA.appendLine(f.toString());
        }

      }
    } catch (Exception ex) {
      ex.printStackTrace();
      infoTA.appendLine(ex.getMessage());
    }
  }

  private void showDates2D(CoordinateAxis2D axis2D, CalendarDateUnit cdu) {
    Formatter f = new Formatter();

    if (axis2D.isInterval()) {
      ArrayDouble.D2 coords = axis2D.getCoordValuesArray();
      ArrayDouble.D3 bounds = axis2D.getCoordBoundsArray();
      if (bounds == null) {
        infoTA.appendLine("No bounds for interval " + axis2D.getFullName());
        return;
      }

      IndexIterator coordIter = coords.getIndexIterator();
      IndexIterator boundsIter = bounds.getIndexIterator();
      while (coordIter.hasNext()) {
        double coordValue = coordIter.getDoubleNext();
        if (!boundsIter.hasNext()) break;
        double bounds1 = boundsIter.getDoubleNext();
        if (!boundsIter.hasNext()) break;
        double bounds2 = boundsIter.getDoubleNext();
        f.format("%s (%s,%s)%n", makeCalendarDateStringOrMissing(cdu, coordValue),
                makeCalendarDateStringOrMissing(cdu, bounds1),
                makeCalendarDateStringOrMissing(cdu, bounds2));
      }

    } else {
      ArrayDouble.D2 coords = axis2D.getCoordValuesArray();
      IndexIterator coordIter = coords.getIndexIterator();
      while (coordIter.hasNext()) {
        double coordValue = coordIter.getDoubleNext();
        f.format("%s%n", makeCalendarDateStringOrMissing(cdu, coordValue));
      }

    }

    infoTA.appendLine(f.toString());
  }

  private String makeCalendarDateStringOrMissing(CalendarDateUnit cdu, double value) {
    if (Double.isNaN(value)) return "missing";
    return cdu.makeCalendarDate(value).toString();
  }

  private String getCalendarAttribute(VariableEnhanced vds) {
    Attribute cal = vds.findAttribute("calendar");
    return (cal == null) ? null : cal.getStringValue();
  }

  private void showDates1D(CoordinateAxis1D axis1D, CalendarDateUnit cdu) {
    if (!axis1D.isInterval()) {
      for (double val : axis1D.getCoordValues()) {
        if (Double.isNaN(val)) infoTA.appendLine(" N/A");
        else infoTA.appendLine(" " + cdu.makeCalendarDate(val));
      }
    } else { // is interval
      Formatter f = new Formatter();
      double[] b1 = axis1D.getBound1();
      double[] b2 = axis1D.getBound2();
      for (int i = 0; i < b1.length; i++)
        f.format(" (%f, %f) == (%s, %s)%n", b1[i], b2[i], cdu.makeCalendarDate((b1[i])), cdu.makeCalendarDate((b2[i])));
      infoTA.appendLine(f.toString());
    }
  }

  private void printArray(String title, double vals[]) {
    Formatter sbuff = new Formatter();
    sbuff.format(" %s=", title);
    for (double val : vals) {
      sbuff.format(" %f", val);
    }
    sbuff.format("%n");
    infoTA.appendLine(sbuff.toString());
  }

  private void printArrays(String title, double vals[], double vals2[]) {
    Formatter sbuff = new Formatter();
    sbuff.format(" %s%n", title);
    for (int i=0; i<vals.length; i++) {
      sbuff.format(" %3d: %10.2f  %10.2f%n", i, vals[i], vals2[i]);
    }
    sbuff.format("%n");
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
    prefs.putBeanObject("InfoWindowBounds", infoWindow.getBounds());
    if (attWindow != null) prefs.putBeanObject("AttWindowBounds", attWindow.getBounds());
  }

  public void clear() {
    this.ds = null;
    varTable.clearBeans();
    axisTable.clearBeans();
    csTable.clearBeans();
  }

  public void setDataset(NetcdfDataset ds) {
    this.ds = ds;
    parseInfo = new Formatter();

    List<VariableBean> beanList = new ArrayList<>();
    List<AxisBean> axisList = new ArrayList<>();
    setVariables(ds.getVariables(), axisList, beanList);

    varTable.setBeans(beanList);
    axisTable.setBeans(axisList);
    csTable.setBeans(getCoordinateSystemBeans(ds));
  }

  private void setVariables(List<Variable> varList, List<AxisBean> axisList, List<VariableBean> beanList) {
    for (Variable aVarList : varList) {
      VariableEnhanced v = (VariableEnhanced) aVarList;
      if (v instanceof CoordinateAxis)
        axisList.add(new AxisBean((CoordinateAxis) v));
      else
        beanList.add(new VariableBean(v));

      if (v instanceof Structure) {
        List<Variable> nested = ((Structure) v).getVariables();
        setVariables(nested, axisList, beanList);
      }
    }
  }

  public List<CoordinateSystemBean> getCoordinateSystemBeans(NetcdfDataset ds) {
    List<CoordinateSystemBean> vlist = new ArrayList<>();
    for (CoordinateSystem elem : ds.getCoordinateSystems()) {
      vlist.add(new CoordinateSystemBean(elem));
    }
    return vlist;
  }

  private void setSelectedCoordinateSystem(CoordinateSystem coordSys) {
    List beans = csTable.getBeans();
    for (Object bean1 : beans) {
      CoordinateSystemBean bean = (CoordinateSystemBean) bean1;
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
    for (Object bean1 : beans) {
      AxisBean bean = (AxisBean) bean1;
      if (bean.axis == axis) {
        axisTable.setSelectedBean(bean);
        return;
      }
    }
  }

  private String tryGrid(VariableEnhanced v) {
    Formatter buff = new Formatter();
    buff.format("%s:", v.getFullName());
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

  private String showMissing(Variable v) {
    if (!(v instanceof VariableDS)) return "";
    VariableDS ve = (VariableDS) v;
    Formatter buff = new Formatter();
    buff.format("%s:", v.getFullName());
    EnumSet<NetcdfDataset.Enhance> enhanceMode = ve.getEnhanceMode();
    buff.format("enhanceMode= %s%n", enhanceMode);
    ve.showScaleMissingProxy(buff);
    return buff.toString();
  }

  public class VariableBean {
    // static public String editableProperties() { return "title include logging freq"; }

    VariableEnhanced ve;
    CoordinateSystem firstCoordSys = null;
    String name, desc, units;
    String dims, shape, csNames, dataType = "";

    // no-arg constructor

    public VariableBean() {
    }

    // create from a dataset

    public VariableBean(VariableEnhanced v) {
      this.ve = v;

      setName(v.getFullName());
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
        String name = dim.isShared() ? dim.getShortName() : "anon";
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

    public String getAbbrev() {
      Attribute att = ve.findAttributeIgnoreCase(CDM.ABBREV);
      return (att == null) ? null : att.getStringValue();
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
    private String name, ctNames, dataType = "", coverageType;
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

      coverageType = CoverageCSFactory.describe(null, cs);

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

    public String getCoverage() {
      return coverageType;
    }
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
      return NCdumpW.toString(value, null, null);
    }

  }

  public class AxisBean {
    // static public String editableProperties() { return "title include logging freq"; }

    CoordinateAxis axis;
    CoordinateSystem firstCoordSys = null;
    String name, desc, units, axisType = "", positive = "", incr = "";
    String dims, shape;
    boolean isCoordVar;
    boolean isLayer;
    boolean isInterval;

    // no-arg constructor

    public AxisBean() {
    }

    // create from a dataset

    public AxisBean(CoordinateAxis v) {
      this.axis = v;

      setName(v.getFullName());
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
        String name = dim.isShared() ? dim.getShortName() : "anon";
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
        if (v1.isRegular()) setRegular(Double.toString(v1.getIncrement()));
      }
      isLayer = (null != axis.findAttribute(_Coordinate.ZisLayer));
      isInterval = axis.isInterval();
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

    /* public boolean isLayer() {
      return isLayer;
    } */

    public boolean isInterval() {
      return isInterval;
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