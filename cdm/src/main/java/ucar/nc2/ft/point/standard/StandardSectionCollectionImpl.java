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
import ucar.nc2.ft.PointFeature;
import ucar.nc2.ft.PointFeatureCC;
import ucar.nc2.ft.PointFeatureCCIterator;
import ucar.nc2.ft.PointFeatureCollection;
import ucar.nc2.ft.PointFeatureCollectionIterator;
import ucar.nc2.ft.PointFeatureIterator;
import ucar.nc2.ft.TrajectoryProfileFeature;
import ucar.nc2.ft.point.CollectionInfo;
import ucar.nc2.ft.point.DsgCollectionImpl;
import ucar.nc2.ft.point.NestedCollectionIteratorAdapter;
import ucar.nc2.ft.point.ProfileFeatureImpl;
import ucar.nc2.ft.point.SectionCollectionImpl;
import ucar.nc2.ft.point.SectionFeatureImpl;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.time.CalendarDateUnit;
import ucar.nc2.util.IOIterator;

/**
 * Nested Table implementation of SectionCollection.
 * a collection of section features
 *
 * @author caron
 * @since Oct 22, 2009
 */


public class StandardSectionCollectionImpl extends SectionCollectionImpl {
  private NestedTable ft;

  StandardSectionCollectionImpl(NestedTable ft, CalendarDateUnit timeUnit, String altUnits) throws IOException {
    super(ft.getName(), timeUnit, altUnits);
    this.ft = ft;
    this.extras = ft.getExtras();
  }

  @Override
  public Iterator<TrajectoryProfileFeature> iterator() {
    try {
      PointFeatureCCIterator pfIterator = getNestedPointFeatureCollectionIterator();
      return new NestedCollectionIteratorAdapter<>(pfIterator);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override // new way
  public IOIterator<PointFeatureCC> getCollectionIterator() throws IOException {
    return new SectionIterator();
  }

  @Override // old way
  public PointFeatureCCIterator getNestedPointFeatureCollectionIterator() throws IOException {
    return new SectionIterator();
  }

  private class SectionIterator implements PointFeatureCCIterator, IOIterator<PointFeatureCC> {
    private StructureDataIterator sdataIter = ft.getRootFeatureDataIterator();
    private StructureData sectionData;
    DsgCollectionImpl prev;
    CollectionInfo calcInfo;

    SectionIterator() throws IOException {
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
          if (calcInfo != null) calcInfo.setComplete();
          return false;
        }
        sectionData = sdataIter.next();
        if (!ft.isFeatureMissing(sectionData)) break;
      }
      return true;
    }

    @Override
    public TrajectoryProfileFeature next() throws IOException {
      Cursor cursor = new Cursor(ft.getNumberOfLevels());
      cursor.recnum[2] = sdataIter.getCurrentRecno();
      cursor.tableData[2] = sectionData; // obs(leaf) = 0, profile=1, section(root)=2
      cursor.currentIndex = 2;
      ft.addParentJoin(cursor); // there may be parent joins

      TrajectoryProfileFeature result = new StandardSectionFeature(cursor, sectionData);
      prev = (DsgCollectionImpl) result;
      return result;
    }

    @Override
    public void close() {
      sdataIter.close();
    }
  }

  // a single section: a collection of profiles along a trajectory
  private class StandardSectionFeature extends SectionFeatureImpl {
    Cursor cursor;
    StructureData sectionData;

    StandardSectionFeature(Cursor cursor, StructureData sectionData) {
      super(ft.getFeatureName(cursor), StandardSectionCollectionImpl.this.getTimeUnit(), StandardSectionCollectionImpl.this.getAltUnits());
      this.cursor = cursor;
      this.sectionData = sectionData;
    }

    @Override
    public PointFeatureCollectionIterator getPointFeatureCollectionIterator() throws IOException {
      return new ProfileIterator(cursor.copy());
    }

    @Nonnull
    @Override
    public StructureData getFeatureData() throws IOException {
      return sectionData;
    }

    @Override
    public IOIterator<PointFeatureCollection> getCollectionIterator() throws IOException {
      return new ProfileIterator(cursor.copy());
    }
  }

  private class ProfileIterator implements PointFeatureCollectionIterator, IOIterator<PointFeatureCollection> {
    Cursor cursor;
    private ucar.ma2.StructureDataIterator sdataIter;
    StructureData profileData;
    DsgCollectionImpl prev;
    CollectionInfo calcInfo;

    ProfileIterator(Cursor cursor) throws IOException {
      this.cursor = cursor;
      sdataIter = ft.getMiddleFeatureDataIterator(cursor);
      CollectionInfo info = getInfo();
      if (!info.isComplete())
        calcInfo = info;
    }

    @Override
    public boolean hasNext() throws IOException {
      if (prev != null && calcInfo != null)
        calcInfo.extend(prev.getInfo());

      boolean more = sdataIter.hasNext();
      if (!more) {
        sdataIter.close();
        if (calcInfo != null) calcInfo.setComplete();
      }
      return more;
    }

    @Override
    public PointFeatureCollection next() throws IOException {
      Cursor cursorIter = cursor.copy();
      profileData = sdataIter.next();
      cursorIter.tableData[1] = profileData;
      cursorIter.recnum[1] = sdataIter.getCurrentRecno();
      cursorIter.currentIndex = 1;
      ft.addParentJoin(cursorIter); // there may be parent joins LOOK cursor or cursorIter ?

      // double time = ft.getObsTime(cursorIter);
      PointFeatureCollection result = new StandardSectionProfileFeature(cursorIter, ft.getObsTime(cursor), profileData);
      prev = (DsgCollectionImpl) result; // common for Station and StationProfile
      return result;
    }

    @Override
    public void close() {
      sdataIter.close();
    }
  }

  // LOOK duplicate from StandardProfileCollection - also check StationProfile
  private class StandardSectionProfileFeature extends ProfileFeatureImpl {
    Cursor cursor;
    StructureData profileData;

    StandardSectionProfileFeature(Cursor cursor, double time, StructureData profileData) {
      super(ft.getFeatureName(cursor), StandardSectionCollectionImpl.this.getTimeUnit(), StandardSectionCollectionImpl.this.getAltUnits(),
              ft.getLatitude(cursor), ft.getLongitude(cursor), time, -1);

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

    @Override
    public PointFeatureIterator getPointFeatureIterator() throws IOException {
      Cursor cursorIter = cursor.copy();
      StructureDataIterator siter = ft.getLeafFeatureDataIterator(cursorIter);
      return new PointIterator(ft, timeUnit, siter, cursorIter);
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

    private class PointIterator extends StandardPointFeatureIterator {

      PointIterator(NestedTable ft, CalendarDateUnit timeUnit, StructureDataIterator structIter, Cursor cursor) throws IOException {
        super(StandardSectionProfileFeature.this, ft, timeUnit, structIter, cursor);
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
