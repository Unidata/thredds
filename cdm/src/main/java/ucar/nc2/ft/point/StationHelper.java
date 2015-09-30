/*
 * Copyright 1998-2015 John Caron and University Corporation for Atmospheric Research/Unidata
 *
 *  Portions of this software were developed by the Unidata Program at the
 *  University Corporation for Atmospheric Research.
 *
 *  Access and use of this software shall impose the following obligations
 *  and understandings on the user. The user is granted the right, without
 *  any fee or cost, to use, copy, modify, alter, enhance and distribute
 *  this software, and any derivative works thereof, and its supporting
 *  documentation for any purpose whatsoever, provided that this entire
 *  notice appears in all copies of the software, derivative works and
 *  supporting documentation.  Further, UCAR requests that the user credit
 *  UCAR/Unidata in any publications that result from the use of this
 *  software or in any product that includes this software. The names UCAR
 *  and/or Unidata, however, may not be used in any advertising or publicity
 *  to endorse or promote any products or commercial entity unless specific
 *  written permission is obtained from UCAR/Unidata. The user also
 *  understands that UCAR/Unidata is not obligated to provide the user with
 *  any support, consulting, training or assistance of any kind with regard
 *  to the use, operation and performance of this software nor to provide
 *  the user with any updates, revisions, new versions or "bug fixes."
 *
 *  THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 *  IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *  WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *  DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 *  INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 *  FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 *  NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 *  WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */
package ucar.nc2.ft.point;

import ucar.unidata.geoloc.LatLonPointImpl;
import ucar.unidata.geoloc.LatLonRect;
import ucar.unidata.geoloc.Station;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Helper class for Station Collections.
 * This assumes that calling getData( Station s) is cheap, ie that theres no cheaper filtering to do.
 * @author caron
 * @since Feb 5, 2008
 */
public class StationHelper {
  private List<StationFeature> stations;
  private Map<String, StationFeature> stationHash;
  private static final boolean debug = false;

  public StationHelper() {
    stations = new ArrayList<>();
    stationHash = new HashMap<>();
  }

  public void addStation( StationFeature s) {
    stations.add(s);
    stationHash.put(s.getName(), s);
  }

  public void setStations( List<StationFeature> nstations) {
    stations = new ArrayList<>();
    stationHash = new HashMap<>();
    for (StationFeature s : nstations)
      addStation(s);
  }

  private LatLonRect rect;
  public LatLonRect getBoundingBox() {
    if (rect == null) {
      if (stations.size() == 0)
        return null;

      Station s = stations.get(0);
      LatLonPointImpl llpt = new LatLonPointImpl();
      llpt.set( s.getLatitude(), s.getLongitude());
      rect = new LatLonRect(llpt, 0, 0);
      if (debug) System.out.println("start="+s.getLatitude()+" "+s.getLongitude()+" rect= "+rect.toString2());

      for (int i = 1; i < stations.size(); i++) {
        s = stations.get(i);
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

  public List<Station> getStations(LatLonRect boundingBox) throws IOException {
    if (boundingBox == null) return getStations();
    
    LatLonPointImpl latlonPt = new LatLonPointImpl();
    List<Station> result = new ArrayList<>();
    for (StationFeature s : stations) {
      latlonPt.set(s.getLatitude(), s.getLongitude());
      if (boundingBox.contains(latlonPt))
        result.add(s);
    }
    return result;
  }

  public List<StationFeature> getStationFeatures(LatLonRect boundingBox) throws IOException {
    if (boundingBox == null) return stations;

    LatLonPointImpl latlonPt = new LatLonPointImpl();
    List<StationFeature> result = new ArrayList<>();
    for (StationFeature s : stations) {
      latlonPt.set(s.getLatitude(), s.getLongitude());
      if (boundingBox.contains(latlonPt))
        result.add(s);
    }
    return result;
  }

  public StationFeature getStation(String name) {
    return stationHash.get( name);
  }

  public List<StationFeature> getStationFeatures() {
    return stations;
  }

  public List<Station> getStations() {
    List<Station> result = new ArrayList<>(stations.size());
    result.addAll(stations);
    return result;
  }

  public List<StationFeature> getStationFeaturesFromNames( List<String> stnNames) {
    List<StationFeature> result = new ArrayList<>(stnNames.size());
    for (String ss : stnNames) {
      StationFeature s = stationHash.get(ss);
      if (s != null)
        result.add(s);
    }
    return result;
  }

  public List<StationFeature> getStationFeatures( List<Station> stations) {
    List<StationFeature> result = new ArrayList<>(stations.size());
    for (Station s : stations) {
      StationFeature ss = stationHash.get(s.getName());
      if (ss != null)
        result.add(ss);
    }
    return result;
  }

  public StationFeature getStationFeature( Station stn) {
      return stationHash.get(stn.getName());
  }

  public List<Station> getStations( List<String> stnNames) {
    List<Station> result = new ArrayList<>(stnNames.size());
    for (String ss : stnNames) {
      Station s = stationHash.get(ss);
      if (s != null)
        result.add(s);
    }
    return result;
  }

  public StationHelper subset(LatLonRect bb) throws IOException {
    StationHelper result = new StationHelper();
    result.setStations( getStationFeatures(bb));
    return result;
  }

  public StationHelper subset(List<Station> stns) throws IOException {
    StationHelper result = new StationHelper();
    result.setStations( getStationFeatures(stns));
    return result;
  }

}

