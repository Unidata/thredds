/*
 * Copyright 1998-2009 University Corporation for Atmospheric Research/Unidata
 *
 * Portions of this software were developed by the Unidata Program at the
 * University Corporation for Atmospheric Research.
 *
 * Access and use of this software shall impose the following obligations
 * and understandings on the user. The user is granted the right, without
 * any fee or cost, to use, copy, modify, alter, enhance and distribute
 * this software, and any derivative works thereof, and its supporting
 * documentation for any purpose whatsoever, provided that this entire
 * notice appears in all copies of the software, derivative works and
 * supporting documentation.  Further, UCAR requests that the user credit
 * UCAR/Unidata in any publications that result from the use of this
 * software or in any product that includes this software. The names UCAR
 * and/or Unidata, however, may not be used in any advertising or publicity
 * to endorse or promote any products or commercial entity unless specific
 * written permission is obtained from UCAR/Unidata. The user also
 * understands that UCAR/Unidata is not obligated to provide the user with
 * any support, consulting, training or assistance of any kind with regard
 * to the use, operation and performance of this software nor to provide
 * the user with any updates, revisions, new versions or "bug fixes."
 *
 * THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 * INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 * FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 * NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 * WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */
package ucar.nc2.iosp.hdf5;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import ucar.ma2.*;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Attribute;
import ucar.nc2.Variable;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;

import java.io.*;

/**
 * Test nc2 read JUnit framework.
 */
@Category(NeedsCdmUnitTest.class)
public class TestH5Vlength {
  File tempFile;
  PrintStream out;

  @Before
  public void setUp() throws Exception {
    tempFile = File.createTempFile("TestLongOffset", "out");
    out = new PrintStream(new FileOutputStream(tempFile));
  }

  @After
  public void tearDown() throws Exception {
    out.close();
    boolean status = tempFile.delete();
    if (!status) System.out.printf("delete failed%n");
  }

  @Test
  public void testVlengthAttribute() throws IOException {
    try (NetcdfFile ncfile = TestH5.openH5("support/vlstra.h5")) {
      Attribute att = ncfile.findGlobalAttribute("test_scalar");
      assert (null != att);
      assert (!att.isArray());
      assert (att.isString());
      assert (att.getStringValue().equals("This is the string for the attribute"));
    }
  }

  @Test
  public void testVlengthVariableChunked() throws IOException {
    try (NetcdfFile ncfile = TestH5.openH5("support/uvlstr.h5")) {

      Variable v = ncfile.findVariable("Space1");
      assert (null != v);
      assert (v.getDataType() == DataType.STRING);
      assert (v.getRank() == 1);
      assert (v.getShape()[0] == 9);

      try {
        Array data = v.read();
        assert (data.getElementType() == String.class);
        assert (data instanceof ArrayObject);
        IndexIterator iter = data.getIndexIterator();
        while (iter.hasNext()) {
          out.println(iter.next());
        }

      } catch (IOException e) {
        e.printStackTrace();
        assert false;
      }

      int[] origin = new int[]{3};
      int[] shape = new int[]{3};
      try {
        Array data2 = v.read(origin, shape);
        Index ima = data2.getIndex();
        assert (data2.getElementType() == String.class);
        assert (data2 instanceof ArrayObject);
        assert ((String) data2.getObject(ima.set(0))).startsWith("testing whether that nation");
        assert ((String) data2.getObject(ima.set(1))).startsWith("O Gloria inmarcesible!");
        assert ((String) data2.getObject(ima.set(2))).startsWith("bien germina ya!");
      } catch (IOException | InvalidRangeException e) {
        assert false;
      }

    }
  }

  @Test
  public void testVlengthVariable() throws IOException {
    try (NetcdfFile ncfile = TestH5.openH5("support/vlslab.h5")) {

      Variable v = ncfile.findVariable("Space1");
      assert (null != v);
      assert (v.getDataType() == DataType.STRING);
      assert (v.getRank() == 1);
      assert (v.getShape()[0] == 12);

      try {
        Array data = v.read();
        assert (data.getElementType() == String.class);
        assert (data instanceof ArrayObject);
        IndexIterator iter = data.getIndexIterator();
        while (iter.hasNext()) {
          out.println(iter.next());
        }

      } catch (IOException e) {
        assert false;
      }

      int[] origin = new int[]{4};
      int[] shape = new int[]{1};
      try {
        Array data2 = v.read(origin, shape);
        Index ima = data2.getIndex();
        assert (data2.getElementType() == String.class);
        assert (data2 instanceof ArrayObject);
        assert ( data2.getObject(ima.set(0))).equals("Five score and seven years ago our forefathers brought forth on this continent a new nation,");
      } catch (IOException | InvalidRangeException e) {
        assert false;
      }

    }
  }

  // from bsantos@ipfn.ist.utl.pt
  @Test
  public void testVlenEndian() throws IOException {
    testVlenEndian(TestN4reading.testDir+"vlenBigEndian.nc", 10);
    //testVlenEndian("C:/data/work/bruno/test3_p1_d1wave.nc", 10);
    testVlenEndian(TestN4reading.testDir+"vlenLittleEndian.nc", 100);
    //testVlenEndian("C:/data/work/bruno/fpscminicodac_1.nc", 100);
  }

  private void testVlenEndian(String filename, int n) throws IOException {
    try (NetcdfFile ncfile = NetcdfDataset.openFile(filename, null)) {

      Variable v = ncfile.findVariable("levels");
      assert (null != v);
      assert (v.getDataType() == DataType.INT);
      assert (v.getRank() == 2);
      assert (v.getShape()[0] == n) : v.getShape()[0];

      try {
        Array data = v.read();
        // assert(data.getElementType() instanceof ucar.ma2.ArrayInt.class) : data.getElementType();
        assert (data instanceof ArrayObject);
        IndexIterator iter = data.getIndexIterator();
        while (iter.hasNext()) {
          Array inner = (Array) iter.next();
          assert (inner instanceof ArrayInt.D1);
          int firstVal = inner.getInt(0);
          System.out.printf("%d (%d) = %s%n", firstVal, inner.getSize(), inner);
          assert (firstVal < Short.MAX_VALUE) : firstVal;
        }

      } catch (IOException e) {
        assert false;
      }

    }
  }

}
