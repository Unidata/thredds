package ucar.nc2.iosp.nexrad2;

import ucar.nc2.*;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.ma2.*;

import java.io.IOException;
import java.io.File;

import junit.framework.TestCase;

/**
 * Created by IntelliJ IDEA.
 * User: caron
 * Date: Oct 16, 2005
 * Time: 1:05:16 PM
 * To change this template use File | Settings | File Templates.
 */
public class TestNexrad2 extends TestCase {

  public TestNexrad2( String name) {
    super(name);
  }

  public void testRead() throws IOException {
    long start = System.currentTimeMillis();
    doDirectory(TestAll.testdataDir + "radar/nexrad/level2/VCP11", false);
    long took = System.currentTimeMillis() - start;
    System.out.println("that took = "+took+" msec");
  }

  private void doDirectory(String dirName, boolean alwaysUncompress) throws IOException {

    File dir = new File(dirName);
    File[] files = dir.listFiles();
    if (alwaysUncompress) {
      for (int i = 0; i < files.length; i++) {
        File file = files[i];
        String path = file.getPath();
        if (path.endsWith(".uncompress"))
          file.delete();
      }
    }

    for (int i = 0; i < files.length; i++) {
      File file = files[i];
      String path = file.getPath();
      if (path.endsWith(".uncompress")) continue;

      if (file.isDirectory())
        doDirectory(path, alwaysUncompress);
      else {
        NetcdfFile ncfile = NetcdfDataset.openFile(path, null);
        testRead(ncfile);
      }

    }
  }

  private void testRead( NetcdfFile nexrad2) throws IOException {
    System.out.println(nexrad2.getLocation());

    Dimension scanR = nexrad2.findDimension("scanR");
    assert null != scanR;
    Dimension scanV = nexrad2.findDimension("scanV");
    assert null != scanV;

    assert scanR.getLength() == scanV.getLength();

    Variable elevR =  nexrad2.findVariable("elevationR");
    assert elevR != null;
    Array elevRdata = elevR.read();

    Variable elevV =  nexrad2.findVariable("elevationV");
    assert elevV != null;
    Array elevVdata = elevV.read();

    assert elevRdata.getSize() ==  elevVdata.getSize();

    Variable v =  nexrad2.findVariable("Reflectivity");
    assert v != null;
    Array data = v.read();

    v =  nexrad2.findVariable("RadialVelocity");
    assert v != null;
    data = v.read();

    v =  nexrad2.findVariable("SpectrumWidth");
    assert v != null;
    data = v.read();

  }

  static public boolean testCoordSystem( NetcdfFile nexrad2) throws IOException {
    Dimension scanR = nexrad2.findDimension("scanR");
    assert null != scanR;
    Dimension scanV = nexrad2.findDimension("scanV");
    assert null != scanV;

    assert scanR.getLength() == scanR.getLength();

    Variable elevR =  nexrad2.findVariable("elevationR");
    assert elevR != null;
    Array elevRdata = elevR.read();
    IndexIterator elevRiter = elevRdata.getIndexIterator();

    Variable elevV =  nexrad2.findVariable("elevationV");
    assert elevV != null;
    Array elevVdata = elevV.read();
    IndexIterator elevViter = elevVdata.getIndexIterator();

    assert elevRdata.getSize() ==  elevVdata.getSize();

    int count = 0;
    boolean ok = true;
    while (elevRiter.hasNext()) {
      if (elevRiter.getFloatNext() != elevViter.getFloatNext()) {
        ok = false;
        System.out.println(count+" "+elevRiter.getFloatCurrent()+" != "+elevViter.getFloatCurrent());
      }
      count++;
    }

    return ok;
  }



  public void utestCoordSys() throws IOException {
    //NetcdfDataset ncd = NetcdfDataset.openDataset(
    //        "dods://localhost:8080/thredds/dodsC/testAll/Level2_KSOX_20051010_2322.ar2v", false, null);
    String filename = TestAll.testdataDir + "radar/nexrad/level2/Level2_KYUX_20060527_2335.ar2v";
    NetcdfFile ncfile = NetcdfDataset.openFile( filename, null);
    testCoordSystem( ncfile);
  }
}
