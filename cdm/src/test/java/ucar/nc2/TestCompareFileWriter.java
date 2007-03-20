package ucar.nc2;

import junit.framework.*;
import java.io.*;
import java.util.*;

/** test FileWriting, then reading back and comparing to original. */

public class TestCompareFileWriter extends TestCase {
  static boolean showCompare = true, showEach = false;

  public TestCompareFileWriter( String name) {
    super(name);
  }

  public ArrayList files;
  public void testCompare() throws IOException {
    doOne(TestAll.upcShareTestDataDir+"satellite/gini/n0r_20041013_1852-compress", "C:/temp/n0r_20041013_1852.nc");
  }

  public void utestCompareAll() throws IOException {
    readAllDir(TestAll.upcShareTestDataDir+"satellite/gini/");
  }

  void readAllDir(String dirName) throws IOException {
    System.out.println("---------------Reading directory "+dirName);
    File allDir = new File( dirName);
    File[] allFiles = allDir.listFiles();

    for (int i = 0; i < allFiles.length; i++) {
      File f = allFiles[i];
      if (f.isDirectory()) continue;

      String name = f.getAbsolutePath();
      doOne(name, "C:/temp/data/temp.nc");
    }

    for (int i = 0; i < allFiles.length; i++) {
      File f = allFiles[i];
      if (f.isDirectory())
        readAllDir(allFiles[i].getAbsolutePath());
    }

  }

  private void doOne(String datasetIn, String filenameOut) throws IOException {
    NetcdfFile ncfileIn = ucar.nc2.dataset.NetcdfDataset.openFile(datasetIn, null);
    NetcdfFile ncfileOut = FileWriter.writeToFile( ncfileIn, filenameOut);
    TestCompare.compareFiles(ncfileIn, ncfileOut);

    ncfileIn.close();
    ncfileOut.close();
  }

}
