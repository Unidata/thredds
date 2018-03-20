/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.dt.point;

import ucar.nc2.util.CancelTask;
import ucar.nc2.dt.*;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.constants.FeatureType;
import ucar.unidata.geoloc.LatLonRect;

import java.io.*;
import java.util.*;

/**
 * Superclass for implementations of StationObsDataset.
 *
 * Subclass needs to:
 * <li> fill the station array
 * <li> implement getData( Station s, CancelTask)
 *
 * @deprecated use ucar.nc2.ft.point
 * @author caron
 */

public abstract class StationObsDatasetImpl extends PointObsDatasetImpl implements StationObsDataset {
  protected StationDatasetHelper stationHelper;
  protected List<ucar.unidata.geoloc.Station> stations = new ArrayList<ucar.unidata.geoloc.Station>();

  public StationObsDatasetImpl() {
    super();
    stationHelper = new StationDatasetHelper( this);
  }

  public StationObsDatasetImpl(String title, String description, String location) {
    super(title, description, location);
    stationHelper = new StationDatasetHelper( this);
  }

  public StationObsDatasetImpl(NetcdfDataset ncfile) {
    super( ncfile);
    stationHelper = new StationDatasetHelper( this);
  }

  public String getDetailInfo() {
    StringBuffer sbuff = new StringBuffer();
    sbuff.append("StationObsDataset\n");
    sbuff.append(super.getDetailInfo());

    return sbuff.toString();
  }

  public FeatureType getScientificDataType() {
    return FeatureType.STATION;
  }

  public Class getDataClass() { return StationObsDatatype.class; }

  public List<ucar.unidata.geoloc.Station> getStations() throws IOException {
    return getStations( (CancelTask) null);
  }
  public List<ucar.unidata.geoloc.Station> getStations(CancelTask cancel)  throws IOException {
    return stations;
  }

  public List<ucar.unidata.geoloc.Station> getStations(LatLonRect boundingBox) throws IOException {
    return getStations(boundingBox, null);
  }

  public List<ucar.unidata.geoloc.Station> getStations(LatLonRect boundingBox, CancelTask cancel) throws IOException {
    return stationHelper.getStations(boundingBox, cancel);
  }

  public ucar.unidata.geoloc.Station getStation(String id) {
    return stationHelper.getStation(id);
  }

  public int getStationDataCount(ucar.unidata.geoloc.Station s) {
    StationImpl si = (StationImpl) s;
    return si.getNumObservations();
  }

  public List getData(ucar.unidata.geoloc.Station s) throws IOException {
    return getData(s, null);
  }

  public List getData(ucar.unidata.geoloc.Station s, Date start, Date end) throws IOException {
    return getData(s, start, end, null);
  }

  public List getData(ucar.unidata.geoloc.Station s, Date start, Date end, CancelTask cancel) throws IOException {
    double startTime = timeUnit.makeValue( start);
    double endTime = timeUnit.makeValue( end);
    return stationHelper.getStationObs(s, startTime, endTime, cancel);
  }

  public List getData(List<ucar.unidata.geoloc.Station> stations) throws IOException {
    return getData(stations, null);
  }

  public List getData(List<ucar.unidata.geoloc.Station> stations, CancelTask cancel) throws IOException {
    return stationHelper.getStationObs(stations, cancel);
  }

  public List getData(List<ucar.unidata.geoloc.Station> stations, Date start, Date end) throws IOException {
    return getData(stations, start, end, null);
  }

  public List getData(List<ucar.unidata.geoloc.Station> stations, Date start, Date end, CancelTask cancel) throws IOException {
    double startTime = timeUnit.makeValue( start);
    double endTime = timeUnit.makeValue( end);
    return stationHelper.getStationObs(stations, startTime, endTime, cancel);
  }

  public List getData(LatLonRect boundingBox, CancelTask cancel) throws IOException {
    return stationHelper.getStationObs(boundingBox, cancel);
  }

  public List getData(LatLonRect boundingBox, Date start, Date end, CancelTask cancel) throws IOException {
    double startTime = timeUnit.makeValue( start);
    double endTime = timeUnit.makeValue( end);
    return stationHelper.getStationObs(boundingBox, startTime, endTime, cancel);
  }

  public void sortByTime(List<StationObsDatatype> stationObs) {
    stationHelper.sortByTime(stationObs);
  }

  public DataIterator getDataIterator( ucar.unidata.geoloc.Station s) {
    try {
      return new DataIteratorAdapter( getData(s).iterator());
    } catch (IOException e) {
      e.printStackTrace();
      return null;
    }
  }

  /** Get data for this Station within the specified date range.
   * @return Iterator over type getDataClass() */
  public DataIterator getDataIterator( ucar.unidata.geoloc.Station s, Date start, Date end) {
    try {
      return new DataIteratorAdapter( getData(s, start, end).iterator());
    } catch (IOException e) {
      e.printStackTrace();
      return null;
    }
  }

}