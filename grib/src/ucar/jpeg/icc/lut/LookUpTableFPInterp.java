/*****************************************************************************
 *
 * $Id: LookUpTableFPInterp.java,v 1.1 2002/07/25 14:56:48 grosbois Exp $
 *
 * Copyright Eastman Kodak Company, 343 State Street, Rochester, NY 14650
 * $Date $
 *****************************************************************************/

package ucar.jpeg.icc.lut;

import ucar.jpeg.icc .tags.ICCCurveType;

/**
 * An interpolated floating point lut
 * 
 * @version	1.0
 * @author	Bruce A.Kern
 */
public class LookUpTableFPInterp extends LookUpTableFP {     
    
    /**
     * Create an abbreviated string representation of a 16 bit lut.
     * @return the lut as a String
     */
    public String toString () {
        StringBuffer 
            rep = new StringBuffer ("[LookUpTable32 ")
                .append(" nentries= " + lut.length);
        return rep.append("]").toString(); }

    /**
     * Construct the lut from the curve data
     *   @oaram  curve the data
     *   @oaram  dwNumInput the lut size
     */
    public LookUpTableFPInterp (
                 ICCCurveType curve,   // Pointer to the curve data            
                 int dwNumInput       // Number of input values in created LUT
                 ) {
        super (curve, dwNumInput);

        int    dwLowIndex, dwHighIndex;		    // Indices of interpolation points
        double dfLowIndex, dfHighIndex;			// FP indices of interpolation points
        double dfTargetIndex;					// Target index into interpolation table
        double dfRatio;							// Ratio of LUT input points to curve values
        double dfLow, dfHigh;					// Interpolation values
	
        dfRatio = (double)(curve.nEntries-1) / (double)(dwNumInput-1);   

        for (int i = 0; i < dwNumInput; i++) {
            dfTargetIndex = (double) i * dfRatio;
            dfLowIndex    = Math.floor(dfTargetIndex);
            dwLowIndex    = (int) dfLowIndex;
            dfHighIndex   = Math.ceil(dfTargetIndex);
            dwHighIndex   = (int) dfHighIndex;   
            if (dwLowIndex == dwHighIndex)  lut[i] = (float) ICCCurveType.CurveToDouble(curve.entry(dwLowIndex));
            else {  
                dfLow  = ICCCurveType.CurveToDouble(curve.entry(dwLowIndex));
                dfHigh = ICCCurveType.CurveToDouble(curve.entry(dwHighIndex));
                lut[i]  = (float) (dfLow + (dfHigh - dfLow) * (dfTargetIndex - dfLowIndex)); }}}

    /* end class LookUpTableFPInterp */ }














