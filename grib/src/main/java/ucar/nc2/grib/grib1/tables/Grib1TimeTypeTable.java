/*
 * Copyright (c) 1998 - 2012. University Corporation for Atmospheric Research/Unidata
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

package ucar.nc2.grib.grib1.tables;

import ucar.nc2.time.CalendarPeriod;

/**
 * Describe
 *
 * @author caron
 * @since 1/13/12
 */
public class Grib1TimeTypeTable {

  static public enum StatType {Average, Accumulation, Difference, StdDev, Variance}

  /**
   * Convert a time unit to a CalendarPeriod
   *
   * @param timeUnit (table 4)
   * @return equivalent CalendarPeriod
   */
  static public CalendarPeriod getCalendarPeriod(int timeUnit) {
    // LOOK - some way to intern these ?
    switch (timeUnit) { // code table 4.4
      case 0:
        return CalendarPeriod.of(1, CalendarPeriod.Field.Minute);
      case 1:
        return CalendarPeriod.of(1, CalendarPeriod.Field.Hour);
      case 2:
        return CalendarPeriod.of(1, CalendarPeriod.Field.Day);
      case 3:
        return CalendarPeriod.of(1, CalendarPeriod.Field.Month);
      case 4:
        return CalendarPeriod.of(1, CalendarPeriod.Field.Year);
      case 5:
        return CalendarPeriod.of(10, CalendarPeriod.Field.Year);
      case 6:
        return CalendarPeriod.of(30, CalendarPeriod.Field.Year);
      case 7:
        return CalendarPeriod.of(100, CalendarPeriod.Field.Year);
      case 10:
        return CalendarPeriod.of(3, CalendarPeriod.Field.Hour);
      case 11:
        return CalendarPeriod.of(6, CalendarPeriod.Field.Hour);
      case 12:
        return CalendarPeriod.of(12, CalendarPeriod.Field.Hour);
      case 13:
        return CalendarPeriod.of(15, CalendarPeriod.Field.Minute);
      case 14:
        return CalendarPeriod.of(30, CalendarPeriod.Field.Minute);
      default:
        throw new UnsupportedOperationException("Unknown time unit = " + timeUnit);
    }
  }

  /**
   * The time unit statistical type, derived from code table 5)
   *
   * @return time unit statistical type
   */
  static public StatType getStatType(int timeRangeIndicator) {
    switch (timeRangeIndicator) {
      case 3:
      case 6:
      case 7:
      case 51:
      case 113:
      case 115:
      case 117:
      case 123:
        return StatType.Average;
      case 4:
      case 114:
      case 116:
      case 124:
        return StatType.Accumulation;
      case 5:
        return StatType.Difference;
      case 118:
        return StatType.Variance;
      case 119:
      case 125:
        return StatType.StdDev;
      default:
        return null;
    }
  }

  // code table 5 - 2010 edition of WMO manual on codes
  static public String getTimeTypeName(int timeRangeIndicator, int p1, int p2) {
    String timeRange;

    switch (timeRangeIndicator) {

      /* Forecast product valid for reference time + P1 (P1 > 0), or
        Uninitialized analysis product for reference time (P1 = 0), or
        Image product for reference time (P1 = 0) */
      case 0:
        timeRange = (p1 == 0) ? "Uninitialized analysis or image product for reference time" : "Forecast product valid for RT + P1";
        break;

      // Initialized analysis product for reference time (P1 = 0)
      case 1:
        timeRange = "Initialized analysis product for reference time";
        break;

      // Product with a valid time ranging between reference time + P1 and reference time + P2
      case 2:
        timeRange = "product valid, interval = (RT + P1) to (RT + P2)";
        break;

      // Average (reference time + P1 to reference time + P2)
      case 3:
        timeRange = "Average, interval = (RT + P1) to (RT + P2)";
        break;

      /* Accumulation  (reference  time  +  P1  to  reference  time  +  P2)  product  considered  valid  at
        reference time + P2 */
      case 4:
        timeRange = "Accumulation, interval = (RT + P1) to (RT + P2)";
        break;

      /* Difference  (reference  time  +  P2  minus  reference  time  +  P1)  product  considered  valid  at
        reference time + P2 */
      case 5:
        timeRange = "Difference, interval = (RT + P2) - (RT + P1)";
        break;

      // Average (reference time - P1 to reference time - P2)
      case 6:
        timeRange = "Average, interval = (RT - P1) to (RT - P2)";
        break;

      // Average (reference time - P1 to reference time + P2)
      case 7:
        timeRange = "Average, interval = (RT - P1) to (RT + P2)";
        break;

      // P1 occupies octets 19 and 20; product valid at reference time + P1
      case 10:
        timeRange = "product valid at RT + P1";
        break;

      /* Climatological  mean  value:  multiple  year  averages  of  quantities  which  are  themselves
        means over some period of time (P2) less than a year. The reference time (R) indicates the
        date and time of the start of a period of time, given by R to R + P2, over which a mean is
        formed; N indicates the number of such period-means that are averaged together to form
        the  climatological  value,  assuming  that  the  N  period-mean  fields  are  separated  by  one
        year. The reference time indicates the start of the N-year climatology.

        If P1 = 0 then the data averaged in the basic interval P2 are assumed to be continuous, i.e. all available data
        are simply averaged together.

        If P1 = 1 (the unit of time  octet 18, Code table 4  is not
        relevant here) then the data averaged together in the basic interval P2 are valid only at the
        time (hour, minute) given in the reference time, for all the days included in the P2 period.
        The units of P2 are given by the contents of octet 18 and Code table 4 */
      case 51:
        timeRange = "Climatological mean values from RT to (RT + P2)";
        break;

      /* Average  of  N  forecasts  (or  initialized  analyses);  each  product  has  forecast  period  of  P1
        (P1 = 0 for initialized analyses); products have reference times at intervals of P2, beginning
        at the given reference time */
      case 113:
        timeRange = "Average of N forecasts, intervals = (refTime + i * P2, refTime + i * P2 + P1)";
        break;

      /* Accumulation of N forecasts (or initialized analyses); each product has forecast period of
        P1  (P1  =  0  for  initialized  analyses);  products  have  reference  times  at  intervals  of  P2,
        beginning at the given reference time */
      case 114:
        timeRange = "Accumulation of N forecasts, intervals = (refTime + i * P2, refTime + i * P2 + P1)";
        break;

      /* Average of N forecasts, all with the same reference time; the first has a forecast period of
         P1, the remaining forecasts follow at intervals of P2 */
      case 115:
        timeRange = "Average of N forecasts, intervals = (refTime, refTime + P1 + i * P2)";
        break;

      /* Accumulation  of  N  forecasts,  all  with  the  same  reference  time;  the  first  has  a  forecast
        period of P1, the remaining forecasts follow at intervals of P2 */
      case 116:
        timeRange = "Accumulation of N forecasts, intervals = (refTime, refTime + P1 + i * P2)";
        break;

      /* Average of N forecasts; the first has a forecast period of P1, the subsequent ones have
        forecast periods reduced from the previous one by an interval of P2; the reference time for
        the first is given in octets 13 to 17, the subsequent ones have reference times increased
        from the previous one by an interval of P2. Thus all the forecasts have the same valid time,
        given by the initial reference time + P1 */
      case 117:
        timeRange = "Average of N forecasts, intervals = (refTime + i * P2, refTime + P1)";
        break;

      /* Temporal  variance,  or  covariance,  of  N  initialized  analyses;  each  product  has  forecast
        period of P1 = 0; products have reference times at intervals of P2, beginning at the given
        reference time */
      case 118:
        timeRange = "Temporal variance or covariance of N initialized analyses, timeCoord = (refTime + i * P2)";
        break;

      /* Standard deviation of N forecasts, all with the same reference time with respect to the time
        average of forecasts; the first forecast has a forecast period of P1, the remaining forecasts
        follow at intervals of P2 */
      case 119:
        timeRange = "Standard Deviation of N forecasts, timeCoord = (refTime + P1 + i * P2)";
        break;

      // Average of N uninitialized analyses, starting at the reference time, at intervals of P2
      case 123:
        timeRange = "Average of N uninitialized analyses, intervals = (refTime, refTime + i * P2)";
        break;

      // Accumulation of N uninitialized analyses, starting at the reference time, at intervals of P2
      case 124:
        timeRange = "Accumulation of N uninitialized analyses, intervals = (refTime, refTime + i * P2)";
        break;

      /* Standard deviation of N forecasts, all with the same reference time with respect to time
        average of the time tendency of forecasts; the first forecast has a forecast period of P1,
        the remaining forecasts follow at intervals of P2 */
      case 125:
        timeRange = "Standard deviation of N forecasts, intervals = (refTime, refTime + P1 + i * P2)";
        break;

      default:
        timeRange = "Unknown Time Range Indicator " + timeRangeIndicator;
    }

    return timeRange;
  }

  ///////////////////////////////////////////////////////////////////////////////////////////////
}
