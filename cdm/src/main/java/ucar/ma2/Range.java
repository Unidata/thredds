/*
 IBM SOFTWARE DISCLAIMER
 Java array package (draft 0.2). Copyright (1998), International Business
 Machines Corporation.
 Permission to use, copy, modify and distribute this software for any
 noncommercial purpose and without fee is hereby granted, provided that
 this copyright and permission notice appear on all copies of the
 software. The name of the IBM Corporation may not be used in any
 advertising or publicity pertaining to the use of the software. IBM
 makes no warranty or representations about the suitability of the
 software for any purpose.  It is provided "AS IS" without any express
 or implied warranty, including the implied warranties of
 merchantability, fitness for a particular purpose and non-infringement.
 IBM shall not be liable for any direct, indirect, special or
 consequential damages resulting from the loss of use, data or projects,
 whether in an action of contract or tort, arising out of or in
 connection with the use or performance of this software.
 */

package ucar.ma2;

import java.util.*;

/**
   Represents a range of integers, used as an index set for arrays.
   <P>
   To extract array sections from arrays, it is necessary to define
   ranges of indices along axes of arrays. Range objects are used for
   that purpose.
   <P>
   Ranges are monotonically increasing.
   Elements must be nonnegative.
   Ranges can be empty, if last = first - 1.
   <p> Note last is inclusive, so standard iteration is
   <pre>for (int i=range.first(); i<=range.last(); i+= range.stride()) {
   } </pre>

 *
 * @author caron
 * @version $Revision:51 $ $Date:2006-07-12 17:13:13Z $
 */

public class Range {

  /** Convert shape array to List of Ranges. Assume 0 origin for all. */
  public static List factory( int[] shape) {
    ArrayList result = new ArrayList();
    for (int i=0; i<shape.length; i++ ) {
      try {
        result.add( new Range( 0, Math.max(shape[i]-1, -1)));
      } catch (InvalidRangeException e) {
        return null;
      }
    }
    return result;
  }

  /** Check rangeList has no nulls, set from shape array.  */
  public static List setDefaults( List rangeList, int[] shape) {
    try {
      // entire rangeList is null
      if (rangeList == null) {
        rangeList = new ArrayList();
        for (int i = 0; i < shape.length; i++) {
          rangeList.add(new Range(0, shape[i]));
        }
        return rangeList;
      }

      // check that any individual range is null
      for (int i = 0; i < shape.length; i++) {
        Range r = (Range) rangeList.get(i);
        if (r == null) {
          rangeList.set(i, new Range(0, shape[i]-1));
        }
      }
      return rangeList;
    }
    catch (InvalidRangeException ex) {
      return null; // could happen if shape[i] is negetive
    }
  }

  /** Convert shape, origin array to List of Ranges.  */
  public static List factory( int[] origin, int[] shape) throws InvalidRangeException {
    ArrayList result = new ArrayList();
    for (int i=0; i<shape.length; i++ ) {
      try {
        result.add(new Range(origin[i], origin[i] + shape[i] - 1));
      } catch (Exception e) {
        throw new InvalidRangeException( e.getMessage());
      }
    }
    return result;
  }

  /** Convert List of Ranges to shape array using the range.length.  */
  public static int[] getShape( List ranges) {
    if (ranges == null) return null;
    int[] result = new int[ranges.size()];
    for (int i=0; i<ranges.size(); i++ ) {
      result[i] = ((Range)ranges.get(i)).length();
    }
    return result;
  }

  public static String toString(List ranges) {
    if (ranges == null) return "";
    StringBuffer sbuff = new StringBuffer();
    for (int i=0; i<ranges.size(); i++ ) {
      if (i>0) sbuff.append(",");
      sbuff.append(((Range)ranges.get(i)).length());
    }
    return sbuff.toString();
  }

  /**
   /** Compute total number of elements represented by the section.
   * @param section List of Range objects
   * @return total number of elements
   */
  static public long computeSize(List section) {
    int[] shape = getShape( section);
    return Index.computeSize( shape);
  }

  /**
   * Append a new Range(0,size-1) to the list
   * @param ranges list of Range
   * @param size add this Range
   * @return same list
   * @throws InvalidRangeException if size < 1
   */
  public static List appendShape( List ranges, int size) throws InvalidRangeException {
    ranges.add( new Range(0, size-1));
    return ranges;
  }

  /** Convert List of Ranges to origin array using the range.first.  */
  public static int[] getOrigin( List ranges) {
    if (ranges == null) return null;
    int[] result = new int[ranges.size()];
    for (int i=0; i<ranges.size(); i++ ) {
      result[i] = ((Range)ranges.get(i)).first();
    }
    return result;
  }

  /** Convert List of Ranges to array of Ranges.  */
  public static Range[] toArray( List ranges) {
    if (ranges == null) return null;
    return (Range[]) ranges.toArray( new Range[ ranges.size()] );
  }

  /** Convert array of Ranges to List of Ranges.  */
  public static List toList( Range[] ranges) {
    if (ranges == null) return null;
    return java.util.Arrays.asList( ranges);
  }

  /** Convert List of Ranges to String Spec.
   * Inverse of parseSpec */
  public static String makeSectionSpec( List ranges) {
    StringBuffer sbuff = new StringBuffer();
    for (int i = 0; i < ranges.size(); i++) {
      Range r = (Range) ranges.get(i);
      if (i>0) sbuff.append(",");
      sbuff.append(r.toString());
    }
    return sbuff.toString();
  }


  /**
   * Parse an index section String specification, return equivilent list of ucar.ma2.Range objects.
   * The sectionSpec string uses fortran90 array section syntax, namely:
   * <pre>
   *   sectionSpec := dims
   *   dims := dim | dim, dims
   *   dim := ':' | slice | start ':' end | start ':' end ':' stride
   *   slice := INTEGER
   *   start := INTEGER
   *   stride := INTEGER
   *   end := INTEGER
   *
   * where nonterminals are in lower case, terminals are in upper case, literals are in single quotes.
   *
   * Meaning of index selector :
   *  ':' = all
   *  slice = hold index to that value
   *  start:end = all indices from start to end inclusive
   *  start:end:stride = all indices from start to end inclusive with given stride
   *
   * </pre>
   *
   * @param sectionSpec the token to parse, eg "(1:20,:,3,10:20:2)", parenthesis optional
   * @return return List of ucar.ma2.Range objects corresponding to the index selection. A null
   *   Range means "all" (i.e.":") indices in that dimension.
   *
   * @throws IllegalArgumentException when sectionSpec is misformed
   */
  public static List parseSpec(String sectionSpec) throws InvalidRangeException {

    ArrayList result = new ArrayList();
    Range section;

    StringTokenizer stoke = new StringTokenizer(sectionSpec,"(),");
    while (stoke.hasMoreTokens()) {
      String s = stoke.nextToken().trim();
      if (s.equals(":"))
        section = null; // all

      else if (s.indexOf(':') < 0) { // just a number : slice
        try {
          int index = Integer.parseInt(s);
          section = new Range( index, index);
        } catch (NumberFormatException e) {
          throw new IllegalArgumentException(" illegal selector: "+s+" part of <"+sectionSpec+">");
        }

      } else {  // gotta be "start : end" or "start : end : stride"
        StringTokenizer stoke2 = new StringTokenizer(s,":");
        String s1 = stoke2.nextToken();
        String s2 = stoke2.nextToken();
        String s3 = stoke2.hasMoreTokens() ? stoke2.nextToken() : null;
        try {
          int index1 = Integer.parseInt(s1);
          int index2 = Integer.parseInt(s2);
          int stride = (s3 != null) ? Integer.parseInt(s3) : 1;
          section = new Range( index1, index2, stride);
        } catch (NumberFormatException e) {
          throw new IllegalArgumentException(" illegal selector: "+s+" part of <"+sectionSpec+">");
        }
      }

      result.add(section);
    }

    return result;
  }

  // Each Range has a start, length and optionally a stride: start + stride*length < max.
  public static String checkInRange( List section, int shape[]) {
    if (section.size() != shape.length)
      return "Number of ranges in section must be ="+shape.length;
    for (int i=0; i<section.size(); i++) {
      Range r = (Range) section.get(i);
      if (r == null) continue;
      if (r.last() > shape[i])
        return "Illegal range for dimension "+i+": requested "+r.last()+" > max "+shape[i];
    }

    return null;
  }

  /////////////////////////////////////////////////////////////////////////////
  private String name; // optional name
  private int n; // number of elements
  private int first; // first value in range
  private int stride; // stride

  /**
     Create a range with unit stride.
     @param first	first value in range
     @param last	last value in range, inclusive
     @exception InvalidRangeException elements must be nonnegative
   */
  public Range(int first, int last) throws InvalidRangeException {
    this( first, last, 1);
  }

  /**
     Create a range with a specified stride.
     @param first	first value in range
     @param last	last value in range, inclusive
     @param stride	stride between consecutive elements (positive or negative)
     @exception InvalidRangeException elements must be nonnegative
   */
  public  Range(int first, int last, int stride) throws InvalidRangeException {

    if (first < 0)
      throw new InvalidRangeException();
    if (last < first-1)
      throw new InvalidRangeException();

    this.first = first;
    this.stride = stride;
    this.n = Math.max( (last - first) / stride + 1, 0);
  }

  /**
     Copy Constructor
   */
  public Range(Range r) throws InvalidRangeException {
    this(r.first(), r.last(), r.stride());
    setName( r.getName());
  }

  /**
     Create a range by combining two other ranges.
     @param base	base range
     @param r	range reletive to base
     @exception InvalidRangeException elements must be nonnegative
   */
  public Range(Range base, Range r) throws InvalidRangeException {
    this.first = base.element( r.first());
    this.stride = base.stride() * r.stride();

    if ((base.length() == 0) || (r.length() == 0)) {
      this.n = 0;
    } else {
      int last = base.element(r.last());
      this.n = Math.max( (last - first) / stride + 1, 0);
    }
    setName( r.getName());
  }

  /** Get name */
  public String getName() { return name; }
  /** Set name */
  public void setName( String name) { this.name = name; }

  /**
   * Return the number of elements in the range.
   */
  public int length() { return n; }

  /**
     Return the i-th element of a range.
     @param i	index of the element
     @exception InvalidRangeException 0 <= i < length
   */
  public int element(int i) throws InvalidRangeException {
    if (i < 0)
      throw new InvalidRangeException();
    if (i >= n)
      throw new InvalidRangeException();
    return first + i * stride;
  }

  /**
     Is the ith element contained in this Range?
     @param i	index in the original Range
     @return true if the ith element would be returned by the Range iterator
   */
  public boolean contains(int i) {
    if (i < min())
      return false;
    if (i > max())
      return false;
    if (stride == 1) return true;
    return (i-first) % stride == 0;
  }

  /**
     Return the i-th element of a range, no check
     @param i	index of the element
   */
  protected int elementNC(int i) {
    return first + i * stride;
  }

  /** first in range */
  public int first() {
    return first;
  }

  /** last in range, inclusive */
  public int last() {
    return first + (n - 1) * stride;
  }

  /** stride, may be negetive */
  public int stride() { return stride;  }

  /**
   * Minimum index, inclusive.
   */
  public int min() {
    if (n > 0) {
      if (stride > 0)
        return first;
      else
        return first + (n - 1) * stride;
    }
    else {
      return first;
    }
  }

  /**
   * Maximum index, inclusive.
   */
  public int max() {
    if (n > 0) {
      if (stride > 0)
        return first + (n - 1) * stride;
      else
        return first;
    }
    else {
      if (stride > 0)
        return first - 1;
      else
        return first + 1;
    }
  }

  public String toString() {
    return first+":"+last()+":"+stride;
  }

  /** Range elements with same first, last, stride are equal. */
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof Range)) return false;
    Range or =(Range) o;

    return (or.first == first) && (or.n == n) && (or.stride == stride);
  }

  /** Override Object.hashCode() to implement equals. */
  public int hashCode() {
    if (hashCode == 0) {
      int result = first();
      result = 3700*result + last();
      result = 370*result + stride();
      hashCode = result;
    }
    return hashCode;
  }
  private volatile int hashCode = 0;

  /**
   * Iterate over Range index
   * Usage: <pre>
   * Iterator iter = range.getIterator();
   * while (iter.hasNext()) {
   *   int index = iter.next();
   *   doSomething(index);
   * }
   * </pre>
   */
  public Iterator getIterator() { return new Iterator(); }
  public class Iterator {
    private int current = 0;
    public boolean hasNext() { return current < n; }
    public int next() {
      return elementNC(current++);
    }
  }

  /** return the smallest element k in the Range, such that <ul>
   * <li>k >= first
   * <li>k >= start
   * <li>k <= last
   * <li>k = first + i * stride for some integer i.
   * </ul>
   *  return -1 if there is no such element.
   */
  public int getFirstInInterval(int start) {
    if (start > last()) return -1;
    if (start <= first) return first;
    if (stride == 1) return start;
    int offset = start - first;
    int incr = offset % stride;
    int result = start + incr;
    return (result > last()) ? -1 : result;
  }

}

  /* public class SingleElementRangeIterator {
    private int current = 0;

    public boolean hasNext() { return current < last(); }
    public Range next() {
      int next = elementNC( current++);
      try {
        return new Range(next, next + stride - 1, stride); // only has one element
      } catch (InvalidRangeException e) {
        return null; // cant happen
      }
    }
  } */
