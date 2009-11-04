/*
 * Copyright 1998-2009 University Corporation for Atmospheric Research/Unidata
 *
 * Portions of this software were developed by the Unidata Program at the
 * University Corporation for Atmospheric Research.
 *
 * Access and use of this software shall impose the following obligations
 * and understandings on the user. The user is granted the right, without
 * any fee or cost, to use, copy, modify, alter, enhance and distribute
 * this software, and any derivative works thereof, and its supporting
 * documentation for any purpose whatsoever, provided that this entire
 * notice appears in all copies of the software, derivative works and
 * supporting documentation.  Further, UCAR requests that the user credit
 * UCAR/Unidata in any publications that result from the use of this
 * software or in any product that includes this software. The names UCAR
 * and/or Unidata, however, may not be used in any advertising or publicity
 * to endorse or promote any products or commercial entity unless specific
 * written permission is obtained from UCAR/Unidata. The user also
 * understands that UCAR/Unidata is not obligated to provide the user with
 * any support, consulting, training or assistance of any kind with regard
 * to the use, operation and performance of this software nor to provide
 * the user with any updates, revisions, new versions or "bug fixes."
 *
 * THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 * INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 * FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 * NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 * WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */
package ucar.nc2.ft.point.standard;

import ucar.nc2.ft.point.PointIteratorFromStructureData;
import ucar.nc2.ft.point.PointFeatureImpl;
import ucar.nc2.ft.point.StationPointFeature;
import ucar.nc2.ft.PointFeature;
import ucar.nc2.units.DateUnit;
import ucar.ma2.StructureData;
import ucar.unidata.geoloc.Station;

import java.io.IOException;

/**
 * A PointFeatureIterator which uses a NestedTable to implement makeFeature().
 *
 * @author caron
 * @since Mar 29, 2008
 */
public class StandardPointFeatureIterator extends PointIteratorFromStructureData {
  protected NestedTable ft;
  protected DateUnit timeUnit;
  protected Cursor cursor;

  StandardPointFeatureIterator(NestedTable ft, DateUnit timeUnit, ucar.ma2.StructureDataIterator structIter, Cursor cursor) throws IOException {
    super(structIter, null);
    this.ft = ft;
    this.timeUnit = timeUnit;
    this.cursor = cursor;
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
    if (ft.isTimeMissing(this.cursor)) return true;
    if (ft.isMissing(this.cursor)) return true;
    return false;
  }

  private class StandardPointFeature extends PointFeatureImpl implements StationPointFeature {
    protected Cursor cursor;

    public StandardPointFeature(Cursor cursor, DateUnit timeUnit, double obsTime) {
      super( timeUnit);
      this.cursor = cursor;

      this.obsTime = obsTime;
      nomTime = ft.getNomTime( this.cursor);
      if (Double.isNaN(nomTime)) nomTime = obsTime;
      location = ft.getEarthLocation( this.cursor);
    }

    @Override
    public StructureData getData() {
      return ft.makeObsStructureData( cursor);
    }

    @Override
    public Station getStation() {
      return ft.makeStation(cursor.getParentStructure());
    }
  }

}
