/*
 * $Id: DateUtil.java,v 1.3 2007/05/10 11:57:21 jeffmc Exp $
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

package ucar.unidata.util;


import java.text.ParseException;
import java.text.SimpleDateFormat;

import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;


/**
 * _more_
 *
 * @author edavis
 * @since May 4, 2007 1:01:53 PM
 */
public class DateUtil {

    /** _more_          */
    private static org.slf4j.Logger log =
        org.slf4j.LoggerFactory.getLogger(DateUtil.class);

    /**
     * _more_
     *
     * @return _more_
     */
    public static String getCurrentSystemTimeAsISO8601() {
        return getTimeAsISO8601(System.currentTimeMillis());
    }


    /**
     * _more_
     *
     * @param time _more_
     *
     * @return _more_
     */
    public static String getTimeAsISO8601(long time) {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
        cal.setTimeInMillis(time);
        Date curSysDate = cal.getTime();
        return DateFormatHandler.ISO_DATE_TIME.getDateTimeStringFromDate(
            curSysDate);
    }

    /**
     * Class for dealing with date/time formats.
     */
    static class DateFormatHandler {
        // Available date format handlers.

        /** _more_          */
        public final static DateFormatHandler ISO_DATE =
            new DateFormatHandler("yyyy-MM-dd");

        /** _more_          */
        public final static DateFormatHandler ISO_TIME =
            new DateFormatHandler("HH:mm:ss.SSSz");

        /** _more_          */
        public final static DateFormatHandler ISO_DATE_TIME =
            new DateFormatHandler("yyyy-MM-dd\'T\'HH:mm:ssz");

        /** _more_          */
        public final static DateFormatHandler ISO_DATE_TIME_MILLIS =
            new DateFormatHandler("yyyy-MM-dd\'T\'HH:mm:ss.SSSz");

        /** _more_          */
        private String dateTimeFormatString = null;

        /**
         * _more_
         *
         * @param dateTimeFormatString _more_
         */
        private DateFormatHandler(String dateTimeFormatString) {
            this.dateTimeFormatString = dateTimeFormatString;
        }

        /**
         * _more_
         *
         * @return _more_
         */
        public String getDateTimeFormatString() {
            return this.dateTimeFormatString;
        }

        /**
         * Return a java.util.Date given a date string using the date/time format string
         * or null if can't parse the given date string.
         *
         * @param dateTimeString - date/time string to be used to set java.util.Date.
         * @return The java.util.Date set by the given date/time string or null.
         */
        public Date getDateFromDateTimeString(String dateTimeString) {
            Date theDate = null;

            SimpleDateFormat dateFormat =
                new SimpleDateFormat(this.dateTimeFormatString, Locale.US);
            dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));

            try {
                theDate = dateFormat.parse(dateTimeString);
            } catch (ParseException e) {
                log.warn(e.getMessage());
                return null;
            }

            return theDate;
        }

        /**
         * Return the date/time string that represents the given a java.util.Date
         * in the format of this DataFormatHandler.
         *
         * @param date - the Date to be formatted into a date/time string.
         * @return The date/time string formatted from the given Date.
         */
        public String getDateTimeStringFromDate(Date date) {
            SimpleDateFormat dateFormat =
                new SimpleDateFormat(this.dateTimeFormatString, Locale.US);
            dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));

            String dateString = dateFormat.format(date);

            return (dateString);
        }
    }



    /**
     * utility to convert a given number of hours to milliseconds
     *
     * @param hour hours
     *
     * @return milliseconds
     */
    public static long hoursToMillis(double hour) {
        return minutesToMillis(hour * 60);
    }

    /**
     * utility to convert a given number of milliseconds to minutes
     *
     * @param millis milliseconds
     *
     * @return minutes
     */

    public static double millisToMinutes(double millis) {
        return millis / 1000 / 60;
    }

    /**
     * utility to convert a given number of minutes to milliseconds
     *
     * @param minutes minutes
     *
     * @return milliseconds
     */
    public static long minutesToMillis(double minutes) {
        return (long)(minutes * 60 * 1000);
    }




}

