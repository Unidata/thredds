/*
 * Copyright 1997-2008 Unidata Program Center/University Corporation for
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

import ucar.nc2.dt2.*;
import ucar.nc2.ui.point.StationRegionDateChooser;
import ucar.nc2.units.DateRange;
import ucar.nc2.units.DateFormatter;
import ucar.nc2.constants.FeatureType;

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
 * A Swing widget to view the contents of a ucar.nc2.dt2.PointFeatureDataset
 * <p/>
 * If its a StationObsDataset, the available Stations are shown in a BeanTable.
 * The obs are shown in a StructureTable.
 *
 * @author caron
 */

public class PointFeatureDatasetViewer extends JPanel {
  private PreferencesExt prefs;

  private FeatureCollection selectedCollection;
  private FeatureType selectedType;

  private StationRegionDateChooser stationChooser;
  private BeanTableSorted fcTable, stnTable;
  private StructureTable obsTable;
  private JSplitPane splitT, splitH = null, splitV = null;
  private IndependentDialog infoWindow;
  private TextHistoryPane infoTA;

  private DateFormatter df = new DateFormatter();

  private boolean eventsOK = true;
  private boolean debugStationRegionSelect = false, debugQuery = false;

  public PointFeatureDatasetViewer(PreferencesExt prefs) {
    this.prefs = prefs;

    stationChooser = new StationRegionDateChooser();
    stationChooser.addPropertyChangeListener(new PropertyChangeListener() {
      public void propertyChange(java.beans.PropertyChangeEvent e) {
        if (e.getPropertyName().equals("Station")) {
          StationBean selectedStation = (StationBean) e.getNewValue();
          if (debugStationRegionSelect) System.out.println("selectedStation= " + selectedStation.getName());
          eventsOK = false;
          stnTable.setSelectedBean(selectedStation);
          eventsOK = true;
        }
      }
    });

    // do the query
    AbstractAction queryAction = new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        if (selectedCollection == null) return;

        // is the date window showing ?
        DateRange dateRange = stationChooser.getDateRange();

        if (debugQuery) System.out.println("date range=" + dateRange);

        // is the geoRegion mode true ?
        LatLonRect geoRegion = null;
        StationBean selectedStation = null;

        boolean useRegion = stationChooser.getGeoSelectionMode();
        if (useRegion) {
          geoRegion = stationChooser.getGeoSelectionLL();
          if (debugQuery) System.out.println("geoRegion=" + geoRegion);
        } else {
          selectedStation = (StationBean) stationChooser.getSelectedStation();
        }

        try {
          if (selectedStation != null) {
            setStation(selectedStation, dateRange);
          } else if (useRegion) {
            subset(geoRegion, dateRange);
          }

        } catch (IOException e1) {
          e1.printStackTrace();
        }
      }
    };
    BAMutil.setActionProperties(queryAction, "query", "query for data", false, 'Q', -1);
    stationChooser.addToolbarAction(queryAction);  // */

    // get all data
    AbstractAction getallAction = new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        if (selectedCollection == null) return;
        try {
          setObservationsAll();

        } catch (IOException e1) {
          e1.printStackTrace();
        }
      }
    };
    BAMutil.setActionProperties(getallAction, "GetAll", "get ALL data", false, 'A', -1);
    stationChooser.addToolbarAction(getallAction);

    // feature collection table
    fcTable = new BeanTableSorted(FeatureCollectionBean.class, (PreferencesExt) prefs.node("FeatureCollectionBean"), false);
    fcTable.addListSelectionListener(new ListSelectionListener() {
      public void valueChanged(ListSelectionEvent e) {
        FeatureCollectionBean fcb = (FeatureCollectionBean) fcTable.getSelectedBean();
        try {
          setFeatureCollection(fcb);
        } catch (IOException e1) {
          JOptionPane.showMessageDialog(null, "Error reading FeatureCollection " + fcb.fc.getName() + " error=" + e1.getMessage());
          e1.printStackTrace();
        }
      }
    });

    // station table
    stnTable = new BeanTableSorted(StationBean.class, (PreferencesExt) prefs.node("StationBeans"), false);
    stnTable.addListSelectionListener(new ListSelectionListener() {
      public void valueChanged(ListSelectionEvent e) {
        StationBean sb = (StationBean) stnTable.getSelectedBean();
        try {
          setStation(sb, null);
        } catch (IOException e1) {
          e1.printStackTrace();
        }
        if (debugStationRegionSelect) System.out.println("stnTable selected= " + sb.getName());
        if (eventsOK) stationChooser.setSelectedStation(sb.getName());
      }
    });

    // the obs table
    obsTable = new StructureTable((PreferencesExt) prefs.node("ObsBean"));

    // the info window
    infoTA = new TextHistoryPane();
    infoWindow = new IndependentDialog(null, true, "Station Information", infoTA);
    infoWindow.setBounds((Rectangle) prefs.getBean("InfoWindowBounds", new Rectangle(300, 300, 500, 300)));

    // layout
    splitT = new JSplitPane(JSplitPane.VERTICAL_SPLIT, false, fcTable, stnTable);
    splitT.setDividerLocation(prefs.getInt("splitPosT", 50));

    splitH = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, false, splitT, stationChooser);
    splitH.setDividerLocation(prefs.getInt("splitPosH", 400));

    splitV = new JSplitPane(JSplitPane.VERTICAL_SPLIT, false, splitH, obsTable);
    splitV.setDividerLocation(prefs.getInt("splitPos", 500));

    setLayout(new BorderLayout());
    add(splitV, BorderLayout.CENTER);
  }

  public void setDataset(PointFeatureDataset dataset) {
    infoTA.clear();
    stnTable.setBeans(new ArrayList());
    stationChooser.setStations(new ArrayList());
    obsTable.clear();

    List<FeatureCollectionBean> fcBeans = new ArrayList<FeatureCollectionBean>();

    for (FeatureCollection fc : dataset.getPointFeatureCollectionList()) {
      FeatureType ftype = fc.getCollectionFeatureType();
      if ((ftype == FeatureType.POINT) || (ftype == FeatureType.STATION)) {
        fcBeans.add(new FeatureCollectionBean(fc));
      }
    }

    if (fcBeans.size() == 0) {
      JOptionPane.showMessageDialog(null, "No PointFeatureCollections found that could be displayed");
    }

    fcTable.setBeans(fcBeans);
    infoTA.clear();
  }

  private void setFeatureCollection(FeatureCollectionBean fcb) throws IOException {
    FeatureType ftype = fcb.fc.getCollectionFeatureType();
    if (ftype == FeatureType.STATION) {
      StationFeatureCollection sfc = (StationFeatureCollection) fcb.fc;
      setStationCollection(sfc);
    } else if (ftype == FeatureType.POINT) {
      PointFeatureCollection pfc = (PointFeatureCollection) fcb.fc;
      setPointCollection(pfc);
    }
    this.selectedCollection = fcb.fc;
    this.selectedType = ftype;
  }

  private void setStationCollection(StationFeatureCollection stationCollection) {
    List<StationBean> stationBeans = new ArrayList<StationBean>();
    try {
      List<Station> stations = stationCollection.getStations();
      if (stations == null) return;

      for (Station station : stations)
        stationBeans.add(new StationBean(stationCollection.getStationFeature(station)));

    } catch (IOException ioe) {
      ioe.printStackTrace();
      return;
    }

    stnTable.setBeans(stationBeans);
    stationChooser.setStations(stationBeans);
    obsTable.clear();
  }

  private void setPointCollection(PointFeatureCollection pointCollection) throws IOException {
    List<PointObsBean> pointBeans = new ArrayList<PointObsBean>();
    int count = 0;

    PointFeatureIterator iter = pointCollection.getPointFeatureIterator(-1);
    while (iter.hasNext()) {
      PointFeature pob = iter.nextData();
      pointBeans.add(new PointObsBean(count++, pob));
    }

    stnTable.setBeans(pointBeans);
    stationChooser.setStations(pointBeans);
    stnTable.clearSelectedCells();
  }

  private void subset(LatLonRect geoRegion, DateRange dateRange) throws IOException {
    PointFeatureCollection pc = null;

    if (selectedType == FeatureType.STATION) {
      StationFeatureCollection stationCollection = (StationFeatureCollection) selectedCollection;
      pc = stationCollection.flatten(geoRegion, dateRange);
    }

    if (null != pc)
      setObservations(pc);
  }

  private void setStation(StationBean sb, DateRange dr) throws IOException {
    if (selectedType == FeatureType.POINT) {
      PointObsBean pobsBean = (PointObsBean) sb;
      List<PointFeature> obsList = new ArrayList<PointFeature>();
      obsList.add( pobsBean.pobs);
      setObservations(obsList);

    } else if (selectedType == FeatureType.STATION) {
      StationFeatureCollection stationCollection = (StationFeatureCollection) selectedCollection;
      StationFeature feature = stationCollection.getStationFeature(sb.s, dr);
      setObservations(feature);

      // iterator may count points
      int npts = feature.getNumberPoints();
      if (npts >= 0) {
        sb.setNobs(npts);
      }
    }
  }

  private void setObservationsAll() throws IOException {
    if (selectedType == FeatureType.POINT) {
      PointFeatureCollection pointCollection = (PointFeatureCollection) selectedCollection;
      setObservations(pointCollection);

    } else if (selectedType == FeatureType.STATION) {
      StationFeatureCollection stationCollection = (StationFeatureCollection) selectedCollection;
      PointFeatureCollectionIterator iter = stationCollection.getPointFeatureCollectionIterator(-1);
      List<PointFeature> obsList = new ArrayList<PointFeature>();
      while (iter.hasNext())
        obsList.add((PointFeature) iter.nextFeature());
      setObservations(obsList);
    }
  }

  private void setObservations(PointFeatureCollection pointCollection) throws IOException {
    PointFeatureIterator iter = pointCollection.getPointFeatureIterator(-1);
    List<PointFeature> obsList = new ArrayList<PointFeature>();
    while (iter.hasNext())
      obsList.add(iter.nextData());
    setObservations(obsList);
  }


  private void setObservations(List<PointFeature> obsList) throws IOException {
    if (obsList.size() == 0) {
      obsTable.clear();
      return;
    }
    obsTable.setPointObsData2(obsList);
  }

  public PreferencesExt getPrefs() {
    return prefs;
  }

  public void save() {
    fcTable.saveState(false);
    stnTable.saveState(false);
    prefs.putBeanObject("InfoWindowBounds", infoWindow.getBounds());
    prefs.putInt("splitPos", splitV.getDividerLocation());
    prefs.putInt("splitPosT", splitT.getDividerLocation());
    prefs.putInt("splitPosH", splitH.getDividerLocation());
    obsTable.saveState();
  }

  public class FeatureCollectionBean {
    FeatureCollection fc;

    public FeatureCollectionBean(FeatureCollection fc) {
      this.fc = fc;
    }

    public String getName() {
      return fc.getName();
    }

    public String getFeatureType() {
      return fc.getCollectionFeatureType().toString();
    }
  }

  public class StationBean implements ucar.nc2.dt.Station {
    private Station s;
    private int npts = -1;

    public StationBean() {
    }

    public StationBean(StationFeature s) {
      this.s = s;
      this.npts = s.getNumberPoints();
    }

    public String getName() {
      return s.getName();
    }

    public String getDescription() {
      return s.getDescription();
    }

    public int getNobs() {
      return npts;
    }

    public void setNobs(int npts) {
      this.npts = npts;
    }

    public String getWmoId() {
      return "";
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
      StationImpl so = (StationImpl) o;
      return getName().compareTo(so.getName());
    }
  }

  public class PointObsBean extends StationBean {  // fake Station, so we can use StationRegionChooser
    private PointFeature pobs;
    private String timeObs;
    private int id;

    public PointObsBean(int id, PointFeature obs) {
      this.id = id;
      this.pobs = obs;
      setTime(obs.getObservationTimeAsDate());
    }

    public String getTime() {
      return timeObs;
    }

    public void setTime(Date timeObs) {
      this.timeObs = df.toDateTimeString(timeObs);
    }

    public String getName() {
      return Integer.toString(id);
    }

    public String getDescription() {
      return null;
    }

    public String getWmoId() {
      return null;
    }

    public double getLatitude() {
      return pobs.getLocation().getLatitude();
    }

    public double getLongitude() {
      return pobs.getLocation().getLongitude();
    }

    public double getAltitude() {
      return pobs.getLocation().getAltitude();
    }

    public int compareTo(Object o) {
      StationImpl so = (StationImpl) o;
      return getName().compareTo(so.getName());
    }
  }

}