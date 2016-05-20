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

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import ucar.ma2.Array;
import ucar.ma2.ArrayByte;
import ucar.ma2.ArrayInt;
import ucar.ma2.ArrayObject;
import ucar.ma2.DataType;
import ucar.ma2.Index;
import ucar.ma2.IndexIterator;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.StructureData;
import ucar.ma2.StructureDataIterator;
import ucar.nc2.Structure;
import ucar.nc2.Variable;
import ucar.nc2.iosp.hdf5.H5header;
import ucar.nc2.util.DebugFlagsImpl;
import ucar.unidata.util.test.category.NeedsExternalResource;

/**
 * Test nc2 dods in the JUnit framework.
 * Dataset {
 * Structure {
 * Byte b;
 * Int32 i32;
 * UInt32 ui32;
 * Int16 i16;
 * UInt16 ui16;
 * Float32 f32;
 * Float64 f64;
 * String s;
 * Url u;
 * } types[10];
 * } ArrayOfStructures;
 */
@Category(NeedsExternalResource.class)
public class TestDODSArrayOfStructure  {
  private DODSNetcdfFile dodsfile;

  @After
  public void after() throws IOException {
    dodsfile.close();
    H5header.setDebugFlags(new DebugFlagsImpl(""));  // make sure debug flags are off
  }

  @Before
  public void setUp() throws Exception {
    DODSNetcdfFile.setPreload(false);
    dodsfile = TestDODSRead.open("test.50");
    DODSNetcdfFile.setPreload(true);
    DODSNetcdfFile.setDebugFlags(new DebugFlagsImpl("DODS/serverCall"));
  }

  @Test
  public void testScalarReadByte() throws IOException {
    Variable v = null;
    Array a = null;

    assert null != (v = dodsfile.findVariable("types"));
    v.setCaching(false);
    assert v instanceof Structure;
    Structure s = (Structure) v;

    assert (null != (v = s.findVariable("b")));
    assert v.getShortName().equals("b");
    assert v.getRank() == 0;
    assert v.getSize() == 1;
    assert v.getDataType() == DataType.BYTE;

    // note this reads all 10 bytes
    a = v.read();
    assert a.getRank() == 1;
    assert a.getSize() == 10;
    assert a.getElementType() == byte.class;
    assert a instanceof ArrayByte.D1;
    byte valb = ((ArrayByte.D1) a).get(0);
    assert (valb == 0);
  }

  @Test
  public void testReadScalarMemberVariable() throws IOException {

    Variable v = dodsfile.findVariable("types.i32");
    assert v != null;
    assert v.getRank() == 0;
    assert v.getDataType() == DataType.INT : v.getDataType();

    Array a = v.read();
    Index ima = a.getIndex();
    assert a.getRank() == 1;
    assert a.getInt(ima) == 1 : a.getInt(ima);
    assert a instanceof ArrayInt.D1;

    v = dodsfile.findVariable("types.s");
    assert v != null;
    assert v.getRank() == 0;
    assert v.getDataType() == DataType.STRING : v.getDataType();

    a = v.read();
    assert a.getRank() == 1;
    assert a instanceof ArrayObject.D1;

    ArrayObject.D1 a0 = (ArrayObject.D1) a;
    assert a0.get(0).equals("This is a data test string (pass 0).");
  }

  @Test
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

  @Test
  public void testRead1DArrayOfStructs() throws IOException, InvalidRangeException {
    Variable v = dodsfile.findVariable("types");
    assert v != null;
    assert v instanceof DODSStructure;
    assert v.getRank() == 1;
    assert v.getDataType() == DataType.STRUCTURE;

    DODSStructure struct = (DODSStructure) v;
    for (int i = 0; i < struct.getSize(); i++) {
      StructureData sd = struct.readStructure(i);

      assert sd.getScalarByte("b") == 0 : sd.getScalarByte("b");
      assert sd.getScalarString("s").equals("This is a data test string (pass " + 0 + ").");
    }
  }

  @Test
  public void testReadIteratorArrayOfStructs() throws IOException, InvalidRangeException {
    Variable v = dodsfile.findVariable("types");
    assert v != null;
    assert v instanceof DODSStructure;
    assert v.getRank() == 1;
    assert v.getDataType() == DataType.STRUCTURE;

    DODSStructure struct = (DODSStructure) v;
    StructureDataIterator iter = struct.getStructureIterator();
    try {
      while (iter.hasNext()) {
        StructureData sd = iter.next();
      }
    } finally {
      iter.finish();
    }
  }

}
