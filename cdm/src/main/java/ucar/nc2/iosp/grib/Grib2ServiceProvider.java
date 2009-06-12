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
package ucar.nc2.iosp.grib;

import ucar.grib.Index;
import ucar.grib.NotSupportedException;
import ucar.grib.grib2.*;

import ucar.nc2.util.CancelTask;

import ucar.unidata.io.RandomAccessFile;

import java.io.*;
import java.util.*;

/**
 * Grib2 iosp.
 *
 * @deprecated
 */
public class Grib2ServiceProvider extends GribServiceProvider {

  private Grib2Data dataReader;

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

  public String getFileTypeId() {
    return "GRIB2";
  }

  public String getFileTypeDescription() {
    return "WMO GRIB Edition 2";
  }

  protected void open(Index index, CancelTask cancelTask) throws IOException {

    long startTime = System.currentTimeMillis();
    if (GribServiceProvider.debugOpen) System.out.println("GribServiceProvider open = "+ncfile.getLocation());

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

    Grib2Lookup lookup = new Grib2Lookup( firstRecord );

    // make it into netcdf objects
    Index2NC delegate = new Index2NC();
    delegate.open( index, lookup, 2, ncfile, fmrcCoordSys, cancelTask);

    ncfile.finish();

    dataReader = new Grib2Data( raf );

    if (debugTiming) {
      long took = System.currentTimeMillis() - startTime;
      System.out.println(" open "+ncfile.getLocation()+" took="+took+" msec ");
    }
  }


  /////////////////////////////////////////////////////////////////////////////////////////////////

  protected float[] _readData( long GdsOffset, long PdsOffset, int decimalScale, boolean bmsExists) throws IOException {
      return dataReader.getData( GdsOffset, PdsOffset );
  }

}
