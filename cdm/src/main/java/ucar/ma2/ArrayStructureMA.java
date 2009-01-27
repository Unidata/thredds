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

    StructureMembers tosm = new StructureMembers(from.getStructureMembers());
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

}