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

import ucar.nc2.ft.point.*;
import ucar.nc2.ft.*;
import ucar.nc2.units.DateUnit;
import ucar.nc2.units.DateFormatter;
import ucar.ma2.StructureData;
import ucar.ma2.StructureDataIterator;
import ucar.unidata.geoloc.Station;

import java.io.IOException;
import java.util.Iterator;
import java.util.Date;
import java.util.List;

/**
 * Object Heirarchy:
 * StationProfileFeatureCollection (StandardStationProfileCollectionImpl)
 * StationProfileFeature (StandardStationProfileFeature)
 * ProfileFeature (StandardProfileFeature)
 * PointFeatureIterator (StandardPointFeatureIterator)
 * PointFeature
 *
 * @author caron
 * @since Mar 28, 2008
 */
public class StandardStationProfileCollectionImpl extends StationProfileCollectionImpl {
  private DateUnit timeUnit;
  private NestedTable ft;
  private DateFormatter dateFormatter = new DateFormatter();

  StandardStationProfileCollectionImpl(NestedTable ft, DateUnit timeUnit) throws IOException {
    super(ft.getName());
    this.ft = ft;
    this.timeUnit = timeUnit;
  }

  protected void initStations() {
    if (stationHelper != null) return;
    stationHelper = new StationHelper();

    try {
      int count = 0;
      List<Station> stnList = ft.makeStations(-1);
      for (Station s : stnList) {
        stationHelper.addStation(new StandardStationProfileFeature(s, null, count++));
      }
    } catch (IOException ioe) {
      throw new RuntimeException("Failed to init stations", ioe);
    }
  }

  // iterate over stations
  public NestedPointFeatureCollectionIterator getNestedPointFeatureCollectionIterator(int bufferSize) throws IOException {
    return new NestedPointFeatureCollectionIterator() {
      private Iterator iter = getStations().iterator();
      public boolean hasNext() throws IOException {
        return iter.hasNext();
      }
      public NestedPointFeatureCollection next() throws IOException {
        return (StandardStationProfileFeature) iter.next();
      }
      public void setBufferSize(int bytes) {
      }
    };
  }

  // a time series of profiles at one station
  private class StandardStationProfileFeature extends StationProfileFeatureImpl {
    Station s;
    StructureData stationData;
    int recnum;

    StandardStationProfileFeature(Station s, StructureData stationData, int recnum) {
      super(s, StandardStationProfileCollectionImpl.this.timeUnit, -1);
      this.s = s;
      this.stationData = stationData;
      this.recnum = recnum;
    }

    // iterate over series of profiles at a given station
    public PointFeatureCollectionIterator getPointFeatureCollectionIterator(int bufferSize) throws IOException {
      Cursor cursor = new Cursor(ft.getNumberOfLevels());
      cursor.what = s;
      cursor.recnum[2] = recnum;
      cursor.tableData[2] = stationData; // obs(leaf) = 0, profile=1, station(root)=2
      cursor.parentIndex = 2; // LOOK ??
      return new StandardStationProfileFeatureIterator(cursor);
    }

    private class StandardStationProfileFeatureIterator implements PointFeatureCollectionIterator {
      Cursor cursor;
      private ucar.ma2.StructureDataIterator iter;
      private int count = 0;

      StandardStationProfileFeatureIterator(Cursor cursor) throws IOException {
        this.cursor = cursor;
        iter = ft.getStationProfileDataIterator(cursor, -1);
      }

      public boolean hasNext() throws IOException {
        boolean r = iter.hasNext();
        if (!r)
          timeSeriesNpts = count; // field in StationProfileFeatureImpl
        return r;
      }

      public PointFeatureCollection next() throws IOException {
        Cursor cursorIter = cursor.copy();
        cursorIter.recnum[1] = count++;
        cursorIter.tableData[1] = iter.next();
        cursorIter.parentIndex = 1; // LOOK ??

        double time = ft.getObsTime(cursorIter);
        return new StandardProfileFeature(s, timeUnit.makeDate(time), cursorIter);
      }

      public void setBufferSize(int bytes) {
        iter.setBufferSize(bytes);
      }
    }
  }

  // one profile
  private class StandardProfileFeature extends ProfileFeatureImpl {
    private Cursor cursor;
    private String desc;

    StandardProfileFeature(Station s, Date time, Cursor cursor) throws IOException {
      super(dateFormatter.toDateTimeStringISO(time), s.getLatitude(), s.getLongitude(), -1);
      this.cursor = cursor;
      this.desc = "time=" + time + "stn=" + s.getDescription();
    }

    public String getDescription() {
      return desc;
    }

    // iterate over obs in the profile
    public PointFeatureIterator getPointFeatureIterator(int bufferSize) throws IOException {
      StructureDataIterator structIter = ft.getStationProfileObsDataIterator(cursor, bufferSize);
      return new StandardStationProfilePointIterator(structIter, cursor.copy());
    }

    // the iterator over the observations
    private class StandardStationProfilePointIterator extends StandardPointFeatureIterator {
      StationFeatureImpl station;

      StandardStationProfilePointIterator(StructureDataIterator structIter, Cursor cursor) throws IOException {
        super(ft, timeUnit, structIter, cursor.copy(), false);
      }

      // decorate to capture npts
      @Override
      public boolean hasNext() throws IOException {
        boolean result = super.hasNext();
        if (!result)
          setNumberPoints(getCount());
        return result;
      }
    }

  }

}
