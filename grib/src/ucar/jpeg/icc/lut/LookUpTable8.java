/*****************************************************************************
 *
 * $Id: LookUpTable8.java,v 1.1 2002/07/25 14:56:48 grosbois Exp $
 *
 * Copyright Eastman Kodak Company, 343 State Street, Rochester, NY 14650
 * $Date $
 *****************************************************************************/

package ucar.jpeg.icc.lut;

import ucar.jpeg.icc .tags.ICCCurveType;

/**
 * Toplevel class for a byte [] lut.
 * 
 * @version	1.0
 * @author	Bruce A. Kern
 */
public abstract class LookUpTable8 extends LookUpTable {     
    
    /** Maximum output value of the LUT */  protected final byte dwMaxOutput;   // Maximum output value of the LUT
    /** The lut values.                 */  protected final byte [] lut;


    /**
     * Create an abbreviated string representation of a 16 bit lut.
     * @return the lut as a String
     */
    public String toString () {
        StringBuffer rep = new StringBuffer ("[LookUpTable8 ");
        int row,col;
        rep .append("max= " + dwMaxOutput);
        rep .append(", nentries= " + dwMaxOutput);
        return rep.append("]").toString(); }



    public String toStringWholeLut () {
        StringBuffer rep = new StringBuffer ("LookUpTable8" + eol);
        rep .append("maxOutput = " + dwMaxOutput + eol);
        for (int i=0; i<dwNumInput; ++i) 
            rep .append("lut["+i+"] = " + lut[i] + eol);
        return rep.append("]").toString(); }

    protected  LookUpTable8  
        ( int dwNumInput,       // Number of i   nput values in created LUT
          byte dwMaxOutput       // Maximum output value of the LUT   
          ) {
        super (null, dwNumInput);
        lut = new byte [dwNumInput];
        this.dwMaxOutput = dwMaxOutput; }
    

    /**
     * Create the string representation of a 16 bit lut.
     * @return the lut as a String
     */
    protected LookUpTable8 
        ( ICCCurveType curve,   // Pointer to the curve data            
          int dwNumInput,       // Number of input values in created LUT
          byte dwMaxOutput       // Maximum output value of the LUT
          ) { 
        super (curve, dwNumInput);
        this.dwMaxOutput = dwMaxOutput;
        lut = new byte [dwNumInput]; }
    
    /**
     * lut accessor
     *   @param index of the element
     * @return the lut [index]
     */
    public final byte elementAt  ( int index ) {
        return lut [index]; }
    
    /* end class LookUpTable8 */ }
















