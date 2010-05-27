/*****************************************************************************
 *
 * $Id: LookUpTableFP.java,v 1.1 2002/07/25 14:56:49 grosbois Exp $
 *
 * Copyright Eastman Kodak Company, 343 State Street, Rochester, NY 14650
 * $Date $
 *****************************************************************************/

package ucar.jpeg.icc.lut;

import ucar.jpeg.icc .tags.ICCCurveType;

/**
 * Toplevel class for a float [] lut.
 * 
 * @version	1.0
 * @author	Bruce A. Kern
 */
public abstract class LookUpTableFP extends LookUpTable {     
    
    /** The lut values. */ public final float [] lut;

    /**
     * Factory method for getting a lut from a given curve.
     *   @param curve  the data
     *   @param dwNumInput the size of the lut 
     * @return the lookup table
     */

    public static LookUpTableFP createInstance (
                 ICCCurveType curve,   // Pointer to the curve data            
                 int dwNumInput        // Number of input values in created LUT
                ) {

        if (curve.nEntries == 1) return new LookUpTableFPGamma  (curve, dwNumInput);
        else                     return new LookUpTableFPInterp (curve, dwNumInput); }

    /**
      * Construct an empty lut
      *   @param dwNumInput the size of the lut t lut.
      *   @param dwMaxOutput max output value of the lut
      */
    protected LookUpTableFP (
                 ICCCurveType curve,   // Pointer to the curve data            
                 int dwNumInput       // Number of input values in created LUT
                 ) { 
        super (curve, dwNumInput);
        lut = new float [dwNumInput]; }
    
    /**
     * lut accessor
     *   @param index of the element
     * @return the lut [index]
     */
    public final float elementAt  ( int index ) {
        return lut [index]; }
    
    /* end class LookUpTableFP */ }
















