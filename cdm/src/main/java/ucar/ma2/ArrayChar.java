// $Id$
/*
 * Copyright 1997-2006 Unidata Program Center/University Corporation for
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
 * Concrete implementation of Array specialized for chars.
 * Data storage is with 1D java array of chars.
 *
 * issues: what should we do if a conversion loses accuracy? nothing ? Exception ?
 *
 * @see Array
 * @author caron
 * @version $Revision$ $Date$
 */
public class ArrayChar extends Array {

  /** package private. use Array.factory() */
  static ArrayChar factory(Index index) {
    return ArrayChar.factory(index, null);
  }

  /* create new ArrayChar with given indexImpl and backing store.
   * Should be private.
   * @param index use this Index
   * @param stor. use this storage. if null, allocate.
   * @return. new ArrayDouble.D<rank> or ArrayDouble object.
   */
  static ArrayChar factory( Index index, char [] storage) {
    switch (index.getRank()) {
      case 0 : return new ArrayChar.D0(index, storage);
      case 1 : return new ArrayChar.D1(index, storage);
      case 2 : return new ArrayChar.D2(index, storage);
      case 3 : return new ArrayChar.D3(index, storage);
      case 4 : return new ArrayChar.D4(index, storage);
      case 5 : return new ArrayChar.D5(index, storage);
      case 6 : return new ArrayChar.D6(index, storage);
      case 7 : return new ArrayChar.D7(index, storage);
      default : return new ArrayChar(index, storage);
    }
  }

  ///////////////////////////////////////////////////////////////////////////////
  protected char[] storage;

  /**
  * Create a new Array of type char and the given shape.
  * dimensions.length determines the rank of the new Array.
  * @param dimensions the shape of the Array.
  */
  public ArrayChar(int [] dimensions) {
    super(dimensions);
    storage = new char[(int)indexCalc.getSize()];
  }

  /**
  * Create a new Array using the given IndexArray and backing store.
  * used for sections. Trusted package private.
  * @param ima use this IndexArray as the index
  * @param data use this as the backing store
  */
  ArrayChar(Index ima, char [] data) {
    super(ima);
    /* replace by something better
    if (ima.getSize() != data.length)
      throw new IllegalArgumentException("bad data length"); */
    if (data != null)
      storage = data;
    else
      storage = new char[(int)ima.getSize()];
  }

  /** create new Array with given indexImpl and same backing store */
  Array createView( Index index) {
    return ArrayChar.factory( index, storage);
  }

    // used only by copyTo1DJavaArray
  public Object getStorage() { return storage; }

    // copy from javaArray to storage using the iterator: used by factory( Object);
  void copyFrom1DJavaArray(IndexIterator iter, Object javaArray) {
    char[] ja = (char []) javaArray;
    for (int i=0; i<ja.length; i++)
      iter.setCharNext( ja[i]);
  }

  // copy to javaArray from storage using the iterator: used by copyToNDJavaArray;
  void copyTo1DJavaArray(IndexIterator iter, Object javaArray) {
    char[] ja = (char []) javaArray;
    for (int i=0; i<ja.length; i++)
      ja[i] = iter.getCharNext();
  }

 /** Return the element class type */
  public Class getElementType() { return char.class; }

    /** get the value at the specified index. */
  public char get(Index i) {
    return storage[i.currentElement()];
  }
    /** set the value at the sepcified index. */
  public void set(Index i, char value) {
    storage[i.currentElement()] = value;
  }

  /* double
  public double getDouble(int[] index) {
    return (double) storage[indexCalc.element(index)];
  }
  public void setDouble(int[] index, double value) {
    storage[indexCalc.element(index)] = (char) value;
  }  */
  public double getDouble(Index i) {
    return (double) storage[i.currentElement()];
  }
  public void setDouble(Index i, double value) {
    storage[i.currentElement()] = (char) value;
  }

  /* float
  public float getFloat(int[] index) {
    return (float) storage[indexCalc.element(index)];
  }
  public void setFloat(int[] index, float value) {
    storage[indexCalc.element(index)] = (char) value;
  }  */
  public float getFloat(Index i) {
    return (float) storage[i.currentElement()];
  }
  public void setFloat(Index i, float value) {
    storage[i.currentElement()] = (char) value;
  }

  /* long
  public long getLong(int[] index) {
    return (long) storage[indexCalc.element(index)];
  }
  public void setLong(int[] index, long value) {
    storage[indexCalc.element(index)] = (char) value;
  } */
  public long getLong(Index i) {
    return (long) storage[i.currentElement()];
  }
  public void setLong(Index i, long value) {
    storage[i.currentElement()] = (char) value;
  }

  /* int
  public int getInt(int[] index) {
    return (int) storage[indexCalc.element(index)];
  }
  public void setInt(int[] index, int value) {
    storage[indexCalc.element(index)] = (char) value;
  } */
  public int getInt(Index i) {
    return (int) storage[i.currentElement()];
  }
  public void setInt(Index i, int value) {
    storage[i.currentElement()] = (char) value;
  }

  /* short
  public short getShort(int[] index) {
    return (short) storage[indexCalc.element(index)];
  }
  public void setShort(int[] index, short value) {
    storage[indexCalc.element(index)] = (char) value;
  } */
  public short getShort(Index i) {
    return (short) storage[i.currentElement()];
  }
  public void setShort(Index i, short value) {
    storage[i.currentElement()] = (char) value;
  }

  /* byte
  public byte getByte(int[] index) {
    return (byte) storage[indexCalc.element(index)];
  }
  public void setByte(int[] index, byte value) {
    storage[indexCalc.element(index)] = (char) value;
  } */
  public byte getByte(Index i) {
    return (byte) storage[i.currentElement()];
  }
  public void setByte(Index i, byte value) {
    storage[i.currentElement()] = (char) value;
  }

  /* char
  public char getChar(int[] index) {
    return (char) storage[indexCalc.element(index)];
  }
  public void setChar(int[] index, char value) {
    storage[indexCalc.element(index)] = (char) value;
  } */
  public char getChar(Index i) {
    return (char) storage[i.currentElement()];
  }
  public void setChar(Index i, char value) {
    storage[i.currentElement()] = (char) value;
  }

  /** not legal, throw ForbiddenConversionException */
  public boolean getBoolean(Index i) {
    throw new ForbiddenConversionException();
  }
  /** not legal, throw ForbiddenConversionException */
  public void setBoolean(Index i, boolean value) {
     throw new ForbiddenConversionException();
  }

  public Object getObject(Index i) { return new Character(storage[i.currentElement()]); }
  public void setObject(Index i, Object value) { storage[i.currentElement()] = ((Character)value).charValue(); }

  /**
   * Create a String out of this rank one ArrayChar object.
   * If there is a null (0) value in the ArrayChar array, the String will end there.
   * The null is not returned as part of the String.
   *
   * @return String value of CharArray
   * @throws IllegalArgumentException if rank != 1
   */
  public String getString() {
    int rank = getRank();
    if (rank != 1)
      throw new IllegalArgumentException("ArayChar.getString rank must be 1");
    int strLen = getShape()[0];

    int count = 0;
    for (int k=0; k<strLen; k++) {
      if (0 == storage[k])
        break;
      count++;
    }
    return new String(storage, 0, count);
  }

   /**
   * Create a String out of this rank two ArrayChar object.
   * This treats the ArrayChar as a 1D array of Strings.
   * If there is a null (0) value in the ArrayChar array, the String will end there.
   * The null is not returned as part of the String.
   *
   * @param index index into 1D String array, must be < getShape(0).
   * @return String value
   * @throws IllegalArgumentException if rank != 2
   */
  public String getString(int index) {
    int rank = getRank();
    if (rank != 2)
      throw new IllegalArgumentException("ArayChar.getString rank must be 2");

    Index ima = getIndex();
    return getString( ima.set(index));
  }


  /**
   * Create a String out of this ArrayChar object. The rank must be 1 or greater.
   * If there is a null (0) value in the ArrayChar array, the String will end there.
   * The null is not returned as part of the String.
   *
   * If rank=1, then this will make a string out of the entire CharArray, ignoring ima.
   * If rank is greater than 1, then make a String out of the characters of the last dimension,
   * indexed by ima.  This method treats the CharArray like an array of Strings, and
   * allows you to iterate over them, eg for a 2D ArrayChar:
   * <p><code>
   *   ArrayChar ca;
   *   Index ima = ca.getIndex();
   *   for (int i=0; i<ca.getShape()[0]; i++)
   *     String s = ca.getString(ima.set0(i));
   * </code>
   * @return String value of CharArray
   * @throws IllegalArgumentException if rank != 1
   */
  public String getString(Index ima) {
    int rank = getRank();
    if (rank == 0)
      throw new IllegalArgumentException("ArayChar.getString rank must not be 0");
    if (rank == 1)
      return getString();

    int strLen = getShape()[rank-1];

    char [] carray = new char[ strLen];
    int count = 0;
    for (int k=0; k<strLen; k++) {
      ima.setDim(rank-1, k);
      carray[k] = getChar( ima);
      if (0 == carray[k])
        break;
      count++;
    }
    return new String(carray, 0, count);
  }


  /**
   * Set the ArrayChar values from the characters in the String. Rank must be 1.
   * If String longer than ArrayChar, ignore extra chars; if shorter, fill with 0.
   *
   * @param val set characters from this String
   * @throws IllegalArgumentException if rank != 2
   */
  public void setString(String val) {
    int rank = getRank();
    if (rank != 1)
      throw new IllegalArgumentException("ArayChar.setString rank must be 1");
    int arrayLen = getShape()[0];
    int strLen = Math.min(val.length(), arrayLen);

    for (int k=0; k<strLen; k++)
      storage[k] = val.charAt(k);

    char c = 0;
    for (int k=strLen; k<arrayLen; k++)
      storage[k] = c;
  }

  /**
   * Set the ArrayChar values from the characters in the String. Rank must be 2.
   * This treats the ArrayChar as a 1D array of Strings.
   * If String val longer than ArrayChar, ignore extra chars; if shorter, fill with 0.
   *
   * <p><code>
   *   String[] val = new String[n];
   *   ArrayChar ca;
   *   Index ima = ca.getIndex();
   *   for (int i=0; i<n; i++)
   *     ca.setString(i, val[i]);
   * </code>
   *
   * @param index index into 1D String array, must be < getShape(0).
   * @param val set chars from this sString
   */
  public void setString(int index, String val) {
    int rank = getRank();
    if (rank != 2)
      throw new IllegalArgumentException("ArayChar.setString rank must be 2");

    Index ima = getIndex();
    setString( ima.set(index), val);
  }


  /**
   * Set the ArrayChar values from the characters in the String. Rank must be 1 or greater.
   * If String longer than ArrayChar, ignore extra chars; if shorter, fill with 0.
   * If rank 1, set entire ArrayChar, ignoring ima.
   * If rank > 1, treat the ArrayChar like an array of Strings of rank-1, and set the
   * row indexed by ima. For example, rank 3:
   * <p><code>
   *   String[][] val;
   *   ArrayChar ca;
   *   Index ima = ca.getIndex();
   *   int rank0 = ca.getShape()[0];
   *   int rank1 = ca.getShape()[1];
   *
   *   for (int i=0; i<rank0; i++)
   *     for (int j=0; j<rank1; j++) {
   *       ima.set(i,j);
   *       ca.setString(ima, val[i][j]);
   *     }
   * </code>
   *
   * @param ima
   * @param val
   */
  public void setString(Index ima, String val) {
    int rank = getRank();
    if (rank == 0)
      throw new IllegalArgumentException("ArayChar.setString rank must not be 0");
    int arrayLen = getShape()[rank-1];
    int strLen = Math.min(val.length(), arrayLen);

    int count = 0;
    for (int k=0; k<strLen; k++) {
      ima.setDim(rank-1, k);
      setChar( ima, val.charAt(k));
      count++;
    }
    char c = 0;
    for (int k=count; k<arrayLen; k++) {
      ima.setDim(rank-1, k);
      setChar( ima, c);
    }
  }


    // package private : mostly for iterators
  double getDouble(int index) {return (double) storage[index]; }
  void setDouble(int index, double value) { storage[index] = (char) value; }

  float getFloat(int index) { return storage[index]; }
  void setFloat(int index, float value) { storage[index] = (char) value;}

  long getLong(int index) {return (long) storage[index];}
  void setLong(int index, long value) { storage[index] = (char) value;}

  int getInt(int index) { return (int) storage[index]; }
  void setInt(int index, int value) { storage[index] = (char) value;}

  short getShort(int index) { return (short) storage[index]; }
  void setShort(int index, short value) { storage[index] = (char) value; }

  byte getByte(int index) { return (byte) storage[index]; }
  void setByte(int index, byte value) {storage[index] = (char) value;}

  char getChar(int index) { return (char) storage[index];}
  void setChar(int index, char value) { storage[index] = (char) value; }

  boolean getBoolean(int index) { throw new ForbiddenConversionException(); }
  void setBoolean(int index, boolean value) {throw new ForbiddenConversionException(); }

  Object getObject(int index) { return new Character( getChar( index)); }
  void setObject(int index, Object value) { storage[index] = ((Character)value).charValue(); }

  /** Concrete implementation of Array specialized for char, rank 0. */
  public static class D0 extends ArrayChar {
    private Index0D ix;
    /** Constructor. */
    public D0 () {
      super(new int [] {});
      ix = (Index0D) indexCalc;
    }
    private D0 (Index i, char[] store) {
      super(i, store);
      ix = (Index0D) indexCalc;
    }
    /** get the value. */
    public char get() {
      return storage[ix.currentElement()];
    }
    /** set the value. */
    public void set(char value) {
      storage[ix.currentElement()] = value;
    }
  }

  /** Concrete implementation of Array specialized for char, rank 1. */
  public static class D1 extends ArrayChar {
    private Index1D ix;
    /** Constructor for array of shape {len0}. */
    public D1 (int len0) {
      super(new int [] {len0});
      ix = (Index1D) indexCalc;
    }
    private D1 (Index i, char[] store) {
      super(i, store);
      ix = (Index1D) indexCalc;
    }
    /** get the value. */
    public char get(int i) {
      return storage[ix.setDirect(i)];
    }
    /** set the value. */
    public void set(int i, char value) {
      storage[ix.setDirect(i)] = value;
    }
  }

  /** Concrete implementation of Array specialized for char, rank 2. */
  public static class D2 extends ArrayChar {
    private Index2D ix;
    /** Constructor for array of shape {len0,len1}. */
    public D2 (int len0, int len1) {
      super(new int [] {len0, len1});
      ix = (Index2D) indexCalc;
    }
    private D2 (Index i, char[] store) {
      super(i, store);
      ix = (Index2D) indexCalc;
    }
    /** get the value. */
    public char get(int i, int j) {
      return storage[ix.setDirect(i,j)];
    }
    /** set the value. */
    public void set(int i, int j, char value) {
      storage[ix.setDirect(i,j)] = value;
    }
  }

  /** Concrete implementation of Array specialized for char, rank 3. */
  public static class D3 extends ArrayChar {
    private Index3D ix;
    /** Constructor for array of shape {len0,len1,len2}. */
    public D3 (int len0, int len1, int len2) {
      super(new int [] {len0, len1, len2});
      ix = (Index3D) indexCalc;
    }
    private D3(Index i, char[] store) {
      super(i, store);
      ix = (Index3D) indexCalc;
    }
    /** get the value. */
    public char get(int i, int j, int k) {
      return storage[ix.setDirect(i,j,k)];
    }
    /** set the value. */
    public void set(int i, int j, int k, char value) {
      storage[ix.setDirect(i,j,k)] = value;
    }
  }

  /** Concrete implementation of Array specialized for char, rank 4. */
  public static class D4 extends ArrayChar {
    private Index4D ix;
    /** Constructor for array of shape {len0,len1,len2,len3}. */
    public D4 (int len0, int len1, int len2, int len3) {
      super(new int [] {len0, len1, len2, len3});
      ix = (Index4D) indexCalc;
    }
    private D4 (Index i, char[] store) {
      super(i, store);
      ix = (Index4D) indexCalc;
    }
    /** get the value. */
    public char get(int i, int j, int k, int l) {
      return storage[ix.setDirect(i,j,k,l)];
    }
    /** set the value. */
    public void set(int i, int j, int k, int l, char value) {
      storage[ix.setDirect(i,j,k,l)] = value;
    }
  }

  /** Concrete implementation of Array specialized for char, rank 5. */
  public static class D5 extends ArrayChar {
    private Index5D ix;
    /** Constructor for array of shape {len0,len1,len2,len3,len4}. */
    public D5 (int len0, int len1, int len2, int len3, int len4) {
      super(new int [] {len0, len1, len2, len3, len4});
      ix = (Index5D) indexCalc;
    }
    private D5 (Index i, char[] store) {
      super(i, store);
      ix = (Index5D) indexCalc;
    }
    /** get the value. */
    public char get(int i, int j, int k, int l, int m) {
      return storage[ix.setDirect(i,j,k,l, m)];
    }
    /** set the value. */
    public void set(int i, int j, int k, int l, int m, char value) {
      storage[ix.setDirect(i,j,k,l, m)] = value;
    }
  }

  /** Concrete implementation of Array specialized for char, rank 6. */
  public static class D6 extends ArrayChar {
    private Index6D ix;
    /** Constructor for array of shape {len0,len1,len2,len3,len4,len5,}. */
    public D6 (int len0, int len1, int len2, int len3, int len4, int len5) {
      super(new int [] {len0, len1, len2, len3, len4, len5});
      ix = (Index6D) indexCalc;
    }
    private D6 (Index i, char[] store) {
      super(i, store);
      ix = (Index6D) indexCalc;
    }
    /** get the value. */
    public char get(int i, int j, int k, int l, int m, int n) {
      return storage[ix.setDirect(i,j,k,l,m,n)];
    }
    /** set the value. */
    public void set(int i, int j, int k, int l, int m, int n, char value) {
      storage[ix.setDirect(i,j,k,l,m,n)] = value;
    }
  }

  /** Concrete implementation of Array specialized for char, rank 7. */
  public static class D7 extends ArrayChar {
    private Index7D ix;
    /** Constructor for array of shape {len0,len1,len2,len3,len4,len5,len6}. */
    public D7 (int len0, int len1, int len2, int len3, int len4, int len5, int len6) {
      super(new int [] {len0, len1, len2, len3, len4, len5, len6});
      ix = (Index7D) indexCalc;
    }
    private D7 (Index i, char[] store) {
      super(i, store);
      ix = (Index7D) indexCalc;
    }
    /** get the value. */
    public char get(int i, int j, int k, int l, int m, int n, int o) {
      return storage[ix.setDirect(i,j,k,l,m,n,o)];
    }
    /** set the value. */
    public void set(int i, int j, int k, int l, int m, int n, int o, char value) {
      storage[ix.setDirect(i,j,k,l,m,n,o)] = value;
    }
  }

  ///////////////////////////////////////////////////////////////////////////////
  /**
   * Treat this Variable as an array of Strings, and iterate over all the strings in the array.
   * If rank == 0, will make single String of length 1.
   * @return Iterator over Strings.
   */
  public StringIterator getStringIterator() { return new StringIterator(); }

  /**
   * rank must be > 0
   */
  public class StringIterator {
    private IndexIterator ii = getIndexIterator();
    private int strLen;
    private char [] carray;

    StringIterator () {
      if (rank == 0)
        strLen = 1;
      else
        strLen = getShape()[rank-1];
      carray = new char[ strLen];
    }

    public int getNumElems() {
      return (int) getSize() / strLen;
    }

    public boolean hasNext() { return ii.hasNext(); }
    public String next() {
      int stop=strLen;
      for (int k=0; k<strLen; k++) {
        carray[k] = ii.getCharNext();
        if (0 == carray[k] && stop==strLen) // catch zero termination
          stop=k;
      }
      return new String(carray, 0, stop);
    }
  }

  /**
   * Make this into the equivilent 1D ArrayObject of Strings.
   */
  public ArrayObject make1DStringArray() {
    int nelems = (int) getSize() / getShape() [ getRank()-1];
    Array sarr = Array.factory( String.class, new int[] {nelems} );
    IndexIterator newsiter = sarr.getIndexIterator();

    ArrayChar.StringIterator siter = getStringIterator();
    while (siter.hasNext()) {
      newsiter.setObjectNext( siter.next());
    }
    return (ArrayObject) sarr;
  }

}
