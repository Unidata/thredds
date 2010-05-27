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
 * Date: Jul 1, 2009
 * Time: 12:33:01 PM
 */

package ucar.grib.grib1;

/**
 * Class contains most of the hard coded tables for Grib1. Tables extracted from PDS
 * and GDS sections.
 */
public class Grib1Tables {

  // PDS tables
  
  /**
   * Generating Type Process Name.
   * ON388 - TABLE A.
   * Generating Process or Model.
   * from Originating Center 7 (USNWS NCEP).
   * GRIB1 - PDS Octet 6.
   * Revised 12/15/2005.
   *
   * Also used by Grib2
   *
   * @param typeGenProcess
   * @return model Name
   */
  // Only good for center 7 currently
  public static final String getTypeGenProcessName(int center, int typeGenProcess) {
    return getModelName(center, typeGenProcess);
  }

  /**
   * Currently only defined for center 7 NCEP
   * @param center
   * @param model
   * @return model for NCEP  or null
   */
  public static final String getModelName(int center, int model) {
    if( center == 7 ) { // currently only defined for NCEP
    switch (model) {

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
    } else {
      return null;
    }

  }

  /**
   * GDS Octet 6
   * WMO Table 6 Grid Definition
   */
  public static String getGridDefinition( int grid ) {

    switch ( grid ) {
      case 0:	return "Latitude/longitude grid - equidistant cylindrical or Plate Carre projection";
      case 1:	return "Mercator projection";
      case 2:	return "Gnomonic projection";
      case 3:	return "Lambert conformal, secant or tangent, conic or bi-polar, projection";
      case 4:	return "Gaussian latitude/longitude grid";
      case 5:	return "Polar stereographic projection";
      case 6:	return "Universal Transverse Mercator (UTM) projection";
      case 7:	return "Simple polyconic projection";
      case 8:	return "Albers equal-area, secant or tangent, conic or bi-polar, projection";
      case 9:	return "Miller's cylindrical projection";
      case 10:	return "Rotated latitude/longitude grid";
      //case 11-12:	return "Reserved";
      case 13:	return "Oblique Lambert conformal, secant or tangent, conic or bi-polar, projection";
      case 14:	return "Rotated Gaussian latitude/longitude grid";
      //case 15-19:	return "Reserved";
      case 20:	return "Stretched latitude/longitude grid";
      //case 21-23:	return "Reserved";
      case 24:	return "Stretched Gaussian latitude/longitude grid";
      //case 25-29:	return "Reserved";
      case 30:	return "Stretched and rotated latitude/longitude grids";
      //case 31-33:	return "Reserved";
      case 34:	return "Stretched and rotated Gaussian latitude/longitude grids";
      //case 35-49:	return "Reserved";
      case 50:	return "Spherical harmonic coefficients";
      //case 51-59:	return "Reserved";
      case 60:	return "Rotated spherical harmonic coefficients";
      //case 61-69:	return "Reserved";
      case 70:	return "Stretched spherical harmonics";
      //case 71-79:	return "Reserved";
      case 80:	return "Stretched and rotated spherical harmonic coefficients";
      //case 81-89:	return "Reserved";
      case 90:	return "Space view, perspective orthographic";
      //case 91-191:	return "Reserved";
      //case 192-254:	return "Reserved for local use";
      default:
        //return "Unknown ("+ Integer.toString( grid ) +")";
        return null;
    }
  }

  /**
   * Center name
   *
   * @param center int
   * @return center name
   */
  public static String getCenter_idName(int center) {

    switch (center) {

      case 0:
        return "WMO Secretariat";

      case 1:
      case 2:
      case 3:
        return "Melbourne(WMC)";

      case 4:
      case 5:
      case 6:
        return "Moscow(WMC)";

      case 7:
        return "US National Weather Service - NCEP(WMC)";

      case 8:
        return "US National Weather Service - NWSTG(WMC)";

      case 9:
        return "US National Weather Service - Other(WMC)";

      case 10:
      case 11:
        return "Cairo (RSMC/RAFC)";

      case 12:
      case 13:
        return "Dakar (RSMC/RAFC)";

      case 14:
      case 15:
        return "Nairobi (RSMC/RAFC)";

      case 16:
        return "Casablanca (RSMC)";

      case 17:
        return "Tunis (RSMC)";

      case 18:
      case 19:
        return "Tunis Casablanca (RSMC)";

      case 20:
        return "Las Palmas (RAFC)";

      case 21:
        return "Algiers (RSMC)";

      case 22:
        return "ACMAD";

      case 23:
        return "Mozambique (NMC)";

      case 24:
        return "Pretoria (RSMC)";

      case 25:
        return "La Reunion (RSMC)";

      case 26:
      case 27:
        return "Khabarovsk (RSMC)";

      case 28:
      case 29:
        return "New Delhi (RSMC/RAFC)";

      case 30:
      case 31:
        return "Novosibirsk (RSMC)";

      case 32:
        return "Tashkent (RSMC)";

      case 33:
        return "Jeddah (RSMC)";

      case 34:
      case 35:
        return "Japan Meteorological Agency - Tokyo (RSMC), ";

      case 36:
        return "Bangkok";

      case 37:
        return "Ulan Bator";

      case 38:
      case 39:
        return "Beijing (RSMC)";

      case 40:
        return "Seoul";

      case 41:
      case 42:
        return "Buenos Aires (RSMC/RAFC)";

      case 43:
      case 44:
        return "Brasilia (RSMC/RAFC)";

      case 45:
        return "Santiago";

      case 46:
        return "Brazilian Space Agency - INPE";

      case 47:
        return "Columbia (NMC)";

      case 48:
        return "Ecuador (NMC)";

      case 49:
        return "Peru (NMC)";

      case 50:
        return "Venezuela (NMC)";

      case 51:
        return "Miami (RSMC/RAFC)";

      case 52:
        return "Tropical Prediction Center(NHC), Miami (RSMC)";

      case 53:
      case 54:
        return "Canadian Meteorological Service - Montreal (RSMC)";

      case 55:
        return "San Francisco";

      case 56:
        return "ARINC Center";

      case 57:
        return "U.S. Air Force - Global Weather Center";

      case 58:
        return "U.S. Navy Fleet Numerical Meteorology and Oceanography Center";

      case 59:
        return "The NOAA Forecast Systems Laboratory";

      case 60:
        return "National Centre for Atmospheric Research (NCAR), Boulder, CO";

      case 61:
        return "Service ARGOS - Landover, MD, USA";

      case 62:
        return "US Naval Oceanographic Office";

      case 64:
        return "Honolulu";

      case 65:
      case 66:
        return "Darwin (RSMC)";

      case 67:
        return "Melbourne (RSMC)";

      case 69:
      case 70:
        return "Wellington (RSMC/RAFC)";

      case 71:
        return "Nadi (RSMC)";

      case 72:
        return "Singapore";

      case 73:
        return "Malaysia (NMC)";

      case 74:
      case 75:
        return "U.K. Met Office - Exeter (RSMC)";

      case 76:
        return "Moscow (RSMC/RAFC)";

      case 78:
      case 79:
        return "Offenbach (RSMC)";

      case 80:
      case 81:
        return "Rome (RSMC)";

      case 82:
      case 83:
        return "Norrkoping";

      case 84:
      case 85:
        return "French Weather Service - Toulouse (RSMC)";

      case 86:
        return "Helsinki";

      case 87:
        return "Belgrade";

      case 88:
        return "Oslo";

      case 89:
        return "Prague";

      case 90:
        return "Episkopi";

      case 91:
        return "Ankara";

      case 92:
        return "Frankfurt/Main (RAFC)";

      case 93:
        return "London (WAFC)";

      case 94:
        return "Copenhagen";

      case 95:
        return "Rota";

      case 96:
        return "Athens";

      case 97:
        return "European Space Agency (ESA)";

      case 98:
        return "European Center for Medium-Range Weather Forecasts (RSMC)";

      case 99:
        return "De Bilt, Netherlands";

      case 100:
        return "Brazzaville";

      case 101:
        return "Abidjan";

      case 102:
        return "Libyan Arab Jamahiriya (NMC)";

      case 103:
        return "Madagascar (NMC)";

      case 104:
        return "Mauritius (NMC)";

      case 105:
        return "Niger (NMC)";

      case 106:
        return "Seychelles (NMC)";

      case 107:
        return "Uganda (NMC)";

      case 108:
        return "Tanzania (NMC)";

      case 109:
        return "Zimbabwe (NMC)";

      case 110:
        return "Hong-Kong";

      case 111:
        return "Afghanistan (NMC)";

      case 112:
        return "Bahrain (NMC)";

      case 113:
        return "Bangladesh (NMC)";

      case 114:
        return "Bhutan (NMC)";

      case 115:
        return "Cambodia (NMC)";

      case 116:
        return "Democratic People's Republic of Korea (NMC)";

      case 117:
        return "Islamic Republic of Iran (NMC)";

      case 118:
        return "Iraq (NMC)";

      case 119:
        return "Kazakhstan (NMC)";

      case 120:
        return "Kuwait (NMC)";

      case 121:
        return "Kyrgyz Republic (NMC)";

      case 122:
        return "Lao People's Democratic Republic (NMC)";

      case 123:
        return "Macao, China";

      case 124:
        return "Maldives (NMC)";

      case 125:
        return "Myanmar (NMC)";

      case 126:
        return "Nepal (NMC)";

      case 127:
        return "Oman (NMC)";

      case 128:
        return "Pakistan (NMC)";

      case 129:
        return "Qatar (NMC)";

      case 130:
        return "Republic of Yemen (NMC)";

      case 131:
        return "Sri Lanka (NMC)";

      case 132:
        return "Tajikistan (NMC)";

      case 133:
        return "Turkmenistan (NMC)";

      case 134:
        return "United Arab Emirates (NMC)";

      case 135:
        return "Uzbekistan (NMC)";

      case 136:
        return "Socialist Republic of Viet Nam (NMC)";

      case 140:
        return "Bolivia (NMC)";

      case 141:
        return "Guyana (NMC)";

      case 142:
        return "Paraguay (NMC)";

      case 143:
        return "Suriname (NMC)";

      case 144:
        return "Uruguay (NMC)";

      case 145:
        return "French Guyana";

      case 146:
        return "Brazilian Navy Hydrographic Center";

      case 150:
        return "Antigua and Barbuda (NMC)";

      case 151:
        return "Bahamas (NMC)";

      case 152:
        return "Barbados (NMC)";

      case 153:
        return "Belize (NMC)";

      case 154:
        return "British Caribbean Territories Center";

      case 155:
        return "San Jose";

      case 156:
        return "Cuba (NMC)";

      case 157:
        return "Dominica (NMC)";

      case 158:
        return "Dominican Republic (NMC)";

      case 159:
        return "El Salvador (NMC)";

      case 160:
        return "US NOAA/NESDIS";

      case 161:
        return "US NOAA Office of Oceanic and Atmospheric Research";

      case 162:
        return "Guatemala (NMC)";

      case 163:
        return "Haiti (NMC)";

      case 164:
        return "Honduras (NMC)";

      case 165:
        return "Jamaica (NMC)";

      case 166:
        return "Mexico";

      case 167:
        return "Netherlands Antilles and Aruba (NMC)";

      case 168:
        return "Nicaragua (NMC)";

      case 169:
        return "Panama (NMC)";

      case 170:
        return "Saint Lucia (NMC)";

      case 171:
        return "Trinidad and Tobago (NMC)";

      case 172:
        return "French Departments";

      case 190:
        return "Cook Islands (NMC)";

      case 191:
        return "French Polynesia (NMC)";

      case 192:
        return "Tonga (NMC)";

      case 193:
        return "Vanuatu (NMC)";

      case 194:
        return "Brunei (NMC)";

      case 195:
        return "Indonesia (NMC)";

      case 196:
        return "Kiribati (NMC)";

      case 197:
        return "Federated States of Micronesia (NMC)";

      case 198:
        return "New Caledonia (NMC)";

      case 199:
        return "Niue";

      case 200:
        return "Papua New Guinea (NMC)";

      case 201:
        return "Philippines (NMC)";

      case 202:
        return "Samoa (NMC)";

      case 203:
        return "Solomon Islands (NMC)";

      case 210:
        return "Frascati (ESA/ESRIN)";

      case 211:
        return "Lanion";

      case 212:
        return "Lisboa";

      case 213:
        return "Reykjavik";

      case 214:
        return "Madrid";

      case 215:
        return "Zurich";

      case 216:
        return "Service ARGOS - Toulouse, FR";

      case 217:
        return "Bratislava";

      case 218:
        return "Budapest";

      case 219:
        return "Ljubljana";

      case 220:
        return "Warsaw";

      case 221:
        return "Zagreb";

      case 222:
        return "Albania (NMC)";

      case 223:
        return "Armenia (NMC)";

      case 224:
        return "Austria (NMC)";

      case 225:
        return "Azerbaijan (NMC)";

      case 226:
        return "Belarus (NMC)";

      case 227:
        return "Belgium (NMC)";

      case 228:
        return "Bosnia and Herzegovina (NMC)";

      case 229:
        return "Bulgaria (NMC)";

      case 230:
        return "Cyprus (NMC)";

      case 231:
        return "Estonia (NMC)";

      case 232:
        return "Georgia (NMC)";

      case 233:
        return "Dublin";

      case 234:
        return "Israel (NMC)";

      case 235:
        return "Jordan (NMC)";

      case 236:
        return "Latvia (NMC)";

      case 237:
        return "Lebanon (NMC)";

      case 238:
        return "Lithuania (NMC)";

      case 239:
        return "Luxembourg";

      case 240:
        return "Malta (NMC)";

      case 241:
        return "Monaco";

      case 242:
        return "Romania (NMC)";

      case 243:
        return "Syrian Arab Republic (NMC)";

      case 244:
        return "The former Yugoslav Republic of Macedonia (NMC)";

      case 245:
        return "Ukraine (NMC)";

      case 246:
        return "Republic of Moldova";

      case 254:
        return "EUMETSAT Operation Centre";

      default:
        return "Unknown";
    }

  }  // end getCenter_idName

  /**
   * SubCenter as String defined by center. Only NCEP has sub-center table
   *
   * @param center_id center
   * @param subCenter subCenter
   * @return subCenter
   */
  public static final String getSubCenter_idName(int center_id, int subCenter) {
    if (center_id == 7) {  //NCEP
      switch (subCenter) {

        case 0:
          //return "WMO Secretariat";
          return null;

        case 1:
          return "NCEP Re-Analysis Project";

        case 2:
          return "NCEP Ensemble Products";

        case 3:
          return "NCEP Central Operations";

        case 4:
          return "Environmental Modeling Center";

        case 5:
          return "Hydrometeorological Prediction Center";

        case 6:
          return "Ocean Prediction Center";

        case 7:
          return "Climate Prediction Center";

        case 8:
          return "Aviation Weather Center";

        case 9:
          return "Storm Prediction Center";

        case 10:
          return "Tropical Prediction Center";

        case 11:
          return "NWS Techniques Development Laboratory";

        case 12:
          return "NESDIS Office of Research and Applications";

        case 13:
          return "FAA";

        case 14:
          return "NWS Meteorological Development Laboratory(MDL)";

        case 15:
          return "The North American Regional Reanalysis (NARR) Project";

        case 16:
          return "Space Environment Center";

        default:
          return "Unknown NCEP Sub Center";
      }
    }
    // Can't determine because center specified
    //return getCenter_idName(subCenter);
    return null;
  }

  /**
   * ProductDefinition name.
   *
   * @param type
   * @return name of ProductDefinition
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
      case 137:
      case 139:
        return "Average of forecast accumulations";

      case 129:
        return "Average of successive forecast accumulations";

      case 130:
      case 138:
      case 140:
        return "Average of forecast averages";

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
   * tUnit time Unit.
   *
   * @param tUnit time Unit
   * @return tUnit type
   */
  static public String getTimeUnit(int tUnit) {
    switch (tUnit) {

      case 0:    // minute
        return "minutes";

      case 1:    // hours
        return "hours";

      case 2:    // day
        return "days";

      case 3:    // month
        return "months";

      case 4:    //  year
        return "years";

      case 5:    // decade
        return "decade";

      case 6:    // normal
        return "normal";

      case 7:    // century
        return "century";

      case 10:   //3 hours
        return "3hours";

      case 11:   // 6 hours
        return "6hours";

      case 12:   // 12 hours
        return "12hours";

      case 254:  // second
        return "seconds";

      default:
        System.err.println("PDS: Time Unit " + tUnit + " is not yet supported");
    }
    return "";
  }

  /**
   * tRange indicator.
   *
   * @param tRange indicator
   * @return tRange indicator
   */
  static public String getTimeRange(int tRange) {
    switch (tRange) {

      case 0:
        return "product valid at RT + P1";

      case 1:
        return "product valid for RT, P1=0";

      case 2:
        return "product valid from (RT + P1) to (RT + P2)";

      case 3:
        return "product is an average between (RT + P1) to (RT + P2)";

      case 4:
        return "product is an accumulation between (RT + P1) to (RT + P2)";

      case 5:
        return "product is the difference (RT + P2) - (RT + P1)";

      case 6:
        return "product is an average from (RT - P1) to (RT - P2)";

      case 7:
        return "product is an average from (RT - P1) to (RT + P2)";

      case 10:
        return "product valid at RT + P1";

      case 51:
        return "mean value from RT to (RT + P2)";

      case 113:
        return "Average of N forecasts, forecast period of P1, reference intervals of P2";

      default:
        System.err.println("PDS: Time Range Indicator "
            + tRange + " is not yet supported");
        return "";
    }
  }

  /**
     * type of vertical coordinate: Description or short Name
     * derived from  ON388 - TABLE 3.
     * @param id
     * @return level description as String
     */
    static public String getLevelDescription(int id) {

        switch (id) {

          case 0 :
              return "Reserved";

          case 1 :
              return "Ground or water surface";

          case 2 :
              return "Cloud base level";

          case 3 :
              return "Level of cloud tops";

          case 4 :
              return "Level of 0o C isotherm";

          case 5 :
              return "Level of adiabatic condensation lifted from the surface";

          case 6 :
              return "Maximum wind level";

          case 7 :
              return "Tropopause";

          case 8 :
              return "Nominal top of the atmosphere";

          case 9 :
              return "Sea bottom";

          case 20 :
              return "Isothermal level";

          case 100 :
              return "Isobaric surface";

          case 101 :
              return "Layer between 2 isobaric levels";

          case 102 :
              return "Mean sea level";

          case 103 :
              return "Altitude above mean sea level";

          case 104 :
              return "Layer between 2 altitudes above msl";

          case 105 :
              return "Specified height level above ground";

          case 106 :
              return "Layer between 2 specified height level above ground";

          case 107 :
              return "Sigma level";

          case 108 :
              return "Layer between 2 sigma levels";

          case 109 :
              return "Hybrid level";

          case 110 :
              return "Layer between 2 hybrid levels";

          case 111 :
              return "Depth below land surface";

          case 112 :
              return "Layer between 2 depths below land surface";

          case 113 :
              return "Isentropic theta level";

          case 114 :
              return "Layer between 2 isentropic levels";

          case 115 :
              return "Level at specified pressure difference from ground to level";

          case 116 :
              return "Layer between 2 level at pressure difference from ground to level";

          case 117 :
              return "Potential vorticity surface";

          case 119 :
              return "Eta level";

          case 120 :
              return "Layer between 2 Eta levels";

          case 121 :
              return "Layer between 2 isobaric levels";

          case 125 :
              return "Specified height level above ground";

          case 126 :
              return "Isobaric level";

          case 128 :
              return "Layer between 2 sigma levels (hi precision)";

          case 141 :
              return "Layer between 2 isobaric surfaces";

          case 160 :
              return "Depth below sea level";

          case 200 :
              return "Entire atmosphere";

          case 201 :
              return "Entire ocean";

          case 204 :
              return "Highest tropospheric freezing level";

          case 206 :
              return "Grid scale cloud bottom level";

          case 207 :
              return "Grid scale cloud top level";

          case 209 :
              return "Boundary layer cloud bottom level";

          case 210 :
              return "Boundary layer cloud top level";

          case 211 :
              return "Boundary layer cloud layer";

          case 212 :
              return "Low cloud bottom level";

          case 213 :
              return "Low cloud top level";

          case 214 :
              return "Low Cloud Layer";

          case 222 :
              return "Middle cloud bottom level";

          case 223 :
              return "Middle cloud top level";

          case 224 :
              return "Middle Cloud Layer";

          case 232 :
              return "High cloud bottom level";

          case 233 :
              return "High cloud top level";

          case 234 :
              return "High Cloud Layer";

          case 242 :
              return "Convective cloud bottom level";

          case 243 :
              return "Convective cloud top level";

          case 244 :
              return "Convective cloud layer";

          case 245 :
              return "Lowest level of the wet bulb zero";

          case 246 :
              return "Maximum equivalent potential temperature level";

          case 247 :
              return "Equilibrium level";

          case 248 :
              return "Shallow convective cloud bottom level";

          case 249 :
              return "Shallow convective cloud top level";

          case 251 :
              return "Deep convective cloud bottom level";

          case 252 :
              return "Deep convective cloud top level";

          case 255 :
              return "Missing";

          default :
              return "Unknown=" + id;
        }

    }

    /**
     * short name of level.
     * @param id
     * @return name of level
     */
    static public String getLevelName(int id) {

        switch (id) {

          case 1 :
              return "surface";

          case 2 :
              return "cloud_base";

          case 3 :
              return "cloud_tops";

          case 4 :
              return "zeroDegC_isotherm";

          case 5 :
              return "adiabatic_condensation_lifted";

          case 6 :
              return "maximum_wind";

          case 7 :
              return "tropopause";

          case 8 :
              return "atmosphere_top";

          case 9 :
              return "sea_bottom";

          case 20 :
              return "isotherm";

          case 100 :
              return "isobaric";

          case 101 :
              return "layer_between_two_isobaric";

          case 102 :
              return "msl";

          case 103 :
              return "altitude_above_msl";

          case 104 :
              return "layer_between_two_altitudes_above_msl";

          case 105 :
              return "height_above_ground";

          case 106 :
              return "layer_between_two_heights_above_ground";

          case 107 :
              return "sigma";

          case 108 :
              return "layer_between_two_sigmas";

          case 109 :
              return "hybrid";

          case 110 :
              return "layer_between_two_hybrids";

          case 111 :
              return "depth_below_surface";

          case 112 :
              return "layer_between_two_depths_below_surface";

          case 113 :
              return "isentrope";

          case 114 :
              return "layer_between_two_isentrope";

          case 115 :
              return "pressure_difference";

          case 116 :
              return "layer_between_two_pressure_difference_from_ground";

          case 117 :
              return "potential_vorticity_surface";

          case 119 :
              return "eta";

          case 120 :
              return "layer_between_two_eta";

          case 121 :
              return "layer_between_two_isobaric_surfaces";

          case 125 :
              return "height_above_ground";

          case 126 :
              return "isobaric";

          case 128 :
              return "layer_between_two_sigmas_hi";

          case 141 :
              return "layer_between_two_isobaric_surfaces";

          case 160 :
              return "depth_below_sea";

          case 200 :
              return "entire_atmosphere";

          case 201 :
              return "entire_ocean";

          case 204 :
              return "highest_tropospheric_freezing";

          case 206 :
              return "grid_scale_cloud bottom";

          case 207 :
              return "grid_scale_cloud_top";

          case 209 :
              return "boundary_layer_cloud_bottom";

          case 210 :
              return "boundary_layer_cloud_top";

          case 211 :
              return "boundary_layer_cloud_layer";

          case 212 :
              return "low_cloud_bottom";

          case 213 :
              return "low_cloud_top";

          case 214 :
              return "low_cloud_layer";

          case 222 :
              return "middle_cloud_bottom";

          case 223 :
              return "middle_cloud_top";

          case 224 :
              return "middle_cloud_layer";

          case 232 :
              return "high_cloud_bottom";

          case 233 :
              return "high_cloud_top";

          case 234 :
              return "high_cloud_layer";

          case 242 :
              return "convective_cloud_bottom";

          case 243 :
              return "convective_cloud_top";

          case 244 :
              return "convective_cloud_layer";

          case 245 :
              return "lowest_level_of_wet_bulb_zero";

          case 246 :
              return "maximum_equivalent_potential_temperature";

          case 247 :
              return "equilibrium";

          case 248 :
              return "shallow_convective_cloud_bottom";

          case 249 :
              return "shallow_convective_cloud_top";

          case 251 :
              return "deep_convective_cloud_bottom";

          case 252 :
              return "deep_convective_cloud_top";

          case 255 :
              return "";

          default :
              return "Unknown" + id;
        }

    }  // end getNameShort

    /**
     * type of vertical coordinate: units
     * derived from  ON388 - TABLE 3.
     * @param  id units number
     * @return unit as String
     */
    static public String getLevelUnits(int id) {

        switch (id) {

          case 20 :
          case 113 :
          case 114 :
              return "K";

          case 100 :
          case 101 :
          case 115 :
          case 116 :
          case 121 :
          case 141 :
              return "hPa";

          case 103 :
          case 104 :
          case 105 :
          case 106 :
          case 160 :
              return "m";

          case 107 :
          case 108 :
          case 128 :
              return "sigma";

          case 111 :
          case 112 :
          case 125 :
              return "cm";

          case 117 :
              return "10-6Km2/kgs";

          case 126 :
              return "Pa";
        }
        return "";
    }  // end getUnits

  
  // GDS static tables

  /**
   * Get Grid name.
   *
   * @param type
   * @return name
   */
  static public String getName(int type) {
    switch (type) {

      case 0:
        return "Latitude_Longitude";

      case 1:
        return "Mercator";

      case 2:
        return "Gnomonic";

      case 3:
        return "Lambert_Conformal";

      case 4:
        return "Gaussian_Latitude_Longitude";

      case 5:
      case 87:
        return "Polar_Stereographic";

      case 6:
        return "Universal_Transverse_Mercator";

      case 7:
        return "Simple_Polyconic";

      case 8:
        return "Albers_Equal_Area";

      case 9:
        return "Millers_Cylindrical";

      case 10:
        return "Rotated_Latitude_Longitude";

      case 13:
        return "Oblique_Lambert_Conformal";

      case 14:
        return "Rotated_Gaussian_Latitude_Longitude";

      case 20:
        return "Stretched_Latitude_Longitude";

      case 24:
        return "Stretched_Gaussian_Latitude_Longitude";

      case 30:
        return "Stretched_and_Rotated_Latitude_Longitude";

      case 34:
        return "Stretched_and_Rotated_Gaussian_Latitude_Longitude";

      case 50:
        return "Spherical_Harmonic_Coefficients";

      case 60:
        return "Rotated_Spherical_Harmonic_Coefficients";

      case 70:
        return "Stretched_Spherical_Harmonics";

      case 80:
        return "Stretched_and_Rotated_Spherical_Harmonic_Coefficients";

      case 90:
        return "Space_View_Perspective_or_Orthographic";

      case 201:
        return "Arakawa_Semi_Staggered_E_grid_on_Rotated_Latitude_Longitude_Grid_Point_Array_1D";

      case 202:
        return "Arakawa_Filled_E_Grid_on_Rotated_Latitude_Longitude_Grid_Point_Array_1D";

      case 203:
        return "Arakawa_Semi_Staggered_E_grid_on_Rotated_Latitude_Longitude_Grid_Point_Array_2D";

      case 204:
        return "Curvilinear_Orthogonal";

      case 205:
        return "Arakawa_Filled_B_Grid_on_Rotated_Latitude_Longitude";

      case 254:
        return "Grid_point_locations_specified_by_auxilliary_file_or_GRIB_message";
    }

    return "Unknown Grid ("+ Integer.toString( type ) +")";

  }  // end getName

  /**
   * shape of grid.
   *
   * @param code grid shape code
   * @return String grid shape name
   */
  static public String getShapeName(int code) {
    if (code == 1) {
      return "oblate spheroid";
    } else {
      return "spherical";
    }
  }

  /**
   * Grib 1 has static radius.
   *
   * @return ShapeRadius of 6367.47
   */
  public static final double getShapeRadius() {
    return 6367.47;
  }

  /**
   * Grib 1 has static MajorAxis.
   *
   * @return ShapeMajorAxis of 6378.160
   */
  public static final double getShapeMajorAxis() {
    return 6378.160;
  }

  /**
   * Grib 1 has static MinorAxis.
   *
   * @return ShapeMinorAxis of 6356.775
   */
  public static final double getShapeMinorAxis() {
    return 6356.775;
  }


}
