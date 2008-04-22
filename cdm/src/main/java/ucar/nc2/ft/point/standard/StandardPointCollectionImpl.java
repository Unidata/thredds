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

import ucar.nc2.ft.point.PointCollectionImpl;
import ucar.nc2.ft.coordsys.NestedTable;
import ucar.nc2.ft.PointFeatureIterator;
import ucar.nc2.units.DateUnit;
import ucar.ma2.StructureData;

import java.io.IOException;
import java.util.List;
import java.util.ArrayList;

/**
 * Implementation of PointFeatureCollection using NestedTables
 * @author caron
 * @since Mar 28, 2008
 */
public class StandardPointCollectionImpl extends PointCollectionImpl {
  private DateUnit timeUnit;
  private NestedTable ft;

  StandardPointCollectionImpl(NestedTable ft, DateUnit timeUnit) {
    super(ft.getName());
    this.ft = ft;
    this.timeUnit = timeUnit;
  }

  public PointFeatureIterator getPointFeatureIterator(int bufferSize) throws IOException {
    // only one List object needed - it will be use for each iteration with different structData's
    List<StructureData> sdataList = new ArrayList<StructureData>(1);
    sdataList.add(null);
    boolean calcBB = (boundingBox == null) || (dateRange == null);

    return new DefaultPointFeatureIterator(ft.getObsDataIterator(bufferSize), sdataList, calcBB);
  }

  // the iterator over the observations
  private class DefaultPointFeatureIterator extends StandardPointFeatureIterator {

    DefaultPointFeatureIterator(ucar.ma2.StructureDataIterator structIter, List<StructureData> sdataList, boolean calcBB) throws IOException {
      super( ft, timeUnit, structIter, sdataList, calcBB);
    }

    // decorate hasNext to know when the iteraton is complete
    @Override
    public boolean hasNext() throws IOException {
      boolean r = super.hasNext();
      if (calcBB && !r) {
        if (boundingBox == null)
          boundingBox = getBoundingBox();
        if (dateRange == null)
          dateRange = getDateRange(timeUnit);
      }
      return r;
    }
  }
}
