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