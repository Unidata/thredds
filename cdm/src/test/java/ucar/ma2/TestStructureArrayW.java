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
package ucar.ma2;

import junit.framework.TestCase;
import ucar.unidata.util.test.UtilsTestStructureArray;

import java.io.IOException;


public class TestStructureArrayW extends TestCase {

  public TestStructureArrayW(String name) {
    super(name);
  }

  /* <pre>
   Structure {
     float f1;
     short f2(3);

     Structure {
       int g1;
       double(2) g2;
       double(3,4) g3;

       Structure {
         int h1;
         double(2) h2;
       } nested2(7);

     } nested1(9);
   } s(4);
   </pre>

   <ul>
   <li>For f1, you need an ArrayFloat of shape {4}
   <li>For f2, you need an ArrayShort of shape {4, 3} .
   <li>For nested1, you need an ArrayStructure of shape {4, 9}.
   Use an ArrayStructureMA that has 3 members:
   <ul><li>For g1, you need an ArrayInt of shape (4, 9}
   <li>For g2, you need an ArrayDouble of shape {4, 9, 2}.
   <li>For g3, you need an ArrayDouble of shape {4, 9, 3, 4}.
   </ul>
   <li>For nested2, you need an ArrayStructure of shape {4, 9, 7}.
   Use an ArrayStructureMA that has 2 members:
   <ul><li>For h1, you need an ArrayInt of shape (4, 9, 7}
   <li>For h2, you need an ArrayDouble of shape {4, 9, 7, 2}.
   </ul>
   </ul>
  */
  public void testW() throws IOException, InvalidRangeException {
    StructureMembers members = new StructureMembers("s");

    StructureMembers.Member f1 = members.addMember("f1", "desc", "units", DataType.FLOAT, new int[]{1});
    members.addMember(f1);

    StructureMembers.Member f2 = members.addMember("f2", "desc", "units", DataType.SHORT, new int[]{3});
    members.addMember(f2);

    StructureMembers.Member nested1 = members.addMember("nested1", "desc", "units", DataType.STRUCTURE, new int[]{9});
    members.addMember(nested1);

    int size = 4;
    StructureData[] sdata = new StructureData[size];
    for (int i=0; i<size; i++) {
      StructureDataW sdw = new StructureDataW( members);
      sdata[i] = sdw;

      Array data = Array.factory(DataType.FLOAT, new int[]{1});
      sdw.setMemberData(f1, data);
      fill(data, i);

      data = Array.factory(DataType.SHORT, new int[]{3});
      sdw.setMemberData(f2, data);
      fill(data, i*2);

      data = makeNested1(nested1, 9, 7);
      sdw.setMemberData(nested1, data);
    }

    ArrayStructureW as = new ArrayStructureW(members, new int[]{4}, sdata);
    //System.out.println( NCdumpW.printArray(as, "", null));
    new UtilsTestStructureArray().testArrayStructure(as);

    // get f2 out of the 2nd "s"
    short[] f2data = as.getJavaArrayShort(1, f2);
    assert f2data[0] == 2;
    assert f2data[1] == 3;
    assert f2data[2] == 4;

    // get nested1 out of the 3nd "s"
    ArrayStructure nested1Data = as.getArrayStructure(2, nested1);

    // get g1 out of the 4th "nested1"
    StructureMembers.Member g1 = nested1Data.getStructureMembers().findMember("g1");
    int g1data = nested1Data.getScalarInt(3, g1);
    assert g1data == 66;

    // get g3 out of the 4th "nested1"
    StructureMembers.Member g3 = nested1Data.getStructureMembers().findMember("g3");
    double[] g3data = nested1Data.getJavaArrayDouble(3, g3);
    assert g3data[0] == 73326.0;

    // get nested2 out of the 7th "nested1"
    StructureMembers.Member nested2 = nested1Data.getStructureMembers().findMember("nested2");
    ArrayStructure nested2Data = nested1Data.getArrayStructure(6, nested2);

    // get h1 out of the 5th "nested2"
    StructureMembers.Member h1 = nested2Data.getStructureMembers().findMember("h1");
    int val = nested2Data.getScalarInt(4, h1);
    assert (val == 1218) : val;

    // get h2 out of the 5th "nested2"
    StructureMembers.Member h2 = nested2Data.getStructureMembers().findMember("h2");
    double[] h2data = nested2Data.getJavaArrayDouble(4, h2);
    assert (h2data[0] == 12018);
    assert (h2data[1] == 12019);
  }


  public Array makeNested1(StructureMembers.Member nested1, int size1, int size2) throws IOException, InvalidRangeException {
    StructureMembers members = new StructureMembers(nested1.getName());
    nested1.setStructureMembers(members);

    StructureMembers.Member g1 = members.addMember("g1", "desc", "units", DataType.INT, new int[]{1});
    members.addMember(g1);
    StructureMembers.Member g2 = members.addMember("g2", "desc", "units", DataType.DOUBLE, new int[]{2});
    members.addMember(g2);
    StructureMembers.Member g3 =  members.addMember("g3", "desc", "units", DataType.DOUBLE, new int[]{3, 4});
    members.addMember(g3);
    StructureMembers.Member nested2 =  members.addMember("nested2", "desc", "units", DataType.STRUCTURE, new int[]{7});
    members.addMember(nested2);

    StructureData[] sdata = new StructureData[size1];
    for (int i=0; i<size1; i++) {
      StructureDataW sdw = new StructureDataW( members);
      sdata[i] = sdw;

      Array data = Array.factory(DataType.INT, new int[]{1});
      sdw.setMemberData(g1, data);
      fill(data, i * 22);

      data = Array.factory(DataType.DOUBLE, new int[]{2});
      sdw.setMemberData(g2, data);
      fill(data, i * 222);

      data = Array.factory(DataType.DOUBLE, new int[]{3, 4});
      sdw.setMemberData(g3, data);
      fill(data, i * 2222);

      data = makeNested2(nested2, i, size2);
      sdw.setMemberData(nested2, data);
    }

    return new ArrayStructureW(members, new int[]{size1}, sdata);
  }

  public Array makeNested2(StructureMembers.Member nested, int who, int size) throws IOException, InvalidRangeException {
    StructureMembers members = new StructureMembers(nested.getName());
    nested.setStructureMembers(members);

    StructureMembers.Member h1 = members.addMember("h1", "desc", "units", DataType.INT, new int[]{1});
    members.addMember(h1);
    StructureMembers.Member h2 = members.addMember("h2", "desc", "units", DataType.DOUBLE, new int[]{2});
    members.addMember(h2);

    StructureData[] sdata = new StructureData[size];
    for (int i=0; i<size; i++) {
      StructureDataW sdw = new StructureDataW( members);
      sdata[i] = sdw;

      Array data = Array.factory(DataType.INT, new int[]{1});
      sdw.setMemberData(h1, data);
      fill(data, i * 303 + who);

      data = Array.factory(DataType.DOUBLE, new int[]{2});
      sdw.setMemberData(h2, data);
      fill(data, i * 3003+ who);
    }

    return new ArrayStructureW(members, new int[]{size}, sdata);
  }

  private void fill(Array a, int start) {
    IndexIterator ii = a.getIndexIterator();
    while (ii.hasNext()) {
      ii.getIntNext();
      int[] counter = ii.getCurrentCounter();
      int value = 0;
      for (int i = 0; i < counter.length; i++)
        value = start + value * 10 + counter[i];
      ii.setIntCurrent(value);
    }
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
  } */

}

