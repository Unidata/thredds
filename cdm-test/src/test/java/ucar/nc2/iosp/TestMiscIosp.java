/*
 * Copyright 1998-2014 University Corporation for Atmospheric Research/Unidata
 *
 *   Portions of this software were developed by the Unidata Program at the
 *   University Corporation for Atmospheric Research.
 *
 *   Access and use of this software shall impose the following obligations
 *   and understandings on the user. The user is granted the right, without
 *   any fee or cost, to use, copy, modify, alter, enhance and distribute
 *   this software, and any derivative works thereof, and its supporting
 *   documentation for any purpose whatsoever, provided that this entire
 *   notice appears in all copies of the software, derivative works and
 *   supporting documentation.  Further, UCAR requests that the user credit
 *   UCAR/Unidata in any publications that result from the use of this
 *   software or in any product that includes this software. The names UCAR
 *   and/or Unidata, however, may not be used in any advertising or publicity
 *   to endorse or promote any products or commercial entity unless specific
 *   written permission is obtained from UCAR/Unidata. The user also
 *   understands that UCAR/Unidata is not obligated to provide the user with
 *   any support, consulting, training or assistance of any kind with regard
 *   to the use, operation and performance of this software nor to provide
 *   the user with any updates, revisions, new versions or "bug fixes."
 *
 *   THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 *   IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *   WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *   DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 *   INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 *   FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 *   NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 *   WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */

package ucar.nc2.iosp;

import org.junit.*;
import org.junit.experimental.categories.Category;
import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Attribute;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.nc2.constants.CDM;
import ucar.nc2.util.Misc;
import ucar.nc2.util.cache.FileCache;
import ucar.unidata.io.RandomAccessFile;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;
import ucar.unidata.util.test.TestDir;

import java.io.IOException;
import java.util.Arrays;

/**
 * Misc tests on iosp, mostly just sanity (opens ok)
 *
 * @author caron
 * @since 7/29/2014
 */
@Category(NeedsCdmUnitTest.class)
public class TestMiscIosp {
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
       System.out.printf("open %s %n", ncf.getLocation());

       String val = ncf.findAttValueIgnoreCase(null, "version", null);
       assert val != null;
       assert val.equals("SAT2004");

       Variable v = ncf.findVariable("snow");
       assert v != null;
       assert v.getDataType() == DataType.SHORT;

       Array data = v.read();
       assert Arrays.equals(data.getShape(), new int[]{1, 91, 181});
     }

   }

  @Test
  public void testUamiv() throws IOException {
    try (NetcdfFile ncfile = NetcdfFile.open(TestDir.cdmUnitTestDir + "formats/uamiv/uamiv.grid", null)) {
      System.out.printf("open %s %n", ncfile.getLocation());
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
      System.out.printf("open %s %n", ncf.getLocation());

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
      System.out.printf("open %s %n", ncf.getLocation());

      ucar.nc2.Variable v = ncf.findVariable("mask");
      assert v != null;
      assert v.getDataType() == DataType.FLOAT;
      Attribute att = v.findAttribute(CDM.MISSING_VALUE);
      assert att != null;
      assert att.getDataType() == DataType.FLOAT;
      assert Misc.closeEnough(att.getNumericValue().floatValue(), -9999.0f);

      Array data = v.read();
      assert Arrays.equals(data.getShape(), new int[]{1, 1, 180, 360});
    }
  }

  @Test
  public void testGradsWithRAFCache() throws IOException, InvalidRangeException {
    String fileIn = TestDir.cdmUnitTestDir + "formats/grads/mask.ctl";
    try (ucar.nc2.NetcdfFile ncf = ucar.nc2.NetcdfFile.open(fileIn)) {
      System.out.printf("open %s %n", ncf.getLocation());

      ucar.nc2.Variable v = ncf.findVariable("mask");
      assert v != null;
      assert v.getDataType() == DataType.FLOAT;
      Attribute att = v.findAttribute(CDM.MISSING_VALUE);
      assert att != null;
      assert att.getDataType() == DataType.FLOAT;
      assert Misc.closeEnough(att.getNumericValue().floatValue(), -9999.0f);

      Array data = v.read();
      assert Arrays.equals(data.getShape(), new int[]{1, 1, 180, 360});
    }
  }

  // @Test
  // dunno what kind of grads file this is.
  public void testGrads2() throws IOException, InvalidRangeException {
    String fileIn = TestDir.cdmUnitTestDir + "formats/grads/pdef.ctl";
    try (ucar.nc2.NetcdfFile ncf = ucar.nc2.NetcdfFile.open(fileIn)) {
      System.out.printf("open %s %n", ncf.getLocation());

      ucar.nc2.Variable v = ncf.findVariable("pdef");
      assert v != null;
      assert v.getDataType() == DataType.FLOAT;
      Attribute att = v.findAttribute(CDM.MISSING_VALUE);
      assert att != null;
      assert att.getDataType() == DataType.FLOAT;
      assert Misc.closeEnough(att.getNumericValue().floatValue(), -9999.0f);

      Array data = v.read();
      assert Arrays.equals(data.getShape(), new int[]{1, 1, 180, 360});
    }
  }

}
