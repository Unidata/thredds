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
 * Superclass for Grib1 and Grib2 IOSPs, using GridServiceProvider
 *
 * @author Robb Kambic
 * @since Feb 6, 2009
 */

package ucar.nc2.iosp.grib;

import ucar.nc2.iosp.grid.GridServiceProvider;
import ucar.nc2.iosp.grid.GridIndexToNC;
import ucar.nc2.util.CancelTask;
import ucar.nc2.util.DiskCache;
import ucar.nc2.NetcdfFile;
import ucar.nc2.dt.fmrc.FmrcDefinition;
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

  private long rafLength;    // length of the file when opened - used for syncing
  private long indexLength;  // length of the index in getIndex - used for syncing
  private int saveEdition = 0;
  private float version = 0;

  private Grib1Data dataReaderGrib1;
  private Grib2Data dataReaderGrib2;

  @Override
  public boolean isValidFile(RandomAccessFile raf) {
    try {
      raf.seek(0);
      raf.order(RandomAccessFile.BIG_ENDIAN);

      Grib2Input g2i = new Grib2Input(raf); // LOOK rather heavyweight for isValidFile()
      int edition = g2i.getEdition();
      return (edition == 1 || edition == 2);

    } catch (Exception e) {
      return false;
    }
  }

  @Override
  public String getFileTypeId() {
    return (saveEdition == 2) ? "GRIB2" : "GRIB1";
  }

  @Override
  public String getFileTypeDescription() {
    return (saveEdition == 2) ? "WMO GRIB Edition 2" : "WMO GRIB Edition 1";
  }

  @Override
  public Object sendIospMessage(Object special) {
    if (special instanceof String) {
      String s = (String) special;
      if (s.equalsIgnoreCase("GridIndex"))
        try {
          return getIndex(raf.getLocation());
        } catch (IOException e) {
          return null;
        }
    }
    return super.sendIospMessage(special);
  }


  @Override
  public void open(RandomAccessFile raf, NetcdfFile ncfile, CancelTask cancelTask) throws IOException {
    this.raf = raf;
    this.ncfile = ncfile;
    this.rafLength = raf.length();

    long start = System.currentTimeMillis();
    if (GridServiceProvider.debugOpen)
      System.out.println(" open() = " + ncfile.getLocation());

    GridIndex index = getIndex(raf.getLocation());
    Map<String, String> attr = index.getGlobalAttributes();
    saveEdition = attr.get("grid_edition").equals("2") ? 2 : 1;
    version = Float.parseFloat(attr.get("index_version"));

    GridTableLookup lookup;
    if (saveEdition == 2) {
      lookup = getLookup2();
    } else {
      lookup = getLookup1();
    }
    // code to test fmrcCoordSys, need to make sure definition file matches data file
    //FmrcDefinition fmrcCoordSys = new FmrcDefinition();
    //fmrcCoordSys.readDefinitionXML("/local/robb/data/grib/SREF/NCEP-SREF-PacificNE_0p4-ensprod.fmrcDefinition.xml");
    // make it into netcdf objects
    new GridIndexToNC().open(index, lookup, saveEdition, ncfile, fmrcCoordSys, cancelTask);
    ncfile.finish();

    if (debugTiming) {
      long took = System.currentTimeMillis() - start;
      System.out.println(" open " + ncfile.getLocation() + " took=" + took + " msec ");
    }
    log.debug(" open() " + ncfile.getLocation() + " took " + (System.currentTimeMillis() - start));
  }


  protected void open(GridIndex index, CancelTask cancelTask) throws IOException {
    long start = System.currentTimeMillis();
    GridTableLookup lookup = (saveEdition == 2) ? getLookup2() : getLookup1();

    // make it into netcdf objects
    new GridIndexToNC().open(index, lookup, saveEdition, ncfile, fmrcCoordSys, cancelTask);

    ncfile.finish();

    if (debugTiming) {
      long took = System.currentTimeMillis() - start;
      System.out.println(" open() " + ncfile.getLocation() + " took=" + took + " msec ");
    }
    log.debug(" open() from sync" + ncfile.getLocation() + " took " + (System.currentTimeMillis() - start));
  }

  // debugging
  public GridTableLookup getLookup() throws IOException {
    GridTableLookup lookup;
    if (saveEdition == 2) {
      lookup = getLookup2();
    } else {
      lookup = getLookup1();
    }
    return lookup;
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
   * Open the index file. If not exists, create it.
   * When writing use DiskCache, to make sure location is writeable.
   *
   * @param dataLocation location of the file. The index file has ".gbx" appended.
   * @return ucar.grib.Index
   * @throws IOException on io error
   */
  protected GridIndex getIndex(String dataLocation) throws IOException {

    if (dataLocation.startsWith("http:")) { // direct access through http
      //String indexLocation = dataLocation + ".gbx";
      String indexLocation = GribIndexName.get( dataLocation );
      InputStream ios = indexExistsAsURL(indexLocation);
      if (ios != null) {
        log.debug(" getIndex() HTTP index = " + indexLocation);
        return new GribReadIndex().open(indexLocation, ios);
      }
    }

    File indexFile = getIndexFile(dataLocation);
    GridIndex index = null;

    // if index exist already, read it
    if (!forceNewIndex && indexFile.exists()) {
      try {
        index = new GribReadIndex().open(indexFile.getPath());
        log.debug("  opened index = " + indexFile.getPath());

        // deal with possiblity that the grib file has changed, and the index should be extended or rewritten.
        if ((indexFileModeOnOpen != IndexExtendMode.readonly)) {

          String lengthS = index.getGlobalAttributes().get("length");
          long indexRafLength = (lengthS == null) ? 0 : Long.parseLong(lengthS);
          if (indexRafLength != rafLength) {
            if (log.isDebugEnabled())
              log.debug("  dataFile " + dataLocation + " length has changed: indexRafLength= " + indexRafLength + " rafLength= " + rafLength);

            if (indexFileModeOnOpen == IndexExtendMode.extendwrite) {
              if (indexRafLength < rafLength) {
                if (log.isDebugEnabled()) log.debug("  extend Index = " + indexFile.getPath());
                index = extendIndex(new File(raf.getLocation()), indexFile, raf);
              } else {
                if (log.isDebugEnabled()) log.debug("  rewrite index = " + indexFile.getPath());
                index = writeIndex(indexFile, raf);
              }

            } else if (indexFileModeOnOpen == IndexExtendMode.rewrite) {
              if (log.isDebugEnabled()) log.debug("  rewrite index = " + indexFile.getPath());
              index = writeIndex(indexFile, raf);
            }
          }
        }

      } catch (Exception e) {
        log.warn("GribReadIndex() failed, will try to rewrite at " + indexFile.getPath(), e);
        index = writeIndex(indexFile, raf);
      }

      // doesnt exist (or is being forced), create it
    } else {
      log.debug("  write index = " + indexFile.getPath());
      index = writeIndex(indexFile, raf);
    }

    indexLength = indexFile.length();
    return index;
  }

  /**
   * Get the Index as a File. Always check if the index file lives in the same dir as the data file, and use it if so.
   * After that, look for it in the DiskCache, and use it if it exists.
   * If not exist, find a location that is writeable, using the DiskCache algorithm.
   *
   * @param dataLocation location of the data file
   * @return Index as a File, may not exist.
   * @throws IOException on read error
   */
  private File getIndexFile(String dataLocation) throws IOException {
    String indexLocation = GribIndexName.getIndex( dataLocation, false );
    File indexFile = null;

    if (indexLocation.startsWith("http:")) { // LOOK direct access through http maybe should disallow ??
      indexFile = DiskCache.getCacheFile(indexLocation);
      log.debug("  HTTP index = " + indexFile.getPath());

    } else {
      // always check first if the index file lives in the same dir as the regular file, and use it
      indexFile = new File(indexLocation);
      if (!indexFile.exists()) { // look in cache if need be
        log.debug(" saveIndexFile not exist " + indexFile.getPath() + " ++ " + indexLocation);
        indexFile = DiskCache.getFile(indexLocation, alwaysInCache);
        log.debug(" use " + indexFile.getPath());
      }
    }

    return indexFile;
  }


  private GridIndex writeIndex(File indexFile, RandomAccessFile raf) throws IOException, NotSupportedException {
    GridIndex index = null;

    if (indexFile.exists()) {
      boolean ok = indexFile.delete();
      log.debug("Deleted old index " + indexFile.getPath() + " = " + ok);
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
  }

  public boolean sync() throws IOException {

    // has the file changed?
    File indexFile = getIndexFile(raf.getLocation());
    if (rafLength != raf.length() || indexLength != indexFile.length()) {
      GridIndex index;
      if (indexFileModeOnSync == IndexExtendMode.readonly) {
        log.debug("  sync() read Index = " + indexFile.getPath());
        try {
          index = new GribReadIndex().open(indexFile.getPath());
        } catch (Exception e) {
          log.warn("  sync() return false: GribReadIndex() failed = " + indexFile.getPath());
          return false;
        }

      } else if (indexFileModeOnSync == IndexExtendMode.extendwrite) {
        if ((rafLength <= raf.length()) && (indexLength <= indexFile.length())) {
          if (log.isDebugEnabled()) log.debug("  sync() extend Index = " + indexFile.getPath());
          index = extendIndex(new File(raf.getLocation()), indexFile, raf);
        } else {
          if (log.isDebugEnabled()) log.debug("  sync() rewrite index = " + indexFile.getPath());
          index = writeIndex(indexFile, raf);
        }

      } else {
        // write new index
        log.debug("  sync() rewrite index = " + indexFile.getPath());
        index = writeIndex(indexFile, raf);
      }

      // update so next sync call doesn't reread unnecessary
      rafLength = raf.length();
      indexLength = indexFile.length();

      // reconstruct the ncfile objects
      ncfile.empty();
      open(index, null);

      return true;
    }

    return false;
  }

  private GridIndex extendIndex(File gribFile, File indexFile, RandomAccessFile raf) throws IOException {

    GridIndex index = null;

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
    return index;

  }

  // if exists, return input stream, otherwise null
  private InputStream indexExistsAsURL(String indexLocation) {
    try {
      URL url = new URL(indexLocation);
      return url.openStream();
    } catch (Exception e) {
      return null;
    }
  }
  /////////////////////////////////////////////////////////////////////////////////////////////////

  protected float[] _readData(GridRecord gr) throws IOException {
    GribGridRecord ggr = (GribGridRecord) gr;
    if (saveEdition == 2) {
      return dataReaderGrib2.getData(ggr.offset1, ggr.offset2);
    } else if (version >= 8 ) {
      return dataReaderGrib1.getData(ggr.offset1, ggr.offset2, ggr.decimalScale, ggr.bmsExists);
    } else {  
      return dataReaderGrib1.getData(ggr.offset1, ggr.decimalScale, ggr.bmsExists);
    }
  }
}
