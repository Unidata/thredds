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

import ucar.unidata.geoloc.LatLonRect;
import ucar.unidata.geoloc.LatLonPointImpl;
import ucar.nc2.dt.StationObsDataset;
import ucar.nc2.dt.StationObsDatatype;
import ucar.nc2.util.CancelTask;

import java.util.*;
import java.io.IOException;

/**
 * Helper class for StationObsDataset.
 * StationObsDataset must implement:
 *   getStations()
 *   getData( Station s)
 *
 * This assumes that calling getData( Station s) is reletively cheap, ie that theres no cheaper filtering to do.
 *
 * @author caron
 */
public class StationDatasetHelper {
  private StationObsDataset obsDataset;
  private Map<String,ucar.unidata.geoloc.Station> stationHash;
  private boolean debug = false;

  public StationDatasetHelper( StationObsDataset obsDataset) {
    this.obsDataset = obsDataset;
  }

  private LatLonRect rect;
  public LatLonRect getBoundingBox() {
    if (rect == null) {
      List stations;
      try {
        stations = obsDataset.getStations();
      } catch (IOException e) {
        return null;
      }
      if (stations.size() == 0)
        return null;

      ucar.unidata.geoloc.Station s =  (ucar.unidata.geoloc.Station) stations.get(0);
      LatLonPointImpl llpt = new LatLonPointImpl();
      llpt.set( s.getLatitude(), s.getLongitude());
      rect = new LatLonRect(llpt, .001, .001);
      if (debug) System.out.println("start="+s.getLatitude()+" "+s.getLongitude()+" rect= "+rect.toString2());

      for (int i = 1; i < stations.size(); i++) {
        s =  (ucar.unidata.geoloc.Station) stations.get(i);
        llpt.set( s.getLatitude(), s.getLongitude());
        rect.extend( llpt);
        if (debug) System.out.println("add="+s.getLatitude()+" "+s.getLongitude()+" rect= "+rect.toString2());
      }
    }
    if (rect.crossDateline() && rect.getWidth() > 350.0) { // call it global - less confusing
      double lat_min = rect.getLowerLeftPoint().getLatitude();
      double deltaLat = rect.getUpperLeftPoint().getLatitude() - lat_min;
      rect = new LatLonRect( new LatLonPointImpl(lat_min, -180.0), deltaLat, 360.0);
    }

    return rect;
  }

  public List<ucar.unidata.geoloc.Station> getStations(LatLonRect boundingBox, CancelTask cancel) throws IOException {
    LatLonPointImpl latlonPt = new LatLonPointImpl();
    List<ucar.unidata.geoloc.Station> result = new ArrayList<ucar.unidata.geoloc.Station>();
    List<ucar.unidata.geoloc.Station> stations = obsDataset.getStations();
    for (ucar.unidata.geoloc.Station s : stations) {
      latlonPt.set(s.getLatitude(), s.getLongitude());
      if (boundingBox.contains(latlonPt))
        result.add(s);
      if ((cancel != null) && cancel.isCancel()) return null;
    }
    return result;
  }

  public ucar.unidata.geoloc.Station getStation(String name) {
    if (stationHash == null) {
      List<ucar.unidata.geoloc.Station> stations;
      try {
        stations = obsDataset.getStations();
      } catch (IOException e) {
        return null;
      }

      stationHash = new HashMap<String,ucar.unidata.geoloc.Station>( 2*stations.size());
      for (ucar.unidata.geoloc.Station s : stations) {
        stationHash.put(s.getName(), s);
      }
    }

    return stationHash.get( name);
  }

  public List getStationObs(ucar.unidata.geoloc.Station s, double startTime, double endTime, CancelTask cancel) throws IOException {
    ArrayList result = new ArrayList();
    List stationObs = obsDataset.getData( s, cancel);
    for (int i = 0; i < stationObs.size(); i++) {
      StationObsDatatype obs =  (StationObsDatatype) stationObs.get(i);
      double timeValue = obs.getObservationTime();
      if ((timeValue >= startTime) && (timeValue <= endTime))
          result.add( obs);
      if ((cancel != null) && cancel.isCancel()) return null;
    }
    return result;
  }

  public List getStationObs(List<ucar.unidata.geoloc.Station> stations, CancelTask cancel) throws IOException {
    ArrayList result = new ArrayList();
    for (int i = 0; i < stations.size(); i++) {
      ucar.unidata.geoloc.Station s = stations.get(i);
      result.addAll( obsDataset.getData( s, cancel));
      if ((cancel != null) && cancel.isCancel()) return null;
    }
    return result;
  }

  public List getStationObs(List<ucar.unidata.geoloc.Station> stations, double startTime, double endTime, CancelTask cancel) throws IOException {
    ArrayList result = new ArrayList();
    for (int i = 0; i < stations.size(); i++) {
      ucar.unidata.geoloc.Station s = stations.get(i);
      result.addAll( getStationObs( s, startTime, endTime, cancel));
      if ((cancel != null) && cancel.isCancel()) return null;
    }
    return result;
  }

  public List getStationObs(LatLonRect boundingBox, CancelTask cancel) throws IOException {
    List stations = obsDataset.getStations( boundingBox, cancel);
    if (stations == null) return null;
    return getStationObs( stations, cancel);
  }

  public List getStationObs(LatLonRect boundingBox, double startTime, double endTime, CancelTask cancel) throws IOException {
    List stations = obsDataset.getStations( boundingBox);
    if (stations == null) return null;
    return getStationObs( stations, startTime, endTime, cancel);
  }

  public void sortByTime(List<StationObsDatatype> stationObs) {
    Collections.sort( stationObs, new StationObsComparator());
  }

  private class StationObsComparator implements Comparator {
    public int compare(Object o1, Object o2) {
      StationObsDatatype s1 = (StationObsDatatype) o1;
      StationObsDatatype s2 = (StationObsDatatype) o2;
      return (int) (s1.getObservationTime() - s2.getObservationTime());
    }
  }

}
