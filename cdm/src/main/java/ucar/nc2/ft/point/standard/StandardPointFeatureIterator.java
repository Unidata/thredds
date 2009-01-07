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
import ucar.nc2.ft.point.standard.NestedTable;
import ucar.nc2.units.DateUnit;
import ucar.ma2.StructureData;

import java.io.IOException;
import java.util.List;
import java.util.ArrayList;

/**
 * A PointFeatureIterator which uses a NestedTable to implement makeFeature().
 *
 * @author caron
 * @since Mar 29, 2008
 */
public class StandardPointFeatureIterator extends PointIteratorImpl {
  private NestedTable ft;
  private DateUnit timeUnit;
  private List<StructureData> sdataList;

  StandardPointFeatureIterator(NestedTable ft, DateUnit timeUnit, ucar.ma2.StructureDataIterator structIter, List<StructureData> sdataList, boolean calcBB) throws IOException {
    super(structIter, null, calcBB);
    this.ft = ft;
    this.timeUnit = timeUnit;
    this.sdataList = sdataList;
  }

  protected PointFeature makeFeature(int recnum, StructureData sdata) throws IOException {
    sdataList.set(0, sdata); // always in the first position
    // there may be parent joins LOOK
    if (ft.needParentJoin())
      ft.addParentJoin(sdataList, sdata);
    
    return new StandardPointFeature(sdataList, timeUnit, recnum);
  }

  private class StandardPointFeature extends PointFeatureImpl {
    protected int id;
    protected List<StructureData> sdataList;

    // one could use an opaque object here instead of List<StructureData> sdataList
    public StandardPointFeature(List<StructureData> sdataList, DateUnit timeUnit, int id) {
      super( timeUnit);
      this.sdataList = new ArrayList<StructureData>( sdataList); // must keep own copy, since sdata is changing each time
      this.id = id;

      obsTime = ft.getObsTime( sdataList);
      nomTime = ft.getNomTime( sdataList);
      if (Double.isNaN(nomTime)) nomTime = obsTime;
      location = ft.getEarthLocation( sdataList);
    }

    public String getId() {
      return Integer.toString(id);
    }

    public StructureData getData() {
      return ft.makeObsStructureData(sdataList);
    }
  }

}
