/*****************************************************************************
 *
 * $Id: LookUpTable16.java,v 1.1 2002/07/25 14:56:47 grosbois Exp $
 *
 * Copyright Eastman Kodak Company, 343 State Street, Rochester, NY 14650
 * $Date $
 *****************************************************************************/

package ucar.jpeg.icc.lut;

import ucar.jpeg.icc .tags.ICCCurveType;

/**
 * Toplevel class for a short [] lut.
 * 
 * @version	1.0
 * @author	Bruce A. Kern
 */
public abstract class LookUpTable16 extends LookUpTable {     
    
    /** Maximum output value of the LUT */ protected final int dwMaxOutput;
    /** The lut values.                 */ protected final short [] lut;

    /**
     * Create an abbreviated string representation of a 16 bit lut.
     * @return the lut as a String
     */
    public String toString () {
        StringBuffer rep = new StringBuffer ("[LookUpTable16 ");
        int row,col;
        rep .append("max= " + dwMaxOutput);
        rep .append(", nentries= " + dwMaxOutput);
        return rep.append("]").toString(); }

    /**
     * Create a full string representation of a 16 bit lut.
     * @return the lut as a String
     */
    public String toStringWholeLut () {
        StringBuffer rep = new StringBuffer ("[LookUpTable16" + eol);
        int row,col;

        rep .append("max output = " + dwMaxOutput + eol);
        for (row = 0; row < dwNumInput/10; ++row) {
            rep .append("lut["+10*row+"] : ");
            for (col = 0; col<10; ++col) {
                rep .append(lut[10*row+col]).append(" "); }
            rep .append(eol); }
        // Partial row.
        rep .append("lut["+10*row+"] : ");
        for (col = 0; col < dwNumInput%10; ++col)
            rep .append(lut[10*row+col] + " ");
        rep .append(eol+eol);
        return rep.toString(); }

    /**
     * Factory method for getting a 16 bit lut from a given curve.
     *   @param curve  the data
     *   @param dwNumInput the size of the lut 
     *   @param dwMaxOutput max output value of the lut
     * @return the lookup table
     */
    public static LookUpTable16 createInstance (ICCCurveType curve, int dwNumInput, int dwMaxOutput ) {
        
        if (curve.count == 1) return new LookUpTable16Gamma  (curve, dwNumInput, dwMaxOutput);
        else                  return new LookUpTable16Interp (curve, dwNumInput, dwMaxOutput); }

    /**
      * Construct an empty 16 bit lut
      *   @param dwNumInput the size of the lut t lut.
      *   @param dwMaxOutput max output value of the lut
      */
    protected  LookUpTable16 ( int dwNumInput, int dwMaxOutput ) {

        super (null, dwNumInput);
        lut = new short [dwNumInput];
        this.dwMaxOutput = dwMaxOutput; }

    /**
     * Construct a 16 bit lut from a given curve.
      *   @param curve the data
      *   @param dwNumInput the size of the lut t lut.
      *   @param dwMaxOutput max output value of the lut
     */
    protected LookUpTable16 (ICCCurveType curve, int dwNumInput, int dwMaxOutput) { 

        super (curve, dwNumInput);
        this.dwMaxOutput = dwMaxOutput;
        lut = new short [dwNumInput]; }
    
    /**
     * lut accessor
     *   @param index of the element
     * @return the lut [index]
     */
    public final short elementAt  ( int index ) {
        return lut [index]; }
    
    /* end class LookUpTable16 */ }
















