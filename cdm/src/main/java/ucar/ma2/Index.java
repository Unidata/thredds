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

/**
 * Indexes for Multidimensional arrays. This refers to a particular element of an array.
 * <p/>
 * This is a generalization of index as int []. Its main function is
 * to do the index arithmetic to translate an n-dim index into a 1-dim
 * index.
 * The user obtains this by calling getIndex() on a Array.
 * The set() and seti() routines are convenience routines for 1-7 dim arrays.
 *
 * @author caron
 * @see Array
 */

public class Index implements Cloneable {

  /**
   * array shape
   */
  protected int[] shape;
  /**
   * array rank
   */
  protected int rank;
  /**
   * total number of elements
   */
  protected long size;
  /**
   * array stride
   */
  protected int[] stride;
  /**
   * element = offset + stride[0]*current[0] + ...
   */
  protected int offset;

  /**
   * current element's index, used only for the general case
   */
  protected int[] current;

  /**
   * index names (optional)
   */
  protected String[] name;

  /**
   * Iterator implementation
   */
  protected boolean fastIterator = true;

  /**
   * General case Index - use when you want to manipulate current elements yourself
   */
  protected Index(int rank) {
    shape = new int[rank];
    this.rank = rank;
    current = new int[rank];
    stride = new int[rank];
  }

  /**
   * constructor for subclasses only.
   */
  protected Index(int[] _shape) {

    this.shape = new int[_shape.length];  // optimization over clone
    System.arraycopy(_shape, 0, this.shape, 0, _shape.length);

    rank = shape.length;
    current = new int[rank];
    stride = new int[rank];
    size = computeStrides(shape, stride);
    offset = 0;
  }

  /**
   * constructor for subclasses only.
   */
  protected Index(int[] _shape, int[] _stride) {
    this.shape = new int[_shape.length];  // optimization over clone
    System.arraycopy(_shape, 0, this.shape, 0, _shape.length);

    this.stride = new int[_stride.length];  // optimization over clone
    System.arraycopy(_stride, 0, this.stride, 0, _stride.length);

    rank = shape.length;
    current = new int[rank];
    size = computeSize(shape);
    offset = 0;
  }

  /**
   * subclass specialization/optimization calculations
   */
  protected void precalc() {
  }

  /**
   * Create a new Index based on current one, except
   * flip the index so that it runs from shape[index]-1 to 0.
   *
   * @param index dimension to flip
   * @return new index with flipped dimension
   */
  Index flip(int index) {
    if ((index < 0) || (index >= rank))
      throw new IllegalArgumentException();

    Index i = (Index) this.clone();
    i.offset += stride[index] * (shape[index] - 1);
    i.stride[index] = -stride[index];

    i.fastIterator = false;
    i.precalc(); // any subclass-specific optimizations
    return i;
  }

  /**
   * create a new Index based on a subsection of this one, with rank reduction if
   * dimension length == 1.
   *
   * @param ranges array of Ranges that specify the array subset.
   *               Must be same rank as original Array.
   *               A particular Range: 1) may be a subset; 2) may be null, meaning use entire Range.
   * @return new Index, with same or smaller rank as original.
   * @throws InvalidRangeException if ranges dont match current shape
   */
  Index section(List<Range> ranges) throws InvalidRangeException {

    // check ranges are valid
    if (ranges.size() != rank)
      throw new InvalidRangeException("Bad ranges [] length");
    for (int ii = 0; ii < rank; ii++) {
      Range r = (Range) ranges.get(ii);
      if (r == null)
        continue;
      if ((r.first() < 0) || (r.first() >= shape[ii]))
        throw new InvalidRangeException("Bad range starting value at index " + ii + " == " + r.first());
      if ((r.last() < 0) || (r.last() >= shape[ii]))
        throw new InvalidRangeException("Bad range ending value at index " + ii + " == " + r.last());
    }

    int reducedRank = rank;
    for (Range r : ranges) {
      if ((r != null) && (r.length() == 1))
        reducedRank--;
    }
    Index newindex = Index.factory(reducedRank);
    newindex.offset = offset;

    // calc shape, size, and index transformations
    // calc strides into original (backing) store
    int newDim = 0;
    for (int ii = 0; ii < rank; ii++) {
      Range r = ranges.get(ii);
      if (r == null) {          // null range means use the whole original dimension
        newindex.shape[newDim] = shape[ii];
        newindex.stride[newDim] = stride[ii];
        if (name != null) newindex.name[newDim] = name[ii];
        newDim++;
      } else if (r.length() != 1) {
        newindex.shape[newDim] = r.length();
        newindex.stride[newDim] = stride[ii] * r.stride();
        newindex.offset += stride[ii] * r.first();
        if (name != null) newindex.name[newDim] = name[ii];
        newDim++;
      } else {
        newindex.offset += stride[ii] * r.first();   // constant due to rank reduction
      }
    }
    newindex.size = computeSize(newindex.shape);
    newindex.fastIterator = false;
    newindex.precalc(); // any subclass-specific optimizations
    return newindex;
  }

  /**
   * create a new Index based on a subsection of this one, without rank reduction.
   *
   * @param ranges list of Ranges that specify the array subset.
   *               Must be same rank as original Array.
   *               A particular Range: 1) may be a subset; 2) may be null, meaning use entire Range.
   * @return new Index, with same rank as original.
   * @throws InvalidRangeException if ranges dont match current shape
   */
  Index sectionNoReduce(List<Range> ranges) throws InvalidRangeException {

    // check ranges are valid
    if (ranges.size() != rank)
      throw new InvalidRangeException("Bad ranges [] length");
    for (int ii = 0; ii < rank; ii++) {
      Range r = (Range) ranges.get(ii);
      if (r == null)
        continue;
      if ((r.first() < 0) || (r.first() >= shape[ii]))
        throw new InvalidRangeException("Bad range starting value at index " + ii + " == " + r.first());
      if ((r.last() < 0) || (r.last() >= shape[ii]))
        throw new InvalidRangeException("Bad range ending value at index " + ii + " == " + r.last());
    }

    // allocate
    Index newindex = Index.factory(rank);
    newindex.offset = offset;

    // calc shape, size, and index transformations
    // calc strides into original (backing) store
    for (int ii = 0; ii < rank; ii++) {
      Range r = ranges.get(ii);
      if (r == null) {          // null range means use the whole original dimension
        newindex.shape[ii] = shape[ii];
        newindex.stride[ii] = stride[ii];
      } else {
        newindex.shape[ii] = r.length();
        newindex.stride[ii] = stride[ii] * r.stride();
        newindex.offset += stride[ii] * r.first();
      }
      if (name != null) newindex.name[ii] = name[ii];
    }
    newindex.size = computeSize(newindex.shape);
    newindex.fastIterator = false;
    newindex.precalc(); // any subclass-specific optimizations
    return newindex;
  }

  /**
   * Create a new Index based on current one by
   * eliminating any dimensions with length one.
   *
   * @return the new Index
   */
  Index reduce() {
    Index c = this;
    for (int ii = 0; ii < rank; ii++)
      if (shape[ii] == 1) {  // do this on the first one you find
        Index newc = c.reduce(ii);
        return newc.reduce();  // any more to do?
      }
    return c;
  }

  /**
   * Create a new Index based on current one by
   * eliminating the specified dimension;
   *
   * @param dim: dimension to eliminate: must be of length one, else IllegalArgumentException
   * @return the new Index
   */
  Index reduce(int dim) {
    if ((dim < 0) || (dim >= rank))
      throw new IllegalArgumentException("illegal reduce dim " + dim);
    if (shape[dim] != 1)
      throw new IllegalArgumentException("illegal reduce dim " + dim + " : length != 1");

    Index newindex = Index.factory(rank - 1);
    newindex.offset = offset;
    int count = 0;
    for (int ii = 0; ii < rank; ii++) {
      if (ii != dim) {
        newindex.shape[count] = shape[ii];
        newindex.stride[count] = stride[ii];
        if (name != null)
          newindex.name[count] = name[ii];

        count++;
      }
    }
    newindex.size = computeSize(newindex.shape);
    newindex.fastIterator = fastIterator;
    newindex.precalc();         // any subclass-specific optimizations
    return newindex;
  }

  /**
   * create a new Index based on current one, except
   * transpose two of the indices.
   *
   * @param index1 transpose these two indices
   * @param index2 transpose these two indices
   * @return new Index with transposed indices
   */
  Index transpose(int index1, int index2) {
    if ((index1 < 0) || (index1 >= rank))
      throw new IllegalArgumentException();
    if ((index2 < 0) || (index2 >= rank))
      throw new IllegalArgumentException();

    Index newIndex = (Index) this.clone();
    newIndex.stride[index1] = stride[index2];
    newIndex.stride[index2] = stride[index1];
    newIndex.shape[index1] = shape[index2];
    newIndex.shape[index2] = shape[index1];
    if (name != null) {
      newIndex.name[index1] = name[index2];
      newIndex.name[index2] = name[index1];
    }

    newIndex.fastIterator = false;
    newIndex.precalc(); // any subclass-specific optimizations
    return newIndex;
  }

  /**
   * create a new Index based on a permutation of the current indices
   *
   * @param dims: the old index dim[k] becomes the new kth index.
   * @return new Index with permuted indices
   */
  Index permute(int[] dims) {
    if (dims.length != shape.length)
      throw new IllegalArgumentException();
    for (int i = 0; i < dims.length; i++)
      if ((dims[i] < 0) || (dims[i] >= rank))
        throw new IllegalArgumentException();

    boolean isPermuted = false;
    Index newIndex = (Index) this.clone();
    for (int i = 0; i < dims.length; i++) {
      newIndex.stride[i] = stride[dims[i]];
      newIndex.shape[i] = shape[dims[i]];
      if (name != null) newIndex.name[i] = name[dims[i]];
      if (i != dims[i]) isPermuted = true;
    }

    newIndex.fastIterator = !isPermuted; // useful optimization
    newIndex.precalc(); // any subclass-specific optimizations
    return newIndex;
  }


  /**
   * Get the number of dimensions in the array.
   * @return the number of dimensions in the array.
   */
  public int getRank() {
    return rank;
  }

  /**
   * Get the shape: length of array in each dimension.
   * @return  the shape
   */
  public int[] getShape() {
    int[] result = new int[shape.length];  // optimization over clone
    System.arraycopy(shape, 0, result, 0, shape.length);
    return result;
  }

  /** Get the current element's index as an int [] LOOK why not ?
   public int [] getCurrentIndex() { return (int []) current.clone(); } */

  /**
   * Get an index iterator for traversing the array in canonical order.
   * @param maa the array to iterate through
   * @return an index iterator for traversing the array in canonical order.
   * @see IndexIterator
   */
  IndexIterator getIndexIterator(Array maa) {
    if (fastIterator)
      return new IteratorFast(size, maa);
    else
      return new IteratorImpl(maa);
  }

  IteratorFast getIndexIteratorFast(Array maa) {
    return new IteratorFast(size, maa);
  }

  /**
   * Get the total number of elements in the array.
   * @return the total number of elements in the array.
   */
  public long getSize() {
    return size;
  }

  /**
   * Get the current element's index into the 1D backing array.
   * @return the current element's index into the 1D backing array.
   */
  public int currentElement() {
    int value = offset;                 // NB: dont have to check each index again
    for (int ii = 0; ii < rank; ii++)    // general rank
      value += current[ii] * stride[ii];
    return value;
  }

  /*Get the current element's index.
  public int[] current() {
    return current;
  } */

  // only use from FasstIterator, where the indices are not permuted

  /**
   * currElement = offset + stride[0]*current[0] + ...
   * @param currElement set to this value
   */
  void setCurrentElement(int currElement) {
    currElement -= offset;
    for (int ii = 0; ii < rank; ii++) { // general rank
      current[ii] = currElement / stride[ii];
      currElement -= current[ii] * stride[ii];
    }
  }

  /** Use index[] to calculate the index into the 1D backing array.
   * Does not set the current element.
   *
   public int element(int [] index) {
   int value = offset;
   for(int ii = 0; ii < rank; ii++) {
   final int thisIndex = index[ii];
   if( thisIndex < 0 || thisIndex >= shape[ii])  // check each index
   throw new ArrayIndexOutOfBoundsException();
   value += thisIndex * stride[ii];
   }
   return value;
   } */

  /**
   * Increment the current element by 1. Used by IndexIterator.
   * General rank, with subclass specialization.
   *
   * @return currentElement()
   */
  protected int incr() {
    int digit = rank - 1;
    while (digit >= 0) {
      current[digit]++;
      if (current[digit] < shape[digit])
        break;                        // normal exit
      current[digit] = 0;               // else, carry
      digit--;
    }
    return currentElement();
  }


  /**
   * Set the current element's index. General-rank case.
   *
   * @param index set current value to these values
   * @return this, so you can use A.get(i.set(i))
   * @throws ArrayIndexOutOfBoundsException if index.length != rank.
   */
  public Index set(int[] index) {
    if (index.length != rank)
      throw new ArrayIndexOutOfBoundsException();

    for (int ii = 0; ii < rank; ii++)
      current[ii] = index[ii];
    return this;
  }


  /**
   * set current element at dimension dim to v
   * @param dim set this dimension
   * @param value to this value
   */
  public void setDim(int dim, int value) {
    if (value < 0 || value >= shape[dim])  // check index here
      throw new ArrayIndexOutOfBoundsException();
    current[dim] = value;
  }

  /**
   * set current element at dimension 0 to v
   *
   * @return this, so you can use A.get(i.set(i))
   */
  public Index set0(int v) {
    setDim(0, v);
    return this;
  }

  /**
   * set current element at dimension 1 to v
   *
   * @return this, so you can use A.get(i.set(i))
   */
  public Index set1(int v) {
    setDim(1, v);
    return this;
  }

  /**
   * set current element at dimension 2 to v
   *
   * @return this, so you can use A.get(i.set(i))
   */
  public Index set2(int v) {
    setDim(2, v);
    return this;
  }

  /**
   * set current element at dimension 3 to v
   *
   * @return this, so you can use A.get(i.set(i))
   */
  public Index set3(int v) {
    setDim(3, v);
    return this;
  }

  /**
   * set current element at dimension 4 to v
   *
   * @return this, so you can use A.get(i.set(i))
   */
  public Index set4(int v) {
    setDim(4, v);
    return this;
  }

  /**
   * set current element at dimension 5 to v
   *
   * @return this, so you can use A.get(i.set(i))
   */
  public Index set5(int v) {
    setDim(5, v);
    return this;
  }

  /**
   * set current element at dimension 6 to v
   *
   * @return this, so you can use A.get(i.set(i))
   */
  public Index set6(int v) {
    setDim(6, v);
    return this;
  }

  /**
   * set current element at dimension 0 to v0
   *
   * @return this, so you can use A.get(i.set(i))
   */
  public Index set(int v0) {
    setDim(0, v0);
    return this;
  }

  /**
   * set current element at dimension 0,1 to v0,v1
   *
   * @return this, so you can use A.get(i.set(i,j))
   */
  public Index set(int v0, int v1) {
    setDim(0, v0);
    setDim(1, v1);
    return this;
  }

  /**
   * set current element at dimension 0,1,2 to v0,v1,v2
   *
   * @return this, so you can use A.get(i.set(i,j,k))
   */
  public Index set(int v0, int v1, int v2) {
    setDim(0, v0);
    setDim(1, v1);
    setDim(2, v2);
    return this;
  }

  /**
   * set current element at dimension 0,1,2,3 to v0,v1,v2,v3
   *
   * @return this, so you can use A.get(i.set(i,j,k,l))
   */
  public Index set(int v0, int v1, int v2, int v3) {
    setDim(0, v0);
    setDim(1, v1);
    setDim(2, v2);
    setDim(3, v3);
    return this;
  }

  /**
   * set current element at dimension 0,1,2,3,4 to v0,v1,v2,v3,v4
   *
   * @return this, so you can use A.get(i.set(i,j,k,l,m))
   */
  public Index set(int v0, int v1, int v2, int v3, int v4) {
    setDim(0, v0);
    setDim(1, v1);
    setDim(2, v2);
    setDim(3, v3);
    setDim(4, v4);
    return this;
  }

  /**
   * set current element at dimension 0,1,2,3,4,5 to v0,v1,v2,v3,v4,v5
   *
   * @return this, so you can use A.get(i.set(i,j,k,l,m,n))
   */
  public Index set(int v0, int v1, int v2, int v3, int v4, int v5) {
    setDim(0, v0);
    setDim(1, v1);
    setDim(2, v2);
    setDim(3, v3);
    setDim(4, v4);
    setDim(5, v5);
    return this;
  }

  /**
   * set current element at dimension 0,1,2,3,4,5,6 to v0,v1,v2,v3,v4,v5,v6
   *
   * @return this, so you can use A.get(i.set(i,j,k,l,m,n,p))
   */
  public Index set(int v0, int v1, int v2, int v3, int v4, int v5, int v6) {
    setDim(0, v0);
    setDim(1, v1);
    setDim(2, v2);
    setDim(3, v3);
    setDim(4, v4);
    setDim(5, v5);
    setDim(6, v6);
    return this;
  }


  /**
   * String representation
   * @return String representation
   */
  public String toStringDebug() {
    StringBuffer sbuff = new StringBuffer(100);
    sbuff.setLength(0);

    sbuff.append(" shape= ");
    for (int ii = 0; ii < rank; ii++) {
      sbuff.append(shape[ii]);
      sbuff.append(" ");
    }

    sbuff.append(" stride= ");
    for (int ii = 0; ii < rank; ii++) {
      sbuff.append(stride[ii]);
      sbuff.append(" ");
    }

    if (name != null) {
      sbuff.append(" names= ");
      for (int ii = 0; ii < rank; ii++) {
        sbuff.append(name[ii]);
        sbuff.append(" ");
      }
    }

    sbuff.append(" offset= ").append(offset);
    sbuff.append(" rank= ").append(rank);
    sbuff.append(" size= ").append(size);

    sbuff.append(" current= ");
    for (int ii = 0; ii < rank; ii++) {
      sbuff.append(current[ii]);
      sbuff.append(" ");
    }

    return sbuff.toString();
  }

  public String toString() {
    StringBuffer sbuff = new StringBuffer(100);
    sbuff.setLength(0);
    for (int ii = 0; ii < rank; ii++) {
      if (ii > 0) sbuff.append(",");
      sbuff.append(current[ii]);
    }
    return sbuff.toString();
  }

  public int[] getCurrentCounter() {
    return current.clone();
  }

  public Object clone() {
    Index i;
    try {
      i = (Index) super.clone();
    } catch (CloneNotSupportedException e) {
      return null;
    }
    i.stride = stride.clone();
    i.shape = shape.clone();
    i.current = new int[rank];  // want zeros

    if (name != null)
      i.name = name.clone();

    return i;
  }

  //////////////////////////////////////////////////////////////
  /**
   * Set the name of one of the indices.
   *
   * @param dim       which index?
   * @param indexName name of index
   */
  public void setIndexName(int dim, String indexName) {
    if (name == null) name = new String[rank];
    name[dim] = indexName;
  }

  /**
   * Get the name of one of the indices.
   *
   * @param dim which index?
   * @return name of index, or null if none.
   */
  public String getIndexName(int dim) {
    if (name == null) return null;
    return name[dim];
  }

  ////////////////////// inner class ///////////////////////////

  /* the idea is IteratorFast can do the iteration without an Index
  public class IteratorFast implements IndexIterator {

    private int currElement = -1;
    private final Array maa;

    private IteratorFast(Array maa) {
      this.maa = maa;
      //System.out.println("IteratorFast");
    }

    public boolean hasNext() {
      return currElement < size-1;
    }

    public boolean hasMore(int howMany) {
      return currElement < size-howMany;
    }

    public double getDoubleCurrent() { return maa.getDouble(currElement); }
    public double getDoubleNext() { return maa.getDouble(++currElement); }
    public void setDoubleCurrent(double val) { maa.setDouble(currElement, val); }
    public void setDoubleNext(double val) { maa.setDouble(++currElement, val); }

    public float getFloatCurrent() { return maa.getFloat(currElement); }
    public float getFloatNext() { return maa.getFloat(++currElement); }
    public void setFloatCurrent(float val) { maa.setFloat(currElement, val); }
    public void setFloatNext(float val) { maa.setFloat(++currElement, val); }

    public long getLongCurrent() { return maa.getLong(currElement); }
    public long getLongNext() { return maa.getLong(++currElement); }
    public void setLongCurrent(long val) { maa.setLong(currElement, val); }
    public void setLongNext(long val) { maa.setLong(++currElement, val); }

    public int getIntCurrent() { return maa.getInt(currElement); }
    public int getIntNext() { return maa.getInt(++currElement); }
    public void setIntCurrent(int val) { maa.setInt(currElement, val); }
    public void setIntNext(int val) { maa.setInt(++currElement, val); }

    public short getShortCurrent() { return maa.getShort(currElement); }
    public short getShortNext() { return maa.getShort(++currElement); }
    public void setShortCurrent(short val) { maa.setShort(currElement, val); }
    public void setShortNext(short val) { maa.setShort(++currElement, val); }

    public byte getByteCurrent() { return maa.getByte(currElement); }
    public byte getByteNext() { return maa.getByte(++currElement); }
    public void setByteCurrent(byte val) { maa.setByte(currElement, val); }
    public void setByteNext(byte val) { maa.setByte(++currElement, val); }

    public char getCharCurrent() { return maa.getChar(currElement); }
    public char getCharNext() { return maa.getChar(++currElement); }
    public void setCharCurrent(char val) { maa.setChar(currElement, val); }
    public void setCharNext(char val) { maa.setChar(++currElement, val); }

    public boolean getBooleanCurrent() { return maa.getBoolean(currElement); }
    public boolean getBooleanNext() { return maa.getBoolean(++currElement); }
    public void setBooleanCurrent(boolean val) { maa.setBoolean(currElement, val); }
    public void setBooleanNext(boolean val) { maa.setBoolean(++currElement, val); }

    public Object next() { return maa.getObject(++currElement); }
  } */

  private class IteratorImpl implements IndexIterator {
    private int count = 0;
    private int currElement = 0;
    private Index counter;
    private Array maa;

    private IteratorImpl(Array maa) {
      this.maa = maa;
      counter = (Index) Index.this.clone();  // could be subtype of Index
      if (rank > 0)
        counter.current[rank - 1] = -1;                  // avoid "if first" on every incr.
      counter.precalc();
      //System.out.println("IteratorSlow");
    }

    public boolean hasNext() {
      return count < size;
    }

    public String toString() {
      return counter.toString();
    }

    public int[] getCurrentCounter() {
      return counter.getCurrentCounter();
    }

    public Object next() {
      count++;
      currElement = counter.incr();
      return maa.getObject(currElement);
    }

    public double getDoubleCurrent() {
      return maa.getDouble(currElement);
    }

    public double getDoubleNext() {
      count++;
      currElement = counter.incr();
      return maa.getDouble(currElement);
    }

    public void setDoubleCurrent(double val) {
      maa.setDouble(currElement, val);
    }

    public void setDoubleNext(double val) {
      count++;
      currElement = counter.incr();
      maa.setDouble(currElement, val);
    }

    public float getFloatCurrent() {
      return maa.getFloat(currElement);
    }

    public float getFloatNext() {
      count++;
      currElement = counter.incr();
      return maa.getFloat(currElement);
    }

    public void setFloatCurrent(float val) {
      maa.setFloat(currElement, val);
    }

    public void setFloatNext(float val) {
      count++;
      currElement = counter.incr();
      maa.setFloat(currElement, val);
    }

    public long getLongCurrent() {
      return maa.getLong(currElement);
    }

    public long getLongNext() {
      count++;
      currElement = counter.incr();
      return maa.getLong(currElement);
    }

    public void setLongCurrent(long val) {
      maa.setLong(currElement, val);
    }

    public void setLongNext(long val) {
      count++;
      currElement = counter.incr();
      maa.setLong(currElement, val);
    }

    public int getIntCurrent() {
      return maa.getInt(currElement);
    }

    public int getIntNext() {
      count++;
      currElement = counter.incr();
      return maa.getInt(currElement);
    }

    public void setIntCurrent(int val) {
      maa.setInt(currElement, val);
    }

    public void setIntNext(int val) {
      count++;
      currElement = counter.incr();
      maa.setInt(currElement, val);
    }

    public short getShortCurrent() {
      return maa.getShort(currElement);
    }

    public short getShortNext() {
      count++;
      currElement = counter.incr();
      return maa.getShort(currElement);
    }

    public void setShortCurrent(short val) {
      maa.setShort(currElement, val);
    }

    public void setShortNext(short val) {
      count++;
      currElement = counter.incr();
      maa.setShort(currElement, val);
    }

    public byte getByteCurrent() {
      return maa.getByte(currElement);
    }

    public byte getByteNext() {
      count++;
      currElement = counter.incr();
      return maa.getByte(currElement);
    }

    public void setByteCurrent(byte val) {
      maa.setByte(currElement, val);
    }

    public void setByteNext(byte val) {
      count++;
      currElement = counter.incr();
      maa.setByte(currElement, val);
    }

    public char getCharCurrent() {
      return maa.getChar(currElement);
    }

    public char getCharNext() {
      count++;
      currElement = counter.incr();
      return maa.getChar(currElement);
    }

    public void setCharCurrent(char val) {
      maa.setChar(currElement, val);
    }

    public void setCharNext(char val) {
      count++;
      currElement = counter.incr();
      maa.setChar(currElement, val);
    }

    public boolean getBooleanCurrent() {
      return maa.getBoolean(currElement);
    }

    public boolean getBooleanNext() {
      count++;
      currElement = counter.incr();
      return maa.getBoolean(currElement);
    }

    public void setBooleanCurrent(boolean val) {
      maa.setBoolean(currElement, val);
    }

    public void setBooleanNext(boolean val) {
      count++;
      currElement = counter.incr();
      maa.setBoolean(currElement, val);
    }

    public Object getObjectCurrent() {
      return maa.getObject(currElement);
    }

    public Object getObjectNext() {
      count++;
      currElement = counter.incr();
      return maa.getObject(currElement);
    }

    public void setObjectCurrent(Object val) {
      maa.setObject(currElement, val);
    }

    public void setObjectNext(Object val) {
      count++;
      currElement = counter.incr();
      maa.setObject(currElement, val);
    }
  }

  ////////////////////// static /////////////////////////////////

  /**
   * Generate a subclass of Index optimized for this array's rank
   * @param shape use this shape
   * @return a subclass of Index optimized for this array's rank
   */
  static public Index factory(int[] shape) {
    int rank = shape.length;
    switch (rank) {
      case 0:
        return new Index0D(shape);
      case 1:
        return new Index1D(shape);
      case 2:
        return new Index2D(shape);
      case 3:
        return new Index3D(shape);
      case 4:
        return new Index4D(shape);
      case 5:
        return new Index5D(shape);
      case 6:
        return new Index6D(shape);
      case 7:
        return new Index7D(shape);
      default:
        return new Index(shape);
    }
  }

  private static Index factory(int rank) {
    switch (rank) {
      case 0:
        return new Index0D();
      case 1:
        return new Index1D();
      case 2:
        return new Index2D();
      case 3:
        return new Index3D();
      case 4:
        return new Index4D();
      case 5:
        return new Index5D();
      case 6:
        return new Index6D();
      case 7:
        return new Index7D();
      default:
        return new Index(rank);
    }
  }

  /**
   * Compute total number of elements in the array.
   *
   * @param shape length of array in each dimension.
   * @return total number of elements in the array.
   */
  static public long computeSize(int[] shape) {
    long product = 1;
    for (int ii = shape.length - 1; ii >= 0; ii--)
      product *= shape[ii];
    return product;
  }

  /**
   * Compute standard strides based on array's shape.
   *
   * @param shape  length of array in each dimension.
   * @param stride put result here
   * @return standard strides based on array's shape.
   */
  static private long computeStrides(int[] shape, int[] stride) {
    long product = 1;
    for (int ii = shape.length - 1; ii >= 0; ii--) {
      final int thisDim = shape[ii];
      if (thisDim < 0)
        throw new NegativeArraySizeException();
      stride[ii] = (int) product;
      product *= thisDim;
    }
    return product;
  }

}

/* Change History:
   $Log: Index.java,v $
   Revision 1.9  2006/01/31 21:15:53  caron
   bug in setCurrentElement() - it was totally wrong

   Revision 1.8  2005/12/15 00:29:08  caron
   *** empty log message ***

   Revision 1.7  2005/12/09 04:24:34  caron
   Aggregation
   caching
   sync

   Revision 1.6  2005/03/03 20:52:22  caron
   datatype checkin

   Revision 1.5  2004/08/26 17:55:06  caron
   no message

   Revision 1.4  2004/08/16 20:53:44  caron
   2.2 alpha (2)

   Revision 1.3  2004/07/12 23:40:13  caron
   2.2 alpha 1.0 checkin

 */
