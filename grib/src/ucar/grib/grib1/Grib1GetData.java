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

// $Id: Grib1GetData.java,v 1.14 2005/12/13 22:58:55 rkambic Exp $


package ucar.grib.grib1;


import ucar.grib.*;

import ucar.unidata.io.RandomAccessFile;

import java.io.BufferedOutputStream;  // Input/Output functions
import java.io.FileNotFoundException;
import java.io.FileOutputStream;      // Input/Output functions
import java.io.IOException;
import java.io.PrintStream;           // Input/Output functions

/**
 * Grib1GetData.java
 *
 * @author Robb Kambic  08/31/04 .
 *
 */

// import statements
import java.lang.*;  // Standard java functions

import java.util.*;  // Extra utilities from sun


/**
 * Routine to extract data values from a Grib1 file. This routine uses values
 * from a Grib Index file that is created by the Grib1Indexer
 * program.
 * see <a href="../../../IndexFormat.txt"> IndexFormat.txt</a>
 *
 * @see Grib1Data
 */
public final class Grib1GetData {


  /**
   * Dumps usage of the class.
   *
   * @param className Grib1GetData
   */
  private static void usage(String className) {
    System.out.println();
    System.out.println("Usage of " + className + ":");
    System.out.println("Parameters:");
    System.out.println("<GribFileContainingData> ");
    System.out.println("<GDSOffset> obtained from GribIndexer ");
    System.out.println("<DataOffset> obtained from GribIndexer ");
    System.out.println("<DeciamlScale> obtained from GribIndexer ");
    System.out.println("<true/false> does bmsExists from Grib1Indexer ");
    //System.out.println("<output file>");
    System.out.println();
    System.out.println(
        "java -Xmx256m ucar/grib/grib1/" + className
            + " <GribFileContainingData> <OffsetToData> <DecimalScale> <output file>");
    System.exit(0);
  }

  /**
   * Obtain a particular product's data from a Grib1 file.
   *
   * @param args Grib 1 file containing the data to be returned,
   *             Offset into file where to start decoding the data,
   *             Decimal scale value, (true|false)  does the BMS exists,
   *             optional output file.
   *             These values can be obtained from a Grib Index file that is
   *             created by the Grib1Indexer program. Also there is information
   *             on the Index in file IndexFormat.txt in the root of the
   *             distribution.
   */
  public static void main(String args[]) {

    // Function References
    Grib1GetData func = new Grib1GetData();

    // Test usage
    if (args.length < 4) {
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
    //System.out.println(now.toString() + " ... Start of Grib1GetData");

    // Reading of grib data must be inside a try-catch block
    try {
      // Create RandomAccessFile instance
      RandomAccessFile raf = null;
      PrintStream ps = System.out;
      long offset1 = 0;
      long offset2 = 0;
      boolean bmsExists = true;
      int decimalScale = 1;
      // input file, offset, decimalScale given
      // Create RandomAccessFile
      raf = new RandomAccessFile(args[0], "r");
      raf.order(RandomAccessFile.BIG_ENDIAN);
      offset1 = Long.decode(args[1]).longValue();
      //if ((args.length == 4) || (args.length == 5)) {
      if ((args.length == 4)) {
        decimalScale = Integer.parseInt(args[2]);
        Boolean B = Boolean.valueOf(args[3]);
        bmsExists = B.booleanValue();
      } else if ((args.length == 5)) {
        offset2 = Long.decode(args[2]).longValue();
        decimalScale = Integer.parseInt(args[3]);
        Boolean B = Boolean.valueOf(args[4]);
        bmsExists = B.booleanValue();
      } else if ((args.length == 6)) {
        offset2 = Long.decode(args[2]).longValue();
        decimalScale = Integer.parseInt(args[3]);
        Boolean B = Boolean.valueOf(args[4]);
        bmsExists = B.booleanValue();
        ps = new PrintStream(
          new BufferedOutputStream(
          new FileOutputStream(args[5], false)));
      } else {
        System.exit(0);
      }
      long start = System.currentTimeMillis();
      Grib1Data g1d = new Grib1Data(raf);
      float[] data;
      if ((args.length == 4)) {
        data = g1d.getData(offset1, decimalScale, bmsExists);
      } else {
        data = g1d.getData(offset1, offset2, decimalScale, bmsExists);
      }
      System.out.println("getting data size " + data.length + " took "
          + (System.currentTimeMillis() - start) + " msec");

      if (data != null) {
        //ps.println("length ="+ data.length );
        for (int j = 0; j < data.length; j++) {
          ps.println("data[ " + j + " ]=" + data[j]);
        }
      }
      raf.close();
      ps.close();

      // Catch thrown errors from Grib1GetData
    } catch (FileNotFoundException noFileError) {
      System.err.println("FileNotFoundException : " + noFileError);
    } catch (IOException ioError) {
      System.err.println("IOException : " + ioError);
    }

    // Goodbye message
    now = Calendar.getInstance().getTime();
    //System.out.println(now.toString() + " ... End of Grib1GetData!");

  }  // end main
}  // end Grib1GetData


