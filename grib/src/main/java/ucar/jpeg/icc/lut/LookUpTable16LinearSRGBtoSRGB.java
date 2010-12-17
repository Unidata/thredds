/*****************************************************************************
 *
 * $Id: LookUpTable16LinearSRGBtoSRGB.java,v 1.1 2002/07/25 14:56:47 grosbois Exp $
 *
 * Copyright Eastman Kodak Company, 343 State Street, Rochester, NY 14650
 * $Date $
 *****************************************************************************/

package ucar.jpeg.icc.lut;

import ucar.jpeg.icc .tags.ICCCurveType;

/**
 * A Linear 16 bit SRGB to SRGB lut
 * 
 * @version	1.0
 * @author	Bruce A. Kern
 */
public class LookUpTable16LinearSRGBtoSRGB extends LookUpTable16 {     
    
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
    public static LookUpTable16LinearSRGBtoSRGB createInstance (
                                                               int wShadowCutoff, 
                                                               double dfShadowSlope,
                                                               int ksRGBLinearMaxValue, 
                                                               double ksRGB8ScaleAfterExp, 
                                                               double ksRGBExponent, 
                                                               double ksRGB8ReduceAfterEx) { 
        return new LookUpTable16LinearSRGBtoSRGB
            ( wShadowCutoff, 
              dfShadowSlope,
              ksRGBLinearMaxValue, 
              ksRGB8ScaleAfterExp, 
              ksRGBExponent, 
              ksRGB8ReduceAfterEx); 
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
    protected LookUpTable16LinearSRGBtoSRGB 
        (
         int wShadowCutoff, 
         double dfShadowSlope,
         int ksRGBLinearMaxValue, 
         double ksRGB8ScaleAfterExp, 
         double ksRGBExponent, 
         double ksRGB8ReduceAfterExp) {

        super (ksRGBLinearMaxValue+1, (short)0);

        int i=-1;
        double dfNormalize = 1.0 / (float)ksRGBLinearMaxValue;

        // Generate the final linear-sRGB to non-linear sRGB LUT
        for (i = 0; i <= wShadowCutoff; i++)
            lut[i] = (byte)Math.floor(dfShadowSlope * (double)i + 0.5);

        // Now calculate the rest
        for (; i <= ksRGBLinearMaxValue; i++)
            lut[i] = (byte)Math.floor(ksRGB8ScaleAfterExp  * 
                                      Math.pow((double)i * dfNormalize, ksRGBExponent) 
                                      - ksRGB8ReduceAfterExp + 0.5); }

    /* end class LookUpTable16LinearSRGBtoSRGB */ }


