/*
 * $Id: OceanS.java,v 1.17 2006/11/18 19:03:32 dmurray Exp $
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

import ucar.unidata.util.Parameter;

import java.io.IOException;
import java.util.List;


/**
 * Create a 3D height(z,y,x) array using the CF formula for
 * "ocean s vertical coordinate g2".
 * standard name: ocean_s_coordinate_g2
 *
 * @author Sachin (skbhate@ngi.msstate.edu)
 * @see <a href="http://cf-pcmdi.llnl.gov/">http://cf-pcmdi.llnl.gov/</a>
 * @see <a href="https://www.myroms.org/wiki/index.php/Vertical_S-coordinate#Metadata_Considerations">https://www.myroms.org/wiki/index.php/Vertical_S-coordinate#Metadata_Considerations</a>
 */

public class OceanSG2 extends VerticalTransformImpl {

    /**
     * The eta variable name identifier
     */
    public static final String ETA = "Eta_variableName";

    /**
     * The "s" variable name identifier
     */
    public static final String S = "S_variableName";

    /**
     * The "depth" variable name identifier
     */
    public static final String DEPTH = "Depth_variableName";

    /**
     * The "depth c" variable name identifier
     */
    public static final String DEPTH_C = "Depth_c_variableName";

    /**
     * The "C" variable name identifier
     */
    public static final String C = "c_variableName";
    /**
     * the values of depth_c
     */
    private double depth_c;

    /**
     * The eta, s, C and depth variables
     */
    private Variable etaVar, sVar, depthVar, cVar, depthCVar;


    /**
     * Create a new vertical transform for Ocean_S_coordinate_g2
     *
     * @param ds      dataset
     * @param timeDim time dimension
     * @param params  list of transformation Parameters
     */
    public OceanSG2(NetcdfFile ds, Dimension timeDim, List<Parameter> params) {

        super(timeDim);
        String etaName = getParameterStringValue(params, ETA);
        String sName = getParameterStringValue(params, S);
        String depthName = getParameterStringValue(params, DEPTH);
        String depthCName = getParameterStringValue(params, DEPTH_C);
        String cName = getParameterStringValue(params, C);

        etaVar    = ds.findVariable(etaName);
        sVar      = ds.findVariable(sName);
        depthVar  = ds.findVariable(depthName);
        depthCVar = ds.findVariable(depthCName);
        cVar = ds.findVariable(cName);

        units     = ds.findAttValueIgnoreCase(depthVar, "units", "none");
    }

    /**
     * Get the 3D vertical coordinate array for this time step.
     *
     * @param timeIndex the time index. Ignored if !isTimeDependent().
     * @return vertical coordinate array
     * @throws java.io.IOException problem reading data
     * @throws ucar.ma2.InvalidRangeException _more_
     */
   public ArrayDouble.D3 getCoordinateArray(int timeIndex)
            throws IOException, InvalidRangeException {
        Array etaArray   = readArray(etaVar, timeIndex);
        Array sArray     = readArray(sVar, timeIndex);
        Array depthArray = readArray(depthVar, timeIndex);
        Array cArray = readArray(cVar, timeIndex);

        depth_c = depthCVar.readScalarDouble();

        return makeHeight(etaArray, sArray, depthArray, cArray, depth_c);
    }

    /** Add new method for retrieving 1D Z values for specified indices for lat/lon. */

    /**
        * Get the 1D vertical coordinate array for this time step and
        * the specified X,Y index for Lat-Lon point.
        *
        * @param timeIndex the time index. Ignored if !isTimeDependent().
        * @param xIndex    the x index
        * @param yIndex    the y index
        * @return vertical coordinate array
        * @throws java.io.IOException problem reading data
        * @throws ucar.ma2.InvalidRangeException _more_
        */
      public ArrayDouble.D1 getCoordinateArray1D(int timeIndex, int xIndex, int yIndex)
               throws IOException, InvalidRangeException {
           Array etaArray   = readArray(etaVar, timeIndex);
           Array sArray     = readArray(sVar, timeIndex);
           Array depthArray = readArray(depthVar, timeIndex);
           Array cArray = readArray(cVar, timeIndex);

           depth_c = depthCVar.readScalarDouble();

           return makeHeight1D(etaArray, sArray, depthArray, cArray, depth_c, xIndex, yIndex);
       }

    /**
     * Make height from the given data. <br>
     *
     *
     * height(x,y,z) = eta(x,y) + ( eta(x,y) + depth([n],x,y) ) *  S(x,y,z)
     *
     * where,
     * S(x,y,z) = (depth_c*s(z) + (depth([n],x,y) * C(z)) / (depth_c + depth([n],x,y))
     * /
     *
     * @param eta     eta Array
     * @param s       s Array
     * @param depth   depth Array
     * @param c       c Array
     * @param depth_c value of depth_c
     * @return height data
     */
   private ArrayDouble.D3 makeHeight(Array eta, Array s, Array depth,
                                      Array c, double depth_c) {
        int            nz         = (int) s.getSize();
        Index          sIndex     = s.getIndex();
        Index          cIndex     = c.getIndex();

        int[]          shape2D    = eta.getShape();
        int            ny         = shape2D[0];
        int            nx         = shape2D[1];
        Index          etaIndex   = eta.getIndex();
        Index          depthIndex = depth.getIndex();

        ArrayDouble.D3 height     = new ArrayDouble.D3(nz, ny, nx);

        for (int z = 0; z < nz; z++) {
            double sz    = s.getDouble(sIndex.set(z));
            double cz    = c.getDouble(cIndex.set(z));

            double term1 = depth_c * sz;

            for (int y = 0; y < ny; y++) {
                for (int x = 0; x < nx; x++) {

                    double fac1 = depth.getDouble(depthIndex.set(y, x));
                    double term2 = fac1 * cz;

                    double Sterm = (term1 + term2)/(depth_c + fac1);

                    double term3 = eta.getDouble(etaIndex.set(y, x));
                    double term4 = (term3 + fac1) * Sterm;
                    double hterm = term3 + term4;

                    height.set(z, y, x,hterm);

                }
            }
        }

        return height;
    }

    
  private ArrayDouble.D1 makeHeight1D(Array eta, Array s, Array depth,
                                      Array c, double depth_c, int x_index, int y_index) {
        int            nz         = (int) s.getSize();
        Index          sIndex     = s.getIndex();
        Index          cIndex     = c.getIndex();


        Index          etaIndex   = eta.getIndex();
        Index          depthIndex = depth.getIndex();

       ArrayDouble.D1 height     = new ArrayDouble.D1(nz);

        for (int z = 0; z < nz; z++) {
            double sz    = s.getDouble(sIndex.set(z));
            double cz    = c.getDouble(cIndex.set(z));

            double term1 = depth_c * sz;

            double fac1 = depth.getDouble(depthIndex.set(y_index, x_index));
            double term2 = fac1 * cz;

            double Sterm = (term1 + term2)/(depth_c + fac1);

            double term3 = eta.getDouble(etaIndex.set(y_index, x_index));
            double term4 = (term3 + fac1) * Sterm;
            double hterm = term3 + term4;

            height.set(z,hterm);             

        }

        return height;
    }
}