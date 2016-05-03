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
import ucar.ma2.StructureData;
import ucar.ma2.StructureMembers;
import ucar.nc2.Structure;
import ucar.nc2.Variable;
import ucar.unidata.util.test.category.NeedsExternalResource;

/** Test nc2 dods in the JUnit framework.
 *
 * Dataset {
    Structure {
        Structure {
            Byte b;
            Int32 i32;
            UInt32 ui32;
            Int16 i16;
            UInt16 ui16;
        } integers;
        Structure {
            Float32 f32;
            Float64 f64;
        } floats;
        Structure {
            String s;
            Url u;
        } strings;
    } types;
} NestedStructure;
 */
@Category(NeedsExternalResource.class)
public class TestDODSStructureScalarsNested {

  private DODSNetcdfFile dodsfile;

  @org.junit.Test
  public void testScalarRead() throws IOException {
    dodsfile = TestDODSRead.open("test.05");

    Variable v = null;
    Structure types, s = null;
    Array a = null;

    assert(null != (v = dodsfile.findVariable("types")));
    assert v instanceof Structure;
    types = (Structure) v;
    assert(null != (v = types.findVariable("integers")));
    assert v instanceof Structure;
    s = (Structure) v;

    // byte
    assert(null != (v = s.findVariable("b")));
    CheckByte( v);

    // int16
    assert(null != (v = s.findVariable("i16")));
    CheckInt16( v);

    // int32
    assert(null != (v = s.findVariable("i32")));
    CheckInt32( v);

    // uint32
    assert(null != (v = s.findVariable("ui32")));
    CheckUInt32( v);

    // uint16
    assert(null != (v = s.findVariable("ui16")));
    CheckUint16( v);

    assert(null != (v = types.findVariable("floats")));
    assert v instanceof Structure;
    s = (Structure) v;

    // float
    assert(null != (v = s.findVariable("f32")));
    CheckF( v);

    // double
    assert(null != (v = s.findVariable("f64")));
    CheckD( v);

    assert(null != (v = types.findVariable("strings")));
    assert v instanceof Structure;
    s = (Structure) v;

    // string
    assert(null != (v = s.findVariable("s")));
    CheckS( v);

    // url
    assert(null != (v = s.findVariable("u")));
    CheckUrl( v);

  }

  @Test
  public void testStructureRead() throws IOException {
    dodsfile = TestDODSRead.open("test.05");

    Structure s;
    Variable v = null;
    Array a = null;

    DODSStructure types = null;
    StructureMembers.Member m;
    StructureData integers = null, floats = null, strings = null;

    assert(null != (v = dodsfile.findVariable("types")));
    System.out.println("types="+v);
    assert v instanceof DODSStructure;
    types = (DODSStructure) v;

    assert(null != (v = types.findVariable("integers")));
    assert v instanceof Structure;
    s = (Structure) v;
    StructureData sdata = s.readStructure();

    m = sdata.findMember("b");
    assert (null != m);
    CheckByteValue(sdata.getArray(m));

    m = sdata.findMember("i16");
    assert (null != m);
    CheckInt16Value(sdata.getArray(m));

    m = sdata.findMember("i32");
    assert (null != m);
    CheckInt32Value(sdata.getArray(m));

    m = sdata.findMember("ui16");
    assert (null != m);
    CheckUInt16Value(sdata.getArray(m));

    m = sdata.findMember("ui32");
    assert (null != m);
    CheckUInt32Value(sdata.getArray(m));

    assert(null != (v = types.findVariable("floats")));
    assert v instanceof Structure;
    s = (Structure) v;
    sdata = s.readStructure();

    m = sdata.findMember("f32");
    assert (null != m);
    CheckFValue(sdata.getArray(m));

    m = sdata.findMember("f64");
    assert (null != m);
    CheckDValue(sdata.getArray(m));

    assert(null != (v = types.findVariable("strings")));
    assert v instanceof Structure;
    s = (Structure) v;
    sdata = s.readStructure();

    m = sdata.findMember("s");
    assert (null != m);
    CheckSValue(sdata.getArray(m));
  }


  @Test
  public void testStructureRead2() throws IOException {
    dodsfile = TestDODSRead.open("test.05");

    Structure s;
    Variable v = null;
    Array a = null;

    DODSStructure types = null;
    StructureMembers.Member m = null;
    StructureData integers = null, floats = null, strings = null;

    assert(null != (v = dodsfile.findVariable("types")));
    System.out.println("types="+v);
    assert v instanceof DODSStructure;
    types = (DODSStructure) v;

    StructureData sdata = types.readStructure();

    assert(null != (m = sdata.findMember("integers")));
    Array arr = sdata.getArray(m);
    integers = (StructureData) arr.getObject(arr.getIndex());

    m = integers.findMember("b");
    assert (null != m);
    CheckByteValue(integers.getArray(m));

    m = integers.findMember("i16");
    assert (null != m);
    CheckInt16Value(integers.getArray(m));

    m = integers.findMember("i32");
    assert (null != m);
    CheckInt32Value(integers.getArray(m));

    m = integers.findMember("ui16");
    assert (null != m);
    CheckUInt16Value(integers.getArray(m));

    m = integers.findMember("ui32");
    assert (null != m);
    CheckUInt32Value(integers.getArray(m));

    assert(null != (m = sdata.findMember("floats")));
    arr = sdata.getArray(m);
    floats = (StructureData) arr.getObject(arr.getIndex());

    m = floats.findMember("f32");
    assert (null != m);
    CheckFValue(floats.getArray(m));

    m = floats.findMember("f64");
    assert (null != m);
    CheckDValue(floats.getArray(m));

    assert(null != (m = sdata.findMember("strings")));
    arr = sdata.getArray(m);
    strings = (StructureData) arr.getObject(arr.getIndex());

    m = strings.findMember("s");
    assert (null != m);
    CheckSValue(strings.getArray(m));
  }

  /** LOOK not dealing with nested structures yet ?? 
  public void testStructureRead3() throws IOException {
    dodsfile = TestDODSRead.open("test.05");

    StructureMembers.Member m = null;
    DODSStructure types = (DODSStructure) dodsfile.findVariable("types");
    StructureData sdata = types.readStructure();
    m = sdata.findNestedMember("integers.b");
    assert (null != m);
    CheckByteValue(sdata.getArray(m));

    m = sdata.findNestedMember("integers.i16");
    assert (null != m);
    CheckInt16Value(sdata.getArray(m));

    m = sdata.findNestedMember("integers.i32");
    assert (null != m);
    CheckInt32Value(sdata.getArray(m));

    m = sdata.findNestedMember("integers.ui16");
    assert (null != m);
    CheckUInt16Value(sdata.getArray(m));

    m = sdata.findNestedMember("integers.ui32");
    assert (null != m);
    CheckUInt32Value(sdata.getArray(m));

    m = sdata.findNestedMember("floats.f32");
    assert (null != m);
    CheckFValue(sdata.getArray(m));

    m = sdata.findNestedMember("floats.f64");
    assert (null != m);
    CheckDValue(sdata.getArray(m));

    m = sdata.findNestedMember("strings.s");
    assert (null != m);
    CheckSValue(sdata.getArray(m));
  } */

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

  void CheckUInt32( Variable v) throws IOException {

    // uint32
    //assert(null != (v = dodsfile.findVariable("types.integers.ui32")));
    //assert v.getName().equals("types.integers.ui32");
    assert v.getRank() == 0;
    assert v.getSize() == 1;
    assert v.getDataType() == DataType.INT : v.getDataType();
    CheckUInt32Value(v.read());
  }

  void CheckUInt32Value( Array a) {
    assert a.getRank() == 0;
    assert a.getSize() == 1;
    assert a.getElementType() == int.class;
    assert a instanceof ArrayInt.D0;
    int vall = ((ArrayInt.D0)a).get();
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
