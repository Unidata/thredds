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

import junit.framework.*;
import ucar.ma2.*;
import ucar.nc2.*;

import java.io.*;

/** Test nc2 dods in the JUnit framework.
 * Dataset {
    Structure {
        Byte b;
        Int32 i32;
        UInt32 ui32;
        Int16 i16;
        UInt16 ui16;
        Float32 f32;
        Float64 f64;
        String s;
        Url u;
    } types[10];
} ArrayOfStructures;
 */

public class TestDODSArrayOfStructure extends TestCase {
  private boolean debug = false;

  public TestDODSArrayOfStructure( String name) {
    super(name);
  }

  private DODSNetcdfFile dodsfile;
  protected void setUp() throws Exception {
    DODSNetcdfFile.setPreload(false);
    dodsfile = TestDODSRead.open("test.50");
    DODSNetcdfFile.setPreload(true);
  }
  protected void tearDown() throws Exception {
    dodsfile.close();
  }

  public void testScalarReadByte() throws IOException {
     Variable v = null;
     Array a = null;

     assert null != (v = dodsfile.findVariable("types"));
     v.setCaching(false);
     assert v instanceof Structure;
     Structure s = (Structure) v;

     assert(null != (v = s.findVariable("b")));
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
   }

  public void testReadScalarMemberVariable() throws IOException {

    Variable v = dodsfile.findVariable("types.i32");
    assert v != null;
    assert v.getRank() == 0;
    assert v.getDataType() == DataType.INT : v.getDataType();

    Array a = v.read();
    Index ima = a.getIndex();
    assert a.getRank() == 0;
    assert a.getInt( ima) == 1;
    assert a instanceof ArrayInt.D0;

    v = dodsfile.findVariable("types.s");
    assert v != null;
    assert v.getRank() == 0;
    assert v.getDataType() == DataType.STRING : v.getDataType();

    a = v.read();
    assert a.getRank() == 0;
    assert a instanceof ArrayObject.D0;

    ArrayObject.D0 a0 = (ArrayObject.D0) a;
    assert a0.get().equals("This is a data test string (pass 0).");
  }

  public void testReadArrayOfStructs() throws IOException, InvalidRangeException {
    Variable v = dodsfile.findVariable("types");
    assert v != null;
    assert v instanceof DODSStructure;
    assert v.getRank() == 1;
    assert v.getDataType() == DataType.STRUCTURE;

    DODSStructure struct = (DODSStructure) v;
    Array data = struct.read();
    assert data.getRank() == 1;
    assert data.getElementType().equals(StructureData.class);

    IndexIterator ii = data.getIndexIterator();
    while (ii.hasNext()) {
      Object d = ii.next();
      assert d instanceof StructureData : d.getClass().getName();
    }
  }

  public void testRead1DArrayOfStructs() throws IOException, InvalidRangeException {
    Variable v = dodsfile.findVariable("types");
    assert v != null;
    assert v instanceof DODSStructure;
    assert v.getRank() == 1;
    assert v.getDataType() == DataType.STRUCTURE;

    DODSStructure struct = (DODSStructure) v;
    for (int i=0; i<struct.getSize(); i++) {
      StructureData sd = struct.readStructure(i);

      assert sd.getScalarByte("b") == 0 : sd.getScalarByte("b");
      assert sd.getScalarString("s").equals("This is a data test string (pass "+0+").");
    }
  }

  public void testReadIteratorArrayOfStructs() throws IOException, InvalidRangeException {
    Variable v = dodsfile.findVariable("types");
    assert v != null;
    assert v instanceof DODSStructure;
    assert v.getRank() == 1;
    assert v.getDataType() == DataType.STRUCTURE;

    DODSStructure struct = (DODSStructure) v;
    StructureDataIterator iter = struct.getStructureIterator();
    while (iter.hasNext()) {
      StructureData sd = iter.next();

    }
  }

  public void testMemberReadNoFlatten() throws IOException, InvalidRangeException {
     Array a = dodsfile.read("types(1).b", false);

     assert a.getRank() == 1;
     assert a.getSize() == 1;
     assert a.getElementType() == StructureData.class : a.getElementType();
     assert a instanceof ArrayStructure;

     StructureData sd = (StructureData) a.getObject( a.getIndex());
     StructureMembers.Member m = sd.findMember("b");
     assert m != null;
     Array data = sd.getArray(m);
     assert data != null;
     assert data.getRank() == 0;
     assert data.getSize() == 1;
     assert data.getElementType() == byte.class;
     assert data instanceof ArrayByte.D0;

     ArrayByte.D0 b = (ArrayByte.D0) data;
     assert (b.get() == 0);
   }

  public void testArrayReadNoFlatten() throws IOException, InvalidRangeException {
      Array a = dodsfile.read("types(1:7).b", false);

      assert a.getRank() == 1;
      assert a.getSize() == 7;
      assert a.getElementType() == StructureData.class : a.getElementType();
      assert a instanceof ArrayStructure;

      StructureData sd = (StructureData) a.getObject( a.getIndex());
      StructureMembers.Member m = sd.findMember("b");
      assert m != null;
      Array data = sd.getArray(m);
      assert data != null;
      assert data.getRank() == 0;
      assert data.getSize() == 1;
      assert data.getElementType() == byte.class;
      assert data instanceof ArrayByte.D0;

      ArrayByte.D0 b = (ArrayByte.D0) data;
      assert (b.get() == 0) : b.get();
    }

  public void testArrayReadFlatten() throws IOException, InvalidRangeException {
      Array a = dodsfile.read("types(1:7).b", true);

      assert a.getRank() == 1;
      assert a.getSize() == 7;
      assert a.getElementType() == byte.class;
      assert a instanceof ArrayByte.D1;

      ArrayByte.D1 b1 = (ArrayByte.D1) a;
      assert (b1.get(0) == 0);
      assert (b1.get(6) == 6);
    }



  /* void checkS1( Variable v) throws IOException {

    // string
    assert v.getRank() == 1;
    assert v.getDataType() == DataType.STRING : v.getDataType();

    Array a = v.read();
    assert a.getRank() == 1;
    assert a.getElementType() == String.class;
    assert a instanceof ArrayObject.D2;

    int[] shape = a.getShape();
    for (int i=0; i<shape[0]; i++) {
      String str = ((ArrayChar)a).getString(i);
      assert str.equals("This is a data test string (pass "+i+").");
    }
  } */
}
