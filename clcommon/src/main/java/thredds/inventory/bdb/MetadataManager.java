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

package thredds.inventory.bdb;

import com.sleepycat.je.*;

import java.io.*;
import java.util.*;

import thredds.inventory.MFile;
import thredds.inventory.StoreKeyValue;
import ucar.nc2.constants.CDM;

/**
 * MetadataManager using Berkeley DB Java Edition.
 * Single environment, with multiple databases.
 * All threads share one environment; multiple processes can only have one writer at a time.
 * Each collection is a "database".
 * Each database has a set of key/value pairs.
 * default root dir is ${user.home}/.unidata/bdb
 * default TDS uses {tomcat_home}/content/thredds/cache/collection
 *
 * @author caron
 * @since Aug 20, 2008
 */
public class MetadataManager implements StoreKeyValue {
  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(MetadataManager.class);
  private static final String UTF8 = "UTF-8";

  private static String root = null;
  private static long maxSizeBytes = 0;
  private static int jvmPercent = 2;
  private static Environment myEnv = null;
  private static List<MetadataManager> openDatabases = new ArrayList<MetadataManager>();
  private static boolean readOnly = false;
  private static boolean debug = false;

  //private static boolean debugDelete = false;

  static {
    String home = System.getProperty("user.home");

    if (home == null)
      home = System.getProperty("user.dir");

    if (home == null)
      home = ".";

    root = home + "/.unidata/bdb/";
  }

  static public synchronized void setCacheDirectory(String dir, long _maxSizeBytes, int _jvmPercent) {
    root = dir;
    maxSizeBytes = _maxSizeBytes;
    jvmPercent = _jvmPercent;
  }

  static private synchronized void setup() throws DatabaseException {
    if (myEnv != null) return; // someone else did it
    logger.info("try to open bdb");

    EnvironmentConfig myEnvConfig = new EnvironmentConfig();
    myEnvConfig.setReadOnly(false);
    myEnvConfig.setAllowCreate(true);
    myEnvConfig.setSharedCache(true);

    if (maxSizeBytes > 0)
      myEnvConfig.setCacheSize(maxSizeBytes);
    else
      myEnvConfig.setCachePercent(jvmPercent);

    File dir = new File(root);
    if (!dir.exists() && !dir.mkdirs())
      logger.warn("MetadataManager failed to make directory " + root);

    try {
      myEnv = new Environment(dir, myEnvConfig); // LOOK may want to try multiple Environments
      readOnly = false;

     } catch (com.sleepycat.je.EnvironmentLockedException e) {
      // another process has it open: try read-only
      logger.warn("MetadataManager failed to open directory {}, try read-only", root);
      logger.error("failed to open bdb", e);
      logger.warn("myEnvConfig {}", myEnvConfig);

      readOnly = true;
      myEnvConfig.setReadOnly(true);
      myEnvConfig.setAllowCreate(false);
      myEnv = new Environment(dir, myEnvConfig);
    }
    logger.info("MetadataManager: open bdb at root "+root+" readOnly = " +readOnly);

    /* primary
    DatabaseConfig dbConfig = new DatabaseConfig();
    dbConfig.setAllowCreate(true);
    dbConfig.setDeferredWrite(true);
    database = myEnv.openDatabase(null, "primaryDatabase", dbConfig);

    /* secondary
    SecondaryKeyCreator keyCreator = new SecondaryKeyCreator(new DataBinding());
    SecondaryConfig secConfig = new SecondaryConfig();
    secConfig.setAllowCreate(!readOnly);
    secConfig.setSortedDuplicates(!readOnly);
    secConfig.setKeyCreator(keyCreator);

    secondary = myEnv.openSecondaryDatabase(null, "secDatabase", database, secConfig);  */
  }

  // Close all databases and environment
  // this is called on TDS shutdown and reinit
  static synchronized public void closeAll() {

    List<MetadataManager> closeDatabases = new ArrayList<MetadataManager>(openDatabases);
    for (MetadataManager mm : closeDatabases) {
      if (debug) System.out.println("  close database " + mm.collectionName);
      mm.close();
    }
    openDatabases = new ArrayList<MetadataManager>(); // empty

    if (myEnv != null) {
      try {
        // Finally, close the store and environment.
        myEnv.close();
        myEnv = null;
        logger.info("closed bdb caching");

      } catch (DatabaseException dbe) {
        logger.error("Error closing bdb: ", dbe);
      }
    }
  }

  static public void showEnvStats(Formatter f) {
    if (myEnv == null)
      setup();

    try {
      EnvironmentStats stats = myEnv.getStats(null);
      f.format("EnvironmentStats%n%s%n", stats);

      f.format("%nDatabaseNames%n");
      for (String dbName : myEnv.getDatabaseNames()) {
        f.format(" %s%n", dbName);
      }
    } catch (DatabaseException e) {
      e.printStackTrace();
    }
  }

  static public synchronized String getCacheLocation() {
    return root;
  }

  static public void sync() {
    if (myEnv != null)
      myEnv.sync();
  }

  static public List<String> getCollectionNames() {
    if (myEnv == null)
      setup();
    return myEnv.getDatabaseNames();
  }

  static public synchronized void deleteCollection(String collectionName) throws Exception {
    List<MetadataManager> closeDatabases = new ArrayList<>(openDatabases);

    // close any open handles
    for (MetadataManager mm : closeDatabases) {
      if (mm.collectionName.equals(collectionName)) {
        if (mm.database != null) {
          mm.database.close();
          mm.database = null;
          openDatabases.remove(mm);
        }
      }
    }
    myEnv.removeDatabase(null, collectionName);
  }

  static public void delete(String collectionName, String key) {
    try {
      MetadataManager mm = new MetadataManager(collectionName);
      mm.delete(key);
      mm.close();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  static public StoreKeyValue.Factory getFactory() {
    return new MetadataManagerFactory();
  }

  static private class MetadataManagerFactory implements StoreKeyValue.Factory {

    @Override
    public StoreKeyValue open(String name) {
      try {
        return new MetadataManager(name);
      } catch (IOException ioe) {
        throw new RuntimeException(ioe);
      }
    }
  }

  ////////////////////////////////////////////////////

  private String collectionName;
  private Database database;
  //private SecondaryDatabase secondary;
  //private Calendar cal = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
  //private DateFormatter dateFormatter = new DateFormatter();

  public MetadataManager(String collectionName) throws DatabaseException, IOException {
    this.collectionName = collectionName;

    // fail fast
    if (myEnv == null) {
      setup();
    }
  }

  // assumes only one open at a time; could have MetadataManagers share open databases
  private void openDatabase() {
    if (database != null) return;
    DatabaseConfig dbConfig = new DatabaseConfig();
    dbConfig.setReadOnly(readOnly);
    dbConfig.setAllowCreate(!readOnly);
    if (!readOnly)
      dbConfig.setDeferredWrite(true);
    database = myEnv.openDatabase(null, collectionName, dbConfig);
    
    openDatabases.add(this);
  }

  public void put(String key, String value) {
    if (readOnly) return;
    openDatabase();
    try {
      database.put(null, new DatabaseEntry(key.getBytes(UTF8)), new DatabaseEntry(value.getBytes(UTF8)));
    } catch (UnsupportedEncodingException e) {
      throw new RuntimeException(e.getMessage());
    }
  }

  public void put(byte[] key, byte[] value) {
    if (readOnly) return;
    openDatabase();
    database.put(null, new DatabaseEntry(key), new DatabaseEntry(value));
  }

  public void put(String key, byte[] value) {
    if (readOnly) return;
    openDatabase();
    try {
      database.put(null, new DatabaseEntry(key.getBytes(UTF8)), new DatabaseEntry(value));
    } catch (UnsupportedEncodingException e) {
      throw new RuntimeException(e.getMessage());
    }
  }

  public byte[] get(byte[] key) {
    openDatabase();
    DatabaseEntry value = new DatabaseEntry();
    database.get(null, new DatabaseEntry(key), value, LockMode.DEFAULT);
    return value.getData();
  }

  public byte[] getBytes(String key) {
    openDatabase();
    try {
      DatabaseEntry value = new DatabaseEntry();
      database.get(null, new DatabaseEntry(key.getBytes(UTF8)), value, LockMode.DEFAULT);
      return value.getData();
    } catch (UnsupportedEncodingException e) {
      throw new RuntimeException(e);
    }
  }

  public String get(String key) {
    openDatabase();
    try {
      DatabaseEntry value = new DatabaseEntry();
      OperationStatus status = database.get(null, new DatabaseEntry(key.getBytes(UTF8)), value, LockMode.DEFAULT);
      if (status == OperationStatus.SUCCESS)
        return new String(value.getData(), UTF8);
      else
        return null;
    } catch (UnsupportedEncodingException e) {
      throw new RuntimeException(e);
    }
  }

  public void delete(String theKey) {
    if (readOnly) {
      logger.warn("Cant delete - readOnly mode");
      return;
    }

    openDatabase();
    try {
      DatabaseEntry entry = new DatabaseEntry(theKey.getBytes(CDM.utf8Charset));
      database.delete(null, entry);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public void delete(Map<String, MFile> current) {
    if (readOnly) {
      logger.warn("Cant delete - readOnly mode");
      return;
    }
    
    openDatabase();
    List<DatabaseEntry> result = new ArrayList<DatabaseEntry>();
    Cursor myCursor = null;
    try {
      myCursor = database.openCursor(null, null);
      DatabaseEntry foundKey = new DatabaseEntry();
      DatabaseEntry foundData = new DatabaseEntry();

      int count = 0;
      while (myCursor.getNext(foundKey, foundData, LockMode.DEFAULT) == OperationStatus.SUCCESS) {
        String key = new String(foundKey.getData(), UTF8);
        int pos = key.indexOf('#');
        if (pos > 0) {
          String filename = key.substring(0, pos);
          if (null == current.get(filename)) {
            logger.debug("{} not in current - want to delete from bdb", filename);
            result.add(new DatabaseEntry(foundKey.getData()));
            count++;
          } else {
            logger.debug("{} is in current", filename);
          }
        }
      }

      int countDelete = 0;
      for (DatabaseEntry entry : result) {
        OperationStatus status = database.delete(null, entry);
        String key = new String(entry.getData(), UTF8);
        logger.debug("{} deleted {}", status, key);
        countDelete++;
      }
      logger.info("{} #files deleted = {}", collectionName, countDelete);

    } catch (UnsupportedOperationException e) {
      logger.error("Trying to delete " + collectionName, e);

    } catch (UnsupportedEncodingException e) {
      logger.error("Trying to delete " + collectionName, e);

    } finally {
      if (null != myCursor)
        myCursor.close();
    }

  }

  public void close() {
    if (database != null) {
      database.close();
      openDatabases.remove(this);
      database = null;
    }
  }

  public void showStats(Formatter f) {
    openDatabase();
    try {
      DatabaseStats dstats = database.getStats(null);
      f.format("primary stats %n%s%n", dstats);

    } catch (DatabaseException e) {
      e.printStackTrace();
    }
  }

  public List<KeyValue> getContent() throws DatabaseException, UnsupportedEncodingException {
    openDatabase();
    List<KeyValue> result = new ArrayList<KeyValue>();
    Cursor myCursor = null;
    try {
      myCursor = database.openCursor(null, null);
      DatabaseEntry foundKey = new DatabaseEntry();
      DatabaseEntry foundData = new DatabaseEntry();

      //int count = 0;
      while (myCursor.getNext(foundKey, foundData, LockMode.DEFAULT) == OperationStatus.SUCCESS) {
        String key = new String(foundKey.getData(), UTF8);
        String data = new String(foundData.getData(), UTF8);
        result.add(new KeyValue(key, data));
        //System.out.printf("key = %s; data = %s %n", key, data);
        //count++;
      }
      //System.out.printf("count = %d %n", count);
    } finally {
      if (null != myCursor)
        myCursor.close();
    }

    return result;
  }

  public static class KeyValue {
    public String key;
    public String value;

    KeyValue(String key, String value) {
      this.key = key;
      this.value = value;
    }
  }

  public static void main(String args[]) throws Exception {
    MetadataManager indexer = new MetadataManager("dummy");
    indexer.showStats(new Formatter(System.out));
    MetadataManager.closeAll();
  }


}
