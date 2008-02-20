/*
 * Copyright 1997-2007 Unidata Program Center/University Corporation for
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
package ucar.nc2.dt2.point;

import ucar.nc2.Structure;
import ucar.nc2.dt2.FeatureIterator;
import ucar.nc2.dt2.PointObsFeature;
import ucar.nc2.dt2.Feature;
import ucar.ma2.StructureData;

import java.io.IOException;

/**
 * @author caron
 * @since Sep 7, 2007
 */
public abstract class StructureDataIterator implements FeatureIterator {

  protected abstract Feature makeDatatypeWithData(int recnum, StructureData sdata) throws IOException;

  private Filter filter;
  private Structure.Iterator structIter;
  private Feature feature = null;

  private int recnum = 0;

  protected StructureDataIterator(Structure struct, int bufferSize, Filter filter) {
    this.structIter = struct.getStructureIterator(bufferSize);
    this.filter = filter;
  }

  public boolean hasNext() throws IOException {
    while (structIter.hasNext()) {
      StructureData sdata = structIter.next();
      feature = makeDatatypeWithData(recnum, sdata);
      if (filter == null || filter.filter(feature)) return true;
    }
    return false;
  }

  public Feature nextFeature() throws IOException {
    if (feature == null) return null;
    recnum++;
    return feature;
  }

  interface Filter {
    // return true if this PointObsFeature passes the filter.
    // boolean filter(StructureData sdata);
    boolean filter(Feature feature);
  }

}
