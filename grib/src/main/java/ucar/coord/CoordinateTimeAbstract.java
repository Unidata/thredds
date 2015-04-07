/*
 *
 *  * Copyright 1998-2013 University Corporation for Atmospheric Research/Unidata
 *  *
 *  *  Portions of this software were developed by the Unidata Program at the
 *  *  University Corporation for Atmospheric Research.
 *  *
 *  *  Access and use of this software shall impose the following obligations
 *  *  and understandings on the user. The user is granted the right, without
 *  *  any fee or cost, to use, copy, modify, alter, enhance and distribute
 *  *  this software, and any derivative works thereof, and its supporting
 *  *  documentation for any purpose whatsoever, provided that this entire
 *  *  notice appears in all copies of the software, derivative works and
 *  *  supporting documentation.  Further, UCAR requests that the user credit
 *  *  UCAR/Unidata in any publications that result from the use of this
 *  *  software or in any product that includes this software. The names UCAR
 *  *  and/or Unidata, however, may not be used in any advertising or publicity
 *  *  to endorse or promote any products or commercial entity unless specific
 *  *  written permission is obtained from UCAR/Unidata. The user also
 *  *  understands that UCAR/Unidata is not obligated to provide the user with
 *  *  any support, consulting, training or assistance of any kind with regard
 *  *  to the use, operation and performance of this software nor to provide
 *  *  the user with any updates, revisions, new versions or "bug fixes."
 *  *
 *  *  THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 *  *  IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *  *  WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *  *  DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 *  *  INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 *  *  FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 *  *  NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 *  *  WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 *
 */

package ucar.coord;

import net.jcip.annotations.Immutable;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.time.CalendarDateRange;
import ucar.nc2.time.CalendarPeriod;

import java.util.List;

/**
 * Abstract superclass for time coordinates ( time, timeIntv, time2D)
 * Effectively Immutable
 *
 * @author caron
 * @since 1/23/14
 */
@Immutable
public abstract class CoordinateTimeAbstract implements Coordinate {
  static public final String MIXED_INTERVALS = "Mixed_intervals";
  static public CalendarDateFactory cdf;

  protected final int code;                  // unit of time (Grib1 table 4, Grib2 table 4.4), eg hour, day, month
  protected final CalendarPeriod timeUnit;   // time duration, based on code
  protected final String periodName;         // used to create the udunit
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

  public CalendarPeriod getPeriod() {
    return timeUnit;
  }

  @Override
  public String getUnit() {
    return periodName;
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

  public double getTimeUnitScale() { return timeUnit.getValue(); }

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
