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
import com.sleepycat.persist.StoreConfig;

import java.io.*;
import java.util.*;

import thredds.inventory.MFile;
import ucar.nc2.units.DateFormatter;

/**
 * MetadataManager using Berkeley DB Java Edition.
 * Single environment. Each collection is a "database".
 * Each database has a set of key/value pairs.
 * default root dir is ${user.home}/.unidata/bdb
 *
 * @author caron
 * @since Aug 20, 2008
 */
public class MetadataManager {
  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(MetadataManager.class);
  private static final String UTF8 = "UTF-8";

  private static String root = null;
  private static Environment myEnv = null;
  private static List<Database> openDatabases = new ArrayList<Database>();
  private static boolean readOnly;
  private static boolean debug = false;

  static {
    String home = System.getProperty("user.home");

    if (home == null)
      home = System.getProperty("user.dir");

    if (home == null)
      home = ".";

    root = home + "/.unidata/bdb/";
  }


  static public void setCacheDirectory(String dir) {
    root = dir;
    setup(root, false);
  }

  static private void setup(String dirName, boolean _readOnly) throws DatabaseException {
    readOnly = _readOnly;
    EnvironmentConfig myEnvConfig = new EnvironmentConfig();
    //StoreConfig storeConfig = new StoreConfig();

    myEnvConfig.setReadOnly(readOnly);
    //storeConfig.setReadOnly(readOnly);

    myEnvConfig.setAllowCreate(!readOnly);
    //storeConfig.setAllowCreate(!readOnly);

    myEnvConfig.setSharedCache(true);

    File dir = new File(dirName);
    dir.mkdirs();

    try {
      myEnv = new Environment(dir, myEnvConfig); // LOOK may want to try multiple Environments
      logger.info("MetadataManager opened bdb in directory=" + dir);

    } catch (com.sleepycat.je.EnvironmentLockedException e) {
      // try read-only
      readOnly = true;
      myEnvConfig.setReadOnly(true);
      myEnvConfig.setAllowCreate(false);
      myEnv = new Environment(dir, myEnvConfig);
    }
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

  // Close the store and environment

  static public void closeAll() {
    if (debug) System.out.println("close MetadataManager");

    for (Database db : openDatabases) {
      if (debug) System.out.println("  close database " + db.getDatabaseName());
      db.close();
    }

    /* if (secondary != null) {
      try {
        secondary.close();
      } catch (DatabaseException dbe) {
        System.err.println("Error closing secondary database: " + dbe.toString());
        System.exit(-1);
      }
    }

    if (database != null) {
      try {
        database.close();
      } catch (DatabaseException dbe) {
        System.err.println("Error closing database: " + dbe.toString());
        System.exit(-1);
      }
    } */

    if (myEnv != null) {
      try {
        // Finally, close the store and environment.
        myEnv.close();
        myEnv = null;

      } catch (DatabaseException dbe) {
        logger.error("Error closing MyDbEnv: " + dbe.toString());
        System.exit(-1);
      }
    }
  }

  static public void showEnvStats(Formatter f) {
    if (myEnv == null)
      setup(root, false);

    try {
      EnvironmentStats stats = myEnv.getStats(null);
      f.format("env stats= %s%n", stats);

      for (String dbName : myEnv.getDatabaseNames()) {
        f.format(" collection = %s%n", dbName);
      }
    } catch (DatabaseException e) {
      e.printStackTrace();
    }
  }

  static public String getCacheLocation() {
    return root;
  }

  static public List<String> getCollectionNames() {
    if (myEnv == null)
      setup(root, false);
    return myEnv.getDatabaseNames();
  }

  static public boolean deleteCollection(String collectionName) {
    try {
      myEnv.removeDatabase(null, collectionName);
      return true;
    } catch (Exception e) {
      e.printStackTrace();
      logger.error("BDB failed to delete Collection ", e);
      return false;
    }
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

  ////////////////////////////////////////////////////

  private String collectionName;
  private Database database;
  private SecondaryDatabase secondary;

  private Calendar cal = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
  private DateFormatter dateFormatter = new DateFormatter();
  private boolean showDelete = false;

  public MetadataManager(String collectionName) throws DatabaseException, IOException {
    this.collectionName = collectionName;

    if (myEnv == null) {
      setup(root, false);
    }

    // primary
    DatabaseConfig dbConfig = new DatabaseConfig();
    dbConfig.setReadOnly(readOnly);
    dbConfig.setAllowCreate(!readOnly);
    if (!readOnly)
      dbConfig.setDeferredWrite(true);
    database = myEnv.openDatabase(null, collectionName, dbConfig);
    openDatabases.add(database);
  }

  public void put(String key, String value) {
    if (readOnly) return;
    try {
      database.put(null, new DatabaseEntry(key.getBytes(UTF8)), new DatabaseEntry(value.getBytes(UTF8)));
    } catch (UnsupportedEncodingException e) {
      throw new RuntimeException(e.getMessage());
    }
  }

  public void put(byte[] key, byte[] value) {
    if (readOnly) return;
    database.put(null, new DatabaseEntry(key), new DatabaseEntry(value));
  }

  public void put(String key, byte[] value) {
    try {
      database.put(null, new DatabaseEntry(key.getBytes(UTF8)), new DatabaseEntry(value));
    } catch (UnsupportedEncodingException e) {
      throw new RuntimeException(e.getMessage());
    }
  }

  public byte[] get(byte[] key) {
    DatabaseEntry value = new DatabaseEntry();
    database.get(null, new DatabaseEntry(key), value, LockMode.DEFAULT);
    return value.getData();
  }

  public byte[] getBytes(String key) {
    try {
      DatabaseEntry value = new DatabaseEntry();
      database.get(null, new DatabaseEntry(key.getBytes(UTF8)), value, LockMode.DEFAULT);
      return value.getData();
    } catch (UnsupportedEncodingException e) {
      throw new RuntimeException(e);
    }
  }

  public String get(String key) {
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
    try {
      DatabaseEntry entry = new DatabaseEntry(theKey.getBytes("UTF-8"));
      database.delete(null, entry);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public void delete(Map<String, MFile> current) {
    List<DatabaseEntry> result = new ArrayList<DatabaseEntry>();
    Cursor myCursor = null;
    try {
      myCursor = database.openCursor(null, null);
      DatabaseEntry foundKey = new DatabaseEntry();
      DatabaseEntry foundData = new DatabaseEntry();

      int count = 0;
      while (myCursor.getNext(foundKey, foundData, LockMode.DEFAULT) == OperationStatus.SUCCESS) {
        String key = new String(foundKey.getData(), UTF8);
        int pos = key.indexOf("#");
        if (pos > 0) {
          String filename = key.substring(0, pos);
          if (null == current.get(filename)) {
            //System.out.printf("%s not current %n", filename);
            result.add(new DatabaseEntry(foundKey.getData()));
            count++;
          } else {
            //System.out.printf("%s is current%n", filename);
          }
        }
      }

      //System.out.printf("total found to delete = %d%n", count);

      if (showDelete) {
        for (DatabaseEntry entry : result) {
          OperationStatus status = database.delete(null, entry);
          String key = new String(entry.getData(), UTF8);
          System.out.printf("%s deleted %s%n", status, key);
        }
      }

    } catch (UnsupportedOperationException e) {
      logger.error("Trying to delete "+collectionName, e);

    } catch (UnsupportedEncodingException e) {
      logger.error("Trying to delete "+collectionName, e);

    } finally {
      if (null != myCursor)
        myCursor.close();
    }

  }

  public void close() {
    database.close();
    openDatabases.remove(database);
  }

  public void showStats(Formatter f) {
    try {
      DatabaseStats dstats = database.getStats(null);
      f.format("primary stats %n%s%n", dstats);

    } catch (DatabaseException e) {
      e.printStackTrace();
    }
  }

  public List<KeyValue> getContent() throws DatabaseException, UnsupportedEncodingException {
    List<KeyValue> result = new ArrayList<KeyValue>();
    Cursor myCursor = null;
    try {
      myCursor = database.openCursor(null, null);
      DatabaseEntry foundKey = new DatabaseEntry();
      DatabaseEntry foundData = new DatabaseEntry();

      int count = 0;
      while (myCursor.getNext(foundKey, foundData, LockMode.DEFAULT) == OperationStatus.SUCCESS) {
        String key = new String(foundKey.getData(), UTF8);
        String data = new String(foundData.getData(), UTF8);
        result.add(new KeyValue(key, data));
        //System.out.printf("key = %s; data = %s %n", key, data);
        count++;
      }
      //System.out.printf("count = %d %n", count);
    } finally {
      if (null != myCursor)
        myCursor.close();
    }

    return result;
  }

public class KeyValue {
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
