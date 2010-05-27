/*****************************************************************************
 *
 * $Id: MonochromeTransformTosRGB.java,v 1.1 2002/07/25 14:56:50 grosbois Exp $
 *
 * Copyright Eastman Kodak Company, 343 State Street, Rochester, NY 14650
 * $Date $
 *****************************************************************************/

package ucar.jpeg.icc.lut;

import java.lang.reflect.Array;
import java.io.PipedOutputStream;
import java.io.PipedInputStream;
import ucar.jpeg.colorspace .ColorSpace;
import ucar.jpeg.icc .ICCProfile;
import ucar.jpeg.icc .RestrictedICCProfile;
import ucar.jpeg.icc .lut.LookUpTableFP;
import ucar.jpeg.jj2000.j2k.image.DataBlkInt;
import ucar.jpeg.jj2000.j2k.image.DataBlkFloat;

/**
 * 
 * This class constructs a LookUpTableFP from a RestrictedICCProfile.
 * The values in this table are used to calculate a second lookup table (simply a short []).  
 * table.  When this transform is applied to an input DataBlk, an output data block is
 * constructed by using the input samples as indices into the lookup table, whose values
 * are used to populate the output DataBlk.
 * 
 * @see		jj2000.j2k.icc.RestrictedICCProfile
 * @see		jj2000.j2k.icc.lut.LookUpTableFP
 * @version	1.0
 * @author	Bruce A. Kern
 */

public class MonochromeTransformTosRGB {

    private static final String eol = System.getProperty ("line.separator");

    /** Transform parameter. */ public final static double ksRGBShadowCutoff    = 0.0031308;
    /** Transform parameter. */ public final static double ksRGBShadowSlope     = 12.92;
    /** Transform parameter. */ public final static double ksRGB8ShadowSlope    = (255 * ksRGBShadowSlope);
    /** Transform parameter. */ public final static double ksRGBExponent        = (1.0 / 2.4);
    /** Transform parameter. */ public final static double ksRGB8ScaleAfterExp  = 269.025;
    /** Transform parameter. */ public final static double ksRGB8ReduceAfterExp = 14.025;

    private short [] lut = null;
    private int dwInputMaxValue = 0;
    private LookUpTableFP fLut = null;


    /**
     * String representation of class
     * @return suitable representation for class 
     */
    public String toString () {
        StringBuffer rep = new StringBuffer ("[MonochromeTransformTosRGB ");
        StringBuffer body = new StringBuffer ("  ");

        // Print the parameters:
        body.append(eol).append("ksRGBShadowSlope= ").append(String.valueOf(ksRGBShadowSlope));
        body.append(eol).append("ksRGBShadowCutoff= ").append(String.valueOf(ksRGBShadowCutoff));
        body.append(eol).append("ksRGBShadowSlope= ").append(String.valueOf(ksRGBShadowSlope));
        body.append(eol).append("ksRGB8ShadowSlope= ").append(String.valueOf(ksRGB8ShadowSlope));
        body.append(eol).append("ksRGBExponent= ").append(String.valueOf(ksRGBExponent));
        body.append(eol).append("ksRGB8ScaleAfterExp= ").append(String.valueOf(ksRGB8ScaleAfterExp));
        body.append(eol).append("ksRGB8ReduceAfterExp= ").append(String.valueOf((ksRGB8ReduceAfterExp)));
        body.append(eol).append("dwInputMaxValue= ").append(String.valueOf(dwInputMaxValue));
        
        // Print the LinearSRGBtoSRGB lut.
        body .append(eol).append("[lut = [short["+lut.length+"]]]");

        // Print the FP luts.
        body .append(eol).append ("fLut=  " + fLut.toString());

        rep.append(ColorSpace.indent("  ",body));
        return rep.append("]").toString(); }

    /**
     * Construct the lut from the RestrictedICCProfile.
     *
     *   @param ricc input RestrictedICCProfile
     *   @param dwInputMaxValue size of the output lut.
     *   @param dwInputShiftValue value used to shift samples to positive
     */
    public MonochromeTransformTosRGB 
        (RestrictedICCProfile  ricc, 
         int dwInputMaxValue,
         int dwInputShiftValue) {

        if (ricc.getType() != RestrictedICCProfile.kMonochromeInput)
            throw new IllegalArgumentException ("MonochromeTransformTosRGB: wrong type ICCProfile supplied");

        this.dwInputMaxValue = dwInputMaxValue;
        lut = new short [dwInputMaxValue+1];
        fLut = LookUpTableFP.createInstance (ricc.trc[ICCProfile.GRAY], dwInputMaxValue + 1);

        // First calculate the value for the shadow region
        int i;
        for ( i = 0; ((i <= dwInputMaxValue) && (fLut.lut[i] <= ksRGBShadowCutoff)); i++)
            lut[i] = (short)(Math.floor (ksRGB8ShadowSlope * (double)fLut.lut[i] + 0.5)-dwInputShiftValue);
	
        // Now calculate the rest   
        for (; i <= dwInputMaxValue; i++)
            lut[i] = (short)(Math.floor (ksRGB8ScaleAfterExp * Math.pow((double)fLut.lut[i], ksRGBExponent) -
                                         ksRGB8ReduceAfterExp + 0.5) - dwInputShiftValue); }

    /**
     * Populate the output block by looking up the values in the lut, using the input
     * as lut indices.
     *   @param inb input samples
     *   @param outb output samples.
     * @exception MonochromeTransformException
     */
    public void apply (DataBlkInt inb, DataBlkInt outb) 
        throws MonochromeTransformException {

        int i,j,x,y,o;

        int [] in  = (int []) inb.getData();
        int [] out = (int []) outb.getData();
        
        if (out==null || out.length < in.length) {
            out = new int [in.length];
            outb.setData(out); }

        outb.uly = inb.uly;
        outb.ulx = inb.ulx;
        outb.h = inb.h;
        outb.w = inb.w;
        outb.offset = inb.offset;
        outb.scanw = inb.scanw;

        o=inb.offset;
        for (i=0; i<inb.h*inb.w; ++i) {
            j=in[i];
            if (j<0) j=0;
            else if (j>dwInputMaxValue) j=dwInputMaxValue;
            out[i] = lut[j]; }}

    /**
     * Populate the output block by looking up the values in the lut, using the input
     * as lut indices.
     *   @param inb input samples
     *   @param outb output samples.
     * @exception MonochromeTransformException
     */
    public void apply (DataBlkFloat inb, DataBlkFloat outb) 
        throws MonochromeTransformException {

        int i,j,x,y,o;

        float [] in  = (float []) inb.getData();
        float [] out = (float []) outb.getData();
        

        if (out==null || out.length < in.length) {
            out = new float [in.length];
            outb.setData(out); 

        outb.uly = inb.uly;
        outb.ulx = inb.ulx;
        outb.h = inb.h;
        outb.w = inb.w;
        outb.offset = inb.offset;
        outb.scanw = inb.scanw;}

        o=inb.offset;
        for (i=0; i<inb.h*inb.w; ++i) {
            j=(int) in[i];
            if (j<0) j=0;
            else if (j>dwInputMaxValue) j=dwInputMaxValue;
            out[i] = lut[j]; }}


    /* end class MonochromeTransformTosRGB */ }








