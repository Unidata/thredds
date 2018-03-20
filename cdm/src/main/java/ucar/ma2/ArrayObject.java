/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.ma2;

/**
 * Concrete implementation of Array specialized for Objects.
 * Data storage is with 1D java array of Objects.
 *
 * @see Array
 * @author caron
 */
public class ArrayObject extends Array {

  /** package private. use Array.factory() */
  static ArrayObject factory(DataType dtype, Class elemType, boolean isVlen, Index index) {
    return ArrayObject.factory(dtype, elemType, isVlen, index, null);
  }

  /* Create new ArrayObject with given indexImpl and backing store.
   * Should be private.
   * @param index use this Index
   * @param stor. use this storage. if null, allocate.
   * @return. new ArrayObject.D<rank> or ArrayObject object.
   */
  static ArrayObject factory(DataType dtype, Class elemType, boolean isVlen, Index index, Object[] storage) {
      if (index instanceof Index0D) {
          return new ArrayObject.D0(dtype, elemType, isVlen, index, storage);
      } else if (index instanceof Index1D) {
          return new ArrayObject.D1(dtype, elemType, isVlen, index, storage);
      } else if (index instanceof Index2D) {
          return new ArrayObject.D2(dtype, elemType, isVlen, index, storage);
      } else if (index instanceof Index3D) {
          return new ArrayObject.D3(dtype, elemType, isVlen, index, storage);
      } else if (index instanceof Index4D) {
          return new ArrayObject.D4(dtype, elemType, isVlen, index, storage);
      } else if (index instanceof Index5D) {
          return new ArrayObject.D5(dtype, elemType, isVlen, index, storage);
      } else if (index instanceof Index6D) {
          return new ArrayObject.D6(dtype, elemType, isVlen, index, storage);
      } else if (index instanceof Index7D) {
          return new ArrayObject.D7(dtype, elemType, isVlen, index, storage);
      } else {
          return new ArrayObject(dtype, elemType, isVlen, index, storage);
      }
  }

  ///////////////////////////////////////////////////////////////////////////////
  protected final boolean isVlen;
  protected final Class elementType;
  protected final Object[] storage;

  /**
  * Create a new Array of type Object and the given shape.
  * dimensions.length determines the rank of the new Array.
  * @param elementType the type of element, eg String
  * @param shape the shape of the Array.
  */
  public ArrayObject(DataType dtype, Class elementType, boolean isVlen, int[] shape) {
    super(dtype, shape);
    this.elementType = elementType;
    storage = new Object[(int) indexCalc.getSize()];
    this.isVlen = isVlen;
  }


  /**
  * Create a new Array of type Object and the given shape and storage.
  * dimensions.length determines the rank of the new Array.
  * @param elementType the type of element, eg String
  * @param shape the shape of the Array.
  */
  public ArrayObject(DataType dtype, Class elementType, boolean isVlen, int [] shape, Object[] storage) {
    super(dtype, shape);
    this.elementType = elementType;
    this.storage = storage;
    this.isVlen = isVlen;
  }

 /**
  * Create a new Array using the given IndexArray and backing store.
  * used for sections, and factory. Trusted package private.
  * @param elementType the type of element, eg String
  * @param ima use this IndexArray as the index
  * @param data use this as the backing store. if null, allocate
  */
  ArrayObject(DataType dtype, Class elementType, boolean isVlen, Index ima, Object[] data) {
    super(dtype, ima);
    this.elementType = elementType;
    this.isVlen = isVlen;

    if (data != null) {
      storage = data;
    } else {
      storage = new Object[(int) indexCalc.getSize()];
    }
  }

  /** create new Array with given indexImpl and the same backing store */
  protected Array createView( Index index) {
    return ArrayObject.factory( dataType, elementType, isVlen, index, storage);
  }

  @Override
  public Array copy() {
    Array newA = factory(getDataType(), getElementType(), isVlen(), Index.factory(getShape()));
    MAMath.copy(newA, this);
    return newA;
  }

  /** Get underlying primitive array storage. CAUTION! You may invalidate your warrentee! */
  public Object getStorage() { return storage; }

  // copy from javaArray to storage using the iterator: used by factory( Object);
  protected void copyFrom1DJavaArray(IndexIterator iter, Object javaArray) {
    Object[] ja = (Object []) javaArray;
    for (Object aJa : ja) iter.setObjectNext(aJa);
  }

  // copy to javaArray from storage using the iterator: used by copyToNDJavaArray;
  protected void copyTo1DJavaArray(IndexIterator iter, Object javaArray) {
    Object[] ja = (Object []) javaArray;
    for (int i=0; i<ja.length; i++)
      ja[i] = iter.getObjectNext();
  }

 /** Return the element class type */
 @Override
  public Class getElementType() { return elementType; }

  @Override
  public boolean isVlen() {
    return isVlen;
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

  /** not legal, throw ForbiddenConversionException */
  public boolean getBoolean(Index i) { throw new ForbiddenConversionException(); }
  /** not legal, throw ForbiddenConversionException */
  public void setBoolean(Index i, boolean value) { throw new ForbiddenConversionException(); }

  /** not legal, throw ForbiddenConversionException */
  public char getChar(Index i) { throw new ForbiddenConversionException(); }
  /** not legal, throw ForbiddenConversionException */
  public void setChar(Index i, char value) { throw new ForbiddenConversionException(); }

  public Object getObject(Index i) {
    return storage[i.currentElement()];
  }
  public void setObject(Index i, Object value) {
    storage[i.currentElement()] = value;
  }

    // trusted, assumes that individual dimension lengths have been checked
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

  public boolean getBoolean(int index) { throw new ForbiddenConversionException(); }
  public void setBoolean(int index, boolean value) { throw new ForbiddenConversionException(); }

  public Object getObject(int index) { return storage[index]; }
  public void setObject(int index, Object value) { storage[index] = value; }

  /** Concrete implementation of Array specialized for Objects, rank 0. */
  public static class D0 extends ArrayObject {
    private Index0D ix;
    /** Constructor. */
    public D0 (DataType dtype, Class elemType, boolean isVlen) {
      super(dtype, elemType, isVlen, new int [] {});
      ix = (Index0D) indexCalc;
    }
    private D0 (DataType dtype, Class elemType, boolean isVlen, Index i, Object[] store) {
      super(dtype, elemType, isVlen, i, store);
      ix = (Index0D) indexCalc;
    }
    /** get the value. */
    public Object get() {
      return storage[ix.currentElement()];
    }
    /** set the value. */
    public void set(Object value) {
      storage[ix.currentElement()] = value;
    }
  }

  /** Concrete implementation of Array specialized for Objects, rank 1. */
  public static class D1 extends ArrayObject {
    private Index1D ix;
    /** Constructor for array of shape {len0}. */
    public D1 (DataType dtype, Class elemType, boolean isVlen, int len0) {
      super(dtype, elemType, isVlen, new int [] {len0});
      ix = (Index1D) indexCalc;
    }
    private D1 (DataType dtype, Class elemType, boolean isVlen, Index i, Object[] store) {
      super(dtype, elemType, isVlen, i, store);
      ix = (Index1D) indexCalc;
    }
    /** get the value. */
    public Object get(int i) {
      return storage[ix.setDirect(i)];
    }
    /** set the value. */
    public void set(int i, Object value) {
      storage[ix.setDirect(i)] = value;
    }
  }

  /** Concrete implementation of Array specialized for Objects, rank 2. */
  public static class D2 extends ArrayObject {
    private Index2D ix;
    /** Constructor for array of shape {len0,len1}. */
    public D2 (DataType dtype, Class elemType, boolean isVlen, int len0, int len1) {
      super(dtype, elemType, isVlen, new int [] {len0, len1});
      ix = (Index2D) indexCalc;
    }
    private D2 (DataType dtype, Class elemType, boolean isVlen, Index i, Object[] store) {
      super(dtype, elemType, isVlen, i, store);
      ix = (Index2D) indexCalc;
    }
    /** get the value. */
    public Object get(int i, int j) {
      return storage[ix.setDirect(i,j)];
    }
    /** set the value. */
    public void set(int i, int j, Object value) {
      storage[ix.setDirect(i,j)] = value;
    }
  }

  /** Concrete implementation of Array specialized for Objects, rank 3. */
  public static class D3 extends ArrayObject {
    private Index3D ix;
    /** Constructor for array of shape {len0,len1,len2}. */
    public D3 (DataType dtype, Class elemType, boolean isVlen, int len0, int len1, int len2) {
      super(dtype, elemType, isVlen, new int [] {len0, len1, len2});
      ix = (Index3D) indexCalc;
    }
    private D3 (DataType dtype, Class elemType, boolean isVlen, Index i, Object[] store) {
      super(dtype, elemType, isVlen, i, store);
      ix = (Index3D) indexCalc;
    }
    /** get the value. */
    public Object get(int i, int j, int k) {
      return storage[ix.setDirect(i,j,k)];
    }
    /** set the value. */
    public void set(int i, int j, int k, Object value) {
      storage[ix.setDirect(i,j,k)] = value;
    }
  }

  /** Concrete implementation of Array specialized for Objects, rank 4. */
  public static class D4 extends ArrayObject {
    private Index4D ix;
    /** Constructor for array of shape {len0,len1,len2,len3}. */
    public D4 (DataType dtype, Class elemType, boolean isVlen, int len0, int len1, int len2, int len3) {
      super(dtype, elemType, isVlen, new int [] {len0, len1, len2, len3});
      ix = (Index4D) indexCalc;
    }
    private D4 (DataType dtype, Class elemType, boolean isVlen, Index i, Object[] store) {
      super(dtype, elemType, isVlen, i, store);
      ix = (Index4D) indexCalc;
    }
    /** get the value. */
    public Object get(int i, int j, int k, int l) {
      return storage[ix.setDirect(i,j,k,l)];
    }
    /** set the value. */
    public void set(int i, int j, int k, int l, Object value) {
      storage[ix.setDirect(i,j,k,l)] = value;
    }
  }

  /** Concrete implementation of Array specialized for Objects, rank 5. */
  public static class D5 extends ArrayObject {
    private Index5D ix;
    /** Constructor for array of shape {len0,len1,len2,len3,len4}. */
    public D5 (DataType dtype, Class elemType, boolean isVlen, int len0, int len1, int len2, int len3, int len4) {
      super(dtype, elemType, isVlen, new int [] {len0, len1, len2, len3, len4});
      ix = (Index5D) indexCalc;
    }
    private D5 (DataType dtype, Class elemType, boolean isVlen, Index i, Object[] store) {
      super(dtype, elemType, isVlen, i, store);
      ix = (Index5D) indexCalc;
    }
    /** get the value. */
    public Object get(int i, int j, int k, int l, int m) {
      return storage[ix.setDirect(i,j,k,l, m)];
    }
    /** set the value. */
    public void set(int i, int j, int k, int l, int m, Object value) {
      storage[ix.setDirect(i,j,k,l, m)] = value;
    }
  }

  /** Concrete implementation of Array specialized for Objects, rank 6. */
  public static class D6 extends ArrayObject {
    private Index6D ix;
    /** Constructor for array of shape {len0,len1,len2,len3,len4,len5,}. */
    public D6 (DataType dtype, Class elemType, boolean isVlen, int len0, int len1, int len2, int len3, int len4, int len5) {
      super(dtype, elemType, isVlen, new int [] {len0, len1, len2, len3, len4, len5});
      ix = (Index6D) indexCalc;
    }
    private D6 (DataType dtype, Class elemType, boolean isVlen, Index i, Object[] store) {
      super(dtype, elemType, isVlen, i, store);
      ix = (Index6D) indexCalc;
    }
    /** get the value. */
    public Object get(int i, int j, int k, int l, int m, int n) {
      return storage[ix.setDirect(i,j,k,l,m,n)];
    }
    /** set the value. */
    public void set(int i, int j, int k, int l, int m, int n, Object value) {
      storage[ix.setDirect(i,j,k,l,m,n)] = value;
    }
  }

  /** Concrete implementation of Array specialized for Objects, rank 7. */
  public static class D7 extends ArrayObject {
    private Index7D ix;
    /** Constructor for array of shape {len0,len1,len2,len3,len4,len5,len6}. */
    public D7 (DataType dtype, Class elemType, boolean isVlen, int len0, int len1, int len2, int len3, int len4, int len5, int len6) {
      super(dtype, elemType, isVlen, new int [] {len0, len1, len2, len3, len4, len5, len6});
      ix = (Index7D) indexCalc;
    }
    private D7 (DataType dtype, Class elemType, boolean isVlen, Index i, Object[] store) {
      super(dtype, elemType, isVlen, i, store);
      ix = (Index7D) indexCalc;
    }
    /** get the value. */
    public Object get(int i, int j, int k, int l, int m, int n, int o) {
      return storage[ix.setDirect(i,j,k,l,m,n,o)];
    }
    /** set the value. */
    public void set(int i, int j, int k, int l, int m, int n, int o, Object value) {
      storage[ix.setDirect(i,j,k,l,m,n,o)] = value;
    }
  }

}
