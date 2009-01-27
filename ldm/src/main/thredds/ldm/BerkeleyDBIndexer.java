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

package thredds.ldm;

import com.sleepycat.je.*;
import com.sleepycat.persist.StoreConfig;
import com.sleepycat.bind.tuple.TupleBinding;
import com.sleepycat.bind.tuple.TupleOutput;
import com.sleepycat.bind.tuple.TupleInput;

import java.io.*;
import java.util.*;

import ucar.nc2.iosp.bufr.Message;
import ucar.nc2.iosp.bufr.BufrDataDescriptionSection;
import ucar.nc2.iosp.bufr.DataDescriptor;
import ucar.nc2.iosp.bufr.Descriptor;
import ucar.nc2.units.DateFormatter;
import ucar.ma2.DataType;

/**
 * Indexer using Berkeley DB Java Edition.
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
  private List<Integer> indexFlds;
  private boolean isInit = false;

  private Calendar cal = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
  private DateFormatter dateFormatter = new DateFormatter();

  public BerkeleyDBIndexer(String dirName, List<Short> indexFlds) throws DatabaseException {
    this.dirName = dirName;
    setup(dirName, false);

    // add time fields
    dataflds.add(new DataFld(Descriptor.getFxy("0-4-1")));
    dataflds.add(new DataFld(Descriptor.getFxy("0-4-2")));
    dataflds.add(new DataFld(Descriptor.getFxy("0-4-3")));
    dataflds.add(new DataFld(Descriptor.getFxy("0-4-4")));
    dataflds.add(new DataFld(Descriptor.getFxy("0-4-5")));
    dataflds.add(new DataFld(Descriptor.getFxy("0-4-6")));
    dataflds.add(new DataFld(Descriptor.getFxy("0-4-43")));

    for (short fxy : indexFlds)
      dataflds.add(new DataFld(fxy));
  }

  public BerkeleyDBIndexer(String dirName) throws DatabaseException, IOException {
    this.dirName = dirName;
    setup(dirName, false);

    readSchema();
  }

  private class DataFld {
    String descName;
    short fxy;
    String fldName;
    DataType dtype;
    int index = -1;

    DataFld(String line) {
      String[] toke = line.split(",");
      this.descName = toke[0];
      this.fxy = Descriptor.getFxy(descName);
      this.fldName = toke[1];
      this.dtype = DataType.getType(toke[2]);
      if (toke.length > 3)
        this.index = Integer.parseInt(toke[3]);
    }

    DataFld(short fxy) {
      this.fxy = fxy;
      this.descName = Descriptor.makeString(fxy);
    }
  }

  // we need an actual message to finish initializing
  private void setFldInfo(Message m) throws IOException {
    DataDescriptor root = m.getRootDataDescriptor();
    List<DataDescriptor> ddsList = root.getSubKeys();
    for (int index = 0; index < ddsList.size(); index++) {
      DataDescriptor dds = ddsList.get(index);
      DataFld fld = findKey(dds.getFxy());
      if ((fld != null) && (fld.index < 0)) {
        fld.index = index;
        fld.fldName = dds.getName();
        if (dds.getType() == 0) {
          fld.dtype = (dds.getScale() == 0) ? DataType.INT : DataType.FLOAT;
        } else {
          fld.dtype = DataType.STRING;
        }
      }
    }

    indexFlds = new ArrayList<Integer>(dataflds.size());
    for (DataFld fld : dataflds) {
      indexFlds.add(fld.index);
    }

    writeSchema();
    isInit = true;
  }

  private DataFld findKey(short fxy) {
    for (DataFld fld : dataflds) {
      if (fld.fxy == fxy) return fld;
    }
    return null;
  }

  private void writeSchema() throws FileNotFoundException {
    String filename = dirName + "/schema.txt";
    Formatter out = new Formatter(new FileOutputStream(filename));
    for (DataFld fld : dataflds) {
      out.format("%s,%s,%s,%d%n", fld.descName, fld.fldName, fld.dtype, fld.index);
    }
    out.close();
  }

  private void readSchema() throws IOException {
    String filename = dirName + "/schema.txt";
    File inputFile = new File(filename);
    if (!inputFile.exists()) return;

    BufferedReader dataIS = new BufferedReader(new InputStreamReader(new FileInputStream(inputFile)));
    while (true) {
      String line = dataIS.readLine();
      if (line == null) break;
      line = line.trim();
      if (line.length() == 0) break;
      if (line.charAt(0) == '#') continue;
      dataflds.add(new DataFld(line));
    }
  }
  /////////////////////////

  private void setup(String dirName, boolean readOnly) throws DatabaseException {
    EnvironmentConfig myEnvConfig = new EnvironmentConfig();
    StoreConfig storeConfig = new StoreConfig();

    myEnvConfig.setReadOnly(readOnly);
    storeConfig.setReadOnly(readOnly);

    myEnvConfig.setAllowCreate(!readOnly);
    storeConfig.setAllowCreate(!readOnly);

    myEnvConfig.setSharedCache(true);

    File dir = new File(dirName);
    dir.mkdirs();

    myEnv = new Environment(dir, myEnvConfig); // LOOK may want to try a single Environment

    // primary
    DatabaseConfig dbConfig = new DatabaseConfig();
    dbConfig.setAllowCreate(true);
    dbConfig.setDeferredWrite(true);
    database = myEnv.openDatabase(null, "primaryDatabase", dbConfig);

    // secondary
    SecondaryKeyCreator keyCreator = new SecondaryKeyCreator(new DataBinding());
    SecondaryConfig secConfig = new SecondaryConfig();
    secConfig.setAllowCreate(!readOnly);
    secConfig.setSortedDuplicates(!readOnly);
    secConfig.setKeyCreator(keyCreator);

    secondary = myEnv.openSecondaryDatabase(null, "secDatabase", database, secConfig);
  }

  // Close the store and environment
  public void close() {
    System.out.println("close Indexer=" + dirName);

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

  private class SecondaryKeyBinding extends TupleBinding {

    public void objectToEntry(Object object, TupleOutput to) {
    }

    public Object entryToObject(TupleInput ti) {
      MyData myData = new MyData();
      int nflds = dataflds.size() - nStndFlds;
      myData.flds = new Object[nflds];

      for (int i = nStndFlds; i < dataflds.size(); i++) {
        int fldidx = i - nStndFlds;
        DataFld fld = dataflds.get(i);
        if (fld.dtype == DataType.STRING) {
          myData.flds[fldidx] = ti.readString();
        } else if (fld.dtype == DataType.FLOAT)
          myData.flds[fldidx] = ti.readFloat();
        else if (fld.dtype == DataType.INT)
          myData.flds[fldidx] = ti.readInt();
      }

      myData.date = ti.readLong();
      return myData;
    }
  }

  private class SecondaryKeyCreator implements com.sleepycat.je.SecondaryKeyCreator {
    DataBinding dataBind;

    SecondaryKeyCreator(DataBinding dataBind) {
      this.dataBind = dataBind;
    }

    public boolean createSecondaryKey(SecondaryDatabase secondaryDatabase, DatabaseEntry key, DatabaseEntry dataEntry, DatabaseEntry result) throws DatabaseException {
      MyData mydata = (MyData) dataBind.entryToObject(dataEntry);
      TupleOutput to = new TupleOutput();

      // other fields go first
      for (int i = 0; i < mydata.flds.length; i++) {
        Object data = mydata.flds[i];
        //if (data == null) continue;  // LOOK

        if (data instanceof byte[]) {
          byte[] bdata = (byte[]) data;
          to.writeString(new String(bdata));
        } else if (data instanceof String) {
          to.writeString((String) data);
         } else if (data instanceof Float) {
          to.writeFloat((Float) data);
        } else if (data instanceof Integer) {
          to.writeInt((Integer) data);
        }
      }

      // then the time
      to.writeLong(mydata.date);
      result.setData(to.getBufferBytes(), 0, to.getBufferLength());
      //System.out.println("--createSecondaryKey on "+dirName);
      return true;
    }
  }

  // 8 bytes primary key
  private class PrimaryKey {
    short fileno, obsno;
    int pos;

    public String toString() {
      return fileno + ", " + pos + ", " + obsno;
    }
  }

  private class PrimaryKeyBinding extends TupleBinding {
    public void objectToEntry(Object object, TupleOutput to) {
      PrimaryKey myKey = (PrimaryKey) object;
      to.writeShort(myKey.fileno);
      to.writeInt(myKey.pos);
      to.writeShort(myKey.obsno);
    }

    public Object entryToObject(TupleInput ti) {
      PrimaryKey myKey = new PrimaryKey();
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
      out.format("%s", dateFormatter.toDateTimeStringISO(d));
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
        int year = ((Number) dataArray[0]).intValue();
        int month = (dataArray[1] == null) ? 1 : ((Number) dataArray[1]).intValue();
        int day = (dataArray[2] == null) ? 0 : ((Number) dataArray[2]).intValue();
        int hour = ((Number) dataArray[3]).intValue();
        int min = (dataArray[4] == null) ? 0 : ((Number) dataArray[4]).intValue();
        int sec = (dataArray[5] == null) ? 0 : ((Number) dataArray[5]).intValue();
        int doy = (dataArray[6] == null) ? -1 : ((Number) dataArray[6]).intValue();

        cal.clear();
        cal.set(year, month - 1, day, hour, min, sec);
        if (doy >= 0)
          cal.set(Calendar.DAY_OF_YEAR, doy);
        to.writeLong(cal.getTime().getTime());

        //System.out.printf("write %d, %d, %d, %d, %d == %s %n",  year, month, day, hour, min,
        //        dateFormatter.toDateTimeStringISO(cal.getTime()));

      } catch (Exception e) {
        System.out.println("Error on " + dirName);
        e.printStackTrace();
      }

      for (int i = nStndFlds; i < dataArray.length; i++) {
        Object data = dataArray[i];
        if (data == null) continue;

        if (data instanceof byte[]) {
          byte[] bdata = (byte[]) data;
          //to.writeFast((byte) bdata.length);
          //to.writeFast(bdata);
          to.writeString(new String(bdata));
        } else if (data instanceof Float) {
          to.writeFloat((Float) data);
        } else if (data instanceof Integer) {
          to.writeInt((Integer) data);
        }
      }
    }

    public Object entryToObject(TupleInput ti) {
      MyData myData = new MyData();
      myData.date = ti.readLong();
      myData.flds = new Object[dataflds.size() - nStndFlds];

      for (int i = nStndFlds; i < dataflds.size(); i++) {
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
    if (!isInit)
      setFldInfo(m);

    PrimaryKeyBinding keyBinding = new PrimaryKeyBinding();
    DataBinding dataBinding = new DataBinding();
    PrimaryKey myKey = new PrimaryKey();

    Object[][] result;
    try {
      result = m.readValues(indexFlds); // read specific data fields from message
    } catch (Exception e) {
      System.err.println("Fail writing index on " + m.getHeader());
      e.printStackTrace();
      return false;
    }

    int n = result.length;
    for (int i = 0; i < n; i++) {
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
        System.out.println("--index write error on " + dirName);
        throw new IOException(e);
      }
    }
    return true;
  }

  private void retrieve() throws DatabaseException {
    System.out.printf("primary dump on %s %n", dirName);
    TupleBinding keyBinding = new PrimaryKeyBinding();
    TupleBinding dataBinding = new DataBinding();

    Cursor myCursor = null;
    try {
      myCursor = database.openCursor(null, null);
      DatabaseEntry foundKey = new DatabaseEntry();
      DatabaseEntry foundData = new DatabaseEntry();

      int count = 0;
      while (myCursor.getNext(foundKey, foundData, LockMode.DEFAULT) == OperationStatus.SUCCESS) {
        PrimaryKey key = (PrimaryKey) keyBinding.entryToObject(foundKey);
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

  private void retrieve2(Formatter out) throws DatabaseException {
    System.out.printf("secondary dump on %s %n", dirName);
    TupleBinding keyBinding = new PrimaryKeyBinding();
    TupleBinding dataBinding = new DataBinding();
    TupleBinding secBinding = new SecondaryKeyBinding();

    SecondaryCursor secCursor = null;
    try {
      secCursor = secondary.openSecondaryCursor(null, null);
      DatabaseEntry secKeyEntry = new DatabaseEntry();
      DatabaseEntry primaryKeyEntry = new DatabaseEntry();
      DatabaseEntry foundDataEntry = new DatabaseEntry();

      int count = 0;
      OperationStatus retVal = secCursor.getFirst(secKeyEntry, primaryKeyEntry, foundDataEntry, LockMode.DEFAULT);
      while (retVal == OperationStatus.SUCCESS) {
        //MyData secKey = (MyData) secBinding.entryToObject(secKeyEntry);
        PrimaryKey key = (PrimaryKey) keyBinding.entryToObject(primaryKeyEntry);
        MyData data = (MyData) dataBinding.entryToObject(foundDataEntry);

        out.format("%d, %s, %s %n", count, key, data);
        retVal = secCursor.getNext(secKeyEntry, primaryKeyEntry, foundDataEntry,  LockMode.DEFAULT);
        count++;
        if (count % 1000 ==0) System.out.printf("count = %d %n", count);
      }

      System.out.printf("total = %d %n", count);
    } finally {
      if (null != secCursor)
        secCursor.close();
    }
  }

  private void showStats() {
    try {
      EnvironmentStats stats = myEnv.getStats(null);
      System.out.println("env stats= "+stats);

      DatabaseStats dstats = database.getStats(null);
      System.out.println("primary stats\n"+dstats);

      dstats = secondary.getStats(null);
      System.out.println("sec stats\n"+dstats);

    } catch (DatabaseException e) {
      e.printStackTrace();  
    }
  }

  static boolean showFlds = false;

  static public BerkeleyDBIndexer factory(String dir, String index) throws Exception {
    String[] tokes = index.trim().split(" ");
    List<Short> indexFields = new ArrayList<Short>(tokes.length);
    Formatter out = new Formatter(System.out);
    if (showFlds) out.format("BerkeleyDBIndexer dir= %s indexFlds= ", dir);
    for (String s : tokes) {
      if (s.length() == 0) continue;
      short fxy = Descriptor.getFxy("0-" + s);
      indexFields.add(fxy);
      if (showFlds) out.format("%s ", Descriptor.makeString(fxy));
    }
    if (showFlds) out.format("%n");

    return new BerkeleyDBIndexer(dir + "/bdb", indexFields);
  }

  public static void main(String args[]) throws Exception {
    BerkeleyDBIndexer indexer = new BerkeleyDBIndexer("D:/bufr/dispatch/fslprofilers/bdb");
    //indexer.showStats();
    //indexer.retrieve();   
    Formatter csv = new Formatter(new FileOutputStream("D:/bufr/out/testSecIndex.csv"));
    indexer.retrieve2(csv);
    csv.close();
    indexer.close();
  }


}
