/*
 * Copyright 1998-2014 University Corporation for Atmospheric Research/Unidata
 *
 *   Portions of this software were developed by the Unidata Program at the
 *   University Corporation for Atmospheric Research.
 *
 *   Access and use of this software shall impose the following obligations
 *   and understandings on the user. The user is granted the right, without
 *   any fee or cost, to use, copy, modify, alter, enhance and distribute
 *   this software, and any derivative works thereof, and its supporting
 *   documentation for any purpose whatsoever, provided that this entire
 *   notice appears in all copies of the software, derivative works and
 *   supporting documentation.  Further, UCAR requests that the user credit
 *   UCAR/Unidata in any publications that result from the use of this
 *   software or in any product that includes this software. The names UCAR
 *   and/or Unidata, however, may not be used in any advertising or publicity
 *   to endorse or promote any products or commercial entity unless specific
 *   written permission is obtained from UCAR/Unidata. The user also
 *   understands that UCAR/Unidata is not obligated to provide the user with
 *   any support, consulting, training or assistance of any kind with regard
 *   to the use, operation and performance of this software nor to provide
 *   the user with any updates, revisions, new versions or "bug fixes."
 *
 *   THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 *   IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *   WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *   DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 *   INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 *   FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 *   NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 *   WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */

package ucar.ma2;

import javax.annotation.concurrent.Immutable;

import java.util.Iterator;

/**
 * Represents a set of integers, used as an index for arrays.
 * No duplicates are allowed.
 * It should be considered as a subset of the interval of integers [first(), last()] inclusive.
 * For example Range(1:11:3) represents the set of integers {1,4,7,10}
 * Note that Range no longer is always strided or monotonic.
 * Immutable.
 * <p>
 * Elements must be nonnegative and unique.
 * EMPTY is the empty Range.
 * VLEN is for variable length dimensions.
 * <p> Standard iteration is
 * <pre>
 *  for (int idx : range) {
 *    ...
 *  }
 * </pre>
 *
 * @author caron
 */

@Immutable
public class Range implements RangeIterator {
  public static final Range EMPTY = new Range(); // used for unlimited dimension = 0
  public static final Range ONE = new Range(1);
  public static final Range VLEN = new Range(-1);

  public static Range make(String name, int len) {
    try {
      return new Range(name, 0, len - 1, 1);
    } catch (InvalidRangeException e) {
      throw new RuntimeException(e); // cant happen if len > 0
    }
  }

  ////////////////////////////////////////////////////////

  protected final int length; // number of elements
  private final int first; // first value in range
  private final int last; // last value in range, inclusive
  private final int stride; // stride, must be >= 1
  protected final String name; // optional name

  /**
   * Used for EMPTY
   */
  private Range() {
    this.length = 0;
    this.first = 0;
    this.last = 0;
    this.stride = 1;
    this.name = null;
  }

  /**
   * Create a range with unit stride.
   *
   * @param first first value in range
   * @param last  last value in range, inclusive
   * @throws InvalidRangeException elements must be nonnegative, 0 <= first <= last
   */
  public Range(int first, int last) throws InvalidRangeException {
    this(null, first, last, 1);
  }

  /**
   * Create a range starting at zero, with unit stride.
   *
   * @param length number of elements in the Range
   */
  public Range(int length) {
    assert (length != 0);
    this.name = null;
    this.first = 0;
    this.last = length - 1;
    this.stride = 1;
    this.length = length;
  }

  /**
   * Create a named range with unit stride.
   *
   * @param name  name of Range
   * @param first first value in range
   * @param last  last value in range, inclusive
   * @throws InvalidRangeException elements must be nonnegative, 0 <= first <= last
   */
  public Range(String name, int first, int last) throws InvalidRangeException {
    this(name, first, last, 1);
  }

  /**
   * Create a range with a specified values.
   *
   * @param first  first value in range
   * @param last   last value in range, inclusive
   * @param stride stride between consecutive elements, must be > 0
   * @throws InvalidRangeException elements must be nonnegative: 0 <= first <= last, stride > 0
   */
  public Range(int first, int last, int stride) throws InvalidRangeException {
    this(null, first, last, stride);
  }

  /**
   * Create a named range with a specified name and values.
   *
   * @param name   name of Range
   * @param first  first value in range
   * @param last   last value in range, inclusive
   * @param stride stride between consecutive elements, must be > 0
   * @throws InvalidRangeException elements must be nonnegative: 0 <= first <= last, stride > 0
   */
  public Range(String name, int first, int last, int stride) throws InvalidRangeException {
    if (first < 0)
      throw new InvalidRangeException("first (" + first + ") must be >= 0");
    if (last < first)
      throw new InvalidRangeException("last (" + last + ") must be >= first (" + first + ")");
    if (stride < 1)
      throw new InvalidRangeException("stride (" + stride + ") must be > 0");

    this.name = name;
    this.first = first;
    this.stride = stride;
    this.length = 1 + (last - first) / stride;
    this.last = first + (this.length - 1) * stride;
    if (stride == 1)
      assert this.last == last;
    assert this.length != 0;
  }

  protected Range(String name, int first, int last, int stride, int length) throws InvalidRangeException {
    if (first < 0)
      throw new InvalidRangeException("first (" + first + ") must be >= 0");
    if (last < first)
      throw new InvalidRangeException("last (" + last + ") must be >= first (" + first + ")");
    if (stride < 1)
      throw new InvalidRangeException("stride (" + stride + ") must be > 0");
    if (length < (1 + last - first) / stride)
      throw new InvalidRangeException("length (" + length + ") must be > (1 + last - first) / stride");

    this.name = name;
    this.first = first;
    this.last = last;
    this.stride = stride;
    this.length = length;
  }

  // copy on change
  public Range setStride(int stride) throws InvalidRangeException {
    return new Range(this.first(), this.last(), stride);
  }

  @Override
  public Range setName(String name) {
    if (name.equals(this.getName())) return this;
    try {
      return new Range(name, first, last, stride, length);
    } catch (InvalidRangeException e) {
      throw new RuntimeException(e); // cant happen
    }
  }

  /**
   * @return name, or null if none
   */
  public String getName() {
    return name;
  }

  /**
   * @return first in range
   */
  public int first() {
    return first;
  }

  /**
   * @return last in range, inclusive
   */
  public int last() {
    return last;
  }

  /**
   * @return the number of elements in the range.
   */
  public int length() {
    return length;
  }

  /**
   * @return stride, must be >= 1 when evenly strided, -1 if not
   * // * @deprecated use iterator(), dont assume evenly strided
   */
  public int stride() {
    return stride;
  }

  /////////////////////////////////////////////

  /**
   * Is want contained in this Range?
   *
   * @param want index in the original Range
   * @return true if the ith element would be returned by the Range iterator
   */
  public boolean contains(int want) {
    if (want < first())
      return false;
    if (want > last())
      return false;
    if (stride == 1) return true;
    return (want - first) % stride == 0;
  }

  /**
   * Create a new Range by composing a Range that is reletive to this Range.
   * Revised 2013/04/19 by Dennis Heimbigner to handle edge cases.
   * See the commentary associated with the netcdf-c file dceconstraints.h,
   * function dceslicecompose().
   *
   * @param r range reletive to base
   * @return combined Range, may be EMPTY
   * @throws InvalidRangeException elements must be nonnegative, 0 <= first <= last
   */
  public Range compose(Range r) throws InvalidRangeException {
    if ((length() == 0) || (r.length() == 0))
      return EMPTY;
    if (this == VLEN || r == VLEN)
      return VLEN;
/* if(false) {// Original version
    // Note that this version assumes that range r is
    // correct with respect to this.
    int first = element(r.first());
    int stride = stride() * r.stride();
    int last = element(r.last());
    return new Range(name, first, last, stride);
} else {//new version: handles versions all values of r. */
    int sr_stride = this.stride * r.stride;
    int sr_first = element(r.first()); // MAP(this,i) == element(i)
    int lastx = element(r.last());
    int sr_last = (last() < lastx ? last() : lastx); //min(last(),lastx)
    //unused int sr_length = (sr_last + 1) - sr_first;
    return new Range(name, sr_first, sr_last, sr_stride);
  }

  /**
   * Create a new Range by compacting this Range by removing the stride.
   * first = first/stride, last=last/stride, stride=1.
   *
   * @return compacted Range
   * @throws InvalidRangeException elements must be nonnegative, 0 <= first <= last
   */
  public Range compact() throws InvalidRangeException {
    if (stride == 1) return this;
    int first = first() / stride;           // LOOK WTF ?
    int last = first + length() - 1;
    return new Range(name, first, last, 1);
  }

  /**
   * Get ith element
   *
   * @param i index of the element
   * @return the i-th element of a range.
   * @throws InvalidRangeException i must be: 0 <= i < length
   */
  public int element(int i) throws InvalidRangeException {
    if (i < 0)
      throw new InvalidRangeException("i must be >= 0");
    if (i >= length)
      throw new InvalidRangeException("i must be < length");

    return first + i * stride;
  }

  /**
   * Get the index for this element: inverse of element
   *
   * @param want the element of the range
   * @return index
   * @throws InvalidRangeException if illegal elem
   */
  public int index(int want) throws InvalidRangeException {
    if (want < first)
      throw new InvalidRangeException("elem must be >= first");
    int result = (want - first) / stride;
    if (result > length)
      throw new InvalidRangeException("elem must be <= first = n * stride");
    return result;
  }

  /**
   * Create a new Range by intersecting with a Range using same interval as this Range.
   * NOTE: we dont yet support intersection when both Ranges have strides
   *
   * @param r range to intersect
   * @return intersected Range, may be EMPTY
   * @throws InvalidRangeException elements must be nonnegative
   */
  public Range intersect(Range r) throws InvalidRangeException {
    if ((length() == 0) || (r.length() == 0))
      return EMPTY;
    if (this == VLEN || r == VLEN)
      return VLEN;

    int last = Math.min(this.last(), r.last());
    int resultStride = stride * r.stride();

    int useFirst;
    if (resultStride == 1) {  // both strides are 1
      useFirst = Math.max(this.first(), r.first());

    } else if (stride == 1) { // then r has a stride

      if (r.first() >= first())
        useFirst = r.first();
      else {
        int incr = (first() - r.first()) / resultStride;
        useFirst = r.first() + incr * resultStride;
        if (useFirst < first()) useFirst += resultStride;
      }

    } else if (r.stride == 1) { // then this has a stride

      if (first() >= r.first())
        useFirst = first();
      else {
        int incr = (r.first() - first()) / resultStride;
        useFirst = first() + incr * resultStride;
        if (useFirst < r.first()) useFirst += resultStride;
      }

    } else {
      throw new UnsupportedOperationException("Intersection when both ranges have a stride");
    }

    if (useFirst > last)
      return EMPTY;
    return new Range(name, useFirst, last, resultStride);
  }

  /**
   * Determine if a given Range intersects this one.
   * NOTE: we dont yet support intersection when both Ranges have strides
   *
   * @param r range to intersect
   * @return true if they intersect
   * @throws UnsupportedOperationException if both Ranges have strides
   */
  public boolean intersects(Range r) {
    if ((length() == 0) || (r.length() == 0))
      return false;
    if (this == VLEN || r == VLEN)
      return true;

    int last = Math.min(this.last(), r.last());
    int resultStride = stride * r.stride();

    int useFirst;
    if (resultStride == 1) {   // both strides are 1
      useFirst = Math.max(this.first(), r.first());

    } else if (stride == 1) { // then r has a stride

      if (r.first() >= first())
        useFirst = r.first();
      else {
        int incr = (first() - r.first()) / resultStride;
        useFirst = r.first() + incr * resultStride;
        if (useFirst < first()) useFirst += resultStride;
      }

    } else if (r.stride() == 1) { // then this has a stride

      if (first() >= r.first())
        useFirst = first();
      else {
        int incr = (r.first() - first()) / resultStride;
        useFirst = first() + incr * resultStride;
        if (useFirst < r.first()) useFirst += resultStride;
      }

    } else {
      throw new UnsupportedOperationException("Intersection when both ranges have a stride");
    }

    return (useFirst <= last);
  }

  /**
   * If this range is completely past the wanted range
   *
   * @param want desired range
   * @return true if  first() > want.last()
   */
  public boolean past(Range want) {
    return (first() > want.last());
  }

  /**
   * Create a new Range shifting this range by a constant factor.
   *
   * @param origin subtract this from each element
   * @return shifted range
   * @throws InvalidRangeException elements must be nonnegative, 0 <= first <= last
   */
  public Range shiftOrigin(int origin) throws InvalidRangeException {
    if (this == VLEN)
      return VLEN;

    int first = first() - origin;
    int last = last() - origin;
    return new Range(name, first, last, stride);
  }

  /**
   * Create a new Range by making the union with a Range using same interval as this Range.
   * NOTE: no strides
   *
   * @param r range to add
   * @return intersected Range, may be EMPTY
   * @throws InvalidRangeException elements must be nonnegative
   */
  public Range union(Range r) throws InvalidRangeException {
    if (length() == 0)
      return r;
    if (this == VLEN || r == VLEN)
      return VLEN;

    if (r.length() == 0)
      return this;

    int first = Math.min(this.first(), r.first());
    int last = Math.max(this.last(), r.last());
    return new Range(name, first, last);
  }

  /**
   * Find the first element in a strided array after some index start.
   * Return the smallest element k in the Range, such that <ul>
   * <li>k >= first
   * <li>k >= start
   * <li>k <= last
   * <li>k = element of this Range
   * </ul>
   *
   * @param start starting index
   * @return first in interval, else -1 if there is no such element.
   */
  public int getFirstInInterval(int start) {
    if (start > last()) return -1;
    if (start <= first) return first;
    if (stride == 1) return start;
    int offset = start - first;
    int i = offset / stride;
    i = (offset % stride == 0) ? i : i + 1; // round up
    return first + i * stride;
  }

  public String toString() {
    if (this.length == 0)
      return ":";  // EMPTY
    else if (this.length < 0)
      return ":";  // VLEN
    else
      return first + ":" + last() + (stride > 1 ? ":" + stride : "");
  }

  /**
   * Range elements with same first, last, stride are equal.
   */
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof Range)) return false;   // this catches nulls
    Range or = (Range) o;

    if ((length == 0) && (or.length == 0)) // empty ranges are equal
      return true;

    return (or.first == first) && (or.length == length) && (or.stride == stride) && (or.last == last);
  }

  /**
   * Override Object.hashCode() to implement equals.
   */
  public int hashCode() {
    int result = first;
    result = 37 * result + last;
    result = 37 * result + stride;
    result = 37 * result + length;
    return result;
  }

  /////////////////////////////////////////////////////////

  /**
   * Iterate over Range index
   *
   * @return Iterator over element indices
   * @deprecated use iterator() or foreach
   */
  public Iterator<Integer> getIterator() {
    return new MyIterator();
  }

  @Override
  public Iterator<Integer> iterator() {
    return new MyIterator();
  }

  private class MyIterator implements java.util.Iterator<Integer> {
    private int current = 0;

    public boolean hasNext() {
      return current < length;
    }

    public Integer next() {
      return elementNC(current++);
    }
  }

  /**
   * Get ith element; skip checking, for speed.
   */
  private int elementNC(int i) {
    return first + i * stride;
  }

}
