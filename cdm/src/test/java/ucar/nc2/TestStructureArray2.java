package ucar.nc2;

import junit.framework.*;
import ucar.ma2.*;
import ucar.nc2.dataset.NetcdfDataset;

import java.io.*;
import java.util.*;

/** Test reading record data */

public class TestStructureArray2 extends TestCase {

  public TestStructureArray2( String name) {
    super(name);
  }

  public void testBB() throws IOException, InvalidRangeException {
    NetcdfFile ncfile = TestNC2.openFile("testWriteRecord.nc");
    ncfile.sendIospMessage(NetcdfFile.IOSP_MESSAGE_ADD_RECORD_STRUCTURE);

    Structure v = (Structure) ncfile.findVariable("record");
    assert v != null;

    assert( v.getDataType() == DataType.STRUCTURE);

    Array data = v.read();
    assert( data instanceof ArrayStructure);
    assert( data instanceof ArrayStructureBB);
    assert(data.getElementType() == StructureData.class);

    testStructureArray( (ArrayStructure) data);

    Structure.Iterator si = v.getStructureIterator();
    while (si.hasNext())
      testStructureData(si.next());

    ncfile.close();
  }

  public void testMA() throws IOException, InvalidRangeException {
    NetcdfFile ncfile = TestNC2.openFile("jan.nc");
    Dimension dim = ncfile.findDimension("time");
    assert dim != null;

    Structure p = new StructurePseudo( ncfile, null, "Psuedo", dim);

    assert( p.getDataType() == DataType.STRUCTURE);

    Array data = p.read();
    assert( data instanceof ArrayStructure);
    assert( data instanceof ArrayStructureMA);
    assert(data.getElementType() == StructureData.class);

    testStructureArray( (ArrayStructure) data);

    Structure.Iterator si = p.getStructureIterator();
    while (si.hasNext())
      testStructureData(si.next());

    ncfile.close();
  }

  public void testDODS() throws IOException, InvalidRangeException {
   /* testW("http://dods.coas.oregonstate.edu:8080/dods/dts/test.04", "types", true);
    testW("http://dods.coas.oregonstate.edu:8080/dods/dts/test.21", "exp", true);
    testW("http://dods.coas.oregonstate.edu:8080/dods/dts/test.50", "types", false);
    testW("http://dods.coas.oregonstate.edu:8080/dods/dts/test.05", "types", true);   */
    testW("http://dods.coas.oregonstate.edu:8080/dods/dts/test.53", "types", false);
  }

  public void testW(String url, String sname, boolean isScalar) throws IOException, InvalidRangeException {
    NetcdfFile ncfile = NetcdfDataset.openFile(url, null);
    Structure v = (Structure) ncfile.findVariable(sname);
    assert v != null;

    assert( v.getDataType() == DataType.STRUCTURE);

    Array data = v.read();
    assert( data instanceof ArrayStructure);
    assert(data.getElementType() == StructureData.class);

    testStructureArray( (ArrayStructure) data);

    if (isScalar)
      testStructureData(v.readStructure());
    else {
      Structure.Iterator si = v.getStructureIterator();
      while (si.hasNext())
        testStructureData(si.next());

    }
    ncfile.close();
  }

  private void testStructureArray(ArrayStructure as ) {

    StructureMembers sms = as.getStructureMembers();
    List members = sms.getMembers();

    int n = (int) as.getSize();
    for (int recno=0; recno<n; recno++) {
      Object o = as.getObject( recno);
      assert (o instanceof StructureData);
      StructureData sdata = as.getStructureData( recno);
      assert (o == sdata);
      testStructureData( sdata);

      for (int i = 0; i < members.size(); i++) {
        StructureMembers.Member m = (StructureMembers.Member) members.get(i);

        Array sdataArray = sdata.getArray(m);
        assert (sdataArray.getElementType() == m.getDataType().getPrimitiveClassType());

        Array sdataArray2 = sdata.getArray(m.getName());
        ucar.ma2.TestMA2.testEquals( sdataArray, sdataArray2);

        Array a = as.getArray(recno, m);
        assert (a.getElementType() == m.getDataType().getPrimitiveClassType());
        ucar.ma2.TestMA2.testEquals( sdataArray, a);

        NCdump.printArray(a, m.getName(), System.out, null);

        testGetArrayByType(as, recno, m, a);
      }
    }
  }

  private void testGetArrayByType(ArrayStructure as, int recno, StructureMembers.Member m, Array a ) {
    DataType dtype = m.getDataType();
    Object data = null;
    if (dtype == DataType.DOUBLE) {
      assert  a.getElementType() == double.class;
      data = as.getJavaArrayDouble(recno, m);
    } else if (dtype == DataType.FLOAT) {
      assert  a.getElementType() == float.class;
      data = as.getJavaArrayFloat(recno, m);
    } else if (dtype == DataType.LONG) {
      assert  a.getElementType() == long.class;
      data = as.getJavaArrayLong(recno, m);
    } else if (dtype == DataType.INT) {
      assert  a.getElementType() == int.class;
      data = as.getJavaArrayInt(recno, m);
    } else if (dtype == DataType.SHORT) {
      assert  a.getElementType() == short.class;
      data = as.getJavaArrayShort(recno, m);
    } else if (dtype == DataType.BYTE) {
      assert  a.getElementType() == byte.class;
      data = as.getJavaArrayByte(recno, m);
    } else if (dtype == DataType.CHAR) {
      assert  a.getElementType() == char.class;
      data = as.getJavaArrayChar(recno, m);
    }

    if (data != null)
      ucar.ma2.TestMA2.testJarrayEquals( data, a.getStorage(), m.getSize());
  }

  private void testStructureData(StructureData sdata ) {

    StructureMembers sms = sdata.getStructureMembers();
    List members = sms.getMembers();

    for (int i = 0; i < members.size(); i++) {
      StructureMembers.Member m = (StructureMembers.Member) members.get(i);

      Array sdataArray = sdata.getArray(m);
      assert (sdataArray.getElementType() == m.getDataType().getPrimitiveClassType());

      Array sdataArray2 = sdata.getArray(m.getName());
      ucar.ma2.TestMA2.testEquals( sdataArray, sdataArray2);

      NCdump.printArray(sdataArray, m.getName(), System.out, null);

      testGetArrayByType(sdata, m, sdataArray);
    }
  }

  private void testGetArrayByType(StructureData sdata, StructureMembers.Member m, Array a ) {
    DataType dtype = m.getDataType();
    Object data = null;
    if (dtype == DataType.DOUBLE) {
      assert  a.getElementType() == double.class;
      data = sdata.getJavaArrayDouble( m);
    } else if (dtype == DataType.FLOAT) {
      assert  a.getElementType() == float.class;
      data = sdata.getJavaArrayFloat( m);
    } else if (dtype == DataType.LONG) {
      assert  a.getElementType() == long.class;
      data = sdata.getJavaArrayLong( m);
    } else if (dtype == DataType.INT) {
      assert  a.getElementType() == int.class;
      data = sdata.getJavaArrayInt( m);
    } else if (dtype == DataType.SHORT) {
      assert  a.getElementType() == short.class;
      data = sdata.getJavaArrayShort( m);
    } else if (dtype == DataType.BYTE) {
      assert  a.getElementType() == byte.class;
      data = sdata.getJavaArrayByte( m);
    } else if (dtype == DataType.CHAR) {
      assert  a.getElementType() == char.class;
      data = sdata.getJavaArrayChar( m);
    }

    if (data != null)
      ucar.ma2.TestMA2.testJarrayEquals( data, a.getStorage(), m.getSize());
  }



}
