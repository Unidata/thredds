/*****************************************************************************
 *
 * $Id: ICCProfileHeader.java,v 1.1 2002/07/25 14:56:31 grosbois Exp $
 *
 * Copyright Eastman Kodak Company, 343 State Street, Rochester, NY 14650
 * $Date $
 *****************************************************************************/

package ucar.jpeg.icc.types;

import java.io.RandomAccessFile;
import java.io.IOException;
import ucar.jpeg.icc .ICCProfile;


/**
 * An ICC profile contains a 128-byte header followed by a variable
 * number of tags contained in a tag table. This class models the header
 * portion of the profile.  Most fields in the header are ints.  Some, such
 * as data and version are aggregations of ints. This class provides an api to
 * those fields as well as the definition of standard constants which are used 
 * in the header.
 * 
 * @see		jj2000.j2k.icc.ICCProfile
 * @version	1.0
 * @author	Bruce A. Kern
 */

public class 
ICCProfileHeader 
{
    private static final String eol = System.getProperty("line.separator");
        
    /** ICCProfile header byte array. */  private byte [] header = null;
        
    /* Define the set of standard signature and type values. Only
     * those codes required for Restricted ICC use are defined here.
     */

    /** Profile header signature */ public static int  kdwProfileSignature;
    static { kdwProfileSignature = ICCProfile.getInt(new String ("acsp").getBytes(), 0); }

    /** Profile header signature */ public static int  kdwProfileSigReverse;
    static { kdwProfileSigReverse = ICCProfile.getInt(new String ("psca").getBytes(),0); }

    private static final String  kdwInputProfile	 = "scnr";
    private static final String  kdwDisplayProfile	 = "mntr";
    private static final String  kdwRGBData			 = "RGB ";
    private static final String  kdwGrayData		 = "GRAY";
    private static final String  kdwXYZData			 = "XYZ ";
    private static final String  kdwGrayTRCTag		 = "kTRC";
    private static final String  kdwRedColorantTag	 = "rXYZ";
    private static final String  kdwGreenColorantTag = "gXYZ";
    private static final String  kdwBlueColorantTag	 = "bXYZ";
    private static final String  kdwRedTRCTag		 = "rTRC";
    private static final String  kdwGreenTRCTag		 = "gTRC";
    private static final String  kdwBlueTRCTag		 = "bTRC";
    
    /* Offsets into ICCProfile header byte array. */

    private final static int offProfileSize = 0;
    private final static int offCMMTypeSignature         = offProfileSize + ICCProfile.int_size;
    private final static int offProfileVersion           = offCMMTypeSignature + ICCProfile.int_size;	   
    private final static int offProfileClass             = offProfileVersion + ICCProfileVersion.size;
    private final static int offColorSpaceType           = offProfileClass + ICCProfile.int_size;  
    private final static int offPCSType                  = offColorSpaceType + ICCProfile.int_size;		   
    private final static int offDateTime                 = offPCSType + ICCProfile.int_size;		   
    private final static int offProfileSignature         = offDateTime + ICCDateTime.size;
    private final static int offPlatformSignature        = offProfileSignature + ICCProfile.int_size;
    private final static int offCMMFlags                 = offPlatformSignature + ICCProfile.int_size; 
    private final static int offDeviceManufacturer       = offCMMFlags + ICCProfile.int_size;
    private final static int offDeviceModel              = offDeviceManufacturer + ICCProfile.int_size;		
    private final static int offDeviceAttributes1        = offDeviceModel + ICCProfile.int_size;
    private final static int offDeviceAttributesReserved = offDeviceAttributes1 + ICCProfile.int_size;
    private final static int offRenderingIntent          = offDeviceAttributesReserved + ICCProfile.int_size;
    private final static int offPCSIlluminant            = offRenderingIntent + ICCProfile.int_size;
    private final static int offCreatorSig               = offPCSIlluminant + XYZNumber.size;
    private final static int offReserved                 = offCreatorSig + ICCProfile.int_size;
    /** Size of the header */ public  final static int size = offReserved + 44 * ICCProfile.byte_size;

    /* Header fields mapped to primitive types. */
    /** Header field */ public int	dwProfileSize;			// Size of the entire profile in bytes	
    /** Header field */ public int	dwCMMTypeSignature;		// The preferred CMM for this profile
    /** Header field */ public int	dwProfileClass;			// Profile/Device class signature
    /** Header field */ public int	dwColorSpaceType;		// Colorspace signature
    /** Header field */ public int	dwPCSType;				// PCS type signature
    /** Header field */ public int	dwProfileSignature;		// Must be 'acsp' (0x61637370)
    /** Header field */ public int	dwPlatformSignature;	// Primary platform for which this profile was created
    /** Header field */ public int	dwCMMFlags;				// Flags to indicate various hints for the CMM
    /** Header field */ public int	dwDeviceManufacturer;	// Signature of device manufacturer
    /** Header field */ public int	dwDeviceModel;			// Signature of device model
    /** Header field */ public int	dwDeviceAttributes1;	// Attributes of the device
    /** Header field */ public int	dwDeviceAttributesReserved;
    /** Header field */ public int	dwRenderingIntent;		// Desired rendering intent for this profile
    /** Header field */ public int	dwCreatorSig;			// Profile creator signature

    /** Header field */ public byte[] reserved = new byte[44];          // 

    /* Header fields mapped to ggregate types. */
    /** Header field */ public ICCProfileVersion profileVersion; // Version of the profile format on which
    /** Header field */ public ICCDateTime	     dateTime;		 // Date and time of profile creation// this profile is based
    /** Header field */ public XYZNumber		 PCSIlluminant;	 // Illuminant used for this profile


    /** Construct and empty header */
    public ICCProfileHeader () {
    }
    
    /**
     * Construct a header from a complete ICCProfile
     *   @param byte [] -- holds ICCProfile contents
     */
    public ICCProfileHeader (byte [] data)  {

        dwProfileSize              = ICCProfile.getInt (data, offProfileSize);
        dwCMMTypeSignature         = ICCProfile.getInt (data, offCMMTypeSignature);
        dwProfileClass             = ICCProfile.getInt (data, offProfileClass);
        dwColorSpaceType           = ICCProfile.getInt (data, offColorSpaceType);
        dwPCSType                  = ICCProfile.getInt (data, offPCSType);
        dwProfileSignature         = ICCProfile.getInt (data, offProfileSignature);
        dwPlatformSignature        = ICCProfile.getInt (data, offPlatformSignature);
        dwCMMFlags                 = ICCProfile.getInt (data, offCMMFlags);
        dwDeviceManufacturer       = ICCProfile.getInt (data, offDeviceManufacturer);
        dwDeviceModel              = ICCProfile.getInt (data, offDeviceModel);
        dwDeviceAttributes1        = ICCProfile.getInt (data, offDeviceAttributesReserved);
        dwDeviceAttributesReserved = ICCProfile.getInt (data, offDeviceAttributesReserved);
        dwRenderingIntent          = ICCProfile.getInt (data, offRenderingIntent);
        dwCreatorSig               = ICCProfile.getInt (data, offCreatorSig);
        profileVersion             = ICCProfile.getICCProfileVersion(data, offProfileVersion);
        dateTime                   = ICCProfile.getICCDateTime(data, offDateTime);
        PCSIlluminant              = ICCProfile.getXYZNumber(data, offPCSIlluminant);

        for (int i=0; i<reserved.length; ++i)
            reserved[i] = data [offReserved+i]; }

    /**
     * Write out this ICCProfile header to a RandomAccessFile
     *   @param raf sink for data
     * @exception IOException
     */
    public void write (RandomAccessFile raf) throws IOException {

        raf.seek (offProfileSize);                raf.write (dwProfileSize);
        raf.seek (offCMMTypeSignature);           raf.write (dwCMMTypeSignature);
        raf.seek (offProfileVersion);             profileVersion.write (raf);
        raf.seek (offProfileClass);               raf.write (dwProfileClass);
        raf.seek (offColorSpaceType);             raf.write (dwColorSpaceType);
        raf.seek (offPCSType);                    raf.write (dwPCSType);
        raf.seek (offDateTime);                   dateTime.write (raf);
        raf.seek (offProfileSignature);           raf.write (dwProfileSignature);
        raf.seek (offPlatformSignature);          raf.write (dwPlatformSignature);
        raf.seek (offCMMFlags);                   raf.write (dwCMMFlags);
        raf.seek (offDeviceManufacturer);         raf.write (dwDeviceManufacturer);
        raf.seek (offDeviceModel);                raf.write (dwDeviceModel);
        raf.seek (offDeviceAttributes1);          raf.write (dwDeviceAttributes1);
        raf.seek (offDeviceAttributesReserved);   raf.write (dwDeviceAttributesReserved);
        raf.seek (offRenderingIntent);            raf.write (dwRenderingIntent);
        raf.seek (offPCSIlluminant);              PCSIlluminant.write (raf);
        raf.seek (offCreatorSig);                 raf.write (dwCreatorSig);
        raf.seek (offReserved);                   raf.write (reserved);  }


    /** String representation of class */
    public String toString() {
        StringBuffer rep = new StringBuffer ("[ICCProfileHeader: ");

        rep .append(eol + "         ProfileSize: " + Integer.toHexString(dwProfileSize));
        rep .append(eol + "    CMMTypeSignature: " + Integer.toHexString(dwCMMTypeSignature));
        rep .append(eol + "        ProfileClass: " + Integer.toHexString(dwProfileClass));
        rep .append(eol + "      ColorSpaceType: " + Integer.toHexString(dwColorSpaceType));
        rep .append(eol + "           dwPCSType: " + Integer.toHexString(dwPCSType));
        rep .append(eol + "  dwProfileSignature: " + Integer.toHexString(dwProfileSignature));
        rep .append(eol + " dwPlatformSignature: " + Integer.toHexString(dwPlatformSignature));
        rep .append(eol + "          dwCMMFlags: " + Integer.toHexString(dwCMMFlags));
        rep .append(eol + "dwDeviceManufacturer: " + Integer.toHexString(dwDeviceManufacturer));
        rep .append(eol + "       dwDeviceModel: " + Integer.toHexString(dwDeviceModel));
        rep .append(eol + " dwDeviceAttributes1: " + Integer.toHexString(dwDeviceAttributes1));
        rep .append(eol + "   dwRenderingIntent: " + Integer.toHexString(dwRenderingIntent));
        rep .append(eol + "        dwCreatorSig: " + Integer.toHexString(dwCreatorSig));
        rep .append(eol + "      profileVersion: " + profileVersion);
        rep .append(eol + "            dateTime: " + dateTime);
        rep .append(eol + "       PCSIlluminant: " + PCSIlluminant); 
        return rep.append("]").toString(); }

    /* end class ICCProfileHeader */ }









