/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.ft.point.standard;

import java.io.IOException;
import javax.annotation.Nonnull;
import ucar.ma2.StructureData;
import ucar.nc2.ft.DsgFeatureCollection;
import ucar.nc2.ft.PointFeature;
import ucar.nc2.ft.point.CollectionInfo;
import ucar.nc2.ft.point.PointCollectionImpl;
import ucar.nc2.ft.point.PointFeatureImpl;
import ucar.nc2.ft.point.PointIteratorFromStructureData;
import ucar.nc2.ft.point.StationFeature;
import ucar.nc2.ft.point.StationFeatureHas;
import ucar.nc2.ft.point.StationPointFeature;
import ucar.nc2.time.CalendarDateUnit;

/**
 * A PointFeatureIterator which uses a NestedTable to implement makeFeature().
 *
 * @author caron
 * @since Mar 29, 2008
 */
public class StandardPointFeatureIterator extends PointIteratorFromStructureData {
  protected PointCollectionImpl collectionDsg;
  protected NestedTable ft;
  protected CalendarDateUnit timeUnit;
  protected Cursor cursor;

  StandardPointFeatureIterator(PointCollectionImpl dsg, NestedTable ft, CalendarDateUnit timeUnit, ucar.ma2.StructureDataIterator structIter, Cursor cursor) throws IOException {
    super(structIter, null);
    this.collectionDsg = dsg;
    this.ft = ft;
    this.timeUnit = timeUnit;
    this.cursor = cursor;
    CollectionInfo info = dsg.getInfo();
    if (!info.isComplete()) setCalculateBounds(info);
  }

  @Override
  protected PointFeature makeFeature(int recnum, StructureData sdata) throws IOException {
    cursor.recnum[0] = recnum;
    cursor.tableData[0] = sdata; // always in the first position
    cursor.currentIndex = 0;
    ft.addParentJoin(cursor); // there may be parent joins

    if (isMissing()) return null; // missing data

    double obsTime = ft.getObsTime( this.cursor);
    // must send a copy, since sdata is changing each time, and StandardPointFeature may be stored
    return new StandardPointFeature(cursor.copy(), timeUnit, obsTime);
  }

  protected boolean isMissing() throws IOException {
    return ft.isTimeMissing(this.cursor) || ft.isMissing(this.cursor);
  }

  private class StandardPointFeature extends PointFeatureImpl implements StationPointFeature, StationFeatureHas {
    protected Cursor cursor;

    StandardPointFeature(Cursor cursor, CalendarDateUnit timeUnit, double obsTime) {
      super(collectionDsg, timeUnit);
      this.cursor = cursor;
      cursor.currentIndex = 1; // LOOK ????

      this.obsTime = obsTime;
      nomTime = ft.getNomTime( this.cursor);
      if (Double.isNaN(nomTime)) nomTime = obsTime;
      location = ft.getEarthLocation( this.cursor);
    }

    @Nonnull
    @Override
    public StructureData getFeatureData() {
      return ft.makeObsStructureData( cursor, 0);
    }

    @Nonnull
    @Override
    public StructureData getDataAll() {
      return ft.makeObsStructureData( cursor);
    }

    @Nonnull
    @Override
    public DsgFeatureCollection getFeatureCollection() {
      return dsg;
    }

    @Override
    public StationFeature getStation() {
      return ft.makeStation(cursor.getParentStructure());  // LOOK is this always possible??
    }

    @Override
    public StationFeature getStationFeature() {
      if (collectionDsg instanceof StationFeature)
        return (StationFeature) collectionDsg;
      else
        return null;
    }
  }

}
