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
 * Date: Jun 15, 2009
 * Time: 10:47:08 AM
 */

package ucar.grib;

import java.util.Date;
import java.util.Calendar;
import java.text.SimpleDateFormat;
import java.text.ParseException;

/**
 * Handles the data input from the text and the older binary type of Indexes for the
 * GribGridRecord.
 */
public class GribPDSVariables implements GribPDSVariablesIF {

  /**
   * _more_
   */
  public int productType, discipline, category, paramNumber;

  /**
   * _more_
   */
  public int typeGenProcess;

  /**
   * _more_
   */
  public int levelType1, levelType2;

  /**
   * _more_
   */
  public float levelValue1, levelValue2;

  /**
   * _more_
   */
  public int gdsKey;

  /**
   * _more_
   */
  public long offset1;

  /**
   * _more_
   */
  public long offset2;

  /**
   * _more_
   */
  public Date refTime;

  /**
   * _more_
   */
  public int forecastTime;

  /**
   * _more_
   */
  public int decimalScale = GribNumbers.UNDEFINED;

  /**
   * _more_
   */
  public boolean bmsExists = true;

  /**
   * _more_
   */
  public int center = -1, subCenter = -1, table = -1;

  /**
   * _more_
   */
  public int numberForecasts;

  /**
   * Type of ensemble or Probablity forecast
   */
  public int type = GribNumbers.UNDEFINED;

  /**
   * _more_
   */
  public float lowerLimit, upperLimit;

  /**
   * _more_
   */
  private Date validTime = null;

  /**
   * PDS as Variables from a byte[]
   */
   private final GribPDSVariablesIF pdsVars = null;

  /**
   * default constructor, used by GribReadIndex (binary indices)
   */
  public GribPDSVariables() {
  }

  /**
   * constructor given all parameters native. used to write indices
   *
   * @param productType
   * @param discipline
   * @param category
   * @param param
   * @param typeGenProcess
   * @param levelType1
   * @param levelValue1
   * @param levelType2
   * @param levelValue2
   * @param refTime
   * @param foreTime
   * @param gdsKey
   * @param offset1
   * @param offset2
   * @param decimalScale
   * @param bmsExists      either true or false bit-map exists
   * @param center
   * @param subCenter
   * @param table
   */
  public GribPDSVariables(
      Calendar calendar,
                        int productType, int discipline, int category,
                        int param, int typeGenProcess, int levelType1,
                        float levelValue1, int levelType2,
                        float levelValue2, Date refTime, int foreTime,
                        int gdsKey, long offset1, long offset2,
                        int decimalScale, boolean bmsExists,
                        int center, int subCenter, int table) {

    try {
      this.gdsKey = gdsKey;

      this.productType = productType;
      this.discipline = discipline;
      this.category = category;
      this.paramNumber = param;
      this.typeGenProcess = typeGenProcess;
      this.levelType1 = levelType1;
      this.levelValue1 = levelValue1;
      this.levelType2 = levelType2;
      this.levelValue2 = levelValue2;

      this.refTime = refTime;
      this.forecastTime = foreTime;
      calendar.setTime(refTime);
      calendar.add(Calendar.HOUR, forecastTime); // TODO: not always HOUR
      validTime = calendar.getTime();

      this.offset1 = offset1;
      this.offset2 = offset2;
      // only set for Grib1 Indexes
      if (decimalScale != GribNumbers.UNDEFINED) {
        this.decimalScale = decimalScale;
        this.bmsExists = bmsExists;
      }
      this.center = center;
      this.subCenter = subCenter;
      this.table = table;
    } catch (NumberFormatException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * constructor given all parameters as Strings. Used only by GribReadTextIndex
   *
   * @param calendar to convert to Dates
   * @param productTypeS
   * @param disciplineS
   * @param categoryS
   * @param paramS
   * @param typeGenProcessS
   * @param levelType1S
   * @param levelValue1S
   * @param levelType2S
   * @param levelValue2S
   * @param refTimeS
   * @param foreTimeS
   * @param gdsKeyS
   * @param offset1S
   * @param offset2S
   * @param decimalScaleS
   * @param bmsExistsS      either true or false bit-map exists
   * @param centerS
   * @param subCenterS
   * @param tableS
   */
  GribPDSVariables(Calendar calendar, SimpleDateFormat dateFormat,
                 String productTypeS, String disciplineS, String categoryS,
                 String paramS, String typeGenProcessS, String levelType1S,
                 String levelValue1S, String levelType2S,
                 String levelValue2S, String refTimeS, String foreTimeS,
                 String gdsKeyS, String offset1S, String offset2S,
                 String decimalScaleS, String bmsExistsS,
                 String centerS, String subCenterS, String tableS) {

    try {
      // old indexes used long, scale down to int
      //this.gdsKey = gdsKeyS.hashCode();
      this.gdsKey = Integer.parseInt(gdsKeyS);

      productType = Integer.parseInt(productTypeS);
      discipline = Integer.parseInt(disciplineS);
      category = Integer.parseInt(categoryS);
      paramNumber = Integer.parseInt(paramS);
      typeGenProcess = Integer.parseInt(typeGenProcessS);
      levelType1 = Integer.parseInt(levelType1S);
      levelValue1 = Float.parseFloat(levelValue1S);
      levelType2 = Integer.parseInt(levelType2S);
      levelValue2 = Float.parseFloat(levelValue2S);

      this.refTime = dateFormat.parse(refTimeS);
      forecastTime = Integer.parseInt(foreTimeS);
      calendar.setTime(refTime);
      calendar.add(Calendar.HOUR, forecastTime); // TODO: not always HOUR
      validTime = calendar.getTime();

      offset1 = Long.parseLong(offset1S);
      offset2 = Long.parseLong(offset2S);
      if (decimalScaleS != null) {
        decimalScale = Integer.parseInt(decimalScaleS);
      }
      if (bmsExistsS != null) {
        bmsExists = bmsExistsS.equals("true");
      }
      if (centerS != null) {
        center = Integer.parseInt(centerS);
      }
      if (subCenterS != null) {
        subCenter = Integer.parseInt(subCenterS);
      }
      if (tableS != null) {
        table = Integer.parseInt(tableS);
      }
    } catch (NumberFormatException e) {
      throw new RuntimeException(e);
    } catch (ParseException e) {
      throw new RuntimeException(e);
    }
  }


  // getters for the PDS variables.

  /**
   * PDS as byte[]
   */
  public byte[] getPDSBytes() {
    //return input;
    return null;
  }

  //  Length of PDS
  public final int getLength() {
    return GribNumbers.UNDEFINED;
  }

  /**
   * Number of this section .
   */
  public final int getSection() {
    return GribNumbers.UNDEFINED;
  }

  /**
   * Number of this coordinates.
   *
   * @return Coordinates number
   */
  public final int getCoordinates() {
    return   GribNumbers.UNDEFINED;
  }

   /**
   * productDefinition.
   *
   * @return ProductDefinition
   */
  public final int getProductDefinition() {
    return productType;
  }

   /**
   * parameter Category .
   *
   * @return parameterCategory as int
   */
  public final int getParameterCategory() {
    return category;
  }

  /**
   * parameter Number.
   *
   * @return ParameterNumber
   */
  public final int getParameterNumber() {
    return  paramNumber;
  }

  /**
   * type of Generating Process.
   *
   * @return GenProcess
   */
  public final int getTypeGenProcess() {
        return typeGenProcess;
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
        return GribNumbers.UNDEFINED;
  }

  /**
   * minutesAfter.
   *
   * @return MinutesAfter
   */
  public final int getMinutesAfter() {
        return GribNumbers.UNDEFINED;
  }

  /**
   * returns timeRangeUnit .
   *
   * @return TimeRangeUnitName
   */
  public final int getTimeRangeUnit() {
        return GribNumbers.UNDEFINED;
  }

  /**
   * forecastTime.
   *
   * @return ForecastTime
   */
  public final int getForecastTime() {
        return forecastTime;
  }

  /**
   * Get the first level of this GridRecord
   *
   * @return the first level value
   */
  public float getValueFirstFixedSurface() {
    return levelValue1;
  }

  /**
   * Get the second level of this GridRecord
   *
   * @return the second level value
   */
  public float getValueSecondFixedSurface() {
    return levelValue2;
  }

  /**
   * Get the type for the first level of this GridRecord
   *
   * @return level type
   */
  public int getTypeFirstFixedSurface() {
    return levelType1;
  }

  /**
   * Get the type for the second level of this GridRecord
   *
   * @return level type
   */
  public int getTypeSecondFixedSurface() {
    return levelType2;
  }

  /**
   * Get the first reference time of this GridRecord
   *
   * @return reference time
   */
  public Date getReferenceTime() {
    return refTime;
  }

  /**
   * Get the valid time for this grid.
   *
   * @return valid time
   */
  public Date getValidTime() {
    return validTime;
  }

  /**
   * _more_
   *
   * @param t _more_
   */
  public void setValidTime(Date t) {
    validTime = t;
  }

  /**
   * Get valid time offset (minutes) of this GridRecord
   *
   * @return time offset
   */
  //public int getValidTimeOffset() {
  //  return forecastTime;
  //}

  /**
   * Get the parameter name
   *
   * @return parameter name
   */
//  public String getParameterName() {
//    //return param;
//    return null;
//  }

  /**
   * Get the grid def record id
   *
   * @return parameter name
   */
//  public int getGridDefRecordIdInt() {
//    return gdsKey;
//  }
//
//  public String getGridDefRecordId() {
//    return Integer.toString( gdsKey );
//  }

  /**
   * Get the grid number
   *
   * @return grid number
   */
//  public int getGridNumber() {
//    //return gridNumber;
//    return 0;
//  }

  /**
   * Get the decimal scale
   *
   * @return decimal scale
   */
  public int getDecimalScale() {
    return decimalScale;
  }




  /**
   * Type of Derived   Code table 4.7.
   * Type of Ensemble  code table 4.6
   * Type of Probability  code table 4.9
   *
   * @return int Type Derived, Ensemble, or Probability
   */
  public final int getType () {
        return GribNumbers.UNDEFINED;
  }
  /**
   * ForecastProbability.
   *
   * @return int ForecastProbability
   */
  public final int getForecastProbability() {
        return GribNumbers.UNDEFINED;
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
   * number of forecasts.
   *
   * @return int
   */
  public final int getNumberForecasts() {
        return GribNumbers.UNDEFINED;
  }


  /**
   * ValueLowerLimit
   * @return float ValueLowerLimit
   */
  public final float getValueLowerLimit() {
        return GribNumbers.UNDEFINED;
  }


  /**
   * ValueUpperLimit
   * @return float ValueUpperLimit
   */
  public final float getValueUpperLimit() {
        return GribNumbers.UNDEFINED;

  }



  @Override
  public String toString() {
    return "GribGridRecord{" +
            "productType=" + productType +
            ", discipline=" + discipline +
            ", category=" + category +
            ", paramNumber=" + paramNumber +
            ", typeGenProcess=" + typeGenProcess +
            ", levelType1=" + levelType1 +
            ", levelType2=" + levelType2 +
            ", levelValue1=" + levelValue1 +
            ", levelValue2=" + levelValue2 +
            ", gdsKey=" + gdsKey +
            ", offset1=" + offset1 +
            ", offset2=" + offset2 +
            ", refTime=" + refTime +
            ", forecastTime=" + forecastTime +
            ", decimalScale=" + decimalScale +
            ", bmsExists=" + bmsExists +
            ", center=" + center +
            ", subCenter=" + subCenter +
            ", table=" + table +
            ", validTime=" + validTime +
            '}';
  }

}
