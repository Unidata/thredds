/*
 * Copyright 1997-2008 Unidata Program Center/University Corporation for
 * Atmospheric Research, P.O. Box 3000, Boulder, CO 80307,
 * support@unidata.ucar.edu.
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or (at
 * your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser
 * General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
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
   * @return true is there is another PointFeature in the iteration.
   * @throws java.io.IOException on read error
   */
  public boolean hasNext() throws java.io.IOException;

  /**
   * Use the internal iterator to get the next PointFeature in the iteration.
   * You must call hasNext() before you call this.
   * @return the next PointFeature in the iteration
   * @throws java.io.IOException on read error
   */
  public PointFeature next() throws java.io.IOException;

  /**
   * Reset the internal iterator for another iteration over the PointFeatures in this Collection.
   * @throws java.io.IOException on read error
   */
  public void resetIteration() throws IOException;

  /**
   * The number of points in the collection. May not be known until after iterating through the collection.
   * @return number of points in the collection, or -1 if not known.
   */
  public int size();

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
