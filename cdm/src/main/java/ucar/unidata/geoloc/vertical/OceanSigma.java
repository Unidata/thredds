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

import ucar.unidata.util.SpecialMathFunction;

import java.io.IOException;


/**
 * Create a 3D height(z,y,x) array using the CF formula for
 * "ocean sigma vertical coordinate".
 *
 * @author  Unidata Development Team
 * @version $Revision: 64 $
 */

public class OceanSigma extends VerticalTransformImpl {

    /** The eta variable name identifier */
    public static final String ETA = "eta variable name";

    /** The "s" variable name identifier */
    public static final String SIGMA = "sigma variable name";

    /** The "depth" variable name identifier */
    public static final String DEPTH = "depth variable name";

    /** the names of the eta, s and depth variable */
    private String etaName, sName, depthName;

    /** The eta, s and depth variables */
    private Variable etaVar, sVar, depthVar;

    /**
     * Create a new vertical transform for Ocean S coordinates
     *
     * @param ds           dataset
     * @param timeDim      time dimension
     * @param vCT          vertical coordinate transform
     *
     */
    public OceanSigma(NetcdfDataset ds, Dimension timeDim, VerticalCT vCT) {

        super(timeDim);
        this.etaName   = vCT.findParameterIgnoreCase(ETA).getStringValue();
        this.sName     = vCT.findParameterIgnoreCase(SIGMA).getStringValue();
        this.depthName = vCT.findParameterIgnoreCase(DEPTH).getStringValue();

        etaVar         = ds.findStandardVariable(etaName);
        sVar           = ds.findStandardVariable(sName);
        depthVar       = ds.findStandardVariable(depthName);

        units          = ds.findAttValueIgnoreCase(depthVar, "units", "none");
    }

    /**
     * Get the 3D vertical coordinate array for this time step.
     *
     * @param timeIndex the time index. Ignored if !isTimeDependent().
     * @return  vertical coordinate array
     *
     * @throws IOException  problem reading data
     */
    public ArrayDouble.D3 getCoordinateArray(int timeIndex)
            throws IOException, InvalidRangeException {
        Array etaArray   = readArray(etaVar, timeIndex);
        Array sArray     = readArray(sVar, timeIndex);
        Array depthArray = readArray(depthVar, timeIndex);

        return makeHeight(etaArray, sArray, depthArray);
    }

    /**
     * Make height from the given data. <br>
     * height(x,y,z) = eta(x,y) + sigma(z) * (depth(x,y) + eta(x,y))
     *
     * @param eta        eta Array
     * @param sigma      sigma Array
     * @param depth      depth Array
     *
     * @return height, relative to ocean datum (eg mean sea level)
     */
    private ArrayDouble.D3 makeHeight(Array eta, Array sigma, Array depth) {
        int            nz          = (int) sigma.getSize();
        Index          sIndex      = sigma.getIndex();

        int[]          shape2D     = eta.getShape();
        int            ny          = shape2D[0];
        int            nx          = shape2D[1];
      Index          etaIndex    = eta.getIndex();

        ArrayDouble.D3 height      = new ArrayDouble.D3(nz, ny, nx);
        Index          heightIndex = height.getIndex();

        for (int z = 0; z < nz; z++) {
            double sigmaVal = sigma.getDouble(sIndex.set(z));
            for (int y = 0; y < ny; y++) {
                for (int x = 0; x < nx; x++) {
                    double etaVal   = eta.getDouble(etaIndex.set(y, x));
                    double depthVal = depth.getDouble(etaIndex);  // LOOK use etaIndex

                    height.setDouble(heightIndex.set(z, y, x),
                                     etaVal + sigmaVal * (depthVal + etaVal));
                }
            }
        }

        return height;
    }
}

/* Change History:
   $Log: OceanSigma.java,v $
   Revision 1.5  2006/05/25 20:15:29  caron
   CF-1: assignment of vertical transforms may require time axis in coordinate system.
   CoordSysBuilder: improve info messages on CoordTrans assignments

   Revision 1.4  2006/02/07 13:28:53  caron
   VerticalTransform subsetting

   Revision 1.3  2005/08/11 22:42:13  dmurray
   jindent (I'll leave the javadoc to those who forgot to)

   Revision 1.2  2005/05/13 18:29:24  jeffmc
   Clean up the odd copyright symbols

   Revision 1.1  2005/02/18 01:12:54  caron
   add Ocean Sigma vertical transform

*/
