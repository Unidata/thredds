/*
 * Copyright 1998-2013 University Corporation for Atmospheric Research/Unidata
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
package ucar.ma2;

import org.junit.Assert;
import org.junit.Test;
import ucar.unidata.util.test.UtilsTestStructureArray;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

public class TestStructureArrayBB {

  /* <pre>
   Structure {
     int  f1;
     int f2(9);

     Structure {
       int g1;
       int(2) g2;
       int(3,4) g3;

       Structure {
         int(3) h1;
         int(2) h2;
       } nested2(17);

     } nested1(10);
   } s(4);
   </pre>
   so
   <pre>
   each s record  1010 * 4 = 4040
     10 ints
     10 nested1  10 * 100 = 1000
        15 ints
        17 nested2 17 * 5 = 85
           5 ints
  */
  @Test
  public void testBB() throws IOException, InvalidRangeException {
    StructureMembers members = new StructureMembers("s");
    members.addMember("f1", "desc", "units", DataType.INT, new int[]{1});
    members.addMember("f2", "desc", "units", DataType.INT, new int[]{9});  // 10

    StructureMembers.Member nested1 = members.addMember("nested1", "desc", "units", DataType.STRUCTURE, new int[]{10});
    StructureMembers nested1_members = new StructureMembers("nested1");
    nested1_members.addMember("g1", "desc", "units", DataType.INT, new int[]{1});
    nested1_members.addMember("g2", "desc", "units", DataType.INT, new int[]{2});
    nested1_members.addMember("g3", "desc", "units", DataType.INT, new int[]{3,4});  // (15 + 85) * 10 = 1000

    StructureMembers.Member nested2 = nested1_members.addMember("nested2", "desc", "units", DataType.STRUCTURE, new int[]{17}); // 5 * 17 = 85
    nested1.setStructureMembers(nested1_members);

    StructureMembers nested2_members = new StructureMembers("nested2");
    nested2_members.addMember("h1", "desc", "units", DataType.INT, new int[]{3});
    nested2_members.addMember("h2", "desc", "units", DataType.INT, new int[]{2});  // 5
    nested2.setStructureMembers(nested2_members);

    ArrayStructureBB.setOffsets(members);
    int[] offs = {0, 4, 40};
    for (int i = 0; i < offs.length; ++i) {
      StructureMembers.Member m = members.getMember(i);
      Assert.assertEquals("Bad offset for " + m.getName(), offs[i],
              m.getDataParam());
    }

    int[] offs2 = {0, 4, 12, 60};
    for (int i = 0; i < offs2.length; ++i) {
        StructureMembers.Member m = nested1_members.getMember(i);
        Assert.assertEquals("Bad offset for " + m.getName(), offs2[i],
                m.getDataParam());
    }

//    Formatter f = new Formatter(System.out);
//    Indent indent = new Indent(2);
//    ArrayStructureBB.showOffsets(members, indent, f);

    ArrayStructureBB bb = new ArrayStructureBB(members, new int[]{4});
    fillStructureArray(bb);

//    System.out.println( NCdumpW.toString(bb, "test arrayBB", null));

    new UtilsTestStructureArray().testArrayStructure(bb);

    int sreclen = 1010;
    int n1reclen = 100;
    int n2reclen = 5;

    // get f2 out of the 3nd "s"
    int srecno = 2;
    StructureMembers.Member f2 = bb.getStructureMembers().findMember("f2");
    int[] f2data = bb.getJavaArrayInt(srecno, f2);
    assert f2data[0] == srecno * sreclen + 1 : f2data[0];
    assert f2data[1] == srecno * sreclen + 2 : f2data[0];
    assert f2data[2] == srecno * sreclen + 3 : f2data[0];

    // get nested1 out of the 3nd "s"
    ArrayStructure nested1Data = bb.getArrayStructure(srecno, nested1);
    // get g1 out of the 7th "nested1"
    int n1recno = 6;
    StructureMembers.Member g1 = nested1Data.getStructureMembers().findMember("g1");
    int g1data = nested1Data.getScalarInt(n1recno, g1);
    assert g1data == srecno * sreclen + n1recno * n1reclen + 10 : g1data;

    // get nested2 out of the 7th "nested1"
    ArrayStructure nested2Data = nested1Data.getArrayStructure(n1recno, nested2);
    // get h1 out of the 4th "nested2"
    int n2recno = 3;
    StructureMembers.Member h1 = nested2Data.getStructureMembers().findMember("h1");
    int val = nested2Data.getScalarInt(n2recno, h1);
    assert (val == srecno * sreclen + n1recno * n1reclen + n2recno * n2reclen + 15 + 10) : val;
  }

  private void fillStructureArray(ArrayStructureBB sa) {
    ByteBuffer bb = sa.getByteBuffer();
    IntBuffer ibb = bb.asIntBuffer();
    int count = 0;
    for (int i=0; i<ibb.capacity(); i++)
      ibb.put(i, count++);
  }


  private void fill(Array a) {
    IndexIterator ii = a.getIndexIterator();
    while (ii.hasNext()) {
      ii.getIntNext();
      int[] counter = ii.getCurrentCounter();
      int value = 0;
      for (int i = 0; i < counter.length; i++)
        value = value * 10 + counter[i];
      ii.setIntCurrent(value);
    }
  }


  public ArrayStructure makeNested1(StructureMembers.Member parent) throws IOException, InvalidRangeException {
    StructureMembers members = new StructureMembers(parent.getName());
    parent.setStructureMembers(members);

    StructureMembers.Member m = members.addMember("g1", "desc", "units", DataType.INT, new int[]{1});
    Array data = Array.factory(DataType.INT, new int[]{4, 9});
    m.setDataArray(data);
    fill(data);

    m = members.addMember("g2", "desc", "units", DataType.DOUBLE, new int[]{2});
    data = Array.factory(DataType.DOUBLE, new int[]{4, 9, 2});
    m.setDataArray(data);
    fill(data);

    m = members.addMember("g3", "desc", "units", DataType.DOUBLE, new int[]{3, 4});
    data = Array.factory(DataType.DOUBLE, new int[]{4, 9, 3, 4});
    m.setDataArray(data);
    fill(data);

    m = members.addMember("nested2", "desc", "units", DataType.STRUCTURE, new int[]{7});
    data = makeNested2(m);
    m.setDataArray(data);

    return new ArrayStructureBB(members, new int[]{4, 9});
  }

  public ArrayStructure makeNested2(StructureMembers.Member parent) throws IOException, InvalidRangeException {
    StructureMembers members = new StructureMembers(parent.getName());
    parent.setStructureMembers(members);

    StructureMembers.Member m = members.addMember("h1", "desc", "units", DataType.INT, new int[]{1});
    Array data = Array.factory(DataType.INT, new int[]{4, 9, 7});
    m.setDataArray(data);
    fill(data);

    m = members.addMember("h2", "desc", "units", DataType.DOUBLE, new int[]{2});
    data = Array.factory(DataType.DOUBLE, new int[]{4, 9, 7, 2});
    m.setDataArray(data);
    fill(data);

    return new ArrayStructureBB(members, new int[]{4, 9, 7});
  }

  /* static public void testArrayStructure(ArrayStructure as) {

    StructureMembers sms = as.getStructureMembers();
    List members = sms.getMembers();
    String name = sms.getName();

    int n = (int) as.getSize();
    for (int recno = 0; recno < n; recno++) {
      Object o = as.getObject(recno);
      assert (o instanceof StructureData);
      StructureData sdata = as.getStructureData(recno);
      assert (o == sdata);

      for (int i = 0; i < members.size(); i++) {
        StructureMembers.Member m = (StructureMembers.Member) members.get(i);

        Array sdataArray = sdata.getArray(m);
        assert (sdataArray.getElementType() == m.getDataType().getPrimitiveClassType());

        Array sdataArray2 = sdata.getArray(m.getName());
        ucar.ma2.TestMA2.testEquals(sdataArray, sdataArray2);

        Array a = as.getArray(recno, m);
        assert (a.getElementType() == m.getDataType().getPrimitiveClassType());
        ucar.ma2.TestMA2.testEquals(sdataArray, a);

        testGetArrayByType(as, recno, m, a);
      }
      testStructureData(sdata);
    }
  }

  static public void printArrayStructure(ArrayStructure as) throws IOException {

    StructureMembers sms = as.getStructureMembers();
    List members = sms.getMembers();
    String name = sms.getName();

    int n = (int) as.getSize();
    for (int recno = 0; recno < n; recno++) {
      System.out.println("\n***Dump " + name + " record=" + recno);
      for (int i = 0; i < members.size(); i++) {
        StructureMembers.Member m = (StructureMembers.Member) members.get(i);

        Array a = as.getArray(recno, m);
        if (a instanceof ArrayStructure)
          printArrayStructure((ArrayStructure) a);
        else
          NCdump.printArray(a, m.getName(), System.out, null);
      }
    }

    System.out.println(NCdumpW.printArray(as, "", null));
  }

  static private void testGetArrayByType(ArrayStructure as, int recno, StructureMembers.Member m, Array a) {
    DataType dtype = m.getDataType();
    Object data = null;
    if (dtype == DataType.DOUBLE) {
      assert a.getElementType() == double.class;
      data = as.getJavaArrayDouble(recno, m);
    } else if (dtype == DataType.FLOAT) {
      assert a.getElementType() == float.class;
      data = as.getJavaArrayFloat(recno, m);
    } else if (dtype == DataType.LONG) {
      assert a.getElementType() == long.class;
      data = as.getJavaArrayLong(recno, m);
    } else if (dtype == DataType.INT) {
      assert a.getElementType() == int.class;
      data = as.getJavaArrayInt(recno, m);
    } else if (dtype == DataType.SHORT) {
      assert a.getElementType() == short.class;
      data = as.getJavaArrayShort(recno, m);
    } else if (dtype == DataType.BYTE) {
      assert a.getElementType() == byte.class;
      data = as.getJavaArrayByte(recno, m);
    } else if (dtype == DataType.CHAR) {
      assert a.getElementType() == char.class;
      data = as.getJavaArrayChar(recno, m);
    } else if (dtype == DataType.STRING) {
      assert a.getElementType() == String.class;
      data = as.getJavaArrayString(recno, m);
    } else if (dtype == DataType.STRUCTURE) {
      assert a.getElementType() == StructureData.class;
      ArrayStructure nested = as.getArrayStructure(recno, m);
      testArrayStructure(nested);
    }

    if (data != null)
      ucar.ma2.TestMA2.testJarrayEquals(data, a.getStorage(), m.getSize());
  }

  static private void testStructureData(StructureData sdata) {

    StructureMembers sms = sdata.getStructureMembers();
    List members = sms.getMembers();

    for (int i = 0; i < members.size(); i++) {
      StructureMembers.Member m = (StructureMembers.Member) members.get(i);

      Array sdataArray = sdata.getArray(m);
      assert (sdataArray.getElementType() == m.getDataType().getPrimitiveClassType());

      Array sdataArray2 = sdata.getArray(m.getName());
      ucar.ma2.TestMA2.testEquals(sdataArray, sdataArray2);

      //NCdump.printArray(sdataArray, m.getName(), System.out, null);

      testGetArrayByType(sdata, m, sdataArray);
    }
  }

  static private void testGetArrayByType(StructureData sdata, StructureMembers.Member m, Array a) {
    DataType dtype = m.getDataType();
    Object data = null;
    if (dtype == DataType.DOUBLE) {
      assert a.getElementType() == double.class;
      data = sdata.getJavaArrayDouble(m);
    } else if (dtype == DataType.FLOAT) {
      assert a.getElementType() == float.class;
      data = sdata.getJavaArrayFloat(m);
    } else if (dtype == DataType.LONG) {
      assert a.getElementType() == long.class;
      data = sdata.getJavaArrayLong(m);
    } else if (dtype == DataType.INT) {
      assert a.getElementType() == int.class;
      data = sdata.getJavaArrayInt(m);
    } else if (dtype == DataType.SHORT) {
      assert a.getElementType() == short.class;
      data = sdata.getJavaArrayShort(m);
    } else if (dtype == DataType.BYTE) {
      assert a.getElementType() == byte.class;
      data = sdata.getJavaArrayByte(m);
    } else if (dtype == DataType.CHAR) {
      assert a.getElementType() == char.class;
      data = sdata.getJavaArrayChar(m);
    } else if (dtype == DataType.STRING) {
      assert a.getElementType() == String.class;
      data = sdata.getJavaArrayString(m);
    } else if (dtype == DataType.STRUCTURE) {
      assert a.getElementType() == StructureData.class;
      ArrayStructure nested = sdata.getArrayStructure(m);
      testArrayStructure(nested);
    }

    if (data != null)
      ucar.ma2.TestMA2.testJarrayEquals(data, a.getStorage(), m.getSize());
  }  */

}

