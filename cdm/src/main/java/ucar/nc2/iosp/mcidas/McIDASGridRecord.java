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


package ucar.nc2.iosp.mcidas;


import edu.wisc.ssec.mcidas.GridDirectory;
import edu.wisc.ssec.mcidas.McIDASException;

import edu.wisc.ssec.mcidas.McIDASUtil;

import ucar.nc2.iosp.grid.*;

import java.util.Date;


/**
 * A class to hold McIDAS grid record information
 *
 * @author IDV Development Team
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

