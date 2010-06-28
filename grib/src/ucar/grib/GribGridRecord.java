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
 *
 * By:   Robb Kambic
 * Date: Jan 31, 2009
 * Time: 4:18:23 PM
 *
 */

package ucar.grib;

import ucar.grib.grib2.Grib2Tables;
import ucar.grid.GridRecord;

import java.util.Date;
import java.util.Calendar;
import java.text.ParseException;
import java.text.SimpleDateFormat;

/**
 * Represents index information for one record in the Grib file.
 */
public final class GribGridRecord implements GridRecord {

  /**
   * Represents the PDS section of a Grib Message.
   * Probably only set for Ensemble data
   */
  //public GribPDSVariablesIF pdsVars = null;

  /** discipline (from indicator section) */
  public int discipline;


  //// from  identification section

  /**
   * refTime as Date
   */
  public Date refTime;

  /**
   * center  subCenter  table of record.
   */
  public int center = -1, subCenter = -1, table = -1;


  //// these are all from the PDS

  /**
   * product definition template, param category, param number
   */
  public int productTemplate, category, paramNumber;

  /**
   * typeGenProcess of record.
   */
  public int typeGenProcess;

  /**
   * levelType1, levelType2  of record.
   */
  public int levelType1, levelType2;

  /**
   * levelValue1, levelValue2  of record.
   */
  public double levelValue1, levelValue2;

  /**
   * forecastTime as int.
   * if forecast time is an interval, it's the end of the interval
   */
  public int forecastTime;

  /**
   * if forecast time is an interval, this is the start of the interval
   */
  public int startOfInterval = GribNumbers.UNDEFINED;

  /**
   * time unit of forecast time
   */
  public int timeUnit;

  /**
   * forecastTime as Date.
   */
  private Date validTime = null;

  /**
   * decimalScale for Grib1 data.
   */
  public int decimalScale = GribNumbers.UNDEFINED;

  /**
   * is this record an Ensemble
   */
  public boolean isEnsemble = false;

  /**
   * Ensemble number.
   */
  public int ensembleNumber  = GribNumbers.UNDEFINED;

  /**
   * numberForecasts of Ensemble.
   */
  public int numberForecasts = GribNumbers.UNDEFINED;

  /**
   * Type of ensemble or Probablity forecast
   */
  public int type = GribNumbers.UNDEFINED;

  /**
   * lowerLimit, upperLimit of Probability type
   */
  public float lowerLimit = GribNumbers.UNDEFINED, upperLimit = GribNumbers.UNDEFINED;

  public int intervalStatType;

  ///////////////////////

  /**
   * bms (Bit mapped section) Exists of record.
   */
  public boolean bmsExists = true;

  /**
   * gdsKey  of record.
   */
  public int gdsKey;

  /**
   * offset1 of record.
   */
  public long offset1;

  /**
   * offset2 of record.
   */
  public long offset2;

  /**
   * default constructor, used by GribReadIndex (binary indices)
   */
  public GribGridRecord() {
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
  GribGridRecord(Calendar calendar, SimpleDateFormat dateFormat,
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

      productTemplate = Integer.parseInt(productTypeS);
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

  /**
   * Get the first level of this GridRecord
   *
   * @return the first level value
   */
  public double getLevel1() {
    return levelValue1;
  }

  /**
   * Get the second level of this GridRecord
   *
   * @return the second level value
   */
  public double getLevel2() {
    return levelValue2;
  }

  /**
   * Get the type for the first level of this GridRecord
   *
   * @return level type
   */
  public int getLevelType1() {
    return levelType1;
  }

  /**
   * Get the type for the second level of this GridRecord
   *
   * @return level type
   */
  public int getLevelType2() {
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
   * Get the valid time for this record.
   *
   * @return valid time
   */
  public Date getValidTime() {
    return validTime;
  }

  /**
   * Set the valid time for this record.
   *
   * @param t valid time
   */
  public void setValidTime(Date t) {
    validTime = t;
  }

  /**
   * Get valid time offset (minutes) of this GridRecord
   *
   * @return time offset
   */
  public int getValidTimeOffset() {
    return forecastTime;
  }

  /**
   * Get the parameter name
   *
   * @return parameter name
   */
  public String getParameterName() {
    //return param;
    return null; // This was moved to GribGridTableLookup
  }

  /**
   * Get the grid def record id
   *
   * @return parameter name
   */
  public int getGridDefRecordIdInt() {
    return gdsKey;
  }

  public String getGridDefRecordId() {
    return Integer.toString( gdsKey );
  }

  /**
   * Get the grid number
   *
   * @return grid number
   */
  public int getGridNumber() {
    //return gridNumber;
    return 0; // not used
  }

  /**
   * Get the decimal scale
   *
   * @return decimal scale
   */
  public int getDecimalScale() {
    return decimalScale;
  }

  /**
   * is this an ensemble type record
   * @return isEnsemble
   */
  public boolean isEnsemble() {
    return isEnsemble;
  }

  /**
   * if ensemble, ensemble type
   * @return type as int
   */
  public int getEnsembleType() {
    return type;
  }

  /**
   * if ensemble, ensemble member number
   * @return ensembleNumber
   */
  public int getEnsembleNumber() {
    return ensembleNumber;
  }

  /**
   * total number of ensemble forecasts
   * @return numberForecasts
   */
  public int getNumberForecasts() {
    return numberForecasts;
  }

  @Override
  public String toString() {
    return "GribGridRecord{" +
            "productType=" + productTemplate +
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

  /**
   * Makes an interval name for template between 8 and 15 inclusive.
   *
   * @return interval name if there is one or an empty string
   */
  public String makeIntervalName( ) {

    if( productTemplate > 7 && productTemplate < 16 ) {
      int span = forecastTime - startOfInterval;
      String intervalName = Integer.toString( span ) + Grib2Tables.getTimeUnitFromTable4_4( timeUnit );
      String intervalTypeName = Grib2Tables.codeTable4_10short(intervalStatType);
      if (intervalTypeName != null)
        intervalName += "_"+intervalTypeName;
      return intervalName;

    } else
      return "";
  }

  /*
   * Because the templates are different for Grib 1 and 2, the startOfInterval is set
   * to designate an interval type parameter
   */
  public boolean isInterval( ) {
    //return ( productTemplate > 7) && ( productTemplate < 16 );
    return ( startOfInterval != GribNumbers.UNDEFINED );
  }

  public String getIntervalTypeName( ) {
    if( isInterval( ) ) {
      String intervalTypeName = Grib2Tables.codeTable4_10short(intervalStatType);
      if (intervalTypeName != null)
        return intervalTypeName;
    }

    return "";
  }


  /**
   * Makes a Ensemble, Derived, Probability or error Suffix
   *
   * @return suffix as String
   */
  public String makeSuffix( ) {

    /* check for accumulation/probability/percentile variables
    if( productType > 7 && productType < 16 ) {
      int span = forecastTime - startOfInterval;
      interval = Integer.toString( span ) + Grib2Tables.getTimeUnitFromTable4_4( timeUnit );
      String intervalTypeName = Grib2Tables.codeTable4_10short(intervalStatType);
      if (intervalTypeName != null) interval += "_"+intervalTypeName;
    } */

    switch (productTemplate) {
      case 0:
      case 7:
      case 40: {
        if (typeGenProcess == 6 || typeGenProcess == 7 ) {
          return "error";
        }
      }
      break;
      case 1:
      case 11:
      case 41:
      case 43: {
        // ensemble data
        /*
        if (typeGenProcess == 4) {
          if (type == 0) {
            return "Cntrl_high";
          } else if (type == 1) {
            return "Cntrl_low";
          } else if (type == 2) {
            return "Perturb_neg";
          } else if (type == 3) {
            return "Perturb_pos";
          } else {
            return "unknownEnsemble";
          }

        }
        */
        break;
      }

      case 2:
      case 3:
      case 4: {
        // Derived data
        if (typeGenProcess == 4) {
          if (type == 0) {
            return  "unweightedMean";
          } else if (type == 1) {
            return  "weightedMean";
          } else if (type == 2) {
            return  "stdDev";
          } else if (type == 3) {
            return  "stdDevNor";
          } else if (type == 4) {
            return  "spread";
          } else if (type == 5) {
            return  "anomaly";
          } else if (type == 6) {
            return  "unweightedMeanCluster";
          } else {
            return  "unknownEnsemble";
          }
        }
        break;
      }

      case 12:
      case 13:
      case 14: {
        // Derived data
        if (typeGenProcess == 4) {
          if (type == 0) {
            return "unweightedMean";
          } else if (type == 1) {
            return "weightedMean";
          } else if (type == 2) {
            return "stdDev";
          } else if (type == 3) {
            return "stdDevNor";
          } else if (type == 4) {
            return "spread";
          } else if (type == 5) {
            return "anomaly";
          } else if (type == 6) {
            return "unweightedMeanCluster";
          } else {
            return "unknownEnsemble";
          }
        }
        break;
      }

      case 5: {
        // probability data
        if (typeGenProcess == 5) {
          return getProbabilityVariableNameSuffix( lowerLimit, upperLimit, type );
        }
      }
      break;
      case 9: {
        // probability data
        if (typeGenProcess == 5) {
          return getProbabilityVariableNameSuffix( lowerLimit, upperLimit, type );
        }
      }
      break;

      default:
        return "";
    }
    return "";
  }

  static public String getProbabilityVariableNameSuffix(float lowerLimit, float upperLimit, int type) {
    String ll = Float.toString(lowerLimit).replace('.', 'p').replaceFirst("p0$", "");
    String ul = Float.toString(upperLimit).replace('.', 'p').replaceFirst("p0$", "");
    if (type == 0) {
      //return "below_" + Float.toString(lowerLimit).replace('.', 'p');
      return "probability_below_" + ll;
    } else if (type == 1) {
      //return "above_" + Float.toString(upperLimit).replace('.', 'p');
      return "probability_above_" + ul;
    } else if (type == 2) {
      //return "between_" + Float.toString(lowerLimit).replace('.', 'p') + "_" +
      //    Float.toString(upperLimit).replace('.', 'p');
      return "probability_between_" + ll + "_" + ul;
    } else if (type == 3) {
      //return "above_" + Float.toString(lowerLimit).replace('.', 'p');
      return "probability_above_" + ll;
    } else if (type == 4) {
      //return "below_" + Float.toString(upperLimit).replace('.', 'p');
      return "probability_below_" + ul;
    } else {
      return "unknownProbability";
    }

  }

}