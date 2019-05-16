/*
 * Copyright (c) 1998-2019 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.ui.op;

import javax.annotation.Nullable;
import org.apache.xmlbeans.XmlException;
import ucar.ma2.StructureData;
import ucar.nc2.NCdumpW;
import ucar.nc2.NetcdfFileWriter;
import ucar.nc2.constants.CDM;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.ft.*;
import ucar.nc2.ft.point.StationFeature;
import ucar.nc2.ft.point.writer.CFPointWriter;
import ucar.nc2.ogc.MarshallingUtil;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.time.CalendarDateRange;
import ucar.nc2.ui.dialog.NetcdfOutputChooser;
import ucar.nc2.ui.point.PointController;
import ucar.nc2.ui.point.StationRegionDateChooser;
import ucar.ui.util.Resource;
import ucar.ui.widget.BAMutil;
import ucar.ui.widget.IndependentDialog;
import ucar.ui.widget.PopupMenu;
import ucar.ui.widget.TextHistoryPane;
import ucar.nc2.units.DateRange;
import ucar.unidata.geoloc.LatLonPoint;
import ucar.unidata.geoloc.LatLonPointImpl;
import ucar.unidata.geoloc.LatLonRect;
import ucar.unidata.geoloc.Station;
import ucar.util.prefs.PreferencesExt;
import ucar.ui.prefs.BeanTable;

import ucar.nc2.ui.StructureTable;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Frame;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;
import javax.swing.AbstractAction;
import javax.swing.JPanel;
import javax.swing.JOptionPane;
import javax.swing.JSplitPane;

/**
 * <p>
 * A Swing widget to view the contents of a {@link }PointFeatureDataset}.
 * </p>
 *
 * <p>
 * If it's a {@code StationObsDataset}, the available {@code Station}s are shown in a {@code
 * BeanTable}. The obs are shown in a {@code StructureTable}.
 * </p>
 *
 * @author caron
 */
public class PointFeatureDatasetViewer extends JPanel {

  private static final org.slf4j.Logger log
      = org.slf4j.LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private PreferencesExt prefs;

  private DsgFeatureCollection selectedCollection;
  private FeatureType selectedType;

  private BeanTable fcTable, profileTable, stnTable, stnProfileTable;
  private JPanel changingPane = new JPanel(new BorderLayout());
  private StationRegionDateChooser stationMap;
  private StructureTable obsTable;
  private JSplitPane splitFeatures, splitMap, splitObs;
  private IndependentDialog infoWindow;
  private TextHistoryPane infoTA;

  private PointController pointController;
  private NetcdfOutputChooser outChooser;
  private FeatureDatasetPoint pfDataset;

  private boolean eventsOK = true;

  private int maxCount = Integer.MAX_VALUE;

  /**
   *
   */
  public PointFeatureDatasetViewer(PreferencesExt prefs, JPanel buttPanel) {
    this.prefs = prefs;

    // the info window
    infoTA = new TextHistoryPane();
    infoWindow = new IndependentDialog(null, true, "Station Information", infoTA);
    infoWindow.setBounds(
        (Rectangle) prefs.getBean("InfoWindowBounds", new Rectangle(300, 300, 500, 300)));

    stationMap = new StationRegionDateChooser();
    stationMap.addPropertyChangeListener(new PropertyChangeListener() {
      public void propertyChange(PropertyChangeEvent e) {
        if (e.getPropertyName().equals("Station")) {
          final StationBean selectedStation = (StationBean) e.getNewValue();

          log.debug("selectedStation= {}", selectedStation.getName());

          eventsOK = false;
          stnTable.setSelectedBean(selectedStation);
          eventsOK = true;
        }
      }
    });

    // do the query
    final AbstractAction queryAction = new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent e) {
        if (selectedCollection == null) {
          return;
        }

        // is the date window showing ?
        DateRange dateRange = stationMap.getDateRange();
        log.debug("date range={}", dateRange);

        // is the geoRegion mode true ?
        LatLonRect geoRegion = null;
        StationBean selectedStation = null;

        boolean useRegion = stationMap.getGeoSelectionMode();
        if (useRegion) {
          geoRegion = stationMap.getGeoSelectionLL();
          log.debug("geoRegion={}", geoRegion);
        } else {
          selectedStation = (StationBean) stationMap.getSelectedStation();
        }

        try {
          if (selectedStation != null) {
            setStation(selectedStation, dateRange);
          } else if (useRegion || (dateRange != null)) {
            subset(geoRegion, dateRange);
          } else {
            JOptionPane.showMessageDialog(null, "You must subset in space and/or time ");
          }
        } catch (IOException ioe) {
          ioe.printStackTrace();
        }
      }
    };
    BAMutil.setActionProperties(queryAction, "nj22/Query", "query for data", false, 'Q', -1);
    stationMap.addToolbarAction(queryAction);  // */

    // get all data
    final AbstractAction getallAction = new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent e) {
        if (selectedCollection == null) {
          return;
        }
        try {
          setObservationsAll();
        } catch (IOException ioe) {
          ioe.printStackTrace();
        }
      }
    };
    BAMutil.setActionProperties(getallAction, "GetAll", "get ALL data", false, 'A', -1);
    stationMap.addToolbarAction(getallAction);
    stationMap.addToolbarAction(new WaterMLConverterAction());

    final AbstractAction netcdfAction = new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent e) {
        if (pfDataset == null) {
          return;
        }

        if (outChooser == null) {
          outChooser = new NetcdfOutputChooser((Frame) null);
          outChooser.addPropertyChangeListener("OK", new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt) {
              writeNetcdf((NetcdfOutputChooser.Data) evt.getNewValue());
            }
          });
        }
        outChooser.setOutputFilename(pfDataset.getLocation());
        outChooser.setVisible(true);
      }
    };
    BAMutil.setActionProperties(netcdfAction, "nj22/Netcdf", "Write netCDF-CF file", false, 'S', -1);
    BAMutil.addActionToContainer(buttPanel, netcdfAction);

    // feature collection table
    fcTable = new BeanTable(FeatureCollectionBean.class,
        (PreferencesExt) prefs.node("FeatureCollectionBean"), false);
    fcTable.addListSelectionListener(e -> {
      FeatureCollectionBean fcb = (FeatureCollectionBean) fcTable.getSelectedBean();
      try {
        setFeatureCollection(fcb);
      } catch (Exception exc) {
        JOptionPane.showMessageDialog(null,
            "Error reading FeatureCollection " + fcb.fc.getName() + " error=" + exc.getMessage());
        exc.printStackTrace();
      }
    });
    new BeanContextMenu(fcTable);

    // profile table
    profileTable = new BeanTable(ProfileFeatureBean.class,
        (PreferencesExt) prefs.node("ProfileFeatureBean"), false);
    profileTable.addListSelectionListener(e -> {
      ProfileFeatureBean sb = (ProfileFeatureBean) profileTable.getSelectedBean();
      try {
        setProfile(sb);
        profileTable.fireBeanDataChanged(sb);
      } catch (IOException ioe) {
        ioe.printStackTrace();
      }
    });
    new BeanContextMenu(profileTable);

    // station table
    stnTable = new BeanTable(StationBean.class, (PreferencesExt) prefs.node("StationBeans"), false);
    stnTable.addListSelectionListener(e -> {
      StationBean sb = (StationBean) stnTable.getSelectedBean();
      try {
        setStation(sb, null);
        stnTable.fireBeanDataChanged(sb);
      } catch (IOException ioe) {
        ioe.printStackTrace();
      }
      if (eventsOK) {
        stationMap.setSelectedStation(sb.getName());
      }
    });
    new BeanContextMenu(stnTable);

    // station profile table
    stnProfileTable = new BeanTable(StnProfileFeatureBean.class,
        (PreferencesExt) prefs.node("StnProfileFeatureBean"), false);
    stnProfileTable.addListSelectionListener(e -> {
      StnProfileFeatureBean sb = (StnProfileFeatureBean) stnProfileTable.getSelectedBean();
      try {
        setStnProfile(sb);
        stnProfileTable.fireBeanDataChanged(sb);
      } catch (IOException ioe) {
        ioe.printStackTrace();
      }
    });
    new BeanContextMenu(stnProfileTable);

    pointController = new PointController();

    // the obs table
    obsTable = new StructureTable((PreferencesExt) prefs.node("ObsBean"));

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

  /**
   *
   */
  private class BeanContextMenu extends PopupMenu {

    final BeanTable beanTable2;

    /**
     *
     */
    BeanContextMenu(BeanTable beanTable) {
      super(beanTable.getJTable(), "Options");
      this.beanTable2 = beanTable;

      addAction("Show Fields", new AbstractAction() {
        public void actionPerformed(ActionEvent e) {
          final FeatureBean bean = (FeatureBean) beanTable2.getSelectedBean();
          if (bean == null) {
            return;
          }
          infoTA.clear();
          infoTA.appendLine(bean.showFields());
          infoTA.gotoTop();
          infoWindow.setVisible(true);
        }
      });
    }
  }

// I'd like to offload this class into its own file, but it needs to read the pfDataset field, whose value may change
// during execution. I could still do it if I added the necessary event listener machinery, but meh.

  /**
   *
   */
  private class WaterMLConverterAction extends AbstractAction {

    /**
     *
     */
    private WaterMLConverterAction() {
      putValue(NAME, "WaterML 2.0 Writer");
      putValue(SMALL_ICON, Resource.getIcon(BAMutil.getResourcePath() + "nj22/drop_24.png", true));
      putValue(SHORT_DESCRIPTION, "Write timeseries as an OGC WaterML v2.0 document.");
    }

    /**
     *
     */
    @Override
    public void actionPerformed(ActionEvent e) {
      if (pfDataset == null) {
        return;
      }

      if (!pfDataset.getFeatureType().equals(FeatureType.STATION)) {
        final Component parentComponent = PointFeatureDatasetViewer.this;
        final Object message =
            "Currently, only the STATION feature type is supported, not " + pfDataset
                .getFeatureType();
        final String title = "Invalid feature type";
        final int messageType = JOptionPane.ERROR_MESSAGE;

        JOptionPane.showMessageDialog(parentComponent, message, title, messageType);
        return;
      }

      try {
        ByteArrayOutputStream outStream = new ByteArrayOutputStream(5000);
        MarshallingUtil.marshalPointDataset(pfDataset, pfDataset.getDataVariables(), outStream);

        infoTA.setText(outStream.toString(CDM.utf8Charset.name()));
        infoTA.gotoTop();
        infoWindow.setVisible(true);
      } catch (IOException | XmlException ex) {
        StringWriter sw = new StringWriter(5000);
        ex.printStackTrace(new PrintWriter(sw));

        infoTA.setText(sw.toString());
        infoTA.gotoTop();
        infoWindow.setVisible(true);
      }
    }
  }

  /**
   *
   */
  public void clear() {
    fcTable.clearBeans();
    stnTable.clearBeans();
    profileTable.clearBeans();
    stnProfileTable.clearBeans();

    infoTA.clear();
    stationMap.setStations(new ArrayList());
    obsTable.clear();
    selectedCollection = null;
  }

  /**
   *
   */
  public void save() {
    fcTable.saveState(false);
    stnTable.saveState(false);
    profileTable.saveState(false);
    stnProfileTable.saveState(false);

    prefs.putBeanObject("InfoWindowBounds", infoWindow.getBounds());
    //if (pointDisplayWindow != null) prefs.putBeanObject("PointDisplayBounds", pointDisplayWindow.getBounds());
    prefs.putInt("splitPosO", splitObs.getDividerLocation());
    prefs.putInt("splitPosF", splitFeatures.getDividerLocation());
    prefs.putInt("splitPosM", splitMap.getDividerLocation());
    obsTable.saveState();
  }

  /**
   *
   */
  private void writeNetcdf(NetcdfOutputChooser.Data data) {
    if (data.version == NetcdfFileWriter.Version.ncstream) {
      return;
    }

    try {
      int count = CFPointWriter
          .writeFeatureCollection(pfDataset, data.outputFilename, data.version);
      JOptionPane.showMessageDialog(this, count + " records written");
    } catch (Exception ioe) {
      JOptionPane.showMessageDialog(this, "ERROR: " + ioe.getMessage());
      ioe.printStackTrace();
    }
  }

  /**
   *
   */
  public void setDataset(FeatureDatasetPoint dataset) {
    this.pfDataset = dataset;

    clear();

    // set the feature collection table - all use this
    List<FeatureCollectionBean> fcBeans = new ArrayList<>();
    for (DsgFeatureCollection fc : dataset.getPointFeatureCollectionList()) {
      fcBeans.add(new FeatureCollectionBean(fc));
    }
    if (fcBeans.size() == 0) {
      JOptionPane
          .showMessageDialog(null, "No PointFeatureCollections found that could be displayed");
    }

    fcTable.setBeans(fcBeans);
    infoTA.clear();

    // set the date range if possible
    CalendarDateRange dr = dataset.getCalendarDateRange();
    if (dr != null) {
      stationMap.setDateRange(dr.toDateRange());
    }

    // set the bounding box if possible
    LatLonRect bb = dataset.getBoundingBox();
    if (bb != null) {
      stationMap.setGeoBounds(bb);
    }
  }

  /**
   *
   */
  private void setFeatureCollection(FeatureCollectionBean fcb) throws IOException {
    // reconfigure the UI depending on the type
    changingPane.removeAll();

    FeatureType ftype = fcb.fc.getCollectionFeatureType();

    switch (ftype) {
      case POINT: {
        //PointFeatureCollection pfc = (PointFeatureCollection) fcb.fc;
        //setPointCollection(pfc);
        changingPane.add(pointController, BorderLayout.CENTER);
        break;
      }
      case PROFILE: {
        ProfileFeatureCollection pfc = (ProfileFeatureCollection) fcb.fc;
        setProfileCollection(pfc);
        changingPane.add(stnTable, BorderLayout.CENTER);
        break;
      }
      case STATION: {
        setStations(((StationTimeSeriesFeatureCollection) fcb.fc).getStationFeatures());
        changingPane.add(stnTable, BorderLayout.CENTER);
        break;
      }
      case STATION_PROFILE: {
        setStations(((StationProfileFeatureCollection) fcb.fc).getStationFeatures());

        JSplitPane splitExtra = new JSplitPane(JSplitPane.VERTICAL_SPLIT, false, stnTable,
            stnProfileTable);
        splitExtra.setDividerLocation(changingPane.getHeight() / 2);
        changingPane.add(splitExtra, BorderLayout.CENTER);
        break;
      }
      case TRAJECTORY: {
        TrajectoryFeatureCollection pfc = (TrajectoryFeatureCollection) fcb.fc;
        setTrajectoryCollection(pfc);
        changingPane.add(stnTable, BorderLayout.CENTER);
        break;
      }
      case TRAJECTORY_PROFILE: {
        TrajectoryProfileFeatureCollection pfc = (TrajectoryProfileFeatureCollection) fcb.fc;
        setSectionCollection(pfc);

        JSplitPane splitExtra = new JSplitPane(JSplitPane.VERTICAL_SPLIT, false, stnTable,
            profileTable);
        splitExtra.setDividerLocation(changingPane.getHeight() / 2);
        changingPane.add(splitExtra, BorderLayout.CENTER);
        break;
      }

      default:
        // Nothing to do here
    }

    this.selectedCollection = fcb.fc;
    this.selectedType = ftype;

    // Redraw the GUI with the new tables.
    revalidate();
    repaint();
  }

  /**
   *
   */
  private void setStations(List<StationFeature> stations) {
    List<StationBean> stationBeans = new ArrayList<>();
    try {
      for (Station station : stations) {
        stationBeans.add(new StationBean(station));
      }
    } catch (IOException ioe) {
      ioe.printStackTrace();
      return;
    }

    stnTable.setBeans(stationBeans);
    stationMap.setStations(stationBeans);
    obsTable.clear();
  }

  /**
   *
   */
  private void setPointCollection(PointFeatureCollection pointCollection) throws IOException {
    List<PointObsBean> pointBeans = new ArrayList<>();
    int count = 0;

    try (PointFeatureIterator iter = pointCollection.getPointFeatureIterator()) {
      while (iter.hasNext() && (count++ < maxCount)) {
        PointFeature pob = iter.next();
        pointBeans.add(new PointObsBean(count++, pob));
      }
    }

    stnTable.setBeans(pointBeans);
    stationMap.setStations(pointBeans);
    stnTable.clearSelectedCells();
  }

  /**
   *
   */
  private void setProfileCollection(ProfileFeatureCollection profileCollection) throws IOException {
    List<ProfileFeatureBean> beans = new ArrayList<>();
    for (ProfileFeature profile : profileCollection) {
      ProfileFeatureBean bean = new ProfileFeatureBean(profile);
      if (bean.pf != null) {
        // may have missing values
        beans.add(bean);
      }
    }

    stnTable.setBeans(beans);
    stationMap.setStations(beans);
    stnTable.clearSelectedCells();
  }

  /**
   *
   */
  private void setTrajectoryCollection(TrajectoryFeatureCollection trajCollection)
      throws IOException {
    List<TrajectoryFeatureBean> beans = new ArrayList<>();
    for (TrajectoryFeature traj : trajCollection) {
      TrajectoryFeatureBean trajBean = new TrajectoryFeatureBean(traj);
      if (trajBean.pf != null) {
        // may have missing values
        beans.add(trajBean);
      }
    }

    stnTable.setBeans(beans);
    stationMap.setStations(beans);
    stnTable.clearSelectedCells();
  }

  /**
   *
   */
  private void setSectionCollection(TrajectoryProfileFeatureCollection sectionCollection)
      throws IOException {
    List<SectionFeatureBean> beans = new ArrayList<>();

    for (TrajectoryProfileFeature sectionFeature : sectionCollection) {
      SectionFeatureBean bean = new SectionFeatureBean(sectionFeature);
      if (bean.pf != null) {
        // may have missing values
        beans.add(bean);
      }
    }

    stnTable.setBeans(beans);
    stationMap.setStations(beans);
    stnTable.clearSelectedCells();
  }

  /**
   *
   */
  private void subset(LatLonRect geoRegion, DateRange dateRange) throws IOException {
    PointFeatureCollection pc = null;
    CalendarDateRange cdr = CalendarDateRange.of(dateRange);

    if (selectedType == FeatureType.POINT) {
      PointFeatureCollection ptCollection = (PointFeatureCollection) selectedCollection;
      pc = ptCollection.subset(geoRegion, cdr);

    } else if (selectedType == FeatureType.STATION) {
      StationTimeSeriesFeatureCollection stationCollection = (StationTimeSeriesFeatureCollection) selectedCollection;
         /*  if (geoRegion != null) {
            StationTimeSeriesFeatureCollection stationSubset = stationCollection.subset(geoRegion);
            setStations( stationSubset);
            return;
          } else { */
      pc = stationCollection.flatten(geoRegion, cdr);
      //} LOOK
    }
        /* else if (selectedType == FeatureType.STATION_PROFILE) {
          StationProfileFeatureCollection stationProfileCollection = (StationProfileFeatureCollection) selectedCollection;
          pc = stationProfileCollection.flatten(geoRegion, cdr);
        } */

    if (null != pc) {
      setObservations(pc);
    }
  }

  /**
   *
   */
  private void setStation(StationBean sb, DateRange dr) throws IOException {
    if (selectedType == FeatureType.POINT) {
      PointObsBean pobsBean = (PointObsBean) sb;
      List<PointFeature> obsList = new ArrayList<>();
      obsList.add(pobsBean.pobs);
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
      StationTimeSeriesFeature feature = stationCollection.getStationTimeSeriesFeature(sb.stnFeat);
      if (dr != null) {
        feature = feature.subset(CalendarDateRange.of(dr));
      }
      setObservations(feature);

      // iterator may count points
      int npts = feature.size();
      if (npts >= 0) {
        sb.setNobs(npts);
      }
    } else if (selectedType == FeatureType.STATION_PROFILE) {
      StationProfileFeatureCollection stationCollection = (StationProfileFeatureCollection) selectedCollection;
      StationProfileFeature feature = stationCollection.getStationProfileFeature(sb.stnFeat);
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
    } else if (selectedType == FeatureType.TRAJECTORY_PROFILE) {
      SectionFeatureBean sectionBean = (SectionFeatureBean) sb;
      setSection(sectionBean);

      // iterator may count points
      TrajectoryProfileFeature feature = sectionBean.pfc;
      int npts = feature.size();
      if (npts >= 0) {
        sb.setNobs(npts);
      }
    }
  }

  /**
   *
   */
  private void setStnProfile(StnProfileFeatureBean sb) throws IOException {
    ProfileFeature feature = sb.pfc;
    setObservations(feature);

    // iterator may count points
    int npts = feature.size();
    if (npts >= 0) {
      sb.setNobs(npts);
    }
  }

  /**
   *
   */
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

  /**
   *
   */
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

  /**
   *
   */
  private void setSection(SectionFeatureBean sb) throws IOException {
    TrajectoryProfileFeature feature = sb.pfc;
    setSectionObservations(feature);

    // iterator may count points
    int npts = feature.size();
    if (npts >= 0) {
      sb.setNobs(npts);
      stnTable.refresh();
    }
  }

  /**
   *
   */
  private void setStnProfiles(List<PointFeatureCollection> pfcList) throws IOException {
    List<StnProfileFeatureBean> beans = new ArrayList<>();

    for (PointFeatureCollection pfc : pfcList) {
      beans.add(new StnProfileFeatureBean((ProfileFeature) pfc));
    }

    stnProfileTable.setBeans(beans);
    obsTable.clear();
  }

  /**
   *
   */
  private void setSectionProfiles(List<PointFeatureCollection> pfcList) throws IOException {
    List<ProfileFeatureBean> beans = new ArrayList<>();

    for (PointFeatureCollection pfc : pfcList) {
      beans.add(new ProfileFeatureBean((ProfileFeature) pfc));
    }

    profileTable.setBeans(beans);
    obsTable.clear();
  }

  /**
   *
   */
  private void setObservationsAll() throws IOException {
    if (selectedType == FeatureType.POINT) {
      PointFeatureCollection pointCollection = (PointFeatureCollection) selectedCollection;
      setObservations(pointCollection);
    } else if (selectedType == FeatureType.STATION) {
      StationTimeSeriesFeatureCollection stationCollection = (StationTimeSeriesFeatureCollection) selectedCollection;
      PointFeatureCollection pfc = stationCollection.flatten(null, null);
      List<PointFeature> obsList = new ArrayList<>();
      int count = 0;
      for (PointFeature pf : pfc) {
        if (count++ > maxCount) {
          break;
        }
        obsList.add(pf);
      }
      setObservations(obsList);
    } else if (selectedType == FeatureType.STATION_PROFILE) {
      StationProfileFeatureCollection stationCollection = (StationProfileFeatureCollection) selectedCollection;
      List<PointFeature> obsList = new ArrayList<>();
      int count = 0;
      for (StationProfileFeature spf : stationCollection) {
        for (ProfileFeature pf : spf) {
          for (PointFeature f : pf) {
            if (count++ > maxCount) {
              break;
            }
            obsList.add(f);
          }
        }
      }
      setObservations(obsList);
    }
  }

  /**
   *
   */
  private int setObservations(PointFeatureCollection pointCollection) throws IOException {
    int count = 0;
    List<PointFeature> obsList = new ArrayList<>();
    for (PointFeature pf : pointCollection) {
      if (count++ > maxCount) {
        break;
      }
      obsList.add(pf);
    }

    setObservations(obsList);
    return obsList.size();
  }

  /**
   *
   */
  private void setStnProfileObservations(StationProfileFeature stationProfileFeature)
      throws IOException {
    List<PointFeatureCollection> pfcList = new ArrayList<>();
    for (PointFeatureCollection pfc : stationProfileFeature) {
      pfcList.add(pfc);
    }
    setStnProfiles(pfcList);
  }

  /**
   *
   */
  private void setSectionObservations(TrajectoryProfileFeature sectionFeature) throws IOException {
    List<PointFeatureCollection> pfcList = new ArrayList<>();
    for (PointFeatureCollection pfc : sectionFeature) {
      pfcList.add(pfc);
    }
    setSectionProfiles(pfcList);
  }

  /**
   *
   */
  private void setObservations(List<PointFeature> obsList) throws IOException {
    if (obsList.size() == 0) {
      obsTable.clear();
      JOptionPane.showMessageDialog(null, "There are no observations for this selection");
      return;
    }
    obsTable.setPointFeatureData(obsList);

    pointController.setPointFeatures(obsList);
    //pointDisplayWindow.setVisible(true);
  }

  /**
   *
   */
  public PreferencesExt getPrefs() {
    return prefs;
  }

  /**
   *
   */
  public static class FeatureCollectionBean {

    DsgFeatureCollection fc;

    /**
     *
     */
    public FeatureCollectionBean(DsgFeatureCollection fc) {
      this.fc = fc;
    }

    public String getName() {
      return fc.getName();
    }

    public String getFeatureType() {
      return fc.getCollectionFeatureType().toString();
    }
  }

  /**
   *
   */
  public static class FeatureBean {

    StructureData sdata;
    String fields;

    /**
     *
     */
    public FeatureBean() {
    }

    /**
     *
     */
    FeatureBean(StructureData sdata) throws IOException {
      this.sdata = sdata;
      fields = NCdumpW.toString(sdata);
    }

    public String getFields() {
      return fields;
    }

    public String showFields() {
      StringWriter sw = new StringWriter(10000);
      try {
        NCdumpW.printStructureData(new PrintWriter(sw), sdata);
      } catch (IOException e) {
        e.printStackTrace(new PrintWriter(sw));
      }
      return sw.toString();
    }
  }

  /**
   *
   */
  public static class StationBean extends FeatureBean implements Station {

    private StationFeature stnFeat;
    private int npts = -1;

    /**
     *
     */
    public StationBean() {
    }

    /**
     *
     */
    public StationBean(StructureData sdata) throws IOException {
      super(sdata);
    }

    /**
     *
     */
    public StationBean(Station s) throws IOException {
      super(((StationFeature) s).getFeatureData());
      this.stnFeat = (StationFeature) s;
      this.npts = s.getNobs();
    }

    // for BeanTable
    public static String hiddenProperties() {
      return "latLon";
    }

    public int getNobs() {
      return npts;
    }

    public void setNobs(int npts) {
      this.npts = npts;
    }

    public String getWmoId() {
      return stnFeat.getWmoId();
    }

    // all the station dependent methods need to be overridden
    public String getName() {
      return stnFeat.getName();
    }

    public String getDescription() {
      return stnFeat.getDescription();
    }

    public double getLatitude() {
      return stnFeat.getLatitude();
    }

    public double getLongitude() {
      return stnFeat.getLongitude();
    }

    public double getAltitude() {
      return stnFeat.getAltitude();
    }

    public LatLonPoint getLatLon() {
      return stnFeat.getLatLon();
    }

    public boolean isMissing() {
      return stnFeat.isMissing();
    }

    public int compareTo(Station so) {
      return getName().compareTo(so.getName());
    }
  }

  /**
   * fake Station, so we can use StationRegionChooser
   */
  public static class PointObsBean extends StationBean {

    private PointFeature pobs;
    private String timeObs;
    private int id;

    /**
     *
     */
    public PointObsBean(int id, PointFeature obs) {
      this.id = id;
      this.pobs = obs;
      timeObs = obs.getObservationTimeAsCalendarDate().toString();
    }

    // for BeanTable
    public static String hiddenProperties() {
      return "latLon description wmoId";
    }

    public String getTime() {
      return timeObs;
    }

    public String getName() {
      return Integer.toString(id);
    }

    @Nullable
    public String getDescription() {
      return null;
    }

    @Nullable
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

  /**
   *
   */
  public static class TrajectoryFeatureBean extends StationBean {

    int npts;
    TrajectoryFeature pfc;
    PointFeature pf;

    /**
     *
     */
    public TrajectoryFeatureBean(TrajectoryFeature pfc) throws IOException {
      super(pfc.getFeatureData());
      this.pfc = pfc;
      try {
        pfc.resetIteration();
        if (pfc.hasNext()) {
          pf = pfc.next();    // get first one
        }
      } catch (IOException ioe) {
        log.warn("Trajectory empty ", ioe);
      }
      npts = pfc.size();
    }

    // for BeanTable
    public static String hiddenProperties() {
      return "latLon description wmoId";
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

    @Nullable
    public String getDescription() {
      return null;
    }

    @Nullable
    public String getWmoId() {
      return null;
    }

    public double getLatitude() {
      return pf.getLocation().getLatitude();
    }

    public double getLongitude() {
      return pf.getLocation().getLongitude();
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

  /**
   *
   */
  public static class ProfileFeatureBean extends StationBean {

    int npts;
    ProfileFeature pfc;
    PointFeature pf;

    /**
     *
     */
    public ProfileFeatureBean(ProfileFeature pfc) throws IOException {
      super(pfc.getFeatureData());
      this.pfc = pfc;

      // this calculates the size, etc
      for (PointFeature pf2 : pfc) {
        pf = pf2; // a random point
      }
      npts = pfc.size();
    }

    // for BeanTable
    public static String hiddenProperties() {
      return "latLon description wmoId";
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

    @Nullable
    public String getDescription() {
      return null;
    }

    @Nullable
    public String getWmoId() {
      return null;
    }

    public double getLatitude() {
      return pf == null ? Double.NaN : pf.getLocation().getLatitude();
    }

    public double getLongitude() {
      return pf == null ? Double.NaN : pf.getLocation().getLongitude();
    }

    public double getAltitude() {
      return pf == null ? Double.NaN : pf.getLocation().getAltitude();
    }

    @Nullable
    public LatLonPoint getLatLon() {
      return pf == null ? null : pf.getLocation().getLatLon();
    }

    public boolean isMissing() {
      return Double.isNaN(getLatitude());
    }

    public int compareTo(Station so) {
      return getName().compareTo(so.getName());
    }
  }

  /**
   *
   */
  public static class StnProfileFeatureBean extends FeatureBean {

    ProfileFeature pfc;
    int npts;

    /**
     *
     */
    public StnProfileFeatureBean(ProfileFeature pfc) throws IOException {
      super(pfc.getFeatureData());
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

    public CalendarDate getDate() {
      return pfc.getTime();
    }
  }

  /**
   *
   */
  public static class SectionFeatureBean extends StationBean {

    int npts;
    TrajectoryProfileFeature pfc;
    ProfileFeature pf;

    /**
     *
     */
    public SectionFeatureBean(TrajectoryProfileFeature pfc) throws IOException {
      super(pfc.getFeatureData());
      this.pfc = pfc;
      try {
        if (pfc.hasNext()) {
          pf = pfc.next();                   // get first one
        }
      } catch (IOException ioe) {
        log.warn("Section empty ", ioe);
      }
      npts = pfc.size();
    }

    // for BeanTable
    public static String hiddenProperties() {
      return "latLon description wmoId";
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

    @Nullable
    public String getDescription() {
      return null;
    }

    @Nullable
    public String getWmoId() {
      return null;
    }

    public double getLatitude() {
      return pf.getLatLon().getLatitude();
    }

    public double getLongitude() {
      return pf.getLatLon().getLongitude();
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
}
