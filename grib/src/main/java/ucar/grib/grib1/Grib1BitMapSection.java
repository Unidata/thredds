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

// $Id: Grib1BitMapSection.java,v 1.9 2005/12/02 23:53:42 rkambic Exp $

/*
 * Grib1BitMapSection.java  1.0  10/10/2004
 * @author Robb Kambic
 *
 */

package ucar.grib.grib1;


import ucar.grib.*;

import ucar.unidata.io.RandomAccessFile;

import java.io.IOException;


/**
 * A class that represents the bitmap section (BMS) of a GRIB record. It
 * indicates grid points where no grid value is defined by a 0.
 *
 * @version 1.0
 */

public final class Grib1BitMapSection {

    /**
     * Length in bytes of this section.
     */
    private final int length;

    /**
     * The bit map.
     */
    private boolean[] bitmap;


    // *** constructors *******************************************************

    /**
     * Constructs a <tt> Grib1BitMapSection</tt> object from a raf input stream.
     *
     * @param raf input stream with BMS content
     *
     * @throws IOException if stream can not be opened etc.
     * @throws  IOException  if stream contains no valid GRIB file
     */
    public Grib1BitMapSection(RandomAccessFile raf) throws IOException {
        int[] bitmask = {
            128, 64, 32, 16, 8, 4, 2, 1
        };

        // octet 1-3 (length of section)
        length = GribNumbers.uint3(raf);
        //System.out.println( "BMS length = " + length );

        // octet 4 unused bits
        int unused = raf.read();
        //System.out.println( "BMS unused = " + unused );

        // octets 5-6
        int bm = raf.readShort();
        if (bm != 0) {
            System.out.println("BMS pre-defined BM provided by center");
            if ((length - 6) == 0) {
                return;
            }
            byte[] data = new byte[length - 6];
            raf.read(data);
            return;
        }
        byte[] data = new byte[length - 6];
        raf.read(data);

        // create new bit map, octet 4 contains number of unused bits at the end
        bitmap = new boolean[(length - 6) * 8 - unused];
        //System.out.println( "BMS bitmap.length = " + bitmap.length );

        // fill bit map
        for (int i = 0; i < bitmap.length; i++) {
            bitmap[i] = (data[i / 8] & bitmask[i % 8]) != 0;
        }
    }  // end Grib1BitMapSection

    /**
     * Get bit map.
     *
     * @return bit map as array of boolean values
     */
    public final boolean[] getBitmap() {
        return bitmap;
    }
}  // end Grib1BitMapSection


