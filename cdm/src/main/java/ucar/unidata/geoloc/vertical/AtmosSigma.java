/*
 * $Id:AtmosSigma.java 63 2006-07-12 21:50:51Z edavis $
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

import java.io.IOException;


/**
 * Create a 3D height(z,y,x) array using the CF formula for
 * "atmospheric sigma vertical coordinate".
 * <p><strong>pressure(x,y,z) = ptop + sigma(z)*surfacePressure(x,y)</strong>
 *
 * @author caron
 */

public class AtmosSigma extends VerticalTransformImpl {

  /**
   * P-naught identifier
   */
  public static final String PTOP = "PressureTop_variableName";

  /**
   * Surface pressure name identifier
   */
  public static final String PS = "SurfacePressure_variableName";

  /**
   * The "depth" variable name identifier
   */
  public static final String SIGMA = "Sigma_variableName";

  /**
   * The ps, sigma variables
   */
  private Variable psVar;

  /**
   * The sigma array, function of z
   */
  private double[] sigma;

  /**
   * Top of the model
   */
  private double ptop;

  /**
   * Create a new vertical transform for Ocean S coordinates
   *
   * @param ds      dataset
   * @param timeDim time dimension
   * @param vCT     vertical coordinate transform
   */
  public AtmosSigma(NetcdfDataset ds, Dimension timeDim, VerticalCT vCT) {
    super(timeDim);

    String psName = vCT.findParameterIgnoreCase(PS).getStringValue();
    psVar = ds.findStandardVariable(psName);

    String ptopName = vCT.findParameterIgnoreCase(PTOP).getStringValue();
    Variable ptopVar = ds.findStandardVariable(ptopName);
    try {
      this.ptop = ptopVar.readScalarDouble();
    } catch (IOException e) {
      throw new IllegalArgumentException("AtmosSigma failed to read " + ptopVar + " err= "+e.getMessage());
    }

    String sigmaName = vCT.findParameterIgnoreCase(SIGMA).getStringValue();
    Variable sigmaVar = ds.findStandardVariable(sigmaName);

    try {
      Array data = sigmaVar.read();
      sigma = (double[]) data.get1DJavaArray(double.class);
    } catch (IOException e) {
      throw new IllegalArgumentException("AtmosSigma failed to read " + sigmaName+ " err= "+e.getMessage());
    }

    units = ds.findAttValueIgnoreCase(psVar, "units", "none");
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
    Array ps = readArray(psVar, timeIndex);
    Index psIndex = ps.getIndex();

    int nz = sigma.length;
    int[] shape2D = ps.getShape();
    int ny = shape2D[0];
    int nx = shape2D[1];

    ArrayDouble.D3 result = new ArrayDouble.D3(nz, ny, nx);

    for (int y = 0; y < ny; y++) {
      for (int x = 0; x < nx; x++) {
        double psVal = ps.getDouble(psIndex.set(y, x));
        for (int z = 0; z < nz; z++)
          result.set(z,y,x, ptop + sigma[z] * (psVal - ptop));
      }
    }

    return result;
  }
}