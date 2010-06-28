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
 * Date: Jun 11, 2009
 * Time: 3:24:50 PM
 */

package ucar.grib.grib1;

import ucar.grib.GribNumbers;
import ucar.grib.GribPDSVariablesIF;

import java.io.IOException;
import java.util.Date;
import java.util.Calendar;

/**
 * A class representing the product definition section (PDS) of a GRIB product.
 * This is section 1 of a Grib record that contains information about the parameter
 */

public final class Grib1PDSVariables implements GribPDSVariablesIF {

  private static org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(Grib1PDSVariables.class);

  /**
   * PDS as byte array.
   */
  private final byte[] input;

  /**
   * Length in bytes of this PDS.
   */
  private final int length;

  // *** constructors *******************************************************

  /**
   * Constructs Grib1PDSVariables from a byte[].
   *
   * @param input PDS
   * @throws java.io.IOException byte[] read
   */
  public Grib1PDSVariables(byte[] input) throws IOException {

    this.input = input;
    this.length = GribNumbers.int3(getInt(0), getInt(1), getInt(2));
  }

  // getters

  /**
   * PDS as byte[]
   */
  public byte[] getPDSBytes() {
    return input;
  }

  // octets 1-3 (Length of PDS)

  public final int getLength() {
    return length;
  }

  /**
   * Number PDS section .
   */
  public final int getSection() {
    return 1;
  }

  // octet 4
  /**
   * gets the Table version as a int.
   *
   * @return table_version
   */
  public final int getTableVersion() {
    return getInt(3);
  }

  // octet 5
  /**
   * Center as int.
   *
   * @return center_id
   */
  public final int getCenter() {
    return getInt(4);
  }

  // octet 6
  /**
   * Process Id as int.
   *
   * @return typeGenProcess
   */
  public final int getTypeGenProcess() {
    return getInt(5);
  }

  // octet 7
  /**
   * Grid ID as int.
   *
   * @return grid_id
   */
  public final int getGrid_Id() {
    return getInt(6);
  }

  // octet 8
  /**
   * Check if GDS exists.
   *
   * @return true, if GDS exists
   */
  public final boolean gdsExists() {
    return (getInt(7) & 128) == 128;
  }

  /**
   * Check if BMS exists.
   *
   * @return true, if BMS exists
   */
  public final boolean bmsExists() {
    return (getInt(7) & 64) == 64;
  }

  /**
   * parameter Category .
   *
   * @return parameterCategory as int
   */
  public final int getParameterCategory() {
    return -1;
  }

  // octet 9
  /**
   * Get the number of the parameter.
   *
   * @return index number of parameter in table
   */
  public final int getParameterNumber() {
    return getInt(8);
  }

  //octet 10
  /**
   * Get the numeric type for 1st level.
   *
   * @return type of level (height or pressure)
   */
  public final int getTypeFirstFixedSurface() {
    return getInt(9);
  }

  // octet 11-12
  /**
   * Get the numeric value for this level.
   *
   * @return int level value
   */
  public final float getValueFirstFixedSurface() {
    GribPDSLevel level = new GribPDSLevel(getInt(9), getInt(10), getInt(11));
    return level.getValue1();
  }

  /**
   * Get the numeric type for 2nd level.
   *
   * @return type of level always 255, Grib1 does't have type 2nd level
   */
  public final int getTypeSecondFixedSurface() {
    return 255;
  }

  // octet 11-12
  /**
   * Get value 2 (if it exists) for this level.
   *
   * @return int level value
   */
  public final float getValueSecondFixedSurface() {
    GribPDSLevel level = new GribPDSLevel(getInt(9), getInt(10), getInt(11));
    return level.getValue2();
  }

  /**
   * Get the base (analysis) time of the forecast.
   *
   * @return date of basetime
   */
  public final Date getBaseTime() {
    // octet 25
    int century = getInt(24) - 1;
    if (century == -1) century = 20;
    // octets 13-17 (base time for reference time)
    int year = getInt(12);
    int month = getInt(13);
    int day = getInt(14);
    int hour = getInt(15);
    int minute = getInt(16);

    Calendar calendar = Calendar.getInstance();
    calendar.clear();
    calendar.setTimeZone(java.util.TimeZone.getTimeZone("GMT"));
    calendar.set(Calendar.DST_OFFSET, 0);
    calendar.set((century * 100 + year), month - 1, day, hour, minute, 0);

    return calendar.getTime();
  }

  /**
   * gets reference time as a long millis.
   *
   * @return refTime
   */
  public final long getRefTime() {
    // octet 25
    int century = getInt(24) - 1;
    if (century == -1) century = 20;
    // octets 13-17 (base time for reference time)
    int year = getInt(12);
    int month = getInt(13);
    int day = getInt(14);
    int hour = getInt(15);
    int minute = getInt(16);

    Calendar calendar = Calendar.getInstance();
    calendar.clear();
    calendar.setTimeZone(java.util.TimeZone.getTimeZone("GMT"));
    calendar.set(Calendar.DST_OFFSET, 0);
    calendar.set((century * 100 + year), month - 1, day, hour, minute, 0);

    return calendar.getTimeInMillis();
  }

  // octet 18 Forecast time unit
  /**
   * @return int time unit index
   */
  public final int getTimeRangeUnit() {
    return getInt(17);
  }

  // octet 19  used to create Forecast time
  /**
   * P1.
   *
   * @return p1
   */
  public final int getP1() {
    if( getTimeRange() == 10 )
       return GribNumbers.int2(getInt(18), getInt(19));
    return getInt(18);
  }

  // octet  20 used to create Forecast time
  /**
   * P2.
   *
   * @return p2
   */
  public final int getP2() {
    if( getTimeRange() == 10 )
       return 0;
    return getInt(19);
  }

  // octet  21
  /**
   * TimeRange as int.
   *
   * @return timeRangeValue
   */
  public final int getTimeRange() {
    return getInt(20);
  }

  /**
   * ProductDefinition
   * Since Grib1 doesn't have a Product Definition, use the ime range. This is
   * subjective but works.
   *
   * @return ProductDefinition
   */
  public final int getProductDefinition() {
    return getTimeRange();
  }

  /**
   * Get the time of the forecast.
   *
   * @return date and time
   */
  public final int getForecastTime() {
    // forecast time is always at the end of the range
    switch (getTimeRange()) {

      case 0:
        return getP1();

      case 1:
        return 0;

      case 2:
        return getP2();

      case 3:
        return getP2();

      case 4:
        return getP2();

      case 5:
        return getP2();

      case 6:
        return -getP2();

      case 7:
        return getP2();

      case 10:
        // p1 really consists of 2 bytes p1 and p2
        //return GribNumbers.int2(getP1(), getP2());
        return getP1();
      
      case 51:
        return getP2();

      case 113:
        return getP1();

      default:
        log.error("PDS: Time Range Indicator "
            + getTimeRange() + " is not yet supported");
    }
    return GribNumbers.UNDEFINED;
  }

   /**
   * Get the time interval of the forecast.
   *
   * @return interval as int[2]
   */
  public int[] getForecastTimeInterval() {
     int[] interval = new int[ 2 ];
     interval[ 0 ] = getP1();
     interval[ 1 ] = getP2();
     return interval;
  }

  /**  // TODO: move to Grib1GridTableLookup
   * TimeRange as String.
   *
   * @return timeRange
   */
  public final String getTimeRangeString() {
    switch (getTimeRange()) {

      case 0:
        return "product valid at RT + P1";

      case 1:
        return "product valid for RT, P1=0";

      case 2:
        return "product valid from (RT + P1) to (RT + P2)";

      case 3:
        return "product is an average between (RT + P1) to (RT + P2)";

      case 4:
        return "product is an accumulation between (RT + P1) to (RT + P2)";

      case 5:
        return "product is the difference (RT + P2) - (RT + P1)";

      case 6:
        return "product is an average from (RT - P1) to (RT - P2)";

      case 7:
        return "product is an average from (RT - P1) to (RT + P2)";

      case 10:
        return "product valid at RT + P1";
        // p1 really consists of 2 bytes p1 and p2

      case 51:
        return "mean value from RT to (RT + P2)";

      case 113:
        return "Average of N forecasts, forecast period of P1, reference intervals of P2";

      default:
        log.error("PDS: Time Range Indicator "
            + getTimeRange() + " is not yet supported");
    }
    return "";
  }

  /*
   * Get Grib1 statistical processing by using Grib1 Table 5 TIME RANGE INDICATOR as a reference.
   * Since Grib1 and Grib2 use different tables, convert the Grib1 value to the Grib2
   * value so GribGridRecord.getIntervalTypeName() returns the correct IntervalStatType.
   *
   */
  public int getIntervalStatType() {
    switch (getTimeRange()) {
      // average
      case 3:
      case 6:
      case 7:
      case 113:
      case 115:
      case 117:
      case 123:
        return 0;

      // accumulation
      case 4:
      case 114:
      case 116:
      case 124:
        return 1;

      // difference
      case 5:
        return 4;

      // climatological mean
      case 51:
        return 0; //TODO: check

      // covariance
      case 118:
        return 7;

      // standard deviation
      case 119:
      case 125:
        return 7;

      default:
        return -1;
    }

  }

  // octet 22 & 23
  /**
   * AvgInclude as int.
   *
   * @return AvgInclude
   */
  public final int getAvgInclude() {
    return GribNumbers.int2(getInt(21), getInt(22));
  }

  // octet 24
  /**
   * getAvgMissing as int.
   *
   * @return getAvgMissing
   */
  public final int getAvgMissing() {
    return getInt(23);
  }

  // octet 26
  /**
   * SubCenter as int.
   *
   * @return subCenter
   */
  public final int getSubCenter() {
    return getInt(25);
  }

  // octets 27-28 (decimal scale factor)
  /**
   * Get the exponent of the decimal scale used for all data values.
   *
   * @return exponent of decimal scale
   */
  public final int getDecimalScale() {
    return GribNumbers.int2(getInt(26), getInt(27));
  }

  // octet 41 id's ensemble
  // Ensemble processing
  /**
   * NCEP Appendix C Manual 388
   * states that if the PDS is > 28 bytes and octet 41 == 1
   * then it's ensemble an product.
   *
   * @return
   */
  public final boolean isEnsemble() {
    return (length > 40 && getInt(40) != 0);
  }
  public final int getExtension() {
    if (length > 40 ) {
      return  getInt(40);
    } else {
      return GribNumbers.UNDEFINED;
    }
  }
  // octet 42 Type
  /**
   * type of ensemble
   *
   * @return type
   */
  public final int getType() {
    switch (getCenter()) {
      case 7:
      case 8:
      case 9: {
        if (length > 41 ) {
          return getInt(41);
        } else {
          return GribNumbers.UNDEFINED;
        }
      }
      // octet 43   ECWMF
      case 98: {
        if (length > 42 ) {
          return getInt(42);
        } else {
          return GribNumbers.UNDEFINED;
        }
      }
      default:
        return GribNumbers.UNDEFINED;
    }
  }
  // octet 42   ECWMF
  /**
   * Class
   *
   * @return Class
   */
  public final int getEcmwfClass() {
    switch (getCenter()) {
      case 98: {
        if (length > 41 ) {
          return getInt(41);
        } else {
          return GribNumbers.UNDEFINED;
        }
      }
      default:
        return GribNumbers.UNDEFINED;
    }
  }

  // octet 43 Identification number
  /**
   * ID of ensemble
   *
   * @return ID
   */
  public final int getID() {
    switch (getCenter()) {
      case 7:
      case 8:
      case 9: {
      if (length > 42 ) {
        return getInt(42);
      } else {
        return GribNumbers.UNDEFINED;
      }
    }
      default:
        return GribNumbers.UNDEFINED;
    }
  }

  // octets 44-45
  /**
   * Stream.
   *
   * @return Stream.
   */
  public final int getStream() {
    return GribNumbers.int2(getInt(43), getInt(44));
  }
  
  // octet 44 Product Identifier
  /**
   * Product of ensemble
   *
   * @return ID
   */
  public final int getProductID() {
    if (length > 43 ) {
      return getInt(43);
    } else {
      return GribNumbers.UNDEFINED;
    }
  }

  // octet 45 Spatial Identifier or Probability
  /**
   * Spatial Identifier or Probability of ensemble
   *
   * @return ID
   */
  public final int getSpatialorProbability() {
    if (length > 44 ) {
      return getInt(44);
    } else {
      return GribNumbers.UNDEFINED;
    }
  }

  // octet 46 Probability product definition
  /**
   * Product of ensemble
   *
   * @return ID
   */
  public final int getProbabilityProduct() {
    if (length > 45 &&
        ((getParameterNumber() == 191) || (getParameterNumber() == 192))) {
      // octet 46 Probability product definition
      return getInt(45);
    } else {
      return GribNumbers.UNDEFINED;
    }
  }

  // octet 47 Probability type
  /**
   * Product type of probability
   *
   * @return ID
   */
  public final int getProbabilityType() {
    if (length > 46 &&
        ((getParameterNumber() == 191) || (getParameterNumber() == 192))) {
      return getInt(46);
    } else {
      return GribNumbers.UNDEFINED;
    }
  }

  /**
   * Octet 50
   *
   * @return int octet 50
   */
  public final int getOctet50() {
    return getInt(49);
  }

  // octet 50   ECWMF
  /**
   * Class
   *
   * @return Class
   */
  public final int getEnsembleNumber() {
    switch (getCenter()) {
      case 98: {
        if ( getExtension() == 30 ) {
          if (length > 49 ) {
            return getInt(49);
          } else {
            return GribNumbers.UNDEFINED;
          }
        }
      }
      default:
        return GribNumbers.UNDEFINED;
    }
  }

  /**
   * Octet 51
   *
   * @return int octet 51
   */
  public final int getOctet51() {
    return getInt(50);
  }

  /**
   * Octet 52
   *
   * @return int octet 52
   */
  public final int getOctet52() {
    return getInt(51);
  }

  // octet 48-51 Probability lower limit
  /**
   * lower limit of probability
   *
   * @return ID
   */
  public final float getValueLowerLimit() {
    if (length > 50 &&
        ((getParameterNumber() == 191) || (getParameterNumber() == 192))) {
      return GribNumbers.float4(getInt(47), getInt(48), getInt(49), getInt(50));
    } else {
      return GribNumbers.UNDEFINED;
    }
  }

  // octet 52-55 Probability upper limit
  /**
   * upper limit of probability
   *
   * @return ID
   */
  public final float getValueUpperLimit() {
    if (length > 54 &&
        ((getParameterNumber() == 191) || (getParameterNumber() == 192))) {
      return GribNumbers.float4(getInt(51), getInt(52), getInt(53), getInt(54));
    } else {
      return GribNumbers.UNDEFINED;
    }
  }

  // octet 61 members / forecasts
  /**
   * number members / forecasts
   *
   * @return ID
   */
  public final int getNumberForecasts() {
    switch (getCenter()) {
      case 7:
      case 8:
      case 9: {
        if (length > 60 &&
            ((getType() == 4) || (getType() == 5))) {
          return getInt(60);
        } else {
          return GribNumbers.UNDEFINED;
        }
      }
      // octet 51   ECWMF
      case 98: {
        if ( getExtension() == 30 ) {
          if (length > 50 ) {
            return getInt(50);
          } else {
            return GribNumbers.UNDEFINED;
          }
        }  
      }
      default:
        return GribNumbers.UNDEFINED;
    }
  }

  // octet 62 Cluster size
  /**
   * size Clusters
   *
   * @return ID
   */
  public final int getSizeClusters() {
    if (length > 61 &&
        ((getType() == 4) || (getType() == 5))) {
      return getInt(61);
    } else {
      return GribNumbers.UNDEFINED;
    }
  }

  // octet 63 Number of clusters
  /**
   * number Clusters
   *
   * @return Number
   */
  public final int getNumberClusters() {
    if (length > 62 &&
        ((getType() == 4) || (getType() == 5))) {
      return getInt(62);
    } else {
      return GribNumbers.UNDEFINED;
    }
  }

  // octet 64 Clustering Method
  /**
   * Method
   *
   * @return Method
   */
  public final int getMethod() {
    if (length > 63 &&
        ((getType() == 4) || (getType() == 5))) {
      return getInt(63);
    } else {
      return GribNumbers.UNDEFINED;
    }
  }

  // octet 65-67 Northern latitude of clustering domain
  /**
   * Northern latitude
   *
   * @return Northern latitude
   */
  public final float getNorthLatitude() {
    if (length > 66 &&
        ((getType() == 4) || (getType() == 5))) {
      return GribNumbers.int3(getInt(64), getInt(65), getInt(66)) / 1000;
    } else {
      return GribNumbers.UNDEFINED;
    }
  }

  // octet 68-70 Southern latitude of clustering domain
  /**
   * Southern latitude
   *
   * @return Southern latitude
   */
  public final float getSouthLatitude() {
    if (length > 69 &&
        ((getType() == 4) || (getType() == 5))) {
      return GribNumbers.int3(getInt(67), getInt(68), getInt(69)) / 1000;
    } else {
      return GribNumbers.UNDEFINED;
    }
  }

  // octet 71-73 Eastern Longitude of clustering domain
  /**
   * Eastern Longitude
   *
   * @return Eastern Longitude
   */
  public final float getEastLongitude() {
    if (length > 72 &&
        ((getType() == 4) || (getType() == 5))) {
      return GribNumbers.int3(getInt(70), getInt(71), getInt(72)) / 1000;
    } else {
      return GribNumbers.UNDEFINED;
    }
  }

  // octet 74-76 Western Longitude of clustering domain
  /**
   * Western Longitude
   *
   * @return Western Longitude
   */
  public final float getWestLongitude() {
    if (length > 75 &&
        ((getType() == 4) || (getType() == 5))) {
      return GribNumbers.int3(getInt(73), getInt(74), getInt(75)) / 1000;
    } else {
      return GribNumbers.UNDEFINED;
    }
  }

  // octet 77-86
  /**
   * Membership
   *
   * @return Membership
   */
  public final int[] getMembership() {
    if (length > 85 && (getType() == 4)) {
      int[] member = new int[10];
      int idx = 76;
      for (int i = 0; i < 10; i++)
        member[i] = getInt(idx++);
      return member;
    } else {
      return null;
    }
  }

  // implemented to satisfy interface GribPDSVariablesIF

  /**
   * Number of this coordinates.
   *
   * @return Coordinates number
   */
  public final int getCoordinates() {
    return GribNumbers.UNDEFINED;
  }

  /**
   * ChemicalType.
   *
   * @return ChemicalType
   */
  public final int getChemicalType() {
        return GribNumbers.UNDEFINED;
  }

  /**
   * backGenProcess.
   *
   * @return BackGenProcess
   */
  public final int getBackGenProcess() {
        return GribNumbers.UNDEFINED;
  }

  /**
   * ObservationProcess.
   *
   * @return ObservationProcess
   */
  public final int getObservationProcess() {
        return GribNumbers.UNDEFINED;
  }

  /**
   * Number Bands.
   *
   * @return NB
   */
  public final int getNB() {
        return GribNumbers.UNDEFINED;
  }
  /**
   * analysisGenProcess.
   *
   * @return analysisGenProcess
   */
  public final int getAnalysisGenProcess() {
        return GribNumbers.UNDEFINED;
  }

  /**
   * hoursAfter.
   *
   * @return HoursAfter
   */
  public final int getHoursAfter() {
        return 0;
  }

  /**
   * minutesAfter.
   *
   * @return MinutesAfter
   */
  public final int getMinutesAfter() {
        return 0;
  }

  /**
   * ForecastProbability.
   *
   * @return int ForecastProbability
   */
  public final int getForecastProbability() {
        return getProbabilityType();
  }

  /**
   * ForecastPercentile.
   *
   * @return int ForecastPercentile
   */
  public final int getForecastPercentile() {
        return GribNumbers.UNDEFINED;
  }

  /**
   * Perturbation number
   * @return int Perturbation
   */
  public final int getPerturbation() {
        return GribNumbers.UNDEFINED;
  }

  /**
   * Converts byte to int.
   *
   * @return int  byte as int
   */
  public final int getInt(int index) {
    return input[index] & 0xff;
  }

}
