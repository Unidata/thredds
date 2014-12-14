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

import SevenZip.LzmaAlone;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream;
import org.tukaani.xz.LZMA2Options;
import org.tukaani.xz.XZInputStream;
import org.tukaani.xz.XZOutputStream;
import ucar.nc2.grib.GribData;
import ucar.nc2.grib.collection.Grib2CollectionBuilder;
import ucar.nc2.grib.grib2.Grib2Record;
import ucar.nc2.grib.grib2.Grib2RecordScanner;
import ucar.nc2.grib.grib2.Grib2SectionDataRepresentation;
import ucar.nc2.grib.grib2.table.Grib2Customizer;
import ucar.nc2.grib.writer.Grib2NetcdfWriter;
import ucar.nc2.util.IO;
import ucar.nc2.util.Misc;
import ucar.unidata.io.RandomAccessFile;
import ucar.unidata.test.util.TestDir;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Formatter;
import java.util.List;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

/**
 * Try different compress algorithms on GRIB files
 *
 * @author caron
 * @since 8/12/2014
 */
public class TestGribSize {

  static File outDir;
  static PrintStream summaryOut;


  TestGribSize() {
    makeSummaryHeader();
  }

  private int doGrib2(boolean showDetail, String filename) {
    int nrecords = 0;
    int npoints = 0;
    long msgSize = 0;
    long dataSize = 0;
    long bitmapSize = 0;
    long readTime = 0;

    try (RandomAccessFile raf = new RandomAccessFile(filename, "r")) {
      Grib2RecordScanner reader = new Grib2RecordScanner(raf);
      while (reader.hasNext()) {
        ucar.nc2.grib.grib2.Grib2Record gr = reader.next();

        Grib2SectionDataRepresentation drss = gr.getDataRepresentationSection();
        int template = drss.getDataTemplate();
        if (template != 40) continue; // skip all but JPEG-2000

        GribData.Info info = gr.getBinaryDataInfo(raf);
        int nBits = info.numberOfBits;
        if (nBits == 0) continue; // skip constant fields

        long start2 = System.nanoTime();
        float[] fdata = gr.readData(raf);
        long end2 = System.nanoTime();
        long gribReadTime = end2 - start2;
        if (npoints == 0) {
          npoints = fdata.length;
        }

        nrecords++;
        msgSize += info.msgLength;
        dataSize += info.dataLength;
        bitmapSize += info.bitmapLength;
        readTime += gribReadTime;
      }
    } catch (IOException e) {
      e.printStackTrace();
    }

    if (nrecords > 0)
      summaryReport(filename, npoints, nrecords, readTime, msgSize, dataSize, bitmapSize);

    return nrecords;
  }

  private void makeSummaryHeader() {
    Formatter f = new Formatter();

    f.format("%s, %s, %s, %s, %s, %s, %s, %s%n", "file", "npoints", "nrecords", "readTime", "gribFileSize", "dataSize", "bitMapSize", "ratio");

    System.out.printf("%s%n", f.toString());
    if (summaryOut != null) summaryOut.printf("%s", f.toString());
  }

  private void summaryReport(String filename, int npoints, int nrecords, long readTime, long msgSize, long dataSize, long bitmapSize) {
    // double took = ((double)System.nanoTime() - start) * 1.0e-9;

    Formatter f = new Formatter();
    double ratio = ((double)(dataSize+bitmapSize)) / msgSize;
    f.format("%s, %d, %d, %d, %d, %d, %d, %f%n", filename, npoints, nrecords, readTime, msgSize, dataSize, bitmapSize, ratio);

    System.out.printf("%s%n", f.toString());
    summaryOut.printf("%s", f.toString());
    summaryOut.flush();
  }


  private static class ReportAction implements TestDir.Act {
    TestGribSize readSizes;
    boolean showDetail;

    private ReportAction(TestGribSize readSizes, boolean showDetail) {
      this.readSizes = readSizes;
      this.showDetail = showDetail;
    }

    public int doAct(String filename) throws IOException {
      readSizes.doGrib2(showDetail, filename);
      return 1;
    }
  }



  public static void main(String[] args) throws IOException {
    outDir = new File("E:/grib2nc/size/");
    outDir.mkdirs();
    summaryOut = new PrintStream(new File(outDir, "summary.csv"));

    try {
      TestGribSize readSizes = new TestGribSize();
      ReportAction test = new ReportAction(readSizes, true);
      String dirName = "Q:/cdmUnitTest/tds/ncep/";
      TestDir.actOnAll(dirName, new MyFileFilter(), test);

    } finally {
      summaryOut.close();
    }
  }

  private static class MyFileFilter implements FileFilter {

    @Override
    public boolean accept(File pathname) {
      String name = pathname.getName();
      if (name.contains("GEFS_Global_1p0deg_Ensemble")) return false; // too big
      return name.endsWith(".grib2");
    }
  }

}
