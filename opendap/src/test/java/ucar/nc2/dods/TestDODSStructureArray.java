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

import opendap.test.TestSources;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import ucar.ma2.Array;
import ucar.ma2.ArrayInt;
import ucar.ma2.ArrayShort;
import ucar.ma2.ArrayStructure;
import ucar.ma2.DataType;
import ucar.ma2.Index;
import ucar.ma2.IndexIterator;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.StructureData;
import ucar.ma2.StructureMembers;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Structure;
import ucar.nc2.Variable;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.unidata.util.test.category.NeedsExternalResource;
import ucar.unidata.util.test.UtilsTestStructureArray;

/** Test nc2 dods in the JUnit framework. */
@Category(NeedsExternalResource.class)
public class TestDODSStructureArray {

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
