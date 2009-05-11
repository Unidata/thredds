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


package ucar.nc2.iosp.gempak;


import ucar.grib.NoValidGribException;
import ucar.grib.NotSupportedException;
import ucar.grib.QuasiRegular;

import ucar.grib.grib2.*;

import ucar.unidata.io.RandomAccessFile;

import java.io.IOException;


/**
 * A class used to extract Grib2 data from a Gempak file.
 * see <a href="../../../IndexFormat.txt"> IndexFormat.txt</a>
 * @author Robb Kambic
 */
public final class GempakGrib2Data {

    /**
     *  used to hold open file descriptor
     */
    private RandomAccessFile raf = null;

    /**
     *  Expand Quasi-Regular grids
     */
    private boolean expandQuasi = true;


    /**
     * Constructs a  GempakGrib2Data object for a RandomAccessFile.
     *
     * @param raf ucar.unidata.io.RandomAccessFile with GRIB content
     */
    public GempakGrib2Data(RandomAccessFile raf) {
        this.raf = raf;
    }

    /**
     * Constructs a  GempakGrib2Data object for a RandomAccessFile.
     *
     * @param raf ucar.unidata.io.RandomAccessFile with GRIB content.
     * @param expandQuasi  whether to expand Quasi grids, default is true.
     */
    public GempakGrib2Data(RandomAccessFile raf, boolean expandQuasi) {
        this.raf         = raf;
        this.expandQuasi = expandQuasi;
    }

    /**
     * Reads the Grib data with a certain offsets in the file.
     *
     * @param start starting point for reads
     * @throws IOException  if raf does not contain a valid GRIB record.
     * @return float[]
     */
    public final float[] getData(long start) throws IOException {
        long                           time = System.currentTimeMillis();
        Grib2IdentificationSection     id   = null;
        Grib2LocalUseSection           lus  = null;
        Grib2GridDefinitionSection     gds  = null;
        Grib2ProductDefinitionSection  pds  = null;
        Grib2DataRepresentationSection drs  = null;
        Grib2BitMapSection             bms  = null;
        Grib2DataSection               ds   = null;

        raf.order(raf.BIG_ENDIAN);
        raf.seek(start);
        int secLength = raf.readInt();
        if (secLength > 0) {
            id = new Grib2IdentificationSection(raf);  // Section 1
        }
        secLength = raf.readInt();
        if (secLength > 0) {
            // check for Local Use Section 2
            lus = new Grib2LocalUseSection(raf);
        }

        secLength = raf.readInt();
        if (secLength > 0) {
            // Need section 3, 4, 5, 6, and 7 to read/interpet the data
            gds = new Grib2GridDefinitionSection(raf, false);  // Section 3 no checksum
        }
        secLength = raf.readInt();
        if (secLength > 0) {
            pds = new Grib2ProductDefinitionSection(raf);  // Section 4
        }

        secLength = raf.readInt();
        if (secLength > 0) {
            drs = new Grib2DataRepresentationSection(raf);  // Section 5
        }

        secLength = raf.readInt();
        if (secLength > 0) {
            bms = new Grib2BitMapSection(true, raf, gds);  // Section 6
        }
        if (bms.getBitmapIndicator() == 254) {  //previously defined in the same GRIB2 record
            long offset = raf.getFilePointer();
            //raf.seek(GdsOffset);                // go get it
            //Grib2GridDefinitionSection savegds = gds;
            gds = new Grib2GridDefinitionSection(raf, false);
            Grib2ProductDefinitionSection savepds = pds;
            pds = new Grib2ProductDefinitionSection(raf);   // Section 4

            Grib2DataRepresentationSection savedrs = drs;
            drs = new Grib2DataRepresentationSection(raf);  // Section 5

            bms = new Grib2BitMapSection(true, raf, gds);         // Section 6

            // reset pds, drs
            pds = savepds;
            drs = savedrs;
            raf.seek(offset);
        }

        secLength = raf.readInt();
        if (secLength > 0) {
            ds = new Grib2DataSection(true, raf, gds, drs, bms);  // Section 7
        }
        //System.out.println("DS offset=" + ds.getOffset() );

        // not a quasi grid or don't expand Quasi
        if ((gds.getOlon() == 0) || !expandQuasi) {
            return ds.getData();
        } else {
            QuasiRegular qr = new QuasiRegular(ds.getData(), (Object) gds);
            return qr.getData();
        }
    }  // end getData

}  // end GempakGrib2Data


