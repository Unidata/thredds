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
import java.util.Date;
import java.util.List;
import java.util.ArrayList;
import java.util.Formatter;

import ucar.bufr.Message;
import ucar.bufr.BufrDataDescriptionSection;
import ucar.nc2.units.DateFormatter;

/**
 * Class Description.
 *
 * @author caron
 * @since Aug 20, 2008
 */
public class BerkeleyDBIndexer implements Indexer {
  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(BerkeleyDBIndexer.class);

  private Environment myEnv;
  private Database database;
  private SecondaryDatabase secondary;

  private List<Short> indexFlds;
  private DateFormatter dateFormatter = new DateFormatter();

  public BerkeleyDBIndexer(String dir, List<Short> indexFlds) throws DatabaseException {
    this.indexFlds = indexFlds;
    setup(dir, false);
  }

  private void setup(String dirName, boolean readOnly) throws DatabaseException {
    EnvironmentConfig myEnvConfig = new EnvironmentConfig();
    StoreConfig storeConfig = new StoreConfig();

    myEnvConfig.setReadOnly(readOnly);
    storeConfig.setReadOnly(readOnly);

    myEnvConfig.setAllowCreate(!readOnly);
    storeConfig.setAllowCreate(!readOnly);

    File dir = new File(dirName);
    dir.mkdirs();

    myEnv = new Environment(dir, myEnvConfig);

    // primary
    DatabaseConfig dbConfig = new DatabaseConfig();
    dbConfig.setAllowCreate(true);
    dbConfig.setDeferredWrite(true);
    database = myEnv.openDatabase(null, "primaryDatabase", dbConfig);

    /* secondary
    MyKeyCreator keyCreator = new MyKeyCreator( new DataBinding());
    SecondaryConfig secConfig = new SecondaryConfig();
    secConfig.setAllowCreate(!readOnly);
    secConfig.setSortedDuplicates(!readOnly);
    secConfig.setKeyCreator( keyCreator);

    secondary = myEnv.openSecondaryDatabase(null, "secDatabase", database, secConfig);  */
  }

  // Close the store and environment
  public void close() {
    if (secondary != null) {
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

  //////////////////////////////////////

  /* private class SecKey {
    String stn;
    long date;
    public String toString() {
      Date d = new Date(date);
      return " <"+stn+"> " + dateFormatter.toDateTimeStringISO(d);
    }
  }

    private class SecKeyBinding extends TupleBinding {

    public void objectToEntry(Object object, TupleOutput to) {
      SecKey myKey = (SecKey) object;
      to.writeString(myKey.stn);
      to.writeLong(myKey.date);
    }

    public Object entryToObject(TupleInput ti) {
      SecKey myKey = new SecKey();
      myKey.stn = ti.readString();
      myKey.date = ti.readLong();
      return myKey;
    }
  }

  private class MyKeyCreator implements SecondaryKeyCreator {
    DataBinding dataBind;
    MyKeyCreator( DataBinding dataBind) {
      this.dataBind = dataBind;
    }

    public boolean createSecondaryKey(SecondaryDatabase secondaryDatabase, DatabaseEntry key, DatabaseEntry data, DatabaseEntry result) throws DatabaseException {
      MyData mydata = (MyData) dataBind.entryToObject(data);
      TupleOutput to = new TupleOutput();
      to.writeString(mydata.stn);
      to.writeLong(mydata.date);
      result.setData( to.getBufferBytes(), 0, to.getBufferLength());
      return true;
    }
  }   */

  private class MyData {
    String stn;
    long date;

    public String toString() {
      Date d = new Date(date);
      return dateFormatter.toDateTimeStringISO(d) + " <"+stn+">";
    }
  }

  private class DataBinding extends TupleBinding {
    public void objectToEntry(Object object, TupleOutput to) {
      Object[] dataArray = (Object[]) object;
      for (int i=0; i<dataArray.length; i++) {
        Object data = dataArray[i];
        if (data instanceof byte[])
          to.writeFast((byte[]) data);
        else if (data instanceof Float)
          to.writeFloat((Float) data);
        else if (data instanceof Integer)
          to.writeInt((Integer) data);
      }
    }
    public Object entryToObject(TupleInput ti) { // LOOK wrong
      MyData myData = new MyData();
      myData.date = ti.readLong();
      myData.stn = ti.readString();
      return null;
    }
  }

  private class MyKey {
    short fileno, obsno;
    int pos;
    public String toString() { return fileno + " "+pos+ " "+obsno; }
  }
  private class KeyBinding extends TupleBinding {
    public void objectToEntry(Object object, TupleOutput to) {
      MyKey myKey = (MyKey) object;
      to.writeShort(myKey.fileno);
      to.writeInt(myKey.pos);
      to.writeShort(myKey.obsno);
    }
    public Object entryToObject(TupleInput ti) {
      MyKey myKey = new MyKey();
      myKey.fileno = ti.readShort();
      myKey.pos = ti.readInt();
      myKey.obsno = ti.readShort();
      return myKey;
    }
  }

  public boolean writeIndex(short fileno, Message m) throws IOException {
    KeyBinding keyBinding = new KeyBinding();
    DataBinding dataBinding = new DataBinding();
    MyKey myKey = new MyKey();

    Object[][] result = m.readValues(indexFlds);
    int n = result.length;
    for (int i=0; i<n; i++) {
      myKey.fileno = fileno;
      myKey.pos = (int) m.getStartPos(); // file < 2^32
      myKey.obsno = (short) i;

      DatabaseEntry keyEntry = new DatabaseEntry();
      keyBinding.objectToEntry(myKey, keyEntry);

      DatabaseEntry dataEntry = new DatabaseEntry();
      dataBinding.objectToEntry(result[i], dataEntry);
      // System.out.println("--index write "+myKey+" == "+myData);

      try {
        database.put(null, keyEntry, dataEntry);
      } catch (DatabaseException e) {
        throw new IOException(e);
      }
    }
    return true;
  }

  private void retrieve() throws DatabaseException {
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
        System.out.printf("key = %s data = %s %n", key, data);
        count++;
      }
      System.out.printf("count = %d %n", count);
    } finally {
      if (null != myCursor)
        myCursor.close();
    }
  }

  /* private void retrieve2() throws DatabaseException {
    TupleBinding keyBinding = new KeyBinding();
    TupleBinding dataBinding = new DataBinding();

    SecondaryCursor myCursor = null;
    try {
      myCursor = secondary.openSecondaryCursor(null, null);
      DatabaseEntry foundKey = new DatabaseEntry();
      DatabaseEntry foundData = new DatabaseEntry();

      int count = 0;
      while (myCursor.getNext(foundKey, foundData, LockMode.DEFAULT) == OperationStatus.SUCCESS) {
        MyKey key = (MyKey) keyBinding.entryToObject(foundKey);
        MyData data = (MyData) dataBinding.entryToObject(foundData);
        System.out.printf("key = %s data = %s %n", key, data);
        count++;
      }
      System.out.printf("count = %d %n", count);
    } finally {
      if (null != myCursor)
        myCursor.close();
    }
  } */

  static public BerkeleyDBIndexer factory( String dir, String index) throws Exception {
    String[] tokes = index.split(" ");
    List<Short> indexFields = new ArrayList<Short>(tokes.length);
    Formatter out = new Formatter(System.out);
    out.format("BerkeleyDBIndexer dir= %s indexFlds= ", dir);
    for( String s : tokes) {
      short fxy = BufrDataDescriptionSection.getDesc("0-"+s);
      indexFields.add( fxy);
      out.format("%s ", BufrDataDescriptionSection.getDescName(fxy));
    }
    out.format("%n");

    return new BerkeleyDBIndexer( MessageDispatchDDS.dispatchDir+dir+"/bdb", indexFields);
  }

  public static void main(String args[]) throws Exception {
    BerkeleyDBIndexer indexer = BerkeleyDBIndexer.factory(MessageDispatchDDS.dispatchDir+"fslprofilers/bdb", "1-18");
    indexer.retrieve();
    indexer.close();    
  }


}
