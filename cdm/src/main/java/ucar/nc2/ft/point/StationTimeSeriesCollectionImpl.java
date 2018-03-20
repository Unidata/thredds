/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.ft.point;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import ucar.nc2.VariableSimpleIF;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.ft.PointFeature;
import ucar.nc2.ft.PointFeatureCollection;
import ucar.nc2.ft.PointFeatureCollectionIterator;
import ucar.nc2.ft.StationTimeSeriesFeature;
import ucar.nc2.ft.StationTimeSeriesFeatureCollection;
import ucar.nc2.time.CalendarDateRange;
import ucar.nc2.time.CalendarDateUnit;
import ucar.nc2.util.IOIterator;
import ucar.unidata.geoloc.LatLonRect;

/**
 * Abstract superclass for implementations of StationFeatureCollection.
 * Subclass must supply createStationHelper, may need to override getPointFeatureCollectionIterator().
 *
 * @author caron
 * @since Feb 5, 2008
 */
public abstract class StationTimeSeriesCollectionImpl extends PointFeatureCCImpl implements StationTimeSeriesFeatureCollection {
  private volatile StationHelper stationHelper;

  public StationTimeSeriesCollectionImpl(String name, CalendarDateUnit timeUnit, String altUnits) {
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

    assert stationHelper != null : "stationHelper is null for StationTimeSeriesCollectionImpl"+getName();
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
  public StationTimeSeriesFeature getStationTimeSeriesFeature(StationFeature s) {
    return (StationTimeSeriesFeature) s; // LOOK
  }

  ////////////////////////////////////////////////////////////////
  // subset

  @Override
  public StationTimeSeriesFeatureCollection subset(ucar.unidata.geoloc.LatLonRect boundingBox) throws IOException {
    return subset( getStationFeatures(boundingBox));
  }

  @Override
  public StationTimeSeriesFeatureCollection subset(LatLonRect boundingBox, CalendarDateRange dateRange) throws IOException {
    return subset(getStationFeatures(boundingBox), dateRange);
  }

  @Override
  public StationTimeSeriesFeatureCollection subset(List<StationFeature> stations) throws IOException {
    return new StationSubset(this, stations);
  }

  @Override
  public StationTimeSeriesFeatureCollection subset(List<StationFeature> stnsWanted, CalendarDateRange dateRange) throws IOException {
    if (dateRange == null)
      return subset(stnsWanted);

    List<StationFeature> subsetStations = new ArrayList<>();
    for (StationFeature sf : stnsWanted) {
      StationTimeSeriesFeature stsf = (StationTimeSeriesFeature) sf; // LOOK
      StationTimeSeriesFeature subset = stsf.subset(dateRange);
      subsetStations.add(subset);
    }

    return new StationSubset(this, subsetStations);
  }

  @Override
  public StationTimeSeriesFeatureCollection subsetFeatures(List<StationFeature> stationsFeatures) throws IOException {
    return new StationSubset(this, stationsFeatures);
  }

  private static class StationSubset extends StationTimeSeriesCollectionImpl {
    private final List<StationFeature> stations;

    StationSubset(StationTimeSeriesCollectionImpl from, List<StationFeature> stations) {
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
  }

  ///////////////////////////////
  // flatten

  // might need to override for efficiency
  @Override
  public PointFeatureCollection flatten(List<String> stationNames, CalendarDateRange dateRange, List<VariableSimpleIF> varList) throws IOException {
    if ((stationNames == null) || (stationNames.size() == 0))
      return new StationTimeSeriesCollectionFlattened(this, dateRange);

    List<StationFeature> subsetStations = getStationHelper().getStationFeaturesFromNames(stationNames);
    return new StationTimeSeriesCollectionFlattened(new StationSubset(this, subsetStations), dateRange);
  }

  @Override
  public PointFeatureCollection flatten(LatLonRect boundingBox, CalendarDateRange dateRange) throws IOException {
    if (boundingBox == null)
      return new StationTimeSeriesCollectionFlattened(this, dateRange);

    List<StationFeature> subsetStations = getStationHelper().getStationFeatures(boundingBox);
    return new StationTimeSeriesCollectionFlattened(new StationSubset(this, subsetStations), dateRange);
  }


  @Override
  public StationFeature getStationFeature(PointFeature flatPointFeature) throws IOException {
    if (flatPointFeature instanceof StationFeatureHas) {
      return ((StationFeatureHas)flatPointFeature).getStationFeature();
    }
    return null;
  }

  /////////////////////////////////////////////////////////////////////////////////////

  @Override
  public Iterator<StationTimeSeriesFeature> iterator() {
    try {
      PointFeatureCollectionIterator pfIterator = getPointFeatureCollectionIterator();
      return new CollectionIteratorAdapter<>(pfIterator);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public IOIterator<PointFeatureCollection> getCollectionIterator() throws IOException {
    return new StationIterator();
  }

  @Override
  public PointFeatureCollectionIterator getPointFeatureCollectionIterator() throws IOException {
    return new StationIterator();
  }

  // note this assumes that a StationFeature is-a PointFeatureCollection (meaning is-a StationTimeSeriesFeature) (see the cast in next())
  // subclasses must override if thats not true
  // note that subset() may have made a subset of stationHelper
  private class StationIterator implements PointFeatureCollectionIterator, IOIterator<PointFeatureCollection> {
    Iterator<StationFeature> stationIter;
    DsgCollectionImpl prev;
    CollectionInfo calcInfo;

    public StationIterator() {
      stationIter = getStationHelper().getStationFeatures().iterator();
      CollectionInfo info = getInfo();
      if (!info.isComplete())
        calcInfo = info;
    }

    @Override
    public boolean hasNext() throws IOException {
      if (prev != null && calcInfo != null)
        calcInfo.extend(prev.getInfo());

      boolean more = stationIter.hasNext();
      if (!more && calcInfo != null) calcInfo.setComplete();
      return more;
    }

    @Override
    public PointFeatureCollection next() throws IOException {
      PointFeatureCollection result = (PointFeatureCollection) stationIter.next(); // LOOK
      prev = (DsgCollectionImpl) result; // common for Station and StationProfile
      return result;
    }
  }

  /////////////////////////////////////////////////////////////////////////////////////
  // deprecated

  protected PointFeatureCollectionIterator localIterator;

  @Override
  public boolean hasNext() throws IOException {
    if (localIterator == null) resetIteration();
    return localIterator.hasNext();
  }

  @Override
  public void finish() {
    if (localIterator != null)
      localIterator.close();
  }

  @Override
  public StationTimeSeriesFeature next() throws IOException {
    return (StationTimeSeriesFeature) localIterator.next();
  }

  @Override
  public void resetIteration() throws IOException {
    localIterator = getPointFeatureCollectionIterator();
  }
}
