package ucar.nc2.jni.netcdf;

import org.apache.commons.io.filefilter.NotFileFilter;
import org.apache.commons.io.filefilter.SuffixFileFilter;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ucar.ma2.DataType;
import ucar.nc2.*;
import ucar.nc2.constants.CDM;
import ucar.nc2.iosp.netcdf4.Nc4;
import ucar.nc2.util.CompareNetcdf2;
import ucar.unidata.io.RandomAccessFile;
import ucar.unidata.test.util.TestDir;

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
public class TestNc4JniReadCompare {

  @Before
  public void setLibrary() {
    Nc4Iosp.setLibraryAndPath("/opt/netcdf/lib", "netcdf");
    //Nc4Iosp.setLibraryAndPath("C:/cdev/lib", "netcdf");
    System.out.printf("Nc4Iosp.isClibraryPresent = %s%n", Nc4Iosp.isClibraryPresent());
  }

  @Parameterized.Parameters
  public static List<Object[]> getTestParameters() {

    List<Object[]> result = new ArrayList<Object[]>(500);
    try {
      addFromScan(result, TestDir.cdmUnitTestDir + "formats/netcdf3/", new NotFileFilter( new SuffixFileFilter(".cdl")));
      addFromScan(result, TestDir.cdmUnitTestDir + "formats/netcdf4/", new NotFileFilter( new SuffixFileFilter(".cdl")));

    } catch (IOException e) {
      e.printStackTrace();
    }

    return result;
  }

  static void addFromScan(final List<Object[]> list, String dirName, FileFilter ff) throws IOException {
    TestDir.actOnAll(dirName, ff, new TestDir.Act() {
      public int doAct(String filename) throws IOException {
        list.add(new Object[]{filename});
        return 1;
      }
    }, true);
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

      Formatter f = new Formatter();
      CompareNetcdf2 mind = new CompareNetcdf2(f, false, false, false);
      boolean ok = mind.compare(ncfile, jni, new Netcdf4ObjectFilter(), false, false, false);
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

  public static class Netcdf4ObjectFilter implements CompareNetcdf2.ObjFilter {
    @Override
    public boolean attCheckOk(Variable v, Attribute att) {
      // if (v != null && v.isMemberOfStructure()) return false;
      String name = att.getShortName();

      // added by cdm
      if (name.equals(CDM.CHUNK_SIZES)) return false;
      if (name.equals(CDM.FILL_VALUE)) return false;
      if (name.equals("_lastModified")) return false;

      // hidden by nc4
      if (name.equals(Nc4.NETCDF4_DIMID)) return false;  // preserve the order of the dimensions
      if (name.equals(Nc4.NETCDF4_COORDINATES)) return false;  // ??
      if (name.equals(Nc4.NETCDF4_STRICT)) return false;

      // not implemented yet
      //if (att.getDataType().isEnum()) return false;

      return true;
    }

    @Override
    public boolean varDataTypeCheckOk(Variable v) {
      if (v.getDataType() == DataType.CHAR) return false;    // temp workaround
      if (v.getDataType() == DataType.STRING) return false;
      return true;
    }
  }


}

