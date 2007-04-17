package ucar.nc2.dt.point;

import ucar.unidata.geoloc.LatLonRect;
import ucar.unidata.geoloc.LatLonPointImpl;
import ucar.nc2.dt.StationObsDataset;
import ucar.nc2.dt.Station;
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
 * @version $Revision:51 $ $Date:2006-07-12 17:13:13Z $
 */
public class StationDatasetHelper {
  private StationObsDataset obsDataset;
  private HashMap stationHash;

  public StationDatasetHelper( StationObsDataset obsDataset) {
    this.obsDataset = obsDataset;
  }

  private LatLonRect rect;
  public LatLonRect getBoundingBox() {
    if (rect == null) {
      List stations = null;
      try {
        stations = obsDataset.getStations();
      } catch (IOException e) {
        return null;
      }
      if (stations.size() == 0)
        return null;

      Station s =  (Station) stations.get(0);
      LatLonPointImpl llpt = new LatLonPointImpl();
      llpt.set( s.getLatitude(), s.getLongitude());
      rect = new LatLonRect(llpt, .001, .001);
      System.out.println("start="+s.getLatitude()+" "+s.getLongitude()+" rect= "+rect.toString2());

      for (int i = 1; i < stations.size(); i++) {
        s =  (Station) stations.get(i);
        llpt.set( s.getLatitude(), s.getLongitude());
        rect.extend( llpt);
        System.out.println("add="+s.getLatitude()+" "+s.getLongitude()+" rect= "+rect.toString2());
      }
    }
    if (rect.crossDateline() && rect.getWidth() > 350.0) { // call it global - less confusing
      double lat_min = rect.getLowerLeftPoint().getLatitude();
      double deltaLat = rect.getUpperLeftPoint().getLatitude() - lat_min;
      rect = new LatLonRect( new LatLonPointImpl(lat_min, -180.0), deltaLat, 360.0);
    }

    return rect;
  }

  public List getStations(LatLonRect boundingBox, CancelTask cancel) throws IOException {
    LatLonPointImpl latlonPt = new LatLonPointImpl();
    ArrayList result = new ArrayList();
    List stations = obsDataset.getStations();
    for (int i = 0; i < stations.size(); i++) {
      Station s =  (Station) stations.get(i);
      latlonPt.set( s.getLatitude(), s.getLongitude());
      if (boundingBox.contains( latlonPt))
        result.add( s);
      if ((cancel != null) && cancel.isCancel()) return null;
    }
    return result;
  }

  public Station getStation(String name) {
    if (stationHash == null) {
      List stations = null;
      try {
        stations = obsDataset.getStations();
      } catch (IOException e) {
        return null;
      }

      stationHash = new HashMap( 2*stations.size());
      for (int i = 0; i < stations.size(); i++) {
        Station s =  (Station) stations.get(i);
        stationHash.put( s.getName(), s);
      }
    }

    return (Station) stationHash.get( name);
  }

  public List getStationObs(Station s, double startTime, double endTime, CancelTask cancel) throws IOException {
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

  public List getStationObs(List stations, CancelTask cancel) throws IOException {
    ArrayList result = new ArrayList();
    for (int i = 0; i < stations.size(); i++) {
      Station s = (Station) stations.get(i);
      result.addAll( obsDataset.getData( s, cancel));
      if ((cancel != null) && cancel.isCancel()) return null;
    }
    return result;
  }

  public List getStationObs(List stations, double startTime, double endTime, CancelTask cancel) throws IOException {
    ArrayList result = new ArrayList();
    for (int i = 0; i < stations.size(); i++) {
      Station s = (Station) stations.get(i);
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

  public void sortByTime(List stationObs) {
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
