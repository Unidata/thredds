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
import ucar.grib.NotSupportedException;
import ucar.grib.GribGridRecord;
import ucar.grib.Index;
import ucar.grib.GribReadIndex;
import ucar.grib.grib2.*;
import ucar.grid.GridRecord;
import ucar.grid.GridIndex;
import ucar.unidata.io.RandomAccessFile;

import java.io.*;
import java.util.List;
import java.net.URL;

public class GribGridServiceProvider extends GridServiceProvider {
  static private org.slf4j.Logger log =
      org.slf4j.LoggerFactory.getLogger(GribGridServiceProvider.class);

   // keep this info to reopen index when extending or syncing
  private long rafLength;    // length of the file when opened - used for syncing
  private int saveEdition = 0;
  private String saveLocation;

  /**
   * is this a Grib2 type file
   */
  private boolean isGrib2 = true;

  /**
   * returns Grib2 data
   */
  private Grib2Data dataReaderGrib2;

  public boolean isValidFile(RandomAccessFile raf) {

    try {
      raf.seek(0);
      raf.order( RandomAccessFile.BIG_ENDIAN );

      Grib2Input g2i = new Grib2Input( raf );
      int edition = g2i.getEdition();
      return edition == 2;

    } catch (Exception e) {
      return false;
    }
  }

  public void open(RandomAccessFile raf, NetcdfFile ncfile, CancelTask cancelTask) throws IOException {
    this.raf = raf;
    this.ncfile = ncfile;
    this.rafLength = raf.length();

    long start = System.currentTimeMillis();

    //int edition = (this instanceof Grib1ServiceProvider) ? 1 : 2;
    //Index index = getIndex(edition, raf.getLocation());
    //GridIndex index = getIndex(edition, raf.getLocation());
    GridIndex index = getIndex(raf.getLocation());

    open(index, cancelTask);
    log.debug("GridServiceProvider.open " + ncfile.getLocation() + " took " + (System.currentTimeMillis() - start));
  }

  protected void open(GridIndex index, CancelTask cancelTask) throws IOException {

      long startTime = System.currentTimeMillis();
      if (GridServiceProvider.debugOpen) System.out.println("GribServiceProvider open = "+ncfile.getLocation());

      Grib2Record firstRecord = null;
      try {
        Grib2Input g2i = new Grib2Input( raf );

        long start2 = System.currentTimeMillis();
        // params getProducts (implies  unique GDSs too), oneRecord
        // open it up and get the first product
        raf.seek(0);
        g2i.scan( false, true );

        List records = g2i.getRecords();
        firstRecord = (Grib2Record) records.get(0);
        if (debugTiming) {
          long took = System.currentTimeMillis() - start2;
          System.out.println("  read one record took="+took+" msec ");
        }

      } catch (NotSupportedException noSupport) {
        System.err.println("NotSupportedException : " + noSupport);
      }

      Grib2GridTableLookup lookup = new Grib2GridTableLookup( firstRecord );
      dataReaderGrib2 = new Grib2Data( raf );

      // make it into netcdf objects
      GridIndexToNC delegate = new GridIndexToNC();
      delegate.open( index, lookup, 2, ncfile, fmrcCoordSys, cancelTask);

      ncfile.finish();

      if (debugTiming) {
        long took = System.currentTimeMillis() - startTime;
        System.out.println(" open "+ncfile.getLocation()+" took="+took+" msec ");
      }
    }

  /**
     * Open the index file. If not exists, create it.
     * When writing use DiskCache, to make sure location is writeable.
     *
     * @param location   location of the file. The index file has ".gbx" appended.
     * @return ucar.grib.Index
     * @throws IOException on io error
     */
    protected GridIndex getIndex( String location) throws IOException {
      // get an Index
      //saveEdition = edition;
      saveLocation = location;
      String indexLocation = location + ".gbx";

      GridIndex index = null;
      File indexFile;

      if (indexLocation.startsWith("http:")) { // direct access through http
        InputStream ios = indexExistsAsURL(indexLocation);
        if (ios != null) {
          //index = new GridIndex();
          //index = GribReadIndex.open(indexLocation, ios);
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
          //indexFile = DiskCache.getFile(indexLocation, alwaysInCache);
          log.debug("GribServiceProvider: use " + indexFile.getPath());
        }
      }

      // if index exist already, read it
      //if (!forceNewIndex && indexFile.exists()) {
      if ( indexFile.exists()) {
        //index = new GridIndex();
        GribReadIndex gri = new GribReadIndex();
        boolean ok = true;
        try {
          index = gri.open(indexFile.getPath());
        } catch (Exception e) {
          ok = false;
        }

        if (ok) {
          log.debug("  opened index = " + indexFile.getPath());

          // deal with possiblity that the grib file has changed, and the index should be extended or rewritten.
          // action depends on extendMode
          //String lengthS = index.getGlobalAttribute("length");
//          long indexRafLength = (lengthS == null) ? 0 : Long.parseLong(lengthS);
//          if (indexRafLength != rafLength) {

//            if ((extendMode == IndexExtendMode.extend) && (indexRafLength < rafLength)) {
//              log.debug("  extend Index = " + indexFile.getPath());
//              index = extendIndex(edition, raf, indexFile, index);
//
//            } else if (extendMode == IndexExtendMode.rewrite) {
//              // write new index
//              log.debug("  rewrite index = " + indexFile.getPath());
//              index = writeIndex(edition, indexFile, raf);
//
//            } else {
//              log.debug("  index had new length, ignore ");
//            }
//          }

        } else {  // rewrite if fail to open
          log.debug("  write index = " + indexFile.getPath());
          //index = writeIndex(edition, indexFile, raf);
        }

      } else {
        // doesnt exist (or is being forced), create it and write it
        log.debug("  write index = " + indexFile.getPath());
        //index = writeIndex(edition, indexFile, raf);
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
          //indexFile = DiskCache.getFile(indexLocation, alwaysInCache);
          log.debug("GribGridServiceProvider: use " + indexFile.getPath());
        }
      }

      return indexFile;
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
    private Index writeIndex(int edition, File indexFile, RandomAccessFile raf) throws IOException {
      if (indexFile.exists()) {
        indexFile.delete();
        log.debug("Delete old index " + indexFile);
      }

      Index index = null;
      raf.seek(0);

      if (edition == 1) {
        ucar.grib.grib1.Grib1Indexer indexer = new ucar.grib.grib1.Grib1Indexer();
        PrintStream ps = new PrintStream(new BufferedOutputStream(new FileOutputStream(indexFile)));
        index = indexer.writeFileIndex(raf, ps, true);

      } else if (edition == 2) {
        ucar.grib.grib2.Grib2Indexer indexer2 = new ucar.grib.grib2.Grib2Indexer();
        PrintStream ps = new PrintStream(new BufferedOutputStream(new FileOutputStream(indexFile)));
        index = indexer2.writeFileIndex(raf, ps, true);
      }

      return index;
    }

  /////////////////////////////////////////////////////////////////////////////////////////////////

  protected float[] _readData( GridRecord gr) throws IOException {
    GribGridRecord ggr = (GribGridRecord)gr;
    if( isGrib2 ) {
      return dataReaderGrib2.getData( ggr.offset1, ggr.offset2 );
    } else {  //TODO: needs g1 params
      return dataReaderGrib2.getData( ggr.offset1, ggr.offset2 );
    }
  }
}
