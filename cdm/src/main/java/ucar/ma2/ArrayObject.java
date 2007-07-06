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
 * Concrete implementation of Array specialized for Objects.
 * Data storage is with 1D java array of Objects.
 *
 * @see Array
 * @author caron
 */
public class ArrayObject extends Array {

  /** package private. use Array.factory() */
  static ArrayObject factory(Class classType, Index index) {
    return ArrayObject.factory(classType, index, null);
  }

  /* Create new ArrayObject with given indexImpl and backing store.
   * Should be private.
   * @param index use this Index
   * @param stor. use this storage. if null, allocate.
   * @return. new ArrayObject.D<rank> or ArrayObject object.
   */
  static ArrayObject factory(Class classType, Index index, Object[] storage) {
    switch (index.getRank()) {
      case 0 : return new ArrayObject.D0(classType, index, storage);
      case 1 : return new ArrayObject.D1(classType, index, storage);
      case 2 : return new ArrayObject.D2(classType, index, storage);
      case 3 : return new ArrayObject.D3(classType, index, storage);
      case 4 : return new ArrayObject.D4(classType, index, storage);
      case 5 : return new ArrayObject.D5(classType, index, storage);
      case 6 : return new ArrayObject.D6(classType, index, storage);
      case 7 : return new ArrayObject.D7(classType, index, storage);
      default : return new ArrayObject(classType, index, storage);
    }
  }

  ///////////////////////////////////////////////////////////////////////////////
  protected Class elementType;
  protected Object[] storage;

  /**
  * Create a new Array of type Object and the given shape.
  * dimensions.length determines the rank of the new Array.
  * @param elementType the type of element, eg String
  * @param shape the shape of the Array.
  */
  public ArrayObject(Class elementType, int [] shape) {
    super(shape);
    this.elementType = elementType;
    storage = new Object[(int) indexCalc.getSize()];
  }


  /** create new Array with given indexImpl and the same backing store */
  Array createView( Index index) {
    return ArrayObject.factory( elementType, index, storage);
  }

 /**
  * Create a new Array using the given IndexArray and backing store.
  * used for sections, and factory. Trusted package private.
  * @param elementType the type of element, eg String
  * @param ima use this IndexArray as the index
  * @param data use this as the backing store. if null, allocate
  */
  ArrayObject(Class elementType, Index ima, Object[] data) {
    super(ima);
    this.elementType = elementType;

    if (data != null) {
      storage = data;
    } else {
      storage = new Object[(int) indexCalc.getSize()];
    }
  }

  /** Get underlying primitive array storage. CAUTION! You may invalidate your warrentee! */
  public Object getStorage() { return storage; }

  // copy from javaArray to storage using the iterator: used by factory( Object);
  void copyFrom1DJavaArray(IndexIterator iter, Object javaArray) {
    Object[] ja = (Object []) javaArray;
    for (int i=0; i<ja.length; i++)
      iter.setObjectNext( ja[i]);
  }

  // copy to javaArray from storage using the iterator: used by copyToNDJavaArray;
  void copyTo1DJavaArray(IndexIterator iter, Object javaArray) {
    Object[] ja = (Object []) javaArray;
    for (int i=0; i<ja.length; i++)
      ja[i] = iter.getObjectNext();
  }

 /** Return the element class type */
  public Class getElementType() { return elementType; }

    /** get the value at the specified index.
  public Object get(Index i) {
    return storage[i.currentElement()];
  }
    /** set the value at the specified index.
  public void set(Index i, Object value) {
    storage[i.currentElement()] = value;
  } */

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

  Object getObject(int index) { return storage[index]; }
  void setObject(int index, Object value) { storage[index] = value; }

  /** Concrete implementation of Array specialized for Objects, rank 0. */
  public static class D0 extends ArrayObject {
    private Index0D ix;
    /** Constructor. */
    public D0 (Class classType) {
      super(classType, new int [] {});
      ix = (Index0D) indexCalc;
    }
    private D0 (Class classType, Index i, Object[] store) {
      super(classType, i, store);
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
    public D1 (Class classType, int len0) {
      super(classType, new int [] {len0});
      ix = (Index1D) indexCalc;
    }
    private D1 (Class classType, Index i, Object[] store) {
      super(classType, i, store);
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
    public D2 (Class classType, int len0, int len1) {
      super(classType, new int [] {len0, len1});
      ix = (Index2D) indexCalc;
    }
    private D2 (Class classType, Index i, Object[] store) {
      super(classType, i, store);
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
    public D3 (Class classType, int len0, int len1, int len2) {
      super(classType, new int [] {len0, len1, len2});
      ix = (Index3D) indexCalc;
    }
    private D3 (Class classType, Index i, Object[] store) {
      super(classType, i, store);
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
    public D4 (Class classType, int len0, int len1, int len2, int len3) {
      super(classType, new int [] {len0, len1, len2, len3});
      ix = (Index4D) indexCalc;
    }
    private D4 (Class classType, Index i, Object[] store) {
      super(classType, i, store);
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
    public D5 (Class classType, int len0, int len1, int len2, int len3, int len4) {
      super(classType, new int [] {len0, len1, len2, len3, len4});
      ix = (Index5D) indexCalc;
    }
    private D5 (Class classType, Index i, Object[] store) {
      super(classType, i, store);
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
    public D6 (Class classType, int len0, int len1, int len2, int len3, int len4, int len5) {
      super(classType, new int [] {len0, len1, len2, len3, len4, len5});
      ix = (Index6D) indexCalc;
    }
    private D6 (Class classType, Index i, Object[] store) {
      super(classType, i, store);
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
    public D7 (Class classType, int len0, int len1, int len2, int len3, int len4, int len5, int len6) {
      super(classType, new int [] {len0, len1, len2, len3, len4, len5, len6});
      ix = (Index7D) indexCalc;
    }
    private D7 (Class classType, Index i, Object[] store) {
      super(classType, i, store);
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