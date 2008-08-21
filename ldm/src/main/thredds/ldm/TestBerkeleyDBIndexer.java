/*
 * Copyright 1997-2008 Unidata Program Center/University Corporation for
 * Atmospheric Research, P.O. Box 3000, Boulder, CO 80307,
 * support@unidata.ucar.edu.
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or (at
 * your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser
 * General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */

package thredds.ldm;

import com.sleepycat.je.*;
import com.sleepycat.persist.StoreConfig;
import com.sleepycat.bind.tuple.TupleOutput;
import com.sleepycat.bind.tuple.TupleInput;
import com.sleepycat.bind.tuple.TupleBinding;

import java.io.File;
import java.util.Random;

/**
 * Class Description.
 *
 * @author caron
 * @since Aug 14, 2008
 */
public class TestBerkeleyDBIndexer {
  private static File myDbEnvPath = new File("D:/bufr/bdb/");

  private class MyKey {
    int fileno;
    long pos;

    public String toString() { return fileno + " "+pos; }
  }

  private class MyData {
    int nobs;
    long date;

    public String toString() { return date + " "+nobs; }
  }

  private class DataBinding extends TupleBinding {

    public void objectToEntry(Object object, TupleOutput to) {
      MyData myData = (MyData) object;
      to.writeLong(myData.date);
      to.writeInt(myData.nobs);
    }

    // Convert a TupleInput to a MyData2 object
    public Object entryToObject(TupleInput ti) {
      MyData myData = new MyData();
      myData.date = ti.readLong();
      myData.nobs = ti.readInt();
      return myData;
    }

  }

  private class KeyBinding extends TupleBinding {

    public void objectToEntry(Object object, TupleOutput to) {
      MyKey myKey = (MyKey) object;
      to.writeInt(myKey.fileno);
      to.writeLong(myKey.pos);
    }

    // Convert a TupleInput to a MyData2 object
    public Object entryToObject(TupleInput ti) {
      MyKey myKey = new MyKey();
      myKey.fileno = ti.readInt();
      myKey.pos = ti.readLong();
      return myKey;
    }
  }


  private Environment myEnv;
  private Database database;

  private void populate() throws DatabaseException {
    Random r = new Random(System.currentTimeMillis());
    for (int i = 0; i < 100; i++) {
      String aKey = "key" + r.nextInt();
      String aData = "data" + i;
      DatabaseEntry theKey = new DatabaseEntry(aKey.getBytes());
      DatabaseEntry theData = new DatabaseEntry(aData.getBytes());
      database.put(null, theKey, theData);
    }
  }

  private void populate2() throws DatabaseException {
    TupleBinding keyBinding = new KeyBinding();
    TupleBinding dataBinding = new DataBinding();
    MyKey myKey = new MyKey();
    MyData myData = new MyData();

    Random r = new Random(System.currentTimeMillis());
    for (int i = 0; i < 100 * 1000; i++) {
      myKey.fileno = i / 10;
      myKey.pos = r.nextLong();
      myData.date = r.nextLong();
      myData.nobs = r.nextInt(100);

      DatabaseEntry keyEntry = new DatabaseEntry();
      keyBinding.objectToEntry(myKey, keyEntry);

      DatabaseEntry dataEntry = new DatabaseEntry();
      dataBinding.objectToEntry(myData, dataEntry);
      database.put(null, keyEntry, dataEntry);
    }

  }

  private void retrieve() throws DatabaseException {
    for (int i = 0; i < 100; i++) {
      String aKey = "key" + i;
      DatabaseEntry theKey = new DatabaseEntry(aKey.getBytes());
      DatabaseEntry theData = new DatabaseEntry();
      if (database.get(null, theKey, theData, LockMode.DEFAULT) == OperationStatus.SUCCESS) {
        byte[] retData = theData.getData();
        String foundData = new String(retData);
        System.out.println("For key: '" + aKey + "' found data: '" + foundData + "'.");
      } else {
        System.out.println("No record found for key '" + aKey + "'.");
      }
    }
  }

  private void retrieve2() throws DatabaseException {
    TupleBinding keyBinding = new KeyBinding();
    TupleBinding dataBinding = new DataBinding();

    Cursor myCursor = null;
    try {
      myCursor = database.openCursor(null, null);
      DatabaseEntry foundKey = new DatabaseEntry();
      DatabaseEntry foundData = new DatabaseEntry();

      int count = 0;
      while (myCursor.getNext(foundKey, foundData, LockMode.DEFAULT) == OperationStatus.SUCCESS) {
        MyKey key = (MyKey) keyBinding.entryToObject(foundKey);
        MyData data = (MyData) dataBinding.entryToObject(foundData);
        System.out.printf("key = %s data = %s %n ", key, data);
        count++;
      }
      System.out.printf("count = %d %n", count);
    } finally {
      if (null != myCursor)
        myCursor.close();
    }
  }

  private void setup(boolean readOnly) throws DatabaseException {
    EnvironmentConfig myEnvConfig = new EnvironmentConfig();
    StoreConfig storeConfig = new StoreConfig();

    myEnvConfig.setReadOnly(readOnly);
    storeConfig.setReadOnly(readOnly);

    myEnvConfig.setAllowCreate(!readOnly);
    storeConfig.setAllowCreate(!readOnly);

    myEnv = new Environment(myDbEnvPath, myEnvConfig);

    DatabaseConfig dbConfig = new DatabaseConfig();
    dbConfig.setAllowCreate(true);
    dbConfig.setDeferredWrite(true);

    database = myEnv.openDatabase(null, "sampleDatabase", dbConfig);
  }

  // Close the store and environment
  public void close() {
    if (database != null) {
      try {
        database.close();
      } catch (DatabaseException dbe) {
        System.err.println("Error closing database: " + dbe.toString());
        System.exit(-1);
      }
    }

    if (myEnv != null) {
      try {
        // Finally, close the store and environment.
        myEnv.close();
      } catch (DatabaseException dbe) {
        System.err.println("Error closing MyDbEnv: " + dbe.toString());
        System.exit(-1);
      }
    }
  }

  public static void main(String args[]) {
    TestBerkeleyDBIndexer edp = new TestBerkeleyDBIndexer();

    try {
      edp.setup(false);
      edp.populate2();
      edp.retrieve2();

    } catch (DatabaseException dbe) {
      System.err.println("ExampleDatabasePut: " + dbe.toString());
      dbe.printStackTrace();

    } catch (Exception e) {
      System.out.println("Exception: " + e.toString());
      e.printStackTrace();

    } finally {
      edp.close();
    }

    System.out.println("All done.");
  }
}
