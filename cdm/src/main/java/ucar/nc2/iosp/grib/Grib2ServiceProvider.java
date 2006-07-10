package ucar.nc2.iosp.grib;

import ucar.grib.Index;
import ucar.grib.NotSupportedException;
import ucar.grib.grib2.*;

import ucar.nc2.*;
import ucar.nc2.util.CancelTask;

import ucar.unidata.io.RandomAccessFile;

import java.io.*;
import java.util.*;

/**
 * Grib2 iosp.
 *
 * @author caron
 * @version $Revision: 1.18 $ $Date: 2006/05/24 00:12:56 $
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

      ArrayList records = g2i.getRecords();
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
    delegate.open( index, lookup, 2, ncfile, cancelTask);

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
