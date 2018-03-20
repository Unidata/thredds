/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.ft;

import java.io.Closeable;
import java.util.Iterator;

/**
 * An iterator over PointFeatures.
 * Use try-with-resource to make sure resources are released:
 * <pre>
  try (PointFeatureIterator iter = getIter()) {
    while (iter.hasNext())
      process(iter.next());
  }
  </pre>
 *
 * @author caron
 * @since Feb 18, 2008
 */
public interface PointFeatureIterator extends Closeable, Iterator<PointFeature> {

  /**
   * Check if another PointFeature is available.
   * <p>
   * Since this iterator may be used in a for-each statement, implementations should {@link #close close} it the first
   * time this method returns {@code false}, as it may not get closed otherwise.
   *
   * @return true if another PointFeature is available
   * @throws RuntimeException on i/o error
   */
  boolean hasNext();

  /**
   * Returns the next PointFeature.
   * You must call hasNext() before calling next(), even if you know it will return true.
   * @return the next PointFeature
   * @throws RuntimeException on i/o error
   */
  PointFeature next();

  /**
   * Make sure that the iterator is complete, and recover resources.
   * You must complete the iteration (until hasNext() returns false) or call close().
   * It may be called more than once (idempotent).
   */
  void close();

  /**
   * A filter on PointFeatures
   */
  interface Filter {
    /**
     * True if the PointFeature passes this filter
     * @param pointFeature the PointFeature to test
     * @return true if given pointFeature passes the filter
     */
    boolean filter(PointFeature pointFeature);
  }

}
