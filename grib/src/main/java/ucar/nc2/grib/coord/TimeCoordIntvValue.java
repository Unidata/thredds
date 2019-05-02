package ucar.nc2.grib.coord;

import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.time.CalendarPeriod;

// use for time intervals
@Immutable
public class TimeCoordIntvValue implements Comparable<TimeCoordIntvValue> {
  private final int b1, b2;  // bounds

  public TimeCoordIntvValue(int b1, int b2) {
    this.b1 = b1;
    this.b2 = b2;
  }

  public int getBounds1() {
    return b1;
  }

  public int getBounds2() {
    return b2;
  }

  public int getIntervalSize() {
    return Math.abs(b2 - b1);
  }

  public TimeCoordIntvValue convertReferenceDate(CalendarDate fromDate, CalendarPeriod fromUnit, CalendarDate toDate,
      CalendarPeriod toUnit) {
    CalendarDate start = fromDate.add(fromUnit.multiply(b1));
    CalendarDate end = fromDate.add(fromUnit.multiply(b2));
    int startOffset = toUnit.getOffset(toDate, start);
    int endOffset = toUnit.getOffset(toDate, end);
    return new TimeCoordIntvValue(startOffset, endOffset);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    TimeCoordIntvValue that = (TimeCoordIntvValue) o;
    return b1 == that.b1 &&
        b2 == that.b2;
  }

  @Override
  public int hashCode() {
    return Objects.hash(b1, b2);
  }

  @Override
  public int compareTo(@Nonnull TimeCoordIntvValue o) {
    int c1 = b2 - o.b2;
    return (c1 == 0) ? b1 - o.b1 : c1;
  }

  @Override
  public String toString() {
    return String.format("(%d,%d)", b1, b2);
  }

  public TimeCoordIntvValue offset(double offset) {
    return new TimeCoordIntvValue((int) (offset + b1), (int) (offset + b2));
  }
}

