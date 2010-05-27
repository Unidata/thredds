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

import ucar.grib.GribResourceReader;
import ucar.grib.NotSupportedException;
import ucar.grib.Parameter;
import ucar.grid.GridParameter;

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
 *        threadsafe 9/25/08 jcaron see http://www.ibm.com/developerworks/java/library/j-hashmap.html
 */

public final class GribPDSParamTable {
  static private org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(GribPDSParamTable.class);

  static private final String RESOURCE_PATH = "resources/grib/tables";
  static private final String TABLE_LIST = "tablelookup.lst";

  static private final Pattern valid = Pattern.compile("^[a-zA-Z_][a-zA-Z0-9_@:\\.\\-\\+]*$");
  static private final Pattern numberFirst = Pattern.compile("^[0-9]" );

  /**
   * Added by Richard D. Gonzalez
   * static Array with parameter tables used by the GRIB file
   * (should only be one, but not actually limited to that - this allows
   * GRIB files to be read that have more than one center's information in it)
   */
  static private volatile GribPDSParamTable[] paramTables = null;
  static private Object lock = new Object();

  static private boolean debug = false;


  /**
   * This is a mapping from (center,subcenter,number)-> Param table for any data that has been loaded
   */
  static private Map<String, GribPDSParamTable> tableMap = new ConcurrentHashMap<String, GribPDSParamTable>();

  static {
    try {
      ArrayList<GribPDSParamTable> tables = new ArrayList<GribPDSParamTable>();
      initFromJAR(tables);
      paramTables = (GribPDSParamTable[]) tables.toArray( new GribPDSParamTable[tables.size()]);

    } catch (IOException ioe) {
       throw new RuntimeException( ioe);
    }
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

    // leave out of lock since it does IO
    ArrayList<GribPDSParamTable> tables = new ArrayList<GribPDSParamTable>();
    if (!readTableEntries(is, null, tables)) {
      return;
    }

    synchronized (lock) {
      // tmp table stores new user defined tables plus tablelookup.lst table entries
      GribPDSParamTable[] tmp = new GribPDSParamTable[paramTables.length + tables.size()];
      for (int idx = 0; idx < paramTables.length + tables.size(); idx++) {
        if (idx < tables.size()) {
          tmp[idx] = (GribPDSParamTable) tables.get(idx);
          //System.out.println( "usrlookup tables = " + tmp[ idx ].path );
        } else {
          tmp[idx] = paramTables[idx - tables.size()];  // tablelookup.lst entries
        }
      }
      paramTables = tmp;  // new copy of the data structure
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
   * @throws IOException on read error
   */
  public static void addParameterUserLookup(String userGribTabList) throws IOException {

    // leave out of lock since it does IO
    ArrayList<GribPDSParamTable> tables = new ArrayList<GribPDSParamTable>();
    if (!readTableEntries(userGribTabList, tables)) {
      //            System.err.println ("could not read:" + userGribTabList);
      return;
    }

    synchronized (lock) {
      //           System.err.println ("read:" + userGribTabList);
      // tmp table stores new user defined tables plus tablelookup.lst table entries
      GribPDSParamTable[] tmp = new GribPDSParamTable[paramTables.length + tables.size()];
      for (int idx = 0; idx < paramTables.length + tables.size(); idx++) {
        if (idx < tables.size()) {
          tmp[idx] = tables.get(idx); // new stuff first
        } else {
          tmp[idx] = paramTables[idx - tables.size()];  // old stuff
        }
      }
      paramTables = tmp;  // new copy of the data structure
    }
  }


  /**
   * Load default tables from jar file (class path)
   * <p/>
   * Reads in the list of tables available and stores them.  Does not actually
   * open the parameter tables files, nor store the list of parameters, but
   * just stores the file names of the parameter tables.
   * Parameters for a table are read in when the table is requested (in the
   * getParameterTable method).
   * Currently hardcoded the file name to "tablelookup".
   * <p/>
   * Added by Tor C.Bekkvik.
   *
   * @param aTables put tables here
   * @throws IOException on read error
   */
  private static void initFromJAR(ArrayList<GribPDSParamTable> aTables) throws IOException {
    String resourceName = RESOURCE_PATH + "/" + TABLE_LIST;
    readTableEntries(resourceName, aTables);
  }

  /**
   * Get an input stream for the resource.
   *
   * @param resourceName name of resource
   * @return corresponding input stream
   */
  private static InputStream getInputStream(String resourceName) {
    //Just turn around and let the GribResourceReader do its work
    return GribResourceReader.getInputStream(resourceName);
  }

  /**
   * _more_
   *
   * @param aTableList _more_
   * @param aTables    _more_
   * @return Was read successful
   * @throws IOException On badness
   */
  private static boolean readTableEntries(String aTableList, ArrayList<GribPDSParamTable> aTables) throws IOException {
    InputStream inputStream = getInputStream(aTableList);
    if (inputStream == null) {
      logger.debug("Could not open table file:" + aTableList);
      return false;
    }
    return readTableEntries(inputStream, aTableList, aTables);
  }

  /**
   * Read the table list pointed to by the given input stream
   *
   * @param is         The input stream
   * @param aTableList The name of the table list file
   * @param aTables    The list to add the tables into
   * @return Was successful
   * @throws IOException On badness
   */
  private static boolean readTableEntries(InputStream is, String aTableList, ArrayList<GribPDSParamTable> aTables) throws IOException {
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
      table.table_number = Integer.parseInt(tableDefArr[2].trim());
      table.filename = tableDefArr[3].trim();
      if (table.filename.startsWith("/")
              || table.filename.startsWith("\\")
              || table.filename.startsWith("file:")
              || table.filename.startsWith("http://")) {
        table.path = table.filename;
      } else if (aTableList != null) {
        table.path = GribResourceReader.getFileRoot(aTableList);
        if (table.path.equals(aTableList)) {
          table.path = table.filename;
        } else {
          table.path = table.path + "/" + table.filename;
        }
        table.path = table.path.replace('\\', '/');
      }
      aTables.add(table);
    }
    is.close();
    return true;
  }


  /**
   * Looks for the parameter table which matches the center, subcenter
   * and table version from the tables array.
   * If this is the first time asking for this table, then the parameters for
   * this table have not been read in yet, so this is done as well.
   *
   * @param center    - integer from PDS octet 5, representing Center.
   * @param subcenter - integer from PDS octet 26, representing Subcenter
   * @param number    - integer from PDS octet 4, representing Parameter Table Version
   * @return GribPDSParamTable matching center, subcenter, and number
   * @throws NotSupportedException no table found
   */
  public static GribPDSParamTable getParameterTable(int center, int subcenter, int number)
          throws NotSupportedException {

    String key = center + "_" + subcenter + "_" + number;
    if ( center == -1 ) { // non existent table
       logger.error( "GribPDSParamTable: non existent table for center, subcenter, table = "+ key );
       return null;
    }

    GribPDSParamTable table = tableMap.get(key);
    if (table != null)
      return table;

    table = readParameterTable(center, subcenter, number, true);

    if (table == null) {
      logger.error( "GribPDSParamTable: cannot find table for center, subcenter, table "+ key );
      throw new NotSupportedException("Could not find a table entry for GRIB file with center: "
                      + center + " subCenter: " + subcenter + " number: " + number);
    }

    tableMap.put(key, table);
    return table;
  }


  /**
   * Looks for the parameter table which matches the center, subcenter
   * and table version from the tables array.
   * If this is the first time asking for this table, then the parameters for
   * this table have not been read in yet, so this is done as well.
   *
   * @param center    - integer from PDS octet 5, representing Center.
   * @param subcenter - integer from PDS octet 26, representing Subcenter
   * @param number    - integer from PDS octet 4, representing Parameter Table Version
   * @param firstTry  - Is this the first call or are we trying the wild cards
   *
   * @return GribPDSParamTable matching center, subcenter, and number
   */
  private static int wmoTable;
  private static GribPDSParamTable readParameterTable(int center, int subcenter, int number, boolean firstTry) {

   if( firstTry )
     wmoTable = number;

   GribPDSParamTable[] localCopy = paramTables; // thread safe

    for (GribPDSParamTable table : localCopy) {

      if (center == table.center_id) {
        if ((table.subcenter_id == -1) || (subcenter == table.subcenter_id)) {
          if (number == table.table_number) {
            // now that this table is being used, check to see if the
            //   parameters for this table have been read in yet.
            // If not, initialize table and read them in now.
            if (table.parameters == null) {
              if (!firstTry) {
                logger. warn("GribPDSParamTable: Using default table:"
                        + table.path + " (" + table.center_id
                        + ":" + table.subcenter_id + ":"
                        + table.table_number + ")");
              }
              table.readParameterTable();
              if (table.parameters == null) // failed - maybe theres another entry table in paramTables
                continue;

              // success - initialize other tables parameters with the same name
              for (int j = 0; j < paramTables.length; j++) {
                GribPDSParamTable tab = paramTables[j];
                if (tab.path.equals(table.path)) {
                  tab.parameters = table.parameters;
                }
              }

            }
            return table;
          }
        }
      }
    }

    //Try with the  wild cards
    if (number != -1) {
      return readParameterTable(center, subcenter, -1, false);

    } else if (subcenter != -1) {
      logger. warn("GribPDSParamTable: Could not find table for center:" + center
              + " subcenter:" + subcenter + " number:" + wmoTable);
      return readParameterTable(center, -1, -1, false);

    } else if (center != -1) {
      //return readParameterTable(-1, -1, -1, false);
      return readParameterTable(-1, -1, wmoTable, false);
    }

    return null;
  }

  /**
   * Munge a description to make it suitable as variable name
   *
   * @param  description start with this
   * @return Valid Description
   */
  static private String makeValidDesc(String description) {
    description = description.replaceAll("\\s+", "_");
    if (valid.matcher(description).find())
      return description;
    // else check for special characters
    if (numberFirst.matcher(description).find())
       description = "N" + description;
    return description.replaceAll("\\)|\\(|=|,|;|\\[|\\]","");
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
  private int table_number;

  /**
   * Stores the name of the file containing this table - not opened unless
   * required for lookup.
   */
  private String filename = null;

  /**
   * path of filename containing this table.
   * Opened if required for lookup.
   */
  private String path = null;

  /**
   * Map ids to GridParameter objects
   */
  private Map<String, GridParameter> parameters = null;

  private GribPDSParamTable() {
  }

  /**
   * Read parameter table.
   */
  private void readParameterTable() {
    if (path == null) return;

    try {
      InputStream is = getInputStream(path);
      if (is == null)
        return;
      BufferedReader br = new BufferedReader(new InputStreamReader(is));

      // Read first line that has center, subcenter, and version of table
      String line = br.readLine();
      if( debug ) System.out.println( line );
      String[] tableDefArr = line.split(":");

      /*  LOOK - why not test values ?
      center    = Integer.parseInt(tableDefArr[1].trim());
      subcenter = Integer.parseInt(tableDefArr[2].trim());
      number    = Integer.parseInt(tableDefArr[3].trim());
      if ((center != center_id) && (subcenter != subcenter_id)
              && (number != table_number)) {
          throw new java.io.IOException(
              "parameter table header values do not "
              + " match values in GRIB file.  Possible error in lookup table.");
      }
      */

      HashMap<String, GridParameter>  tmpParameters = new HashMap<String, GridParameter>(); // thread safe - temp hash

      // rdg - added the 0 line length check to cover the case of blank lines at
      //       the end of the parameter table file.
      while ((line = br.readLine()) != null ) {
        if ((line.length() == 0) || line.startsWith("#")) {
          continue;
        }
        Parameter parameter = new Parameter();
        tableDefArr = line.split(":");
        parameter.setNumber(Integer.parseInt(tableDefArr[0].trim()));
        parameter.setName(tableDefArr[1].trim());
        // check to see if unit defined, if not, parameter is undefined
        if (tableDefArr[2].indexOf('[') == -1) {
          // Undefined unit
          parameter.setDescription(tableDefArr[2].trim());
          parameter.setUnit(tableDefArr[2].trim());
        } else {
          String[] arr2 = tableDefArr[2].split("\\[");
          parameter.setDescription( makeValidDesc(arr2[0].trim()));
          //System.out.println( "Desc ="+ parameter.getDescription());
          // Remove "]"
          parameter.setUnit(arr2[1].substring(0, arr2[1].lastIndexOf(']')).trim());
        }
        tmpParameters.put(Integer.toString(parameter.getNumber()), parameter);
        if( debug )
          System.out.println( parameter.getNumber() +" "+ parameter.getDescription() +" "+ parameter.getUnit());
      }

      this.parameters = tmpParameters; // thread safe

    } catch (IOException ioError) {
      logger.warn ("An error occurred in GribPDSParamTable while trying to open the parameter table "
              + filename + " : " + ioError);
    }

  }

  /**
   * Get the parameter with id <tt>id</tt>.
   *
   * @param id the parameter id
   * @return the GridParameter
   */
  public GridParameter getParameter(int id) {
    GridParameter p = parameters.get( Integer.toString(id));
    if (p != null)
      return p;

    logger.warn("GribPDSParamTable: Could not find parameter "+ id +" for center:" + center_id
      +" subcenter:"+ subcenter_id +" number:"+ table_number +" table "+  filename );
    String unknown = "UnknownParameter_"+ Integer.toString(id ) +"_table_"+  filename;
    return new GridParameter(id, unknown, unknown, "Unknown" );
  }

  static public void main (String[] args ) {
    debug = true;
    GribPDSParamTable.readParameterTable( 221, 221, 1, true );
  }
}

