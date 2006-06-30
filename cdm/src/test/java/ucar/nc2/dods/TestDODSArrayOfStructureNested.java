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
        Structure {
            UInt16 ui16[10];
            Float32 f32;
            Float64 f64;
            String s;
            Url u[5];
        } ss;
    } types[10];
} WackyArray02;
 */

public class TestDODSArrayOfStructureNested extends TestCase {
  private boolean debug = false;

  public TestDODSArrayOfStructureNested( String name) {
    super(name);
  }

  private DODSNetcdfFile dodsfile;
  protected void setUp() throws Exception {
    DODSNetcdfFile.setPreload(false);
    dodsfile = TestDODSRead.open("test.53");
    DODSNetcdfFile.setPreload(true);
  }
  protected void tearDown() throws Exception {
    dodsfile.close();
  }

  public void testScalarReadByte() throws IOException {

    Variable v = null;
    Array a = null;

    // byte
    assert null != (v = dodsfile.findVariable("types"));
    assert v instanceof Structure;
    Structure s = (Structure) v;

    assert null != (v = s.findVariable("ss"));
    assert v instanceof Structure;
    Structure ss = (Structure) v;

    assert(null != (v = ss.findVariable("f32")));
    assert v.getShortName().equals("f32");
    assert v.getRank() == 0;
    assert v.getSize() == 1;
    assert v.getDataType() == DataType.FLOAT;
    a = v.read();
    assert a.getRank() == 0;
    assert a.getSize() == 1;
    assert a.getElementType() == float.class;
    assert a instanceof ArrayFloat.D0;
    float val = a.getFloat( a.getIndex());
    assert (val == 0.0);
  }

  public void testReadScalarMemberVariable() throws IOException {

    Variable v = dodsfile.findVariable("types.ss.ui16");
    assert v != null;
    assert v.getRank() == 1;
    assert v.getSize() == 10;
    assert v.getDataType() == DataType.SHORT : v.getDataType();
    assert v.isUnsigned();
    
    Array a = v.read();
    Index ima = a.getIndex();
    assert a.getRank() == 1;
    assert a.getSize() == 10;
    assert a.getInt( ima) == 0;

    v = dodsfile.findVariable("types.ss.s");
    assert v != null;
    assert v.getRank() == 0;
    assert v.getDataType() == DataType.STRING : v.getDataType();

    a = v.read();
    assert a.getRank() == 0;
    assert a instanceof ArrayObject.D0 : a.getClass().getName();

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

      StructureMembers.Member m = sd.findMember("ss");
      assert m != null;
      Array a = sd.getArray(m);
      assert a.getRank() == 0;
      assert a.getElementType() == StructureData.class;

      StructureData ss = (StructureData) a.getObject( a.getIndex());
      assert ss.getScalarString("s").equals("This is a data test string (pass "+0+").");
    }
  }

}
