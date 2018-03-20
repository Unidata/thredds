/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.ma2;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.unidata.util.test.UtilsTestStructureArray;

import java.io.IOException;
import java.lang.invoke.MethodHandles;

public class TestStructureArrayMA {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

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

