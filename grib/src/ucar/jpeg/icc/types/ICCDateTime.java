/*****************************************************************************
 *
 * $Id: ICCDateTime.java,v 1.1 2002/07/25 14:56:31 grosbois Exp $
 *
 * Copyright Eastman Kodak Company, 343 State Street, Rochester, NY 14650
 * $Date $
 *****************************************************************************/

package ucar.jpeg.icc.types;

import java.io.IOException;
import java.io.RandomAccessFile;
import ucar.jpeg.icc .ICCProfile;

/**
 * Date Time format for tags
 * 
 * @version	1.0
 * @author	Bruce A. Kern
 */
public class ICCDateTime
{
    public final static int size = 6 * ICCProfile.short_size;

	/** Year datum.   */ public short wYear;     // Number of the actual year (i.e. 1994)
	/** Month datum.  */ public short wMonth;    // Number of the month (1-12)
	/** Day datum.    */ public short wDay;      // Number of the day
	/** Hour datum.   */ public short wHours;    // Number of hours (0-23)
	/** Minute datum. */ public short wMinutes;  // Number of minutes (0-59)
	/** Second datum. */ public short wSeconds;  // Number of seconds (0-59)

    /** Construct an ICCDateTime from parts */
    public ICCDateTime (short year, short month, short day, short hour, short minute, short second) {
        wYear = year; wMonth = month; wDay = day; 
        wHours = hour; wMinutes = minute; wSeconds = second; }

    /** Write an ICCDateTime to a file. */
    public void write (RandomAccessFile raf) throws IOException {
        raf.writeShort(wYear);
        raf.writeShort(wMonth);
        raf.writeShort(wDay);
        raf.writeShort(wHours);
        raf.writeShort(wMinutes);
        raf.writeShort(wSeconds); }

    /** Return a ICCDateTime representation. */
    public String toString () {
        String rep = "";
        return 
            String.valueOf(wYear) + "/" +
            String.valueOf(wMonth)   + "/" +
            String.valueOf(wDay) + " " +
            String.valueOf(wHours) + ":" +
            String.valueOf(wMinutes) + ":" +
            String.valueOf(wSeconds);
    }
    
    /* end class ICCDateTime*/ }





