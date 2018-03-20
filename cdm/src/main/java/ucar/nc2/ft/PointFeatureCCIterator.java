/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.ft;

import java.io.Closeable;

import ucar.nc2.util.IOIterator;

/**
 * An iterator over PointFeatureCC.
 * Use try-with-resource to make sure resources are released:
 * <pre>
   try (PointFeatureCCIterator iter = getIter()) {
     while (iter.hasNext())
       process(iter.next());
   }
   </pre>
 *
 * @author caron
 * @since Mar 20, 2008
 */
public interface PointFeatureCCIterator extends Closeable, IOIterator<PointFeatureCC> {

  /**
   * true if another Feature object is available
   * @return true if another Feature object is available
   * @throws java.io.IOException on i/o error
   */
  boolean hasNext() throws java.io.IOException;

  /**
   * Returns the next NestedPointFeatureCollection object
   * You must call hasNext() before calling next(), even if you know it will return true.
   * @return the next NestedPointFeatureCollection object
   * @throws java.io.IOException on i/o error
   */
  PointFeatureCC next() throws java.io.IOException;

  /**
   * Make sure that the iterator is complete, and recover resources.
   * You must complete the iteration (until hasNext() returns false) or call close().
   * may be called more than once.
   */
  void close();

  /**
   * A filter on nestedPointFeatureCollection
   */
  interface Filter {
    /**
     * Filter collections.
     * @param nestedPointFeatureCollection check this collection
     * @return true if the collection passes the filter
     */
    boolean filter(PointFeatureCC nestedPointFeatureCollection);
  }

}
