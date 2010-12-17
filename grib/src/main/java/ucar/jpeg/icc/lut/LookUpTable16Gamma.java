/*****************************************************************************
 *
 * $Id: LookUpTable16Gamma.java,v 1.1 2002/07/25 14:56:46 grosbois Exp $
 *
 * Copyright Eastman Kodak Company, 343 State Street, Rochester, NY 14650
 * $Date $
 *****************************************************************************/

package ucar.jpeg.icc.lut;

import ucar.jpeg.icc .tags.ICCCurveType;

/**
 * A Gamma based 16 bit lut.
 * 
 * @see		jj2000.j2k.icc.tags.ICCCurveType
 * @version	1.0
 * @author	Bruce A. Kern
 */
public class LookUpTable16Gamma extends LookUpTable16 {     
    
    /* Construct the lut 
     *   @param curve data 
     *   @param dwNumInput size of lut 
     *   @param dwMaxOutput max value of lut   
     */
    public LookUpTable16Gamma (ICCCurveType curve, int dwNumInput, int dwMaxOutput) {
        super (curve, dwNumInput, dwMaxOutput);
        double dfE = ICCCurveType.CurveGammaToDouble(curve.entry(0)); // Gamma exponent for inverse transformation
        for (int i = 0; i < dwNumInput; i++)
            lut[i] = (short) Math.floor(Math.pow((double)i / (dwNumInput - 1), dfE) * dwMaxOutput + 0.5); }

    /* end class LookUpTable16Gamma */ }
















