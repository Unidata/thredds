/*****************************************************************************
 *
 * $Id: MonochromeInputRestrictedProfile.java,v 1.1 2002/07/25 14:56:56 grosbois Exp $
 *
 * Copyright Eastman Kodak Company, 343 State Street, Rochester, NY 14650
 * $Date $
 *****************************************************************************/

package ucar.jpeg.icc;

import ucar.jpeg.icc .tags.ICCCurveType;

/**
 * This class is a 1 component RestrictedICCProfile
 * 
 * @version	1.0
 * @author	Bruce A Kern
 */
public class MonochromeInputRestrictedProfile extends RestrictedICCProfile {

    /**
     * Factory method which returns a 1 component RestrictedICCProfile
     *   @param c Gray TRC curve
     * @return the RestrictedICCProfile
     */
    public static RestrictedICCProfile createInstance (ICCCurveType c) {
        return new MonochromeInputRestrictedProfile(c);}

    /**
     * Construct a 1 component RestrictedICCProfile
     *   @param c Gray TRC curve
     */
    private MonochromeInputRestrictedProfile (ICCCurveType c) {
        super (c); }

    /**
     * Get the type of RestrictedICCProfile for this object
     * @return kMonochromeInput
     */
    public  int getType () {return kMonochromeInput;}

    /**
     * @return String representation of a MonochromeInputRestrictedProfile
     */
    public String toString () {
        StringBuffer rep = new StringBuffer ("Monochrome Input Restricted ICC profile" + eol);

        rep .append("trc[GRAY]:"   + eol).append(trc[GRAY]).append(eol);

        return rep.toString(); }

    /* end class MonochromeInputRestrictedProfile */ }








