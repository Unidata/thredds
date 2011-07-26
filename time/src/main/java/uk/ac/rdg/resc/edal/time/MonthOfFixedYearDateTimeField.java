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

import org.joda.time.DateTimeField;
import org.joda.time.DateTimeFieldType;
import org.joda.time.DurationField;
import org.joda.time.IllegalFieldValueException;
import org.joda.time.field.ImpreciseDateTimeField;

/**
 * A {@link DateTimeField} representing a month within a year of fixed duration.
 * @author Jon
 */
final class MonthOfFixedYearDateTimeField extends ImpreciseDateTimeField {
    
    private final FixedYearVariableMonthChronology chron;
    private final int[] daysInMonth;
    private final int numMonthsInYear;

    public MonthOfFixedYearDateTimeField(FixedYearVariableMonthChronology chron) {
        super(DateTimeFieldType.monthOfYear(), chron.getAverageMillisInMonth());
        this.chron = chron;
        this.daysInMonth = chron.getMonthLengths();
        this.numMonthsInYear = chron.getMonthLengths().length;
    }

    @Override
    public int get(long instant) {
        int dayOfYear = this.chron.dayOfYear().get(instant);
        // Now search through the months of the year
        int totalDays = 0;
        for (int i = 0; i < daysInMonth.length; i++) {
            totalDays += daysInMonth[i];
            if (dayOfYear <= totalDays) {
                return i + 1; // Month numbers are one-based
            }
        }
        throw new AssertionError("Shouldn't get here");
    }

    @Override
    public long set(long instant, int value) {
        // Check for illegal values: this is not a lenient field
        if (value < 1 || value > this.numMonthsInYear) {
            throw new IllegalFieldValueException(this.getType(), value, 1, this.numMonthsInYear);
        }
        // What is the current month?
        int monthOfYear = this.get(instant);
        // How many months do we have to add to arrive at the new value
        int monthsToAdd = value - monthOfYear;
        // Now add the required number of months
        return this.add(instant, monthsToAdd);
    }

    @Override
    public long add(final long instant, final int numMonths) {
        if (numMonths == 0) return instant;

        // Calculate the number of years we have to add
        int numYearsToAdd = numMonths / this.numMonthsInYear;
        // Add the required number of years to the millisecond instant
        long newInstant = instant + numYearsToAdd * this.chron.years().getUnitMillis();

        // What is the current month?
        int monthOfYear = this.get(instant);
        // Calculate the number of months we have to add
        int numMonthsToAdd = numMonths % this.numMonthsInYear;
        // Add the required number of months TODO!!!
        if (numMonthsToAdd > 0) {
            // Add the months, starting with the current month
            for (int i = 0; i < numMonthsToAdd; i++) {
                // monthOfYear is 1-based
                int monthToAdd = monthOfYear - 1 + i;
                // we might wrap around the array
                monthToAdd %= this.numMonthsInYear;
                long millisToAdd = this.daysInMonth[monthToAdd] * this.chron.days().getUnitMillis();
                newInstant += millisToAdd;
            }
        } else if (numMonthsToAdd < 0) {
            // Subtract the month lengths, starting with the previous month
            for (int i = 0; i < Math.abs(numMonthsToAdd); i++) {
                // The previous month is monthOfYear - 2 because monthOfYear is 1-based
                int monthToSubtract = monthOfYear - 2 - i;
                // we might wrap around the array
                if (monthToSubtract < 0) monthToSubtract += this.numMonthsInYear;
                long millisToSubtract = this.daysInMonth[monthToSubtract] * this.chron.days().getUnitMillis();
                newInstant -= millisToSubtract;
            }
        }

        return newInstant;
    }

    @Override
    public long add(long instant, long numMonths) {
        // TODO: should do a better check for overflow here
        if (numMonths > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Value too large");
        }
        return this.add(instant, (int)numMonths);
    }

    @Override
    public long roundFloor(long instant) {
        int year = this.chron.year().get(instant);
        int monthOfYear = this.get(instant);
        int numCompleteMonths = monthOfYear - 1;

        long millis = (year - 1970) * this.chron.years().getUnitMillis();
        for (int i = 0; i < numCompleteMonths; i++) {
            millis += this.daysInMonth[i] * this.chron.days().getUnitMillis();
        }

        return millis;
    }

    @Override
    public int getMinimumValue() { return 1; }

    @Override
    public int getMaximumValue() { return this.numMonthsInYear; }

    @Override
    public DurationField getRangeDurationField() {
        return this.chron.years();
    }

    /** Always returns false: does not accept impossible months like Floopuary */
    @Override
    public boolean isLenient() { return false; }

}
