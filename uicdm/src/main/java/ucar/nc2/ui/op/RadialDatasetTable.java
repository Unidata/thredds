/*
 * Copyright (c) 1998-2019 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.ui.op;

import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.nc2.VariableSimpleIF;
import ucar.nc2.ui.image.ImageViewPanel;
import ucar.ui.widget.BAMutil;
import ucar.ui.widget.IndependentWindow;
import ucar.ui.widget.PopupMenu;
import ucar.ui.widget.TextHistoryPane;
import ucar.nc2.dt.RadialDatasetSweep;
import ucar.nc2.dt.image.image.ImageArrayAdapter;
import ucar.util.prefs.PreferencesExt;
import ucar.ui.prefs.BeanTable;

import java.awt.BorderLayout;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Formatter;
import java.util.List;
import java.lang.invoke.MethodHandles;
import javax.swing.AbstractAction;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;

/**
 * A Swing widget to examine a RadialDataset.
 *
 * @author caron
 */

public class RadialDatasetTable extends JPanel {

    private static final org.slf4j.Logger logger
                = org.slf4j.LoggerFactory.getLogger (MethodHandles.lookup ( ).lookupClass ( ));

  private PreferencesExt prefs;
  private RadialDatasetSweep radialDataset;

  private BeanTable varTable, sweepTable;
  private JSplitPane split;
  private TextHistoryPane infoTA;
  private IndependentWindow infoWindow;

  public RadialDatasetTable(PreferencesExt prefs) {
    this.prefs = prefs;

    varTable = new BeanTable(VariableBean.class, (PreferencesExt) prefs.node("VariableBeans"), false);
    varTable.addListSelectionListener(e -> {
        VariableBean vb = (VariableBean) varTable.getSelectedBean();
        if (vb != null) setVariable(vb);
    });

    JTable jtable = varTable.getJTable();

    PopupMenu csPopup = new PopupMenu(jtable, "Options");
    csPopup.addAction("Show Declaration", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        VariableBean vb = (VariableBean) varTable.getSelectedBean();
        if (vb == null) return;
        VariableSimpleIF v = radialDataset.getDataVariable(vb.getName());
        if (v == null) return;
        infoTA.clear();
        infoTA.appendLine(v.toString());
        infoTA.gotoTop();
        infoWindow.show();
      }
    });
    csPopup.addAction("Show Info", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        VariableBean vb = (VariableBean) varTable.getSelectedBean();
        if (vb == null) return;
        Formatter f = new Formatter();
        showInfo(radialDataset, vb.getName(), f);
        infoTA.clear();
        infoTA.appendLine(f.toString());
        infoTA.gotoTop();
        infoWindow.show();
      }
    });

    // the info window
    infoTA = new TextHistoryPane();
    infoWindow = new IndependentWindow("Variable Information", BAMutil.getImage("nj22/NetcdfUI"), infoTA);
    infoWindow.setBounds((Rectangle) prefs.getBean("InfoWindowBounds", new Rectangle(300, 300, 500, 300)));

    sweepTable = new BeanTable(SweepBean.class, (PreferencesExt) prefs.node("SweepBean"), false);

    PopupMenu sweepPopup = new PopupMenu(sweepTable.getJTable(), "Options");
    sweepPopup.addAction("Show Image", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        showImage((SweepBean) sweepTable.getSelectedBean());
      }
    });

    split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, false, varTable, sweepTable);
    split.setDividerLocation(prefs.getInt("splitPos", 500));

    setLayout(new BorderLayout());
    add(split, BorderLayout.CENTER);
  }

  public PreferencesExt getPrefs() {
    return prefs;
  }

  public void save() {
    varTable.saveState(false);
    prefs.putBeanObject("InfoWindowBounds", infoWindow.getBounds());
    if (split != null) prefs.putInt("splitPos", split.getDividerLocation());
    if (sweepTable != null) sweepTable.saveState(false);
  }

  public void clear() {
    varTable.setBeans(new ArrayList());
    sweepTable.setBeans(new ArrayList());
  }

  public void setDataset(RadialDatasetSweep rds) {
    this.radialDataset = rds;
    // dateUnit = rds.getTimeUnits();

    varTable.setBeans(getVariableBeans(rds));
    sweepTable.setBeans(new ArrayList());
  }

  public RadialDatasetSweep getRadialDataset() {
    return radialDataset;
  }

  public List<VariableBean> getVariableBeans(RadialDatasetSweep rds) {
    List<VariableBean> vlist = new ArrayList<>();
    List list = rds.getDataVariables();
    for (Object aList : list) {
      RadialDatasetSweep.RadialVariable v = (RadialDatasetSweep.RadialVariable) aList;
      vlist.add(new VariableBean(v));
    }
    return vlist;
  }

  public void setVariable(VariableBean vb) {
    List<SweepBean> sweeps = new ArrayList<>();
    int n = vb.v.getNumSweeps();
    for (int i = 0; i < n; i++) {
      RadialDatasetSweep.Sweep sweep = vb.v.getSweep(i);
      sweeps.add(new SweepBean(sweep));
    }
    sweepTable.setBeans(sweeps);
  }

  private void showInfo(RadialDatasetSweep rds, String varName, Formatter f) {
    f.format("Radial Dataset %s%n", rds.getLocation());

      /* radar information */
    //String stationID = rds.getRadarID();
    String stationName = rds.getRadarName();
    boolean isVolume = rds.isVolume();
    f.format("  stationName = %s%n", stationName);
    f.format("  isVolume = %s%n", isVolume);

      /* radial variable */
    RadialDatasetSweep.RadialVariable v = (RadialDatasetSweep.RadialVariable) rds.getDataVariable(varName);
    if (v == null) return;

    f.format("  info for variable = %s%n", varName);
    f.format("  number of sweeps = %d%n", v.getNumSweeps());

    // loop over sweeps
    for (int sweep = 0; sweep < v.getNumSweeps(); sweep++) {
      RadialDatasetSweep.Sweep sw = v.getSweep(sweep);
      float me = sw.getMeanElevation();
      int nrays = sw.getRadialNumber();
      int ngates = sw.getGateNumber();
      f.format("    %d : elev=%f nrays=%d ngates=%d%n", sweep, me, nrays, ngates);

      try {
        for (int j = 0; j < nrays; j++) {
          float azi = sw.getAzimuth(j);
          float ele = sw.getElevation(j);
          float[] data = sw.readData(j);
          f.format("      %d : azimuth=%f elev=%f data len=%d%n", j, azi, ele, data.length);
        }
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }


  public class VariableBean {
    // static public String editableProperties() { return "title include logging freq"; }

    RadialDatasetSweep.RadialVariable v;

    private String name, desc, units, dataType;
    String dims, r, t;
    //private boolean isCoordVar, isRadial, axis;

    // no-arg constructor
    public VariableBean() {
    }

    // create from a dataset
    public VariableBean(RadialDatasetSweep.RadialVariable v) {
      this.v = v;

      setName(v.getShortName());
      setDescription(v.getDescription());
      setUnits(v.getUnitsString());
      dataType = v.getDataType().toString();

      // collect dimensions
      StringBuilder buff = new StringBuilder();
      int[] shape = v.getShape();
      for (int j = 0; j < shape.length; j++) {
        if (j > 0) buff.append(",");
        buff.append(shape[j]);
      }
      dims = buff.toString();
    }

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
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

    public String getDims() {
      return dims;
    }

  }

  public class SweepBean {
    // static public String editableProperties() { return "title include logging freq"; }

    RadialDatasetSweep.Sweep sweep;

    // no-arg constructor
    public SweepBean() {
    }

    // create from a dataset
    public SweepBean(RadialDatasetSweep.Sweep sweep) {
      this.sweep = sweep;

    }

    public String getType() {
      RadialDatasetSweep.Type type = sweep.getType();
      return (type == null) ? "" : type.toString();
    }

    public int getNumRadial() {
      return sweep.getRadialNumber();
    }

    public int getNumGates() {
      return sweep.getGateNumber();
    }

    public float getBeamWidth() {
      return sweep.getBeamWidth();
    }

    public float getNyqFreq() {
      return sweep.getNyquistFrequency();
    }

    public float getFirstGate() {
      return sweep.getRangeToFirstGate();
    }

    public float getGateSize() {
      return sweep.getGateSize();
    }

    public float getMeanElevation() {
      return sweep.getMeanElevation();
    }

    public float getMeanAzimuth() {
      return sweep.getMeanAzimuth();
    }

    public Date getStartingTime() {
      return sweep.getStartingTime();
    }

    public Date getEndingTime() {
      return sweep.getEndingTime();
    }
  }

  // show image
  private static final String ImageViewer_WindowSize = "RadialImageViewer_WindowSize";
  private IndependentWindow imageWindow = null;
  private ImageViewPanel imageView = null;

  private void showImage(SweepBean bean) {
    if (bean == null) return;

    if (imageWindow == null) {
      imageWindow = new IndependentWindow("Image Viewer", BAMutil.getImage("nj22/ImageData"));
      imageView = new ImageViewPanel(null);
      imageWindow.setComponent(new JScrollPane(imageView));
      //imageWindow.setComponent( imageView);
      Rectangle b = (Rectangle) prefs.getBean(ImageViewer_WindowSize, new Rectangle(99, 33, 700, 900));
      //System.out.println("bounds in = "+b);
      imageWindow.setBounds(b);
    }

    float[] data;
    try {
      data = bean.sweep.readData();
      int[] shape = new int[]{bean.getNumRadial(), bean.getNumGates()};
      Array arrData = Array.factory(DataType.FLOAT, shape, data);

      imageView.setImage(ImageArrayAdapter.makeGrayscaleImage(arrData, null));
      imageWindow.show();

    } catch (IOException e) {
      logger.warn("sweep read data failed", e);
    }
  }
}
