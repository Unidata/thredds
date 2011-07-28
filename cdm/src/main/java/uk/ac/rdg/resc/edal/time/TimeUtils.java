/*
 * Copyright (c) 2010 The University of Reading
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. Neither the name of the University of Reading, nor the names of the
 *    authors or contributors may be used to endorse or promote products
 *    derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package uk.ac.rdg.resc.edal.time;

import java.util.StringTokenizer;
import org.joda.time.Chronology;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.chrono.ISOChronology;

/**
 * Utility methods for dealing with dates and times
 * @todo Does this belong in the cdm package?  It's UDUNITS specific.
 * @author Jon
 */
public final class TimeUtils
{

    /** Private constructor to prevent instantiation */
    private TimeUtils() { throw new AssertionError(); }

    /**
     * Gets the length of the given unit in milliseconds.  This accepts
     * seconds, minutes, hours and days, and should be constant across calendar
     * systems.
     */
    public static long getUnitLengthMillis(String unit)
    {
        unit = unit.trim();
        if (unit.equals("seconds") || unit.equals("second") || unit.equals("secs") || unit.equals("sec") || unit.equals("s"))
        {
            return 1000;
        }
        else if (unit.equals("minutes") || unit.equals("minute") || unit.equals("mins") || unit.equals("min"))
        {
            return 1000 * 60;
        }
        else if (unit.equals("hours") || unit.equals("hour") || unit.equals("hrs") || unit.equals("hr") || unit.equals("h"))
        {
            return 1000 * 60 * 60;
        }
        else if (unit.equals("days") || unit.equals("day") || unit.equals("d"))
        {
            return 1000 * 60 * 60 * 24;
        }
        else
        {
            throw new IllegalArgumentException("Unrecognized unit for time axis: " + unit);
        }
    }

    /**
     * Parses a UDUNITS time string (of the form "1992-10-8 15:15:42.5 -6:00")
     * and returns a DateTime in the ISO Chronology.
     * @param baseDateTimeString UDUNITS-formatted time string
     * @return a DateTime in the ISO Chronology, with the time zone set to
     * UTC.
     */
    public static DateTime parseUdunitsTimeString(String baseDateTimeString)
    {
        return parseUdunitsTimeString(baseDateTimeString, null);
    }

    /**
     * Parses a UDUNITS time string (of the form "1992-10-8 15:15:42.5 -6:00")
     * and returns a DateTime in the given Chronology.
     * @param baseDateTimeString UDUNITS-formatted time string
     * @param chronology The Chronology (calendar system) in which the time string
     * is to be interpreted.  This must have a time zone of UTC, otherwise an
     * IllegalArgumentException will be thrown.  If this is null, the ISOChronology
     * (in the UTC time zone) will be assumed.
     * @return a DateTime in the given Chronology, with the time zone set to
     * UTC.
     */
    public static DateTime parseUdunitsTimeString(String baseDateTimeString, Chronology chronology)
    {
        if (chronology == null) chronology = ISOChronology.getInstanceUTC();
        if (!chronology.getZone().equals(DateTimeZone.UTC))
        {
            throw new IllegalArgumentException("The time zone of the Chronology must be UTC");
        }

        // Set the defaults for any values that are not specified
        int year = 0;
        int month = 1;
        int day = 1;
        int hour = 0;
        int minute = 0;
        double second = 0.0;

        // We parse the string using a tokenizer to allow for partial strings
        // (e.g. those that contain only the date and not the time)
        StringTokenizer tokenizer = new StringTokenizer(baseDateTimeString, " ");
        try
        {
            // Parse the date if present
            if (tokenizer.hasMoreTokens())
            {
                StringTokenizer dateTokenizer = new StringTokenizer(tokenizer.nextToken(), "-");
                if (dateTokenizer.hasMoreTokens()) year = Integer.parseInt(dateTokenizer.nextToken());
                if (dateTokenizer.hasMoreTokens()) month = Integer.parseInt(dateTokenizer.nextToken());
                if (dateTokenizer.hasMoreTokens()) day = Integer.parseInt(dateTokenizer.nextToken());
            }

            // Parse the time if present
            if (tokenizer.hasMoreTokens())
            {
                StringTokenizer timeTokenizer = new StringTokenizer(tokenizer.nextToken(), ":");
                if (timeTokenizer.hasMoreTokens()) hour = Integer.parseInt(timeTokenizer.nextToken());
                if (timeTokenizer.hasMoreTokens()) minute = Integer.parseInt(timeTokenizer.nextToken());
                if (timeTokenizer.hasMoreTokens()) second = Double.parseDouble(timeTokenizer.nextToken());
            }

            // Get a DateTime object in this Chronology
            DateTime dt = new DateTime(year, month, day, hour, minute, 0, 0, chronology);
            // Add the seconds
            dt = dt.plus((long)(1000 * second));

            // Parse the time zone if present
            if (tokenizer.hasMoreTokens())
            {
                StringTokenizer zoneTokenizer = new StringTokenizer(tokenizer.nextToken(), ":");
                int hourOffset = zoneTokenizer.hasMoreTokens() ? Integer.parseInt(zoneTokenizer.nextToken()) : 0;
                int minuteOffset = zoneTokenizer.hasMoreTokens() ? Integer.parseInt(zoneTokenizer.nextToken()) : 0;
                DateTimeZone dtz = DateTimeZone.forOffsetHoursMinutes(hourOffset, minuteOffset);

                // Apply the time zone offset, retaining the field values.  This
                // manipulates the millisecond instance.
                dt = dt.withZoneRetainFields(dtz);
                // Now convert to the UTC time zone, retaining the millisecond instant
                dt = dt.withZone(DateTimeZone.UTC);
            }

            return dt;
        }
        catch (NumberFormatException nfe)
        {
            throw new IllegalArgumentException("Illegal base time specification "
                + baseDateTimeString);
        }
    }

}
