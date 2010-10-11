package ucar.grib;

import ucar.grib.grib1.Grib1Pds;
import ucar.grib.grib2.Grib2Pds;

import java.io.IOException;
import java.util.Calendar;
import java.util.Date;

/**
 * Grib1 and Grib 2 PDS superclass.
 * Try to abstract the common fields.
 * This is fairly low-level, corresponding to the actual values in the file.
 * Meaning often depends if its GRIB1 or GRIB2
 * GridRecord translates these into values that are file type independdednt.
 *
 * @author caron
 * @since Aug 4, 2010
 */
public abstract class GribPds {

  static public GribPds factory(int edition, byte[] raw, long baseTime, Calendar cal) throws IOException {
    if (edition == 1)
      return new Grib1Pds(raw, cal);
    else
      return Grib2Pds.factory(raw, baseTime, cal);
  }

  /////////////////////////
  protected byte[] input;

  /**
   * PDS as byte[]
   * @return PDS as byte[]
   */
  public byte[] getPDSBytes() {
    return input;
  }

  /**
   * Get the indexth byte in the PDS as an integer.
   * THIS IS ONE BASED (not zero) to correspond with the manual
   *
   * @param index 1 based index
   * @return input[index-1] & 0xff
   */
  public final int getOctet(int index) {
    if (index > input.length) return GribNumbers.UNDEFINED;
    return input[index - 1] & 0xff;
  }

  ///////////////

  /**
   * Parameter number
   * @return Parameter number
   */
  abstract public int getParameterNumber();

   /**
   * Get the first level value
   *
   * @return the first level value
   */
   abstract public double getLevelValue1();

  /**
   * Get the second level
   *
   * @return the second level value
   */
  abstract public double getLevelValue2();

  /**
   * Get the type for the first level
   *
   * @return level type
   */
  abstract public int getLevelType1();

  /**
   * Get the type for the second level
   *
   * @return level type
   */
  abstract public int getLevelType2();

  /**
   * Time Unit code
   *
   * @return Time Unit code
   */
  abstract public int getTimeUnit();

  /**
   * Forecast time in units defined by getTimeUnit()) from getReferenceTime()
   *
   * @return Forecast time
   */
  abstract public int getForecastTime();

  /**
   * Forecast time as a Date
   *
   * @return Forecast Date
   */
  abstract public Date getForecastDate();

  /**
   * Reference time as a long millis.
   *
   * @return refTime
   */
  abstract public long getReferenceTime();

  /**
   * Reference time as a Date
   *
   * @return Reference Date
   */
  public final Date getReferenceDate() {
    return new Date(getReferenceTime());
  }

  /**
   * Is this a time interval. If so, then coordinate is a range [min, max].
   * Otherwise coord is assumed to be a point (instance) in time.
   * @return if time interval
   */
  abstract public boolean isInterval();

  /**
   * Get Grib-2 Interval Statistic Type code, only valid if isInterval()
   *
   * @return Grib-2 Interval Statistic Type (Table 4-10)
   */
  abstract public int getIntervalStatType();

  /**
   * Grib-2 encodes an "end of overall time interval" .
   * Grib-1 return < 0.
   * @return end of overall time interval
   */
  abstract public long getIntervalTimeEnd();

  /**
   * Get the time interval of the forecast.
   *
   * @return interval as int[2], or null if not isInterval()
   */
  abstract public int[] getForecastTimeInterval();

  abstract public int getGenProcessId(); 

  abstract public boolean isEnsemble();
  abstract public int getPerturbationNumber();
  abstract public int getPerturbationType();
  abstract public boolean isEnsembleDerived();
  abstract public int getNumberEnsembleForecasts();

  abstract public boolean isProbability();
  abstract  public double getProbabilityLowerLimit();
  abstract  public double getProbabilityUpperLimit();
  abstract public int getProbabilityType();

  abstract public boolean isPercentile();
  abstract public int getPercentileValue();

}
