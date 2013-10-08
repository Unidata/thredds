package ucar.nc2.jni.netcdf;

import org.junit.Before;
import org.junit.Test;
import ucar.ma2.Array;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.MAMath;
import ucar.nc2.*;
import ucar.nc2.constants.CDM;
import ucar.nc2.iosp.netcdf4.Nc4;
import ucar.nc2.util.CompareNetcdf2;
import ucar.unidata.io.RandomAccessFile;
import ucar.unidata.test.util.TestDir;

import java.io.File;
import java.io.IOException;
import java.util.Formatter;

/**
 * Test JNI netcdf-4 iosp
 * Compare reading with native java reading
 *
 * @author caron
 * @since 7/3/12
 */
public class TestNc4IospReading {
  private boolean showCompareResults = true;
  private int countNotOK = 0;

  @Before
  public void setLibrary() {
    Nc4Iosp.setLibraryAndPath("/home/mhermida/opt/lib", "netcdf");
    //Nc4Iosp.setLibraryAndPath("C:/cdev/lib", "netcdf");
    System.out.printf("Nc4Iosp.isClibraryPresent = %s%n", Nc4Iosp.isClibraryPresent());
  }

  @Test
  public void testReadSubsection() throws IOException, InvalidRangeException {
    String location = TestDir.cdmUnitTestDir + "formats/netcdf4/ncom_relo_fukushima_1km_tmp_2011040800_t000.nc4";
    NetcdfFile ncfile = NetcdfFile.open(location);
    NetcdfFile jni = openJni(location);
    jni.setLocation(location+" (jni)");

    // float salinity(time=1, depth=40, lat=667, lon=622);
    Array data1 = read(ncfile, "salinity", "0,11:12,22,:");
    //NCdumpW.printArray(data1);
    System.out.printf("Read from jni%n");
    Array data2 = read(jni, "salinity", "0,11:12,22,:");
    assert MAMath.isEqual(data1, data2);
    System.out.printf("data is equal%n");
    ncfile.close();
    jni.close();
  }

  private Array read(NetcdfFile ncfile, String vname, String section) throws IOException, InvalidRangeException {
    Variable v = ncfile.findVariable(vname);
    assert v != null;
    return v.read(section) ;
  }

  @Test
  public void testNestedStructure() throws IOException {
    doCompare(TestDir.cdmUnitTestDir + "formats/netcdf4/testNestedStructure.nc", true, false, true);
  }

  // @Test
  public void timeRead() throws IOException {
    String location = "Q:/NARR/narr-TMP-200mb_221_yyyymmdd_hh00_000.grb.grb2.nc4";

    NetcdfFile jni = openJni(location);
    Variable v = jni.findVariable("time");

    long start = System.currentTimeMillis();
    Array data = v.read();
    long took = System.currentTimeMillis() - start;
    System.out.printf(" jna took= %d msecs size=%d%n", took, data.getSize());

    jni.close();

    NetcdfFile ncfile = NetcdfFile.open(location);
    v = ncfile.findVariable("time");

    start = System.currentTimeMillis();
    data = v.read();
    took = System.currentTimeMillis() - start;
    System.out.printf(" java took= %d msecs size=%d%n", took, data.getSize());

    ncfile.close();

  }

  @Test
  public void readAllNetcdf4() throws IOException {
    int count = 0;
    count += TestDir.actOnAll(TestDir.cdmUnitTestDir + "formats/netcdf4/", new MyFileFilter(), new MyAct(), true);
    System.out.printf("***READ %d files FAIL = %d%n", count, countNotOK);
  }

  @Test
  public void readAllHDF5() throws IOException {
    int count = 0;
    count += TestDir.actOnAll(TestDir.cdmUnitTestDir + "formats/hdf5/", new MyFileFilter(), new MyAct(), true);
    System.out.printf("***READ %d files FAIL = %d%n", count, countNotOK);
  }

  // @Test
  public void problem() throws IOException {
    String filename = "Q:/cdmUnitTest/formats/hdf5/npoess/GATRO-SATMR_npp_d20020906_t0409572_e0410270_b19646_c20090720223122943227_devl_int.h5";
    doCompare(filename, false, false, false);
  }


  @Test
  public void readAllNetcdf3() throws IOException {
    int count = 0;
    count += TestDir.actOnAll(TestDir.cdmUnitTestDir + "formats/netcdf3/", new MyFileFilter(), new MyAct());
    System.out.printf("***READ %d files FAIL = %d%n", count, countNotOK);
  }

  private class MyFileFilter implements java.io.FileFilter {
    public boolean accept(File pathname) {
      return true;
    }
  }

  public static class Netcdf4ObjectFilter implements CompareNetcdf2.ObjFilter {
    @Override
    public boolean attOk(Variable v, Attribute att) {
      // if (v != null && v.isMemberOfStructure()) return false;
      String name = att.getShortName();

      // added by cdm
      if (name.equals(CDM.CHUNK_SIZE)) return false;
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
  }

  private class MyAct implements TestDir.Act {
    public int doAct(String filename) throws IOException {
      if (!doCompare(filename, false, false, false))
        countNotOK++;
      return 1;
    }
  }


  private boolean doCompare(String location, boolean showCompare, boolean showEach, boolean compareData) throws IOException {
    NetcdfFile ncfile = NetcdfFile.open(location);
    NetcdfFile jni = openJni(location);
    jni.setLocation(location+" (jni)");
    //System.out.printf("Compare %s to %s%n", ncfile.getIosp().getClass().getName(), jni.getIosp().getClass().getName());

    Formatter f= new Formatter();
    CompareNetcdf2 tc = new CompareNetcdf2(f, showCompare, showEach, compareData);
    boolean ok = tc.compare(ncfile, jni, new Netcdf4ObjectFilter(), showCompare, showEach, compareData);
    System.out.printf(" %s compare %s ok = %s%n", ok ? "" : "***", location, ok);
    if (!ok ||(showCompare && showCompareResults)) System.out.printf("%s%n=====================================%n", f);
    ncfile.close();
    jni.close();
    return ok;
  }

  private NetcdfFile openJni(String location) throws IOException {
    Nc4Iosp iosp = new Nc4Iosp(NetcdfFileWriter.Version.netcdf4);
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
