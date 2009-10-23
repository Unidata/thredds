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
package ucar.nc2.ft.point;

import ucar.nc2.ft.*;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.units.DateRange;
import ucar.nc2.VariableSimpleIF;
import ucar.unidata.geoloc.LatLonRect;
import ucar.unidata.geoloc.Station;

import java.util.List;
import java.util.Iterator;
import java.io.IOException;

/**
 * Abstract superclass for implementations of StationFeatureCollection.
 * Subclass must supply initStationHelper, may need to override getPointFeatureCollectionIterator().
 *
 * @author caron
 * @since Feb 5, 2008
 */
public abstract class StationTimeSeriesCollectionImpl extends OneNestedPointCollectionImpl implements StationTimeSeriesFeatureCollection {

  protected StationHelper stationHelper;
  protected PointFeatureCollectionIterator localIterator;

  public StationTimeSeriesCollectionImpl(String name) {
    super(name, FeatureType.STATION);
  }

  protected abstract void initStationHelper(); // allow station helper to be deffered initialization

  // note this assumes that a Station is-a PointFeatureCollection
  // subclasses must override if thats not true
  // note that subset() may have made a subset of stationHelper
  public PointFeatureCollectionIterator getPointFeatureCollectionIterator(int bufferSize) throws IOException {
    initStationHelper();

    return new PointFeatureCollectionIterator() {  // an anonymous class iterating over the stations
      Iterator<Station> stationIter = stationHelper.getStations().iterator();

      public boolean hasNext() throws IOException {
        return stationIter.hasNext();
      }

      public PointFeatureCollection next() throws IOException {
        return (PointFeatureCollection) stationIter.next();
      }

      public void setBufferSize(int bytes) {
      }

      public void finish() {
      }

    };
  }

  // note this assumes that a Station is-a StationTimeSeriesFeature
  public StationTimeSeriesFeature getStationFeature(Station s) throws IOException {
    return (StationTimeSeriesFeature) s;  // subclasses nust override if not true
  }

  // note this assumes that a PointFeature is-a StationPointFeature
  public Station getStation(PointFeature feature) throws IOException {
    StationPointFeature stationFeature = (StationPointFeature) feature;
    return stationFeature.getStation();
  }

  // might want to preserve the bb instead of the station list
  public StationTimeSeriesFeatureCollection subset(ucar.unidata.geoloc.LatLonRect boundingBox) throws IOException {
    return subset( getStations(boundingBox));
  }

  // might need to override for efficiency
  public StationTimeSeriesFeatureCollection subset(List<Station> stations) throws IOException {
    if (stations == null) return this;
    return new StationTimeSeriesCollectionSubset(this, stations);
  }

  // might need to override for efficiency
  public PointFeatureCollection flatten(List<String> stations, DateRange dateRange, List<VariableSimpleIF> varList) throws IOException {
    if ((stations == null) || (stations.size() == 0))
      return new StationTimeSeriesCollectionFlattened(this, dateRange);
    initStationHelper();
    List<Station> subsetStations = stationHelper.getStations(stations);
    return new StationTimeSeriesCollectionFlattened(new StationTimeSeriesCollectionSubset(this, subsetStations), dateRange);
  }

  public PointFeatureCollection flatten(LatLonRect boundingBox, DateRange dateRange) throws IOException {
    if (boundingBox == null)
      return new StationTimeSeriesCollectionFlattened(this, dateRange);
    initStationHelper();
    List<Station> subsetStations = stationHelper.getStations(boundingBox);
    return new StationTimeSeriesCollectionFlattened(new StationTimeSeriesCollectionSubset(this, subsetStations), dateRange);
  }

  private class StationTimeSeriesCollectionSubset extends StationTimeSeriesCollectionImpl {
    StationTimeSeriesCollectionImpl from; // probably not needed

    StationTimeSeriesCollectionSubset(StationTimeSeriesCollectionImpl from, List<Station> stations) {
      super(from.getName());
      this.from = from;
      this.stationHelper = new StationHelper();
      this.stationHelper.setStations(stations);
    }

    protected  void initStationHelper() {}

    /* dont think this is needed
    public PointFeatureCollectionIterator getPointFeatureCollectionIterator(int bufferSize) throws IOException {
      return new PointCollectionIteratorFiltered(from.getPointFeatureCollectionIterator(bufferSize), new Filter());
    }

    public Station getStation(PointFeature feature) throws IOException {
      return from.getStation(feature);
    }

    // LOOK: major ick! FIX THIS
    private class Filter implements PointFeatureCollectionIterator.Filter {

      public boolean filter(PointFeatureCollection pointFeatureCollection) {
        StationTimeSeriesFeature stationFeature = (StationTimeSeriesFeature) pointFeatureCollection;
        return stationHelper.getStation(stationFeature.getName()) != null;
      }
    } */
  }

  //////////////////////////
  // boilerplate

  public List<Station> getStations() {
    if (stationHelper == null) initStationHelper();
    return stationHelper.getStations();
  }

  public List<Station> getStations(List<String> stnNames) {
    if (stationHelper == null) initStationHelper();
    return stationHelper.getStations(stnNames);
  }

  public List<Station> getStations(LatLonRect boundingBox) throws IOException {
    if (stationHelper == null) initStationHelper();
    return stationHelper.getStations(boundingBox);
  }

  public Station getStation(String name) {
    if (stationHelper == null) initStationHelper();
    return stationHelper.getStation(name);
  }

  public LatLonRect getBoundingBox() {
    if (stationHelper == null) initStationHelper();
    return stationHelper.getBoundingBox();
  }

  public NestedPointFeatureCollectionIterator getNestedPointFeatureCollectionIterator(int bufferSize) throws IOException {
    throw new UnsupportedOperationException("StationFeatureCollection does not implement getNestedPointFeatureCollection()");
  }

  public boolean hasNext() throws IOException {
    if (localIterator == null) resetIteration();
    return localIterator.hasNext();
  }

  public void finish() {
    if (localIterator != null)
      localIterator.finish();
  }

  public StationTimeSeriesFeature next() throws IOException {
    return (StationTimeSeriesFeature) localIterator.next();
  }

  public void resetIteration() throws IOException {
    localIterator = getPointFeatureCollectionIterator(-1);
  }

}
