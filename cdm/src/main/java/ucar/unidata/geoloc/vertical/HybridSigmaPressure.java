/*
 * $Id:HybridSigmaPressure.java 63 2006-07-12 21:50:51Z edavis $
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


import ucar.nc2.*;
import ucar.nc2.dataset.*;

import ucar.ma2.*;

import java.io.IOException;


/**
 * Create a 3D height(z,y,x) array using the netCDF CF convention formula for
 * "Hybrid Sigma Pressure".
 * <p><strong>pressure(x,y,z) = a(z)*p0 + b(z)*surfacePressure(x,y)</strong>
 *
 * @author  Unidata Development Team
 * @version $Revision:63 $
 * @see "http://www.cgd.ucar.edu/cms/eaton/cf-metadata"
 */
public class HybridSigmaPressure extends VerticalTransformImpl {

    /** P-naught identifier */
    public static final String P0 = "P0";

    /** Surface pressure name identifier */
    public static final String PS = "surfacePressure variable name";

    /** The "a" variable name identifier */
    public static final String A = "a variable name";

    /** The "b" variable name identifier */
    public static final String B = "b variable name";

    /** name of the ps, a and b variables */
    private String psName, aName, bName;

    /** value of p-naught */
    private double p0;

    /** ps, a, and b variables */
    private Variable psVar, aVar, bVar;

    /** a and b Arrays */
    private Array aArray = null,
                  bArray = null;

    /**
     * Construct a coordinate transform for sigma pressure
     *
     * @param ds         netCDF dataset
     * @param timeDim    time dimension
     * @param vCT        vertical coordinate transform
     */
    public HybridSigmaPressure(NetcdfDataset ds, Dimension timeDim,
                               VerticalCT vCT) {

        super(timeDim);
        this.psName = vCT.findParameterIgnoreCase(PS).getStringValue();
        this.aName  = vCT.findParameterIgnoreCase(A).getStringValue();
        this.bName  = vCT.findParameterIgnoreCase(B).getStringValue();
        this.p0     = vCT.findParameterIgnoreCase(P0).getNumericValue();

        psVar       = ds.findStandardVariable(psName);
        aVar        = ds.findStandardVariable(aName);
        bVar        = ds.findStandardVariable(bName);

        units       = ds.findAttValueIgnoreCase(psVar, "units", "none");
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
        Array psArray = readArray(psVar, timeIndex);

        if (null == aArray) {
            aArray = aVar.read();
        }
        if (null == bArray) {
            bArray = bVar.read();
        }
        return makePressure(aArray, bArray, psArray, p0);
    }

    // pressure(x,y,z) = a(z)*p0 + b(z)*surfacePressure(x,y)

    /**
     * Make a pressure array from the variables
     *
     * @param a           "a" variable
     * @param b           "b" variable
     * @param ps          surface pressure variable
     * @param p0          p-naught variable
     * @return data
     */
    private ArrayDouble.D3 makePressure(Array a, Array b, Array ps,
                                        double p0) {
        int            nz         = (int) a.getSize();
        Index          aIndex     = a.getIndex();
        Index          bIndex     = a.getIndex();

        int[]          shape2D    = ps.getShape();
        int            ny         = shape2D[0];
        int            nx         = shape2D[0];
        Index          psIndex    = ps.getIndex();

        ArrayDouble.D3 press      = new ArrayDouble.D3(nz, ny, nx);
        Index          pressIndex = press.getIndex();

        for (int z = 0; z < nz; z++) {
            double term1 = a.getDouble(aIndex.set(z)) * p0;
            double bz    = b.getDouble(bIndex.set(z));

            for (int y = 0; y < ny; y++) {
                for (int x = 0; x < nx; x++) {
                    double term2 = ps.getDouble(psIndex.set(y, x)) * bz;
                    press.setDouble(pressIndex.set(z, y, x), term1 + term2);
                }
            }
        }

        return press;
    }

}

/* Change History:
   $Log: HybridSigmaPressure.java,v $
   Revision 1.13  2006/02/07 13:28:53  caron
   VerticalTransform subsetting

   Revision 1.12  2005/05/13 18:29:24  jeffmc
   Clean up the odd copyright symbols

   Revision 1.11  2004/11/04 20:07:48  caron
   move readArray() up to superclass.

   Revision 1.10  2004/10/28 23:56:32  caron
   use findStandardVariable for geoloc.vertical

   Revision 1.9  2004/10/25 23:08:18  caron
   use VariableStandardized to get scale/offset handling

   Revision 1.8  2004/10/21 17:08:43  dmurray
   refactor.  Create VerticalTransformImpl to handle timeDim and units
   and have others extend this.

   Revision 1.7  2004/09/22 21:19:29  caron
   use Parameter, not Attribute

   Revision 1.6  2004/07/30 17:22:23  dmurray
   Jindent and doclint

   Revision 1.5  2004/07/30 15:24:40  dmurray
   add javadocs.  If I'm wanting Doug to do it, I guess I'd better give
   examples (even if I didn't write the code)

   Revision 1.4  2004/02/27 21:21:46  jeffmc
   Lots of javadoc warning fixes

   Revision 1.3  2004/01/29 17:35:11  jeffmc
   A big sweeping checkin after a big sweeping reformatting
   using the new jindent.

   jindent adds in javadoc templates and reformats existing javadocs. In the new javadoc
   templates there is a '_more_' to remind us to fill these in.

   Revision 1.2  2003/07/12 23:09:02  caron
   add cvs headers, trailers

*/
