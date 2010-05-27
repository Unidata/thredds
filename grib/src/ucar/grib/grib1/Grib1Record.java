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

// $Id: Grib1Record.java,v 1.6 2005/12/02 23:53:43 rkambic Exp $


package ucar.grib.grib1;


/**
 * Grib1Record contains all the sections of a Grib record.
 * @author Robb Kambic  11/13/03
 */
public final class Grib1Record {

    /** _more_          */
    private final String header;

    /**
     *  The indicator section.
     */
    private final Grib1IndicatorSection is;

    /**
     * The product definition section.
     */
    private final Grib1ProductDefinitionSection pds;

    /**
     * The grid definition section.
     */
    private final Grib1GridDefinitionSection gds;

    // --Commented out by Inspection START (11/9/05 10:25 AM):
    //   /**
    //    * Array with bytes
    //    */
    //   protected byte[] buf;
    // --Commented out by Inspection STOP (11/9/05 10:25 AM)

    /** _more_          */
    private long dataOffset = -1;

    /** _more_          */
    private long endRecordOffset = -1;

    /**
     * Constructor.
     * @param hdr record header
     * @param aIs IS section
     * @param aPds PDS section
     * @param aGds GDS section
     * @param offset to the BMS/BDS section of file
     * @param recOffset to the EndOfRecord
     */
    public Grib1Record(String hdr, Grib1IndicatorSection aIs,
                       Grib1ProductDefinitionSection aPds,
                       Grib1GridDefinitionSection aGds, long offset,
                       long recOffset) {
        header          = hdr;
        is              = aIs;
        pds             = aPds;
        gds             = aGds;
        dataOffset      = offset;
        endRecordOffset = recOffset;
    }

    /**
     *  Get header.
     *  @return header
     */
    public final String getHeader() {
        return header;
    }

    /**
     *  Get Information record.
     * @return an IS record
     */
    public final Grib1IndicatorSection getIs() {
        return is;
    }

    /**
     * Get Product Definition record.
     * @return a PDS record
     */
    public final Grib1ProductDefinitionSection getPDS() {
        return pds;
    }

    /**
     * Get Grid Definition record.
     * @return a
     */
    public final Grib1GridDefinitionSection getGDS() {
        return gds;
    }

    // --Commented out by Inspection START (11/9/05 10:25 AM):
    //   /**
    //    * Get buffer with bds and bms
    //    * @return buf
    //    */
    //   public byte[] getBuf()
    //   {
    //      return buf;
    //   }
    // --Commented out by Inspection STOP (11/9/05 10:25 AM)

    /**
     * Get offset to bms.
     * @return long
     */
    public final long getDataOffset() {
        return dataOffset;
    }

    // --Commented out by Inspection START (11/9/05 10:25 AM):
    //   /**
    //    * Size to the end of the file
    //    * @return long
    //    */
    //   public long getEndRecordOffset()
    //   {
    //      return endRecordOffset;
    //   }
    // --Commented out by Inspection STOP (11/9/05 10:25 AM)
}  // end Grib1Record


