/*
 * $Id: OceanSigma.java 64 2006-07-12 22:30:50Z edavis $
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
 * "ocean sigma vertical coordinate".
 *
 * @author caron
 */

public class OceanSigma extends VerticalTransformImpl {

  /**
   * The eta variable name identifier
   */
  public static final String ETA = "Eta_variableName";

  /**
   * The "s" variable name identifier
   */
  public static final String SIGMA = "Sigma_variableName";

  /**
   * The "depth" variable name identifier
   */
  public static final String DEPTH = "Depth_variableName";

  /**
   * The eta, s and depth variables
   */
  private Variable etaVar, sVar, depthVar;

  /**
   * Create a new vertical transform for Ocean S coordinates
   *
   * @param ds      dataset
   * @param timeDim time dimension
   * @param vCT     vertical coordinate transform
   */
  public OceanSigma(NetcdfDataset ds, Dimension timeDim, VerticalCT vCT) {

    super(timeDim);
    String etaName = vCT.findParameterIgnoreCase(ETA).getStringValue();
    String sName = vCT.findParameterIgnoreCase(SIGMA).getStringValue();
    String depthName = vCT.findParameterIgnoreCase(DEPTH).getStringValue();

    etaVar = ds.findStandardVariable(etaName);
    sVar = ds.findStandardVariable(sName);
    depthVar = ds.findStandardVariable(depthName);

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
    Array eta = readArray(etaVar, timeIndex);
    Array sigma = readArray(sVar, timeIndex);
    Array depth = readArray(depthVar, timeIndex);

    int nz = (int) sigma.getSize();
    Index sIndex = sigma.getIndex();

    int[] shape2D = eta.getShape();
    int ny = shape2D[0];
    int nx = shape2D[1];
    Index etaIndex = eta.getIndex();
    Index depthIndex = depth.getIndex();

    ArrayDouble.D3 height = new ArrayDouble.D3(nz, ny, nx);

    for (int z = 0; z < nz; z++) {
      double sigmaVal = sigma.getDouble(sIndex.set(z));
      for (int y = 0; y < ny; y++) {
        for (int x = 0; x < nx; x++) {
          double etaVal = eta.getDouble(etaIndex.set(y, x));
          double depthVal = depth.getDouble(depthIndex.set(y, x));

          height.set(z, y, x, etaVal + sigmaVal * (depthVal + etaVal));
        }
      }
    }

    return height;
  }
}
