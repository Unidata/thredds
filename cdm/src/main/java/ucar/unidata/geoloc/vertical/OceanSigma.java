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
package ucar.unidata.geoloc.vertical;

import ucar.ma2.*;
import ucar.nc2.*;
import ucar.unidata.util.Parameter;

import java.io.IOException;
import java.util.List;

/**
 * Create a 3D height(z,y,x) array using the CF formula for
 * "ocean sigma vertical coordinate".
 *
 * @author caron
 * @see <a href="http://cf-pcmdi.llnl.gov/">http://cf-pcmdi.llnl.gov/</a>
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
   * @param params  list of transformation Parameters
   */
  public OceanSigma(NetcdfFile ds, Dimension timeDim, List<Parameter> params) {

    super(timeDim);
    String etaName = getParameterStringValue(params, ETA);
    String sName = getParameterStringValue(params, SIGMA);
    String depthName = getParameterStringValue(params, DEPTH);

    etaVar = ds.findVariable(etaName);
    sVar = ds.findVariable(sName);
    depthVar = ds.findVariable(depthName);

    units = ds.findAttValueIgnoreCase(depthVar, "units", "none");
  }

  /**
   * Get the 3D vertical coordinate array for this time step.
   *
   * @param timeIndex the time index. Ignored if !isTimeDependent().
   * @return vertical coordinate array
   * @throws IOException           problem reading data
   * @throws InvalidRangeException _more_
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

          height.set(z, y, x,
              etaVal + sigmaVal * (depthVal + etaVal));
        }
      }
    }

    return height;
  }
}

