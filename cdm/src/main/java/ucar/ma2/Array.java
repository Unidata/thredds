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

import java.util.List;
import java.util.ArrayList;

/**
 * Superclass for implementations of multidimensional arrays. An Array has a <b>classType</b> which gives
 *  the Class of its elements, and a <b>shape</b> which describes the number of elements in each index.
 *  The <b>rank</b> is the number of indices. A <b>scalar</b> Array has rank = 0. An
 *  Array may have arbitrary rank. The Array <b>size</b> is the total number of elements, which must be less than
 *  2^31 (about 2x10^9).
 * <p>
 * Actual data storage is done with Java 1D arrays and stride index calculations.
 *  This makes our Arrays rectangular, i.e. no "ragged arrays" where different elements
 *  can have different lengths as in Java multidimensional arrays, which are arrays of arrays.
 *  <p>
 * Each primitive Java type (boolean, byte, char, short, int, long, float, double) has a corresponding concrete
 *  implementation, e.g. ArrayBoolean, ArrayDouble. Reference types are all implemented using the ArrayObject class,
 *  with the exceptions of the reference types that correspond to the primitive types, eg Double.class is mapped to
 *  double.class.
 *  <p>
 * For efficiency, each Array type implementation has concrete subclasses for ranks 0-7, eg ArrayDouble.D0 is a double
 *  array of rank 0, ArrayDouble.D1 is a double array of rank 1, etc. These type and rank specific classes are convenient
 *  to work with when you know the type and rank of the Array. Ranks greater than 7 are handled by the type-specific
 *  superclass e.g. ArrayDouble. The Array class itself is used for fully general handling of any type and rank array.
 *  Use the Array.factory() methods to create Arrays in a general way.
 * <p>
 * The stride index calculations allow <b>logical views</b> to be efficiently implemented, eg subset, transpose, slice, etc.
 *  These views use the same data storage as the original Array they are derived from. The index stride calculations are
 *  equally efficient for any chain of logical views.
 * <p>
 * The type, shape and backing storage of an Array are immutable.
 *  The data itself is read or written using an Index or an IndexIterator, which stores any needed state information
 *  for efficient traversal. This makes use of Arrays thread-safe (as long as you dont share the Index or IndexIterator)
 *  except for the possibility of non-atomic read/write on long/doubles. If this is the case, you should probably
 *  synchronize your calls. Presumably 64-bit CPUs will make those operations atomic also.
 *
 * @see Index
 * @see IndexIterator
 *
 * @author caron
 */
public abstract class Array {
    public static final Index scalarIndex = new Index0D( new int[0]); // immutable, so can be shared

/* implementation notes.
  Could create interface for Ranges, ScatterIndex and pass array of that (?)
 */

  /**
   * Generate new Array with given type and shape and zeroed storage.
   * @param dataType instance of DataType.
   * @param shape shape of the array.
   * @return new Array<type> or Array<type>.D<rank> if 0 <= rank <= 7.
   */
  static public Array factory( DataType dataType, int [] shape) {
    Index index = Index.factory(shape);
    return factory( dataType.getPrimitiveClassType(), index);
  }

  /**
   * Generate new Array with given type and shape and zeroed storage.
   * @param classType element Class type, eg double.class.
   * @param shape shape of the array.
   * @return new Array<type> or Array<type>.D<rank> if 0 <= rank <= 7.
   */
  static public Array factory( Class classType, int [] shape) {
    Index index = Index.factory(shape);
    return factory( classType, index);
  }



  /**
   * Generate new Array with given type and shape and an Index that allways return 0.
   * @param classType element Class type, eg double.class.
   * @param shape shape of the array.
   * @param storage primitive array of correct type of length 1
   * @return new Array<type> or Array<type>.D<rank> if 0 <= rank <= 7.
   */
  static public Array factoryConstant( Class classType, int [] shape, Object storage) {
    Index index = new IndexConstant( shape);

    if ((classType == double.class) || (classType == Double.class))
      return new ArrayDouble(index, (double []) storage);
    else if ((classType == float.class) || (classType == Float.class))
      return new ArrayFloat(index, (float []) storage);
    else if ((classType == long.class) || (classType == Long.class))
      return new ArrayLong(index, (long []) storage);
    else if ((classType == int.class) || (classType == Integer.class))
      return new ArrayInt(index, (int []) storage);
    else if ((classType == short.class) || (classType == Short.class))
      return new ArrayShort(index, (short []) storage);
     else if ((classType == byte.class) || (classType == Byte.class))
      return new ArrayByte(index, (byte []) storage);
    else if ((classType == char.class) || (classType == Character.class))
      return new ArrayChar(index, (char []) storage);
    else if ((classType == boolean.class) || (classType == Boolean.class))
      return new ArrayBoolean(index, (boolean []) storage);
    else
      return new ArrayObject(classType, index, (Object []) storage);
  }

  /* generate new Array with given type and shape and zeroed storage */
  static private Array factory( Class classType, Index index) {
    if ((classType == double.class) || (classType == Double.class))
      return ArrayDouble.factory(index);
    else if ((classType == float.class) || (classType == Float.class))
      return ArrayFloat.factory(index);
    else if ((classType == long.class) || (classType == Long.class))
      return ArrayLong.factory(index);
    else if ((classType == int.class) || (classType == Integer.class))
      return ArrayInt.factory(index);
    else if ((classType == short.class) || (classType == Short.class))
      return ArrayShort.factory(index);
     else if ((classType == byte.class) || (classType == Byte.class))
      return ArrayByte.factory(index);
    else if ((classType == char.class) || (classType == Character.class))
      return ArrayChar.factory(index);
    else if ((classType == boolean.class) || (classType == Boolean.class))
      return ArrayBoolean.factory(index);
    else
      return ArrayObject.factory(classType, index);
  }

  /** Generate new Array with given type, shape, storage.
   * This should be package private, but is exposed for efficiency.
   * Normally use factory( Class classType, int [] shape) instead.
   * storage must be 1D array of type classType.
   * storage.length must equal product of shapes
   * storage data needs to be in canonical order
   *
   * @param classType element class type, eg double.class. Corresponding Object types like Double.class are
   *   mapped to double.class. Any reference types use ArrayObject.
   * @param shape array shape
   * @param storage 1D java array of type classType, except object types like Double.class are mapped to
   *   their corresponding primitive type, eg double.class. So the primitive
   * @return Array of given  type, shape and storage
   * @exception IllegalArgumentException storage.length != product of shapes
   * @exception ClassCastException wrong storage type
   */
  static public Array factory( Class classType, int [] shape, Object storage) {
    Index indexCalc = Index.factory(shape);
    return factory( classType, indexCalc, storage);
  }

  static private Array factory( Class classType, Index indexCalc, Object storage) {
    if ((classType == double.class) || (classType == Double.class))
      return ArrayDouble.factory(indexCalc, (double []) storage);
    else if ((classType == float.class) || (classType == Float.class))
      return ArrayFloat.factory(indexCalc, (float []) storage);
    else if ((classType == long.class) || (classType == Long.class))
      return ArrayLong.factory(indexCalc, (long []) storage);
    else if ((classType == int.class) || (classType == Integer.class))
      return ArrayInt.factory(indexCalc, (int []) storage);
    else if ((classType == short.class) || (classType == Short.class))
      return ArrayShort.factory(indexCalc, (short []) storage);
    else if ((classType == byte.class) || (classType == Byte.class))
      return ArrayByte.factory(indexCalc, (byte []) storage);
    else if ((classType == char.class) || (classType == Character.class))
      return ArrayChar.factory(indexCalc, (char []) storage);
    else if ((classType == boolean.class) || (classType == Boolean.class))
      return ArrayBoolean.factory(indexCalc, (boolean []) storage);
    else
      return ArrayObject.factory(classType, indexCalc, (Object []) storage);
  }

  /** Generate a new Array from a java array of any rank and type.
   *  This makes a COPY of the data values of javaArray.
   *  LOOK: not sure this works for reference types.
   *
   * @param javaArray scalar Object or a java array of any rank and type
   * @return Array of the appropriate rank and type, with the data copied from javaArray.
   */
  static public Array factory( Object javaArray) {
      // get the rank and type
    int rank_ = 0;
    Class componentType = javaArray.getClass();
    while(componentType.isArray()) {
      rank_++;
      componentType = componentType.getComponentType();
    }
    /* if( rank_ == 0)
      throw new IllegalArgumentException("Array.factory: not an array");
    if( !componentType.isPrimitive())
      throw new UnsupportedOperationException("Array.factory: not a primitive array"); */

      // get the shape
    int count = 0;
    int [] shape = new int[rank_];
    Object jArray = javaArray;
    Class cType = jArray.getClass();
    while(cType.isArray()) {
      shape[ count++] = java.lang.reflect.Array.getLength(jArray);
      jArray = java.lang.reflect.Array.get(jArray, 0);
      cType = jArray.getClass();
    }

    // create the Array
    Array aa = factory( componentType, shape);

    // copy the original array
    IndexIterator aaIter = aa.getIndexIterator();
    reflectArrayCopyIn( javaArray, aa, aaIter);

    return aa;
  }

  static private void reflectArrayCopyIn(Object jArray, Array aa, IndexIterator aaIter) {
    Class cType = jArray.getClass().getComponentType();
    if (cType.isPrimitive()) {
      aa.copyFrom1DJavaArray( aaIter, jArray);  // subclass does type-specific copy
    } else {
      for (int i=0; i< java.lang.reflect.Array.getLength(jArray); i++)  // recurse
        reflectArrayCopyIn(java.lang.reflect.Array.get(jArray, i), aa, aaIter);
    }
  }

  static private void reflectArrayCopyOut(Object jArray, Array aa, IndexIterator aaIter) {
    Class cType = jArray.getClass().getComponentType();
    if (cType.isPrimitive()) {
      aa.copyTo1DJavaArray( aaIter, jArray);  // subclass does type-specific copy
    } else {
      for (int i=0; i< java.lang.reflect.Array.getLength(jArray); i++)  // recurse
        reflectArrayCopyOut(java.lang.reflect.Array.get(jArray, i), aa, aaIter);
    }
  }

  /**
   * Cover for System.arraycopy(). Works with the underlying data arrays. Exposed for efficiency; use at your
   *  own risk.
   *  LOOK: not sure this works for reference types.
   * @param arraySrc copy from here
   * @param srcPos starting at
   * @param arrayDst copy to here
   * @param dstPos starting at
   * @param len number of elements to copy
   */
  static public void arraycopy( Array arraySrc, int srcPos, Array arrayDst, int dstPos, int len) {
    System.arraycopy( arraySrc.getStorage(), srcPos, arrayDst.getStorage(), dstPos, len);
  }

  /////////////////////////////////////////////////////
  protected final Index indexCalc;
  protected final int rank;

  // for subclasses only
  protected Array(int [] shape) {
    rank = shape.length;
    indexCalc = Index.factory(shape);
  }

  protected Array(Index index) {
    rank = index.getRank();
    indexCalc = index;
  }

  /** Get an Index object used for indexed access of this Array.
   * @see Index
   * @return an Index for this Array
   */
  public Index getIndex() { return (Index) indexCalc.clone(); }

  /** Get an index iterator for traversing the array in canonical order.
   * @see IndexIterator
   * @return an IndexIterator for this Array
   */
  public IndexIterator getIndexIterator() { return indexCalc.getIndexIterator(this); }

  /**
   * Get the number of dimensions of the array.
   * @return number of dimensions of the array
   */
  public int getRank() { return rank;}

   /**
   * Get the shape: length of array in each dimension.
   *
   * @return array whose length is the rank of this
   * Array and whose elements represent the length of each of its indices.
   */
  public int [] getShape() { return indexCalc.getShape(); }

  /**
   * Get the total number of elements in the array.
   * @return total number of elements in the array
   */
  public long getSize() { return indexCalc.getSize(); }

  /** Get an index iterator for traversing a section of the array in canonical order.
   * This is equivalent to Array.section(ranges).getIterator();
   * @param ranges list of Ranges that specify the array subset.
   *   Must be same rank as original Array.
   *   A particular Range: 1) may be a subset, or 2) may be null, meaning use entire Range.
   *
   * @return an IndexIterator over the named range.
   * @throws InvalidRangeException if ranges is invalid
   */
  public IndexIterator getRangeIterator(List<Range> ranges) throws InvalidRangeException {
    return section(ranges).getIndexIterator();
  }

  /** Get an index iterator for traversing the array in arbitrary order.
   *  Use this if you dont care what order the elements are returned, eg if you are summing an Array.
   *  To get an iteration in order,  use getIndexIterator(), which returns a fast iterator if possible.
   * @see #getIndexIterator
   * @return  an IndexIterator for traversing the array in arbitrary order.
   */
  public IndexIterator getIndexIteratorFast() {
    return indexCalc.getIndexIteratorFast(this);
  }

 /** Get the element class type of this Array
  * @return the class of the element
  */
  public abstract Class getElementType();

  /** create new Array with given Index and the same backing store
   * @param index use this Index
   * @return a view of the Array using the given Index
   */
  abstract Array createView( Index index);

  /** Get underlying primitive array storage.
   *  Exposed for efficiency, use at your own risk.
   * @return underlying primitive array storage
   */
  public abstract Object getStorage();

  // used to create Array from java array
  abstract void copyFrom1DJavaArray(IndexIterator iter, Object javaArray);
  abstract void copyTo1DJavaArray(IndexIterator iter, Object javaArray);

   /**
    * Create a new Array as a subsection of this Array, with rank reduction.
    * No data is moved, so the new Array references the same backing store as the original.
    * @param ranges list of Ranges that specify the array subset.
    *   Must be same rank as original Array.
    *   A particular Range: 1) may be a subset, or 2) may be null, meaning use entire Range.
    *   If Range[dim].length == 1, then the rank of the resulting Array is reduced at that dimension.
    * @return the new Array
    * @throws InvalidRangeException if ranges is invalid
    */
  public Array section( List<Range> ranges) throws InvalidRangeException {
    return createView( indexCalc.section(ranges));
  }

  /**
   * Create a new Array as a subsection of this Array, with rank reduction.
   * No data is moved, so the new Array references the same backing store as the original.
   * <p>
   * @param origin int array specifying the starting index. Must be same rank as original Array.
   * @param shape  int array specifying the extents in each dimension.
   *	This becomes the shape of the returned Array. Must be same rank as original Array.
   *   If shape[dim] == 1, then the rank of the resulting Array is reduced at that dimension.
   * @return the new Array
   * @throws InvalidRangeException if ranges is invalid
   */
  public Array section( int [] origin, int [] shape) throws InvalidRangeException {
    return section( origin, shape, null);
  }

    /**
     * Create a new Array as a subsection of this Array, with rank reduction.
     * No data is moved, so the new Array references the same backing store as the original.
     * <p>
     * @param origin int array specifying the starting index. Must be same rank as original Array.
     * @param shape  int array specifying the extents in each dimension.
     *	This becomes the shape of the returned Array. Must be same rank as original Array.
     *   If shape[dim] == 1, then the rank of the resulting Array is reduced at that dimension.
     * @param stride int array specifying the strides in each dimension. If null, assume all ones.
     * @return the new Array
     * @throws InvalidRangeException if ranges is invalid
     */
   public Array section( int [] origin, int [] shape, int[] stride) throws InvalidRangeException {
    List<Range> ranges = new ArrayList<Range>( origin.length);
    if (stride == null) {
      stride = new int[ origin.length];
      for (int i=0; i<stride.length; i++) stride[i] = 1;
    }
    for (int i=0; i<origin.length; i++)
      ranges.add( new Range(origin[i], origin[i]+stride[i]*shape[i]-1, stride[i]));
    return createView( indexCalc.section(ranges));
  }

   /**
    * Create a new Array as a subsection of this Array, without rank reduction.
    * No data is moved, so the new Array references the same backing store as the original.
    * @param ranges list of Ranges that specify the array subset.
    *   Must be same rank as original Array.
    *   A particular Range: 1) may be a subset, or 2) may be null, meaning use entire Range.
    * @return the new Array
    * @throws InvalidRangeException if ranges is invalid
    */
  public Array sectionNoReduce( List<Range> ranges) throws InvalidRangeException {
    return createView( indexCalc.sectionNoReduce(ranges));
  }

   /**
    * Create a new Array as a subsection of this Array, without rank reduction.
    * No data is moved, so the new Array references the same backing store as the original.
    * @param origin int array specifying the starting index. Must be same rank as original Array.
    * @param shape  int array specifying the extents in each dimension.
    *	This becomes the shape of the returned Array. Must be same rank as original Array.
    * @param stride int array specifying the strides in each dimension. If null, assume all ones.
    * @return the new Array
    * @throws InvalidRangeException if ranges is invalid
    */
  public Array sectionNoReduce( int [] origin, int [] shape, int[] stride) throws InvalidRangeException {
    List<Range> ranges = new ArrayList<Range>( origin.length);
    if (stride == null) {
      stride = new int[ origin.length];
      for (int i=0; i<stride.length; i++) stride[i] = 1;
    }
    for (int i=0; i<origin.length; i++)
      ranges.add( new Range(origin[i], origin[i]+stride[i]*shape[i]-1, stride[i]));
    return createView( indexCalc.sectionNoReduce(ranges));
  }

  /**
   * Create a new Array using same backing store as this Array, by
   * fixing the specified dimension at the specified index value. This reduces rank by 1.
   * @param dim which dimension to fix
   * @param value at what index value
   * @return a new Array
   */
  public Array slice(int dim, int value) {
    int[] origin = new int[rank];
    int[] shape = getShape();
    origin[dim] = value;
    shape[dim] = 1;
    try {
      return sectionNoReduce(origin, shape, null).reduce(dim);  // preserve other dim 1
    } catch (InvalidRangeException e) {
      throw new IllegalArgumentException();
    }
  }

   /**
    * Create a copy of this Array, copying the data so that physical order is the same as
    * logical order
    * @return the new Array
    */
    public Array copy() {
      Array newA = factory( getElementType(), getShape());
      MAMath.copy(newA, this);
      return newA;
    }

  /**
   * This gets the equivilent java array of the wanted type, in correct order.
   * It avoids copying if possible.
   * @param wantType returned object will be an array of this type. This must be convertible to it.
   * @return copyTo1DJavaArray
   */
   public Object get1DJavaArray(Class wantType) {
     if (wantType == getElementType()) {
       if (indexCalc.fastIterator) return getStorage(); // already in order
       else return copyTo1DJavaArray(); // gotta copy
     }

     // gotta convert to new type
     Array newA = factory( wantType, getShape());
     MAMath.copy(newA, this);
     return newA.getStorage();
   }

   /**
    * Copy this array to a 1D Java primitive array of type getElementType(), with the physical order
    * of the result the same as logical order.
    * @return a Java 1D array of type getElementType().
    */
  public Object copyTo1DJavaArray() {
    Array newA = copy();
    return newA.getStorage();
  }

   /**
    * Copy this array to a n-Dimensioanl Java primitive array of type getElementType()
    * and rank getRank(). Makes a copy of the data.
    * @return a Java ND array of type getElementType().
    */
  public Object copyToNDJavaArray() {
    Object javaArray;
    try {
      javaArray = java.lang.reflect.Array.newInstance(getElementType(), getShape());
    } catch (Exception e) {
      throw new IllegalArgumentException();
    }

    // copy data
    IndexIterator iter = getIndexIterator();
    reflectArrayCopyOut( javaArray, this, iter);

    return javaArray;
  }

 /**
  * Create a new Array using same backing store as this Array, by
  * flipping the index so that it runs from shape[index]-1 to 0.
  * @param dim dimension to flip
  * @return the new Array
  */
  public Array flip( int dim) {
    return createView( indexCalc.flip(dim));
  }

  /**
  * Create a new Array using same backing store as this Array, by
  * transposing two of the indices.
  * @param dim1 transpose these two indices
  * @param dim2 transpose these two indices
  * @return the new Array
  */
  public Array transpose( int dim1, int dim2)  {
    return createView( indexCalc.transpose(dim1, dim2));
  }

   /**
     * Create a new Array using same backing store as this Array, by
     * permuting the indices.
     * @param dims the old index dims[k] becomes the new kth index.
     * @return the new Array
     * @exception IllegalArgumentException: wrong rank or dim[k] not valid
     */
  public Array permute( int[] dims)  {
    return createView( indexCalc.permute(dims));
  }

  /**
   * Create a new Array by copying this Array to a new one with given shape
   * @param shape the new shape
   * @return the new Array
   * @exception IllegalArgumentException a and b are not conformable
   */
  public Array reshape( int [] shape) {
    Array result = factory( this.getElementType(), shape);
    if (result.getSize() != getSize())
      throw new IllegalArgumentException("reshape arrays must have same total size");

    Array.arraycopy( this, 0, result, 0, (int) getSize());
    return result;
  }

  /**
     * Create a new Array using same backing store as this Array, by
     * eliminating any dimensions with length one.
     * @return the new Array
     */
  public Array reduce() {
    return createView( indexCalc.reduce());
  }

  /**
    * Create a new Array using same backing store as this Array, by
    * eliminating the specified dimension.
    * @param dim dimension to eliminate: must be of length one, else IllegalArgumentException
    * @return the new Array
    */
  public Array reduce(int dim) {
    return createView( indexCalc.reduce(dim));
  }

  //////////////////////////////////////////////////////////////
  /**
   * Set the name of one of the indices.
   * @param dim which index?
   * @param indexName name of index
   */
  public void setIndexName( int dim, String indexName) {
    indexCalc.setIndexName( dim, indexName);
  }

  /**
   * Get the name of one of the indices.
   * @param dim which index?
   * @return name of index, or null if none.
   */
  public String getIndexName( int dim) {
    return indexCalc.getIndexName( dim);
  }

  /** This is present so that Array is-a MultiArray: equivalent to sectionNoReduce().
  public Array read(int [] origin, int [] shape) throws InvalidRangeException {
    return sectionNoReduce(origin, shape);
  }
  /** This is present so that Array is-a MultiArray: returns itself.
  public Array read() { return this; } */

      ///////////////////////////////////////////////////
    /* these are the type-specific element accessors */
    ///////////////////////////////////////////////////

    /** Get the array element at the current element of ima, as a double.
     * @param ima Index with current element set
     * @return value at <code>index</code> cast to double if necessary.
     */
  public abstract double getDouble(Index ima);

    /** Set the array element at the current element of ima.
     * @param ima Index with current element set
     * @param value the new value; cast to underlying data type if necessary.
     */
  public abstract void setDouble(Index ima, double value);

    /** Get the array element at the current element of ima, as a float.
     * @param ima Index with current element set
     * @return value at <code>index</code> cast to float if necessary.
     */
   public abstract float getFloat(Index ima);

    /** Set the array element at the current element of ima.
     * @param ima Index with current element set
     * @param value the new value; cast to underlying data type if necessary.
     */
   public abstract void setFloat(Index ima, float value);

    /** Get the array element at the current element of ima, as a long.
     * @param ima Index with current element set
     * @return value at <code>index</code> cast to long if necessary.
     */
   public abstract long getLong(Index ima);

    /** Set the array element at the current element of ima.
     * @param ima Index with current element set
     * @param value the new value; cast to underlying data type if necessary.
     */
   public abstract void setLong(Index ima, long value);

    /** Get the array element at the current element of ima, as a int.
     * @param ima Index with current element set
     * @return value at <code>index</code> cast to int if necessary.
     */
   public abstract int getInt(Index ima);

    /** Set the array element at the current element of ima.
     * @param ima Index with current element set
     * @param value the new value; cast to underlying data type if necessary.
     */
   public abstract void setInt(Index ima, int value);

    /** Get the array element at the current element of ima, as a short.
     * @param ima Index with current element set
     * @return value at <code>index</code> cast to short if necessary.
     */
   public abstract short getShort(Index ima);

    /** Set the array element at the current element of ima.
     * @param ima Index with current element set
     * @param value the new value; cast to underlying data type if necessary.
     */
   public abstract void setShort(Index ima, short value);

    /** Get the array element at the current element of ima, as a byte.
     * @param ima Index with current element set
     * @return value at <code>index</code> cast to float if necessary.
     */
   public abstract byte getByte(Index ima);

    /** Set the array element at the current element of ima.
     * @param ima Index with current element set
     * @param value the new value; cast to underlying data type if necessary.
     */
   public abstract void setByte(Index ima, byte value);

    /** Get the array element at the current element of ima, as a char.
     * @param ima Index with current element set
     * @return value at <code>index</code> cast to char if necessary.
     */
   public abstract char getChar(Index ima);

    /** Set the array element at the current element of ima.
     * @param ima Index with current element set
     * @param value the new value; cast to underlying data type if necessary.
     */
   public abstract void setChar(Index ima, char value);

    /** Get the array element at the current element of ima, as a boolean.
     * @param ima Index with current element set
     * @return value at <code>index</code> cast to boolean if necessary.
     * @exception ForbiddenConversionException if underlying array not boolean
     */
   public abstract boolean getBoolean(Index ima);

    /** Set the array element at the current element of ima.
     * @param ima Index with current element set
     * @param value the new value; cast to underlying data type if necessary.
     * @exception ForbiddenConversionException if underlying array not boolean
     */
   public abstract void setBoolean(Index ima, boolean value);

    /**
     * Get the array element at index as an Object.
     * The returned value is wrapped in an object, eg Double for double
     * @param ima element Index
     * @return Object value at <code>index</code>
     * @exception ArrayIndexOutOfBoundsException if index incorrect rank or out of bounds
     */
   public abstract Object getObject(Index ima);

    /**
     * Set the array element at index to the specified value.
     * the value must be passed wrapped in the appropriate Object (eg Double for double)
     * @param ima Index with current element set
     * @param value the new value.
     * @exception ArrayIndexOutOfBoundsException if index incorrect rank or out of bounds
     * @exception ClassCastException if Object is incorrect type
     */
  abstract  public void setObject(Index ima, Object value);

  // package private
  //// these are for optimized access with no need to check index values
  //// elem is the index into the backing data
  abstract double getDouble(int elem);
  abstract void setDouble(int elem, double val);
  abstract float getFloat(int elem);
  abstract void setFloat(int elem, float val);
  abstract long getLong(int elem);
  abstract void setLong(int elem, long value);
  abstract int getInt(int elem);
  abstract void setInt(int elem, int value);
  abstract short getShort(int elem);
  abstract void setShort(int elem, short value);
  abstract byte getByte(int elem);
  abstract void setByte(int elem, byte value);
  abstract char getChar(int elem);
  abstract void setChar(int elem, char value);
  abstract boolean getBoolean(int elem);
  abstract void setBoolean(int elem, boolean value);
  abstract Object getObject(int elem);
  abstract void setObject(int elem, Object value);  

  public String toString() {
    StringBuffer sbuff = new StringBuffer();
    IndexIterator ii = getIndexIterator();
    while (ii.hasNext()) {
      Object data = ii.getObjectNext();
      sbuff.append(data);
      sbuff.append(" ");
    }
    return sbuff.toString();
  }

  public String shapeToString() {
    int[] shape = getShape();
    if (shape.length == 0) return "";
    StringBuffer sb = new StringBuffer();
    sb.append('(');
    for (int i = 0; i < shape.length; i++) {
      int s = shape[i];
      if (i > 0) sb.append(",");
      sb.append(s);
    }
    sb.append(')');
    return sb.toString();
  }

}

