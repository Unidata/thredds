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

import org.n52.oxf.xmlbeans.parser.XMLHandlingException;
import ucar.ma2.StructureData;
import ucar.nc2.NCdumpW;
import ucar.nc2.NetcdfFileWriter;
import ucar.nc2.constants.CDM;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.ft.*;
import ucar.nc2.ft.point.StationFeature;
import ucar.nc2.ft.point.writer.CFPointWriter;
import ucar.nc2.ogc.MarshallingUtil;
import ucar.nc2.time.CalendarDateRange;
import ucar.nc2.ui.dialog.NetcdfOutputChooser;
import ucar.nc2.ui.point.PointController;
import ucar.nc2.ui.point.StationRegionDateChooser;
import ucar.nc2.ui.util.Resource;
import ucar.nc2.ui.widget.*;
import ucar.nc2.units.DateFormatter;
import ucar.nc2.units.DateRange;
import ucar.unidata.geoloc.LatLonPoint;
import ucar.unidata.geoloc.LatLonPointImpl;
import ucar.unidata.geoloc.LatLonRect;
import ucar.unidata.geoloc.Station;
import ucar.util.prefs.PreferencesExt;
import ucar.util.prefs.ui.BeanTable;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.beans.*;
import java.io.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

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

  private BeanTable fcTable, profileTable, stnTable, stnProfileTable;
  private JPanel changingPane = new JPanel(new BorderLayout());
  private StationRegionDateChooser stationMap;
  private StructureTable obsTable;
  private JSplitPane splitFeatures, splitMap, splitObs;
  private IndependentDialog infoWindow;
  //private IndependentWindow pointDisplayWindow;
  private TextHistoryPane infoTA;

  private PointController pointController;
  private NetcdfOutputChooser outChooser;

  private FeatureDatasetPoint pfDataset;


  private DateFormatter df = new DateFormatter();

  private boolean eventsOK = true;
  private boolean debugStationRegionSelect = false, debugQuery = false;

  private int maxCount = Integer.MAX_VALUE;

  public PointFeatureDatasetViewer(PreferencesExt prefs, JPanel buttPanel) {
    this.prefs = prefs;

    // the info window
    infoTA = new TextHistoryPane();
    infoWindow = new IndependentDialog(null, true, "Station Information", infoTA);
    infoWindow.setBounds((Rectangle) prefs.getBean("InfoWindowBounds", new Rectangle(300, 300, 500, 300)));

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
          } else if (useRegion || (dateRange != null)) {
            subset(geoRegion, dateRange);
          } else {
            JOptionPane.showMessageDialog(null, "You must subset in space and/or time ");
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
    stationMap.addToolbarAction(new WaterMLConverterAction());

    AbstractAction netcdfAction = new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        if (pfDataset == null) return;

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
    BAMutil.setActionProperties(netcdfAction, "netcdf", "Write netCDF-CF file", false, 'S', -1);
    BAMutil.addActionToContainer(buttPanel, netcdfAction);


    // feature collection table
    fcTable = new BeanTable(FeatureCollectionBean.class, (PreferencesExt) prefs.node("FeatureCollectionBean"), false);
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
    new BeanContextMenu(fcTable);

    // profile table
    profileTable = new BeanTable(ProfileFeatureBean.class, (PreferencesExt) prefs.node("ProfileFeatureBean"), false);
    profileTable.addListSelectionListener(new ListSelectionListener() {
      public void valueChanged(ListSelectionEvent e) {
        ProfileFeatureBean sb = (ProfileFeatureBean) profileTable.getSelectedBean();
        try {
          setProfile(sb);
          profileTable.fireBeanDataChanged(sb);
        } catch (IOException e1) {
          e1.printStackTrace();
        }
      }
    });
    new BeanContextMenu(profileTable);

    // station table
    stnTable = new BeanTable(StationBean.class, (PreferencesExt) prefs.node("StationBeans"), false);
    stnTable.addListSelectionListener(new ListSelectionListener() {
      public void valueChanged(ListSelectionEvent e) {
        StationBean sb = (StationBean) stnTable.getSelectedBean();
        try {
          setStation(sb, null);
          stnTable.fireBeanDataChanged(sb);
        } catch (IOException e1) {
          e1.printStackTrace();
        }
        if (eventsOK) stationMap.setSelectedStation(sb.getName());
      }
    });
    new BeanContextMenu(stnTable);

    // station profile table
    stnProfileTable = new BeanTable(StnProfileFeatureBean.class, (PreferencesExt) prefs.node("StnProfileFeatureBean"), false);
    stnProfileTable.addListSelectionListener(new ListSelectionListener() {
      public void valueChanged(ListSelectionEvent e) {
        StnProfileFeatureBean sb = (StnProfileFeatureBean) stnProfileTable.getSelectedBean();
        try {
          setStnProfile(sb);
          stnProfileTable.fireBeanDataChanged(sb);
        } catch (IOException e1) {
          e1.printStackTrace();
        }
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

  private class BeanContextMenu extends ucar.nc2.ui.widget.PopupMenu {
    final BeanTable beanTable2;

    BeanContextMenu(BeanTable beanTable) {
      super(beanTable.getJTable(), "Options");
      this.beanTable2 = beanTable;

      addAction("Show Fields", new AbstractAction() {
        public void actionPerformed(ActionEvent e) {
          FeatureBean bean = (FeatureBean) beanTable2.getSelectedBean();
          if (bean == null) return;
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
  private class WaterMLConverterAction extends AbstractAction {
    private WaterMLConverterAction() {
      putValue(NAME, "WaterML 2.0 Writer");
      putValue(SMALL_ICON, Resource.getIcon(BAMutil.getResourcePath() + "drop_24.png", true));
      putValue(SHORT_DESCRIPTION, "Write timeseries as an OGC WaterML v2.0 document.");
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      if (pfDataset == null) {
        return;
      }

      if (!pfDataset.getFeatureType().equals(FeatureType.STATION)) {
        Component parentComponent = PointFeatureDatasetViewer.this;
        Object message = "Currently, only the STATION feature type is supported, not " + pfDataset.getFeatureType();
        String title = "Invalid feature type";
        int messageType = JOptionPane.ERROR_MESSAGE;

        JOptionPane.showMessageDialog(parentComponent, message, title, messageType);
        return;
      }

      try {
        ByteArrayOutputStream outStream = new ByteArrayOutputStream(5000);
        MarshallingUtil.marshalPointDataset(pfDataset, pfDataset.getDataVariables(), outStream);

        infoTA.setText(outStream.toString(CDM.utf8Charset.name()));
        infoTA.gotoTop();
        infoWindow.setVisible(true);
      } catch (IOException | XMLHandlingException ex) {
        StringWriter sw = new StringWriter(5000);
        ex.printStackTrace(new PrintWriter(sw));

        infoTA.setText(sw.toString());
        infoTA.gotoTop();
        infoWindow.setVisible(true);
      }
    }
  }

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

  private void writeNetcdf(NetcdfOutputChooser.Data data) {
    if (data.version == NetcdfFileWriter.Version.ncstream) return;

    try {
      int count = CFPointWriter.writeFeatureCollection(pfDataset, data.outputFilename, data.version);
      JOptionPane.showMessageDialog(this, count + " records written");
    } catch (Exception ioe) {
      JOptionPane.showMessageDialog(this, "ERROR: " + ioe.getMessage());
      ioe.printStackTrace();
    }
  }

  public void setDataset(FeatureDatasetPoint dataset) {
    this.pfDataset = dataset;

    clear();

    // set the feature collection table - all use this
    List<FeatureCollectionBean> fcBeans = new ArrayList<>();
    for (FeatureCollection fc : dataset.getPointFeatureCollectionList()) {
      fcBeans.add(new FeatureCollectionBean(fc));
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
      changingPane.add(pointController, BorderLayout.CENTER);

    } else if (ftype == FeatureType.PROFILE) {
      ProfileFeatureCollection pfc = (ProfileFeatureCollection) fcb.fc;
      setProfileCollection(pfc);
      changingPane.add(stnTable, BorderLayout.CENTER);
    } else if (ftype == FeatureType.STATION) {
      StationCollection sfc = (StationCollection) fcb.fc;
      setStations(sfc);
      changingPane.add(stnTable, BorderLayout.CENTER);

    } else if (ftype == FeatureType.STATION_PROFILE) {
      StationCollection sfc = (StationCollection) fcb.fc;
      setStations(sfc);

      JSplitPane splitExtra = new JSplitPane(JSplitPane.VERTICAL_SPLIT, false, stnTable, stnProfileTable);
      splitExtra.setDividerLocation(changingPane.getHeight() / 2);
      changingPane.add(splitExtra, BorderLayout.CENTER);

    } else if (ftype == FeatureType.TRAJECTORY) {
      TrajectoryFeatureCollection pfc = (TrajectoryFeatureCollection) fcb.fc;
      setTrajectoryCollection(pfc);
      changingPane.add(stnTable, BorderLayout.CENTER);

    } else if (ftype == FeatureType.SECTION) {
      SectionFeatureCollection pfc = (SectionFeatureCollection) fcb.fc;
      setSectionCollection(pfc);

      JSplitPane splitExtra = new JSplitPane(JSplitPane.VERTICAL_SPLIT, false, stnTable, profileTable);
      splitExtra.setDividerLocation(changingPane.getHeight() / 2);
      changingPane.add(splitExtra, BorderLayout.CENTER);
    }

    this.selectedCollection = fcb.fc;
    this.selectedType = ftype;

    // Redraw the GUI with the new tables.
    revalidate();
    repaint();
  }

  private void setStations(StationCollection stationCollection) {
    List<StationBean> stationBeans = new ArrayList<>();
    try {
      List<Station> stations = stationCollection.getStations();
      if (stations == null) return;

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

  private void setPointCollection(PointFeatureCollection pointCollection) throws IOException {
    List<PointObsBean> pointBeans = new ArrayList<>();
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
    List<ProfileFeatureBean> beans = new ArrayList<>();

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
    List<TrajectoryFeatureBean> beans = new ArrayList<>();

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
    List<SectionFeatureBean> beans = new ArrayList<>();

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
    } else if (selectedType == FeatureType.STATION) {
      StationTimeSeriesFeatureCollection stationCollection = (StationTimeSeriesFeatureCollection) selectedCollection;
     /*  if (geoRegion != null) {
        StationTimeSeriesFeatureCollection stationSubset = stationCollection.subset(geoRegion);
        setStations( stationSubset);
        return;
      } else { */
      pc = stationCollection.flatten(geoRegion, dateRange);
      //}
    } else if (selectedType == FeatureType.STATION_PROFILE) {
      StationProfileFeatureCollection stationProfileCollection = (StationProfileFeatureCollection) selectedCollection;
      pc = stationProfileCollection.flatten(geoRegion, dateRange);
    }

    if (null != pc)
      setObservations(pc);
  }

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
    List<StnProfileFeatureBean> beans = new ArrayList<>();

    for (PointFeatureCollection pfc : pfcList)
      beans.add(new StnProfileFeatureBean((ProfileFeature) pfc));

    stnProfileTable.setBeans(beans);
    obsTable.clear();
  }

  private void setSectionProfiles(List<PointFeatureCollection> pfcList) throws IOException {
    List<ProfileFeatureBean> beans = new ArrayList<>();

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
      PointFeatureCollection pfc = stationCollection.flatten(null, (CalendarDateRange) null);
      PointFeatureIterator iter = pfc.getPointFeatureIterator(-1);
      List<PointFeature> obsList = new ArrayList<>();
      int count = 0;
      while (iter.hasNext() && (count++ < maxCount))
        obsList.add(iter.next());
      setObservations(obsList);

    } else if (selectedType == FeatureType.STATION_PROFILE) {
      StationProfileFeatureCollection stationCollection = (StationProfileFeatureCollection) selectedCollection;
      PointFeatureCollectionIterator iter = stationCollection.getPointFeatureCollectionIterator(-1);
      List<PointFeature> obsList = new ArrayList<>();
      int count = 0;
      while (iter.hasNext() && (count++ < maxCount))
        obsList.add((PointFeature) iter.next());
      setObservations(obsList);
    }
  }

  private int setObservations(PointFeatureCollection pointCollection) throws IOException {
    PointFeatureIterator iter = pointCollection.getPointFeatureIterator(-1);
    //iter.setCalculateBounds(pointCollection);
    List<PointFeature> obsList = new ArrayList<>();
    int count = 0;
    try {
      while (iter.hasNext() && (count++ < maxCount))
        obsList.add(iter.next());
    } finally {
      iter.finish();
    }
    setObservations(obsList);
    return obsList.size();
  }

  private void setStnProfileObservations(NestedPointFeatureCollection nestedPointCollection) throws IOException {
    PointFeatureCollectionIterator iter = nestedPointCollection.getPointFeatureCollectionIterator(-1); // not multiple
    List<PointFeatureCollection> pfcList = new ArrayList<>();
    while (iter.hasNext()) {
      pfcList.add(iter.next());
    }
    setStnProfiles(pfcList);
  }

  private void setSectionObservations(SectionFeature sectionFeature) throws IOException {
    PointFeatureCollectionIterator iter = sectionFeature.getPointFeatureCollectionIterator(-1); // not multiple
    List<PointFeatureCollection> pfcList = new ArrayList<>();
    while (iter.hasNext()) {
      pfcList.add(iter.next());
    }
    setSectionProfiles(pfcList);
  }

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

  public PreferencesExt getPrefs() {
    return prefs;
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////

  static public class FeatureCollectionBean {
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

  static public class FeatureBean {
    StructureData sdata;
    String fields;

    public FeatureBean() {
    }

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

  static public class StationBean extends FeatureBean implements ucar.unidata.geoloc.Station {
    private Station s;
    private int npts = -1;

    public StationBean() {
    }

    public StationBean(StructureData sdata) throws IOException {
      super(sdata);
    }

    public StationBean(Station s) throws IOException {
      super(((StationFeature) s).getFeatureData());
      this.s = s;
      this.npts = s.getNobs();
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

    // for BeanTable
    static public String hiddenProperties() {
      return "latLon description wmoId";
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

  public static class TrajectoryFeatureBean extends StationBean {
    int npts;
    TrajectoryFeature pfc;
    PointFeature pf;

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
    static public String hiddenProperties() {
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

  static public class ProfileFeatureBean extends StationBean {
    int npts;
    ProfileFeature pfc;
    PointFeature pf;

    public ProfileFeatureBean(ProfileFeature pfc) throws IOException {
      super(pfc.getFeatureData());
      this.pfc = pfc;
      try {
        pfc.calcBounds();
        pfc.resetIteration();
        if (pfc.hasNext())
          pf = pfc.next();
      } catch (IOException ioe) {
        log.warn("Profile empty ", ioe);
      }
      pfc.finish();
      npts = pfc.size();
    }

    // for BeanTable
    static public String hiddenProperties() {
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

    public String getDescription() {
      return null;
    }

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

  static public class StnProfileFeatureBean extends FeatureBean {
    ProfileFeature pfc;
    int npts;

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

    public Date getDate() {
      return pfc.getTime();
    }
  }

  public static class SectionFeatureBean extends StationBean {
    int npts;
    SectionFeature pfc;
    ProfileFeature pf;

    public SectionFeatureBean(SectionFeature pfc) throws IOException {
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
    static public String hiddenProperties() {
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
