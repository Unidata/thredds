/*****************************************************************************
 *
 * $Id: LookUpTable32.java,v 1.1 2002/07/25 14:56:47 grosbois Exp $
 *
 * Copyright Eastman Kodak Company, 343 State Street, Rochester, NY 14650
 * $Date $
 *****************************************************************************/

package ucar.jpeg.icc.lut;

import ucar.jpeg.icc .tags.ICCCurveType;

/**
 * Toplevel class for a int [] lut.
 * 
 * @version	1.0
 * @author	Bruce A. Kern
 */
abstract class LookUpTable32 extends LookUpTable {     
    
    /** Maximum output value of the LUT */ protected final int dwMaxOutput;
    /** the lut values.                 */ public final int [] lut;

    /**
     * Create an abbreviated string representation of a 16 bit lut.
     * @return the lut as a String
     */
    public String toString () {
        StringBuffer rep = new StringBuffer ("[LookUpTable32 ");
        int row,col;
        rep .append("max= " + dwMaxOutput);
        rep .append(", nentries= " + dwNumInput);
        return rep.append("]").toString(); }

    /**
     * Create the string representation of a 32 bit lut.
     * @return the lut as a String
     */
    public String toStringWholeLut () {
        StringBuffer rep = new StringBuffer ("[LookUpTable32" + eol);
        int row,col;
        rep .append("max output = " + dwMaxOutput + eol);
        for (row = 0; row < dwNumInput/10; ++row) {
            rep .append("lut["+10*row+"] : ");
            for (col = 0; col<10; ++col) {
                rep .append(lut[10*row+col] + " "); }
            rep .append(eol); }
        // Partial row.
        rep .append("lut["+10*row+"] : ");
        for (col = 0; col < dwNumInput%10; ++col)
            rep .append(lut[10*row+col] + " ");
        rep .append(eol+ eol);
        return rep.toString(); }

    /**
     * Factory method for getting a 32 bit lut from a given curve.
     *   @param curve  the data
     *   @param dwNumInput the size of the lut 
     *   @param dwMaxOutput max output value of the lut
     * @return the lookup table
     */
    public static LookUpTable32 createInstance (
                 ICCCurveType curve,   // Pointer to the curve data            
                 int dwNumInput,       // Number of input values in created LUT
                 int dwMaxOutput       // Maximum output value of the LUT
                ) {
        if (curve.count == 1) return new LookUpTable32Gamma  (curve, dwNumInput, dwMaxOutput);
        else                  return new LookUpTable32Interp (curve, dwNumInput, dwMaxOutput); }

    /**
      * Construct an empty 32 bit
      *   @param dwNumInput the size of the lut t lut.
      *   @param dwMaxOutput max output value of the lut
      */
    protected  LookUpTable32 
        ( int dwNumInput,       // Number of i   nput values in created LUT
          int dwMaxOutput       // Maximum output value of the LUT   
          ) {
        super (null, dwNumInput);
        lut = new int [dwNumInput];
        this.dwMaxOutput = dwMaxOutput; 
    }

    /**
     * Construct a 16 bit lut from a given curve.
      *   @param curve the data
      *   @param dwNumInput the size of the lut t lut.
      *   @param dwMaxOutput max output value of the lut
     */
    protected LookUpTable32 (
                 ICCCurveType curve,   // Pointer to the curve data            
                 int dwNumInput,       // Number of input values in created LUT
                 int dwMaxOutput       // Maximum output value of the LUT
                 ) { 
        super (curve, dwNumInput);
        this.dwMaxOutput = dwMaxOutput;
        lut = new int [dwNumInput]; }
    
    /**
     * lut accessor
     *   @param index of the element
     * @return the lut [index]
     */
    public final int elementAt  ( int index ) {
        return lut [index]; }
    
    /* end class LookUpTable32 */ }
















