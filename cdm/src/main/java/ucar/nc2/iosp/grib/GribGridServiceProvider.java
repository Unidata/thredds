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
import ucar.grib.grib1.Grib1Data;
import ucar.grib.grib1.Grib1Record;
import ucar.grib.grib1.Grib1Input;
import ucar.grib.grib1.Grib1GridTableLookup;
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
  private static boolean alwaysInCache = false;

  // keep this info to reopen index when extending or syncing
  private long rafLength;    // length of the file when opened - used for syncing
  private int edition = 0;
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
    GridIndex index = getIndex(raf.getLocation());
    Map<String, String> attr = index.getGlobalAttributes();
    edition = attr.get("grid_edition").equals("2") ? 2 : 1;

    GridTableLookup lookup;
    if (edition == 2) {
      lookup = getLookup2();
    } else {
      lookup = getLookup1();
    }
    // make it into netcdf objects
    new GridIndexToNC().open(index, lookup, edition, ncfile, fmrcCoordSys, cancelTask);

    ncfile.finish();

    if (debugTiming) {
      long took = System.currentTimeMillis() - start;
      System.out.println(" open " + ncfile.getLocation() + " took=" + took + " msec ");
    }

    log.debug("GridServiceProvider.open " + ncfile.getLocation() + " took " + (System.currentTimeMillis() - start));
  }

  // not used
  public void open(GridIndex index, CancelTask cancelTask) {
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
    } catch (NoValidGribException noValid ) {
      System.err.println("NoValidGribException : " + noValid);
    }

    Grib1GridTableLookup lookup = new Grib1GridTableLookup(firstRecord);
    dataReaderGrib1 = new Grib1Data(raf);

    return lookup;
  }

  /**
   * Open the index file. If not exists, create it.
   * When writing use DiskCache, to make sure location is writeable.
   *
   * @param location location of the file. The index file has ".gbx" appended.
   * @return ucar.grid.GridIndex
   * @throws IOException on io error
   */
  protected GridIndex getIndex(String location) throws IOException {
    // get an Index
    saveLocation = location;
    String indexLocation = location + ".gbx";

    GridIndex index = null;
    File indexFile;

    if (indexLocation.startsWith("http:")) { // direct access through http
      index = new GribReadIndex().open(indexLocation);
      if (index == null) {
        // need to write it some where
        //index = new GribReadIndex().open(indexLocation, ios);
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
        log.debug("GribServiceProvider: use " + indexFile.getPath());
      }
    }

    // if index exist already, read it
    if (indexFile.exists()) {
      try {
        index = new GribReadIndex().open(indexFile.getPath());
      } catch (Exception e) {
      }

      if (index != null) {
        log.debug("  opened index = " + indexFile.getPath());
      } else {  // rewrite if fail to open
        log.debug("  write index = " + indexFile.getPath());
        //index = writeIndex(edition, indexFile, raf);
      }

    } else {
      // doesnt exist (or is being forced), create it and write it
      log.debug("  write index = " + indexFile.getPath());
      index = writeIndex( indexFile, raf);
    }

    return index;
  }

  private GridIndex writeIndex( File indexFile, RandomAccessFile raf) throws IOException {
    GridIndex index = null;

    try {

      if (indexFile.exists()) {
        indexFile.delete();
        log.debug("Delete old index " + indexFile);
      }

      raf.seek(0);
      Grib2Input g2i = new Grib2Input(raf);
      int edition = g2i.getEdition();

      if (edition == 1) {
        DataOutputStream out = new DataOutputStream(
            new BufferedOutputStream(
                new FileOutputStream(indexFile.getPath(), false)));

        //index = new Grib1WriteIndex().writeFileIndex(raf, out, true);

      } else if (edition == 2) {
        DataOutputStream out = new DataOutputStream(
            new BufferedOutputStream(
                new FileOutputStream(indexFile.getPath(), false)));

        index = new Grib2WriteIndex().writeFileIndex(raf, out, true);
      }

      return index;

    } catch (NotSupportedException noSupport) {
      System.err.println("NotSupportedException : " + noSupport);
    //} catch (NoValidGribException noValid ) {
    //  System.err.println("NoValidGribException : " + noValid);
    }
    return index;
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////

  protected float[] _readData(GridRecord gr) throws IOException {
    GribGridRecord ggr = (GribGridRecord) gr;
    if (edition == 2) {
      return dataReaderGrib2.getData(ggr.offset1, ggr.offset2);
    } else {   
      return dataReaderGrib1.getData(ggr.offset1, ggr.decimalScale, ggr.bmsExists);
    }
  }
}
