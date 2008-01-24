/*
 * Copyright 1997-2008 Unidata Program Center/University Corporation for
 * Atmospheric Research, P.O. Box 3000, Boulder, CO 80307,
 * support@unidata.ucar.edu.
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or (at
 * your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser
 * General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package ucar.ma2;

/**
 * Concrete implementation of ArrayStructure, data storage is in member arrays, which are converted to
 *   StructureData member data on the fly.
 * This defers object creation for efficiency. Use getArray<type>() and getScalar<type>() data accessors if possible.
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

    StructureMembers.Member m = new StructureMembers.Member("f1", "desc", "units", DataType.FLOAT, new int[] {1});
    members.addMember( m);
    Array data = Array.factory(DataType.FLOAT, new int[] {4});
    m.setDataObject( data);
    fill(data);

    m = new StructureMembers.Member("f2", "desc", "units", DataType.SHORT, new int[] {3});
    members.addMember( m);
    data = Array.factory(DataType.SHORT, new int[] {4, 3});
    m.setDataObject( data);
    fill(data);

    m = new StructureMembers.Member("nested1", "desc", "units", DataType.STRUCTURE, new int[] {9});
    members.addMember( m);
    data = makeNested1( m);
    m.setDataObject( data);

    ArrayStructureMA as = new ArrayStructureMA( members, new int[] {4});
  }

  public ArrayStructure makeNested1(StructureMembers.Member parent) throws IOException, InvalidRangeException {
    StructureMembers members = new StructureMembers(parent.getName());
    parent.setStructureMembers( members);

    StructureMembers.Member m = new StructureMembers.Member("g1", "desc", "units", DataType.INT, new int[] {1});
    members.addMember( m);
    Array data = Array.factory(DataType.INT, new int[] {4, 9});
    m.setDataObject( data);
    fill(data);

    m = new StructureMembers.Member("g2", "desc", "units", DataType.DOUBLE, new int[] {2});
    members.addMember( m);
    data = Array.factory(DataType.DOUBLE, new int[] {4, 9, 2});
    m.setDataObject( data);
    fill(data);

    m = new StructureMembers.Member("g3", "desc", "units", DataType.DOUBLE, new int[] {3, 4});
    members.addMember( m);
    data = Array.factory(DataType.DOUBLE, new int[] {4, 9, 3, 4});
    m.setDataObject( data);
    fill(data);

    m = new StructureMembers.Member("nested2", "desc", "units", DataType.STRUCTURE, new int[] {7});
    members.addMember( m);
    data = makeNested2( m);
    m.setDataObject( data);

    return new ArrayStructureMA( members, new int[] {4, 9});
  }

  public ArrayStructure makeNested2(StructureMembers.Member parent) throws IOException, InvalidRangeException {
    StructureMembers members = new StructureMembers(parent.getName());
    parent.setStructureMembers( members);

    StructureMembers.Member m = new StructureMembers.Member("h1", "desc", "units", DataType.INT, new int[] {1});
    members.addMember( m);
    Array data = Array.factory(DataType.INT, new int[] {4, 9, 7});
    m.setDataObject( data);
    fill(data);

    m = new StructureMembers.Member("h2", "desc", "units", DataType.DOUBLE, new int[] {2});
    members.addMember( m);
    data = Array.factory(DataType.DOUBLE, new int[] {4, 9, 7, 2});
    m.setDataObject( data);
    fill(data);

    return new ArrayStructureMA( members, new int[] {4, 9, 7});
  }
 </pre>

 * @author caron
 * @see Array
 */
public class ArrayStructureMA extends ArrayStructure {

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

}