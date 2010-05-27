/*****************************************************************************
 *
 * $Id: LookUpTable32LinearSRGBtoSRGB.java,v 1.1 2002/07/25 14:56:47 grosbois Exp $
 *
 * Copyright Eastman Kodak Company, 343 State Street, Rochester, NY 14650
 * $Date $
 *****************************************************************************/

package ucar.jpeg.icc.lut;

import ucar.jpeg.icc .tags.ICCCurveType;

/**
 * A Linear 32 bit SRGB to SRGB lut
 * 
 * @version	1.0
 * @author	Bruce A. Kern
 */
public class LookUpTable32LinearSRGBtoSRGB extends LookUpTable32 {     
    
    /**
     * Factory method for creating the lut.
     *   @param wShadowCutoff size of shadow region
     *   @param dfShadowSlope shadow region parameter
     *   @param ksRGBLinearMaxValue size of lut
     *   @param ksRGB8ScaleAfterExp post shadow region parameter
     *   @param ksRGBExponent post shadow region parameter
     *   @param ksRGB8ReduceAfterEx post shadow region parameter
     * @return the lut
     */
    public static LookUpTable32LinearSRGBtoSRGB createInstance (
                                                                int inMax,
                                                                int outMax,
                                                                double shadowCutoff, 
                                                                double shadowSlope,
                                                                double scaleAfterExp, 
                                                                double exponent, 
                                                                double reduceAfterExp) {
        return new LookUpTable32LinearSRGBtoSRGB
            (inMax, outMax,
             shadowCutoff, shadowSlope,
             scaleAfterExp, exponent, reduceAfterExp); 
    }
    
    /**
     * Construct the lut
     *   @param wShadowCutoff size of shadow region
     *   @param dfShadowSlope shadow region parameter
     *   @param ksRGBLinearMaxValue size of lut
     *   @param ksRGB8ScaleAfterExp post shadow region parameter
     *   @param ksRGBExponent post shadow region parameter
     *   @param ksRGB8ReduceAfterExp post shadow region parameter
     */    
    protected LookUpTable32LinearSRGBtoSRGB 
        (
         int inMax,
         int outMax,
         double shadowCutoff, 
         double shadowSlope,
         double scaleAfterExp, 
         double exponent, 
         double reduceAfterExp) {

        super (inMax+1, outMax);

        int i=-1;
        // Normalization factor for i.
        double normalize = 1.0 / (double)inMax;

        // Generate the final linear-sRGB to non-linear sRGB LUT    

        // calculate where shadow portion of lut ends.
        int cutOff = (int)Math.floor(shadowCutoff*inMax);

        // Scale to account for output
        shadowSlope *= outMax;

        // Our output needs to be centered on zero so we shift it down.
        int shift = (outMax+1)/2;

        for (i = 0; i <= cutOff; i++)
            lut[i] = (int)(Math.floor(shadowSlope*(i*normalize) + 0.5) - shift);

        // Scale values for output.
        scaleAfterExp  *= outMax;
        reduceAfterExp *= outMax;

        // Now calculate the rest
        for (; i <= inMax; i++)
            lut[i] = (int)(Math.floor(scaleAfterExp  * 
                                      Math.pow(i*normalize, exponent) 
                                      - reduceAfterExp + 0.5) - shift);  
    }

    public String toString () {
        StringBuffer rep = new StringBuffer("[LookUpTable32LinearSRGBtoSRGB:");
        return rep.append("]").toString();
    }

    /* end class LookUpTable32LinearSRGBtoSRGB */ }









