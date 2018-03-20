/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.dt.point;

import ucar.nc2.dt.StationObsDataset;
import ucar.nc2.dt.StationObsDatatype;
import ucar.nc2.util.CancelTask;
import ucar.unidata.geoloc.LatLonPointImpl;
import ucar.unidata.geoloc.LatLonRect;

import java.io.IOException;
import java.util.*;

/**
 * Helper class for StationObsDataset.
 * StationObsDataset must implement:
 *   getStations()
 *   getData( Station s)
 *
 * This assumes that calling getData( Station s) is reletively cheap, ie that theres no cheaper filtering to do.
 *
 * @deprecated use ucar.nc2.ft.point
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
      rect = new LatLonRect(llpt, 0, 0);
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

    // To give a little "wiggle room", we're going to slightly expand the bounding box.
    double newLowerLeftLat = rect.getLowerLeftPoint().getLatitude() - .0005;
    double newLowerLeftLon = rect.getLowerLeftPoint().getLongitude() - .0005;
    LatLonPointImpl newLowerLeftPoint = new LatLonPointImpl(newLowerLeftLat, newLowerLeftLon);

    double newUpperRightLat = rect.getUpperRightPoint().getLatitude() + .0005;
    double newUpperRightLon = rect.getUpperRightPoint().getLongitude() + .0005;
    LatLonPointImpl newUpperRightPoint = new LatLonPointImpl(newUpperRightLat, newUpperRightLon);

    rect.extend(newLowerLeftPoint);
    rect.extend(newUpperRightPoint);

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
