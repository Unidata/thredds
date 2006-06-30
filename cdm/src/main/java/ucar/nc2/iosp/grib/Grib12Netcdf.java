package ucar.nc2.iosp.grib;

// import statements
import ucar.unidata.io.RandomAccessFile;
import ucar.nc2.*;
import ucar.nc2.util.CancelTask;

import java.lang.*;     // Standard java functions
import java.util.*;
import java.io.IOException;
import java.io.FileNotFoundException;   	// Extra utilities from sun


/***************************************************************************
 *
 * @author Robb Kambic  3/22/06
 *
 * @version 1.0
 *
 ****************************************************************************/
public class Grib12Netcdf {


  /************************************************************************
    *
    * Grib12Netcdf usage of the class, if called without arguments
    *
    ************************************************************************/
   public void usage(String className) {
      System.out.println();
      System.out.println("Usage of " + className + ":");
      System.out.println( "Parameters:");
      System.out.println( "<Grib1FileToRead> reads/scans metadata");
      System.out.println( "<NetCDF output file> file to store results");
      System.out.println();
      System.out.println("java -Xmx256m " + className + 
         " <GribFileToRead> <NetCDF output file>");
      System.exit(0);
   }

   /***********************************************************************
    * Grib12Netcdf<br>
    *
    * @param  args input Filename of gribfile to read
    * @param  args output Netcdf file name
    *
    *************************************************************************/
   public static void main(String args[]) throws IOException {

      // Function References
      Grib12Netcdf func = new Grib12Netcdf();

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
      //System.out.println(now.toString() + " ... Start of Grib12Netcdf");
      System.out.println("read grib1 file="+args[0]+" write to netCDF file="+args[1]);

      // Reading of Grib files must be inside a try-catch block
      try {
         IOServiceProvider iosp = null;
         iosp = (IOServiceProvider) new Grib1ServiceProvider();
         RandomAccessFile raf = null;
         raf = new RandomAccessFile( args[0], "r" );

         NetcdfFile ncfile = (NetcdfFile) new MakeNetcdfFile( iosp, raf, args[0], null );
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
   //System.out.println(now.toString() + " ... End of Grib12Netcdf!");

   } // end main

  static class MakeNetcdfFile extends NetcdfFile {
      MakeNetcdfFile( IOServiceProvider spi, RandomAccessFile raf,
         String location, CancelTask cancelTask ) throws IOException {
            super( spi, raf, location, cancelTask );
      }
   }
}
