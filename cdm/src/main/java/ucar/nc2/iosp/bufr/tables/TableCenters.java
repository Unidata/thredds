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

package ucar.nc2.iosp.bufr.tables;

/**
 * COMMON CODE TABLE C-1: Identification of originating/generating centre
 * This should be kept in an external table so it can be updated regularly.
 *
 * @see "http://www.wmo.int/pages/prog/www/WMOCodes/Operational/CommonTables/BufrCommon-11-2007.pdf"
 * @author caron
 * @since Oct 24, 2008
 */
public class TableCenters {
  
  /**
   * Name of Identification of center.
   *
   * @param center_id center id
   * @return center name
   */
  static public String getCenterName(int center_id) {

    switch (center_id) {
      case 0:
        return "WMO Secretariat";
      case 1:
      case 2:
        return "Melbourne";
      case 4:
      case 5:
        return "Moscow";
      case 7:
        return "US National Weather Service (NCEP)";
      case 8:
        return "US National Weather Service (NWSTG)";
      case 9:
        return "US National Weather Service (other)";
      case 10:
        return "Cairo (RSMC/RAFC)";
      case 12:
        return "Dakar (RSMC/RAFC)";
      case 14:
        return "Nairobi (RSMC/RAFC)";
      case 18:
        return "Tunis Casablanca (RSMC)";
      case 20:
        return "Las Palmas (RAFC)";
      case 21:
        return "Algiers (RSMC)";
      case 24:
        return "Pretoria (RSMC)";
      case 25:
        return "La Reunion (RSMC)";
      case 26:
        return "Khabarovsk (RSMC)";
      case 28:
        return "New Delhi (RSMC/RAFC)";
      case 30:
        return "Novosibirsk (RSMC)";
      case 32:
        return "Tashkent (RSMC)";
      case 33:
        return "Jeddah (RSMC)";
      case 34:
        return "Tokyo (RSMC), Japan Meteorological Agency";
      case 36:
        return "Bangkok";
      case 37:
        return "Ulan Bator";
      case 38:
        return "Beijing (RSMC)";
      case 40:
        return "Seoul";
      case 41:
        return "Buenos Aires (RSMC/RAFC)";
      case 43:
        return "Brasilia (RSMC/RAFC)";
      case 45:
        return "Santiago";
      case 46:
        return "Brazilian Space Agency ? INPE";
      case 51:
        return "Miami (RSMC/RAFC)";
      case 52:
        return "Miami RSMC, National Hurricane Center";
      case 53:
        return "Montreal (RSMC)";
      case 55:
        return "San Francisco";
      case 57:
        return "Air Force Weather Agency";
      case 58:
        return "Fleet Numerical Meteorology and Oceanography Center";
      case 59:
        return "The NOAA Forecast Systems Laboratory";
      case 60:
        return "United States National Centre for Atmospheric Research (NCAR)";
      case 64:
        return "Honolulu";
      case 65:
        return "Darwin (RSMC)";
      case 67:
        return "Melbourne (RSMC)";
      case 69:
        return "Wellington (RSMC/RAFC)";
      case 71:
        return "Nadi (RSMC)";
      case 74:
        return "UK Meteorological Office Bracknell (RSMC)";
      case 76:
        return "Moscow (RSMC/RAFC)";
      case 78:
        return "Offenbach (RSMC)";
      case 80:
        return "Rome (RSMC)";
      case 82:
        return "Norrkoping";
      case 85:
        return "Toulouse (RSMC)";
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
        return "ECMWF, RSMC";
      case 99:
        return "De Bilt";
      case 110:
        return "Hong-Kong";
      case 160:
        return "US NOAA/NESDIS";
      case 210:
        return "Frascati (ESA/ESRIN)";
      case 211:
        return "Lanion";
      case 212:
        return "Lisboa";
      case 213:
        return "Reykjavik";
      case 254:
        return "EUMETSAT Operation Centre";

      default:
        return "Unknown center=" + center_id;
    }
  }

  /**
   * Name of NCEP subcenter.
   * Source?
   * @param subcenter_id NCEP subcenter id
   * @return subcenter name
   */
  static public String getNCEPSubCenterName(int subcenter_id) {
      switch (subcenter_id) {
        case 1:
          return "NCEP / Re-Analysis Project";
        case 2:
          return "NCEP /  Ensemble Products";
        case 3:
          return "NCEP /  Central Operations";
        case 4:
          return "NCEP / Environmental Modeling Center";
        case 5:
          return "NCEP / Hydrometeorological Prediction Center";
        case 6:
          return "NCEP / Marine Prediction Center";
        case 7:
          return "NCEP / Climate Prediction Center";
        case 8:
          return "NCEP / Aviation Weather Center";
        case 9:
          return "NCEP / Storm Prediction Center";
        case 10:
          return "NCEP / Tropical Prediction Center";
        case 11:
          return "NCEP / NWS Techniques Development Laboratory";
        case 12:
          return "NCEP / NESDIS Office of Research and Applications";
        case 13:
          return "NCEP / FAA";
        case 14:
          return "NCEP / NWS Meteorological Development Laboratory";
        case 15:
          return "NCEP / The North American Regional Reanalysis (NARR) Project";
        default:
          return "US National Weather Service (NCEP) subcenter=" + subcenter_id;
    }
  }
}
