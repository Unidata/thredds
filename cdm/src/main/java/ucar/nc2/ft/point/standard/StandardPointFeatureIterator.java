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

import ucar.nc2.ft.DsgFeatureCollection;
import ucar.nc2.ft.point.*;
import ucar.nc2.ft.PointFeature;
import ucar.nc2.time.CalendarDateUnit;
import ucar.ma2.StructureData;

import javax.annotation.Nonnull;
import java.io.IOException;

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
      super( timeUnit);
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
