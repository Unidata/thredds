/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.ft.point.standard;

import java.io.IOException;
import java.util.Iterator;
import javax.annotation.Nonnull;

import ucar.ma2.StructureData;
import ucar.ma2.StructureDataIterator;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.ft.PointFeature;
import ucar.nc2.ft.PointFeatureCollection;
import ucar.nc2.ft.PointFeatureCollectionIterator;
import ucar.nc2.ft.PointFeatureIterator;
import ucar.nc2.ft.ProfileFeature;
import ucar.nc2.ft.ProfileFeatureCollection;
import ucar.nc2.ft.point.CollectionInfo;
import ucar.nc2.ft.point.CollectionIteratorAdapter;
import ucar.nc2.ft.point.PointCollectionIteratorFiltered;
import ucar.nc2.ft.point.PointFeatureCCImpl;
import ucar.nc2.ft.point.ProfileFeatureImpl;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.time.CalendarDateRange;
import ucar.nc2.time.CalendarDateUnit;
import ucar.nc2.util.IOIterator;
import ucar.unidata.geoloc.LatLonRect;

/**
 * Nested Table implementation of ProfileCollection
 *
 * @author caron
 * @since Jan 20, 2009
 */
public class StandardProfileCollectionImpl extends PointFeatureCCImpl implements ProfileFeatureCollection {
  private NestedTable ft;

  protected StandardProfileCollectionImpl(String name, CalendarDateUnit timeUnit, String altUnits) {
    super(name, timeUnit, altUnits, FeatureType.PROFILE);
  }

  StandardProfileCollectionImpl(NestedTable ft, CalendarDateUnit timeUnit, String altUnits) {
    super(ft.getName(), timeUnit, altUnits, FeatureType.PROFILE);
    this.ft = ft;
    this.extras = ft.getExtras();
  }

  @Override
  public ProfileFeatureCollection subset(LatLonRect boundingBox) throws IOException {
    return new StandardProfileCollectionSubset(this, boundingBox);
  }

  @Override
  public ProfileFeatureCollection subset(LatLonRect boundingBox, CalendarDateRange dateRange) throws IOException {
    return new StandardProfileCollectionSubset(this, boundingBox); // LOOK ignoring dateRange
  }

  private class StandardProfileFeature extends ProfileFeatureImpl {
    Cursor cursor;
    StructureData profileData;

    StandardProfileFeature(Cursor cursor, double time, StructureData profileData) {
      super(ft.getFeatureName(cursor), StandardProfileCollectionImpl.this.getTimeUnit(), StandardProfileCollectionImpl.this.getAltUnits(),
              ft.getLatitude(cursor), ft.getLongitude(cursor), time, -1);

      this.cursor = cursor;
      this.profileData = profileData;

      if (name.equalsIgnoreCase("unknown"))
        name = timeUnit.makeCalendarDate(time).toString(); // use time as the name

      if (Double.isNaN(time)) { // gotta read an obs to get the time
        try {
          PointFeatureIterator iter = getPointFeatureIterator();
          if (iter.hasNext()) {
            PointFeature pf = iter.next();
            this.time = pf.getObservationTime();
            if (name == null) this.name = timeUnit.makeCalendarDate(this.time).toString();
          } else {
            if (name == null) this.name = "empty";
            getInfo().nfeatures = 0;
          }
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    }

    @Nonnull
    @Override
    public StructureData getFeatureData() {
      return profileData;
    }

    @Override
    public PointFeatureIterator getPointFeatureIterator() throws IOException {
      Cursor cursorIter = cursor.copy();
      StructureDataIterator siter = ft.getLeafFeatureDataIterator(cursorIter);
      return new StandardProfileFeatureIterator(ft, timeUnit, siter, cursorIter);
    }

    @Nonnull
    @Override
    public CalendarDate getTime() {
      return timeUnit.makeCalendarDate(time);
    }

    private class StandardProfileFeatureIterator extends StandardPointFeatureIterator {

      StandardProfileFeatureIterator(NestedTable ft, CalendarDateUnit timeUnit, StructureDataIterator structIter, Cursor cursor) throws IOException {
        super(StandardProfileFeature.this, ft, timeUnit, structIter, cursor);
      }

      @Override
      protected boolean isMissing() throws IOException {
        // standard filter is to check for missing time data
        if (super.isMissing()) return true;

        // must also check for missing z values
        return ft.isAltMissing(this.cursor);
      }
    }
  }

  private static class StandardProfileCollectionSubset extends StandardProfileCollectionImpl {
    StandardProfileCollectionImpl from;
    LatLonRect boundingBox;

    StandardProfileCollectionSubset(StandardProfileCollectionImpl from, LatLonRect boundingBox) {
      super(from.getName() + "-subset", from.getTimeUnit(), from.getAltUnits());
      this.from = from;
      this.boundingBox = boundingBox;
    }

    @Override
    public PointFeatureCollectionIterator getPointFeatureCollectionIterator() throws IOException {
      return new PointCollectionIteratorFiltered(from.getPointFeatureCollectionIterator(), new Filter());
    }

    private class Filter implements PointFeatureCollectionIterator.Filter {

      @Override
      public boolean filter(PointFeatureCollection pointFeatureCollection) {
        ProfileFeature profileFeature = (ProfileFeature) pointFeatureCollection;
        return boundingBox.contains(profileFeature.getLatLon());
      }
    }
  }

  /////////////////////////////////////////////////////////////////////////////////////

  @Override
  public Iterator<ProfileFeature> iterator() {
    try {
      PointFeatureCollectionIterator pfIterator = getPointFeatureCollectionIterator();
      return new CollectionIteratorAdapter<>(pfIterator);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public IOIterator<PointFeatureCollection> getCollectionIterator() throws IOException {
    return new ProfileIterator(ft.getRootFeatureDataIterator());
  }

  @Override
  public PointFeatureCollectionIterator getPointFeatureCollectionIterator() throws IOException {
    return new ProfileIterator(ft.getRootFeatureDataIterator());
  }

  private class ProfileIterator implements PointFeatureCollectionIterator, IOIterator<PointFeatureCollection> {
    StructureDataIterator structIter;
    StructureData nextProfileData;
    StandardProfileFeature prev;
    CollectionInfo calcInfo;

    ProfileIterator(ucar.ma2.StructureDataIterator structIter) throws IOException {
      this.structIter = structIter;
      CollectionInfo info = getInfo();
      if (!info.isComplete())
        calcInfo = info;
    }

    @Override
    public boolean hasNext() throws IOException {
      while (true) {
        if (prev != null && calcInfo != null)
          calcInfo.extend(prev.getInfo());

        if (!structIter.hasNext()) {
          structIter.close();
          if (calcInfo != null) calcInfo.setComplete();
          return false;
        }
        nextProfileData = structIter.next();
        if (!ft.isFeatureMissing(nextProfileData)) break;
      }
      return true;
    }

    @Override
    public ProfileFeature next() throws IOException {
      Cursor cursor = new Cursor(ft.getNumberOfLevels());
      cursor.tableData[1] = nextProfileData;
      cursor.recnum[1] = structIter.getCurrentRecno();
      cursor.currentIndex = 1;
      ft.addParentJoin(cursor); // there may be parent joins
      prev = new StandardProfileFeature(cursor, ft.getObsTime(cursor), nextProfileData);
      return prev;
    }

    @Override
    public void close() {
      structIter.close();
    }
  }

  ///////////////////////////////////////////////////////////////////////////////////////////////


  /* adapt a PointFeatureCollectionIterator to an Iterator<ProfileFeature>
  // LOOK could generify
  private class ProfileFeatureIterator implements Iterator<ProfileFeature> {
    PointFeatureCollectionIterator pfIterator;

    public ProfileFeatureIterator() {
      try {
        this.pfIterator = getPointFeatureCollectionIterator(-1);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
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
    public ProfileFeature next() {
      try {
        return (ProfileFeature) pfIterator.next();
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
  } */


  /////////////////////////////////////////////////////////////////////////////////////
  // deprecated

  private ProfileIterator localIterator = null;

  @Override
  public boolean hasNext() throws IOException {
    if (localIterator == null) resetIteration();
    return localIterator.hasNext();
  }

  // need covariant return to allow superclass to implement
  @Override
  public ProfileFeature next() throws IOException {
    return localIterator.next();
  }

  @Override
  public void resetIteration() throws IOException {
    localIterator = (ProfileIterator) getPointFeatureCollectionIterator();
  }

}
