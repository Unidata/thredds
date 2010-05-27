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

// $Id: Grib2IndicatorSection.java,v 1.13 2005/12/05 19:24:21 rkambic Exp $


package ucar.grib.grib2;


import ucar.grib.GribNumbers;
import ucar.grib.NotSupportedException;

/*
 * Grib2IndicatorSection.java  1.0  07/25/2003
 * @author  Robb Kambic
 * @version 1.0
 *
 */

import ucar.unidata.io.RandomAccessFile;

import java.io.IOException;


/**
 * A class that represents the IndicatorSection of a GRIB record.
 * The first section of a Grib record
 *
 */

public final class Grib2IndicatorSection {

    /**
     * Length in bytes of GRIB record.
     */
    private long gribLength;

    /**
     * Length in bytes of IndicatorSection.
     * Section length differs between GRIB editions 1 and 2
     * Currently only GRIB edition 2 supported - length is 16 octets/bytes.
     */
    private int length;

    /**
     * Discipline - GRIB Master Table Number.
     */
    private int discipline;

    /**
     * Edition of GRIB specification used.
     */
    private final int edition;


    // *** constructors *******************************************************

    /**
     * Constructs a <tt>Grib2IndicatorSection</tt> object from a byteBuffer.
     *
     * @param raf RandomAccessFile with IndicatorSection content
     *
     * @throws IOException  if raf contains no valid GRIB file
     * @throws NotSupportedException _more_
     */
    public Grib2IndicatorSection(RandomAccessFile raf)  throws IOException {

        //if Grib edition 1, get bytes for the gribLength
        int[] data = new int[3];
        for (int i = 0; i < 3; i++) {
            data[i] = raf.read();
        }
        //System.out.println( "data[]=" + data[0] +", "+ data[1]+", "+data[2] ) ;

        // edition of GRIB specification
        edition = raf.read();
        //System.out.println( "edition=" + edition ) ;
        if (edition == 1) {
            // length of GRIB record
            gribLength = (long) GribNumbers.uint3(data[0], data[1], data[2]);
            //System.out.println( "Error Grib 1 record in Grib2 file" ) ;
            //System.out.println( "edition 1 gribLength=" + gribLength ) ;
            // skip the grib1 record
            raf.seek( raf.getFilePointer() + gribLength -4 );
            length = 8;

        } else if (edition == 2) {
            // length of GRIB record
            discipline = data[2];
            //System.out.println( "discipline=" + discipline) ;
            gribLength = GribNumbers.int8(raf);
            //System.out.println( "editon 2 gribLength=" + gribLength) ;
            length = 16;
        } else {
            throw new NotSupportedException("GRIB edition " + edition
                                            + " is not yet supported");
        }
    }  // end Grib2IndicatorSection

    /**
     * Get the byte length of this GRIB record.
     *
     * @return length in bytes of GRIB record
     */
    public final long getGribLength() {
        return gribLength;
    }

    /**
     * Get the byte length of the IndicatorSection0 section.
     *
     * @return length in bytes of IndicatorSection0 section
     */
    public final int getLength()
    {
       return length;
    }

    /**
     * Discipline - GRIB Master Table Number.
     * @return discipline number
     */
    public final int getDiscipline() {
        return discipline;
    }

    /**
     * Discipline - GRIB Master Table Name.
     * @return disciplineName
     */
    public final String getDisciplineName() {
        switch (discipline) {

          case 0 :
              return "Meteorological products";

          case 1 :
              return "Hydrological products";

          case 2 :
              return "Land surface products";

          case 3 :
              return "Space products";

          case 10 :
              return "Oceanographic products";

          default :
              return "Unknown";
        }
    }


    /**
     * Get the edition of the GRIB specification used.
     *
     * @return edition number of GRIB specification
     */
    public final int getGribEdition() {
        return edition;
    }
}

