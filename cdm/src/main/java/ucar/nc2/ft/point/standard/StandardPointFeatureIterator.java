/*
 * Copyright 1997-2008 Unidata Program Center/University Corporation for
 * Atmospheric Research, P.O. Box 3000, Boulder, CO 80307,
 * support@unidata.ucar.edu.
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or (at
 * your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser
 * General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package ucar.nc2.ft.point.standard;

import ucar.nc2.ft.point.PointIteratorImpl;
import ucar.nc2.ft.point.PointFeatureImpl;
import ucar.nc2.ft.PointFeature;
import ucar.nc2.units.DateUnit;
import ucar.ma2.StructureData;

import java.io.IOException;

/**
 * A PointFeatureIterator which uses a NestedTable to implement makeFeature().
 *
 * @author caron
 * @since Mar 29, 2008
 */
public class StandardPointFeatureIterator extends PointIteratorImpl {
  private NestedTable ft;
  private DateUnit timeUnit;
  private Cursor cursor;

  StandardPointFeatureIterator(NestedTable ft, DateUnit timeUnit, ucar.ma2.StructureDataIterator structIter,
                               Cursor cursor, boolean calcBB) throws IOException {
    super(structIter, null, calcBB);
    this.ft = ft;
    this.timeUnit = timeUnit;
    this.cursor = cursor;
  }

  protected PointFeature makeFeature(int recnum, StructureData sdata) throws IOException {
    cursor.tableData[0] = sdata; // always in the first position
    ft.addParentJoin(cursor); // there may be parent joins LOOK
    
    return new StandardPointFeature(cursor, timeUnit, recnum);
  }

  private class StandardPointFeature extends PointFeatureImpl {
    protected int id;
    protected Cursor cursor;

    public StandardPointFeature(Cursor cursor, DateUnit timeUnit, int id) {
      super( timeUnit);
      this.cursor = cursor.copy(); // must keep own copy, since sdata is changing each time
      this.id = id;

      obsTime = ft.getObsTime( this.cursor);
      nomTime = ft.getNomTime( this.cursor);
      if (Double.isNaN(nomTime)) nomTime = obsTime;
      location = ft.getEarthLocation( this.cursor);
    }

    public Object getId() {
      return Integer.toString(id);
    }

    public StructureData getData() {
      return ft.makeObsStructureData( cursor);
    }
  }

}
