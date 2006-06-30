/*
 * $Id: OceanS.java,v 1.15 2006/02/07 13:28:53 caron Exp $
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
 * "ocean s vertical coordinate".
 *
 * @author  Unidata Development Team
 * @version $Revision: 1.15 $
 */

public class OceanS extends VerticalTransformImpl {

    /** The eta variable name identifier */
    public static final String ETA = "eta variable name";

    /** The "s" variable name identifier */
    public static final String S = "s variable name";

    /** The "depth" variable name identifier */
    public static final String DEPTH = "depth variable name";

    /** The "depth c" variable name identifier */
    public static final String DEPTH_C = "depth_c";

    /** The "a" variable name */
    public static final String A = "a";

    /** The "b" variable name */
    public static final String B = "b";

    /** the names of the eta, s and depth variable */
    private String etaName, sName, depthName;

    /** the values of depth_c, a, and b */
    private double depth_c, a, b;

    /** The eta, s and depth variables */
    private Variable etaVar, sVar, depthVar;

    /** the c array */
    private Array c = null;

    /**
     * Create a new vertical transform for Ocean S coordinates
     *
     * @param ds           dataset
     * @param timeDim      time dimension
     * @param vCT          vertical coordinate transform
     *
     */
    public OceanS(NetcdfDataset ds, Dimension timeDim, VerticalCT vCT) {

        super(timeDim);
        this.etaName   = vCT.findParameterIgnoreCase(ETA).getStringValue();
        this.sName     = vCT.findParameterIgnoreCase(S).getStringValue();
        this.depthName = vCT.findParameterIgnoreCase(DEPTH).getStringValue();
        this.depth_c = vCT.findParameterIgnoreCase(DEPTH_C).getNumericValue();
        this.a         = vCT.findParameterIgnoreCase(A).getNumericValue();
        this.b         = vCT.findParameterIgnoreCase(B).getNumericValue();

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

        if (null == c) {
            c = makeC(sArray, a, b);
        }
        return makeHeight(etaArray, sArray, depthArray, c, depth_c);
    }



    // C(z) = (1-b)*sinh(a*s(z))/sinh(a) + b*(tanh(a*(s(z)+0.5))/(2*tanh(0.5*a))-0.5)

    /**
     * Make the C array
     *
     * @param s    s Array
     * @param a    "a" value
     * @param b    "b" value
     * @return the C array
     */
    private Array makeC(Array s, double a, double b) {
        int            nz     = (int) s.getSize();
        Index          sIndex = s.getIndex();

        ArrayDouble.D1 c      = new ArrayDouble.D1(nz);
        Index          cIndex = c.getIndex();

        double         fac1   = 1.0 - b;
        double         denom1 = 1.0 / SpecialMathFunction.sinh(a);
        double denom2 = 1.0 / (2.0 * SpecialMathFunction.tanh(0.5 * a));

        for (int i = 0; i < nz; i++) {
            double sz    = s.getDouble(sIndex.set(i));
            double term1 = fac1 * SpecialMathFunction.sinh(a * sz) * denom1;
            double term2 = b * (SpecialMathFunction.tanh(a * (sz + 0.5))
                                * denom2 - 0.5);
            c.setDouble(cIndex.set(i), term1 + term2);
        }

        return c;
    }


    /**
     * Make height from the given data. <br>
     * height(x,y,z) =
     *       eta(x,y)*(1+s(z)) + depth_c*s(z) + (depth(x,y)-depth_c)*C(z)
     *
     * @param eta        eta Array
     * @param s          s Array
     * @param depth      depth Array
     * @param c          c Array
     * @param depth_c    value of depth_c
     * @return hieght data
     */
    private ArrayDouble.D3 makeHeight(Array eta, Array s, Array depth,
                                      Array c, double depth_c) {
        int            nz          = (int) s.getSize();
        Index          sIndex      = s.getIndex();
        Index          cIndex      = c.getIndex();

        int[]          shape2D     = eta.getShape();
        int            ny          = shape2D[0];
        int            nx          = shape2D[1];
        Index          etaIndex    = eta.getIndex();
        Index          depthIndex  = depth.getIndex();

        ArrayDouble.D3 height      = new ArrayDouble.D3(nz, ny, nx);
        Index          heightIndex = height.getIndex();

        for (int z = 0; z < nz; z++) {
            double sz    = s.getDouble(sIndex.set(z));
            double cz    = c.getDouble(cIndex.set(z));
            double fac1  = 1.0 + sz;
            double term2 = depth_c * sz;
            for (int y = 0; y < ny; y++) {
                for (int x = 0; x < nx; x++) {
                    double term1 = fac1 * eta.getDouble(etaIndex.set(y, x));
                    double term3 =
                        (depth.getDouble(depthIndex.set(y, x)) - depth_c)
                        * cz;
                    height.setDouble(heightIndex.set(z, y, x),
                                     term1 + term2 + term3);
                }
            }
        }

        return height;
    }
}

/* Change History:
   $Log: OceanS.java,v $
   Revision 1.15  2006/02/07 13:28:53  caron
   VerticalTransform subsetting

   Revision 1.14  2005/05/13 18:29:24  jeffmc
   Clean up the odd copyright symbols

   Revision 1.13  2004/11/04 20:07:48  caron
   move readArray() up to superclass.

   Revision 1.12  2004/10/28 23:56:32  caron
   use findStandardVariable for geoloc.vertical

   Revision 1.11  2004/10/25 23:08:18  caron
   use VariableStandardized to get scale/offset handling

   Revision 1.10  2004/10/21 17:08:43  dmurray
   refactor.  Create VerticalTransformImpl to handle timeDim and units
   and have others extend this.

   Revision 1.9  2004/10/04 19:38:15  dmurray
   fix bug in calculation of C term (parens in wrong place)

   Revision 1.8  2004/09/22 21:19:30  caron
   use Parameter, not Attribute

   Revision 1.7  2004/07/30 17:22:23  dmurray
   Jindent and doclint

   Revision 1.6  2004/07/30 15:24:40  dmurray
   add javadocs.  If I'm wanting Doug to do it, I guess I'd better give
   examples (even if I didn't write the code)

   Revision 1.5  2004/07/02 18:50:01  caron
   reduce time arrays

   Revision 1.4  2004/02/27 21:21:48  jeffmc
   Lots of javadoc warning fixes

   Revision 1.3  2004/01/29 17:35:11  jeffmc
   A big sweeping checkin after a big sweeping reformatting
   using the new jindent.

   jindent adds in javadoc templates and reformats existing javadocs. In the new javadoc
   templates there is a '_more_' to remind us to fill these in.

   Revision 1.2  2003/07/12 23:09:03  caron
   add cvs headers, trailers

*/
