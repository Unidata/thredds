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

/**
 * Concrete implementation of Array specialized for bytes.
 * Data storage is with 1D java array of bytes.
 *
 * issues: what should we do if a conversion loses accuracy? nothing ? Exception ?
 *
 * @see Array
 * @author caron
 */
public class ArrayByte extends Array {

  // package private. use Array.factory() */
  static ArrayByte factory(Index index) {
    return ArrayByte.factory(index, null);
  }

  /* create new ArrayByte with given indexImpl and backing store.
   * Should be private.
   * @param index use this Index
   * @param stor. use this storage. if null, allocate.
   * @return. new ArrayDouble.D<rank> or ArrayDouble object.
   */
  static ArrayByte factory( Index index, byte [] storage) {
    switch (index.getRank()) {
      case 0 : return new ArrayByte.D0(index, storage);
      case 1 : return new ArrayByte.D1(index, storage);
      case 2 : return new ArrayByte.D2(index, storage);
      case 3 : return new ArrayByte.D3(index, storage);
      case 4 : return new ArrayByte.D4(index, storage);
      case 5 : return new ArrayByte.D5(index, storage);
      case 6 : return new ArrayByte.D6(index, storage);
      case 7 : return new ArrayByte.D7(index, storage);
      default : return new ArrayByte(index, storage);
    }
  }

  ///////////////////////////////////////////////////////////////////////////////
  protected byte[] storage;

  /**
  * Create a new Array of type byte and the given shape.
  * dimensions.length determines the rank of the new Array.
  * @param dimensions the shape of the Array.
  */
  public ArrayByte(int [] dimensions) {
    super(dimensions);
    storage = new byte[(int)indexCalc.getSize()];
  }

  /**
  * Create a new Array using the given IndexArray and backing store.
  * used for sections. Trusted package private.
  * @param ima use this IndexArray as the index
  * @param data use this as the backing store
  */
  ArrayByte(Index ima, byte [] data) {
    super(ima);
    /* replace by something better
    if (ima.getSize() != data.length)
      throw new IllegalArgumentException("bad data length");  */
    if (data != null)
      storage = data;
    else
      storage = new byte[(int)ima.getSize()];
  }

  /** create new Array with given indexImpl and same backing store */
  Array createView( Index index) {
    return ArrayByte.factory( index, storage);
  }

  /* Get underlying primitive array storage. CAUTION! You may invalidate your warrentee! */
  public Object getStorage() { return storage; }

  // copy from javaArray to storage using the iterator: used by factory( Object);
  void copyFrom1DJavaArray(IndexIterator iter, Object javaArray) {
    byte[] ja = (byte []) javaArray;
    for (int i=0; i<ja.length; i++)
      iter.setByteNext( ja[i]);
  }

  // copy to javaArray from storage using the iterator: used by copyToNDJavaArray;
  void copyTo1DJavaArray(IndexIterator iter, Object javaArray) {
    byte[] ja = (byte []) javaArray;
    for (int i=0; i<ja.length; i++)
      ja[i] = iter.getByteNext();
  }

  public ByteBuffer getDataAsByteBuffer() {
    return ByteBuffer.wrap((byte[]) get1DJavaArray(byte.class));
  }

 /** Return the element class type */
  public Class getElementType() { return byte.class; }

    /** get the value at the specified index. */
  public byte get(Index i) {
    return storage[i.currentElement()];
  }
    /** set the value at the sepcified index. */
  public void set(Index i, byte value) {
    storage[i.currentElement()] = value;
  }


  /* double
  public double getDouble(int[] index) {
    return (double) storage[indexCalc.element(index)];
  }
  public void setDouble(int[] index, double value) {
    storage[indexCalc.element(index)] = (byte) value;
  } */
  public double getDouble(Index i) {
    return (double) storage[i.currentElement()];
  }
  public void setDouble(Index i, double value) {
    storage[i.currentElement()] = (byte) value;
  }

  /* float
  public float getFloat(int[] index) {
    return (float) storage[indexCalc.element(index)];
  }
  public void setFloat(int[] index, float value) {
    storage[indexCalc.element(index)] = (byte) value;
  } */
  public float getFloat(Index i) {
    return (float) storage[i.currentElement()];
  }
  public void setFloat(Index i, float value) {
    storage[i.currentElement()] = (byte) value;
  }

  /* long
  public long getLong(int[] index) {
    return (long) storage[indexCalc.element(index)];
  }
  public void setLong(int[] index, long value) {
    storage[indexCalc.element(index)] = (byte) value;
  } */
  public long getLong(Index i) {
    return (long) storage[i.currentElement()];
  }
  public void setLong(Index i, long value) {
    storage[i.currentElement()] = (byte) value;
  }

  /* int
  public int getInt(int[] index) {
    return (int) storage[indexCalc.element(index)];
  }
  public void setInt(int[] index, int value) {
    storage[indexCalc.element(index)] = (byte) value;
  } */
  public int getInt(Index i) {
    return (int) storage[i.currentElement()];
  }
  public void setInt(Index i, int value) {
    storage[i.currentElement()] = (byte) value;
  }

  /* short
  public short getShort(int[] index) {
    return (short) storage[indexCalc.element(index)];
  }
  public void setShort(int[] index, short value) {
    storage[indexCalc.element(index)] = (byte) value;
  } */
  public short getShort(Index i) {
    return (short) storage[i.currentElement()];
  }
  public void setShort(Index i, short value) {
    storage[i.currentElement()] = (byte) value;
  }

  /* byte
  public byte getByte(int[] index) {
    return (byte) storage[indexCalc.element(index)];
  }
  public void setByte(int[] index, byte value) {
    storage[indexCalc.element(index)] = (byte) value;
  } */
  public byte getByte(Index i) {
    return storage[i.currentElement()];
  }
  public void setByte(Index i, byte value) {
    storage[i.currentElement()] = value;
  }

  /* char
  public char getChar(int[] index) {
    return (char) storage[indexCalc.element(index)];
  }
  public void setChar(int[] index, char value) {
    storage[indexCalc.element(index)] = (byte) value;
  } */
  public char getChar(Index i) {
    return (char) storage[i.currentElement()];
  }
  public void setChar(Index i, char value) {
    storage[i.currentElement()] = (byte) value;
  }

  /** not legal, throw ForbiddenConversionException */
  public boolean getBoolean(Index i) {
    throw new ForbiddenConversionException();
  }
  /** not legal, throw ForbiddenConversionException */
  public void setBoolean(Index i, boolean value) {
     throw new ForbiddenConversionException();
  }

  /* Object
  public Object getObject(int [] index) {
    return new Byte(storage[indexCalc.element(index)]);
  }
  public void setObject(int [] index, Object value) {
    storage[indexCalc.element(index)] = ((Number)value).byteValue();
  } */
  public Object getObject(Index i) { return storage[i.currentElement()]; }
  public void setObject(Index i, Object value) { storage[i.currentElement()] = ((Number)value).byteValue(); }

    // package private : mostly for iterators
  double getDouble(int index) {return (double) storage[index]; }
  void setDouble(int index, double value) { storage[index] = (byte) value; }

  float getFloat(int index) { return storage[index]; }
  void setFloat(int index, float value) { storage[index] = (byte) value;}

  long getLong(int index) {return (long) storage[index];}
  void setLong(int index, long value) { storage[index] = (byte) value;}

  int getInt(int index) { return (int) storage[index]; }
  void setInt(int index, int value) { storage[index] = (byte) value;}

  short getShort(int index) { return (short) storage[index]; }
  void setShort(int index, short value) { storage[index] = (byte) value; }

  byte getByte(int index) { return storage[index]; }
  void setByte(int index, byte value) {storage[index] = value;}

  char getChar(int index) { return (char) storage[index];}
  void setChar(int index, char value) { storage[index] = (byte) value; }

  boolean getBoolean(int index) { throw new ForbiddenConversionException(); }
  void setBoolean(int index, boolean value) {throw new ForbiddenConversionException(); }

  Object getObject(int index) { return getByte(index); }
  void setObject(int index, Object value) { storage[index] = ((Number)value).byteValue(); }

  /** Concrete implementation of Array specialized for byte, rank 0. */
  public static class D0 extends ArrayByte {
    private Index0D ix;
    /** Constructor. */
    public D0 () {
      super(new int [] {});
      ix = (Index0D) indexCalc;
    }
    private D0 (Index i, byte[] store) {
      super(i, store);
      ix = (Index0D) indexCalc;
    }
    /** get the value. */
    public byte get() {
      return storage[ix.currentElement()];
    }
    /** set the value. */
    public void set(byte value) {
      storage[ix.currentElement()] = value;
    }
  }

  /** Concrete implementation of Array specialized for byte, rank 1. */
  public static class D1 extends ArrayByte {
    private Index1D ix;
    /** Constructor for array of shape {len0}. */
    public D1 (int len0) {
      super(new int [] {len0});
      ix = (Index1D) indexCalc;
    }
    private D1 (Index i, byte[] store) {
      super(i, store);
      ix = (Index1D) indexCalc;
    }
    /** get the value. */
    public byte get(int i) {
      return storage[ix.setDirect(i)];
    }
    /** set the value. */
    public void set(int i, byte value) {
      storage[ix.setDirect(i)] = value;
    }
  }

  /** Concrete implementation of Array specialized for byte, rank 2. */
  public static class D2 extends ArrayByte {
    private Index2D ix;
    /** Constructor for array of shape {len0,len1}. */
    public D2 (int len0, int len1) {
      super(new int [] {len0, len1});
      ix = (Index2D) indexCalc;
    }
    private D2 (Index i, byte[] store) {
      super(i, store);
      ix = (Index2D) indexCalc;
    }
    /** get the value. */
    public byte get(int i, int j) {
      return storage[ix.setDirect(i,j)];
    }
    /** set the value. */
    public void set(int i, int j, byte value) {
      storage[ix.setDirect(i,j)] = value;
    }
  }

  /** Concrete implementation of Array specialized for byte, rank 3. */
  public static class D3 extends ArrayByte {
    private Index3D ix;
    /** Constructor for array of shape {len0,len1,len2}. */
    public D3 (int len0, int len1, int len2) {
      super(new int [] {len0, len1, len2});
      ix = (Index3D) indexCalc;
    }
    private D3 (Index i, byte[] store) {
      super(i, store);
      ix = (Index3D) indexCalc;
    }
    /** get the value. */
    public byte get(int i, int j, int k) {
      return storage[ix.setDirect(i,j,k)];
    }
    /** set the value. */
    public void set(int i, int j, int k, byte value) {
      storage[ix.setDirect(i,j,k)] = value;
    }
  }

  /** Concrete implementation of Array specialized for byte, rank 4. */
  public static class D4 extends ArrayByte {
    private Index4D ix;
    /** Constructor for array of shape {len0,len1,len2,len3}. */
    public D4 (int len0, int len1, int len2, int len3) {
      super(new int [] {len0, len1, len2, len3});
      ix = (Index4D) indexCalc;
    }
    private D4 (Index i, byte[] store) {
      super(i, store);
      ix = (Index4D) indexCalc;
    }
    /** get the value. */
    public byte get(int i, int j, int k, int l) {
      return storage[ix.setDirect(i,j,k,l)];
    }
    /** set the value. */
    public void set(int i, int j, int k, int l, byte value) {
      storage[ix.setDirect(i,j,k,l)] = value;
    }
  }

  /** Concrete implementation of Array specialized for byte, rank 5. */
  public static class D5 extends ArrayByte {
    private Index5D ix;
    /** Constructor for array of shape {len0,len1,len2,len3,len4}. */
    public D5 (int len0, int len1, int len2, int len3, int len4) {
      super(new int [] {len0, len1, len2, len3, len4});
      ix = (Index5D) indexCalc;
    }
    private D5 (Index i, byte[] store) {
      super(i, store);
      ix = (Index5D) indexCalc;
    }
    /** get the value. */
    public byte get(int i, int j, int k, int l, int m) {
      return storage[ix.setDirect(i,j,k,l, m)];
    }
    /** set the value. */
    public void set(int i, int j, int k, int l, int m, byte value) {
      storage[ix.setDirect(i,j,k,l, m)] = value;
    }
  }

  /** Concrete implementation of Array specialized for byte, rank 6. */
  public static class D6 extends ArrayByte {
    private Index6D ix;
    /** Constructor for array of shape {len0,len1,len2,len3,len4,len5,}. */
    public D6 (int len0, int len1, int len2, int len3, int len4, int len5) {
      super(new int [] {len0, len1, len2, len3, len4, len5});
      ix = (Index6D) indexCalc;
    }
    private D6 (Index i, byte[] store) {
      super(i, store);
      ix = (Index6D) indexCalc;
    }
    /** get the value. */
    public byte get(int i, int j, int k, int l, int m, int n) {
      return storage[ix.setDirect(i,j,k,l,m,n)];
    }
    /** set the value. */
    public void set(int i, int j, int k, int l, int m, int n, byte value) {
      storage[ix.setDirect(i,j,k,l,m,n)] = value;
    }
  }

  /** Concrete implementation of Array specialized for byte, rank 7. */
  public static class D7 extends ArrayByte {
    private Index7D ix;
    /** Constructor for array of shape {len0,len1,len2,len3,len4,len5,len6}. */
    public D7 (int len0, int len1, int len2, int len3, int len4, int len5, int len6) {
      super(new int [] {len0, len1, len2, len3, len4, len5, len6});
      ix = (Index7D) indexCalc;
    }
    private D7 (Index i, byte[] store) {
      super(i, store);
      ix = (Index7D) indexCalc;
    }
    /** get the value. */
    public byte get(int i, int j, int k, int l, int m, int n, int o) {
      return storage[ix.setDirect(i,j,k,l,m,n,o)];
    }
    /** set the value. */
    public void set(int i, int j, int k, int l, int m, int n, int o, byte value) {
      storage[ix.setDirect(i,j,k,l,m,n,o)] = value;
    }
  }
}
