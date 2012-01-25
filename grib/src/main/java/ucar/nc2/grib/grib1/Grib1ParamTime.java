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
import ucar.nc2.grib.grib1.tables.Grib1WmoTimeType;

/**
 * Time coordinate from the PDS.
 * Process information from GRIB-1 Table 4: "Forecast time unit"
 * Process information from GRIB-1 Table 5: "Time range indicator"
 *
 * @author John
 * @since 9/4/11
 */
@Immutable
public class Grib1ParamTime {
  private final Grib1Customizer cust;

  private final int timeRangeIndicator; // code Table 5 (octet 21)
  private final int p1, p2; // octet 19 and 20
  private final boolean isInterval;
  private final int start;
  private final int end;
  private final int forecastTime;

  /**
   * Handles GRIB-1 code table 5 : "Time range indicator".
   *
   * @param cust customizer
   * @param pds the Grib1SectionProductDefinition
   */
  public Grib1ParamTime(Grib1Customizer cust, Grib1SectionProductDefinition pds) {
    this.cust = cust;

    timeRangeIndicator = pds.getTimeRangeIndicator();
    p1 = pds.getTimeValue1();
    p2 = pds.getTimeValue2();
    int n = pds.getNincluded();

    switch (timeRangeIndicator) {

      /*Forecast product valid for reference time + P1 (P1 > 0), or
        Uninitialized analysis product for reference time (P1 = 0), or
        Image product for reference time (P1 = 0) */
      case 0:
        forecastTime = p1;
        start = end = 0;
        isInterval = false;
        break;

      // Initialized analysis product for reference time (P1 = 0)
      case 1:
        forecastTime = 0;
        start = end = 0;
        isInterval = false;
        break;

      // Product with a valid time ranging between reference time + P1 and reference time + P2
      case 2:
        start = p1;
        end = p2;
        forecastTime = 0;
        isInterval = true;
        break;

      // Average (reference time + P1 to reference time + P2)
      case 3:
        start = p1;
        end = p2;
        forecastTime = 0;
        isInterval = true;
        break;

      /* Accumulation  (reference  time  +  P1  to  reference  time  +  P2)  product  considered  valid  at reference time + P2 */
      case 4:
        start = p1;
        end = p2;
        forecastTime = 0;
        isInterval = true;
        break;

      /* Difference  (reference  time  +  P2  minus  reference  time  +  P1)  product  considered  valid  at reference time + P2 */
      case 5:
        start = p1;
        end = p2;
        forecastTime = 0;
        isInterval = true;
        break;

      // Average (reference time - P1 to reference time - P2)
      case 6:
        start = -p1;
        end = -p2;
        forecastTime = 0;
        isInterval = true;
        break;

      // Average (reference time - P1 to reference time + P2)
      case 7:
        start = -p1;
        end = p2;
        forecastTime = 0;
        isInterval = true;
        break;

      // P1 occupies octets 19 and 20; product valid at reference time + P1
      case 10:
        forecastTime = GribNumbers.int2(p1, p2);
        start = end = 0;
        isInterval = false;
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
        forecastTime = p2;
        start = end = 0;
        isInterval = false;
        break;

      /* Average  of  N  forecasts  (or  initialized  analyses);  each  product  has  forecast  period  of  P1
        (P1 = 0 for initialized analyses); products have reference times at intervals of P2, beginning
        at the given reference time */
      case 113:
        start = 0;
        end = p1 + n * p2;  // LOOK might be n-1 ??
        forecastTime = 0;
        isInterval = true;
        break;

      /* Accumulation of N forecasts (or initialized analyses); each product has forecast period of
        P1  (P1  =  0  for  initialized  analyses);  products  have  reference  times  at  intervals  of  P2,
        beginning at the given reference time */
      case 114:
        start = 0;
        end = p1 + n * p2;
        forecastTime = 0;
        isInterval = true;
        break;

      /* Average of N forecasts, all with the same reference time; the first has a forecast period of
         P1, the remaining forecasts follow at intervals of P2 */
      case 115:
        start = 0;
        end = p1 + n * p2;
        forecastTime = 0;
        isInterval = true;
        break;

      /* Accumulation  of  N  forecasts,  all  with  the  same  reference  time;  the  first  has  a  forecast
        period of P1, the remaining forecasts follow at intervals of P2 */
      case 116:
        start = 0;
        end = p1 + n * p2;
        forecastTime = 0;
        isInterval = true;
        break;

      /* Average of N forecasts; the first has a forecast period of P1, the subsequent ones have
        forecast periods reduced from the previous one by an interval of P2; the reference time for
        the first is given in octets 13 to 17, the subsequent ones have reference times increased
        from the previous one by an interval of P2. Thus all the forecasts have the same valid time,
        given by the initial reference time + P1 */
      case 117:
        start = 0;
        end = p1;
        forecastTime = 0;
        isInterval = true;
        break;

      /* Temporal  variance,  or  covariance,  of  N  initialized  analyses;  each  product  has  forecast
        period of P1 = 0; products have reference times at intervals of P2, beginning at the given
        reference time */
      case 118:
        start = 0;
        end = n * p2;
        forecastTime = 0;
        isInterval = true;
        break;

      /* Standard deviation of N forecasts, all with the same reference time with respect to the time
        average of forecasts; the first forecast has a forecast period of P1, the remaining forecasts
        follow at intervals of P2 */
      case 119:
        start = p1;
        end = p1 + n * p2;
        forecastTime = 0;
        isInterval = true;
        break;

      // Average of N uninitialized analyses, starting at the reference time, at intervals of P2
      case 123:
        start = 0;
        end = n * p2;
        forecastTime = 0;
        isInterval = true;
        break;

      // Accumulation of N uninitialized analyses, starting at the reference time, at intervals of P2
      case 124:
        start = 0;
        end = n * p2;
        forecastTime = 0;
        isInterval = true;
        break;

      /* Standard deviation of N forecasts, all with the same reference time with respect to time
        average of the time tendency of forecasts; the first forecast has a forecast period of P1,
        the remaining forecasts follow at intervals of P2 */
      case 125:
        start = 0;
        end = p1 + n * p2;
        forecastTime = 0;
        isInterval = true;
        break;

      default:
        throw new IllegalArgumentException("PDS: Unknown Time Range Indicator " + timeRangeIndicator);
    }

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
