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
/**
 * User: rkambic
 * Date: Jun 5, 2009
 * Time: 2:52:45 PM
 */

package ucar.grib.grib2;

import ucar.grib.GribNumbers;
import ucar.grib.GribPDSVariablesIF;

import java.io.IOException;
import java.util.Calendar;
import java.util.Date;

/**
 * Representing the product definition section (PDS) of a GRIB product as variables
 * extracted from a byte[].
 * This is section 4 of a Grib record that contains information about the parameter
 */

public final class Grib2PDSVariables implements GribPDSVariablesIF {

  private static org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(Grib2PDSVariables.class);

  /**
   *   PDS as byte array.
   */
  private final byte[] input;

  /**
   * productDefinition.
   */
  private final int productDefinition;

  // *** constructors *******************************************************

  /**
   * Constructs a Grib2PDSVariables  object from a byte[].
   *
   * @param input PDS
   * @throws java.io.IOException if raf contains no valid GRIB file
   */
  public Grib2PDSVariables( byte[] input ) throws IOException {

    this.input = input;

    // octet 8-9
    productDefinition = GribNumbers.int2( getInt(7), getInt(8) );
    //System.out.println( "PDS productDefinition=" + productDefinition );
  }

  /**
   * PDS as byte[]
   */
  public byte[] getPDSBytes() {
    return input;
  }

  /**
   * calculates the increment between time intervals
   * @param tui time unit indicator,
   * @param length of interval
   * @return increment
   */
  private int calculateIncrement(int tui, int length) {
    switch (tui) {

      case 1:
        return length;
      case 10:
        return 3 * length;
      case 11:
        return 6 * length;
      case 12:
        return 12 * length;
      case 0:
      case 2:
      case 3:
      case 4:
      case 5:
      case 6:
      case 7:
      case 13:
        return length;
      default:
        return GribNumbers.UNDEFINED;
    }

  }


  // getters  Covers ProductDefinitions 0-14 first

  // octets 1-4 (Length of PDS)
  public final int getLength() {
    return GribNumbers.int4(getInt(0), getInt(1),getInt(2),getInt(3));
  }

  /**
   * octet 5
   * Number of this section, should be 4.
   */
  public final int getSection() {
    return getInt(4);
  }

  // octet 6-7
  /**
   * Number of this coordinates.
   *
   * @return Coordinates number
   */
  public final int getCoordinates() {
    return GribNumbers.int2( getInt(5), getInt(6) );
  }

  // octet 8-9
   /**
   * productDefinition.
   *
   * @return ProductDefinition
   */
  public final int getProductDefinition() {
    //return GribNumbers.int2( getInt(7), getInt(8) );
    return productDefinition;
  }

  // octet 10
   /**
   * parameter Category .
   *
   * @return parameterCategory as int
   */
  public final int getParameterCategory() {
    return getInt(9);
  }

  // octet 11
  /**
   * parameter Number.
   *
   * @return ParameterNumber
   */
  public final int getParameterNumber() {
    return  getInt( 10 );
  }

  /**
   * type of Generating Process.
   *
   * @return GenProcess
   */
  public final int getTypeGenProcess() {
    switch (productDefinition) {
      // octet 12
      case 0:
      case 1:
      case 2:
      case 3:
      case 4:
      case 5:
      case 6:
      case 7:
      case 8:
      case 9:
      case 10:
      case 11:
      case 12:
      case 13:
      case 14:
      case 20:
      case 30:
      case 31:
      case 1000:
      case 1001:
      case 1002:
      case 1100:
      case 1101: {
        return getInt(11);
      }
      // octet 14
      case 40:
      case 41:
      case 42:
      case 43: {
        return getInt(13);
      }
      default:
        return GribNumbers.UNDEFINED;

    }
  }

  /**
   * ChemicalType.
   *
   * @return ChemicalType
   */
  public final int getChemicalType() {
    switch (productDefinition) {
      // octet 12-13
      case 40:
      case 41:
      case 42:
      case 43: {
        return GribNumbers.int2( getInt(11), getInt(12) );
      }
      default:
        return GribNumbers.UNDEFINED;

    }
  }
  /**
   * backGenProcess.
   *
   * @return BackGenProcess
   */
  public final int getBackGenProcess() {
    switch (productDefinition) {
      // octet 13
      case 0:
      case 1:
      case 2:
      case 3:
      case 4:
      case 5:
      case 6:
      case 7:
      case 8:
      case 9:
      case 10:
      case 11:
      case 12:
      case 13:
      case 14:
      case 1000:
      case 1001:
      case 1002:
      case 1100:
      case 1101: {
        return getInt(12);
      }
      // octet 15
      case 40:
      case 41:
      case 42:
      case 43: {
        return getInt(14);
      }
      default:
        return GribNumbers.UNDEFINED;

    }
  }

  /**
   * ObservationProcess.
   *
   * @return ObservationProcess
   */
  public final int getObservationProcess() {
    switch (productDefinition) {
      // octet 13
      case 30:
      case 31: {
        return getInt(12);
      }
      default:
        return GribNumbers.UNDEFINED;
    }
  }

  /**
   * Number Bands for Satellite.
   *
   * @return NB
   */
  public final int getNB() {
    switch (productDefinition) {
      // octet 14
      case 30:
      case 31: {
        return getInt(13);
      }
      default:
        return GribNumbers.UNDEFINED;
    }
  }

  /**
   * SatelliteSeries.
   *
   * @return series
   */
  public final int[] getSatelliteSeries() {
    switch (productDefinition) {
      // octet (15+11(nb-1)) - (16+11(nb-1))
      case 30:
      case 31: {
        int nb = getNB();
        int[] series = new int[ nb ];
        for( int i = 0; i < nb; i++)
          series[ i ] = GribNumbers.int2( getInt( 14 +11*i ), getInt( 15 +11*i ) );
        return series;
      }
      default:
        return new int[] { GribNumbers.UNDEFINED };
    }
  }

  /**
   * Satellite number.
   *
   * @return satellite
   */
  public final int[] getSatellite() {
    switch (productDefinition) {
      // octet (17+11(nb-1)) - (18+11(nb-1)) 
      case 30:
      case 31: {
        int nb = getNB();
        int[] series = new int[ nb ];
        for( int i = 0; i < nb; i++)
          series[ i ] = GribNumbers.int2( getInt( 16 +11*i ), getInt( 17 +11*i ) );
        return series;
      }
      default:
        return new int[] { GribNumbers.UNDEFINED };
    }
  }

  /**
   * Satellite Instrument.
   *
   * @return instrument
   */
  public final int[] getSatelliteInstrument() {
    switch (productDefinition) {
      // octet (19+11(nb-1)) - (20+11(nb-1))
      case 30:
      case 31: {
        int nb = getNB();
        int[] series = new int[ nb ];
        for( int i = 0; i < nb; i++)
          series[ i ] = GribNumbers.int2( getInt( 18 +11*i ), getInt( 19 +11*i ) );
          //series[ i ] = getInt( 18 +11*i );
        return series;
      }
      default:
        return new int[] { GribNumbers.UNDEFINED };
    }
  }

  /**
   * Satellite Wave.
   *
   * @return ss2
   */
  public final float[] getSatelliteWave() {
    switch (productDefinition) {
      // octet (20+11(nb-1)) and (21+11(nb-1)) - (24+11(nb-1))
      case 30:
      case 31: {
        int nb = getNB();
        float[] wave = new float[ nb ];
        for( int i = 0; i < nb; i++) {
          //int scale = getInt( 19 +11*i );
          int scale = getInt( 20 +11*i );
          //int value = GribNumbers.int4(getInt( 20 +11*i ),getInt( 21 +11*i ),getInt( 22 +11*i ),getInt( 23 +11*i ));
          int value = GribNumbers.int4(getInt( 21 +11*i ),getInt( 22 +11*i ),getInt( 23 +11*i ),getInt( 24 +11*i ));
          wave[ i ] = (float) (((scale == 0) || (value == 0))
            ? value
            : value * Math.pow(10, -scale));
          //System.out.println( scale +" "+ value);
        }
        return wave;
      }
      default:
        return new float[] { GribNumbers.UNDEFINED };
    }
  }
  /**
   * analysisGenProcess.
   *
   * @return analysisGenProcess
   */
  public final int getAnalysisGenProcess() {
    switch (productDefinition) {
      // octet 14
      case 0:
      case 1:
      case 2:
      case 3:
      case 4:
      case 5:
      case 6:
      case 7:
      case 8:
      case 9:
      case 10:
      case 11:
      case 12:
      case 13:
      case 14:
      case 1000:
      case 1001:
      case 1002:
      case 1100:
      case 1101: {
        return getInt(13);
      }
      // octet 16
      case 40:
      case 41:
      case 42:
      case 43: {
        return getInt(15);
      }
      default:
        return GribNumbers.UNDEFINED;

    }
  }

  /**
   * hoursAfter.
   *
   * @return HoursAfter
   */
  public final int getHoursAfter() {
    switch (productDefinition) {
      // octet 15-16
      case 0:
      case 1:
      case 2:
      case 3:
      case 4:
      case 5:
      case 6:
      case 7:
      case 8:
      case 9:
      case 10:
      case 11:
      case 12:
      case 13:
      case 14:
      case 1000:
      case 1001:
      case 1002:
      case 1100:
      case 1101: {
        return GribNumbers.int2( getInt(14), getInt(15) );
      }
      // octet 17-18
      case 40:
      case 41:
      case 42:
      case 43: {
        return GribNumbers.int2( getInt(16), getInt(17) );
      }
      default:
        return GribNumbers.UNDEFINED;

    }
  }

  /**
   * minutesAfter.
   *
   * @return MinutesAfter
   */
  public final int getMinutesAfter() {
    switch (productDefinition) {
      // octet 17
      case 0:
      case 1:
      case 2:
      case 3:
      case 4:
      case 5:
      case 6:
      case 7:
      case 8:
      case 9:
      case 10:
      case 11:
      case 12:
      case 13:
      case 14:
      case 1000:
      case 1001:
      case 1002:
      case 1100:
      case 1101: {
        return getInt(16);
      }
      // octet 19
      case 40:
      case 41:
      case 42:
      case 43: {
        return getInt(18);
      }
      default:
        return GribNumbers.UNDEFINED;

    }
  }

  /**
   * returns timeRangeUnit .
   *
   * @return TimeRangeUnitName
   */
  public final int getTimeRangeUnit() {
    switch (productDefinition) {
      // octet 18
      case 0:
      case 1:
      case 2:
      case 3:
      case 4:
      case 5:
      case 6:
      case 7:
      case 8:
      case 9:
      case 10:
      case 11:
      case 12:
      case 13:
      case 14:
      case 1000:
      case 1001:
      case 1002:
      case 1100:
      case 1101: {
        return getInt(17);
      }
      // octet 14
      case 20: {
        return getInt(13);
      }
      // octet 20
      case 40:
      case 41:
      case 42:
      case 43: {
        return getInt(19);
      }
      default:
        return GribNumbers.UNDEFINED;

    }
  }

  /**
   * forecastTime.
   *
   * @return ForecastTime
   */
  public final int getForecastTime() {

    int length =  getLength();
    int forecast1 = GribNumbers.UNDEFINED, forecast2 = GribNumbers.UNDEFINED;
    // octet 19-22
    if( length > 21 )
        forecast1 = GribNumbers.int4(getInt(18), getInt(19),getInt(20), getInt(21))
            * calculateIncrement( getTimeRangeUnit(), 1 );
    // octet 21-24
    if( length > 23 )
      forecast2 =  GribNumbers.int4(getInt(20), getInt(21),getInt(22), getInt(23))
            * calculateIncrement( getTimeRangeUnit(), 1 );

    switch (productDefinition) {
      // octet 19-22
      case 0:
      case 1:
      case 2:
      case 3:
      case 4:
      case 5:
      case 6:
      case 7:
      case 1000:
      case 1001:
      case 1002:
      case 1100:
      case 1101: {
        return forecast1;
      }
      // octet 42
      case 8: return calculateForecast( 41, forecast1 );
      // octet 55
      case 9: return calculateForecast( 54, forecast1 );
      // octet 43
      case 10: return calculateForecast( 42, forecast1 );
      // octet 45
      case 11: return calculateForecast( 44, forecast1 );
      // octet 44
      case 12: return calculateForecast( 43, forecast1 );
      // octet 76
      case 13: return calculateForecast( 75, forecast1 );
      // octet 72
      case 14: return calculateForecast( 71, forecast1 );
      // octet 21-24
      case 40:
      case 41: {
        return GribNumbers.int4(getInt(20), getInt(21),getInt(22), getInt(23))
            * calculateIncrement( getTimeRangeUnit(), 1 );
      }
      case 30: // to make backwards compatible
      case 31: {
        return 0;
      }
      // octet 44
      case 42: return calculateForecast( 43, forecast2 );
      // octet 47
      case 43: return calculateForecast( 46, forecast2 );

      default:
        return GribNumbers.UNDEFINED;

    }
  }

  /**
   * calculateForecast.
   * @param idx where to start in the byte[]
   * @param forecastTime initial forecast time
   * @return calculateForecast as int
   */
  public final int calculateForecast( int idx, int forecastTime ) {

    // 42 for Product Definition 8
    int timeRanges = getInt( idx++ );
    // 43 - 46
    int missingDataValues = GribNumbers.int4(getInt(idx++), getInt(idx++),getInt(idx++), getInt(idx++));

    int[] timeIncrement = new int[timeRanges * 6];
    for (int t = 0; t < timeRanges; t++) {
      // 47 statProcess
      timeIncrement[t*6] = getInt(idx++);
      // 48  timeType
      timeIncrement[t*6 + 1] = getInt(idx++);
      // 49   time Unit
      timeIncrement[t*6 + 2] = getInt(idx++);
      // 50 - 53 timeIncrement
      timeIncrement[t*6 + 3] = GribNumbers.int4(getInt(idx++), getInt(idx++),getInt(idx++), getInt(idx++));
      // 54 time Unit
      timeIncrement[t*6 + 4] = getInt(idx++);
      // 55 - 58 timeIncrement
      timeIncrement[t*6 + 5] = GribNumbers.int4(getInt(idx++), getInt(idx++),getInt(idx++), getInt(idx++));
    }
    // modify forecast time to reflect the end of the
    // interval according to timeIncrement information.
    // http://www.nco.ncep.noaa.gov/pmb/docs/grib2/grib2_temp4-8.shtml starting byte 42
    if  ( timeRanges == 1 ) {
      forecastTime += calculateIncrement(timeIncrement[2], timeIncrement[3]);
    } else if (timeRanges == 2 ) {
      // sample data is Monthly Climate Forecast System Reanalysis(CFSR) data
      // check statistical processing and if the time units are the same
      // http://www.nco.ncep.noaa.gov/pmb/docs/grib2/grib2_table4-10.shtml
      if ( timeIncrement[ 0 ] == 194 && timeIncrement[ 2 ] == timeIncrement[ 4 ]) {
        int len = timeIncrement[ 3 ] * timeIncrement[ 5 ];
        forecastTime += calculateIncrement(timeIncrement[2], len );
      } else {
        log.error( "Grib2PDSVariable: time ranges = 2 with not equal time units not implemented");
        forecastTime = GribNumbers.UNDEFINED;
      }
    } else { // throw flag
      log.error( "Grib2PDSVariable: time ranges > 2 not implemented");
      forecastTime = GribNumbers.UNDEFINED;
    }
    return forecastTime;
  }

  /**
   * forecastTimeInterval for accumulation type variables.
   *
   * @return ForecastTimeInterval int[]  start, end of interval
   */
  public final int[] getForecastTimeInterval() {

    int length =  getLength();
    int forecast1 = GribNumbers.UNDEFINED, forecast2 = GribNumbers.UNDEFINED;
    // octet 19-22
    if( length > 21 )
        forecast1 = GribNumbers.int4(getInt(18), getInt(19),getInt(20), getInt(21))
            * calculateIncrement( getTimeRangeUnit(), 1 );
    // octet 21-24
    if( length > 23 )
      forecast2 =  GribNumbers.int4(getInt(20), getInt(21),getInt(22), getInt(23))
            * calculateIncrement( getTimeRangeUnit(), 1 );

    int[] interval = new int[ 2 ];
    interval[ 0 ] = forecast1;
    switch (productDefinition) {

      // octet 42
      case 8:
        interval[ 1 ] = calculateForecast( 41, forecast1 );
        return interval;
      // octet 55
      case 9:
        interval[ 1 ] = calculateForecast( 54, forecast1 );
        return interval;
      // octet 43
      case 10:
        interval[ 1 ] = calculateForecast( 42, forecast1 );
        return interval;
      // octet 45
      case 11:
        interval[ 1 ] = calculateForecast( 44, forecast1 );
        return interval;
      // octet 44
      case 12:
        interval[ 1 ] = calculateForecast( 43, forecast1 );
        return
            interval;
      // octet 76
      case 13:
        interval[ 1 ] = calculateForecast( 75, forecast1 );
        return interval;
      // octet 72
      case 14:
        interval[ 1 ] = calculateForecast( 71, forecast1 );
        return interval;
      // octet 44
      case 42:
        interval[ 1 ] = calculateForecast( 43, forecast2 );
        interval[ 0 ] = forecast2;
        return interval;
      // octet 47
      case 43:
        interval[ 1 ] = calculateForecast( 46, forecast2 );
        interval[ 0 ] = forecast2;
        return interval;

      default:
        return new int[] {GribNumbers.UNDEFINED, GribNumbers.UNDEFINED};

    }
  }

 /**
 * End Time Interval for productDefinition 8-14, 42 and 43 type variables.
 *
 * @return EndTimeInterval long
 */
  public final long getEndTimeInterval() {

    int idx;
    switch (productDefinition) {

      // octets 35-41
      case 8:
        idx = 34;
        break;
      // octet 48
      case 9:
        idx = 47;
        break;
      // octet 36
      case 10:
        idx = 35;
        break;
      // octet 38
      case 11:
        idx = 37;
        break;
      // octet 37
      case 12:
        idx = 36;
        break;
      // octet 69
      case 13:
        idx = 68;
        break;
      // octet 65
      case 14:
        idx = 64;
        break;
      // octet 37
      case 42:
        idx = 36;
        break;
      // octet 40
      case 43:
        idx = 39;
        break;
      default:
        return  GribNumbers.UNDEFINED ;
    }

    int year = GribNumbers.int2( getInt(idx++), getInt(idx++) );
    int month = getInt(idx++) -1;
    int day = getInt(idx++);
    int hour = getInt(idx++);
    int minute = getInt(idx++);
    int second = getInt(idx++);
    Calendar calendar = Calendar.getInstance();
    calendar.clear();
    calendar.setTimeZone(java.util.TimeZone.getTimeZone("GMT"));
    calendar.set(Calendar.DST_OFFSET, 0);
    calendar.set(year, month, day, hour, minute, second);

    //System.out.println( "End of Time Interval from octets 35-41 "+ calendar.getTime() );
    long refTime = calendar.getTimeInMillis();

    return refTime;
  }

  /**
   * typeFirstFixedSurface.
   *
   * @return FirstFixedSurface as int
   */
  public final int getTypeFirstFixedSurface() {
    switch (productDefinition) {
      // octet 23
      case 0:
      case 1:
      case 2:
      case 3:
      case 4:
      case 5:
      case 6:
      case 7:
      case 8:
      case 9:
      case 10:
      case 11:
      case 12:
      case 13:
      case 14:
      case 1100:
      case 1101: {
        return getInt(22);
      }
      case 30: // to make backwards compatible
      case 31: {
        return 0;
      }
      // octet 25
      case 40:
      case 41:
      case 42:
      case 43: {
        return getInt(24);
      }
      default:
        return GribNumbers.UNDEFINED;

    }
  }

  /**
   * FirstFixedSurfaceValue
   * @return float FirstFixedSurfaceValue
   */
  public float getValueFirstFixedSurface() {
     int scaleFirstFixedSurface, valueFirstFixedSurface;
     switch (productDefinition) {
      // octet 24-28
      case 0:
      case 1:
      case 2:
      case 3:
      case 4:
      case 5:
      case 6:
      case 7:
      case 8:
      case 9:
      case 10:
      case 11:
      case 12:
      case 13:
      case 14:
      case 1100:
      case 1101: {
        // octet 24
        scaleFirstFixedSurface = getInt(23);
        // octet 25-28
        valueFirstFixedSurface = GribNumbers.int4(getInt(24), getInt(25),getInt(26), getInt(27));
        break;
      }
      // octet 26-30
      case 40:
      case 41:
      case 42:
      case 43: {
        scaleFirstFixedSurface = getInt(25);
        // octet 27-30
        valueFirstFixedSurface = GribNumbers.int4(getInt(26), getInt(27),getInt(28), getInt(29));
        break;
      }
      default:
        return GribNumbers.UNDEFINED;

    }
    return (float) (((scaleFirstFixedSurface == 0) || (valueFirstFixedSurface == 0))
            ? valueFirstFixedSurface
            : valueFirstFixedSurface
            * Math.pow(10, -scaleFirstFixedSurface));
  }

  /**
   * typeSecondFixedSurface.
   *
   * @return SecondFixedSurface as int
   */
  public final int getTypeSecondFixedSurface() {
    switch (productDefinition) {
      // octet 29
      case 0:
      case 1:
      case 2:
      case 3:
      case 4:
      case 5:
      case 6:
      case 7:
      case 8:
      case 9:
      case 10:
      case 11:
      case 12:
      case 13:
      case 14:
      case 1100:
      case 1101: {
        return getInt(28);
      }
      case 30: // to make backwards compatible
      case 31: {
        return 0;
      }
      // octet 31
      case 40:
      case 41:
      case 42:
      case 43: {
        //return getInt(30);
        return getInt(30);
      }
      default:
        return GribNumbers.UNDEFINED;

    }
  }

  /**
   * SecondFixedSurfaceValue
   * @return float SecondFixedSurfaceValue
   */
  public float getValueSecondFixedSurface() {
     int scaleSecondFixedSurface, valueSecondFixedSurface;
     switch (productDefinition) {
      // octet 30-34
      case 0:
      case 1:
      case 2:
      case 3:
      case 4:
      case 5:
      case 6:
      case 7:
      case 8:
      case 9:
      case 10:
      case 11:
      case 12:
      case 13:
      case 14:
      case 1100:
      case 1101: {
        // octet 30
        scaleSecondFixedSurface = getInt(29);
        // octet 31-34
        valueSecondFixedSurface = GribNumbers.int4(getInt(30), getInt(31),getInt(32), getInt(33));
        break;
      }
      // octet 32-36
      case 40:
      case 41:
      case 42:
      case 43: {
        scaleSecondFixedSurface = getInt(31);
        // octet 33-36
        valueSecondFixedSurface = GribNumbers.int4(getInt(32), getInt(33),getInt(34), getInt(35));
        break;
      }
      default:
        return GribNumbers.UNDEFINED;

    }
    return (float) (((scaleSecondFixedSurface == 0) || (valueSecondFixedSurface == 0))
            ? valueSecondFixedSurface
            : valueSecondFixedSurface
            * Math.pow(10, -scaleSecondFixedSurface));
  }

 /**
   * Ensemble type data.
   *
   * @return ensemble type of data
   */
  public final boolean isEnsemble() {
    switch (getTypeGenProcess()) {
      case 4:
      case 5:
      case 10:
      case 193: {
        return true;
      }
      default:
        return false;
    }
  }
  
  /**
   * Type of Derived   Code table 4.7.
   * Type of Ensemble  code table 4.6
   * Type of Probability  code table 4.9
   *
   * @return int Type Derived, Ensemble, or Probability
   */
  public final int getType () {
    switch (productDefinition) {
      // octet 35
      case 1:
      case 2:
      case 3:
      case 4:
      case 11:
      case 12:
      case 13:
      case 14: {
        return getInt(34);
      }
      // octet 37
      case 5:
      case 9:
      case 41:
      case 43: {
        return getInt(36);
      }
      default:
        return GribNumbers.UNDEFINED;

    }
  }
  /**
   * ForecastProbability.
   *
   * @return int ForecastProbability
   */
  public final int getForecastProbability() {
    switch (productDefinition) {
      // octet 35
      case 5:
      case 9: {
        return getInt(34);
      }
      default:
        return GribNumbers.UNDEFINED;

    }
  }

  /**
   * ForecastPercentile.
   *
   * @return int ForecastPercentile
   */
  public final int getForecastPercentile() {
    switch (productDefinition) {
      // octet 35
      case 6:
      case 10: {
        return getInt(34);
      }
      default:
        return GribNumbers.UNDEFINED;

    }
  }

  /**
   * Perturbation number
   * @return int Perturbation
   */
  public final int getPerturbation() {
    switch (productDefinition) {
      // octet 36
      case 1:
      case 11: {
        return getInt(35);
      }
      // octet 38
      case 41:
      case 43: {
        return getInt(37);
      }
      default:
        return GribNumbers.UNDEFINED;

    }
  }

  /**
   * number of forecasts.
   *
   * @return int
   */
  public final int getNumberForecasts() {
    switch (productDefinition) {
      // octet 36
      case 2:
      case 3:
      case 4:
      case 5:
      case 9:
      case 12:
      case 13:
      case 14: {
        return getInt(35);
      }
      // octet 37
      case 1:
      case 11: {
        return getInt(36);
      }
      // octet 39
      case 41:
      case 43: {
        return getInt(38);
      }
      default:
        return GribNumbers.UNDEFINED;

    }
  }

  /**
   * ValueLowerLimit
   * @return float ValueLowerLimit
   */
  public final float getValueLowerLimit() {
     int scaleLowerLimit, valueLowerLimit;
     switch (productDefinition) {
      // octet 38-42
      case 5:
      case 9: {
        // octet 38
        scaleLowerLimit = getInt(37);
        // octet 39-42
        valueLowerLimit = GribNumbers.int4(getInt(38), getInt(39),getInt(40), getInt(41));
        break;
      }
      default:
        return GribNumbers.UNDEFINED;

    }
    return (float) (((scaleLowerLimit == 0) || (valueLowerLimit == 0))
            ? valueLowerLimit
            : valueLowerLimit * Math.pow(10, -scaleLowerLimit));
  }

  /**
   * ValueUpperLimit
   * @return float ValueUpperLimit
   */
  public final float getValueUpperLimit() {
     int scaleUpperLimit, valueUpperLimit;
     switch (productDefinition) {
      // octet 43-47
      case 5:
      case 9: {
        // octet 43
        scaleUpperLimit = getInt(42);
        // octet 44-47
        valueUpperLimit = GribNumbers.int4(getInt(43), getInt(44),getInt(45), getInt(46));
        break;
      }
      default:
        return GribNumbers.UNDEFINED;

    }
    return (float) (((scaleUpperLimit == 0) || (valueUpperLimit == 0))
            ? valueUpperLimit
            : valueUpperLimit * Math.pow(10, -scaleUpperLimit));
  }

  /**
   * Get the indexth byte as an int.
   * @param index in the byte[]
   * @return int  byte as int
   */
  private final int getInt( int index ) {
    return input[ index ] & 0xff;
  }

  public int getIntervalStatType() {
    int byteOffset;
    switch (productDefinition) {
      case 8:
        byteOffset = 46; // octet 47
        break;

      case 9:
        byteOffset = 59; // octet 60
        break;

      case 11:
        byteOffset = 49; // octet 50
        break;

      case 10:
      case 12:
        byteOffset = 48; // octet 49
        break;

      case 13:
         byteOffset = 80; // octet 81
         break;

      case 14:
         byteOffset = 76; // octet 77
         break;

      default:
        return -1;
    }

    return getInt(byteOffset);
  }

}
