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

  /**
   * Currently only defined for center 7 NCEP
   * @param center center id
   * @param genProcess generating process id (pds octet 6)
   * @return generating process name, or null if unknown
   */
  public static final String getTypeGenProcessName(int center, int genProcess) {
    if( center != 7 ) return null;

    switch (genProcess) {

      case 2:
        return "Ultra Violet Index Model";

      case 3:
        return "NCEP/ARL Transport and Dispersion Model";

      case 4:
        return "NCEP/ARL Smoke Model";

      case 5:
        return "Satellite Derived Precipitation and temperatures, from IR";

      case 6:
        return "NCEP/ARL Dust Model";

      case 10:
        return "Global Wind-Wave Forecast Model";

      case 11:
        return "Global Multi-Grid Wave Model";

      case 12:
        return "Probabilistic Storm Surge";

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
        return "Global Optimum Interpolation Analysis (GOI) from Final run";

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
        return "Analysis from GFS";

      case 82:
        return "Analysis from Global Data Assimilation System";

      case 84:
        return "MESO NAM Model";

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

      case 107:
        return "Global Ensemble Forecast System";

      case 108:
        return "LAMP";

      case 109:
        return "RTMA (Real Time Mesoscale Analysis)";

      case 110:
        return "NAM Model - 15km version";

      case 111:
        return "NAM model, generic resolution (Used in SREF processing)";

      case 112:
        return "WRF-NMM model, generic resolution NMM=Nondydrostatic Mesoscale Model (NCEP)";

      case 113:
        return "Products from NCEP SREF processing";

      case 114:
        return "NAEFS Products from joined NCEP, CMC global ensembles";

      case 115:
        return "Downscaled GFS from NAM eXtension";

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
        return "Merge of fields from the RUC, NAM, and Spectral Model";

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

      case 184:
        return "Climatological Calibrated Precipitation Analysis - CCPA";

      case 190:
        return "National Convective Weather Diagnostic generated by NCEP/AWC";

      case 191:
        return "Current Icing Potential automated product generated by NCEP/AWC";

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

      case 199:
        return "Climate Forecast System Reanalysis (CFSR)";

      case 200:
        return "CPC Manual Forecast Product";

      case 201:
        return "CPC Automated Product";

      case 210:
        return "EPA Air Quality Forecast - Currently North East US domain";

      case 211:
        return "EPA Air Quality Forecast - Currently Eastern US domain";

      case 215:
        return "SPC Manual Forecast Product";

      case 220:
        return "NCEP/OPC automated product";

      case 255:
        return "Missing";

        default:
        //return "Unknown "+ Integer.toString( model );
        return null;
      }

  }

}
