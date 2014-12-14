/*
 * Copyright 1998-2014 University Corporation for Atmospheric Research/Unidata
 *
 *   Portions of this software were developed by the Unidata Program at the
 *   University Corporation for Atmospheric Research.
 *
 *   Access and use of this software shall impose the following obligations
 *   and understandings on the user. The user is granted the right, without
 *   any fee or cost, to use, copy, modify, alter, enhance and distribute
 *   this software, and any derivative works thereof, and its supporting
 *   documentation for any purpose whatsoever, provided that this entire
 *   notice appears in all copies of the software, derivative works and
 *   supporting documentation.  Further, UCAR requests that the user credit
 *   UCAR/Unidata in any publications that result from the use of this
 *   software or in any product that includes this software. The names UCAR
 *   and/or Unidata, however, may not be used in any advertising or publicity
 *   to endorse or promote any products or commercial entity unless specific
 *   written permission is obtained from UCAR/Unidata. The user also
 *   understands that UCAR/Unidata is not obligated to provide the user with
 *   any support, consulting, training or assistance of any kind with regard
 *   to the use, operation and performance of this software nor to provide
 *   the user with any updates, revisions, new versions or "bug fixes."
 *
 *   THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 *   IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *   WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *   DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 *   INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 *   FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 *   NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 *   WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */

package ucar.nc2.grib.grib1.tables;

import ucar.nc2.constants.CDM;
import ucar.nc2.grib.GribResourceReader;
import ucar.unidata.util.StringUtil2;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * NCEP River Forecast Centers
 *
 * @author caron
 * @since 1/13/12
 */
public class NcepRfcTables extends NcepTables {
  static private final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(NcepRfcTables.class);
  static private Map<Integer, String> nwsoSubCenter;

  NcepRfcTables(Grib1ParamTables tables) {
    super(8, tables);
  }

  @Override
  public String getGeneratingProcessName(int genProcess) {
    switch (genProcess) {
      case 150:
        return "NWS River Forecast System (NWSRFS)";
      case 151:
        return "NWS Flash Flood Guidance System (NWSFFGS)";
      case 152:
        return "Quantitative Precipitation Estimation (QPE) - 1 hr dur";
      case 154:
        return "Quantitative Precipitation Estimation (QPE) - 6 hr dur";
      case 155:
        return "Quantitative Precipitation Estimation (QPE) - 24hr dur";
      case 156:
        return "Process 1 (P1) Precipitation Estimation - automatic";
      case 157:
        return "Process 1 (P1) Precipitation Estimation - manual";
      case 158:
        return "Process 2 (P2) Precipitation Estimation - automatic";
      case 159:
        return "Process 2 (P2) Precipitation Estimation - manual";
      case 160:
        return "Multisensor Precipitation Estimation (MPE) - automatic";
      case 161:
        return "Multisensor Precipitation Estimation (MPE) - manual";
      case 165:
        return "Enhanced MPE - automatic";
      case 166:
        return "Bias Enhanced MPE - automatic";
      case 170:
        return "Post Analysis of Precipitation Estimation (aggregate)";
      case 171:
        return "XNAV Aggregate Precipitation Estimation";
      case 172:
        return "Mountain Mapper Precipitation Estimation";
      case 180:
        return "Quantitative Precipitation Forecast (QPF)";
      case 185:
        return "NOHRSC_OPPS";
      case 190:
        return "Satellite Autoestimator Precipitation";
      case 191:
        return "Satellite Interactive Flash Flood Analyzer (IFFA)";
    }
    return null;
  }

  ///////////////////////////////////////////////////////////////////////
  /* TABLE C - SUB-CENTERS FOR CENTER 9  US NWS FIELD STATIONS
  * from bdgparm.f John Halquist <John.Halquist@noaa.gov> 9/12/2011
  * These are not in the WMO common tables like NCEP's are
  */
  @Override
  public String getSubCenterName(int subcenter) {
    if (nwsoSubCenter == null)
      nwsoSubCenter = readNwsoSubCenter("resources/grib1/noaa_rfc/tableC.txt");
    if (nwsoSubCenter == null) return null;

    return nwsoSubCenter.get(subcenter);
  }

    // order: num, name, desc, unit
  private static Map<Integer, String> readNwsoSubCenter(String path) {
    Map<Integer, String> result = new HashMap<>();

    try (InputStream is = GribResourceReader.getInputStream(path)) {

      if (is == null) return null;

      BufferedReader br = new BufferedReader(new InputStreamReader(is, CDM.utf8Charset));

      // rdg - added the 0 line length check to cover the case of blank lines at
      //       the end of the parameter table file.
      while (true) {
        String line = br.readLine();
        if (line == null) break;
        if ((line.length() == 0) || line.startsWith("#")) continue;

        StringBuilder lineb =  new StringBuilder(line);
        StringUtil2.remove(lineb, "'+,/");
        String[] flds = lineb.toString().split("[:]");

        int val = Integer.parseInt(flds[0].trim()); // must have a number
        String name = flds[1].trim() + ": " + flds[2].trim();

        result.put(val, name);
      }

      return Collections.unmodifiableMap(result);  // all at once - thread safe

    } catch (IOException ioError) {
      logger.warn("An error occurred in Grib1Tables while trying to open the table " + path + " : " + ioError);
      return null;
    }

  }
}
