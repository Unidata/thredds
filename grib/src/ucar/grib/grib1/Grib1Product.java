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

// $Id: Grib1Product.java,v 1.10 2005/12/08 21:00:06 rkambic Exp $


package ucar.grib.grib1;


/**
 * Title:        Grib1
 * Description:  Class which has the necessary information about
 * a product in a Grib1 File to extract the data for the product.
 * @author Robb Kambic
 * @version 1.0
 */

public final class Grib1Product {

    /** _more_          */
    private final String header;

    /** _more_          */
    private final int discipline = 0;

    /** _more_          */
    private final int category = -1;

    /** _more_          */
    private final Grib1ProductDefinitionSection pds;

    /** _more_          */
    private String gdsKey;
    private final int gdskey;

    /** _more_          */
    private final long offset1;

    /** _more_          */
    private final long offset2;

    /**
     * Constructor.
     * @param header
     * @param pds
     * @param gdsKey
     * @param offset
     * @param size offset2 in file
     */
    public Grib1Product(String header, Grib1ProductDefinitionSection pds,
                        String gdsKey, int gdskey, long offset, long size) {
        this.header          = header;
        this.gdsKey          = gdsKey;
        this.gdskey          = gdskey;
        this.pds             = pds;
        this.offset1      = offset;
        this.offset2 = size;
    }

    // --Commented out by Inspection START (11/17/05 2:15 PM):
    //   public final String getHeader(){
    //      return header;
    //   }
    // --Commented out by Inspection STOP (11/17/05 2:15 PM)

    /**
     * get the discipline of product as int.
     * @return discipline
     */
    public final int getDiscipline() {
        return discipline;
    }

    /**
     * get category of this product as int.
     * @return category as a int
     */
    public final int getCategory() {
        return category;
    }

    /**
     * gets GDS key for this product.
     * @return gdsKey
     */
    public final String getGDSkey() {
        return gdsKey;
    }
    public final int getGDSkeyInt() {
        return gdskey;
    }
    /**
     * sets the GDS key for this product.
     * @param aGDSkey  MD5 checksum as text
     */
    public final void setGDSkey(String aGDSkey) {
        gdsKey = aGDSkey;
    }

    /**
     * get the PDS for this product.
     * @return pds
     */
    public final Grib1ProductDefinitionSection getPDS() {
        return pds;
    }

    /**
     * offset1 usually is the Gds offset for this record.
     * @return offset1
     */
    public final long getOffset1() {
        return offset1;
    }
    /**
     * offset2 usually is the BMS or Data section offset.
     * @return offset2
     */
    public final long getOffset2() {
        return offset2;
    }
}  // end Grib1Product


