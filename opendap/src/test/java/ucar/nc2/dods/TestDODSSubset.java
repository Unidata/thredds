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
import ucar.ma2.ArrayLong;
import ucar.ma2.ArrayShort;
import ucar.ma2.DataType;
import ucar.nc2.Variable;
import ucar.nc2.util.Misc;
import ucar.unidata.util.test.category.NeedsExternalResource;

/** Test nc2 dods in the JUnit framework. */
@Category(NeedsExternalResource.class)
public class TestDODSSubset {

  @Test
  public void testArraySubset() throws IOException {
    DODSNetcdfFile dodsfile = TestDODSRead.open("test.02?i32[1:10],f64[2:2:10]");

    Variable v = null;
    Array a = null;

    // int32
    assert(null != (v = dodsfile.findVariable("i32")));
    assert v.getFullName().equals("i32");
    assert v.getRank() == 1;
    assert v.getSize() == 10;
    assert v.getDataType() == DataType.INT;
    a = v.read();
    assert a.getRank() == 1;
    assert a.getSize() == 25;
    assert a.getElementType() == int.class;
    assert a instanceof ArrayInt.D1;

    ArrayInt.D1 ai = (ArrayInt.D1) a;
    for (int i=0; i<10; i++) {
      int val = ai.get(i);
      assert (val == i * 2048) : val +" != "+(i * 2048);
    }

    // uint16
    assert null == (v = dodsfile.findVariable("ui16"));
    assert null == (v = dodsfile.findVariable("ui32"));


    // double
    assert(null != (v = dodsfile.findVariable("f64")));
    assert v.getFullName().equals("f64");
    assert v.getRank() == 1;
    assert v.getSize() == 5;
    assert v.getDataType() == DataType.DOUBLE : v.getDataType();
    a = v.read();
    assert a.getRank() == 1;
    assert a.getSize() == 25;
    assert a.getElementType() == double.class;
    assert a instanceof ArrayDouble.D1;
    ArrayDouble.D1 ad = (ArrayDouble.D1) a;
    double[] tFloat64 = new double[] { 1.0, 0.9999500004166653, 0.9998000066665778,
      0.9995500337489875, 0.9992001066609779, 0.9987502603949663, 0.9982005399352042,
      0.9975510002532796, 0.9968017063026194, 0.9959527330119943, 0.9950041652780257,
      0.9939560979566968, 0.9928086358538663, 0.9915618937147881, 0.9902159962126371,
      0.9887710779360422, 0.9872272833756269, 0.9855847669095608, 0.9838436927881214,
      0.9820042351172703, 0.9800665778412416, 0.9780309147241483, 0.9758974493306055,
      0.9736663950053749, 0.9713379748520297 };

    for (int i=0; i<5; i++) {
      double val = ad.get(i);
      assert Misc.closeEnough(val, tFloat64[i], 1.0e-9);
    }

    dodsfile.close();
  }


  @Test
  public void testSubset() throws IOException {
    DODSNetcdfFile dodsfile = TestDODSRead.open("test.05?types.integers");

    Variable v = null;
    Array a = null;

    // byte
    assert null != (v = dodsfile.findVariable("types.integers.b"));
    CheckByte( v);

    // int16
    assert null != (v = dodsfile.findVariable("types.integers.i16"));
    CheckInt16( v);

    // int32
    assert null != (v = dodsfile.findVariable("types.integers.i32"));
    CheckInt32( v);

    // uint32
    assert null != (v = dodsfile.findVariable("types.integers.ui32"));
    CheckUint32( v);

    // uint16
    assert null != (v = dodsfile.findVariable("types.integers.ui16"));
    CheckUint16( v);

    // uint32
    assert null != (v = dodsfile.findVariable("types.integers.ui32"));
    CheckUint32( v);

    dodsfile.close();
  }


  void CheckByte( Variable v) throws IOException {
    assert v.getRank() == 0;
    assert v.getSize() == 1;
    assert v.getDataType() == DataType.BYTE;
    CheckByteValue(v.read());
  }

  void CheckByteValue( Array a) {
    assert a.getRank() == 0;
    assert a.getSize() == 1;
    assert a.getElementType() == byte.class;
    assert a instanceof ArrayByte.D0;
    byte valb = ((ArrayByte.D0)a).get();
    assert (valb == 0);
  }

  void CheckInt16( Variable v) throws IOException {
    // int16
    //assert(null != (v = dodsfile.findVariable("types.integers.i16")));
    //assert v.getName().equals("types.integers.i16");
    assert v.getRank() == 0;
    assert v.getSize() == 1;
    assert v.getDataType() == DataType.SHORT;
    CheckInt16Value(v.read());
  }

  void CheckInt16Value( Array a) {
    assert a.getRank() == 0;
    assert a.getSize() == 1;
    assert a.getElementType() == short.class;
    assert a instanceof ArrayShort.D0;
    short vals = ((ArrayShort.D0)a).get();
    assert (vals == 0);
  }

  void CheckInt32( Variable v) throws IOException {

    // int32
    //assert(null != (v = dodsfile.findVariable("types.integers.i32")));
   // assert v.getName().equals("types.integers.i32");
    assert v.getRank() == 0;
    assert v.getSize() == 1;
    assert v.getDataType() == DataType.INT;
    CheckInt32Value(v.read());
  }

  void CheckInt32Value( Array a) {
    assert a.getRank() == 0;
    assert a.getSize() == 1;
    assert a.getElementType() == int.class;
    assert a instanceof ArrayInt.D0;
    int vali = ((ArrayInt.D0)a).get();
    assert (vali == 1) : vali;
  }

  void CheckUint32( Variable v) throws IOException {

    // uint32
    //assert(null != (v = dodsfile.findVariable("types.integers.ui32")));
    //assert v.getName().equals("types.integers.ui32");
    assert v.getRank() == 0;
    assert v.getSize() == 1;
    assert v.getDataType() == DataType.INT : v.getDataType();
    CheckUint32Value(v.read());
  }

  void CheckUint32Value( Array a) {
    assert a.getRank() == 0;
    assert a.getSize() == 1;
    assert a.getElementType() == int.class;
    assert a instanceof ArrayInt.D0;
    long vall = ((ArrayInt.D0)a).get();
    assert (vall == 0);
  }

  void CheckUint16( Variable v) throws IOException {

    // uint16
    //assert(null != (v = dodsfile.findVariable("types.integers.ui16")));
    //assert v.getName().equals("types.integers.ui16");
    assert v.getRank() == 0;
    assert v.getSize() == 1;
    assert v.getDataType() == DataType.SHORT : v.getDataType();
     CheckUInt16Value(v.read());
  }

  void CheckUInt16Value( Array a) {
    assert a.getRank() == 0;
    assert a.getSize() == 1;
    assert a.getElementType() == short.class;
    assert a instanceof ArrayShort.D0;
    int vali = ((ArrayShort.D0)a).get();
    assert (vali == 0);
  }

  void CheckLong32( Variable v) throws IOException {

    // uint32
    //assert(null != (v = dodsfile.findVariable("types.integers.ui32")));
    //assert v.getName().equals("types.integers.ui32");
    assert v.getRank() == 0;
    assert v.getSize() == 1;
    assert v.getDataType() == DataType.LONG : v.getDataType();
    CheckLongValue(v.read());
  }

  void CheckLongValue( Array a) {
    assert a.getRank() == 0;
    assert a.getSize() == 1;
    assert a.getElementType() == long.class;
    assert a instanceof ArrayLong.D0;
    long vall = ((ArrayLong.D0)a).get();
    assert (vall == 0);
  }

  void CheckF( Variable v) throws IOException {

    // float
    //assert(null != (v = dodsfile.findVariable("types.floats.f32")));
    //assert v.getName().equals("types.floats.f32");
    assert v.getRank() == 0;
    assert v.getSize() == 1;
    assert v.getDataType() == DataType.FLOAT : v.getDataType();
    CheckFValue(v.read());
  }

  void CheckFValue( Array a) {
    assert a.getRank() == 0;
    assert a.getSize() == 1;
    assert a.getElementType() == float.class;
    assert a instanceof ArrayFloat.D0;
    float valf = ((ArrayFloat.D0)a).get();
    assert (valf == 0.0);
  }

  void CheckD( Variable v) throws IOException {

    // double
    //assert(null != (v = dodsfile.findVariable("types.floats.f64")));
    //assert v.getName().equals("types.floats.f64");
    assert v.getRank() == 0;
    assert v.getSize() == 1;
    assert v.getDataType() == DataType.DOUBLE : v.getDataType();
    CheckDValue(v.read());
  }

  void CheckDValue( Array a) {
    assert a.getRank() == 0;
    assert a.getSize() == 1;
    assert a.getElementType() == double.class;
    assert a instanceof ArrayDouble.D0;
    double vald = ((ArrayDouble.D0)a).get();
    assert (vald == 1000.0);
  }

  void CheckS( Variable v) throws IOException {

    // string
    //assert(null != (v = dodsfile.findVariable("types.strings.s")));
    //assert v.getName().equals("types.strings.s");
    assert v.getRank() == 0;
    assert v.getDataType() == DataType.STRING : v.getDataType();
     CheckSValue(v.read());
  }

  void CheckSValue( Array a) {
    assert a.getRank() == 0;
    assert a.getElementType() == String.class;
    String str = (String) a.getObject(a.getIndex());
    assert str.equals("This is a data test string (pass 0).");
  }

  void CheckUrl( Variable v) throws IOException {

    // url
    //assert(null != (v = dodsfile.findVariable("types.strings.u")));
    //assert v.getName().equals("types.strings.u");
    assert v.getRank() == 0;
    assert v.getDataType() == DataType.STRING : v.getDataType();
    String str = v.readScalarString();
    assert str.equals("http://www.opendap.org") || str.equals("http://www.dods.org") : str;
  }


}
