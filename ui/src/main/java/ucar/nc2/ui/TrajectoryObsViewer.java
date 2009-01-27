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

import ucar.nc2.dt.*;
import ucar.nc2.ui.point.TrajectoryRegionDateChooser;

import ucar.util.prefs.*;
import ucar.util.prefs.ui.*;
import thredds.ui.*;

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
 * @author caron
 */

public class TrajectoryObsViewer extends JPanel {
  private PreferencesExt prefs;

  private TrajectoryRegionDateChooser chooser;
  private BeanTableSorted trajTable;
  private StructureTable obsTable;
  private JSplitPane splitV, splitH;
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
    TextHistoryPane infoTA = new TextHistoryPane();
    infoWindow = new IndependentWindow("Station Information", BAMutil.getImage( "netcdfUI"), infoTA);
    infoWindow.setBounds( (Rectangle) prefs.getBean("InfoWindowBounds", new Rectangle( 300, 300, 500, 300)));

    // layout
    splitV = new JSplitPane(JSplitPane.VERTICAL_SPLIT, false, splitH, obsTable);
    splitV.setDividerLocation(prefs.getInt("splitPos", 500));

    setLayout(new BorderLayout());
    add(splitV, BorderLayout.CENTER);
  }

  public void setDataset(TrajectoryObsDataset trajDataset) {
    if (debugStationDatsets)
      System.out.println("StationObsViewer open type "+trajDataset.getClass().getName());
    /* Date startDate = tds.getStartDate();
    Date endDate = tds.getEndDate();
    if ((startDate != null) && (endDate != null))
      chooser.setDateRange( new DateRange( startDate, endDate)); */

    List trajList = trajDataset.getTrajectories();
    List<TrajBean> trajBeans = new ArrayList<TrajBean>();
    for (int i = 0; i < trajList.size(); i++) {
      TrajectoryObsDatatype traj = (TrajectoryObsDatatype) trajList.get(i);
      trajBeans.add( new TrajBean( traj));
    }
    
    trajTable.setBeans(trajBeans);
  }

  public void setTrajectory(TrajBean sb) {
    TrajectoryObsDatatype tdt = sb.traj;
    try {
      obsTable.setTrajectory(tdt);
      chooser.setTrajectory(tdt);
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