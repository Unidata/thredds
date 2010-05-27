/*****************************************************************************
 *
 * $Id: XYZNumber.java,v 1.1 2002/07/25 14:56:31 grosbois Exp $
 *
 * Copyright Eastman Kodak Company, 343 State Street, Rochester, NY 14650
 * $Date $
 *****************************************************************************/

package ucar.jpeg.icc.types;

import java.io.IOException;
import java.io.RandomAccessFile;
import ucar.jpeg.icc .ICCProfile;

/**
 * A convientient representation for the contents of the
 * ICCXYZTypeTag class.
 * 
 * @see		jj2000.j2k.icc.tags.ICCXYZType
 * @version	1.0
 * @author	Bruce A. Kern
 */
public class XYZNumber
{
    public final static int size = 3 * ICCProfile.int_size;

	/** x value */public int dwX;	// X tristimulus value
	/** y value */public int dwY;	// Y tristimulus value
	/** z value */public int dwZ;	// Z tristimulus value

    /** Construct from constituent parts. */
    public XYZNumber (int x, int y, int z) {
        dwX = x; dwY = y; dwZ = z; }

    /** Normalization utility */
    public static int DoubleToXYZ ( double x ) {
        return (int) Math.floor(x * 65536.0 + 0.5); }

    /** Normalization utility */
    public static double XYZToDouble (int x) { 
        return (double)x / 65536.0; }

    /** Write to a file */
    public void write (RandomAccessFile raf) throws IOException {
        raf.writeInt (dwX);
        raf.writeInt (dwY);
        raf.writeInt (dwZ); }

    /** String representation of class instance. */
    public String toString () {
        return "[" + dwX + ", " + dwY + ", " + dwZ + "]"; }

    
    /* end class XYZNumber */ }






