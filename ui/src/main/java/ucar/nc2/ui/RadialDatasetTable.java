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
import ucar.nc2.ui.image.ImageViewPanel;
import ucar.nc2.units.DateUnit;
import ucar.nc2.dt.RadialDatasetSweep;
import ucar.nc2.dt.image.ImageArrayAdapter;

import ucar.util.prefs.*;
import ucar.util.prefs.ui.*;
import ucar.ma2.Array;
import ucar.ma2.DataType;
import thredds.ui.*;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;
import java.io.PrintStream;

import javax.swing.*;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.ListSelectionEvent;

/**
 * A Swing widget to examine a RadialDataset.
 *
 * @author caron
 */

public class RadialDatasetTable extends JPanel {
  private PreferencesExt prefs;
  private RadialDatasetSweep radialDataset;
  private DateUnit dateUnit;

  private BeanTableSorted varTable, sweepTable = null;
  private JSplitPane split = null;
  private TextHistoryPane infoTA;
  private IndependentWindow infoWindow;

  public RadialDatasetTable(PreferencesExt prefs) {
    this.prefs = prefs;

    varTable = new BeanTableSorted(VariableBean.class, (PreferencesExt) prefs.node("VariableBeans"), false);
    varTable.addListSelectionListener(new ListSelectionListener() {
      public void valueChanged(ListSelectionEvent e) {
        VariableBean vb = (VariableBean) varTable.getSelectedBean();
        setVariable( vb);
      }
    });

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

    sweepTable = new BeanTableSorted(SweepBean.class, (PreferencesExt) prefs.node("SweepBean"), false);

    thredds.ui.PopupMenu sweepPopup = new thredds.ui.PopupMenu(sweepTable.getJTable(), "Options");
    sweepPopup.addAction("Show Image", new AbstractAction() {
       public void actionPerformed(ActionEvent e) {
         showImage(  (SweepBean) sweepTable.getSelectedBean());
       }
     });

    split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, false, varTable, sweepTable);
    split.setDividerLocation(prefs.getInt("splitPos", 500));

    setLayout(new BorderLayout());
    add(split, BorderLayout.CENTER);
  }

  public PreferencesExt getPrefs() { return prefs; }

  public void save() {
    varTable.saveState(false);
    prefs.putBeanObject("InfoWindowBounds", infoWindow.getBounds());
    if (split != null) prefs.putInt("splitPos", split.getDividerLocation());
    if (sweepTable != null) sweepTable.saveState(false);
  }

  public void clear() {
    varTable.setBeans( new ArrayList());
    sweepTable.setBeans( new ArrayList());
  }

  public void setDataset(RadialDatasetSweep rds) {
    this.radialDataset = rds;
    dateUnit = rds.getTimeUnits();

    varTable.setBeans( getVariableBeans(rds));
    sweepTable.setBeans( new ArrayList());
  }

  public RadialDatasetSweep getRadialDataset() { return radialDataset; }

  public List<VariableBean> getVariableBeans(RadialDatasetSweep rds) {
    List<VariableBean> vlist = new ArrayList<VariableBean>();
    java.util.List list = rds.getDataVariables();
    for (int i=0; i<list.size(); i++) {
      RadialDatasetSweep.RadialVariable v = (RadialDatasetSweep.RadialVariable) list.get(i);
      vlist.add( new VariableBean( v));
    }
    return vlist;
  }

  public void setVariable(VariableBean vb) {
    List<SweepBean> sweeps = new ArrayList<SweepBean>();
    int n = vb.v.getNumSweeps();
    for (int i=0; i<n; i++) {
      RadialDatasetSweep.Sweep sweep = vb.v.getSweep(i);
      sweeps.add( new SweepBean( sweep));
    }
    sweepTable.setBeans( sweeps);
  }

  public class VariableBean {
    // static public String editableProperties() { return "title include logging freq"; }

    RadialDatasetSweep.RadialVariable v;

    private String name, desc, units, dataType;
    String dims, r, elev, azi, t;
    //private boolean isCoordVar, isRadial, axis;

    // no-arg constructor
    public VariableBean() {}

    // create from a dataset
    public VariableBean( RadialDatasetSweep.RadialVariable v) {
      this.v = v;

      setName( v.getName());
      setDescription( v.getDescription());
      setUnits( v.getUnitsString());
      dataType = v.getDataType().toString();

            // collect dimensions
      StringBuffer buff = new StringBuffer();
      int[] shape = v.getShape();
      for (int j=0; j<shape.length; j++) {
        if (j>0) buff.append(",");
        buff.append(shape[j]);
      }
      dims = buff.toString();
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return desc; }
    public void setDescription(String desc) { this.desc = desc; }

    public String getUnits() { return units; }
    public void setUnits(String units) { this.units = units; }

    public String getDataType() { return dataType; }
    public String getDims() { return dims; }
  }

  public class SweepBean {
    // static public String editableProperties() { return "title include logging freq"; }

    RadialDatasetSweep.Sweep sweep;

    // no-arg constructor
    public SweepBean() {}

    // create from a dataset
    public SweepBean( RadialDatasetSweep.Sweep sweep) {
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

  private void showImage( SweepBean bean) {
    if (bean == null) return;

    if (imageWindow == null) {
        imageWindow = new IndependentWindow("Image Viewer", BAMutil.getImage("ImageData"));
        imageView = new ImageViewPanel( null);
        imageWindow.setComponent( new JScrollPane(imageView));
        //imageWindow.setComponent( imageView);
        Rectangle b = (Rectangle) prefs.getBean(ImageViewer_WindowSize, new Rectangle(99, 33, 700, 900));
        //System.out.println("bounds in = "+b);
        imageWindow.setBounds( b);
      }

    float[] data;
    try {
      data = bean.sweep.readData();
      int[] shape = new int[] {bean.getNumRadial(), bean.getNumGates()};
      Array arrData = Array.factory( DataType.FLOAT.getPrimitiveClassType(), shape, data);

      imageView.setImage( ImageArrayAdapter.makeGrayscaleImage(arrData));
      imageWindow.show();

    } catch (Exception e) {
    }

  }
}