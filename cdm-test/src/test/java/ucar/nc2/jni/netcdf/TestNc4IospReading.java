package ucar.nc2.jni.netcdf;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import ucar.ma2.Array;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.MAMath;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFileSubclass;
import ucar.nc2.NetcdfFileWriter;
import ucar.nc2.Variable;
import ucar.nc2.util.CompareNetcdf2;
import ucar.unidata.io.RandomAccessFile;
import ucar.unidata.test.util.NeedsCdmUnitTest;
import ucar.unidata.test.util.TestDir;

import java.io.IOException;
import java.util.Formatter;

/**
 * Test JNI netcdf-4 iosp
 * Compare reading with native java reading
 *
 * @author caron
 * @since 7/3/12
 */
@Category(NeedsCdmUnitTest.class)
public class TestNc4IospReading {
  private boolean showCompareResults = true;
  private int countNotOK = 0;

  @Before
  public void setLibrary() {
    Nc4Iosp.setLibraryAndPath("/opt/netcdf/lib", "netcdf");
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
    String location = TestDir.cdmUnitTestDir+"/NARR/narr-TMP-200mb_221_yyyymmdd_hh00_000.grb.grb2.nc4";

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
  public void fractalHeapProblem() throws IOException {
    String filename = TestDir.cdmUnitTestDir + "formats/netcdf4/espresso_his_20130913_0000_0007.nc";
    System.out.printf("***READ %s%n", filename);
    doCompare(filename, false, false, false);

    NetcdfFile ncfile = NetcdfFile.open(filename);
    assert ncfile.findVariable("h")  != null;
    ncfile.close();
  }

  /*
    ** Missing dim phony_dim_0 = 15; not in file2
    ...
   */
  // @Test
  public void problem() throws IOException {
    String filename = TestDir.cdmUnitTestDir + "formats\\hdf5\\OMI-Aura_L2G-OMCLDRRG_2007m0105_v003-2008m0105t101212.he5";
    System.out.printf("***READ %s%n", filename);
    doCompare(filename, false, false, false);
  }

  private boolean doCompare(String location, boolean showCompare, boolean showEach, boolean compareData) throws IOException {
    NetcdfFile ncfile = NetcdfFile.open(location);
    NetcdfFile jni = openJni(location);
    jni.setLocation(location+" (jni)");
    //System.out.printf("Compare %s to %s%n", ncfile.getIosp().getClass().getName(), jni.getIosp().getClass().getName());

    Formatter f= new Formatter();
    CompareNetcdf2 tc = new CompareNetcdf2(f, showCompare, showEach, compareData);
    boolean ok = tc.compare(ncfile, jni, new TestNc4JniReadCompare.Netcdf4ObjectFilter(), showCompare, showEach, compareData);
    System.out.printf(" %s compare %s ok = %s%n", ok ? "" : "***", location, ok);
    if (!ok ||(showCompare && showCompareResults)) System.out.printf("%s%n=====================================%n", f);
    ncfile.close();
    jni.close();
    return ok;
  }

  private NetcdfFile openJni(String location) throws IOException {
    Nc4Iosp iosp = new Nc4Iosp(NetcdfFileWriter.Version.netcdf4);
    NetcdfFile ncfile = new NetcdfFileSubclass(iosp, location);
    RandomAccessFile raf = new RandomAccessFile(location, "r");
    iosp.open(raf, ncfile, null);
    return ncfile;
  }

}
