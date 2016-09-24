package ucar.ma2;

import ucar.nc2.util.Misc;

import javax.annotation.concurrent.Immutable;
import java.util.Arrays;
import java.util.Iterator;

/**
 * A Range of indices describes by a list, rather than start:stop:stride.
 * Issues:
 *   ucar.ma2.Index
 *
 * @author John
 * @since 8/12/2015
 */
@Immutable
public class RangeScatter implements RangeIterator {
  private final int[] vals;
  private final String name;
  /**
   * Ctor
   * @param name optional name
   * @param val  should be sorted
   * @throws InvalidRangeException
   */
  public RangeScatter(String name, int... val) throws InvalidRangeException {
    // super(name, val[0], val[val.length-1], 1, val.length);
    this.name = name;
    this.vals = val;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public RangeIterator setName(String name) {
    if (name.equals(this.getName())) return this;
    try {
      return new RangeScatter(name, vals);
    } catch (InvalidRangeException e) {
      throw new RuntimeException(e); // cant happen
    }
  }

  @Override
  public int length() {
    return vals.length;
  }

 /* @Override
  public Range copy(String name) {
    try {
      return new RangeScatter(name, vals);
    } catch (InvalidRangeException e) {
      throw new RuntimeException(e); // cant happen
    }
  }

  @Override
  public RangeScatter compose(Range r) throws InvalidRangeException {
    int[] svals = new int[r.length()];

    int count = 0;
    for (int idx : r) {
      svals[count++] = element(idx);
    }
    return new RangeScatter(name, svals);
  }

  @Override
  public RangeScatter shiftOrigin(int origin) throws InvalidRangeException {
    int[] svals = new int[length];
    for (int i=0; i< length; i++) svals[i] = vals[i] + origin;
    return new RangeScatter(name, svals);
  }

  @Override
  public Range intersect(Range r) throws InvalidRangeException {
    Set<Integer> sect = new HashSet<>();
    for (int idx : r) {
      sect.add(idx);
    }

    List<Integer> result = new ArrayList<>();
    for (int idx : this) {
      if (sect.contains(idx))
        result.add(idx);
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
    for (int idx : r)
      union.add(idx);
    for (int idx : this)
      union.add(idx);

    List<Integer> result = union.stream().collect(Collectors.toList());
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
   *
  @Override
  public int getFirstInInterval(int start) {
    if (start > last()) return -1;
    if (start <= first()) return first();
    for (int i=1; i< length; i++)
      if (start > vals[i])
        return i-1;

    return length -1;
  }                                */

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
  public Iterator<Integer> iterator() {
    return new ScatterIterator();
  }

  private class ScatterIterator implements Iterator<Integer> {
    private int current = 0;

    @Override
    public boolean hasNext() {
      return current < vals.length;
    }

    @Override
    public Integer next() {
      return vals[current++];
    }
  }
}
