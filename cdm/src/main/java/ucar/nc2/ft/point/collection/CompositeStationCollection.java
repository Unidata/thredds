/*
 * Copyright (c) 1998 - 2009. University Corporation for Atmospheric Research/Unidata
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
package ucar.nc2.ft.point.collection;

import thredds.inventory.TimedCollection;
import ucar.ma2.StructureData;
import ucar.nc2.Attribute;
import ucar.nc2.VariableSimpleIF;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.ft.*;
import ucar.nc2.ft.point.*;
import ucar.nc2.time.CalendarDateRange;
import ucar.nc2.units.DateUnit;
import ucar.unidata.geoloc.LatLonRect;
import ucar.unidata.geoloc.Station;

import java.io.IOException;
import java.util.Formatter;
import java.util.Iterator;
import java.util.List;

/**
 * StationTimeSeries composed of a collection of individual StationTimeSeries. "Composite" pattern.
 *
 * @author caron
 * @since May 19, 2009
 */
public class CompositeStationCollection extends StationTimeSeriesCollectionImpl implements UpdateableCollection {
  private TimedCollection dataCollection;
  protected List<VariableSimpleIF> dataVariables;
  protected List<Attribute> globalAttributes;

  protected CompositeStationCollection(String name, DateUnit timeUnit, String altUnits, TimedCollection dataCollection,
                                       List<StationFeature> stns, List<VariableSimpleIF> dataVariables) throws IOException {
    super(name, timeUnit, altUnits);
    this.dataCollection = dataCollection;
    TimedCollection.Dataset td = dataCollection.getPrototype();
    if (td == null)
      throw new RuntimeException("No datasets in the collection");

    if ((stns != null) && (stns.size() > 0)) {
      stationHelper = new StationHelper();
      for (StationFeature s : stns)
        stationHelper.addStation(new CompositeStationFeature(s, timeUnit, altUnits, s.getFeatureData(), this.dataCollection));
    }

    this.dataVariables = dataVariables;
  }

  @Override
  protected void initStationHelper() {
    TimedCollection.Dataset td = dataCollection.getPrototype();
    if (td == null)
      throw new RuntimeException("No datasets in the collection");

    Formatter errlog = new Formatter();
    FeatureDatasetPoint openDataset = null;
    try {
      openDataset = (FeatureDatasetPoint) FeatureDatasetFactoryManager.open(FeatureType.STATION, td.getLocation(), null, errlog);

      List<FeatureCollection> fcList = openDataset.getPointFeatureCollectionList();
      StationTimeSeriesCollectionImpl openCollection = (StationTimeSeriesCollectionImpl) fcList.get(0);
      List<Station> stns = openCollection.getStations();
      stationHelper = new StationHelper();
      for (Station s : stns) {
        StationTimeSeriesFeature stnFeature = openCollection.getStationFeature(s);
        stationHelper.addStation(new CompositeStationFeature(s, timeUnit, altUnits, stnFeature.getFeatureData(), this.dataCollection));
      }

      dataVariables = openDataset.getDataVariables();
      globalAttributes = openDataset.getGlobalAttributes();

    } catch (Exception ioe) {
      throw new RuntimeException(td.getLocation(), ioe);

    } finally {
      try {
        if (openDataset != null) openDataset.close();
      } catch (Throwable t) {
      }
    }
  }

  public List<VariableSimpleIF> getDataVariables() {
    if (dataVariables == null) initStationHelper();

    return dataVariables;
  }

  public List<Attribute> getGlobalAttributes() {
    if (globalAttributes == null) initStationHelper();
    return globalAttributes;
  }

  @Override
  public void update() throws IOException {
    dataCollection.update();
  }

  // Must override default subsetting implementation for efficiency
  // StationTimeSeriesFeatureCollection

  @Override
  public StationTimeSeriesFeatureCollection subset(List<Station> stations) throws IOException {
    if (stations == null) return this;
    List<StationFeature> subset = stationHelper.getStationFeatures(stations);
    return new CompositeStationCollection(getName(), getTimeUnit(), getAltUnits(), dataCollection, subset, dataVariables);
  }

  @Override
  public StationTimeSeriesFeatureCollection subset(ucar.unidata.geoloc.LatLonRect boundingBox) throws IOException {
    if (boundingBox == null) return this;
    List<StationFeature> subset = stationHelper.getStationFeatures(boundingBox);
    return new CompositeStationCollection(getName(), getTimeUnit(), getAltUnits(), dataCollection, subset, dataVariables);
  }

  @Override
  public StationTimeSeriesFeature getStationFeature(Station s) throws IOException {
    StationFeature stnFeature = stationHelper.getStationFeature(s);
    return new CompositeStationFeature(stnFeature, timeUnit, altUnits, stnFeature.getFeatureData(), dataCollection);
  }

  @Override
  public PointFeatureCollection flatten(LatLonRect boundingBox, CalendarDateRange dateRange) throws IOException {
    TimedCollection subsetCollection = (dateRange != null) ? dataCollection.subset(dateRange) : dataCollection;
    return new CompositeStationCollectionFlattened(getName(), getTimeUnit(), getAltUnits(), boundingBox, dateRange, subsetCollection);

    //return flatten(stationHelper.getStations(boundingBox), dateRange, null);
  }

  @Override
  public PointFeatureCollection flatten(List<String> stations, CalendarDateRange dateRange, List<VariableSimpleIF> varList) throws IOException {
    TimedCollection subsetCollection = (dateRange != null) ? dataCollection.subset(dateRange) : dataCollection;
    return new CompositeStationCollectionFlattened(getName(), getTimeUnit(), getAltUnits(), stations, dateRange, varList, subsetCollection);
  }

  //////////////////////////////////////////////////////////
  // the iterator over StationTimeSeriesFeature objects
  // problematic - since each station will independently iterate over the datasets.
  // betterto use the flatten() method, then reconstitute the station with getStation(pointFeature)

  @Override
  public PointFeatureCollectionIterator getPointFeatureCollectionIterator(int bufferSize) throws IOException {

    // an anonymous class iterating over the stations
    return new PointFeatureCollectionIterator() {
      Iterator<Station> stationIter = stationHelper.getStations().iterator();

      @Override
      public boolean hasNext() throws IOException {
        return stationIter.hasNext();
      }

      @Override
      public PointFeatureCollection next() throws IOException {
        return (PointFeatureCollection) stationIter.next();
      }

      @Override
      public void setBufferSize(int bytes) {
      }

      @Override
      public void finish() {
      }
    };
  }

  // the StationTimeSeriesFeature

  private class CompositeStationFeature extends StationTimeSeriesFeatureImpl {
    private TimedCollection collForFeature;
    private StructureData sdata;

    CompositeStationFeature(Station s, DateUnit timeUnit, String altUnits, StructureData sdata, TimedCollection collForFeature) {
      super(s, timeUnit, altUnits, -1);
      setCalendarDateRange(collForFeature.getDateRange());
      this.sdata = sdata;
      this.collForFeature = collForFeature;
    }

    // an iterator over the observations for this station

    @Override
    public PointFeatureIterator getPointFeatureIterator(int bufferSize) throws IOException {
      CompositeStationFeatureIterator iter = new CompositeStationFeatureIterator();
      if ((boundingBox == null) || (dateRange == null) || (npts < 0))
        iter.setCalculateBounds(this);
      return iter;
    }

    @Override
    public StationTimeSeriesFeature subset(CalendarDateRange dateRange) throws IOException {
      if (dateRange == null) return this;
      return new CompositeStationFeature(s, getTimeUnit(), getAltUnits(), sdata, collForFeature.subset(dateRange));
    }

    @Override
    public StructureData getFeatureData() throws IOException {
      return sdata;
    }

    @Override
    public PointFeatureCollection subset(LatLonRect boundingBox, CalendarDateRange dateRange) throws IOException {
      if (boundingBox != null) {
        if (!boundingBox.contains(s.getLatLon())) return null;
        if (dateRange == null) return this;
      }
      return subset(dateRange);
    }

    // the iterator over PointFeature - an iterator over iterators, one for each dataset

    private class CompositeStationFeatureIterator extends PointIteratorAbstract {
      private int bufferSize = -1;
      private Iterator<TimedCollection.Dataset> iter;
      private FeatureDatasetPoint currentDataset;
      private PointFeatureIterator pfIter = null;
      private boolean finished = false;

      CompositeStationFeatureIterator() {
        iter = collForFeature.getDatasets().iterator();
      }

      private PointFeatureIterator getNextIterator() throws IOException {
        if (!iter.hasNext()) return null;
        TimedCollection.Dataset td = iter.next();
        Formatter errlog = new Formatter();
        currentDataset = (FeatureDatasetPoint) FeatureDatasetFactoryManager.open(FeatureType.STATION, td.getLocation(), null, errlog);
        List<FeatureCollection> fcList = currentDataset.getPointFeatureCollectionList();
        StationTimeSeriesFeatureCollection stnCollection = (StationTimeSeriesFeatureCollection) fcList.get(0);
        Station s = stnCollection.getStation(getName());
        if (s == null) {
          System.out.printf("CompositeStationFeatureIterator dataset: %s missing station %s%n",
                  td.getLocation(), getName());
          return getNextIterator();
        }

        StationTimeSeriesFeature stnFeature = stnCollection.getStationFeature(s);
        if (CompositeDatasetFactory.debug)
          System.out.printf("CompositeStationFeatureIterator open dataset: %s for %s%n", td.getLocation(), s.getName());
        return stnFeature.getPointFeatureIterator(bufferSize);
      }

      public boolean hasNext() throws IOException {
        if (pfIter == null) {
          pfIter = getNextIterator();
          if (pfIter == null) {
            finish();
            return false;
          }
        }

        if (!pfIter.hasNext()) {
          pfIter.finish();
          currentDataset.close();
          if (CompositeDatasetFactory.debug)
            System.out.printf("CompositeStationFeatureIterator close dataset: %s%n", currentDataset.getLocation());
          pfIter = getNextIterator();
          return hasNext();
        }

        return true;
      }

      public PointFeature next() throws IOException {
        npts++;
        return pfIter.next();
      }

      public void finish() {
        if (finished) return;

        if (pfIter != null)
          pfIter.finish();

        if (currentDataset != null)
          try {
            currentDataset.close();
            if (CompositeDatasetFactory.debug)
              System.out.printf("CompositeStationFeatureIterator close dataset: %s%n", currentDataset.getLocation());
          } catch (IOException e) {
            throw new RuntimeException(e);
          }

        finishCalcBounds();
        finished = true;
        //if (CompositeStationFeature.this.npts < 0) // LOOK needed ?
        //  CompositeStationFeature.this.npts = getCount();
      }

      public void setBufferSize(int bytes) {
        bufferSize = bytes;
      }
    }
  }

}
