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
 * Create a 3D height(z,y,x) array using the netCDF CF convention formula for
 * "Atmospheric Hybrid Height".
 * <p><strong>height(x,y,z) = a(z) + b(z)*orog(x,y)</strong>
 *
 * @author murray
 * @see <a href="http://cf-pcmdi.llnl.gov/">http://cf-pcmdi.llnl.gov/</a>
 */
public class HybridHeight extends VerticalTransformImpl {

  /**
   * Surface pressure name identifier
   */
  public static final String OROG = "Orography_variableName";

  /**
   * The "a" variable name identifier
   */
  public static final String A = "A_variableName";

  /**
   * The "b" variable name identifier
   */
  public static final String B = "B_variableName";

  /**
   * ps, a, and b variables
   */
  private Variable aVar, bVar, orogVar;

  /**
   * a and b Arrays
   */
  private Array aArray = null,
      bArray = null;

  /**
   * Construct a coordinate transform for hybrid height
   *
   * @param ds      netCDF dataset
   * @param timeDim time dimension
   * @param params  list of transformation Parameters
   */
  public HybridHeight(NetcdfFile ds, Dimension timeDim, List<Parameter> params) {

    super(timeDim);
    String aName = getParameterStringValue(params, A);
    String bName = getParameterStringValue(params, B);
    String orogName = getParameterStringValue(params, OROG);

    aVar = ds.findVariable(aName);
    bVar = ds.findVariable(bName);
    orogVar = ds.findVariable(orogName);
    units = ds.findAttValueIgnoreCase(orogVar, "units", "none");

  }

  /**
   * Get the 3D vertical coordinate array for this time step.
   *
   * @param timeIndex the time index. Ignored if !isTimeDependent().
   * @return vertical coordinate array
   * @throws IOException           problem reading data
   * @throws InvalidRangeException not a valid time range
   */
  public ArrayDouble.D3 getCoordinateArray(int timeIndex)
      throws IOException, InvalidRangeException {
    Array orogArray = readArray(orogVar, timeIndex);

    if (null == aArray) {
      aArray = aVar.read();
      bArray = bVar.read();
    }

    int nz = (int) aArray.getSize();
    Index aIndex = aArray.getIndex();
    Index bIndex = bArray.getIndex();

    int[] shape2D = orogArray.getShape();
    int ny = shape2D[0];
    int nx = shape2D[1];
    Index orogIndex = orogArray.getIndex();

    ArrayDouble.D3 height = new ArrayDouble.D3(nz, ny, nx);

    for (int z = 0; z < nz; z++) {
      double az = aArray.getDouble(aIndex.set(z));
      double bz = bArray.getDouble(bIndex.set(z));

      for (int y = 0; y < ny; y++) {
        for (int x = 0; x < nx; x++) {
          double orog = orogArray.getDouble(orogIndex.set(y, x));
          height.set(z, y, x, az + bz * orog);
        }
      }
    }

    return height;
  }

}

