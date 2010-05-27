/*
 * Copyright 1998-2009 University Corporation for Atmospheric Research/Unidata
 *
 * Portions of this software were developed by the Unidata Program at the
 * University Corporation for Atmospheric Research.
 *
 * Access and use of this software shall impose the following obligations
 * and understandings on the user. The user is granted the right, without
 * any fee or cost, to use, copy, modify, alter, enhance and distribute
 * this software, and any derivative works thereof, and its supporting
 * documentation for any purpose whatsoever, provided that this entire
 * notice appears in all copies of the software, derivative works and
 * supporting documentation.  Further, UCAR requests that the user credit
 * UCAR/Unidata in any publications that result from the use of this
 * software or in any product that includes this software. The names UCAR
 * and/or Unidata, however, may not be used in any advertising or publicity
 * to endorse or promote any products or commercial entity unless specific
 * written permission is obtained from UCAR/Unidata. The user also
 * understands that UCAR/Unidata is not obligated to provide the user with
 * any support, consulting, training or assistance of any kind with regard
 * to the use, operation and performance of this software nor to provide
 * the user with any updates, revisions, new versions or "bug fixes."
 *
 * THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 * INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 * FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 * NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 * WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */

// $Id: Grib1EndSection.java,v 1.8 2005/12/02 23:53:42 rkambic Exp $


package ucar.grib.grib1;


import ucar.grib.*;

/*
 * Grib1EndSection.java  1.0  09/30/2004
 * @author Robb Kambic
 *
 */

import ucar.unidata.io.RandomAccessFile;

import java.io.IOException;


/**
 * A class that represents the EndSection of a GRIB1 product.
 *
 */

final class Grib1EndSection {
    /*
     * was the grib endding 7777 found.
    */

    /** _more_          */
    private boolean endFound = false;

    /*
     * how long was the ending, should be 4 bytes.
    */

    /** _more_          */
    private int length = 0;

    // *** constructors *******************************************************

    /**
     * Constructs a <tt>Grib1EndSection</tt> object from a byteBuffer.
     *
     * @param raf RandomAccessFile with EndSection content
     *
     * @throws IOException
     */
    public Grib1EndSection(RandomAccessFile raf) throws IOException {
        int match = 0;
        while (raf.getFilePointer() < raf.length()) {
            // code must be "7" "7" "7" "7"
            byte c = raf.readByte();
            //System.out.println( "c=" + (char) c );
            length++;
            if (c == '7') {
                match += 1;
                //System.out.println( "seekEnd raf.getFilePointer()=" + raf.getFilePointer() );
            } else {
                //System.out.println( "c=" + (char) c );
                match = 0;  /* Needed to protect against bad ending case. */
            }
            if (match == 4) {
                endFound = true;
                //System.out.println( "7777 ending found" );
                break;
            }
        }
    }  // end Grib1EndSection

    /**
     * Get ending flag for Grib record.
     *
     * @return true if  "7777" found
     */
    public final boolean getEndFound() {
        return endFound;
    }

    // --Commented out by Inspection START (11/17/05 1:32 PM):
    //   /**
    //    * how long was the ending, should be 4 bytes.
    //    * @return int
    //   */
    //   public static final int getLength()
    //   {
    //      return length;
    //   }
    // --Commented out by Inspection STOP (11/17/05 1:32 PM)
}  // end Grib1EndSection


