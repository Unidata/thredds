/* This file is Copyright (c) 2005 Robert Alten Simons (info@cohort.com).
 * See the MIT/X-like license in LICENSE.txt.
 * For more information visit www.cohort.com or contact info@cohort.com.
 */
package com.cohort.util;

import com.cohort.array.DoubleArray;
import com.cohort.array.StringArray;

import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;

/**
 * This class has static methods for dealing with dates and times.
 *
 * <p><b>newGCalendar only accounts for daylight savings if 
 * your computer is correctly 
 * set up.</b> E.g., in Windows, make sure "Start : Control Panel : 
 * Date and Time : Time Zone : Automatically adjust clock for daylight
 * savings changes" is checked. Otherwise, the TimeZone used by GregorianCalendar
 * will be for standard time (not including daylight savings time, if any).
 *
 * <p>Comments about working with Java's GregorianCalendar class:
 * <ul>
 * <li>GregorianCalendar holds millis since Jan 1, 1970 and a timeZone
 *   which influences the values that get/set deal with.
 * <li>Using a simpleDateFormat to parse a string to a Gregorian Calendar: 
 *   the simpleDateFormat has a timeZone which specified where the
 *   the strings value is from (e.g., 2005-10-31T15:12:10 in PST).
 *   When parsed, it is then interpreted by the GregorianCalendar's timeZone
 *   (e.g., it was 3pm PST but now I'll treat it as 6pm EST).
 * <li>Similarly, using a simpleDateFormat to format a Gregorian Calendar
 *   to a String: 
 *   the simpleDateFormat has a timeZone which specified where the
 *   the strings value will be for (e.g., 6pm EST will be formatted as
 *   5pm Central).
 * </ul>
 *
 * <p>But this class seeks to simplify things to the more common cases
 * of parsing and formatting using the same time zone as the GregorianCalendar
 * class, and offering GregorianCalendar constructors for Local (with 
 * daylight savings if that is what your area does) and Zulu (aka GMT and UTC,
 * which doesn't ever use daylight savings).
 *
 * <p>A summary of ISO 8601 Date Time formats is at
 * http://www.cl.cam.ac.uk/~mgk25/iso-time.html 
 * http://en.wikipedia.org/wiki/ISO_8601
 * and http://dotat.at/tmp/ISO_8601-2004_E.pdf 
 * (was http://www.iso.org/iso/date_and_time_format)
 * and years B.C at http://www.tondering.dk/claus/cal/node4.html#SECTION00450000000000000000
 *
 * <p>Calendar2 does not use ERA designations. It uses negative year values for B.C years
 * (calendar2Year = 1 - BCYear).  Note that BCYears are 1..., so 1 BC is calendar2Year 0 (or 0000),
 * and 2 BC is calendar2Year -1 (or -0001).
 *
 */
public class Calendar2 {

    //useful static variables
    public final static int ERA         = Calendar.ERA;
    public final static int BC          = GregorianCalendar.BC;
    public final static int YEAR        = Calendar.YEAR;
    public final static int MONTH       = Calendar.MONTH;  //java counts 0..
    public final static int DATE        = Calendar.DATE;   //1..  of month
    public final static int DAY_OF_YEAR = Calendar.DAY_OF_YEAR; //1..
    public final static int HOUR        = Calendar.HOUR;   //0..11     //rarely used
    public final static int HOUR_OF_DAY = Calendar.HOUR_OF_DAY; //0..23
    public final static int MINUTE      = Calendar.MINUTE;
    public final static int SECOND      = Calendar.SECOND;
    public final static int MILLISECOND = Calendar.MILLISECOND;
    public final static int AM_PM       = Calendar.AM_PM;
    public final static int ZONE_OFFSET = Calendar.ZONE_OFFSET; //millis
    public final static int DST_OFFSET  = Calendar.DST_OFFSET;  //millis

    /*
    //for thread safety, always use:  synchronized(<itself>) {<use...>}
    //do before defaultValue=  (below)
    private static SimpleDateFormat isoDateTimeFormat = 
        new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
    private static SimpleDateFormat isoDateFormat = 
        new SimpleDateFormat("yyyy-MM-dd");
    private static SimpleDateFormat isoDateHMFormat = 
        new SimpleDateFormat("yyyy-MM-dd HH:mm");
    private static SimpleDateFormat compactDateTimeFormat = 
        new SimpleDateFormat("yyyyMMddHHmmss");
    private static SimpleDateFormat YYYYDDDFormat = 
        new SimpleDateFormat("yyyyDDD");
    private static SimpleDateFormat YYYYMMFormat = 
        new SimpleDateFormat("yyyyMM");
    */

    public final static int MINUTES_PER_DAY    = 1440;
    public final static int MINUTES_PER_7DAYS  =  7 * MINUTES_PER_DAY; //10080
    public final static int MINUTES_PER_30DAYS = 30 * MINUTES_PER_DAY; //43200
    public final static int SECONDS_PER_MINUTE = 60; 
    public final static int SECONDS_PER_HOUR   = 60 * 60; //3600
    public final static int SECONDS_PER_DAY    = 24 * 60 * 60; //86400   31Days=2678400  365days=31536000
    public final static long MILLIS_PER_MINUTE = SECONDS_PER_MINUTE * 1000L; 
    public final static long MILLIS_PER_HOUR   = SECONDS_PER_HOUR * 1000L; 
    public final static long MILLIS_PER_DAY    = SECONDS_PER_DAY * 1000L; 

    public final static String SECONDS_SINCE_1970 = "seconds since 1970-01-01T00:00:00Z";

    public final static TimeZone zuluTimeZone = TimeZone.getTimeZone("Zulu");
    private final static String[] MONTH_3 = {
        "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
    private final static String[] MONTH_FULL = { 
        "January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December"};
    private final static String[] DAY_OF_WEEK_3 = { //corresponding to DAY_OF_WEEK values
        "", "Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};
    private final static String[] DAY_OF_WEEK_FULL = { 
        "", "Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"};

    /** The IDEAL values are used for makeIdealGC. */
    public static String IDEAL_N_OPTIONS[] = new String[100];
    static {
        for (int i = 0; i < 100; i++)
            IDEAL_N_OPTIONS[i] = "" + (i + 1);
    }
    public static String IDEAL_UNITS_OPTIONS[] = new String[]{
        "second(s)", "minute(s)", "hour(s)", "day(s)", "month(s)", "year(s)"};
    public static double IDEAL_UNITS_SECONDS[] = new double[]{ //where imprecise, these are on the low end
        1, 60, SECONDS_PER_HOUR, SECONDS_PER_DAY, 
        30.0  * SECONDS_PER_DAY,    
        365.0 * SECONDS_PER_DAY};
    public static int IDEAL_UNITS_FIELD[] = new int[]{
        SECOND, MINUTE, HOUR_OF_DAY, DATE, MONTH, YEAR};  //month is 0..

    /**
     * Set this to true (by calling verbose=true in your program, 
     * not but changing the code here)
     * if you want lots of diagnostic messages sent to String2.log.
     */
    public static boolean verbose = false; 

    /**
     * Set this to true (by calling reallyVerbose=true in your program, 
     * not but changing the code here)
     * if you want lots of diagnostic messages sent to String2.log.
     */
    public static boolean reallyVerbose = false; 

    /**
     * For diagnostic purposes, this returns the name of one of the fields defined above (or "unknown_field").
     *
     * @param field
     * @return the name of the field
     */
    public static String fieldName(int field) {
        if (field == YEAR)        return "year";
        if (field == MONTH)       return "month";
        if (field == DATE)        return "date";
        if (field == DAY_OF_YEAR) return "day_of_year";
        if (field == HOUR)        return "hour";
        if (field == HOUR_OF_DAY) return "hour_of_day";
        if (field == MINUTE)      return "minute";
        if (field == SECOND)      return "second";
        if (field == MILLISECOND) return "millisecond";
        if (field == AM_PM)       return "am_pm";
        if (field == ZONE_OFFSET) return "zone_offset";
        if (field == DST_OFFSET)  return "dst_offset";  
        return "unknown_field";
    }

    /**
     * This converts a string "[units] since [isoDate]" 
     * (e.g., "minutes since 1985-01-01") into 
     * a baseSeconds (seconds since 1970-01-01) and a factor ("minutes" returns 60).
     * <br>So simplistically, epochSeconds = storedTime * factor + baseSeconds. 
     * <br>Or simplistically, storedTime = (epochSeconds - baseSeconds) / factor.
     *
     * <p>WARNING: don't use the equations above. Use unitsSinceToEpochSeconds or
     * epochSecondsToUnitsSince which correctly handle special cases.
     *
     * @param tsUnits e.g., "minutes since 1985-01-01".
     *   This may include hours, minutes, seconds, decimal, and Z or timezone offset (default=Zulu).  
     * @return double[]{baseSeconds, factorToGetSeconds} 
     * @throws Exception if trouble (tsUnits is null or invalid)
     */
    public static double[] getTimeBaseAndFactor(String tsUnits) throws Exception {
        String errorInMethod = String2.ERROR + " in Calendar2.getTimeBaseAndFactor(" + tsUnits + "):\n";

        Test.ensureNotNull(tsUnits, errorInMethod + "units string is null.");       
        int sincePo = tsUnits.toLowerCase().indexOf(" since ");
        if (sincePo <= 0)
            throw new SimpleException(errorInMethod + "units string doesn't contain \" since \".");
        double factorToGetSeconds = factorToGetSeconds(tsUnits.substring(0, sincePo));
        GregorianCalendar baseGC = parseISODateTimeZulu(tsUnits.substring(sincePo + 7));
        double baseSeconds = baseGC.getTimeInMillis() / 1000.0;
        //String2.log("  time unitsString (" + tsUnits + 
        //    ") converted to factorToGetSeconds=" + factorToGetSeconds +
        //    " baseSeconds=" + baseSeconds);
        return new double[]{baseSeconds, factorToGetSeconds};
    }

    /** 
     * This converts a unitsSince value into epochSeconds.
     * This properly handles 'special' factorToGetSeconds values (for month and year).
     *
     * @param baseSeconds
     * @param factorToGetSeconds
     * @param unitsSince
     * @return seconds since 1970-01-01 (or NaN if unitsSince is NaN)
     */
    public static double unitsSinceToEpochSeconds(double baseSeconds, 
        double factorToGetSeconds, double unitsSince)  {
        if (factorToGetSeconds >= 30 * SECONDS_PER_DAY) {  //i.e. >= a month
            //floor yields consistent results below for decimal months
            int intUnitsSince = Math2.roundToInt(Math.floor(unitsSince)); 
            if (intUnitsSince == Integer.MAX_VALUE)
                return Double.NaN;
            int field;
            if      (factorToGetSeconds ==  30 * SECONDS_PER_DAY) field = MONTH;
            else if (factorToGetSeconds == 360 * SECONDS_PER_DAY) field = YEAR;
            else throw new RuntimeException(
                String2.ERROR + " in Calendar2.unitsSinceToEpochSeconds: factorToGetSeconds=\"" + 
                factorToGetSeconds + "\" not expected.");
            GregorianCalendar gc = epochSecondsToGc(baseSeconds);
            gc.add(field, intUnitsSince); 
            if (unitsSince != intUnitsSince) {
                double frac = unitsSince - intUnitsSince;  //will be positive because floor was used
                if (field == MONTH) {
                    //Round fractional part to nearest day.  Better if based on nDays in current month?
                    //(Note this differs from UDUNITS month = 3.15569259747e7 / 12 seconds.)
                    gc.add(DATE, Math2.roundToInt(frac * 30)); 
                } else if (field == YEAR) {  
                    //Round fractional part to nearest month.
                    //(Note this differs from UDUNITS year = 3.15569259747e7 seconds.)
                    gc.add(MONTH, Math2.roundToInt(frac * 12)); 
                }
            }
            return gcToEpochSeconds(gc);
        }
        return baseSeconds + unitsSince * factorToGetSeconds;
    }
        
    /** 
     * This converts an epochSeconds value into a unitsSince value.
     * This properly handles 'special' factorToGetSeconds values (for month and year).
     *
     * @param baseSeconds
     * @param factorToGetSeconds
     * @param epochSeconds
     * @return seconds since 1970-01-01 (or NaN if epochSeconds is NaN)
     */
    public static double epochSecondsToUnitsSince(double baseSeconds, 
        double factorToGetSeconds, double epochSeconds)  {
        if (factorToGetSeconds >= 30 * SECONDS_PER_DAY) {
            if (!Math2.isFinite(epochSeconds))
                return Double.NaN;
            GregorianCalendar es = epochSecondsToGc(epochSeconds);
            GregorianCalendar bs = epochSecondsToGc(baseSeconds);
            if (factorToGetSeconds == 30 * SECONDS_PER_DAY) {
                //months (and days)
//expand this to support fractional months???
                int esm = getYear(es) * 12 + es.get(MONTH);
                int bsm = getYear(bs) * 12 + bs.get(MONTH);
                return esm - bsm;
            } else if (factorToGetSeconds == 360 * SECONDS_PER_DAY) {
                //years (and months)
//expand this to support fractional years???
                return getYear(es) - getYear(bs);               
            } else throw new RuntimeException(
                String2.ERROR + " in Calendar2.epochSecondsToUnitsSince: factorToGetSeconds=\"" + 
                factorToGetSeconds + "\" not expected.");
        }
        return (epochSeconds - baseSeconds) / factorToGetSeconds;
    }
        
    /**
     * This returns the factor to multiply by 'units' data to get seconds
     * data (e.g., "minutes" returns 60).
     * This is used for part of dealing with udunits-style "minutes since 1970-01-01"-style
     * strings.
     *
     * @param units
     * @return the factor to multiply by 'units' data to get seconds data.
     *    Since there is no exact value for months or years, this returns
     *    special values of 30*SECONDS_PER_DAY and 360*SECONDS_PER_DAY, respectively. 
     * @throws Exception if trouble (e.g., units is null or not an expected value)
     */
    public static double factorToGetSeconds(String units) throws Exception {
        units = units.trim().toLowerCase();
        if (units.equals("ms") || 
            units.equals("msec") || 
            units.equals("msecs") || 
            units.equals("millis") ||
            units.equals("millisec") ||
            units.equals("millisecs") ||
            units.equals("millisecond") ||
            units.equals("milliseconds")) return 0.001;      
        if (units.equals("s") || 
            units.equals("sec") || 
            units.equals("secs") || 
            units.equals("second") || 
            units.equals("seconds")) return 1;                 
        if (units.equals("m") || 
            units.equals("min") || 
            units.equals("mins") || 
            units.equals("minute") || 
            units.equals("minutes")) return SECONDS_PER_MINUTE; 
        if (units.equals("h") || 
            units.equals("hr") || 
            units.equals("hrs") || 
            units.equals("hour") || 
            units.equals("hours")) return SECONDS_PER_HOUR; 
        if (units.equals("d") || 
            units.equals("day") || 
            units.equals("days")) return SECONDS_PER_DAY;   
        if (units.equals("week") || 
            units.equals("weeks")) return 7 * SECONDS_PER_DAY;   
        if (units.equals("mon") ||
            units.equals("mons") ||
            units.equals("month") ||
            units.equals("months")) return 30 * SECONDS_PER_DAY;  
        if (units.equals("yr") || 
            units.equals("yrs") || 
            units.equals("year") || 
            units.equals("years")) return 360 * SECONDS_PER_DAY;   
        Test.error(String2.ERROR + " in Calendar2.factorToGetSeconds: units=\"" + units + "\" is invalid.");
        return Double.NaN; //won't happen, but method needs return statement
    }
     
    /**
     * This converts an ISO Zulu dateTime String to seconds since 1970-01-01T00:00:00Z,
     * rounded to the nearest milli.
     * [Before 2012-05-22, millis were removed. Now they are kept.]
     * In many ways trunc would be better, but doubles are often bruised.
     * round works symmetrically with + and - numbers.
     * If any of the end of the dateTime is missing, a trailing portion of 
     * "1970-01-01T00:00:00" is added.
     * The 'T' connector can be any non-digit.
     * This may include hours, minutes, seconds, decimal, and Z or timezone offset (default=Zulu).  
     *
     * @param isoZuluString
     * @return seconds 
     * @throws Exception if trouble (e.g., input is null or invalid format)
     */
    public static double isoStringToEpochSeconds(String isoZuluString) {
        //pre 2012-05-22 was return Math2.floorDiv(isoZuluStringToMillis(isoZuluString), 1000);
        return isoZuluStringToMillis(isoZuluString) / 1000.0;
    }

    /**
     * This is like isoStringToEpochSeconds, but returns NaN if trouble.
     */
    public static double safeIsoStringToEpochSeconds(String isoZuluString) {
        try {
            //pre 2012-05-22 was return Math2.floorDiv(isoZuluStringToMillis(isoZuluString), 1000);
            return isoZuluStringToMillis(isoZuluString) / 1000.0;
        } catch (Exception e) {
            return Double.NaN;
        }
    }

    /**
     * This converts an EDDTable "now-nUnits" string to epochSeconds.
     * - can also be + or space.
     * units can be singular or plural.
     *
     * @param nowString  
     * @return epochSeconds  (rounded up to the next second) (or Double.NaN if trouble)
     * @throws SimpleException if trouble
     */
    public static double nowStringToEpochSeconds(String nowString) {

        //now is next second (ms=0)
        GregorianCalendar gc = Calendar2.newGCalendarZulu();
        gc.add(Calendar2.SECOND, 1);
        gc.set(Calendar2.MILLISECOND, 0); 
        String tError = 
            "Query error: Timestamp constraints with \"now\" must be in the form " +
            "\"now(+|-)[positiveInteger](seconds|minutes|hours|days|months|years)\".  " +
            "\"" + nowString + "\" is invalid.";
        if (nowString == null || !nowString.startsWith("now"))
            throw new SimpleException(tError);
        if (nowString.length() > 3) {
            // e.g., now-5hours
            char ch = nowString.charAt(3);
            int start = -1;  //trouble
            //non-%encoded '+' will be decoded as ' ', so treat ' ' as equal to '+' 
            if (ch == '+' || ch == ' ') start = 4;  
            else if (ch == '-') start = 3;
            else throw new SimpleException(tError);

            //keep going?  parse the number
            int n = 0;
            if (start > 0) {
                int end = 4;
                while (nowString.length() > end && String2.isDigit(nowString.charAt(end)))
                    end++;
                n = String2.parseInt(nowString.substring(start, end));
                if (n == Integer.MAX_VALUE) {
                    throw new SimpleException(tError);
                } else { 
                    start = end;
                }
            }

            //keep going?  find the units, adjust gc
            if (start > 0) {
                //test sUnits.equals to ensure no junk at end of constraint
                String sUnits = nowString.substring(start);  
                if (     sUnits.equals("second") || 
                         sUnits.equals("seconds"))
                    gc.add(Calendar2.SECOND, n);
                else if (sUnits.equals("minute") || 
                         sUnits.equals("minutes"))
                    gc.add(Calendar2.MINUTE, n);
                else if (sUnits.equals("hour") || 
                         sUnits.equals("hours"))
                    gc.add(Calendar2.HOUR, n);
                else if (sUnits.equals("day") || 
                         sUnits.equals("days"))
                    gc.add(Calendar2.DATE, n);
                else if (sUnits.equals("month") || 
                         sUnits.equals("months"))
                    gc.add(Calendar2.MONTH, n);
                else if (sUnits.equals("year") || 
                         sUnits.equals("years"))
                    gc.add(Calendar2.YEAR, n);
                else throw new SimpleException(tError);
            }
        } 
        return Calendar2.gcToEpochSeconds(gc);
    }

    /**
     * This is like nowStringToEpochSeconds, but returns troubleValue if trouble.
     *
     * @param nowString  
     * @param troubleValue
     * @return epochSeconds   (or troubleValue if trouble)
     */
    public static double safeNowStringToEpochSeconds(String nowString, double troubleValue) {
        try {
            return nowStringToEpochSeconds(nowString);
        } catch (Throwable t) {
            String2.log(t.toString());
            return troubleValue;
        }
    }

    /**
     * This returns true if the string appears to be an ISO date/time 
     * (matching YYYY-MM...).
     *
     * @param s 
     * @return true if the string appears to be an ISO date/time 
     * (matching YYYY-MM...).
     */
    public static boolean isIsoDate(String s) {
        if (s == null)
            return false;
        return s.matches("-?\\d{4}-\\d{2}.*");
    }

    /**
     * This converts a GregorianCalendar to seconds since 1970-01-01T00:00:00Z.
     * Note that System.currentTimeMillis/1000 = epochSeconds(zulu).
     *
     * @param gc
     * @return seconds, including fractional seconds (Double.NaN if trouble)
     * @throws Exception if trouble (e.g., gc is null)
     */
    public static double gcToEpochSeconds(GregorianCalendar gc) {
        return gc.getTimeInMillis() / 1000.0;
    }

    /**
     * This converts seconds since 1970-01-01T00:00:00Z to a GregorianCalendar.
     *
     * @param seconds  (including fractional seconds)
     * @return an iso zulu time-zone GregorianCalendar   (rounded to nearest ms)
     * @throws Exception if trouble (e.g., seconds is NaN)
     */
    public static GregorianCalendar epochSecondsToGc(double seconds) {
        if (!Math2.isFinite(seconds))
            Test.error(String2.ERROR + " in epochSecondsToGc: seconds value is NaN!");
        return newGCalendarZulu(Math2.roundToLong(seconds * 1000.0));
    }

    /**
     * This converts an ISO Zulu dateTime String to hours since 1970-01-01T00:00:00Z,
     * rounded to the nearest hour.
     * In many ways trunc would be better, but doubles are often bruised.
     * round works symmetrically with + and - numbers.
     * If any of the end of the dateTime is missing, a trailing portion of 
     * "1970-01-01T00:00:00Z" or "1970-01-01T00:00:00-00:00" is added.
     * The 'T' connector can be any non-digit.
     * This may include hours, minutes, seconds, decimal, and timezone offset (default=Zulu).  
     *
     * @param isoZuluString
     * @return seconds 
     * @throws Exception if trouble (e.g., input is null or invalid format)
     */
    public static int isoStringToEpochHours(String isoZuluString) {
        long tl = isoZuluStringToMillis(isoZuluString);
        return Math2.roundToInt(tl / (double)MILLIS_PER_HOUR);
    }

    /**
     * This converts seconds since 1970-01-01T00:00:00Z  
     * to an ISO Zulu dateTime String with 'T'.
     * The doubles are rounded to the nearest second.
     * In many ways trunc would be better, but doubles are often bruised.
     * round works symmetrically with + and - numbers.
     *
     * @param seconds  with optional fractional part
     * @return isoZuluString with 'T' (without the trailing Z)
     * @throws Exception if trouble (e.g., seconds is NaN)
     */
    public static String epochSecondsToIsoStringT(double seconds) {
        if (!Math2.isFinite(seconds))
            Test.error(String2.ERROR + " in epochSecondsToIsoStringT: seconds is NaN!");
        return millisToIsoZuluString(Math2.roundToLong(seconds * 1000));
    }

    /**
     * This is like epochSecondsToIsoStringT, but includes millis.
     */
    public static String epochSecondsToIsoStringT3(double seconds) {
        if (!Math2.isFinite(seconds))
            Test.error(String2.ERROR + " in epochSecondsToIsoStringT3: seconds is NaN!");
        return millisToIso3ZuluString(Math2.roundToLong(seconds * 1000));
    }

    /**
     * This is like epochSecondsToIsoStringT, but returns NaNString if seconds is NaN.
     */
    public static String safeEpochSecondsToIsoStringT(double seconds, String NaNString) {
        return Math2.isFinite(seconds)? 
            millisToIsoZuluString(Math2.roundToLong(seconds * 1000)) :
            NaNString;
    }

    /**
     * This is like epochSecondsToIsoStringT, but add "Z" at end of time,
     * and returns NaNString if seconds is NaN..
     */
    public static String safeEpochSecondsToIsoStringTZ(double seconds, String NaNString) {
        return Math2.isFinite(seconds)? 
            millisToIsoZuluString(Math2.roundToLong(seconds * 1000)) + "Z" :
            NaNString;
    }

    /**
     * This is like epochSecondsToIsoStringT3, but returns NaNString if seconds is NaN.
     */
    public static String safeEpochSecondsToIsoStringT3(double seconds, String NaNString) {
        return Math2.isFinite(seconds)? 
            millisToIso3ZuluString(Math2.roundToLong(seconds * 1000)) :
            NaNString;
    }

    /**
     * This is like epochSecondsToIsoStringT3, but add "Z" at end of time,
     * and returns NaNString if seconds is NaN..
     */
    public static String safeEpochSecondsToIsoStringT3Z(double seconds, String NaNString) {
        return Math2.isFinite(seconds)? 
            millisToIso3ZuluString(Math2.roundToLong(seconds * 1000)) + "Z" :
            NaNString;
    }

    /**
     * This is like safeEpochSecondsToIsoStringT3Z, but returns a 
     * limited precision string.
     *
     * @param time_precision can be "1970", "1970-01", "1970-01-01", "1970-01-01T00Z",
     *    "1970-01-01T00:00Z", "1970-01-01T00:00:00Z" (used if time_precision not matched), 
     *    "1970-01-01T00:00:00.0Z", "1970-01-01T00:00:00.00Z", "1970-01-01T00:00:00.000Z".
     *    Or any of those without "Z".  But ERDDAP requires any format with hours(min(sec)) to have Z.
     */
    public static String epochSecondsToLimitedIsoStringT(String time_precision,
        double seconds, String NaNString) {

        if (!Math2.isFinite(seconds)) 
            return NaNString;

        //should be floor(?), but round avoids issues with computer precision
        return limitedFormatAsISODateTimeT(time_precision, 
            newGCalendarZulu(Math2.roundToLong(seconds * 1000))); 
    }

    /**
     * This converts seconds since 1970-01-01T00:00:00Z  
     * to an ISO Zulu dateTime String with space.
     * The doubles are rounded to the nearest milli.
     * [Before 2012-05-22, millis were removed. Now they are kept.]
     * In many ways trunc would be better, but doubles are often bruised.
     * round works symmetrically with + and - numbers.
     *
     * @param seconds   with optional fractional part
     * @return isoZuluString with space (without the trailing Z)
     * @throws Exception if trouble (e.g., seconds is NaN)
     */
    public static String epochSecondsToIsoStringSpace(double seconds) {
        if (!Math2.isFinite(seconds))
            Test.error(String2.ERROR + " in epochSecondsToIsoStringSpace: seconds value is NaN!");
        String s = millisToIsoZuluString(Math2.roundToLong(seconds * 1000));
        return String2.replaceAll(s, 'T', ' ');
    }


    /**
     * This converts hours since 1970-01-01T00:00:00Z 
     * to an ISO Zulu dateTime String 'T'.
     * If your hours are doubles, use Math2.roundToInt first.
     * In many ways trunc would be better, but doubles are often bruised.
     * round works symmetrically with + and - numbers.
     *
     * @param hours
     * @return isoZuluString 'T' (without the trailing Z).
     *    If hours==Integer.MAX_VALUE, this returns null.
     * @throws Exception if trouble (e.g., hours is Integer.MAX_VALUE)
     */
    public static String epochHoursToIsoString(int hours) {
        if (hours == Integer.MAX_VALUE)
            Test.error(String2.ERROR + " in epochHoursToIsoString: hours value is Integer.MAX_VALUE!");
        return millisToIsoZuluString(hours * MILLIS_PER_HOUR);
    }

    /**
     * This returns a 3 character month name (eg. "Jan").
     *
     * @param month 1..12
     * @throws Exception if month is out of range
     */
    public static String getMonthName3(int month) {
        return MONTH_3[month - 1];
    }

    /**
     * This returns the full month name (e.g., "January").
     *
     * @param month 1..12
     * @throws Exception if month is out of range
     */
    public static String getMonthName(int month) {
        return MONTH_FULL[month - 1];
    }

    /**
     * This returns a gregorianCalendar object which has the correct 
     * current time 
     * (e.g., wall clock time, for the local time zone, 
     * which includes daylight savings, if applicable) and the local time zone.
     *
     * @return a new GregorianCalendar object (local time zone)
     */
    public static GregorianCalendar newGCalendarLocal() {
        GregorianCalendar gc = new GregorianCalendar();
        //TimeZone tz = gc.getTimeZone();
        //String2.log("getGCalendar inDaylightTime="+ tz.inDaylightTime(gc.getTime()) +
        //    " useDaylightTime=" + tz.useDaylightTime() +
        //    " timeZone=" + tz);
        return gc;
    }

    /**
     * Get a GregorianCalendar object with the specified millis time (UTC), 
     * but with the local time zone (when displayed).
     *
     * @return the GregorianCalendar object.
     * @throws Exception if trouble (e.g., millis == Long.MAX_VALUE)
     */
    public static GregorianCalendar newGCalendarLocal(long millis) {
        if (millis == Long.MAX_VALUE)
            Test.error(String2.ERROR + " in newGCalendarLocal: millis value is Long.MAX_VALUE!");
        GregorianCalendar gcL = newGCalendarLocal();
        gcL.setTimeInMillis(millis);
        return gcL;
    }

    /**
     * Get a GregorianCalendar object with the current UTC 
     * (A.K.A., GMT or Zulu) time and a UTC time zone.
     * You can find the current Zulu/GMT time at: http://www.xav.com/time.cgi
     * Info about UTC vs GMT vs TAI... see http://www.leapsecond.com/java/gpsclock.htm.
     * And there was another good site... can't find it.
     *
     * @return the GregorianCalendar object for right now (Zulu time zone)
     */
    public static GregorianCalendar newGCalendarZulu() {
        //GregorianCalendar gc = new GregorianCalendar();
        //gc.add(MILLISECOND, -TimeZone.getDefault().getOffset());  
        //return gc;

        //* Note that the time zone is still local, but the day and hour are correct for gmt. 
        //* To try to do this correctly leads to Java's timeZone hell hole.
        //return localToUtc(new GregorianCalendar());

        return new GregorianCalendar(zuluTimeZone);
    }

    /**
     * Get a GregorianCalendar object with the specified millis time (UTC) 
     * and a UTC time zone.
     *
     * @return the GregorianCalendar object.
     * @throws Exception if trouble (e.g., millis == Long.MAX_VALUE)
     */
    public static GregorianCalendar newGCalendarZulu(long millis) {
        if (millis == Long.MAX_VALUE)
            Test.error(String2.ERROR + " in newGCalendarZulu: millis value is Long.MAX_VALUE!");
        GregorianCalendar gcZ = newGCalendarZulu();
        gcZ.setTimeInMillis(millis);
        return gcZ;
    }

    /**
     * Given a time in the local time zone, this determines the equivalent 
     * time in Greenwich England.
     * Note that the time zone is still local, but the day and hour are correct 
     * for gmt. 
     * To try to do this correctly leads to Java's timeZone hell hole.
     *
     * @return the same GregorianCalendar object (for convenience)
     */
//    public static GregorianCalendar localToUtc(GregorianCalendar gc) {
//        gc.add(MILLISECOND, -gc.get(ZONE_OFFSET) - gc.get(DST_OFFSET)); 
//        return gc;
//    }

    /**
     * Given a time in Greenwich, this determines the equivalent local time.
     * The time zone of the gc should be local (even though it holds the day/hour
     * of a Greenwich time).
     * To try to do this correctly leads to Java's timeZone hell hole.
     *
     * @return the same GregorianCalendar object (for convenience)
     */
//    public static GregorianCalendar utcToLocal(GregorianCalendar gc) {
//        gc.add(MILLISECOND, gc.get(ZONE_OFFSET) + gc.get(DST_OFFSET)); 
//        return gc;
//    }

    /**
     * This converts a GregorianCalendar
     * (where year/month/day/hour/min/sec indicate UTC time,
     * but time zone may be incorrect) to millis since Jan 1 1970 UTC.
     * This is at least correct from 1901 to 2099 (every intervening %4=0 year 
     * was indeed a leap year). 
     */
/*    public static long utcToMillis(GregorianCalendar gc) {
        int years = getYear(gc) - 1970;
        //-.1 ensures that x.5 rounds down
        int leapYears = Math2.roundToInt((years / 4.0) - .1);
        int nDays = leapYears * 366 + (years - leapYears) * 365 + 
            gc.get(DAY_OF_YEAR) - 1; //DAY_OF_YEAR is 1..
        return ((((nDays * 24 +
                gc.get(HOUR_OF_DAY)) * 60L +
                gc.get(MINUTE)) * 60L +
                gc.get(SECOND)) * 1000L) +
            gc.get(MILLISECOND);
    }
*/
    /**
     * This sets endGC so that it will appear to have the UTC time created by 
     * adding 'millis' to 1970-01-01T00:00:00Z (with no daylight savings influence).
     *
     * @param millis some number of millis since 1970-01-01T00:00:00Z
     * @param endGC captures the result.
     *   It will appear to have the UTC date/time (ignore the time zone).
     */
/*    public static void millisToUTC(long millis, GregorianCalendar endGC) {
        //This is a clumsy approach. Isn't there a better way?

        //
        endGC.setTimeInMillis(millis);
//        if (endGC.getTimeZone().inDaylightTime(endGC.getTime()))  //adjust for daylight savings
//            endGC.add(MILLISECOND, -endGC.getTimeZone().getDSTSavings());
    }
*/

    /**
     * This converts a GregorianCalendar
     * (where year/month/day/hour/min/sec indicate UTC time,
     * but time zone may be incorrect) to seconds (rounded) since Jan 1 1970 UTC.
     * This is at least correct from 1901 to 2099 (every intervening %4=0 year 
     * was indeed a leap year). 
     */
/*    public static int utcToSeconds(GregorianCalendar gc) {
        return Math2.roundToInt(utcToMillis(gc) / 1000.0);
    }
*/
    /**
     * Get a GregorianCalendar object (local time zone) for the specified.
     * [Currently, it is lenient -- e.g., Dec 32 -> Jan 1 of the next year.]
     * Information can be retrieved via calendar.get(Calendar.XXXX),
     * where XXXX is one of the Calendar constants, like DAY_OF_YEAR.
     *
     * @param year  (e.g., 2005)
     * @param month (1..12)  (this is consciously different than Java's standard)
     * @param dayOfMonth (1..31)
     * @return the corresponding GregorianCalendar object (local time zone)
     * @throws Exception if trouble (e.g., year is Integer.MAX_VALUE)
     */
    public static GregorianCalendar newGCalendarLocal(int year, int month, int dayOfMonth) {
        if (year == Integer.MAX_VALUE)
            Test.error(String2.ERROR + " in newGCalendarLocal: year value is Integer.MAX_VALUE!");
        return new GregorianCalendar(year, month - 1, dayOfMonth); 
    }

    /**
     * Get a GregorianCalendar object (Zulu time zone) for the specified time.
     * [Currently, it is lenient -- e.g., Dec 32 -> Jan 1 of the next year.]
     * Information can be retrieved via calendar.get(Calendar.XXXX),
     * where XXXX is one of the Calendar constants, like DAY_OF_YEAR.
     *
     * @param year  (e.g., 2005)
     * @param month (1..12)  (this is consciously different than Java's standard)
     * @param dayOfMonth (1..31)
     * @return the corresponding GregorianCalendar object (Zulu time zone)
     * @throws Exception if trouble (e.g., year is Integer.MAX_VALUE)
     */
    public static GregorianCalendar newGCalendarZulu(int year, int month, int dayOfMonth) {
        if (year == Integer.MAX_VALUE)
            Test.error(String2.ERROR + " in newGCalendarZulu: year is Integer.MAX_VALUE!");
        return newGCalendarZulu(year, month, dayOfMonth, 0, 0, 0, 0); 
    }

    /**
     * Get a GregorianCalendar object (local time zone) for the specified time.
     * [Currently, it is lenient -- e.g., Dec 32 -> Jan 1 of the next year.]
     * Information can be retrieved via calendar.get(Calendar.XXXX),
     * where XXXX is one of the Calendar constants, like DAY_OF_YEAR.
     *
     * @param year  (e.g., 2005)
     * @param month (1..12)  (this is consciously different than Java's standard)
     * @param dayOfMonth (1..31)
     * @param hour (0..23)
     * @param minute (0..59)
     * @param second (0..59)
     * @param millis (0..999)
     * @return the corresponding GregorianCalendar object (local time zone)
     * @throws Exception if trouble (e.g., year is Integer.MAX_VALUE)
     */
    public static GregorianCalendar newGCalendarLocal(int year, int month, int dayOfMonth,
            int hour, int minute, int second, int millis) {

        if (year == Integer.MAX_VALUE)
            Test.error(String2.ERROR + " in newGCalendarLocal: year value is Integer.MAX_VALUE!");
        GregorianCalendar gc = 
            new GregorianCalendar(year, month - 1, dayOfMonth, hour, minute, second); 
        gc.add(MILLISECOND, millis);
        return gc; 
    }

    /**
     * Get a GregorianCalendar object (Zulu time zone) for the specified time.
     * [Currently, it is lenient -- e.g., Dec 32 -> Jan 1 of the next year.]
     * Information can be retrieved via calendar.get(Calendar.XXXX),
     * where XXXX is one of the Calendar constants, like DAY_OF_YEAR.
     *
     * @param year  (e.g., 2005)
     * @param month (1..12)  (this is consciously different than Java's standard)
     * @param dayOfMonth (1..31)
     * @param hour (0..23)
     * @param minute (0..59)
     * @param second (0..59)
     * @param millis (0..999)
     * @return the corresponding GregorianCalendar object (Zulu time zone)
     * @throws Exception if trouble (e.g., year is Integer.MAX_VALUE)
     */
    public static GregorianCalendar newGCalendarZulu(int year, int month, int dayOfMonth,
            int hour, int minute, int second, int millis) {

        if (year == Integer.MAX_VALUE)
            Test.error(String2.ERROR + " in newGCalendarZulu: year value is Integer.MAX_VALUE!");
        GregorianCalendar gc = new GregorianCalendar(zuluTimeZone);
        gc.clear();
        gc.set(year, month - 1, dayOfMonth, hour, minute, second); 
        gc.set(MILLISECOND, millis);
        gc.get(YEAR); //force recalculations
        return gc; 
    }

    /**
     * Get a GregorianCalendar object (local time zone) for the specified time.
     * [Currently, it is lenient -- e.g., day 366 -> Jan 1 of the next year.]
     * Information can be retrieved via calendar.get(Calendar.XXXX),
     * where XXXX is one of the Calendar constants, like DAY_OF_YEAR.
     *
     * @param year  (e.g., 2005)
     * @param dayOfYear (usually 1..365, but 1..366 in leap years)
     * @return the corresponding GregorianCalendar object (local time zone)
     * @throws Exception if trouble (e.g., year is Integer.MAX_VALUE)
     */
    public static GregorianCalendar newGCalendarLocal(int year, int dayOfYear) {
        if (year == Integer.MAX_VALUE)
            Test.error(String2.ERROR + " in newGCalendarLocal: year value is Integer.MAX_VALUE!");
        GregorianCalendar gc = new GregorianCalendar(year, 0, 1); 
        gc.set(Calendar.DAY_OF_YEAR, dayOfYear);
        gc.get(YEAR); //force recalculations
        return gc;
    }

    /**
     * Get a GregorianCalendar object (Zulu time zone) for the specified time.
     * [Currently, it is lenient -- e.g., day 366 -> Jan 1 of the next year.]
     * Information can be retrieved via calendar.get(Calendar.XXXX),
     * where XXXX is one of the Calendar constants, like DAY_OF_YEAR.
     *
     * @param year  (e.g., 2005)
     * @param dayOfYear (usually 1..365, but 1..366 in leap years)
     * @return the corresponding GregorianCalendar object (Zulu time zone)
     * @throws Exception if trouble (e.g., year is Integer.MAX_VALUE)
     */
    public static GregorianCalendar newGCalendarZulu(int year, int dayOfYear) {
        if (year == Integer.MAX_VALUE)
            Test.error(String2.ERROR + " in newGCalendarLocal: year value is Integer.MAX_VALUE!");
        GregorianCalendar gc = newGCalendarZulu(year, 1, 1); 
        gc.set(Calendar.DAY_OF_YEAR, dayOfYear);
        gc.get(YEAR); //force recalculations
        return gc;
    }

    /**
     * This returns the year.
     *   For years B.C., this returns Calendar2Year = 1 - BCYear.  
     *   Note that BCYears are 1..., so 1 BC is calendar2Year 0,
     *   and 2 BC is calendar2Year -1.
     * @param gc
     * @return the year (negative for BC).
     */
    public static int getYear(GregorianCalendar gc) {
        return gc.get(ERA) == BC? 1 - gc.get(YEAR) : gc.get(YEAR);
    }

    /**
     * This returns the year as YYYY.
     *   For years B.C., this returns Calendar2Year = 1 - BCYear.  
     *   Note that BCYears are 1..., so 1 BC is calendar2Year 0000,
     *   and 2 BC is calendar2Year -0001.
     * @param gc
     * @return the year as YYYY (or -YYYY for BC).
     */
    public static String formatAsISOYear(GregorianCalendar gc) {
        int year = getYear(gc);
        return (year < 0? "-" : "") + String2.zeroPad("" + Math.abs(year), 4);
    }

    /**
     * This returns a ISO-style formatted date string e.g., "2004-01-02"
     * using its current get() values (not influenced by the format's timeZone).
     *
     * @param gc a GregorianCalendar object
     * @return the date in gc, formatted as (for example) "2004-01-02"
     * @throws Exception if trouble (e.g., gc is null)
     */
    public static String formatAsISODate(GregorianCalendar gc) {
        
        return
            formatAsISOYear(gc) + "-" +
            String2.zeroPad("" + (gc.get(MONTH) + 1), 2) + "-" +
            String2.zeroPad("" + gc.get(DATE), 2);

        //this method is influenced by the format's timeZone
        //synchronized (isoDateFormat) {
        //    return isoDateFormat.format(gc.getTime());
        //}
    }

    /**
     * This converts a GregorianCalendar object into an
     * ISO-format dateTime string (with 'T' separator: [-]YYYY-MM-DDTHH:MM:SS)
     * using its current get() values (not influenced by the format's timeZone).
     * [was calendarToString]
     *
     * @param gc
     * @return the corresponding dateTime String (without the trailing Z).
     * @throws Exception if trouble (e.g., gc is null)
     */
    public static String formatAsISODateTimeT(GregorianCalendar gc) {
        return formatAsISODate(gc) + "T" + 
            String2.zeroPad("" + gc.get(HOUR_OF_DAY), 2) + ":" +
            String2.zeroPad("" + gc.get(MINUTE), 2) + ":" +
            String2.zeroPad("" + gc.get(SECOND), 2);

        //this method is influenced by the format's timeZone
        //synchronized (isoDateTimeFormat) {
        //    return isoDateTimeFormat.format(gc.getTime());
        //}
    }

    /**
     * Like formatAsISODateTimeT, but seconds will have 3 decimal digits.
     *
     * @param gc
     * @return the corresponding dateTime String (without the trailing Z).
     * @throws Exception if trouble (e.g., gc is null)
     */
    public static String formatAsISODateTimeT3(GregorianCalendar gc) {
        return formatAsISODate(gc) + "T" + 
            String2.zeroPad("" + gc.get(HOUR_OF_DAY), 2) + ":" +
            String2.zeroPad("" + gc.get(MINUTE), 2) + ":" +
            String2.zeroPad("" + gc.get(SECOND), 2) + "." +
            String2.zeroPad("" + gc.get(MILLISECOND), 3);
    }

   
    /**
     * This is like formatAsISODateTime, but returns a 
     * limited precision string.
     *
     * @param time_precision can be "1970", "1970-01", "1970-01-01", "1970-01-01T00Z",
     *    "1970-01-01T00:00Z", "1970-01-01T00:00:00Z" (used if time_precision not matched), 
     *    "1970-01-01T00:00:00.0Z", "1970-01-01T00:00:00.00Z", "1970-01-01T00:00:00.000Z".
     *    Versions without 'Z' are allowed.
     */
    public static String limitedFormatAsISODateTimeT(String time_precision,
        GregorianCalendar gc) {

        String zString = "";  
        if (time_precision == null || time_precision.length() == 0) 
            time_precision = "Z";
        if (time_precision.charAt(time_precision.length() - 1) == 'Z') {
            time_precision = time_precision.substring(0, time_precision.length() - 1);
            zString = "Z";
        }

        //build it    
        //Warning: year may be 5 chars, e.g., -0003
        StringBuilder sb = new StringBuilder(formatAsISOYear(gc)); 
        if (time_precision.equals("1970"))
            return sb.toString();

        sb.append("-" + String2.zeroPad("" + (gc.get(MONTH) + 1), 2));
        if (time_precision.equals("1970-01"))
            return sb.toString();
        
        sb.append("-" + String2.zeroPad("" + gc.get(DATE), 2));
        if (time_precision.equals("1970-01-01"))
            return sb.toString();

        sb.append("T" + String2.zeroPad("" + gc.get(HOUR_OF_DAY), 2));
        if (time_precision.equals("1970-01-01T00")) {
            sb.append(zString);
            return sb.toString();
        }
        
        sb.append(":" + String2.zeroPad("" + gc.get(MINUTE), 2));
        if (time_precision.equals("1970-01-01T00:00")) {
            sb.append(zString);
            return sb.toString();
        }
               
        sb.append(":" + String2.zeroPad("" + gc.get(SECOND), 2));
        if (time_precision.length() == 0 || //-> default
            time_precision.equals("1970-01-01T00:00:00")) {
            sb.append(zString);
            return sb.toString();
        }
        
        sb.append("." + String2.zeroPad("" + gc.get(MILLISECOND), 3));
        if (time_precision.equals("1970-01-01T00:00:00.0")) {
            sb.setLength(sb.length() - 2);
            sb.append(zString);
            return sb.toString();
        }
        if (time_precision.equals("1970-01-01T00:00:00.00")) {
            sb.setLength(sb.length() - 1);
            sb.append(zString);
            return sb.toString();
        }
        if (time_precision.equals("1970-01-01T00:00:00.000")) {
            sb.append(zString);
            return sb.toString();
        }

        //default
        sb.setLength(sb.length() - 4);
        sb.append('Z');  //default has Z
        return sb.toString();
    }


    /**
     * This converts a GregorianCalendar object into an ISO-format 
     * dateTime string (with space separator: [-]YYYY-MM-DD HH:MM:SS)
     * using its current get() values (not influenced by the format's timeZone).
     * [was calendarToString]
     *
     * @param gc
     * @return the corresponding dateTime String (without the trailing Z).
     * @throws Exception if trouble (e.g., gc is null)
     */
    public static String formatAsISODateTimeSpace(GregorianCalendar gc) {
        return formatAsISODate(gc) + " " + 
            String2.zeroPad("" + gc.get(HOUR_OF_DAY), 2) + ":" +
            String2.zeroPad("" + gc.get(MINUTE), 2) + ":" +
            String2.zeroPad("" + gc.get(SECOND), 2);

        //this method is influenced by the format's timeZone
        //synchronized (isoDateTimeFormat) {
        //    return isoDateTimeFormat.format(gc.getTime());
        //}
    }

    /**
     * This converts a GregorianCalendar object into an ESRI
     * dateTime string (YYYY/MM/DD HH:MM:SS UTC)
     * using its current get() values (not influenced by the format's timeZone).
     *
     * @param gc
     * @return the corresponding ESRI dateTime String.
     * @throws Exception if trouble (e.g., gc is null)
     */
    public static String formatAsEsri(GregorianCalendar gc) {
        return 
            formatAsISOYear(gc) + "/" +
            String2.zeroPad("" + (gc.get(MONTH) + 1), 2) + "/" +
            String2.zeroPad("" + gc.get(DATE), 2) + " " +
            String2.zeroPad("" + gc.get(HOUR_OF_DAY), 2) + ":" +
            String2.zeroPad("" + gc.get(MINUTE), 2) + ":" +
            String2.zeroPad("" + gc.get(SECOND), 2) + " UTC";
    }

    /**
     * This returns a compact formatted [-]YYYYMMDDHHMMSS string e.g., "20040102030405"
     * using its current get() values (not influenced by the format's timeZone).
     *
     * @param gc a GregorianCalendar object
     * @return the date in gc, formatted as (for example) "20040102030405".
     * @throws Exception if trouble (e.g., gc is null)
     */
    public static String formatAsCompactDateTime(GregorianCalendar gc) {
        return
            formatAsISOYear(gc) + 
            String2.zeroPad("" + (gc.get(MONTH) + 1), 2) +
            String2.zeroPad("" + gc.get(DATE), 2) + 
            String2.zeroPad("" + gc.get(HOUR_OF_DAY), 2) + 
            String2.zeroPad("" + gc.get(MINUTE), 2) +
            String2.zeroPad("" + gc.get(SECOND), 2);

        //this method is influenced by the format's timeZone
        //synchronized (CompactDateTimeFormat) {
        //    return CompactDateTimeFormat.format(gc.getTime());
        //}
    }

    /**
     * This returns a [-]YYYYDDD string e.g., "2004001"
     * using its current get() values (not influenced by the format's timeZone).
     *
     * @param gc a GregorianCalendar object
     * @return the date in gc, formatted as (for example) "2004001".
     * @throws Exception if trouble (e.g., gc is null)
     */
    public static String formatAsYYYYDDD(GregorianCalendar gc) {
        return
            formatAsISOYear(gc) + 
            String2.zeroPad("" + gc.get(DAY_OF_YEAR), 3); 

        //this method is influenced by the format's timeZone
        //synchronized (YYYYDDDFormat) {
        //    return YYYYDDDFormat.format(gc.getTime());
        //}
    }

    /**
     * This returns a [-]YYYYMM string e.g., "200401"
     * using its current get() values (not influenced by the format's timeZone).
     *
     * @param gc a GregorianCalendar object
     * @return the date in gc, formatted as (for example) "200401".
     * @throws Exception if trouble (e.g., gc is null)
     */
    public static String formatAsYYYYMM(GregorianCalendar gc) {
        return
            formatAsISOYear(gc) + 
            String2.zeroPad("" + (gc.get(MONTH) + 1), 2);
        
        //this method is influenced by the format's timeZone
        //synchronized (YYYYMMFormat) {
        //    return YYYYMMFormat.format(gc.getTime());
        //}
    }

    /**
     * This returns a DD-Mon-[-]YYYY string e.g., "31-Jul-2004 00:00:00"
     * using its current get() values (not influenced by the format's timeZone).
     * Ferret often uses this format.
     *
     * @param gc a GregorianCalendar object
     * @return the date in gc, formatted as (for example) "31-Jul-2004 00:00:00".
     * @throws Exception if trouble (e.g., gc is null)
     */
    public static String formatAsDDMonYYYY(GregorianCalendar gc) {
        return
            String2.zeroPad("" + gc.get(DATE), 2) + "-" +
            MONTH_3[gc.get(MONTH)] + "-" +  //0 based
            formatAsISOYear(gc) + " " +
            String2.zeroPad("" + gc.get(HOUR_OF_DAY), 2) + ":" +
            String2.zeroPad("" + gc.get(MINUTE), 2) + ":" +
            String2.zeroPad("" + gc.get(SECOND), 2);
        
        //this method is influenced by the format's timeZone
        //synchronized (YYYYMMFormat) {
        //    return YYYYMMFormat.format(gc.getTime());
        //}
    }

    /**
     * This returns a US-style slash format date time string 
     * ("1/20/2006 9:00:00 pm").
     *
     * @param gc a GregorianCalendar object. The dateTime will be interpreted
     *   as being in gc's time zone.
     * @return gc in the US slash format ("1/20/2006 9:00:00 pm").
     * @throws Exception if trouble (e.g., gc is null)
     */
    public static String formatAsUSSlashAmPm(GregorianCalendar gc) {
        int hour = gc.get(HOUR); //0..11
        return 
            (gc.get(MONTH) + 1) + "/" +
            gc.get(DATE) + "/" +
            formatAsISOYear(gc) + " " +
            (hour == 0? 12 : hour) + ":" +
            String2.zeroPad("" + gc.get(MINUTE), 2) + ":" +
            String2.zeroPad("" + gc.get(SECOND), 2) + " " +
            (gc.get(AM_PM) == Calendar.AM? "am" : "pm");
    }

    /**
     * This returns an RFC 822 format date time string 
     * ("Sun, 06 Nov 1994 08:49:37 GMT").
     *
     * @param gc a GregorianCalendar object. The dateTime will be interpreted
     *   as being in the gc's time zone (which should always be GMT because "GMT" is put at the end).
     * @return gc in the RFC 822 format ("Sun, 06 Nov 1994 08:49:37 GMT").
     * @throws Exception if trouble (e.g., gc is null)
     */
    public static String formatAsRFC822GMT(GregorianCalendar gc) {
        return 
            DAY_OF_WEEK_3[gc.get(Calendar.DAY_OF_WEEK)] + ", " +
            String2.zeroPad("" + gc.get(DATE), 2) + " " +
            MONTH_3[gc.get(MONTH)] + " " +  //0 based
            formatAsISOYear(gc) + " " +
            String2.zeroPad("" + gc.get(HOUR_OF_DAY), 2) + ":" +
            String2.zeroPad("" + gc.get(MINUTE), 2) + ":" +
            String2.zeroPad("" + gc.get(SECOND), 2) + " GMT"; //not UTC or Z
    }

    /**
     * This returns a US-style slash format date 24-hour time string 
     * ("1/20/2006 21:00:00") (commonly used by Microsoft Access).
     *
     * @param gc a GregorianCalendar object. The dateTime will be interpreted
     *   as being in gc's time zone.
     * @return gc in the US slash date 24 hour format ("1/20/2006 21:00:00").
     * @throws Exception if trouble (e.g., gc is null)
     */
    public static String formatAsUSSlash24(GregorianCalendar gc) {
        return 
            (gc.get(MONTH) + 1) + "/" +
            gc.get(DATE) + "/" +
            formatAsISOYear(gc) + " " +
            String2.zeroPad("" + gc.get(HOUR_OF_DAY), 2) + ":" +
            String2.zeroPad("" + gc.get(MINUTE), 2) + ":" +
            String2.zeroPad("" + gc.get(SECOND), 2);
    }


    /** This parses n int values from s and stores results in resultsN (or leaves
     * items in resultsN untouched if no value available).
     * 
     * @param s the date time string
     * @param separatorN is the separators (use "\u0000" to match any non-digit).
     *    (± matches + or - and that becomes part of the number)
     *    (. matches . or , (the European decimal point))
     * @param resultsN should initially have the defaults and 
     *   will receive the results.  If trouble, resultsN[0] will be Integer.MAX_VALUE,
     *   so caller can throw exception with good error message.
     */
    private static void parseN(String s, char separatorN[], int resultsN[]) {
        //ensure s starts with a digit
        if (s == null)
            s = "";
        s = s.trim();
        int sLength = s.length();
        if (sLength < 1 || !(s.charAt(0) == '-' || String2.isDigit(s.charAt(0)))) {
            resultsN[0] = Integer.MAX_VALUE;
            return;  
        }
        int po1, po2 = -1;
        //String2.log("parseN " + s);

        //search for digits, non-digit.   "1970-01-01T00:00:00.000-01:00"
        boolean mMode = s.charAt(0) == '-'; //initial '-' is required and included when evaluating number
        int nParts = separatorN.length;
        for (int part = 0; part < nParts; part++) {
            if (po2 + 1 < sLength) {
                //accumulate digits
                po1 = po2 + 1;
                po2 = po1;
                if (mMode) {
                    if (po2 < sLength && s.charAt(po2) == '-') po2++; 
                    else {resultsN[0] = Integer.MAX_VALUE; return; }
                }
                while (po2 < sLength && String2.isDigit(s.charAt(po2))) po2++; //digit

                //if no number, return; we're done
                if (po2 == po1)
                    return;
                if (part > 0 && separatorN[part - 1] == '.') {
                    resultsN[part] = Math2.roundToInt(1000 * 
                        String2.parseDouble("0." + s.substring(po1, po2)));
                    //String2.log("  millis=" + resultsN[part]);
                } else {
                    resultsN[part] = String2.parseInt(s.substring(po1, po2));
                }


                //if invalid number, return trouble
                if (resultsN[part] == Integer.MAX_VALUE) {
                    resultsN[0] = Integer.MAX_VALUE;
                    return; 
                }

                //if no more source characters, we're done
                if (po2 >= sLength) {
                    //String2.log("  " + String2.toCSSVString(resultsN));
                    return;
                }

                //if invalid separator, stop trying to read more; return trouble
                mMode = false;
                char ch = s.charAt(po2);
                if (ch == ',') 
                    ch = '.';
                if (separatorN[part] == '\u0000') {

                } else if (separatorN[part] == '±') {
                    if (ch == '+') { //do nothing
                    }else if (ch == '-') {
                        po2--; //number starts with -
                        mMode = true;
                    } else {
                        resultsN[0] = Integer.MAX_VALUE; return; 
                    }

                } else if (ch != separatorN[part]) { //if not exact match ...

                    //if current part is ':' or '.' and not matched, try to skip forward to '±'
                    if ((separatorN[part] == ':' || separatorN[part] == '.') && 
                        part < nParts - 1) {
                        int pmPart = String2.indexOf(separatorN, '±', part + 1);
                        if (pmPart >= 0) {
                            //String2.log("  jump to +/-");
                            part = pmPart; 
                            if (ch == '+') { //do nothing
                            }else if (ch == '-') {
                                po2--; //number starts with -
                                mMode = true;
                            } else {
                                resultsN[0] = Integer.MAX_VALUE; return; 
                            }
                            continue;
                        } //if < 0, fall through to failure
                    }
                    resultsN[0] = Integer.MAX_VALUE;
                    //String2.log("  " + String2.toCSSVString(resultsN));
                    return;  
                }
            }
        }
        //String2.log("  " + String2.toCSSVString(resultsN));
    }

    /**
     * This tests if s is probably an ISO 8601 Date Time (at least [-]YYYY-M).
     * null and "" return false;
     * This isn't strict since it doesn't test the remainder of the string.
     */
    public static boolean probablyISODateTime(String s) {
        char ch;
        if (s == null)
            return false;
        int sLength = s.length();
        if (sLength < 6)
            return false;
        int po = 0;
        if (s.charAt(po) == '-') {
            po++;
            if (sLength < 7)
                return false;
        }
        if (!String2.isDigit(s.charAt(po++))) return false;
        if (!String2.isDigit(s.charAt(po++))) return false;
        if (!String2.isDigit(s.charAt(po++))) return false;
        if (!String2.isDigit(s.charAt(po++))) return false;
        if (s.charAt(po++) != '-')    return false;
        if (!String2.isDigit(s.charAt(po++))) return false; //perhaps not 0-padded
        return true;
    }

    /**
     * This converts an ISO date time string ([-]YYYY-MM-DDTHH:MM:SS.SSS±ZZ:ZZ) into
     *   a GregorianCalendar object.
     * <br>It is lenient; so Jan 32 is converted to Feb 1;
     * <br>The 'T' may be any non-digit.
     * <br>The time zone can be omitted.
     * <br>The parts at the end of the time can be omitted.
     * <br>If there is no time, the end parts of the date can be omitted.  Year is required.
     * <br>This tries hard to be tolerant of non-valid formats (e.g., "1971-1-2", "1971-01")
     * <br>As of 11/9/2006, NO LONGER TRUE: If year is 0..49, it is assumed to be 2000..2049.
     * <br>As of 11/9/2006, NO LONGER TRUE: If year is 50..99, it is assumed to be 1950..1999.
     * <br>If the string is too short, the end of "1970-01-01T00:00:00.000Z" will be added (effectively).
     * <br>If the string is too long, the excess will be ignored.
     * <br>If a required separator is incorrect, it is an error.
     * <br>If the date is improperly formatted, it returns null.
     * <br>Timezone "Z" or "" is treated as "-00:00" (UTC/Zulu time)
     * <br>Timezones: e.g., 2007-01-02T03:04:05-01:00 is same as 2007-01-02T04:04:05
     *
     * @param gc a GregorianCalendar object. The dateTime will be interpreted
     *   as being in gc's time zone.
     *   Timezone info is relative to the gc's time zone.
     * @param s the dateTimeString in the ISO format (YYYY-MM-DDTHH:MM:SS.SSS±ZZ:ZZ
     *   or -YYYY-MM-DDTHH:MM:SS.SSS±ZZ:ZZ for years B.C.)
     *   For years B.C., use calendar2Year = 1 - BCYear.  
     *   Note that BCYears are 1..., so 1 BC is calendar2Year 0 (or 0000),
     *   and 2 BC is calendar2Year -1 (or -0001).
     *   This supports SS.SSS and SS,SSS (which ISO 8601 prefers!).
     * @return the same GregorianCalendar object, but with the date info
     * @throws RuntimeException if trouble (e.g., gc is null or s is null or 
     *    not at least #)
     */
    public static GregorianCalendar parseISODateTime(GregorianCalendar gc, 
        String s) {

        if (s == null)
            s = "";
        boolean negative = s.startsWith("-");
        if (negative) 
            s = s.substring(1);
        if (s.length() < 1 || !String2.isDigit(s.charAt(0))) 
            Test.error(String2.ERROR + " in parseISODateTime: for first character of dateTime='" + s + "' isn't a digit!");
        if (gc == null) 
            Test.error(String2.ERROR + " in parseISODateTime: gc is null!");

        //default ymdhmsmom     year is the only required value
        int ymdhmsmom[] = {Integer.MAX_VALUE, 1, 1, 0, 0, 0, 0, 0, 0};


        //remove trailing Z or "UTC"
        s = s.trim();
        if (Character.toLowerCase(s.charAt(s.length() - 1)) == 'z') 
            s = s.substring(0, s.length() - 1).trim();
        if (s.length() >= 3) {
            String last3 = s.substring(s.length() - 3).toLowerCase();
            if (last3.equals("utc") || last3.equals("gmt"))
                s = s.substring(0, s.length() - 3).trim();
        }

        //if e.g., 1970-01-01 00:00:00 0:00, change ' ' to '+' (first ' '->'+' is irrelevant)
        s = String2.replaceAll(s, ' ', '+');

        //separators (\u0000=any non-digit)
        char separator[] = {'-','-','\u0000',':',':','.','±', ':', '\u0000'};
        parseN(s, separator, ymdhmsmom);
        if (ymdhmsmom[0] == Integer.MAX_VALUE) 
            Test.error(String2.ERROR + " in parseISODateTime: dateTime='" + s + "' has an invalid format!");

        //do time zone adjustment
        //String2.log("#7=" + ymdhmsmom[7] + " #8=" + ymdhmsmom[8]);
        if (ymdhmsmom[7] != 0)
            ymdhmsmom[3] -= ymdhmsmom[7];
        if (ymdhmsmom[8] != 0) 
            ymdhmsmom[4] -= ymdhmsmom[7] < 0? -ymdhmsmom[8] : ymdhmsmom[8];

        //set gc      month -1 since gc month is 0..
        gc.set((negative? -1 : 1) * ymdhmsmom[0], ymdhmsmom[1] - 1, ymdhmsmom[2], 
            ymdhmsmom[3], ymdhmsmom[4], ymdhmsmom[5]);
        gc.set(MILLISECOND, ymdhmsmom[6]);
        gc.get(YEAR); //force recalculations

        //synchronized (isoDateTimeFormat) {
        //    gc.setTime(isoDateTimeFormat.parse(isoDateTimeString));
        //}
        //String2.log("  " + gc.getTimeInMillis() + " = " + formatAsISODateTimeT3(gc));
        return gc;
    }

    /**
     * This converts an ISO (default *ZULU* time zone) date time string ([-]YYYY-MM-DDTHH:MM:SS±ZZ:ZZ) into
     * a GregorianCalendar object with the Zulu time zone.
     * See parseISODateTime documentation.
     *
     * @param s the dateTimeString in the ISO format ([-]YYYY-MM-DDTHH:MM:SS)
     *   This may include hours, minutes, seconds, decimal, and Z or timezone offset (default=Zulu).  
     * @return a GregorianCalendar object
     * @throws Exception if trouble (e.g., s is null or not at least #)
     */
    public static GregorianCalendar parseISODateTimeZulu(String s) {
        return parseISODateTime(newGCalendarZulu(), s);
    }

    /**
     * This converts a US slash 24 hour string ("1/20/2006" or "1/20/2006 14:23:59") 
     * (commonly used by Microsoft Access) into a GregorianCalendar object.
     * <br>It is lenient; so Jan 32 is converted to Feb 1.
     * <br>If year is 0..49, it is assumed to be 2000..2049.
     * <br>If year is 50..99, it is assumed to be 1950..1999.
     * <br>The year may be negative (calendar2Year = 1 - BCYear).  (But 0 - 24 assumed to be 2000 - 2049!)
     * <br>There must be at least #/#/#, or this returns null.
     * <br>The time is optional; if absent, it is assumed to be 00:00:00
     *
     * @param gc a GregorianCalendar object. The dateTime will be interpreted
     *   as being in gc's time zone.
     * @param s the dateString in the US slash format ("1/20/2006" or 
     *    "1/20/2006 14:23:59")
     * @return the same GregorianCalendar object, but with the date info
     * @throws Exception if trouble (e.g., gc is null or s is null or not at least #/#/#)
     */
    public static GregorianCalendar parseUSSlash24(GregorianCalendar gc, 
        String s) {

        //default mdyhms     month is the only required value
        int mdyhms[] = {Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE, 0, 0, 0};

        //separators (\u0000=any non-digit)
        char separator[] = {'/','/',' ',':',':','\u0000'}; 

        parseN(s, separator, mdyhms);
        if (mdyhms[0] == Integer.MAX_VALUE ||
            mdyhms[1] == Integer.MAX_VALUE ||
            mdyhms[2] == Integer.MAX_VALUE) {
            Test.error(String2.ERROR + " in parseUSSlash24: s=" + s + " has an invalid format!");
        }

        //clean up year
        if (mdyhms[2] >=  0 && mdyhms[2] <= 49) mdyhms[2] += 2000;
        if (mdyhms[2] >= 50 && mdyhms[2] <= 99) mdyhms[2] += 1900;

        //set as ymdhms      month -1 since gc month is 0..
        gc.set(mdyhms[2], mdyhms[0] - 1, mdyhms[1], mdyhms[3], mdyhms[4], mdyhms[5]);
        gc.set(MILLISECOND, 0);
        gc.get(YEAR); //force recalculations

        //synchronized (isoDateTimeFormat) {
        //    gc.setTime(isoDateTimeFormat.parse(isoDateTimeString));
        //}
        return gc;
    }

    /**
     * This is like parseUSSlash24, but assumes the time zone is Zulu.
     *
     * @throws Exception if trouble (e.g., s is null or not at least #/#/#)
     */
    public static GregorianCalendar parseUSSlash24Zulu(String s) {
        return parseUSSlash24(newGCalendarZulu(), s);
    }

    /**
     * This converts compact string (must be [-]YYYYMMDD, [-]YYYYMMDDhh,
     * [-]YYYYMMDDhhmm, or [-]YYYYMMDDhhmmss) into
     * a GregorianCalendar object.
     * It is lenient; so Jan 32 is converted to Feb 1.
     * If the date is improperly formatted, it returns null.
     *
     * @param gc a GregorianCalendar object. The dateTime will be interpreted
     *   as being in gc's time zone.
     * @param s dateTimeString in compact format (must be [-]YYYYMMDD, [-]YYYYMMDDhh,
     *   [-]YYYYMMDDhhmm, or [-]YYYYMMDDhhmmss)
     * @return the same GregorianCalendar object, but with the date info
     * @throws Exception if trouble (e.g., gc is null or s is null or not at least
     *    YYYYMMDD)
     */
    public static GregorianCalendar parseCompactDateTime(GregorianCalendar gc, 
        String s) {

        //ensure it has at least 8 characters, and all characters are digits
        if (s == null) s = "";
        boolean negative = s.startsWith("-");
        if (negative) 
            s = s.substring(1);
        int sLength = s.length();
        if (sLength < 8)
            Test.error(String2.ERROR + " in parseCompactDateTime: s=" + s + " has an invalid format!");
        for (int i = 0; i < sLength; i++)
            if (!String2.isDigit(s.charAt(i)))
                Test.error(String2.ERROR + " in parseCompactDateTime: s=" + s + " has an invalid format!");

        s += String2.makeString('0', 14 - sLength); 
        gc.clear();
        gc.set(
            (negative? -1 : 1) * String2.parseInt(s.substring(0, 4)),
            String2.parseInt(s.substring(4, 6)) - 1, //-1 = month is 0..
            String2.parseInt(s.substring(6, 8)),
            String2.parseInt(s.substring(8, 10)),
            String2.parseInt(s.substring(10, 12)),
            String2.parseInt(s.substring(12, 14)));
        gc.set(MILLISECOND, 0);
        gc.get(YEAR); //force recalculations

        //synchronized (CompactDateTimeFormat) {
        //    gc.setTime(CompactDateTimeFormat.parse(s));
        //}
        //String2.log("parseCompactDateTime " + s + " -> " + formatAsISODateTimeT(gc));        
        return gc;
    }

    /**
     * This is like parseCompactDateTime, but assumes the time zone is Zulu.
     *
     * @throws Exception if trouble (e.g., s is null or invalid)
     */
    public static GregorianCalendar parseCompactDateTimeZulu(String s) {
        return parseCompactDateTime(newGCalendarZulu(), s);
    }

    /**
     * This converts a DD-Mon-[-]YYYY string e.g., "31-Jul-2004 00:00:00"
     * into a GregorianCalendar object. 
     * It is lenient; so day 0 is converted to Dec 31 of previous year.
     * If the date is shortenend, this does the best it can, or returns null.
     * Ferret often uses this format.
     *
     * @param gc a GregorianCalendar object. The dateTime will be interpreted
     *   as being in gc's time zone.
     * @param s dateTimeString in YYYYDDD format
     * @return the same GregorianCalendar object, but with the date info
     * @throws Exception if trouble (e.g., gc is null or s is null or not
     *    DDMonYYYY)
     */
    public static GregorianCalendar parseDDMonYYYY(GregorianCalendar gc, String s) {

        if (s == null)
            s = "";
        int sLength = s.length();
        boolean negative = sLength >= 8 && s.charAt(7) == '-';
        if (negative)
            s = s.substring(0, 7) + s.substring(8);
        if (sLength < 11 ||
            !String2.isDigit(s.charAt(0)) ||
            !String2.isDigit(s.charAt(1)) ||
            s.charAt(2) != '-' ||
            s.charAt(6) != '-' ||
            !String2.isDigit(s.charAt(7)) ||
            !String2.isDigit(s.charAt(8)) ||
            !String2.isDigit(s.charAt(9)) ||
            !String2.isDigit(s.charAt(10)))
            Test.error(String2.ERROR + " in parseDDMonYYYY: s=" + s + " has an invalid format!");

        gc.clear();
        int hour = 0, min = 0, sec = 0;
        if (sLength >= 20) {
            if (s.charAt(11) != ' ' ||
                !String2.isDigit(s.charAt(12)) ||
                !String2.isDigit(s.charAt(13)) ||
                s.charAt(14) != ':' ||
                !String2.isDigit(s.charAt(15)) ||
                !String2.isDigit(s.charAt(16)) ||
                s.charAt(17) != ':')
                Test.error(String2.ERROR + " in parseDDMonYYYY: s=" + s + " has an invalid format!");
            hour = String2.parseInt(s.substring(12, 14));
            min  = String2.parseInt(s.substring(15, 17));
            sec  = String2.parseInt(s.substring(18, 20));
        }

        String month = s.substring(3, 6).toLowerCase();
        int mon = 0;
        while (mon < 12) {
            if (MONTH_3[mon].toLowerCase().equals(month))
                break;
            mon++;
        }
        if (mon == 12) Test.error(String2.ERROR + " in parseDDMonYYYY: s=" + s + " has an invalid format!");

        gc.set(
            (negative? -1 : 1) * String2.parseInt(s.substring(7, 11)),
            mon, //month is already 0..
            String2.parseInt(s.substring(0, 2)), 
            hour, min, sec);

        gc.get(YEAR); //force recalculations

        return gc;       
    }

    /**
     * This is like parseDDMonYYYY, but assumes the time zone is Zulu.
     *
     * @throws Exception if trouble (e.g., s is null or invalid)
     */
    public static GregorianCalendar parseDDMonYYYYZulu(String s) {
        return parseDDMonYYYY(newGCalendarZulu(), s);
    }

    /**
     * This converts a [-]YYYYDDD string into
     * a GregorianCalendar object.
     * It is lenient; so day 0 is converted to Dec 31 of previous year.
     * If the date is improperly formatted, this does the best
     * it can, or returns null.
     *
     * @param gc a GregorianCalendar object. The dateTime will be interpreted
     *   as being in gc's time zone.
     * @param s dateTimeString in YYYYDDD format
     * @return the same GregorianCalendar object, but with the date info
     * @throws Exception if trouble (e.g., gc is null or s is null or not
     *    YYYYDDDD)
     */
    public static GregorianCalendar parseYYYYDDD(GregorianCalendar gc, 
        String s) {

        //ensure it is a string with 7 digits
        if (s == null)
            s = "";
        boolean negative = s.startsWith("-");
        if (negative) 
            s = s.substring(1);
        int sLength = s.length();
        if (sLength != 7)
            Test.error(String2.ERROR + " in parseYYYYDDD: s=" + s + " has an invalid format!");
        for (int i = 0; i < sLength; i++)
            if (!String2.isDigit(s.charAt(i)))
                Test.error(String2.ERROR + " in parseYYYYDDD: s=" + s + " has an invalid format!");

        gc.clear();
        gc.set(
            (negative? -1 : 1) * String2.parseInt(s.substring(0, 4)),
            1 - 1, //-1 = month is 0..
            1, 0, 0, 0);
        gc.set(Calendar.DAY_OF_YEAR, String2.parseInt(s.substring(4, 7)));
        gc.set(MILLISECOND, 0);
        gc.get(YEAR); //force recalculations

        //synchronized (YYYYDDDFormat) {
        //    gc.setTime(YYYYDDDFormat.parse(YYYYDDDString));
        //}
        //String2.log("parseYYYYDDD " + s + " -> " + formatAsISODate(gc));
        return gc;
    }

    /**
     * This is like parseYYYYDDD, but assumes the time zone is Zulu.
     * @throws Exception if trouble (e.g., s is null or not YYYYDDD)
     */
    public static GregorianCalendar parseYYYYDDDZulu(String s) {
        return parseYYYYDDD(newGCalendarZulu(), s);
    }

    /**
     * This returns an error message 
     * indicating that the specified isoDateString couldn't be parsed.
     *
     * @param s dateTimeString 
     * @param e a Exception
     * @return an error string
     */
    public static String getParseErrorString(String s, Exception e) {
        String error = MustBe.throwable(
            String2.ERROR + " while parsing \"" + s + "\".", e);
        //String2.log(error);
        return error;
    }


    /**
     * Convert a String with [-]yyyyddd to a String with YYYY-mm-dd.
     * This works the same for Local or Zulu or other time zones.
     * 
     * @param s a String with a date in the form yyyyddd
     * @return the date formatted as YYYY-mm-dd 
     * @throws Exception if trouble (e.g., s is null or not YYYYDDD)
     */
    public static String yyyydddToIsoDate(String s) {
        //ensure it is a string with 7 digits
        if (s == null)
            s = "";
        boolean negative = s.startsWith("-");
        if (negative) 
            s = s.substring(1);
        int sLength = s.length();
        if (sLength != 7)
            Test.error(String2.ERROR + " in yyyydddToIsoDate: yyyyddd='" + s + "' has an invalid format!");
        for (int i = 0; i < sLength; i++)
            if (!String2.isDigit(s.charAt(i)))
                Test.error(String2.ERROR + " in yyyydddToIsoDate: yyyyddd='" + s + "' has an invalid format!");

        GregorianCalendar gc = newGCalendarZulu(
            (negative? -1 : 1) * Integer.parseInt(s.substring(0, 4)),
            Integer.parseInt(s.substring(4)));
        return formatAsISODate(gc);
    }

    /**
     * This returns the current local dateTime in ISO T format.
     *
     * @return the current local dateTime in ISO T format (without the trailing Z)
     */
    public static String getCurrentISODateTimeStringLocal() {
        return formatAsISODateTimeT(newGCalendarLocal());
    }

    /**
     * This returns the current local dateTime in compact ISO format (yyyyMMddhhmmss).
     *
     * @return the current local dateTime in compact ISO format (yyyyMMddhhmmss).
     */
    public static String getCompactCurrentISODateTimeStringLocal() {
        return formatAsCompactDateTime(newGCalendarLocal());
    }

    /**
     * This returns the current Zulu dateTime in ISO T format.
     *
     * @return the current Zulu dateTime in ISO T format (without the trailing Z)
     */
    public static String getCurrentISODateTimeStringZulu() {
        return formatAsISODateTimeT(newGCalendarZulu());
    }

    /**
     * This returns the current Zulu date in RFC 822 format.
     *
     * @return the current Zulu date in RFC 822 format
     */
    public static String getCurrentRFC822Zulu() {
        return formatAsRFC822GMT(newGCalendarZulu());
    }

    /**
     * This returns the current Zulu date in ISO format.
     *
     * @return the current Zulu date in ISO format
     */
    public static String getCurrentISODateStringZulu() {
        return formatAsISODate(newGCalendarZulu());
    }

    /**
     * This returns the current local date in ISO format.
     *
     * @return the current local date in ISO format
     */
    public static String getCurrentISODateStringLocal() {
        return formatAsISODate(newGCalendarLocal());
    }
     /**
     * This converts an ISO Zulu DateTime string to millis since 1970-01-01T00:00:00Z.
     *
     * @param s the ISO Zulu DateTime string.
     *   This may include hours, minutes, seconds, millis and Z or timezone offset (default=Zulu).  
     * @return the millis since 1970-01-01T00:00:00Z 
     * @throws Exception if trouble (e.g., s is null or not at least #)
     */
    public static long isoZuluStringToMillis(String s) {
        GregorianCalendar gc = parseISODateTime(newGCalendarZulu(), s);
        return gc.getTimeInMillis();
    }

    /**
     * This converts millis since 1970-01-01T00:00:00Z to an ISO Zulu DateTime string.
     *
     * @param millis the millis since 1970-01-01T00:00:00Z
     * @return the ISO Zulu DateTime string 'T' (without the trailing Z)
     * @throws Exception if trouble (e.g., millis is Long.MAX_VALUE)
     */
    public static String millisToIsoZuluString(long millis) {
        GregorianCalendar gc = newGCalendarZulu(millis); 
        return formatAsISODateTimeT(gc);
    }

    /**
     * This converts millis since 1970-01-01T00:00:00Z to an ISO Zulu DateTime string.
     *
     * @param millis the millis since 1970-01-01T00:00:00Z
     * @return the ISO Zulu DateTime string 'T' (with 3 decimal places) (without the trailing Z)
     * @throws Exception if trouble (e.g., millis is Long.MAX_VALUE)
     */
    public static String millisToIso3ZuluString(long millis) {
        GregorianCalendar gc = newGCalendarZulu(millis); 
        return formatAsISODateTimeT3(gc);
    }

    /**
     * Remove any spaces, dashes (except optional initial dash), colons, and T's from s.
     *
     * @param s a string
     * @return s with any spaces, dashes, colons removed
     *    (if s == null, this throws Exception)
     * @throws Exception if trouble (e.g., s is null)
     */
    public static String removeSpacesDashesColons(String s) {
        boolean negative = s.startsWith("-");
        if (negative) 
            s = s.substring(1);        
        s = String2.replaceAll(s, " ", ""); 
        s = String2.replaceAll(s, "-", "");
        s = String2.replaceAll(s, "T", "");
        return (negative? "-" : "") + String2.replaceAll(s, ":", ""); 
    }

    /**
     * Find the closest match for timeValue in isoDates
     * which must be sorted in ascending order.
     * This gives precise answer if there is an exact match
     * (and gives closest answer timeValue is imprecise, e.g., if "2006-01-07" is used
     * to represent a precise time of "2006-01-07 12:00:00").
     *
     * <p>This throws RuntimeException if some years are negative (0000 is ok).
     *
     * @param isoDates is an ascending sorted list of ISO dates [times].
     *   It the array has duplicates and timeValue equals one of them,
     *   it isn't specified which duplicate's index will be returned.  
     * @param timeValue the ISO timeValue to be matched
     *    (with connector "T" or " " matching the isoDates).
     *   This may include hours, minutes, seconds, decimal, and timezone offset (default=Zulu).  
     * @return the index (in isoDates) of the best match for timeValue.
     *   If timeValue is null or "", this returns isoDates.length-1.
     */
    public static int binaryFindClosest(String isoDates[], String timeValue) {
        try {       
            if (isoDates[0].startsWith("-"))
                throw new RuntimeException(String2.ERROR + 
                    ": Calendar2.binaryFindClosest doesn't work with years < 0.");

            //likely place for exception thrown (that's ok)
            double timeValueSeconds = isoStringToEpochSeconds(timeValue);

            //do standard String binary search
            //(since isoDate strings work with standard String ordering)
            int i = Arrays.binarySearch(isoDates, timeValue);
            if (i >= 0)
                return i; //success

            //insertionPoint at end point?
            int insertionPoint = -i - 1;  //0.. isoDates.length
            if (insertionPoint == 0) 
                return 0;
            if (insertionPoint >= isoDates.length)
                return insertionPoint - 1;

            //insertionPoint between 2 points 
            //tie? favor later time so "2006-01-07" finds "2006-01-07 12:00:00",
            //   not "2006-01-06 12:00:00"
            if (Math.abs(isoStringToEpochSeconds(isoDates[insertionPoint - 1]) - timeValueSeconds) <
                Math.abs(isoStringToEpochSeconds(isoDates[insertionPoint]) - timeValueSeconds))
                 return insertionPoint - 1;
            else return insertionPoint;
        } catch (Exception e) {
            return isoDates.length - 1;
        }
    }

    /**
     * Find the last element which is <= timeValue in isoDates (sorted ascending).
     *
     * <p>If firstGE &gt; lastLE, there are no matching elements (because
     * the requested range is less than or greater than all the values,
     * or between two adjacent values).
     *
     * <p>This throws RuntimeException if some years are negative (0000 is ok).
     *
     * @param isoDates is an ascending sorted list of ISO dates [times]  
     *   which may have duplicates
     * @param timeValue an iso formatted date value
     *    (with connector "T" or " " matching the isoDates).
     *   This may include hours, minutes, seconds, decimal, and timezone offset (default=Zulu).  
     * @return the index of the last element which is <= timeValue in an ascending sorted array.
     *   If timeValue is invalid or timeValue < the smallest element, this returns -1  (no element is appropriate).
     *   If timeValue > the largest element, this returns isoDates.length-1.
     */
    public static int binaryFindLastLE(String[] isoDates, String timeValue) {
        try {
            if (isoDates[0].startsWith("-"))
                throw new RuntimeException(String2.ERROR + 
                    ": Calendar2.binaryFindLastLE doesn't work with years < 0.");

            //likely place for exception thrown (that's ok)
            double timeValueSeconds = isoStringToEpochSeconds(timeValue);

            int i = Arrays.binarySearch(isoDates, timeValue);
            //String2.log("binaryLE: i=" + i);

            //if (i >= 0) an exact match; look for duplicates
            if (i < 0) {
                int insertionPoint = -i - 1;  //0.. isoDates.length
                i = insertionPoint - 1;   
            }

            while (i < isoDates.length - 1 && 
                   isoStringToEpochSeconds(isoDates[i + 1]) <= timeValueSeconds) {
                //String2.log("binaryLE: i++ because " + isoStringToEpochSeconds(isoDates[i + 1]) + " <= " + timeValueSeconds);
                i++;
            }
            return i; 
        } catch (Exception e) {
            return -1;
        }

    }

    /**
     * Find the first element which is >= timeValue in isoDates (sorted ascending.
     *
     * <p>If firstGE &gt; lastLE, there are no matching elements (because
     * the requested range is less than or greater than all the values,
     * or between two adjacent values).
     *
     * <p>This throws RuntimeException if some years are negative (0000 is ok).
     *
     * @param isoDates is a sorted list of ISO dates [times]  
     *    which may have duplicates
     * @param timeValue an iso formatted date value
     *    (with connector "T" or " " matching the isoDates).
     *   This may include hours, minutes, seconds, decimal, and timezone offset (default=Zulu).  
     * @return the index of the first element which is >= timeValue in an ascending sorted array.
     *   <br>If timeValue < the smallest element, this returns 0.
     *   <br>If timeValue is invalid or timeValue > the largest element, 
     *     this returns isoDates.length (no element is appropriate).
     */
    public static int binaryFindFirstGE(String[] isoDates, String timeValue) {
        try {        
            if (isoDates[0].startsWith("-"))
                throw new RuntimeException(String2.ERROR + 
                    ": Calendar2.binaryFindFirstGE doesn't work with years < 0.");

            //likely place for exception thrown (that's ok)
            double timeValueSeconds = isoStringToEpochSeconds(timeValue);

            int i = Arrays.binarySearch(isoDates, timeValue);
          
            //if (i >= 0) an exact match; look for duplicates
            if (i < 0) 
                i = -i - 1;  //the insertion point,  0.. isoDates.length

            while (i > 0 && isoStringToEpochSeconds(isoDates[i - 1]) >= timeValueSeconds)
                i--;
            return i; 
        } catch (Exception e) {
            return isoDates.length;
        }
    }

    /**
     * This adds the specified n field's to the isoDate,
     * and returns the resulting GregorianCalendar object.
     *
     * <p>This correctly handles B.C. dates.
     *
     * @param isoDate an iso formatted date time string.
     *   This may include hours, minutes, seconds, decimal, and Z or timezone offset (default=Zulu).  
     * @param n the number of 'units' to be added
     * @param field one of the Calendar or Calendar2 constants for a field
     *    (e.g., Calendar2.YEAR).
     * @return the GregorianCalendar for isoDate with the specified n field's added
     * @throws Exception if trouble  e.g., n is Integer.MAX_VALUE
     */
    public static GregorianCalendar isoDateTimeAdd(String isoDate, int n, int field) 
        throws Exception {
       
        if (n == Integer.MAX_VALUE)
            Test.error(String2.ERROR + " in Calendar2.isoDateTimeAdd: invalid addN=" + n);
        GregorianCalendar gc = parseISODateTimeZulu(isoDate);
        gc.add(field, n);  //no need to adjust for B.C.   gc handles it.
        return gc;
    }

    /**
     * This converts a millis elapsed time value (139872234 ms or 783 ms) to a nice 
     * string (e.g., "7h 4m 5s", "5.783 s", or "783 ms"). 
     * <br>was (e.g., "7:04:05.233" or "783 ms").
     *
     * @param millis  may be negative
     * @return a simplified approximate string representation of elapsed time
     *  (or "infinite[!]" if trouble, e.g., millis is Double.NaN).
     */
    public static String elapsedTimeString(double millis) {
        if (!Math2.isFinite(millis))
            return "infinity";

        long time = Math2.roundToLong(millis);
        String negative = "";
        if (time < 0) {
            negative = "-";
            time = Math.abs(time);
        }
        if (time == Long.MAX_VALUE)
            return "infinity";
        long ms = time % 1000; 
        long sec = time / 1000;
        long min = sec / 60; sec = sec % 60;
        long hr  = min / 60; min = min % 60;
        long day = hr  / 24; hr  = hr  % 24;

        if (day + hr + min + sec == 0)
            return negative + time + " ms";
        if (day + hr + min == 0)
            return negative + sec + "." + String2.zeroPad("" + ms,  3) + " s";
        String ds = day + (day == 1? " day" : " days"); 
        if (hr + min + sec == 0)
            return negative + ds;

        //was
        //return (day > 0? negative + ds + " " : negative) + 
        //    String2.zeroPad("" + hr,  2) + ":" + 
        //    String2.zeroPad("" + min, 2) + ":" + 
        //    String2.zeroPad("" + sec, 2) + 
        //    (ms > 0? "." + String2.zeroPad("" + ms,  3) : "");

        //e.g., 4h 17m 3s apple uses this style; easier to read
        return (day > 0? negative + ds + " " : negative) + 
            ((day > 0|| hr > 0)? hr  + "h " : "") + 
            min + "m " + //hr or min will be >0, so always include it
            sec + 
            //since >59 seconds, don't include millis
            //(ms > 0? "." + String2.zeroPad("" + ms,  3) : "") + 
            "s";
    }


    /**
     * This converts the date, hour, minute, second so gc is at the exact center 
     * of its current month.
     *
     * @param gc  
     * @return the same gc, but modified, for convenience
     * @throws Exception if trouble (e.g., gc is null)
     */
    public static GregorianCalendar centerOfMonth(GregorianCalendar gc) throws Exception {
        int nDaysInMonth = gc.getActualMaximum(Calendar.DATE);   
        gc.set(DATE,        1 + nDaysInMonth / 2);  
        gc.set(HOUR_OF_DAY, Math2.odd(nDaysInMonth)? 12 : 0);
        gc.set(MINUTE, 0);
        gc.set(SECOND, 0);
        gc.set(MILLISECOND, 0);
        return gc;
    }

    /**
     * This clears the fields smaller than 'field' 
     * (e.g., HOUR_OF_DAY clears MINUTE, SECOND, and MILLISECOND,
     * but not HOUR_OF_DAY, MONTH, or YEAR).
     *
     * @param gc
     * @param field e.g., HOUR_OF_DAY
     * @return the same gc, but modified, for convenience
     * @throws Exception if trouble (e.g., gc is null or field is not supported)
     */
    public static GregorianCalendar clearSmallerFields(GregorianCalendar gc, 
        int field) throws Exception {
        
        if (field == MILLISECOND || 
            field == SECOND ||
            field == MINUTE ||
            field == HOUR || field == HOUR_OF_DAY ||
            field == DATE || field == DAY_OF_YEAR ||
            field == MONTH ||
            field == YEAR) {
        } else {
            Test.error(String2.ERROR + " in Calendar2.clearSmallerFields: unsupported field=" + field);
        }
                                  if (field == MILLISECOND) return gc;
        gc.set(MILLISECOND, 0);   if (field == SECOND) return gc;
        gc.set(SECOND, 0);        if (field == MINUTE) return gc;
        gc.set(MINUTE, 0);        if (field == HOUR || field == HOUR_OF_DAY) return gc;
        gc.set(HOUR_OF_DAY, 0);   if (field == DATE) return gc;
        gc.set(DATE, 1);          if (field == MONTH) return gc;
        gc.set(MONTH, 0);         //DAY_OF_YEAR works like YEAR
        return gc;
    }

    /** 
     * This returns the start of a day, n days back from max (or from now if max=NaN).
     *
     * @param nDays
     * @param max  seconds since epoch
     * @return seconds since epoch for the start of a day, n days back from max (or from now if max=NaN).
     */
    public static double backNDays(int nDays, double max) throws Exception {
        GregorianCalendar gc = Math2.isFinite(max)?
            Calendar2.epochSecondsToGc(max) :
            Calendar2.newGCalendarZulu();
        //round to previous midnight, then go back nDays
        Calendar2.clearSmallerFields(gc, Calendar2.DATE);
        return Calendar2.gcToEpochSeconds(gc) - Calendar2.SECONDS_PER_DAY * nDays;
    }

    /**
     * This returns a double[] of maxNValues (or fewer)
     * evenly spaced, between start and stop.
     * The first and last values will be start and stop.
     * The intermediate values will be evenly spaced in a human sense (eg monthly)
     * but the start and stop won't necessarily use the same stride.
     *
     * @param start epoch seconds 
     * @param stop  epoch seconds
     * @param maxNValues maximum desired nValues
     * @return a double[] of nValues (or fewer) epoch seconds values,
     *   evenly spaced, between start and stop.
     *   <br>If start or stop is not finite, this returns null.
     *   <br>If start=stop, this returns just one value.
     *   <br>If start > stop, they are swapped so the results are always ascending.
     *   <br>If trouble, this returns null.
     */
    public static double[] getNEvenlySpaced(double start, double stop,
        int maxNValues) {

        try {
            if (!Math2.isFinite(start) || 
                !Math2.isFinite(stop))
                return null;
            if (start == stop)
                return new double[]{start};
            if (start > stop) {double d = start; start = stop; stop = d;}

            double spm = SECONDS_PER_MINUTE; //double avoids int MAX_VALUE problem
            double sph = SECONDS_PER_HOUR;
            double spd = SECONDS_PER_DAY;
            double range = stop - start;
            double mnv2 = maxNValues/2; //double avoids int MAX_VALUE problem
            int field, biggerField, nice[];   
            double divisor;
            if      (range <= mnv2 * spm)      {field = SECOND;      biggerField = MINUTE;      divisor = 1;         nice = new int[]{1,2,5,10,15,20,30,60}; }
            else if (range <= mnv2 * sph)      {field = MINUTE;      biggerField = HOUR_OF_DAY; divisor = spm;       nice = new int[]{1,2,5,10,15,20,30,60}; }
            else if (range <= mnv2 * spd)      {field = HOUR_OF_DAY; biggerField = DATE;        divisor = sph;       nice = new int[]{1,2,3,4,6,12,24}; }
            else if (range <= mnv2 * 30  * spd){field = DATE;        biggerField = MONTH;       divisor = spd;       nice = new int[]{1,2,5,7}; }
            else if (range <= mnv2 * 365 * spd){field = MONTH;       biggerField = YEAR;        divisor = 30  * spd; nice = new int[]{1,2,3,6,12}; }
            else                               {field = YEAR;        biggerField = -9999;       divisor = 365 * spd; nice = new int[]{1,2,5,10}; }

            //find stride (some number of fields, e.g., 10 seconds)
            //range testing above ensures range/divisor=n, e.g. seconds will be < 60, 
            //  or n minutes will be < 60, nHours < 24, ...
            //and ensure stride is at least 1.
            double dnValues = (range / divisor) / maxNValues;
            int stride = nextNice(dnValues, nice);  //minimum stride will be 1
            if (field == DATE) stride = Math.min(14, stride);
            DoubleArray da = new DoubleArray();
            da.add(start);
            GregorianCalendar nextGc = epochSecondsToGc(start);
            if (field != YEAR) clearSmallerFields(nextGc, biggerField);
            double next = gcToEpochSeconds(nextGc);
            while (next < stop) {
                if (next > start) da.add(next); //it may not be for the first few
                if (field == DATE) { 
                    //repeatedly using DATE=1 is nice, so ...
                    //will subsequent value be in next month?
                    //non-permanent test of this: ndbcSosSalinity has stride = 2 days; results have 2008-09-27 then 2008-10-01
                    int oMonth = nextGc.get(MONTH);
                    nextGc.add(field, 2*stride); //2* sets subsequent value
                    if (nextGc.get(MONTH) == oMonth) {
                        nextGc.add(field, -stride); //go back to regular value
                    } else {
                        nextGc.set(DATE, 1); //go for DATE=1 in next month  e.g., 1,15,1,15 or 1,8,14,21,1,8,14,21,
                    }
                } else {
                    nextGc.add(field, stride);
                }
                next = gcToEpochSeconds(nextGc);
            }
            da.add(stop);
            if (reallyVerbose) String2.log(
                "Calendar2.getNEvenlySpaced start=" + epochSecondsToIsoStringT(start) +
                " stop=" + epochSecondsToIsoStringT(stop) + " field=" + fieldName(field) +
                "\n divisor=" + divisor + " range/divisor/maxNValues=" + dnValues + 
                " stride=" + stride + " nValues=" + da.size());
            return da.toArray();

        } catch (Exception e) {
            String2.log(MustBe.throwableToString(e));
            return null;
        }
    }

    /**
     * This returns the value in nice which is &gt;= d, or a multiple of the last value which is
     * higher than d.
     * This is used to suggest the division distance along an axis.
     *
     * @param d   a value e.g., 2.3 seconds
     * @param nice  an ascending list. e.g., for seconds: 1,2,5,10,15,20,30,60
     * @return the value in nice which is &gt;= d, or a multiple of the last value which is
     * higher than d 
     */
    public static int nextNice(double d, int nice[]) {
        int n = nice.length;
        for (int i = 0; i < n; i++) {
            if (d <= nice[i])
                return nice[i];
        }
        return Math2.roundToInt(Math.ceil(d / nice[n - 1]));
    }


    /** 
     * This rounds to the nearest idealN, idealUnits (e.g., 2 months)
     * (starting at Jan 1, 0000).
     *
     * @param epochSeconds
     * @param idealN  e.g., 1 to 100
     * @param idealUnits  an index of one of the IDEAL_UNITS
     * @return epochSeconds, converted to Zulu GC and rounded to the nearest idealN, idealUnits 
     *    (e.g., 2 months)
     */
    public static GregorianCalendar roundToIdealGC(double epochSeconds, 
        int idealN, int idealUnits) {
        GregorianCalendar gc = newGCalendarZulu(Math2.roundToLong(epochSeconds * 1000));
        if (idealUnits == 5) { //year
            double td = getYear(gc) + gc.get(MONTH) / 12.0; //month is 0..
            int ti = Math2.roundToInt(td / idealN) * idealN; //round to nearest n units
            gc = newGCalendarZulu(ti, 1, 1);

        } else if (idealUnits == 4) { //months
            double td = getYear(gc) * 12 + gc.get(MONTH); //month is 0..
            int ti = Math2.roundToInt(td / idealN) * idealN; //round to nearest n units
            gc = newGCalendarZulu(ti / 12, (ti % 12) + 1, 1);

        } else { //seconds ... days: all have consistent length
            double chunk = idealN * IDEAL_UNITS_SECONDS[idealUnits];  //e.g., decimal number of days
            double td = Math.rint(epochSeconds / chunk) * chunk; //round to nearest n units
            gc = newGCalendarZulu(Math2.roundToLong(td * 1000));
        }
        return gc;
    }

    /**
     * Given a text date time string, this suggests a Java/Joda date/time format.
     *
     * @param sample   
     * @return an appropriate Java/Joda date/time format
     *   or "" if not matched.
     */
    public static String suggestDateTimeFormat(String sample) {
        if (sample == null || sample.length() == 0)
            return "";

        char ch = Character.toLowerCase(sample.charAt(0));
        if (ch >= '0' && ch <= '9') {
            //test formats that start with a digit
            //For all 4 digit years, ensure first digit is 0|1|2 (especially all numeric formats).
            //For all 2 digit months, ensure first digit is 0|1 (especially all numeric formats).
            //etc for dates, hours, minutes, seconds.

            //check for julian date before ISO 8601 format
            if (sample.matches("[0-2][0-9]{3}-[0-3][0-9]{2}"))         return "yyyy-DDD";  
            if (sample.matches("[0-2][0-9]{3}[0-3][0-9]{2}"))          return "yyyyDDD";  
            //special EDVTimeStamp.ISO8601TZ_FORMAT accepts a wide range of variants of 1970-01-01T00:00:00Z
            if (sample.matches("[0-2][0-9]{3}-[0-1][0-9].*"))          return "yyyy-MM-dd'T'HH:mm:ssZ"; 
            if (sample.matches("[0-2][0-9]{3}[0-1][0-9][0-3][0-9][0-2][0-9][0-5][0-9][0-5][0-9]"))         
                                                                       return "yyyyMMddHHmmss";
            if (sample.matches("[0-2][0-9]{3}[0-1][0-9][0-3][0-9][0-2][0-9][0-5][0-9]")) 
                                                                       return "yyyyMMddHHmm";
            if (sample.matches("[0-2][0-9]{3}[0-1][0-9][0-3][0-9][0-2][0-9]")) 
                                                                       return "yyyyMMddHH";
            if (sample.matches("[0-2][0-9]{3}[0-1][0-9][0-3][0-9]"))   return "yyyyMMdd";
            if (sample.matches("[0-2][0-9]{3}[0-1][0-9]"))             return "yyyyMM";
            //note that yy handles conversion of 2 digit year to 4 digits (e.g., 85 -> 1985)
            if (sample.matches("[0-9]{1,2}/[0-9]{1,2}/[0-9]{2,4}"))    return "M/d/yy";      //assume US ordering
            if (sample.matches("[0-9]{1,2} [a-zA-Z]{3} [0-9]{2,4}"))   return "d MMM yy";    //2 Jan 85
            if (sample.matches("[0-9]{1,2}-[a-zA-Z]{3}-[0-9]{2,4}"))   return "d-MMM-yy";    //02-JAN-1985

        } else if (ch >= 'a' && ch <= 'z') {
            //test formats that start with a letter
            if (sample.matches("[a-zA-Z]{3} [0-9]{1,2}, [0-9]{2,4}"))  return "MMM d, yy";   //Jan 2, 1985
            //                 "Sun, 06 Nov 1994 08:49:37 GMT"  //GMT is literal. Joda doesn't parse z
            if (sample.matches("[a-zA-Z]{3}, [0-9]{2} [a-zA-Z]{3} [0-9]{4} [0-9]{2}:[0-9]{2}:[0-9]{2} GMT")) 
                        return "EEE, dd MMM yyyy HH:mm:ss 'GMT'";  //RFC 822 format date time
            //                 "Sun, 06 Nov 1994 08:49:37 -0800" or -08:00
            if (sample.matches("[a-zA-Z]{3}, [0-9]{2} [a-zA-Z]{3} [0-9]{4} [0-9]{2}:[0-9]{2}:[0-9]{2} -[0-9]{2}:?[0-9]{2}")) 
                        return "EEE, dd MMM yyyy HH:mm:ss Z";  //RFC 822 format date time
        }

        //fail
        return "";
    }

    /**
     * This looks for a date time format which is suitable for all elements of sa
     * (other than nulls and ""'s).
     * 
     * @param sa a StringArray, perhaps with consistently formatted date time String values.
     * @return a date time format which is suitable for all elements of sa
     *   (other than nulls and ""'s), or "" if no suggestion.
     */
    public static String suggestDateTimeFormat(StringArray sa) {
        boolean debugMode = false;
        int size = sa.size();
        String format = null;
        for (int row = 0; row < size; row++) {
            String s = sa.get(row);
            if (s == null || s.length() == 0)
                continue;
            if (format == null) {
                format = suggestDateTimeFormat(s);
                if (format.length() == 0) {
                    if (debugMode)
                        String2.log("  suggestDateTimeFormat: no format for \"" + s + "\".");
                    return "";
                }
            } else {
                String tFormat = suggestDateTimeFormat(s);
                if (!format.equals(tFormat)) {
                    if (debugMode)
                        String2.log("  suggestDateTimeFormat: [" + row + "]=\"" + s + 
                            "\" doesn't match format=\"" + format + "\".");
                    return "";
                }
            }
        }
        return format == null? "" : format;
    }

}
