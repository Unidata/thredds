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

package ucar.grib.grib1;

import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import ucar.grib.GribResourceReader;
import ucar.grib.NotSupportedException;
import ucar.nc2.grib.table.GribTables;
import ucar.nc2.iosp.grid.GridParameter;

import java.io.*;

import java.util.*;
import java.util.regex.Pattern;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A class containing static methods which deliver descriptions and names of
 * parameters, levels and units for byte codes from GRIB records.
 * <p/>
 * Performs operations related to loading parameter tables stored in files.
 * Through a lookup table (see readParameterTableLookup) all of the supported
 * Parameter Tables are known.  An actual table is not loaded until a parameter
 * from that center/subcenter/table is loaded.
 * see <a href="../../../Parameters.txt">Parameters.txt</a>
 * <p/>
 * For now, the lookup table name is hard coded to "resources/grib/tables/tablelookup.lst"
 *
 * @author Capt Richard D. Gonzalez
 *         modified by Robb Kambic
 *         threadsafe 9/25/08 jcaron see http://www.ibm.com/developerworks/java/library/j-hashmap.html
 */

public class GribPDSParamTable {
  static private final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(GribPDSParamTable.class);

  static private final String RESOURCE_PATH = "resources/grib1";
  static private final String TABLE_LIST = "grib1Tables.txt";

  static private final Pattern valid = Pattern.compile("^[a-zA-Z_][a-zA-Z0-9_@:\\.\\-\\+]*$");
  static private final Pattern numberFirst = Pattern.compile("^[0-9]");

  static private List<GribPDSParamTable> paramTables;
  static private Object lock = new Object();

  static private boolean debug = false;
  static private GribPDSParamTable defaultTable;

  /**
   * This is a mapping from (center,subcenter,number)-> Param table for any data that has been loaded
   */
  static private Map<String, GribPDSParamTable> tableMap = new ConcurrentHashMap<String, GribPDSParamTable>();

  static {
    try {
      paramTables = new ArrayList<GribPDSParamTable>();
      String resourceName = RESOURCE_PATH + "/" + TABLE_LIST;
      readLookupTable(resourceName, paramTables);
      defaultTable = getParameterTable(0, -1, -1);

    } catch (IOException ioe) {
      throw new RuntimeException(ioe);
    }
  }

  /**
   * read the lookup table
   *
   * @param filename read from file
   * @param result   put results here
   * @return true if successful
   * @throws IOException On badness
   */
  private static boolean readLookupTable(String filename, List<GribPDSParamTable> result) throws IOException {
    InputStream inputStream = GribResourceReader.getInputStream(filename);
    if (inputStream == null) {
      logger.debug("Could not open table file:" + filename);
      return false;
    }
    return readLookupTable(inputStream, filename, result);
  }

  /**
   * Read the table list contained in the input stream
   *
   * @param is         The input stream
   * @param filename The name of the lookup table file
   * @param result    The list to add the tables into
   * @return true if successful
   * @throws IOException On badness
   */
  private static boolean readLookupTable(InputStream is, String filename, List<GribPDSParamTable> result) throws IOException {
    if (is == null)
      return false;

    InputStreamReader isr = new InputStreamReader(is);
    BufferedReader br = new BufferedReader(isr);

    String line;
    while ((line = br.readLine()) != null) {
      line = line.trim();
      if ((line.length() == 0) || line.startsWith("#")) {
        continue;
      }
      GribPDSParamTable table = new GribPDSParamTable();
      String[] tableDefArr = line.split(":");

      table.center_id = Integer.parseInt(tableDefArr[0].trim());
      table.subcenter_id = Integer.parseInt(tableDefArr[1].trim());
      table.version = Integer.parseInt(tableDefArr[2].trim());
      table.filename = tableDefArr[3].trim();
      if (table.filename.startsWith("/") || table.filename.startsWith("\\")
              || table.filename.startsWith("file:") || table.filename.startsWith("http://")) {
        table.path = table.filename;

      } else if (filename != null) {
        table.path = GribResourceReader.getFileRoot(filename);
        if (table.path.equals(filename)) {
          table.path = table.filename;
        } else {
          table.path = table.path + "/" + table.filename;
        }
        table.path = table.path.replace('\\', '/');
      }
      result.add(table);
    }
    is.close();
    return true;
  }

  public static List<GribPDSParamTable> getParameterTables() {
    return paramTables;
  }

  /**
   * Reads in the list of tables available and stores them.  Does not actually
   * open the parameter tables files, nor store the list of parameters, but
   * just stores the file names of the parameter tables.
   * Parameters for a table are read in when the table is requested (in the
   * getParameterTable method).
   *
   * @param is UserGribTabList as a InputStream
   * @throws IOException or read error
   */
  public static void addParameterUserLookup(InputStream is) throws IOException {
    List<GribPDSParamTable> tables = new ArrayList<GribPDSParamTable>();
    if (!readLookupTable(is, null, tables)) {
      return;
    }

    synchronized (lock) {
      tables.addAll(paramTables);
      paramTables = tables;  // switcheroo
    }
  }

  /**
   * Reads in the list of tables available and stores them.  Does not actually
   * open the parameter tables files, nor store the list of parameters, but
   * just stores the file names of the parameter tables.
   * Parameters for a table are read in when the table is requested (in the
   * getParameterTable method).
   *
   * @param userGribTabList string of userlookup file
   * @return true if  read ok, false if file not found
   * @throws IOException if file found but read error
   */
  public static boolean addParameterUserLookup(String userGribTabList) throws IOException {
    List<GribPDSParamTable> tables = new ArrayList<GribPDSParamTable>();
    if (!readLookupTable(userGribTabList, tables)) {
      return false;
    }

    synchronized (lock) {
      tables.addAll(paramTables);
      paramTables = tables;  // switcheroo
    }

    return true;
  }

  /**
   * Looks for the parameter table which matches the center, subcenter
   * and table version from the tables array.
   * If this is the first time asking for this table, then the parameters for
   * this table have not been read in yet, so this is done as well.
   *
   * @param center       - integer from PDS octet 5, representing Center.
   * @param subcenter    - integer from PDS octet 26, representing Subcenter
   * @param tableVersion - integer from PDS octet 4, representing Parameter Table Version
   * @return GribPDSParamTable matching center, subcenter, and number
   * @throws NotSupportedException no table found
   */
  public static GribPDSParamTable getParameterTable(int center, int subcenter, int tableVersion) throws NotSupportedException {

    String key = center + "_" + subcenter + "_" + tableVersion;
    GribPDSParamTable table = tableMap.get(key);
    if (table != null)
      return table;

    table = readParameterTable(center, subcenter, tableVersion);

    if (table == null) {
      throw new NotSupportedException("Could not find a table entry for GRIB file with center: "
              + center + " subCenter: " + subcenter + " number: " + tableVersion);
    }

    tableMap.put(key, table);
    return table;
  }

  /////////////////////////////////////////////////////////////////////////////////////////////


  /**
   * Looks for the parameter table which matches the center, subcenter
   * and table version from the tables array.
   * If this is the first time asking for this table, then the parameters for
   * this table have not been read in yet, so this is done as well.
   *
   * @param center    - integer from PDS octet 5, representing Center.
   * @param subcenter - integer from PDS octet 26, representing Subcenter
   * @param version    - integer from PDS octet 4, representing Parameter Table Version
   * @return GribPDSParamTable matching center, subcenter, and number
   */

  private static GribPDSParamTable readParameterTable(int center, int subcenter, int version) {

    List<GribPDSParamTable> localCopy = paramTables; // thread safe

    // look for a match
    for (GribPDSParamTable table : localCopy) {

      if (center == table.center_id) {
        if ((table.subcenter_id == -1) || (subcenter == table.subcenter_id)) {
          if ((table.version == -1) || version == table.version) {  // match
            //  see if the parameters for this table have been read in yet.
            if (table.parameters == null) {
              table.readParameterTable();
              if (table.parameters == null) // failed - maybe theres another entry table in paramTables
                continue;

              // success - initialize other tables parameters with the same name
              for (GribPDSParamTable table2 : localCopy) {
                if (table2.path.equals(table.path))
                  table2.parameters = table.parameters;
              }
            }
            return table;
          }
        }
      }
    }

    return null;
  }

  /**
   * Munge a description to make it suitable as variable name
   *
   * @param description start with this
   * @return Valid Description
   */
  static private String makeValidDesc(String description) {
    description = description.replaceAll("\\s+", "_");
    if (valid.matcher(description).find())
      return description;
    // else check for special characters
    if (numberFirst.matcher(description).find())
      description = "N" + description;
    return description.replaceAll("\\)|\\(|=|,|;|\\[|\\]", "");
  }

  //////////////////////////////////////////////////////////////////////////

  /**
   * Identification of center e.g. 88 for Oslo
   */
  private int center_id;

  /**
   * Identification of center defined sub-center - not fully implemented yet.
   */
  private int subcenter_id;

  /**
   * Identification of parameter table version number.
   */
  private int version;

  /**
   * Stores the name of the file containing this table
   */
  private String filename;

  /**
   * path of filename containing this table.
   * Opened if required for lookup.
   */
  private String path;

  /**
   * Map ids to GridParameter objects
   */
  private Map<Integer, GridParameter> parameters;

  public GribPDSParamTable(String filename) throws IOException {
    this.filename = filename;
    this.path = filename;
    this.parameters = readParameterTable();
  }

  private GribPDSParamTable() {
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

  public String getPath() {
    return path;
  }

  public String getFilename() {
    return filename;
  }

  // debugging
  public Map<Integer, GridParameter> getParameters() {
    if (parameters == null)
      readParameterTable();
    return parameters;
  }

    /**
   * Get the parameter with id <tt>id</tt>.
   *
   * @param id the parameter id
   * @return the GridParameter
   */
  public GridParameter getParameter(int id) {
    GridParameter p = parameters.get(id);
    if (p != null) return p;

    // get out of the wmo table if possible
    p = defaultTable.parameters.get(id);
    if (p != null) return p;

    // warning
    logger.warn("GribPDSParamTable: Could not find parameter " + id + " for center:" + center_id
            + " subcenter:" + subcenter_id + " number:" + version + " table " + filename);
    String unknown = "UnknownParameter_" + Integer.toString(id) + "_table_" + filename;
    return new GridParameter(id, unknown, unknown, "Unknown", null);
  }

  @Override
  public String toString() {
    return "GribPDSParamTable{" +
            "center_id=" + center_id +
            ", subcenter_id=" + subcenter_id +
            ", version=" + version +
            ", filename='" + filename + '\'' +
            ", path='" + path + '\'' +
            '}';
  }

  //////////////////////////////////////////////////////////////////////////////////////
  // reading

  private Map<Integer, GridParameter> readParameterTable() {
    if (filename.endsWith(".tab"))
      readParameterTableTab();
    else if (filename.endsWith(".wrf"))
      readParameterTableSplit("\\|", new int[]{0, 3, 1, 2});
    else if (filename.endsWith(".dss"))
      readParameterTableSplit("\t", new int[]{0, -1, 1, 2});
    else if (filename.endsWith(".xml"))
      readParameterTableXml();
    else if (filename.endsWith(".htm"))
      readParameterTableNcepScrape();

    return parameters;
  }


  /*
    <tr>
      <td>
      <center>243</center>
      </td>
      <td>Deep convective moistening rate</td>
      <td>kg/kg/s</td>
      <td>CNVMR</td>
    </tr>
   */
  private boolean readParameterTableNcepScrape() {
    InputStream is = null;
    try {
      is = GribResourceReader.getInputStream(path);
      if (is == null) return false;

      SAXBuilder builder = new SAXBuilder();
      org.jdom.Document doc = builder.build(is);
      Element root = doc.getRootElement();

      HashMap<Integer, GridParameter> result = new HashMap<Integer, GridParameter>();

      List<Element> params = root.getChildren("tr");
      for (Element elem1 : params) {
        List<Element> elems = elem1.getChildren("td");
        Element e1 = elems.get(0);
        String codeS = e1.getChildText("center");
        int code = Integer.parseInt(codeS);
        String desc = elems.get(1).getText();
        String units = elems.get(2).getText();
        String name = elems.get(3).getText();

        GridParameter parameter = new GridParameter(code, name, desc, units);
        result.put(parameter.getNumber(), parameter);
        if (debug) System.out.printf(" %s%n", parameter);
      }
      parameters = result;
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

  /* http://dss.ucar.edu/metadata/ParameterTables/WMO_GRIB1.60-1.3.xml
   <parameter code="5">
  <description>ICAO Standard Atmosphere reference height</description>
  <units>m</units>
  </parameter>
  */
  private boolean readParameterTableXml() {
    InputStream is = null;
    try {
      is = GribResourceReader.getInputStream(path);
      if (is == null) return false;

      SAXBuilder builder = new SAXBuilder();
      org.jdom.Document doc = builder.build(is);
      Element root = doc.getRootElement();

      HashMap<Integer, GridParameter> result = new HashMap<Integer, GridParameter>();

      List<Element> params = root.getChildren("parameter");
      for (Element elem1 : params) {
        int code = Integer.parseInt(elem1.getAttributeValue("code"));
        String desc = elem1.getChildText("description");
        if (desc == null) continue;
        String units = elem1.getChildText("units");
        if (units == null) units = "";
        String name = elem1.getChildText("shortName");
        String cf = elem1.getChildText("CF");
        GridParameter parameter = new GridParameter(code, name, desc, units, cf);
        result.put(parameter.getNumber(), parameter);
        if (debug) System.out.printf(" %s%n", parameter);
      }
      parameters = result;
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

  // order: num, name, desc, unit
  private boolean readParameterTableSplit(String regexp, int[] order) {
    HashMap<Integer, GridParameter> result = new HashMap<Integer, GridParameter>();

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

        GridParameter parameter = new GridParameter();
        parameter.setNumber(Integer.parseInt(flds[order[0]].trim())); // must have a number
        if (order[1] >= 0) parameter.setName(flds[order[1]].trim());
        parameter.setDescription(GribTables.cleanupDescription(flds[order[2]].trim()));
        if (flds.length > order[3]) parameter.setUnit(flds[order[3]].trim());
        result.put(parameter.getNumber(), parameter);
        if (debug) System.out.printf(" %s%n", parameter);
      }

      parameters = result;
      return true;

    } catch (IOException ioError) {
      logger.warn("An error occurred in GribPDSParamTable while trying to open the parameter table "
              + filename + " : " + ioError);
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
      logger.error("GribPDSParamTable: unknown path for " + this);
      return false;
    }

    InputStream is = null;
    try {
      is = GribResourceReader.getInputStream(path);
      if (is == null) {
        logger.error("GribPDSParamTable: error getInputStream on " + this);
        return false;
      }
      BufferedReader br = new BufferedReader(new InputStreamReader(is));
      br.readLine(); // skip a line

      HashMap<Integer, GridParameter> tmpParameters = new HashMap<Integer, GridParameter>(); // thread safe - temp hash
      while (true) {
        String line = br.readLine();
        if (line == null) break;
        if ((line.length() == 0) || line.startsWith("#")) continue;
        String[] tableDefArr = line.split(":");

        GridParameter parameter = new GridParameter();
        parameter.setNumber(Integer.parseInt(tableDefArr[0].trim()));
        parameter.setName(tableDefArr[1].trim());
        // check to see if unit defined, if not, parameter is undefined
        if (tableDefArr[2].indexOf('[') == -1) {
          // Undefined unit
          parameter.setDescription(tableDefArr[2].trim());
          parameter.setUnit(tableDefArr[2].trim());
        } else {
          String[] arr2 = tableDefArr[2].split("\\[");
          parameter.setDescription(arr2[0].trim());
          parameter.setUnit(arr2[1].substring(0, arr2[1].lastIndexOf(']')).trim());
        }
        tmpParameters.put(parameter.getNumber(), parameter);
        if (debug)
          System.out.println(parameter.getNumber() + " " + parameter.getDescription() + " " + parameter.getUnit());
      }

      this.parameters = tmpParameters; // thread safe
      return true;

    } catch (IOException ioError) {
      logger.warn("An error occurred in GribPDSParamTable while trying to open the parameter table " + filename + " : " + ioError);
      return false;

    } finally {
      if (is != null) try {
        is.close();
      } catch (IOException e) {
      }
    }

  }

  static public void main(String[] args) throws IOException {
    debug = true;
    addParameterUserLookup("C:/dev/tds4.2/thredds/grib/resources/resources/grib/tables/zagreb_221_1.tab");
  }
}

