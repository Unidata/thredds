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

package ucar.nc2.grib.grib1.tables;

import net.jcip.annotations.Immutable;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import ucar.grib.GribResourceReader;
import ucar.nc2.grib.GribTables;
import ucar.nc2.grib.grib1.Grib1ParamLevel;
import ucar.nc2.wmo.CommonCodeTable;
import ucar.unidata.util.StringUtil2;

import java.io.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This is the interface to manage GRIB-1 Code and Param Tables.
 *
 * Allow different table versions in the same file.
 * Allow overriding standard grib1 tables on the dataset level.
 *
 * @author caron
 * @since 9/13/11
 */
@Immutable
public class Grib1Tables implements GribTables {
  static private final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(Grib1ParamTable.class);

  /**
   * Get a Grib1Tables object, optionally specifiying a parameter table or lookup table specific to this dataset.
   * @param paramTablePath path to a parameter table, in format Grib1ParamTable can read.
   * @param lookupTablePath path to a lookup table, in format Grib1StandardTables.Lookup.readLookupTable() can read.
   * @return Grib1Tables
   * @throws IOException on read error
   */
  static public Grib1Tables factory(String paramTablePath, String lookupTablePath) throws IOException {
    if (paramTablePath == null && lookupTablePath == null) return new Grib1Tables();

    Grib1Tables result = new Grib1Tables();
    if (paramTablePath != null) {
      result.override = new Grib1ParamTable(paramTablePath);
      if (result.override == null)
        throw new FileNotFoundException("cant read parameter table=" + paramTablePath);
    }
    if (lookupTablePath != null) {
      result.lookup = new Grib1StandardTables.Lookup();
      if (!result.lookup.readLookupTable(lookupTablePath))
        throw new FileNotFoundException("cant read lookup table=" + lookupTablePath);
    }

    return result;
  }

  /**
   * Get a Grib1Tables object, optionally specifiying a parameter table in XML specific to this dataset.
   * @param paramTableElem parameter table in XML
   * @return Grib1Tables
   * @throws IOException on read error
   */
  static public Grib1Tables factory(org.jdom.Element paramTableElem) throws IOException {
    if (paramTableElem == null) return new Grib1Tables();

    Grib1Tables result = new Grib1Tables();
    result.override = new Grib1ParamTable(paramTableElem);

    return result;
  }

  ///////////////////////////////////////////////////////////////////////////

  private Grib1StandardTables.Lookup lookup; // if lookup table was set
  private Grib1ParamTable override; // if parameter table was set

  public Grib1Tables() {
  }

  @Override
  public String getLevelNameShort(int code) {
    return Grib1ParamLevel.getNameShort(code);
  }

  public String getLevelDescription(int levelType) {
    return Grib1ParamLevel.getLevelDescription(levelType);
  }

  public Grib1Parameter getParameter(int center, int subcenter, int tableVersion, int param_number) {
    Grib1Parameter param = null;
    if (override != null)
      param = override.getParameter(param_number);
    if (param == null && lookup != null)
      param = lookup.getParameter(center, subcenter, tableVersion, param_number);
    if (param == null)
      param = Grib1StandardTables.getParameter(center, subcenter, tableVersion, param_number); // standard tables
    return param;
  }

  /**
   * Debugging only
   */
  public Grib1ParamTable getParameterTable(int center, int subcenter, int tableVersion) {
    Grib1ParamTable result = null;
    if (lookup != null)
      result = lookup.getParameterTable(center, subcenter, tableVersion);
    if (result == null)
      result = Grib1StandardTables.getParameterTable(center, subcenter, tableVersion); // standard tables
    return result;
  }

  ////////////////////////////////////////////////////////////////////////////////////

  private Map<Integer, String> ncepGenProcess;

  /**
   * Currently only defined for center 7 NCEP and 9
   *
   * @param center     center id
   * @param genProcess generating process id (pds octet 6)
   * @return generating process name, or null if unknown
   */
  public String getTypeGenProcessName(int center, int genProcess) {
    /*
    ON388 - TABLE A
    Generating Process or Model
    from Originating Center 7 (USNWS NCEP)
    GRIB1 - PDS Octet 6
     */
    if (center == 7) {
      if (ncepGenProcess == null) readNcepGenProcess("resources/grib1/ncep/ncepTableA.xml");
      if (ncepGenProcess == null) return null;
      return ncepGenProcess.get(genProcess);
    }

    // TABLE A - GENERATING PROCESS OR MODEL - ORIGINATING CENTER 9
    //        * from bdgparm.f John Halquist <John.Halquist@noaa.gov> 9/12/2011
    if (center == 9) {
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
    }
    return null;
  }

  private void readNcepGenProcess(String path) {
    InputStream is = null;
    try {
      is = GribResourceReader.getInputStream(path);
      if (is == null) {
        logger.error("Cant find NCEP Table 1 = " + path);
        return;
      }

      SAXBuilder builder = new SAXBuilder();
      org.jdom.Document doc = builder.build(is);
      Element root = doc.getRootElement();

      HashMap<Integer, String> result = new HashMap<Integer, String>(200);
      List<Element> params = root.getChildren("parameter");
      for (Element elem1 : params) {
        int code = Integer.parseInt(elem1.getAttributeValue("code"));
        String desc = elem1.getChildText("description");
        result.put(code, desc);
      }

      ncepGenProcess = result;  // all at once - thread safe
      return;

    } catch (IOException ioe) {
      logger.error("Cant read NCEP Table 1 = " + path, ioe);
      return;

    } catch (JDOMException e) {
      logger.error("Cant parse NCEP Table 1 = " + path, e);
      return;

    } finally {
      if (is != null) try {
        is.close();
      } catch (IOException e) {
      }
    }
  }

  /////////////////////////////////////////////////////////////////////////////////////

  private static Map<Integer, String> nwsoSubCenter;

  /* TABLE C - SUB-CENTERS FOR CENTER 9  US NWS FIELD STATIONS
  * from bdgparm.f John Halquist <John.Halquist@noaa.gov> 9/12/2011
  */
  public static String getSubCenterName(int center, int subcenter) {
    if (center == 9) {
      if (nwsoSubCenter == null) readNwsoSubCenter("resources/grib1/noaa_rfc/tableC.txt");
      if (nwsoSubCenter == null) return null;
      return nwsoSubCenter.get(subcenter);
    }

    return CommonCodeTable.getSubCenterName(center, subcenter);
  }

  // order: num, name, desc, unit
  private static void readNwsoSubCenter(String path) {
    HashMap<Integer, String> result = new HashMap<Integer, String>();

    InputStream is = null;
    try {
      is = ucar.nc2.grib.GribResourceReader.getInputStream(path);
      if (is == null) return;

      BufferedReader br = new BufferedReader(new InputStreamReader(is));

      // rdg - added the 0 line length check to cover the case of blank lines at
      //       the end of the parameter table file.
      while (true) {
        String line = br.readLine();
        if (line == null) break;
        if ((line.length() == 0) || line.startsWith("#")) continue;

        StringBuilder lineb =  new StringBuilder(line);
        StringUtil2.remove(lineb,"'+,/");
        String[] flds = lineb.toString().split("[:]");

        int val = Integer.parseInt(flds[0].trim()); // must have a number
        String name = flds[1].trim() + ": " + flds[2].trim();

        result.put(val, name);
        if (false) System.out.printf(" %d == %s%n", val, name);
      }

      nwsoSubCenter = result; // all at once - thread safe

    } catch (IOException ioError) {
      logger.warn("An error occurred in Grib1Tables while trying to open the table " + path + " : " + ioError);

    } finally {
      if (is != null) try {
        is.close();
      } catch (IOException e) {
      }
    }

  }

  public static void main(String[] args) {
     readNwsoSubCenter("resources/grib1/noaa_rfc/tableC.txt");
  }
}
