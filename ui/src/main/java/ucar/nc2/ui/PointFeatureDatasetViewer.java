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

import ucar.nc2.ft.*;
import ucar.nc2.ui.point.StationRegionDateChooser;
import ucar.nc2.ui.point.PointController;
import ucar.nc2.units.DateRange;
import ucar.nc2.units.DateFormatter;
import ucar.nc2.constants.FeatureType;

import ucar.util.prefs.*;
import ucar.util.prefs.ui.*;
import ucar.unidata.geoloc.LatLonRect;
import ucar.unidata.geoloc.Station;
import ucar.unidata.geoloc.LatLonPoint;
import ucar.unidata.geoloc.LatLonPointImpl;
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
  static private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(PointFeatureDatasetViewer.class);

  private PreferencesExt prefs;

  private FeatureCollection selectedCollection;
  private FeatureType selectedType;

  private BeanTableSorted fcTable, profileTable, stnTable, stnProfileTable, trajTable, sectionTable;
  private JPanel changingPane = new JPanel(new BorderLayout());
  private StationRegionDateChooser stationMap;
  private StructureTable obsTable;
  private JSplitPane splitFeatures, splitMap, splitObs;
  private IndependentDialog infoWindow;
  //private IndependentWindow pointDisplayWindow;
  private TextHistoryPane infoTA;

  private PointController pointController;


  private DateFormatter df = new DateFormatter();

  private boolean eventsOK = true;
  private boolean debugStationRegionSelect = false, debugQuery = false;

  private int maxCount = Integer.MAX_VALUE;

  public PointFeatureDatasetViewer(PreferencesExt prefs) {
    this.prefs = prefs;

    stationMap = new StationRegionDateChooser();
    stationMap.addPropertyChangeListener(new PropertyChangeListener() {
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
        DateRange dateRange = stationMap.getDateRange();
        if (debugQuery) System.out.println("date range=" + dateRange);

        // is the geoRegion mode true ?
        LatLonRect geoRegion = null;
        StationBean selectedStation = null;

        boolean useRegion = stationMap.getGeoSelectionMode();
        if (useRegion) {
          geoRegion = stationMap.getGeoSelectionLL();
          if (debugQuery) System.out.println("geoRegion=" + geoRegion);
        } else {
          selectedStation = (StationBean) stationMap.getSelectedStation();
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
    stationMap.addToolbarAction(queryAction);  // */

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
    stationMap.addToolbarAction(getallAction);

    // feature collection table
    fcTable = new BeanTableSorted(FeatureCollectionBean.class, (PreferencesExt) prefs.node("FeatureCollectionBean"), false);
    fcTable.addListSelectionListener(new ListSelectionListener() {
      public void valueChanged(ListSelectionEvent e) {
        FeatureCollectionBean fcb = (FeatureCollectionBean) fcTable.getSelectedBean();
        try {
          setFeatureCollection(fcb);
        } catch (Exception e1) {
          JOptionPane.showMessageDialog(null, "Error reading FeatureCollection " + fcb.fc.getName() + " error=" + e1.getMessage());
          e1.printStackTrace();
        }
      }
    });

    // profile table
    profileTable = new BeanTableSorted(ProfileFeatureBean.class, (PreferencesExt) prefs.node("ProfileFeatureBean"), false);
    profileTable.addListSelectionListener(new ListSelectionListener() {
      public void valueChanged(ListSelectionEvent e) {
        ProfileFeatureBean sb = (ProfileFeatureBean) profileTable.getSelectedBean();
        try {
          setProfile(sb);
        } catch (IOException e1) {
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
        if (eventsOK) stationMap.setSelectedStation(sb.getName());
      }
    });

    // station profile table
    stnProfileTable = new BeanTableSorted(StnProfileFeatureBean.class, (PreferencesExt) prefs.node("StnProfileFeatureBean"), false);
    stnProfileTable.addListSelectionListener(new ListSelectionListener() {
      public void valueChanged(ListSelectionEvent e) {
        StnProfileFeatureBean sb = (StnProfileFeatureBean) stnProfileTable.getSelectedBean();
        try {
          setStnProfile(sb);
        } catch (IOException e1) {
          e1.printStackTrace();
        }
      }
    });

    // trajectory table
    trajTable = new BeanTableSorted(TrajectoryFeatureBean.class, (PreferencesExt) prefs.node("TrajFeatureBean"), false);
    trajTable.addListSelectionListener(new ListSelectionListener() {
      public void valueChanged(ListSelectionEvent e) {
        TrajectoryFeatureBean sb = (TrajectoryFeatureBean) trajTable.getSelectedBean();
        try {
          setTrajectory(sb);
        } catch (IOException e1) {
          e1.printStackTrace();
        }
      }
    });

    // section table
    sectionTable = new BeanTableSorted(SectionFeatureBean.class, (PreferencesExt) prefs.node("TrajFeatureBean"), false);
    sectionTable.addListSelectionListener(new ListSelectionListener() {
      public void valueChanged(ListSelectionEvent e) {
        SectionFeatureBean sb = (SectionFeatureBean) trajTable.getSelectedBean();
        try {
          setSection(sb);
        } catch (IOException e1) {
          e1.printStackTrace();
        }
      }
    });

    pointController = new PointController();    

    // the obs table
    obsTable = new StructureTable((PreferencesExt) prefs.node("ObsBean"));

    // the info window
    infoTA = new TextHistoryPane();
    infoWindow = new IndependentDialog(null, true, "Station Information", infoTA);
    infoWindow.setBounds((Rectangle) prefs.getBean("InfoWindowBounds", new Rectangle(300, 300, 500, 300)));

    // layout
    splitFeatures = new JSplitPane(JSplitPane.VERTICAL_SPLIT, false, fcTable, changingPane);
    splitFeatures.setDividerLocation(prefs.getInt("splitPosF", 50));

    splitMap = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, false, splitFeatures, stationMap);
    splitMap.setDividerLocation(prefs.getInt("splitPosM", 400));

    splitObs = new JSplitPane(JSplitPane.VERTICAL_SPLIT, false, splitMap, obsTable);
    splitObs.setDividerLocation(prefs.getInt("splitPosO", 500));

    setLayout(new BorderLayout());
    add(splitObs, BorderLayout.CENTER);
  }

  void makePointController() {
    //pointDisplayWindow = new IndependentWindow("PointData", BAMutil.getImage( "thredds"), pointController);
    //pointDisplayWindow.setBounds((Rectangle) prefs.getBean("PointDisplayBounds", new Rectangle(300, 300, 500, 300)));
  }

  public void save() {
    fcTable.saveState(false);
    stnTable.saveState(false);
    prefs.putBeanObject("InfoWindowBounds", infoWindow.getBounds());
    //if (pointDisplayWindow != null) prefs.putBeanObject("PointDisplayBounds", pointDisplayWindow.getBounds());
    prefs.putInt("splitPosO", splitObs.getDividerLocation());
    prefs.putInt("splitPosF", splitFeatures.getDividerLocation());
    prefs.putInt("splitPosM", splitMap.getDividerLocation());
    obsTable.saveState();
  }

  public void setDataset(FeatureDatasetPoint dataset) {
    infoTA.clear();
    stnTable.setBeans(new ArrayList());
    stationMap.setStations(new ArrayList());
    obsTable.clear();
    selectedCollection = null;

    // set the feature collection table - all use this
    List<FeatureCollectionBean> fcBeans = new ArrayList<FeatureCollectionBean>();
    for (FeatureCollection fc : dataset.getPointFeatureCollectionList()) {
      fcBeans.add( new FeatureCollectionBean(fc));
    }
    if (fcBeans.size() == 0)
      JOptionPane.showMessageDialog(null, "No PointFeatureCollections found that could be displayed");

    fcTable.setBeans(fcBeans);
    infoTA.clear();

    // set the date range if possible
    DateRange dr = dataset.getDateRange();
    if (dr != null)
      stationMap.setDateRange(dr);

    // set the bounding box if possible
    LatLonRect bb = dataset.getBoundingBox();
    if (bb != null)
      stationMap.setGeoBounds(bb);
  }

  private void setFeatureCollection(FeatureCollectionBean fcb) throws IOException {
    // reconfigure the UI depending on the type
    changingPane.removeAll();

    FeatureType ftype = fcb.fc.getCollectionFeatureType();

    if (ftype == FeatureType.POINT) {
      //PointFeatureCollection pfc = (PointFeatureCollection) fcb.fc;
      //setPointCollection(pfc);
      changingPane.add( pointController, BorderLayout.CENTER);

    } else if (ftype == FeatureType.PROFILE) {
      ProfileFeatureCollection pfc = (ProfileFeatureCollection) fcb.fc;
      setProfileCollection(pfc);
      changingPane.add( stnTable, BorderLayout.CENTER);

    } else if (ftype == FeatureType.STATION) {
      StationCollection sfc = (StationCollection) fcb.fc;
      setStations(sfc);
      changingPane.add( stnTable, BorderLayout.CENTER);

    } else if (ftype == FeatureType.STATION_PROFILE) {
      StationCollection sfc = (StationCollection) fcb.fc;
      setStations(sfc);

      JSplitPane splitExtra = new JSplitPane(JSplitPane.VERTICAL_SPLIT, false, stnTable, stnProfileTable);
      splitExtra.setDividerLocation(prefs.getInt("splitPosX", 50));
      changingPane.add( splitExtra, BorderLayout.CENTER);

    } else if (ftype == FeatureType.TRAJECTORY) {
      TrajectoryFeatureCollection pfc = (TrajectoryFeatureCollection) fcb.fc;
      setTrajectoryCollection(pfc);
      changingPane.add( stnTable, BorderLayout.CENTER);

    } else if (ftype == FeatureType.SECTION) {
      SectionFeatureCollection pfc = (SectionFeatureCollection) fcb.fc;
      setSectionCollection(pfc);

      JSplitPane splitExtra = new JSplitPane(JSplitPane.VERTICAL_SPLIT, false, stnTable, profileTable);
      splitExtra.setDividerLocation(prefs.getInt("splitPosX", 50));
      changingPane.add( splitExtra, BorderLayout.CENTER);
    }

    this.selectedCollection = fcb.fc;
    this.selectedType = ftype;
  }

  private void setStations(StationCollection stationCollection) {
    List<StationBean> stationBeans = new ArrayList<StationBean>();
    try {
      List<Station> stations = stationCollection.getStations();
      if (stations == null) return;

      for (Station station : stations)
        stationBeans.add( new StationBean(station));

    } catch (IOException ioe) {
      ioe.printStackTrace();
      return;
    }

    stnTable.setBeans(stationBeans);
    stationMap.setStations(stationBeans);
    obsTable.clear();
  }

  private void setPointCollection(PointFeatureCollection pointCollection) throws IOException {
    List<PointObsBean> pointBeans = new ArrayList<PointObsBean>();
    int count = 0;

    PointFeatureIterator iter = pointCollection.getPointFeatureIterator(-1);
    iter.setCalculateBounds(pointCollection);
    try {
      while (iter.hasNext() && (count++ < maxCount)) {
        PointFeature pob = iter.next();
        pointBeans.add(new PointObsBean(count++, pob, df));
      }
    } finally {
      iter.finish();
    }

    stnTable.setBeans(pointBeans);
    stationMap.setStations(pointBeans);
    stnTable.clearSelectedCells();
  }

  private void setProfileCollection(ProfileFeatureCollection profileCollection) throws IOException {
    List<ProfileFeatureBean> beans = new ArrayList<ProfileFeatureBean>();

    PointFeatureCollectionIterator iter = profileCollection.getPointFeatureCollectionIterator(-1);
    while (iter.hasNext()) {
      PointFeatureCollection pob = iter.next();
      ProfileFeatureBean bean = new ProfileFeatureBean((ProfileFeature) pob);
      if (bean.pf != null) // may have missing values
        beans.add(bean);
    }

    stnTable.setBeans(beans);
    stationMap.setStations(beans);
    stnTable.clearSelectedCells();
  }

  private void setTrajectoryCollection(TrajectoryFeatureCollection trajCollection) throws IOException {
    List<TrajectoryFeatureBean> beans = new ArrayList<TrajectoryFeatureBean>();

    PointFeatureCollectionIterator iter = trajCollection.getPointFeatureCollectionIterator(-1);
    while (iter.hasNext()) {
      PointFeatureCollection pob = iter.next();
      TrajectoryFeatureBean trajBean = new TrajectoryFeatureBean((TrajectoryFeature) pob);
      if (trajBean.pf != null) // may have missing values
        beans.add(trajBean);
    }

    stnTable.setBeans(beans);
    stationMap.setStations(beans);
    stnTable.clearSelectedCells();
  }

  private void setSectionCollection(SectionFeatureCollection sectionCollection) throws IOException {
    List<SectionFeatureBean> beans = new ArrayList<SectionFeatureBean>();

    NestedPointFeatureCollectionIterator iter = sectionCollection.getNestedPointFeatureCollectionIterator(-1);
    while (iter.hasNext()) {
      NestedPointFeatureCollection pob = iter.next();
      SectionFeatureBean bean = new SectionFeatureBean((SectionFeature) pob);
      if (bean.pf != null) // may have missing values
        beans.add(bean);
    }

    stnTable.setBeans(beans);
    stationMap.setStations(beans);
    stnTable.clearSelectedCells();
  }

  private void subset(LatLonRect geoRegion, DateRange dateRange) throws IOException {
    PointFeatureCollection pc = null;

    if (selectedType == FeatureType.POINT) {
      PointFeatureCollection ptCollection = (PointFeatureCollection) selectedCollection;
      pc = ptCollection.subset(geoRegion, dateRange);
    }

    else if (selectedType == FeatureType.STATION) {
      StationTimeSeriesFeatureCollection stationCollection = (StationTimeSeriesFeatureCollection) selectedCollection;
     /*  if (geoRegion != null) {
        StationTimeSeriesFeatureCollection stationSubset = stationCollection.subset(geoRegion);
        setStations( stationSubset);
        return;
      } else { */
        pc = stationCollection.flatten(geoRegion, dateRange);
      //}
    }

    else if (selectedType == FeatureType.STATION_PROFILE) {
      StationProfileFeatureCollection stationProfileCollection = (StationProfileFeatureCollection) selectedCollection;
      pc = stationProfileCollection.flatten(geoRegion, dateRange);
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

     } else if (selectedType == FeatureType.PROFILE) {
      ProfileFeatureBean profBean = (ProfileFeatureBean) sb;
      ProfileFeature feature = profBean.pfc;
      setObservations(feature);

      // iterator may count points
      int npts = feature.size();
      if (npts >= 0) {
        sb.setNobs(npts);
      }

    } else if (selectedType == FeatureType.STATION) {
      StationTimeSeriesFeatureCollection stationCollection = (StationTimeSeriesFeatureCollection) selectedCollection;
      StationTimeSeriesFeature feature = stationCollection.getStationFeature(sb.s);
      if (dr != null)
        feature = feature.subset(dr);
      setObservations(feature);

      // iterator may count points
      int npts = feature.size();
      if (npts >= 0) {
        sb.setNobs(npts);
      }

    } else if (selectedType == FeatureType.STATION_PROFILE) {
      StationProfileFeatureCollection stationCollection = (StationProfileFeatureCollection) selectedCollection;
      StationProfileFeature feature = stationCollection.getStationProfileFeature(sb.s);
      setStnProfileObservations(feature);

      // iterator may count points
      int npts = feature.size();
      if (npts >= 0) {
        sb.setNobs(npts);
      }

    } else if (selectedType == FeatureType.TRAJECTORY) {
      TrajectoryFeatureBean trajBean = (TrajectoryFeatureBean) sb;
      TrajectoryFeature feature = trajBean.pfc;
      setObservations(feature);

      // iterator may count points
      int npts = feature.size();
      if (npts >= 0) {
        sb.setNobs(npts);
      }

    } else if (selectedType == FeatureType.SECTION) {
      SectionFeatureBean sectionBean = (SectionFeatureBean) sb;
      setSection(sectionBean);

      // iterator may count points
      SectionFeature feature = sectionBean.pfc;
      int npts = feature.size();
      if (npts >= 0) {
        sb.setNobs(npts);
      }
    }
  }

  private void setStnProfile(StnProfileFeatureBean sb) throws IOException {
     ProfileFeature feature = sb.pfc;
     setObservations(feature);

     // iterator may count points
     int npts = feature.size();
     if (npts >= 0) {
       sb.setNobs(npts);
     }
   }

  private void setProfile(ProfileFeatureBean sb) throws IOException {
     ProfileFeature feature = sb.pfc;
     setObservations(feature);

     // iterator may count points
     int npts = feature.size();
     if (npts >= 0) {
       sb.setNobs(npts);
       stnTable.refresh();
     }
   }

  private void setTrajectory(TrajectoryFeatureBean sb) throws IOException {
     TrajectoryFeature feature = sb.pfc;
     setObservations(feature);

     // iterator may count points
     int npts = feature.size();
     if (npts >= 0) {
       sb.setNobs(npts);
       stnTable.refresh();
     }
   }

  private void setSection(SectionFeatureBean sb) throws IOException {
     SectionFeature feature = sb.pfc;
     setSectionObservations(feature);

     // iterator may count points
     int npts = feature.size();
     if (npts >= 0) {
       sb.setNobs(npts);
       stnTable.refresh();
     }
   }

  private void setStnProfiles(List<PointFeatureCollection> pfcList) throws IOException {
    List<StnProfileFeatureBean> beans = new ArrayList<StnProfileFeatureBean>();

    for (PointFeatureCollection pfc : pfcList)
      beans.add(new StnProfileFeatureBean((ProfileFeature) pfc));

    stnProfileTable.setBeans(beans);
    obsTable.clear();
  }

  private void setSectionProfiles(List<PointFeatureCollection> pfcList) throws IOException {
    List<ProfileFeatureBean> beans = new ArrayList<ProfileFeatureBean>();

    for (PointFeatureCollection pfc : pfcList)
      beans.add(new ProfileFeatureBean((ProfileFeature) pfc));

    profileTable.setBeans(beans);
    obsTable.clear();
  }

   private void setObservationsAll() throws IOException {
    if (selectedType == FeatureType.POINT) {
      PointFeatureCollection pointCollection = (PointFeatureCollection) selectedCollection;
      setObservations(pointCollection);

    } else if (selectedType == FeatureType.STATION) {
      StationTimeSeriesFeatureCollection stationCollection = (StationTimeSeriesFeatureCollection) selectedCollection;
      setStations(stationCollection);
      /* PointFeatureCollectionIterator iter = stationCollection.getPointFeatureCollectionIterator(-1);
      List<PointFeature> obsList = new ArrayList<PointFeature>();
      int count = 0;
      while (iter.hasNext() && (count++ < maxCount))
        obsList.add((PointFeature) iter.next());
      setObservations(obsList);  */

    } else if (selectedType == FeatureType.STATION_PROFILE) {
      StationProfileFeatureCollection stationCollection = (StationProfileFeatureCollection) selectedCollection;
      PointFeatureCollectionIterator iter = stationCollection.getPointFeatureCollectionIterator(-1);
      List<PointFeature> obsList = new ArrayList<PointFeature>();
      int count = 0;
      while (iter.hasNext() && (count++ < maxCount))
        obsList.add((PointFeature) iter.next());
      setObservations(obsList);
    }
  }

  private void setObservations(PointFeatureCollection pointCollection) throws IOException {
    PointFeatureIterator iter = pointCollection.getPointFeatureIterator(-1);
    //iter.setCalculateBounds(pointCollection);
    List<PointFeature> obsList = new ArrayList<PointFeature>();
    int count = 0;
    try {
      while (iter.hasNext() && (count++ < maxCount))
        obsList.add(iter.next());
    } finally {
      iter.finish();
    }
    setObservations(obsList);
  }

  private void setStnProfileObservations(NestedPointFeatureCollection nestedPointCollection) throws IOException {
    PointFeatureCollectionIterator iter = nestedPointCollection.getPointFeatureCollectionIterator(-1); // not multiple
    List<PointFeatureCollection> pfcList = new ArrayList<PointFeatureCollection>();
    while (iter.hasNext()) {
      pfcList.add(iter.next());
    }
    setStnProfiles(pfcList);
  }

  private void setSectionObservations(SectionFeature sectionFeature) throws IOException {
    PointFeatureCollectionIterator iter = sectionFeature.getPointFeatureCollectionIterator(-1); // not multiple
    List<PointFeatureCollection> pfcList = new ArrayList<PointFeatureCollection>();
    while (iter.hasNext()) {
      pfcList.add(iter.next());
    }
    setSectionProfiles(pfcList);
  }

  private void setObservations(List<PointFeature>obsList) throws IOException {
    if (obsList.size() == 0) {
      obsTable.clear();
      JOptionPane.showMessageDialog(null, "There are no observations for this selection");
      return;
    }
    obsTable.setPointFeatureData(obsList);
    
    if (pointController == null) makePointController();
    pointController.setPointFeatures(obsList);
    //pointDisplayWindow.setVisible(true);
  }

  public PreferencesExt getPrefs() {
    return prefs;
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

  public class StnProfileFeatureBean {
    ProfileFeature pfc;
    int npts;

    public StnProfileFeatureBean(ProfileFeature pfc) {
      this.pfc = pfc;
      npts = pfc.size();
    }

    public String getProfileName() {
      return pfc.getName();
    }

    public void setNobs(int npts) {
      this.npts = npts;
    }

    public int getNobs() {
      return npts;
    }

    public double getLatitude() {
      return pfc.getLatLon().getLatitude();
    }

    public double getLongitude() {
      return pfc.getLatLon().getLongitude();
    }
  }

  public static class StationBean implements ucar.unidata.geoloc.Station {
    private Station s;
    private int npts = -1;

    public StationBean() {
    }

    public StationBean(Station s) {
      this.s = s;
      // this.npts = s.getNumberPoints();
    }

    // for BeanTable
    static public String hiddenProperties() {
      return "latLon";
    }

    public int getNobs() {
      return npts;
    }

    public void setNobs(int npts) {
      this.npts = npts;
    }

    public String getWmoId() {
      return s.getWmoId();
    }

    // all the station dependent methods need to be overridden
    public String getName() {
      return s.getName();
    }

    public String getDescription() {
      return s.getDescription();
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
  }
                                   
  public static class TrajectoryFeatureBean extends StationBean {
    int npts;
    TrajectoryFeature pfc;
    PointFeature pf;

    public TrajectoryFeatureBean(TrajectoryFeature pfc) {
      this.pfc = pfc;
      try {
        if (pfc.hasNext()) {
          pf = pfc.next();
        }
      } catch (IOException ioe) {
        log.warn("Trajectory empty ", ioe);
      }
      npts = pfc.size();
    }

        // for BeanTable
    static public String hiddenProperties() {
      return "latLon";
    }

    public void setNobs(int npts) {
      this.npts = npts;
    }

    public int getNobs() {
      return npts;
    }

    public String getName() {
      return pfc.getName();
    }

    public String getDescription() {
      return null;
    }

    public String getWmoId() {
      return null;
    }

    public double getLatitude() {
      return pf.getLocation().getLatitude();
    }

    public double getLongitude() {
      return  pf.getLocation().getLongitude();
    }

    public double getAltitude() {
      return pf.getLocation().getAltitude();
    }

    public LatLonPoint getLatLon() {
      return pf.getLocation().getLatLon();
    }

    public int compareTo(Station so) {
      return getName().compareTo(so.getName());
    }

    public boolean isMissing() {
      return Double.isNaN(getLatitude());
    }
  }

  public static class SectionFeatureBean extends StationBean {
    int npts;
    SectionFeature pfc;
    ProfileFeature pf;

    public SectionFeatureBean(SectionFeature pfc) {
      this.pfc = pfc;
      try {
        if (pfc.hasNext()) {
          pf = pfc.next();
        }
      } catch (IOException ioe) {
        log.warn("Trajectory empty ", ioe);
      }
      npts = pfc.size();
    }

        // for BeanTable
    static public String hiddenProperties() {
      return "latLon";
    }

    public void setNobs(int npts) {
      this.npts = npts;
    }

    public int getNobs() {
      return npts;
    }

    public String getName() {
      return pfc.getName();
    }

    public String getDescription() {
      return null;
    }

    public String getWmoId() {
      return null;
    }

    public double getLatitude() {
      return pf.getLatLon().getLatitude();
    }

    public double getLongitude() {
      return  pf.getLatLon().getLongitude();
    }

    public double getAltitude() {
      return Double.NaN;
    }

    public LatLonPoint getLatLon() {
      return pf.getLatLon();
    }

    public int compareTo(Station so) {
      return getName().compareTo(so.getName());
    }

    public boolean isMissing() {
      return Double.isNaN(getLatitude());
    }
  }

  public class ProfileFeatureBean extends StationBean {
    int npts;
    ProfileFeature pfc;
    PointFeature pf;

    public ProfileFeatureBean(ProfileFeature pfc) {
      this.pfc = pfc;
      try {
        pfc.calcBounds();
        if (pfc.hasNext())
          pf = pfc.next();
      } catch (IOException ioe) {
        log.warn("Trajectory empty ", ioe);
      }
      pfc.finish();
      npts = pfc.size();
    }

    public void setNobs(int npts) {
      this.npts = npts;
    }

    public int getNobs() {
      return npts;
    }

    public String getName() {
      return pfc.getName();
    }

    public String getDescription() {
      return null;
    }

    public String getWmoId() {
      return null;
    }

    public double getLatitude() {
      return pf.getLocation().getLatitude();
    }

    public double getLongitude() {
      return  pf.getLocation().getLongitude();
    }

    public double getAltitude() {
      return pf.getLocation().getAltitude();
    }

    public LatLonPoint getLatLon() {
      return pf.getLocation().getLatLon();
    }

    public boolean isMissing() {
      return Double.isNaN(getLatitude());
    }

    public int compareTo(Station so) {
      return getName().compareTo(so.getName());
    }
  }

  public static class PointObsBean extends StationBean {  // fake Station, so we can use StationRegionChooser
    private PointFeature pobs;
    private String timeObs;
    private int id;
    private DateFormatter df;

    public PointObsBean(int id, PointFeature obs, DateFormatter df) {
      this.id = id;
      this.pobs = obs;
      this.df = df;
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

    public LatLonPoint getLatLon() {
      return new LatLonPointImpl(getLatitude(), getLongitude());
    }

    public boolean isMissing() {
      return Double.isNaN(getLatitude()) || Double.isNaN(getLongitude());
    }

    public int compareTo(Station so) {
      return getName().compareTo(so.getName());
    }
  }

}