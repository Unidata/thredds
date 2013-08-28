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
 * Grib1ProductDefinitionSection.java  1.1  09/30/2004
 *
 *   Parameters use external tables, so program does not have to be modified to
 *      add support for new tables.
 *   @see  ucar.nc2.iosp.grid.GridParameter , GribPDSParamTable, and GribPDSLevel classes.
 */

package ucar.grib.grib1;


import ucar.grib.*;

import ucar.nc2.iosp.grid.GridParameter;
import ucar.nc2.wmo.CommonCodeTable;
import ucar.unidata.io.RandomAccessFile;

import java.io.IOException;
import java.io.PrintStream;
import java.util.Date;
import java.util.Calendar;


/**
 * A class representing the product definition section (PDS) of a GRIB record.
 */

public final class Grib1ProductDefinitionSection {

  /**
   * Length in bytes of this PDS.
   */
  private final int length;

  /**
   * Exponent of decimal scale.
   */
  private final int decscale;

  /**
   * ID of grid type.
   */
  private final int grid_id;

  /**
   * True, if GDS exists.
   */
  private final boolean gds_exists;

  /**
   * True, if BMS exists.
   */
  private final boolean bms_exists;

  /**
   * The parameter as defined in the Parameter Table.
   */
  private final GridParameter parameter;

  /**
   * parameterNumber.
   */
  private final int parameterNumber;

  /**
   * Class containing the information about the level.  This helps to actually
   * use the data, otherwise the string for level will have to be parsed.
   */
  private final GribPDSLevel level;

  /**
   * Model Run/Analysis/Reference time.
   */
  private final Date baseTime;

  /**
   * _more_
   */
  private final long refTime;

  /**
   * Forecast time (valid time).
   */
  private int forecastTime;

  /**
   * Forecast time. (valid time 1)
   * Also used as starting time when times represent a period.
   */
  private int p1;

  /**
   * Ending time when times represent a period (valid time 2).
   */
  private int p2;

  /**
   * Strings used in building a string to represent the time(s) for this PDS
   * See the decoder for octet 21 to get an understanding.
   */
  private String timeRange = null;

  /**
   * _more_
   */
  private final int timeRangeValue;

  /**
   * _more_
   */
  private String tUnit = null;

  /**
   * Parameter Table Version number.
   */
  private final int table_version;

  /**
   * Identification of center e.g. 88 for Oslo.
   */
  private final int center_id;

  /**
   * Identification of subcenter.
   */
  private final int subcenter_id;

  /**
   * Identification of Generating Process.
   */
  private final int typeGenProcess;

  /**
   * ensemble products have more information.
   */
  //private Grib1Ensemble epds = null;

  /**
   * PDS length not equal to number bytes read.
   */
  private final boolean lengthErr = false;

  /**
   * PDS as Variables from a byte[]
   */
  private final Grib1Pds pdsVars;
  // *** constructors *******************************************************

  /**
   * Constructs a <tt>Grib1ProductDefinitionSection</tt> object from a raf.
   *
   * @param raf with PDS content
   * @throws IOException           if raf can not be opened etc.
   * @throws NotSupportedException if raf contains no valid GRIB file
   */
  public Grib1ProductDefinitionSection(RandomAccessFile raf) throws NotSupportedException, IOException {

    long sectionEnd = raf.getFilePointer();
    // octets 1-3 PDS length
    length = GribNumbers.uint3(raf);
    //System.out.println( "PDS length = " + length );

    // read in whole PDS as byte[]
    byte[] pdsData = new byte[length];
    // reset to beginning of section and read data
    raf.skipBytes(-3);
    raf.read(pdsData);
    pdsVars = new Grib1Pds(pdsData);

    // reset for variable section read and set sectionEnd
    raf.seek(sectionEnd + 3);
    sectionEnd += length;

    // Paramter table octet 4
    table_version = raf.read();

    // Center  octet 5
    center_id = raf.read();

    // octet 6 Generating Process - See Table A
    typeGenProcess = raf.read();

    // octet 7 (id of grid type) - not supported yet
    grid_id = raf.read();

    // octet 8 (flag for presence of GDS and BMS)
    int exists = raf.read();
    gds_exists = (exists & 128) == 128;
    bms_exists = (exists & 64) == 64;

    // octet 9 (parameter and unit)
    parameterNumber = raf.read();

    // octets 10-12 (level)
    int levelType = raf.read();
    int levelValue1 = raf.read();
    int levelValue2 = raf.read();
    level = new GribPDSLevel(levelType, levelValue1, levelValue2);

    // octets 13-17 (base time for reference time)
    int year = raf.read();
    int month = raf.read();
    int day = raf.read();
    int hour = raf.read();
    int minute = raf.read();

    // get info for forecast time
    // octet 18 Forecast time unit
    int fUnit = raf.read();

    switch (fUnit) {

      case 0:    // minute
        tUnit = "minute";
        break;

      case 1:    // hours
        tUnit = "hour";
        break;

      case 2:    // day
        tUnit = "day";
        break;

      case 3:    // month
        tUnit = "month";
        break;

      case 4:    //1 year
        tUnit = "1year";
        break;

      case 5:    // decade
        tUnit = "decade";
        break;

      case 6:    // normal
        tUnit = "day";
        break;

      case 7:    // century
        tUnit = "century";
        break;

      case 10:   //3 hours
        tUnit = "3hours";
        break;

      case 11:   // 6 hours
        tUnit = "6hours";
        break;

      case 12:   // 12 hours
        tUnit = "12hours";
        break;

      case 254:  // second
        tUnit = "second";
        break;

      default:
        System.err.println("PDS: Time Unit " + fUnit + " is not yet supported");
    }

    // octet 19 & 20 used to create Forecast time
    p1 = raf.read();
    p2 = raf.read();

    // octet 21 (time range indicator)
    timeRangeValue = raf.read();
    // forecast time is always at the end of the range
    //System.out.println( "PDS timeRangeValue =" + timeRangeValue );
    switch (timeRangeValue) {

      case 0:
        timeRange = "product valid at RT + P1";
        forecastTime = p1;
        break;

      case 1:
        timeRange = "product valid for RT, P1=0";
        forecastTime = 0;
        break;

      case 2:
        timeRange = "product valid from (RT + P1) to (RT + P2)";
        forecastTime = p2;
        break;

      case 3:
        timeRange = "product is an average between (RT + P1) to (RT + P2)";
        forecastTime = p2;
        break;

      case 4:
        timeRange = "product is an accumulation between (RT + P1) to (RT + P2)";
        forecastTime = p2;
        break;

      case 5:
        timeRange = "product is the difference (RT + P2) - (RT + P1)";
        forecastTime = p2;
        break;

      case 6:
        timeRange = "product is an average from (RT - P1) to (RT - P2)";
        forecastTime = -p2;
        break;

      case 7:
        timeRange = "product is an average from (RT - P1) to (RT + P2)";
        forecastTime = p2;
        break;

      case 10:
        timeRange = "product valid at RT + P1";
        // p1 really consists of 2 bytes p1 and p2
        forecastTime = p1 = GribNumbers.int2(p1, p2);
        p2 = 0;
        break;

      case 51:
        timeRange = "mean value from RT to (RT + P2)";
        forecastTime = p2;
        break;

      case 113:
        timeRange = "Average of N forecasts, forecast period of P1, reference intervals of P2";
        forecastTime = p1;
        break;

      case 123:
        timeRange = "Average of N uninitialized analyses, starting at the reference time, at intervals of P2";
        forecastTime = 0;
        break;

      case 124:
        timeRange = "Accumulation of N uninitialized analyses, starting at the reference time, at intervals of P2";
        forecastTime = 0;
        break;

      default:
        System.err.println("PDS: Time Range Indicator " + timeRangeValue + " is not yet supported");
    }

    // octet 22 & 23
    int avgInclude = GribNumbers.int2(raf);

    // octet 24
    int avgMissing = raf.read();

    // octet 25
    int century = raf.read() - 1;
    if (century == -1) century = 20;

    // octet 26, sub center
    subcenter_id = raf.read();

    // octets 27-28 (decimal scale factor)
    decscale = GribNumbers.int2(raf);

    Calendar calendar = Calendar.getInstance();
    calendar.clear();
    calendar.setTimeZone(java.util.TimeZone.getTimeZone("GMT"));
    calendar.set(Calendar.DST_OFFSET, 0);
    calendar.set((century * 100 + year), month - 1, day, hour, minute, 0);

    baseTime = calendar.getTime();
    refTime = calendar.getTimeInMillis();
    parameter = GribPDSParamTable.getParameter(center_id, subcenter_id, table_version, parameterNumber);

    Grib1Pds gpv = pdsVars;
    if (false && gpv.isEnsemble()) {
      if ((center_id == 7) && (subcenter_id == 2)) {  // ensemble product
        System.out.println("Parm =" + parameterNumber
                + " Extension =" + gpv.getExtension()
                + " Type =" + gpv.getType()
                + " ID =" + gpv.getID()
                + " ProductID =" + gpv.getProductID()
                + " SpatialorProbability =" + gpv.getSpatialorProbability());
      } else if (center_id == 98) {

        System.out.print("Extension =" + gpv.getExtension()
                + " Class =" + gpv.getEcmwfClass()
                + " Type =" + gpv.getType()
                + " Stream =" + gpv.getStream());
        if (gpv.getExtension() == 30) {
          System.out.println(" Ensemble number =" + gpv.getPerturbationNumber()
                  + " NumberForecasts =" + gpv.getNumberForecasts()
          );
        }
        /*
        if (gpv.isEnsemble() ) {
           System.out.println("Class ="+ gpv.getType() +" Type ="+ gpv.getID()
             +" Stream ="+ gpv.getStream() +" Labeling ="+ gpv.getOctet50()
             +" NumberOfForecast ="+ gpv.getOctet51()  );
        } else {
          System.out.println("Type ="+ gpv.getType() +" ID ="+ gpv.getID()
             //+" Product ="+ gpv.getProduct() +" Octet45 ="+ gpv.getOctet45()
             +" Probability Product ="+ gpv.getProbabilityProduct()
             +" Probability Type ="+ gpv.getProbabilityType() );
           System.out.println( "Lower Limit ="+ gpv.getValueLowerLimit() +" Upper Limit ="+ gpv.getValueUpperLimit());
           System.out.println("Number of members ="+ gpv.getNumberForecasts() );

             System.out.println("Cluster size ="+ gpv.getSizeClusters()
             +" Number of Clusters ="+ gpv.getNumberClusters()
             +" Cluster method ="+ gpv.getMethod()
             +" Northern latitude ="+ gpv.getNorthLatitude()
             +" Southern latitude ="+ gpv.getSouthLatitude()
             +" Easthern longitude ="+ gpv.getEastLongitude()
             +" Westhern longitude ="+ gpv.getWestLongitude());
        }
        */
        //        } else if (length != 28) {                      // check if all bytes read in section
        //            //lengthErr = true;
        //            int extra;
        //            for (int i = 29; i <= length; i++) {
        //                extra = raf.read();
        //            }
      }
    }
    raf.seek(sectionEnd);
  }  // end Grib1ProductDefinitionSection


  /**
   * Check if GDS exists.
   *
   * @return true, if GDS exists
   * @deprecated
   */
  public final boolean gdsExists() {
    return gds_exists;
  }

  /**
   * Check if BMS exists.
   *
   * @return true, if BMS exists
   * @deprecated
   */
  public final boolean bmsExists() {
    return bms_exists;
  }

  /**
   * Center as int.
   *
   * @return center_id
   * @deprecated
   */
  public final int getCenter() {
    return center_id;
  }

  /**
   * Process Id as int.
   *
   * @return typeGenProcess
   * @deprecated
   */
  public final int getTypeGenProcess() {
    return typeGenProcess;
  }

  /**
   * @param typeGenProcess
   * @return
   * @deprecated
   */
  public static final String getTypeGenProcessName(int typeGenProcess) {

    switch (typeGenProcess) {

      case 2:
        return "Ultra Violet Index Model";

      case 3:
        return "NCEP/ARL Transport and Dispersion Model";

      case 4:
        return "NCEP/ARL Smoke Model";

      case 5:
        return "Satellite Derived Precipitation and temperatures, from IR";

      case 10:
        return "Global Wind-Wave Forecast Model";

      case 19:
        return "Limited-area Fine Mesh (LFM) analysis";

      case 25:
        return "Snow Cover Analysis";

      case 30:
        return "Forecaster generated field";

      case 31:
        return "Value added post processed field";

      case 39:
        return "Nested Grid forecast Model (NGM)";

      case 42:
        return "Global Optimum Interpolation Analysis (GOI) from GFS model";

      case 43:
        return "Global Optimum Interpolation Analysis (GOI) from  Final run";

      case 44:
        return "Sea Surface Temperature Analysis";

      case 45:
        return "Coastal Ocean Circulation Model";

      case 46:
        return "HYCOM - Global";

      case 47:
        return "HYCOM - North Pacific basin";

      case 48:
        return "HYCOM - North Atlantic basin";

      case 49:
        return "Ozone Analysis from TIROS Observations";

      case 52:
        return "Ozone Analysis from Nimbus 7 Observations";

      case 53:
        return "LFM-Fourth Order Forecast Model";

      case 64:
        return "Regional Optimum Interpolation Analysis (ROI)";

      case 68:
        return "80 wave triangular, 18-layer Spectral model from GFS model";

      case 69:
        return "80 wave triangular, 18 layer Spectral model from Medium Range Forecast run";

      case 70:
        return "Quasi-Lagrangian Hurricane Model (QLM)";

      case 73:
        return "Fog Forecast model - Ocean Prod. Center";

      case 74:
        return "Gulf of Mexico Wind/Wave";

      case 75:
        return "Gulf of Alaska Wind/Wave";

      case 76:
        return "Bias corrected Medium Range Forecast";

      case 77:
        return "126 wave triangular, 28 layer Spectral model from GFS model";

      case 78:
        return "126 wave triangular, 28 layer Spectral model from Medium Range Forecast run";

      case 79:
        return "Backup from the previous run";

      case 80:
        return "62 wave triangular, 28 layer Spectral model from Medium Range Forecast run";

      case 81:
        return "Spectral Statistical Interpolation (SSI) analysis from  GFS model";

      case 82:
        return "Spectral Statistical Interpolation (SSI) analysis from Final run.";

      case 84:
        return "MESO ETA Model";

      case 86:
        return "RUC Model, from Forecast Systems Lab (isentropic; scale: 60km at 40N)";

      case 87:
        return "CAC Ensemble Forecasts from Spectral (ENSMB)";

      case 88:
        return "NOAA Wave Watch III (NWW3) Ocean Wave Model";

      case 89:
        return "Non-hydrostatic Meso Model (NMM)";

      case 90:
        return "62 wave triangular, 28 layer spectral model extension of the Medium Range Forecast run";

      case 91:
        return "62 wave triangular, 28 layer spectral model extension of the GFS model";

      case 92:
        return "62 wave triangular, 28 layer spectral model run from the Medium Range Forecast final analysis";

      case 93:
        return "62 wave triangular, 28 layer spectral model run from the T62 GDAS analysis of the Medium Range Forecast run";

      case 94:
        return "T170/L42 Global Spectral Model from MRF run";

      case 95:
        return "T126/L42 Global Spectral Model from MRF run";

      case 96:
        return "Global Forecast System Model (formerly known as the Aviation)";

      case 98:
        return "Climate Forecast System Model -- Atmospheric model (GFS) coupled to a multi level ocean model.";

      case 99:
        return "Miscellaneous Test ID";

      case 100:
        return "RUC Surface Analysis (scale: 60km at 40N)";

      case 101:
        return "RUC Surface Analysis (scale: 40km at 40N)";

      case 105:
        return "RUC Model from FSL (isentropic; scale: 20km at 40N)";

      case 108:
        return "LAMP";

      case 109:
        return "RTMA (Real Time Mesoscale Analysis)";

      case 110:
        return "ETA Model - 15km version";

      case 111:
        return "Eta model, generic resolution (Used in SREF processing)";

      case 112:
        return "WRF-NMM model, generic resolution NMM=Nondydrostatic Mesoscale Model (NCEP)";

      case 113:
        return "Products from NCEP SREF processing";

      case 115:
        return "Downscaled GFS from Eta eXtension";

      case 116:
        return "WRF-EM model, generic resolution EM - Eulerian Mass-core (NCAR - aka Advanced Research WRF)";

      case 120:
        return "Ice Concentration Analysis";

      case 121:
        return "Western North Atlantic Regional Wave Model";

      case 122:
        return "Alaska Waters Regional Wave Model";

      case 123:
        return "North Atlantic Hurricane Wave Model";

      case 124:
        return "Eastern North Pacific Regional Wave Model";

      case 125:
        return "North Pacific Hurricane Wave Model";

      case 126:
        return "Sea Ice Forecast Model";

      case 127:
        return "Lake Ice Forecast Model";

      case 128:
        return "Global Ocean Forecast Model";

      case 129:
        return "Global Ocean Data Analysis System (GODAS)";

      case 130:
        return "Merge of fields from the RUC, Eta, and Spectral Model";

      case 131:
        return "Great Lakes Wave Model";

      case 140:
        return "North American Regional Reanalysis (NARR)";

      case 141:
        return "Land Data Assimilation and Forecast System";

      case 150:
        return "NWS River Forecast System (NWSRFS)";

      case 151:
        return "NWS Flash Flood Guidance System (NWSFFGS)";

      case 152:
        return "WSR-88D Stage II Precipitation Analysis";

      case 153:
        return "WSR-88D Stage III Precipitation Analysis";

      case 180:
        return "Quantitative Precipitation Forecast generated by NCEP";

      case 181:
        return "River Forecast Center Quantitative Precipitation Forecast mosaic generated by NCEP";

      case 182:
        return "River Forecast Center Quantitative Precipitation estimate mosaic generated by NCEP";

      case 183:
        return "NDFD product generated by NCEP/HPC";

      case 190:
        return "National Convective Weather Diagnostic generated by NCEP/AWC";

      case 191:
        return "Current Icing Potential automated product genterated by NCEP/AWC";

      case 192:
        return "Analysis product from NCEP/AWC";

      case 193:
        return "Forecast product from NCEP/AWC";

      case 195:
        return "Climate Data Assimilation System 2 (CDAS2)";

      case 196:
        return "Climate Data Assimilation System 2 (CDAS2) - used for regeneration runs";

      case 197:
        return "Climate Data Assimilation System (CDAS)";

      case 198:
        return "Climate Data Assimilation System (CDAS) - used for regeneration runs";

      case 200:
        return "CPC Manual Forecast Product";

      case 201:
        return "CPC Automated Product";

      case 210:
        return "EPA Air Quality Forecast";

      case 211:
        return "EPA Air Quality Forecast";

      case 215:
        return "SPC Manual Forecast Product";

      case 220:
        return "NCEP/OPC automated product";

      case 255:
        return "Missing";

      default:
        return "Unknown";
    }

  }

  /**
   * Grid ID as int.
   *
   * @return grid_id
   * @deprecated
   */
  public final int getGrid_Id() {
    return grid_id;
  }

  public final String getCenterName() {
    return CommonCodeTable.getCenterName(center_id, 1);
  }

  public final int getSubCenter() {
    return subcenter_id;
  }

  public final String getSubCenterName() {
    return CommonCodeTable.getSubCenterName(center_id, subcenter_id);
  }


  /**
   * gets the Table version as a int.
   *
   * @return table_version
   * @deprecated
   */
  public final int getTableVersion() {
    return table_version;
  }

  /**
   * Get the exponent of the decimal scale used for all data values.
   *
   * @return exponent of decimal scale
   * @deprecated
   */
  public final int getDecimalScale() {
    return decscale;
  }

  /**
   * Get the number of the parameter.
   *
   * @return index number of parameter in table
   * @deprecated
   */
  public final int getParameterNumber() {
    return parameterNumber;
  }

  /**
   * Get the type of the parameter.
   *
   * @return type of parameter
   * @deprecated
   */
  public final String getType() {
    return (parameter == null) ? "" : parameter.getName();
  }

  /**
   * Get a descritpion of the parameter.
   *
   * @return descritpion of parameter
   * @deprecated
   */
  public final String getDescription() {
    return (parameter == null) ? "" : parameter.getDescription();
  }

  /**
   * Get the name of the unit of the parameter.
   *
   * @return name of the unit of the parameter
   * @deprecated
   */
  public final String getUnit() {
    return (parameter == null) ? "" : parameter.getUnit();
  }

  /**
   * Get the name for the type of level for forecast/analysis.
   *
   * @return name of level (height or pressure)
   * @deprecated
   */
  public final String getLevelName() {
    return level.getName();
  }

  /**
   * Get the numeric value for this level.
   *
   * @return name of level (height or pressure)
   * @deprecated
   */
  public final int getLevelType() {
    return level.getIndex();
  }

  /**
   * Get the numeric value for this level.
   *
   * @return name of level (height or pressure)
   * @deprecated
   */
  public final float getLevelValue1() {
    return level.getValue1();
  }

  /**
   * Get value 2 (if it exists) for this level.
   *
   * @return name of level (height or pressure)
   * @deprecated
   */
  public final float getLevelValue2() {
    return level.getValue2();
  }

  /**
   * Get the base (analysis) time of the forecast.
   *
   * @return date and time
   * @deprecated
   */
  public final Date getBaseTime() {
    return baseTime;
  }

  /**
   * gets reference time as a long millis.
   *
   * @return refTime
   * @deprecated
   */
  public final long getRefTime() {
    return refTime;
  }

  /**
   * Get the time of the forecast.
   *
   * @return date and time
   * @deprecated
   */
  public final int getForecastTime() {
    return forecastTime;
  }

  /**
   * P1.
   *
   * @return p1
   * @deprecated
   */
  public final int getP1() {
    return p1;
  }

  /**
   * P2.
   *
   * @return p2
   * @deprecated
   */
  public final int getP2() {
    return p2;
  }

  /*
   * Get the parameter for this pds.
   *
   * @return parameter
   * @deprecated
   *
  public final GridParameter getParameter() {
    return parameter;
  } */

  /**
   * gets the time unit ie hour.
   *
   * @return tUnit
   * @deprecated
   */
  public final String getTimeUnit() {
    return tUnit;
  }

  /**
   * ProductDefinition as a int.
   *
   * @return timeRangeValue
   * @deprecated
   */
  public final int getProductDefinition() {
    return timeRangeValue;
  }

  /**
   * ProductDefinition name.
   *
   * @param type
   * @return name of ProductDefinition
   * @deprecated
   */
  public static String getProductDefinitionName(int type) {
    switch (type) {

      case 0:
        return "Forecast/Uninitialized Analysis/Image Product";

      case 1:
        return "Initialized analysis product";

      case 2:
        return "Product with a valid time between P1 and P2";

      case 3:
      case 6:
      case 7:
        return "Average";

      case 4:
        return "Accumulation";

      case 5:
        return "Difference";

      case 10:
        return "product valid at reference time P1";

      case 51:
        return "Climatological Mean Value";

      case 113:
      case 115:
      case 117:
        return "Average of N forecasts";

      case 114:
      case 116:
        return "Accumulation of N forecasts";

      case 118:
        return "Temporal variance";

      case 119:
      case 125:
        return "Standard deviation of N forecasts";

      case 123:
        return "Average of N uninitialized analyses";

      case 124:
        return "Accumulation of N uninitialized analyses";

      case 128:
        return "Average of daily forecast accumulations";

      case 129:
        return "Average of successive forecast accumulations";

      case 130:
        return "Average of daily forecast averages";

      case 131:
        return "Average of successive forecast averages";

      case 132:
        return "Climatological Average of N analyses";

      case 133:
        return "Climatological Average of N forecasts";

      case 134:
        return "Climatological Root Mean Square difference between N forecasts and their verifying analyses";

      case 135:
        return "Climatological Standard Deviation of N forecasts from the mean of the same N forecasts";

      case 136:
        return "Climatological Standard Deviation of N analyses from the mean of the same N analyses";
    }
    return "Unknown";
  }

  /**
   * grid_id as int.
   *
   * @return grid_id
   * @deprecated
   */
  public final int getGrid_ID() {
    return grid_id;
  }

  /**
   * TimeRange as int.
   *
   * @return timeRangeValue
   * @deprecated
   */
  public final int getTimeRange() {
    return timeRangeValue;
  }

  /**
   * TimeRange as String.
   *
   * @return timeRange
   * @deprecated
   */
  public final String getTimeRangeString() {
    return timeRange;
  }

  /**
   * PDS length did not correspond with read .
   *
   * @return lengthErr
   * @deprecated
   */
  public final boolean getLengthErr() {
    return lengthErr;
  }

  /**
   * length of PDS
   *
   * @return int length
   * @deprecated
   */
  public int getLength() {
    return length;
  }

  /**
   * PDS as Grib1PDSVariables
   *
   * @return Grib1PDSVariables PDS vars
   */
  public Grib1Pds getPdsVars() {
    return pdsVars;
  }

  /**
   * main.
   *
   * @param args Grib name and PDS offset in Grib
   * @throws IOException on io error
   */
  // process command line switches
  static public void main(String[] args) throws IOException, NoValidGribException {
    RandomAccessFile raf = null;
    PrintStream ps = System.out;
    String infile = args[0];
    raf = new RandomAccessFile(infile, "r");
    raf.order(RandomAccessFile.BIG_ENDIAN);
    // This is the GDS offset
    raf.skipBytes(Integer.parseInt(args[1]));
    // backup to PDS; most of the time it's only 28 bytes
    //raf.skipBytes( -28 ); //TODO: check
    Grib1ProductDefinitionSection pds = new Grib1ProductDefinitionSection(raf);
    Grib1Pds gpv = pds.pdsVars;
    ps.println("Length = " + gpv.getLength());
    ps.println("TimeRangeIndicator = " + gpv.getTimeRangeIndicator());

    assert (pds.length == gpv.getLength());
    assert (pds.table_version == gpv.getParameterTableVersion());
    assert (pds.center_id == gpv.getCenter());
    assert (pds.typeGenProcess == gpv.getGenProcessId());

    assert (pds.typeGenProcess == gpv.getGenProcessId());
    assert (pds.grid_id == gpv.getGridId());
    assert (pds.gds_exists == gpv.gdsExists());
    assert (pds.bms_exists == gpv.bmsExists());
    assert (pds.parameterNumber == gpv.getParameterNumber());
    assert (pds.getLevelType() == gpv.getLevelType1());
    assert (pds.getLevelValue1() == gpv.getLevelValue1());
    assert (pds.getLevelValue2() == gpv.getLevelValue2());

    assert (pds.baseTime.equals(gpv.getReferenceDate()));
    assert (pds.refTime == gpv.getReferenceTime());
    //assert( pds.tUnit == gpv.getTimeUnit() );
    assert (pds.p1 == gpv.getP1());
    assert (pds.p2 == gpv.getP2());
    assert (pds.timeRangeValue == gpv.getTimeRangeIndicator());
    assert (pds.subcenter_id == gpv.getSubCenter());
    assert (pds.decscale == gpv.getDecimalScale());
    assert (pds.forecastTime == gpv.getForecastTime());
    /* //TODO:check and delete
    System.out.println("Center ="+ pds.center_id +" Sub Center ="+ pds.subcenter_id
        +" table_version ="+ pds.table_version);
    if( gpv.isEnsemble() && pds.center_id == 98 ) {
       System.out.println("Class ="+ gpv.getType() +" Type ="+ gpv.getID()
         +" Stream ="+ gpv.getStream() +" Labeling ="+ gpv.getOctet50()
         +" NumberOfForecast ="+ gpv.getOctet51()  );
    } else {
      System.out.println("Type ="+ gpv.getType() +" ID ="+ gpv.getID()
         +" Product ="+ gpv.getProductID() +" Octet45 ="+ gpv.getSpatialorProbability()
         +" Probability Product ="+ gpv.getProbabilityProduct()
         +" Probability Type ="+ gpv.getProbabilityType() );
       System.out.println( "Lower Limit ="+ gpv.getValueLowerLimit() +" Upper Limit ="+ gpv.getValueUpperLimit());
       System.out.println("Number of members ="+ gpv.getNumberForecasts() );

         System.out.println("Cluster size ="+ gpv.getSizeClusters()
         +" Number of Clusters ="+ gpv.getNumberClusters()
         +" Cluster method ="+ gpv.getMethod()
         +" Northern latitude ="+ gpv.getNorthLatitude()
         +" Southern latitude ="+ gpv.getSouthLatitude()
         +" Easthern longitude ="+ gpv.getEastLongitude()
         +" Westhern longitude ="+ gpv.getWestLongitude());
    }
    */
  }

}  // end class Grib1ProductDefinitionSection


