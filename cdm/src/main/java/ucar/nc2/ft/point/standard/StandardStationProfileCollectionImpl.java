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

package ucar.nc2.ft.point.standard;

import ucar.ma2.StructureData;
import ucar.ma2.StructureDataIterator;
import ucar.nc2.ft.*;
import ucar.nc2.ft.point.*;
import ucar.nc2.units.DateUnit;
import ucar.unidata.geoloc.Station;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
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
  private NestedTable ft;

  StandardStationProfileCollectionImpl(NestedTable ft, DateUnit timeUnit, String altUnits) throws IOException {
    super(ft.getName(), timeUnit, altUnits);
    this.ft = ft;
  }

  @Override
  protected StationHelper createStationHelper() throws IOException {
    StationHelper stationHelper = new StationHelper();
    StructureDataIterator siter = ft.getStationDataIterator(-1);
    try {
      while (siter.hasNext()) {
        StructureData stationData = siter.next();
        StationFeature s = makeStation(stationData, siter.getCurrentRecno());
        if (s != null)
          stationHelper.addStation(s);
      }
    } finally {
      siter.finish();
    }

    return stationHelper;
  }

  public StationFeature makeStation(StructureData stationData, int recnum) {
    StationFeature s = ft.makeStation(stationData);
    if (s == null) return null;
    return new StandardStationProfileFeature(s, stationData, recnum);
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

      @Override
      public void finish() {
        // ignore
      }

      public void setBufferSize(int bytes) {
      }
    };
  }

  // a time series of profiles at one station

  private class StandardStationProfileFeature extends StationProfileFeatureImpl {
    StationFeature s;
    StructureData stationData;
    int recnum;

    StandardStationProfileFeature(StationFeature s, StructureData stationData, int recnum) {
      super(s, StandardStationProfileCollectionImpl.this.getTimeUnit(), StandardStationProfileCollectionImpl.this.getAltUnits(), -1);
      this.s = s;
      this.stationData = stationData;
      this.recnum = recnum;
    }

    // iterate over series of profiles at a given station

    public PointFeatureCollectionIterator getPointFeatureCollectionIterator(int bufferSize) throws IOException {
      Cursor cursor = new Cursor(ft.getNumberOfLevels());
      cursor.recnum[2] = recnum; // the station record
      cursor.tableData[2] = stationData; // obs(leaf) = 0, profile=1, station(root)=2
      cursor.currentIndex = 2;
      ft.addParentJoin(cursor); // there may be parent joins

      return new TimeSeriesOfProfileFeatureIterator(cursor);
    }

    @Override
    public List<Date> getTimes() throws IOException {
      List<Date> result = new ArrayList<>();
      resetIteration();
      while (hasNext()) {
        ProfileFeature pf = next();
        result.add(pf.getTime());
      }
      return result;
    }

    @Override
    public ProfileFeature getProfileByDate(Date date) throws IOException {
      resetIteration();
      while (hasNext()) {
        ProfileFeature pf = next();
        if (pf.getTime().equals(date)) return pf;
      }
      return null;
    }

    @Override
    public StructureData getFeatureData() throws IOException {
      return s.getFeatureData();
    }

    private class TimeSeriesOfProfileFeatureIterator implements PointFeatureCollectionIterator {
      private Cursor cursor;
      private ucar.ma2.StructureDataIterator iter;
      private int count = 0;
      private StructureData profileData;

      TimeSeriesOfProfileFeatureIterator(Cursor cursor) throws IOException {
        this.cursor = cursor;
        iter = ft.getMiddleFeatureDataIterator(cursor, -1);
      }

      public boolean hasNext() throws IOException {
        while (true) {
          if (!iter.hasNext()) {
            timeSeriesNpts = count; // field in StationProfileFeatureImpl
            return false;
          }
          //nextProfile = iter.next();
          profileData = iter.next();
          cursor.tableData[1] = profileData;
          cursor.recnum[1] = iter.getCurrentRecno();
          cursor.currentIndex = 1;
          ft.addParentJoin(cursor); // there may be parent joins
          if (!ft.isMissing(cursor)) break;
        }
        return true;
      }

      public PointFeatureCollection next() throws IOException {
        count++;
        return new StandardProfileFeature(s, getTimeUnit(), getAltUnits(), ft.getObsTime(cursor), cursor.copy(), profileData);
      }

      public void setBufferSize(int bytes) {
        iter.setBufferSize(bytes);
      }

      public void finish() {
        iter.finish();
      }
    }
  }

  // one profile

  private class StandardProfileFeature extends ProfileFeatureImpl {
    private Cursor cursor;
    StructureData profileData;

    StandardProfileFeature(Station s, DateUnit timeUnit, String altUnits, double time, Cursor cursor, StructureData profileData) throws IOException {
      super(timeUnit.makeStandardDateString(time), timeUnit, altUnits, s.getLatitude(), s.getLongitude(), time, -1);
      this.cursor = cursor;
      this.profileData = profileData;

      if (Double.isNaN(time)) { // gotta read an obs to get the time
        try {
          PointFeatureIterator iter = getPointFeatureIterator(-1);
          if (iter.hasNext()) {
            PointFeature pf = iter.next();
            this.time = pf.getObservationTime();
            this.name = timeUnit.makeStandardDateString(this.time);
          } else {
            this.name = "empty";
          }
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    }

    // iterate over obs in the profile

    public PointFeatureIterator getPointFeatureIterator(int bufferSize) throws IOException {
      Cursor cursorIter = cursor.copy();
      StructureDataIterator structIter = ft.getLeafFeatureDataIterator(cursorIter, bufferSize);
      StandardPointFeatureIterator iter = new StandardProfileFeatureIterator(ft, timeUnit, structIter, cursorIter);
      if ((boundingBox == null) || (dateRange == null) || (npts < 0))
        iter.setCalculateBounds(this);
      return iter;
    }

    @Override
    public Date getTime() {
      return timeUnit.makeDate(time);
    }

    @Override
    public StructureData getFeatureData() throws IOException {
      return profileData;
    }
  }

  private static class StandardProfileFeatureIterator extends StandardPointFeatureIterator {

    StandardProfileFeatureIterator(NestedTable ft, DateUnit timeUnit, StructureDataIterator structIter, Cursor cursor) throws IOException {
      super(ft, timeUnit, structIter, cursor);
    }

    @Override
    protected boolean isMissing() throws IOException {
      if (super.isMissing()) return true;
      // must also check for missing z values
      return ft.isAltMissing(this.cursor);
    }
  }
}
