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
 * TestGrib2GetData.java
 * @author Robb Kambic  01/08/2009
 *
 */
package ucar.grib.grib2;


import ucar.grib.NoValidGribException;
import ucar.grib.NotSupportedException;
import ucar.grib.GribIndexName;

import ucar.unidata.io.RandomAccessFile;

import java.io.*;
import java.net.URL;
import java.util.*;
import java.text.ParseException;
//import java.io.BufferedOutputStream;  // Input/Output functions
//import java.io.FileNotFoundException;
//import java.io.FileOutputStream;      // Input/Output functions
//import java.io.IOException;
//import java.io.PrintStream;           // Input/Output functions

// import statements
import java.lang.*;  // Standard java functions

import java.util.*;  // Extra utilities from sun


/**
 * Routine to extract data values from a Grib2 file. This routine uses values
 * from a Grib Index file that is created by the Grib2Indexer
 * program.
 * see <a href="../../../IndexFormat.txt"> IndexFormat.txt</a>
 * @see Grib2Data
 *
 */
public final class TestGrib2GetData{


    /**
     *
     * Dumps usage of the class.
     *
     * @param className TestGrib2GetData
     */
    private static void usage(String className) {
        System.out.println();
        System.out.println("Usage of " + className + ":");
        System.out.println("Parameters:");
        System.out.println("<GribFileContainingData> ");
        System.out.println(
            "<OffsetToGds> obtained from Grib2Indexer program");
        System.out.println(
            "<OffsetToPds> obtained from Grib2Indexer program");
        System.out.println("<output file>");
        System.out.println();
        System.out.println(
            "java -Xmx256m ucar/grib/grib2/" + className
            + " <GribFileContainingData> <OffsetToGds> <OffsetToPds>");
        System.exit(0);
    }

    /**
     * Obtain a particular product's data.
     *
     * @param args   Grib 2 file containing the data to be returned,
     *               GDS Offset into file,
     *               PDS Offset into file.
     *               These values can be obtained from a Grib Index file that is
     *               created by the Grib2Indexer program. Also there is information
     *               on the Index in file IndexFormat.txt in the root of the
     *               distribution.
     *
     */
    public static void main(String args[]) {

        // Function References
        TestGrib2GetData func = new TestGrib2GetData();

/*
        // Test usage
        if (args.length < 3) {
            // Get class name as String
            Class cl = func.getClass();
            func.usage(cl.getName());
        }
*/

        // Get UTC TimeZone
        // A list of available ID's show that UTC has ID = 127
        TimeZone tz = TimeZone.getTimeZone("127");
        TimeZone.setDefault(tz);

        // Say hello
        Date now = Calendar.getInstance().getTime();
        //System.out.println(now.toString() + " ... Start of TestGrib2GetData");

        // Reading of grib data must be inside a try-catch block
        try {
            // Create RandomAccessFile instance
            RandomAccessFile raf       = null;
            PrintStream      ps        = System.out;
            long             GdsOffset = 0;
            long             PdsOffset = 0;

            // input file and Gds/Pds Offsets given
            if (args.length == 1) {
                // Create RandomAccessFile
                raf = new RandomAccessFile(args[0], "r");
                raf.order(RandomAccessFile.BIG_ENDIAN);
                //GdsOffset = Long.parseLong(args[1]);
                //PdsOffset = Long.parseLong(args[2]);
            } else {
                System.exit(0);
            }
            if (args.length == 4) {
                ps = new PrintStream(
                    new BufferedOutputStream(
                        new FileOutputStream(args[3], false)));
            }
            Grib2Data g2d    = new Grib2Data(raf);
            // open index file
            InputStream ios = open( GribIndexName.getCurrentSuffix(args[ 0 ] ) );

    long start = System.currentTimeMillis();

    BufferedReader dataIS =
            new BufferedReader(new InputStreamReader(ios));

    // section 1 - global attributes
    while (true) {
      String line = dataIS.readLine();
      if (line == null || line.length() == 0) { // 0 length/corrupted index
        break;
      }
      if (line.startsWith("--")) {
        break;
      }
    }

    // section 2 -- grib records
    while (true) {
      String line = dataIS.readLine();
      if (line == null || line.length() == 0) { // 0 length/corrupted index
        break;
      }
      if (line.startsWith("--")) {
        break;
      }

      StringTokenizer stoke = new StringTokenizer(line);

      String productType = stoke.nextToken();
      String discipline = stoke.nextToken();
      String category = stoke.nextToken();
      String param = stoke.nextToken();
      String typeGenProcess = stoke.nextToken();
      String levelType1 = stoke.nextToken();
      String levelValue1 = stoke.nextToken();
      String levelType2 = stoke.nextToken();
      String levelValue2 = stoke.nextToken();
      String refTime = stoke.nextToken();
      String foreTime = stoke.nextToken();
      String gdsKey = stoke.nextToken();
      GdsOffset = Long.parseLong( stoke.nextToken() );
      PdsOffset = Long.parseLong( stoke.nextToken() );
            float     data[] = g2d.getData(GdsOffset, PdsOffset);

            if (data != null) {
                System.err.println("OK "+ line );
/*
                int row = 0;
                for (int j = 0; j < data.length; j++) {
                    //if( j % 800 == 0 ) // used to test quasi data
                    //ps.println( "row ="+ row++ );
                    ps.println("data[ " + j + " ]=" + data[j]);
                }
*/
            } else {
                System.err.println("No data returned "+ line );
            }
        }
            raf.close();
            ps.close();

            // Catch thrown errors from Grib2GetData
        } catch (FileNotFoundException noFileError) {
            System.err.println("FileNotFoundException : " + noFileError);
        } catch (IOException ioError) {
            System.err.println("IOException : " + ioError);
        }

        // Goodbye message
        now = Calendar.getInstance().getTime();
        System.out.println(now.toString() + " ... End of GribGetDataCheck");

    }  // end main


  /**
   * open Grib Index file for scanning.
   *
   * @param location URL or local filename of Grib Index file
   * @return false if does not match current version; you should regenerate it in that case.
   * @throws IOException
   */
  static public final InputStream open(String location) throws IOException {
    InputStream ios;
    if (location.startsWith("http:")) {
      URL url = new URL(location);
      ios = url.openStream();
    } else {
      ios = new FileInputStream(location);
    }

    return ios;
  }


}  // end TestGrib2GetData
