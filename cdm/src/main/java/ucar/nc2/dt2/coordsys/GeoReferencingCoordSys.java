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
package ucar.nc2.dt2.coordsys;

import ucar.nc2.dataset.CoordinateSystem;
import ucar.nc2.dataset.CoordinateAxis;
import ucar.nc2.Dimension;
import ucar.nc2.Variable;

import java.util.List;

/**
 * @author caron
 * @since Mar 21, 2008
 */
public class GeoReferencingCoordSys {
  private CoordinateSystem cs;

  public GeoReferencingCoordSys( CoordinateSystem cs) {
    this.cs = cs;
  }

  public int[] getLatitudeIndex(List<Dimension> dims, int[] index) {
    CoordinateAxis axis = cs.getLatAxis();
    int[] result = new int[ axis.getRank()];
    List<Dimension> axisDims = axis.getDimensions();
    for (int i=0; i<axisDims.size(); i++) {
      int varIndex = dims.indexOf(axisDims.get(i));
      result[i] = index[varIndex];
    }
    return result;
  }

  public int[] mapIndex(Variable toVar, List<Dimension> fromDims, int[] fromIndex) {
    int[] result = new int[ toVar.getRank()];
    List<Dimension> toDims = toVar.getDimensions();
    for (int i=0; i<toDims.size(); i++) {
      int varIndex = fromDims.indexOf(toDims.get(i));
      if (varIndex < 0)
      result[i] = fromIndex[varIndex];
    }
    return result;
  }
}
