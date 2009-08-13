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

// import statements

import ucar.unidata.io.RandomAccessFile;
import ucar.nc2.*;
import ucar.nc2.iosp.IOServiceProvider;
import ucar.nc2.util.CancelTask;

import java.lang.*;     // Standard java functions
import java.util.*;
import java.io.IOException;
import java.io.FileNotFoundException;     // Extra utilities from sun


/**
 * ************************************************************************
 *
 * @author kambic
 *         <p/>
 *         **************************************************************************
 */
public class Grib2Netcdf {


  /**
   * *********************************************************************
   * <p/>
   * Grib2Netcdf usage of the class, if called without arguments
   *
   * @param className name of class
   *                  <p/>
   *                  *********************************************************************
   */
  public void usage(String className) {
    System.out.println();
    System.out.println("Usage of " + className + ":");
    System.out.println("Parameters:");
    System.out.println("<GribFileToRead> reads/scans metadata");
    System.out.println("<NetCDF output file> file to store results");
    System.out.println();
    System.out.println("java -Xmx256m " + className +
        " <GribFileToRead> <NetCDF output file>");
    System.exit(0);
  }

  /**
   * ********************************************************************
   * Grib2Netcdf<br>
   *
   * @param args input gribfile to read,  output Netcdf file
   * @throws java.io.IOException on io error
   */
  public static void main(String args[]) throws IOException {

    // Function References
    Grib2Netcdf func = new Grib2Netcdf();

    // Test usage
    if (args.length != 2) {
      // Get class name as String
      Class cl = func.getClass();
      func.usage(cl.getName());
    }

    // Get UTC TimeZone
    // A list of available ID's show that UTC has ID = 127
    TimeZone tz = TimeZone.getTimeZone("127");
    TimeZone.setDefault(tz);

    // Say hello
    Date now = Calendar.getInstance().getTime();
    System.out.println(now.toString() + " ... Start of Grib2Netcdf");
    System.out.println("read grib file=" + args[0] + " write to netCDF file=" + args[1]);

    // Reading of Grib files must be inside a try-catch block
    try {
      RandomAccessFile raf;
      raf = new RandomAccessFile(args[0], "r");
      raf.order(RandomAccessFile.BIG_ENDIAN);
      
      Class c = ucar.nc2.iosp.grib.GribGridServiceProvider.class;
      IOServiceProvider iosp = null;
      try {
        iosp = (IOServiceProvider) c.newInstance();
      } catch (InstantiationException e) {
        throw new IOException("IOServiceProvider " + c.getName() + "must have no-arg constructor.");
      } catch (IllegalAccessException e) {
        throw new IOException("IOServiceProvider " + c.getName() + " IllegalAccessException: " + e.getMessage());
      }

      NetcdfFile ncfile = new MakeNetcdfFile(iosp, raf, args[0], null);
      NetcdfFile nc = FileWriter.writeToFile(ncfile, args[1]);
      nc.close();
      raf.close();  // done reading

      // Catch thrown errors from GribFile
    } catch (FileNotFoundException noFileError) {
      System.err.println("FileNotFoundException : " + noFileError);
    } catch (IOException ioError) {
      System.err.println("IOException : " + ioError);
    }

    // Goodbye message
    now = Calendar.getInstance().getTime();
    System.out.println(now.toString() + " ... End of Grib2Netcdf!");

  } // end main

  static class MakeNetcdfFile extends NetcdfFile {
    MakeNetcdfFile(
        IOServiceProvider spi, RandomAccessFile raf,
        String location, CancelTask cancelTask) throws IOException {
      super(spi, raf, location, cancelTask);
    }
  }
}
