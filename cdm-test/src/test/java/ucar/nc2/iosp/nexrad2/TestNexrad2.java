/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.iosp.nexrad2;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.ma2.Array;
import ucar.ma2.IndexIterator;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.VariableDS;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;
import ucar.unidata.util.test.TestDir;

import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;

@Category(NeedsCdmUnitTest.class)
public class TestNexrad2 {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  // FIXME: This method sucks: it doesn't fail when dirName can't be read.
  @Test
  public void testRead() throws IOException {
    long start = System.currentTimeMillis();
    TestDir.actOnAll(TestDir.cdmUnitTestDir + "formats/nexrad/level2/VCP11", null, new MyAct(true));
    TestDir.actOnAll(TestDir.cdmUnitTestDir + "formats/nexrad/level2/VCP11", null, new MyAct(false));
    long took = System.currentTimeMillis() - start;
    System.out.println("that took = "+took+" msec");
  }

  private class MyAct implements TestDir.Act {
    boolean deleteUncompress;

    private MyAct(boolean deleteUncompress) {
      this.deleteUncompress = deleteUncompress;
    }

    @Override
    public int doAct(String filename) throws IOException {
      if (deleteUncompress && filename.endsWith(".uncompress")) {
        File uf = new File(filename);
        if (!uf.delete())
          System.out.printf("Failed to delete %s%n", filename);
        return 0;
      }

      try (NetcdfFile ncfile = NetcdfDataset.openFile(filename, null)) {
        return testRead(ncfile);
      }
    }
  }

  private int testRead( NetcdfFile nexrad2) throws IOException {
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

    return 1;

  }

  static public boolean testCoordSystem( NetcdfFile nexrad2) throws IOException {
    Dimension scanR = nexrad2.findDimension("scanR");
    assert null != scanR;
    Dimension scanV = nexrad2.findDimension("scanV");
    assert null != scanV;

    assert scanR.getLength() == scanV.getLength();

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

  @Test
  public void testCoordSys() throws IOException {
    //NetcdfDataset ncd = NetcdfDataset.openDataset(
    //        "dods://localhost:8080/thredds/dodsC/testAll/Level2_KSOX_20051010_2322.ar2v", false, null);
    String filename = TestDir.cdmUnitTestDir + "formats/nexrad/level2/Level2_KYUX_20060527_2335.ar2v";
    try (NetcdfFile ncfile = NetcdfDataset.openFile( filename, null)) {
      testCoordSystem(ncfile);
    }
  }

  @Test
  public void testBzipProblem() throws IOException, InvalidRangeException {
    // file where there was an error unzipping the file
    String filename = TestDir.cdmUnitTestDir + "formats/nexrad/level2/Level2_KFTG_20060818_1814.ar2v.uncompress.missingradials";
    try (NetcdfDataset ncd = NetcdfDataset.openDataset( filename)) {

      VariableDS azi = (VariableDS) ncd.findVariable("azimuthR");
      assert azi != null;
      VariableDS elev = (VariableDS) ncd.findVariable("elevationR");
      assert elev != null;
      VariableDS time = (VariableDS) ncd.findVariable("timeR");
      assert time != null;
      VariableDS r = (VariableDS) ncd.findVariable("Reflectivity");
      assert r != null;
      checkMissingValues(elev, azi, time, r);

      azi = (VariableDS) ncd.findVariable("azimuthV");
      assert azi != null;
      elev = (VariableDS) ncd.findVariable("elevationV");
      assert elev != null;
      time = (VariableDS) ncd.findVariable("timeV");
      assert time != null;
      r = (VariableDS) ncd.findVariable("RadialVelocity");
      assert r != null;
      checkMissingValues(elev, azi, time, r);

      r = (VariableDS) ncd.findVariable("SpectrumWidth");
      assert r != null;
      checkMissingValues(elev, azi, time, r);
    }
  }

  private void checkMissingValues(VariableDS elev, VariableDS azi, VariableDS time, VariableDS q) throws IOException, InvalidRangeException {
    Array elevData = elev.read();
    IndexIterator elevII = elevData.getIndexIterator();
    Array aziData = azi.read();
    IndexIterator aziII = aziData.getIndexIterator();
    Array timeData = time.read();
    IndexIterator timeII = timeData.getIndexIterator();
    while (elevII.hasNext()) {
      float elevValue = elevII.getFloatNext();
      float aziValue = aziII.getFloatNext();
      assert azi.isMissing(aziValue) == elev.isMissing(elevValue);

      // LOOK missing data broken for non-float coordinate axes
      //int timeValue = timeII.getIntNext();
      //assert azi.isMissing(aziValue) == time.isMissing(timeValue) : " azi= "+aziValue +" time= "+timeValue;
    }

    int[] shape = q.getShape();
    int rank = q.getRank();
    int[] origin = new int[rank];
    shape[rank-1] = 1;
    Array qData = q.read(origin, shape);
    assert qData.getSize() == aziData.getSize();

    IndexIterator qII = qData.getIndexIterator();
    aziII = aziData.getIndexIterator();
    while (qII.hasNext()) {
      float qValue = qII.getFloatNext();
      float aziValue = aziII.getFloatNext();
      if (azi.isMissing(aziValue))
        assert q.isMissing(qValue);
    }
  }

}
