/*****************************************************************************
 *
 * $Id: ICCTag.java,v 1.1 2002/07/25 14:56:37 grosbois Exp $
 *
 * Copyright Eastman Kodak Company, 343 State Street, Rochester, NY 14650
 * $Date $
 *****************************************************************************/

package ucar.jpeg.icc.tags;

import java.util.Vector;
import ucar.jpeg.icc .ICCProfile;

/**
 * An ICC profile contains a 128-byte header followed by a variable
 * number of tags contained in a tag table. Each tag is a structured 
 * block of ints. The tags share a common format on disk starting with 
 * a signature, an offset to the tag data, and a length of the tag data.
 * The tag data itself is found at the given offset in the file and 
 * consists of a tag type int, followed by a reserved int, followed by
 * a data block, the structure of which is unique to the tag type.
 * <p>
 * This class is the abstract super class of all tags. It models that 
 * part of the structure which is common among tags of all types.<p>
 * It also contains the definitions of the various tag types.
 *
 * 
 * @see	jj2000.j2k.icc.tags.ICCTagTable
 * @version	1.0
 * @author	Bruce A. Kern
 */
public abstract class ICCTag
{

    // Tag Signature Strings
    private static final String sdwCprtSignature = "cprt";
    private static final String sdwDescSignature = "desc";
    private static final String sdwWtPtSignature = "wtpt";
    private static final String sdwBkPtSignature = "bkpt";
    private static final String sdwRXYZSignature = "rXYZ";
    private static final String sdwGXYZSignature = "gXYZ";
    private static final String sdwBXYZSignature = "bXYZ";
    private static final String sdwKXYZSignature = "kXYZ";
    private static final String sdwRTRCSignature = "rTRC";
    private static final String sdwGTRCSignature = "gTRC";
    private static final String sdwBTRCSignature = "bTRC";
    private static final String sdwKTRCSignature = "kTRC";
    private static final String sdwDmndSignature = "dmnd";
    private static final String sdwDmddSignature = "dmdd";

    // Tag Signatures
    private final static int kdwCprtSignature = ICCProfile.getInt(sdwCprtSignature.getBytes(), 0);
    private final static int kdwDescSignature = ICCProfile.getInt(sdwDescSignature.getBytes(), 0);
    private final static int kdwWtPtSignature = ICCProfile.getInt(sdwWtPtSignature.getBytes(), 0);
    private final static int kdwBkPtSignature = ICCProfile.getInt(sdwBkPtSignature.getBytes(), 0);
    private final static int kdwRXYZSignature = ICCProfile.getInt(sdwRXYZSignature.getBytes(), 0);
    private final static int kdwGXYZSignature = ICCProfile.getInt(sdwGXYZSignature.getBytes(), 0);
    private final static int kdwBXYZSignature = ICCProfile.getInt(sdwBXYZSignature.getBytes(), 0);
    private final static int kdwKXYZSignature = ICCProfile.getInt(sdwKXYZSignature.getBytes(), 0);
    private final static int kdwRTRCSignature = ICCProfile.getInt(sdwRTRCSignature.getBytes(), 0);
    private final static int kdwGTRCSignature = ICCProfile.getInt(sdwGTRCSignature.getBytes(), 0);
    private final static int kdwBTRCSignature = ICCProfile.getInt(sdwBTRCSignature.getBytes(), 0);
    private final static int kdwKTRCSignature = ICCProfile.getInt(sdwKTRCSignature.getBytes(), 0);
    private final static int kdwDmndSignature = ICCProfile.getInt(sdwDmndSignature.getBytes(), 0);
    private final static int kdwDmddSignature = ICCProfile.getInt(sdwDmddSignature.getBytes(), 0);

    // Tag Type Strings
    private static final String sdwTextDescType     = "desc";
    private static final String sdwTextType         = "text";
    private static final String sdwCurveType        = "curv";
    private static final String sdwCurveTypeReverse = "vruc";
    private static final String sdwXYZType          = "XYZ ";
    private static final String sdwXYZTypeReverse   = " ZYX";

    // Tag Types
    private final static int kdwTextDescType      = ICCProfile.getInt(sdwTextDescType.getBytes(), 0);
    private final static int kdwTextType          = ICCProfile.getInt(sdwTextType.getBytes(), 0);
    private final static int kdwCurveType		  = ICCProfile.getInt(sdwCurveType.getBytes(), 0);
    private final static int kdwCurveTypeReverse  = ICCProfile.getInt(sdwCurveTypeReverse.getBytes(), 0);
    private final static int kdwXYZType		      = ICCProfile.getInt(sdwXYZType.getBytes(), 0);
    private final static int kdwXYZTypeReverse    = ICCProfile.getInt(sdwXYZTypeReverse.getBytes(), 0);

	/** Tag id                            */ public final int signature; // Tag signature
    /** Tag type                          */ public final int type;
	/** Tag data                          */ public final byte [] data; // Tag type
    /** offset to tag data in the array   */ public final int offset;
    /** size of the tag data in the array */ public final int count;

    /**
      * Create a string representation of the tag type
      *   @param type input
      * @return String representation of the type
      */
    public static String typeString (int type) {

        if      (type==kdwTextDescType) return sdwTextDescType;
        else if (type==kdwTextType) return sdwTextDescType;
        else if (type==kdwCurveType) return sdwCurveType;
        else if (type==kdwCurveTypeReverse) return sdwCurveTypeReverse;
        else if (type==kdwXYZType) return sdwXYZType;
        else if (type==kdwXYZTypeReverse) return sdwXYZTypeReverse;
        else return "bad tag type"; }


    /**
      * Create a string representation of the signature
      *   @param signature input
      * @return String representation of the signature
      */
    public static String signatureString (int signature) {

        if      (signature==kdwCprtSignature) return sdwCprtSignature;
        else if (signature==kdwDescSignature) return sdwDescSignature;
        else if (signature==kdwWtPtSignature) return sdwWtPtSignature;
        else if (signature==kdwBkPtSignature) return sdwBkPtSignature;
        else if (signature==kdwRXYZSignature) return sdwRXYZSignature;
        else if (signature==kdwGXYZSignature) return sdwGXYZSignature;
        else if (signature==kdwBXYZSignature) return sdwBXYZSignature;
        else if (signature==kdwRTRCSignature) return sdwRTRCSignature;
        else if (signature==kdwGTRCSignature) return sdwGTRCSignature;
        else if (signature==kdwBTRCSignature) return sdwBTRCSignature;
        else if (signature==kdwKTRCSignature) return sdwKTRCSignature;
        else if (signature==kdwDmndSignature) return sdwDmndSignature;
        else if (signature==kdwDmddSignature) return sdwDmddSignature;
        else return "bad tag signature"; }


    /**
     * Factory method for creating a tag of a specific type.
     *   @param signature tag to create
     *   @param data byte array containg embedded tag data
     *   @param offset to tag data in the array
     *   @param count size of tag data in bytes
     * @return specified ICCTag
     */
    public static ICCTag createInstance (int signature, byte [] data, int offset, int count) {
            
        int type = ICCProfile.getInt(data,offset);

        if       (type==kdwTextDescType)     return new ICCTextDescriptionType(signature, data, offset, count);
        else  if (type==kdwTextType)         return new ICCTextType(signature, data, offset, count);
        else  if (type==kdwXYZType)          return new ICCXYZType(signature, data, offset, count);
        else  if (type==kdwXYZTypeReverse)   return new ICCXYZTypeReverse(signature, data, offset, count);
        else  if (type==kdwCurveType)        return new ICCCurveType(signature, data, offset, count);
        else  if (type==kdwCurveTypeReverse) return new ICCCurveTypeReverse(signature, data, offset, count);
        else                                 throw  new IllegalArgumentException ("bad tag type"); }

    
    /**
     * Ued by subclass initialization to store the state common to all tags
     *   @param signature tag being created
     *   @param data byte array containg embedded tag data
     *   @param offset to tag data in the array
     *   @param count size of tag data in bytes
     */
    protected ICCTag (int signature, byte [] data, int offset, int count) { 
        this.signature = signature;
        this.data = data;
        this.offset = offset;
        this.count = count;
        this.type = ICCProfile.getInt(data,offset); }

    public String toString () {
        return signatureString(signature) + ":" + typeString(type); }
    
    /* end class ICCTag */ }















