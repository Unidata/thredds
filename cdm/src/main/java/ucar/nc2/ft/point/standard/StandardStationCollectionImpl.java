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
package ucar.nc2.ft.point.standard;

import ucar.nc2.ft.point.StationTimeSeriesCollectionImpl;
import ucar.nc2.ft.point.StationFeatureImpl;
import ucar.nc2.ft.point.StationHelper;
import ucar.nc2.ft.*;
import ucar.nc2.units.DateUnit;
import ucar.ma2.StructureDataIterator;
import ucar.ma2.StructureData;
import ucar.unidata.geoloc.Station;

import java.io.IOException;
import java.util.Iterator;

/**
 * Object Heirarchy for StationFeatureCollection:
 *   StationFeatureCollection (StandardStationCollectionImpl
 *   PointFeatureCollectionIterator (anon)
 *     StationFeature (StandardStationFeatureImpl)
 *     PointFeatureIterator (StandardStationPointIterator)
 *       PointFeatureImpl
 *
 * @author caron
 * @since Mar 28, 2008
 */
public class StandardStationCollectionImpl extends StationTimeSeriesCollectionImpl {
  private DateUnit timeUnit;
  private NestedTable ft;

  StandardStationCollectionImpl(NestedTable ft, DateUnit timeUnit) throws IOException {
    super(ft.getName());
    this.timeUnit = timeUnit;
    this.ft = ft;

    // LOOK can we defer StationHelper ?
    stationHelper = new StationHelper();
    int count = 0;
    StructureDataIterator siter = ft.getStationDataIterator(-1);
    while (siter.hasNext()) {
      StructureData stationData = siter.next();
      stationHelper.addStation( makeStation(stationData, count++));
    }
  }

  private Station makeStation(StructureData stationData, int recnum) {
    Station s = ft.makeStation(stationData);
    return new StandardStationFeatureImpl(s, timeUnit, stationData, recnum);
  }

  public PointFeatureCollectionIterator getPointFeatureCollectionIterator(int bufferSize) throws IOException {
    // an anonymous class iterating over the stations
    return new PointFeatureCollectionIterator() {
      Iterator<Station> stationIter= stationHelper.getStations().iterator();

      public boolean hasNext() throws IOException {
        return stationIter.hasNext();
      }

      public PointFeatureCollection next() throws IOException {
        return (StationFeatureImpl) stationIter.next();
      }

      public void setBufferSize(int bytes) { }

      public void finish() { }

    };
  }

  private class StandardStationFeatureImpl extends StationFeatureImpl {
      int recnum;
      StructureData stationData;

    StandardStationFeatureImpl(Station s, DateUnit dateUnit, StructureData stationData, int recnum) {
      super(s, dateUnit, -1);
      this.recnum = recnum;
      this.stationData = stationData;
    }

    // an iterator over the observations for this station
    public PointFeatureIterator getPointFeatureIterator(int bufferSize) throws IOException {
      Cursor cursor = new Cursor(ft.getNumberOfLevels());
      cursor.recnum[1] = recnum;
      cursor.tableData[1] = stationData;
      cursor.parentIndex = 1; // LOOK ?
      StructureDataIterator obsIter = ft.getStationObsDataIterator(cursor, bufferSize);
      return new StandardStationPointIterator((size() < 0) ? this : null, obsIter, cursor);
    }

  }

  // the iterator over the observations
  private class StandardStationPointIterator extends StandardPointFeatureIterator {
    StationFeatureImpl station;

    StandardStationPointIterator(StationFeatureImpl station, StructureDataIterator structIter, Cursor tableData) throws IOException {
      super(ft, timeUnit, structIter, tableData);
      this.station = station;
    }

    // decorate to capture npts
    @Override
    public boolean hasNext() throws IOException {
      boolean result = super.hasNext();
      if (!result && (station != null))
        station.setNumberPoints(getCount());
      return result;
    }
  }

}