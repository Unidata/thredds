/*****************************************************************************
 *
 * $Id: ICCMatrixBasedInputProfile.java,v 1.1 2002/07/25 14:56:54 grosbois Exp $
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
 * This class enables an application to construct an 3 component ICCProfile
 * 
 * @version	1.0
 * @author	Bruce A. Kern
 */

public class ICCMatrixBasedInputProfile extends ICCProfile {
     
    /**
     * Factory method to create ICCMatrixBasedInputProfile based on a
     * suppled profile file.
     *   @param f contains a disk based ICCProfile.
     * @return the ICCMatrixBasedInputProfile
     * @exception ICCProfileInvalidException
     * @exception ColorSpaceException
     */
    public static ICCMatrixBasedInputProfile createInstance (ColorSpace csm) 
        throws ColorSpaceException, ICCProfileInvalidException {
        return new ICCMatrixBasedInputProfile (csm); }

    /**
     * Construct an ICCMatrixBasedInputProfile based on a
     * suppled profile file.
     *   @param f contains a disk based ICCProfile.
     * @exception ColorSpaceException
     * @exception ICCProfileInvalidException
     */
    protected ICCMatrixBasedInputProfile (ColorSpace csm)
        throws ColorSpaceException, ICCProfileInvalidException {
            super (csm); }
    
    /* end class ICCMatrixBasedInputProfile */ }
















