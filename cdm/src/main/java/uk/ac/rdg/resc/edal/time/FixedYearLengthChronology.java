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

import org.joda.time.Chronology;
import org.joda.time.DateTimeConstants;
import org.joda.time.DateTimeField;
import org.joda.time.DateTimeFieldType;
import org.joda.time.DateTimeZone;
import org.joda.time.DurationField;
import org.joda.time.DurationFieldType;
import org.joda.time.chrono.BaseChronology;
import org.joda.time.field.MillisDurationField;
import org.joda.time.field.PreciseDateTimeField;
import org.joda.time.field.PreciseDurationDateTimeField;
import org.joda.time.field.PreciseDurationField;
import org.joda.time.field.ZeroIsMaxDateTimeField;

/**
 * <p>A Chronology in which every year has the same number of days.  Such calendar
 * systems are used in many climate simulations.</p>
 * <p>In these Chronologies, a millisecond instant of zero corresponds with
 * 1970-01-01T00:00:00.000Z and a year has a fixed number of milliseconds.</p>
 * <p>There is no concept of an era in these Chronologies, so all durations and fields
 * relating to this concept are not supported.  Additionally, the concept of a
 * "weekyear" (the year that "owns" a given week) is not implemented.</p>
 * <p>Instances of this class can only be created in {@link DateTimeZone#UTC}.
 * (Support for time zones makes little sense in this chronology).</p>
 * <p>Instances of this class are immutable.</p>
 * <p><i>Note: Much of this code was copied from the package-private
 * BasicChronology.</i></p>
 * @author Jon Blower
 * @see "http://cf-pcmdi.llnl.gov/documents/cf-conventions/1.4/cf-conventions.html#calendar"
 */
abstract class FixedYearLengthChronology extends BaseChronology {

    ///// DURATIONS /////

    private static final DurationField millisecondDuration = MillisDurationField.INSTANCE;

    private static final DurationField secondDuration =
        new PreciseDurationField(DurationFieldType.seconds(), DateTimeConstants.MILLIS_PER_SECOND);

    private static final DurationField minuteDuration =
        new PreciseDurationField(DurationFieldType.minutes(), DateTimeConstants.MILLIS_PER_MINUTE);

    private static final DurationField hourDuration =
        new PreciseDurationField(DurationFieldType.hours(), DateTimeConstants.MILLIS_PER_HOUR);

    private static final DurationField halfdayDuration =
        new PreciseDurationField(DurationFieldType.halfdays(), 12 * DateTimeConstants.MILLIS_PER_HOUR);

    private static final DurationField dayDuration =
        new PreciseDurationField(DurationFieldType.days(), 2 * halfdayDuration.getUnitMillis());

    private static final DurationField weekDuration =
        new PreciseDurationField(DurationFieldType.weeks(), 7 * dayDuration.getUnitMillis());

    // We don't know the length of the year or century until we know how many
    // days there are in a year
    private final DurationField yearDuration;
    private final DurationField centuryDuration;


    ///// DATE-TIME FIELDS /////

    private static final DateTimeField millisOfSecond =
        new PreciseDateTimeField(DateTimeFieldType.millisOfSecond(), millisecondDuration, secondDuration);

    private static final DateTimeField millisOfDay =
        new PreciseDateTimeField(DateTimeFieldType.millisOfDay(), millisecondDuration, dayDuration);

    private static final DateTimeField secondOfMinute =
        new PreciseDateTimeField(DateTimeFieldType.secondOfMinute(), secondDuration, minuteDuration);

    private static final DateTimeField secondOfDay =
        new PreciseDateTimeField(DateTimeFieldType.secondOfDay(), secondDuration, dayDuration);

    private static final DateTimeField minuteOfHour =
        new PreciseDateTimeField(DateTimeFieldType.minuteOfHour(), minuteDuration, hourDuration);

    private static final DateTimeField minuteOfDay =
        new PreciseDateTimeField(DateTimeFieldType.minuteOfDay(), minuteDuration, dayDuration);

    private static final DateTimeField hourOfDay =
        new PreciseDateTimeField(DateTimeFieldType.hourOfDay(), hourDuration, dayDuration);

    private static final DateTimeField hourOfHalfday =
        new PreciseDateTimeField(DateTimeFieldType.hourOfHalfday(), hourDuration, halfdayDuration);

    private static final DateTimeField halfdayOfDay =
        new PreciseDateTimeField(DateTimeFieldType.halfdayOfDay(), halfdayDuration, dayDuration);

    private static final DateTimeField clockhourOfDay =
        new ZeroIsMaxDateTimeField(hourOfDay, DateTimeFieldType.clockhourOfDay());

    private static final DateTimeField clockhourOfHalfday =
        new ZeroIsMaxDateTimeField(hourOfHalfday, DateTimeFieldType.clockhourOfHalfday());

    private static final DateTimeField dayOfWeek =
        new PreciseDateTimeField(DateTimeFieldType.dayOfWeek(), dayDuration, weekDuration);

    // We don't know the length of the year or century until we know how many
    // days there are in a year
    private final DateTimeField dayOfYear;
    private final DateTimeField yearOfCentury;
    private final DateTimeField year;

    private final int daysInYear;
    
    private static final class YearField extends PreciseDurationDateTimeField {

        public YearField(DurationField yearDuration) {
            super(DateTimeFieldType.year(), yearDuration);
        }

        @Override
        public int get(long instant) {
            // We need to use Math.floor() to deal with negative instants
          long millis = this.getDurationField().getUnitMillis();
          double imillis = millis == 0 ? 0 : 1.0 / millis;
          return (int) Math.floor(instant * imillis) + 1970;
        }

        /** Returns null: the field has no range */
        @Override
        public DurationField getRangeDurationField() { return null; }

        @Override
        public int getMinimumValue() { return this.get(Long.MIN_VALUE); }

        @Override
        // We subtract one to ensure that the whole of this year can be
        // encoded
        public int getMaximumValue() { return this.get(Long.MAX_VALUE) - 1; }
    };


    ///// CONSTRUCTORS AND FACTORIES /////

    /**
     * @param daysInYear The number of days in each year
     */
    protected FixedYearLengthChronology(int daysInYear) {
        this.daysInYear = daysInYear;

        this.yearDuration = new PreciseDurationField(DurationFieldType.years(),
                daysInYear * dayDuration.getUnitMillis());
        this.centuryDuration = new PreciseDurationField(DurationFieldType.centuries(),
                100 * yearDuration.getUnitMillis());

        this.dayOfYear = new OneBasedPreciseDateTimeField(DateTimeFieldType.dayOfYear(),
                dayDuration, this.yearDuration);
        this.yearOfCentury =  new PreciseDateTimeField(DateTimeFieldType.yearOfCentury(),
                this.yearDuration, this.centuryDuration);
        this.year = new YearField(this.yearDuration);
    }

    ///// DURATION ACCESSORS /////

    @Override
    public final DurationField millis() { return millisecondDuration; }

    @Override
    public final DurationField seconds() { return secondDuration; }

    @Override
    public final DurationField minutes() { return minuteDuration; }

    @Override
    public final DurationField hours() { return hourDuration; }

    @Override
    public final DurationField halfdays() { return halfdayDuration; }

    /** Each day has exactly the same length: there is no daylight saving */
    @Override
    public final DurationField days() { return dayDuration; }

    /** Each week has 7 days */
    @Override
    public final DurationField weeks() { return weekDuration; }

    @Override
    public abstract DurationField months();

    @Override
    public final DurationField years() { return this.yearDuration; }

    @Override
    public final DurationField centuries() { return this.centuryDuration; }



    ///// DATE-TIME FIELD ACCESSORS /////

    @Override
    public final DateTimeField millisOfSecond() { return millisOfSecond; }

    @Override
    public final DateTimeField millisOfDay() { return millisOfDay; }

    @Override
    public final DateTimeField secondOfMinute() { return secondOfMinute; }

    @Override
    public final DateTimeField secondOfDay() { return secondOfDay; }

    @Override
    public final DateTimeField minuteOfHour() { return minuteOfHour; }

    @Override
    public final DateTimeField minuteOfDay() { return minuteOfDay; }

    @Override
    public final DateTimeField hourOfDay() { return hourOfDay; }

    @Override
    public final DateTimeField hourOfHalfday() { return hourOfHalfday; }

    @Override
    public final DateTimeField halfdayOfDay() { return halfdayOfDay; }

    @Override
    public final DateTimeField clockhourOfDay() { return clockhourOfDay; }

    @Override
    public final DateTimeField clockhourOfHalfday() { return clockhourOfHalfday; }

    @Override
    public final DateTimeField dayOfWeek() { return dayOfWeek; }

    @Override
    public abstract DateTimeField dayOfMonth();

    @Override
    public final DateTimeField dayOfYear() { return dayOfYear;}

    @Override
    public abstract DateTimeField monthOfYear();

    @Override
    public final DateTimeField year() { return year; }

    @Override
    public final DateTimeField yearOfCentury() { return yearOfCentury; }

    /** Returns the number of days in the year */
    final int getDaysInYear() { return this.daysInYear; }

    /** Always returns UTC */
    @Override
    public final DateTimeZone getZone() { return DateTimeZone.UTC; }

    /** Throws UnsupportedOperationException unless the time zone is UTC */
    @Override
    public final Chronology withZone(DateTimeZone zone) {
        if (zone.equals(DateTimeZone.UTC)) return this.withUTC();
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /** Returns this object */
    @Override
    public final Chronology withUTC() { return this; }

}
