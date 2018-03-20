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
import ucar.nc2.ft.PointFeatureCollection;
import ucar.nc2.ft.PointFeatureCollectionIterator;
import ucar.nc2.ft.PointFeatureIterator;
import ucar.nc2.ft.ProfileFeature;
import ucar.nc2.ft.TrajectoryFeature;
import ucar.nc2.ft.TrajectoryFeatureCollection;
import ucar.nc2.ft.point.CollectionInfo;
import ucar.nc2.ft.point.CollectionIteratorAdapter;
import ucar.nc2.ft.point.PointCollectionIteratorFiltered;
import ucar.nc2.ft.point.PointFeatureCCImpl;
import ucar.nc2.ft.point.TrajectoryFeatureImpl;
import ucar.nc2.time.CalendarDateUnit;
import ucar.nc2.util.IOIterator;
import ucar.unidata.geoloc.LatLonRect;

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

  @Override
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

    @Override
    public PointFeatureIterator getPointFeatureIterator() throws IOException {
      Cursor cursorIter = cursor.copy();
      StructureDataIterator siter = ft.getLeafFeatureDataIterator(cursorIter);
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

    @Override
    public PointFeatureCollectionIterator getPointFeatureCollectionIterator() throws IOException {
      return new PointCollectionIteratorFiltered( from.getPointFeatureCollectionIterator(), new FilterBB());
    }

    private class FilterBB implements PointFeatureCollectionIterator.Filter {

      @Override
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
      PointFeatureCollectionIterator pfIterator = getPointFeatureCollectionIterator();
      return new CollectionIteratorAdapter<>(pfIterator);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public PointFeatureCollectionIterator getPointFeatureCollectionIterator() throws IOException {
    return new TrajCollectionIterator( ft.getRootFeatureDataIterator());
  }

  @Override
  public IOIterator<PointFeatureCollection> getCollectionIterator() throws IOException {
    return new TrajCollectionIterator( ft.getRootFeatureDataIterator());
  }

  private class TrajCollectionIterator implements PointFeatureCollectionIterator, IOIterator<PointFeatureCollection> {
    StructureDataIterator structIter;
    StructureData nextTraj;
    StandardTrajectoryFeature prev;
    CollectionInfo calcInfo;

    TrajCollectionIterator(ucar.ma2.StructureDataIterator structIter) throws IOException {
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

        if(!structIter.hasNext()) {
          structIter.close();
          if (calcInfo != null) calcInfo.setComplete();
          return false;
        }
        nextTraj = structIter.next();
        if (!ft.isFeatureMissing(nextTraj)) break;
      }
      return true;
    }

    @Override
    public TrajectoryFeature next() throws IOException {
      Cursor cursor = new Cursor(ft.getNumberOfLevels());
      cursor.recnum[1] = structIter.getCurrentRecno();
      cursor.tableData[1] = nextTraj;
      cursor.currentIndex = 1;
      ft.addParentJoin(cursor); // there may be parent joins

      prev = new StandardTrajectoryFeature(cursor, nextTraj);
      return prev;
    }

    @Override
    public void close() {
      structIter.close();
    }
  }

  /////////////////////////////////////////////////////////////////////////////////////
  // deprecated

  private TrajCollectionIterator localIterator = null;

  @Override
  public boolean hasNext() throws IOException {
    if (localIterator == null) resetIteration();
    return localIterator.hasNext();
  }

  // need covariant return to allow superclass to implement
  @Override
  public TrajectoryFeature next() throws IOException {
    return localIterator.next();
  }

  @Override
  public void resetIteration() throws IOException {
    localIterator = (TrajCollectionIterator) getPointFeatureCollectionIterator();
  }
}
