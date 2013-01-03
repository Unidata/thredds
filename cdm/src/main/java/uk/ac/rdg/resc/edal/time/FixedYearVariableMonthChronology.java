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
import org.joda.time.DateTimeField;
import org.joda.time.DurationField;

/**
 * <p>A {@link Chronology} in which each year has the same number of days but
 * the lengths of the months may be different.  May be useful for climate and
 * palaeoclimate calendars.</p>
 * <p>In this Chronology, a millisecond instant of zero corresponds with
 * 1970-01-01T00:00:00.000Z and a year has a fixed number of milliseconds.</p>
 * <p>There is no concept of an era in this calendar, so all durations and fields
 * relating to this concept are not supported.  Additionally, the concept of a
 * "weekyear" (the year that "owns" a given week) is not implemented.</p>
 * <p>Instances of this class can only be created in {@link "DateTimeZone#UTC"}.
 * (Support for time zones makes little sense in this chronology).</p>
 * <p>Instances of this class are immutable.</p>
 * @author Jon Blower
 * @see "http://cf-pcmdi.llnl.gov/documents/cf-conventions/1.4/cf-conventions.html#calendar"
 * @author Jon
 */
public class FixedYearVariableMonthChronology extends FixedYearLengthChronology {
    
    private final int[] monthLengths;
    
    private final DateTimeField dayOfMonth;
    private final DateTimeField monthOfYear;
    private final DurationField monthDuration;
    
    /**
     * @param monthLengths The number of days in each month
     * @throws NullPointerException if monthLengths is null
     * @throws IllegalArgumentException if monthLengths is empty or contains
     * zero or negative values.
     */
    public FixedYearVariableMonthChronology(int[] monthLengths) {
        super(sumArray(monthLengths));
        this.monthLengths = monthLengths.clone(); // defensive copy
        this.dayOfMonth = new DayOfMonthOfFixedYearDateTimeField(this);
        this.monthOfYear = new MonthOfFixedYearDateTimeField(this);
        this.monthDuration = this.monthOfYear.getDurationField();
    }
    
    /**
     * Calculates the sum of the values in the given array.
     * @throws NullPointerException if arr is null
     * @throws IllegalArgumentException if arr is empty or contains
     * zero or negative values.
     */
    private static int sumArray(int[] arr) {
        if (arr == null) throw new NullPointerException("null array");
        if (arr.length == 0) throw new IllegalArgumentException("Zero-length array");
        int sum = 0;
        for (int i = 0; i < arr.length; i++) {
            if (arr[i] <= 0) {
                throw new IllegalArgumentException("All array values must be > 0");
            }
            sum += arr[i];
        }
        return sum;
    }

    @Override
    public final DateTimeField dayOfMonth() {
        return this.dayOfMonth;
    }

    @Override
    public final DateTimeField monthOfYear() {
        return this.monthOfYear;
    }

    @Override
    public final DurationField months() {
        return this.monthDuration;
    }

    /**
     * Gets the length of each month in days.  Returns a new array with each
     * invocation to maintain integrity of internal data.
     * @todo return an immutable List
     */
    public int[] getMonthLengths() {
        return this.monthLengths.clone(); // Defensive copy
    }

    /** Gets the average number of milliseconds in each month */
    public long getAverageMillisInMonth() {
        return this.years().getUnitMillis() / this.monthLengths.length;
    }

    @Override
    public String toString() {
        return "Custom calendar: fixed year length, variable month length";
    }

}
