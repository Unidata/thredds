/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.ft;

import java.io.Closeable;
import ucar.nc2.util.IOIterator;

/**
 * An iterator over PointFeatureCollections.
 * Use try-with-resource to make sure resources are released:
 * <pre>
  try (PointFeatureCollectionIterator iter = getIter()) {
    while (iter.hasNext())
      process(iter.next());
  }
  </pre>
 *
 * @author caron
 */
public interface PointFeatureCollectionIterator extends Closeable, IOIterator<PointFeatureCollection> {

  /**
   * true if another PointFeatureCollection is available
   * @return true if another PointFeatureCollection is available
   * @throws java.io.IOException on i/o error
   */
  boolean hasNext() throws java.io.IOException;

  /**
   * Returns the next PointFeatureCollection
   * You must call hasNext() before calling next(), even if you know it will return true.
   * @return the next PointFeatureCollection 
   * @throws java.io.IOException on i/o error
   */
  PointFeatureCollection next() throws java.io.IOException;

  /**
   * Make sure that the iterator is complete, and recover resources.
   * You must complete the iteration (until hasNext() returns false) or call close().
   * may be called more than once.
   */
  default void close()  {
    // doan do nuthin
  }

  /**
   * @deprecated use try-with-resource
   */
  default void finish() {
    close();
  }

  /**
   * A filter on PointFeatureCollection.
   */
  interface Filter {
   /**
     * Filter collections.
     * @param pointFeatureCollection check this collection
     * @return true if the collection passes the filter
     */
    boolean filter(PointFeatureCollection pointFeatureCollection);
  }

}
