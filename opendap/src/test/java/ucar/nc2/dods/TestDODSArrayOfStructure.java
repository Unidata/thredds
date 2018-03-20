/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.dods;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.ma2.*;
import ucar.nc2.Structure;
import ucar.nc2.Variable;
import ucar.nc2.iosp.hdf5.H5header;
import ucar.nc2.util.DebugFlagsImpl;

import java.io.IOException;
import java.lang.invoke.MethodHandles;

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
public class TestDODSArrayOfStructure  {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

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
    assert v.getDataType() == DataType.UBYTE;

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
    try (StructureDataIterator iter = struct.getStructureIterator()) {
      while (iter.hasNext()) {
        StructureData sd = iter.next();
      }
    }
  }

}
