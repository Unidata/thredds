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

import ucar.nc2.units.DateRange;

import java.io.IOException;

/**
 * A collection of PointFeatures.
 *
 * @author caron
 * @since Mar 1, 2008
 */
public interface PointFeatureCollection extends FeatureCollection {

  /**
   * Use the internal iterator to check if there is another PointFeature in the iteration.
   * Note that this is not thread-safe; use getPointFeatureIterator() for a threadsafe iterator.
   * @return true is there is another PointFeature in the iteration.
   * @throws java.io.IOException on read error
   * @see PointFeatureIterator#hasNext
   */
  public boolean hasNext() throws java.io.IOException;

  /**
   * Use the internal iterator to get the next PointFeature in the iteration.
   * You must call hasNext() before you call this.
   * @return the next PointFeature in the iteration
   * @throws java.io.IOException on read error
   * @see PointFeatureIterator#next
   */
  public PointFeature next() throws java.io.IOException;

  /**
   * Reset the internal iterator for another iteration over the PointFeatures in this Collection.
   * @throws java.io.IOException on read error
   */
  public void resetIteration() throws IOException;

  /**
   * Make sure that the internal iterator is complete, and recover resources.
   * You must complete the iteration (until hasNext() returns false)
   *  or call finish().
   * @see PointFeatureIterator#finish
   */
  public void finish();

  /**
   * The number of points in the collection. May not be known until after iterating through the collection.
   * @return number of points in the collection, or -1 if not known.
   */
  public int size();

  /**
   * Date range for the FeatureCollection. May not be known until after iterating through the collection.
   *
   * @return the date range for the entire collection, or null if unknown
   */
  public DateRange getDateRange();

  /**
   * The boundingBox for the FeatureCollection. May not be known until after iterating through the collection.
   *
   * @return the lat/lon boundingBox for the entire collection, or null if unknown.
   */
  public ucar.unidata.geoloc.LatLonRect getBoundingBox();

  /**
   * Set the date range for the FeatureCollection.
   *
   * @param range the date range for the entire collection
   */
  public void setDateRange(DateRange range);

  /**
   * Set the boundingBox for the FeatureCollection.
   *
   * @param bb the lat/lon boundingBox for the entire collection.
   */
  public void setBoundingBox(ucar.unidata.geoloc.LatLonRect bb);

  /**
   * Set the size of the FeatureCollection.
   *
   * @param npts size of the collection
   */
  public void setSize(int npts);

  /**
   * Caclulate date range and bounding box, and size, even if the data has to be scanned.
   * This ensures that getDateRange() and getBoundingBox() return non-null.
   * If the collection already knows its size, date range and bounding box, then this has no effect.
   *
   * @throws java.io.IOException or read error.
   */
  public void calcBounds() throws java.io.IOException;

  /**
   * Get an iterator over the PointFeatures of this collection
   * @param bufferSize how many bytes can be used to buffer data, use -1 to use default.
   * @return iterator over the PointFeatures of this collection
   * @throws IOException on read error
   */
  public PointFeatureIterator getPointFeatureIterator(int bufferSize) throws java.io.IOException;

  /**
   * Subset this collection by boundingBox and/or dateRange
   * @param boundingBox only points in this lat/lon bounding box. may be null.
   * @param dateRange only points in this date range. may be null.
   * @return subsetted collection, may be null if empty
   * @throws IOException on read error
   */
  public PointFeatureCollection subset(ucar.unidata.geoloc.LatLonRect boundingBox, DateRange dateRange) throws IOException;
}
