package ucar.nc2.dods;

import junit.framework.*;
import ucar.ma2.*;
import ucar.nc2.*;
import ucar.nc2.dataset.NetcdfDataset;

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
    } types;
} SimpleStructure;*/

public class TestDODSStructureScalars extends TestCase {

  public TestDODSStructureScalars( String name) {
    super(name);
  }

  public void testStructureRead() throws IOException {
    DODSNetcdfFile dodsfile = TestDODSRead.open("test.04");

    Variable v = null;
    Array a = null;

    assert null != (v = dodsfile.findVariable("types"));
    assert v instanceof Structure;
    Structure s = (Structure) v;
    StructureData sdata = s.readStructure();
    checkSD( sdata);
  }

  public void testRead() throws IOException {
    DODSNetcdfFile dodsfile = TestDODSRead.open("test.04");

    Variable v = null;
    Array a = null;

    // byte
    assert null != (v = dodsfile.findVariable("types"));
    assert v instanceof Structure;
    Structure s = (Structure) v;
    Array sdata = s.read();
    checkSD( (StructureData) sdata.getObject( sdata.getIndex()));
  }

  private void checkSD( StructureData s) {
    Variable v = null;
    Array a = null;

    assert(null != (a = s.findMemberArray("b")));
    assert a.getRank() == 0;
    assert a.getSize() == 1;
    assert a.getElementType() == byte.class;
    assert a instanceof ArrayByte.D0;
    byte valb = ((ArrayByte.D0)a).get();
    assert (valb == 0);

    // int16
    assert(null != (a = s.findMemberArray("i16")));
    assert a.getRank() == 0;
    assert a.getSize() == 1;
    assert a.getElementType() == short.class;
    assert a instanceof ArrayShort.D0;
    short vals = ((ArrayShort.D0)a).get();
    assert (vals == 0);

    // int32
    assert(null != (a = s.findMemberArray("i32")));
    assert a.getRank() == 0;
    assert a.getSize() == 1;
    assert a.getElementType() == int.class;
    assert a instanceof ArrayInt.D0;
    int vali = ((ArrayInt.D0)a).get();
    assert (vali == 1) : vali;

    // uint32
    assert(null != (a = s.findMemberArray("ui32")));
    assert a.getRank() == 0;
    assert a.getSize() == 1;
    assert a.getElementType() == int.class;
    assert a instanceof ArrayInt.D0;
    long vall = ((ArrayInt.D0)a).get();
    assert (vall == 0);

    // uint16
    assert(null != (a = s.findMemberArray("ui16")));
    assert a.getRank() == 0;
    assert a.getSize() == 1;
    assert a.getElementType() == short.class;
    assert a instanceof ArrayShort.D0;
    vali = ((ArrayShort.D0)a).get();
    assert (vali == 0);

    // float
    assert(null != (a = s.findMemberArray("f32")));
    assert a.getRank() == 0;
    assert a.getSize() == 1;
    assert a.getElementType() == float.class;
    assert a instanceof ArrayFloat.D0;
    float valf = ((ArrayFloat.D0)a).get();
    assert (valf == 0.0);

    // double
    assert(null != (a = s.findMemberArray("f64")));
    assert a.getRank() == 0;
    assert a.getSize() == 1;
    assert a.getElementType() == double.class;
    assert a instanceof ArrayDouble.D0;
    double vald = ((ArrayDouble.D0)a).get();
    assert (vald == 1000.0);

    // string
    assert(null != (a = s.findMemberArray("s")));
    assert a.getRank() == 0;
    assert a.getElementType() == String.class;
    assert a instanceof ArrayObject.D0;
    String str = (String) a.getObject(a.getIndex());
    assert str.equals("This is a data test string (pass 0).");

    // url
    assert(null != (a = s.findMemberArray("u")));
    assert a.getRank() == 0;
    assert a.getElementType() == String.class;
    assert a instanceof ArrayObject.D0;
    str = (String) a.getObject(a.getIndex());
    assert str.equals("http://www.opendap.org") || str.equals("http://www.dods.org") : str;

  }

  public void testScalarRead() throws IOException {
    DODSNetcdfFile dodsfile = TestDODSRead.open("test.04");

    Variable v = null;
    assert null != (v = dodsfile.findVariable("types"));
    assert v instanceof Structure;
    scalarRead((Structure) v);
  }

  public void testScalarReadUncached() throws IOException {
    DODSNetcdfFile.setPreload( false);
    DODSNetcdfFile dodsfile = TestDODSRead.open("test.04");
    DODSNetcdfFile.setPreload( true);

    Variable v = null;
    assert null != (v = dodsfile.findVariable("types"));
    assert v instanceof Structure;
    v.setCaching(false);
    scalarRead((Structure) v);
  }

   private void scalarRead(Structure s) throws IOException {

    Variable v = null;
    Array a = null;

    // byte
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

    // int16
    assert(null != (v = s.findVariable("i16")));
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
    assert(null != (v = s.findVariable("i32")));
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
    assert(null != (v = s.findVariable("ui32")));
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
    assert(null != (v = s.findVariable("ui16")));
    assert v.getShortName().equals("ui16");
    assert v.getRank() == 0;
    assert v.getSize() == 1;
    assert v.getDataType() == DataType.SHORT : v.getDataType();
    a = v.read();
    assert a.getRank() == 0;
    assert a.getSize() == 1;
    assert a.getElementType() == short.class;
    assert a instanceof ArrayShort.D0;
    vali = ((ArrayShort.D0)a).get();
    assert (vali == 0);

    // uint32
    assert(null != (v = s.findVariable("ui32")));
    assert v.getShortName().equals("ui32");
    assert v.getRank() == 0;
    assert v.getSize() == 1;
    assert v.getDataType() == DataType.INT : v.getDataType();
    a = v.read();
    assert a.getRank() == 0;
    assert a.getSize() == 1;
    assert a.getElementType() == int.class;
    assert a instanceof ArrayInt.D0;
    vall = ((ArrayInt.D0)a).get();
    assert (vall == 0);

    // float
    assert(null != (v = s.findVariable("f32")));
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
    assert(null != (v = s.findVariable("f64")));
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

    // string
    assert(null != (v = s.findVariable("s")));
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
    assert(null != (v = s.findVariable("u")));
    assert v.getShortName().equals("u");
    assert v.getRank() == 0;
    assert v.getDataType() == DataType.STRING : v.getDataType();
    str = v.readScalarString();
    assert str.equals("http://www.opendap.org") || str.equals("http://www.dods.org") : str;
  }

  public void testDODSwithDataset() throws IOException {
    DODSNetcdfFile dodsfile = TestDODSRead.open("test.04");
    NetcdfDataset ds = new  NetcdfDataset( dodsfile, false);

    // bug in forming dods name
    Variable v = null;
    assert null != (v = ds.findVariable("types.b"));
    v.invalidateCache();
    v.read();
  }



}
