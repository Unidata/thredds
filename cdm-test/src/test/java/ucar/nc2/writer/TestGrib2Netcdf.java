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

import ucar.nc2.NetcdfFileWriter;
import ucar.nc2.dt.grid.CFGridWriter2;
import ucar.nc2.write.Nc4Chunking;
import ucar.nc2.write.Nc4ChunkingStrategy;
import ucar.unidata.util.test.TestDir;

import java.io.*;
import java.util.Formatter;

/**
 * Describe
 *
 * @author caron
 * @since 8/4/2014
 */
public class TestGrib2Netcdf {
  static String dirOut;
  static String csvOut;
  static PrintStream fw;

 static  void  writeHeader() throws FileNotFoundException {
    File outDir = new File(dirOut);
    fw = new PrintStream(new File(outDir, csvOut));
    fw.printf("file, sizeIn(MB), type, shuffle, deflate, sizeOut(MB), sizeOut/sizeIn, dataLen(MB), dataLen/sizeOut, time(secs) %n");
  }

  double writeNetcdf(String fileInName, NetcdfFileWriter.Version version,
                   Nc4Chunking.Strategy chunkerType, int deflateLevel, boolean shuffle) throws IOException {

    File fin = new File(fileInName);
    //if (fin.length() > 2000 * 1000 * 1000) {
   //   System.out.format("   skip %s: %d%n", fin.getName(), fin.length());
    //  return 0; // skip > 2G
    //}
    Formatter foutf = new Formatter();
    foutf.format("%s", fin.getName());
    if (deflateLevel > 0) foutf.format(".%d", deflateLevel);
    if (chunkerType != null) foutf.format(".%s", chunkerType);
    if (shuffle) foutf.format(".shuffle");
    foutf.format("%s", version.getSuffix());

    File fout = new File(dirOut, foutf.toString());

    ucar.nc2.dt.GridDataset gds = ucar.nc2.dt.grid.GridDataset.open(fileInName);
    Nc4Chunking chunking = (version == NetcdfFileWriter.Version.netcdf3) ? null : Nc4ChunkingStrategy.factory(chunkerType, deflateLevel, shuffle);
    NetcdfFileWriter writer = NetcdfFileWriter.createNew(version, fout.getCanonicalPath(), chunking);

    long start = System.currentTimeMillis();

    long totalBytes;
    try {
      totalBytes = CFGridWriter2.writeFile(gds, null, null, null, 1, null, null, 1, false, writer);
      totalBytes /= 1000 * 1000;
    } catch (Throwable e) {
      e.printStackTrace();
      return 0;
    }

    double lenIn = (double) fin.length();
    lenIn /= 1000 * 1000;

    File fout2 = new File(dirOut, foutf.toString());
    double lenOut = (double) fout2.length();
    lenOut /= 1000 * 1000;

    System.out.format("   %10.3f: %s%n", lenOut / lenIn, fout.getCanonicalPath());
    double took = (System.currentTimeMillis() - start) /1000.0;
    System.out.format("   that took: %f secs%n", took);

    fw.printf("%s,%10.3f, %s,%s, %d, %10.3f,%10.3f,%d,%f,%f%n", fin.getName(), lenIn,
            (chunkerType != null) ? chunkerType : "nc3",
            shuffle ? "shuffle" : "",
            deflateLevel,
            lenOut, lenOut / lenIn, totalBytes, totalBytes / lenOut , took);

    return lenOut;
  }

  /////////////////////////////////////////////////////////////////////////

  private static class ChunkGribAct implements TestDir.Act {
    public int doAct(String filename) throws IOException {

      TestGrib2Netcdf writer = new TestGrib2Netcdf();
      total += writer.writeNetcdf(filename, NetcdfFileWriter.Version.netcdf4, Nc4Chunking.Strategy.grib, 9, false);

      System.out.format("   total so far: %10.3f%n%n", total);
      fw.flush();

      return 1;
    }
  }

  private static class DeflateByLevelAct implements TestDir.Act {

    public int doAct(String filename) throws IOException {

      TestGrib2Netcdf writer = new TestGrib2Netcdf();
      for (int level=1; level<8; level++) {
        total += writer.writeNetcdf(filename, NetcdfFileWriter.Version.netcdf4, Nc4Chunking.Strategy.grib, level, true);
        total += writer.writeNetcdf(filename, NetcdfFileWriter.Version.netcdf4, Nc4Chunking.Strategy.grib, level, false);
        fw.flush();
      }

      fw.flush();

      return 1;
    }
  }

  static double total = 0;


  public static void main2(String[] args) throws IOException {
    dirOut = "G:/write/";
    csvOut = "results.grib.csv";
    writeHeader();

    try {
      String dirName = "Q:/cdmUnitTest/tds/ncep/";
      TestDir.actOnAll(dirName, new TestDir.FileFilterFromSuffixes("grib1 grib2"), new ChunkGribAct(), false);
      System.out.printf("%n%n%10.3f Mbytes%n", total);

    } finally {
      fw.close();
    }
  }


  public static void main(String[] args) throws IOException {
    dirOut = "C:/compress/writeBzip3/";
    File dir = new File(dirOut);
    dir.mkdirs();

    csvOut = "results.csv";
    writeHeader();

    try {
      ChunkGribAct act = new ChunkGribAct();
      // act.doAct("Q:/cdmUnitTest/tds/ncep/NAM_CONUS_12km_conduit_20140804_0000.grib2");
      act.doAct("Q:\\cdmUnitTest\\tds\\ncep\\RUC2_CONUS_20km_surface_20100516_1600.grib2");
      act.doAct("Q:\\cdmUnitTest\\tds\\ncep\\WW3_Coastal_US_West_Coast_20140804_1800.grib2");
      act.doAct("Q:\\cdmUnitTest\\tds\\ncep\\RR_CONUS_13km_20121028_0000.grib2");

      System.out.printf("%n%n%10.3f Mbytes%n", total);

    } finally {
      fw.close();
    }
  }

}
