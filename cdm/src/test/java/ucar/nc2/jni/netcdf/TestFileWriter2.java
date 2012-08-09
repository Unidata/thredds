package ucar.nc2.jni.netcdf;

import org.junit.Test;
import ucar.nc2.FileWriter2;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFileWriter;
import ucar.unidata.test.util.TestDir;

import java.io.File;
import java.io.IOException;

/**
 * Describe
 *
 * @author caron
 * @since 7/27/12
 */
public class TestFileWriter2 {
  int countNotOK = 0;

  @Test
  public void problem() throws IOException {
    copyFile3("Q:/cdmUnitTest/ft/point/netcdf/Surface_Buoy_20090921_0000.nc", "C:/temp/Surface_Buoy_20090921_0000.nc");
    NetcdfFile ncfile = NetcdfFile.open("C:/temp/Surface_Buoy_20090921_0000.nc");
    ncfile.close();
  }

  public void readAllNetcdf4() throws IOException {
    int count = 0;
    count += TestDir.actOnAll(TestDir.cdmUnitTestDir + "formats/netcdf4/tst/", null, new MyAct(), true);
    System.out.printf("***READ %d files FAIL = %d%n", count, countNotOK);
  }

  public void readAllHDF5() throws IOException {
    int count = 0;
    count += TestDir.actOnAll(TestDir.cdmUnitTestDir + "formats/hdf5/", null, new MyAct(), true);
    System.out.printf("***READ %d files FAIL = %d%n", count, countNotOK);
  }

  public void readAllNetcdf3() throws IOException {
    int count = 0;
    count += TestDir.actOnAll(TestDir.cdmUnitTestDir + "formats/netcdf3/", null, new MyAct());
    System.out.printf("***READ %d files FAIL = %d%n", count, countNotOK);
  }

  private class MyAct implements TestDir.Act {
    public int doAct(String datasetIn) throws IOException {
      File fin = new File(datasetIn);
      String datasetOut = tempDir + fin.getName();

      if (!copyFile(datasetIn, datasetOut))
        countNotOK++;
      return 1;
    }
  }

  private String tempDir = "C:/temp/";
  private boolean copyFile(String datasetIn, String datasetOut) throws IOException {
    System.out.printf("copy %s to %s%n", datasetIn, datasetOut);

    NetcdfFile ncfileIn = ucar.nc2.NetcdfFile.open(datasetIn, null);
    FileWriter2 writer2 = new FileWriter2(ncfileIn, datasetOut,  NetcdfFileWriter.Version.netcdf4);
    NetcdfFile ncfileOut = writer2.write();
    ncfileIn.close();
    ncfileOut.close();
    // System.out.println("NetcdfFile written = " + ncfileOut);
    return true;
  }

  private boolean copyFile3(String datasetIn, String datasetOut) throws IOException {
     System.out.printf("copy %s to %s%n", datasetIn, datasetOut);

     NetcdfFile ncfileIn = ucar.nc2.NetcdfFile.open(datasetIn, null);
     FileWriter2 writer2 = new FileWriter2(ncfileIn, datasetOut,  NetcdfFileWriter.Version.netcdf3);
     NetcdfFile ncfileOut = writer2.write();
     ncfileIn.close();
     ncfileOut.close();
     // System.out.println("NetcdfFile written = " + ncfileOut);
     return true;
   }


}
