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

import ucar.nc2.ft.point.StationTimeSeriesCollectionImpl;
import ucar.nc2.ft.point.StationFeatureImpl;
import ucar.nc2.ft.point.PointIteratorAbstract;
import ucar.nc2.ft.point.StationHelper;
import ucar.nc2.ft.*;
import ucar.nc2.units.DateRange;
import ucar.nc2.units.DateUnit;
import ucar.nc2.constants.FeatureType;
import ucar.unidata.geoloc.LatLonRect;
import ucar.unidata.geoloc.Station;

import java.io.IOException;
import java.util.Iterator;
import java.util.Formatter;
import java.util.List;

/**
 * Class Description
 *
 * @author caron
 * @since May 19, 2009
 */
public class CompositeStationCollection extends StationTimeSeriesCollectionImpl {
  private TimedCollection stnCollections;
  private FeatureDatasetPoint openDataset;
  private boolean debug = true;

  protected CompositeStationCollection(String name, TimedCollection stnCollections) throws IOException {
    super(name);
    this.stnCollections = stnCollections;
    TimedCollection.Dataset td = stnCollections.getPrototype();

    Formatter errlog = new Formatter();
    openDataset = (FeatureDatasetPoint) FeatureDatasetFactoryManager.open(FeatureType.STATION, td.getLocation(), null, errlog);
    List<FeatureCollection> fcList = openDataset.getPointFeatureCollectionList();
    StationTimeSeriesCollectionImpl openCollection = (StationTimeSeriesCollectionImpl) fcList.get(0);

    // construct list of stations
    stationHelper = new StationHelper();
    for (Station s : openCollection.getStations()) {
      stationHelper.addStation(new CompositeStationFeature(s, null));
    }
  }

  @Override
  public StationTimeSeriesFeature getStationFeature(Station s) throws IOException {
    return new CompositeStationFeature(s, null);
  }

  @Override
  public StationTimeSeriesFeatureCollection subset(List<Station> stations) throws IOException {
    return null;  //To change body of implemented methods use File | Settings | File Templates.
  }

  @Override
  public StationTimeSeriesFeatureCollection subset(LatLonRect boundingBox) throws IOException {
    return null;  //To change body of implemented methods use File | Settings | File Templates.
  }

  @Override
  public PointFeatureCollection flatten(LatLonRect boundingBox, DateRange dateRange) throws IOException {
    CompositePointCollection flat = new CompositePointCollection(getName(), stnCollections);
    return flat.subset(boundingBox, dateRange);
  }

  public PointFeatureCollectionIterator getPointFeatureCollectionIterator(int bufferSize) throws IOException {

    // an anonymous class iterating over the stations
    return new PointFeatureCollectionIterator() {
      Iterator<Station> stationIter = stationHelper.getStations().iterator();

      public boolean hasNext() throws IOException {
        return stationIter.hasNext();
      }

      public PointFeatureCollection next() throws IOException {
        return (PointFeatureCollection) stationIter.next();
      }

      public void setBufferSize(int bytes) { }
      public void finish() { }
    };
  }

  private class CompositeStationFeature extends StationFeatureImpl {

    CompositeStationFeature(Station s, DateUnit dateUnit) {
      super(s, dateUnit, -1);
    }

    // an iterator over the observations for this station
    public PointFeatureIterator getPointFeatureIterator(int bufferSize) throws IOException {
      return new CompositeStationFeatureIterator();
    }

    @Override
    public PointFeatureCollection subset(LatLonRect boundingBox, DateRange dateRange) throws IOException {
      return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public StationTimeSeriesFeature subset(DateRange dateRange) throws IOException {
      return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    private class CompositeStationFeatureIterator extends PointIteratorAbstract {
      int bufferSize = -1;
      Iterator<TimedCollection.Dataset> iter;
      FeatureDatasetPoint currentDataset;
      PointFeatureIterator pfIter = null;

      CompositeStationFeatureIterator() {
        iter = stnCollections.getIterator();
      }

      PointFeatureIterator getNextIterator() throws IOException {
        if (!iter.hasNext()) return null;
        TimedCollection.Dataset td = iter.next();
        Formatter errlog = new Formatter();
        currentDataset = (FeatureDatasetPoint) FeatureDatasetFactoryManager.open(FeatureType.STATION, td.getLocation(), null, errlog);
        List<FeatureCollection> fcList = currentDataset.getPointFeatureCollectionList();
        StationTimeSeriesFeatureCollection stnCollection = (StationTimeSeriesFeatureCollection) fcList.get(0);
        Station s = stnCollection.getStation(getName());
        StationTimeSeriesFeature stnFeature = stnCollection.getStationFeature(s);

        if (debug) System.out.printf("CompositeStationFeatureIterator open dataset%s%n", td.getLocation());

        return stnFeature.getPointFeatureIterator(bufferSize);
      }

      public boolean hasNext() throws IOException {
        if (pfIter == null) {
          pfIter = getNextIterator();
          if (pfIter == null) return false;
        }

        if (!pfIter.hasNext()) {
          pfIter.finish();
          currentDataset.close();
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
        if (pfIter != null)
          pfIter.finish();

        if (currentDataset != null)
          try {
            currentDataset.close();
          } catch (IOException e) {
            throw new RuntimeException(e);
          }

        if (CompositeStationFeature.this.npts < 0)
          CompositeStationFeature.this.npts = getCount();
      }

      public void setBufferSize(int bytes) {
        bufferSize = bytes;
      }
    }
  }

}
