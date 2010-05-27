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

// $Id: Grib2Record.java,v 1.14 2005/12/12 18:22:40 rkambic Exp $

/**
 * Grib2Record.java.
 * @author Robb Kambic
 */
package ucar.grib.grib2;


/**
 * Class which represents a record in a Grib2File.
 * This is a heavy weight representation of the Grib record.
 */

public final class Grib2Record {

    /**
     * Grib record header.
     */
    private final String header;

    /**
     * Grib2IndicatorSection object.
     */
    private Grib2IndicatorSection is = null;

    /**
     * Grib2IdentificationSection object.
     */
    private Grib2IdentificationSection id = null;

    /**
     * Grib2GridDefinitionSection object.
     */
    private Grib2GridDefinitionSection gds = null;

    /**
     * Grib2ProductDefinitionSection object.
     */
    private Grib2ProductDefinitionSection pds = null;

    /**
     * Grib2DataRepresentationSection object.
     */
    private Grib2DataRepresentationSection drs = null;

    /**
     * GdsOffset in file.
     */
    private long GdsOffset = -1;

    /**
     * PdsOffset in file.
     */
    private long PdsOffset = -1;

    /**
     * Construction for Grib2Record.
     * @param header Grib header
     * @param is Grib2IndicatorSection
     * @param id Grib2IdentificationSection
     * @param pds Grib2ProductDefinitionSection
     * @param drs Grib2DataRepresentationSection
     * @param GdsOffset GDS offset in Grib file
     * @param PdsOffset PDS offset in Grib file

     */
    public Grib2Record(String header, Grib2IndicatorSection is,
                       Grib2IdentificationSection id,
                       Grib2GridDefinitionSection gds,
                       Grib2ProductDefinitionSection pds,
                       Grib2DataRepresentationSection drs,
                       long GdsOffset,
                       long PdsOffset) {

        this.header = header;
        this.is     = is;
        this.id     = id;
        this.gds    = gds;
        this.pds    = pds;
        this.drs    = drs;
        this.GdsOffset = GdsOffset;
        this.PdsOffset = PdsOffset;
    }

    /**
     * returns Header of Grib record.
     * @return header
     */
    public final String getHeader() {
        return header;
    }

    /**
     * returns GDS offset in file.
     * @return GdsOffset
     */
    public final long getGdsOffset() {
        return GdsOffset;
    }

    /**
     * returns Pds Offset.
     * @return PdsOffset
     */
    public final long getPdsOffset() {
        return PdsOffset;
    }

    /**
     * returns Inofrmation Section of record.
     * @return is
     */
    public final Grib2IndicatorSection getIs() {
        return is;
    }

    /**
     * returns IdentificationSection.
     * @return IdentificationSection
     */
    public final Grib2IdentificationSection getId() {
        return id;
    }

    /**
     * returns GDS of record.
     * @return gds
     */
    public final Grib2GridDefinitionSection getGDS() {
        return gds;
    }

    /**
     * returns PDS.
     * @return pds
     */
    public final Grib2ProductDefinitionSection getPDS() {
        return pds;
    }

    /**
     * returns Data Representation Section.
     * @return DataRepresentationSection
     */
    public final Grib2DataRepresentationSection getDRS() {
        return drs;
    }
}

