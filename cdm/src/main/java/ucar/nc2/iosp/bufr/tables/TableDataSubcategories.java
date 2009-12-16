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
 * NCEP specific I guess ???
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
