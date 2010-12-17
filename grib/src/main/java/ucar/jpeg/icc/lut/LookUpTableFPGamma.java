/*****************************************************************************
 *
 * $Id: LookUpTableFPGamma.java,v 1.1 2002/07/25 14:56:48 grosbois Exp $
 *
 * Copyright Eastman Kodak Company, 343 State Street, Rochester, NY 14650
 * $Date $
 *****************************************************************************/

package ucar.jpeg.icc.lut;

import ucar.jpeg.icc .tags.ICCCurveType;

/**
 * Class Description
 * 
 * @version	1.0
 * @author	Bruce A. Kern
 */

public class LookUpTableFPGamma extends LookUpTableFP {     

    double dfE = -1;
    private static final String eol = System.getProperty("line.separator");

    public LookUpTableFPGamma (
                 ICCCurveType curve,   // Pointer to the curve data            
                 int dwNumInput       // Number of input values in created LUT
                 ) {
        super (curve, dwNumInput); 

        // Gamma exponent for inverse transformation
        dfE = ICCCurveType.CurveGammaToDouble(curve.entry(0));
        for (int i = 0; i < dwNumInput; i++)
            lut[i] = (float) Math.pow((double)i / (dwNumInput - 1), dfE); }

    /**
     * Create an abbreviated string representation of a 16 bit lut.
     * @return the lut as a String
     */
    public String toString () {
        StringBuffer rep = new StringBuffer ("[LookUpTableGamma ");
        int row,col;
        rep .append("dfe= " + dfE);
        rep .append(", nentries= " + lut.length);
        return rep.append("]").toString(); }


    /* end class LookUpTableFPGamma */ }
















