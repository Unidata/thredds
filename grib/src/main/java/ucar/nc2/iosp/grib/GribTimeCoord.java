/*
 * Copyright 1998-2009 University Corporation for Atmospheric Research/Unidata
 *
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

package ucar.nc2.iosp.grib;

import ucar.grib.GribGridRecord;
import ucar.grib.GribPds;

import ucar.nc2.grib.GribUtils;
import ucar.nc2.grib.TimeCoord;
import ucar.nc2.grib.grib1.tables.Grib1WmoTimeType;
import ucar.nc2.iosp.grid.GridRecord;
import ucar.nc2.iosp.grid.GridTimeCoord;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.time.CalendarPeriod;

import java.util.*;

/**
 * A Time Coordinate for a Grib dataset.
 *
 * @author caron
 */
public class GribTimeCoord extends GridTimeCoord {
  static private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(GribTimeCoord.class);

  /**
   * Create a new GridTimeCoord from the list of GridRecord
   *
   * @param records records to use
   * @param where file location for warn message
   */
  GribTimeCoord(List<GridRecord> records, String where) {
    super(records, where);

    // interval case - only for GRIB
    GribGridRecord ggr = (GribGridRecord) records.get(0);
    if (!ggr.isInterval()) return;

    timeIntvs = (refDateDiffers) ? computeIntervalsWithDiff(records, true) : computeIntervals(records, true);
  }

  private List<TimeCoordWithInterval> computeIntervals(List<GridRecord> records, boolean checkConstant) {
    boolean same = true;
    List<TimeCoordWithInterval> result = new ArrayList<TimeCoordWithInterval>(records.size());

    int intv = -1;
    for (GridRecord gr : records) {
      GribGridRecord ggr = (GribGridRecord) gr;

      GribPds pds = ggr.getPds();
      int[] timeInv = pds.getForecastTimeInterval();

      int start = timeInv[0];
      int end = timeInv[1];
      int intv2 = end - start;

      if (intv2 > 0) { // skip those weird zero-intervals when testing for constant interval
        if (intv < 0) intv = intv2;
        else same = same && (intv == intv2);
      }

      Date validTime = gr.getValidTime();
      TimeCoordWithInterval timeCoordIntv = new TimeCoordWithInterval(validTime, start, intv2);
      if (!result.contains(timeCoordIntv))
        result.add(timeCoordIntv);
    }
    if (same && checkConstant) constantInterval = intv;
    Collections.sort(result);
    return result;
  }

  private List<TimeCoordWithInterval> computeIntervalsWithDiff(List<GridRecord> records, boolean checkConstant) {
    boolean same = true;
    List<TimeCoordWithInterval> result = new ArrayList<TimeCoordWithInterval>(records.size());

    int intv = -1;
    for (GridRecord gr : records) {
      GribGridRecord ggr = (GribGridRecord) gr;
      TimeCoord.Tinv tinv = getTinv(ggr);

      int start = tinv.getBounds1();
      int end = tinv.getBounds2();
      int intv2 = end - start;

      if (intv2 > 0) { // skip those weird zero-intervals when testing for constant interval
        if (intv < 0) intv = intv2;
        else same = same && (intv == intv2);
      }

      Date validTime = gr.getValidTime();
      TimeCoordWithInterval timeCoordIntv = new TimeCoordWithInterval(validTime, start, intv2);
      if (!result.contains(timeCoordIntv))
        result.add(timeCoordIntv);
    }
    if (same && checkConstant) constantInterval = intv;
    Collections.sort(result);
    return result;
  }

  private TimeCoord.Tinv getTinv(GribGridRecord ggr) {
    GribPds pds = ggr.getPds();
    int[] timeInv = pds.getForecastTimeInterval();

    // taken from Rectilyser
    TimeCoord.Tinv org = new TimeCoord.Tinv(timeInv[0], timeInv[1]);
    CalendarPeriod fromUnit = GribUtils.getCalendarPeriod(pds.getTimeUnit());
    CalendarPeriod toUnit = GribUtils.getCalendarPeriod(this.timeUnit);
    return org.convertReferenceDate(CalendarDate.of(pds.getReferenceDate()), fromUnit, CalendarDate.of(baseDate), toUnit);
  }

  /**
   * match time values - can this list of GridRecords use this coordinate?
   *
   * @param records list of records
   * @return true if they are the same as this
   */
  @Override
  protected boolean matchTimes(List<GridRecord> records) {
    if (!(records.get(0) instanceof GribGridRecord)) return false;

     // check intervals match
    GribGridRecord ggr = (GribGridRecord) records.get(0);
    if (ggr.isInterval() != isInterval())
      return false;

    // make sure that the time units agree
    for (GridRecord record : records) {
      if (!this.timeUdunit.equals(record.getTimeUdunitName()))
        return false;
    }

    if (isInterval()) {
      List<TimeCoordWithInterval> timeList = (refDateDiffers) ? computeIntervalsWithDiff(records, false) : computeIntervals(records, false);
      Collections.sort(timeList);
      return timeList.equals(timeIntvs);

    } else {
      return super.matchTimes(records);
    }

  }

  /**
   * Find the index of a GridRecord in the list of times
   *
   * @param record the GridRecord
   * @return the index in the list of time values, or -1 if not found
   */
  @Override
  public int findIndex(GridRecord record) {
    Date validTime = record.getValidTime();
    if (!isInterval())
      return times.indexOf(validTime);
    else {
      int index = 0;
      for (TimeCoordWithInterval t : timeIntvs) {
        GribGridRecord ggr = (GribGridRecord) record;
        TimeCoord.Tinv tinv = getTinv(ggr);
        int diff = tinv.getBounds2() - tinv.getBounds1();
        if (t.coord.equals(validTime) && t.interval == diff) return index;
        index++;
      }
      return -1;
    }
  }

}
