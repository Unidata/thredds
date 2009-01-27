/*
 * $Id: DateUtil.java,v 1.11 2007/07/02 21:14:00 jeffmc Exp $
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



package ucar.unidata.util;


import java.text.ParseException;
import java.text.SimpleDateFormat;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;


/**
 * A set of date oriented utilities
 *
 * @author edavis and the IDV development team
 * @since May 4, 2007 1:01:53 PM
 */
public class DateUtil {

    /** _more_          */
    public static final String[] MONTH_NAMES = {
        "January", "February", "March", "April", "May", "June", "July",
        "August", "September", "October", "November", "December"
    };

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


    /** timezone */
    public static final TimeZone TIMEZONE_GMT = TimeZone.getTimeZone("GMT");


    /** a set of regular expressions that go along with the below DATE_FORMATS */
    public static final String[] DATE_PATTERNS = { "(\\d\\d\\d\\d\\d\\d\\d\\d_\\d\\d\\d\\d)",
            "(\\d\\d\\d\\d\\d\\d\\d\\d_\\d\\d)",
            "(\\d\\d\\d\\d\\d\\d\\d\\d)" };

    /** A set of date formats */
    public static final String[] DATE_FORMATS = { "yyyyMMdd_HHmm",
            "yyyyMMdd_HH", "yyyyMMdd" };



    //Comment this out to remove dependencies to external classes
    //    private static org.slf4j.Logger log =
    //        org.slf4j.LoggerFactory.getLogger(DateUtil.class);

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
     * @param date date
     *
     * @return formatted time
     */
    public static String getTimeAsISO8601(Date date) {
        return getTimeAsISO8601(date.getTime());
    }



    /**
     * format time
     *
     * @param time time
     *
     * @return formatted time
     */
    public static String getTimeAsISO8601(long time) {
        Calendar cal = Calendar.getInstance(TIMEZONE_GMT);
        cal.setTimeInMillis(time);
        Date curSysDate = cal.getTime();
        return DateFormatHandler.ISO_DATE_TIME.getDateTimeStringFromDate(
            curSysDate);
    }


    /** A set of common date formats */
    private static final String[] formats = {
        "yyyy-MM-dd'T'HH:mm:ss", "yyyy-MM-dd HH:mm:ss", "yyyy/MM/dd HH:mm:ss",
        "yyyyMMdd'T'HHmmss", "yyyy-MM-dd", "yyyy/MM/dd",
        "EEE MMM dd HH:mm:ss Z yyyy", "yyyy-MM", "yyyy/MM", "yyyy"
    };

    /** The SimpleDateFormat objects we make from the above formats */
    private static SimpleDateFormat[] sdfs;

    /** The lasts SDF that was successfully used. We keep this around for efficiency */
    private static SimpleDateFormat lastSdf;


    /**
     * Rounds up or down (if negative) the number of days.
     *
     * @param dttm date to round
     * @param day number of days
     *
     * @return rounded date.
     */
    public static Date roundByDay(Date dttm, int day) {
        if (day == 0) {
            return dttm;
        }
        if (day < 0) {
            day++;
        }
        Calendar cal = Calendar.getInstance(TIMEZONE_GMT);
        cal.setTimeInMillis(dttm.getTime());
        cal.clear(Calendar.MILLISECOND);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.add(Calendar.DAY_OF_YEAR, day);
        return new Date(cal.getTimeInMillis());
    }

    /**
     * This finds the SDF to use for the given date string
     *
     * @param dateString example date
     *
     * @return formatter to use
     */
    public static SimpleDateFormat findFormatter(String dateString) {
        if (sdfs == null) {
            sdfs = new SimpleDateFormat[formats.length];
            for (int i = 0; i < formats.length; i++) {
                sdfs[i] = new SimpleDateFormat(formats[i]);
            }
        }
        if (lastSdf != null) {
            try {
                lastSdf.parse(dateString);
                return lastSdf;
            } catch (ParseException pe) {}
        }

        for (int i = 0; i < formats.length; i++) {
            try {
                sdfs[i].parse(dateString);
                lastSdf = sdfs[i];
                return sdfs[i];
            } catch (ParseException pe) {}
        }
        throw new IllegalArgumentException("Could not find date format for:"
                                           + dateString);
    }


    /**
     * This gets a date range based on the text dates. The incoming dates can be of the form:<pre>
     * absolute date
     * now (for current time)
     * relative date (e.g., (offset unit), -5 seconds, +2 hours, +5 days, -3 weeks, -1 month
     * This is calculated relative to the other date, e.g:
     * -1 hour, now
     *
     * </pre>
     *
     * @param fromDate from date
     * @param toDate to date
     * @param dflt base default date
     *
     * @return date range
     *
     * @throws java.text.ParseException On badness
     */
    public static Date[] getDateRange(String fromDate, String toDate,
                                      Date dflt)
            throws java.text.ParseException {

        Date fromDttm = DateUtil.parseRelative(dflt, fromDate, -1);
        Date toDttm   = DateUtil.parseRelative(dflt, toDate, +1);
        //        System.err.println ("dflt: " + dflt);
        //        System.err.println ("toDttm:" + toDate + " " + toDttm);

        if ((fromDate.length() > 0) && (fromDttm == null)) {
            if ( !fromDate.startsWith("-")) {
                fromDttm = DateUtil.parse(fromDate);
            }
        }
        if ((toDate.length() > 0) && (toDttm == null)) {
            if ( !toDate.startsWith("+")) {
                toDttm = DateUtil.parse(toDate);
            }
        }

        if ((fromDttm == null) && fromDate.startsWith("-")) {
            if (toDttm == null) {
                throw new IllegalArgumentException(
                    "Cannot do relative From Date when To Date is not set");
            }
            fromDttm = DateUtil.getRelativeDate(toDttm, fromDate);
        }

        if ((toDttm == null) && toDate.startsWith("+")) {
            if (fromDttm == null) {
                throw new IllegalArgumentException(
                    "Cannot do relative From Date when To Date is not set");
            }
            toDttm = DateUtil.getRelativeDate(fromDttm, toDate);
        }

        //        System.err.println("from:" + Repository.fmt(fromDttm) + " -- " + Repository.fmt(toDttm));
        return new Date[] { fromDttm, toDttm };
    }




    /**
     * parse the date string (s) (e.g., -1 hour) that is relative to the given baseDate
     *
     * @param baseDate base date
     * @param s date string
     * @param roundDays  round down or up the given number of days
     *
     * @return date
     *
     * @throws java.text.ParseException on badness
     */
    public static Date parseRelative(Date baseDate, String s, int roundDays)
            throws java.text.ParseException {
        s = s.trim();
        Calendar cal = Calendar.getInstance(TIMEZONE_GMT);
        cal.setTimeInMillis(baseDate.getTime());
        Date dttm = null;
        if (s.equals("now")) {
            dttm = cal.getTime();
            return dttm;
        } else if (s.equals("today")) {
            dttm = cal.getTime();
        } else if (s.equals("yesterday")) {
            dttm = new Date(cal.getTime().getTime() - daysToMillis(1));
        } else if (s.equals("tomorrow")) {
            dttm = new Date(cal.getTime().getTime() + daysToMillis(1));
        } else if (s.startsWith("last") || s.startsWith("next")) {
            List toks = StringUtil.split(s, " ", true, true);
            if (toks.size() != 2) {
                throw new IllegalArgumentException("Bad time format:" + s);
            }
            int    factor = (toks.get(0).equals("last")
                             ? -1
                             : +1);
            String unit   = (String) toks.get(1);
            if (unit.equals("week")) {
                cal.add(Calendar.WEEK_OF_MONTH, factor);
            } else if (unit.equals("month")) {
                cal.add(Calendar.MONTH, factor);
            } else if (unit.equals("year")) {
                cal.add(Calendar.YEAR, factor);
            } else if (unit.equals("century")) {
                cal.add(Calendar.YEAR, factor * 100);
            } else if (unit.equals("millenium")) {
                cal.add(Calendar.YEAR, factor * 1000);
            } else {
                throw new IllegalArgumentException("Bad time format:" + s
                        + " unknown time field:" + unit);
            }
            dttm = cal.getTime();
        }
        if (dttm != null) {
            return roundByDay(dttm, roundDays);
        }
        return null;
    }


    /**
     * Parse the date string
     *
     * @param s date string
     *
     * @return date
     *
     * @throws java.text.ParseException on badness
     */
    public static Date parse(String s) throws java.text.ParseException {
        SimpleDateFormat sdf = findFormatter(s);
        return sdf.parse(s);
    }

    /**
     * parse the array of date strings and returns the date as seconds
     *
     * @param s array of date strings
     *
     * @return array of seconds
     *
     * @throws java.text.ParseException On badness
     */
    public static double[] toSeconds(String[] s)
            throws java.text.ParseException {
        double[] d = new double[s.length];
        if (s.length == 0) {
            return d;
        }
        SimpleDateFormat sdf      = findFormatter(s[0]);
        double           lastTime = 0;
        for (int i = 0; i < s.length; i++) {
            d[i] = sdf.parse(s[i]).getTime() / 1000.0;
            if (d[i] < lastTime) {
                System.out.println("****" + s[i]);
            }
            //            else  System.out.println (s[i]);
            lastTime = d[i];
            //            System.out.println(d[i]);
        }
        return d;
    }

    /**
     * Class for dealing with date/time formats.
     */
    static class DateFormatHandler {
        // Available date format handlers.

        /** date formatter */
        public final static DateFormatHandler ISO_DATE =
            new DateFormatHandler("yyyy-MM-dd");

        /** date formatter */
        public final static DateFormatHandler ISO_TIME =
            new DateFormatHandler("HH:mm:ss.SSSz");

        /** date formatter */
        public final static DateFormatHandler ISO_DATE_TIME =
            new DateFormatHandler("yyyy-MM-dd\'T\'HH:mm:ssz");

        /** date formatter */
        public final static DateFormatHandler ISO_DATE_TIME_MILLIS =
            new DateFormatHandler("yyyy-MM-dd\'T\'HH:mm:ss.SSSz");

        /** format */
        private String dateTimeFormatString = null;

        /**
         * ctor
         *
         * @param dateTimeFormatString format string
         */
        private DateFormatHandler(String dateTimeFormatString) {
            this.dateTimeFormatString = dateTimeFormatString;
        }

        /**
         * get format string
         *
         * @return format string
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
            dateFormat.setTimeZone(TIMEZONE_GMT);

            try {
                theDate = dateFormat.parse(dateTimeString);
            } catch (ParseException e) {
                throw new IllegalArgumentException(e.getMessage());
                //                log.warn(e.getMessage());
                //                return null;
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
            dateFormat.setTimeZone(TIMEZONE_GMT);

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

    /**
     * Get a new date relative to the given date.
     *
     * @param from base date
     * @param relativeTimeString Relative time string, e.g., -1 hour
     *
     * @return new date
     */
    public static Date getRelativeDate(Date from, String relativeTimeString) {
        Date result = new Date(from.getTime()
                               + parseRelativeTimeString(relativeTimeString));
        return result;
    }

    /**
     * Return the delta number of milliseconds specified in the relative time string
     *
     * @param relativeTimeString This is of the form "offset unit", e.g.:<pre>
     * -1 hour
     * +2 weeks
     * etc.
     * </pre>
     *
     * @return milliseconds
     */
    public static long parseRelativeTimeString(String relativeTimeString) {
        List toks = StringUtil.split(relativeTimeString, " ", true, true);
        if (toks.size() != 2) {
            throw new IllegalArgumentException(
                "Bad format for relative time string:" + relativeTimeString
                + " Needs to be of the form: +/-<number> timeunit");
        }
        long delta = 0;
        try {
            String s      = toks.get(0).toString();
            long   factor = 1;
            if (s.startsWith("+")) {
                s = s.substring(1);
            } else if (s.startsWith("-")) {
                s      = s.substring(1);
                factor = -1;
            }

            delta = factor * new Integer(s).intValue();
            //            System.err.println ("factor:" + factor + " delta:" + delta);
        } catch (Exception exc) {
            throw new IllegalArgumentException(
                "Bad format for relative time string:" + relativeTimeString
                + " Could not parse initial number:" + toks.get(0));
        }
        String what         = (String) toks.get(1);
        long   milliseconds = 0;
        if (what.startsWith("second")) {
            milliseconds = delta * 1000;
        } else if (what.startsWith("minute")) {
            milliseconds = 60 * delta * 1000;
        } else if (what.startsWith("hour")) {
            milliseconds = 60 * 60 * delta * 1000;
        } else if (what.startsWith("day")) {
            milliseconds = 24 * 60 * 60 * delta * 1000;
        } else if (what.startsWith("week")) {
            milliseconds = 7 * 24 * 60 * 60 * delta * 1000;
        } else if (what.startsWith("month")) {
            milliseconds = 30 * 24 * 60 * 60 * delta * 1000;
        } else if (what.startsWith("year")) {
            milliseconds = 365 * 24 * 60 * 60 * delta * 1000;
        } else if (what.startsWith("century")) {
            milliseconds = 100 * 365 * 24 * 60 * 60 * delta * 1000;
        } else if (what.startsWith("millenium")) {
            milliseconds = 1000 * 365 * 24 * 60 * 60 * delta * 1000;
        } else {
            throw new IllegalArgumentException(
                "Unknown unit in relative time string:" + relativeTimeString);
        }
        return milliseconds;
    }


    /**
     * Decode a date from a WMO header of the form ddHHmm.
     *
     * @param wmoDate  WMO header string
     * @param baseDate  base date to get the year and month
     *
     * @return  the date that is represented by wmoDate
     */
    public static Date decodeWMODate(String wmoDate, Date baseDate) {
        if (baseDate == null) {
            baseDate = new Date();
        }
        if (wmoDate.length() > 6) {
            return baseDate;
        } else {
            wmoDate = StringUtil.padLeft(wmoDate, 6, "0");
        }
        Calendar cal = Calendar.getInstance(TIMEZONE_GMT);
        cal.setTimeInMillis(baseDate.getTime());
        int day    = Integer.parseInt(wmoDate.substring(0, 2));
        int hour   = Integer.parseInt(wmoDate.substring(2, 4));
        int min    = Integer.parseInt(wmoDate.substring(4));
        int calDay = cal.get(cal.DAY_OF_MONTH);
        if ((calDay - day) > 26) {
            cal.add(cal.MONTH, 1);
        } else if ((day - calDay) > 20) {
            cal.add(cal.MONTH, -1);
        }
        cal.set(cal.DAY_OF_MONTH, day);
        cal.set(cal.HOUR_OF_DAY, hour);
        cal.set(cal.MINUTE, min);
        return cal.getTime();
    }


    /**
     * main
     *
     * @param args args
     *
     * @throws Exception On badness
     */
    public static void main(String[] args) throws Exception {
        SimpleDateFormat fmt  = new SimpleDateFormat("yyyy-mm-dd HH");
        Date             dttm = fmt.parse("2007-12-01 00.65");
        System.err.println("dttm:" + dttm);
        if (true) {
            return;
        }

        Date now = new Date();
        for (int i = 0; i < args.length; i++) {
            Date   fromDttm = null;
            String fromDate = args[i];
            fromDttm = DateUtil.parseRelative(new Date(), fromDate, 0);
            System.err.println(args[i] + "=" + fromDttm);
        }

    }


}

