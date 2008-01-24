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
 * Superclass for implementations of Array of StructureData.
 *
 * @author caron
 * @see Array
 */
public abstract class ArrayStructure extends Array {
  protected StructureMembers members;
  protected int nelems;
  protected StructureData[] sdata;

  /**
   * Create a new Array of type StructureData and the given members and shape.
   * dimensions.length determines the rank of the new Array.
   *
   * @param members a description of the structure members
   * @param shape       the shape of the Array.
   */
  public ArrayStructure(StructureMembers members, int[] shape) {
    super(shape);
    this.members = members;
    this.nelems = (int) indexCalc.getSize();
  }

  // for subclasses to create views
  protected ArrayStructure(StructureMembers members, Index ima) {
    super(ima);
    this.members = members;
    this.nelems = (int) indexCalc.getSize();
  }

  // copy from javaArray to storage using the iterator: used by factory( Object);
  void copyFrom1DJavaArray(IndexIterator iter, Object javaArray) {
    Object[] ja = (Object[]) javaArray;
    for (Object aJa : ja)
      iter.setObjectNext(aJa);
  }

  // copy to javaArray from storage using the iterator: used by copyToNDJavaArray;
  void copyTo1DJavaArray(IndexIterator iter, Object javaArray) {
    Object[] ja = (Object[]) javaArray;
    for (int i = 0; i < ja.length; i++)
      ja[i] = iter.getObjectNext();
  }

  public Class getElementType() { return StructureData.class; }

  /**
   * Get the structure members.
   * @return the structure members.
   */
  public StructureMembers getStructureMembers() { return members; }

   /**
   * Get the index-th StructureData of this ArrayStructure.
   * @param i which one to get, specified by an Index.
   * @return object of type StructureData.
   */
  public Object getObject(Index i) {
    return getObject(i.currentElement());
  }

  /**
   * Set one of the StructureData of this ArrayStructure.
   * @param i which one to set, specified by an Index.
   * @param value must be type StructureData.
   */
  public void setObject(Index i, Object value) {
    setObject(i.currentElement(),  value);
  }

   /**
   * Get the index-th StructureData of this ArrayStructure.
   * @param index which one to get, specified by an integer.
   * @return object of type StructureData.
   */
  Object getObject(int index) {
    return getStructureData(index);
  }

  /**
   * Set the index-th StructureData of this ArrayStructure.
   * @param index which one to set.
   * @param value must be type StructureData.
   */
  void setObject(int index, Object value) {
    if (sdata == null)
      sdata = new StructureData[nelems];
    sdata[index] = (StructureData) value;
  }

   /**
   * Get the index-th StructureData of this ArrayStructure.
   * @param i which one to get, specified by an Index.
   * @return object of type StructureData.
   */
  public StructureData getStructureData(Index i) {
    return getStructureData(i.currentElement());
  }

  /**
   * Get the index-th StructureData of this ArrayStructure.
   * @param index which one to get, specified by an integer.
   * @return object of type StructureData.
   */
  public StructureData getStructureData(int index) {
    if (sdata == null)
      sdata = new StructureData[nelems];
    if (index >= sdata.length)
      throw new IllegalArgumentException(index+" > "+sdata.length);
    if (sdata[index] == null)
      sdata[index] = makeStructureData( this, index);
    return sdata[index];
  }

  public Object getStorage() {
    // this fills the sdata array
    for (int i=0; i<nelems; i++)
      getStructureData(i);
    return sdata;
  }

  abstract protected StructureData makeStructureData( ArrayStructure as, int index);

  /**
   * Get the size of each StructureData object in bytes.
   * @return the size of each StructureData object in bytes.
   */
  public int getStructureSize() {
    return members.getStructureSize();
  }

  ///////////////////////////////////////////////////////////////////////////////

  /**
   * Get member data of any type for a specific record as an Array.
   * @param recno get data from the recnum-th StructureData of the ArrayStructure. Must be less than getSize();
   * @param m get data from this StructureMembers.Member.
   * @return Array values.
   */
  public Array getArray(int recno, StructureMembers.Member m) {
    DataType dataType = m.getDataType();

    if (dataType == DataType.DOUBLE) {
      double[] pa = getJavaArrayDouble(recno, m);
      return Array.factory( double.class, m.getShape(), pa);

    } else if (dataType == DataType.FLOAT) {
      float[] pa = getJavaArrayFloat(recno, m);
      return Array.factory( float.class, m.getShape(), pa);

    } else if ((dataType == DataType.BYTE) || (dataType == DataType.OPAQUE)) {
      byte[] pa = getJavaArrayByte(recno, m);
      return Array.factory( byte.class, m.getShape(), pa);

    } else if (dataType == DataType.SHORT) {
      short[] pa = getJavaArrayShort(recno, m);
      return Array.factory( short.class, m.getShape(), pa);

    } else if (dataType == DataType.INT) {
      int[] pa = getJavaArrayInt(recno, m);
      return Array.factory( int.class, m.getShape(), pa);

    } else if (dataType == DataType.LONG) {
      long[] pa = getJavaArrayLong(recno, m);
      return Array.factory( long.class, m.getShape(), pa);

    } else if (dataType == DataType.CHAR) {
      char[] pa = getJavaArrayChar(recno, m);
      return Array.factory( char.class, m.getShape(), pa);

    } else if (dataType == DataType.STRING) {
      String[] pa = getJavaArrayString(recno, m);
      return Array.factory( String.class, m.getShape(), pa);

    } else if (dataType == DataType.STRUCTURE) {
      return getArrayStructure(recno, m);
    }

    throw new RuntimeException("Dont have implemenation for "+dataType);
  }

  /**
   * Set data for one member, over all structures.
   * This is used by VariableDS to do scale/offset.
   * @param m set data for this StructureMembers.Member.
   * @param memberArray Array values.
   */
  public void setMemberArray(StructureMembers.Member m, Array memberArray) {
    m.setDataArray(memberArray);
  }

  /**
   * Extract data for one member, over all structures.
   * @param m get data from this StructureMembers.Member.
   * @return Array values.
   */
  public Array getMemberArray(StructureMembers.Member m) {
    if (m.getDataArray() != null)
      return (Array) m.getDataArray();

    DataType dataType = m.getDataType();

    // combine the shapes
    int[] mshape = m.getShape();
    int rrank = rank + mshape.length;
    int[] rshape = new int[rrank];
    System.arraycopy(getShape(), 0, rshape, 0, rank);
    for (int i=0; i<mshape.length; i++)
      rshape[i+rank] = mshape[i];

    // create an empty array
    Array result = Array.factory( dataType.getPrimitiveClassType(), rshape);
    IndexIterator resultIter = result.getIndexIterator();

    if (dataType == DataType.DOUBLE) {
      for (int recno=0; recno<getSize(); recno++)
        copyDoubles(recno, m, resultIter);

    } else if (dataType == DataType.FLOAT) {
      for (int recno=0; recno<getSize(); recno++)
        copyFloats(recno, m, resultIter);

    } else if ((dataType == DataType.BYTE) || (dataType == DataType.OPAQUE)) {
      for (int recno=0; recno<getSize(); recno++)
        copyBytes(recno, m, resultIter);

    } else if (dataType == DataType.SHORT) {
      for (int recno=0; recno<getSize(); recno++)
        copyShorts(recno, m, resultIter);

    } else if (dataType == DataType.INT) {
      for (int recno=0; recno<getSize(); recno++)
        copyInts(recno, m, resultIter);

    } else if (dataType == DataType.LONG) {
      for (int recno=0; recno<getSize(); recno++)
        copyLongs(recno, m, resultIter);

    } else if (dataType == DataType.CHAR) {
      for (int recno=0; recno<getSize(); recno++)
        copyChars(recno, m, resultIter);

    } else if (dataType == DataType.STRING) {
      for (int recno=0; recno<getSize(); recno++)
        copyStrings(recno, m, resultIter);

    } else if (dataType == DataType.STRUCTURE) {
      for (int recno=0; recno<getSize(); recno++)
        copyStructures(recno, m, resultIter);
   }

    return result;
  }

  protected void copyChars(int recnum, StructureMembers.Member m, IndexIterator result) {
    IndexIterator dataIter = getArray(recnum, m).getIndexIterator();
    while (dataIter.hasNext())
      result.setCharNext( dataIter.getCharNext());
  }
  protected void copyDoubles(int recnum, StructureMembers.Member m, IndexIterator result) {
    IndexIterator dataIter = getArray(recnum, m).getIndexIterator();
    while (dataIter.hasNext())
      result.setDoubleNext( dataIter.getDoubleNext());
  }
  protected void copyFloats(int recnum, StructureMembers.Member m, IndexIterator result) {
    IndexIterator dataIter = getArray(recnum, m).getIndexIterator();
    while (dataIter.hasNext())
      result.setFloatNext( dataIter.getFloatNext());
  }
  protected void copyBytes(int recnum, StructureMembers.Member m, IndexIterator result) {
    IndexIterator dataIter = getArray(recnum, m).getIndexIterator();
    while (dataIter.hasNext())
      result.setByteNext( dataIter.getByteNext());
  }
  protected void copyShorts(int recnum, StructureMembers.Member m, IndexIterator result) {
    IndexIterator dataIter = getArray(recnum, m).getIndexIterator();
    while (dataIter.hasNext())
      result.setShortNext( dataIter.getShortNext());
  }
  protected void copyInts(int recnum, StructureMembers.Member m, IndexIterator result) {
    IndexIterator dataIter = getArray(recnum, m).getIndexIterator();
    while (dataIter.hasNext())
      result.setIntNext( dataIter.getIntNext());
  }
  protected void copyLongs(int recnum, StructureMembers.Member m, IndexIterator result) {
    IndexIterator dataIter = getArray(recnum, m).getIndexIterator();
    while (dataIter.hasNext())
      result.setLongNext( dataIter.getLongNext());
  }
  protected void copyStrings(int recnum, StructureMembers.Member m, IndexIterator result) {
    IndexIterator dataIter = getArray(recnum, m).getIndexIterator();
    while (dataIter.hasNext())
      result.setObjectNext( dataIter.getObjectNext());
  }
  protected void copyStructures(int recnum, StructureMembers.Member m, IndexIterator result) {
    IndexIterator dataIter = getArray(recnum, m).getIndexIterator();
    while (dataIter.hasNext())
      result.setObjectNext( dataIter.getObjectNext()); // LOOK ??
  }

  /**
   * Get member data array of any type as an Object, eg, Float, Double, String, StructureData etc.
   * @param recno get data from the recnum-th StructureData of the ArrayStructure. Must be less than getSize();
   * @param m get data from this StructureMembers.Member.
   * @return value as Float, Double, etc..
   */
  public Object getScalarObject(int recno, StructureMembers.Member m) {
    DataType dataType = m.getDataType();

    if (dataType == DataType.DOUBLE) {
        return getScalarDouble(recno, m);

    } else if (dataType == DataType.FLOAT) {
      return getScalarFloat(recno, m);

    } else if ((dataType == DataType.BYTE) || (dataType == DataType.OPAQUE)) {
      return getScalarByte(recno, m);

    } else if (dataType == DataType.SHORT) {
      return getScalarShort(recno, m);

    } else if (dataType == DataType.INT) {
      return getScalarInt(recno, m);

    } else if (dataType == DataType.LONG) {
      return getScalarLong(recno, m);

    } else if (dataType == DataType.CHAR) {
      return getScalarString(recno, m);

    } else if (dataType == DataType.STRING) {
      return getScalarString(recno, m);

    } else if (dataType == DataType.STRUCTURE) {
      return getScalarStructure(recno, m);
    }

     throw new RuntimeException("Dont have implemenation for "+dataType);
  }

  /**
   * Get scalar member data of type double.
   * @param recnum get data from the recnum-th StructureData of the ArrayStructure. Must be less than getSize();
   * @param m get data from this StructureMembers.Member. Must be of type double.
   * @return scalar double value
   */
  public double getScalarDouble(int recnum, StructureMembers.Member m) {
    if (m.getDataType() != DataType.DOUBLE) throw new IllegalArgumentException("Type is "+m.getDataType()+", must be double");
    Array data = (Array) m.getDataArray();
    return data.getDouble( recnum * m.getSize()); // gets first one in the array
  }

  /**
   * Get member data of type double as a 1D array. The member data may be any rank.
   * @param recnum get data from the recnum-th StructureData of the ArrayStructure. Must be less than getSize();
   * @param m get data from this StructureMembers.Member. Must be of type double.
   * @return double[]
   */
  public double[] getJavaArrayDouble(int recnum, StructureMembers.Member m) {
    if (m.getDataType() != DataType.DOUBLE) throw new IllegalArgumentException("Type is "+m.getDataType()+", must be double");
    int count = m.getSize();
    Array data = m.getDataArray();
    double[] pa = new double[count];
    for (int i=0; i<count; i++)
      pa[i] = data.getDouble( recnum * count + i);
    return pa;
  }

  /**
   * Get scalar member data of type float.
   * @param recnum get data from the recnum-th StructureData of the ArrayStructure. Must be less than getSize();
   * @param m get data from this StructureMembers.Member. Must be of type float.
   * @return scalar double value
   */
  public float getScalarFloat(int recnum, StructureMembers.Member m) {
    if (m.getDataType() != DataType.FLOAT) throw new IllegalArgumentException("Type is "+m.getDataType()+", must be float");
    Array data = m.getDataArray();
    return data.getFloat( recnum * m.getSize()); // gets first one in the array
  }

  /**
   * Get member data of type float as a 1D array.
   * @param recnum get data from the recnum-th StructureData of the ArrayStructure. Must be less than getSize();
   * @param m get data from this StructureMembers.Member. Must be of type float.
   * @return float[]
   */
  public float[] getJavaArrayFloat(int recnum, StructureMembers.Member m) {
    if (m.getDataType() != DataType.FLOAT) throw new IllegalArgumentException("Type is "+m.getDataType()+", must be float");
    int count = m.getSize();
    Array data = m.getDataArray();
    float[] pa = new float[count];
    for (int i=0; i<count; i++)
      pa[i] = data.getFloat( recnum * count + i);
    return pa;
  }

  /**
   * Get scalar member data of type byte.
   * @param recnum get data from the recnum-th StructureData of the ArrayStructure. Must be less than getSize();
   * @param m get data from this StructureMembers.Member. Must be of type byte.
   * @return scalar double value
   */
  public byte getScalarByte(int recnum, StructureMembers.Member m) {
    if (m.getDataType() != DataType.BYTE) throw new IllegalArgumentException("Type is "+m.getDataType()+", must be byte");
    Array data = m.getDataArray();
    return data.getByte( recnum * m.getSize()); // gets first one in the array
  }

  /**
   * Get member data of type byte as a 1D array.
   * @param recnum get data from the recnum-th StructureData of the ArrayStructure. Must be less than getSize();
   * @param m get data from this StructureMembers.Member. Must be of type byte.
   * @return byte[]
   */
  public byte[] getJavaArrayByte(int recnum, StructureMembers.Member m) {
    if (m.getDataType() != DataType.BYTE) throw new IllegalArgumentException("Type is "+m.getDataType()+", must be byte");
    int count = m.getSize();
    Array data = m.getDataArray();
    byte[] pa = new byte[count];
    for (int i=0; i<count; i++)
      pa[i] = data.getByte( recnum * count + i);
    return pa;
  }

  /**
   * Get scalar member data of type short.
   * @param recnum get data from the recnum-th StructureData of the ArrayStructure. Must be less than getSize();
   * @param m get data from this StructureMembers.Member. Must be of type short.
   * @return scalar double value
   */
  public short getScalarShort(int recnum, StructureMembers.Member m) {
    if (m.getDataType() != DataType.SHORT) throw new IllegalArgumentException("Type is "+m.getDataType()+", must be short");
    Array data = m.getDataArray();
    return data.getShort( recnum * m.getSize()); // gets first one in the array
  }

  /**
   * Get member data of type short as a 1D array.
   * @param recnum get data from the recnum-th StructureData of the ArrayStructure. Must be less than getSize();
   * @param m get data from this StructureMembers.Member. Must be of type float.
   * @return short[]
   */
  public short[] getJavaArrayShort(int recnum, StructureMembers.Member m) {
    if (m.getDataType() != DataType.SHORT) throw new IllegalArgumentException("Type is "+m.getDataType()+", must be short");
    int count = m.getSize();
    Array data = m.getDataArray();
    short[] pa = new short[count];
    for (int i=0; i<count; i++)
      pa[i] = data.getShort( recnum * count + i);
    return pa;
  }

  /**
   * Get scalar member data of type int.
   * @param recnum get data from the recnum-th StructureData of the ArrayStructure. Must be less than getSize();
   * @param m get data from this StructureMembers.Member. Must be of type int.
   * @return scalar double value
   */
  public int getScalarInt(int recnum, StructureMembers.Member m) {
    if (m.getDataType() != DataType.INT) throw new IllegalArgumentException("Type is "+m.getDataType()+", must be int");
    Array data = m.getDataArray();
    return data.getInt( recnum * m.getSize()); // gets first one in the array
  }

  /**
   * Get member data of type int as a 1D array.
   * @param recnum get data from the recnum-th StructureData of the ArrayStructure. Must be less than getSize();
   * @param m get data from this StructureMembers.Member. Must be of type int.
   * @return int[]
   */
  public int[] getJavaArrayInt(int recnum, StructureMembers.Member m) {
    if (m.getDataType() != DataType.INT) throw new IllegalArgumentException("Type is "+m.getDataType()+", must be int");
    int count = m.getSize();
    Array data = m.getDataArray();
    int[] pa = new int[count];
    for (int i=0; i<count; i++)
      pa[i] = data.getInt( recnum * count + i);
    return pa;
  }

  /**
   * Get scalar member data of type long.
   * @param recnum get data from the recnum-th StructureData of the ArrayStructure. Must be less than getSize();
   * @param m get data from this StructureMembers.Member. Must be of type long.
   * @return scalar double value
   */
  public long getScalarLong(int recnum, StructureMembers.Member m) {
    if (m.getDataType() != DataType.LONG) throw new IllegalArgumentException("Type is "+m.getDataType()+", must be long");
    Array data = m.getDataArray();
    return data.getLong( recnum * m.getSize()); // gets first one in the array
  }

  /**
   * Get member data of type long as a 1D array.
   * @param recnum get data from the recnum-th StructureData of the ArrayStructure. Must be less than getSize();
   * @param m get data from this StructureMembers.Member. Must be of type long.
   * @return long[]
   */
  public long[] getJavaArrayLong(int recnum, StructureMembers.Member m) {
    if (m.getDataType() != DataType.LONG) throw new IllegalArgumentException("Type is "+m.getDataType()+", must be long");
    int count = m.getSize();
    Array data = m.getDataArray();
    long[] pa = new long[count];
    for (int i=0; i<count; i++)
      pa[i] = data.getLong( recnum * count + i);
    return pa;
  }

  /**
   * Get scalar member data of type char.
   * @param recnum get data from the recnum-th StructureData of the ArrayStructure. Must be less than getSize();
   * @param m get data from this StructureMembers.Member. Must be of type char.
   * @return scalar double value
   */
  public char getScalarChar(int recnum, StructureMembers.Member m) {
    if (m.getDataType() != DataType.CHAR) throw new IllegalArgumentException("Type is "+m.getDataType()+", must be char");
    Array data = m.getDataArray();
    return data.getChar( recnum * m.getSize()); // gets first one in the array
  }

  /**
   * Get member data of type char as a 1D array.
   * @param recnum get data from the recnum-th StructureData of the ArrayStructure. Must be less than getSize();
   * @param m get data from this StructureMembers.Member. Must be of type char.
   * @return char[]
   */
  public char[] getJavaArrayChar(int recnum, StructureMembers.Member m) {
    if (m.getDataType() != DataType.CHAR) throw new IllegalArgumentException("Type is "+m.getDataType()+", must be char");
    int count = m.getSize();
    Array data = m.getDataArray();
    char[] pa = new char[count];
    for (int i=0; i<count; i++)
      pa[i] = data.getChar( recnum * count + i);
    return pa;
  }

   /**
   * Get member data of type String or char.
   * @param recnum get data from the recnum-th StructureData of the ArrayStructure. Must be less than getSize();
   * @param m get data from this StructureMembers.Member. Must be of type String or char.
   * @return scalar String value
   */
   public String getScalarString(int recnum, StructureMembers.Member m) {
     if (m.getDataType() == DataType.CHAR) {
       ArrayChar data = (ArrayChar) m.getDataArray();
       return data.getString( recnum);
     }

     if (m.getDataType() == DataType.STRING) {
       Array data = m.getDataArray();
       return (String) data.getObject( recnum);
     }

     throw new IllegalArgumentException("Type is " + m.getDataType() + ", must be String or char");
   }

   /**
   * Get member data of type String as a 1D array.
   * @param recnum get data from the recnum-th StructureData of the ArrayStructure. Must be less than getSize();
   * @param m get data from this StructureMembers.Member. Must be of type String.
   * @return String[]
   */
   public String[] getJavaArrayString(int recnum, StructureMembers.Member m) {
     if (m.getDataType() == DataType.STRING) {
       int n = m.getSize();
       String[] result = new String[n];
       Array data = m.getDataArray();
       for (int i=0; i<n; i++)
         result[i] = (String) data.getObject( recnum * n + i);
       return result;
     }

     if (m.getDataType() == DataType.CHAR) {
       ArrayChar data = (ArrayChar) m.getDataArray();
       int n = m.getSize();
       int strLen = indexCalc.getShape(rank - 1);
       String[] result = new String[n];
       for (int i=0; i<n; i++)
         result[i] = data.getString( recnum * n + i);
       return result;
     }

     throw new IllegalArgumentException("Type is " + m.getDataType() + ", must be String or char");
   }

  /* LOOK can we optimize ??
  public String[] getJavaArrayString(StructureMembers.Member m) {

    if (m.getDataType() == DataType.STRING) {
      Array data = getArray(m);
      int n = m.getSize();
      String[] result = new String[n];
      for (int i = 0; i < result.length; i++)
        result[i] = (String) data.getObject(i);
     return result;

    } else if (m.getDataType() == DataType.CHAR) {
      ArrayChar data = (ArrayChar) getArray(m);
      ArrayChar.StringIterator iter = data.getStringIterator();
      String[] result = new String[ iter.getNumElems()];
      int count = 0;
      while (iter.hasNext())
        result[count++] =  iter.next();
      return result;
    }

    throw new IllegalArgumentException("getJavaArrayString: not String DataType :"+m.getDataType());
  }  /*


  /**
  * Get member data of type Structure.
  * @param recnum get data from the recnum-th StructureData of the ArrayStructure. Must be less than getSize();
  * @param m get data from this StructureMembers.Member. Must be of type Structure.
  * @return scalar StructureData
  */
  public StructureData getScalarStructure(int recnum, StructureMembers.Member m) {
    if (m.getDataType() != DataType.STRUCTURE) throw new IllegalArgumentException("Type is "+m.getDataType()+", must be Structure");

    ArrayStructure data = (ArrayStructure) m.getDataArray();
    return data.getStructureData( recnum * m.getSize());  // gets first in the array
  }

  /**
  * Get member data of type array of Structure.
  * @param recnum get data from the recnum-th StructureData of the ArrayStructure. Must be less than getSize();
  * @param m get data from this StructureMembers.Member. Must be of type Structure.
  * @return nested ArrayStructure. */
  public ArrayStructure getArrayStructure(int recnum, StructureMembers.Member m) {
    if (m.getDataType() != DataType.STRUCTURE) throw new IllegalArgumentException("Type is "+m.getDataType()+", must be Structure");

    ArrayStructure array = (ArrayStructure) m.getDataArray();

    // LOOK
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

  /**
   * Convert to double value, with scale, offset if applicable.
   * Underlying type must be convertible to double.
   * @param m member Variable.
   * @throws IllegalArgumentException if m is not legal member.
   * @throws ForbiddenConversionException if not convertible to float.
   *
  public double convertScalarDouble(int recno, StructureMembers.Member m) {
    DataType dt = m.getDataType();
    if (dt == DataType.FLOAT)
      return m.convertScaleOffsetMissing( (double) getScalarFloat(recno, m));
    else if (dt == DataType.DOUBLE)
      return m.convertScaleOffsetMissing( getScalarDouble(recno, m));
    else if (dt == DataType.BYTE)
      return m.convertScaleOffsetMissing( getScalarByte(recno, m));
    else if (dt == DataType.SHORT)
      return m.convertScaleOffsetMissing( getScalarShort(recno, m));
    else if (dt == DataType.INT)
      return m.convertScaleOffsetMissing( getScalarInt(recno, m));
    else if (dt == DataType.LONG)
      return m.convertScaleOffsetMissing( getScalarLong(recno, m));
    else if (dt == DataType.CHAR)
      return m.convertScaleOffsetMissing( getScalarChar(recno, m));

    throw new ForbiddenConversionException();
  } */

  /////////////////////////////////////////////////////////////////////////////////////////////////////

  /** DO NOT USE, throws UnsupportedOperationException */
  public Array createView( Index index) {
    if (index.getSize() == getSize()) return this;
    throw new UnsupportedOperationException();
  }

 /** DO NOT USE, throws UnsupportedOperationException */
  public Array copy() {
    throw new UnsupportedOperationException();
  }

  /**
   * DO NOT USE, throw ForbiddenConversionException
   */
  public double getDouble(Index i) { throw new ForbiddenConversionException(); }

  /**
   * DO NOT USE, throw ForbiddenConversionException
   */
  public void setDouble(Index i, double value) { throw new ForbiddenConversionException(); }

  /**
   * DO NOT USE, throw ForbiddenConversionException
   */
  public float getFloat(Index i) { throw new ForbiddenConversionException(); }

  /**
   * DO NOT USE, throw ForbiddenConversionException
   */
  public void setFloat(Index i, float value) { throw new ForbiddenConversionException(); }

  /**
   * DO NOT USE, throw ForbiddenConversionException
   */
  public long getLong(Index i) { throw new ForbiddenConversionException(); }

  /**
   * DO NOT USE, throw ForbiddenConversionException
   */
  public void setLong(Index i, long value) { throw new ForbiddenConversionException(); }

  /**
   * DO NOT USE, throw ForbiddenConversionException
   */
  public int getInt(Index i) { throw new ForbiddenConversionException(); }

  /**
   * DO NOT USE, throw ForbiddenConversionException
   */
  public void setInt(Index i, int value) { throw new ForbiddenConversionException(); }

  /**
   * DO NOT USE, throw ForbiddenConversionException
   */
  public short getShort(Index i) { throw new ForbiddenConversionException(); }

  /**
   * DO NOT USE, throw ForbiddenConversionException
   */
  public void setShort(Index i, short value) { throw new ForbiddenConversionException(); }

  /**
   * DO NOT USE, throw ForbiddenConversionException
   */
  public byte getByte(Index i) { throw new ForbiddenConversionException(); }

  /**
   * DO NOT USE, throw ForbiddenConversionException
   */
  public void setByte(Index i, byte value) { throw new ForbiddenConversionException(); }

  /**
   * DO NOT USE, throw ForbiddenConversionException
   */
  public boolean getBoolean(Index i) { throw new ForbiddenConversionException(); }

  /**
   * DO NOT USE, throw ForbiddenConversionException
   */
  public void setBoolean(Index i, boolean value) { throw new ForbiddenConversionException(); }

  /**
   * DO NOT USE, throw ForbiddenConversionException
   */
  public char getChar(Index i) { throw new ForbiddenConversionException(); }

  /**
   * DO NOT USE, throw ForbiddenConversionException
   */
  public void setChar(Index i, char value) { throw new ForbiddenConversionException(); }

  // trusted, assumes that individual dimension lengths have been checked
  // package private : mostly for iterators
  double getDouble(int index) {throw new ForbiddenConversionException(); }
  void setDouble(int index, double value) { throw new ForbiddenConversionException(); }
  float getFloat(int index) { throw new ForbiddenConversionException(); }
  void setFloat(int index, float value) { throw new ForbiddenConversionException(); }
  long getLong(int index) {throw new ForbiddenConversionException(); }
  void setLong(int index, long value) { throw new ForbiddenConversionException(); }
  int getInt(int index) { throw new ForbiddenConversionException(); }
  void setInt(int index, int value) { throw new ForbiddenConversionException(); }
  short getShort(int index) { throw new ForbiddenConversionException(); }
  void setShort(int index, short value) { throw new ForbiddenConversionException(); }
  byte getByte(int index) { throw new ForbiddenConversionException(); }
  void setByte(int index, byte value) {throw new ForbiddenConversionException(); }
  char getChar(int index) { throw new ForbiddenConversionException(); }
  void setChar(int index, char value) { throw new ForbiddenConversionException(); }
  boolean getBoolean(int index) { throw new ForbiddenConversionException(); }
  void setBoolean(int index, boolean value) { throw new ForbiddenConversionException(); }

}