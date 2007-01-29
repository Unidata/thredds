/*
 * $Id: HybridSigmaPressure.java,v 1.15 2006/11/18 19:03:31 dmurray Exp $
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
 * Create a 3D height(z,y,x) array using the netCDF CF convention formula for
 * "Hybrid Sigma Pressure".
 * <p><strong>pressure(x,y,z) = a(z)*p0 + b(z)*surfacePressure(x,y)</strong>
 *
 * @author caron
 * @see "http://www.cgd.ucar.edu/cms/eaton/cf-metadata"
 */
public class HybridSigmaPressure extends VerticalTransformImpl {

    /**
     * P-naught identifier
     */
    public static final String P0 = "P0_variableName";

    /**
     * Surface pressure name identifier
     */
    public static final String PS = "SurfacePressure_variableName";

    /**
     * The "a" variable name identifier
     */
    public static final String A = "A_variableName";

    /**
     * The "b" variable name identifier
     */
    public static final String B = "B_variableName";

    /**
     * value of p-naught
     */
    private double p0;

    /**
     * ps, a, and b variables
     */
    private Variable psVar, aVar, bVar, p0Var;

    /**
     * a and b Arrays
     */
    private Array aArray = null,
                  bArray = null;

    /**
     * Construct a coordinate transform for sigma pressure
     *
     * @param ds      netCDF dataset
     * @param timeDim time dimension
     * @param vCT     vertical coordinate transform
     */
    public HybridSigmaPressure(NetcdfDataset ds, Dimension timeDim,
                               VerticalCT vCT) {

        super(timeDim);
        String psName = vCT.findParameterIgnoreCase(PS).getStringValue();
        String aName  = vCT.findParameterIgnoreCase(A).getStringValue();
        String bName  = vCT.findParameterIgnoreCase(B).getStringValue();
        String p0Name = vCT.findParameterIgnoreCase(P0).getStringValue();

        psVar = ds.findStandardVariable(psName);
        aVar  = ds.findStandardVariable(aName);
        bVar  = ds.findStandardVariable(bName);
        p0Var = ds.findStandardVariable(p0Name);

        units = ds.findAttValueIgnoreCase(psVar, "units", "none");
    }

    /**
     * Get the 3D vertical coordinate array for this time step.
     *
     * @param timeIndex the time index. Ignored if !isTimeDependent().
     * @return vertical coordinate array
     * @throws IOException problem reading data
     * @throws InvalidRangeException _more_
     */
    public ArrayDouble.D3 getCoordinateArray(int timeIndex)
            throws IOException, InvalidRangeException {
        Array psArray = readArray(psVar, timeIndex);

        if (null == aArray) {
            aArray = aVar.read();
            bArray = bVar.read();
            p0     = p0Var.readScalarDouble();
        }

        int            nz      = (int) aArray.getSize();
        Index          aIndex  = aArray.getIndex();
        Index          bIndex  = bArray.getIndex();

        int[]          shape2D = psArray.getShape();
        int            ny      = shape2D[0];
        int            nx      = shape2D[1];
        Index          psIndex = psArray.getIndex();

        ArrayDouble.D3 press   = new ArrayDouble.D3(nz, ny, nx);

        for (int z = 0; z < nz; z++) {
            double term1 = aArray.getDouble(aIndex.set(z)) * p0;
            double bz    = bArray.getDouble(bIndex.set(z));

            for (int y = 0; y < ny; y++) {
                for (int x = 0; x < nx; x++) {
                    double ps = psArray.getDouble(psIndex.set(y, x));
                    press.set(z, y, x, term1 + bz * ps);
                }
            }
        }

        return press;
    }

}

