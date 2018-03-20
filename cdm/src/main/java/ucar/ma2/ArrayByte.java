/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.ma2;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Concrete implementation of Array specialized for bytes.
 * Data storage is with 1D java array of bytes.
 * <p/>
 * issues: what should we do if a conversion loses accuracy? nothing ? Exception ?
 *
 * @author caron
 * @see Array
 */
public class ArrayByte extends Array {

  // package private. use Array.factory() */
  static ArrayByte factory(Index index, boolean isUnsigned) {
    return ArrayByte.factory(index, isUnsigned, null);
  }

  /* create new ArrayByte with given Index and backing store.
   * @param index use this Index
   * @param storage use this storage. if null, allocate.
   * @return new ArrayByte.D<rank> or ArrayByte object.
   */
  static ArrayByte factory( Index index, boolean isUnsigned, byte [] storage) {
    if (index instanceof Index0D) {
      return new ArrayByte.D0(index, isUnsigned, storage);
    } else if (index instanceof Index1D) {
      return new ArrayByte.D1(index, isUnsigned, storage);
    } else if (index instanceof Index2D) {
      return new ArrayByte.D2(index, isUnsigned, storage);
    } else if (index instanceof Index3D) {
      return new ArrayByte.D3(index, isUnsigned, storage);
    } else if (index instanceof Index4D) {
      return new ArrayByte.D4(index, isUnsigned, storage);
    } else if (index instanceof Index5D) {
      return new ArrayByte.D5(index, isUnsigned, storage);
    } else if (index instanceof Index6D) {
      return new ArrayByte.D6(index, isUnsigned, storage);
    } else if (index instanceof Index7D) {
      return new ArrayByte.D7(index, isUnsigned, storage);
    } else {
      return new ArrayByte(index, isUnsigned, storage);
    }
  }

  ///////////////////////////////////////////////////////////////////////////////
  protected byte[] storage;

  /**
   * Create a new Array of type byte and the given shape.
   * dimensions.length determines the rank of the new Array.
   *
   * @param dimensions the shape of the Array.
   */
  public ArrayByte(int[] dimensions, boolean isUnsigned) {
    super(isUnsigned ? DataType.UBYTE : DataType.BYTE, dimensions);
    storage = new byte[(int) indexCalc.getSize()];
  }

  /**
   * Create a new Array using the given IndexArray and backing store.
   * used for sections. Trusted package private.
   *
   * @param ima  use this IndexArray as the index
   * @param data use this as the backing store
   */
  ArrayByte(Index ima, boolean isUnsigned, byte[] data) {
    super(isUnsigned ? DataType.UBYTE : DataType.BYTE, ima);
    /* replace by something better
    if (ima.getSize() != data.length)
      throw new IllegalArgumentException("bad data length");  */
    if (data != null)
      storage = data;
    else
      storage = new byte[(int) ima.getSize()];
  }

  protected Array createView(Index index) {
    return ArrayByte.factory(index, isUnsigned(), storage);
  }

  public Object getStorage() {
    return storage;
  }

  // copy from javaArray to storage using the iterator: used by factory( Object);
  protected void copyFrom1DJavaArray(IndexIterator iter, Object javaArray) {
    byte[] ja = (byte[]) javaArray;
    for (byte aJa : ja) iter.setByteNext(aJa);
  }

  // copy to javaArray from storage using the iterator: used by copyToNDJavaArray;
  protected void copyTo1DJavaArray(IndexIterator iter, Object javaArray) {
    byte[] ja = (byte[]) javaArray;
    for (int i = 0; i < ja.length; i++)
      ja[i] = iter.getByteNext();
  }

  @Override
  public ByteBuffer getDataAsByteBuffer() {return getDataAsByteBuffer(null);}

  public ByteBuffer getDataAsByteBuffer(ByteOrder order) {// order irrelevant here
    return ByteBuffer.wrap((byte[]) get1DJavaArray(getDataType()));
  }

  /**
   * Return the element class type
   */
  public Class getElementType() {
    return byte.class;
  }

  /**
   * get the value at the specified index.
   *
   * @param i index
   * @return byte value
   */
  public byte get(Index i) {
    return storage[i.currentElement()];
  }

  /**
   * set the value at the sepcified index.
   *
   * @param i     index
   * @param value set to this value
   */
  public void set(Index i, byte value) {
    storage[i.currentElement()] = value;
  }

  public double getDouble(Index i) {
    byte val = storage[i.currentElement()];
    return (double) (isUnsigned() ? DataType.unsignedByteToShort(val) : val);
  }

  public void setDouble(Index i, double value) {
    storage[i.currentElement()] = (byte) value;
  }

  public float getFloat(Index i) {
    byte val = storage[i.currentElement()];
    return (float) (isUnsigned() ? DataType.unsignedByteToShort(val) : val);
  }

  public void setFloat(Index i, float value) {
    storage[i.currentElement()] = (byte) value;
  }

  public long getLong(Index i) {
    byte val = storage[i.currentElement()];
    return (long) (isUnsigned() ? DataType.unsignedByteToShort(val) : val);
  }

  public void setLong(Index i, long value) {
    storage[i.currentElement()] = (byte) value;
  }

  public int getInt(Index i) {
    byte val = storage[i.currentElement()];
    return (int) (isUnsigned() ? DataType.unsignedByteToShort(val) : val);
  }

  public void setInt(Index i, int value) {
    storage[i.currentElement()] = (byte) value;
  }

  public short getShort(Index i) {
    byte val = storage[i.currentElement()];
    return (isUnsigned() ? DataType.unsignedByteToShort(val) : val);
  }

  public void setShort(Index i, short value) {
    storage[i.currentElement()] = (byte) value;
  }

  public byte getByte(Index i) {
    return storage[i.currentElement()];
  }

  public void setByte(Index i, byte value) {
    storage[i.currentElement()] = value;
  }

  public char getChar(Index i) {
    byte val = storage[i.currentElement()];
    return (char) (isUnsigned() ? DataType.unsignedByteToShort(val) : val);
  }

  public void setChar(Index i, char value) {
    storage[i.currentElement()] = (byte) value;
  }

  /**
   * not legal, throw ForbiddenConversionException
   */
  public boolean getBoolean(Index i) {
    throw new ForbiddenConversionException();
  }

  /**
   * not legal, throw ForbiddenConversionException
   */
  public void setBoolean(Index i, boolean value) {
    throw new ForbiddenConversionException();
  }

  public Object getObject(Index i) {
    return storage[i.currentElement()];
  }

  public void setObject(Index i, Object value) {
    storage[i.currentElement()] = ((Number) value).byteValue();
  }

  // package private : mostly for iterators
  public double getDouble(int index) {
    byte val = storage[index];
    return (double) (isUnsigned() ? DataType.unsignedByteToShort(val) : val);
  }

  public void setDouble(int index, double value) {
    storage[index] = (byte) value;
  }

  public float getFloat(int index) {
    byte val = storage[index];
    return (float) (isUnsigned() ? DataType.unsignedByteToShort(val) : val);
  }

  public void setFloat(int index, float value) {
    storage[index] = (byte) value;
  }

  public long getLong(int index) {
    byte val = storage[index];
    return (long) (isUnsigned() ? DataType.unsignedByteToShort(val) : val);
  }

  public void setLong(int index, long value) {
    storage[index] = (byte) value;
  }

  public int getInt(int index) {
    byte val = storage[index];
    return (int) (isUnsigned() ? DataType.unsignedByteToShort(val) : val);
  }

  public void setInt(int index, int value) {
    storage[index] = (byte) value;
  }

  public short getShort(int index) {
    byte val = storage[index];
    return (isUnsigned() ? DataType.unsignedByteToShort(val) : val);
  }

  public void setShort(int index, short value) {
    storage[index] = (byte) value;
  }

  public byte getByte(int index) {
    return storage[index];
  }

  public void setByte(int index, byte value) {
    storage[index] = value;
  }

  public char getChar(int index) {
    byte val = storage[index];
    return (char) (isUnsigned() ? DataType.unsignedByteToShort(val) : val);
  }

  public void setChar(int index, char value) {
    storage[index] = (byte) value;
  }

  public boolean getBoolean(int index) {
    throw new ForbiddenConversionException();
  }

  public void setBoolean(int index, boolean value) {
    throw new ForbiddenConversionException();
  }

  public Object getObject(int index) {
    return getByte(index);
  }

  public void setObject(int index, Object value) {
    storage[index] = ((Number) value).byteValue();
  }

  /**
   * Concrete implementation of Array specialized for byte, rank 0.
   */
  public static class D0 extends ArrayByte {
    private Index0D ix;

    public D0(boolean isUnsigned) {
      super(new int[]{}, isUnsigned);
      ix = (Index0D) indexCalc;
    }

    private D0(Index i, boolean isUnsigned, byte[] store) {
      super(i, isUnsigned, store);
      ix = (Index0D) indexCalc;
    }

    public byte get() {
      return storage[ix.currentElement()];
    }

    public void set(byte value) {
      storage[ix.currentElement()] = value;
    }
  }

  /**
   * Concrete implementation of Array specialized for byte, rank 1.
   */
  public static class D1 extends ArrayByte {
    private Index1D ix;

    public D1(int len0, boolean isUnsigned) {
      super(new int[]{len0}, isUnsigned);
      ix = (Index1D) indexCalc;
    }

    private D1(Index i, boolean isUnsigned, byte[] store) {
      super(i, isUnsigned, store);
      ix = (Index1D) indexCalc;
    }

    public byte get(int i) {
      return storage[ix.setDirect(i)];
    }

    public void set(int i, byte value) {
      storage[ix.setDirect(i)] = value;
    }
  }

  /**
   * Concrete implementation of Array specialized for byte, rank 2.
   */
  public static class D2 extends ArrayByte {
    private Index2D ix;

    public D2(int len0, int len1, boolean isUnsigned) {
      super(new int[]{len0, len1}, isUnsigned);
      ix = (Index2D) indexCalc;
    }

    private D2(Index i, boolean isUnsigned, byte[] store) {
      super(i, isUnsigned, store);
      ix = (Index2D) indexCalc;
    }

    public byte get(int i, int j) {
      return storage[ix.setDirect(i, j)];
    }

    public void set(int i, int j, byte value) {
      storage[ix.setDirect(i, j)] = value;
    }
  }

  /**
   * Concrete implementation of Array specialized for byte, rank 3.
   */
  public static class D3 extends ArrayByte {
    private Index3D ix;

    public D3(int len0, int len1, int len2, boolean isUnsigned) {
      super(new int[]{len0, len1, len2}, isUnsigned);
      ix = (Index3D) indexCalc;
    }

    private D3(Index i, boolean isUnsigned, byte[] store) {
      super(i, isUnsigned, store);
      ix = (Index3D) indexCalc;
    }

    public byte get(int i, int j, int k) {
      return storage[ix.setDirect(i, j, k)];
    }

    public void set(int i, int j, int k, byte value) {
      storage[ix.setDirect(i, j, k)] = value;
    }
  }

  /**
   * Concrete implementation of Array specialized for byte, rank 4.
   */
  public static class D4 extends ArrayByte {
    private Index4D ix;

    public D4(int len0, int len1, int len2, int len3, boolean isUnsigned) {
      super(new int[]{len0, len1, len2, len3}, isUnsigned);
      ix = (Index4D) indexCalc;
    }

    private D4(Index i, boolean isUnsigned, byte[] store) {
      super(i, isUnsigned, store);
      ix = (Index4D) indexCalc;
    }

    public byte get(int i, int j, int k, int l) {
      return storage[ix.setDirect(i, j, k, l)];
    }

    public void set(int i, int j, int k, int l, byte value) {
      storage[ix.setDirect(i, j, k, l)] = value;
    }
  }

  /**
   * Concrete implementation of Array specialized for byte, rank 5.
   */
  public static class D5 extends ArrayByte {
    private Index5D ix;

    public D5(int len0, int len1, int len2, int len3, int len4, boolean isUnsigned) {
      super(new int[]{len0, len1, len2, len3, len4}, isUnsigned);
      ix = (Index5D) indexCalc;
    }

    private D5(Index i, boolean isUnsigned, byte[] store) {
      super(i, isUnsigned, store);
      ix = (Index5D) indexCalc;
    }

    public byte get(int i, int j, int k, int l, int m) {
      return storage[ix.setDirect(i, j, k, l, m)];
    }

    public void set(int i, int j, int k, int l, int m, byte value) {
      storage[ix.setDirect(i, j, k, l, m)] = value;
    }
  }

  /**
   * Concrete implementation of Array specialized for byte, rank 6.
   */
  public static class D6 extends ArrayByte {
    private Index6D ix;

    public D6(int len0, int len1, int len2, int len3, int len4, int len5, boolean isUnsigned) {
      super(new int[]{len0, len1, len2, len3, len4, len5}, isUnsigned);
      ix = (Index6D) indexCalc;
    }

    private D6(Index i, boolean isUnsigned, byte[] store) {
      super(i, isUnsigned, store);
      ix = (Index6D) indexCalc;
    }

    public byte get(int i, int j, int k, int l, int m, int n) {
      return storage[ix.setDirect(i, j, k, l, m, n)];
    }

    public void set(int i, int j, int k, int l, int m, int n, byte value) {
      storage[ix.setDirect(i, j, k, l, m, n)] = value;
    }
  }

  /**
   * Concrete implementation of Array specialized for byte, rank 7.
   */
  public static class D7 extends ArrayByte {
    protected Index7D ix;

    public D7(int len0, int len1, int len2, int len3, int len4, int len5, int len6, boolean isUnsigned) {
      super(new int[]{len0, len1, len2, len3, len4, len5, len6}, isUnsigned);
      ix = (Index7D) indexCalc;
    }

    private D7(Index i, boolean isUnsigned, byte[] store) {
      super(i, isUnsigned, store);
      ix = (Index7D) indexCalc;
    }

    public byte get(int i, int j, int k, int l, int m, int n, int o) {
      return storage[ix.setDirect(i, j, k, l, m, n, o)];
    }

    public void set(int i, int j, int k, int l, int m, int n, int o, byte value) {
      storage[ix.setDirect(i, j, k, l, m, n, o)] = value;
    }
  }

}
