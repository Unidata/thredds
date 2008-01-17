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

import ucar.nc2.dt.*;
import ucar.nc2.ui.point.StationRegionDateChooser;
import ucar.nc2.units.DateRange;

import ucar.util.prefs.*;
import ucar.util.prefs.ui.*;
import ucar.unidata.geoloc.LatLonRect;
import thredds.ui.*;

import java.awt.BorderLayout;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.io.*;
import java.util.*;
import java.beans.PropertyChangeListener;

import javax.swing.*;
import javax.swing.event.*;

/**
 * A Swing widget to view the contents of a ucar.nc2.dt.StationObsDataset or PointObsDataset.
 *
 * If its a StationObsDataset, the available Stations are shown in a BeanTable.
 * The obs are shown in a StructureTabel.
 *
 * @author caron
 */

public class StationObsViewer extends JPanel {
  private PreferencesExt prefs;

  private StationObsDataset sds;

  private StationRegionDateChooser chooser;
  private BeanTableSorted stnTable;
  private StructureTable obsTable;
  private JSplitPane splitH = null, splitV = null;
  private IndependentDialog infoWindow;

  private boolean eventsOK = true;
  private boolean debugStationRegionSelect = false, debugStationDatsets = false, debugQuery = false;

  public StationObsViewer(PreferencesExt prefs) {
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

    // do the query
    AbstractAction queryAction = new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        if (sds == null) return;

        // is the date window showing ?
        DateRange dateRange = chooser.getDateRange();
        boolean useDate = (null != dateRange);
        if (!useDate) return;

        Date startDate = dateRange.getStart().getDate();
        Date endDate = dateRange.getEnd().getDate();
        if (debugQuery) System.out.println("date range="+dateRange);

        // is the geoRegion mode true ?
        LatLonRect geoRegion = null;
        Station selectedStation = null;

        boolean useRegion = chooser.getGeoSelectionMode();
        if (useRegion) {
          geoRegion = chooser.getGeoSelectionLL();
          if (debugQuery) System.out.println("geoRegion="+geoRegion);
        } else {
          selectedStation = chooser.getSelectedStation();
        }

        if ((selectedStation == null) && !useRegion) return;

        // fetch the requested dobs
        try {
          List obsList;
          if (useRegion)
            obsList = useDate ? sds.getData(geoRegion, startDate, endDate) : sds.getData(geoRegion);
          else
            obsList = useDate ? sds.getData(selectedStation, startDate, endDate) : sds.getData(selectedStation);

          if (debugQuery)System.out.println("obsList="+obsList.size());
          setObservations( obsList);

        } catch (IOException e1) {
          e1.printStackTrace();
        }
      }
    };
    BAMutil.setActionProperties( queryAction, "query", "query for data", false, 'Q', -1);
    chooser.addToolbarAction( queryAction);

    // get all data
    AbstractAction getallAction = new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        if (sds == null) return;
        try {
          List obsList = sds.getData();
          if (obsList != null)
            setObservations( obsList);
          else
            JOptionPane.showMessageDialog(StationObsViewer.this, "GetAllData not implemented");

        } catch (IOException e1) {
          e1.printStackTrace();
        }
      }
    };
    BAMutil.setActionProperties( getallAction, "GetAll", "get ALL data", false, 'A', -1);
    chooser.addToolbarAction( getallAction);

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

    // the obs table
    obsTable = new StructureTable( (PreferencesExt) prefs.node("ObsBean"));

    // the info window
    TextHistoryPane infoTA = new TextHistoryPane();
    infoWindow = new IndependentDialog(null, true, "Station Information", infoTA);
    infoWindow.setBounds( (Rectangle) prefs.getBean("InfoWindowBounds", new Rectangle( 300, 300, 500, 300)));

    // layout
    splitH = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, false, stnTable, chooser);
    splitH.setDividerLocation(prefs.getInt("splitPosH", 400));

    splitV = new JSplitPane(JSplitPane.VERTICAL_SPLIT, false, splitH, obsTable);
    splitV.setDividerLocation(prefs.getInt("splitPos", 500));

    setLayout(new BorderLayout());
    add(splitV, BorderLayout.CENTER);
  }

  public void setDataset(StationObsDataset dataset) {
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
    obsTable.clear();
  }

  public void setStation(StationBean sb) {
    try {
      List obsList = sds.getData(sb.s);
      stnTable.getJTable().repaint();
      setObservations( obsList);

    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  /**
   * Set the list of Obs to show in the obs table
   * @param obsList list of type PointObsDatatype
   * @throws java.io.IOException on i/o error
   */
  public void setObservations( List obsList) throws IOException {
    if (obsList.size() == 0) {
      obsTable.clear();
      return;
    }
    obsTable.setPointObsData( obsList);
  }

  public PreferencesExt getPrefs() { return prefs; }

  public void save() {
   stnTable.saveState(false);
   prefs.putBeanObject("InfoWindowBounds", infoWindow.getBounds());
   prefs.putInt("splitPos", splitV.getDividerLocation());
   prefs.putInt("splitPosH", splitH.getDividerLocation());
   obsTable.saveState();
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