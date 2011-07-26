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
import org.joda.time.ReadablePartial;
import org.joda.time.field.PreciseDurationDateTimeField;

/**
 * A {@link DateTimeField} that represents the day in the month within a fixed-length
 * year.  This is a {@link PreciseDurationDateTimeField} because a day is a precise
 * duration, even though a month is not.
 * @author Jon
 */
final class DayOfMonthOfFixedYearDateTimeField extends PreciseDurationDateTimeField {
    
    private final FixedYearVariableMonthChronology chron;
    private final int[] daysInMonth;
    private int maxValue;
    
    public DayOfMonthOfFixedYearDateTimeField(FixedYearVariableMonthChronology chron) {
        super(DateTimeFieldType.dayOfMonth(), chron.days());
        this.chron = chron;

        this.daysInMonth = chron.getMonthLengths();
        this.maxValue = daysInMonth[0];
        for (int i = 1; i < daysInMonth.length; i++) {
            this.maxValue = Math.max(this.maxValue, daysInMonth[i]);
        }
    }

    @Override
    public int get(long instant) {
        int dayOfYear = this.chron.dayOfYear().get(instant);
        int monthOfYear = this.chron.monthOfYear().get(instant);
        // Calculate the number of days in the completed months so far
        int numCompletedMonths = monthOfYear - 1;
        int daysInCompletedMonths = 0;
        for (int i = 0; i < numCompletedMonths; i++) {
            daysInCompletedMonths += this.daysInMonth[i];
        }
        return dayOfYear - daysInCompletedMonths;
    }

    @Override
    public int getMinimumValue() {
        return 1;
    }

    @Override
    public int getMaximumValue() {
        return this.maxValue;
    }

    @Override
    public int getMaximumValue(long instant) {
        int monthOfYear = this.chron.monthOfYear().get(instant);
        return this.daysInMonth[monthOfYear - 1];
    }

    // Adapted from the package-private BasicDayOfMonthDateTimeField
    @Override
    public int getMaximumValue(ReadablePartial partial) {
        if (partial.isSupported(DateTimeFieldType.monthOfYear())) {
            int month = partial.get(DateTimeFieldType.monthOfYear());
            return this.daysInMonth[month - 1]; // Months are 1-based
        }
        return this.getMaximumValue();
    }

    // Adapted from the package-private BasicDayOfMonthDateTimeField
    @Override
    public int getMaximumValue(ReadablePartial partial, int[] values) {
        int size = partial.size();
        for (int i = 0; i < size; i++) {
            if (partial.getFieldType(i) == DateTimeFieldType.monthOfYear()) {
                int month = values[i];
                return this.daysInMonth[month - 1];
            }
        }
        return this.getMaximumValue();
    }

    @Override
    public DurationField getRangeDurationField() {
        return this.chron.months();
    }

}
