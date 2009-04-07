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
/**
 *
 * By:   Robb Kambic
 * Date: Feb 6, 2009
 * Time: 1:18:25 PM
 *
 */

package ucar.nc2.iosp.grib;

import ucar.nc2.iosp.grid.GridServiceProvider;
import ucar.nc2.iosp.grid.GridIndexToNC;
import ucar.nc2.util.CancelTask;
import ucar.nc2.util.DiskCache;
import ucar.nc2.NetcdfFile;
import ucar.grib.*;
import ucar.grib.grib1.*;
import ucar.grib.grib2.*;
import ucar.grid.GridRecord;
import ucar.grid.GridIndex;
import ucar.grid.GridTableLookup;
import ucar.unidata.io.RandomAccessFile;

import java.io.*;
import java.util.List;
import java.util.Map;
import java.net.URL;

public class GribGridServiceProvider extends GridServiceProvider {
  private static org.slf4j.Logger log =
      org.slf4j.LoggerFactory.getLogger(GribGridServiceProvider.class);
  //private long rafLength;    // length of the file when opened - used for syncing
  private int saveEdition = 0;
  private String saveIndexLocation;
  private long gribLastModified = 0;
  private long indexLastModified = 0;

  /**
   * returns Grib data
   */
  private Grib1Data dataReaderGrib1;
  private Grib2Data dataReaderGrib2;

  public boolean isValidFile(RandomAccessFile raf) {

    try {
      raf.seek(0);
      raf.order(RandomAccessFile.BIG_ENDIAN);

      Grib2Input g2i = new Grib2Input(raf);
      int edition = g2i.getEdition();
      return (edition == 1 || edition == 2);

    } catch (Exception e) {
      return false;
    }
  }

  public void open(RandomAccessFile raf, NetcdfFile ncfile, CancelTask cancelTask)
          throws IOException
  {
    this.raf = raf;
    this.ncfile = ncfile;
    //this.rafLength = raf.length();

    long start = System.currentTimeMillis();
    if (GridServiceProvider.debugOpen)
      System.out.println("GribGridServiceProvider open = " + ncfile.getLocation());
    GridIndex index = getIndex(raf.getLocation());
    Map<String, String> attr = index.getGlobalAttributes();
    saveEdition = attr.get("grid_edition").equals("2") ? 2 : 1;

    GridTableLookup lookup;
    if (saveEdition == 2) {
      lookup = getLookup2();
    } else {
      lookup = getLookup1();
    }
    // make it into netcdf objects
    new GridIndexToNC().open(index, lookup, saveEdition, ncfile, fmrcCoordSys, cancelTask);

    ncfile.finish();
    if (debugTiming) {
      long took = System.currentTimeMillis() - start;
      System.out.println(" open " + ncfile.getLocation() + " took=" + took + " msec ");
    }
    log.debug("GribGridServiceProvider.open " + ncfile.getLocation() + " took " + (System.currentTimeMillis() - start));
  }


  public void open(GridIndex index, CancelTask cancelTask) throws IOException{

    long start = System.currentTimeMillis();
    GridTableLookup lookup;
    if (saveEdition == 2) {
      lookup = getLookup2();
    } else {
      lookup = getLookup1();
    }
    // make it into netcdf objects
    new GridIndexToNC().open(index, lookup, saveEdition, ncfile, fmrcCoordSys, cancelTask);

    ncfile.finish();

    if (debugTiming) {
      long took = System.currentTimeMillis() - start;
      System.out.println(" open " + ncfile.getLocation() + " took=" + took + " msec ");
    }
    log.debug("GribGridServiceProvider.open from sync" + ncfile.getLocation() + " took " + (System.currentTimeMillis() - start));
  }

  protected GridTableLookup getLookup2() throws IOException {

    Grib2Record firstRecord = null;
    try {
      Grib2Input g2i = new Grib2Input(raf);

      long start2 = System.currentTimeMillis();
      // params getProducts (implies  unique GDSs too), oneRecord
      // open it up and get the first product
      raf.seek(0);
      g2i.scan(false, true);

      List records = g2i.getRecords();
      firstRecord = (Grib2Record) records.get(0);
      if (debugTiming) {
        long took = System.currentTimeMillis() - start2;
        System.out.println("  read one record took=" + took + " msec ");
      }

    } catch (NotSupportedException noSupport) {
      System.err.println("NotSupportedException : " + noSupport);
    }

    Grib2GridTableLookup lookup = new Grib2GridTableLookup(firstRecord);
    dataReaderGrib2 = new Grib2Data(raf);

    return lookup;

  }

  protected GridTableLookup getLookup1() throws IOException {

    Grib1Record firstRecord = null;
    try {
      Grib1Input g1i = new Grib1Input(raf);

      long start2 = System.currentTimeMillis();
      // params getProducts (implies  unique GDSs too), oneRecord
      // open it up and get the first product
      raf.seek(0);
      g1i.scan(false, true);

      List records = g1i.getRecords();
      firstRecord = (Grib1Record) records.get(0);
      if (debugTiming) {
        long took = System.currentTimeMillis() - start2;
        System.out.println("  read one record took=" + took + " msec ");
      }

    } catch (NotSupportedException noSupport) {
      System.err.println("NotSupportedException : " + noSupport);
    } catch (NoValidGribException noValid) {
      System.err.println("NoValidGribException : " + noValid);
    }

    Grib1GridTableLookup lookup = new Grib1GridTableLookup(firstRecord);
    dataReaderGrib1 = new Grib1Data(raf);

    return lookup;
  }

  /**
   * Open the index file. If not exists, else create it.
   *
   * @param gribLocation of the file. The index file has ".gbx" appended.
   * @return ucar.grid.GridIndex
   * @throws IOException on io error
   */
  protected GridIndex getIndex(String gribLocation)
          throws IOException
  {
    //http test
    //gribLocation = "http://motherlode.ucar.edu:9080/thredds/fileServer/fmrc/NCEP/NAM/Polar_90km/files/NAM_Polar_90km_20090403_1200.grib2";
    String indexLocation = gribLocation + ".gbx";

    GridIndex index = null;
    File indexFile = null;

    // just use the cache
    if (indexLocation.startsWith("http:")) {
      InputStream ios = indexExistsAsURL(indexLocation);
      if (ios != null) {
        index = new GribReadIndex().open(indexLocation, ios);
        saveIndexLocation = indexLocation;
        indexLastModified = System.currentTimeMillis(); // used in sync
        log.debug("opened HTTP index = " + indexLocation);
        return index;

      } else { // otherwise write it to cache
        indexFile = DiskCache.getCacheFile(indexLocation);
        log.debug("HTTP index = " + indexFile.getPath());
      }
    } else {
        // check first if the index file lives in the same dir as the regular file, and use it
        indexFile = new File(indexLocation); // don't know about write permission
        if (!indexFile.exists()) { // look in cache if need be
          log.debug("IndexFile not exist " + indexFile.getPath() + " ++ " + indexLocation);
          indexFile = DiskCache.getFile(indexLocation, alwaysInCache);
        }
    }
    log.debug("GribGridServiceProvider: using index " + indexFile.getPath());

    // get GribLastModified and IndexLastModified for sync, IndexLastModified
    // will be updated in extendIndex and writeIndex.
    File gribFile = new File( gribLocation );
    gribLastModified = gribFile.lastModified();
    indexLastModified = indexFile.lastModified();

    // once index determined, if forceNewIndex then write it. very expensive
    if( forceNewIndex && extendMode ) { // up to caller to make sure write permission exist
      if( indexFile.exists() && indexFile.canWrite()) {
        return writeIndex( indexFile, raf);
      } else if( ! indexFile.exists() ) {
        return writeIndex( indexFile, raf);
      } else {
        log.error("GribGridServiceProvider: cannot sync index, no write permission " + indexFile.getPath());
        return null;
      }
    }
    // if index exist already and extendMode, check/read it
    if ( extendMode && indexFile.exists() ) {
      try {
        if (indexFile.canWrite() ) {
          // index later then grib, so just read it else extend it
          if( gribFile.lastModified() < indexFile.lastModified() ) {
             index = new GribReadIndex().open(indexFile.getPath());
             saveIndexLocation = indexFile.getPath();
          } else {
             index = extendIndex( gribFile, indexFile, raf );
          }
        } else { // no index write permission, just read it but set warning
           index = new GribReadIndex().open(indexFile.getPath());
           saveIndexLocation = indexFile.getPath();
           log.warn("GribGridServiceProvider: cannot extend index, no write permission " + indexFile.getPath());
        }
      }
      catch ( Exception e)
      {
        String msg = "Problem reading or extending index file";
        log.error( "getIndex(): " + msg + "[" + indexLocation + "].");
        throw new IOException( msg, e);
      }

      if (index != null) {
        log.debug("GribGridServiceProvider: opened index = " + indexFile.getPath());
      } else {
        log.error("GribGridServiceProvider: index open failed, extend index = " + indexFile.getPath());
        //index = writeIndex( indexFile, raf);
      }

    } else if (indexFile.exists() ) {
      try {
        index = new GribReadIndex().open(indexFile.getPath());
        saveIndexLocation = indexFile.getPath();
      }
      catch (Exception e)
      {
        String msg = "Problem reading index file";
        log.error( "getIndex(): " + msg + "[" + indexLocation + "]." );
        throw new IOException( msg, e );
      }
      if (index != null) {
        log.debug("GribGridServiceProvider: opened index = " + indexFile.getPath());
      } else {  // rewrite if possible, failed to open
        log.error("GribGridServiceProvider: index open failed, try write index = " + indexFile.getPath());
        if (indexFile.canWrite() )
          index = writeIndex( indexFile, raf);
      }

    } else { // index doesn't exist
      log.debug("GribGridServiceProvider: creating index = " + indexFile.getPath());
      index = writeIndex( indexFile, raf);
    }
    return index;
  }

  private File getIndexFile(String indexLocation) throws IOException {
    File indexFile = null;

    if (indexLocation.startsWith("http:")) { // LOOK direct access through http maybe should disallow ??
      indexFile = DiskCache.getCacheFile(indexLocation);
      log.debug("  HTTP index = " + indexFile.getPath());

    } else {
      // always check first if the index file lives in the same dir as the regular file, and use it
      indexFile = new File(indexLocation);
      if (!indexFile.exists()) { // look in cache if need be
        log.debug("GribGridServiceProvider: saveIndexFile not exist " + indexFile.getPath() + " ++ " + indexLocation);
        indexFile = DiskCache.getFile(indexLocation, alwaysInCache);
        log.debug("GribGridServiceProvider: use " + indexFile.getPath());
      }
    }

    return indexFile;
  }

  public boolean sync() throws IOException {

    GridIndex index = null;

    // could be an url, possible reread index
    if (saveIndexLocation.startsWith("http:")) {
      if ( (System.currentTimeMillis() - indexLastModified) > 30000 ) {
        log.debug("  sync reread http index = " + saveIndexLocation );
        index = new GribReadIndex().open(saveIndexLocation);
        indexLastModified = System.currentTimeMillis();
         ncfile.empty();
         open(index, null);
         return true;
      }
      return false;
    }

    // regular or cache file
    File gribFile = new File( raf.getLocation() );
    // caused to look in cache wrongly
    File indexFile = getIndexFile(saveIndexLocation);
    //File indexFile = new File( saveIndexLocation );

    // write or extend
    if(  extendMode && (gribLastModified < gribFile.lastModified() ||
      indexLastModified < indexFile.lastModified() )) {

      if ( extendMode && indexFile.exists() ) {
        log.debug("  sync extend Index = " + indexFile.getPath());
        index = extendIndex( gribFile, indexFile, raf );

      } else {
        // write new index
        log.debug("  sync rewrite index = " + indexFile.getPath());
        index = writeIndex( indexFile, raf);
      }
      gribLastModified = gribFile.lastModified();
      indexLastModified = indexFile.lastModified();

    // just read Index, it's been updated
    } else if ( indexLastModified < indexFile.lastModified() ) {
      log.debug("  sync reread index = " + indexFile.getPath());
      index = new GribReadIndex().open(indexFile.getPath());
      indexLastModified = indexFile.lastModified();
    }
    if ( index != null ) {
      // reconstruct the ncfile objects
      ncfile.empty();
      open(index, null);
      return true;
    }
    return false;
  }

  private GridIndex writeIndex( File indexFile, RandomAccessFile raf) throws IOException {
    GridIndex index = null;
    try {
      saveIndexLocation = indexFile.getPath();
      if (indexFile.exists()) {
        indexFile.delete();
        log.debug("Deleting old index " + indexFile.getPath());
      }

      if ( saveEdition == 0 ) {
        raf.seek(0);
        Grib2Input g2i = new Grib2Input(raf);
        saveEdition = g2i.getEdition();
      }
      File gribFile = new File(raf.getLocation());

      if (saveEdition == 1) {
          index = new Grib1WriteIndex().writeGribIndex( gribFile, indexFile.getPath(), raf, true);
      } else if (saveEdition == 2) {
        index = new Grib2WriteIndex().writeGribIndex( gribFile, indexFile.getPath(), raf, true);
      }
      return index;

    } catch (NotSupportedException noSupport) {
      System.err.println("NotSupportedException : " + noSupport);
    }
    indexLastModified = indexFile.lastModified();
    return index;
  }

  private GridIndex extendIndex(File gribFile, File indexFile, RandomAccessFile raf) throws IOException {

    GridIndex index = null;
    try {
      saveIndexLocation = indexFile.getPath();
      if ( saveEdition == 0 ) {
        raf.seek(0);
        Grib2Input g2i = new Grib2Input(raf);
        saveEdition = g2i.getEdition();
      }

      if (saveEdition == 1) {
          index = new Grib1WriteIndex().extendGribIndex(gribFile, indexFile, indexFile.getPath(), raf, true);
      } else if (saveEdition == 2) {
          index = new Grib2WriteIndex().extendGribIndex(gribFile, indexFile, indexFile.getPath(), raf, true);
      }
      indexLastModified = indexFile.lastModified();
      return index;

    } catch (NotSupportedException noSupport) {
      System.err.println("NotSupportedException : " + noSupport);
    }
    return index;
  }

  // if exists, return input stream, otherwise null
  private InputStream indexExistsAsURL(String indexLocation) {
    try {
      URL url = new URL(indexLocation);
      return url.openStream();
    } catch (IOException e) {
      return null;
    }
  }
  /////////////////////////////////////////////////////////////////////////////////////////////////

  protected float[] _readData(GridRecord gr) throws IOException {
    GribGridRecord ggr = (GribGridRecord) gr;
    if (saveEdition == 2) {
      return dataReaderGrib2.getData(ggr.offset1, ggr.offset2);
    } else {
      return dataReaderGrib1.getData(ggr.offset1, ggr.decimalScale, ggr.bmsExists);
    }
  }
}
