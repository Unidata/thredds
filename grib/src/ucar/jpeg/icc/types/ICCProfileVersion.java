/*****************************************************************************
 *
 * $Id: ICCProfileVersion.java,v 1.1 2002/07/25 14:56:31 grosbois Exp $
 *
 * Copyright Eastman Kodak Company, 343 State Street, Rochester, NY 14650
 * $Date $
 *****************************************************************************/

package ucar.jpeg.icc.types;

import java.io.IOException;
import java.io.RandomAccessFile;
import ucar.jpeg.icc .ICCProfile;

/**
 * This class describes the ICCProfile Version as contained in
 * the header of the ICC Profile.
 * 
 * @see		jj2000.j2k.icc.ICCProfile
 * @see		jj2000.j2k.icc.types.ICCProfileHeader
 * @version	1.0
 * @author	Bruce A. Kern
 */
public class ICCProfileVersion {
    /** Field size */ public final static int size = 4 * ICCProfile.byte_size;
     
	/** Major revision number in binary coded decimal */   public byte uMajor;
    /** Minor revision in high nibble, bug fix revision           
        in low nibble, both in binary coded decimal   */   public byte uMinor;
	
    private byte reserved1;
    private byte reserved2;

    /** Construct from constituent parts. */
    public ICCProfileVersion (byte major, byte minor, byte res1, byte res2) {
        uMajor = major; uMinor = minor; reserved1 = res1; reserved2 = res2; }

    /** Construct from file content. */
    public void write (RandomAccessFile raf) throws IOException {
        raf.write(uMajor); raf.write(uMinor); raf.write(reserved1); raf.write(reserved2);  }

    /** String representation of class instance. */
    public String toString () {
        return "Version " + uMajor + "." + uMinor; }

    /* end class ICCProfileVersion */ }



