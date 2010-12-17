/*****************************************************************************
 *
 * $Id: ICCMonochromeInputProfile.java,v 1.1 2002/07/25 14:56:54 grosbois Exp $
 *
 * Copyright Eastman Kodak Company, 343 State Street, Rochester, NY 14650
 * $Date $
 *****************************************************************************/

package ucar.jpeg.icc;

import java.io.InputStream;
import java.io.File;
import java.io.IOException;
import ucar.jpeg.colorspace .ColorSpace;
import ucar.jpeg.colorspace .ColorSpaceException;
import ucar.jpeg.jj2000.j2k.io.RandomAccessIO;

/**
 * The monochrome ICCProfile.
 * 
 * @version	1.0
 * @author	Bruce A. Kern
 */
public class ICCMonochromeInputProfile extends ICCProfile {
        
    /**
     * Return the ICCProfile embedded in the input image
     *   @param in jp2 image with embedded profile
     * @return ICCMonochromeInputProfile 
     * @exception ColorSpaceICCProfileInvalidExceptionException
     * @exception 
     */
    public static ICCMonochromeInputProfile createInstance (ColorSpace csm) 
        throws ColorSpaceException, ICCProfileInvalidException {
        return new ICCMonochromeInputProfile (csm); }
    
    /**
     * Construct a ICCMonochromeInputProfile corresponding to the profile file
     *   @param f disk based ICCMonochromeInputProfile
     * @return theICCMonochromeInputProfile
     * @exception ColorSpaceException
     * @exception ICCProfileInvalidException
     */
    protected ICCMonochromeInputProfile (ColorSpace csm) 
        throws ColorSpaceException, ICCProfileInvalidException {
            super (csm);  }
    
    /* end class ICCMonochromeInputProfile */ }
