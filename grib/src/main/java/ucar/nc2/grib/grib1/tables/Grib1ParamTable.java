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

import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.Namespace;
import org.jdom2.input.SAXBuilder;
import ucar.nc2.grib.GribResourceReader;
import ucar.nc2.grib.grib1.*;
import ucar.nc2.ncml.NcMLReader;
import ucar.unidata.util.StringUtil2;

import java.io.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A Grib1 Parameter Table (table 2).
 * This is a map: code -> Grib1Parameter.
 * Handles reading the table in from various formats
 *
 * @author caron
 * @since 11/16/11
 */
public class Grib1ParamTable {
  static private final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(Grib1ParamTable.class);
  static private final boolean debug = false;

  private int center_id;
  private int subcenter_id;
  private int version;
  private String name;  // name of the table
  private String path; // path of filename containing this table
  private Map<Integer, Grib1Parameter> parameters; // param number -> param
  private boolean useName;

  /**
   * Read a dataset-specific table from a file
   *
   * @param path read from this path, may be reletive
   * @throws IOException on io error
   */
  public Grib1ParamTable(String path) throws IOException {
    this.path = StringUtil2.replace(path, "\\", "/");
    this.name = GribResourceReader.getFilename(path);
    this.parameters = readParameterTable();
  }

  /**
   * Create a standard table from a file.
   * Defer reading until getParameters is called.
   *
   * @param center_id    associate with this center
   * @param subcenter_id associate with this subcenter
   * @param version      associate with this version
   * @param path         read from this path, may be reletive
   */
  public Grib1ParamTable(int center_id, int subcenter_id, int version, String path) {
    this.center_id = center_id;
    this.subcenter_id = subcenter_id;
    this.version = version;
    this.path = path;
    this.name = GribResourceReader.getFilename(path);
  }

  /**
   * Create a dataset-specific table from a jdom tree using the DSS parser
   *
   * @param paramTableElem the jdom tree
   * @throws IOException on io error
   */
  public Grib1ParamTable(org.jdom2.Element paramTableElem) throws IOException {
    this.name = paramTableElem.getChildText("title");
    DssParser p = new DssParser(NcMLReader.ncNS);
    this.parameters = p.parseXml(paramTableElem);
  }

  public int getCenter_id() {
    return center_id;
  }

  public int getSubcenter_id() {
    return subcenter_id;
  }

  public int getVersion() {
    return version;
  }

  public String getName() {
    return name;
  }

  public boolean useParamName() {
    return useName;
  }

  public int getKey() {
    return Grib1ParamTables.makeKey(center_id, subcenter_id, version);
  }

  public String getPath() {
    return path;
  }

  public Map<Integer, Grib1Parameter> getParameters() {
    if (parameters == null)
      readParameterTable();
    return parameters;
  }

  public void setParameters(Map<Integer, Grib1Parameter> params) {
    parameters = params;
  }

  /**
   * Get the parameter with id. If not found, look in default table.
   *
   * @param id the parameter number
   * @return the Grib1Parameter, or null if not found
   */
  public Grib1Parameter getParameter(int id) {
    if (parameters == null)
      readParameterTable();

    Grib1Parameter p = parameters.get(id);
    return p;
  }

  /**
   * Get the parameter with id, but dont look in default table.
   *
   * @param id the parameter number
   * @return the Grib1Parameter, or null if not found
   */
  public Grib1Parameter getLocalParameter(int id) {
    if (parameters == null)
      readParameterTable();
    return parameters.get(id);
  }

  @Override
  public String toString() {
    return "Grib1ParamTable{" +
            "center_id=" + center_id +
            ", subcenter_id=" + subcenter_id +
            ", version=" + version +
            ", name='" + name + '\'' +
            ", path='" + path + '\'' +
            '}';
  }

  //////////////////////////////////////////////////////////////////////////////////////
  // reading

  private Map<Integer, Grib1Parameter> readParameterTable() {
    if (name.startsWith("table_2_") || name.startsWith("local_table_2_"))
      readParameterTableEcmwf(); // ecmwf
    else if (name.startsWith("US058"))
      readParameterTableXml(new FnmocParser());// FNMOC
     else if (name.endsWith(".tab"))
      readParameterTableTab();                               // wgrib format
    else if (name.endsWith(".wrf"))
      readParameterTableSplit("\\|", new int[]{0, 3, 1, 2}); // WRF AMPS
    else if (name.endsWith(".h"))
      readParameterTableNcl(); // NCL
    else if (name.endsWith(".dss"))
      readParameterTableSplit("\t", new int[]{0, -1, 1, 2}); // NCAR DSS
    else if (name.endsWith(".xml"))
      readParameterTableXml(new DssParser(Namespace.NO_NAMESPACE));// NCAR DSS XML format
    else
      logger.warn("Dont know how to read " + name + " file=" + path);
    return parameters;
  }

/*
 * Brazilian Space Agency - INPE/CPTEC
 * Center: 46
 * Subcenter: 0
 * Parameter table version: 254
 *

TBLE2 cptec_254_params[] = {
{1, "Pressure", "hPa", "PRES"},
{2, "Pressure reduced to MSL", "hPa", "PSNM"},
{3, "Pressure tendency", "Pa/s", "TSPS"},
{6, "Geopotential", "dam", "GEOP"},
{7, "Geopotential height", "gpm", "ZGEO"},
{8, "Geometric height", "m", "GZGE"},
{11, "ABSOLUTE TEMPERATURE", "K", "TEMP"},

   */

  static private final Pattern nclPattern = Pattern.compile("\\{(\\d*)\\,\\s*\"([^\"]*)\"\\,\\s*\"([^\"]*)\"\\,\\s*\"([^\"]*)\".*");

  private boolean readParameterTableNcl() {
    HashMap<Integer, Grib1Parameter> result = new HashMap<Integer, Grib1Parameter>();

    InputStream is = null;
    try {
      is = GribResourceReader.getInputStream(path);
      if (is == null) return false;

      BufferedReader br = new BufferedReader(new InputStreamReader(is));

      // Ignore header
      int count = 0;
      while (true) {
        String line = br.readLine();
        if (line == null) break; // done with the file
        if (line.startsWith("TBLE2")) break;

        if (line.contains("Center:"))
          center_id = extract(line, "Center:");
        else if (line.contains("Subcenter:"))
          subcenter_id = extract(line, "Subcenter:");
        else if (line.contains("version:"))
          version = extract(line, "version:");
        count++;
      }

      while (true) {
        String line = br.readLine();
        if (line == null) break; // done with the file
        if ((line.length() == 0) || line.startsWith("#")) continue;

        Matcher m = nclPattern.matcher(line);
        if (!m.matches()) continue;

        int p1;
        try {
          p1 = Integer.parseInt(m.group(1));
        } catch (Exception e) {
          logger.warn("Cant parse " + m.group(1) + " in file " + path);
          continue;
        }
        Grib1Parameter parameter = new Grib1Parameter(this, p1, m.group(4), m.group(2), m.group(3));
        result.put(parameter.getNumber(), parameter);
        if (debug) System.out.printf(" %s%n", parameter);
      }

      parameters = result; // all at once - thread safe
      return true;

    } catch (IOException ioError) {
      logger.warn("An error occurred in Grib1ParamTable while trying to open the parameter table "
              + path + " : " + ioError);
      return false;

    } finally {
      if (is != null) try {
        is.close();
      } catch (IOException e) {
      }
    }

  }

  private int extract(String line, String key) {
    int pos = line.indexOf(key);
    if (pos < 0) return -1;
    String want = line.substring(pos + key.length());
    try {
      return Integer.parseInt(want.trim());
    } catch (NumberFormatException e) {
      System.out.printf("BAD %s (%s)%n", line, path);
      return -1;
    }
  }

  /*
  WMO standard table 2: Version Number 3.
  Codes and data units for FM 92-X Ext.GRIB.
  ......................
  001
  P
  Pressure
  Pa
  Pa
  ......................
  002
  MSL
  Mean sea level pressure
  Pa
  Pa
  ......................
  003
  None
  Pressure tendency
  Pa s**-1
  Pa s**-1
  ......................
  004
  PV
  Potential vorticity
  K m**2 kg**-1 s**-1
  K m**2 kg**-1 s**-1
  ......................
  005
  None
  ICAO Standard Atmosphere reference height
  m
  m
   */

  private boolean readParameterTableEcmwf() {
    HashMap<Integer, Grib1Parameter> result = new HashMap<Integer, Grib1Parameter>();

    InputStream is = null;
    try {
      is = GribResourceReader.getInputStream(path);
      if (is == null) {
        logger.error("Cant open "+path);
        return false;
      }

      BufferedReader br = new BufferedReader(new InputStreamReader(is));
      String line = br.readLine();
      if (!line.startsWith("...")) name = line; // maybe ??
      while (!line.startsWith("..."))
        line = br.readLine(); // skip

      while (true) {
        line = br.readLine();
        if (line == null) break; // done with the file
        if ((line.length() == 0) || line.startsWith("#")) continue;
        if (line.startsWith("...")) { // ...  may have already been read
          line = br.readLine();
          if (line == null) break;
        }
        String num = line.trim();
        line = br.readLine();
        String name = (line != null) ? line.trim() : null;
        line = br.readLine();
        String desc = (line != null) ? line.trim() : null;
        line = br.readLine();
        String units1 = (line != null) ? line.trim() : null;

        // optional notes
        line = br.readLine();
        String notes = (line == null || line.startsWith("...")) ? null : line.trim();

        if (desc.equalsIgnoreCase("undefined")) continue; // skip

        int p1;
        try {
          p1 = Integer.parseInt(num);
        } catch (Exception e) {
          logger.warn("Cant parse " + num + " in file " + path);
          continue;
        }
        Grib1Parameter parameter = new Grib1Parameter(this, p1, name, desc, units1);
        result.put(parameter.getNumber(), parameter);
        if (debug) System.out.printf(" %s%n", parameter);
      }

      parameters = result; // all at once - thread safe
      return true;

    } catch (IOException ioError) {
      logger.warn("An error occurred in Grib1ParamTable while trying to open the parameter table "
              + path + " : " + ioError);
      return false;

    } finally {
      if (is != null) try {
        is.close();
      } catch (IOException e) {
      }
    }

  }

  private boolean readParameterTableXml(XmlTableParser parser) {
    InputStream is = null;
    try {
      is = GribResourceReader.getInputStream(path);
      if (is == null) return false;

      SAXBuilder builder = new SAXBuilder();
      org.jdom2.Document doc = builder.build(is);
      Element root = doc.getRootElement();
      parameters = parser.parseXml(root);  // all at once - thread safe
      return true;

    } catch (IOException ioe) {
      ioe.printStackTrace();
      return false;

    } catch (JDOMException e) {
      e.printStackTrace();
      return false;

    } finally {
      if (is != null) try {
        is.close();
      } catch (IOException e) {
      }
    }
  }

  private interface XmlTableParser {
    HashMap<Integer, Grib1Parameter> parseXml(Element root);
  }

  private class DssParser implements XmlTableParser {

    private Namespace ns;
    DssParser(Namespace ns) {
      this.ns = ns;
    }

   /* http://dss.ucar.edu/metadata/ParameterTables/WMO_GRIB1.60-1.3.xml
    <parameter code="5">
    <description>ICAO Standard Atmosphere reference height</description>
    <units>m</units>
    </parameter>
    */
   public HashMap<Integer, Grib1Parameter> parseXml(Element root) {
      HashMap<Integer, Grib1Parameter> result = new HashMap<Integer, Grib1Parameter>();
      List<Element> params = root.getChildren("parameter", ns);
      for (Element elem1 : params) {
        int code = Integer.parseInt(elem1.getAttributeValue("code"));
        String desc = elem1.getChildText("description", ns);
        if (desc == null) continue;
        String units = elem1.getChildText("units", ns);
        if (units == null) units = "";
        String name = elem1.getChildText("name", ns);
        if (name == null) name = elem1.getChildText("shortName", ns);
        String cf = elem1.getChildText("CF", ns);
        Grib1Parameter parameter = new Grib1Parameter(Grib1ParamTable.this, code, name, desc, units, cf);
        result.put(parameter.getNumber(), parameter);
        if (debug) System.out.printf(" %s%n", parameter);
      }
      return result;
    }
  }

  private class FnmocParser implements XmlTableParser {
    /*
    <pnTable>
      <name>Master Parameter Table</name>
      <updatedDTG>201110110856</updatedDTG>
      <tableStatus setDTG="2007050712">current</tableStatus>
      <fnmocTable>
        <entry>
          <grib1Id>001</grib1Id>
          <fnmocId>pres</fnmocId>
          <name>pres</name>
          <nameFull/>
          <description>Commonly used for atmospheric pressure, the pressure exerted by the atmosphere as a consequence of
            gravitational attraction exerted upon the "column" of air lying directly above the point in question.
          </description>
          <unitsFNMOC>pa</unitsFNMOC>
          <productionStatus>current</productionStatus>
        </entry>
    */

    public HashMap<Integer, Grib1Parameter> parseXml(Element root) {
      HashMap<Integer, Grib1Parameter> result = new HashMap<Integer, Grib1Parameter>();
      Element fnmocTable = root.getChild("fnmocTable");
      List<Element> params = fnmocTable.getChildren("entry");
      for (Element elem1 : params) {
        int code;
        try {
          code = Integer.parseInt(elem1.getChildText("grib1Id"));
        } catch (NumberFormatException e) {
          System.out.printf("BAD number= %s%n", elem1.getChildText("grib1Id"));
          continue;
        }
        String desc = elem1.getChildText("description");
        if (desc == null) continue;
        //if (desc.startsWith("no definition")) continue; // skip; use standard def
        desc = StringUtil2.collapseWhitespace(desc);
        String units = elem1.getChildText("unitsFNMOC");
        if (units == null) units = "";
        String name = elem1.getChildText("name");
        Grib1Parameter parameter = new Grib1Parameter(Grib1ParamTable.this, code, name, desc, units, null);
        result.put(parameter.getNumber(), parameter);
        if (debug) System.out.printf(" %s%n", parameter);
      }
      useName = true;
      return result;
    }
  }

  // order: num, name, desc, unit
  private boolean readParameterTableSplit(String regexp, int[] order) {
    HashMap<Integer, Grib1Parameter> result = new HashMap<Integer, Grib1Parameter>();

    InputStream is = null;
    try {
      is = GribResourceReader.getInputStream(path);
      if (is == null) return false;

      BufferedReader br = new BufferedReader(new InputStreamReader(is));

      // rdg - added the 0 line length check to cover the case of blank lines at
      //       the end of the parameter table file.
      while (true) {
        String line = br.readLine();
        if (line == null) break;
        if ((line.length() == 0) || line.startsWith("#")) continue;
        String[] flds = line.split(regexp);

        int p1 = Integer.parseInt(flds[order[0]].trim()); // must have a number
        String name = (order[1] >= 0) ? flds[order[1]].trim() : null;
        String desc = flds[order[2]].trim();
        String units = (flds.length > order[3]) ? flds[order[3]].trim() : "";

        Grib1Parameter parameter = new Grib1Parameter(this, p1, name, desc, units);
        result.put(parameter.getNumber(), parameter);
        if (debug) System.out.printf(" %s%n", parameter);
      }

      parameters = result; // all at once - thread safe
      return true;

    } catch (IOException ioError) {
      logger.warn("An error occurred in Grib1ParamTable while trying to open the parameter table "
              + path + " : " + ioError);
      return false;

    } finally {
      if (is != null) try {
        is.close();
      } catch (IOException e) {
      }
    }

  }

  private boolean readParameterTableTab() {
    if (path == null) {
      logger.error("Grib1ParamTable: unknown path for " + this);
      return false;
    }

    InputStream is = null;
    try {
      is = GribResourceReader.getInputStream(path);
      if (is == null) {
        logger.error("Grib1ParamTable: error getInputStream on " + this);
        return false;
      }
      BufferedReader br = new BufferedReader(new InputStreamReader(is));
      br.readLine(); // skip a line

      HashMap<Integer, Grib1Parameter> params = new HashMap<Integer, Grib1Parameter>(); // thread safe - local var
      while (true) {
        String line = br.readLine();
        if (line == null) break;
        if ((line.length() == 0) || line.startsWith("#")) continue;
        String[] tableDefArr = line.split(":");

        int p1 = Integer.parseInt(tableDefArr[0].trim());
        String name = tableDefArr[1].trim();
        String desc, units;
        // check to see if unit defined, if not, parameter is undefined
        if (tableDefArr[2].indexOf('[') == -1) {
          // Undefined unit
          desc = tableDefArr[2].trim();
          units = "";
        } else {
          String[] arr2 = tableDefArr[2].split("\\[");
          desc = arr2[0].trim();
          units = arr2[1].substring(0, arr2[1].lastIndexOf(']')).trim();
        }

        Grib1Parameter parameter = new Grib1Parameter(this, p1, name, desc, units);
        if (!parameter.getDescription().equalsIgnoreCase("undefined"))
          params.put(parameter.getNumber(), parameter);
        if (debug)
          System.out.println(parameter.getNumber() + " " + parameter.getDescription() + " " + parameter.getUnit());
      }

      this.parameters = params; // thread safe
      return true;

    } catch (IOException ioError) {
      logger.warn("An error occurred in Grib1ParamTable while trying to open the parameter table " + path + " : " + ioError);
      return false;

    } finally {
      if (is != null) try {
        is.close();
      } catch (IOException e) {
      }
    }

  }

  static public void main(String[] args) throws IOException {
    String dirS = "C:\\dev\\github\\thredds\\grib\\src\\main\\resources\\resources\\grib1\\ncl";
    File dir = new File(dirS);
    for (File f : dir.listFiles()) {
      if (!f.getName().endsWith(".h")) continue;
      Grib1ParamTable table = new Grib1ParamTable(f.getPath());

      //  60:	 1:		180:	WMO_GRIB1.60-1.180.xml
      System.out.printf("%5d: %5d: %5d: %s%n", table.getCenter_id(), table.getSubcenter_id(), table.getVersion(), table.getName());
    }
  }
}
