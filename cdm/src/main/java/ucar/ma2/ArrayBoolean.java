/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.ma2;

/**
 * Concrete implementation of Array specialized for booleans.
 * Data storage is with 1D java array of booleans.
 *
 * issues: what should we do if a conversion loses accuracy? nothing ? Exception ?
 *
 * @see Array
 * @author caron
 */
public class ArrayBoolean extends Array {

  // package private. use Array.factory() */
  static ArrayBoolean factory(Index index) {
    return ArrayBoolean.factory(index, null);
  }

  /* create new ArrayBoolean with given indexImpl and backing store.
   * Should be private.
   * @param index use this Index
   * @param stor. use this storage. if null, allocate.
   * @return. new ArrayDouble.D<rank> or ArrayDouble object.
   */
  static ArrayBoolean factory( Index index, boolean [] storage) {
      if (index instanceof Index0D) {
          return new ArrayBoolean.D0(index, storage);
      } else if (index instanceof Index1D) {
          return new ArrayBoolean.D1(index, storage);
      } else if (index instanceof Index2D) {
          return new ArrayBoolean.D2(index, storage);
      } else if (index instanceof Index3D) {
          return new ArrayBoolean.D3(index, storage);
      } else if (index instanceof Index4D) {
          return new ArrayBoolean.D4(index, storage);
      } else if (index instanceof Index5D) {
          return new ArrayBoolean.D5(index, storage);
      } else if (index instanceof Index6D) {
          return new ArrayBoolean.D6(index, storage);
      } else if (index instanceof Index7D) {
          return new ArrayBoolean.D7(index, storage);
      } else {
          return new ArrayBoolean(index, storage);
      }
  }

  ///////////////////////////////////////////////////////////////////////////////
  protected boolean[] storage;

  /**
  * Create a new Array of type boolean and the given shape.
  * dimensions.length determines the rank of the new Array.
  * @param dimensions the shape of the Array.
  */
  public ArrayBoolean(int [] dimensions) {
    super(DataType.BOOLEAN, dimensions);
    storage = new boolean[(int)indexCalc.getSize()];
  }

  /**
  * Create a new Array using the given IndexArray and backing store.
  * used for sections. Trusted package private.
  * @param ima use this IndexArray as the index
  * @param data use this as the backing store
  */
  ArrayBoolean(Index ima, boolean [] data) {
    super(DataType.BOOLEAN, ima);
    /* replace by something better
    if (ima.getSize() != data.length)
      throw new IllegalArgumentException("bad data length"); */
    if (data != null)
      storage = data;
    else
      storage = new boolean[(int)ima.getSize()];
  }

  /** create new Array with given indexImpl and same backing store */
  protected Array createView( Index index) {
    return ArrayBoolean.factory( index, storage);
  }

  /* Get underlying primitive array storage. CAUTION! You may invalidate your warrentee! */
  public Object getStorage() { return storage; }

  // copy from javaArray to storage using the iterator: used by factory( Object);
  protected void copyFrom1DJavaArray(IndexIterator iter, Object javaArray) {
    boolean[] ja = (boolean []) javaArray;
    for (boolean aJa : ja) iter.setBooleanNext(aJa);
  }

  // copy to javaArray from storage using the iterator: used by copyToNDJavaArray;
  protected void copyTo1DJavaArray(IndexIterator iter, Object javaArray) {
    boolean[] ja = (boolean []) javaArray;
    for (int i=0; i<ja.length; i++)
      ja[i] = iter.getBooleanNext();
  }

 /** Return the element class type */
  public Class getElementType() { return boolean.class; }

    /** get the value at the specified index. */
  public boolean get(Index i) {
    return storage[i.currentElement()];
  }
    /** set the value at the sepcified index. */
  public void set(Index i, boolean value) {
    storage[i.currentElement()] = value;
  }

  /** not legal, throw ForbiddenConversionException */
  public double getDouble(Index i) { throw new ForbiddenConversionException(); }
  /** not legal, throw ForbiddenConversionException */
  public void setDouble(Index i, double value) { throw new ForbiddenConversionException(); }

   /** not legal, throw ForbiddenConversionException */
  public float getFloat(Index i) { throw new ForbiddenConversionException(); }
  /** not legal, throw ForbiddenConversionException */
  public void setFloat(Index i, float value) { throw new ForbiddenConversionException(); }

  /** not legal, throw ForbiddenConversionException */
  public long getLong(Index i) { throw new ForbiddenConversionException(); }
  /** not legal, throw ForbiddenConversionException */
  public void setLong(Index i, long value) { throw new ForbiddenConversionException(); }

  /** not legal, throw ForbiddenConversionException */
  public int getInt(Index i) { throw new ForbiddenConversionException(); }
  /** not legal, throw ForbiddenConversionException */
  public void setInt(Index i, int value) { throw new ForbiddenConversionException(); }

  /** not legal, throw ForbiddenConversionException */
  public short getShort(Index i) { throw new ForbiddenConversionException(); }
  /** not legal, throw ForbiddenConversionException */
  public void setShort(Index i, short value) { throw new ForbiddenConversionException(); }

  /** not legal, throw ForbiddenConversionException */
  public byte getByte(Index i) { throw new ForbiddenConversionException(); }
  /** not legal, throw ForbiddenConversionException */
  public void setByte(Index i, byte value) { throw new ForbiddenConversionException(); }

  public boolean getBoolean(Index i) { return storage[i.currentElement()]; }
  public void setBoolean(Index i, boolean value) { storage[i.currentElement()] = value; }

  /** not legal, throw ForbiddenConversionException */
  public char getChar(Index i) { throw new ForbiddenConversionException(); }
  /** not legal, throw ForbiddenConversionException */
  public void setChar(Index i, char value) { throw new ForbiddenConversionException(); }

  public Object getObject(Index i) { return storage[i.currentElement()]; }
  public void setObject(Index i, Object value) { storage[i.currentElement()] = (Boolean) value; }

  // package private : mostly for iterators
  public double getDouble(int index) {throw new ForbiddenConversionException(); }
  public void setDouble(int index, double value) { throw new ForbiddenConversionException(); }

  public float getFloat(int index) { throw new ForbiddenConversionException(); }
  public void setFloat(int index, float value) { throw new ForbiddenConversionException(); }

  public long getLong(int index) {throw new ForbiddenConversionException(); }
  public void setLong(int index, long value) { throw new ForbiddenConversionException(); }

  public int getInt(int index) { throw new ForbiddenConversionException(); }
  public void setInt(int index, int value) { throw new ForbiddenConversionException(); }

  public short getShort(int index) { throw new ForbiddenConversionException(); }
  public void setShort(int index, short value) { throw new ForbiddenConversionException(); }

  public byte getByte(int index) { throw new ForbiddenConversionException(); }
  public void setByte(int index, byte value) {throw new ForbiddenConversionException(); }

  public char getChar(int index) { throw new ForbiddenConversionException(); }
  public void setChar(int index, char value) { throw new ForbiddenConversionException(); }

  public boolean getBoolean(int index) { return storage[index]; }
  public void setBoolean(int index, boolean value) {storage[index] = value; }

  public Object getObject(int index) { return getBoolean(index); }
  public void setObject(int index, Object value) { storage[index] = (Boolean) value; }

    /** Concrete implementation of Array specialized for byte, rank 0. */
  public static class D0 extends ArrayBoolean {
    private Index0D ix;
    /** Constructor. */
    public D0 () {
      super(new int [] {});
      ix = (Index0D) indexCalc;
    }
    private D0 (Index i, boolean[] store) {
      super(i, store);
      ix = (Index0D) indexCalc;
    }
    /** get the value. */
    public boolean get() {
      return storage[ix.currentElement()];
    }
    /** set the value. */
    public void set(boolean value) {
      storage[ix.currentElement()] = value;
    }
  }

  /** Concrete implementation of Array specialized for boolean, rank 1. */
  public static class D1 extends ArrayBoolean {
    private Index1D ix;
    /** Constructor for array of shape {len0}. */
    public D1 (int len0) {
      super(new int [] {len0});
      ix = (Index1D) indexCalc;
    }
    private D1 (Index i, boolean[] store) {
      super(i, store);
      ix = (Index1D) indexCalc;
    }
    /** get the value. */
    public boolean get(int i) {
      return storage[ix.setDirect(i)];
    }
    /** set the value. */
    public void set(int i, boolean value) {
      storage[ix.setDirect(i)] = value;
    }
  }

  /** Concrete implementation of Array specialized for boolean, rank 2. */
  public static class D2 extends ArrayBoolean {
    private Index2D ix;
    /** Constructor for array of shape {len0,len1}. */
    public D2 (int len0, int len1) {
      super(new int [] {len0, len1});
      ix = (Index2D) indexCalc;
    }
    private D2 (Index i, boolean[] store) {
      super(i, store);
      ix = (Index2D) indexCalc;
    }
    /** get the value. */
    public boolean get(int i, int j) {
      return storage[ix.setDirect(i,j)];
    }
    /** set the value. */
    public void set(int i, int j, boolean value) {
      storage[ix.setDirect(i,j)] = value;
    }
  }

  /** Concrete implementation of Array specialized for boolean, rank 3. */
  public static class D3 extends ArrayBoolean {
    private Index3D ix;
    /** Constructor for array of shape {len0,len1,len2}. */
    public D3 (int len0, int len1, int len2) {
      super(new int [] {len0, len1, len2});
      ix = (Index3D) indexCalc;
    }
    private D3(Index i, boolean[] store) {
      super(i, store);
      ix = (Index3D) indexCalc;
    }
    /** get the value. */
    public boolean get(int i, int j, int k) {
      return storage[ix.setDirect(i,j,k)];
    }
    /** set the value. */
    public void set(int i, int j, int k, boolean value) {
      storage[ix.setDirect(i,j,k)] = value;
    }
  }

  /** Concrete implementation of Array specialized for boolean, rank 4. */
  public static class D4 extends ArrayBoolean {
    private Index4D ix;
    /** Constructor for array of shape {len0,len1,len2,len3}. */
    public D4 (int len0, int len1, int len2, int len3) {
      super(new int [] {len0, len1, len2, len3});
      ix = (Index4D) indexCalc;
    }
    private D4(Index i, boolean[] store) {
      super(i, store);
      ix = (Index4D) indexCalc;
    }
    /** get the value. */
    public boolean get(int i, int j, int k, int l) {
      return storage[ix.setDirect(i,j,k,l)];
    }
    /** set the value. */
    public void set(int i, int j, int k, int l, boolean value) {
      storage[ix.setDirect(i,j,k,l)] = value;
    }
  }

  /** Concrete implementation of Array specialized for boolean, rank 5. */
  public static class D5 extends ArrayBoolean {
    private Index5D ix;
    /** Constructor for array of shape {len0,len1,len2,len3,len4}. */
    public D5 (int len0, int len1, int len2, int len3, int len4) {
      super(new int [] {len0, len1, len2, len3, len4});
      ix = (Index5D) indexCalc;
    }
    private D5 (Index i, boolean[] store) {
      super(i, store);
      ix = (Index5D) indexCalc;
    }
    /** get the value. */
    public boolean get(int i, int j, int k, int l, int m) {
      return storage[ix.setDirect(i,j,k,l, m)];
    }
    /** set the value. */
    public void set(int i, int j, int k, int l, int m, boolean value) {
      storage[ix.setDirect(i,j,k,l, m)] = value;
    }
  }

  /** Concrete implementation of Array specialized for boolean, rank 6. */
  public static class D6 extends ArrayBoolean {
    private Index6D ix;
    /** Constructor for array of shape {len0,len1,len2,len3,len4,len5,}. */
    public D6 (int len0, int len1, int len2, int len3, int len4, int len5) {
      super(new int [] {len0, len1, len2, len3, len4, len5});
      ix = (Index6D) indexCalc;
    }
    private D6(Index i, boolean[] store) {
      super(i, store);
      ix = (Index6D) indexCalc;
    }
    /** get the value. */
    public boolean get(int i, int j, int k, int l, int m, int n) {
      return storage[ix.setDirect(i,j,k,l,m,n)];
    }
    /** set the value. */
    public void set(int i, int j, int k, int l, int m, int n, boolean value) {
      storage[ix.setDirect(i,j,k,l,m,n)] = value;
    }
  }

  /** Concrete implementation of Array specialized for boolean, rank 7. */
  public static class D7 extends ArrayBoolean {
    private Index7D ix;
    /** Constructor for array of shape {len0,len1,len2,len3,len4,len5,len6}. */
    public D7 (int len0, int len1, int len2, int len3, int len4, int len5, int len6) {
      super(new int [] {len0, len1, len2, len3, len4, len5, len6});
      ix = (Index7D) indexCalc;
    }
    private D7 (Index i, boolean[] store) {
      super(i, store);
      ix = (Index7D) indexCalc;
    }
    /** get the value. */
    public boolean get(int i, int j, int k, int l, int m, int n, int o) {
      return storage[ix.setDirect(i,j,k,l,m,n,o)];
    }
    /** set the value. */
    public void set(int i, int j, int k, int l, int m, int n, int o, boolean value) {
      storage[ix.setDirect(i,j,k,l,m,n,o)] = value;
    }
  }

}
