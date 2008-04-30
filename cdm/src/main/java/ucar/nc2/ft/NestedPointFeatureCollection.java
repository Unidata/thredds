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
 * A collection of PointFeatures nested inside one or more PointFeatureCollection.
 * @author caron
 * @since Mar 20, 2008
 */
public interface NestedPointFeatureCollection extends FeatureCollection {

  /**
   * The number of elements in the collection. May not be known until after iterating through the collection.
   * @return number of elements in the collection, or -1 if not known.
   */
  public int size();

  /**
   * If true, use getNestedPointFeatureCollectionIterator, otherwise use getPointFeatureCollectionIterator.
   * @return if multiple nested
   */
  public boolean isMultipleNested();

  /**
   * Iterate through the collection, composed of PointFeatureCollection.  Use this only if isMultipleNested() = false.
   * @param bufferSize how many bytes can be used to buffer data, use -1 to use default.
   * @return an iterator through PointFeatureCollection objects.
   * @throws java.io.IOException on read error
   */
  public PointFeatureCollectionIterator getPointFeatureCollectionIterator(int bufferSize) throws java.io.IOException;

  /**
   * Iterate through the collection, composed of NestedPointFeatureCollection.  Use this only if isMultipleNested() = true.
   * @param bufferSize how many bytes can be used to buffer data, use -1 to use default.
   * @return an iterator through NestedPointFeatureCollection objects.
   * @throws java.io.IOException on read error
   */
  public NestedPointFeatureCollectionIterator getNestedPointFeatureCollectionIterator(int bufferSize) throws java.io.IOException;

  /**
   *  Flatten into a PointFeatureCollection, discarding connectedness information. Optionally subset.
   * @param boundingBox only points in this lat/lon bounding box. may be null.
   * @param dateRange only points in this date range. may be null.
   * @return a PointFeatureCollection, may be null if its empty.
   * @throws IOException on read error
   */
  public PointFeatureCollection flatten(ucar.unidata.geoloc.LatLonRect boundingBox, DateRange dateRange) throws IOException;

}
