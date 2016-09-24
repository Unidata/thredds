/*
 * Copyright 1998-2015 John Caron and University Corporation for Atmospheric Research/Unidata
 *
 *  Portions of this software were developed by the Unidata Program at the
 *  University Corporation for Atmospheric Research.
 *
 *  Access and use of this software shall impose the following obligations
 *  and understandings on the user. The user is granted the right, without
 *  any fee or cost, to use, copy, modify, alter, enhance and distribute
 *  this software, and any derivative works thereof, and its supporting
 *  documentation for any purpose whatsoever, provided that this entire
 *  notice appears in all copies of the software, derivative works and
 *  supporting documentation.  Further, UCAR requests that the user credit
 *  UCAR/Unidata in any publications that result from the use of this
 *  software or in any product that includes this software. The names UCAR
 *  and/or Unidata, however, may not be used in any advertising or publicity
 *  to endorse or promote any products or commercial entity unless specific
 *  written permission is obtained from UCAR/Unidata. The user also
 *  understands that UCAR/Unidata is not obligated to provide the user with
 *  any support, consulting, training or assistance of any kind with regard
 *  to the use, operation and performance of this software nor to provide
 *  the user with any updates, revisions, new versions or "bug fixes."
 *
 *  THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 *  IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *  WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *  DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 *  INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 *  FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 *  NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 *  WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */
package ucar.nc2.time;

import org.joda.time.DateTime;
import ucar.nc2.units.DateRange;

import javax.annotation.concurrent.Immutable;
import java.util.Date;

/**
 * A range of CalendarDates.
 *
 * @author caron
 * @since 3/21/11
 */
@Immutable
public class CalendarDateRange {
  private final CalendarDate start, end;
  private final DateTime startDt, endDt;
  private final Calendar cal;

  static public CalendarDateRange of(CalendarDate start, CalendarDate end) {
    return new CalendarDateRange(start, end);
  }

  static public CalendarDateRange of(Date start, Date end) {
    return new CalendarDateRange(CalendarDate.of(start), CalendarDate.of(end));
  }

  private CalendarDateRange(CalendarDate start, CalendarDate end) {
    this.start = start;
    this.end = end;
    this.startDt = start.getDateTime();
    this.endDt = end.getDateTime();
    this.cal = start.getCalendar();
    assert this.cal.equals(end.getCalendar());
  }

  public CalendarDateRange(CalendarDate start, long durationInSecs) {
    this.start = start;
    this.end = start.add((int) durationInSecs, CalendarPeriod.Field.Second );
    this.startDt = start.getDateTime();
    this.endDt = end.getDateTime();
    this.cal = start.getCalendar();
  }

  public CalendarDate getStart() {
    return start;
  }

  public CalendarDate getEnd() {
    return end;
  }

  public long getDurationInSecs() {
    return (endDt.getMillis() - startDt.getMillis()) / 1000;
  }

  // LOOK
  public CalendarDuration getDuration() {
    return null;
  }

  // LOOK
  public CalendarDuration getResolution()  {
    return null;
  }

  // LOOK
  public void setResolution()  {
  }

  public boolean intersects(CalendarDateRange o)  {
    return intersects(o.getStart(), o.getEnd());
  }

  public boolean intersects(CalendarDate start, CalendarDate end)  {
    if (startDt.isAfter(end.getDateTime())) return false;
    if (endDt.isBefore(start.getDateTime())) return false;
    return true;
  }

  public boolean includes(CalendarDate cd) {
    DateTime dt = cd.getDateTime();
    if (startDt.isAfter(dt)) return false;
    if (endDt.isBefore(dt)) return false;
    return true;
  }

  public CalendarDateRange intersect(CalendarDateRange clip) {
    DateTime cs = clip.getStart().getDateTime();
    DateTime s = startDt.isBefore(cs) ? cs : startDt;  // later one

    DateTime ce = clip.getEnd().getDateTime();
    DateTime e = endDt.isBefore(ce) ? endDt : ce;  // earlier one

    return CalendarDateRange.of(CalendarDate.of(cal, s), CalendarDate.of(cal, e));
  }

  public CalendarDateRange extend(CalendarDateRange other)  {
    DateTime cs = other.getStart().getDateTime();
    DateTime s = startDt.isBefore(cs) ? startDt : cs; // earlier one

    DateTime ce = other.getEnd().getDateTime();
    DateTime e = endDt.isBefore(ce) ? ce : endDt;  // later one

    return CalendarDateRange.of(CalendarDate.of(cal, s), CalendarDate.of(cal, e));
  }

  public boolean isPoint() {
    return start.equals(end);
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder();
    sb.append(start).append(" - ").append(end);
    return sb.toString();
  }



  ///////////////////////////////////////////////
  /**
   * Does not handle non-standard calendars
   * @deprecated
   */
  static public CalendarDateRange of(DateRange dr) {
    if (dr == null) return null;
    return CalendarDateRange.of( dr.getStart().getDate(), dr.getEnd().getDate());
  }

  /**
   * Does not handle non-standard calendars
   * @deprecated
   */
  public DateRange toDateRange() {
    return new DateRange(start.toDate(), end.toDate());
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    CalendarDateRange that = (CalendarDateRange) o;

    // All other fields in this class are derived from start or end.
    return start.equals(that.start) && end.equals(that.end);
  }

  @Override
  public int hashCode() {
    int result = start.hashCode();
    result = 31 * result + end.hashCode();
    return result;
  }
}
