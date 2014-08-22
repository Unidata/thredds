package ucar.nc2.time;

import net.jcip.annotations.Immutable;
import org.joda.time.DateTime;
import ucar.nc2.units.DateRange;

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

}