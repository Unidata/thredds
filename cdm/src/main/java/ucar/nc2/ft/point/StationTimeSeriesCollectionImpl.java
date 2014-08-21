/*
 * Copyright 1998-2014 University Corporation for Atmospheric Research/Unidata
 *
 *   Portions of this software were developed by the Unidata Program at the
 *   University Corporation for Atmospheric Research.
 *
 *   Access and use of this software shall impose the following obligations
 *   and understandings on the user. The user is granted the right, without
 *   any fee or cost, to use, copy, modify, alter, enhance and distribute
 *   this software, and any derivative works thereof, and its supporting
 *   documentation for any purpose whatsoever, provided that this entire
 *   notice appears in all copies of the software, derivative works and
 *   supporting documentation.  Further, UCAR requests that the user credit
 *   UCAR/Unidata in any publications that result from the use of this
 *   software or in any product that includes this software. The names UCAR
 *   and/or Unidata, however, may not be used in any advertising or publicity
 *   to endorse or promote any products or commercial entity unless specific
 *   written permission is obtained from UCAR/Unidata. The user also
 *   understands that UCAR/Unidata is not obligated to provide the user with
 *   any support, consulting, training or assistance of any kind with regard
 *   to the use, operation and performance of this software nor to provide
 *   the user with any updates, revisions, new versions or "bug fixes."
 *
 *   THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 *   IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *   WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *   DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 *   INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 *   FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 *   NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 *   WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */
package ucar.nc2.ft.point;

import ucar.nc2.ft.*;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.time.CalendarDateRange;
import ucar.nc2.units.DateRange;
import ucar.nc2.VariableSimpleIF;
import ucar.nc2.units.DateUnit;
import ucar.unidata.geoloc.LatLonRect;
import ucar.unidata.geoloc.Station;

import java.util.List;
import java.util.Iterator;
import java.io.IOException;

/**
 * Abstract superclass for implementations of StationFeatureCollection.
 * Subclass must supply createStationHelper, may need to override getPointFeatureCollectionIterator().
 *
 * @author caron
 * @since Feb 5, 2008
 */
public abstract class StationTimeSeriesCollectionImpl extends OneNestedPointCollectionImpl implements StationTimeSeriesFeatureCollection {
  private volatile StationHelper stationHelper;
  protected PointFeatureCollectionIterator localIterator;

  public StationTimeSeriesCollectionImpl(String name, DateUnit timeUnit, String altUnits) {
    super(name, timeUnit, altUnits, FeatureType.STATION);
  }

  // Double-check idiom for lazy initialization of instance fields. See Effective Java 2nd Ed, p. 283.
  protected StationHelper getStationHelper() {
    if (stationHelper == null) {
      synchronized (this) {
        if (stationHelper == null) {
          try {
            stationHelper = createStationHelper();
          } catch (IOException e) {
            // The methods that will call getStationHelper() aren't declared to throw IOException, so we must
            // wrap it in an unchecked exception.
            throw new RuntimeException(e);
          }
        }
      }
    }

    assert stationHelper != null : "We screwed this up.";
    return stationHelper;
  }

  // Allow StationHelper to be lazily initialized.
  protected abstract StationHelper createStationHelper() throws IOException;

  // note this assumes that a Station is-a PointFeatureCollection
  // subclasses must override if thats not true
  // note that subset() may have made a subset of stationHelper
  public PointFeatureCollectionIterator getPointFeatureCollectionIterator(int bufferSize) throws IOException {
    return new PointFeatureCollectionIterator() {  // an anonymous class iterating over the stations
      Iterator<StationFeature> stationIter = getStationHelper().getStationFeatures().iterator();

      public boolean hasNext() throws IOException {
        return stationIter.hasNext();
      }

      public PointFeatureCollection next() throws IOException {
        return (StationTimeSeriesFeature) stationIter.next();
      }

      public void setBufferSize(int bytes) {
      }

      public void finish() {
      }

    };
  }

  // note this assumes that a Station is-a StationTimeSeriesFeature
  public StationTimeSeriesFeature getStationFeature(Station s) throws IOException {
    return (StationTimeSeriesFeature) getStationHelper().getStationFeature(s);  // subclasses nust override if not true
  }

  // note this assumes that a PointFeature is-a StationPointFeature
  public Station getStation(PointFeature feature) throws IOException {
    StationPointFeature stationFeature = (StationPointFeature) feature;
    return stationFeature.getStation();
  }

  @Override
  public List<StationFeature> getStationFeatures() throws IOException {
    return getStationHelper().getStationFeatures();
  }

  public List<StationFeature> getStationFeatures( List<String> stnNames) {
    return getStationHelper().getStationFeaturesFromNames(stnNames);
  }

  public List<StationFeature> getStationFeatures( ucar.unidata.geoloc.LatLonRect boundingBox) throws IOException {
    return getStationHelper().getStationFeatures(boundingBox);
  }

  // might want to preserve the bb instead of the station list
  public StationTimeSeriesFeatureCollection subset(ucar.unidata.geoloc.LatLonRect boundingBox) throws IOException {
    return subset( getStations(boundingBox));
  }

  // might need to override for efficiency
  public StationTimeSeriesFeatureCollection subset(List<Station> stations) throws IOException {
    if (stations == null) return this;

    List<StationFeature> stationsFeatures = getStationHelper().getStationFeatures(stations);
    return new StationTimeSeriesCollectionSubset(this, stationsFeatures);
  }

  public StationTimeSeriesFeatureCollection subsetFeatures(List<StationFeature> stationsFeatures) throws IOException {
    return new StationTimeSeriesCollectionSubset(this, stationsFeatures);
  }

  // might need to override for efficiency
  public PointFeatureCollection flatten(List<String> stationNames, CalendarDateRange dateRange, List<VariableSimpleIF> varList) throws IOException {
    if ((stationNames == null) || (stationNames.size() == 0))
      return new StationTimeSeriesCollectionFlattened(this, dateRange);

    List<StationFeature> subsetStations = getStationHelper().getStationFeaturesFromNames(stationNames);
    return new StationTimeSeriesCollectionFlattened(new StationTimeSeriesCollectionSubset(this, subsetStations), dateRange);
  }

  public PointFeatureCollection flatten(LatLonRect boundingBox, CalendarDateRange dateRange) throws IOException {
    if (boundingBox == null)
      return new StationTimeSeriesCollectionFlattened(this, dateRange);

    List<StationFeature> subsetStations = getStationHelper().getStationFeatures(boundingBox);
    return new StationTimeSeriesCollectionFlattened(new StationTimeSeriesCollectionSubset(this, subsetStations), dateRange);
  }

  public PointFeatureCollection flatten(List<String> stations, DateRange dateRange, List<VariableSimpleIF> varList) throws IOException {
    return flatten(stations, CalendarDateRange.of(dateRange), varList);
  }

  private static class StationTimeSeriesCollectionSubset extends StationTimeSeriesCollectionImpl {
    private final List<StationFeature> stations;

    StationTimeSeriesCollectionSubset(StationTimeSeriesCollectionImpl from, List<StationFeature> stations) {
      super(from.getName(), from.getTimeUnit(), from.getAltUnits());
      this.stations = stations;
    }

    @Override
    protected StationHelper createStationHelper() throws IOException {
      StationHelper stationHelper = new StationHelper();
      stationHelper.setStations(stations);
      return stationHelper;
    }

    @Override
    public List<StationFeature> getStationFeatures() throws IOException {
      return getStationHelper().getStationFeatures();
    }

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

  @Override
  public List<Station> getStations() {
    return getStationHelper().getStations();
  }

  @Override
  public List<Station> getStations(List<String> stnNames) {
    return getStationHelper().getStations(stnNames);
  }

  @Override
  public List<Station> getStations(LatLonRect boundingBox) throws IOException {
    return getStationHelper().getStations(boundingBox);
  }

  @Override
  public Station getStation(String name) {
    return getStationHelper().getStation(name);
  }

  @Override
  public LatLonRect getBoundingBox() {
    return getStationHelper().getBoundingBox();
  }

  @Override
  public NestedPointFeatureCollectionIterator getNestedPointFeatureCollectionIterator(int bufferSize) throws IOException {
    throw new UnsupportedOperationException("StationFeatureCollection does not implement getNestedPointFeatureCollection()");
  }

  @Override
  public boolean hasNext() throws IOException {
    if (localIterator == null) resetIteration();
    return localIterator.hasNext();
  }

  @Override
  public void finish() {
    if (localIterator != null)
      localIterator.finish();
  }

  @Override
 public StationTimeSeriesFeature next() throws IOException {
    return (StationTimeSeriesFeature) localIterator.next();
  }

  @Override
  public void resetIteration() throws IOException {
    localIterator = getPointFeatureCollectionIterator(-1);
  }
}
