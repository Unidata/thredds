/*
 * $Id: DateUtil.java,v 1.10 2007/05/22 23:33:26 jeffmc Exp $
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
 * A set of date oriented utilities
 *
 * @author edavis and the IDV development team
 * @since May 4, 2007 1:01:53 PM
 */
public class DateUtil {

    /** milliseconds in  a millisecond */
    public static final long MILLIS = 1;

    /** milliseconds in  a second */
    public static final long MILLIS_SECOND = 1000;

    /** milliseconds in  a minute */
    public static final long MILLIS_MINUTE = 1000 * 60;

    /** milliseconds in an hour */
    public static final long MILLIS_HOUR = 1000 * 60 * 60;

    /** milliseconds in  a day */
    public static final long MILLIS_DAY = MILLIS_HOUR * 24;

    /** milliseconds in  a week */
    public static final long MILLIS_WEEK = MILLIS_DAY * 7;

    /** milliseconds in  a month   (approximately) */
    public static final long MILLIS_MONTH = MILLIS_DAY * 30;

    /** milliseconds in  a year (approximately) */
    public static final long MILLIS_YEAR = MILLIS_DAY * 365;

    /** milliseconds in  a decade (approximately) */
    public static final long MILLIS_DECADE = MILLIS_YEAR * 10;

    /** milliseconds in  a century (approximately) */
    public static final long MILLIS_CENTURY = MILLIS_DECADE * 10;

    /** milliseconds in  a century (approximately) */
    public static final long MILLIS_MILLENIUM = MILLIS_CENTURY * 10;



    /** logger */
    private static org.slf4j.Logger log =
        org.slf4j.LoggerFactory.getLogger(DateUtil.class);

    /**
     * format current time
     *
     * @return current time formatted
     */
    public static String getCurrentSystemTimeAsISO8601() {
        return getTimeAsISO8601(System.currentTimeMillis());
    }


    /**
     * format time
     *
     * @param time time
     *
     * @return formatted time
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

        /** _more_ */
        public final static DateFormatHandler ISO_DATE =
            new DateFormatHandler("yyyy-MM-dd");

        /** _more_ */
        public final static DateFormatHandler ISO_TIME =
            new DateFormatHandler("HH:mm:ss.SSSz");

        /** _more_ */
        public final static DateFormatHandler ISO_DATE_TIME =
            new DateFormatHandler("yyyy-MM-dd\'T\'HH:mm:ssz");

        /** _more_ */
        public final static DateFormatHandler ISO_DATE_TIME_MILLIS =
            new DateFormatHandler("yyyy-MM-dd\'T\'HH:mm:ss.SSSz");

        /** _more_ */
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
     * utility to convert a given number of days to milliseconds
     *
     * @param days days
     *
     * @return milliseconds
     */
    public static long daysToMillis(double days) {
        return hoursToMillis(days * 24);
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
        return (long) (minutes * 60 * 1000);
    }




}

