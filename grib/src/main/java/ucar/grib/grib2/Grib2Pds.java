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
 * Represents the product definition section (PDS) of a GRIB-2
 * extracted from a byte[].
 * This is section 4 of a Grib record that contains information about the parameter
 */
package ucar.grib.grib2;

import net.jcip.annotations.Immutable;
import ucar.grib.GribNumbers;
import ucar.grib.GribPds;

import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.Formatter;

@Immutable
abstract public class Grib2Pds extends GribPds {

  private static org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(Grib2Pds.class);
  private static final int MISSING = -9999;
  private static final double MISSINGD = -9999.0;

  /**
   * Factory for Grib2Pds
   * @param input raw bytes
   * @param refTime reference time in msecs
   * @param cal helper object
   * @return Grib2Pds or null on error
   * @throws IOException on read error
   */
  static public Grib2Pds factory(byte[] input, long refTime, Calendar cal) throws IOException {
    int template = GribNumbers.int2(input[7] & 0xff, input[8] & 0xff); // octet 8 and 9
    switch (template) {
      case 0:
        return new Grib2Pds0(input, refTime, cal);
      case 1:
        return new Grib2Pds1(input, refTime, cal);
      case 2:
        return new Grib2Pds2(input, refTime, cal);
      case 5:
        return new Grib2Pds5(input, refTime, cal);
      case 6:
        return new Grib2Pds6(input, refTime, cal);
      case 8:
        return new Grib2Pds8(input, refTime, cal);
      case 9:
        return new Grib2Pds9(input, refTime, cal);
      case 10:
        return new Grib2Pds10(input, refTime, cal);
      case 11:
        return new Grib2Pds11(input, refTime, cal);
      case 12:
        return new Grib2Pds12(input, refTime, cal);
      case 15:
        return new Grib2Pds15(input, refTime, cal);
      case 30:
        return new Grib2Pds30(input, refTime, cal);
      default:
        log.warn("Missing template " + template);
        return null;
    }
  }

  // public for testing (TestGribFromTds)
  static public long makeDate(long refTime, int timeUnit, int forecastTime, Calendar cal) {

    if (cal == null) cal = Calendar.getInstance();
    cal.clear();
    cal.setTimeInMillis(refTime);

    // add the forecast time
    int type = 1; // default = hour
    int factor = 1;
    switch (timeUnit) { // code table 4.4
      case 0:
        type = Calendar.MINUTE;
        break;
      case 1:
        type = Calendar.HOUR_OF_DAY;
        break;
      case 2:
        type = Calendar.HOUR_OF_DAY;
        factor = 24;
        break;
      case 3:
        type = Calendar.MONTH;
        break;
      case 4:
        type = Calendar.YEAR;
        break;
      case 5:
        type = Calendar.YEAR;
        factor = 10;
        break;
      case 6:
        type = Calendar.YEAR;
        factor = 30;
        break;
      case 7:
        type = Calendar.YEAR;
        factor = 100;
        break;
      case 10:
        type = Calendar.HOUR_OF_DAY;
        factor = 3;
        break;
      case 11:
        type = Calendar.HOUR_OF_DAY;
        factor = 6;
        break;
      case 12:
        type = Calendar.HOUR_OF_DAY;
        factor = 12;
        break;
      case MISSING: // if there is no time unit / valid time, assume valid time == ref time
        type = Calendar.HOUR_OF_DAY;
        factor = 0;
        break;
      default:
        log.warn("Unknown timeUnit= " + timeUnit);
        factor = 0;
        break;
    }
    if (factor != 0)
      cal.add(type, factor * forecastTime);

    return cal.getTimeInMillis();
  }

  // given reference and forecast date, calculate forecastTime in units of timeUnit

  static public int makeForecastTime(long refTime, long foreDate, int timeUnit) {

    int intv = (int) ((foreDate - refTime) / 1000); // secs

    // common cases
    if (timeUnit == 1)
      return intv / 3600; // hour
    else if (timeUnit == 0)
      return intv / 60; // minute
    else if (timeUnit == 2)
      return intv / 3600 / 24; // day
    else if (timeUnit == 3)
      return intv / 3600 / 24 / 30; // month LOOK WRONG
    else if (timeUnit == 10)
      return intv / 3600 / 3; // 3 hour
    else if (timeUnit == 11)
      return intv / 3600 / 6; // 6 hour
    else if (timeUnit == 12)
      return intv / 3600 / 12; // 12 hour
    else if (timeUnit == 13)
      return intv; // sec

    // LOOK otherwise ??

    throw new UnsupportedOperationException("timeUnit = "+timeUnit);
  }

  ////////////////////////

  protected final int template; // product definition template
  protected final long refTime; // reference date in millisecs
  protected long validTime = -1; // reference date in millisecs
  protected int[] intv = null;

  /**
   * Constructs a Grib2PDSVariables object from a byte[].
   *
   * @param input   PDS
   * @param refTime reference time in msecs
   * @param cal     helper for creating Dates
   * @throws java.io.IOException if raf contains no valid GRIB file
   */
  protected Grib2Pds(byte[] input, long refTime, Calendar cal) throws IOException {
    this.input = input;
    this.refTime = refTime;
    template = GribNumbers.int2(getOctet(8), getOctet(9));
  }

  public long getReferenceTime() {
    return refTime;
  }

  // octets 1-4 (Length of PDS)

  public final int getLength() {
    return GribNumbers.int4(getOctet(1), getOctet(2), getOctet(3), getOctet(4));
  }

  /**
   * Number of this section, should be 4.
   */
  public final int getSection() {
    return getOctet(5);
  }

  /**
   * Number of coordinate values at end of template.
   *
   * @return Coordinates number
   */
  public final int getNumberCoordinates() {
    return GribNumbers.int2(getOctet(6), getOctet(7));
  }

  /**
   * product Definition template, Table 4.0
   *
   * @return ProductDefinition
   */
  public final int getProductDefinitionTemplate() {
    return template;
  }

  /**
   * Parameter Category
   *
   * @return parameterCategory as int
   */
  public final int getParameterCategory() {
    return getOctet(10);
  }

  /**
   * Parameter Number
   *
   * @return ParameterNumber
   */
  public final int getParameterNumber() {
    return getOctet(11);
  }

  /**
   * Type of Generating Process (Code Table 4.3)
   *
   * @return Type of Generating Process
   */
  public int getGenProcessType() {
    return MISSING;
  }

  public String getUseGenProcessType() {
    int type = getGenProcessType();
    if ((type == 7) || (type == 6)) return "error";
    return null;
  }

  @Override
  public Date getForecastDate() {
    if (validTime < 0)
      validTime = makeDate(refTime, getTimeUnit(), getForecastTime(), null);
    return new Date(validTime);
  }

  /**
   * Forecast/Analysis generating process identifier (defined by originating centre).
   * <p/>
   * For NCEP, apparently
   * http://www.nco.ncep.noaa.gov/pmb/docs/on388/tablea.html
   * as linked from here:
   * http://www.nco.ncep.noaa.gov/pmb/docs/grib2/grib2_temp4-0.shtml
   *
   * @return generating process id
   */
  public int getGenProcessId() {
    return MISSING;
  }

  @Override
  public boolean isInterval() {
    return (template >= 8) && (template <= 14);
  }


  @Override
  public int getStatisticalProcessType() {
    if (!isInterval()) return -1;

    // assume they are all the same
    PdsInterval pdsIntv = (PdsInterval) this;
    TimeInterval[] ti = pdsIntv.getTimeIntervals();
    if (ti.length > 0) {
      TimeInterval ti0 = ti[0];
      return ti0.statProcessType;
    }

    return -1;
  }

  @Override
  public long getIntervalTimeEnd() {
    return MISSING;
  }


  /**
   * Forecast time in units defined by octet 18 (getTimeUnit())
   *
   * @return Forecast time
   */
  @Override
  public int getForecastTime() {
    if (isInterval()) {
      int[] intv = getForecastTimeInterval();
      return intv[1];
    }
    return _getForecastTime();
  }

  /**
   * Time Interval for accumulation type variables.
   * Forecast Time is always at the end.
   *
   * @return TimeInterval int[2] = start, end of interval in units of getTimeUnit()
   */
  public int[] getForecastTimeInterval() {
    if (!isInterval()) return null;
    if (intv != null) return intv;

    int timeUnit = getTimeUnit();
    PdsInterval pdsIntv = (PdsInterval) this;

    int incr = 0;
    for (TimeInterval ti : pdsIntv.getTimeIntervals()) {
      if ((ti.timeRangeUnit != timeUnit) || (ti.timeIncrementUnit != timeUnit && ti.timeIncrementUnit != 255)) {
        log.warn("TimeInterval has different units timeUnit= " + timeUnit + " TimeInterval=" + ti);
      }

      incr += ti.timeRangeLength;
      if (ti.timeIncrementUnit != 255) incr += ti.timeIncrement;

      /*
      // rather mysterious
      switch (ti.timeIncrementType) { // code table 4.11
        case 1:
        case 2:
        case 3:
        case 4:
        case 5:
          incr += ti.timeRangeLength;
          if (ti.timeIncrementUnit != 255) incr += ti.timeIncrement;
          break;
        case 255:
          break;
        default:
          log.warn("Unknown timeIncrementType= " + ti.timeIncrementType+" for "+getProductDefinitionTemplate()+"/"+getParameterCategory()+"/"+getParameterNumber());
      } */

    }

    int[] result = new int[2];
    //result[0] = _getForecastTime();  // LOOK probably wrong - depends on ti.timeIncrementType, see code table 4.11
    // result[1] = result[0] + incr;
    result[1] = pdsIntv.getForecastTime();
    result[0] = result[1] - incr;

    intv = result;
    return result;
  }

  public int[] getForecastTimeInterval(int wantUnit) {
    int[] intv = getForecastTimeInterval();
    int timeUnit = getTimeUnit();
    if (timeUnit == wantUnit)
      return intv;

    long facHave = Grib2Tables.codeTable4_4_toSecs(timeUnit);
    long facWant = Grib2Tables.codeTable4_4_toSecs(wantUnit);
    double fac = (double) facHave / facWant;

    int[] convertIntv = new int[2];
    convertIntv[0] = (int) (intv[0] * fac);
    convertIntv[1] = (int) (intv[1] * fac);
    return convertIntv;
  }


  // forecast time for points, beginning of interval for intervals

  protected int _getForecastTime() {
    return GribNumbers.int4(getOctet(19), getOctet(20), getOctet(21), getOctet(22));
  }

  @Override
  public boolean isEnsemble() {
    return false;
  }

  @Override
  public int getPerturbationNumber() {
    return MISSING;
  }

  @Override
  public int getPerturbationType() {
    return MISSING;
  }

  @Override
  public boolean isEnsembleDerived() {
    return false;
  }

  @Override
  public int getNumberEnsembleForecasts() {
    return MISSING;
  }

  @Override
  public boolean isProbability() {
    return false;
  }

  @Override
  public double getProbabilityLowerLimit() {
    return MISSINGD;
  }

  @Override
  public double getProbabilityUpperLimit() {
    return MISSINGD;
  }

  @Override
  public int getProbabilityType() {
    return MISSING;
  }

  @Override
  public boolean isPercentile() {
    return false;
  }

  @Override
  public int getPercentileValue() {
    return -1;
  }


  public void show(Formatter f) {
    f.format("Grib2Pds{ template=%d, validTime=%s }", template, getForecastDate());
  }

  //////////////////////////////////////////////////////////////////////////////////////////////

  static public interface PdsInterval {
    public long getIntervalTimeEnd();

    public int getForecastTime();

    public int getNumberTimeRanges();

    public int getNumberMissing();

    public TimeInterval[] getTimeIntervals();
  }

  static public interface PdsEnsemble {
    public int getPerturbationType();

    public int getPerturbationNumber();

    public int getNumberEnsembleForecasts();
  }

  static public interface PdsEnsembleDerived {
    public int getDerivedForecastType();

    public int getNumberEnsembleForecasts();
  }

  static public interface PdsPercentile {
    public int getPercentileValue();
  }


  static public interface PdsProbability {
    public int getForecastProbabilityNumber();

    public int getNumberForecastProbabilities();

    public int getProbabilityType();

    public double getProbabilityLowerLimit();

    public double getProbabilityUpperLimit();
  }

  //////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * Product definition template 4.0 - analysis or forecast at a horizontal level or in a horizontal layer at a point in time
   * Many other templates (1-14) have same fields in sameplaces, so can use this as the superclass.
   */
  static private class Grib2Pds0 extends Grib2Pds {

    Grib2Pds0(byte[] input, long refTime, Calendar cal) throws IOException {
      super(input, refTime, cal);
    }

    public int getGenProcessType() {
      return getOctet(12);
    }

    /**
     * Background generating process identifier (defined by originating centre)
     *
     * @return Background generating process id
     */
    public int getGenProcessIdBackground() {
      return getOctet(13);
    }

    public int getGenProcessId() {
      return getOctet(14);
    }

    /**
     * Hours after reference time of data cutoff
     *
     * @return HoursAfter
     */
    public int getHoursAfterCutoff() {
      return GribNumbers.int2(getOctet(15), getOctet(16));
    }

    /**
     * Minutes after reference time of data cutoff
     *
     * @return MinutesAfter
     */
    public int getMinutesAfterCutoff() {
      return getOctet(17);
    }

    /**
     * Indicator of unit of time range (see Code table 4.4)
     *
     * @return TimeRangeUnit
     */
    @Override
    public int getTimeUnit() {
      return getOctet(18);
    }

    /**
     * Type of first fixed surface (see Code table 4.5)
     *
     * @return Type of first fixed surface
     */
    @Override
    public int getLevelType1() {
      return getOctet(23);
    }

    /*
       case 30: // to make backwards compatible
       case 31: {
         return 0;
       }
       // octet 25
       case 40:
       case 41:
       case 42:
       case 43: {
         return getOctet(24);
       }
       default:
         return GribNumbers.UNDEFINED;

     }
   } */

    /**
     * Value of first fixed surface, with scale factor already applied
     *
     * @return float FirstFixedSurfaceValue
     */
    @Override
    public double getLevelValue1() {
      int scaleFirstFixedSurface = getOctet(24);
      int valueFirstFixedSurface = GribNumbers.int4(getOctet(25), getOctet(26), getOctet(27), getOctet(28));
      return applyScaleFactor(scaleFirstFixedSurface, valueFirstFixedSurface);
    }

    /*
    // octet 26-30
    case 40:
    case 41:
    case 42:
    case 43: {
      scaleFirstFixedSurface = getOctet(25);
      // octet 27-30
      valueFirstFixedSurface = GribNumbers.int4(getOctet(26), getOctet(27), getOctet(28), getOctet(29));
      break;
    }
    default:
      return GribNumbers.UNDEFINED;

  }  */

    /**
     * Type of second fixed surface (see Code table 4.5)
     *
     * @return Type of second fixed surface
     */
    @Override
    public int getLevelType2() {
      return getOctet(29);
    }

    /*
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
        return getOctet(30);
      }
      default:
        return GribNumbers.UNDEFINED;

    }
  }  */

    /**
     * Value of second fixed surface, with scale factor already applied
     *
     * @return float FirstFixedSurfaceValue
     */
    @Override
    public double getLevelValue2() {
      int scale = getOctet(30);
      int value = GribNumbers.int4(getOctet(31), getOctet(32), getOctet(33), getOctet(34));
      return applyScaleFactor(scale, value);
    }

    /*
    // octet 32-36
    case 40:
    case 41:
    case 42:
    case 43: {
      scaleSecondFixedSurface = getOctet(31);
      // octet 33-36
      valueSecondFixedSurface = GribNumbers.int4(getOctet(32), getOctet(33), getOctet(34), getOctet(35));
      break;
    }
    default:
      return GribNumbers.UNDEFINED;

  }
 }   */

    /**
     * length of template (next byte starts info after template, if any
     *
     * @return length of template
     */
    public int templateLength() {
      return 34;
    }

    ///////////////////////////////////////////////

    /**
     * ChemicalType.
     *
     * @return ChemicalType
     */
    public final int getChemicalType() {
      switch (template) {
        // octet 12-13
        case 40:
        case 41:
        case 42:
        case 43: {
          return GribNumbers.int2(getOctet(11), getOctet(12));
        }
        default:
          return GribNumbers.UNDEFINED;

      }
    }

    public void show(Formatter f) {
      super.show(f);
    }

  }

  //////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * Product definition template 4.15 - average, accumulation, extreme values, or other statistically-processed
	 * values over a spatial area at a horizontal level or in a horizontal layer at a point in time
   */
  static private class Grib2Pds15 extends Grib2Pds0 {
    Grib2Pds15(byte[] input, long refTime, Calendar cal) throws IOException {
      super(input, refTime, cal);
    }

    // table 4.10
    @Override
    public int getStatisticalProcessType() {
      return getOctet(35);
    }

    // code 4.15
    public int getSpatialProcessType() {
      return getOctet(36);
    }

    public int getNSpatialDataPoints() {
      return getOctet(37);
    }

  }


    //////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Product definition template 4.1 -
     * individual ensemble forecast, control and perturbed, at a horizontal level or in a horizontal layer at a point in time
     */
    static private class Grib2Pds1 extends Grib2Pds0 implements PdsEnsemble {
      Grib2Pds1(byte[] input, long refTime, Calendar cal) throws IOException {
        super(input, refTime, cal);
      }

    public boolean isEnsemble() {
      return true;
    }

    /**
     * Type of ensemble forecast (see Code table 4.6)
     *
     * @return Type of ensemble forecast
     */
    public int getPerturbationType() {
      return getOctet(35);
    }

    /**
     * Perturbation Number - which member of the ensemble is this ?
     *
     * @return Perturbation Number
     */
    public int getPerturbationNumber() {
      return getOctet(36);
    }

    /**
     * Number of forecasts in ensemble
     *
     * @return Number of forecasts in ensemble
     */
    public int getNumberEnsembleForecasts() {
      return getOctet(37);
    }

    @Override
    public int templateLength() {
      return 37;
    }

  }

  //////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * Product definition template 4.11 -
   * individual ensemble forecast, control and perturbed, at a horizontal level or in a horizontal layer in a continuous
   * or non-continuous time interval
   */
  static private class Grib2Pds11 extends Grib2Pds1 implements PdsInterval {
    private long endInterval; // Date msecs
    private int ft; // forecast time in units of time intv

    Grib2Pds11(byte[] input, long refTime, Calendar cal) throws IOException {
      super(input, refTime, cal);
      endInterval = calcTime(refTime, cal, 38);
      ft = makeForecastTime(refTime, endInterval, getTimeUnit());
    }

    /**
     * End of overall time interval
     *
     * @return End of overall time interval
     */
    public long getIntervalTimeEnd() {
      return endInterval;
    }

    @Override
    public int getForecastTime() {
      return ft;
    }

    /**
     * number of time range specifications describing the time intervals used to calculate the statistically-processed field
     *
     * @return number of time range
     */
    public int getNumberTimeRanges() {
      return getOctet(45);
    }

    /**
     * Total number of data values missing in statistical process
     *
     * @return Total number of data values missing in statistical process
     */
    public final int getNumberMissing() {
      return GribNumbers.int4(getOctet(46), getOctet(47), getOctet(48), getOctet(49));
    }

    public TimeInterval[] getTimeIntervals() {
      return readTimeIntervals(getNumberTimeRanges(), 50);
    }

    @Override
    public int templateLength() {
      return 49 + getNumberTimeRanges() * 12;
    }

  }

  //////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * Product definition template 4.2 -
   * derived forecasts based on all ensemble members at a horizontal level or in a horizontal layer at a point in time
   */
  static private class Grib2Pds2 extends Grib2Pds0 implements PdsEnsembleDerived {

    Grib2Pds2(byte[] input, long refTime, Calendar cal) throws IOException {
      super(input, refTime, cal);
    }

    public boolean isEnsembleDerived() {
      return true;
    }

    /**
     * Derived forecast Type (see Code table 4.7)
     *
     * @return Derived forecast Type
     */
    public int getDerivedForecastType() {
      return getOctet(35);
    }

    /**
     * Number of forecasts in ensemble
     *
     * @return Number of forecasts in ensemble
     */
    public int getNumberForecastsInEnsemble() {
      return getOctet(36);
    }

    @Override
    public int templateLength() {
      return 36;
    }

  }

  //////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * Product definition template 4.12 -
   * derived forecasts based on all ensemble members at a horizontal level or in a horizontal layer in a
   * continuous or non-continuous time interval
   */
  static private class Grib2Pds12 extends Grib2Pds2 implements PdsInterval {
    long endInterval; // Date msecs
    int ft;

    Grib2Pds12(byte[] input, long refTime, Calendar cal) throws IOException {
      super(input, refTime, cal);
      endInterval = calcTime(refTime, cal, 37);
      ft = makeForecastTime(refTime, endInterval, getTimeUnit());
    }

    /**
     * End of overall time interval
     *
     * @return End of overall time interval
     */
    public long getIntervalTimeEnd() {
      return endInterval;
    }

    @Override
    public int getForecastTime() {
      return ft;
    }

    /**
     * number of time range specifications describing the time intervals used to calculate the statistically-processed field
     *
     * @return number of time range
     */
    public int getNumberTimeRanges() {
      return getOctet(44);
    }

    /**
     * Total number of data values missing in statistical process
     *
     * @return Total number of data values missing in statistical process
     */
    public final int getNumberMissing() {
      return GribNumbers.int4(getOctet(45), getOctet(46), getOctet(47), getOctet(48));
    }

    public TimeInterval[] getTimeIntervals() {
      return readTimeIntervals(getNumberTimeRanges(), 49);
    }

    @Override
    public int templateLength() {
      return 48 + getNumberTimeRanges() * 12;
    }

  }

  //////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * Product definition template 4.5 -
   * probability forecasts at a horizontal level or in a horizontal layer at a point in time
   */
  static private class Grib2Pds5 extends Grib2Pds0 implements PdsProbability {

    Grib2Pds5(byte[] input, long refTime, Calendar cal) throws IOException {
      super(input, refTime, cal);
    }

    public boolean isProbability() {
      return true;
    }

    /**
     * Forecast probability number
     *
     * @return Forecast probability number
     */
    public int getForecastProbabilityNumber() {
      return getOctet(35);
    }

    /**
     * Number of forecasts probabilities
     *
     * @return Number of forecasts probabilities
     */
    public int getNumberForecastProbabilities() {
      return getOctet(36);
    }

    /**
     * Probability type (see Code table 4.9)
     *
     * @return Probability type
     */
    public int getProbabilityType() {
      return getOctet(37);
    }

    public double getProbabilityLowerLimit() {
      int scale = getOctet(37);
      int value = GribNumbers.int4(getOctet(39), getOctet(40), getOctet(41), getOctet(42));
      return applyScaleFactor(scale, value);
    }

    public double getProbabilityUpperLimit() {
      int scale = getOctet(43);
      int value = GribNumbers.int4(getOctet(44), getOctet(45), getOctet(46), getOctet(47));
      return applyScaleFactor(scale, value);
    }

    @Override
    public int templateLength() {
      return 47;
    }
  }

  //////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * Product definition template 4.9 -
   * probability forecasts at a horizontal level or in a horizontal layer in a continuous or non-continuous time interval
   */
  static private class Grib2Pds9 extends Grib2Pds5 implements PdsInterval {
    long endInterval; // Date msecs
    int ft;

    Grib2Pds9(byte[] input, long refTime, Calendar cal) throws IOException {
      super(input, refTime, cal);
      endInterval = calcTime(refTime, cal, 48);
      ft = makeForecastTime(refTime, endInterval, getTimeUnit());
    }

    /**
     * End of overall time interval
     *
     * @return End of overall time interval
     */
    public long getIntervalTimeEnd() {
      return endInterval;
    }

    @Override
    public int getForecastTime() {
      return ft;
    }

    /**
     * number of time range specifications describing the time intervals used to calculate the statistically-processed field
     *
     * @return number of time range
     */
    public int getNumberTimeRanges() {
      return getOctet(55);
    }

    /**
     * Total number of data values missing in statistical process
     *
     * @return Total number of data values missing in statistical process
     */
    public final int getNumberMissing() {
      return GribNumbers.int4(getOctet(56), getOctet(57), getOctet(58), getOctet(59));
    }

    public TimeInterval[] getTimeIntervals() {
      return readTimeIntervals(getNumberTimeRanges(), 60);
    }

    @Override
    public int templateLength() {
      return 59 + getNumberTimeRanges() * 12;
    }

  }

  //////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * Product definition template 4.8 -
   * Average, accumulation, extreme values or other statistically processed values at a horizontal level or in a
   * horizontal layer in a continuous or non-continuous time interval
   */
  static private class Grib2Pds8 extends Grib2Pds0 implements PdsInterval {
    long endInterval; // Date msecs
    int ft;

    Grib2Pds8(byte[] input, long refTime, Calendar cal) throws IOException {
      super(input, refTime, cal);
      endInterval = calcTime(refTime, cal, 35);
      ft = makeForecastTime(refTime, endInterval, getTimeUnit());
    }

    /**
     * End of overall time interval
     *
     * @return End of overall time interval
     */
    public long getIntervalTimeEnd() {
      return endInterval;
    }

    @Override
    public int getForecastTime() {
      return ft;
    }

    /**
     * number of time range specifications describing the time intervals used to calculate the statistically-processed field
     *
     * @return number of time range
     */
    public int getNumberTimeRanges() {
      return getOctet(42);
    }

    /**
     * Total number of data values missing in statistical process
     *
     * @return Total number of data values missing in statistical process
     */
    public final int getNumberMissing() {
      return GribNumbers.int4(getOctet(43), getOctet(44), getOctet(45), getOctet(46));
    }

    public TimeInterval[] getTimeIntervals() {
      return readTimeIntervals(getNumberTimeRanges(), 47);
    }

    @Override
    public int templateLength() {
      return 46 + getNumberTimeRanges() * 12;
    }

    public void show(Formatter f) {
      super.show(f);
      f.format("%n   endInterval=%s%n", new Date(endInterval));
      for (TimeInterval ti : getTimeIntervals()) {
        ti.show(f);
      }
    }

  }

  //////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * Product definition template 4.6 -
   * percentile forecasts at a horizontal level or in a horizontal layer at a point in time
   */
  static private class Grib2Pds6 extends Grib2Pds0 implements PdsPercentile {

    Grib2Pds6(byte[] input, long refTime, Calendar cal) throws IOException {
      super(input, refTime, cal);
    }

    public boolean isPercentile() {
      return true;
    }

    /**
     * Percentile - from 100 to 0
     *
     * @return Percentile
     */
    public int getPercentileValue() {
      return getOctet(35);
    }

    @Override
    public int templateLength() {
      return 36;
    }

  }


  /**
   * Product definition template 4.10 -
   * percentile forecasts at a horizontal level or in a horizontal layer in a continuous or non-continuous time interval
   */
  static private class Grib2Pds10 extends Grib2Pds6 implements PdsInterval {
    long endInterval;
    int ft;

    Grib2Pds10(byte[] input, long refTime, Calendar cal) throws IOException {
      super(input, refTime, cal);
      endInterval = calcTime(refTime, cal, 36);
      ft = makeForecastTime(refTime, endInterval, getTimeUnit());
    }

    /**
     * End of overall time interval
     *
     * @return End of overall time interval
     */
    public long getIntervalTimeEnd() {
      return endInterval;
    }

    @Override
    public int getForecastTime() {
      return ft;
    }

    /**
     * number of time range specifications describing the time intervals used to calculate the statistically-processed field
     *
     * @return number of time range
     */
    public int getNumberTimeRanges() {
      return getOctet(43);
    }

    /**
     * Total number of data values missing in statistical process
     *
     * @return Total number of data values missing in statistical process
     */
    public final int getNumberMissing() {
      return GribNumbers.int4(getOctet(44), getOctet(45), getOctet(46), getOctet(47));
    }

    public TimeInterval[] getTimeIntervals() {
      return readTimeIntervals(getNumberTimeRanges(), 48);
    }

    @Override
    public int templateLength() {
      return 48 + getNumberTimeRanges() * 12;
    }

  }


  //////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * Product definition template 4.30 - satellite product
   *
   * @deprecated
   */
  static private class Grib2Pds30 extends Grib2Pds {

    Grib2Pds30(byte[] input, long refTime, Calendar cal) throws IOException {
      super(input, refTime, cal);
    }

    // LOOK - could put this into a dummy superclass in case others need

    @Override
    public double getLevelValue1() {
      return MISSINGD;
    }

    @Override
    public double getLevelValue2() {
      return MISSING;
    }

    @Override
    public int getLevelType1() {
      return MISSING;
    }

    @Override
    public int getLevelType2() {
      return MISSING;
    }

    @Override
    public int getTimeUnit() {
      return 0;
    }

    @Override
    public int getForecastTime() {
      return 0;
    }

    /**
     * Observation generating process identifier (defined by originating centre)
     *
     * @return GenProcess
     */
    public int getGenProcessId() {
      return getOctet(13);
    }

    /**
     * Number of contributing spectral bands (NB)
     *
     * @return Number of contributing spectral
     */
    public int getNumSatelliteBands() {
      return getOctet(14);
    }

    /**
     * SatelliteBand
     *
     * @return SatelliteBands
     */
    public SatelliteBand[] getSatelliteBands() {
      int nb = getNumSatelliteBands();
      SatelliteBand[] result = new SatelliteBand[nb];
      int pos = 15;
      for (int i = 0; i < nb; i++) {
        SatelliteBand sb = new SatelliteBand();
        sb.number = GribNumbers.int2(getOctet(pos), getOctet(pos + 1));
        sb.series = GribNumbers.int2(getOctet(pos + 2), getOctet(pos + 3));
        sb.instrumentType = getOctet(pos + 4);
        int scaleFactor = getOctet(pos + 5);
        int svalue = GribNumbers.int4(getOctet(pos + 6), getOctet(pos + 7), getOctet(pos + 8), getOctet(pos + 9));
        sb.value = applyScaleFactor(scaleFactor, svalue);
        pos += 10;
        result[i] = sb;
      }
      return result;
    }

    public int templateLength() {
      return 14 + getNumSatelliteBands() * 10;
    }
  }

  static public class SatelliteBand {
    public int series; // Satellite series of band nb (code table defined by originating/generating centre)
    public int number; // Satellite numbers of band nb (code table defined by originating/generating centre)
    public int instrumentType; // Instrument types of band nb (code table defined by originating/generating centre)
    public double value; // value of central wave number of band nb (units: m**-1)
  }

  /////////////////////////////////////////

  // translate 7 byte time into Date millisecs

  long calcTime(long refTime, Calendar calendar, int startIndex) {

    int year = GribNumbers.int2(getOctet(startIndex++), getOctet(startIndex++));
    int month = getOctet(startIndex++);
    int day = getOctet(startIndex++);
    int hour = getOctet(startIndex++);
    int minute = getOctet(startIndex++);
    int second = getOctet(startIndex++);

    if ((year == 0) && (month == 0) && (day == 0) && (hour == 0) && (minute == 0) && (second == 0)) return refTime;

    calendar.clear();
    calendar.setTimeZone(java.util.TimeZone.getTimeZone("GMT"));
    calendar.set(Calendar.DST_OFFSET, 0);
    calendar.set(year, month-1, day, hour, minute, second);

    return calendar.getTimeInMillis();
  }

  double applyScaleFactor(int scale, int value) {
    return ((scale == 0) || (value == 0)) ? value : value * Math.pow(10, -scale);
  }

  TimeInterval[] readTimeIntervals(int n, int startIndex) {
    TimeInterval[] result = new TimeInterval[n];
    for (int i = 0; i < n; i++) {
      TimeInterval ti = new TimeInterval();
      ti.statProcessType = getOctet(startIndex++);
      ti.timeIncrementType = getOctet(startIndex++);
      ti.timeRangeUnit = getOctet(startIndex++);
      ti.timeRangeLength = GribNumbers.int4(getOctet(startIndex++), getOctet(startIndex++), getOctet(startIndex++), getOctet(startIndex++));
      ti.timeIncrementUnit = getOctet(startIndex++);
      ti.timeIncrement = GribNumbers.int4(getOctet(startIndex++), getOctet(startIndex++), getOctet(startIndex++), getOctet(startIndex++));
      result[i] = ti;
    }
    return result;
  }

  static public class TimeInterval {
    public int statProcessType; // (code table 4.10) Statistical process used to calculate the processed field from the field at each time increment during the time range
    public int timeIncrementType;  // (code table 4.11) Type of time increment between successive fields used in the statistical processing<
    public int timeRangeUnit;  // (code table 4.4) Indicator of unit of time for time range over which statistical processing is done
    public int timeRangeLength; // Length of the time range over which statistical processing is done, in units defined by the previous octet
    public int timeIncrementUnit; // (code table 4.4) Indicator of unit of time for the increment between the successive fields used
    public int timeIncrement; // Time increment between successive fields, in units defined by the previous octet

    public void show(Formatter f) {
      f.format("TimeInterval: statProcessType= %d, timeIncrementType= %d, timeRangeUnit= %d, timeRangeLength= %d, timeIncrementUnit= %d, timeIncrement=%d%n",
              statProcessType, timeIncrementType, timeRangeUnit, timeRangeLength, timeIncrementUnit, timeIncrement);
    }
  }


  /////////////////////////////////////////////////////////////
  // old

  /* calculateForecast.
  * @param idx where to start in the byte[]
  * @param forecastTime initial forecast time
  * @return calculateForecast as int
  *

  private int calculateForecast(int idx, int forecastTime) {

    // 42 for Product Definition 8
    int timeRanges = getOctet(idx++);
    // 43 - 46
    int missingDataValues = GribNumbers.int4(getOctet(idx++), getOctet(idx++), getOctet(idx++), getOctet(idx++));

    int[] timeIncrement = new int[timeRanges * 6];
    for (int t = 0; t < timeRanges; t++) {
      // 47 statProcess
      timeIncrement[t * 6] = getOctet(idx++);
      // 48  timeType
      timeIncrement[t * 6 + 1] = getOctet(idx++);
      // 49   time Unit
      timeIncrement[t * 6 + 2] = getOctet(idx++);
      // 50 - 53 timeIncrement
      timeIncrement[t * 6 + 3] = GribNumbers.int4(getOctet(idx++), getOctet(idx++), getOctet(idx++), getOctet(idx++));
      // 54 time Unit
      timeIncrement[t * 6 + 4] = getOctet(idx++);
      // 55 - 58 timeIncrement
      timeIncrement[t * 6 + 5] = GribNumbers.int4(getOctet(idx++), getOctet(idx++), getOctet(idx++), getOctet(idx++));
    }
    // modify forecast time to reflect the end of the
    // interval according to timeIncrement information.
    // http://www.nco.ncep.noaa.gov/pmb/docs/grib2/grib2_temp4-8.shtml starting byte 42
    if (timeRanges == 1) {
      forecastTime += calculateIncrement(timeIncrement[2], timeIncrement[3]);
    } else if (timeRanges == 2) {
      // sample data is Monthly Climate Forecast System Reanalysis(CFSR) data
      // check statistical processing and if the time units are the same
      // http://www.nco.ncep.noaa.gov/pmb/docs/grib2/grib2_table4-10.shtml
      if (timeIncrement[0] == 194 && timeIncrement[2] == timeIncrement[4]) {
        int len = timeIncrement[3] * timeIncrement[5];
        forecastTime += calculateIncrement(timeIncrement[2], len);
      } else {
        log.error("Grib2PDSVariable: time ranges = 2 with not equal time units not implemented");
        forecastTime = GribNumbers.UNDEFINED;
      }
    } else { // throw flag
      log.error("Grib2PDSVariable: time ranges > 2 not implemented");
      forecastTime = GribNumbers.UNDEFINED;
    }
    return forecastTime;
  }

  /*
   * forecastTimeInterval for accumulation type variables.
   *
   * @return ForecastTimeInterval int[]  start, end of interval
   *
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


   * calculates the increment between time intervals
   *
   * @param tui    time unit indicator,
   * @param length of interval
   * @return increment
   *
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

  } */

/**
 * End Time Interval for productDefinition 8-14, 42 and 43 type variables.
 *
 * @return EndTimeInterval long
 *
public final long getEndTimeInterval() {

int idx;
switch (template) {

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

int year = GribNumbers.int2( getOctet(idx++), getOctet(idx++) );
int month = getOctet(idx++) -1;
int day = getOctet(idx++);
int hour = getOctet(idx++);
int minute = getOctet(idx++);
int second = getOctet(idx++);
Calendar calendar = Calendar.getInstance();
calendar.clear();
calendar.setTimeZone(java.util.TimeZone.getTimeZone("GMT"));
calendar.set(Calendar.DST_OFFSET, 0);
calendar.set(year, month, day, hour, minute, second);

//System.out.println( "End of Time Interval from octets 35-41 "+ calendar.getTime() );
long refTime = calendar.getTimeInMillis();

return refTime;
}  */


}
