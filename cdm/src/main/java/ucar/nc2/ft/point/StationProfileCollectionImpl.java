/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.ft.point;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import ucar.nc2.constants.FeatureType;
import ucar.nc2.ft.PointFeatureCC;
import ucar.nc2.ft.PointFeatureCCIterator;
import ucar.nc2.ft.StationProfileFeature;
import ucar.nc2.ft.StationProfileFeatureCollection;
import ucar.nc2.time.CalendarDateRange;
import ucar.nc2.time.CalendarDateUnit;
import ucar.nc2.util.IOIterator;
import ucar.unidata.geoloc.LatLonRect;
import ucar.unidata.geoloc.Station;

/**
 * Abstract superclass for StationProfileFeatureCollection
 * Subclasses must implement getNestedPointFeatureCollection
 * @author caron
 * @since Mar 20, 2008
 */
public abstract class StationProfileCollectionImpl extends PointFeatureCCCImpl implements StationProfileFeatureCollection {
  private volatile StationHelper stationHelper;

  public StationProfileCollectionImpl(String name, CalendarDateUnit timeUnit, String altUnits) {
    super( name, timeUnit, altUnits, FeatureType.STATION_PROFILE);
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

  @Override
  public LatLonRect getBoundingBox() {
    return getStationHelper().getBoundingBox();
  }

  @Override
  public List<StationFeature> getStationFeatures() throws IOException {
    return getStationHelper().getStationFeatures();
  }

  @Override
  public List<StationFeature> getStationFeatures(List<String> stnNames) {
    return getStationHelper().getStationFeaturesFromNames(stnNames);
  }

  @Override
  public List<StationFeature> getStationFeatures(ucar.unidata.geoloc.LatLonRect boundingBox) throws IOException {
    return getStationHelper().getStationFeatures(boundingBox);
  }

  @Override
  public StationFeature findStationFeature(String name) {
    return getStationHelper().getStation(name);
  }

  @Override
  public StationProfileFeature getStationProfileFeature(StationFeature s) throws IOException {
    return (StationProfileFeature) s; // LOOK
  }

  @Override
  public StationProfileCollectionImpl subset(List<StationFeature> stations) throws IOException {
    if (stations == null) return this;
    return new StationProfileFeatureCollectionSubset(this, stations);
  }

  @Override
  public StationProfileCollectionImpl subset(ucar.unidata.geoloc.LatLonRect boundingBox) throws IOException {
    return subset(getStationFeatures(boundingBox));
  }

  @Override
  public StationProfileFeatureCollection subset(LatLonRect boundingBox, CalendarDateRange dateRange) throws IOException {
    return subset(getStationFeatures(boundingBox), dateRange);
  }

  @Override
  public StationProfileCollectionImpl subset(List<StationFeature> stnsWanted, CalendarDateRange dateRange) throws IOException {
    if (dateRange == null)
      return subset(stnsWanted);

    List<StationFeature> subsetStations = new ArrayList<>();
    for (StationFeature sf : stnsWanted) {
      StationProfileFeature stsf = (StationProfileFeature) sf; // LOOK
      StationProfileFeature subset = stsf.subset(dateRange);
      subsetStations.add(subset);
    }

    return new StationProfileFeatureCollectionSubset(this, subsetStations);
  }

  public int compareTo(Station so) {
    return name.compareTo( so.getName());
  }

  // LOOK subset by filtering on the stations, but it would be easier if we could get the StationFeature from the Station
  private static class StationProfileFeatureCollectionSubset extends StationProfileCollectionImpl {
    private final StationProfileCollectionImpl from;
    private final List<StationFeature> stations;

    StationProfileFeatureCollectionSubset(StationProfileCollectionImpl from, List<StationFeature> stations) throws IOException {
      super( from.getName(), from.getTimeUnit(), from.getAltUnits());
      this.from = from;
      this.stations = stations;
    }

    @Override
    protected StationHelper createStationHelper() throws IOException {
      return from.getStationHelper().subset(stations);
    }

    @Override
    public PointFeatureCCIterator getNestedPointFeatureCollectionIterator() throws IOException {
      return new PointFeatureCCIteratorFiltered( from.getNestedPointFeatureCollectionIterator(), new Filter());
    }

    @Override
    public IOIterator<PointFeatureCC> getCollectionIterator() throws IOException {
      return new NestedCollectionIOIteratorAdapter<>(getNestedPointFeatureCollectionIterator());
    }

    private class Filter implements PointFeatureCCIterator.Filter {
      @Override
      public boolean filter(PointFeatureCC pointFeatureCollection) {
        StationProfileFeature stationFeature = (StationProfileFeature) pointFeatureCollection;
        return getStationHelper().getStation(stationFeature.getName()) != null;
      }
    }
  }

  // LOOK make into top-level; how come section didnt need this?
  public class NestedCollectionIOIteratorAdapter<T> implements IOIterator<T> {
    PointFeatureCCIterator pfIterator;

    public NestedCollectionIOIteratorAdapter(PointFeatureCCIterator pfIterator) {
      this.pfIterator = pfIterator;
    }

    @Override
    public boolean hasNext() {
      try {
        return pfIterator.hasNext();
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }

    @Override
    public T next() {
      try {
        return (T) pfIterator.next();
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
  }

  /////////////////////////////////////////////////////////////////////////////////////

  @Override
  public Iterator<StationProfileFeature> iterator() {
    try {
      PointFeatureCCIterator pfIterator = getNestedPointFeatureCollectionIterator();
      return new NestedCollectionIteratorAdapter<>(pfIterator);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  /////////////////////////////////////////////////////////////////////////////////////
  // deprecated
  protected PointFeatureCCIterator localIterator;

  @Override
  public boolean hasNext() throws IOException {
    if (localIterator == null) resetIteration();
    return localIterator.hasNext();
  }

  @Override
  public StationProfileFeature next() throws IOException {
    return (StationProfileFeature) localIterator.next();
  }

  @Override
  public void resetIteration() throws IOException {
    localIterator = getNestedPointFeatureCollectionIterator();
  }

}
