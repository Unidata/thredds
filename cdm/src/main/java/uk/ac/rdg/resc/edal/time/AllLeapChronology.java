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

import org.joda.time.DateTimeZone;

/**
 * <p>A Chronology in which each year has exactly 366 days (February is always
 * 29 days long).  This calendar system is used in some climate simulations.</p>
 * <p>In this Chronology, a millisecond instant of zero corresponds with
 * 1970-01-01T00:00:00.000Z and a year has a fixed number of milliseconds
 * (1000*60*60*24*366).</p>
 * <p>There is no concept of an era in this calendar, so all durations and fields
 * relating to this concept are not supported.  Additionally, the concept of a
 * "weekyear" (the year that "owns" a given week) is not implemented.</p>
 * <p>Instances of this class can only be created in {@link DateTimeZone#UTC}.
 * (Support for time zones makes little sense in this chronology).</p>
 * <p>Instances of this class are immutable.</p>
 * @author Jon Blower
 * @see "http://cf-pcmdi.llnl.gov/documents/cf-conventions/1.4/cf-conventions.html#calendar"
 */
public final class AllLeapChronology extends FixedYearVariableMonthChronology {

    private static final int[] MONTH_LENGTHS = new int[]{31, 29, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31};

    private static final AllLeapChronology INSTANCE_UTC = new AllLeapChronology();

    /** Private constructor to prevent direct instantiation */
    private AllLeapChronology() {
        super(MONTH_LENGTHS);
    }

    /** Gets an instance of this Chronology in the UTC time zone */
    public static AllLeapChronology getInstanceUTC() {
        return INSTANCE_UTC;
    }

    @Override
    public String toString() {
        return "All-leap Chronology in UTC";
    }

}
