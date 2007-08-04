/*
 * Copyright 1997-2007 Unidata Program Center/University Corporation for
 * Atmospheric Research, P.O. Box 3000, Boulder, CO 80307,
 * support@unidata.ucar.edu.
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or (at
 * your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser
 * General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
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
 * @author caron
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
    try {
      //System.out.println("dataOffset1="+ dataOffset1 +" scale ="+ decimalScale +" bmsE ="+ bmsExists );
      return dataReader.getData( dataOffset1, decimalScale, bmsExists );
    } catch (NotSupportedException e) {
      throw new IOException(e);
    }
  }

} // end Grib1ServiceProvider
