/*****************************************************************************
 *
 * $Id: ICCTextType.java,v 1.1 2002/07/25 14:56:37 grosbois Exp $
 *
 * Copyright Eastman Kodak Company, 343 State Street, Rochester, NY 14650
 * $Date $
 *****************************************************************************/

package ucar.jpeg.icc.tags;

import java.util.Vector;
import ucar.jpeg.icc .ICCProfile;

/**
 * A text based ICC tag
 * 
 * @version	1.0
 * @author	Bruce A. Kern
 */
public class ICCTextType extends ICCTag {

    /** Tag fields */ public final int type;
    /** Tag fields */ public final int reserved;
    /** Tag fields */  public final byte[] ascii;

    /**
     * Construct this tag from its constituant parts
     *   @param signature tag id
     *   @param data array of bytes
     *   @param offset to data in the data array
     *   @param length of data in the data array
     */
    protected ICCTextType (int signature, byte [] data, int offset, int length) {
        super (signature, data, offset, length);
        type = ICCProfile.getInt (data, offset);
        offset += ICCProfile.int_size;
        reserved = ICCProfile.getInt (data, offset);
        offset += ICCProfile.int_size;
        int size = 0;
        while (data[offset+size]!=0) ++size;
        ascii = new byte [size];
        System.arraycopy (data,offset,ascii,0,size); }

    /** Return the string rep of this tag. */
    public String toString () {
        return "[" + super.toString() + " \"" + new String (ascii) + "\"]"; }

    /* end class ICCTextType */ }









