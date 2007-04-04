package ucar.nc2;

import junit.framework.*;
import java.io.*;
import java.util.*;

import ucar.nc2.*;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.TestDataset;
import ucar.nc2.ncml.NcMLWriter;
import ucar.unidata.util.StringUtil;

/** test FileWriting, then reading back and comparing to original. */

public class TestStreamWriter extends TestCase {
  static boolean showCompare = true, showEach = false;

  public TestStreamWriter( String name) {
    super(name);
  }


  static public void test()  throws IOException {
    String fileInName = "C:/data/station/collection/Surface_METAR_20051027_0000.nc";
    //String fileInName = "C:/data/test.nc";
    //String fileInName = "C:/data/test_UD.nc";
    //String fileInName = "C:/data/testEmpty.nc";

    NetcdfFile fileIn = NetcdfFile.open(fileInName);

    long start = System.currentTimeMillis();
    N3streamWriter.writeFromFile( fileIn, "C:/data/testStream.nc");
    long took = System.currentTimeMillis() - start;
    System.out.println("that took "+took+" msecs");
    //fileIn.close();

    //NetcdfFile file1= NetcdfFile.open(fileInName);
    NetcdfFile file2= NetcdfFile.open("C:/data/testStream.nc");

    ucar.nc2.TestCompare.compareFiles(fileIn, file2, true, true, false);
  }
}