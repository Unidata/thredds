package ucar.nc2.jni.netcdf;

import org.junit.Test;
import ucar.nc2.Attribute;
import ucar.nc2.NetcdfFile;
import ucar.nc2.util.CompareNetcdf2;
import ucar.unidata.io.RandomAccessFile;
import ucar.unidata.test.util.TestDir;

import java.io.File;
import java.io.IOException;
import java.util.Formatter;

/**
 * Describe
 *
 * @author caron
 * @since 7/3/12
 */
public class TestNc4Iosp {
  private boolean showCompareResults = true;

  @Test
  public void readAllNetcdf4() throws IOException {
    int count = 0;
    count += TestDir.actOnAll(TestDir.cdmUnitTestDir + "formats/netcdf4/files", new MyFileFilter(), new MyAct());
    System.out.println("***READ " + count + " files");
  }

  @Test
  public void readAllNetcdf3() throws IOException {
    int count = 0;
    count += TestDir.actOnAll(TestDir.cdmUnitTestDir + "formats/netcdf3/", new MyFileFilter(), new MyAct());
    System.out.println("***READ " + count + " files");
  }

  private class MyFileFilter implements java.io.FileFilter {
    public boolean accept(File pathname) {
      return true;
    }
  }

  private class MyObjectFilter implements CompareNetcdf2.ObjFilter {

    @Override
    public boolean attOk(Attribute att) {
      String name = att.getName();
      if (name.equals("HDF5_chunksize")) return false;
      if (att.getDataType().isEnum()) return false;
      if (att.isString() && att.getStringValue() == null) return false;
      if (att.isString() && (att.getStringValue().length() == 0)) return false;
      return true;
    }
  }

  private class MyAct implements TestDir.Act {
    public int doAct(String filename) throws IOException {
      doCompare(filename, true, true, true);
      return 0;
    }
  }

  @Test
  public void problem() throws IOException {
    doCompare("Q:\\cdmUnitTest\\formats\\netcdf4\\tst\\c0_4.nc4", true, true, true);
  }

  private boolean doCompare(String location, boolean showCompare, boolean showEach, boolean compareData) throws IOException {
    NetcdfFile ncfile = NetcdfFile.open(location);
    NetcdfFile jni = openJni(location);
    // System.out.printf("Compare %s to %s%n", ncfile.getIosp().getClass().getName(), jni.getIosp().getClass().getName());

    Formatter f= new Formatter();
    CompareNetcdf2 tc = new CompareNetcdf2(f, showCompare, showEach, compareData);
    boolean ok = tc.compare(ncfile, jni, new MyObjectFilter(), showCompare, showEach, compareData);
    System.out.printf(" %s compare %s ok = %s%n", ok ? "" : "***", location, ok);
    if (!ok && showCompareResults) System.out.printf("%s%n=====================================%n", f);
    return ok;
  }

  private NetcdfFile openJni(String location) throws IOException {
    Nc4Iosp iosp = new Nc4Iosp();
    NetcdfFile ncfile = new MyNetcdfFile(iosp, location);
    RandomAccessFile raf = new RandomAccessFile(location, "r");
    iosp.open(raf, ncfile, null);
    return ncfile;
  }

  private class MyNetcdfFile extends NetcdfFile {
    private MyNetcdfFile(Nc4Iosp iosp, String location) {
      super();
      spi = iosp;
      this.location = location;
    }
  }

}
