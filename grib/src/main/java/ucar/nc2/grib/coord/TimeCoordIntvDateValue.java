package ucar.nc2.grib.coord;

import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.time.CalendarPeriod;

/**
 * Time intervals represented by start and end CalendarDate.
 */
@Immutable
public class TimeCoordIntvDateValue implements Comparable<TimeCoordIntvDateValue> {

  private final CalendarDate start, end;

  public TimeCoordIntvDateValue(CalendarPeriod period, CalendarDate end) {
    this.end = end;
    this.start = end.subtract(period);
  }

  public TimeCoordIntvDateValue(CalendarDate start, CalendarPeriod period) {
    this.start = start;
    this.end = start.add(period);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    TimeCoordIntvDateValue that = (TimeCoordIntvDateValue) o;
    return start.equals(that.start) &&
        end.equals(that.end);
  }

  @Override
  public int hashCode() {
    return Objects.hash(start, end);
  }

  public TimeCoordIntvDateValue(CalendarDate start, CalendarDate end) {
    this.start = start;
    this.end = end;
  }

  public CalendarDate getStart() {
    return start;
  }

  public CalendarDate getEnd() {
    return end;
  }

  // Calculate the offset in units of timeUnit from the given reference date?
  public TimeCoordIntvValue convertReferenceDate(CalendarDate refDate, CalendarPeriod timeUnit) {
    if (timeUnit == null) {
      throw new IllegalArgumentException("null time unit");
    }
    int startOffset = timeUnit.getOffset(refDate, start);   // LOOK wrong - not dealing with value ??
    int endOffset = timeUnit.getOffset(refDate, end);
    return new TimeCoordIntvValue(startOffset, endOffset);
  }

  public int compareTo(@Nonnull TimeCoordIntvDateValue that) {  // first compare start, then end
    int c1 = start.compareTo(that.start);
    return (c1 == 0) ? end.compareTo(that.end) : c1;
  }

  @Override
  public String toString() {
    return String.format("(%s,%s)", start, end);
  }

}

