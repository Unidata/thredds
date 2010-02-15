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
 * "atmospheric sigma vertical coordinate".
 * <p><strong>pressure(x,y,z) = ptop + sigma(z)*surfacePressure(x,y)</strong>
 *
 * @author caron
 * @see <a href="http://cf-pcmdi.llnl.gov/">http://cf-pcmdi.llnl.gov/</a>
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
   * @param params  list of transformation Parameters
   */
  public AtmosSigma(NetcdfFile ds, Dimension timeDim, List<Parameter> params) {
    super(timeDim);

    String psName = getParameterStringValue(params, PS);
    psVar = ds.findVariable(psName);

    String ptopName = getParameterStringValue(params, PTOP);
    Variable ptopVar = ds.findVariable(ptopName);
    try {
      this.ptop = ptopVar.readScalarDouble();
    } catch (IOException e) {
      throw new IllegalArgumentException("AtmosSigma failed to read "
          + ptopVar + " err= " + e.getMessage());
    }

    String sigmaName = getParameterStringValue(params, SIGMA);
    Variable sigmaVar = ds.findVariable(sigmaName);

    try {
      Array data = sigmaVar.read();
      sigma = (double[]) data.get1DJavaArray(double.class);
    } catch (IOException e) {
      throw new IllegalArgumentException("AtmosSigma failed to read "
          + sigmaName + " err= " + e.getMessage());
    }

    units = ds.findAttValueIgnoreCase(psVar, "units", "none");
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
        for (int z = 0; z < nz; z++) {
          result.set(z, y, x, ptop + sigma[z] * (psVal - ptop));
        }
      }
    }

    return result;
  }
}

