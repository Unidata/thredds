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

import org.junit.Test;
import ucar.unidata.util.test.UtilsTestStructureArray;

import java.io.IOException;

public class TestStructureArrayMA {

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
  @Test
  public void testMA() throws IOException, InvalidRangeException {
    StructureMembers members = new StructureMembers("s");

    StructureMembers.Member m = members.addMember("f1", "desc", "units", DataType.FLOAT, new int[]{1});
    Array data = Array.factory(DataType.FLOAT, new int[]{4});
    m.setDataArray(data);
    fill(data);

    m = members.addMember("f2", "desc", "units", DataType.SHORT, new int[]{3});
    data = Array.factory(DataType.SHORT, new int[]{4, 3});
    m.setDataArray(data);
    fill(data);

    m = members.addMember("nested1", "desc", "units", DataType.STRUCTURE, new int[]{9});
    data = makeNested1(m);
    m.setDataArray(data);

    ArrayStructureMA as = new ArrayStructureMA(members, new int[]{4});
    //System.out.println( NCdumpW.printArray(as, "", null));
    new UtilsTestStructureArray().testArrayStructure(as);

    // get f2 out of the 3nd "s"
    StructureMembers.Member f2 = as.getStructureMembers().findMember("f2");
    short[] f2data = as.getJavaArrayShort(2, f2);
    assert f2data[0] == 20;
    assert f2data[1] == 21;
    assert f2data[2] == 22;

    // get nested1 out of the 3nd "s"
    StructureMembers.Member nested1 = as.getStructureMembers().findMember("nested1");
    ArrayStructure nested1Data = as.getArrayStructure(2, nested1);

    // get g1 out of the 7th "nested1"
    StructureMembers.Member g1 = nested1Data.getStructureMembers().findMember("g1");
    int g1data = nested1Data.getScalarInt(6, g1);
    assert g1data == 26;

    // get nested2 out of the 7th "nested1"
    StructureMembers.Member nested2 = nested1Data.getStructureMembers().findMember("nested2");
    ArrayStructure nested2Data = nested1Data.getArrayStructure(6, nested2);

    // get h1 out of the 4th "nested2"
    StructureMembers.Member h1 = nested2Data.getStructureMembers().findMember("h1");
    int val = nested2Data.getScalarInt(4, h1);
    assert (val == 264);
  }


  private ArrayStructure makeNested1(StructureMembers.Member parent) throws IOException, InvalidRangeException {
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

    return new ArrayStructureMA(members, new int[]{4, 9});
  }

  private ArrayStructure makeNested2(StructureMembers.Member parent) throws IOException, InvalidRangeException {
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

    return new ArrayStructureMA(members, new int[]{4, 9, 7});
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

}

