/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.grib.coord;

import ucar.nc2.time.CalendarDate;
import ucar.nc2.time.CalendarDateRange;
import ucar.nc2.time.CalendarPeriod;

import javax.annotation.concurrent.Immutable;

/**
 * Abstract superclass for time coordinates ( time, timeIntv, time2D)
 * Effectively Immutable
 *
 * @author caron
 * @since 1/23/14
 */
@Immutable
public abstract class CoordinateTimeAbstract implements Coordinate {
  public static final String MIXED_INTERVALS = "Mixed_intervals";
  public static CalendarDateFactory cdf;

  final String periodName;                   // used to create the udunit
  protected final int code;                  // unit of time (Grib1 table 4, Grib2 table 4.4), eg hour, day, month
  protected final CalendarPeriod timeUnit;   // time duration, based on code
  protected final CalendarDate refDate;      // used to create the udunit
  protected final int[] time2runtime;        // for each time, which runtime is used; index into master runtime

  protected String name = "time";

  CoordinateTimeAbstract(int code, CalendarPeriod timeUnit, CalendarDate refDate, int[] time2runtime) {
    this.code = code;
    this.timeUnit = timeUnit;
    this.refDate = (cdf == null) ? refDate : cdf.get(refDate);
    this.time2runtime = time2runtime;

    CalendarPeriod.Field cf = timeUnit.getField();
    if (cf == CalendarPeriod.Field.Month || cf == CalendarPeriod.Field.Year)
      this.periodName = "calendar "+ cf.toString();
    else
      this.periodName = cf.toString();
  }

  @Override
  public int getCode() {
    return code;
  }

  @Override
  public String getUnit() {
    return periodName;
  }

  public String getTimeUdUnit() {
    return periodName + " since " + refDate;
  }

  @Override
  public String getName() {
    return name;
  }

  public void setName(String name) {
    if (!this.name.equals("time")) throw new IllegalStateException("Cant modify");
    this.name = name;
  }

  public CalendarDate getRefDate() {
    return refDate;
  }

  public CalendarPeriod getTimeUnit() {
    return timeUnit;
  }

  public int[] getTime2runtime() {
    return time2runtime;
  }

  public int getMasterRuntimeIndex(int timeIdx) {
    if (time2runtime == null) return -1;
    if (timeIdx < 0 || timeIdx >= time2runtime.length) return -1;
    return time2runtime[timeIdx];
  }

  @Override
  public int getNCoords() {
    return getSize();
  }

  public double getOffsetInTimeUnits(CalendarDate start) {
    return timeUnit.getOffset(start, getRefDate());
  }
  ////////////////////////////////////////

  /**
   * Implements coverting a "complete best" to a "monotonic best".
   * The reftime is not allowed to decrease
   * @return "monotonic best" CoordinateTimeAbstract, based on this one, which is a "complete best"
   */
  public CoordinateTimeAbstract makeBestFromComplete() {
    int[] best = new int[time2runtime.length];
    int last = -1;
    int count = 0;
    for (int i=0; i<time2runtime.length; i++) {
      int time = time2runtime[i];
      if (time >= last) {
        last = time;
        best[i] = time;
        count++;
      } else {
        best[i] = -1;
      }
    }
    return makeBestFromComplete(best, count);
  }

  protected abstract CoordinateTimeAbstract makeBestFromComplete(int[] best, int n);

  public abstract CalendarDateRange makeCalendarDateRange(ucar.nc2.time.Calendar cal);

}
