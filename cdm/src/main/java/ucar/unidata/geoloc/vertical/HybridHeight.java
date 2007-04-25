/*
 * $Id: HybridHeight.java,v 1.1 2007/04/23 22:21:57 dmurray Exp $
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
 * "Atmospheric Hybrid Height".
 * <p><strong>height(x,y,z) = a(z) + b(z)*orog(x,y)</strong>
 *
 * @author murray
 * @see "http://www.cgd.ucar.edu/cms/eaton/cf-metadata"
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
     * @param vCT     vertical coordinate transform
     */
    public HybridHeight(NetcdfDataset ds, Dimension timeDim, VerticalCT vCT) {

        super(timeDim);
        String aName    = vCT.findParameterIgnoreCase(A).getStringValue();
        String bName    = vCT.findParameterIgnoreCase(B).getStringValue();
        String orogName = vCT.findParameterIgnoreCase(OROG).getStringValue();

        aVar    = ds.findStandardVariable(aName);
        bVar    = ds.findStandardVariable(bName);
        orogVar = ds.findStandardVariable(orogName);
        units   = ds.findAttValueIgnoreCase(orogVar, "units", "none");

    }

    /**
     * Get the 3D vertical coordinate array for this time step.
     *
     * @param timeIndex the time index. Ignored if !isTimeDependent().
     * @return vertical coordinate array
     * @throws IOException problem reading data
     * @throws InvalidRangeException not a valid time range
     */
    public ArrayDouble.D3 getCoordinateArray(int timeIndex)
            throws IOException, InvalidRangeException {
        Array orogArray = readArray(orogVar, timeIndex);

        if (null == aArray) {
            aArray = aVar.read();
            bArray = bVar.read();
        }

        int            nz        = (int) aArray.getSize();
        Index          aIndex    = aArray.getIndex();
        Index          bIndex    = bArray.getIndex();

        int[]          shape2D   = orogArray.getShape();
        int            ny        = shape2D[0];
        int            nx        = shape2D[1];
        Index          orogIndex = orogArray.getIndex();

        ArrayDouble.D3 height    = new ArrayDouble.D3(nz, ny, nx);

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

