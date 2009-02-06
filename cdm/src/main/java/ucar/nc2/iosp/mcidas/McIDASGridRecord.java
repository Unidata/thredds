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


package ucar.nc2.iosp.mcidas;


import edu.wisc.ssec.mcidas.GridDirectory;
import edu.wisc.ssec.mcidas.McIDASException;

import edu.wisc.ssec.mcidas.McIDASUtil;

//import ucar.nc2.iosp.grid.*;

import java.util.Date;

import ucar.grid.GridRecord;


/**
 * A class to hold McIDAS grid record information
 *
 * @author Unidata Development Team
 * @version $Revision: 1.3 $
 */
public class McIDASGridRecord extends GridDirectory implements GridRecord {

    /** offset to header */
    private int offsetToHeader;  // offset to header

    /** grid definition */
    private McGridDefRecord gridDefRecord;

    /** decimal scale */
    private int decimalScale = 0;

    /**
     * Create a grid header from the integer bits
     * @param offset  offset to grid header in file
     * @param header  header words
     *
     * @throws McIDASException Problem creating the record
     */
    public McIDASGridRecord(int offset, int[] header) throws McIDASException {
        super(header);
        gridDefRecord  = new McGridDefRecord(header);
        offsetToHeader = offset;
    }

    /**
     * Get the first level of this GridRecord
     *
     * @return the first level value
     */
    public double getLevel1() {
        return getLevelValue();
    }

    /**
     * Get the second level of this GridRecord
     *
     * @return the second level value
     */
    public double getLevel2() {
        return getSecondLevelValue();
    }

    /**
     * Get the type for the first level of this GridRecord
     *
     * @return level type
     */
    public int getLevelType1() {
        // TODO:  flush this out
        int gribLevel = getDirBlock()[51];
        int levelType = 0;
        if ( !((gribLevel == McIDASUtil.MCMISSING) || (gribLevel == 0))) {
            levelType = gribLevel;
        } else {
            levelType = 1;
        }
        return levelType;
    }

    /**
     * Get the type for the second level of this GridRecord
     *
     * @return level type
     */
    public int getLevelType2() {
        return getLevelType1();
    }

    /**
     * Get valid time offset (minutes) of this GridRecord
     *
     * @return time offset
     */
    public int getValidTimeOffset() {
        return getForecastHour();
    }

    /**
     * Get the parameter name
     *
     * @return parameter name
     */
    public String getParameterName() {
        return getParamName();
    }

    /**
     * Get the decimal scale
     *
     * @return decimal scale
     */
    public int getDecimalScale() {
        return decimalScale;
    }

    /**
     * Get the grid def record id
     *
     * @return parameter name
     */
    public String getGridDefRecordId() {
        return gridDefRecord.toString();
    }

    /**
     * Get the grid def record id
     *
     * @return parameter name
     */
    public McGridDefRecord getGridDefRecord() {
        return gridDefRecord;
    }

    /**
     * Get the offset to the grid header (4 byte words)
     *
     * @return word offset
     */
    public int getOffsetToHeader() {
        return offsetToHeader;
    }

    /**
     * Had GRIB info?
     *
     * @return  true if has grib info
     */
    public boolean hasGribInfo() {
        int gribSection = getDirBlock()[48];
        return ( !((gribSection == McIDASUtil.MCMISSING)
                   || (gribSection == 0)));
    }
}

