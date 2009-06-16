/*
 * Copyright 1998-2009 University Corporation for Atmospheric Research/Unidata
 *
 * Portions of this software were developed by the Unidata Program at the
 * University Corporation for Atmospheric Research.
 *
 * Access and use of this software shall impose the following obligations
 * and understandings on the user. The user is granted the right, without
 * any fee or cost, to use, copy, modify, alter, enhance and distribute
 * this software, and any derivative works thereof, and its supporting
 * documentation for any purpose whatsoever, provided that this entire
 * notice appears in all copies of the software, derivative works and
 * supporting documentation.  Further, UCAR requests that the user credit
 * UCAR/Unidata in any publications that result from the use of this
 * software or in any product that includes this software. The names UCAR
 * and/or Unidata, however, may not be used in any advertising or publicity
 * to endorse or promote any products or commercial entity unless specific
 * written permission is obtained from UCAR/Unidata. The user also
 * understands that UCAR/Unidata is not obligated to provide the user with
 * any support, consulting, training or assistance of any kind with regard
 * to the use, operation and performance of this software nor to provide
 * the user with any updates, revisions, new versions or "bug fixes."
 *
 * THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 * INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 * FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 * NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 * WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */
package ucar.nc2.ft;

import ucar.unidata.geoloc.LatLonRect;
import ucar.nc2.units.DateRange;

/**
 * An iterator over PointFeatures.
 * You must complete the iteration or call finish(). Best to put in a try/finally block like:
  <pre>
  try {
   while (iter.hasNext())
     process(iter.next());
  } finally {
    iter.finish();
  }
  </pre>
 * @author caron
 * @since Feb 18, 2008
 */
public interface PointFeatureIterator {

  /**
   * Check if another PointFeature is available
   * @return true if another PointFeature is available
   * @throws java.io.IOException on i/o error
   */
  public boolean hasNext() throws java.io.IOException;

  /**
   * Returns the next PointFeature.
   * You must call hasNext() before calling next(), even if you know it will return true.
   * @return the next PointFeature
   * @throws java.io.IOException on i/o error
   */
  public PointFeature next() throws java.io.IOException;

  /**
   * Make sure that the iterator is complete, and recover resources.
   * You must complete the iteration (until hasNext() returns false) or call finish().
   * may be called more than once.
   */
  public void finish();
  
  /**
   * Hint to use this much memory in buffering the iteration.
   * No guarentee that it will be used by the implementation.
   * @param bytes amount of memory in bytes
   */
  public void setBufferSize( int bytes);

  /**
   * If this is set, then the iterator will calculate the bounding box, time range, and size,
   *   and make it available through getBoundingBox(), getDateRange(), and getSize().
   * @param collection if not null, on complete iteration set the results into the collection.
   */
  public void setCalculateBounds( PointFeatureCollection collection);

   /**
   * Get BoundingBox after iteration is finished, if calcBounds was set true
   * @return BoundingBox of all returned points
   */
  public LatLonRect getBoundingBox();

  /**
   * Get DateRange of observation time after iteration is finished, if calcBounds was set true
   * @return DateRange of all returned points
   */
  public DateRange getDateRange();

  /**
   * Get number of points after the iteration is finished, if calcBounds was set true
   * @return number of points in the iteration
   */
  public int getCount();

  /**
   * A filter on PointFeatures
   */
  public interface Filter {
    /**
     * True if the PointFeature passes this filter
     * @param pointFeature the PointFeature to test
     * @return true if given pointFeature passes the filter
     */
    public boolean filter(PointFeature pointFeature);
  }

}