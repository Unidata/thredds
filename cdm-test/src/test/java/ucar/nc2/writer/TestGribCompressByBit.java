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

import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream;
import org.tukaani.xz.LZMA2Options;
import org.tukaani.xz.XZOutputStream;
import ucar.ma2.DataType;
import ucar.nc2.grib.grib2.Grib2Drs;
import ucar.nc2.grib.grib2.Grib2RecordScanner;
import ucar.nc2.grib.grib2.Grib2SectionData;
import ucar.nc2.grib.grib2.Grib2SectionDataRepresentation;
import ucar.nc2.util.IO;
import ucar.unidata.io.RandomAccessFile;
import ucar.unidata.test.util.TestDir;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;
import java.util.zip.Deflater;

/**
 * Try different compress algorithms on GRIB files
 *
 * @author caron
 * @since 8/12/2014
 */
public class TestGribCompressByBit {

  static enum Algo {deflate, bzip2, bzip2T, xy}

  static File outDir;
  static int deflate_level = 3;
  static PrintStream detailOut;
  static PrintStream summaryOut;
  static final int showEvery = 1;

  List<CompressAlgo> runAlg = new ArrayList<>();

  TestGribCompressByBit(Algo... wantAlgs) {
    for (Algo want : wantAlgs) {
      switch (want) {
        case deflate: runAlg.add( new JavaDeflate()); break;
        case bzip2: runAlg.add( new ApacheBzip2()); break;
        case bzip2T: runAlg.add( new ItadakiBzip2()); break;
        case xy: runAlg.add( new TukaaniLZMA2()); break;
      }
    }
    makeSummaryHeader();
  }

  int nfiles = 0;
  int tot_nrecords = 0;
  double file_gribsize;

  void reset() {
    for (CompressAlgo alg : runAlg) alg.reset();
  }

  private int doGrib2(String filename)  {
    int nrecords = 0;
    int npoints = 0;
    long start = System.nanoTime();
    file_gribsize = 0.0;

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

        float[] fdata = gr.readData(raf);
        if (npoints == 0) npoints = fdata.length;
        byte[] bdata = convertToBytes(fdata);

        int[] rawData = gr.readRawData(raf);

        detailReport(nBits, gribMsgLength, fdata, bdata, rawData);

        nrecords++;
        tot_nrecords++;
        file_gribsize += gribMsgLength;
      }
    } catch (IOException e) {
      e.printStackTrace();
    }

    summaryReport(filename, start, npoints, nrecords);

    nfiles++;
    return nrecords;
  }

  private void makeDetailHeader(boolean showDetail, String filename) throws FileNotFoundException {
    if (showDetail) {
      File f = new File(filename);
      detailOut = new PrintStream(new File(outDir, f.getName()+".csv"));
    } else
      detailOut = null;

    Formatter f = new Formatter();
    f.format("%s, %s, %s, %s, ", "nbits", "grib", "entropyI", "entropyB") ;

    for (CompressAlgo alg : runAlg) {
      f.format("%s, %s, %s, %s, ", alg.getAlgo(), "msecs", "ratio", "entropy") ;
    }
    f.format("%n");

    // results
    if (detailOut != null) detailOut.printf("%s", f.toString());
    System.out.printf("%s", f.toString());
  }

  private void detailReport(int nbits, int gribMsgLength, float[] fdata, byte[] bdata, int[] rawData) throws IOException {
    Formatter f = new Formatter();

    double entropyI = entropy(nbits, rawData);
    double entropyB = entropy(bdata);
    f.format("%d, %d, %f, %f, ", nbits, bdata.length, entropyI, entropyB) ;

    for (CompressAlgo alg : runAlg) {
      float size = (float) alg.compress(bdata);
      float ratio = size/ gribMsgLength;
      f.format("%d, %d, %f, %f, ", alg.size, alg.msecs/1000/1000, ratio, alg.entropy) ;
    }
    f.format("%n");

    // results
    if (detailOut != null) detailOut.printf("%s", f.toString());
    if (tot_nrecords % showEvery == 0) System.out.printf("%s", f.toString());
  }

  private void makeSummaryHeader() {
    Formatter f = new Formatter();
    f.format("%s, %s, %s, %s, %s, ", "file", "npoints", "nrecords", "msecs", "grib") ;

    for (CompressAlgo alg : runAlg) {
      f.format("%s, %s, %s,", alg.getAlgo(), "msecs", "ratio") ;
    }
    f.format("%n");

    // results
    if (summaryOut != null) summaryOut.printf("%s", f.toString());
    reset();
  }

  private void summaryReport(String filename, long start, int npoints, int nrecords) {
    long took = System.currentTimeMillis() - start;

    Formatter f = new Formatter();
    f.format("%s, %d, %d, %d, %f, %n", filename, npoints, nrecords, took, file_gribsize);

    for (CompressAlgo alg : runAlg) {
      double ratio = ((double)alg.tot_size)/ file_gribsize;
      f.format("%d, %d, %f,", alg.tot_size, alg.tot_time/1000/1000, ratio) ;
    }
    f.format("%n");

    summaryOut.printf("%s", f.toString());
    summaryOut.flush();
  }

  //////////////////////////////////////////////////////////////////////
  abstract class CompressAlgo {
    long msecs;
    int size;
    int tot_size;
    long tot_time;
    double entropy;

    int compress(byte[] bdata) throws IOException {
      long start = System.nanoTime();
      byte[] result = reallyCompress(bdata);
      entropy = entropy(result);

      tot_size += size;
      long end = System.nanoTime();
      msecs = end - start;
      tot_time += msecs;
      return size;
    }

    void reset() {
      this.msecs = 0;
      this.size = 0;
      this.tot_size = 0;
      this.tot_time = 0;
    }

    abstract byte[] reallyCompress(byte[] data) throws IOException;
    abstract Algo getAlgo();
  }

  class JavaDeflate extends CompressAlgo {
    Algo getAlgo() { return Algo.deflate; }
    byte[] reallyCompress(byte[] data) {
      Deflater compresser = new Deflater(deflate_level);
      compresser.setInput(data);
      compresser.finish();

      byte[] output = new byte[data.length * 2];
      size = compresser.deflate(output);
      compresser.end();

      // copy just the resulting bytes
      byte[] out = new byte[size];
      System.arraycopy(output, 0, out, 0, size);
      return out;
    }
  }

    /* private int deflateShave(float[] data, int bitN) {
    int bitMask = Grib2NetcdfWriter.getBitMask(bitN+1);
    Deflater compresser = new Deflater(deflate_level);
    ByteBuffer bb = ByteBuffer.allocate(data.length * 4);
    for (float d : data)
      bb.putFloat(Grib2NetcdfWriter.bitShave(d, bitMask));

    compresser.setInput(bb.array());
    compresser.finish();

    byte[] output = new byte[data.length * 4];
    int compressedDataLength = compresser.deflate(output);
    compresser.end();
    return compressedDataLength;
  } */

  class ApacheBzip2 extends CompressAlgo {
    Algo getAlgo() { return Algo.bzip2; }

    byte[] reallyCompress(byte[] bdata) {

      ByteArrayOutputStream out = new ByteArrayOutputStream(bdata.length);
      try (BZip2CompressorOutputStream bzOut = new BZip2CompressorOutputStream(out)) {
        bzOut.write(bdata, 0, bdata.length);
        bzOut.finish();
        size = out.size();

      } catch (IOException e) {
        e.printStackTrace();
      }

      return out.toByteArray();
    }
  }

  class ItadakiBzip2 extends CompressAlgo {
    Algo getAlgo() { return Algo.bzip2T; }

    byte[] reallyCompress(byte[] bdata) throws IOException {
      int orgSize = bdata.length;
      ByteArrayOutputStream out = new ByteArrayOutputStream(2 * orgSize);
      org.itadaki.bzip2.BZip2OutputStream zipper = new org.itadaki.bzip2.BZip2OutputStream(out);
      InputStream fin = new ByteArrayInputStream(bdata);
      IO.copy(fin, zipper);
      zipper.close();
      size = out.size();
      return out.toByteArray();
    }
  }

  class TukaaniLZMA2 extends CompressAlgo {
    Algo getAlgo() { return Algo.xy; }

    byte[] reallyCompress(byte[] bdata) throws IOException {
      ByteArrayOutputStream out = new ByteArrayOutputStream(bdata.length * 2);
      try (XZOutputStream compressor = new XZOutputStream(out, new LZMA2Options())) {
        compressor.write(bdata, 0, bdata.length);
        compressor.finish();

        size = out.size();

      } catch (IOException e) {
        e.printStackTrace();
      }

      return out.toByteArray();
    }
  }

  private double entropy(byte[] data) {
    int[] p = new int[256];

    // count occurrences
    for (byte b : data)
      p[DataType.unsignedByteToShort(b)]++;

    double n = data.length;
    double iln2 = 1.0 / Math.log(2.0);
    double sum = 0.0;
    for (int i=0; i<256; i++) {
      if (p[i] == 0) continue;
      double prob = ((double) p[i]) / n;
      sum += Math.log(prob) * prob * iln2;
    }

    return (sum == 0) ? 0.0 : -sum;
  }

  private double entropy(int nbits, int[] data) {
    if (data == null) return 0.0;

    int n = (int) Math.pow(2, nbits);
    int[] p = new int[n];

    // count occurrences
    for (int b : data) {
      if (b > n-1)
        System.out.printf("BAD %d max=%d%n", b, n-1);
      else
        p[b]++;
    }

    double len = data.length;
    double iln2 = 1.0 / Math.log(2.0);
    double sum = 0.0;
    for (int i=0; i<n; i++) {
      if (p[i] == 0) continue;
      double prob = ((double) p[i]) / len;
      sum += Math.log(prob) * prob * iln2;
    }

    return (sum == 0) ? 0.0 : -sum;
  }

  private byte[] convertToBytes(float[] data) {
    ByteBuffer bb = ByteBuffer.allocate(data.length * 4);
    for (float val : data)  bb.putFloat(val);
    return bb.array();
  }

  private byte[] convertToBytes(int[] data) {
    ByteBuffer bb = ByteBuffer.allocate(data.length * 4);
    for (int val : data)  bb.putInt(val);
    return bb.array();
  }


  /* private int doGrib1(String filename)  {
    try (RandomAccessFile raf = new RandomAccessFile(filename, "r")) {
      Grib1RecordScanner reader = new Grib1RecordScanner(raf);
      while (reader.hasNext()) {
        ucar.nc2.grib.grib1.Grib1Record gr = reader.next();

        Grib1SectionBinaryData dataSection = gr.getDataSection();
        Grib1SectionBinaryData.BinaryDataInfo info = dataSection.getBinaryDataInfo(raf);

        float[] data = gr.readData(raf);
        int compressDeflate = deflate( convertToBytes(data)); // run it through the deflator
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
  }  */



  private static class CompressReportAction implements TestDir.Act {
    TestGribCompressByBit compressByBit;
    boolean showDetail;
    boolean isGrib1;

    private CompressReportAction(TestGribCompressByBit compressByBit, boolean showDetail, boolean isGrib1) {
      this.compressByBit = compressByBit;
      this.showDetail = showDetail;
      this.isGrib1 = isGrib1;
    }

    public int doAct(String filename) throws IOException {
      compressByBit.makeDetailHeader(showDetail, filename);
      compressByBit.doGrib2(filename);

      if (detailOut != null)
        detailOut.close();

      return 1;
    }
  }


  public static void main(String[] args) throws IOException {
    outDir = new File("G:/grib2nc/compress/");
    outDir.mkdirs();
    summaryOut = new PrintStream(new File(outDir, "summary.csv"));

    try {
      TestGribCompressByBit compressByBit = new TestGribCompressByBit(Algo.deflate, Algo.bzip2, Algo.bzip2T, Algo.xy);
      CompressReportAction test = new CompressReportAction(compressByBit, true, false);
      test.doAct("Q:\\cdmUnitTest\\tds\\ncep\\NAM_CONUS_20km_surface_20100913_0000.grib2");

    } finally {
      summaryOut.close();
    }
  }

  /* public static void main2(String[] args) throws IOException {
    outDir = new File("G:/grib2nc/bzip2/");
    summaryOut = new PrintStream(new File(outDir, "summary.csv"));
    writeSummaryHeader();

    try {
      String dirName = "Q:/cdmUnitTest/tds/ncep/";
      TestDir.actOnAll(dirName, new TestDir.FileFilterFromSuffixes("grib2"), new CompressReportAction(true, false), false);

    } finally {
      summaryOut.close();
    }
  } */

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

-----------------
D = has duplicates

DRS template
     0: count = 30
     2: count = 283
     3: count = 15017
    40: count = 384980


    DGEX_Alaska_12km_20100524_0000.grib2
    DGEX_CONUS_12km_20100514_1800.grib2
    GEFS_Global_1p0deg_Ensemble_20120215_0000.grib2
    GEFS_Global_1p0deg_Ensemble_derived_20120214_0000.grib2
    GFS_Global_0p5deg_20100913_0000.grib2
    GFS_Global_0p5deg_20140804_0000.grib2
3   GFS_Global_2p5deg_20100602_1200.grib2
    GFS_Global_onedeg_20100913_0000.grib2
    GFS_Puerto_Rico_0p5deg_20140106_1800.grib2
3   HRRR_CONUS_3km_wrfprs_201408120000.grib2
    NAM_Alaska_11km_20100519_0000.grib2
    NAM_Alaska_45km_conduit_20100913_0000.grib2
    NAM_CONUS_12km_20100915_1200.grib2
    NAM_CONUS_12km_20140804_0000.grib2
    NAM_CONUS_12km_conduit_20140804_0000.grib2
    NAM_CONUS_20km_selectsurface_20100913_0000.grib2
    NAM_CONUS_20km_surface_20100913_0000.grib2
    NAM_CONUS_40km_conduit_20100913_0000.grib2
    NAM_Firewxnest_20140804_0000.grib2
    NAM_Polar_90km_20100913_0000.grib2
2   NDFD_CONUS_5km_20140805_1200.grib2
2/3 NDFD_CONUS_5km_conduit_20140804_0000.grib2
2   NDFD_Fireweather_CONUS_20140804_1800.grib2
    RR_CONUS_13km_20121028_0000.grib2
    RR_CONUS_20km_20140804_1900.grib2
    RR_CONUS_40km_20140805_1600.grib2
0/2 RTMA_CONUS_2p5km_20111221_0800.grib2
0   RTMA_GUAM_2p5km_20140803_0600.grib2
    RUC2_CONUS_20km_hybrid_20100913_0000.grib2
    RUC2_CONUS_20km_pressure_20100509_1300.grib2
    RUC2_CONUS_20km_surface_20100516_1600.grib2
    SREF_Alaska_45km_ensprod_20120213_1500.grib2
    SREF_CONUS_40km_ensprod_20120214_1500.grib2
    SREF_CONUS_40km_ensprod_20140804_1500.grib2
    SREF_CONUS_40km_ensprod_biasc_20120213_2100.grib2
D   SREF_CONUS_40km_ensprod_biasc_20140805_1500.grib2
    SREF_CONUS_40km_pgrb_biasc_nmm_n2_20100602_1500.grib2
    SREF_CONUS_40km_pgrb_biasc_rsm_p2_20120213_1500.grib2
    SREF_PacificNE_0p4_ensprod_20120213_2100.grib2
D   WW3_Coastal_Alaska_20140804_0000.grib2
D   WW3_Coastal_US_East_Coast_20140804_0000.grib2
D   WW3_Coastal_US_West_Coast_20140804_1800.grib2
D   WW3_Global_20140804_0000.grib2
D   WW3_Regional_Alaska_20140804_0000.grib2
D   WW3_Regional_Eastern_Pacific_20140804_0000.grib2
D   WW3_Regional_US_East_Coast_20140804_0000.grib2
D   WW3_Regional_US_West_Coast_20140803_0600.grib2


 */
