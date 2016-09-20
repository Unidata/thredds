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
 *
 */
package ucar.nc2.ft2.coverage;

import ucar.nc2.AttributeContainer;
import ucar.nc2.constants.CDM;
import ucar.nc2.constants.CF;
import ucar.nc2.time.*;
import ucar.nc2.util.NamedAnything;
import ucar.nc2.util.NamedObject;

import javax.annotation.concurrent.Immutable;
import java.util.ArrayList;
import java.util.List;

/**
 * Helper class for Time coordinate axes
 *
 * @author caron
 * @since 7/11/2015
 */
@Immutable
public class TimeHelper {

  static public TimeHelper factory(String units, AttributeContainer atts) {
    if (units == null)
      units = atts.findAttValueIgnoreCase(CDM.UDUNITS, null);
    if (units == null)
      units = atts.findAttValueIgnoreCase(CDM.UNITS, null);
    if (units == null)
        throw new IllegalStateException("No units");

    Calendar cal = getCalendarFromAttribute(atts);
    CalendarDateUnit dateUnit;
    try {
      dateUnit = CalendarDateUnit.withCalendar(cal, units); // this will throw exception on failure
      return new TimeHelper( dateUnit);
    } catch (IllegalArgumentException e) {
      return null;
    }
  }

  //////////////////////////////////////////////

  // final Calendar cal;
  final CalendarDateUnit dateUnit;
  // final CalendarDate refDate;
  // final double duration;

  private TimeHelper(CalendarDateUnit dateUnit) {
    // this.cal = cal;
    this.dateUnit = dateUnit;
    //this.refDate = dateUnit.getBaseCalendarDate();
    //this.duration = dateUnit.getTimeUnit().getValueInMillisecs();
  }

  // copy on modify
  public TimeHelper setReferenceDate(CalendarDate refDate) {
    CalendarDateUnit cdUnit = CalendarDateUnit.of(dateUnit.getCalendar(), dateUnit.getCalendarField(), refDate);
    return new TimeHelper(cdUnit);
  }

  public String getUdUnit() {
    return dateUnit.getUdUnit();
  }

  // get offset from runDate, in units of dateUnit
  public double offsetFromRefDate(CalendarDate date) {
    return dateUnit.makeOffsetFromRefDate(date);
  }

  public List<NamedObject> getCoordValueNames(CoverageCoordAxis1D axis) {
    axis.loadValuesIfNeeded();
    List<NamedObject> result = new ArrayList<>();
    for (int i = 0; i < axis.getNcoords(); i++) {
      double value;
      switch (axis.getSpacing()) {
        case regularPoint:
        case irregularPoint:
          value = axis.getCoordMidpoint(i);
          result.add(new NamedAnything(makeDate(value), axis.getAxisType().toString()));
          break;

        case regularInterval:
        case contiguousInterval:
        case discontiguousInterval:
          CoordInterval coord = new CoordInterval(axis.getCoordEdge1(i), axis.getCoordEdge2(i), 3);
          result.add(new NamedAnything(coord, coord + " " + axis.getUnits()));
          break;
      }
    }

    return result;
  }

  public CalendarDate getRefDate() {
    return dateUnit.getBaseCalendarDate();
  }

  public CalendarDate makeDate(double value) {
    return dateUnit.makeCalendarDate(value);
  }

  public CalendarDateRange getDateRange(double startValue, double endValue) {
    CalendarDate start = makeDate( startValue);
    CalendarDate end = makeDate( endValue);
    return CalendarDateRange.of(start, end);
  }

  public double getOffsetInTimeUnits(CalendarDate start, CalendarDate end) {
    return dateUnit.getCalendarPeriod().getOffset(start, end);
  }

  public CalendarDate makeDateInTimeUnits(CalendarDate start, double addTo) {
    return start.add(CalendarPeriod.of((int) addTo, dateUnit.getCalendarField()));
  }

  public static ucar.nc2.time.Calendar getCalendarFromAttribute(AttributeContainer atts) {
    String cal = atts.findAttValueIgnoreCase(CF.CALENDAR, null);
    if (cal == null) return null;
    return ucar.nc2.time.Calendar.get(cal);
  }

  public Calendar getCalendar() {
    return dateUnit.getCalendar();
  }

  public CalendarDateUnit getCalendarDateUnit() {
    return dateUnit;
  }


}
