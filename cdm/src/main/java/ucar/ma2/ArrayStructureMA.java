// $Id: ArrayStructureMA.java,v 1.6 2006/05/24 17:47:44 caron Exp $
/*
 * Copyright 1997-2006 Unidata Program Center/University Corporation for
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
 * @version $Revision: 1.6 $ $Date: 2006/05/24 17:47:44 $
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

  /**
   * Set the data array for this member.
   * @param memberName name of member
   * @param data Array for this member. 
   */
  public void setMemberArray( String memberName, Array data) {
    StructureMembers.Member m = members.findMember( memberName);
    m.setDataObject( data);
  }


  protected StructureData makeStructureData( ArrayStructure as, int index) {
    return new StructureDataA( as, index);
  }

  /**
   * LOOK doesnt work, because of the methods using recnum, not Index (!)
   * create new Array with given indexImpl and the same backing store
   *
  public Array createView(Index index) {
    return new ArrayStructureMA(members, index, nelems, sdata);
  }

  /**
   * Create a new Array using the given IndexArray and backing store.
   * used for sections, and factory.
   *
   * @param members     a description of the structure members
   * @param ima         use this IndexArray as the index
   * @param nelems      the total number of StructureData elements in the backing array
   * @param sdata       the backing StructureData array; may be null.
   *
  public ArrayStructureMA(StructureMembers members, Index ima, int nelems, StructureData[] sdata) {
    super(members, ima);
    this.nelems = nelems;
    this.sdata = sdata;
  } */

  /** Return backing storage in the StructureMembers */
  public Object getStorage() { return members; }

  public double getScalarDouble(int recnum, StructureMembers.Member m) {
    if (m.getDataType() != DataType.DOUBLE) throw new IllegalArgumentException("Type is "+m.getDataType()+", must be double");
    Array data = (Array) m.getDataObject();
    return data.getDouble( recnum * m.getSize()); // gets first one in the array
  }

  public double[] getJavaArrayDouble(int recnum, StructureMembers.Member m) {
    if (m.getDataType() != DataType.DOUBLE) throw new IllegalArgumentException("Type is "+m.getDataType()+", must be double");
    int count = m.getSize();
    Array data = (Array) m.getDataObject();
    double[] pa = new double[count];
    for (int i=0; i<count; i++)
      pa[i] = data.getDouble( recnum * count + i);
    return pa;
  }

  public float getScalarFloat(int recnum, StructureMembers.Member m) {
    if (m.getDataType() != DataType.FLOAT) throw new IllegalArgumentException("Type is "+m.getDataType()+", must be float");
    Array data = (Array) m.getDataObject();
    return data.getFloat( recnum * m.getSize()); // gets first one in the array
  }

  public float[] getJavaArrayFloat(int recnum, StructureMembers.Member m) {
    if (m.getDataType() != DataType.FLOAT) throw new IllegalArgumentException("Type is "+m.getDataType()+", must be float");
    int count = m.getSize();
    Array data = (Array) m.getDataObject();
    float[] pa = new float[count];
    for (int i=0; i<count; i++)
      pa[i] = data.getFloat( recnum * count + i);
    return pa;
  }

  public byte getScalarByte(int recnum, StructureMembers.Member m) {
    if (m.getDataType() != DataType.BYTE) throw new IllegalArgumentException("Type is "+m.getDataType()+", must be byte");
    Array data = (Array) m.getDataObject();
    return data.getByte( recnum * m.getSize()); // gets first one in the array
  }

  public byte[] getJavaArrayByte(int recnum, StructureMembers.Member m) {
    if (m.getDataType() != DataType.BYTE) throw new IllegalArgumentException("Type is "+m.getDataType()+", must be byte");
    int count = m.getSize();
    Array data = (Array) m.getDataObject();
    byte[] pa = new byte[count];
    for (int i=0; i<count; i++)
      pa[i] = data.getByte( recnum * count + i);
    return pa;
  }

  public short getScalarShort(int recnum, StructureMembers.Member m) {
    if (m.getDataType() != DataType.SHORT) throw new IllegalArgumentException("Type is "+m.getDataType()+", must be short");
    Array data = (Array) m.getDataObject();
    return data.getShort( recnum * m.getSize()); // gets first one in the array
  }

  public short[] getJavaArrayShort(int recnum, StructureMembers.Member m) {
    if (m.getDataType() != DataType.SHORT) throw new IllegalArgumentException("Type is "+m.getDataType()+", must be short");
    int count = m.getSize();
    Array data = (Array) m.getDataObject();
    short[] pa = new short[count];
    for (int i=0; i<count; i++)
      pa[i] = data.getShort( recnum * count + i);
    return pa;
  }

  public int getScalarInt(int recnum, StructureMembers.Member m) {
    if (m.getDataType() != DataType.INT) throw new IllegalArgumentException("Type is "+m.getDataType()+", must be int");
    Array data = (Array) m.getDataObject();
    return data.getInt( recnum * m.getSize()); // gets first one in the array
  }

  public int[] getJavaArrayInt(int recnum, StructureMembers.Member m) {
    if (m.getDataType() != DataType.INT) throw new IllegalArgumentException("Type is "+m.getDataType()+", must be int");
    int count = m.getSize();
    Array data = (Array) m.getDataObject();
    int[] pa = new int[count];
    for (int i=0; i<count; i++)
      pa[i] = data.getInt( recnum * count + i);
    return pa;
  }

  public long getScalarLong(int recnum, StructureMembers.Member m) {
    if (m.getDataType() != DataType.LONG) throw new IllegalArgumentException("Type is "+m.getDataType()+", must be long");
    Array data = (Array) m.getDataObject();
    return data.getLong( recnum * m.getSize()); // gets first one in the array
  }

  public long[] getJavaArrayLong(int recnum, StructureMembers.Member m) {
    if (m.getDataType() != DataType.LONG) throw new IllegalArgumentException("Type is "+m.getDataType()+", must be long");
    int count = m.getSize();
    Array data = (Array) m.getDataObject();
    long[] pa = new long[count];
    for (int i=0; i<count; i++)
      pa[i] = data.getLong( recnum * count + i);
    return pa;
  }

  public char getScalarChar(int recnum, StructureMembers.Member m) {
    if (m.getDataType() != DataType.CHAR) throw new IllegalArgumentException("Type is "+m.getDataType()+", must be char");
    Array data = (Array) m.getDataObject();
    return data.getChar( recnum * m.getSize()); // gets first one in the array
  }

  public char[] getJavaArrayChar(int recnum, StructureMembers.Member m) {
    if (m.getDataType() != DataType.CHAR) throw new IllegalArgumentException("Type is "+m.getDataType()+", must be char");
    int count = m.getSize();
    Array data = (Array) m.getDataObject();
    char[] pa = new char[count];
    for (int i=0; i<count; i++)
      pa[i] = data.getChar( recnum * count + i);
    return pa;
  }

  public String getScalarString(int recnum, StructureMembers.Member m) {
    if (m.getDataType() == DataType.CHAR) {
      ArrayChar data = (ArrayChar) m.getDataObject();
      return data.getString( recnum);
    }

    if (m.getDataType() == DataType.STRING) {
      ArrayObject data = (ArrayObject) m.getDataObject();
      return (String) data.getObject( recnum);
    }

    throw new IllegalArgumentException("Type is " + m.getDataType() + ", must be String or char");
  }

  public String[] getJavaArrayString(int recnum, StructureMembers.Member m) {
    int n = m.getSize();
    String[] result = new String[n];

    if (m.getDataType() == DataType.CHAR) {

      ArrayChar data = (ArrayChar) m.getDataObject();
      for (int i=0; i<n; i++)
        result[i] = data.getString( recnum * n + i);
      return result;

    } else if (m.getDataType() == DataType.STRING) {

      Array data = (Array) m.getDataObject();
      for (int i=0; i<n; i++)
        result[i] = (String) data.getObject( recnum * n + i);
      return result;
    }

    throw new IllegalArgumentException("Type is " + m.getDataType() + ", must be String or char");
  }

  public StructureData getScalarStructure(int recnum, StructureMembers.Member m) {
    if (m.getDataType() == DataType.STRUCTURE) {
      ArrayStructure data = (ArrayStructure) m.getDataObject();
      return data.getStructureData( recnum * m.getSize());  // gets first in the array
    }

    throw new IllegalArgumentException("Type is " + m.getDataType() + ", must be Structure");
  }

  public ArrayStructure getArrayStructure(int recnum, StructureMembers.Member m) {
    if (m.getDataType() == DataType.STRUCTURE) {
      ArrayStructure array = (ArrayStructure) m.getDataObject();

      // we need to subset this array structure to deal with just the subset for this recno
      // use "brute force" for now, see if we can finesse later

      StructureData[] this_sdata;
      int shape[];

      if (array instanceof ArraySequence) {
        ArraySequence arraySeq = (ArraySequence) array;
        int count = arraySeq.getSequenceLength(recnum);
        int start = arraySeq.getSequenceOffset(recnum);
        this_sdata = new StructureData[count];
        for (int i=0; i<count; i++)
          this_sdata[i] = arraySeq.makeStructureData( arraySeq, start + i);
        shape = new int[] {count};

      } else {
        int count = m.getSize();
        this_sdata = new StructureData[count];
        for (int i=0; i<count; i++)
          this_sdata[i] = array.getStructureData( recnum * count + i);
        shape = m.getShape();
      }

      return new ArrayStructureW( array.getStructureMembers(), shape, this_sdata);
    }

    throw new IllegalArgumentException("Type is " + m.getDataType() + ", must be Structure");
  }

}