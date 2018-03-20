/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.ma2;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.LongBuffer;

/**
 * Concrete implementation of Array specialized for longs.
 * Data storage is with 1D java array of longs.
 *
 * issues: what should we do if a conversion loses accuracy? nothing ? Exception ?
 *
 * @see Array
 * @author caron
 */
public class ArrayLong extends Array {

  /** package private. use Array.factory() */
  static ArrayLong factory(Index index, boolean isUnsigned) {
    return ArrayLong.factory(index, isUnsigned, null);
  }

  /* create new ArrayLong with given indexImpl and backing store.
   * @param index use this Index
   * @param stor. use this storage. if null, allocate.
   * @return. new ArrayLong.D<rank> or ArrayLong object.
   */
  static ArrayLong factory( Index index, boolean isUnsigned, long [] storage) {
      if (index instanceof Index0D) {
          return new ArrayLong.D0(index, isUnsigned, storage);
      } else if (index instanceof Index1D) {
          return new ArrayLong.D1(index, isUnsigned, storage);
      } else if (index instanceof Index2D) {
          return new ArrayLong.D2(index, isUnsigned, storage);
      } else if (index instanceof Index3D) {
          return new ArrayLong.D3(index, isUnsigned, storage);
      } else if (index instanceof Index4D) {
          return new ArrayLong.D4(index, isUnsigned, storage);
      } else if (index instanceof Index5D) {
          return new ArrayLong.D5(index, isUnsigned, storage);
      } else if (index instanceof Index6D) {
          return new ArrayLong.D6(index, isUnsigned, storage);
      } else if (index instanceof Index7D) {
          return new ArrayLong.D7(index, isUnsigned, storage);
      } else {
          return new ArrayLong(index, isUnsigned, storage);
      }
  }

  //////////////////////////////////////////////////////
  protected long[] storage;

  /**
  * Create a new Array of type long and the given shape.
  * dimensions.length determines the rank of the new Array.
  * @param dimensions the shape of the Array.
  */
  public ArrayLong(int [] dimensions, boolean isUnsigned) {
    super(isUnsigned ? DataType.ULONG : DataType.LONG, dimensions);
    storage = new long[(int)indexCalc.getSize()];
  }

  /**
  * Create a new Array using the given IndexArray and backing store.
  * used for sections. Trusted package private.
  * @param ima use this IndexArray as the index
  * @param data use this as the backing store
  */
  ArrayLong(Index ima, boolean isUnsigned, long [] data) {
    super(isUnsigned ? DataType.ULONG : DataType.LONG, ima);
    /* replace by something better
    if (ima.getSize() != data.length)
      throw new IllegalArgumentException("bad data length"); */
    if (data != null)
      storage = data;
    else
      storage = new long[(int)ima.getSize()];
  }

  /** create new Array with given indexImpl and same backing store */
  protected Array createView( Index index) {
    return ArrayLong.factory( index, isUnsigned(), storage);
  }

  /* Get underlying primitive array storage. CAUTION! You may invalidate your warrentee! */
  public Object getStorage() { return storage; }

      // copy from javaArray to storage using the iterator: used by factory( Object);
  protected void copyFrom1DJavaArray(IndexIterator iter, Object javaArray) {
    long[] ja = (long []) javaArray;
    for (long aJa : ja) iter.setLongNext(aJa);
  }

  // copy to javaArray from storage using the iterator: used by copyToNDJavaArray;
  protected void copyTo1DJavaArray(IndexIterator iter, Object javaArray) {
    long[] ja = (long []) javaArray;
    for (int i=0; i<ja.length; i++)
      ja[i] = iter.getLongNext();
  }

    public ByteBuffer getDataAsByteBuffer() {return getDataAsByteBuffer(null);}

    public ByteBuffer getDataAsByteBuffer(ByteOrder order) {
        ByteBuffer bb = super.getDataAsByteBuffer((int) (8 * getSize()), order);
        LongBuffer ib = bb.asLongBuffer();
        ib.put( (long[]) get1DJavaArray(getDataType())); // make sure its in canonical order
        return bb;
  }

 /** Return the element class type */
  public Class getElementType() { return long.class; }

    /** get the value at the specified index. */
  public long get(Index i) {
    return storage[i.currentElement()];
  }
    /** set the value at the sepcified index. */
  public void set(Index i, long value) {
    storage[i.currentElement()] = value;
  }

  public double getDouble(Index i) {
    return (double) storage[i.currentElement()];
  }
  public void setDouble(Index i, double value) {
    storage[i.currentElement()] = (long) value;
  }

  public float getFloat(Index i) {
    return (float) storage[i.currentElement()];
  }
  public void setFloat(Index i, float value) {
    storage[i.currentElement()] = (long) value;
  }

  public long getLong(Index i) {
    return storage[i.currentElement()];
  }
  public void setLong(Index i, long value) {
    storage[i.currentElement()] = value;
  }

  public int getInt(Index i) {
    return (int) storage[i.currentElement()];
  }
  public void setInt(Index i, int value) {
    storage[i.currentElement()] = (long) value;
  }

  public short getShort(Index i) {
    return (short) storage[i.currentElement()];
  }
  public void setShort(Index i, short value) {
    storage[i.currentElement()] = (long) value;
  }

  public byte getByte(Index i) {
    return (byte) storage[i.currentElement()];
  }
  public void setByte(Index i, byte value) {
    storage[i.currentElement()] = (long) value;
  }

  public char getChar(Index i) {
    return (char) storage[i.currentElement()];
  }
  public void setChar(Index i, char value) {
    storage[i.currentElement()] = (long) value;
  }

  /** not legal, throw ForbiddenConversionException */
  public boolean getBoolean(Index i) {
    throw new ForbiddenConversionException();
  }
  /** not legal, throw ForbiddenConversionException */
  public void setBoolean(Index i, boolean value) {
     throw new ForbiddenConversionException();
  }

  public Object getObject(Index i) {
    return storage[i.currentElement()];
  }
  public void setObject(Index i, Object value) {
    storage[i.currentElement()] = ((Number)value).longValue();
  }

    // package private : mostly for iterators
  public double getDouble(int index) {return (double) storage[index]; }
  public void setDouble(int index, double value) { storage[index] = (long) value; }

  public float getFloat(int index) { return storage[index]; }
  public void setFloat(int index, float value) { storage[index] = (long) value;}

  public long getLong(int index) {return storage[index];}
  public void setLong(int index, long value) { storage[index] = value;}

  public int getInt(int index) { return (int) storage[index]; }
  public void setInt(int index, int value) { storage[index] = (long) value;}

  public short getShort(int index) { return (short) storage[index]; }
  public void setShort(int index, short value) { storage[index] = (long) value; }

  public byte getByte(int index) { return (byte) storage[index]; }
  public void setByte(int index, byte value) {storage[index] = (long) value;}

  public char getChar(int index) { return (char) storage[index];}
  public void setChar(int index, char value) { storage[index] = (long) value; }

  public boolean getBoolean(int index) { throw new ForbiddenConversionException(); }
  public void setBoolean(int index, boolean value) {throw new ForbiddenConversionException(); }

  public Object getObject(int index) { return getLong(index); }
  public void setObject(int index, Object value) { storage[index] = ((Number)value).longValue(); }

  /** Concrete implementation of Array specialized for longs, rank 0. */
  public static class D0 extends ArrayLong {
    private Index0D ix;
    /** Constructor. */
    public D0 (boolean isUnsigned) {
      super(new int [] {}, isUnsigned);
      ix = (Index0D) indexCalc;
    }
    D0 (Index i, boolean isUnsigned, long[] store) {
      super(i, isUnsigned, store);
      ix = (Index0D) indexCalc;
    }
    /** get the value. */
    public long get() {
      return storage[ix.currentElement()];
    }
    /** set the value. */
    public void set(long value) {
      storage[ix.currentElement()] = value;
    }
  }

  /** Concrete implementation of Array specialized for longs, rank 1. */
  public static class D1 extends ArrayLong {
    private Index1D ix;
    /** Constructor for array of shape {len0}. */
    public D1 (int len0, boolean isUnsigned) {
      super(new int [] {len0}, isUnsigned);
      ix = (Index1D) indexCalc;
    }
    private D1(Index i, boolean isUnsigned, long[] store) {
      super(i, isUnsigned, store);
      ix = (Index1D) indexCalc;
    }
    /** get the value. */
    public long get(int i) {
      return storage[ix.setDirect(i)];
    }
    /** set the value. */
    public void set(int i, long value) {
      storage[ix.setDirect(i)] = value;
    }
  }

  /** Concrete implementation of Array specialized for longs, rank 2. */
  public static class D2 extends ArrayLong {
    private Index2D ix;
    /** Constructor for array of shape {len0,len1}. */
    public D2 (int len0, int len1, boolean isUnsigned) {
      super(new int [] {len0, len1}, isUnsigned);
      ix = (Index2D) indexCalc;
    }
    private D2 (Index i, boolean isUnsigned, long[] store) {
      super(i, isUnsigned, store);
      ix = (Index2D) indexCalc;
    }
    /** get the value. */
    public long get(int i, int j) {
      return storage[ix.setDirect(i,j)];
    }
    /** set the value. */
    public void set(int i, int j, long value) {
      storage[ix.setDirect(i,j)] = value;
    }
  }

  /** Concrete implementation of Array specialized for longs, rank 3. */
  public static class D3 extends ArrayLong {
    private Index3D ix;
    /** Constructor for array of shape {len0,len1,len2}. */
    public D3 (int len0, int len1, int len2, boolean isUnsigned) {
      super(new int [] {len0, len1, len2}, isUnsigned);
      ix = (Index3D) indexCalc;
    }
    private D3 (Index i, boolean isUnsigned, long[] store) {
      super(i, isUnsigned, store);
      ix = (Index3D) indexCalc;
    }
    /** get the value. */
    public long get(int i, int j, int k) {
      return storage[ix.setDirect(i,j,k)];
    }
    /** set the value. */
    public void set(int i, int j, int k, long value) {
      storage[ix.setDirect(i,j,k)] = value;
    }
  }

  /** Concrete implementation of Array specialized for longs, rank 4. */
  public static class D4 extends ArrayLong {
    private Index4D ix;
    /** Constructor for array of shape {len0,len1,len2,len3}. */
    public D4 (int len0, int len1, int len2, int len3, boolean isUnsigned) {
      super(new int [] {len0, len1, len2, len3}, isUnsigned);
      ix = (Index4D) indexCalc;
    }
    private D4 (Index i, boolean isUnsigned, long[] store) {
      super(i, isUnsigned, store);
      ix = (Index4D) indexCalc;
    }
    /** get the value. */
    public long get(int i, int j, int k, int l) {
      return storage[ix.setDirect(i,j,k,l)];
    }
    /** set the value. */
    public void set(int i, int j, int k, int l, long value) {
      storage[ix.setDirect(i,j,k,l)] = value;
    }
  }

  /** Concrete implementation of Array specialized for longs, rank 5. */
  public static class D5 extends ArrayLong {
    private Index5D ix;
    /** Constructor for array of shape {len0,len1,len2,len3,len4}. */
    public D5 (int len0, int len1, int len2, int len3, int len4, boolean isUnsigned) {
      super(new int [] {len0, len1, len2, len3, len4}, isUnsigned);
      ix = (Index5D) indexCalc;
    }
    private D5 (Index i, boolean isUnsigned, long[] store) {
      super(i, isUnsigned, store);
      ix = (Index5D) indexCalc;
    }
    /** get the value. */
    public long get(int i, int j, int k, int l, int m) {
      return storage[ix.setDirect(i,j,k,l, m)];
    }
    /** set the value. */
    public void set(int i, int j, int k, int l, int m, long value) {
      storage[ix.setDirect(i,j,k,l, m)] = value;
    }
  }

  /** Concrete implementation of Array specialized for longs, rank 6. */
  public static class D6 extends ArrayLong {
    private Index6D ix;
    /** Constructor for array of shape {len0,len1,len2,len3,len4,len5,}. */
    public D6 (int len0, int len1, int len2, int len3, int len4, int len5, boolean isUnsigned) {
      super(new int [] {len0, len1, len2, len3, len4, len5}, isUnsigned);
      ix = (Index6D) indexCalc;
    }
    private D6 (Index i, boolean isUnsigned, long[] store) {
      super(i, isUnsigned, store);
      ix = (Index6D) indexCalc;
    }
    /** get the value. */
    public long get(int i, int j, int k, int l, int m, int n) {
      return storage[ix.setDirect(i,j,k,l,m,n)];
    }
    /** set the value. */
    public void set(int i, int j, int k, int l, int m, int n, long value) {
      storage[ix.setDirect(i,j,k,l,m,n)] = value;
    }
  }

  /** Concrete implementation of Array specialized for longs, rank 7. */
  public static class D7 extends ArrayLong {
    private Index7D ix;
    /** Constructor for array of shape {len0,len1,len2,len3,len4,len5,len6}. */
    public D7 (int len0, int len1, int len2, int len3, int len4, int len5, int len6, boolean isUnsigned) {
      super(new int [] {len0, len1, len2, len3, len4, len5, len6}, isUnsigned);
      ix = (Index7D) indexCalc;
    }
    private D7 (Index i, boolean isUnsigned, long[] store) {
      super(i, isUnsigned, store);
      ix = (Index7D) indexCalc;
    }
    /** get the value. */
    public long get(int i, int j, int k, int l, int m, int n, int o) {
      return storage[ix.setDirect(i,j,k,l,m,n,o)];
    }
    /** set the value. */
    public void set(int i, int j, int k, int l, int m, int n, int o, long value) {
      storage[ix.setDirect(i,j,k,l,m,n,o)] = value;
    }
  }

}
