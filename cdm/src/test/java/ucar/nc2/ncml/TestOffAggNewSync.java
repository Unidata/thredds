package ucar.nc2.ncml;

import junit.framework.TestCase;

import java.io.IOException;
import java.io.File;
import java.io.StringReader;

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

    String filename = "file:./" + TestNcML.topDir + "offsite/aggExistingSync.xml";
    NetcdfFile ncfile = NcMLReader.readNcML(filename, null);
    testAggCoordVar(ncfile, 7);
    ncfile.close();

    moveBack(dataDir + "SUPER-NATIONAL_8km_WV_20051128_2100.gini");

    ncfile = NcMLReader.readNcML(filename, null);
    testAggCoordVar(ncfile, 8);
    ncfile.close();
  }


  private String aggExistingSync =
            "<?xml version='1.0' encoding='UTF-8'?>\n" +
            "<netcdf xmlns='http://www.unidata.ucar.edu/namespaces/netcdf/ncml-2.2'>\n" +
            "  <aggregation  dimName='time' type='joinExisting' recheckEvery='1 sec' >\n" +
            "    <variableAgg name='IR_WV'/>\n" +
            "    <scan location='//zero/share/testdata/image/testSync' suffix='.gini' dateFormatMark='SUPER-NATIONAL_8km_WV_#yyyyMMdd_HHmm'/>\n" +
            "  </aggregation>\n" +
            "</netcdf>";

  public void testRemove() throws IOException, InterruptedException {
    NetcdfFile ncfile = NcMLReader.readNcML(new StringReader(aggExistingSync), "aggExistingSync", null);
    testAggCoordVar(ncfile, 8);
    System.out.println("");
    ncfile.close();

    boolean ok = move(dataDir + "SUPER-NATIONAL_8km_WV_20051128_2100.gini");
    int nfiles = ok ? 7 : 8;  // sometimes fails

    ncfile = NcMLReader.readNcML(new StringReader(aggExistingSync), "aggExistingSync", null);
    testAggCoordVar(ncfile, nfiles);
    ncfile.close();

    moveBack(dataDir + "SUPER-NATIONAL_8km_WV_20051128_2100.gini");
  }

  public void testSync() throws IOException, InterruptedException {
    move(dataDir + "SUPER-NATIONAL_8km_WV_20051128_2100.gini");

    NetcdfFile ncfile = NcMLReader.readNcML(new StringReader(aggExistingSync), "aggExistingSync", null);
    testAggCoordVar(ncfile, 7);

    moveBack(dataDir + "SUPER-NATIONAL_8km_WV_20051128_2100.gini");
    Thread.sleep(2000);

    ncfile.sync();
    testAggCoordVar(ncfile, 8);
    ncfile.close();
  }

  public void testSyncRemove() throws IOException, InterruptedException {
    NetcdfFile ncfile = NcMLReader.readNcML(new StringReader(aggExistingSync), "aggExistingSync", null);
    testAggCoordVar(ncfile, 8);
    System.out.println("");

    boolean ok = move(dataDir + "SUPER-NATIONAL_8km_WV_20051128_2100.gini");
    int nfiles = ok ? 7 : 8;  // sometimes fails
    Thread.sleep(2000);

    ncfile.sync();
    testAggCoordVar(ncfile, nfiles);
    ncfile.close();

    moveBack(dataDir + "SUPER-NATIONAL_8km_WV_20051128_2100.gini");
  }


  public void testAggCoordVar(NetcdfFile ncfile, int n) throws IOException {
    Variable time = ncfile.findVariable("time");
    assert null != time;
    assert time.getName().equals("time");
    assert time.getRank() == 1;
    assert time.getSize() == n : time.getSize() +" != " + n;
    assert time.getShape()[0] == n;
    assert time.getDataType() == DataType.DOUBLE;

    assert time.getDimension(0) == ncfile.findDimension("time");

    Array data = time.read();
    assert data.getRank() == 1;
    assert data.getSize() == n;
    assert data.getShape()[0] == n;
    assert data.getElementType() == double.class;

    double prev = Double.NaN;
    IndexIterator dataI = data.getIndexIterator();
    while (dataI.hasNext()) {
      double dval = dataI.getDoubleNext();
      System.out.println(" coord=" + dval);
      assert (Double.isNaN(prev) || dval > prev);
      prev = dval;
    }
  }

  boolean move(String filename) {
    File f = new File(filename);
    if (f.exists())
      return f.renameTo(new File(filename + ".save"));
    return false;
  }

  void moveBack(String filename) {
    File f = new File(filename + ".save");
    f.renameTo(new File(filename));
  }

}


