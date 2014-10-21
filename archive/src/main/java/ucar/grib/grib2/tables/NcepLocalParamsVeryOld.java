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

package ucar.nc2.grib.grib2.table;

import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import ucar.nc2.grib.grib2.Grib2Parameter;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/**
 * NCEP local parameter tables - old.
 * grib2StdQuantities.xml appears to capture NCEP docs on
 * http://www.nco.ncep.noaa.gov/pmb/docs/grib2/grib2_table4-2.shtml
 *
 * @author caron
 * @see "http://www.nco.ncep.noaa.gov/pmb/docs/grib2/grib2_table4-2.shtml"
 * @since 4/3/11
 */
public class NcepLocalParamsVeryOld {

  static private enum Version {
    Current;

    String getResourceName() {
      return "/resources/grib2/tablesOld/grib2StdQuantities.xml";
    }
  }

  /*
    <discipline id="Meteorological_products" number="0">
        <category id="Temperature" number="0">
            <quantity id="Temperature" number="0" unit="K"
                description="Temperature" />
   */

  static private Map<String, Grib2Parameter> readParameters(Version version) throws IOException {
      Class c = NcepLocalParamsVeryOld.class;
      try (InputStream ios = c.getResourceAsStream(version.getResourceName())) {
      if (ios == null) {
        System.out.printf("cant open %s%n", version.getResourceName());
        return null;
      }

      org.jdom2.Document doc;
      try {
        SAXBuilder builder = new SAXBuilder();
        doc = builder.build(ios);
      } catch (JDOMException e) {
        throw new IOException(e.getMessage());
      }
      Element root = doc.getRootElement();

      Map<String, Grib2Parameter> map = new HashMap<>();

      List<Element> disciplines = root.getChildren("discipline");
      for (Element elem1 : disciplines) {
        int discipline = Integer.parseInt(elem1.getAttributeValue("number"));
        List<Element> categories = elem1.getChildren("category");
        for (Element elem2 : categories) {
          int category = Integer.parseInt(elem2.getAttributeValue("number"));
          List<Element> quantities = elem2.getChildren("quantity");
          for (Element elem3 : quantities) {
            int number = Integer.parseInt(elem3.getAttributeValue("number"));
            if ((number < 192) && (category < 192)) continue;  // LOOK cant override, may not be correct, see Note 2 section 1: master table = 255
            String name = elem3.getAttributeValue("id");
            String unit = elem3.getAttributeValue("unit");
            String id = makeId(discipline, category, number);
            map.put(id, new Grib2Parameter(discipline, category, number, name, unit, null, null));
          }
        }
      }

      ios.close();
      return map;
    }
  }

  /////////////////////////////////////////////
  private Version version;
  private Map<String, Grib2Parameter> paramMap;
  private Map<String, String> codeMap;

  public NcepLocalParamsVeryOld(Version version, Map<String, Grib2Parameter> paramMap, Map<String, String> codeMap) {
    this.version = version;
    this.paramMap = paramMap;
    this.codeMap = codeMap;
  }

  public String getTableValue(String tableName, int code) {
    return codeMap.get(tableName + "" + code);
  }

  public Grib2Parameter getParameter(int discipline, int category, int number) {
    return paramMap.get(makeId(discipline, category, number));
  }

  private static String makeId(int discipline, int category, int number) {
    return "P"+discipline + "" + category + "." + number;
  }

  private static NcepLocalParamsVeryOld currentTable;

  static Map<String, Grib2Parameter> getParamMap() { return currentTable.paramMap; }

  public static String getTableValueFromCurrent(String tableName, int code) {
    init();
    return currentTable.getTableValue(tableName, code);
  }

  public static Grib2Parameter getParameterFromCurrent(int discipline, int category, int number) {
    init();
    return currentTable.getParameter(discipline, category, number);
  }

  static void init(){
    if (currentTable == null)
      try {
        Map<String, Grib2Parameter> paramMap = readParameters(Version.Current);
        Map<String, String> codeMap = readCodes(Version.Current);
        currentTable = new NcepLocalParamsVeryOld(Version.Current, paramMap, codeMap);
      } catch (IOException e) {
        throw new IllegalStateException("cant open wmo tables");
      }
  }

  // see http://www.nco.ncep.noaa.gov/pmb/docs/grib2/grib2_doc.shtml
  private static Map<String, String> readCodes(Version v) {
    Map<String, String> codeMap = new HashMap<>();
    codeMap.put("3.1.204", "Curvilinear_Orthogonal");

    codeMap.put("4.3.192", "Forecast Confidence Indicator");
    codeMap.put("4.3.193", "Bias Corrected Ensemble Forecast");

    codeMap.put("4.5.200","Entire atmosphere layer");
    codeMap.put("4.5.201","Entire ocean layer");
    codeMap.put("4.5.204","Highest tropospheric freezing level");
    codeMap.put("4.5.206","Grid scale cloud bottom level");
    codeMap.put("4.5.207","Grid scale cloud top level");
    codeMap.put("4.5.209","Boundary layer cloud bottom level");
    codeMap.put("4.5.210","Boundary layer cloud top level");
    codeMap.put("4.5.211","Boundary layer cloud layer");
    codeMap.put("4.5.212","Low cloud bottom level");
    codeMap.put("4.5.213","Low cloud top level");
    codeMap.put("4.5.214", "Low cloud layer");
    codeMap.put("4.5.215", "Cloud ceiling");
    codeMap.put("4.5.220", "Planetary Boundary Layer");
    codeMap.put("4.5.221", "Layer Between Two Hybrid Levels");
    codeMap.put("4.5.222", "Middle cloud bottom level");
    codeMap.put("4.5.223", "Middle cloud top level");
    codeMap.put("4.5.224", "Middle cloud layer");
    codeMap.put("4.5.232", "High cloud bottom level");
    codeMap.put("4.5.233", "High cloud top level");
    codeMap.put("4.5.234", "High cloud layer");
    codeMap.put("4.5.235", "Ocean isotherm level");
    codeMap.put("4.5.236", "Layer between two depths below ocean surface");
    codeMap.put("4.5.237", "Bottom of ocean mixed layer");
    codeMap.put("4.5.238", "Bottom of ocean isothermal layer");
    codeMap.put("4.5.239", "Layer Ocean Surface and 26C Ocean Isothermal Level");
    codeMap.put("4.5.240", "Ocean Mixed Layer");
    codeMap.put("4.5.241", "Ordered Sequence of Data");
    codeMap.put("4.5.242", "Convective cloud bottom level");
    codeMap.put("4.5.243", "Convective cloud top level");
    codeMap.put("4.5.244", "Convective cloud layer");
    codeMap.put("4.5.245", "Lowest level of the wet bulb zero");
    codeMap.put("4.5.246", "Maximum equivalent potential temperature level");
    codeMap.put("4.5.247", "Equilibrium level");
    codeMap.put("4.5.248", "Shallow convective cloud bottom level");
    codeMap.put("4.5.249", "Shallow convective cloud top level");
    codeMap.put("4.5.251", "Deep convective cloud bottom level");
    codeMap.put("4.5.252", "Deep convective cloud top level");
    codeMap.put("4.5.253", "Lowest bottom level of supercooled liquid water layer");
    codeMap.put("4.5.254", "Highest top level of supercooled liquid water layer");

    codeMap.put("4.6.192", "Perturbed Ensemble Member");

    codeMap.put("4.7.192", "Unweighted Mode of All Members");
    codeMap.put("4.7.193", "Percentile value (10%) of All Members");
    codeMap.put("4.7.194", "Percentile value (50%) of All Members");
    codeMap.put("4.7.195", "Percentile value (90%) of All Members");

    codeMap.put("4.10.192", "Climatological Mean Value");
    codeMap.put("4.10.193", "Average of N forecasts");
    codeMap.put("4.10.194", "Average of N uninitialized analyses");
    codeMap.put("4.10.195", "Average of forecast accumulations");
    codeMap.put("4.10.196", "Average of successive forecast accumulations");
    codeMap.put("4.10.197", "Average of forecast averages");
    codeMap.put("4.10.198", "Average of successive forecast averages");
    codeMap.put("4.10.199", "Climatological Average of N analyses, each a year apart");
    codeMap.put("4.10.200", "Climatological Average of N forecasts, each a year apart");
    codeMap.put("4.10.201", "Climatological Root Mean Square difference between N forecasts and their verifying analyses, each a year apart");
    codeMap.put("4.10.202", "Climatological Standard Deviation of N forecasts from the mean of the same N forecasts, for forecasts one year apart");
    codeMap.put("4.10.203", "Climatological Standard Deviation of N analyses from the mean of the same N analyses, for analyses one year apart");
    codeMap.put("4.10.204", "Average of forecast accumulations");
    codeMap.put("4.10.205", "Average of forecast averages");
    codeMap.put("4.10.206", "Average of forecast accumulations");
    codeMap.put("4.10.207", "Average of forecast averages");

    return codeMap;
  }

}
