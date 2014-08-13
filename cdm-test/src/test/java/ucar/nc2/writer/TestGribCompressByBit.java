/*
 * Copyright 1998-2014 University Corporation for Atmospheric Research/Unidata
 *
 *   Portions of this software were developed by the Unidata Program at the
 *   University Corporation for Atmospheric Research.
 *
 *   Access and use of this software shall impose the following obligations
 *   and understandings on the user. The user is granted the right, without
 *   any fee or cost, to use, copy, modify, alter, enhance and distribute
 *   this software, and any derivative works thereof, and its supporting
 *   documentation for any purpose whatsoever, provided that this entire
 *   notice appears in all copies of the software, derivative works and
 *   supporting documentation.  Further, UCAR requests that the user credit
 *   UCAR/Unidata in any publications that result from the use of this
 *   software or in any product that includes this software. The names UCAR
 *   and/or Unidata, however, may not be used in any advertising or publicity
 *   to endorse or promote any products or commercial entity unless specific
 *   written permission is obtained from UCAR/Unidata. The user also
 *   understands that UCAR/Unidata is not obligated to provide the user with
 *   any support, consulting, training or assistance of any kind with regard
 *   to the use, operation and performance of this software nor to provide
 *   the user with any updates, revisions, new versions or "bug fixes."
 *
 *   THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 *   IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *   WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *   DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 *   INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 *   FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 *   NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 *   WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */

package ucar.nc2.writer;

import ucar.nc2.grib.grib1.Grib1RecordScanner;
import ucar.nc2.grib.grib1.Grib1SectionBinaryData;
import ucar.unidata.io.RandomAccessFile;
import ucar.unidata.test.util.TestDir;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.ByteBuffer;
import java.util.zip.Deflater;

/**
 * Describe
 *
 * @author caron
 * @since 8/12/2014
 */
public class TestGribCompressByBit {
  static int deflate_level = 3;
  static String dirOut;
  static String csvOut;
  static PrintStream fw;
  static long totalIn, totalOut, totalRecords;

  private int doGrib1(String filename)  {
    int count = 0;
    try (RandomAccessFile raf = new RandomAccessFile(filename, "r")) {
      Grib1RecordScanner reader = new Grib1RecordScanner(raf);
      while (reader.hasNext()) {
        ucar.nc2.grib.grib1.Grib1Record gr = reader.next();

        Grib1SectionBinaryData dataSection = gr.getDataSection();
        Grib1SectionBinaryData.BinaryDataInfo info = dataSection.getBinaryDataInfo(raf);

        float[] data = gr.readData(raf);
        int compressBytes = deflate(data); // run it through the deflator
        float r = ((float) compressBytes) / info.msgLength;

        fw.printf("%d, %d, %d, %d, %f%n", info.numbits, data.length, info.msgLength, compressBytes, r);
        System.out.printf("%d, %d, %d, %d, %f%n", info.numbits, data.length, info.msgLength, compressBytes, r);
        count++;
        totalIn += info.msgLength;
        totalOut += compressBytes;
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
    return count;
  }

  private int deflate(float[] data) {
    Deflater compresser = new Deflater(deflate_level);
    ByteBuffer bb = ByteBuffer.allocate(data.length * 4);
    for (float d : data)  bb.putFloat(d);

    compresser.setInput(bb.array());
    compresser.finish();

    byte[] output = new byte[data.length * 4];
    int compressedDataLength = compresser.deflate(output);
    compresser.end();
    return compressedDataLength;
  }

  private static class CompressByBit implements TestDir.Act {

    public int doAct(String filename) throws IOException {

      TestGribCompressByBit compressByBit = new TestGribCompressByBit();
      totalRecords += compressByBit.doGrib1(filename);

      fw.flush();

      return 1;
    }
  }

  static  void  writeHeader() throws FileNotFoundException {
     File outDir = new File(dirOut);
     fw = new PrintStream(new File(outDir, csvOut));
     fw.printf("nbits, npoints, grib, deflate, ratio%n");
   }

  public static void main2(String[] args) throws IOException {
    dirOut = "G:/write/";
    csvOut = "results.grib.csv";
    writeHeader();

    try {
      String dirName = "Q:/cdmUnitTest/tds/ncep/";
      TestDir.actOnAll(dirName, new TestDir.FileFilterFromSuffixes("grib1"), new CompressByBit(), false);
      System.out.printf("%ntotalRecords = %d%n", totalRecords);

    } finally {
      fw.close();
    }
  }

  public static void main(String[] args) throws IOException {
    dirOut = "G:/write3/";
    csvOut = "results.grib.csv";
    writeHeader();

    try {
      String filename = "Q:/cdmUnitTest/tds/ncep/NAM_CONUS_20km_noaaport_20100602_0000.grib1";
      TestGribCompressByBit compressByBit = new TestGribCompressByBit();
      totalRecords += compressByBit.doGrib1(filename);
      System.out.printf("%ntotalRecords = %d%n", totalRecords);
      System.out.printf("totalIn = %d%n", totalIn);
      System.out.printf("totalOut = %d%n", totalOut);

      double r = ((double) totalOut) / totalIn;
      System.out.printf("ratio = %f%n", r);

    } finally {
      fw.close();
    }
  }

}
