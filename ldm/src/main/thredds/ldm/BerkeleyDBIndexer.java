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
import com.sleepycat.bind.tuple.TupleBinding;
import com.sleepycat.bind.tuple.TupleOutput;
import com.sleepycat.bind.tuple.TupleInput;

import java.io.File;
import java.io.IOException;
import java.util.Random;

import ucar.bufr.Message;

/**
 * Class Description.
 *
 * @author caron
 * @since Aug 20, 2008
 */
public class BerkeleyDBIndexer {

  private Environment myEnv;
  private Database database;

  private int fileno;

  public BerkeleyDBIndexer(String dir, int fileno) {
    this.fileno = fileno;

    try {
      setup(dir, false);

    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private void setup(String dir, boolean readOnly) throws DatabaseException {
    EnvironmentConfig myEnvConfig = new EnvironmentConfig();
    StoreConfig storeConfig = new StoreConfig();

    myEnvConfig.setReadOnly(readOnly);
    storeConfig.setReadOnly(readOnly);

    myEnvConfig.setAllowCreate(!readOnly);
    storeConfig.setAllowCreate(!readOnly);

    myEnv = new Environment(new File(dir), myEnvConfig);

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

  private class MyData {
    int nobs;
    long date;

    public String toString() { return date + " "+nobs; }
  }

  private class MyKey {
    int fileno;
    long pos;

    public String toString() { return fileno + " "+pos; }
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

  boolean writeIndex(Message m, long pos) throws IOException {

    TupleBinding keyBinding = new KeyBinding();
    TupleBinding dataBinding = new DataBinding();
    MyKey myKey = new MyKey();
    MyData myData = new MyData();

    int n = m.getNumberDatasets();
    for (int i=0; i<n; i++) {
      myKey.fileno = fileno;
      myKey.pos = pos;
      //myData.date = m.getObsTime(i);
      //myData.lat = m.getLat(i);
      //myData.lon = m.getLon(i);

      DatabaseEntry keyEntry = new DatabaseEntry();
      keyBinding.objectToEntry(myKey, keyEntry);

      DatabaseEntry dataEntry = new DatabaseEntry();
      dataBinding.objectToEntry(myData, dataEntry);

      try {
        database.put(null, keyEntry, dataEntry);
      } catch (DatabaseException e) {
        throw new IOException(e);
      }
    }
    return true;
  }

}
