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

import ucar.nc2.Variable;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Dimension;
import ucar.unidata.util.Parameter;
import ucar.ma2.*;

import java.util.List;
import java.io.IOException;

/**
 * Implement CF vertical coordinate "atmosphere_ln_pressure_coordinate"
 * pressure(z) = p0 * exp(-lev(k))" .
 *
 * Theres a problem here, since its not 3D, we dont know what the 2D extent is.

 * @author caron
 * @see <a href="http://cf-pcmdi.llnl.gov/">http://cf-pcmdi.llnl.gov/</a>
 * @since May 6, 2008
 */
public class AtmosLnPressure extends VerticalTransformImpl {
  public static final String P0 = "ReferencePressureVariableName";
  public static final String LEV = "VerticalCoordinateVariableName";

  private Array pressure;
  private double p0;

  /**
   * Create a new vertical transform for Ocean S coordinates
   *
   * @param ds      dataset
   * @param timeDim time dimension
   * @param params  list of transformation Parameters
   */
  public AtmosLnPressure(NetcdfFile ds, Dimension timeDim, List<Parameter> params) {
    super(timeDim);

    String p0name = getParameterStringValue(params, P0);
    Variable p0var = ds.findVariable(p0name);
    try {
      this.p0 = p0var.readScalarDouble();
    } catch (IOException e) {
      throw new IllegalArgumentException("AtmosLnPressure failed to read "
          + p0name + " err= " + e.getMessage());
    }

    String levName = getParameterStringValue(params, LEV);
    Variable levVar = ds.findVariable(levName);

    try {
      Array lev = levVar.read();
      assert lev.getRank() == 1;
      ArrayDouble.D1 pressure = new ArrayDouble.D1( (int) lev.getSize());
      IndexIterator ii = pressure.getIndexIterator();
      while (lev.hasNext()) {
        double result = p0 * Math.exp(-lev.nextDouble());
        ii.setDoubleNext( result );
      }

    } catch (IOException e) {
      throw new IllegalArgumentException("AtmosLnPressure failed to read "
          + levName + " err= " + e.getMessage());
    }

    units = p0var.getUnitsString();
  }

  /**
   * Get the 3D vertical coordinate array for this time step.
   *
   * @param timeIndex the time index. Ignored if !isTimeDependent().
   * @return vertical coordinate array
   * @throws IOException           problem reading data
   * @throws ucar.ma2.InvalidRangeException _more_
   */
  public ArrayDouble.D3 getCoordinateArray(int timeIndex) throws IOException, InvalidRangeException {
    // LOOK! we dont know what size to make this !!
    Array ps = null; // readArray(psVar, timeIndex);
    Index psIndex = ps.getIndex();

    int nz = (int) pressure.getSize();
    int[] shape2D = ps.getShape();
    int ny = shape2D[0];
    int nx = shape2D[1];

    ArrayDouble.D3 result = new ArrayDouble.D3(nz, ny, nx);

    IndexIterator ii = pressure.getIndexIterator();
    for (int z = 0; z < nz; z++) {
      double zcoord = ii.getDoubleNext();
      for (int y = 0; y < ny; y++) {
        for (int x = 0; x < nx; x++) {
          result.set(z, y, x, zcoord);
        }
      }
    }

    return result;
  }
}


