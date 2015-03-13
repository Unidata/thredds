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

import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import ucar.nc2.grib.GribResourceReader;
import ucar.nc2.grib.GribLevelType;
import ucar.nc2.grib.GribStatType;
import ucar.nc2.grib.VertCoord;
import ucar.nc2.grib.grib1.Grib1ParamTime;
import ucar.nc2.grib.grib1.Grib1SectionProductDefinition;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * NCEP overrides of GRIB tables
 * LOOK: Why not a singleton?
 *
 * @author caron
 * @since 1/13/12
 */
public class NcepTables extends Grib1Customizer {
  static private final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(NcepTables.class);

  private static Map<Integer, String> genProcessMap;  // shared by all instances
  private static Map<Integer, GribLevelType> levelTypesMap;  // shared by all instances

  NcepTables(Grib1ParamTables tables) {
    super(7, tables);
  }

  protected NcepTables(int center, Grib1ParamTables tables) {
    super(center, tables);
  }

  ////////////// time types ////////////////////////////////////
  // http://www.nco.ncep.noaa.gov/pmb/docs/on388/table5.html
  @Override
  public GribStatType getStatType(int timeRangeIndicator) {

    switch (timeRangeIndicator) {
      case 128:
      case 129:
      case 130:
      case 131:
      case 132:
      case 133:
      case 137:
      case 138:
      case 139:
      case 140:
        return GribStatType.Average;
      case 134:
        return GribStatType.RootMeanSquare;
      case 135:
      case 136:
        return GribStatType.StandardDeviation;
      default:
        return super.getStatType(timeRangeIndicator);
    }
  }

    /////////////////// local time types
  @Override
  public Grib1ParamTime getParamTime(Grib1SectionProductDefinition pds) {
    int p1 = pds.getTimeValue1();  // octet 19
    int p2 = pds.getTimeValue2();  // octet 20
    int timeRangeIndicator = pds.getTimeRangeIndicator(); // octet 21
    int n = pds.getNincluded();

    int start = 0;
    int end = 0;
    int forecastTime = 0;
    boolean isInterval = false;

    switch (timeRangeIndicator) {

      case 128:
      case 129:
      case 130:
      case 131:
      case 137:
      case 138:
      case 139:
      case 140:
        isInterval = true;
        start = p1;
        end = p2;
        break;

      case 132:
      case 133:
      case 134:
      case 135:
      case 136:
        forecastTime = p1;
        start = p1;
        end = (n > 0) ? p1 + (n-1) * p2 : p1;  // LOOK ??
        isInterval = (n > 0);
        break;

      default:
        return super.getParamTime(pds);
    }

    return new Grib1ParamTime(this, timeRangeIndicator, isInterval, start, end, forecastTime);
  }

  @Override
  public String getTimeTypeName(int timeRangeIndicator) {

    switch (timeRangeIndicator) {
      case 128:
        /* Average of forecast accumulations. P1 = start of accumulation period. P2 = end of accumulation period.
          Reference time is the start time of the first forecast, other forecasts at 24-hour intervals.
          Number in Ave = number of forecasts used. */
        return "Average of forecast accumulations at 24 hour intervals, period = (RT + P1) to (RT + P2)";

      case 129:
        /* Average of successive forecast accumulations. P1 = start of accumulation period.
        P2 = end of accumulation period. Reference time is the start time of the first forecast,
        other forecasts at (P2 - P1) intervals. Number in Ave = number of forecasts used */
        return "Average of successive forecast accumulations, period = (RT + P1) to (RT + P2)";

      case 130:
        /* Average of forecast averages. P1 = start of averaging period. P2 = end of averaging period.
        Reference time is the start time of the first forecast, other forecasts at 24-hour intervals.
        Number in Ave = number of forecast used*/
        return "Average of forecast averages at 24 hour intervals, period = (RT + P1) to (RT + P2)";

      case 131:
        /* Average of successive forecast averages. P1 = start of averaging period. P2 = end of averaging period.
        Reference time is the start time of the first forecast, other forecasts at (P2 - P1) intervals.
        Number in Ave = number of forecasts used*/
        return "Average of successive forecast averages, period = (RT + P1) to (RT + P2)";

      case 132:
        return "Climatological Average of N analyses, each a year apart, starting from initial time R and for the period from R+P1 to R+P2.";

      case 133:
        return "Climatological Average of N forecasts, each a year apart, starting from initial time R and for the period from R+P1 to R+P2.";

      case 134:
        return "Climatological Root Mean Square difference between N forecasts and their verifying analyses, each a year apart, starting with initial time R and for the period from R+P1 to R+P2.";

      case 135:
        return "Climatological Standard Deviation of N forecasts from the mean of the same N forecasts, for forecasts one year apart. ";

      case 136:
        return "Climatological Standard Deviation of N analyses from the mean of the same N analyses, for analyses one year apart.";

      case 137:
        /* Average of forecast accumulations. P1 = start of accumulation period. P2 = end of accumulation period.
        Reference time is the start time of the first forecast, other forecasts at 6-hour intervals.
        Number in Ave = number of forecast used */
        return "Average of forecast accumulations at 6 hour intervals, period = (RT + P1) to (RT + P2)";

      case 138:
        /* Average of forecast averages. P1 = start of averaging period. P2 = end of averaging period.
        Reference time is the start time of the first forecast, other forecasts at 6-hour intervals.
        Number in Ave = number of forecast used */
        return "Average of forecast averages at 6 hour intervals, period = (RT + P1) to (RT + P2)";

      case 139:
        /* Average of forecast accumulations. P1 = start of accumulation period. P2 = end of accumulation period.
        Reference time is the start time of the first forecast, other forecasts at 12-hour intervals.
        Number in Ave = number of forecast used */
        return "Average of forecast accumulations at 12 hour intervals, period = (RT + P1) to (RT + P2)";

      case 140:
        /* Average of forecast averages. P1 = start of averaging period. P2 = end of averaging period.
        Reference time is the start time of the first forecast, other forecasts at 12-hour intervals.
        Number in Ave = number of forecast used */
        return "Average of forecast averages at 12 hour intervals, period = (RT + P1) to (RT + P2)";

      default:
        return super.getTimeTypeName(timeRangeIndicator);
    }
  }

  //////////////////////////////////////////// genProcess

  @Override
  public String getGeneratingProcessName(int genProcess) {
    if (genProcessMap == null)
        genProcessMap = getNcepGenProcess();
    if (genProcessMap == null) return null;

    return genProcessMap.get(genProcess);
  }

  // public so can be called from Grib2
  static public Map<Integer, String> getNcepGenProcess() {
    if (genProcessMap != null) return genProcessMap;
    String path = "resources/grib1/ncep/ncepTableA.xml";
    try (InputStream is = GribResourceReader.getInputStream(path)) {

      if (is == null) {
        logger.error("Cant find NCEP Table 1 = " + path);
        return null;
      }

      SAXBuilder builder = new SAXBuilder();
      org.jdom2.Document doc = builder.build(is);
      Element root = doc.getRootElement();

      HashMap<Integer, String> result = new HashMap<>(200);
      List<Element> params = root.getChildren("parameter");
      for (Element elem1 : params) {
        int code = Integer.parseInt(elem1.getAttributeValue("code"));
        String desc = elem1.getChildText("description");
        result.put(code, desc);
      }

      return Collections.unmodifiableMap(result);  // all at once - thread safe

    } catch (IOException ioe) {
      logger.error("Cant read NCEP Table 1 = " + path, ioe);
      return null;
    } catch (JDOMException e) {
      logger.error("Cant parse NCEP Table 1 = " + path, e);
      return null;
    }
  }

  ///////////////////////////////////////// levels
  protected GribLevelType getLevelType(int code) {
    if (code < 129)
      return super.getLevelType(code); // LOOK dont let NCEP override standard tables (??) looks like a conflict with level code 210 (!)

    if (levelTypesMap == null)
      levelTypesMap = readTable3("resources/grib1/ncep/ncepTable3.xml");
    if (levelTypesMap == null)
      return super.getLevelType(code);

    GribLevelType levelType = levelTypesMap.get(code);
    if (levelType != null) return levelType;

    return super.getLevelType(code);
  }


}
