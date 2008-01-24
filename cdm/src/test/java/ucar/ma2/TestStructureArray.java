package ucar.ma2;

import junit.framework.TestCase;

import java.io.IOException;
import java.util.List;

import ucar.nc2.*;


public class TestStructureArray extends TestCase {

  public TestStructureArray( String name) {
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
  public void testMA() throws IOException, InvalidRangeException {
    StructureMembers members = new StructureMembers("s");

    StructureMembers.Member m = new StructureMembers.Member("f1", "desc", "units", DataType.FLOAT, new int[] {1});
    members.addMember( m);
    Array data = Array.factory(DataType.FLOAT, new int[] {4});
    m.setDataArray( data);
    fill(data);

    m = new StructureMembers.Member("f2", "desc", "units", DataType.SHORT, new int[] {3});
    members.addMember( m);
    data = Array.factory(DataType.SHORT, new int[] {4, 3});
    m.setDataArray( data);
    fill(data);

    m = new StructureMembers.Member("nested1", "desc", "units", DataType.STRUCTURE, new int[] {9});
    members.addMember( m);
    data = makeNested1( m);
    m.setDataArray( data);

    ArrayStructureMA as = new ArrayStructureMA( members, new int[] {4});
    testArrayStructure( as);
    printArrayStructure( as);

    StructureMembers.Member nested1 = as.getStructureMembers().findMember("nested1");
    ArrayStructure nestedData = as.getArrayStructure(2, nested1);
    StructureMembers.Member nested2 = nestedData.getStructureMembers().findMember("nested2");
    ArrayStructure nestedData2 = nestedData.getArrayStructure(6, nested2);
    StructureMembers.Member nested3 = nestedData2.getStructureMembers().findMember("h1");
    int val = nestedData2.getScalarInt(4, nested3);
    assert (val == 264) : val;
  }

  public ArrayStructure makeNested1(StructureMembers.Member parent) throws IOException, InvalidRangeException {
    StructureMembers members = new StructureMembers(parent.getName());
    parent.setStructureMembers( members);

    StructureMembers.Member m = new StructureMembers.Member("g1", "desc", "units", DataType.INT, new int[] {1});
    members.addMember( m);
    Array data = Array.factory(DataType.INT, new int[] {4, 9});
    m.setDataArray( data);
    fill(data);

    m = new StructureMembers.Member("g2", "desc", "units", DataType.DOUBLE, new int[] {2});
    members.addMember( m);
    data = Array.factory(DataType.DOUBLE, new int[] {4, 9, 2});
    m.setDataArray( data);
    fill(data);

    m = new StructureMembers.Member("g3", "desc", "units", DataType.DOUBLE, new int[] {3, 4});
    members.addMember( m);
    data = Array.factory(DataType.DOUBLE, new int[] {4, 9, 3, 4});
    m.setDataArray( data);
    fill(data);

    m = new StructureMembers.Member("nested2", "desc", "units", DataType.STRUCTURE, new int[] {7});
    members.addMember( m);
    data = makeNested2( m);
    m.setDataArray( data);

    return new ArrayStructureMA( members, new int[] {4, 9});
  }

  public ArrayStructure makeNested2(StructureMembers.Member parent) throws IOException, InvalidRangeException {
    StructureMembers members = new StructureMembers(parent.getName());
    parent.setStructureMembers( members);

    StructureMembers.Member m = new StructureMembers.Member("h1", "desc", "units", DataType.INT, new int[] {1});
    members.addMember( m);
    Array data = Array.factory(DataType.INT, new int[] {4, 9, 7});
    m.setDataArray( data);
    fill(data);

    m = new StructureMembers.Member("h2", "desc", "units", DataType.DOUBLE, new int[] {2});
    members.addMember( m);
    data = Array.factory(DataType.DOUBLE, new int[] {4, 9, 7, 2});
    m.setDataArray( data);
    fill(data);

    return new ArrayStructureMA( members, new int[] {4, 9, 7});
  }

 private void fill(Array a) {
    IndexIterator ii = a.getIndexIterator();
    while (ii.hasNext()) {
      ii.getIntNext();
      int[] counter = ii.getCurrentCounter();
      int value = 0;
      for (int i=0; i<counter.length; i++)
        value = value * 10 + counter[i];
      ii.setIntCurrent( value);
    }
  }

  static public void testArrayStructure(ArrayStructure as ) {

    StructureMembers sms = as.getStructureMembers();
    List members = sms.getMembers();
    String name = sms.getName();

    int n = (int) as.getSize();
    for (int recno=0; recno<n; recno++) {
      Object o = as.getObject( recno);
      assert (o instanceof StructureData);
      StructureData sdata = as.getStructureData( recno);
      assert (o == sdata);

      for (int i = 0; i < members.size(); i++) {
        StructureMembers.Member m = (StructureMembers.Member) members.get(i);

        Array sdataArray = sdata.getArray(m);
        assert (sdataArray.getElementType() == m.getDataType().getPrimitiveClassType());

        Array sdataArray2 = sdata.getArray(m.getName());
        ucar.ma2.TestMA2.testEquals( sdataArray, sdataArray2);

        Array a = as.getArray(recno, m);
        assert (a.getElementType() == m.getDataType().getPrimitiveClassType());
        ucar.ma2.TestMA2.testEquals( sdataArray, a);

        testGetArrayByType(as, recno, m, a);
      }
      testStructureData( sdata);
    }
  }

  static public void printArrayStructure(ArrayStructure as ) {

    StructureMembers sms = as.getStructureMembers();
    List members = sms.getMembers();
    String name = sms.getName();

    int n = (int) as.getSize();
    for (int recno=0; recno<n; recno++) {
      System.out.println("\n***Dump "+name+" record="+recno);
      for (int i = 0; i < members.size(); i++) {
        StructureMembers.Member m = (StructureMembers.Member) members.get(i);

        Array a = as.getArray(recno, m);
        if (a instanceof ArrayStructure)
          printArrayStructure((ArrayStructure) a);
        else
          NCdump.printArray(a, m.getName(), System.out, null);
      }
    }
  }

  static private void testGetArrayByType(ArrayStructure as, int recno, StructureMembers.Member m, Array a ) {
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
    } else if (dtype == DataType.STRING) {
      assert  a.getElementType() == String.class;
      data = as.getJavaArrayString( recno, m);
    } else if (dtype == DataType.STRUCTURE) {
      assert  a.getElementType() == StructureData.class;
      ArrayStructure nested = as.getArrayStructure( recno, m);
      testArrayStructure( nested);
    }

    if (data != null)
      ucar.ma2.TestMA2.testJarrayEquals( data, a.getStorage(), m.getSize());
  }

  static private void testStructureData(StructureData sdata ) {

    StructureMembers sms = sdata.getStructureMembers();
    List members = sms.getMembers();

    for (int i = 0; i < members.size(); i++) {
      StructureMembers.Member m = (StructureMembers.Member) members.get(i);

      Array sdataArray = sdata.getArray(m);
      assert (sdataArray.getElementType() == m.getDataType().getPrimitiveClassType());

      Array sdataArray2 = sdata.getArray(m.getName());
      ucar.ma2.TestMA2.testEquals( sdataArray, sdataArray2);

      //NCdump.printArray(sdataArray, m.getName(), System.out, null);

      testGetArrayByType(sdata, m, sdataArray);
    }
  }

  static private void testGetArrayByType(StructureData sdata, StructureMembers.Member m, Array a ) {
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
    } else if (dtype == DataType.STRING) {
      assert  a.getElementType() == String.class;
      data = sdata.getJavaArrayString( m);
    } else if (dtype == DataType.STRUCTURE) {
      assert  a.getElementType() == StructureData.class;
      ArrayStructure nested = sdata.getArrayStructure( m);
      testArrayStructure( nested);
    }

    if (data != null)
      ucar.ma2.TestMA2.testJarrayEquals( data, a.getStorage(), m.getSize());
  }

}

