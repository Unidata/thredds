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

/**
 * Handles nested sequences: a 1D array of variable length 1D arrays of StructureData.
 * Uses same technique as ArrayStructureMA for the inner fields; data storage is in member arrays.
 *
 *
 * Example use:
 * <pre>
    ArraySequence aseq = new ArraySequence( members, outerLength);
    for (int seq=0; seq < outerLength; seq++) {
      aseq.setSequenceLength(seq, seqLength);
    }
    aseq.finish();
 </pre>
 *
 * @author caron
 */
public class ArraySequence extends ArrayStructure {
  private int[] sequenceLen;
  private int[] sequenceOffset;
  private int total = 0;

  /**
   * This is used for inner sequences, ie variable length structures nested inside of another structure.
   * @param members the members of the STructure
   * @param nseq the number of sequences, ie the length of the outer structure.
   */
  public ArraySequence(StructureMembers members, int nseq) {
    super(members, new int[] {nseq});
    sequenceLen = new int[nseq];
  }

  // not sure how this is used
  protected StructureData makeStructureData( ArrayStructure as, int index) {
    return new StructureDataA( as, index);
  }

  public StructureData getStructureData(int index) {
    return new StructureDataA( this, index);
  }

  /**
   * Set the length of one of the sequences.
   * @param outerIndex which sequence?
   * @param len what is its length?
   */
  public void setSequenceLength( int outerIndex, int len) {
    sequenceLen[outerIndex] = len;
  }

  /**
   * Get the length of the ith sequence.
   * @param outerIndex which sequence?
   * @return its length
   */
  public int getSequenceLength( int outerIndex) {
    return sequenceLen[outerIndex];
  }

  /**
   * Get the the starting index of the ith sequence.
   * @param outerIndex which sequence?
   * @return its starting index
   */
  public int getSequenceOffset( int outerIndex) {
    return sequenceOffset[outerIndex];
  }

  /**
   * Call this when you have set all the sequence lengths.
   */
  public void finish() {
    sequenceOffset = new int[nelems];

    total = 0;
    for (int i=0; i<nelems; i++) {
      sequenceOffset[i] = total;
      total += sequenceLen[i];
    }

    sdata = new StructureData[nelems];
    for (int i=0; i<nelems; i++)
      sdata[i] = new StructureDataA( this, sequenceOffset[i]);

    // make the member arrays
    for (StructureMembers.Member m : members.getMembers()) {
      int[] mShape = m.getShape();
      int[] shape = new int[mShape.length + 1];
      shape[0] = total;
      for (int i = 0; i < mShape.length; i++)
        shape[i + 1] = mShape[i];

      // LOOK not doing nested structures
      Array data = Array.factory(m.getDataType(), shape);
      m.setDataArray(data);
    }
  }

  /**
   * @return the total number of Structures over all the nested sequences.
   */
  public int getTotalNumberOfStructures() { return total; }

  /**
   * Flatten the Structures into a 1D array of Structures of length getTotalNumberOfStructures().
   * @return Array of Structures
   */
  public ArrayStructure flatten() {
    ArrayStructureW aw = new ArrayStructureW( getStructureMembers(), new int[] {total});
    for (int i=0; i<total; i++) {
      StructureData sdata = new StructureDataA( this, i);
      aw.setStructureData(sdata, i);
    }
    return aw;
  }

  public double getScalarDouble(int recnum, StructureMembers.Member m) {
    if (m.getDataType() != DataType.DOUBLE) throw new IllegalArgumentException("Type is "+m.getDataType()+", must be double");
    Array data = (Array) m.getDataArray();
    return data.getDouble( recnum * m.getSize()); // gets first one in the array
  }

  public double[] getJavaArrayDouble(int recnum, StructureMembers.Member m) {
    if (m.getDataType() != DataType.DOUBLE) throw new IllegalArgumentException("Type is "+m.getDataType()+", must be double");
    int count = m.getSize();
    Array data = (Array) m.getDataArray();
    double[] pa = new double[count];
    for (int i=0; i<count; i++)
      pa[i] = data.getDouble( recnum * count + i);
    return pa;
  }

  public float getScalarFloat(int recnum, StructureMembers.Member m) {
    if (m.getDataType() != DataType.FLOAT) throw new IllegalArgumentException("Type is "+m.getDataType()+", must be float");
    Array data = (Array) m.getDataArray();
    return data.getFloat( recnum * m.getSize()); // gets first one in the array
  }

  public float[] getJavaArrayFloat(int recnum, StructureMembers.Member m) {
    if (m.getDataType() != DataType.FLOAT) throw new IllegalArgumentException("Type is "+m.getDataType()+", must be float");
    int count = m.getSize();
    Array data = (Array) m.getDataArray();
    float[] pa = new float[count];
    for (int i=0; i<count; i++)
      pa[i] = data.getFloat( recnum * count + i);
    return pa;
  }

  public byte getScalarByte(int recnum, StructureMembers.Member m) {
    if (m.getDataType() != DataType.BYTE) throw new IllegalArgumentException("Type is "+m.getDataType()+", must be byte");
    Array data = (Array) m.getDataArray();
    return data.getByte( recnum * m.getSize()); // gets first one in the array
  }

  public byte[] getJavaArrayByte(int recnum, StructureMembers.Member m) {
    if (m.getDataType() != DataType.BYTE) throw new IllegalArgumentException("Type is "+m.getDataType()+", must be byte");
    int count = m.getSize();
    Array data = (Array) m.getDataArray();
    byte[] pa = new byte[count];
    for (int i=0; i<count; i++)
      pa[i] = data.getByte( recnum * count + i);
    return pa;
  }

  public short getScalarShort(int recnum, StructureMembers.Member m) {
    if (m.getDataType() != DataType.SHORT) throw new IllegalArgumentException("Type is "+m.getDataType()+", must be short");
    Array data = (Array) m.getDataArray();
    return data.getShort( recnum * m.getSize()); // gets first one in the array
  }

  public short[] getJavaArrayShort(int recnum, StructureMembers.Member m) {
    if (m.getDataType() != DataType.SHORT) throw new IllegalArgumentException("Type is "+m.getDataType()+", must be short");
    int count = m.getSize();
    Array data = (Array) m.getDataArray();
    short[] pa = new short[count];
    for (int i=0; i<count; i++)
      pa[i] = data.getShort( recnum * count + i);
    return pa;
  }

  public int getScalarInt(int recnum, StructureMembers.Member m) {
    if (m.getDataType() != DataType.INT) throw new IllegalArgumentException("Type is "+m.getDataType()+", must be int");
    Array data = (Array) m.getDataArray();
    return data.getInt( recnum * m.getSize()); // gets first one in the array
  }

  public int[] getJavaArrayInt(int recnum, StructureMembers.Member m) {
    if (m.getDataType() != DataType.INT) throw new IllegalArgumentException("Type is "+m.getDataType()+", must be int");
    int count = m.getSize();
    Array data = (Array) m.getDataArray();
    int[] pa = new int[count];
    for (int i=0; i<count; i++)
      pa[i] = data.getInt( recnum * count + i);
    return pa;
  }

  public long getScalarLong(int recnum, StructureMembers.Member m) {
    if (m.getDataType() != DataType.LONG) throw new IllegalArgumentException("Type is "+m.getDataType()+", must be long");
    Array data = (Array) m.getDataArray();
    return data.getLong( recnum * m.getSize()); // gets first one in the array
  }

  public long[] getJavaArrayLong(int recnum, StructureMembers.Member m) {
    if (m.getDataType() != DataType.LONG) throw new IllegalArgumentException("Type is "+m.getDataType()+", must be long");
    int count = m.getSize();
    Array data = (Array) m.getDataArray();
    long[] pa = new long[count];
    for (int i=0; i<count; i++)
      pa[i] = data.getLong( recnum * count + i);
    return pa;
  }

  public char getScalarChar(int recnum, StructureMembers.Member m) {
    if (m.getDataType() != DataType.CHAR) throw new IllegalArgumentException("Type is "+m.getDataType()+", must be char");
    Array data = (Array) m.getDataArray();
    return data.getChar( recnum * m.getSize()); // gets first one in the array
  }

  public char[] getJavaArrayChar(int recnum, StructureMembers.Member m) {
    if (m.getDataType() != DataType.CHAR) throw new IllegalArgumentException("Type is "+m.getDataType()+", must be char");
    int count = m.getSize();
    Array data = (Array) m.getDataArray();
    char[] pa = new char[count];
    for (int i=0; i<count; i++)
      pa[i] = data.getChar( recnum * count + i);
    return pa;
  }

  public String getScalarString(int recnum, StructureMembers.Member m) {
    if (m.getDataType() == DataType.CHAR) {
      ArrayChar data = (ArrayChar) m.getDataArray();
      return data.getString( recnum);
    }

    if (m.getDataType() == DataType.STRING) {
      ArrayObject data = (ArrayObject) m.getDataArray();
      return (String) data.getObject( recnum);
    }

    throw new IllegalArgumentException("Type is " + m.getDataType() + ", must be String or char");
  }

  public String[] getJavaArrayString(int recnum, StructureMembers.Member m) {
    int n = m.getSize();
    String[] result = new String[n];

    if (m.getDataType() == DataType.CHAR) {

      ArrayChar data = (ArrayChar) m.getDataArray();
      for (int i=0; i<n; i++)
        result[i] = data.getString( recnum * n + i);
      return result;

    } else if (m.getDataType() == DataType.STRING) {

      Array data = (Array) m.getDataArray();
      for (int i=0; i<n; i++)
        result[i] = (String) data.getObject( recnum * n + i);
      return result;
    }

    throw new IllegalArgumentException("Type is " + m.getDataType() + ", must be String or char");
  }

  public StructureData getScalarStructure(int recnum, StructureMembers.Member m) {
    if (m.getDataType() == DataType.STRUCTURE) {
      ArrayStructure data = (ArrayStructure) m.getDataArray();
      return data.getStructureData( recnum * m.getSize());  // gets first in the array
    }

    throw new IllegalArgumentException("Type is " + m.getDataType() + ", must be Structure");
  }

  public ArrayStructure getArrayStructure(int recnum, StructureMembers.Member m) {
    if (m.getDataType() == DataType.STRUCTURE) {
      ArrayStructure data = (ArrayStructure) m.getDataArray();
      // we need to subset this array structure to deal with just the subset for this recno
      // use "brute force" for now, see if we can finesse later
      int count = m.getSize();
      StructureData[] sdata = new StructureData[count];
      for (int i=0; i<count; i++)
        sdata[i] = data.getStructureData( recnum * count + i);

      return new ArrayStructureW( data.getStructureMembers(), m.getShape(), sdata);
    }

    throw new IllegalArgumentException("Type is " + m.getDataType() + ", must be Structure");
  }

}