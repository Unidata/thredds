/*
 * Copyright 1997-2004 Unidata Program Center/University Corporation for
 * Atmospheric Research, P.O. Box 3000, Boulder, CO 80307,
 * support@unidata.ucar.edu.
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or (at
 * your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser
 * General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
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
            bms = new Grib2BitMapSection(raf, gds);  // Section 6
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

            bms = new Grib2BitMapSection(raf, gds);         // Section 6

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
