// $Id: TrajectoryObsViewer.java,v 1.4 2005/06/11 18:42:04 caron Exp $
/*
 * Copyright 1997-2004 Unidata Program Center/University Corporation for
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

import ucar.nc2.dt.*;
import ucar.nc2.ui.point.TrajectoryRegionDateChooser;

import ucar.util.prefs.*;
import ucar.util.prefs.ui.*;
import thredds.ui.*;
import thredds.datatype.DateRange;

import java.awt.BorderLayout;
import java.awt.Rectangle;
import java.io.*;
import java.util.*;
import java.beans.PropertyChangeListener;

import javax.swing.*;
import javax.swing.event.*;

/**
 * A Swing widget to view the contents of a ucar.nc2.dt.TrajectoryObsDataset.
 *
 * The obs are shown in a StructureTabel.
 *
 * @author John Caron
 * @version $Id: TrajectoryObsViewer.java,v 1.4 2005/06/11 18:42:04 caron Exp $
 */

public class TrajectoryObsViewer extends JPanel {
  private PreferencesExt prefs;

  private TrajectoryObsDataset tds;
  private TrajectoryObsDatatype tdt;

  private ArrayList trajBeans;

  private TrajectoryRegionDateChooser chooser;
  private BeanTableSorted trajTable;
  private StructureTable obsTable;
  private JSplitPane splitV, splitH;
  private TextHistoryPane infoTA;
  private IndependentWindow infoWindow;

  private boolean debugStationRegionSelect = false, debugStationDatsets = false;

  public TrajectoryObsViewer(PreferencesExt prefs) {
    this.prefs = prefs;

    chooser = new TrajectoryRegionDateChooser();
    chooser.addPropertyChangeListener( new PropertyChangeListener() {
      public void propertyChange(java.beans.PropertyChangeEvent e) {
        if (e.getPropertyName().equals("Pick")) {
        }
      }
    });

    // table of trajectories
    trajTable = new BeanTableSorted(TrajBean.class, (PreferencesExt) prefs.node("TrajBeans"), false);
    trajTable.addListSelectionListener(new ListSelectionListener() {
      public void valueChanged(ListSelectionEvent e) {
        TrajBean sb = (TrajBean) trajTable.getSelectedBean();
        setTrajectory( sb);
        if (debugStationRegionSelect) System.out.println("trajTable selected= "+sb.getId());
      }
    });

    splitH = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, false, trajTable, chooser);
    splitH.setDividerLocation(prefs.getInt("splitPosH", 500));

    // the obs table
    obsTable = new StructureTable( (PreferencesExt) prefs.node("ObsBean"));
    obsTable.addListSelectionListener(new ListSelectionListener() {
      public void valueChanged(ListSelectionEvent e) {
        PointObsDatatype sb = (PointObsDatatype) obsTable.getSelectedRow();
        chooser.setSelected( sb);
      }
    });
    // the info window
    infoTA = new TextHistoryPane();
    infoWindow = new IndependentWindow("Station Information", BAMutil.getImage( "netcdfUI"), infoTA);
    infoWindow.setBounds( (Rectangle) prefs.getBean("InfoWindowBounds", new Rectangle( 300, 300, 500, 300)));

    // layout
    splitV = new JSplitPane(JSplitPane.VERTICAL_SPLIT, false, splitH, obsTable);
    splitV.setDividerLocation(prefs.getInt("splitPos", 500));

    setLayout(new BorderLayout());
    add(splitV, BorderLayout.CENTER);
  }

  public void setDataset(TrajectoryObsDataset trajDataset) {
    this.tds = trajDataset;
    if (debugStationDatsets)
      System.out.println("StationObsViewer open type "+trajDataset.getClass().getName());
    /* Date startDate = tds.getStartDate();
    Date endDate = tds.getEndDate();
    if ((startDate != null) && (endDate != null))
      chooser.setDateRange( new DateRange( startDate, endDate)); */

    List trajList = tds.getTrajectories();
    trajBeans = new ArrayList();
    for (int i = 0; i < trajList.size(); i++) {
      TrajectoryObsDatatype traj = (TrajectoryObsDatatype) trajList.get(i);
      trajBeans.add( new TrajBean( traj));
    }
    
    trajTable.setBeans( trajBeans);
  }

  public void setTrajectory(TrajBean sb) {
    this.tdt = sb.traj;
    try {
      obsTable.setTrajectory( tdt);
      chooser.setTrajectory( tdt);
    } catch (IOException e) {
      e.printStackTrace();
      return;
    }

    sb.setCount( tdt.getNumberPoints());
    trajTable.getJTable().repaint();

    if (tdt.getNumberPoints() == 0) {
      obsTable.clear();
      return;
    }
  }

  public PreferencesExt getPrefs() { return prefs; }

  public void save() {
   trajTable.saveState(false);
   prefs.putBeanObject("InfoWindowBounds", infoWindow.getBounds());
   prefs.putInt("splitPos", splitV.getDividerLocation());
   prefs.putInt("splitPosH", splitH.getDividerLocation());
   obsTable.saveState();
  }

  public class TrajBean {
    // static public String editableProperties() { return "title include logging freq"; }
    //static public String hiddenProperties() { return "recordNum"; }

    TrajectoryObsDatatype traj;
    private int count;

    // no-arg constructor
    public TrajBean() {}

    public TrajBean( TrajectoryObsDatatype traj) {
      this.traj = traj;
      count = traj.getNumberPoints();
    }

    public String getId() { return traj.getId(); }
    public String getDescription() { return traj.getDescription(); }

    public int getCount() { return count; }
    public void setCount( int count) { this.count = count; }

     public Date getStartDate() { return traj.getStartDate(); }
     public Date getEndDate() { return traj.getEndDate(); }
  }

  /* public class ObsBean {
    private PointObsDatatype obs;
    private double timeObs;

    public ObsBean( PointObsDatatype obs) {
      this.obs = obs;
      setTime( obs.getTime());
    }

    public double getTime() { return timeObs; }
    public void setTime( double timeObs) { this.timeObs = timeObs; }
  } */

}