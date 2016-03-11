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
package ucar.coord;

import net.jcip.annotations.Immutable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.nc2.grib.GribUtils;
import ucar.nc2.grib.TimeCoord;
import ucar.nc2.grib.grib1.Grib1ParamTime;
import ucar.nc2.grib.grib1.Grib1Record;
import ucar.nc2.grib.grib1.Grib1SectionProductDefinition;
import ucar.nc2.grib.grib1.tables.Grib1Customizer;
import ucar.nc2.grib.grib2.Grib2Utils;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.time.CalendarDateRange;
import ucar.nc2.time.CalendarDateUnit;
import ucar.nc2.grib.grib2.Grib2Pds;
import ucar.nc2.grib.grib2.Grib2Record;
import ucar.nc2.time.CalendarPeriod;
import ucar.nc2.util.Counters;
import ucar.nc2.util.Indent;
import ucar.nc2.util.Misc;

import java.util.*;

/**
 * Time coordinates that are offsets from the reference date (not intervals).
 *
 * @author caron
 * @since 11/24/13
 */
@Immutable
public class CoordinateTime extends CoordinateTimeAbstract implements Coordinate {
  static private final Logger logger = LoggerFactory.getLogger(CoordinateTime.class);

  private final List<Integer> offsetSorted;

  public CoordinateTime(int code, CalendarPeriod timeUnit, CalendarDate refDate, List<Integer> offsetSorted, int[] time2runtime) {
    super(code, timeUnit, refDate, time2runtime);
    this.offsetSorted = Collections.unmodifiableList(offsetSorted);
  }

  CoordinateTime(CoordinateTime org, CalendarDate refDate) {
    super(org.code, org.timeUnit, refDate, null);
    this.offsetSorted = org.getOffsetSorted();
  }

  public List<Integer> getOffsetSorted() {
    return offsetSorted;
  }

  /* @Override
  public int findIndexContaining(double need) {
    double bestDiff = Double.MAX_VALUE;
    int bestIdx = 0;
    for (int i = 0; i < offsetSorted.size(); i++) {
      Integer coord = offsetSorted.get(i);
      double diff = Math.abs(need - coord);
      if (diff < bestDiff) {
        bestDiff = diff;
        bestIdx = i;
      }
    }
    return bestIdx;
  }
  @Override
  public int findIndexContaining(double need) {
    for (int i = 0; i < offsetSorted.size(); i++) {
      Integer coord = offsetSorted.get(i);
      if (coord == (int) need) return i;
    }
    return -1;
  } */

  @Override
  public List<? extends Object> getValues() {
    return offsetSorted;
  }

  @Override
  public int getIndex(Object val) {
    return Collections.binarySearch(offsetSorted, (Integer) val);
  }

  @Override
  public Object getValue(int idx) {
    if (idx < 0 || idx >= offsetSorted.size())
      return null;
    return offsetSorted.get(idx);
  }

  @Override
  public int getSize() {
    return offsetSorted.size();
  }

  @Override
  public Type getType() {
    return Type.time;
  }

  @Override
  public int estMemorySize() {
    return 320 + getSize() * (16);
  }

  @Override
  public CalendarDateRange makeCalendarDateRange(ucar.nc2.time.Calendar cal) {
    CalendarDateUnit cdu = CalendarDateUnit.withCalendar(cal, periodName + " since " + refDate.toString());
    CalendarDate start = cdu.makeCalendarDate(offsetSorted.get(0));
    CalendarDate end = cdu.makeCalendarDate(offsetSorted.get(getSize() - 1));
    return CalendarDateRange.of(start, end);
  }

  @Override
  public void showInfo(Formatter info, Indent indent) {
    info.format("%s%s:", indent, getType());
    for (Integer cd : offsetSorted)
      info.format(" %3d,", cd);
    info.format(" (%d) %n", offsetSorted.size());
    if (time2runtime != null)
      info.format("%stime2runtime: %s", indent, Misc.showInts(time2runtime));
  }

  @Override
  public void showCoords(Formatter info) {
    info.format("Time offsets: (%s) ref=%s %n", getUnit(), getRefDate());
    for (Integer cd : offsetSorted)
      info.format("   %3d%n", cd);
  }

  @Override
  public Counters calcDistributions() {
    ucar.nc2.util.Counters counters = new Counters();
    counters.add("resol");

    List<Integer> offsets = getOffsetSorted();
    for (int i = 0; i < offsets.size() - 1; i++) {
      int diff = offsets.get(i + 1) - offsets.get(i);
      counters.count("resol", diff);
    }

    return counters;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    CoordinateTime that = (CoordinateTime) o;

    if (code != that.code) return false;
    if (!offsetSorted.equals(that.offsetSorted)) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = offsetSorted.hashCode();
    result = 31 * result + code;
    return result;
  }

  ////////////////////////////////////////////////

  protected CoordinateTimeAbstract makeBestFromComplete(int[] best, int n) {
    List<Integer> offsetSortedBest = new ArrayList<>(offsetSorted.size());
    int[] time2runtimeBest = new int[n];
    int count = 0;
    for (int i = 0; i < best.length; i++) {
      int time = best[i];
      if (time >= 0) {
        time2runtimeBest[count] = time;
        offsetSortedBest.add(offsetSorted.get(i));
        count++;
      }
    }

    return new CoordinateTime(code, timeUnit, refDate, offsetSortedBest, time2runtimeBest);
  }

  //////////////////////////////////////////////////////

  static public class Builder2 extends CoordinateBuilderImpl<Grib2Record> {
    private final int code;  // pdsFirst.getTimeUnit()
    private final CalendarPeriod timeUnit;
    private final CalendarDate refDate;

    public Builder2(int code, CalendarPeriod timeUnit, CalendarDate refDate) {
      this.code = code;
      this.timeUnit = timeUnit;
      this.refDate = refDate;
    }

    public Builder2(CoordinateTime from) {
      this.code = from.getCode();
      this.timeUnit = from.getTimeUnit();
      this.refDate = from.getRefDate();
    }

    @Override
    public Object extract(Grib2Record gr) {
      Grib2Pds pds = gr.getPDS();
      int offset = pds.getForecastTime();
      int tuInRecord = pds.getTimeUnit();
      if (tuInRecord == code) {
        return offset;

      } else {
        CalendarPeriod period = Grib2Utils.getCalendarPeriod(tuInRecord);
        if (period == null) {
          logger.warn("Cant find period for time unit="+tuInRecord);
          return offset;
        }
        CalendarDate validDate = refDate.add(period.multiply(offset));
        return TimeCoord.getOffset(refDate, validDate, timeUnit);
      }
    }

    @Override
    public Coordinate makeCoordinate(List<Object> values) {
      List<Integer> offsetSorted = new ArrayList<>(values.size());
      for (Object val : values) offsetSorted.add((Integer) val);
      Collections.sort(offsetSorted);
      return new CoordinateTime(code, timeUnit, refDate, offsetSorted, null);
    }
  }

  static public class Builder1 extends CoordinateBuilderImpl<Grib1Record> {
    final Grib1Customizer cust;
    final int code;  // pdsFirst.getTimeUnit()
    final CalendarPeriod timeUnit;
    final CalendarDate refDate;

    public Builder1(Grib1Customizer cust, int code, CalendarPeriod timeUnit, CalendarDate refDate) {
      this.cust = cust;
      this.code = code;
      this.timeUnit = timeUnit;
      this.refDate = refDate;
    }

    @Override
    public Object extract(Grib1Record gr) {
      Grib1SectionProductDefinition pds = gr.getPDSsection();
      Grib1ParamTime ptime = gr.getParamTime(cust);

      int offset = ptime.getForecastTime();
      int tuInRecord = pds.getTimeUnit();
      if (tuInRecord == code) {
        return offset;

      } else {
        CalendarDate validDate = GribUtils.getValidTime(refDate, tuInRecord, offset);
        return TimeCoord.getOffset(refDate, validDate, timeUnit);
      }

    }

    @Override
    public Coordinate makeCoordinate(List<Object> values) {
      List<Integer> offsetSorted = new ArrayList<>(values.size());
      for (Object val : values) offsetSorted.add((Integer) val);
      Collections.sort(offsetSorted);
      return new CoordinateTime(code, timeUnit, refDate, offsetSorted, null);
    }
  }


}
