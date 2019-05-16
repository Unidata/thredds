/*
 * Copyright (c) 1998-2019 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.ui.op;

import ucar.nc2.dt.RadialDatasetSweep;
import ucar.nc2.ft.FeatureDataset;
import ucar.nc2.ft.radial.StationRadialDataset;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.ui.point.StationRegionDateChooser;
import ucar.ui.widget.IndependentDialog;
import ucar.ui.widget.TextHistoryPane;
import ucar.nc2.units.DateRange;
import ucar.unidata.geoloc.Station;
import ucar.unidata.geoloc.LatLonPoint;
import ucar.util.prefs.PreferencesExt;
import ucar.ui.prefs.BeanTable;

import java.awt.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Date;
import java.util.List;
import java.util.ArrayList;
import java.io.IOException;
import javax.swing.JPanel;
import javax.swing.JSplitPane;

/**
 * A Swing widget to view the contents of a ucar.nc2.dt.StationRadarCollection
 *
 * @author caron
 */

public class StationRadialViewer extends JPanel {
  private PreferencesExt prefs;

  private StationRadialDataset sds;

  private StationRegionDateChooser chooser;
  private BeanTable stnTable;
  private RadialDatasetTable rdTable;
  private JSplitPane splitH, splitV;
  private IndependentDialog infoWindow;

  private boolean eventsOK = true;
  private boolean debugStationRegionSelect = false, debugStationDatsets = false, debugQuery = false;

/**
 *
 */
  public StationRadialViewer(PreferencesExt prefs) {
    this.prefs = prefs;

    chooser = new StationRegionDateChooser();
    chooser.addPropertyChangeListener( new PropertyChangeListener() {
      public void propertyChange(PropertyChangeEvent e) {
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
    stnTable = new BeanTable(StationBean.class, (PreferencesExt) prefs.node("StationBeans"), false);
    stnTable.addListSelectionListener(e -> {
        StationBean sb = (StationBean) stnTable.getSelectedBean();
        setStation( sb);
        if (debugStationRegionSelect) System.out.println("stnTable selected= "+sb.getName());
        if (eventsOK) chooser.setSelectedStation( sb.getName());
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

/**
 *
 */
  public void setDataset(FeatureDataset dataset) {
    this.sds = (StationRadialDataset) dataset;

    if (debugStationDatsets)
      System.out.println("PointObsViewer open type "+dataset.getClass().getName());
    CalendarDate startDate = dataset.getCalendarDateStart();
    CalendarDate endDate = dataset.getCalendarDateEnd();
    if ((startDate != null) && (endDate != null))
      chooser.setDateRange( new DateRange( startDate.toDate(), endDate.toDate()));

    List<StationBean> stationBeans = new ArrayList<>();
      try {
        List<Station> stations = sds.getStations();
        if (stations == null) return;

        for (Station station : stations)
          stationBeans.add(new StationBean(station));

      } catch (IOException ioe) {
        ioe.printStackTrace();
        return;
      }

    stnTable.setBeans( stationBeans);
    chooser.setStations( stationBeans);
    rdTable.clear();
  }

/**
 *
 */
  public void setStation(StationBean sb) {
    try {
      RadialDatasetSweep rsds = sds.getRadarDataset(sb.getName(), new Date()); // LOOK kludge - should show all possibilities
      rdTable.setDataset( rsds);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

/**
 *
 */
  public PreferencesExt getPrefs() { return prefs; }

/**
 *
 */
  public void save() {
   stnTable.saveState(false);
   prefs.putBeanObject("InfoWindowBounds", infoWindow.getBounds());
   prefs.putInt("splitPos", splitV.getDividerLocation());
   prefs.putInt("splitPosH", splitH.getDividerLocation());
   //rdTable.saveState();
  }

/**
 *
 */
  public class StationBean implements Station {
    private Station s;

    public StationBean( Station s) {
      this.s = s;
    }

    public String getName() {
      return s.getName();
    }

    public String getDescription() {
      return s.getDescription();
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

    public LatLonPoint getLatLon() {
      return s.getLatLon();
    }

    public boolean isMissing() {
      return s.isMissing();
    }

    public int compareTo(Station so) {
      return getName().compareTo(so.getName());
    }

    public int getNobs() { return -1; }

  }

}
