/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.ma2;

import java.util.Iterator;
import java.util.Spliterator;
import java.util.function.Consumer;

/**
 * Generalization of Range (which is restricted to (start:stop:stride).
 * RangeIterator is over an arbitrary set of integers from the set {0..fullSize-1}.
 *
 * @author John
 * @since 8/19/2015
 */
public interface RangeIterator extends Iterable<Integer> {

  @Override
  Iterator<Integer> iterator();

  int length();

  String getName();

  // copy on mutate
  RangeIterator setName(String name);

}
