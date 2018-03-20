/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.writer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.nc2.grib.GribData;
import ucar.nc2.grib.grib1.Grib1RecordScanner;
import ucar.nc2.grib.grib2.Grib2RecordScanner;
import ucar.nc2.grib.grib2.Grib2SectionDataRepresentation;
import ucar.unidata.io.RandomAccessFile;
import ucar.unidata.util.test.TestDir;

import java.io.*;
import java.lang.invoke.MethodHandles;
import java.util.Formatter;

/**
 * Try different compress algorithms on GRIB files
 *
 * @author caron
 * @since 8/12/2014
 */
public class TestGribSize {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  static File outDir;
  static PrintStream summaryOut;


  TestGribSize() {
    makeSummaryHeader();
  }

  private long doGrib2(boolean showDetail, String filename) {
    long nrecords = 0;
    long nDataPoints = 0;
    long nPoints = 0;
    long msgSize = 0;
    long dataSize = 0;
    long bitmapSize = 0;
    long start = System.nanoTime();

    File f = new File(filename);
    long fileSize = f.length();

    try (RandomAccessFile raf = new RandomAccessFile(filename, "r")) {
      Grib2RecordScanner reader = new Grib2RecordScanner(raf);
      while (reader.hasNext()) {
        ucar.nc2.grib.grib2.Grib2Record gr = reader.next();

        //Grib2SectionDataRepresentation drss = gr.getDataRepresentationSection();
        //int template = drss.getDataTemplate();
        // if (template != 40) continue; // skip all but JPEG-2000

        GribData.Info info = gr.getBinaryDataInfo(raf);
        int nBits = info.numberOfBits;
        if (nBits == 0) continue; // skip constant fields

        //float[] fdata = gr.readData(raf);
        long end = System.nanoTime();
        long gribReadTime = end - start;
        start = end;

        nrecords++;
        msgSize += info.msgLength;
        dataSize += info.dataLength;
        bitmapSize += info.bitmapLength;
        nDataPoints += info.ndataPoints;
        nPoints += info.nPoints;
        //readTime += gribReadTime;

        if (nrecords % 1000 == 0) System.out.printf("%d nsecs / record%n", gribReadTime/1000);
      }
    } catch (IOException e) {
      e.printStackTrace();
    }

    if (nrecords > 0)
      summaryReport(filename, 2, fileSize, nPoints, nDataPoints, nrecords, msgSize, dataSize, bitmapSize);

    return nrecords;
  }

  private long doGrib1(boolean showDetail, String filename) {
    long nrecords = 0;
    long nDataPoints = 0;
    long nPoints = 0;
    long msgSize = 0;
    long dataSize = 0;
    long bitmapSize = 0;
    long readTime = 0;
    long start = System.nanoTime();

    File f = new File(filename);
    long fileSize = f.length();

    try (RandomAccessFile raf = new RandomAccessFile(filename, "r")) {
      Grib1RecordScanner reader = new Grib1RecordScanner(raf);
      while (reader.hasNext()) {
        ucar.nc2.grib.grib1.Grib1Record gr = reader.next();

        GribData.Info info = gr.getBinaryDataInfo(raf);
        int nBits = info.numberOfBits;
        if (nBits == 0) continue; // skip constant fields

        //float[] fdata = gr.readData(raf);
        long end = System.nanoTime();
        long gribReadTime = end - start;
        start = end;

        nrecords++;
        msgSize += info.msgLength;
        dataSize += info.dataLength;
        bitmapSize += info.bitmapLength;
        nDataPoints += info.ndataPoints;
        nPoints += info.nPoints;
        readTime += gribReadTime;

        if (nrecords % 1000 == 0) System.out.printf("%d nsecs / record%n", gribReadTime/1000);
      }
    } catch (IOException e) {
      e.printStackTrace();
    }

    if (nrecords > 0)
      summaryReport(filename, 1, fileSize, nPoints, nDataPoints, nrecords, msgSize, dataSize, bitmapSize);

    return nrecords;
  }

  private void makeSummaryHeader() {
    Formatter f = new Formatter();

    f.format("%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s%n", "file", "edition", "fileSize", "nPoints", "nDataPoints", "nrecords", "gribFileSize", "dataSize", "bitMapSize", "ratio", "dataR", "float size", "float/grib");

    System.out.printf("%s%n", f.toString());
    if (summaryOut != null) summaryOut.printf("%s", f.toString());
  }

  private void summaryReport(String filename, int edition, long fileSize, long nPoints, long nDataPoints, long nrecords, long msgSize, long dataSize, long bitmapSize) {
    // double took = ((double)System.nanoTime() - start) * 1.0e-9;

    Formatter f = new Formatter();
    double ratio = ((double)(dataSize+bitmapSize)) / msgSize;
    double dataR = ((double)(dataSize)) / msgSize;
    long floatSize = nPoints * 4;
    double floatRatio = ((double)floatSize) / fileSize;
    f.format("%s, %d, %d, %d, %d, %d, %d, %d, %d, %f, %f, %d, %f%n", filename, edition, fileSize, nPoints, nDataPoints, nrecords, msgSize, dataSize, bitmapSize, ratio, dataR, floatSize, floatRatio);

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
      if (filename.endsWith("grib2"))
        readSizes.doGrib2(showDetail, filename);
      else
        readSizes.doGrib1(showDetail, filename);
      return 1;
    }
  }


  public static void main(String[] args) throws IOException {
    outDir = new File("E:/grib2nc/size2/");
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

  public static void main2(String[] args) throws IOException {
    outDir = new File("E:/grib2nc/size/");
    outDir.mkdirs();
    summaryOut = new PrintStream(new File(outDir, "size.csv"));

    try {
      TestGribSize readSizes = new TestGribSize();
      ReportAction test = new ReportAction(readSizes, true);
      test.doAct("Q:/cdmUnitTest/tds/ncep/SREF_CONUS_40km_pgrb_biasc_rsm_p2_20120213_1500.grib2");

    } finally {
      summaryOut.close();
    }
  }

  private static class MyFileFilter implements FileFilter {

    @Override
    public boolean accept(File pathname) {
      String name = pathname.getName();
      // if (name.contains("GEFS_Global_1p0deg_Ensemble")) return false; // too big
      return name.endsWith(".grib1") || name.endsWith(".grib2");
    }
  }

}
