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
