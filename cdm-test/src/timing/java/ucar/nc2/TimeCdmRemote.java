package ucar.nc2;

import timing.Stat;
import ucar.ma2.Array;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.Section;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dods.DODSNetcdfFile;
import ucar.nc2.stream.CdmRemote;
import ucar.nc2.stream.NcStreamReader;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Compare time of cdmremote vs opendap data reading on a local TDS.
 *
 * @author John
 * @since 7/18/11
 */
public class TimeCdmRemote {

  interface MClosure {
    void run(String filename) throws IOException, InvalidRangeException;
  }

  static void testAllInDir(File dir, MClosure closure) throws IOException, InvalidRangeException {
    File[] fa = dir.listFiles();
    if (fa == null || fa.length == 0) return;

    List<File> list = Arrays.asList(fa);
    Collections.sort(list);

    for (File f : list) {
      if (f.isDirectory())
        testAllInDir(f, closure);
      else {
        closure.run(f.getPath());
      }
    }
  }

  static long readAllData( NetcdfFile ncfile) {
    long bytes = 0;
    try {
      for (Variable v : ncfile.getVariables()) {
        if (v.getSize() > max_size) {
          Section s = makeSubset(v);
          if (verbose) System.out.println("  Try to read variable " + v.getNameAndDimensions() + " size= " + v.getSize() + " section= " + s);
          Array data = v.read(s);
          bytes += data.getSizeBytes();
        } else {
          if (verbose) System.out.println("  Try to read variable " + v.getNameAndDimensions() + " size= " + v.getSize());
          Array data = v.read();
          bytes += data.getSizeBytes();
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    return bytes;
  }

  static boolean show = false;
  static boolean verbose = false;
  static int max_size = 1000 * 1000 * 10;
  static Section makeSubset(Variable v) throws InvalidRangeException {
    int[] shape = v.getShape();
    shape[0] = 1;
    Section s = new Section(shape);
    long size = s.computeSize();
    shape[0] = (int) Math.max(1, max_size / size);
    return new Section(shape);
  }


  static void testRead(String filename, Stat stat, boolean readData) throws IOException, InvalidRangeException {
    long bytes = 0;
    long start = System.nanoTime();
    if (show) System.out.printf("%n------Reading filename %s (%s)%n", filename, stat.getName());

    try ( NetcdfFile ncfile = NetcdfDataset.openFile(filename, null)) {
      if (readData) bytes = readAllData(ncfile);
      long end = System.nanoTime();
      double took = ((double)(end - start)) / 1000 / 1000 / 1000; // secs
      double rate = 0;
      ncfile.close();
      if (stat != null && bytes > 0) {
        rate = bytes/took/1000/1000;
        stat.sample(rate); // Mb/sec
      }
      if (show) System.out.printf(" bytes = %d took =%f secs rate=%f MB/sec%n", bytes, took, rate);
    }
  }

  static String server = "//localhost:8081/thredds";
  static void compare(String url, Stat statCdm, Stat statDods, int n, boolean readData) throws IOException, InvalidRangeException {
    for (int i=0; i<n; i++) {
      testRead("cdmremote:"+server+"/cdmremote/"+url, statCdm, readData);
      testRead("dods:"+server+"/dodsC/"+url, statDods, readData);
    }

    for (int i=0; i<n; i++) {
      testRead("dods:"+server+"/dodsC/"+url, statDods, readData);
      testRead("cdmremote:"+server+"/cdmremote/"+url, statCdm, readData);
    }
  }

  static void testDodsCompress(String url, Stat statCompress, Stat statNone, int n, boolean readData) throws IOException, InvalidRangeException {
    for (int i=0; i<n; i++) {
      DODSNetcdfFile.setAllowCompression(false);
      testRead("dods:" + server + "/dodsC/" + url, statNone, readData);
      DODSNetcdfFile.setAllowCompression(true);
      testRead("dods:"+server+"/dodsC/"+url, statCompress, readData);
    }
  }

  static void testCdmremoteCompress(String url, Stat statCompress, Stat statNone, int n, boolean readData) throws IOException, InvalidRangeException {
    for (int i=0; i<n; i++) {
      CdmRemote.setAllowCompression(false);
      testRead("cdmremote:" + server + "/cdmremote/" + url, statNone, readData);
      CdmRemote.setAllowCompression(true);
      testRead("cdmremote:"+server+"/cdmremote/"+url, statCompress, readData);
    }
    System.out.printf(" compression ratio = %f%n", NcStreamReader.getCompression(true));
  }

  static void testCdmremote(String url, Stat tstat, int n, boolean readData) throws IOException, InvalidRangeException {
    Stat stat = new Stat(tstat.getName(), false);
    for (int i=0; i<n; i++) {
      testRead("cdmremote:" + server + "/cdmremote/" + url, stat, readData);
    }
    System.out.printf(" %s MB/sec%n", stat);
    tstat.add(stat);
  }

  static void doAll(String url, Stat tstat1, Stat tstat2) throws IOException, InvalidRangeException {
    // testCdmremoteCompress(url, stat1.setName("compress"), stat2.setName("nocomprs"), 10, true);
    System.out.printf("%n------ filename %s%n", url);

    Stat stat1 = new Stat(tstat1.getName(), false);
    Stat stat2 = new Stat(tstat2.getName(), false);

    compare(url, stat1, stat2, 5, true);

    System.out.printf(" %s MB/sec%n", stat1);
    System.out.printf(" %s MB/sec%n", stat2);

    tstat1.add(stat1);
    tstat2.add(stat2);
  }

  static void doOne(String url, Stat stat1, Stat stat2) throws IOException, InvalidRangeException {
    System.out.printf("%n------ filename %s%n", url);
    testCdmremote(url, stat1, 20, true);
  }

  public static void main(String args[]) throws IOException, InvalidRangeException {
    Stat stat1 = new Stat("CDM4", false);
    Stat stat2 = new Stat("DODS", false);

    CdmRemote.setAllowCompression(true);
    DODSNetcdfFile.setAllowCompression(true);

    doOne("scanCdmUnitTests/formats/grib1/Mercator.grib1", stat1, stat2);
    doOne("scanCdmUnitTests/formats/grib2/ds.pop12.bin", stat1, stat2);
    doOne("scanCdmUnitTests/formats/grib2/AVOR_000.grb", stat1, stat2);
    doOne("scanCdmUnitTests/conventions/cf/bora_feb_001.nc", stat1, stat2);
    doOne("scanCdmUnitTests/conventions/cf/ccsm2.nc", stat1, stat2);

    System.out.printf("%n %s MB/sec%n",stat1);
    // System.out.printf(" %s MB/sec%n", stat2);
  }
}
