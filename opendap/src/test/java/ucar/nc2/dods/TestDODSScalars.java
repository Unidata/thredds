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
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Variable;
import ucar.unidata.util.test.category.NeedsExternalResource;

/** Test nc2 dods in the JUnit framework. */
@Category(NeedsExternalResource.class)
public class TestDODSScalars {

  @Test
  public void testScalar() throws IOException {
    DODSNetcdfFile dodsfile = TestDODSRead.open("test.01");

    Variable v = null;
    Array a = null;

    // byte
    assert(null != (v = dodsfile.findVariable("b")));
    assert v.getShortName().equals("b");
    assert v.getRank() == 0;
    assert v.getSize() == 1;
    assert v.getDataType() == DataType.BYTE;
    a = v.read();
    assert a.getRank() == 0;
    assert a.getSize() == 1;
    assert a.getElementType() == byte.class;
    assert a instanceof ArrayByte.D0;
    byte valb = ((ArrayByte.D0)a).get();
    assert (valb == 0);

    // int16
    assert(null != (v = dodsfile.findVariable("i16")));
    assert v.getShortName().equals("i16");
    assert v.getRank() == 0;
    assert v.getSize() == 1;
    assert v.getDataType() == DataType.SHORT;
    a = v.read();
    assert a.getRank() == 0;
    assert a.getSize() == 1;
    assert a.getElementType() == short.class;
    assert a instanceof ArrayShort.D0;
    short vals = ((ArrayShort.D0)a).get();
    assert (vals == 0);

    // int32
    assert(null != (v = dodsfile.findVariable("i32")));
    assert v.getShortName().equals("i32");
    assert v.getRank() == 0;
    assert v.getSize() == 1;
    assert v.getDataType() == DataType.INT;
    a = v.read();
    assert a.getRank() == 0;
    assert a.getSize() == 1;
    assert a.getElementType() == int.class;
    assert a instanceof ArrayInt.D0;
    int vali = ((ArrayInt.D0)a).get();
    assert (vali == 1) : vali;

    // uint32
    assert(null != (v = dodsfile.findVariable("ui32")));
    assert v.getShortName().equals("ui32");
    assert v.getRank() == 0;
    assert v.getSize() == 1;
    assert v.getDataType() == DataType.INT : v.getDataType();
    assert v.isUnsigned();

    a = v.read();
    assert a.getRank() == 0;
    assert a.getSize() == 1;
    assert a.getElementType() == int.class;
    assert a instanceof ArrayInt.D0;
    int vall = ((ArrayInt.D0)a).get();
    assert (vall == 0);

    // uint16
    assert(null != (v = dodsfile.findVariable("ui16")));
    assert v.getShortName().equals("ui16");
    assert v.getRank() == 0;
    assert v.getSize() == 1;
    assert v.getDataType() == DataType.SHORT : v.getDataType();
    assert v.isUnsigned();
    a = v.read();
    assert a.getRank() == 0;
    assert a.getSize() == 1;
    assert a.getElementType() == short.class;
    assert a instanceof ArrayShort.D0;
    vali = ((ArrayShort.D0)a).get();
    assert (vali == 0);

    // float
    assert(null != (v = dodsfile.findVariable("f32")));
    assert v.getShortName().equals("f32");
    assert v.getRank() == 0;
    assert v.getSize() == 1;
    assert v.getDataType() == DataType.FLOAT : v.getDataType();
    a = v.read();
    assert a.getRank() == 0;
    assert a.getSize() == 1;
    assert a.getElementType() == float.class;
    assert a instanceof ArrayFloat.D0;
    float valf = ((ArrayFloat.D0)a).get();
    assert (valf == 0.0);

    // double
    assert(null != (v = dodsfile.findVariable("f64")));
    assert v.getShortName().equals("f64");
    assert v.getRank() == 0;
    assert v.getSize() == 1;
    assert v.getDataType() == DataType.DOUBLE : v.getDataType();
    a = v.read();
    assert a.getRank() == 0;
    assert a.getSize() == 1;
    assert a.getElementType() == double.class;
    assert a instanceof ArrayDouble.D0;
    double vald = ((ArrayDouble.D0)a).get();
    assert (vald == 1000.0);

    try {
      String s = v.readScalarString();
      assert false;
    } catch (Exception e) {
      assert e instanceof IllegalArgumentException : e.getClass().getName();
    }

    // string
    assert(null != (v = dodsfile.findVariable("s")));
    assert v.getShortName().equals("s");
    assert v.getRank() == 0;
    assert v.getDataType() == DataType.STRING : v.getDataType();
    a = v.read();
    assert a.getRank() == 0;
    assert a.getElementType() == String.class;
    assert a instanceof ArrayObject.D0;
    String str = (String) a.getObject(a.getIndex());
    assert str.equals("This is a data test string (pass 0).");

    // url
    assert(null != (v = dodsfile.findVariable("u")));
    assert v.getShortName().equals("u");
    assert v.getRank() == 0;
    assert v.getDataType() == DataType.STRING : v.getDataType();

    a = v.read();
    assert a.getRank() == 0;
    assert a.getElementType() == String.class;
    assert a instanceof ArrayObject.D0;
    ArrayObject.D0 a0 = (ArrayObject.D0) a;
    Object s = a0.get();
    assert s instanceof String;
    assert s.equals("http://www.opendap.org") || s.equals("http://www.dods.org") : s;

    String str2 = v.readScalarString();
    assert str2.equals("http://www.opendap.org") || s.equals("http://www.dods.org") : str2;

    try {
      double val = v.readScalarDouble();
      assert false;
    } catch (Exception e) {
      assert e instanceof ucar.ma2.ForbiddenConversionException : e.getClass().getName();
    }

  }

  @Test
  public void testStrings() throws IOException, InvalidRangeException {
    DODSNetcdfFile dodsfile = TestDODSRead.open("test.02");
    Variable v = null;

    // string
    assert(null != (v = dodsfile.findVariable("s")));
    assert v.getShortName().equals("s");
    assert v.getRank() == 1;
    assert v.getSize() == 25;
    assert v.getDataType() == DataType.STRING : v.getDataType();

    Array a = v.read("1:10");
    assert a.getRank() == 1;
    assert a.getSize() == 10 : a.getSize();
    assert a.getElementType() == String.class;
  }
}
