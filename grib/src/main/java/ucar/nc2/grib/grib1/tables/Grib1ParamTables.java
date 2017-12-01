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

import net.jcip.annotations.Immutable;

import ucar.nc2.constants.CDM;
import ucar.nc2.grib.GribResourceReader;
import ucar.nc2.grib.grib1.Grib1Parameter;
import ucar.nc2.grib.grib1.Grib1Record;
import ucar.nc2.grib.grib1.Grib1SectionProductDefinition;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
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

  static private Lookup standardLookup;
  static private Grib1ParamTableReader defaultTable;

  static {
    try {
      standardLookup = new Lookup();
      standardLookup.readLookupTable("resources/grib1/lookupTables.txt");
      standardLookup.readLookupTable("resources/grib1/ecmwfEcCodes/lookupTables.txt");
      standardLookup.readLookupTable("resources/grib1/ecmwf/lookupTables.txt");
      standardLookup.readLookupTable("resources/grib1/ncl/lookupTables.txt");
      standardLookup.readLookupTable("resources/grib1/dss/lookupTables.txt");
      // standardLookup.readLookupTable("resources/grib1/ncep/lookupTables.txt");
      standardLookup.readLookupTable("resources/grib1/wrf/lookupTables.txt"); // */
      // lookup.readLookupTable("resources/grib1/tablesOld/lookupTables.txt");  // too many problems - must check every one !
      standardLookup.tables = new CopyOnWriteArrayList<>(standardLookup.tables); // in case user adds tables
      defaultTable = standardLookup.getParameterTable(0, -1, -1); // user cannot override default

    } catch (Throwable t) {
      logger.warn("Grib1ParamTables init failed: ", t);
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

  public static Grib1ParamTableReader getDefaultTable() {
    return defaultTable;
  }

  static public int makeKey(int center, int subcenter, int version) {
    if (center < 0) center = 255;
    if (subcenter < 0) subcenter = 255;
    if (version < 0) version = 255;
    return center * 1000 * 1000 + subcenter * 1000 + version;
  }

  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  private static final Map<String, Grib1ParamTableReader> localTableHash = new ConcurrentHashMap<>();

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
    Lookup lookup = null;
    Grib1ParamTableReader override = null;

    Grib1ParamTableReader table;
    if (paramTablePath != null) {
      table = localTableHash.get(paramTablePath);
      if (table == null) {
        table = new Grib1ParamTableReader(paramTablePath);
        localTableHash.put(paramTablePath, table);
        override = table;
      }
    }

    if (lookupTablePath != null) {
      lookup = new Lookup();
      if (!lookup.readLookupTable(lookupTablePath))
        throw new FileNotFoundException("cant read lookup table=" + lookupTablePath);
    }

    return new Grib1ParamTables(lookup, override);
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
    return new Grib1ParamTables(null, new Grib1ParamTableReader(paramTableElem));
  }

  ///////////////////////////////////////////////////////////////////////////

  private final Lookup lookup; // if lookup table was set
  private final Grib1ParamTableReader override; // if parameter table was set

  public Grib1ParamTables() {
    this.lookup = null;
    this.override = null;
  }

  public Grib1ParamTables(Lookup lookup, Grib1ParamTableReader override) {
    this.lookup = lookup;
    this.override = override;
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
  public Grib1ParamTableReader getParameterTable(int center, int subcenter, int tableVersion) {
    Grib1ParamTableReader result = null;
    if (lookup != null)
      result = lookup.getParameterTable(center, subcenter, tableVersion);
    if (result == null)
      result = standardLookup.getParameterTable(center, subcenter, tableVersion); // standard tables

    return result;
  }

  // debugging
  public static List<Grib1ParamTableReader> getStandardParameterTables() {
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
    Grib1ParamTableReader table = new Grib1ParamTableReader(center, subcenter, tableVersion, tableFilename);
    synchronized (lock) {
      standardLookup.tables.add(standardTablesStart, table);
      standardTablesStart++;
    }
  }

  //////////////////////////////////////////////////////////////////////////

  public static class Lookup {
    List<Grib1ParamTableReader> tables = new ArrayList<>();
    Map<Integer, Grib1ParamTableReader> tableMap = new ConcurrentHashMap<>();

    /**
     * read the lookup table from file
     *
     * @param resourceName read from file
     * @return true if successful
     * @throws IOException On badness
     */
    public boolean readLookupTable(String resourceName) throws IOException {
      try (InputStream inputStream = GribResourceReader.getInputStream(resourceName)) {
        if (inputStream == null) {
          logger.warn("Could not open table file:" + resourceName);
          return false;
        }
        return readLookupTable(inputStream, resourceName);
      }
    }

    /**
     * read the lookup table from an input stream
     *
     * @param is         The input stream
     * @param lookupFile full pathname of lookup file
     * @return true if successful
     * @throws IOException On badness
     */
    private boolean readLookupTable(InputStream is, String lookupFile) throws IOException {
      if (is == null)
        return false;

      File parent = new File(lookupFile).getParentFile();
      InputStreamReader isr = new InputStreamReader(is, CDM.utf8Charset);
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
          File tableFile = new File(parent, filename); // reletive file
          path = tableFile.getPath();
        }

        Grib1ParamTableReader table = new Grib1ParamTableReader(center, subcenter, version, path);
        tables.add(table);
      }

      return true;
    }

    public Grib1Parameter getParameter(int center, int subcenter, int tableVersion, int param_number) {
      if (strict && param_number < 128 && tableVersion < 128)
        return defaultTable.getParameter(param_number);

      Grib1ParamTableReader pt = getParameterTable(center, subcenter, tableVersion);
      Grib1Parameter param = null;
      if (pt != null) param = pt.getParameter(param_number);
      if (!strict && param == null) param = defaultTable.getParameter(param_number);
      return param;

    }

    public Grib1ParamTableReader getParameterTable(int center, int subcenter, int tableVersion) {
      // look in hash table
      int key = makeKey(center, subcenter, tableVersion);
      Grib1ParamTableReader table = tableMap.get(key);
      if (table != null)
        return table;

      // match from lookup tables(s)
      table = findParameterTableExact(center, subcenter, tableVersion);
      if (table == null)
        table = findParameterTable(center, subcenter, tableVersion);
      if (table == null) {
        if (strict || defaultTable == null) {
          // table = findParameterTable(center, subcenter, tableVersion); // debug
          logger.warn("Could not find a table for GRIB file with center: " + center + " subCenter: " + subcenter + " version: " + tableVersion);
          throw new UnsupportedOperationException("Could not find a table for GRIB file with center: " + center + " subCenter: " + subcenter + " version: " + tableVersion);
        }
        return defaultTable;
      }

      tableMap.put(key, table); // assume we would get the same table in any thread, so race condition is ok
      return table;
    }

    private Grib1ParamTableReader findParameterTableExact(int center, int subcenter, int version) {
      List<Grib1ParamTableReader> localCopy = tables; // thread safe
      for (Grib1ParamTableReader table : localCopy) {
        // look for a match
        if (center == table.getCenter_id() && subcenter == table.getSubcenter_id() && version == table.getVersion()) {  // match
          if (table.getParameters() == null) //  see if the parameters for this table have been read in yet.
            continue; // failed - maybe theres another entry table that matches

          // success - initialize other tables with the same path
          for (Grib1ParamTableReader table2 : localCopy) {
            if (table2.getPath().equals(table.getPath()))
              table2.setParameters(table.getParameters());
          }
          return table;
        }
      }

      return null;
    }

    // wildcard match
    private Grib1ParamTableReader findParameterTable(int center, int subcenter, int version) {
      List<Grib1ParamTableReader> localCopy = tables; // thread safe
      for (Grib1ParamTableReader table : localCopy) {
        // look for a match
        if (center == table.getCenter_id()) {
          if ((table.getSubcenter_id() == -1) || (subcenter == table.getSubcenter_id())) {
            //if ((table.subcenter_id == -1) || (table.subcenter_id == 0) || (subcenter == table.subcenter_id)) {
            if ((table.getVersion() == -1) || version == table.getVersion()) {  // match
              //  see if the parameters for this table have been read in yet.
              if (table.getParameters() == null)
                continue; // failed - maybe theres another entry table that matches

              // success - initialize other tables with the same path
              for (Grib1ParamTableReader table2 : localCopy) {
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
