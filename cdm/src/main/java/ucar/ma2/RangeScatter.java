package ucar.ma2;

import ucar.nc2.util.Misc;

import java.util.*;

/**
 * A Range of indices describes by a list, rather than start:stop:stride.
 * Issues:
 *   ucar.ma2.Index
 *
 * @author John
 * @since 8/12/2015
 */
public class RangeScatter extends Range {
  int[] vals;

  public RangeScatter(String name, int... val) throws InvalidRangeException {
    super(name, val[0], val[val.length-1], val.length);
    this.vals = val;
  }

  @Override
  public RangeScatter compose(Range r) throws InvalidRangeException {
    int[] svals = new int[r.length()];

    int count = 0;
    Iterator iter = r.getIterator();
    while (iter.hasNext()) {
      svals[count++] = element(iter.next());
    }
    return new RangeScatter(name, svals);
  }

  @Override
  public RangeScatter shiftOrigin(int origin) throws InvalidRangeException {
    int[] svals = new int[n];
    for (int i=0; i<n; i++) svals[i] = vals[i] + origin;
    return new RangeScatter(name, svals);
  }

  @Override
  public Range intersect(Range r) throws InvalidRangeException {
    Set<Integer> sect = new HashSet<>();
    Iterator iter = r.getIterator();
    while (iter.hasNext()) {
      sect.add(iter.next());
    }

    List<Integer> result = new ArrayList<>();
    Iterator iter2 = getIterator();
    while (iter2.hasNext()) {
      int val = iter2.next();
      if (sect.contains(val))
        result.add(val);
    }

    if (result.size() == 0) return EMPTY;

    int[] svals = new int[result.size()];
    for (int i=0; i<result.size(); i++) svals[i] = result.get(i);
    return new RangeScatter(name, svals);
  }

  @Override
  public boolean intersects(Range r) {
    try {
      return intersect(r) != EMPTY;
    } catch (InvalidRangeException e) {
      e.printStackTrace();
      return false;
    }
  }

  @Override
  public RangeScatter union(Range r) throws InvalidRangeException {
    Set<Integer> union = new HashSet<>();
    Iterator iter = r.getIterator();
    while (iter.hasNext())
      union.add(iter.next());

    iter = getIterator();
    while (iter.hasNext())
      union.add(iter.next());

    List<Integer> result = new ArrayList<>();
    for (int val : union)
        result.add(val);
    Collections.sort(result);

    int[] svals = new int[result.size()];
    for (int i=0; i<result.size(); i++) svals[i] = result.get(i);
    return new RangeScatter(name, svals);  }

  @Override
  public int element(int i) throws InvalidRangeException {
    if (i < 0)
      throw new InvalidRangeException("i must be >= 0");
    if (i >= vals.length)
      throw new InvalidRangeException("i must be < length");

    return vals[i];
  }

  @Override
  public int index(int want) throws InvalidRangeException {
    int res =  Arrays.binarySearch(vals, want);
    if (res >= 0) return res;
    throw new InvalidRangeException("elem not found in RangeScatter");
  }

  @Override
  public boolean contains(int want) {
    return Arrays.binarySearch(vals, want) >= 0;
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
  @Override
  public int getFirstInInterval(int start) {
    if (start > last()) return -1;
    if (start <= first()) return first();
    for (int i=1; i<n; i++)
      if (start > vals[i])
        return i-1;

    return n-1;
  }

  @Override
  public String toString() {
    return "{"+Misc.showInts(vals)+"}";
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    if (!super.equals(o)) return false;

    RangeScatter that = (RangeScatter) o;
    return Arrays.equals(vals, that.vals);
  }

  @Override
  public int hashCode() {
    return Arrays.hashCode(vals);
  }

  @Override
  public Iterator getIterator() {
    return new ScatterIterator();
  }

  private class ScatterIterator extends Iterator {
    private int current = 0;

    @Override
    public boolean hasNext() {
      return current < n;
    }

    @Override
    public int next() {
      return vals[current++];
    }
  }
}
