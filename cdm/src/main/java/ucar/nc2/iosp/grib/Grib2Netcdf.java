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

// import statements
import ucar.unidata.io.RandomAccessFile;
import ucar.nc2.*;
import ucar.nc2.iosp.IOServiceProvider;
import ucar.nc2.util.CancelTask;
import ucar.grib.*;

import java.lang.*;     // Standard java functions
import java.util.*;
import java.io.IOException;
import java.io.FileNotFoundException;   	// Extra utilities from sun


/***************************************************************************
 *
 *
 * @author kambic
 *
 ****************************************************************************/
public class Grib2Netcdf {


  /************************************************************************
    *
    * Grib2Netcdf usage of the class, if called without arguments
    * @param className name of class
    *
    ***********************************************************************/
  public void usage(String className) {
      System.out.println();
      System.out.println("Usage of " + className + ":");
      System.out.println( "Parameters:");
      System.out.println( "<GribFileToRead> reads/scans metadata");
      System.out.println( "<NetCDF output file> file to store results");
      System.out.println();
      System.out.println("java -Xmx256m " + className + 
         " <GribFileToRead> <NetCDF output file>");
      System.exit(0);
   }

   /***********************************************************************
    * Grib2Netcdf<br>
    *
    * @param  args input gribfile to read,  output Netcdf file 
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
      System.out.println("read grib file="+args[0]+" write to netCDF file="+args[1]);

      // Reading of Grib files must be inside a try-catch block
      try {
         RandomAccessFile raf;
         raf = new RandomAccessFile( args[0], "r" );
         raf.order( RandomAccessFile.BIG_ENDIAN ); 
         int version = GribChecker.getEdition( raf );
         IOServiceProvider iosp = null;
         if( version == 1 ) {
            iosp = new Grib1ServiceProvider();
         } else if( version == 2 ) {
            iosp = new Grib2ServiceProvider();
         } else {
             System.out.println( args[0] +" is not a Grib file" );
             System.exit( 1 );
         }

         NetcdfFile ncfile = new MakeNetcdfFile( iosp, raf, args[0], null );
         NetcdfFile nc = FileWriter.writeToFile( ncfile, args[1] );
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
      MakeNetcdfFile( IOServiceProvider spi, RandomAccessFile raf,
         String location, CancelTask cancelTask ) throws IOException {
            super( spi, raf, location, cancelTask );
      }
   }
}
