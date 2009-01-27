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

import junit.framework.*;
import ucar.ma2.*;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Attribute;
import ucar.nc2.Variable;

import java.io.*;

/** Test nc2 read JUnit framework. */

public class TestH5Vlength extends TestCase {

  public TestH5Vlength( String name) {
    super(name);
  }

  File tempFile;
  PrintStream out;
  protected void setUp() throws Exception {
    tempFile = File.createTempFile("TestLongOffset", "out");
    out = new PrintStream( new FileOutputStream( tempFile));
  }
  protected void tearDown() throws Exception {
    out.close();
    tempFile.delete();
  }

  public void testVlengthAttribute() throws IOException {
    NetcdfFile ncfile = TestH5.openH5("support/vlstra.h5");

    Attribute att = ncfile.findGlobalAttribute("test_scalar");
    assert( null != att);
    assert( !att.isArray());
    assert( att.isString());
    assert( att.getStringValue().equals("This is the string for the attribute"));
    ncfile.close();
  }

  public void testVlengthVariableChunked() throws IOException {
    NetcdfFile ncfile = TestH5.openH5("support/uvlstr.h5");

    Variable v = ncfile.findVariable("Space1");
    assert( null != v);
    assert( v.getDataType() == DataType.STRING);
    assert( v.getRank() == 1);
    assert( v.getShape()[0] == 9);

    try {
      Array data = v.read();
      assert(data.getElementType() == String.class);
      assert (data instanceof ArrayObject);
      IndexIterator iter = data.getIndexIterator();
      while (iter.hasNext()) {
        out.println(iter.next());
      }

    } catch (IOException e) {
      e.printStackTrace();
      assert false;
    }

    int[] origin = new int[] {3};
    int[] shape = new int[] {3};
    try {
      Array data2 = v.read(origin, shape);
      Index ima = data2.getIndex();
      assert(data2.getElementType() == String.class);
      assert (data2 instanceof ArrayObject);
      assert ((String)data2.getObject(ima.set(0))).startsWith("testing whether that nation");
      assert ((String)data2.getObject(ima.set(1))).startsWith("O Gloria inmarcesible!");
      assert ((String)data2.getObject(ima.set(2))).startsWith("bien germina ya!");
    }
    catch (IOException e) { assert false; }
    catch (InvalidRangeException e) { assert false; }

    ncfile.close();
  } // */

  public void testVlengthVariable() throws IOException {
    NetcdfFile ncfile = TestH5.openH5("support/vlslab.h5");

    Variable v = ncfile.findVariable("Space1");
    assert( null != v);
    assert( v.getDataType() == DataType.STRING);
    assert( v.getRank() == 1);
    assert( v.getShape()[0] == 12);

    try {
      Array data = v.read();
      assert(data.getElementType() == String.class);
      assert (data instanceof ArrayObject);
      IndexIterator iter = data.getIndexIterator();
      while (iter.hasNext()) {
        out.println(iter.next());
      }

    } catch (IOException e) { assert false; }

    int[] origin = new int[] {4};
    int[] shape = new int[] {1};
    try {
      Array data2 = v.read(origin, shape);
      Index ima = data2.getIndex();
      assert(data2.getElementType() == String.class);
      assert (data2 instanceof ArrayObject);
      assert ((String)data2.getObject(ima.set(0))).equals("Five score and seven years ago our forefathers brought forth on this continent a new nation,");
    }
    catch (IOException e) { assert false; }
    catch (InvalidRangeException e) { assert false; }

    ncfile.close();
  } // */

}
