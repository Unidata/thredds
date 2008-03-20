/*
 * Copyright 1997-2008 Unidata Program Center/University Corporation for
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
package ucar.nc2.dt2.point;

import ucar.nc2.dt2.*;
import ucar.unidata.geoloc.*;

import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.io.IOException;

/**
 * Helper class for Station Collections.
 * This assumes that calling getData( Station s) is reletively cheap, ie that theres no cheaper filtering to do.
 * @author caron
 * @since Feb 5, 2008
 */
public class StationHelper {
  private List<Station> stations;
  private Map<String, Station> stationHash;
  private boolean debug = false;

  public StationHelper() {
    stations = new ArrayList<Station>();
    stationHash = new HashMap<String, Station>();
  }

  public void addStation( Station s) {
    stations.add(s);
    stationHash.put(s.getName(), s);
  }

  public void setStations( List<Station> nstations) {
    stations = new ArrayList<Station>();
    stationHash = new HashMap<String, Station>();
    for (Station s : nstations)
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
      rect = new LatLonRect(llpt, .001, .001);
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

    return rect;
  }

  public List<Station> getStations(LatLonRect boundingBox) throws IOException {
    LatLonPointImpl latlonPt = new LatLonPointImpl();
    List<Station> result = new ArrayList<Station>();
    for (Station s : stations) {
      latlonPt.set(s.getLatitude(), s.getLongitude());
      if (boundingBox.contains(latlonPt))
        result.add(s);
    }
    return result;
  }

  public Station getStation(String name) {
    return stationHash.get( name);
  }

  public List<Station> getStations() {
    return stations;
  }

}

