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

import java.io.*;
import java.util.*;

import ucar.bufr.Message;
import ucar.bufr.BufrDataDescriptionSection;
import ucar.bufr.DataDescriptor;
import ucar.nc2.units.DateFormatter;
import ucar.ma2.DataType;

/**
 * Class Description.
 *
 * @author caron
 * @since Aug 20, 2008
 */
public class BerkeleyDBIndexer implements Indexer {
  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(BerkeleyDBIndexer.class);

  private String dirName;
  private Environment myEnv;
  private Database database;
  private SecondaryDatabase secondary;

  private int nStndFlds = 7;
  private boolean useDoy = false;
  private List<DataFld> dataflds = new ArrayList<DataFld>();
  private List<Short> indexFlds;
  private boolean isNew = false;

  private Calendar cal = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
  private DateFormatter dateFormatter = new DateFormatter();

  public BerkeleyDBIndexer(String dirName, List<Short> indexFlds) throws DatabaseException {
    this.dirName = dirName;
    setup(dirName, false);

    // add time fields
    dataflds.add( new DataFld( BufrDataDescriptionSection.getDesc("0-4-1")));
    dataflds.add( new DataFld( BufrDataDescriptionSection.getDesc("0-4-2")));
    dataflds.add( new DataFld( BufrDataDescriptionSection.getDesc("0-4-3")));
    dataflds.add( new DataFld( BufrDataDescriptionSection.getDesc("0-4-4")));
    dataflds.add( new DataFld( BufrDataDescriptionSection.getDesc("0-4-5")));
    dataflds.add( new DataFld( BufrDataDescriptionSection.getDesc("0-4-6")));
    dataflds.add( new DataFld( BufrDataDescriptionSection.getDesc("0-4-43")));

    for (short fxy : indexFlds)
      dataflds.add( new DataFld(fxy));
    makeIndexFlds();
    isNew = true;
  }

  public BerkeleyDBIndexer(String dirName) throws DatabaseException, IOException {
    this.dirName = dirName;
    setup(dirName, false);

    String filename = dirName +"/schema.txt";
    File inputFile = new File(filename);
    if (inputFile.exists()) {
      BufferedReader dataIS = new BufferedReader(new InputStreamReader(new FileInputStream(inputFile)));
      while (true) {
        String line = dataIS.readLine();
        if (line == null) break;
        line = line.trim();
        if (line.length() == 0) break;
        if (line.charAt(0) == '#') continue;
        dataflds.add( new DataFld(line));
      }
    }
    makeIndexFlds();
  }

  private class DataFld {
    String descName;
    short fxy;
    String fldName;
    DataType dtype;

    DataFld( String line) {
      String[] toke = line.split(",");
      this.descName = toke[0];
      this.fxy = BufrDataDescriptionSection.getDesc(descName);
      this.fldName = toke[1];
      this.dtype = DataType.getType(toke[2]);
    }

    DataFld( short fxy) {
      this.fxy = fxy;
      this.descName = BufrDataDescriptionSection.getDescName(fxy);
    }
  }

  private void setFldInfo( Message m) throws IOException {
    DataDescriptor root = m.getRootDataDescriptor();
    for (DataDescriptor dds : root.getSubKeys()) {
      DataFld fld = findKey( dds.fxy);
      if (fld != null) {
        fld.fldName = dds.name;
        if (dds.type == 0) {
          fld.dtype = (dds.scale == 0) ? DataType.INT : DataType.FLOAT;
        } else {
          fld.dtype = DataType.STRING;
        }
      }
    }
    isNew = false;
    writeIndexFlds();
  }

  private void makeIndexFlds() {
    indexFlds = new ArrayList<Short>(dataflds.size());
    for (DataFld fld : dataflds)
      indexFlds.add(fld.fxy);
}

  private DataFld findKey( short fxy) {
    for (DataFld fld : dataflds) {
      if (fld.fxy == fxy) return fld;
    }
    return null;
  }

  private void writeIndexFlds() throws FileNotFoundException {
    String filename = dirName +"/schema.txt";
    Formatter out = new Formatter(new FileOutputStream(filename));
    for (DataFld fld : dataflds) {
      out.format("%s,%s,%s%n", fld.descName, fld.fldName, fld.dtype);
    }
    out.close();
  }
  /////////////////////////

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
    System.out.println("close Indexer="+ dirName);

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

  private class MyData {
    long date;
    Object[] flds;

    public String toString() {
      Date d = new Date(date);
      Formatter out = new Formatter();
      out.format("%s", dateFormatter.toDateTimeStringISO(new Date(date)));
      for (Object data : flds)
        out.format(", %s", data);
      return out.toString();
    }
  }

  private class DataBinding extends TupleBinding {

    public void objectToEntry(Object object, TupleOutput to) {
      Object[] dataArray = (Object[]) object;

      try {
        // first extract the time
        int year = ((Number)dataArray[0]).intValue();
        int month = (dataArray[1] == null) ? 1 : ((Number)dataArray[1]).intValue();
        int day = (dataArray[2] == null) ? 0 : ((Number)dataArray[2]).intValue();
        int hour = ((Number)dataArray[3]).intValue();
        int min = (dataArray[4] == null) ? 0 : ((Number)dataArray[4]).intValue();
        int sec = (dataArray[5] == null) ? 0 : ((Number)dataArray[5]).intValue();
        int doy = (dataArray[6] == null) ? -1 : ((Number)dataArray[6]).intValue();

        cal.set(year,month-1,day,hour,min,sec);
        if (doy >= 0)
          cal.set(Calendar.DAY_OF_YEAR, doy);
        to.writeLong(cal.getTime().getTime());

      //System.out.printf("write %d, %d, %d, %d, %d == %s %n",  year, month, day, hour, min,
      //        dateFormatter.toDateTimeStringISO(cal.getTime()));
        
      } catch (Exception e) {
        System.out.println("Error on "+dirName);
        e.printStackTrace();
      }


      for (int i=nStndFlds; i<dataArray.length; i++) {
        Object data = dataArray[i];
        if (data == null) continue;

        if (data instanceof byte[]) {
          byte[] bdata = (byte []) data;
          //to.writeFast((byte) bdata.length);
          //to.writeFast(bdata);
          to.writeString(new String(bdata));
        } else if (data instanceof Float)
          to.writeFloat((Float) data);
        else if (data instanceof Integer)
          to.writeInt((Integer) data);
      }
    }

    public Object entryToObject(TupleInput ti) { // LOOK wrong
      MyData myData = new MyData();
      myData.date = ti.readLong();
      myData.flds = new Object[dataflds.size()-nStndFlds];

      for (int i=nStndFlds;i<dataflds.size(); i++) {
        int fldidx = i - nStndFlds;
        DataFld fld = dataflds.get(i);
        if (fld.dtype == DataType.STRING) {
          myData.flds[fldidx] = ti.readString();
        } else if (fld.dtype == DataType.FLOAT)
          myData.flds[fldidx] = ti.readFloat();
        else if (fld.dtype == DataType.INT)
          myData.flds[fldidx] = ti.readInt();
      }
      return myData;
    }
  }

  public boolean writeIndex(short fileno, Message m) throws IOException {
    if (isNew)
      setFldInfo(m);

    KeyBinding keyBinding = new KeyBinding();
    DataBinding dataBinding = new DataBinding();
    MyKey myKey = new MyKey();

    Object[][] result;
    try {
      result = m.readValues(indexFlds);
    } catch (Exception e) {
      System.err.println("Fail on "+m.getHeader());
      e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
      return false;
    }

    int n = result.length;
    for (int i=0; i<n; i++) {
      myKey.fileno = fileno;
      myKey.pos = (int) m.getStartPos(); // file < 2^32
      myKey.obsno = (short) i;

      DatabaseEntry keyEntry = new DatabaseEntry();
      keyBinding.objectToEntry(myKey, keyEntry);

      DatabaseEntry dataEntry = new DatabaseEntry();
      dataBinding.objectToEntry(result[i], dataEntry);
      //System.out.println("--index write "+myKey+" on "+dirName);

      try {
        database.put(null, keyEntry, dataEntry);
      } catch (DatabaseException e) {
        System.out.println("--index write error on "+dirName);
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
        System.out.printf("key = %s; data = %s %n", key, data);
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

  static boolean showFlds = false;
  static public BerkeleyDBIndexer factory( String dir, String index) throws Exception {
    String[] tokes = index.trim().split(" ");
    List<Short> indexFields = new ArrayList<Short>(tokes.length);
    Formatter out = new Formatter(System.out);
    if (showFlds) out.format("BerkeleyDBIndexer dir= %s indexFlds= ", dir);
    for( String s : tokes) {
      if (s.length() == 0) continue;
      short fxy = BufrDataDescriptionSection.getDesc("0-"+s);
      indexFields.add( fxy);
      if (showFlds) out.format("%s ", BufrDataDescriptionSection.getDescName(fxy));
    }
    if (showFlds) out.format("%n");

    return new BerkeleyDBIndexer( MessageDispatchDDS.dispatchDir+dir+"/bdb", indexFields);
  }

  public static void main(String args[]) throws Exception {
    BerkeleyDBIndexer indexer = new BerkeleyDBIndexer(MessageDispatchDDS.dispatchDir+"RJTD-IUCN53/bdb");
    indexer.retrieve();
    indexer.close();    
  }


}
