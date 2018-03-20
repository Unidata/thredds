/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.iosp.grads;


import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;


/**
 * A class to hold a GrADS time structure.  The full time spec is:
 *
 *        HH:mm'Z'ddMMMyyyy (e.g. 12:04Z05Mar2011)
 *
 * @author   Don Murray CU-CIRES
 */
public class GradsTimeStruct {

    /** months */
    public static final String[] months = {
        "jan", "feb", "mar", "apr", "may", "jun", "jul", "aug", "sep", "oct",
        "nov", "dec"
    };

    /** year field */
    int year = 0;

    /** month field (1 based) */
    int month = 0;

    /** day field */
    int day = 0;

    /** hour field */
    int hour = 0;

    /** minute field */
    int minute = 0;

    /** julian day field */
    int jday = 0;

    /**
     * Create a new time structure
     */
    public GradsTimeStruct() {}

    /**
     * Get a String representation of this object
     *
     * @return the GrADS time specification
     */
    public String toString() {
        return String.format("%02d:%02dZ%02d%s%d", hour, minute, day,
                             months[month - 1], year);
    }

    /**
     * Return this as a java Date object
     *
     * @return the corresponding Date
     */
    public Date getDate() {
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
        calendar.set(Calendar.YEAR, year);
        calendar.set(Calendar.MONTH, month - 1);  // MONTH is zero based
        calendar.set(Calendar.DAY_OF_MONTH, day);
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTime();
    }
}

