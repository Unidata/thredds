/*****************************************************************************
 *
 * $Id: LookUpTable.java,v 1.1 2002/07/25 14:56:49 grosbois Exp $
 *
 * Copyright Eastman Kodak Company, 343 State Street, Rochester, NY 14650
 * $Date $
 *****************************************************************************/

package ucar.jpeg.icc.lut;

import ucar.jpeg.icc .tags.ICCCurveType;


/**
 * Toplevel class for a lut.  All lookup tables must
 * extend this class.
 * 
 * @version	1.0
 * @author	Bruce A. Kern
 */
public abstract class LookUpTable {

    /** End of line string.             */ protected static final String eol = System.getProperty ("line.separator");
    /** The curve data                  */ protected ICCCurveType curve = null;
    /** Number of values in created lut */ protected int          dwNumInput  = 0;
                

    /**
     * For subclass usage.
     *   @param curve The curve data  
     *   @param dwNumInput Number of values in created lut
     */
    protected LookUpTable (
                 ICCCurveType curve,
                 int dwNumInput
                 ) {
        this.curve       = curve;
        this.dwNumInput  = dwNumInput; }

    /* end class LookUpTable */ }
















