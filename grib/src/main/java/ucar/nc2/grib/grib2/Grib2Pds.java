package ucar.nc2.grib.grib2;

import net.jcip.annotations.Immutable;
import ucar.nc2.grib.GribNumbers;
import ucar.nc2.time.CalendarDate;
import ucar.unidata.util.Format;
import ucar.unidata.util.StringUtil2;

import java.util.Formatter;
import java.util.zip.CRC32;

/**
 * Abstract superclass for GRIB2 PDS handling.
 * Inner classes are specific to each template.
 *
 * To add a new template:
 *  1) add Grib2PdsXX class
 *  2)
 *
 * @author caron
 * @since 3/28/11
 */

@Immutable
public abstract class Grib2Pds {
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(Grib2Pds.class);

  /**
   * Factory for Grib2Pds
   *
   * @param template pds template number
   * @param input   raw bytes
   * @return Grib2Pds or null on error
   */
  static public Grib2Pds factory(int template, byte[] input) {
    switch (template) {
      case 0:
        return new Grib2Pds0(input);
      case 1:
        return new Grib2Pds1(input);
      case 2:
        return new Grib2Pds2(input);
      case 5:
        return new Grib2Pds5(input);
      case 6:
        return new Grib2Pds6(input);
      case 8:
        return new Grib2Pds8(input);
      case 9:
        return new Grib2Pds9(input);
      case 10:
        return new Grib2Pds10(input);
      case 11:
        return new Grib2Pds11(input);
      case 12:
        return new Grib2Pds12(input);
      case 15:
        return new Grib2Pds15(input);
      case 30:
        return new Grib2Pds30(input);
      case 31:
        return new Grib2Pds31(input);
      case 48:
          return new Grib2Pds48(input);
      case 61:
          return new Grib2Pds61(input);
      default:
        log.warn("Missing template " + template);
        return null;
    }
  }

  /////////////////////////////////////////////////
  protected final byte[] input;
  protected final int template; // product definition template

  /**
   * Constructs a Grib2PDSVariables object from a byte[].
   *
   * @param input   raw bytes
   */
  protected Grib2Pds(byte[] input) {
    this.input = input;
    template = GribNumbers.int2(getOctet(8), getOctet(9));
  }

  // optional coordinates start after this
  public abstract int templateLength();

  /**
   * Number of coordinate values at end of template.
   *
   * @return Coordinates number
   */
  public int getExtraCoordinatesCount() {
    return GribNumbers.int2(getOctet(6), getOctet(7));
  }

  public final float[] getExtraCoordinates() {
    int n =  getExtraCoordinatesCount();
    if (n == 0) return null;
    float[] result = new float[n];
    int count = templateLength() + 1;
    for (int i=0; i<n; i++) {
      result[i] = GribNumbers.float4(getOctet(count++),getOctet(count++),getOctet(count++),getOctet(count++));
    }
    return result;
  }

  /**
   * product Definition template, Table 4.0
   *
   * @return ProductDefinition
   */
  public final int getTemplateNumber() {
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
    return GribNumbers.UNDEFINED;
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
    return GribNumbers.UNDEFINED;
  }

  /**
    Get Background generating process identifier (defined by originating centre)
   @return Background generating process identifier
   */
   public int getBackProcessId() {
     return GribNumbers.UNDEFINED;
  }

  /**
   * Indicator of unit of time range (see Code table 4.4)
   * @return unit of time range
   */
  public abstract int getTimeUnit();

  /**
   * Forecast time in units of getTimeUnit()
   * forecast time for points, should not be used for intervals
   * @return Forecast time
   */
  public int getForecastTime() {
    return GribNumbers.int4(getOctet(19), getOctet(20), getOctet(21), getOctet(22));
  }

  public double getLevelValue1() {
    return GribNumbers.UNDEFINED;
  }

  public int getLevelScale1() {
    return GribNumbers.UNDEFINED;
  }

  public double getLevelValue2() {
    return GribNumbers.UNDEFINED;
  }

  public int getLevelType1() {
    return GribNumbers.UNDEFINED;
  }

  public int getLevelType2() {
    return GribNumbers.UNDEFINED;
  }

  public int getLevelScale2() {
    return GribNumbers.UNDEFINED;
  }

  public boolean isAerosol() {
    return (template == 48);
  }

  public boolean isEnsemble() {
    return false;
  }

  public boolean isEnsembleDerived() {
    return false;
  }

  public boolean isProbability() {
    return false;
  }

  public boolean isTimeInterval() {
    return (template >= 8) && (template <= 14);
  }

  public boolean isSpatialInterval() {
    return (template == 15);
  }

  /* public int getPerturbationNumber() {
    return GribNumbers.UNDEFINED;
  }

  public int getPerturbationType() {
    return GribNumbers.UNDEFINED;
  }

  public boolean isEnsembleDerived() {
    return false;
  }

  public int getNumberEnsembleForecasts() {
    return GribNumbers.UNDEFINED;
  }   */

  /* public double getProbabilityLowerLimit() {
    return GribNumbers.UNDEFINED;
  }

  public double getProbabilityUpperLimit() {
    return GribNumbers.UNDEFINED;
  }

  public int getProbabilityType() {
    return GribNumbers.UNDEFINED;
  }

  public boolean isPercentile() {
    return false;
  }

  public int getPercentileValue() {
    return -1;
  } */


  public void show(Formatter f) {
    f.format("Grib2Pds{ id=%d-%d template=%d, forecastTime= %d timeUnit=%s vertLevel=%f}", getParameterCategory(), getParameterNumber(),
            template, getForecastTime(), getTimeUnit(), getLevelValue1());
  }

  /**
   * Get the indexth byte in the PDS as an integer.
   * THIS IS ONE BASED (not zero) to correspond with the manual
   *
   * @param index 1 based index
   * @return input[index-1] & 0xff
   */
  public final int getOctet(int index) {
    //if (index > input.length) return GribNumbers.UNDEFINED; // allow exception
    return input[index - 1] & 0xff;
  }

  public final int getOctetSigned(int index) {
    return GribNumbers.convertSignedByte(input[index - 1]);
  }

  public final int getRawLength() {
    return input.length;
  }


  protected double getScaledValue(int start) {
    int scale = getOctetSigned(start++);
    int value = GribNumbers.int4(getOctet(start++), getOctet(start++), getOctet(start++), getOctet(start++));
    return applyScaleFactor(scale, value);
  }

  public int getStatisticalProcessType() {
    if (!(this instanceof PdsInterval)) return -1;
    PdsInterval pint = (PdsInterval) this;
    TimeInterval[] ti = pint.getTimeIntervals();
    if (ti.length > 0) {
      TimeInterval ti0 = ti[0];
      return ti0.statProcessType;
    }
    return -1;
  }

  //////////////////////////////////////////////////////////////////////////////////////////////

  static public interface PdsAerosol {
    public int getAerosolType() ;
    public double getAerosolIntervalSizeType();
    public double getAerosolSize1();
    public double getAerosolSize2();
    public double getAerosolIntervalWavelengthType();
    public double getAerosolWavelength1();
    public double getAerosolWavelength2();
  }

  static public interface PdsInterval {
    public int getStatisticalProcessType();
    public CalendarDate getIntervalTimeEnd();
    public int getForecastTime();
    public int getNumberTimeRanges();
    public int getNumberMissing();
    public TimeInterval[] getTimeIntervals();
    public long getIntervalHash();
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
    public int getProbabilityHashcode();
    public String getProbabilityName();
  }

  //////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * Product definition template 4.0 - analysis or forecast at a horizontal level or in a horizontal layer at a point in time
   * Many other templates (1-14) have same fields in sameplaces, so can use this as the superclass.
   */
  static private class Grib2Pds0 extends Grib2Pds {

    Grib2Pds0(byte[] input) {
      super(input);
    }

    @Override
    public int getGenProcessType() {
      return getOctet(12);
    }

    /**
     * Background generating process identifier (defined by originating centre)
     *
     * @return Background generating process id
     */
    @Override
    public int getBackProcessId() {
      return getOctet(13);
    }

    @Override
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
    public int getLevelType1() {
      return getOctet(23);
    }

    /**
     * Value of first fixed surface, with scale factor already applied
     *
     * @return float FirstFixedSurfaceValue
     */
    public double getLevelValue1() {
      return getScaledValue(24);
    }

    // debug
    public int getLevelScale() {
      return getOctet(24);
    }

    /**
     * Type of second fixed surface (see Code table 4.5)
     *
     * @return Type of second fixed surface
     */
    public int getLevelType2() {
      return getOctet(29);
    }

    /**
     * Value of second fixed surface, with scale factor already applied
     *
     * @return float FirstFixedSurfaceValue
     */
    public double getLevelValue2() {
      return getScaledValue(30);
    }

    /**
     * length of template (next byte starts info after template, if any
     *
     * @return length of template
     */
    public int templateLength() {
      return 34;
    }

  }

  //////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * Product definition template 4.15 - average, accumulation, extreme values, or other statistically-processed
   * values over a spatial area at a horizontal level or in a horizontal layer at a point in time
   */
  static private class Grib2Pds15 extends Grib2Pds0 {
    Grib2Pds15(byte[] input) {
      super(input);
    }

    // table 4.10
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
    Grib2Pds1(byte[] input) {
      super(input);
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

    Grib2Pds11(byte[] input) {
      super(input);
    }

    /**
     * End of overall time interval
     *
     * @return End of overall time interval
     */
    public CalendarDate getIntervalTimeEnd() {
      return calcTime(38);
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

    public long getIntervalHash() {
      CRC32 crc32 = new CRC32();
      crc32.update(input, 49, getNumberTimeRanges() * 12);
      return crc32.getValue();
    }

     public void show(Formatter f) {
      super.show(f);
      f.format("%n   Grib2Pds8: endInterval=%s%n", getIntervalTimeEnd());
      for (TimeInterval ti : getTimeIntervals()) {
        ti.show(f);
      }
    }
  }

  //////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * Product definition template 4.61 -
   * individual ensemble forecast, control and perturbed, at a horizontal level or in a horizontal layer in a continuous
   * or non-continuous time interval
   */
  static private class Grib2Pds61 extends Grib2Pds1 implements PdsInterval{

    Grib2Pds61(byte[] input) {
      super(input);
    }

    /**
     * Model version date
     *
     * @return Model version date
     */
    public CalendarDate getModelVersionDate() {
      return calcTime(38);
    }

    /**
     * End of overall time interval
     *
     * @return End of overall time interval
     */
    public CalendarDate getIntervalTimeEnd() {
      return calcTime(45);
    }

    /**
     * number of time range specifications describing the time intervals used to calculate the statistically-processed field
     *
     * @return number of time range
     */
    public int getNumberTimeRanges() {
      return getOctet(52);
    }

    /**
     * Total number of data values missing in statistical process
     *
     * @return Total number of data values missing in statistical process
     */
    public final int getNumberMissing() {
      return GribNumbers.int4(getOctet(53), getOctet(54), getOctet(55), getOctet(56));
    }

    public TimeInterval[] getTimeIntervals() {
      return readTimeIntervals(getNumberTimeRanges(), 57);
    }

    @Override
    public int templateLength() {
      return 56 + getNumberTimeRanges() * 12;
    }

    public long getIntervalHash() {
      CRC32 crc32 = new CRC32();
      crc32.update(input, 56, getNumberTimeRanges() * 12);
      return crc32.getValue();
    }

     public void show(Formatter f) {
      super.show(f);
      f.format("%n   Grib2Pds8: endInterval=%s%n", getIntervalTimeEnd());
      for (TimeInterval ti : getTimeIntervals()) {
        ti.show(f);
      }
    }
  }

  
  //////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * Product definition template 4.2 -
   * derived forecasts based on all ensemble members at a horizontal level or in a horizontal layer at a point in time
   */
  static private class Grib2Pds2 extends Grib2Pds0 implements PdsEnsembleDerived {

    Grib2Pds2(byte[] input) {
      super(input);
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
    public int getNumberEnsembleForecasts() {
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
    //CalendarDate endInterval; // Date msecs
    //int ft;

    Grib2Pds12(byte[] input) {
      super(input);
      //endInterval = calcTime(37);
      //ft = makeForecastTime(endInterval, getTimeUnit());
    }

    /**
     * End of overall time interval
     *
     * @return End of overall time interval
     */
    public CalendarDate getIntervalTimeEnd() {
      return calcTime(37);
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

    public long getIntervalHash() {
      CRC32 crc32 = new CRC32();
      crc32.update(input, 48, getNumberTimeRanges() * 12);
      return crc32.getValue();
    }

    public void show(Formatter f) {
      super.show(f);
      f.format("%n   Grib2Pds8: endInterval=%s%n", getIntervalTimeEnd());
      for (TimeInterval ti : getTimeIntervals()) {
        ti.show(f);
      }
    }
  }

  //////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * Product definition template 4.5 – probability forecasts at a horizontal level or in a horizontal layer at a point in time
   Octet No. Contents
   10 Parameter category (see Code table 4.1)
   11 Parameter number (see Code table 4.2)
   12 Type of generating process (see Code table 4.3)
   13 Background generating process identifier (defined by originating centre)
   14 Forecast generating process identifier (defined by originating centre)
   15–16 Hours after reference time of data cut-off (see Note)
   17 Minutes after reference time of data cut-off
   18 Indicator of unit of time range (see Code table 4.4)
   19–22 Forecast time in units defined by octet 18
   23 Type of first fixed surface (see Code table 4.5)
   24 Scale factor of first fixed surface
   25–28 Scaled value of first fixed surface
   29 Type of second fixed surface (see Code table 4.5)
   30 Scale factor of second fixed surface
   31–34 Scaled value of second fixed surface
   35 Forecast probability number
   36 Total number of forecast probabilities
   37 Probability type (see Code table 4.9)
   38 Scale factor of lower limit
   39–42 Scaled value of lower limit
   43 Scale factor of upper limit
   44–47 Scaled value of upper limit
   Note: Hours greater than 65534 will be coded as 65534
   */
  static private class Grib2Pds5 extends Grib2Pds0 implements PdsProbability {

    Grib2Pds5(byte[] input) {
      super(input);
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
      int scale = getOctetSigned(38);
      int value = GribNumbers.int4(getOctet(39), getOctet(40), getOctet(41), getOctet(42));
      return applyScaleFactor(scale, value);
    }

    public double getProbabilityUpperLimit() {
      int scale = getOctetSigned(43);
      int value = GribNumbers.int4(getOctet(44), getOctet(45), getOctet(46), getOctet(47));
      return applyScaleFactor(scale, value);
    }


    @Override
    public int getProbabilityHashcode() {
      if (probHash == 0) {
        int result = 0;
        long temp;
        double prob1, prob2;

        switch (getProbabilityType()) {
          case 0:
          case 3:
            prob1 = getProbabilityLowerLimit();
            temp = prob1 != +0.0d ? Double.doubleToLongBits(prob1) : 0L;
            result = (int) (temp ^ (temp >>> 32));
            break;

          case 1:
          case 4:
            prob2 = getProbabilityUpperLimit();
            temp = prob2 != +0.0d ? Double.doubleToLongBits(prob2) : 0L;
            result = (int) (temp ^ (temp >>> 32));
            break;

          case 2:
            prob1 = getProbabilityLowerLimit();
            prob2 = getProbabilityUpperLimit();
            temp = prob1 != +0.0d ? Double.doubleToLongBits(prob1) : 0L;
            result = (int) (temp ^ (temp >>> 32));
            temp = prob2 != +0.0d ? Double.doubleToLongBits(prob2) : 0L;
            result = 31 * result + (int) (temp ^ (temp >>> 32));
            break;

          default:
        }
        result = 31 * result + getProbabilityType();
        probHash = result;
      }
      return probHash;
    }
    int probHash = 0;

        /*
    Code Table Code table 4.9 - Probability type (4.9)
        0: Probability of event below lower limit
        1: Probability of event above upper limit
        2: Probability of event between lower and upper limits (the range includes the lower limit but not the upper limit)
        3: Probability of event above lower limit
        4: Probability of event below upper limit
       -1: Reserved
       -1: Reserved for local use
      255: Missing
     */
    @Override
    public String getProbabilityName() {
      Formatter f = new Formatter();
      int scale1 = Math.max(1, getOctet(38));
      int scale2 = Math.max(1, getOctet(43));

      switch (getProbabilityType()) {
        case 0:
          f.format("below_%s", Format.dfrac(getProbabilityLowerLimit(), scale1));
          break;
        case 1:
          f.format("above_%s", Format.dfrac(getProbabilityUpperLimit(), scale2));
          break;
        case 2:
          if (getProbabilityLowerLimit() == getProbabilityUpperLimit())
            f.format("equals_%s", Format.dfrac(getProbabilityLowerLimit(), scale1));
          else
            f.format("between_%s_and_%s", Format.dfrac(getProbabilityLowerLimit(), scale1), Format.dfrac(getProbabilityUpperLimit(), scale2));
          break;
        case 3:
          f.format("above_%s", Format.dfrac(getProbabilityLowerLimit(), scale1));
          break;
        case 4:
          f.format("below_%s", Format.dfrac(getProbabilityUpperLimit(), scale2));
          break;
        default:
          f.format("UknownProbType=%d", getProbabilityType());
      }

      String result = StringUtil2.removeFromEnd(f.toString(), '0');
      return StringUtil2.removeFromEnd(result, '.');
    }

    @Override
    public int templateLength() {
      return 47;
    }
  }

  //////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * Product definition template 4.9 – probability forecasts at a horizontal level or in a horizontal layer in a continuous or non-continuous time interval
   Octet No. Contents
   10 Parameter category (see Code table 4.1)
   11 Parameter number (see Code table 4.2)
   12 Type of generating process (see Code table 4.3)
   13 Background generating process identifier (defined by originating centre)
   14 Forecast generating process identifier (defined by originating centre)
   15–16 Hours after reference time of data cut-off (see Note 1)
   17 Minutes after reference time of data cut-off
   18 Indicator of unit of time range (see Code table 4.4)
   19–22 Forecast time in units defined by octet 18 (see Note 2)
   23 Type of first fixed surface (see Code table 4.5)
   24 Scale factor of first fixed surface
   25–28 Scaled value of first fixed surface
   29 Type of second fixed surface (see Code table 4.5)
   30 Scale factor of second fixed surface
   31–34 Scaled value of second fixed surface
   35 Forecast probability number
   36 Total number of forecast probabilities
   37 Probability type (see Code table 4.9)
   38 Scale factor of lower limit
   39–42 Scaled value of lower limit
   43 Scale factor of upper limit
   44–47 Scaled value of upper limit
   48–49 Year of end of overall time interval
   50 Month of end of overall time interval
   51 Day of end of overall time interval
   52 Hour of end of overall time interval
   53 Minute of end of overall time interval
   54 Second of end of overall time interval
   55 n – number of time range specifications describing the time intervals used to calculate the statistically processed field
   56–59 Total number of data values missing in the statistical process
   60–71 Specification of the outermost (or only) time range over which statistical processing is done
   60 Statistical process used to calculate the processed field from the field at each time increment during the time range (see Code table 4.10)
   61 Type of time increment between successive fields used in the statistical processing (see Code table 4.11)
   62 Indicator of unit of time for time range over which statistical processing is done (see Code table 4.4)
   63–66 Length of the time range over which statistical processing is done, in units defined by the previous octet
   67 Indicator of unit of time for the increment between the successive fields used (see Code table 4.4)
   68–71 Time increment between successive fields, in units defined by the previous octet (see Note 3)
   72–nn These octets are included only if n > 1, where nn = 59 + 12 x n
   72–83 As octets 60 to 71, next innermost step of processing
   84–nn Additional time range specifications, included in accordance with the value of n. Contents
   as octets 60 to 71, repeated as necessary.
   Notes:
   (1) Hours greater than 65534 will be coded as 65534.
   (2) The reference time in section 1 and the forecast time together define the beginning of the overall time interval.
   (3) An increment of zero means that the statistical processing is the result of a continuous (or near continuous) process, not
   the processing of a number of discrete samples. Examples of such continuous processes are the temperatures measured
   by analogue maximum and minimum thermometers or thermographs, and the rainfall measured by a raingauge.
   The reference and forecast times are successively set to their initial values plus or minus the increment, as defined by
   the type of time increment (one of octets 46, 58, 70, ...). For all but the innermost (last) time range, the next inner range is
   then processed using these reference and forecast times as the initial reference and forecast times.
   */
  static private class Grib2Pds9 extends Grib2Pds5 implements PdsInterval {
    // CalendarDate endInterval; // Date msecs
    //int ft;

    Grib2Pds9(byte[] input) {
      super(input);
      //endInterval = calcTime(48);
      //ft = makeForecastTime(endInterval, getTimeUnit());
    }

    /**
     * End of overall time interval
     *
     * @return End of overall time interval
     */
    public CalendarDate getIntervalTimeEnd() {
      return calcTime(48);
    }

    /* @Override
    public int getForecastTime() {
      return ft;
    } */

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

    public long getIntervalHash() {
      CRC32 crc32 = new CRC32();
      crc32.update(input, 59, getNumberTimeRanges() * 12);
      return crc32.getValue();
    }

    public void show(Formatter f) {
      super.show(f);
      f.format("%n   Grib2Pds9: endInterval=%s%n", getIntervalTimeEnd());
      for (TimeInterval ti : getTimeIntervals()) {
        ti.show(f);
      }
    }

  }

  //////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * Product definition template 4.8 -
   * Average, accumulation, extreme values or other statistically processed values at a horizontal level or in a
   * horizontal layer in a continuous or non-continuous time interval
   */
  static private class Grib2Pds8 extends Grib2Pds0 implements PdsInterval {
    // CalendarDate endInterval; // Date msecs

    Grib2Pds8(byte[] input) {
      super(input);
      //endInterval = calcTime(35);
    }

    /**
     * End of overall time interval
     *
     * @return End of overall time interval
     */
    @Override
    public CalendarDate getIntervalTimeEnd() {
      return calcTime(35);
    }

    /**
     * number of time range specifications describing the time intervals used to calculate the statistically-processed field
     *
     * @return number of time range
     */
    @Override
    public int getNumberTimeRanges() {
      return getOctet(42);
    }

    /**
     * Total number of data values missing in statistical process
     *
     * @return Total number of data values missing in statistical process
     */
    @Override
    public final int getNumberMissing() {
      return GribNumbers.int4(getOctet(43), getOctet(44), getOctet(45), getOctet(46));
    }

    @Override
    public TimeInterval[] getTimeIntervals() {
      return readTimeIntervals(getNumberTimeRanges(), 47);
    }

    @Override
    public int templateLength() {
      return 46 + getNumberTimeRanges() * 12;
    }

    public void show(Formatter f) {
      super.show(f);
      try {
        f.format("%n   Grib2Pds8: endInterval=%s%n", getIntervalTimeEnd());
        for (TimeInterval ti : getTimeIntervals()) ti.show(f);
      } catch (Throwable t) {
        f.format("%n   Grib2Pds8: endInterval error=%s%n", t.getMessage());
      }
    }

    public long getIntervalHash() {
      CRC32 crc32 = new CRC32();
      crc32.update(input, 46, 12);
      if (getNumberTimeRanges() > 1)
        crc32.update(input, 58, 1);
      return crc32.getValue();
    }

  }

  //////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * Product definition template 4.6 -
   * percentile forecasts at a horizontal level or in a horizontal layer at a point in time
   */
  static private class Grib2Pds6 extends Grib2Pds0 implements PdsPercentile {

    Grib2Pds6(byte[] input) {
      super(input);
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
    // CalendarDate endInterval;

    Grib2Pds10(byte[] input) {
      super(input);
      //endInterval = calcTime(36);
    }

    /**
     * End of overall time interval
     *
     * @return End of overall time interval
     */
    public CalendarDate getIntervalTimeEnd() {
      return calcTime(36);
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

    public long getIntervalHash() {
      CRC32 crc32 = new CRC32();
      crc32.update(input, 48, getNumberTimeRanges() * 12);
      return crc32.getValue();
    }

  }


  //////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * Product definition template 4.30 - satellite product
   *
   * @deprecated  4.31 should be used
   */
  static private class Grib2Pds30 extends Grib2Pds {

    Grib2Pds30(byte[] input) {
      super(input);
      log.debug("Product Definition Template 4.30 is deprecated in favor of 4.31 (WMO Manual on Codes");
    }

    // LOOK - could put this into a dummy superclass in case others need

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
        int scaleFactor = getOctetSigned(pos + 5);
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

    //////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Product definition template 4.31 - satellite product
     */
    static private class Grib2Pds31 extends Grib2Pds {

        static int octetsPerBand = 11;

        Grib2Pds31(byte[] input) {
            super(input);
        }

        // LOOK - could put this into a dummy superclass in case others need

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
                sb.instrumentType = GribNumbers.int2(getOctet(pos + 4), getOctet(pos + 5));
                int scaleFactor = getOctetSigned(pos + 6);
                int svalue = GribNumbers.int4(getOctet(pos + 7), getOctet(pos + 8), getOctet(pos + 9), getOctet(pos +  10));
                sb.value = applyScaleFactor(scaleFactor, svalue);
                pos += octetsPerBand;
                result[i] = sb;
            }
            return result;
        }

        public int templateLength() {
            return 14 + getNumSatelliteBands() * octetsPerBand;
        }
    }

    ///////////////////////////////////////////////////////////////////////////////

  /* Product definition template 4.48 – analysis or forecast at a horizontal level or in a horizontal layer at a point in time for optical properties of aerosol
  Octet No. Contents
  10 Parameter category (see Code table 4.1)
  11 Parameter number (see Code table 4.2)
  12–13 Aerosol type (see Common Code table C–14)
  14 Type of interval for first and second size (see Code table 4.91)
  15 Scale factor of first size
  16–19 Scaled value of first size in metres
  20 Scale factor of second size
  21–24 Scaled value of second size in metres
  25 Type of interval for first and second wavelength (see Code table 4.91)
  26 Scale factor of first wavelength
  27–30 Scaled value of first wavelength in metres
  31 Scale factor of second wavelength
  32–35 Scaled value of second wavelength in metres
  36 Type of generating process (see Code table 4.3)
  37 Background generating process identifier (defined by originating centre)
  38 Analysis or forecast generating process identifier (defined by originating centre)
  39–40 Hours of observational data cut-off after reference time (see Note)
  41 Minutes of observational data cut-off after reference time
  42 Indicator of unit of time range (see Code table 4.4)
  43–46 Forecast time in units defined by octet 42
  47 Type of first fixed surface (see Code table 4.5)
  48 Scale factor of first fixed surface
  49–52 Scaled value of first fixed surface
  53 Type of second fixed surface (see Code table 4.5)
  54 Scale factor of second fixed surface
  55–58 Scaled value of second fixed surface
  Note: Hours greater than 65534 will be coded as 65534.
  */

  static private class Grib2Pds48 extends Grib2Pds implements PdsAerosol {

    Grib2Pds48(byte[] input) {
      super(input);
    }

    //   12–13 Aerosol type (see Common Code table C–14)
    public int getAerosolType() {
      return GribNumbers.uint2(getOctet(12), getOctet(13));
    }

    //  14 Type of interval for first and second size (see Code table 4.91)
    public double getAerosolIntervalSizeType() {
      return getOctet(14);
    }

    //  15 Scale factor of first size
    //  16–19 Scaled value of first size in metres
    public double getAerosolSize1() {
      return getScaledValue(15);
    }

    //  20 Scale factor of second size
    //  21–24 Scaled value of second size in metres
    public double getAerosolSize2() {
      return getScaledValue(20);
    }

    //  25 Type of interval for first and second wavelength (see Code table 4.91)
    public double getAerosolIntervalWavelengthType() {
      return getOctet(25);
    }

    //  26 Scale factor of first wavelength
    //  27–30 Scaled value of first wavelength in metres
    public double getAerosolWavelength1() {
      return getScaledValue(26);
    }

    //  31 Scale factor of second wavelength
    //  32–35 Scaled value of second wavelength in metres
    public double getAerosolWavelength2() {
      return getScaledValue(31);
    }

    //  36 Type of generating process (see Code table 4.3)
    @Override
    public int getGenProcessType() {
      return getOctet(36);
    }

    //  37 Background generating process identifier (defined by originating centre)
    @Override
    public int getBackProcessId() {
      return getOctet(37);
    }

    //  38 Analysis or forecast generating process identifier (defined by originating centre)
    @Override
    public int getGenProcessId() {
      return getOctet(38);
    }

    //  39–40 Hours of observational data cut-off after reference time (see Note)
    //  Note: Hours greater than 65534 will be coded as 65534.
    public int getHoursAfterCutoff() {
      return GribNumbers.int2(getOctet(39), getOctet(40));
    }

    //  41 Minutes of observational data cut-off after reference time
    public int getMinutesAfterCutoff() {
      return getOctet(41);
    }

    //  42 Indicator of unit of time range (see Code table 4.4)
    @Override
    public int getTimeUnit() {
      return getOctet(42);
    }

    //  43–46 Forecast time in units defined by octet 42
    public int getForecastTime() {
      return GribNumbers.int4(getOctet(43), getOctet(44), getOctet(45), getOctet(46));
    }

    //  47 Type of first fixed surface (see Code table 4.5)
    @Override
    public int getLevelType1() {
      return getOctet(47);
    }

    //  48 Scale factor of first fixed surface
    @Override
    public int getLevelScale1() {
      return getOctet(48);
    }

    //  49–52 Scaled value of first fixed surface
    @Override
    public double getLevelValue1() {
      return getScaledValue(48);
    }

    //  53 Type of second fixed surface (see Code table 4.5)
    @Override
    public int getLevelType2() {
      return getOctet(53);
    }

    //  54 Scale factor of second fixed surface
    @Override
    public int getLevelScale2() {
      return getOctet(54);
    }

     // 55–58 Scaled value of second fixed surface
     @Override
     public double getLevelValue2() {
       return getScaledValue(54);
     }

    public int templateLength() {
      return 58;
    }
  }

  //////////////////////////////////////////////////////////////////////////////////

  // translate 7 byte time into CalendarDate
  // null means use refTime
 protected CalendarDate calcTime(int startIndex) {

    int year = GribNumbers.int2(getOctet(startIndex++), getOctet(startIndex++));
    int month = getOctet(startIndex++);
    int day = getOctet(startIndex++);
    int hour = getOctet(startIndex++);
    int minute = getOctet(startIndex++);
    int second = getOctet(startIndex++);

     // LOOK: is this cruft or official ?
     if ((year == 0) && (month == 0) && (day == 0) && (hour == 0) && (minute == 0) && (second == 0))
       return null;

   // href.t00z.prob.f36.grib2
     if (hour > 23) {
       day += (hour/24);
       hour = hour % 24;
     }

    return CalendarDate.of(null, year, month, day, hour, minute, second);
  }

  /**
   * Apply scale factor to value, return a double result.
   *
   * @param scale signed scale factor
   * @param value apply to this value
   * @return   value ^ -scale
   */
  double applyScaleFactor(int scale, int value) {
    return ((scale == 0) || (scale == 255) || (value == 0)) ? value : value * Math.pow(10, -scale);
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
      f.format("  TimeInterval: statProcessType= %d, timeIncrementType= %d, timeRangeUnit= %d, timeRangeLength= %d, timeIncrementUnit= %d, timeIncrement=%d%n",
              statProcessType, timeIncrementType, timeRangeUnit, timeRangeLength, timeIncrementUnit, timeIncrement);
    }
  }
}

/*

Hi John,

I did create the degrib program, but I'm fairly positive you're using your own software and are just wondering about how to interpret NDFD's encoding of the valid time.  The correct people to answer that would be the people who encoded the messages which is why I've included nws.ndfd@noaa.gov.  There probably should be a FAQ on this subject.

As you point out, the reference time (Ref) is easy (The reference time is 2010-09-21T12:00:00Z)
The forecast time (ForeT) can similarly be read (Forecast time in units defined by previous octet == 6) (Presumably the earlier units was hours)
The time range (range) also can be read (51: Length of the time range over which statistical processing is done, in units defined by the previous octet == 6)

So now the question is what does one do with these?  We know we have a 6 hour interval which starts or stops around Ref + ForeT.  So we could either have:
A) End of Interval = Ref + ForeT + range; Begin of Interval = Ref + ForeT
or
B) End of Interval = Ref + ForeT; Begin of Interval = Ref  + ForeT - range

The way NDFD decided to encode it is with B).  The reasoning was that the valid times are always the end of the period, so B made more sense to the folks encoding the NDFD.  Also, the logic went, if decoders were confused, then the "end of overall time interval" would clarify whether A) or B) was chosen.

So now we go back to the note:
> (2) The reference time in section 1 and the forecast time together
>      define the beginning of the overall time interval.
and try to determine whether the people encoding the message were in error.  It specifies that the two values together "define" the beginning, but it doesn't say that "the sum of those two values equals the beginning of the overall time interval".  If you think that is funky logic, consider the statement that "Pressure and Volume together define Temperature".  Does that mean P + V = T?  No, it meas P*V / nR = T.  So note 2, as written, unfortunately has a lot of wiggle room.

The result is that when I was writing degrib, I couldn't trust either equation A) or B), so I went with a simpler method... I simply read the end of the interval.  If there was a range I used:

End of interval (EI) = (bytes 36-42 show an "end of overall time interval")
C1) End of Interval = EI;      Begin of Interval = EI - range

and if there was no interval then I used:
C2) End of Interval = Begin of Interval = Ref + ForeT.

Using equations C1 and C2 has the elegance of being simple, and not having to convert the word 'define' into an equation.

Does that help?

Arthur
Arthur.Taylor@noaa.gov

 */
