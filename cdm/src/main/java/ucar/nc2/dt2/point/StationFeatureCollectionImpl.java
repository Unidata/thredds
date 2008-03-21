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
import ucar.nc2.units.DateRange;
import ucar.unidata.geoloc.LatLonRect;

import java.util.List;
import java.io.IOException;

/**
 * Abstract superclass for implementations of StationFeatureCollection.
 * Subclass must supply getPointFeatureCollectionIterator().
 *
 * @author caron
 * @since Feb 5, 2008
 */
public abstract class StationFeatureCollectionImpl extends NestedPointFeatureCollectionImpl implements StationFeatureCollection {

  protected StationHelper stationHelper;

  public StationFeatureCollectionImpl() {
    super(false, StationFeature.class);
    stationHelper = new StationHelper();
  }

  protected void setStationHelper(StationHelper stationHelper) {
    this.stationHelper = stationHelper;
  }

  public List<Station> getStations() {
    return stationHelper.getStations();
  }

  public List<Station> getStations(LatLonRect boundingBox) throws IOException {
    return stationHelper.getStations(boundingBox);
  }

  public Station getStation(String name) {
    return stationHelper.getStation(name);
  }

  public LatLonRect getBoundingBox() {
    return stationHelper.getBoundingBox();
  }

  public StationFeatureCollection subset(List<Station> stations) throws IOException {
    if (stations == null) return this;
    return new StationFeatureCollectionSubset(this, stations);
  }

  public StationFeature getStationFeature(Station s) throws IOException {
    return (StationFeature) s;  // LOOK
  }

  public StationFeature getStationFeature(Station s, DateRange dateRange) throws IOException {
    return (StationFeature) s;  // LOOK
  }

  public NestedPointFeatureCollectionIterator getNestedPointFeatureCollectionIterator(int bufferSize) throws IOException {
    throw new UnsupportedOperationException("StationFeatureCollection does not implement getNestedPointFeatureCollection()");
  }

  // LOOK subset by filtering on the stations, but it would be easier if we could get the StationFeature from the Station
  private class StationFeatureCollectionSubset extends StationFeatureCollectionImpl {
    StationFeatureCollectionImpl from;

    StationFeatureCollectionSubset(StationFeatureCollectionImpl from, List<Station> stations) {
      this.from = from;
      stationHelper = new StationHelper();
      stationHelper.setStations(stations);
    }

    public PointFeatureCollectionIterator getPointFeatureCollectionIterator(int bufferSize) throws IOException {
      return new PointFeatureCollectionIteratorFiltered( from.getPointFeatureCollectionIterator(bufferSize), new Filter());
    }

    private class Filter implements PointFeatureCollectionIterator.Filter {

      public boolean filter(PointFeatureCollection pointFeatureCollection) {
        StationFeature stationFeature = (StationFeature) pointFeatureCollection;
        return stationHelper.getStation(stationFeature.getName()) != null;
      }
    }
  }

  /* private class FeatureIteratorAdapter implements PointFeatureCollectionIterator {
    private PointFeatureCollectionIterator fiter;
    private StationFeature sfeature;
    private int bufferSize = -1;

    FeatureIteratorAdapter(PointFeatureCollectionIterator fiter) {
      this.fiter = fiter;
    }

    public boolean hasNext() throws IOException {
      sfeature = nextFilteredFeature();
      return (sfeature != null);
    }

    public Feature nextFeature() throws IOException {
      return sfeature;
    }

    public void setBufferSize(int bytes) {
      fiter.setBufferSize(bytes);
    }

    private StationFeature nextFilteredFeature() throws IOException {
      if (!fiter.hasNext()) return null;
      StationFeature sfeature = (StationFeature) fiter.nextFeature();

      if (isStationSubset) {
        while (null == stationHelper.getStation(sfeature.getName())) {
          if (!fiter.hasNext()) return null;
          sfeature = (StationFeature) fiter.nextFeature();
        }
      }

      return sfeature;
    }
  }

  private class PointFeatureIteratorAdapter implements PointFeatureIterator {
    private PointFeatureCollectionIterator fiter;
    public PointFeatureIterator pfiter;
    private PointFeature pfeature;
    private boolean done = false;
    private int bufferSize = -1;

    PointFeatureIteratorAdapter(PointFeatureCollectionIterator fiter) {
      this.fiter = fiter;
    }

    // dont start iteration in the constructor
    private void init() throws IOException {
      if (!fiter.hasNext()) {
        done = true;
        return;
      }
      StationFeature sfeature = (StationFeature) fiter.nextFeature();
      pfiter = sfeature.getPointFeatureIterator(bufferSize);
    }

    public boolean hasNext() throws IOException {
      if (pfiter == null) init();

      if (done) return false;
      pfeature = nextFilteredPointFeature();
      if (pfeature != null) return true;

      if (!fiter.hasNext()) return false;
      StationFeature sfeature = (StationFeature) fiter.nextFeature();
      pfiter = sfeature.getPointFeatureIterator(bufferSize);
      pfeature = nextFilteredPointFeature();
      if (pfeature == null) return hasNext();
      return true;
    }

    public PointFeature nextData() throws IOException {
      if (done) return null;
      return pfeature;
    }

    public void setBufferSize(int bytes) {
      fiter.setBufferSize(bytes / 2);
      bufferSize = bytes / 2;
    }

    private PointFeature nextFilteredPointFeature() throws IOException {
      if (!pfiter.hasNext()) return null;
      PointFeature pointFeature = pfiter.nextData();

      if (filter_date != null) {
        while (!filter_date.included(pointFeature.getObservationTimeAsDate())) {
          if (!pfiter.hasNext()) return null;
          pointFeature = pfiter.nextData();
        }
      }

      return pointFeature;
    }
  }  */
}
