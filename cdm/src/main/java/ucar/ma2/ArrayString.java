/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.ma2;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;

/**
 * Concrete implementation of Array specialized for Strings.
 * Data storage is with 1D java array of Strings.
 *
 * @author Heimbigner
 * @see Array
 */
public class ArrayString extends Array {

  /**
   * package private. use Array.factory()
   */
  static ArrayString factory(Index index) {
    return ArrayString.factory(index, null);
  }

  /* create new ArrayString with given indexImpl and backing store.
   * Should be private.
   * @param index use this Index
   * @param stor. use this storage. if null, allocate.
   * @return. new ArrayString.D<rank> or ArrayString object.
   */
  static ArrayString factory(Index index, String[] storage) {
    if (index instanceof Index0D) {
      return new ArrayString.D0(index, storage);
    } else if (index instanceof Index1D) {
      return new ArrayString.D1(index, storage);
    } else if (index instanceof Index2D) {
      return new ArrayString.D2(index, storage);
    } else if (index instanceof Index3D) {
      return new ArrayString.D3(index, storage);
    } else if (index instanceof Index4D) {
      return new ArrayString.D4(index, storage);
    } else if (index instanceof Index5D) {
      return new ArrayString.D5(index, storage);
    } else if (index instanceof Index6D) {
      return new ArrayString.D6(index, storage);
    } else if (index instanceof Index7D) {
      return new ArrayString.D7(index, storage);
    } else {
      return new ArrayString(index, storage);
    }
  }

  //////////////////////////////////////////////////////
  protected String[] storage;

  /**
   * Create a new Array of type long and the given shape.
   * dimensions.length determines the rank of the new Array.
   *
   * @param dimensions the shape of the Array.
   */
  public ArrayString(int[] dimensions) {
    super(DataType.STRING, dimensions);
    storage = new String[(int) indexCalc.getSize()];
  }

  /**
   * Create a new Array using the given IndexArray and backing store.
   * used for sections. Trusted package private.
   *
   * @param ima  use this IndexArray as the index
   * @param data use this as the backing store
   */
  ArrayString(Index ima, String[] data) {
    super(DataType.STRING, ima);
    /* replace by something better
    if (ima.getSize() != data.length)
      throw new IllegalArgumentException("bad data length"); */
    if (data != null)
      storage = data;
    else
      storage = new String[(int) ima.getSize()];
  }

  /**
   * create new Array with given indexImpl and same backing store
   */
  protected Array createView(Index index) {
    return ArrayString.factory(index, storage);
  }

  /* Get underlying primitive array storage. CAUTION! You may invalidate your warranty! */
  public Object getStorage() {
    return storage;
  }

  // copy from javaArray to storage using the iterator: used by factory( Object);
  protected void copyFrom1DJavaArray(IndexIterator iter, Object javaArray) {
    String[] ja = (String[]) javaArray;
    for (String aJa : ja) iter.setObjectNext(aJa);
  }

  // copy to javaArray from storage using the iterator: used by copyToNDJavaArray;
  protected void copyTo1DJavaArray(IndexIterator iter, Object javaArray) {
    String[] ja = (String[]) javaArray;
    for (int i = 0; i < ja.length; i++)
      ja[i] = (String) iter.getObjectNext();
  }

  public ByteBuffer getDataAsByteBuffer() {
    // Store strings as null terminated character sequences
    int totalsize = 0;
    for (String aStorage : storage)
      totalsize += (aStorage.length() + 1); // 1 for null terminator
    ByteBuffer bb = ByteBuffer.allocate(2 * totalsize);
    CharBuffer cb = bb.asCharBuffer();
    // Concatenate 
    for (String s : storage) {
      cb.append(s);
      cb.append('\0');
    }
    return bb;
  }

  /**
   * Return the element class type
   */
  public Class getElementType() {
    return String.class;
  }

  /**
   * get the value at the specified index.
   */
  public String get(Index i) {
    return storage[i.currentElement()];
  }

  /**
   * set the value at the sepcified index.
   */
  public void set(Index i, String value) {
    storage[i.currentElement()] = value;
  }

  /**
   * not legal, throw ForbiddenConversionException
   */
  public double getDouble(Index i) {
    throw new ForbiddenConversionException();
  }

  /**
   * not legal, throw ForbiddenConversionException
   */
  public void setDouble(Index i, double value) {
    throw new ForbiddenConversionException();
  }

  /**
   * not legal, throw ForbiddenConversionException
   */
  public float getFloat(Index i) {
    throw new ForbiddenConversionException();
  }

  /**
   * not legal, throw ForbiddenConversionException
   */
  public void setFloat(Index i, float value) {
    throw new ForbiddenConversionException();
  }

  /**
   * not legal, throw ForbiddenConversionException
   */
  public long getLong(Index i) {
    throw new ForbiddenConversionException();
  }

  /**
   * not legal, throw ForbiddenConversionException
   */
  public void setLong(Index i, long value) {
    throw new ForbiddenConversionException();
  }

  /**
   * not legal, throw ForbiddenConversionException
   */
  public int getInt(Index i) {
    throw new ForbiddenConversionException();
  }

  /**
   * not legal, throw ForbiddenConversionException
   */
  public void setInt(Index i, int value) {
    throw new ForbiddenConversionException();
  }

  /**
   * not legal, throw ForbiddenConversionException
   */
  public short getShort(Index i) {
    throw new ForbiddenConversionException();
  }

  /**
   * not legal, throw ForbiddenConversionException
   */
  public void setShort(Index i, short value) {
    throw new ForbiddenConversionException();
  }

  /**
   * not legal, throw ForbiddenConversionException
   */
  public byte getByte(Index i) {
    throw new ForbiddenConversionException();
  }

  /**
   * not legal, throw ForbiddenConversionException
   */
  public void setByte(Index i, byte value) {
    throw new ForbiddenConversionException();
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

  /**
   * not legal, throw ForbiddenConversionException
   */
  public char getChar(Index i) {
    throw new ForbiddenConversionException();
  }

  /**
   * not legal, throw ForbiddenConversionException
   */
  public void setChar(Index i, char value) {
    throw new ForbiddenConversionException();
  }

  public Object getObject(Index i) {
    return storage[i.currentElement()];
  }

  public void setObject(Index i, Object value) {
    storage[i.currentElement()] = ((String) value);
  }

  // package private : mostly for iterators
  public double getDouble(int index) {
    throw new ForbiddenConversionException();
  }

  public void setDouble(int index, double value) {
    throw new ForbiddenConversionException();
  }

  public float getFloat(int index) {
    throw new ForbiddenConversionException();
  }

  public void setFloat(int index, float value) {
    throw new ForbiddenConversionException();
  }

  public long getLong(int index) {
    throw new ForbiddenConversionException();
  }

  public void setLong(int index, long value) {
    throw new ForbiddenConversionException();
  }

  public int getInt(int index) {
    throw new ForbiddenConversionException();
  }

  public void setInt(int index, int value) {
    throw new ForbiddenConversionException();
  }

  public short getShort(int index) {
    throw new ForbiddenConversionException();
  }

  public void setShort(int index, short value) {
    throw new ForbiddenConversionException();
  }

  public byte getByte(int index) {
    throw new ForbiddenConversionException();
  }

  public void setByte(int index, byte value) {
    throw new ForbiddenConversionException();
  }

  public char getChar(int index) {
    throw new ForbiddenConversionException();
  }

  public void setChar(int index, char value) {
    throw new ForbiddenConversionException();
  }

  public boolean getBoolean(int index) {
    throw new ForbiddenConversionException();
  }

  public void setBoolean(int index, boolean value) {
    throw new ForbiddenConversionException();
  }

  public Object getObject(int index) {
    return storage[index];
  }

  public void setObject(int index, Object value) {
    storage[index] = (String) value;
  }

  /**
   * Concrete implementation of Array specialized for String, rank 0.
   */
  public static class D0 extends ArrayString {
    private Index0D ix;

    /**
     * Constructor.
     */
    public D0() {
      super(new int[]{});
      ix = (Index0D) indexCalc;
    }

    private D0(Index i, String[] store) {
      super(i, store);
      ix = (Index0D) indexCalc;
    }

    /**
     * get the value.
     */
    public String get() {
      return storage[ix.currentElement()];
    }

    /**
     * set the value.
     */
    public void set(String value) {
      storage[ix.currentElement()] = value;
    }
  }

  /**
   * Concrete implementation of Array specialized for Strings, rank 1.
   */
  public static class D1 extends ArrayString {
    private Index1D ix;

    /**
     * Constructor for array of shape {len0}.
     */
    public D1(int len0) {
      super(new int[]{len0});
      ix = (Index1D) indexCalc;
    }

    private D1(Index i, String[] store) {
      super(i, store);
      ix = (Index1D) indexCalc;
    }

    /**
     * get the value.
     */
    public String get(int i) {
      return storage[ix.setDirect(i)];
    }

    /**
     * set the value.
     */
    public void set(int i, String value) {
      storage[ix.setDirect(i)] = value;
    }
  }

  /**
   * Concrete implementation of Array specialized for Strings, rank 2.
   */
  public static class D2 extends ArrayString {
    private Index2D ix;

    /**
     * Constructor for array of shape {len0,len1}.
     */
    public D2(int len0, int len1) {
      super(new int[]{len0, len1});
      ix = (Index2D) indexCalc;
    }

    private D2(Index i, String[] store) {
      super(i, store);
      ix = (Index2D) indexCalc;
    }

    /**
     * get the value.
     */
    public String get(int i, int j) {
      return storage[ix.setDirect(i, j)];
    }

    /**
     * set the value.
     */
    public void set(int i, int j, String value) {
      storage[ix.setDirect(i, j)] = value;
    }
  }

  /**
   * Concrete implementation of Array specialized for Strings, rank 3.
   */
  public static class D3 extends ArrayString {
    private Index3D ix;

    /**
     * Constructor for array of shape {len0,len1,len2}.
     */
    public D3(int len0, int len1, int len2) {
      super(new int[]{len0, len1, len2});
      ix = (Index3D) indexCalc;
    }

    private D3(Index i, String[] store) {
      super(i, store);
      ix = (Index3D) indexCalc;
    }

    /**
     * get the value.
     */
    public String get(int i, int j, int k) {
      return storage[ix.setDirect(i, j, k)];
    }

    /**
     * set the value.
     */
    public void set(int i, int j, int k, String value) {
      storage[ix.setDirect(i, j, k)] = value;
    }
  }

  /**
   * Concrete implementation of Array specialized for Strings, rank 4.
   */
  public static class D4 extends ArrayString {
    private Index4D ix;

    /**
     * Constructor for array of shape {len0,len1,len2,len3}.
     */
    public D4(int len0, int len1, int len2, int len3) {
      super(new int[]{len0, len1, len2, len3});
      ix = (Index4D) indexCalc;
    }

    private D4(Index i, String[] store) {
      super(i, store);
      ix = (Index4D) indexCalc;
    }

    /**
     * get the value.
     */
    public String get(int i, int j, int k, int l) {
      return storage[ix.setDirect(i, j, k, l)];
    }

    /**
     * set the value.
     */
    public void set(int i, int j, int k, int l, String value) {
      storage[ix.setDirect(i, j, k, l)] = value;
    }
  }

  /**
   * Concrete implementation of Array specialized for Strings, rank 5.
   */
  public static class D5 extends ArrayString {
    private Index5D ix;

    /**
     * Constructor for array of shape {len0,len1,len2,len3,len4}.
     */
    public D5(int len0, int len1, int len2, int len3, int len4) {
      super(new int[]{len0, len1, len2, len3, len4});
      ix = (Index5D) indexCalc;
    }

    private D5(Index i, String[] store) {
      super(i, store);
      ix = (Index5D) indexCalc;
    }

    /**
     * get the value.
     */
    public String get(int i, int j, int k, int l, int m) {
      return storage[ix.setDirect(i, j, k, l, m)];
    }

    /**
     * set the value.
     */
    public void set(int i, int j, int k, int l, int m, String value) {
      storage[ix.setDirect(i, j, k, l, m)] = value;
    }
  }

  /**
   * Concrete implementation of Array specialized for Strings, rank 6.
   */
  public static class D6 extends ArrayString {
    private Index6D ix;

    /**
     * Constructor for array of shape {len0,len1,len2,len3,len4,len5,}.
     */
    public D6(int len0, int len1, int len2, int len3, int len4, int len5) {
      super(new int[]{len0, len1, len2, len3, len4, len5});
      ix = (Index6D) indexCalc;
    }

    private D6(Index i, String[] store) {
      super(i, store);
      ix = (Index6D) indexCalc;
    }

    /**
     * get the value.
     */
    public String get(int i, int j, int k, int l, int m, int n) {
      return storage[ix.setDirect(i, j, k, l, m, n)];
    }

    /**
     * set the value.
     */
    public void set(int i, int j, int k, int l, int m, int n, String value) {
      storage[ix.setDirect(i, j, k, l, m, n)] = value;
    }
  }

  /**
   * Concrete implementation of Array specialized for Strings, rank 7.
   */
  public static class D7 extends ArrayString {
    private Index7D ix;

    /**
     * Constructor for array of shape {len0,len1,len2,len3,len4,len5,len6}.
     */
    public D7(int len0, int len1, int len2, int len3, int len4, int len5, int len6) {
      super(new int[]{len0, len1, len2, len3, len4, len5, len6});
      ix = (Index7D) indexCalc;
    }

    private D7(Index i, String[] store) {
      super(i, store);
      ix = (Index7D) indexCalc;
    }

    /**
     * get the value.
     */
    public String get(int i, int j, int k, int l, int m, int n, int o) {
      return storage[ix.setDirect(i, j, k, l, m, n, o)];
    }

    /**
     * set the value.
     */
    public void set(int i, int j, int k, int l, int m, int n, int o, String value) {
      storage[ix.setDirect(i, j, k, l, m, n, o)] = value;
    }
  }

}
