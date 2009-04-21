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
 * A collection of PointFeatures nested inside one or more PointFeatureCollection.
 * 
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
   * Get a subsetted NestedPointFeatureCollection based on a LatLonRect
   *
   * @param boundingBox spatial subset
   * @return subsetted collection
   * @throws java.io.IOException on i/o error
   */
  public NestedPointFeatureCollection subset(ucar.unidata.geoloc.LatLonRect boundingBox) throws IOException;

  /**
   *  Flatten into a PointFeatureCollection, discarding connectedness information. Optionally subset.
   * @param boundingBox only points in this lat/lon bounding box. may be null.
   * @param dateRange only points in this date range. may be null.
   * @return a PointFeatureCollection, may be null if its empty.
   * @throws IOException on read error
   */
  public PointFeatureCollection flatten(ucar.unidata.geoloc.LatLonRect boundingBox, DateRange dateRange) throws IOException;

}
