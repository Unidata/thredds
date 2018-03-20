/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.ft.point.standard;

import java.io.IOException;
import javax.annotation.Nonnull;

import ucar.ma2.StructureData;
import ucar.ma2.StructureDataIterator;
import ucar.nc2.ft.PointFeatureIterator;
import ucar.nc2.ft.StationTimeSeriesFeature;
import ucar.nc2.ft.point.StationFeature;
import ucar.nc2.ft.point.StationHelper;
import ucar.nc2.ft.point.StationTimeSeriesCollectionImpl;
import ucar.nc2.ft.point.StationTimeSeriesFeatureImpl;
import ucar.nc2.time.CalendarDateUnit;

/**
 * Object Heirarchy for StationFeatureCollection:
 * StationFeatureCollection (StandardStationCollectionImpl
 * PointFeatureCollectionIterator (anon)
 * StationFeature (StandardStationFeatureImpl)
 * PointFeatureIterator (StandardStationPointIterator)
 * PointFeatureImpl
 *
 * @author caron
 * @since Mar 28, 2008
 */
public class StandardStationCollectionImpl extends StationTimeSeriesCollectionImpl {
  private NestedTable ft;

  StandardStationCollectionImpl(NestedTable ft, CalendarDateUnit timeUnit, String altUnits) throws IOException {
    super(ft.getName(), timeUnit, altUnits);
    this.ft = ft;
    this.extras = ft.getExtras();
  }

  /**
   * Make a Station from the station data structure.
   *
   * @param stationData station data structure
   * @param recnum      station data recnum within table
   * @return Station or null, skip this Station
   */
  public StationTimeSeriesFeature makeStation(StructureData stationData, int recnum) {
    StationFeature s = ft.makeStation(stationData);
    if (s == null) return null;
    return new StandardStationFeatureImpl(s, timeUnit, stationData, recnum);
  }

  @Override
  protected StationHelper createStationHelper() throws IOException {
    StationHelper stationHelper = new StationHelper();

    try (StructureDataIterator siter = ft.getStationDataIterator()) {
      while (siter.hasNext()) {
        StructureData stationData = siter.next();
        StationTimeSeriesFeature stfs = makeStation(stationData, siter.getCurrentRecno());
        if (stfs != null) {
          stationHelper.addStation(stfs);
        }
      }
    }

    return stationHelper;
  }

  private class StandardStationFeatureImpl extends StationTimeSeriesFeatureImpl {
    int recnum;
    StructureData stationData;

    StandardStationFeatureImpl(StationFeature s, CalendarDateUnit dateUnit, StructureData stationData, int recnum) {
      super(s, dateUnit, StandardStationCollectionImpl.this.getAltUnits(), -1);
      this.recnum = recnum;
      this.stationData = stationData;
    }

    // an iterator over the observations for this station

    @Override
    public PointFeatureIterator getPointFeatureIterator() throws IOException {
      Cursor cursor = new Cursor(ft.getNumberOfLevels());
      cursor.recnum[1] = recnum;
      cursor.tableData[1] = stationData;
      cursor.currentIndex = 1;
      ft.addParentJoin(cursor); // there may be parent joins

      StructureDataIterator obsIter = ft.getLeafFeatureDataIterator(cursor);
      return new StandardPointFeatureIterator(this, ft, timeUnit, obsIter, cursor);
    }

    @Nonnull
    @Override
    public StructureData getFeatureData() {
      return stationData;
    }

  }
}
