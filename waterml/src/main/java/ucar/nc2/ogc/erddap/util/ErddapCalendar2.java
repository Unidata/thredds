/* This file is Copyright (c) 2005 Robert Alten Simons (info@cohort.com).
 * See the MIT/X-like license in LICENSE.txt.
 * For more information visit www.cohort.com or contact info@cohort.com.
 */
package ucar.nc2.ogc.erddap.util;

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
public class ErddapCalendar2 {
    //useful static variables
    public final static int YEAR        = Calendar.YEAR;
    public final static int MILLISECOND = Calendar.MILLISECOND;

    public final static int SECONDS_PER_MINUTE = 60; 
    public final static int SECONDS_PER_HOUR   = 60 * 60; //3600
    public final static int SECONDS_PER_DAY    = 24 * 60 * 60; //86400   31Days=2678400  365days=31536000

    public final static TimeZone zuluTimeZone = TimeZone.getTimeZone("Zulu");

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
        String errorInMethod = ErddapString2.ERROR + " in Calendar2.getTimeBaseAndFactor(" + tsUnits + "):\n";

        if (tsUnits == null) {
            throw new NullPointerException(errorInMethod + "tsUnits must be non-null.");
        }

        int sincePo = tsUnits.toLowerCase().indexOf(" since ");
        if (sincePo <= 0)
            throw new IllegalArgumentException(errorInMethod + "units string doesn't contain \" since \".");
        double factorToGetSeconds = factorToGetSeconds(tsUnits.substring(0, sincePo));
        GregorianCalendar baseGC = parseISODateTimeZulu(tsUnits.substring(sincePo + 7));
        double baseSeconds = baseGC.getTimeInMillis() / 1000.0;

        return new double[]{baseSeconds, factorToGetSeconds};
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

        throw new RuntimeException(
                ErddapString2.ERROR + " in Calendar2.factorToGetSeconds: units=\"" + units + "\" is invalid.");
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
        return new GregorianCalendar(zuluTimeZone);
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
        if (sLength < 1 || !(s.charAt(0) == '-' || ErddapString2.isDigit(s.charAt(0)))) {
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
                while (po2 < sLength && ErddapString2.isDigit(s.charAt(po2))) po2++; //digit

                //if no number, return; we're done
                if (po2 == po1)
                    return;
                if (part > 0 && separatorN[part - 1] == '.') {
                    resultsN[part] = ErddapMath2.roundToInt(1000 *
                            ErddapString2.parseDouble("0." + s.substring(po1, po2)));
                    //String2.log("  millis=" + resultsN[part]);
                } else {
                    resultsN[part] = ErddapString2.parseInt(s.substring(po1, po2));
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
                        int pmPart = ErddapString2.indexOf(separatorN, '±', part + 1);
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
                    return;  
                }
            }
        }
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
        if (s.length() < 1 || !ErddapString2.isDigit(s.charAt(0)))
            throw new RuntimeException(
                    ErddapString2.ERROR + " in parseISODateTime: for first character of dateTime='" + s + "' isn't a digit!");
        if (gc == null)
            throw new RuntimeException(ErddapString2.ERROR + " in parseISODateTime: gc is null!");

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
        s = ErddapString2.replaceAll(s, ' ', '+');

        //separators (\u0000=any non-digit)
        char separator[] = {'-','-','\u0000',':',':','.','±', ':', '\u0000'};
        parseN(s, separator, ymdhmsmom);
        if (ymdhmsmom[0] == Integer.MAX_VALUE)
            throw new RuntimeException(
                    ErddapString2.ERROR + " in parseISODateTime: dateTime='" + s + "' has an invalid format!");

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
     */
    public static GregorianCalendar parseISODateTimeZulu(String s) {
        return parseISODateTime(newGCalendarZulu(), s);
    }
}
