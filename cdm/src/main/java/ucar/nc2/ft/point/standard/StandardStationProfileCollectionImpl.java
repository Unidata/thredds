/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.ft.point.standard;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nonnull;

import ucar.ma2.StructureData;
import ucar.ma2.StructureDataIterator;
import ucar.nc2.ft.PointFeature;
import ucar.nc2.ft.PointFeatureCC;
import ucar.nc2.ft.PointFeatureCCIterator;
import ucar.nc2.ft.PointFeatureCollection;
import ucar.nc2.ft.PointFeatureCollectionIterator;
import ucar.nc2.ft.PointFeatureIterator;
import ucar.nc2.ft.ProfileFeature;
import ucar.nc2.ft.StationProfileFeature;
import ucar.nc2.ft.point.CollectionInfo;
import ucar.nc2.ft.point.DsgCollectionImpl;
import ucar.nc2.ft.point.ProfileFeatureImpl;
import ucar.nc2.ft.point.StationFeature;
import ucar.nc2.ft.point.StationHelper;
import ucar.nc2.ft.point.StationProfileCollectionImpl;
import ucar.nc2.ft.point.StationProfileFeatureImpl;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.time.CalendarDateUnit;
import ucar.nc2.util.IOIterator;
import ucar.unidata.geoloc.Station;

/**
 * Netsed Table implementat ion of StationProfileCollection
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

  StandardStationProfileCollectionImpl(NestedTable ft, CalendarDateUnit timeUnit, String altUnits) throws IOException {
    super(ft.getName(), timeUnit, altUnits);
    this.ft = ft;
  }

  @Override
  protected StationHelper createStationHelper() throws IOException {
    StationHelper stationHelper = new StationHelper();
    StationProfileIterator iter = new StationProfileIterator();
    while (iter.hasNext()) {
      stationHelper.addStation(iter.next());
    }
    return stationHelper;
  }

  @Override // new way
  public IOIterator<PointFeatureCC> getCollectionIterator() throws IOException {
    return new StationProfileIterator();
  }

  @Override // old way
  public PointFeatureCCIterator getNestedPointFeatureCollectionIterator() throws IOException {
    return new StationProfileIterator();
  }

  private class StationProfileIterator implements PointFeatureCCIterator, IOIterator<PointFeatureCC> {
    private StructureDataIterator sdataIter = ft.getRootFeatureDataIterator();
    private StructureData stationProfileData;
    private DsgCollectionImpl prev;
    private CollectionInfo calcInfo;

    StationProfileIterator() throws IOException {
      sdataIter = ft.getRootFeatureDataIterator();
      CollectionInfo info = getInfo();
      if (!info.isComplete())
        calcInfo = info;
    }

    @Override
    public boolean hasNext() throws IOException {
      while (true) {
        if (prev != null && calcInfo != null)
          calcInfo.extend(prev.getInfo());

        if (!sdataIter.hasNext()) {
          close();
          if (calcInfo != null)
            calcInfo.setComplete();
          return false;
        }

        stationProfileData = sdataIter.next();
        Station s = ft.makeStation(stationProfileData);
        if (s == null) continue; // skip missing station ids
        if (!ft.isFeatureMissing(stationProfileData)) break;
      }
      return true;
    }

    @Override
    public StationProfileFeature next() throws IOException {
      Cursor cursor = new Cursor(ft.getNumberOfLevels());
      cursor.recnum[2] = sdataIter.getCurrentRecno();
      cursor.tableData[2] = stationProfileData; // obs(leaf) = 0, profile=1, section(root)=2
      cursor.currentIndex = 2;
      ft.addParentJoin(cursor); // there may be parent joins

      StationProfileFeature result =  new StandardStationProfileFeature(ft.makeStation(stationProfileData), cursor, stationProfileData, cursor.recnum[2]);
      prev = (DsgCollectionImpl) result; // common for Station and StationProfile
      return result;
    }

    @Override
    public void close() {
      sdataIter.close();
    }
  }


  //////////////////////////////////////////////////////////
  // a single StationProfileFeature in this collection
  private class StandardStationProfileFeature extends StationProfileFeatureImpl {
    //int recnum;
    Cursor cursor;

    StandardStationProfileFeature(Station s, Cursor cursor, StructureData stationProfileData, int recnum) throws IOException {
      super(s, StandardStationProfileCollectionImpl.this.getTimeUnit(), StandardStationProfileCollectionImpl.this.getAltUnits(), -1);
      this.cursor = cursor;
      //this.recnum = recnum;

      cursor = new Cursor(ft.getNumberOfLevels());
      cursor.recnum[2] = recnum; // the station record
      cursor.tableData[2] = stationProfileData; // obs(leaf) = 0, profile=1, station(root)=2
      cursor.currentIndex = 2;
      ft.addParentJoin(cursor); // there may be parent joins
    }

    @Override
    public List<CalendarDate> getTimes() throws IOException {
      List<CalendarDate> result = new ArrayList<>();
      for (ProfileFeature pf : this) {
        result.add(pf.getTime());
      }
      return result;
    }

    @Override
    public ProfileFeature getProfileByDate(CalendarDate date) throws IOException {
      for (ProfileFeature pf : this) {
        if (pf.getTime().equals(date)) return pf;
      }
      return null;
    }

    @Nonnull
    @Override
    public StructureData getFeatureData() throws IOException {
      return ((StationFeature) station).getFeatureData();
    }

    @Override
    public PointFeatureCollectionIterator getPointFeatureCollectionIterator() throws IOException {
      return new ProfileFeatureIterator(cursor.copy());
    }

    @Override
    public IOIterator<PointFeatureCollection> getCollectionIterator() throws IOException {
      return new ProfileFeatureIterator(cursor.copy());
    }

    // iterate over series of profiles at a given station
    private class ProfileFeatureIterator implements PointFeatureCollectionIterator, IOIterator<PointFeatureCollection> {
      private Cursor cursor;
      private ucar.ma2.StructureDataIterator sdataIter;
      private int count = 0;
      private StructureData profileData;
      DsgCollectionImpl prev;
      CollectionInfo calcInfo;

      ProfileFeatureIterator(Cursor cursor) throws IOException {
        this.cursor = cursor;
        sdataIter = ft.getMiddleFeatureDataIterator(cursor);
        CollectionInfo info = getInfo();
        if (!info.isComplete())
          calcInfo = info;
      }

      @Override
      public boolean hasNext() throws IOException {
        while (true) {
          if (prev != null && calcInfo != null)
            calcInfo.extend(prev.getInfo());

          if (!sdataIter.hasNext()) {
            close();
            timeSeriesNpts = count; // field in StationProfileFeatureImpl
            if (calcInfo != null)
              calcInfo.setComplete();
            return false;
          }
          //nextProfile = iter.next();
          profileData = sdataIter.next();
          cursor.tableData[1] = profileData;
          cursor.recnum[1] = sdataIter.getCurrentRecno();
          cursor.currentIndex = 1;
          ft.addParentJoin(cursor); // there may be parent joins
          if (!ft.isMissing(cursor)) break; // skip missing data!
        }
        return true;
      }

      @Override
      public PointFeatureCollection next() throws IOException {
        count++;
        PointFeatureCollection result = new StandardProfileFeature(station, getTimeUnit(), getAltUnits(), ft.getObsTime(cursor), cursor.copy(), profileData);
        prev = (DsgCollectionImpl) result;
        return result;
      }

      @Override
      public void close() {
        sdataIter.close();
      }
    }
  }

  ///////////////////////////////////////////////////////////////////////////
  // a single profile in this StationProfileFeature
  private class StandardProfileFeature extends ProfileFeatureImpl {
    private Cursor cursor;
    StructureData profileData;

    StandardProfileFeature(Station s, CalendarDateUnit timeUnit, String altUnits, double time, Cursor cursor, StructureData profileData) throws IOException {
      super(timeUnit.makeCalendarDate(time).toString(), timeUnit, altUnits, s.getLatitude(), s.getLongitude(), time, -1);
      this.cursor = cursor;
      this.profileData = profileData;

      if (Double.isNaN(time)) { // gotta read an obs to get the time
        try {
          PointFeatureIterator iter = getPointFeatureIterator();
          if (iter.hasNext()) {
            PointFeature pf = iter.next();
            this.time = pf.getObservationTime();
            this.name = timeUnit.makeCalendarDate(this.time).toString();
          } else {
            this.name = "empty";
          }
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    }

    // iterate over obs in the profile
    @Override
    public PointFeatureIterator getPointFeatureIterator() throws IOException {
      Cursor cursorIter = cursor.copy();
      StructureDataIterator structIter = ft.getLeafFeatureDataIterator(cursorIter);
      return new StandardProfileFeatureIterator(ft, timeUnit, structIter, cursorIter);
    }

    @Nonnull
    @Override
    public CalendarDate getTime() {
      return timeUnit.makeCalendarDate(time);
    }

    @Nonnull
    @Override
    public StructureData getFeatureData() throws IOException {
      return profileData;
    }

    private class StandardProfileFeatureIterator extends StandardPointFeatureIterator {

      StandardProfileFeatureIterator(NestedTable ft, CalendarDateUnit timeUnit, StructureDataIterator structIter, Cursor cursor) throws IOException {
        super(StandardProfileFeature.this, ft, timeUnit, structIter, cursor);
      }

      @Override
      protected boolean isMissing() throws IOException {
        if (super.isMissing()) return true;
        // must also check for missing z values
        return ft.isAltMissing(this.cursor);
      }
    }
  }

}
