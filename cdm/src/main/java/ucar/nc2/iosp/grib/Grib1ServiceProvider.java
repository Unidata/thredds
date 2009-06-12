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

import ucar.grib.*;
import ucar.grib.grib1.*;

import ucar.nc2.util.CancelTask;

import ucar.unidata.io.RandomAccessFile;

import java.io.*;
import java.util.*;

/**
 * Grib1 iosp.
 *
 * @deprecated
 */
public class Grib1ServiceProvider extends GribServiceProvider {

  private Grib1Data dataReader;

  public boolean isValidFile(RandomAccessFile raf) {

    try {
      raf.seek(0);
      raf.order( RandomAccessFile.BIG_ENDIAN );

      Grib1Input scanner = new Grib1Input( raf );
      int edition =  scanner.getEdition();
      return (edition == 1);

    } catch (Exception e) {
      return false;
    }

  }

  public String getFileTypeId() {
    return "GRIB1";
  }

  public String getFileTypeDescription() {
    return "WMO GRIB Edition 1";
  }

  protected void open(Index index, CancelTask cancelTask) throws IOException {

    long startTime = System.currentTimeMillis();
    if (GribServiceProvider.debugOpen) System.out.println("GribServiceProvider open = "+ncfile.getLocation());

    Grib1Product firstProduct = null;
    try {
      Grib1Input scanner = new Grib1Input( raf );

      long start2 = System.currentTimeMillis();
      // params getProducts (implies  unique GDSs too), oneRecord
      // open it up and get the first product
      raf.seek(0);
      scanner.scan( true, true );

      ArrayList products = scanner.getProducts();
      if (products.size() == 0)
        throw new IOException("no valid products were found");

      firstProduct = (Grib1Product) products.get(0);
      if (debugTiming) {
        long took = System.currentTimeMillis() - start2;
        System.out.println("  read one record took="+took+" msec ");
      }

    } catch (NoValidGribException noGrib) {
      System.err.println("NoValidGribException : " + noGrib);
    } catch (NotSupportedException noSupport) {
      System.err.println("NotSupportedException : " + noSupport);
    }

    Grib1Lookup lookup = new Grib1Lookup( firstProduct);

    // make it into netcdf objects
    Index2NC delegate = new Index2NC();
    delegate.open( index, lookup, 1, ncfile, fmrcCoordSys, cancelTask);

    ncfile.finish();

    dataReader = new Grib1Data( raf );

    if (debugTiming) {
      long took = System.currentTimeMillis() - startTime;
      System.out.println(" open "+ncfile.getLocation()+" took="+took+" msec ");
    }
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////

  protected float[] _readData( long dataOffset1, long dataOffset2, int decimalScale, boolean bmsExists ) throws IOException {
      //System.out.println("dataOffset1="+ dataOffset1 +" scale ="+ decimalScale +" bmsE ="+ bmsExists );
      return dataReader.getData( dataOffset1, decimalScale, bmsExists );
  }

} // end Grib1ServiceProvider
