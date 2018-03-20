/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.dods;

import opendap.test.TestSources;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.ma2.*;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Structure;
import ucar.nc2.Variable;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.unidata.util.test.UtilsTestStructureArray;

import java.io.IOException;
import java.lang.invoke.MethodHandles;

/** Test nc2 dods in the JUnit framework. */
public class TestDODSStructureArray {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Test
  public void testArrays() throws IOException {
    DODSNetcdfFile dodsfile = TestDODSRead.open("test.21");

    Variable v = null;
    Array a = null;

    // int32
    assert(null != (v = dodsfile.findVariable("exp.j")));
    assert v.getShortName().equals("j");
    assert v.getRank() == 0;
    assert v.getSize() == 1;
    assert v.getDataType() == DataType.INT;
    a = v.read();
    assert a.getElementType() == int.class;
    assert a instanceof ArrayInt.D0;
    int vali = ((ArrayInt.D0)a).get();
    assert (vali == 1);

    assert null != (v = dodsfile.findVariable("exp.i"));
    assert v.getShortName().equals("i");
    assert v.getRank() == 0;
    assert v.getSize() == 1;
    assert v.getDataType() == DataType.INT;
    a = v.read();
    assert a.getElementType() == int.class;
    assert a instanceof ArrayInt.D0;
    vali = ((ArrayInt.D0)a).get();
    assert vali == 2 : vali;    //?? LOOK ? CHANGES depending on how its read. POTTER!!!!

    // int16 array
    assert null != (v = dodsfile.findVariable("exp.g"));
    assert v.getShortName().equals("g");
    assert v.getRank() == 3;
    assert v.getSize() == 27;
    assert v.getDataType() == DataType.SHORT;
    a = v.read();
    assert a.getElementType() == short.class;
    assert a instanceof ArrayShort.D3;
    IndexIterator iter = a.getIndexIterator();
    int count = 0;
    while (iter.hasNext()) {
      assert iter.getShortNext() == 256 * count;
      count++;
    }

    assert null != (v = dodsfile.findVariable("exp.f" ));
    assert v.getShortName().equals("f");
    assert v.getRank() == 2;
    assert v.getSize() == 4;
    assert v.getDataType() == DataType.SHORT;
    a = v.read();
    assert a.getElementType() == short.class;
    assert a instanceof ArrayShort.D2;
    iter = a.getIndexIterator();
    count = 0;
    while (iter.hasNext()) {
      assert iter.getShortNext() == 256 * count;
      count++;
    }

  }

  @Test
  public void testArray2() throws IOException {
    DODSNetcdfFile dodsfile = TestDODSRead.open("test.21");
    DODSStructure exp = (DODSStructure) dodsfile.findVariable("exp");
    StructureData sd = exp.readStructure();

    StructureMembers.Member m = null;
    Variable v = null;
    Array a = null;

    // int32
    assert(null != (m = sd.findMember("j")));
    assert m.getName().equals("j");
    assert m.getDataType() == DataType.INT;
    a = sd.getArray(m);
    assert a.getElementType() == int.class;
    assert a instanceof ArrayInt.D0;
    int vali = ((ArrayInt.D0)a).get();
    assert (vali == 1);

    assert(null != (m = sd.findMember("i")));
    assert m.getName().equals("i");
    assert m.getDataType() == DataType.INT;
    a = sd.getArray(m);
    assert a.getSize() == 1;
    assert a.getElementType() == int.class;
    assert a instanceof ArrayInt.D0;
    vali = ((ArrayInt.D0)a).get();
    assert vali == 2 : vali;    //?? LOOK ? CHANGES depending on how its read. POTTER!!!!

    // int16 array
    assert(null != (m = sd.findMember("g")));
    assert m.getName().equals("g");
    assert m.getDataType() == DataType.SHORT;
    a = sd.getArray(m);
    assert a.getSize() == 27;
    assert a.getElementType() == short.class;
    assert a instanceof ArrayShort.D3;
    IndexIterator iter = a.getIndexIterator();
    int count = 0;
    while (iter.hasNext()) {
      assert iter.getShortNext() == 256 * count;
      count++;
    }

    assert(null != (m = sd.findMember("f" )));
    assert m.getName().equals("f");
    assert m.getDataType() == DataType.SHORT;
    a = sd.getArray(m);
    assert a.getSize() == 4;
    assert a.getElementType() == short.class;
    assert a instanceof ArrayShort.D2;
    iter = a.getIndexIterator();
    count = 0;
    while (iter.hasNext()) {
      assert iter.getShortNext() == 256 * count;
      count++;
    }

  }

  @Test
  public void testSARead() throws IOException {
    DODSNetcdfFile dodsfile = TestDODSRead.open("test.21");
    DODSStructure exp = (DODSStructure) dodsfile.findVariable("exp");
    StructureData data = exp.readStructure();

    StructureMembers.Member m = data.findMember("f");
    assert m != null;
    //assert m.getV() instanceof DODSVariable;
    //DODSVariable dv = (DODSVariable) m.getV();
    //assert dv.hasCachedData();

    Array a = data.getArray(m);
    Index ima = a.getIndex();
    assert a.getShort( ima.set(1,1)) == (short) 768;
  }

  @Test
  public void testDODS() throws IOException, InvalidRangeException {
    testW(TestSources.XURL1+"/test.53", "types", false);
  }

  public void testW(String url, String sname, boolean isScalar) throws IOException, InvalidRangeException {
    NetcdfFile ncfile = NetcdfDataset.openFile(url, null);
    Structure v = (Structure) ncfile.findVariable(sname);
    assert v != null;

    assert( v.getDataType() == DataType.STRUCTURE);

    Array data = v.read();
    assert( data instanceof ArrayStructure);
    assert(data.getElementType() == StructureData.class);

    new UtilsTestStructureArray().testArrayStructure( (ArrayStructure) data);
    ncfile.close();
  }


}
