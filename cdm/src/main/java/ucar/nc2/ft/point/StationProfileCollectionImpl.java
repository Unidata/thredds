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
import ucar.unidata.geoloc.LatLonRect;
import ucar.unidata.geoloc.Station;

import java.util.List;
import java.io.IOException;

/**
 * Abstract superclass for StationProfileFeatureCollection
 * Subclasses must implement getNestedPointFeatureCollection
 * @author caron
 * @since Mar 20, 2008
 */
public abstract class StationProfileCollectionImpl extends MultipleNestedPointCollectionImpl implements StationProfileFeatureCollection {

  protected StationHelper stationHelper;
  protected NestedPointFeatureCollectionIterator localIterator;

  public StationProfileCollectionImpl(String name) {
    super( name, FeatureType.STATION_PROFILE);
  }

  protected abstract void initStationHelper(); // allow station helper to be deffered initialization

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

  public StationProfileCollectionImpl subset(List<Station> stations) throws IOException {
    if (stations == null) return this;
    return new StationProfileFeatureCollectionSubset(this, stations);
  }

  public StationProfileCollectionImpl subset(ucar.unidata.geoloc.LatLonRect boundingBox) throws IOException {
    return subset( getStations(boundingBox));
  }

  public StationProfileFeature getStationProfileFeature(Station s) throws IOException {
    return (StationProfileFeature) s;  // LOOK subclass must override if not true
  }

  public PointFeatureCollectionIterator getPointFeatureCollectionIterator(int bufferSize) throws IOException {
    throw new UnsupportedOperationException("StationProfileFeatureCollection does not implement getPointFeatureCollectionIterator()");
  }

  public boolean hasNext() throws IOException {
    if (localIterator == null) resetIteration();
    return localIterator.hasNext();
  }

  public StationProfileFeature next() throws IOException {
    return (StationProfileFeature) localIterator.next();
  }

  public void resetIteration() throws IOException {
    localIterator = getNestedPointFeatureCollectionIterator(-1);
  }

  public int compareTo(Station so) {
    return name.compareTo( so.getName());
  }

    // LOOK subset by filtering on the stations, but it would be easier if we could get the StationFeature from the Station
  private class StationProfileFeatureCollectionSubset extends StationProfileCollectionImpl {
    StationProfileCollectionImpl from;

    StationProfileFeatureCollectionSubset(StationProfileCollectionImpl from, List<Station> stations) {
      super( from.getName());
      this.from = from;
      stationHelper = new StationHelper();
      stationHelper.setStations(stations);
    }

      // already done
    protected void initStationHelper() {}

    // use this only if it is multiply nested
    public NestedPointFeatureCollectionIterator getNestedPointFeatureCollectionIterator(int bufferSize) throws IOException {
      return new NestedPointCollectionIteratorFiltered( from.getNestedPointFeatureCollectionIterator(bufferSize), new Filter());
    }

    private class Filter implements NestedPointFeatureCollectionIterator.Filter {

      public boolean filter(NestedPointFeatureCollection pointFeatureCollection) {
        StationProfileFeature stationFeature = (StationProfileFeature) pointFeatureCollection;
        return stationHelper.getStation(stationFeature.getName()) != null;
      }
    }
  }

}
