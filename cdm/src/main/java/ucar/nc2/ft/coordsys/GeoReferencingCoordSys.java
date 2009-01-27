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
package ucar.nc2.ft.coordsys;

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
