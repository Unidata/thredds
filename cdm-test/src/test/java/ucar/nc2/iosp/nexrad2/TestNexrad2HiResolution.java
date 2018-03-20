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
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;
import ucar.unidata.util.test.TestDir;

import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;

@Category(NeedsCdmUnitTest.class)
public class TestNexrad2HiResolution {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Test
  public void testRead() throws IOException {
    long start = System.currentTimeMillis();
    TestDir.actOnAll(TestDir.cdmUnitTestDir + "formats/nexrad/newLevel2/testfiles", null, new MyAct(true));
    TestDir.actOnAll(TestDir.cdmUnitTestDir + "formats/nexrad/newLevel2/testfiles", new TestDir.FileFilterFromSuffixes(".bz2"), new MyAct(false));
    //doDirectory("/upc/share/testdata2/radar/nexrad/newLevel2/testfiles", false);
    long took = System.currentTimeMillis() - start;
    System.out.println("that took = " + took + " msec");
  }

  private class MyAct implements TestDir.Act {
    boolean deleteUncompress;

    private MyAct(boolean deleteUncompress) {
      this.deleteUncompress = deleteUncompress;
    }

    @Override
    public int doAct(String filename) throws IOException {
      if (deleteUncompress && !filename.endsWith(".bz2")) {
        File uf = new File(filename);
        if (!uf.delete())
          System.out.printf("Failed to delete %s%n", filename);
        return 0;
      }

      try (NetcdfFile ncfile = NetcdfDataset.openFile(filename, null)) {
        testRead(ncfile);
        testCoordSystem(ncfile);
        return 1;
      }
    }
  }


  private void testRead(NetcdfFile nexrad2) throws IOException {
    Dimension scanR = nexrad2.findDimension("scanR");
    assert null != scanR;
    Dimension scanR_HI = nexrad2.findDimension("scanR_HI");
    assert null != scanR_HI;
    Dimension scanV = nexrad2.findDimension("scanV");
    assert null != scanV;
    Dimension scanV_HI = nexrad2.findDimension("scanV_HI");
    assert null != scanV_HI;

    assert scanR.getLength() == scanV.getLength();

    Variable elevR = nexrad2.findVariable("elevationR");
    assert elevR != null;
    Array elevRdata = elevR.read();
    Variable elevR_HI = nexrad2.findVariable("elevationR_HI");
    assert elevR_HI != null;
    Array elevR_HIdata = elevR_HI.read();
    assert elevR_HIdata != null;

    Variable elevV = nexrad2.findVariable("elevationV");
    assert elevV != null;
    Array elevVdata = elevV.read();
    Variable elevV_HI = nexrad2.findVariable("elevationV_HI");
    assert elevV_HI != null;
    Array elevV_HIdata = elevV_HI.read();
    assert elevV_HIdata != null;

    assert elevRdata.getSize() == elevVdata.getSize();

    Variable v = nexrad2.findVariable("Reflectivity");
    assert v != null;
    Array data = v.read();

    v = nexrad2.findVariable("RadialVelocity");
    assert v != null;
    data = v.read();

    v = nexrad2.findVariable("SpectrumWidth");
    assert v != null;
    data = v.read();

    v = nexrad2.findVariable("Reflectivity_HI");
    assert v != null;
    data = v.read();

    v = nexrad2.findVariable("RadialVelocity_HI");
    assert v != null;
    data = v.read();

    v = nexrad2.findVariable("SpectrumWidth_HI");
    assert v != null;
    if (v != null)
      data = v.read();
  }

  private void testCoordSystem(NetcdfFile nexrad2) throws IOException {
    Dimension scanR = nexrad2.findDimension("scanR");
    assert null != scanR;
    Dimension scanR_HI = nexrad2.findDimension("scanR_HI");
    assert null != scanR_HI;
    Dimension scanV = nexrad2.findDimension("scanV");
    assert null != scanV;
    Dimension scanV_HI = nexrad2.findDimension("scanV_HI");
    assert null != scanV_HI;

    assert scanR.getLength() == scanV.getLength();

    Variable elevR = nexrad2.findVariable("elevationR");
    assert elevR != null;
    Array elevRdata = elevR.read();
    IndexIterator elevRiter = elevRdata.getIndexIterator();
    Variable elevR_HI = nexrad2.findVariable("elevationR_HI");
    assert elevR_HI != null;
    Array elevRdataHI = elevR_HI.read();
    IndexIterator elevRiterHI = elevRdataHI.getIndexIterator();

    Variable elevV = nexrad2.findVariable("elevationV");
    assert elevV != null;
    Array elevVdata = elevV.read();
    IndexIterator elevViter = elevVdata.getIndexIterator();
    Variable elevV_HI = nexrad2.findVariable("elevationV_HI");
    assert elevV_HI != null;
    Array elevVdataHI = elevV.read();
    IndexIterator elevViterHI = elevVdataHI.getIndexIterator();

    assert elevRdata.getSize() == elevVdata.getSize();

    int count = 0;
    boolean ok = true;
    while (elevRiter.hasNext()) {
      if (elevRiter.getFloatNext() != elevViter.getFloatNext()) {
        ok = false;
        System.out.println(count + " " + elevRiter.getFloatCurrent() + " != " + elevViter.getFloatCurrent());
      }
      count++;
    }
    count = 0;
    while (elevRiterHI.hasNext()) {
      if (Float.isNaN(elevRiterHI.getFloatNext())) {
        ok = false;
        System.out.println("elevationR_HI contains Float.NAN " + count);
      }
      count++;
    }
    count = 0;
    while (elevViterHI.hasNext()) {
      if (Float.isNaN(elevViterHI.getFloatNext())) {
        ok = false;
        System.out.println("elevationV_HI contains Float.NAN " + count);
      }
      count++;
    }

    assert ok;
  }


}
