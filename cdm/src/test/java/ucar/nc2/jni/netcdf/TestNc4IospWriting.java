package ucar.nc2.jni.netcdf;

import org.junit.Before;
import org.junit.Test;
import ucar.nc2.FileWriter2;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFileWriter;
import ucar.nc2.util.CompareNetcdf2;
import ucar.nc2.util.DebugFlagsImpl;
import ucar.unidata.test.util.TestDir;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.Formatter;

/**
 * Test copying files to netcdf4 with FileWriter2.
 * Compare original.
 *
 * @author caron
 * @since 7/27/12
 */
public class TestNc4IospWriting {
  int countNotOK = 0;

  @Before
  public void setLibrary() {
    //Nc4Iosp.setLibraryAndPath("C:/cdev/lib", "netcdf");
    Nc4Iosp.setLibraryAndPath("/home/mhermida/opt/lib", "netcdf");   // jenkins testing on spock; works locally when netcdf.dll is in pth
    //FileWriter2.setDebugFlags(new DebugFlagsImpl("ncfileWriter2/debug"));
  }

  //@Test
  public void problem() throws IOException {
    copyFile("Q:\\cdmUnitTest\\formats\\netcdf4\\files\\tst_string_data.nc", "C:/temp/tst_string_data.nc4", NetcdfFileWriter.Version.netcdf4);
    //copyFile("C:/dev/github/thredds/cdm/src/test/data/testWriteRecord.nc", "C:/temp/testWriteRecord.classic.nc3", NetcdfFileWriter.Version.netcdf3c);
  }

  @Test
  public void writeAllNetcdf4() throws IOException {
    int count = 0;
    count += TestDir.actOnAll(TestDir.cdmUnitTestDir + "formats/netcdf4/files/", new MyFileFilter(), new MyAct(), true);
    System.out.printf("***READ %d files FAIL = %d%n", count, countNotOK);
  }

  //@Test
  public void readAllHDF5() throws IOException {
    int count = 0;
    count += TestDir.actOnAll(TestDir.cdmUnitTestDir + "formats/hdf5/", null, new MyAct(), true);
    System.out.printf("***READ %d files FAIL = %d%n", count, countNotOK);
  }

  @Test
  public void writeAllNetcdf3() throws IOException {
    int count = 0;
    count += TestDir.actOnAll(TestDir.cdmUnitTestDir + "formats/netcdf3/", null, new MyAct());
    System.out.printf("***READ %d files FAIL = %d%n", count, countNotOK);
  }

  private class MyAct implements TestDir.Act {
    public int doAct(String datasetIn) throws IOException {
      File fin = new File(datasetIn);
      String datasetOut = tempDir + fin.getName();

      if (!copyFile(datasetIn, datasetOut, NetcdfFileWriter.Version.netcdf4))
        countNotOK++;
      return 1;
    }
  }

  private class MyFileFilter implements FileFilter {

    @Override
    public boolean accept(File pathname) {
      if (pathname.getName().equals("tst_opaque_data.nc4")) return false;
      if (pathname.getName().equals("tst_opaques.nc4")) return false;
      return true;
    }
  }


  private String tempDir = TestDir.temporaryLocalDataDir; // "C:/temp/";
  private boolean copyFile(String datasetIn, String datasetOut, NetcdfFileWriter.Version version) throws IOException {

     System.out.printf("copy %s to %s%n", datasetIn, datasetOut);
     NetcdfFile ncfileIn = ucar.nc2.NetcdfFile.open(datasetIn, null);
     FileWriter2 writer2 = new FileWriter2(ncfileIn, datasetOut, version, null);
     NetcdfFile ncfileOut = writer2.write();
     compare(ncfileIn, ncfileOut, true, false, true);
     ncfileIn.close();
     ncfileOut.close();
     // System.out.println("NetcdfFile written = " + ncfileOut);
     return true;
   }

  private boolean compare(NetcdfFile nc1, NetcdfFile nc2, boolean showCompare, boolean showEach, boolean compareData) throws IOException {
    Formatter f= new Formatter();
    CompareNetcdf2 tc = new CompareNetcdf2(f, showCompare, showEach, compareData);
    boolean ok = tc.compare(nc1, nc2, new TestNc4IospReading.Netcdf4ObjectFilter(), showCompare, showEach, compareData);
    System.out.printf(" %s compare %s to %s ok = %s%n", ok ? "" : "***", nc1.getLocation(), nc2.getLocation(), ok);
    if (!ok) System.out.printf(" %s%n", f);
    return ok;
  }



}
