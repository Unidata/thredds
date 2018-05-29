/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.iosp;

import org.junit.*;
import org.junit.experimental.categories.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Attribute;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.nc2.constants.CDM;
import ucar.nc2.util.cache.FileCache;
import ucar.unidata.io.RandomAccessFile;
import ucar.unidata.util.test.Assert2;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;
import ucar.unidata.util.test.TestDir;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.Arrays;

/**
 * Misc tests on iosp, mostly just sanity (opens ok)
 *
 * @author caron
 * @since 7/29/2014
 */
@Category(NeedsCdmUnitTest.class)
public class TestMiscIosp {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static int leaks;

  @BeforeClass
  static public void startup() {
    RandomAccessFile.setDebugLeaks(true);
    RandomAccessFile.enableDefaultGlobalFileCache();
    leaks =  RandomAccessFile.getOpenFiles().size();
  }

  @AfterClass
  static public void checkLeaks() {
    FileCache.shutdown();
    RandomAccessFile.setGlobalFileCache(null);
    assert leaks == TestDir.checkLeaks();
    RandomAccessFile.setDebugLeaks(false);
  }

  @Test
  public void testFyiosp() throws IOException {
     String fileIn =  TestDir.cdmUnitTestDir + "formats/fysat/SATE_L3_F2C_VISSR_MWB_SNO_CNB-DAY-2008010115.AWX";
     try (ucar.nc2.NetcdfFile ncf = ucar.nc2.NetcdfFile.open(fileIn)) {
       logger.debug("open {}", ncf.getLocation());

       String val = ncf.findAttValueIgnoreCase(null, "version", null);
       assert val != null;
       assert val.equals("SAT2004");

       Variable v = ncf.findVariable("snow");
       assert v != null;
       assert v.getDataType() == DataType.USHORT;

       Array data = v.read();
       assert Arrays.equals(data.getShape(), new int[]{1, 91, 181});
     }

   }

  @Test
  public void testUamiv() throws IOException {
    try (NetcdfFile ncfile = NetcdfFile.open(TestDir.cdmUnitTestDir + "formats/uamiv/uamiv.grid", null)) {
      logger.debug("open {}", ncfile.getLocation());
      ucar.nc2.Variable v = ncfile.findVariable("UP");
      assert v != null;
      assert v.getDataType() == DataType.FLOAT;

      Array data = v.read();
      assert Arrays.equals(data.getShape(), new int[]{12, 5, 7, 6});
    }
  }

  @Test
  public void testGini() throws IOException, InvalidRangeException {
    String fileIn = TestDir.cdmUnitTestDir + "formats/gini/n0r_20041013_1852-compress";
    try (ucar.nc2.NetcdfFile ncf = ucar.nc2.NetcdfFile.open(fileIn)) {
      logger.debug("open {}", ncf.getLocation());

      ucar.nc2.Variable v = ncf.findVariable("Reflectivity");
      assert v != null;
      assert v.getDataType() == DataType.FLOAT;

      Array data = v.read();
      assert Arrays.equals(data.getShape(), new int[]{1, 3000, 4736});
    }
  }


  @Test
  public void testGrads() throws IOException, InvalidRangeException {
    String fileIn = TestDir.cdmUnitTestDir + "formats/grads/mask.ctl";
    try (ucar.nc2.NetcdfFile ncf = ucar.nc2.NetcdfFile.open(fileIn)) {
      logger.debug("open {}", ncf.getLocation());

      ucar.nc2.Variable v = ncf.findVariable("mask");
      assert v != null;
      assert v.getDataType() == DataType.FLOAT;
      Attribute att = v.findAttribute(CDM.MISSING_VALUE);
      assert att != null;
      assert att.getDataType() == DataType.FLOAT;
      Assert2.assertNearlyEquals(att.getNumericValue().floatValue(), -9999.0f);

      Array data = v.read();
      assert Arrays.equals(data.getShape(), new int[]{1, 1, 180, 360});
    }
  }

  @Test
  public void testGradsWithRAFCache() throws IOException, InvalidRangeException {
    String fileIn = TestDir.cdmUnitTestDir + "formats/grads/mask.ctl";
    try (ucar.nc2.NetcdfFile ncf = ucar.nc2.NetcdfFile.open(fileIn)) {
      logger.debug("open {}", ncf.getLocation());

      ucar.nc2.Variable v = ncf.findVariable("mask");
      assert v != null;
      assert v.getDataType() == DataType.FLOAT;
      Attribute att = v.findAttribute(CDM.MISSING_VALUE);
      assert att != null;
      assert att.getDataType() == DataType.FLOAT;
      Assert2.assertNearlyEquals(att.getNumericValue().floatValue(), -9999.0f);

      Array data = v.read();
      assert Arrays.equals(data.getShape(), new int[]{1, 1, 180, 360});
    }
  }

  // @Test
  // dunno what kind of grads file this is.
  public void testGrads2() throws IOException, InvalidRangeException {
    String fileIn = TestDir.cdmUnitTestDir + "formats/grads/pdef.ctl";
    try (ucar.nc2.NetcdfFile ncf = ucar.nc2.NetcdfFile.open(fileIn)) {
      logger.debug("open {}", ncf.getLocation());

      ucar.nc2.Variable v = ncf.findVariable("pdef");
      assert v != null;
      assert v.getDataType() == DataType.FLOAT;
      Attribute att = v.findAttribute(CDM.MISSING_VALUE);
      assert att != null;
      assert att.getDataType() == DataType.FLOAT;
      Assert2.assertNearlyEquals(att.getNumericValue().floatValue(), -9999.0f);

      Array data = v.read();
      assert Arrays.equals(data.getShape(), new int[]{1, 1, 180, 360});
    }
  }

}
