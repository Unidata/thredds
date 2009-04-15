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
  private static org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(GribGridServiceProvider.class);
  //private String saveIndexLocation;
  //private long gribLastModified = 0;
 // private long indexLastModified = 0;

  private long rafLength;    // length of the file when opened - used for syncing
  private int saveEdition = 0;
  private String saveLocation;

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

  public void open(RandomAccessFile raf, NetcdfFile ncfile, CancelTask cancelTask) throws IOException {
    this.raf = raf;
    this.ncfile = ncfile;
    this.rafLength = raf.length();

    long start = System.currentTimeMillis();
    if (GridServiceProvider.debugOpen)
      System.out.println("GribGridServiceProvider open = " + ncfile.getLocation());

    //String indexLocation = raf.getLocation() + ".gbx";
    //GridIndex index = getIndex(raf.getLocation(), indexLocation);
    GridIndex index = getIndex( 0, raf.getLocation());
    Map<String, String> attr = index.getGlobalAttributes();
    saveEdition = attr.get("grid_edition").equals("2") ? 2 : 1;
    //saveIndexLocation = indexLocation;

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


  protected void open(GridIndex index, CancelTask cancelTask) throws IOException {
    long start = System.currentTimeMillis();
    GridTableLookup lookup = (saveEdition == 2) ? getLookup2() : getLookup1();

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
   * Open the index file. If not already exist, then create it.
   *
   * //@param gribLocation  location of data file.
   * //@param indexLocation of the index file. The index file has ".gbx" appended.
   * @return ucar.grid.GridIndex
   * @throws IOException on io error
   */
   protected GridIndex getIndex(int edition, String location) throws IOException {
    // get an Index
    //saveEdition = edition;  //this is done after this method
    saveLocation = location;
    String indexLocation = location + ".gbx";

    GridIndex index = null;
    File indexFile;

    if (indexLocation.startsWith("http:")) { // direct access through http
      InputStream ios = indexExistsAsURL(indexLocation);
      if (ios != null) {
        //index = new Index();
        //index.open(indexLocation, ios);
        index = new GribReadIndex().open(indexLocation, ios);
        log.debug("opened HTTP index = " + indexLocation);
        return index;

      } else { // otherwise write it to / get it from the cache
        indexFile = DiskCache.getCacheFile(indexLocation);
        log.debug("HTTP index = " + indexFile.getPath());
      }

    } else {
      // always check first if the index file lives in the same dir as the regular file, and use it
      indexFile = new File(indexLocation);
      if (!indexFile.exists()) { // look in cache if need be
        log.debug("saveIndexFile not exist " + indexFile.getPath() + " ++ " + indexLocation);
        indexFile = DiskCache.getFile(indexLocation, alwaysInCache);
        log.debug("GribGridServiceProvider: use " + indexFile.getPath());
      }
    }

    // if index exist already, read it
    if (!forceNewIndex && indexFile.exists()) {
      //index = new Index();
      boolean ok = true;
      try {
        //ok = index.open(indexFile.getPath());
        index = new GribReadIndex().open(indexFile.getPath());
      } catch (Exception e) {
        ok = false;
      }

      if (ok && index != null ) {
        log.debug("  opened index = " + indexFile.getPath());

        // deal with possiblity that the grib file has changed, and the index should be extended or rewritten.
        // action depends on extendMode
        //String lengthS = index.getGlobalAttribute("length");
        String lengthS = index.getGlobalAttributes().get("length");
        long indexRafLength = (lengthS == null) ? 0 : Long.parseLong(lengthS);
        if (indexRafLength != rafLength) {

          if ((extendMode == IndexExtendMode.extend) && (indexRafLength < rafLength)) {
            log.debug("  extend Index = " + indexFile.getPath());
            //index = extendIndex(edition, raf, indexFile, index);
            File gribFile = new File(raf.getLocation());
            index = extendIndex(gribFile, indexFile, raf);

          } else if (extendMode == IndexExtendMode.rewrite) {
            // write new index
            log.debug("  rewrite index = " + indexFile.getPath());
            //index = writeIndex(edition, indexFile, raf);
            index = writeIndex(indexFile, raf);

          } else {
            log.debug("  index had new length, ignore ");
          }
        }

      } else {  // rewrite if fail to open
        log.debug("  write index = " + indexFile.getPath());
        //index = writeIndex(edition, indexFile, raf);
        index = writeIndex(indexFile, raf);
      }

    } else {
      // doesnt exist (or is being forced), create it and write it
      log.debug("  write index = " + indexFile.getPath());
      //index = writeIndex(edition, indexFile, raf);
      index = writeIndex(indexFile, raf);
    }

    return index;
  }

  private File getIndexFile(String location) throws IOException {
    String indexLocation = location + ".gbx";
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
    if (syncMode == IndexExtendMode.none) return false;

    // has the file chenged?
    if (rafLength != raf.length()) {
      File indexFile = getIndexFile(saveLocation);
      //Index index;
      GridIndex index;

      if ((syncMode == IndexExtendMode.extend) && (rafLength < raf.length())) {
        log.debug("  sync extend Index = " + indexFile.getPath());
        //extendIndex(saveEdition, raf, indexFile, null);
        //index = new Index();
        //index.open(indexFile.getPath());
        // i think the above was done because of bug on first making index had problems
        File gribFile = new File(raf.getLocation());
        index = extendIndex(gribFile, indexFile, raf);

      } else {
        // write new index
        log.debug("  sync rewrite index = " + indexFile.getPath());
        //index = writeIndex(saveEdition, indexFile, raf);
        index = writeIndex(indexFile, raf);
      }

      // reconstruct the ncfile objects
      ncfile.empty();
      open(index, null);

      return true;
    }

    return false;

  }

  private GridIndex writeIndex(File indexFile, RandomAccessFile raf) throws IOException {
    GridIndex index = null;
    try {
      //saveIndexLocation = indexFile.getPath();
      if (indexFile.exists()) {
        indexFile.delete();
        log.debug("Deleting old index " + indexFile.getPath());
      }

      if (saveEdition == 0) {
        raf.seek(0);
        Grib2Input g2i = new Grib2Input(raf);
        saveEdition = g2i.getEdition();
      }
      File gribFile = new File(raf.getLocation());

      if (saveEdition == 1) {
        index = new Grib1WriteIndex().writeGribIndex(gribFile, indexFile.getPath(), raf, true);
      } else if (saveEdition == 2) {
        index = new Grib2WriteIndex().writeGribIndex(gribFile, indexFile.getPath(), raf, true);
      }
      return index;

    } catch (NotSupportedException noSupport) {
      System.err.println("NotSupportedException : " + noSupport);
    }
    //indexLastModified = indexFile.lastModified();
    return index;
  }

  private GridIndex extendIndex(File gribFile, File indexFile, RandomAccessFile raf) throws IOException {

    GridIndex index = null;
    try {
      //saveIndexLocation = indexFile.getPath();
      if (saveEdition == 0) {
        raf.seek(0);
        Grib2Input g2i = new Grib2Input(raf);
        saveEdition = g2i.getEdition();
      }

      if (saveEdition == 1) {
        index = new Grib1WriteIndex().extendGribIndex(gribFile, indexFile, indexFile.getPath(), raf, true);
      } else if (saveEdition == 2) {
        index = new Grib2WriteIndex().extendGribIndex(gribFile, indexFile, indexFile.getPath(), raf, true);
      }
      //indexLastModified = indexFile.lastModified();
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
