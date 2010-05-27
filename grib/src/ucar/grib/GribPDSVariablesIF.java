/*
 *
 *
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

package ucar.grib;

/**
 * User: rkambic
 * Date: Jun 15, 2009
 * Time: 9:31:23 AM
 */

/**
 * A interface for handling Grib1 and Grib2 PDS variables from a byte[].
 */
public interface GribPDSVariablesIF {

  /**
   * PDS as a byte[]
   * @return  PDS bytes
   */
  public byte[] getPDSBytes();

  // getters for ProductDefinitions  Variables

  //  Length of PDS
  public int getLength();

  /**
   * Number of this section .
   * @return section number
   */
  public int getSection();


  /**
   * Number of this coordinates.
   *
   * @return Coordinates number
   */
  public int getCoordinates();


   /**
   * productDefinition.
   *
   * @return ProductDefinition
   */
  public int getProductDefinition();

   /**
   * parameter Category .
   *
   * @return parameterCategory as int
   */
  public int getParameterCategory();


  /**
   * parameter Number.
   *
   * @return ParameterNumber
   */
  public int getParameterNumber();


  /**
   * type of Generating Process.
   *
   * @return GenProcess
   */
  public int getTypeGenProcess();


  /**
   * ChemicalType.
   *
   * @return ChemicalType
   */
  public int getChemicalType();

  /**
   * backGenProcess.
   *
   * @return BackGenProcess
   */
  public int getBackGenProcess();

  /**
   * ObservationProcess.
   *
   * @return ObservationProcess
   */
  public int getObservationProcess();

  /**
   * Number Bands.
   *
   * @return NB
   */
  public int getNB();

  /**
   * analysisGenProcess.
   *
   * @return analysisGenProcess
   */
  public int getAnalysisGenProcess();


  /**
   * hoursAfter.
   *
   * @return HoursAfter
   */
  public int getHoursAfter();

  /**
   * minutesAfter.
   *
   * @return MinutesAfter
   */
  public int getMinutesAfter();

  /**
   * returns timeRangeUnit .
   *
   * @return TimeRangeUnitName
   */
  public int getTimeRangeUnit();

  /**
   * forecastTime.
   *
   * @return ForecastTime
   */
  public int getForecastTime();


  /**
   * typeFirstFixedSurface.
   *
   * @return FirstFixedSurface as int
   */
  public int getTypeFirstFixedSurface();


  /**
   * FirstFixedSurfaceValue
   * @return float FirstFixedSurfaceValue
   */
  public float getValueFirstFixedSurface();

  /**
   * typeSecondFixedSurface.
   *
   * @return SecondFixedSurface as int
   */
  public int getTypeSecondFixedSurface();

  /**
   * SecondFixedSurfaceValue
   * @return float SecondFixedSurfaceValue
   */
  public float getValueSecondFixedSurface();

  /**
   * Ensemble Type information
   *
   * @return int Type Derived, Ensemble, or Probability
   */
  public int getType ();

  /**
   * ForecastProbability.
   *
   * @return int ForecastProbability
   */
  public int getForecastProbability();


  /**
   * ForecastPercentile.
   *
   * @return int ForecastProbability
   */
  public int getForecastPercentile();


  /**
   * Perturbation number
   * @return int Perturbation
   */
  public int getPerturbation();

  /**
   * number of forecasts/members.
   *
   * @return int
   */
  public int getNumberForecasts();

  /**
   * ValueLowerLimit
   * @return float ValueLowerLimit
   */
  public float getValueLowerLimit();


  /**
   * ValueUpperLimit
   * @return float ValueUpperLimit
   */
  public float getValueUpperLimit();

}
