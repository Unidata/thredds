package ucar.nc2.jni.netcdf;

import org.apache.commons.io.filefilter.NotFileFilter;
import org.apache.commons.io.filefilter.SuffixFileFilter;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ucar.nc2.*;
import ucar.nc2.util.CompareNetcdf2;
import ucar.unidata.io.RandomAccessFile;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;
import ucar.unidata.util.test.TestDir;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;

/**
 * Compare reading through jni with native java reading
 *
 * @author caron
 * @since 10/22/13
 */
@RunWith(Parameterized.class)
@Category(NeedsCdmUnitTest.class)
public class TestNc4JniReadCompare {

  @Before
  public void setLibrary() {
    // Ignore this class's tests if NetCDF-4 isn't present.
    // We're using @Before because it shows these tests as being ignored.
    // @BeforeClass shows them as *non-existent*, which is not what we want.
    Assume.assumeTrue("NetCDF-4 C library not present.", Nc4Iosp.isClibraryPresent());
  }

  @Parameterized.Parameters(name="{0}")
  public static List<Object[]> getTestParameters() {

    FileFilter ff = new NotFileFilter( new SuffixFileFilter(".cdl"));
    List<Object[]> result = new ArrayList<Object[]>(500);
    try {
      TestDir.actOnAllParameterized(TestDir.cdmUnitTestDir + "formats/netcdf3/", ff, result);
      TestDir.actOnAllParameterized(TestDir.cdmUnitTestDir + "formats/netcdf4/", ff, result);
    } catch (IOException e) {
      e.printStackTrace();
    }

    return result;
  }

  private static class Hdf5FileFilter implements java.io.FileFilter {
    public boolean accept(File pathname) {
        /* java.io.IOException: -101: NetCDF: HDF error
          at ucar.nc2.jni.netcdf.Nc4Iosp._open(Nc4Iosp.java:243)
        	at ucar.nc2.jni.netcdf.Nc4Iosp.open(Nc4Iosp.java:227) */
      if (pathname.getPath().endsWith("wrf_bdy_par.h5")) return false; // temporary
      if (pathname.getPath().endsWith("wrf_input_par.h5")) return false; // temporary
      if (pathname.getPath().endsWith("wrf_out_par.h5")) return false; // temporary
      if (pathname.getPath().endsWith("time.h5")) return false; // temporary
      if (pathname.getPath().contains("npoess")) return false; // temporary
      // if (pathname.getName().endsWith(".xml")) return false;
      return true;
    }
  }

  /////////////////////////////////////////////////////////////

  public TestNc4JniReadCompare(String filename) {
    this.filename = filename;
  }

  String filename;

  int fail = 0;
  int success = 0;

  @Test
  public void compareDatasets() throws IOException {

    NetcdfFile ncfile = null, jni = null;
    try {
      ncfile = NetcdfFile.open(filename);
      jni = openJni(filename);
      jni.setLocation(filename + " (jni)");
      System.err.println("Test input: "+ncfile.getLocation());
      System.err.println("Baseline: "+jni.getLocation());
      System.err.flush();

      Formatter f = new Formatter();
      CompareNetcdf2 mind = new CompareNetcdf2(f, true, true, false);
      boolean ok = mind.compare(ncfile, jni, new CompareNetcdf2.Netcdf4ObjectFilter(), true, true, false);
      if (!ok) {
        fail++;
        System.out.printf("--Compare %s%n", filename);
        System.out.printf("  %s%n", f);
      } else {
        System.out.printf("--Compare %s is OK%n", filename);
        success++;
      }
      Assert.assertTrue(filename, ok);
    } finally {
      if (ncfile != null) ncfile.close();
      if (jni != null) jni.close();
    }
  }

  private NetcdfFile openJni(String location) throws IOException {
    Nc4Iosp iosp = new Nc4Iosp(NetcdfFileWriter.Version.netcdf4);
    NetcdfFile ncfile = new NetcdfFileSubclass(iosp, location);
    RandomAccessFile raf = new RandomAccessFile(location, "r");
    iosp.open(raf, ncfile, null);
    return ncfile;
  }



}

