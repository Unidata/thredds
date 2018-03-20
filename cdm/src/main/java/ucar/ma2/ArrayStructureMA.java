/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.ma2;

import ucar.nc2.Structure;
import ucar.nc2.Variable;
import ucar.nc2.Sequence;

import java.io.IOException;

/**
 * Concrete implementation of ArrayStructure, data storage is in member arrays, which are converted to
 *   StructureData member data on the fly.
 * This defers object creation for efficiency. Use getJavaArrayXXX<type>() and getScalarXXX<type>() data accessors if possible.
 *
 * How to create:
 * <pre>
    ArrayStructureMA asma = new ArrayStructureMA( smembers, getShape());
    for (int i = 0; i < orgVariables.size(); i++) {
      Variable v = (Variable) orgVariables.get(i);
      Array data = v.read();
      asma.setMemberArray( v.getName(), data);
    } </pre>

 * How to do Nested Structures:
  <pre>
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

     } nested1(12);
   } s(4);
   </pre>
   <ul>
   <li>For f1, you need an ArrayFloat of shape {4}
   <li>For f2, you need an ArrayShort of shape {4, 3} .
   <li>For nested1, you need an ArrayStructure of shape {4, 12}.
   Use an ArrayStructureMA that has 3 members:
   <ul><li>For g1, you need an ArrayInt of shape (4, 12}
   <li>For g2, you need an ArrayDouble of shape {4, 12, 2}.
   <li>For g3, you need an ArrayDouble of shape {4, 12, 3, 4}.
   </ul>
   <li>For nested2, you need an ArrayStructure of shape {4, 12, 7}.
   Use an ArrayStructureMA that has 2 members:
   <ul><li>For h1, you need an ArrayInt of shape (4, 12, 7}
   <li>For h2, you need an ArrayDouble of shape {4, 12, 7, 2}.
   </ul>
   </ul>
   Example code:
 <pre>
  public void testMA() throws IOException, InvalidRangeException {
    StructureMembers members = new StructureMembers("s");

    StructureMembers.Member m = members.addMember("f1", "desc", CDM.UNITS, DataType.FLOAT, new int[]{1});
    Array data = Array.factory(DataType.FLOAT, new int[]{4});
    m.setDataArray(data);
    fill(data);

    m = members.addMember("f2", "desc", CDM.UNITS, DataType.SHORT, new int[]{3});
    data = Array.factory(DataType.SHORT, new int[]{4, 3});
    m.setDataArray(data);
    fill(data);

    m = members.addMember("nested1", "desc", CDM.UNITS, DataType.STRUCTURE, new int[]{9});
    data = makeNested1(m);
    m.setDataArray(data);

    ArrayStructureMA as = new ArrayStructureMA(members, new int[]{4});
    //System.out.println( NCdumpW.printArray(as, "", null));
    new TestStructureArray().testArrayStructure(as);

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


  public ArrayStructure makeNested1(StructureMembers.Member parent) throws IOException, InvalidRangeException {
    StructureMembers members = new StructureMembers(parent.getName());
    parent.setStructureMembers(members);

    StructureMembers.Member m = members.addMember("g1", "desc", CDM.UNITS, DataType.INT, new int[]{1});
    Array data = Array.factory(DataType.INT, new int[]{4, 9});
    m.setDataArray(data);
    fill(data);

    m = members.addMember("g2", "desc", CDM.UNITS, DataType.DOUBLE, new int[]{2});
    data = Array.factory(DataType.DOUBLE, new int[]{4, 9, 2});
    m.setDataArray(data);
    fill(data);

    m = members.addMember("g3", "desc", CDM.UNITS, DataType.DOUBLE, new int[]{3, 4});
    data = Array.factory(DataType.DOUBLE, new int[]{4, 9, 3, 4});
    m.setDataArray(data);
    fill(data);

    m = members.addMember("nested2", "desc", CDM.UNITS, DataType.STRUCTURE, new int[]{7});
    data = makeNested2(m);
    m.setDataArray(data);

    return new ArrayStructureMA(members, new int[]{4, 9});
  }

  public ArrayStructure makeNested2(StructureMembers.Member parent) throws IOException, InvalidRangeException {
    StructureMembers members = new StructureMembers(parent.getName());
    parent.setStructureMembers(members);

    StructureMembers.Member m = members.addMember("h1", "desc", CDM.UNITS, DataType.INT, new int[]{1});
    Array data = Array.factory(DataType.INT, new int[]{4, 9, 7});
    m.setDataArray(data);
    fill(data);

    m = members.addMember("h2", "desc", CDM.UNITS, DataType.DOUBLE, new int[]{2});
    data = Array.factory(DataType.DOUBLE, new int[]{4, 9, 7, 2});
    m.setDataArray(data);
    fill(data);

    return new ArrayStructureMA(members, new int[]{4, 9, 7});
  }
 </pre>

 * @author caron
 * @see Array
 */
public class ArrayStructureMA extends ArrayStructure {
  /* Implementation notes
     Most of the methods are now the default methods in the superclass, so that other subclasses can call them.
     This happens when the data is "enhanced", member arrays are set and must override the other possible storage methods.
   */

  /**
   * Create a new Array of type StructureData and the given members and shape.
   * <p> You must set the data Arrays on each of the Members, using setDataObject(). These data Arrays contain the data
   * for that member Variable, for all the StructureData. Therefore it has rank one greater that the Members. The extra
   * dimension must be the outermost (slowest varying) dimension. ie, if some member has shape [3,10], the array would have
   * shape [nrows, 3, 10].
   *
   * @param members a description of the structure members
   * @param shape       the shape of the Array.
   */
  public ArrayStructureMA(StructureMembers members, int[] shape) {
    super(members, shape);
  }

  public ArrayStructureMA(StructureMembers members, int[] shape, StructureData[] sdata) {
    super(members, shape);
    if (nelems != sdata.length)
      throw new IllegalArgumentException("StructureData length= "+sdata.length+"!= shape.length="+nelems);
    this.sdata = sdata;
  }

  /**
   * Turn any ArrayStructure into a ArrayStructureMA
   * @param from copy from here. If from is a ArrayStructureMA, return it.
   * @return equivilent ArrayStructureMA
   * @throws java.io.IOException on error reading a sequence
   */
  static public ArrayStructureMA factoryMA(ArrayStructure from) throws IOException {
    if (from instanceof ArrayStructureMA)
      return (ArrayStructureMA) from;

    StructureMembers tosm = new StructureMembers( new StructureMembers(from.getStructureMembers()));
    ArrayStructureMA to = new ArrayStructureMA(tosm, from.getShape());
    for (StructureMembers.Member m : from.getMembers()) {
      to.setMemberArray(m.getName(), from.extractMemberArray(m));
    }
    return to;
  }

  @Override
  protected StructureData makeStructureData( ArrayStructure as, int index) {
    return new StructureDataA( as, index);
  }

  /**
   * Set the data array for this member.
   * @param memberName name of member
   * @param data Array for this member.
   */
  public void setMemberArray( String memberName, Array data) {
    StructureMembers.Member m = members.findMember( memberName);
    m.setDataArray( data);
  }

  public Array getArray(int recno, StructureMembers.Member m) {
    if (m.isVariableLength()) {
      Array data = m.getDataArray();
      if (data instanceof ArrayObject) {
        ArrayObject ao = (ArrayObject) data;
        Object dao = ao.getObject(recno);
        assert dao instanceof Array;
        return (Array) dao;
      }
    }
    return super.getArray(recno, m);
  }

  /**
   * Create an ArrayStructure for a Structure. Allow nested Structures.
   * Create the data arrays, and an iterator.
   *
   * @param from copy from here. If from is a ArrayStructureMA, return it.
   * @param shape the shape of the ArrayStructure
   * @return equivilent ArrayStructureMA
   * @throws java.io.IOException on error reading a sequence
   */
  static public ArrayStructureMA factoryMA(Structure from, int[] shape) throws IOException {
    StructureMembers sm = from.makeStructureMembers();
    for (Variable v : from.getVariables()) {
      Array data;
      if (v instanceof Sequence) {
        data = Array.factory(DataType.SEQUENCE, shape);  // an array sequence - one for each parent element
        //Structure s = (Structure) v;
        //StructureMembers smn = s.makeStructureMembers();
        // data = new ArraySequenceNested(smn, (int) Index.computeSize(v.getShapeAll())); // ??

      } else if (v instanceof Structure)
        data = ArrayStructureMA.factoryMA((Structure) v, combine(shape, v.getShape()));

      else
        data = Array.factory(v.getDataType(), combine(shape, v.getShape()));

      StructureMembers.Member m = sm.findMember(v.getShortName());
      m.setDataArray(data);
    }

    return new ArrayStructureMA(sm, shape);
  }

  static private int[] combine(int[] shape1, int[] shape2) {
    int[] result = new int[shape1.length + shape2.length];
    System.arraycopy(shape1, 0, result, 0, shape1.length);
    System.arraycopy(shape2, 0, result, shape1.length, shape2.length);
    return result;
  }


}