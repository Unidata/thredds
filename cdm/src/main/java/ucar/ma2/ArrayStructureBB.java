/*
 * Copyright 1997-2007 Unidata Program Center/University Corporation for
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

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;
import java.util.ArrayList;

/**
 * Concrete implementation of ArrayStructure, data storage is in a ByteBuffer, which is converted to member data on the fly.
 * In order to use this, the records must have the same size, and the member offset must be the same for each record.
 * Use StructureMembers.setStructureSize() to set the record size.
 * Use StructureMembers.Member.setDataParam() to set the offset of the member from the start of each record.
 * The member data will then be located in the BB at offset = recnum * getStructureSize() + member.getDataParam().
 * This defers object creation for efficiency. Use getArray<type>() and getScalar<type>() data accessors if possible.
 * <pre>
     Structure pdata = (Structure) ncfile.findVariable( name);
     StructureMembers members = pdata.makeStructureMembers();
     members.findMember("value").setDataParam(0); // these are the offsets into the record
     members.findMember("x_start").setDataParam(2);
     members.findMember("y_start").setDataParam(4);
     members.findMember("direction").setDataParam(6);
     members.findMember("speed").setDataParam(8);
     int recsize = pos[1] - pos[0]; // each record  must be all the same size
     members.setStructureSize( recsize);
     ArrayStructureBB asbb = new ArrayStructureBB( members, new int[] { size}, bos, pos[0]);
 * </pre>
 * String members must store the Strings in the stringHeap. An integer index into the heap is used in the ByteBuffer.
 * @author caron
 * @see Array
 */
public class ArrayStructureBB extends ArrayStructure {

  protected ByteBuffer bbuffer;
  protected int bb_offset = 0;

  /**
   * Create a new Array of type StructureData and the given members and shape.
   * Generally, you extract the byte array and fill it: <pre>
     byte [] result = (byte []) structureArray.getStorage(); </pre>
   *
   * @param members a description of the structure members
   * @param shape   the shape of the Array.
   */
  public ArrayStructureBB(StructureMembers members, int[] shape) {
    super(members, shape);
    this.bbuffer = ByteBuffer.allocate(nelems * getStructureSize());
    bbuffer.order(ByteOrder.BIG_ENDIAN);
  }

  /**
   * Construct an ArrayStructureBB with the given ByteBuffer.
   *
   * @param members the list of structure members.
   * @param shape   the shape of the structure array
   * @param bbuffer the data is stored in this ByteBuffer. bbuffer.order must already be set.
   * @param offset  offset from the start of the ByteBufffer to the first record.
   */
  public ArrayStructureBB(StructureMembers members, int[] shape, ByteBuffer bbuffer, int offset) {
    super(members, shape);
    this.bbuffer = bbuffer;
    this.bb_offset = offset;
  }

  protected StructureData makeStructureData(ArrayStructure as, int index) {
    return new StructureDataA(as, index);
  }

  /**
   * LOOK doesnt work, because of the methods using recnum, not Index (!)
   * create new Array with given indexImpl and the same backing store
   *
   public Array createView(Index index) {
   return new ArrayStructureBB(members, index, nelems, sdata, bbuffer);
   }

   /**
   * Create a new Array using the given IndexArray and backing store.
   * used for sections, and factory.
   *
   * @param members     a description of the structure members
   * @param ima         use this IndexArray as the index
   * @param nelems      the total number of StructureData elements in the backing array
   * @param sdata       the backing StructureData array; may be null.
   * @param bbuffer     use this for the ByteBuffer storage.
   *
  public ArrayStructureBB(StructureMembers members, Index ima, int nelems, StructureData[] sdata, ByteBuffer bbuffer) {
  super(members, ima);
  this.nelems = nelems;
  this.sdata = sdata;
  this.bbuffer = bbuffer;
  }  */

  /**
   * Return backing storage as a byte[]
   * @return backing storage as a byte[]
   */
  public Object getStorage() {
    return bbuffer.array();
  }

  /**
   * Return backing storage as a ByteBuffer
   * @return backing storage as a ByteBuffer
   */
  public ByteBuffer getByteBuffer() {
    return bbuffer;
  }

  public double getScalarDouble(int recnum, StructureMembers.Member m) {
    if (m.getDataType() != DataType.DOUBLE)
      throw new IllegalArgumentException("Type is " + m.getDataType() + ", must be double");
    int offset = calcOffset(recnum, m);
    return bbuffer.getDouble(offset);
  }

  public double[] getJavaArrayDouble(int recnum, StructureMembers.Member m) {
    if (m.getDataType() != DataType.DOUBLE)
      throw new IllegalArgumentException("Type is " + m.getDataType() + ", must be double");
    int offset = calcOffset(recnum, m);
    int count = m.getSize();
    double[] pa = new double[count];
    for (int i = 0; i < count; i++)
      pa[i] = bbuffer.getDouble(offset + i * 8);
    return pa;
  }

  public float getScalarFloat(int recnum, StructureMembers.Member m) {
    if (m.getDataType() != DataType.FLOAT)
      throw new IllegalArgumentException("Type is " + m.getDataType() + ", must be float");
    int offset = calcOffset(recnum, m);
    return bbuffer.getFloat(offset);
  }

  public float[] getJavaArrayFloat(int recnum, StructureMembers.Member m) {
    if (m.getDataType() != DataType.FLOAT)
      throw new IllegalArgumentException("Type is " + m.getDataType() + ", must be float");
    int offset = calcOffset(recnum, m);
    int count = m.getSize();
    float[] pa = new float[count];
    for (int i = 0; i < count; i++)
      pa[i] = bbuffer.getFloat(offset + i * 4);
    return pa;
  }

  public byte getScalarByte(int recnum, StructureMembers.Member m) {
    if (m.getDataType() != DataType.BYTE)
      throw new IllegalArgumentException("Type is " + m.getDataType() + ", must be byte");
    int offset = calcOffset(recnum, m);
    return bbuffer.get(offset);
  }

  public byte[] getJavaArrayByte(int recnum, StructureMembers.Member m) {
    if (m.getDataType() != DataType.BYTE)
      throw new IllegalArgumentException("Type is " + m.getDataType() + ", must be byte");
    int offset = calcOffset(recnum, m);
    int count = m.getSize();
    byte[] pa = new byte[count];
    for (int i = 0; i < count; i++)
      pa[i] = bbuffer.get(offset + i);
    return pa;
  }

  public short getScalarShort(int recnum, StructureMembers.Member m) {
    if (m.getDataType() != DataType.SHORT)
      throw new IllegalArgumentException("Type is " + m.getDataType() + ", must be short");
    int offset = calcOffset(recnum, m);
    return bbuffer.getShort(offset);
  }

  public short[] getJavaArrayShort(int recnum, StructureMembers.Member m) {
    if (m.getDataType() != DataType.SHORT)
      throw new IllegalArgumentException("Type is " + m.getDataType() + ", must be short");
    int offset = calcOffset(recnum, m);
    int count = m.getSize();
    short[] pa = new short[count];
    for (int i = 0; i < count; i++)
      pa[i] = bbuffer.getShort(offset + i * 2);
    return pa;
  }

  public int getScalarInt(int recnum, StructureMembers.Member m) {
    if (m.getDataType() != DataType.INT)
      throw new IllegalArgumentException("Type is " + m.getDataType() + ", must be int");
    int offset = calcOffset(recnum, m);
    return bbuffer.getInt(offset);
  }

  public int[] getJavaArrayInt(int recnum, StructureMembers.Member m) {
    if (m.getDataType() != DataType.INT)
      throw new IllegalArgumentException("Type is " + m.getDataType() + ", must be int");
    int offset = calcOffset(recnum, m);
    int count = m.getSize();
    int[] pa = new int[count];
    for (int i = 0; i < count; i++)
      pa[i] = bbuffer.getInt(offset + i * 4);
    return pa;
  }

  public long getScalarLong(int recnum, StructureMembers.Member m) {
    if (m.getDataType() != DataType.LONG)
      throw new IllegalArgumentException("Type is " + m.getDataType() + ", must be long");
    int offset = calcOffset(recnum, m);
    return bbuffer.getLong(offset);
  }

  public long[] getJavaArrayLong(int recnum, StructureMembers.Member m) {
    if (m.getDataType() != DataType.LONG)
      throw new IllegalArgumentException("Type is " + m.getDataType() + ", must be long");
    int offset = calcOffset(recnum, m);
    int count = m.getSize();
    long[] pa = new long[count];
    for (int i = 0; i < count; i++)
      pa[i] = bbuffer.getLong(offset + i * 8);
    return pa;
  }

  public char getScalarChar(int recnum, StructureMembers.Member m) {
    if (m.getDataType() != DataType.CHAR)
      throw new IllegalArgumentException("Type is " + m.getDataType() + ", must be char");
    int offset = calcOffset(recnum, m);
    return (char) bbuffer.get(offset);
  }

  public char[] getJavaArrayChar(int recnum, StructureMembers.Member m) {
    if (m.getDataType() != DataType.CHAR)
      throw new IllegalArgumentException("Type is " + m.getDataType() + ", must be char");
    int offset = calcOffset(recnum, m);
    int count = m.getSize();
    char[] pa = new char[count];
    for (int i = 0; i < count; i++)
      pa[i] = (char) bbuffer.get(offset + i);
    return pa;
  }

  public String getScalarString(int recnum, StructureMembers.Member m) {

    if (m.getDataType() == DataType.STRING) {
      int offset = calcOffset(recnum, m);
      int index = bbuffer.getInt(offset);
      return stringHeap.get(index);
    }

    if (m.getDataType() == DataType.CHAR) {
      int offset = calcOffset(recnum, m);
      int count = m.getSize();
      byte[] pa = new byte[count];
      int i;
      for (i = 0; i < count; i++) {
        pa[i] = bbuffer.get(offset + i);
        if (0 == pa[i]) break;
      }
      return new String(pa, 0, i);
    }

    throw new IllegalArgumentException("Type is " + m.getDataType() + ", must be String or char");
  }

  public String[] getJavaArrayString(int recnum, StructureMembers.Member m) {
    if (m.getDataType() == DataType.STRING) {
      int n = m.getSize();
      int offset = calcOffset(recnum, m);
      String[] result = new String[n];
      for (int i = 0; i < n; i++) {
        int index = bbuffer.getInt(offset + i*4);
        result[i] = stringHeap.get(index);
      }
      return result;
    }

    if (m.getDataType() == DataType.CHAR) {
      int[] shape = m.getShape();
      int rank = shape.length;
      if (rank < 2) {
        String[] result = new String[1];
        result[0] = getScalarString(recnum, m);
        return result;
      }

      int strlen = shape[rank - 1];
      int n = m.getSize() / strlen;
      int offset = calcOffset(recnum, m);
      String[] result = new String[n];
      for (int i = 0; i < n; i++) {
        byte[] bytes = new byte[strlen];
        for (int j = 0; j < bytes.length; j++)
          bytes[j] = bbuffer.get(offset + i * strlen + j);
        result[i] = new String(bytes);
      }
      return result;
    }

    throw new IllegalArgumentException("Type is " + m.getDataType() + ", must be char");
  }

  // LOOK - has not been tested !!!
  public StructureData getScalarStructure(int recnum, StructureMembers.Member m) {
    if (m.getDataType() != DataType.STRUCTURE)
      throw new IllegalArgumentException("Type is " + m.getDataType() + ", must be Structure");
    int offset = calcOffset(recnum, m);
    ArrayStructureBB subset = new ArrayStructureBB(m.getStructureMembers(), new int[]{1}, this.bbuffer, offset);

    return new StructureDataA(subset, 0);
  }

  public ArrayStructure getArrayStructure(int recnum, StructureMembers.Member m) {
    if (m.getDataType() != DataType.STRUCTURE)
      throw new IllegalArgumentException("Type is " + m.getDataType() + ", must be Structure");
    int offset = calcOffset(recnum, m);
    return new ArrayStructureBB(m.getStructureMembers(), m.getShape(), this.bbuffer, offset);
  }

  protected int calcOffset(int recnum, StructureMembers.Member m) {
    return bb_offset + recnum * getStructureSize() + m.getDataParam();
  }

  private List<String> stringHeap = new ArrayList<String>();
  public int addStringToHeap(String s) {
    stringHeap.add(s);
    return stringHeap.size() - 1;
  }

  // debugging
  public static void main(String argv[]) {
    byte[] ba = new byte[20];
    for (int i = 0; i < ba.length; ++i)
      ba[i] = (byte) i;

    ByteBuffer bbw = ByteBuffer.wrap(ba, 5, 15);
    bbw.get(0);
    System.out.println(" bbw(0)=" + bbw.get(0) + " i would expect = 5");

    bbw.position(5);
    System.out.println(" bbw(0)=" + bbw.get(0) + " i would expect = 4");
  }

}