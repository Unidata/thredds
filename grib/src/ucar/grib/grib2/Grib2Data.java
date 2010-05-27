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

// $Id: Grib2Data.java,v 1.19 2005/12/12 18:22:40 rkambic Exp $


package ucar.grib.grib2;


import ucar.grib.QuasiRegular;

import ucar.unidata.io.RandomAccessFile;

/*
 * Grib2Data.java  1.0  10/25/2003
 * @author Robb Kambic
 *
 */

import java.io.IOException;


/**
 * A class used to extract data from a Grib2 file.
 * see <a href="../../../IndexFormat.txt"> IndexFormat.txt</a>
 */
public final class Grib2Data {
    /*
     *  the raf of the Grib file to get the data
     */
    private final RandomAccessFile raf;

    // *** constructors *******************************************************

    /**
     * Constructs a  Grib2Data object for a RandomAccessFile.
     *
     * @param raf ucar.unidata.io.RandomAccessFile with GRIB content
     */
    public Grib2Data(RandomAccessFile raf) {
        this.raf = raf;
    }

    /**
     * Reads the Grib data with a certain offsets in the file.
     *
     * @param GdsOffset position in record where GDS starts
     * @param PdsOffset position in record where PDS starts
     * @throws IOException  if raf does not contain a valid GRIB record.
     * @return float[] the data
     */
    public final float[] getData(long GdsOffset, long PdsOffset)
            throws IOException {
        //long start = System.currentTimeMillis();

        /*
         *  Expand Quasi-Regular grids
        */
        boolean expandQuasi = true;
        raf.seek(GdsOffset);

        // Need section 3, 4, 5, 6, and 7 to read/interpet the data
        Grib2GridDefinitionSection gds = new Grib2GridDefinitionSection(raf,
                                             false);  // Section 3 no checksum

        raf.seek(PdsOffset);  // could have more than one pds for a gds
        Grib2ProductDefinitionSection pds =
            new Grib2ProductDefinitionSection(raf);  // Section 4

        Grib2DataRepresentationSection drs =
            new Grib2DataRepresentationSection(raf);  // Section 5

        Grib2BitMapSection bms = new Grib2BitMapSection(true, raf, gds);  // Section 6
        if( bms.getBitmapIndicator() == 254 ) { //previously defined in the same GRIB2 record
            long offset = raf.getFilePointer();
            raf.seek(GdsOffset);                // go get it
            //Grib2GridDefinitionSection savegds = gds;
            gds = new Grib2GridDefinitionSection(raf, false);
            Grib2ProductDefinitionSection savepds =  pds;
            pds =  new Grib2ProductDefinitionSection(raf);  // Section 4

            Grib2DataRepresentationSection savedrs = drs;
            drs = new Grib2DataRepresentationSection(raf);  // Section 5

            bms = new Grib2BitMapSection(true, raf, gds);  // Section 6

            // reset pds, drs
            pds = savepds;
            drs = savedrs;
            raf.seek( offset );
        }

        // Get the data
        Grib2DataSection ds = new Grib2DataSection(true, raf, gds, drs, bms);  // Section 7
        //System.out.println("DS offset=" + ds.getOffset() );

        // not a quasi grid or don't expand Quasi
        if ((gds.getGdsVars().getOlon() == 0) || !expandQuasi) {
            return ds.getData();
        } else {
            QuasiRegular qr = new QuasiRegular(ds.getData(), (Object)gds);
            return qr.getData();
        }
    }  // end getData
}  // end Grib2Data


