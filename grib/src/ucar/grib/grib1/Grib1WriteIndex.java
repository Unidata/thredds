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

import ucar.grib.*;
import ucar.grib.grib2.Grib2WriteIndex;
import ucar.grid.GridIndex;
import ucar.unidata.io.RandomAccessFile;

import java.io.*;
import java.util.*;
import java.util.zip.CRC32;

/**
 * Creates an index for a Grib1 file; optionally makes an in-memory one.
 * @author  Robb Kambic
 */
public class Grib1WriteIndex {

  private static boolean debugTiming = false;
  private static boolean verbose = false;
  /*
   * set true to check for duplicate records in file by comparing PDSs
   */
  private static boolean checkPDS = false;
  /*
   *  Control the type of duplicate record logging
   */
  private static Grib2WriteIndex.pdsLogType logPDS = Grib2WriteIndex.pdsLogType.logger;

  private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger( getClass() );
  ////////////////////////////////////////////////////////////////////
  public Grib1WriteIndex() {
  }

  /**
   * create a Grib1 file index.
   *
   * @param grib  as File handle
   * @param gribName  gribName
   * @param gbxName   gbxName
   * @param makeIndex make an in-memory index if true
   * @return Index if makeIndex is true, else null
   * @throws IOException  on Grib read
   */
  public final GridIndex writeGribIndex(File grib, String gribName, String gbxName, boolean makeIndex) throws IOException {

    RandomAccessFile raf = null;
    // default from standalone indexer, check for duplicate records and log to System.out
    checkPDS = true;
    logPDS = Grib2WriteIndex.pdsLogType.systemout;
    try {
      raf = new RandomAccessFile(gribName, "r");
      raf.order(RandomAccessFile.BIG_ENDIAN);
      return  writeGribIndex( grib, gbxName, raf, makeIndex);
    } finally {
      if (raf != null)
        raf.close();
    }
  }

  /**
   * extend a Grib file index; optionally create an in-memory index.
   *
   * @param grib  Grib file
   * @param gbxName Index name
   * @param raf  RandomAccessFile
   * @param makeIndex make an in-memory index if true
   * @return Index if makeIndex is true, else null
   * @throws IOException  on Grib file read
   */

  public final GridIndex writeGribIndex(File grib, String gbxName, RandomAccessFile raf, boolean makeIndex) throws IOException {

    DataOutputStream out = null;
    boolean success;
    try {
      out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(gbxName, false)));
      success = writeGribIndex(raf, grib.lastModified(), out );
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
        return new GribIndexReader().open( gbxName );
      } catch (InterruptedException e1) {
      }
    }
    return null;
  }

  /**
   * Write a Grib file index.
   *
   * @param inputRaf  GRIB file raf
   * @param rafLastModified long
   * @param out DataOutputStream
   * @return success boolean.
   * @throws java.io.IOException on Grib file read
   */
  public final boolean writeGribIndex(RandomAccessFile inputRaf, long rafLastModified, DataOutputStream out )
    throws IOException {

    /**
     * date format "yyyy-MM-dd'T'HH:mm:ss'Z'".
     */
    java.text.SimpleDateFormat dateFormat = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
    dateFormat.setTimeZone(java.util.TimeZone.getTimeZone("GMT"));  //same as UTC

    Date now = new Date();
    if (debugTiming)
      System.out.println(now.toString() + " ... Start of Grib1WriteIndex");
    long start = System.currentTimeMillis();
    int count = 0;
    int numberDups = 0;
    // set buffer size for performance
    int rafBufferSize = inputRaf.getBufferSize();
    inputRaf.setBufferSize( Grib2WriteIndex.indexRafBufferSize );

    try {
      inputRaf.seek(0);
      // Create Grib1Input instance
      Grib1Input g1i = new Grib1Input(inputRaf);
      // params getProducts (implies  unique GDSs too), oneRecord
      g1i.scan(true, false);

      // write lastModified time of file 1st
      out.writeLong(rafLastModified);

      // Section 1 Global attributes
      StringBuilder sb = new StringBuilder();
      sb.append("index_version " + GridIndex.current_index_version);
      sb.append(" grid_edition " + 1);
      sb.append(" location " + inputRaf.getLocation().replaceAll( " ", "%20"));
      sb.append(" length " + inputRaf.length());
      sb.append(" created " + dateFormat.format(now));

      // section 2 grib records
      ArrayList<Grib1Product> products = g1i.getProducts();

      // check all the products for duplicates by comparing PDSs
      if( checkPDS ) {
        HashMap<String, Integer> pdsMap = new HashMap<String, Integer>();
        ArrayList<Integer> duplicate = new ArrayList<Integer>();
        CRC32 csc32 = new CRC32();
        for (int i = 0; i < products.size(); i++) {
          Grib1Product product = products.get( i );
          csc32.reset();
          csc32.update(product.getPDS().getPdsVars().getPDSBytes());
          String csc = Long.toString( csc32.getValue() );
          if ( verbose )
            System.out.println(  "csc32="+ csc );
          // duplicate found
          if ( pdsMap.containsKey( csc )) {
            StringBuilder str = new StringBuilder();
            str.append( "Duplicate " );
            str.append( product.getHeader() );
            // keep products if PDS don't match
            if ( check2Products( inputRaf, products.get( pdsMap.get( csc ) ), product, str)) {
              duplicate.add( i );
              str.append( " at file position "+ i +" verses "+ pdsMap.get( csc ));
              if ( logPDS.equals( Grib2WriteIndex.pdsLogType.systemout ))
                System.out.println( str.toString() );
              else if ( logPDS.equals( Grib2WriteIndex.pdsLogType.logger ))
                log.info( str.toString());
            }
          } else {
            pdsMap.put( csc, i);
          }
        }
        if( duplicate.size() > 0 ) {
          numberDups = duplicate.size();
          Collections.sort(duplicate, new CompareKeyDescend());
          // remove duplicates from products, highest first
          for( int idx : duplicate ) {
            products.remove( idx );
          }
        }
      }
      for (int i = 0; i < products.size(); i++) {
        Grib1Product product = products.get(i);
        Grib1ProductDefinitionSection pds = product.getPDS();
        Grib1Pds pdsv = pds.getPdsVars();
        if (i == 0) {
          sb.append(" center " + pdsv.getCenter());
          sb.append(" sub_center " + pdsv.getSubCenter());
          sb.append(" table_version " + pdsv.getParameterTableVersion());
          sb.append(" basetime " + dateFormat.format(pdsv.getReferenceDate()));
          sb.append(" ensemble ");
          sb.append( (pdsv.isEnsemble()) ? "true" : "false");
          out.writeUTF(sb.toString());

          // number of records
          out.writeInt(products.size());
          if( verbose )
            System.out.println( "Index created with number records ="+ products.size());
        }
        out.writeInt(product.getDiscipline());
        out.writeLong(pdsv.getReferenceTime());
        out.writeInt(product.getGDSkeyInt());
        out.writeLong(product.getOffset1());
        out.writeLong(product.getOffset2());
        out.writeInt(pdsv.getLength());
        out.write(pdsv.getPDSBytes(), 0, pdsv.getLength());
        count++;
      }

      // section 3: GDSs in this File
      HashMap<String, Grib1GridDefinitionSection> gdsHM = g1i.getGDSs();

      java.util.Set<String> keys = gdsHM.keySet();
      out.writeInt( keys.size() );
      for ( String key : keys) {
        Grib1GridDefinitionSection gds = gdsHM.get(key);
        if (gds.getGdsVars() == null ) { //no GDS in record 
          out.writeInt( 4 );
          out.writeInt( Integer.parseInt( key ) );
          continue;
        }  
        //out.writeInt( gds.getGdsVars().getLength() );
        int length = gds.getGdsVars().getGDSBytes().length;
        out.writeInt(length);
        out.write(gds.getGdsVars().getGDSBytes(), 0, length);
      }

      // Catch thrown errors from GribFile
    } catch (NotSupportedException noSupport) {
      System.err.println("NotSupportedException : " + noSupport);
      return false;
    } catch (NoValidGribException noValid) {
      System.err.println("NoValidGribException : " + noValid);
      return false;
    } finally {
      //reset
      inputRaf.setBufferSize( rafBufferSize );
    }

    if (debugTiming)
      System.out.println(" " + count + " products took " + (System.currentTimeMillis() - start) + " msec");
    if( numberDups > 0 ) {
      count += numberDups;
      System.out.println( " has Percentage of duplicates "+
          (int)((((double)numberDups/(double)count) * 100) +.5)
          +"% duplicates ="+ numberDups +" out of "+ count +" records." );
    }

    return true;
  }  // end writeGribIndex

  /**
   * extend a Grib file index; optionally create an in-memory index.
   *
   * @param grib as a File
   * @param gbx as a File
   * @param gribName as a String
   * @param gbxName as a String
   * @param makeIndex make an in-memory index if true
   * @return GridIndex if makeIndex is true, else null
   * @throws IOException on gbx write
   */
  public final GridIndex extendGribIndex(File grib, File gbx,
    String gribName, String gbxName, boolean makeIndex) throws IOException {

    RandomAccessFile raf = null;
    // default from standalone indexer, check for duplicate records and log to System.out
    checkPDS = true;
    logPDS = Grib2WriteIndex.pdsLogType.systemout;
    try {
      raf = new RandomAccessFile(gribName, "r");
      raf.order(RandomAccessFile.BIG_ENDIAN);
      return extendGribIndex( grib,  gbx, gbxName, raf,  makeIndex);
    } finally {
      if (raf != null)
        raf.close();
    }
  }

  /**
   * extend a Grib file index; optionally create an in-memory index.
   *
   * @param grib as a File
   * @param gbx as a File
   * @param gbxName as a String
   * @param raf RandomAccessFile
   * @param makeIndex make an in-memory index if true
   * @return GridIndex if makeIndex is true, else null
   * @throws IOException  gbx write
   */

  public final GridIndex extendGribIndex(File grib, File gbx, String gbxName, RandomAccessFile raf, boolean makeIndex) throws IOException {

    // create tmp  index file
    DataOutputStream out = null;
    boolean success = false;
    try {
      out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(gbxName + ".tmp", false)));
      success = extendGribIndex(gbxName, raf, grib.lastModified(), out );
    } finally {
      if (out != null ) {
          out.flush();
          out.close();
      }
    }

    // failure or no new data in grib, return old gbx
    if( ! success || out.size() == 8) {
      File tidx = new File(gbxName + ".tmp");
      tidx.delete();
      gbx.setLastModified( grib.lastModified() + 1000 );
    } else {
      gbx.delete();
      File tidx = new File(gbxName + ".tmp");
      tidx.renameTo(gbx);
    }
    // Since a new index isn't requested much in a write, just read the new index.
    if( success && makeIndex ) {
      try {
        Thread.sleep(2000); // 2 secs to let file system catch up
        return new GribIndexReader().open( gbxName );
      } catch (InterruptedException e1) {
      }
    }
    return null;

  }

  /**
   * extend a Grib file index; optionally create an in-memory index.
   *
   * @param gbxName  a GridIndex is used to extend/create a new GridIndex
   * @param inputRaf  GRIB file raf
   * @param rafLastModified of the raf
   * @param out where to write
   * @return Index if makeIndex is true, else null
   * @throws IOException on gbx write
   */
  public final boolean extendGribIndex(String gbxName, RandomAccessFile inputRaf,
    long rafLastModified, DataOutputStream out ) throws IOException {

    /**
     * date format "yyyy-MM-dd'T'HH:mm:ss'Z'".
     */
    java.text.SimpleDateFormat dateFormat = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
    dateFormat.setTimeZone(java.util.TimeZone.getTimeZone("GMT"));  //same as UTC

    Date now = new Date();
    if (debugTiming)
      System.out.println(now.toString() + " ... Start of Grib2ExtendIndex");
    long start = System.currentTimeMillis();
    int count = 0;
    int numberDups = 0;
    // set buffer size for performance
    int rafBufferSize = inputRaf.getBufferSize();
    inputRaf.setBufferSize( Grib2WriteIndex.indexRafBufferSize );

    try {
      // get oldIndex in raw format, Grib2 and Grib1 raw format are the same
      List<Grib2WriteIndex.RawRecord> recordList = new ArrayList<Grib2WriteIndex.RawRecord>();
      Map<String, GribGDSVariablesIF> gdsMap = new HashMap<String, GribGDSVariablesIF>();
      boolean newversion = new Grib2WriteIndex().rawGridIndex( gbxName, recordList, gdsMap );
      if( newversion ) {
        // Process old index to get where to start reading the new data
        Grib2WriteIndex.RawRecord rr = recordList.get(recordList.size() - 1);
        inputRaf.seek(rr.offset2); // seek to where last index left off
      } else {
        inputRaf.seek( 0L ); // start at beginning of file
      }
      // Create Grib2Input instance
      Grib1Input g1i = new Grib1Input(inputRaf);
      // params getProducts (implies  unique GDSs too), oneRecord
      g1i.scan(true, false);
      // write rafLastModified 1st
      out.writeLong(rafLastModified);

      // Section 1 Global attributes
      StringBuilder sb = new StringBuilder();
      sb.append("index_version " + GridIndex.current_index_version);
      sb.append(" grid_edition " + 1);
      sb.append(" location " + inputRaf.getLocation().replaceAll( " ", "%20"));
      sb.append(" length " + inputRaf.length());
      sb.append(" created " + dateFormat.format(now));

      // section 2 grib records
      List<Grib1Product> products = g1i.getProducts();
      // no new data, just return
      if (products.size() == 0) {
        return false;
      }

      // check all the products for duplicates by comparing PDSs
      if( checkPDS ) {
        HashMap<String, Integer> pdsMap = new HashMap<String, Integer>();
        ArrayList<Integer> duplicate = new ArrayList<Integer>();
        CRC32 csc32 = new CRC32();
        // initialize pdsMap with already indexed records
        int originalSize = recordList.size();
        for (int i = 0; i < recordList.size(); i++) {
          Grib2WriteIndex.RawRecord rr = recordList.get( i );
          csc32.reset();
          csc32.update( rr.pdsData );
          String csc = Long.toString( csc32.getValue() );
          pdsMap.put( csc, i );
        }
        // now check new records for duplicates, assumes original index has no duplicates
        for (int i = 0; i < products.size(); i++) {
          Grib1Product product = products.get( i );
          csc32.reset();
          csc32.update(product.getPDS().getPdsVars().getPDSBytes());
          String csc = Long.toString( csc32.getValue() );
          if ( verbose )
            System.out.println(  "csc32="+ csc );
          // duplicate found
          if ( pdsMap.containsKey( csc )) {
            StringBuilder str = new StringBuilder();
            str.append( "Duplicate " );
            str.append( product.getHeader() );
            // keep products if PDS don't match
            int idx = pdsMap.get( csc );
            boolean pdsMatch;
            if ( idx < originalSize )
              pdsMatch = checkRawRecordProduct( inputRaf, recordList.get( idx ), product, str);
            else
             pdsMatch = check2Products( inputRaf, products.get( idx-originalSize ), product,str);

            if ( pdsMatch ) {
              duplicate.add( i );
              str.append( " at file position "+ (i + originalSize) +" verses "+ idx);
              if ( logPDS.equals( Grib2WriteIndex.pdsLogType.systemout ))
                System.out.println( str.toString() );
              else if ( logPDS.equals( Grib2WriteIndex.pdsLogType.logger ))
                log.info( str.toString());
            }  
          } else {
            pdsMap.put( csc, i + originalSize );
          }
        }
        if( duplicate.size() > 0 ) {
          numberDups = duplicate.size();
          Collections.sort(duplicate, new CompareKeyDescend());
          // remove duplicates from products, highest first
          for( int idx : duplicate ) {
            products.remove( idx );
          }
        }
        // no new data, just return
        if (products.size() == 0) {
          return false;
        }
      }
      for (int i = 0; i < products.size(); i++) {
        Grib1Product product = products.get(i);
        Grib1ProductDefinitionSection pds = product.getPDS();
        Grib1Pds pdsv = pds.getPdsVars();
        if (i == 0) {
          sb.append(" center " + pdsv.getCenter());
          sb.append(" sub_center " + pdsv.getSubCenter());
          sb.append(" table_version " + pdsv.getParameterTableVersion());
          sb.append(" basetime " + dateFormat.format(pdsv.getReferenceDate()));
          sb.append(" ensemble ");
          sb.append( (pdsv.isEnsemble()) ? "true" : "false");
          out.writeUTF(sb.toString());

          // number of records
          out.writeInt(products.size() + recordList.size());
          if (verbose)
            System.out.println("Index extended with old new records " +
                recordList.size() + "  " + products.size());
          // need to write out old index
          for( Grib2WriteIndex.RawRecord raw : recordList ) {
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
        out.writeLong(pdsv.getReferenceTime());
        out.writeInt(product.getGDSkeyInt());
        out.writeLong(product.getOffset1());
        out.writeLong(product.getOffset2());
        out.writeInt(pdsv.getLength());
        out.write(pdsv.getPDSBytes(), 0, pdsv.getLength());
        count++;
      }

      // section 3: GDSs in this File
      // need to get oldIndex gds info
      Map<String, Grib1GridDefinitionSection> gdsHM = g1i.getGDSs();
      java.util.Set<String> keysHM = gdsHM.keySet();
      for (String key : keysHM) {
        Grib1GridDefinitionSection gds = gdsHM.get(key);
        if (!gdsMap.containsKey(key)) {
          gdsMap.put(key, gds.getGdsVars());
        }
      }

      out.writeInt(gdsMap.size());
      java.util.Set<String> keyAll = gdsMap.keySet();
      for (String key : keyAll) {
        GribGDSVariablesIF gdsv = gdsMap.get(key);
        if (gdsv == null ) { //no GDS in record 
          out.writeInt( 4 );
          out.writeInt( Integer.parseInt( key ) );
          continue;
        }
        int length = gdsv.getGDSBytes().length;
        out.writeInt(length);
        out.write(gdsv.getGDSBytes(), 0, length);
      }
      
      // Catch thrown errors from GribFile
    } catch (NotSupportedException noSupport) {
      System.err.println("NotSupportedException : " + noSupport);
    } catch (NoValidGribException noValid) {
      System.err.println("NoValidGribException : " + noValid);

    } finally {
      //reset
      inputRaf.setBufferSize( rafBufferSize );
    }

    if (debugTiming)
      System.out.println(" " + count + " products took " + (System.currentTimeMillis() - start) + " msec");
    if( numberDups > 0 ) {
      count += numberDups;
      System.out.println( " has Percentage of duplicates "+
          (int)((((double)numberDups/(double)count) * 100) +.5)
          +"% duplicates ="+ numberDups +" out of "+ count +" records." );
    }
    return true;
  }  // end extendGribIndex

  /*
    Check for a raw record object and product object has equal pds's and same data.
   */
  private boolean checkRawRecordProduct( RandomAccessFile inputRaf, Grib2WriteIndex.RawRecord raw, Grib1Product product,
                    StringBuilder str ) throws IOException  {

    Grib1Pds pdsv1 = new Grib1Pds( raw.pdsData );
    Grib1Pds pdsv2 = product.getPDS().getPdsVars();

    return checkPdsAndData( inputRaf, raw.offset1, raw.offset2, pdsv1,
      product.getOffset1(), product.getOffset2(),  pdsv2, str );
  }
  /*
    Check for 2 product objects has equal pds's and same data.
   */
  private boolean check2Products( RandomAccessFile inputRaf, Grib1Product product1, Grib1Product product2,
                    StringBuilder str ) throws IOException {

    Grib1Pds pdsv1 = product1.getPDS().getPdsVars();
    Grib1Pds pdsv2 = product2.getPDS().getPdsVars();

    return checkPdsAndData( inputRaf,
      product1.getOffset1(), product1.getOffset2(), pdsv1,
      product2.getOffset1(), product2.getOffset2(),  pdsv2, str );
  }
  /*
    Check for equal pds's and same data.
   */
  private boolean checkPdsAndData( RandomAccessFile inputRaf,
    long p1Offset1, long p1Offset2, Grib1Pds pdsv1,
    long p2Offset1, long p2Offset2, Grib1Pds pdsv2, StringBuilder str )
    throws IOException {

    byte[] pds1 = pdsv1.getPDSBytes();
    byte[] pds2 = pdsv2.getPDSBytes();

    // check pds bytes are the same
    boolean same = true;
    if ( pds1.length != pds2.length)
      same = false;
    else
      for( int j = 0; j < pds1.length; j++ )
        if( pds1[ j ] != pds2[ j ]) {
          same = false;
          break;
        }
    if ( ! same ) {
      str.append( " and PDSs didn't match" );
      return false;
    }
    // Add parameter information
    /*
    str.append( " Center " );
    str.append( pdsv1.getCenter() );
    str.append( " Sub-Center " );
    str.append( pdsv1.getSubCenter() );
    str.append( " Version " );
    str.append( pdsv1.getParameterTableVersion()  );
    */
    str.append( " Parameter " );
    str.append( pdsv1.getParameterNumber() );
    str.append( " Level1 ");
    str.append( pdsv1.getLevelType1() );
    str.append( " value ");
    str.append( pdsv1.getLevelValue1() );
    str.append( " valid at ");
    str.append( pdsv1.getForecastTime() );
    
    // check if the data is the same
    Grib1Data g1d = new Grib1Data( inputRaf );
    float[] data1 = g1d.getData
      ( p1Offset1, p1Offset2, pdsv1.getDecimalScale(), pdsv1.bmsExists());
    float[] data2 = g1d.getData
      ( p2Offset1, p2Offset2, pdsv2.getDecimalScale(), pdsv2.bmsExists());
    boolean datasame = true;
    for ( int i = 0; i < data1.length; i++ ) {
      if ( data1[ i ] != data2[ i ]) {
        if( Float.valueOf( data1[ i ]).isNaN() && Float.valueOf( data2[ i ]).isNaN() )
          continue;
        datasame = false;
        break;
      }
    }
    if (! datasame )
      str.append( " and Data didn't match");

    return same;
  }

  /*
   * set the duplicate record checking flag
   */
  public void setCheckPDS( boolean flag ) {
    checkPDS = flag;
  }

  /*
   * set the logging type for duplicate record checking
   */
  public void setLogPDS( Grib2WriteIndex.pdsLogType flag ) {
    logPDS = flag;
  }

  public void setDebug( boolean flag ) {
    debugTiming = flag;
  }

  public void setVerbose( boolean flag ) {
    verbose = flag;
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
        "<IndexFile.idx> where to write index, default STDOUT");
    System.out.println();
    System.out.println("java " + className
        + " <GribFileToRead> <IndexFile>");
    System.exit(0);
  }

  /**
   * creates a Grib1 index for given Grib1 file.
   *
   * @param args 2 if Grib file and index file name given
   * @throws IOException  on raf read
   */
  public static void main(String args[]) throws IOException {

    Grib1WriteIndex indexer = new Grib1WriteIndex();
    debugTiming = false;

    // Test usage
    if (args.length < 1) {
      // Get class name as String
      Class cl = indexer.getClass();
      usage(cl.getName());
      System.exit(0);
    }

    String gribName = args[0];
    long rafLastModified;

    File grib = new File(args[0]);
    rafLastModified = grib.lastModified();

    String gbxName = GribIndexName.getCurrentSuffix( gribName );
    if (args.length == 2) {  // input file and index file name given
      File gbx = new File(gbxName);
      if (gbx.exists() && gbx.lastModified() > rafLastModified) { // index > raf
        return;
      } else if (gbx.exists()) {
        indexer.extendGribIndex( grib, gbx,  gribName,  gbxName, false );
      } else {  // create index
        indexer.writeGribIndex(grib, gribName, gbxName, false);
      }
    } else if (args.length == 1) {
      indexer.writeGribIndex(grib, gribName, gbxName , false);
    }
  }

  /*
   * Used to sort duplicate record numbers
   */
  protected class CompareKeyDescend implements Comparator<Object> {

    public int compare(Object o1, Object o2) {
      int i1 = (Integer) o1;
      int i2 = (Integer) o2;

      return i2 - i1;
    }

  }
}
