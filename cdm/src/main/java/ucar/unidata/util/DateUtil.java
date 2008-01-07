/*
 * $Id: DateUtil.java,v 1.11 2007/07/02 21:14:00 jeffmc Exp $
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
import java.util.List;
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


    public static final TimeZone TIMEZONE_GMT = TimeZone.getTimeZone("GMT");


    /** logger */
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

    
    private static final String[] formats = {
        "yyyy-MM-dd'T'HH:mm:ss",
        "yyyy-MM-dd HH:mm:ss",
        "yyyyMMdd'T'HHmmss",
        "yyyy-MM-dd",
        "EEE MMM dd HH:mm:ss Z yyyy"
    };

    private static SimpleDateFormat[] sdfs;
    private static SimpleDateFormat lastSdf;


    public static Date roundByDay(Date dttm,int day) {
        if(day ==0) return dttm;
        if(day<0) day++;
        Calendar cal = Calendar.getInstance(TIMEZONE_GMT);
        cal.setTimeInMillis(dttm.getTime());
        cal.clear(Calendar.MILLISECOND);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.add(Calendar.DAY_OF_YEAR, day);
        return new Date( cal.getTimeInMillis());
    }

    public static SimpleDateFormat findFormatter(String dateString) {
        if(sdfs == null) {
            sdfs = new SimpleDateFormat[formats.length];
            for(int i=0;i<formats.length;i++) {
                sdfs[i] = new SimpleDateFormat(formats[i]);
            }
        }
        if(lastSdf!=null) {
            try {
                lastSdf.parse(dateString);
                return lastSdf;
            } catch(ParseException pe) {}
        }

        for(int i=0;i<formats.length;i++) {
            try {
                sdfs[i].parse(dateString);
                lastSdf = sdfs[i];
                return sdfs[i];
            } catch(ParseException pe) {}
        }
        throw new IllegalArgumentException ("Could not find date format for:" + dateString);
    }


    public static Date parseRelative(Date baseDate, String s, int roundDays) throws java.text.ParseException {
        s = s.trim();
        Calendar cal = Calendar.getInstance(TIMEZONE_GMT);
        cal.setTimeInMillis(baseDate.getTime());
        Date dttm = null;
        if(s.equals("now")) {
            dttm = cal.getTime();
        } else if(s.equals("today")) {
            dttm = cal.getTime();
        } else if(s.equals("yesterday")) {
            dttm = new Date(cal.getTime().getTime()-daysToMillis(1));
        } else if(s.equals("tomorrow")) {
            dttm = new Date(cal.getTime().getTime()+daysToMillis(1));
        } else if(s.startsWith("last")  || s.startsWith("next")) { 
            List toks = StringUtil.split(s, " ", true,true);
            if(toks.size()!=2) {
                throw new IllegalArgumentException("Bad time format:" + s);
            }
            int factor = (toks.get(0).equals("last")?-1:+1);
            String unit  = (String) toks.get(1);
            if(unit.equals("week")) {
                cal.add(Calendar.WEEK_OF_MONTH,factor);
            } else  if(unit.equals("month")) {
                cal.add(Calendar.MONTH,factor);
            } else  if(unit.equals("year")) {
                cal.add(Calendar.YEAR,factor);
            } else  if(unit.equals("century")) {
                cal.add(Calendar.YEAR,factor*100);
            } else  if(unit.equals("millenium")) {
                cal.add(Calendar.YEAR,factor*1000);
            } else {
                throw new IllegalArgumentException("Bad time format:" + s + " unknown time field:" + unit);
            }
            dttm =  cal.getTime();
        } 
        if(dttm!=null) {
            return roundByDay(dttm,roundDays);
        }
        return null;
    }


    public static Date parse(String s) throws java.text.ParseException {
        SimpleDateFormat sdf = findFormatter(s);
        return sdf.parse(s);
    }

    public static double[] toSeconds(String[]s) throws java.text.ParseException {
        double[]d = new double[s.length];
        if(s.length == 0) return d;
        SimpleDateFormat sdf = findFormatter(s[0]);
        double lastTime = 0;
        for(int i=0;i<s.length;i++) {
            d[i] = sdf.parse(s[i]).getTime()/1000.0;
            if(d[i]< lastTime) System.out.println ("****" + s[i]);
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

    public static Date getRelativeDate(Date from, String relativeTimeString) {
        return new Date(from.getTime()+ parseRelativeTimeString(relativeTimeString));
    }

    public static long parseRelativeTimeString(String relativeTimeString) {
        List toks = StringUtil.split(relativeTimeString," ", true, true);
        if(toks.size()!=2)
            throw new IllegalArgumentException("Bad format for relative time string:" + relativeTimeString +" Needs to be of the form: +/-<number> timeunit");
        int delta=0;
        try {
            String s = toks.get(0).toString();
            int factor = 1;
            if(s.startsWith("+")) {
                s = s.substring(1);
            } else if(s.startsWith("-")) {
                s = s.substring(1);
                factor = -1;
            }
            delta = factor*new Integer(s);
        } catch(Exception exc) {
            throw new IllegalArgumentException("Bad format for relative time string:" + relativeTimeString+" Could not parse initial number:" + toks.get(0));
        }
        String what = (String)toks.get(1);
        long milliseconds = 0;
        if(what.startsWith("second")) {
            milliseconds = delta*1000;
        } else if(what.startsWith("minute")) {
            milliseconds = 60*delta*1000;
        } else if(what.startsWith("hour")) {
            milliseconds = 60*60*delta*1000;
        } else if(what.startsWith("day")) {
            milliseconds = 24*60*60*delta*1000;
        } else if(what.startsWith("week")) {
            milliseconds = 7*24*60*60*delta*1000;
        } else if(what.startsWith("month")) {
            milliseconds = 30*24*60*60*delta*1000;
        } else if(what.startsWith("year")) {
            milliseconds = 365*24*60*60*delta*1000;
        } else if(what.startsWith("century")) {
            milliseconds = 100*365*24*60*60*delta*1000;
        } else if(what.startsWith("millenium")) {
            milliseconds = 1000*365*24*60*60*delta*1000;
        } else {
            throw new IllegalArgumentException("Unknown unit in relative time string:" + relativeTimeString);
        }
        return milliseconds;
    }



    public static void main(String[]args) throws Exception {
        SimpleDateFormat fmt = new SimpleDateFormat("yyyy-mm-dd HH");
        Date dttm = fmt.parse("2007-12-01 00.65");
        System.err.println("dttm:" + dttm);
        if(true) return;

        Date now = new Date();
        for(int i=0;i<args.length;i++) {
            Date fromDttm = null;
            String fromDate = args[i];
            fromDttm = DateUtil.parseRelative(new Date(),fromDate,0);
            System.err.println (args[i] + "=" + fromDttm);
        }

    } 


}

