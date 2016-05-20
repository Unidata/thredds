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
package ucar.nc2.dods;

import java.io.IOException;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import ucar.ma2.Array;
import ucar.ma2.ArrayByte;
import ucar.ma2.ArrayDouble;
import ucar.ma2.ArrayFloat;
import ucar.ma2.ArrayInt;
import ucar.ma2.ArrayObject;
import ucar.ma2.ArrayShort;
import ucar.ma2.DataType;
import ucar.ma2.IndexIterator;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Variable;
import ucar.unidata.util.test.category.NeedsExternalResource;

import static org.junit.Assert.assertEquals;

/** Test nc2 dods in the JUnit framework. */
@Category(NeedsExternalResource.class)
public class TestDODSMultiArrayPrimitiveTypes {

  @org.junit.Test
  public void testArrays() throws IOException {
    DODSNetcdfFile dodsfile = TestDODSRead.open("test.03");

    Variable v = null;
    Array a = null;

    // byte
    assert(null != (v = dodsfile.findVariable("b")));
    assert v.getShortName().equals("b");
    assert v.getRank() == 3;
    assert v.getSize() == 24;
    assert v.getDataType() == DataType.BYTE;
    a = v.read();
    assert a.getElementType() == byte.class;
    assert a instanceof ArrayByte.D3;
    IndexIterator iter = a.getIndexIterator();
    int count = 0;
    while (iter.hasNext()) {
      assert iter.getByteNext() == count;
      count++;
    }


    // int16
    assert(null != (v = dodsfile.findVariable("i16")));
    assert v.getShortName().equals("i16");
    assert v.getRank() == 3;
    assert v.getSize() == 8;
    assert v.getDataType() == DataType.SHORT;
    a = v.read();
    assert a.getElementType() == short.class;
    assert a instanceof ArrayShort.D3;
    iter = a.getIndexIterator();
    count = 0;
    while (iter.hasNext()) {
      assert iter.getShortNext() == 256 * count;
      count++;
    }

    // int32
    assert(null != (v = dodsfile.findVariable("i32")));
    assert v.getShortName().equals("i32");
    assert v.getRank() == 3;
    assert v.getSize() == 24;
    assert v.getDataType() == DataType.INT;
    a = v.read();
    assert a.getElementType() == int.class;
    assert a instanceof ArrayInt.D3;
    iter = a.getIndexIterator();
    count = 0;
    while (iter.hasNext()) {
      assert iter.getIntNext() == 2048 * count;
      count++;
    }


    // uint16
    assert(null != (v = dodsfile.findVariable("ui16")));
    assert v.getShortName().equals("ui16");
    assert v.getRank() == 3;
    assert v.getSize() == 8;
    assert v.getDataType() == DataType.SHORT;
    assert v.isUnsigned();

    a = v.read();
    assert a.getElementType() == short.class;
    assert a instanceof ArrayShort.D3;
    iter = a.getIndexIterator();
    count = 0;
    while (iter.hasNext()) {
      assert iter.getIntNext() == 1024 * count;
      count++;
    }


    // uint32
    assert(null != (v = dodsfile.findVariable("ui32")));
    assert v.getShortName().equals("ui32");
    assert v.getRank() == 5;
    assert v.getSize() == 16 * 9;
    assert v.getDataType() == DataType.INT : v.getDataType();
    assert v.isUnsigned();
    a = v.read();
    assert a.getElementType() == int.class;
    assert a instanceof ArrayInt.D5;
    iter = a.getIndexIterator();
    count = 0;
    while (iter.hasNext()) {
      assert iter.getLongNext() == 4096 * count;
      count++;
    }

    // float
    assert(null != (v = dodsfile.findVariable("f32")));
    assert v.getShortName().equals("f32");
    assert v.getRank() == 3;
    assert v.getSize() == 8;
    assert v.getDataType() == DataType.FLOAT : v.getDataType();
    a = v.read();
    assert a.getElementType() == float.class;
    assert a instanceof ArrayFloat.D3;

    float[] tFloat32 = {0.0f, 0.009999833f,
      0.019998666f, 0.029995501f,
    0.039989334f, 0.04997917f,
    0.059964005f, 0.06994285f };

    iter = a.getIndexIterator();
    count = 0;
    while (iter.hasNext()) {
      assertEquals(iter.getFloatNext(), tFloat32[count], 1.0e-5);
      count++;
    }

    // double
    assert(null != (v = dodsfile.findVariable("f64")));
    assert v.getShortName().equals("f64");
    assert v.getRank() == 3;
    assert v.getSize() == 8;
    assert v.getDataType() == DataType.DOUBLE : v.getDataType();
    a = v.read();
    assert a.getElementType() == double.class;
    assert a instanceof ArrayDouble.D3;

    double[] tFloat64 = new double[] { 1.0, 0.9999500004166653,
      0.9998000066665778, 0.9995500337489875,
      0.9992001066609779, 0.9987502603949663,
      0.9982005399352042, 0.9975510002532796 };

    iter = a.getIndexIterator();
    count = 0;
    while (iter.hasNext()) {
      assertEquals(iter.getDoubleNext(), tFloat64[count], 1.0e-9);
      count++;
    }

    // string
    assert(null != (v = dodsfile.findVariable("s0")));
    assert v.getShortName().equals("s0");
    assert v.getRank() == 3;
    assert v.getDataType() == DataType.STRING : v.getDataType();
    a = v.read();
    assert a.getElementType() == String.class;
    assert a instanceof ArrayObject.D3;
    IndexIterator siter = a.getIndexIterator();
    count = 0;
    while (siter.hasNext()) {
      String str = (String) siter.next();
      assert str.equals("This is a data test string (pass "+count+").") : str;
      count++;
    }

    // url
    assert(null != (v = dodsfile.findVariable("u")));
    assert v.getShortName().equals("u");
    assert v.getRank() == 3;
    assert v.getDataType() == DataType.STRING : v.getDataType();
    a = v.read();
    assert a.getElementType() == String.class;
    assert a instanceof ArrayObject.D3;
    IndexIterator siter2 = a.getIndexIterator();
    count = 0;
    while (siter2.hasNext()) {
      String str = (String) siter2.next();
      assert str.equals("http://www.opendap.org") || str.equals("http://www.dods.org") : str;
      count++;
    }

  }

  @Test
  public void testStride() throws IOException, InvalidRangeException {
    DODSNetcdfFile dodsfile = TestDODSRead.open("test.03");

    DODSVariable v = null;
    Array a = null;

    // uint32
    assert(null != (v = (DODSVariable) dodsfile.findVariable("b")));
    assert v.getShortName().equals("b");
    assert v.getRank() == 3;
    assert v.getSize() == 24;
    assert v.getDataType() == DataType.BYTE;

    // Byte b[2][3][4];
    a = v.read( "0:1:2, 0:2:1, 0:3:2" );
    assert a.getElementType() == byte.class;
    assert a instanceof ArrayByte.D3;
    assert a.getRank() == 3;
    assert a.getSize() == 6;
    assert a.getShape()[0] == 1;
    assert a.getShape()[1] == 3;
    assert a.getShape()[2] == 2;

    IndexIterator iter = a.getIndexIterator();
    assert iter.getByteNext() == 0;
    assert iter.getByteNext() == 2;
    assert iter.getByteNext() == 4;
    assert iter.getByteNext() == 6;
    assert iter.getByteNext() == 8;
    assert iter.getByteNext() == 10;
    assert !iter.hasNext();

    System.out.println("TestDODSMultiArrayPrimitiveTypes TestStride ok");
  }

  @Test
  public void testSection() throws IOException, InvalidRangeException {
    DODSNetcdfFile.setPreload( false);
    DODSNetcdfFile dodsfile = TestDODSRead.open("test.03");
    DODSNetcdfFile.setPreload( true);

    DODSVariable v = null;
    Array a = null;

    // uint32
    assert(null != (v = (DODSVariable) dodsfile.findVariable("b")));
    assert v.getShortName().equals("b");
    assert v.getRank() == 3;
    assert v.getSize() == 24;
    assert v.getDataType() == DataType.BYTE;
    v.setCaching(false);

    // Byte b[2][3][4];
    a = v.read( "0:1:2, 0:2:1, 0:3:2" );
    assert a.getElementType() == byte.class;
    assert a instanceof ArrayByte.D3;
    assert a.getRank() == 3;
    assert a.getSize() == 6;
    assert a.getShape()[0] == 1;
    assert a.getShape()[1] == 3;
    assert a.getShape()[2] == 2;

    IndexIterator iter = a.getIndexIterator();
    assert iter.getByteNext() == 0;
    assert iter.getByteNext() == 2;
    assert iter.getByteNext() == 4;
    assert iter.getByteNext() == 6;
    assert iter.getByteNext() == 8;
    assert iter.getByteNext() == 10;
    assert !iter.hasNext();

    System.out.println("TestDODSMultiArrayPrimitiveTypes TestStride ok");
  }
}
