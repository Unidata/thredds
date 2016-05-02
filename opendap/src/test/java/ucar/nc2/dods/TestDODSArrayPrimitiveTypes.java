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

import static org.junit.Assert.assertEquals;

/** Test nc2 dods in the JUnit framework. */
@Category(NeedsExternalResource.class)
public class TestDODSArrayPrimitiveTypes {

  @Test
  public void testArrays() throws IOException {
    DODSNetcdfFile dodsfile = TestDODSRead.open("test.02");

    Variable v = null;
    Array a = null;

    // byte
    assert(null != (v = dodsfile.findVariable("b")));
    assert v.getShortName().equals("b");
    assert v.getRank() == 1;
    assert v.getSize() == 25;
    assert v.getDataType() == DataType.BYTE;
    a = v.read();
    assert a.getRank() == 1;
    assert a.getSize() == 25;
    assert a.getElementType() == byte.class;
    assert a instanceof ArrayByte.D1;
    ArrayByte.D1 ab = (ArrayByte.D1) a;
    for (int i=0; i<25; i++) {
      byte valb = ab.get(i);
      assert (valb == i);
    }

    // int16
    assert(null != (v = dodsfile.findVariable("i16")));
    assert v.getShortName().equals("i16");
    assert v.getRank() == 1;
    assert v.getSize() == 25;
    assert v.getDataType() == DataType.SHORT;
    a = v.read();
    assert a.getRank() == 1;
    assert a.getSize() == 25;
    assert a.getElementType() == short.class;
    assert a instanceof ArrayShort.D1;
    ArrayShort.D1 as = (ArrayShort.D1) a;
    for (int i=0; i<25; i++) {
      short vals = as.get(i);
      assert (vals == i * 256);
    }

    // int32
    assert(null != (v = dodsfile.findVariable("i32")));
    assert v.getShortName().equals("i32");
    assert v.getRank() == 1;
    assert v.getSize() == 25;
    assert v.getDataType() == DataType.INT;
    a = v.read();
    assert a.getRank() == 1;
    assert a.getSize() == 25;
    assert a.getElementType() == int.class;
    assert a instanceof ArrayInt.D1;
    ArrayInt.D1 ai = (ArrayInt.D1) a;
    for (int i=0; i<25; i++) {
      int val = ai.get(i);
      assert (val == i * 2048);
    }

    // uint16
    assert(null != (v = dodsfile.findVariable("ui16")));
    assert v.getShortName().equals("ui16");
    assert v.getRank() == 1;
    assert v.getSize() == 25;
    assert v.getDataType() == DataType.SHORT;
    assert v.isUnsigned();

    a = v.read();
    assert a.getRank() == 1;
    assert a.getSize() == 25;
    assert a.getElementType() == short.class;
    assert a instanceof ArrayShort.D1;
    as = (ArrayShort.D1) a;
    for (int i=0; i<25; i++) {
      short val = as.get(i);
      assert (val == i * 1024) : val;
    }

    // uint32
    assert(null != (v = dodsfile.findVariable("ui32")));
    assert v.getShortName().equals("ui32");
    assert v.getRank() == 1;
    assert v.getSize() == 25;
    assert v.getDataType() == DataType.INT : v.getDataType();
    assert v.isUnsigned();
    a = v.read();
    assert a.getRank() == 1;
    assert a.getSize() == 25;
    assert a.getElementType() == int.class;
    assert a instanceof ArrayInt.D1;
    ai = (ArrayInt.D1) a;
    for (int i=0; i<25; i++) {
      int val = ai.get(i);
      assert (val == i * 4096);
    }

    // float
    assert(null != (v = dodsfile.findVariable("f32")));
    assert v.getShortName().equals("f32");
    assert v.getRank() == 1;
    assert v.getSize() == 25;
    assert v.getDataType() == DataType.FLOAT : v.getDataType();
    a = v.read();
    assert a.getRank() == 1;
    assert a.getSize() == 25;
    assert a.getElementType() == float.class;
    assert a instanceof ArrayFloat.D1;
    ArrayFloat.D1 af = (ArrayFloat.D1) a;

    float[] tFloat32 = {0.0f, 0.009999833f, 0.019998666f, 0.029995501f,
      0.039989334f, 0.04997917f, 0.059964005f, 0.06994285f, 0.0799147f, 0.08987855f,
      0.099833414f, 0.1097783f, 0.119712204f, 0.12963414f, 0.13954312f, 0.14943813f,
      0.15931821f, 0.16918235f, 0.17902957f, 0.1888589f, 0.19866933f, 0.2084599f,
      0.21822962f, 0.22797753f, 0.23770262f };

    for (int i=0; i<25; i++) {
      float val = af.get(i);
      assertEquals(val, tFloat32[i], 1.0e-5);
    }

    // double
    assert(null != (v = dodsfile.findVariable("f64")));
    assert v.getShortName().equals("f64");
    assert v.getRank() == 1;
    assert v.getSize() == 25;
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

    for (int i=0; i<25; i++) {
      double val = ad.get(i);
      assertEquals(val, tFloat64[i], 1.0e-9);
    }

    // string
    assert(null != (v = dodsfile.findVariable("s")));
    assert v.getShortName().equals("s");
    assert v.getRank() == 1;
    assert v.getDataType() == DataType.STRING : v.getDataType();
    a = v.read();
    assert a.getRank() == 1;
    assert a.getSize() == 25;
    assert a.getShape()[0] == 25;
    assert a.getElementType() == String.class;
    assert a instanceof ArrayObject.D1;
    ArrayObject.D1 ao = (ArrayObject.D1) a;
    for (int i=0; i<25; i++) {
      String str = (String) ao.get(i);
      assert str.equals("This is a data test string (pass "+i+").") : str;
    }

    // url
    assert(null != (v = dodsfile.findVariable("u")));
    assert v.getShortName().equals("u");
    assert v.getRank() == 1;
    assert v.getDataType() == DataType.STRING : v.getDataType();
    a = v.read();
    assert a.getRank() == 1;
    assert a.getElementType() == String.class;
    assert a instanceof ArrayObject.D1;
    ArrayObject.D1 ao2 = (ArrayObject.D1) a;
    for (int i=0; i<25; i++) {
      String str = (String) ao2.get(i);
      assert str.equals("http://www.opendap.org") || str.equals("http://www.dods.org") : str;
    }
  }

  @Test
  public void testStrides() throws IOException {
    DODSNetcdfFile dodsfile = TestDODSRead.open("test.02");

    DODSVariable v = null;
    Array a = null;

    // byte
    assert(null != (v = (DODSVariable) dodsfile.findVariable("b")));
    assert v.getShortName().equals("b");
    assert v.getRank() == 1;
    assert v.getSize() == 25;
    assert v.getDataType() == DataType.BYTE;

    try {
      a = v.read( "0:24:3" );
    } catch (InvalidRangeException e) {
        assert false;
    }

    assert a.getRank() == 1;
    assert a.getSize() == 9 : a.getSize();
    assert a.getElementType() == byte.class;
    assert a instanceof ArrayByte.D1;
    ArrayByte.D1 ab = (ArrayByte.D1) a;
    for (int i = 0; i < 8; i++) {
      byte valb = ab.get(i);
      assert(valb == 3*i);
    }

    //System.out.println("TestDODSArrayPrimitiveTypes TestStride ok");
  }




}
