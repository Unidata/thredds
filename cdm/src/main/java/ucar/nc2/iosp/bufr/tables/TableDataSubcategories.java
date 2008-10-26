/*
 * Copyright 1997-2008 Unidata Program Center/University Corporation for
 * Atmospheric Research, P.O. Box 3000, Boulder, CO 80307,
 * support@unidata.ucar.edu.
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or (at
 * your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser
 * General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package ucar.nc2.iosp.bufr.tables;

/**
 * @author caron
 * @since Oct 25, 2008
 */
public class TableDataSubcategories {

  /**
   * Name of sub category, probably common table C-12.
   * This should be kept in an external table so it can be updated regularly.
   *
   * @param cat
   * @param subCat
   * @return sub category Name
   */
  static public String getSubCategory(int cat, int subCat) {
    switch (cat) {
      case 0: // 0 Surface - land
        switch (subCat) {
          case 1:
            return "Synoptic manual and automatic";
          case 7:
            return "Aviation METAR";
          case 11:
            return "SHEF";
          case 12:
            return "Aviation SCD";
          case 20:
            return "MESONET Denver";
          case 21:
            return "MESONET RAWS";
          case 22:
            return "MESONET MesoWest ";
          case 23:
            return "MESONET APRS Weather";
          case 24:
            return "MESONET Kansas DOT";
          case 25:
            return "MESONET Florida";
          case 30:
            return "MESONET Other";

          default:
            return "Unknown";
        }

      case 1:
        switch (subCat) {
          case 1:
            return "Ship manual and automatic";
          case 2:
            return "Drifting Buoy";
          case 3:
            return "Moored Buoy";
          case 4:
            return "Land based C-MAN station";
          case 5:
            return "Tide gage";
          case 6:
            return "Sea level pressure bogus";
          case 7:
            return "Coast guard";
          case 8:
            return "Moisture bogus";
          case 9:
            return "SSMIr";
          default:
            return "Unknown";
        }

      case 2:
        switch (subCat) {
          case 1:
            return "Radiosonde fixed land";
          case 2:
            return "Radiosonde mobile land";
          case 3:
            return "Radiosonde ship";
          case 4:
            return "Dropwinsonde";
          case 5:
            return "Pibal";
          case 7:
            return "Wind Profiler NOAA";
          case 8:
            return "NEXRAD winds";
          case 9:
            return "Wind profiler PILOT";

          default:
            return "Unknown";
        }

      case 3:
        switch (subCat) {
          case 1:
            return "Geostationary";
          case 2:
            return "Polar orbitin";
          case 3:
            return "Sun synchronous";

          default:
            return "Unknown";
        }

      case 4:
        switch (subCat) {
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
            return "Marine Prediction Center";
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
            return "NWS Meteorological Development Laboratory";
          case 15:
            return "The North American Regional Reanalysis (NARR) Project";
          default:
            return "Unknown";
        }

      case 5:
        switch (subCat) {
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
            return "Marine Prediction Center";
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
            return "NWS Meteorological Development Laboratory";
          case 15:
            return "The North American Regional Reanalysis (NARR) Project";
          default:
            return "Unknown";
        }
      case 12:

        switch (subCat) {

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
            return "Marine Prediction Center";
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
            return "NWS Meteorological Development Laboratory";
          case 15:
            return "The North American Regional Reanalysis (NARR) Project";
          default:
            return "Unknown";
        }
      case 31:

        switch (subCat) {

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
            return "Marine Prediction Center";
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
            return "NWS Meteorological Development Laboratory";
          case 15:
            return "The North American Regional Reanalysis (NARR) Project";
          default:
            return "Unknown";
        }
      default:
        return "Unknown";
    }
  }
}
