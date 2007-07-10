package ucar.nc2;

import junit.framework.*;

import java.io.*;

import ucar.nc2.iosp.netcdf3.N3streamWriter;
import ucar.nc2.iosp.netcdf3.N3channelWriter;
import ucar.ma2.InvalidRangeException;

/**
 * test FileWriting, then reading back and comparing to original.
 */

public class TestStreamWriter extends TestCase {
  static boolean showCompare = true, showEach = false;

  public TestStreamWriter(String name) {
    super(name);
  }


  static public void test() throws IOException {
    //String fileInName = "C:/data/metars/Surface_METAR_20070326_0000.nc";
    String fileInName = "C:/data/RUC2_CONUS_40km_20070709_1800.nc";
    //String fileInName = "C:/data/test.nc";
    //String fileInName = "C:/data/test_UD.nc";
    //String fileInName = "C:/data/testEmpty.nc";

    System.out.println("\nFile= " + fileInName + " size=" + new File(fileInName).length());

    NetcdfFile fileIn = NetcdfFile.open(fileInName);

    long start = System.currentTimeMillis();
    N3streamWriter.writeFromFile(fileIn, "C:/data/testStream.nc");
    long took = System.currentTimeMillis() - start;
    System.out.println("N3streamWriter took " + took + " msecs");
    //fileIn.close();

    //NetcdfFile file1= NetcdfFile.open(fileInName);
    NetcdfFile file2 = NetcdfFile.open("C:/data/testStream.nc");

    ucar.nc2.TestCompare.compareFiles(fileIn, file2, true, true, false);
  }

  static public void testTime(String[] filename) throws IOException, InvalidRangeException {
    System.out.println("\nFile= " + filename[0] + " size=" + new File(filename[0]).length());
 
    NetcdfFile fileIn = NetcdfFile.open(filename[0]);
    File fileOut = File.createTempFile("Test", ".tmp", new File("C:/data/"));
    long start = System.currentTimeMillis();
    N3streamWriter.writeFromFile(fileIn, fileOut.getPath());
    long took = System.currentTimeMillis() - start;
    System.out.println("N3streamWriter took " + took + " msecs");  // */
    fileIn.close();

    NetcdfFile fileIn2 = NetcdfFile.open(filename[1]);
    File fileOut2 = File.createTempFile("Test", ".tmp", new File("C:/data/"));
    long start2 = System.currentTimeMillis();
    NetcdfFile ncout2 = FileWriter.writeToFile(fileIn2, fileOut2.getPath());
    ncout2.close();
    long took2 = System.currentTimeMillis() - start2;
    System.out.println("FileWriter took " + took2 + " msecs");
    fileIn2.close();

    NetcdfFile fileIn3 = NetcdfFile.open(filename[2]);
    File fileOut3 = File.createTempFile("Test", ".tmp", new File("C:/data/"));
    long start3 = System.currentTimeMillis();
    N3channelWriter.writeFromFile(fileIn3, fileOut3.getPath());
    long took3 = System.currentTimeMillis() - start3;
    System.out.println("N3channelWriter took " + took3 + " msecs");
    fileIn3.close();
  }

  static public void testTime() throws IOException, InvalidRangeException {
    testTime(new String[] {"C:/data/RUC2_CONUS_40km_20070709_1800.nc",
        "C:/data/CopyRUC2_CONUS_40km_20070709_1800.nc",
        "C:/data/Copy2RUC2_CONUS_40km_20070709_1800.nc"});
    
    testTime(new String[] {"C:/data/metars/Surface_METAR_20070329_0000.nc",
        "C:/data/metars/Surface_METAR_20070330_0000.nc",
        "C:/data/metars/Surface_METAR_20070326_0000.nc"});
  }


}