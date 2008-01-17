/*
 * Copyright 1997-2007 Unidata Program Center/University Corporation for
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

import ucar.util.prefs.PreferencesExt;
import ucar.util.prefs.ui.BeanTableSorted;
import ucar.nc2.dt.Station;
import ucar.nc2.dt.StationImpl;
import ucar.nc2.dt.RadialDatasetSweep;
import ucar.nc2.ui.point.StationRegionDateChooser;
import ucar.nc2.thredds.DqcRadarDatasetCollection;

import javax.swing.*;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.ListSelectionEvent;

import thredds.ui.IndependentDialog;
import thredds.ui.TextHistoryPane;
import ucar.nc2.units.DateRange;

import java.beans.PropertyChangeListener;
import java.awt.*;
import java.util.Date;
import java.util.List;
import java.util.ArrayList;
import java.io.IOException;

/**
 * A Swing widget to view the contents of a ucar.nc2.dt.StationRadarCollection
 *
 * @author caron
 */

public class StationRadialViewer extends JPanel {
  private PreferencesExt prefs;

  private ucar.nc2.thredds.DqcRadarDatasetCollection sds;

  private StationRegionDateChooser chooser;
  private BeanTableSorted stnTable;
  private RadialDatasetTable rdTable;
  private JSplitPane splitH = null, splitV = null;
  private IndependentDialog infoWindow;

  private boolean eventsOK = true;
  private boolean debugStationRegionSelect = false, debugStationDatsets = false, debugQuery = false;

  public StationRadialViewer(PreferencesExt prefs) {
    this.prefs = prefs;

    chooser = new StationRegionDateChooser();
    chooser.addPropertyChangeListener( new PropertyChangeListener() {
      public void propertyChange(java.beans.PropertyChangeEvent e) {
        if (e.getPropertyName().equals("Station")) {
          Station selectedStation = (Station) e.getNewValue();
          if (debugStationRegionSelect) System.out.println("selectedStation= "+selectedStation.getName());
          eventsOK = false;
          stnTable.setSelectedBean( selectedStation);
          eventsOK = true;
        }
      }
    });

    // station table
    stnTable = new BeanTableSorted(StationBean.class, (PreferencesExt) prefs.node("StationBeans"), false);
    stnTable.addListSelectionListener(new ListSelectionListener() {
      public void valueChanged(ListSelectionEvent e) {
        StationBean sb = (StationBean) stnTable.getSelectedBean();
        setStation( sb);
        if (debugStationRegionSelect) System.out.println("stnTable selected= "+sb.getName());
        if (eventsOK) chooser.setSelectedStation( sb.getName());
      }
    });

    // the RadialDatasetTable
    rdTable = new RadialDatasetTable((PreferencesExt) prefs.node("RadialDatasetTable"));

    // the info window
    TextHistoryPane infoTA = new TextHistoryPane();
    infoWindow = new IndependentDialog(null, true, "Station Information", infoTA);
    infoWindow.setBounds( (Rectangle) prefs.getBean("InfoWindowBounds", new Rectangle( 300, 300, 500, 300)));

    // layout
    splitH = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, false, stnTable, chooser);
    splitH.setDividerLocation(prefs.getInt("splitPosH", 400));

    splitV = new JSplitPane(JSplitPane.VERTICAL_SPLIT, false, splitH, rdTable);
    splitV.setDividerLocation(prefs.getInt("splitPos", 500));

    setLayout(new BorderLayout());
    add(splitV, BorderLayout.CENTER);
  }

  public void setDataset(DqcRadarDatasetCollection dataset) {
    this.sds = dataset;

    if (debugStationDatsets)
      System.out.println("PointObsViewer open type "+dataset.getClass().getName());
    Date startDate = dataset.getStartDate();
    Date endDate = dataset.getEndDate();
    if ((startDate != null) && (endDate != null))
      chooser.setDateRange( new DateRange( startDate, endDate));

    List<StationBean> stationBeans = new ArrayList<StationBean>();
      try {
        List<Station> stations = sds.getStations();
        if (stations == null) return;

        for (Station station : stations)
          stationBeans.add(new StationBean((StationImpl) station));

      } catch (IOException ioe) {
        ioe.printStackTrace();
        return;
      }

    stnTable.setBeans( stationBeans);
    chooser.setStations( stationBeans);
    rdTable.clear();
  }

  public void setStation(StationBean sb) {
    try {
      RadialDatasetSweep rsds = sds.getRadarDataset(sb.getName(), new Date()); // LOOK kludge - should show all possibilities
      rdTable.setDataset( rsds);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public PreferencesExt getPrefs() { return prefs; }

  public void save() {
   stnTable.saveState(false);
   prefs.putBeanObject("InfoWindowBounds", infoWindow.getBounds());
   prefs.putInt("splitPos", splitV.getDividerLocation());
   prefs.putInt("splitPosH", splitH.getDividerLocation());
   //rdTable.saveState();
  }

  public class StationBean implements Station {
    private StationImpl s;

    public StationBean( StationImpl s) {
      this.s = s;
    }

    public String getName() {
      return s.getName();
    }

    public String getDescription() {
      return s.getDescription();
    }

    public int getNobs() {
      return s.getNumObservations();
    }

    public String getWmoId() {
      return s.getWmoId();
    }

    public double getLatitude() {
      return s.getLatitude();
    }

    public double getLongitude() {
      return s.getLongitude();
    }

    public double getAltitude() {
      return s.getAltitude();
    }

    public int compareTo(Object o) {
      Station so = (Station) o;
      return getName().compareTo( so.getName());
    }
  }

}
