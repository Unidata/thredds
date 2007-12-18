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

package ucar.nc2.dt.point;

import ucar.nc2.util.CancelTask;
import ucar.nc2.dt.*;
import ucar.nc2.dataset.NetcdfDataset;
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
 * @author caron
 */

public abstract class StationObsDatasetImpl extends PointObsDatasetImpl implements StationObsDataset {
  protected StationDatasetHelper stationHelper;
  protected List<Station> stations = new ArrayList<Station>();

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

  public thredds.catalog.DataType getScientificDataType() {
    return thredds.catalog.DataType.STATION;
  }

  public Class getDataClass() { return StationObsDatatype.class; }

  public List<Station> getStations() throws IOException {
    return getStations( (CancelTask) null);
  }
  public List<Station> getStations(CancelTask cancel)  throws IOException {
    return stations;
  }

  public List<Station> getStations(LatLonRect boundingBox) throws IOException {
    return getStations(boundingBox, null);
  }

  public List<Station> getStations(LatLonRect boundingBox, CancelTask cancel) throws IOException {
    return stationHelper.getStations(boundingBox, cancel);
  }

  public Station getStation(String id) {
    return stationHelper.getStation(id);
  }

  public int getStationDataCount(Station s) {
    StationImpl si = (StationImpl) s;
    return si.getNumObservations();
  }

  public List getData(Station s) throws IOException {
    return getData(s, null);
  }

  public List getData(Station s, Date start, Date end) throws IOException {
    return getData(s, start, end, null);
  }

  public List getData(Station s, Date start, Date end, CancelTask cancel) throws IOException {
    double startTime = timeUnit.makeValue( start);
    double endTime = timeUnit.makeValue( end);
    return stationHelper.getStationObs(s, startTime, endTime, cancel);
  }

  public List getData(List<Station> stations) throws IOException {
    return getData(stations, null);
  }

  public List getData(List<Station> stations, CancelTask cancel) throws IOException {
    return stationHelper.getStationObs(stations, cancel);
  }

  public List getData(List<Station> stations, Date start, Date end) throws IOException {
    return getData(stations, start, end, null);
  }

  public List getData(List<Station> stations, Date start, Date end, CancelTask cancel) throws IOException {
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

  public DataIterator getDataIterator( Station s) {
    try {
      return new DataIteratorAdapter( getData(s).iterator());
    } catch (IOException e) {
      e.printStackTrace();
      return null;
    }
  }

  /** Get data for this Station within the specified date range.
   * @return Iterator over type getDataClass() */
  public DataIterator getDataIterator( Station s, Date start, Date end) {
    try {
      return new DataIteratorAdapter( getData(s, start, end).iterator());
    } catch (IOException e) {
      e.printStackTrace();
      return null;
    }
  }

}