package ucar.nc2;

import timing.Stat;
import ucar.ma2.Array;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.Section;
import ucar.nc2.dataset.NetcdfDataset;

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
public class CdmRemote {

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
    if (show) System.out.println("\n------Reading filename "+ncfile.getLocation());
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
    try {
      long start = System.nanoTime();
      NetcdfFile ncfile = NetcdfDataset.openFile(filename, null);
      if (readData) bytes = readAllData(ncfile);
      long end = System.nanoTime();
      double took = (double) ((end - start)) / 1000 / 1000 / 1000; // secs
      ncfile.close();
      if (stat != null) {
        if (bytes != 0)
          stat.sample(bytes/took/1000/1000); // Mb/sec
        else
          stat.sample(took); // secs
      }
      if (show) System.out.printf(" bytes = %d took =%f%n", bytes, took);
    } catch (Exception e) {
      System.out.println("BAD " + filename);
      e.printStackTrace();
    }
  }

  static void doOne(String url, Stat statCdm, Stat statDods, int n, boolean readData) throws IOException, InvalidRangeException {
    for (int i=0; i<n; i++) {
      testRead("dods://localhost:8080/thredds/dodsC/"+url, statDods, readData);
      testRead("cdmremote:http://localhost:8080/thredds/cdmremote/"+url, statCdm, readData);
    }

    for (int i=0; i<n; i++) {
      testRead("cdmremote:http://localhost:8080/thredds/cdmremote/"+url, statCdm, readData);
      testRead("dods://localhost:8080/thredds/dodsC/"+url, statDods, readData);
    }

  }

  public static void main(String args[]) throws IOException, InvalidRangeException {
    Stat statCdm = new Stat("CDM ", false);
    Stat statDods = new Stat("DODS", false);

    boolean readData = true;
    int n = 2;
    //doOne("testDataAll/fmrc/gomoos/gomoos.20090223.cdf", statCdm, statDods, n, readData);
    //doOne("testDataAll/fmrc/gomoos/gomoos.20090222.cdf", statCdm, statDods, n, readData);
    doOne("testDataAll/formats/grib/NAM_CONUS_40km_conduit_20090317_0000.grib2", statCdm, statDods, n, readData);

    System.out.printf("%n%s%n",statCdm);
    System.out.printf("%s%n", statDods);
  }
}
