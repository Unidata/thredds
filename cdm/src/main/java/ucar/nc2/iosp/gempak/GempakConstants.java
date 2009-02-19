/*
 * $Id: IDV-Style.xjs,v 1.3 2007/02/16 19:18:30 dmurray Exp $
 *
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


/**
 * Class to support the GEMPRM.PRM constants
 */
public interface GempakConstants {

    // Machine types

    /** VAX */
    public static final int MTVAX = 2;

    /** Sun (SPARC) */
    public static final int MTSUN = 3;

    /** Irix */
    public static final int MTIRIS = 4;

    /** APOL */
    public static final int MTAPOL = 5;

    /** IBM */
    public static final int MTIBM = 6;

    /** Intergraph */
    public static final int MTIGPH = 7;

    /** Ultrix */
    public static final int MTULTX = 8;

    /** HP */
    public static final int MTHP = 9;

    /** Alpha */
    public static final int MTALPH = 10;

    /** Linux */
    public static final int MTLNUX = 11;

    /** Integer missing value */
    public static final int IMISSD = -9999;

    /** float missing value */
    public static final float RMISSD = -9999.f;

    /** missing value fuzziness */
    public static final float RDIFFD = 0.1f;

    // Data file types

    /** Surface file type */
    public static final int MFSF = 1;

    /** Sounding File Type */
    public static final int MFSN = 2;

    /** Grid file type */
    public static final int MFGD = 3;

    // Data types

    /** No packing */
    public static final int MDREAL = 1;

    /** Integer packing */
    public static final int MDINTG = 2;

    /** Character packing */
    public static final int MDCHAR = 3;

    /** real packing */
    public static final int MDRPCK = 4;

    /** Grid packing */
    public static final int MDGRID = 5;

    // Grid params

    /** Grid nav block length */
    public static final int LLNNAV = 256;

    /** Grid anl block length */
    public static final int LLNANL = 128;

    /** Max header size */
    public static final int LLSTHL = 20;

    /** Max grid hdr length */
    public static final int LLGDHD = 128;

    // Grid packing types

    /** no packing */
    public static final int MDGNON = 0;

    /** GRIB1 packing */
    public static final int MDGGRB = 1;

    /** NMC packing */
    public static final int MDGNMC = 2;

    /** DIF packing */
    public static final int MDGDIF = 3;

    /** decimal packing? */
    public static final int MDGDEC = 4;

    /** GRIB2 packing */
    public static final int MDGRB2 = 5;

    // DM stuff

    /** row identifier */
    public static final String ROW = "ROW";

    /** column identifier */
    public static final String COL = "COL";

    /** Block size */
    public static final int MBLKSZ = 128;
}

