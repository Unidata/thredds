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

// $Id: Grib2BitMapSection.java,v 1.12 2005/12/08 20:59:54 rkambic Exp $


package ucar.grib.grib2;


import ucar.grib.GribNumbers;
import ucar.unidata.io.RandomAccessFile;

/*
 * Grib2BitMapSection.java  1.0  08/02/2003
 * @author Robb Kambic
 *
 */

import java.io.IOException;

/**
 * A class that represents the BitMapSection of a GRIB product.
 * A bitmap is a boolean array that designates if the values is missing or not
 * in the final data array.  If the value is missing, then the determined missing
 * value is entered in that position in the final data array.
 *
 * @author  Robb Kambic
 * @version 1.0
 *
 */
public final class Grib2BitMapSection {

    /**
     * Length in bytes of BitMapSection section.
     */
    private final int length;

    /**
     * Number of this section, should be 6.
     */
    private final int section;

    /**
     * Bit-map indicator (see Code Table 6.0 and Note (1))
     */
    private final int bitMapIndicator;

    /**
     * The bit map.
     */
    private boolean[] bitmap = null;

  /**
   * data used to populate bitmap,
   */
    private byte[] data;

    /**
     *  numberOfPoints in bitmap
     */
    private int numberOfPoints;

    // *** constructors *******************************************************

    /**
     * Constructs a <tt>Grib2BitMapSection</tt> object from a RandomAccessFile.
     *
     * @param raf RandomAccessFile with Section BMS content
     * @param gds Grib2GridDefinitionSection
     * @throws IOException  if stream contains no valid GRIB file
     */
    public Grib2BitMapSection( boolean createBM, RandomAccessFile raf, Grib2GridDefinitionSection gds )
            throws IOException {

        long  sectionEnd = raf.getFilePointer();

        // octets 1-4 (Length of PDS)
        length = GribNumbers.int4(raf);
        //System.out.println( "BMS length=" + length );
        sectionEnd += length;

        // octet 5
        section = raf.read();
        //System.out.println( "BMS is 6, section=" + section );

        // octet 6
        bitMapIndicator = raf.read();
        //System.out.println( "BMS bitMapIndicator=" + bitMapIndicator );

        // no bitMap
        if (bitMapIndicator != 0) {
            return;
        }

        // skip data read and bitMap creation
        if( ! createBM ) {
          raf.seek(sectionEnd);
          return;
        }

        long t1 = System.currentTimeMillis();                
        data = new byte[this.length - 6];
        raf.read(data);

        numberOfPoints = gds.getGdsVars().getNumberPoints();

        /* Don't do this right now. Do it when we are asked for it
        int[] bitmask    = {
                128, 64, 32, 16, 8, 4, 2, 1
            };
        this.bitmap = new boolean[gds.getGdsVars().getNumberPoints()];
        for (int i = 0; i < this.bitmap.length; i++) {
            this.bitmap[i] = (data[i / 8] & bitmask[i % 8]) != 0;
        }
        */
        raf.seek(sectionEnd);
    }

    /**
     * Get bit map indicator.
     *
     * @return int
     */
    public final int getBitmapIndicator() {
        return this.bitMapIndicator;
    }

    /**
     * Get bit map.
     *
     * @return bit map as array of boolean values
     */
    public final boolean[] getBitmap() {
        // no bitMap
        if (bitMapIndicator != 0)
            return null;

        if(  this.bitmap == null && this.data != null) {
            // create new bit map when it is first asked for
            boolean[] tmpBitmap =  new boolean[numberOfPoints];
            int[] bitmask    = {
                128, 64, 32, 16, 8, 4, 2, 1
            };
            for (int i = 0; i < tmpBitmap.length; i++) {
                tmpBitmap[i] = (data[i / 8] & bitmask[i % 8]) != 0;
            }
            this.bitmap = tmpBitmap;
            this.data = null;
        }
        return this.bitmap;
    }
}

