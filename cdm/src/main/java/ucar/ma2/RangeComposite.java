/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.ma2;

import javax.annotation.concurrent.Immutable;

import java.util.*;

/**
 * A Composite of other RangeIterators.
 * Iterate over them in sequence.
 *
 * @author John
 * @since 8/19/2015
 */
@Immutable
public class RangeComposite implements RangeIterator {
  private final List<RangeIterator> ranges;
  private final String name;

  public RangeComposite(String name, List<RangeIterator> ranges) throws InvalidRangeException {
    this.name = name;
    this.ranges = ranges;
  }

  @Override
  public String getName() {
    return name;
  }

  public List<RangeIterator> getRanges() {
    return ranges;
  }

  @Override
  public RangeIterator setName(String name) {
    if (name.equals(this.getName())) return this;
    try {
      return new RangeComposite(name, ranges);
    } catch (InvalidRangeException e) {
      throw new RuntimeException(e); // cant happen
    }
  }

  @Override
  public java.util.Iterator<Integer> iterator() {
    Collection<Iterable<Integer>> iters = new ArrayList<>();
    for (RangeIterator r : ranges)
      iters.add(r);

    return new CompositeIterator<>(iters);
  }

  @Override
  public int length() {
    int result = 0;
    for (RangeIterator r : ranges)
      result += r.length();
    return result;
  }

  // generic could be moved to utils
  static private class CompositeIterator<T> implements Iterator<T> {
    Iterator<Iterable<T>> iters;
    Iterator<T> current;

    CompositeIterator(Collection<Iterable<T>> iters) {
      this.iters = iters.iterator();
      current = this.iters.next().iterator();
    }

    @Override
    public boolean hasNext() {
      if (current.hasNext()) return true;
      if (!iters.hasNext()) return false;
      current = iters.next().iterator();
      return hasNext();
    }

    @Override
    public T next() {
      return current.next();
    }
  }

}
