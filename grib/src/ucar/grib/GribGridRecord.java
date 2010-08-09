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

package ucar.grib;

import ucar.grib.grib1.Grib1Pds;
import ucar.grib.grib1.Grib1Tables;
import ucar.grib.grib1.GribPDSParamTable;
import ucar.grib.grib2.Grib2Pds;
import ucar.grib.grib2.Grib2Tables;
import ucar.grib.grib2.ParameterTable;
import ucar.grid.GridParameter;
import ucar.grid.GridRecord;
import ucar.grid.GridTableLookup;

import java.util.Date;
import java.util.Formatter;

/**
 * Represents information for one record in the Grib file.
 */
public final class GribGridRecord implements GridRecord {
  static private org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(GribGridRecord.class);

  //// from  indicator section

  int edition; // grib 1 or 2

  int discipline;  // grib 2 only  ?

  //// from  identification section

  /**
   * reference Time as Date
   */
  Date refTime;

  /**
   * center  subCenter  table of record.
   */
  int center = -1, subCenter = -1, table = -1;

  //// these are all from the PDS
  private GribPds pds;

   ///////////////////////

  /**
   * bms (Bit mapped section) exists - used only by Grib1
   */
  boolean bmsExists = true;

  /**
   * gdsKey  of record.
   */
  int gdsKey;

  /**
   * offset1 of record.
   */
  long offset1;

  /**
   * offset2 of record.
   */
  long offset2;

  private String paramName, paramDesc;

  /**
   * default constructor, used by GribReadIndex (binary indices)
   */
  public GribGridRecord() {
  }

  void setPds(GribPds pds) {
    this.pds = pds;
  }

  public GribPds getPds() {
    return pds;
  }

  public int getEdition() {
    return edition;
  }

  public int getDiscipline() {
    return discipline;
  }

  public int getCenter() {
    return center;
  }

  public int getSubCenter() {
    return subCenter;
  }

  public int getTableVersion() {
    return table;
  }

  public long getOffset1() {
    return offset1;
  }

  public long getOffset2() {
    return offset2;
  }

  public int getGdsKey() {
    return gdsKey;
  }

  public boolean isBmsExists() {
    return bmsExists;
  }

  @Override
  public String getGridDefRecordId() {
    return Integer.toString(gdsKey);
  }

  /**
   * Get the first level of this GridRecord
   *
   * @return the first level value
   */
  @Override
  public double getLevel1() {
    return pds.getLevelValue1();
  }

  /**
   * Get the second level of this GridRecord
   *
   * @return the second level value
   */
  @Override
  public double getLevel2() {
    return pds.getLevelValue2();
  }

  /**
   * Get the type for the first level of this GridRecord
   *
   * @return level type
   */
  @Override
  public int getLevelType1() {
    return pds.getLevelType1();
  }

  /**
   * Get the type for the second level of this GridRecord
   *
   * @return level type
   */
  @Override
  public int getLevelType2() {
    return pds.getLevelType2();
  }

  /**
   * Get the first reference time of this GridRecord
   *
   * @return reference time
   */
  @Override
  public Date getReferenceTime() {
    return refTime;
  }

  /*
   * Get valid time offset (minutes) of this GridRecord
   *
   * @return time offset in minutes from
   *
  @Override
  public int getValidTimeOffset() {
    if (validTimeOffset == null)
      calcValidTime();
    return validTimeOffset;
  } */

  /**
   * Get the valid time for this record.
   *
   * @return valid time
   */
  @Override
  public Date getValidTime() {
    return pds.getForecastDate();
  }

  public int getParameterNumber() {
    return pds.getParameterNumber();
  }

  /**
   * Get the parameter name
   *
   * @return parameter name
   */
  @Override
  public String getParameterName() {
    if (paramName == null) {
      GridParameter p = getParameter();
      paramName = p.getName();
      paramDesc = p.getDescription();
    }
    return paramName;
  }

  /**
   * Get the parameter description
   *
   * @return parameter description
   */
  @Override
  public String getParameterDescription() {
     if (paramDesc == null) {
      GridParameter p = getParameter();
      paramName = p.getName();
      paramDesc = p.getDescription();
    }
    return paramDesc;
  }

  private GridParameter getParameter() {
    GridParameter p = null;
      if (edition == 2) {
        Grib2Pds pds2 = (Grib2Pds) pds;
        p = ParameterTable.getParameter(discipline, pds2.getParameterCategory(), pds.getParameterNumber());

      } else {
        GribPDSParamTable pt = null;
        try {
          pt = GribPDSParamTable.getParameterTable(center, subCenter, table);
        } catch (NotSupportedException e) {
          logger.error("Failed to get Parameter name for " + this);
        }
        p = pt.getParameter( pds.getParameterNumber());
      }

    return p;
  }

  /**
   * Get the decimal scale
   *
   * @return decimal scale
   */
  @Override
  public int getDecimalScale() {
    if (edition == 1) {
      Grib1Pds pds1 = (Grib1Pds) pds;
      return pds1.getDecimalScale();
    } else {
      return GribNumbers.UNDEFINED;
    }
  }

  @Override
  public String getTimeUnitName() {
    if (edition == 2)
      return Grib2Tables.getUdunitTimeUnitFromTable4_4(getTimeUnit());
    else
      return Grib1Tables.getTimeUnit(getTimeUnit());
  }

  public int getTimeUnit() {
    return pds.getTimeUnit();
  }

  public int getTypeGenProcess() {
    return pds.getTypeGenProcess();
  }

  /////////////////////////////////////////////////////////////////////////////
  /**
   * A hash code to group records into a CDM variable
   *
   * @return group hash code
   */
  @Override
  public int cdmVariableHash() {
    if (hashcode == 0) {
      int result = 17;

      if (edition == 1) {
        result += result * 37 + getLevelType1();
        result += result * 37 + getParameterName().hashCode();
        if (isInterval()) result += result * 37 + getIntervalTypeName().hashCode();

      } else {
        Grib2Pds pds2 = (Grib2Pds) pds;
        int productTemplate = pds2.getProductDefinitionTemplate();

        result += result * 37 + getLevelType1();
        result += result * 37 + getParameterName().hashCode();
        result += result * 37 + productTemplate;
        if (isInterval()) result += result * 37 + getIntervalTypeName().hashCode();
        result += result * 37 +  getLevelType2(); // ??

        if (pds2.isEnsembleDerived()) {
          Grib2Pds.PdsEnsembleDerived pdsDerived = (Grib2Pds.PdsEnsembleDerived) pds2;
          result += result * 37 + pdsDerived.getDerivedForecastType(); // derived type (table 4.7)

        } else if (pds2.isEnsemble()) {
          result += result * 37 + 1; 
        }

        if (pds2.isProbability()) {
          Grib2Pds.PdsProbability pdsProb = (Grib2Pds.PdsProbability) pds2;
          String name = getProbabilityVariableNameSuffix(pdsProb.getProbabilityLowerLimit(), pdsProb.getProbabilityUpperLimit(), pdsProb.getProbabilityType());
          result +=  result * 37 + name.hashCode();
        }
      }
      
      hashcode = result;
    }
    return hashcode;
  }

  private int hashcode = 0;

  /**
   * A unique name for the CDM variable, must be consistent with cdmVariableHash
   *
   * @return unique CDM variable name
   */
  @Override
  public String cdmVariableName(GridTableLookup lookup, boolean useLevel, boolean useStat) {
    Formatter f = new Formatter();
    String desc = getParameterDescription();
    f.format("%s", desc);

    if (useLevel) {
      String levelName = lookup.getLevelName(this);
      if (levelName.length() != 0) {
        //if (lookup.isLayer(this))
        //  f.format("_%s_layer", levelName);
       // else
          f.format("_%s", levelName);
      }
    }

    if (useStat) {
      f.format("%s", makeSuffix());
    }

    return f.toString();
  }

  public String makeSuffix() {
    Formatter f = new Formatter();
    boolean disambig = false;

    if (isInterval()) {
      String intervalTypeName = getIntervalTypeName();
      if (intervalTypeName != null && intervalTypeName.length() != 0) {
        f.format("_%s", intervalTypeName);
        disambig = true;
      }
    }

    if (edition == 2) {
       Grib2Pds pds2 = (Grib2Pds) pds;

       if (pds2.isEnsembleDerived()) {
         Grib2Pds.PdsEnsembleDerived pdsDerived = (Grib2Pds.PdsEnsembleDerived) pds2;
         int type = pdsDerived.getDerivedForecastType(); // derived type (table 4.7)
         f.format("_%s", Grib2Tables.codeTable4_7short(type));
         disambig = true;

       } else if (pds2.isProbability()) {
         Grib2Pds.PdsProbability pdsProb = (Grib2Pds.PdsProbability) pds2;
         String name = getProbabilityVariableNameSuffix(pdsProb.getProbabilityLowerLimit(), pdsProb.getProbabilityUpperLimit(), pdsProb.getProbabilityType());
         f.format("_%s", name);
         disambig = true;
       }

        //if (!disambig) {
        //  f.format("_template%d", pds2.getProductDefinitionTemplate());
        //}
     }

    return f.toString();
  }

  static public String getProbabilityVariableNameSuffix(double lowerLimit, double upperLimit, int type) {
    String ll = Double.toString(lowerLimit).replace('.', 'p').replaceFirst("p0$", "");
    String ul = Double.toString(upperLimit).replace('.', 'p').replaceFirst("p0$", "");
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

  //////////////////////////////////////////////

  /*
   * Type of derived, ensemble or probability, see Pdsv.getType()
   *
   * @return type of derived, ensemble or probability variable
   *
  public int getType() {
    return type;
  }

  /*
   * if ensemble, ensemble member number
   *
   * @return ensembleNumber
   *
  public int getEnsembleNumber() {
    return ensembleNumber;
  }

  /**
   * total number of ensemble forecasts
   *
   * @return numberForecasts
   *
  public int getNumberForecasts() {
    return numberForecasts;
  } */

  /*
   * Makes an interval name for template between 8 and 15 inclusive.
   *
   * @return interval name if there is one or an empty string
   *
  public String makeIntervalName() {

    if (productTemplate > 7 && productTemplate < 16) {
      int span = forecastTime - startOfInterval;
      String intervalName = Integer.toString(span) + Grib2Tables.getUdunitTimeUnitFromTable4_4(timeUnit);
      String intervalTypeName = Grib2Tables.codeTable4_10short(intervalStatType);
      if (intervalTypeName != null)
        intervalName += "_" + intervalTypeName;
      return intervalName;

    } else
      return "";
  } */

  public boolean isInterval() {
    return pds.isInterval();
  }

  @Override
  public int getTimeInterval() {
    if (!isInterval()) return -1;
    int[] intv = pds.getForecastTimeInterval();
    return intv[1] - intv[0];
  }

  public String getIntervalTypeName() {
    if (isInterval())
      return Grib2Tables.codeTable4_10short( getIntervalStatType());

    return null;
  }

  /**
   * Get interval type, GRIB-2 code table 4.10
   * @return interval statistic type
   */
  public int getIntervalStatType() {
    return pds.getIntervalStatType();
  }

  //// debugging


  @Override
  public String toString() {
    return "GribGridRecord{" +
            "edition=" + edition +
            ", discipline=" + discipline +
            ", refTime=" + refTime +
            ", center=" + center +
            ", subCenter=" + subCenter +
            ", table=" + table +
            ", bmsExists=" + bmsExists +
            ", gdsKey=" + gdsKey +
            ", offset1=" + offset1 +
            ", offset2=" + offset2 +
            ", paramName='" + paramName + '\'' +
            ", paramDesc='" + paramDesc + '\'' +
            ", pds=" + pds +
            '}';
  }

  public String toString2() {
    return "GribGridRecord{" +
        ", param=" + getParameterName() +
        ", levelType1=" + pds.getLevelType1() +
        ", levelValue1=" + pds.getLevelType2() +
        ", forecastDate=" + pds.getForecastDate() +
        '}';
  }

  private Object belongsTo;

  public Object getBelongs() {
    return belongsTo;
  }

  public void setBelongs(Object gv) {
    this.belongsTo = gv;
  }

  /*
  byte[] raw;
  public byte[] getPdsBytes() {
    return raw;
  }
  public void setPdsBytes(byte[] raw) {
    this.raw = raw;
  } */


  ///////////////////////
  // deprecated

  /**
   * constructor given all parameters as Strings. Used only by deprecated GribReadTextIndex
   * @deprecated text indices should be rewritten to binary
   *
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
  }    */

  /*

     /*
   * Makes a Ensemble, Derived, Probability or error Suffix
   *
   * @return suffix as String
   *
  public String makeSuffix() {

    /* check for accumulation/probability/percentile variables
    if( productType > 7 && productType < 16 ) {
      int span = forecastTime - startOfInterval;
      interval = Integer.toString( span ) + Grib2Tables.getTimeUnitFromTable4_4( timeUnit );
      String intervalTypeName = Grib2Tables.codeTable4_10short(intervalStatType);
      if (intervalTypeName != null) interval += "_"+intervalTypeName;
    } *

    switch (productTemplate) {
      case 0:
      case 7:
      case 40: {
        if (typeGenProcess == 6 || typeGenProcess == 7) {
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
        *
        break;
      }

      case 2:
      case 3:
      case 4: {
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
          return getProbabilityVariableNameSuffix(lowerLimit, upperLimit, type);
        }
      }
      break;
      case 9: {
        // probability data
        if (typeGenProcess == 5) {
          return getProbabilityVariableNameSuffix(lowerLimit, upperLimit, type);
        }
      }
      break;

      default:
        return "";
    }
    return "";
  }
   */


}