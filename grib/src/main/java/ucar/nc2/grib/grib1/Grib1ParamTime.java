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

package ucar.nc2.grib.grib1;

import net.jcip.annotations.Immutable;
import ucar.nc2.grib.GribNumbers;
import ucar.nc2.grib.GribStatType;
import ucar.nc2.grib.grib1.tables.Grib1Customizer;

/**
 * Time coordinate from the PDS.
 * Process information from GRIB-1 Table 4: "Forecast time unit"
 * Process information from GRIB-1 Table 5: "Time range indicator"
 *
 * Handles standard (WMO), Grib1Customizer must override / augment
 *
 * @author John
 * @since 9/4/11
 */
@Immutable
public class Grib1ParamTime {

    // code table 5 - 2010 edition of WMO manual on codes
  static public String getTimeTypeName(int timeRangeIndicator) {
    String timeRange;

    switch (timeRangeIndicator) {

      /* Forecast product valid for reference time + P1 (P1 > 0), or
        Uninitialized analysis product for reference time (P1 = 0), or
        Image product for reference time (P1 = 0) */
      case 0:
        timeRange = "Uninitialized analysis / image product / forecast product valid for RT + P1";
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
        // if (p1 == 0) timeRange += " continuous";
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

      // ECMWF "Average of N Forecast" added 11/21/2014. pretend its WMO standard. maybe should move to ecmwf ??
      // see "http://emoslib.sourcearchive.com/documentation/000370.dfsg.2/grchk1_8F-source.html"
      // C     Add Time range indicator = 120 Average of N Forecast. Each product
      // C             is an accumulation from forecast lenght P1 to forecast
      // C              lenght P2, with reference times at intervals P2-P1
      case 120:
        timeRange = "Average of N Forecasts (ECMWF), accumulation from forecast P1 to P2, with reference times at intervals P2-P1";
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

  ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  private final Grib1Customizer cust;   // passed in

  private final int timeRangeIndicator; // code Table 5 (octet 21)
  private final boolean isInterval;
  private final int start, end;     // for intervals
  private final int forecastTime;   // for non-intervals

  public Grib1ParamTime(Grib1Customizer cust, int timeRangeIndicator, boolean isInterval, int start, int end, int forecastTime) {
    this.cust = cust;
    this.timeRangeIndicator = timeRangeIndicator;
    this.isInterval = isInterval;
    this.start = start;
    this.end = end;
    this.forecastTime = forecastTime;
  }

  /**
   * Handles GRIB-1 code table 5 : "Time range indicator".
   *
   * @param cust customizer
   * @param pds the Grib1SectionProductDefinition
   */
  public Grib1ParamTime(Grib1Customizer cust, Grib1SectionProductDefinition pds) {
    this.cust = cust;

    int p1 = pds.getTimeValue1();  // octet 19
    int p2 = pds.getTimeValue2();  // octet 20
    int timeRangeIndicatorLocal = pds.getTimeRangeIndicator(); // octet 21
    int n = pds.getNincluded();

    int startLocal = 0;
    int endLocal = 0;
    int forecastTimeLocal = 0;
    boolean isIntervalLocal = false;

    switch (timeRangeIndicatorLocal) {

      /* Forecast product valid for reference time + P1 (P1 > 0), or
        Uninitialized analysis product for reference time (P1 = 0), or
        Image product for reference time (P1 = 0) */
      case 0:
        forecastTimeLocal = p1;
        break;

      // Initialized analysis product for reference time (P1 = 0)
      case 1:
        // accept defaults
        break;

      case 2:  // Product with a valid time ranging between reference time + P1 and reference time + P2
      case 3:  // Average (reference time + P1 to reference time + P2)
      case 4:  // Accumulation  (reference  time  +  P1  to  reference  time  +  P2)  product  considered  valid  at reference time + P2
      case 5:  // Difference  (reference  time  +  P2  minus  reference  time  +  P1)  product  considered  valid  at reference time + P2
        startLocal = p1;
        endLocal = p2;
        isIntervalLocal = true;
        break;

      // Average (reference time - P1 to reference time - P2)
      case 6:
        startLocal = -p1;
        endLocal = -p2;
        isIntervalLocal = true;
        break;

      // Average (reference time - P1 to reference time + P2)
      case 7:
        startLocal = -p1;
        endLocal = p2;
        isIntervalLocal = true;
        break;

      // P1 occupies octets 19 and 20; product valid at reference time + P1
      case 10:
        forecastTimeLocal = GribNumbers.int2(p1, p2);
        break;


      /* Climatological  mean  value:  multiple  year  averages  of  quantities  which  are  themselves
        means over some period of time (P2) less than a year. The reference time (R) indicates the
        date and time of the start of a period of time, given by R to R + P2, over which a mean is
        formed; N indicates the number of such period-means that are averaged together to form
        the  climatological  value,  assuming  that  the  N  period-mean  fields  are  separated  by  one
        year. The reference time indicates the start of the N-year climatology. If P1 = 0 then the
        data averaged in the basic interval P2 are assumed to be continuous, i.e. all available data
        are simply averaged together. If P1 = 1 (the unit of time – octet 18, Code table 4 – is not
        relevant here) then the data averaged together in the basic interval P2 are valid only at the
        time (hour, minute) given in the reference time, for all the days included in the P2 period.
        The units of P2 are given by the contents of octet 18 and Code table 4 */
      case 51:  // LOOK ??
        forecastTimeLocal = p2;
        break;

      /* 113: Average  of  N  forecasts  (or  initialized  analyses);  each  product  has  forecast  period  of  P1
        (P1 = 0 for initialized analyses); products have reference times at intervals of P2, beginning
        at the given reference time.

         114: Accumulation of N forecasts (or initialized analyses); each product has forecast period of
                 P1  (P1  =  0  for  initialized  analyses);  products  have  reference  times  at  intervals  of  P2,
                 beginning at the given reference time
         115: Average of N forecasts, all with the same reference time; the first has a forecast period of
                  P1, the remaining forecasts follow at intervals of P2
         116: Accumulation  of  N  forecasts,  all  with  the  same  reference  time;  the  first  has  a  forecast
                 period of P1, the remaining forecasts follow at intervals of P2

         */
      case 113:
      case 114:
      case 115:
      case 116:
        forecastTimeLocal = p1;
        startLocal = p1;
        endLocal = (n > 0) ? p1 + (n-1) * p2 : p1;  // LOOK switch to n-1 on 3/13/2015
        isIntervalLocal = (n > 0);
        break;

      /* Average of N forecasts; the first has a forecast period of P1, the subsequent ones have
        forecast periods reduced from the previous one by an interval of P2; the reference time for
        the first is given in octets 13 to 17, the subsequent ones have reference times increased
        from the previous one by an interval of P2. Thus all the forecasts have the same valid time,
        given by the initial reference time + P1 */
      case 117:
        endLocal = p1;
        isIntervalLocal = true;
        break;

      /* Temporal  variance,  or  covariance,  of  N  initialized  analyses;  each  product  has  forecast
        period of P1 = 0; products have reference times at intervals of P2, beginning at the given
        reference time */
      case 118:
        endLocal = n * p2;
        isIntervalLocal = true;
        break;

      /* Standard deviation of N forecasts, all with the same reference time with respect to the time
        average of forecasts; the first forecast has a forecast period of P1, the remaining forecasts
        follow at intervals of P2 */
      case 119:
        startLocal = p1;
        endLocal = p1 + n * p2;
        isIntervalLocal = true;
        break;

      // ECMWF "Average of N Forecast" added 11/21/2014
      // see "http://emoslib.sourcearchive.com/documentation/000370.dfsg.2/grchk1_8F-source.html"
      // C     Add Time range indicator = 120 Average of N Forecast. Each product
      // C             is an accumulation from forecast lenght P1 to forecast
      // C              lenght P2, with reference times at intervals P2-P1
      case 120:
        startLocal = p1;
        endLocal = p2;
        isIntervalLocal = true;
        break;

       // Average of N uninitialized analyses, starting at the reference time, at intervals of P2
      case 123:
        endLocal = n * p2;
        isIntervalLocal = true;
        break;

      // Accumulation of N uninitialized analyses, starting at the reference time, at intervals of P2
      case 124:
        endLocal = n * p2;
        isIntervalLocal = true;
        break;

      /* Standard deviation of N forecasts, all with the same reference time with respect to time
        average of the time tendency of forecasts; the first forecast has a forecast period of P1,
        the remaining forecasts follow at intervals of P2 */
      case 125:
        endLocal = p1 + n * p2;
        isIntervalLocal = true;
        break;

      default:
        throw new IllegalArgumentException("PDS: Unknown Time Range Indicator " + timeRangeIndicatorLocal);
    }

    // added 11/30/2014. If interval (0,0), change to non interval at 0
    // analysis (0-hour) datasets use these (0,0) intervals, they are initialization values (I think).
    // by eliminating the extra coordinate, things get simpler.
    /* LOOK default we eliminate (0,0) so maybe not needed?  3/13/2015
    if (isIntervalLocal && (p1 == p2) && (p1 == 0)) {
      timeRangeIndicatorLocal = 1;
      forecastTimeLocal = 0;
      startLocal = endLocal = 0;
      isIntervalLocal = false;
    } */

    // rigamorole to keep things final
    timeRangeIndicator = timeRangeIndicatorLocal;
    isInterval = isIntervalLocal;
    start = startLocal;
    end = endLocal;
    forecastTime = forecastTimeLocal;
  }

  /**
   * Get interval [start, end] since reference time in units of timeUnit, only if  an interval.
   * @return interval [start, end]
   */
  public int[] getInterval() {
    return isInterval ? new int[]{start, end} : null;
  }

  /**
   * Get interval size (end - start) in units of timeUnit, only if an interval.
   * @return interval size
   */
  public int getIntervalSize() {
    return isInterval ? end - start : 0;
  }

  /**
   * Is this an interval time coordinate
   * @return If an interval time coordinate
   */
  public boolean isInterval() {
    return isInterval;
  }

  /**
   * Forecast time since reference time in units of timeUnit, only if not an interval.
   * @return Forecast time
   */
  public int getForecastTime() {
    return forecastTime;
  }

  /**
   * The time unit name (code table 5)
   * @return time unit name
   */
  public String getTimeTypeName() {
    return cust.getTimeTypeName(timeRangeIndicator);
  }

   /**
   * The time unit statistical type, derived from code table 5)
   * @return time unit statistical type
   */
  public GribStatType getStatType() {
    return cust.getStatType(timeRangeIndicator);
  }

  /**
   * A string representation of the time coordinate, whether its an interval or not.
   * @return string representation of the time coordinate
   */
  public String getTimeCoord() {
    if (isInterval()) {
      int[] intv = getInterval();
      return intv[0] + "-" + intv[1];
    }
    return Integer.toString(getForecastTime());
  }
}
