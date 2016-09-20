/*
 * Copyright 1998-2015 John Caron and University Corporation for Atmospheric Research/Unidata
 *
 *  Portions of this software were developed by the Unidata Program at the
 *  University Corporation for Atmospheric Research.
 *
 *  Access and use of this software shall impose the following obligations
 *  and understandings on the user. The user is granted the right, without
 *  any fee or cost, to use, copy, modify, alter, enhance and distribute
 *  this software, and any derivative works thereof, and its supporting
 *  documentation for any purpose whatsoever, provided that this entire
 *  notice appears in all copies of the software, derivative works and
 *  supporting documentation.  Further, UCAR requests that the user credit
 *  UCAR/Unidata in any publications that result from the use of this
 *  software or in any product that includes this software. The names UCAR
 *  and/or Unidata, however, may not be used in any advertising or publicity
 *  to endorse or promote any products or commercial entity unless specific
 *  written permission is obtained from UCAR/Unidata. The user also
 *  understands that UCAR/Unidata is not obligated to provide the user with
 *  any support, consulting, training or assistance of any kind with regard
 *  to the use, operation and performance of this software nor to provide
 *  the user with any updates, revisions, new versions or "bug fixes."
 *
 *  THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 *  IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *  WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *  DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 *  INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 *  FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 *  NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 *  WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
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
