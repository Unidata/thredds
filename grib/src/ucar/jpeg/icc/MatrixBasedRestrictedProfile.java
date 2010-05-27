/*****************************************************************************
 *
 * $Id: MatrixBasedRestrictedProfile.java,v 1.1 2002/07/25 14:56:56 grosbois Exp $
 *
 * Copyright Eastman Kodak Company, 343 State Street, Rochester, NY 14650
 * $Date $
 *****************************************************************************/

package ucar.jpeg.icc;

import ucar.jpeg.icc .tags.ICCCurveType;
import ucar.jpeg.icc .tags.ICCXYZType;

/**
 * This class is a 3 component RestrictedICCProfile
 * 
 * @version	1.0
 * @author	Bruce A Kern
 */
public class MatrixBasedRestrictedProfile extends  RestrictedICCProfile { 

    /**
     * Factory method which returns a 3 component RestrictedICCProfile
     *   @param rcurve Red TRC curve
     *   @param gcurve Green TRC curve
     *   @param bcurve Blue TRC curve
     *   @param rcolorant Red colorant
     *   @param gcolorant Green colorant
     *   @param bcolorant Blue colorant
     * @return the RestrictedICCProfile
     */
    public static RestrictedICCProfile createInstance (ICCCurveType rcurve, ICCCurveType gcurve, ICCCurveType bcurve, 
                        ICCXYZType rcolorant, ICCXYZType gcolorant, ICCXYZType bcolorant) {
        return new MatrixBasedRestrictedProfile(rcurve,gcurve,bcurve,rcolorant,gcolorant,bcolorant); }
    
    /**
     * Construct a 3 component RestrictedICCProfile
     *   @param rcurve Red TRC curve
     *   @param gcurve Green TRC curve
     *   @param bcurve Blue TRC curve
     *   @param rcolorant Red colorant
     *   @param gcolorant Green colorant
     *   @param bcolorant Blue colorant
     */
    protected MatrixBasedRestrictedProfile (ICCCurveType rcurve, ICCCurveType gcurve, ICCCurveType bcurve, 
                        ICCXYZType rcolorant, ICCXYZType gcolorant, ICCXYZType bcolorant) {
        super (rcurve, gcurve, bcurve, rcolorant, gcolorant, bcolorant); }

    /**
     * Get the type of RestrictedICCProfile for this object
     * @return kThreeCompInput
     */
    public  int getType () {return kThreeCompInput;}

    /**
     * @return String representation of a MatrixBasedRestrictedProfile
     */
    public String toString () {
        StringBuffer rep = 
            new StringBuffer ("[Matrix-Based Input Restricted ICC profile").append(eol);

        rep .append("trc[RED]:").append(eol).append(trc[RED]).append(eol);
        rep .append("trc[RED]:").append(eol).append(trc[GREEN]).append(eol);
        rep .append("trc[RED]:").append(eol).append(trc[BLUE]).append(eol);

        rep .append("Red colorant:  ").append(colorant[RED]).append(eol);
        rep .append("Red colorant:  ").append(colorant[GREEN]).append(eol);
        rep .append("Red colorant:  ").append(colorant[BLUE]).append(eol);

        return rep.append("]").toString(); }

    /* end class MatrixBasedRestrictedProfile */ }



