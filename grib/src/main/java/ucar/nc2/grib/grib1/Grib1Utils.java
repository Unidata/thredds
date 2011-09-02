/*
 * Copyright (c) 1998 - 2011. University Corporation for Atmospheric Research/Unidata
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

package ucar.nc2.grib.grib1;

import ucar.grib.GribNumbers;
import ucar.nc2.time.CalendarPeriod;

/**
 * static utilities for Grib-1
 *
 * @author caron
 * @since 8/30/11
 */
public class Grib1Utils {

  /* Grib1
    Code table 4 Unit of time
    Code figure Meaning
    0 Minute
    1 Hour
    2 Day
    3 Month
    4 Year
    5 Decade (10 years)
    6 Normal (30 years)
    7 Century (100 years)
    8-9 Reserved
    10 3 hours
    11 6 hours
    12 12 hours
    13 Quarter of an hour
    14 Half an hour
    15-253 Reserved
   */

  static public CalendarPeriod getCalendarPeriod(int timeUnit) {

    switch (timeUnit) { // code table 4.4
      case 0:
        return CalendarPeriod.of(1, CalendarPeriod.Field.Minute);
      case 1:
        return CalendarPeriod.of(1, CalendarPeriod.Field.Hour);
      case 2:
        return CalendarPeriod.of(1, CalendarPeriod.Field.Day);
      case 3:
        return CalendarPeriod.of(1, CalendarPeriod.Field.Month);
      case 4:
        return CalendarPeriod.of(1, CalendarPeriod.Field.Year);
      case 5:
        return CalendarPeriod.of(10, CalendarPeriod.Field.Year);
      case 6:
        return CalendarPeriod.of(30, CalendarPeriod.Field.Year);
      case 7:
        return CalendarPeriod.of(100, CalendarPeriod.Field.Year);
      case 10:
        return CalendarPeriod.of(3, CalendarPeriod.Field.Hour);
      case 11:
        return CalendarPeriod.of(6, CalendarPeriod.Field.Hour);
      case 12:
        return CalendarPeriod.of(12, CalendarPeriod.Field.Hour);
      case 13:
        return CalendarPeriod.of(15, CalendarPeriod.Field.Minute);
      case 14:
        return CalendarPeriod.of(30, CalendarPeriod.Field.Minute);
      default:
        throw new UnsupportedOperationException("Unknown time unit = "+timeUnit);
    }
  }

  static public String getTimeUnitName(int timeRangeValue, int p1, int p2) {
    String timeRange = null;
    int forecastTime;

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
    return timeRange;
  }


  /**
   * LOOK - this is NCEP !
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
}
