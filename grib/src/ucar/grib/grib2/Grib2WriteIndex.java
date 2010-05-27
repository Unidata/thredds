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

package ucar.grib.grib2;

import ucar.unidata.io.RandomAccessFile;

import ucar.grid.GridIndex;
import ucar.grib.*;
import ucar.grib.grib1.Grib1GDSVariables;

import java.io.*;
import java.lang.*;
import java.util.*;

/**
 * Creates an index for a Grib2 file in a Binary format verses the old text format.
 *
 * @author Robb Kambic
 */
public class Grib2WriteIndex {

  private static boolean debugTiming = false;
  private static boolean verbose = false;

  /**
   * use to improve performance in Grib file
   */
  public static int indexRafBufferSize = 300;

  /**
   * Maintain index type, either text or binary on extend
   * when true cause double read of text index, so it's expensive
   * When false, converts text indexes to binary indexes
   */
  private static boolean maintainIndexType = true;

  public Grib2WriteIndex() {
  }

  /**
   * create a Grib file index; optionally create an in-memory index.
   *
   * @param grib      as a File
   * @param gribName  as a String
   * @param gbxName   as a String
   * @param makeIndex make an in-memory index if true
   * @return Index if makeIndex is true, else null
   * @throws IOException on gbx write
   */
  public final GridIndex writeGribIndex( File grib,
      String gribName, String gbxName, boolean makeIndex) throws IOException {

    RandomAccessFile raf = null;
    try {
      raf = new RandomAccessFile(gribName, "r");
      raf.order(RandomAccessFile.BIG_ENDIAN);
      return writeGribIndex(grib, gbxName, raf, makeIndex);
    } finally {
      if (raf != null)
        raf.close();
    }
  }

  /**
   * extend a Grib file index; optionally create an in-memory index.
   *
   * @param grib      as a File
   * @param gbxName   as a String
   * @param raf       RandomAccessFile
   * @param makeIndex make an in-memory index if true
   * @return Index if makeIndex is true, else null
   * @throws IOException on gbx write
   */
  public final GridIndex writeGribIndex(
      File grib,
      String gbxName, RandomAccessFile raf, boolean makeIndex) throws IOException {

    DataOutputStream out = null;
    boolean success;
    try {
      out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(gbxName, false)));
      success = writeGribIndex(raf, grib.lastModified(), out);
      // Since a new index isn't requested much in a write, just read the new index.
    } finally {
      if ( out != null ) {
        out.flush();
        out.close();
      }
    }
    if( success && makeIndex ) {
      try {
        Thread.sleep(2000); // 2 secs to let file system catch up
        return new GribReadIndex().open( gbxName );
      } catch (InterruptedException e1) {
      }
    }
    return null;
  }

  /**
   * Write a Grib file index.
   *
   * @param inputRaf        GRIB file raf
   * @param rafLastModified of the raf
   * @param out             where to write
   * @return success boolean
   * @throws IOException on gbx write
   */
  public final boolean writeGribIndex(
      RandomAccessFile inputRaf, long rafLastModified,
      DataOutputStream out) throws IOException {

    /**
     * date format "yyyy-MM-dd'T'HH:mm:ss'Z'".
     */
    java.text.SimpleDateFormat dateFormat = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
    dateFormat.setTimeZone(java.util.TimeZone.getTimeZone("GMT"));  //same as UTC

    Date now = Calendar.getInstance().getTime();
    if (debugTiming)
      System.out.println(now.toString() + " ... Start of Grib2WriteIndex");
    long start = System.currentTimeMillis();
    int count = 0;
    // set buffer size for performance
    int rafBufferSize = inputRaf.getBufferSize();
    inputRaf.setBufferSize(indexRafBufferSize);

    try {
      inputRaf.seek(0);
      // Create Grib2Input instance
      Grib2Input g2i = new Grib2Input(inputRaf);
      // params getProducts (implies  unique GDSs too), oneRecord
      g2i.scan(true, false);
      // write lastModified time of file 1st
      out.writeLong(rafLastModified);

      // Section 1 Global attributes
      StringBuilder sb = new StringBuilder();
      sb.append("index_version " + GridIndex.current_index_version);
      sb.append(" grid_edition " + 2);
      sb.append(" location " + inputRaf.getLocation().replaceAll(" ", "%20"));
      sb.append(" length " + inputRaf.length());
      sb.append(" created " + dateFormat.format(now));

      // section 2 grib records
      Date baseTime = null;
      List<Grib2Product> products = g2i.getProducts();
      for (int i = 0; i < products.size(); i++) {
        Grib2Product product = products.get(i);
        Grib2ProductDefinitionSection pds = product.getPDS();
        Grib2IdentificationSection id = product.getID();
        if (i == 0) {
          sb.append(" center " + id.getCenter_id());
          sb.append(" sub_center " + id.getSubcenter_id());
          sb.append(" table_version " + id.getLocal_table_version());
          baseTime = product.getBaseTime();
          sb.append(" basetime " + dateFormat.format(baseTime));
          sb.append(" ensemble ");
          sb.append((pds.getPdsVars().isEnsemble()) ? "true" : "false");
          out.writeUTF(sb.toString());

          // number of records
          out.writeInt(products.size());
          if (verbose)
            System.out.println("Index created with number records =" + products.size());
        }
        out.writeInt(product.getDiscipline());
        out.writeLong(product.getRefTime());
        out.writeInt(product.getGDSkeyInt());
        out.writeLong(product.getGdsOffset());
        out.writeLong(product.getPdsOffset());
        int length = pds.getPdsVars().getPDSBytes().length;
        out.writeInt(length);
        //out.writeInt(pds.getPdsVars().getLength());
        out.write(pds.getPdsVars().getPDSBytes(), 0, length);
        count++;
      }

      // section 3: GDSs in this File
      Map<String, Grib2GridDefinitionSection> gdsHM = g2i.getGDSs();

      java.util.Set<String> keys = gdsHM.keySet();
      out.writeInt(keys.size());
      for (String key : keys) {
        Grib2GridDefinitionSection gds = gdsHM.get(key);
        //out.writeInt( gds.getGdsVars().getLength() );
        int length = gds.getGdsVars().getGDSBytes().length;
        out.writeInt(length);
        out.write(gds.getGdsVars().getGDSBytes(), 0, length);
      }

      // Catch thrown errors from GribFile
    } catch (NotSupportedException noSupport) {
      System.err.println("NotSupportedException : " + noSupport);
      return false;
    } finally {
      //reset
      inputRaf.setBufferSize(rafBufferSize);
    }

    if (debugTiming)
      System.out.println(" " + count + " products took " + (System.currentTimeMillis() - start) + " msec");

    return true;
  }  // end writeGribIndex

  /**
   * extend a Grib file index; optionally create an in-memory index.
   *
   * @param grib      as a File
   * @param gbx       as a File
   * @param gribName  as a String
   * @param gbxName   as a String
   * @param makeIndex make an in-memory index if true
   * @return GridIndex if makeIndex is true, else null
   * @throws IOException on gbx write
   */
  public final GridIndex extendGribIndex(
      File grib, File gbx,
      String gribName, String gbxName, boolean makeIndex) throws IOException {

    RandomAccessFile raf = null;
    try {
      raf = new RandomAccessFile(gribName, "r");
      raf.order(RandomAccessFile.BIG_ENDIAN);
      return extendGribIndex(grib, gbx, gbxName, raf, makeIndex);
    } finally {
      if (raf != null)
        raf.close();
    }
  }

  /**
   * extend a Grib file index; optionally create an in-memory index.
   *
   * @param grib      as a File
   * @param gbx       as a File
   * @param gbxName   as a String
   * @param raf       RandomAccessFile
   * @param makeIndex make an in-memory index if true
   * @return GridIndex if makeIndex is true, else null
   * @throws IOException on gbx write
   */
  public final GridIndex extendGribIndex(
      File grib, File gbx,
      String gbxName, RandomAccessFile raf, boolean makeIndex) throws IOException {

    // create tmp  index file
    DataOutputStream out = null;
    boolean success = false;
    try {
      out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(gbxName + ".tmp", false)));
      success = extendGribIndex(gbxName, raf, grib.lastModified(), out);
    } finally {
      if (out != null) {
        out.flush();
        out.close();
      }
    }

    // failure or no new data in grib, return old gbx
    if (!success || out.size() == 8) {
      File tidx = new File(gbxName + ".tmp");
      tidx.delete();
      gbx.setLastModified(grib.lastModified() + 1000);
    } else {
      gbx.delete();
      File tidx = new File(gbxName + ".tmp");
      tidx.renameTo(gbx);
    }
    // Since a new index isn't requested much in a write, just read the new index.
    if( success && makeIndex ) {
          try {
            Thread.sleep(2000); // 2 secs to let file system catch up
            return new GribReadIndex().open(gbxName);
          } catch (InterruptedException e1) {
          }
      }
      return null;
  }

  /**
   * extend a Grib file index
   *
   * @param gbxName         a GridIndex is used to extend/create a new GridIndex
   * @param inputRaf        GRIB file raf
   * @param rafLastModified of the raf
   * @param out             where to write
   * @return Index if makeIndex is true, else null
   * @throws IOException on gbx write
   */
  public final boolean extendGribIndex(
      String gbxName, RandomAccessFile inputRaf,
      long rafLastModified, DataOutputStream out) throws IOException {

    /**
     * date format "yyyy-MM-dd'T'HH:mm:ss'Z'".
     */
    java.text.SimpleDateFormat dateFormat = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
    dateFormat.setTimeZone(java.util.TimeZone.getTimeZone("GMT"));  //same as UTC

    Date now = Calendar.getInstance().getTime();
    if (debugTiming)
      System.out.println(now.toString() + " ... Start of Grib2ExtendIndex");
    long start = System.currentTimeMillis();
    int count = 0;
    // set buffer size for performance
    int rafBufferSize = inputRaf.getBufferSize();
    inputRaf.setBufferSize(indexRafBufferSize);

    try {
      // get oldIndex in raw format
      List<RawRecord> recordList = new ArrayList<RawRecord>();
      Map<String, GribGDSVariablesIF> gdsMap = new HashMap<String, GribGDSVariablesIF>();
      boolean newversion = rawGridIndex(gbxName, recordList, gdsMap);
      if (newversion) {
        // Process old index to get where to start reading the new data
        RawRecord rr = recordList.get(recordList.size() - 1);
        inputRaf.seek(rr.offset2); // seek to where last index left off
      } else {
        inputRaf.seek(0L); // start at beginning of file
      }
      // Create Grib2Input instance
      Grib2Input g2i = new Grib2Input(inputRaf);
      // params getProducts (implies  unique GDSs too), oneRecord
      g2i.scan(true, false);
      // write rafLastModified 1st
      out.writeLong(rafLastModified);

      // Section 1 Global attributes
      StringBuilder sb = new StringBuilder();
      sb.append("index_version " + GridIndex.current_index_version);
      sb.append(" grid_edition " + 2);
      sb.append(" location " + inputRaf.getLocation().replaceAll(" ", "%20"));
      sb.append(" length " + inputRaf.length());
      sb.append(" created " + dateFormat.format(now));

      // section 2 grib records
      Date baseTime = null;
      List<Grib2Product> products = g2i.getProducts();
      // no new data, just return
      if (products.size() == 0) {
        return false;
      }
      for (int i = 0; i < products.size(); i++) {
        Grib2Product product = products.get(i);
        Grib2ProductDefinitionSection pds = product.getPDS();
        Grib2IdentificationSection id = product.getID();
        if (i == 0) {
          sb.append(" center " + id.getCenter_id());
          sb.append(" sub_center " + id.getSubcenter_id());
          sb.append(" table_version " + id.getLocal_table_version());
          baseTime = product.getBaseTime();
          sb.append(" basetime " + dateFormat.format(baseTime));
          sb.append(" ensemble ");
          sb.append((pds.getPdsVars().isEnsemble()) ? "true" : "false");
          out.writeUTF(sb.toString());

          // number of records
          out.writeInt(products.size() + recordList.size());
          if (verbose)
            System.out.println("Index extended with old new records " +
                recordList.size() + "  " + products.size());
          // need to write out old index
          for (RawRecord raw : recordList) {
            out.writeInt(raw.discipline);
            out.writeLong(raw.refTime);
            out.writeInt(raw.gdsKey);
            out.writeLong(raw.offset1);
            out.writeLong(raw.offset2);
            out.writeInt(raw.pdsSize);
            out.write(raw.pdsData, 0, raw.pdsSize);
          }
          count = recordList.size();
        }
        out.writeInt(product.getDiscipline());
        out.writeLong(product.getRefTime());
        out.writeInt(product.getGDSkeyInt());
        out.writeLong(product.getGdsOffset());
        out.writeLong(product.getPdsOffset());
        int length = pds.getPdsVars().getPDSBytes().length;
        out.writeInt(length);
        //out.writeInt(pds.getPdsVars().getLength());
        out.write(pds.getPdsVars().getPDSBytes(), 0, length);

        count++;
      }

      // section 3: GDSs in this File
      Map<String, Grib2GridDefinitionSection> gdsHM = g2i.getGDSs();
      java.util.Set<String> keysHM = gdsHM.keySet();
      for (String key : keysHM) {
        Grib2GridDefinitionSection gds = gdsHM.get(key);
        if (!gdsMap.containsKey(key)) {
          gdsMap.put(key, gds.getGdsVars());
        }
      }

      out.writeInt(gdsMap.size());
      java.util.Set<String> keyAll = gdsMap.keySet();
      for (String key : keyAll) {
        GribGDSVariablesIF gdsv = gdsMap.get(key);
        int length = gdsv.getGDSBytes().length;
        out.writeInt(length);
        out.write(gdsv.getGDSBytes(), 0, length);
      }

      // Catch thrown errors from GribFile
    } catch (NotSupportedException noSupport) {
      System.err.println("NotSupportedException : " + noSupport);
      return false;
    } finally {
      //reset
      inputRaf.setBufferSize(rafBufferSize);
    }

    if (debugTiming)
      System.out.println(" " + count + " products took " + (System.currentTimeMillis() - start) + " msec");

    return true;
  }  // end extendGribIndex

  /**
   * makes a raw representation of GridIndex
   *
   * @param gbxName    String
   * @param recordList List
   * @param gdsMap     Map
   * @throws IOException on oldIndex read
   */
  public boolean rawGridIndex(
      String gbxName, List<RawRecord> recordList, Map<String, GribGDSVariablesIF> gdsMap) throws IOException {

    DataInputStream dis = null;
    try {
      dis = new DataInputStream(new BufferedInputStream(new FileInputStream(gbxName)));

      // read lastModified time
      dis.readLong();
      // read Global attributes
      String attributes = dis.readUTF();
      // check for older binary index
      if (attributes.contains("index_version 7"))
        return false;
      boolean grid_edition_2 = attributes.contains("grid_edition 2") ? true : false;
      int number = dis.readInt();
      for (int i = 0; i < number; i++) {
        RawRecord rr = new RawRecord();
        rr.discipline = dis.readInt();
        rr.refTime = dis.readLong();
        rr.gdsKey = dis.readInt();
        rr.offset1 = dis.readLong();
        rr.offset2 = dis.readLong();
        // read PDS data
        rr.pdsSize = dis.readInt();
        rr.pdsData = new byte[rr.pdsSize];
        dis.read(rr.pdsData);
        recordList.add(rr);
      }

      // section 3+ - GDS
      number = dis.readInt();
      for (int j = 0; j < number; j++) {
        int gdsSize = dis.readInt();
        if (gdsSize == 4 ) { //records with no GDS,just gdsKey
          recordList.removeAll( recordList );
          return false;
        }
        byte[] gdsData = new byte[gdsSize];
        dis.read(gdsData);
        if (grid_edition_2) {
          Grib2GDSVariables gdsv = new Grib2GDSVariables(gdsData);
          gdsMap.put(Integer.toString(gdsv.getGdsKey()), gdsv);
        } else {
          Grib1GDSVariables gdsv = new Grib1GDSVariables(gdsData);
          gdsMap.put(Integer.toString(gdsv.getGdsKey()), gdsv);
        }
      }
    } finally {
      if (dis != null)
        dis.close();
    }
    return true;
  }

  /**
   * Dumps usage of the class.
   *
   * @param className Grib2WriteIndex
   */
  private static void usage(String className) {
    System.out.println();
    System.out.println("Usage of " + className + ":");
    System.out.println("Parameters:");
    System.out.println("<GribFileToRead> scans for index creation");
    System.out.println(
        "<IndexFile.idx> where to write index, default name + "+ GribIndexName.currentSuffix);
    System.out.println();
    System.out.println("java " + className
        + " <GribFileToRead> <IndexFile>");
    System.exit(0);
  }

  public void setDebug(boolean flag) {
    debugTiming = flag;
  }

  public void setVerbose(boolean flag) {
    verbose = flag;
  }

  /**
   * creates a Grib2 index for given Grib2 file.
   *
   * @param args 2 if Grib file and index file name given
   * @throws IOException on gbx write
   */
  public static void main(String args[]) throws IOException {

    Grib2WriteIndex indexer = new Grib2WriteIndex();
    debugTiming = true;

    // Test usage
    if (args.length < 1) {
      // Get class name as String
      Class cl = indexer.getClass();
      usage(cl.getName());
      System.exit(0);
    }

    //RandomAccessFile raf = null;
    String gribName = args[0];
    long rafLastModified;

    File grib = new File(args[0]);
    rafLastModified = grib.lastModified();

    String gbxName = GribIndexName.getCurrentSuffix( gribName );;
    if (args.length == 2) {  // input file and index file name given
      File gbx = new File(gbxName);
      if (gbx.exists() && gbx.lastModified() > rafLastModified) { // index > raf
        return;
      } else if (gbx.exists()) {
        indexer.extendGribIndex(grib, gbx, gribName, gbxName, false);
      } else {
        indexer.writeGribIndex(grib, gribName, gbxName, false);
      }

    } else if (args.length == 1) {
      indexer.writeGribIndex(grib, gribName, gbxName, false);
    }
  }

  /**
   * light weight record representation for extendIndex
   */
  public class RawRecord {

    public int discipline;

    public long refTime;

    public int gdsKey;

    public long offset1;

    public long offset2;

    public int pdsSize;

    public byte[] pdsData;

    /**
     * stores the information of a record in raw format
     */
    public void RawRecord() {
    }

    public void RawRecord(
        int discipline, long refTime, int gdsKey, long offset1,
        long offset2, int pdsSize, byte[] pdsData) {
      this.discipline = discipline;
      this.refTime = refTime;
      this.gdsKey = gdsKey;
      this.offset1 = offset1;
      this.offset2 = offset2;
      this.pdsSize = pdsSize;
      this.pdsData = pdsData;

    }
  }
}  // end Grib2WriteIndex
