/*
 * $Id: IDV-Style.xjs,v 1.3 2007/02/16 19:18:30 dmurray Exp $
 *
 * Copyright 1997-2007 Unidata Program Center/University Corporation for
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

