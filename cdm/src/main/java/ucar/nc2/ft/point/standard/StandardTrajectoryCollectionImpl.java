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
import ucar.nc2.constants.FeatureType;
import ucar.nc2.time.CalendarDateUnit;
import ucar.ma2.StructureDataIterator;
import ucar.ma2.StructureData;
import ucar.nc2.util.IOIterator;
import ucar.unidata.geoloc.LatLonRect;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.Iterator;

/**
 * TrajectoryFeatureCollection using nested tables.
 * @author caron
 * @since Dec 31, 2008
 */
public class StandardTrajectoryCollectionImpl extends PointFeatureCCImpl implements TrajectoryFeatureCollection {
  private NestedTable ft;

  protected StandardTrajectoryCollectionImpl(String name, CalendarDateUnit timeUnit, String altUnits) {
    super(name, timeUnit, altUnits, FeatureType.TRAJECTORY);
  }

  StandardTrajectoryCollectionImpl(NestedTable ft, CalendarDateUnit timeUnit, String altUnits) {
    super(ft.getName(), timeUnit, altUnits, FeatureType.TRAJECTORY);
    this.ft = ft;
    this.extras = ft.getExtras();
  }

  public TrajectoryFeatureCollection subset(LatLonRect boundingBox) throws IOException {
    return new StandardTrajectoryCollectionSubset( this, boundingBox);
  }

  ///////////////////////////////////////
  // TrajectoryFeature using nested tables.
  private class StandardTrajectoryFeature extends TrajectoryFeatureImpl {
    Cursor cursor;
    StructureData trajData;

    StandardTrajectoryFeature(Cursor cursor, StructureData trajData) {
      super(ft.getFeatureName(cursor), StandardTrajectoryCollectionImpl.this.getTimeUnit(), StandardTrajectoryCollectionImpl.this.getAltUnits(), -1);
      this.cursor = cursor;
      this.trajData = trajData;
    }

    public PointFeatureIterator getPointFeatureIterator(int bufferSize) throws IOException {
      Cursor cursorIter = cursor.copy();
      StructureDataIterator siter = ft.getLeafFeatureDataIterator( cursorIter, bufferSize);
      return new StandardPointFeatureIterator(this, ft, timeUnit, siter, cursorIter);
    }

    @Nonnull
    @Override
    public StructureData getFeatureData() {
      return trajData;
    }
  }

  ///////////////////////////////////////
  private static class StandardTrajectoryCollectionSubset extends StandardTrajectoryCollectionImpl {
    TrajectoryFeatureCollection from;
    LatLonRect boundingBox;

    StandardTrajectoryCollectionSubset(TrajectoryFeatureCollection from, LatLonRect boundingBox) {
      super(from.getName()+"-subset", from.getTimeUnit(), from.getAltUnits());
      this.from = from;
      this.boundingBox = boundingBox;
    }

    public PointFeatureCollectionIterator getPointFeatureCollectionIterator(int bufferSize) throws IOException {
      return new PointCollectionIteratorFiltered( from.getPointFeatureCollectionIterator(bufferSize), new FilterBB());
    }

    private class FilterBB implements PointFeatureCollectionIterator.Filter {

      public boolean filter(PointFeatureCollection pointFeatureCollection) {
        ProfileFeature profileFeature = (ProfileFeature) pointFeatureCollection;
        return boundingBox.contains(profileFeature.getLatLon());
      }
    }
  }

  /////////////////////////////////////////////////////////////////////////////////////

  @Override
  public Iterator<TrajectoryFeature> iterator() {
    try {
      PointFeatureCollectionIterator pfIterator = getPointFeatureCollectionIterator(-1);
      return new CollectionIteratorAdapter<>(pfIterator);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public PointFeatureCollectionIterator getPointFeatureCollectionIterator(int bufferSize) throws IOException {
    return new TrajCollectionIterator( ft.getRootFeatureDataIterator(bufferSize));
  }

  @Override
  public IOIterator<PointFeatureCollection> getCollectionIterator(int bufferSize) throws IOException {
    return new TrajCollectionIterator( ft.getRootFeatureDataIterator(bufferSize));
  }

  private class TrajCollectionIterator implements PointFeatureCollectionIterator, IOIterator<PointFeatureCollection> {
    StructureDataIterator structIter;
    StructureData nextTraj;

    TrajCollectionIterator(ucar.ma2.StructureDataIterator structIter) throws IOException {
      this.structIter = structIter;
    }

    public boolean hasNext() throws IOException {
      while (true) {
        if(!structIter.hasNext()) {
          structIter.close();
          return false;
        }
        nextTraj = structIter.next();
        if (!ft.isFeatureMissing(nextTraj)) break;
      }
      return true;
    }

    public TrajectoryFeature next() throws IOException {
      Cursor cursor = new Cursor(ft.getNumberOfLevels());
      cursor.recnum[1] = structIter.getCurrentRecno();
      cursor.tableData[1] = nextTraj;
      cursor.currentIndex = 1;
      ft.addParentJoin(cursor); // there may be parent joins

      return new StandardTrajectoryFeature(cursor, nextTraj);
    }

    public void close() {
      structIter.close();
    }
  }

  /////////////////////////////////////////////////////////////////////////////////////
  // deprecated

  private TrajCollectionIterator localIterator = null;

  public boolean hasNext() throws IOException {
    if (localIterator == null) resetIteration();
    return localIterator.hasNext();
  }

  // need covariant return to allow superclass to implement
  public TrajectoryFeature next() throws IOException {
    return localIterator.next();
  }

  public void resetIteration() throws IOException {
    localIterator = (TrajCollectionIterator) getPointFeatureCollectionIterator(-1);
  }
}
