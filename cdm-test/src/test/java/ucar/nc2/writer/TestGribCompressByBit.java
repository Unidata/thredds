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
import ucar.nc2.grib.grib2.Grib2Drs;
import ucar.nc2.grib.grib2.Grib2RecordScanner;
import ucar.nc2.grib.grib2.Grib2SectionData;
import ucar.nc2.grib.grib2.Grib2SectionDataRepresentation;
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
  static File outDir;
  static int deflate_level = 3;
  static PrintStream detailOut;
  static PrintStream summaryOut;
  static final boolean debug = false;

  int npoints;
  int nrecords = 0;
  float totDeflate;
  float totOrg;
  float totalBytesIn;

  private int doGrib2(String filename)  {
    try (RandomAccessFile raf = new RandomAccessFile(filename, "r")) {
      Grib2RecordScanner reader = new Grib2RecordScanner(raf);
      while (reader.hasNext()) {
        ucar.nc2.grib.grib2.Grib2Record gr = reader.next();

        Grib2SectionData dataSection = gr.getDataSection();
        int gribMsgLength = dataSection.getMsgLength();

        Grib2SectionDataRepresentation drss = gr.getDataRepresentationSection();
        int template = drss.getDataTemplate();
        if (template != 40) continue; // skip all but JPEG-2000

        Grib2Drs gdrs = drss.getDrs(raf);
        int nBits = gdrs.getNBits();


        float[] data = gr.readData(raf);
        int compressDeflate = deflate(data); // run it through the deflator
        if (npoints == 0) npoints = data.length;

         // deflate the original (packed) data
        int compressOrg = 1; // deflate(dataSection.getBytes(raf)); // run it through the deflator

        // results
        if (detailOut != null) detailOut.printf("%d, %d, %d, %d, %d%n", nBits, data.length, gribMsgLength, compressDeflate, compressOrg);
        if (debug) System.out.printf("%d, %d, %d, %d, %d%n", nBits, data.length, gribMsgLength, compressDeflate, compressOrg);
        nrecords++;
        totalBytesIn += gribMsgLength;
        totDeflate += compressDeflate;
        totOrg += compressOrg;
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
    return nrecords;
  }

  private int doGrib1(String filename)  {
    try (RandomAccessFile raf = new RandomAccessFile(filename, "r")) {
      Grib1RecordScanner reader = new Grib1RecordScanner(raf);
      while (reader.hasNext()) {
        ucar.nc2.grib.grib1.Grib1Record gr = reader.next();

        Grib1SectionBinaryData dataSection = gr.getDataSection();
        Grib1SectionBinaryData.BinaryDataInfo info = dataSection.getBinaryDataInfo(raf);

        float[] data = gr.readData(raf);
        int compressDeflate = deflate(data); // run it through the deflator
        if (npoints == 0) npoints = data.length;

         // deflate the original (bit-packed) data
        int compressOrg = deflate(dataSection.getBytes(raf)); // run it through the deflator

        // results
        if (detailOut != null) detailOut.printf("%d, %d, %d, %d, %d%n", info.numbits, data.length, info.msgLength, compressDeflate, compressOrg);
        //System.out.printf("%d, %d, %d, %d, %f, %d%n", info.numbits, data.length, info.msgLength, compressBytes, r, compressOrg);
        nrecords++;
        totalBytesIn += info.msgLength;
        totDeflate += compressDeflate;
        totOrg += compressOrg;
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
    return nrecords;
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

  private int deflate(byte[] data) {
    Deflater compresser = new Deflater(deflate_level);
    compresser.setInput(data);
    compresser.finish();

    byte[] output = new byte[data.length * 2];
    int compressedDataLength = compresser.deflate(output);
    compresser.end();
    return compressedDataLength;
  }

  private static class CompressByBit implements TestDir.Act {
    boolean showDetail;
    boolean isGrib1;

    private CompressByBit(boolean showDetail, boolean isGrib1) {
      this.showDetail = showDetail;
      this.isGrib1 = isGrib1;
    }

    public int doAct(String filename) throws IOException {
      if (showDetail) {
        File f = new File(filename);
        detailOut = new PrintStream(new File(outDir, f.getName()+".csv"));
        detailOut.printf("nbits, npoints, grib, deflate, deflateOrg%n");
      }

      TestGribCompressByBit compressByBit = new TestGribCompressByBit();
      if (isGrib1)
        compressByBit.doGrib1(filename);
      else
        compressByBit.doGrib2(filename);


      summaryOut.printf("%s, %d, %d, %f, %f, %f%n", filename, compressByBit.npoints, compressByBit.nrecords, compressByBit.totalBytesIn/compressByBit.nrecords,
              compressByBit.totDeflate/compressByBit.nrecords, compressByBit.totOrg/compressByBit.nrecords);
      summaryOut.flush();

      if (detailOut != null) {
        detailOut.close();
      }

      return 1;
    }
  }

  static void writeSummaryHeader() throws FileNotFoundException {
    summaryOut.printf("file, npoints, nrecords, avgGribSize, avgDeflate, avgOrg%n");
   }

  public static void main(String[] args) throws IOException {
    outDir = new File("G:/grib2nc/grib2/");
    summaryOut = new PrintStream(new File(outDir, "summary.csv"));
    writeSummaryHeader();

    try {
      CompressByBit test = new CompressByBit(true, false);
      test.doAct("Q:/cdmUnitTest/tds/ncep/GFS_Global_0p5deg_20100913_0000.grib2");

    } finally {
      summaryOut.close();
    }
  }

  public static void main2(String[] args) throws IOException {
    outDir = new File("G:/grib2nc/grib2/");
    summaryOut = new PrintStream(new File(outDir, "summary.csv"));
    writeSummaryHeader();

    try {
      String dirName = "Q:/cdmUnitTest/tds/ncep/";
      TestDir.actOnAll(dirName, new TestDir.FileFilterFromSuffixes("grib2"), new CompressByBit(true, false), false);

    } finally {
      summaryOut.close();
    }
  }

}

/*

GFS_Alaska_191km_20100913_0000.grib1
GFS_CONUS_191km_20100519_1800.grib1
GFS_CONUS_80km_20100513_0600.grib1
GFS_CONUS_95km_20100506_0600.grib1
GFS_Hawaii_160km_20100428_0000.grib1
GFS_N_Hemisphere_381km_20100516_0600.grib1
GFS_Puerto_Rico_191km_20100515_0000.grib1
NAM_Alaska_22km_20100504_0000.grib1
NAM_Alaska_45km_noaaport_20100525_1200.grib1
NAM_Alaska_95km_20100502_0000.grib1
NAM_CONUS_20km_noaaport_20100602_0000.grib1
NAM_CONUS_80km_20100508_1200.grib1
RUC2_CONUS_40km_20100515_0200.grib1
RUC2_CONUS_40km_20100914_1200.grib1
RUC_CONUS_80km_20100430_0000.grib1
 */
