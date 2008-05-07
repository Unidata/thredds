package ucar.nc2.ncml;

import junit.framework.TestCase;

import java.io.IOException;
import java.io.File;

import ucar.nc2.*;
import ucar.ma2.DataType;
import ucar.ma2.Array;
import ucar.ma2.IndexIterator;

public class TestOffAggNewSync extends TestCase {

  public TestOffAggNewSync(String name) {
    super(name);
  }

  String dataDir = "//zero/share/testdata/image/testSync/";

  public void testMove() throws IOException, InterruptedException {
    move(dataDir + "SUPER-NATIONAL_8km_WV_20051128_2100.gini");

    String filename = "file:./" + TestNcML.topDir + "offsite/aggNewSync.xml";
    NetcdfFile ncfile = NcMLReader.readNcML(filename, null);
    testAggCoordVar(ncfile, 7);
    ncfile.close();

    moveBack(dataDir + "SUPER-NATIONAL_8km_WV_20051128_2100.gini");

    ncfile = NcMLReader.readNcML(filename, null);
    testAggCoordVar(ncfile, 8);
    ncfile.close();
  }

  public void testRemove() throws IOException, InterruptedException {

    String filename = "file:./" + TestNcML.topDir + "offsite/aggNewSync.xml";
    NetcdfFile ncfile = NcMLReader.readNcML(filename, null);
    testAggCoordVar(ncfile, 8);
    System.out.println("");
    ncfile.close();

    move(dataDir + "SUPER-NATIONAL_8km_WV_20051128_2100.gini");

    ncfile = NcMLReader.readNcML(filename, null);
    testAggCoordVar(ncfile, 7);
    ncfile.close();

    moveBack(dataDir + "SUPER-NATIONAL_8km_WV_20051128_2100.gini");
  }

  public void testSync() throws IOException, InterruptedException {
    move(dataDir + "SUPER-NATIONAL_8km_WV_20051128_2100.gini");

    String filename = "file:./" + TestNcML.topDir + "offsite/aggNewSync.xml";
    NetcdfFile ncfile = NcMLReader.readNcML(filename, null);
    testAggCoordVar(ncfile, 7);

    moveBack(dataDir + "SUPER-NATIONAL_8km_WV_20051128_2100.gini");
    Thread.sleep(2000);

    ncfile.sync();
    testAggCoordVar(ncfile, 8);
    ncfile.close();
  }

  public void testSyncRemove() throws IOException, InterruptedException {
    String filename = "file:./" + TestNcML.topDir + "offsite/aggNewSync.xml";
    NetcdfFile ncfile = NcMLReader.readNcML(filename, null);
    testAggCoordVar(ncfile, 8);
    System.out.println("");

    move(dataDir + "SUPER-NATIONAL_8km_WV_20051128_2100.gini");
    Thread.sleep(2000);

    ncfile.sync();
    testAggCoordVar(ncfile, 7);
    ncfile.close();

    moveBack(dataDir + "SUPER-NATIONAL_8km_WV_20051128_2100.gini");
  }


  public void testAggCoordVar(NetcdfFile ncfile, int n) throws IOException {
    Variable time = ncfile.findVariable("time");
    assert null != time;
    assert time.getName().equals("time");
    assert time.getRank() == 1;
    assert time.getSize() == n : time.getSize();
    assert time.getShape()[0] == n;
    assert time.getDataType() == DataType.STRING;

    assert time.getDimension(0) == ncfile.findDimension("time");

    Array data = time.read();
    assert data.getRank() == 1;
    assert data.getSize() == n;
    assert data.getShape()[0] == n;
    assert data.getElementType() == String.class;

    String prev = null;
    IndexIterator dataI = data.getIndexIterator();
    while (dataI.hasNext()) {
      String curr = (String) dataI.getObjectNext();
      System.out.println(" coord=" + curr);
      assert (prev == null) || prev.compareTo(curr) < 0;
      prev = curr;
    }
  }

  void move(String filename) {
    File f = new File(filename);
    if (f.exists())
      f.renameTo(new File(filename + ".save"));
  }

  void moveBack(String filename) {
    File f = new File(filename + ".save");
    f.renameTo(new File(filename));
  }

}


