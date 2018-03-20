/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.dods;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.ma2.*;
import ucar.nc2.Variable;

import java.io.IOException;
import java.lang.invoke.MethodHandles;

/** Test nc2 dods in the JUnit framework. */
public class TestDODSScalars {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

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
    assert v.getDataType() == DataType.UBYTE;
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
    assert v.getDataType() == DataType.UINT : v.getDataType();

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
    assert v.getDataType() == DataType.USHORT : v.getDataType();
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
