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

package thredds.db;

import org.xtreemfs.babudb.BabuDB;
import org.xtreemfs.babudb.BabuDBException;
import org.xtreemfs.babudb.BabuDBFactory;
import org.xtreemfs.babudb.BabuDBInsertGroup;
import org.xtreemfs.babudb.log.DiskLogger;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.ft.*;
import ucar.ma2.StructureData;

import java.io.IOException;
import java.util.*;
import java.nio.ByteBuffer;

public class TestBabu {

  interface Closure {
    void process(byte[] key, byte[] value);
  }

  long testPointFeatureCollection(PointFeatureCollection pfc, Closure closure) throws IOException {
    ByteBuffer bb = ByteBuffer.allocate(8);
    long count = 0;
    pfc.resetIteration();
    while (pfc.hasNext()) {
      PointFeature pobs = pfc.next();
      StructureData sdata = pobs.getData();
      String stn = sdata.getScalarString("report_id"); // lame to use String
      int time = sdata.getScalarInt("time_observation");
      String report = sdata.getScalarString("report");

      bb.clear();
      byte[] stnb = stn.getBytes();
      bb.put(stnb, 0, 4);
      bb.putInt(time);
      byte[] key = bb.array();
      closure.process(key, report.getBytes());
      count++;
    }
    return count;
  }

  long testNestedPointFeatureCollection(NestedPointFeatureCollection npfc, Closure closure) throws IOException {
    long count = 0;
    PointFeatureCollectionIterator iter = npfc.getPointFeatureCollectionIterator(-1);
    while (iter.hasNext()) {
      PointFeatureCollection pfc = iter.next();
      count += testPointFeatureCollection(pfc, closure);
    }
    return count;
  }

  private void testPointDataset(String location, FeatureType type, Closure closure) throws IOException {
    long start = System.currentTimeMillis();
    long nrecs = 0;

    Formatter out = new Formatter();
    FeatureDataset fdataset = FeatureDatasetFactoryManager.open(type, location, null, out);
    if (fdataset == null) {
      System.out.printf("**failed on %s %n --> %s %n", location, out);
      return;
    }
    System.out.printf("*** Open %s as %s%n ", location, type);

    // FeatureDatasetPoint
    assert fdataset instanceof FeatureDatasetPoint;
    FeatureDatasetPoint fdpoint = (FeatureDatasetPoint) fdataset;

    for (FeatureCollection fc : fdpoint.getPointFeatureCollectionList()) {
      // PointFeatureCollection;
      assert (fc instanceof PointFeatureCollection) || (fc instanceof NestedPointFeatureCollection) : fc.getClass().getName();
      if (fc instanceof PointFeatureCollection) {
        PointFeatureCollection pfc = (PointFeatureCollection) fc;
        nrecs += testPointFeatureCollection(pfc, closure);
        System.out.printf(" getData size=%d%n ", pfc.size());
      } else {
        nrecs += testNestedPointFeatureCollection((NestedPointFeatureCollection) fc, closure);
      }
    }

    fdataset.close();
    long took = System.currentTimeMillis() - start;
    System.out.printf(" took=%d msecs to write %d records from %s%n ", took, nrecs, location);
  }

  private static final String dbname = "metars";
  private static final String location = "C:/tmp/db3/";

  BabuDB create() throws BabuDBException {
    /*
    * String baseDir, String dbLogDir, int numThreads,
         long maxLogfileSize, int checkInterval, SyncMode syncMode, int pseudoSyncWait,
         int maxQ
    *
    * @param baseDir directory in which the database snapshots are stored
    * @param dbLogDir directory in which the database append logs are stored (can be same as baseDir)
    * @param numThreads number of worker threads to use
    * @param maxLogfileSize a checkpoint is generated if  maxLogfileSize is exceeded
    * @param checkInterval interval between two checks in seconds, 0 disables auto checkpointing
    * @param syncMode the synchronization mode to use for the logfile
    * @param pseudoSyncWait if value > 0 then requests are immediateley aknowledged and synced to disk every
    *        pseudoSyncWait ms.
    * @param maxQ if > 0, the queue for each worker is limited to maxQ

    * @throws BabuDBException
    */
    BabuDB database = BabuDBFactory.getBabuDB(location, location + "log/", 2, 1024 * 1024 * 16, 5 * 60,
            DiskLogger.SyncMode.ASYNC, 0, 0);

    //create a new database
    database.createDatabase(dbname, 1);

    return database;
  }

  void populate(final BabuDB database, String location) {
    try {
      testPointDataset(location, FeatureType.POINT, new Closure() {
        public void process(byte[] key, byte[] value) {
          try {
            BabuDBInsertGroup ig = database.createInsertGroup(dbname);
            ig.addInsert(0, key, value);
            database.directInsert(ig);
          } catch (BabuDBException e) {
            throw new RuntimeException(e);
          }
        }
      });

      //create a checkpoint for faster start-ups
      database.checkpoint();

    /* } catch (BabuDBException ex) {
      ex.printStackTrace();  */

    } catch (IOException ex) {
      ex.printStackTrace();

    } /* catch (InterruptedException ex) {
      ex.printStackTrace();
    }  */

  }

  BabuDB open() throws BabuDBException {
    /**
     * String baseDir, String dbLogDir, int numThreads,
     long maxLogfileSize, int checkInterval, SyncMode syncMode, int pseudoSyncWait,
     int maxQ, List<InetSocketAddress> slaves, int port, SSLOptions ssl, int repMode, int qLimit
     * Starts the BabuDB database as Master (with Replication enabled).
     *
     * @param baseDir directory in which the datbase snapshots are stored
     * @param dbLogDir directory in which the database append logs are stored (can be same as baseDir)
     * @param numThreads number of worker threads to use
     * @param maxLogfileSize a checkpoint is generated if  maxLogfileSize is exceeded
     * @param checkInterval interval between two checks in seconds, 0 disables auto checkpointing
     * @param syncMode the synchronization mode to use for the logfile
     * @param pseudoSyncWait if value > 0 then requests are immediateley aknowledged and synced to disk every
     *        pseudoSyncWait ms.
     * @param maxQ if > 0, the queue for each worker is limited to maxQ
     * @param slaves hosts, where the replicas should be send to.
     * @param port where the application listens at. (use 0 for default configuration)
     * @param ssl if set SSL will be used while replication.
     * @param repMode <p>repMode == 0: asynchronous replication mode</br>
     *                   repMode == slaves.size(): synchronous replication mode</br>
     *                   repMode > 0 && repMode < slaves.size(): N -sync replication mode with N = repMode</p>
     * @param qLimit if > 0, the queue for the replication-requests is limited to qLimit
     *
     * @throws BabuDBException
     */

    return BabuDBFactory.getMasterBabuDB(location, location + "log/", 2, 1024 * 1024 * 16, 5 * 60,
            DiskLogger.SyncMode.ASYNC, 0, 0, new ArrayList(), 0, null, 0, 0);
  }

  void find(BabuDB db, String prefix) {
    try {
      System.out.printf(" key=%s%n", prefix);
      Iterator<Map.Entry<byte[], byte[]>> it = db.directPrefixLookup(dbname, 0, prefix.getBytes());
      while (it.hasNext()) {
        Map.Entry<byte[], byte[]> keyValuePair = it.next();
        ByteBuffer bb = ByteBuffer.wrap(keyValuePair.getKey());
        byte[] b = new byte[4];
        bb.get(b);
        String key = new String(b);
        long msecs = 1000 * (long) bb.getInt();
        Date date = new Date(msecs);
        byte[] v = keyValuePair.getValue();
        System.out.printf(" stn=%s time=%s value=%s (%d)%n", key, date, new String(v), v.length);
      }

    } catch (BabuDBException ex) {
      ex.printStackTrace();

    }

  }

  public static void main(String[] args) {
    try {
      TestBabu test = new TestBabu();
      BabuDB database = test.open();
      //test.populate(database, "D:/data/metars/Surface_METAR_20070509_0000.nc");
      test.find(database, "ZWWW");

      database.shutdown();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}

