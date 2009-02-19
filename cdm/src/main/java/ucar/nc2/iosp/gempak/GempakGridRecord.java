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


import edu.wisc.ssec.mcidas.McIDASUtil;


//import ucar.nc2.iosp.grid.GridRecord;
import ucar.grid.GridRecord;


import ucar.unidata.util.StringUtil;

import java.util.Calendar;
import java.util.Date;


/**
 * A class to hold grid record information
 *
 * @author IDV Development Team
 * @version $Revision: 1.3 $
 */
public class GempakGridRecord implements GridRecord {

    /** Time 1 */
    public String time1;

    /** Time 2 */
    public String time2;

    /** Level 1 */
    public int level1 = GempakConstants.IMISSD;

    /** Level 2 */
    public int level2 = GempakConstants.IMISSD;

    /** coordinate type */
    public int ivcord;

    /** parameter */
    public String param;

    /** grid number */
    public int gridNumber;  // column

    /** packing type */
    public int packingType;

    /** packing type */
    public NavigationBlock navBlock;

    /** reference time as a Date */
    private Date refTime;

    /** valid time offset in ninutes */
    private int validOffset;

    /** decimal scale */
    private int decimalScale = 0;

    /** actual valid time */
    private Date validTime;


    /**
     * Create a grid header from the integer bits
     * @param number  grid number
     * @param header integer bits
     */
    public GempakGridRecord(int number, int[] header) {
        gridNumber = number;
        int[] times1 = GempakUtil.TG_FTOI(header, 0);
        time1 = GempakUtil.TG_ITOC(times1);
        int[] times2 = GempakUtil.TG_FTOI(header, 2);
        time2  = GempakUtil.TG_ITOC(times2);
        level1 = header[4];
        level2 = header[5];
        ivcord = header[6];
        param = GempakUtil.ST_ITOC(new int[] { header[7], header[8],
                header[9] });
        param = param.trim();
        int ymd = times1[0];
        if (ymd / 10000 < 50) {
            ymd += 20000000;
        } else if (ymd / 10000 < 100) {
            ymd += 19000000;
        }
        int hms = times1[1] * 100;  // need to add seconds
        refTime = new Date(McIDASUtil.mcDateHmsToSecs(ymd, hms) * 1000l);
        int offset = times1[2] % 100000;
        if ((offset == 0) || (offset % 100 == 0)) {  // 0 or no minutes
            validOffset = (offset / 100) * 60;
        } else {                                     // have minutes
            validOffset = (offset / 100) * 60 + offset % 100;
        }
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeZone(java.util.TimeZone.getTimeZone("GMT"));
        calendar.setTime(refTime);
        calendar.add(Calendar.MINUTE, validOffset);
        validTime = calendar.getTime();
        GempakParameter ggp = GempakGridParameterTable.getParameter(param);
        if (ggp != null) {
            decimalScale = ggp.getDecimalScale();
        }
    }

    /**
     * Get the first level of this GridRecord
     *
     * @return the first level value
     */
    public double getLevel1() {
        return level1;
    }

    /**
     * Get the second level of this GridRecord
     *
     * @return the second level value
     */
    public double getLevel2() {
        return level2;
    }

    /**
     * Get the type for the first level of this GridRecord
     *
     * @return level type
     */
    public int getLevelType1() {
        return ivcord;
    }

    /**
     * Get the type for the second level of this GridRecord
     *
     * @return level type
     */
    public int getLevelType2() {
        return ivcord;
    }

    /**
     * Get the first reference time of this GridRecord
     *
     * @return reference time
     */
    public Date getReferenceTime() {
        return refTime;
    }

    /**
     * Get the valid time for this grid.
     *
     * @return valid time
     */
    public Date getValidTime() {
        return validTime;
    }

    /**
     * Get valid time offset (minutes) of this GridRecord
     *
     * @return time offset
     */
    public int getValidTimeOffset() {
        return validOffset;
    }

    /**
     * Get the parameter name
     *
     * @return parameter name
     */
    public String getParameterName() {
        return param;
    }

    /**
     * Get the grid def record id
     *
     * @return parameter name
     */
    public String getGridDefRecordId() {
        return navBlock.toString();
    }

    /**
     * Get the grid number
     *
     * @return grid number
     */
    public int getGridNumber() {
        return gridNumber;
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
     * Get a String representation of this object
     * @return a String representation of this object
     */
    public String toString() {
        StringBuffer buf = new StringBuffer();
        buf.append(StringUtil.padLeft(String.valueOf(gridNumber), 5));
        buf.append(StringUtil.padLeft(time1, 20));
        buf.append(" ");
        buf.append(StringUtil.padLeft(time2, 20));
        buf.append(" ");
        buf.append(StringUtil.padLeft(String.valueOf(level1), 5));
        if (level2 != -1) {
            buf.append(StringUtil.padLeft(String.valueOf(level2), 5));
        } else {
            buf.append("     ");
        }
        buf.append("  ");
        buf.append(StringUtil.padLeft(GempakUtil.LV_CCRD(ivcord), 6));
        buf.append(" ");
        buf.append(param);
        buf.append(" ");
        buf.append(GempakUtil.getGridPackingName(packingType));
        return buf.toString();
    }

}

