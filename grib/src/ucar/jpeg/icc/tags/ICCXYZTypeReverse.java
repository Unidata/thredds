/*****************************************************************************
 *
 * $Id: ICCXYZTypeReverse.java,v 1.1 2002/07/25 14:56:38 grosbois Exp $
 *
 * Copyright Eastman Kodak Company, 343 State Street, Rochester, NY 14650
 * $Date $
 *****************************************************************************/

package ucar.jpeg.icc.tags;

import java.io.IOException;
import java.util.Vector;
import java.io.RandomAccessFile;
import ucar.jpeg.icc .ICCProfile;
import ucar.jpeg.icc .types.XYZNumber;

/**
 * A tag containing a triplet.
 * 
 * @see		jj2000.j2k.icc.tags.ICCXYZType
 * @see	    jj2000.j2k.icc.types.XYZNumber
 * @version	1.0
 * @author	Bruce A. Kern
 */
public class ICCXYZTypeReverse extends ICCXYZType {

    /** x component */ public final long x;
    /** y component */ public final long y;
    /** z component */ public final long z;

    /**
     * Construct this tag from its constituant parts
     *   @param signature tag id
     *   @param data array of bytes
     *   @param offset to data in the data array
     *   @param length of data in the data array
     */
    protected ICCXYZTypeReverse (int signature, byte [] data, int offset, int length) {
        super (signature, data, offset, length);
        z=ICCProfile.getInt (data, offset+2*ICCProfile.int_size);
        y=ICCProfile.getInt (data, offset+3*ICCProfile.int_size);
        x=ICCProfile.getInt (data, offset+4*ICCProfile.int_size); }


    /** Return the string rep of this tag. */
    public String toString () {
        return "[" + super.toString() + "(" + x + ", " + y + ", " + z + ")]"; }

    /* end class ICCXYZTypeReverse */ }











