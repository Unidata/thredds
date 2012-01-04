/*
 * Copyright (c) 1998 - 2011. University Corporation for Atmospheric Research/Unidata
 * Portions of this software were developed by the Unidata Program at the
 * University Corporation for Atmospheric Research.
 *
 * Access and use of this software shall impose the following obligations
 * and understandings on the user. The user is granted the right, without
 * any fee or cost, to use, copy, modify, alter, enhance and distribute
 * this software, and any derivative works thereof, and its supporting
 * documentation for any purpose whatsoever, provided that this entire
 * notice appears in all copies of the software, derivative works and
 * supporting documentation.  Further, UCAR requests that the user credit
 * UCAR/Unidata in any publications that result from the use of this
 * software or in any product that includes this software. The names UCAR
 * and/or Unidata, however, may not be used in any advertising or publicity
 * to endorse or promote any products or commercial entity unless specific
 * written permission is obtained from UCAR/Unidata. The user also
 * understands that UCAR/Unidata is not obligated to provide the user with
 * any support, consulting, training or assistance of any kind with regard
 * to the use, operation and performance of this software nor to provide
 * the user with any updates, revisions, new versions or "bug fixes."
 *
 * THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 * INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 * FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 * NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 * WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */

package ucar.nc2.grib;

import net.jcip.annotations.Immutable;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.time.CalendarDateRange;
import ucar.nc2.time.CalendarDateUnit;
import ucar.nc2.time.CalendarPeriod;

import java.util.*;

/**
 * Generalized Time Coordinate.
 * Can handle
 * <ol>
 * <li> "n unit since refDate"</li>
 * <li> "Date"</li>
 * <li> interval (start,end) using "n unit since refDate"</li>
 * <li> interval (startDate,endDate)</li>
 * </ol>
 *
 * @author caron
 * @since 3/30/11
 */
@Immutable
public class TimeCoord {

  public static int getOffset(CalendarDate refDate, CalendarDate cd, CalendarPeriod timeUnit) {
    long msecs = cd.getDifferenceInMsecs(refDate);
    return (int) Math.round(msecs / timeUnit.getValueInMillisecs());
  }

  private CalendarDate runDate;
  private CalendarPeriod timeUnit;
  protected List<Integer> coords;
  protected List<Tinv> intervals;

  private String units;
  private int code = -1;

  public TimeCoord(int code, String units, List coords) {
    this.code = code;
    this.units = units;

    CalendarDateUnit cdu = CalendarDateUnit.of(null, units);
    this.runDate = cdu.getBaseCalendarDate();
    this.timeUnit = cdu.getTimeUnit();

    Object atom = (coords.size() > 0) ? coords.get(0) : null;

    if (atom instanceof Tinv) {
      this.coords = null;
      this.intervals = coords;
    } else {
      this.coords = coords;
      this.intervals = null;
    }
  }

  public TimeCoord(CalendarDate runDate, CalendarPeriod timeUnit, List coords) {
    this.runDate = runDate;
    this.timeUnit = timeUnit;

    Object atom = (coords == null || coords.size() == 0) ? null : coords.get(0);

    if (atom instanceof CalendarDate) {
      List<Integer> offsets = new ArrayList<Integer>(coords.size());
      double duration = timeUnit.getValueInMillisecs();
      for (Object coord : coords) {
        CalendarDate cd = (CalendarDate) coord;
        long msecs = cd.getDifferenceInMsecs(runDate);
        int val = (int) Math.round(msecs / duration);
        offsets.add(val);
      }
      this.coords = offsets;
      this.intervals = null;

    } else if (atom instanceof Tinv) {
      this.intervals = coords;
      this.coords = null;

    } else {
      this.coords = coords;
      this.intervals = null;
    }
  }

  public CalendarDate getRunDate() {
    return runDate;
  }

  public CalendarDateRange getCalendarRange() {
    CalendarDate rd = getRunDate();
    if (coords != null) {
      CalendarDate start = rd.add(timeUnit.multiply(coords.get(0)));
      CalendarDate end = rd.add(timeUnit.multiply(coords.get(coords.size() - 1)));
      return CalendarDateRange.of(start, end);
    } else {
      CalendarDate start = rd.add(timeUnit.multiply(intervals.get(0).b1));
      CalendarDate end = rd.add(timeUnit.multiply(intervals.get(intervals.size() - 1).b2));
      return CalendarDateRange.of(start, end);
    }
  }

  public boolean isInterval() {
    return (intervals != null);
  }

  public List<Integer> getCoords() {
    return coords;
  }

  public List<Tinv> getIntervals() {
    return intervals;
  }

  public String getUnits() {
    if (units != null) return units;
    return timeUnit.getField().toString() + " since " + runDate;
  }

  public double getTimeUnitScale() {
    return timeUnit.getValue();
  }

  public CalendarPeriod getTimeUnit() {
    return timeUnit;
  }

  public int getCode() {
    return code;
  }

  public String getName() {
    return (code == 0) ? "time": "time"+code;
  }

  public String getType() {
    return isInterval() ? "interval" : "integers";
  }

  public int getSize() {
    return isInterval() ? intervals.size() : coords.size();
  }

  public String getTimeIntervalName() {
    if (!isInterval()) return null;
    Tinv first = intervals.get(0); // they should all be the same
    int value = (int) ((first.b2 - first.b1) * getTimeUnitScale());
    return value + "_" + timeUnit.getField().toString();
  }

  @Override
  public String toString() {
    Formatter out = new Formatter();
    out.format(" type=%-10s timeUnit=%s runDate= %-26s%n    ", getType(), timeUnit, runDate);
    if (isInterval())
      for (Tinv tinv : intervals) out.format("%s, ", tinv);
    else {
      for (Integer val : coords) out.format("%d, ", val);
      out.format(" units (%s) since %s", timeUnit, runDate);
    }

    return out.toString();
  }

  /**
   * Instances that have the same runtime, timeUnit and coordinates are equal
   *
   * @param tother compare this TimeCoord's data
   * @return true if data are equal
   */
  public boolean equalsData(TimeCoord tother) {
    // LOOK could speed up by using a hashcode - still have to compare coords if true, however
    if (!runDate.equals(tother.runDate)) return false;
    if (!timeUnit.equals(tother.timeUnit)) return false;

    if (isInterval() != tother.isInterval()) return false;

    if (isInterval()) {
      if (intervals.size() != tother.intervals.size())
        return false;
      for (int i = 0; i < intervals.size(); i++) {
        if (!(intervals.get(i).equals(tother.intervals.get(i))))
          return false;
      }
      return true;

    } else {
      if (coords.size() != tother.coords.size())
        return false;
      for (int i = 0; i < coords.size(); i++) {
        if (!(coords.get(i).equals(tother.coords.get(i))))
          return false;
      }
      return true;
    }
  }

  public int findInterval(Tinv tinv) {
    for (int i = 0; i < intervals.size(); i++) {
      if (intervals.get(i).equals(tinv))
        return i;
    }
    return -1;
  }

  public int findIdx(int offsetHour) {
    for (int i = 0; i < coords.size(); i++) {
      if (coords.get(i).equals(offsetHour))
        return i;
    }
    return -1;
  }

  /////////////////////////////////////////////

  /**
   * Look through timeCoords to see if one matches want.
   * Matches means equalsData() is true.
   * If not found, make a new one and add to timeCoords.
   *
   * @param timeCoords look through this list
   * @param want       find equivilent
   * @return return equivilent or make a new one and add to timeCoords
   */
  static public int findCoord(List<TimeCoord> timeCoords, TimeCoord want) {
    if (want == null) return -1;

    for (int i = 0; i < timeCoords.size(); i++) {
      if (want.equalsData(timeCoords.get(i)))
        return i;
    }

    timeCoords.add(want);
    return timeCoords.size() - 1;
  }

  // use for time intervals
  public static class Tinv implements Comparable<Tinv> {
    private final int b1, b2;  // bounds

    public Tinv(int b1, int b2) {
      this.b1 = b1;
      this.b2 = b2;
    }

    public int getBounds1() {
      return b1;
    }

    public int getBounds2() {
      return b2;
    }

    public Tinv convertReferenceDate(CalendarDate fromDate, CalendarPeriod fromUnit, CalendarDate toDate, CalendarPeriod toUnit) {
      CalendarDate start = fromDate.add(fromUnit.multiply(b1));
      CalendarDate end = fromDate.add(fromUnit.multiply(b2));
      int startOffset = TimeCoord.getOffset(toDate, start, toUnit);
      int endOffset = TimeCoord.getOffset(toDate, end, toUnit);
      return new TimeCoord.Tinv(startOffset, endOffset);
    }

    @Override
    public boolean equals(Object o) {
      Tinv tinv = (Tinv) o;
      if (b1 != tinv.b1) return false;
      if (b2 != tinv.b2) return false;
      return true;
    }

    @Override
    public int hashCode() {
      int result;
      result = b1;
      result = 31 * result + b2;
      return result;
    }

    @Override
    public int compareTo(Tinv o) {
      int c1 = b2 - o.b2;
      return (c1 == 0) ? b1 - o.b1 : c1;
    }

    @Override
    public String toString() {
      Formatter out = new Formatter();
      out.format("(%d,%d)", b1, b2);
      return out.toString();
    }

  }


}
