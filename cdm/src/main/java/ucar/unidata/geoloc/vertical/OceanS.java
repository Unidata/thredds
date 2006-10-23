/*
 * $Id:OceanS.java 63 2006-07-12 21:50:51Z edavis $
 *
 * Copyright  1997-2004 Unidata Program Center/University Corporation for
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

package ucar.unidata.geoloc.vertical;


import ucar.ma2.*;

import ucar.nc2.*;
import ucar.nc2.dataset.*;

import ucar.unidata.util.SpecialMathFunction;

import java.io.IOException;


/**
 * Create a 3D height(z,y,x) array using the CF formula for
 * "ocean s vertical coordinate".
 *
 * @author caron
 */

public class OceanS extends VerticalTransformImpl {

  /**
   * The eta variable name identifier
   */
  public static final String ETA = "Eta_variableName";

  /**
   * The "s" variable name identifier
   */
  public static final String S = "S_variableName";

  /**
   * The "depth" variable name identifier
   */
  public static final String DEPTH = "Depth_variableName";

  /**
   * The "depth c" variable name identifier
   */
  public static final String DEPTH_C = "Depth_c_variableName";

  /**
   * The "a" variable name
   */
  public static final String A = "A_variableName";

  /**
   * The "b" variable name
   */
  public static final String B = "B_variableName";

  /**
   * the values of depth_c, a, and b
   */
  private double depth_c;

  /**
   * The eta, s and depth variables
   */
  private Variable etaVar, sVar, depthVar, aVar, bVar, depthCVar;

  /**
   * the c array
   */
  private Array c = null;

  /**
   * Create a new vertical transform for Ocean S coordinates
   *
   * @param ds      dataset
   * @param timeDim time dimension
   * @param vCT     vertical coordinate transform
   */
  public OceanS(NetcdfDataset ds, Dimension timeDim, VerticalCT vCT) {

    super(timeDim);
    String etaName = vCT.findParameterIgnoreCase(ETA).getStringValue();
    String sName = vCT.findParameterIgnoreCase(S).getStringValue();
    String depthName = vCT.findParameterIgnoreCase(DEPTH).getStringValue();
    String aName = vCT.findParameterIgnoreCase(A).getStringValue();
    String bName = vCT.findParameterIgnoreCase(B).getStringValue();
    String depthCName = vCT.findParameterIgnoreCase(DEPTH_C).getStringValue();

    etaVar = ds.findStandardVariable(etaName);
    sVar = ds.findStandardVariable(sName);
    depthVar = ds.findStandardVariable(depthName);
    aVar = ds.findStandardVariable(aName);
    bVar = ds.findStandardVariable(bName);
    depthCVar = ds.findStandardVariable(depthCName);

    units = ds.findAttValueIgnoreCase(depthVar, "units", "none");
  }

  /**
   * Get the 3D vertical coordinate array for this time step.
   *
   * @param timeIndex the time index. Ignored if !isTimeDependent().
   * @return vertical coordinate array
   * @throws IOException problem reading data
   */
  public ArrayDouble.D3 getCoordinateArray(int timeIndex)
          throws IOException, InvalidRangeException {
    Array etaArray = readArray(etaVar, timeIndex);
    Array sArray = readArray(sVar, timeIndex);
    Array depthArray = readArray(depthVar, timeIndex);

    if (null == c) {
      double a = aVar.readScalarDouble();
      double b = bVar.readScalarDouble();
      depth_c = depthCVar.readScalarDouble();
      c = makeC(sArray, a, b);
    }

    return makeHeight(etaArray, sArray, depthArray, c, depth_c);
  }

  // C(z) = (1-b)*sinh(a*s(z))/sinh(a) + b*(tanh(a*(s(z)+0.5))/(2*tanh(0.5*a))-0.5)

  /**
   * Make the C array
   *
   * @param s s Array
   * @param a "a" value
   * @param b "b" value
   * @return the C array
   */
  private Array makeC(Array s, double a, double b) {
    int nz = (int) s.getSize();
    Index sIndex = s.getIndex();

    ArrayDouble.D1 c = new ArrayDouble.D1(nz);

    double fac1 = 1.0 - b;
    double denom1 = 1.0 / SpecialMathFunction.sinh(a);
    double denom2 = 1.0 / (2.0 * SpecialMathFunction.tanh(0.5 * a));

    for (int i = 0; i < nz; i++) {
      double sz = s.getDouble(sIndex.set(i));
      double term1 = fac1 * SpecialMathFunction.sinh(a * sz) * denom1;
      double term2 = b * (SpecialMathFunction.tanh(a * (sz + 0.5)) * denom2 - 0.5);
      c.set(i, term1 + term2);
    }

    return c;
  }


  /**
   * Make height from the given data. <br>
   * height(x,y,z) =
   * eta(x,y)*(1+s(z)) + depth_c*s(z) + (depth(x,y)-depth_c)*C(z)
   *
   * @param eta     eta Array
   * @param s       s Array
   * @param depth   depth Array
   * @param c       c Array
   * @param depth_c value of depth_c
   * @return hieght data
   */
  private ArrayDouble.D3 makeHeight(Array eta, Array s, Array depth, Array c, double depth_c) {
    int nz = (int) s.getSize();
    Index sIndex = s.getIndex();
    Index cIndex = c.getIndex();

    int[]          shape2D = eta.getShape();
    int ny = shape2D[0];
    int nx = shape2D[1];
    Index etaIndex = eta.getIndex();
    Index depthIndex = depth.getIndex();

    ArrayDouble.D3 height = new ArrayDouble.D3(nz, ny, nx);

    for (int z = 0; z < nz; z++) {
      double sz = s.getDouble(sIndex.set(z));
      double cz = c.getDouble(cIndex.set(z));
      double fac1 = 1.0 + sz;
      double term2 = depth_c * sz;

      for (int y = 0; y < ny; y++) {
        for (int x = 0; x < nx; x++) {
          double term1 = fac1 * eta.getDouble(etaIndex.set(y, x));
          double term3 = (depth.getDouble(depthIndex.set(y, x)) - depth_c) * cz;
          height.set(z, y, x, term1 + term2 + term3);
        }
      }
    }

    return height;
  }
}
