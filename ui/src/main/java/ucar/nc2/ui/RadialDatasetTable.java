// $Id: RadialDatasetTable.java,v 1.6 2006/02/16 23:02:38 caron Exp $
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
import ucar.nc2.units.DateUnit;
import ucar.nc2.dt.RadialDatasetSweep;
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
 * A Swing widget to examine a RadialDataset.
 *
 * @author caron
 * @version $Revision: 1.18 $ $Date: 2006/05/24 00:12:56 $
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

  public void setDataset(RadialDatasetSweep rds) {
    this.radialDataset = rds;
    dateUnit = rds.getTimeUnits();

    varTable.setBeans( getVariableBeans(rds));
    sweepTable.setBeans( new ArrayList());
  }

  public RadialDatasetSweep getRadialDataset() { return radialDataset; }

  public ArrayList getVariableBeans(RadialDatasetSweep rds) {
    ArrayList vlist = new ArrayList();
    java.util.List list = rds.getDataVariables();
    for (int i=0; i<list.size(); i++) {
      RadialDatasetSweep.RadialVariable v = (RadialDatasetSweep.RadialVariable) list.get(i);
      vlist.add( new VariableBean( v));
    }
    return vlist;
  }

  public void setVariable(VariableBean vb) {
    ArrayList sweeps = new ArrayList();
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
    private boolean isCoordVar, isRadial, axis;

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
}