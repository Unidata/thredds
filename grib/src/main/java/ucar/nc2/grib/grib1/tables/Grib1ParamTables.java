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

import ucar.nc2.grib.GribResourceReader;
import ucar.nc2.grib.grib1.Grib1Parameter;
import ucar.nc2.grib.grib1.Grib1Record;
import ucar.nc2.grib.grib1.Grib1SectionProductDefinition;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * This is the interface to manage GRIB-1 Parameter Tables (table 2).
 * These are the tables that are loaded at runtime, matching center and versions.
 * <p/>
 * Allow different table versions in the same file.
 * Allow overriding standard grib1 tables on the dataset level.
 *
 * @author caron
 * @since 9/13/11
 */
@Immutable
public class Grib1ParamTables {
  static private final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(Grib1ParamTables.class);

  static private final Object lock = new Object();
  static private int standardTablesStart = 0; // heres where the standard tables start - keep track so user additions can go first

  static private final boolean warn = false;

  static private final Lookup standardLookup;
  static private final Grib1ParamTable defaultTable;

  static {
    try {
      standardLookup = new Lookup();
      standardLookup.readLookupTable("resources/grib1/lookupTables.txt");
      standardLookup.readLookupTable("resources/grib1/ecmwf/lookupTables.txt");
      standardLookup.readLookupTable("resources/grib1/ncl/lookupTables.txt");
      standardLookup.readLookupTable("resources/grib1/dss/lookupTables.txt");
      // standardLookup.readLookupTable("resources/grib1/ncep/lookupTables.txt");
      standardLookup.readLookupTable("resources/grib1/wrf/lookupTables.txt"); // */
      // lookup.readLookupTable("resources/grib1/tablesOld/lookupTables.txt");  // too many problems - must check every one !
      standardLookup.tables = new CopyOnWriteArrayList<Grib1ParamTable>(standardLookup.tables); // in case user adds tables
      defaultTable = standardLookup.getParameterTable(0, -1, -1); // user cannot override default

    } catch (IOException ioe) {
      throw new RuntimeException(ioe);
    }
  }

  private static boolean strict = false;

  public static boolean isStrict() {
    return strict;
  }

  /**
   * Set strict mode.
   * <li>If strict:
   * <ol>Must find a match in the tables. Otherwise, use default</ol>
   * <ol>Tables cannot override standard WMO parameters. Thus param_no < 128 and version < 128 must use default table</ol>
   * </li>
   *
   * @param strict true for strict mode.
   */
  public static void setStrict(boolean strict) {
    Grib1ParamTables.strict = strict;
  }

  public static Grib1ParamTable getDefaultTable() {
    return defaultTable;
  }

  static public int makeKey(int center, int subcenter, int version) {
    if (center < 0) center = 255;
    if (subcenter < 0) subcenter = 255;
    if (version < 0) version = 255;
    return center * 1000 * 1000 + subcenter * 1000 + version;
  }

  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * Get a Grib1ParamTables object, optionally specifying a parameter table or lookup table specific to this dataset.
   *
   * @param paramTablePath  path to a parameter table, in format Grib1ParamTable can read.
   * @param lookupTablePath path to a lookup table, in format Lookup.readLookupTable() can read.
   * @return Grib1Tables
   * @throws IOException on read error
   */
  static public Grib1ParamTables factory(String paramTablePath, String lookupTablePath) throws IOException {
    if (paramTablePath == null && lookupTablePath == null) return new Grib1ParamTables();

    Grib1ParamTables result = new Grib1ParamTables();
    if (paramTablePath != null) {
      result.override = new Grib1ParamTable(paramTablePath);
      if (result.override == null)
        throw new FileNotFoundException("cant read parameter table=" + paramTablePath);
    }
    if (lookupTablePath != null) {
      result.lookup = new Lookup();
      if (!result.lookup.readLookupTable(lookupTablePath))
        throw new FileNotFoundException("cant read lookup table=" + lookupTablePath);
    }

    return result;
  }

  /**
   * Get a Grib1Tables object, optionally specifiying a parameter table in XML specific to this dataset.
   *
   * @param paramTableElem parameter table in XML
   * @return Grib1Tables
   * @throws IOException on read error
   */
  static public Grib1ParamTables factory(org.jdom2.Element paramTableElem) throws IOException {
    if (paramTableElem == null) return new Grib1ParamTables();

    Grib1ParamTables result = new Grib1ParamTables();
    result.override = new Grib1ParamTable(paramTableElem);

    return result;
  }

  ///////////////////////////////////////////////////////////////////////////

  private Lookup lookup; // if lookup table was set
  private Grib1ParamTable override; // if parameter table was set

  public Grib1ParamTables() {
  }

  public Grib1Parameter getParameter(Grib1Record record) {
    Grib1SectionProductDefinition pds = record.getPDSsection();
    return getParameter(pds.getCenter(), pds.getSubCenter(), pds.getTableVersion(), pds.getParameterNumber());
  }

  public Grib1Parameter getParameter(int center, int subcenter, int tableVersion, int param_number) {
    Grib1Parameter param = null;
    if (override != null)
      param = override.getParameter(param_number);
    if (param == null && lookup != null)
      param = lookup.getParameter(center, subcenter, tableVersion, param_number);
    if (param == null)
      param = standardLookup.getParameter(center, subcenter, tableVersion, param_number); // standard tables
    return param;
  }

  //////////////////////////////////////////////////////////////////////////////////////////

  /**
   * Debugging only
   */
  public Grib1ParamTable getParameterTable(int center, int subcenter, int tableVersion) {
    Grib1ParamTable result = null;
    if (lookup != null)
      result = lookup.getParameterTable(center, subcenter, tableVersion);
    if (result == null)
      result = standardLookup.getParameterTable(center, subcenter, tableVersion); // standard tables

    return result;
  }

  // debugging
  public static List<Grib1ParamTable> getStandardParameterTables() {
    return standardLookup.tables;
  }

  /**
   * Add all tables in list to standard tables
   *
   * @param lookupFilename filename containing list of tables
   * @return true if  read ok, false if file not found
   * @throws IOException if file found but read error
   */
  public static boolean addParameterTableLookup(String lookupFilename) throws IOException {
    Lookup lookup = new Lookup();
    if (!lookup.readLookupTable(lookupFilename))
      return false;

    synchronized (lock) {
      standardLookup.tables.addAll(standardTablesStart, lookup.tables);
      standardTablesStart += lookup.tables.size();
    }
    return true;
  }

  /**
   * Add table to standard tables for a specific center, subcenter and version.
   *
   * @param center        center id
   * @param subcenter     subcenter id, or -1 for all
   * @param tableVersion  table verssion, or -1 for all
   * @param tableFilename file to read parameter table from
   */
  public static void addParameterTable(int center, int subcenter, int tableVersion, String tableFilename) {
    Grib1ParamTable table = new Grib1ParamTable(center, subcenter, tableVersion, tableFilename);
    synchronized (lock) {
      standardLookup.tables.add(standardTablesStart, table);
      standardTablesStart++;
    }
  }

  //////////////////////////////////////////////////////////////////////////

  public static class Lookup {
    List<Grib1ParamTable> tables = new ArrayList<Grib1ParamTable>();
    Map<Integer, Grib1ParamTable> tableMap = new ConcurrentHashMap<Integer, Grib1ParamTable>();

    /**
     * read the lookup table from file
     *
     * @param resourceName read from file
     * @return true if successful
     * @throws IOException On badness
     */
    public boolean readLookupTable(String resourceName) throws IOException {
      InputStream inputStream = GribResourceReader.getInputStream(resourceName);
      if (inputStream == null) {
        logger.warn("Could not open table file:" + resourceName);
        return false;
      }
      return readLookupTable(inputStream, resourceName);
    }

    /**
     * read the lookup table from input stream
     *
     * @param is         The input stream
     * @param lookupFile full pathname of lookup file
     * @return true if successful
     * @throws IOException On badness
     */
    private boolean readLookupTable(InputStream is, String lookupFile) throws IOException {
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
        String[] tableDefArr = line.split(":");

        int center = Integer.parseInt(tableDefArr[0].trim());
        int subcenter = Integer.parseInt(tableDefArr[1].trim());
        int version = Integer.parseInt(tableDefArr[2].trim());
        String filename = tableDefArr[3].trim();
        String path;
        if (filename.startsWith("/") || filename.startsWith("\\") || filename.startsWith("file:") || filename.startsWith("http://")) {
          path = filename;
        } else {
          path = GribResourceReader.getFileRoot(lookupFile) + "/" + filename;
        }

        Grib1ParamTable table = new Grib1ParamTable(center, subcenter, version, path);
        tables.add(table);
      }
      is.close();

      return true;
    }

    public Grib1Parameter getParameter(int center, int subcenter, int tableVersion, int param_number) {
      if (strict && param_number < 128 && tableVersion < 128)
        return defaultTable.getParameter(param_number);

      Grib1ParamTable pt = getParameterTable(center, subcenter, tableVersion);
      Grib1Parameter param = null;
      if (pt != null) param = pt.getParameter(param_number);
      if (!strict && param == null) param = defaultTable.getParameter(param_number);
      return param;

    }

    public Grib1ParamTable getParameterTable(int center, int subcenter, int tableVersion) {
      // look in hash table
      int key = makeKey(center, subcenter, tableVersion);
      Grib1ParamTable table = tableMap.get(key);
      if (table != null)
        return table;

      // match from lookup tables(s)
      table = findParameterTableExact(center, subcenter, tableVersion);
      if (table == null)
        table = findParameterTable(center, subcenter, tableVersion);
      if (table == null) {
        if (strict || defaultTable == null) {
          table = findParameterTable(center, subcenter, tableVersion); // debug
          logger.warn("Could not find a table for GRIB file with center: " + center + " subCenter: " + subcenter + " version: " + tableVersion);
          throw new UnsupportedOperationException("Could not find a table for GRIB file with center: " + center + " subCenter: " + subcenter + " version: " + tableVersion);
        }
        return defaultTable;
      }

      tableMap.put(key, table);
      return table;
    }

    private Grib1ParamTable findParameterTableExact(int center, int subcenter, int version) {
      List<Grib1ParamTable> localCopy = tables; // thread safe
      for (Grib1ParamTable table : localCopy) {
        // look for a match
        if (center == table.getCenter_id() && subcenter == table.getSubcenter_id() && version == table.getVersion()) {  // match
          if (table.getParameters() == null) //  see if the parameters for this table have been read in yet.
            continue; // failed - maybe theres another entry table that matches

          // success - initialize other tables with the same path
          for (Grib1ParamTable table2 : localCopy) {
            if (table2.getPath().equals(table.getPath()))
              table2.setParameters(table.getParameters());
          }
          return table;
        }
      }

      return null;
    }

    // wildcard match
    private Grib1ParamTable findParameterTable(int center, int subcenter, int version) {
      List<Grib1ParamTable> localCopy = tables; // thread safe
      for (Grib1ParamTable table : localCopy) {
        // look for a match
        if (center == table.getCenter_id()) {
          if ((table.getSubcenter_id() == -1) || (subcenter == table.getSubcenter_id())) {
            //if ((table.subcenter_id == -1) || (table.subcenter_id == 0) || (subcenter == table.subcenter_id)) {
            if ((table.getVersion() == -1) || version == table.getVersion()) {  // match
              //  see if the parameters for this table have been read in yet.
              if (table.getParameters() == null)
                continue; // failed - maybe theres another entry table that matches

              // success - initialize other tables with the same path
              for (Grib1ParamTable table2 : localCopy) {
                if (table2.getPath().equals(table.getPath()))
                  table2.setParameters(table.getParameters());
              }
              return table;
            }
          }
        }
      }

      return null;
    }

  }  // Lookup

}
