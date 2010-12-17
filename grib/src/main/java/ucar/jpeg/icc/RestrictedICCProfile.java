/*****************************************************************************
 *
 * $Id: RestrictedICCProfile.java,v 1.1 2002/07/25 14:56:56 grosbois Exp $
 *
 * Copyright Eastman Kodak Company, 343 State Street, Rochester, NY 14650
 * $Date $
 *****************************************************************************/

package ucar.jpeg.icc;

import ucar.jpeg.icc .tags.ICCCurveType;
import ucar.jpeg.icc .tags.ICCXYZType;
import ucar.jpeg.icc .tags.ICCTagTable;
import ucar.jpeg.icc .tags.ICCTag;

/**
 * This profile is constructed by parsing an ICCProfile and
 * is the profile actually applied to the image.
 * 
 * @see		jj2000.j2k.icc.ICCProfile
 * @version	1.0
 * @author	Bruce A. Kern
 */
public abstract class RestrictedICCProfile {

    protected static final String eol = System.getProperty("line.separator");

    /**
     * Factory method for creating a RestrictedICCProfile from 
     * 3 component curve and colorant data.
     *   @param rcurve red curve
     *   @param gcurve green curve
     *   @param bcurve blue curve
     *   @param rcolorant red colorant
     *   @param gcolorant green colorant
     *   @param bcolorant blue colorant
     * @return MatrixBasedRestrictedProfile
     */
    public static RestrictedICCProfile createInstance 
        (ICCCurveType rcurve, ICCCurveType gcurve, ICCCurveType bcurve, 
         ICCXYZType rcolorant, ICCXYZType gcolorant, ICCXYZType bcolorant) {

        return   
            MatrixBasedRestrictedProfile.createInstance
            (rcurve, gcurve, bcurve, 
             rcolorant, gcolorant, bcolorant); }

    /**
     * Factory method for creating a RestrictedICCProfile from 
     * gray curve data.
     *   @param gcurve gray curve
     * @return MonochromeInputRestrictedProfile
     */
    public static RestrictedICCProfile createInstance (ICCCurveType gcurve) {
        return  
            MonochromeInputRestrictedProfile.createInstance
            (gcurve); }

    /** Component index       */ protected final static int GRAY  = ICCProfile.GRAY ;
    /** Component index       */ protected final static int RED   = ICCProfile.RED;
    /** Component index       */ protected final static int GREEN = ICCProfile.GREEN;
    /** Component index       */ protected final static int BLUE  = ICCProfile.BLUE ;
    /** input type enumerator */ public    final static int kMonochromeInput =  0;
    /** input type enumerator */ public    final static int kThreeCompInput  =  1;

    /** Curve data    */ public ICCCurveType [] trc; 
    /** Colorant data */ public ICCXYZType	  [] colorant;

    /** Returns the appropriate input type enum. */
    public abstract int getType();

    /**
     * Construct the common state of all gray RestrictedICCProfiles
     *   @param gcurve curve data
     */
    protected RestrictedICCProfile (ICCCurveType gcurve) {
        trc = new ICCCurveType [1];
        colorant = null;
        trc[GRAY] = gcurve;
    }

    /**
     * Construct the common state of all 3 component RestrictedICCProfiles
     * 
     *   @param rcurve red curve
     *   @param gcurve green curve
     *   @param bcurve blue curve
     *   @param rcolorant red colorant
     *   @param gcolorant green colorant
     *   @param bcolorant blue colorant
     */
    protected RestrictedICCProfile (ICCCurveType rcurve, ICCCurveType gcurve, ICCCurveType bcurve, 
                                    ICCXYZType rcolorant, ICCXYZType gcolorant, ICCXYZType bcolorant) {
        trc         = new ICCCurveType [3];
        colorant    = new ICCXYZType [3];

        trc [RED]   = rcurve;
        trc [GREEN] = gcurve;
        trc [BLUE]  = bcurve;

        colorant [RED] = rcolorant;
        colorant [GREEN] = gcolorant;
        colorant [BLUE] = bcolorant;
    }

    /* end class RestrictedICCProfile */ }



