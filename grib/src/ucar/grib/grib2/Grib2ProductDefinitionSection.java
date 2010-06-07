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

// $Id: Grib2ProductDefinitionSection.java,v 1.25 2006/08/18 20:22:10 rkambic Exp $

/**
 * Grib2ProductDefinitionSection.java  1.1  08/29/2003.
 * @author Robb Kambic
 */
package ucar.grib.grib2;


import ucar.grib.GribNumbers;
import ucar.grib.NotSupportedException;

import ucar.unidata.io.RandomAccessFile;

import java.io.IOException;
import java.io.PrintStream;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;


/**
 * A class representing the product definition section (PDS) of a GRIB product.
 * This is section 4 of a Grib record that contains information about the parameter
 */

public final class Grib2ProductDefinitionSection {

  /**
   * Length in bytes of this PDS.
   */
  private final int length;

  /**
   * Number of this section, should be 4.
   */
  private final int section;

  /**
   * Number of this coordinates.
   */
  private final int coordinates;

  /**
   * productDefinition.
   */
  private final int productDefinition;

  /**
   * parameterCategory.
   */
  private int parameterCategory;

  /**
   * parameterNumber.
   */
  private int parameterNumber;

  /**
   * typeGenProcess.
   */
  private int typeGenProcess;

  /**
   * backGenProcess.
   */
  private int backGenProcess;

  /**
   * analysisGenProcess.
   */
  private int analysisGenProcess;

  /**
   * hoursAfter.
   */
  private int hoursAfter;

  /**
   * minutesAfter.
   */
  private int minutesAfter;

  /**
   * timeRangeUnit.
   */
  protected int timeRangeUnit;

  /**
   * forecastTime.
   */
  private int forecastTime;

  /**
   * typeFirstFixedSurface.
   */
  private int typeFirstFixedSurface;

  /**
   * value of FirstFixedSurface.
   */
  private float FirstFixedSurfaceValue;

  /**
   * typeSecondFixedSurface.
   */
  private int typeSecondFixedSurface;

  /**
   * SecondFixedSurface Value.
   */
  private float SecondFixedSurfaceValue;

  /**
   * Type of Ensemble.
   */
  private int typeEnsemble;

  /**
   * Perturbation number.
   */
  private int perturbNumber;

  /**
   * number of Forecasts.
   */
  private int numberForecasts;

  /**
   * number of bands.
   */
  private int nb;

  /**
   * Model Run/Analysis/Reference time.
   */
  private Date endTI;
  private int timeRanges;
  private int[] timeIncrement;
  private float lowerLimit, upperLimit;

  /**
   * PDS as Variables from a byte[]
   */
  private final Grib2PDSVariables pdsVars;
  // *** constructors *******************************************************

  /**
   * Constructs a Grib2ProductDefinitionSection  object from a raf.
   *
   * @param raf RandomAccessFile with PDS content
   * @throws IOException if raf contains no valid GRIB file
   */
  public Grib2ProductDefinitionSection(RandomAccessFile raf) throws IOException {
    long sectionEnd = raf.getFilePointer();

    // octets 1-4 (Length of PDS)
    length = GribNumbers.int4(raf);
    //System.out.println( "PDS length=" + length );

    // read in whole PDS as byte[]
    byte[] pdsData = new byte[ length ];
    // reset to beginning of section and read data
    raf.skipBytes( -4 );
    raf.read( pdsData);
    pdsVars = new Grib2PDSVariables( pdsData );

    // reset for variable section read and set sectionEnd
    raf.seek( sectionEnd +4 );
    sectionEnd += length;

    // octet 5
    section = raf.read();
    //System.out.println( "PDS is 4, section=" + section );

    // octet 6-7
    coordinates = GribNumbers.int2(raf);
    //System.out.println( "PDS coordinates=" + coordinates );

    // octet 8-9
    productDefinition = GribNumbers.int2(raf);
    //System.out.println( "PDS productDefinition=" + productDefinition );

    switch (productDefinition) {

      // Analysis or forecast at a horizontal level or in a horizontal
      // layer at a point in time
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
      case 11: {

        // octet 10
        parameterCategory = raf.read();
        //System.out.println( "PDS parameterCategory=" +
        //parameterCategory );

        // octet 11
        parameterNumber = raf.read();
        //System.out.println( "PDS parameterNumber=" + parameterNumber );

        // octet 12
        typeGenProcess = raf.read();
        //System.out.println( "PDS typeGenProcess=" + typeGenProcess );

        // octet 13
        backGenProcess = raf.read();
        //System.out.println( "PDS backGenProcess=" + backGenProcess );

        // octet 14
        analysisGenProcess = raf.read();
        //System.out.println( "PDS analysisGenProcess=" +
        //analysisGenProcess );

        // octet 15-16
        hoursAfter = GribNumbers.int2(raf);
        //System.out.println( "PDS hoursAfter=" + hoursAfter );

        // octet 17
        minutesAfter = raf.read();
        //System.out.println( "PDS minutesAfter=" + minutesAfter );

        // octet 18
        timeRangeUnit = raf.read();
        //System.out.println( "PDS timeRangeUnit=" + timeRangeUnit );

        // octet 19-22
        forecastTime = GribNumbers.int4(raf) * calculateIncrement(timeRangeUnit, 1);
        //System.out.println( "PDS forecastTime=" + forecastTime );

        // octet 23
        typeFirstFixedSurface = raf.read();
        //System.out.println( "PDS typeFirstFixedSurface=" +
        //typeFirstFixedSurface );

        // octet 24
        int scaleFirstFixedSurface = raf.read();
        //System.out.println( "PDS scaleFirstFixedSurface=" +
        //scaleFirstFixedSurface );

        // octet 25-28
        int valueFirstFixedSurface = GribNumbers.int4(raf);
        //System.out.println( "PDS valueFirstFixedSurface=" +
        //valueFirstFixedSurface );

        FirstFixedSurfaceValue = (float) (((scaleFirstFixedSurface
            == 0) || (valueFirstFixedSurface == 0))
            ? valueFirstFixedSurface
            : valueFirstFixedSurface
            * Math.pow(10, -scaleFirstFixedSurface));
        //System.out.println( "PDS FirstFixedSurfaceValue ="+ FirstFixedSurfaceValue );

        // octet 29
        typeSecondFixedSurface = raf.read();
        //System.out.println( "PDS typeSecondFixedSurface=" +
        //typeSecondFixedSurface );

        // octet 30
        int scaleSecondFixedSurface = raf.read();
        //System.out.println( "PDS scaleSecondFixedSurface=" +
        //scaleSecondFixedSurface );

        // octet 31-34
        int valueSecondFixedSurface = GribNumbers.int4(raf);
        //System.out.println( "PDS valueSecondFixedSurface=" +
        //valueSecondFixedSurface );

        SecondFixedSurfaceValue = (float) (((scaleSecondFixedSurface
            == 0) || (valueSecondFixedSurface == 0))
            ? valueSecondFixedSurface
            : valueSecondFixedSurface
            * Math.pow(10, -scaleSecondFixedSurface));

        try {  // catches NotSupportedExceptions

          // Individual ensemble forecast, control and perturbed, at a
          // horizontal level or in a horizontal layer at a point in time
          if ((productDefinition == 1) || (productDefinition == 11)) {
            // octet 35
            typeEnsemble = raf.read();
            // octet 36
            perturbNumber = raf.read();
            // octet 37
            numberForecasts = raf.read();
            /*
            System.out.println( "Cat ="+ parameterCategory
             +" parameter ="+ parameterNumber +" Time ="+ forecastTime
             +" typeEns ="+ typeEnsemble + " perturbation ="+ perturbNumber
             +" numberOfEns ="+ numberForecasts );
            */
            if (productDefinition == 11) {  // skip 38-74-nn detail info
              raf.seek(sectionEnd);
            }
//            if (typeGenProcess == 4) { // ensemble var
//              // TODO: should perturbNumber be numberForecasts
//              typeGenProcess = 40000 + (1000 * typeEnsemble) + perturbNumber;
//            }
            //System.out.println( "typeGenProcess ="+ typeGenProcess );
            //Derived forecast based on all ensemble members at a horizontal
            // level or in a horizontal layer at a point in time
          } else if (productDefinition == 2) {
            // octet 35
            typeEnsemble = raf.read();
            // octet 36
            numberForecasts = raf.read();
//            if (typeGenProcess == 4) { // ensemble var
//              typeGenProcess = 40000 + (1000 * typeEnsemble) + numberForecasts;
//            }
//            System.out.println( "typeGenProcess ="+ typeGenProcess );
            //
            // Derived forecasts based on a cluster of ensemble members over
            // a rectangular area at a horizontal level or in a horizontal layer
            // at a point in time
          } else if (productDefinition == 3) {
            //System.out.println("PDS productDefinition == 3 not done");
            throw new NotSupportedException("PDS productDefinition = 3 not implemented");

            // Derived forecasts based on a cluster of ensemble members
            // over a circular area at a horizontal level or in a horizontal
            // layer at a point in time
          } else if (productDefinition == 4) {
            //System.out.println("PDS productDefinition == 4 not done");
            throw new NotSupportedException("PDS productDefinition = 4 not implemented");

            // Probability forecasts at a horizontal level or in a horizontal
            //  layer at a point in time
          } else if (productDefinition == 5) {
            // 35
            int probabilityNumber = raf.read();
            // 36
            numberForecasts = raf.read();
            // 37
            typeEnsemble = raf.read();
            // 38
            int scaleFactorLL = raf.read();
            // 39-42
            int scaleValueLL = GribNumbers.int4(raf);
            lowerLimit = (float) (((scaleFactorLL == 0) || (scaleValueLL == 0))
                ? scaleValueLL
                : scaleValueLL * Math.pow(10, -scaleFactorLL));
            // 43
            int scaleFactorUL = raf.read();
            // 44-47
            int scaleValueUL = GribNumbers.int4(raf);
            upperLimit = (float) (((scaleFactorUL == 0) || (scaleValueUL == 0))
                ? scaleValueUL
                : scaleValueUL * Math.pow(10, -scaleFactorUL));
//            if (typeGenProcess == 5) { // Probability var
//              typeGenProcess = 50000 + (1000 * typeEnsemble) + totalProbabilities;
//            }
            //System.out.print("PDS productDefinition == 5 PN="+probabilityNumber +" TP="+totalProbabilities +" PT="+probabilityType);
            //System.out.println( " LL="+lowerLimit +" UL="+upperLimit);
            //System.out.println( " typeGenProcess ="+ typeGenProcess );

            // Percentile forecasts at a horizontal level or in a horizontal layer
            // at a point in time
          } else if (productDefinition == 6) {
            //System.out.println("PDS productDefinition == 6 not done");
            throw new NotSupportedException("PDS productDefinition = 6 not implemented");

            // Analysis or forecast error at a horizontal level or in a horizontal
            // layer at a point in time
          } else if (productDefinition == 7) {
            //System.out.println("PDS productDefinition == 7 not done");
            throw new NotSupportedException("PDS productDefinition = 7 not implemented");

            // Average, accumulation, and/or extreme values at a horizontal
            // level or in a horizontal layer in a continuous or non-continuous
            // time interval
          } else if (productDefinition == 8) {
            //System.out.println( "PDS productDefinition == 8 " );
            //  35-41 bytes
            int year = GribNumbers.int2(raf);
            int month = (raf.read()) - 1;
            int day = raf.read();
            int hour = raf.read();
            int minute = raf.read();
            int second = raf.read();
            //System.out.println( "PDS date:" + year +":" + month +
            //":" + day + ":" + hour +":" + minute +":" + second );
            GregorianCalendar c = new GregorianCalendar(TimeZone.getTimeZone("GMT"));
            c.clear();
            c.set(year, month, day, hour, minute, second);
            endTI = c.getTime();
            // 42
            timeRanges = raf.read();
            //System.out.println( "PDS timeRanges=" + timeRanges ) ;
            // 43 - 46
            int missingDataValues = GribNumbers.int4(raf);
            //System.out.println( "PDS missingDataValues=" + missingDataValues ) ;

            timeIncrement = new int[timeRanges * 6];
            for (int t = 0; t < timeRanges; t += 6) {
              // 47 statProcess
              timeIncrement[t] = raf.read();
              // 48  timeType
              timeIncrement[t + 1] = raf.read();
              // 49   time Unit
              timeIncrement[t + 2] = raf.read();
              // 50 - 53 lenTimeRange
              timeIncrement[t + 3] = GribNumbers.int4(raf);
              // 54 indicatorTU
              timeIncrement[t + 4] = raf.read();
              // 55 - 58 timeIncrement
              timeIncrement[t + 5] = GribNumbers.int4(raf);
            }
            // modify forecast time to reflect the end of the
            // interval according to timeIncrement information.
            // 1 accumulation
            // 2 F.T. inc
            // 1 Hour
            // 3 number of hours to inc F.T.
            // 255 missing
            // 0 continuous processing
            //if( timeRanges == 1 && timeIncrement[ 2 ] == 1) {
            if (timeRanges == 1) {
              forecastTime += calculateIncrement(timeIncrement[2], timeIncrement[3]);
            } else { // throw flag
              forecastTime = GribNumbers.UNDEFINED;
            }
            // Probability forecasts at a horizontal level or in a horizontal
            // layer in a continuous or non-continuous time interval
          } else if (productDefinition == 9) {
            //  35-71 bytes
            // 35
            int probabilityNumber = raf.read();
            // 36
            numberForecasts = raf.read();
            // 37
            typeEnsemble = raf.read();
//            if (typeGenProcess == 5) { // Probability var
//              typeGenProcess = 50000 + (1000 * typeEnsemble) + numberForecasts;
//            }
            // 38
            int scaleFactorLL = raf.read();
            // 39-42
            int scaleValueLL = GribNumbers.int4(raf);
            lowerLimit = (float) (((scaleFactorLL == 0) || (scaleValueLL == 0))
                ? scaleValueLL
                : scaleValueLL * Math.pow(10, -scaleFactorLL));

            // 43
            int scaleFactorUL = raf.read();
            // 44-47
            int scaleValueUL = GribNumbers.int4(raf);
            upperLimit = (float) (((scaleFactorUL == 0) || (scaleValueUL == 0))
                ? scaleValueUL
                : scaleValueUL * Math.pow(10, -scaleFactorUL));

            // 48-49
            int year = GribNumbers.int2(raf);
            // 50
            int month = (raf.read()) - 1;
            // 51
            int day = raf.read();
            // 52
            int hour = raf.read();
            // 53
            int minute = raf.read();
            // 54
            int second = raf.read();
            //System.out.println( "PDS date:" + year +":" + month +
            //":" + day + ":" + hour +":" + minute +":" + second );
            GregorianCalendar c = new GregorianCalendar(TimeZone.getTimeZone("GMT"));
            c.clear();
            c.set(year, month, day, hour, minute, second);
            endTI = c.getTime();
            // 55
            timeRanges = raf.read();
            //System.out.println( "PDS timeRanges=" + timeRanges ) ;
            // 56-59
            int missingDataValues = GribNumbers.int4(raf);
            //System.out.println( "PDS missingDataValues=" + missingDataValues ) ;

            timeIncrement = new int[timeRanges * 6];
            for (int t = 0; t < timeRanges; t += 6) {
              // 60 statProcess
              timeIncrement[t] = raf.read();
              // 61 time type
              timeIncrement[t + 1] = raf.read();
              // 62  time Unit
              timeIncrement[t + 2] = raf.read();
              // 63 - 66  lenTimeRange
              timeIncrement[t + 3] = GribNumbers.int4(raf);
              // 67  indicatorTU
              timeIncrement[t + 4] = raf.read();
              // 68-71 time Inc
              timeIncrement[t + 5] = GribNumbers.int4(raf);
            }
            // modify forecast time to reflect the end of the
            // interval according to timeIncrement information.
            // 1 accumulation
            // 2 F.T. inc
            // 1 Hour
            // 3 number of hours to inc F.T.
            // 255 missing
            // 0 continuous processing
            if (timeRanges == 1) {
              forecastTime += calculateIncrement(timeIncrement[2], timeIncrement[3]);
            } else { // throw flag
              forecastTime = GribNumbers.UNDEFINED;
            }

          }
          break;
        } catch (NotSupportedException nse) {
          nse.printStackTrace();
        }
      }  // cases 0-14

      // Radar product
      case 20: {

        parameterCategory = raf.read();
        //System.out.println( "PDS parameterCategory=" +
        //parameterCategory );

        parameterNumber = raf.read();
        //System.out.println( "PDS parameterNumber=" + parameterNumber );

        typeGenProcess = raf.read();
        //System.out.println( "PDS typeGenProcess=" + typeGenProcess );

        break;
      }  // case 20

      // Satellite Product
      case 30: {

        parameterCategory = raf.read();
        //System.out.println( "PDS parameterCategory=" + parameterCategory );

        parameterNumber = raf.read();
        //System.out.println( "PDS parameterNumber=" + parameterNumber );

        typeGenProcess = raf.read();
        //System.out.println( "PDS typeGenProcess=" + typeGenProcess );

        backGenProcess = raf.read();
        //System.out.println( "PDS backGenProcess=" + backGenProcess );

        nb = raf.read();
        //System.out.println( "PDS nb =" + nb );
        // nb sometime 0 based, other times 1 base
        for (int j = 0; j < nb; j++) {
          raf.skipBytes(10);
        }
        break;
      }  // case 30

      // CCITTIA5 character string
      case 254: {

        parameterCategory = raf.read();
        //System.out.println( "PDS parameterCategory=" +
        //parameterCategory );

        parameterNumber = raf.read();
        //System.out.println( "PDS parameterNumber=" + parameterNumber );

        //numberOfChars = GribNumbers.int4( raf );
        //System.out.println( "PDS numberOfChars=" +
        //numberOfChars );
        break;
      }  // case 254

      default:
        break;

    }    // end switch

    raf.seek(sectionEnd);

  }

  /**
   * calculates the increment between time intervals
   * @param tui time unit indicator,
   * @param length of interval
   * returns increment
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

  /**
   * Number of this coordinates.
   * @deprecated
   * @return Coordinates number
   */
  public final int getCoordinates() {
    return coordinates;
  }

  /**
   * productDefinition.
   * @deprecated
   * @return ProductDefinition
   */
  public final int getProductDefinition() {
    return productDefinition;
  }

  /**
   * product Definition  Name.
   * @deprecated
   * @return ProductDefinitionName
   */
  public final String getProductDefinitionName() {
    return Grib2Tables.codeTable4_0(productDefinition);
  }

  /**
   * parameter Category .
   * @deprecated
   * @return parameterCategory as int
   */
  public final int getParameterCategory() {
    return parameterCategory;
  }

  /**
   * parameter Number.
   * @deprecated
   * @return ParameterNumber
   */
  public final int getParameterNumber() {
    return parameterNumber;
  }

  /**
   * type of Generating Process.
   * @deprecated
   * @return GenProcess
   */
  public final String getTypeGenProcess() {
    if (typeGenProcess == 4) {  //ensemble
      String type;
      if (typeEnsemble == 0) {
        type = "C_high";
      } else if (typeEnsemble == 1) {
        type = "C_low";
      } else if (typeEnsemble == 2) {
        type = "P_neg";
      } else if (typeEnsemble == 3) {
        type = "P_pos";
      } else {
        type = "unknown";
      }

      return "4-" + type + "-" + Integer.toString(perturbNumber);
    }
    return Integer.toString(typeGenProcess);
  }

  /**
   * type of Generating Process.
   * @deprecated
   * @return GenProcess
   */
  public final int getTypeGenProcessNumeric() {
    return typeGenProcess;
  }

  /**
   * backGenProcess.
   * @deprecated
   * @return BackGenProcess
   */
  public final int getBackGenProcess() {
    return backGenProcess;
  }

  /**
   * analysisGenProcess.
   * @deprecated
   * @return analysisGenProcess
   */
  public final int getAnalysisGenProcess() {
    return analysisGenProcess;
  }

  /**
   * hoursAfter.
   * @deprecated
   * @return HoursAfter
   */
  public final int getHoursAfter() {
    return hoursAfter;
  }

  /**
   * minutesAfter.
   * @deprecated
   * @return MinutesAfter
   */
  public final int getMinutesAfter() {
    return minutesAfter;
  }

  /**
   * returns timeRangeUnit .
   * @deprecated
   * @return TimeRangeUnitName
   */
  public final int getTimeRangeUnit() {
    return timeRangeUnit;
  }

  /*
   * returns    Time Range Unit Name.
   * @deprecated
   * @return TimeRangeUnitName
   *
  public final String getTimeRangeUnitName() {
    return Grib2Tables.codeTable4_4(timeRangeUnit);
  }  */

  /**
   * forecastTime.
   * @deprecated
   * @return ForecastTime
   */
  public final int getForecastTime() {
    return forecastTime;
  }

  /**
   * typeFirstFixedSurface.
   * @deprecated
   * @return FirstFixedSurface as int
   */
  public final int getTypeFirstFixedSurface() {
    return typeFirstFixedSurface;
  }

  /**
   * typeFirstFixedSurface Name.
   * @deprecated
   * @return FirstFixedSurfaceName
   */
  public final String getTypeFirstFixedSurfaceName() {
    return Grib2Tables.codeTable4_5(typeFirstFixedSurface);
  }

  /**
   * valueFirstFixedSurface.
   * @deprecated
   * @return FirstFixedSurfaceValue
   */
  public final float getValueFirstFixedSurface() {
    return FirstFixedSurfaceValue;
  }

  /**
   * typeSecondFixedSurface.
   * @deprecated
   * @return SecondFixedSurface as int
   */
  public final int getTypeSecondFixedSurface() {
    return typeSecondFixedSurface;
  }

  /**
   * typeSecondFixedSurface Name.
   * @deprecated
   * @return SecondFixedSurfaceName
   */
  public final String getTypeSecondFixedSurfaceName() {
    return Grib2Tables.codeTable4_5(typeSecondFixedSurface);
  }

  /**
   * valueSecondFixedSurface.
   * @deprecated
   * @return SecondFixedSurfaceValue
   */
  public final float getValueSecondFixedSurface() {
    return SecondFixedSurfaceValue;
  }

  /**
   * @deprecated
   * @return Date
   */
  public final Date getEndTI() {
    return endTI;
  }

  /**
   * @deprecated
   * @return int
   */
  public final int getTimeRanges() {
    return timeRanges;
  }

  /**
   * extra information about timeRanges
   * @deprecated
   * @return int[]
   */
  public final int[] getTimeIncrement() {
    return timeIncrement;
  }

  /**
   * @deprecated
   * @param tr timeRange used for StatProcess
   * @return int
   */
  public final int getStatProcess(int tr) {
    return timeIncrement[(tr * 6)];
  }

  /**
   * @deprecated
   * @param tr timeRange used for  TimeType
   * @return int
   */
  public final int getTimeType(int tr) {
    return timeIncrement[(tr * 6) + 1];
  }

  /**
   * @deprecated
   * @param tr timeRange used for TimeUnit
   * @return int
   */
  public final int getTimeUnit(int tr) {
    return timeIncrement[(tr * 6) + 2];
  }

  /**
   * @deprecated
   * @param tr timeRange used for LenTimeRange
   * @return int
   */
  public final int getLenTimeRange(int tr) {
    return timeIncrement[(tr * 6) + 3];
  }

  /**
   * @deprecated
   * @param tr timeRange used for IndicatorTU
   * @return int
   */
  public final int getIndicatorTU(int tr) {
    return timeIncrement[(tr * 6) + 4];
  }

  /**
   * @deprecated
   * @param tr timeRange used for TimeIncrement
   * @return int
   */
  public final int getTimeIncrement(int tr) {
    return timeIncrement[(tr * 6) + 5];
  }

  /**
   * @deprecated
   * number of forecasts for this parameter
   *
   * @return int
   */
  public final int getNumberForecasts() {
    return numberForecasts;
  }

  /**
   * @deprecated
   * length of PDS
   * @return int length
   */
  public int getLength() {
    return length;
  }

  /**
   * PDS as Grib2PDSVariables
   * @return  Grib2PDSVariables PDS vars
   */
  public Grib2PDSVariables getPdsVars() {
    return pdsVars;
  }

  /**
   * main.
   *
   * @param args  Grib name and PDS offset in Grib
   * @throws IOException on io error
   */
  // process command line switches
  static public void main(String[] args) throws IOException {
    RandomAccessFile raf    = null;
    PrintStream ps = System.out;
    String           infile = args[0];
    raf = new RandomAccessFile(infile, "r");
    raf.order(RandomAccessFile.BIG_ENDIAN);
    raf.skipBytes( Integer.parseInt( args[1]));
    Grib2ProductDefinitionSection pds = new Grib2ProductDefinitionSection( raf );
    Grib2PDSVariables gpv = pds.pdsVars;
    ps.println( "Section = "+ gpv.getSection());
    ps.println( "Length = "+ gpv.getLength());
    ps.println( "ProductDefinition = "+ gpv.getProductDefinition());

    assert( pds.length == gpv.getLength());
    assert( pds.section == gpv.getSection());
    assert( pds.coordinates == gpv.getCoordinates());
    assert( pds.productDefinition == gpv.getProductDefinition());
    assert( pds.parameterCategory == gpv.getParameterCategory());
    assert( pds.parameterNumber == gpv.getParameterNumber());
    if ( pds.productDefinition < 20 ) {  // NCEP models
      assert( pds.typeGenProcess == gpv.getTypeGenProcess());
      assert( pds.backGenProcess == gpv.getBackGenProcess());
      assert( pds.analysisGenProcess == gpv.getAnalysisGenProcess());
      assert( pds.hoursAfter == gpv.getHoursAfter());
      assert( pds.minutesAfter == gpv.getMinutesAfter());
      assert( pds.timeRangeUnit == gpv.getTimeRangeUnit());
      assert( pds.forecastTime == gpv.getForecastTime());
      assert( pds.typeFirstFixedSurface == gpv.getTypeFirstFixedSurface());
      assert( pds.FirstFixedSurfaceValue == gpv.getValueFirstFixedSurface());
      assert( pds.typeSecondFixedSurface == gpv.getTypeSecondFixedSurface());
      assert( pds.SecondFixedSurfaceValue == gpv.getValueSecondFixedSurface());
    }

    if ((pds.productDefinition == 1) || (pds.productDefinition == 11)) {
      assert( pds.typeEnsemble == gpv.getType());
      assert( pds.perturbNumber == gpv.getPerturbation());
      assert( pds.numberForecasts == gpv.getNumberForecasts());

    } else if( pds.productDefinition == 2 ) {
      assert( pds.typeEnsemble == gpv.getType());
      assert( pds.numberForecasts == gpv.getNumberForecasts());

    } else if( pds.productDefinition == 5 ) {
      assert( pds.typeEnsemble == gpv.getType() );
      assert( pds.lowerLimit == gpv.getValueLowerLimit() );
      assert( pds.upperLimit == gpv.getValueUpperLimit() );

    } else if( pds.productDefinition == 9 ) {
      assert( pds.typeEnsemble == gpv.getType() );
      assert( pds.numberForecasts == gpv.getNumberForecasts());
      // probability type
      assert( pds.lowerLimit == gpv.getValueLowerLimit() );
      assert( pds.upperLimit == gpv.getValueUpperLimit() );
    }

  }

}  // end Grib2ProductDefinitionSection


