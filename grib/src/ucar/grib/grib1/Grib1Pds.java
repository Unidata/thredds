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

package ucar.grib.grib1;

import net.jcip.annotations.Immutable;
import ucar.grib.GribNumbers;
import ucar.grib.GribPds;

import java.io.IOException;
import java.util.Date;
import java.util.Calendar;

/**
 * A class representing the product definition section (PDS) of a GRIB-1 product.
 * This is section 1 of a Grib record that contains information about the parameter
 */
@Immutable
public final class Grib1Pds extends GribPds {

  private static org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(Grib1Pds.class);

  /**
   * Length in bytes of this PDS.
   */
  private final int length;

  /**
   * Reference time in msecs
   */
  private final long refTime;

  /**
   * Valid time in msecs
   */
  private final long validTime;

  private GribPDSLevel level;

  // *** constructors *******************************************************

    /**
   * Constructs Grib1PDSVariables from a byte[].
   *
   * @param input PDS
   * @throws java.io.IOException byte[] read
   */
  public Grib1Pds(byte[] input) throws IOException {
    this(input,  Calendar.getInstance());
  }

  /**
   * Constructs Grib1PDSVariables from a byte[].
   *
   * @param input PDS
   * @param cal use this calendar to set the date
   * @throws java.io.IOException byte[] read
   */
  public Grib1Pds(byte[] input, Calendar cal) throws IOException {
    this.input = input;
    this.length = GribNumbers.int3(getOctet(1), getOctet(2), getOctet(3));
    this.level = new GribPDSLevel(getOctet(10), getOctet(11), getOctet(12));

    // calculate the reference date
    // octet 25
    int century = getOctet(25) - 1;
    if (century == -1) century = 20;
    // octets 13-17 (base time for reference time)
    int year = getOctet(13);
    int month = getOctet(14);
    int day = getOctet(15);
    int hour = getOctet(16);
    int minute = getOctet(17);

    cal.clear();
    cal.setTimeZone(java.util.TimeZone.getTimeZone("GMT"));
    cal.set(Calendar.DST_OFFSET, 0);
    cal.set((century * 100 + year), month - 1, day, hour, minute, 0);
    refTime = cal.getTimeInMillis();

    // calculate the valid date
    int type = 1;
    int factor = 1;
    switch (getTimeUnit()) {
      case 0: type = Calendar.MINUTE; break;
      case 1: type = Calendar.HOUR; break;
      case 2: type = Calendar.HOUR; factor = 24; break;
      case 3: type = Calendar.MONTH; break;
      case 4: type = Calendar.YEAR; break;
      case 5: type = Calendar.YEAR; factor = 10; break;
      case 6: type = Calendar.YEAR; factor = 30; break;
      case 7: type = Calendar.YEAR; factor = 100; break;
      case 10: type = Calendar.HOUR; factor = 3; break;
      case 11: type = Calendar.HOUR; factor = 6; break;
      case 12: type = Calendar.HOUR; factor = 12; break;
      default: throw new IllegalArgumentException("Unknown timeUnit= "+ getTimeUnit());
    }
    cal.add(type, factor * getForecastTime());
    validTime = cal.getTimeInMillis();
  }

  // getters

  /**
   * Number PDS section .
   */
  public final int getSection() {
    return 1;
  }

    // octets 1-3 (Length of PDS)

  public final int getLength() {
    return length;
  }

  // octet 4

  /**
   * gets the Table version as a int.
   *
   * @return table_version
   */
  public final int getParameterTableVersion() {
    return getOctet(4);
  }

  // octet 5

  /**
   * Center see table 0
   *
   * @return center_id
   */
  public final int getCenter() {
    return getOctet(5);
  }

  // octet 6

  /**
   * Type of Generating Process code see Table A
   *
   * @return Type of Generating Process code
   */
  public final int getTypeGenProcess() {
    return getOctet(6);
  }

  // octet 7

  /**
   * Grid ID see Table B
   *
   * @return grid_id
   */
  public final int getGridId() {
    return getOctet(7);
  }

  // octet 8

  /**
   * Check if GDS exists
   *
   * @return true, if GDS exists
   */
  public final boolean gdsExists() {
    return (getOctet(8) & 128) == 128;
  }

  /**
   * Check if BMS exists.
   *
   * @return true, if BMS exists
   */
  public final boolean bmsExists() {
    return (getOctet(8) & 64) == 64;
  }

  // octet 9

  /**
   * Get the number of the parameter, see Table 2
   *
   * @return index number of parameter in table
   */
  public final int getParameterNumber() {
    return getOctet(9);
  }

  //octet 10

  /**
   * Get the numeric type for 1st level, see Table 3 and 3a
   *
   * @return type of level (height or pressure)
   */
  public final int getLevelType1() {
    return level.getIndex();
  }

  public final String getLevelName() {
    return level.getName();
  }

  // octet 11-12

  /**
   * Get the numeric value for this level, see Table 3
   *
   * @return int level value
   */
  public final double getLevelValue1() {
    return level.getValue1();
  }

  /**
   * Get the numeric type for 2nd level.
   *
   * @return type of level always 255, Grib1 doesn't have type 2nd level
   */
  public final int getLevelType2() {
    return 255; // LOOK ??
  }

  // octet 11-12

  /**
   * Get value 2 (if it exists) for this level.
   *
   * @return int level value
   */
  public final double getLevelValue2() {
    return level.getValue2();
  }

  /**
   * Get the base (analysis) time of the forecast.
   *
   * @return date of basetime
   */
  public final Date getReferenceDate() {
    return new Date(refTime);
  }

  /**
   * gets reference time as a long millis.
   *
   * @return refTime
   */
  public final long getReferenceTime() {
    return refTime;
  }

  /**
   * octet 18 Forecast time unit, see Table 4
   *
   * @return int time unit index
   */
  public final int getTimeUnit() {
    return getOctet(18);
  }

  // octet 19  used to create Forecast time

  /**
   * Period of time, octet 19 in units of getTimeRangeUnit()
   *
   * @return P1
   */
  public final int getP1() {
    if (getTimeRangeIndicator() == 10)
      return GribNumbers.int2(getOctet(19), getOctet(20));
    return getOctet(19);
  }

  /**
   * P2 - octet 20 - Period of time or time interval
   * in units of getTimeRangeUnit()
   *
   * @return P2
   */
  public final int getP2() {
    if (getTimeRangeIndicator() == 10)
      return 0;
    return getOctet(20);
  }

  /**
   * Time Range indicator - octet 21 (see Table 5)
   *
   * @return Time Range indicator
   */
  public final int getTimeRangeIndicator() {
    return getOctet(21);
  }

  public final Date getForecastDate() {
    return new Date(validTime);
  }

  /**
   * Get the time of the forecast.
   *
   * @return date and time
   */
  public final int getForecastTime() {
    // forecast time is always at the end of the range
    switch (getTimeRangeIndicator()) {

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
                + getTimeRangeIndicator() + " is not yet supported");
    }
    return GribNumbers.UNDEFINED;
  }

  public final boolean isInterval() {
    int code = getTimeRangeIndicator();
    return (code == 2) || (code == 3) || (code == 4) || (code == 5) || (code == 6) || (code == 7);
    // LOOK not sure about code > 10
  }

  public long getIntervalTimeEnd() {
    return -1;
  }


  /**
   * Get the time interval of the forecast.
   *
   * @return interval as int[2]
   */
  public int[] getForecastTimeInterval() {
    if (!isInterval()) return null;

    int[] interval = new int[2];
    interval[0] = getP1();
    interval[1] = getP2();
    return interval;
  }

  /**
   * Get Grib-2 Interval Statistic Type (Table 4-10) by converting Grib-1 Table 5
   *
   * @return Grib-2 Interval Statistic Type (Table 4-10)
   */
  public int getIntervalStatType() {
    switch (getTimeRangeIndicator()) {
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
   * Number Included In Average, octet 22 & 23
   *
   * @return Number Included In Average
   */
  public final int getNumberIncludedInAverage() {
    return GribNumbers.int2(getOctet(22), getOctet(23));
  }

  // octet 24

  /**
   * Number Missing In Average, octet 24
   *
   * @return Number Missing In Average
   */
  public final int getNumberMissingInAverage() {
    return getOctet(24);
  }

  // octet 26

  /**
   * SubCenter, allocated by center (Table C)
   *
   * @return subCenter
   */
  public final int getSubCenter() {
    return getOctet(26);
  }

  // octets 27-28 (decimal scale factor)

  /**
   * Get the exponent of the decimal scale used for all data values.
   *
   * @return exponent of decimal scale
   */
  public final int getDecimalScale() {
    return GribNumbers.int2(getOctet(27), getOctet(28));
  }

  //////////////////////////////////////////////////////////////////
  // Local table Specific processing

  // octet 41 id's ensemble
  // Ensemble processing

  public final int getExtension() {
    return getOctet(41);
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
        return getOctet(42);
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
        return getOctet(43);
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
    return GribNumbers.int2(getOctet(44), getOctet(45));
  }

  // octet 44 Product Identifier

  /**
   * Product of ensemble
   *
   * @return ID
   */
  public final int getProductID() {
    return getOctet(44);
  }

  // octet 45 Spatial Identifier or Probability

  /**
   * Spatial Identifier or Probability of ensemble
   *
   * @return ID
   */
  public final int getSpatialorProbability() {
    return getOctet(45);
  }

  // octet 46 Probability product definition

  /**
   * Product of ensemble
   *
   * @return ID
   */
  public final int getProbabilityProduct() {
    if (length > 45 && ((getParameterNumber() == 191) || (getParameterNumber() == 192))) {
      // octet 46 Probability product definition
      return getOctet(46);
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
    if (length > 46 && ((getParameterNumber() == 191) || (getParameterNumber() == 192))) {
      return getOctet(47);
    } else {
      return GribNumbers.UNDEFINED;
    }
  }

  /**
   * NCEP Appendix C Manual 388
   * http://www.nco.ncep.noaa.gov/pmb/docs/on388/appendixc.html
   * states that if the PDS is > 28 bytes and octet 41 == 1
   * then it's an ensemble an product.
   *
   * @return true if this is an ensemble
   */
  public final boolean isEnsemble() {
    if ((getCenter() == 7) && (length >= 44 && getOctet(41) == 1 && getOctet(42) < 4 )) return true;
    if ((getCenter() == 98) && (length > 40 && getOctet(41) != 0)) return true; // LOOK ecmwf reference ??
    return false;
  }

  public final int getPerturbationType() {
    if (!isEnsemble()) return GribNumbers.UNDEFINED;
    if (getCenter() == 7) return getOctet(42);
    if (getCenter() == 98) return getOctet(43);
    return GribNumbers.UNDEFINED;
  }

  public final int getPerturbationNumber() {
    if (!isEnsemble()) return GribNumbers.UNDEFINED;

    /*
    0 = Unperturbed control forecast
    1-5 = Individual negatively perturbed forecast
    6-10 = Individual positively perturbed forecast
     */
    if (getCenter() == 7) {
      int type =  getOctet(42);
      int id =  getOctet(43);
      if (type == 1) return 0;
      if (type == 2) return id;
      if (type == 3) return 5 + id;
    }
    if (getCenter() == 98) return getOctet(43);
    return GribNumbers.UNDEFINED;
  }

  @Override
  public int getNumberEnsembleForecasts() {
    return 0;
  }

  @Override
  public boolean isEnsembleDerived() {
    return false;
  }

  @Override
  public boolean isProbability() {
    return false;
  }

  @Override
  public double getProbabilityLowerLimit() {
    return Double.NaN;
  }

  @Override
  public double getProbabilityUpperLimit() {
    return Double.NaN;
  }

  public final int getType() {
      switch (getCenter()) {
        case 7:
        case 8:
        case 9: {
          return getOctet(42);
        }
        // octet 43   ECWMF
        case 98: {
          return getOctet(43);
        }
        default:
          return GribNumbers.UNDEFINED;
      }
    }



  // octet 48-51 Probability lower limit

  /**
   * lower limit of probability
   *
   * @return ID
   */
  public final float getValueLowerLimit() {
    if (length > 50 && ((getParameterNumber() == 191) || (getParameterNumber() == 192))) {
      return GribNumbers.float4(getOctet(48), getOctet(49), getOctet(50), getOctet(51));
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
    if (length > 54 && ((getParameterNumber() == 191) || (getParameterNumber() == 192))) {
      return GribNumbers.float4(getOctet(52), getOctet(53), getOctet(54), getOctet(55));
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
        if (length > 60 && ((getType() == 4) || (getType() == 5))) {
          return getOctet(61);
        } else {
          return GribNumbers.UNDEFINED;
        }
      }
      // octet 51   ECWMF
      case 98: {
        if (getExtension() == 30) {
          return getOctet(51);
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
    if (length > 61 && ((getType() == 4) || (getType() == 5))) {
      return getOctet(62);
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
    if (length > 62 && ((getType() == 4) || (getType() == 5))) {
      return getOctet(63);
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
    if (length > 63 && ((getType() == 4) || (getType() == 5))) {
      return getOctet(64);
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
    if (length > 66 && ((getType() == 4) || (getType() == 5))) {
      return GribNumbers.int3(getOctet(65), getOctet(66), getOctet(67)) / 1000;
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
    if (length > 69 && ((getType() == 4) || (getType() == 5))) {
      return GribNumbers.int3(getOctet(68), getOctet(69), getOctet(70)) / 1000;
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
    if (length > 72 && ((getType() == 4) || (getType() == 5))) {
      return GribNumbers.int3(getOctet(71), getOctet(72), getOctet(73)) / 1000;
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
    if (length > 75 && ((getType() == 4) || (getType() == 5))) {
      return GribNumbers.int3(getOctet(74), getOctet(75), getOctet(76)) / 1000;
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
      int idx = 77;
      for (int i = 0; i < 10; i++)
        member[i] = getOctet(idx++);
      return member;
    } else {
      return null;
    }
  }

}
