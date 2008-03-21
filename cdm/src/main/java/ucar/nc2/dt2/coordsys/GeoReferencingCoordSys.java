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
import ucar.ma2.Array;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.Section;

import java.util.List;
import java.io.IOException;

/**
 * @author caron
 * @since Mar 21, 2008
 */
public class GeoReferencingCoordSys {
  private CoordinateSystem cs;

  public GeoReferencingCoordSys( CoordinateSystem cs) {
    this.cs = cs;
  }

  public double readLatitudeCoord(Variable fromVar, int[] index) throws IOException, InvalidRangeException {
    CoordinateAxis axis = cs.getLatAxis();
    if (null == axis) throw new IllegalArgumentException("There is no latitude coordinate");
    return readValue( axis, fromVar, index);
  }

  public double readLongitudeCoord(Variable fromVar, int[] index) throws IOException, InvalidRangeException {
    CoordinateAxis axis = cs.getLonAxis();
    if (null == axis) throw new IllegalArgumentException("There is no longiude coordinate");
    return readValue( axis, fromVar, index);
  }

  public double readPressureCoord(Variable fromVar, int[] index) throws IOException, InvalidRangeException {
    CoordinateAxis axis = cs.getPressureAxis();
    if (null == axis) throw new IllegalArgumentException("There is no pressure coordinate");
    return readValue( axis, fromVar, index);
  }

  public double readHeightCoord(Variable fromVar, int[] index) throws IOException, InvalidRangeException {
    CoordinateAxis axis = cs.getHeightAxis();
    if (null == axis) throw new IllegalArgumentException("There is no height coordinate");
    return readValue( axis, fromVar, index);
  }

  public double readTimeCoord(Variable fromVar, int[] index) throws IOException, InvalidRangeException {
    CoordinateAxis axis = cs.getTaxis();
    if (null == axis) throw new IllegalArgumentException("There is no time coordinate");
    return readValue( axis, fromVar, index);
  }

  public double readGeoXCoord(Variable fromVar, int[] index) throws IOException, InvalidRangeException {
    CoordinateAxis axis = cs.getXaxis();
    if (null == axis) throw new IllegalArgumentException("There is no GeoX coordinate");
    return readValue( axis, fromVar, index);
  }

  public double readGeoYCoord(Variable fromVar, int[] index) throws IOException, InvalidRangeException {
    CoordinateAxis axis = cs.getYaxis();
    if (null == axis) throw new IllegalArgumentException("There is no GeoY coordinate");
    return readValue( axis, fromVar, index);
  }

  public double readGeoZCoord(Variable fromVar, int[] index) throws IOException, InvalidRangeException {
    CoordinateAxis axis = cs.getZaxis();
    if (null == axis) throw new IllegalArgumentException("There is no GeoZ coordinate");
    return readValue( axis, fromVar, index);
  }

  public double readValue(Variable targetVar, Variable fromVar, int[] index) throws InvalidRangeException, IOException {
    Section axisElement = mapIndex( targetVar, fromVar, index);
    Array result = targetVar.read(axisElement);
    return result.nextDouble();
  }

  public Section mapIndex(Variable targetVar, Variable fromVar, int[] fromIndex) throws InvalidRangeException {
    List<Dimension> toDims = targetVar.getDimensions();
    List<Dimension> fromDims = fromVar.getDimensions();
    Section result = new Section();

    // each dimension in the target must be present in the source
    for (int i=0; i<toDims.size(); i++) {
      Dimension dim = toDims.get(i);
      int varIndex = fromDims.indexOf(toDims.get(i));
      if (varIndex < 0) throw new IllegalArgumentException("Dimension "+dim+" does not exist");
      result.appendRange(fromIndex[varIndex], fromIndex[varIndex]);
    }
    return result;
  }
}
