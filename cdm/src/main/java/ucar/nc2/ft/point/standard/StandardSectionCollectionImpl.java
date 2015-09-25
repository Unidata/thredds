/*
 * Copyright 1998-2015 John Caron and University Corporation for Atmospheric Research/Unidata
 *
 *  Portions of this software were developed by the Unidata Program at the
 *  University Corporation for Atmospheric Research.
 *
 *  Access and use of this software shall impose the following obligations
 *  and understandings on the user. The user is granted the right, without
 *  any fee or cost, to use, copy, modify, alter, enhance and distribute
 *  this software, and any derivative works thereof, and its supporting
 *  documentation for any purpose whatsoever, provided that this entire
 *  notice appears in all copies of the software, derivative works and
 *  supporting documentation.  Further, UCAR requests that the user credit
 *  UCAR/Unidata in any publications that result from the use of this
 *  software or in any product that includes this software. The names UCAR
 *  and/or Unidata, however, may not be used in any advertising or publicity
 *  to endorse or promote any products or commercial entity unless specific
 *  written permission is obtained from UCAR/Unidata. The user also
 *  understands that UCAR/Unidata is not obligated to provide the user with
 *  any support, consulting, training or assistance of any kind with regard
 *  to the use, operation and performance of this software nor to provide
 *  the user with any updates, revisions, new versions or "bug fixes."
 *
 *  THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 *  IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *  WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *  DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 *  INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 *  FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 *  NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 *  WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */

package ucar.nc2.ft.point.standard;

import ucar.nc2.ft.point.*;
import ucar.nc2.ft.*;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.time.CalendarDateUnit;
import ucar.ma2.StructureData;
import ucar.ma2.StructureDataIterator;
import ucar.nc2.util.IOIterator;

import java.io.IOException;
import java.util.Iterator;

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
  public Iterator<SectionFeature> iterator() {
    try {
      NestedPointFeatureCollectionIterator pfIterator = getNestedPointFeatureCollectionIterator(-1);
      return new NestedCollectionIteratorAdapter<>(pfIterator);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override // new way
  public IOIterator<PointFeatureCC> getCollectionIterator(int bufferSize) throws IOException {
    return new SectionCollectionIterator();
  }

  @Override // old way
  public NestedPointFeatureCollectionIterator getNestedPointFeatureCollectionIterator(int bufferSize) throws IOException {
    return new SectionCollectionIterator();
  }

  private class SectionCollectionIterator implements NestedPointFeatureCollectionIterator, IOIterator<PointFeatureCC> {
    private StructureDataIterator sdataIter = ft.getRootFeatureDataIterator(-1);
    private StructureData nextSection;

    SectionCollectionIterator() throws IOException {
      sdataIter = ft.getRootFeatureDataIterator(-1);
    }

    public SectionFeature next() throws IOException {
      Cursor cursor = new Cursor(ft.getNumberOfLevels());
      cursor.recnum[2] = sdataIter.getCurrentRecno();
      cursor.tableData[2] = nextSection; // obs(leaf) = 0, profile=1, section(root)=2
      cursor.currentIndex = 2;
      ft.addParentJoin(cursor); // there may be parent joins

      return new StandardSectionFeature(cursor, nextSection);
    }

    public boolean hasNext() throws IOException {
      while (true) {
        if (!sdataIter.hasNext()) return false;
        nextSection = sdataIter.next();
        if (!ft.isFeatureMissing(nextSection)) break;
      }
      return true;
    }

    public void setBufferSize(int bytes) {
    }

    @Override
    public void close() {
      sdataIter.close();
    }
  }

  ;

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
    public PointFeatureCollectionIterator getPointFeatureCollectionIterator(int bufferSize) throws IOException {
      return new StandardSectionFeatureIterator(cursor.copy());
    }

    public StructureData getFeatureData() throws IOException {
      return sectionData;
    }

    @Override
    public IOIterator<PointFeatureCollection> getCollectionIterator(int bufferSize) throws IOException {
      return new StandardSectionFeatureIterator(cursor.copy());
    }
  }

  private class StandardSectionFeatureIterator implements PointFeatureCollectionIterator, IOIterator<PointFeatureCollection> {
    Cursor cursor;
    private ucar.ma2.StructureDataIterator iter;
    StructureData profileData;

    StandardSectionFeatureIterator(Cursor cursor) throws IOException {
      this.cursor = cursor;
      iter = ft.getMiddleFeatureDataIterator(cursor, -1);
    }

    public boolean hasNext() throws IOException {
      return iter.hasNext();
    }

    public PointFeatureCollection next() throws IOException {
      Cursor cursorIter = cursor.copy();
      profileData = iter.next();
      cursorIter.tableData[1] = profileData;
      cursorIter.recnum[1] = iter.getCurrentRecno();
      cursorIter.currentIndex = 1;
      ft.addParentJoin(cursor); // there may be parent joins

      // double time = ft.getObsTime(cursorIter);
      return new StandardSectionProfileFeature(cursorIter, ft.getObsTime(cursor), profileData);
    }

    public void setBufferSize(int bytes) {
      iter.setBufferSize(bytes);
    }

    @Override
    public void close() {
      iter.close();
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
          PointFeatureIterator iter = getPointFeatureIterator(-1);
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

    public PointFeatureIterator getPointFeatureIterator(int bufferSize) throws IOException {
      Cursor cursorIter = cursor.copy();
      StructureDataIterator siter = ft.getLeafFeatureDataIterator(cursorIter, bufferSize);
      StandardPointFeatureIterator iter = new StandardSectionProfileFeatureIterator(ft, timeUnit, siter, cursorIter);
      if ((boundingBox == null) || (dateRange == null) || (npts < 0))
        iter.setCalculateBounds(this);
      return iter;
    }

    @Override
    public CalendarDate getTime() {
      return timeUnit.makeCalendarDate(time);
    }

    @Override
    public StructureData getFeatureData() throws IOException {
      return profileData;
    }

    private class StandardSectionProfileFeatureIterator extends StandardPointFeatureIterator {

      StandardSectionProfileFeatureIterator(NestedTable ft, CalendarDateUnit timeUnit, StructureDataIterator structIter, Cursor cursor) throws IOException {
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
