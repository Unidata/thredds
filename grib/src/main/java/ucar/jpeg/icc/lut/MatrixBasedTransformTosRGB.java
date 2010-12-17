/*****************************************************************************
 *
 * $Id: MatrixBasedTransformTosRGB.java,v 1.1 2002/07/25 14:56:49 grosbois Exp $
 *
 * Copyright Eastman Kodak Company, 343 State Street, Rochester, NY 14650
 * $Date $
 *****************************************************************************/

package ucar.jpeg.icc.lut;

import ucar.jpeg.colorspace .ColorSpace;
import ucar.jpeg.icc .ICCProfile;
import ucar.jpeg.icc .RestrictedICCProfile;
import ucar.jpeg.icc .tags.ICCXYZType;
import ucar.jpeg.icc .lut.LookUpTableFP;
import ucar.jpeg.jj2000.j2k.image.DataBlkInt;
import ucar.jpeg.jj2000.j2k.image.DataBlkFloat;

/**
 * Transform for applying ICCProfiling to an input DataBlk
 * 
 * @see		jj2000.j2k.image.DataBlkInt
 * @see		jj2000.j2k.image.DataBlkFloat
 * @version	1.0
 * @author	Bruce A. Kern
 */
public class MatrixBasedTransformTosRGB {

    private static final String eol = System.getProperty ("line.separator");

    // Start of contant definitions:

    private static final int               // Convenience
        RED   = ICCProfile.RED,
        GREEN = ICCProfile.GREEN,
        BLUE  = ICCProfile.BLUE;

    private static final double            // Define the PCS to linear sRGB matrix coefficients
        SRGB00 =  3.1337,
        SRGB01 = -1.6173,
        SRGB02 = -0.4907,
        SRGB10 = -0.9785,
        SRGB11 =  1.9162,
        SRGB12 =  0.0334,
        SRGB20 =  0.0720,
        SRGB21 = -0.2290,
        SRGB22 =  1.4056;
    
    // Define constants representing the indices into the matrix array
    private static final int M00 = 0;
    private static final int M01 = 1;
    private static final int M02 = 2;
    private static final int M10 = 3;
    private static final int M11 = 4;
    private static final int M12 = 5;
    private static final int M20 = 6;
    private static final int M21 = 7;
    private static final int M22 = 8;

    private static final double ksRGBExponent		 = (1.0 / 2.4);
    private static final double ksRGBScaleAfterExp	 = 1.055;
    private static final double ksRGBReduceAfterExp  = 0.055;
    private static final double ksRGBShadowCutoff	 = 0.0031308;
    private static final double ksRGBShadowSlope     = 12.92; 

    // End of contant definitions:

    private final double [] matrix;      // Matrix coefficients 

    private LookUpTableFP [] fLut = new LookUpTableFP [3];			
    private LookUpTable32LinearSRGBtoSRGB lut; // Linear sRGB to sRGB LUT

    private final int [] dwMaxValue;
    private final int [] dwShiftValue;

    private int            dwMaxCols = 0;			// Maximum number of columns that can be processed
    private int            dwMaxRows = 0;			// Maximum number of rows that can be processed

    private float [][] fBuf = null;      // Intermediate output of the first LUT operation.

    /**
     * String representation of class
     * @return suitable representation for class 
     */
    public String toString () {
        int i,j;

        StringBuffer rep = new StringBuffer ("[MatrixBasedTransformTosRGB: ");

        StringBuffer body = new StringBuffer ("  ");
        body.append(eol).append("ksRGBExponent= ").append(String.valueOf(ksRGBExponent));
        body.append(eol).append("ksRGBScaleAfterExp= ").append(String.valueOf(ksRGBScaleAfterExp));
        body.append(eol).append("ksRGBReduceAfterExp= ").append(String.valueOf(ksRGBReduceAfterExp));

        
        body.append(eol)
            .append("dwMaxValues= ")
            .append(String.valueOf(dwMaxValue[0])).append(", ")
            .append(String.valueOf(dwMaxValue[1])).append(", ")
            .append(String.valueOf(dwMaxValue[2]));

        body.append(eol)
            .append("dwShiftValues= ")
            .append(String.valueOf(dwShiftValue[0])).append(", ")
            .append(String.valueOf(dwShiftValue[1])).append(", ")
            .append(String.valueOf(dwShiftValue[2]));

        body.append(eol)
            .append(eol).append("fLut= ")
            .append(eol).append(ColorSpace.indent ("  ", "fLut[RED]=  "+ fLut[0].toString()))
            .append(eol).append(ColorSpace.indent ("  ", "fLut[GRN]=  "+ fLut[1].toString()))
            .append(eol).append(ColorSpace.indent ("  ", "fLut[BLU]=  "+ fLut[2].toString()));

        // Print the matrix
        body .append(eol).append(eol).append("[matrix ");
        for (i=0; i<3; ++i) {
            body .append(eol).append("  ");
            for (j=0; j<3; ++j) {
                body .append(matrix [3*i+j] + "   "); }}
        body  .append("]");
        
        
        // Print the LinearSRGBtoSRGB lut.
        body.append(eol).append(eol).append(lut.toString());

        rep.append(ColorSpace.indent("  ",body)).append("]");
        return rep.append("]").toString(); }


    /**
     * Construct a 3 component transform based on an input RestricedICCProfile
     * This transform will pass the input throught a floating point lut (LookUpTableFP),
     * apply a matrix to the output and finally pass the intermediate buffer through
     * a 8-bit lut (LookUpTable8).  This operation will be designated (LFP*M*L8) * Data
     * The operators (LFP*M*L8) are constructed here.  Although the data for
     * only one component is returned, the transformation must be done for all
     * components, because the matrix application involves a linear combination of
     * component input to produce the output.
     *   @param ricc input profile
     *   @param dwMaxValue clipping value for output.
     *   @param dwMaxCols number of columns to transform
     *   @param dwMaxRows number of rows to transform
     */
    public MatrixBasedTransformTosRGB 
        (RestrictedICCProfile  ricc,
         int dwMaxValue [],
         int dwShiftValue []) {

        // Assure the proper type profile for this xform.
        if (ricc.getType() != RestrictedICCProfile.kThreeCompInput)
            throw new IllegalArgumentException ("MatrixBasedTransformTosRGB: wrong type ICCProfile supplied");

        int c;					     				// component index.
        this .dwMaxValue   = dwMaxValue;
        this .dwShiftValue = dwShiftValue;

        // Create the LUTFP from the input profile.
        for (c=0; c<3; ++c) {
            fLut[c] = LookUpTableFP.createInstance (ricc.trc[c], dwMaxValue[c]+1); }

        // Create the Input linear to PCS matrix
        matrix = createMatrix(ricc, dwMaxValue);      // Create and matrix from the ICC profile.

        // Create the final LUT32
        lut = LookUpTable32LinearSRGBtoSRGB.createInstance
            (dwMaxValue[0], dwMaxValue[0], 
             ksRGBShadowCutoff, ksRGBShadowSlope, 
             ksRGBScaleAfterExp, ksRGBExponent, ksRGBReduceAfterExp); }


    private double [] createMatrix (RestrictedICCProfile ricc, int [] maxValues) {

        // Coefficients from the input linear to PCS matrix
        double dfPCS00 = ICCXYZType.XYZToDouble(ricc.colorant[RED].x);
        double dfPCS01 = ICCXYZType.XYZToDouble(ricc.colorant[GREEN].x);
        double dfPCS02 = ICCXYZType.XYZToDouble(ricc.colorant[BLUE].x);
        double dfPCS10 = ICCXYZType.XYZToDouble(ricc.colorant[RED].y);
        double dfPCS11 = ICCXYZType.XYZToDouble(ricc.colorant[GREEN].y);
        double dfPCS12 = ICCXYZType.XYZToDouble(ricc.colorant[BLUE].y);
        double dfPCS20 = ICCXYZType.XYZToDouble(ricc.colorant[RED].z);
        double dfPCS21 = ICCXYZType.XYZToDouble(ricc.colorant[GREEN].z);
        double dfPCS22 = ICCXYZType.XYZToDouble(ricc.colorant[BLUE].z);

        double [] matrix = new double [9]; 
        matrix[M00] = maxValues[0] * (SRGB00 * dfPCS00 + SRGB01 * dfPCS10 + SRGB02 * dfPCS20);
        matrix[M01] = maxValues[0] * (SRGB00 * dfPCS01 + SRGB01 * dfPCS11 + SRGB02 * dfPCS21);
        matrix[M02] = maxValues[0] * (SRGB00 * dfPCS02 + SRGB01 * dfPCS12 + SRGB02 * dfPCS22);
        matrix[M10] = maxValues[1] * (SRGB10 * dfPCS00 + SRGB11 * dfPCS10 + SRGB12 * dfPCS20);
        matrix[M11] = maxValues[1] * (SRGB10 * dfPCS01 + SRGB11 * dfPCS11 + SRGB12 * dfPCS21);
        matrix[M12] = maxValues[1] * (SRGB10 * dfPCS02 + SRGB11 * dfPCS12 + SRGB12 * dfPCS22);
        matrix[M20] = maxValues[2] * (SRGB20 * dfPCS00 + SRGB21 * dfPCS10 + SRGB22 * dfPCS20);
        matrix[M21] = maxValues[2] * (SRGB20 * dfPCS01 + SRGB21 * dfPCS11 + SRGB22 * dfPCS21);
        matrix[M22] = maxValues[2] * (SRGB20 * dfPCS02 + SRGB21 * dfPCS12 + SRGB22 * dfPCS22);

        return matrix; }


    /**
     * Performs the transform.  Pass the input throught the LookUpTableFP, apply the
     * matrix to the output and finally pass the intermediate buffer through the
     * LookUpTable8.  This operation is designated (LFP*M*L8) * Data are already 
     * constructed.  Although the data for only one component is returned, the
     * transformation must be done for all components, because the matrix application
     * involves a linear combination of component input to produce the output.
     *   @param ncols number of columns in the input
     *   @param nrows number of rows in the input
     *   @param inb input data block
     *   @param outb output data block
     * @exception MatrixBasedTransformException
     */
    public void apply (DataBlkInt inb [], DataBlkInt outb [])
        throws MatrixBasedTransformException {
        int [][] in  = new int [3][], out = new int [3][]; // data references.

        int nrows = inb[0].h, ncols = inb[0].w;

        if ((fBuf == null) || (fBuf[0].length < ncols*nrows)) {
            fBuf = new float [3][ncols*nrows];}

        // for each component (rgb)
        for (int c=0; c<3; ++c) {

            // Reference the input and output samples.
            in[c]  = (int []) inb[c].getData(); 
            out[c] = (int []) outb[c].getData();
            
            // Assure a properly sized output buffer.
            if (out[c]==null || out[c].length < in[c].length) {
                out[c] = new int [in[c].length];
                outb[c].setData(out[c]); }

            // The first thing to do is to process the input into a standard form
            // and through the first input LUT, producing floating point output values
            standardizeMatrixLineThroughLut(inb[c], fBuf[c], dwMaxValue[c], fLut[c]); }

        // For each row and column
        float [] ra = fBuf[RED];
        float [] ga = fBuf[GREEN];
        float [] ba = fBuf[BLUE];

        int   [] ro = out [RED];
        int   [] go = out [GREEN];
        int   [] bo = out [BLUE];
        int   [] lut32 = lut.lut;

        double r, g, b;
        int val, index=0;
        for (int y = 0; y < inb[0].h; ++y) {
            int end = index+inb[0].w;
            while (index < end) {
                // Calculate the rgb pixel indices for this row / column
                r = ra[index];
                g = ga[index];
                b = ba[index];

                // Apply the matrix to the intermediate floating point data in order to index the 
                // final LUT.
                val = (int)(matrix[M00] * r + 
                            matrix[M01] * g + 
                            matrix[M02] * b + 
                            0.5);
                // Clip the calculated value if necessary..
                if      (val <  0)             ro[index] = lut32[0];
                else if (val >= lut32.length ) ro[index] = lut32[lut32.length-1];
                else                           ro[index] = lut32[val];
                        
                val = (int)(matrix[M10] * r + 
                            matrix[M11] * g + 
                            matrix[M12] * b + 
                            0.5);
                // Clip the calculated value if necessary..
                if      (val <  0)             go[index] = lut32[0];
                else if (val >= lut32.length ) go[index] = lut32[lut32.length-1];
                else                           go[index] = lut32[val];
                        
                val = (int)(matrix[M20] * r + 
                            matrix[M21] * g + 
                            matrix[M22] * b + 
                            0.5);
                // Clip the calculated value if necessary..
                if      (val <  0)             bo[index] = lut32[0];
                else if (val >= lut32.length ) bo[index] = lut32[lut32.length-1];
                else                           bo[index] = lut32[val];

                index++; }}}       

    /**
     * Performs the transform.  Pass the input throught the LookUpTableFP, apply the
     * matrix to the output and finally pass the intermediate buffer through the
     * LookUpTable8.  This operation is designated (LFP*M*L8) * Data are already 
     * constructed.  Although the data for only one component is returned, the
     * transformation must be done for all components, because the matrix application
     * involves a linear combination of component input to produce the output.
     *   @param ncols number of columns in the input
     *   @param nrows number of rows in the input
     *   @param inb input data block
     *   @param outb output data block
     * @exception MatrixBasedTransformException
     */
    public void apply (DataBlkFloat inb [], DataBlkFloat outb []) 
        throws MatrixBasedTransformException {

        float [][] in  = new float [3][], out = new float [3][]; // data references.

        int nrows = inb[0].h, ncols = inb[0].w;

        if ((fBuf == null) || (fBuf[0].length < ncols*nrows)) {
            fBuf = new float [3][ncols*nrows]; }

        // for each component (rgb)
        for (int c=0; c<3; ++c) {

            // Reference the input and output pixels.
            in[c]  = (float []) inb[c].getData(); 
            out[c] = (float []) outb[c].getData();
            
            // Assure a properly sized output buffer.
            if (out[c]==null || out[c].length < in[c].length) {
                out[c] = new float [in[c].length];
                outb[c].setData(out[c]); }

            // The first thing to do is to process the input into a standard form
            // and through the first input LUT, producing floating point output values
            standardizeMatrixLineThroughLut(inb[c], fBuf[c], dwMaxValue[c], fLut[c]); }

        int   [] lut32 = lut.lut;

        // For each row and column 
        int index=0, val;
        for (int y = 0; y < inb[0].h; ++y) {
            int end = index+inb[0].w;
            while (index < end) {
                // Calculate the rgb pixel indices for this row / column
                        
                // Apply the matrix to the intermediate floating point data inorder to index the 
                // final LUT.
                val = (int)(matrix[M00] * fBuf[RED]  [index] + 
                            matrix[M01] * fBuf[GREEN][index] + 
                            matrix[M02] * fBuf[BLUE] [index] + 
                            0.5);
                // Clip the calculated value if necessary..
                if      (val <  0)             out[0][index] = lut32[0];
                else if (val >= lut32.length ) out[0][index] = lut32[lut32.length-1];
                else                           out[0][index] = lut32[val];

                val = (int)(matrix[M10] * fBuf[RED]  [index] + 
                            matrix[M11] * fBuf[GREEN][index] + 
                            matrix[M12] * fBuf[BLUE] [index] + 
                            0.5);
                // Clip the calculated value if necessary..
                if      (val <  0)             out[1][index] = lut32[0];
                else if (val >= lut32.length ) out[1][index] = lut32[lut32.length-1];
                else                           out[1][index] = lut32[val];
                        
                val = (int)(matrix[M20] * fBuf[RED]  [index] + 
                            matrix[M21] * fBuf[GREEN][index] + 
                            matrix[M22] * fBuf[BLUE] [index] + 
                            0.5);
                // Clip the calculated value if necessary..
                if      (val <  0)             out[2][index] = lut32[0];
                else if (val >= lut32.length ) out[2][index] = lut32[lut32.length-1];
                else                           out[2][index] = lut32[val];
                        
                index++; }}
    }

    private static void standardizeMatrixLineThroughLut
        (
         DataBlkInt inb,            // input datablock
         float [] out,				// output data reference
         int dwInputMaxValue,		// Maximum value of the input for clipping  
         LookUpTableFP lut			// Inital input LUT
         )   {
        int wTemp, j=0;
        int [] in = (int []) inb.getData(); // input pixel reference
        float [] lutFP = lut.lut;
        for (int y = inb.uly; y < inb.uly+inb.h; ++y) {
            for (int x = inb.ulx; x < inb.ulx+inb.w; ++x) {
                int i = inb.offset+(y-inb.uly)*inb.scanw+(x-inb.ulx); // pixel index.
                if (in[i] > dwInputMaxValue) wTemp = dwInputMaxValue;
                else if (in[i] < 0) wTemp = 0;                       
                else wTemp = in[i];                                  
                out[j++] = lutFP[wTemp]; }}}      
     

    private static void standardizeMatrixLineThroughLut
        (
         DataBlkFloat inb,          // input datablock
         float [] out,				// output data reference
         float dwInputMaxValue,		// Maximum value of the input for clipping  
         LookUpTableFP lut			// Inital input LUT
         )   {
        int j=0;
        float wTemp;
        float [] in = (float []) inb.getData(); // input pixel reference
        float [] lutFP = lut.lut;

        for (int y = inb.uly; y < inb.uly+inb.h; ++y) {
            for (int x = inb.ulx; x < inb.ulx+inb.w; ++x) {
                int i = inb.offset+(y-inb.uly)*inb.scanw+(x-inb.ulx); // pixel index.
                if (in[i] > dwInputMaxValue) wTemp = dwInputMaxValue;
                else if (in[i] < 0) wTemp = 0;                       
                else wTemp = in[i];                                  
                out[j++] = lutFP[(int)wTemp]; }}}            

    /* end class MatrixBasedTransformTosRGB */ }
