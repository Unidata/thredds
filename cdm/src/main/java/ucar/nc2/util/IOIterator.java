/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.util;

import java.io.IOException;
import java.util.NoSuchElementException;

/**
 * An Iterator that can throw an IOException
 *
 * @author caron
 * @since 9/23/2015.
 */
public interface IOIterator<T> {

  /**
   * Returns {@code true} if the iteration has more elements.
   * (In other words, returns {@code true} if {@link #next} would
   * return an element rather than throwing an exception.)
   *
   * @return {@code true} if the iteration has more elements
   * @throws IOException on read error
   */
  boolean hasNext() throws IOException;

  /**
   * Returns the next element in the iteration.
   *
   * @return the next element in the iteration
   * @throws IOException on read error
   * @throws NoSuchElementException if the iteration has no more elements
   */
  T next() throws IOException;

}
