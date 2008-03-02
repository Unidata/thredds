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
import ucar.nc2.VariableSimpleIF;
import ucar.unidata.geoloc.LatLonRect;

import java.util.List;
import java.io.IOException;

/**
 * Abstract superclass for implementations of StationFeatureCollection.
 *
 * @author caron
 * @since Feb 5, 2008
 */
public class StationFeatureCollectionImpl extends PointFeatureCollectionImpl implements StationFeatureCollection {
  protected StationHelper stationHelper;
  private DateRange filter_date;
  private boolean isStationSubset = false;

  public StationFeatureCollectionImpl(List<VariableSimpleIF> dataVariables, FeatureIterator fiter) {
    super(PointFeature.class, dataVariables);
    stationHelper = new StationHelper();

    PointFeatureIteratorAdapter pfiter = new PointFeatureIteratorAdapter(fiter);
    setIterators(fiter, pfiter);
  }

  protected StationFeatureCollectionImpl(StationFeatureCollectionImpl from, List<Station> stations, DateRange dateRange) {
    super(from);
    if (stations == null) {
      stationHelper = from.stationHelper;
      isStationSubset = from.isStationSubset;
    } else {
      stationHelper = new StationHelper();
      stationHelper.setStations(stations);
      isStationSubset = true;
    }

    if (from.filter_date == null)
      this.filter_date = dateRange;
    else
      this.filter_date = (dateRange == null) ? from.filter_date : from.filter_date.intersect(dateRange);

    FeatureIteratorAdapter ffiter = new FeatureIteratorAdapter(fiter);
    PointFeatureIteratorAdapter fpfiter = new PointFeatureIteratorAdapter(ffiter);
    setIterators(ffiter, fpfiter);
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

  public Class getFeatureClass() {
    return StationFeature.class;
  }

  public PointFeatureCollection subset(LatLonRect boundingBox, DateRange dateRange) throws IOException {
    List<Station> stations = (boundingBox == null) ? null : getStations(boundingBox);
    return new StationFeatureCollectionImpl(this, stations, dateRange);
  }

  public StationFeatureCollection subset(List<Station> stations) throws IOException {
    return new StationFeatureCollectionImpl(this, stations, null);
  }

  public StationFeature getStationFeature(Station s) throws IOException {
    return (StationFeature) s;  // LOOK
  }

  public StationFeature getStationFeature(Station s, DateRange dateRange) throws IOException {
    return (StationFeature) s;  // LOOK
  }

  private class FeatureIteratorAdapter implements FeatureIterator {
    private FeatureIterator fiter;
    private StationFeature sfeature;
    private int bufferSize = -1;

    FeatureIteratorAdapter(FeatureIterator fiter) {
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
    private FeatureIterator fiter;
    public PointFeatureIterator pfiter;
    private PointFeature pfeature;
    private boolean done = false;
    private int bufferSize = -1;

    PointFeatureIteratorAdapter(FeatureIterator fiter) {
      this.fiter = fiter;
    }

    // dont start iteration in the constructor
    private void init() throws IOException {
      if (!fiter.hasNext()) {
        done = true;
        return;
      }
      StationFeature sfeature = (StationFeature) fiter.nextFeature();
      pfiter = sfeature.getPointIterator(bufferSize);
    }

    public boolean hasNext() throws IOException {
      if (pfiter == null) init();

      if (done) return false;
      pfeature = nextFilteredPointFeature();
      if (pfeature != null) return true;

      if (!fiter.hasNext()) return false;
      StationFeature sfeature = (StationFeature) fiter.nextFeature();
      pfiter = sfeature.getPointIterator(bufferSize);
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
  }
}
